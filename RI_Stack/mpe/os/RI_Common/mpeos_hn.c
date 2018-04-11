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

#include "mpe_types.h"
#include "mpe_error.h"
#include "mpe_os.h"

#include "mpeos_hn.h"
#include "mpeos_dbg.h"
#include "mpeos_event.h"
#include "mpeos_thread.h"
#include "mpeos_sync.h"
#include "mpeos_mem.h"
#include "mpeos_socket.h"
#include "mpeos_util.h"
#include "hn_server.h"
#include "hn_player.h"
#include "hn_dtcpip.h"

#include "platform_filter.h"

#include <ri_pipeline_manager.h>

#include <ri_test_interface.h>
#include "test_3dtv.h"

// Define the memory to be HN allocated category
#define MPE_MEM_DEFAULT MPE_MEM_HN

// Next available session id
static uint32_t g_nextSessionId;

// HN Players
static HnPlayer* g_players = NULL;
static uint32_t g_numPlayers;

// HN Servers
static HnServer* g_servers = NULL;
static uint32_t g_numServers;

// Overall mutex
static mpe_Mutex g_hnMutex;
static mpe_Bool g_hnInitialized = FALSE;

// Fake implementation of 3DTV - default to 2D
static mpe_Media3DPayloadType g_payloadType = 0;
static mpe_DispStereoscopicMode g_stereoscopicMode = 0;
static uint8_t *g_payload = NULL;
static uint32_t g_payloadSz = 0;

// Fake implementation of input video scan mode
static mpe_MediaScanMode g_videoScanMode = SCANMODE_UNKNOWN;

// For RI test interface (telnet interface) HN Menu
#define MPEOS_HN_MENU \
    "\r\n" \
    "|---+-----------------------\r\n" \
    "| g | HTTP GET Response\r\n" \
    "|---+-----------------------\r\n" \
    "| h | HTTP HEAD Response\r\n" \
    "|---+-----------------------\r\n" \
    "| t | Transformation Menu\r\n"

static HnPlayer *getCurrentSession()
{
    int i;
    HnPlayer* player = NULL;

    mpeos_mutexAcquire(g_hnMutex);
    for (i = 0; i < g_numPlayers; i++)
    {
        if (g_players[i].activePlayback->handle != 0)
        {
            player = &g_players[i];
            break;
        }
    }
    mpeos_mutexRelease(g_hnMutex);

    return player;
}

static int testHN3DTVInputHandler(int sock, char *rxBuf, int *retCode, char **retStr)
{
    int ret = 0;

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s(%d, %s);\n",
              __FUNCTION__, sock, rxBuf);
    *retCode = MENU_SUCCESS;

    if (strstr(rxBuf, "x"))
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s - Exit -1\n", __FUNCTION__);
        return -1;
    }

    HnPlayer *playback = getCurrentSession();
    if (playback == NULL)
    {
        ri_test_SendString(sock, "\r\n\nNO CURRENT HN STREAM SESSION -- EVENTS WILL NOT BE SENT!\r\n");
    }

#ifdef __linux__
    mpe_EventQueue queue = 0;
#else
    mpe_EventQueue queue = NULL;
#endif
    void* act = NULL;
    if (playback != NULL)
    {
        queue = playback->queueID;
        act = playback->act;
    }

    ret = test3DTVInputHandler(sock, rxBuf, queue, act,
            &g_payloadType, &g_stereoscopicMode, &g_payload, &g_payloadSz,
            &g_videoScanMode);

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s - Exit %d\n", __FUNCTION__, ret);
    return ret;
}

static MenuItem HN3DTVMenuItem =
{ false, "h", "HN 3DTV Test", MPEOS_3DTV_TESTS, testHN3DTVInputHandler };

static int testHNMpeosInputHandler(int sock, char *rxBuf, int *retCode, char **retStr)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s(%d, %s);\n",
              __FUNCTION__, sock, rxBuf);
    *retCode = MENU_SUCCESS;

    char* response = NULL;

    if (strstr(rxBuf, "x"))
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s - Exit -1\n", __FUNCTION__);
        return -1;
    }

    HnPlayer *player = getCurrentSession();

    switch (rxBuf[0])
    {

    // Retrieve GET response string
    case 'g':
        ri_test_SendString(sock, "\r\n\nHTTP GET Response...\r\n");

        if (player == NULL)
        {
            ri_test_SendString(sock, "\r\n\nNO CURRENT HN STREAM SESSION!\r\n");
            *retCode = MENU_FAILURE;
            return 0;
        }

        response = hnPlayer_getHttpGetResponse(player);
        if (response == NULL)
        {
            response = "HTTP GET Response is null";
        }
        ri_test_SendString(sock, response);
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DVR, "%s - %s);\n", __FUNCTION__, response);

        *retStr = g_strdup(response);

        break;

    // Retrieve HEAD response string
    case 'h':
        ri_test_SendString(sock, "\r\n\nHTTP HEAD Response...\r\n");

        if (player == NULL)
        {
            ri_test_SendString(sock, "\r\n\nNO CURRENT HN STREAM SESSION!\r\n");
            *retCode = MENU_FAILURE;
            return 0;
        }

        response = hnPlayer_getHttpHeadResponse(player);
        if (response == NULL)
        {
            response = "HTTP HEAD Response is null";
        }
        ri_test_SendString(sock, response);
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DVR, "%s - %s);\n", __FUNCTION__, response);

        *retStr = g_strdup (response);   

        break;

    // go to Transformation menu
    case 't':
        ri_test_SetNextMenu(sock, ri_test_FindMenu("Transformation Tests"));
        return 1;

    default:
        {
            char *temp = " - unrecognized\r\n\n";

            *retStr = (char *) g_malloc (strlen(rxBuf) + strlen(temp) + 1);
            strcpy(*retStr, rxBuf);
            strcat(*retStr, temp);

            ri_test_SendString(sock, *retStr);
            *retCode = MENU_INVALID;

            break;
        }
    } // END switch (rxBuf[0])

    return 0;
}

static MenuItem HNMpeosMenuItem =
{ false, "h", "HN", MPEOS_HN_MENU, testHNMpeosInputHandler };

// Local methods
//
static HnServer* hn_findServerWithStreamSession(mpe_HnStreamSession session);

static HnServer* hn_findServerWithPlaybackSession(
        mpe_HnPlaybackSession playbackSession);

static HnPlayer* hn_findPlayerWithPlaybackSession(
        mpe_HnPlaybackSession playbackSession);

static HnPlayer* hn_findPlayerWithStreamSession(mpe_HnStreamSession session);

/**
 *  <i>mpeos_hnInit()</i>
 *
 * mpeos_hnInit() should perform platform-specific initialization support
 * for the home networking streaming API.
 *
 * @return MPE_HN_ERR_NOERR if the subsystem initialization is successful
 *   otherwise, MPE_HN_ERR_OS_FAILURE.
 *
 */
