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


#include <inttypes.h>
#include <ri_log.h>
#include <DumpApdu.h>
#include <glib.h>

#include "sas.h"
#include "sectionutils.h"

// Logging category
log4c_category_t* sas_RILogCategory = NULL;
#define RILOG_CATEGORY sas_RILogCategory

struct Sas
{
    ri_bool Enabled;
    Tunnel SasTunnel[MAX_CONNECTIONS];
    GAsyncQueue *SasEventQ;
} sas;

ri_bool getNextTunnelIndex(uint64_t appId, uint32_t *tunnelIndex)
{
    int i;

    for (i = 0; (NULL != tunnelIndex) && (i < MAX_CONNECTIONS); i++)
    {
        if (sas.SasTunnel[i].PrivateAppId == 0)
        {
            sas.SasTunnel[i].PrivateAppId = appId;
            *tunnelIndex = i;
            return TRUE;
        }
    }

    return FALSE;
}

ri_bool findTunnelByAppId(uint64_t appId, uint32_t *tunnelIndex)
{
    int i;

    for (i = 0; (NULL != tunnelIndex) && (i < MAX_CONNECTIONS); i++)
    {
        if (sas.SasTunnel[i].PrivateAppId == appId)
        {
            *tunnelIndex = i;
            return TRUE;
        }
    }

    return FALSE;
}

ri_bool findTunnelBySessionNb(uint32_t sessionNb, uint32_t *tunnelIndex)
{
    int i;

    for (i = 0; (NULL != tunnelIndex) && (i < MAX_CONNECTIONS); i++)
    {
        if (sas.SasTunnel[i].SessionNb == sessionNb)
        {
            *tunnelIndex = i;
            return TRUE;
        }
    }

    return FALSE;
}

ri_bool getNextEvent(uint32_t *type, uint32_t *sess, uint8_t *data, uint32_t *len)
{
    GTimeVal timeout;

    if (NULL == type || NULL == sess || NULL == data || NULL == len)
    {
        RILOG_ERROR("%s -- bad input arg(s)!\n", __FUNCTION__);
    }
    else
    {
        g_get_current_time(&timeout);
        g_time_val_add(&timeout, 100000);
        Event *sasEvent = g_async_queue_timed_pop(sas.SasEventQ, &timeout);

        if (NULL != sasEvent)
        {
            *type = sasEvent->EventType;
            *sess = sasEvent->SessionNb;

            if (0 != (*len = sasEvent->DataLen))
            {
                memcpy(data, sasEvent->Data, sasEvent->DataLen);
            }

            g_free(sasEvent);
            return TRUE;
        }
    }

    return FALSE;
}

ri_bool SasInit()
{
    // Create our logging category
    sas_RILogCategory = log4c_category_get("RI.SAS");
    RILOG_DEBUG("%s -- Entry\n", __FUNCTION__);

    // initialize our SAS module data...
    memset(&sas, 0, sizeof(sas));
    sas.SasEventQ = g_async_queue_new();
    sas.Enabled = TRUE;

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return TRUE;
}

void SasExit()
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    sas.Enabled = FALSE;
    g_async_queue_unref(sas.SasEventQ);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

ri_error OpenSasTunnel(uint8_t appID[8])
{
    uint32_t tunnelIndex = 0;
    uint64_t appId = 0;
    RILOG_TRACE("%s -- Entry, (%p)\n", __FUNCTION__, appID);

    if (sas.Enabled)
    {
        ConvertFromBuffer(appID, 0, appId, sizeof(appId));

        if (!findTunnelByAppId(appId, &tunnelIndex))
        {
            if (getNextTunnelIndex(appId, &tunnelIndex))
            {
                Event *sasEvent = g_try_malloc0(sizeof(Event));

                if (NULL == sasEvent)
                {
                    RILOG_FATAL(-1,
                        "line %d of %s, %s memory allocation failure!\n",
                        __LINE__, __FILE__, __func__);
                }

                sasEvent->EventType = 0;
                sasEvent->SessionNb = tunnelIndex + 1;
                memcpy(sasEvent->Data, appID, sizeof(appId));
                sasEvent->DataLen = sizeof(appId);
                g_async_queue_push(sas.SasEventQ, sasEvent);
                sas.SasTunnel[tunnelIndex].TunnelOpen = TRUE;
                sas.SasTunnel[tunnelIndex].SessionNb = sasEvent->SessionNb;
                RILOG_INFO("%s success!\n", __FUNCTION__);
                return RI_ERROR_NONE;
            }
            else
            {
                // no tunnels to open!
                RILOG_ERROR("%s connection not available?\n", __FUNCTION__);
                return RI_ERROR_CONNECTION_NOT_AVAIL;
            }
        }
        else
        {
            // tunnel already open!
            RILOG_ERROR("%s bad AppID for open?\n", __FUNCTION__);
            return RI_ERROR_INVALID_SAS_APPID;
        }
    }
    else
    {
        RILOG_ERROR("%s -- SAS not ready?!\n", __FUNCTION__);
        return RI_ERROR_CABLECARD_NOT_READY;
    }
}

