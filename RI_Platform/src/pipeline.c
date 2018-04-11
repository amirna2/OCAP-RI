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

//lint -e429 Suppress PC-Lint custodial pointer warning for pCommand

//********************* Temporary begin **************************************
// Note that the following is used by the recording_playback_start() function.
// Once the function is fully implemented, this include can be deleted.
#include <ri_config.h>  // for  ricfg_getValue()
// ******************** Temporary end ****************************************
#include <ri_pipeline_manager.h>
#include <ri_pipeline.h>
#include <ri_video_device.h>
#include <ri_log.h>
#include <glib.h>
#include <glib/gprintf.h>

#include <gst/gst.h>

#include <stdio.h>      // Needed for remove()
#include <stdlib.h>
#include <string.h>
#include <inttypes.h>

#include "gst_utils.h"
#include "gstreamer/gstpidfilter.h"
#include "gstreamer/gsttrickplayfilesrc.h"
#include "gstreamer/gstdisplay.h"
#include "tuner.h"
// *TODO* - new sf #include "ib_section_filter.h"
#include "section_filter.h"
#include "pipeline.h"
#include "pipeline_manager.h"
#include "video_device.h"
#include "tsb.h"
#include "pipeline_hn_server.h"
#include "pipeline_hn_player.h"

#define REMAP_PIDS

#define MAX_REMAPINFO_LEN ((14*32)+1)  // 14 chars per pid ("0x1FFF=0x1FFF ") * MAX_PN + PMT pid plus terminator
#define MAX_PIDLIST_LEN ((14*64)+1)   // 14 chars per pid ("0x1FFF=0x1FFF ") * 64 pids plus terminator
#define RTP_CAPS "application/x-rtp, media=(string)video, clock-rate=(int)90000"

//#define CHECK_OK_OBJECT(o,x,m) if (!(x)) { GST_ERROR_OBJECT((o), (m)); }

#define PLAYBACK_THREAD_WAKE_INTERVAL 1000000  // Interval for PLAYBACK notifications in usec
#define RILOG_CATEGORY pipeCategory
log4c_category_t* pipeCategory = NULL;

// ================
// Local prototypes
// ================
//


gpointer playback_thread(gpointer pData);

typedef enum _playback_thread_action
{
    PLAYBACK_ACTION_START,
    PLAYBACK_ACTION_STOP,
    PLAYBACK_ACTION_END_OF_FILE,
    PLAYBACK_ACTION_START_OF_FILE,
    PLAYBACK_ACTION_SET_RATE_POSITION
} playback_thread_action;

typedef struct _playback_thread_command
{
    playback_thread_action action;
    gdouble rate;
    guint64 position;
} playback_thread_command;

static gboolean create_gst_pipeline_playback_local(ri_pipeline_t* pPipeline);

static gboolean create_gst_pipeline_live(ri_pipeline_t* pPipeline);

static void create_output_bin(ri_pipeline_t* pPipeline);

static void create_tsb_bin(ri_pipeline_t* pPipeline);
char* get_pipeline_type_str(ri_pipeline_type_t type);

static gboolean bus_call(GstBus* bus, GstMessage* msg, gpointer data);

static gboolean set_pipeline_state(GstElement* pipeline, GstState desiredState);

void set_decode_prog_num(ri_pipeline_t* pPipeline, uint16_t program)
{
    pPipeline->data->decode_prog_num = program;
}

///////////////////////////////////////////////////////////////////////////////
/**
 * Temporary function used to obtain the only playback pipeline that is known
 * to the platform.
 * It is expected that this routine will eventually be removed due to future
 * changes to the MPE-layer code (19 Jan 2010).
 *
 * @return the only playback pipeline, or NULL if not found.
 */
ri_pipeline_t* get_playback_pipeline(void)
{
    ri_pipeline_t* retVal = NULL;
    uint32_t numPipelines;

    RILOG_TRACE("%s() -- Entry\n", __FUNCTION__);
    ri_pipeline_manager_t* pPipelineManager = ri_get_pipeline_manager();

    const ri_pipeline_t** ppPlaybackPipelines =
            pPipelineManager->get_playback_pipelines(pPipelineManager,
                    &numPipelines);
    if (0 < numPipelines)
    {
        retVal = (ri_pipeline_t*) ppPlaybackPipelines[0];

        RILOG_DEBUG("%s -- Found pipeline %s\n", __func__, retVal->data->name);
    }
    RILOG_TRACE("%s() -- Exit\n", __FUNCTION__);
    return retVal;
}
///////////////////////////////////////////////////////////////////////////////


/**
 * Initiate a service decode of video and audio pids
 *
 * @param object The "this" pointer
 * @param video_device platform decode display device to use
 * @param pids An array of pids that should be decoded
 * @param pid_count The number of pids in the array
 */
void decode(ri_pipeline_t* pPipeline, ri_video_device_t* video_device,
        ri_pid_info_t* pids, uint32_t pid_count)
{
    if (NULL == pipeCategory)
    {
        pipeCategory = log4c_category_get("RI.Pipeline");
    }

    uint32_t i;
    gchar pid[8];

    RILOG_TRACE("%s() -- Entry\n", __FUNCTION__);

    // Get the mutex so we can manipulate the pipeline
    g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));

    // call to set up blocking if video size info is requested before
    // service selection is complete
    gst_display_set_tune_started ();


    // TODO save the PIDS in the pipeline structure
    // Copy the pid information to pipeline instance structure

    // First delete any existing one
    if (pPipeline->data->decode_pids)
    {
        g_free(pPipeline->data->decode_pids);
        pPipeline->data->decode_pids = NULL;
        pPipeline->data->decode_pid_count = 0;
    } /* endif already had pids */

    // If this pipeline doesn't have video device, ask pipeline mgr to attach it
    if (!has_video_device(pPipeline))
    {
        // Get the pipeline manager and attach video device
        ri_pipeline_manager_t *pipeline_manager = ri_get_pipeline_manager();

        // Set max buffers in decode bin queues based on pipeline specific setting
        decode_bin_set_queue_max_buffers(video_device,
                pPipeline->data->decode_queue_size);

        // note that attaching the video device also sets the pipeline state to 'playing'
        attach_video_device_to_pipeline(pipeline_manager, pPipeline);
    }
    else
    {
        // attaching the video device will call gst_element_get_state(),
        //  calling it here makes sure that it is called whenever this function is called.
        // Requesting the state will cause this thread to block
        //  (for up to the timeout time) while any elements
        //  transition from the async state to their final states.
        GstState state;
        (void) gst_element_get_state(pPipeline->data->gst_pipeline, // element
                &state, // state
                NULL, // pending
                10000000000LL); // timeout(1 second = 10^9 nanoseconds)
    }

    // Allocate space for the pid info
    pPipeline->data->decode_pids = g_try_malloc(pid_count * sizeof(ri_pid_info_t));
    if (pPipeline->data->decode_pids)
    {
        memcpy(pPipeline->data->decode_pids, pids, pid_count
                * sizeof(ri_pid_info_t));
        pPipeline->data->decode_pid_count = pid_count;

        // Parse out the passed pid list for the video and (TODO) audio PIDs
        // and set the relevant PID filters
        for (i = 0; i < pid_count; i++)
        {
            // Set the PID for decode
            (void) g_sprintf(pid, "0x%4.4X", (pids[i].srcPid & 0x1FFF));

            // decode can be called for pipelines w/o a tuner (playback)
            if (NULL != pPipeline->data->tuner)
            {
                (void)pPipeline->data->tuner->add_TS_pid(pPipeline->data->tuner,
                                                     (pids[i].srcPid & 0x1FFF));
            }

            if ((pids[i].srcFormat == RI_SI_ELEM_MPEG_1_VIDEO) ||
                (pids[i].srcFormat == RI_SI_ELEM_MPEG_2_VIDEO) ||
                (pids[i].srcFormat == RI_SI_ELEM_VIDEO_DCII))
            {
#ifdef REMAP_PIDS
                set_video_device_pidlist(pPipeline->data->video_device,
                        VIDEO_DECODE_PID);
                RILOG_INFO("%s() -- video pid %s (remapped to %X)\n",
                        __func__, pid, VIDEO_DECODE_PID);
#else
                set_video_device_pidlist(pPipeline->data->video_device, (pids[i].srcPid & 0x1FFF));
                RILOG_INFO("%s() -- video pid %s\n", __func__, pid);
#endif
            }
            else if ((pids[i].srcFormat == RI_SI_ELEM_MPEG_1_AUDIO) ||
                     (pids[i].srcFormat == RI_SI_ELEM_MPEG_2_AUDIO) ||
                     (pids[i].srcFormat == RI_SI_ELEM_AAC_ADTS_AUDIO) ||
                     (pids[i].srcFormat == RI_SI_ELEM_AAC_AUDIO_LATM) ||
                     (pids[i].srcFormat == RI_SI_ELEM_ATSC_AUDIO) ||
                     (pids[i].srcFormat == RI_SI_ELEM_ENHANCED_ATSC_AUDIO))
            {
#ifdef REMAP_PIDS
                set_audio_device_pidlist(pPipeline->data->video_device,
                        AUDIO_DECODE_PID);
                RILOG_INFO("%s() -- audio pid %s (remapped to %X)\n",
                        __func__, pid, AUDIO_DECODE_PID);
#else
                set_audio_device_pidlist(pPipeline->data->video_device, (pids[i].srcPid & 0x1FFF));
                RILOG_INFO("%s() -- audio pid %s\n", __func__, pid);
#endif
            }
            else
            {
                RILOG_INFO("%s() -- unhandled format: %d\n", __func__,
                           pids[i].srcFormat);
            }
        }
        // The 'live' pipelines have a preprocessor, so...
        if ((RI_PIPELINE_TYPE_LIVE_TSB == pPipeline->data->type)
                || (RI_PIPELINE_TYPE_LIVE_NON_TSB == pPipeline->data->type)
                || (RI_PIPELINE_TYPE_HN_PLAYER == pPipeline->data->type))
        {
            // ...update the preprocessor with the decode pids
            update_preproc(pPipeline);
        }
    }
    else
    {
        RILOG_ERROR(
                "%s() -- Failed to allocate memory for pid information - cannot decode \n",
                __FUNCTION__);
    } /* endif allocating pid info OK */

    // Finally release the mutex
    g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));

    // requesting the state will cause this thread to block
    //   (for up to the timeout time) while any elements
    //   transition from the async state to their final states.
    GstState state;
    (void) gst_element_get_state(pPipeline->data->gst_pipeline, // element
            &state, // state
            NULL, // pending
            10000000000LL); // timeout(1 second = 10^9 nanoseconds)

    //print_bin_elements("decode()", (GstBin*)pPipeline->data->gst_pipeline, 0);
    RILOG_TRACE("%s() -- Exit\n", __FUNCTION__);
}

/**
 * Stops decoding on the specified pipeline.
 * Decoding is stopped by detaching the video device, otherwise known
 * as the decode bin, from the specified pipeline.
 *
 * @param pipeline the pipeline which is to have the decoding stopped
 */
void decode_stop(ri_pipeline_t* pPipeline)
{
    if (NULL == pipeCategory)
    {
        pipeCategory = log4c_category_get("RI.Pipeline");
    }

    RILOG_TRACE("%s() -- Entry\n", __FUNCTION__);

    // Get the mutex so we can manipulate the pipeline
    g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));
    {
        (void) detach_video_device(pPipeline);

        if (pPipeline->data->decode_pids)
        {
            g_free(pPipeline->data->decode_pids);
            pPipeline->data->decode_pids = NULL;
            pPipeline->data->decode_pid_count = 0;
        }
    }
    // release the mutex
    g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));

    RILOG_TRACE("%s() -- Exit\n", __FUNCTION__);
}

/**
 * Returns the tuner associated with supplied pipeline.
 *
 * @param   object   get tuner associated with this pipeline
 *
 * @return  tuner associated with pipeline, may be NULL
 */

