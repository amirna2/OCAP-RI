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

#include <ri_pipeline.h>
#include <ri_log.h>
#include <gst/app/gstappsrc.h>
#include "gst_utils.h"
#include "pipeline.h"
#include "section_filter.h"
#include "video_device.h"

#define RILOG_CATEGORY pipeHNCCategory
log4c_category_t* pipeHNCCategory = NULL;

static uint64_t totBytes = 0;

// Timeout to received events sent in pipeline
#define EVENT_RECEIVE_TIMEOUT_MS 25

// ================
// Local prototypes
// ================
//
ri_error pipeline_hn_player_start(ri_pipeline_t* pPipeline,
        ri_hn_decode_callback_f decode_cb,
        ri_hn_need_data_callback_f need_data_cb, void* decode_data);
ri_error pipeline_hn_player_inject_data(ri_pipeline_t* pPipeline, char* buf,
        uint32_t numBytes, uint64_t nptNS);
ri_error pipeline_hn_player_pause(ri_pipeline_t* pPipeline);
ri_error pipeline_hn_player_resume(ri_pipeline_t* pPipeline);
ri_error pipeline_hn_player_stop(ri_pipeline_t* pPipeline);
ri_error pipeline_hn_player_set_rate(ri_pipeline_t* pPipeline,
        ri_video_device_t* video_device, float rate);

void pipeline_hn_player_destroy_cb(gpointer data);
gboolean pipeline_hn_player_seek_data(GstAppSrc* src, guint64 offset,
        gpointer user_data);
void pipeline_hn_player_enough_data(GstAppSrc* src, gpointer user_data);
void pipeline_hn_player_need_data(GstAppSrc* src, guint length,
        gpointer user_data);
static void pipeline_hn_player_flush(ri_pipeline_t* pPipeline);
static void pipeline_hn_player_send_event(ri_pipeline_t* pPipeline,
                                          GstEvent* event);
gboolean event_probe(GstPad *pad, GstEvent *event, gpointer u_data);

/**
 * Creates the necessary gstreamer elements to support HN Streaming
 * to the local device (vs. HN remote playback).
 *
 * @pipeline   pipeline which supports HN streaming
 *
 * @return  TRUE if pipeline is created without problems, FALSE if problems encountered
 */
