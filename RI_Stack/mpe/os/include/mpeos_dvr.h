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

#ifndef _MPEOS_DVR_H_
#define _MPEOS_DVR_H_

/** @file */

#include <mpe_types.h>
#include <mpe_error.h>
#include <mpeos_media.h>
#include <mpeos_si.h>        /* for mpe_SiElemStreamType */
#include <mpeos_event.h>
#include <mpeos_disp.h>
#include <mpeos_storage.h>
#include <os_dvr.h>

/**
 * MPEOS DVR API return codes.
 * Most MPEOS DVR APIs return one of these values.  Some values may be returned by all
 * APIs <b>(ALL)</b> while others are only returned by APIs that specifically
 * document their use.
 */
typedef enum mpe_DvrError
{
    MPE_DVR_ERR_NOERR,           /**< <b>(ALL)</b> No error. */

    MPE_DVR_ERR_INVALID_PARAM,   /**< <b>(ALL)</b> One or more ouput parameter pointers
                                    are null or an input parameter is invalid */
    MPE_DVR_ERR_OS_FAILURE,      /**< <b>(ALL)</b> An OS-specific error occurred */
    MPE_DVR_ERR_NOT_IMPLEMENTED, /**< <b>(ALL)</b> The requested operation is supported
                                    but has not yet been implemented */
    MPE_DVR_ERR_DEVICE_ERR,      /**< <b>(ALL)</b> A hardware error occurred */

    MPE_DVR_ERR_UNSUPPORTED,     /**< The requested operation is not supported */
    MPE_DVR_ERR_NOT_ALLOWED,     /**< Operation is not allowed */
    MPE_DVR_ERR_OUT_OF_SPACE,    /**< Storage device has insufficient space */
    MPE_DVR_NO_ACTIVE_SESSION,   /**< The specified session identifier does not correspond to an active DVR session */
    MPE_DVR_ERR_BUF_TOO_SMALL,   /**< The given buffer is too small to hold to expected return contents */

} mpe_DvrError;

/**
 * Maximum native recording name length.
 * Identifies the maximum length in characters (including null terminator) of a unique
 * native recording name used to identify a piece of recorded content.  Ports should
 * define the value <code>OS_DVR_MAX_NAME_SIZE</code> to the maximum supported
 * recording name length
 */
#define MPE_DVR_MAX_NAME_SIZE OS_DVR_MAX_NAME_SIZE

/**
 * Media time indicating end-of-content.
 * Defines a media time value that can be used to:
 *    - Jump to the end of recorded content
 *    - Switch to the live point in time-shifted content.
 * Ports should define the value <code>OS_DVR_POSITIVE_INFINITY</code> to
 * a value appropriate for their platform
 */
#define MPE_DVR_POSITIVE_INFINITY OS_DVR_POSITIVE_INFINITY

/**
 * Defines the maximum number of characters (including null-terminator) in the path
 * for a media storage volume.  Ports should define that value
 * <code>OS_DVR_MEDIA_VOL_MAX_PATH_SIZE</code> to the maximum supported length
 * of media storage volume path names.
 */
#define MPE_DVR_MEDIA_VOL_MAX_PATH_SIZE OS_DVR_MEDIA_VOL_MAX_PATH_SIZE

/*! \public
 * DVR-related events.
 * Each MPEOS API that takes a mpe_EventQueue as a parameter will document which of
 * these events can be sent to that queue.  In addition to the event itself, certain
 * events also require the implementation to pass event-specific data to 
 * mpeos_eventQueueSend.  For ALL events, the implementation must pass the
 * "asynchronous completion token" (ACT) as optionalEventData2.  The ACT is passed to
 * each MPEOS API call that registers an event queue..
 */
typedef enum mpe_DvrEvent
{
    /**
     * A session was terminated due to insufficient storage space
     * <ul>
     *   <li>optionalEventData1 : N/A</li>
     *   <li>optionalEventData3 : N/A</li>
     * </ul>
     */
    MPE_DVR_EVT_OUT_OF_SPACE = 0x1000,

    /**
     * A playback session has reached the beginning of recorded content
     * <ul>
     *   <li>optionalEventData1 : N/A</li>
     *   <li>optionalEventData3 : N/A</li>
     * </ul>
     */
    MPE_DVR_EVT_END_OF_FILE,

    /**
     * A playback session has reached the end of recorded content
     * <ul>
     *   <li>optionalEventData1 : N/A</li>
     *   <li>optionalEventData3 : N/A</li>
     * </ul>
     */
    MPE_DVR_EVT_START_OF_FILE,

    /**
     * A TSB-to-recording conversion process has terminated
     * <ul>
     *   <li>optionalEventData1 : N/A</li>
     *   <li>optionalEventData3 : N/A</li>
     * </ul>
     */
    MPE_DVR_EVT_CONVERSION_STOP,

    /**
     * Indicates a pid change during a playback session
     * <ul>
     *   <li>optionalEventData1 : N/A</li>
     *   <li>optionalEventData3 : N/A</li>
     * </ul>
     */
    MPE_DVR_EVT_PLAYBACK_PIDCHANGE, 

    /**
     * Must be sent by the implementation after any DVR session (playback, recording
     * conversion, or buffering) has been terminated.  The signaling of this event
     * signifies the end of the DVR activity.
     * <ul>
     *   <li>optionalEventData1 : N/A</li>
     *   <li>optionalEventData3 : N/A</li>
     * </ul>
     */
    MPE_DVR_EVT_SESSION_CLOSED,

    /**
     * Buffering/recording session has started.
     * <ul>
     *   <li>optionalEventData1 : N/A</li>
     *   <li>optionalEventData3 : N/A</li>
     * </ul>
     */
    MPE_DVR_EVT_SESSION_RECORDING,

    /**
     * The buffering/recording session is not recording due to lack of
     * source data (i.e. PID stream).
     * <ul>
     *   <li>optionalEventData1 : N/A</li>
     *   <li>optionalEventData3 : N/A</li>
     * </ul>
     */
    MPE_DVR_EVT_SESSION_NO_DATA,

    /**
     * Indicates a CCI (Copy Control Information) change for a TSB
     * buffering session (see the OpenCable CableCARD Copy Protection
     * 2.0 Specification for details).
     * <ul>
     *   <li>optionalEventData1 : N/A</li>
     *   <li>optionalEventData3 : uimsbf - RESERVED(24 bits) | CCI(8 bits)</li>
     * </ul>
     */
    MPE_DVR_EVT_CCI_UPDATE,

    /**
     * Indicates the playback media time alarm has been crossed/encountered.
     * OptionalEventData1 contains the affected session identifier.
     * <ul>
     *   <li>optionalEventData1 : N/A</li>
     *   <li>optionalEventData3 : N/A</li>
     * </ul>
     */
    MPE_DVR_EVT_PLAYBACK_ALARM,
} mpe_DvrEvent;

/*! \public
 * DVR Information identifiers.
 * The functions mpeos_dvrGet(), mpeos_dvrRecordingGet(), mpeos_dvrTsbGet()
 * are used to set or retrieve values from the DVR sub-system.  The following
 * values identify the particular piece of information that is to be retrieved
 * or modified.  See the documentation for the individual functions for the
 * list of identifiers supported by each
 */
