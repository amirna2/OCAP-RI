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

#include <stddef.h>
#include <stdlib.h>
#include <string.h>

#include <mpe_file.h>
#include <mpe_types.h>
#include <mpe_dbg.h>
#include <mpe_os.h>
#include <mpe_filter.h>
#include <mpe_ed.h>

#include <mpeos_file.h>

#include "mpe_objectCarousel.h"
#include "mpe_dataCarousel.h"

// Define a maxmimum number of object carousel mounts.

#define URL_OC_PREFIX       "ocap:"

#define MAX_OC_MOUNTS       (32)
#define NO_MOUNTS_AVAILABLE (-1)

#define OCFS_THREAD_STACK_SIZE      (64 * 1024)

#define OCFS_CHECK_QUIT             (0x5000)

#define INVALID_VERSION     (0xffffffff)

#define PATH_SEPARATOR                  ('/')

// Get rid of inline if it's not supported
#ifndef __GNUC__
#define inline
#endif

/**
 * Container for callbacks on version change notifications.
 */
typedef struct ocfsFileVersion ocfsFileVersion;
struct ocfsFileVersion
{
    char *name; // File name
    uint32_t version; // Most recent version seen
    mpe_EventQueue evQueue; // Queue to send events to
    mpe_Bool evQueueRegistered; // Queue to send events to
    mpe_ObjectCarousel *oc; // The object carousel to look in.
    void *act;
    ocfsFileVersion *next; // Linked list pointer
};

/* Mount point. */
typedef struct
{
    char *url;
    char *mountName;
    mpe_SiServiceHandle siHandle;
    uint32_t carouselId;
    mpe_ObjectCarousel *oc;
    mpe_Bool inUse;
} mpe_ocMountPoint;

/* OC File Handle */
typedef struct
{
    mpe_OcFile *ocFile;
    int64_t readLoc;
} mpe_FileOC;

static mpe_ocMountPoint mountPoints[MAX_OC_MOUNTS];
static int lastMountPointUsed;
static int watchedFiles = 0;
static mpe_Mutex ocfsGlobalMutex;
static mpe_EventQueue ocfsEventQueue;
static mpe_ThreadId ocfsWatcherThreadId = 0;

// Forward Declarations
static void ocfsThreadMain(void *);

/**
 *
 */
static
void ocfsInit(const char* mountPoint)
{
    int i;
    mpe_Error retCode;

    MPE_UNUSED_PARAM(mountPoint);

    (void) mpe_dcInit();
    (void) mpe_ocInit();

    // Mark all the mount points as empty.
    for (i = 0; i < MAX_OC_MOUNTS; i++)
    {
        mountPoints[i].url = NULL;
        mountPoints[i].siHandle = 0;
        mountPoints[i].carouselId = 0;
        mountPoints[i].oc = NULL;
        mountPoints[i].inUse = false;
        // memset(mountPoints[i].nsap, 0, (OCP_DSI_SERVERID_LENGTH * sizeof(uint8_t)));
    }
    lastMountPointUsed = MAX_OC_MOUNTS - 1;

    retCode = mpe_mutexNew(&ocfsGlobalMutex);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCFS: Could not OCFS Mutex: Error code %04x\n", retCode);
    }

    retCode = mpe_eventQueueNew(&ocfsEventQueue, "MpeOcEvent");
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCFS: Could not create OCFS Event Queue: Error code %04x\n",
                retCode);
    }
}

/**
 * Scan the mounts points and find one that's not being used.
 * Always starts searching at a "lastMountPointUsed" so that
 * each time it will find different mount points, hopefully
 * to help eliminate problems with using the same name multiple times
 * and having the apps try to mount files in an FS which they no longer
 * have access to.
 *
 * Returns NO_MOUNTS_AVAILABLE if it can't find one, the mount point
 * otherwise.
 */
static
int getOpenMount(void)
{
    int i;
    int index;
    int mountPoint = NO_MOUNTS_AVAILABLE;

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(ocfsGlobalMutex);

    for (i = 0; (i < MAX_OC_MOUNTS) && (mountPoint == NO_MOUNTS_AVAILABLE); i++)
    {
        index = (lastMountPointUsed + i + 1) % MAX_OC_MOUNTS;
        // If this is NULL, return it
        if (mountPoints[index].inUse == false)
        {
            lastMountPointUsed = mountPoint;
            mountPoint = index;
            mountPoints[index].inUse = true;
        }
    }

    mpe_mutexRelease(ocfsGlobalMutex);
    // END CRITICAL SECTION
    return mountPoint;
}

static inline mpe_Bool namecmp(const char *a, const char *b, char **c)
{
    char A = *a;
    char B = *b;
    // while we're in string characters, and we haven't hit a directory separator, keep going.
    while ((A == B) && (A != '\0') && (B != '\0') && (A != PATH_SEPARATOR)
            && (B != PATH_SEPARATOR))
    {
        ++a;
        ++b;
        A = *a;
        B = *b;
    }
    // if either of the characters we're dealing with is a directory separator, turn it into a NULL
    if (A == PATH_SEPARATOR)
    {
        A = '\0';
    }
    if (B == PATH_SEPARATOR)
    {
        B = '\0';
    }
    *c = (char *) a;
    return (A == B);
}

#define FIND_CAROUSEL(name, nameInFS, mountPoint, oc) \
    if ((oc = findCarousel(name, nameInFS, mountPoint) == NULL)) { \
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS, "OCFS: Carousel not found: %s\n", name); return MPE_FS_ERROR_NOT_FOUND; }

/**
 * This routine does two things.  One, it extracts the mount point component
 * of the name, and looks that up in the mount points table. Two, it returns a pointer
 * to the section of the name after the mountpoint.
 *
 * Ex: mpe_fileOpen("/oc/1/foobar") calls in here as ocfsOpen("/1/foobar"), and
 *     this routine pulls out the /1, returns object carousel 1, and returns /foobar
 *     as the filename within the FS.
 */

// cache the last one accessed.
static uint32_t lastCarousel = 0;

static mpe_ObjectCarousel *
findCarousel(const char *name, char **nameInFS, uint32_t *mountPoint)
{
    int pos = 0;
    int i;
    int temp;
    uint32_t ocNumber = 0;
    char *nextName = NULL;
    mpe_Bool found = false;

    // Error case, just in case.
    *nameInFS = NULL;

    // Search for the first non-slash character
    while (name[pos] != '\0' && name[pos] == '/')
    {
        pos++;
    }

    temp = lastCarousel; // Grab it in case it changes so we don't have to synchronize
    for (i = 0; !found && i < MAX_OC_MOUNTS; i++)
    {
        ocNumber = (temp + i) % MAX_OC_MOUNTS;
        if (mountPoints[ocNumber].inUse && namecmp(&name[pos],
                mountPoints[ocNumber].mountName, &nextName))
        {
            lastCarousel = ocNumber;
            found = true;
        }
    }
    // Didn't find it.
    if (!found || (nextName == NULL))
    {
        return NULL;
    }

    // Make sure the next character is a slash, ie, no garbage in
    // the name
    if (*nextName == '\0')
    {
        // Special case.  Reached the end of the string and there's nothing there.
        // Assume we want the root directory.  Convert name to a constant "/"
        nextName = "/";
    }
    else if (*nextName != '/')
    {
        return NULL;
    }

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_FILESYS,
            "OCFS: findCarousel %s found carousel ID %d new filename %s\n",
            name, ocNumber, nextName);

    // Good enough, return the name we found, and the object carousel.
    *nameInFS = nextName;
    *mountPoint = ocNumber;
    return mountPoints[ocNumber].oc;
}

static uint32_t getFileVersion(mpe_ObjectCarousel *oc, char *name)
{
    mpe_Error retCode;
    uint32_t version = INVALID_VERSION;
    mpe_OcFile *file;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "OCFS: Checking for version on file %s\n", name);
    retCode = mpe_ocOpenFile(oc, name, true, Cache_StreamOrCache, &file);
    if (retCode == MPE_SUCCESS)
    {
        retCode = mpe_ocGetVersion(file, &version);
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                    "OCFS: Unable to retrieve version from file %s: %04x\n",
                    name, retCode);
            version = INVALID_VERSION;
        }
        (void) mpe_ocCloseFile(file);
    }
    else
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                "OCFS: File open for version check %s failed: %04x\n", name,
                retCode);
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OCFS: Got version for %s: %d\n",
            name, version);
    return version;
}

