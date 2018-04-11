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

// Include system header files.
#include <stdlib.h>
#include <glib.h>

// Include RI Platform header files.
#include <ri_log.h>
#include <ri_config.h>
#include "audio_output_port.h"
#include "video_output_port.h"

// Back Panel logging.
#define RILOG_CATEGORY riVideoOutputPortCat
log4c_category_t *riVideoOutputPortCat = NULL;

// VideoOutputPort object information.
struct ri_videoOutputPortData_s
{
    char* id;
    av_output_port_type type;
    ri_bool enabled;
    ri_bool dtcpSupported;
    ri_bool hdcpSupported;
    uint32_t restrictedResolution;
    void* audioOutputPort;
    ri_bool displayConnected;
    //type?               configuration...
};

// VideoOutputPort forward declarations
char* vp_getId(ri_videoOutputPort_t*);
av_output_port_type vp_getType(ri_videoOutputPort_t*);
ri_bool vp_isEnabled(ri_videoOutputPort_t*);
void vp_setEnabled(ri_videoOutputPort_t*, ri_bool);
void vp_getCap(ri_videoOutputPort_t*, av_output_port_capability, void*);
void vp_dumpState(char*, ri_videoOutputPort_t*);
void* vp_getAudioOutputPort(ri_videoOutputPort_t*);
ri_bool vp_isDisplayConnected(ri_videoOutputPort_t*);
void vp_setDisplayConnected(ri_videoOutputPort_t*, ri_bool);

//Fallback default values
static ri_bool d_enabled = FALSE;
static ri_bool d_dtcpSupported = FALSE;
static ri_bool d_hdcpSupported = FALSE;
static uint32_t d_restrictedResolution = -1;
static ri_bool d_displayConnected = FALSE;

static char *apCfgNameBase = "RI.Platform.backpanel.video_output_port.";
static char* getConfigOrDefault(char* configItem, int i)
{
    char cvn[256];
    char *cv;
    sprintf(cvn, "%s%d.%s", apCfgNameBase, i, configItem);
    cv = ricfg_getValue("RIPlatform", cvn);
    if (cv == NULL)
    {
        sprintf(cvn, "%sx.%s", apCfgNameBase, configItem);
        cv = ricfg_getValue("RIPlatform", cvn);
    }
    return cv;
}

