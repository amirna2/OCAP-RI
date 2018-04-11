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

#ifndef STANDALONE
#include <ri_log.h>
#define RILOG_CATEGORY snmp_RILogCategory
log4c_category_t *snmp_RILogCategory = NULL;


#define CHECK_LOGGER() \
{ \
   if(NULL == snmp_RILogCategory) \
   snmp_RILogCategory = log4c_category_get("RI.NET.SNMP"); \
}

#else
#define CHECK_LOGGER()
#endif

#include <ri_config.h>

// log4c includes mingw32.h which defines _WIN32_WINNT and net-snmp
// includes windef.h which also defines _WIN32_WINNT undef here to compile!
#ifdef _WIN32_WINNT
#include <ws2tcpip.h>
#undef _WIN32_WINNT
#endif

#include <net-snmp/net-snmp-config.h>
#include <net-snmp/net-snmp-includes.h>
#include <net-snmp/agent/net-snmp-agent-includes.h>

#include "agent.h"

#define CHECK_SHUTDOWN() (snmp.mShutdownInProgress)

extern void snmp_MibInit(void);

// contain all the module data snmp_Interface requires:
static struct Snmp
{
    char mProtocol[24];            // protocol: udp, tcp, udp6, or tcp6
    char mIpAddr[INET6_ADDRSTRLEN];// Local host interface to bind
    int mAgentXPort;               // sub-agent communications port
    int mPort;                     // master agent listener port
    GThread *agent;                // agent thread
    gboolean mShutdownInProgress;  // set when shutting down
    gboolean mSubagent;            // set to start as an AgentX subagent
    netsnmp_log_handler logger;
} snmp;

char *snmp_PriorityString(int priority)
{
    switch (priority)
    {
        case LOG_EMERG:
            return "LOG_EMERG";
        case LOG_ALERT:
            return "LOG_ALERT";
        case LOG_CRIT:
            return "LOG_CRIT";
        case LOG_ERR:
            return "LOG_ERR";
        case LOG_WARNING:
            return "LOG_WARNING";
        case LOG_NOTICE:
            return "LOG_NOTICE";
        case LOG_INFO:
            return "LOG_INFO";
        case LOG_DEBUG:
            return "LOG_DEBUG";
        default:
            RILOG_ERROR("%s: unknown priority %d\n", __func__, priority);
            break;
    }

    return "unknown?";
}

int snmp_Log(netsnmp_log_handler *handler, int priority, const char *message)
{
    char *lf = NULL;

    if (NULL != message && (lf = strchr(message, '\n')))
    {
        *lf = 0;
    }

    switch (priority)
    {
        case LOG_EMERG:
            RILOG_FATAL(-106, "agent: %s\n", message);
            break;
        case LOG_ALERT:
        case LOG_CRIT:
            RILOG_CRIT("agent: %s\n", message);
            break;
        case LOG_ERR:
            RILOG_ERROR("agent: %s\n", message);
            break;
        case LOG_WARNING:
            RILOG_WARN("agent: %s\n", message);
            break;
        case LOG_NOTICE:
            RILOG_NOTICE("agent: %s\n", message);
            break;
        case LOG_INFO:
            RILOG_INFO("agent: %s\n", message);
            break;
        case LOG_DEBUG:
            RILOG_DEBUG("agent: %s\n", message);
            break;
        default:
            RILOG_ERROR("%s: unknown priority %d, %s\n",
                        __func__, priority, message);
            break;
    }

    return 1;
}

void snmp_InterfaceInit(char *proto, char *ip, int port, int agentXport,
                        gboolean subagent)
{
    CHECK_LOGGER();
    RILOG_INFO("%s(%s, %s, %d, %d, %s);\n", __func__, proto, ip, port,
               agentXport, boolStr(subagent));

    if (0 > snprintf(snmp.mProtocol, sizeof(snmp.mProtocol), "%s", proto))
    {
        RILOG_ERROR("%s snprintf failure?!\n", __func__);
    }

    if (0 > snprintf(snmp.mIpAddr, sizeof(snmp.mIpAddr), "%s", ip))
    {
        RILOG_ERROR("%s snprintf failure?!\n", __func__);
    }

    snmp.mPort = port;
    snmp.mAgentXPort = agentXport;
    snmp.mShutdownInProgress = FALSE;
    snmp.mSubagent = subagent;

#ifdef STANDALONE
    (void) snmp_AgentThread(0);
#else
    if (NULL == (snmp.agent = g_thread_create(snmp_AgentThread, 0, FALSE, 0)))
    {
        RILOG_ERROR("%s -- g_thread_create() returned NULL?!\n", __func__);
    }

    //g_usleep(600000);    // DEBUG! sleep waiting for agent to initialize...
#endif
}

