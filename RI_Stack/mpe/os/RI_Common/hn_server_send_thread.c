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

#include "mpeos_dbg.h"

#include <ri_pipeline_manager.h>
#include <ri_pipeline.h>
#include "hn_server.h"
#include "hn_dtcpip.h"
#include "hn_server_send_thread.h"

static void assignRangeByteCnt(HnServer* server);

static inline void openDebugFiles(HnServer* server);

static ri_error getBuffer(HnServer* server, char** data, uint32_t* size);

static mpe_Bool handleNoBuffer(HnServer* server, ri_error gotBufferRC,
    char** data, uint32_t* size);

static mpe_Bool trimToRange(HnServer* server, char* inData, uint32_t inSize,
    char** outData, uint32_t* outSize);

static inline void closeDebugFiles(HnServer* server);

static void handleEOS(HnServer* server);

static void formatNPT(char* hdrStr, int strLen, uint64_t mediaTime);

static size_t socketSendAll(mpe_Socket socket, void *buffer, size_t length, int flags, int connectionStalling);

static const char CRLF[] = "\r\n";

/**
 * Method which is used by streaming thread to send data on socket.
 *
 * @param   serverData  reference to server passed to thread
 */
void hnServerSendThread_sendData(void* serverData)
{
    HnServer* server = serverData;

    // Pointer and length of data that holds raw pipeline content. Needs to be
    // explicitly allocated and freed with pipeline_hn_server_get_buffer() and
    // pipeline_hn_server_free_buffer().
    char*    pipeData = NULL;
    uint32_t pipeSize = 0;

    // Pointer and length of data that holds DTCP/IP encrypted content. Needs
    // to be explicitly allocated and freed with dtcpip_src_alloc_encrypt() and
    // dtcpip_src_free() methods.
    char*    dtcpData = NULL;
    uint32_t dtcpSize = 0;

    // Pointer and length of data that holds content with some initial bytes
    // removed. This is necessary for cases when we encrypt at DTCP/IP packet
    // boundries but need to satisfy a Range/Range.dtcp.com HTTP request that
    // does not align with the data read from the pipeline or encrypted with
    // DTCP/IP library. This data is not allocated but merely a points to data
    // contained within pipeData or dtcpData. Hence, it does need to be freed.
    char*    rngeData = NULL;
    uint32_t rngeSize = 0;

    // Pointer and length of data that holds chunk-encoded content. Needs to be
    // explicitly allocated and freed with hnServerSendThread_encodeChunk() and
    // mpe_memFree().
    char*    chnkData = NULL;
    uint32_t chnkSize = 0;

    // Pointer and length of data that will be sent over the socket. It is not
    // supposed to be freed, only serves as a current reference; it is affected
    // by settings of DTCP/IP encryption and chunk mode encoding.
    char*    sockData = NULL;
    uint32_t sockSize = 0;

    mpe_TimeMillis startTimeMS = 0;
    mpe_TimeMillis endTimeMS = 0;

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - called\n", __FUNCTION__);

    // bytePos is ptr into cleartext stream -- it gets updated in call to getBuffer
    server->bytePos =  server->startBytePosition;

    server->totByteCnt = 0;      // total bytes sent, including DTCP PCP header and padding
    server->totDataByteCnt = 0;

    openDebugFiles(server);

    // Determine the range byte count and assign
    // TODO - this appears to be nearly identical logic as hn_server.c function hnServer_playbackUpdateEndPosition()
    assignRangeByteCnt(server);

    mpe_Bool connectionStallingSupported = FALSE;
    if (server->connectionStallingTimeoutMS != -1)
    {
        connectionStallingSupported = TRUE;
    }
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - connection stalling supported? %d\n",
            __FUNCTION__, connectionStallingSupported);


    //
    //
    // MAIN LOOP
    //
    //
    while (!server->streamThreadExitPending)
    {
        //*********
        // Step 1) Get a raw data buffer from pipeline.
        //*********
        ri_error gotBufferRC = getBuffer(server, &pipeData, &pipeSize);

        if (gotBufferRC != RI_ERROR_NONE)
        {
            // Could not get a buffer, try again or determine if done sending data
            server->streamThreadExitPending = handleNoBuffer(server,
                gotBufferRC, &pipeData, &pipeSize);
        }

        if (gotBufferRC == RI_ERROR_NONE)
        {
            int dtcpip_ret = 0;
#ifdef DEBUG_SAVE_PIPE_CONTENT
            if (fwrite(pipeData, pipeSize, 1, server->fpPipeContent) != 1)
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - failed to write "
                    "%u pipeData bytes\n", __FUNCTION__, pipeSize);
            }
