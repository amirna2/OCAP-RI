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
 * Server side implementation of mpeos HN APIs
 */
#include <stdio.h>
#include <inttypes.h>
#include <string.h>

#include <ri_pipeline.h>
#include <ri_test_interface.h>

#include "mpe_file.h"
#include "mpeos_dbg.h"

#include "platform_common.h"
#include "platform_dvr.h"

#include "hn_server.h"
#include "hn_server_send_thread.h"
#include "hn_dtcpip.h"

#define INCREASE_HN_SERVER_THREAD_PRIORITY
#define INCREASE_SOCKET_SND_BUF_SIZE // defining sets Win32 sock bufsiz == Linux

static mpe_Error hnServer_streamStoreParams(HnServer* server,
        mpe_HnStreamParams* streamInfo);

static mpe_Error hnServer_playbackStoreParams(HnServer* server,
        mpe_HnPlaybackParams* playbackInfo, mpe_Bool* isDVR);

static mpe_Error hnServer_getFileLocationRecording(
        mpe_HnStreamLocalSVContentDescription* contentDescription,
        char* path, char* filename);

static mpe_Error hnServer_getFileLocationLocal(
        mpe_HnStreamAppContentDescription* contentDescription,
        char* path, char* filename);

static mpe_Error hnServer_getFileLocationTsb(HnServer* server,
        mpe_HnStreamTSBContentDescription* contentDescription,
        char* path, char* filename);

static mpe_Error hnServer_terminateThread(HnServer* server);

static int convertRecordingNameToPlatform(const char* recordingName,
        char path[OS_DVR_MEDIA_VOL_MAX_PATH_SIZE],
        char platformRecName[RI_MAX_RECORDING_NAME_LENGTH]);

static int expandRecordingName(const char* recordingName,
        char location[OS_FS_MAX_PATH],
        char platformRecName[RI_MAX_RECORDING_NAME_LENGTH]);

static mpe_Bool hnServer_isProfileValid(mpe_HnStreamContentLocation contentLocation,
        void* contentDescription, char* profileId, char* mimeType);

// Exclude npt and byte position chunk field headers which are used by RI HN Players
// to maintain stream position.
static mpe_Bool g_excludeExtraChunkHdrs = FALSE;

#ifdef MPE_FEATURE_DVR
extern mpe_Bool isRecordingInProgress(char *recordingName);
#endif

#define TRANSFORMATION_MENU \
    "\r\n" \
    "|---+----------------------- \r\n" \
    "| a | Add transformation\r\n" \
    "|---+----------------------- \r\n" \
    "| r | Remove transformation\r\n" \
    "|---+----------------------- \r\n" \
    "| s | transformation Status\r\n" \
    "|---+----------------------- \r\n" \
    "| t | set next Transformation\r\n" \

static HnServer* g_test_server = NULL; 
static ri_transformation_t* nextTransform = NULL;

static int test_InputHandler(int sock, char *rxBuf, int *retCode, char **retStr)
{
    char buffer[FILENAME_MAX];
    mpe_Error ret = MPE_HN_ERR_NOERR;
    ri_transformation_t *transformation = NULL;
    int bytes = 0;

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s(%d, %s);\n", __func__, sock, rxBuf);
    *retCode = MENU_SUCCESS;

    switch (rxBuf[0])
    {
    case 'a': // Add transformation...
        ri_test_SendString(sock,"\r\nAdd transformation...\r\n");

        if (MPE_SUCCESS != mpe_memAlloc(sizeof(ri_transformation_t),
                                        (void**) &transformation))
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s - Cannot allocate transformation\n", __FUNCTION__);
            break;
        }

        strcpy(transformation->transformedProfileStr, "AVC_TS_NA_ISO");
        transformation->bitrate = 3000000;
        transformation->width = 720;
        transformation->height = 480;
        transformation->progressive = TRUE;
        (void)ri_test_GetString(sock, buffer, FILENAME_MAX-1,
                                "\r\nSelect 'T' for tuner or 'F' for file: ");
        if ('T' == buffer[0] || 't' == buffer[0])
        {
            ret = g_test_server->pipeline->pipeline_transform_live_stream(
                                                     g_test_server->pipeline,
                                                     g_test_server->tunerId,
                                                     transformation);
            if (ret != MPE_HN_ERR_NOERR)
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s - pipeline_transform_live_stream returned error: %d\n",
                    __func__, ret);
            }
        }
        else if ('F' == buffer[0] || 'f' == buffer[0])
        {
            ret = g_test_server->pipeline->pipeline_transform_file_stream(
                                                g_test_server->pipeline,
                                                g_test_server->path,
                                                g_test_server->platformFileName,
                                                transformation);
            if (ret != MPE_HN_ERR_NOERR)
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s - pipeline_transform_file_stream returned error: %d\n",
                    __func__, ret);
            }
        }
        break;
 
    case 'r': // Remove transformation...
        if (NULL != nextTransform)
        {
            sprintf(buffer,
                    "removed transformation: %s, %dbps, (%dx%d), prog:%d",
                    nextTransform->transformedProfileStr,
                    nextTransform->bitrate,
                    nextTransform->width,
                    nextTransform->height,
                    nextTransform->progressive);
            mpe_memFree(nextTransform);
            nextTransform = NULL;
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s\n", buffer);
            ri_test_SendString(sock, buffer);
            break;
        }

        ri_test_SendString(sock,"\r\nRemove transformation...\r\n");
        (void)ri_test_GetString(sock, buffer, FILENAME_MAX-1,
                                "\r\nSelect 'T' for tuner or 'F' for file: ");
        if ('T' == buffer[0] || 't' == buffer[0])
        {
            ret = g_test_server->pipeline->pipeline_transform_live_stream(
                                                     g_test_server->pipeline,
                                                     g_test_server->tunerId,
                                                     NULL);
            if (ret != MPE_HN_ERR_NOERR)
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s - pipeline_transform_live_stream returned error: %d\n",
                    __func__, ret);
            }
        }
        else if ('F' == buffer[0] || 'f' == buffer[0])
        {
            ret = g_test_server->pipeline->pipeline_transform_file_stream(
                                                g_test_server->pipeline,
                                                g_test_server->path,
                                                g_test_server->platformFileName,
                                                NULL);
            if (ret != MPE_HN_ERR_NOERR)
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s - pipeline_transform_file_stream returned error: %d\n",
                    __func__, ret);
            }
        }
        break;
 
    case 's': // transformation Status...
        ri_test_SendString(sock,"\r\ntransformation Status...\r\n");

        if (NULL != nextTransform)
        {
            sprintf(buffer, "next transformation: %s, %dbps, (%dx%d), prog:%d",
                    nextTransform->transformedProfileStr,
                    nextTransform->bitrate,
                    nextTransform->width,
                    nextTransform->height,
                    nextTransform->progressive);
        }
        else
        {
            ret = g_test_server->pipeline->pipeline_transform_status(
                                                        g_test_server->pipeline,
                                                        buffer, sizeof(buffer));
            if (ret != MPE_HN_ERR_NOERR)
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                          "%s - pipeline_transform_status returned error: %d\n",
                          __func__, ret);
                break;
            }
        }

        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s\n", buffer);
        ri_test_SendString(sock, buffer);
        break;

    case 't': // set next Transformation...
        ri_test_SendString(sock,"\r\nset next Transformation...\r\n");

        if (MPE_SUCCESS != mpe_memAlloc(sizeof(ri_transformation_t),
                                        (void**) &nextTransform))
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s - Cannot allocate transformation\n", __FUNCTION__);
            break;
        }

        bytes = ri_test_GetString(sock, buffer, FILENAME_MAX-1,
                                  "\r\nprofile string (AVC_TS_NA_ISO): ");
        if (bytes < 3)
        {
            strcpy(nextTransform->transformedProfileStr, "AVC_TS_NA_ISO");
        }
        else
        {
            strcpy(nextTransform->transformedProfileStr, buffer);
        }

        nextTransform->bitrate = ri_test_GetNumber(sock, buffer,
                                                   FILENAME_MAX-1,
                                                   "\r\nbitrate (1000000) ",
                                                   1000000);
        nextTransform->width = ri_test_GetNumber(sock, buffer,
                                                   FILENAME_MAX-1,
                                                   "\r\nwidth (720) ", 720);
        nextTransform->height = ri_test_GetNumber(sock, buffer,
                                                   FILENAME_MAX-1,
                                                   "\r\nheight (480) ", 480);
        nextTransform->progressive = ri_test_GetNumber(sock, buffer,
                                                   FILENAME_MAX-1,
                                                   "\r\nprogressive (1) ", 1);
        sprintf(rxBuf, "\r\nnext transformation: %s, %dbps, (%dx%d), prog:%d\n",
                nextTransform->transformedProfileStr,
                nextTransform->bitrate,
                nextTransform->width,
                nextTransform->height,
                nextTransform->progressive);
        ri_test_SendString(sock, rxBuf);
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s\n", rxBuf);
        break;

    case 'x': // Exit
        return -1;

    default:
        strcat(rxBuf, " - unrecognized\r\n\n");
        ri_test_SendString(sock, rxBuf);
        *retCode = MENU_INVALID;
        break;
    } 

    return 0;
} 

static MenuItem transformation_menu =
{ false, "t", "Transformation Tests", TRANSFORMATION_MENU, test_InputHandler};

/**
 * Initialize the HN servers
 *
 * @return MPE_HN_ERR_NOERR if the server initialization is successful
 * otherwise, MPE_HN_ERR_OS_FAILURE.
 */
