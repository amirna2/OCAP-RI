// COPYRIGHT_BEGIN
//  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
//  
//  Copyright (C) 2008-2013, Cable Television Laboratories, Inc. 
//  
//  This software is available under multiple licenses: 
//  
//  (1) BSD 2-clause 
//   Redistribution and use in source and binary forms, with or without modification, are
//   permitted provided that the following conditions are met:
//        ·Redistributions of source code must retain the above copyright notice, this list 
//             of conditions and the following disclaimer.
//        ·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
//             and the following disclaimer in the documentation and/or other materials provided with the 
//             distribution.
//   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
//   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
//   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
//   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
//   HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
//   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
//   LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
//   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
//   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
//   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
//   THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//  
//  (2) GPL Version 2
//   This program is free software; you can redistribute it and/or modify
//   it under the terms of the GNU General Public License as published by
//   the Free Software Foundation, version 2. This program is distributed
//   in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
//   even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
//   PURPOSE. See the GNU General Public License for more details.
//  
//   You should have received a copy of the GNU General Public License along
//   with this program.If not, see<http:www.gnu.org/licenses/>.
//  
//  (3)CableLabs License
//   If you or the company you represent has a separate agreement with CableLabs
//   concerning the use of this code, your rights and obligations with respect
//   to this code shall be as set forth therein. No license is granted hereunder
//   for any other purpose.
//  
//   Please contact CableLabs if you need additional information or 
//   have any questions.
//  
//       CableLabs
//       858 Coal Creek Cir
//       Louisville, CO 80027-9750
//       303 661-9100
// COPYRIGHT_END


#include "mpe_cacheManager.h"
#include "mpe_filter.h"
#include "mpe_file.h"
#include "mpe_dbg.h"
#include "mpe_types.h"
#include "mpe_os.h"
#include "mpe_objectCarouselUtils.h"

#include <string.h> // memcpy
#ifndef __GNUCC__
#define inline
#endif

// Forward Declarations
static int32_t dccPurgeMemory(mpe_MemColor, int32_t, int64_t, void *);
static mpe_Error dccPurgeObject(mpe_DccCacheObject *);

static inline void dccRemoveObject(mpe_DccCacheObject *);
static inline void dccInsertHead(mpe_DccCacheObject *);

// Define an invalid section value.
#ifdef MPE_FEATURE_OC_CACHE_SECTIONS
#define INVALID_SECTION      0
#else
#define INVALID_SECTION      NULL
#endif

// Specify the color for the memory reclamation callback
#ifdef MPE_FEATURE_OC_CACHE_SECTIONS
#define MEM_CALLBACK_COLOR   MPE_MEM_FILTER
#else
#define MEM_CALLBACK_COLOR   MPE_MEM_FILE_CAROUSEL
#endif

// Environment variables
#define OC_MAX_CACHE_SIZE_NAME      "OC.MAX.CACHESIZE"
#define OC_MAX_CACHE_SIZE_DEFAULT   0

/**
 * Cache object.
 * An element in a doubly linked list of cached elements.
 * The head element is the most recent one, the tail the LRU.
 */
struct mpe_DccCacheObject
{
    uint32_t lockCount;
    uint32_t size;
    mpe_DccCacheObject *prev;
    mpe_DccCacheObject *next;
#ifdef MPE_FEATURE_OC_CACHE_SECTIONS
    mpe_FilterSectionHandle section;
#else
    uint8_t *section;
#endif
};

static mpe_DccCacheObject head;
static mpe_DccCacheObject tail;

static mpe_Mutex dccGlobalMutex = NULL;

static uint32_t maxCacheSize = OC_MAX_CACHE_SIZE_DEFAULT;
static uint32_t cacheSize = 0;

static uint32_t objects = 0;
static uint32_t validObjects = 0;

/**
 * Create a new cache object, and insert it at the head of the LRU list (ie, NUR).
 * Creates the object from the section handle, and will release the section handle, the caller
 * should not release the handle.
 *
 * @param section       Handle for the section being processed.
 * @param retObject     [out] Pointer to where to return a pointer to the cache block.
 *
 * @return MPE_SUCCESS if the block is successfully created, error codes otherwise.
 */
