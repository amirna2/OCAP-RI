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

#ifndef _RI_PIPELINE_H_
#define _RI_PIPELINE_H_

#include <ri_tuner.h>
#include <ri_section_filter.h>
#include <ri_video_device.h>
#include <ri_ui_manager.h>
#include <ri_types.h>

typedef struct ri_pipeline_s ri_pipeline_t;
typedef struct ri_pipeline_data_s ri_pipeline_data_t;

#define RI_MAX_RECORDING_NAME_LENGTH 64

/*
 typedef enum _ri_media_stream_type
 {
 } ri_media_stream_type;
 */

typedef enum _ri_media_es_format
{
    RI_SI_ELEM_MPEG_1_VIDEO = 0x01,
    RI_SI_ELEM_MPEG_2_VIDEO = 0x02,
    RI_SI_ELEM_MPEG_1_AUDIO = 0x03,
    RI_SI_ELEM_MPEG_2_AUDIO = 0x04,
    RI_SI_ELEM_MPEG_PRIVATE_SECTION = 0x05,
    RI_SI_ELEM_MPEG_PRIVATE_DATA = 0x06,
    RI_SI_ELEM_MHEG = 0x07,
    RI_SI_ELEM_DSM_CC = 0x08,
    RI_SI_ELEM_H_222 = 0x09,
    RI_SI_ELEM_DSM_CC_MPE = 0x0A,
    RI_SI_ELEM_DSM_CC_UN = 0x0B,
    RI_SI_ELEM_DSM_CC_STREAM_DESCRIPTORS = 0x0C,
    RI_SI_ELEM_DSM_CC_SECTIONS = 0x0D,
    RI_SI_ELEM_AUXILIARY = 0x0E,
    RI_SI_ELEM_AAC_ADTS_AUDIO = 0x0F,
    RI_SI_ELEM_ISO_14496_VISUAL = 0x10,
    RI_SI_ELEM_AAC_AUDIO_LATM = 0x11,
    RI_SI_ELEM_FLEXMUX_PES = 0x12,
    RI_SI_ELEM_FLEXMUX_SECTIONS = 0x13,
    RI_SI_ELEM_SYNCHRONIZED_DOWNLOAD = 0x14,
    RI_SI_ELEM_METADATA_PES = 0x15,
    RI_SI_ELEM_METADATA_SECTIONS = 0x16,
    RI_SI_ELEM_METADATA_DATA_CAROUSEL = 0x17,
    RI_SI_ELEM_METADATA_OBJECT_CAROUSEL = 0x18,
    RI_SI_ELEM_METADATA_SYNCH_DOWNLOAD = 0x19,
    RI_SI_ELEM_MPEG_2_IPMP = 0x1A,
    RI_SI_ELEM_AVC_VIDEO = 0x1B,
    RI_SI_ELEM_VIDEO_DCII = 0x80,
    RI_SI_ELEM_ATSC_AUDIO = 0x81,
    RI_SI_ELEM_STD_SUBTITLE = 0x82,
    RI_SI_ELEM_ISOCHRONOUS_DATA = 0x83,
    RI_SI_ELEM_ASYNCHRONOUS_DATA = 0x84,
    RI_SI_ELEM_ENHANCED_ATSC_AUDIO = 0x87
} ri_media_es_format;

/* Basic Media types associated with PIDs */
typedef enum _ri_media_type
{
    RI_MEDIA_TYPE_UNKNOWN,
    RI_MEDIA_TYPE_VIDEO,
    RI_MEDIA_TYPE_AUDIO,
    RI_MEDIA_TYPE_DATA,
    RI_MEDIA_TYPE_SUBTITLES,
    RI_MEDIA_TYPE_SECTIONS,
    RI_MEDIA_TYPE_PCR,
    RI_MEDIA_TYPE_PMT
} ri_media_type;

/**
 * Represents a single PID from an MPEG-2 transport stream
 */
