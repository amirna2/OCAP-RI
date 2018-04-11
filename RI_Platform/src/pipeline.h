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

#ifndef _PIPELINE_H_
#define _PIPELINE_H_

#include <glib.h>
#include <gst/gst.h>
#include <gst/app/gstappsink.h>
#include <gst/app/gstappsrc.h>
#include <ri_pipeline.h>
#include "tsb.h"
#include "pipeline_hn_server.h"

#undef TEE_HN_SERVER_OUTPUT_TO_FILE

typedef enum ri_pipeline_type_s
{
    RI_PIPELINE_TYPE_LIVE_TSB = 1,
    RI_PIPELINE_TYPE_LIVE_NON_TSB,
    RI_PIPELINE_TYPE_PLAYBACK_LOCAL,
    RI_PIPELINE_TYPE_HN_SERVER,
    RI_PIPELINE_TYPE_HN_PLAYER,
    RI_PIPELINE_TYPE_OOB,

} ri_pipeline_type_t;

// declare state machine function pointer types
typedef void* (*doPlaybackState)(ri_pipeline_t* pPipeline);

typedef struct ri_playback_data_s
{
    GThread * play_thread; // Thread to handle playback processing
    GAsyncQueue * play_queue; // Queue to send commands to playback thread
    gulong play_wake_interval; // Timeout for playback_thread queue pop in uS

    ri_pid_info_t* pids; // Pointer to the PID info for this recording
    uint32_t pid_count; // Number of PID entries in 'pids'
    ri_dvr_callback_f callback; // A callback function that will receive all
    // playback-related events involving this playback
    void* cb_data; // User data that will be passed to every callback invocation

    char* rec_name_content; // A full-path file name of the content of the recording
    char* filename;
    char* filepath;

    // Playback items
    gboolean playing; // Indicates if playback in progress
    uint64_t requested_start_position; // Offset in nS from the recording start
    float requested_rate; // Playback rate (signed) 1.0 is normal 1x
} ri_playback_data_t;

struct ri_pipeline_data_s
{
    GStaticRecMutex pipeline_mutex; // Recursive mutex to protect pipeline

    ri_pipeline_type_t type;

    ri_tuner_t* tuner;
    ri_section_filter_t* section_filter;
    ri_video_device_t* video_device;

    char *name; // Pipeline name
    GstElement* gst_pipeline; // Pipeline
    GstElement* input; // UDP Source or HN Stream source
    GstElement* payload; // Payload Handler
    GstElement* payloadqueue;
    GstElement* passthru0;
    GstElement* inputtee; // Splits out section data and rest of pipeline
    GstElement* preproc; // Transport stream preprocessor/filter
    GstElement* tsbtee; // Tee to feed out to Time Shift Buffer
    GstElement* recordswitch; // On-Off switch to the TSB
    GstElement* tsbsink; // TSB record element (file sink with indexing)
    GstElement* dvrsrc; // DVR play element (file source with seek/speed)
    GstElement* queue1;
    GstElement* queue2;
    GstElement* queue3;
    GstElement* queue4;

    GstElement* input_bin; // Bin for the TSB elements
    GstElement* cte_bin; // Bin for the content transformation engine elements

    ri_pid_info_t* decode_pids; // Pointer to the PID info for live decode
    uint32_t decode_pid_count; // Number of PID entries in 'decode_pids'
    uint16_t decode_prog_num; // the Program Number to decode
    uint32_t decode_queue_size; // Max number of buffers allowed to be queued
    // used to reduce latency in pipeline

    GstElement* output_bin; // Bin for the output elements
    GstElement* outputtee; // Tee to feed different decode legs
    GstElement* fakesink;
    GstElement* passthru1;

    ri_pid_info_t* tsb_pids; // Pointer to PIDs to be buffered into the TSB
    uint32_t tsb_pid_count; // Number of PID entries in tsb_pids

    ri_playback_data_t playback; // Holds the details of a recording playback (play pipeline only)

    GstElement* udpsink; // Sink for hn server external transformation send data
    GstElement* udpsrc;  // Src for hn server external transformation recv data
    GstElement* appsink; // Sink for hn remote playback pipeline
    GstElement* filesrc; // File source without seek/speed
    pipeline_hn_server_data_t* server;