/**
 * Create the structures necessary to start watching for version changes on a file.
 * @param oc            The Object Carousel
 * @param name          Name of the file to monitor within the carousel
 * @param info          FileInfo structure containing the callback info.
 *
 * @return MPE_SUCCESS if we inserted, error codes otherwise.
 */
static mpe_FileError setWatcher(mpe_ObjectCarousel *oc, char *name,
        mpe_EventQueue evQueue, void *act, ocfsFileVersion **outRecord)
{
    mpe_FileError retCode = MPE_SUCCESS;
    ocfsFileVersion *versionRecord;
    uint32_t version;

    // Just in case.
    *outRecord = NULL;

    // Get the version
    version = getFileVersion(oc, name);

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_FILESYS,
            "OCFS: Setting version change watcher on file %s.  Current version: %d\n",
            name, version);

    // Allocate up the structures
    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, sizeof(ocfsFileVersion),
            (void **) &versionRecord);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "OCFS: Could not create version change record for watching %s\n",
                name);
        return retCode;
    }
    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, strlen(name) + 1,
            (void **) &(versionRecord->name));
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "OCFS: Could not create version change record for watching %s\n",
                name);
        mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, versionRecord);
        return retCode;
    }

    // Populate it
    strcpy(versionRecord->name, name);
    versionRecord->version = version;

    versionRecord->evQueue = evQueue;
    versionRecord->evQueueRegistered = TRUE;
    versionRecord->act = act;

    versionRecord->oc = oc;

    // Put it in the list
    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(ocfsGlobalMutex);

    if (mpe_ocReferenceOC(oc))
    {
        // First, make sure we have the version watcher thread running.
        // Yeah, this is slow being in it's own critical section, but this operation
        // should be fairly rare (like, once per app or so), so I'm not incredibly worried.
        if (ocfsWatcherThreadId == 0)
        {
            retCode = mpe_threadCreate(ocfsThreadMain, NULL,
                    MPE_THREAD_PRIOR_SYSTEM, OCFS_THREAD_STACK_SIZE,
                    &ocfsWatcherThreadId, "mpeOCFSVersionChangeThread");
        }

        // Did that work?
        if (retCode != MPE_SUCCESS)
        {
            mpe_ocDereferenceOC(oc);
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_FILESYS,
                    "OCFS: Could not OCFS create version change watcher thread: Error code %04x\n",
                    retCode);
        }
        else
        {
            // Reference the carousel

            // Set the change notification.  Ignore the return code, it really can't fail.
            // If it's already set, ah, who cares.  Set it again.
            (void) mpe_ocSetChangeNotification(oc, ocfsEventQueue, (void *) oc);

            // Put the number into the list.
            versionRecord->next = mpe_ocGetUserData(oc);
            mpe_ocSetUserData(oc, versionRecord);

            // Count that we're watching this file
            watchedFiles++;
            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_FILESYS,
                    "OCFS: Set version watcher on file %s Initial version: %d Total watched files: %d\n",
                    name, version, watchedFiles);
        }
    }
    else
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "OCFS: Attempting to insert watcher into unmounted carousel.  File %s\n",
                name);
        mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, versionRecord->name);
        mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, versionRecord);
        versionRecord = NULL;
        retCode = MPE_EINVAL;
    }

    mpe_mutexRelease(ocfsGlobalMutex);
    // END CRITICAL SECTION

    *outRecord = versionRecord;
    return retCode;
}

static mpe_FileError ocfsSetChangeListener(const char *name,
        mpe_EventQueue evQueue, void *act, mpe_FileChangeHandle *out)
{
    mpe_ObjectCarousel *oc;
    mpe_Error retCode = MPE_SUCCESS;
    char *filename;
    uint32_t ocNumber;
    ocfsFileVersion *versionRecord;

    // Find the object carousel this belongs in, and strip out the sub-mount point
    oc = findCarousel(name, &filename, &ocNumber);
    if (oc == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCFS: Carousel not found: %s\n", name);
        return MPE_FS_ERROR_NOT_FOUND;
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "OCFS: Setting watch on file %s (%s)\n", name, filename);
    retCode = setWatcher(oc, filename, evQueue, act, &versionRecord);

    // Set up return state.
    *out = (mpe_FileChangeHandle) versionRecord;
    return retCode;
}

static mpe_FileError deleteWatcher(ocfsFileVersion *target)
{
    mpe_FileError retCode = MPE_EINVAL; // Assume not found
    ocfsFileVersion *curr;
    ocfsFileVersion *prev = NULL;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_FILESYS,
            "OCFS: Attempting to remove version change record %p for file %s\n",
            target, target->name);

    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(ocfsGlobalMutex);

    curr = (ocfsFileVersion *) mpe_ocGetUserData(target->oc);
    while (curr != NULL)
    {
        // Match the name and make sure we've got the same event queue information
        if (curr == target)
        {
            // Matched, delete it.
            if (prev == NULL)
            {
                mpe_ocSetUserData(target->oc, curr->next);
            }
            else
            {
                prev->next = curr->next;
            }
            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_FILESYS,
                    "OCFS: Removed version watcher on file %s Total watched files: %d\n",
                    target->name, watchedFiles - 1);

            mpe_ocDereferenceOC(target->oc);

            mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, target->name);
            mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, target);

            // Uncount this file
            watchedFiles--;

            retCode = MPE_SUCCESS; // Found it, all good
            break;
        }
        prev = curr;
        curr = curr->next;
    }

    // If this carousel no longer has any files being watched,
    // stop the notification
    if (mpe_ocGetUserData(target->oc) == NULL)
    {
        // Remove the watcher notification
        (void) mpe_ocUnsetChangeNotification(target->oc);

        // If there are no more files being watched at all, go and tell the
        // watcher thread to think about dying.  This obviously can only happen
        // if the current carousel has no files being watched, hence the nested if
        if (watchedFiles == 0)
        {
            if (ocfsWatcherThreadId != 0) // send only when there is a thread
                mpe_eventQueueSend(ocfsEventQueue, OCFS_CHECK_QUIT, 0, NULL, 0);
        }
    }

    mpe_mutexRelease(ocfsGlobalMutex);
    // END CRITICAL SECTION

    return retCode;
}

static mpe_FileError ocfsRemoveChangeListener(mpe_FileChangeHandle handle)
{
    mpe_Error retCode = MPE_SUCCESS;

    retCode = deleteWatcher((ocfsFileVersion *) handle);
    return retCode;
}

/**
 * Open a file.  This does the core work of opening a file.  It doesn't discriminate
 * between files, directories, stream events, or monkeys.  This function is called
 * by type specific functions.
 */
static mpe_FileError ocfsOpen(const char *name, mpe_Bool followLinks,
        mpe_DcCacheMode cacheMode, mpe_FileOC **h)
{
    mpe_FileError retCode = MPE_FS_ERROR_SUCCESS;
    mpe_FileOC *handle;
    mpe_ObjectCarousel *oc;
    char *fileName;
    uint32_t ocNumber;

    uint32_t version;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OCFS: Opening %s\n", name);

    // Clear out the error code.
    *h = NULL;

    /* allocate an OCFS file handle */
    if (mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, sizeof(mpe_FileOC),
            (void**) &handle) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCFS: Could not allocate file structure for file %s\n", name);
        return MPE_FS_ERROR_OUT_OF_MEM;
    }

    // Find the object carousel this belongs in, and strip out the sub-mount point
    oc = findCarousel(name, &fileName, &ocNumber);
    if (oc == NULL)
    {
        mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, handle);
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCFS: Carousel not found: %s\n", name);
        return MPE_FS_ERROR_NOT_FOUND;
    }
    if (fileName == NULL)
    {
        mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, handle);
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCFS: Empty filename after mountpoint: %s\n", name);
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    // Open the file
    retCode = mpe_ocOpenFile(oc, fileName, followLinks, cacheMode,
            &(handle->ocFile));
    if (retCode != MPE_SUCCESS)
    {
        mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, handle);
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                "OCFS: File open %s (%s) failed: Error %04x\n", name, fileName,
                retCode);
        return retCode;
    }

    retCode = mpe_ocGetVersion(handle->ocFile, &version);
    if (retCode == MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OCFS: Version of %s is %d\n",
                name, version);
    }
    else
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCFS: Could not get version of %s\n", name);
    }

    // Clear the pointer, set it to 0
    handle->readLoc = 0;

    // Make the handle into an mpe_File type, and be done with it.
    *h = handle;

    return retCode;
} /* end ocfsOpen() */

