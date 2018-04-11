#if !defined(_MPEOS_MEM_H)
#define _MPEOS_MEM_H
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
#include <mpe_types.h>	/* Resolve basic type references. */

#ifdef __cplusplus
extern "C"
{
#endif

/** Highest priority means no purging. */
#define MPE_MEM_NOPURGE      0
#define MPE_MEM_PRIOR_HIGH   1
#define MPE_MEM_PRIOR_LOW    255

/** structure returned in array form from mpeos_memGetStats */
typedef struct mpe_MemStatsInfo
{
    const char *name; /* ASCIIZ name of the color */
    uint32_t currAllocated; /* number of bytes currently allocated */
    uint32_t maxAllocated; /* highest number of bytes ever allocated */
} mpe_MemStatsInfo;

/*
 * Memory API prototypes:
 */

/**
 * The <i>mpeos_memAllocP()</i> function will allocate a block of system memory of 
 * the specified size.  The address of the memory block allocated is returned via 
 * the pointer.
 *
 * @param color a somewhat loose designation of the type or intended use of the
 *          memory; may or may not be used depending upon implementation.
 *          May map to a distinct heap (for example).
 * @param size is the size of the memory block to allocate
 * @param memory is a pointer for returning the pointer to the newly allocated 
 *          memory block.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
#ifdef MPE_FEATURE_MEM_PROF
#define mpeos_memAllocP(c,s,m)		mpeos_memAllocPProf(c, s, m, __FILE__, __LINE__)
#else
#define mpeos_memAllocP				mpeos_memAllocPGen
#endif
mpe_Error mpeos_memAllocPGen(mpe_MemColor color, uint32_t size, void **memory);

/**
 * The <i>mpeos_memFreeP()</i> function will free the specified block of system
 * memory.
 *
 * @param color the original color specified on allocation
 * @param memory is a pointer to the memory block to free.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
#ifdef MPE_FEATURE_MEM_PROF
#define mpeos_memFreeP(c,m)			mpeos_memFreePProf(c, m, __FILE__, __LINE__)
#else
#define mpeos_memFreeP				mpeos_memFreePGen
#endif
mpe_Error mpeos_memFreePGen(mpe_MemColor color, void *memory);

/**
 * The <i>mpeos_memReallocP()</i> function will resize the specified block of
 * memory to the specified size. 
 *
 * @param color a somewhat loose designation of the type or intended use of the
 *          memory; may or may not be used depending upon implementation.
 *          May map to a distinct heap (for example).
 * @param size is the size to resize to.
 * @param memory is a pointer to the pointer to the memory block to resize.  The
 *          new memory block pointer is returned via this pointer.  If the passed
 *          pointer is NULL, then this call has the same effect as a call to 
 *          mpeos_memAllocP();
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
#ifdef MPE_FEATURE_MEM_PROF
#define mpeos_memReallocP(c,s,m)	mpeos_memReallocPProf(c, s, m, __FILE__, __LINE__)
#else
#define mpeos_memReallocP			mpeos_memReallocPGen
#endif
mpe_Error
        mpeos_memReallocPGen(mpe_MemColor color, uint32_t size, void **memory);

/**
 * The <i>mpeos_memGetFreeSize()</i> function will get the amount of free system
 * memory available.
 *
 * @param color a somewhat loose designation of the type or intended use of the
 *          memory; may or may not be used depending upon implementation.
 *          May map to a distinct heap (for example).
 * @param freeSize is a pointer for returning the size of free memory in the
 *          system.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_memGetFreeSize(mpe_MemColor color, uint32_t *freeSize);

/**
 * The <i>mpeos_memGetLargestFree()</i> function will get the size of the largest
 * available block of memory in the system. 
 *
 * @param color a somewhat loose designation of the type or intended use of the
 *          memory; may or may not be used depending upon implementation.
 *          May map to a distinct heap (for example).
 * @param freeSize is a pointer for returning the size of free memory in the
 *          system.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_memGetLargestFree(mpe_MemColor color, uint32_t *freeSize);

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
mpe_Error mpeos_memAllocH(mpe_MemColor color, uint32_t size, uint32_t priority,
        mpe_MemHandle *h);

/**
 * Frees the block of memory referenced by the given handle.
 *
 * @param color the original color specified on allocation
 * @param h handle returned by mpeos_memAllocH or mpeos_memReallocH
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_memFreeH(mpe_MemColor color, mpe_MemHandle h);

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
 *          to newly allocated memory if necessary.    If the passed handle is 
 *          invalid, this method has the same effect as a call to 
 *          mpeos_memAllocH().
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 *
 * @see #mpeos_memRealloc
 */
mpe_Error mpeos_memReallocH(mpe_MemColor color, uint32_t size,
        uint32_t priority, mpe_MemHandle *h);

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
mpe_Error mpeos_memLockH(mpe_MemHandle h, void** ptr);

/**
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
mpe_Error mpeos_memPurge(mpe_MemColor color, uint32_t priority);

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
mpe_Error mpeos_memCompact(mpe_MemColor color);

/**
 * Performs any low-level initialization required to set up the OS-level
 * memory subsystem.  
 * The memory subsystem should be initialized following the environment
 * subsystem (via <code>mpeos_envInit()</code>) to allow it to be configured
 * by the environment.  However, access to the memory APIs prior to initialization
 * (e.g., by <code>mpeos_envInit()</code>) should be allowed.
 */
void mpeos_memInit(void);

/**
 * Forces a dump of the current internal memory statistics.
 *
 * @param toConsole if TRUE specifies that the statistics should be dumped to stdout
 *                  versus the path specified in the ini file.
 * @param color specifies which colot to dump the stats for (-1 = all colors).
 * @param label specifies a label to be applied to this statistical dump.
 */
void mpeos_memStats(mpe_Bool toConsole, mpe_MemColor color, const char *label);

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
mpe_Error mpeos_memGetStats(uint32_t statsSize, mpe_MemStatsInfo *stats);

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
 * @param context   void * that will be passed to the callback
 * 
 * return error value or MPE_SUCCESS if successful
 */
mpe_Error mpeos_memRegisterMemFreeCallback(mpe_MemColor color,
        int32_t(*function)(mpe_MemColor, int32_t, int64_t, void *),
        void *context);

/**
 * Unregisters a callback which will be called when memory is low and the memory
 * manager needs the various components to release memory.
 *
 * color,function, and context must all match a current registration.
 *
 * @param color     The color to be unregistered
 * @param function  The callback function to be unregistered
 * @param context   The context to be unregistered
 * 
 * return error value or MPE_SUCCESS if successful
 */
mpe_Error mpeos_memUnregisterMemFreeCallback(mpe_MemColor color,
        int32_t(*function)(mpe_MemColor, int32_t, int64_t, void *),
        void *context);

/**
 * The <i>mpeos_memAllocPProf()</i> function will allocate a block of system memory of 
 * the specified size.  The address of the memory block allocated is returned via 
 * the pointer.
 *
 * @param color a somewhat loose designation of the type or intended use of the
 *          memory; may or may not be used depending upon implementation.
 *          May map to a distinct heap (for example).
 * @param size is the size of the memory block to allocate
 * @param memory is a pointer for returning the pointer to the newly allocated 
 *          memory block.
 * @param fileName is a pointer for the name of the .c file
 * @param lineNum is the line number in the .c file
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_memAllocPProf(mpe_MemColor color, uint32_t size, void **memory,
        char* fileName, uint32_t lineNum);

/**
 * The <i>mpeos_memFreePProf()</i> function will free the specified block of system
 * memory.
 *
 * @param color the original color specified on allocation
 * @param memory is a pointer to the memory block to free.
 * @param fileName is a pointer for the name of the .c file
 * @param lineNum is the line number in the .c file
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_memFreePProf(mpe_MemColor color, void *memory, char* fileName,
        uint32_t lineNum);

/**
 * The <i>mpeos_memReallocPProf()</i> function will resize the specified block of
 * memory to the specified size. 
 *
 * @param color a somewhat loose designation of the type or intended use of the
 *          memory; may or may not be used depending upon implementation.
 *          May map to a distinct heap (for example).
 * @param size is the size to resize to.
 * @param memory is a pointer to the pointer to the memory block to resize.  The
 *          new memory block pointer is returned via this pointer.  If the pointer
 *          is NULL, then this method has the same effect as a call to 
 mpeos_memAllocPProf().
 * @param fileName is a pointer for the name of the .c file
 * @param lineNum is the line number in the .c file
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_memReallocPProf(mpe_MemColor color, uint32_t size,
        void **memory, char* fileName, uint32_t lineNum);

#ifdef __cplusplus
}
#endif

#endif /* _MPEOS_MEM_H */
