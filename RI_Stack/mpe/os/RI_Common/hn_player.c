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

/*
 * Stubs of MPE OS HN API for the Simulator(Windows) platform.
 *
 * For ease of maintenance the complete description of all hn functions are commented
 * in mpeos_hn.h and not in each of the platform specific implementation files such
 * as this one.
 *
 */
#include <stdlib.h>
#include <limits.h>
#include <stdio.h>
#include <string.h>
#include <time.h>
#include <inttypes.h>

#include "mpe_types.h"
#include "mpe_error.h"
#include "mpe_os.h"

#include "mpeos_hn.h"
#include "mpeos_media.h"
#include "mpeos_dbg.h"
#include "mpeos_event.h"
#include "mpeos_thread.h"
#include "mpeos_sync.h"
#include "mpeos_mem.h"
#include "mpeos_util.h"
#include "mpeos_time.h"

#include "platform_common.h"
#include "platform_filter.h"

#include <ri_pipeline_manager.h>
#include <ri_pipeline.h>
#include "hn_player.h"
#include "hn_player_read_thread.h"
#include "hn_player_http.h"
#include "hn_dtcpip.h"

#define INCREASE_HN_PLAYER_THREAD_PRIORITY
#define INCREASE_SOCKET_RCV_BUF_SIZE // defining sets Win32 sock bufsiz == Linux

// Define the memory to be HN allocated category
#define MPE_MEM_DEFAULT MPE_MEM_HN

#define UNKNOWN_PID_VALUE 65535

// Local Player specific methods
//
static mpe_Error hnPlayer_playbackInit(HnPlayer* player);

static mpe_Error hnPlayer_streamStoreParams(HnPlayer* player,
        mpe_HnStreamParamsMediaPlayerHttp* streamInfo);

static mpe_Error hnPlayer_playbackStoreParams(HnPlayer* player,
        mpe_HnPlaybackParamsMediaPlayerHttp* playbackInfo);

static mpe_Error hnPlayer_connectToHost(HnPlayer* player);

static mpe_Error hnPlayer_gatherServerContentInfo(HnPlayer* player);

static mpe_Error hnPlayer_formulateHttpHeadRequest(HnPlayer* player);

static mpe_Error hnPlayer_formulateHttpGetRequest(HnPlayer* player);

static mpe_Error hnPlayer_handleHttpGetResponse(HnPlayer* player);

static mpe_Error hnPlayer_readHttpResponse(HnPlayer* player, void** response);

static mpe_Error hnPlayer_saveSpecifiedPIDs(HnPlayer* player,
        mpe_HnHttpHeaderAVStreamParameters avStreamParameters);

static mpe_Error hnPlayer_isProfileSupported(char* profileId);

static void hnPlayer_formatPlayspeed(char* requestStr, float rate);

static int hnPlayer_findProfile(char* profileIDStr);

static void hnPlayer_mediaTimesInitialize(HnPlayer* player);


static const char CRLF[] = "\r\n";

// DLNA Profiles supported by this player
static uint32_t g_profilesCnt = 6;

static char* g_playspeeds[] = {"-64", "-32", "-16", "-8", "-4", "-2", "-1", "-0.5", "0.5", "1", "2", "4", "8", "16", "32", "64"};

// NOTE: These should be listed in the platform-preferred order
static hn_player_dlna_profile g_profiles[] = {
        {
                "MPEG_TS_SD_NA_ISO",                // profile id string
                "video/mpeg",                       // mime type
                1,                                  // Frames per second, frame rate in trick mode
                MPE_HN_TRICK_MODE_FRAME_TYPE_ALL,   // Frame type in trick mode, default to all
                16,                                 // playspeeds count
                g_playspeeds                        // playspeed strings
        },
        {
                "DTCP_MPEG_TS_SD_NA_ISO",           // profile id string
                "video/mpeg",                       // mime type
                1,                                  // Frames per second, frame rate in trick mode
                MPE_HN_TRICK_MODE_FRAME_TYPE_ALL,   // Frame type in trick mode, default to all
                16,                                 // playspeeds count
                g_playspeeds                        // playspeed strings
        },
        {
                "MPEG_TS_NA_ISO",                   // profile id string
                "video/mpeg",                       // mime type
                1,                                  // Frames per second, frame rate in trick mode
                MPE_HN_TRICK_MODE_FRAME_TYPE_ALL,   // Frame type in trick mode, default to all
                16,                                 // playspeeds count
                g_playspeeds                        // playspeed strings
        },
        {
                "DTCP_MPEG_TS_NA_ISO",              // profile id string
                "video/mpeg",                       // mime type
                1,                                  // Frames per second, frame rate in trick mode
                MPE_HN_TRICK_MODE_FRAME_TYPE_ALL,   // Frame type in trick mode, default to all
                16,                                 // playspeeds count
                g_playspeeds                        // playspeed strings
        },
        {
                "MPEG_TS_HD_NA_ISO",                // profile id string
                "video/mpeg",                       // mime type
                1,                                  // Frames per second, frame rate in trick mode
                MPE_HN_TRICK_MODE_FRAME_TYPE_ALL,   // Frame type in trick mode, default to all
                16,                                 // playspeeds count
                g_playspeeds                        // playspeed strings
        },
        {
                "DTCP_MPEG_TS_HD_NA_ISO",           // profile id string
                "video/mpeg",                       // mime type
                1,                                  // Frames per second, frame rate in trick mode
                MPE_HN_TRICK_MODE_FRAME_TYPE_ALL,   // Frame type in trick mode, default to all
                16,                                 // playspeeds count
                g_playspeeds                        // playspeed strings
        }
};

// Socket read timeout in milliseconds
static int32_t g_socketReadTimeoutMS = 30000;

/**
 * Initialize the HN Client stream players
 *
 * @return MPE_HN_ERR_NOERR if the player initialization is successful
 * otherwise, MPE_HN_ERR_OS_FAILURE.
 */
mpe_Error hnPlayer_init(HnPlayer* player, ri_pipeline_t* pipeline, uint32_t idx)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    // Initialize our player data structures
    player->playerId = idx + 1;
    player->state = HN_STATE_IDLE;

    if (mpeos_mutexNew(&player->mutex) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - Could not create mutex for player %d\n", __FUNCTION__,
                idx);
        return MPE_HN_ERR_OS_FAILURE;
    }

    if (mpeos_mutexNew(&player->readThreadMutex) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - Could not create read thread mutex for player %d\n", __FUNCTION__,
                idx);
        return MPE_HN_ERR_OS_FAILURE;
    }

    // Allocate playback structure
    if (mpe_memAlloc(sizeof(os_HnPlaybackSession),
            (void**) &player->activePlayback) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s - Cannot allocate new playback structure\n", __FUNCTION__);
        return MPE_HN_ERR_OS_FAILURE;
    }
    player->pipeline = pipeline;
    player->activePlayback->handle = 0;
    player->activePlayback->rate = 0.0;
    player->act = NULL;
    player->pids = NULL;
    player->pid_cnt = 0;
    player->connectionId = 0;
    player->connectionIdStr = NULL;
    player->streamSessionId = 0;
    player->dtcpipSessionHandle = DTCPIP_INVALID_SESSION_HANDLE;
    player->uri = NULL;
    player->host = NULL;
    player->port = 0;
    player->dtcp_host = NULL;
    player->dtcp_port = 0;
    player->httpGetRequestStr = NULL;
    player->httpGetResponseStr = NULL;
    player->httpHeadRequestStr = NULL;
    player->httpHeadResponseStr = NULL;
    player->riVideoDevice = NULL;
    player->requestedRate = 0;
    player->initialMediaTimeNS = 0;
    player->startTimeMS = 0;
    player->endTimeMS = 0;
    player->socket = MPE_SOCKET_INVALID_SOCKET;
    player->isAvailableSeekSupported = FALSE;
    player->isTimeSeekSupported = FALSE;
    player->socketReadTimeoutMS = g_socketReadTimeoutMS;

#ifdef DEBUG_SAVE_RAW_CONTENT
    player->fpRawContent = NULL;
#endif
#ifdef DEBUG_SAVE_ENC_CONTENT
    player->fpEncContent = NULL;
#endif
#ifdef DEBUG_SAVE_CLR_CONTENT
    player->fpClrContent = NULL;
#endif

    if (hnPlayer_httpInit(&player->http_decoder,
            MAX_HTTP_HDR_LEN, MAX_HTTP_CHUNK_HDR_LEN,
            MAX_HTTP_BUF_LEN, MPE_MEM_DEFAULT) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s - Cannot initialize HTTP decoder\n", __FUNCTION__);
        return MPE_HN_ERR_OS_FAILURE;
    }

    return MPE_HN_ERR_NOERR;
}

/**
 * Initialize parameters based on environment variable values
 */
void hnPlayer_initEnv()
{
    const char* configValue = mpeos_envGet("HN.PLAYER.SOCKET.READ.TIMEOUT.MS");
    if (NULL != configValue)
    {
        g_socketReadTimeoutMS = atoi(configValue);
    }
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
         "%s() - socket read timeout in ms: %d\n", __FUNCTION__, g_socketReadTimeoutMS);
}

/**
 * Performs actions necessary to cache the info to begin the HN stream
 * player.
 *
 * @param openParams - allocation and populated streaming params.
 * @param queueId to post streaming related events
 * @param act is a context value for the event dispacher
 * @param streamingSession opaque handle to the streaming session.
 *
 * @event MPE_HN_EVT_SESSION_OPEN for a successfully opened player stream.
 *
 * @return MPE_HN_ERR_NOERR         if successful
 *         MPE_HN_ERR_INVALID_PARAM if a parameter is invalid
 *         MPE_HN_ERR_OS_FAILURE    os specific failures
 *         MPE_HN_ERR_CONTENT       if the failure has occurred while acquiring the content
 */