/**
 * <i>ocfsFileClose()</i>
 *
 * Closes a file previously opened with <i>ocfsFileOpen()</i>.
 *
 * @param h A ocfsFile handle, previously returned by <i>ocfsFileOpen()</i>.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError ocfsFileClose(mpe_File h)
{
    mpe_FileOC *handle = (mpe_FileOC *) h;
    mpe_FileError retCode;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OCFS: Closing file: %p\n", h);
    retCode = mpe_ocCloseFile(handle->ocFile);
    mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, handle);
    return retCode;
}

/**
 * <i>ocfsFileOpen()</i>
 *
 * Opens a file from a read-only carousel file system.
 *
 * @param name The path to the file to open.
 * @param openMode The type of access requested (read, write, create, truncate, append).
 * @param h A pointer to an ocfsFile handle, through which the opened file is returned.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.  <i>MPE_FS_ERROR_DEVICE_FAILURE</i> is returned if resources are
 *          unavailable.  This should be cleaned up.
 */
static mpe_FileError ocfsFileOpen(const char *name, mpe_FileOpenMode openMode,
        mpe_File *h)
{
    mpe_Error retCode;
    mpe_OcFileType filetype;
    mpe_FileOC *handle;
    mpe_DcCacheMode cacheMode;
    mpe_Bool followLinks = true;

    /* OC is a read-only filesystem.  Check that attributes are such */
    if ((openMode & (MPE_FS_OPEN_READ | MPE_FS_OPEN_WRITE
            | MPE_FS_OPEN_READWRITE)) != MPE_FS_OPEN_READ)
    {
        /* Not read, return "Read Only" Error */
        return MPE_FS_ERROR_READ_ONLY;
    }

    // Figure out caching mode
    switch (openMode & (MPE_FS_OPEN_STREAMONLY | MPE_FS_OPEN_CACHEONLY))
    {
    case MPE_FS_OPEN_STREAMONLY:
        cacheMode = Cache_StreamOnly;
        break;
    case MPE_FS_OPEN_CACHEONLY:
        cacheMode = Cache_CacheOnly;
        break;
    default:
        cacheMode = Cache_StreamOrCache;
        break;
    }

    // And should we follow links.
    if (openMode & MPE_FS_OPEN_NOLINKS)
    {
        followLinks = false;
    }

    retCode = ocfsOpen(name, followLinks, cacheMode, (mpe_FileOC **) (h));
    // If the file opened
    if (retCode == MPE_SUCCESS)
    {
        handle = (mpe_FileOC *) *h;
        // Make sure it's a file
        retCode = mpe_ocGetFileType(handle->ocFile, &filetype);
        if (retCode != MPE_SUCCESS || filetype != Type_File)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS, "OCFS: Not a file: %s\n",
                    name);
            ocfsFileClose(*h);
            *h = NULL;
            return MPE_FS_ERROR_INVALID_TYPE;
        }
    }

    return retCode;
}

/**
 * <i>ocfsFilePrefetch()
 *
 * Start prefetching a file.
 * Stupid imprementation.  Opens the file and then closes it.  Needs to be optimized.
 *
 * @param name      The name of the file to prefetch
 *
 */
