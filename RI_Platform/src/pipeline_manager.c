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

#include <ri_pipeline_manager.h>
#include <ri_pipeline.h>
#include <ri_log.h>
#include <ri_display.h>
#include <glib.h>
#include <gst/gst.h>
#include <glib/gprintf.h>
#include <test_interface.h>

#include <stdlib.h>

#include "pipeline.h"
#include "platform.h"
#include "oob_pipeline.h"
#include "dsg_pipeline.h"
#include "tuner.h"
#include "display.h"

#define RILOG_CATEGORY pipelineCategory
log4c_category_t* pipelineCategory = NULL;

static ri_pipeline_manager_t* pipeline_manager_instance = NULL;

#define TUNER_UDP_BUFFER_SIZE 1024*300

// The pipeline_manager private data
struct ri_pipeline_manager_data_s
{
    // Out-of-band pipeline.  Only 1 per platform
    ri_oob_pipeline_t* oob_pipeline;

    // Out-of-band pipeline.  Only 1 per platform
    ri_dsg_pipeline_t* dsg_pipeline;

    // Live media pipelines
    uint8_t num_live_pipelines;
    ri_pipeline_t** live_pipelines;

    // DVR record pipelines
    //uint8_t num_record_pipelines;
    //ri_record_pipeline_t* record_pipelines;

    // DVR local playback pipelines
    uint8_t num_play_pipelines;
    ri_pipeline_t** play_pipelines;

    // DVR remote playback pipelines
    uint8_t num_hn_server_pipelines;
    ri_pipeline_t** hn_server_pipelines;

    // HN Client Stream pipeline
    ri_pipeline_t* hn_player_pipeline;

    // Display Device display, currently only 1 per platform
    ri_display_t* display;
};

#define PIPELINE_TESTS \
   "\r\n" \
   "|---+-----------------------\r\n" \
   "| q | show QoS message count\r\n" \
   "|---+-----------------------\r\n" \
   "| s | decode start \r\n"          \
   "|---+-----------------------\r\n" \
   "| t | decode stop \r\n"           \


static int testInputHandler(int sock, char *rxBuf, int *retCode, char **retStr)
{
    RILOG_TRACE("%s -- Entry, received: %s\n", __FUNCTION__, rxBuf);
    *retCode = MENU_SUCCESS;
    ri_pipeline_manager_t *pipeline_manager = ri_get_pipeline_manager();
    uint32_t num_pipelines = 0;
    ri_pipeline_t **pipelines = (ri_pipeline_t **)
        pipeline_manager->get_live_pipelines(pipeline_manager, &num_pipelines);

    if (strstr(rxBuf, "q"))
    {
        int i = 0;
        test_SendString(sock, "\r\n");

        for (i = 0; i < num_pipelines; i++)
        {
            if (NULL != pipelines[i])
            {
                char log[64];
                snprintf(log, sizeof(log),
                        " - pipeline[%d] QoS message count: %lu",
                         i, pipelines[i]->qos_messages);
                RILOG_INFO("%s%s\n", __FUNCTION__, log);
                strcat(log, "\r\n");
                test_SendString(sock, log);
            }
            else
            {
                RILOG_WARN("%s - pipeline was NULL", __FUNCTION__);
            }
        }

        RILOG_TRACE("%s -- Exit", __FUNCTION__);
        return 0;
    }
    else if (strstr(rxBuf, "s"))
    {
        if (NULL != pipelines[0])
        {
            RILOG_INFO("%s -- about to call decode\n", __FUNCTION__);
            pipelines[0]->decode(pipelines[0],
                    pipelines[0]->data->video_device,
                    pipelines[0]->data->decode_pids,
                    pipelines[0]->data->decode_pid_count);
        }
        else
        {
            RILOG_INFO("%s -- pipeline was NULL", __FUNCTION__);
        }

        RILOG_TRACE("%s -- Exit", __FUNCTION__);
        return 0;
    }
    else if (strstr(rxBuf, "t"))
    {
        if (NULL != pipelines[0])
        {
            RILOG_WARN("%s - about to call decode_stop\n", __FUNCTION__);
            pipelines[0]->decode_stop(pipelines[0]);
        }
        else
        {
            RILOG_WARN("%s - pipeline was NULL", __FUNCTION__);
        }

        RILOG_TRACE("%s -- Exit", __FUNCTION__);
        return 0;
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
        RILOG_WARN("%s %s\n", __FUNCTION__, rxBuf);
        *retCode = MENU_INVALID;

        RILOG_TRACE("%s -- Exit", __FUNCTION__);
        return 0;
    }
}

