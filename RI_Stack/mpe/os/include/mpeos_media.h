#if !defined(_MPEOS_MEDIA_H)
#define _MPEOS_MEDIA_H
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

#ifdef __cplusplus
extern "C"
{
#endif

#include <mpe_types.h>  /* Resolve basic type references. */
#include <mpe_error.h>
#include "mpeos_event.h"
#include "mpeos_si.h"
#include "os_media.h"	/* Resolve target specific definitions. */
#include "mpeos_gfx.h"		// for mpe_GfxRectangle
#include "mpeos_disp.h"		// for mpe_DispDevice

//
// NB: These event numbers must be kept consistent with
//     those defined in the Java classes which receive them.
//

//
// Events arising from a tune request.
//
#define MPE_TUNE_STARTED            0x09 // Tuning attempt begins. This must be sent after any
                                         //  mpeos_mediaTune() that returns MPE_SUCCESS and before
                                         //  any other events. Note: mpeos_mediaTune() will not
                                         //  be called for the same tuner between the time that
                                         //  mpeos_mediaTune() is called and MPE_TUNE_STARTED is
                                         //  received.
#define MPE_TUNE_SYNC               0x02 // Tuner is tuned and synced. This indicates the
                                         //  tuner is tuned to the frequency specified in
                                         //  mpeos_mediaTune() and is able to process the transport
                                         //  stream or analog data. This may be indicated at any
                                         //  time after MPE_TUNE_STARTED (including after
                                         //  MPE_TUNE_UNSYNC).
#define MPE_TUNE_UNSYNC             0x0E // Tuner is tuned and not synced. This indicates the
                                         //  tuner is tuned to the frequency specified in
                                         //  mpeos_mediaTune() but was not able to access/process
                                         //  the transport stream or analog data. Note that this
                                         //  should only be indicated after a reasonable amount of
                                         //  time has passed attempting to achieve synchronization.
                                         //  This may be indicated at any time after
                                         //  MPE_TUNE_STARTED (including after MPE_TUNE_SYNC).
#define MPE_TUNE_FAIL               0x03 // Tuner failed. This indicates the tune failed due to
                                         //  an internal platform failure to acquire/maintain the
                                         //  frequency/modulation specified in mpeos_mediaTune().
                                         //  No events may follow until the next mpeos_mediaTune().
#define MPE_TUNE_ABORT              0x04 // Deprecated. Handled the same as MPE_TUNE_FAIL for
                                         //  backwards compatibility.

//
// Dripfeed related events
//
#define MPE_STILL_FRAME_DECODED		0x07 // A still frame has been decoded
//
// Events which indicate a change to the overall decode request.
//
#define MPE_CONTENT_PRESENTING		0x05 // The requested streams or a subset of those streams are presenting.
                                         // data1 contains the mpe_PresentingReason
#define MPE_CONTENT_NOT_PRESENTING  0x0F // The requested streams or a subset of those streams are not being presented.
                                         // data1 contains the mpe_NotPresentingReason
#define MPE_FAILURE_UNKNOWN			0x06 // The decode request has failed due to an unknown reason.
//
// Shutdown events to be sent to tuner, decoder queues during clean-up.
//
#define	MPE_EVENT_SHUTDOWN			0x08

//
// Events for media change events
//
#define MPE_ACTIVE_FORMAT_CHANGED   0x15
#define MPE_ASPECT_RATIO_CHANGED    0x16
#define MPE_DFC_CHANGED             0x17
#define MPE_3D_FORMAT_CHANGED       0x18 //  data1 contains the mpe_Media3DTransitionType

#define MPE_CCI_UPDATE              0x19 // CCI (Copy Control Information) notification
                                         // (see the OpenCable CableCARD Copy Protection 2.0 Specification for details).
                                         // OptionalEventData1 contains the affected session identifier.
                                         // OptionalEventData3: (msb to lsb) (RESERVED (24 bits) | CCI (8 bits))

/****************************************************************************************
 *
 * MEDIA ERROR CODES
 *
 * The following error codes are used by functions below but are defined in mpe_error.h
 * so they do not need to be mapped here.
 *
 * MPE_EINVAL
 * MPE_ENODATA
 * MPE_ENOMEM
 *
 ***************************************************************************************/

// redefine error codes so they don't overlap with other module's error codes
typedef enum _mpe_MediaErrorCode
{
    MPE_ERROR_MEDIA_BAD_TUNING_REQUEST = OS_MEDIA_ERROR_BADTUNINGREQUEST,
    MPE_ERROR_MEDIA_INVALID_ID = OS_MEDIA_ERROR_INVALIDID,
    MPE_ERROR_MEDIA_INVALID_CHANNEL = OS_MEDIA_ERROR_INVALIDCHANNEL,
    MPE_ERROR_MEDIA_INVALID_SOURCEID = OS_MEDIA_ERROR_INVALIDSOURCEID,
    MPE_ERROR_MEDIA_INVALID_PLAYER = OS_MEDIA_ERROR_INVALIDPLAYER,
    MPE_ERROR_MEDIA_NO_LONGER_AUTHORIZED = OS_MEDIA_ERROR_NOLONGERAUTHORIZED,
    MPE_ERROR_MEDIA_AUTHORIZATION_FAILED = OS_MEDIA_ERROR_AUTHORIZATIONFAILED,
    MPE_ERROR_MEDIA_API_NOT_IMPLEMENTED = OS_MEDIA_ERROR_APINOTIMPLEMENTED,
    MPE_ERROR_MEDIA_API_NOT_SUPPORTED = OS_MEDIA_ERROR_APINOTSUPPORTED,
    MPE_ERROR_MEDIA_OS = OS_MEDIA_ERROR_GENERIC,
    MPE_ERROR_MEDIA_STREAM_OPEN = OS_MEDIA_ERROR_STREAMOPEN,
    MPE_ERROR_MEDIA_STREAM_READ = OS_MEDIA_ERROR_STREAMREAD,
    MPE_ERROR_MEDIA_BUFFER_OVERRUN = OS_MEDIA_ERROR_BUFFEROVERRUN,
    MPE_ERROR_MEDIA_NO_IDS_AVAILABLE = OS_MEDIA_ERROR_NOIDSAVAILABLE,
    MPE_ERROR_MEDIA_PRIORITY_LEVEL = OS_MEDIA_ERROR_PRIORITYLEVEL,
    MPE_ERROR_MEDIA_RESOURCE_NOT_ACTIVE = OS_MEDIA_ERROR_RESOURCENOTACTIVE,
    MPE_ERROR_MEDIA_NOT_OWNER = OS_MEDIA_ERROR_NOTOWNER,
    MPE_ERROR_MEDIA_BAD_LINK = OS_MEDIA_ERROR_BADLINK,
    MPE_ERROR_MEDIA_BLACKED_OUT = OS_MEDIA_ERROR_BLACKEDOUT,
    MPE_ERROR_MEDIA_ECM_STREAM = OS_MEDIA_ERROR_ECMSTREAM,
    MPE_ERROR_MEDIA_NOT_TUNED,
    MPE_ERROR_MEDIA_BAD_PATH,
    MPE_ERROR_MEDIA_RESOURCE_BUSY,
    MPE_ERROR_MEDIA_BAD_QUEUE,
    MPE_ERROR_MEDIA_BAD_DEVICE
} mpe_MediaErrorCode;

/* media stop hold frame modes */
typedef enum
{
    /* Display black frame when playback is stopped */
    MPE_MEDIA_STOP_MODE_BLACK = 0,
    /* Display a still of the last frame displayed when playback is stopped */
    MPE_MEDIA_STOP_MODE_HOLD_FRAME = 1
} mpe_MediaHoldFrameMode;

/* media decode session handle */
typedef struct _mpe_MediaDecodeSessionH
{
    int unused1;
}*mpe_MediaDecodeSession;

typedef enum _mpe_MediaTuneType
{
    MPE_MEDIA_TUNE_BY_TUNING_PARAMS, MPE_MEDIA_TUNE_BY_UNKNOWN
} mpe_MediaTuneType;

typedef enum _mpe_MediaAspectRatio
{
    MPE_ASPECT_RATIO_UNKNOWN = -1,
    MPE_ASPECT_RATIO_4_3 = 2,
    MPE_ASPECT_RATIO_16_9,
    MPE_ASPECT_RATIO_2_21_1
} mpe_MediaAspectRatio;

typedef enum _mpe_MediaActiveFormatDescription
{
    MPE_AFD_NOT_PRESENT = -1,
    MPE_AFD_16_9_TOP = 2,
    MPE_AFD_14_9_TOP,
    MPE_AFD_GT_16_9,
    MPE_AFD_SAME = 8,
    MPE_AFD_4_3,
    MPE_AFD_16_9,
    MPE_AFD_14_9,
    MPE_AFD_4_3_SP_14_9 = 13,
    MPE_AFD_16_9_SP_14_9,
    MPE_AFD_16_9_SP_4_3
} mpe_MediaActiveFormatDescription;

typedef enum _mpe_MediaPositioningCapabilities
{
    MPE_POS_CAP_OTHER = -1,
    MPE_POS_CAP_FULL = 0,
    MPE_POS_CAP_FULL_IF_ENTIRE_VIDEO_ON_SCREEN,
    MPE_POS_CAP_FULL_EVEN_LINES = 3,
    MPE_POS_CAP_FULL_EVEN_LINES_IF_ENTIRE_VIDEO_ON_SCREEN
} mpe_MediaPositioningCapabilities;

/**
 * The possible scan modes for incoming video
 */
typedef enum _mpe_MediaScanMode
{
    // unknown or unspecified line scan mode.
    SCANMODE_UNKNOWN = 0,
    
    // interlaced line scan mode.
    SCANMODE_INTERLACED = 1,

    // progressive line scan mode.
    SCANMODE_PROGRESSIVE = 2

} mpe_MediaScanMode;

/**
 * The format of the 3D description data
 */
typedef enum _mpe_Media3DPayloadType
{
    /**
     * AVC SEI payload type for 3D frame packing arrangement defined in 
     * the OpenCable Content Encoding Profiles specification
     */
    MPE_3D_AVC_SEI_PAYLOAD_TYPE = 1,

    /**
     * MPEG-2 S3D signaling data defined in OpenCable Content
     * Encoding Profiles specification
     */
    MPE_3D_MPEG2_USER_DATA_TYPE = 2
} mpe_Media3DPayloadType;

/**
 * The types of 3D transitions
 */
typedef enum _mpe_Media3DTransitionType
{
    /**
     * 3D formatting data in content transitioned from no 3D
     * formatting data present (i.e., 2D content), to 3D formatting
     * data present in content.
     */
    S3D_TRANSITION_FROM_2D_TO_3D = 1,

    /**
     * 3D formatting data in content transitioned from 3D formatting
     * data present in content to no 3D formatting data present in content
     * (i.e., 2D content).
     */
    S3D_TRANSITION_FROM_3D_TO_2D,

    /**
     * 3D formatting data in content transitioned from one format to another;
     * e.g., Side by Side to Top and Bottom, Top and Bottom to Side by Side.
     */
    S3D_TRANSITION_OF_3D_FORMAT
} mpe_Media3DTransitionType;

typedef enum _mpe_NotPresentingReason
{

    /**
     * Data starvation is preventing presentation
     */
    MPE_NOT_PRESENTING_NO_DATA = 0,

    /**
     * The display device is not capable of presenting 3D formatted content.
     */
    MPE_NOT_PRESENTING_3D_DISPLAY_DEVICE_NOT_CAPABLE = 1,

    /**
     * 3D content is being presented but no 3D-capable display device is connected.
     */
    MPE_NOT_PRESENTING_3D_NO_CONNECTED_DISPLAY_DEVICE
} mpe_NotPresentingReason;

typedef enum _mpe_PresentingReason
{
    /**
     * 2D content is being successfully presented
     */
     MPE_PRESENTING_2D_SUCCESS = 0,

    /**
     * 3D content is being successfully presented
     */
     MPE_PRESENTING_3D_SUCCESS,

    /**
     * 3D content is being presented, but the content is encoded in a format
     * that is not known to be supported by the display device (i.e. it may
     * require user intervention for proper display)
     */
    MPE_PRESENTING_3D_FORMAT_UNCONFIRMED
} mpe_PresentingReason;

// mpe_MediaTuneParams
typedef struct _mpe_MediaTuneParams
{
    mpe_MediaTuneType tuneType;
    uint32_t frequency;
    int32_t programNumber;
    mpe_SiModulationMode qamMode;
} mpe_MediaTuneParams;

// mpe_MediaTuneType
typedef struct _mpe_MediaTuneRequestParams
{
    uint32_t tunerId;
    uint32_t tsId;
    mpe_MediaTuneParams tuneParams;
} mpe_MediaTuneRequestParams;

// mpe_MediaPID
// mpe_SiElemStreamType defined in mpeos_si.h
typedef struct _mpe_MediaPID
{
    mpe_SiElemStreamType pidType; /* PID type: audio, video, etc. */
    uint32_t pid; /* PID value. */
} mpe_MediaPID;

// mpe_MediaDecodeRequestParams
typedef struct _mpe_MediaDecodeRequestParams
{
    uint32_t tunerId;
    mpe_DispDevice videoDevice;
    uint32_t numPids;
    mpe_MediaPID *pids;
    uint32_t pcrPid;
    mpe_Bool blocked;
    mpe_Bool muted;
    float requestedGain;
    float actualGain;
    uint8_t cci; /* The Copy Control Information associated with the content being
                    decoded, per Section 9 of the OpenCable CableCARD Copy
                    Protection 2.0 Specification (CCCP 2.0). Values of CCI indicate
                    whether output-port-specific content protection should be applied. */
    uint8_t ltsid; /* When non-0, identifies the Local Transport Stream ID carrying the content. */
} mpe_MediaDecodeRequestParams;

/*
 *  Parameters for starting a drip feed.  This structure is in place to serve
 *  as a placeholder for future parameters needed to start a drip feed.  Currently,
 *  the only parameter needed is a video device for the drip feed output.  Future
 *  parameters may include the clipping and destination regions for scaling.
 */
typedef struct _mpe_MediaDripFeedRequestParams
{
    mpe_DispDevice videoDevice;
} mpe_MediaDripFeedRequestParams;

// media decode session returned by mpe_mediaDecode and by mpe_mediaDripFeedStart
//typedef struct _mpe_MediaDecodeS { int unused1; } *mpe_MediaDecodeSession;

// Same format as mpe_DispScreenArea which uses normalized screen coordinates
typedef mpe_DispScreenArea mpe_MediaRectangle;

/* Macro used for identifying Video PIDs. */
#define IS_VIDEO_PID(type) 	((type == MPE_SI_ELEM_MPEG_1_VIDEO)	\
							 || (type == MPE_SI_ELEM_MPEG_2_VIDEO)	\
							 || (type == MPE_SI_ELEM_VIDEO_DCII) \
							 || (type == MPE_SI_ELEM_AVC_VIDEO))