mpe_Error hnServer_init(HnServer* server, ri_pipeline_t* pipeline, uint32_t idx)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    if (NULL == pipeline)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s(): unable to get HN pipeline %d\n", __FUNCTION__, idx);
        return MPE_HN_ERR_OS_FAILURE;
    }
    server->pipeline = pipeline;
    g_test_server = server; 

    if (!ri_test_RegisterMenu(&transformation_menu))
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                  "%s: Error registering transformation_menu\n", __FUNCTION__);
    }

    // Initialize our data structures
    server->serverId = idx + 1;
    server->state = HN_STATE_IDLE;

    // Create mutex
    if (mpeos_mutexNew(&server->mutex) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - Could not create mutex for server %d\n", __FUNCTION__,
                idx);
        return MPE_HN_ERR_OS_FAILURE;
    }

    // Allocate playback structure and initialize
    if (mpe_memAlloc(sizeof(os_HnPlaybackSession),
            (void**) &server->activePlayback) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s - Cannot allocate new playback structure\n", __FUNCTION__);
        return MPE_HN_ERR_OS_FAILURE;
    }
    server->activePlayback->handle = 0;
    server->activePlayback->rate = 0.0;
    server->activePlayback->cci = (uint8_t) 0x3; // RCT=0b; CIT=0b; APS=00b; EMI=11b

    // Initialize stream and playback params
    server->act = NULL;
    server->queueID = 0;
    server->streamSessionId = 0;
    server->dtcpipSessionHandle = DTCPIP_INVALID_SESSION_HANDLE;
    server->streamThreadExitPending = FALSE;
    server->hadEOS = FALSE;
    server->streamThread = NULL;
    server->contentLocationType = MPE_HN_CONTENT_LOCATION_UNKNOWN;
    server->socket = MPE_SOCKET_INVALID_SOCKET;
    server->chunkedEncodingMode = MPE_HN_CHUNKED_ENCODING_MODE_NONE;
    memset(server->dlnaProfileId, 0, OS_HN_MAX_DLNA_PROFILE_ID_STR_SIZE);
    memset(server->mimeType, 0, OS_HN_MAX_MIME_TYPE_STR_SIZE);
    memset((void *) &(server->transformation), 0, sizeof(mpe_hnContentTransformation));
    server->startBytePosition = 0;
    server->startSkipBytes = 0;
    server->endBytePosition = 0;
    server->startTimePosition = 0;
    server->endTimePosition = 0;
    server->rangeByteCnt = 0;
    server->excludeExtraChunkHdrs = g_excludeExtraChunkHdrs;
    server->connectionStallingTimeoutMS = -1;

    return MPE_HN_ERR_NOERR;
}

/**
 * Initialize parameters based on environment variable values
 */
void hnServer_initEnv()
{
    // Experimental option which will disable the extra header fields used
    // by the RI HN Player.  Other players should ignore chunk headers they
    // don't understand but if another player has problems with chunk encoding,
    // consider using this option to see if it helps.
    // Also, in the future if AV Transport UPnP Service is adopted to allow player to
    // determine position in HN Server stream, these extra header fields
    // can be removed and use of this parameter indicates where code used to
    // support these headers can be removed.
    const char* configValue = mpeos_envGet("HN.EXCLUDE.EXTRA.CHUNK.HEADERS");
    if (NULL != configValue)
    {
        g_excludeExtraChunkHdrs = (stricmp(configValue, "TRUE") == 0);
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
             "%s() - excluding extra headers from chunk encoding\n", __FUNCTION__);
    }
}

/**
 * Performs actions necessary to cache the info to begin the HN stream
 * server.
 *
 * @param openParams - allocation and populated streaming params.
 * @param queueId to post streaming related events
 * @param act is a context value for the event dispacher
 * @param streamingSession opaque handle to the streaming session.
 *
 * @event MPE_HN_EVT_SESSION_OPEN for a successfully opened server stream.
 *
 * @return MPE_HN_ERR_NOERR         if successful
 *         MPE_HN_ERR_INVALID_PARAM if a parameter is invalid
 *         MPE_HN_ERR_OS_FAILURE    os specific failures
 *         MPE_HN_ERR_CONTENT       if the failure has occurred while acquiring the content
 */
mpe_Error hnServer_streamOpen(HnServer* server, uint32_t sessionId,
        mpe_HnStreamParams* streamParams, mpe_EventQueue queueId, void * act,
        mpe_HnStreamSession* streamingSession)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() -- called for server %d\n",
            __FUNCTION__, server->serverId);

    mpe_Error ret = MPE_HN_ERR_NOERR;

    mpeos_mutexAcquire(server->mutex);

    // Save the stream session parameters
    ret = hnServer_streamStoreParams(server, streamParams);
    if (ret != MPE_HN_ERR_NOERR)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() -- Could not store stream session parameters\n",
                __FUNCTION__);
        mpeos_mutexRelease(server->mutex);
        return ret;
    }

    server->dtcpipSessionHandle = DTCPIP_INVALID_SESSION_HANDLE;
    if (server->dlnaProfileId[0] == 'D' &&
        server->dlnaProfileId[1] == 'T' &&
        server->dlnaProfileId[2] == 'C' &&
        server->dlnaProfileId[3] == 'P' &&
        server->dlnaProfileId[4] == '_')
    {
        //Assume content type audiovisual
        int dtcpip_ret = g_dtcpip_ftable->dtcpip_src_open(&server->dtcpipSessionHandle, 0);
        if (dtcpip_ret != 0)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() -- Failed to open DTCP/IP server session with error code %d\n",
                __FUNCTION__, dtcpip_ret);
            return MPE_HN_ERR_CONTENT;
        }

        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
            "%s() -- Opened DTCP/IP server session - handle %d\n",
            __FUNCTION__, server->dtcpipSessionHandle);
    }

    // Return session id as the streaming session handle
    server->streamSessionId = sessionId;

    server->queueID = queueId;
    server->act = act;
    server->streamThreadExitPending = FALSE;
    server->hadEOS = FALSE;

    // Return current session id as handle
    *streamingSession = (mpe_HnStreamSession)server->streamSessionId;

    // Set server state to open
    server->state = HN_STATE_OPENED;

    // Send the stream open event
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - sending server session open event\n", __FUNCTION__);
    mpeos_eventQueueSend(server->queueID, MPE_HN_EVT_SESSION_OPEN, NULL,
            (void*) server->act, 0);

    mpeos_mutexRelease(server->mutex);

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() -- returning: %d\n",
            __FUNCTION__, MPE_HN_ERR_NOERR);

    return MPE_HN_ERR_NOERR;
}

/**
 * Allocate memory and store parameters associated with the stream/session
 * so they can be referenced during playback and returned in response to get info call.
 *
 * @param   server      server associated with this stream session
 * @param   streamInfo  server's stream session info to save
 *
 * @return  MPE_HN_ERR_NOERR if successful
 *          otherwise MPE_HN_ERR_OS_FAILURE
 */
static mpe_Error hnServer_streamStoreParams(HnServer* server,
        mpe_HnStreamParams* streamParams)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    mpe_HnStreamParamsMediaServerHttp* streamInfo = streamParams->streamParams;
    if (NULL == streamInfo)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() -- NULL stream info supplied\n", __FUNCTION__);
        return MPE_HN_ERR_OS_FAILURE;
    }

    // Store connection id in order to return in getStreamInfo()
    if (streamInfo->connectionId > 0)
    {
        server->connectionId = streamInfo->connectionId;
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                "%s() -- received connection id was %d\n", __FUNCTION__,
                server->connectionId);

        // Save the connection Id as a string also
        char tmpStr[64];
        sprintf(&tmpStr[0], "%u", server->connectionId);
        int size = strlen((char*) &tmpStr[0]);
        if (mpe_memAlloc(size+1, (void **) &server->connectionIdStr)!= MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s() - Could not allocated memory for connection id string\n", __FUNCTION__);
            mpeos_mutexRelease(server->mutex);
            return MPE_HN_ERR_OS_FAILURE;
        }
        else
        {
            strcpy(server->connectionIdStr, (char*) &tmpStr[0]);
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                    "%s() - connection id %d, len %d, string: %s\n",
                    __FUNCTION__, server->connectionId, size, server->connectionIdStr);
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                "%s() -- leaving connection id unset at %d\n", __FUNCTION__,
                server->connectionId);
    }

    // Save the supplied socket
    server->socket = streamInfo->socket;
    MPE_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() socket address:  = 0x%x\n",
            __FUNCTION__, server->socket);

    server->connectionStallingTimeoutMS = streamInfo->connectionStallingTimeoutMS;
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() -- connection Stalling Timeout MS %d\n",
            __FUNCTION__, server->connectionStallingTimeoutMS);

    // Set send timeout on socket if valid value supplied for connection stalling timeout
    if (server->connectionStallingTimeoutMS > -1)
    {
        // Convert connection stalling timeout MS into mpe time value
        mpe_TimeVal mtv;
        mtv.tv_sec = server->connectionStallingTimeoutMS / 1000;
        mtv.tv_usec = (server->connectionStallingTimeoutMS % 1000) * 1000;

        // Set send timeout option on socket so sends time out based on supplied inactivity timeout
        if (mpeos_socketSetOpt(server->socket, MPE_SOCKET_SOL_SOCKET, MPE_SOCKET_SO_SNDTIMEO,
                (const void *)&mtv, sizeof(mtv)) != 0)
        {
            // Get error code
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s() - Unable to set socket send timeout option, error code = %d\n", __FUNCTION__,
                    mpeos_socketGetLastError());
            mpeos_mutexRelease(server->mutex);
            return MPE_HN_ERR_OS_FAILURE;
        }
        else
        {
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() set server socket timeout to %d ms\n",
                    __FUNCTION__, server->connectionStallingTimeoutMS);
        }
    }
