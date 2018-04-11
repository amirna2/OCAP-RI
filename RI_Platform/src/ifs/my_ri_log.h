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

#ifndef _RI_LOG_H
#define _RI_LOG_H "$Rev: 141 $"

//#define DEBUG_ERROR_LOGS

#include <stdio.h>

#ifndef llong
#define llong long long
#endif

typedef unsigned char uint8_t;
typedef unsigned short uint16_t;
typedef unsigned long uint24_t;
typedef unsigned long uint32_t;
typedef unsigned llong uint64_t;

#define log4c_category_t void

//LOG4C_API log4c_category_t * log4c_category_get(const char* a_name);
#define log4c_category_get(a_name) NULL

#define RILOG_FATAL(code, format, ...) \
    printf((format), ## __VA_ARGS__), exit(code)

#ifdef DEBUG_ERROR_LOGS
#define RILOG_ERROR(format, ...) \
    printf((format), ## __VA_ARGS__)
#define RILOG_CRIT(format, ...) \
    printf((format), ## __VA_ARGS__)
#define RILOG_WARN(format, ...) \
    printf((format), ## __VA_ARGS__)
#else
#define RILOG_ERROR(format, ...)
#define RILOG_CRIT(format, ...)
#define RILOG_WARN(format, ...)
#endif

#define RILOG_NOTICE(format, ...) \
    printf((format), ## __VA_ARGS__)

#define RILOG_INFO(format, ...) \
    printf((format), ## __VA_ARGS__)

#ifdef DEBUG_PAT_AND_PMT
#define RILOG_DEBUG(format, ...) \
    printf((format), ## __VA_ARGS__)
#else
#define RILOG_DEBUG(format, ...)
#endif

#define RILOG_TRACE(format, ...) \
    printf((format), ## __VA_ARGS__)

#endif
