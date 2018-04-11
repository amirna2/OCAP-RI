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
#include "mock_sync.h"

#include <mpeos_dbg.h>

#include <os_thread.h>

#include <windows.h>

mpe_Error mock_timeGetMillis(mpe_TimeMillis *ptime);
mpe_Error mock_memAllocP(mpe_MemColor color, uint32_t size, void **mem);
mpe_Error mock_memFreeP(mpe_MemColor color, void *mem);


/**
 * The <i>mock_mutexNew()</i> function will create a new mutex.
 *
 * @param mutex Is a pointer for returning the identifier of the new mutex.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mock_mutexNew(mock_Mutex *mutex)
{
    CRITICAL_SECTION *crit;

    /* Sanity check... */
    if (mutex == NULL)
    {
        return MPE_EINVAL;
    }

    if (mock_memAllocP(MPE_MEM_SYNC, sizeof(CRITICAL_SECTION), (void **) &crit)
            != MPE_SUCCESS)
    {
        return MPE_ENOMEM;
    }

    /* Utilize windows critical sections for mutex functionality. */
    InitializeCriticalSection(crit);

    *mutex = (mock_Mutex) crit;

    return MPE_SUCCESS;
}

/**
 * The <i>mock_mutexDelete()</i> function will destroy a previously created mutex.
 *
 * @param mutex Is the identifier of the mutex to destroy.
 *
 * @return The MPE error code if the destroy fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mock_mutexDelete(mock_Mutex mutex)
{
    /* Sanity check... */
    if (mutex == NULL)
    {
        return MPE_EINVAL;
    }

    DeleteCriticalSection(mutex);

    mock_memFreeP(MPE_MEM_SYNC, mutex);

    return MPE_SUCCESS;
}

/**
 * The <i>mock_mutexAqcuire()</i> function will acquire ownership of the target
 * mutex.  If the mutex is already owned by another thread, the calling thread will
 * be suspended within a priority based queue until the mutex is free for this
 * thread's acquisition.
 *
 * @param mutex Is the identifier of the mutex to acquire.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mock_mutexAcquire(mock_Mutex mutex)
{
    EnterCriticalSection(mutex);

    return MPE_SUCCESS;
}

/**
 * The <i>mock_mutexAcquireTry()</i> function will attempt to acquire ownership of
 * the target mutex without blocking.  If the mutex is busy an error will be returned
 * to indicate failure to acquire the mutex.
 *
 * @param mutex Is the identifier of the mutex to acquire.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mock_mutexAcquireTry(mock_Mutex mutex)
{
    if (TryEnterCriticalSection(mutex))
    {
        return MPE_SUCCESS;
    }

    return MPE_EMUTEX;
}

/**
 * The <i>mock_mutexRelease()</i> function will release ownership of a mutex.  The
 * current thread must own the mutex in order to release it.
 *
 * @param mutex Is the identifier of the mutex to release.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mock_mutexRelease(mock_Mutex mutex)
{
    LeaveCriticalSection(mutex);

    return MPE_SUCCESS;
}

/*
 * Condition API prototypes:
 */

/**
 * The <i>mock_condNew()</i> function will create a new condition synchronization
 * object.
 *
 * @param autoReset Is a boolean variable that indicated whether the new
 *          condition is an "autoreset" condition object, in which case the
 *          condition object automatically resets to the unset (FALSE) state
 *          when a thread acquires it using the <i>mpe_condGet()<i/> function.
 *          If autoReset is TRUE, the condition object is automatically reset to
 *          the unset (FALSE) condition.
 * @param initialState Is a boolean variable that specifies the initial state of
 *          the condition object. The condition object is in the set state if
 *          <i>initialState<i/> is TRUE and in the unset state if <i>initialState<i/>
 *          is FALSE.
 * @param cond Is a pointer for returning the new condition identifier used for
 *          subsequent condition variable operations.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mock_condNew(mpe_Bool autoReset, mpe_Bool initialState,
        mock_Cond *cond)
{
    mpe_Error err;
    os_Condition newCond;
    
    /* Sanity check... */
    if (cond == NULL)
    {
        return MPE_EINVAL;
    }

    /* Allocate our cond structure and create mutex */
    if ((err = mock_memAllocP(MPE_MEM_SYNC, sizeof(_os_Condition), (void **) &newCond))
            != MPE_SUCCESS)
    {
        return err;
    }
    if ((err = mock_mutexNew(&newCond->mutex)) != MPE_SUCCESS)
    {
        return err;
    }

    /* Utilize windows Events to implement condition functionality. */
    newCond->hnd = CreateEvent(NULL, !autoReset, initialState, NULL);

    /*  Initialize autoReset and initialState */
    newCond->autoReset = autoReset;
    newCond->state = initialState;

    *cond = newCond;
    return MPE_SUCCESS;
}

/**
 * The <i>mock_condDelete()</i> function will destroy a condition object.
 *
 * @param cond Is the identifier of the target condition to destroy.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mock_condDelete(mock_Cond cond)
{
    /* Sanity check... */
    if (cond == NULL)
    {
        return MPE_EINVAL;
    }

    if (!CloseHandle((HANDLE)cond->hnd))
    {
        return MPE_ECOND;
    }
    mock_mutexDelete(cond->mutex);
    mock_memFreeP(MPE_MEM_SYNC, cond);

    return MPE_SUCCESS;
}