mpe_Error mpeos_hnInit(void)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() called\n", __FUNCTION__);

    if (g_hnInitialized)
        return MPE_HN_ERR_NOERR;

    // Set flag to indicate initialized so another thread doesn't come in
    g_hnInitialized = TRUE;

    mpeos_hnPlayerPlaybackInit3DConfig();

    // Initialize server parameters whose values are based on config parameters
    hnServer_initEnv();

    // Initialize player parameters whose values are based on config parameters
    hnPlayer_initEnv();

    // Allocate the overall mutex
    if (mpeos_mutexNew(&g_hnMutex) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - Could not create overall mutex\n", __FUNCTION__);
        return MPE_HN_ERR_OS_FAILURE;
    }

    // Acquire mutex while initializing
    mpeos_mutexAcquire(g_hnMutex);

    // Get the platform pipeline manager in order to retrieve number of servers & pipelines
    ri_pipeline_manager_t* pipelineMgr = ri_get_pipeline_manager();
    if (NULL == pipelineMgr)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - Could not retrieve platform pipeline manager\n",
                __FUNCTION__);
        mpeos_mutexRelease(g_hnMutex);
        return MPE_HN_ERR_OS_FAILURE;
    }

    ri_test_RegisterMenu(&HN3DTVMenuItem);
    ri_test_RegisterMenu(&HNMpeosMenuItem);

    // Initialize the session id
    g_nextSessionId = 1;

    // Currently only one HN Player is supported due to restriction in platform of one decoder
    g_numPlayers = 1;

    // Allocate the player data.
    if (mpe_memAlloc(sizeof(HnPlayer) * g_numPlayers, (void **) &g_players)
            != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - Could not allocated player data array\n", __FUNCTION__);
        mpeos_mutexRelease(g_hnMutex);
        return MPE_HN_ERR_OS_FAILURE;
    }

    // Initialize each of the players
    ri_pipeline_t* pipeline = NULL;
    uint32_t i = 0;
    for (i = 0; i < g_numPlayers; i++)
    {
        pipeline = pipelineMgr->get_hn_player_pipeline(pipelineMgr);
        if (NULL == pipeline)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s(): unable to get player HN pipeline %d\n",
                    __FUNCTION__, i);
            mpeos_mutexRelease(g_hnMutex);
            return MPE_HN_ERR_OS_FAILURE;
        }

        // Initialize the players
        if (MPE_HN_ERR_OS_FAILURE == hnPlayer_init((HnPlayer*) &g_players[i],
                pipeline, i))
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "Problems initializing HN player streams\n");
            mpeos_mutexRelease(g_hnMutex);
            return MPE_HN_ERR_OS_FAILURE;
        }
    }

    // Initialize the servers
    // Query the platform to determine the number of servers supported
    const ri_pipeline_t** pipelines = pipelineMgr->get_hn_server_pipelines(
            pipelineMgr, &g_numServers);

    // Allocate the server data.
    if (mpe_memAlloc(sizeof(HnServer) * g_numServers, (void **) &g_servers)
            != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - Could not allocate server data array\n", __FUNCTION__);
        mpeos_mutexRelease(g_hnMutex);
        return MPE_HN_ERR_OS_FAILURE;
    }

    // Get mpeenv config parameter common to all servers

    // Initialize each of the servers
    for (i = 0; i < g_numServers; i++)
    {
        if (MPE_HN_ERR_OS_FAILURE == hnServer_init((HnServer*) &g_servers[i],
                (ri_pipeline_t*) pipelines[i], i))
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "Problems initializing HN player streams\n");
            mpeos_mutexRelease(g_hnMutex);
            return MPE_HN_ERR_OS_FAILURE;
        }
    }

        if (dtcpip_init() != MPE_SUCCESS)
        {
            // DTCP has not been disabled, throw the big hammer and exit
            MPEOS_LOG(MPE_LOG_FATAL, MPE_MOD_HN,
                "Problems initializing DTCP - TERMINATING SYSTEM\n");
            exit(1);
        }
        else
        {
            char dtcpip_version_str[512];
            g_dtcpip_ftable->dtcpip_cmn_get_version(dtcpip_version_str, 512);
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                "%s - %s\n", __FUNCTION__, dtcpip_version_str);
        }
        // TODO In case of failure, keep trying to find an unassinged port.
        if (g_dtcpip_ftable->dtcpip_src_init((unsigned short) 8999) != 0)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s - Cannot initialize "
                    "DTCP/IP source with AKE port 8999.\n", __FUNCTION__);
            return MPE_HN_ERR_OS_FAILURE;
        }
        else
        {
            mpeos_envSet("MPE.HN.SERVER.DTCPIP.AKE.PORT", "8999");
        }

        if (g_dtcpip_ftable->dtcpip_snk_init() != 0)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s - Cannot initialize DTCP/IP sink\n", __FUNCTION__);
            return MPE_HN_ERR_OS_FAILURE;
        }

    // Release overall mutex since done initializing
    mpeos_mutexRelease(g_hnMutex);

    return MPE_HN_ERR_NOERR;
}

/**
 *  <i>mpeos_hnStreamOpen()</i>
 *
 * The mpeos_hnStreamOpen() function should start the asynchronous streaming
 * operation. An error code is synchronously returned to the caller to indicate
 * a preliminary state of resource allocation.
 *
 * @param openParams - allocation and populated streaming params.
 * @param queueId to post streaming related events
 * @param act is a context value for the event dispacher
 * @param streamingSession opaque handle to the streaming session.
 *     See mpeos_hnStreamClose(), mpeos_hnStreamGetInfo()
 *
 * @event MPE_HN_EVT_SESSION_OPEN for a successfully opened streaming session.
 *
 * @return MPE_HN_ERR_NOERR         if successful
 *         MPE_HN_ERR_INVALID_PARAM if a parameter is invalid
 *         MPE_HN_ERR_OS_FAILURE    os specific failures
 *         MPE_HN_ERR_CONTENT       if the failure has occured while acquiring the content
 */
