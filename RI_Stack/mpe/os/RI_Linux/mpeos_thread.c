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
#include <unistd.h>         /* usleep(3) */
#include <string.h>
#include <stdlib.h>
#include <pthread.h>
#include <signal.h>

#ifdef _POSIX_PRIORITY_SCHEDULING
#include <sched.h>
#endif

#include <errno.h>

#include <signal.h>

#include "mpe_types.h"
#include "mpe_error.h"
#include "mpeos_thread.h"
#include "mpeos_mem.h"
#include "mpeos_sync.h"
#include "mpeos_dbg.h"

pthread_key_t gThreadDataKey;

mpe_Bool gInitialized = FALSE;

static void signalHandler(int sig)
{
    ThreadDesc* td;

    if (mpeos_threadGetCurrent(&td) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_THREAD,
                "thread suspension signal handler - could not get current thread\n");
        return;
    }

    if (td->td_inHandler || td->td_blocked || td->td_waiting)
        return;

    td->td_inHandler = TRUE;

    if (td->td_suspended)
    {
        sigset_t signals;
        pthread_sigmask(SIG_SETMASK, NULL, &signals);
        sigdelset(&signals, SIGUSR1);

        do
        {
            sigsuspend(&signals);
        } while (td->td_suspended);
    }
    td->td_inHandler = FALSE;
}

void suspendMe()
{
    sigset_t signals;

    sigemptyset(&signals);
    sigaddset(&signals, SIGUSR1);

    pthread_sigmask(SIG_BLOCK, &signals, NULL);
    signalHandler( SIGUSR1);
    pthread_sigmask(SIG_UNBLOCK, &signals, NULL);
}

/**
 * <i>threadInit()<i/>
 *
 * Initialize MPE OS thread specific implementation variables.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *         is returned.
 */
mpe_Error threadInit(void)
{
    // Register our SIGUSR1 signal handler for thread suspend/resume
    struct sigaction action;

    action.sa_handler = signalHandler;
    action.sa_flags = SA_RESTART;
    sigemptyset(&action.sa_mask);

    if (sigaction(SIGUSR1, &action, NULL) == -1)
    {
        MPEOS_LOG(MPE_LOG_FATAL, MPE_MOD_THREAD,
                "Could not register signal handler for thread suspend/resume!\n");
        return MPE_EINVAL;
    }

    pthread_key_create(&gThreadDataKey, NULL);

    return MPE_SUCCESS;
}

/**
 * <i>threadMapPriority()
 *
 * Linux pthread priorities (using the RoundRobin scheduler policy) on most
 * are 1-99 with 1 being the lowest priority.  Java thread priorities are
 * always between 1 and 10 with 1 being the lowest priority.  Therefore,
 * we actually define our minimum pthread priority to be 11 so that we can
 * distinguish betweek the two ranges
 *
 * @param p is the priority to map
 *
 * @return the mapped priority value
 */
static uint32_t threadMapPriority(uint32_t p)
{
    /* Java priority mapping */
    uint32_t java_mapping[] = { 0, 11, 20, 30, 40, 50, 60, 70, 80, 90, 99 };

    /* Check extremes */
    if (p == 0)
        return MPE_THREAD_PRIOR_MIN;
    if (p > MPE_THREAD_PRIOR_MAX)
        return MPE_THREAD_PRIOR_MAX;
    
    /* Java thread priorities */
    if (p <= 10)
    {
        /* Distribute the thread priority throughout the acceptable range */
        return java_mapping[p];
    }
    
    return p;
}

// Allocate a new thread description structure with the given information and
// adds it to our global list of threads
static ThreadDesc* allocateThread(void(*entry)(void*), void* data,
        const char* name)
{
    ThreadDesc* td;
    char* threadName;

    /* If the thread isn't named, generate one. */
    if (NULL == name)
    {
        char threadNameBuf[16];
        static uint32_t unnamed = 0;

        (void) sprintf(threadNameBuf, "mpe_%u", ++unnamed);
        threadName = threadNameBuf;
    }
    else
    {
        threadName = (char*) name;
    }

    /* Allocate a new thread structure. */
    if (mpeos_memAllocP(MPE_MEM_THREAD, sizeof(ThreadDesc), (void**) &td)
            != MPE_SUCCESS)
        return NULL;

    /* Allocate thread's private data structure. */
    if (mpeos_memAllocP(MPE_MEM_THREAD, sizeof(mpe_ThreadPrivateData),
            (void**) &td->td_locals) != MPE_SUCCESS)
    {
        mpeos_memFreeP(MPE_MEM_THREAD, td);
        return NULL;
    }

    /* Set thread data. */
    td->td_name = strdup(threadName); /* Thread name */
    td->td_entry = entry; /* Thread entry function. */
    td->td_entryData = data; /* Thread user data passed in. */

    td->td_blocked = FALSE;
    td->td_waiting = FALSE;
    td->td_inHandler = FALSE;
    td->td_suspended = FALSE;

    td->td_blocked_fd = -1;

    td->td_id = 0; /* Clear thread Id. */

    td->td_next = NULL;
    td->td_prev = NULL;

    return td;
}