typedef struct ri_pid_info_s
{
    ri_media_type mediaType;
    uint16_t srcPid;
    uint16_t recPid;
    ri_media_es_format srcFormat;
    ri_media_es_format recFormat;

} ri_pid_info_t;

/**
 * Indentifies a unique media time shift buffer
 */
//typedef uint32_t ri_tsbHandle;
typedef void* ri_tsbHandle;

/**
 * Event data provided with RI_DVR_TSB_STATUS and RI_DVR_TSB_CONVERSION_STATUS
 * events
 */
typedef struct ri_tsb_status_s
{
    uint64_t start_time;
    uint64_t end_time;
    uint64_t size;
} ri_tsb_status_t;

/**
 * Event data provided with RI_DVR_PLAYBACK_STATUS events
 */
typedef struct ri_playback_status_s
{
    uint64_t position;
    float rate;
} ri_playback_status_t;

/**
 * Event data provided with RI_DVR_CONVERSION_COMPLETE events
 */
typedef struct ri_conversion_results_s
{
    uint64_t duration;
    uint64_t size;
} ri_conversion_results_t;

/**
 * TSB Events:
 *
 *    RI_DVR_EVENT_TSB_START -- event_data = uint64_t*
 *        This event is sent when a TSB recording is officially started. The
 *        event data contains:
 *             1) start_time -- The actual start time of the TSB recording in
 *                system time (UNIXepoch/UTC nanoseconds)
 *
 *    RI_DVR_EVENT_TSB_STATUS -- event_data = ri_tsb_status_t*
 *        This event is periodically sent while a TSB is active to update
 *        clients about its status.  The event data contains:
 *             1) start_time -- the current TSB start time (in nanoseconds) in
 *                system time (UNIXepoch/UTC nanoseconds)
 *             2) end_time -- the current TSB end time (in nanoseconds) in
 *                system time (UNIXepoch/UTC nanoseconds)
 *             3) size -- the current TSB size in bytes
 *
 *    RI_DVR_EVENT_TSB_STOPPED -- event_data = ri_tsb_status_t*
 *         This event is sent when a TSB session has officially been closed.
 *         No more TSB status events will be delivered for this session.
 *         The event data contains:
 *             1) start_time -- the final TSB start time (in nanoseconds) as
 *                measured from the original TSB start time.
 *             2) end_time -- the final TSB end time (in nanoseconds) as
 *                measured from the original TSB start time.
 *             3) size -- the final TSB size in bytes
 *
 *    RI_DVR_EVENT_TSB_CONVERSION_STATUS -- event_data = ri_tsb_status_t*
 *        This event is periodically sent while a TSB conversion is active
 *        to update clients about its status.  The event data contains:
 *             1) start_time -- unused
 *             2) end_time -- The current converted recording end time (in
 *                nanoseconds) as measured from the converted recording start
 *                time
 *             3) size -- current size (in bytes) of the converted recording
 *
 *    RI_DVR_EVENT_TSB_CONVERSION_COMPLETE -- event_data = ri_conversion_results_t*
 *        This event is sent when an active TSB conversion completes. The event
 *        data contains:
 *             1) duration -- The final recording duration in seconds
 *             2) size -- The final recording size on disk (in bytes)
 *
 *    RI_DVR_EVENT_PLAYBACK_STARTED -- event_data = ri_playback_status_t*
 *        This event is sent when playback of a recording or TSB has started.
 *        The event data contains:
 *             1) position -- Unused
 *             2) rate -- The current playback rate
 *
 *    RI_DVR_EVENT_PLAYBACK_STATUS -- event_data = ri_playback_status_t*
 *        This event is sent periodically during TSB or recording playback to
 *        update clients about its status.  The event data contains:
 *             1) position -- The current playback position in nanoseconds.
 *                For TSB, this is measured from the original TSB start time.
 *                For recordings this is measured from 0.
 *             2) rate -- The current playback rate
 *
 *    RI_DVR_EVENT_PLAYBACK_STOPPED -- event_data = unused
 *        This event is sent when a non-TSB playback session is terminated.
 *        No more playback status events will be delivered for this session.
 *
 *    RI_DVR_EVENT_END_OF_FILE -- event_data = unused
 *        This event may be sent during any playback session when the
 *        current playback position crosses the end of recorded content.
 *        For TSB playback, this indicates that playback has terminated
 *        and live playback has commenced.  For recording playback, the
 *        playrate will be set to 0.0 and the last valid frame of video
 *        is displayed.
 *
 *    RI_DVR_EVENT_START_OF_FILE -- event_data = unused
 *        This event may be sent during any playback session when the
 *        current playback position crosses the beginning of recorded content.
 *        For TSB playback, this indicates that playback rate has been set
 *        to 1.0 and playback continues.  For recording playback, the
 *        playrate will be set to 0.0 and the first valid frame of video
 *        is displayed.
 *
 *    RI_DVR_EVENT_DISK_SPACE -- event_data = unused
 *        This event is sent if any TSB or recording actions have failed due
 *        to insufficient disk space.
 */