mpe_Error mpeos_hnStreamOpen(mpe_HnStreamParams * openParams,
        mpe_EventQueue queueId, void * act,
        mpe_HnStreamSession * streamingSession)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() called\n", __FUNCTION__);

    // Initialize to error
    mpe_Error ret = MPE_HN_ERR_INVALID_PARAM;
    HnServer* server = NULL;
    HnPlayer* player = NULL;
    uint32_t i = 0;
    uint32_t sessionId = 0;

    // Determine if this a player or server request
    if (NULL == openParams)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "mpeos_hnStreamOpen -- NULL open params\n");
    }
    else
    {
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - openParams=0x%p)\n",
                __FUNCTION__, openParams);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - streamParams=0x%p)\n",
                __FUNCTION__, openParams->streamParams);

        // Acquire overall mutex because grabbing at level above individual player/server
        mpeos_mutexAcquire(g_hnMutex);

        if (MPE_HNSTREAM_MEDIA_PLAYER_HTTP == openParams->requestType)
        {
            // Find an available player
            for (i = 0; i < g_numPlayers; i++)
            {
                if (HN_STATE_IDLE == g_players[i].state)
                {
                    // Found a player
                    player = &g_players[i];
                    break;
                }
            }

            // Make sure an available player was found
            if (NULL == player)
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                        "%s() -- Could not find an available HN player\n",
                        __FUNCTION__);
                mpeos_mutexRelease(g_hnMutex);
                return MPE_HN_ERR_OS_FAILURE;
            }
            sessionId = ++g_nextSessionId;
            ret = hnPlayer_streamOpen(player, sessionId, openParams, queueId,
                    act, streamingSession);
        }
        else if (MPE_HNSTREAM_MEDIA_SERVER_HTTP == openParams->requestType)
        {
            // Find an available server
            for (i = 0; i < g_numServers; i++)
            {
                if (HN_STATE_IDLE == g_servers[i].state)
                {
                    // Found an available server
                    server = &g_servers[i];
                    break;
                }
            }

            // If no available server was found, return error
            if (NULL == server)
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                        "%s() -- Could not find an available HN server\n",
                        __FUNCTION__);
                mpeos_mutexRelease(g_hnMutex);
                return MPE_HN_ERR_OS_FAILURE;
            }

            sessionId = ++g_nextSessionId;
            ret = hnServer_streamOpen(server, sessionId, openParams, queueId,
                    act, streamingSession);
        }
        else
        {
            MPEOS_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_HN,
                    "mpeos_hnStreamOpen -- unsupported request stream type %d\n",
                    openParams->requestType);
        }

        // Release overall mutex
        mpeos_mutexRelease(g_hnMutex);
    }

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "mpeos_hnStreamOpen -- returning %d\n",
            ret);
    return ret;
}

/**
 *  <i>mpeos_hnPlayerStreamGetInfo()</i>
 *
 * mpeos_ hnPlayerStreamGetInfo () should return the current session parameters
 * for the target session.
 * @param session                  an active streaming session handle.
 * @param streamParams            the allocated stream parameter to acquire.
 *
 * @events
 *    None.
 *
 * @return MPE_HN_ERR_NOERR         if successful
 *           MPE_HN_ERR_INVALID_PARAM  if a parameter is invalid
 *           MPE_HN_ERR_OS_FAILURE    os specific failures
 *
 */
mpe_Error mpeos_hnPlayerStreamGetInfo(mpe_HnStreamSession session,
        mpe_HnStreamParams * sessionParams)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    mpe_Error ret = MPE_HN_ERR_INVALID_PARAM;

    // Look for player or server which is associated this supplied session ID
    HnServer* server = NULL;
    HnPlayer* player = hn_findPlayerWithStreamSession(session);
    if (NULL == player)
    {
        server = hn_findServerWithStreamSession(session);
    }
    if (NULL != player)
    {
        return hnPlayer_streamGetInfo(player, sessionParams);
    }
    else if (NULL != server)
    {
        return hnServer_streamGetInfo(server, sessionParams);
    }
    else
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_HN,
                "%s() - no player or server found associated with stream session %p\n",
                __FUNCTION__, session);
    }

    return ret;
}

/**
 *  <i>mpeos_hnStreamClose()</i>
 *
 * The mpeos_hnStreamClose() function  stops streaming of hn streamable content.
 * mpeos_hnStreamClose() sends the MPE_HN_EVENT_ STOP event to the queue.
 *
 * @param session  an active streaming session handle.
 *
 * @event
 *    MPE_HN_EVT_SESSION_CLOSED indicating that the streaming session was
 *    closed successfully.
 *
 * @return MPE_HN_ERR_NOERR         if successful
 *           MPE_HN_ERR_INVALID_PARAM  if a parameter is invalid
 *           MPE_HN_ERR_OS_FAILURE    os specific failures
 *
 *
 */
mpe_Error mpeos_hnStreamClose(mpe_HnStreamSession session)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    mpe_Error ret = MPE_HN_ERR_INVALID_PARAM;

    // Acquire overall mutex since releasing individual player/server
    mpeos_mutexAcquire(g_hnMutex);

    // Look for player or server which is associated this supplied session ID
    HnServer* server = NULL;
    HnPlayer* player = hn_findPlayerWithStreamSession(session);
    if (NULL == player)
    {
        server = hn_findServerWithStreamSession(session);
    }
    if (NULL != player)
    {
        ret = hnPlayer_streamClose(player);
    }
    else if (NULL != server)
    {
        ret = hnServer_streamClose(server);
    }
    else
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_HN,
                "%s() - no player or server found associated with stream session %p\n",
                __FUNCTION__, session);
    }

    // Release overall mutex
    mpeos_mutexRelease(g_hnMutex);

    return ret;
}

/**
 *  <i>mpeos_hnPlaybackStart()</i>
 *
 * This function creates and starts a new HN playback for a given hn streaming session.
 * A new playback handle is returned if the call is successful.  The returned handle can
 * be used to control the speed, the direction and the position of the play back in the stream.
 *
 * If presentation cannot be performed due to missing A/V components, and/or
 * the videoDevice is not specified (0), the platform must make best-effort
 * presentation and honor SectionFilters against the associated
 * streamingSession. If video presentation cannot be performed and the
 * videoDevice is specified, black must be rendered into the video plane.
 * If the platform can determine components when none are specified (e.g.
 * via AVStreamParameters with HTTP-based streaming), it may use those
 * components to perform presentation until a call to
 * mpeos_hnPlaybackChangePIDs() is made which specifies component PIDs.
 *
 * SectionFilters set on the associated streamingSession must be honored for
 * play rates between -2.0 and +2.0 (inclusive) when the playback
 * session is active.
 *
 * @param streamingSession          an active streaming session handle describing the streaming values of
 *                                  this playback session.
 * @param playbackParams            the stream parameter changes for this playback.
 * @param act                       the completion token for async events
 * @param playbackSession           pointer to a HN session playback handle. This handle is used
 *                                  to control play rate and media positions.
 *
 * @events
 *
 * MPE_HN_EVT_PLAYBACK_START signaling that the server has sent data or player has received data.
 *
 * MPE_HN_EVT_PLAYBACK_END signaling that server sent all data in requested range
 * or player has received all data in requested range.
 *
 * MPE_HN_EVT_PLAYBACK_STOP signaling that server terminated sending data prior to reaching end of request range
 * or player has stopped receiving data prior to requested range.
 *
 * @return MPE_HN_ERR_NOERR         if successful
 *         MPE_HN_ERR_INVALID_PARAM if a parameter is invalid or there is playback already which has not been stopped.
 *         MPE_HN_ERR_OS_FAILURE    os specific failures
 *         MPE_HN_ERR_CONTENT       if the failure has occurred while acquiring the content
 */