/* Macro used for identifying Audio PIDs. */
#define IS_AUDIO_PID(type)	((type == MPE_SI_ELEM_MPEG_1_AUDIO)	\
					  		 || (type == MPE_SI_ELEM_MPEG_2_AUDIO) \
							 || (type == MPE_SI_ELEM_ATSC_AUDIO) \
							 || (type == MPE_SI_ELEM_ENHANCED_ATSC_AUDIO) \
							 || (type == MPE_SI_ELEM_AAC_ADTS_AUDIO) \
							 || (type == MPE_SI_ELEM_AAC_AUDIO_LATM))

// Initialization
mpe_Error mpeos_mediaInit(void);

// Clean-up
mpe_Error mpeos_mediaShutdown(void);

/**
 * Initialize the 3D config info
 */
mpe_Error mpeos_mediaInit3DConfig(void);

/**
 * The <i>mpeos_mediaTune()</i> function shall tune to the given tuning paramaters.
 *
 * @param tuneRequest tune request parameters including tunerId, transport stream Id, frequency, program_number and qam mode
 * @param queueId to post tune related events
 * @param act is a context value for the event dispatcher
 *
 * This is an asynchronous operation and returns MPE_SUCCESS if successful otherwise appropriate error code is returned.
 * If an error is returned no future events will be delivered.
 */