static mpe_FileError ocfsFilePrefetch(const char *name)
{
    mpe_FileError retCode = MPE_FS_ERROR_SUCCESS;
    mpe_ObjectCarousel *oc;
    char *fileName;
    uint32_t ocNumber;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OCFS: Prefetching %s\n", name);

    // Find the object carousel this belongs in, and strip out the sub-mount point
    oc = findCarousel(name, &fileName, &ocNumber);
    if (oc == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCFS: Carousel not found: %s\n", name);
        return MPE_FS_ERROR_NOT_FOUND;
    }
    if (fileName == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCFS: Empty filename after mountpoint: %s\n", name);
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    // Open the file
    retCode = mpe_ocPrefetchFile(oc, fileName);

    return retCode;
}

/**
 * <i>ocfsFileRead()</i>
 *
 * Reads data from a file in carousel storage.
 *
 * @param h A ocfsFile handle, previously returned by <i>ocfsFileOpen()</i>.
 * @param count A pointer to a byte count.  On entry, this must point to the number of bytes to
 *                  read.  On exit, this will indicate the number of bytes actually read.
 * @param buffer A pointer to a buffer to receive the data.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError ocfsFileRead(mpe_File h, uint32_t *count, void *buffer)
{
    mpe_FileOC *handle = (mpe_FileOC *) h;
    mpe_FileError retCode;
    uint32_t bytesRead;

    // Check the params
    if (handle == NULL || buffer == NULL)
    {
        *count = 0;
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    // Check for EOF condition
    // BUG: Returns MPE_SUCCESS, which is what the layers above expect, but I think it should be
    // EOF.
    if (handle->readLoc >= handle->ocFile->length)
    {
        *count = 0;
        return MPE_SUCCESS;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "OCFS: Reading %d bytes at locaton %d (Object %p, Buffer %p)\n",
            *count, (uint32_t) handle->readLoc, handle->ocFile, buffer);
    // Read the data
    retCode = mpe_ocReadFile(handle->ocFile, (uint32_t) handle->readLoc,
            *count, (uint8_t *) buffer, &bytesRead);

    // Move the read pointer
    handle->readLoc += bytesRead;
    // Return the number of bytes read
    *count = bytesRead;

    // Later
    return retCode;
}

/**
 * <i>ocfsFileWrite()</i>
 *
 * This function is included for compatibility only.
 *
 * @param h A ocfsFile handle, previously returned by <i>ocfsFileOpen()</i>.
 * @param count A pointer to a byte count.  On entry, this must point to the number of bytes to
 *                  write.  On exit, this will indicate the number of bytes actually written.
 * @param buffer A pointer to a buffer containing the data to send.
 *
 * @return Always returns MPE_FS_ERROR_READ_ONLY
 */
static mpe_FileError ocfsFileWrite(mpe_File h, uint32_t* count, void* buffer)
{
    MPE_UNUSED_PARAM(h);
    MPE_UNUSED_PARAM(count);
    MPE_UNUSED_PARAM(buffer);

    return MPE_FS_ERROR_READ_ONLY;
}

/**
 * <i>ocfsFileSeek()</i>
 *
 * Changes and reports the current position within a file in persistent storage.
 *
 * @param h A ocfsFile handle, previously returned by <i>ocfsFileOpen()</i>.
 * @param seekMode A seek mode constant indicating whether the offset value should be considered
 *                  relative to the start, end, or current position within the file.
 * @param offset A pointer to a file position offset.  On entry, this should indicate the number
 *                  of bytes to seek, offset from the seekMode.  On exit, this will indicate the
 *                  new absolute position within the file.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError ocfsFileSeek(mpe_File h, mpe_FileSeekMode seekMode,
        int64_t *offset)
{
    mpe_FileError retCode = MPE_FS_ERROR_SUCCESS;
    mpe_FileOC *handle = (mpe_FileOC *) h;
    uint32_t size;

    retCode = mpe_ocGetFileSize(handle->ocFile, &size);
    if (retCode != MPE_SUCCESS)
    {
        return retCode;
    }

    switch (seekMode)
    {
    case MPE_FS_SEEK_SET:
        handle->readLoc = *offset;
        break;
    case MPE_FS_SEEK_CUR:
        handle->readLoc += *offset;
        break;
    case MPE_FS_SEEK_END:
        handle->readLoc = (int64_t) size + *offset;
        break;
    default:
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    // Check to make sure the position makes sense
    if (handle->readLoc > (int64_t) size)
    {
        handle->readLoc = (int64_t) size;
    }
    else if (handle->readLoc < 0)
    {
        handle->readLoc = 0;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OCFS: Seeking to location %d\n",
            (int32_t) handle->readLoc);

    // Set the output
    *offset = handle->readLoc;

    return MPE_FS_ERROR_SUCCESS;
}

/**
 * <i>ocfsFileGetFStat()</i>
 *
 * Get the status for the open file.
 *
 * @param handle An mpe_File handle, previously returned by <i>ocfsFileOpen()</i>.
 * @param mode The specific status of the file to return (see MPE_FS_STAT_xxx).
 * @param info A pointer to the buffer in which to receive the indicated file stat data.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError ocfsFileGetFStat(mpe_File h, mpe_FileStatMode mode,
        mpe_FileInfo *info)
{
    mpe_FileError retCode = MPE_FS_ERROR_SUCCESS; /* Assume success */
    mpe_FileOC *handle = (mpe_FileOC *) h;
    uint32_t tempUint;
    mpe_OcFileType filetype;

    switch (mode)
    {
    case MPE_FS_STAT_SIZE:
        retCode = mpe_ocGetFileSize(handle->ocFile, &tempUint);
        if (retCode != MPE_SUCCESS)
        {
            return retCode;
        }
        info->size = (uint64_t) tempUint;
        break;
    case MPE_FS_STAT_TYPE:
        retCode = mpe_ocGetFileType(handle->ocFile, &filetype);
        if (retCode != MPE_SUCCESS)
        {
            return retCode;
        }
        switch (filetype)
        {
        case Type_File:
            info->type = MPE_FS_TYPE_FILE;
            break;
        case Type_Directory:
            info->type = MPE_FS_TYPE_DIR;
            break;
        case Type_Stream:
            info->type = MPE_FS_TYPE_STREAM;
            break;
        case Type_StreamEvent:
            info->type = MPE_FS_TYPE_STREAMEVENT;
            break;
        default:
            info->type = MPE_FS_TYPE_UNKNOWN;
            break;
        }
        break;
    case MPE_FS_STAT_IS_VIDEO:
        retCode = mpe_ocIsStreamType(handle->ocFile, StreamType_Video,
                (mpe_Bool *) &(info->hasType));
        break;
    case MPE_FS_STAT_IS_AUDIO:
        retCode = mpe_ocIsStreamType(handle->ocFile, StreamType_Audio,
                (mpe_Bool *) &(info->hasType));
        break;
    case MPE_FS_STAT_IS_DATA:
        retCode = mpe_ocIsStreamType(handle->ocFile, StreamType_Data,
                &(info->hasType));
        break;
    case MPE_FS_STAT_DURATION:
        retCode = mpe_ocGetStreamDuration(handle->ocFile, &info->duration);
        break;
    case MPE_FS_STAT_TUNING_INFO:
        retCode = mpe_ocGetTuningInfo(handle->ocFile, &info->freq, &info->prog,
                &info->qam, &info->sourceId);
        break;
    case MPE_FS_STAT_ISCURRENT:
        retCode = mpe_ocIsCurrent(handle->ocFile, &info->isCurrent);
        break;
    case MPE_FS_STAT_MODDATE:
        retCode = mpe_ocGetVersion(handle->ocFile, &tempUint);
        info->modDate = (mpe_Time) tempUint;
        break;
    case MPE_FS_STAT_SIHANDLE:
        retCode = mpe_ocGetSIHandle(handle->ocFile, &info->siHandle);
        break;
    case MPE_FS_STAT_CONTENTTYPE:
        retCode = mpe_ocGetContentType(handle->ocFile, info->size, info->buf);
        break;
    case MPE_FS_STAT_CREATEDATE:
        retCode = MPE_FS_ERROR_UNSUPPORT;
        break;
    default:
        retCode = MPE_FS_ERROR_INVALID_PARAMETER;
        break;
    }
    return retCode;
}

/**
 * Open a file and do a get stat on it.
 */
static mpe_FileError ocfsOpenGetFStat(const char* name, mpe_FileStatMode mode,
        mpe_FileInfo *info)
{
    mpe_FileOC *handle;
    mpe_FileError retCode;

    // Open the file.
    retCode = ocfsOpen(name, false, Cache_StreamOrCache, &handle);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCFS: Could not find file %s\n", name);
        return retCode;
    }
    // Check the file type
    retCode = ocfsFileGetFStat((mpe_File) handle, mode, info);

    // Close the file
    ocfsFileClose(handle);
    return retCode;
}