mpe_Error mpeos_hnPlaybackStart(mpe_HnStreamSession streamingSession,
        mpe_HnPlaybackParams* playbackParams,
        void *act, mpe_HnPlaybackSession* playbackSession)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    // Verify calling params
    if (NULL == playbackParams)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - playback params were NULL\n", __FUNCTION__);
        return MPE_HN_ERR_INVALID_PARAM;
    }

    // Look for player which is associated this supplied session ID
    HnServer* server = NULL;
    HnPlayer* player = hn_findPlayerWithStreamSession(streamingSession);
    if (NULL == player)
    {
        server = hn_findServerWithStreamSession(streamingSession);
    }
    mpeos_mutexAcquire(g_hnMutex);
    uint32_t sessionId = g_nextSessionId++;
    mpeos_mutexRelease(g_hnMutex);

    if (NULL != player)
    {
        return hnPlayer_playbackStart(player, sessionId, playbackParams, act, playbackSession);
    }
    else if (NULL != server)
    {
        return hnServer_playbackStart(server, sessionId, playbackParams, act, playbackSession);
    }
    else
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_HN,
                "%s() - no player or server found associated with stream session %p\n",
                __FUNCTION__, streamingSession);
    }

    return MPE_HN_ERR_INVALID_PARAM;
}

/**
 * Refer to mpeos_hn.h for full method description.
 **/
mpe_Error mpeos_hnPlaybackStop(mpe_HnPlaybackSession playbackSession,
                               mpe_MediaHoldFrameMode holdFrameMode)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    mpe_Error ret = MPE_HN_ERR_INVALID_PARAM;

    // Look for player or server which is associated this supplied session ID
    HnServer* server = NULL;
    HnPlayer* player = hn_findPlayerWithPlaybackSession(playbackSession);
    if (NULL == player)
    {
        server = hn_findServerWithPlaybackSession(playbackSession);
    }
    if (NULL != player)
    {
        return hnPlayer_playbackStop(player, playbackSession, holdFrameMode);
    }
    else if (NULL != server)
    {
        return hnServer_playbackStop(server, playbackSession);
    }
    else
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_HN,
                "%s() - no player or server found associated with play back session %p\n",
                __FUNCTION__, playbackSession);
    }

    return ret;
}

/**
 *  <i>mpeos_hnPlaybackGetTime()</i>
 *
 *  This function returns the current play back time. This time
 *  represents how long the playback has been ongoing.
 *
 *  @param playbackSession  an active playback handle.
 *  @param mediaTime            returned media time in nanoseconds.
 *
 * @event
 *   None.
 *
 * @return MPE_HN_ERR_NOERR         if successful
 *         MPE_HN_ERR_INVALID_PARAM if a parameter is invalid
 *         MPE_HN_ERR_OS_FAILURE    os specific failures
 */
mpe_Error mpeos_hnPlayerPlaybackGetTime(mpe_HnPlaybackSession playbackSession,
        int64_t* mediaTimeNanos)
{
    //MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    mpe_Error ret = MPE_HN_ERR_INVALID_PARAM;

    // Look for player or server which is associated this supplied session ID
    HnServer* server = NULL;
    HnPlayer* player = hn_findPlayerWithPlaybackSession(playbackSession);
    if (NULL == player)
    {
        server = hn_findServerWithPlaybackSession(playbackSession);
    }
    if (NULL != player)
    {
        ret = hnPlayer_playbackGetTime(player, playbackSession, mediaTimeNanos);
    }
    else if (NULL != server)
    {
        // *TODO* - need to query the pipeline to get current time position
        *mediaTimeNanos = 0;
        ret = MPE_HN_ERR_NOERR;
    }
    else
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_HN,
                "%s() - no player or server found associated with play back session %p\n",
                __FUNCTION__, playbackSession);
    }

    return ret;
}

/**
 *  <i>mpeos_hnPlaybackSetTime()</i>
 *
 * This function sets the playback position in the playback stream.
 *
 * @param playbackSession       an active playback handle.
 * @param mediaTime         position to jump to (in nanoseconds).
 *
 * @event
 *   None.
 * @return MPE_HN_ERR_NOERR         if successful
 *         MPE_HN_ERR_INVALID_PARAM if a parameter is invalid
 *         MPE_HN_ERR_OS_FAILURE    os specific failures
 */
/*
mpe_Error mpeos_hnPlaybackSetTime(mpe_HnPlaybackSession playbackSession,
        int64_t mediaTimeNanos)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    mpe_Error ret = MPE_HN_ERR_INVALID_PARAM;

    // Look for player or server which is associated this supplied session ID
    HnServer* server = NULL;
    HnPlayer* player = hn_findPlayerWithPlaybackSession(playbackSession);
    if (NULL == player)
    {
        server = hn_findServerWithPlaybackSession(playbackSession);
    }
    if (NULL != player)
    {
        //reset usedefault flag (only applicable on initial start)
        player->useDefaultMediaTime = FALSE;
        return hnPlayer_playbackSetTime(player, playbackSession, mediaTimeNanos);
    }
    else if (NULL != server)
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_HN,
                "%s() - server doesn't support setTime(), changeParams method should be used instead session\n",
                __FUNCTION__);
    }
    else
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_HN,
                "%s() - no player or server found associated with play back session %p\n",
                __FUNCTION__, playbackSession);
    }

    return ret;
}
*/
/**
 *  <i>mpeos_hnPlaybackSetRate()</i>
 *
 *  This function controls the playback speed and direction.
 *  The playback rate may not be supported by the OS.
 *  In this case the closest supported play rate is set and returned.
 *
 *  @param playbackSession  handle to a HN playback
 *  @param mode             desired trick mode
 *  @param actualMode       the actual rate set by the system.
 *
 * @event
 *   None.
 * @return MPE_HN_ERR_NOERR         if successful
 *         MPE_HN_ERR_INVALID_PARAM if a parameter is invalid
 *         MPE_HN_ERR_OS_FAILURE    os specific failures
 */
