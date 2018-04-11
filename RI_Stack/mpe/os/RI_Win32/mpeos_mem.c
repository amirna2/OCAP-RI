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

/* Header Files */
#include <mpe_types.h>
#include <mpe_error.h>
#include <mpeos_mem.h>
#include <mpeos_util.h>
#include <mpeos_dbg.h>

#include <stdlib.h>
#include <stdio.h>

/**
 * Initialize the Memory API.
 */
void mpeos_memInit()
{
    // Do nothing for now.
}

/**
 * The <i>mpeos_memAllocP()</i> function will allocate a block of system memory of
 * the specified size.  The address of the memory block allocated is returned via
 * the pointer.
 *
 * @param color A somewhat loose designation of the type or intended use of the
 *          memory; may or may not be used depending upon implementation.
 *          May map to a distinct heap (for example).
 * @param size Is the size of the memory block to allocate
 * @param memory Is a pointer for returning the pointer to the newly allocated
 *          memory block.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_memAllocP(mpe_MemColor color, uint32_t size, void **mem)
{
    mpe_Error retval = MPE_SUCCESS;

    /* Parameter sanity check... */
    if (mem == NULL)
    {
        return MPE_EINVAL;
    }

    if (size == 0)
    {
        return MPE_EINVAL;
    }

    if (color < MPE_MEM_GENERAL || color >= MPE_MEM_NCOLORS)
    {
        return MPE_EINVAL;
    }

    // Allocate the memory. For now we are ignoring the color of the
    // requested memory and simply using the system malloc.
    *mem = malloc(size);

    /* Report failures only. */
    if (*mem == NULL)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_MEM,
                "<<MEM>> ALLOC of size = %d and color = %d - FAILED\n", size,
                color);
        retval = MPE_ENOMEM;
    }

    return retval;
}

/**
 * The <i>mpeos_memFreeP()</i> function will free the specified block of system
 * memory.
 *
 * @param color The original color specified on allocation
 * @param memory Is a pointer to the memory block to free.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_memFreeP(mpe_MemColor color, void *mem)
{
    /* Parameter sanity check... */
    if (mem == NULL)
    {
        return MPE_EINVAL;
    }

    if (color < MPE_MEM_GENERAL || color >= MPE_MEM_NCOLORS)
    {
        return MPE_EINVAL;
    }

    // Free the memory. For now we are ignoring the color of the
    // requested memory and simply using the system malloc.
    free(mem);

    return MPE_SUCCESS;
}

/**
 * The <i>mpeos_memGetFreeSize()</i> function will get the amount of free system
 * memory available.
 *
 * @param color A somewhat loose designation of the type or intended use of the
 *          memory; may or may not be used depending upon implementation.
 *          May map to a distinct heap (for example).
 * @param freeSize Is a pointer for returning the size of free memory in the
 *          system.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_memGetFreeSize(mpe_MemColor color, uint32_t *freeSize)
{
    MEMORYSTATUS stats;

    /* Parameter sanity check... */
    if (color < MPE_MEM_GENERAL || color >= MPE_MEM_NCOLORS)
    {
        return MPE_EINVAL;
    }

    /*
     * Attempt to extract the information from Windows. On computers with more than 4 GB of memory,
     * the MEMORYSTATUS structure can return incorrect information. Windows reports a value of -1
     * to indicate an overflow, while Windows NT reports a value that is the real amount of memory,
     * modulo 4 GB. If this becomes a problem, use the GlobalMemoryStatusEx function instead of
     * the GlobalMemoryStatus function.
     */
    GlobalMemoryStatus(&stats);
    *freeSize = stats.dwAvailPhys;

    return MPE_SUCCESS;
}

/**
 * The <i>mpeos_memGetLargestFree()</i> function will get the size of the largest
 * available block of memory in the system.
 *
 * @param color A somewhat loose designation of the type or intended use of the
 *          memory; may or may not be used depending upon implementation.
 *          May map to a distinct heap (for example).
 * @param freeSize Is a pointer for returning the size of free memory in the
 *          system.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_memGetLargestFree(mpe_MemColor color, uint32_t *freeSize)
{
    MEMORYSTATUS stats;

    /* Parameter sanity check... */
    if (color < MPE_MEM_GENERAL || color >= MPE_MEM_NCOLORS)
    {
        return MPE_EINVAL;
    }

    /*
     * Attempt to extract the information from Windows. On computers with more than 4 GB of memory,
     * the MEMORYSTATUS structure can return incorrect information. Windows reports a value of -1
     * to indicate an overflow, while Windows NT reports a value that is the real amount of memory,
     * modulo 4 GB. If this becomes a problem, use the GlobalMemoryStatusEx function instead of
     * the GlobalMemoryStatus function.
     */
    GlobalMemoryStatus(&stats);
    *freeSize = stats.dwAvailPhys;

    return MPE_SUCCESS;
}