#ifdef INCREASE_SOCKET_SND_BUF_SIZE
    if (MPE_HN_CONTENT_LOCATION_LOCAL_VIDEO_DEVICE ==
        server->contentLocationType)
    {
        // Increase buffer limit so server doesn't drop buffers when the client
        // isn't picking up buffers fast enough
        int bufSize = (900 * 1024);      // roughly the max on Linux

        if (mpeos_socketSetOpt(server->socket, MPE_SOCKET_SOL_SOCKET,
                               MPE_SOCKET_SO_SNDBUF,
                               (const void *)&bufSize, sizeof(bufSize)) != 0)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
            "%s() - Unable to set socket send buffer size option, "
            "error code = %d\n", __FUNCTION__, mpeos_socketGetLastError());
            mpeos_mutexRelease(server->mutex);
            return MPE_HN_ERR_OS_FAILURE;
        }
        else
        {
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                    "%s set server send buffer size: %d\n", __func__, bufSize);
        }
    }
#endif
    // Save chunk encoding mode
    server->chunkedEncodingMode = streamInfo->chunkedEncodingMode;
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() -- chunked encoding mode %d\n",
            __FUNCTION__, server->chunkedEncodingMode);

    // Save the DLNA profile ID string to use
    strncpy(server->dlnaProfileId, streamInfo->dlnaProfileId, OS_HN_MAX_DLNA_PROFILE_ID_STR_SIZE);
    strncpy(server->mimeType, streamInfo->mimeType, OS_HN_MAX_MIME_TYPE_STR_SIZE);
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() -- stored DLNA profileID str %s, mime type %s\n",
            __FUNCTION__, server->dlnaProfileId, server->mimeType);

    return MPE_HN_ERR_NOERR;
}

/**
 * Returns the current session parameters
 *
 * @param server        server to retrieve info about
 * @param streamParams  info about the current stream of supplied server
 *
 * @events
 *    None.
 *
 * @return MPE_HN_ERR_NOERR         if successful
 *         MPE_HN_ERR_INVALID_PARAM if a parameter is invalid
 *         MPE_HN_ERR_OS_FAILURE    os specific failures
 */
mpe_Error hnServer_streamGetInfo(HnServer* server,
        mpe_HnStreamParams* sessionParams)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    mpeos_mutexAcquire(server->mutex);

    sessionParams->requestType = MPE_HNSTREAM_MEDIA_SERVER_HTTP;
    mpe_HnStreamParamsMediaServerHttp* streamParams = (mpe_HnStreamParamsMediaServerHttp*)sessionParams->streamParams;
    streamParams->connectionId = server->connectionId;
    streamParams->socket = server->socket;
    streamParams->chunkedEncodingMode = server->chunkedEncodingMode;
    streamParams->dlnaProfileId = server->dlnaProfileId;
    streamParams->mimeType = server->mimeType;
    streamParams->connectionStallingTimeoutMS = server->connectionStallingTimeoutMS;

    mpeos_mutexRelease(server->mutex);

    return MPE_HN_ERR_NOERR;
}

/**
 * This function creates and starts a new HN server for a given hn streaming session.
 * A new playback handle is returned if the call is successful.  The returned handle can
 * be used to control the speed, the direction and the position of the play back in the stream.
 *
 * @param server                 server associated with this specific session
 * @param sessionId
 * @param playbackParams         the stream parameter changes for this playback.
 * @param act                    the completion token for async events
 * @param playbackSession        pointer to a HN session playback handle. This handle is used
 *                               to control play rate and media positions.
 *
 * @events   MPE_HN_EVT_PLAYBACK_START signaling that the server is in a started playback state.
 *
 * @return MPE_HN_ERR_NOERR         if successful
 *         MPE_HN_ERR_INVALID_PARAM if a parameter is invalid
 *         MPE_HN_ERR_OS_FAILURE    os specific failures
 *         MPE_HN_ERR_CONTENT       if the failure has occured while acquiring the content
 */
mpe_Error hnServer_playbackStart(HnServer* server, uint32_t sessionId,
        mpe_HnPlaybackParams* playbackParams, void *act,
        mpe_HnPlaybackSession* playbackSession)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
            "%s() -- called for server %d\n", __FUNCTION__, server->serverId);

    mpe_Error ret = MPE_HN_ERR_NOERR;

    mpeos_mutexAcquire(server->mutex);

    mpe_Bool isDVR = FALSE;

    // Save playback file related parameters
    ret = hnServer_playbackStoreParams(server, playbackParams, (mpe_Bool*)&isDVR);
    {
        // If transformation is not NULL it means that the server
        // is going to stream transformed content
        if(server->transformation.id != 0)
        {
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() transformation: id %d, source %s, dest %s, width %d, height %d, bitrate %d, progressive %d\n",
                    __FUNCTION__, server->transformation.id, server->transformation.sourceProfile,
                    server->transformation.transformedProfile, server->transformation.width,
                    server->transformation.height, server->transformation.bitrate, server->transformation.progressive );
        }
    }
    if (ret != MPE_HN_ERR_NOERR)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() -- Problems with playback parameters\n", __FUNCTION__);
        mpeos_mutexRelease(server->mutex);
        return ret;
    }
    server->startSkipBytes = 0;

    if (MPE_HN_CONTENT_LOCATION_LOCAL_TUNER == server->contentLocationType)
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                 "%s() -- starting LiveStreaming pipeline\n", __FUNCTION__);

        server->pipeline->pipeline_hn_server_start(server->pipeline,
                server->tunerId,
                NULL, NULL, server->playspeedRate, server->frameRate,
                0, RI_HN_SRVR_TYPE_TUNER);
    }
    else if (MPE_HN_CONTENT_LOCATION_LOCAL_VIDEO_DEVICE !=
        server->contentLocationType)
    {
       MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                 "%s() -- requested start byte position %"PRId64"\n",
                 __FUNCTION__, server->startBytePosition);

        int64_t packet_num = server->startBytePosition / RI_PIPELINE_HN_SERVER_BLOCK_SIZE;
        int64_t packet_off = server->startBytePosition % RI_PIPELINE_HN_SERVER_BLOCK_SIZE;

        server->startBytePosition = packet_num * RI_PIPELINE_HN_SERVER_BLOCK_SIZE;
        server->startSkipBytes = packet_off;

        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() -- pipeline will start at %"PRId64" bytes;"
                 " trimming %"PRId64" bytes from the beginning of pipeline content\n",
                 __FUNCTION__, server->startBytePosition, server->startSkipBytes);

        // Initialize the pipeline, need to do this prior to sending response
        // so that content length and duration can be determined
        server->pipeline->pipeline_hn_server_start(server->pipeline,
                server->tunerId,
                (char*)&server->path[0], (char*)&server->platformFileName[0],
                server->playspeedRate, server->frameRate,
                server->startBytePosition,
                isDVR? RI_HN_SRVR_TYPE_TSB : RI_HN_SRVR_TYPE_FILE);
    }

    // Set playback session parameters to starting values
    server->activePlayback->handle = 0;
    server->activePlayback->rate = 0.0;

    // Return session id as the playback session handle
    server->activePlayback->handle = sessionId;

    // Create thread to handle sending data out on socket
    if (mpeos_threadCreate(hnServerSendThread_sendData, // method which is entry point
            (void*) server, // data to pass to method
#ifdef INCREASE_HN_SERVER_THREAD_PRIORITY
            MPE_THREAD_PRIOR_SYSTEM, // thread priority
#else
            MPE_THREAD_PRIOR_DFLT, // thread priority
#endif
            0, // stack size
            &server->streamThread, // ptr to created thread
            "HN Server Stream Thread") // thread name
            != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() = Could not create server stream thread!\n", __FUNCTION__);
        mpeos_mutexRelease(server->mutex);
        return MPE_HN_ERR_OS_FAILURE;
    }
    else
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                "%s() - server stream thread created\n", __FUNCTION__);
    }

    // Set server state to playing
    server->state = HN_STATE_PLAYING;
    server->activePlayback->rate = server->playspeedRate;

    mpeos_mutexRelease(server->mutex);

    // Send the play back start event
    mpeos_eventQueueSend(server->queueID, MPE_HN_EVT_PLAYBACK_START, NULL,
            (void*) server->act, 0);

    // Allocate structure to return as playback session data
    os_HnPlaybackSession* newPlayback = NULL;
    if (mpe_memAlloc(sizeof(os_HnPlaybackSession), (void**)&newPlayback)
            != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s - Cannot allocate new playback structure\n", __FUNCTION__);
        return MPE_HN_ERR_OS_FAILURE;
    }
    newPlayback->handle = server->activePlayback->handle;
    newPlayback->rate = server->activePlayback->rate;
    // TODO propagate CCI/E-EMI parameter from playback params cciDesc[Size|Data] -> playback->cci

    *playbackSession = (mpe_HnPlaybackSession)newPlayback;

    return MPE_HN_ERR_NOERR;
}

