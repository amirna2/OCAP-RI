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

#include <stdio.h>
#include <malloc.h>
#include <mpe_types.h>
#include <mpeos_mem.h>
#include <mpe_error.h>
#include <mpeos_dbg.h>

/* PORT095 */

/* PORT095 */
// static void memtkCommand(eMon_Why, Mon_InputFunc, Mon_OutputFunc, ui8 *);
#ifdef MPE_FEATURE_MEM_PROF
static const char *color2String( mpe_MemColor );
static const char *extractFilename (const char *);
static void addMemInTrackList(void*, char*, int);
static void removeMemInTrackList(void*);
#endif
#ifdef NEW_REALLOC
static void* memRealloc(Mem_Pointer, uint32_t);
#endif

/* PORT095 */
mpe_Error mpeos_memAllocPProf(mpe_MemColor color, uint32_t size, void **memory,
        char* fileName, uint32_t lineNum)
{
    return MPE_SUCCESS;
}
mpe_Error mpeos_memFreePProf(mpe_MemColor color, void *memory, char* fileName,
        uint32_t lineNum)
{
    return MPE_SUCCESS;
}
mpe_Error mpeos_memReallocPProf(mpe_MemColor color, uint32_t size,
        void **memory, char* fileName, uint32_t lineNum)
{
    return MPE_SUCCESS;
}
mpe_Error mpeos_memUnregisterMemFreeCallback(mpe_MemColor color,
        int32_t(*function)(mpe_MemColor, int32_t, int64_t, void *),
        void *context)
{
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_memAlloc()</i>
 *
 * Allocate a block of system memory of the specified size. The address of
 * the memory block allocated is returned via the pointer.
 *
 * @param size is the size of the memory block to allocate.
 * @param mem is a pointer for returning the pointer to the newly allocated
 *        memory block
 *
 * @return The MPE error code if allocation fails, otherwise <i>MPE_SUCCESS</i>
 *         is returned.
 */
mpe_Error mpeos_memAllocPGen(mpe_MemColor color, uint32_t size, void **mem)
{
    mpe_Error retval = MPE_SUCCESS;

    if ((MPE_MEM_NCOLORS < color) || (NULL == mem) || (0 == size))
    {
        retval = MPE_EINVAL;
    }
    else
    {
        if ((*mem = malloc(size)) == NULL)
        {
            retval = MPE_ENOMEM;
        }
    }
    return (retval);
}

/**
 * <i>mpeos_memFreeP()</i>
 *
 * Free the specified block of system memory.
 *
 * @param mem is a pointer to the memory block to free.
 *
 * @return The MPE error code if free fails, otherwise <i>MPE_SUCCESS</i>
 *         is returned.
 */
mpe_Error mpeos_memFreePGen(mpe_MemColor color, void *mem)
{
    mpe_Error retval = MPE_SUCCESS;

    if ((MPE_MEM_NCOLORS < color) || (NULL == mem))
    {
        retval = MPE_EINVAL;
    }
    else
    {
        free(mem);
    }

    return (retval);
}

/**
 * <i>mpeos_memReallocP()</i>
 *
 * Resize the specified block of memory to the specified size
 *
 * @param size is the size to resize to.
 * @param mem is a pointer to the pointer to the memory block to resize. The
 *        new memory block pointer is returned via this pointer.
 *
 * @return The MPE error code if resize fails, otherwise <i>MPE_SUCCESS</i>
 *         is returned.
 */
mpe_Error mpeos_memReallocPGen(mpe_MemColor color, uint32_t size, void **mem)
{
    mpe_Error retval = MPE_SUCCESS;

    if (MPE_MEM_NCOLORS < color)
    {
        retval = MPE_EINVAL;
    }
    else if ((NULL == mem) || ((NULL == *mem) && (0 == size)))
    {
        retval = MPE_EINVAL;
    }

    if (retval == MPE_SUCCESS)
    {
        void *p;
        if ((p = realloc(*mem, size)) == NULL)
        {
            retval = MPE_ENOMEM;
        }
        else
        {
            *mem = p;
        }
    }

    return (retval);
}

/**
 * <i>mpeos_memGetFreeSize()</i>
 *
 * Get the amount of free system memory available.
 *
 * @param freeSize is a pointer for returning the size of free memory
 * the system.
 *
 * @return The MPE error code if get size fails, otherwise <i>MPE_SUCCESS</i>
 *         is returned.
 */
mpe_Error mpeos_memGetFreeSize(mpe_MemColor color, uint32_t *freeSize)
{
    if (MPE_MEM_NCOLORS < color)
    {
        return MPE_EINVAL;
    }
    /* Functionality not available. */
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_MEM,
            "mpeos_memGetFreeSize functionality not available\n");
    return MPE_ENODATA;
}