mpe_Error hnPlayer_streamOpen(HnPlayer* player, uint32_t sessionId,
        mpe_HnStreamParams* openParams, mpe_EventQueue queueId, void* act,
        mpe_HnStreamSession* streamingSession)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called for player %d\n",
            __FUNCTION__, player->playerId);
    mpe_Error ret = MPE_HN_ERR_NOERR;

    // Make sure the platform supports the requested content type
    mpe_HnStreamParamsMediaPlayerHttp* streamInfo = openParams->streamParams;

    mpeos_mutexAcquire(player->mutex);

    // Return session id as the streaming session handle
    player->streamSessionId = sessionId;

    player->queueID = queueId;
    player->act = act;

    // Save the parameters associated with this stream session
    ret = hnPlayer_streamStoreParams(player, streamInfo);
    if (ret != MPE_HN_ERR_NOERR)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - Could not store parameters associated with session\n",
                __FUNCTION__);
        mpeos_mutexRelease(player->mutex);
        return ret;
    }

    // Return current session id as handle
    *streamingSession = (mpe_HnStreamSession)player->streamSessionId;

    // Set player state to open
    player->state = HN_STATE_OPENED;

    // Initialize the stream related parameters
    player->curMediaTimeMS = 0;
    player->prevMediaTimeMS = 0;
    player->segmentOffsetTimeMS = 0;
    player->curBytePos = 0;
    player->holdingFrame = FALSE;

    // Send the stream open event
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - sending player session open event\n", __FUNCTION__);
    mpeos_eventQueueSend(player->queueID, MPE_HN_EVT_SESSION_OPEN, NULL,
            (void*) player->act, 0);

    mpeos_mutexRelease(player->mutex);
    return MPE_HN_ERR_NOERR;
}

/**
 * Allocate and store the parameters associated with the stream session
 * so they can be returned in response to get info call.
 *
 * @param   player      player associated with this stream session
 * @param   streamInfo  player's stream session info to save
 */
mpe_Error hnPlayer_streamStoreParams(HnPlayer* player,
        mpe_HnStreamParamsMediaPlayerHttp* streamInfo)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    // Store connection id as number and string
    if (streamInfo->connectionId > 0)
    {
        player->connectionId = streamInfo->connectionId;
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                "%s() -- received connection id was %d\n", __FUNCTION__,
                player->connectionId);

        // Save the connection Id as a string also
        char tmpStr[64];
        sprintf(&tmpStr[0], "%u", player->connectionId);
        int size = strlen((char*) &tmpStr[0]);
        if (mpe_memAlloc(size+1, (void **) &player->connectionIdStr)
                != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s() - Could not allocated memory for connection id string\n",
                    __FUNCTION__);
            mpeos_mutexRelease(player->mutex);
            return MPE_HN_ERR_OS_FAILURE;
        }
        else
        {
            strcpy(player->connectionIdStr, (char*) &tmpStr[0]);
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                    "%s() - connection id %d, len %d, string: %s\n",
                    __FUNCTION__, player->connectionId, size, player->connectionIdStr);
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                "%s() -- leaving connection id unset at %d\n", __FUNCTION__,
                player->connectionId);
    }

    //profileID may be null
    // *TODO* - OCORI-4733 reverted MPEOS change
    // if (NULL == streamInfo->protocolInfo.fourthField.pn_param.dlnaProfileId)
    if (NULL == streamInfo->dlnaProfileId)
    {
        strncpy(player->dlnaProfileId, (char*)&g_profiles[0].profileId[0], MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE);                 
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() -- null DLNA profileID str - using default (index zero): %s\n",
                 __FUNCTION__, player->dlnaProfileId);
    }
    else
    {
        // Make sure this DLNA profile is supported
        // *TODO* - OCORI-4733 reverted MPEOS change
        // if (hnPlayer_isProfileSupported(streamInfo->protocolInfo.fourthField.pn_param.dlnaProfileId) != MPE_HN_ERR_NOERR)
        if (hnPlayer_isProfileSupported(streamInfo->dlnaProfileId) != MPE_HN_ERR_NOERR)
        {
             MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                     "%s() - Supplied profile id is not supported by player: %s\n",
                     __FUNCTION__, streamInfo->dlnaProfileId);
             // *TODO* - OCORI-4733 reverted MPEOS change
             //      __FUNCTION__, streamInfo->protocolInfo.fourthField.pn_param.dlnaProfileId);
             mpeos_mutexRelease(player->mutex);
             return MPE_HN_ERR_OS_FAILURE;
        }
        // *TODO* - OCORI-4733 reverted MPEOS change
        //strncpy(player->dlnaProfileId, streamInfo->protocolInfo.fourthField.pn_param.dlnaProfileId,
        //        OS_HN_MAX_DLNA_PROFILE_ID_STR_SIZE);
        strncpy(player->dlnaProfileId, streamInfo->dlnaProfileId, OS_HN_MAX_DLNA_PROFILE_ID_STR_SIZE);

        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() -- stored DLNA profileID str %s\n",
                 __FUNCTION__, player->dlnaProfileId);
    }


    // Allocate & store URI
    if (NULL != streamInfo->uri)
    {
        if (mpe_memAlloc((strlen(streamInfo->uri) + 1),
                 (void **)&player->uri) != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                     "%s() - Could not allocated memory for URI Host string\n", __FUNCTION__);
            return MPE_HN_ERR_OS_FAILURE;
        }
        else
        {
            strcpy(player->uri, streamInfo->uri);
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - No URI provided for stream session\n",__FUNCTION__);
        mpeos_mutexRelease(player->mutex);
        return MPE_HN_ERR_OS_FAILURE;
    }
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() -- stored URI str %s\n",
            __FUNCTION__, player->uri);

    // Allocate & store URI Host String
    if (NULL != streamInfo->host)
    {
        if (mpe_memAlloc((strlen(streamInfo->host) + 1),
                 (void **) &player->host) != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                     "%s() - Could not allocated memory for URI Host string\n",
                     __FUNCTION__);
            return MPE_HN_ERR_OS_FAILURE;
        }
        else
        {
            strcpy(player->host, streamInfo->host);
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - No host provided for stream session\n",__FUNCTION__);
        mpeos_mutexRelease(player->mutex);
        return MPE_HN_ERR_OS_FAILURE;
    }
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() -- stored host str %s\n",
            __FUNCTION__, player->host);

    // Store supplied port value
    player->port = streamInfo->port;
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - port: %d\n", __FUNCTION__, player->port);

    // Allocate & store DTCP URI Host String
    // *TODO* - OCORI-4733 reverted MPEOS change
    // if (NULL != streamInfo->protocolInfo.dtcpHost)
    if (NULL != streamInfo->dtcp_host)
    {
        // *TODO* - OCORI-4733 reverted MPEOS change
        // if (mpe_memAlloc((strlen(streamInfo->protocolInfo.dtcpHost) + 1),
        //        (void **)&player->dtcp_host) != MPE_SUCCESS)
        if (mpe_memAlloc((strlen(streamInfo->dtcp_host) + 1),
                (void **)&player->dtcp_host) != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s() - Could not allocated memory for DTCP URI Host string\n", __FUNCTION__);
            return MPE_HN_ERR_OS_FAILURE;
        }
        else
        {
            // *TODO* - OCORI-4733 reverted MPEOS change
            // strcpy(player->dtcp_host, streamInfo->protocolInfo.dtcpHost);
            strcpy(player->dtcp_host, streamInfo->dtcp_host);
        }
     }
     else
     {
         player->dtcp_host = NULL;
     }

    // Store supplied DTCP port value
    // *TODO* - OCORI-4733 reverted MPEOS change
    //player->dtcp_port = streamInfo->protocolInfo.dtcpPort;
    player->dtcp_port = streamInfo->dtcp_port;
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - dtcp port: %d\n", __FUNCTION__, player->dtcp_port);

    // Store content item protocol info
    // *TODO* - OCORI-4733 reverted MPEOS change
    // player->isTimeSeekSupported = streamInfo->protocolInfo.fourthField.op_param.isTimeSeekSupported;

    // *TODO* - OCORI-4733 reverted MPEOS change
    //if ((streamInfo->protocolInfo.fourthField.flags_param.isLimitedTimeSeekSupported) ||
    //    (streamInfo->protocolInfo.fourthField.flags_param.isLimitedByteSeekSupported))
    //{
    //    player->isAvailableSeekSupported = TRUE;
    //}

    return MPE_HN_ERR_NOERR;
}

/**
 * Returns the current session parameters
 *
 * @param player        player to retrieve info about
 * @param streamParams  info about the current stream of supplied player
 *
 * @events
 *    None.
 *
 * @return MPE_HN_ERR_NOERR         if successful
 *         MPE_HN_ERR_INVALID_PARAM if a parameter is invalid
 *         MPE_HN_ERR_OS_FAILURE    os specific failures
 */
mpe_Error hnPlayer_streamGetInfo(HnPlayer* player,
        mpe_HnStreamParams* sessionParams)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    mpeos_mutexAcquire(player->mutex);

    sessionParams->requestType = MPE_HNSTREAM_MEDIA_PLAYER_HTTP;
    mpe_HnStreamParamsMediaPlayerHttp* streamParams = (mpe_HnStreamParamsMediaPlayerHttp*)sessionParams->streamParams;
    streamParams->connectionId = player->connectionId;
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - returning connection id: %d\n", __FUNCTION__,
            streamParams->connectionId);

    mpeos_mutexRelease(player->mutex);

    return MPE_HN_ERR_NOERR;
}

/**
 * Start HN player decode...
 *
 * @param   player   stream associated with this player
 * @return  boolean result of decode start
 */