#endif
            sockData = pipeData;
            sockSize = pipeSize;

            //*********
            // Step 2) Trim bytes
            //*********
            server->streamThreadExitPending = trimToRange(server, sockData, sockSize, &rngeData, &rngeSize);
            // Handle special case where all bytes in first buffer
            // are skipped, chunk size ends up as zero, don't send chunk
            if (rngeSize == 0)
            {
                continue;
            }

            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s() Trim #1 rngeSize = %d - sockSize = %d\n",
                    __FUNCTION__, rngeSize, sockSize);

            sockData = rngeData;
            sockSize = rngeSize;

#ifdef DEBUG_SAVE_RNGE_CONTENT
            if (fwrite(rngeData, rngeSize, 1, server->fpRngeContent) != 1)
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - failed to "
                    "write %u rngeData bytes\n", __FUNCTION__, rngeSize);
            }
#endif

            // Encrypt if protected content
            if (server->dtcpipSessionHandle != DTCPIP_INVALID_SESSION_HANDLE)
            {
                //*********
                // Step 3) Encrypt it, if required.
                //*********
                dtcpip_ret = g_dtcpip_ftable->dtcpip_src_alloc_encrypt(
                    server->dtcpipSessionHandle, server->activePlayback->cci,
                    sockData, sockSize, &dtcpData, &dtcpSize);
#ifdef DEBUG_SAVE_DTCP_CONTENT
                if (fwrite(dtcpData, dtcpSize, 1, server->fpDtcpContent) != 1)
                {
                    MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - failed to "
                        "write %u dtcpData bytes\n", __FUNCTION__, dtcpSize);
                }