ri_tuner_t* get_tuner(ri_pipeline_t* pPipeline)
{
    ri_tuner_t* retVal = NULL;

    if (NULL != pPipeline)
    {
        if (NULL != pPipeline->data)
        {
            if (NULL != pPipeline->data->tuner)
            {
                retVal = pPipeline->data->tuner;
            }
            else
            {
                RILOG_ERROR("%s: NULL pPipeline->data->tuner\n", __func__);
            }
        }
        else
        {
            RILOG_ERROR("%s: NULL pPipeline->data\n", __func__);
        }
    }
    else
    {
        RILOG_ERROR("%s: NULL pPipeline\n", __func__);
    }

    return retVal;
}

/**
 * Returns the section filter associated with supplied pipeline.
 *
 * @param   object   get section filter associated with this pipeline
 *
 * @return  section filter associated with pipeline, may be NULL
 */
ri_section_filter_t* get_section_filter(ri_pipeline_t* pPipeline)
{
    return pPipeline->data->section_filter;
}

/**
 * Performs the actions necessary to change the pipeline state to playing.
 *
 * @param   pipeline starts up this supplied pipeline
 */
void start_pipeline(ri_pipeline_t* pPipeline)
{
    RILOG_TRACE("%s -- Entry, pipeline: %s\n", __func__, pPipeline->data->name);

    (void) gst_element_set_state(pPipeline->data->gst_pipeline,
            GST_STATE_PLAYING);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Performs the actions necessary to change the pipeline state to not playing.
 *
 * @param   pipeline stops this supplied pipeline
 */
void stop_pipeline(ri_pipeline_t* pPipeline)
{
    RILOG_TRACE("%s -- Entry, pipeline: %s\n", __func__, pPipeline->data->name);

    // Make sure decode bin is not attached
    if (has_video_device(pPipeline))
    {
        (void) detach_video_device(pPipeline);
    }

    (void) gst_element_set_state(pPipeline->data->gst_pipeline, GST_STATE_READY);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Performs the actions necessary to destroy the pipeline
 *
 * @param   pipeline    destroys this supplied pipeline
 */
void destroy_pipeline(ri_pipeline_t* pPipeline)
{
    RILOG_TRACE("%s -- Entry, pipeline: %s\n", __func__, pPipeline->data->name);

    // Make sure decode bin is not attached
    if (has_video_device(pPipeline))
    {
        (void) detach_video_device(pPipeline);
    }

    (void) gst_element_set_state(pPipeline->data->gst_pipeline, GST_STATE_NULL);
    gst_object_unref(pPipeline->data->gst_pipeline);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Terminates a TSB or recording playback and returns to live playback.
 *
 * @param object The "this" pointer
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_NO_PLAYBACK: No recording or TSB playback is currently
 *                          running on this pipeline
 */
ri_error playback_stop(ri_video_device_t* video_device)
{
    RILOG_INFO("%s -- Entry\n", __FUNCTION__);

    ri_pipeline_t* pPipeline = get_playback_pipeline();

    // assume that there is no playback
    ri_error rc = RI_ERROR_NONE;

    //print_bin_elements("playback_stop entry", (GstBin*)pPipeline->data->gst_pipeline, 0);

    g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));

    // indicate that the pipeline is no longer playing
    if (TRUE == pPipeline->data->playback.playing)
    {
        // Indicate that the pipeline is no longer playing
        pPipeline->data->playback.playing = FALSE;

        // ...allocate memory for a STOP command
        playback_thread_command* pCommand = g_try_malloc(
                sizeof(playback_thread_command));
        if (NULL != pCommand)
        {
            // need to unlock mutex to allow any queued actions to complete in the thread
            g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));

            // send STOP command to be submitted to the playback thread
            pCommand->action = PLAYBACK_ACTION_STOP;
            g_async_queue_push(pPipeline->data->playback.play_queue, pCommand);

            RILOG_DEBUG(
                    "%s waiting for playback thread to terminate, then delete thread queue\n",
                    __FUNCTION__);
            (void) g_thread_join(pPipeline->data->playback.play_thread);

            // relock mutex for additional pipeline manipulation
            g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));

            // cleanup everything related to the playback thread
            pPipeline->data->playback.play_thread = NULL;
            g_async_queue_unref(pPipeline->data->playback.play_queue);
            pPipeline->data->playback.play_queue = NULL;
        }

        // Flush the decode bin in case it got an EOS event
        decode_bin_flush(pPipeline->data->video_device, FALSE);

        // detach video device from pipeline - this blocks until the pipeline
        //  state is quiescent
        decode_stop(pPipeline);

        // Stop the local playback pipeline, not the live
        stop_pipeline(pPipeline);
        //lint -e429 Suppress PC-Lint custodial pointer warning for pCommand
    }
    else
    {
        rc = RI_ERROR_NO_PLAYBACK;
    }

    // Set the IFS source element to "no source file" (makes it inactive)
    g_object_set(G_OBJECT(pPipeline->data->dvrsrc), "filepath", ".", NULL);
    g_object_set(G_OBJECT(pPipeline->data->dvrsrc), "filename", "", NULL);

    g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, rc);

    return rc;
} /* playback_stop */

/**
 * Initiates playback of a non-TSB recording.
 *
 * @param object The "this" pointer
 *
 * @param video_device platform decode display device to use for play back
 *
 * @param rec_name_root The root name for the desired recording as specified
 *                      in the call to tsb_convert()
 * @param position The 0-based position in the recording (in nanoseconds) from
 *                 which playback should start
 * @param rate The desired playback rate
 * @param pids Array of pids to be played from the recording (the memory for
 *                  this array is freed by the caller).
 * @param pid_count number of pid elements in the pid array
 * @param callback A callback function that will receive all events related to
 *                 the playback of this recording
 * @param cb_data User data that will be passed in every callback invocation
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_ILLEGAL_ARG:  Invalid parameters supplied
 */
ri_error recording_playback_start(ri_video_device_t* video_device,
        const char* rec_path, const char* rec_name, uint64_t position,
        float rate, ri_pid_info_t* pids, uint32_t pid_count,
        ri_dvr_callback_f callback, void* cb_data)
{
    ri_error rc = RI_ERROR_NONE; // assume successful result

    ri_pid_info_t* rec_playback_pids = NULL;
    GError *pError; // Used to collect error from thread create

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    // Validate input parameters
    if ((NULL != video_device) && (NULL != rec_name) && (NULL != rec_path)
            && (NULL != pids))
    {
        // get the (currently only) playback pipeline
        ri_pipeline_t* pPipeline = get_playback_pipeline();

        // Lock the mutex before attempting to manipulate the pipeline
        g_static_rec_mutex_lock(&pPipeline->data->pipeline_mutex);

        // indicate that the pipeline is in an active playing state
        pPipeline->data->playback.playing = TRUE;

        if (NULL != callback)
        {
            pPipeline->data->playback.callback = callback;
            pPipeline->data->playback.cb_data = cb_data;
        }

        // set the element state to READY - this seems to allow for more graceful
        //  changes to the plugin properties
        (void) gst_element_set_state(pPipeline->data->dvrsrc, GST_STATE_READY);

        RILOG_INFO("%s -- Requested: filepath = %s, filename = %s, position = %"PRIu64", rate = %f\n",
                __FUNCTION__,
                rec_path,
                rec_name,
                position,
                rate);

        // update the file source element in preparation for playback
        g_object_set(G_OBJECT(pPipeline->data->dvrsrc), "filepath", rec_path,
                NULL);
        g_object_set(G_OBJECT(pPipeline->data->dvrsrc), "filename", rec_name,
                NULL);

        g_object_set(G_OBJECT(pPipeline->data->dvrsrc), "position_time",
                position, NULL);

        set_rate(pPipeline, rate);

        // set the initial, default rate and position
        pPipeline->data->playback.requested_rate = IGNORE_RATE;
        pPipeline->data->playback.requested_start_position = IGNORE_POSITION;

        //
        // start up the playback thread
        //
        pPipeline->data->playback.play_wake_interval
                = PLAYBACK_THREAD_WAKE_INTERVAL;
        pPipeline->data->playback.play_queue = g_async_queue_new();

        pPipeline->data->playback.play_thread = g_thread_create(
                playback_thread, // func
                (gpointer) pPipeline, // data
                TRUE, // joinable
                &pError); // error

        g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));

        rec_playback_pids = g_try_malloc(sizeof(ri_pid_info_t) * pid_count);
        if (NULL != rec_playback_pids)
        {
            int i = 0;
            memset(rec_playback_pids, 0, sizeof(ri_pid_info_t) * pid_count);
            for (i = 0; i < pid_count; i++)
            {
                rec_playback_pids[i].srcPid = pids[i].recPid;
                rec_playback_pids[i].srcFormat = pids[i].recFormat;
            }

            // prepare the decoder for processing
            // NOTE: This function will attach the video device to the pipeline
            decode(pPipeline, video_device, rec_playback_pids, pid_count);

            g_free(rec_playback_pids);
            rec_playback_pids = NULL;
        }

        // start up the pipeline - this will cause the trick play file source
        // to open the indexed file for reading
        start_pipeline(pPipeline);

        //
        // send START command to be submitted to the playback thread
        //
        playback_thread_command* pCommand = g_try_malloc(
                sizeof(playback_thread_command));
        if (NULL != pCommand)
        {
            pCommand->action = PLAYBACK_ACTION_START;
            g_async_queue_push(pPipeline->data->playback.play_queue, pCommand);
        }
        //lint -e429 Suppress PC-Lint custodial pointer warning for pCommand
    }
    else
    {
        // at least one of the input arguments was invalid, so say so
        rc = RI_ERROR_ILLEGAL_ARG;
    }

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, rc);
    return rc;
} /* recording_playback_start */

/**
 * Sets the playback rate for the playback currently running on this pipeline.
 *
 * @param object The "this" pointer
 * @param rate The desired playback rate
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_NO_PLAYBACK: No recording or TSB playback is currently
 *                          running on this pipeline
 */
ri_error playback_set_rate(ri_video_device_t* video_device, float rate)
{
    RILOG_TRACE("%s -- Entry, rate = %f\n", __FUNCTION__, rate);

    // get the (currently only) playback pipeline
    ri_pipeline_t* pPipeline = get_playback_pipeline();

    ri_error rc = RI_ERROR_GENERAL; // assume general error

    playback_thread_command* pCommand = g_try_malloc(sizeof(playback_thread_command));
    if (NULL != pCommand)
    {
        pCommand->action = PLAYBACK_ACTION_SET_RATE_POSITION;
        pCommand->rate = rate;
        pCommand->position = IGNORE_POSITION;
        g_async_queue_push(pPipeline->data->playback.play_queue, pCommand);

        // indicate success
        rc = RI_ERROR_NONE;
    }
    //lint -e429 Suppress PC-Lint custodial pointer warning for pCommand

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, rc);
    return rc;
} /* playback_set_rate */

/**
 * Sets the position for the playback currently running on this pipeline.
 *
 * @param object The "this" pointer
 * @param position For recording playback, the 0-based position in the recording
 *                 (in nanoseconds).  For TSB playback, the time (in nanoseconds)
 *                 since the the original TSB start time.
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_NO_PLAYBACK: No recording or TSB playback is currently
 *                          running on this pipeline
 */
ri_error playback_set_position(ri_video_device_t* video_device,
        uint64_t position)
{
    RILOG_TRACE("%s -- Entry,\n", __FUNCTION__);

    // get the (currently only) playback pipeline
    ri_pipeline_t* pPipeline = get_playback_pipeline();

    ri_error rc = RI_ERROR_GENERAL; // assume general error

    // Lock the mutex before attempting to manipulate the pipeline
    g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));
    {
        //
        // send START command to be submitted to the playback thread
        //
        playback_thread_command* pCommand = g_try_malloc(
                sizeof(playback_thread_command));
        if (NULL != pCommand)
        {
            pCommand->action = PLAYBACK_ACTION_SET_RATE_POSITION;
            pCommand->rate = IGNORE_RATE;
            pCommand->position = position;
            g_async_queue_push(pPipeline->data->playback.play_queue, pCommand);

            // indicate success
            rc = RI_ERROR_NONE;
        }
        //lint -e429 Suppress PC-Lint custodial pointer warning for pCommand
    }
    g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, rc);
    return rc;
} /* playback_set_position */