mpe_Error hnServer_playbackUpdateEndPosition(HnServer* server,
        uint64_t endBytePosition)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    server->endBytePosition = endBytePosition;
    if (server->endBytePosition == -1)
    {
        server->rangeByteCnt = 0;
    }
    else if (server->endBytePosition > server->startBytePosition)
    {
        server->rangeByteCnt = server->endBytePosition - server->startBytePosition + 1;
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - updated range byte cnt: %"PRIu64"\n",
                 __FUNCTION__, server->rangeByteCnt);
    }
    // TODO - How can this be?  If the end byte is -1 should there be other logic here?  Is this even a valid request?
    else if (server->endBytePosition < server->startBytePosition)
    {
        server->rangeByteCnt = server->startBytePosition - server->endBytePosition + 1;
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - updated range byte cnt: %"PRIu64"\n",
                 __FUNCTION__, server->rangeByteCnt);
    }
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
static mpe_Error hnServer_playbackStoreParams(HnServer* server,
        mpe_HnPlaybackParams* playbackParams, mpe_Bool* isDVR)
{
    mpe_Error ret = MPE_HN_ERR_NOERR;
    ri_transformation_t *transformation = NULL;

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    mpe_HnPlaybackParamsMediaServerHttp* playbackInfo = playbackParams->playbackParams;
    if (NULL == playbackInfo)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() -- NULL playback info supplied\n", __FUNCTION__);
        return MPE_HN_ERR_OS_FAILURE;
    }

    // Store playback param values
    //
    server->contentLocationType = playbackInfo->contentLocation;
    server->playspeedRate = playbackInfo->playspeedRate;
    server->useTimeOffset = playbackInfo->useTimeOffset;
    server->startBytePosition = playbackInfo->startBytePosition;
    server->endBytePosition = playbackInfo->endBytePosition;
    server->startTimePosition = playbackInfo->startTimePosition;
    server->endTimePosition = playbackInfo->endTimePosition;

    if(playbackInfo->transformation != NULL && playbackInfo->transformation->id != 0)
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                    "%s - transformation:%d \n", __FUNCTION__, playbackInfo->transformation->id);
        // If transformation is not NULL it means that the server
        // is going to stream transformed content
        // This is a different code path
        // RI uses canned transformed streams and does NOT support transformation on the fly
        // start streaming the correct transformed content
        server->transformation = *(playbackInfo->transformation);

        if (mpe_memAlloc(sizeof(ri_transformation_t),
                (void**) &transformation) != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s - Cannot allocate transformation\n", __FUNCTION__);
            return MPE_HN_ERR_OS_FAILURE;
        }

        strcpy(transformation->transformedProfileStr, server->transformation.transformedProfile);
        transformation->bitrate = server->transformation.bitrate;
        transformation->width = server->transformation.width;
        transformation->height = server->transformation.height;
        transformation->progressive = server->transformation.progressive;
    }
    else if (NULL != nextTransform)
    {
        if (mpe_memAlloc(sizeof(ri_transformation_t),
                (void**) &transformation) != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s - Cannot allocate transformation\n", __FUNCTION__);
            return MPE_HN_ERR_OS_FAILURE;
        }

        strcpy(transformation->transformedProfileStr,
               nextTransform->transformedProfileStr);
        transformation->bitrate = nextTransform->bitrate;
        transformation->width = nextTransform->width;
        transformation->height = nextTransform->height;
        transformation->progressive = nextTransform->progressive;
        server->transformation.id = 255;
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                    "%s - forced test transformation: %s, %d, %dx%d, prog:%d\n",
                    __FUNCTION__,
                    transformation->transformedProfileStr,
                    transformation->bitrate,
                    transformation->width,
                    transformation->height,
                    transformation->progressive);
    }

    // Verify the requested content location is supported here and
    // return so proper error response can be sent
    if ((MPE_HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT != server->contentLocationType)
        && (MPE_HN_CONTENT_LOCATION_LOCAL_FILE_CONTENT != server->contentLocationType)
        && (MPE_HN_CONTENT_LOCATION_LOCAL_TSB != server->contentLocationType)
        && (MPE_HN_CONTENT_LOCATION_LOCAL_TUNER != server->contentLocationType)
        && (MPE_HN_CONTENT_LOCATION_LOCAL_VIDEO_DEVICE != server->contentLocationType))
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - Unsupported content location type %d\n", __FUNCTION__,
                server->contentLocationType);
        mpe_memFree(transformation);
        return MPE_HN_ERR_INVALID_PARAM;
    }

    // Verify the requested content can be found and store path to file
    *isDVR = TRUE;
    if (MPE_HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT == server->contentLocationType)
    {
        ret = hnServer_getFileLocationRecording(
                  (mpe_HnStreamLocalSVContentDescription*)playbackInfo->contentDescription,
                  (char*)&server->path[0], (char*)&server->platformFileName[0]);

        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                "%s() - content location type is MPE_HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT.\n", __FUNCTION__);

        // If time seek is being requested translate time values to byte positions
        int64_t localBytePosition = -1;
        if((server->useTimeOffset) && (server->startTimePosition != -1) && (server->startBytePosition == -1))
        {
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - calculating byte offset for startTimePositionNS: %"PRId64"\n",
                    __FUNCTION__, server->startTimePosition);
            ret = server->pipeline->pipeline_hn_server_get_byte_for_time(
                    (char*)server->path, (char*)server->platformFileName, server->startTimePosition, &localBytePosition);
            if (ret == RI_ERROR_NONE)
            {
                int64_t bytePositionWithinBlock = localBytePosition % RI_PIPELINE_HN_SERVER_BLOCK_SIZE;
                if (bytePositionWithinBlock > 0)
                {
                    // Round down to the nearest packet boundary
                    // Need to round down so recording rewind from end of recording request isn't out of range
                    localBytePosition -= bytePositionWithinBlock;
                }
                server->startBytePosition = localBytePosition;

                MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - server->startBytePosition: %"PRId64"\n",
                        __FUNCTION__, server->startBytePosition);
            }
        }

        if((server->useTimeOffset) && (server->endTimePosition != -1) && (server->endBytePosition == -1))
        {
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - calculating byte offset for endTimePosition: %"PRId64"\n",
                    __FUNCTION__, server->endTimePosition);
            ret = server->pipeline->pipeline_hn_server_get_byte_for_time(
                    (char*)server->path, (char*)server->platformFileName, server->endTimePosition, &localBytePosition);
            if (ret == RI_ERROR_NONE)
            {
                int64_t bytePositionWithinBlock = localBytePosition % RI_PIPELINE_HN_SERVER_BLOCK_SIZE;
                if (bytePositionWithinBlock > 0)
                {
                    // Round down to the nearest packet boundary
                    // Need to round down so recording rewind from end of recording request isn't out of range
                    localBytePosition -= bytePositionWithinBlock;
                }
                server->endBytePosition = localBytePosition;

                MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - server->endBytePosition: %"PRId64"\n",
                        __FUNCTION__, server->endBytePosition);
            }
        }

        if(server->transformation.id != 0)
        {
            ret = server->pipeline->pipeline_transform_file_stream(
                                                server->pipeline,
                                                server->path,
                                                server->platformFileName,
                                                transformation);
            if (ret != MPE_HN_ERR_NOERR)
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - pipeline_transform_file_stream returned error : %d\n", __FUNCTION__,
                                                      ret);
            }
        }
    }
    else if (MPE_HN_CONTENT_LOCATION_LOCAL_FILE_CONTENT == server->contentLocationType)
    {
        ret = hnServer_getFileLocationLocal(
                  (mpe_HnStreamAppContentDescription*)playbackInfo->contentDescription,
                          (char*)&server->path[0], (char*)&server->platformFileName[0]);

        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                "%s() - content location type is MPE_HN_CONTENT_LOCATION_LOCAL_FILE_CONTENT.\n", __FUNCTION__);

        *isDVR = FALSE;
        if(server->transformation.id != 0)
        {
            ret = server->pipeline->pipeline_transform_file_stream(
                                                server->pipeline,
                                                server->path,
                                                server->platformFileName,
                                                transformation);
            if (ret != MPE_HN_ERR_NOERR)
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - pipeline_transform_file_stream returned error : %d\n", __FUNCTION__,
                                                      ret);
            }
        }
    }
    else if (MPE_HN_CONTENT_LOCATION_LOCAL_TSB == server->contentLocationType)
    {
        ret = hnServer_getFileLocationTsb(server,
                  (mpe_HnStreamTSBContentDescription*)playbackInfo->contentDescription,
                          (char*)&server->path[0], (char*)&server->platformFileName[0]);

        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                "%s() - content location type is MPE_HN_CONTENT_LOCATION_LOCAL_TSB. \n", __FUNCTION__);

        // If time seek is being requested translate time values to byte positions
        int64_t localBytePosition = -1;
        if((server->useTimeOffset) && (server->startTimePosition != -1) && (server->startBytePosition == -1))
        {
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - calculating byte offset for startTimePosition: %"PRId64"\n",
                    __FUNCTION__, server->startTimePosition);
            ret = server->pipeline->pipeline_hn_server_get_byte_for_time(
                    (char*)server->path, (char*)server->platformFileName, server->startTimePosition, &localBytePosition);
            if (ret == RI_ERROR_NONE)
            {
                int64_t bytePositionWithinBlock = localBytePosition % RI_PIPELINE_HN_SERVER_BLOCK_SIZE;
                if (bytePositionWithinBlock > 0)
                {
                    // Round down to the nearest packet boundary
                    // Need to round down so recording rewind from end of recording request isn't out of range
                    localBytePosition -= bytePositionWithinBlock;
                }
                server->startBytePosition = localBytePosition;

                MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - server->startBytePosition: %"PRId64"\n",
                        __FUNCTION__, server->startBytePosition);
            }
        }

        if((server->useTimeOffset) && (server->endTimePosition != -1) && (server->endBytePosition == -1))
        {
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - calculating byte offset for endTimePosition: %"PRId64"\n",
                    __FUNCTION__, server->endTimePosition);
            ret = server->pipeline->pipeline_hn_server_get_byte_for_time(
                    (char*)server->path, (char*)server->platformFileName, server->endTimePosition, &localBytePosition);
            if (ret == RI_ERROR_NONE)
            {
                int64_t bytePositionWithinBlock = localBytePosition % RI_PIPELINE_HN_SERVER_BLOCK_SIZE;
                if (bytePositionWithinBlock > 0)
                {
                    // Round down to the nearest packet boundary
                    // Need to round down so recording rewind from end of recording request isn't out of range
                    localBytePosition -= bytePositionWithinBlock;
                }
                server->endBytePosition = localBytePosition;

                MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - server->endBytePosition: %"PRId64"\n",
                        __FUNCTION__, server->endBytePosition);
            }
        }

        if(server->transformation.id != 0)
        {
            ret = server->pipeline->pipeline_transform_file_stream(
                                                server->pipeline,
                                                server->path,
                                                server->platformFileName,
                                                transformation);
            if (ret != MPE_HN_ERR_NOERR)
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - pipeline_transform_file_stream returned error : %d\n", __FUNCTION__,
                                                      ret);
            }
        }
    }
    else if (MPE_HN_CONTENT_LOCATION_LOCAL_VIDEO_DEVICE == server->contentLocationType)
    {
        mpe_DispDevice display = (mpe_DispDevice)
                ((mpe_HnStreamVideoDeviceContentDescription*)
                playbackInfo->contentDescription)->videoDevice;
        server->riVideoDevice = dispGetVideoDevice(display);
        *isDVR = FALSE;

        if (NULL != server->riVideoDevice)
        {
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                      "%s() - VPOP current videoDevice %p, get:%p, free:%p\n",
                      __FUNCTION__, server->riVideoDevice,
                      server->riVideoDevice->vpop_get_buffer,
                      server->riVideoDevice->vpop_free_buffer);
            server->riVideoDevice->vpop_flow_starting(server->riVideoDevice);
        }
    }
    else if (MPE_HN_CONTENT_LOCATION_LOCAL_TUNER == server->contentLocationType)
    {
        *isDVR = FALSE;
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                  "%s() - non-DVR LiveStreaming...\n", __FUNCTION__);

        // Extract pidCount and lis of PIDs from contentDescription to
        // stream from the tuner into the live streaming SPTS.
        mpe_HnStreamTunerContentDescription *contentDesc = (mpe_HnStreamTunerContentDescription *)playbackInfo->contentDescription;
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                  "%s() - tunerId: %d freq: %d ltsid: %d numPids: %d\n",
                  __FUNCTION__, contentDesc->tunerId, contentDesc->freq,
                  contentDesc->ltsid, contentDesc->pidCount);
        server->tunerId = MAX((contentDesc->tunerId - 1), 0);

        if (contentDesc->pidCount == 0)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s pid cnt is 0!%d\n",
                    __FUNCTION__, contentDesc->pidCount);
            ret = MPE_HN_ERR_INVALID_PARAM;
        }
        else
        {
            ri_pid_info_t* pPids = NULL;
            ri_tuner_t* pTuner = NULL;
            int i=0;
            uint32_t numLivePipes = 0;
            ri_pipeline_t* pPipe = NULL;
            const ri_pipeline_t** pPipelines = NULL;
            ri_pipeline_manager_t* pPipelineManager = ri_get_pipeline_manager();

            // Allocate PID array
            if (mpe_memAlloc(sizeof(ri_pid_info_t) * contentDesc->pidCount,
                    (void**) &pPids) != MPE_SUCCESS)
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                        "%s - Cannot allocate new PID array\n", __FUNCTION__);
                ret = MPE_HN_ERR_OS_FAILURE;
            }

            pPipelines = pPipelineManager->get_live_pipelines(pPipelineManager,
                                                              &numLivePipes);
            pPipe = (ri_pipeline_t*)pPipelines[server->tunerId];

            if (NULL != pPipe->get_tuner)
            {
                pTuner = pPipe->get_tuner(pPipe);
                // Pass the Pids down to platform
                // Handle PIDs
                for (i = 0; i < contentDesc->pidCount; i++)
                {
                    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                    "%s index: %d pid: 0x%x streamType:%d eltStreamType: %d\n",
                            __FUNCTION__, i, contentDesc->pids[i].pid,
                            contentDesc->pids[i].streamType,
                            contentDesc->pids[i].eltStreamType);

                    pPids[i].mediaType = contentDesc->pids[i].streamType;
                    pPids[i].srcFormat = contentDesc->pids[i].eltStreamType;
                    pPids[i].srcPid = contentDesc->pids[i].pid;
                    pPids[i].recFormat = contentDesc->pids[i].eltStreamType;
                    pPids[i].recPid = contentDesc->pids[i].pid;

                    if (NULL != pTuner)
                    {
                        pTuner->add_TS_pid(pTuner, contentDesc->pids[i].pid);
                    }
                }
            }
            else
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                          "%s server->pipeline->get_tuner = NULL!\n", __func__);
            }

            if(server->transformation.id != 0)
            {
                // Set the transformation details before flow is started
                ret = server->pipeline->pipeline_transform_live_stream(
                                                         server->pipeline,
                                                         server->tunerId,
                                                         transformation);
                if (ret != MPE_HN_ERR_NOERR)
                {
                    MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - pipeline_transform_live_stream returned error : %d\n", __FUNCTION__,
                                                          ret);
                }
            }

            if (NULL != server->pipeline->pipeline_hn_server_flow_start)
            {
                server->pipeline->pipeline_hn_server_flow_start(
                          server->pipeline, server->tunerId,
                          pPids, contentDesc->pidCount);
            }
            else
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                          "%s server->pipeline->hn_server_flow_start = NULL!\n",
                          __func__);
            }
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - Unknown contentLocationType: %d\n", __FUNCTION__,
                  server->contentLocationType);
        ret = MPE_HN_ERR_INVALID_PARAM;
    }

    if (ret != MPE_HN_ERR_NOERR)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - Unable to locate content: %s\n", __FUNCTION__,
                playbackInfo->contentDescription);
        mpe_memFree(transformation);
        return MPE_HN_ERR_INVALID_PARAM;
    }

    // Determine framerate to be used for this playback
    ret = mpeos_hnServerGetFrameRateInTrickMode(
            server->contentLocationType, playbackInfo->contentDescription,
            server->dlnaProfileId, server->mimeType, &(server->transformation), server->playspeedRate,
            (int32_t*)&server->frameRate);
    if (ret != MPE_HN_ERR_NOERR)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - Unable to get frame rate in trick mode\n", __FUNCTION__);
        mpe_memFree(transformation);
        return ret;
    }

    // Clear flag since starting with a new file
    server->hadEOS = FALSE;

    mpe_memFree(transformation);
    return MPE_HN_ERR_NOERR;
}