typedef enum _ri_dvr_event
{

    RI_DVR_EVENT_TSB_START,
    RI_DVR_EVENT_TSB_STATUS,
    RI_DVR_EVENT_TSB_STOPPED,
    RI_DVR_EVENT_TSB_CONVERSION_STATUS,
    RI_DVR_EVENT_TSB_CONVERSION_COMPLETE,
    RI_DVR_EVENT_PLAYBACK_STARTED,
    RI_DVR_EVENT_PLAYBACK_STATUS,
    RI_DVR_EVENT_PLAYBACK_STOPPED,
    RI_DVR_EVENT_END_OF_FILE,
    RI_DVR_EVENT_START_OF_FILE,

} ri_dvr_event;


/**
 * HN Server types:
 *
 * RI_HN_SRVR_TYPE_UNKNOWN - invalid HN server type
 * RI_HN_SRVR_TYPE_TSB - the pipeline src is from a trickplay filesrc
 * RI_HN_SRVR_TYPE_FILE - the pipeline src is from a normal filesrc
 * RI_HN_SRVR_TYPE_TUNER - the pipeline src is from a tuner not a TSB (non-DVR)
 */
typedef enum _ri_hn_srvr_type
{
    RI_HN_SRVR_TYPE_UNKNOWN,
    RI_HN_SRVR_TYPE_TSB,
    RI_HN_SRVR_TYPE_FILE,
    RI_HN_SRVR_TYPE_TUNER,
} ri_hn_srvr_type;


#define MAX_DLNA_PROFILE_STR_SIZE   32

/**
 * Representation of an HN content transformation in the RI platform
 */
typedef struct
{
    /// the desired profile of the output stream
    char transformedProfileStr[MAX_DLNA_PROFILE_STR_SIZE];

    /// the max bitrate in Kbps (post transformation)
    int32_t bitrate;

    /// the max horizontile resolution (post transformation)
    int32_t width;

    /// the max vertical resolution (post transformation)
    int32_t height;

    /// the supported frame format (progressive/interlaced)
    ri_bool progressive;

} ri_transformation_t;

/**
 * The standard DVR event callback.
 */
typedef void (*ri_dvr_callback_f)(ri_dvr_event event, void* event_data,
        void* cb_data);

/**
 * The standard section filter decode callback to be called when PIDs have
 * been discovered on closed filter
 */
typedef void (*ri_hn_decode_callback_f)(ri_pid_info_t* pids,
        uint32_t pid_count, void* cb_data);

/**
 * Callback function to notify that the HN Player pipeline needs data
 * and that another socket read should be performed.
 */
typedef void (*ri_hn_need_data_callback_f)(void* cb_data);

