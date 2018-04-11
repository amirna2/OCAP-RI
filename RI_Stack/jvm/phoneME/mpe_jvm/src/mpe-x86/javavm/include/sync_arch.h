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
 * CPU/OS-specific synchronization definitions.  Contains defintions
 * for all x86 operating system platforms.
 */

/*
 * Windows definitons:
 */
#ifdef MPE_TARGET_OS_WINDOWS

#ifndef _WIN32_SYNC_X86_H
#define _WIN32_SYNC_X86_H

/* Use atomic operation for fast locking on x86 */
#define CVM_FASTLOCK_TYPE CVM_FASTLOCK_ATOMICOPS

#ifndef _ASM

/*
 * These map directly to Windows APIs.
 */
#define CVMatomicCompareAndSwap(a, n, o)			\
    (CVMAddr)InterlockedCompareExchange((LONG volatile*)(a), (LONG)(n), (LONG)(o))

#define CVMatomicSwap(a, n)	\
    (CVMAddr)InterlockedExchange((LONG volatile*)(a), (LONG)(n))

/*
 * TODO: determine if this file is required.  It's used in phoneme port for including
 *       jit specific compare and swap routin.  That jit code doesn't exist in our port.
 */
//#include "javavm/include/sync_cpu.h"

#endif /* !_ASM */
#endif /* _WIN32_SYNC_X86_H */

#endif /* MPE_TARGET_OS_WINDOWS */

#ifdef MPE_TARGET_OS_LINUX
/*
 * Linux definitons:
 */

#ifndef _LINUX_SYNC_i686_H
#define _LINUX_SYNC_i686_H

#if defined(i486) || defined(__i486__) || defined(__i486) || \
    defined(i686) || defined(__i686__) || defined(__i686) || \
    defined(pentiumpro) || defined(__pentiumpro__) || defined(__pentiumpro)

/* Use atomic operation for fast locking on x86 */

#define CVM_FASTLOCK_TYPE CVM_FASTLOCK_ATOMICOPS

#define CVMatomicCompareAndSwap(a, n, o)	\
	atomicCmpSwap((n), (a), (o))

/* Purpose: Performs an atomic compare and swap operation. */
static inline CVMAddr atomicCmpSwap(CVMAddr new_value, volatile CVMAddr *addr,
        CVMAddr old_value)
{
    int x;
    asm volatile (
            "lock cmpxchgl %3, %1"
            : "=a" (x)
            : "m" (*addr), "a" (old_value), "q" (new_value)
            /* clobber? */);
    return (CVMAddr)x;
}

#define CVMatomicSwap(a, n)	\
        atomicSwap((n), (a))

/* Purpose: Performs an atomic swap operation. */
static inline CVMAddr atomicSwap(CVMAddr new_value, volatile CVMAddr *addr)
{
    int x;
    asm volatile (
            "xchgl %1, %2"
            : "=a" (x)
            : "m" (*addr), "a" (new_value)
            /* clobber? */);
    return (CVMAddr)x;
}

#endif

#endif /* _LINUX_SYNC_i686_H */

#endif /* MPE_TARGET_OS_LINUX */