typedef enum mpe_DvrInfoParam
{
    /**
     * Current length (in media time nanoseconds) of this recording
     */
    MPE_DVR_MEDIA_TIME,

    /**
     * The TSB start time (in nanoseconds) since buffering was started.
     * The value should be greater than 0 if the TSB has wrapped
     */
    MPE_DVR_TSB_START_TIME,

    /**
     * The TSB end time (in nanoseconds) since buffering was started.
     * Represents the total length of content buffered since buffering
     * was started.
     */
    MPE_DVR_TSB_END_TIME,

    /**
     * The smallest time shift buffer (in seconds) that the implementation<
     * can create<
     */
    MPE_DVR_TSB_MIN_BUF_SIZE,

    /**
     * Current size on disk of this recording (in bytes)
     */
    MPE_DVR_RECORDING_SIZE,

    /**
     * Current length (in seconds) of this recording
     * Note: the parameter is depreciated - use MPE_DVR_RECORDING_LENGTH_MS</td>
     */
    MPE_DVR_RECORDING_LENGTH,

    /**
     * List of PID information structures describing the PIDs associated<
     * with this recording
     */
    MPE_DVR_PID_INFO,

    /**
     * Number of PIDs associated with this recording
     */
    MPE_DVR_PID_COUNT,

    /**
     * Maximum supported bit rate for recordings
     */
    MPE_DVR_MAX_BITRATE,

    /**
     * Maximum recording bandwidth
     */
    MPE_DVR_MAX_RECORDING_BANDWIDTH,

    /**
     * Maximum playback bandwidth
     */
    MPE_DVR_MAX_PLAYBACK_BANDWIDTH,

    /**
     * Does the implementation support simultaneous play and record?
     * Return <b>1</b> for yes, <b>0</b> for no
     */
    MPE_DVR_SIMULTANEOUS_PLAY_RECORD,

    /**
     * The size of the media filesystem partition (in bytes) of the
     * given storage device
     */
    MPE_DVR_STORAGE_MEDIAFS_CAPACITY,

    /**
     * The size (in bytes) of un-partioned media filesystem space on
     * the given storage device
     */
    MPE_DVR_STORAGE_MEDIAFS_ALLOCATABLE_SPACE,

    /**
     * The amount of free space (in bytes) available on the media filesystem
     * partition on the given storage device
     */
    MPE_DVR_STORAGE_MEDIAFS_FREE_SPACE,

    /**
     * Does the implementation support conversion from a TSB on one
     * media storage volume to a permanent recording on another?
     * Return <b>1</b> for yes, <b>0</b> for no
     */
    MPE_DVR_SUPPORTS_CROSS_MSV_TSB_CONVERT,

    /**
     * Many of the MPEOS APIs refer to <i>media time</i>.  The returned  value
     * is the basis for media time.  For implementations in which media time starts
     * at 0, you would return 0
     */
    MPE_DVR_MEDIA_START_TIME,

    /**
     * Current length (in milliseconds) of this recording</td>
     */
    MPE_DVR_RECORDING_LENGTH_MS,
} mpe_DvrInfoParam;

/*! \public
 * DVR recording/playback quality.
 * Platforms must report their supported maximum recording and playback  
 * bitrate in one of these forms (units are kilobits/second).  Returned
 * by mpeos_dvrGet() in response to mpe_DvrInfoParam values of:
 * - ::MPE_DVR_MAX_BITRATE
 * - ::MPE_DVR_MAX_RECORDING_BANDWIDTH
 * - ::MPE_DVR_MAX_PLAYBACK_BANDWIDTH
 */
typedef enum mpe_DvrBitRate
{
    MPE_DVR_BITRATE_LOW = 6000,
    MPE_DVR_BITRATE_MEDIUM = 12000,
    MPE_DVR_BITRATE_HIGH = 19500
} mpe_DvrBitRate;

/*! \public
 * Direction identifier.
 * Used by mpeos_dvrRecordingMediaTimeForFrame() and mpeos_dvrPlaybackStepFrame()
 * to indicate a relative direction in the stream 
 */
typedef enum mpe_DvrDirection
{
    MPE_DVR_DIRECTION_FORWARD,
    MPE_DVR_DIRECTION_REVERSE
} mpe_DvrDirection;

/**
 * Convenience typedef for recording name strings
 */
typedef char mpe_DvrString_t[MPE_DVR_MAX_NAME_SIZE];

#define MPE_DVR_PID_UNKNOWN (-1) /**< An unknown or uninitialized PID value */
#define MPE_DVR_MAX_PIDS    (10) /**< The maximum number of Pids allowed in the mpe_DvrPidTable */

/**
 * See mpeos_dvrRecordingPlayStart(), mpeos_dvrTsbPlayStart(), mpeos_dvrPlaybackSetAlarm(),
 */
#define MPE_DVR_MEDIATIME_UNSPECIFIED (-1)

/*! \public
 * Media stream types.
 * Identifies the type of data carried on a particular PID.  These values
 * correspond to constants defined in the Java class MediaStreamType.
 */
typedef enum mpe_DvrMediaStreamType
{
    MPE_DVR_MEDIA_UNKNOWN = 0,
    MPE_DVR_MEDIA_VIDEO = 1,
    MPE_DVR_MEDIA_AUDIO = 2,
    MPE_DVR_MEDIA_DATA = 3,
    MPE_DVR_MEDIA_SUBTITLES = 4,
    MPE_DVR_MEDIA_SECTIONS = 5,
    MPE_DVR_MEDIA_PCR = 6,
    MPE_DVR_MEDIA_PMT = 7
} mpe_DvrMediaStreamType;

/**
 * Recording information for a single PID.
 */
typedef struct _mpe_DvrPidInfo
{
    /** Type of data carried on this PID */
    mpe_DvrMediaStreamType streamType;

    /** Original PID value as delivered over the network */
    int16_t srcPid;

    /**
     * PID value as recorded. If PID re-mapping occured during the
     * recording process, this field may contain a value different
     * from the srcPid.  This field is populated by the platform.
     * If no PID re-mapping occurs, this field should be set
     * to the same value as srcPid
     */
    int16_t recPid;

    /**
     * Specific format of media carried on this PID as originally
     * delivered over the network.
     */
    mpe_SiElemStreamType srcEltStreamType;

    /**
     * Specific format of media carriend on this PID as it was
     * recorded. If media transcoding occurred during the recording
     * process, this field may contain a different value from the
     * srcEltStreamType.  This field is populated by the platform.
     * If no transcoding occurs, this field should be set to the
     * same value as srcEltStreamType
     */
    mpe_SiElemStreamType recEltStreamType;  

} mpe_DvrPidInfo;

/**
 * Defines a list of PIDs associated with a specific media time.
 */
typedef struct _mpe_DvrPidTable
{
    int64_t mediaTime;                     /**< Media time in nanoseconds */
    uint32_t count;                        /**< Number of PIDs in the array */
    mpe_DvrPidInfo pids[MPE_DVR_MAX_PIDS]; /**< PIDs */
} mpe_DvrPidTable;

/**
 * Handle representing an active DVR playback session.
 * This is a dummy structure meant to enforce of use of pointers in the
 * MPEOS implementation.  MPEOS handles will be re-worked in the future
 * to make this less of a hack
 */
typedef struct _mpe_DvrPlaybackH
{
    int unused1; /**< Unused */
} *mpe_DvrPlayback; 

/**
 * Handle representing an active TSB buffering session.
 * This is a dummy structure meant to enforce of use of pointers in the
 * MPEOS implementation.  MPEOS handles will be re-worked in the future
 * to make this less of a hack
 */