static MenuItem GstMenuItem =
{ TRUE, "g", "Gstreamer pipline", PIPELINE_TESTS, testInputHandler };

gboolean test_decode_switch(gpointer display);

const ri_oob_pipeline_t* get_oob_pipeline(ri_pipeline_manager_t* object)
{
    return (const ri_oob_pipeline_t*) (pipeline_manager_instance->data->oob_pipeline);
}

const ri_dsg_pipeline_t* get_dsg_pipeline(ri_pipeline_manager_t* object)
{
    return (const ri_dsg_pipeline_t*) (pipeline_manager_instance->data->dsg_pipeline);
}

const ri_pipeline_t** get_live_pipelines(ri_pipeline_manager_t* object,
        uint32_t* num_pipelines)
{
    *num_pipelines = object->data->num_live_pipelines;
    return (const ri_pipeline_t**) (object->data->live_pipelines);
}

const ri_pipeline_t** get_playback_pipelines(ri_pipeline_manager_t* object,
        uint32_t* num_pipelines)
{
    *num_pipelines = object->data->num_play_pipelines;
    return (const ri_pipeline_t**) (object->data->play_pipelines);
}

const ri_pipeline_t** get_hn_server_pipelines(ri_pipeline_manager_t* object,
        uint32_t* num_pipelines)
{
    *num_pipelines = object->data->num_hn_server_pipelines;
    return (const ri_pipeline_t**) (object->data->hn_server_pipelines);
}

ri_pipeline_t* get_hn_player_pipeline(ri_pipeline_manager_t* object)
{
    return (ri_pipeline_t*) (pipeline_manager_instance->data->hn_player_pipeline);
}

ri_display_t* get_display(ri_pipeline_manager_t* object)
{
    return object->data->display;
}

