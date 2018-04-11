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

/**
 This module is the specific implementation of MPE OS Media API for the
 CableLabs Reference Implementation.
 It includes tuner control methods, MPEG decoder and audio player(only AIFF)
 control methods. Also included are methods to set/get video destination/source
 boundaries, methods to set/get volume control etc.
 */

/* Header Files */
#include "mpe_types.h"
#include "mpe_error.h"
#include "mpe_os.h"

#include "mpeos_media.h"
#include "mpeos_dbg.h"
#include "mpeos_event.h"
#include "mpeos_sync.h"
#include "mpeos_util.h"

#include "platform.h"
#include "platform_filter.h"

#include <ri_pipeline_manager.h>
#include <ri_pipeline.h>

#include <stdlib.h>
#include <string.h>
#include <ri_test_interface.h>
#include "test_3dtv.h"

#define MPE_MEM_DEFAULT MPE_MEM_MEDIA

// Enumeration of valid tuner states
typedef enum _MediaTunerState
{
    MEDIA_TUNER_STATE_IDLE,
    MEDIA_TUNER_STATE_BUSY,
    MEDIA_TUNER_STATE_TUNED_SYNC,
    MEDIA_TUNER_STATE_TUNED_NOSYNC
} MediaTunerState;

// Structure to maintain data for each tuner.
typedef struct
{
    /** This tuner's ID */
    uint32_t tunerId;

    /** This tuner's state. */
    MediaTunerState tunerState;

    /** The state of the decoder associated with this tuner */
    //MediaDecoderState dispDeviceState;

    /** Tuning info for the current tune */
    mpe_MediaTuneParams* currentTuneRequest;
    mpe_EventQueue currentQueueID;
    void* currentAct;

    /** Mutex for tuning this tuner. */
    mpe_Mutex mutex;

    /** Pointer to the RI Platform tuner object */
    ri_tuner_t* riTuner;
} TunerData;

/** The tuner data array. */
static TunerData* g_tuners = NULL;
static uint32_t g_numTuners;

/* LAST TUNED TUNER DATA - FOR Telnet Interface TESTING ONLY */
static TunerData* g_lastTunedTunerData = NULL;

/* List of tune event listeners */
static GSList* g_registeredTuneEventListeners = NULL;
static mpe_Mutex g_tuneEventListenerMutex;

static mpe_Bool g_mediaInitialized = FALSE;

// Fake implementation of 3DTV - default to 2D
static mpe_Media3DPayloadType g_payloadType = 0;
static mpe_DispStereoscopicMode g_stereoscopicMode = 0;
static uint8_t *g_payload = NULL;
static uint32_t g_payloadSz = 0;


// Fake implementation of input video scan mode
static mpe_MediaScanMode g_videoScanMode = SCANMODE_UNKNOWN;


/* Used for notifying tune event listeners */
typedef struct
{
    mpe_Event event;
    uint32_t tunerID;
} TuneEvent;

typedef enum _MediaDecoderState
{
    MEDIA_DECODER_STATE_UNKNOWN,
    MEDIA_DECODER_STATE_IDLE,
    MEDIA_DECODER_STATE_BUSY,
    MEDIA_DECODER_STATE_PLAYING,
    MEDIA_DECODER_STATE_PAUSED
} MediaDecoderState;

//static uint32_t g_decoderCount = 2;
//static uint32_t g_decoderIds[] = {MPE_DISPLAY_DEST_TV, MPE_DISPLAY_DEST_VIDEO};

/* -------------------- Media Decode Session Definitions ------------------- */
#define MEDIA_DECODE_UNAVAILABLE ((uint32_t)-1)  // denotes all decode sessions are taken
/**
 *   Enumeration of all media decode session types used to identify contents of
 *   an mpeos_MediaDecodeSession structure
 */
typedef enum
{
    MEDIA_DECODE_SESSION_UNDEFINED = 0,
    MEDIA_DECODE_SESSION_BROADCAST,
    MEDIA_DECODE_SESSION_DRIPFEED,
    MEDIA_DECODE_SESSION_DVR
} MediaDecodeSessionType;

/**
 * A decode session structure.
 */
typedef struct
{
    MediaDecodeSessionType m_type; // BROADCAST, DRIPFEED, or DVR
    mpe_DispDevice m_dispDevice; /** The decoder handling this. */
    mpe_EventQueue m_dispDeviceQueueID; /** The decode request response queue */
    void *m_dispDeviceAct; /** The decode request response queue data */
    MediaDecoderState m_dispDeviceState;
    union
    {
        TunerData *broadcastTuner; // Ptr to broadcast specific info (tuner struct)
        // no additional data needed for drip feed, thus no drip feed struct
        // mpeos_DvrPlayback    *dvrPlayback; // TODO: to be implemented
    } m_typeData;
} mpeos_MediaDecodeSession;

// TODO: Make this dynamic and read from environment
#define MPE_MAX_DECODE_SESSIONS (2)

// Array of decode sessions (used by broadcast, dripfeed, dvr)
static mpeos_MediaDecodeSession g_decodeSessions[MPE_MAX_DECODE_SESSIONS];
static mpe_Mutex g_decodeSessionsMutex; // mutex to protect global array access

/**
 * Returns the associated session or NULL.
 * @param tuner A tuner.
 */
static mpeos_MediaDecodeSession* tunerToSessionID(TunerData* tuner);

/**
 * Creates a media decode session. Tuning must occur first.
 *
 * @param videoDevice the video device on which to display the decoded output
 * @param eventQueue the event queue to post drip feed events.
 * @param act     (async completion token) the completion token for async events
 * @param type the decode session type being created
 * @param mediaDecodeSession the session filled in by the method
 */
mpe_Error createBroadcastDecodeSession(
        mpe_MediaDecodeRequestParams *decodeParams, TunerData *tuner,
        mpe_EventQueue queueId, void *act, MediaDecodeSessionType type,
        mpeos_MediaDecodeSession **session);

/**
 * Creates a drip feed session.
 *
 * @param dripFeedParams the drip feed params struct
 * @param eventQueue the event queue to post drip feed events.
 * @param act     (async completion token) the completion token for async events
 * @param type the decode session type being created
 * @param mediaDecodeSession the session filled in by the method
 */
mpe_Error createDripFeedSession(mpe_MediaDripFeedRequestParams *dripFeedParams,
        mpe_EventQueue queueId, void *act, MediaDecodeSessionType type,
        mpeos_MediaDecodeSession **mds);

/**
 * <i>mpeos_mediaDripFeedStop()</i>
 *
 *  Stop the drip feed
 *
 *
 * @param videoDevice display device handle
 *
 * @return <i>MPE_SUCCESS</i>  if successfully stopped the decoder,
 *          return <i>MPE_ERROR_MEDIA_OS</i> if the OS throws an exception
 *
 */

mpe_Error mpeos_mediaDripFeedStop(mpe_MediaDecodeSession mediaDecodeSession);

/**
 *  Return an available session slot or error if none available.
 */
static uint32_t getDecodeSessionEntry(void);
static ri_pipeline_t* getPipeline(uint32_t tunerId);
static ri_video_device_t* getVideoDevice(
        mpe_MediaDecodeRequestParams *decodeRequest);
static void os_mediaDeleteDecodeSession(mpe_MediaDecodeSession session,
        mpe_Bool notify);


void os_mediaNotifySyncStateChange(gboolean synced);
void os_mediaNotifyTuneAbort(void);

static mpe_Bool s_UseTelnetInterfaceSyncNotification = FALSE;
static mpe_Bool s_TelnetInterfaceForceTuneFail = FALSE;

// For RI test interface (telnet interface)
#define MEDIA_TESTS \
    "\r\n" \
    "|---+-----------------------\r\n" \
    "| e | Enable manual sync control\r\n" \
    "|---+-----------------------\r\n" \
    "| d | Disable manual sync control\r\n" \
    "|---+-----------------------\r\n" \
    "| s | send SYNC\r\n" \
    "|---+-----------------------\r\n" \
    "| u | send UNSYNC\r\n"\
    "|---+-----------------------\r\n" \
    "| a | send ABORT\r\n"\
    "|---+-----------------------\r\n" \
    "| f | make tunes FAIL/not FAIL\r\n"


static int mediaMenuInputHandler(int sock, char *rxBuf, int *retCode, char **retStr)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA, "%s(%d, %s);\n",
              __FUNCTION__, sock, rxBuf);
    *retCode = MENU_SUCCESS;

    if (strstr(rxBuf, "x"))
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA, "%s - Exit -1\n", __FUNCTION__);
        return -1;
    }

    switch (rxBuf[0])
    {
        case 'd':
        {
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
                      "%s - disabled manual sync control\n", __FUNCTION__);
            s_UseTelnetInterfaceSyncNotification = FALSE;
            break;
        }
        case 'e':
        {
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
                      "%s - enabled manual sync control\n", __FUNCTION__);
            s_UseTelnetInterfaceSyncNotification = TRUE;
            break;
        }
        case 's':
        {
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
                      "%s - send SYNC\n", __FUNCTION__);
            if (g_lastTunedTunerData != NULL)
            {
                os_mediaNotifySyncStateChange(TRUE);
            }
            else
            {
                ri_test_SendString(sock, "lastTunedTunerData is NULL!");
                *retCode = MENU_FAILURE;
                char * strTemp = "lastTunedTunerData is NULL!";
                *retStr = g_strdup (strTemp);   
            }
            
            break;
        }
        case 'u':
        {
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
                      "%s - send UNSYNC\n", __FUNCTION__);
            if (g_lastTunedTunerData != NULL)
            {
                os_mediaNotifySyncStateChange(FALSE);
            }
            else
            {
                ri_test_SendString(sock, "lastTunedTunerData is NULL!");
                *retCode = MENU_FAILURE;
                char * strTemp = "lastTunedTunerData is NULL!";
                *retStr = g_strdup (strTemp);   
            }
            break;
        }
        case 'a':
        {
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
                      "%s - send ABORT\n", __FUNCTION__);
            os_mediaNotifyTuneAbort();
            break;
        }
        case 'f':
        {
            s_TelnetInterfaceForceTuneFail = !s_TelnetInterfaceForceTuneFail;
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
                      "%s - tunes will now %s\n", __FUNCTION__,
                      (s_TelnetInterfaceForceTuneFail ? "FAIL" : "NOT FAIL") );
            break;
        }
        default:
        {
            strcat(rxBuf, " - unrecognized\r\n\n");
            ri_test_SendString(sock, rxBuf);
            *retCode = MENU_INVALID;
        }
            break;
    } // END switch (rxBuf[0])

    return 0;
} // END medaiMenuInputHandler()

static MenuItem MpeosMediaMenuItem =
{ false, "n", "Tuning", MEDIA_TESTS, mediaMenuInputHandler };

/**
 * Convert RI Platform modulation mode to MPE
 */
