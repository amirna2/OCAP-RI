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

#include <stdlib.h>
#include <inttypes.h>
#include <ri_config.h>
#include <ri_pipeline.h>
#include <ri_log.h>
#include "gst_utils.h"
#include "pipeline.h"
#include "pipeline_manager.h"
#include "gstreamer/gsttrickplayfilesrc.h"

#undef EXTERNAL_CT_ENGINE

#define RILOG_CATEGORY pipeHNCategory
log4c_category_t* pipeHNCategory = NULL;

/**
 * LOCAL METHODS & PARAMETERS
 */
static void pipeline_hn_server_set_type(ri_pipeline_t* pPipeline, int tuner, ri_hn_srvr_type);

static ri_bool pipeline_hn_server_is_dvr(ri_pipeline_t* pPipeline);

static void pipeline_hn_server_send_event(ri_pipeline_t* pPipeline,
        GstEvent* event);

static gboolean appsink_event_probe(GstPad *pad, GstEvent *event,
        gpointer u_data);

gboolean pipeline_hn_server_test(gpointer object);

ri_error pipeline_hn_server_get_file_duration(ri_pipeline_t* pPipeline,
        uint64_t* file_duration_ms);

GstFlowReturn pipeline_hn_server_new_buffer(GstAppSink* sink, gpointer user_data);

void pipeline_hn_server_eos(GstAppSink* sink, gpointer user_data);

static ri_bool check_appsink_transition(ri_pipeline_t* pPipeline)
{
    int maxCnt = 5;
    int curCnt = 0;
    GstState state = GST_STATE_NULL;
    ri_bool retVal = FALSE;

    do
    {
        (void)gst_element_get_state(
                  (GstElement*)pPipeline->data->appsink, // element
                   &state,                               // state
                   NULL,                                 // pending
                   100000000LL);        // timeout(1 second = 10^9 nanoseconds)
        curCnt++;

        if (state != GST_STATE_PLAYING)
        {
            // Sleep for a short time
            RILOG_DEBUG("%s -- sleeping 1 sec, loop %d, waiting for state playing, cur state: %s\n", __FUNCTION__,
                 curCnt, gst_element_state_get_name(state));
            g_usleep(1000000L);
        }
        else
        {
            retVal = TRUE;
        }
    }
    while ((GST_STATE_PLAYING != state) && (curCnt < maxCnt));

    RILOG_INFO("%s -- Appsink state after waiting for pipeline to start: %s\n",
            __FUNCTION__, gst_element_state_get_name(state));
    return retVal;
}

static ri_bool get_dvr_ct_file(char* file, int size)
{
    char *name = "opb.20120614.program_5.h264.352x288_256k.ts";
    ri_bool retVal = FALSE;

    if (strlen(name) < size)
    {
        strcpy(file, name);
        retVal = TRUE;
    }
    else
    {
        RILOG_ERROR("%s (%s) is too long\n", __func__, name);
    }

    return retVal;
}

/**
 * Starts the remote playback of an hn stream with RI acting as server
 *
 * @param object    The "this" pointer
 * @param rec_path  platform specific directory where recording resides
 * @param rec_name  platform specific filename for the recording to playback
 * @param rate      rate of stream playback
 * @param bytePos   begin playback at this "network" byte position
 * @param isDVR     indicates if this a recording so pipeline will
 *                  use dvrsrc rather than plain old filesrc
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_STREAMING: Problems starting hn streaming pipeline
 */
ri_error pipeline_hn_server_start(ri_pipeline_t* pPipeline, int tuner,
        const char* file_path, const char* file_name, float rate,
        int32_t frame_rate, int64_t bytePos, ri_hn_srvr_type pipe_type)
{
    ri_error rc = RI_ERROR_NONE;
    char location[FILENAME_MAX*2];
    char dvr_file_name[FILENAME_MAX] = "";
    ri_bool isDVRCTPlay = FALSE;

    if (NULL == pipeHNCategory)
    {
        pipeHNCategory = log4c_category_get("RI.Pipeline.HN");
    }

    RILOG_TRACE("%s -- Entry, pipeline = %s\n", __FUNCTION__,
                pPipeline->data->name);

    // Get the mutex so we can manipulate the pipeline
    g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));

    // Create mutex which controls access to buffer count and EOS flag
    pPipeline->data->server->buf_mutex = g_mutex_new();

    pPipeline->data->server->bufCnt = 0;
    pPipeline->data->server->isEOS = FALSE;
    pPipeline->data->server->bytePos = bytePos;
    pPipeline->data->server->rate = rate;

    GstAppSink *appsink = (GstAppSink*) pPipeline->data->appsink;

    // Setup the callback so hn server pipeline can be notified when
    // buffer is available and avoid special EOS handling
    pPipeline->data->server->appsink_callbacks.eos = pipeline_hn_server_eos;
    pPipeline->data->server->appsink_callbacks.new_preroll = NULL;
    pPipeline->data->server->appsink_callbacks.new_buffer_list = NULL;
    pPipeline->data->server->appsink_callbacks.new_buffer = pipeline_hn_server_new_buffer;

    // Set appsrc callback to this function
    gst_app_sink_set_callbacks(appsink, &pPipeline->data->server->appsink_callbacks,
            pPipeline, NULL);

    if (RI_HN_SRVR_TYPE_TSB == pipe_type && NULL != pPipeline->data->server->ct)
    {
        isDVRCTPlay = TRUE;
    }

    // if we are performing a transformation; for now, read it from a file...
    if (NULL != pPipeline->data->server->ct)
    {
        // preserve the original (pre CT) info
        if (RI_HN_SRVR_TYPE_UNKNOWN == pPipeline->data->server->orig_pipe_type)
        {
            pPipeline->data->server->orig_pipe_type = pipe_type;
            pPipeline->data->server->orig_file_path = g_strdup(file_path);
            pPipeline->data->server->orig_file_name = g_strdup(file_name);
        }
        else
        {
            RILOG_INFO("%s() -- changing transformation...\n", __func__);
        }

        // set-up the CT file service
        pipe_type = RI_HN_SRVR_TYPE_FILE;
        file_path = pPipeline->data->server->ct_path;
        file_name = pPipeline->data->server->ct_file;
    }

    // Set the type based on whether or not pipeline is DVR
    pipeline_hn_server_set_type(pPipeline, tuner, pipe_type);

    // Set properties of elements based on type
    if (RI_HN_SRVR_TYPE_TSB == pipe_type)
    {
        g_object_set(G_OBJECT(pPipeline->data->dvrsrc), "filepath", file_path,
                NULL);
        pPipeline->data->playback.filename = g_strdup(file_name);
        g_object_set(G_OBJECT(pPipeline->data->dvrsrc), "filename", file_name,
                NULL);
        pPipeline->data->playback.filepath = g_strdup(file_path);
        RILOG_INFO("%s() -- setting hn playback file to path %s, name %s\n",
                __FUNCTION__, file_path, file_name);

        g_object_set(G_OBJECT(pPipeline->data->dvrsrc), "position_bytes",
                    bytePos, NULL);
        RILOG_INFO("%s() -- set hn playback position bytes to %"PRIi64"\n",
                    __FUNCTION__, bytePos);

        uint32_t blockSize = RI_PIPELINE_HN_SERVER_BLOCK_SIZE;
        g_object_set(G_OBJECT(pPipeline->data->dvrsrc), "blocksize", blockSize,
                NULL);
        RILOG_DEBUG("%s() -- setting hn playback block size to %d\n",
                __FUNCTION__, blockSize);

        g_object_set(G_OBJECT(pPipeline->data->dvrsrc), "playrate", rate, "framerate", frame_rate,
                NULL);
        RILOG_INFO("%s() -- setting playrate %f, frame rate %d\n",
                __FUNCTION__, rate, frame_rate);

        g_object_set(G_OBJECT(pPipeline->data->dvrsrc), "timestamp_with_position",
                TRUE, NULL);
        RILOG_INFO("%s() -- setting timestamp with position to TRUE\n", __FUNCTION__);
    }
    else if (RI_HN_SRVR_TYPE_FILE == pipe_type)
    {
        if (isDVRCTPlay == TRUE)
        {
            if (TRUE == get_dvr_ct_file(dvr_file_name,
                                          sizeof(dvr_file_name)))
            {
                RILOG_INFO("%s() - override file location for DVR\n", __func__);
                file_name = dvr_file_name;
            }
        }

        // Append together file name and path to form complete file name
        strcpy(location, file_path);
        strcat(location, "/");
        strcat(location, file_name);
        g_object_set(G_OBJECT(pPipeline->data->filesrc), "location", location,
                NULL);
        RILOG_INFO("%s() -- set file location to %s\n", __FUNCTION__, location);

        // release the pipeline mutex before calling pipeline_hn_server_reset
        g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));

        // Reset the pipeline in case it was in DVR mode prior to start up
        rc = pPipeline->pipeline_hn_server_reset(pPipeline);
        g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));
    }
    else if (RI_HN_SRVR_TYPE_TUNER == pipe_type)
    {
        // release the pipeline mutex before calling pipeline_hn_server_reset
        g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));

        // Reset the pipeline in case it was in DVR mode prior to start up
        rc = pPipeline->pipeline_hn_server_reset(pPipeline);
        g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));

        RILOG_INFO("%s() -- starting Live Streaming w/o TSB\n", __FUNCTION__);
    }
    else
    {
        RILOG_ERROR("%s -- incorrect/illegal pipeline type\n", __FUNCTION__);
        rc = RI_ERROR_GENERAL;
    }

    // Start the pipeline
    RILOG_INFO("%s -- starting pipeline\n", __FUNCTION__);
    start_pipeline(pPipeline);

    // Make sure app sink transitions into playing state
    check_appsink_transition(pPipeline);

    // Finally release the pipeline mutex
    g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, rc);
    return rc;
}

/**
 * Pause the output of data from this remote playback pipeline.
 *
 * @param object    remote playback pipeline to pause
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_STREAMING: Problems streaming data in pipeline
 */
ri_error pipeline_hn_server_pause(ri_pipeline_t* pPipeline)
{
    ri_error rc = RI_ERROR_NONE;

    if (NULL == pipeHNCategory)
    {
        pipeHNCategory = log4c_category_get("RI.Pipeline.HN");
    }

    RILOG_INFO("%s -- Pausing remote playback, pipeline = %s\n", __FUNCTION__,
                pPipeline->data->name);

    // Get the mutex so we can manipulate the pipeline
    g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));

    // Pause the pipeline
    (void) gst_element_set_state(pPipeline->data->gst_pipeline,
            GST_STATE_PAUSED);

    // Finally release the pipeline mutex
    g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, rc);
    return rc;
}

