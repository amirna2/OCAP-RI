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

#ifndef _ATE_INTERFACE_H_
#define _ATE_INTERFACE_H_

#include <glib.h>
#include <gmodule.h>
#include <ri_types.h>

#include "sectionutils.h"

#ifndef RI_PLATFORM_LOG

extern char *ate_if_log_formatter(char *, char *, char *, char *);

// Base logging call with priority (uses RILOG_CATEGORY macro)
#define RI_PLATFORM_LOG(priority, format, ...) \
{ \
   char _buf[RCVBUFSIZE+40], _msg[RCVBUFSIZE]; \
   (void)snprintf(_msg, RCVBUFSIZE-1, format, ## __VA_ARGS__); \
   fprintf(stderr,"%s", ate_if_log_formatter(_buf, priority, "ATE.IF", _msg)); \
}

// Logging calls that encapsulate a priority using the RILOG_CATEGORY
// macro.  Calls listed below are from most critical to least critical
// logging priorities
#define RILOG_FATAL(code, format, ...) \
    RI_PLATFORM_LOG("FATAL", (format), ## __VA_ARGS__), exit(code)

#define RILOG_CRIT(format, ...) \
    RI_PLATFORM_LOG("CRITICAL", (format), ## __VA_ARGS__)

#define RILOG_ERROR(format, ...) \
    RI_PLATFORM_LOG("ERROR", (format), ## __VA_ARGS__)

#define RILOG_WARN(format, ...) \
    RI_PLATFORM_LOG("WARN", (format), ## __VA_ARGS__)

#define RILOG_INFO(format, ...) \
    RI_PLATFORM_LOG("INFO", (format), ## __VA_ARGS__)

#define RILOG_DEBUG(format, ...) \
    RI_PLATFORM_LOG("DEBUG", (format), ## __VA_ARGS__)

#endif

// uS Time to wait for during telnet connections.
#define TELNET_DEFAULT_WAIT 50000
// uS Time to wait for the response.
#define TELNET_DEFAULT_DELAY 500000

#define TELNET_CONTROL 1
#define BACKLOG 3  // how many pending connections queue will hold
#define BOOT_DELAY_SEC 5
#define OPT_LEN 6

//String for menu prompt
#define MENU_TABLE \
	"\r\n" \
	"\r\n" \
	"Internet Power Switch v1.01    Site ID: DRI Device\r\n" \
	"\r\n" \
	"|Plug | Name             | Status  | Boot Delay\r\n"   \
	"|-----+------------------+---------+------------+\r\n" \
	"| 1   | Tuner_1          |   ON    |   5  sec   |\r\n" \
	"| 2   | Tuner_2          |   ON    |   5  sec   |\r\n" \
	"| 3   | Tuner_3          |   ON    |   5  sec   |\r\n" \
	"| 4   |                  |   OFF   |   0  sec   |\r\n" \
	"| 5   |                  |   OFF   |   0  sec   |\r\n" \
	"|-----+------------------+---------+------------+\r\n" \
	"\"/H\" for help.\r\n"

//Help text
#define HELP_STRING \
	"\r\n" \
	"Command   Option\r\n" \
	"----------------------\r\n" \
	"/H        this help\r\n" \
	"/x        exit\r\n" \
	"/BOOT n   reboot plug n\r\n"

//Boot reply text, all %d's are tuner number
#define BOOT_REPLY \
	"/BOOT %d\r\n" \
	"Plugs to be booted:\r\n" \
	"Plug %d: Tuner_%d\r\n" \
	"\r\n" \
	"Processing - please wait ... "

extern void ate_InterfaceInit(char *srvrIp, int srvrPort);
extern void ate_InterfaceAbort(void);
gpointer ate_TelnetThread(gpointer data);
ri_bool ate_ExchangeTelnetOptions(int sock);
void ate_SendMenu(int sock);
void ate_SendHelp(int sock);
void ate_ProcessBoot(int tuner);
ri_bool ate_BootReply(int sock, int tuner);
ri_bool ate_ProcessXait(char *msg);
ri_bool ate_XaitReply(int sock, ri_bool result);
ri_bool ate_ProcessTestName(char *msg);
ri_bool ate_TestNameReply(int sock, ri_bool result);
ri_bool ate_ProcessTspFile(char *msg, int tuner);
ri_bool ate_TspFileReply(int sock, ri_bool result);
char *ate_GetTspFileURL(void);

#endif

