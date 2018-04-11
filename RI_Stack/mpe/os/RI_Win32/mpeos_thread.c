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

/*
 * This file provides the CableLabs Reference Implementation of the mpeos_thread APIs.
 * Every thread will have a "thread descriptor" that is used to contain its
 * vitals (e.g. MPE specific data, MPE thread status/control variable,
 * thread termination jump buffer, etc).
 *
 */

/* Header Files */
#include <mpe_error.h>
#include <mpe_types.h>
#include <mpeos_sync.h>
#include <mpeos_thread.h>
#include <mpeos_mem.h>
#include <mpeos_util.h>
#include <mpe_dbg.h>
#include <errno.h>

#include <windows.h>
#include <process.h>

DWORD TLS_INDEX = TLS_OUT_OF_INDEXES;

/**
 * <i>threadAllocDesc()<i/>
 *
 * Allocate a new thread descriptor structure and allocate fields with
 * default values.
 *
 * @param td Is a pointer for returning the new thread descriptor pointer.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *         is returned.
 */
static mpe_Error threadAllocDesc(threadDesc **td)
{
    mpe_Error ec;
    threadDesc* thread;

    if (td == NULL)
        return MPE_EINVAL;

    /* Allocate a new thread local storage structure. */
    if ((ec = mpeos_memAllocP(MPE_MEM_THREAD, sizeof(threadDesc),
            (void **) &thread)) != MPE_SUCCESS)
    {
        return ec;
    }

    /* Allocate room for local thread data */
    if ((ec = mpeos_memAllocP(MPE_MEM_THREAD, sizeof(mpe_ThreadPrivateData),
            (void **) &thread->locals)) != MPE_SUCCESS)
    {
        mpeos_memFreeP(MPE_MEM_THREAD, thread);
        return ec;
    }

    /* Initialize fields. */
    thread->status = 0;
    thread->vmlocals = NULL;
    memset(thread->locals, 0, sizeof(mpe_ThreadPrivateData));
    thread->entry = NULL;
    thread->entryData = NULL;
    thread->handle = NULL;
    thread->refcount = 0;
    thread->prev = thread->next = thread;
    mpeos_mutexNew(&thread->mutex);
    mpeos_condNew(FALSE, FALSE, &thread->start_cond);
#ifdef MPE_FEATURE_THREAD_SUSPEND
    thread->is_suspended_while_blocked = FALSE;
    thread->is_suspended_while_waiting = FALSE;
    thread->is_suspended = FALSE;
    thread->is_blocked = FALSE;
    thread->is_waiting = FALSE;
    mpeos_condNew(TRUE,FALSE,&thread->suspend_cond);
#endif

    *td = thread;

    return MPE_SUCCESS;
}

/**
 * <i>threadTermDesc()<i/>
 *
 * Remove the target descriptor from the thread descriptor database
 * (e.g. thread list, hashtable, etc) and return the thread descriptor
 * memory to the system.
 *
 * @param td Is the thread descriptor pointer.
 */
static void threadTermDesc(threadDesc *td)
{
    /* Thread has terminated, close it's handle and free it's thread local structure. */
    if (td->handle != NULL)
    {
        (void) CloseHandle(td->handle);
    }
    mpeos_mutexDelete(td->mutex);
    mpeos_condDelete(td->start_cond);
    mpeos_memFreeP(MPE_MEM_THREAD, td->locals);

#ifdef MPE_FEATURE_THREAD_SUSPEND
    mpeos_condDelete(td->suspend_cond);
#endif

    mpeos_memFreeP(MPE_MEM_THREAD, td);
}

/**
 * <i>threadStart()<i/>
 *
 * Setup for termination of the current thread prior to actually calling the
 * thread's entry point.  This setup allows the thread to "long jump" back
 * to this routine.  This ability to long jump will help with implementing
 * "thread death" scenarios for assisting with robust application isolation.
 *
 * @param td Is a pointer for returning the new thread descriptor pointer.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *         is returned.
 */