//****************************************************************************
//****************************************************************************
// Begin playback operational states
// The following states are used to implement a state machine that is executed
//  by the playback thread.
//****************************************************************************
//****************************************************************************
static doPlaybackState playbackStateIdle(ri_pipeline_t* pPipeline);
static doPlaybackState playbackStateStarting(ri_pipeline_t* pPipeline);
static doPlaybackState playbackStateInitPosition(ri_pipeline_t* pPipeline);
static doPlaybackState playbackStateActive(ri_pipeline_t* pPipeline);
static doPlaybackState playbackStateStopping(ri_pipeline_t* pPipeline);
static doPlaybackState playbackStateStopped(ri_pipeline_t* pPipeline);
static doPlaybackState playbackStateTerminating(ri_pipeline_t* pPipeline);

/**
 * The state that the playback state machine is in when it is not
 * actively doing anything else.
 */
static doPlaybackState playbackStateIdle(ri_pipeline_t* pPipeline)
{
    RILOG_DEBUG("%s -- state = PLAYBACK_STATE_IDLE\n", __FUNCTION__);

    return (doPlaybackState) playbackStateIdle;
}

/**
 * A transitional state that synchronizes the pipeline with the playback state
 * machine.  This state does the following:
 * 1. Requests the current play rate from the Indexing File Source (IFS)
 *  element.
 * 2. Notifies upper layers that that the playback of content has started.
 *
 * Next state:
 * playbackStateInitPosition
 */
static doPlaybackState playbackStateStarting(ri_pipeline_t* pPipeline)
{
    RILOG_DEBUG("%s -- state = PLAYBACK_STATE_STARTING\n", __FUNCTION__);

    ri_playback_status_t status =
    { 0, 0 };
    GstState state;
    doPlaybackState retVal = (doPlaybackState) playbackStateStarting;

    state = GST_STATE((GstElement*) pPipeline->data->gst_pipeline);
    if (GST_STATE_PLAYING == state)
    {
        // get the current playback rate from the dvr source element
        g_object_get(G_OBJECT(pPipeline->data->dvrsrc), "playrate",
                &status.rate, NULL); // property list terminator

        RILOG_INFO(
                "%s -- state = PLAYBACK_STATE_STARTING - playback rate is %f\n",
                __FUNCTION__, status.rate);

        // if we have a callback function...
        if (NULL != pPipeline->data->playback.callback)
        {
            // ...invoke the callback to inform upper layers that playback has started
            g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));
            pPipeline->data->playback.callback(RI_DVR_EVENT_PLAYBACK_STARTED, // event
                    &status, // event_data
                    pPipeline->data->playback.cb_data); // cb_data

            g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));
        }

        // transition to the 'init position' state
        retVal = (doPlaybackState) playbackStateInitPosition;
    }
    return retVal;
}

/**
 * Prepares the IFS to begin playing content at a specific position and rate
 * as specified in the playback data structure associated with the pipeline.
 *
 * Next State:
 *  playbackStateActive
 */
static doPlaybackState playbackStateInitPosition(ri_pipeline_t* pPipeline)
{
    RILOG_DEBUG("%s -- state = PLAYBACK_STATE_INIT_POSITION\n", __FUNCTION__);

    if (IGNORE_POSITION != pPipeline->data->playback.requested_start_position)
    {
        g_object_set(G_OBJECT(pPipeline->data->dvrsrc), "position_time",
                pPipeline->data->playback.requested_start_position, NULL);
    }
    else
    {
        RILOG_DEBUG("%s -- not setting position\n", __FUNCTION__);
    }

    if (IGNORE_RATE != pPipeline->data->playback.requested_rate)
    {
        set_rate(pPipeline, pPipeline->data->playback.requested_rate);
    }
    else
    {
        RILOG_DEBUG("%s -- not setting rate\n", __FUNCTION__);
    }

    // Make sure the pipeline is in the playing state since it may have been paused
    // if an EOS event was received in order to reset the pipeline
    if (set_pipeline_state(pPipeline->data->gst_pipeline, GST_STATE_PLAYING)
            != TRUE)
    {
        RILOG_ERROR("%s -- problems setting the pipeline into playing state\n",
                __FUNCTION__);
    }

    // transition to the 'active' state
    return (doPlaybackState) playbackStateActive;
}

/**
 * Monitors the playback, notifying upper layers of the current
 * position and rate of content playback.
 * This state does the following:
 * 1. Checks that the pipeline is in the 'playing' state.
 * 2. Retrieves the playback rate and position from the IFS.
 * 3. Notifies upper layers of the current playback position and rate.
 *
 * Next state:
 * playbackStateActive
 *
 * On error:
 * playbackStateIdle
 *
 */
static doPlaybackState playbackStateActive(ri_pipeline_t* pPipeline)
{
    RILOG_DEBUG("%s -- state = PLAYBACK_STATE_ACTIVE\n", __FUNCTION__);

    GstState gstState;
    ri_playback_status_t status =
    { 0, 0.0 };

    doPlaybackState retVal = (doPlaybackState) playbackStateActive;

    // if the pipeline is still in the playing state...
    gstState = GST_STATE((GstElement*) pPipeline->data->gst_pipeline);
    if (GST_STATE_PLAYING == gstState)
    {
        // ...get the position information from the dvr source element
        g_object_get(G_OBJECT(pPipeline->data->dvrsrc), "position_time",
                &status.position, "playrate", &status.rate, NULL); // property list terminator

        // if an upper-layer callback function is registered...
        if (NULL != pPipeline->data->playback.callback)
        {
            // ... invoke it to inform of the current position
            g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));
            pPipeline->data->playback.callback(RI_DVR_EVENT_PLAYBACK_STATUS, // event
                    (void*) &status, // event_data
                    pPipeline->data->playback.cb_data); // cb_data
            g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));

#ifndef PRODUCTION_BUILD
            char temp[32] =
            { '\0' };
            RILOG_DEBUG("%s -- Reporting play position = %s ns, rate = %f\n",
                    __FUNCTION__, IfsLongLongToString(status.position, temp),
                    status.rate);
#endif
        }
    }
    else
    {
        char *gstStateName = (char *)gst_element_state_get_name(gstState);
        RILOG_WARN(
                "%s -- GStreamer state is: %s, doesn't match state machine\n",
                __FUNCTION__, gstStateName);
        g_free(gstStateName);

        retVal = (doPlaybackState) playbackStateIdle;
    }

    return retVal;
}

/**
 * A transitional state that moves from the active to the stopped state.
 *
 * Next state:
 *  playbackStateStopped
 */
static doPlaybackState playbackStateStopping(ri_pipeline_t* pPipeline)
{
    RILOG_TRACE("%s -- state = PLAYBACK_STATE_STOPPING\n", __FUNCTION__);

    // transition to the stopped state
    return (doPlaybackState) playbackStateStopped;
}

/**
 * Notifies upper layers that the playback has stopped.
 *
 * Next state:
 *  playbackStateTerminating
 */
static doPlaybackState playbackStateStopped(ri_pipeline_t* pPipeline)
{
    RILOG_TRACE("%s -- state = PLAYBACK_STATE_STOPPED\n", __FUNCTION__);

    // inform upper layer that the DVR playback has stopped
    if (NULL != pPipeline->data->playback.callback)
    {
        g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));
        pPipeline->data->playback.callback(RI_DVR_EVENT_PLAYBACK_STOPPED, // event
                NULL, // event_data
                pPipeline->data->playback.cb_data); // cb_data
        g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));
    }

    // transition to the idle state
    return (doPlaybackState) playbackStateTerminating;
}

/**
 * Terminates a playback session.
 * This state triggers the termination of the playback thread.
 *
 * Next state:
 *  playbackStateTerminating
 */
static doPlaybackState playbackStateTerminating(ri_pipeline_t* pPipeline)
{
    RILOG_TRACE("%s -- state = PLAYBACK_STATE_TERMINATING\n", __FUNCTION__);

    return (doPlaybackState) playbackStateTerminating;
}

/////////////////////////////// playback thread begin ////////////////////////

/**
 * GStreamer pipeline bus callback function.
 *
 * The primary purpose of this function is to detect End Of Stream (EOS) events.
 *
 * In addition, this routine will emit any warning, info, or error messages.
 * These 'cases' can be deleted without any consequence to the rest of the
 * code if they are not desired.
 */
static gboolean bus_call(GstBus* bus, GstMessage* msg, gpointer data)
{
    ri_pipeline_t* pPipeline = data;
    gchar* debug = NULL;
    GError* error = NULL;
    char errBuff[1024];
    errBuff[0] = 0;
    float playRate = 0.0;

    switch (GST_MESSAGE_TYPE(msg))
    {
    case GST_MESSAGE_EOS:
    {
        // An end of stream (end of file) has been detected.  Since we have
        // received an EOS event, we need to flush the pipeline in order to
        // continue processing input.
        RILOG_INFO("%s -- flushing pipeline due to EOS\n", __FUNCTION__);

        // Reset the trick play file src so it doesn't generate additional EOS events
        if (NULL != pPipeline->data->dvrsrc)
        {
            gst_trick_play_file_src_reset(
                    (GstTrickPlayFileSrc*) pPipeline->data->dvrsrc);
        }

        // Flush the decode bin in preparation for a rate change if still attached
        if (NULL != pPipeline->data->video_device)
        {
            decode_bin_flush(pPipeline->data->video_device, FALSE);
        }

        // Set the pipeline state to paused so decode bin is in a paused state
        // and not set to playing until explicitly told to start playing again
        (void) set_pipeline_state(pPipeline->data->gst_pipeline,
                GST_STATE_PAUSED);

        // get the current playback rate from the dvr source element
        g_object_get(G_OBJECT(pPipeline->data->dvrsrc), "playrate", &playRate,
                NULL); // property list terminator

        // if we are playing in the 'forward' direction...
        if (0 < playRate)
        {
            // indicate to the playback thread to perform end of file handling
            playback_thread_command* pCommand = g_try_malloc(
                    sizeof(playback_thread_command));
            if (NULL != pCommand)
            {
                (void) set_pipeline_state(pPipeline->data->gst_pipeline,
                        GST_STATE_READY);
                pCommand->action = PLAYBACK_ACTION_END_OF_FILE;
                g_async_queue_push(pPipeline->data->playback.play_queue,
                        pCommand);
            }
            (void) g_snprintf(errBuff, sizeof(errBuff) / sizeof(errBuff[0]),
                    "end of file encountered\n");
            //lint -e429 Suppress PC-Lint custodial pointer warning for pCommand
        }
        else
        {
            // indicate to the playback thread to perform end of file handling
            playback_thread_command* pCommand = g_try_malloc(
                    sizeof(playback_thread_command));
            if (NULL != pCommand)
            {
                (void) set_pipeline_state(pPipeline->data->gst_pipeline,
                        GST_STATE_READY);
                pCommand->action = PLAYBACK_ACTION_START_OF_FILE;
                g_async_queue_push(pPipeline->data->playback.play_queue,
                        pCommand);
            }
            (void) g_snprintf(errBuff, sizeof(errBuff) / sizeof(errBuff[0]),
                    "beginning of file encountered\n");
            //lint -e429 Suppress PC-Lint custodial pointer warning for pCommand
        }
    }
        break;

    case GST_MESSAGE_WARNING:
        gst_message_parse_warning(msg, &error, &debug);
        (void) g_snprintf(errBuff, sizeof(errBuff) / sizeof(errBuff[0]),
                "message = %s, (%s)\n", error->message, debug);

        g_free(debug);
#ifdef WIN32     // the following line doesn't work in Linux at the moment!?
        g_error_free(error);
#endif
        break;

    case GST_MESSAGE_INFO:
        gst_message_parse_info(msg, &error, &debug);
        (void) g_snprintf(errBuff, sizeof(errBuff) / sizeof(errBuff[0]),
                "message = %s, (%s)\n", error->message, debug);

        g_free(debug);
#ifdef WIN32     // the following line doesn't work in Linux at the moment!?
        g_error_free(error);
#endif
        break;

    case GST_MESSAGE_STATE_CHANGED:
    {
        /*******
         * commented out to reduce noise in the log file, it remains
         * as a useful monitoring tool
         GstState oldstate;
         GstState newstate;
         GstState pending;

         char* pszSource = gst_object_get_name((GstObject*)GST_MESSAGE_SRC(msg));
         if(NULL == pszSource)
         {
         pszSource = "Unknown";
         }

         gst_message_parse_state_changed(msg, &oldstate, &newstate, &pending);

         g_snprintf(errBuff,
         sizeof(errBuff)/sizeof(errBuff[0]),
         "state changed(%s): oldstate = %s, newstate = %s, pending = %s\n",
         pszSource,
         gst_element_state_get_name(oldstate),
         gst_element_state_get_name(newstate),
         gst_element_state_get_name(pending));
         ******/
        break;
    }

    case GST_MESSAGE_ERROR:
    {
        gst_message_parse_error(msg, &error, &debug);
        (void) g_snprintf(errBuff, sizeof(errBuff) / sizeof(errBuff[0]),
                "message = %s, (%s)\n", error->message, debug);
        g_free(debug);
#ifdef WIN32     // the following line doesn't work in Linux at the moment!?
        g_error_free(error);
#endif
        break;
    }

    case GST_MESSAGE_QOS:
        //
        // A QOS message is posted on the bus whenever an element decides
        // to drop a buffer because of QoS reasons or whenever it changes
        // its processing strategy because of QoS reasons (quality
        // adjustments such as processing at lower accuracy).
        //
        // This message can be posted by an element that performs
        // synchronisation against the clock (live) or it could be dropped
        // by an element that performs QoS because of QOS events received
        // from a downstream element (!live).
        //
        // So, we'll count it as there's nothing we can do, the buffer has
        // already been dropped - logging just exacerbates the problem...
        pPipeline->qos_messages++;
        break;

        //case GST_MESSAGE_NEW_CLOCK:
        //{
        //  sprintf(errBuff, "New clock message");
        //  break;
        //}

    default:
        // nothing to do
        (void) g_snprintf(errBuff, sizeof(errBuff) / sizeof(errBuff[0]),
                "default = %s (%d)\n", GST_MESSAGE_TYPE_NAME(msg),
                GST_MESSAGE_TYPE(msg));

        break;
    }

    if (0 != errBuff[0])
    {
        RILOG_INFO("%s -- %s\n", __FUNCTION__, errBuff);
    }

    return TRUE;
}