/**
 * <i>mpeos_memGetLargestFree()</i>
 *
 * Get the size of the largest available block of memory in the system.
 *
 * @param freeSize is a pointer for returning the size of free memory
 * in the system.
 *
 * @return The MPE error code if get size fails, otherwise <i>MPE_SUCCESS</i>
 *         is returned.
 */
mpe_Error mpeos_memGetLargestFree(mpe_MemColor color, uint32_t *freeSize)
{
    if (MPE_MEM_NCOLORS < color)
    {
        return MPE_EINVAL;
    }

    /* Functionality not available. */
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_MEM,
            "mpeos_memGetLargestFree functionality not available\n");
    return MPE_ENODATA;
}

#ifdef MPE_MEM_HANDLES
/*
 * Some thoughts...
 * - We may wish to rethink the priority mappings.  Perhaps use an OS based definition.
 * - We might not really need an external "purge" API.  And just implicitly do it on
 * memory allocation.
 * - The same *could* be said for compactions, but it would make sense to keep compaction
 * around for a 2AM operation.
 * - Perhaps we don't even need purge here at all, and it could be part of a specialized
 * caching API.
 */

/**
 * Allocates a block of memory that is referenced by a <i>handle</i>.
 * Handle-referenced memory may be purgeable and relocatable if the
 * implementation supports it.
 *
 * @param color a somewhat loose designation of the type or intended use of the
 *          memory; may or may not be used depending upon implementation.
 *          May map to a distinct heap (for example).
 * @param size is the size of the memory block to allocate
 * @param priority the priority of purgeable handles with lower priority more
 *          likely to be purged than higher;
 *          MPE_MEM_NOPURGE (0) represents an unpurgeable priority;
 *          MPE_MEM_PRIOR_HIGH is the highest purgeable priority and
 *          MPE_MEM_PRIOR_LOW is the lowest purgeable priority.
 * @param h is a pointer for returning the handle to the newly allocated
 *          memory block.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 *
 * @see #mpeos_memAlloc
 */

#ifndef MPE_MEM_DEFAULT
#define MPE_MEM_DEFAULT 0
#endif

mpe_Error mpeos_memAllocH(mpe_MemColor color,
        uint32_t size, uint32_t priority,
        mpe_MemHandle *h)
{
    /*
     * Per assertion:
     *     "memAllocH() with an invalid color returns MPE_SUCCESS
     *      and allocates default memory color"
     * re: bug #2981
     */

    if ( MPE_MEM_NCOLORS < color )
    {
        return mpeos_memAllocP(MPE_MEM_DEFAULT, size, (void**)h);
    }
    else
    {
        return mpeos_memAllocP(color, size, (void**)h);
    }
}

/**
 * Frees the block of memory referenced by the given handle.
 *
 * @param color the original color specified on allocation
 * @param h handle returned by mpeos_memAllocH or mpeos_memReallocH
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_memFreeH(mpe_MemColor color, mpe_MemHandle h)
{
    return mpeos_memFreeP(color, (void*)h);
}

/**
 * Resizes the memory block referenced by the given handle.
 * If necessary, a new block is allocated and the contents of the old block
 * copied.
 * This can also be used to change the priority of a handle.
 *
 * @param color a somewhat loose designation of the type or intended use of the
 *          memory; may or may not be used depending upon implementation.
 *          May map to a distinct heap (for example).
 * @param size is the size of the memory block to allocate
 * @param priority the priority of purgeable handles with lower priority more
 *          likely to be purged than higher;
 *          MPE_MEM_NOPURGE (0) represents an unpurgeable priority;
 *          MPE_MEM_PRIOR_HIGH is the highest purgeable priority and
 *          MPE_MEM_PRIOR_LOW is the lowest purgeable priority.
 * @param h is a pointer to the existing handle and is used to store the handle
 *          to newly allocated memory if necessary.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 *
 * @see #mpeos_memRealloc
 */