// Remove this thread description structure from our list and free
// its memory
static void freeThread(ThreadDesc* thread)
{
    // Free structure resources
    if (thread != NULL)
    {
        free(thread->td_name);
        mpeos_memFreeP(MPE_MEM_THREAD, thread->td_locals);
        mpeos_memFreeP(MPE_MEM_THREAD, thread);
    }
}

/**
 * <i>threadStart()<i/>
 *
 * Setup for termination of the current thread prior to actually calling the
 * thread's entry point.  This setup allows the thread to "long jump" back
 * to this routine.  This ability to long jump will help with implementing
 * "thread death" scenarios for assisting with robust application isolation.
 *
 * @param tls is the thread descriptor pointer.
 *
 * @return nothing.
 */
static void threadStart(void *tls)
{
    ThreadDesc *td = (ThreadDesc *) tls; /* Thread descriptor pointer. */

    /* Sanity check... */
    if (tls == NULL)
    {
        return;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_THREAD,
            "Thread Starting! desc=0x%p, id=%lu name=%s\n", td, td->td_id,
            td->td_name);

    /* If parent didn't get a chance to set Id in mpeos_threadCreate, set it now. */
    if (td->td_id == 0)
        td->td_id = pthread_self();

    pthread_setspecific(gThreadDataKey, (void*) td);

    /* Perform exit support setjmp operation. */
    if (mpeos_setJmp(td->td_exitJmp) == 0)
    {
        /* Invoke thread's execution entry point. */
        (td->td_entry)(td->td_entryData);
    }

    /*
     * The thread is done executing, release its structure.
     */
    if (pthread_getspecific(gThreadDataKey) != NULL)
        freeThread(td);
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
 * @param name is the name of the new thread.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_threadCreate(void(*entry)(void *), void *data,
        uint32_t priority, uint32_t stackSize, mpe_ThreadId *threadId,
        const char *name)
{ // the create a unique thread each time

    volatile uint32_t newPriority; /* mapped priority. */
    ThreadDesc *td = NULL; /* New thread descriptor pointer. */
    char *threadName = NULL; /* Thread name pointer. */
    struct sched_param param;

    MPE_UNUSED_PARAM(stackSize);

    if (!gInitialized)
    {
        threadInit();
        gInitialized = TRUE;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_THREAD, "mpeos_threadCreate() %s(%p)\n",
            name, name);

    if (NULL == entry || NULL == threadId)
        return MPE_EINVAL;

    if ((td = allocateThread(entry, data, name)) == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_THREAD,
                "mpeos_threadCreate() failed: Could not allocate thread structure\n");
        return MPE_ENOMEM;
    }

    /* Make sure priority is suitable for target. */
    newPriority = threadMapPriority(priority);

    if (pthread_attr_init(&td->td_attr) != 0)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_THREAD,
                "mpeos_threadCreate() failed: pthread_attr_init failed\n");
        return MPE_ENOMEM;
    }
    
    /* Ensure that thread priorities are not linked to the creating thread */
    if (pthread_attr_setdetachstate(&td->td_attr, PTHREAD_CREATE_DETACHED) != 0)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_THREAD,
                "mpeos_threadCreate() failed: pthread_attr_setdetachstate failed\n");
        return MPE_EINVAL;
    }

    /* Set Round Robin scheduling policy */
    if (pthread_attr_setschedpolicy(&td->td_attr, SCHED_RR) != 0)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_THREAD,
                "mpeos_threadCreate() failed: pthread_attr_setschedpolicy failed\n");
        return MPE_EINVAL;
    }

    /* Set thread priority */
    param.sched_priority = newPriority;
    if (pthread_attr_setschedparam(&td->td_attr, &param) != 0)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_THREAD,
                  "mpeos_threadCreate() failed: pthread_attr_setschedparam (thread priority = %d) failed\n", newPriority);
        return MPE_EINVAL;
    }

    if (pthread_create(&td->td_id, &td->td_attr,
            (void *(*)(void *)) threadStart, td) != 0)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_THREAD,
                "mpeos_threadCreate() failed: pthread create failed!\n");
        freeThread(td);
        return MPE_ENOMEM;
    }

    *threadId = td;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_THREAD, "new thread id:%lu name=%s\n",
            td->td_id, threadName);

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
    ThreadDesc *td = threadId;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_THREAD, "mpeos_threadDestroy(%p)\n",
            threadId);

    if (threadId == 0)
    {
        if (mpeos_threadGetCurrent(&td) != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_THREAD, "%s: thread not found!\n",
                    __FUNCTION__);
            return MPE_EINVAL;
        }

        pthread_setspecific(gThreadDataKey, (void*) NULL);
        freeThread(threadId);
        pthread_exit( NULL);
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_THREAD,
                "mpeos_threadDestroy() Killing other threads is not supported\n");
    }

    /* Mark the target thread for death. */
    td->td_status |= MPE_THREAD_STAT_DEATH;

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
    ThreadDesc *td = threadId;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_THREAD, "mpeos_threadGetStatus:%p\n",
            threadId);

    /* Sanity check... */
    if (status == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_THREAD,
                "mpeos_threadGetStatus status is NULL");
        return MPE_EINVAL;
    }

    if (threadId == 0 && mpeos_threadGetCurrent(&td) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_THREAD, "%s: thread not found!\n",
                __FUNCTION__);
        return MPE_EINVAL;
    }

    /* Return the currect thread status. */
    *status = td->td_status;

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
    ThreadDesc *td = threadId;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_THREAD, "mpeos_threadSetStatus:%p\n",
            threadId);

    if (threadId == 0 && mpeos_threadGetCurrent(&td) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_THREAD, "%s: thread not found!\n",
                __FUNCTION__);
        return MPE_EINVAL;
    }

    /* Set the thread's status variable. */
    td->td_status = status;

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
    ThreadDesc *td = threadId;

    /* Sanity check. */
    if (threadLocals == NULL)
        return MPE_EINVAL;

    if (threadId == 0 && mpeos_threadGetCurrent(&td) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_THREAD, "%s: thread not found!\n",
                __FUNCTION__);
        return MPE_EINVAL;
    }

    /* Return the correct thread local storage pointer. */
    *threadLocals = td->td_locals->threadData;

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
    ThreadDesc *td = threadId;

    /* Sanity check. */
    if (data == NULL)
        return MPE_EINVAL;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_THREAD, "mpeos_threadGetPrivateData:%p\n",
            threadId);

    if (threadId == 0 && mpeos_threadGetCurrent(&td) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_THREAD, "%s: thread not found!\n",
                __FUNCTION__);
        return MPE_EINVAL;
    }

    *data = td->td_locals;

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
    ThreadDesc *td = threadId;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_THREAD,
            "mpeos_threadSetData:%p data=0x%p\n", threadId, threadLocals);

    if (threadId == 0 && mpeos_threadGetCurrent(&td) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_THREAD, "%s: thread not found!\n",
                __FUNCTION__);
        return MPE_EINVAL;
    }

    /* Set the thread's local data storage pointer. */
    td->td_locals->threadData = threadLocals;

    return MPE_SUCCESS;
}