/**
 * <i>ocfsFileGetStat()</i>
 *
 * Get the status for the open file.
 *
 * @param name The pathlist to the file of which to obtain the status.
 * @param mode The specific status of the file to return (see MPE_FS_STAT_xxx).
 * @param info A pointer to the buffer in which to receive the indicated file stat data.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError ocfsFileGetStat(const char* name, mpe_FileStatMode mode,
        mpe_FileInfo *info)
{
    mpe_FileError retCode = MPE_SUCCESS;
    char *filename;
    uint32_t ocNumber;
    mpe_ObjectCarousel *oc;
    mpe_OcDirEntry *dirEntry;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "OCFS: ocfsFileGetStat %s 0x%04x\n", name, (int) mode);

    switch (mode)
    {
    case MPE_FS_STAT_TARGET_INFO:
        oc = findCarousel(name, &filename, &ocNumber);
        if (oc == NULL)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "OCFS: Carousel not found: %s\n", name);
            return MPE_FS_ERROR_NOT_FOUND;
        }
        retCode = mpe_ocResolveFilename(oc, filename, (uint32_t) info->size,
                info->buf, info->nsap);
        break;
    case MPE_FS_STAT_MODDATE:
    case MPE_FS_STAT_SIZE:
    case MPE_FS_STAT_TYPE:
    case MPE_FS_STAT_ISKNOWN:
    case MPE_FS_STAT_CONTENTTYPE:
        // Get the carousel
        oc = findCarousel(name, &filename, &ocNumber);
        if (oc == NULL)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "OCFS: Carousel not found: %s\n", name);
            return MPE_FS_ERROR_NOT_FOUND;
        }
        // And the directory entry.
        // TODO: ocGetDirectoryEntry needs a cache mode parameter which should be passed
        // down so that we can not fetch the data from the carousel if we don't want it.
        // Kinda like openFile
        retCode = mpe_ocGetDirectoryEntry(oc, filename, &dirEntry);
        if (retCode != MPE_SUCCESS)
        {
            return retCode;
        }
        switch (mode)
        {
        case MPE_FS_STAT_MODDATE:
            info->modDate = dirEntry->version;
            break;
        case MPE_FS_STAT_SIZE:
            info->size = (uint64_t) dirEntry->size;
            break;
        case MPE_FS_STAT_TYPE:
            switch (dirEntry->fileType)
            {
            case Type_File:
                info->type = MPE_FS_TYPE_FILE;
                break;
            case Type_Directory:
                info->type = MPE_FS_TYPE_DIR;
                break;
            case Type_Stream:
                info->type = MPE_FS_TYPE_STREAM;
                break;
            case Type_StreamEvent:
                info->type = MPE_FS_TYPE_STREAMEVENT;
                break;
            default:
                info->type = MPE_FS_TYPE_UNKNOWN;
                break;
            }
            break;
        case MPE_FS_STAT_ISKNOWN:
            info->isKnown = true;
            break;
        case MPE_FS_STAT_CONTENTTYPE:
            if (info->size == 0 || info->buf == NULL)
            {
                retCode = MPE_EINVAL;
            }
            else
            {
                // If it's specified in the directory entry, retrieve it
                if (dirEntry->mimeType != NULL)
                {
                    strncpy(info->buf, dirEntry->mimeType, info->size);
                }
                else
                {
                    // Nothing specified in the directory entry. It might be in the file object.
                    // Check there.
                    retCode = ocfsOpenGetFStat(name, mode, info);
                }
            }
        }
        mpe_ocFreeDirectoryEntry(dirEntry);
        break;
    default:
        retCode = ocfsOpenGetFStat(name, mode, info);
        break;
    }

    return retCode;
}

/**
 * <i>ocfsFileSetFStat()</i>
 *
 * Set the status for the open file.
 *
 * @param h An mpe_File handle, previously returned by <i>ocfsFileOpen()</i>.
 * @param mode The specific status of the file to set (see MPE_FS_STAT_xxx).
 * @param info A pointer to the buffer with which to set the indicated file stat data.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError ocfsFileSetFStat(mpe_File h, mpe_FileStatMode mode,
        mpe_FileInfo *info)
{
    MPE_UNUSED_PARAM(mode);
    MPE_UNUSED_PARAM(info);
    MPE_UNUSED_PARAM(h);

    return MPE_FS_ERROR_INVALID_PARAMETER;
}

/**
 * <i>ocfsFileSetStat()</i>
 *
 * Set the status for the open file.
 *
 * @param name The pathlist to the file of which to obtain the status.
 * @param mode The specific status of the file to set (see MPE_FS_STAT_xxx).
 * @param info A pointer to the buffer with which to set the indicated file stat data.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError ocfsFileSetStat(const char* name, mpe_FileStatMode mode,
        mpe_FileInfo *info)
{
    mpe_FileError retCode = MPE_FS_ERROR_SUCCESS;
    mpe_FileOC *handle;
    mpe_ObjectCarousel *oc;
    char *lName;
    uint32_t ocNumber;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "OCFS: In fileSetStat(%s, %04x, %p)\n", name, mode, info);

    if (mode == MPE_FS_STAT_SIHANDLE)
    {
        if (info == NULL)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "OCFS: Null Info structure or SI Handle to ocfsFileSetStat");
            return MPE_EINVAL;
        }
        oc = findCarousel(name, &lName, &ocNumber);
        if (oc == NULL)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "OCFS: Carousel not found: %s\n", name);
            return MPE_FS_ERROR_NOT_FOUND;
        }
        (void) mpe_ocSetSIHandle(oc, info->siHandle);
    }
    else
    {
        // Open the file.
        retCode = ocfsFileOpen(name, MPE_FS_OPEN_READ, (mpe_File *) (&handle));
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "OCFS: Could not find file %s\n", name);
            return retCode;
        }
        // Check the file type
        retCode = ocfsFileSetFStat((mpe_File) handle, mode, info);

        // Close the file
        ocfsFileClose(handle);
    }

    return retCode;
}

static mpe_FileError ocfsPrefetchModule(const char *mountPoint,
        const char *moduleName)
{
    mpe_ObjectCarousel *oc;
    char *lName;
    uint32_t ocNumber;

    // Find the carousel
    oc = findCarousel(mountPoint, &lName, &ocNumber);
    if (oc == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCFS: Carousel not found: %s\n", mountPoint);
        return MPE_FS_ERROR_NOT_FOUND;
    }

    return mpe_ocPrefetchModule(oc, moduleName);
}

static mpe_FileError ocfsDIILocation(const char *mountPoint,
        uint16_t diiIdentification, uint16_t associationTag)
{
    mpe_ObjectCarousel *oc;
    char *lName;
    uint32_t ocNumber;

    // Find the carousel
    oc = findCarousel(mountPoint, &lName, &ocNumber);
    if (oc == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCFS: Carousel not found: %s\n", mountPoint);
        return MPE_FS_ERROR_NOT_FOUND;
    }

    return mpe_ocAddDII(oc, diiIdentification, associationTag);
}

/**
 * <i>ocfsFileSync()</i>
 *
 * Synchronizes the contents of a file in persistent storage.  This will write any data that is
 *  pending.  Pending data is data that has been written to a file, but which hasn't been flushed
 *  to the storage device yet.
 *
 * @param h A ocfsFile handle, previously returned by <i>ocfsFileOpen()</i>.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError ocfsFileSync(mpe_File h)
{
    MPE_UNUSED_PARAM(h);

    /* always sync'ed */
    return MPE_FS_ERROR_SUCCESS;
}