/**
 * This function returns the total number of bytes of content streamed across the network
 * associated with the recording matching the supplied name.
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
mpe_Error hnServer_getNetworkContentItemSize(HnServer* server,
                                             mpe_HnStreamContentLocation contentLocation,
                                             void* contentDescription,
                                             char* profileIdStr,
                                             char* mimeTypeStr,
                                             mpe_hnContentTransformation * transformation,
                                             int64_t* fileSizeBytes)
{
    mpe_Error ret = MPE_HN_ERR_NOERR;
    char path[MPE_FS_MAX_PATH];
    char filename[MPE_FS_MAX_PATH];

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    // Verify this is a valid profile ID
    if (profileIdStr != NULL && mimeTypeStr != NULL)
    {
        // If transformation id is non-zero, cannot validate profile string, mime-type
        if(transformation != NULL && transformation->id != 0)
        {
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "hnServer_getNetworkContentItemSize() - profile:%s, cannot determine size for transformed content. \n",
                                                 transformation->transformedProfile);
            *fileSizeBytes = -1;
            return MPE_HN_ERR_NOERR;
        }
        else
        {
            if (hnServer_isProfileValid(contentLocation, contentDescription,
                        profileIdStr, mimeTypeStr) == FALSE)
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                        "%s() -- invalid profile ID %s, or mime type %s, no match found\n",
                        __FUNCTION__, profileIdStr, mimeTypeStr);
                return MPE_HN_ERR_INVALID_PARAM;
            }
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() -- Null profile ID or mime type string supplied\n", __FUNCTION__);
        return MPE_HN_ERR_INVALID_PARAM;
    }

    if (MPE_HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT == contentLocation)
    {
        ret = hnServer_getFileLocationRecording(contentDescription, (char*)&path[0], (char*)&filename[0]);
    }
    else if (MPE_HN_CONTENT_LOCATION_LOCAL_FILE_CONTENT == contentLocation)
    {
        ret = hnServer_getFileLocationLocal(contentDescription, (char*)&path[0], (char*)&filename[0]);
    }
    else if (MPE_HN_CONTENT_LOCATION_LOCAL_TSB == contentLocation)
    {
        ret = hnServer_getFileLocationTsb(server, contentDescription, (char*)&path[0], (char*)&filename[0]);
    }
    else if (MPE_HN_CONTENT_LOCATION_LOCAL_VIDEO_DEVICE == contentLocation)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN,
                  "%s() - not needed for current VPOP videoDevice\n",
                  __FUNCTION__);
        ret = MPE_HN_ERR_NOERR;
    }
    else if (MPE_HN_CONTENT_LOCATION_LOCAL_TUNER == contentLocation)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN,
                  "%s() - not needed for local tuner streaming\n",
                  __FUNCTION__);
        ret = MPE_HN_ERR_NOERR;
    }
    else
    {
        ret = MPE_HN_ERR_INVALID_PARAM;
    }
    if (ret != MPE_HN_ERR_NOERR)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - Unable to locate content: %s\n",
                __FUNCTION__, contentDescription);
        return MPE_HN_ERR_INVALID_PARAM;
    }

    // Ask platform for file size
    if ((MPE_HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT == contentLocation) ||
            (MPE_HN_CONTENT_LOCATION_LOCAL_TSB == contentLocation))
    {
        mpe_Bool inProgress = FALSE;

        // If the recording is currently in progress, size cannot be determined
        // return no error and set size to -1
        // Get the recording name
        if(MPE_HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT == contentLocation)
        {
            mpe_HnStreamLocalSVContentDescription* contentDesc = (mpe_HnStreamLocalSVContentDescription* )contentDescription;
            char* recordingName = (char*)contentDesc->contentName;
#ifdef MPE_FEATURE_DVR
            inProgress = isRecordingInProgress(recordingName);
#endif
            if(inProgress)
            {
                MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "isRecordingInProgress() for recording:%s returned TRUE, content size cannot be determined. \n",
                        recordingName);
                *fileSizeBytes = -1;
                ret = MPE_HN_ERR_NOERR;
            }
        }
        else if(MPE_HN_CONTENT_LOCATION_LOCAL_TSB == contentLocation)
        {
            // content location MPE_HN_CONTENT_LOCATION_LOCAL_TSB implies (DVR) live streaming
            // similar to ongoing recording case, return success but content size is set to -1
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "Live streaming (DVR), content size cannot be determined. \n");
            inProgress = TRUE;
            *fileSizeBytes = -1;
            ret = MPE_HN_ERR_NOERR;
        }

        if(!inProgress)
        {
            ret = server->pipeline->pipeline_hn_server_get_ifs_file_size(
                    (char*)&path[0], (char*)&filename[0], fileSizeBytes);
        }
    }
    else if ((MPE_HN_CONTENT_LOCATION_LOCAL_VIDEO_DEVICE == contentLocation) ||
             (MPE_HN_CONTENT_LOCATION_LOCAL_TUNER == contentLocation))
    {
        *fileSizeBytes = -1; // Indeterminate size (same as MPE_HN_CONTENT_LOCATION_LOCAL_TUNER)
        ret = MPE_HN_ERR_NOERR;
    }
    else
    {
        ret = server->pipeline->pipeline_hn_server_get_file_size(
                (char*)&path[0], (char*)&filename[0], fileSizeBytes);
    }
    if (ret != MPE_HN_ERR_NOERR)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() -- problems get size of file name %s, path %s\n",
                __FUNCTION__, (char*)&filename[0], (char*)&path[0]);
        return MPE_HN_ERR_OS_FAILURE;
    }

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - returning %"PRId64" for DLNA profile ID %s\n",
            __FUNCTION__, *fileSizeBytes, profileIdStr);

    return ret;
}

/**
 * Refer to mpeos_hn.h for full method description.
 **/