mpe_Error mpeos_mediaTune(mpe_MediaTuneRequestParams *tuneRequest,
        mpe_EventQueue queueId, void *act);

/**
 * The <i>mpeos_mediaDecode()</i> function shall start presenting the media given the pids
 *
 * @param decodeRequest decode request parameters including pids (audio, video) to select and the mpe_DispDevice param that will present the selected media.
 *						Also includes the tuner Id to bind the display device to.
 * @param queueId to post decode related events
 * @param act is a context value for the event dispatcher
 *
 * This is an asynchronous operation and returns MPE_SUCCESS if successful otherwise appropriate error code is returned.
 * If an error is returned no future events will be delivered.
 */
mpe_Error mpeos_mediaDecode(mpe_MediaDecodeRequestParams *decodeRequest,
        mpe_EventQueue queueId, void *act, mpe_MediaDecodeSession *session);

/**
 * The <i>mpeos_mediaStop()</i> function shall stop presenting the media
 *
 * @param session - handle to the media decode session that should be stopped 
 * @param holdFrameMode   controls if the session goes black [default] or freezes last frame displayed
 *
 * Returns MPE_SUCCESS if successful otherwise appropriate error code is returned.
 */
mpe_Error mpeos_mediaStop(mpe_MediaDecodeSession session, uint32_t holdFrameMode);