ri_videoOutputPort_t* create_video_output_port(int i, GHashTable* aht)
{
    riVideoOutputPortCat = log4c_category_get("RI.BackPanel.videoOutputPort");

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    char cvn[256];
    char *cv;

    sprintf(cvn, "%s%d.%s", apCfgNameBase, i, "id");
    cv = ricfg_getValue("RIPlatform", cvn);
    if (NULL == cv)
    {
        RILOG_DEBUG("%s -- No id specified for video port #%d\n.",
                __FUNCTION__, i);

        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return NULL;
    }

    ri_videoOutputPort_t* vp = g_try_malloc0(sizeof(ri_videoOutputPort_t));
    if (NULL == vp)
    {
        RILOG_ERROR("%s -- couldn't allocate vp?!\n.", __FUNCTION__);

        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return NULL;
    }

    vp->m_data = g_try_malloc0(sizeof(ri_videoOutputPortData_t));
    if (NULL == vp->m_data)
    {
        RILOG_ERROR("%s -- couldn't allocate vp->m_data?!\n.", __FUNCTION__);
        g_free(vp);

        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return NULL;
    }

    vp->getId = vp_getId;
    vp->getType = vp_getType;
    vp->isEnabled = vp_isEnabled;
    vp->setEnabled = vp_setEnabled;
    vp->getCap = vp_getCap;
    vp->dumpState = vp_dumpState;
    vp->getAudioOutputPort = vp_getAudioOutputPort;
    vp->isDisplayConnected = vp_isDisplayConnected;
    vp->setDisplayConnected = vp_setDisplayConnected;
    vp->m_data->id = cv;

    sprintf(cvn, "%s%d.%s", apCfgNameBase, i, "type");
    cv = ricfg_getValue("RIPlatform", cvn);
    if (NULL != cv)
    {
        int avType = atoi(cv);
        if (avType >= RF && avType <= INTERNAL)
        {
            vp->m_data->type = avType;
        }
    }

    cv = getConfigOrDefault("enabled", i);
    if (NULL != cv)
    {
        if (strcmp(cv, "TRUE") == 0)
        {
            vp->m_data->enabled = TRUE;
        }
        else
        {
            vp->m_data->enabled = FALSE;
        }
    }
    else
    {
        vp->m_data->enabled = d_enabled;
    }
    RILOG_DEBUG("+(ds-vop-p) video_output_port.c::create_video_output_port:VOP [%s] enabled = %d\n", 
                vp->m_data->id, vp->m_data->enabled);
    
    cv = getConfigOrDefault("dtcpSupported", i);
    if (NULL != cv)
    {
        if (strcmp(cv, "TRUE") == 0)
        {
            vp->m_data->dtcpSupported = TRUE;
        }
        else
        {
            vp->m_data->dtcpSupported = FALSE;
        }
    }
    else
    {
        vp->m_data->dtcpSupported = d_dtcpSupported;
    }

    cv = getConfigOrDefault("hdcpSupported", i);
    if (NULL != cv)
    {
        if (strcmp(cv, "TRUE") == 0)
        {
            vp->m_data->hdcpSupported = TRUE;
        }
        else
        {
            vp->m_data->hdcpSupported = FALSE;
        }
    }
    else
    {
        vp->m_data->hdcpSupported = d_hdcpSupported;
    }

    cv = getConfigOrDefault("restrictedResolution", i);
    if (NULL != cv)
    {
        vp->m_data->restrictedResolution = atoi(cv);
    }
    else
    {
        vp->m_data->restrictedResolution = d_restrictedResolution;
    }

    cv = getConfigOrDefault("audioOutputPort", i);
    if (NULL != cv)
    {
        RILOG_DEBUG("%s -- Looking for audio output port, id = %s\n",
                __FUNCTION__, cv);

        ri_audioOutputPort_t* forVideoPort = g_hash_table_lookup(aht, cv);
        if (forVideoPort == NULL)
        {
            RILOG_ERROR(
                    "%s -- Invalid audio port specified for video port: video port = %s, audio port = %s\n",
                    __FUNCTION__, vp->m_data->id, cv);
        }
        else
        {
            RILOG_DEBUG(
                    "%s -- Found audio output port, id = %s, address = %p\n",
                    __FUNCTION__, cv, forVideoPort);
            vp->m_data->audioOutputPort = forVideoPort;
        }
    }
    else
    {
        RILOG_ERROR("%s -- No audio port specified for video port! %s\n",
                __FUNCTION__, vp->m_data->id);
    }

    cv = getConfigOrDefault("displayConnected", i);
    if (NULL != cv)
    {
        if (strcmp(cv, "TRUE") == 0)
        {
            vp->m_data->displayConnected = TRUE;
        }
        else
        {
            vp->m_data->displayConnected = FALSE;
        }
    }
    else
    {
        vp->m_data->displayConnected = d_displayConnected;
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return vp;
}

void destroy_video_output_port(ri_videoOutputPort_t* vp)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    g_free(vp->m_data);
    g_free(vp);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

char* vp_getId(ri_videoOutputPort_t* vp)
{
    return vp->m_data->id;
}
av_output_port_type vp_getType(ri_videoOutputPort_t* vp)
{
    return vp->m_data->type;
}
ri_bool vp_isEnabled(ri_videoOutputPort_t* vp)
{
    RILOG_DEBUG("+(ds-vop-p) video_output_port.c::vp_isEnabled:VOP [%s] enabled = %d\n", 
                vp->m_data->id, vp->m_data->enabled);
    return vp->m_data->enabled;
}
void vp_setEnabled(ri_videoOutputPort_t* vp, ri_bool enableDisableFlag)
{
    RILOG_DEBUG("+(ds-vop-p) video_output_port.c::vp_setEnabled:VOP [%s] enabled = %d\n", 
                vp->m_data->id, vp->m_data->enabled);
    vp->m_data->enabled = enableDisableFlag;
}
void vp_getCap(ri_videoOutputPort_t* vp, av_output_port_capability cap,
        void* result)
{
    switch (cap)
    {
    case DTCP:
        *((int*) result) = vp->m_data->dtcpSupported;
        break;
    case HDCP:
        *((int*) result) = vp->m_data->hdcpSupported;
        break;
    case RESOLUTION_RESTRICTION:
        *((int*) result) = vp->m_data->restrictedResolution;
        break;
    default:
        RILOG_ERROR("%s -- Invalid argument: cap = %d", __FUNCTION__, cap);
        break;
    }
}

void* vp_getAudioOutputPort(ri_videoOutputPort_t* vp)
{
    return vp->m_data->audioOutputPort;
}
ri_bool vp_isDisplayConnected(ri_videoOutputPort_t* vp)
{
    return vp->m_data->displayConnected;
}
void vp_setDisplayConnected(ri_videoOutputPort_t* vp, ri_bool connectedDisconnectedFlag)
{
    vp->m_data->displayConnected = connectedDisconnectedFlag;
}

void vp_dumpState(char* pre, ri_videoOutputPort_t* vp)
{
    RILOG_DEBUG("%s id: %s\n", pre, vp->m_data->id);
    RILOG_DEBUG("%s type: %d\n", pre, vp->m_data->type);
    RILOG_DEBUG("%s enabled: %d\n", pre, vp->m_data->enabled);
    RILOG_DEBUG("%s dtcpSupported: %d\n", pre, vp->m_data->dtcpSupported);
    RILOG_DEBUG("%s hdcpSupported: %d\n", pre, vp->m_data->hdcpSupported);
    RILOG_DEBUG("%s displayConnected: %d\n", pre, vp->m_data->displayConnected);
    RILOG_DEBUG("%s restrictedResolution: %d\n", pre, vp->m_data->restrictedResolution);
}