gboolean pipeline_hn_player_create(ri_pipeline_t* pPipeline)
{
    if (NULL == pipeHNCCategory)
    {
        pipeHNCCategory = log4c_category_get("RI.Pipeline.HN");
    }

    RILOG_TRACE("%s -- Entry, pipeline = %s\n", __FUNCTION__,
                pPipeline->data->name);

    // Input src for HN stream is app src
    pPipeline->data->input = gst_load_element("appsrc", "input");
    pPipeline->data->passthru0 = gst_load_element("passthru", "passthru0");
    pPipeline->data->queue1 = gst_load_element("queue", "hn_q_tee");
    pPipeline->data->inputtee = gst_load_element("tee", "inputtee");
    pPipeline->data->queue2 = gst_load_element("queue", "hn_q_pidfilter");
    pPipeline->data->preproc = gst_load_element("pidfilter", "preproc");

    GstElement* filesink = gst_load_element("filesink", "filesink");

    // Create the section filtering bin
    create_ssbin(pPipeline);

    // Create the input bin for this pipeline
    pPipeline->data->input_bin = gst_bin_new("input_bin");
    pPipeline->data->event_cond = g_cond_new();
    pPipeline->data->event_mutex = g_mutex_new();

    // If all elements were created, add to bin and link
    if (pPipeline->data->input_bin && pPipeline->data->input
            && pPipeline->data->queue1 && pPipeline->data->preproc
            && pPipeline->data->queue2)
    {
        // Add all elements to input bin
        gst_bin_add_many(GST_BIN(pPipeline->data->input_bin),
                pPipeline->data->input, pPipeline->data->passthru0,
                pPipeline->data->queue1, pPipeline->data->inputtee,
                pPipeline->data->ss_bin, pPipeline->data->queue2,
                pPipeline->data->preproc,
                //filesink, // *TODO* - remove this
                NULL);

        // Add a src pad to input bin for support of connection of decode bin
        GstPad* input_bin_src_pad = gst_element_get_static_pad(
                pPipeline->data->preproc, "src");
        CHECK_OK_OBJECT(pPipeline->data->input_bin, gst_element_add_pad(
                pPipeline->data->input_bin, gst_ghost_pad_new("src",
                        input_bin_src_pad)),
                "Adding ghost src pad to input_bin failed");
        if (NULL != input_bin_src_pad)
        {
            gst_object_unref(GST_OBJECT(input_bin_src_pad));
        }

        // Link together the items that were added to the bin
        CHECK_OK_OBJECT(pPipeline->data->input_bin, gst_element_link_many(
                pPipeline->data->input, pPipeline->data->queue1,
                pPipeline->data->passthru0, pPipeline->data->inputtee,
                pPipeline->data->queue2, pPipeline->data->preproc, NULL),
                "Linking input_bin elements failed");

        // Add the section filter to the input bin
        CHECK_OK_OBJECT(pPipeline->data->input_bin, gst_element_link_many(
                pPipeline->data->inputtee, pPipeline->data->ss_bin, NULL),
                "Linking ss_bin to input failed");

        // Add the file sink for testing
        if (0)
        {
            CHECK_OK_OBJECT(pPipeline->data->input_bin, gst_element_link_many(
                    pPipeline->data->inputtee, filesink, NULL),
                    "Linking filesink to input failed");
        }
    }
    else
    {
        RILOG_ERROR(
                "%s -- problems creating at least one element for pipeline\n",
                __FUNCTION__);

        RILOG_TRACE("%s -- Exit, return value = FALSE\n", __FUNCTION__);
        return FALSE;
    }

    // Clear out the PID list on the preprocessor and video pid since stack will be setting
    // PID via decode() and other methods.  If we don't do this the pid filters pass everything.
    g_object_set(G_OBJECT(pPipeline->data->preproc), "pidlist", NO_PIDS, NULL);

    // Set up the appsrc to block and not drop packets
    g_object_set(G_OBJECT(pPipeline->data->input), "block", TRUE, NULL);

    guint64 maxBytes = 10000;
    g_object_set(G_OBJECT(pPipeline->data->input), "max-bytes", maxBytes, NULL);
    //g_object_set(G_OBJECT(pPipeline->data->input), "is-live", TRUE, NULL);

    g_object_set(G_OBJECT(filesink), "location", "pipeline_hn_player.txt", NULL);

    // Set the max buffers in the two queues in hn stream pipeline to 2 to reduce latency
    g_object_set(G_OBJECT(pPipeline->data->queue1), "max-size-buffers", 2, NULL);
    g_object_set(G_OBJECT(pPipeline->data->queue2), "max-size-buffers", 2, NULL);
    pPipeline->data->decode_queue_size = 2;

    pPipeline->pipeline_hn_player_start = pipeline_hn_player_start;
    pPipeline->pipeline_hn_player_pause = pipeline_hn_player_pause;
    pPipeline->pipeline_hn_player_resume = pipeline_hn_player_resume;
    pPipeline->pipeline_hn_player_stop = pipeline_hn_player_stop;
    pPipeline->pipeline_hn_player_set_rate = pipeline_hn_player_set_rate;
    pPipeline->pipeline_hn_player_inject_data = pipeline_hn_player_inject_data;
    pPipeline->set_decode_prog_num = set_decode_prog_num;
    pPipeline->decode = decode;
    pPipeline->get_section_filter = get_section_filter;

    RILOG_INFO("%s -- Exit, return value = TRUE\n", __FUNCTION__);
    return TRUE;
}

/**
 * Initiates a hn stream using the supplied player pipeline
 *
 * @param pPipeline     The "this" pointer
 * @param decode_cb     function to callback when PIDs have been discovered
 * @param need_data_cb  function pipeline calls when data is needed
 * @param player_data   reference to MPEOS hn player
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_STREAMING: Problems starting hn streaming pipeline
 */
