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

#include "javavm/include/porting/sync.h"
#include "javavm/include/porting/doubleword.h"
#include "javavm/include/porting/threads.h"
#include "javavm/include/porting/float.h"	/* for setFPMode() */
#include <sys/time.h>
#include <errno.h>
#include <signal.h>
#include <unistd.h>
#include <stdio.h>
#include <sys/types.h>
#include "javavm/include/assert.h"
#ifdef CVM_JVMPI
#include "javavm/include/globals.h"
#endif
#ifdef CVM_JIT
#include "javavm/include/porting/jit/jit.h"
#endif

#include <mpe_dbg.h>

/* Define this along with CVM_DEBUG for debugging. */
//#define CVM_DEBUG_SYNC


CVMBool CVMmutexInit(CVMMutex *m)
{
    return ((mpe_mutexNew(&m->mutex) == MPE_SUCCESS) ? CVM_TRUE : CVM_FALSE);
}

void CVMmutexDestroy(CVMMutex *m)
{
    mpe_mutexDelete(m->mutex);
}

CVMBool CVMmutexTryLock(CVMMutex *m)
{
    return ((mpe_mutexAcquireTry(m->mutex) == MPE_SUCCESS) ? CVM_TRUE
            : CVM_FALSE);
}

void CVMmutexLock(CVMMutex *m)
{
    mpe_mutexAcquire(m->mutex);
}

void CVMmutexUnlock(CVMMutex *m)
{
    mpe_mutexRelease(m->mutex);
}

#ifdef CVM_ADV_MUTEX_SET_OWNER
void
CVMmutexSetOwner(CVMThreadID *self, CVMMutex *m, CVMThreadID *target)
{
    m->mutex = target->locked;
    CVMmutexTryLock(m);
}
#endif

static void enqueue(CVMCondVar *c, CVMThreadID *t)
{
    mpe_mutexAcquire(c->mutex);

    if (c->waiters == NULL)
        c->waiters = t;
    else
    {
        t->prev = c->last;
        c->last->next = t;
    }

    c->last = t;

    mpe_mutexRelease(c->mutex);
}

static CVMThreadID *
dequeue(CVMCondVar *c)
{
    CVMThreadID *t;

    mpe_mutexAcquire(c->mutex);

    t = c->waiters;
    if (t != NULL)
    {
        if (c->last == t)
            c->last = NULL;
        else
            t->next->prev = NULL;

        c->waiters = t->next;
        t->next = NULL;
    }

    mpe_mutexRelease(c->mutex);

    return t;
}

static void dequeue_me(CVMCondVar *c, CVMThreadID *t)
{
    mpe_mutexAcquire(c->mutex);
    
    if (c->waiters == t)
        c->waiters = t->next;
    else
        t->prev->next = t->next;

    if (c->last == t)
        c->last = t->prev;
    else
        t->next->prev = t->prev;

    t->next = NULL;
    t->prev = NULL;

    mpe_mutexRelease(c->mutex);
}

CVMBool CVMcondvarWait(CVMCondVar* c, CVMMutex* m, CVMJavaLong millis)
{
    CVMJavaInt i;
    CVMJavaLong l;
    CVMBool waitForever = CVM_FALSE;
    CVMThreadID *self = CVMthreadSelf();

    /* a zero value means wait forever */
    if (!CVMlongEqz(millis))
    {
        i = CVMlong2Int(millis);
        l = CVMint2Long(i);

        /* check for overflow */
        if (!CVMlongEq(millis, l))
        {
            /* the value overflows so wait forever */
            waitForever = CVM_TRUE;
        }
    }
    else
    {
        waitForever = CVM_TRUE;
    }

    /* Acquire the thread mutex */
    mpe_mutexAcquire(self->lock);

    /* If a thread has been interrupted when Object.wait() is called,
     the interrupt flag must be cleared */
    if (self->interrupted)
    {
        self->interrupted = CVM_FALSE;
        mpe_mutexRelease(self->lock);
        return CVM_FALSE;
    }

    /* Clear our notification flag and assume that we will be waiting */
    self->in_wait = CVM_TRUE;
    self->notified = CVM_FALSE;

    /* add this thread to the list of waiters */
    enqueue(c, self);

    /* Release the cond var mutex provided by the caller */
    CVMmutexUnlock(m);

    /* Done modifying thread state */
    mpe_mutexRelease(self->lock);

    /*
     * Thread will now wait on its own local condition variable until either
     * the condition variable is "notified", "interrupted" or the wait
     * times out.
     */
#if defined(CVM_DEBUG) && defined (CVM_DEBUG_SYNC)
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JVM, "CVMcondvarWait: Waiting on condition var. cond var = %x, thread = %x.\n", c, self);
#endif
    if (waitForever)
        mpe_condGet(self->wait_cv);
    else
    {
        mpe_condWaitFor(self->wait_cv, millis);
    }

    mpe_mutexAcquire(self->lock);

