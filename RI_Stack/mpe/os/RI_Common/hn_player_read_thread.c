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
#include "hn_player_http.h"
#include "hn_dtcpip.h"

static const char* READ_THREAD_STATE_STRS[] =
       {"INACTIVE",
        "READING",
        "DECODING",
        "DECRYPTING",
        "DISPATCHING",
        "PAUSING",
        "RESUMING",
        "COMPLETE",
        "FAILED",
        "NOT_AUTHORIZED",
        "TIMED_OUT",
        "TERMINATED",
        "DONE"};

// Local Player specific methods
//
static int sendDataToPipeline(HnPlayer* player, char* buf,
        int numBytes, uint64_t nptNS);

static void setState(HnPlayer* player, hn_player_thread_state_e newState);

static const char* getStateStr(hn_player_thread_state_e state);

static size_t reading(HnPlayer* player, uint8_t* inBuf);

static mpe_Error decoding(HnPlayer* player, size_t socketRead, uint8_t* inBuf,
        uint32_t* decoderRead, uint8_t* outBuf, uint32_t* decoderWritten);

static void decrypting(HnPlayer* player,
        uint8_t* outBuf, uint32_t decoderWritten, char** cleartextData, uint32_t* cleartextSize);

static void dispatching(HnPlayer* player, char* cleartextData,
                                            uint32_t cleartextSize);

static void pausing(HnPlayer* player);

static size_t resuming(HnPlayer* player, uint8_t* inBuf);

static void stopping(HnPlayer* player);

static void updateMediaTimes(HnPlayer* player);

/**
 * Reads the data based on the content length specfied in the response header.
 *
 * @param   player   player handling HTTP request
 */
/**
 * Reads the data based on the current state of playback.
 *
 * @param   player   player handling HTTP request
 */
void hnPlayerReadThread_readData(void* playerData)
{
    HnPlayer* player = playerData;

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
            "%s() - creating new thread %p for player %p to read socket %d\n",
            __FUNCTION__, &player->readThread, player->playerId, player->socket);

    size_t socketRead = 0;
    uint8_t inBuf[MAXDATASIZE];
    uint8_t outBuf[MAX_HTTP_BUF_LEN];
    uint32_t decoderRead = 0;
    uint32_t decoderWritten = 0;
    uint8_t* cleartextData = NULL;
    uint32_t cleartextSize = 0;

    // Set read timeout option on socket so reads time out
    mpe_TimeVal mtv;
    mtv.tv_sec = player->socketReadTimeoutMS / 1000;
    mtv.tv_usec = (player->socketReadTimeoutMS % 1000) * 1000;
    if (mpeos_socketSetOpt(player->socket, MPE_SOCKET_SOL_SOCKET, MPE_SOCKET_SO_RCVTIMEO,
            (const void *)&mtv, sizeof(mtv)) != 0)
    {
        // Get error code
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - Unable to set socket send timeout option, error code = %d\n", __FUNCTION__,
                mpeos_socketGetLastError());
        return;
    }
    else
    {
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() set socket timeout to %d ms\n",
                __FUNCTION__, player->socketReadTimeoutMS);
    }

    mpe_TimeMillis startTimeMS = 0;
    mpe_TimeMillis endTimeMS = 0;

    mpeos_timeGetMillis(&startTimeMS);

    // Set the initial state of player
    setState(player, THREAD_STATE_READING);

    // Player state machine
    do
    {
        //MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
        //        "%s() - starting loop with state: %s\n",
        //        __FUNCTION__, getStateStr(player->readThreadState));

        switch (player->readThreadState)
        {
        case THREAD_STATE_READING:
            socketRead = reading(player, inBuf);
            break;

        case THREAD_STATE_DECODING:
            decoderWritten = 0;
            decoding(player, socketRead, inBuf, &decoderRead, outBuf, &decoderWritten);
            break;

        case THREAD_STATE_DECRYPTING:
            cleartextData = NULL;
            cleartextSize = 0;
            decrypting(player, outBuf, decoderWritten, (char**)&cleartextData,
                    (uint32_t*)&cleartextSize);
            break;

        case THREAD_STATE_DISPATCHING:
            dispatching(player, (char*)cleartextData, cleartextSize);
            break;

        case THREAD_STATE_PAUSING:
            pausing(player);
            break;

        case THREAD_STATE_RESUMING:
            socketRead = resuming(player, inBuf);
            break;

        case THREAD_STATE_COMPLETE:
        case THREAD_STATE_FAILED:
        case THREAD_STATE_NOT_AUTHORIZED:
        case THREAD_STATE_TIMED_OUT:
        case THREAD_STATE_TERMINATED:
             stopping(player);
             break;

        default:
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s() - internal error, player in invalid state: %d\n",
                    __FUNCTION__, player->readThreadState);
            setState(player, THREAD_STATE_FAILED);
        }
    }
    while (player->readThreadState != THREAD_STATE_DONE);

    // Gather statistics and log findings
    mpeos_timeGetMillis(&endTimeMS);
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
            "%s() - thread %p exiting, elapsed secs: %d\n",
            __FUNCTION__, &player->readThread, ((endTimeMS - startTimeMS) / 1000));

    player->readThread = NULL;

