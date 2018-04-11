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

#if !defined(_OS_THREAD_H)
#define _OS_THREAD_H

#include <os_types.h>       /* Resolve basic type references. */
#include <os_sync.h>       /* Resolve basic type references. */
#include <os_util.h>

#ifdef __cplusplus
extern "C"
{
#endif

/***
 * Thread macro and type definitions:
 */
#define OS_THREAD_PRIOR_MAX  (31)   /* Maximum thread priority. */
#define OS_THREAD_PRIOR_MIN  (24)   /* Minimum thread priority. */
#define OS_THREAD_PRIOR_DFLT (28)   /* Default thread priority. */
#define OS_THREAD_PRIOR_INC  (1)    /* Priority setting increment. */

#define OS_THREAD_PRIOR_SYSTEM_HI (OS_THREAD_PRIOR_MAX)
#define OS_THREAD_PRIOR_SYSTEM_MED (OS_THREAD_PRIOR_MAX-1)
#define OS_THREAD_PRIOR_SYSTEM (OS_THREAD_PRIOR_MAX-2)

/*
 * Define standard thread stack size:
 *
 * The default stack size is large enough for TCP/IP stack usage, where
 * the PTV documentation indicates at least 10k is required.
 */
#define OS_THREAD_STACK_SIZE (12*1024)

/**
 * Implementation specific type declarations:
 *
 * Note this is exposed for use within multiple files of the mpeos layer.
 */

typedef uint32_t os_ThreadStat;

/* Thread description structure */
typedef struct _threadDesc
{
    uint32_t refcount; /* Reference count thread attaches */
    void *vmlocals; /* VM thread local pointer. */
    HANDLE handle; /* Windows handle associated w/ this thread */
    os_Mutex mutex; /* Mutex for accessing thread descriptor. */
    DWORD id; /* Windows thread unique identifier */
#ifdef MPE_FEATURE_THREAD_SUSPEND
    mpe_Bool is_blocked; /* Thread is blocked on a mutex */
    mpe_Bool is_waiting; /* Thread is waiting on a cond var */
    mpe_Bool is_suspended_while_blocked; /* Thread is suspended while blocked on a mutex */
    mpe_Bool is_suspended_while_waiting; /* Thread is suspended wnile waiting on a cond var */
    mpe_Bool is_suspended; /* This thread is currently suspended */
    os_Condition suspend_cond; /* Used to handle suspend/resume in coordination with other sync primitives */
#endif
    os_Condition start_cond; /* Used to make sure the creating thread does not return until the new thread has started */
    os_ThreadStat status; /* MPE implementation thread status. */
    void* locals; /* MPE thread local storage. */
    os_JmpBuf exitJmp; /* Termination jump buffer. */
    void (*entry)(void*); /* Thread entry point. */
    void *entryData; /* Thread entry point data. */
    struct _threadDesc *prev; /* Previous descriptor. */
    struct _threadDesc *next; /* Next descriptor. */
    char name[256];
} threadDesc;

/* Windows thread local storage index where our threadDesc is kept */
extern DWORD TLS_INDEX;

typedef threadDesc* os_ThreadId; /* Define the thread ID type. */

#ifdef __cplusplus
}
#endif

#endif /* _OS_THREAD_H */