mpe_SiModulationMode modmodeRItoMPE(const ri_tuner_modulation_mode mode)
{
    switch (mode)
    {
    case RI_MODULATION_UNKNOWN:
        return MPE_SI_MODULATION_UNKNOWN;
    case RI_MODULATION_QPSK:
        return MPE_SI_MODULATION_QPSK;
    case RI_MODULATION_BPSK:
        return MPE_SI_MODULATION_BPSK;
    case RI_MODULATION_OQPSK:
        return MPE_SI_MODULATION_OQPSK;
    case RI_MODULATION_VSB8:
        return MPE_SI_MODULATION_VSB8;
    case RI_MODULATION_VSB16:
        return MPE_SI_MODULATION_VSB16;
    case RI_MODULATION_QAM16:
        return MPE_SI_MODULATION_QAM16;
    case RI_MODULATION_QAM32:
        return MPE_SI_MODULATION_QAM32;
    case RI_MODULATION_QAM64:
        return MPE_SI_MODULATION_QAM64;
    case RI_MODULATION_QAM80:
        return MPE_SI_MODULATION_QAM80;
    case RI_MODULATION_QAM96:
        return MPE_SI_MODULATION_QAM96;
    case RI_MODULATION_QAM112:
        return MPE_SI_MODULATION_QAM112;
    case RI_MODULATION_QAM128:
        return MPE_SI_MODULATION_QAM128;
    case RI_MODULATION_QAM160:
        return MPE_SI_MODULATION_QAM160;
    case RI_MODULATION_QAM192:
        return MPE_SI_MODULATION_QAM192;
    case RI_MODULATION_QAM224:
        return MPE_SI_MODULATION_QAM224;
    case RI_MODULATION_QAM256:
        return MPE_SI_MODULATION_QAM256;
    case RI_MODULATION_QAM320:
        return MPE_SI_MODULATION_QAM320;
    case RI_MODULATION_QAM384:
        return MPE_SI_MODULATION_QAM384;
    case RI_MODULATION_QAM448:
        return MPE_SI_MODULATION_QAM448;
    case RI_MODULATION_QAM512:
        return MPE_SI_MODULATION_QAM512;
    case RI_MODULATION_QAM640:
        return MPE_SI_MODULATION_QAM640;
    case RI_MODULATION_QAM768:
        return MPE_SI_MODULATION_QAM768;
    case RI_MODULATION_QAM896:
        return MPE_SI_MODULATION_QAM896;
    case RI_MODULATION_QAM1024:
        return MPE_SI_MODULATION_QAM1024;
    case RI_MODULATION_QAM_NTSC:
        return MPE_SI_MODULATION_QAM_NTSC;
    }

    return MPE_SI_MODULATION_UNKNOWN;
}

static mpeos_MediaDecodeSession *getCurrentSession()
{
    int i;
    for (i = 0; i < MPE_MAX_DECODE_SESSIONS; i++)
    {
        if (g_decodeSessions[i].m_type != MEDIA_DECODE_SESSION_UNDEFINED)
        {
            return &g_decodeSessions[i];
        }
    }
    return NULL;
}

static int testInputHandler(int sock, char *rxBuf, int *retCode, char **retStr)
{
    int ret = 0;

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DISP, "%s(%d, %s);\n",
              __FUNCTION__, sock, rxBuf);
    *retCode = MENU_SUCCESS;

    if (strstr(rxBuf, "x"))
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA, "%s - Exit -1\n", __FUNCTION__);
        return -1;
    }

    mpeos_MediaDecodeSession *session = getCurrentSession();
    if (session == NULL)
    {
        ri_test_SendString(sock, "\r\n\nNO CURRENT MEDIA DECODE SESSION -- EVENTS WILL NOT BE SENT!\r\n");
    }

#ifdef __linux__
    mpe_EventQueue queue = 0;
#else
    mpe_EventQueue queue = NULL;
#endif
    void* act = NULL;
    if (session != NULL)
    {
        queue = session->m_dispDeviceQueueID;
        act = session->m_dispDeviceAct;
    }

    ret = test3DTVInputHandler(sock, rxBuf, queue, act,
            &g_payloadType, &g_stereoscopicMode, &g_payload, &g_payloadSz,
            &g_videoScanMode);

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA, "%s - Exit %d\n", __FUNCTION__, ret);
    return ret;
}

static MenuItem MpeosMenuItem =
{ false, "m", "Media", MPEOS_3DTV_TESTS, testInputHandler };

/**
 * Convert MPE modulation mode to RI Platform
 */
ri_tuner_modulation_mode modmodeMPEtoRI(const mpe_SiModulationMode mode)
{
    switch (mode)
    {
    case MPE_SI_MODULATION_UNKNOWN:
        return RI_MODULATION_UNKNOWN;
    case MPE_SI_MODULATION_QPSK:
        return RI_MODULATION_QPSK;
    case MPE_SI_MODULATION_BPSK:
        return RI_MODULATION_BPSK;
    case MPE_SI_MODULATION_OQPSK:
        return RI_MODULATION_OQPSK;
    case MPE_SI_MODULATION_VSB8:
        return RI_MODULATION_VSB8;
    case MPE_SI_MODULATION_VSB16:
        return RI_MODULATION_VSB16;
    case MPE_SI_MODULATION_QAM16:
        return RI_MODULATION_QAM16;
    case MPE_SI_MODULATION_QAM32:
        return RI_MODULATION_QAM32;
    case MPE_SI_MODULATION_QAM64:
        return RI_MODULATION_QAM64;
    case MPE_SI_MODULATION_QAM80:
        return RI_MODULATION_QAM80;
    case MPE_SI_MODULATION_QAM96:
        return RI_MODULATION_QAM96;
    case MPE_SI_MODULATION_QAM112:
        return RI_MODULATION_QAM112;
    case MPE_SI_MODULATION_QAM128:
        return RI_MODULATION_QAM128;
    case MPE_SI_MODULATION_QAM160:
        return RI_MODULATION_QAM160;
    case MPE_SI_MODULATION_QAM192:
        return RI_MODULATION_QAM192;
    case MPE_SI_MODULATION_QAM224:
        return RI_MODULATION_QAM224;
    case MPE_SI_MODULATION_QAM256:
        return RI_MODULATION_QAM256;
    case MPE_SI_MODULATION_QAM320:
        return RI_MODULATION_QAM320;
    case MPE_SI_MODULATION_QAM384:
        return RI_MODULATION_QAM384;
    case MPE_SI_MODULATION_QAM448:
        return RI_MODULATION_QAM448;
    case MPE_SI_MODULATION_QAM512:
        return RI_MODULATION_QAM512;
    case MPE_SI_MODULATION_QAM640:
        return RI_MODULATION_QAM640;
    case MPE_SI_MODULATION_QAM768:
        return RI_MODULATION_QAM768;
    case MPE_SI_MODULATION_QAM896:
        return RI_MODULATION_QAM896;
    case MPE_SI_MODULATION_QAM1024:
        return RI_MODULATION_QAM1024;
    case MPE_SI_MODULATION_QAM_NTSC:
        return RI_MODULATION_QAM_NTSC;
    }

    return RI_MODULATION_UNKNOWN;
}

// Called by the GSList foreach to notify a single listener
static void notifyTuneEvent(gpointer data, TuneEvent* tuneEvent)
{
    mpe_EventQueue queue = (mpe_EventQueue) data;

    // Send the event.
    if (mpeos_eventQueueSend(queue, tuneEvent->event,
            (void*) tuneEvent->tunerID, NULL, 0) != MPE_SUCCESS)
    {
        // Report any error to the log.
        MPEOS_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_MEDIA,
                "NotifyRegisteredTuneEventQueues() - error sending tune event %d to queue 0x%x for tunerID %d\n",
                tuneEvent->event, queue, tuneEvent->tunerID);
    }
}

// Called when we need to notify all registered tune event listeners of a new
// tuning event
static void notifyTuneEventListeners(mpe_Event event, uint32_t tunerID)
{
    TuneEvent tuneEvent;
    tuneEvent.event = event;
    tuneEvent.tunerID = tunerID;

    mpeos_mutexAcquire(g_tuneEventListenerMutex);

    // Just run thru the list and send events.
    g_slist_foreach(g_registeredTuneEventListeners, (GFunc) notifyTuneEvent,
            &tuneEvent);

    mpeos_mutexRelease(g_tuneEventListenerMutex);
}

/**
 * Finalized the current tune request in the given tuner.  This will send
 * the SHUTDOWN event to its queue and free its request memory.  The
 * mutex for this tuner data should be held when calling this function.
 */
static void finalizeCurrentTuneRequest(TunerData* tunerData)
{
    if (tunerData->currentTuneRequest != NULL)
    {
        // Send shutdown event to the queue
        mpeos_eventQueueSend(tunerData->currentQueueID, MPE_EVENT_SHUTDOWN,
                NULL, (void*) tunerData->currentAct, 0);

        // Free resources and reset variables
        mpe_memFree(tunerData->currentTuneRequest);
        tunerData->currentTuneRequest = NULL;
        tunerData->currentAct = NULL;
        tunerData->tunerState = MEDIA_TUNER_STATE_IDLE;
    }
}

/**
 * Performs a tune request with the given parameters.  If the tune request is
 * successfully processed by the platform, the given request, queue, and ACT
 * will be assigned to the given TunerData as the currentTuneRequest.
 */
static void doPlatformTune(TunerData* tunerData,
        mpe_MediaTuneParams* tuneRequest, mpe_EventQueue queue, void* act)
{
    // ASSERT:  The caller holds the mutex for the given TunerData
    ri_error rierror;
    ri_tune_params_t tuneParams;
    mpe_Bool tuneFailed = FALSE;

    // Create the RI tuner tuning params from the MPE tune request
    tuneParams.frequency = tuneRequest->frequency;
    tuneParams.mode = modmodeMPEtoRI(tuneRequest->qamMode);
    tuneParams.program_num = tuneRequest->programNumber;

    // This is now our current tune request
    tunerData->currentTuneRequest = tuneRequest;
    tunerData->currentQueueID = queue;
    tunerData->currentAct = act;

    // Alert the section filtering system that a tune is about to start.
    // This allows any previous filters to be shutdown correctly prior to
    // new data flow from the upcoming tune
    sf_tuneInitiated(tunerData->tunerId);

    // Perform tune

    if (s_TelnetInterfaceForceTuneFail)
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
          "FAILING tune - under Telnet Interface control!\n");
        // Faking a failed tune (for testing)...
        notifyTuneEventListeners(MPE_TUNE_STARTED, tunerData->tunerId);
        mpeos_eventQueueSend(tunerData->currentQueueID, MPE_TUNE_STARTED, NULL,
                (void*) tunerData->currentAct, 0);

        tuneFailed = TRUE;
    }
    else
    {
        rierror = tunerData->riTuner->request_tune(tunerData->riTuner, tuneParams);

        switch (rierror)
        {
            case RI_ERROR_NONE:
            {
                tunerData->tunerState = MEDIA_TUNER_STATE_BUSY;

                MPEOS_LOG(
                        MPE_LOG_INFO,
                        MPE_MOD_MEDIA,
                        "Tune request returned success! Freq=%d, Mode=%d, ProgNum=%d.  Tune started!\n",
                        tuneParams.frequency, tuneParams.mode, tuneParams.program_num);
                break;
            }
            case RI_ERROR_INVALID_TUNE_REQUEST:
            {
                MPEOS_LOG(
                        MPE_LOG_INFO,
                        MPE_MOD_MEDIA,
                        "Platform considered tune request invalid! Freq=%d, Mode=%d, ProgNum=%ds (issuing MPE_TUNE_UNSYNC)\n",
                        tuneParams.frequency, tuneParams.mode, tuneParams.program_num);
                // To better emulate how a real STB should handle this, consider a "bad"
                //  frequency/qam condition to be transient/recoverable (since on a real
                //  network, conditions could change). (note: This logic could be performed in
                //  the platform instead of here...)

                mpeos_eventQueueSend(tunerData->currentQueueID, MPE_TUNE_STARTED, NULL,
                                     (void*) tunerData->currentAct, 0);
                notifyTuneEventListeners(MPE_TUNE_STARTED, tunerData->tunerId);

                mpeos_eventQueueSend(tunerData->currentQueueID, MPE_TUNE_UNSYNC, NULL,
                                     (void*) tunerData->currentAct, 0);
                notifyTuneEventListeners(MPE_TUNE_UNSYNC, tunerData->tunerId);

                tunerData->tunerState = MEDIA_TUNER_STATE_TUNED_NOSYNC;
                // We'll never get out of this state since the platform didn't even accept
                //  this as a legitimate tune
                break;
            }
            default:
            {
                MPEOS_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_MEDIA,
                        "Tune request returned error! Freq=%d, Mode=%d, ProgNum=%d.  Error = %d\n",
                        tuneParams.frequency, tuneParams.mode, tuneParams.program_num,
                        rierror);
                tuneFailed = TRUE;
                break;
            }
        }
    }

    if (tuneFailed)
    {
        // Done before we started...
        mpeos_eventQueueSend(tunerData->currentQueueID, MPE_TUNE_STARTED, NULL,
                             (void*) tunerData->currentAct, 0);
        notifyTuneEventListeners(MPE_TUNE_STARTED, tunerData->tunerId);

        // Set our tuner state to idle, notify listeners, and cleanup
        // the current tune request
        tunerData->tunerState = MEDIA_TUNER_STATE_IDLE;
        mpeos_eventQueueSend(tunerData->currentQueueID, MPE_TUNE_FAIL,
                NULL, (void*) tunerData->currentAct, 0);
        notifyTuneEventListeners(MPE_TUNE_FAIL, tunerData->tunerId);
        finalizeCurrentTuneRequest(tunerData);
    }
}