// Do some cleanup related to debug support
#ifdef DEBUG_SAVE_RAW_CONTENT
    fclose(player->fpRawContent);
    player->fpRawContent = NULL;
#endif
#ifdef DEBUG_SAVE_ENC_CONTENT
    if (player->dtcpipSessionHandle != DTCPIP_INVALID_SESSION_HANDLE)
    {
        fclose(player->fpEncContent);
        player->fpEncContent = NULL;
    }
#endif
#ifdef DEBUG_SAVE_CLR_CONTENT
    fclose(player->fpClrContent);
    player->fpClrContent = NULL;
#endif
}

/**
 * Sends the number of specified bytes in the supplied character string
 * to the hn stream pipeline.
 *
 * @param   player   stream associated with this player
 * @param   buf      data to write to file
 * @param   numBytes number of data bytes which are passed
 *
 * @return  number of bytes written
 */
static int sendDataToPipeline(HnPlayer* player, char* buf,
        int numBytes, uint64_t nptMS)
{
    // Send data to pipeline
    int bytesWritten = 0;
    uint64_t nptNS = nptMS * NANO_MSECS;
    if (numBytes > 0)
    {
        //MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - calling pipeline inject data\n", __FUNCTION__);

        if (player->pipeline->pipeline_hn_player_inject_data(player->pipeline,
                buf, numBytes, nptNS) != RI_ERROR_NONE)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s() - problems sending %d bytes to hn pipeline\n",
                    __FUNCTION__, numBytes);

            // Send failure event
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                    "%s() - sending failure event due to problems with pipeline\n",
                    __FUNCTION__);
            mpeos_eventQueueSend(player->queueID, MPE_HN_EVT_FAILURE, NULL,
                    (void*) player->act, 0);
        }
        else
        {
            bytesWritten = numBytes;
        }
        //MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - injected %d byte\n",
        //        __FUNCTION__, numBytes);
    }

    return bytesWritten;
}

/**
 * Performs actions necessary to terminate the thread which reads from the socket.
 *
 * @param   player   terminate the read thread associated with this player
 *
 * @return  MPE_HN_ERR_NOERR if thread was sucessfully terminated, returns
 *          MPE_HN_ERR_OS_FAILURE if problems were encountered
 */
void hnPlayerReadThread_terminate(void* playerData)
{
    HnPlayer* player = playerData;

    // Set flag requesting thread to exit and wait until is is cleared
    int cnt = 0;
    int max_cnt = 100;

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
            "%s() - setting player %d state to terminated\n",
            __FUNCTION__, player->playerId);

    setState(player, THREAD_STATE_TERMINATED);

    // Wait for flag to be cleared by thread indication that is has exited
    while ((player->readThreadState != THREAD_STATE_DONE) && (cnt < max_cnt)
            && (NULL != player->readThread))
    {
        cnt++;

        // Sleeping while waiting for stream thread to exit
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                "%s() - waiting for thread to exit, cnt = %d\n", __FUNCTION__, cnt);
        mpeos_threadSleep(500, 0);
    }

    if (player->readThreadState != THREAD_STATE_DONE)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - thread did not exit - %p\n", __FUNCTION__,
                &player->readThread);
    }
    else
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                "%s() - thread has exited\n", __FUNCTION__);
    }
}