mpe_Error hnServer_getNetworkBytePosition(HnServer* server,
        mpe_HnStreamContentLocation contentLocation, void * contentDescription,
        char * profileIDStr, char * mimeTypeStr, mpe_hnContentTransformation* transformation,
        int64_t localBytePosition, int64_t * networkBytePosition)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    MPE_UNUSED_PARAM(server);
    MPE_UNUSED_PARAM(contentLocation);
    MPE_UNUSED_PARAM(contentDescription);
    MPE_UNUSED_PARAM(mimeTypeStr);
    MPE_UNUSED_PARAM(transformation);

    *networkBytePosition = localBytePosition;

    return MPE_HN_ERR_NOERR;
}

/**
 * Returns the total number of bytes of
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
mpe_Error hnServer_getNetworkBytePositionForMediaTimeNS(HnServer* server,
                                                        mpe_HnStreamContentLocation contentLocation,
                                                        void* contentDescription,
                                                        char* profileIDStr,
                                                        char* mimeTypeStr,
                                                        mpe_hnContentTransformation* transformation,
                                                        int64_t mediaTimeNS,
                                                        int64_t* bytePosition)
{
    mpe_Error ret = MPE_HN_ERR_NOERR;
    char path[MPE_FS_MAX_PATH];
    char filename[MPE_FS_MAX_PATH];
    ri_error rc = RI_ERROR_NONE;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s() - called with time NS: %"PRId64"\n",
            __FUNCTION__, mediaTimeNS);

    // Verify this is a valid profile ID
    if (profileIDStr != NULL)
    {
        // If transformation id is non-zero, cannot validate profile string, mime-type
        if(transformation != NULL && transformation->id != 0)
        {
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "hnServer_getNetworkBytePositionForMediaTimeNS() - profile:%s, cannot determine byte position for transformed content.\n",
                                                 transformation->transformedProfile);
            *bytePosition = -1;
            return MPE_HN_ERR_NOERR;;
        }
        else
        {
            if (hnServer_isProfileValid(contentLocation, contentDescription,
                        profileIDStr, mimeTypeStr) == FALSE)
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                        "%s() -- invalid profile ID %s, or mime type %s, no match found\n",
                        __FUNCTION__, profileIDStr, mimeTypeStr);
                return MPE_HN_ERR_INVALID_PARAM;
            }
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() -- Null profile ID or mime type string supplied\n", __FUNCTION__);
        return MPE_HN_ERR_INVALID_PARAM;
    }

    if (MPE_HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT == contentLocation)
    {
        ret = hnServer_getFileLocationRecording(contentDescription, (char*)&path[0], (char*)&filename[0]);
    }
    else if (MPE_HN_CONTENT_LOCATION_LOCAL_FILE_CONTENT == contentLocation)
    {
        ret = hnServer_getFileLocationLocal(contentDescription, (char*)&path[0], (char*)&filename[0]);
    }
    else if (MPE_HN_CONTENT_LOCATION_LOCAL_TSB == contentLocation)
    {
        ret = hnServer_getFileLocationTsb(server, contentDescription, (char*)&path[0], (char*)&filename[0]);
    }
    else if (MPE_HN_CONTENT_LOCATION_LOCAL_VIDEO_DEVICE == contentLocation)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN,
                  "%s() - not needed for current VPOP videoDevice\n",
                  __FUNCTION__);
        ret = MPE_HN_ERR_NOERR;
    }
    else if (MPE_HN_CONTENT_LOCATION_LOCAL_TUNER == contentLocation)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN,
                  "%s() - not needed for local tuner streaming\n",
                  __FUNCTION__);
        ret = MPE_HN_ERR_NOERR;
    }
    else
    {
        ret = MPE_HN_ERR_INVALID_PARAM;
    }

    if (ret != MPE_HN_ERR_NOERR)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - Unable to locate content: %s\n",
                __FUNCTION__, contentDescription);
        return MPE_HN_ERR_INVALID_PARAM;
    }

    // Ask platform for byte position based on media time
    if ((MPE_HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT == contentLocation) ||
            (MPE_HN_CONTENT_LOCATION_LOCAL_TSB == contentLocation))
    {
        mpe_Bool inProgress = FALSE;

        // Check if recording is actively recording and if so return unknown byte position...
        // If the recording is currently in progress, size cannot be determined
        // return no error and set size to -1
        // Get the recording name
        if(MPE_HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT == contentLocation)
        {
            mpe_HnStreamLocalSVContentDescription* contentDesc = (mpe_HnStreamLocalSVContentDescription* )contentDescription;
            char* recordingName = (char*)contentDesc->contentName;
#ifdef MPE_FEATURE_DVR
            inProgress = isRecordingInProgress(recordingName);
#endif
            if(inProgress)
            {
                MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "isRecordingInProgress() for recording:%s returned TRUE, byte position cannot be determined. \n",
                        recordingName);
                *bytePosition = -1;
                ret = MPE_HN_ERR_NOERR;
            }
        }
        else if(MPE_HN_CONTENT_LOCATION_LOCAL_TSB == contentLocation)
        {
            // content location MPE_HN_CONTENT_LOCATION_LOCAL_TSB implies (DVR) live streaming
            // similar to in progress recording case, return success but byte position set to -1
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "Live streaming (DVR), byte position cannot be determined. \n");
            inProgress = TRUE;
            *bytePosition = -1;
            ret = MPE_HN_ERR_NOERR;
        }

        if(!inProgress)
        {
            int64_t localBytePosition = -1;

            rc = server->pipeline->pipeline_hn_server_get_byte_for_time(
                    (char*)&path[0], (char*)&filename[0], mediaTimeNS, &localBytePosition);
            // Convert ri error code to mpe return code
            if (rc != RI_ERROR_NONE)
            {
                ret = MPE_HN_ERR_OS_FAILURE;
            }
            else
            {
                int64_t bytePositionWithinBlock = localBytePosition % RI_PIPELINE_HN_SERVER_BLOCK_SIZE;
                if (bytePositionWithinBlock > 0)
                {
                    // Round down to the nearest packet boundary
                    // Need to round down so recording rewind from end of recording request isn't out of range
                    localBytePosition -= bytePositionWithinBlock;
                }
                ret = hnServer_getNetworkBytePosition(server, contentLocation,
                    contentDescription, profileIDStr, mimeTypeStr, transformation,
                    localBytePosition, bytePosition);
            }
        }
    }
    else if ((MPE_HN_CONTENT_LOCATION_LOCAL_VIDEO_DEVICE == contentLocation) ||
             (MPE_HN_CONTENT_LOCATION_LOCAL_TUNER == contentLocation))
    {
        // Cannot get byte position for media time for this type of content
        // VPOP and live streaming (non-dvr)
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN,
                "%s() -- Cannot get byte position for media time for this type of content: %d\n",
                __FUNCTION__, contentLocation);
        *bytePosition = -1;
        ret = MPE_HN_ERR_NOERR;
    }
    else
    {
        // Other types of content are not supported
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() -- unable to get byte position for media time for this type of content: %d\n",
                __FUNCTION__, contentLocation);
        return MPE_HN_ERR_OS_FAILURE;
    }
    if (ret != MPE_HN_ERR_NOERR)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() -- problems getting byte position for time: %"PRId64"\n",
                __FUNCTION__, mediaTimeNS);
        return MPE_HN_ERR_OS_FAILURE;
    }

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - for media time NS: %"PRId64", returning byte pos: %"PRId64"\n",
            __FUNCTION__, mediaTimeNS, *bytePosition);
    return ret;
}

/**
 * Refer to mpeos_hn.h for full method description.
 **/
