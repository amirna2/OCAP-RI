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

#include <ri_display.h>
#include <ri_video_device.h>
#include "video_device.h"
#include <ri_log.h>
#include "gst_utils.h"
#include "display.h"
#include "pipeline.h"
#include <glib/gprintf.h>
#include <stdlib.h>
#include <ri_config.h>


#define RILOG_CATEGORY videoDeviceCategory
log4c_category_t* videoDeviceCategory = NULL;

#define CHECK_OK_OBJECT(o,x,m) if (!(x)) { GST_ERROR_OBJECT((o), (m)); }

#define EVENT_RECEIVE_TIMEOUT_MS 2500

#undef SEND_VPOP_OUTPUT_TO_FILE

struct ri_video_device_data_s
{
    GStaticRecMutex video_device_mutex; // Recursive mutex to protect video device

    GstElement* decode_bin; // Bin for the Video Player elements

    GstElement* decodetee; // Tee to feed different decode legs
    GstElement* passthru;

    GstElement* audio_pid; // Audio pid filter for audio decode leg of decoder
    GstElement* audio_es;  // Audio elementary stream assembler for audio decode leg
    GstElement* audiosink; // Audio display element
    GstElement* ap_bin;    // Bin for the Audio Player elements

    GstElement* video_pid; // Video pid filter for video decode leg of decoder
    GstElement* video_es;  // Video elementary stream assembler for video decode leg
    GstElement* videosink; // Video display element
    GstElement* vp_bin;    // Bin for the Video Player elements

    GstElement* vpopsink;  // VPOP display element
    GstElement* sptsassembler;  // VPOP SPTS assembler element
    GstElement* vpop_bin;  // Bin for the VPOP elements

    GstElement* mpegdecoder; // MPEG2 video decoder
    GstElement* queue5;
    GstElement* queue6;
    GstElement* queue7;
    GstElement* queue8;
    GstElement* queue9;
    GstElement* queue10;

    gboolean eventReceived; // flag which indicates event has been received decode bin elements
    GCond*   event_cond; // Condition data structure for notification from GStreamer
    GMutex*  event_mutex; // Mutex to protect the flag denoting notification from GStreamer

    guint videosink_probe; // handler id of event probe on videosink pad
};

static gboolean decode_bin_attach(GstElement* gst_pipeline,
        GstElement* decode_bin, GstPad* pipeline_src_pad,
        GstPad* decode_bin_sink_pad);

static gboolean decode_bin_detach(GstElement* gst_pipeline,
        GstElement* decode_bin, GstPad* pipeline_src_pad,
        GstPad* decode_bin_sink_pad, ri_video_device_t* video_device,
        GstState curState);

static void decode_bin_input_block(gboolean block, GstElement* gst_pipeline,
        GstPad* pipeline_src_pad, ri_video_device_t* video_device);

void decode_bin_block_cb(GstPad *pad, gboolean blocked, gpointer user_data);

void decode_bin_status(ri_video_device_t* video_device);

static void decode_bin_send_event(ri_video_device_t* video_device,
        GstEvent* event);

static gboolean videosink_event_probe(GstPad *pad, GstEvent *event,
        gpointer u_data);

/**
 * Creates the video device
 *
 * @return Returns the created device
 */