/*
mpe_Error mpeos_hnPlaybackSetRate(mpe_HnPlaybackSession playbackSession,
        float rate, float* actualRate)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    mpe_Error ret = MPE_HN_ERR_INVALID_PARAM;

    // Look for player or server which is associated this supplied session ID
    HnServer* server = NULL;
    HnPlayer* player = hn_findPlayerWithPlaybackSession(playbackSession);
    if (NULL == player)
    {
        server = hn_findServerWithPlaybackSession(playbackSession);
    }

    if (NULL != player)
    {
       //reset usedefault flag (only applicable on initial start)
       player->useDefaultMediaTime = FALSE;
        return hnPlayer_playbackSetRate(player, playbackSession, rate,
                actualRate);
    }
    else if (NULL != server)
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_HN,
                "%s() - server doesn't support setRate(), changeParams method should be used instead session\n",
                __FUNCTION__);
    }
    else
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_HN,
                "%s() - no player or server found associated with play back session %p\n",
                __FUNCTION__, playbackSession);
    }

    return ret;
}
*/
/**
 *  <i>mpeos_hnPlaybackGetRate()</i>
 *
 *  This function returns the current playback rate associated with a given playback.
 *
 *  @params playbackSession Handle to a HN playback
 *  @params pointer to the current playback rate
 *
 *
 *  @return MPE_HN_ERR_NOERR            if successful
 *         MPE_HN_ERR_INVALID_PARAM     if a parameter is invalid
 *         MPE_HN_ERR_OS_FAILURE        os specific failures
 */
mpe_Error mpeos_hnPlayerPlaybackGetRate(mpe_HnPlaybackSession playbackSession,
        float* rate)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    mpe_Error ret = MPE_HN_ERR_INVALID_PARAM;

    // Look for player or server which is associated this supplied session ID
    HnServer* server = NULL;
    HnPlayer* player = hn_findPlayerWithPlaybackSession(playbackSession);
    if (NULL == player)
    {
        server = hn_findServerWithPlaybackSession(playbackSession);
    }
    if (NULL != player)
    {
        *rate = player->activePlayback->rate;
        ret = MPE_HN_ERR_NOERR;
    }
    else if (NULL != server)
    {
        *rate = server->activePlayback->rate;
        ret = MPE_HN_ERR_NOERR;
    }
    else
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_HN,
                "%s() - no player or server found associated with play back session %p\n",
                __FUNCTION__, playbackSession);
    }

    return ret;
}

/**
 * <i>mpeos_hnPlaybackChangePIDs()</i>
 *
 * This function will change the AV stream parameters used for decoding
 * and video display on the player.  This method is not applicable to server.
 *
 * @param playbackSession     is the playback session to be updated
 * @param pids                new video and audio PID types and values
 *
 * @return MPE_HN_ERR_NOERR         if successful
 *     MPE_HN_ERR_INVALID_PARAM if a parameter is invalid
 *     MPE_HN_ERR_OS_FAILURE    os specific failures
 */
mpe_Error mpeos_hnPlayerPlaybackChangePIDs(
        mpe_HnPlaybackSession playbackSession,
        mpe_HnHttpHeaderAVStreamParameters * pids)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    mpe_Error ret = MPE_HN_ERR_INVALID_PARAM;

    // Look for player which is associated this supplied session ID
    HnPlayer* player = hn_findPlayerWithPlaybackSession(playbackSession);
    if (NULL != player)
    {
        return hnPlayer_playbackChangePIDs(player, playbackSession, pids);
    }
    else
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_HN,
                "%s() - no player found associated with play back session %p\n",
                __FUNCTION__, playbackSession);
    }

    return ret;
}

/**
 * <i>mpeos_hnPlaybackUpdateCCI()</i>
 *
 * This function updates the copy control information for an
 * active client playback session. This method is not applicable to server.
 *
 * @param playbackSession     is the playback session to be updated
 * @param cciDescSize         number of mpe_HnPlaybackTransportCCI elements
 *                            in the structure that follows
 * @param cciDescData         updated CCI descriptor information
 *
 * @return MPE_HN_ERR_NOERR         if successful
 *     MPE_HN_ERR_INVALID_PARAM if a parameter is invalid
 *     MPE_HN_ERR_OS_FAILURE    os specific failures
 **/
mpe_Error mpeos_hnPlayerPlaybackUpdateCCI(mpe_HnPlaybackSession playbackSession,
        uint32_t cciDescSize, mpe_HnPlaybackTransportCCI* cciDescData)
{
    //TODO_MPEOS Implement me!
    return MPE_HN_ERR_NOT_IMPLEMENTED;
}

/**
 * <i>mpeos_hnPlaybackBlockPresentation()</i>
 *
 * Block or unblock the presentation of the desired hn playback session.  Blocking a playback
 * session is accomplished by muting audio and displaying a black video area.
 * The playback session continues to process the stream as expected, however, the audio
 * is not emitted and the video is not displayed.   This method controls the blocking
 * state of the decode session by either blocking or unblocking the audio/video output.
 *
 * @param session - handle to the media decode session to be block / unblocked
 * @param block - boolean indicating whether to block (TRUE) or unblock (FALSE)
 *
 * @event
 *   None.
 *
 * @return MPE_HN_ERR_NOERR         if successful
 *     MPE_HN_ERR_INVALID_PARAM if a parameter is invalid
 *     MPE_HN_ERR_OS_FAILURE    os specific failures
 */
mpe_Error mpeos_hnPlayerPlaybackBlockPresentation(
        mpe_HnPlaybackSession playbackSession, mpe_Bool block)
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
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "mpeos_hnPlayerPlaybackBlockPresentation() - failed to get display\n");
            return MPE_EINVAL;
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "mpeos_hnPlayerPlaybackBlockPresentation() - failed to get pipeline manager\n");
        return MPE_EINVAL;
    }

    return MPE_SUCCESS;
}

/**
 * mpeos_hnServerUpdateEndPosition() adjusts the end byte position of the
 * currently-presenting stream.
 *
 * @param playbackSession    Active playback session to be updated.
 * @param endBytePosition    The new end byte position.  A value representing a
 *                           byte position that has already been streamed
 *                           should behave the same as if the request was fully
 *                           satisfied, including MPE_HN_EVT_END_OF_CONTENT
 *                           being sent to the event queue. A value of -1
 *                           should result in streaming of all available
 *                           content.
 *
 * @return MPE_HN_ERR_NOERR            If successful.
 *         MPE_HN_ERR_INVALID_PARAM    If one of the parameters is invalid.
 *         MPE_HN_ERR_OS_FAILURE       OS-specific failures.
 **/
mpe_Error mpeos_hnServerUpdateEndPosition(mpe_HnPlaybackSession
        playbackSession, int64_t endBytePosition)
{
    // Use the first server since it is essentially a static method
    HnServer* server = NULL;
    server = hn_findServerWithPlaybackSession(playbackSession);
        
    return hnServer_playbackUpdateEndPosition(server, endBytePosition);
}

/**
 *  <i>mpeos_hnGetRecordingContentItemLocalSize()</i>
 *
 * mpeos_hnGetRecordingContentItemLocalSize() should return the total number of bytes of
 * content streamed across the network associated with the recording matching the
 * supplied name.
 *
 * @param recordingName     name of the recording.
 * @param fileSizeBytes     total number of bytes to be streamed across the network for
 *                          this recording
 *
 * @events
 *    None.
 *
 * @return MPE_HN_ERR_NOERR         if successful
 *         MPE_HN_ERR_INVALID_PARAM if a parameter is invalid
 *         MPE_HN_ERR_OS_FAILURE    os specific failures
 */