//
// VMDATA is not currently used - leave this code here in case we ever need local storage for the VM
//
//
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
    ThreadDesc *td; /* Thread descriptor pointer. */

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_THREAD, "mpeos_threadGetVMData:%d\n",threadId);
    /* Sanity check... */
    if ( threadLocals == NULL ))
    {
        return MPE_EINVAL;
    }

    /* Get the thread's locals. */
    if ( (td = threadGetDesc(threadId)) == NULL )
    {
        return MPE_EINVAL;
    }

    /* Return the currect thread's VM local storage pointer. */
    *threadLocals = td->td_vmlocals;

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
    ThreadDesc *td; /* Thread descriptor pointer. */

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_THREAD, "mpeos_threadSetVMData:%d\n",threadId);
    /* Get the thread's descriptor. */
    if ( (td = threadGetDesc(threadId)) == NULL )
    {
        return MPE_EINVAL;
    }

    /* Set the thread's local data storage pointer. */
    td->td_vmlocals = threadLocals;

    return MPE_SUCCESS;
}
#endif /* MPE_FEATURE_VMDATA */

/**
 * <i>mpeos_threadGetCurrent()</i>
 *
 * Retrieves the thread identifier of the current (calling) thread.
 *
 * @param threadId is a pointer for returning the current thread's identifier.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_threadGetCurrent(mpe_ThreadId *threadId)
{
    ThreadDesc* data = (ThreadDesc*) pthread_getspecific(gThreadDataKey);
    if (data == NULL)
    {
        MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_THREAD,
                "mpeos_threadGetCurrent() Thread not found! ...\n");
        return MPE_EINVAL;
    }

    *threadId = data;

    return MPE_SUCCESS;
}

/**
 * <i>mpeos_threadSleep()</i>
 *
 * Put the current thread to sleep (i.e. suspend execution) for specified
 * time.  The amount of time that the thread will be suspened is the sum
 * of the number of milliseconds and microoseconds specified.  An error will
 * be returned if the thread is woken (activated) prematurely.
 *
 * @param milliseconds is the number of milliseconds to sleep.
 * @param nanoseconds is the number of microseconds to sleep.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_threadSleep(uint32_t milliseconds, uint32_t microseconds)
{

    unsigned long usec = milliseconds * 1000 + microseconds;
    usleep(usec);
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_threadYield()<i/>
 *
 * Give up the remainder of the current thread's timeslice.
 */