ri_video_device_t* create_video_device()
{
    videoDeviceCategory = log4c_category_get("RI.Display.VideoDevice");

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    ri_video_device_t* video_device;
    gboolean convert_vpop_output_to_spts = FALSE;

    video_device = g_try_malloc(sizeof(ri_video_device_t));
    if (video_device)
    {
        memset(video_device, 0, sizeof(ri_video_device_t));
        video_device->vpop_get_buffer = vpop_get_buffer;
        video_device->vpop_free_buffer = vpop_free_buffer;
        video_device->vpop_flow_starting = vpop_flow_starting;
        video_device->vpop_flow_stopping = vpop_flow_stopping;
        video_device->data = g_try_malloc(sizeof(ri_video_device_data_t));
        if (video_device->data)
        {
            memset(video_device->data, 0, sizeof(ri_video_device_data_t));

            // Initialize the mutex
            g_static_rec_mutex_init(&(video_device->data->video_device_mutex));

            // Load elements of decode display
            video_device->data->decodetee
                    = gst_load_element("tee", "decodetee");
            video_device->data->passthru = gst_load_element("passthru",
                    "decode_bin_passthru");
            video_device->data->video_pid = gst_load_element("pidfilter",
                    "videopid");
            video_device->data->audio_pid = gst_load_element("pidfilter",
                    "audiopid");
            video_device->data->video_es = gst_load_element("esassembler",
                    "video_es");
            video_device->data->audio_es = gst_load_element("esassembler",
                    "audio_es");
            video_device->data->mpegdecoder = gst_load_element("mpegdecoder",
                    "mpegdecoder");
            video_device->data->queue5 = gst_load_element("queue", "queue5");
            video_device->data->queue6 = gst_load_element("queue", "queue6");
            video_device->data->queue7 = gst_load_element("queue", "queue7");
            video_device->data->queue8 = gst_load_element("queue", "queue8");
            video_device->data->queue9 = gst_load_element("queue", "queue9");
            video_device->data->queue10 = gst_load_element("queue", "queue10");
            video_device->data->videosink = gst_load_element("display",
                    "videosink");
            video_device->data->audiosink = gst_load_element("fakesink",
                    "audiosink");
            video_device->data->sptsassembler = gst_load_element(
                    "sptsassembler", "vpop-sptsassembler");
#ifdef SEND_VPOP_OUTPUT_TO_FILE
            video_device->data->vpopsink = gst_load_element("filesink",
                    "vpopsink");
            g_object_set(video_device->data->vpopsink, "location",
                    "vpop.mpg", NULL);
#else
            video_device->data->vpopsink = gst_load_element("appsink",
                    "vpopsink");
            GstAppSink *sink = (GstAppSink*)video_device->data->vpopsink;
            int maxVpopBuffers = 5;
            char* cfgVal = NULL;

            if ((cfgVal = ricfg_getValue("RIPlatform",
                    "RI.Platform.display.vpop.maxbuffers")) != NULL)
            {
                if (NULL != cfgVal)
                {
                    maxVpopBuffers = atoi(cfgVal);
                    RILOG_INFO("%s -- set VPOP maxbuffers to %d\n",
                               __FUNCTION__, maxVpopBuffers);
                }
            }

            gst_app_sink_set_max_buffers(sink, maxVpopBuffers);
            gst_app_sink_set_drop(sink, TRUE);
#endif
            convert_vpop_output_to_spts = ricfg_getBoolValue("RIPlatform",
                                "RI.Platform.display.vpop.convert_to_spts");
            if (convert_vpop_output_to_spts)
            {
                RILOG_INFO("%s -- convert VPOP output to SPTS\n", __func__);
                g_object_set(G_OBJECT(video_device->data->sptsassembler),
                             "zero_prgm_pats",
                             ricfg_getBoolValue("RIPlatform",
                             "RI.Platform.display.vpop.zeroPrgmPatInsertion"),
                             "insert_canned_pat_pmt", TRUE, NULL);
            }
            else
            {
                RILOG_INFO("%s -- do NOT convert VPOP output\n", __func__);
            }

            // Initialise the Conditional data structure and Mutex used for GStreamer notifications
            video_device->data->event_cond = g_cond_new();
            video_device->data->event_mutex = g_mutex_new();

            // Clear out the PID list on the preprocessor and video pid since stack will be setting PID via decode() and other methods
            // If we don't do this the pid filters pass everything.
            GValue prop =
            { 0, };
            (void) g_value_init(&prop, G_TYPE_STRING);
            g_value_set_string(&prop, " ");
            g_object_set_property(G_OBJECT(video_device->data->video_pid),
                    "pidlist", &prop);
            g_object_set_property(G_OBJECT(video_device->data->audio_pid),
                    "pidlist", &prop);
            g_value_unset(&prop);

            g_object_set(video_device->data->videosink, "async", FALSE, NULL);
            g_object_set(video_device->data->audiosink, "async", FALSE, NULL);
            g_object_set(video_device->data->vpopsink, "async", FALSE, NULL);

            // prevent the VPOP appsink from synchronizing on timestamps
            g_object_set(video_device->data->vpopsink, "sync", FALSE, NULL);

            // we are currently setting Queue 10 to be leaky so old buffers are
            // dropped when the HN client is not connected to the server.
            g_object_set(video_device->data->queue10, "leaky", 2, NULL);

            // Determine if the low quality decoder should be used based on config parameter
            // Low quality decode should be used on slow PCs to limit CPU usage
            g_object_set(G_OBJECT(video_device->data->mpegdecoder),
                         "low-quality-decode",
                         ricfg_getBoolValue("RIPlatform",
                               "RI.Platform.display.low_quality_decode"), NULL);
            // Video player part of pipeline
            video_device->data->vp_bin = gst_bin_new("vp_bin");

            gst_bin_add_many(GST_BIN(video_device->data->vp_bin),
                    video_device->data->passthru,
                    video_device->data->video_pid,
                    video_device->data->video_es,
                    video_device->data->mpegdecoder,
                    video_device->data->videosink, video_device->data->queue5,
                    video_device->data->queue6, video_device->data->queue7,
                    NULL);

            // Ghost the input pad to the bin
            GstPad* vp_bin_pad = gst_element_get_static_pad(
                    video_device->data->queue5, "sink");
            CHECK_OK_OBJECT(video_device->data->vp_bin,
                    gst_element_add_pad(video_device->data->vp_bin,
                    gst_ghost_pad_new("sink", vp_bin_pad)),
                    "Adding ghost pad to vp_bin failed");

            if (NULL != vp_bin_pad)
            {
                gst_object_unref(GST_OBJECT(vp_bin_pad));
            }

            // Link the video player bin elements
            CHECK_OK_OBJECT(video_device->data->vp_bin,
                    gst_element_link_many(video_device->data->queue5,
                            video_device->data->passthru,
                            video_device->data->video_pid,
                            video_device->data->video_es,
                            video_device->data->queue6,
                            video_device->data->mpegdecoder,
                            video_device->data->queue7,
                            video_device->data->videosink,
                            NULL),
                    "Linking vp_bin elements failed");

            // Audio player part of pipeline
            video_device->data->ap_bin = gst_bin_new("ap_bin");

            gst_bin_add_many(GST_BIN(video_device->data->ap_bin),
                    video_device->data->queue8,
                    video_device->data->audio_pid,
                    video_device->data->audio_es,
                    video_device->data->queue9,
                    video_device->data->audiosink,
                    NULL);

            // Ghost the input pad to the bin
            GstPad* ap_bin_pad = gst_element_get_static_pad(
                    video_device->data->queue8, "sink");
            CHECK_OK_OBJECT(video_device->data->ap_bin,
                    gst_element_add_pad(video_device->data->ap_bin,
                        gst_ghost_pad_new("sink", ap_bin_pad)),
                        "Adding ghost pad to ap_bin failed");

            if (NULL != ap_bin_pad)
            {
                gst_object_unref(GST_OBJECT(ap_bin_pad));
            }

            // Link the audio player bin elements
            CHECK_OK_OBJECT(video_device->data->ap_bin,
                    gst_element_link_many(video_device->data->queue8,
                            video_device->data->audio_pid,
                            video_device->data->audio_es,
                            video_device->data->queue9,
                            video_device->data->audiosink,
                            NULL),
                    "Linking ap_bin elements failed");

            // VPOP part of pipeline
            video_device->data->vpop_bin = gst_bin_new("vpop_bin");

            if (TRUE == convert_vpop_output_to_spts)
            {
                gst_bin_add_many(GST_BIN(video_device->data->vpop_bin),
                                         video_device->data->queue10,
                                         video_device->data->sptsassembler,
                                         video_device->data->vpopsink,
                                         NULL);
            }
            else
            {
                gst_bin_add_many(GST_BIN(video_device->data->vpop_bin),
                                         video_device->data->queue10,
                                         video_device->data->vpopsink,
                                         NULL);
            }

            // Ghost the input pad to the bin
            GstPad* vpop_bin_pad = gst_element_get_static_pad(
                    video_device->data->queue10, "sink");
            CHECK_OK_OBJECT(video_device->data->vpop_bin,
                    gst_element_add_pad(video_device->data->vpop_bin,
                    gst_ghost_pad_new("sink", vpop_bin_pad)),
                    "Adding ghost pad to vpop_bin failed");

            if (NULL != vpop_bin_pad)
            {
                gst_object_unref(GST_OBJECT(vpop_bin_pad));
            }

            // Link the VPOP bin elements
            if (TRUE == convert_vpop_output_to_spts)
            {
                CHECK_OK_OBJECT(video_device->data->vpop_bin,
                    gst_element_link_many(video_device->data->queue10,
                                          video_device->data->sptsassembler,
                                          video_device->data->vpopsink,
                                          NULL),
                                          "Linking vpop_bin elements failed");
            }
            else
            {
                CHECK_OK_OBJECT(video_device->data->vpop_bin,
                    gst_element_link_many(video_device->data->queue10,
                                          video_device->data->vpopsink,
                                          NULL),
                                          "Linking vpop_bin elements failed");
            }

            // Create the overall decode bin
            video_device->data->decode_bin = gst_bin_new("decode_bin");

            // Add the video, audio, and VPOP bins and decode tee to decode bin
            gst_bin_add_many(GST_BIN(video_device->data->decode_bin),
                             video_device->data->decodetee,
                             video_device->data->vp_bin,
                             video_device->data->ap_bin,
                             video_device->data->vpop_bin,
                             NULL);

            // Ghost the input pad to the decode bin
            GstPad* decode_bin_sink_pad = gst_element_get_static_pad(
                    video_device->data->decodetee, "sink");
            CHECK_OK_OBJECT(video_device->data->decode_bin,
                    gst_element_add_pad(video_device->data->decode_bin,
                    gst_ghost_pad_new("sink", decode_bin_sink_pad)),
                    "Adding ghost pad to decode_bin failed");

            // Link the video to the decode bin elements
            CHECK_OK_OBJECT(video_device->data->decode_bin,
                    gst_element_link_many(video_device->data->decodetee,
                            video_device->data->vp_bin,
                            NULL),
                    "Linking video to decode_bin failed");

            // Now link the audio to the decode bin elements
            CHECK_OK_OBJECT(video_device->data->decode_bin,
                    gst_element_link_many(video_device->data->decodetee,
                            video_device->data->ap_bin,
                            NULL),
                    "Linking audio to decode_bin failed");

            // Link the vpop to the decode bin elements
            CHECK_OK_OBJECT(video_device->data->decode_bin,
                    gst_element_link_many(video_device->data->decodetee,
                            video_device->data->vpop_bin,
                            NULL),
                    "Linking VPOP to decode_bin failed");

            // Set the decode bin state to playing
            (void) gst_element_set_state(video_device->data->decode_bin,
                    GST_STATE_PLAYING);

            // Hang on to a reference to the decode bin so that when it is not
            // destroyed when detached (momentarily) from all pipelines
            (void) gst_object_ref(video_device->data->decode_bin);

            RILOG_INFO("%s -- current videoDevice %p, get:%p, free%p\n",
                       __FUNCTION__, video_device,
                       video_device->vpop_get_buffer,
                       video_device->vpop_free_buffer);
        }
        else
        {
            g_free(video_device);
            RILOG_ERROR("%s -- g_try_malloc of data failed\n", __FUNCTION__);
            video_device = NULL;
        }
    }
    else
    {
        RILOG_ERROR("%s -- g_try_malloc failed\n", __FUNCTION__);
        video_device = NULL;
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return video_device;
}

/**
 * Sets the state of the video device's decode bin gstreamer elements to playing.
 *
 * @param   video_device   owner of gstreamer decode bin elements
 */
void video_device_set_playing(ri_video_device_t* video_device)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);
    (void) gst_element_set_state(video_device->data->decode_bin,
            GST_STATE_PLAYING);
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Sets the state of the video device's decode bin gstreamer elements to playing.
 *
 * @param   video_device   owner of gstreamer decode bin elements
 * @param   rate           desired play back speed rate
 */