mpe_Error mpeos_hnServerGetNetworkContentItemSize(mpe_HnStreamContentLocation contentLocation,
                                            void* contentDescription,
                                            char* profileIdStr,
                                            char* mimeTypeStr,
                                            mpe_hnContentTransformation* transformation,
                                            int64_t* fileSizeBytes)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    // Use the first server since it is essentially a static method
    return hnServer_getNetworkContentItemSize(&g_servers[0], contentLocation, contentDescription,
                                              profileIdStr, mimeTypeStr, transformation, fileSizeBytes);
}

/**
 * Refer to mpeos_hn.h for full method description.
 **/
mpe_Error mpeos_hnServerGetNetworkBytePosition(
        mpe_HnStreamContentLocation contentLocation, void * contentDescription,
        char * profileIDStr, char * mimeTypeStr, mpe_hnContentTransformation * transformation,
        int64_t localBytePosition, int64_t * networkBytePosition)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    // Use the first server since it is essentially a static method
    return hnServer_getNetworkBytePosition(&g_servers[0], contentLocation, contentDescription,
                                           profileIDStr, mimeTypeStr, transformation,
                                           localBytePosition,
                                           networkBytePosition);
}

/**
 *  <i>mpeos_hnGetNetworkBytePositionForMediaTimeMS()</i>
 *
 * mpeos_hnGetNetworkBytePositionForMediaTimeNS() should return the total number of bytes of
 * content streamed across the network associated with the supplied content item using
 * the profile identified by the supplied string.
 *
 * @param contentInfo       content item to be transfered across network
 * @param profileIDStr      format to be used in network transfer of this content
 * @param mediaTimeNS       media time in nanoseconds
 * @param fileSizeBytes     byte position of content streamed across the network for
 *                          this media time
 *
 * @events
 *    None.
 *
 * @return MPE_HN_ERR_NOERR         if successful
 *         MPE_HN_ERR_INVALID_PARAM if a parameter is invalid
 *         MPE_HN_ERR_OS_FAILURE    os specific failures
 *
 */
mpe_Error mpeos_hnServerGetNetworkBytePositionForMediaTimeNS(mpe_HnStreamContentLocation contentLocation,
                                                       void * contentDescription,
                                                       char * profileIDStr,
                                                       char * mimeTypeStr,
                                                       mpe_hnContentTransformation* transformation,
                                                       int64_t mediaTimeNS,
                                                       int64_t * bytePosition)
{
    //TODO: workaround to handle the case where the requested mediatime is in an in-memory buffer (requesting byte position of 'live-point')
    int i;
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    mpe_Error err;

    for (i=0; i<6; i++)
    {
        // Use the first server since it is essentially a static method
        err = hnServer_getNetworkBytePositionForMediaTimeNS(&g_servers[0], contentLocation, contentDescription,
                                                  profileIDStr, mimeTypeStr, transformation, mediaTimeNS, bytePosition);
        if (err == MPE_HN_ERR_NOERR)
        {
            break;
        }
        mpeos_threadSleep(10,0);
    }
    return err;
}

/**
 * Refer to mpeos_hn.h for full method description.
 **/
mpe_Error mpeos_hnServerGetConnectionStallingFlag(mpe_HnStreamContentLocation contentLocation,
                                                  void * contentDescription,
                                                  char * profileIDStr,
                                                  char * mimeTypeStr,
                                                  mpe_hnContentTransformation* transformation,
                                                  mpe_Bool * connectionStallingSupported)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    // Use the first server since it is essentially a static method
    return hnServer_getConnectionStallingFlag(contentLocation, contentDescription,
                                           profileIDStr, mimeTypeStr, transformation, connectionStallingSupported);
}

/**
 * Refer to mpeos_hn.h for full method description.
 **/
mpe_Error mpeos_hnServerGetServerSidePacingRestampFlag(mpe_HnStreamContentLocation contentLocation,
                                                  void * contentDescription,
                                                  char * profileIDStr,
                                                  char * mimeTypeStr,
                                                  mpe_hnContentTransformation* transformation,
                                                  mpe_Bool * willRestamp)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    MPE_UNUSED_PARAM(transformation);

    // Use the first server since it is essentially a static method
    return hnServer_getServerSidePacingRestampFlag(contentLocation, contentDescription,
                                                profileIDStr, mimeTypeStr, willRestamp);
}

/**
 * <i>mpeos_hnGetMacAddress()</i>
 *
 * Return the mac address associated with the network interface whose
 * display name matches the supplied name.
 *
 * @param   ifDisplayName   find interface whose name matches this value
 * @param   macAddress      returned mac address associated with network interface
 *
 * @return  MPE_HN_ERR_NOERR        if successful
 *          MPE_HN_ERR_OS_FAILURE   if problems are encountered
 */
mpe_Error mpeos_hnGetMacAddress(char* displayName, char* macAddress)
{
    mpe_Error retCode = MPE_HN_ERR_NOERR;
    if (os_getMacAddress(displayName, macAddress) != 0)
    {
        retCode = MPE_HN_ERR_OS_FAILURE;
    }
    return retCode;
}

/**
 * <i>mpeos_hnGetNetworkInterfaceType()</i>
 *
 * Return the type of the network interface as defined in org.ocap.hn.NetworkInterface
 * associated with the network interface whose display name matches the supplied name.
 *
 * @param   ifDisplayName   find interface whose name matches this value
 * @param   type            returned type of network interface
 *
 * @return  MPE_HN_ERR_NOERR        if successful
 *          MPE_HN_ERR_OS_FAILURE   if problems are encountered
 */
mpe_Error mpeos_hnGetNetworkInterfaceType(char* displayName, int32_t* type)
{
    mpe_Error retCode = MPE_HN_ERR_NOERR;
    if (os_getNetworkInterfaceType(displayName, type) != 0)
    {
        retCode = MPE_HN_ERR_OS_FAILURE;
    }
    return retCode;
}

/**
 * <i>mpeos_hnPlaybackSetMute()</i>
 *
 * Set the mute
 */