/**
 * <i>mpeos_mediaFreeze()</i>
 *
 * This function captures a frame and display it on the screen while the media
 * stream is still being decoded.
 *
 * @param videoDevice is the decoder video device from which the frame is captured and displayed.
 *
 * @return <i>MPE_SUCCESS</i>  if the decoder is frozen,
 *          return <i>MPE_ERROR_MEDIA_OS</i> if the OS returns and error.
 */
mpe_Error mpeos_mediaFreeze(mpe_DispDevice videoDevice);

/**
 * <i>mpeos_mediaResume()</i>
 *
 * This function will resume the media decoding
 * (i.e remove the frozen frame from the screen and show the media stream)
 *
 * @param videoDevice is the decoder video device from which we want to resume the video.
 *
 * @return <i>MPE_SUCCESS</i>  if the decoder is resumed,
 *          return <i>MPE_ERROR_MEDIA_OS</i> if the OS returns and error.
 */
mpe_Error mpeos_mediaResume(mpe_DispDevice videoDevice);

/**
 * The <i>mpeos_mediaGetTunerInfo</i> function shall get the current tuning parameters for the target tuner.
 *
 * @param tunerId is the identifier of the target tuner.
 * @param freq is a pointer for returning the currently tuned frequency value.
 * @param mode is a pointer for returning the current modulation mode.
 * @param freq is a pointer for returning the current program number.
 *
 * @return MPE_SUCCESS if the info was returned correctly.
 *
 * NOTE: This will replace mpeos_mediaGetTunerFrequency() after integration.
 */