/**
 * Callback function which is called when a buffer of data is ready
 * to send out on the socket to support HN remote playback.
 */
typedef void (*ri_hn_socket_callback_f)(void* buffer, uint32_t bufLen,
        void* cb_data);

/**
 * Callback function which is called to notify that entire file
 * has been read from remote play back file
 */
typedef void (*ri_hn_eos_callback_f)(void* cb_data);

/**
 * Create a new timeshift buffer
 *
 * @param path The absolute path on disk under which all data for this TSB
 *             should be stored
 * @param duration The requested duration (in seconds) of this TSB
 * @param handle The location where the platform will return the unique
 *        identifier for this TSB.
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_ILLEGAL_ARG: Invalid parameters supplied
 */
RI_MODULE_EXPORT ri_error tsb_init(const char* path, uint64_t duration,
        ri_tsbHandle* handle);

/**
 * Deletes the time shift buffer (TSB) associated with this pipeline including
 * all media and metadata files from the filesystem. If a TSB playback is
 * currently taking place, the playback is stopped exactly as if playback_stop()
 * was called.  If buffering is currently taking place, it will be stopped exactly
 * as if tsb_stop() was called.  If a TSB conversion is currently taking place, the
 * conversion will terminate exactly as if tsb_convert_stop() was called.
 *
 * @param tsb The unique TSB identifier
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_NO_TSB: The given TSB handle is invalid
 */
RI_MODULE_EXPORT ri_error tsb_delete(ri_tsbHandle tsb);

/**
 * Clears all data from the given TSB.  The TSB must not be currently in use
 * for the operation to succeed.
 *
 * @param tsb The unique TSB identifier
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_RECORDING_IN_USE: The given TSB is currently being recorded into
 *       or played back.
 */
RI_MODULE_EXPORT ri_error tsb_flush(ri_tsbHandle tsb);

/**
 * Modifies the duration of the given TSB.  For durations smaller that the current
 * duration (shrink):
 *     - If the TSB is currently wrapped (start time non-zero) OR if the new
 *       duration would cause the TSB to start wrapping, the buffer will be
 *       reduced from the start of recorded content
 *     - Otherwise, the buffer will be reduced from the end of the buffer
 * For durations larger than the current duration (grow):
 *     - If the TSB is currently wrapped, then it will no longer be wrapped
 *       and any future or current buffering will be into newly allocated buffer
 *       space
 *     - Otherwise, the buffer will be extended to the new size
 *
 * @param tsb The unique TSB identifier
 * @param duration The required duration (in seconds) of the TSB.
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_ILLEGAL_ARG:  Invalid parameters supplied
 *    RI_ERROR_NO_TSB: No TSB has been started on this pipeline
 */
RI_MODULE_EXPORT ri_error tsb_set_duration(ri_tsbHandle tsb, uint64_t duration);

/**
 * Initiates the conversion of a time shift buffer recording to a permanent
 * recording.
 *
 * @param tsb The TSB from which to perform the conversion
 * @param rec_path An absolute path that indicates the directory under which all
 *                 files associated with this recording should be stored.
 * @param rec_name Upon successful return of this call, the platform will return the
 *                 unique recording name for this conversion
 * @param pids The pids from the TSB to convert (recPids only)
 * @param pid_count The number of pids in the pid array
 * @param inouttime The requested time (in nanoseconds), from the original
 *                        TSB start time, for the start of the conversion. Upon
 *                        successful return, the platform will return the actual
 *                        conversion start time.
 * @param duration The expected duration (in seconds) of the converted recording.
 * @param callback A callback function that will receive all events related to
 *                 the conversion of this TSB
 * @param cb_data User data that will be passed in every callback invocation
 *
 * @return Upon success, return RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_ILLEGAL_ARG:  Invalid parameters supplied
 *    RI_ERROR_ALREADY_EXISTS:  A conversion is already taking place on this
 *                              TSB
 */