/**
 * The <i>mpeos_memReallocP()</i> function will resize the specified block of
 * memory to the specified size.
 *
 * @param color A somewhat loose designation of the type or intended use of the
 *          memory; may or may not be used depending upon implementation.
 *          May map to a distinct heap (for example).
 * @param size Is the size to resize to.
 * @param memory Is a pointer to the pointer to the memory block to resize.  The
 *          new memory block pointer is returned via this pointer.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_memReallocP(mpe_MemColor color, uint32_t size, void **mem)
{
    mpe_Error retval = MPE_SUCCESS;
    void *ptr;

    /* Parameter sanity check... */
    if (mem == NULL)
    {
        return MPE_EINVAL;
    }

    if (size == 0)
    {
        return MPE_EINVAL;
    }

    if (color < MPE_MEM_GENERAL || color >= MPE_MEM_NCOLORS)
    {
        return MPE_EINVAL;
    }

    // Reallocate the memory. For now we are ignoring the color of the
    // requested memory and simply using the system malloc.
    ptr = realloc(*mem, size);

    /* Report failures only. */
    if (ptr == NULL)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_MEM,
                "<<MEM>> REALLOC of size = %d and color = %d - FAILED\n", size,
                color);
        retval = MPE_ENOMEM;
    }
    *mem = ptr;

    return retval;
}

/**
 * The <i>mpeos_memAllocH</i> allocates a block of memory
 * via a handle.
 *
 * @param color Specifies the type or intended use of the memory to allocate.
 * @param size Specifies the size, in bytes, of the memory block to allocate.
 * @param priority Indicates the priority. Purgeable handles with lower
 *        priority are more likely to be purged than higher.
 * @param handle Is an output handle for returning the handle to the newly
 *        allocated memory block.
 *
 * @return If successful, <i>MPE_SUCCESS</i> sould be returned along
 *        with a pointer to the new memory block's handle.
 *        Otherwise, is should return an error code.
 */
mpe_Error mpeos_memAllocH(mpe_MemColor color, uint32_t size, uint32_t priority,
        mpe_MemHandle *handle)
{
MPE_UNUSED_PARAM(priority)
return mpeos_memAllocP(color, size, (void **) handle);
}

/**
 * The <i>mpeos_memFreeH</i> frees a block of memory
 * via a handle.
 *
 * @param color Specifies the type or intended use of the memory to free.
 * @param handle Is a handle to the memory block to free.
 *
 * @return If successful, <i>MPE_SUCCESS</i> sould be returned.
 *        Otherwise, is should return an error code.
 */
mpe_Error mpeos_memFreeH(mpe_MemColor color, mpe_MemHandle handle)
{
    return mpeos_memFreeP(color, (void *) handle);
}

/**
 * The <i>mpeos_memRellocH</i> resizes a block of memory
 * via a handle.
 *
 * @param color Specifies the type or intended use of the memory to reallocate.
 * @param size Specifies the size, in bytes, of the memory block to reallocate.
 * @param priority Indicates the priority. Purgeable handles with lower
 *        priority are more likely to be purged than higher.
 * @param handle Is an output handle for returning the handle to the newly
 *        reallocated memory block.
 *
 * @return If successful, <i>MPE_SUCCESS</i> sould be returned along
 *        with a pointer to the new memory block's handle.
 *        Otherwise, is should return an error code.
 */
mpe_Error mpeos_memReallocH(mpe_MemColor color, uint32_t size,
        uint32_t priority, mpe_MemHandle *handle)
{
MPE_UNUSED_PARAM(priority)
return mpeos_memReallocP(color, size, (void ** ) handle);
}

/**
 * The <i>mpeos_memLockH</i> locks or unlocks the data referenced by the handle.
 *
 * @param handle Specifies the handle to a block of memory that should be
 *        considered locked.
 * @param address Is an output pointer to a pointer where the address of a
 *        relevant data block is written following a successful locking
 *        of the data block. If <code>address</code> is <b>NULL</b>, the
 *        previous locking of <code>handle</code> should be reversed. *
 *
 * @return If successful, <i>MPE_SUCCESS</i> sould be returned.
 *        Otherwise, is should return an error code.
 */