/**
 * This thread hosts the DVR and TSB playback state machines.  Included
 * here is a switch statement that accepts commands from external threads.
 */
gpointer playback_thread(gpointer pData)
{
    if (NULL == pipeCategory)
    {
        pipeCategory = log4c_category_get("RI.Pipeline");
    }

    ri_pipeline_t* pPipeline = pData;
    playback_thread_command* pCommand;
    GTimeVal timeWake;
    gboolean bTerminate = FALSE;
    guint watch_id;

    doPlaybackState playbackState = (doPlaybackState) playbackStateIdle;

    // add a listener to the bus so that we can be apprised of end-of-stream events
    GstBus* gstBus = gst_pipeline_get_bus(GST_PIPELINE(
            pPipeline->data->gst_pipeline));
    watch_id = gst_bus_add_watch(gstBus, bus_call, pPipeline);
    gst_object_unref(gstBus);

    // set the initial state of the playback state machine to 'idle'...
    playbackState = (doPlaybackState) playbackStateIdle;

    (void) g_async_queue_ref(pPipeline->data->playback.play_queue);

    while (FALSE == bTerminate)
    {
        g_get_current_time(&timeWake);
        g_time_val_add(&timeWake, pPipeline->data->playback.play_wake_interval);

        // check for the existence of a command to this thread
        pCommand = g_async_queue_timed_pop(
                pPipeline->data->playback.play_queue, &timeWake);

        //
        // Handle any commands requested from outside of this thread
        //
        if (NULL != pCommand)
        {
            //
            // there is a command, so process it
            //
            switch (pCommand->action)
            {
            case PLAYBACK_ACTION_START:
                // handle request to start playback by...
                RILOG_DEBUG("%s -- Processing PLAYBACK_ACTION_START command\n",
                        __FUNCTION__);

                // ...transitioning to the starting state
                playbackState = (doPlaybackState) playbackStateStarting;
                break;

            case PLAYBACK_ACTION_STOP:
                // handle request to stop playback by...
                RILOG_DEBUG("%s -- Processing PLAYBACK_ACTION_STOP command\n",
                        __FUNCTION__);

                // ...transitioning to the stopping state
                playbackState = (doPlaybackState) playbackStateStopping;

                break;

            case PLAYBACK_ACTION_END_OF_FILE:
                // handle notification of attempt to read past end of file
                RILOG_DEBUG(
                        "%s -- Processing PLAYBACK_ACTION_END_OF_FILE command\n",
                        __FUNCTION__);

                // if an upper-layer callback function is registered...
                if (NULL != pPipeline->data->playback.callback)
                {
                    // ... invoke it to inform that the end of the playback
                    //  file has been encountered
                    pPipeline->data->playback.callback(
                            RI_DVR_EVENT_END_OF_FILE, // event
                            NULL, // event_data
                            pPipeline->data->playback.cb_data); // cb_data
                }

                // if in one of the streaming states...
                if ((playbackState == (doPlaybackState) playbackStateStarting)
                        || (playbackState
                                == (doPlaybackState) playbackStateInitPosition)
                        || (playbackState
                                == (doPlaybackState) playbackStateActive)
                        || (playbackState
                                == (doPlaybackState) playbackStateStarting))
                {
                    // ...transition to the idle playback state
                    playbackState = (doPlaybackState) playbackStateIdle;
                }

                break;

            case PLAYBACK_ACTION_START_OF_FILE:
                // handle notification of attempt to read past end of file
                RILOG_DEBUG(
                        "%s -- Processing PLAYBACK_ACTION_START_OF_FILE command\n",
                        __FUNCTION__);

                // if an upper-layer callback function is registered...
                if (NULL != pPipeline->data->playback.callback)
                {
                    // ... invoke it to inform that the end of the playback
                    //  file has been encountered
                    pPipeline->data->playback.callback(
                            RI_DVR_EVENT_START_OF_FILE, // event
                            NULL, // event_data
                            pPipeline->data->playback.cb_data); // cb_data
                }

                // if in one of the streaming states...
                if ((playbackState == (doPlaybackState) playbackStateStarting)
                        || (playbackState
                                == (doPlaybackState) playbackStateInitPosition)
                        || (playbackState
                                == (doPlaybackState) playbackStateActive)
                        || (playbackState
                                == (doPlaybackState) playbackStateStarting))
                {
                    // ...transition to the idle playback state
                    playbackState = (doPlaybackState) playbackStateIdle;
                }

                break;

            case PLAYBACK_ACTION_SET_RATE_POSITION:
                RILOG_DEBUG("%s -- Processing PLAYBACK_ACTION_SET_RATE_POSITION command, rate = %f, position = %"PRIu64"\n",
                        __FUNCTION__, pCommand->rate, pCommand->position);

                g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));
                {
                    // save the requested position
                    pPipeline->data->playback.requested_start_position
                            = pCommand->position;

                    // if a rate has been specified...
                    if (IGNORE_RATE != pCommand->rate)
                    {
                        // ...save it
                        pPipeline->data->playback.requested_rate
                                = pCommand->rate;
                    }
                }
                g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));

                playbackState = (doPlaybackState) playbackStateInitPosition;
                break;

            default:
                RILOG_WARN(
                        "%s -- Playback thread received invalid command action",
                        __FUNCTION__);
                break;
            };
            g_free(pCommand);
        }

        //
        // Now process the playback state machine under mutex protection
        //
        g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));
        {
            playbackState = playbackState(pPipeline);
        }
        g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));

        // if 'terminating' is the current state...
        if (((doPlaybackState) playbackStateTerminating) == playbackState)
        {
            // ...terminate this thread
            bTerminate = TRUE;
        }
    }; // end while()


    // thread has terminated, so clean up
    RILOG_DEBUG("%s -- Thread terminated\n", __FUNCTION__);

    // remove signal listener
    (void) g_source_remove(watch_id);
    g_async_queue_unref(pPipeline->data->playback.play_queue);

    return NULL;
}
/////////////////////////////// playback thread end //////////////////////////

/**
 * Sets the input to live or time shifted on the passed input
 * selector
 *
 * @param inputselect   The gstreamer input selector to be
 *                      controlled
 * @param live          True to select live, false for time
 *                      shifted input
 *
 * @return None
 */
void select_live(GstElement *inputselect, gboolean live)
{
    GValue prop =
    { 0, }; // Used to set various elements' initial state
    GstPad *selected_pad; // The live input pad on the input selector

    RILOG_TRACE("%s -- Entry, live = %s\n", __FUNCTION__, TRUE == live ? "true"
            : "false");
    if (live)
    {
        selected_pad = gst_element_get_static_pad(inputselect, "sink0");
    }
    else
    {
        selected_pad = gst_element_get_static_pad(inputselect, "sink1");
    }
    if (selected_pad)
    {
        (void) g_value_init(&prop, G_TYPE_OBJECT);
        g_value_set_object(&prop, G_OBJECT(selected_pad));
        g_object_set_property(G_OBJECT(inputselect), "active-pad", &prop);
        g_value_unset(&prop);
        gst_object_unref(selected_pad);
    } /* endif live pad retrieval worked */

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);

} /* select_live */

/**
 * Creates the gstreamer pipeline based on the type of the
 * supplied pipeline.  This method will call additional methods
 * specific to the type of pipeline to be created.
 *
 * After calling to the specific pipeline methods, the actual
 * gstreamer pipeline element is created and the input put is
 * added to this.
 *
 * If the input bin has a src pad, the elements referred to
 * as the output bin are created in order to support attaching
 * the decode bin dynamically.
 *
 * @param object The pipeline object pointer
 *
 * @return TRUE if pipeline was created, false if problems were encountered.
 */
static gboolean create_gst_pipeline(ri_pipeline_t* pPipeline)
{
    // assume success
    gboolean rc = TRUE;

    if (NULL == pipeCategory)
    {
        pipeCategory = log4c_category_get("RI.Pipeline");
    }

    RILOG_TRACE("%s -- Entry, pipeline\n", __FUNCTION__);

    // create the various pipelines
    if ((RI_PIPELINE_TYPE_LIVE_TSB == pPipeline->data->type)
            || (RI_PIPELINE_TYPE_LIVE_NON_TSB == pPipeline->data->type))
    {
        pPipeline->data->gst_pipeline = gst_pipeline_new("live pipeline");
        rc = create_gst_pipeline_live(pPipeline);
    }
    else if (RI_PIPELINE_TYPE_PLAYBACK_LOCAL == pPipeline->data->type)
    {
        pPipeline->data->gst_pipeline = gst_pipeline_new(
                "playback-local pipeline");
        rc = create_gst_pipeline_playback_local(pPipeline);
    }
    else if (RI_PIPELINE_TYPE_HN_PLAYER == pPipeline->data->type)
    {
        pPipeline->data->gst_pipeline = gst_pipeline_new("hn-player pipeline");
        rc = pipeline_hn_player_create(pPipeline);
    }
    else if (RI_PIPELINE_TYPE_HN_SERVER == pPipeline->data->type)
    {
        pPipeline->data->gst_pipeline = gst_pipeline_new("hn-server pipeline");
        rc = pipeline_hn_server_create(pPipeline);
    }
    else
    {
        RILOG_ERROR("%s -- unsupported pipeline type: %d\n", __FUNCTION__,
                pPipeline->data->type);
        rc = FALSE;
    }

    if (TRUE == rc)
    {
        // get the pipeline name just once as it performs a strdup
        pPipeline->data->name = gst_element_get_name(
                                        pPipeline->data->gst_pipeline);

        // Add input bin which applies to every pipeline type
        rc = gst_bin_add(GST_BIN(pPipeline->data->gst_pipeline),
                pPipeline->data->input_bin);

        // If the pipeline's input bin has a src pad, this indicates that the decode
        // bin can be attached to the pipeline
        GstPad* input_bin_src_pad = gst_element_get_static_pad(
                pPipeline->data->input_bin, "src");

        // If pipeline's input bin has src pad, create the output bin which facilitates
        // attaching the decode bin
        if ((NULL != input_bin_src_pad) && (!gst_pad_is_linked(
                input_bin_src_pad)))
        {
            create_output_bin(pPipeline);
        }
    }

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, rc);
    return rc;
}