void video_device_set_rate(ri_video_device_t* video_device, float rate)
{
    RILOG_INFO("%s -- Entry\n", __FUNCTION__);

    // Set the playrate property of the ESAssembler in the video device bin
    g_object_set(G_OBJECT(video_device->data->video_es), "playrate", rate, NULL);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Set the pid list for this video device pid filter
 */
void set_video_device_pidlist(ri_video_device_t* video_device, uint32_t pid)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    // Make sure the args are valid
    if (NULL != video_device)
    {
        if (NULL != video_device->data)
        {
            if (NULL != video_device->data->video_pid)
            {
                gchar pidlist[16];
                GValue prop =
                { 0, };
                (void) g_value_init(&prop, G_TYPE_STRING);
                sprintf(pidlist, "0x%4.4X", pid);
                g_value_set_string(&prop, pidlist);
                g_object_set_property(G_OBJECT(video_device->data->video_pid),
                        "pidlist", &prop);
                g_value_unset(&prop);
            }
            else
            {
                RILOG_WARN(
                        "%s unable to set video pid list - video pid is NULL\n",
                        __FUNCTION__);
            }
        }
        else
        {
            RILOG_WARN(
                    "%s unable to set video pid list - device data is NULL\n",
                    __FUNCTION__);
        }
    }
    else
    {
        RILOG_WARN(
                "%s unable to set video pid list - video device is NULL\n",
                __FUNCTION__);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Set the pid list for this audio device pid filter
 */
void set_audio_device_pidlist(ri_video_device_t* video_device, uint32_t pid)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    // Make sure the args are valid
    if (NULL != video_device)
    {
        if (NULL != video_device->data)
        {
            if (NULL != video_device->data->audio_pid)
            {
                gchar pidlist[16];
                GValue prop =
                { 0, };
                (void) g_value_init(&prop, G_TYPE_STRING);
                sprintf(pidlist, "0x%4.4X", pid);
                g_value_set_string(&prop, pidlist);
                g_object_set_property(G_OBJECT(video_device->data->audio_pid),
                        "pidlist", &prop);
                g_value_unset(&prop);
            }
            else
            {
                RILOG_WARN(
                        "%s unable to set audio pid list - audio pid is NULL\n",
                        __FUNCTION__);
            }
        }
        else
        {
            RILOG_WARN(
                    "%s unable to set audio pid list - device data is NULL\n",
                    __FUNCTION__);
        }
    }
    else
    {
        RILOG_WARN(
                "%s unable to set audio pid list - video device is NULL\n",
                __FUNCTION__);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Returns vpop sink element
 */
GstElement* get_vpop_sink_element(ri_video_device_t* video_device)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);
    GstElement* retVal = video_device->data->vpopsink;
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return retVal;
}

/**
 * Returns video sink element
 */
GstElement* get_video_sink_element(ri_video_device_t* video_device)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);
    GstElement* retVal = video_device->data->videosink;
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return retVal;
}

