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

#include <sys/types.h> /* getpid(2) */
#include <unistd.h>    /* getpid(2) */
#include <errno.h>     /* EBUSY, EINVAL, EPERM */
#include <stdio.h>     /* stderr */
#include <sys/time.h>  /* stderr */
#include <pthread.h>

#include <mpe_error.h>
#include <mpeos_sync.h>
#include <mpeos_mem.h>
#include <mpeos_dbg.h>

#include <os_thread.h>

#define MPE_MEM_DEFAULT MPE_MEM_SYNC

/**
 * <i>mpeos_mutexNew()<i/> 
 *
 * Create a new mutext object.  The PTV mutex is created to allow for nested
 * calling by the same thread (i.e. once a thread has acquired exclusive
 * access to the mutex it can repeatedly acquire it without blocking).
 *
 * @param mutex is a pointer for returning the identifier of the new mutex.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *         is returned.
 */
mpe_Error mpeos_mutexNew(mpe_Mutex *mutex)
{
    static pthread_mutexattr_t attr;
    auto int ec = 0;

    if (NULL == mutex)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_MUTEX,
                "mpeos_mutexNew() - mutex NULL returning\n");
        return MPE_EINVAL;
    }

    if ((ec = mpeos_memAllocP(MPE_MEM_DEFAULT, sizeof(pthread_mutex_t),
            (void**) mutex)) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_COND,
                "mpeos_mutexNew() - failed, no memory.\n");
        return ec;
    }

    /***
     *** TODO - do we need to set attributes?
     ***/
    pthread_mutexattr_init(&attr);
    pthread_mutexattr_settype(&attr, PTHREAD_MUTEX_RECURSIVE);
    pthread_mutex_init((pthread_mutex_t *) *mutex, &attr);

    return MPE_SUCCESS;
}

/**
 * <i>mpeos_mutexDelete()</i> 
 *
 * Destroy a previously created mutex.
 *
 * @param mutex is the identifier of the mutex to destroy.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *         is returned.
 */
mpe_Error mpeos_mutexDelete(mpe_Mutex mutex)
{
    int pRet;

    pRet = pthread_mutex_destroy(mutex);
    if (0 != pRet)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MUTEX,
                "mpe_mutexDelete() - failed: %d\n", pRet);
        return MPE_EINVAL;
    }
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_MUTEX,
            "mpeos_mutexDelete() - succeeded.\n");

    mpeos_memFreeP(MPE_MEM_DEFAULT, mutex);

    return MPE_SUCCESS;
}

/**
 * <i>mpeos_mutexAcquire()</i> 
 *
 * Acquire ownership of the target mutex.  If the mutex is already owned by 
 * another thread, the calling thread will be suspended within a priority based 
 * queue until the mutex is free for this thread's acquisition.
 *
 * @param mutex is the identifier of the mutex to acquire.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *         is returned.
 */
mpe_Error mpeos_mutexAcquire(mpe_Mutex mutex)
{
    ThreadDesc* td = NULL;

    if (mutex == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MUTEX,
                "mpe_mutexAcquire() - mutex null invalid value\n");
        return MPE_EINVAL;
    }

    mpeos_threadGetCurrent(&td);

#ifdef MPE_FEATURE_THREAD_SUSPEND
    while (1)
    {
        int pRet;
        if (td != NULL)
        td->td_blocked = TRUE;
        if ((pRet = pthread_mutex_lock(mutex)) != 0)
        {
            if (td != NULL)
            td->td_blocked = FALSE;
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MUTEX,
                    "mpe_mutexAcquire() - failed: %d\n", pRet);
            return MPE_EINVAL;
        }
        if (td != NULL)
        {
            td->td_blocked = FALSE;
            if (td->td_suspended)
            {
                pthread_mutex_unlock(mutex);
                suspendMe();
            }
            else
            break;
        }
        else
        break;
    }
#else
    pthread_mutex_lock(mutex);
#endif

    return MPE_SUCCESS;
}

/**
 * <i>mpeos_mutexAcquireTry()</i> 
 *
 * Attempt to acquire ownership of the target mutex without blocking.  If the 
 * mutex is busy an error will be returned to indicate failure to acquire the 
 * mutex.
 *
 * @param mutex is the identifier of the mutex to acquire.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *         is returned.
 */