#endif
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s() Encrypt dtcpSize = %d - sockSize = %d\n",
                        __FUNCTION__, dtcpSize, sockSize);

                sockData = dtcpData;
                sockSize = dtcpSize;
            }

            if (dtcpip_ret == 0)
            {
                mpe_Error chunkEncodeRC = MPE_HN_ERR_NOERR;
                if (server->chunkedEncodingMode !=
                    MPE_HN_CHUNKED_ENCODING_MODE_NONE)
                {
                    chunkEncodeRC = hnServerSendThread_encodeChunk(server,
                        sockData, sockSize, server->nptNS, server->bytePos,
                        &chnkData, &chnkSize);
#ifdef DEBUG_SAVE_CHNK_CONTENT
                    if (fwrite(chnkData, chnkSize,
                        1, server->fpChnkContent) != 1)
                    {
                        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - failed "
                            "to write %u chnkData bytes\n",
                            __FUNCTION__, chnkSize);
                    }
#endif
                    sockData = chnkData;
                    sockSize = chnkSize;
                }

                if (chunkEncodeRC == MPE_HN_ERR_NOERR)
                {
                    size_t sendAllRC = 0;
                    mpeos_timeGetMillis(&startTimeMS);

                    sendAllRC = socketSendAll(server->socket,
                        sockData, sockSize, 0, server->connectionStallingTimeoutMS);
                    if (sendAllRC != (size_t) -1)
                    {
#ifdef DEBUG_SAVE_SOCK_CONTENT
                        if (fwrite(sockData, sockSize,
                            1, server->fpSockContent) != 1)
                        {
                            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - "
                                " failed to write %u sockData bytes\n",
                                __FUNCTION__, sockSize);
                        }
#endif
                        server->totByteCnt += sockSize;
                        server->totDataByteCnt += rngeSize;
                        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s() Sent sockSize = %d\n",
                                __FUNCTION__, sockSize);
                    }
                    else
                    {
                        // Get the error code of socket send
                        int socketErr = mpeos_socketGetLastError();
                        mpeos_timeGetMillis(&endTimeMS);

                        // Determine if socket send timed out
                        mpe_Bool socketTimedOut = FALSE;
                        // Allowing for a timelag as the timer fires but the
                        // time reported by the app is slightly shorter than
                        // the timeout causing the server to continue session 
                        // and not terminate.
                        int timelag = server->connectionStallingTimeoutMS / 10;
                        if (server->connectionStallingTimeoutMS > -1)
                        {
                            if (socketErr == MPE_SOCKET_ETIMEDOUT)
                            {
                                MPE_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() got server socket timeout\n", __FUNCTION__);
                                socketTimedOut = TRUE;
                            }
                            else if ((endTimeMS - startTimeMS) >= (server->connectionStallingTimeoutMS - timelag))
                            {
                                MPE_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                                        "%s() got server socket error and elapsed time indicates timeout\n",
                                        __FUNCTION__);
                                socketTimedOut = TRUE;
                            }
                        }

                        // Was this a socket send timeout or client disconnect/other error?
                        if (socketTimedOut)
                        {
                            MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_HN, "%s() - timed out "
                                "sending data for server %d on socket %d, other "
                                "side has stopped receiving; "
                                "bytes sent: %"PRIu64", requested: %"PRIu64" \n",
                                __FUNCTION__, server->serverId, server->socket,
                                server->totDataByteCnt, server->rangeByteCnt);

                            // Send playback stop rather than failure since this
                            // isn't a server problem
                            mpeos_eventQueueSend(server->queueID,
                                    MPE_HN_EVT_INACTIVITY_TIMEOUT,
                                NULL, (void*) server->act, 0);
                            server->streamThreadExitPending = TRUE;
                        }
                        else
                        {
                            // This is a disconnect on player side, send stop event
                            MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_HN, "%s() - problems "
                                "sending data for server %d on socket %d, other "
                                "end may have disconnected; stopping playback - "
                                "bytes sent: %"PRIu64", requested: %"PRIu64" \n",
                                __FUNCTION__, server->serverId, server->socket,
                                server->totDataByteCnt, server->rangeByteCnt);

                            // Send playback stop rather than failure since this
                            // isn't a server problem
                            mpeos_eventQueueSend(server->queueID,
                                MPE_HN_EVT_PLAYBACK_STOPPED,
                                NULL, (void*) server->act, 0);
                            server->streamThreadExitPending = TRUE;
                        }
                    }
                }
                else
                {
                    MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - Problems "
                        "encoding chunk\n", __FUNCTION__);
                    mpeos_eventQueueSend(server->queueID, MPE_HN_EVT_FAILURE,
                        NULL, (void*) server->act, 0);
                    server->streamThreadExitPending = TRUE;
                }
            }
            else
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - problems "
                    "encrypting data: dtcpip_src_alloc_encrypt returned %d\n",
                    __FUNCTION__, dtcpip_ret);
                mpeos_eventQueueSend(server->queueID,
                    MPE_HN_EVT_SESSION_NO_LONGER_AUTHORIZED, NULL,
                    (void*) server->act, 0);
                server->streamThreadExitPending = TRUE;
            }
        }

        //
        // Free all allocated memory
        //
        sockData = NULL;
        sockSize = 0;
        if (chnkData != NULL)
        {
            mpe_memFree(chnkData);
            MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN, "%s - freed chnkData[%d]\n",
                        __FUNCTION__, chnkSize);
            chnkData = NULL;
            chnkSize = 0;
        }
        rngeData = NULL;
        rngeSize = 0;
        if (dtcpData != NULL)
        {
            g_dtcpip_ftable->dtcpip_src_free(dtcpData);
            MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_HN, "%s() freed dtcpData[%d]\n",
                        __FUNCTION__, dtcpSize);
            dtcpData = NULL;
            dtcpSize = 0;
        }
        if (pipeData != NULL)
        {
            if (MPE_HN_CONTENT_LOCATION_LOCAL_VIDEO_DEVICE ==
                server->contentLocationType)
            {
                server->riVideoDevice->vpop_free_buffer(
                    server->riVideoDevice, pipeData);
            }
            else
            {
                server->pipeline->pipeline_hn_server_free_buffer(
                    server->pipeline, pipeData);
            }
            pipeData = NULL;
            pipeSize = 0;
        }
    }

    // Clear exit pending flag if set
    if (server->streamThreadExitPending)
    {
        server->streamThreadExitPending = FALSE;
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
               "%s() - cleared exit pending flag\n", __FUNCTION__);
    }

    // Clear reference to stream thread since exiting
    mpeos_mutexAcquire(server->mutex);
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
             "%s() - setting thread to null, server %d sent tot bytes: %"PRIu64", sent tot data bytes: %"PRIu64", requested range: %"PRIu64"\n", __FUNCTION__,
             server->serverId, server->totByteCnt, server->totDataByteCnt, server->rangeByteCnt);
    server->streamThread = NULL;
    mpeos_mutexRelease(server->mutex);

    closeDebugFiles(server);

    server->totByteCnt = 0;
    server->totDataByteCnt = 0;
}

