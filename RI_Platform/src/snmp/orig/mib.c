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
#define RILOG_CATEGORY snmpMib_RILogCategory
log4c_category_t *snmpMib_RILogCategory = NULL;

#define CHECK_LOGGER() \
{ \
   if(NULL == snmpMib_RILogCategory) \
   snmpMib_RILogCategory = log4c_category_get("RI.NET.SNMP.MIB"); \
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

oid sysDescr[] = {1, 3, 6, 1, 2, 1, 1, 1};
oid sysUptime[] = {1, 3, 6, 1, 2, 1, 1, 3, 0};  // ulong reg needs .0 ???
oid sysContact[] = {1, 3, 6, 1, 2, 1, 1, 4};
oid sysName[] = {1, 3, 6, 1, 2, 1, 1, 5};
oid sysLocation[] = {1, 3, 6, 1, 2, 1, 1, 6};

typedef struct oidPair
{
    oid *mOid;
    union
    {
        char *string;
        unsigned long ulong;
    } mVal;
} OidPair;

// contain all the MIB data the Agent requires:
static struct Mib
{
    OidPair sysDescr;
    OidPair sysUptime;
    OidPair sysContact;
    OidPair sysName;
    OidPair sysLocation;
} mib;

void snmp_SystemMibInit(void)
{
    CHECK_LOGGER();
    RILOG_INFO("%s();\n", __func__);

    mib.sysDescr.mOid = g_malloc0(sizeof(sysDescr));
    memcpy(mib.sysDescr.mOid, sysDescr, sizeof(sysDescr));
    mib.sysDescr.mVal.string = "OCAP-RI";
    mib.sysUptime.mOid = g_malloc0(sizeof(sysUptime));
    memcpy(mib.sysUptime.mOid, sysUptime, sizeof(sysUptime));
    mib.sysUptime.mVal.ulong = 0x12345678;
    mib.sysContact.mOid = g_malloc0(sizeof(sysContact));
    memcpy(mib.sysContact.mOid, sysContact, sizeof(sysContact));
    mib.sysContact.mVal.string = "ocap-ri@cablelabs.com";
    mib.sysName.mOid = g_malloc0(sizeof(sysName));
    memcpy(mib.sysName.mOid, sysName, sizeof(sysName));
    mib.sysName.mVal.string = "jupiter";
    mib.sysLocation.mOid = g_malloc0(sizeof(sysLocation));
    memcpy(mib.sysLocation.mOid, sysLocation, sizeof(sysLocation));
    mib.sysLocation.mVal.string = "Louisville, CO";

    netsnmp_handler_registration *reg = NULL;
    netsnmp_watcher_info *winfo = NULL;

    reg = netsnmp_create_handler_registration("sysDescr", NULL,
                                              mib.sysDescr.mOid,
                                              OID_LENGTH(sysDescr),
                                              HANDLER_CAN_RONLY);
    winfo = netsnmp_create_watcher_info(mib.sysDescr.mVal.string,
                                        strlen(mib.sysDescr.mVal.string),
                                        ASN_OCTET_STR,
                                        WATCHER_FIXED_SIZE);
    netsnmp_register_watched_scalar(reg, winfo);

    netsnmp_register_read_only_ulong_instance("sysUptime",
                                              mib.sysUptime.mOid,
                                              OID_LENGTH(sysUptime),
                                              &mib.sysUptime.mVal.ulong, NULL);

    reg = netsnmp_create_handler_registration("sysContact", NULL,
                                              mib.sysContact.mOid,
                                              OID_LENGTH(sysContact),
                                              HANDLER_CAN_RONLY);
    winfo = netsnmp_create_watcher_info(mib.sysContact.mVal.string,
                                        strlen(mib.sysContact.mVal.string),
                                        ASN_OCTET_STR,
                                        WATCHER_FIXED_SIZE);
    netsnmp_register_watched_scalar(reg, winfo);

    reg = netsnmp_create_handler_registration("sysName", NULL,
                                              mib.sysName.mOid,
                                              OID_LENGTH(sysName),
                                              HANDLER_CAN_RONLY);
    winfo = netsnmp_create_watcher_info(mib.sysName.mVal.string,
                                        strlen(mib.sysName.mVal.string),
                                        ASN_OCTET_STR,
                                        WATCHER_FIXED_SIZE);
    netsnmp_register_watched_scalar(reg, winfo);

    reg = netsnmp_create_handler_registration("sysLocation", NULL,
                                              mib.sysLocation.mOid,
                                              OID_LENGTH(sysLocation),
                                              HANDLER_CAN_RONLY);
    winfo = netsnmp_create_watcher_info(mib.sysLocation.mVal.string,
                                        strlen(mib.sysLocation.mVal.string),
                                        ASN_OCTET_STR,
                                        WATCHER_FIXED_SIZE);
    netsnmp_register_watched_scalar(reg, winfo);

    RILOG_INFO("%s exiting...\n", __func__);
}


void snmp_MibInit(void)
{
    CHECK_LOGGER();
    RILOG_INFO("%s();\n", __func__);

    snmp_SystemMibInit();

    RILOG_INFO("%s exiting...\n", __func__);
}