void hnPlayerReadThread_pause(void* playerData)
{
    HnPlayer* player = playerData;
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
            "%s() - setting player %d state to paused\n",
            __FUNCTION__, player->playerId);

    setState(player, THREAD_STATE_PAUSING);
}

void hnPlayerReadThread_resume(void* playerData)
{
    HnPlayer* player = playerData;

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
            "%s() - setting player %d state to resumed\n",
            __FUNCTION__, player->playerId);

    setState(player, THREAD_STATE_RESUMING);
}

static void setState(HnPlayer* player, hn_player_thread_state_e newState)
{
    // Acquire mutex to prevent collision between threads
    mpeos_mutexAcquire(player->readThreadMutex);

    // Only set state if it has not been set to stop or has not finished
    if (player->readThreadState <= newState)
    {
        player->readThreadState = newState;
    }
    // Going back to earlier state is allowed if haven't not paused or completed
    else if (player->readThreadState < THREAD_STATE_PAUSING)
    {
        player->readThreadState = newState;
    }
    // Allow state to transition from resuming
    else if (player->readThreadState == THREAD_STATE_RESUMING)
    {
        player->readThreadState = newState;
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - unable to set state to %s due to current state %s\n",
                __FUNCTION__, getStateStr(newState), getStateStr(player->readThreadState));
    }
    mpeos_mutexRelease(player->readThreadMutex);

    //MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - state set to %s\n",
    //        __FUNCTION__, getStateStr(player->readThreadState));
}

static size_t reading(HnPlayer* player, uint8_t* inBuf)
{
    size_t socketRead = 0;

    //MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
    //         "%s() - about to read socket\n", __FUNCTION__);

    // Read on socket, handling timeouts properly
    do
    {
        mpe_TimeMillis startTimeMS = 0;
        mpeos_timeGetMillis(&startTimeMS);
        mpe_TimeMillis endTimeMS = 0;

        socketRead = mpeos_socketRecv(player->socket, inBuf, MAXDATASIZE, 0);
#ifdef DEBUG_SAVE_RAW_CONTENT
        if (socketRead != -1)
        {
            if (fwrite(inBuf, 1, socketRead, player->fpRawContent) != socketRead)
            {
                MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_HN,
                        "%s() - failed to save raw content for session %010u\n", __FUNCTION__, player->streamSessionId);
            }
        }
#endif
        if (socketRead == -1)
        {
            // Get the error code of socket send
            int socketErr = mpeos_socketGetLastError();
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() got - player socket err %d\n", __FUNCTION__, socketErr);
            mpeos_timeGetMillis(&endTimeMS);

            // Determine if socket send timed out
            mpe_Bool socketTimedOut = FALSE;

            if (socketErr == MPE_SOCKET_ETIMEDOUT)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() got - player socket timeout\n", __FUNCTION__);
                socketTimedOut = TRUE;
            }
            else if ((endTimeMS - startTimeMS) >= player->socketReadTimeoutMS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                        "%s() got server socket error and elapsed time indicates timeout\n",
                        __FUNCTION__);
                socketTimedOut = TRUE;
            }

            // Was this a socket read timeout or actual error?
            if (!socketTimedOut)
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - problems receiving on socket\n",
                        __FUNCTION__, socketRead);
                setState(player, THREAD_STATE_FAILED);
            }
        }
        else if (socketRead == 0)
        {
            if (player->http_decoder.data_size == UNSPECIFIED_CONTENT_LENGTH)
            {
                // If the content length was not specified, then we cant tell
                // the difference between the end of the content which ends in a socket
                // closure OR a socket closure due to some other reason.
                // The state logic, however, treats the COMPLETE and FAILED 
                // cases the same.
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - Setting state to THREAD_STATE_COMPLETE\n",
                    __FUNCTION__, socketRead);
                setState(player, THREAD_STATE_COMPLETE);
            }
            else
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s() - read 0 bytes, problems receiving on socket\n",
                        __FUNCTION__, socketRead);
                setState(player, THREAD_STATE_FAILED);
            }
        }
        else
        {
            setState(player, THREAD_STATE_DECODING);
        }
    }
    while (player->readThreadState == THREAD_STATE_READING);

    return socketRead;
}