mpe_Error mpeos_memReallocH(mpe_MemColor color,
        uint32_t size, uint32_t priority, mpe_MemHandle *h)
{
    return mpeos_memReallocP(color, size, (void**)h);
}

/**
 * Locks or unlocks the data referenced by the given handle.
 * The handle must be locked prior to accessing the data referenced by it
 * and should be unlocked as soon as possible.
 * Handle data that is locked cannot be purged or relocated.
 * <p>
 * The handle is locked with a call to <code>mpeos_memLock</code>H specifying
 * a non-NULL <i>ptr</i> argument.  E.g.,
 * <pre>
 * void* data;
 * mpeos_memLockH(handle, &data);
 * </pre>
 * <p>
 * The handle is unlocked with a call to <code>mpeos_memLock</code>H specifying
 * a NULL <i>ptr</i> argument.  E.g.,
 * <pre>
 * mpe_memLockH(handle, NULL);
 * </pre>
 * <p>
 * Each call to lock the handle should have a matching call to unlock the
 * handle.
 *
 * @param h the handle that refers to a block of memory that should be
 *    considered locked.
 * @param ptr a pointer to a pointer where the address of the relevant
 *    data block will be written following a successful locking of the data block;
 *    if <i>ptr</i> is NULL then a previous locking of <i>h</i> should be reversed.
 * @return The MPE error code if the operation fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_memLockH(mpe_MemHandle h, void** ptr)
{
    if ( h == NULL )
    {
        return MPE_EINVAL;
    }

    if ( ptr != NULL )
    {
        *ptr = (void *) h;
    }

    return MPE_SUCCESS;
}

/**    if (ptr != NULL)
 {
 *ptr = (void *) h;
 }

 * Attempts to purge handle-based allocations of the given or lower priority.
 * If priority is not supported, then purging only takes place if MPE_MEM_PRIOR_HIGH
 * is specified.
 * <p>
 * Will not purge unpurgeable or locked data.
 * <p>
 * Will not do anything if the implementation does not support purging.
 *
 * @param color color of allocated memory
 * @param priority specifies that all handle-based allocations of the
 *    given priority or lower should be purged
 * @return The MPE error code if the operation fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_memPurge(mpe_MemColor color, uint32_t priority)
{
    return MPE_SUCCESS;
}

/**
 * Attempts to compact memory.  If the implementation supports it,
 * non-locked and relocatable handle-based allocations will be rearranged to
 * provide the largest amount of free-space available.
 * <p>
 * Will not compact if the implementation does not support compaction.
 *
 * @param color color of allocated memory
 * @return The MPE error code if the operation fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_memCompact(mpe_MemColor color)
{
    if ( MPE_MEM_NCOLORS < color )
    {
        return MPE_EINVAL;
    }
    return MPE_SUCCESS;
}

#endif /* MPE_MEM_HANDLES */

/**
 * Performs any low-level initialization required to set up the OS-level
 * memory subsystem.
 * The memory subsystem should be initialized following the environment
 * subsystem (via <code>mpeos_envInit()</code>) to allow it to be configured
 * by the environment.  However, access to the memory APIs prior to initialization
 * (e.g., by <code>mpeos_envInit()</code>) should be allowed.
 */
void mpeos_memInit(void)
{
}

/**
 * <i>mpeos_memStats<i/>
 *
 * Low-level interface to memory statistics.  Used by any MPE or
 * java code to trigger a dump of the current memory statistics.
 *
 * @param toConsole flags whether to send the stats to the console or the default
 *        output stream specified in the mpeenv.ini file.
 * @param color specifies which color statistics to dump (-1 = all).
 */
void mpeos_memStats(mpe_Bool toConsole, mpe_MemColor color, const char *label)
{
#if MPE_FEATURE_TRACK_MEM
    /* on Mot if it's not going to the console, it's not going anywhere */
    if ( !toConsole )
    return;

    /* Check for dump label. */
    if ( NULL != label )
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_MEM, "%s", label);

    /* Dump entire map? */
    if ( -1 == color )
    {
        dumpUsageMap(NULL);
    }
    else if ( color < (sizeof(stats) / sizeof(ColorStat)) )
    {
        dumpUsage(&stats[color], color);
    }