/**
 *
 */
static void tunerEventCallback(ri_tuner_event event, void* cb_data)
{
    uint32_t tunerIndex = (uint32_t)cb_data;
    TunerData* tunerData;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_MEDIA, "TunerCallback(event %d, tuner %d) \n",
                                            event, tunerIndex );

    if (tunerIndex >= g_numTuners)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "TunerCallback() -- Invalid tuner index %d\n", tunerIndex);
        return;
    }

    tunerData = &g_tuners[tunerIndex];

    // Lock the tuner.
    mpeos_mutexAcquire(tunerData->mutex);

    if (tunerData->currentTuneRequest == NULL)
    {
        // This shouldn't happen...
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_MEDIA, "TunerCallback: Ignoring callback "
                                                "(current tune request on tuner %d is NULL) \n",
                                                tunerIndex );
    }
    else
    {
        if (tunerData->tunerState == MEDIA_TUNER_STATE_BUSY)
        { // The RI platform doesn't really have an async indication to signal when
          //  tuning has started. So consider the first event received after a tune
          //  to be the "start" of the tune... (so the RI Platform never has more than
          //  one in-flight tune on a tuner)
            notifyTuneEventListeners(MPE_TUNE_STARTED, tunerData->tunerId);
            mpeos_eventQueueSend(tunerData->currentQueueID, MPE_TUNE_STARTED, NULL,
                    (void*) tunerData->currentAct, 0);
        }

        // Calculate our new tuner state and notify its event queue
        switch (event)
        {
        case RI_TUNER_EVENT_FAIL: // Failed to set the tuner to the requested parameters
            switch (tunerData->tunerState)
            {
            case MEDIA_TUNER_STATE_BUSY: // Failed tune
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                        "tunerEventCallback -- Failed tune!\n");
                // Set our tuner state to idle, notify listeners, and cleanup
                // the current tune request
                tunerData->tunerState = MEDIA_TUNER_STATE_IDLE;
                mpeos_eventQueueSend(tunerData->currentQueueID, MPE_TUNE_FAIL,
                        NULL, (void*) tunerData->currentAct, 0);
                notifyTuneEventListeners(MPE_TUNE_FAIL, tunerData->tunerId);
                finalizeCurrentTuneRequest(tunerData);
                break;

            case MEDIA_TUNER_STATE_TUNED_NOSYNC:
            case MEDIA_TUNER_STATE_TUNED_SYNC:
            case MEDIA_TUNER_STATE_IDLE:
                MPEOS_LOG(
                        MPE_LOG_DEBUG,
                        MPE_MOD_MEDIA,
                        "tunerEventCallback -- Only expect to get tuner FAIL event when in BUSY state!\n");
                break;
            }
            break;

        case RI_TUNER_EVENT_SYNC: // Tuned and locked
            switch (tunerData->tunerState)
            {
            case MEDIA_TUNER_STATE_BUSY: // sync notification when busy..unexpected
                if (s_UseTelnetInterfaceSyncNotification)
                {
                    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
                      "SYNC state ignored - under Telnet Interface control!\n");
                    break;
                }

                MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
                        "tunerEventCallback -- Got SYNC state!\n");
                tunerData->tunerState = MEDIA_TUNER_STATE_TUNED_SYNC;
                mpeos_eventQueueSend(tunerData->currentQueueID, MPE_TUNE_SYNC,
                        NULL, (void*) tunerData->currentAct, 0);
                notifyTuneEventListeners(MPE_TUNE_SYNC, tunerData->tunerId);
                break;

            case MEDIA_TUNER_STATE_TUNED_NOSYNC:
                if (s_UseTelnetInterfaceSyncNotification)
                {
                    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
                      "SYNC state ignored - under Telnet Interface control!\n");
                    break;
                }

                MPEOS_LOG(
                        MPE_LOG_INFO,
                        MPE_MOD_MEDIA,
                        "tunerEventCallback -- Successful tune!  Transition from NOSYNC to SYNC state!\n");
                tunerData->tunerState = MEDIA_TUNER_STATE_TUNED_SYNC;
                mpeos_eventQueueSend(tunerData->currentQueueID, MPE_TUNE_SYNC,
                        NULL, (void*) tunerData->currentAct, 0);
                notifyTuneEventListeners(MPE_TUNE_SYNC, tunerData->tunerId);
                break;

            case MEDIA_TUNER_STATE_TUNED_SYNC:
                MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_MEDIA,
                        "tunerEventCallback -- got redundant SYNC state event\n");
                break;

            case MEDIA_TUNER_STATE_IDLE:
                MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_MEDIA,
                        "tunerEventCallback -- got SYNC state event when IDLE. Ignoring.\n");
                break;
            }
            break;

        case RI_TUNER_EVENT_NOSYNC: // Tuned and not locked
            switch (tunerData->tunerState)
            {
            case MEDIA_TUNER_STATE_BUSY: // Successful tune, but not locked
                if (s_UseTelnetInterfaceSyncNotification)
                {
                    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
                    "NOSYNC state ignored - under Telnet Interface control!\n");
                    break;
                }

                MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
                        "tunerEventCallback -- Got NOSYNC state!\n");
                tunerData->tunerState = MEDIA_TUNER_STATE_TUNED_NOSYNC;
                mpeos_eventQueueSend(tunerData->currentQueueID,
                        MPE_TUNE_UNSYNC, NULL, (void*) tunerData->currentAct, 0);
                notifyTuneEventListeners(MPE_TUNE_UNSYNC, tunerData->tunerId);
                break;

            case MEDIA_TUNER_STATE_TUNED_SYNC:
                if (s_UseTelnetInterfaceSyncNotification)
                {
                    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
                    "NOSYNC state ignored - under Telnet Interface control!\n");
                    break;
                }

                MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
                        "tunerEventCallback -- Transition from SYNC to NOSYNC state!\n");
                tunerData->tunerState = MEDIA_TUNER_STATE_TUNED_NOSYNC;
                mpeos_eventQueueSend(tunerData->currentQueueID,
                        MPE_TUNE_UNSYNC, NULL, (void*) tunerData->currentAct, 0);
                notifyTuneEventListeners(MPE_TUNE_UNSYNC, tunerData->tunerId);
                break;

            case MEDIA_TUNER_STATE_TUNED_NOSYNC:
                MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_MEDIA,
                        "tunerEventCallback -- got redundant NOSYNC state event\n");
                break;

            case MEDIA_TUNER_STATE_IDLE:
                MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_MEDIA,
                        "tunerEventCallback -- got SYNC state event when IDLE. Ignoring.\n");
                break;
            }
            break;
        default:
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                    "tunerEventCallback -- invalid tuner event (%d)\n", event);
            return;
        }
    }

    mpeos_mutexRelease(tunerData->mutex);
}

/* Initialization */
mpe_Error mpeos_mediaInit(void)
{
    uint32_t i;

    if (g_mediaInitialized)
        return MPE_SUCCESS;

    ri_test_RegisterMenu(&MpeosMenuItem);
    ri_test_RegisterMenu(&MpeosMediaMenuItem);

    mpeos_mediaInit3DConfig();

    // Query the platform to determine the number of tuners.
    ri_pipeline_manager_t* pipelineMgr = ri_get_pipeline_manager();
    const ri_pipeline_t** pipelines = pipelineMgr->get_live_pipelines(
            pipelineMgr, &g_numTuners);

    // Ensure that the number of tuners matches the value set in the mpeenv.ini
    // file.  Although the stack implementation now supports querying of the platform
    // to determine the number of tuners, the values can be different if the
    // MPE.SYS.NUM.TUNERS is configured in the mpeenv.ini file and it is different than
    // the number of tuners specified in the RI Platform platform.cfg file.
    const char *configValue = mpeos_envGet("MPE.SYS.NUM.TUNERS");
    int numTuners = atoi(configValue);
    if (numTuners != g_numTuners)
    {
        MPEOS_LOG(
                MPE_LOG_FATAL,
                MPE_MOD_MEDIA,
                "Num platform tuners (%d), does not match MPE.SYS.NUM.TUNERS (%d)\n",
                g_numTuners, numTuners);
        exit(1);
    }

    // Allocate the tuner data.
    if (mpe_memAlloc(sizeof(TunerData) * g_numTuners, (void **) &g_tuners)
            != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "Could not allocated tuner data array\n");
        return MPE_ENOMEM;
    }

    // Allocate the mutex for the tune listeners list
    if (mpeos_mutexNew(&g_tuneEventListenerMutex) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "Could not create mutex for registered tune listeners\n");
        return MPE_ENOMEM;
    }

    for (i = 0; i < g_numTuners; i++)
    {
        // Register our callback for each tuner so that we can receive state information
        ri_tuner_t* tuner = pipelines[i]->get_tuner(
                (ri_pipeline_t*) pipelines[i]);

        if (tuner == NULL)
        {
            MPEOS_LOG(MPE_LOG_FATAL, MPE_MOD_MEDIA,
                    "RI Platform returned NULL tuner at index %d\n", i);
            exit(1);
        }

        g_tuners[i].riTuner = tuner;
        g_tuners[i].riTuner->register_tuner_event_cb(tuner, tunerEventCallback,
                (void*) i);

        // Initialize our tuner data structures
        g_tuners[i].tunerId = i + 1;
        g_tuners[i].tunerState = MEDIA_TUNER_STATE_IDLE;
        g_tuners[i].currentTuneRequest = NULL;
        g_tuners[i].currentAct = NULL;

        if (mpeos_mutexNew(&g_tuners[i].mutex) != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                    "Could not create mutex for tuner %d\n", i);
        }
    }

    if (mpeos_mutexNew(&g_decodeSessionsMutex) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "Could not create decode sessions mutex\n");
        return MPE_ENOMEM;
    }

    for (i = 0; i < MPE_MAX_DECODE_SESSIONS; i++)
    {
        memset(&g_decodeSessions[i], 0, sizeof(mpeos_MediaDecodeSession));
        g_decodeSessions[i].m_type = MEDIA_DECODE_SESSION_UNDEFINED;
        g_decodeSessions[i].m_dispDeviceAct = NULL;
        g_decodeSessions[i].m_dispDeviceQueueID = 0;
        g_decodeSessions[i].m_dispDeviceState = MEDIA_DECODER_STATE_IDLE;
    }

    return MPE_SUCCESS;
}