/*
 * The logic used in decode_bin_modify_link() follows the suggested logic
 * in the gstreamer design documentation - part-block.txt and is as follows:
 *
 Dynamically switching an element in a PLAYING pipeline.


 .----------.      .----------.      .----------.
 | element1 |      | element2 |      | element3 |
 ...        src -> sink       src -> sink       ...
 '----------'      '----------'      '----------'
 .----------.
 | element4 |
 sink       src
 '----------'

 The purpose is to replace element2 with element4 in the PLAYING
 pipeline.

 1) block element1 src pad. This can be done async.
 2) wait for block to happen. at that point nothing is flowing between
 element1 and element2 and nothing will flow until unblocked.
 3) unlink element1 and element2
 4) optional step: make sure data is flushed out of element2:
 4a) pad event probe on element2 src
 4b) send EOS to element2, this makes sure that element2 flushes
 out the last bits of data it holds.
 4c) wait for EOS to appear in the probe, drop the EOS.
 4d) remove the EOS pad event probe.
 5) unlink element2 and element3
 5a) optionally element2 can now be set to NULL and/or removed from the
 pipeline.
 6) link element4 and element3
 7) link element1 and element4 (FIXME, how about letting element4 know
 about the currently running segment?, see issues.)
 8) make sure element4 is in the same state as the rest of the elements. The
 element should at least be PAUSED.
 9) unblock element1 src

 The same flow can be used to replace an element in a PAUSED pipeline. Only
 special care has to be taken when performing step 2) which has to be done
 async or it might deadlock. In the async callback one can then perform the
 steps from 3). In a playing pipeline one can of course use the async block
 as well, so that there is a generic method for both PAUSED and PLAYING.

 The same flow works as well for any chain of multiple elements and might
 be implemented with a helper function in the future.


 Issues
 ------

 When an EOS event has passed a pad and the pad is set to blocked, the block will
 never happen because no data is going to flow anymore. One possibility is to
 keep track of the pad's EOS state and make the block succeed immediatly. This is
 not yet implemenented.

 When dynamically reconnecting pads, some events (like NEWSEGMENT, EOS,
 TAGS, ...) are not yet retransmitted to the newly connected element. It's
 unclear if this can be done by core automatically by caching those events and
 resending them on a relink. It might also be possible that this needs a
 GstFlowReturn value from the event function, in which case the implementation
 must be delayed for after 0.11, when we can break API/ABI.
 *
 */

/**
 * Either attaches or detaches the supplied decode bin to this pipeline
 * using the output src pad.
 *
 * @param   gst_pipeline   pipeline to modify
 * @param   video_device   attach or detach this video device
 * @param   attach         if true links or unlinks from pipeline successfully,
 *                         false if problems encountered
 *
 * @return  true if link was successfully modified, false otherwise
 */
