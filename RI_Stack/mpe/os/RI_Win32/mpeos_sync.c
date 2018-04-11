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
#include <mpeos_sync.h>

#include <mpeos_dbg.h>

#include <os_thread.h>

/**
 * The <i>mpeos_mutexNew()</i> function will create a new mutex.
 *
 * @param mutex Is a pointer for returning the identifier of the new mutex.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_mutexNew(mpe_Mutex *mutex)
{
    CRITICAL_SECTION *crit;

    /* Sanity check... */
    if (mutex == NULL)
    {
        return MPE_EINVAL;
    }

    if (mpeos_memAllocP(MPE_MEM_SYNC, sizeof(CRITICAL_SECTION), (void **) &crit)
            != MPE_SUCCESS)
    {
        return MPE_ENOMEM;
    }

    /* Utilize windows critical sections for mutex functionality. */
    InitializeCriticalSection(crit);

    *mutex = (mpe_Mutex) crit;

#ifdef DO_MUTEX_TRACKING
    newMutex( (DWORD) crit );
#endif

    return MPE_SUCCESS;
}

/**
 * The <i>mpeos_mutexDelete()</i> function will destroy a previously created mutex.
 *
 * @param mutex Is the identifier of the mutex to destroy.
 *
 * @return The MPE error code if the destroy fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_mutexDelete(mpe_Mutex mutex)
{
    /* Sanity check... */
    if (mutex == NULL)
    {
        return MPE_EINVAL;
    }

#ifdef DO_MUTEX_TRACKING
    deleteMutex( (DWORD) mutex );
#endif

    DeleteCriticalSection(mutex);

    mpeos_memFreeP(MPE_MEM_SYNC, mutex);

    return MPE_SUCCESS;
}

/**
 * The <i>mpeos_mutexAqcuire()</i> function will acquire ownership of the target
 * mutex.  If the mutex is already owned by another thread, the calling thread will
 * be suspended within a priority based queue until the mutex is free for this
 * thread's acquisition.
 *
 * @param mutex Is the identifier of the mutex to acquire.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_mutexAcquire(mpe_Mutex mutex)
{
#ifdef MPE_FEATURE_THREAD_SUSPEND
    threadDesc* td = (threadDesc*)TlsGetValue(TLS_INDEX);

    if (td != NULL)
    {
        td->is_blocked = TRUE;

        if (td->is_suspended &&
                !td->is_suspended_while_waiting && ! td->is_suspended_while_blocked)
        {
            DWORD suspendCount;

            // Make sure the thread is fully resumed, no matter how many times
            // it was suspended
            while ( ((suspendCount = ResumeThread(td->handle)) > 1) && (suspendCount != 0xFFFFFFFF) ); // BOZO should this be (DWORD)-1 ?

            EnterCriticalSection(td->mutex);
            td->is_suspended_while_blocked = TRUE;

            if (!td->is_suspended)
            td->is_suspended_while_blocked = FALSE;

            LeaveCriticalSection(td->mutex);
        }

        while (1)
        {
            // As long as we are supposed to be suspended, wait
            // on our cond var
            EnterCriticalSection(mutex);
            if (td->is_suspended_while_blocked)
            {

                LeaveCriticalSection(mutex);
                WaitForSingleObject((HANDLE)td->suspend_cond->hnd,INFINITE);
            }
            else
            break;
        }
        td->is_blocked = FALSE;
    }
    else
    {
        EnterCriticalSection(mutex);
    }
#else
    EnterCriticalSection(mutex);
#endif

#ifdef DO_MUTEX_TRACKING
    setMutexOwner( (DWORD) mutex, GetCurrentThreadId() );
#endif

    return MPE_SUCCESS;
}

/**
 * The <i>mpeos_mutexAcquireTry()</i> function will attempt to acquire ownership of
 * the target mutex without blocking.  If the mutex is busy an error will be returned
 * to indicate failure to acquire the mutex.
 *
 * @param mutex Is the identifier of the mutex to acquire.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_mutexAcquireTry(mpe_Mutex mutex)
{
    if (TryEnterCriticalSection(mutex))
    {
#ifdef DO_MUTEX_TRACKING
        setMutexOwner( (DWORD) mutex, GetCurrentThreadId() );
#endif
        return MPE_SUCCESS;
    }

    return MPE_EMUTEX;
}

/**
 * The <i>mpeos_mutexRelease()</i> function will release ownership of a mutex.  The
 * current thread must own the mutex in order to release it.
 *
 * @param mutex Is the identifier of the mutex to release.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_mutexRelease(mpe_Mutex mutex)
{
#ifdef DO_MUTEX_TRACKING
    clearMutexOwner( (DWORD) mutex, GetCurrentThreadId() );
#endif

    LeaveCriticalSection(mutex);

#ifdef MPE_FEATURE_THREAD_SUSPEND
    {
        threadDesc* td = TlsGetValue(TLS_INDEX);
        if (td != NULL && td->is_suspended)
        {
            if (ResumeThread(td->handle) >= 1)
            td->is_suspended = TRUE;
        }
    }
#endif

    return MPE_SUCCESS;
}

/*
 * Condition API prototypes:
 */