static DWORD WINAPI threadStart(void *data)
{
    threadDesc *td = (threadDesc *)data;

    /* Set the thread's descriptor storage structure pointer. */
    TlsSetValue(TLS_INDEX,(void*)td);

    /* Let the creating thread continue now that we are started */
    mpeos_condSet(td->start_cond);

    /* Perform exit support setjmp operation. */
    if (mpeos_setJmp(td->exitJmp) == 0)
    {
        /* Invoke thread's execution entry point. */
        (td->entry)(td->entryData);
    }

    /*
     * The thread is exiting, check for need to release its descriptor.
     */
    if (TlsGetValue(TLS_INDEX) == td)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_THREAD, "%s (thread ending) - td = %p, mutex = %x\n",
                __FUNCTION__, td, td->mutex);

        TlsSetValue(TLS_INDEX,NULL);

        /* Terminate descriptor use. */
        threadTermDesc(td);
    }

    return 0;
}

/* Priority mapping table. */
static uint32_t winPriorities[] =
{ 0, (uint32_t) THREAD_PRIORITY_TIME_CRITICAL,
        (uint32_t) THREAD_PRIORITY_HIGHEST,
        (uint32_t) THREAD_PRIORITY_ABOVE_NORMAL,
        (uint32_t) THREAD_PRIORITY_NORMAL,
        (uint32_t) THREAD_PRIORITY_BELOW_NORMAL,
        (uint32_t) THREAD_PRIORITY_BELOW_NORMAL,
        (uint32_t) THREAD_PRIORITY_LOWEST, (uint32_t) THREAD_PRIORITY_LOWEST, };

/**
 * <i>threadMapPriority()
 *
 * Map the specified thread priority to a valid OS value.
 * If the value is coming from the VM, it will be a value from 1-10.  If
 * It's an implementation layer thread, it could be a value already
 * appropriate for the platform.
 *
 * Note, java priorities go from 1-10 and are incremental.
 *
 * Note: this function is coded without the use of macros for the priorities
 * for clarity.  The macros defined in os_thread.h are used primarily to
 * support thread priority specification independant of the platform's
 * thread prioritization implementation.
 *
 * @param p Is the priority to map.
 *
 * @return The mapped priority value.
 */
static uint32_t threadMapPriority(uint32_t p)
{
    if (p > 8)
    {
        p = 8;
    }
    if (p == 0)
    {
        p = 1;
    }
    return winPriorities[p];
}

/**
 * <i>mpeos_threadCreate()</i>
 *
 * Create a new operating system specific thread.
 *
 * @param entry is a function pointer to the thread's execution entry point.
 *          The function pointer definition specifies reception of a single parameter,
 *          which is a void pointer to any data the thread requires.
 * @param data is a void pointer to any thread specific data that is to be passed
 *          as the single parameter to the thread's execution entry point.
 * @param priority is the initial execution priority of the new thread.
 * @param threadId is a pointer for returning the new thread's identifier or
 *          handle.  The identifier is of the <i>mpe_ThreadId<i/> type.
 * @param name is the name of the new thread, which is only applicable to target
 *          operatings systems that allow thread naming.  The MPE layer does
 *          not support naming if the target operating system doesn't.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_threadCreate(void(*entry)(void *), void *data,
        uint32_t priority, uint32_t stackSize, mpe_ThreadId *threadId,
        const char *name)
{
    mpe_Error ec;
    threadDesc *td;
    threadDesc *self;

    if (entry == NULL)
        return MPE_EINVAL;

    /* Check for need to initialize thread local storage index */
    if (TLS_INDEX == TLS_OUT_OF_INDEXES)
    {
        if ((TLS_INDEX = TlsAlloc()) == TLS_OUT_OF_INDEXES)
            return MPE_EINVAL;
    }

    /* Allocate a new thread descriptor. */
    if ((ec = threadAllocDesc(&td)) != MPE_SUCCESS)
    {
        return ec;
    }
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_THREAD, "%s - td = %p, mutex = %x\n",
            __FUNCTION__, td, td->mutex);

    td->entryData = data; /* User data passed in */
    td->entry = entry; /* User's function */
    td->refcount = 1; /* Initialize reference to 1 */

    if (name != NULL)
        strncpy(td->name, name, 255);

    /* Copy inheritable private thread local data. */
    if ((self = (threadDesc*) TlsGetValue(TLS_INDEX)) != NULL)
    {
        mpe_ThreadPrivateData* tpdata = (mpe_ThreadPrivateData*) td->locals;
        mpe_ThreadPrivateData* self_data =
                (mpe_ThreadPrivateData*) self->locals;
        tpdata->memCallbackId = self_data->memCallbackId;
    }

    /* Create the thread in a running state */
    td->handle = CreateThread(NULL, stackSize, threadStart, (void*) td, 0,
            &td->id);
    if (td->handle == NULL)
    {
        threadTermDesc(td);
        return MPE_ENOMEM;
    }

    /* Wait for the thread to actually start to ensure that the thread
     local storage has been set */
    mpeos_condGet(td->start_cond);

    /* Make sure priority is suitable for target. */
    (void) SetThreadPriority(td->handle, threadMapPriority(priority));

    /* Return thread identifier. */
    if (threadId != NULL)
        *threadId = td;

    return MPE_SUCCESS;
}