/**
 * The <i>mock_condGet()</i> function will get exclusive access of the specified
 * condition object.  If the condition object is in the FALSE state at the time of
 * the call the calling thread is suspended until the condition is set to the
 * TRUE state.
 *
 * @param cond Is the identifier of the target condition object to acquire.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mock_condGet(mock_Cond cond)
{
    return mock_condWaitFor(cond,0);
}

/**
 * The <i>mock_condWaitFor()</i> function will attempt to get exclusive access
 * of the specified condition object.  If the condition object is in the FALSE state
 * at the time of the call the calling thread is suspended for a maximum period of
 * time as specified by the time out parameter until the condition is set to the
 * TRUE state.
 *
 * @param cond Is the identifier of the target condition object to acquire.
 * @param timeout Is the maximum time in milliseconds to wait for condition object
 *          to become TRUE. Alternatively a timeout of 0 (zero) will indicate to
 *          wait until the condition becomes TRUE.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mock_condWaitFor(mock_Cond cond, uint32_t timeout)
{
    // We have seen cases where WaitForSingleObject will wake up
    // a thread before the object has been signaled, so we have to
    // have this extra protection
    
    mock_mutexAcquire(cond->mutex);
    mpe_TimeMillis elapsed = 0;
    
    while (TRUE)
    {
        mock_mutexRelease(cond->mutex);

        if (timeout == 0)
        {
            WaitForSingleObject((HANDLE)cond->hnd, INFINITE);
        }
        else
        {
            mpe_TimeMillis time1;
            mpe_TimeMillis time2;
            mock_timeGetMillis(&time1);
            WaitForSingleObject((HANDLE)cond->hnd, timeout - elapsed);
            mock_timeGetMillis(&time2);
            
            elapsed += time2 - time1;
        }
        
        mock_mutexAcquire(cond->mutex);

        // Check to see if we've timed out
        if (timeout > 0 && elapsed >= timeout)
        {
            break;
        }
        // Check our cond var state. If set, break out of the loop.
        // Also reset the state if necessary
        else if (cond->state)
        {
            if (cond->autoReset)
            {
                cond->state = FALSE;
            }
            break;
        }
    }

    mock_mutexRelease(cond->mutex);

    return MPE_SUCCESS;
}

/**
 * The <i>mock_condSet()</i> function will set the condition variable to TRUE
 * state and activate the first thread waiting.
 *
 * @param cond Is the identifier of the target condition object to set.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mock_condSet(mock_Cond cond)
{
    mock_mutexAcquire(cond->mutex);
    
    cond->state = TRUE;
    SetEvent((HANDLE)cond->hnd);

    mock_mutexRelease(cond->mutex);

    return MPE_SUCCESS;
}

/**
 * The <i>mock_condUnset()</i> function will set the condition variable to the
 * FALSE state without modifying ownership of the condition variable???
 *
 * @param cond Is the identifier of the target condition object to unset.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mock_condUnset(mock_Cond cond)
{
    mock_mutexAcquire(cond->mutex);
    
    cond->state = FALSE;
    (void) ResetEvent((HANDLE)cond->hnd);

    mock_mutexRelease(cond->mutex);

    return MPE_SUCCESS;
}


// GORP: Note that this is the WIndows version of this method
mpe_Error mock_timeGetMillis(mpe_TimeMillis * pMpeTimeMillis)
{
    // Milliseconds between Jan 1 1601 00:00 and Jan 1 1970 00:00
#define WIN32_POSIX_TIME_OFF_MILLIS 0xa9730b66800LL

    static CRITICAL_SECTION g_targTimeMillis_lock;

    static mpe_Bool g_targTimeMillis_lock_initialised = FALSE;
    static LONGLONG millisSinceEpoch = 0;
    static LONGLONG lastTickSynch = 0;

    LONGLONG millisSinceSynch = 0;
    FILETIME f;

    if (pMpeTimeMillis == NULL)
        return MPE_EINVAL;

    if (!g_targTimeMillis_lock_initialised)
    {
        InitializeCriticalSection(&g_targTimeMillis_lock);
        g_targTimeMillis_lock_initialised = TRUE;
    }

    EnterCriticalSection(&g_targTimeMillis_lock);

    if (millisSinceEpoch) // In synch
    {
        LONGLONG millisSinceStart;

        // Get the number of milliseconds since the program start
        millisSinceStart = GetTickCount();

        // Calculate the number of milliseconds since the last synch
        millisSinceSynch = millisSinceStart - lastTickSynch;

        // Calculate the result
        *pMpeTimeMillis = millisSinceEpoch + millisSinceSynch;

        if (millisSinceSynch < 60000) // if less than 60 seconds then done
        {
            LeaveCriticalSection(&g_targTimeMillis_lock);
            return MPE_SUCCESS;
        }
    }

    // Synch the system time and the performance counter
    GetSystemTimeAsFileTime(&f);
    lastTickSynch = GetTickCount();
    millisSinceEpoch = (((((LONGLONG) f.dwHighDateTime) << 32)
            | f.dwLowDateTime) / 10000 - WIN32_POSIX_TIME_OFF_MILLIS);

    if (millisSinceSynch && (*pMpeTimeMillis > millisSinceEpoch)) // Went back in time!
        millisSinceEpoch = *pMpeTimeMillis; // Don't allow time reversal

    *pMpeTimeMillis = millisSinceEpoch;

    LeaveCriticalSection(&g_targTimeMillis_lock);

    return MPE_SUCCESS;
}

/**
 * The <i>mock_memAllocP()</i> function will allocate a block of system memory of
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
mpe_Error mock_memAllocP(mpe_MemColor color, uint32_t size, void **mem)
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
        retval = MPE_ENOMEM;
    }

    return retval;
}

/**
 * The <i>mock_memFreeP()</i> function will free the specified block of system
 * memory.
 *
 * @param color The original color specified on allocation
 * @param memory Is a pointer to the memory block to free.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mock_memFreeP(mpe_MemColor color, void *mem)
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