#if defined(CVM_DEBUG) && defined (CVM_DEBUG_SYNC)
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JVM, "CVMcondvarWait: Awakening on condition var. cond var = %x, thread = %x.\n", c, self);
#endif

    /* Reset the wait CV and flag while we hold the mutex */
    self->in_wait = CVM_FALSE;

    /*
     * Verify that this thread is not still in a suspended state (e.g. debugger) 
     * before reacquiring the caller's mutex.  This condition should only occur
     * in debug (JDWP) builds. 
     */
    if (self->suspended)
    {
#if defined(CVM_DEBUG) && defined (CVM_DEBUG_SYNC)
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JVM, "CVMcondvarWait: thread has been suspended! cond var = %x, thread = %x.\n", c, self);
#endif
        mpe_mutexRelease(self->lock);
        mpe_condGet(self->suspend_cv);

#if defined(CVM_DEBUG) && defined (CVM_DEBUG_SYNC)
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JVM, "CVMcondvarWait: thread has been resumed! cond var = %x, thread = %x.\n", c, self);
#endif
    }
    else
    {
        mpe_mutexRelease(self->lock);
    }

    /* Reacquire the caller's cond variable mutex. */
    CVMmutexLock(m);

    mpe_mutexAcquire(self->lock);

    /*
     * Determine reason for being woken up (e.g. interrupted/notified/timeout).
     * If interrupted or time out, must dequeue self from cond variable queue.
     */
    if (!self->notified)
    {
#if defined(CVM_DEBUG) && defined (CVM_DEBUG_SYNC)
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JVM, "CVMcondvarWait: thread either timed out or was interrupted! cond var = %x, thread = %x.\n", c, self);
#endif
        dequeue_me(c, self);
    }

    /* Return interrupted condition. */
    if (self->interrupted)
    {
#if defined(CVM_DEBUG) && defined (CVM_DEBUG_SYNC)
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JVM, "CVMcondvarWait: thread was interrupted! cond var = %x, thread = %x.\n", c, self);
#endif
        self->interrupted = CVM_FALSE; /* Clear interrupted flag. */

        /* Release thread lock and reacquire the caller's cond variable mutex. */
        mpe_mutexRelease(self->lock);

        return CVM_FALSE;
    }

#if defined(CVM_DEBUG) && defined (CVM_DEBUG_SYNC)
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JVM, "CVMcondvarWait: thread was notified or timed out -- Returning. cond var = %x, thread = %x.\n", c, self);
#endif

    /* Release thread lock*/
    mpe_mutexRelease(self->lock);

    /* Notified or timed out. */
    return CVM_TRUE;
}

CVMBool CVMcondvarInit(CVMCondVar * c, CVMMutex * m)
{
    if (mpe_mutexNew(&c->mutex) != MPE_SUCCESS)
        return CVM_FALSE;
    
    c->waiters = c->last = NULL;
    return CVM_TRUE;
}

void CVMcondvarDestroy(CVMCondVar * c)
{
    mpe_mutexDelete(c->mutex);
    c->waiters = c->last = NULL;
}

void CVMcondvarNotify(CVMCondVar * c)
{
    CVMThreadID *ptr;

    /* dequeue and notify the first thread in the list */
    if ((ptr = dequeue(c)) != NULL)
    {
#if defined(CVM_DEBUG) && defined (CVM_DEBUG_SYNC)
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JVM, "CVMcondNotify: Thread %x notifying a thread. cond var = %x, thread = %x.\n", CVMthreadSelf(), c, ptr);
#endif
        mpe_mutexAcquire(ptr->lock);

        ptr->notified = CVM_TRUE;
        if (ptr->in_wait)
        {
            mpe_condSet(ptr->wait_cv);
        }

        mpe_mutexRelease(ptr->lock);
    }
}

void CVMcondvarNotifyAll(CVMCondVar * c)
{
    /* traverse the list and "set" each thread synchronization object */
    CVMThreadID *ptr;

    /* traverse the list and set the notify flag and set each thread */
    while ((ptr = dequeue(c)) != NULL)
    {
        /* wake up the thread */
#if defined(CVM_DEBUG) && defined (CVM_DEBUG_SYNC)
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JVM, "CVMcondNotifyAll: Thread %x notifying a thread.  cond var = %x, thread = %x.\n", CVMthreadSelf(), c, ptr);
#endif
        mpe_mutexAcquire(ptr->lock);

        ptr->notified = CVM_TRUE;
        if (ptr->in_wait)
        {
            mpe_condSet(ptr->wait_cv);
        }

        mpe_mutexRelease(ptr->lock);
    }
}