void mpeos_threadYield(void)
{
#ifdef _POSIX_PRIORITY_SCHEDULING
    int ret = sched_yield();
    if ( 0 != ret )
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_THREAD,
                "sched_yield() failed: %d (%d)\n", ret, errno);
    }
#else
#error "No mpeos_threadYield implementation!"
#endif
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
    ThreadDesc* td = threadId;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_THREAD,
            "mpeos_threadSuspend: suspending thread ID %p\n",threadId);

    td->td_suspended = TRUE;

    pthread_kill(td->td_id, SIGUSR1);
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
    ThreadDesc* td = threadId;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_THREAD,
            "mpeos_threadResume: resuming thread ID %p\n",threadId);

    td->td_suspended = FALSE;

    pthread_kill(td->td_id, SIGUSR1);
#endif

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
    ThreadDesc *td; /* Thread descriptor pointer. */

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_THREAD, "mpeos_threadAttach\n");

    if (!gInitialized)
    {
        threadInit();
        gInitialized = TRUE;
    }

    /* If thread Id pointer is not NULL, it's an attach call. */
    if (threadId != NULL)
    {
        char threadName[255];
        static int attachedThreadCount = 1;

        sprintf(threadName, "mpe_attached_%d", attachedThreadCount++);

        /*
         * Thread was create externally (i.e. not with mpeos_threadCreate),
         * need to setup resource to make it MPE compatible.
         */
        td = allocateThread(NULL, NULL, threadName);
        td->td_id = pthread_self();
        pthread_setspecific(gThreadDataKey, (void*) td);
        *threadId = td;
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_THREAD,
                "mpeos_threadAttached! desc=0x%p, id=%lu\n", td, td->td_id);
    }
    else if (mpeos_threadGetCurrent(&td) == MPE_SUCCESS)
    {
        pthread_setspecific(gThreadDataKey, (void*) NULL);
        freeThread(td);
    }

    return MPE_SUCCESS;
}

/**
 * <i>mpeos_threadSetPriority()</i>
 *
 * Set the priority of the specified thread. If the target OS does not support
 * modification of a thread's priority after creation, this function shall do
 * nothing.
 *
 * @param threadId is the identifier of the target thread.
 * @param priority is the new priority level.
 *
 * @return The MPE error code if set priority fails, otherwise <i>MPE_SUCCESS</i>
 *         is returned.
 */
mpe_Error mpeos_threadSetPriority(mpe_ThreadId threadId, uint32_t priority)
{
    // Can't set the priority of a pthread after it is created.
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
              "%s: Setting thread name not supported on x86 Linux\n",
              __FUNCTION__);
    return MPE_EINVAL;
}