mpe_Error mpeos_mediaGetTunerInfo(uint32_t tunerId,
        mpe_MediaTuneParams *tuneParams);

/**
 * <i>mpeos_mediaFrequencyToTuner</i>
 *
 * Given a frequency in Hz, return, through a pointer, the ID of the tuner
 * currently tuned to that frequency. If more than one tuner is set to the
 * frequency, the first one found will be selected. If none found, returns
 * MPE_EINVAL and sets tunerId to zero.
 * Synchronous.
 *
 * @param frequency - Frequency to search for among tuners.
 * @param tunerId - Pointer to integer in which to return the discovered tuner ID.
 *
 * return MPE_SUCCESS if a tuner was found that is tuned to the specfied
 *        fequency.
 */
mpe_Error mpeos_mediaFrequencyToTuner(uint32_t frequency, uint32_t *tunerId);

/**
 * The <i>mpeos_mediaSetBounds()</i> function shall set the source and destination bounds for the given display device
 *
 * @param videoDevice input display(video) device
 * @param srcRect source bounds
 * @param destRect destination bounds
 *
 * Returns MPE_SUCCESS if successful otherwise appropriate error code is returned.
 */
mpe_Error mpeos_mediaSetBounds(mpe_DispDevice videoDevice,
        mpe_MediaRectangle *srcRect, mpe_MediaRectangle *destRect);

/**
 * The <i>mpeos_mediaGetBounds()</i> function shall return the source and destination bounds for the given display device
 *
 * @param videoDevice input display(video) device
 * @param srcRect returned source bounds
 * @param destRect returned destination bounds
 *
 * Returns MPE_SUCCESS if successful otherwise appropriate error code is returned.
 */
mpe_Error mpeos_mediaGetBounds(mpe_DispDevice videoDevice,
        mpe_MediaRectangle *srcRect, mpe_MediaRectangle *destRect);

/**
 * The <i>mpeos_mediaCheckBounds()</i> function shall check the desired source and destination bounds for the given display device against platform supported bounds
 *
 * @param videoDevice input display(video) device
 * @param desiredSrc desired source bounds
 * @param desiredDest desired destination bounds
 * @param actualSrc actual source bounds
 * @param actualDest actual destination bounds
 *
 * Returns MPE_SUCCESS if successful otherwise appropriate error code is returned.
 */