static mpe_Error decoding(HnPlayer* player, size_t socketRead, uint8_t* inBuf,
        uint32_t* decoderRead, uint8_t* outBuf, uint32_t* decoderWritten)
{
    mpe_Error decoderStatus = MPE_SUCCESS;

    decoderStatus = hnPlayer_httpDecode(&player->http_decoder,
            socketRead, inBuf, decoderRead, MAX_HTTP_BUF_LEN, outBuf, decoderWritten);

    if (player->http_decoder.chunk_media_time_ms != INVALID_MEDIA_TIME_MS)
    {
        updateMediaTimes(player);
    }

    //MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
    //    "%s() - in loop, curMediaTimeMS: %"PRIi64" ms\n",
    //    __FUNCTION__, player->curMediaTimeMS);

    if (player->http_decoder.chunk_byte_position != INVALID_MEDIA_TIME_MS)
    {
        player->curBytePos = player->http_decoder.chunk_byte_position;
    }
    // Set state to decrypting if no errors
    if (*decoderWritten > 0)
    {
        //MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
        //        "%s() - got a buffer to decode, setting state to decrypting, size %d\n",
        //        __FUNCTION__, *decoderWritten);
        setState(player, THREAD_STATE_DECRYPTING);
    }
    else if (decoderStatus != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                "%s() - problems decoding, setting state to failed\n", __FUNCTION__);
        setState(player, THREAD_STATE_FAILED);
    }
    else
    {
        //MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
        //        "%s() - setting state to reading due to decoderWritten: %d\n",
        //        __FUNCTION__, *decoderWritten);
        setState(player, THREAD_STATE_READING);
    }

    return decoderStatus;
}

static void decrypting(HnPlayer* player, uint8_t* outBuf,
        uint32_t decoderWritten, char** cleartextData, uint32_t* cleartextSize)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s - decrypting decoder written bytes: %d\n",
            __FUNCTION__, decoderWritten);

    if (decoderWritten > 0)
    {
        if (player->dtcpipSessionHandle != DTCPIP_INVALID_SESSION_HANDLE)
        {
            int dtcpip_ret = 0;

#ifdef DEBUG_SAVE_ENC_CONTENT
            if (fwrite(outBuf, 1, decoderWritten, player->fpEncContent) != decoderWritten)
            {
                MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_HN,
                        "%s() - failed to save encrypted content for session %010u\n", __FUNCTION__, player->streamSessionId);
            }
#endif

            dtcpip_ret = g_dtcpip_ftable->dtcpip_snk_alloc_decrypt(player->dtcpipSessionHandle,
                    (char*) outBuf, decoderWritten, cleartextData, cleartextSize);
            if (dtcpip_ret != 0)
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN, "%s - dtcpip_snk_alloc_decrypt returned %d\n",
                        __FUNCTION__, dtcpip_ret);
                setState(player, THREAD_STATE_NOT_AUTHORIZED);
            }
        }
        else
        {
            *cleartextData = (char*)&outBuf[0];
            *cleartextSize = decoderWritten;
        }

#ifdef DEBUG_SAVE_CLR_CONTENT
        if (fwrite(cleartextData, 1, *cleartextSize, player->fpClrContent) != *cleartextSize)
        {
            MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_HN,
                    "%s() - failed to save content for session %010u\n", __FUNCTION__, player->streamSessionId);
        }
#endif

        // Go on to dispatching if not stopping
        setState(player, THREAD_STATE_DISPATCHING);
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s - should not be decrypting if decoder written is not greater than zero\n",
                __FUNCTION__);
        setState(player, THREAD_STATE_FAILED);
    }
}

