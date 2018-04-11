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

#ifndef _MPE_TYPES_H_
#define _MPE_TYPES_H_

/*
 ** Ensure that either MPE_BIG_ENDIAN or MPE_LITTLE_ENDIAN is defined for all
 ** MPE related code.
 */
#if !defined(MPE_BIG_ENDIAN) && !defined(MPE_LITTLE_ENDIAN)
#error Either MPE_BIG_ENDIAN or MPE_LITTLE_ENDIAN must be defined
#endif

/*
 ** This file declares the custom event definitions used by the MPE.
 */

#include <mpeos_types.h>

/*
 * It is assumed that the following integer types are defined by the platform,
 * either in <stdint.h> (if C99) or in <os_types.h>.
 *   int8_t  / uint8_t
 *   int16_t / uint16_t
 *   int32_t / uint32_t
 *   int64_t / uint64_t
 */

/* platform-independent types */
typedef uint32_t mpe_Error;
typedef int mpe_Bool;

/* insure that these common macros are defined */

#ifndef NULL
#define NULL 0
#endif

#ifndef FALSE
#define FALSE 0
#endif
#ifndef false
#define false FALSE
#endif

#ifndef TRUE
#define TRUE !FALSE
#endif
#ifndef true
#define true !false
#endif

/*
 * Define macro for exporting library functions.
 */
#define MPE_LIBEXPORT(type, symbol) OS_LIBEXPORT(type, symbol)

/*
 * Define macro for calculating the offest of a field in a struct:
 *
 * @param Struct is the structure pointer type
 * @param field is the field of interest in the struct
 */
#define MPE_OFFSET(Struc, field) ((uint32_t)&((Struc *)0)->field)

/*
 * Define macro for construction of a fake list head pointer:
 *
 * @param Struc is the structure pointer type
 * @param head is the address location to offset from (usually the first fake link)
 * @param lnk is the link field name within the struct
 */
#define MPE_FAKEHEAD(Struc, head, lnk) /*lint -e(413)*/ ((Struc *)((char*)&head - MPE_OFFSET(Struc, lnk)))

/*
 * Macros for preventing unused parameter warnings.
 */
#ifndef MPE_UNUSED_PARAM
#define MPE_UNUSED_PARAM(var)   { (void)var; }
#endif

/**
 * Type used to describe the intended "use" of allocated memory.
 * Some otential uses of the color value within the implementation
 * are:
 * <ul>
 *   <li> For use in tracing memory usage.
 *   <li> To indicate distinct allocation heaps.
 *   <li> To enable purging/compaction of one type of memory.
 * </ul>
 */

#if defined(PORT_MEM_COLORS)
#define PORT_MEM_COLORS_COMMA PORT_MEM_COLORS,
#if !defined(PORT_MEM_COLORS_NAMES)
#error 'PORT_MEM_COLORS_NAMES' must be defined if 'PORT_MEM_COLORS' is defined!
#endif
#else
#define PORT_MEM_COLORS_COMMA       /* no port-specific additional memory colors */
#define PORT_MEM_COLORS_NAMES       /* no port-specific additional memory colors */
#endif /* defined(PORT_MEM_COLORS) */

typedef enum
{
    MPE_MEM_SYSTEM = -1, /* For GetFreeSize and GetLargestFree only */
    MPE_MEM_GENERAL = 0, /* Generic color/type. */
    MPE_MEM_TEMP, /* Temporary memory. */
    MPE_MEM_JVM, /* JVM (includes system and java heaps) */
    MPE_MEM_GFX, /* Generic graphics memory. */
    MPE_MEM_GFX_LL, /* Low-level graphics memory (e.g., DFB). */
    MPE_MEM_SYNC, /* Synchronization primitives. */
    MPE_MEM_THREAD, /* Threads. */
    MPE_MEM_FILE, /* Generic file system memory. */
    MPE_MEM_FILE_CAROUSEL, /* Data and Object carousel. */
    MPE_MEM_MEDIA, /* Generic media. */
    MPE_MEM_UTIL, /* Util. */
    MPE_MEM_EVENT, /* Events. */
    MPE_MEM_FILTER, /* Section filtering memory. */
    MPE_MEM_POD, /* CableCard (aka POD) memory. */
    MPE_MEM_SI, /* Service Information memory. */
    MPE_MEM_TEST, /* Memory used during testing. */
    MPE_MEM_NET, /* Networking memory. */
    MPE_MEM_CC, /* Closed captioning module */
    MPE_MEM_DVR, /* DVR manager module */
    MPE_MEM_SND, /* Sound Support */
    MPE_MEM_FP, /* Front Panel module */
    MPE_MEM_PORT, /* Port-specific memory */
    MPE_MEM_STORAGE, /* Storage manager module */
    MPE_MEM_VBI, /* Vbi filtering memory. */
    MPE_MEM_HN, /* Home networking. */
    /* Add new ones here as necessary.
     * Generally at least one for every logical code module.
     * Potentially with additional sub-areas per code module.
     * May be a good idea to keep under 32 total (to allow use
     * in bitfield).
     */
    PORT_MEM_COLORS_COMMA /* Port-specific additional memory colors */
    MPE_MEM_NCOLORS
} mpe_MemColor;

#define MPE_MEM_COLOR_NAMES \
    "SYSTEM",                                   \
    "GENERAL",                                  \
    "TEMP",                                     \
    "JVM",                                      \
    "GFX",                                      \
    "GFX_LL",                                   \
    "SYNC",                                     \
    "THREAD",                                   \
    "FILE",                                     \
    "FILE_CAROUSEL",                            \
    "MEDIA",                                    \
    "UTIL",                                     \
    "EVENT",                                    \
    "FILTER",                                   \
    "POD",                                      \
    "SI",                                       \
    "TEST",                                     \
    "NET",                                      \
    "CC",                                       \
    "DVR",                                      \
    "SND",                                      \
    "FRONTPANEL",                               \
    "PORT",                                     \
    "STORAGE",                                  \
    "VBI",                                  \
    PORT_MEM_COLORS_NAMES

/**
 * Memory reclamation definitions
 */

#define MPE_MEM_CALLBACK_SYSEVENT ((mpe_MemColor)MPE_MEM_NCOLORS + 0x100)
#define MPE_MEM_CALLBACK_KILLAPP ((mpe_MemColor)MPE_MEM_CALLBACK_SYSEVENT + 1)

#define MPE_MEM_RECLAIM_THRESHOLD (0x0000)
#define MPE_MEM_RECLAIM_FAILURE   (0x1000)
#define MPE_MEM_RECLAIM_MASK      (0x1000)
#define MPE_MEM_COLOR_MASK        (0xFFF)

/**
 * Opaque type used to represent a handle to allocated memory.
 * Handles should be used to indicate that allocated memory
 * can be purged and or relocated.
 */
typedef struct
{
    int unused;
}*mpe_MemHandle;

/**
 * Type used to indicate the STB power status.
 */
typedef enum
{
    MPE_POWER_FULL = 1, /**< Full power mode. */
    MPE_POWER_STANDBY,
/**< Standby (low) poer mode. */
} mpe_PowerStatus;

/**
 * Type used to indicate the Audio status.
 */
typedef enum
{
    MPE_AUDIO_ON = 1,
    MPE_AUDIO_MUTED,
} mpe_AudioStatus;

#endif /* MPE_TYPES_H */