mpe_Error mpeos_mutexAcquireTry(mpe_Mutex mutex)
{
    int pRet = pthread_mutex_trylock(mutex);

    if (EBUSY == pRet)
    {
        MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_MUTEX,
                "mpe_mutexAcquireTry() - failed, mutex busy.\n");
        return MPE_EBUSY;
    }
    else if (EINVAL == pRet)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MUTEX,
                "mpe_mutexAcquireTry() - failed, invalid mutex.\n");
        return MPE_EINVAL;
    }
    else if (0 != pRet)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MUTEX,
                "mpe_mutexAcquireTry() - failed: %d\n", pRet);
        return MPE_EINVAL;
    }
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_mutexRelease()</i> 
 *
 * Release ownership of a mutex.  The current thread must own the mutex in order 
 * to release it.
 *
 * @param mutex is the identifier of the mutex to release.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_mutexRelease(mpe_Mutex mutex)
{
    int pRet;

    if (mutex == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MUTEX,
                "mutex is NULL return invalid params\n");
        return MPE_EINVAL;
    }

    pRet = pthread_mutex_unlock(mutex);
    if (EINVAL == pRet)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MUTEX,
                "mpe_mutexRelease() - failed, invalid mutex.\n");
        return MPE_EINVAL;
    }
    else if (EPERM == pRet)
    {
        MPEOS_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_MUTEX,
                "mpe_mutexRelease() - failed, caller (PID %d TID %lu) does not own mutex.\n",
                getpid(), pthread_self());
        return MPE_EINVAL;
    }
    else if (0 != pRet)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MUTEX,
                "mpe_mutexRelease() - failed: %d.\n", pRet);
        return MPE_EINVAL;
    }
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_condNew()</i> 
 *
 * Create a new condition synchronization object.
 *
 * @param autoReset is a boolean variable that indicated whether the new 
 *          condition is an "autoreset" condition object, in which case the 
 *          condition object automatically resets to the unset (FALSE) state 
 *          when a thread acquires it using the <i>mpe_condGet()<i/> function. 
 *          If autoReset is TRUE, the condition object is automatically reset to 
 *          the unset (FALSE) condition.
 * @param initialState is a boolean variable that specifies the initial state of 
 *          the condition object. The condition object is in the set state if 
 *          <i>initialState<i/> is TRUE and in the unset state if <i>initialState<i/> 
 *          is FALSE.
 * @param condition is a pointer for returning the new condition variable.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_condNew(mpe_Bool autoReset, mpe_Bool initialState,
        mpe_Cond *condition)
{
    mpe_Error ec;
    struct os_Cond_s *cond;
    int pRet;

    if (NULL == condition)
    {
        return MPE_EINVAL;
    }

    if ((ec = mpeos_memAllocP(MPE_MEM_DEFAULT, sizeof(struct os_Cond_s),
            (void**) &cond)) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_COND,
                "mpeos_condNew() - failed, no memory.\n");
        return ec;
    }

    cond->cd_id = OS_COND_ID;
    cond->cd_autoReset = autoReset;
    cond->cd_state = initialState;

    ec = mpeos_threadGetCurrent(&cond->cd_owner);
    if (MPE_SUCCESS != ec)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_COND,
                "mpeos_condNew() - failed condition: %p. (ec: %d)\n", cond, ec);
        return MPE_EINVAL;
    }

    mpeos_mutexNew(&(cond->cd_mutex));

    // no LINUX support for pthread_condattr_t
    pRet = pthread_cond_init(&(cond->cd_cond), NULL);
    if (0 != pRet)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_COND,
                "mpeos_condNew() - failed condition: %p. (pRet: %d)\n", cond,
                pRet);
        return MPE_EINVAL;
    }

    *condition = (mpe_Cond) cond;
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_condDelete()</i> 
 *
 * Destroy a condition object.
 *
 * @param cond is the identifier of the target condition to destroy.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_condDelete(mpe_Cond cond)
{
    int pRet;

    if ((NULL == cond) || (OS_COND_ID != cond->cd_id))
    {
        return MPE_EINVAL;
    }

    cond->cd_id = 0; /* Flag condition no longer functional. */

    /*
     * There is always the potential for a race condition with any thread
     * that might be in the process of calling mpeos_condSet or one of the
     * other condition APIs that utilizes the internal lock.  Hence, after
     * having marked the condition variable as no longer valid we only need
     * to worry about any threads that are already in one of the mpeos_cond
     * APIs (having verified a valid condition object).  To overcome this
     * situation, any threads will be activated until there are no owners.
     */
    while (cond->cd_owner != 0)
    {
        cond->cd_owner = 0;
        (void) pthread_cond_signal(&(cond->cd_cond));
        sleep(1);
    }

    pRet = pthread_cond_destroy(&(cond->cd_cond));
    if (0 != pRet)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_COND,
                "mpeos_condDelete(%p: failed: %d\n", cond, pRet);
        return MPE_EINVAL;
    }

    mpeos_mutexDelete(cond->cd_mutex);

    mpeos_memFreeP(MPE_MEM_DEFAULT, cond);
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_COND, "mpeos_condDelete(%p: succeeded.\n",
            cond);
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_condGet()</i> 
 *
 * Get exclusive access of the specified condition object.  If the condition 
 * object is in the FALSE state at the time of the call the calling thread is 
 * suspended until the condition is set to the TRUE state.
 *
 * @param cond is the identifier of the target condition object to acquire.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_condGet(mpe_Cond cond)
{
    ThreadDesc* td = NULL;

    if ((NULL == cond) || (cond->cd_id != OS_COND_ID))
    {
        return MPE_EINVAL;
    }

    mpeos_threadGetCurrent(&td);

    if (mpeos_mutexAcquire(cond->cd_mutex) == 0)
    {
        // If condition state is TRUE, it can go ahead
        while (cond->cd_state == FALSE)
        {
#ifdef MPE_FEATURE_THREAD_SUSPEND
            if (td != NULL)
            td->td_waiting = TRUE;
#endif
            pthread_cond_wait(&cond->cd_cond, cond->cd_mutex);
#ifdef MPE_FEATURE_THREAD_SUSPEND
            if (td != NULL)
            td->td_waiting = FALSE;
#endif
        }
        if (cond->cd_autoReset)
            cond->cd_state = FALSE;
        mpeos_mutexRelease(cond->cd_mutex);
    }

#ifdef MPE_FEATURE_THREAD_SUSPEND
    if (td != NULL && td->td_suspended)
    {
        suspendMe();
    }
#endif

    return MPE_SUCCESS;
}