ri_error pipeline_hn_player_start(ri_pipeline_t* pPipeline,
        ri_hn_decode_callback_f decode_cb,
        ri_hn_need_data_callback_f need_data_cb, void* player_data)
{
    // *TODO* - get rid of decode_cb parameter
    ri_error rc = RI_ERROR_NONE;

    if (NULL == pipeHNCCategory)
    {
        pipeHNCCategory = log4c_category_get("RI.Pipeline.HN");
    }

    RILOG_TRACE("%s -- Entry, pipeline = %s\n", __FUNCTION__,
                pPipeline->data->name);

    // Get the mutex so we can manipulate the pipeline
    g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));

    RILOG_INFO("%s -- Start HN remote streaming, pipeline = %s\n",
            __FUNCTION__, pPipeline->data->name);

    // Setup the callback so hn player pipeline can notify MPEOS layer is should
    // retrieve another buffer from the network socket
    if (NULL != need_data_cb)
    {
        pPipeline->data->hn_need_data_cb = need_data_cb;
        pPipeline->data->hn_cb_data = player_data;

        GstAppSrc *appsrc = GST_APP_SRC(pPipeline->data->input);

        pPipeline->data->appsrc_callbacks.need_data
                = pipeline_hn_player_need_data;
        pPipeline->data->appsrc_callbacks.enough_data
                = pipeline_hn_player_enough_data;
        pPipeline->data->appsrc_callbacks.seek_data
                = pipeline_hn_player_seek_data;

        // Set appsrc callback to this function
        gst_app_src_set_callbacks(appsrc, &pPipeline->data->appsrc_callbacks,
                pPipeline, pipeline_hn_player_destroy_cb);
    }

    totBytes = 0;

    // Start the pipeline
    start_pipeline(pPipeline);

    // Finally release the pipeline mutex
    g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, rc);
    return rc;
}

/**
 * Injects the supplied data received via HN stream into pipeline.
 *
 * @param object    The "this" pointer
 * @param buf       data to inject into pipeline
 * @param numBytes  size of data in bytes
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_STREAMING: Problems streaming data in pipeline
 */
ri_error pipeline_hn_player_inject_data(ri_pipeline_t* pPipeline, char* buf,
        uint32_t numBytes, uint64_t nptNS)
{
    ri_error rc = RI_ERROR_NONE;

    if (NULL == pipeHNCCategory)
    {
        pipeHNCCategory = log4c_category_get("RI.Pipeline.HN");
    }

    RILOG_TRACE("%s -- Entry, pipeline = %s\n", __FUNCTION__,
                pPipeline->data->name);

    totBytes += numBytes;

    // Get the mutex so we can manipulate the pipeline
    g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));

    GstAppSrc *appsrc = GST_APP_SRC(pPipeline->data->input);

    // Create a new GstBuffer with our stream data
    GstBuffer *gstbuf = gst_buffer_new_and_alloc(numBytes);
    memcpy(GST_BUFFER_DATA(gstbuf), buf, numBytes);

   // Timestamp buffer with supplied media time
    GST_BUFFER_TIMESTAMP(gstbuf) = nptNS;

    RILOG_TRACE("%s -- pushing buffer with timestamp: %llu\n",
            __FUNCTION__, GST_BUFFER_TIMESTAMP(gstbuf));

    // Send this buffer to the appsrc
    // Buffer will automatically be freed by pipeline when it is done with buffer
    (void) gst_app_src_push_buffer(appsrc, gstbuf);

    RILOG_TRACE("%s -- buffer pushed\n", __FUNCTION__);

    // Finally release the pipeline mutex
    g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, rc);
    return rc;
}

/**
 * Pause the output of data from hn stream / client pipeline.
 *
 * @param object    pipeline to pause
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_STREAMING: Problems streaming data in pipeline
 */
