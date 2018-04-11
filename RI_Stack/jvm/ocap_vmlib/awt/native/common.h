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

#ifndef MPE_COMMON_H_INCLUDED /*{ MPE_COMMON_H_INCLUDED */
# define MPE_COMMON_H_INCLUDED

#include <mpe_os.h>
#include <mpe_dbg.h>

#ifndef UNUSED /* { */
#define UNUSED(x) { (void)x; }
#endif /* } UNUSED */

/*
 * The following macro can be used to define the MPE_LOG()
 * module that all MPE AWT logging messages are logged against.
 */
# define MPEAWT_LOG_MOD     MPE_MOD_JNI_AWT

/*
 * The following macros map semantic logging levels used within the AWT
 * implementation to actual MPE_LOG() logging levels.
 */
# define MPEAWT_LOGFAIL     MPE_LOG_ERROR
# define MPEAWT_LOGFAILUSER MPE_LOG_WARN
# define MPEAWT_LOGENTRY    MPE_LOG_TRACE1
# define MPEAWT_LOGEVENT    MPE_LOG_TRACE2
# define MPEAWT_LOGCONFIG   MPE_LOG_TRACE3
# define MPEAWT_LOGPIXELS   MPE_LOG_TRACE4
# define MPEAWT_LOGIMGDUMP  MPE_LOG_TRACE5
# define MPEAWT_LOGFONT     MPE_LOG_TRACE6
# define MPEAWT_LOGSYNC     MPE_LOG_TRACE7
# define MPEAWT_LOGTIME     MPE_LOG_TRACE8

/*
 * The following macros are used to control at compile-time some types
 * of logging.
 * These macros must be defined as non-zero for their respective logging
 * levels to be considered.
 * This is done to avoid unnecessary code being included when these
 * are definitely not wanted.
 * Once enabled at compile-time (via the following macros), the associated
 * MPE_LOG() levels may be used for further configuration.
 */
# define MPEAWT_DBGPIXELS   (0)
# define MPEAWT_DBGIMGDUMP  (0)
# define MPEAWT_DBGTIME     (0)

#if MPEAWT_DBGTIME
#define MPEAWT_TIME_INIT()  mpe_TimeMillis time_start, time_end;\
                             uint32_t time_elapsed;
# define MPEAWT_TIME_START() do { mpe_timeGetMillis(&time_start); } while(0)
# define MPEAWT_TIME_END()   do { mpe_timeGetMillis(&time_end);  time_elapsed = (uint32_t)(time_end-time_start); } while(0)
#else
#define MPEAWT_TIME_INIT()   {;}
# define MPEAWT_TIME_START() {;}
# define MPEAWT_TIME_END()   {;}
#endif

/* #define MPEAWT_JNI_STANDARD */
#if defined(MPEAWT_JNI_STANDARD)
# define MPEAWT_JNI_PREFIX Java_
# define JNI_START_BLOCKING(env) do {} while(0)
# define JNI_STOP_BLOCKING(env)  do {} while(0)
#else
/*
 * When native methods are implemented with an Evm_ prefix instead 
 * of a Java_ prefix, that is instruction to the Jeode EVM that
 * such code should be considered snareable.
 * Essentially, it results in faster invocation, by reducing the overhead
 * of such calls.
 * It should only be used for code that is already fast, and definitely
 * not code that "blocks".
 * For more info see: docs/Suite/develop/jni-optimize.htm.
 */
# define MPEAWT_JNI_PREFIX Java_
# define JNI_START_BLOCKING(env) JNI_beSnareableEnd(env)
# define JNI_STOP_BLOCKING(env)  JNI_beSnareableStart(env)
#endif

#define PREFIX_NAME_CAT(p,x) p ## x
#define PREFIX_NAME(p,x) PREFIX_NAME_CAT(p,x)
#define JNI_NAME(x) PREFIX_NAME(MPEAWT_JNI_PREFIX, x)
#define xJNI_NAME(x) PREFIX_NAME(Java_, x)
#define PREFIX_NAME_STR_CAT(p,x) #p #x
#define PREFIX_NAME_STR(p,x) PREFIX_NAME_STR_CAT(p,x)
#define JNI_NAME_STR(x) PREFIX_NAME_STR(PREFIX,x)

#endif /*} MPE_COMMON_H_INCLUDED */