/**
 * Resumes the output of data from this remote playback pipeline.
 *
 * @param object    remote playback pipeline to resume
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_STREAMING: Problems streaming data in pipeline
 */
ri_error pipeline_hn_server_resume(ri_pipeline_t* pPipeline)
{
    ri_error rc = RI_ERROR_NONE;

    if (NULL == pipeHNCategory)
    {
        pipeHNCategory = log4c_category_get("RI.Pipeline.HN");
    }

    RILOG_INFO("%s -- Resuming remote playback, pipeline = %s\n", __FUNCTION__,
                pPipeline->data->name);

    // Get the mutex so we can manipulate the pipeline
    g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));

    // Tell pipeline to start playing
    (void) gst_element_set_state(pPipeline->data->gst_pipeline,
            GST_STATE_PLAYING);

    // Finally release the pipeline mutex
    g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, rc);
    return rc;
}

/**
 * Retrieves the size in bytes of the supplied name and path ifs file.
 *
 * @param   file_path         directory of file to get size of
 * @param   file_name         name of file to get size of
 * @param   file_size_bytes   size of the ifs file in bytes, 0 if file not found
 *
 * @return  RI_ERROR_NONE if no errors, RI_ERROR_GENERAL if problems were encountered
 */
ri_error pipeline_hn_server_get_ifs_file_size(const char* filepath, const char* filename,
        int64_t* file_size_bytes)
{
    ri_error rc = RI_ERROR_NONE;

    IfsInfo *ifs_info = NULL;
    IfsReturnCode ret = IfsReturnCodeNoErrorReported;

    if (NULL == pipeHNCategory)
    {
        pipeHNCategory = log4c_category_get("RI.Pipeline.HN");
    }

    RILOG_TRACE("%s -- file path: %s, name: %s\n", __FUNCTION__,
            filepath, filename);

    ret = IfsPathNameInfo(filepath,
                          filename,
                          &ifs_info);

    if (ret != IfsReturnCodeNoErrorReported)
    {
        RILOG_ERROR("%s -- IfsPathNameInfo returned %s\n", __FUNCTION__,
            IfsReturnCodeToString(ret));
        rc = RI_ERROR_GENERAL;
        *file_size_bytes = 0;
    }
    else
    {
        *file_size_bytes = ifs_info->mpegSize;
        ret = IfsFreeInfo(ifs_info);
        ifs_info = NULL;
        if (ret != IfsReturnCodeNoErrorReported)
        {
            RILOG_WARN("%s -- IfsFreeInfo returned %s - ignoring\n",
                __FUNCTION__, IfsReturnCodeToString(ret));
        }
    }

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, rc);
    return rc;
}

/**
 * Retrieves the size in bytes of the file with supplied path & name.
 *
 * @param   file_path         directory of file to get size of
 * @param   file_name         name of file to get size of
 * @param   file_size_bytes   size of the file in bytes, 0 if file not found
 *
 * @return  RI_ERROR_NONE
 */
ri_error pipeline_hn_server_get_file_size(const char* file_path, const char* file_name,
        int64_t* file_size_bytes)
{
    ri_error rc = RI_ERROR_NONE;
    char location[256];
    FILE* fp = NULL;

    if (NULL == pipeHNCategory)
    {
        pipeHNCategory = log4c_category_get("RI.Pipeline.HN");
    }

    RILOG_INFO("%s -- file path: %s, name: %s\n", __FUNCTION__,
             file_path, file_name);

    // Append together file name and path to form complete file name
    strcpy(location, file_path);
    strcat(location, "/");
    strcat(location, file_name);

    RILOG_INFO("%s() -- file location %s\n", __FUNCTION__, location);

    // Open the file and seek to end to determine size
    fp = fopen(location, "r");
    if (NULL != fp)
    {
        fseek(fp, 0L, SEEK_END);
        *file_size_bytes = ftell(fp);
        fclose(fp);
    }
    else
    {
        RILOG_ERROR(
                "%s() -- unable to get file size of file %s, returning 0\n",
                __FUNCTION__, location);
        *file_size_bytes = 0;
    }
    RILOG_INFO("%s() -- returning file size %llu\n", __FUNCTION__,
            *file_size_bytes);

    return rc;
}
/**
 * Retrieves network byte position for supplied media time of supplied file.
 *
 * @param   filepath        directory of file to determine byte position
 * @param   file_name       name of file to determine byte position
 * @param   mediaTimeNS     determines network byte position for this time
 * @param   file_size_bytes returns byte position for given media time
 *
 * @return  RI_ERROR_NONE
 */
ri_error pipeline_hn_server_get_byte_for_time(
        const char* filepath, const char* filename,
        int64_t mediaTimeNS, int64_t* bytePosition)
{
    ri_error rc = RI_ERROR_NONE;

    IfsHandle ifsHandle = NULL;
    IfsReturnCode ret = IfsReturnCodeNoErrorReported;

    if (NULL == pipeHNCategory)
    {
        pipeHNCategory = log4c_category_get("RI.Pipeline.HN");
    }

    RILOG_INFO("%s -- file path: %s, name: %s\n", __FUNCTION__,
            filepath, filename);

    ret = IfsOpenReader(filepath, filename, &ifsHandle);
    if (ret != IfsReturnCodeNoErrorReported)
    {
        RILOG_ERROR("%s -- IfsOpenReader returned %s\n", __FUNCTION__,
            IfsReturnCodeToString(ret));
        rc = RI_ERROR_GENERAL;
        *bytePosition = 0;
    }
    else
    {
        IfsInfo * ifsInfo = NULL;
        ret = IfsHandleInfo(ifsHandle, &ifsInfo);
        if (ret != IfsReturnCodeNoErrorReported)
        {
            RILOG_ERROR("%s -- IfsHandleInfo returned %s\n", __FUNCTION__,
                IfsReturnCodeToString(ret));
            rc = RI_ERROR_GENERAL;
            *bytePosition = 0;
        }
        else
        {
            NumPackets packetPos = IFS_UNDEFINED_PACKET;
            IfsClock reqRelTimePos = (IfsClock) mediaTimeNS;
            IfsClock reqAbsTimePos = reqRelTimePos + ifsInfo->begClock;
            IfsClock actAbsTimePos = reqAbsTimePos;

            ret = IfsSeekToTime(ifsHandle, IfsDirectBegin, &actAbsTimePos, &packetPos);
            if (ret != IfsReturnCodeNoErrorReported)
            {
                RILOG_ERROR("%s -- IfsSeekToTime returned %s\n", __FUNCTION__,
                    IfsReturnCodeToString(ret));
                rc = RI_ERROR_GENERAL;
                *bytePosition = 0;
            }
            else
            {
                rc = RI_ERROR_NONE;
                *bytePosition = ((int64_t) packetPos) * IFS_TRANSPORT_PACKET_SIZE;
                if (actAbsTimePos != reqAbsTimePos)
                {
                    RILOG_INFO("%s -- adjusted by %"PRIu64"ns, byte position %"PRIi64"\n", __FUNCTION__,
                        (reqAbsTimePos - actAbsTimePos), *bytePosition);
                }

                // Include total byte size for debug purposes
                RILOG_INFO("%s -- total size in bytes %"PRIi64"\n", __FUNCTION__, ifsInfo->mpegSize);

                // Include total duration for debug purposes
                RILOG_INFO("%s -- total duration in ns %"PRIi64"\n", __FUNCTION__, (ifsInfo->endClock - ifsInfo->begClock));
            }

            ret = IfsFreeInfo(ifsInfo);
            if (ret != IfsReturnCodeNoErrorReported)
            {
                RILOG_WARN("%s -- IfsFreeInfo returned %s - ignoring\n", __FUNCTION__,
                    IfsReturnCodeToString(ret));
            }
            ifsInfo = NULL;
        }

        ret = IfsClose(ifsHandle);
        if (ret != IfsReturnCodeNoErrorReported)
        {
            RILOG_WARN("%s -- IfsClose returned %s - ignoring\n", __FUNCTION__,
                IfsReturnCodeToString(ret));
        }
    }

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, rc);
    return rc;
}

/**
 * Retrieves the duration in normal play time in milliseconds of the file source
 * associated with this pipeline.
 *
 * @param   pPipeline         retrieve duration of file associated with this pipeline
 * @param   file_duration_ms  duration in ms of normal play time of entire file,
 *                            0 if no associated file
 *
 * @return  RI_ERROR_NONE
 */
ri_error pipeline_hn_server_get_file_duration(ri_pipeline_t* pPipeline,
        uint64_t* file_duration_ms)
{
    ri_error rc = RI_ERROR_NONE;

    IfsInfo *ifs_info = NULL;
    IfsReturnCode ret = IfsReturnCodeNoErrorReported;

    if (NULL == pipeHNCategory)
    {
        pipeHNCategory = log4c_category_get("RI.Pipeline.HN");
    }

    RILOG_TRACE("%s -- Entry, pipeline = %s\n", __FUNCTION__,
                pPipeline->data->name);

    // Get the mutex so we can manipulate the pipeline
    g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));

    ri_bool isDVR = pipeline_hn_server_is_dvr(pPipeline);

    // Get the total duration of normal play time in milliseconds the file this pipeline
    // is currently streaming
    if ((isDVR) && (NULL != pPipeline->data->dvrsrc))
    {
        ret = IfsPathNameInfo(pPipeline->data->playback.filepath,
                              pPipeline->data->playback.filename,
                              &ifs_info);

        if (ret != IfsReturnCodeNoErrorReported)
        {
            RILOG_ERROR("%s -- IfsPathNameInfo returned %s\n", __FUNCTION__,
                IfsReturnCodeToString(ret));
            rc = RI_ERROR_GENERAL;
        }
        else
        {
            *file_duration_ms =
                (ifs_info->endClock - ifs_info->begClock) / 1000000LL;
            ret = IfsFreeInfo(ifs_info);
            ifs_info = NULL;
            if (ret != IfsReturnCodeNoErrorReported)
            {
                RILOG_WARN("%s -- IfsFreeInfo returned %s - ignoring\n",
                    __FUNCTION__, IfsReturnCodeToString(ret));
            }
        }

        RILOG_INFO("%s() -- retrieved file duration %lld\n", __FUNCTION__,
                *file_duration_ms);
    }

    // Finally release the pipeline mutex
    g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, rc);
    return rc;
}