static mpe_Bool hnPlayer_startDecode(HnPlayer* player)
{
    mpe_Bool started = FALSE;

    // If the PIDs are known...
    if ((NULL != player->pids) && (player->pid_cnt > 0))
    {
        // and video device was obtained...
        if (NULL != player->riVideoDevice)
        {
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                "%s() - got video device & pids, decoding...\n", __func__);
            // start pipeline decode
            player->pipeline->decode(player->pipeline,
                                     player->riVideoDevice,
                                     player->pids,
                                     player->pid_cnt);
            started = TRUE;

            // Set the desired play rate in the pipeline to utilize
            // ESAssembler timestamping
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                "%s() - setting rate for video device...\n", __FUNCTION__);
            (void) player->pipeline->pipeline_hn_player_set_rate(
                                     player->pipeline,
                                     player->riVideoDevice,
                                     player->activePlayback->rate);
        }
        else
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - not decoding due to NULL video device\n", __func__);
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
            "%s() - not decoding due to null PIDs\n", __func__);
    }

    return started;
}

/**
 * This function creates and starts a new HN playback for a given hn streaming session.
 * A new playback handle is returned if the call is successful.  The returned handle can
 * be used to control the speed, the direction and the position of the play back in the stream.
 *
 * @param player                 player associated with this specific session
 * @param playbackParams         the stream parameter changes for this playback.
 * @param mediaTime              initial playback time (in nanoseconds)
 * @param rate                   initial rate
 * @param initialBlockingState   designates the whether the video is initially considered blocked.
 * @param act                    the completion token for async events
 * @param playbackSession        pointer to a HN session playback handle. This handle is used
 *                               to control play rate and media positions.
 *
 * @events   MPE_HN_EVT_PLAYBACK_START signaling that the player is in a started playback state.
 *
 * @return MPE_HN_ERR_NOERR         if successful
 *         MPE_HN_ERR_INVALID_PARAM if a parameter is invalid
 *         MPE_HN_ERR_OS_FAILURE    os specific failures
 *         MPE_HN_ERR_CONTENT       if the failure has occured while acquiring the content
 */
mpe_Error hnPlayer_playbackStart(HnPlayer* player, uint32_t sessionId,
        mpe_HnPlaybackParams* playbackParams, void *act,
        mpe_HnPlaybackSession* playbackSession)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called for player %d\n",
            __FUNCTION__, player->playerId);

    mpe_Error ret = MPE_HN_ERR_OS_FAILURE;
    os_HnPlaybackSession* newPlayback;

    // Verify supplied parameters are not null
    mpe_HnPlaybackParamsMediaPlayerHttp* playerParams = playbackParams->playbackParams;
    if (NULL == playerParams)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - player playback params were NULL\n", __FUNCTION__);
        return MPE_HN_ERR_INVALID_PARAM;
    }

    // Acquire mutex while modifying player
    mpeos_mutexAcquire(player->mutex);

    // Initialize session parameters to starting values
    player->activePlayback->handle = 0;
    player->activePlayback->rate = 0.0;
    player->readThread = NULL;

    // Store parameters for this playback session
    ret = hnPlayer_playbackStoreParams(player, playerParams);
    if (MPE_HN_ERR_NOERR != ret)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - problems storing player playback params\n", __FUNCTION__);
        mpeos_mutexRelease(player->mutex);
        return ret;
    }

    // Initialize this playback session
    ret = hnPlayer_playbackInit(player);
    if (MPE_HN_ERR_NOERR != ret)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - problems initializing player playback\n", __FUNCTION__);
        mpeos_mutexRelease(player->mutex);
        return ret;
    }

    // Use any optional video & audio pids where specified in playback params
    ret = hnPlayer_saveSpecifiedPIDs(player, playerParams->avStreamParameters);
    if (MPE_HN_ERR_NOERR != ret)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - problems storing supplied PIDs from playback params\n",
                __FUNCTION__);
        mpeos_mutexRelease(player->mutex);
        return ret;
    }

    // Setup the callback function which is used by pipeline to request more data
    player->hnPlayer_needDataCB = hnPlayer_needDataCB;

    // *TODO* - OCORI-4558 remove decode cb from method

    // Only need to start pipeline if rate is not zero
    if (player->requestedRate != 0.0)
    {
        // If holding frame, stop pipeline prior to restarting
        if (player->holdingFrame)
        {
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                    "%s() - stopping pipeline to clear holding frame\n", __FUNCTION__);
            player->pipeline->pipeline_hn_player_stop(player->pipeline);
            player->holdingFrame = FALSE;
        }

        // Start the pipeline supplying the decode callback function
        (void) player->pipeline->pipeline_hn_player_start(player->pipeline,
                NULL, player->hnPlayer_needDataCB,
                (void*) player);

        if (hnPlayer_startDecode(player))
        {
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                      "%s() decode started\n", __func__);
        }

        // Make sure the exit and paused flags are cleared since starting new thread
        player->holdingFrame = FALSE;
        player->readThreadState = THREAD_STATE_INACTIVE;

        // Spawn a thread to handle stream
        if (mpeos_threadCreate(hnPlayerReadThread_readData, // method which is entry point
                (void*) player, // data to pass to method
#ifdef INCREASE_HN_PLAYER_THREAD_PRIORITY
                MPE_THREAD_PRIOR_SYSTEM, // thread priority
#else
                MPE_THREAD_PRIOR_DFLT, // thread priority
#endif
                0, // stack size
                &player->readThread, // ptr to created thread
                "HN Player Stream Thread") // thread name
                != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s() = Could not create player stream thread!\n", __FUNCTION__);
            mpeos_mutexRelease(player->mutex);
            return MPE_HN_ERR_OS_FAILURE;
        }
        else
        {
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                    "%s() - player stream thread created\n", __FUNCTION__);
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                 "%s() - player stream thread not started due to rate = 0\n", __FUNCTION__);
    }

    // Assign handle
    mpeos_mutexRelease(player->mutex);
    player->activePlayback->handle = sessionId;
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - set playback session id to %d\n",
            __FUNCTION__, player->activePlayback->handle);

    // Send the play back start event
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
            "%s() - sending event that playback has started, event %d\n",
            __FUNCTION__, MPE_HN_EVT_PLAYBACK_START);
    mpeos_eventQueueSend(player->queueID, MPE_HN_EVT_PLAYBACK_START, (void*)MPE_PRESENTING_2D_SUCCESS,
            (void*) player->act, 0);

    // Allocate structure to return as playback session data
    if (mpe_memAlloc(sizeof(os_HnPlaybackSession), (void**) &newPlayback) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - Could not allocate memory for playback session data\n", __FUNCTION__);
        return MPE_HN_ERR_OS_FAILURE;
    }

    // Set return values
    newPlayback->handle = player->activePlayback->handle;
    newPlayback->rate = player->activePlayback->rate;
    *playbackSession = (mpe_HnPlaybackSession) newPlayback;

    return MPE_HN_ERR_NOERR;

}

/**
 * Allocate and store the parameters associated with playback.
 *
 * @param   server      server associated with this stream session
 * @param   streamInfo  server's playback info to save
 *
 * @return  MPE_HN_ERR_NOERR if successful
 *          otherwise MPE_HN_ERR_OS_FAILURE
 */
static mpe_Error hnPlayer_playbackStoreParams(HnPlayer* player,
        mpe_HnPlaybackParamsMediaPlayerHttp* playbackInfo)
{
    mpe_Error ret = MPE_HN_ERR_NOERR;

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    // Store playback param values

    // Use any optional video & audio pids where specified in playback params
    ret = hnPlayer_saveSpecifiedPIDs(player, playbackInfo->avStreamParameters);
    if (MPE_HN_ERR_NOERR != ret)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - problems storing supplied PIDs from playback params\n",
                __FUNCTION__);
        return ret;
    }

    // Get video device if value was supplied
    if (playbackInfo->videoDevice != NULL)
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                 "%s() - Getting video device %d\n", __FUNCTION__, playbackInfo->videoDevice);
        player->riVideoDevice = dispGetVideoDevice(playbackInfo->videoDevice);
    }
    else
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                 "%s() - Not getting video device, null device supplied\n", __FUNCTION__);
    }

    player->requestedRate = playbackInfo->requestedRate;
    player->initialMediaTimeNS = playbackInfo->initialMediaTimeNS;

    return ret;
}

/**
 * Perform the actions necessary to initialize a playback at the specified rate.
 *
 * @param   player   initialize this player
 * @param   rate     rate to use for this playback sesssion
 *
 * @return  MPE_HN_ERR_NOERR if no problems were encountered,
 *          MPE_HN_ERR_OS_FAILURE otherwise
 */
static mpe_Error hnPlayer_playbackInit(HnPlayer* player)
{
    mpe_Error ret = MPE_HN_ERR_OS_FAILURE;

    player->dtcpipSessionHandle = DTCPIP_INVALID_SESSION_HANDLE;
    if (player->dlnaProfileId[0] == 'D' &&
        player->dlnaProfileId[1] == 'T' &&
        player->dlnaProfileId[2] == 'C' &&
        player->dlnaProfileId[3] == 'P' &&
        player->dlnaProfileId[4] == '_')
    {
        unsigned short dtcp_port = (unsigned short) player->dtcp_port;
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s - Opening DTCP/IP AKE with "
                "%s:%hu\n", __FUNCTION__, player->dtcp_host, player->dtcp_port);

        int dtcpip_ret = g_dtcpip_ftable->dtcpip_snk_open(player->dtcp_host,
                dtcp_port, &player->dtcpipSessionHandle);
        if (dtcpip_ret != 0)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s - dtcpip_snk_open failed with %d\n", __FUNCTION__, dtcpip_ret);
            mpeos_mutexRelease(player->mutex);
            return MPE_HN_ERR_CONTENT;
        }

        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                "%s - dtcpip_snk_open returned session handle is %d\n",
                __FUNCTION__, player->dtcpipSessionHandle);
    }

    // Connect to HTTP Server
    ret = hnPlayer_connectToHost(player);
    if (MPE_HN_ERR_NOERR != ret)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - problems connecting to host\n", __FUNCTION__);
        return ret;
    }

#ifdef DEBUG_SAVE_RAW_CONTENT
    {
        char tmpBuf[64];

        sprintf(tmpBuf, "hn_player_%02d_session_%010u_raw_content.bin",
            player->playerId, player->streamSessionId);
        player->fpRawContent = fopen(tmpBuf, "wb");
    }