mpe_Error mpeos_hnPlayerPlaybackSetMute(mpe_HnPlaybackSession playbackSession, mpe_Bool mute)
{
    MPE_UNUSED_PARAM(playbackSession);
    MPE_UNUSED_PARAM(mute);
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_hnPlaybackSetGain()</i>
 *
 * Set the gain
 */
mpe_Error mpeos_hnPlayerPlaybackSetGain(mpe_HnPlaybackSession playbackSession, float gain, float *actualGain)
{
    MPE_UNUSED_PARAM(playbackSession);
    MPE_UNUSED_PARAM(gain);
    *actualGain = gain;
    return MPE_SUCCESS;
}

mpe_Error mpeos_hnPlayerPlaybackGetVideoScanMode (mpe_HnPlaybackSession session, mpe_MediaScanMode* scanMode)
{
    *scanMode = g_videoScanMode;

    return (mpe_Error) MPE_SUCCESS;
}

mpe_Error mpeos_hnPlayerPlaybackGet3DConfig (mpe_HnPlaybackSession playbackSession, mpe_DispStereoscopicMode* formatType,
    mpe_Media3DPayloadType* payloadType, uint8_t* payload, uint32_t* payloadSz)
{
    if (*payloadSz < g_payloadSz)
    {
        *payloadSz = g_payloadSz;
        return (mpe_Error) MPE_ENOMEM;
    }

    *formatType = g_stereoscopicMode;
    *payloadType = g_payloadType;

    memcpy (payload, g_payload, g_payloadSz);

    *payloadSz = g_payloadSz;

    return (mpe_Error) MPE_SUCCESS;
}

/**
 * Utility function which finds either the player associated
 * with the supplied session info.
 *
 * @param   session  session id which is associated with player
 *
 * @return  player associated with supplied session, NULL if no player is
 * associated with session
 */
static HnPlayer* hn_findPlayerWithStreamSession(mpe_HnStreamSession session)
{
    // Extract session id from handle
    int32_t sessionID = (int32_t) session;

    // Look for a current session which matches this supplied session ID
    uint32_t i = 0;
    for (i = 0; i < g_numPlayers; i++)
    {
        if (g_players[i].streamSessionId == sessionID)
        {
            return (HnPlayer*) &g_players[i];
        }
    }

    return NULL;
}

/**
 * Utility function which finds the player associated
 * with the supplied session info.
 *
 * @param   session  session id which is associated with player
 *
 * @return  player associated with supplied session, NULL if no player is
 * associated with session
 */
static HnPlayer* hn_findPlayerWithPlaybackSession(
        mpe_HnPlaybackSession playbackSession)
{
    //MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() -- called\n", __FUNCTION__);

    os_HnPlaybackSession* session = (os_HnPlaybackSession*) playbackSession;

    // Extract session id from handle
    int32_t sessionID = session->handle;

    //MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN,
    //        "%s() - looking for player with pb id %d\n", __FUNCTION__,
    //        sessionID);

    // Look for a current session which matches this supplied session ID
    uint32_t i = 0;
    for (i = 0; i < g_numPlayers; i++)
    {
        //MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s() - player %d pb id %d\n",
        //        __FUNCTION__, (i + 1), g_players[i].activePlayback->handle);

        if (g_players[i].activePlayback->handle == sessionID)
        {
            return (HnPlayer*) &g_players[i];
        }
    }

    return NULL;
}

/**
 * Utility function which finds the server associated
 * with the supplied session info.
 *
 * @param   session  session id which is associated with server
 *
 * @return  server associated with supplied session, NULL if no server is
 * associated with session
 */
static HnServer* hn_findServerWithStreamSession(mpe_HnStreamSession session)
{
    // Extract session id from handle
    int32_t sessionID = (int32_t) session;

    // Look for a current session which matches this supplied session ID
    uint32_t i = 0;
    for (i = 0; i < g_numServers; i++)
    {
        if (g_servers[i].streamSessionId == sessionID)
        {
            return (HnServer*) &g_servers[i];
        }
    }

    return NULL;
}

/**
 * Utility function which finds the server associated
 * with the supplied session info.
 *
 * @param   session  session id which is associated with server
 *
 * @return  server associated with supplied session, NULL if no server is
 * associated with session
 */
static HnServer* hn_findServerWithPlaybackSession(
        mpe_HnPlaybackSession playbackSession)
{
    //MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() -- called\n", __FUNCTION__);

    os_HnPlaybackSession* session = (os_HnPlaybackSession*) playbackSession;

    // Extract session id from handle
    int32_t sessionID = session->handle;

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
            "%s() - looking for server with playback id %d\n", __FUNCTION__,
            sessionID);

    // Look for a current session which matches this supplied session ID
    uint32_t i = 0;
    for (i = 0; i < g_numServers; i++)
    {
        if (g_servers[i].activePlayback->handle == sessionID)
        {
            return (HnServer*) &g_servers[i];
        }
    }

    return NULL;
}

mpe_Error mpeos_hnPlayerPlaybackInit3DConfig ()
{
    ri_display_t* display = NULL;
    int32_t formatTypeTemp;
    int32_t payloadTypeTemp;
    int32_t scanModeTemp;

    ri_pipeline_manager_t* pMgr = ri_get_pipeline_manager();
    if (NULL != pMgr)
    {
        display = pMgr->get_display(pMgr);
        if (NULL != display)
        {
            int nReturnCode = display->get_threedtv_info(display, &formatTypeTemp, &payloadTypeTemp,
                &g_payloadSz, NULL, &scanModeTemp);
            if (nReturnCode != 0)
            {
                g_payload = (uint8_t*) malloc (g_payloadSz);

                nReturnCode = display->get_threedtv_info(display, &formatTypeTemp, &payloadTypeTemp,
                    &g_payloadSz, g_payload, &scanModeTemp);
                if (nReturnCode == 0)
                {
                    g_payloadType = payloadTypeTemp;
                    g_stereoscopicMode = formatTypeTemp;
                    g_videoScanMode = scanModeTemp;
                }
                else
                {
                    MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                            "HN: could not retrieve 3DTV settings -- 1\n");
                    return MPE_EINVAL;
                }
            }
            else
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                        "HN: could not retrieve 3DTV settings -- 2\n");
                return MPE_EINVAL;
            }
        }
        else
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "HN: could not retrieve 3DTV settings -- 3\n");
            return MPE_EINVAL;
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "HN: could not retrieve pipeline manager -- 4\n");
        return MPE_EINVAL;
    }
        
    return (mpe_Error) MPE_SUCCESS;
}

/**
 * Returns the number of DLNA profile IDs that the platform player is able to support.
 *
 * @param profileIDCnt          Returns number/count of player-supported DLNA profile IDs
 *
 * @return MPE_SUCCESS if successful.
*/
mpe_Error mpeos_hnPlayerGetDLNAProfileIDsCnt(uint32_t* profileIDCnt)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    return hnPlayer_getDLNAProfileIDsCnt(profileIDCnt);
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
mpe_Error mpeos_hnPlayerGetDLNAProfileIDStr(uint32_t idx, char profileIDStr[MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE])
{
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    return hnPlayer_getDLNAProfileIDStr(idx, profileIDStr);
}

mpe_Error mpeos_hnPlayerGetMimeTypesCnt(char * profileIDStr, uint32_t * mimeTypeCnt)
{
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    return hnPlayer_getMimeTypesCnt(profileIDStr, mimeTypeCnt);
}