/**
 * <i>ocfsFileDelete()</i>
 *
 * Deletes the specific file from the persistent storage file system.
 *
 * @param name The path to the file to delete.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError ocfsFileDelete(const char* name)
{
    MPE_UNUSED_PARAM(name);
    return MPE_FS_ERROR_READ_ONLY;
}

/**
 * <i>ocfsFileRename()</i>
 *
 * Renames or moves the specific file in persistent storage.
 *
 * @param old_name The path to the file to rename or move.
 * @param new_name The new path and/or name for the file.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError ocfsFileRename(const char* old_name, const char* new_name)
{
    MPE_UNUSED_PARAM(old_name);
    MPE_UNUSED_PARAM(new_name);
    return MPE_FS_ERROR_READ_ONLY;
}

/**
 * <i>ocfsDirOpen()</i>
 *
 * Opens a directory in persistent storage.
 *
 * @param name The path to the directory to open.
 * @param h A pointer to an ocfsDir handle, through which the opened directory is returned.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError ocfsDirOpen(const char* path, mpe_Dir *h)
{
    mpe_Error retCode;
    mpe_OcFileType filetype;
    mpe_FileOC *handle;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OCFS: DirOpen %s\n", path);
    retCode = ocfsOpen(path, false, Cache_StreamOrCache, (mpe_FileOC **) (h));

    // If the file opened
    if (retCode == MPE_SUCCESS)
    {
        handle = (mpe_FileOC *) *h;
        // Make sure it's a file
        retCode = mpe_ocGetFileType(handle->ocFile, &filetype);
        if (retCode != MPE_SUCCESS || filetype != Type_Directory)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "OCFS: %s: Not a directory\n", path);
            ocfsFileClose(*h);
            *h = NULL;
            return MPE_FS_ERROR_INVALID_TYPE;
        }
    }
    return retCode;
}

/**
 * <i>ocfsDirRead()</i>
 *
 * Reads the contents of a directory in persistent storage.  This can be used to iterate
 *  through the contents a directory in persistent storage.
 *
 * @param h A ocfsDir handle, previously returned by <i>ocfsDirOpen()</i>.
 * @param dirEnt A pointer to a ocfsDirEntry object.  On return, this will contain
 *                  data about a directory entry.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError ocfsDirRead(mpe_Dir h, mpe_DirEntry *dirEnt)
{
    mpe_FileError retCode;
    mpe_FileOC *handle = (mpe_FileOC *) h;
    mpe_OcDirEntry *ocDirEntry;

    // Check the params
    if (handle == NULL || dirEnt == NULL)
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OCFS: DirRead %d\n",
            (int32_t) handle->readLoc);

    // Check for EOF condition
    if (handle->readLoc >= handle->ocFile->length)
    {
        return MPE_FS_ERROR_EOF;
    }

    retCode = mpe_ocReadDirectoryEntry(handle->ocFile,
            (uint32_t) handle->readLoc, &ocDirEntry);
    if (retCode != MPE_SUCCESS)
    {
        return retCode;
    }

    // Copy the name
    strncpy(dirEnt->name, ocDirEntry->name, MPE_FS_MAX_PATH);
    // Make sure it's null terminated, in case the internal name was too long
    dirEnt->name[MPE_FS_MAX_PATH] = '\0';

    // Fill in the other fields
    dirEnt->fileSize = (uint64_t) ocDirEntry->size;
    dirEnt->isDir = (ocDirEntry->fileType == Type_Directory);

    // increment the read pointer.
    handle->readLoc++;
    mpe_ocFreeDirectoryEntry(ocDirEntry);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OCFS: DirRead done: %s %d %d\n",
            dirEnt->name, (int32_t) dirEnt->fileSize, (int32_t) dirEnt->isDir);

    return retCode;
}

/**
 * <i>ocfsDirClose()</i>
 *
 * Closes a directory in persistent storage.
 *
 * @param h A ocfsDir handle, previously returned by <i>ocfsDirOpen()</i>.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError ocfsDirClose(mpe_Dir h)
{
    return ocfsFileClose((mpe_File) h);
}

/**
 * <i>ocfsDirDelete()</i>
 *
 * Deletes a directory from persistent storage.
 *
 * @param name The path to the directory to delete.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError ocfsDirDelete(const char* name)
{
    MPE_UNUSED_PARAM(name);
    return MPE_FS_ERROR_READ_ONLY;
}

/**
 * <i>ocfsDirRename()</i>
 *
 * Renames or moves the specific directory in persistent storage.
 *
 * @param old_name The path to the directory to rename or move.
 * @param new_name The new path and/or name for the directory.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError ocfsDirRename(const char *old_name, const char *new_name)
{
    MPE_UNUSED_PARAM(old_name);
    MPE_UNUSED_PARAM(new_name);
    return MPE_FS_ERROR_READ_ONLY;
}

/**
 * <i>ocfsDirCreate()</i>
 *
 * Creates the specific directory in persistent storage.
 *
 * @param name The path to the directory to create.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError ocfsDirCreate(const char* name)
{
    MPE_UNUSED_PARAM(name);
    return MPE_FS_ERROR_READ_ONLY;
}

/**
 * <i>ocfsStreamOpen()</i>
 *
 * Opens a Stream object in persistent storage.
 *
 * @param name The path to the stream to open.
 * @param h A pointer to an ocfsStream handle, through which the opened stream is returned.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError ocfsStreamOpen(const char* path, mpe_Stream *h)
{
    mpe_Error retCode;
    mpe_OcFileType filetype;
    mpe_FileOC *handle;

    *h = NULL;

    retCode = ocfsOpen(path, false, Cache_StreamOrCache, (mpe_FileOC **) (h));

    // If the file opened
    if (retCode == MPE_SUCCESS)
    {
        handle = (mpe_FileOC *) *h;
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "OCFS: Got a file.  Making sure it's a stream\n");
        // Make sure it's a Stream
        retCode = mpe_ocGetFileType(handle->ocFile, &filetype);
        if (retCode != MPE_SUCCESS || (filetype != Type_Stream && filetype
                != Type_StreamEvent))
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS, "OCFS: %s: Not a stream\n",
                    path);
            ocfsFileClose(*h);
            *h = NULL;
            return MPE_FS_ERROR_INVALID_TYPE;
        }
    }
    return retCode;
}

/**
 * <i>ocfsStreamClose()</i>
 *
 * Closes a stream in persistent storage.
 *
 * @param h A ocfsStream handle, previously returned by <i>ocfsStreamOpen()</i>.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError ocfsStreamClose(mpe_Stream h)
{
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OCFS: Stream Close\n");
    return ocfsFileClose((mpe_File) h);
}

/**
 * Reads the contents of a StreamEvent previously opened with <i>mpe_streamOpen</i>.
 * This can be used to iterate through all the events.
 * Will return no data if this is a stream, and not a stream event.
 *
 * @param h             A mpe_Stream handle, previously returned by <i>mpe_DirOpen()</i>.
 * @param event         A pointer to a mpe_StreamEventInfo object.  On return, this will contain
 *                      data about a event description.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError ocfsStreamReadEvent(mpe_Stream h,
        mpe_StreamEventInfo *event)
{
    mpe_FileError retCode;
    mpe_FileOC *handle = (mpe_FileOC *) h;
    mpe_OcStreamEventEntry *ocEvent;

    // Check the params
    if (handle == NULL || event == NULL)
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    // Check for EOF condition
    if (handle->readLoc >= handle->ocFile->length)
    {
        return MPE_FS_ERROR_EOF;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OCFS: Reading stream event %d\n",
            (uint32_t) handle->readLoc);
    retCode = mpe_ocReadStreamEventEntry(handle->ocFile,
            (uint32_t) handle->readLoc, &ocEvent);
    if (retCode != MPE_SUCCESS)
    {
        return retCode;
    }

    // Copy the name
    strncpy(event->name, ocEvent->name, MPE_FS_MAX_PATH);
    // Make sure it's null terminated, in case the internal name was too long
    event->name[MPE_FS_MAX_PATH] = '\0';

    // Fill in the other fields
    event->eventId = ocEvent->eventID;

    // increment the read pointer.
    handle->readLoc++;
    mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, ocEvent);

    return retCode;
}

/**
 * Read a TAP tag out of a stream or stream event object.
 *
 * @param h             A mpe_Stream handle, previously returned by <i>mpe_DirOpen()</i>.
 * @param tapType       The type of tap to look for.  (MPE_OC_ALL_TAPS means all TAPS)
 * @param tapNumber     The instance of this tap to read.  0 = first tap.
 * @param tap           [out] pointer to a tap object where to fill with the tap
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError ocfsStreamReadTap(mpe_Stream h, uint16_t tapType,
        uint32_t tapNumber, uint16_t *tapTag, uint16_t *tapId)
{

    mpe_FileOC *handle = (mpe_FileOC *) h;

    // Check the params
    if (handle == NULL || tapTag == NULL || tapId == NULL)
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }
    return mpe_ocGetTap(handle->ocFile, tapType, tapNumber, tapTag, tapId);
}

/**
 * Read the number of taps of a certain type (or all types) in a stream object.
 *
 * @param h             A mpe_Stream handle, previously returned by <i>mpe_DirOpen()</i>.
 * @param tapType       The type of tap to look for.  (MPE_OC_ALL_TAPS means all TAPS)
 * @param numTaps       The number of taps
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError ocfsStreamGetNumTaps(mpe_Stream h, uint16_t tapType,
        uint32_t *numTaps)
{

    mpe_FileOC *handle = (mpe_FileOC *) h;

    // Check the params
    if (handle == NULL || numTaps == NULL)
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }
    return mpe_ocGetNumTaps(handle->ocFile, tapType, numTaps);
}

/*
 * Preliminary support for OC mount and OC unmount status events.
 */
mpe_Error ocRegisterEvent(mpe_DbgStatusId, mpe_DbgStatusFormat, void *param);
mpe_Error ocUnregisterEvent(mpe_DbgStatusId, mpe_DbgStatusFormat);
mpe_Error ocGetStatus(mpe_DbgStatusId, mpe_DbgStatusFormat, uint32_t *, void *,
        void*);
mpe_Bool ocStatusMountRegistered = FALSE;
mpe_Bool ocStatusUnmountRegistered = FALSE;
char ocStat[256];

/**
 * Register for OC mount or OC unmount events.  This function is called from the "dbgmgr"
 * when as status consumer wishes to register for OC mount and unmount events.
 *
 * @param typeId is the status type identifier.
 * @param format is the request status format.
 * @param param is the optional parameter to the registration.
 *
 * @return MPE_SUCCESS if the registration completed successfully.
 */
mpe_Error ocRegisterEvent(mpe_DbgStatusId typeId, mpe_DbgStatusFormat format,
        void *param)
{
    switch (typeId)
    {
    case MPE_DBG_STATUS_OC_MOUNT_EVENT:
        ocStatusMountRegistered = TRUE;
        break;
    case MPE_DBG_STATUS_OC_UNMOUNT_EVENT:
        ocStatusUnmountRegistered = TRUE;
        break;
    default:
        return MPE_EINVAL;
    }
    return MPE_SUCCESS;
}

/**
 * Deregister from OC mount and OC unmount events.
 *
 * @param typeId is the status type identifier of the original registration.
 * @param format is the format of the status of the original registration.
 *
 * @return MPE_SUCCESS if successfully unregistered.
 */
mpe_Error ocUnregisterEvent(mpe_DbgStatusId typeId, mpe_DbgStatusFormat format)
{
    switch (typeId)
    {
    case MPE_DBG_STATUS_OC_MOUNT_EVENT:
        ocStatusMountRegistered = FALSE;
        break;
    case MPE_DBG_STATUS_OC_UNMOUNT_EVENT:
        ocStatusUnmountRegistered = FALSE;
        break;
    default:
        return MPE_EINVAL;
    }
    return MPE_SUCCESS;
}