/**
 * The <i>mpeos_condNew()</i> function will create a new condition synchronization
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
mpe_Error mpeos_condNew(mpe_Bool autoReset, mpe_Bool initialState,
        mpe_Cond *cond)
{
    mpe_Error err;
    os_Condition newCond;
    
    /* Sanity check... */
    if (cond == NULL)
    {
        return MPE_EINVAL;
    }

    /* Allocate our cond structure and create mutex */
    if ((err = mpeos_memAllocP(MPE_MEM_SYNC, sizeof(_os_Condition), (void **) &newCond))
            != MPE_SUCCESS)
    {
        return err;
    }
    if ((err = mpeos_mutexNew(&newCond->mutex)) != MPE_SUCCESS)
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
 * The <i>mpeos_condDelete()</i> function will destroy a condition object.
 *
 * @param cond Is the identifier of the target condition to destroy.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_condDelete(mpe_Cond cond)
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
    mpeos_mutexDelete(cond->mutex);
    mpeos_memFreeP(MPE_MEM_SYNC, cond);

    return MPE_SUCCESS;
}

/**
 * The <i>mpeos_condGet()</i> function will get exclusive access of the specified
 * condition object.  If the condition object is in the FALSE state at the time of
 * the call the calling thread is suspended until the condition is set to the
 * TRUE state.
 *
 * @param cond Is the identifier of the target condition object to acquire.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_condGet(mpe_Cond cond)
{
    return mpeos_condWaitFor(cond,0);
}

/**
 * The <i>mpeos_condWaitFor()</i> function will attempt to get exclusive access
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
mpe_Error mpeos_condWaitFor(mpe_Cond cond, uint32_t timeout)
{
#ifdef MPE_FEATURE_THREAD_SUSPEND
    threadDesc* td = (threadDesc*)TlsGetValue(TLS_INDEX);
    if (td != NULL)
    {
        EnterCriticalSection(td->mutex);
        td->is_waiting = TRUE;
        LeaveCriticalSection(td->mutex);
    }
#endif

    // We have seen cases where WaitForSingleObject will wake up
    // a thread before the object has been signaled, so we have to
    // have this extra protection
    
    mpeos_mutexAcquire(cond->mutex);
    mpe_TimeMillis elapsed = 0;
    
    while (TRUE)
    {
        mpeos_mutexRelease(cond->mutex);

        if (timeout == 0)
        {
            WaitForSingleObject((HANDLE)cond->hnd, INFINITE);
        }
        else
        {
            mpe_TimeMillis time1;
            mpe_TimeMillis time2;
            mpeos_timeGetMillis(&time1);
            WaitForSingleObject((HANDLE)cond->hnd, timeout - elapsed);
            mpeos_timeGetMillis(&time2);
            
            elapsed += time2 - time1;
        }
        
        mpeos_mutexAcquire(cond->mutex);

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

    mpeos_mutexRelease(cond->mutex);

#ifdef MPE_FEATURE_THREAD_SUSPEND
    if (td != NULL)
    {
        EnterCriticalSection(td->mutex);
        td->is_waiting = FALSE;

        // As long as we are supposed to be suspended, wait on our
        // condition var
        while (td->is_suspended)
        {
            LeaveCriticalSection(td->mutex);
            mpeos_condGet(td->suspend_cond);
            EnterCriticalSection(td->mutex);
        }

        LeaveCriticalSection(td->mutex);
    }
#endif

    return MPE_SUCCESS;
}

/**
 * The <i>mpeos_condSet()</i> function will set the condition variable to TRUE
 * state and activate the first thread waiting.
 *
 * @param cond Is the identifier of the target condition object to set.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_condSet(mpe_Cond cond)
{
    mpeos_mutexAcquire(cond->mutex);
    
    cond->state = TRUE;
    SetEvent((HANDLE)cond->hnd);

    mpeos_mutexRelease(cond->mutex);

    return MPE_SUCCESS;
}

/**
 * The <i>mpeos_condUnset()</i> function will set the condition variable to the
 * FALSE state without modifying ownership of the condition variable???
 *
 * @param cond Is the identifier of the target condition object to unset.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_condUnset(mpe_Cond cond)
{
    mpeos_mutexAcquire(cond->mutex);
    
    cond->state = FALSE;
    (void) ResetEvent((HANDLE)cond->hnd);

    mpeos_mutexRelease(cond->mutex);

    return MPE_SUCCESS;
}