#endif
}

/**
 * Gets the memory statistics for all the colors into an array of
 * mpe_MemStatsInfo structures. Returns MPE_EINVAL if statsSize is not
 * the size of mpe_MemStatsInfo times the constant MPE_MEM_NCOLORS.
 *
 * @param statsSize number of bytes pointed to by stats. Should always be
 * sizeof(mpe_MemStatsInfo) * MPE_MEM_NCOLORS. [Input]
 * @param stats pointer to an array of MPE_MEM_NCOLORS mpe_MemStatsInfo
 * structures.
 *
 * return error value or MPE_SUCCESS if successful
 */
mpe_Error mpeos_memGetStats(uint32_t statsSize, mpe_MemStatsInfo *statsArray)
{
#ifndef MPE_FEATURE_TRACK_MEM_MIN
    return MPE_EINVAL;
#else
    uint32_t i;

    if ( statsSize != sizeof(mpe_MemStatsInfo) * MPE_MEM_NCOLORS )
    return MPE_EINVAL;

    for ( i = 1; i < MPE_MEM_NCOLORS + 1; ++i )
    {
        statsArray->name = colors[i];
        statsArray->currAllocated = stats[i].currSize;
        statsArray->maxAllocated = stats[i].maxSize;

        ++statsArray;
    }

    return MPE_SUCCESS;
#endif /* MPE_FEATURE_TRACK_MEM_MIN */
}

/**
 * Registers a callback which will be called when memory is low and the memory
 * manager needs the various components to release memory.  When memory is low,
 * the memory manager will request that the components free memory, and will ask
 * specify the amount of memory it's looking for.  The callback function should then
 * attempt to free the memory, and, when finished, return the amount of memory it
 * released. At the very least, the callback should return 0 if unable to free any
 * memory, and non-zero if it successfully freed something.
 *
 * @param color     The color this callback will attempt to free.
 * @param function  The callback function.
 *
 * return error value or MPE_SUCCESS if successful
 */
#if 0 /* PORT095 */

mpe_Error mpeos_memRegisterMemFreeCallback(mpe_MemColor color, uint32_t (*function)(uint32_t))
#endif
mpe_Error mpeos_memRegisterMemFreeCallback(mpe_MemColor color,
        int32_t(*function)(mpe_MemColor, int32_t, int64_t, void *),
        void *context)
{
    return MPE_SUCCESS;
}

/* PORT095 PHH 2/28/07 */
#ifdef MPE_FEATURE_MEM_PROF

#define MEM_SYS_MINIMUM_SIZE_KB     (1024)

/**
 * Returns Color string for easy debugging.
 *
 * @param color the color for which a heap is requested
 *
 * @return a string corresponds to color type
 */
static const char *color2String( mpe_MemColor color )
{
    printf("STUB: <mpeos_mem.c::mpe_MemColor> implementation pending\n");
    return "UNIMPLEMENTED";

    /* code from 0.9.5 powertv */
    return(color >= MPE_MEM_SYSTEM && color < MPE_MEM_NCOLORS)
    ? colors[COLOR_TO_INDEX(color)] : "UNKNOWN";
}

static const char *extractFilename (const char *path)
{
    char *name;
    printf("STUB: <mpeos_mem.c::extractFilename> implementation pending\n");
    return "NULL";

    /* code from 0.9.5 powertv */
    if (path == NULL)
    return "NULL";

    if ((name = strrchr(path, '/')) == NULL)
    return path;
    else
    return name+1;
}

#ifdef NEW_REALLOC
static void* memRealloc(Mem_Pointer oldPtr, uint32_t size)
{
    void* ptr;

    printf("STUB: <mpeos_mem.c::memRealloc> implementation pending\n");
    return NULL;

#ifdef PORTED_PLACEHOLDER_CODE
    /* code from 0.9.5 powertv */
    pk_Try
    {
        ptr = mem_ResizePointer((Mem_Pointer)oldPtr, size, TRUE);
    }
    pk_Catch(kPk_ExceptionAll)
    {
        ptr = NULL;
    }
    pk_EndCatch;

    return ptr;
#endif
}
#endif