/**
 * Returns the file path and name associated with the supplied tsb handle.
 *
 * @param tsbInfo   structure containing tsb information
 * @param file_path path to the file which contains the tsb contents
 * @param file_name name of the file which contains the tsb contents
 *
 * @return returns RI_ERROR_NONE
 */
ri_error pipeline_hn_server_get_tsb_file_name_path(ri_tsbHandle tsbInfo,
          char* file_path, char* file_name)
{
    ri_error rc = RI_ERROR_NONE;

    if (NULL == pipeHNCategory)
    {
        pipeHNCategory = log4c_category_get("RI.Pipeline.HN");
    }

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    IfsInfo* pIfsHandleInfo = (IfsInfo*)tsbInfo;

    sprintf(file_path, "%s", pIfsHandleInfo->path);
    sprintf(file_name, "%s", pIfsHandleInfo->name);

    RILOG_INFO("%s -- Exit, name %s, path %s, return value = %d\n", __FUNCTION__,
            file_path, file_name, rc);
    return rc;
}

/**
 * Sets the playback start time of this remote playback pipeline.
 *
 * @param object    remote playback pipeline to set time
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_STREAMING: Problems streaming data in pipeline
 */
ri_error pipeline_hn_server_set_time(ri_pipeline_t* pPipeline,
        uint64_t start_time)
{
    ri_error rc = RI_ERROR_NONE;

    if (NULL == pipeHNCategory)
    {
        pipeHNCategory = log4c_category_get("RI.Pipeline.HN");
    }

    RILOG_TRACE("%s -- Entry, pipeline = %s\n", __FUNCTION__,
                pPipeline->data->name);

    // Get the mutex so we can manipulate the pipeline
    g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));

    ri_bool isDVR = pipeline_hn_server_is_dvr(pPipeline);

    // Setting postion is only supported for DVR
    if (isDVR)
    {
        // Set the start position of the file src to this new value
        if (NULL != pPipeline->data->dvrsrc)
        {
            g_object_set(G_OBJECT(pPipeline->data->dvrsrc), "position_time",
                    start_time, NULL);
            RILOG_INFO("%s() -- setting hn playback position time to %llu\n",
                    __FUNCTION__, start_time);
        }
    }

    // Finally release the pipeline mutex
    g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, rc);
    return rc;
}

/**
 * Sets the playback byte position of this remote playback pipeline.
 *
 * @param object    remote playback pipeline to set byte position
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_STREAMING: Problems streaming data in pipeline
 */
ri_error pipeline_hn_server_set_byte_pos(ri_pipeline_t* pPipeline,
        uint64_t byte_pos)
{
    ri_error rc = RI_ERROR_NONE;

    if (NULL == pipeHNCategory)
    {
        pipeHNCategory = log4c_category_get("RI.Pipeline.HN");
    }

    RILOG_TRACE("%s -- Entry, pipeline = %s\n", __FUNCTION__,
                pPipeline->data->name);

    // Get the mutex so we can manipulate the pipeline
    g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));

    ri_bool isDVR = pipeline_hn_server_is_dvr(pPipeline);

    // Set the start position of the file src to this new value
    if (isDVR)
    {
        if (NULL != pPipeline->data->dvrsrc)
        {
            g_object_set(G_OBJECT(pPipeline->data->dvrsrc), "position_bytes",
                    byte_pos, NULL);
            RILOG_INFO("%s() -- setting hn playback position bytes to %llu\n",
                    __FUNCTION__, byte_pos);
        }
    }

    // Finally release the pipeline mutex
    g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, rc);
    return rc;
}

/**
 * Resets this remote playback pipeline.
 * It is necessary when an EOS is encountered.
 *
 * @param object    HN pipeline to reset file src
 *
 * @returns RI_ERROR_NONE
 */
ri_error pipeline_hn_server_reset(ri_pipeline_t* pPipeline)
{
    ri_error rc = RI_ERROR_NONE;
    GstEvent* flush_stop = NULL;
    GstEvent* flush_start = NULL;

    if (NULL == pipeHNCategory)
    {
        pipeHNCategory = log4c_category_get("RI.Pipeline.HN");
    }

    RILOG_TRACE("%s -- Entry, pipeline = %s\n", __FUNCTION__,
            pPipeline->data->name);

    // Get the mutex so we can manipulate the pipeline
    g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));

    ri_bool isDVR = pipeline_hn_server_is_dvr(pPipeline);

    if (isDVR)
    {
        // Reset the data source if not null
        if (NULL != pPipeline->data->dvrsrc)
        {
            gst_trick_play_file_src_reset(
                    (GstTrickPlayFileSrc*) pPipeline->data->dvrsrc);
            RILOG_INFO("%s() -- reset trick play file src of remote playback server\n",
                    __FUNCTION__);
        }
    }
    // Send flush events to clear EOS.
    RILOG_INFO("%s -- flushing remote playback pipeline\n", __FUNCTION__);
    flush_start = gst_event_new_flush_start();
    pipeline_hn_server_send_event(pPipeline, flush_start);

    flush_stop = gst_event_new_flush_stop();
    pipeline_hn_server_send_event(pPipeline, flush_stop);

    // Pause the pipeline
    RILOG_INFO("%s -- setting pipeline state to PAUSED\n", __FUNCTION__);
    (void) gst_element_set_state(pPipeline->data->gst_pipeline,
            GST_STATE_PAUSED);

    // Reset the pipeline to ready
    RILOG_INFO("%s -- setting pipeline state to READY\n", __FUNCTION__);
    (void) gst_element_set_state(pPipeline->data->gst_pipeline, GST_STATE_READY);

    // Make sure app sink transitions into ready state
    int curCnt = 0;
    int maxCnt = 5;
    GstState state = GST_STATE_NULL;
    do
    {
        (void)gst_element_get_state((GstElement*)pPipeline->data->appsink, // element
                &state, // state
                NULL, // pending
                100000000LL); // timeout(1 second = 10^9 nanoseconds)

        curCnt++;

        if (state != GST_STATE_READY)
        {
            // Sleep for a short time
            RILOG_DEBUG("%s -- sleeping 1 sec, loop %d, waiting for state ready, cur state: %s\n", __FUNCTION__,
                    curCnt, gst_element_state_get_name(state));
            g_usleep(1000000L);
        }
    }
    while ((GST_STATE_READY != state) && (curCnt < maxCnt));
    RILOG_INFO("%s -- Appsink state after waiting for pipeline to be ready: %s\n",
            __FUNCTION__, gst_element_state_get_name(state));

    // Finally release the pipeline mutex
    g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, rc);
    return rc;
}

/**
 * Stop the output of data from this remote playback pipeline.
 *
 * @param object    remote playback pipeline to stop
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_STREAMING: Problems streaming data in pipeline
 */
ri_error pipeline_hn_server_stop(ri_pipeline_t* pPipeline)
{
    ri_error rc = RI_ERROR_NONE;

    if (NULL == pipeHNCategory)
    {
        pipeHNCategory = log4c_category_get("RI.Pipeline.HN");
    }

    RILOG_INFO("%s -- stopping remote playback, pipeline = %s\n", __FUNCTION__,
                pPipeline->data->name);

    // Get the mutex so we can manipulate the pipeline
    g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));

    stop_pipeline(pPipeline);

    if (pPipeline->data->playback.filename != NULL)
    {
        g_free(pPipeline->data->playback.filename);
        pPipeline->data->playback.filename = NULL;
    }

    if (pPipeline->data->playback.filepath != NULL)
    {
        g_free(pPipeline->data->playback.filepath);
        pPipeline->data->playback.filepath = NULL;
    }

    // Finally release the pipeline mutex
    g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));

    return rc;
}

/*
 * Method called by gst_buffer_list_for_each() to process buffer list data
 *
 * @param   pBuf        buffer pointer to be processed
 * @param   group       the group of buffers being processed
 * @param   idx         the index of the buffer being processed
 * @param   user_data   reference to this appsrc to send data to
 */
GstBufferListItem gst_buffer_list_process_buf(GstBuffer** pBuf,
                                              guint group,
                                              guint idx,
                                              gpointer user_data)
{
    GstAppSrc* src = (GstAppSrc*)user_data;

    if (NULL != pBuf)
    {
        // Send this buffer to the LS tuner appsrc in the HN server pipeline
        (void) gst_app_src_push_buffer(src, *pBuf);

        // indicate the push_buffer took ownership of the buffer and it can
        // be removed from the list...
        pBuf = NULL;

        return GST_BUFFER_LIST_CONTINUE;
    }

    return GST_BUFFER_LIST_END;
}

/*
 * Method called by GStreamer appsrc element to indicate it needs more data.
 *
 * @param   src         appsrc element in hn player pipeline
 * @param   length      amount of data requested
 * @param   user_data   reference to this pipeline which was supplied to appsrc
 *                      when callback function was setup
 */
void pipeline_ls_tuner_appsrc_need_data(GstAppSrc* src, guint length,
        gpointer user_data)
{
    ri_pipeline_t* pPipeline = user_data;

    RILOG_TRACE("%s -- Entry, pipeline = %s\n", __FUNCTION__,
                pPipeline->data->name);

    // Get a buffer from the LS tuner appsink
    GstAppSink *sink = (GstAppSink*)pPipeline->data->ls_tuner_appsink;

    if (NULL != sink)
    {
#if 0
        GstBufferList* buflist = gst_app_sink_pull_buffer_list(sink);

        if (NULL != buflist)
        {
            // process all the buffers in the list...
            buflist = gst_buffer_list_make_writable(buflist);
            gst_buffer_list_foreach(buflist, gst_buffer_list_process_buf, src);
            gst_buffer_list_unref(buflist);
        }
#else
        GstBuffer* gstbuf = gst_app_sink_pull_buffer(sink);

        if (NULL != gstbuf)
        {
            // Send this buffer to the LS tuner appsrc in the HN server pipeline
            (void) gst_app_src_push_buffer(src, gstbuf);
        }
#endif
        else
        {
            RILOG_INFO("%s: %s, LiveStreaming appsink empty\n", __FUNCTION__,
                       pPipeline->data->name);
        }
    }
    else
    {
        RILOG_WARN("%s: %s, LiveStreaming appsink not set\n", __FUNCTION__,
                   pPipeline->data->name);
    }
}

/*
 * Method called by GStreamer appsrc element to indicate queue is currently full
 * and no more data is needed.  Currently not implemented.
 *
 * @param   src         appsrc element in hn player pipeline
 * @param   user_data   reference to this pipeline which was supplied to appsrc
 *                      when callback function was setup
 */