/**
 * <i>mpeos_threadAttach()</i>
 *
 * Create or release thread tracking information for a thread not created
 * with the <i>mpeos_threadCreate<i/> API.  Most likely this is only utilized
 * for the initial primordial thread of the VM.  If the thread identifier
 * pointer is NULL, the call is considered a "detach" call and all resources
 * established during the "attach" phase will be released.
 *
 * @param threadId is a pointer for returning the thread's identifier or
 *          handle.  The identifier is of the <i>mpe_ThreadId<i/> type.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_threadAttach(mpe_ThreadId *threadId)
{
    threadDesc *td;
    mpe_Error ec;

    /* Check for need to initialize thread local storage index */
    if (TLS_INDEX == TLS_OUT_OF_INDEXES)
    {
        if ((TLS_INDEX = TlsAlloc()) == TLS_OUT_OF_INDEXES)
            return MPE_EINVAL;
    }

    /* If thread Id pointer is not NULL, it's an attach call. */
    if (threadId != NULL)
    {
        HANDLE procHnd = GetCurrentProcess();

        /* Do we already have a descriptor? */
        if ((td = (threadDesc*) TlsGetValue(TLS_INDEX)) != NULL)
        {
            td->refcount++; /* Bump the attach reference on this thread */
            *threadId = td;
            return MPE_SUCCESS;
        }

        /* Allocate a new thread descriptor. */
        if ((ec = threadAllocDesc(&td)) != MPE_SUCCESS)
            return ec;

        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_THREAD,
                "%s (attach) - td = %p, mutex = %x\n", __FUNCTION__, td,
                td->mutex);

        /* Retrieve this thread's handle */
        if (!DuplicateHandle(procHnd, GetCurrentThread(), procHnd, &td->handle,
                0, FALSE, DUPLICATE_SAME_ACCESS))
            return MPE_EINVAL;

        /* Set the thread's descriptor storage structure pointer. */
        TlsSetValue(TLS_INDEX, (void*) td);

        *threadId = td; /* Return identifier. */
    }
    else
    {
        td = (threadDesc*) TlsGetValue(TLS_INDEX);

        if (td != NULL)
        {
            td->refcount--;

            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_THREAD,
                    "%s (detach) - refcount = %d\n", __FUNCTION__, td->refcount);

            /* if this is the final detach, Terminate descriptor use. */
            if (td->refcount == 0)
            {
                MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_THREAD,
                        "%s (detach) - td = %p, mutex = %x\n", __FUNCTION__,
                        td, td->mutex);

                TlsSetValue(TLS_INDEX, NULL);
                threadTermDesc(td);
            }
        }
    }
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_threadDestroy()</i>
 *
 * Terminates the current thread or a specified target operating system thread.
 * If the target thread identifier is zero the current (calling) thread is
 * terminated.
 *
 * @param threadId This is the identifier/handle of the target thread to terminate.
 *          It is the original handle returned from the thread creation API.  If the
 *          thread identifier is zero or mathes the identifier of the current
 *          thread, then the calling thread will be terminated.
 *
 * @return An MPE error code if the destroy fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_threadDestroy(mpe_ThreadId threadId)
{
    threadDesc *td = threadId == 0 ? (threadDesc*) TlsGetValue(TLS_INDEX)
            : (threadDesc*) threadId;

    /* Check for termination of the current thread. */
    if (threadId == 0)
    {
        /* Jump to the termination point. */
        mpeos_longJmp(td->exitJmp, 0);
    }

    /* Mark the thread for death. */
    if (td != NULL)
        td->status |= MPE_THREAD_STAT_DEATH;

    return MPE_SUCCESS;
}