static void dispatching(HnPlayer* player, char* cleartextData, uint32_t cleartextSize)
{
    while (player->pipelineNeedsData == FALSE && player->readThreadState < THREAD_STATE_COMPLETE)
    {
        // Sleeping while waiting for pipeline to need data
        //MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - waiting for pipeline to need data\n",
        //      __FUNCTION__);
        mpeos_threadSleep(1, 0);
    }

    if (player->pipelineNeedsData == TRUE)
    {
        //MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
        //"%s() - thread %d sending received %d bytes on socket %d to pipeline, cur byte pos %"PRIu64"\n",
        //__FUNCTION__, player->readThread, numBytes, player->socket, player->curBytePos);
        player->pipelineNeedsData = FALSE;
        player->curBytePos += sendDataToPipeline(player, (char*) cleartextData, cleartextSize,
                player->http_decoder.chunk_media_time_ms);
    }

    if (player->dtcpipSessionHandle != DTCPIP_INVALID_SESSION_HANDLE)
    {
        int dtcpip_ret = g_dtcpip_ftable->dtcpip_snk_free((char*) cleartextData);
        if (dtcpip_ret != 0)
        {
            MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_HN,
                    "%s - dtcpip_snk_free returned %d\n",__FUNCTION__, dtcpip_ret);
        }
    }

    if (player->http_decoder.state == HTTP_COMPLETE)
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                 "%s() - decoder state is complete, setting state to stopping\n", __FUNCTION__);
        setState(player, THREAD_STATE_COMPLETE);
    }
    else if (player->http_decoder.data_size == INVALID_CONTENT_LENGTH)
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                 "%s() - invalid content length, done reading data\n", __FUNCTION__);
        setState(player, THREAD_STATE_COMPLETE);
    }
    else
    {
        setState(player, THREAD_STATE_READING);
    }
}

static void pausing(HnPlayer* player)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
             "%s() - entering pause state, pausing\n", __FUNCTION__);

    while (player->readThreadState == THREAD_STATE_PAUSING)
    {
        // Sleeping while waiting to be resumed, check every half second to be resumed
        mpeos_threadSleep(500, 0);
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN, "%s() - done pausing\n",
            __FUNCTION__);
}

static size_t resuming(HnPlayer* player, uint8_t* inBuf)
{
    size_t socketRead = reading(player, inBuf);
    if (socketRead == -1)
    {
        // Got failure when trying to resume, server must have timed out
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - failure when trying to resume, assuming server timed out\n",
                __FUNCTION__);

        setState(player, THREAD_STATE_TIMED_OUT);
    }
    else
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                "%s() - playback resumed, decoding data size: %d\n", __FUNCTION__, socketRead);
        setState(player, THREAD_STATE_DECODING);
    }

    return socketRead;
}

static void stopping(HnPlayer* player)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
              "%s() - read thread is stopping with state %s\n",
              __FUNCTION__, getStateStr(player->readThreadState));

    mpe_HnEvent evt = MPE_HN_EVENT_BASE;

    switch (player->readThreadState)
    {
    case THREAD_STATE_COMPLETE:
        // Determine if at beginning of file or end of file
        if (player->activePlayback->rate > 0)
        {
            evt = MPE_HN_EVT_END_OF_CONTENT;
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                    "%s() - posting end of file event, rate = %3.2f\n",
                    __FUNCTION__, player->activePlayback->rate);
        }
        else
        {
            evt = MPE_HN_EVT_BEGINNING_OF_CONTENT;
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN,
                    "%s() - posting beginning of file event, rate = %3.2f\n",
                    __FUNCTION__, player->activePlayback->rate);

            // Make sure media time is now set to zero
            player->curMediaTimeMS = 0;
            player->prevMediaTimeMS = 0;
            player->segmentOffsetTimeMS = 0;
        }
        break;

    case THREAD_STATE_FAILED:
        evt = MPE_HN_EVT_FAILURE;
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - posting decoder failure event\n", __FUNCTION__);
        break;

    case THREAD_STATE_NOT_AUTHORIZED:
        evt = MPE_HN_EVT_SESSION_NO_LONGER_AUTHORIZED;
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - posting no longer authorized event\n", __FUNCTION__);
        break;

    case THREAD_STATE_TIMED_OUT:
        evt = MPE_HN_EVT_INACTIVITY_TIMEOUT;
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - posting inactivity timeout event\n", __FUNCTION__);
        break;

    case THREAD_STATE_TERMINATED:
        // No need to send event here, handled via hnPlayer_playbackStop
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_HN,
                "%s() - playback has been terminated, event has alredy been posted\n",
                __FUNCTION__);
        break;

    default:
        // All others are treated as internal errors or failures
        evt = MPE_HN_EVT_FAILURE;
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - internal error, unexpected state %s\n", __FUNCTION__,
                getStateStr(player->readThreadState));
    }

    // Send event to stack if one has been assigned
    if ((NULL != player->act) && (evt != MPE_HN_EVENT_BASE))
    {
        mpeos_eventQueueSend(player->queueID, evt, NULL, (void*) player->act, 0);
    }

    // Set state so read thread will terminate
    setState(player, THREAD_STATE_DONE);
}