mpe_Error mpeos_mediaCheckBounds(mpe_DispDevice videoDevice,
        mpe_MediaRectangle *desiredSrc, mpe_MediaRectangle *desiredDst,
        mpe_MediaRectangle *actualSrc, mpe_MediaRectangle *actualDst);

/**
 * The <i>mpeos_mediaRegisterQueueForTuneEvents()</i> function registers an MPE event
 * queue for reception of all tuner based events.
 *
 * @param queueId is the identifier of the queue to deliver events to.
 *
 * @return MPE_SUCCESS if the queue was successfully registered.
 */
mpe_Error mpeos_mediaRegisterQueueForTuneEvents(mpe_EventQueue queueId);

/**
 * The <i>mpeos_mediaUnregisterQueue()</i> function unregisters an MPE event
 * queue for reception of all tuner based events.
 *
 * @param queueId is the identifier of the previously registered queue.
 *
 * @return MPE_SUCCESS if the queue was successfully unregistered.
 */
mpe_Error mpeos_mediaUnregisterQueue(mpe_EventQueue queueId);

/**
 * Swap the video source streams feeding the two specified decoders.
 *
 * @param decoder1 is the first mpe_DispDevice for the swap
 * @param decoder2 is the second mpe_DispDevice for the swap
 * @param useAudio is a boolean value indicating which audio to
 *        be presented after the swap, if true the audio associated
 *        with the first decoder's stream is presented after the swap.
 *
 * @return mpe_Error if either of the display devices is invalid. Even though
 *         the platform may not support the specified audio configuration no
 *         error should be returned.
 */
mpe_Error mpeos_mediaSwapDecoders(mpe_DispDevice decoder1,
        mpe_DispDevice decoder2, mpe_Bool useAudio);

/**
 * Get the current broadcast aspect ratio for the specified decoder
 *
 * @param decoder is the decoder
 * @param ar is a pointer to an aspect ratio
 *
 * @return Aspect ratio (or unknown if can't determine)
 */
mpe_Error mpeos_mediaGetAspectRatio(mpe_DispDevice decoder,
        mpe_MediaAspectRatio *ar);

/**
 * Get the current Active Format Description for the given decoder
 *
 * @param decoder is the decoder
 * @param afd is a pointer to an AFD variable
 *
 * @return error level
 */
mpe_Error mpeos_mediaGetAFD(mpe_DispDevice decoder,
        mpe_MediaActiveFormatDescription *afd);

/** 
 * Acquire the scaling capabilities of the specified decoder.
 *
 * @param decoder is the target mpe_DispDevice (target decoder)
 * @param positioning is a pointer for returning the positioning capabilities
 * @param horiz is a pointer for returning a pointer to an array of floats
 *        representing either the arbitrary range or discrete horizontal
 *        scaling factors supported (array is terminated with a value of (-1.0))
 * @param vert is a pointer for returning a pointer to an array of floats
 *        representing either the arbitrary range or discrete vertical
 *        scaling factors supported (array is terminated with a value of (-1.0))
 * @param hRange is a pointer for returning a boolean value indicating
 *        whether the horizontal values represent an arbitrary range.
 * @param vRange is a pointer for returning a boolean value indicating
 *        whether the vertical values represent an arbitrary range.
 * @param canClip is a pointer for returning a boolean value indicating
 *        whether the specified decoder can support clipping.
 * @param supportsComponent is a pointer for returning a boolean value indicating
 *        whether the specified decoder supports component based scaling.
 *
 * @return MPE_EINVAL if an invalid display device is specified, or
 *         MPE_ERROR_MEDIA_OS if the target decoder does not support
 *         scaling at all.  
 */
mpe_Error mpeos_mediaGetScaling(mpe_DispDevice decoder,
        mpe_MediaPositioningCapabilities* positioning, float** horiz,
        float** vert, mpe_Bool* hRange, mpe_Bool* vRange, mpe_Bool* canClip,
        mpe_Bool* supportsComponent);

/**
 * Get the dimensions of the input video before scaling.
 * 
 * @param dev the mpe_DispDevice to get the video size from
 * @param dim pointer to a mpe_GfxDimensions that will contain the size of the
 *        video before any scaling has taken place.
 */