/**
 * <i>mpeos_threadSetPriority()</i>
 *
 * Set the priority of the specified thread.
 * If the target OS does not support modification
 *          of a thread's priority after creation, this function shall do nothing.
 *
 * @param threadId is the identifier of the target thread.
 * @param priority is the new priority level.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_threadSetPriority(mpe_ThreadId threadId, uint32_t priority)
{
    threadDesc *td = threadId == 0 ? (threadDesc*) TlsGetValue(TLS_INDEX)
            : (threadDesc*) threadId;

    /* Make sure priority is suitable for target. */
    if (td != NULL)
        (void) SetThreadPriority(td->handle, threadMapPriority(priority));

    return MPE_SUCCESS;
}

/**
 * The <i>mpeos_threadGetStatus()</i> function gets the target thread's status
 * variable.  If the thread identifier parameter is zero or matches the identifier
 * of the current thread, then the calling thread's status is returned.
 *
 * @param threadId is the identifier of the target thread.
 * @param threadStatus is a pointer to an unsigned integer for returning the thread's
 *          current status.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_threadGetStatus(mpe_ThreadId threadId, mpe_ThreadStat *status)
{
    threadDesc *td = threadId == 0 ? (threadDesc*) TlsGetValue(TLS_INDEX)
            : (threadDesc*) threadId;

    if (status == NULL || td == NULL)
        return MPE_EINVAL;

    /* Return the currect thread status. */
    *status = td->status;

    return MPE_SUCCESS;
}

/**
 * The <i>mpeos_threadSetStatus()</i> function sets the target thread's status
 * variable. If the thread identifier parameter is zero or matches the identifier
 * of the current thread, then the calling thread's status is set.
 *
 * @param threadId is the identifier of the target thread.
 * @param threadStatus is the threads new status.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_threadSetStatus(mpe_ThreadId threadId, mpe_ThreadStat status)
{
    threadDesc *td = threadId == 0 ? (threadDesc*) TlsGetValue(TLS_INDEX)
            : (threadDesc*) threadId;

    if (td == NULL)
        return MPE_EINVAL;

    /* Set the thread's status variable. */
    mpeos_mutexAcquire(td->mutex);
    td->status = status;
    mpeos_mutexRelease(td->mutex);

    return MPE_SUCCESS;
}

/**
 * <i>mpeos_threadGetData()</i>
 *
 * Get the target thread's "thread local storage".  If the thread identifier
 * parameter is zero or matches the identifier of the current thread, then
 * the calling thread's "thread local storage" is returned.
 *
 * @param threadId is the identifier of the target thread.
 * @param threadLocals is a void pointer for returning the thread specific data
 *          pointer.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_threadGetData(mpe_ThreadId threadId, void **threadLocals)
{
    threadDesc *td = threadId == 0 ? (threadDesc*) TlsGetValue(TLS_INDEX)
            : (threadDesc*) threadId;
    mpe_ThreadPrivateData* data;

    if (threadLocals == NULL || td == NULL)
        return MPE_EINVAL;

    /* Return the currect thread local storage pointer. */
    mpeos_mutexAcquire(td->mutex);
    data = (mpe_ThreadPrivateData*) td->locals;
    *threadLocals = data->threadData;
    mpeos_mutexRelease(td->mutex);

    return MPE_SUCCESS;
}

/**
 * <i>mpeos_threadSetData()</i>
 *
 * Set the target thread's local storage. If the thread identifier parameter
 * is zero or matches the identifier of the current thread, then the calling
 * thread's "thread local storage" is set.
 *
 * @param threadId is the identifier of the target thread.
 * @param threadLocals is a void pointer to the thread specific data.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_threadSetData(mpe_ThreadId threadId, void *threadLocals)
{
    threadDesc *td = threadId == 0 ? (threadDesc*) TlsGetValue(TLS_INDEX)
            : (threadDesc*) threadId;
    mpe_ThreadPrivateData* data;

    if (threadLocals == NULL || td == NULL)
        return MPE_EINVAL;

    /* Set the thread's local data storage pointer. */
    mpeos_mutexAcquire(td->mutex);
    data = (mpe_ThreadPrivateData*) td->locals;
    data->threadData = threadLocals;
    mpeos_mutexRelease(td->mutex);

    return MPE_SUCCESS;
}