/**
 * <i>mpe_FileError ocfsDirMount(const mpe_DirUrl *dirUrl);</i>
 *
 * Mount the indicated URL on the optional (non '-1) indicated persistent id
 * into the object persistent file-system namespace.
 *
 * @param dirUrl The URL of the directory to mount.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError ocfsDirMount(const mpe_DirUrl *dirUrl)
{
    int ocNumber;
    mpe_ObjectCarousel *oc;
    mpe_Error retCode;
    char *temp;

#if 0 /* FIXME: disabled to workaround bug 4770 */
#ifdef PTV /* PowerTV only stuff */
    OC_mount *pOC = &gOC.mountPoints_array[gOC.mountPoint_cnt];
#endif /* PTV */
#endif /* disabled to workaround bug 4770 */

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "OCFS: Attempting to mount %s:%d:%d\n", dirUrl->url,
            dirUrl->siHandle, dirUrl->carouselId);

    if (strncmp(URL_OC_PREFIX, dirUrl->url, (sizeof(URL_OC_PREFIX) - 1)) == 0)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OCFS: Mounting %s:%d:%d\n",
                dirUrl->url, dirUrl->siHandle, dirUrl->carouselId);
        // This is one that we like, let's do something with it.
        ocNumber = getOpenMount();
        if (ocNumber == NO_MOUNTS_AVAILABLE)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "OCFS: Too many mounts, unable to mount %s\n", dirUrl->url);
            return MPE_FS_ERROR_NO_MOUNT;
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "OCFS: Mounting %s:%d:%d in location %d\n", dirUrl->url,
                    dirUrl->siHandle, dirUrl->carouselId, ocNumber);

            // Copy the URL into the carousel
            retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, (strlen(
                    dirUrl->url) + 1) * sizeof(char),
                    (void **) &(mountPoints[ocNumber].url));
            if (retCode != MPE_SUCCESS)
            {
                MPE_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_FILESYS,
                        "OCFS: Unable to allocate name for mountpoint %d: %s:%d\n",
                        ocNumber, dirUrl->url, dirUrl->carouselId);
                mountPoints[ocNumber].inUse = false;
                return retCode;
            }
            strcpy(mountPoints[ocNumber].url, dirUrl->url);
            // Now, set a pointer to past the "ocap:" part.
            temp = &mountPoints[ocNumber].url[strlen(URL_OC_PREFIX)];
            // Now, skip any slashes
            while (*temp == PATH_SEPARATOR)
            {
                temp++;
            }
            // And save this name away.
            mountPoints[ocNumber].mountName = temp;

            // And ignore anything past the
            // Note, this mangles the mountpoint string as well.
            temp = strchr(mountPoints[ocNumber].mountName, PATH_SEPARATOR);
            if (temp != NULL)
            {
                MPE_LOG(
                        MPE_LOG_INFO,
                        MPE_MOD_FILESYS,
                        "OCFS: Mounted pathname %s includes a filename component (ie, a '/')",
                        mountPoints[ocNumber].mountName);
                *temp = '\0';
            }

            // Now, actually Mount the carousel
            mountPoints[ocNumber].siHandle = dirUrl->siHandle;
            mountPoints[ocNumber].carouselId = dirUrl->carouselId;
            retCode = mpe_ocMount(dirUrl->siHandle, dirUrl->carouselId, &oc);
            if (retCode != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                        "OCFS: Mounting of %s:%d failed: %04x\n", dirUrl->url,
                        dirUrl->carouselId, retCode);
                mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, mountPoints[ocNumber].url);
                mountPoints[ocNumber].url = NULL;
                mountPoints[ocNumber].siHandle = 0;
                mountPoints[ocNumber].carouselId = 0;
                mountPoints[ocNumber].inUse = false;
                return retCode;
            }
            mountPoints[ocNumber].oc = oc;

            /* Check for registered OC mounts status listener. */
            if (ocStatusMountRegistered == TRUE)
            {
                /* Formulate status as a string. */
                sprintf(
                        ocStat,
                        "OC Mount complete: cnt=%d, url = %s, id = %d, nsap = %x%x%x%x%x, oob = %d\n",
                        (int) ocNumber, dirUrl->url, (int) dirUrl->carouselId,
                        (int) *((int*) oc->nsapAddress),
                        (int) *((int*) oc->nsapAddress + 1),
                        (int) *((int*) oc->nsapAddress + 2),
                        (int) *((int*) oc->nsapAddress + 3),
                        (int) *((int*) oc->nsapAddress + 4), dc_IsOOB(
                                oc->gatewayDC));

                /* Deliver the status event. */
                (void) mpe_dbgStatusDeliverEvent(MPE_DBG_STATUS_ID(
                        MPE_MOD_FILESYS, MPE_DBG_STATUS_OC_MOUNT_EVENT),
                        MPE_DBG_STATUS_FMT_STRING, ocStat);
            }

            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "OCFS: Mounting %s:%d in location %d complete\n",
                    dirUrl->url, dirUrl->carouselId, ocNumber);
        }

        return MPE_FS_ERROR_SUCCESS;
    }
    else
    {
        return MPE_FS_ERROR_UNKNOWN_URL;
    }
}

/**
 * <i>mpe_FileError ocfsDirUnmount(const mpe_DirUrl *dirUrl);</i>
 *
 * Unmount the indicated URL from the object persistent file-system namespace.
 *
 * @param dirUrl The URL of the directory to mount.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError ocfsDirUnmount(const mpe_DirUrl *dirUrl)
{
    int i;
    mpe_ObjectCarousel *oc;

    /* check for supported url */
    if (strncmp(URL_OC_PREFIX, dirUrl->url, (sizeof(URL_OC_PREFIX) - 1)) == 0)
    {
        for (i = 0; i < MAX_OC_MOUNTS; i++)
        {
            // Search for the URL
            // BUG: FIXME: HACK: TODO:
            // Does this need to be synchronized?
            // Probably.  This whole structure needs to be reworked wrt the way that MPE mounts and
            // unmounts carousels.
            if ((mountPoints[i].inUse) && (mountPoints[i].url != NULL)
                    && (strcmp(mountPoints[i].url, dirUrl->url) == 0)
                    && (mountPoints[i].siHandle == dirUrl->siHandle)
                    && (mountPoints[i].carouselId == dirUrl->carouselId))
            {
                MPE_LOG(
                        MPE_LOG_DEBUG,
                        MPE_MOD_FILESYS,
                        "OCFS: Unmounting object carousel in %s:%d:%d in location %d\n",
                        dirUrl->url, dirUrl->siHandle, dirUrl->carouselId, i);

                // BEGIN CRITICAL SECTION
                mpe_mutexAcquire(ocfsGlobalMutex);

                // Take the carousel out of the structure, and
                oc = mountPoints[i].oc;
                mountPoints[i].oc = NULL;

                // Clear out the mount point
                mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, mountPoints[i].url);
                mountPoints[i].url = NULL;
                mountPoints[i].siHandle = 0;
                mountPoints[i].carouselId = 0;

                // Not using it any longer, mark as not in use
                mountPoints[i].inUse = false;

                mpe_mutexRelease(ocfsGlobalMutex);
                // END CRITICAL SECTION

                if (ocStatusUnmountRegistered == TRUE)
                {
                    /* Formulate status as a string. */
                    sprintf(
                            ocStat,
                            "OC unmount complete: cnt=%d, url = %s, id = %d, nsap = %x%x%x%x%x, oob = %d\n",
                            (int) i, dirUrl->url, (int) dirUrl->carouselId,
                            (int) *((int*) oc->nsapAddress),
                            (int) *((int*) oc->nsapAddress + 1),
                            (int) *((int*) oc->nsapAddress + 2),
                            (int) *((int*) oc->nsapAddress + 3),
                            (int) *((int*) oc->nsapAddress + 4), dc_IsOOB(
                                    oc->gatewayDC));

                    (void) mpe_dbgStatusDeliverEvent(MPE_DBG_STATUS_ID(
                            MPE_MOD_FILESYS, MPE_DBG_STATUS_OC_UNMOUNT_EVENT),
                            MPE_DBG_STATUS_FMT_STRING, ocStat);
                }

                // Now, do the long latency operations on the things we've allocated above.
                (void) mpe_ocUnmount(oc);

                return MPE_FS_ERROR_SUCCESS;
            }
        }
        // Didn't find it, return error
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCFS: Couldn't find object carousel %s:%d:%d to unmount\n",
                dirUrl->url, dirUrl->siHandle, dirUrl->carouselId);
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }
    else
    {
        return MPE_FS_ERROR_UNKNOWN_URL;
    }
}