mpe_Error mpeos_mediaGetInputVideoSize(mpe_DispDevice dev,
        mpe_GfxDimensions *dim);

/**
 * Initialize a drip feed decode session based on the input parameters.   The decode 
 * session can be used to later submit frames of data for decode and to stop the 
 * decode.   For now, other aspects of the drip feed (such as bounds) can be
 * manipulated using the associated video device with other mpe_media routines.  In 
 * the future, the intent is to have all media routines accept a decode session.
 *
 * @param dripFeedRequest the drip feed request parameters for starting the drip feed
 * @param queueId the event queue to post drip feed events.
 * @param act     (async completion token) the completion token for async events 
 * @param mediaDecodeSession the session filled in by the method to id the drip feed
 *
 * @return 
 *     MPE_EINVAL - an input parameter is invalid (null pointer)
 *     MPE_ERROR_MEDIA_RESOURCE_BUSY - All decode sessions are in use
 *	   MPE_SUCCESS - the function completed successfully
 */
mpe_Error mpeos_mediaDripFeedStart(
        mpe_MediaDripFeedRequestParams *dripFeedRequest,
        mpe_EventQueue queueId, void *act,
        mpe_MediaDecodeSession *mediaDecodeSession);

/**
 * Render a single MPEG-2 Video frame synchronously.  The expected is use is 
 * to receive I or P-frames that do not require future frames for decode.
 *
 * @param mediaDecodeSession the drip feed session created by mpe_mediaDripFeedStart
 * @param buffer the byte array the contains the MPEG-2 I-Frame or P-Frame
 * @param length the number of bytes in the byte array
 *
 * @return
 *     MPE_EINVAL - an input parameter is invalid (null pointer)
 *	MPE_SUCCESS - the function completed successfully
 */
mpe_Error mpeos_mediaDripFeedRenderFrame(
        mpe_MediaDecodeSession mediaDecodeSession, uint8_t *buffer,
        size_t length);

/**
 * Stop a drip feed decode session that was started with mpe_mediaDripFeedStart
 *
 * @param mediaDecodeSession the session filled in by the method to id the drip feed
 *
 * @return 
 *     MPE_EINVAL - an input parameter is invalid (null pointer)
 *	MPE_SUCCESS - the function completed successfully
 */
mpe_Error mpeos_mediaDripFeedStop(mpe_MediaDecodeSession mediaDecodeSession);

/**
 * Get the STC (PCR/300) on a given tuner as decoded from a given PCR PID.
 * It may not be able to get a PID from the tuner in question unless the tuner is
 * actually actively decoding the 
 *
 * @param tuner  The tuner to get the PCR from.
 * @param pcrPID The PID containing the PCR we're interested in.
 * @param out    [out] Pointer to a location where to return the current STC value.
 *
 * @return MPE_EINVAL - a parameter is invalid.
 *         MPE_ENODATA - The tuner is not decoding a program containing the PCR PID in question.
 *         MPE_SUCCESS - the function completed successfully.
 */
mpe_Error mpeos_mediaGetSTC(uint32_t tuner, uint32_t pcrPid, uint64_t *out);

/**
 * Block or unblock the presentation of the desired decode session.  Blocking a decode
 * session is accomplished by muting audio and displaying a black video area.   
 * The decode session continues to process the stream as expected, however, the audio 
 * is not emitted and the video is not displayed.   This method controls the blocking
 * state of the decode session by either blocking or unblocking the audio/video output.
 *
 * @param session - handle to the media decode session to be block / unblocked 
 * @param block - boolean indicating whether to block (TRUE) or unblock (FALSE)
 *
 * @return MPE_EINVAL - the session parameter is null or invalid.
 *         MPE_SUCCESS - the function completed successfully.
 */
mpe_Error mpeos_mediaBlockPresentation(mpe_MediaDecodeSession session,
        mpe_Bool block);

/**
 * <i>mpe_mediaChangePids()</i> 
 * 
 * This function will change the decoded PIDs asynchronously, on a 
 * given decode session.
 * The following event is sent to the caller's queue to indicate success
 * MPE_CONTENT_PRESENTING
 * And a failure to decode is indicated by one of the following events:
 * MPE_FAILURE_CA_DENIED     
 * MPE_FAILURE_UNKNOWN
 *
 * @param session     is the decode session to be updated
 * @param numPids     is the number of Pids in the array.
 * @param pids        is the array of new pids to decode
 * @param pcrPid      is the new PCR pid to decode.
 */