/**
 * The <i>mpeos_threadGetPrivateData()</i> function retrieves the MPE private data
 * for the given target thread.  If the thread identifier is zero, that the private
 * data for the current thread is returned.
 *
 * @param threadId is the identifier of the target thread.
 * @param data is a pointer to the location where the pointer the the target thread's
 *         private data should be written
 * @return the MPE error code if the operation fails, otherwise <i>MPE_SUCCESS</i>
 *          is returned
 */
mpe_Error mpeos_threadGetPrivateData(mpe_ThreadId threadId,
        mpe_ThreadPrivateData **data)
{
    threadDesc *td = threadId == 0 ? (threadDesc*) TlsGetValue(TLS_INDEX)
            : (threadDesc*) threadId;

    if (data == NULL || td == NULL)
        return MPE_EINVAL;

    *data = (mpe_ThreadPrivateData*) td->locals;

    return MPE_SUCCESS;
}

#if defined(MPE_FEATURE_VMDATA)
/**
 * <i>mpeos_threadGetVMData()</i>
 *
 * Get the target thread's "thread local storage".  If the thread identifier
 * parameter is zero or matches the identifier of the current thread, then
 * the calling thread's "thread local storage" is returned.
 *
 * @param threadId is the identifier of the target thread.
 * @param threadLocals is a void pointer for returning the VM thread specific data
 *          pointer.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_threadGetVMData(mpe_ThreadId threadId, void **threadLocals)
{
    threadDesc *td = threadId == 0 ?
    (threadDesc*)TlsGetValue(TLS_INDEX) :
    (threadDesc*)threadId;

    if (threadLocals == NULL || td == NULL)
    return MPE_EINVAL;

    /* Return the currect thread's VM local storage pointer. */
    *threadLocals = td->vmlocals;

    return MPE_SUCCESS;
}

/**
 * <i>mpeos_threadSetVMData()</i>
 *
 * Set the target thread's VM local storage. If the thread identifier parameter
 * is zero or matches the identifier of the current thread, then the calling
 * thread's VM "thread local storage" is set.
 *
 * @param threadId is the identifier of the target thread.
 * @param threadLocals is a void pointer to the thread specific data.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_threadSetVMData(mpe_ThreadId threadId, void *threadLocals)
{
    threadDesc *td = threadId == 0 ?
    (threadDesc*)TlsGetValue(TLS_INDEX) :
    (threadDesc*)threadId;

    if (td == NULL)
    return MPE_EINVAL;

    /* Set the thread's local data storage pointer. */
    mpeos_mutexAcquire(td->mutex);
    td->vmlocals = threadLocals;
    mpeos_mutexRelease(td->mutex);

    return MPE_SUCCESS;
}
#endif /* MPE_FEATURE_VMDATA */

/**
 * <i>mpeos_threadGetCurrent()</i>
 *
 * Retrieves the thread identifier of the current (calling) thread.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_threadGetCurrent(mpe_ThreadId *threadId)
{
    threadDesc* td;

    if (threadId == NULL)
        return MPE_EINVAL;

    /* Get platform ID. */
    if ((td = (threadDesc*) TlsGetValue(TLS_INDEX)) == NULL)
        return MPE_EINVAL;

    *threadId = td;

    return MPE_SUCCESS;
}

/**
 * <i>mpeos_threadSleep()</i>
 *
 * Put the current thread to sleep (i.e. suspend execution) for specified
 * time.  The amount of time that the thread will be suspened is the sum
 * of the number of milliseconds and nanoseconds specified.  An error will
 * be returned if the thread is woken (activated) prematurely.
 *
 * @param milliseconds is a the number of milliseconds to sleep.
 * @param nanoseconds is a the number of microseconds to sleep.

 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_threadSleep(uint32_t milliseconds, uint32_t microseconds)
{
    /* Windows runs on a millisecond granularity, so ignore microseconds */
    MPE_UNUSED_PARAM(microseconds);
    Sleep(milliseconds);

    return MPE_SUCCESS;
}