ri_bool CloseSasTunnel(uint32_t sessionNb)
{
    uint32_t tunnelIndex = 0;
    RILOG_TRACE("%s -- Entry, (%X)\n", __FUNCTION__, sessionNb);

    if (sas.Enabled)
    {
        if (findTunnelBySessionNb(sessionNb, &tunnelIndex))
        {
            Event *sasEvent = g_try_malloc0(sizeof(Event));

            if (NULL == sasEvent)
            {
                RILOG_FATAL(-1,
                    "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
            }

            sasEvent->EventType = 1;
            sasEvent->SessionNb = tunnelIndex + 1;
            ConvertToBuffer(sasEvent->Data, 0,
                    sas.SasTunnel[tunnelIndex].PrivateAppId,
                    sizeof(uint64_t));
            sasEvent->DataLen = sizeof(uint64_t);
            g_async_queue_push(sas.SasEventQ, sasEvent);
            sas.SasTunnel[tunnelIndex].TunnelOpen = FALSE;
            sas.SasTunnel[tunnelIndex].SessionNb = 0;
            sas.SasTunnel[tunnelIndex].PrivateAppId = 0;
            RILOG_INFO("%s success!\n", __FUNCTION__);
            return TRUE;
        }
        else
        {
            RILOG_ERROR("%s -- Session %X not found\n", __func__, sessionNb);
            return FALSE;
        }
    }
    else
    {
        RILOG_ERROR("%s -- SAS not ready?!\n", __FUNCTION__);
        return FALSE;
    }
}

ri_error SendSasApdu(uint32_t sessionNb, uint32_t tag, size_t len, uint8_t *data)
{
    uint32_t tunnelIndex = 0;
    int b = 0;
    char temp[MAX_APDU_SIZE] = {0};
    RILOG_TRACE("%s -- Entry, (%X, %X, %d, %p)\n", __FUNCTION__,
                sessionNb, tag, len, data);
    if (sas.Enabled)
    {
        if (findTunnelBySessionNb(sessionNb, &tunnelIndex))
        {
            int i = sas.SasTunnel[tunnelIndex].CurrentApduIndex;

            if (++i >= MAX_APDUS)
                i = 0;

            sas.SasTunnel[tunnelIndex].CurrentApduIndex = i;
            sas.SasTunnel[tunnelIndex].Apdu[i].tag = tag;
            sas.SasTunnel[tunnelIndex].Apdu[i].size = len;
            memcpy(sas.SasTunnel[tunnelIndex].Apdu[i].data, data, len);

            for (b = 0; b < sas.SasTunnel[tunnelIndex].Apdu[i].size; b++)
            {
                sprintf(&temp[b * 2], "%02X",
                       sas.SasTunnel[tunnelIndex].Apdu[i].data[b]);
            }

            RILOG_INFO("%s APDU:%s\n", __FUNCTION__, temp);
            Event *sasEvent = g_try_malloc0(sizeof(Event));

            if (NULL == sasEvent)
            {
                RILOG_FATAL(-1, "line %d of %s, %d memory"
                            " allocation failure!\n",
                            __LINE__, __FILE__, sizeof(Event));
            }

            sasEvent->EventType = sas.SasTunnel[tunnelIndex].Apdu[i].tag;
            sasEvent->SessionNb = tunnelIndex + 1;
            sasEvent->DataLen = len;
            memcpy(sasEvent->Data, data, len);
            g_async_queue_push(sas.SasEventQ, sasEvent);
            RILOG_TRACE("%s success!\n", __FUNCTION__);
            return RI_ERROR_NONE;
        }
        else
        {
            RILOG_ERROR("%s Session %X not found\n", __func__, sessionNb);
            return RI_ERROR_CONNECTION_NOT_AVAIL;
        }
    }
    else
    {
        RILOG_ERROR("%s -- SAS not ready?!\n", __FUNCTION__);
        return RI_ERROR_CABLECARD_NOT_READY;
    }
}