mpe_Error mpeos_mediaChangePids(mpe_MediaDecodeSession session,
        uint32_t numPids, mpe_MediaPID *pids, uint32_t pcrPid);

/**
 * <i>mpeos_mediaSetMute()</i>
 *
 * Set the current mute state. Audio must be muted if mute is TRUE
 * regardless of content format and the gain value must be unaffected.
 * When FALSE, audio must be restored and gain must be set to the
 * pre-mute level.
 *
 * @param session the handle of the decode session to set the mute setting.
 * @param mute     an mpe_Bool representing the new mute setting
 * @return MPE_EINVAL if any parameter is NULL.
 * @return MPE_SUCCESS if successful.
 */
mpe_Error mpeos_mediaSetMute(mpe_MediaDecodeSession session, mpe_Bool mute);

/**
 * <i>mpeos_mediaSetGain()</i>
 *
 * Set the current decibel gain for the supplied playback session.
 * Positive values amplify the audio signal and negative values
 * attenuate the signal.
 *
 * If the platform does not support the setting of the gain level for the
 * presenting content format, actualGain should be returned as 0.0.
 *
 * @param session the handle of the decode session to set the gain
 * @param gain     a float representing the new gain. in decibels
 * @param actualGain pointer to a float which will be assigned the new gain setting
 * @return MPE_EINVAL if any parameter is NULL.
 * @return MPE_SUCCESS if successful.
 */
mpe_Error mpeos_mediaSetGain(mpe_MediaDecodeSession session, float gain, float *actualGain);

/*
 * Description:
 *
 *   Set the Copy Control Information (CCI) associated with the given playback.
 *   This over-rides the previous CCI associated with this playback session and may
 *   result in a change in the copy protection applied to the content when output to
 *   one or more ports (e.g. analog protection or DTCP encryption).
 *
 * @param playback - handle to the playback session
 * @param cci - The Copy Control Information associated with the content being
 *              played back, per Section 9 of the OpenCable CableCARD Copy
 *              Protection 2.0 Specification (CCCP 2.0). Values of CCI indicate
 *              whether output-port-specific content protection should be applied.
 *
 * @return MPE_EINVAL if the playback parameter is null or invalid.
 *         MPE_SUCCESS if the function completed successfully.
 */
mpe_Error mpeos_mediaSetCCI(mpe_MediaDecodeSession session, uint8_t cci);


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
mpe_Error mpeos_mediaGetInputVideoScanMode (mpe_MediaDecodeSession session, 
        mpe_MediaScanMode* scanMode);


/**
 * Get the current 3DTV configuration.
 *
 * @param session is the active decode session to query for the 3D
 *        configuration
 * @param stereoscopicMode pointer to the field to be set with the current
 *        mpe_DispStereoscopicMode
 * @param payloadType pointer to the field to be set with the current
 *        mpe_Media3DPayloadType
 * @param payload pointer to the field to be set with the current payload of
 *        the 3DTV information description message.  The payload memory is
 *        allocated by the caller and its size is passed in via payloadSz.
 *        The mpeos layer then copies the actual payload size into payloadSz.
 *        If the payload mem block is too small to contain the payload,
 *        MPE_ENOMEM is returned and the necessary storage size is returned
 *        via payloadSz.
 * @param payloadSz as an input, payloadSz is the size in bytes of the memory
 *        allocated for the payload. As an output, payloadSz contains the
 *        size of the actual number of bytes copied into the payload
 *        mem block.  If the payload mem block is too small to contain the
 *        payload, MPE_ENOMEM is returned and the necessary storage size is
 *        returned via payloadSz.
 *
 * @return MPE_EINVAL if session, formatType, payloadType, or paloadSz NULL.
 * @return MPE_ENOMEM if *payloadSz is too small to accommodate the 3DTV
 *                    information description message.
 * @return MPE_SUCCESS if successful.
 */
mpe_Error mpeos_mediaGet3DConfig( mpe_MediaDecodeSession session,
                                  mpe_DispStereoscopicMode* stereoscopicMode,
                                  mpe_Media3DPayloadType * payloadType,
                                  uint8_t * payload,
                                  uint32_t * payloadSz );

#ifdef __cplusplus
}
#endif

#endif  /* _MPEOS_MEDIA_H */