    // *TODO* - make this its own structure???
    ri_hn_socket_callback_f hn_socket_cb; // Function to call when buffer of data is available to send on socket
    ri_hn_eos_callback_f hn_eos_cb; // Function to call when last buffer of data is available to send on socket
    void* hn_cb_data; // reference to mpeos server or player to callback
    GstAppSrcCallbacks appsrc_callbacks; // Callbacks for appsrc in hn player pipeline
    ri_hn_need_data_callback_f hn_need_data_cb; // Function to call when hn player pipeline needs data
#ifdef TEE_HN_SERVER_OUTPUT_TO_FILE
    GstElement* hntee;
    GstElement* filesink;
#endif
    guint event_probe; // handler id of event probe when sending events
    gboolean eventReceived; // flag which indicates event has been received
    GCond*   event_cond; // Condition data structure for notification from GStreamer
    GMutex*  event_mutex; // Mutex to protect the flag denoting notification from GStreamer

    // *TODO* - remove these
    GstElement* ss_bin; // Bin for the Section Sink elements
    GstElement* queue0; // Misc queues to help with threading
    GstElement* sectionassembler; // Section assember for section leg of decoder
    GstElement* sectionfilter; // Section filter for section leg of decoder
    GstElement* sectionsink; // Section sink for section leg of decoder

    // "no TSB" live streaming pipeline elements
    GstElement* ls_tuner_bin;     // Bin for the Live Streaming elements
    GstElement* ls_tuner_queue;   // queue to help with threading after a tee
    GstElement* sptsassembler;    // LS Tuner SPTS assembler element
    GstElement* ls_tuner_appsink; // Sink for Live Streaming data retrieval
    GstElement* ls_tuner_appsrc;  // Src for Live Streaming playback pipeline
    GstAppSrcCallbacks ls_tuner_appsrc_callbacks; // Callbacks for LS tuner src
    guint ls_tuner_event_probe; // handler id of event probe when sending events
};

#ifdef WIN32
#define NO_TARGET_FILE "nul"
#define NO_SOURCE_FILE "nul"
#else
#define NO_TARGET_FILE "//dev//null"
#define NO_SOURCE_FILE "//dev//null"
#endif

#define ALL_PIDS NULL
#define NO_PIDS " "

// an invalid position used in the playback thread
#define IGNORE_POSITION G_MAXUINT64

// an invalid playback rate used in the playback thread
// since rate must be an integer multiple of 2, 3 is therefore invalid
#define IGNORE_RATE 3

#define CHECK_OK_OBJECT(o,x,m) if (!(x)) { GST_ERROR_OBJECT((o), (m)); }

void decode(ri_pipeline_t* pPipeline, ri_video_device_t* video_device,
        ri_pid_info_t* pids, uint32_t pid_count);

void set_decode_prog_num(ri_pipeline_t* pPipeline, uint16_t program);

ri_pipeline_t* create_pipeline(ri_pipeline_type_t type);

void start_pipeline(ri_pipeline_t* pipeline);

void stop_pipeline(ri_pipeline_t* pipeline);

gboolean has_video_device(ri_pipeline_t* pipeline);

gboolean attach_video_device(ri_pipeline_t* pipeline,
        ri_video_device_t* video_device);

gboolean detach_video_device(ri_pipeline_t* pipeline);

void set_rate(ri_pipeline_t* pipeline, float rate);

void update_preproc(ri_pipeline_t* object);

void select_live(GstElement *inputselect, gboolean live);

//ri_error playback_stop(ri_pipeline_t* object);

void destroy_pipeline(ri_pipeline_t* pipeline);

ri_section_filter_t* get_section_filter(ri_pipeline_t* pPipeline);

void create_ssbin(ri_pipeline_t* pPipeline);

void create_ls_tuner_bin(ri_pipeline_t* pPipeline);
void ls_tuner_flush(ri_pipeline_t* pPipeline);


///////////////////////////////////////////////////////////////////////////////
/**
 * Temporary function used to obtain the only playback pipeline that is known
 * to the platform.
 * It is expected that this routine will eventually be removed due to future
 * changes to the MPE-layer code (19 Jan 2010).
 */
ri_pipeline_t* get_playback_pipeline(void);
///////////////////////////////////////////////////////////////////////////////


#endif /* _PIPELINE_H_ */