void create_pipeline_manager()
{
    int i;
    int numPipelines;
    int numPlayPipelines = 1;
    int numHNServerPipelines = 3;

    // Create our logging category
    pipelineCategory = log4c_category_get("RI.Pipeline");
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    test_RegisterMenu(&GstMenuItem);

    // Determine the number of live pipelines based on the number of tuners specified
    // in the configuration file
    char* numTuners = ricfg_getValue("RIPlatform", "RI.Platform.numTuners");
    if ((numPipelines = atoi(numTuners)) == 0)
    {
        RILOG_FATAL(-1, "%s() -- Invalid number of tuners specified!\n",
                __FUNCTION__);
    }
    RILOG_INFO("%s() -- Num live pipelines is %d\n", __FUNCTION__, numPipelines);
    RILOG_INFO("%s() -- Num Local playback pipelines is 1\n", __FUNCTION__);

    // Determine number of remote playback session pipelines that will be supported
    char* hnPlaybackSessions = ricfg_getValue("RIPlatform",
            "RI.Platform.numHNPlaybackSessions");
    if ((NULL != hnPlaybackSessions) && ((numHNServerPipelines = atoi(
            hnPlaybackSessions)) == 0))
    {
        RILOG_FATAL(-1,
                "%s() -- Invalid number of HN playback sessions specified!\n",
                __FUNCTION__);
    }
    RILOG_INFO("%s() -- Num HN server pipelines is %d\n", __FUNCTION__,
            numHNServerPipelines);
    RILOG_INFO("%s() -- Num HN player pipelines is 1\n", __FUNCTION__);

    // Allocate the pipeline_manager structure and private data
    pipeline_manager_instance = g_try_malloc0(sizeof(ri_pipeline_manager_t));

    if (NULL == pipeline_manager_instance)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    pipeline_manager_instance->data = g_try_malloc0(
            sizeof(ri_pipeline_manager_data_t));

    if (NULL == pipeline_manager_instance->data)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    // Allocate display display
    pipeline_manager_instance->data->display = create_display();

    // Allocate live pipelines
    pipeline_manager_instance->data->live_pipelines = g_try_malloc0(
            sizeof(ri_pipeline_t*) * numPipelines);

    if (NULL == pipeline_manager_instance->data->live_pipelines)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    pipeline_manager_instance->data->num_live_pipelines = numPipelines;
    for (i = 0; i < numPipelines; i++)
    {
        int port;

        ri_pipeline_t* pipeline = create_pipeline(RI_PIPELINE_TYPE_LIVE_TSB);

        ri_tuner_t* tuner = pipeline->get_tuner(pipeline);

        // Retrieve the UDP port for this tuner from the config file
        {
            char cfgVar[256];
            char* cfgValue;

            sprintf(cfgVar, "RI.Headend.tuner.%d.TunerRxPort", i);
            if ((cfgValue = ricfg_getValue("RIPlatform", cfgVar)) == NULL)
            {
                cfgValue = "4140";
                RILOG_WARN("%s %s not specified, using default value of %s!\n",
                        __FUNCTION__, cfgVar, cfgValue);
            }
            port = atoi(cfgValue);
        }
        // Set the UDP port
        set_tuner_udp_port(tuner, (uint16_t) port);

        // Set the UDP buffer size
        set_tuner_udp_buffer_size(tuner, (uint32_t) TUNER_UDP_BUFFER_SIZE);

        // Start the pipeline
        start_pipeline(pipeline);

        pipeline_manager_instance->data->live_pipelines[i] = pipeline;
    }

    // Allocate local playback pipelines
    pipeline_manager_instance->data->play_pipelines = g_try_malloc0(
            sizeof(ri_pipeline_t*) * numPlayPipelines);

    if (NULL == pipeline_manager_instance->data->play_pipelines)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    pipeline_manager_instance->data->num_play_pipelines = numPlayPipelines;
    for (i = 0; i < numPlayPipelines; i++)
    {
        ri_pipeline_t* pipeline = create_pipeline(
                RI_PIPELINE_TYPE_PLAYBACK_LOCAL);
        pipeline_manager_instance->data->play_pipelines[i] = pipeline;
    }

    // Allocate out-of-band pipeline
    pipeline_manager_instance->data->oob_pipeline = create_oob_pipeline();

    // Allocate out-of-band pipeline
    pipeline_manager_instance->data->dsg_pipeline = create_dsg_pipeline();

    // Allocate remote playback pipelines
    pipeline_manager_instance->data->hn_server_pipelines = g_try_malloc0(
            sizeof(ri_pipeline_t*) * numHNServerPipelines);

    if (NULL == pipeline_manager_instance->data->hn_server_pipelines)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    pipeline_manager_instance->data->num_hn_server_pipelines
            = numHNServerPipelines;
    for (i = 0; i < numHNServerPipelines; i++)
    {
        ri_pipeline_t* pipeline = create_pipeline(RI_PIPELINE_TYPE_HN_SERVER);
        pipeline_manager_instance->data->hn_server_pipelines[i] = pipeline;
    }

    // Allocate HN stream pipeline
    pipeline_manager_instance->data->hn_player_pipeline = create_pipeline(
            RI_PIPELINE_TYPE_HN_PLAYER);

    // Assign function pointers
    pipeline_manager_instance->get_oob_pipeline = get_oob_pipeline;
    pipeline_manager_instance->get_dsg_pipeline = get_dsg_pipeline;
    pipeline_manager_instance->get_live_pipelines = get_live_pipelines;
    pipeline_manager_instance->get_playback_pipelines = get_playback_pipelines;
    pipeline_manager_instance->get_display = get_display;
    pipeline_manager_instance->get_hn_server_pipelines
            = get_hn_server_pipelines;
    pipeline_manager_instance->get_hn_player_pipeline = get_hn_player_pipeline;

    //g_timeout_add(10000, test_decode_switch, (gpointer)pipeline_manager_instance);
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

ri_pipeline_manager_t* ri_get_pipeline_manager(void)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);
    if (pipeline_manager_instance == NULL)
    {
        create_pipeline_manager();
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);

    return pipeline_manager_instance;
}