mpe_Error mpe_dccNewCacheObject(mpe_FilterSectionHandle section,
        mpe_DccCacheObject **retObject)
{
    mpe_Error retCode = MPE_SUCCESS;
    mpe_DccCacheObject *cacheObject;
    uint32_t size;
#ifndef MPE_FEATURE_OC_CACHE_SECTIONS
    uint32_t bytesRead;
    uint8_t *array;
#endif

    if ((section == MPE_SF_INVALID_SECTION_HANDLE) || (retObject == NULL))
    {
        return MPE_EINVAL;
    }

    // Get the section information
    retCode = mpeos_filterGetSectionSize(section, &size);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DCC: Unable to read section size from section %08x\n", section);
        return retCode;
    }

    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL,
            sizeof(mpe_DccCacheObject), (void **) &cacheObject);
    if (retCode != MPE_SUCCESS)
    {
        return retCode;
    }

    // New objects come unlocked
    cacheObject->lockCount = 0;

#ifdef MPE_FEATURE_OC_CACHE_SECTIONS
    cacheObject->section = section;
#else
    // Allocate up the sub array
    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, size, (void **) &array);
    if (retCode != MPE_SUCCESS)
    {
        mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, cacheObject);
        *retObject = NULL;
        return retCode;
    }

    // Read the array
    retCode = mpe_filterSectionRead(section, 0, size, 0, array, &bytesRead);
    if (bytesRead != size || retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS, "DCC: Could not copy block\n");
        mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, array);
        mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, cacheObject);
        return retCode;
    }
    mpe_filterSectionRelease(section);

    // Save it away
    cacheObject->section = array;
#endif

    // Record the size;
    cacheObject->size = size;

    // Count it
    objects++;
    validObjects++;

    MPE_LOG(
            MPE_LOG_TRACE9,
            MPE_MOD_FILESYS,
            "DCC: New Cache Object Inserting block %p (%p), %d bytes.  Current cache size %d bytes.  %d objects.  %d valid objects\n",
            cacheObject, cacheObject->section, size, cacheSize, objects,
            validObjects);

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dccGlobalMutex);

    // Insert the object at the head of the queue
    dccInsertHead(cacheObject);

    cacheSize += size;

    if ((maxCacheSize != 0) && (cacheSize > maxCacheSize))
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "DCC: Cache size %d exceeded limit %d.  Purging %d bytes\n",
                cacheSize, maxCacheSize, cacheSize - maxCacheSize);

        (void) dccPurgeMemory(MPE_MEM_FILTER, cacheSize - maxCacheSize, 0, NULL);
    }

    mpe_mutexRelease(dccGlobalMutex);
    // END CRITICAL SECTION

    // Set up the return area
    *retObject = cacheObject;

    return retCode;
}

/**
 * Read from a cache object.  The object must be locked before being read.
 *
 * @param cacheObject   The object to read from.
 * @param offset        How far into the object to start reading.
 * @param length        Number of bytes to read.
 * @param dest          Pointer to an array to fill with the copied data.
 *
 * @return MPE_SUCCESS if the data is read.  Error codes otherwise.
 */
mpe_Error mpe_dccReadCacheObject(mpe_DccCacheObject *object, int offset,
        int length, uint8_t *dest)
{
#ifdef MPE_FEATURE_OC_CACHE_SECTIONS
    mpe_Error retCode;
    uint32_t bytesRead;
#endif

    if (dest == NULL || object == NULL)
    {
        return MPE_EINVAL;
    }
    if (object->lockCount == 0)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DCC: Attempting to read from unlocked cache block %p\n",
                object);
        return MPE_EINVAL;
    }

    MPE_LOG(MPE_LOG_TRACE9, MPE_MOD_FILESYS,
            "DCC: readCacheObject (%p, %d, %d, %p)\n", object, offset, length,
            dest);

#ifdef MPE_FEATURE_OC_CACHE_SECTIONS
    retCode = mpe_filterSectionRead(object->section, offset, length, 0, dest, &bytesRead);
    if (bytesRead != length)
    {
        // Do something.
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "DCC: Could not read %d bytes at offset %d in cache object %p\n",
                length, offset, object);

        // Uh, now what....
        return MPE_FS_ERROR_FAILURE;
    }
    return retCode;
#else
    memcpy(dest, &object->section[offset], length * sizeof(uint8_t));
#endif

    return MPE_SUCCESS;
}

