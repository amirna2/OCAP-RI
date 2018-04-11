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
#include "backpanel.h"

// Back Panel logging.
#define RILOG_CATEGORY riBackPanelCat
log4c_category_t *riBackPanelCat = NULL;

#define LOWEST_KEYCODE 49
static int gMaxVideoOutputPortIndex = 0;

// Back Panel object information.
struct ri_backpanel_data_s
{
    // The audio output port map. Key is id, value is audioOutputPort object.
    GHashTable *audioOutputPortMap;
    // The audio output port list mutex.
    GMutex *audioOutputPortMapMutex;

    // The video output port map. Key is id, value is videoOutputPort object.
    GHashTable *videoOutputPortMap;
    // The video output port list mutex.
    GMutex *videoOutputPortMapMutex;

    //Connect/Disconnect callback function
    void (*connectDisconnectCallback)(gboolean connected, void* videoPortHandle);
};

//forward declarations
uint8_t bp_getAudioOutputPortNameList(char*** names);
void bp_freeAudioOutputPortNameList(char** names);
void* bp_getAudioOutputPortHandle(char* id);
void bp_getAudioOutputPortValue(void* handle, ri_audio_output_port_cap cap,
        void* value);
void bp_setAudioOutputPortValue(void* handle, ri_audio_output_port_cap cap,
        void* value);
uint8_t bp_getAudioOutputPortSupportedCompressions(void* handle, int** array);
uint8_t bp_getAudioOutputPortSupportedEncodings(void* handle, int** array);
uint8_t bp_getAudioOutputPortSupportedStereoModes(void* handle, int** array);

uint8_t bp_getVideoOutputPortNameList(char*** ids);
void bp_freeVideoOutputPortNameList(char** ids);
void* bp_getVideoOutputPortHandle(char* id);
void bp_getVideoOutputPortValue(void* handle, ri_video_output_port_cap cap,
        void* value);
void bp_setVideoOutputPortValue(void* handle, ri_video_output_port_cap cap,
        void* value);

void bp_setVideoOutputPortConnectDisconnectCallback(void (*cb)(gboolean connected,
		void* videoOutputPortHandle));

ri_backpanel_data_t *ri_backpanel_data;

// The singleton instance of the Back Panel.
static ri_backpanel_t *g_backpanel_instance = NULL;