/**
 * <i>mpe_FileError ocfsDirGetUStat(const mpe_DirUrl *dirUrl, mpe_DirStatMode mode, mpe_DirInfo *info);</i>
 *
 * Retrieve some status info on a directory object.
 *
 * @param dirUrl The URL to the directory on which to update its status information.
 * @param mode The specific directory stat to get.
 * @param info A pointer to the buffer in which to return the indicated directory stat info.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError ocfsDirGetUStat(const mpe_DirUrl *dirUrl,
        mpe_DirStatMode mode, mpe_DirInfo *info)
{
    int i;
    mpe_FileError retCode;
    mpe_Bool connected = false;
    /* check for supported url */
    if (strncmp(URL_OC_PREFIX, dirUrl->url, (sizeof(URL_OC_PREFIX) - 1)) == 0)
    {
        retCode = MPE_FS_ERROR_INVALID_PARAMETER;

        // BEGIN CRITICAL SECTION
        mpe_mutexAcquire(ocfsGlobalMutex);

        for (i = 0; i < MAX_OC_MOUNTS; i++)
        {
            // Search for the URL
            if (mountPoints[i].inUse && (mountPoints[i].siHandle
                    == dirUrl->siHandle) && (mountPoints[i].carouselId
                    == dirUrl->carouselId))
            {
                switch (mode)
                {
                case MPE_FS_STAT_MOUNTPATH:
                    // Append the prexisting chunk
                    strcat(info->path, "/");
                    strcat(info->path, mountPoints[i].mountName);
                    retCode = MPE_FS_ERROR_SUCCESS;
                    break;
                case MPE_FS_STAT_CONNECTIONAVAIL:
                    retCode = mpe_ocIsConnected(mountPoints[i].oc, &connected);
                    info->isConnectAvail = (uint8_t) connected;
                    break;
                default:
                    retCode = MPE_FS_ERROR_INVALID_PARAMETER;
                    break;
                }
                // Matched.  Break out of the loop.
                break;
            }
        }
        mpe_mutexRelease(ocfsGlobalMutex);
        // END CRITICAL SECTION

        // Return whatever code we've got so far.
        return retCode;
    }
    else
    {
        return MPE_FS_ERROR_UNKNOWN_URL;
    }
}

/**
 * <i>mpe_FileError ocfsDirSetUStat(const mpe_DirUrl *dirUrl, mpe_DirStatMode mode, mpe_DirInfo *info);</i>
 *
 * Set some status info on a directory object.
 *
 * @param dirUrl The URL to the directory on which to update its status information.
 * @param mode The specific directory stat to get.
 * @param info A pointer to the buffer in which to return the indicated directory stat info.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError ocfsDirSetUStat(const mpe_DirUrl *dirUrl,
        mpe_DirStatMode mode, mpe_DirInfo *info)
{
    MPE_UNUSED_PARAM(mode);
    MPE_UNUSED_PARAM(info);

    /* check for supported url */
    if (strncmp(URL_OC_PREFIX, dirUrl->url, (sizeof(URL_OC_PREFIX) - 1)) == 0)
    {
        /* OC url */
        return MPE_FS_ERROR_UNSUPPORT;
    }
    else
    {
        return MPE_FS_ERROR_UNKNOWN_URL;
    }
}

/**
 * Thread function.
 * Waits around and handles events from the object/data carousel which indicate that
 * something in the carousel has changed.
 */
static
void ocfsThreadMain(void *data)
{
    mpe_Event eventID;
    void *eventData;
    ocfsFileVersion *check;
    uint32_t newVersion;
    mpe_Bool running = true;
    mpe_ObjectCarousel *oc;

    MPE_UNUSED_PARAM(data);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OCFS: Watcher thread started\n");

    // Do forever
    while (running)
    {
        (void) mpe_eventQueueWaitNext(ocfsEventQueue, &eventID, &eventData,
                NULL, NULL, 0);
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_FILESYS,
                "OCFS: Watcher thread got Event from Global Queue: %04x %p\n",
                eventID, eventData);

        switch (eventID)
        {
        case MPE_DC_UPDATE_EVENT:
            oc = (mpe_ObjectCarousel *) eventData;

            // BUG: HACK: FIXME: TODO:
            // NEED a better locking strategy.  A MUCH better locking strategy
            // BEGIN CRITICAL SECTION
            mpe_mutexAcquire(ocfsGlobalMutex);

            check = mpe_ocGetUserData(oc);
            // Loop here.
            // Make sure that the mountPoint is still inUse (ie, mounted) while we attempt to check
            // each carousel.  If it goes to false, the carousel is unmounted.
            // Note, with the above synchronization, we're basically screwed anyhow.
            while (check != NULL)
            {
                MPE_LOG(
                        MPE_LOG_DEBUG,
                        MPE_MOD_FILESYS,
                        "OCFS: Checking version on file %s.  Old version %d %p\n",
                        check->name, check->version, oc);
                newVersion = getFileVersion(oc, check->name);
                if (newVersion != check->version)
                {
                    // Version changed.
                    MPE_LOG(
                            MPE_LOG_DEBUG,
                            MPE_MOD_FILESYS,
                            "OCFS: Version on %s changed.  New Version %d.  Old Version %d\n",
                            check->name, newVersion, check->version);
                    if (check->evQueueRegistered)
                    {
                        /* send up the new version number as the event code */
                        mpeos_eventQueueSend(check->evQueue, newVersion, NULL,
                                check->act, 0);
                    }

                    // Save the current version
                    check->version = newVersion;
                }
                else
                {
                    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                            "OCFS: Version on %s unchanged at %d\n",
                            check->name, check->version);
                }
                check = check->next;
            }

            mpe_mutexRelease(ocfsGlobalMutex);
            // END CRITICAL SECTION
            break;
        case OCFS_CHECK_QUIT:
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "OCFS: Got Check for quiting event\n");
            // BEGIN CRITICAL SECTION
            mpe_mutexAcquire(ocfsGlobalMutex);

            // If no more watched files, let's quit
            if (watchedFiles == 0)
            {
                running = false; // Stop the loop
                ocfsWatcherThreadId = 0; // Tell other's that we're toast.
            }

            mpe_mutexRelease(ocfsGlobalMutex);
            // END CRITICAL SECTION
            break;
        default:
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                    "OCFS: Unexpected event: %04x\n", eventID);
            break;
        }
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OCFS: Watcher Thread quitting\n");
}

/*
 * The function table for Object Carousel Files.
 */
mpeos_filesys_ftable_t mpe_fileOCFTable =
{ ocfsInit, ocfsFileOpen, ocfsFileClose, ocfsFileRead, ocfsFileWrite,
        ocfsFileSeek, ocfsFileSync, ocfsFileGetStat, ocfsFileSetStat,
        ocfsFileGetFStat, ocfsFileSetFStat, ocfsFileDelete, ocfsFileRename,
        ocfsDirOpen, ocfsDirRead, ocfsDirClose, ocfsDirDelete, ocfsDirRename,
        ocfsDirCreate, ocfsDirMount, ocfsDirUnmount, ocfsDirGetUStat,
        ocfsDirSetUStat, ocfsStreamOpen, ocfsStreamClose, ocfsStreamReadEvent,
        ocfsStreamGetNumTaps, ocfsStreamReadTap, ocfsFilePrefetch,
        ocfsPrefetchModule, ocfsDIILocation, ocfsSetChangeListener,
        ocfsRemoveChangeListener, };
