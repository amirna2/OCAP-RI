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

#include <ri_config.h>

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <sys/time.h>

#include "agent.h"

char *snmp_if_log_formatter(char *buf, char *priority, char *category, char *msg)
{
#ifndef _WIN32
    struct timeval timestamp;
    struct tm tm;

    gettimeofday(&timestamp, NULL);
    gmtime_r(&timestamp.tv_sec, &tm);
    if (0 > snprintf(buf, 1024,
            "%04d%02d%02d %02d:%02d:%02d.%03ld %-8s %s- %s", tm.tm_year + 1900,
            tm.tm_mon + 1, tm.tm_mday, tm.tm_hour, tm.tm_min, tm.tm_sec,
            timestamp.tv_usec / 1000, priority, category, msg))
    {
        fprintf(stderr, "%s snprintf failure?!\n", __func__);
    }
#else
    SYSTEMTIME stime;

    GetLocalTime(&stime);
    if(0 > snprintf(buf, 1024, "%04d%02d%02d %02d:%02d:%02d.%03d %-8s %s- %s",
                    stime.wYear, stime.wMonth , stime.wDay,
                    stime.wHour, stime.wMinute, stime.wSecond,
                    stime.wMilliseconds,
                    priority, category, msg))
    {
        fprintf(stderr, "%s snprintf failure?!\n", __func__);
    }
#endif
    return buf;
}

void usage()
{
    printf("SNMP Interface Launcher.  Usage:\n");
    printf("\tplatformAgent <path to configuration file>\n");
}

int main(int argc, char** argv)
{
    char *ipAddr;
    char *proto;
    char *port;
    char *agentXport;

    // Retrieve our configuration file name
    if (argc < 2)
    {
        usage();
        return 1;
    }

    // ensure that the glib threading system is up...
    if (!g_thread_supported())
    {
        g_thread_init(NULL);
    }

    // Load and parse our config file
    if (ricfg_parseConfigFile("RIPlatform", argv[1]) != RICONFIG_SUCCESS)
    {
        printf("** ERROR!  Could not parse platform configuration file! **\n");
        printf("%s\n", argv[1]);
        return 1;
    }

    // Get the RI Platform IP address the SNMP should talk to...
    if (NULL == (ipAddr = ricfg_getValue("RIPlatform", "RI.Platform.IpAddr")))
    {
        ipAddr = "127.0.0.1";
        fprintf(stderr, "%s WARNING: IP address not specified!\n", __func__);
    }

    if (NULL == (port = ricfg_getValue("RIPlatform", "RI.Platform.SnmpPort")))
    {
        port = "161";
        fprintf(stderr, "%s WARNING: SNMP IP port not specified!\n", __func__);
    }

    if (NULL == (agentXport = ricfg_getValue("RIPlatform", "RI.Platform.SnmpAgentXPort")))
    {
        agentXport = "705";
        fprintf(stderr, "%s WARNING: SNMP IP agentXport not specified!\n", __func__);
    }

    if (NULL == (proto = ricfg_getValue("RIPlatform", "RI.Platform.SnmpProto")))
    {
        proto = "udp";
        fprintf(stderr, "%s WARNING: SNMP Protocol not specified!\n", __func__);
    }

    /**
     * instantiate the SNMP Agent(s)
     */
    snmp_InterfaceInit(proto, ipAddr, atoi(port), atoi(agentXport), false);
    return 0;
}