void newMemtkMutex(void)
{
    printf("STUB: <mpeos_mem.c::newMemtkMutex> implementation pending\n");
    return NULL;

#ifdef PORTED_PLACEHOLDER_CODE
    /* code from 0.9.5 powertv */
    static mpe_Bool init = false;

    if (init == false)
    {
        gMemTrackMutex = pk_NewMutex(kPtv_Mutex_Inherit, "MemTrackMutex");
        init = true;
    }
#endif
}

void setStartTime(void)
{
    printf("STUB: <mpeos_mem.c::setStartTime> implementation pending\n");

#ifdef PORTED_PLACEHOLDER_CODE
    /* code from 0.9.5 powertv */
    static mpe_Bool init = false;

    if (init == false)
    {
        gStartTime = pk_Time();
        init = true;
    }
#endif
}

#ifdef DEBUG_MEM_TRACK
static void checkMemTrackList (void)
{
    printf("STUB: <mpeos_mem.c::checkMemTrackList> implementation pending\n");

#ifdef PORTED_PLACEHOLDER_CODE
    /* code from 0.9.5 powertv */
    TrackHeader *p, *q;

    pk_GrabMutex(gMemTrackMutex);
    p = q = gMemTrackList;

    while (p && p->reserved == 0xaabbccdd)
    {
        q = p;
        p = p->next;
    }
    if (p == NULL)
    {
        p = q;
        while (p && p->reserved == 0xaabbccdd)
        {
            p = p->prev;
        }
        if (p == NULL)
        {
            pk_ReleaseMutex(gMemTrackMutex);
            return;
        }
        else
        assert(p->reserved == 0xaabbccdd);
    }
    else
    assert(p->reserved == 0xaabbccdd);
#endif
}
#endif

/**
 * <i>addMemInTrackList()</i> function put memory tracking info into the allocated
 * memory and add it into the memory tracking link list.
 *
 * Description:
 *      Always add the memory to the head of the list. The list maintains
 * the order of the allocations. Freeing memory in the middle of list
 * will not affect the order of the allocations.
 *
 * @param ptr is an OS memory address including the TrackHeader.
 * @param fileName is a string of caller's file name.
 * @param lineNum is an integer of line number in the caller's file.
 * @return none
 */
static void addMemInTrackList(void * ptr, char *fileName, int lineNum)
{
    printf("STUB: <mpeos_mem.c::addMemInTrackList> implementation pending\n");

#ifdef PORTED_PLACEHOLDER_CODE
    /* code from 0.9.5 powertv */
    TrackHeader *pTHeader = (TrackHeader*)ptr;

    CHECK_MEM_TRACKLIST();
    pk_GrabMutex(gMemTrackMutex);

    pTHeader->timeStamp = pk_Time() - gStartTime;
    pTHeader->filename = fileName;
    pTHeader->lineno = lineNum;
    pTHeader->checkptNum = gCheckPtNum;
    pTHeader->checkptStr = gCheckPtStr;
    pTHeader->reserved = 0xaabbccdd;
    pTHeader->next = gMemTrackList;
    pTHeader->prev = NULL;
    gMemTrackList = pTHeader;
    if (pTHeader->next)
    pTHeader->next->prev = pTHeader;

    pk_ReleaseMutex(gMemTrackMutex);
    CHECK_MEM_TRACKLIST();
#endif
}

/**
 * <i>removeMemInTrackList()</i> function remove memory from the memory
 * tracking link list.
 *
 * @param ptr is an OS memory address including the TrackHeader.
 * @return none
 */