RI_MODULE_EXPORT ri_error tsb_convert(ri_tsbHandle tsb, const char* rec_path,
        char rec_name[RI_MAX_RECORDING_NAME_LENGTH], ri_pid_info_t* pids,
        uint32_t pid_count, uint64_t* inout_starttime, uint32_t duration,
        ri_dvr_callback_f callback, void* cb_data);

/**
 * Terminates any existing TSB recording conversion currently taking place.
 *
 * @param tsb The TSB
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_ILLEGAL_ARG:  Invalid parameters supplied
 *    RI_ERROR_NO_TSB: No TSB has been started on this pipeline
 *    RI_ERROR_NO_CONVERSION: No TSB-conversion has been initiated on this TSB
 */
RI_MODULE_EXPORT ri_error tsb_convert_stop(ri_tsbHandle tsb);

/**
 * Begins playing back media from this pipeline's TSB.
 *
 * @param tsb The buffer that should be used for playback
 * @param video_device platform decode display device to use for play back
 * @param pids An array of pids that should be played back (recPids only)
 * @param pid_count The number of pids in the array
 * @param position The requested time (in nanoseconds), from the original
 *                 TSB start time, at which the playback should begin
 * @param rate The desired playback rate
 * @param callback A callback function that will receive all events related to
 *                 the playback from this TSB
 * @param cb_data User data that will be passed in every callback invocation
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_ILLEGAL_ARG:  Invalid parameters supplied
 */
RI_MODULE_EXPORT ri_error tsb_playback_start(ri_tsbHandle tsb,
        ri_video_device_t* video_device, ri_pid_info_t* pids,
        uint32_t pid_count, uint64_t position, float rate,
        ri_dvr_callback_f callback, void* cb_data);

/**
 * Initiates playback of a non-TSB recording.
 *
 * @param video_device platform decode display device to use for play back
 * @param rec_path The absolute path location of this recording
 * @param rec_name The unique recording name
 * @param position The 0-based position in the recording (in nanoseconds) from
 *                 which playback should start
 * @param rate The desired playback rate
 * @param pids An array of pids that should be played back (recPids only)
 * @param pid_count The number of pids in the array
 * @param callback A callback function that will receive all events related to
 *                 the playback of this recording
 * @param cb_data User data that will be passed in every callback invocation
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_ILLEGAL_ARG:  Invalid parameters supplied
 */
RI_MODULE_EXPORT ri_error recording_playback_start(
        ri_video_device_t* video_device, const char* rec_path,
        const char* rec_name, uint64_t position, float rate,
        ri_pid_info_t* pids, uint32_t pid_count, ri_dvr_callback_f callback,
        void* cb_data);

/**
 * Terminates a TSB or recording playback
 *
 * @param video_device The output device used to identify the unique playback
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_NO_PLAYBACK: No recording or TSB playback is currently
 *                          running on this video device
 */
RI_MODULE_EXPORT ri_error playback_stop(ri_video_device_t* video_device);

/**
 * Sets the playback rate for the playback currently running on this video device.
 *
 * @param video_device The output device used to identify the unique playback
 * @param rate The desired playback rate
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_NO_PLAYBACK: No recording or TSB playback is currently
 *                          running on this video device
 */
RI_MODULE_EXPORT ri_error playback_set_rate(ri_video_device_t* video_device,
        float rate);

/**
 * Sets the position for the playback currently running on this video device.
 *
 * @param video_device The output device used to identify the unique playback
 * @param position For recording playback, the 0-based position in the recording
 *                 (in nanoseconds).  For TSB playback, the time (in nanoseconds)
 *                 since the the original TSB start time.
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *    RI_ERROR_NO_PLAYBACK: No recording or TSB playback is currently
 *                          running on this video device
 */
RI_MODULE_EXPORT ri_error playback_set_position(
        ri_video_device_t* video_device, uint64_t position);