typedef struct _mpe_DvrBufferingH
{
    int unused1; /**< Unused */
} *mpe_DvrBuffering; 

/**
 * Handle representing an physical time shift buffer.
 * This is a dummy structure meant to enforce of use of pointers in the
 * MPEOS implementation.  MPEOS handles will be re-worked in the future
 * to make this less of a hack
 */
typedef struct _mpe_DvrTsbH
{
    int unused1; /**< Unused */
} *mpe_DvrTsb;

/**
 * Handle representing an active TSB conversion session..
 * This is a dummy structure meant to enforce of use of pointers in the
 * MPEOS implementation.  MPEOS handles will be re-worked in the future
 * to make this less of a hack
 */
typedef struct _mpe_DvrConversionH
{
    int unused1; /**< Unused */
} *mpe_DvrConversion;

/**
 * Handle representing a media storage volume.
 */
typedef os_MediaVolumeInfo* mpe_MediaVolume;

/**
 * DVR Information identifiers.
 * The functions mpeos_dvrMediaVolumeGetInfo() and mpeos_dvrMediaVolumeSetInfo(),
 * are used to set or retrieve values associated with a media storage volume.
 * The following values identify the particular piece of information that is to
 * be retrieved or modified.  See the documentation for the individual functions
 * for the list of identifiers supported by each
 */
typedef enum mpe_MediaVolumeInfoParam
{
    /**
     * Total size (in bytes) of the given media storage volume
     */
    MPE_DVR_MEDIA_VOL_SIZE,

    /**
     * Total size (in bytes) of unused space on the given media
     * storage volume
     */
    MPE_DVR_MEDIA_VOL_FREE_SPACE,

    /**
     * Absolute path to the root of the given media storage volume
     */
    MPE_DVR_MEDIA_VOL_PATH,

    /**
     * The time (in milliseconds since the Unix epoch - January 1,
     * 00:00:00 UTC) that the given media storage volum was created
     */
    MPE_DVR_MEDIA_VOL_CREATE_TIME,

    /**
     * The smallest TSB size (in bytes) that can be created on the
     * given media storage volume.  The default value for newly-created
     * MSVs is implementation-dependent and should reflect the amount
     * of space reserved exclusively for TSBs. Also, if the platform
     * cannot support setting the size to the specified value, it should
     * disregard the set value and report the previously-set value.
     */
    MPE_DVR_MEDIA_VOL_TSB_MIN_SIZE_BYTES
} mpe_MediaVolumeInfoParam;

/*! \public
 * Media volume events that may be delivered to the queue registered via
 * mpe_dvrMediaVolumeRegisterQueue()
 */
typedef enum
{
    /**
     * An alarm registered by mpeos_dvrMediaVolumeAddAlarm() has gone off
     * due to volume free space reaching the alarm level
     */
    MPE_MEDIA_VOL_EVT_FREE_SPACE_ALARM = 0x1100
} mpe_MediaVolumeEvent;

/*****************************************************************************/
/* Porting layer function prototypes                                        */
/*****************************************************************************/

/**
 * Return all trick mode rates supported by the platform for the given storage
 * device.  The play scale is array of floating point values in ascending order
 * starting with reverse playrates (e.g.
 * <code>{ -32.0, -16.0, -4.0, -2.5, -0.5, 0.0, 0.5, 2.5, 4.0, 16.0, 32.0 }</code>
 *
 * @param device the storage device handle
 * @param playScales [out] Location where the implementation will return the
 *                   play scales array.
 * @param num [out] location where the implementation will return the number
 *            of entries in the returned play scales array
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrGetPlayScales(mpe_StorageHandle device,
                                 float **playScales,
                                 uint32_t *num);

/**
 * Return the amount of time it takes for the slowest device needed to make a
 * recording to resume from a low power state.
 *
 * @return the amount of time (in milliseconds) required for the DVR system
 *         to resume from a low power state
 */
uint32_t mpeos_dvrGetLowPowerResumeTime(void);