static void removeMemInTrackList(void * ptr)
{
    printf("STUB: <mpeos_mem.c::removeMemInTrackList> implementation pending\n");

#ifdef PORTED_PLACEHOLDER_CODE
    /* code from 0.9.5 powertv */
    TrackHeader *pTHeader = (TrackHeader*)ptr;

    CHECK_MEM_TRACKLIST();
    pk_GrabMutex(gMemTrackMutex);

    assert(pTHeader->reserved == 0xaabbccdd);

    if (pTHeader == gMemTrackList)
    {
        gMemTrackList = pTHeader->next;
        gMemTrackList->prev = NULL;
    }
    else
    {
#if defined(qDebug) && (qDebug == 1)
        assert(pTHeader->prev && pTHeader->next);
#endif
        pTHeader->prev->next = pTHeader->next;
        pTHeader->next->prev = pTHeader->prev;
    }
    pTHeader->reserved = 0x11223344;
    pTHeader->next = NULL;
    pTHeader->prev = NULL;
    pk_ReleaseMutex(gMemTrackMutex);
    CHECK_MEM_TRACKLIST();
#endif
}

void dumpMemtkStats (void)
{
    printf("STUB: <mpeos_mem.c::dumpMemtkStats> implementation pending\n");

#ifdef PORTED_PLACEHOLDER_CODE
    /* code from 0.9.5 powertv */

#ifdef MPE_FEATURE_TRACK_MEM_FULL
    MemRange total =
    {   0};
#endif
    int n = sizeof(stats) / sizeof(ColorStat);
    int i, j/*, currSize, maxSize, deallocSize*/;
    uint32_t free;
    uint32_t largest;

    MEMTK_OUTPUT("\n");
    MEMTK_OUTPUT("          COLOR        ALLOCS DEALLOCS OUTSTAND FAILURES FAILSIZE   SIZE     TOTAL     HEAP      FRAG\n");
    MEMTK_OUTPUT("      -------------- -------- -------- -------- -------- -------- -------- --------- ---------- ------\n");

    /* loop through all colors[] & stats[] */
    for (i=0; i < n; ++i)
    {
        mpeos_memGetFreeSize(INDEX_TO_COLOR(i), &free);
        mpeos_memGetLargestFree(INDEX_TO_COLOR(i), &largest);

#ifdef MPE_FEATURE_TRACK_MEM_FULL
        memset((void*)&total, 0, sizeof(MemRange));
        /* loop through all the usage[] in each stats[] */
        for (j=0; j<94; j++)
        {
            total.alloc += stats[i].usage[j].alloc;
            total.dealloc += stats[i].usage[j].dealloc;
            total.failure += stats[i].usage[j].failure;
            total.failSize += stats[i].usage[j].failSize;
            total.currSize += stats[i].usage[j].currSize;
            total.totalSize += stats[i].usage[j].totalSize;
        }
        MEMTK_OUTPUT("memtk:%-14s %8lu %8lu %8lu %8lu %8lu %8lu %9lu 0x%08x %5.2f%%\n",
                color2String(INDEX_TO_COLOR(i)), total.alloc, total.dealloc, total.alloc-total.dealloc,
                total.failSize, total.failSize, total.currSize, total.totalSize, (unsigned int)color2Heap(INDEX_TO_COLOR(i)),
                (float)(free - largest) * 100 / (float)free);
        stats[i].currSize = total.currSize;
        stats[i].currSize = total.totalSize;
#else
        MEMTK_OUTPUT("memtk:%-14s %8lu                                               %9lu 0x%08x %5.2f%%\n",
                color2String(INDEX_TO_COLOR(i)), stats[i].currSize, stats[i].currSize, (unsigned int)color2Heap(INDEX_TO_COLOR(i)),
                (float)(free - largest) * 100 / (float)free);
#endif
    }
#endif
}

static void x_PrintMemtkUsage(Mon_OutputFunc output)
{
    printf("STUB: <mpeos_mem.c::x_PrintMemtkUsage> implementation pending\n");

#ifdef PORTED_PLACEHOLDER_CODE
    /* code from 0.9.5 powertv */
    // Display a detailed description
    output("\n----------------\nmemtk\n----------------\n");
    output("\nDescription:\n");
    output("  This command gives you information about memory tracking in OCAP middleware.\n");

    output("\nSyntax:\n");
    output("  memtk               - displays memory tracking usage\n");
    output("  memtk #             - displays outstanding memory allocated with check point number (#)\n");
    output("  memtk all           - displays all outstanding memory allocated\n");
    output("  memtk latest        - displays the outstanding memory allocated that match the current check point\n");
    output("  memtk last          - displays the outstanding memory allocated that match the last check point\n");
    output("  memtk \"a string\"    - displays the outstanding memory allocated that match the check point string\n");
    output("  memtk mark          - mark a new check point\n");
    output("  memtk checkpt       - displays the latest check point\n");
    output("  memtk stat          - displays the sums of all individual colors\n");
#endif
}