gboolean decode_bin_modify_link(GstElement* gst_pipeline,
        ri_video_device_t* video_device, gboolean attach)
{
    gboolean ret = TRUE;

    GstState pendingState;
    GstStateChangeReturn rc;

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    char *name = gst_element_get_name(gst_pipeline);
    RILOG_INFO("%s -- %s video device to pipeline: %s\n", __FUNCTION__, TRUE
            == attach ? "Attaching" : "Detaching", name);
    g_free(name);

    // Get the mutex so we can manipulate the video device
    g_static_rec_mutex_lock(&(video_device->data->video_device_mutex));

    GstElement* decode_bin = video_device->data->decode_bin;

    // Get the pipeline's output bin since this is where the decode bin is attached
    GstElement* output_bin = gst_bin_get_by_name(GST_BIN(gst_pipeline),
            "output_bin");
    if (NULL == output_bin)
    {
        RILOG_ERROR("%s -- unable to get output bin\n", __FUNCTION__);
        g_static_rec_mutex_unlock(&(video_device->data->video_device_mutex));
        return FALSE;
    }

    // Get the pipeline src pad which is it's output bin src pad
    GstPad* pipeline_src_pad = gst_element_get_static_pad(output_bin, "src");
    if (NULL == pipeline_src_pad)
    {
        RILOG_ERROR("%s -- unable to get pipeline's output bin src pad\n",
                __FUNCTION__);
        g_static_rec_mutex_unlock(&(video_device->data->video_device_mutex));
        return FALSE;
    }

    // Get the decode bin sink pad
    GstPad* decode_bin_sink_pad =
            gst_element_get_static_pad(decode_bin, "sink");
    if (NULL == decode_bin_sink_pad)
    {
        RILOG_ERROR("%s -- unable to get decode bin sink pad\n", __FUNCTION__);
        g_static_rec_mutex_unlock(&(video_device->data->video_device_mutex));
        return FALSE;
    }

    // Utility method to print out status of decode bin
    decode_bin_status(video_device);

    // *TODO* - better way to determine if data is flowing???
    GstFormat format = GST_FORMAT_DEFAULT;
    gboolean isFlowing =
            gst_element_query_position(gst_pipeline, &format, NULL);
    RILOG_DEBUG("%s -- flow state %d\n", __FUNCTION__, isFlowing);

    // Get the current state of the pipeline's output bin with 100th sec timeout
    GstState curState;
    rc = gst_element_get_state(output_bin, &curState, &pendingState,
            100000000LL);
#ifndef PRODUCTION_BUILD
    char *curStateName = (char *)gst_element_state_get_name(curState);
    char *pendingStateName = (char *)gst_element_state_get_name(pendingState);
    char *returnStateName = (char *)gst_element_state_change_return_get_name(rc);
    RILOG_DEBUG(
            "%s -- cur pipeline output bin state %s, pending state %s, rc %s\n",
            __FUNCTION__, curStateName, pendingStateName, returnStateName);
#endif

    // If current state is playing and data is flowing, block the input src
    if ((GST_STATE_PLAYING == curState) && (isFlowing))
    {
        if (FALSE == gst_pad_is_blocked(pipeline_src_pad))
        {
            RILOG_DEBUG("%s -- blocking input\n", __FUNCTION__);
            decode_bin_input_block(TRUE, gst_pipeline, pipeline_src_pad,
                    video_device);
        }
    }
    else
    {
        RILOG_DEBUG(
                "%s -- not blocking input, curState %s, isLinked %d, isFlowing %d, attach %d\n",
                __FUNCTION__, curStateName,
                gst_pad_is_linked(pipeline_src_pad), isFlowing, attach);
    }

    // Either link or unlink decode bin depending on attach parameter supplied
    if (attach)
    {
        ret = decode_bin_attach(gst_pipeline, decode_bin, pipeline_src_pad,
                decode_bin_sink_pad);
    }
    else
    {
        ret = decode_bin_detach(gst_pipeline, decode_bin, pipeline_src_pad,
                decode_bin_sink_pad, video_device, curState);

        // Print out status after link
        //if (0)
        //{
        //   decode_bin_status(video_device);
        //}
    }

    // Unblock pipeline src pad if unblocked
    if (TRUE == gst_pad_is_blocked(pipeline_src_pad))
    {
        RILOG_DEBUG("%s -- unblocking input\n", __FUNCTION__);
        decode_bin_input_block(FALSE, gst_pipeline, pipeline_src_pad,
                video_device);
    }

    // gst_bin_get_by_name() refs the returned element, so we unref it here
    gst_object_unref(output_bin);
    gst_object_unref(pipeline_src_pad);
    gst_object_unref(decode_bin_sink_pad);

    // Finally release the mutex
    g_static_rec_mutex_unlock(&(video_device->data->video_device_mutex));

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);

    return ret;
}

/**
 * Either blocks or unblocks the supplied source pad.  Uses an async method
 * with a callback and waits for the src pad block state to change to the
 * desired state.
 *
 * @param   block             either TRUE to block src pad or FALSE to unblock
 * @param   gst_pipeline      object used for logging
 * @param   pipeline_src_pad  pad to block or unblock
 * @param   video_device      user data supplied to the callback function
 */