ri_error pipeline_hn_player_pause(ri_pipeline_t* pPipeline)
{
    ri_error rc = RI_ERROR_NONE;

    if (NULL == pipeHNCCategory)
    {
        pipeHNCCategory = log4c_category_get("RI.Pipeline.HN");
    }

    RILOG_TRACE("%s -- Entry, pipeline = %s\n", __FUNCTION__,
                pPipeline->data->name);

    // Get the mutex so we can manipulate the pipeline
    g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));

    // *TODO* - No pipeline actions required???
    // Leave the pipeline as is?  It will stop getting receive buffers
    //stop_pipeline(pPipeline);
    (void) gst_element_set_state(pPipeline->data->gst_pipeline,
            GST_STATE_PAUSED);

    // Finally release the pipeline mutex
    g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, rc);
    return rc;
}

/**
 * Resumes the output of data from hn stream / client pipeline.
 *
 * @param object    pipeline to resume
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_STREAMING: Problems streaming data in pipeline
 */
ri_error pipeline_hn_player_resume(ri_pipeline_t* pPipeline)
{
    ri_error rc = RI_ERROR_NONE;

    if (NULL == pipeHNCCategory)
    {
        pipeHNCCategory = log4c_category_get("RI.Pipeline.HN");
    }
    RILOG_TRACE("%s -- Entry, pipeline = %s, locking\n", __FUNCTION__,
                pPipeline->data->name);

    // Get the mutex so we can manipulate the pipeline
    g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));

    // *TODO* - Leave the pipeline as is?  It will begin to receive buffers again
    // Tell the file src to start sending buffers
    //start_pipeline(pPipeline);
    (void) gst_element_set_state(pPipeline->data->gst_pipeline,
            GST_STATE_PLAYING);

    // Finally release the pipeline mutex
    g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, rc);
    return rc;
}

/**
 * Stops the output of data from hn stream / client pipeline.
 *
 * @param object    pipeline to stop
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_STREAMING: Problems streaming data in pipeline
 */
ri_error pipeline_hn_player_stop(ri_pipeline_t* pPipeline)
{
    ri_error rc = RI_ERROR_NONE;

    if (NULL == pipeHNCCategory)
    {
        pipeHNCCategory = log4c_category_get("RI.Pipeline.HN");
    }

    RILOG_TRACE("%s -- Entry, pipeline = %s, locking\n", __FUNCTION__,
                pPipeline->data->name);

    // Get the mutex so we can manipulate the pipeline
    g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));

    RILOG_INFO("%s -- Stopping HN remote streaming, pipeline = %s\n",
            __FUNCTION__, pPipeline->data->name);

    // Send EOS and flush pipeline to make sure there are no buffers in pipeline
    pipeline_hn_player_flush(pPipeline);

    // Tell the file src to stop sending buffers which will also detach video device
    stop_pipeline(pPipeline);

    // Reset total byte count
    totBytes = 0;

    // Finally release the pipeline mutex
    g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, rc);
    return rc;
}

/**
 * Performs necessary actions to reset pipeline after EOS is received or
 * if playback of current request has completed or has been stopped.
 * 
 * @param   pPipeline   specified pipeline to reset
 */
static void pipeline_hn_player_flush(ri_pipeline_t* pPipeline)
{
    RILOG_INFO("%s -- Entry\n", __FUNCTION__);

    // Get the mutex so we can manipulate the pipeline
    g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));

    GstEvent* flush_start = NULL;
    GstEvent* flush_stop = NULL;

    // Need to notify app src of end of stream
    gst_app_src_end_of_stream((GstAppSrc*)pPipeline->data->input);

    // Send the FLUSH START & STOP event to elements in pipeline
    flush_start = gst_event_new_flush_start();
    pipeline_hn_player_send_event(pPipeline, flush_start);

    flush_stop = gst_event_new_flush_stop();
    pipeline_hn_player_send_event(pPipeline, flush_stop);

    // Release the mutex
    g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Utility method which sends events through HN Player pipeline.
 * Verifies event is received at end and logs error is not received.
 *
 * @param   pPipeline   send event through this supplied pipeline
 * @param   event       event to send through pipeline
 */