/**
 * Retrieves the pipeline associated with the tuner which has the
 * supplied tuner ID
 *
 * @return  pipeline associated with tuner which matches supplied id,
 *          NULL if not found
 */
ri_pipeline_t* getPipeline(uint32_t tunerId)
{
    uint32_t cnt;
    ri_pipeline_t** ppPipeline;

    // Pipeline index is equal to tuner id - 1
    uint32_t pIdx = tunerId - 1;

    // Get the RI Platform display element from pipeline manager
    ri_pipeline_manager_t* pMgr = ri_get_pipeline_manager();
    if (NULL != pMgr)
    {
        // Get the live pipelines from pipeline manager
        ppPipeline = (ri_pipeline_t**) pMgr->get_live_pipelines(pMgr, &cnt);

        // Make sure the pipeline index is valid
        if (pIdx >= cnt)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                    "tuner id %d has invalid pipeline idx %d\n", tunerId, pIdx);
        }
        else
        {
            if (NULL != ppPipeline)
            {
                return ppPipeline[pIdx];
            }
            else
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                        "%s() - no live pipelines available\n", __FUNCTION__);
            }
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "%s() - unable to retrieve pipeline manager\n", __FUNCTION__);
    }

    return NULL;
}

/**
 * Returns the video device which is specified as part of this decode request
 */
ri_video_device_t* getVideoDevice(mpe_MediaDecodeRequestParams *decodeRequest)
{
    ri_video_device_t* video_device = NULL;
    if (NULL != decodeRequest->videoDevice)
    {
        // Get the ri video device from os device
        video_device = dispGetVideoDevice(decodeRequest->videoDevice);
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "%s() - video device in request was NULL\n", __FUNCTION__);
    }

    return video_device;
}

/* Shut-down */
mpe_Error mpeos_mediaShutdown(void)
{
    uint32_t i;

    for (i = 0; i < g_numTuners; i++)
    {
        // Delete our tuner mutexes
        mpeos_mutexDelete(g_tuners[i].mutex);

        // Deallocate the param structures for any current or pending requests
        mpe_memFree(g_tuners[i].currentTuneRequest);
    }

    mpe_memFree(g_tuners);
    mpeos_mutexDelete(g_tuneEventListenerMutex);

    return MPE_ERROR_MEDIA_OS;
}

/**
 * <i>mpeos_mediaTune()</i>
 *
 * Starts the tune operation -  by frequency, program number and QAM mode
 *
 * @param tuneRequest Tune request parameters including tunerId, freq/prog_num/qam etc
 * @param queueId A queue to which any subsequent tuning related events will be delivered to
 * @param act A tune-specific parameter to pass back with notifications (as 2nd void*).
 * @return This is an asynchronous operation. This makes a tune request and returns immediately.
 *          When the request completes an event is delivered to the queue (queueId) and with resulting information
 *          in status parameter.
 *
 * At first, a check is performed to see if the tuner is currently processing a
 * tune request. If so, we check to see if there is another pending tune request
 * and in that case the incoming tune request becomes the pending request and
 * previously pending tune request returns with a failure.
 * When the tuner is done processing, the most recent pending request becomes the current request
 * and subsequently gets processed immediately. If an error is returned no subsequent events
 * will be delivered.
 */
mpe_Error mpeos_mediaTune(mpe_MediaTuneRequestParams *tuneRequest,
        mpe_EventQueue queueId, void* act)
{
    TunerData* tunerData;
    mpe_MediaTuneParams* newRequest;

    /* check if queueId is null */
    if (tuneRequest == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "mpeos_mediaTune -- NULL tune request or queue \n");
        return MPE_EINVAL;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_MEDIA, "mpeos_mediaTune(%p) \n",
            tuneRequest);

    // If tune by tuning parameters is specified and frequency is zero return error
    if (tuneRequest->tuneParams.tuneType == MPE_MEDIA_TUNE_BY_TUNING_PARAMS
            && tuneRequest->tuneParams.frequency == 0)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "mpeos_mediaTune -- Tune frequency may not be zero \n");
        return MPE_EINVAL;
    }
	
    // If tune by tuning parameters is specified and modulation is analog, return error
    if (tuneRequest->tuneParams.tuneType == MPE_MEDIA_TUNE_BY_TUNING_PARAMS
            && tuneRequest->tuneParams.qamMode == MPE_SI_MODULATION_QAM_NTSC)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "mpeos_mediaTune -- Tune modulation may not be analog (currently unsupported)\n");
        return MPE_EINVAL;
    }

    // Make sure we have a valid tuner ID
    if (tuneRequest->tunerId > g_numTuners)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "mpeos_mediaTune -- Illegal tuner ID (%d)\n",
                tuneRequest->tunerId);
        return MPE_ERROR_MEDIA_INVALID_ID;
    }

    tunerData = &g_tuners[tuneRequest->tunerId - 1];

    // Make a copy of the current request data
    if (mpeos_memAllocP(MPE_MEM_MEDIA, sizeof(mpe_MediaTuneParams),
            (void **) &newRequest) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "mpeos_mediaTune -- Could not allocate tune params memory\n");
        return MPE_ENOMEM;
    }
    memcpy(newRequest, &tuneRequest->tuneParams, sizeof(mpe_MediaTuneParams));

    // Lock the tuner.
    mpeos_mutexAcquire(tunerData->mutex);

    if (tunerData->currentTuneRequest != NULL)
    {
        finalizeCurrentTuneRequest(tunerData);
    }

    doPlatformTune(tunerData, newRequest, queueId, act);

    g_lastTunedTunerData = tunerData;

    mpeos_mutexRelease(tunerData->mutex);

    return MPE_SUCCESS;
}

/**
 * <i>mpeos_mediaDecode()</i>
 *
 *  Start the MPEG decoder
 *
 * @param decodeRequest Decode requesr params including the display device handle, Pids to select etc.
 * @param queueId A queue to which any subsequent decoder related events will be delivered to
 * @param act A parameter for returned events, passed as the 2nd void*.
 * @param decodeResponse A structure that will be returned at the end of play which contains results of the operation
 *
 * @return This is an asynchronous operation. This makes a powerTv request and returns immediately.
 *          When the request completes an event is delivered to the queue (queueId) and with resulting information
 *          in status parameter.
 */