/**
 * <i>mpeos_condWaitFor()</i> 
 *
 * Attempt to get exclusive access of the specified condition object.  If the 
 * condition object is in the FALSE state at the time of the call the calling 
 * thread is suspended for a maximum period of time as specified by the time 
 * out parameter until the condition is set to the TRUE state.
 *
 * @param cond is the identifier of the target condition object to acquire.
 * @param timeout is the maximum time in milliseconds to wait for condition object 
 *          to become TRUE.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_condWaitFor(mpe_Cond cond, uint32_t timeout)
{
    mpe_Error ec = MPE_SUCCESS;
    int retval = 0;
    ThreadDesc* td = NULL;

    if ((NULL == cond) || (cond->cd_id != OS_COND_ID))
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_COND, "mpeos_condWaitFor() - failed\n");
        return MPE_EINVAL;
    }

    mpeos_threadGetCurrent(&td);

    if (mpeos_mutexAcquire(cond->cd_mutex) == 0)
    {
        if (timeout == 0)
        {
            while (cond->cd_state == FALSE)
            {
                // indefinite wait
#ifdef MPE_FEATURE_THREAD_SUSPEND
                if (td != NULL)
                td->td_waiting = TRUE;
#endif
                pthread_cond_wait(&(cond->cd_cond), cond->cd_mutex);
#ifdef MPE_FEATURE_THREAD_SUSPEND
                if (td != NULL)
                td->td_waiting = FALSE;
#endif
            }
        }
        else
        {
            // Timed wait.  Convert relative time parameter to absolute
            // time
            struct timespec time;
            struct timeval curTime;
            gettimeofday(&curTime, NULL);
            time.tv_nsec = curTime.tv_usec * 1000 + (timeout % 1000) * 1000000;
            time.tv_sec = curTime.tv_sec + (timeout / 1000);
            if (time.tv_nsec > 1000000000)
            {
                time.tv_nsec -= 1000000000;
                time.tv_sec++;
            }

            while (cond->cd_state == FALSE)
            {
#ifdef MPE_FEATURE_THREAD_SUSPEND
                if (td != NULL)
                td->td_waiting = TRUE;
#endif
                retval = pthread_cond_timedwait(&(cond->cd_cond),
                        cond->cd_mutex, &time);
#ifdef MPE_FEATURE_THREAD_SUSPEND
                if (td != NULL)
                td->td_waiting = FALSE;
#endif
                // Return an error if the condition is still false and we
                // have a non-zero error code
                if (retval != 0 && cond->cd_state == FALSE)
                {
                    ec = (retval == ETIMEDOUT) ? MPE_EBUSY : MPE_EINVAL;
                    break;
                }
            }
        }
        if (cond->cd_autoReset)
            cond->cd_state = FALSE;
        mpeos_mutexRelease(cond->cd_mutex);
    }

#ifdef MPE_FEATURE_THREAD_SUSPEND
    if (td != NULL && td->td_suspended)
    {
        suspendMe();
    }
#endif

    return ec;
}

/**
 * <i>mpeos_condSet()</i> 
 *
 * Set the condition variable to TRUE state and activate the first thread waiting.
 *
 * @param cond is the identifier of the target condition object to set.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_condSet(mpe_Cond cond)
{
    if ((NULL == cond) || (cond->cd_id != OS_COND_ID))
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_COND, "mpeos_condSet() - failed\n");
        return MPE_EINVAL;
    }

    if (mpeos_mutexAcquire(cond->cd_mutex) == 0)
    {
        /* set the condition code and wake up the waiting thread */
        cond->cd_state = TRUE;
        pthread_cond_signal(&(cond->cd_cond));
        mpeos_mutexRelease(cond->cd_mutex);
    }
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_condUnset()</i> 
 *
 * Unset the condition variable to the FALSE state without modifying ownership 
 * of the condition variable.
 *
 * @param cond is the identifier of the target condition object to unset.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_condUnset(mpe_Cond cond)
{
    if ((NULL == cond) || (cond->cd_id != OS_COND_ID))
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_COND, "mpeos_condUnset() - failed\n");
        return MPE_EINVAL;
    }
    if (mpeos_mutexAcquire(cond->cd_mutex) == 0)
    {
        cond->cd_state = FALSE;
        mpeos_mutexRelease(cond->cd_mutex);
    }
    return MPE_SUCCESS;
}

