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
 * Machine-dependent thread definitions.
 */

#ifndef _MPE_THREADS_MD_H
#define _MPE_THREADS_MD_H

#include <mpe_types.h>
#include <mpe_os.h>
#include "javavm/include/porting/sync.h"
#include "javavm/include/threads_arch.h"

struct CVMThreadID
{
    mpe_ThreadId tid;
    void *stackTop;
    mpe_Mutex lock;

    volatile CVMBool suspended;

    /* support for CVMmutexSetOwner */
    mpe_Mutex locked;

    /* support for Thread.interrupt */
    volatile CVMBool in_wait;
    CVMBool interrupted;
    CVMBool notified;

    /* This CV is used to support Object.wait() behavior in CVM
     condition variables */
    mpe_Cond wait_cv;

    /* This CV is used to support the thread suspension APIs */
    mpe_Cond suspend_cv;

    /* support for close */
    int fd; /* only valid while thread is on I/O queue! */

    /* IO or wait queue */
    CVMThreadID *next;
    CVMThreadID *prev;
};

#define CVMthreadSetPriority	mpe_threadSetPriority

/* NOTE: The current redzone and stack sizes below are taken straight from the
 the debug version of Solaris port.  These numbers will probably fine 
 for the Linux port but may not be optimum.  To obtain the optimum
 numbers, one will have to run a static stack analysis written for
 analyzing the assembly code of CVM on the linux platform. 
 */
#define CVM_THREAD_MIN_C_STACK_SIZE				(32 * 1024)

/* 
 * The static stack analysis tool doesn't know additional stack requirement 
 * for the execution of library functions.
 * Use a reasonable value for the library function overhead. 
 */
#define CVM_REDZONE_Lib_Overhead                                 (3 * 1024)

#define CVM_REDZONE_ILOOP                                       \
    (5976 + CVM_REDZONE_Lib_Overhead) /*  8.84KB */
#define CVM_REDZONE_CVMimplementsInterface                      \
    (1680 + CVM_REDZONE_Lib_Overhead) /*  4.64KB */
#define CVM_REDZONE_CVMCstackCVMpc2string                       \
    (1680 + CVM_REDZONE_Lib_Overhead) /*  4.64KB */
#define CVM_REDZONE_CVMCstackCVMID_objectGetClassAndHashSafe    \
    (1944 + CVM_REDZONE_Lib_Overhead) /*  4.90KB */
#define CVM_REDZONE_CVMclassLookupFromClassLoader               \
    (6704 + CVM_REDZONE_Lib_Overhead) /*  9.55KB */
#define CVM_REDZONE_CVMclassLink                                \
    (3592 + CVM_REDZONE_Lib_Overhead) /*  6.51KB */
#define CVM_REDZONE_CVMCstackmerge_fullinfo_to_types            \
    (1936 + CVM_REDZONE_Lib_Overhead) /*  4.89KB */
#define CVM_REDZONE_CVMsignalErrorVaList                        \
    (6040 + CVM_REDZONE_Lib_Overhead) /* 8.90KB */
#define CVM_REDZONE_CVMJITirdumpIRNode                          \
    (1024 + CVM_REDZONE_Lib_Overhead) /* 4KB */
#define CVM_REDZONE_disassembleRangeWithIndentation             \
    (1024 + CVM_REDZONE_Lib_Overhead) /* 4KB */
#define CVM_REDZONE_CVMcpFindFieldInSuperInterfaces             \
    (1024 + CVM_REDZONE_Lib_Overhead)

#endif /* _MPE_THREADS_MD_H */