mpe_Error mpeos_mediaDecode(mpe_MediaDecodeRequestParams *decodeRequest,
        mpe_EventQueue queueId, void* act, mpe_MediaDecodeSession *session)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA, "mpeos_mediaDecode() - called\n");
    mpe_Error ret = MPE_SUCCESS;

    uint32_t tIndex = 0;
    //uint32_t decoderId = 0;
    //char szMediafile[MPE_FS_MAX_PATH] = {0};
    mpeos_MediaDecodeSession *mds = NULL;
    uint32_t ctr = 0;
    ri_pid_info_t* ri_pids;
    ri_pipeline_t* pipeline;

    //MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
    //      "mpeos_mediaDecode() looking at session\n");

    // validate output parameter
    if (session == NULL)
    {
        return MPE_EINVAL;
    }

    // initialize out param
    *session = NULL;

    //MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
    //      "mpeos_mediaDecode() validating parameters\n");

    // validate parameters
    if (decodeRequest == NULL || (decodeRequest->numPids > 0
            && decodeRequest->pids == NULL) || (gpointer) queueId == NULL)
    {
        return MPE_EINVAL;
    }

    // Find the relevant tuner.
    for (tIndex = 0; tIndex < g_numTuners; ++tIndex)
    {
        if (decodeRequest->tunerId == g_tuners[tIndex].tunerId)
        {
            break;
        }
    }

    // could not find tuner for the tunerId provided
    if (tIndex >= g_numTuners)
    {
        return MPE_ERROR_MEDIA_INVALID_ID;
    }

    //MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
    //      "mpeos_mediaDecode() acquiring tuner and decode mutex\n");

    mpeos_mutexAcquire(g_tuners[tIndex].mutex);
    mpeos_mutexAcquire(g_decodeSessionsMutex);

    // make sure the tuner is already tuned
    /*
     if(g_tuners[ tIndex ].tunerState != MEDIA_TUNER_STATE_TUNED_SYNC)
     {
     ret = MPE_ERROR_MEDIA_NOT_TUNED;
     MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
     "mpeos_mediaDecode() called before tuning is complete.\n");
     goto ReleaseAndReturn;
     }
     */

    //MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
    //       "mpeos_mediaDecode() getting decode session\n");

    // see if there is a media decode session associated with this tuner
    mds = tunerToSessionID(&g_tuners[tIndex]);

    // TODO: Note this is a stop - gap measure to handle a second client call to
    // mediaDecode without calling mediaStop, see bug 5116 for true solution
    // A second call to mediaDecode without calling mediaStop should be a failure

    // If currently decoding, stop the decode, cannot have two decodes active on
    // a single tuner. A decode session slot will be available after mediaStop returns.
    if (mds != NULL && mds->m_dispDeviceState != MEDIA_DECODER_STATE_IDLE)
    {
        //MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
        //         "mpeos_mediaDecode() - stop current session 0x%x\n", mds);
        // note that mediaStop will send an MPE_EVENT_SHUTDOWN notification to the
        // current media decode queue (m_dispDeviceQueueID)
        ret = mpeos_mediaStop((mpe_MediaDecodeSession) mds, MPE_MEDIA_STOP_MODE_BLACK);
        if (ret != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                    "mpeos_mediaDecode - Failed to stop active decode %d\n",
                    ret);
            goto ReleaseAndReturn;
        }
    }

    //MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
    //      "mpeos_mediaDecode() creating broadcast session\n");

    // create the new decode session and save the decode parameters
    ret = createBroadcastDecodeSession(decodeRequest, &g_tuners[tIndex],
            queueId, act, MEDIA_DECODE_SESSION_BROADCAST, &mds);
    if (ret != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "mpeos_mediaDecode - Failed to create decode session %d\n", ret);
        goto ReleaseAndReturn;
    }

    //MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
    //       "mpeos_mediaDecode() setting the session\n");

    *session = (mpe_MediaDecodeSession) mds;
    // MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
    //         "mpeos_mediaDecode() - retVal <t, s> = <0x%08x, 0x%08x>\n",
    //         &g_tuners[ tIndex ], *session);

    // get the media file name from the appropriate tuner
    //for (ctr = 0; ctr < decodeRequest->numPids; ctr++)
    //   if (IS_VIDEO_PID(decodeRequest->pids[ctr].pidType)) break;

    // TODO: Check the return code from Tuner_SelectStream as it can fail
    //if (ctr == decodeRequest->numPids)
    //Tuner_SelectStream((WORD) 0, decodeRequest->tunerId, szMediafile, sizeof(szMediafile));

    //else
    //Tuner_SelectStream((WORD) decodeRequest->pids[ctr].pid, decodeRequest->tunerId, szMediafile, sizeof(szMediafile));

    // get the index of the selected decoder
    //decoderId = dispGetDestDevice(decodeRequest->videoDevice);

    // set the media file source in the decoder
    //Decoder_SetSource(decoderId, szMediafile, TRUE);

    // set the requested blocking level before playing the media
    /*
     ret = mpeos_mediaBlockPresentation(*session, decodeRequest->blocked);
     if (ret != MPE_SUCCESS)
     {
     MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
     "mpeos_mediaDecode() could not set blocking = %d\n",
     decodeRequest->blocked);

     goto ReleaseAndReturn;
     }
     */

    mpe_Bool isPlaying = FALSE;

    // *TODO* - adjust log level
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA, "mpeos_mediaDecode() pid cnt: %d\n",
            decodeRequest->numPids);

    if (decodeRequest->numPids == 0)
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA, "%s pid cnt is 0!%d\n",
                __FUNCTION__, decodeRequest->numPids);
    }
    else
    {
        // Allocate an array of RI_Platform pids
        if (mpe_memAlloc(sizeof(ri_pid_info_t) * decodeRequest->numPids,
                (void**) &ri_pids) != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                    "mpeos_mediaDecode() - ERROR - Cannot allocate ri_pids\n");
        }
        else
        {

            // Handle PIDs
            for (ctr = 0; ctr < decodeRequest->numPids; ctr++)
            {
                // Copy to the RI's pid array
                ri_pids[ctr].mediaType = RI_MEDIA_TYPE_UNKNOWN;
                ri_pids[ctr].srcPid = decodeRequest->pids[ctr].pid;
                ri_pids[ctr].srcFormat = decodeRequest->pids[ctr].pidType;
            }

            // Print out list of PIDS
            for (ctr = 0; ctr < decodeRequest->numPids; ctr++)
            {
                if (IS_VIDEO_PID(decodeRequest->pids[ctr].pidType))
                {
                    // *TODO* - adjust log level
                    MPEOS_LOG(
                            MPE_LOG_INFO,
                            MPE_MOD_MEDIA,
                            "mpeos_mediaDecode() pid %d is video pid, asking pipeline to decode\n",
                            decodeRequest->pids[ctr].pid);
                }
                else
                {
                    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_MEDIA,
                            "mpeos_mediaDecode() pid %d is not video pid\n",
                            decodeRequest->pids[ctr].pid);
                }
            }

            // Tell the pipeline to decode the service
            pipeline = getPipeline(g_tuners[tIndex].tunerId);
            if (NULL == pipeline)
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                        "mpeos_mediaDecode() - pipeline was NULL\n");
            }
            else
            {
                ri_video_device_t* video_device = getVideoDevice(decodeRequest);
                if (NULL != video_device)
                {
                    pipeline->decode(pipeline, video_device, ri_pids,
                            decodeRequest->numPids);
                    isPlaying = TRUE;
                }
                else
                {
                    MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                            "mpeos_mediaDecode() - video device was NULL\n");
                }
            }

            // Free the local memory allocated
            mpe_memFree(ri_pids);
        }
    }

    // TODO: Check the return code from Tuner_SelectStream as it can fail
    //if (ctr == decodeRequest->numPids)
    //Tuner_SelectStream((WORD) 0, decodeRequest->tunerId, szMediafile, sizeof(szMediafile));

    // start the decoder
    if (isPlaying)
    {
        mds->m_dispDeviceState = MEDIA_DECODER_STATE_PLAYING;

        //MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
        //       "mpeos_mediaDecode() send event\n");

        // Notify the upper layers that the first frame is decoded.
        // Even if it really isn't.
        mpeos_eventQueueSend(mds->m_dispDeviceQueueID, //g_tuners[ tIndex ].m_dispDeviceQueueID,
                MPE_CONTENT_PRESENTING, (void*)MPE_PRESENTING_2D_SUCCESS, mds->m_dispDeviceAct, // g_tuners[ tIndex ].m_dispDeviceAct,
                0);
        decodeRequest->actualGain = decodeRequest->requestedGain;
        // Work-around for persistent video blocked issue
        mpeos_mediaBlockPresentation(*session, decodeRequest->blocked);
    }
    else
    {
        ret = MPE_ERROR_MEDIA_RESOURCE_NOT_ACTIVE;
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "mpeos_mediaDecode() failed to start decode\n");
        goto ReleaseAndReturn;
    }

    ReleaseAndReturn: mpeos_mutexRelease(g_decodeSessionsMutex);
    mpeos_mutexRelease(g_tuners[tIndex].mutex);
    //MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
    //              "mpeos_mediaDecode() done releasing, now returning\n");
    return ret;
}

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
mpe_Error mpeos_mediaFreeze(mpe_DispDevice videoDevice)
{
    // *TODO* - want to use video device rather than display
    ri_display_t* display = NULL;
    ri_pipeline_manager_t* pMgr = ri_get_pipeline_manager();
    if (NULL != pMgr)
    {
        display = pMgr->get_display(pMgr);
        if (NULL != display)
        {
            display->freeze_video(display);
        }
        else
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                    "mpeos_mediaFreeze() - failed to get display\n");
            return MPE_EINVAL;
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "mpeos_mediaFreeze() - failed to get pipeline manager\n");
        return MPE_EINVAL;
    }

    return MPE_SUCCESS;
}

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
mpe_Error mpeos_mediaResume(mpe_DispDevice videoDevice)
{
    // *TODO* - want to use video device rather than display
    ri_display_t* display = NULL;
    ri_pipeline_manager_t* pMgr = ri_get_pipeline_manager();
    if (NULL != pMgr)
    {
        display = pMgr->get_display(pMgr);
        if (NULL != display)
        {
            display->resume_video(display);
        }
        else
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                    "mpeos_mediaResume() - failed to get display\n");
            return MPE_EINVAL;
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "mpeos_mediaResume() - failed to get pipeline manager\n");
        return MPE_EINVAL;
    }

    return MPE_SUCCESS;
}

/**
 * <i>mpeos_mediaStop()</i>
 *
 *  Stop the decoder
 *
 *
 * @param session display device handle 
 * @param holdFrameMode   display black (default) or last frame
 *
 * @return <i>MPE_SUCCESS</i>  if successfully stopped the decoder,
 *          return <i>MPE_ERROR_MEDIA_OS</i> if the OS throws an exception
 *
 */
mpe_Error mpeos_mediaStop(mpe_MediaDecodeSession session, uint32_t holdFrameMode)
{
    MPE_UNUSED_PARAM(holdFrameMode);

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA, "mpeos_mediaStop() - called\n");

    mpe_Error ret = MPE_SUCCESS;
    mpeos_MediaDecodeSession* mds = (mpeos_MediaDecodeSession*) session;

    // protect access to the decode session
    mpeos_mutexAcquire(g_decodeSessionsMutex);

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_MEDIA, "mpeos_mediaStop() \n");

    // get the pipeline specific to this session
    if (mds->m_dispDeviceState == MEDIA_DECODER_STATE_PLAYING)
    {
        ri_pipeline_t* pPipeline = getPipeline(
                mds->m_typeData.broadcastTuner->tunerId);
        if (NULL != pPipeline)
        {
            pPipeline->decode_stop(pPipeline);
        }
        mds->m_dispDeviceState = MEDIA_DECODER_STATE_IDLE;
    }

    os_mediaDeleteDecodeSession(session, TRUE);
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_MEDIA, "mpeos_mediaStop() COMPLETED \n");
    // protect access to the decode session
    mpeos_mutexRelease(g_decodeSessionsMutex);

    return ret;
}

/**
 * <i>mpeos_mediaNotifyDecodeListener()</i>
 *
 *  Notifies the listener which is associated with the supplied display
 *  device in the list of current decode sessions.
 *
 * @param   dispDevice  display device which is associated with the event
 * @param   event       event to send to associated decode listener
 *
 * @return <i>MPE_SUCCESS</i>  if successfully found decode session associated
 *          with display device and sent event to listener
 *          return <i>MPE_ERROR_MEDIA_OS</i> if the OS throws an exception
 */
mpe_Error os_mediaNotifyDecodeListener(mpe_DispDevice decoder, mpe_Event event,
        void* data)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
            "mpeos_mediaNotifyDecodeListener() - called: decoder = %d\n", (int) decoder);

    mpe_Error ret = MPE_SUCCESS;
    int i = 0;
    mpeos_MediaDecodeSession mds;
    mpe_Bool foundMatch = FALSE;

    mpeos_mutexAcquire(g_decodeSessionsMutex);

    // Find the decode session which has this decoder
    for (i = 0; i < MPE_MAX_DECODE_SESSIONS; i++)
    {
        mds = g_decodeSessions[i];

        if (mds.m_type != MEDIA_DECODE_SESSION_UNDEFINED && dispDeviceEquals(
                mds.m_dispDevice, decoder))
        {
            // Found match, send event
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
                    "mpeos_mediaNotifyDecodeListener() - sending event to decode listener\n");

            mpeos_eventQueueSend(mds.m_dispDeviceQueueID, event, data,
                    mds.m_dispDeviceAct, 0);
            foundMatch = TRUE;
            break;
        }
    }

    if (FALSE == foundMatch)
    {
        MPEOS_LOG(
                MPE_LOG_INFO,
                MPE_MOD_MEDIA,
                "mpeos_mediaNotifyDecodeListener() - unable to find decode session associatd with disp device\n");
    }

    mpeos_mutexRelease(g_decodeSessionsMutex);

    return ret;
}

#if 0   /* these functions are not specifically needed currently in the porting layer */

/**
 * <i>mpeos_mediaSetVolume()</i>
 *
 *  Set the global volume level. This is the master volume control.
 *
 * @param newVolume new volume
 *
 * @return <i>MPE_SUCCESS</i>  if successful,
 *          return <i>MPE_ERROR_MEDIA_OS</i> if the OS throws an exception
 *
 */