// Keep it under 4096 to prevent segmentation when sending on socket
#define RI_PIPELINE_HN_SERVER_BLOCK_SIZE (21 * 188)

/**
 * This structure represent a live meida pipeline available in the
 * RI platform.
 */
struct ri_pipeline_s
{
    ri_bool isLive;
    ri_bool hasTSB;

    /**
     * Returns the tuner structure associated with this live media pipeline
     *
     * @param object The "this" pointer
     * @return the tuner
     */
    ri_tuner_t* (*get_tuner)(ri_pipeline_t* object);

    /**
     * Returns the section filter structure associated with this live media pipeline
     *
     * @param object The "this" pointer
     * @return the section filter
     */
    ri_section_filter_t* (*get_section_filter)(ri_pipeline_t* object);

    /**
     * Returns the vide decode device associated with this live media pipeline
     */
    ri_video_device_t* (*get_video_device)(ri_pipeline_t* object);

    /**
     * Set the video decode program number
     */
    void (*set_decode_prog_num)(ri_pipeline_t* pPipeline, uint16_t program);

    /**
     * Initiate a video decode using the supplied PID
     */
    void (*decode)(ri_pipeline_t* object, ri_video_device_t* video_device,
            ri_pid_info_t* pids, uint32_t pid_count);

    /**
     * Terminates a video decode by removing the decode bin from the pipeline.
     *
     * @param object the pipeline on which decoding is to stop.
     */
    void (*decode_stop)(ri_pipeline_t* object);

    ////////////////////////////////////////////////////////////////////
    //                             DVR APIs                           //
    ////////////////////////////////////////////////////////////////////

    /**
     * Begin recording into this time shift buffer.
     *
     * @param object The "this" pointer
     * @param tsb The buffer that should be used for recording.
     * @param pids An array of pids that should be played back.  Upon return, this
     *             array is updated to contain the actual recorded PIDs
     * @param pid_count The number of pids in the array
     * @param callback A callback function that will receive all recording and
     *                 playback-related events involving this TSB
     * @param cb_data User data that will be passed in every callback invocation
     *
     * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
     *    RI_ERROR_ILLEGAL_ARG: Invalid parameters supplied
     *    RI_ERROR_ALREADY_EXISTS: A TSB has already been created for this
     *                             pipeline.
     */
    ri_error (*tsb_start)(ri_pipeline_t* object, ri_tsbHandle tsb,
            ri_pid_info_t* pids, uint32_t pid_count,
            ri_dvr_callback_f callback, void* cb_data);

    /**
     * Request that the TSB associated with this pipeline stop buffering.  This will
     * reset the TSB to an empty state.
     *
     * @param object The "this" pointer
     *
     * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
     *    RI_ERROR_ILLEGAL_ARG: Invalid parameters supplied
     *    RI_ERROR_NO_TSB: If there is no TSB associated with this pipeline
     */
    ri_error (*tsb_stop)(ri_pipeline_t* object);

    /**
     * Initiates receiving a hn stream with callback for PID discovery notification.
     *
     * @param  object         The "this" pointer
     * @param  decode_cb      function to call when PIDs are discovered in stream
     * @param  need_data_cb   function pipeline calls when data is needed
     * @param  decode_data    reference to mpeos_hn player which is handling hn stream
     *
     * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
     *    RI_ERROR_STREAMING: Problems starting hn streaming pipeline
     */
    ri_error (*pipeline_hn_player_start)(ri_pipeline_t* object,
            ri_hn_decode_callback_f decode_cb,
            ri_hn_need_data_callback_f need_data_cb, void* decode_data);

    /**
     */
    ri_error (*pipeline_hn_player_set_rate)(ri_pipeline_t* object,
            ri_video_device_t* video_device, float rate);
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
    ri_error (*pipeline_hn_player_inject_data)(ri_pipeline_t* object,
            char* buf, uint32_t numBytes, uint64_t nptNS);