/**
 * Lock a cache object so it can't be purged.
 * Does not prevent the object from being released.
 *
 * @param cacheObject   The object to lock.
 *
 * @return MPE_SUCCESS if the block is locked and can be read, MPE_ENODATA if the block has been purged.
 */
mpe_Error mpe_dccLockCacheObject(mpe_DccCacheObject *object)
{
    mpe_Error retCode;
    if (object == NULL)
    {
        return MPE_EINVAL;
    }

    MPE_LOG(MPE_LOG_TRACE9, MPE_MOD_FILESYS, "DCC: lockCacheObject(%p)\n",
            object);

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dccGlobalMutex);

    if (object->section != INVALID_SECTION)
    {
        object->lockCount++;
        retCode = MPE_SUCCESS;
    }
    else
    {
        retCode = MPE_ENODATA;
    }

    mpe_mutexRelease(dccGlobalMutex);
    // END CRITICAL SECTION

    return retCode;
}

/**
 * Unlock a block.
 *
 * @param cacheObject   The object to unlock.
 *
 * @return MPE_SUCCESS if the block is successfully unlocked, error codes otherwise.
 */
mpe_Error mpe_dccUnlockCacheObject(mpe_DccCacheObject *object)
{
    if (object == NULL)
    {
        return MPE_EINVAL;
    }
    if (object->lockCount == 0)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                "DCC: Attempting to unlock an unlocked cache object: %p\n",
                object);
        return MPE_EINVAL;
    }

    MPE_LOG(MPE_LOG_TRACE9, MPE_MOD_FILESYS, "DCC: unlockCacheObject(%p)\n",
            object);

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dccGlobalMutex);

    object->lockCount--;

    // If we unlocked it, put it at the head
    if (object->lockCount == 0)
    {
        // Remove it from where it is.
        dccRemoveObject(object);

        // Put it at the start.
        dccInsertHead(object);
    }

    mpe_mutexRelease(dccGlobalMutex);
    // END CRITICAL SECTION

    return MPE_SUCCESS;
}

/**
 * Release a cache block and it's underlying memory.  Any block can be released,
 * even locked ones.
 *
 * @param cacheObject   The cache object to release.
 *
 * @return MPE_SUCCESS if the block is successfully freed.
 */
mpe_Error mpe_dccReleaseCacheObject(mpe_DccCacheObject *object)
{
    if (object == NULL)
    {
        return MPE_EINVAL;
    }
    if (object->lockCount != 0)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "DCC: Freeing object %p with lock count = %d\n", object,
                object->lockCount);
    }

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dccGlobalMutex);

    // Note that it's gone.
    if (object->section != INVALID_SECTION)
    {
        // Remove it from the list.
        // If section == INVALID SECTION, we had already been removed from the list.
        dccRemoveObject(object);
        // Untrack the object
        cacheSize -= object->size;
        validObjects--;
    }
    objects--;

    mpe_mutexRelease(dccGlobalMutex);
    // END CRITICAL SECTION

    MPE_LOG(
            MPE_LOG_TRACE9,
            MPE_MOD_FILESYS,
            "DCC: Releasing object %p (%p).  %d objects left.  %d valid objects.  CacheSize %d\n",
            object, object->section, objects, validObjects, cacheSize);

    if (object->section != INVALID_SECTION)
    {
#ifdef MPE_FEATURE_OC_CACHE_SECTIONS
        mpe_filterSectionRelease(object->section);
#else
        mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, object->section);
#endif
    }
    mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, object);

    return MPE_SUCCESS;
}

/**
 * Determine if a block is currently present in the cache.  This is only true at the exact moment
 * of the check, no lock is performed.  The block could easily be purged before you access it.
 *
 * @param cacheObject   The cache object to check.
 *
 * @return True if the block is there, false if it's been purged.
 */
mpe_Bool mpe_dccBlockPresent(mpe_DccCacheObject *cacheObject)
{
    if (cacheObject == NULL)
    {
        return false;
    }
    if (cacheObject->section == INVALID_SECTION)
    {
        return false;
    }
    else
    {
        return true;
    }
}