/**
 * snmp_InterfaceAbort
 */
void snmp_InterfaceAbort(void)
{
    CHECK_LOGGER();
    snmp.mShutdownInProgress = TRUE;
    RILOG_WARN("%s\n", __func__);
}

extern ri_bool ocStbHostSystemLoggingAddTableRow(char *, char *, char *);

ri_bool SnmpAddLogEntry(char *oid, char *timeStamp, char *message)
{
    RILOG_DEBUG("%s(%s, %s, %s);\n", __func__, oid, timeStamp, message);
    return ocStbHostSystemLoggingAddTableRow(oid, timeStamp, message);
}

/**
 * snmp_AgentThread
 */
gpointer snmp_AgentThread(gpointer data)
{
    char *path = NULL;
    char address[INET6_ADDRSTRLEN];
    gboolean quit = FALSE;
    int priority = LOG_DEBUG;

    CHECK_LOGGER();
    RILOG_INFO("%s(%p); for %s:%d\n", __func__, data, snmp.mIpAddr, snmp.mPort);
    snmp_enable_calllog();     // enable Net-SNMP logging via RI_LOG
    snmp.logger.handler = snmp_Log;
    snmp.logger.enabled = 1;
    snmp.logger.priority = LOG_DEBUG+1;
    snmp.logger.type = NETSNMP_LOGHANDLER_CALLBACK;

    if (netsnmp_add_loghandler(&snmp.logger))
    {
        RILOG_INFO("%s - installed the CALLBACK log handler\n", __func__);
    }
    else
    {
        RILOG_ERROR("%s - Unable to install the log handler\n", __func__);
    }

    while (priority > 0)
    {
        snmp_log(priority, "%s - testing the snmp_log(%d, %s) handler\n",
                        __func__, priority, snmp_PriorityString(priority));
        priority--;
    }

    snmp_log(LOG_INFO, "%s - skiping the snmp_log(%d, %s) handler\n",
                    __func__, LOG_EMERG, snmp_PriorityString(LOG_EMERG));

#ifdef AGENT_ON_OUR_ADDR_ONLY
    snprintf(address, sizeof(address), "%s:%s:%d",
             snmp.mProtocol, snmp.mIpAddr, snmp.mPort);
#else
    snprintf(address, sizeof(address), "%s:%d", snmp.mProtocol, snmp.mPort);
#endif
    netsnmp_ds_set_string(NETSNMP_DS_APPLICATION_ID,
                          NETSNMP_DS_AGENT_PORTS, address);
    snprintf(address, sizeof(address), "tcp:localhost:%d", snmp.mAgentXPort);
    netsnmp_ds_set_string(NETSNMP_DS_APPLICATION_ID,
                          NETSNMP_DS_AGENT_X_SOCKET, address);
    path = ricfg_getValue("RIPlatform", "RI.Platform.SnmpCfgDir");

    if (NULL == path)
    {
        path = ".";
        RILOG_WARN("%s -- RI Platform SNMP config path not specified!\n",
                __FUNCTION__);
    }

    netsnmp_ds_set_string(NETSNMP_DS_LIBRARY_ID,
                          NETSNMP_DS_LIB_PERSISTENT_DIR, path);
    if (snmp.mSubagent)
    {
        // set-up agentx subagent
        RILOG_INFO("%s initializing agentx subagent...\n", __func__);
        netsnmp_ds_set_boolean(NETSNMP_DS_APPLICATION_ID,
                               NETSNMP_DS_AGENT_ROLE, SUB_AGENT);
        netsnmp_enable_subagent();
    }
    else
    {
        netsnmp_ds_set_boolean(NETSNMP_DS_APPLICATION_ID,
                               NETSNMP_DS_AGENT_ROLE, MASTER_AGENT);
        netsnmp_ds_set_boolean(NETSNMP_DS_APPLICATION_ID,
                               NETSNMP_DS_AGENT_AGENTX_MASTER, 1);
    }

    SOCK_STARTUP;
    init_agent("platformAgent");

    // initialize mib code here...
    snmp_MibInit();

    init_snmp("platformAgent");

    if (!snmp.mSubagent)
    {
        RILOG_INFO("%s initializing Master Agent...\n", __func__);
        init_master_agent();
    }

    while (quit == FALSE)
    {
        if (CHECK_SHUTDOWN())
        {
            RILOG_ERROR("%s shutting down\n", __func__);
            break;
        }

        RILOG_DEBUG("%s waiting for a connection...\n", __func__);
        agent_check_and_process(1);
        RILOG_DEBUG("%s processing complete.\n", __func__);
    }

    snmp_shutdown("platformAgent");
    SOCK_CLEANUP;
    RILOG_INFO("%s exiting...\n", __func__);
    return NULL;
}