void pipeline_ls_tuner_appsrc_enough_data(GstAppSrc* src, gpointer user_data)
{
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
gboolean pipeline_ls_tuner_appsrc_seek_data(GstAppSrc* src, guint64 offset,
        gpointer user_data)
{
    RILOG_TRACE("%s -- Entry, pipeline = %s\n", __FUNCTION__,
               ((ri_pipeline_t*)user_data)->data->name);
    // do nothing
    return TRUE;
}

/**
 * Destroy notify method used when setting up appsrc callbacks.
 * Currently not implemented.
 *
 * @param   data  pointer to callback related data.
 */
void pipeline_ls_tuner_appsrc_destroy_cb(gpointer user_data)
{
    RILOG_TRACE("%s -- Entry, pipeline = %s\n", __FUNCTION__,
               ((ri_pipeline_t*)user_data)->data->name);
    // do nothing
}

/**
 * Returns buffer containing next set of data to send out on network for a
 * remote playback of a recording.
 *
 * @param   pPipeline   associated remote playback pipeline
 * @param   bufData     returning data to send out on network
 * @param   bufLen      amount of data contained in buffer
 *
 * @return  RI_ERROR_NONE           if no problems were encountered
 *          RI_ERROR_EOS            if no more buffers are left and no one is writing
 *          RI_ERROR_NO_DATA        if no data is available but there is an active writer
 *          RI_ERROR_NO_PLAYBACK    if pipeline is not currently playing
 *          RI_ERROR_GENERAL        problems were encountered
 *
 * NOTE: buffers returned via this methods should be freed via pipeline_hn_server_free_buffer()
 */
ri_error pipeline_hn_server_get_buffer(ri_pipeline_t* pPipeline,
        void** bufData, uint32_t* bufLen, uint64_t* nptNS, uint64_t* bytePos)
{
    ri_error rc = RI_ERROR_NONE;

    GstAppSink *sink = (GstAppSink*) pPipeline->data->appsink;
    GstBuffer* buffer = NULL;
    *bufLen = 0;

    g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));

    // Only attempt to get a buffer if one is available
    //RILOG_DEBUG("%s -- getting buffer called with g_bufCnt: %d\n", __FUNCTION__, g_bufCnt);
    g_mutex_lock(pPipeline->data->server->buf_mutex);
    if (pPipeline->data->server->bufCnt > 0)
    {
        // Get a pipeline buffer from the app sink which contains next buffer of file data to send out
        //RILOG_DEBUG("%s -- pulling buffer from appsink\n", __FUNCTION__);
        buffer = gst_app_sink_pull_buffer(sink);
        pPipeline->data->server->bufCnt--;
        g_mutex_unlock(pPipeline->data->server->buf_mutex);
    }
    else
    {
        if (pPipeline->data->server->isEOS)
        {
            RILOG_INFO("%s -- unable to pull buffer from appsink due to EOS\n", __FUNCTION__);
            rc = RI_ERROR_EOS;
        }
        else
        {
            //RILOG_INFO("%s -- unable to pull buffer from appsink due to no buffers available, current cnt: %d\n",
            //        __FUNCTION__, g_bufCnt);
            rc = RI_ERROR_NO_DATA;
        }
        g_mutex_unlock(pPipeline->data->server->buf_mutex);
        g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));
        return rc;
    }

    if (NULL != buffer)
    {
        *bufLen = GST_BUFFER_SIZE(buffer);
        if (pPipeline->data->server->rate >= 1.0)
        {
            pPipeline->data->server->bytePos += *bufLen;
        }
        else
        {
            pPipeline->data->server->bytePos -= *bufLen;
        }
        *bytePos = pPipeline->data->server->bytePos;
        //RILOG_INFO("%s -- setting current byte pos: %llu\n", __FUNCTION__, g_bytePos);

        *nptNS = GST_BUFFER_TIMESTAMP(buffer);
        //RILOG_INFO("%s -- setting current npt: %llu\n", __FUNCTION__, *nptNS);

        // Allocate memory to return buffer data back up to mpeos layer
        *bufData = g_try_malloc(*bufLen);

        if (NULL == bufData)
        {
            RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                        __LINE__, __FILE__, __func__);
            rc = RI_ERROR_GENERAL;
        }
        else
        {
            memcpy(*bufData, (char*) GST_BUFFER_DATA(buffer), *bufLen);
        }

        // Get rid of reference to buffer so it will be freed
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
                RILOG_INFO(
                        "%s -- unable to get buffer - playing state but EOS, pipeline = %s\n",
                        __FUNCTION__, pPipeline->data->name);
                rc = RI_ERROR_EOS;
            }
            else
            {
                // Pipeline is not playing yet
                rc = RI_ERROR_NO_PLAYBACK;
                RILOG_INFO(
                        "%s -- unable to get buffer due to pipeline not yet playing but EOS, pipeline = %s\n",
                        __FUNCTION__, pPipeline->data->name);
            }
        }
        else
        {
            if (GST_STATE_PLAYING == state)
            {
                RILOG_ERROR(
                        "%s -- unable to get buffer from playing pipeline = %s\n",
                        __FUNCTION__, pPipeline->data->name);

                rc = RI_ERROR_GENERAL;
            }
            else
            {
                // Pipeline is not playing yet
                rc = RI_ERROR_NO_PLAYBACK;
                RILOG_INFO(
                        "%s -- unable to get buffer due to pipeline not yet playing, pipeline = %s\n",
                        __FUNCTION__, pPipeline->data->name);
            }
        }
    }
    g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));

    return rc;
}

/**
 * Frees memory associated with buffer which was allocated in pipeline_hn_server_get_buffer()
 *
 * @param   pPipeline   associated pipeline
 * @param   bufData     buffer to free
 */
void pipeline_hn_server_free_buffer(ri_pipeline_t* pPipeline, void* bufData)
{
    RILOG_DEBUG("%s -- freeing buffer\n", __FUNCTION__);

    g_free(bufData);

    bufData = NULL;
}

/**
 * called upon pipeline HN server stream start
 *
 * @param   pVideoDevice   associated video_device
 */
void pipeline_hn_server_flow_start(ri_pipeline_t* pPipeline, int tuner,
                                   ri_pid_info_t* pids, int pid_count)
{
    int i = 0;
    uint32_t numLivePipes = 0;
    const ri_pipeline_t** pPipelines = NULL;
    ri_pipeline_manager_t* pPipelineManager = ri_get_pipeline_manager();
    GstAppSink *sink = NULL;
    GObject *object = NULL;

    RILOG_INFO("%s(%p, %d, %p, %d) -- called\n", __FUNCTION__, pPipeline,
                                                 tuner, pids, pid_count);
    pPipelines = pPipelineManager->get_live_pipelines(pPipelineManager,
                &numLivePipes);

    // live pipeline selection with tuner reference
    sink = (GstAppSink*)pPipelines[tuner]->data->ls_tuner_appsink;
    object = G_OBJECT(pPipelines[tuner]->data->sptsassembler);
    g_object_set(sink, "log-drop", TRUE, NULL);
    g_object_set(object, "log_pat_pmt", FALSE, NULL);    // TRUE == verbose
    g_object_set(object, "rewrite_pat_pmt", TRUE, NULL);
    gst_app_sink_set_drop(sink, FALSE);

    // set Queue (in LS Tuner bin) NOT to be leaky so old buffers are
    // no longer dropped when the HN client is not keeping up to the server.
    g_object_set(pPipelines[tuner]->data->ls_tuner_queue, "leaky", 0, NULL);

    for (i = 0; i < pid_count; i++)
    {
        switch (pids[i].mediaType)
        {
            case RI_MEDIA_TYPE_VIDEO:
                g_object_set(object, "videopid", pids[i].srcPid, NULL);
                break;
            case RI_MEDIA_TYPE_AUDIO:
                g_object_set(object, "audiopid", pids[i].srcPid, NULL);
                break;
            case RI_MEDIA_TYPE_DATA:
                RILOG_INFO("%s - not setting data media type\n", __func__);
                break;
            case RI_MEDIA_TYPE_SUBTITLES:
                RILOG_INFO("%s - not setting subtitles media type\n", __func__);
                break;
            case RI_MEDIA_TYPE_SECTIONS:
                RILOG_INFO("%s - not setting sections media type\n", __func__);
                break;
            case RI_MEDIA_TYPE_PCR:
                break;
            case RI_MEDIA_TYPE_PMT:
                g_object_set(object, "pmtpid", pids[i].srcPid, NULL);
                break;
            default:
                RILOG_WARN("%s - unknown media type %d\n", __func__,
                           pids[i].mediaType);
                break;
        }
    }
}

/**
 * called upon pipeline HN server stream stop
 *
 * @param   pVideoDevice   associated video_device
 */
void pipeline_hn_server_flow_stop(ri_pipeline_t* pPipeline, int tuner)
{
    uint32_t numLivePipes = 0;
    const ri_pipeline_t** pPipelines = NULL;
    ri_pipeline_manager_t* pPipelineManager = ri_get_pipeline_manager();
    GstAppSink *sink = NULL;
    GObject *object = NULL;

    RILOG_INFO("%s(%p, %d) -- called\n", __FUNCTION__, pPipeline, tuner);
    pPipelines = pPipelineManager->get_live_pipelines(pPipelineManager,
                &numLivePipes);

    // live pipeline selection with tuner reference
    sink = (GstAppSink*)pPipelines[tuner]->data->ls_tuner_appsink;
    object = G_OBJECT(pPipelines[tuner]->data->sptsassembler);

    g_object_set(object, "rewrite_pat_pmt", FALSE, NULL);
    g_object_set(object, "log_pat_pmt", FALSE, NULL);
    g_object_set(sink, "log-drop", FALSE, NULL);
    gst_app_sink_set_drop(sink, TRUE);

    // set Queue (in LS Tuner bin) to be leaky so old buffers are
    // dropped when the HN client is not connected to the server.
    g_object_set(pPipelines[tuner]->data->ls_tuner_queue, "leaky", 2, NULL);
}

/**
 * Creates the necessary gstreamer elements to support playback of a remote DVR recording
 * streaming to a remote device.
 *
 * @pipeline   pipeline which supports remote playback of recording
 *
 * @return  always returns TRUE
 */