static inline void openDebugFiles(HnServer* server)
{
#ifdef DEBUG_SAVE_PIPE_CONTENT
    {
        char tmpBuf[64];
        sprintf(tmpBuf, "hn_srv_%02d_s%04u_p%04u_1pipe.bin", server->serverId,
            server->streamSessionId, server->activePlayback->handle);
        server->fpPipeContent = fopen(tmpBuf, "wb");
    }
#endif
#ifdef DEBUG_SAVE_DTCP_CONTENT
    {
        char tmpBuf[64];
        sprintf(tmpBuf, "hn_srv_%02d_s%04u_p%04u_2dtcp.bin", server->serverId,
            server->streamSessionId, server->activePlayback->handle);
        server->fpDtcpContent = fopen(tmpBuf, "wb");
    }
#endif
#ifdef DEBUG_SAVE_RNGE_CONTENT
    {
        char tmpBuf[64];
        sprintf(tmpBuf, "hn_srv_%02d_s%04u_p%04u_3rnge.bin", server->serverId,
            server->streamSessionId, server->activePlayback->handle);
        server->fpRngeContent = fopen(tmpBuf, "wb");
    }
#endif
#ifdef DEBUG_SAVE_CHNK_CONTENT
    {
        char tmpBuf[64];
        sprintf(tmpBuf, "hn_srv_%02d_s%04u_p%04u_4chnk.bin", server->serverId,
            server->streamSessionId, server->activePlayback->handle);
        server->fpChnkContent = fopen(tmpBuf, "wb");
    }
#endif
#ifdef DEBUG_SAVE_SOCK_CONTENT
    {
        char tmpBuf[64];
        sprintf(tmpBuf, "hn_srv_%02d_s%04u_p%04u_5sock.bin", server->serverId,
            server->streamSessionId, server->activePlayback->handle);
        server->fpSockContent = fopen(tmpBuf, "wb");
    }
#endif
}

/**
 * Determines the value for the range byte count and assigns to server parameter
 * using supplied start and end byte position values.
 *
 * @param server    server of this data
 */
static void assignRangeByteCnt(HnServer* server)
{
    uint64_t startPos = server->startBytePosition + server->startSkipBytes;
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - start: %"PRIu64", end: %"PRIu64"\n",
        __FUNCTION__, startPos, server->endBytePosition);

    if (server->endBytePosition == -1)
    {
        server->rangeByteCnt = 0;
    }
    else if (server->endBytePosition > startPos)
    {
        server->rangeByteCnt = server->endBytePosition - startPos + 1;
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - requested range byte cnt: %"PRIu64"\n",
                 __FUNCTION__, server->rangeByteCnt);
    }
    else if (server->endBytePosition < startPos)
    {
        server->rangeByteCnt = startPos - server->endBytePosition + 1;
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - requested range byte cnt: %"PRIu64"\n",
                 __FUNCTION__, server->rangeByteCnt);
    }
}

/**
 * Gets the next buffer to send on socket from platform.
 *
 * @param server    server of this data
 * @param data      pointer to buffer to put data to be sent
 * @param size      size of buffer containing data
 *
 * @return RI_ERROR_NONE if successfully retrieved buffer from platform pipeline,
 *         RI_ERROR_NO_PLAYBACK if vpop type and not currently playing, other failure codes
 *         as encountered
 */