static void decode_bin_input_block(gboolean block, GstElement* gst_pipeline,
        GstPad* pipeline_src_pad, ri_video_device_t* video_device)
{
    RILOG_TRACE("%s -- Entry, current block state %d, desired state %d\n",
            __FUNCTION__, gst_pad_is_blocked(pipeline_src_pad), block);

    // Need this call when modifying elements within pipeline
    CHECK_OK_OBJECT(gst_pipeline,
            gst_pad_set_blocked_async(pipeline_src_pad, block,
                    decode_bin_block_cb, video_device),
            "Unable to block pipeline src pad");

    // Wait for the block to happen or timeout
    int maxCnt = 2;
    int curCnt = 0;
    while ((block != gst_pad_is_blocked(pipeline_src_pad)) && (curCnt < maxCnt))
    {
        curCnt++;

        // Sleep for a short time
        RILOG_DEBUG(
                "%s -- cnt %d - sleeping waiting for pad, current state %d, desired state %d\n",
                __FUNCTION__, curCnt, gst_pad_is_blocked(pipeline_src_pad),
                block);
        g_usleep(50000L);
    }

    // If out of the loop but flag is still set, report problems with blocking pad
    if (block != gst_pad_is_blocked(pipeline_src_pad))
    {
        RILOG_ERROR("%s -- src pad never in desired block state %d\n",
                __FUNCTION__, block);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Callback function associated with aync pad block method.
 * Currently it does nothing since checking the block state
 * of the actual pad rather than setting a flag appears to be more
 * reliable.
 *
 * @param   pad         pad with block state change
 * @param   blocked     current blocked state of pad
 * @param   user_data   video_device data structure is used here
 */
void decode_bin_block_cb(GstPad *pad, gboolean blocked, gpointer user_data)
{
    RILOG_TRACE("%s -- Entry called, setting current block state to %d\n",
            __FUNCTION__, blocked);
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Attaches the video device's decode bin to the supplied pipeline.
 *
 * @param   gst_pipeline         pipeline to attached to video device
 * @param   decode_bin           bin element of video device
 * @param   pipeline_src_pad     connection point for link on pipeline
 * @param   decode_bin_sink_pad  connection point for video device
 *
 * @return  true if successfully attached, false otherwise
 */
static gboolean decode_bin_attach(GstElement* gst_pipeline,
        GstElement* decode_bin, GstPad* pipeline_src_pad,
        GstPad* decode_bin_sink_pad)
{
    char *name = gst_element_get_name(gst_pipeline);
    RILOG_TRACE("%s -- Entry pipeline: %s\n", __FUNCTION__, name);
    g_free(name);

    // assume failure
    gboolean retVal = FALSE;

    // Decode bin needs to be added to the pipeline
    gst_bin_add_many(GST_BIN(gst_pipeline), decode_bin, NULL);

    // Link input bin src pad to decode bin sink
    if (GST_PAD_LINK_OK == gst_pad_link(pipeline_src_pad, decode_bin_sink_pad))
    {
        RILOG_DEBUG("%s -- successfully linked output and decode bins\n",
                __FUNCTION__);
        retVal = TRUE;
    }
    else
    {
        RILOG_ERROR("%s -- problems linking output and decode bin\n",
                __FUNCTION__);
    }

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, retVal);

    return retVal;
}

/**
 * Detaches the video device's decode bin to the supplied pipeline.
 *
 * @param   gst_pipeline         pipeline to attached to video device
 * @param   decode_bin           bin element of video device
 * @param   pipeline_src_pad     connection point for link on pipeline
 * @param   decode_bin_sink_pad  connection point for video device
 * @param   video_device         video device which needs to be flushed
 * @param   curState             current state of the decode bin
 *
 * @return  true if successfully detached, false otherwise
 */
static gboolean decode_bin_detach(GstElement* gst_pipeline,
        GstElement* decode_bin, GstPad* pipeline_src_pad,
        GstPad* decode_bin_sink_pad, ri_video_device_t* video_device,
        GstState curState)
{
    char *name = gst_element_get_name(gst_pipeline);
    RILOG_TRACE("%s -- Entry detaching decode bin from pipeline: %s\n",
            __FUNCTION__, name);
    g_free(name);

    // assume failure
    gboolean retVal = FALSE;

    // Unlink input bin src pad and decode bin sink
    if (TRUE == gst_pad_unlink(pipeline_src_pad, decode_bin_sink_pad))
    {
        RILOG_DEBUG("%s -- successfully unlinked input and decode bins\n",
                __FUNCTION__);

        // Flush out the decode bin only if decode bin is in playing state otherwise it stalls
        if (GST_STATE_PLAYING == curState)
        {
            decode_bin_flush(video_device, TRUE);
        }
        else
        {
            RILOG_ERROR("%s -- can't flush decode bin, not in playing state\n",
                    __FUNCTION__);
        }

        // Remove decode elements from pipeline bin
        if (TRUE == gst_bin_remove(GST_BIN(gst_pipeline), decode_bin))
        {
            RILOG_DEBUG(
                    "%s -- successfully removed decode bin from pipeline\n",
                    __FUNCTION__);

            // indicate success
            retVal = TRUE;
        }
        else
        {
            RILOG_ERROR("%s -- unable to remove decode bin from pipeline\n",
                    __FUNCTION__);
        }

        // While still attached, transition the bin to PAUSED state.
        // This is required so that upon re-attachment, the element goes through
        // the PAUSED -> PLAYING transition. This ensures that all of the
        // elements in the bin have their base_time reset to follow new pipeline clock.
        (void) gst_element_set_state(decode_bin, GST_STATE_PAUSED);
    }
    else
    {
        RILOG_ERROR("%s -- problems unlinking decode bin\n", __FUNCTION__);
    }

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, retVal);
    return retVal;
}

/**
 * Performs the actions necessary to flush out the decode bin.
 * This involves sending FLUSH_START, FLUSH_STOP, and EOS events.
 *
 * @param   video_device   data structure with elements in decode bin
 * @param   sendEOS        indicates if an EOS event should be sent as part of flush
 */
void decode_bin_flush(ri_video_device_t* video_device, gboolean sendEOS)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    // Get the mutex so we can manipulate the video device
    g_static_rec_mutex_lock(&(video_device->data->video_device_mutex));

    GstEvent* flush_start = NULL;
    GstEvent* flush_stop = NULL;
    GstEvent* eos_event = NULL;

    // Need to determine if the EOS event needs to be sent
    // Send the EOS event to elements in decode bin
    if (sendEOS)
    {
        // Set the video device pid list to empty so that no buffers will pass until
        // set to the desired list when re-attached and decode() is called.
        g_object_set(G_OBJECT(video_device->data->video_pid), "pidlist", " ",
                NULL);

        eos_event = gst_event_new_eos();
        decode_bin_send_event(video_device, eos_event);
    }

    flush_start = gst_event_new_flush_start();
    decode_bin_send_event(video_device, flush_start);

    // Send the FLUSH STOP event to elements in decode bin
    flush_stop = gst_event_new_flush_stop();
    decode_bin_send_event(video_device, flush_stop);

    // Release the mutext
    g_static_rec_mutex_unlock(&(video_device->data->video_device_mutex));

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Sends event to the sink pad of the decode bin which then propogates through
 * entire decode bin.  Sets up an event probe to prevent the EOS event flowing
 * through entire pipeline, restricts it just to the decode bin
 *
 * @param   video_device   data structure containing elements in decode bin
 * @param   event          sends the supplied event to the decode bin
 */
static void decode_bin_send_event(ri_video_device_t* video_device,
        GstEvent* event)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    // save a pointer to the event name since the gst_pad_send_event takes
    // ownership of the event, making it unavailable for later reference
    const gchar* pEventName = GST_EVENT_TYPE_NAME(event);

    // Get the sink pad on decode bin to send event on
    GstElement* decode_bin = video_device->data->decode_bin;
    GstPad* decode_bin_sink_pad =
            gst_element_get_static_pad(decode_bin, "sink");
    GstPad* videosink_pad = gst_element_get_static_pad(
            video_device->data->videosink, "sink");

    GTimeVal timeout = { 0, 0 };
    gboolean cond_signalled = TRUE;

    g_get_current_time(&timeout);
    g_time_val_add(&timeout, EVENT_RECEIVE_TIMEOUT_MS * 1000); // timeout is in msec, function expects usec

    // Initialize the flag to indicate event has not yet been received
    video_device->data->eventReceived = FALSE;

    // Add probe on video sink pad to monitor for EOS event
    video_device->data->videosink_probe = gst_pad_add_event_probe(
            videosink_pad, G_CALLBACK(videosink_event_probe), video_device);

    RILOG_DEBUG("%s -- sending event %s to decode bin\n", __FUNCTION__,
            pEventName);

    (void) gst_pad_send_event(decode_bin_sink_pad, event);

    g_mutex_lock(video_device->data->event_mutex);
    while (FALSE == video_device->data->eventReceived && TRUE == cond_signalled)
    {
        // Sleep for a short time
        RILOG_DEBUG("%s -- waiting for event %s\n", __FUNCTION__, pEventName);
        cond_signalled = g_cond_timed_wait(video_device->data->event_cond,
                   video_device->data->event_mutex, &timeout);
    }
    g_mutex_unlock(video_device->data->event_mutex);

    // If out of the loop but flag is still set, report problems with event
    if (FALSE == video_device->data->eventReceived)
    {
        RILOG_ERROR("%s -- decode bin event %s never received\n", __FUNCTION__,
                pEventName);
    }

    // Unref the objects which were ref'd through method calls
    gst_object_unref(decode_bin_sink_pad);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Callback method called when event is detected on videosink pad.
 * It is used to ensure videosink received the intended events related to
 * flushing.  It does not stop events in order for the basesink
 * to see the event.
 *
 * @param   pad      pad which received event
 * @param   event    new event received
 * @param   u_data   data supplied when probe was created, in this case
 *                   reference to video_device itself
 */
static gboolean videosink_event_probe(GstPad *pad, GstEvent *event,
        gpointer u_data)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    gboolean retVal = TRUE;

    ri_video_device_t* video_device = (ri_video_device_t*) u_data;
    switch GST_EVENT_TYPE(event)
    {
        case GST_EVENT_FLUSH_START:
        case GST_EVENT_FLUSH_STOP:
        case GST_EVENT_EOS:

        RILOG_DEBUG("%s -- got event %s, setting flag to true\n",
                __FUNCTION__, GST_EVENT_TYPE_NAME (event));

        // Clear flag to indicate event received if waiting for event
        if (FALSE == video_device->data->eventReceived)
        {
            g_mutex_lock(video_device->data->event_mutex);
            video_device->data->eventReceived = TRUE;
            g_cond_signal(video_device->data->event_cond);
            g_mutex_unlock(video_device->data->event_mutex);

            // Remove the event pad probe
            gst_pad_remove_event_probe(pad, video_device->data->videosink_probe);
        }
        // return true so event keeps on flowing
        // Videosink doesn't have to worry about the EOS event since its the last element
        // and the EOS event is downstream event
        break;

        default:
        // ignore any other events
        break;
    }

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, retVal);
    return retVal;
}