#endif

#ifdef DEBUG_SAVE_ENC_CONTENT
    if (player->dtcpipSessionHandle != DTCPIP_INVALID_SESSION_HANDLE)
    {
        char tmpBuf[64];

        sprintf(tmpBuf, "hn_player_%02d_session_%010u_enc_content.bin",
            player->playerId, player->streamSessionId);
        player->fpEncContent = fopen(tmpBuf, "wb");
    }
#endif

#ifdef DEBUG_SAVE_CLR_CONTENT
    {
        char tmpBuf[64];

        sprintf(tmpBuf, "hn_player_%02d_session_%010u_clr_content.bin",
            player->playerId, player->streamSessionId);
        player->fpClrContent = fopen(tmpBuf, "wb");
    }
#endif

    // Issue HEAD request with content features in order to determine supported header
    // in GET request
    // Send HEAD request to determine content info and server capabilities
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN,
        "%s() - about to gather server content info\n", __FUNCTION__);

    ret = hnPlayer_gatherServerContentInfo(player);
    if (MPE_HN_ERR_NOERR != ret)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - problems with request\n", __FUNCTION__);
        return ret;
    }

    if (player->http_decoder.close_conn)
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - re-connecting to host\n", __FUNCTION__);
        // close current conn and reconnect to host
        ret = hnPlayer_closeSocket(player);
        if (MPE_HN_ERR_NOERR != ret)
        {
            // log error but ignore it since nothing can be done
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s() - problems connecting to host\n", __FUNCTION__);
        }

        // Connect to HTTP Server
        ret = hnPlayer_connectToHost(player);
        if (MPE_HN_ERR_NOERR != ret)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s() - problems connecting to host\n", __FUNCTION__);
            return ret;
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s() - player->http_decoder.close_conn not true\n", __FUNCTION__);
    }

    // Formulate an GET HTTP Request
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN,
        "%s() - about to formulate GET request\n", __FUNCTION__);
    ret = hnPlayer_formulateHttpGetRequest(player);
    if (MPE_HN_ERR_NOERR != ret)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - problems with request\n", __FUNCTION__);
        return ret;
    }

    // Send the request on the socket - send string length bytes, not length + 1
    int requestSize = strlen(player->httpGetRequestStr);

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN,
        "%s() - about to send GET request on socket\n", __FUNCTION__);
    // Don't have to acquire lock here since already acquired in playbackStart, setRate & setTime
    if (mpeos_socketSend(player->socket, player->httpGetRequestStr, requestSize, 0)
            == (size_t) - 1)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - problems sending request on socket\n", __FUNCTION__);

        return MPE_HN_ERR_OS_FAILURE;
    }
    else
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                "%s() - sent request on socket %d\n", __FUNCTION__, player->socket);
    }

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN,
        "%s() - about to initialize mediatimes\n", __FUNCTION__);
    // Initialize media time parameters
    hnPlayer_mediaTimesInitialize(player);

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN,
        "%s() - about to read http response\n", __FUNCTION__);
    ret = hnPlayer_readHttpResponse(player, (void**)&player->httpGetResponseStr);
    if (MPE_HN_ERR_NOERR != ret)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - problems reading GET response\n", __FUNCTION__);
        return ret;
    }

    ret = hnPlayer_handleHttpGetResponse(player);
    if (MPE_HN_ERR_NOERR != ret)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - invalid response received from server\n", __FUNCTION__);
        return ret;
    }

    // Set player state to playing
    player->state = HN_STATE_PLAYING;
    player->activePlayback->rate = player->requestedRate;

    return MPE_HN_ERR_NOERR;
}

/**
 * Reads the HTTP response received on socket from either HEAD or GET request.
 *
 * @param   player      HTTP response associated player
 * @param   response    store copy of received response if not null
 *
 * @return  MPE_HN_ERR_NOERR if successful, MPE_HN_ERR_OS_FAILURE if problems encountered
 */
static mpe_Error hnPlayer_readHttpResponse(HnPlayer* player, void** response)
{
    mpe_Error ret = MPE_HN_ERR_NOERR;

    uint32_t socketRead = 0;
    uint32_t decoderRead = 0;
    uint32_t decoderWritten = 0;

    uint8_t inBufData[MAXDATASIZE];
    uint8_t outBufData[MAX_HTTP_BUF_LEN];
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN,
        "%s() - about to reset http headers\n", __FUNCTION__);

    hnPlayer_httpReset(&player->http_decoder);
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN,
        "%s() - about to read from socket\n", __FUNCTION__);

    // Read the HTTP response received from server
    // until we have at least the HTTP header
    do
    {
        socketRead = mpeos_socketRecv(player->socket, inBufData, MAXDATASIZE, 0);
        if (socketRead == -1)
        {
            ret = MPE_HN_ERR_OS_FAILURE;
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - problems reading data from socket %u\n", __FUNCTION__, player->socket);
        }
        else
        {
#ifdef DEBUG_SAVE_RAW_CONTENT
            if (fwrite(inBufData, 1, socketRead, player->fpRawContent) != socketRead)
            {
                MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_HN,
                    "%s() - failed to save raw content for session %010u\n", __FUNCTION__, player->streamSessionId);
            }
#endif
        MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN,
            "%s() - about to call httpdecode - socketRead: %u\n", __FUNCTION__, socketRead);

            if (MPE_HN_ERR_NOERR != hnPlayer_httpDecode(&player->http_decoder,
                socketRead, inBufData, &decoderRead,
                MAX_HTTP_BUF_LEN, outBufData, &decoderWritten))
            {
                ret = MPE_HN_ERR_OS_FAILURE;
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s() - problems reading HTTP header from server response\n", __FUNCTION__);
            }
            else if (decoderWritten > 0)
            {
                ret = MPE_HN_ERR_OS_FAILURE;
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                        "%s() - unexpected buffer write while assembling HTTP header\n", __FUNCTION__);
#ifdef DEBUG_SAVE_ENC_CONTENT
                if (player->dtcpipSessionHandle != DTCPIP_INVALID_SESSION_HANDLE)
                {
                    if (fwrite(outBufData, 1, decoderWritten, player->fpEncContent) != decoderWritten)
                    {
                        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_HN,
                                "%s() - failed to save encrypted content for session %010u\n", __FUNCTION__, player->streamSessionId);
                    }
                }
    #ifdef DEBUG_SAVE_CLR_CONTENT
                else
    #endif
#endif
#ifdef DEBUG_SAVE_CLR_CONTENT
                if (fwrite(outBufData, 1, decoderWritten, player->fpClrContent) != decoderWritten)
                {
                     MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_HN,
                            "%s() - failed to save content for session %010u\n", __FUNCTION__, player->streamSessionId);
                }
#endif
            }
        }
    }
    while (player->http_decoder.state < HTTP_HEADER_COMPLETE && ret != MPE_HN_ERR_NOERR);

    if (ret != MPE_HN_ERR_NOERR)
    {
#ifdef DEBUG_SAVE_RAW_CONTENT
        fclose(player->fpRawContent);
        player->fpRawContent = NULL;
#endif
#ifdef DEBUG_SAVE_ENC_CONTENT
        fclose(player->fpEncContent);
        player->fpEncContent = NULL;
#endif
#ifdef DEBUG_SAVE_CLR_CONTENT
        fclose(player->fpClrContent);
        player->fpClrContent = NULL;
#endif
    }

    // Store HTTP response string received
    if (mpe_memAlloc(player->http_decoder.header_size, response)
            != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - Could not allocate %d bytes of memory for response string\n",
                __FUNCTION__, player->http_decoder.header_size);
        return MPE_HN_ERR_OS_FAILURE;
    }
    else
    {
        memcpy((char*)*response, player->http_decoder.header_data, player->http_decoder.header_size);
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                "%s() - response received: %s\n", __FUNCTION__, *response);
    }

    return ret;
}

static mpe_Error hnPlayer_isProfileSupported(char* profileID)
{
    //mpe_Error ret = MPE_HN_ERR_OS_FAILURE;

    // *TODO* - OCORI-4707 implement this logic

    return MPE_HN_ERR_NOERR;
}

/**
 * Connect to HTTP Server host in order to get socket to send HTTP GET request
 *
 * @param   player   player which will connect to host
 *
 * @return MPE_HN_ERR_NOERR         if successful
 *         MPE_HN_ERR_OS_FAILURE    os specific failures
 */
static mpe_Error hnPlayer_connectToHost(HnPlayer* player)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    // Don't have to acquire lock here since already acquired in playbackInit()

    // Create socket
    player->socket = mpeos_socketCreate(AF_INET, // domain
            SOCK_STREAM, // type ? SOCK_STREAM or DGRAM?
            0); // protocol
    if (player->socket == MPE_SOCKET_INVALID_SOCKET)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - failed to create socket\n", __FUNCTION__);
        return MPE_HN_ERR_OS_FAILURE;
    }

    // Formulate host and port number to use
    (void) memset((uint8_t *)&player->hostAddr, 0, sizeof(player->hostAddr));
    player->hostAddr.sin_family = AF_INET;

    int status = mpeos_socketAtoN(player->host, &(player->hostAddr.sin_addr));
    if (status == 0)
    {
        // invalid or unknown host name
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - invalid or unknown host: %s\n", __FUNCTION__, player->host);
        return MPE_HN_ERR_OS_FAILURE;
    }

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - port number = %u\n",
            __FUNCTION__, player->port);
    player->hostAddr.sin_port = htons((uint16_t)player->port);

    if (mpeos_socketSetDLNAQOS(player->socket, MPE_SOCKET_DLNA_QOS_2) != MPE_SUCCESS)
    {
        // non fatal error
	MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
		"%s() - error failed to set QOS on socket\n", __FUNCTION__);
    }