gboolean pipeline_hn_server_create(ri_pipeline_t* pPipeline)
{
#ifdef EXTERNAL_CT_ENGINE
    char* CTEaddr = NULL;
    char* CTEtxPort = NULL;
    char* CTErxPort = NULL;
    char* host = "127.0.0.1";
    int txPort = 5001;
    int rxPort = txPort;
#endif

    if (NULL == pipeHNCategory)
    {
        pipeHNCategory = log4c_category_get("RI.Pipeline.HN");
    }

    RILOG_TRACE("%s -- Entry, pipeline = %s\n", __FUNCTION__,
                pPipeline->data->name);

    pPipeline->data->dvrsrc = gst_load_element("trickplayfilesrc", "dvrsrc");
    pPipeline->data->filesrc = gst_load_element("filesrc", "filesrc");
#ifdef EXTERNAL_CT_ENGINE
    pPipeline->data->udpsink = gst_load_element("udpsink", "CT_udpsink");
    pPipeline->data->udpsrc = gst_load_element("udpsrc", "CT_udpsrc");
#endif
    pPipeline->data->appsink = gst_load_element("appsink", "remotesink");
    pPipeline->data->ls_tuner_appsrc = gst_load_element("appsrc",
            "ls_tuner_appsrc");

    //  For testing only - to verify contents of streamed file
#ifdef TEE_HN_SERVER_OUTPUT_TO_FILE
    RILOG_INFO("%s -- pipeline = %s adding HN tee to file...\n", __FUNCTION__,
                pPipeline->data->name);
    pPipeline->data->filesink = gst_load_element("filesink", "filesink");
    pPipeline->data->hntee = gst_load_element("tee", "hntee");
    g_object_set(G_OBJECT(pPipeline->data->filesink), "location",
            "tee.txt", NULL);
    g_object_set(G_OBJECT(pPipeline->data->filesink), "sync", FALSE, NULL);
    g_object_set(G_OBJECT(pPipeline->data->filesink), "async", FALSE, NULL);
#endif

#ifdef EXTERNAL_CT_ENGINE
    if ((NULL != (CTEaddr = ricfg_getValue("RIPlatform",
                                           "RI.Platform.HNServerCTEaddr"))) &&
        (NULL != (CTEtxPort = ricfg_getValue("RIPlatform",
                                           "RI.Platform.HNServerCTEtxPort"))) &&
        (NULL != (CTErxPort = ricfg_getValue("RIPlatform",
                                           "RI.Platform.HNServerCTErxPort"))))
    {
        host = CTEaddr;
        txPort = atoi(CTEtxPort);
        rxPort = atoi(CTErxPort);
    }

    g_object_set(G_OBJECT(pPipeline->data->udpsink),
                 "host", host, "port", txPort, NULL);
    RILOG_INFO("%s set UDP sink host = %s port = %d\n", __func__, host, txPort);
    g_object_set(G_OBJECT(pPipeline->data->udpsrc), "port", rxPort, NULL);
    RILOG_INFO("%s set UDP src port = %d\n", __func__, rxPort);
#endif
    g_object_set(G_OBJECT(pPipeline->data->appsink), "sync", FALSE, NULL);
    g_object_set(G_OBJECT(pPipeline->data->appsink), "max-buffers", 2, NULL);

    pPipeline->data->input_bin = gst_bin_new("input_bin");
    pPipeline->data->cte_bin = gst_bin_new("cte_bin");

    // Add just the appsink to input bin since src can vary
#ifdef EXTERNAL_CT_ENGINE
    gst_bin_add_many(GST_BIN(pPipeline->data->input_bin),
            pPipeline->data->udpsink, NULL);
    gst_bin_add_many(GST_BIN(pPipeline->data->cte_bin),
            pPipeline->data->udpsrc,
            pPipeline->data->appsink, NULL);
#else
    gst_bin_add_many(GST_BIN(pPipeline->data->input_bin),
            pPipeline->data->appsink, NULL);
#endif

    // Ref the srcs so they will not be destroy when removed from bin
    // *TODO* - need to unref when destroying pipeline
    gst_object_ref(pPipeline->data->dvrsrc);
    gst_object_ref(pPipeline->data->filesrc);
    gst_object_ref(pPipeline->data->ls_tuner_appsrc);

    // Assign pipeline functions
    pPipeline->pipeline_hn_server_start = pipeline_hn_server_start;
    pPipeline->pipeline_hn_server_pause = pipeline_hn_server_pause;
    pPipeline->pipeline_hn_server_resume = pipeline_hn_server_resume;
    pPipeline->pipeline_hn_server_set_time = pipeline_hn_server_set_time;
    pPipeline->pipeline_hn_server_set_byte_pos
            = pipeline_hn_server_set_byte_pos;
    pPipeline->pipeline_hn_server_stop = pipeline_hn_server_stop;
    pPipeline->pipeline_hn_server_get_buffer = pipeline_hn_server_get_buffer;
    pPipeline->pipeline_hn_server_free_buffer = pipeline_hn_server_free_buffer;
    pPipeline->pipeline_hn_server_flow_start = pipeline_hn_server_flow_start;
    pPipeline->pipeline_hn_server_flow_stop = pipeline_hn_server_flow_stop;
    pPipeline->pipeline_transform_live_stream = pipeline_transform_live_stream;
    pPipeline->pipeline_transform_file_stream = pipeline_transform_file_stream;
    pPipeline->pipeline_transform_status = pipeline_transform_status;
    pPipeline->pipeline_hn_server_get_ifs_file_size
            = pipeline_hn_server_get_ifs_file_size;
    pPipeline->pipeline_hn_server_get_file_size
            = pipeline_hn_server_get_file_size;
    pPipeline->pipeline_hn_server_get_byte_for_time
            = pipeline_hn_server_get_byte_for_time;
    pPipeline->pipeline_hn_server_get_tsb_file_name_path
            = pipeline_hn_server_get_tsb_file_name_path;
    pPipeline->pipeline_hn_server_reset = pipeline_hn_server_reset;

    GstAppSrc *appsrc = GST_APP_SRC(pPipeline->data->ls_tuner_appsrc);
    pPipeline->data->ls_tuner_appsrc_callbacks.need_data
            = pipeline_ls_tuner_appsrc_need_data;
    pPipeline->data->ls_tuner_appsrc_callbacks.enough_data
            = pipeline_ls_tuner_appsrc_enough_data;
    pPipeline->data->ls_tuner_appsrc_callbacks.seek_data
            = pipeline_ls_tuner_appsrc_seek_data;

    // Set appsrc callback to this function
    gst_app_src_set_callbacks(appsrc,
            &pPipeline->data->ls_tuner_appsrc_callbacks,
            pPipeline, pipeline_ls_tuner_appsrc_destroy_cb);

    // *TODO* - remove just for testing
    //(void)g_timeout_add(5000, pipeline_hn_server_test, (gpointer)pPipeline);

    // Allocate space for the server specific info
    pPipeline->data->server = g_try_malloc0(sizeof(pipeline_hn_server_data_t));

    RILOG_TRACE("%s -- Exit, return value = TRUE\n", __FUNCTION__);
    return TRUE;
}

/**
 * Adds the specific src element and links to app sink based on type.
 *
 * @param   pPipeline   hn server pipeline
 * @param   isDVR       flag which when true use DVR src, otherwise use file src
 */
