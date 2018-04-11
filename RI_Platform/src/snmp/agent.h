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

#ifndef _AGENT_H_
#define _AGENT_H_

#include <glib.h>
#include <gmodule.h>
#include <ri_types.h>

#define MASTER_AGENT    0
#define SUB_AGENT       1

#ifdef STANDALONE
#ifndef RI_PLATFORM_LOG

extern char *snmp_if_log_formatter(char *, char *, char *, char *);

// Base logging call with priority (uses RILOG_CATEGORY macro)
#define RI_PLATFORM_LOG(priority, format, ...) \
{ \
   char _buf[RCVBUFSIZE+40], _msg[RCVBUFSIZE]; \
   (void)snprintf(_msg, RCVBUFSIZE-1, format, ## __VA_ARGS__); \
   fprintf(stderr,"%s", snmp_if_log_formatter(_buf, priority,"RI.NET.SNMP.Agent",_msg)); \
   fflush(stderr); \
}

// Logging calls that encapsulate a priority using the RILOG_CATEGORY
// macro.  Calls listed below are from most critical to least critical
// logging priorities
#define RILOG_FATAL(code, format, ...) \
    RI_PLATFORM_LOG("FATAL", (format), ## __VA_ARGS__) exit(code)

#define RILOG_CRIT(format, ...) \
    RI_PLATFORM_LOG("CRITICAL", (format), ## __VA_ARGS__)

#define RILOG_ERROR(format, ...) \
    RI_PLATFORM_LOG("ERROR", (format), ## __VA_ARGS__)

#define RILOG_WARN(format, ...) \
    RI_PLATFORM_LOG("WARN", (format), ## __VA_ARGS__)

#define RILOG_NOTICE(format, ...) \
    RI_PLATFORM_LOG("NOTICE", (format), ## __VA_ARGS__)

#define RILOG_INFO(format, ...) \
    RI_PLATFORM_LOG("INFO", (format), ## __VA_ARGS__)

#define RILOG_DEBUG(format, ...) \
    RI_PLATFORM_LOG("DEBUG", (format), ## __VA_ARGS__)

#define RILOG_TRACE(format, ...) \
    RI_PLATFORM_LOG("TRACE", (format), ## __VA_ARGS__)

#endif
#endif // STANDALONE

extern void snmp_InterfaceInit(char *proto, char *ip, int port, int agentXport,
                               gboolean subagent);
extern void snmp_InterfaceAbort(void);
gpointer snmp_AgentThread(gpointer data);

#endif