#ifdef INCREASE_SOCKET_RCV_BUF_SIZE
    {
        // Increase buffer limit so server doesn't drop buffers when the client
        // isn't picking up buffers fast enough
        int bufSize = (900 * 1024);      // roughly the max on Linux

        if (mpeos_socketSetOpt(player->socket, MPE_SOCKET_SOL_SOCKET,
                               MPE_SOCKET_SO_RCVBUF,
                               (const void *)&bufSize, sizeof(bufSize)) != 0)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
            "%s() - Unable to set socket send buffer size option, "
            "error code = %d\n", __FUNCTION__, mpeos_socketGetLastError());
            return MPE_HN_ERR_OS_FAILURE;
        }
        else
        {
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                    "%s set eplayer recv buffer size: %d\n", __func__, bufSize);
        }
    }
#endif
    // Connect to supplied host
    status = mpeos_socketConnect(player->socket,
            (mpe_SocketSockAddr*)&player->hostAddr, sizeof(player->hostAddr));
    if (status != 0)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - error failed to connect socket\n", __FUNCTION__);
        return MPE_HN_ERR_OS_FAILURE;
    }
    else
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - connected on socket %d\n",
                __FUNCTION__, player->socket);
    }

    return MPE_HN_ERR_NOERR;
}

/**
 * Form the HTTP GET request to send to HTTP Server
 *
 * @param   player   player handling HTTP request
 */
static mpe_Error hnPlayer_formulateHttpGetRequest(HnPlayer* player)
{
    char tmpStr[32];
    char requestStr[MAXDATASIZE];
    float startNpt;
    int64_t endTimeMS;
    float endNpt;

    // Used for testing server side only
    //int64_t endBytePos = 0;
    strcpy(requestStr, "GET ");

    strcat(requestStr, player->uri);

    strcat(requestStr, " HTTP/1.1");
    strcat(requestStr, CRLF);

    strcat(requestStr, "HOST: ");
    strcat(requestStr, player->host);
    strcat(requestStr, ":");
    (void) memset((char *)&tmpStr, 0, sizeof(tmpStr));
    sprintf(tmpStr, "%d", player->port);
    strcat(requestStr, tmpStr);
    strcat(requestStr, CRLF);

    // Only include the connection id if it is valid
    if ((player->connectionId > 0) && (player->connectionIdStr != NULL))
    {
        strcat(requestStr, "Scid.dlna.org : ");
        strcat(requestStr, player->connectionIdStr);
        strcat(requestStr, CRLF);
    }

    // Convert rate which is a float into integer and/or fractional value
    if (player->requestedRate != 1)
    {
        strcat(requestStr, "PlaySpeed.dlna.org : speed = ");
        hnPlayer_formatPlayspeed(requestStr, player->requestedRate);
    }

    // Always include this to satisfy DLNA requirements
    strcat(requestStr, "transferMode.dlna.org : Streaming");
    strcat(requestStr, CRLF);

    // Always include request to get content features
    strcat(requestStr, "getcontentFeatures.dlna.org : 1");
    strcat(requestStr, CRLF);

    // Only include available seek range if supported
    if (player->isAvailableSeekSupported)
    {
        strcat(requestStr, "getAvailableSeekRange.dlna.org : 1");
        strcat(requestStr, CRLF);
    }

    // Include the time seek range header if time was supplied was not zero
    // and time seek range is supported
    if ((player->initialMediaTimeNS != -1) && (player->isTimeSeekSupported))
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                "%s() - including time seek range due to specified media time: %"PRIi64" NS\n",
                __FUNCTION__, player->initialMediaTimeNS);

        // Encode the header text
        strcat(requestStr, "TimeSeekRange.dlna.org : npt=");

        // Convert ms into s.mmm format
        int64_t mediaTimeMS = player->initialMediaTimeNS / NANO_MSECS;
        startNpt = (float)mediaTimeMS / (float)1000;

        // Convert numeric value into string follow with a trailing '-'
        sprintf(tmpStr, "%1.3f-", startNpt);

        // Encode value into header
        strcat(requestStr, tmpStr);

        // Determine what end time value should be used
        if (player->requestedRate != 1.0)
        {
            // Need to include end time
            if (player->requestedRate < 0)
            {
                // Rewinding so start time will be larger, end time smaller
                endTimeMS = player->startTimeMS;
            }
            else
            {
                // Going forward so start time will be smaller, end time larger
                endTimeMS = player->endTimeMS;
            }

            // Convert numeric value into string follow with a trailing '-'
            endNpt = (float)endTimeMS / (float)1000;
            sprintf(tmpStr, "%1.3f", endNpt);

            // Encode value into header
            strcat(requestStr, tmpStr);
        }

        // Encode final crlf
        strcat(requestStr, CRLF);
    }

    // Add additional fields if requesting trick mode (non-1x playback)
    if ((1.0 != player->requestedRate) && (0.0 != player->requestedRate))
    {
        // Set the flag to indicate that this player is requesting chunk encoding
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                "%s() - setting chunk encoding to TRUE\n", __FUNCTION__);

        // For this implementation, the only type supported is GOP.  This is because the
        // Server will send out only I-frames in trick mode, "chunk=GOP" means each chunk be made of
        // one I-frame only.
        strcat(requestStr, "ChunkEncodingMode.ochn.org : chunk=GOP");
        strcat(requestStr, CRLF);

        // Set the max GOPs per chunk to 1
        strcat(requestStr, "MaxGOPsPerChunk.ochn.org : gops=1");
        strcat(requestStr, CRLF);

        // Set the FrameTypesInTrickMode=All
        strcat(requestStr, "FrameTypesInTrickMode.ochn.org : frames=I");
        strcat(requestStr, CRLF);

        // Include the FrameRateInTrickMode field with no value
        strcat(requestStr, "FrameRateInTrickMode.ochn.org");
        strcat(requestStr, CRLF);
    }

    // Add termination characters
    strcat(requestStr, "\r\n");

    // Request memory to store request
    int requestSize = strlen(requestStr) + 1;

    if (mpe_memAlloc(requestSize, (void **) &player->httpGetRequestStr)
            != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - Could not allocate %d bytes of memory for request string\n",
                __FUNCTION__, requestSize);
        return MPE_HN_ERR_OS_FAILURE;
    }
    else
    {
        memcpy(player->httpGetRequestStr, requestStr, requestSize);
    }

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - request '%s'\n", __FUNCTION__,
            player->httpGetRequestStr);

    return MPE_HN_ERR_NOERR;
}

/**
 *
 */
static mpe_Error hnPlayer_gatherServerContentInfo(HnPlayer* player)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    mpe_Error ret = MPE_HN_ERR_NOERR;
    int64_t initialMediaTimeMS = player->initialMediaTimeNS / NANO_MSECS;

    // Formulate a head request for content feature
    ret = hnPlayer_formulateHttpHeadRequest(player);
    if (MPE_HN_ERR_NOERR != ret)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - problems with HEAD request\n", __FUNCTION__);
        return ret;
    }

    // Send the head request on the socket - send string length bytes, not length + 1
    int requestSize = strlen(player->httpHeadRequestStr);

    // Don't have to acquire lock here since already acquired in playbackStart, setRate & setTime
    if (mpeos_socketSend(player->socket, player->httpHeadRequestStr, requestSize, 0)
            == (size_t) - 1)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - problems sending HEAD request on socket\n", __FUNCTION__);

        return MPE_HN_ERR_OS_FAILURE;
    }
    else
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                "%s() - sent HEAD request on socket %d\n", __FUNCTION__, player->socket);
    }

    // Read head request response
    ret = hnPlayer_readHttpResponse(player, (void**)&player->httpHeadResponseStr);
    if (MPE_HN_ERR_NOERR != ret)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - problems reading HEAD response\n", __FUNCTION__);
        return ret;
    }

    // Look for successful reply, anything in the 200 realm is considered successful
    if (player->http_decoder.header_status_code < 200 ||
                player->http_decoder.header_status_code > 299)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s() - Received non-successful HEAD HTTP response: '%s', status %u\n",
                    __FUNCTION__, player->http_decoder.header_data, player->http_decoder.header_status_code);
        return MPE_HN_ERR_OS_FAILURE;
    }

    // Look for available seek range if supported
    if (player->isAvailableSeekSupported)
    {
        // *TODO* - OCORI-4733 Temporary workaround for MPEOS reverted change
        player->isAvailableSeekSupported = FALSE;

        if (player->http_decoder.header_available_start_time_ms != INVALID_MEDIA_TIME_MS)
        {
            // *TODO* - OCORI-4733 Temporary workaround for MPEOS reverted change
            player->isAvailableSeekSupported = TRUE;

            player->startTimeMS = player->http_decoder.header_available_start_time_ms;
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                    "%s() - response had available start media time: %"PRIi64" ms\n",
                    __FUNCTION__, player->startTimeMS);
        }
        else
        {
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN,
                    "%s() - response did not have a AvailableStartSeekRange\n", __FUNCTION__);
        }

        if (player->http_decoder.header_available_end_time_ms != INVALID_MEDIA_TIME_MS)
        {
            // *TODO* - OCORI-4733 Temporary workaround for MPEOS reverted change
            player->isAvailableSeekSupported = TRUE;

            player->endTimeMS = player->http_decoder.header_available_end_time_ms;
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                    "%s() - response had available end media time: %"PRIi64" ms\n",
                    __FUNCTION__, player->endTimeMS);
        }
        else
        {
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN,
                    "%s() - response did not have a AvailableEndSeekRange\n", __FUNCTION__);
        }
    }

    // Look for time seek range if supported
    if (player->isTimeSeekSupported)
    {
        // *TODO* - OCORI-4733 Temporary workaround for MPEOS reverted change
        player->isTimeSeekSupported = FALSE;

        if (player->http_decoder.header_time_seek_start_time_ms != INVALID_MEDIA_TIME_MS)
        {
            // *TODO* - OCORI-4733 Temporary workaround for MPEOS reverted change
            player->isTimeSeekSupported = TRUE;

            player->startTimeMS = player->http_decoder.header_time_seek_start_time_ms;
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                    "%s() - response had time seek start media time: %"PRIi64" ms\n",
                    __FUNCTION__, player->startTimeMS);
        }
        else
        {
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN,
                    "%s() - response did not have a TimeSeekRange start\n", __FUNCTION__);
        }

        if (player->http_decoder.header_time_seek_end_time_ms != INVALID_MEDIA_TIME_MS)
        {
            // *TODO* - OCORI-4733 Temporary workaround for MPEOS reverted change
            player->isTimeSeekSupported = TRUE;

            player->endTimeMS = player->http_decoder.header_time_seek_end_time_ms;
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                    "%s() - response had time seek end media time: %"PRIi64" ms\n",
                    __FUNCTION__, player->endTimeMS);
        }
        else
        {
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN,
                    "%s() - response did not have a TimeSeekRange end\n", __FUNCTION__);
        }
    }

    // Adjust the initial media time if outside the available seek range
    if ((initialMediaTimeMS != -1) && (initialMediaTimeMS < player->startTimeMS))
    {
        player->initialMediaTimeNS = player->startTimeMS * NANO_MSECS;
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_HN,
                "%s() - initial media time was less than available start time, adjusting initial media time to: %"PRIi64" ms\n",
                __FUNCTION__, (player->initialMediaTimeNS / NANO_MSECS));
    }
    else if (initialMediaTimeMS > player->endTimeMS)
    {
        player->initialMediaTimeNS = player->endTimeMS * NANO_MSECS;
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_HN,
                "%s() - initial media time was greater than available end time, adjusting initial media time to: %"PRIi64" ms\n",
                __FUNCTION__, (player->initialMediaTimeNS / NANO_MSECS));
    }
    else
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                "%s() - initial media time is within available seek range or -1, leaving initial media time set to: %"PRIi64" ms\n",
                __FUNCTION__, (player->initialMediaTimeNS / NANO_MSECS));
    }

    return ret;
}