    /**
     * Pauses the output of data from this hn stream client pipeline.
     *
     * @param object    pipeline to pause
     *
     * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
     *    RI_ERROR_STREAMING: Problems streaming data in pipeline
     */
    ri_error (*pipeline_hn_player_pause)(ri_pipeline_t* object);

    /**
     * Resumes the output of data from this hn stream client pipeline.
     *
     * @param object    pipeline to resume
     *
     * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
     *    RI_ERROR_STREAMING: Problems streaming data in pipeline
     */
    ri_error (*pipeline_hn_player_resume)(ri_pipeline_t* object);

    /**
     * Stops the output of data from this hn stream client pipeline.
     *
     * @param object    pipeline to stop
     *
     * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
     *    RI_ERROR_STREAMING: Problems streaming data in pipeline
     */
    ri_error (*pipeline_hn_player_stop)(ri_pipeline_t* object);

    /**
     * Starts the remote playback of an hn stream with RI acting as server
     *
     * @param object    The "this" pointer
     * @param rec_path  platform specific directory where recording resides
     * @param rec_name  platform specific filename for the recording to playback
     * @param rate      rate of stream playback
     * @param bytePos   begin playback at this "network" byte position
     * @param pipe_type indicates the pipeline type to activate
     *
     * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
     *    RI_ERROR_STREAMING: Problems starting hn streaming pipeline
     */
    ri_error (*pipeline_hn_server_start)(ri_pipeline_t* object, int tuner,
            const char* rec_path, const char* rec_name, float rate,
            int32_t frame_rate, int64_t bytePos, ri_hn_srvr_type pipe_type);

    /**
     * Pauses the output of data from this remote playback pipeline.
     *
     * @param object    remote playback pipeline to pause
     *
     * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
     *    RI_ERROR_STREAMING: Problems streaming data in pipeline
     */
    ri_error (*pipeline_hn_server_pause)(ri_pipeline_t* object);

    /**
     * Resumes the output of data from this remote playback pipeline.
     *
     * @param object    remote playback pipeline to resume
     *
     * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
     *    RI_ERROR_STREAMING: Problems streaming data in pipeline
     */
    ri_error (*pipeline_hn_server_resume)(ri_pipeline_t* object);

    /**
     * Sets the playback start time for this remote playback pipeline.
     *
     * @param object    remote playback pipeline to set time
     *
     * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
     *    RI_ERROR_STREAMING: Problems streaming data in pipeline
     */
    ri_error (*pipeline_hn_server_set_time)(ri_pipeline_t* object,
            uint64_t time);

    /**
     * Sets the playback byte position of this remote playback pipeline.
     *
     * @param object    remote playback pipeline to set byte position
     *
     * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
     *    RI_ERROR_STREAMING: Problems streaming data in pipeline
     */
    ri_error (*pipeline_hn_server_set_byte_pos)(ri_pipeline_t* object,
            uint64_t byte_pos);

    /**
     * Resets file source associated this remote playback pipeline.
     * It is used when an EOS is encountered due to rewind
     *
     * @param object    remote playback pipeline to reset file src
     *
     * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
     *    RI_ERROR_STREAMING: Problems streaming data in pipeline
     */
    ri_error (*pipeline_hn_server_reset)(ri_pipeline_t* object);

    /**
     * Stops the output of data from this remote playback pipeline.
     *
     * @param object    remote playback pipeline to stop
     *
     * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
     *    RI_ERROR_STREAMING: Problems streaming data in pipeline
     */
    ri_error (*pipeline_hn_server_stop)(ri_pipeline_t* object);