mpe_Error mpeos_mediaSetVolume (int32_t newVolume)
{
    MPE_UNUSED_PARAM(newVolume);
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_mediaGetVolume()</i>
 *
 *  Get the global volume level. This is the master volume level.
 *
 * @param volume returned volume
 *
 * @return <i>MPE_SUCCESS</i>  if successful,
 *          return <i>MPE_ERROR_MEDIA_OS</i> if the OS throws an exception
 *
 */
mpe_Error mpeos_mediaGetVolume (int32_t* volume)
{
    MPE_UNUSED_PARAM(volume);
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_mediaSetMuteState()</i>
 *
 *  Set the global mute state.
 *
 * @param muteState new mute state
 *
 * @return <i>MPE_SUCCESS</i>  if successful,
 *          return <i>MPE_ERROR_MEDIA_OS</i> if the OS throws an exception
 *
 */
mpe_Error mpeos_mediaSetMuteState (mpe_Bool muteState)
{
    MPE_UNUSED_PARAM(muteState);
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_mediaGetMuteState()</i>
 *
 *  Get the global mute state.
 *
 * @param muteState returned mute state
 *
 * @return <i>MPE_SUCCESS</i>  if successful,
 *          return <i>MPE_ERROR_MEDIA_OS</i> if the OS throws an exception
 *
 */
mpe_Error mpeos_mediaGetMuteState (mpe_Bool *muteState)
{
    MPE_UNUSED_PARAM(muteState);
    return MPE_SUCCESS;
}

#endif /* 0 */

/**
 * <i>mpeos_mediaSetBounds()</i>
 *
 *  Set the video playback boundaries and video source boundaries
 *
 * @param videoDevice is the video display device handle
 * @param srcRect source bounds
 * @param destRect destination bounds
 *
 * @return <i>MPE_SUCCESS</i>  if successful,
 *          return <i>MPE_ERROR_MEDIA_OS</i> if the OS throws an exception
 *
 */
mpe_Error mpeos_mediaSetBounds(mpe_DispDevice videoDevice,
        mpe_MediaRectangle *srcRect, mpe_MediaRectangle *destRect)
{
    // *TODO* - want to use video device rather than display
    ri_display_t* display = NULL;
    ri_pipeline_manager_t* pMgr = ri_get_pipeline_manager();
    if (NULL != pMgr)
    {
        display = pMgr->get_display(pMgr);
        if (NULL != display)
        {
            display->set_bounds(display, (ri_rect*) srcRect,
                    (ri_rect*) destRect);
        }
        else
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                    "mpeos_mediaSetBounds() - failed to get display\n");
            return MPE_EINVAL;
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "mpeos_mediaSetBounds() - failed to get pipeline manager\n");
        return MPE_EINVAL;
    }

    return MPE_SUCCESS;
}

/**
 * <i>mpeos_mediaGetBounds()</i>
 *
 *  Get the video playback boundaries and video source boundaries
 *
 * @param videoDevice is the video display device handle
 * @param srcRect returned source bounds
 * @param destRect returned destination bounds
 *
 * @return <i>MPE_SUCCESS</i>  if successful,
 *          return error otherwise.
 *
 */
mpe_Error mpeos_mediaGetBounds(mpe_DispDevice videoDevice,
        mpe_MediaRectangle *srcRect, mpe_MediaRectangle *destRect)
{
    // *TODO* - want to use video device rather than display
    ri_display_t* display = NULL;
    ri_pipeline_manager_t* pMgr = ri_get_pipeline_manager();
    if (NULL != pMgr)
    {
        display = pMgr->get_display(pMgr);
        if (NULL != display)
        {
            display->get_bounds(display, (ri_rect*) srcRect,
                    (ri_rect*) destRect);
        }
        else
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                    "mpeos_mediaGetBounds() - failed to get display\n");
            return MPE_EINVAL;
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "mpeos_mediaGetBounds() - failed to get pipeline manager\n");
        return MPE_EINVAL;
    }

    return MPE_SUCCESS;
}

/**
 * <i>mpeos_mediaCheckBounds()</i>
 *
 * This API validates that the desired source and destination video playback bounds can be supported.
 * If the specified bounds are directly supportable, then the function will return without error and
 * the input desired bounds specifications will match the actual bounds output parameters.  If the
 * bounds are not supportable, an error will be returned and the closest approximation bounds will be
 * returned via the output parameters.
 *
 * @param videoDevice is target decoder video device.
 * @param desiredSrc is a pointer to the desired source dimension.
 * @param desiredDst is a pointer to the desired destination dimension.
 * @param actualSrc is a pointer for returning the actual source dimension.
 * @param actualDst is a pointer for returning the actual destination dimension.
 *
 * @return MPE_SUCCESS if the desired bounds dimensions are supportable, or a usable
 *         alternative dimension has been returned.
 */
mpe_Error mpeos_mediaCheckBounds(mpe_DispDevice videoDevice,
        mpe_MediaRectangle *desiredSrc, mpe_MediaRectangle *desiredDst,
        mpe_MediaRectangle *actualSrc, mpe_MediaRectangle *actualDst)
{
    // *TODO* - want to use video device rather than display
    ri_display_t* display = NULL;
    ri_pipeline_manager_t* pMgr = ri_get_pipeline_manager();
    if (NULL != pMgr)
    {
        display = pMgr->get_display(pMgr);
        if (NULL != display)
        {
            display->check_bounds(display, (ri_rect*) desiredSrc,
                    (ri_rect*) desiredDst, (ri_rect*) actualSrc,
                    (ri_rect*) actualDst);
        }
        else
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                    "mpeos_mediaCheckBounds() - failed to get display\n");
            return MPE_EINVAL;
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "mpeos_mediaCheckBounds() - failed to get pipeline manager\n");
        return MPE_EINVAL;
    }

    return MPE_SUCCESS;
}

/**
 * <i>mpeos_mediaGetTunerInfo</i>
 *
 * Get the current tuning parameters for the target tuner.
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
        mpe_MediaTuneParams* tuneParams)
{
    TunerData* tunerData;
    ri_tuner_status_t tuner_status;

    if (tuneParams == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "mpeos_mediaGetTunerInfo -- NULL tuneParams\n");
        return MPE_EINVAL;
    }

    // Make sure we have a valid tuner ID
    if (tunerId > g_numTuners)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "mpeos_mediaGetTunerInfo -- Illegal tuner ID (%d)\n", tunerId);
        return MPE_ERROR_MEDIA_INVALID_ID;
    }

    tunerData = &g_tuners[tunerId - 1];

    // Request the status of the tuner
    tunerData->riTuner->request_status(tunerData->riTuner, &tuner_status);

    // Assign tuner params to callers structure
    tuneParams->tuneType = MPE_MEDIA_TUNE_BY_TUNING_PARAMS;
    tuneParams->frequency = tuner_status.frequency;
    tuneParams->programNumber = tuner_status.program_num;
    tuneParams->qamMode = modmodeRItoMPE(tuner_status.mode);

    return MPE_SUCCESS;
}

mpe_Error mpeos_mediaFrequencyToTuner(uint32_t frequency, uint32_t *tuner)
{
    uint32_t i;
    uint32_t numPipelines;
    const ri_pipeline_t** pipelines =
            ri_get_pipeline_manager()->get_live_pipelines(
                    ri_get_pipeline_manager(), &numPipelines);
    ri_tuner_t* ri_tuner;
    ri_tuner_status_t tuner_status;

    // Iterate over all of our live pipelines looking for one tuned
    // to the requested frequency
    for (i = 0; i < numPipelines; i++)
    {
        ri_tuner = pipelines[i]->get_tuner((ri_pipeline_t*) pipelines[i]);
        ri_tuner->request_status(ri_tuner, &tuner_status);

        if (tuner_status.frequency == frequency)
        {
            *tuner = i + 1; // MPE in-band tuners start at 1
            return MPE_SUCCESS;
        }
    }

    *tuner = 0;
    return MPE_EINVAL;
}

mpe_Error mpeos_mediaRegisterQueueForTuneEvents(mpe_EventQueue queueId)
{
    mpeos_mutexAcquire(g_tuneEventListenerMutex);

    g_registeredTuneEventListeners = g_slist_prepend(
            g_registeredTuneEventListeners, (gpointer) queueId);

    mpeos_mutexRelease(g_tuneEventListenerMutex);

    return MPE_SUCCESS;
}

mpe_Error mpeos_mediaUnregisterQueue(mpe_EventQueue queueId)
{
    mpeos_mutexAcquire(g_tuneEventListenerMutex);

    // Just run thru the list and send events.
    g_registeredTuneEventListeners = g_slist_remove(
            g_registeredTuneEventListeners, (gconstpointer) queueId);

    mpeos_mutexRelease(g_tuneEventListenerMutex);

    return MPE_SUCCESS;
}

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
mpe_Error mpeos_mediaGetScaling(mpe_DispDevice videoDevice,
        mpe_MediaPositioningCapabilities* positioning, float** horiz,
        float** vert, mpe_Bool* hRange, mpe_Bool* vRange, mpe_Bool* canClip,
        mpe_Bool* supportsComponent)
{
    // Arbitrary scaling is supported on the simulator.
    static float horizontalScaleFactors[3];
    static float verticalScaleFactors[3];
    mpe_Error ret = MPE_SUCCESS;

    // Return the positioning capabilities if output parameter if it is not null
    if (positioning != NULL)
    {
        // PC platform supports positioning anywhere on the screen even if part
        // of the video is off screen *
        *positioning = MPE_POS_CAP_FULL;
    }

    // Return the scaling factors if the output parameters are not null
    if (hRange != NULL)
    {
        // PC Platform supports arbitrary horizontal scaling
        *hRange = TRUE;
        if (horiz != NULL)
        {
            horizontalScaleFactors[0] = 0.0f;
            horizontalScaleFactors[1] = 1.0f;
            horizontalScaleFactors[2] = -1.0f; // list terminator
            *horiz = horizontalScaleFactors;
        }
        else
        {
            ret = MPE_EINVAL;
        }
    }

    if (vRange != NULL)
    {
        // PC Platform supports arbitrary vertical scaling
        *vRange = TRUE;
        if (vert != NULL)
        {
            verticalScaleFactors[0] = 0.0f;
            verticalScaleFactors[1] = 1.0f;
            verticalScaleFactors[2] = -1.0f; // list terminator
            *vert = verticalScaleFactors;
        }
        else
        {
            ret = MPE_EINVAL;
        }
    }

    // Return the clipping support if the output parameter is not null
    if (canClip != NULL)
    {
        *canClip = TRUE;
    }

    // return component support if the output parameter is not null
    if (supportsComponent != NULL)
    {
        *supportsComponent = FALSE;
    }

    return ret;
}

//
// Swap the video source streams feeding the two specified decoders.
//
// @param decoder1 is the first mpe_DispDevice for the swap
// @param decoder2 is the second mpe_DispDevice for the swap
// @param useAudio is a boolean value indicating which audio to
//        be presented after the swap, if true the audio associated
//        with the first decoder's stream is presented after the swap.
//
// @return mpe_Error if either of the display devices is invalid
//
mpe_Error mpeos_mediaSwapDecoders(mpe_DispDevice videoDevice1,
        mpe_DispDevice videoDevice2, mpe_Bool useAudio)
{
    MPE_UNUSED_PARAM(videoDevice1);
    MPE_UNUSED_PARAM(videoDevice2);
    MPE_UNUSED_PARAM(useAudio);
    return MPE_SUCCESS;
}

/**
 * Get the current broadcast aspect ratio for the specified decoder
 *
 * @param decoder is the decoder
 * @param ar is a pointer to an aspect ratio
 *
 * @return Aspect ratio (or unknown if can't determine)
 */
mpe_Error mpeos_mediaGetAspectRatio(mpe_DispDevice decoder,
        mpe_MediaAspectRatio *ar)
{
    // *TODO* - want to use video device rather than display
    ri_display_t* display = NULL;
    ri_pipeline_manager_t* pMgr = ri_get_pipeline_manager();

    if (NULL != pMgr)
    {
        display = pMgr->get_display(pMgr);
        if (NULL != display)
        {
            display->get_incoming_video_aspect_ratio(display, (int32_t*) ar);
        }
        else
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                    "mpeos_mediaGetAspectRatio() - failed to get display\n");
            return MPE_EINVAL;
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "mpeos_mediaGetAspectRatio() - failed to get pipeline manager\n");
        return MPE_EINVAL;
    }

    return MPE_SUCCESS;
}

/**
 * Get the current Active Format Description for the given decoder
 *
 * @param decoder is the decoder
 * @param afd is a pointer to an AFD variable
 *
 * @return error level
 */