/**
 * Form the HTTP GET request to send to HTTP Server
 *
 * @param   player   player handling HTTP request
 */
static mpe_Error hnPlayer_formulateHttpHeadRequest(HnPlayer* player)
{
    char requestStr[MAXDATASIZE];
    char tmpStr[32];

    strcpy(requestStr, "HEAD ");

    strcat(requestStr, player->uri);

    strcat(requestStr, " HTTP/1.1");
    strcat(requestStr, CRLF);

    strcat(requestStr, "HOST: ");
    strcat(requestStr, player->host);
    strcat(requestStr, ":");
    (void) memset((char *)&tmpStr, 0, sizeof(tmpStr));
    sprintf(tmpStr, "%d", player->port);
    strcat(requestStr, tmpStr);
    strcat(requestStr, CRLF);

    // Only include the connection id if it is valid
    if ((player->connectionId > 0) && (player->connectionIdStr != NULL))
    {
        strcat(requestStr, "Scid.dlna.org : ");
        strcat(requestStr, player->connectionIdStr);
        strcat(requestStr, CRLF);
    }

    // Include request to get content features
    strcat(requestStr, "getcontentFeatures.dlna.org : 1");
    strcat(requestStr, CRLF);

    // *TODO* - OCORI-4733 Temporary workaround for MPEOS reverted change
    player->isAvailableSeekSupported = TRUE;
    // Include available seek range if supported
    if (player->isAvailableSeekSupported)
    {
        strcat(requestStr, "getAvailableSeekRange.dlna.org : 1");
        strcat(requestStr, CRLF);
    }

    // *TODO* - OCORI-4733 Temporary workaround for MPEOS reverted change
    player->isTimeSeekSupported = TRUE;
    // Include time seek range if supported
    if (player->isTimeSeekSupported)
    {
        strcat(requestStr, "TimeSeekRange.dlna.org : npt=0-");
        strcat(requestStr, CRLF);
    }

    // Add termination characters
    strcat(requestStr, "\r\n");

    // Request memory to store request
    int requestSize = strlen(requestStr) + 1;

    if (mpe_memAlloc(requestSize, (void **) &player->httpHeadRequestStr)
            != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - Could not allocate %d bytes of memory for HEAD request string\n",
                __FUNCTION__, requestSize);
        return MPE_HN_ERR_OS_FAILURE;
    }
    else
    {
        memcpy(player->httpHeadRequestStr, requestStr, requestSize);
    }

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - head request: %s\n", __FUNCTION__,
            player->httpHeadRequestStr);

    return MPE_HN_ERR_NOERR;
}

/**
 * Reads the response received from the HTTP GET request which includes processing
 * the HTTP header in the response and data based on the content length specfied
 * in the response header.
 *
 * @param   player   player handling HTTP request
 */
static mpe_Error hnPlayer_handleHttpGetResponse(HnPlayer* player)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    // Look for successful reply, anything in the 200 realm is considered successful
    if (player->http_decoder.header_status_code < 200 ||
        player->http_decoder.header_status_code > 299)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - Received non-successful HTTP response: '%s', status %u\n",
                __FUNCTION__, player->http_decoder.header_data, player->http_decoder.header_status_code);
        return MPE_HN_ERR_OS_FAILURE;
    }

    if (player->http_decoder.header_connection_id != INVALID_CONNECTION_ID)
    {
        // Save the connection Id as int and string
        char tmpStr[64];
        if (NULL != player->connectionIdStr)
        {
            mpe_memFree(player->connectionIdStr);
            player->connectionIdStr = NULL;
        }

        // Extract the connection id
        sprintf(tmpStr, "%u", player->http_decoder.header_connection_id);
        player->connectionId = player->http_decoder.header_connection_id;

        // Copy the string form
        if (mpe_memAlloc((strlen((char*) &tmpStr[0]) + 1),
                (void **) &player->connectionIdStr) != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s() - Could not allocate memory for connection id string %s\n",
                    __FUNCTION__, tmpStr);
            return MPE_HN_ERR_OS_FAILURE;
        }

        strcpy(player->connectionIdStr, tmpStr);
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN,
            "%s() - response had connection id str: %s, connection id: %d\n",
            __FUNCTION__, player->connectionIdStr, player->connectionId);
    }
    else
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN,
            "%s() - response did not have a connection id\n", __FUNCTION__);
    }

    return MPE_HN_ERR_NOERR;
}

/**
 * Method which initializes the media time related parameters.  This method
 * determines what the initial current media time, previous media time and segment
 * offset values should be initialized to.  This method has the logic to handle
 * segmented vs non-segmented, recording and live streaming playbacks.
 *
 * NOTE: this method should not be needed if actual media times can be retrieved from
 * streamed content.
 */
static void hnPlayer_mediaTimesInitialize(HnPlayer* player)
{
    // Set the current media time based on initial time.
    // If no initial media time was specified and live streaming, this indicates use live point which
    // will be close to the available end time
    if (player->initialMediaTimeNS == -1)
    {
        if (player->http_decoder.header_s0_increasing)
        {
            player->curMediaTimeMS = player->endTimeMS;
            player->prevMediaTimeMS = player->curMediaTimeMS;
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - set to available end time, curMediaTimeMS: %"PRIi64" ms\n",
                __FUNCTION__, player->curMediaTimeMS);
        }
        else
        {
            // Not live streaming, it's a recording so default time is zero
            player->curMediaTimeMS = 0;
            player->prevMediaTimeMS = 0;
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - set to zero, curMediaTimeMS: %"PRIi64" ms\n",
                 __FUNCTION__, player->curMediaTimeMS);
        }
    }
    else
    {
        player->curMediaTimeMS = player->initialMediaTimeNS / NANO_MSECS;
        player->prevMediaTimeMS = player->curMediaTimeMS;
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - set to initial time, curMediaTimeMS: %"PRIi64" ms\n",
            __FUNCTION__, player->curMediaTimeMS);
    }
    // Reset segment offset time back to zero
    player->segmentOffsetTimeMS = 0;

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
            "%s() - initialized media times, current media time MS: %"PRIu64", seg offset: %"PRIu64", prev media time ms: %"PRIu64"\n",
            __FUNCTION__, player->curMediaTimeMS, player->segmentOffsetTimeMS, player->prevMediaTimeMS);

}

/**
 * Method called from the pipeline to indicate the pipeline needs more
 * data.  This prevents thread deadlock when the buffer is pushed and
 * the block property is set to true.
 *
 * @param   data  reference to the player
 */
void hnPlayer_needDataCB(void* data)
{
    HnPlayer* player = data;

    //MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
    //          "%s() - setting flag that data is needed\n",
    //          __FUNCTION__);

    // Set the flag that the pipeline needs data
    player->pipelineNeedsData = TRUE;
}

/**
 * Determines if PIDs were supplied in the playback start player parameters.
 * If PID values were supplied, use them rather than filtering PAT/PMT to determine PIDs.
 *
 * @param   player         this HN player
 * @param   playerParams   values supplied in playback start call for this player
 */
static mpe_Error hnPlayer_saveSpecifiedPIDs(HnPlayer* player,
        mpe_HnHttpHeaderAVStreamParameters avStreamParameters)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    // Save the supplied values
    player->video_pid = avStreamParameters.videoPID;
    player->video_pid_type = avStreamParameters.videoType;

    player->audio_pid = avStreamParameters.audioPID;
    player->audio_pid_type = avStreamParameters.audioType;

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - player video pid %d\n", __FUNCTION__, player->video_pid);
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - player video pid type %d\n", __FUNCTION__, player->video_pid_type);
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - player audio pid %d\n", __FUNCTION__, player->audio_pid);
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - player audio pid type %d\n", __FUNCTION__, player->audio_pid_type);
    
    // If video pid was supplied, use it
    if (UNKNOWN_PID_VALUE != player->video_pid)
    {
        player->pid_cnt = 1;

        if (mpe_memAlloc(sizeof(ri_pid_info_t), (void **) &player->pids) != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s() - Could not allocate memory for ri pid data\n",
                    __FUNCTION__);
            return MPE_HN_ERR_OS_FAILURE;
        }
        else
        {
            // Store supplied pid in ri pid data structure
            player->pids[0].srcPid = player->video_pid;
            player->pids[0].srcFormat = player->video_pid_type;

            // allow the platform to set the mediaFormat from the pid type
            player->pids[0].mediaType = RI_MEDIA_TYPE_UNKNOWN;
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                "%s() - Unknown video pid - setting pid count to zero\n",
                __FUNCTION__);
        player->pid_cnt = 0;
    }
    return MPE_HN_ERR_NOERR;
}