ri_backpanel_t *create_backpanel()
{
    char *cfgValue;

    riBackPanelCat = log4c_category_get("RI.BackPanel");
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    g_backpanel_instance = g_try_malloc(sizeof(ri_backpanel_t));

    if (NULL == g_backpanel_instance)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    memset(g_backpanel_instance, 0, sizeof(ri_backpanel_t));

    // Allocate object memory.
    g_backpanel_instance->ri_backpanel_data = g_try_malloc(
            sizeof(ri_backpanel_data_t));

    if (NULL == g_backpanel_instance->ri_backpanel_data)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    memset(g_backpanel_instance->ri_backpanel_data, 0,
            sizeof(ri_backpanel_data_t));

    // Initialize internal Back Panel implementation.
    g_backpanel_instance->getAudioOutputPortNameList
            = bp_getAudioOutputPortNameList;
    g_backpanel_instance->freeAudioOutputPortNameList
            = bp_freeAudioOutputPortNameList;
    g_backpanel_instance->getAudioOutputPortHandle
            = bp_getAudioOutputPortHandle;
    g_backpanel_instance->getAudioOutputPortValue = bp_getAudioOutputPortValue;
    g_backpanel_instance->setAudioOutputPortValue = bp_setAudioOutputPortValue;
    g_backpanel_instance->getAudioOutputPortSupportedCompressions
            = bp_getAudioOutputPortSupportedCompressions;
    g_backpanel_instance->getAudioOutputPortSupportedEncodings
            = bp_getAudioOutputPortSupportedEncodings;
    g_backpanel_instance->getAudioOutputPortSupportedStereoModes
            = bp_getAudioOutputPortSupportedStereoModes;

    g_backpanel_instance->getVideoOutputPortNameList
            = bp_getVideoOutputPortNameList;
    g_backpanel_instance->freeVideoOutputPortNameList
            = bp_freeVideoOutputPortNameList;
    g_backpanel_instance->getVideoOutputPortHandle
            = bp_getVideoOutputPortHandle;
    g_backpanel_instance->getVideoOutputPortValue = bp_getVideoOutputPortValue;
    g_backpanel_instance->setVideoOutputPortValue = bp_setVideoOutputPortValue;
    g_backpanel_instance->setVideoOutputPortConnectDisconnectCallback
			= bp_setVideoOutputPortConnectDisconnectCallback;

    g_backpanel_instance->ri_backpanel_data->audioOutputPortMap
            = g_hash_table_new(g_str_hash, g_str_equal);
    g_backpanel_instance->ri_backpanel_data->audioOutputPortMapMutex
            = g_mutex_new();
    g_backpanel_instance->ri_backpanel_data->videoOutputPortMap
            = g_hash_table_new(g_str_hash, g_str_equal);
    g_backpanel_instance->ri_backpanel_data->videoOutputPortMapMutex
            = g_mutex_new();
    g_backpanel_instance->ri_backpanel_data->connectDisconnectCallback = NULL;


    //Construct Audio Output Ports...
    //shorthand for hashtable
    GHashTable* aht =
            g_backpanel_instance->ri_backpanel_data->audioOutputPortMap;
    if ((cfgValue = ricfg_getValue("RIPlatform",
            "RI.Platform.backpanel.number_of_audio_ports")) != NULL)
    {
        int numberOfAudioPorts = atoi(cfgValue);
        RILOG_INFO("Number of Audio ports: %d\n", numberOfAudioPorts);

        int i;
        for (i = 0; i < numberOfAudioPorts; i++)
        {
            ri_audioOutputPort_t* audioOutputPort = create_audio_output_port(i);
            if (NULL != audioOutputPort)
            {
                if (g_hash_table_lookup(aht, audioOutputPort->getId(
                        audioOutputPort)) == NULL)
                {
                    g_hash_table_insert(aht, audioOutputPort->getId(
                            audioOutputPort), audioOutputPort);
                }
                else
                {
                    destroy_audio_output_port(audioOutputPort);
                }
            }
        }
    }

    //Construct Video Output Ports...
    GHashTable* vht =
            g_backpanel_instance->ri_backpanel_data->videoOutputPortMap;
    if ((cfgValue = ricfg_getValue("RIPlatform",
            "RI.Platform.backpanel.max_video_output_port_index")) != NULL)
    {
    	gMaxVideoOutputPortIndex = atoi(cfgValue);
    }
    int i;
    for (i = 0; i <= gMaxVideoOutputPortIndex; i++)
    {
        ri_videoOutputPort_t* videoOutputPort =
                create_video_output_port(i, aht);
        if (NULL != videoOutputPort)
        {
            if (g_hash_table_lookup(vht,
                    videoOutputPort->getId(videoOutputPort)) == NULL)
            {
                g_hash_table_insert(vht,
                        videoOutputPort->getId(videoOutputPort),
                        videoOutputPort);
                RILOG_DEBUG("+(ds-vop-p)----- Constructed Video Port: %s -----\n", videoOutputPort->getId(
                        videoOutputPort));
                videoOutputPort->dumpState("+(ds-vop-p)", videoOutputPort);
            }
            else
            {
                RILOG_ERROR("+(ds-vop-p) Duplicate Video Port found! %s\n",
                        videoOutputPort->getId(videoOutputPort));
                destroy_video_output_port(videoOutputPort);
            }
            RILOG_DEBUG("Video Output Port: %p -- Audio Output Port: %p\n",
                    videoOutputPort, videoOutputPort->getAudioOutputPort(
                            videoOutputPort));
        }
        else
        {
            RILOG_WARN(
                    "+(ds-vop-p) Null videoOutputPort returned. Could be a configuration error. Index = %d\n",
                    i);
        }

    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return g_backpanel_instance;
}

void destroy_backpanel(ri_backpanel_t *bp)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    if (g_backpanel_instance != NULL)
    {
        if (g_backpanel_instance->ri_backpanel_data != NULL)
        {
            //clean up video output ports
            GHashTable* ht =
                    g_backpanel_instance->ri_backpanel_data->videoOutputPortMap;
            GList* ap_ids = g_hash_table_get_keys(ht);
            char* ap_id = (char*) g_list_first(ap_ids);
            while (NULL != ap_id)
            {
                destroy_video_output_port(
                        (ri_videoOutputPort_t*) g_hash_table_lookup(ht, ap_id));
            }
            g_list_free(ap_ids);
            g_hash_table_remove_all(ht);
            g_hash_table_destroy(ht);
            g_mutex_free(
                    g_backpanel_instance->ri_backpanel_data->videoOutputPortMapMutex);

            //clean up audio output ports
            ht = g_backpanel_instance->ri_backpanel_data->audioOutputPortMap;
            ap_ids = g_hash_table_get_keys(ht);
            ap_id = (char*) g_list_first(ap_ids);
            while (NULL != ap_id)
            {
                destroy_audio_output_port(
                        (ri_audioOutputPort_t*) g_hash_table_lookup(ht, ap_id));
            }
            g_list_free(ap_ids);
            g_hash_table_remove_all(ht);
            g_hash_table_destroy(ht);
            g_mutex_free(
                    g_backpanel_instance->ri_backpanel_data->audioOutputPortMapMutex);

            g_free(g_backpanel_instance->ri_backpanel_data);
        }
        g_free(g_backpanel_instance);
        g_backpanel_instance = NULL;
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

ri_backpanel_t *get_backpanel()
{
    return g_backpanel_instance;
}

void lockAudioOutputPortMap()
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    g_mutex_lock(
            g_backpanel_instance->ri_backpanel_data->audioOutputPortMapMutex);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

void unlockAudioOutputPortMap()
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    g_mutex_unlock(
            g_backpanel_instance->ri_backpanel_data->audioOutputPortMapMutex);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

ri_audioOutputPort_t* getAudioOutputPortById(char* id)
{
    return (ri_audioOutputPort_t*) g_hash_table_lookup(
            g_backpanel_instance->ri_backpanel_data->audioOutputPortMap, id);
}

uint8_t bp_getAudioOutputPortNameList(char*** names)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    lockAudioOutputPortMap();
    int nameListSize = g_hash_table_size(
            g_backpanel_instance->ri_backpanel_data->audioOutputPortMap);
    *names = (char **) g_try_malloc(sizeof(char *) * (nameListSize + 1));

    if (NULL == *names)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    (*names)[nameListSize] = NULL;

    gpointer key, value;
    const char* p;
    GHashTableIter iter;
    g_hash_table_iter_init(&iter,
            g_backpanel_instance->ri_backpanel_data->audioOutputPortMap);
    int i = 0;
    while (g_hash_table_iter_next(&iter, &key, &value))
    {
        p = (char *) key;
        (*names)[i] = (char *) g_try_malloc(sizeof(char) * (strlen(p) + 1));

        if (NULL == (*names)[i])
        {
            RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                        __LINE__, __FILE__, __func__);
        }

        strcpy((*names)[i], p);
        i++;
    }
    unlockAudioOutputPortMap();

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return nameListSize;
}

