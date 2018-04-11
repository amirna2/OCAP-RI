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

#include "javavm/include/porting/float.h"
#include "javavm/include/porting/threads.h"
#include "javavm/include/porting/sync.h"
#include "javavm/include/globals.h"
#include "javavm/include/assert.h"
#include "javavm/include/utils.h"
#include <sys/types.h>
#include <signal.h>
#include <assert.h>
#include <stdio.h>
#include <string.h>
#include <fcntl.h>

#include <mpe_dbg.h>

/* Define this along with CVM_DEBUG for debugging. */
//#define CVM_DEBUG_THREADS


void CVMthreadYield(void)
{
    mpe_threadYield();
}

#ifdef CVM_THREAD_SUSPENSION
void
CVMthreadSuspend(CVMThreadID *t)
{
    if (t == CVMthreadSelf())
    {
        t->suspended = CVM_TRUE;
        mpe_threadSuspend(t->tid);
    }
    else
    {
        mpe_mutexAcquire(t->lock);

        if ( !t->suspended )
        {
#if defined(CVM_DEBUG) && defined (CVM_DEBUG_THREADS)
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JVM,"CVMthreadSuspend: Suspending thread. Thread = %x.\n", t);
#endif
            t->suspended = CVM_TRUE;

            /* If we are not currently in wait(), then just go ahead
             and suspend the thread.  If we are in a wait, the suspend
             cv will be checked when the thread wakes up */
            if (t->in_wait)
            {
#if defined(CVM_DEBUG) && defined (CVM_DEBUG_THREADS)
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JVM,"CVMthreadSuspend: Thread is in wait -- unsetting cond var. Thread = %x.\n", t);
#endif
                mpe_condUnset(t->suspend_cv);
            }
            else
            {
#if defined(CVM_DEBUG) && defined (CVM_DEBUG_THREADS)
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JVM,"CVMthreadSuspend: Thread is running normally -- suspending. Thread = %x.\n", t);
#endif
                mpe_threadSuspend(t->tid);
            }
        }
        mpe_mutexRelease(t->lock);
    }
}

void
CVMthreadResume(CVMThreadID *t)
{
    mpe_mutexAcquire(t->lock);

    if ( t->suspended )
    {
#if defined(CVM_DEBUG) && defined (CVM_DEBUG_THREADS)
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JVM,"CVMthreadResume: resuming suspended thread. Thread = %x.\n", t);
#endif
        t->suspended = CVM_FALSE;
        mpe_condSet(t->suspend_cv);

        if ( !t->in_wait )
        {

#if defined(CVM_DEBUG) && defined (CVM_DEBUG_THREADS)
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JVM,"CVMthreadResume: resuming thread NOT suspended in wait mode. Thread = %x.\n", t);
#endif
            mpe_threadResume(t->tid);
        }
    }

    mpe_mutexRelease(t->lock);
}
#endif /* CVM_THREAD_SUSPENSION */

#ifdef CVM_JIT_PROFILE
#include <unistd.h>
#include <sys/param.h>
#include <sys/time.h>

extern CVMBool __profiling_enabled;
extern struct itimerval prof_timer;

#endif

/**
 * <i>CVMthreadCreate</i>
 *
 * Creates a new, runnable thread with the given Java language priority 
 * and C stack size. The new thread must call func(arg) upon creation and 
 * clean up after itself and exit when func() returns.
 */
#ifdef CVM_DEBUG
static int32_t thread_num = 1;
#endif

CVMBool CVMthreadCreate(CVMThreadID *tid, CVMSize stackSize, CVMInt32 priority,
        void(*func)(void *), void *arg)
{
#ifdef CVM_DEBUG
    char buffer[50];
    sprintf(buffer, "CVM #%d", (int)thread_num++);
#endif
    if (mpe_threadCreate(func, arg, priority, stackSize, &tid->tid,
#ifdef CVM_DEBUG
            buffer
#else
            NULL
#endif
    ) != MPE_SUCCESS)
        return CVM_FALSE;

    if (mpe_threadSetData(tid->tid, (void*) tid) != MPE_SUCCESS)
        return CVM_FALSE;

    return CVM_TRUE;
}