mpe_Error mpeos_mediaGetAFD(mpe_DispDevice decoder,
        mpe_MediaActiveFormatDescription *afd)
{
    // *TODO* - want to use video device rather than display
    ri_display_t* display = NULL;
    ri_pipeline_manager_t* pMgr = ri_get_pipeline_manager();
    if (NULL != pMgr)
    {
        display = pMgr->get_display(pMgr);
        if (NULL != display)
        {
            display->get_video_afd(display, (uint32_t*) afd);
        }
        else
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                    "mpeos_mediaGetAFD() - failed to get display\n");
            return MPE_EINVAL;
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "mpeos_mediaGetAFD() - failed to get pipeline manager\n");
        return MPE_EINVAL;
    }

    return MPE_SUCCESS;
}

/**
 * Get the dimensions of the input video.
 */
mpe_Error mpeos_mediaGetInputVideoSize(mpe_DispDevice dev,
        mpe_GfxDimensions *dim)
{
    // *TODO* - want to use video device rather than display
    ri_display_t* display = NULL;

    ri_pipeline_manager_t* pMgr = ri_get_pipeline_manager();
    if (NULL != pMgr)
    {
        display = pMgr->get_display(pMgr);
        if (NULL != display)
        {
            display->get_incoming_video_size(display, (int32_t*) &dim->width,
                    (int32_t*) &dim->height);
        }
        else
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                    "mpeos_mediaGetInputVideoSize() - failed to get display\n");
            return MPE_EINVAL;
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "mpeos_mediaGetInputVideoSize() - failed to get pipeline manager\n");
        return MPE_EINVAL;
    }

    return MPE_SUCCESS;
}

/**
 * Initialize a drip feed decode session based on the input parameters.   The decode
 * session can be used to later submit frames of data for decode and to stop the
 * decode.   For now, other aspects of the drip feed (such as bounds) can be
 * manipulated using the associated video device with other mpe_media routines.  In
 * the future, the intent is to have all media routines accept a decode session.
 *
 * @param dripFeedRequest the drip feed request parameters for starting the drip feed
 * @param eventQueue the event queue to post drip feed events.
 * @param act     (async completion token) the completion token for async events
 * @param mediaDecodeSession the session filled in by the method to id the drip feed
 *
 * @return
 *     MPE_EINVAL - an input parameter is invalid (null pointer)
 *     MPE_ERROR_MEDIA_RESOURCE_BUSY - All decode sessions are in use
 *     MPE_SUCCESS - the function completed successfully
 */
mpe_Error mpeos_mediaDripFeedStart(
        mpe_MediaDripFeedRequestParams *dripFeedRequest,
        mpe_EventQueue eventQueue, void *act,
        mpe_MediaDecodeSession *mediaDripFeedSession)
{
    mpe_Error ret = MPE_SUCCESS;

    mpeos_MediaDecodeSession* session =
            (mpeos_MediaDecodeSession*) mediaDripFeedSession;

    ret = createDripFeedSession(dripFeedRequest, eventQueue, act,
            MEDIA_DECODE_SESSION_DRIPFEED, &session);
    if (ret != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "mpeos_dripFeedStart - Failed to create dripfeed session %d\n",
                ret);
    }
    else
    {
        *mediaDripFeedSession = (mpe_MediaDecodeSession) session;
    }

    return ret;
}

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
 *  MPE_SUCCESS - the function completed successfully
 */
mpe_Error mpeos_mediaDripFeedRenderFrame(
        mpe_MediaDecodeSession mediaDecodeSession, uint8_t *buffer,
        size_t length)
{
    MPE_UNUSED_PARAM(buffer);
    MPE_UNUSED_PARAM(length);

    mpeos_MediaDecodeSession *mds =
            (mpeos_MediaDecodeSession *) mediaDecodeSession;

    //TODO: drip feed support is not yet implemented, but calls to dripFeedRenderFrame need to post the event on the queue
    return mpeos_eventQueueSend(mds->m_dispDeviceQueueID,
            MPE_STILL_FRAME_DECODED, NULL, (void*) mds->m_dispDeviceAct, 0);
}

/**
 * Stop a drip feed decode session that was started with mpe_mediaDripFeedStart
 *
 * @param mediaDecodeSession the session filled in by the method to id the drip feed
 *
 * @return
 *     MPE_EINVAL - an input parameter is invalid (null pointer)
 *  MPE_SUCCESS - the function completed successfully
 */
mpe_Error mpeos_mediaDripFeedStop(mpe_MediaDecodeSession mediaDecodeSession)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA, "mpeos_dripFeedStop() - called\n");
    mpeos_MediaDecodeSession* mds =
            (mpeos_MediaDecodeSession*) mediaDecodeSession;

    // protect access to the decode session
    mpeos_mutexAcquire(g_decodeSessionsMutex);

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_MEDIA, "mpeos_dripFeedStop() \n");

    mds->m_dispDeviceState = MEDIA_DECODER_STATE_IDLE;

    os_mediaDeleteDecodeSession(mediaDecodeSession, TRUE);
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_MEDIA, "mpeos_dripFeedStop() COMPLETED \n");
    // protect access to the decode session
    mpeos_mutexRelease(g_decodeSessionsMutex);

    return MPE_SUCCESS;
}

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
mpe_Error mpeos_mediaGetSTC(uint32_t tuner, uint32_t pcrPid, uint64_t *out)
{
    MPE_UNUSED_PARAM(tuner);
    MPE_UNUSED_PARAM(pcrPid);
    MPE_UNUSED_PARAM(out);
    return MPE_ENODATA;
}

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
        mpe_Bool block)
{
    // *TODO* - want to use video device rather than display
    ri_display_t* display = NULL;
    ri_pipeline_manager_t* pMgr = ri_get_pipeline_manager();
    if (NULL != pMgr)
    {
        display = pMgr->get_display(pMgr);
        if (NULL != display)
        {
            display->block_presentation(display, (ri_bool) block);
        }
        else
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                    "mpeos_mediaBlockPresentation() - failed to get display\n");
            return MPE_EINVAL;
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "mpeos_mediaBlockPresentation() - failed to get pipeline manager\n");
        return MPE_EINVAL;
    }

    return MPE_SUCCESS;
}

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
        uint32_t numPids, mpe_MediaPID *pids, uint32_t pcrPid)
{
    ri_pid_info_t* ri_pids;
    mpeos_MediaDecodeSession* mds = (mpeos_MediaDecodeSession *) session;
    uint32_t ctr = 0;
    ri_pipeline_t* pipeline;
    ri_video_device_t* video_device;
    mpe_Bool isPlaying = FALSE;

    if (session == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "mpe_mediaChangePids() - ERROR - Decode Session is NULL \n");
        return MPE_EINVAL;
    }

    // protect access to the decode session
    mpeos_mutexAcquire(g_decodeSessionsMutex);

    if (mds->m_type == MEDIA_DECODE_SESSION_DRIPFEED)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "mpe_mediaChangePids() can NOT operate on a drip feed decode session\n");
        mpeos_mutexRelease(g_decodeSessionsMutex);
        return MPE_EINVAL;
    }

    // Get the tuner currently associated with this decode session
    if (NULL == mds->m_typeData.broadcastTuner)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "mpe_mediaChangePids() - NULL tuner in decode session\n");
        mpeos_mutexRelease(g_decodeSessionsMutex);
        return MPE_EINVAL;
    }

    // Allocate an array of RI_Platform pids
    if (mpe_memAlloc(sizeof(ri_pid_info_t) * numPids, (void**) &ri_pids)
            != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "%s() - ERROR - Cannot allocate ri_pids\n", __FUNCTION__);
    }
    else
    {
        // Handle PIDs
        for (ctr = 0; ctr < numPids; ctr++)
        {
            // Copy to the RI's pid array
            ri_pids[ctr].mediaType = RI_MEDIA_TYPE_UNKNOWN;
            ri_pids[ctr].srcPid = pids[ctr].pid;
            ri_pids[ctr].srcFormat = pids[ctr].pidType;
        }

        // Print out list of PIDS
        for (ctr = 0; ctr < numPids; ctr++)
        {
            if (IS_VIDEO_PID(pids[ctr].pidType))
            {
                // *TODO* - adjust log level
                MPEOS_LOG(
                        MPE_LOG_INFO,
                        MPE_MOD_MEDIA,
                        "%s() - pid %d is video pid, asking pipeline to decode\n",
                        __FUNCTION__, pids[ctr].pid);
            }
            else
            {
                MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
                        "%s() - pid %d is not video pid\n", __FUNCTION__,
                        pids[ctr].pid);
            }
        }

        // Tell the pipeline to decode the service
        pipeline = getPipeline(mds->m_typeData.broadcastTuner->tunerId);
        if (NULL == pipeline)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                    "%s() - pipeline was NULL\n", __FUNCTION__);
        }
        else
        {
            video_device = dispGetVideoDevice(mds->m_dispDevice);
            if (NULL != video_device)
            {
                pipeline->decode(pipeline, video_device, ri_pids, numPids);
                isPlaying = TRUE;
            }
            else
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                        "mpeos_mediaChangePids() - video device was NULL\n");
            }
        }

        // Release the memory used for ri platform call
        mpe_memFree(ri_pids);
    }
    if (isPlaying)
    {
        //MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
        //       "mpeos_mediaDecode() send event\n");

        // Notify the upper layers that the first frame is decoded.
        // Even if it really isn't.
        mpeos_eventQueueSend(mds->m_dispDeviceQueueID, //g_tuners[ tIndex ].m_dispDeviceQueueID,
                MPE_CONTENT_PRESENTING, (void*)MPE_PRESENTING_2D_SUCCESS, mds->m_dispDeviceAct, // g_tuners[ tIndex ].m_dispDeviceAct,
                0);
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "mpeos_mediaChangePids() failed\n");
        mpeos_eventQueueSend(mds->m_dispDeviceQueueID, //g_tuners[ tIndex ].m_dispDeviceQueueID,
                MPE_FAILURE_UNKNOWN, NULL, mds->m_dispDeviceAct, // g_tuners[ tIndex ].m_dispDeviceAct,
                0);
    }

    mpeos_mutexRelease(g_decodeSessionsMutex);

    return MPE_SUCCESS;
}

/**
 * <i>mpeos_mediaSetMute()</i>
 *
 * Set the mute
 */