/**
 * Creates the gstreamer pipeline to support live decode or
 * playback of a recording. Live play can be with or without
 * time shift buffer capability.
 *
 * A live pipeline is constructed of an input stage and a decode
 * stage. A non-TSB input stage consists of a UDP source
 * (receives UDP packets and acts as a pipeline source). It
 * hooks directly into the decode tee of the decode stage.
 *
 * For a TSB capable live pipeline, the UDP source attaches to a
 * pre-processor which filters the transport stream down to the
 * superset of all PIDs required for sections, all required for
 * decode, and all required for record/TSB.
 * In this case the preprocessor attaches to a tsb tee which
 * sends one leg off to the decoder (via a 2:1 input switch),
 * and one leg off to the TSB. The TSB path contains an on-off
 * switch (implemented with a pid filter) followed by an
 * indexing file sink which records to the TSB file. Then a file
 * source (with indexing knowledge) acts on the same file and
 * feeds it to the second input of the 2:1 input switch. This
 * switch determines whether live or TSB is fed to the decoder
 * stage.
 *
 * In the decoder stage, a tee breaks the transport stream out
 * into a leg for sections, a leg for video decode, and a leg
 * for audio decode. Both the decode legs have a pid filter
 * which takes the stream down to a single pid, followed by an
 * elementary stream assembler, decoder, and sink.
 *
 * The section leg contains a section assembler, followed by a
 * section filter and a section sink. The section assembler/sink
 * act independently on each PID, and support multiple section
 * filters per PID.
 *
 * @param object The pipeline object pointer
 *
 * @return  TRUE if pipeline is created without problems, FALSE if problems encountered
 */
static gboolean create_gst_pipeline_live(ri_pipeline_t* pPipeline)
{
    gboolean link_result;

    if (NULL == pipeCategory)
    {
        pipeCategory = log4c_category_get("RI.Pipeline");
    }

    RILOG_TRACE("%s -- Entry, pipeline: %s\n", __func__, pPipeline->data->name);

    pPipeline->data->input = gst_load_element("udpsrc", "input");
    pPipeline->data->passthru0 = gst_load_element("passthru", "passthru0");
    pPipeline->data->queue1 = gst_load_element("queue", "live_q_tee");
    pPipeline->data->inputtee = gst_load_element("tee", "inputtee");
    pPipeline->data->queue2 = gst_load_element("queue", "live_q_pidfilter");
    pPipeline->data->preproc = gst_load_element("pidfilter", "preproc");

    // Create the inband section filtering components for this pipeline
    pPipeline->data->tuner = create_tuner(&pPipeline->data->input, pPipeline);

    // Create the section filtering
    create_ssbin(pPipeline);

    // Create the live streaming tuner bin for non-tsb implementations
    create_ls_tuner_bin(pPipeline);

    if (pPipeline->hasTSB)
    {
        create_tsb_bin(pPipeline);

        // Bin for TSB Section of pipeline
        pPipeline->data->input_bin = gst_bin_new("input_bin");

        if (pPipeline->data->input_bin && pPipeline->data->input
                && pPipeline->data->queue1 && pPipeline->data->inputtee
                && pPipeline->data->queue2 && pPipeline->data->preproc
                && pPipeline->data->tsbtee && pPipeline->data->queue3
                && pPipeline->data->queue4 && pPipeline->data->recordswitch
                && pPipeline->data->tsbsink)
        {
            {
                gst_bin_add_many(GST_BIN(pPipeline->data->input_bin),
                        pPipeline->data->input, pPipeline->data->queue1,
                        pPipeline->data->passthru0, pPipeline->data->inputtee,
                        pPipeline->data->ss_bin, pPipeline->data->queue2,
                        pPipeline->data->preproc, pPipeline->data->tsbtee,
                        pPipeline->data->queue3, pPipeline->data->queue4,
                        pPipeline->data->recordswitch,
                        pPipeline->data->ls_tuner_bin,
                        pPipeline->data->tsbsink, NULL);
            }

            GstPad* input_bin_src_pad = gst_element_get_static_pad(
                    pPipeline->data->queue3, "src");
            CHECK_OK_OBJECT(pPipeline->data->input_bin, gst_element_add_pad(
                    pPipeline->data->input_bin, gst_ghost_pad_new("src",
                            input_bin_src_pad)),
                    "Adding ghost src pad to input_bin failed");

            /* Link together the live video input path (sans section filter) */
            {
                link_result = gst_element_link_many(pPipeline->data->input,
                        pPipeline->data->queue1, pPipeline->data->passthru0,
                        pPipeline->data->inputtee, pPipeline->data->queue2,
                        pPipeline->data->preproc, pPipeline->data->tsbtee,
                        pPipeline->data->queue3, NULL);
            }
            CHECK_OK_OBJECT(pPipeline->data->input_bin, link_result,
                    "Linking input_bin live elements failed");

            /* Add the section filter to the live input path*/
            /* ss_bin begins with a queue so no need here */
            CHECK_OK_OBJECT(pPipeline->data->input_bin, gst_element_link_many(
                    pPipeline->data->inputtee, pPipeline->data->ss_bin, NULL),
                    "Linking ss_bin to input failed");

            /* Add the live streaming bin to the live input path*/
            CHECK_OK_OBJECT(pPipeline->data->input_bin, gst_element_link_many(
                    pPipeline->data->inputtee,
                    pPipeline->data->ls_tuner_bin, NULL),
                    "Linking ls_tuner_bin to input failed");

            /* Link the tsb sink path to the live input path */
            CHECK_OK_OBJECT(pPipeline->data->input_bin, gst_element_link_many(
                    pPipeline->data->tsbtee, pPipeline->data->queue4,
                    pPipeline->data->recordswitch, pPipeline->data->tsbsink,
                    NULL), "Linking input_bin sink elements failed");
        }
        else
        {
            return FALSE;
        } /* if load of tsb elements succeeded */
    }
    else
    {
        /* Live, no TSB pipeline */
        pPipeline->data->input_bin = gst_bin_new("input_bin");
        if (pPipeline->data->input_bin && pPipeline->data->input
                && pPipeline->data->queue1 && pPipeline->data->preproc
                && pPipeline->data->queue2)
        {
            {
                gst_bin_add_many(GST_BIN(pPipeline->data->input_bin),
                        pPipeline->data->input, pPipeline->data->queue1,
                        pPipeline->data->passthru0, pPipeline->data->inputtee,
                        pPipeline->data->ss_bin, pPipeline->data->queue2,
                        pPipeline->data->preproc,
                        pPipeline->data->ls_tuner_bin, NULL);
            }
            GstPad* input_bin_src_pad = gst_element_get_static_pad(
                    pPipeline->data->preproc, "src");
            CHECK_OK_OBJECT(pPipeline->data->input_bin, gst_element_add_pad(
                    pPipeline->data->input_bin, gst_ghost_pad_new("src",
                            input_bin_src_pad)),
                    "Adding ghost src pad to input_bin failed");

            /* Link together the live input path */
            {
                link_result = gst_element_link_many(pPipeline->data->input,
                        pPipeline->data->queue1, pPipeline->data->passthru0,
                        pPipeline->data->inputtee, pPipeline->data->queue2,
                        pPipeline->data->preproc, NULL);
            }
            CHECK_OK_OBJECT(pPipeline->data->input_bin, link_result,
                    "Linking input_bin live elements failed");

            /* Add the section filter to the live input path*/
            CHECK_OK_OBJECT(pPipeline->data->input_bin, gst_element_link_many(
                    pPipeline->data->inputtee, pPipeline->data->ss_bin, NULL),
                    "Linking ss_bin to input failed");

            /* Add the live streaming bin to the live input path*/
            CHECK_OK_OBJECT(pPipeline->data->input_bin, gst_element_link_many(
                    pPipeline->data->inputtee,
                    pPipeline->data->ls_tuner_bin, NULL),
                    "Linking ls_tuner_bin to input failed");
        } /* endif live pipeline creation succeeded */
    } /* endif has TSB */

    // Clear out the PID list on the preprocessor and video pid since stack will be setting PID via decode() and other methods
    // If we don't do this the pid filters pass everything.
    g_object_set(G_OBJECT(pPipeline->data->preproc), "pidlist", NO_PIDS, NULL);

    /* init some properties for TSB elements */
    if (pPipeline->hasTSB)
    {
        g_object_set(G_OBJECT(pPipeline->data->recordswitch), "pidlist",
                NO_PIDS, NULL);

        // make the file sink initially a bit bucket, that is, until it is placed in use
        g_object_set(G_OBJECT(pPipeline->data->tsbsink), "bitBucket", TRUE,
                NULL);
    }

    RILOG_TRACE("%s -- Exit, return value = TRUE\n", __FUNCTION__);
    return TRUE;
}
/**
 * Creates the necessary gstreamer elements to support playback of a DVR recording
 * to the local device (vs. HN remote playback).
 *
 * @pipeline   pipeline which supports playback of local recording
 *
 * @return  always returns TRUE
 */
static gboolean create_gst_pipeline_playback_local(ri_pipeline_t* pPipeline)
{
    if (NULL == pipeCategory)
    {
        pipeCategory = log4c_category_get("RI.Pipeline");
    }

    RILOG_TRACE("%s -- Entry, pipeline: %s\n", __func__, pPipeline->data->name);

    /* Create DVR playback-only pipeline */
    pPipeline->data->dvrsrc = gst_load_element("trickplayfilesrc", "dvrsrc");
    pPipeline->data->input_bin = gst_bin_new("input_bin");

    if (pPipeline->data->dvrsrc && pPipeline->data->input_bin)
    {
        gst_bin_add_many(GST_BIN(pPipeline->data->input_bin),
                pPipeline->data->dvrsrc, NULL);
        GstPad* input_bin_src_pad = gst_element_get_static_pad(
                pPipeline->data->dvrsrc, "src");
        CHECK_OK_OBJECT(pPipeline->data->input_bin, gst_element_add_pad(
                pPipeline->data->input_bin, gst_ghost_pad_new("src",
                        input_bin_src_pad)),
                "Adding ghost src pad to dvr input_bin failed");
    }
    else
    {
        RILOG_ERROR("%s -- Failed to create dvr source or input bin",
                __FUNCTION__);
    }

    g_object_set(G_OBJECT(pPipeline->data->dvrsrc), "filepath", NO_SOURCE_FILE,
            NULL);
    g_object_set(G_OBJECT(pPipeline->data->dvrsrc), "filename", NO_SOURCE_FILE,
            NULL);

    RILOG_TRACE("%s -- Exit, return value = TRUE\n", __FUNCTION__);
    return TRUE;
}

/**
 * Creates the TSB bin and tsb related elements for the supplied pipeline
 * TODO: rename this function to something like 'create_tsb_elements';
 * the current name is a misnomer since no bin is created in this function.
 *
 * @param   pipeline create a TSB bin for this pipeline
 */