static const char* getStateStr(hn_player_thread_state_e state)
{
    return READ_THREAD_STATE_STRS[state];
}

/**
 * Method which updates the current media time, previous media time and segment offset
 * based on the values in chunk headers received from the server.
 */
static void updateMediaTimes(HnPlayer* player)
{
    uint64_t curMediaTimeMS = 0;

    // Look for a jump in media times, which indicates a segment transisition on server side
    curMediaTimeMS = player->http_decoder.chunk_media_time_ms + player->segmentOffsetTimeMS;

    // *TODO* - OCORI-3919 get rid of this hack when PTS is supported
    if (player->requestedRate >= 0)
    {
        // Rate is greater than zero yet previous chunk time is greater than current
        if (curMediaTimeMS < player->prevMediaTimeMS)
        {
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - saw jump in forward media time, chunk time: %"PRIu64" ms, prev time: %"PRIu64" ms\n",
                    __FUNCTION__, player->http_decoder.chunk_media_time_ms, player->prevMediaTimeMS);
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - jumped back to curMediaTimeMS: %"PRIu64" ms\n",
                __FUNCTION__, curMediaTimeMS);

            if ((player->initialMediaTimeNS == -1) && (player->http_decoder.header_s0_increasing))
            {
                MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - initial media time was -1\n", __FUNCTION__);
                // First chunk received after using default media time, adjust initial media time and seg offset
                player->initialMediaTimeNS = player->prevMediaTimeMS;
                player->segmentOffsetTimeMS = player->prevMediaTimeMS - curMediaTimeMS;
                player->curMediaTimeMS = player->prevMediaTimeMS;
            }
            else
            {
                MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - initial media time: %"PRIi64" ns\n",
                        __FUNCTION__, player->initialMediaTimeNS);
                // Need to adjust the media time for a standard seg boundary crossing where
                // header times went back to zero
                player->segmentOffsetTimeMS = player->prevMediaTimeMS - player->http_decoder.chunk_media_time_ms;
                player->curMediaTimeMS = player->prevMediaTimeMS;
            }

            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - after adjustments, segment offset time: %"PRIu64" ms, curMediaTimeMS: %"PRIu64" ms\n",
                __FUNCTION__, player->segmentOffsetTimeMS, player->curMediaTimeMS);

        }
        else
        {
            // No adjustments necessary
            player->curMediaTimeMS = curMediaTimeMS;
        }
    }
    else
    {
        // Rate is less than zero yet previous chunk time is less than current
        if (curMediaTimeMS > player->prevMediaTimeMS)
        {
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - saw jump in rewind header media time, chunk time: %"PRIu64" ms, prev time: %"PRIu64" ms\n",
                    __FUNCTION__, player->http_decoder.chunk_media_time_ms, player->prevMediaTimeMS);
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - jumped from curMediaTimeMS: %"PRIu64" ms, current segment offset time: %"PRIu64" ms\n",
                __FUNCTION__, curMediaTimeMS, player->segmentOffsetTimeMS);

            if (player->prevMediaTimeMS >= player->http_decoder.chunk_media_time_ms)
            {
                player->segmentOffsetTimeMS = player->prevMediaTimeMS - player->http_decoder.chunk_media_time_ms;
            }
            else
            {
                player->segmentOffsetTimeMS = 0;
            }

            player->curMediaTimeMS = player->http_decoder.chunk_media_time_ms + player->segmentOffsetTimeMS;

            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "%s() - after adjustments, segment offset time: %"PRIu64" ms, curMediaTimeMS: %"PRIu64" ms\n",
                __FUNCTION__, player->segmentOffsetTimeMS, player->curMediaTimeMS);

        }
        else
        {
            player->curMediaTimeMS = curMediaTimeMS;
        }
    }
     player->prevMediaTimeMS = player->curMediaTimeMS;
}