/**
 * Changes the Source Params associated with the current playback session.
 *
 * @param   player            player whose params are to change
 * @param   playbackSession   session which params are changing
 * @param   playbackParams    new parameters for this playback sesssion
 *
 * @return  MPE_HN_ERR_NOERR if no problems were encountered,
 *          MPE_HN_ERR_INVALID_PARAM, or stop/start return value otherwise
 */
mpe_Error hnPlayer_playbackChangePIDs(HnPlayer* player,
            mpe_HnPlaybackSession playbackSession,
            mpe_HnHttpHeaderAVStreamParameters* pids)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called for player %d\n",
            __FUNCTION__, player->playerId);

    mpe_Error ret = MPE_HN_ERR_NOERR;

    if (NULL == player)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - player was NULL\n", __FUNCTION__);
        ret = MPE_HN_ERR_INVALID_PARAM;
    }

    mpeos_mutexAcquire(player->mutex);

    if (MPE_HN_ERR_NOERR == hnPlayer_saveSpecifiedPIDs(player, *pids))
    {
        if (hnPlayer_startDecode(player))
        {
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() decode started\n",
                __FUNCTION__);
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - problems storing supplied PIDs from playback params\n",
                __FUNCTION__);
    }

    mpeos_mutexRelease(player->mutex);
    return ret;
}

/**
 * Returns the current media time of the playback session
 *
 * @param   player            get current time for this player
 * @param   playbackSession   associated playback session
 * @param   mediaTimeNS       current playback time in nanosecs
 */
mpe_Error hnPlayer_playbackGetTime(HnPlayer* player,
        mpe_HnPlaybackSession playbackSession, int64_t* mediaTimeNS)
{
    mpeos_mutexAcquire(player->mutex);

    *mediaTimeNS = player->curMediaTimeMS * NANO_MSECS;

    //MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - player %d media time: %"PRIi64" \n",
    //        __FUNCTION__, player->playerId, *mediaTimeNS);

    mpeos_mutexRelease(player->mutex);
    return MPE_SUCCESS;
}

/**
 * Closes the players socket conn.
 *
 * @param player  player whose socket to close
 *
 */
mpe_Error hnPlayer_closeSocket(HnPlayer* player)
{
    // Close down the socket
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - closing socket\n", __FUNCTION__);

    if ((MPE_SOCKET_INVALID_SOCKET != player->socket) &&
        (mpeos_socketClose(player->socket) == -1))
    {
        // Handle problems closing socket, log error but don't return error
        // to higher level since everything that could have been done is done
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - problems closing socket\n", __FUNCTION__);
    }
    else
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - closed socket\n",
                __FUNCTION__);
    }
    player->socket = MPE_SOCKET_INVALID_SOCKET;

    return MPE_HN_ERR_NOERR;
}

/**
 * Terminates the playback associated with the supplied player.
 *
 * @param player  player to stop
 *
 */
mpe_Error hnPlayer_playbackStop(HnPlayer* player,
            mpe_HnPlaybackSession playbackSession,
            mpe_MediaHoldFrameMode holdFrameMode)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called for player %d\n",
            __FUNCTION__, player->playerId);

    mpe_Error ret = MPE_HN_ERR_NOERR;

    mpeos_mutexAcquire(player->mutex);

    // Stop the pipeline if not directed to hold frame
    if (holdFrameMode == MPE_MEDIA_STOP_MODE_HOLD_FRAME)
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                "%s() - not stopping pipeline due to request to hold frame\n", __FUNCTION__);
        player->holdingFrame = TRUE;
    }
    else
    {
        player->pipeline->pipeline_hn_player_stop(player->pipeline);
        player->holdingFrame = FALSE;
    }

    // Terminate the stream thread
    if (NULL != player->readThread)
    {
        // Terminate the existing stream thread
        hnPlayerReadThread_terminate(player);
    }

    if (player->dtcpipSessionHandle != DTCPIP_INVALID_SESSION_HANDLE)
    {
        int dtcpip_ret = g_dtcpip_ftable->dtcpip_snk_close(player->dtcpipSessionHandle);
        if (dtcpip_ret != 0)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s - Problems closing DTCP/IP session %d - dtcpip_snk_close returned %d\n",
                __FUNCTION__, player->dtcpipSessionHandle, dtcpip_ret);
            player->dtcpipSessionHandle = DTCPIP_INVALID_SESSION_HANDLE;
        }
    }

    // Close down the socket
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - closing socket\n", __FUNCTION__);

    if ((MPE_SOCKET_INVALID_SOCKET != player->socket) &&
        (mpeos_socketClose(player->socket) == -1))
    {
        // Handle problems closing socket, log error but don't return error
        // to higher level since everything that could have been done is done
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - problems closing socket\n", __FUNCTION__);
    }
    else
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - closed socket\n",
                __FUNCTION__);
    }
    player->socket = MPE_SOCKET_INVALID_SOCKET;

    // Clear out pids
    player->pid_cnt = 0;
    player->pids = NULL;

    // Clear video device
    player->riVideoDevice = NULL;
    //player->mpeVideoDevice = NULL;

    // Clear out playback session
    player->activePlayback->handle = 0;
    player->activePlayback->rate = 0.0;

    // Set the state back to stream open since stream has not yet been closed
    player->state = HN_STATE_OPENED;

    // Send the play back stopped event
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - sending playback stopped event\n", __FUNCTION__);
    mpeos_eventQueueSend(player->queueID, MPE_HN_EVT_PLAYBACK_STOPPED, NULL,
            (void*) player->act, 0);

    mpeos_mutexRelease(player->mutex);

    // Free the supplied playback session
    mpe_memFree(playbackSession);

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - returning %d\n", __FUNCTION__, ret);

    return ret;
}

/**
 * Pauses current play back by stopping socket reads until resume method is called.
 *
 * @param   player  this player
 * @param   playbackSession session to pause
 *
 * @return  MPE_HN_ERR_NOERR
 */
mpe_Error hnPlayer_playbackPause(HnPlayer* player,
            mpe_HnPlaybackSession playbackSession)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called for player %d\n",
            __FUNCTION__, player->playerId);

    mpe_Error ret = MPE_HN_ERR_NOERR;

    mpeos_mutexAcquire(player->mutex);

    hnPlayerReadThread_pause(player);

    mpeos_mutexRelease(player->mutex);

    return ret;
}

/**
 * Resumes current play back if paused by resuming socket reads.
 *
 * @param   player  this player
 * @param   playbackSession session to resume
 *
 * @return  MPE_HN_ERR_NOERR
 */
mpe_Error hnPlayer_playbackResume(HnPlayer* player,
            mpe_HnPlaybackSession playbackSession)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called for player %d\n",
            __FUNCTION__, player->playerId);

    mpe_Error ret = MPE_HN_ERR_NOERR;

    mpeos_mutexAcquire(player->mutex);

    hnPlayerReadThread_resume(player);

    mpeos_mutexRelease(player->mutex);

    return ret;
}

/**
 * Stops current session stream on the supplied player by setting state back to idle
 * and freeing allocated memory.
 *
 * @param player  player to shutdown
 *
 * @event
 *    MPE_HN_EVT_SESSION_CLOSED indicating that the streaming session was
 *    closed successfully.
 *
 * @return MPE_HN_ERR_NOERR         if successful
 *         MPE_HN_ERR_INVALID_PARAM if a parameter is invalid
 *         MPE_HN_ERR_OS_FAILURE    os specific failures
 */
mpe_Error hnPlayer_streamClose(HnPlayer* player)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called for player %d\n",
            __FUNCTION__, player->playerId);

    mpe_Error ret = MPE_HN_ERR_NOERR;

    mpeos_mutexAcquire(player->mutex);

    // Stop the pipeline
    player->holdingFrame = FALSE;
    player->pipeline->pipeline_hn_player_stop(player->pipeline);

    // Terminate the stream thread
    if (NULL != player->readThread)
    {
        // Terminate the existing stream thread
        hnPlayerReadThread_terminate(player);
    }

    // Close down the socket
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - closing socket\n", __FUNCTION__);

    if ((MPE_SOCKET_INVALID_SOCKET != player->socket) &&
            (mpeos_socketClose(player->socket) == -1))
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - problems closing socket\n", __FUNCTION__);
    }
    else
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - closed socket\n",
                __FUNCTION__);
    }
    player->socket = MPE_SOCKET_INVALID_SOCKET;

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - freeing memory\n", __FUNCTION__);
    if (NULL != player->connectionIdStr)
    {
        mpe_memFree(player->connectionIdStr);
        player->connectionIdStr = NULL;
    }
    if (NULL != player->uri)
    {
        mpe_memFree(player->uri);
        player->uri = NULL;
    }
    if (NULL != player->host)
    {
        mpe_memFree(player->host);
        player->host = NULL;
    }
    if (NULL != player->dtcp_host)
    {
        mpe_memFree(player->dtcp_host);
        player->dtcp_host = NULL;
    }
    if (NULL != player->httpHeadRequestStr)
    {
        mpe_memFree(player->httpHeadRequestStr);
        player->httpHeadRequestStr = NULL;
    }
    if (NULL != player->httpHeadResponseStr)
    {
        mpe_memFree(player->httpHeadResponseStr);
        player->httpHeadResponseStr = NULL;
    }
    if (NULL != player->httpGetRequestStr)
    {
        mpe_memFree(player->httpGetRequestStr);
        player->httpGetRequestStr = NULL;
    }
    if (NULL != player->httpGetResponseStr)
    {
        mpe_memFree(player->httpGetResponseStr);
        player->httpGetResponseStr = NULL;
    }

    // Send the stream close event
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - sending session closed event\n", __FUNCTION__);
    mpeos_eventQueueSend(player->queueID, MPE_HN_EVT_SESSION_CLOSED, NULL,
            (void*) player->act, 0);

    // Zero out session
    player->streamSessionId = 0;

    // Set the state to idle
    player->state = HN_STATE_IDLE;

    mpeos_mutexRelease(player->mutex);

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - exit\n", __FUNCTION__);

    return ret;
}