/**
 * <i>mpeos_threadYield()<i/>
 *
 * Give up the remainder of the current thread's timeslice.
 */
void mpeos_threadYield(void)
{
    /* Yield to any other threads... */
    (void) SwitchToThread();
}

/**
 * The <i>mpeos_threadSuspend()</i> function suspends execution of the current thread
 * or a specified target operating system thread.  If the target thread identifier
 * is zero the current (calling) thread is suspended.  If the OS does not
 * support the ability to suspend an arbitrary thread, then this function will
 * return an error.
 *
 * @param threadId This is the identifier/handle of the target thread to suspend.
 *          It is the original handle returned from the thread creation API.  If the
 *          thread identifier is zero or matches the identifier of the current
 *          thread, then the calling thread will be suspended.
 * @return An MPE error code if the destroy fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_threadSuspend(mpe_ThreadId threadId)
{
#ifdef MPE_FEATURE_THREAD_SUSPEND
    threadDesc *td = threadId == 0 ?
    (threadDesc*)TlsGetValue(TLS_INDEX) :
    (threadDesc*)threadId;

    if (td != NULL)
    {
        if (td->id == GetCurrentThreadId())
        {
            // Suspending our self
            td->is_suspended = TRUE;
            (void)SuspendThread(td->handle);
        }
        else
        {
            mpeos_mutexAcquire(td->mutex);
            if (!td->is_suspended)
            {
                td->is_suspended = TRUE;

                // If we are waiting on a cond var, just set the variable.  When the
                // thread wakes up from the cond var, it will suspend
                if (td->is_waiting)
                td->is_suspended_while_waiting = TRUE;
                else
                {
                    // Suspend the thread, but if we are blocked on a mutex,
                    // resume and set the flag and
                    (void)SuspendThread(td->handle);
                    if (td->is_blocked)
                    {
                        td->is_suspended_while_blocked = TRUE;
                        (void)ResumeThread(td->handle);
                    }
                }
            }
            mpeos_mutexRelease(td->mutex);
        }
    }
#endif
    return MPE_SUCCESS;
}

/**
 * The <i>mpeos_threadResume()</i> function resumes execution of the specified
 * target operating system thread.  If the OS does not support the ability to
 * resume an arbitrary thread, then this function will return an error.
 *
 * @param threadId This is the identifier/handle of the target thread to resume.
 *          It is the original handle returned from the thread creation API.
 * @return An MPE error code if the destroy fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_threadResume(mpe_ThreadId threadId)
{
#ifdef MPE_FEATURE_THREAD_SUSPEND
    threadDesc *td = threadId == 0 ?
    (threadDesc*)TlsGetValue(TLS_INDEX) :
    (threadDesc*)threadId;

    if (td != NULL)
    {
        mpeos_mutexAcquire(td->mutex);

        /* If we are suspended, then make sure we resume out of all cases,
         including suspended while waiting on a cond var or blocked on a mutex */
        if (td->is_suspended)
        {
            td->is_suspended = FALSE;

            // Cond var
            if (td->is_suspended_while_waiting)
            {
                td->is_suspended_while_waiting = FALSE;
                SetEvent((HANDLE)td->suspend_cond->hnd);
            }
            // Mutex

            else if (td->is_suspended_while_blocked)
            {
                td->is_suspended_while_blocked = FALSE;
                SetEvent((HANDLE)td->suspend_cond->hnd);
            }
            // Neither, just resume

            else
            (void)ResumeThread(td->handle);
        }

        mpeos_mutexRelease(td->mutex);
    }

#endif
    return MPE_SUCCESS;
}

/**
 * The <i>mpeos_threadSetName()</i> function sets the target thread's name
 * If the thread identifier parameter is zero or matches the identifier
 * of the current thread, then the calling thread's name is set.
 *
 * @param threadId is the identifier of the target thread.
 * @param name is string representing the thread's name.
 * @return The MPE error code if the rename fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_threadSetName(mpe_ThreadId threadId, const char* name)
{
    MPE_UNUSED_PARAM(threadId);
    MPE_UNUSED_PARAM(name);
    MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_THREAD,
              "%s: Setting thread name not supported on Windows\n",
              __FUNCTION__);
    return MPE_EINVAL;
}