//----------------------------------------------------------------------------------------
// Internal functions
// ---------------------------------------------------------------------------------------
static mpe_Error dccPurgeObject(mpe_DccCacheObject *object)
{
    mpe_Error retCode = MPE_ENODATA;

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dccGlobalMutex);

    // Purge the block
    if (object->lockCount == 0)
    {
        if (object->section != INVALID_SECTION)
        {
            cacheSize -= object->size;
            validObjects--;

            MPE_LOG(
                    MPE_LOG_TRACE9,
                    MPE_MOD_FILESYS,
                    "DCC: Purging object %p (%d bytes) %p.  Objects Left: %d.  Valid objects: %d.  CacheSize: %d\n",
                    object, object->size, object->section, objects,
                    validObjects, cacheSize);
#ifdef MPE_FEATURE_OC_CACHE_SECTIONS
            mpe_filterSectionRelease(object->section);
#else
            mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, object->section);
#endif

            retCode = MPE_SUCCESS;

            // Get rid of the section
            object->section = INVALID_SECTION;

            // Remove it from the list.
            dccRemoveObject(object);
        }
    }
    else
    {
        MPE_LOG(
                MPE_LOG_WARN,
                MPE_MOD_FILESYS,
                "DCC: Attempting to purge locked object %p.  Lock Count %d.  Ignoring\n",
                object, object->lockCount);
    }

    mpe_mutexRelease(dccGlobalMutex);
    // END CRITICAL SECTION
    return retCode;
}

static int32_t dccPurgeMemory(mpe_MemColor color, int32_t bytesToPurge,
        int64_t contextId, void *context)
{
    int32_t bytesPurged = 0;
    mpe_DccCacheObject *object;
    mpe_DccCacheObject *next;
    uint32_t checkedBlocks = 0;

    MPE_UNUSED_PARAM(contextId);
    MPE_UNUSED_PARAM(context);

    // Return immediately if given color matches that given at registration
    if (MEM_CALLBACK_COLOR == color)
    {
        return 0;
    }

    // If the caller doesn't know how much to purge, then we'll
    // go for 1 meg.
    if (bytesToPurge == -1)
    {
        bytesToPurge = 1024 * 1024;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "DCC: Purge requested for %d bytes\n", bytesToPurge);

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(dccGlobalMutex);

    // Start at the tail
    object = tail.prev;

    // Walk backwards in the list, purging out any objects that are unlocked,
    // until we've purged the right amount
    while ((bytesPurged < bytesToPurge) && (object != &head))
    {
        // Grab the next one (actually the previous) one to check, just in case
        // we purge out this one.
        next = object->prev;
        checkedBlocks++;

        // Is this next object unlocked, if so, purge it.  Assuming it hasn't been
        // purged already, of course.
        if ((object->lockCount == 0) && (object->section != INVALID_SECTION))
        {
            if (dccPurgeObject(object) == MPE_SUCCESS)
            {
                bytesPurged += object->size;
            }
        }
        // And step backwards through the list.
        object = next;
    }

    mpe_mutexRelease(dccGlobalMutex);
    // END CRITICAL SECTION

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "DCC: Checked %d positions to free %d bytes.  Purged %d bytes\n",
            checkedBlocks, bytesToPurge, bytesPurged);

    return bytesPurged;
}

static
inline
void dccRemoveObject(mpe_DccCacheObject *object)
{
    object->prev->next = object->next;
    object->next->prev = object->prev;
}

static
inline
void dccInsertHead(mpe_DccCacheObject *object)
{
    object->next = head.next;
    head.next->prev = object;
    object->prev = &head;
    head.next = object;
}

/**
 * Initialize the cache manager.
 *
 * @return MPE_SUCCESS if we successfully initialize.
 */
void mpe_dccInit(void)
{
    // Set up the list.
    head.next = &tail;
    tail.prev = &head;
    head.prev = NULL;
    tail.next = NULL;

    head.section = INVALID_SECTION;
    head.size = 0;
    head.lockCount = 1; // Mark it locked, just for safety.
    tail.section = INVALID_SECTION;
    tail.size = 0;
    tail.lockCount = 1; // Mark it locked, just for safety.

    // Get any environment variables
    maxCacheSize = ocuGetEnv(OC_MAX_CACHE_SIZE_NAME, OC_MAX_CACHE_SIZE_DEFAULT);

    // Register the purge routine
    (void) mpe_memRegisterMemFreeCallback(MEM_CALLBACK_COLOR, dccPurgeMemory,
            NULL);

    (void) mpe_mutexNew(&dccGlobalMutex);
}