mpe_Error mpeos_memLockH(mpe_MemHandle handle, void** address)
{
    if (address != NULL)
    {
        *address = (void *) handle;
    }
    return MPE_SUCCESS;
}

/**
 * The <i>mpeos_memPurge</i> function attempts to purge memory.
 *
 * @param color Specifies the type or intended use of the memory,
 *        to be purged.
 * @param priority The priority of purge candidate handlers.
 *
 * @return If successful, <i>MPE_SUCCESS</i> sould be returned.
 *        Otherwise, is should return an error code.
 */
mpe_Error mpeos_memPurge(mpe_MemColor color, uint32_t priority)
{
MPE_UNUSED_PARAM(color)
MPE_UNUSED_PARAM(priority)
return MPE_SUCCESS;
}

/**
 * The <i>mpeos_memCompact</i> function attempts to compact memory.
 *
 * @param color Specifies the type or intended use of the memory,
 *        depnding on the implementation.
 *
 * @return If successful, <i>MPE_SUCCESS</i> sould be returned.
 *        Otherwise, is should return an error code.
 */
mpe_Error mpeos_memCompact(mpe_MemColor color)
{
MPE_UNUSED_PARAM(color)
return MPE_SUCCESS;
}

/**
 * The <i>mpeos_memStats<i/> ouputs the current internal memory statistics.
 * <p>
 * Low-level interface to memory statistics.  Used by any MPE or
 * java code to trigger a dump of the current memory statistics.
 </p>
 *
 * @param toConsole Flags whether to send the stats to the console or the default
 *        output stream specified in the mpeenv.ini file.
 * @param color Specifies which color statistics to dump (-1 = all).
 * @param label An input pointer to a string to preface the statistical output.
 *        Specifying NULL omits the preface.
 */
void mpeos_memStats(mpe_Bool toConsole, mpe_MemColor color, const char *label)
{
    MPE_UNUSED_PARAM(color);

    static FILE* dumpStream = NULL;
    FILE* out = NULL;
    uint32_t freeSize;

    /* Check for first time call & need to open file stream. */
    if (NULL == dumpStream)
    {
        const char* value;

        /* Check for alternate output path for memory statistics. */
        if (NULL != (value = mpeos_envGet("MPE.MEMPATH")))
        {
            dumpStream = fopen(value, "w");
        }

        /* Assign output stream handle. */
        dumpStream = (NULL != dumpStream) ? dumpStream : stdout;
    }

    /* Check for console specified. */
    out = (TRUE == toConsole) ? stdout : dumpStream;

    /* If there's a label, dump it. */
    if (NULL != label)
        /*lint -e(592)*/fprintf(out, label);

    /* Dump memory stat. */
    if (mpeos_memGetFreeSize(0, &freeSize) == MPE_SUCCESS)
        fprintf(out, "Total free memory: %d\n", freeSize);
}

/**
 * The <i>mpeos_memGetStats</i> copies memory color statistics into an
 * array.
 * <p>
 * Gets the memory statistics for all the colors into an array of
 * mpe_MemStatsInfo structures. Returns MPE_EINVAL if statsSize is not
 * the size of mpe_MemStatsInfo times the constant MPE_MEM_NCOLORS.
 * </p>
 *
 * @param statsSize The number of bytes pointed to by stats. Should always be
 *          sizeof(mpe_MemStatsInfo) * MPE_MEM_NCOLORS. [Input]
 * @param stats A pointer to an array of MPE_MEM_NCOLORS mpe_MemStatsInfo
 *          structures.
 *
 * @return error value or MPE_SUCCESS if successful
 */
mpe_Error mpeos_memGetStats(uint32_t statsSize, mpe_MemStatsInfo *statsArray)
{
    MPE_UNUSED_PARAM(statsSize);
    MPE_UNUSED_PARAM(statsArray);

    // For now the implementation does not support the tracking of memory
    // statistics.
    return MPE_EINVAL;
}