/**
 * Cause all devices associated with making a recording to be brought out of
 * a low power state.
 *
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrResumeFromLowPower(void);

/**
 * Return a list of existing recording names from the given storage device.
 * Each recording name can be at most ::MPE_DVR_MAX_NAME_SIZE bytes long
 * (including null terminator).  Memory for the returned array must be freed
 * by calling mpeos_dvrFreeRecordingList() before this function can be
 * called again. ::MPE_DVR_ERR_NOT_ALLOWED if the list returned by a previous
 * call has not yet been freed by calling mpeos_dvrGetRecordingList()
 *
 * @param device the storage device handle
 * @param count [out] location where the implementation will return the number
 *              of recording names in the list
 * @param recordingNames [out] location where the implementation will return
 *                       the array of strings representing recording names.
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrGetRecordingList(mpe_StorageHandle device,
                                    uint32_t* count,
                                    mpe_DvrString_t **recordingNames);

/**
 * Releases the memory allocated for the recording list returned by
 * the previous call to mpeos_dvrGetRecordingList(). ::MPE_DVR_ERR_NOT_ALLOWED
 * is returned if there is no list to be freed
 *
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrFreeRecordingList(void);

/**
 * Returns general information about the DVR sub-system
 *
 * Supported values for mpe_DvrInfoParam are:
 * <table>
 *   <tr>
 *      <th>mpe_DvrInfoParam</th>
 *      <th>input</th>
 *      <th>output</th>
 *   </tr>
 *   <tr>
 *      <td>::MPE_DVR_MAX_BITRATE</td>
 *      <td>N/A</td>
 *      <td>mpe_DvrBitRate*</td>
 *   </tr>
 *   <tr>
 *      <td>::MPE_DVR_MAX_RECORDING_BANDWIDTH</td>
 *      <td>N/A</td>
 *      <td>mpe_DvrBitRate*</td>
 *   </tr>
 *   <tr>
 *      <td>::MPE_DVR_MAX_PLAYBACK_BANDWIDTH</td>
 *      <td>N/A</td>
 *      <td>mpe_DvrBitRate*</td>
 *   </tr>
 *   <tr>
 *      <td>::MPE_DVR_SIMULTANEOUS_PLAY_RECORD</td>
 *      <td>N/A</td>
 *      <td>uint32_t*</td>
 *   </tr>
 *   <tr>
 *      <td>::MPE_DVR_SUPPORTS_CROSS_MSV_TSB_CONVERT</td>
 *      <td>N/A</td>
 *      <td>uint32_t*</td>
 *   </tr>
 *   <tr>
 *      <td>::MPE_DVR_STORAGE_MEDIAFS_CAPACITY</td>
 *      <td>mpe_StorageHandle*</td>
 *      <td>uint64_t*</td>
 *   </tr>
 *   <tr>
 *      <td>::MPE_DVR_STORAGE_MEDIAFS_ALLOCATABLE_SPACE</td>
 *      <td>mpe_StorageHandle*</td>
 *      <td>uint64_t*</td>
 *   </tr>
 *   <tr>
 *      <td>::MPE_DVR_STORAGE_MEDIAFS_FREE_SPACE</td>
 *      <td>mpe_StorageHandle*</td>
 *      <td>uint64_t*</td>
 *   </tr>
 *   <tr>
 *      <td>::MPE_DVR_TSB_MIN_BUF_SIZE</td>
 *      <td>N/A</td>
 *      <td>uint32_t*</td>
 *   </tr>
 *   <tr>
 *      <td>::MPE_DVR_MEDIA_START_TIME</td>
 *      <td>N/A</td>
 *      <td>uint32_t*</td>
 *   </tr>
 * </table>
 *
 * @param param the information identifier.
 * @param input additional data associated with the request
 * @param output location where the implementation will return the requested
 *        DVR information
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrGet(mpe_DvrInfoParam param,
                       void *input,
                       void *output);

/**
 * Removes the given recorded content from the disk. An active recording cannot be
 * deleted until the recording session is terminated.  A recording being played
 * back can not be deleted until the playback session is terminated.
 * ::MPE_DVR_ERR_NOT_ALLOWED is returned if the recording is currently associated
 * with an active recording or playback session
 *
 * @param recordingName the unique recording name representing the content to
 *        be deleted
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrRecordingDelete(char *recordingName);

/**
 * Retrieves information related to a recording.
 *
 * Possible mpe_DvrInfoParam are:
 * <table>
 *   <tr>
 *      <th>mpe_DvrInfoParam</th>
 *      <th>output</th>
 *   </tr>
 *   <tr>
 *      <td>::MPE_DVR_RECORDING_SIZE</td>
 *      <td>uint64_t*</td>
 *   </tr>
 *   <tr>
 *      <td>::MPE_DVR_RECORDING_LENGTH</td>
 *      <td>uint64_t*</td>
 *   </tr>
 *   <tr>
 *      <td>::MPE_DVR_RECORDING_LENGTH_MS</td>
 *      <td>uint64_t*</td>
 *   </tr>
 *   <tr>
 *      <td>::MPE_DVR_MEDIA_TIME</td>
 *      <td>uint64_t*</td>
 *   </tr>
 *   <tr>
 *      <td>::MPE_DVR_PID_COUNT</td>
 *      <td>uint32_t*</td>
 *   </tr>
 *   <tr>
 *      <td>::MPE_DVR_PID_INFO</td>
 *      <td>mpe_DvrPidInfo* (array of mpe_DvrPidInfo of length returned by<BR>
 *      call to this function using param ::MPE_DVR_PID_COUNT</td>
 *   </tr>
 * </table>
 *
 * @param device handle to the storage device containing the recording
 * @param recordingName unique recording identifier
 * @param param the information identifier.
 * @param output location where the implementation will return the requested
 *        recording information.  See the table above for specific pointer return
 *        type
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrRecordingGet(mpe_StorageHandle device,
                                char *recordingName,
                                mpe_DvrInfoParam param,
                                void *output);

/**
 * Returns the media time of the closest renderable frame to the given mediaTime
 * in the given direction. It is expected that calling mpeos_dvrPlaybackGetTime()
 * after calling mpeos_dvrPlaybackSetTime() with a value returned from this function
 * will result in the same value returned in frameTime.
 *
 * @param recordingName unique recording identifier
 * @param mediaTime the media time of interest.
 * @param direction the desired direction for the nearest renderable frame
 * @param frameTime [out] the location where the implementation will store
 *        the media time corresponding with the nearest renderable frame
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrRecordingMediaTimeForFrame(char * recordingName,
                                              int64_t mediaTime,
                                              mpe_DvrDirection direction,
                                              int64_t * frameTime);

/**
 * Starts a new DVR playback session for a given recording name.  A new playback handle
 * is returned if the call is successful. The recording will start playing back at the
 * normal rate (1.0) at the given media time.
 *
 * The following events may be delivered to the given event queue:
 * <ul>
 *   <li>::MPE_CONTENT_PRESENTING</li>
 *   <li>::MPE_DVR_EVT_START_OF_FILE</li>
 *   <li>::MPE_DVR_EVT_END_OF_FILE</li>
 *   <li>::MPE_DVR_EVT_SESSION_CLOSED</li>
 *   <li>::MPE_DVR_EVT_PLAYBACK_PIDCHANGE</li>
 *   <li>::MPE_DVR_EVT_PLAYBACK_ALARM</li>
 * </ul>
 *
 * @param recordingName unique recording identifier.
 * @param videoDevice video device handle indicating where the media will be displayed
 * @param pids array of PIDs to be played back (only the <code>streamType</code>,
 *        <code>srcEltStreamType</code>, and <code>srcPid</code> need be populated).
 *        The stack will always provide 0 or 1 audio pids, 0 or 1 video pids, and
 *        exactly 1 PCR pid.
 * @param pidCount number of pids in the array.
 * @param initialMediaTime the media time position (in nanoseconds) from which to begin
          playback
 * @param requestedRate the requested playback rate.
 * @param actualRate [out] location where the implementation will return the actual
 *        playback rate
 * @param blocked indicates whether the video is initially considered blocked (See
 *        mpeos_dvrPlaybackBlockPresentation() for more information)
 * @param muted indicates whether the audio should be muted
 * @param requestedGain the requested audio gain level
 * @param actualGain [out] location where the implementation will return the actual gain
 * @param cci The Copy Control Information associated with the content being played back,
 *        per Section 9 of the OpenCable CableCARD Copy Protection 2.0 Specification
 *        (CCCP 2.0). Values of CCI indicate whether output-port-specific content
 *        protection should be applied.
 * @param alarmMediaTime the alarm media time is expressed in nanoseconds and can be any
 *        value between 0 (the beginning of the stream) and the recording length. If not
 *        ::MPE_DVR_MEDIATIME_UNSPECIFIED, this value represents the media time where the
 *        platform must issue ::MPE_DVR_EVT_PLAYBACK_ALARM.
 * @param queueId the event queue to post events related to this playback session
 * @param act the completion token delivered with events (optionalEventData2)
 * @param playback [out] location where the implementation will return the playback
 *        session handle
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrRecordingPlayStart(char *recordingName,
                                      mpe_DispDevice videoDevice,
                                      mpe_DvrPidInfo *pids, uint32_t pidCount,
                                      int64_t initialMediaTime,
                                      float requestedRate, float *actualRate,
                                      mpe_Bool blocked,
                                      mpe_Bool muted,
                                      float requestedGain, float *actualGain,
                                      uint8_t cci, int64_t alarmMediaTime,
                                      mpe_EventQueue queueId, void *act,
                                      mpe_DvrPlayback *playback);

/**
 * Modifies the PIDs being presented in an active playback session Returns 
 * ::MPE_DVR_ERR_UNSUPPORTED if the platform does not support this operation.
 *
 * @param playback a handle to an active playback session.  Returns
 *        ::MPE_DVR_NO_ACTIVE_SESSION if the session handle is invalid
 * @param pids array of PIDs to play back (the <code>streamType</code>,
 *        <code>srcEltStreamType</code>, and <code>srcPid</code> describe the PIDs to
 *        be played back).
 * @param pidCount number of pids in the array
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrPlaybackChangePids(mpe_DvrPlayback playback,
                                      mpe_DvrPidInfo *pids, uint32_t pidCount);

/**
 * Stops a playback session. It is expected that playback has stopped once this
 * function returns, the data source is no longer being read from, and the video
 * device is no longer rendering content.
 *
 * @param playback a handle to an active playback session.  Returns
 *        ::MPE_DVR_NO_ACTIVE_SESSION if the session handle is invalid
 * @param holdFrameMode the presentation mode to use once the playback has stopped
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrPlayBackStop(mpe_DvrPlayback playback,
                                mpe_MediaHoldFrameMode holdFrameMode);

/**
 * Returns the media time position of the given playback session
 *
 * @param playback a handle to an active playback session.  Returns
 *        ::MPE_DVR_NO_ACTIVE_SESSION if the session handle is invalid
 * @param mediaTime [out] the location where the implementation will store the
 *        current media time position (in nanoseconds)
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrPlaybackGetTime(mpe_DvrPlayback playback,
                                   int64_t *mediaTime);

/**
 * Sets the media time position of the given playback session
 *
 * @param playback a handle to an active playback session.  Returns
 *        ::MPE_DVR_NO_ACTIVE_SESSION if the session handle is invalid
 * @param mediaTime the requested media time position
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrPlaybackSetTime(mpe_DvrPlayback playback,
                                   int64_t mediaTime);

/**
 * Set the Copy Control Information (CCI) associated with the given playback. This
 * overrides the previous CCI associated with this playback session and may result in
 * a change in the copy protection applied to the content when output to one or more
 * ports (e.g. analog protection or DTCP encryption).
 *
 * @param playback a handle to an active playback session.  Returns
 *        ::MPE_DVR_NO_ACTIVE_SESSION if the session handle is invalid
 * @param cci The Copy Control Information associated with the content being played
 *        back, per Section 9 of the OpenCable CableCARD Copy Protection 2.0
 *        Specification (CCCP 2.0). Values of CCI indicate whether output-port-specific
 *        content protection should be applied.
 * @return ::MPE_SUCCESS or a supported error code
 */