static void pipeline_hn_server_set_type(ri_pipeline_t* pPipeline, int tuner,
                                        ri_hn_srvr_type pipe_type)
{
    // Create pipeline source based on the type
    if (RI_HN_SRVR_TYPE_TSB == pipe_type)
    {
        RILOG_INFO("%s -- pipeline type is DVR\n", __FUNCTION__);

        // Determine if the DVR src is already in the bin
        if (NULL == gst_bin_get_by_name(GST_BIN(pPipeline->data->input_bin),
                "dvrsrc"))
        {
            RILOG_INFO("%s -- pipeline needs DVRSrc added\n", __FUNCTION__);

            // Set the state to NULL since changing elements
            (void) gst_element_set_state(pPipeline->data->gst_pipeline,
                    GST_STATE_NULL);

            // DVR src is not in the bin, see if file src is in bin
            if (NULL != gst_bin_get_by_name(
                    GST_BIN(pPipeline->data->input_bin), "filesrc"))
            {
                RILOG_INFO("%s -- pipeline removing filesrc\n", __FUNCTION__);

                // Remove the filesrc from bin, this will auto unlink it
                gst_bin_remove(GST_BIN(pPipeline->data->input_bin),
                        pPipeline->data->filesrc);
            }

            // see if app src is in bin and remove it...
            if (NULL != gst_bin_get_by_name(
                    GST_BIN(pPipeline->data->input_bin), "ls_tuner_appsrc"))
            {
                RILOG_INFO("%s -- pipeline removing appsrc\n", __FUNCTION__);

                // Remove the filesrc from bin, this will auto unlink it
                gst_bin_remove(GST_BIN(pPipeline->data->input_bin),
                        pPipeline->data->ls_tuner_appsrc);
            }

            // Add the dvr src to bin
            gst_bin_add(GST_BIN(pPipeline->data->input_bin),
                    pPipeline->data->dvrsrc);

#ifdef TEE_HN_SERVER_OUTPUT_TO_FILE
            gst_bin_add(GST_BIN(pPipeline->data->input_bin),
                    pPipeline->data->hntee);

            gst_bin_add(GST_BIN(pPipeline->data->input_bin),
                    pPipeline->data->filesink);
#endif
#ifdef EXTERNAL_CT_ENGINE
            // Link the appsink to this src
            CHECK_OK_OBJECT(pPipeline->data->input_bin, gst_element_link_many(
                pPipeline->data->dvrsrc,
                pPipeline->data->udpsink, NULL),
                "Linking remote playback input_bin elements failed");
            // Link the appsink to this src
            CHECK_OK_OBJECT(pPipeline->data->cte_bin, gst_element_link_many(
                pPipeline->data->udpsrc,
#ifdef TEE_HN_SERVER_OUTPUT_TO_FILE
                pPipeline->data->hntee,
#endif
                pPipeline->data->appsink, NULL),
                "Linking remote playback input_bin elements failed");
#else
            // Link the appsink to this src
            CHECK_OK_OBJECT(pPipeline->data->input_bin, gst_element_link_many(
                pPipeline->data->dvrsrc,
#ifdef TEE_HN_SERVER_OUTPUT_TO_FILE
                pPipeline->data->hntee,
#endif
                pPipeline->data->appsink, NULL),
                "Linking remote playback input_bin elements failed");
#endif

#ifdef TEE_HN_SERVER_OUTPUT_TO_FILE
            CHECK_OK_OBJECT(pPipeline->data->input_bin, gst_element_link_many(
                pPipeline->data->hntee,
                pPipeline->data->filesink, NULL),
                "Linking tee to filesink elements failed");
#endif
            RILOG_INFO("%s -- pipeline now has DVRSrc\n", __FUNCTION__);
        }
    }
    else if (RI_HN_SRVR_TYPE_FILE == pipe_type)
    {
        RILOG_INFO("%s -- pipeline type is FILE\n", __FUNCTION__);

        // Determine if the file src is already in the bin
        if (NULL == gst_bin_get_by_name(GST_BIN(pPipeline->data->input_bin),
                "filesrc"))
        {
            RILOG_INFO("%s -- pipeline needs fileSrc added\n", __FUNCTION__);

            // Set the state to NULL since changing elements
            (void) gst_element_set_state(pPipeline->data->gst_pipeline,
                    GST_STATE_NULL);

            // File src is not in the bin, see if dvr src is in bin
            if (NULL != gst_bin_get_by_name(
                    GST_BIN(pPipeline->data->input_bin), "dvrsrc"))
            {
                RILOG_INFO("%s -- pipeline removing dvrsrc\n", __FUNCTION__);

                // Remove the dvrsrc from bin, this will auto unlink it
                gst_bin_remove(GST_BIN(pPipeline->data->input_bin),
                        pPipeline->data->dvrsrc);
            }

            // see if app src is in bin and remove it...
            if (NULL != gst_bin_get_by_name(
                    GST_BIN(pPipeline->data->input_bin), "ls_tuner_appsrc"))
            {
                RILOG_INFO("%s -- pipeline removing appsrc\n", __FUNCTION__);

                // Remove the filesrc from bin, this will auto unlink it
                gst_bin_remove(GST_BIN(pPipeline->data->input_bin),
                        pPipeline->data->ls_tuner_appsrc);
            }

            // Add the file src to bin
            gst_bin_add(GST_BIN(pPipeline->data->input_bin),
                    pPipeline->data->filesrc);

#ifdef EXTERNAL_CT_ENGINE
            // Link the appsink to this src
            CHECK_OK_OBJECT(pPipeline->data->input_bin, gst_element_link_many(
                pPipeline->data->filesrc,
                pPipeline->data->udpsink, NULL),
                "Linking remote playback input_bin elements failed");
            // Link the appsink to this src
            CHECK_OK_OBJECT(pPipeline->data->cte_bin, gst_element_link_many(
                pPipeline->data->udpsrc,
#ifdef TEE_HN_SERVER_OUTPUT_TO_FILE
                pPipeline->data->hntee,
#endif
                pPipeline->data->appsink, NULL),
                "Linking remote playback input_bin elements failed");
#else
            // Link the appsink to this src
            CHECK_OK_OBJECT(pPipeline->data->input_bin, gst_element_link_many(
                pPipeline->data->filesrc,
#ifdef TEE_HN_SERVER_OUTPUT_TO_FILE
                pPipeline->data->hntee,
#endif
                pPipeline->data->appsink, NULL),
                "Linking remote playback input_bin elements failed");
#endif
            RILOG_INFO("%s -- pipeline now has fileSrc\n", __FUNCTION__);
        }
    }
    else if (RI_HN_SRVR_TYPE_TUNER == pipe_type)
    {
        uint32_t numLivePipes = 0;
        ri_pipeline_manager_t* pPipelineManager = ri_get_pipeline_manager();
        const ri_pipeline_t** pPipelines = NULL;

        RILOG_INFO("%s -- pipeline type is LIVE\n", __FUNCTION__);

        // Determine if the file src is already in the bin
        if (NULL == gst_bin_get_by_name(GST_BIN(pPipeline->data->input_bin),
                "ls_tuner_appsrc"))
        {
            RILOG_INFO("%s -- pipeline needs appsrc added\n", __FUNCTION__);

            // Set the state to NULL since changing elements
            (void) gst_element_set_state(pPipeline->data->gst_pipeline,
                    GST_STATE_NULL);

            // see if dvr src is in bin and remove it...
            if (NULL != gst_bin_get_by_name(
                    GST_BIN(pPipeline->data->input_bin), "dvrsrc"))
            {
                RILOG_INFO("%s -- pipeline removing dvrsrc\n", __FUNCTION__);

                // Remove the dvrsrc from bin, this will auto unlink it
                gst_bin_remove(GST_BIN(pPipeline->data->input_bin),
                        pPipeline->data->dvrsrc);
            }

            // see if file src is in bin and remove it...
            if (NULL != gst_bin_get_by_name(
                    GST_BIN(pPipeline->data->input_bin), "filesrc"))
            {
                RILOG_INFO("%s -- pipeline removing filesrc\n", __FUNCTION__);

                // Remove the filesrc from bin, this will auto unlink it
                gst_bin_remove(GST_BIN(pPipeline->data->input_bin),
                        pPipeline->data->filesrc);
            }

            // Add the app src to bin
            gst_bin_add(GST_BIN(pPipeline->data->input_bin),
                    pPipeline->data->ls_tuner_appsrc);

#ifdef EXTERNAL_CT_ENGINE
            // Link the appsink to this src
            CHECK_OK_OBJECT(pPipeline->data->input_bin, gst_element_link_many(
                pPipeline->data->ls_tuner_appsrc,
                pPipeline->data->udpsink, NULL),
                "Linking remote playback input_bin elements failed");
            // Link the appsink to this src
            CHECK_OK_OBJECT(pPipeline->data->cte_bin, gst_element_link_many(
                pPipeline->data->udpsrc,
#ifdef TEE_HN_SERVER_OUTPUT_TO_FILE
                pPipeline->data->hntee,
#endif
                pPipeline->data->appsink, NULL),
                "Linking remote playback input_bin elements failed");
#else
            // Link the appsink to this src
            CHECK_OK_OBJECT(pPipeline->data->input_bin, gst_element_link_many(
                pPipeline->data->ls_tuner_appsrc,
#ifdef TEE_HN_SERVER_OUTPUT_TO_FILE
                pPipeline->data->hntee,
#endif
                pPipeline->data->appsink, NULL),
                "Linking remote playback input_bin elements failed");
#endif
            pPipelines = pPipelineManager->get_live_pipelines(pPipelineManager,
                        &numLivePipes);
            if (0 != numLivePipes)
            {
                RILOG_INFO("%s -- found %d live pipelines\n", __func__,
                        numLivePipes);
                // TODO: fix the live pipeline selection with tuner reference
                pPipeline->data->ls_tuner_appsink =
                        pPipelines[tuner]->data->ls_tuner_appsink;
            }

            RILOG_INFO("%s -- pipeline now has appSrc\n", __FUNCTION__);
        }
    }
    else
    {
        RILOG_WARN("%s -- pipeline type is UNKNOWN\n", __FUNCTION__);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * Determines if the current pipeline configuration is setup to support DVR
 *
 * @param   pPipeline   determine type of this pipeline
 */
static ri_bool pipeline_hn_server_is_dvr(ri_pipeline_t* pPipeline)
{
    ri_bool isDVR = TRUE;

    if (NULL == gst_bin_get_by_name(GST_BIN(pPipeline->data->input_bin),
            "dvrsrc"))
    {
        isDVR = FALSE;
    }
    return isDVR;
}

/**
 * Sends events to clear EOS which may have occurred on this pipeline.
 *
 * @param   pPipeline   send events to clear EOS on this pipeline
 */
static void pipeline_hn_server_send_event(ri_pipeline_t* pPipeline,
        GstEvent* event)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    const gchar* pEventName = GST_EVENT_TYPE_NAME(event);

    // Get the sink pad to send event on
    GstPad* appsink_pad = gst_element_get_static_pad(pPipeline->data->appsink,
            "sink");

    // Initialize the flag to indicate event has not yet been received
    pPipeline->data->server->eventReceived = FALSE;

    // Add probe on video sink pad to monitor for EOS event
    pPipeline->data->server->appsink_event_probe = gst_pad_add_event_probe(appsink_pad, G_CALLBACK(
                                                    appsink_event_probe), pPipeline);

    RILOG_DEBUG("%s -- sending event %s to appsink\n", __FUNCTION__, pEventName);

    //(void)gst_pad_send_event(file_src_pad, event);
    (void) gst_pad_send_event(appsink_pad, event);

    int maxCnt = 5;
    int curCnt = 0;
    while ((FALSE == pPipeline->data->server->eventReceived) && (curCnt < maxCnt))
    {
        curCnt++;

        // Sleep for a short time
        RILOG_TRACE("%s -- sleeping %d waiting for event %s\n", __FUNCTION__,
                curCnt, pEventName);
        g_usleep(500000L);
    }

    // If out of the loop but flag is still set, report problems with event
    if (FALSE == pPipeline->data->server->eventReceived)
    {
        RILOG_DEBUG("%s -- event %s never received\n", __FUNCTION__, pEventName);
    }
    else
    {
        RILOG_DEBUG("%s -- event %s was received\n", __FUNCTION__, pEventName);
    }

    // Unref the objects which were ref'd through method calls
    gst_object_unref(appsink_pad);

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
static gboolean appsink_event_probe(GstPad *pad, GstEvent *event,
        gpointer u_data)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    ri_pipeline_t* pPipeline = u_data;
    gboolean retVal = TRUE;

    switch GST_EVENT_TYPE(event)
    {
        case GST_EVENT_FLUSH_START:
        case GST_EVENT_FLUSH_STOP:

        RILOG_TRACE("%s -- got event %s, setting flag to true\n",
                __FUNCTION__, GST_EVENT_TYPE_NAME (event));

        // Clear flag to indicate event received if waiting for event
        if (FALSE == pPipeline->data->server->eventReceived)
        {
            pPipeline->data->server->eventReceived = TRUE;

            // Remove the event pad probe
            gst_pad_remove_event_probe(pad, pPipeline->data->server->appsink_event_probe);
        }
        break;

        default:
        // ignore any other events
        break;
    }

    RILOG_TRACE("%s -- Exit, return value = %d\n", __FUNCTION__, retVal);
    return retVal;
}

/**
 * Test method to verify events are properly sent and received
 */
gboolean pipeline_hn_server_test(gpointer object)
{
    RILOG_INFO("%s -- Entry\n", __FUNCTION__);

    ri_pipeline_t* pPipeline = (ri_pipeline_t*) object;
    const char* file_path = "c:/CableLabs/trunk/common/resources/tunedata";
    const char* file_name = "drooling.mpg";
    char* bufData = NULL;
    uint32_t bufLen = 0;
    uint64_t nptNS = 0;
    uint64_t bytePos = 0;
    gboolean done = FALSE;
    while (!done)
    {
        // Start pipeline
        pipeline_hn_server_start(pPipeline, 0, file_path, file_name, 1.0, 30, 0,
                                 RI_HN_SRVR_TYPE_TUNER);

        // Emulate getting buffers to send out on the socket
        while (!gst_app_sink_is_eos((GstAppSink*) pPipeline->data->appsink))
        {
            //RILOG_INFO("%s -- emulating sending buffer out on socket\n",
            //            __FUNCTION__);
            pipeline_hn_server_get_buffer(pPipeline, (void**) &bufData,
                    (uint32_t*) &bufLen, (uint64_t*) &nptNS,
                    (uint64_t*) &bytePos);
            //g_usleep(1000L);
            pipeline_hn_server_free_buffer(pPipeline, bufData);
        }

        // Stop pipeline
        pipeline_hn_server_stop(pPipeline);

        // Sleep awhile prior to restarting
        RILOG_INFO("%s -- sleeping, waiting for next start\n", __FUNCTION__);
        g_usleep(500000L);
    }

    return TRUE;
}

/**
 * Method which is called to notify there is a buffer available for sending.
 *
 * @param   sink        reference to calling appsink
 * @param   user_data   pointer to callback related data.
 */
GstFlowReturn pipeline_hn_server_new_buffer(GstAppSink* sink, gpointer user_data)
{
    GstFlowReturn retval = GST_FLOW_OK;
    ri_pipeline_t* pPipeline = user_data;

    g_mutex_lock(pPipeline->data->server->buf_mutex);
    pPipeline->data->server->bufCnt++;
    g_mutex_unlock(pPipeline->data->server->buf_mutex);

    //RILOG_DEBUG("%s -- new buffer cnt: %d\n", __FUNCTION__, g_bufCnt);

    return retval;
}

/**
 * Callback method which notifies that pipeline has encountered EOS.
 *
 * @param   sink        reference to calling appsink
 * @param   user_data   pointer to callback related data.
 */
void pipeline_hn_server_eos(GstAppSink* sink, gpointer user_data)
{
    RILOG_INFO("%s -- got real EOS\n", __FUNCTION__);
    ri_pipeline_t* pPipeline = user_data;

    g_mutex_lock(pPipeline->data->server->buf_mutex);
    pPipeline->data->server->isEOS = TRUE;
    g_mutex_unlock(pPipeline->data->server->buf_mutex);
}

/// Temporary helper functions for CT filename selection...
static char* filename_from_ri_tuner(ri_tuner_t* pTuner, ri_transformation_t* ct)
{
    uint32_t warnNum = 0;
    char* warnStr = NULL;

    if (NULL == ct) 
    {
        RILOG_ERROR("%s - NULL CT pointer!?\n", __func__);
    }
    else if (NULL != strstr(ct->transformedProfileStr, "AVC_TS"))
    {
        if (NULL != pTuner)
        {
            ri_tuner_status_t status = {0};

            pTuner->request_status(pTuner, &status);
            warnNum = status.program_num;
            warnStr = "program number";

            switch(status.frequency)
            {
                case 447000000: // 720x480_MPEG2...
                    switch(status.program_num)
                    {
                        case 1:
                        {
                            if(ct->bitrate == 2000)
                            {
                                return "program-1-baby-h264-1920X1080-2000kbps-looped.ts";
                            }
                            else if(ct->bitrate == 1000)
                            {
                                if(ct->progressive == TRUE)
                                {
                                    return "program-1-baby-h264-1280X720-1000k-looped.ts";                            
                                }
                                return "program-1-baby-h264-1280X720-1000k-interlaced.ts";
                            }
                            else if(ct->bitrate == 256)
                            {
                                return "program-1-baby-h264-352X288-256k-looped.ts";
                            }
                            else
                            {
                                return "ch1_avc_ts_na_iso.ts";
                            }
                        }
                        case 2:
                            return "ch2_avc_ts_na_iso.ts";
                        case 3:
                            return "ch3_avc_ts_na_iso.ts";
                    }
                    break;
                case 453000000: // 720x480_MPEG2...
                case 471000000:
                case 489000000:
                    switch(status.program_num)
                    {
                        case 1:
                            return "ch1_avc_ts_na_iso.ts";
                        case 2:
                            return "ch2_avc_ts_na_iso.ts";
                        case 3:
                            return "ch3_avc_ts_na_iso.ts";
                    }
                    break;
                case 597000000: // hd_airplane...
                case 599000000:
                    switch(status.program_num)
                    {
                        case 1:
                            return "ch4_avc_ts_na_iso.ts";
                        case 2:
                            return "ch5_avc_ts_na_iso.ts";
                    }
                    break;

                case 603000000: // galaxy_pingpong...
                case 699000000:
                    switch(status.program_num)
                    {
                        case 1:
                            return "ch6_avc_ts_na_iso.ts";
                        case 2:
                            return "ch7_avc_ts_na_iso.ts";
                    }
                    break;

                case 651000000: // background...
                    if (1 == status.program_num)
                    {
                        if(ct->bitrate == 1000)
                        {
                            return "background-h264-1280X720-1000k-looped.ts";
                        }
                        else if(ct->bitrate == 256)
                        {
                            return "background-h264-352X288-256k-looped.ts";
                        }
                        else
                        {
                            return "ch8_avc_ts_na_iso.ts";
                        }
                    }
                    break;

                case 491000000: // clock...
                    if (1 == status.program_num)
                    {
                        return "ch10_avc_ts_na_iso.ts";
                    }
                    break;

                default:
                    warnNum = status.frequency;
                    warnStr = "frequency";
                    break;
            }

            RILOG_WARN("%s - unknown %s: %u\n", __func__, warnStr, warnNum);
            return "ch9_avc_ts_na_iso.ts";  // big buck bunny by default...
        }
        else
        {
            RILOG_ERROR("%s - NULL pTuner pointer!?\n", __func__);
        }
    }
    else
    {
        RILOG_ERROR("%s - Unsupported CT profile: %s\n", __func__,
                    ct->transformedProfileStr);
    }

    return "error_in_filename_from_ri_tuner";
}

static char* filename_from_tuner(int tuner, ri_transformation_t* ct)
{
    uint32_t numLivePipes = 0;
    ri_pipeline_manager_t* pPipelineManager = ri_get_pipeline_manager();

    if (NULL == ct) 
    {
        RILOG_ERROR("%s - NULL CT pointer!?\n", __func__);
    }
    else if (NULL != strstr(ct->transformedProfileStr, "AVC_TS"))
    {
        const ri_pipeline_t** pPipelines = pPipelineManager->get_live_pipelines(
                                               pPipelineManager, &numLivePipes);
        if (NULL != pPipelineManager) 
        {
            ri_tuner_t* pTuner = pPipelines[tuner]->data->tuner;

            if (NULL != pTuner)
            {
                return filename_from_ri_tuner(pTuner, ct);
            }
            else
            {
                RILOG_ERROR("%s - NULL pTuner pointer!?\n", __func__);
            }
        }
        else
        {
            RILOG_ERROR("%s - NULL pPipelineManager pointer!?\n", __func__);
        }
    }
    else
    {
        RILOG_ERROR("%s - Unsupported CT profile: %s\n", __func__,
                    ct->transformedProfileStr);
    }

    return "error_in_filename_from_tuner";
}

static char* filename_from_tsb(char* file_name, ri_transformation_t* ct)
{
    ri_pipeline_t* pPipeline = getPipelineFromTsbFileName(file_name);

    if (NULL != pPipeline)
    {
        if (NULL != pPipeline->data) 
        {
            ri_tuner_t* pTuner = pPipeline->data->tuner;

            if (NULL != pTuner)
            {
                return filename_from_ri_tuner(pTuner, ct);
            }
            else
            {
                RILOG_ERROR("%s - NULL pTuner pointer!?\n", __func__);
            }
        }
        else
        {
            RILOG_ERROR("%s - NULL pPipeline->data pointer!?\n", __func__);
        }
    }
    else
    {
        RILOG_ERROR("%s - Unsupported CT profile: %s\n", __func__,
                    ct->transformedProfileStr);
    }

    return "error_in_filename_from_tsb";
}

static ri_error set_ct_file(ri_pipeline_t* pPipeline, char* file)
{
    ri_error retVal = RI_ERROR_NO_CONVERSION;
    char *path, *dir = "transformdata";

    // perform transformation file set-up...
    if (NULL != (path = ricfg_getValue("RIPlatform",
                                       "RI.Headend.resources.directory")))
    {
        if (strlen(path) + strlen(dir) < FILENAME_MAX)
        {
            sprintf(pPipeline->data->server->ct_path, "%s/%s", path, dir);
        }
        else
        {
            RILOG_ERROR("%s (%s/%s) is too long\n", __func__, path, dir);
        }

        if (strlen(path) + strlen(dir) < FILENAME_MAX)
        {
            sprintf(pPipeline->data->server->ct_file, "%s", file);
            retVal = RI_ERROR_NONE;
        }
        else
        {
            RILOG_ERROR("%s (%s) is too long\n", __func__, file);
        }
    }
    else
    {
        RILOG_ERROR("%s could not read resource path from config?!\n",
                __FUNCTION__);
    }

    return retVal;
}

static void reset_for_transformation(ri_pipeline_t* pPipeline)
{
    RILOG_INFO("%s - %s\n", __func__, pPipeline->data->name);
    pPipeline->data->server->bufCnt = 0;
    pPipeline->data->server->isEOS = FALSE;
    pPipeline->data->server->bytePos = 0;
    pPipeline->pipeline_hn_server_reset(pPipeline);
    start_pipeline(pPipeline);
    check_appsink_transition(pPipeline);
}

/**
 * Requests that a live stream be transformed and sent to a HN server
 *
 * Should be called after a HN server is set-up and before or during the
 * streaming process; i.e. if called before, the stream starts transformed,
 * if called after the stream is running, the current stream gets a new
 * transformation for it's output.  Calling with a NULL transformation
 * restores the original output format.
 *
 * NOTE: the lifetime of the transformation is tied to the HN stream, i.e.
 *       the transformation ends when the stream ends and any subsequent
 *       streams from the same server will have un-transformed output
 *       until a new transformation request ie performed.
 *
 * @param   pPipeline   associated pipeline
 * @param   tuner       the index of the tuner / tuned stream
 * @param   ct          the requested transformation, see
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_ILLEGAL_ARG: Invalid parameters supplied
 *    RI_ERROR_NO_DATA: tuner is not tuned to a source
 *    RI_ERROR_NO_CONVERSION: No transformation/conversion not supported
 *    RI_ERROR_GENERAL: any other error encountered
 */
ri_error pipeline_transform_live_stream(ri_pipeline_t* pPipeline,
                                        int tuner,
                                        ri_transformation_t* ct)
{
    char location[256];
    ri_error retVal = RI_ERROR_GENERAL;

    RILOG_INFO("%s(%p, %d, %p);\n", __func__, pPipeline, tuner, ct);

    if (NULL == pPipeline)
    {
        retVal = RI_ERROR_ILLEGAL_ARG;
    }
    else if (NULL != pPipeline->data)
    {
        if (NULL == ct)
        {
            RILOG_INFO("%s remove transformation & restoring original format\n",
                       pPipeline->data->name);

            // Get the mutex so we can manipulate the pipeline
            g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));

            // remove transformation and restore original output
            pipeline_hn_server_set_type(pPipeline, tuner, 
                                        pPipeline->data->server->orig_pipe_type);
            pPipeline->data->server->orig_pipe_type = RI_HN_SRVR_TYPE_UNKNOWN;
            g_free(pPipeline->data->server->orig_file_path);
            g_free(pPipeline->data->server->orig_file_name);
            pPipeline->data->server->orig_file_path = NULL;
            pPipeline->data->server->orig_file_name = NULL;
            pPipeline->data->server->ct_path[0] = 0;
            pPipeline->data->server->ct_file[0] = 0;
            g_free(pPipeline->data->server->ct);
            pPipeline->data->server->ct = NULL;
            reset_for_transformation(pPipeline);

            // release the pipeline mutex
            g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));

            retVal = RI_ERROR_NONE;
        }
        else
        {
            RILOG_INFO("%s profile: %s, bitrate: %d, w(%d) x h(%d), prog: %s\n",
                       pPipeline->data->name, ct->transformedProfileStr,
                       ct->bitrate, ct->width, ct->height,
                       boolStr(ct->progressive));

            // Get the mutex so we can manipulate the pipeline
            g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));

            if (NULL == pPipeline->data->server->ct)
            {
                pPipeline->data->server->orig_pipe_type = RI_HN_SRVR_TYPE_TUNER;
            }

            // perform transformation file set-up...
            retVal = set_ct_file(pPipeline, filename_from_tuner(tuner, ct));

            // preserve the transformation information
            pPipeline->data->server->ct = g_malloc(sizeof(ri_transformation_t));
            memcpy(pPipeline->data->server->ct, ct,sizeof(ri_transformation_t));

            // for now - override the server type and serve from a file
            pipeline_hn_server_set_type(pPipeline, tuner, RI_HN_SRVR_TYPE_FILE);
            strcpy(location, pPipeline->data->server->ct_path);
            strcat(location, "/");
            strcat(location,  pPipeline->data->server->ct_file);
            g_object_set(G_OBJECT(pPipeline->data->filesrc), "location",
                         location, NULL);
            RILOG_INFO("%s() set location to %s\n", __FUNCTION__, location);
            reset_for_transformation(pPipeline);

            // release the pipeline mutex
            g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));
        }
    }

    return retVal;
}

