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

#ifndef LOGGER_H
#define LOGGER_H

#include <ctype.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include <log4c.h>

#include <ri_types.h>

extern log4c_category_t* defaultCategory;
extern log4c_category_t* glibCategory;

// Base logging call with priority (uses RILOG_CATEGORY macro)
#if 1   // invert this logic to test log format correctness
#define RI_PLATFORM_LOG(priority, format, ...) \
    log4c_category_log(RILOG_CATEGORY, (priority), (format), ## __VA_ARGS__)
#else
#define RI_PLATFORM_LOG(priority, format, ...) \
    fprintf(stderr, (format), ## __VA_ARGS__) 
#endif

// Logging calls that encapsulate a priority using the RILOG_CATEGORY
// macro.  Calls listed below are from most critical to least critical
// logging priorities
#define RILOG_FATAL(code, format, ...) \
    RI_PLATFORM_LOG(LOG4C_PRIORITY_FATAL, (format), ## __VA_ARGS__), exit(code)

#define RILOG_CRIT(format, ...) \
    RI_PLATFORM_LOG(LOG4C_PRIORITY_CRIT, (format), ## __VA_ARGS__)

#define RILOG_ERROR(format, ...) \
    RI_PLATFORM_LOG(LOG4C_PRIORITY_ERROR, (format), ## __VA_ARGS__)

#define RILOG_WARN(format, ...) \
    RI_PLATFORM_LOG(LOG4C_PRIORITY_WARN, (format), ## __VA_ARGS__)

#define RILOG_NOTICE(format, ...) \
    RI_PLATFORM_LOG(LOG4C_PRIORITY_NOTICE, (format), ## __VA_ARGS__)

#define RILOG_INFO(format, ...) \
    RI_PLATFORM_LOG(LOG4C_PRIORITY_INFO, (format), ## __VA_ARGS__)

#undef PRODUCTION_BUILD
#ifndef PRODUCTION_BUILD
#define RILOG_DEBUG(format, ...) \
    RI_PLATFORM_LOG(LOG4C_PRIORITY_DEBUG, (format), ## __VA_ARGS__)

#define RILOG_TRACE(format, ...) \
    RI_PLATFORM_LOG(LOG4C_PRIORITY_TRACE, (format), ## __VA_ARGS__)
#else
#define RILOG_DEBUG(format, ...) 
#define RILOG_TRACE(format, ...)
#endif
// Logging calls that encapsulate a priority.  Calls listed below are
// from most critical to least critical logging priorities
#define RILOG_FATAL_C(category, format, ...)                             \
    log4c_category_log((category), LOG4C_PRIORITY_FATAL, (format), ## __VA_ARGS__)

#define RILOG_CRIT_C(category, format, ...)                              \
    log4c_category_log((category), LOG4C_PRIORITY_CRIT, (format), ## __VA_ARGS__)

#define RILOG_ERROR_C(category, format, ...) \
    log4c_category_log((category), LOG4C_PRIORITY_ERROR, (format), ## __VA_ARGS__)

#define RILOG_WARN_C(category, format, ...) \
    log4c_category_log((category), LOG4C_PRIORITY_WARN, (format), ## __VA_ARGS__)

#define RILOG_NOTICE_C(category, format, ...) \
    log4c_category_log((category), LOG4C_PRIORITY_NOTICE, (format), ## __VA_ARGS__)

#define RILOG_INFO_C(category, format, ...) \
    log4c_category_log((category), LOG4C_PRIORITY_INFO, (format), ## __VA_ARGS__)

#define RILOG_DEBUG_C(category, format, ...) \
    log4c_category_log((category), LOG4C_PRIORITY_DEBUG, (format), ## __VA_ARGS__)

#define RILOG_TRACE_C(category, format, ...) \
    log4c_category_log((category), LOG4C_PRIORITY_TRACE, (format), ## __VA_ARGS__)

// Special logging macros for glib/gstreamer, since we can only register
// 2 debug functions with glib
#define RILOG_GLIB_DEBUG(format, ...) \
    log4c_category_log(glibCategory, LOG4C_PRIORITY_DEBUG, (format), ## __VA_ARGS__)

#define RILOG_GLIB_ERROR(format, ...) \
    log4c_category_log(glibCategory, LOG4C_PRIORITY_ERROR, (format), ## __VA_ARGS__)

// Base logging call with priority and var args list (uses RILOG_CATEGORY macro)
#define RI_PLATFORM_LOGV(priority, format, valist)                       \
    log4c_category_vlog(RILOG_CATEGORY, (priority), (format), (valist))

// Same as above, but with va_list arg instead of elipsis
#define RILOGV_FATAL(format, valist)                               \
    RI_PLATFORM_LOGV(LOG4C_PRIORITY_FATAL, (format), (valist))

#define RILOGV_CRIT(format, valist)                                 \
    RI_PLATFORM_LOGV(LOG4C_PRIORITY_CRIT, (format), (valist))

#define RILOGV_ERROR(format, valist) \
    RI_PLATFORM_LOGV(LOG4C_PRIORITY_ERROR, (format), (valist))

#define RILOGV_WARN(format, valist) \
    RI_PLATFORM_LOGV(LOG4C_PRIORITY_WARN, (format), (valist))

#define RILOGV_NOTICE(format, valist) \
    RI_PLATFORM_LOGV(LOG4C_PRIORITY_NOTICE, (format), (valist))

#define RILOGV_INFO(format, valist) \
    RI_PLATFORM_LOGV(LOG4C_PRIORITY_INFO, (format), (valist))

#define RILOGV_DEBUG(format, valist) \
    RI_PLATFORM_LOGV(LOG4C_PRIORITY_DEBUG, (format), (valist))

#define RILOGV_TRACE(format, valist) \
    RI_PLATFORM_LOGV(LOG4C_PRIORITY_TRACE, (format), (valist))

// Same as above, but with va_list arg instead of elipsis
#define RILOGV_FATAL_C(category, format, valist)                             \
    log4c_category_vlog((category), LOG4C_PRIORITY_FATAL, (format), (valist))

#define RILOGV_CRIT_C(category, format, valist)                              \
    log4c_category_vlog((category), LOG4C_PRIORITY_CRIT, (format), (valist))

#define RILOGV_ERROR_C(category, format, valist) \
    log4c_category_vlog((category), LOG4C_PRIORITY_ERROR, (format), (valist))

#define RILOGV_WARN_C(category, format, valist) \
    log4c_category_vlog((category), LOG4C_PRIORITY_WARN, (format), (valist))

#define RILOGV_NOTICE_C(category, format, valist) \
    log4c_category_vlog((category), LOG4C_PRIORITY_NOTICE, (format), (valist))

#define RILOGV_INFO_C(category, format, valist) \
    log4c_category_vlog((category), LOG4C_PRIORITY_INFO, (format), (valist))

#define RILOGV_DEBUG_C(category, format, valist) \
    log4c_category_vlog((category), LOG4C_PRIORITY_DEBUG, (format), (valist))

#define RILOGV_TRACE_C(category, format, valist) \
    log4c_category_vlog((category), LOG4C_PRIORITY_TRACE, (format), (valist))

/**
 * Instantiate the logger...
 * @param cat The default category string for the logging system
 * @return 0 if successful, -1 otherwise
 */
int initLogger(char* category);

/**
 * Cleanup the logger instantiation;
 * @return 0 if successful, -1 otherwise
 */
int freeLogger();

/**
 * Output INFO log messages on behalf of the glib lib
 * @param format Format string for the actual log message
 * @param ... Possible parameters to the format string
 */
void rilog_info_printf(const char *string);

/**
 * Output ERROR log messages on behalf of the glib lib
 * @param format Format string for the actual log message
 * @param ... Possible parameters to the format string
 */
void rilog_error_printf(const char *string);

/**
 *   ri_errorToString: method called to return a string representation of a
 *                     ri_error enumeration for logging, printing, etc.
 * @param riErrorCode: the ri_error code to convert to a string
 *           @returns: the resultant string
 */
char *ri_errorToString(ri_error riErrorCode);

void hex_dump(void *data, int size);

#endif