static void create_tsb_bin(ri_pipeline_t* pPipeline)
{
    if (NULL == pipeCategory)
    {
        pipeCategory = log4c_category_get("RI.Pipeline");
    }

    RILOG_TRACE("%s -- Entry, pipeline: %s\n", __func__, pPipeline->data->name);

    pPipeline->data->tsbtee = gst_load_element("tee", "tsbtee");
    pPipeline->data->queue3 = gst_load_element("queue", "tsb_q_pidfilter");
    pPipeline->data->recordswitch = gst_load_element("pidfilter", "record");
    pPipeline->data->queue4 = gst_load_element("queue", "tsb_q_tsbsink");
    pPipeline->data->tsbsink = gst_load_element("indexingfilesink", "tsbsink");

    g_object_set(G_OBJECT(pPipeline->data->tsbsink), "async", FALSE, NULL); // original w/filesrc

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Creates the output portion of the pipeline which will be used
 * to attach and detach the decode bin.  A fakesink is needed to
 * ensure data keeps flowing if the video sink element is not attached.
 *
 * @param   create output bin for this pipeline
 */
static void create_output_bin(ri_pipeline_t* pPipeline)
{
    if (NULL == pipeCategory)
    {
        pipeCategory = log4c_category_get("RI.Pipeline");
    }

    RILOG_TRACE("%s -- Entry, pipeline: %s\n", __func__, pPipeline->data->name);

    // Create the output bin
    pPipeline->data->output_bin = gst_bin_new("output_bin");

    pPipeline->data->outputtee = gst_load_element("tee", "outputtee");
    pPipeline->data->passthru1 = gst_load_element("passthru", "passthru1");
    pPipeline->data->fakesink = gst_load_element("fakesink", "fakesink");
    g_object_set(G_OBJECT(pPipeline->data->fakesink), "async", FALSE, NULL);

    gst_bin_add_many(GST_BIN(pPipeline->data->output_bin),
            pPipeline->data->outputtee, pPipeline->data->fakesink,
            pPipeline->data->passthru1, NULL);

    GstPad* output_bin_sink_pad = gst_element_get_static_pad(
            pPipeline->data->outputtee, "sink");
    if (NULL == output_bin_sink_pad)
    {
        RILOG_ERROR("%s -- Unable to get outputtee sink pad\n", __FUNCTION__);
    }
    CHECK_OK_OBJECT(pPipeline->data->output_bin, gst_element_add_pad(
            pPipeline->data->output_bin, gst_ghost_pad_new("sink",
                    output_bin_sink_pad)),
            "Adding ghost sink pad to output_bin failed");

    GstPad* output_bin_src_pad = gst_element_get_static_pad(
            pPipeline->data->passthru1, "src");
    CHECK_OK_OBJECT(pPipeline->data->output_bin, gst_element_add_pad(
            pPipeline->data->output_bin, gst_ghost_pad_new("src",
                    output_bin_src_pad)),
            "Adding ghost src pad to output_bin failed");

    CHECK_OK_OBJECT(pPipeline->data->output_bin, gst_element_link_many(
            pPipeline->data->outputtee, pPipeline->data->fakesink, NULL),
            "Linking output tee to fakesink failed");

    // Note: passthru could be replaced by queue with ghost pad
    CHECK_OK_OBJECT(pPipeline->data->output_bin, gst_element_link_many(
            pPipeline->data->outputtee, pPipeline->data->passthru1, NULL),
            "Linking output tee to passthru1 failed");

    (void) gst_bin_add(GST_BIN(pPipeline->data->gst_pipeline),
            pPipeline->data->output_bin);

    CHECK_OK_OBJECT(pPipeline->data->gst_pipeline, gst_element_link_many(
            pPipeline->data->input_bin, pPipeline->data->output_bin, NULL),
            "Linking output and input bin failed");

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Creates a pipeline of the supplied type
 * This involves creating the relevant GStreamer elements, and initializing
 *  function pointers for each of the various pipeline types.
 *
 * @param type  type of pipeline to create
 *
 * @return Returns the created pipeline
 */
ri_pipeline_t* create_pipeline(ri_pipeline_type_t type)
{
    if (NULL == pipeCategory)
    {
        pipeCategory = log4c_category_get("RI.Pipeline");
    }

    RILOG_TRACE("%s -- creating pipeline of type: %s\n", __FUNCTION__,
          get_pipeline_type_str(type));

    ri_pipeline_t* pPipeline;

    pPipeline = g_try_malloc0(sizeof(ri_pipeline_t));
    if (NULL != pPipeline)
    {
        pPipeline->data = g_try_malloc0(sizeof(ri_pipeline_data_t));
        if (NULL != pPipeline->data)
        {
            memset(pPipeline->data, 0, sizeof(ri_pipeline_data_t));
        }
        else
        {
            RILOG_ERROR(
                    "%s() -- Failed to allocate memory for pipeline data\n",
                    __FUNCTION__);
            g_free(pPipeline);
            return NULL;
        }
    }
    else
    {
        RILOG_ERROR("%s() -- Failed to allocate memory for pipeline\n",
                __FUNCTION__);
        return NULL;
    }

    // Initialize common pipeline data
    pPipeline->data->type = type;
    pPipeline->data->decode_queue_size = 10; // lower latency

    g_static_rec_mutex_init(&(pPipeline->data->pipeline_mutex));

    // Need tuner, decode, section filter if live pipeline type
    if ((RI_PIPELINE_TYPE_LIVE_TSB == type) || (RI_PIPELINE_TYPE_LIVE_NON_TSB
            == type))
    {
        pPipeline->isLive = TRUE;
        pPipeline->get_section_filter = get_section_filter;
        pPipeline->set_decode_prog_num = set_decode_prog_num;
        pPipeline->decode = decode;
        pPipeline->decode_stop = decode_stop;
    }

    // TSB if live with TSB
    if (RI_PIPELINE_TYPE_LIVE_TSB == type)
    {
        pPipeline->hasTSB = TRUE;
        pPipeline->tsb_start = tsb_start;
        pPipeline->tsb_stop = tsb_stop;
    }

    pPipeline->get_tuner = get_tuner;
    (void) create_gst_pipeline(pPipeline);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return pPipeline;
}

static ri_media_type mediaFormat2Type(ri_media_es_format format)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    // assume media type is unknown
    ri_media_type ret_val = RI_MEDIA_TYPE_UNKNOWN;

    switch (format)
    {
    case RI_SI_ELEM_MPEG_1_VIDEO:
    case RI_SI_ELEM_MPEG_2_VIDEO:
    case RI_SI_ELEM_MHEG:
    case RI_SI_ELEM_H_222:
    case RI_SI_ELEM_ISO_14496_VISUAL:
    case RI_SI_ELEM_MPEG_2_IPMP:
    case RI_SI_ELEM_AVC_VIDEO:
    case RI_SI_ELEM_VIDEO_DCII:
        ret_val = RI_MEDIA_TYPE_VIDEO;
        break;

    case RI_SI_ELEM_MPEG_1_AUDIO:
    case RI_SI_ELEM_MPEG_2_AUDIO:
    case RI_SI_ELEM_AAC_ADTS_AUDIO:
    case RI_SI_ELEM_AAC_AUDIO_LATM:
    case RI_SI_ELEM_ATSC_AUDIO:
    case RI_SI_ELEM_ENHANCED_ATSC_AUDIO:
        ret_val = RI_MEDIA_TYPE_AUDIO;
        break;

    default:
        RILOG_WARN("%s -- didn't map format type: 0x%X\n", __func__, format);
        break;
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return ret_val;
}

static uint16_t mapAvPIDs(ri_media_type type, uint16_t pid, char *pid_str,
        char *mapping_str, gboolean *previously_mapped)
{
    static int video_pid = 0;
    static int audio_pid = 0;
    static int pcr_pid = 0;
    static int pmt_pid = 0;
    uint16_t remapped_pid = pid; // default to no re-map
    unsigned int p1, p2;
    char test_str[8];
    char *p;

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    if ((type == 0) && (pid == 0))
    {
        video_pid = VIDEO_DECODE_PID;
        audio_pid = AUDIO_DECODE_PID;
        pcr_pid = PCR_PID;
        pmt_pid = PMT_PID;
        RILOG_DEBUG("%s -- A/V PID remap values reset.\n", __FUNCTION__);
    }
    else if (pid_str == NULL)
    {
        RILOG_ERROR("%s -- NULL output pid_str?!\n", __FUNCTION__);
    }
    else if (previously_mapped == NULL)
    {
        RILOG_ERROR("%s -- NULL previously_mapped bool?!\n", __FUNCTION__);
    }
    else
    {
        // if the caller provides a PID that's already mapped, find & return it...
        *previously_mapped = FALSE;
        (void) g_sprintf(test_str, "0x%4.4X=", pid);

        if ((mapping_str != NULL) && (p = strstr(mapping_str, test_str)))
        {
            sscanf(p, "%X=%X", &p1, &p2);
            remapped_pid = p2 & 0x1FFF;
            RILOG_INFO("%s -- existing A/V PID map: 0x%X=0x%X\n", __FUNCTION__,
                    p1, p2);
            *previously_mapped = TRUE;
        }
#ifdef REMAP_PIDS
        else
        {
            switch (type)
            {
            case RI_MEDIA_TYPE_VIDEO:
                remapped_pid = video_pid & 0x1FFF;
                video_pid++;
                break;
            case RI_MEDIA_TYPE_AUDIO:
                remapped_pid = audio_pid & 0x1FFF;
                audio_pid++;
                break;
            case RI_MEDIA_TYPE_DATA:
                RILOG_INFO("%s -- pass DATA PID: 0x%X\n", __func__, pid);
                break;
            case RI_MEDIA_TYPE_SUBTITLES:
                RILOG_INFO("%s -- pass SUBTITLES PID: 0x%X\n", __func__, pid);
                break;
            case RI_MEDIA_TYPE_SECTIONS:
                RILOG_INFO("%s -- pass SECTIONS PID: 0x%X\n", __func__, pid);
                break;
            case RI_MEDIA_TYPE_PCR:
                remapped_pid = pcr_pid & 0x1FFF;
                pcr_pid++;
                break;
            case RI_MEDIA_TYPE_PMT:
                remapped_pid = pmt_pid & 0x1FFF;
                pmt_pid++;
                break;
            default: // not a recognized PID type?
                RILOG_WARN("%s -- pass unknown PID: 0x%X type: 0x%X\n",
                            __func__, pid, type);
                break;
            }
        }
#endif

        (void) g_sprintf(pid_str, "0x%4.4X ", remapped_pid);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return remapped_pid;
}

/**
 * Updates the pre-processor's list of PIDs to be passed based on the supplied
 *  pipeline type.  For 'live' content, the PIDs to be passed are specified
 *  in the <code>decode_pids</code> data structure.
 *  Pipelines that also include a TSB that is recording content may also have
 *  additional PIDS specified in the <code>tsb.pids</code> data structure.
 *
 * @param   pPipeline   pipeline whose pre-processor pid list is to be updated
 */
void update_preproc(ri_pipeline_t* pPipeline)
{
    char *remapinfo; // Holds composite remapinfotring
    char *pidstring; // Holds composite pidstring
    char pid[16]; // Holds a single hex pid string
    char mapstring[8]; // Holds a single hex pid map string
    gboolean previously_mapped = FALSE;
    uint32_t i;

    RILOG_TRACE("%s -- Entry, pipeline: %s\n", __func__, pPipeline->data->name);

    // Create the composite pidlist for the preprocessor
    pidstring = g_try_malloc(MAX_PIDLIST_LEN);

    // Create the composite remapinfo string for the preprocessor
    if (NULL != (remapinfo = g_try_malloc(MAX_REMAPINFO_LEN)))
    {
        (void) g_sprintf(remapinfo, "0x%4.4X=0x%4.4X ",
                pPipeline->data->decode_prog_num,
                pPipeline->data->decode_prog_num);
    }
    else
    {
        RILOG_ERROR("%s -- remapinfo g_try_malloc faulure!?\n", __FUNCTION__);
        return;
    }

    // Init the PID remap state
    uint16_t ret_pid = mapAvPIDs(0, 0, NULL, pidstring, &previously_mapped);
    RILOG_INFO("1:%s %d = mapAvPIDs();\n", __func__, ret_pid);

    if (pidstring)
    {
        pidstring[0] = 0;

        /* Collect all the decode pids */
        for (i = 0; i < pPipeline->data->decode_pid_count; i++)
        {
            (void) g_sprintf(pid, "0x%4.4X=",
                    (pPipeline->data->decode_pids[i].srcPid & 0x1FFF));
            if (pPipeline->data->decode_pids[i].mediaType
                    == RI_MEDIA_TYPE_UNKNOWN)
            {
                ret_pid = mapAvPIDs(mediaFormat2Type(
                        pPipeline->data->decode_pids[i].srcFormat),
                        pPipeline->data->decode_pids[i].srcPid, mapstring,
                        pidstring, &previously_mapped);
                RILOG_INFO("2:%s - %d = mapAvPIDs();\n", __func__, ret_pid);
            }
            else
            {
                (void) mapAvPIDs(pPipeline->data->decode_pids[i].mediaType,
                        pPipeline->data->decode_pids[i].srcPid, mapstring,
                        pidstring, &previously_mapped);
                RILOG_INFO("3:%s - %d = mapAvPIDs();\n", __func__, ret_pid);
            }

            if (!previously_mapped)
            {
                strcat(pidstring, pid);
                strcat(pidstring, mapstring);
            }

            if (RI_MEDIA_TYPE_PMT == pPipeline->data->decode_pids[i].mediaType)
            {
                if (strlen(remapinfo) <= (MAX_REMAPINFO_LEN / 2))
                {
                    strcat(remapinfo, pid);
                    strcat(remapinfo, mapstring);
                }
            }
        }

        /* If a TSB is active, collect all the record pids */
        for (i = 0; i < pPipeline->data->tsb_pid_count; i++)
        {
            (void) g_sprintf(pid, "0x%4.4X=",
                    (pPipeline->data->tsb_pids[i].srcPid & 0x1FFF));
            ret_pid = pPipeline->data->tsb_pids[i].recPid = mapAvPIDs(
                    pPipeline->data->tsb_pids[i].mediaType,
                    pPipeline->data->tsb_pids[i].srcPid, mapstring, pidstring,
                    &previously_mapped);
            RILOG_INFO("4:%s - %d = mapAvPIDs();\n", __func__, ret_pid);
            // No transcoding
            pPipeline->data->tsb_pids[i].recFormat
                    = pPipeline->data->tsb_pids[i].srcFormat;

            if (!previously_mapped)
            {
                strcat(pidstring, pid);
                strcat(pidstring, mapstring);
            }

            if (RI_MEDIA_TYPE_PMT == pPipeline->data->tsb_pids[i].mediaType)
            {
                if (strlen(remapinfo) <= (MAX_REMAPINFO_LEN / 2))
                {
                    strcat(remapinfo, pid);
                    strcat(remapinfo, mapstring);
                }
            }
        }

        // If no TSB is active, and we have a tuner (non-HN stream),
        // add all the broadcast tune pids...
        if ((0 == pPipeline->data->tsb_pid_count) &&
            (NULL != pPipeline->data->tuner))
        {
            uint32_t numPIDs = 0;
            uint16_t pids[MAX_PIDS];
            gchar* pmtPIDlist = NULL;

            // get the current PMT PIDs from the tuner source element
            g_object_get(G_OBJECT(pPipeline->data->input), "pmtpidlist",
                         &pmtPIDlist, NULL);

            if (NULL != pmtPIDlist)
            {
                gchar* endPtr = NULL;
                gchar* strInt = strtok(pmtPIDlist, " ");

                // build our list of PMT pids from a hex string PID list
                while (NULL != strInt)
                {
                    pids[numPIDs++] = (uint16_t)strtoul(strInt, &endPtr, 16);
                    strInt = ((numPIDs < MAX_PIDS)?  strtok(NULL, " ") : NULL);
                }

                g_free(pmtPIDlist);
                // numPIDs has been set in the previous call to the actual
                // number of PIDs returned in the list...
                for (i = 0; i < numPIDs; i++)
                {
                    (void) g_sprintf(pid, "0x%4.4X=", pids[i] & 0x1FFF);
                    ret_pid = mapAvPIDs(RI_MEDIA_TYPE_PMT, pids[i], 
                                     mapstring, pidstring, &previously_mapped);
                    RILOG_INFO("5:%s - %d = mapAvPIDs();\n", __func__, ret_pid);
                    if (!previously_mapped)
                    {
                        strcat(pidstring, pid);
                        strcat(pidstring, mapstring);

                        // these are all PMT PIDs
                        strcat(remapinfo, pid);
                        strcat(remapinfo, mapstring);
                    }
                    else
                    {
                        RILOG_WARN("%s -- PID %s%s was previously remapped!\n",
                                    __FUNCTION__, pid, mapstring);
                    }
                }
            }
            else
            {
                RILOG_WARN("%s called with no tuner set on the pipeline!\n",
                            __FUNCTION__);
            }
        }

        if (strlen(remapinfo) <= (MAX_REMAPINFO_LEN - strlen("0x0000=0x0000")))
        {
            // Add the PAT PID to the pids to pass if we're mapping the PMT!
            strcat(pidstring, "0x0000=0x0000 ");
        }

        /* Make sure the pid string contains a space if otherwise empty */
        if (strlen(pidstring) == 0)
        {
            strcat(pidstring, NO_PIDS);
        }

        // Update the pidlist in the preproc
        RILOG_INFO("%s -- Setting preprocessor pid list to %s\n",
                __FUNCTION__, pidstring);

        g_object_set(G_OBJECT(pPipeline->data->preproc), "pidlist", pidstring,
                NULL);
        g_free(pidstring);
    } /* endif pidstring succeeded */

    if (NULL != remapinfo)
    {
        if (strlen(remapinfo))
        {
            RILOG_INFO("%s -- Setting preprocessor Remap info to %s\n",
                    __FUNCTION__, remapinfo);
            g_object_set(G_OBJECT(pPipeline->data->preproc), "remapinfo",
                    remapinfo, NULL);
        }
        else
        {
            RILOG_DEBUG("%s -- incomplete Remap info (%s)\n", __FUNCTION__,
                    remapinfo);
#ifdef TEST_PAT_PMT_REMAP
            g_object_set(G_OBJECT(pPipeline->data->preproc),
                    "remapinfo", "0X0001=0X0007 0X0064=0X0164 ", NULL);
#endif
        }

        g_free(remapinfo);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
} /* update_preproc */

/**
 * Determines if the supplied pipeline has the video device
 *
 * @param   pipeline check if this pipeline has the video device attached
 *
 * @return  true if supplied pipeline has video device, false otherwise
 */
gboolean has_video_device(ri_pipeline_t* pPipeline)
{
    gboolean hasBin = FALSE;
    RILOG_TRACE("%s -- Entry, pipeline: %s\n", __func__, pPipeline->data->name);

    if (NULL != pPipeline->data->video_device)
    {
        hasBin = TRUE;
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return hasBin;
}

/**
 * Links output bin of supplied pipeline to supplied video device bin.
 *
 * @param   pPipeline to modify
 * @param   video device to attach
 *
 * @return  true if successfully attaches video device, false otherwise
 */
gboolean attach_video_device(ri_pipeline_t* pPipeline,
        ri_video_device_t* video_device)
{
    // assume failure
    gboolean ret_val = FALSE;

    RILOG_TRACE("%s -- Entry, pipeline: %s\n", __func__, pPipeline->data->name);

    // Make sure video device is not already attached
    if (TRUE == has_video_device(pPipeline))
    {
        // it looks like this pipeline already has a video device, nothing to do
        RILOG_WARN("%s -- video device was already attached to pipeline %s\n",
                __FUNCTION__, pPipeline->data->name);
    }
    else
    {
        // Attach the video device to this pipeline
        if (TRUE != decode_bin_modify_link(pPipeline->data->gst_pipeline,
                video_device, TRUE))
        {
            // something bad happened, so just quit
            RILOG_ERROR(
                    "%s -- problems attaching video device to pipeline %s\n",
                    __FUNCTION__, pPipeline->data->name);
        }
        else
        {
            // the video device was successfully attached to the pipeline...
            pPipeline->data->video_device = video_device;

            // Make sure the decode bin is in the 'playing' state
            // This is needed when trying to use playback pipeline because if a EOS was
            // encountered the pipeline is paused.
            video_device_set_playing(video_device);

            // Note: in order to get the 'async' property change to become active, the state
            //   of the video device needs to be interrogated. The need to do this was found
            //   empirically - that is, no documentation relevant to this need was
            //   encountered.
            GstState state;
            if (GST_STATE_CHANGE_SUCCESS == gst_element_get_state(
                    pPipeline->data->gst_pipeline, // element
                    &state, // state
                    NULL, // pending
                    1000000000LL)) // timeout = 1sec
            {
                RILOG_DEBUG("%s -- video device state read ok\n", __FUNCTION__);
            }
            else
            {
                RILOG_DEBUG("%s -- video device state was NOT read ok\n",
                        __FUNCTION__);
            }
            ret_val = TRUE;
        }
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return ret_val;
}

/**
 * Unlinks output bin of supplied pipeline.
 *
 * @param   pPipeline the pipeline to modify
 *
 * @return  true if successfully detaches video device, false otherwise
 */
gboolean detach_video_device(ri_pipeline_t* pPipeline)
{
    // assume failure
    gboolean ret_val = FALSE;

    RILOG_TRACE("%s -- Entry, pipeline: %s\n", __func__, pPipeline->data->name);

    // Make sure video device is attached
    if (FALSE == has_video_device(pPipeline))
    {
        RILOG_ERROR("%s -- video device was NOT attached to pipeline %s\n",
                __FUNCTION__, pPipeline->data->name);
    }
    else
    {
        // *TODO* - this doesn't seem necessary, is it?
        // getting the state here should make sure that the any async states have a
        //   chance to quiesce before removing the decode bin from the pipeline
        GstState state;
        if (GST_STATE_CHANGE_SUCCESS == gst_element_get_state(
                pPipeline->data->gst_pipeline, // element
                &state, // state
                NULL, // pending
                1000000000LL)) // timeout = 1sec
        {
            RILOG_DEBUG("%s -- called, the element state was read ok\n",
                    __FUNCTION__);
        }

        // Detach video device from supplied pipeline
        if (TRUE != decode_bin_modify_link(pPipeline->data->gst_pipeline,
                pPipeline->data->video_device, FALSE))
        {
            RILOG_ERROR(
                    "%s -- problems detaching video device from pipeline %s\n",
                    __FUNCTION__, pPipeline->data->name);
        }
        else
        {
            // Remove the reference to video device in pipeline
            pPipeline->data->video_device = NULL;

            // video device was successfully removed from the pipeline
            ret_val = TRUE;
        }
    }

    return ret_val;
}

/**
 * Utility method which returns a string representation of the
 * supplied pipeline type enum
 *
 * @param   type  pipeline type enum
 *
 * @return  string describing pipeline type
 */
char* get_pipeline_type_str(ri_pipeline_type_t type)
{
    char* typeStr = "UNKNOWN";
    switch (type)
    {
    case RI_PIPELINE_TYPE_LIVE_TSB:
        typeStr = "LIVE TSB PIPELINE";
        break;
    case RI_PIPELINE_TYPE_LIVE_NON_TSB:
        typeStr = "LIVE NON-TSB PIPELINE";
        break;
    case RI_PIPELINE_TYPE_PLAYBACK_LOCAL:
        typeStr = "LOCAL PLAYBACK PIPELINE";
        break;
    case RI_PIPELINE_TYPE_HN_SERVER:
        typeStr = "HN SERVER PIPELINE";
        break;
    case RI_PIPELINE_TYPE_HN_PLAYER:
        typeStr = "HN PLAYER PIPELINE";
        break;
    default:
        // leave at unknown
        break;
    }

    return typeStr;
}

// define this to test live streaming tuner data by sending it to a file
#undef SEND_LSTUNER_OUTPUT_TO_FILE
/**
 * Creates the live streaming tuner bin for non-tsb implementations
 *
 * @param pipeline add the resultant live streaming tuner bin to this pipeline
 */
void create_ls_tuner_bin(ri_pipeline_t* pPipeline)
{
    RILOG_TRACE("%s -- Entry, pipeline: %s\n", __func__, pPipeline->data->name);

    pPipeline->data->event_mutex = g_mutex_new();
#ifdef SEND_LSTUNER_OUTPUT_TO_FILE
    pPipeline->data->ls_tuner_appsink =
            gst_load_element("filesink", "ls_tuner_appsink");
    g_object_set(pPipeline->data->ls_tuner_appsink,
            "location", "livestrm.mpg", NULL);
#else
    int maxLsBuffers = 20;
    char* cfgVal = NULL;

    if ((cfgVal = ricfg_getValue("RIPlatform",
            "RI.Platform.ls_tuner.maxbuffers")) != NULL)
    {
        if (NULL != cfgVal)
        {
            maxLsBuffers = atoi(cfgVal);
            RILOG_INFO("%s -- set LS Tuner appsink maxbuffers to %d\n",
                       __func__, maxLsBuffers);
        }
    }

    pPipeline->data->ls_tuner_appsink =
            gst_load_element("appsink", "ls_tuner_appsink");
    GstAppSink *sink = (GstAppSink*)pPipeline->data->ls_tuner_appsink;
    gst_app_sink_set_max_buffers(sink, maxLsBuffers);
    gst_app_sink_set_drop(sink, TRUE);
    g_object_set(sink, "sync", FALSE, NULL);
#endif

    pPipeline->data->ls_tuner_queue =
            gst_load_element("queue", "ls_tuner_queue");
    pPipeline->data->sptsassembler =
            gst_load_element("sptsassembler", "ls_sptsassembler");
    g_object_set(G_OBJECT(pPipeline->data->sptsassembler),
                 "zero_prgm_pats",
                 ricfg_getBoolValue("RIPlatform",
                 "RI.Platform.display.vpop.zeroPrgmPatInsertion"),
                 "insert_canned_pat_pmt", FALSE, NULL);

    // Live Streaming Tuner bin part of pipeline
    pPipeline->data->ls_tuner_bin = gst_bin_new("ls_tuner_bin");
    gst_bin_add_many(GST_BIN(pPipeline->data->ls_tuner_bin),
            pPipeline->data->ls_tuner_queue,
            pPipeline->data->sptsassembler,
            pPipeline->data->ls_tuner_appsink, NULL);

    /* Ghost the input pad to the bin*/
    GstPad* ls_bin_pad = gst_element_get_static_pad(
            pPipeline->data->ls_tuner_queue, "sink");
    CHECK_OK_OBJECT(pPipeline->data->ls_tuner_bin, gst_element_add_pad(
            pPipeline->data->ls_tuner_bin,
            gst_ghost_pad_new("sink", ls_bin_pad)),
            "Adding ghost pad to ls_tuner_bin failed");

    /* Link the bin elements */
    CHECK_OK_OBJECT(pPipeline->data->ls_tuner_bin, gst_element_link_many(
            pPipeline->data->ls_tuner_queue,
            pPipeline->data->sptsassembler,
            pPipeline->data->ls_tuner_appsink, NULL),
            "Linking ls_tuner_bin elements failed");
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Callback method called when event is detected on appsink pad.
 * It is used to ensure appsink received the intended events related to
 * flushing.
 *
 * @param   pad      pad which received event
 * @param   event    new event received
 * @param   u_data   data supplied when probe was created, in this case
 *                   reference to appsink itself
 */
static gboolean ls_tuner_appsink_event_probe(GstPad *pad, GstEvent *event,
        gpointer u_data)
{
    ri_pipeline_t* pPipeline = u_data;
    gboolean retVal = TRUE;

    switch GST_EVENT_TYPE(event)
    {
        case GST_EVENT_FLUSH_START:
        case GST_EVENT_FLUSH_STOP:
            RILOG_TRACE("%s -- got event %s, setting flag to true\n",
                        __func__, GST_EVENT_TYPE_NAME(event));

            // Clear flag to indicate event received if waiting for event
            if (FALSE == pPipeline->data->eventReceived)
            {
                pPipeline->data->eventReceived = TRUE;

                // Remove the event pad probe
                gst_pad_remove_event_probe(pad,
                        pPipeline->data->ls_tuner_event_probe);
            }
            break;

        default:
            // ignore any other events
            break;
    }

    return retVal;
}

/**
 * Sends event to the sink pad of the ls_tuner bin which propogates through
 * entire ls_tuner bin.  Sets up an event probe to prevent the event flowing
 * through entire pipeline, restricts it just to the ls_tuner bin
 *
 * @param   pPipeline   data structure containing elements in ls_tuner bin
 * @param   event          sends the supplied event to the ls_tuner bin
 */
#define EVENT_RECEIVE_TIMEOUT_MS    10
static void ls_tuner_send_event(ri_pipeline_t* pPipeline, GstEvent* event)
{
    // save a pointer to the event name since the gst_pad_send_event takes
    // ownership of the event, making it unavailable for later reference
    const gchar* pEventName = GST_EVENT_TYPE_NAME(event);

    // Get the sink pad on ls_tuner bin to send event on
    GstElement* ls_tuner_bin = pPipeline->data->ls_tuner_bin;
    GstPad* sink_pad = gst_element_get_static_pad(ls_tuner_bin, "sink");
    GstPad* appsink_pad = gst_element_get_static_pad(
                             pPipeline->data->ls_tuner_appsink, "sink");

    GTimeVal timeout = { 0, 0 };
    gboolean cond_signalled = TRUE;
    g_get_current_time(&timeout);

    // timeout is in msec, function expects usec
    g_time_val_add(&timeout, EVENT_RECEIVE_TIMEOUT_MS * 1000);

    // Initialize the flag to indicate event has not yet been received
    pPipeline->data->eventReceived = FALSE;

    // Add probe on ls_tuner_app sink pad to monitor for EOS event
    pPipeline->data->ls_tuner_event_probe =
        gst_pad_add_event_probe(appsink_pad,
                     G_CALLBACK(ls_tuner_appsink_event_probe), pPipeline);
    RILOG_INFO("%s [%s] to ls_tuner bin\n", __func__, pEventName);
    (void) gst_pad_send_event(sink_pad, event);
    g_mutex_lock(pPipeline->data->event_mutex);

    while (FALSE == pPipeline->data->eventReceived && TRUE == cond_signalled)
    {
        // Sleep for a short time
        RILOG_INFO("%s waiting for event %s\n", __func__, pEventName);
        cond_signalled = g_cond_timed_wait(pPipeline->data->event_cond,
                   pPipeline->data->event_mutex, &timeout);
    }

    g_mutex_unlock(pPipeline->data->event_mutex);

    // If out of the loop but flag is still set, report problems with event
    if (FALSE == pPipeline->data->eventReceived)
    {
        RILOG_ERROR("%s event [%s] never received\n", __func__, pEventName);
    }

    // Unref the objects which were ref'd through method calls
    gst_object_unref(sink_pad);
    gst_object_unref(appsink_pad);
}

void ls_tuner_flush(ri_pipeline_t* pPipeline)
{
    GstEvent* flush_start = gst_event_new_flush_start();
    GstEvent* flush_stop = gst_event_new_flush_stop();

    RILOG_INFO("%s(%p) -- called\n", __FUNCTION__, pPipeline);

    // Send the FLUSH START event to elements in ls_tuner bin
    ls_tuner_send_event(pPipeline, flush_start);

    // Send the FLUSH STOP event to elements in ls_tuner bin
    ls_tuner_send_event(pPipeline, flush_stop);
}

/**
 * Creates the section filtering bin.
 *
 * @param   pipeline       add section filtering bin to this pipeline
 */
void create_ssbin(ri_pipeline_t* pPipeline)
{
    RILOG_TRACE("%s -- Entry, pipeline: %s\n", __func__, pPipeline->data->name);

    pPipeline->data->queue0 = gst_load_element("queue", "queue0");
    pPipeline->data->sectionassembler = gst_load_element("sectionassembler",
            "inbandsectionassembler");
    pPipeline->data->sectionfilter = gst_load_element("sectionfilter",
            "inbandsectionfilter");
    pPipeline->data->sectionsink = gst_load_element("sectionsink",
            "sectionsink");

    // Set the section sink to async which is needed for live sources
    g_object_set(G_OBJECT(pPipeline->data->sectionsink), "async", FALSE, NULL);

    pPipeline->data->section_filter = create_section_filter(
            pPipeline->data->sectionassembler, pPipeline->data->sectionfilter,
            pPipeline->data->sectionsink, pPipeline->data->tuner);

    // Section filtering bin part of pipeline
    // TODO - make conditional on element creation success
    pPipeline->data->ss_bin = gst_bin_new("ss_bin");
    gst_bin_add_many(GST_BIN(pPipeline->data->ss_bin), pPipeline->data->queue0,
            pPipeline->data->sectionassembler, pPipeline->data->sectionfilter, 
            pPipeline->data->sectionsink, NULL);
    /* Ghost the input pad to the bin*/
    GstPad* ss_bin_pad = gst_element_get_static_pad(pPipeline->data->queue0,
            "sink");
    CHECK_OK_OBJECT(pPipeline->data->ss_bin, gst_element_add_pad(
            pPipeline->data->ss_bin, gst_ghost_pad_new("sink", ss_bin_pad)),
            "Adding ghost pad to ss_bin failed");

    /* Link the bin elements */
    CHECK_OK_OBJECT(pPipeline->data->ss_bin, gst_element_link_many(
            pPipeline->data->queue0, pPipeline->data->sectionassembler, 
            pPipeline->data->sectionfilter, pPipeline->data->sectionsink, 
            NULL), "Linking ss_bin elements failed");
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Utility method which will put the supplied pipeline into the desired state.
 * It will call get state to verify the pipeline truely transistioned into
 * desired state.
 *
 * @param   pipeline       set state of this pipeline
 * @param   desiredState   set pipeline to this state
 *
 * @return  true to state transistioned to desired state,
 *          otherwise false
 */
static gboolean set_pipeline_state(GstElement* pipeline, GstState desiredState)
{
    char *name = gst_element_get_name(pipeline);
    RILOG_TRACE("%s -- Entry, pipeline: %s\n", __func__, name);
    g_free(name);

    GstState curState;
    GstState pendingState;
    GstStateChangeReturn rc;
    gboolean retVal = FALSE;

    // Set the pipeline into desired state
    (void) gst_element_set_state(pipeline, desiredState);

    // Retrieve the state now to see if it has transistioned
    rc = gst_element_get_state(pipeline, &curState, &pendingState, 100000000LL);
    char *curStateName = (char *)gst_element_state_get_name(curState);
    char *desiredStateName = (char *)gst_element_state_get_name(desiredState);
#ifndef PRODUCTION_BUILD
    char *pendingStateName = (char *)gst_element_state_get_name(pendingState);
    char *returnStateName = (char *)gst_element_state_change_return_get_name(rc);
#endif

    RILOG_DEBUG("%s -- cur pipeline state %s, pending state %s, rc %s\n",
            __FUNCTION__, curStateName, pendingStateName, returnStateName);

    // If current state is playing and data is flowing, block the input src
    if (desiredState != curState)
    {
        RILOG_WARN(
                "%s -- pipeline did not enter desired state %s, still in state %s\n",
                __FUNCTION__, desiredStateName, curStateName);
    }
    else
    {
        RILOG_DEBUG("%s -- pipeline is now in desired state %s\n",
                __FUNCTION__, desiredStateName);
        retVal = TRUE;
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return retVal;
}

void set_rate(ri_pipeline_t* pipeline, float rate)
{
    GObject* dvrsrc;

    RILOG_TRACE("%s -- Entry, pipeline: %s\n", __func__, pipeline->data->name);

    dvrsrc = G_OBJECT(pipeline->data->dvrsrc);
    if (rate == 0.)
    {
        g_object_set(dvrsrc, "playrate", rate, "framerate", 2, NULL);
    }
    else if (rate == 1.)
    {
        g_object_set(dvrsrc, "playrate", rate, "framerate", 30, NULL);
    }
    else
    {
        float abs_rate = (rate > 0.) ? (rate) : (-rate);
        if (abs_rate <= 2.)
        {
            g_object_set(dvrsrc, "playrate", rate, "framerate", 15, NULL);
        }
        else if (abs_rate <= 4.)
        {
            g_object_set(dvrsrc, "playrate", rate, "framerate", 8, NULL);
        }
        else if (abs_rate <= 8.)
        {
            g_object_set(dvrsrc, "playrate", rate, "framerate", 4, NULL);
        }
        else
        {
            g_object_set(dvrsrc, "playrate", rate, "framerate", 2, NULL);
        }
    }
    RILOG_TRACE("%s -- Exit , pipeline = %s\n", __FUNCTION__,
                pipeline->data->name);
}