/**
 * Requests that a file or TSB stream be transformed and sent to a HN server
 *
 * Should be called after a HN server is set-up and before or during the
 * streaming process; i.e. if called before, the stream starts transformed,
 * if called after the stream is running, the current stream gets a new
 * transformation for it's output.  Calling with a NULL transformation
 * restores the original output format.
 *
 * NOTE: the lifetime of the transformation is tied to the HN stream, i.e.
 *       the transformation ends when the stream ends and any subsequent
 *       streams from the same server will have un-transformed output
 *       until a new transformation request ie performed.
 *
 * @param   pPipeline   associated pipeline
 * @param   file_path   directory of source file for transformation
 * @param   file_name   name of file containing the contents to transform
 * @param   ct          the requested transformation, see
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_ILLEGAL_ARG: Invalid parameters supplied
 *    RI_ERROR_NO_DATA: file or TSB does not exist
 *    RI_ERROR_NO_CONVERSION: No transformation/conversion not supported
 *    RI_ERROR_GENERAL: any other error encountered
 */
ri_error pipeline_transform_file_stream(ri_pipeline_t* pPipeline,
                                        char* file_path,
                                        char* file_name,
                                        ri_transformation_t* ct)
{
    char location[256];
    ri_error retVal = RI_ERROR_GENERAL;

    RILOG_INFO("%s(%p, %s, %s, %p);\n", __func__,
               pPipeline, file_path, file_name, ct);

    if ((NULL == pPipeline) || (NULL == file_path) || (NULL == file_name))
    {
        retVal = RI_ERROR_ILLEGAL_ARG;
    }
    else if (NULL != pPipeline->data)
    {
        if (NULL == ct)
        {
            RILOG_INFO("%s remove transformation & restoring original format\n",
                       pPipeline->data->name);

            // Get the mutex so we can manipulate the pipeline
            g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));

            // remove transformation and restore original output
            pipeline_hn_server_set_type(pPipeline, 0, 
                                        pPipeline->data->server->orig_pipe_type);
            pPipeline->data->server->orig_pipe_type = RI_HN_SRVR_TYPE_UNKNOWN;
            g_free(pPipeline->data->server->orig_file_path);
            g_free(pPipeline->data->server->orig_file_name);
            pPipeline->data->server->orig_file_path = NULL;
            pPipeline->data->server->orig_file_name = NULL;
            pPipeline->data->server->ct_path[0] = 0;
            pPipeline->data->server->ct_file[0] = 0;
            g_free(pPipeline->data->server->ct);
            pPipeline->data->server->ct = NULL;
            reset_for_transformation(pPipeline);

            // release the pipeline mutex
            g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));
            retVal = RI_ERROR_NONE;
        }
        else
        {
            RILOG_INFO("%s profile: %s, bitrate: %d, w(%d) x h(%d), prog: %s\n",
                       pPipeline->data->name, ct->transformedProfileStr,
                       ct->bitrate, ct->width, ct->height,
                       boolStr(ct->progressive));

            // Get the mutex so we can manipulate the pipeline
            g_static_rec_mutex_lock(&(pPipeline->data->pipeline_mutex));

            if (NULL == pPipeline->data->server->ct)
            {
                pPipeline->data->server->orig_pipe_type = RI_HN_SRVR_TYPE_TSB;
                pPipeline->data->server->orig_file_path = g_strdup(file_path);
                pPipeline->data->server->orig_file_name = g_strdup(file_name);
            }

            // perform transformation file set-up...
            retVal = set_ct_file(pPipeline, filename_from_tsb(file_name, ct));

            // preserve the transformation information
            pPipeline->data->server->ct = g_malloc(sizeof(ri_transformation_t));
            memcpy(pPipeline->data->server->ct, ct,sizeof(ri_transformation_t));

            // for now - override the server type and serve from a file
            pipeline_hn_server_set_type(pPipeline, 0, RI_HN_SRVR_TYPE_FILE);
            strcpy(location, pPipeline->data->server->ct_path);
            strcat(location, "/");
            strcat(location,  pPipeline->data->server->ct_file);
            g_object_set(G_OBJECT(pPipeline->data->filesrc), "location",
                         location, NULL);
            RILOG_INFO("%s() set location to %s\n", __FUNCTION__, location);
            reset_for_transformation(pPipeline);

            // release the pipeline mutex
            g_static_rec_mutex_unlock(&(pPipeline->data->pipeline_mutex));
        }
    }
    else
    {
        RILOG_ERROR("%s - pPipeline->data == NULL!\n", __func__);
    }

    return retVal;
}