/**
 * Attaches the one and only display to the supplied pipeline, detaching
 * device if currently attached to another pipeline.
 *
 * @param   manager of pipeline
 * @param   pipeline to receive video device
 */
void attach_video_device_to_pipeline(ri_pipeline_manager_t* pipeline_manager,
        ri_pipeline_t* pipeline)
{
    int i;

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    // search all of the pipelines to see if the video device is
    //  currently attached

    // first search the 'live' pipelines
    for (i = 0; i < pipeline_manager_instance->data->num_live_pipelines; i++)
    {
        // if this pipeline has the video device...
        if (has_video_device(pipeline_manager_instance->data->live_pipelines[i]))
        {
            // ... detach it
            (void) detach_video_device(
                    pipeline_manager_instance->data->live_pipelines[i]);
        }
    }

    // now search the 'playback' pipelines
    for (i = 0; i < pipeline_manager_instance->data->num_play_pipelines; i++)
    {
        // if this pipeline has the video device...
        if (has_video_device(pipeline_manager_instance->data->play_pipelines[i]))
        {
            // ... detach it
            (void) detach_video_device(
                    pipeline_manager_instance->data->play_pipelines[i]);
        }
    }

    // Get the video device from display
    ri_video_device_t* video_device =
            pipeline_manager_instance->data->display->get_video_device(
                    pipeline_manager_instance->data->display);

    // Attach it to supplied pipeline
    (void) attach_video_device(pipeline, video_device);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

gboolean test_decode_switch(gpointer object)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    // Look at each pipeline to determine which one has the associated decode bin
    ri_pipeline_t* pipeline_0 =
            pipeline_manager_instance->data->live_pipelines[0];
    //ri_pipeline_t* pipeline_1 = pipeline_manager_instance->data->live_pipelines[1];

    if (has_video_device(pipeline_0))
    {
        RILOG_INFO("%s -- pipeline 0 has decode bin\n", __FUNCTION__);

        // Unlink pipeline 0
        /*
         decode_bin_detach(pipeline_0,
         pipeline_manager_instance->data->decode_display);

         // Link to pipeline 1
         decode_bin_attach(pipeline_1,
         pipeline_manager_instance->data->decode_display);
         */
    }
    else
    {
        RILOG_INFO("%s -- pipeline 0 does not have decode bin\n", __FUNCTION__);

        /*
         if (has_decode_bin(pipeline_1))
         {
         // Unlink pipeline 1
         decode_bin_detach(pipeline_1,
         pipeline_manager_instance->data->decode_display);
         }

         // Link pipeline 0
         decode_bin_attach(pipeline_0,
         pipeline_manager_instance->data->decode_display);
         */
    }
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return TRUE;
}

void destroy_pipeline_manager()
{
    //int i;
    RILOG_TRACE("%s() -- Entry\n", __FUNCTION__);

    if (pipeline_manager_instance == NULL)
    {
        return;
    }

    // delete out-of-band pipeline
    destroy_oob_pipeline(pipeline_manager_instance->data->oob_pipeline);

    // delete out-of-band pipeline
    destroy_dsg_pipeline(pipeline_manager_instance->data->dsg_pipeline);
#if 0
    // Delete live pipelines
    for (i = 0; i < pipeline_manager_instance->data->num_live_pipelines; i++)
    {
        destroy_pipeline(pipeline_manager_instance->data->live_pipelines[i]);
    }

    // Delete playback pipelines
    for (i = 0; i < pipeline_manager_instance->data->num_play_pipelines; i++)
    {
        destroy_pipeline(pipeline_manager_instance->data->play_pipelines[i]);
    }

    g_free(pipeline_manager_instance->data->play_pipelines);
    g_free(pipeline_manager_instance->data->live_pipelines);
    g_free(pipeline_manager_instance->data);
    g_free(pipeline_manager_instance);
#endif
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}