mpe_Error hnServer_getConnectionStallingFlag(mpe_HnStreamContentLocation contentLocation,
                                             void * contentDescription,
                                             char * profileIDStr,
                                             char * mimeTypeStr,
                                             mpe_hnContentTransformation* transformation,
                                             mpe_Bool * connectionStallingSupported)
{
    mpe_Error ret = MPE_HN_ERR_NOERR;
    uint32_t playspeedCnt = 0;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    // Determine if connection stalling or pause is supported for this content.
    // If trick modes are supported, this implies that connection stalling is also supported
    ret = mpeos_hnServerGetPlayspeedsCnt(contentLocation,
        contentDescription, profileIDStr, mimeTypeStr, transformation, (uint32_t*)&playspeedCnt);
    if (ret != MPE_HN_ERR_NOERR)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() -- problems getting playspeed count\n", __FUNCTION__);
        return MPE_HN_ERR_OS_FAILURE;
    }

    // If playspeed count is zero, this implies no trick modes are supported, hence
    // pause or connection stalling is not supported
    *connectionStallingSupported = TRUE;
    if (playspeedCnt == 0)
    {
        *connectionStallingSupported = FALSE;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s() - returning %d\n", __FUNCTION__,
            *connectionStallingSupported);

    return ret;
}

/**
 * Refer to mpeos_hn.h for full method description.
 **/
mpe_Error hnServer_getServerSidePacingRestampFlag(mpe_HnStreamContentLocation contentLocation,
                                             void * contentDescription,
                                             char * profileIDStr,
                                             char * mimeTypeStr,
                                             mpe_Bool * willRestamp)
{
    mpe_Error ret = MPE_HN_ERR_NOERR;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    // Currently PC platform performs no restamping for any type of content
    *willRestamp = FALSE;
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s() - returning %d\n", __FUNCTION__,
            *willRestamp);

    return ret;
}

/**
 * Looks at the supplied request information and determines platform specific
 * path and file name. Saves as a string which will be used by pipeline as the
 * recording dir path and name.
 *
 * @param   server               server associated with this content request
 * @param   contentDescription   description of content including type and location
 *
 * @return  MPE_HN_ERR_NOERR        if successful
 *          MPE_HN_ERR_INVALID_PARAM  if a parameter is invalid
 *          MPE_HN_ERR_OS_FAILURE    os specific failures
 *          MPE_HN_ERR_CONTENT       if the failure has occured while acquiring the content
 */
static mpe_Error hnServer_getFileLocationRecording(mpe_HnStreamLocalSVContentDescription* contentDesc,
                                                    char* path, char* filename)
{
    mpe_Error ret = MPE_HN_ERR_NOERR;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN,
            "%s() -- converting content name %s to recording name\n",
            __FUNCTION__, contentDesc->contentName);

    // Get the recording name
    const char* recordingName = (const char*)contentDesc->contentName;
    ret = convertRecordingNameToPlatform(recordingName, path, filename);

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN,
            "%s() -- converted recording file name %s, path %s\n",
            __FUNCTION__, filename, path);

    return ret;
}

/**
 * Looks at the supplied request information and determines platform specific
 * path and file name. Saves as a string which will be used by pipeline as the
 * file dir path and name.
 *
 * @param   server               server associated with this content request
 * @param   contentDescription   description of content including type and location
 *
 * @return  MPE_HN_ERR_NOERR        if successful
 *          MPE_HN_ERR_INVALID_PARAM  if a parameter is invalid
 *          MPE_HN_ERR_OS_FAILURE    os specific failures
 *          MPE_HN_ERR_CONTENT       if the failure has occured while acquiring the content
 */
static mpe_Error hnServer_getFileLocationLocal(mpe_HnStreamAppContentDescription* contentDescription,
                                               char* path, char* filename)
{
    mpe_Error ret = MPE_HN_ERR_NOERR;
    const char* delimiter;
    char tmp[MPE_FS_MAX_PATH];

    // Find last '/' char which separates file name from path
    if ((delimiter = strrchr(contentDescription->pathName, '/')) == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "<<HN>> %s - missing delimiter in path = %s\n", __FUNCTION__,
                contentDescription->pathName);
        return MPE_HN_ERR_INVALID_PARAM;
    }

    // Copy the path from content description into path variable
    strncpy(path, contentDescription->pathName, delimiter
            - contentDescription->pathName);

    // Null terminate the server path string
    path[delimiter - contentDescription->pathName] = '\0';

    // Convert the path to an absolute, port-specific path if it begins with '/syscwd'
    if (strncmp(path, "/syscwd", strlen("/syscwd")) == 0)
    {
        sprintf(tmp, "%s%s", "", path + strlen("/syscwd/"));
        strcpy(path, tmp);
    }

    // Copy the file name from content description into server file name variable
    strcpy(filename, contentDescription->contentName);

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
            "%s() -- storing name %s and path %s\n", __FUNCTION__,
            filename, path);
    return ret;
}

/**
 * Looks at the supplied request information and determines platform specific
 * path and file name. Saves as a string which will be used by pipeline as the
 * file dir path and name.
 *
 * @param   server               server associated with this content request
 * @param   contentDescription   description of content including type and location
 *
 * @return  MPE_HN_ERR_NOERR        if successful
 *          MPE_HN_ERR_INVALID_PARAM  if a parameter is invalid
 *          MPE_HN_ERR_OS_FAILURE    os specific failures
 *          MPE_HN_ERR_CONTENT       if the failure has occured while acquiring the content
 */
static mpe_Error hnServer_getFileLocationTsb(HnServer* server,
                                             mpe_HnStreamTSBContentDescription* contentDescription,
                                             char* path, char* filename)
{
    mpe_Error ret = MPE_HN_ERR_NOERR;
    ri_error rc = RI_ERROR_NONE;

    ri_tsbHandle* tsb = NULL;

#ifdef MPE_FEATURE_DVR
    mpe_DvrTsb buffer = (mpe_DvrTsb)contentDescription->tsb;
    tsb = getTsbHandle(buffer);
#endif

    // Call to platform to extract tsb file name and path from tsb data structure
    rc = server->pipeline->pipeline_hn_server_get_tsb_file_name_path(
            tsb, path, filename);
    if (rc != RI_ERROR_NONE)
    {
        ret = MPE_HN_ERR_OS_FAILURE;
    }

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
            "%s() -- stored name %s and path %s\n", __FUNCTION__,
            filename, path);
    return ret;
}

/**
 * Terminates the playback associated with the supplied server.
 *
 * @param server           server to stop
 * @param playbackSession  current session to stop
 *
 * @return  MPE_HN_ERR_NOERR  if successful
 */
mpe_Error hnServer_playbackStop(HnServer* server,
        mpe_HnPlaybackSession playbackSession)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called for server %d\n",
            __FUNCTION__, server->serverId);

    mpe_Error ret = MPE_HN_ERR_NOERR;

    mpeos_mutexAcquire(server->mutex);

    // Stop the pipeline
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - stopping pipeline\n",
            __FUNCTION__);
    server->pipeline->pipeline_hn_server_stop(server->pipeline);

    if ((MPE_HN_CONTENT_LOCATION_LOCAL_VIDEO_DEVICE ==
        server->contentLocationType) && (NULL != server->riVideoDevice))
    {
        server->riVideoDevice->vpop_flow_stopping(server->riVideoDevice);
    }

    if ((MPE_HN_CONTENT_LOCATION_LOCAL_TUNER == server->contentLocationType) &&
        (NULL != server->pipeline->pipeline_hn_server_flow_stop))
    {
        server->pipeline->pipeline_hn_server_flow_stop(server->pipeline,
                                                       server->tunerId);
    }

    // Terminate the thread sending data on the socket
    if (NULL != server->streamThread)
    {
        // Terminate the existing stream thread
        if (hnServer_terminateThread(server) != MPE_HN_ERR_NOERR)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s() - problems terminating stream thread\n", __FUNCTION__);
        }
    }

    // Set active playback back to init stat
    server->activePlayback->handle = 0;
    server->activePlayback->rate = 0.0;

    mpeos_mutexRelease(server->mutex);

    // Free the supplied playback session
    if (playbackSession != NULL)
    {
        mpe_memFree(playbackSession);
    }

    return ret;
}

/**
 * Perform necessary actions to close down a session stream.
 *
 * @param   server   close down session stream associated with this server
 *
 * @return  MPE_HN_ERR_NOERR if no problems encountered,
 *          MPE_HN_ERR_OS_FAILURE if problems were encountered
 */