mpe_Error mpeos_mediaSetMute(mpe_MediaDecodeSession session, mpe_Bool mute)
{
    MPE_UNUSED_PARAM(session);
    MPE_UNUSED_PARAM(mute);
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_mediaSetGain()</i>
 *
 * Set the gain
 */
mpe_Error mpeos_mediaSetGain(mpe_MediaDecodeSession session, float gain, float *actualGain)
{
    MPE_UNUSED_PARAM(session);
    MPE_UNUSED_PARAM(gain);
    *actualGain = gain;
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_mediaSetCCI()</i>
 *
 * Set the cci
 */
mpe_Error mpeos_mediaSetCCI(mpe_MediaDecodeSession session, uint8_t cci)
{
    MPE_UNUSED_PARAM(session);
    MPE_UNUSED_PARAM(cci);
    return MPE_SUCCESS;
}

mpe_Error mpeos_mediaGetInputVideoScanMode (mpe_MediaDecodeSession session, mpe_MediaScanMode* scanMode)
{
    *scanMode = g_videoScanMode;

    return (mpe_Error) MPE_SUCCESS;
}

mpe_Error mpeos_mediaGet3DConfig (mpe_MediaDecodeSession session, mpe_DispStereoscopicMode* stereoscopicMode,
    mpe_Media3DPayloadType* payloadType, uint8_t* payload, uint32_t* payloadSz)
{
    if (*payloadSz < g_payloadSz)
    {
        *payloadSz = g_payloadSz;
        return (mpe_Error) MPE_ENOMEM;
    }

    *payloadType = g_payloadType;
    *stereoscopicMode = g_stereoscopicMode;

    memcpy (payload, g_payload, g_payloadSz);

    *payloadSz = g_payloadSz;

    return (mpe_Error) MPE_SUCCESS;
}

/**
 *  Returns (if in the list) the corresponding tuner.
 *  @param tuner The associated tuner.
 */
static mpeos_MediaDecodeSession* tunerToSessionID(TunerData* tuner)
{
    int index;
    mpeos_MediaDecodeSession* mds = NULL;

    mpeos_mutexAcquire(g_decodeSessionsMutex);

    // If session does not exist return null.
    for (index = 0; index < MPE_MAX_DECODE_SESSIONS; ++index)
    {
        mds = &g_decodeSessions[index];

        if (mds->m_type == MEDIA_DECODE_SESSION_BROADCAST
                && mds->m_typeData.broadcastTuner == tuner)
        {
            MPEOS_LOG(
                    MPE_LOG_TRACE2,
                    MPE_MOD_MEDIA,
                    " tunerToSessionID() - found session  <t,mds> = <0x%p, 0x%p>\n",
                    tuner, mds);
            mpeos_mutexRelease(g_decodeSessionsMutex);
            return (mds);
        }
    }

    MPEOS_LOG(
            MPE_LOG_TRACE2,
            MPE_MOD_MEDIA,
            "tunerToSessionID() - session not found retVal <t,s> = <0x%p, NULL>\n",
            tuner);
    mpeos_mutexRelease(g_decodeSessionsMutex);
    return NULL;
}

/**
 * Creates a media decode session. Tuning must occur first.
 *
 * @param videoDevice the video device on which to display the decoded output
 * @param tuner  the tuner used for the session.
 * @param eventQueue the event queue to post drip feed events.
 * @param act   (async completion token) the completion token for async events
 * @param type  the decode session type being created
 * @param mds   the session filled in by the method
 */
mpe_Error createBroadcastDecodeSession(
        mpe_MediaDecodeRequestParams *decodeParams, TunerData *tuner,
        mpe_EventQueue queueId, void *act, MediaDecodeSessionType type,
        mpeos_MediaDecodeSession **mds)
{
    uint32_t index;
    //mpe_Error err = MPE_SUCCESS;

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_MEDIA,
            "createBroadcastDecodeSession() - type=%d\n", type);

    if (type != MEDIA_DECODE_SESSION_BROADCAST)
    {
        return MPE_EINVAL;
    }

    // protect access to the decode session array
    mpeos_mutexAcquire(g_decodeSessionsMutex);

    // Find an open slot in the decode session array
    if ((index = getDecodeSessionEntry()) == MEDIA_DECODE_UNAVAILABLE)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "createBroadcastDecodeSession() - ERROR - No decode entry available\n");

        mpeos_mutexRelease(g_decodeSessionsMutex);
        return MPE_ERROR_MEDIA_RESOURCE_BUSY;
    }

    //
    // Save decode parameters.
    //
    g_decodeSessions[index].m_dispDevice = decodeParams->videoDevice;
    g_decodeSessions[index].m_dispDeviceQueueID = queueId;
    g_decodeSessions[index].m_dispDeviceAct = act;
    g_decodeSessions[index].m_type = type;
    g_decodeSessions[index].m_typeData.broadcastTuner = tuner;

    *mds = &g_decodeSessions[index];

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_MEDIA,
            "createBroadcastDecodeSession() - session = 0x%p COMPLETE\n", *mds);

    mpeos_mutexRelease(g_decodeSessionsMutex);

    return 0;
}

/**
 * Creates a media dripfeed session. Tuning must occur first.
 *
 * @param eventQueue the event queue to post drip feed events.
 * @param act   (async completion token) the completion token for async events
 * @param type  the decode session type being created
 * @param mds   the session filled in by the method
 */
mpe_Error createDripFeedSession(mpe_MediaDripFeedRequestParams *dripFeedParams,
        mpe_EventQueue queueId, void *act, MediaDecodeSessionType type,
        mpeos_MediaDecodeSession **mds)
{
    uint32_t index;
    //mpe_Error err = MPE_SUCCESS;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_MEDIA,
            "createDripFeedSession() - type=%d\n", type);

    if (type != MEDIA_DECODE_SESSION_DRIPFEED)
    {
        return MPE_EINVAL;
    }

    // protect access to the decode session array
    mpeos_mutexAcquire(g_decodeSessionsMutex);

    // Find an open slot in the decode session array
    if ((index = getDecodeSessionEntry()) == MEDIA_DECODE_UNAVAILABLE)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "createDripFeedSession() - ERROR - No decode entry available\n");

        mpeos_mutexRelease(g_decodeSessionsMutex);
        return MPE_ERROR_MEDIA_RESOURCE_BUSY;
    }

    //
    // Save decode parameters.
    //
    g_decodeSessions[index].m_dispDevice = dripFeedParams->videoDevice;
    g_decodeSessions[index].m_dispDeviceQueueID = queueId;
    g_decodeSessions[index].m_dispDeviceAct = act;
    g_decodeSessions[index].m_type = type;
    g_decodeSessions[index].m_typeData.broadcastTuner = NULL;

    *mds = &g_decodeSessions[index];

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_MEDIA,
            "createDripFeedSession() - session = 0x%p COMPLETE\n", *mds);

    mpeos_mutexRelease(g_decodeSessionsMutex);

    return 0;
}

/*
 * This function finds and returns the first available decode session entry.
 *
 * @param: N/A
 *
 * @return: position of the first available decode session.
 *
 */
static uint32_t getDecodeSessionEntry(void)
{
    uint32_t i;

    for (i = 0; i < MPE_MAX_DECODE_SESSIONS; i++)
    {
        if (g_decodeSessions[i].m_type == MEDIA_DECODE_SESSION_UNDEFINED)
        {
            return i;
        }
    }
    return MEDIA_DECODE_UNAVAILABLE;
}

/**
 *  Utility function used by all decode session types (BROADCAST, DRIPFEED, DVR) to
 *  delete a decode session and make it available for reuse.
 *
 * @param mediaDecodeSession the session being deleted
 */
static void os_mediaDeleteDecodeSession(mpe_MediaDecodeSession session,
        mpe_Bool notify)
{
    mpeos_MediaDecodeSession *mds = (mpeos_MediaDecodeSession *) session;

    // The mutex MUST be acquired by callers of this function.
    // There is no need to acquire the mutex again.
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_MEDIA,
            "os_mediaDeleteDecodeSession() - session = 0x%p\n", session);

    if (mds == NULL)
    {
        MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_MEDIA,
                "os_mediaDeleteDecodeSession() - input is NULL\n");
        mpeos_mutexRelease(g_decodeSessionsMutex);
        return;
    }

    //
    // Inform any queue owner to expect no more events.
    //
    if (notify && (gpointer) mds->m_dispDeviceQueueID != NULL)
        mpeos_eventQueueSend(mds->m_dispDeviceQueueID, MPE_EVENT_SHUTDOWN,
                NULL, mds->m_dispDeviceAct, 0);

    memset(mds, 0, sizeof(mpeos_MediaDecodeSession));
    mds->m_type = MEDIA_DECODE_SESSION_UNDEFINED;

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_MEDIA,
            "os_mediaDeleteDecodeSession() - session = 0x%p COMPLETE\n", mds);
}

mpe_Error mpeos_mediaInit3DConfig ()
{
    ri_display_t* display = NULL;
    int32_t stereoscopicModeTemp;
    int32_t payloadTypeTemp;
    int32_t scanModeTemp;

    ri_pipeline_manager_t* pMgr = ri_get_pipeline_manager();
    if (NULL != pMgr)
    {
        display = pMgr->get_display(pMgr);
        if (NULL != display)
        {
            int nReturnCode = display->get_threedtv_info(display, &stereoscopicModeTemp, &payloadTypeTemp,
                &g_payloadSz, NULL, &scanModeTemp);
            if (nReturnCode != 0)
            {
                g_payload = (uint8_t*) malloc (g_payloadSz);

                nReturnCode = display->get_threedtv_info(display, &stereoscopicModeTemp, &payloadTypeTemp,
                    &g_payloadSz, g_payload, &scanModeTemp);
                if (nReturnCode == 0)
                {
                    g_payloadType = payloadTypeTemp;
                    g_stereoscopicMode = stereoscopicModeTemp;
                    g_videoScanMode = scanModeTemp;
                }
                else
                {
                    MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                            "MEDIA: could not retrieve 3DTV settings -- 1\n");
                    return MPE_EINVAL;
                }
            }
            else
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                        "MEDIA: could not retrieve 3DTV settings -- 2\n");
                return MPE_EINVAL;
            }
        }
        else
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                    "MEDIA: could not retrieve 3DTV settings -- 3\n");
            return MPE_EINVAL;
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_MEDIA,
                "MEDIA: could not retrieve pipeline manager -- 4\n");
        return MPE_EINVAL;
    }
        
    return (mpe_Error) MPE_SUCCESS;
}

void os_mediaNotifySyncStateChange(gboolean synced)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA,
              "os_mediaNotifySyncStateChange(%s)\n", synced ? "TRUE" : "FALSE");
    MediaTunerState newState = synced ? MEDIA_TUNER_STATE_TUNED_SYNC
                                      : MEDIA_TUNER_STATE_TUNED_NOSYNC;
    int msg = synced ? MPE_TUNE_SYNC : MPE_TUNE_UNSYNC;

    if (g_lastTunedTunerData == NULL)
    {
        return;
    }

    g_lastTunedTunerData->tunerState = newState;
    mpeos_eventQueueSend(g_lastTunedTunerData->currentQueueID, msg,
            NULL, (void*) g_lastTunedTunerData->currentAct, 0);
    notifyTuneEventListeners(msg, g_lastTunedTunerData->tunerId);
}

void os_mediaNotifyTuneAbort(void)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA, "os_mediaNotifyTuneAbort()\n");
    mpeos_eventQueueSend(g_lastTunedTunerData->currentQueueID, MPE_TUNE_ABORT,
            NULL, (void*) g_lastTunedTunerData->currentAct, 0);
    notifyTuneEventListeners(MPE_TUNE_ABORT, g_lastTunedTunerData->tunerId);
}

mpe_Error os_mediaNotifyCCIUpdate(uint8_t cci)
{
    mpeos_MediaDecodeSession * decodeSession = NULL;

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA, "os_mediaNotifyCCIUpdate(%02x)\n", cci);

    decodeSession = getCurrentSession();

    if (decodeSession != NULL)
    {
        mpe_EventQueue queue = decodeSession->m_dispDeviceQueueID;
        void* act = decodeSession->m_dispDeviceAct;

        return mpeos_eventQueueSend(queue, MPE_CCI_UPDATE, NULL, act, cci);
    }

    return MPE_EINVAL;
}