mpe_Error mpeos_dvrPlaybackSetCCI(mpe_DvrPlayback playback, uint8_t cci);


/**
 * This function steps one video frame forward or backward on a paused playback session.
 * The next video frame may be the next fully-coded frame (e.g. an MPEG-2 I/P frame) or
 * an intermediate frame, if the platform supports it. After a successful call, the
 * media time returned by mpeos_dvrPlaybackGetTime() must reflect the selected frame.
 *
 * @param playback a handle to an active playback session.  Returns
 *        ::MPE_DVR_NO_ACTIVE_SESSION if the session handle is invalid.  Returns
 *        ::MPE_DVR_ERROR_UNSUPPORTED if the playback session is not in the paused
 *        state or if this functionality is not supported by the native platform
 * @param stepDirection the direction to step
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrPlaybackStepFrame(mpe_DvrPlayback playback,
                                     mpe_DvrDirection stepDirection);

/**
 * Block or unblock the presentation of the given playback session.  Blocking dvr
 * playback is accomplished by muting audio and displaying a black video area.
 * The dvr playback continues to process the stream as expected. However, the audio
 * is not emitted and the video is not displayed.
 *
 * @param playback a handle to an active playback session.  Returns
 *        ::MPE_DVR_NO_ACTIVE_SESSION if the session handle is invalid
 * @param block TRUE if presentation should be blocked, FALSE otherwise
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrPlaybackBlockPresentation(mpe_DvrPlayback playback,
                                             mpe_Bool block);

/**
 * Set the current mute state of the given playback session.
 *
 * @param playback a handle to an active playback session.  Returns
 *        ::MPE_DVR_NO_ACTIVE_SESSION if the session handle is invalid
 * @param mute if TRUE, audio must be muted regardless of content format and the
 *        gain value must be unaffected. When FALSE, audio must be restored and
 *        gain must be set to the pre-mute level.
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrPlaybackSetMute(mpe_DvrPlayback playback,
                                   mpe_Bool mute);

/**
 * Set the current decibel gain for the given playback session. Positive values
 * amplify the audio signal and negative values attenuate the signal.  If the
 * platform does not support the setting of the gain level for the presenting content
 * format, the returned actual gain value shall be 0.0
 *
 * @param playback a handle to an active playback session.  Returns
 *        ::MPE_DVR_NO_ACTIVE_SESSION if the session handle is invalid
 * @param gain the requested gain value, in decibels
 * @param actualGain [out] location where the implementation will return the actual
 *        gain value set for this playback session
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrPlaybackSetGain(mpe_DvrPlayback playback,
                                   float gain,
                                   float *actualGain);

/**
 * Set/change/clear the playback alarm.
 *
 * @param playback a handle to an active playback session.  Returns
 *        ::MPE_DVR_NO_ACTIVE_SESSION if the session handle is invalid
 * @param alarmMediaTime the alarm media time is expressed in nanoseconds and can be
 *        any value between 0 (the beginning of the stream) and the recording length.
 *        If ::MPE_DVR_MEDIATIME_UNSPECIFIED, the current alarm is cancelled. Otherwise
 *        this value represents the media time where the platform must issue
 *        ::MPE_DVR_EVT_PLAYBACK_ALARM when the alarm's media time is crossed in any
 *        direction and at any rate. Once an alarm has been signaled, the platform
 *        shall consider the alarm cancelled.
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrPlaybackSetAlarm(mpe_DvrPlayback playback, int64_t alarmMediaTime);

/**
 * Get the current input video scan mode.
 *
 * @param session is the active decode session to query for the scan mode
 * @param scanMode pointer to the field to be set with the current
 *        mpe_MediaScanMode
 *
 * @return MPE_EINVAL if session is invalid
 * @return MPE_SUCCESS if successful.
 */
mpe_Error mpeos_dvrPlaybackGetVideoScanMode (mpe_DvrPlayback playback, 
        mpe_MediaScanMode* scanMode);