CVMBool CVMthreadAttach(CVMThreadID *self, CVMBool orphan)
{
    if (mpe_mutexNew(&self->lock) != MPE_SUCCESS)
        goto exit0;

    if (mpe_condNew(TRUE, FALSE, &self->wait_cv) != MPE_SUCCESS)
        goto exit1;

    if (mpe_condNew(FALSE, TRUE, &self->suspend_cv) != MPE_SUCCESS)
        goto exit1_5;

    if (mpe_mutexNew(&self->locked) != MPE_SUCCESS)
        goto exit2;

    mpe_mutexAcquire(self->lock);
    if (orphan)
    {
        /* attach the thread into MPE and store the id in the CVMThreadID */
        if (mpe_threadAttach(&self->tid) != MPE_SUCCESS)
        {
            goto exit3;
        }
    }
    else
    {
        /* associate the CVMThreadID with this thread */
        if (mpe_threadGetCurrent(&self->tid) != MPE_SUCCESS)
        {
            goto exit3;
        }

        /* FIX: compute stack top and store it in the CVMThreadID struct */
    }

    /* store the CVMThreadID in the thread specific data */
    if (mpe_threadSetData(self->tid, (void *) self) != MPE_SUCCESS)
    {
        goto exit3;
    }

    self->next = NULL;
    self->prev = NULL;
    self->suspended = CVM_FALSE;
    self->in_wait = CVM_FALSE;

    mpe_mutexRelease(self->lock);
    return CVM_TRUE;

    exit3: mpe_mutexDelete(self->locked);
    exit2: mpe_condDelete(self->suspend_cv);
    exit1_5: mpe_condDelete(self->wait_cv);
    exit1: mpe_mutexDelete(self->lock);
    exit0: return CVM_FALSE;
}

void CVMthreadDetach(CVMThreadID *self)
{
    /* delete the synchronization objects */
    mpe_mutexDelete(self->lock);
    mpe_condDelete(self->wait_cv);
    mpe_mutexDelete(self->locked);

    /* detach the thread */
    mpe_threadAttach( NULL);
}

CVMBool CVMthreadStackCheck(CVMThreadID *self, CVMUint32 redZone)
{
#ifdef LINUX_WATCH_STACK_GROWTH
    if ((void *)&self < self->stackLimit)
    {
        size_t size = (char *)self->stackBottom - (char *)&self;
        size_t dsize = (char *)self->stackLimit - (char *)&self;
        size_t m;
        pthread_mutex_lock(&stk_mutex);
        m = max_stack += dsize;
        pthread_mutex_unlock(&stk_mutex);
        fprintf(stderr, "New stack size %dKB reached for thread %ld "
                "(%dKB all threads)\n",
                size / 1024,
                self->pthreadCookie,
                m / 1024);
        self->stackLimit = (char *)&self;
    }
#endif
    /* return (char *)self->stackTop + redZone < (char *)&self; */
    /* FIXME: Implement this */
    return CVM_TRUE;
}

void CVMthreadInterruptWait(CVMThreadID *thread)
{
    mpe_mutexAcquire(thread->lock);

#if defined(CVM_DEBUG) && defined (CVM_DEBUG_THREADS)
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JVM,"CVMthreadInterruptWait: thread %x interrupting thread %x.\n", CVMthreadSelf(), thread);
#endif
    /* Flag wait interrupted, i.e. thread will dequeue itself from cond variable. */
    thread->interrupted = CVM_TRUE;

    if (thread->in_wait)
    {
#if defined(CVM_DEBUG) && defined (CVM_DEBUG_THREADS)
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JVM,"CVMthreadInterruptWait: thread %x interrupting thread %x in wait mode.\n", CVMthreadSelf(), thread);
#endif
        /* "set" thread condition variable to wake up the thread */
        mpe_condSet(thread->wait_cv);
    }
    mpe_mutexRelease(thread->lock);
}

CVMBool CVMthreadIsInterrupted(CVMThreadID *thread, CVMBool clearInterrupted)
{
    if (clearInterrupted)
    {
        CVMBool wasInterrupted;
        assert(thread == CVMthreadSelf());
        wasInterrupted = thread->interrupted;
        thread->interrupted = CVM_FALSE;
        return wasInterrupted;
    }
    else
    {
        return thread->interrupted;
    }
}

CVMThreadID *
CVMthreadSelf()
{
    mpe_ThreadId mpeThread;
    CVMThreadID *id;

    if (mpe_threadGetCurrent(&mpeThread) != MPE_SUCCESS)
        return NULL;

    if (mpe_threadGetData(mpeThread, (void **) &id) != MPE_SUCCESS)
        return NULL;

    return id;
}