/**
 * Reports the number of buffers currently contained in the queues
 * and other data related to the decode bin
 *
 * @param   video_device   data structure with decode bin elements
 */
void decode_bin_status(ri_video_device_t* video_device)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    // Get the current state of the decode bin with 100th sec timeout
    GstState pendingState;
    GstStateChangeReturn rc;
    GstState curState;
    rc = gst_element_get_state(video_device->data->decode_bin, &curState,
            &pendingState, 100000000LL);
#ifndef PRODUCTION_BUILD
    char *curStateName = (char *)gst_element_state_get_name(curState);
    char *pendingStateName = (char *)gst_element_state_get_name(pendingState);
    char *returnStateName = (char *)gst_element_state_change_return_get_name(rc);
    RILOG_INFO("%s -- cur decode bin state %s, pending state %s, rc %s\n",
            __FUNCTION__, curStateName, pendingStateName, returnStateName);
#endif

    gint buffer_cnt = 0;
    g_object_get(G_OBJECT(video_device->data->queue5), "current-level-buffers",
            &buffer_cnt, NULL); // property list terminator
    RILOG_INFO("%s -- buffers in queue 5: %d\n", __FUNCTION__, buffer_cnt);

    g_object_get(G_OBJECT(video_device->data->queue6), "current-level-buffers",
            &buffer_cnt, NULL); // property list terminator
    RILOG_INFO("%s -- buffers in queue 6: %d\n", __FUNCTION__, buffer_cnt);

    g_object_get(G_OBJECT(video_device->data->queue7), "current-level-buffers",
            &buffer_cnt, NULL); // property list terminator
    RILOG_INFO("%s -- buffers in queue 7: %d\n", __FUNCTION__, buffer_cnt);

    g_object_get(G_OBJECT(video_device->data->queue8), "current-level-buffers",
            &buffer_cnt, NULL); // property list terminator
    RILOG_INFO("%s -- buffers in queue 8: %d\n", __FUNCTION__, buffer_cnt);

    g_object_get(G_OBJECT(video_device->data->queue9), "current-level-buffers",
            &buffer_cnt, NULL); // property list terminator
    RILOG_INFO("%s -- buffers in queue 9: %d\n", __FUNCTION__, buffer_cnt);

    g_object_get(G_OBJECT(video_device->data->queue10), "current-level-buffers",
            &buffer_cnt, NULL); // property list terminator
    RILOG_INFO("%s -- buffers in queue 10: %d\n", __FUNCTION__, buffer_cnt);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Sets the max buffers allowed in the queues within the decode bin to
 * the supplied value.  When the max number of buffers are present in a
 * queue, the queue blocks until room is allowed for the next buffer.
 * Smaller values reduce latency with pipelines.
 *
 * @param   video_device   set the queues within decode bin video device
 * @param   max_buffer_cnt max number of buffers allowed in a queue
 */