/**
 * Get the 3DTV configuration for the DVR playback
 *
 * @param playback a handle to an active playback session.  Returns
 *        ::MPE_DVR_NO_ACTIVE_SESSION if the session handle is invalid
 * @param stereoscopicMode [out] location where the implementation will return
 *        the current mpe_DispStereoscopicMode
 * @param payloadType [out] location where the implementation will return the 
 *        3D payload type of the media being played back
 * @param payload [out] array of bytes where the implementation will return
 *        payload of the 3DTV information description message for the media
 *        being played back.  ::MPE_ENOMEM is returned if the given array is
 *        too small to hold the payload,
 * @param payloadSz [in,out] the size of the payload array passed in by the caller.
 *        Upon successful return, contains the size of the actual number of bytes
 *        copied into the payload byte array.  If ::MPE_ENOMEM is returned, contains
 *        the number of bytes required to hold the current payload data so that
 *        the caller can make a subsequent call with a properly sized array
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrPlaybackGet3DConfig(mpe_DvrPlayback playback,
                                       mpe_DispStereoscopicMode* stereoscopicMode,
                                       mpe_Media3DPayloadType * payloadType,
                                       uint8_t * payload,
                                       uint32_t * payloadSz);

/**
 * Set the direction and speed of the given playback session.  In the case that
 * the given mode is not supported by the implementation, the closest supported
 * mode is set and returned.
 *
 * @param playback a handle to an active playback session.  Returns
 *        ::MPE_DVR_NO_ACTIVE_SESSION if the session handle is invalid
 * @param mode the requested trick mode
 * @param actualMode [out] location where the implementation will return the actual
 *        trick mode set by the system
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrSetTrickMode(mpe_DvrPlayback playback,
                                float mode, float *actualMode);

/**
 * Returns the current trick mode for the given playback session.
 *
 * @param playback a handle to an active playback session.  Returns
 *        ::MPE_DVR_NO_ACTIVE_SESSION if the session handle is invalid
 * @param mode [out] location where the implementation will return the current
 *        trick mode
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrGetTrickMode(mpe_DvrPlayback playback,
                                float *mode);

/**
 * Allocate a new time shift buffer on the given storage device. This call does
 * not start the buffering of content , but simply pre-allocates space on the
 * storage device for future use. This ensures that the time shift buffer is
 * available at the time of recording.  Returns ::MPE_DVR_ERR_OUT_OF_SPACE if
 * there is not sufficient space on the given storage device
 *
 * @param duration the requested duration (in seconds) of the time shift buffer
 * @param device a handle to the storage device where the buffer is to be created.
 * @param buffer [out] the location where the implementation shuold return a
 *        handle to the new buffer
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrTsbNew(mpe_StorageHandle device,
                          int64_t duration,
                          mpe_DvrTsb *buffer);

/**
 * This function deletes the given time shift buffer and release all resources
 * associated with it. The stack will always terminate any active buffering,
 * playback, or conversion sessions involving the given TSB before calling
 * this function.
 *
 * @param buffer a handle to the buffer to delete
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrTsbDelete(mpe_DvrTsb buffer);

/**
 * Initiates the recording of the specified PIDs into the given time shift
 * buffer.
 *
 * The following events may be delivered to the given event queue:
 * <ul>
 *   <li>::MPE_DVR_EVT_SESSION_RECORDING</li>
 *   <li>::MPE_DVR_EVT_SESSION_NO_DATA</li>
 *   <li>::MPE_DVR_EVT_OUT_OF_SPACE</li>
 *   <li>::MPE_DVR_EVT_CCI_UPDATE</li>
 *   <li>::MPE_DVR_EVT_SESSION_CLOSED</li>
 * </ul>
 *
 * @param tunerId the tuner that will provide the content that will be buffered
 * @param ltsid when non-0, identifies the Local Transport Stream ID carrying the content
 * @param buffer a handle to a time shift buffer that will hold the buffered content.
 * @param bitRate the desired recording quality
 * @param desiredDuration the duration (in seconds) that is requested for buffering.
 *        This value may be larger than the TSB's pre-allocated size (only intended
 *        for platforms with expandable/variable-rate TSBs. A 0 value indicates no
 *        preference.
 * @param maxDuration the maximum duration of content (in seconds) that the platform
 *        may hold in the TSB (e.g. when buffering is restricted due to copy control).
 *        Note: This value may be smaller than the TSB's allocated size/duration. A
 *        0 value indicates no maximum.
 * @param queueId the event queue to post events related to this playback session
 * @param act the completion token delivered with events (optionalEventData2)
 * @param pids [int,out] array of PIDs to be played back (the <code>streamType</code>,
 *        <code>srcEltStreamType</code>, and <code>srcPid</code> describe the PIDs to
 *        be recorded).  Upon successful return, the implementation will set the 
 *        <code>recEltStreamType</code>, and <code>recPid</code> values for each PID
 *        to represent any pid re-mapping or transcoding that is taking place during
 *        buffering.
 * @param pidCount number of pids in the array
 * @param tsbSession [out] the location where the implementation will return a handle
 *        to the buffering session
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrTsbBufferingStart(uint32_t tunerId, uint8_t ltsid,
                                     mpe_DvrTsb buffer,
                                     mpe_DvrBitRate bitRate,
                                     int64_t desiredDuration,
                                     int64_t maxDuration,
                                     mpe_EventQueue queueId, void *act,
                                     mpe_DvrPidInfo *pids, uint32_t pidCount, 
                                     mpe_DvrBuffering *tsbSession);

/**
 * Terminates a buffering session
 *
 * @param tsbSession a handle to an active tsb buffering session.  Returns
 *        ::MPE_DVR_NO_ACTIVE_SESSION if the session handle is invalid
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrTsbBufferingStop(mpe_DvrBuffering tsbSession);

/**
 * Starts a new DVR playback from a given time shift buffer. A new playback handle is
 * returned if the call is successful. The recording will start playing back at the
 * normal rate (1.0) at the given position. 
 *
 * The following events may be delivered to the given event queue:
 * <ul>
 *   <li>::MPE_CONTENT_PRESENTING</li>
 *   <li>::MPE_DVR_EVT_START_OF_FILE</li>
 *   <li>::MPE_DVR_EVT_END_OF_FILE</li>
 *   <li>::MPE_DVR_EVT_SESSION_CLOSED</li>
 *   <li>::MPE_DVR_EVT_PLAYBACK_PIDCHANGE</li>
 *   <li>::MPE_DVR_EVT_PLAYBACK_ALARM</li>
 * </ul>
 *
 * @param buffer a TSB handle indicating the buffer from which to play back
 * @param videoDevice video device handle indicating where the media will be displayed
 * @param pids array of PIDs to be played back (only the <code>streamType</code>,
 *        <code>srcEltStreamType</code>, and <code>srcPid</code> need be populated).
 *        The stack will always provide 0 or 1 audio pids, 0 or 1 video pids, and
 *        exactly 1 PCR pid.
 * @param pidCount number of pids in the array.
 * @param initialMediaTime the media time position (in nanoseconds) from which to begin
 *        playback
 * @param requestedRate the requested playback rate.
 * @param actualRate [out] location where the implementation will return the actual
 *        playback rate
 * @param blocked indicates whether the video is initially considered blocked (See
 *        mpeos_dvrPlaybackBlockPresentation() for more information)
 * @param muted indicates whether the audio should be muted
 * @param requestedGain the requested audio gain level
 * @param actualGain [out] location where the implementation will return the actual gain
 * @param cci the Copy Control Information associated with the content being played back,
 *        per Section 9 of the OpenCable CableCARD Copy Protection 2.0 Specification
 *        (CCCP 2.0). Values of CCI indicate whether output-port-specific content
 *        protection should be applied.
 * @param alarmMediaTime the alarm media time is expressed in nanoseconds and can be any
 *        value between 0 (the beginning of the stream) and the recording length. If not
 *        ::MPE_DVR_MEDIATIME_UNSPECIFIED, this value represents the media time where the
 *        platform must issue ::MPE_DVR_EVT_PLAYBACK_ALARM.
 * @param queueId the event queue to post events related to this playback session
 * @param act the completion token delivered with events (optionalEventData2)
 * @param playback [out] location where the implementation will return the playback
 *        session handle
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrTsbPlayStart(mpe_DvrTsb buffer,
                                mpe_DispDevice videoDevice,
                                mpe_DvrPidInfo *pids, uint32_t pidCount,
                                int64_t initialMediaTime,
                                float requestedRate, float *actualRate,
                                mpe_Bool blocked,
                                mpe_Bool muted,
                                float requestedGain, float *actualGain,
                                uint8_t cci, int64_t alarmMediaTime,
                                mpe_EventQueue queueId, void *act,
                                mpe_DvrPlayback *playback);

/**
 * Retrieves information related to a time shift buffer.
 *
 * Possible mpe_DvrInfoParam are:
 * <table>
 *   <tr>
 *      <th>mpe_DvrInfoParam</th>
 *      <th>time</th>
 *   </tr>
 *   <tr>
 *      <td>::MPE_DVR_TSB_START_TIME</td>
 *      <td>int64_t*</td>
 *   </tr>
 *   <tr>
 *      <td>::MPE_DVR_TSB_END_TIME</td>
 *      <td>int64_t*</td>
 *   </tr>
 * </table>
 *
 * @param buffer A time shift buffer handle 
 * @param param the information identifier.
 * @param time [out] location where the implementation will return the requested
 *        TSB information
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrTsbGet(mpe_DvrTsb buffer,
                          mpe_DvrInfoParam param,
                          int64_t *time);

/**
 * This function returns the media time of the closest renderable frame to the given
 * mediaTime in the given direction on the given TSB. It is expected that calling
 * mpeos_dvrPlaybackGetTime() after calling mpeos_dvrPlaybackSetTime() with a value
 * returned from this function will result in the same value returned in frameTime.
 *
 * @param buffer a handle to a TSB
 * @param mediaTime the desired media time
 * @param direction desired direction for the nearest renderable frame
 * @param frameTime [out] the location where the implemention will return the media
 *        time corresponding to the nearest renderable frame
 * @return ::MPE_DVR_ERR_NOERR or a supported error code, ::MPE_DVR_ERR_INVALID_PARAM
 * if the given media time is not valid for the given TSB
 */
