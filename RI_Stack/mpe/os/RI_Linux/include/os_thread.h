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

#ifndef _OS_THREAD_H
#define _OS_THREAD_H

#ifdef USE_THREAD_POOL
#include <semaphore.h>      // get thread synchronization primitive (posix semaphore)
#endif

#include <pthread.h>

#include "os_types.h"		/* Resolve basic type references. */
#include "os_util_common.h"

#ifdef __cplusplus
extern "C"
{
#endif

/***
 * Thread macro and type definitions:
 *
 * TODO - should we reference Linux thingies here?
 */
#if !defined(OS_THREAD_PRIOR_MAX)
#define OS_THREAD_PRIOR_MAX  (99)	/* Maximum thread priority. */
#endif
#if !defined(OS_THREAD_PRIOR_MIN)
#define OS_THREAD_PRIOR_MIN  (11)   /* Minimum thread priority. */
#endif
#if !defined(OS_THREAD_PRIOR_DFLT)
#define OS_THREAD_PRIOR_DFLT (50)   /* Default thread priority. */
#endif
#if !defined(OS_THREAD_PRIOR_INC)
#define OS_THREAD_PRIOR_INC  (1)   /* Priority setting increment. */
#endif
#if !defined(OS_THREAD_PRIOR_SYSTEM_HI)
#define OS_THREAD_PRIOR_SYSTEM_HI (OS_THREAD_PRIOR_MAX)
#endif
#if !defined(OS_THREAD_PRIOR_SYSTEM_MED)
#define OS_THREAD_PRIOR_SYSTEM_MED (OS_THREAD_PRIOR_MAX-15)
#endif
#if !defined(OS_THREAD_PRIOR_SYSTEM)
#define OS_THREAD_PRIOR_SYSTEM (OS_THREAD_PRIOR_MAX-30)
#endif

/*
 * Define standard thread stack size:
 */
#define OS_THREAD_STACK_SIZE (64*1024)

typedef uint32_t os_ThreadStat; /* Define thread status type. */
struct mpe_ThreadPrivateData;

typedef struct _threadDesc
{
    pthread_t td_id; /* Thread identifier */
    void *td_vmlocals; /* VM thread local pointer */
    os_ThreadStat td_status; /* MPE implementation thread status */
    os_JmpBuf td_exitJmp; /* Termination jump buffer */
    void (*td_entry)(void*); /* Thread entry point */
    void *td_entryData; /* Thread entry point data */
    pthread_attr_t td_attr; /* Thread attributes */
    char *td_name; /* Thread name */
    struct mpe_ThreadPrivateData* td_locals; /* MPE thread local storage pointer */

    mpe_Bool td_blocked; /* Is this thread blocked on a mutex? */
    mpe_Bool td_waiting; /* Is this thread waiting on a cond var? */
    mpe_Bool td_inHandler; /* Is this thread in the signal handler? */
    mpe_Bool td_suspended; /* Is this thread currently suspeneded? */

    int td_blocked_fd; /* If this thread is blocked on a fd read/write/etc. the fd is
                          set here.  Otherwise this field is -1 */

    struct _threadDesc *td_next; /* Next descriptor. */
    struct _threadDesc *td_prev; /* Next descriptor. */
} ThreadDesc;

typedef ThreadDesc* os_ThreadId;

void suspendMe();

#ifdef __cplusplus
}
#endif
#endif /* _OS_THREAD_H */
