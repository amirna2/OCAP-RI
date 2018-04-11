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

#include <glib.h>

#include <ri_log.h>
#include <signal.h>
#include <test_interface.h>

#include "gst_utils.h"
#include "pipeline_manager.h"
#include "ui_manager.h"
#include "frontpanel.h"
#include "backpanel.h"
#include "platform.h"
#include "ifs/IfsIntf.h"
#include "cablecard.h"
#include "snmp/agent.h"
#include <stare.h>

// Logging category
#define RILOG_CATEGORY riPlatformLogCat
log4c_category_t* riPlatformLogCat = NULL;

#define PLATFORM_TESTS \
    "\r\n" \
    "|---+-----------------------\r\n" \
    "| b | destroy Backpanel     \r\n" \
    "|---+-----------------------\r\n" \
    "| d | Dump memory info      \r\n" \
    "|---+-----------------------\r\n" \
    "| f | destroy Frontpanel    \r\n" \
    "|---+-----------------------\r\n" \
    "| l | Log text              \r\n" \
    "|---+-----------------------\r\n" \
    "| m | destroy ui Manager    \r\n" \
    "|---+-----------------------\r\n" \
    "| p | destroy Pipeline mgr  \r\n" \
    "|---+-----------------------\r\n" \
    "| t | Tuner tests           \r\n" \


G_MODULE_EXPORT void ri_platform_term(void);

static int testInputHandler(int sock, char *rxBuf, int *retCode, char **retStr)
{
    char buf[1024];
    RILOG_TRACE("%s -- Entry, received: %s\n", __FUNCTION__, rxBuf);
    *retCode = MENU_SUCCESS;

    if (strstr(rxBuf, "b"))
    {
        test_SendString(sock, "\r\n\ndestroy backpanel...\r\n");
        destroy_backpanel(get_backpanel());
        return 0;
    }
    else if (strstr(rxBuf, "d"))
    {
        test_SendString(sock, "\r\n\nMemory Information:\r\n");
        g_mem_profile();
        return 0;
    }
    else if (strstr(rxBuf, "f"))
    {
        test_SendString(sock, "\r\n\ndestroy frontpanel...\r\n");
        destroy_frontpanel( get_frontpanel());
        return 0;
    }
    else if (strstr(rxBuf, "l"))
    {
        if (test_GetString(sock, buf, sizeof(buf), "\r\n\nlogging: "))
        {
            RILOG_INFO("%s -- REMOTE LOG: %s\n", __FUNCTION__, buf);
        }
        else
        {
            RILOG_ERROR("%s -- test_GetString failure?!\n", __FUNCTION__);
            *retCode = MENU_FAILURE;
        }
        return 0;
    }
    else if (strstr(rxBuf, "m"))
    {
        test_SendString(sock, "\r\n\ndestroy ui manager...\r\n");
        destroy_ui_manager();
        return 0;
    }
    else if (strstr(rxBuf, "p"))
    {
        test_SendString(sock, "\r\n\ndestroy pipeline manager...\r\n");
        destroy_pipeline_manager();
        return 0;
    }
    else if (strstr(rxBuf, "t"))
    {
        test_SendString(sock, "\r\n\nTuner tests...\r\n");

        if (!test_SetNextMenu(sock, test_FindMenu("TunerTests")))
        {
            RILOG_ERROR("%s TunerTests sub-menu failed?\n", __FUNCTION__);
            *retCode = MENU_FAILURE;
            return 0;
        }
        else
        {
            return 1;
        }
    }
    else if (strstr(rxBuf, "x"))
    {
        RILOG_TRACE("%s -- Exit", __FUNCTION__);
        return -1;
    }
    else
    {
        strcat(rxBuf, " - unrecognized\r\n\n");
        test_SendString(sock, rxBuf);

        RILOG_TRACE("%s -- %s\n", __FUNCTION__, rxBuf);
        *retCode = MENU_INVALID;
        return 0;
    }
}

static MenuItem PlatformMenuItem =
{ TRUE, "p", "Platform", PLATFORM_TESTS, testInputHandler };

// Configuration list
int g_configMaxArgs = 32;

// GLib MainLoop
GMainLoop* g_glib_main_loop;