static ri_error getBuffer(HnServer* server, char** data, uint32_t* size)
{
    ri_error rc = RI_ERROR_NONE;

    //MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - getting buffer from remote playback pipeline\n", __FUNCTION__);
    if (MPE_HN_CONTENT_LOCATION_LOCAL_VIDEO_DEVICE == server->contentLocationType)
    {
        if ((NULL != server->riVideoDevice->vpop_free_buffer) && (NULL != *data))
        {
            server->riVideoDevice->vpop_free_buffer(server->riVideoDevice, *data);
        }

        if (NULL != server->riVideoDevice->vpop_get_buffer)
        {
            rc = server->riVideoDevice->vpop_get_buffer(server->riVideoDevice, (void**) data, size);
        }
        else
        {
            rc = RI_ERROR_NO_PLAYBACK;
        }
    }
    else
    {
        uint64_t curBytePos = 0;
        uint64_t curNptNS = 0;

        if (NULL != *data)
        {
            server->pipeline->pipeline_hn_server_free_buffer(server->pipeline,
                                                             *data);
        }

        rc = server->pipeline->pipeline_hn_server_get_buffer(server->pipeline,
                (void**) data, size, (uint64_t*) &curNptNS, (uint64_t*) &curBytePos);

        if (rc == RI_ERROR_NONE)
        {
            server->nptNS = curNptNS;
            server->bytePos = curBytePos;
            //MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - got from pipeline, cur npt: %"PRIu64"\n",
            //         __FUNCTION__, server->nptNS);
            //MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - got from pipeline, cur byte pos: %"PRIu64"\n",
            //         __FUNCTION__, server->bytePos);
        }
    }

    return rc;
}

/**
 * Determines if the first or last buffers need to be trimmed in order to
 * satisfy a range request.
 *
 * @return  true if range was requested and has been sent, and therefore this
 *          is the last buffer, false otherwise
 */
static mpe_Bool trimToRange(HnServer* server, char* inData, uint32_t inSize,
    char** outData, uint32_t* outSize)
{
    mpe_Bool done = FALSE;

    *outData = inData;
    *outSize = inSize;

    //*********
    // Step 3a) Trim the beginning, if needed.
    //*********
    if (server->startSkipBytes > 0)
    {
        if (server->startSkipBytes > *outSize)
        {
            *outSize = 0;
        }
        else
        {
            *outData += server->startSkipBytes;
            *outSize -= server->startSkipBytes;
        }

        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
             "%s - adjusted insize %d to out size %d due to start skip bytes = %"PRId64"\n", __FUNCTION__,
             inSize, *outSize, server->startSkipBytes);

        server->startSkipBytes = 0;
    }

    //*********
    // Step 3a) Trim the end, if needed.
    //*********
    if (server->rangeByteCnt > 0 && *outSize > 0)
    {
        // Check if this will complete the range of requested bytes
        if (((server->playspeedRate == 1.0) && (server->totDataByteCnt + *outSize >= server->rangeByteCnt)) ||
            ((server->playspeedRate > 1.0) && (server->bytePos >= server->endBytePosition)) ||
            ((server->playspeedRate <= -1.0) && (server->bytePos <= server->endBytePosition)))
        {
            // Near requested byte count, adjust bytes to send to meet requested range
            // Only adjust data length in 1.0x playback since trick play mode is an approximation
            if (server->playspeedRate == 1.0)
            {
                MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN,
                    "%s - outSize = %d - startBytePosition = %"PRIu64" - startSkipBytes = %"PRIu64" - endBytePosition = %"PRIu64"\n",
                            __FUNCTION__, *outSize, server->startBytePosition, server->startSkipBytes, server->endBytePosition);
                MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN,
                    "%s - rangeByteCnt = %"PRIu64" - totByteCnt = %"PRIu64" - totDataByteCnt = %"PRIu64"\n",
                            __FUNCTION__, server->rangeByteCnt, server->totByteCnt, server->totDataByteCnt);

                uint32_t trimSize = server->totDataByteCnt + *outSize - server->rangeByteCnt;

                *outSize -= trimSize;
                MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                    "%s - adjusted data len %d, cur tot byte pos: %"PRIu64",  requested range: %"PRIu64"\n", __FUNCTION__,
                    *outSize, server->totDataByteCnt, server->rangeByteCnt);
            }
            else
            {
                MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                    "%s - cur bytes sent: %"PRIu64",  requested range: %"PRIu64"\n", __FUNCTION__,
                    server->totDataByteCnt, server->rangeByteCnt);
            }

            // Now done since all data has been retrieved from platform
            done = TRUE;
            handleEOS(server);
        }
    }

    return done;
}