void bp_freeAudioOutputPortNameList(char** names)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    if (NULL == names)
    {
        return;
    }
    int i;
    for (i = 0; names[i] != NULL; i++)
    {
        g_free(names[i]);
    }
    g_free(names);

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);
}

void* bp_getAudioOutputPortHandle(char* id)
{
    return (void*) getAudioOutputPortById(id);
}

void bp_getAudioOutputPortValue(void* handle, ri_audio_output_port_cap cap,
        void* value)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    if (handle != NULL)
    {
        ri_audioOutputPort_t* ap = (ri_audioOutputPort_t*) handle;
        switch (cap)
        {
        case AUDIO_OUTPUT_PORT_ID:
            *((char**) value) = ap->getId(ap);
            break;
        case COMPRESSION:
            *((int*) value) = ap->getCompression(ap);
            break;
        case GAIN:
            *((float*) value) = ap->getGain(ap);
            break;
        case ENCODING:
            *((int*) value) = ap->getEncoding(ap);
            break;
        case LEVEL:
            *((float*) value) = ap->getLevel(ap);
            break;
        case STEREO_MODE:
            *((int*) value) = ap->getStereoMode(ap);
            break;
        case LOOP_THRU:
            *((int*) value) = ap->isLoopThru(ap);
            break;
        case MUTED:
            *((int*) value) = ap->isMuted(ap);
            break;
        case OPTIMAL_LEVEL:
            *((float*) value) = ap->getOptimalLevel(ap);
            break;
        case MAX_DB:
            *((float*) value) = ap->getMaxDb(ap);
            break;
        case MIN_DB:
            *((float*) value) = ap->getMinDb(ap);
            break;
        case LOOP_THRU_SUPPORTED:
            *((int*) value) = ap->isLoopThruSupported(ap);
            break;
        default:
            RILOG_ERROR("Invalid argument to %s, cap = %d", __FUNCTION__, cap);
            break;
        }
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

void bp_setAudioOutputPortValue(void* handle, ri_audio_output_port_cap cap,
        void* value)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    if (handle != NULL)
    {
        ri_audioOutputPort_t* ap = (ri_audioOutputPort_t*) handle;
        switch (cap)
        {
        case COMPRESSION:
            ap->setCompression(ap, *((int*) value));
            break;
        case GAIN:
            ap->setGain(ap, *((float*) value));
            break;
        case ENCODING:
            ap->setEncoding(ap, *((int*) value));
            break;
        case LEVEL:
            ap->setLevel(ap, *((float*) value));
            break;
        case STEREO_MODE:
            ap->setStereoMode(ap, *((int*) value));
            break;
        case LOOP_THRU:
            ap->setLoopThru(ap, *((int*) value));
            break;
        case MUTED:
            ap->setMuted(ap, *((int*) value));
            break;
        default:
            RILOG_ERROR("Invalid argument to %s, cap = %d", __FUNCTION__, cap);
            break;
        }
    }
    else
    {
        RILOG_ERROR("Invalid argument to %s, handle is NULL!", __FUNCTION__);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

uint8_t bp_getAudioOutputPortSupportedCompressions(void* handle, int** array)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    int returnThis = 0;
    ri_audioOutputPort_t* ap = (ri_audioOutputPort_t*) handle;
    if (ap != NULL)
    {
        *array = (int*) (ap->getSupportedCompressions(ap));
        returnThis = ap->getSupportedCompressionsCount(ap);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return returnThis;
}
uint8_t bp_getAudioOutputPortSupportedEncodings(void* handle, int** array)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    int returnThis = 0;
    ri_audioOutputPort_t* ap = (ri_audioOutputPort_t*) handle;
    if (ap != NULL)
    {
        *array = (int*) (ap->getSupportedEncodings(ap));
        returnThis = ap->getSupportedEncodingsCount(ap);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return returnThis;
}
uint8_t bp_getAudioOutputPortSupportedStereoModes(void* handle, int** array)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    int returnThis = 0;
    ri_audioOutputPort_t* ap = (ri_audioOutputPort_t*) handle;
    if (ap != NULL)
    {
        *array = (int*) (ap->getSupportedStereoModes(ap));
        returnThis = ap->getSupportedStereoModesCount(ap);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return returnThis;
}

void lockVideoOutputPortMap()
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);
    g_mutex_lock(
            g_backpanel_instance->ri_backpanel_data->videoOutputPortMapMutex);
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

void unlockVideoOutputPortMap()
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    g_mutex_unlock(
            g_backpanel_instance->ri_backpanel_data->videoOutputPortMapMutex);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

ri_videoOutputPort_t* getVideoOutputPortById(char* id)
{
    return (ri_videoOutputPort_t*) g_hash_table_lookup(
            g_backpanel_instance->ri_backpanel_data->videoOutputPortMap, id);
}



uint8_t bp_getVideoOutputPortNameList(char*** names)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    lockVideoOutputPortMap();
    int nameListSize = g_hash_table_size(
            g_backpanel_instance->ri_backpanel_data->videoOutputPortMap);
    *names = (char **) g_try_malloc(sizeof(char *) * (nameListSize + 1));

    if (NULL == *names)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    (*names)[nameListSize] = NULL;

    gpointer key, value;
    const char* p;
    GHashTableIter iter;
    g_hash_table_iter_init(&iter,
            g_backpanel_instance->ri_backpanel_data->videoOutputPortMap);
    int i = 0;
    while (g_hash_table_iter_next(&iter, &key, &value))
    {
        p = (char *) key;
        (*names)[i] = (char *) g_try_malloc(sizeof(char) * (strlen(p) + 1));

        if (NULL == (*names)[i])
        {
            RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                        __LINE__, __FILE__, __func__);
        }

        strcpy((*names)[i], p);
        i++;
    }
    unlockVideoOutputPortMap();

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return nameListSize;
}

