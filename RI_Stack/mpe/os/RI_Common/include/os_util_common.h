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

#if !defined(_OS_UTIL_COMMON_H)
#define _OS_UTIL_COMMON_H

#ifdef __cplusplus
extern "C"
{
#endif

#include <os_types.h>   /* Resolve basic type references. */
#include <setjmp.h>     /* Include Windows setjmp.h header. */

/***
 * Type definitions:
 */

/* setjmp/longjmp implementation buffer. */
typedef jmp_buf os_JmpBuf;

/* STB configuration information structure. */
typedef struct os_STBConfig_s
{
    void *os_reserved[4]; /* Reserved space. */
} os_STBConfig;

/**
 * A power state change is identified by trapping a power key
 * press in the key events handler. togglePowerMode() will use the
 * Event Dispatcher to send events to a registered listener in the Java layer.
 */
void togglePowerMode(void);

/**
 * A configuration structure for managing the porting environment.
 */
typedef struct os_EnvConfig_s
{
    char *os_config; /* The location of the mpeenv.ini file. */
    int32_t os_numModules; /* The number of moduels in the os_modulePath. */
    char **os_modulePath; /* An array of paths for resolving DLLs. */
} os_EnvConfig;

/**
 * Initialize the porting environment configuration.
 *
 * @param env A pointer to the structure containing the information
 * necessary for intiializing the ports environment.
 */
void os_envInit(os_EnvConfig *env);

mpe_Error removeFiles(char *dirPath);

/**
 * Return the number of tuners from the underlying target platform.
 */
int os_getNumTuners(void);

#ifdef __cplusplus
}
#endif

#endif /* _OS_UTIL_COMMON_H */