/**
 * Performs logic based on reason for no buffer.  If is was an EOS,
 * initates special handling to see is buffer does become available.
 *
 * @param server        server of this data
 * @param gotBufferRC   return code indicating why buffer could not be retrieved
 * @param data          pointer to data in the buffer
 * @param dataLen       size of data
 *
 * @return  true if can no longer send data, false otherwise
 */
static mpe_Bool handleNoBuffer(HnServer* server, ri_error gotBufferRC,
    char** data, uint32_t* size)
{
    mpe_Bool done = FALSE;

    if (MPE_HN_CONTENT_LOCATION_LOCAL_VIDEO_DEVICE == server->contentLocationType)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
            "%s() - unable to get buffer from VPOP output - sleeping...\n",
            __FUNCTION__);
        // Are we really done or is this a service change?
        mpeos_threadSleep(500, 0);
    }
    else if (gotBufferRC == RI_ERROR_GENERAL)
    {
        // Unable to get buffer so we are done due to server failure
        done = TRUE;

        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - done, unexpected error encountered, sent %"PRIu64" total, requested range: %"PRIu64"\n",
                __FUNCTION__, server->totDataByteCnt, server->rangeByteCnt);

        mpeos_eventQueueSend(server->queueID, MPE_HN_EVT_FAILURE,
                NULL, (void*) server->act, 0);
    }
    else if (gotBufferRC == RI_ERROR_EOS)
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                 "%s() - got eos from remote playback pipeline\n", __FUNCTION__);

        // Got all buffers so we are done sending data
        done = TRUE;
        handleEOS(server);
    }
    else
    {
        //MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
        //        "%s() - waiting for pipeline to play and have data available\n", __FUNCTION__);

        // Unable to get buffer due to the pipeline is not playing yet or
        // not enough data is available for a complete buffer
        // Yield the thread so it doesn't steal all CPU cycles
        mpeos_threadYield();
    }

    return done;
}

static inline void closeDebugFiles(HnServer* server)
{
#ifdef DEBUG_SAVE_PIPE_CONTENT
    fclose(server->fpPipeContent);
    server->fpPipeContent = NULL;
#endif
#ifdef DEBUG_SAVE_DTCP_CONTENT
    fclose(server->fpDtcpContent);
    server->fpDtcpContent = NULL;
#endif
#ifdef DEBUG_SAVE_RNGE_CONTENT
    fclose(server->fpRngeContent);
    server->fpRngeContent = NULL;
#endif
#ifdef DEBUG_SAVE_CHNK_CONTENT
    fclose(server->fpChnkContent);
    server->fpChnkContent = NULL;
#endif
#ifdef DEBUG_SAVE_SOCK_CONTENT
    fclose(server->fpSockContent);
    server->fpSockContent = NULL;
#endif
}

/**
 * Puts the data from the content stream into an HTTP chunk which is sent
 * when content chunk transfer encoding has been requested by the client.
 *
 * @param server    server of this data
 * @param data      pointer to data in the buffer
 * @param dataLen   size of data
 * @param nptNS     normal playtime in Nanoseconds of this buffer
 * @param bytes     byte position of this buffer in overall stream
 * @param buffer    pointer to buffer to put data to be sent
 * @param bufLen    size of buffer containing data
 *
 * @return MPE_HN_ERR_NOERR if no problems encountered,
 *         MPE_HN_ERR_OS_FAILURE if memory could not be allocated
 */