void bp_freeVideoOutputPortNameList(char** names)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);
    if (NULL == names)
    {
        return;
    }
    int i;
    for (i = 0; names[i] != NULL; i++)
    {
        g_free(names[i]);
    }
    g_free(names);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

void* bp_getVideoOutputPortHandle(char* id)
{
    return getVideoOutputPortById(id);
}
void bp_getVideoOutputPortValue(void* handle, ri_video_output_port_cap cap,
        void* value)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    ri_videoOutputPort_t* vp = (ri_videoOutputPort_t*) handle;
    switch (cap)
    {
    case VIDEO_OUTPUT_PORT_ID:
        *((char**) value) = vp->getId(vp);
        break;
    case VIDEO_OUTPUT_PORT_TYPE:
        *((int*) value) = vp->getType(vp);
        break;
    case VIDEO_OUTPUT_ENABLED:
        *((ri_bool*) value) = vp->isEnabled(vp);
        break;
    case VIDEO_OUTPUT_CONNECTED:
        *((ri_bool*) value) = vp->isDisplayConnected(vp);
        break;
    case VIDEO_OUTPUT_AUDIO_PORT:
        *((int*) value) = (int) vp->getAudioOutputPort(vp);
        break;
    case VIDEO_OUTPUT_DTCP_SUPPORTED:
        vp->getCap(vp, DTCP, value);
        break;
    case VIDEO_OUTPUT_HDCP_SUPPORTED:
        vp->getCap(vp, HDCP, value);
        break;
    case VIDEO_OUTPUT_RESOLUTION_RESTRICTION:
        vp->getCap(vp, RESOLUTION_RESTRICTION, value);
        break;
    default:
        RILOG_ERROR("Invalid argument to %s, cap = %d", __FUNCTION__, cap);
        break;
    }
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}
void bp_setVideoOutputPortValue(void* handle, ri_video_output_port_cap cap,
        void* value)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    ri_videoOutputPort_t* vp = (ri_videoOutputPort_t*) handle;
    switch (cap)
    {
    case VIDEO_OUTPUT_ENABLED:
        vp->setEnabled(vp, *((ri_bool*) value));
        break;
    case VIDEO_OUTPUT_CONNECTED:
        vp->setDisplayConnected(vp, *((ri_bool*) value));
        break;
    default:
        RILOG_ERROR("Invalid argument to %s, cap = %d", __FUNCTION__, cap);
        break;
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

void bp_setVideoOutputPortConnectDisconnectCallback(void (*cb)(gboolean connected,
		void* videoOutputPortHandle))
{
	g_backpanel_instance->ri_backpanel_data->connectDisconnectCallback = cb;
}

//provided so that window code can "fake" a connect/disconnect.
void toggleDisplayConnectedDisconnected(int portIndex)
{
	if(portIndex < gMaxVideoOutputPortIndex
		&& g_backpanel_instance->ri_backpanel_data->connectDisconnectCallback != NULL)
	{
		ri_videoOutputPort_t* handle =
				(ri_videoOutputPort_t*) g_list_nth_data(g_hash_table_get_values(
				g_backpanel_instance->ri_backpanel_data->videoOutputPortMap), portIndex);

		gboolean currentConnectedState;
		g_backpanel_instance->getVideoOutputPortValue(handle, VIDEO_OUTPUT_CONNECTED, &currentConnectedState);

		gboolean newConnectedState = !currentConnectedState;
		g_backpanel_instance->setVideoOutputPortValue(handle, VIDEO_OUTPUT_CONNECTED, &newConnectedState);

		//Call up to mpeos layer.
		g_backpanel_instance->ri_backpanel_data->connectDisconnectCallback(newConnectedState, handle);
	}
}