/**
 * The <i>mpeos_memRegisterMemFreeCallback</i> registers a function to call
 * in case of memory exhaustion.
 * <p>
 * Registers a callback which will be called when memory is low and the memory
 * manager needs the various components to release memory.  When memory is low,
 * the memory manager will request that the components free memory, and will ask
 * specificly the amount of memory it's looking for.  The callback function should then
 * attempt to free the memory, and, when finished, return the amount of memory it
 * released. At the very least, the callback should return 0 if unable to free any
 * memory, and non-zero if it successfully freed something.
 * </p>
 *
 * @param color The color this callback will attempt to free.
 * @param function The callback function.
 *
 * @return error value or MPE_SUCCESS if successful
 */
mpe_Error mpeos_memRegisterMemFreeCallback(mpe_MemColor color,
        int32_t(*function)(mpe_MemColor, int32_t, int64_t, void *),
        void *context)
{
    MPE_UNUSED_PARAM(color);
    MPE_UNUSED_PARAM(function);
    MPE_UNUSED_PARAM(context);

    // For now the implementation does not support tracking
    // of low memory conditions.
    return MPE_SUCCESS;
}

/**
 * The <i>mpeos_memUnregisterMemFreeCallback</i> unregisters a callback to
 * called when memory is low.
 * <p>
 * Unregisters a callback which will be called when memory is low and the memory
 * manager needs the various components to release memory.
 * </p><p>
 * color, function, and context must all match a current registration.
 * </p>
 *
 * @param color The color to be unregistered.
 * @param function The callback function to be unregistered.
 * @param context The context to be unregistered.
 *
 * @return error value or MPE_SUCCESS if successful
 */
mpe_Error mpeos_memUnregisterMemFreeCallback(mpe_MemColor color,
        int32_t(*function)(mpe_MemColor, int32_t, int64_t, void *),
        void *context)
{
    MPE_UNUSED_PARAM(color);
    MPE_UNUSED_PARAM(function);
    MPE_UNUSED_PARAM(context);

    // For now the implementation does not support tracking
    // of low memory conditions.
    return MPE_SUCCESS;
}

/**
 * The <i>mpeos_memAllocPProf()</i> function will allocate a block of system memory of
 * the specified size.  The address of the memory block allocated is returned via
 * the pointer.
 *
 * @param color A somewhat loose designation of the type or intended use of the
 *          memory; may or may not be used depending upon implementation.
 *          May map to a distinct heap (for example).
 * @param size Is the size of the memory block to allocate
 * @param memory Is a pointer for returning the pointer to the newly allocated
 *          memory block.
 * @param fileName Is a pointer for the name of the .c file
 * @param lineNum Is the line number in the .c file
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_memAllocPProf(mpe_MemColor color, uint32_t size, void **memory,
        char* fileName, uint32_t lineNum)
{
    /* No profiling right now, just call the non-profiling version. */
    MPE_UNUSED_PARAM(fileName);
    MPE_UNUSED_PARAM(lineNum);

    return mpeos_memAllocP(color, size, memory);
}

/**
 * The <i>mpeos_memFreePProf()</i> function will free the specified block of system
 * memory.
 *
 * @param color The original color specified on allocation
 * @param memory Is a pointer to the memory block to free.
 * @param fileName Is a pointer for the name of the .c file
 * @param lineNum Is the line number in the .c file
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_memFreePProf(mpe_MemColor color, void *memory, char* fileName,
        uint32_t lineNum)
{
    /* No profiling right now, just call the non-profiling version. */
    MPE_UNUSED_PARAM(fileName);
    MPE_UNUSED_PARAM(lineNum);

    return mpeos_memFreeP(color, memory);
}

/**
 * The <i>mpeos_memReallocPProf()</i> function will resize the specified block of
 * memory to the specified size.
 *
 * @param color A somewhat loose designation of the type or intended use of the
 *          memory; may or may not be used depending upon implementation.
 *          May map to a distinct heap (for example).
 * @param size Is the size to resize to.
 * @param memory Is a pointer to the pointer to the memory block to resize.  The
 *          new memory block pointer is returned via this pointer.
 * @param fileName Is a pointer for the name of the .c file
 * @param lineNum Is the line number in the .c file
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_memReallocPProf(mpe_MemColor color, uint32_t size,
        void **memory, char* fileName, uint32_t lineNum)
{
    /* No profiling right now, just call the non-profiling version. */
    MPE_UNUSED_PARAM(fileName);
    MPE_UNUSED_PARAM(lineNum);

    return mpeos_memReallocP(color, size, memory);
}