mpe_Error mpeos_dvrTsbMediaTimeForFrame(mpe_DvrTsb buffer,
                                        int64_t mediaTime,
                                        mpe_DvrDirection direction,
                                        int64_t * frameTime);

/**
 * Modifies the PIDs being recorded in an active buffering session. Returns 
 * ::MPE_DVR_ERR_UNSUPPORTED if the platform does not support this operation.
 *
 * @param tsbSession a handle to an active tsb buffering session.  Returns
 *        ::MPE_DVR_NO_ACTIVE_SESSION if the session handle is invalid
 * @param pids [in,out] array of PIDs to buffer (the <code>streamType</code>,
 *        <code>srcEltStreamType</code>, and <code>srcPid</code> describe the PIDs to
 *        be recorded).  Upon successful return, the implementation will set the 
 *        <code>recEltStreamType</code>, and <code>recPid</code> values for each PID
 *        to represent any pid re-mapping or transcoding that is taking place during
 *        buffering.
 * @param pidCount number of pids in the array
 * @param mediaTime [out] location where the implementation will store the actual
 *        media time of the PID change 
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrTsbBufferingChangePids(mpe_DvrBuffering tsbSession,
                                          mpe_DvrPidInfo *pids, uint32_t pidCount,
                                          int64_t *mediaTime);

/**
 * This method modifies the desiredDuration and maxDuration settings on an active
 * TSB buffering session. This operation must be performed without affecting
 * already-buffered content or ongoing playback session associated with the TSB.
 * ::MPE_DVR_ERR_UNSUPPORTED shall be returned if the platform does not support
 * this operation.
 *
 * @param tsbSession a handle to an active tsb buffering session.  Returns
 *        ::MPE_DVR_NO_ACTIVE_SESSION if the session handle is invalid
 * @param desiredDuration duration (in seconds) that is requested for buffering.
 *        This value may be larger than the TSB's pre-allocated size (only intended
 *        for platforms with expandable/variable-rate TSBs). A 0 value implies no
 *        change.
 * @param maxDuration maximum duration of content (in seconds) that the platform may
 *        hold in the TSB (e.g. when buffering is restricted due to copy control).
 *        Note: This value may be smaller than the TSB's allocated size/duration. A
 *        0 value implies no change.
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrTsbBufferingChangeDuration(mpe_DvrBuffering tsbSession,
                                              int64_t desiredDuration,
                                              int64_t maxDuration );

/**
 * Re-allocates the buffer associated with the given TSB, based on the given
 * duration. If the buffer is in use, existing content in the buffer should be
 * preserved. ::MPE_DVR_ERR_UNSUPPORTED is returned if the platform does not support
 * this operation.
 *
 * @param buffer handle to a time shift buffer
 * @param duration the new duration in seconds
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrTsbChangeDuration(mpe_DvrTsb buffer, int64_t duration);

/**
 * Converts the content of a TSB into a permanent file.  Returns ::MPE_DVR_ERR_OUT_OF_SPACE
 * if there is insufficient space on the given media storage volume to perform the 
 * conversion
 *
 * The following events may be delivered to the given event queue:
 * <ul>
 *   <li>::MPE_DVR_EVT_OUT_OF_SPACE</li>
 *   <li>::MPE_DVR_EVT_CONVERSION_STOP</li>
 * </ul>
 *
 * @param buffer a handle to a time shift buffer from which the conversion should 
 *        be performed
 * @param volume a handle to the media storage volume where the converted recording
 *        shall be stored
 * @param startTime [in,out] the requested conversion start time (in milliseconds since
 *        the Unix epoch - January 1, 00:00:00 UTC).  Upon successful return, the
 *        implementation shall return the actual conversion start time.
 * @param duration the desired length of the conversion in milliseconds.
 * @param bitRate the desired recording bit rate.
 * @param queueId the event queue to post events related to this playback session
 * @param act the completion token delivered with events (optionalEventData2)
 * @param pidTableCount number of pid Sets in the array. 0 if not supported.
 * @param pidTable [in, out] array of pids to be recorded. The actual recorded pids
 *        will be returned to the caller.
 * @param conversion [out] location where the implementation will return a handle
 *        representing the ongoing conversion session.  If the conversion was completed
 *        upon return of this function, the handle will be NULL
 * @param recordingName [out] the location where the implementation will return the
 *        unique name of the converted recording
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrTsbConvertStart(mpe_DvrTsb buffer,
                                   mpe_MediaVolume volume,
                                   int64_t *startTime,
                                   int64_t duration,
                                   mpe_DvrBitRate bitRate,
                                   mpe_EventQueue queueId, void *act,
                                   uint32_t pidTableCount, mpe_DvrPidTable *pidTable,
                                   mpe_DvrConversion *conversion,
                                   char *recordingName);

/**
 * Terminates an active TSB conversion session
 *
 * @param conversion handle to an active tsb conversion session.  Returns
 *        ::MPE_DVR_NO_ACTIVE_SESSION if the session handle is invalid
 * @param immediate TRUE if conversion should be stopped immediately.  In the case
 *        of immediate termination, all conversion resources in use by the given
 *        session must be released upon function return.  Additionally, only the
 *        ::MPE_DVR_EVT_SESSION_CLOSED event should be sent to the session event
 *        queue.  If immediate is FALSE, the conversion session may be terminated
 *        asynchronously.  Upon termination of the session ::MPE_DVR_EVT_CONVERSION_STOP
 *        should be sent, followed by ::MPE_DVR_EVT_SESSION_CLOSED.
 *        
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrTsbConvertStop(mpe_DvrConversion conversion,
                                  mpe_Bool immediate);

/**
 * Modifies the PIDs being converted in an active conversion session. Returns 
 * ::MPE_DVR_ERR_UNSUPPORTED if the platform does not support this operation.
 *
 * @param conversion handle to an active tsb conversion session.  Returns
 *        ::MPE_DVR_NO_ACTIVE_SESSION if the session handle is invalid
 * @param pids [in,out] array of PIDs to convert (the <code>streamType</code>,
 *        <code>srcEltStreamType</code>, and <code>srcPid</code> describe the PIDs to
 *        be recorded).  Upon successful return, the implementation will set the 
 *        <code>recEltStreamType</code>, and <code>recPid</code> values for each PID
 *        to represent any pid re-mapping or transcoding that is taking place during
 *        buffering.
 * @param pidCount number of pids in the array
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrTsbConvertChangePids(mpe_DvrConversion conversion,
                                        mpe_DvrPidInfo *pids, uint32_t pidCount);

/**
 * Determines if the recording has a format which can be decoded for presentation
 * by the implementation, e.g., the bit rate, resolution, and encoding are supported
 *
 * @return TRUE if the recording can be presented, FALSE otherwise
 */