/**
 * Returns a given pipeline's transformation status
 *
 * @param   pPipeline   associated pipeline
 * @param   status      the transformation status in a human readable string
 * @param   buffer_size the size of the status buffer to write into
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_ILLEGAL_ARG: Invalid parameters supplied
 *    RI_ERROR_GENERAL: any other error encountered
 */
ri_error pipeline_transform_status(ri_pipeline_t* pPipeline, char* status,
                                   int buffer_size)
{
    char buf[1024];
    ri_error retVal = RI_ERROR_NONE;

    RILOG_INFO("%s(%p, %p);\n", __func__, pPipeline, status);

    if ((NULL == pPipeline) || (NULL == status) || (buffer_size <= 0))
    {
        retVal = RI_ERROR_ILLEGAL_ARG;
    }
    else if (NULL != pPipeline->data)
    {
        if (NULL != pPipeline->data->server->ct)
        {
            snprintf(buf, sizeof(buf), 
                     "orig_type: %d, path: %s (was %s), file: %s, (was %s), "
                     "%s profile: %s, bitrate: %d, w(%d) x h(%d), prog: %s\n",
                     pPipeline->data->server->orig_pipe_type,
                     pPipeline->data->server->ct_path,
                     pPipeline->data->server->orig_file_path,
                     pPipeline->data->server->ct_file,
                     pPipeline->data->server->orig_file_name,
                     pPipeline->data->name,
                     pPipeline->data->server->ct->transformedProfileStr,
                     pPipeline->data->server->ct->bitrate,
                     pPipeline->data->server->ct->width,
                     pPipeline->data->server->ct->height,
                     boolStr(pPipeline->data->server->ct->progressive));
            RILOG_INFO("%s: %s\n", __func__, buf);
            strncpy(status, buf, buffer_size);
            status[buffer_size - 1] = 0;
        }
        else
        {
            snprintf(buf, sizeof(buf), 
                     "orig_type: %d, path: %s (was %s), file: %s, (was %s), "
                     "%s no transformation (output in original format)\n",
                     pPipeline->data->server->orig_pipe_type,
                     pPipeline->data->server->ct_path,
                     pPipeline->data->server->orig_file_path,
                     pPipeline->data->server->ct_file,
                     pPipeline->data->server->orig_file_name,
                     pPipeline->data->name);
            RILOG_INFO("%s: %s\n", __func__, buf);
            strncpy(status, buf, buffer_size);
            status[buffer_size - 1] = 0;
        }
    }
    else
    {
        snprintf(buf, sizeof(buf), "%s - pPipeline->data == NULL!\n", __func__);
        RILOG_ERROR("%s: %s\n", __func__, buf);
        strncpy(status, buf, buffer_size);
        status[buffer_size - 1] = 0;
    }

    return retVal;
}