/**
 * Installed PowerTV monitor command.
 */
static void memtkCommand(eMon_Why why, Mon_InputFunc input, Mon_OutputFunc output, ui8 *buf)
{
    printf("STUB: <mpeos_mem.c::memtkCommand> implementation pending\n");

#ifdef PORTED_PLACEHOLDER_CODE
    /* code from 0.9.5 powertv */
    char cmd[10] = "\0";
    char arg1[64] = "\0";
    ui32 nArgs=0;
    ui32 checkpt;

    switch (why)
    {
        case kMon_Verbose:
        output("memtk - ");
        /* FALLTHROUGH */
        case kMon_Help:
        x_PrintMemtkUsage(output);
        break;
        case kMon_Perform:
        output("Got command: ***%s***\n", buf);

        if (buf != NULL)
        nArgs = sscanf(buf, "%s %s", cmd, arg1);

        if (nArgs <= 1)
        x_PrintMemtkUsage(output);

        else if (!strcmp(arg1, "all"))
        {
            DisplayMemProfiling(ALL_CHECKPT);
        }
        else if (!strcmp(arg1, "latest"))
        {
            DisplayMemProfiling(LATEST_CHECKPT);
        }
        else if (!strcmp(arg1, "last"))
        {
            DisplayMemProfiling(LAST_CHECKPT);
        }
        else if (!strcmp(arg1, "stat"))
        {
            dumpMemtkStats();
        }
        else if (!strcmp(arg1, "mark"))
        {
            setMemCheckptStr(NULL);
            output("The new check point is %d\n", gCheckPtNum);
        }
        else if (!strcmp(arg1, "checkpt"))
        {
            pk_GrabMutex(gMemTrackMutex);
            output("The latest check point is %d\n", gCheckPtNum);
            pk_ReleaseMutex(gMemTrackMutex);
        }
        else if ((checkpt = strtoul(arg1, NULL, 10)) != 0)
        {
            if (checkpt > gCheckPtNum)
            output("Check point is not recognized.\n");
            else
            DisplayMemProfiling(checkpt);
        }
        else if (!strcmp(arg1, "0"))
        {
            DisplayMemProfiling(0);
        }
        else // use it as a string

        {
            TrackHeader *pCurr = gMemTrackList;

            while (pCurr && (pCurr->checkptStr == NULL || strcmp(pCurr->checkptStr, arg1)))
            pCurr = pCurr->next;
            if (pCurr)
            DisplayMemProfiling(pCurr->checkptNum);
            else
            output("The \"%s\" string is not found.\n", arg1);
        }
        //dumpUsageMap(stdout, arg);
        break;
        case kMon_Repeat:
        output("I'm not sure what to do with %d\n", why);
        break;
    }
#endif
}

void setMemCheckptStr (const char *str)
{
    printf("STUB: <mpeos_mem.c::setMemCheckptStr> implementation pending\n");

#ifdef PORTED_PLACEHOLDER_CODE
    /* code from 0.9.5 powertv */
    pk_GrabMutex(gMemTrackMutex);
    gCheckPtStr = (char*)str;
    gCheckPtNum++;
    pk_ReleaseMutex(gMemTrackMutex);
#endif
}

