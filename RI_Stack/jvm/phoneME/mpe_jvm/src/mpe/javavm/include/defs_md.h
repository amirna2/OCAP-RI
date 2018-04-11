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

#ifndef _MPE_DEFS_MD_H
#define _MPE_DEFS_MD_H

/*
 * This file declares basic types. These are C types with the width
 * encoded in a one word name, and a mapping of Java basic types to C types.
 *
 */

#include <mpe_types.h>
#include <os_types.h>

typedef int8_t CVMInt8;
typedef int16_t CVMInt16;
typedef int32_t CVMInt32;
typedef int64_t CVMInt64;

typedef uint8_t CVMUint8;
typedef uint16_t CVMUint16;
typedef uint32_t CVMUint32;

typedef float CVMfloat32;
typedef double CVMfloat64;

typedef size_t CVMSize;

#include "javavm/include/defs_arch.h"

#if defined(MPE_TARGET_OS_LINUX) || defined(MPE_TARGET_OS_WINDOWS)
#define CONST64(val) (val ## LL)
#define UCONST64(val) (val ## ULL)
#endif

#define CVM_HDR_ANSI_LIMITS_H	<limits.h>
#define CVM_HDR_ANSI_STDIO_H	<stdio.h>
#define CVM_HDR_ANSI_STDDEF_H	<stddef.h>
#define CVM_HDR_ANSI_STRING_H	<string.h>
#define CVM_HDR_ANSI_STDLIB_H	<stdlib.h>
#define CVM_HDR_ANSI_TIME_H     <time.h>
#define CVM_HDR_ANSI_SETJMP_H	<setjmp.h>
#define CVM_HDR_ANSI_CTYPE_H	<ctype.h>
#define CVM_HDR_ANSI_ASSERT_H	<assert.h>
#define CVM_HDR_ANSI_STDINT_H   "javavm/include/defs_md.h"

#define CVM_HDR_ANSI_ERRNO_H	"javavm/include/errno_md.h"
#define CVM_HDR_ANSI_STDARG_H	"javavm/include/stdarg_md.h"

#define CVM_HDR_INT_H           "javavm/include/int_md.h"
#define CVM_HDR_FLOAT_H         "javavm/include/float_md.h"
#define CVM_HDR_DOUBLEWORD_H    "javavm/include/doubleword_md.h"
#define CVM_HDR_JNI_H           "javavm/include/jni_md.h"
#define CVM_HDR_GLOBALS_H       "javavm/include/globals_md.h"
#define CVM_HDR_THREADS_H       "javavm/include/threads_md.h"
#define CVM_HDR_SYNC_H          "javavm/include/sync_md.h"
#define CVM_HDR_LINKER_H        "javavm/include/defs_md.h" /* no-op */
#define CVM_HDR_MEMORY_H        "javavm/include/memory_md.h"
#define CVM_HDR_PATH_H          "javavm/include/path_md.h"
#define CVM_HDR_IO_H            "javavm/include/io_md.h"
#define CVM_HDR_NET_H           "javavm/include/net_md.h"
#define CVM_HDR_TIME_H          "javavm/include/time_md.h"
#define CVM_HDR_ENDIANNESS_H    "javavm/include/endianness_md.h"
#define CVM_HDR_SYSTEM_H        "javavm/include/defs_md.h" /* no-op */
#define CVM_HDR_TIMEZONE_H      "javavm/include/defs_md.h" /* no-op */

#define CVM_HDR_JIT_JIT_H       "javavm/include/jit/jit_arch.h"

#define CVM_ADV_MUTEX_SET_OWNER

#define CVM_HAVE_PROCESS_MODEL

#endif /* _MPE_DEFS_MD_H */