mpe_Bool mpeos_dvrIsDecodable(char *recName);

/** 
 * Determines if the recording can be decrypted by the implementation
 *
 * @return TRUE if the recording can be decrypted, FALSE otherwise
 */
mpe_Bool mpeos_dvrIsDecryptable(char *recName);

/**
 * Retrieves the number of media volumes located on the specified storage device.
 *
 * @param device a handle to the storage device
 * @param count [out] location where the implementation will return the number of
 *        media storage volumes found 
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrMediaVolumeGetCount(mpe_StorageHandle device,
        uint32_t* count);

/**
 * Retrieves the list of media storage volumes on a given storage device. Returns
 * ::MPE_DVR_ERR_BUF_TOO_SMALL if the given media storage volume handle list is too
 * small to hold the actual list of handles
 *
 * @param device a handle to the storage device
 * @param count [in,out] indicates the length of the volumes array passed in.
 *        Upon return, the implementation will return the actual number of volume
 *        handles returned in the volumes array. 
 * @param volumes [out] location where the implementation will return the list of 
 *        media storage volume handles
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrMediaVolumeGetList(mpe_StorageHandle device,
        uint32_t* count, mpe_MediaVolume *volumes);

/**
 * Registers an event queue to receive media volume related events. Only one queue
 * may be registered at a time.  Subsequent calls replace the previously registered
 * queue.
 *
 * The following events shall be delivered to the given event queue:
 * <table>
 *   <tr>
 *      <th>Event</th>
 *      <th>optionalEventData1</th>
 *      <th>optionalEventData3</th>
 *   </tr>
 *   <tr>
 *      <td>::MPE_MEDIA_VOL_EVT_FREE_SPACE_ALARM</td>
 *      <td>mpe_MediaVolume</td>
 *      <td>alarm level</td>
 *   </tr>
 * </table>
 *
 * @param queueId the event queue to register
 * @param act the completion token delivered with events (optionalEventData2)
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrMediaVolumeRegisterQueue(mpe_EventQueue queueId, void* act);

/**
 * Registers a free space alarm for the specified media volume. Multiple alarms
 * may be registered for the same volume, but the level must be unique across
 * all free space alarms for that volume.  Returns ::MPE_DVR_ERR_NOT_ALLOWED if
 * if an alarm for the given level on the given volume has already been
 * registerexd
 *
 * @param volume the media volume on which to monitor free space levels
 * @param level specifies the level of free space remaining at which point the
 *        caller would like to be notified.  This value is a percentage of the
 *        specified media volume capacity.  The valid range of values is 1-99.
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrMediaVolumeAddAlarm(mpe_MediaVolume volume, uint8_t level);

/**
 * Unregisters a free space alarm for the specified media volume.  If an alarm
 * for the given level on the given volume has not been registered, no action
 * is taken
 *
 * @param volume the media volume
 * @param level the level to unregister
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrMediaVolumeRemoveAlarm(mpe_MediaVolume volume, uint8_t level);

/**
 * Retrieves information related to a media storage volume.
 *
 * Possible mpe_MediaVolumeInfoParam are:
 * <table>
 *   <tr>
 *      <th>mpe_MediaVolumeInfoParam</th>
 *      <th>output</th>
 *   </tr>
 *   <tr>
 *      <td>::MPE_DVR_MEDIA_VOL_SIZE</td>
 *      <td>uint64_t*</td>
 *   </tr>
 *   <tr>
 *      <td>::MPE_DVR_MEDIA_VOL_FREE_SPACE</td>
 *      <td>uint64_t*</td>
 *   </tr>
 *   <tr>
 *      <td>::MPE_DVR_MEDIA_VOL_PATH</td>
 *      <td>char[::MPE_STORAGE_MAX_PATH_SIZE]</td>
 *   </tr>
 *   <tr>
 *      <td>::MPE_DVR_MEDIA_VOL_CREATE_TIME</td>
 *      <td>uint32_t*</td>
 *   </tr>
 *   <tr>
 *      <td>::MPE_DVR_MEDIA_VOL_TSB_MIN_SIZE_BYTES</td>
 *      <td>uint64_t*</td>
 *   </tr>
 * </table>
 *
 * @param volume a media storage volume handle
 * @param param the information identifier.
 * @param output location where the implementation will return the requested
 *        media volume information
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrMediaVolumeGetInfo(mpe_MediaVolume volume,
        mpe_MediaVolumeInfoParam param, void* output);

/**
 * Creates a new media storage volume on a specified storage device.  By default,
 * the newly created media volume will have no minimum guaranteed size and may use
 * as much space as is available on the storage device that is not already reserved
 * by other media volumes on that device.  To change the size of the media volume, call
 * mpeos_dvrMediaVolumeSetInfo() with ::MPE_DVR_MEDIA_VOL_SIZE.  Returns
 * ::MPE_DVR_ERR_NOT_ALLOWED if the given media storage volume path already exists on
 * the given device
 *
 * @param device the handle of the storage device where the media volume is to
 *        be created
 * @param path this is the full path of the volume as it will be presented to
 *        applications via the LogicalStorageVolume.getPath() method implemented at
 *        the Java layer.  This path must be unique across all media volumes on the
 *        specified storage device.
 * @param volume [out] location where the implementation will return the handle to
 *        the newly created media volume
 * @return ::MPE_DVR_ERR_NOERR or a supported error code, ::MPE_DVR_ERR_INVALID_PARAM
 *         if the path does not begin with ::MPE_STORAGE_GPFS_PATH of the specified
 *         storage device
 */
mpe_Error mpeos_dvrMediaVolumeNew(mpe_StorageHandle device, char* path,
        mpe_MediaVolume* volume);

/**
 * Deletes a media storage volume.  When a media volume is deleted, any space
 * reserved for this volume on its associated storage device is immediately
 * returned to the device.  Subsequent calls to mpeos_dvrGet() using
 * ::MPE_DVR_STORAGE_MEDIAFS_ALLOCATABLE_SPACE should reflect this reclamation. 
 * Returns ::MPE_DVR_ERR_NOT_ALLOWED if the media volume is not empty. 
 * The stack will always terminate any active buffering, playback, or
 * conversion sessions involving the given media volume before calling this
 * function
 *
 * @param volume handle to the media volume to delete
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrMediaVolumeDelete(mpe_MediaVolume volume);

/**
 * Modifies information related to a media storage volume.
 *
 * Possible mpe_MediaVolumeInfoParam are:
 * <table>
 *   <tr>
 *      <th>mpe_MediaVolumeInfoParam</th>
 *      <th>value</th>
 *   </tr>
 *   <tr>
 *      <td>::MPE_DVR_MEDIA_VOL_TSB_MIN_SIZE_BYTES</td>
 *      <td>uint64_t*</td>
 *   </tr>
 * </table>
 *
 * @param volume a media storage volume handle
 * @param param the information identifier.
 * @param value the new value for the identifier
 * @return ::MPE_DVR_ERR_NOERR or a supported error code
 */
mpe_Error mpeos_dvrMediaVolumeSetInfo(mpe_MediaVolume volume,
        mpe_MediaVolumeInfoParam param, void* value);

#endif