    /**
     * Returns buffer containing next set of data to send out on network for a
     * remote playback of a recording.
     *
     * @param   pPipeline   associated remote playback pipeline
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
    ri_error (*pipeline_hn_server_get_buffer)(ri_pipeline_t* object,
            void** bufData, uint32_t* bufLen, uint64_t* nptNS,
            uint64_t* bytePos);

    /**
     * Frees memory associated with buffer which was allocated in pipeline_hn_server_get_buffer()
     *
     * @param   pPipeline   associated pipeline
     * @param   bufData     buffer to free
     */
    void (*pipeline_hn_server_free_buffer)(ri_pipeline_t* object,
                    void* bufData);

    /**
     * called upon pipeline HN server stream start
     *
     * @param   pVideoDevice   associated video_device
     */
    void (*pipeline_hn_server_flow_start)(ri_pipeline_t* object, int tuner,
                    ri_pid_info_t* pids, int pid_count);

    /**
     * called upon pipeline HN server stream stop
     *
     * @param   pVideoDevice   associated video_device
     */
    void (*pipeline_hn_server_flow_stop)(ri_pipeline_t* object, int tuner);

    /**
     * Retrieves the size in bytes of the file with supplied path & name.
     *
     * @param   file_path         directory of file to get size of
     * @param   file_name         name of file to get size of
     * @param   file_size_bytes   size of the file in bytes, 0 if file not found
     *
     * @return  RI_ERROR_NONE
     */
    ri_error (*pipeline_hn_server_get_file_size)(
            const char* file_path, const char* file_name,
            int64_t* file_size_bytes);

    /**
     * Retrieves the size in bytes of the supplied name and path ifs file.
     *
     * @param   file_path         directory of file to get size of
     * @param   file_name         name of file to get size of
     * @param   file_size_bytes   size of the ifs file in bytes, 0 if file not found
     *
     * @return  RI_ERROR_NONE if no errors, RI_ERROR_GENERAL if problems were encountered
     */
    ri_error (*pipeline_hn_server_get_ifs_file_size)(
            const char* file_path, const char* file_name,
            int64_t* file_size_bytes);

    /**
     * Retrieves network byte position for supplied media time of supplied file.
     *
     * @param   filepath        directory of file to determine byte position
     * @param   file_name       name of file to determine byte position
     * @param   mediaTimeNS     determines network byte position for this time
     * @param   bytesPosition   returns byte position for given media time
     *
     * @return  RI_ERROR_NONE
     */
    ri_error (*pipeline_hn_server_get_byte_for_time)(
            const char* filepath, const char* filename,
            int64_t mediaTimeNS, int64_t* bytePosition);

    /**
     * Returns the file path and name associated with the supplied tsb handle.
     *
     * @param tsbInfo   structure containing tsb information
     * @param file_path path to the file which contains the tsb contents
     * @param file_name name of the file which contains the tsb contents
     *
     * @return returns RI_ERROR_NONE
     */
    ri_error (*pipeline_hn_server_get_tsb_file_name_path)(ri_tsbHandle tsbInfo,
              char* file_path, char* file_name);

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
    ri_error (*pipeline_transform_live_stream)(ri_pipeline_t* object,
                                               int tuner,
                                               ri_transformation_t* ct);

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
    ri_error (*pipeline_transform_file_stream)(ri_pipeline_t* object,
                                               char* file_path,
                                               char* file_name,
                                               ri_transformation_t* ct);

    /**
     * Returns a given pipeline's transformation status
     *
     * @param   pPipeline   associated pipeline
     * @param   status      the transformation status in a human-readable str
     * @param   buffer_size the size of the status buffer to write into
     *
     * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
     *    RI_ERROR_ILLEGAL_ARG: Invalid parameters supplied
     *    RI_ERROR_GENERAL: any other error encountered
     */
    ri_error (*pipeline_transform_status)(ri_pipeline_t* object, char* status,
                                          int buffer_size);

    /// count of the number of QoS messages received on the pipeline bus
    unsigned long qos_messages;

    // Private pipeline data
    ri_pipeline_data_t* data;

};

#endif /* _RI_PIPELINE_H_ */