mpe_Error hnServerSendThread_encodeChunk(HnServer* server,
    char* data, uint32_t dataLen, uint64_t nptNS, uint64_t bytes,
    char** buffer, uint32_t* bufLen)
{
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN,"%s(%p, %p, %d, %llu, %llu, %p, %p)\n",
            __FUNCTION__, server, data, dataLen, nptNS, bytes, buffer, bufLen);

    char octetStr[16];
    char nptStr[32];
    char bytesStr[32];

    int bufOffset = 0;

    // Create the octet string for chunk size
    snprintf(octetStr, sizeof(octetStr), "%x", dataLen);

    // Add the size of the octet string to size of buffer needed
    *bufLen += strlen(octetStr);

    if (!server->excludeExtraChunkHdrs)
    {
        // Create the string to store the current normal play time
        // npt=h:m:s.uuu\r\n
        formatNPT((char*) &nptStr[0], sizeof(nptStr), (nptNS / NANO_MSECS));
        *bufLen += strlen(nptStr);

        // Create the string to store the current byte position
        // *TODO* - handle this better, need to limit amount of data sent?
        // Or fail if content exceeds this size???
        // bytes=d\r\n up to 2^48 - 1
        if (bytes > 281474976710655LL)
        {
            bytes = 281474976710655LL;
        }
        sprintf(bytesStr, ";bytes=\"%llu\"", bytes);
        *bufLen += strlen(bytesStr);
    }

    // Add two to size of buffer for CRLF to terminate chunk header
    *bufLen += 2;

    // Add the number of data bytes to the size of the buffer needed
    *bufLen += dataLen;

    // Add two to size of buffer for final CRLF to terminate chunk size
    *bufLen += 2;

    // Allocate memory to store octet str, extra headers if included, data and final CRLF
    if (mpe_memAlloc(*bufLen, (void**) buffer) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s - Cannot allocate buffer\n",
                __FUNCTION__);
        return MPE_HN_ERR_OS_FAILURE;
    }
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN, "%s - allocated chnkData[%d]\n",
                __FUNCTION__, *bufLen);

    memcpy(*buffer, octetStr, strlen(octetStr));
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN,
        "%s() - encoded chunk length %s, len %d\n",
         __FUNCTION__, octetStr, strlen(octetStr));
    bufOffset += strlen(octetStr);

    if (!server->excludeExtraChunkHdrs)
    {
        // Add the npt string
        memcpy(*buffer + bufOffset, nptStr, strlen(nptStr));
        MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN, "%s() - encoded npt %s, len %d\n",
             __FUNCTION__, nptStr, strlen(nptStr));
        bufOffset += strlen(nptStr);

        // Add the bytes string which includes CRLF to terminate chunk header
        memcpy(*buffer + bufOffset, bytesStr, strlen(bytesStr));
        MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN,
            "%s() - encoded byte pos %s, len %d\n",
             __FUNCTION__, bytesStr, strlen(bytesStr));
        bufOffset += strlen(bytesStr);
    }

    // Add the CRLF to terminate chunk header
    memcpy(((unsigned char *) *buffer) + bufOffset, CRLF, 2);
    bufOffset += 2;

    //char tmpStr[36];
    //tmpStr[0] = '\0';
    //strncat(tmpStr, *buffer, bufOffset);
    //MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - encoded %d bytes into chunk hdr: %s\n",
    //      __FUNCTION__, bufOffset, tmpStr);

    // If buffer is not NULL, copy data (may be NULL if this is the last 0 size buffer)
    if (NULL != data)
    {
        memcpy(((unsigned char *) *buffer) + bufOffset, data, dataLen);
    }
    bufOffset += dataLen;

    // Add the final CRLF
    memcpy(((unsigned char *) *buffer) + bufOffset, CRLF, 2);

    //MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - encoded %d bytes, crlf %s, %d data, into chunk, buf %s\n",
    //      __FUNCTION__, *bufLen, CRLF, dataLen, *buffer);

    return MPE_HN_ERR_NOERR;
}

/**
 * Format the HTTP request header Time Seek Range using the supplied media time
 * as value.
 *
 * @param   hdrStr         return formatted header string
 * @param   mediaTimeMS    encode this as the value of header
 */