static void pipeline_hn_player_send_event(ri_pipeline_t* pPipeline,
                                          GstEvent* event)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    // Save a pointer to the event name since the gst_pad_send_event takes
    // ownership of the event, making it unavailable for later reference
    const gchar* pEventName = GST_EVENT_TYPE_NAME(event);

    // Get the sink pad on passthru element to send event on, can't send through appsrc
    GstElement* passthru = pPipeline->data->passthru0;
    GstPad* start_pad = gst_element_get_static_pad(passthru, "sink");

    // Get the sink pad for entire HN player pipeline, will be queue if video device not attached
    GstElement* input_element = pPipeline->data->queue1;
    GstPad* sink_pad = gst_element_get_static_pad(input_element, "sink");

    // If pipeline has video device, use it as sink pad otherwise use queue
    if (has_video_device(pPipeline))
    {
        ri_video_device_t* video_device = pPipeline->data->video_device;
        GstElement* videosink = get_video_sink_element(video_device);
        sink_pad = gst_element_get_static_pad(videosink, "sink");
        RILOG_INFO("%s -- sending event (%s) through decode bin since attached to pipeline\n",
                __FUNCTION__, pEventName);
    }

    if (start_pad == NULL)
    {
        RILOG_ERROR("%s -- unable to send event since source pad is NULL\n", __FUNCTION__);
        return;
    }
    if (sink_pad == NULL)
    {
        RILOG_ERROR("%s -- unable to send event since destination pad is NULL\n", __FUNCTION__);
        return;
    }

    GTimeVal timeout = { 0, 0 };
    gboolean cond_signalled = TRUE;
    g_get_current_time(&timeout);
    g_time_val_add(&timeout, EVENT_RECEIVE_TIMEOUT_MS * 1000); // timeout is in msec, function expects usec

    // Initialize the flag to indicate event has not yet been received
    pPipeline->data->eventReceived = FALSE;

    // Add probe on sink pad to monitor for event
    pPipeline->data->event_probe = gst_pad_add_event_probe(
            sink_pad, G_CALLBACK(event_probe), pPipeline);

    RILOG_INFO("%s -- sending event %s through pipeline\n", __FUNCTION__,
            pEventName);

    (void)gst_pad_send_event(start_pad, event);

    g_mutex_lock(pPipeline->data->event_mutex);
    while (FALSE == pPipeline->data->eventReceived && TRUE == cond_signalled)
    {
        // Sleep for a short time
        RILOG_DEBUG("%s -- waiting for event %s\n", __FUNCTION__, pEventName);
        cond_signalled = g_cond_timed_wait(pPipeline->data->event_cond,
                pPipeline->data->event_mutex, &timeout);
    }
    g_mutex_unlock(pPipeline->data->event_mutex);

    // If out of the loop but flag is still set, report problems with event
    if (FALSE == pPipeline->data->eventReceived)
    {
        RILOG_ERROR("%s -- pipeline event %s never received\n", __FUNCTION__,
                pEventName);
    }

    // Unref the objects which were ref'd through method calls
    gst_object_unref(sink_pad);
    gst_object_unref(start_pad);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Callback method which is called when event is received by the pad.
 *
 * @param pad       monitors for supplied event on this pad
 * @param event     specific event to watch for
 * @param u_data    callback data which is passed to this method
 *                  which will be pipeline data
 *
 * @return          returns TRUE
 */