/**
 * Performs the conversion necessary of the rate which is a float into
 * the format required for the Playspeed.dlna.org HTTP header field which
 * is an integer with possibly a fractional part.
 *
 *  @param  requestStr  current request string which inprogress of formulating
 *  @param  rate        desired playspeed specified as a float which needs to be formated
 *                      into the request in Playspeed.dlna.org format.
 */
static void hnPlayer_formatPlayspeed(char* requestStr, float rate)
{
    // Convert rate value to format defined in dlna spec which is an integer with fractional string
    // factional part
    char rateStr[10];

    // Could do something more sophisticated but currently only supporting these
    // two fractional values so this is simpler and sufficient for now
    if (rate == 0.5)
    {
        strcat(requestStr, "1/2");
    }
    else if (rate == -0.5)
    {
        strcat(requestStr, "-1/2");
    }
    else
    {
        snprintf(rateStr, 10, "%d", (int)rate);
        strcat(requestStr, rateStr);
    }
    strcat(requestStr, CRLF);
}

/**
 * Returns the number of DLNA profile IDs that the platform player is able to support.
 *
 * @param profileIDCnt          Returns number/count of player-supported DLNA profile IDs
 *
 * @return MPE_SUCCESS if successful.
*/
mpe_Error hnPlayer_getDLNAProfileIDsCnt(uint32_t* profileIDCnt)
{
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    *profileIDCnt = g_profilesCnt;

    return MPE_HN_ERR_NOERR;
}

/**
 * Returns DLNA profile ID string at requested index from array of possible DLNA profile IDs
 * supported by the underlying client platform. This method will return profileIDStr,
 * assuming caller has allocated memory whose size is MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE.
 *
 * The index number also indicates the platform preferred DLNA profile. For example,
 * index 0 is the most preferred playback format, index 1 is the second most preferred
 * format, etc.
 *
 * @param idx                   retrieve this index in array of supported profile IDs
 *                              (indices start at 0, through count-1),
 *                              based on count returned via mpeos_hnPlayerGetDLNAProfileIDsSizeCnt()
 * @param profileIDStr          returns requested DLNA profiles ID string from specified
 *                              index in caller allocated memory of MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE
 *
 * @return MPE_SUCCESS if successful.
 * @return MPE_EINVAL if index is out of range.
 */
mpe_Error hnPlayer_getDLNAProfileIDStr(uint32_t idx,
                                       char profileIDStr[MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE])
{
    mpe_Error ret = MPE_HN_ERR_NOERR;

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    if (idx > g_profilesCnt)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() -- invalid index supplied %d, max %d\n",
                __FUNCTION__, idx, g_profilesCnt);
        return MPE_HN_ERR_OS_FAILURE;
    }
    else
    {
        strncpy(profileIDStr, (char*)&g_profiles[idx].profileId[0],
                MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE);
    }
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN, "%s() - returning %s at idx %d\n",
            __FUNCTION__, profileIDStr, idx);

    return ret;
}

mpe_Error hnPlayer_getMimeTypesCnt(char * profileIDStr, uint32_t * mimeTypeCnt)
{
    mpe_Error ret = MPE_HN_ERR_NOERR;

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    int idx = hnPlayer_findProfile(profileIDStr);
    if (idx <= -1)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                  "%s() -- invalid profile ID string supplied %s, no match found\n",
                  __FUNCTION__, profileIDStr);
        return MPE_HN_ERR_OS_FAILURE;
    }
    else
    {
        *mimeTypeCnt = 1; // Only 1 MIME type per profile is currently supported
    }

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN, "%s() - returning %d MIME types for profile %s\n",
              __FUNCTION__, *mimeTypeCnt, profileIDStr);

    return ret;
}

mpe_Error hnPlayer_getMimeTypeStr( char * profileIDStr, uint32_t idx,
                                   char mimeTypeStr[MPE_HN_MAX_MIME_TYPE_STR_SIZE])
{
    mpe_Error ret = MPE_HN_ERR_NOERR;

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    // Find index of specified profile string
    int profileIdx = hnPlayer_findProfile(profileIDStr);
    if (profileIdx <= -1)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() -- invalid profile ID string supplied %s, no match found\n",
                __FUNCTION__, profileIDStr);
        return MPE_HN_ERR_OS_FAILURE;
    }
    else
    {
        if (idx > 1)
        { // Only 1 MIME type per profile is currently supported
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s() -- invalid MIME type index supplied %d, 1 available\n",
                    __FUNCTION__, idx );
            return MPE_HN_ERR_OS_FAILURE;
        }
        else
        {
            strncpy(mimeTypeStr, g_profiles[profileIdx].mimeType, MPE_HN_MAX_MIME_TYPE_STR_SIZE);
        }
    }

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN, "%s() - returning MIME type %s at idx %d\n",
            __FUNCTION__, mimeTypeStr, idx);

    return ret;
}

/**
 * Retrieves number/count of client platform supported playspeeds for the given
 * DLNA profile ID. Playspeeds will be strings such as -2, -1/2, 2, etc.
 *
 * @param profileIDStr          retrieve supported playspeeds count for content item using this profile
 * @param playspeedCnt          returns number/count of supported playspeeds
 *
 * @return MPE_SUCCESS if successful.
 * @return MPE_EINVAL if profile ID is not supported.
 */
mpe_Error hnPlayer_getPlayspeedsCnt(char* profileIDStr, char* mimeTypeStr, uint32_t* playspeedCnt)
{
    mpe_Error ret = MPE_HN_ERR_NOERR;

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    int idx = hnPlayer_findProfile(profileIDStr);
    if (idx <= -1)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() -- invalid profile ID string supplied %s, no match found\n",
                __FUNCTION__, profileIDStr);
        return MPE_HN_ERR_OS_FAILURE;
    }
    else
    {
        *playspeedCnt = g_profiles[idx].playspeedsCnt;
    }

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN, "%s() - returning %d for profile %s\n",
            __FUNCTION__, *playspeedCnt, profileIDStr);

    return ret;
}

/**
 * Returns playspeed strings such as -2, -1/2, 2, etc. which are supported for
 * the specified DLNA profile ID transfer.
 * This method will return playspeed string, assuming caller has allocated memory
 * whose size is MPE_HN_MAX_PLAYSPEED_STR_SIZE.
 *
 * @param profileIDStr          retrieve supported playspeed for this profile
 * @param idx                   retrieve this index in array of supported playspeeds
 *                              (indices start at 0, through count-1),
 *                              based on count returned via mpeos_hnPlayerGetPlayspeedsCnt()
 * @param playspeedStr          returns playspeed string at specified index using caller allocated
 *                              memory of MPE_HN_MAX_PLAYSPEED_STR_SIZE
 */
mpe_Error hnPlayer_getPlayspeedStr(char* profileIDStr, char* mimeTypeStr, uint32_t idx,
                                        char playspeedStr[MPE_HN_MAX_PLAYSPEED_STR_SIZE])
{
    mpe_Error ret = MPE_HN_ERR_NOERR;

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    // Find index of specified profile string
    int profileIdx = hnPlayer_findProfile(profileIDStr);
    if (profileIdx <= -1)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() -- invalid profile ID string supplied %s, no match found\n",
                __FUNCTION__, profileIDStr);
        return MPE_HN_ERR_OS_FAILURE;
    }
    else
    {
        if (idx > g_profiles[profileIdx].playspeedsCnt)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s() -- invalid playspeed index supplied %d, max %d\n",
                    __FUNCTION__, idx, g_profiles[profileIdx].playspeedsCnt);
            return MPE_HN_ERR_OS_FAILURE;
        }
        else
        {
            strncpy(playspeedStr, (char*)&g_profiles[profileIdx].playspeeds[idx][0],
                    MPE_HN_MAX_PLAYSPEED_STR_SIZE);
        }
    }

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN, "%s() - returning %s at idx %d\n",
            __FUNCTION__, playspeedStr, idx);

    return ret;
}

static int hnPlayer_findProfile(char* profileIDStr)
{
    // Find profile which matches supplied profile id string
    int idx = -1;
    int i = 0;
    for (i = 0; i < g_profilesCnt; i++)
    {
        if (strcmp(profileIDStr, (char*)&g_profiles[i].profileId[0]) == 0)
        {
            // Found matching profile
            idx = i;
            MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN,
                    "<<HN>> %s - found matching profile at idx: %d\n",
                    __FUNCTION__, idx);
            break;
        }
        else
        {
            MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN,
                    "<<HN>> %s - string at idx %d, %s, does not match: %s\n",
                    __FUNCTION__, i, (char*)&g_profiles[i].profileId[0], profileIDStr);

        }
    }

    return idx;
}

/**
 * Returns the HTTP head response associated with this player session.
 *
 * @param   get HEAD response associated with this player
 *
 * @return  HTTP HEAD response received, may be null
 */
char* hnPlayer_getHttpHeadResponse(HnPlayer* player)
{
    return player->httpHeadResponseStr;
}

/**
 * Returns the HTTP GET response associated with this player session.
 *
 * @param   get GET response associated with this player
 *
 * @return  HTTP GET response received, may be null
 */
char* hnPlayer_getHttpGetResponse(HnPlayer* player)
{
    return player->httpGetResponseStr;
}