static void formatNPT(char* hdrStr, int strLen, uint64_t mediaTimeMS)
{
    char tmpStr[32];
    int nptStrLen = 0;

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_HN, "%s(%p, %d, %llu)\n",
              __FUNCTION__, hdrStr, strLen, mediaTimeMS);
    // Encode the token text with the starting quote
    strcpy(hdrStr, ";npt=\"");

    // Calculate number of hours, minutes, seconds, and milliseconds
    uint64_t hours = mediaTimeMS / (60 * 60 * 1000);
    uint64_t remainder = mediaTimeMS % (60 * 60 * 1000);
    int minutes = remainder / (60 * 1000);
    float seconds = (remainder % (60 * 1000)) / 1000.0;

    // Encode hours into string
    snprintf(tmpStr, sizeof(tmpStr), "%llu:", hours);
    nptStrLen = strlen(hdrStr);
    strncat(hdrStr, tmpStr, strLen - nptStrLen);

    // Encode minutes into string
    snprintf(tmpStr, sizeof(tmpStr), "%d:", minutes);
    nptStrLen = strlen(hdrStr);
    strncat(hdrStr, tmpStr, strLen - nptStrLen);

    // Encode seconds into string along with the ending quote
    snprintf(tmpStr, sizeof(tmpStr), "%2.3f\"", seconds);
    nptStrLen = strlen(hdrStr);
    strncat(hdrStr, tmpStr, strLen - nptStrLen);
}

/**
 * Method which sends the supplied buffer on the socket and does not return
 * until all supplied bytes are sent or error occurs.
 *
 * @param   socket  socket to send data on
 * @param   buffer  data to send on the socket
 * @param   length  number of bytes to send on socket
 * @param   flags   options to set on the socket send call
 * @param   connectionStallingTimeoutMS  connectionStalling timeout in MS 
 */
static size_t socketSendAll(mpe_Socket socket, void *buffer, size_t length, int flags, int connectionStallingTimeoutMS)
{
    size_t bytesToSend = length;
    void* bufferTemp = buffer;
    mpe_TimeMillis startTimeMS = 0;
    mpe_TimeMillis endTimeMS = 0;

    while (bytesToSend > 0)
    {
        mpeos_timeGetMillis(&startTimeMS);
        size_t returnCode = mpeos_socketSend(socket, bufferTemp, bytesToSend, flags);
        mpeos_timeGetMillis(&endTimeMS);

        // Allow a window for timers being inaccurate 
        // Allowing for a timelag as the timer fires but the
        // time reported by the app is slightly shorter than
        // the timeout causing the server to continue session 
        // and not terminate.
        int timelag = connectionStallingTimeoutMS / 10;
        if ((endTimeMS-startTimeMS) >= (connectionStallingTimeoutMS - timelag))
        {
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "Send timeout\n");
            return -1;
        }  
        if (returnCode == (size_t)(-1))
        {
            MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_HN, "<<HN>> %s - send error: %d\n",
                    __FUNCTION__, mpeos_socketGetLastError());
            return returnCode;
        }
        else if (returnCode != length)
        {
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "<<HN>> %s - sent partial packet\n",
                    __FUNCTION__);
        }

        bytesToSend -= returnCode;
        bufferTemp += returnCode;
    }

    return length;
}

/**
 * Perform the necessary actions when EOS is encountered in underlying pipeline.
 * Also send necessary event up to stack.
 *
 * @param   server  server of this data
 */
static void handleEOS(HnServer* server)
{
    server->hadEOS = TRUE;

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
            "%s() - done, got eos from remote playback pipeline, resetting pipeline\n",
            __FUNCTION__);

    server->pipeline->pipeline_hn_server_reset(server->pipeline);

    // Send event of file event
    if (server->playspeedRate > 0)
    {
        mpeos_eventQueueSend(server->queueID, MPE_HN_EVT_END_OF_CONTENT,
                NULL, (void*) server->act, 0);
    }
    else
    {
        mpeos_eventQueueSend(server->queueID, MPE_HN_EVT_BEGINNING_OF_CONTENT,
                NULL, (void*) server->act, 0);
    }

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
             "%s() - at EOS, server %d sent tot data bytes: %"PRIu64", requested range: %"PRIu64"\n", __FUNCTION__,
             server->serverId, server->totDataByteCnt, server->rangeByteCnt);
}