void DisplayMemProfiling(ui32 checkpt)
{
    printf("STUB: <mpeos_mem.c::DisplayMemProfiling> implementation pending\n");

#ifdef PORTED_PLACEHOLDER_CODE
    /* code from 0.9.5 powertv */
    TrackHeader *pCurr = gMemTrackList;
    TrackHeader *pStart = NULL;
    TrackHeader *pEnd = NULL;
    int i;
    char buffer[256];

    if (pCurr == NULL)
    return;

    pk_GrabMutex(gMemTrackMutex);

    // startPtr
    if (checkpt == ALL_CHECKPT)
    {
        pStart = pCurr;
        pEnd = NULL;
    }
    else
    {
        // Assign the right check point
        if (checkpt == LATEST_CHECKPT)
        checkpt = gCheckPtNum;
        else if (checkpt == LAST_CHECKPT)
        {
            if ((checkpt = gCheckPtNum - 1) < 0)
            {
                MEMTK_OUTPUT("No memory allocations with this check point (%d)\n", checkpt);
                pk_ReleaseMutex(gMemTrackMutex);
                return; // no last
            }
        }

        pCurr = gMemTrackList;

        // Find the start pointer
        if (pCurr->checkptNum == checkpt)
        pStart = pCurr;
        else
        {
            while (pCurr && pCurr->checkptNum != checkpt)
            pCurr = pCurr->next;

            if (pCurr && pCurr->checkptNum == checkpt)
            pStart = pCurr;
            else
            {
                MEMTK_OUTPUT("No memory allocations with this check point (%d)\n", checkpt);
                pk_ReleaseMutex(gMemTrackMutex);
                return; // no checkpt
            }
        }

        // Find the end pointer (still using the last pCurr)
        while (pCurr && pCurr->checkptNum == checkpt)
        pCurr = pCurr->next;

        pEnd = pCurr;
    }

    MEMTK_OUTPUT("\n");
    MEMTK_OUTPUT("COUNT   MEMORY    SIZE       COLOR      CK#    TIME     FILENAME:LINENO       (CHECK PT STRING)\n");
    MEMTK_OUTPUT("----- ---------- ------- -------------- --- ----------\n");

    i = 0;
    pCurr = pStart;
    while (pCurr != pEnd)
    {
        sprintf(buffer, "%5d 0x%08x %7d %-14s %3d 0x%08x %s:%d (%s)\n",
                i++, (int)pCurr,
                (int)pCurr->size, color2String(pCurr->color), (int)pCurr->checkptNum, (int)pCurr->timeStamp,
                extractFilename(pCurr->filename), (int)pCurr->lineno, pCurr->checkptStr?pCurr->checkptStr:" ");
        MEMTK_OUTPUT(buffer);

        pCurr = pCurr->next;
    }
    gCheckPtStr = NULL;
    pk_ReleaseMutex(gMemTrackMutex);
#endif
}

void configAvailableMemory(void)
{
    printf("STUB: <mpeos_mem.c::configAvailableMemory> implementation pending\n");

#ifdef PORTED_PLACEHOLDER_CODE
    /* code from 0.9.5 powertv */
    const char *rsString = mpeos_envGet("MEM.SYS.RESTRICT.SIZE");
    int32_t size, throwAwayHeapSize, freeSize, spaceLeftInByte;
    Mem_Pointer memPtr;
    Mem_Heap memHeap;
    static mpe_Bool init = false;

    if (init == false)
    {
        if (rsString == NULL)
        return;
        else
        {
            char *temp;
            long temp2;
            temp2 = strtol( rsString, &temp, 0 );
            if (rsString == temp)
            return;
            else
            spaceLeftInByte = (temp2 < MEM_SYS_MINIMUM_SIZE_KB ? MEM_SYS_MINIMUM_SIZE_KB : temp2 ) * 1024;
        }

        freeSize = mem_GetFreeSize(kMem_SystemHeap, 0);
        throwAwayHeapSize = freeSize - spaceLeftInByte;
        size = mem_GetLargestPointer(kMem_SystemHeap);

        if (throwAwayHeapSize < 0)
        {
            /* the intended size is bigger than the free system size, no op */
            init = true;
            return;
        }
        if (size > throwAwayHeapSize)
        size = throwAwayHeapSize;

        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_MEM, "<<MEM INIT>>: free Size(%d), throw away size(%d), space left(%d)\n", freeSize, size, spaceLeftInByte );

        {
            int32_t allocSize = size;
            pk_Try
            {
                memPtr = mem_NewPointer(kMem_SystemHeap, allocSize);
                memHeap = mem_NewHeap(memPtr, allocSize);
            }
            pk_Catch(kPk_ExceptionAll)
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEM, "<<MEM INIT>>: could not create heap of %d bytes, freeSize(%d)\n", allocSize, freeSize);
                /* skip this heap. */
            }
            pk_EndCatch;
        }
        init = true;
    }
#endif
}
#endif