mpe_Error mpeos_hnPlayerGetMimeTypeStr( char * profileIDStr, uint32_t idx,
                                        char mimeTypeStr[MPE_HN_MAX_MIME_TYPE_STR_SIZE])
{
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    return hnPlayer_getMimeTypeStr(profileIDStr, idx, mimeTypeStr);
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
mpe_Error mpeos_hnPlayerGetPlayspeedsCnt(char* profileIDStr, char* mimeTypeStr, uint32_t* playspeedCnt)
{
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    return hnPlayer_getPlayspeedsCnt(profileIDStr, mimeTypeStr, playspeedCnt);
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
mpe_Error mpeos_hnPlayerGetPlayspeedStr(char* profileIDStr, char* mimeTypeStr, uint32_t idx,
                                        char playspeedStr[MPE_HN_MAX_PLAYSPEED_STR_SIZE])
{
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    return hnPlayer_getPlayspeedStr(profileIDStr, mimeTypeStr, idx, playspeedStr);
}

/**
 * Refer to mpeos_hn.h for full method description.
 **/
mpe_Error mpeos_hnPlayerPlaybackPause(
        mpe_HnPlaybackSession playbackSession)
{
    mpe_Error ret = MPE_HN_ERR_INVALID_PARAM;

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    // Look for player which is associated this supplied session ID
    HnPlayer* player = hn_findPlayerWithPlaybackSession(playbackSession);
    if (NULL != player)
    {
        return hnPlayer_playbackPause(player, playbackSession);
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - no player found associated with play back session %p\n",
                __FUNCTION__, playbackSession);
    }

    return ret;
}

/**
 * Refer to mpeos_hn.h for full method description.
 **/
mpe_Error mpeos_hnPlayerPlaybackResume(
        mpe_HnPlaybackSession playbackSession)
{
    mpe_Error ret = MPE_HN_ERR_INVALID_PARAM;

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    // Look for player which is associated this supplied session ID
    HnPlayer* player = hn_findPlayerWithPlaybackSession(playbackSession);
    if (NULL != player)
    {
        return hnPlayer_playbackResume(player, playbackSession);
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - no player found associated with play back session %p\n",
                __FUNCTION__, playbackSession);
    }

    return ret;
}

/**
 * <i>mpeos_hnPing()</i>
 *
 * Return the results of a ping 
 *
 * @param   testID     id associated with the test 
 * @param   host       host to ping 
 * @param   reps       number of requests to send 
 * @param   interval   time in msec between requests 
 * @param   timeout    timeout in msec to wait for reply 
 * @param   blocksize  send buffer size 
 * @param   dscp       DSCP value 
 * @param   status     returned status string
 * @param   info       returned additional info 
 * @param   successes  returned number of successful pings 
 * @param   fails      returned number of failed pings 
 * @param   avg        returned average time 
 * @param   min        returned minimum time 
 * @param   max        returned maximum time 
 *
 * Refer to mpeos_hn.h for full method description.
 *
 * @return  MPE_HN_ERR_NOERR        if successful
 *          MPE_HN_ERR_OS_FAILURE   if problems are encountered
 */
mpe_Error mpeos_hnPing(int32_t testID, char* host, int32_t reps, int32_t interval, int32_t timeout, int32_t blocksize, int32_t dscp, char *status, char *info, int *successes, int *fails, int *avg, int *min, int *max)
{
    mpe_Error retCode = MPE_HN_ERR_NOERR;
    if (os_Ping(testID, host, reps, interval, timeout, blocksize, dscp, status,
        info, successes, fails, avg, min, max) != 0)
    {
        retCode = MPE_HN_ERR_OS_FAILURE;
    }
    return retCode;
}

/**
 * <i>mpeos_hnTraceroute()</i>
 *
 * Return the results of a traceroute 
 *
 * @param   testID     id associated with the test 
 * @param   host       host
 * @param   hops       max number of hops 
 * @param   timeout    timeout in msec to wait for reply 
 * @param   blocksize  send buffer size 
 * @param   dscp       DSCP value 
 * @param   status     returned status 
 * @param   info       returned additional info 
 * @param   avgresp    returned average resp time 
 * @param   hophosts   returned hosts found 
 *
 * Refer to mpeos_hn.h for full method description.
 *
 * @return  MPE_HN_ERR_NOERR        if successful
 *          MPE_HN_ERR_OS_FAILURE   if problems are encountered
 */
mpe_Error mpeos_hnTraceroute(int32_t testID, char* host, int32_t hops, int32_t timeout, int32_t blocksize, int32_t dscp, char *status, char *info, int *avgresp, char *hophosts)
{
    mpe_Error retCode = MPE_HN_ERR_NOERR;
    if (os_Traceroute(testID, host, hops, timeout, blocksize, dscp,
        status, info, avgresp, hophosts) != 0)
    {
        retCode = MPE_HN_ERR_OS_FAILURE;
    }
    return retCode;
}

/**
 * <i>mpeos_hnNSLookup()</i>
 *
 * Return the results of a nslookup 
 *
 * @param   testID         id associated with the test 
 * @param   host           host
 * @param   server         DNS server 
 * @param   timeout        timeout in msec to wait for reply 
 * @param   status         returned status 
 * @param   info           returned additional info 
 * @param   resultAnswer   returned DNS answer type 
 * @param   resultName     returned fully qualified host name 
 * @param   resultIPS      returned IP addresses returned DNS server 
 * @param   resultServer   returned DNS Server IP address 
 * @param   resultTime     returned time on msec of response 
 *
 * Refer to mpeos_hn.h for full method description.
 *
 * @return  MPE_HN_ERR_NOERR        if successful
 *          MPE_HN_ERR_OS_FAILURE   if problems are encountered
 */
mpe_Error mpeos_hnNSLookup(int32_t testID, char* host, char *server, int32_t timeout, char *status, char *info, char *resultAnswer, char *resultName, char *resultIPS, char *resultServer, int *resultTime)
{
    mpe_Error retCode = MPE_HN_ERR_NOERR;
    if (os_NSLookup(testID, host, server, timeout, status, info, resultAnswer,
        resultName, resultIPS, resultServer, resultTime) != 0) 
    {
        retCode = MPE_HN_ERR_OS_FAILURE;
    }
    return retCode;
}

/**
 * <i>mpeos_hnCancelTest()</i>
 *
 * Cancels a Ping/Traceroute or NSLookup Test 
 *
 * Refer to mpeos_hn.h for full method description.
 *
 * @return  MPE_HN_ERR_NOERR        if successful
 *          MPE_HN_ERR_OS_FAILURE   if problems are encountered
 */
mpe_Error mpeos_hnCancelTest(int32_t testID)
{
    mpe_Error retCode = MPE_HN_ERR_NOERR;
    if (os_CancelTest(testID) != 0)
    {
        retCode = MPE_HN_ERR_OS_FAILURE;
    }
    return retCode;
}