void decode_bin_set_queue_max_buffers(ri_video_device_t* video_device,
        guint max_buffer_cnt)
{
    RILOG_DEBUG("%s -- setting max buffers to %d\n", __FUNCTION__,
            max_buffer_cnt);
    g_object_set(G_OBJECT(video_device->data->queue5), "max-size-buffers",
            max_buffer_cnt, NULL);
    g_object_set(G_OBJECT(video_device->data->queue6), "max-size-buffers",
            max_buffer_cnt, NULL);
    g_object_set(G_OBJECT(video_device->data->queue7), "max-size-buffers",
            max_buffer_cnt, NULL);
}

/**
 * Returns buffer containing next set of data to send out on network for VPOP
 *
 * @param   pVideoDevice   associated video_device
 * @param   bufData     returning data to send out on network
 * @param   bufLen      amount of data contained in buffer
 *
 * @return  RI_ERROR_NONE           if no problems were encountered
 *          RI_ERROR_EOS            if no more buffers are left
 *          RI_ERROR_NO_PLAYBACK    if pipeline is not currently playing
 *          RI_ERROR_GENERAL        problems were encountered
 *
 * NOTE: buffers returned via this methods should be freed via pipeline_hn_server_free_buffer()
 */
ri_error vpop_get_buffer(ri_video_device_t* pVideoDevice,
                         void** bufData, uint32_t* bufLen)
{
    ri_error rc = RI_ERROR_NONE;

    GstAppSink *sink = (GstAppSink*) get_vpop_sink_element(pVideoDevice);
    GstBuffer* buffer = NULL;
    *bufLen = 0;
    *bufData = NULL;

    // Only attempt to get a buffer if not in EOS state
    if (!gst_app_sink_is_eos(sink))
    {
        // Get a pipeline buffer from the app sink which contains next buffer
        // of data to send out
        buffer = gst_app_sink_pull_buffer(sink);
    }
    else
    {
        RILOG_INFO("%s -- unable to pull buffer from appsink due to EOS\n",
                __FUNCTION__);
    }

    if (NULL != buffer)
    {
        *bufLen = GST_BUFFER_SIZE(buffer);
        RILOG_TRACE("%s - pulled buffer[%d] from appsink\n", __func__, *bufLen);

        // Allocate memory to return buffer data back up to mpeos layer
        *bufData = g_try_malloc(*bufLen);

        if (NULL == bufData)
        {
            RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                        __LINE__, __FILE__, __func__);
        }

        // copy out the desired data then release the GST buffer
        memcpy(*bufData, (char*) GST_BUFFER_DATA(buffer), *bufLen);
        gst_buffer_unref(buffer);
    }
    else // failed to get buffer
    {
        // Determine if sink is in playing state
        GstState state;
        (void) gst_element_get_state((GstElement*) sink, // element
                &state, // state
                NULL, // pending
                10000000000LL); // timeout(1 second = 10^9 nanoseconds)

        // Check to see if pipeline is EOS
        if (gst_app_sink_is_eos(sink))
        {
            // If the element is playing, then the pipeline is EOS
            if (GST_STATE_PLAYING == state)
            {
                RILOG_INFO("%s -- unable to get buffer - playing state but "
                           "EOS\n", __FUNCTION__);
                rc = RI_ERROR_EOS;
            }
            else
            {
                // Pipeline is not playing yet
                RILOG_INFO("%s -- unable to get buffer due to pipeline not "
                           "yet playing but EOS\n", __FUNCTION__);
                rc = RI_ERROR_NO_PLAYBACK;
            }
        }
        else
        {
            if (GST_STATE_PLAYING == state)
            {
                RILOG_ERROR("%s -- unable to get buffer from video device\n",
                            __FUNCTION__);

                rc = RI_ERROR_GENERAL;
            }
            else
            {
                // Pipeline is not playing yet
                RILOG_INFO("%s -- unable to get buffer due to pipeline not "
                           "yet playing\n", __FUNCTION__);
                rc = RI_ERROR_NO_PLAYBACK;
            }
        }
    }

    return rc;
}

/**
 * Frees memory associated with buffer which was allocated in vpop_get_buffer()
 *
 * @param   pVideoDevice   associated video_device
 * @param   bufData     buffer to free
 */
void vpop_free_buffer(ri_video_device_t* pVideoDevice, void* bufData)
{
    if (NULL != bufData)
    {
        uint32_t bufLen = GST_BUFFER_SIZE(bufData);
        RILOG_TRACE("%s -- freeing buffer[%d]\n", __FUNCTION__, bufLen);
        g_free(bufData);
    }
}

/**
 * called upon VPOP stream start
 *
 * @param   pVideoDevice   associated video_device
 */
void vpop_flow_starting(ri_video_device_t* pVideoDevice)
{
    GstAppSink *sink = (GstAppSink*) get_vpop_sink_element(pVideoDevice);

    RILOG_INFO("%s -- called\n", __FUNCTION__);
    g_object_set(sink, "log-drop", TRUE, NULL);
    gst_app_sink_set_drop(sink, FALSE);

    // set Queue 10 (in VPOP bin) NOT to be leaky so old buffers are
    // no longer dropped when the HN client is not keeping up to the server.
    g_object_set(pVideoDevice->data->queue10, "leaky", 0, NULL);
}

/**
 * called upon VPOP stream stop
 *
 * @param   pVideoDevice   associated video_device
 */
void vpop_flow_stopping(ri_video_device_t* pVideoDevice)
{
    GstAppSink *sink = (GstAppSink*) get_vpop_sink_element(pVideoDevice);

    RILOG_INFO("%s -- called\n", __FUNCTION__);
    g_object_set(sink, "log-drop", FALSE, NULL);
    gst_app_sink_set_drop(sink, TRUE);

    // set Queue 10 (in VPOP bin) to be leaky so old buffers are
    // dropped when the HN client is not connected to the server.
    g_object_set(pVideoDevice->data->queue10, "leaky", 2, NULL);
}

