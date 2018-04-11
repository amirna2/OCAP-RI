#if !defined(_MPEOS_THREAD_H)
#define _MPEOS_THREAD_H
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
#include <mpe_types.h>		/* Resolve basic type references. */
#include <os_thread.h>		/* Resolve target specific timer definitions. */

#ifdef __cplusplus
extern "C"
{
#endif

/***
 * Thread macro and type definitions:
 */
#define MPE_THREAD_PRIOR_MAX   OS_THREAD_PRIOR_MAX	/* Maximum thread priority. */
#define MPE_THREAD_PRIOR_MIN   OS_THREAD_PRIOR_MIN  /* Minimum thread priority. */
#define MPE_THREAD_PRIOR_DFLT  OS_THREAD_PRIOR_DFLT /* Default thread priority. */
#define MPE_THREAD_PRIOR_INC   OS_THREAD_PRIOR_INC  /* Priority setting increment. */

/* Thread priorities used to define priorities of system implementatin threads. */
#define MPE_THREAD_PRIOR_SYSTEM_HI  OS_THREAD_PRIOR_SYSTEM_HI
#define MPE_THREAD_PRIOR_SYSTEM_MED OS_THREAD_PRIOR_SYSTEM_MED
#define MPE_THREAD_PRIOR_SYSTEM 	OS_THREAD_PRIOR_SYSTEM

/***
 * Thread status bit definitions: 
 */
typedef os_ThreadStat mpe_ThreadStat; /* Thread status type. */

#define MPE_THREAD_STAT_MUTEX  (0x00000001)     /* Acquire(d) mutext mode. */
#define MPE_THREAD_STAT_COND   (0x00000002)     /* Acquire(d) cond mode. */
#define MPE_THREAD_STAT_EVENT  (0x00000004)     /* Get event mode. */
#define MPE_THREAD_STAT_CALLB  (0x00000008)		/* Event callback context mode. */
#define MPE_THREAD_STAT_DEATH  (0x00008000)		/* Thread death mode. */

#define MPE_THREAD_STACK_SIZE  OS_THREAD_STACK_SIZE /* Default stack size. */

typedef os_ThreadId mpe_ThreadId; /* Thread identifier type binding. */

/**
 * MPE private thread data.
 */
typedef struct mpe_ThreadPrivateData
{
    void* threadData; /**< Thread data as returned by mpe_threadGetData() */
    int64_t memCallbackId; /**< Resource Reclamation contextual information.  Inherited from parent. */
} mpe_ThreadPrivateData;

/***
 * Thread API prototypes:
 */

/**
 * The <i>mpeos_threadCreate()</i> function shall create a new operating system
 * specific thread.  If allowed by the target OS the new thread's priority will
 * be set to the specified priority level.  
 *
 * @param entry is a function pointer to the thread's execution entry point.  
 *          The function pointer definition specifies reception of a single parameter, 
 *          which is a void pointer to any data the thread requires.
 * @param data is a void pointer to any thread specific data that is to be passed
 *          as the single parameter to the thread's execution entry point.
 * @param priority is the initial execution priority of the new thread.
 * @param stackSize is the size of the thread's stack, if zero is specified, a
 *          default size appropriate for the system will be used.
 * @param threadId is a pointer for returning the new thread's identifier or
 *          handle.  The identifier is of the <i>mpe_ThreadId<i/> type.
 * @param name is the name of the new thread, which is only applicable to target
 *        operatings systems that allow thread naming.  The MPE layer does
 *        not support naming if the target operating system doesn't.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_threadCreate(void(*entry)(void *), void *data,
        uint32_t priority, uint32_t stackSize, mpe_ThreadId *threadId,
        const char *name);

/**
 * The <i>mpeos_threadDestroy()</i> function terminates the current thread
 * or a specified target operating system thread.  If the target thread identifier
 * is zero the current (calling) thread is terminated.  If the OS does not
 * support the ability to terminate arbitrary threads, then this function will
 * return an error.
 *
 * @param threadId This is the identifier/handle of the target thread to terminate.  
 *          It is the original handle returned from the thread creation API.  If the
 *          thread identifier is zero or mathes the identifier of the current
 *          thread, then the calling thread will be terminated.
 * @return An MPE error code if the destroy fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_threadDestroy(mpe_ThreadId threadId);

/**
 * The <i>mpeos_threadAttach()</i> function registers/unregisters 
 * a thread that was not previously not launched with the <i>mpeos_threadCreate<i/> 
 * API into the mpeos_ framework.  This operation effectively creates or release 
 * thread tracking information for a thread not created with the 
 * <i>mpeos_threadCreate<i/> API.  Most likely this is only utilized for the 
 * initial primordial thread of the VM.  
 <p>
 * If the thread identifier pointer is NULL, the call is considered a "detach" 
 * call and all resources established during the "attach" phase will be released.
 *
 * @param threadId is a pointer for returning the thread's identifier or
 *          handle.  The identifier is of the <i>mpe_ThreadId<i/> type.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_threadAttach(mpe_ThreadId *threadId);

/**
 * The <i>mpeos_threadSetPriority()</i> function shall set the priority of the
 *          the specified thread.  If the target OS does not support modification
 *          of a thread's priority after creation, this function shall do nothing.
 *
 * @param threadId is the identifier of the target thread.
 * @param priority is the new priority level.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_threadSetPriority(mpe_ThreadId threadId, uint32_t priority);

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
mpe_Error mpeos_threadGetStatus(mpe_ThreadId threadId, uint32_t *threadStatus);

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
mpe_Error mpeos_threadSetStatus(mpe_ThreadId threadId, uint32_t threadStatus);

/**
 * The <i>mpeos_threadGetData()</i> function gets the target thread's "thread local 
 * storage".  If the thread identifier parameter is zero or matches the identifier
 * of the current thread, then the calling thread's "thread local storage" is returned.
 *
 * @param threadId is the identifier of the target thread.
 * @param threadLocals is a void pointer for returning the thread specific data
 *          pointer.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_threadGetData(mpe_ThreadId threadId, void **threadLocals);

/**
 * The <i>mpeos_threadSetData()</i> function sets the target thread's local storage.
 * If the thread identifier parameter is zero or matches the identifier of the current 
 * thread, then the calling thread's "thread local storage" is set.
 *
 * @param threadId is the identifier of the target thread.
 * @param threadLocals is a void pointer to the thread specific data.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_threadSetData(mpe_ThreadId threadId, void *threadLocals);

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
        mpe_ThreadPrivateData **data);

/**
 * The <i>mpeos_threadGetCurrent()</i> function retrieves the thread identifier
 * of the current (calling) thread.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_threadGetCurrent(mpe_ThreadId *threadId);

/**
 * The <i>mpeos_threadSleep()</i> function will put the current thread to sleep
 * (i.e. suspend execution) for specified time.  The amount of time that the thread
 * will be suspened is the sum of the number of milliseconds and microseconds
 * specified.
 *
 * @param milliseconds is the number of milliseconds to sleep.
 * @param microseconds is the number of microseconds to sleep.
 * @return The <i>MPE_SUCCESS<i/> is returned upon reactivation of the thread
 *         after the specified suspension period.
 */
mpe_Error mpeos_threadSleep(uint32_t milliseconds, uint32_t microseconds);

/**
 * The <i>mpeos_threadYield()</i> function will give up the remainder of the calling
 * thread's current timeslice.
 */
void mpeos_threadYield(void);

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
mpe_Error mpeos_threadSuspend(mpe_ThreadId threadId);

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
mpe_Error mpeos_threadResume(mpe_ThreadId threadId);

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
mpe_Error mpeos_threadSetName(mpe_ThreadId threadId, const char* name);

#ifdef __cplusplus
}
#endif
#endif /* _MPEOS_THREAD_H */