void sigHandler(int sig)
{
    RILOG_INFO("%s -- Received signal: %d, RI platform shutting down\n",
            __FUNCTION__, sig);

    platform_reset();

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

RI_MODULE_EXPORT void ri_platform_init(int argc, char** argv)
{
    char* ipAddr;
    char* proto;
    char* cfgValStr = NULL;
    int port;
    int agentXport;
    ri_bool isSnmpSubAgent = FALSE;

    /**
     * instantiate the logger and if successful, setup the gst logging...
     */
    if (initLogger("RI"))
    {
        fprintf(stderr, "%s -- initLogger failure?!\n", __func__);
    }

    // Get our logging category
    riPlatformLogCat = log4c_category_get("RI.Platform");

    RILOG_INFO("%s -- Starting up RI platform\n", __FUNCTION__);

    // Load our platform configuration file
    if (argc < 2 || argv[1] == NULL || ricfg_parseConfigFile("RIPlatform",
            argv[1]) != RICONFIG_SUCCESS)
    {
        RILOG_FATAL(-1, "%s: Failed to load platform config file!\n",
                __FUNCTION__);
    }

    (void) signal(SIGINT, sigHandler);
    (void) signal(SIGABRT, sigHandler);
    (void) signal(SIGTERM, sigHandler);
    RILOG_DEBUG("%s -- assigned signals\n", __FUNCTION__);

    /**
     * ensure that the glib threading system is up...
     */
    if (!g_thread_supported())
    {
        g_thread_init(NULL);
    }

    // Set GLib/GStreamer logging functions
    (void) g_set_print_handler((GPrintFunc) rilog_info_printf);
    (void) g_set_printerr_handler((GPrintFunc) rilog_error_printf);

    // Get the RI Platform IP address...
    if (NULL == (ipAddr = ricfg_getValue("RIPlatform", "RI.Platform.IpAddr")))
    {
        ipAddr = "127.0.0.1";
        RILOG_WARN("%s -- RI Platform IP address not specified!\n",
                __FUNCTION__);
    }

    // Get the RI Platform Test Interface IP port...
    if (NULL == (cfgValStr =
                 ricfg_getValue("RIPlatform", "RI.Platform.TestIfPort")))
    {
        port = 23000;
        RILOG_WARN("%s -- Test Interface IP port not specified!\n",
                __FUNCTION__);
    }
    else
    {
        port = atoi(cfgValStr);
    }

    test_InterfaceInit(ipAddr, port);
  
    // NOTE: uncomment the following to turn on the STARE receiver.  I have left it
    // commented out for now since it is not finished
//    initEYEsReceiver("192.168.0.8" /* pSvrIP */, 10001 /* nSvrPort */, 0 /* uProtocolVersion */,  "anon" /* pUser */);

    test_RegisterMenu(&PlatformMenuItem);

    // Get the RI Platform SNMP Master Agent flag
    if (NULL == (cfgValStr =
                 ricfg_getValue("RIPlatform", "RI.Platform.SnmpIsSubAgent")))
    {
        isSnmpSubAgent = FALSE;
        RILOG_WARN("%s SNMP Sub-Agent flag not specified! default=FALSE\n",
                    __FUNCTION__);
    }
    else if (0 == strcasecmp(cfgValStr, "true"))
    {
        isSnmpSubAgent = TRUE;
    }

    // Get the RI Platform SNMP IP port...
    if (NULL == (cfgValStr =
                 ricfg_getValue("RIPlatform", "RI.Platform.SnmpPort")))
    {
        port = 10161;
        RILOG_WARN("%s SNMP IP port not specified! default=10161\n",
                    __FUNCTION__);
    }
    else
    {
        port = atoi(cfgValStr);
    }

    // Get the RI Platform SNMP IP agentXport...
    if (NULL == (cfgValStr =
                 ricfg_getValue("RIPlatform", "RI.Platform.SnmpAgentXPort")))
    {
        agentXport = 10705;
        RILOG_WARN("%s SNMP IP agentXport not specified! default=10705\n",
                    __FUNCTION__);
    }
    else
    {
        agentXport = atoi(cfgValStr);
    }

    if (NULL == (proto = ricfg_getValue("RIPlatform", "RI.Platform.SnmpProto")))
    {
        proto = "udp";
        RILOG_WARN("%s RI Platform SNMP Protocol not specified! default=udp\n",
                __FUNCTION__);
    }

    /**
     * instantiate the SNMP Agent(s)
     */
    snmp_InterfaceInit(proto, ipAddr, port, agentXport, isSnmpSubAgent);

    // Initialize our indexing file system (IFS) for DVR
    IfsInit();

    /**
     * Initialize GStreamer
     */
    gst_init_library();

    /**
     * initialize CableCARD / POD access
     */
    cablecard_init();

    /**
     * instantiate the pipeline...
     */
    create_pipeline_manager();

    /**
     * enable user interface and key events...
     */
    create_ui_manager();

    /**
     * enable front panel...
     */
    (void) create_frontpanel();

    /**
     * enable back panel...
     */
    (void) create_backpanel();

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

RI_MODULE_EXPORT void ri_platform_term(void)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    test_InterfaceAbort();

    // NOTE: uncomment the following to turn off the STARE receiver.  I have left it
    // commented out for now since it is not finished.
//    terminateEYEsReceiver();

    fprintf(stderr,
            "\nALERT: %s -- no longer performs a shutdown... Just exit(0)!\n",
            __func__);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

RI_MODULE_EXPORT void ri_platform_loop(void)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    g_glib_main_loop = g_main_loop_new(NULL, FALSE);
    g_main_loop_run(g_glib_main_loop);
    fprintf(stderr, "\nALERT: ri_platform_loop exiting...\n");

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

void platform_reset(void)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    if (NULL != g_glib_main_loop)
    {
        fprintf(stderr, "\nALERT: %s -- calling g_main_loop_quit()...\n",
                __func__);
        g_main_loop_quit(g_glib_main_loop);
    }
    else
    {
        fprintf(stderr, "\nALERT: %s -- no main loop to quit!\n", __func__);
        exit(0);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