mpe_Error hnServer_streamClose(HnServer* server)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called for server %d\n",
            __FUNCTION__, server->serverId);

    mpe_Error ret = MPE_HN_ERR_NOERR;
    char* buffer = NULL;
    char* data = NULL;
    uint32_t bufLen = 0;

    mpeos_mutexAcquire(server->mutex);

     // If chunk encoding need to send chunk with zero length data to indicate end of stream
    if ((server->chunkedEncodingMode != MPE_HN_CHUNKED_ENCODING_MODE_NONE) && (server->hadEOS))
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - sending zero length chunk to indicate eos\n",
                __FUNCTION__);

        // Encode proper header fields into chunk with zero length data
        if (MPE_HN_ERR_NOERR != hnServerSendThread_encodeChunk(server, data, 0, server->nptNS, server->bytePos,
              (char**)&buffer, (uint32_t*)&bufLen))
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s - Problems encoding last 0 length data chunk\n", __FUNCTION__);
        }
        else
        {
            // Send the last 0 length chunk to indicate end of stream
            if (mpeos_socketSend(server->socket, buffer, bufLen, 0) == (size_t) - 1)
            {
                MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_HN,
                        "%s() - problems sending last 0 length data chunk\n", __FUNCTION__);
            }
        }
    }

    // Stop the pipeline
    server->pipeline->pipeline_hn_server_stop(server->pipeline);

    // Terminate the thread sending data on the socket
    if (NULL != server->streamThread)
    {
        // Terminate the existing stream thread
        if (hnServer_terminateThread(server) != MPE_HN_ERR_NOERR)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s() - problems terminating stream thread\n", __FUNCTION__);
        }
        server->streamThread = NULL;
    }

    // Set socket to invalid value now that thread has been terminated
    server->socket = MPE_SOCKET_INVALID_SOCKET;

    // Free memory allocated for this stream session
    if (NULL != server->connectionIdStr)
    {
        mpe_memFree(server->connectionIdStr);
        server->connectionIdStr = NULL;
    }

    // Send the stream close event
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - sending session closed event\n",
            __FUNCTION__);
    mpeos_eventQueueSend(server->queueID, MPE_HN_EVT_SESSION_CLOSED, NULL,
            (void*) server->act, 0);

    // Zero out session
    server->streamSessionId = 0;

    // Set the state to idle
    server->state = HN_STATE_IDLE;

    //server->disconnectTime = 0;

    if (server->dtcpipSessionHandle != DTCPIP_INVALID_SESSION_HANDLE)
    {
        int dtcpip_ret = g_dtcpip_ftable->dtcpip_src_close(server->dtcpipSessionHandle);
        if (dtcpip_ret != 0)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s - Problems closing DTCP/IP session %d - dtcpip_src_close returned %d\n",
                __FUNCTION__, server->dtcpipSessionHandle, dtcpip_ret);
           server->dtcpipSessionHandle = DTCPIP_INVALID_SESSION_HANDLE;
        }
    }

    mpeos_mutexRelease(server->mutex);

    return ret;
}

/**
 * Expands an OCAP recording name into components that can be used to:
 * call platform APIs to manipulate DVR media
 *
 * Returns 0 if successful, or -1 if an error occurred.
 */
static int convertRecordingNameToPlatform(const char* recordingName,
        char path[OS_DVR_MEDIA_VOL_MAX_PATH_SIZE],
        char platformRecName[RI_MAX_RECORDING_NAME_LENGTH])
{
    mpe_Error ret = MPE_HN_ERR_NOERR;

    char location[OS_FS_MAX_PATH];

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "<<HN>> %s - recordingName = %s\n",
            __FUNCTION__, recordingName);

    if ((ret = expandRecordingName(recordingName, location, platformRecName))
            != MPE_HN_ERR_NOERR)
    {
        return ret;
    }

    /* We always pass the "MEDIA" directory to the platform */
    sprintf(path, "%s/%s/%s", storageGetRoot(), location, OS_DVR_MEDIA_DIR_NAME);

#ifdef MPE_WINDOWS
    // Convert /c/ to c:/
    path[0] = path[1];
    path[1] = ':';
#endif

    MPEOS_LOG(
            MPE_LOG_INFO,
            MPE_MOD_HN,
            "<<HN>> %s - recordingName = %s, path = %s, platformRecName = %s\n",
            __FUNCTION__, recordingName, path, platformRecName);

    return ret;
}

/*
 * OCAP Recording names for this port are formatted like this:
 *     [location]/[platform_name]
 *
 * Where [location] is the MediaStorageVolume root path minus the
 * system storage root path as defined by storageGetRoot() (in os_storage.h).
 * For example, if the system storage root path is:
 *
 *     /storage/x1/x2
 *
 * and the MediaStorageVolume root path is:
 *
 *     /storage/x1/x2/device1/volumes/1234/1234/myVolume
 *
 * then [location] is:
 *
 *    device1/volumes/1234/1234/myVolume
 *
 * [platform_name] is the platform-specified recording name
 *
 * Returns 0 if successful, or -1 if an error occurred.
 */
static int expandRecordingName(const char* recordingName,
        char location[OS_FS_MAX_PATH],
        char platformRecName[RI_MAX_RECORDING_NAME_LENGTH])
{
    const char* delimiter;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "<<HN>> %s - recordingName = %s\n",
            __FUNCTION__, recordingName);

    // Find last '/' char which separates recording name from location
    if ((delimiter = strrchr(recordingName, '/')) == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "<<HN>> %s - missing delimiter in recordingName = %s\n",
                __FUNCTION__, recordingName);
        return MPE_HN_ERR_INVALID_PARAM;
    }

    strncpy(location, recordingName, delimiter - recordingName);
    location[delimiter - recordingName] = '\0';
    strcpy(platformRecName, delimiter + 1);

    MPEOS_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_HN,
            "<<HN>> %s - recordingName = %s, location = %s, platformRecName = %s\n",
            __FUNCTION__, recordingName, location, platformRecName);

    return MPE_HN_ERR_NOERR;
}

/**
 * Performs actions necessary to terminate the thread which sends data on the socket.
 *
 * @param   server   terminate the send thread associated with this server
 *
 * @return  MPE_HN_ERR_NOERR if thread was successfully terminated, returns
 *          MPE_HN_ERR_OS_FAILURE if problems were encountered
 */
static mpe_Error hnServer_terminateThread(HnServer* server)
{
    mpe_Error ret = MPE_HN_ERR_NOERR;

    // Set flag requesting thread to exit and wait until is is cleared
    int cnt = 0;
    int max_cnt = 10;

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
            "%s() - setting exit pending flag to terminate thread\n", __FUNCTION__);
    server->streamThreadExitPending = TRUE;

    // Wait for flag to be cleared by thread indication that is has exited
    while ((TRUE == server->streamThreadExitPending) && (cnt < max_cnt)
            && (NULL != server->streamThread))
    {
        cnt++;

        // Sleeping while waiting for stream thread to exit
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN,
                "%s() - waiting for thread to exit, cnt = %d\n",
                __FUNCTION__, cnt);
        mpeos_threadSleep(1, 0);
    }

    if (cnt >= max_cnt)
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                "%s() - thread did not exit, destroying thread\n", __FUNCTION__);
        if (MPE_SUCCESS != mpeos_threadDestroy(server->streamThread))
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s() - problems terminating stream thread\n", __FUNCTION__);
            ret = MPE_HN_ERR_OS_FAILURE;
        }
        else
        {
            MPEOS_LOG(
                    MPE_LOG_INFO,
                    MPE_MOD_HN,
                    "%s() - thread successfully destroyed, clearing exit pending flag\n",
                    __FUNCTION__);
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                "%s() - thread has exited, exit flag %d\n", __FUNCTION__,
                server->streamThreadExitPending);
    }
    server->streamThread = NULL;
    server->streamThreadExitPending = FALSE;
    g_test_server = NULL; 
    return ret;
}

static mpe_Bool hnServer_isProfileValid(mpe_HnStreamContentLocation contentLocation,
        void* contentDescription, char* profileId, char* mimeType)
{
    mpe_Bool ret = FALSE;
    uint32_t profileCnt = 0;

    // Remove DTCP_ prefix if included
    if (strncmp(profileId, "DTCP_", 5) == 0)
    {
        profileId += 5;
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN,
                "<<HN>> %s - profile had DTCP prefix: %s, adjusted to: %s\n",
                __FUNCTION__, profileId - 5, profileId);
    }
    else
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN,
                "<<HN>> %s - profile had no DTCP prefix: %s, no adjustment needed\n",
                __FUNCTION__, profileId);
    }

    if (mpeos_hnServerGetDLNAProfileIDsCnt(contentLocation, contentDescription,
            &profileCnt) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - mpeos_hnServerGetDLNAProfileIDsCnt failed\n", __FUNCTION__);
    }
    else
    {
        int i = 0;
        char supportedProfileId[MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE];

        for (i = 0; i < profileCnt; i++)
        {
            if (mpeos_hnServerGetDLNAProfileIDStr(contentLocation, contentDescription, i,
                    supportedProfileId) != MPE_SUCCESS)
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                        "%s() - mpeos_hnServerGetDLNAProfileIDStr failed\n", __FUNCTION__);
            }
            else if (strncmp(profileId, supportedProfileId, MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE) == 0)
            {
                uint32_t mimeTypeCnt = 0;
                if (mpeos_hnServerGetMimeTypesCnt(contentLocation, contentDescription,
                        profileId, &mimeTypeCnt) != MPE_SUCCESS)
                {
                    MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                            "%s() - mpeos_hnServerGetMimeTypesCnt failed\n", __FUNCTION__);
                }
                else
                {
                    int j = 0;
                    char supportedMimeType[MPE_HN_MAX_MIME_TYPE_STR_SIZE];

                    for (j = 0; j < mimeTypeCnt; j++)
                    {
                        if (mpeos_hnServerGetMimeTypeStr(contentLocation, contentDescription,
                                profileId, j, supportedMimeType) != MPE_SUCCESS)
                        {
                            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                                    "%s() - mpeos_hnServerGetMimeTypeStr failed\n", __FUNCTION__);
                        }
                        else if (strncmp(mimeType, supportedMimeType, MPE_HN_MAX_MIME_TYPE_STR_SIZE) == 0)
                        {
                            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s() - validated profileId %s, mimeType %s\n",
                                    __FUNCTION__, profileId, mimeType);
                            ret = TRUE;
                        }
                    }
                }
            }
        }
    }

    return ret;
}