gboolean event_probe(GstPad *pad, GstEvent *event, gpointer u_data)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    gboolean retVal = TRUE;

    ri_pipeline_t* pPipeline = (ri_pipeline_t*)u_data;
    switch GST_EVENT_TYPE(event)
    {
        case GST_EVENT_FLUSH_START:
        case GST_EVENT_FLUSH_STOP:
        case GST_EVENT_EOS:

        RILOG_TRACE("%s -- got event %s, setting flag to true\n",
                __FUNCTION__, GST_EVENT_TYPE_NAME (event));

        // Clear flag to indicate event received if waiting for event
        if (FALSE == pPipeline->data->eventReceived)
        {
            g_mutex_lock(pPipeline->data->event_mutex);
            pPipeline->data->eventReceived = TRUE;
            g_cond_signal(pPipeline->data->event_cond);
            g_mutex_unlock(pPipeline->data->event_mutex);

            // Remove the event pad probe
            gst_pad_remove_event_probe(pad, pPipeline->data->event_probe);
        }
        // return true so event keeps on flowing
        break;

        default:
        // ignore any other events
        break;
    }

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, retVal);
    return retVal;
}

/**
 * Sets the rate of the hn playback pipeline.  The rate affects how the
 * buffers are time stamped and displayed to achieve the desired playback rate.
 *
 * @param pPipeline     pipeline to set rate on
 * @param video_device  device which handles display of playback
 * @param rate          desired rate of playback
 *
 * @return returns RI_ERROR_NONE
 */
ri_error pipeline_hn_player_set_rate(ri_pipeline_t* pPipeline,
        ri_video_device_t* video_device, float rate)
{
    ri_error rc = RI_ERROR_NONE;

    if (NULL == pipeHNCCategory)
    {
        pipeHNCCategory = log4c_category_get("RI.Pipeline.HN");
    }

    RILOG_TRACE("%s -- Entry, pipeline = %s\n", __FUNCTION__,
                pPipeline->data->name);

    // Set the rate on the video device to control play back display
    video_device_set_rate(video_device, rate);
    RILOG_INFO("%s() -- set play rate to %f\n", __FUNCTION__, rate);

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, rc);
    return rc;
}

/*
 * Method called by GStreamer appsrc element to indicate it needs more data.
 *
 * @param   src         appsrc element in hn player pipeline
 * @param   length      amount of data requested
 * @param   user_data   reference to this pipeline which was supplied to appsrc
 *                      when callback function was setup
 */
void pipeline_hn_player_need_data(GstAppSrc* src, guint length,
        gpointer user_data)
{
    ri_pipeline_t* pPipeline = user_data;

    if (NULL == pipeHNCCategory)
    {
        pipeHNCCategory = log4c_category_get("RI.Pipeline.HN");
    }

    RILOG_TRACE("%s -- Entry, pipeline = %s\n", __FUNCTION__,
                pPipeline->data->name);

    // Callback to MPEOS layer to let player know it should read more data
    // from the socket and send it down
    pPipeline->data->hn_need_data_cb(pPipeline->data->hn_cb_data);
}

/*
 * Method called by GStreamer appsrc element to indicate queue is currently full
 * and no more data is needed.  Currently not implemented.
 *
 * @param   src         appsrc element in hn player pipeline
 * @param   user_data   reference to this pipeline which was supplied to appsrc
 *                      when callback function was setup
 */
void pipeline_hn_player_enough_data(GstAppSrc* src, gpointer user_data)
{
    if (NULL == pipeHNCCategory)
    {
        pipeHNCCategory = log4c_category_get("RI.Pipeline.HN");
    }

    RILOG_TRACE("%s -- Entry, pipeline = %s\n", __FUNCTION__,
               ((ri_pipeline_t*)user_data)->data->name);
    // do nothing
}

/*
 * Method called by GStreamer appsrc element in response to seek event.
 * Currently not implemented.
 *
 * @param   src         appsrc element in hn player pipeline
 * @param   offset      seek to this specified offset
 * @param   user_data   reference to this pipeline which was supplied to appsrc
 *                      when callback function was setup
 */
gboolean pipeline_hn_player_seek_data(GstAppSrc* src, guint64 offset,
        gpointer user_dat)
{
    // do nothing
    return TRUE;
}

/**
 * Destroy notify method used when setting up appsrc callbacks.
 * Currently not implemented.
 *
 * @param   data  pointer to callback related data.
 */
void pipeline_hn_player_destroy_cb(gpointer data)
{
    // do nothing
}
