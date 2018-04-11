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
 * The MPE Home Network Streaming API. This API provides a consistent interface to Home Networking
 * functionality regardless of the underlying operating system.
 *
 * @author
 */

#ifndef _HN_PLAYER_H_
#define _HN_PLAYER_H_

#ifdef __cplusplus
extern "C"
{
#endif

#include "hn_player_http.h"
#include "hn_player_read_thread.h"

#include "os_hn.h"
#include <ri_pipeline.h>

//#define DEBUG_SAVE_RAW_CONTENT
//#define DEBUG_SAVE_ENC_CONTENT
//#define DEBUG_SAVE_CLR_CONTENT

// DLNA 7.2.34 DDC Maximum HTTP Header Size
#define MAX_HTTP_HDR_LEN        (4096 + 1) // terminating '\0'
#define MAX_HTTP_CHUNK_HDR_LEN    (64 + 1) // terminating '\0'
#define MAX_HTTP_BUF_LEN (MAXDATASIZE * 2)

typedef enum
{
        THREAD_STATE_INACTIVE,
        THREAD_STATE_READING,
        THREAD_STATE_DECODING,
        THREAD_STATE_DECRYPTING,
        THREAD_STATE_DISPATCHING,
        THREAD_STATE_PAUSING,
        THREAD_STATE_RESUMING,
        THREAD_STATE_COMPLETE,
        THREAD_STATE_FAILED,
        THREAD_STATE_NOT_AUTHORIZED,
        THREAD_STATE_TIMED_OUT,
        THREAD_STATE_TERMINATED,
        THREAD_STATE_DONE
}
hn_player_thread_state_e;


// Structure to maintain data for HN Stream PLAYER
typedef struct
{
    // This Player's ID
    uint32_t playerId;

    // This Player's overall HN state, i.e. idle, open or playing
    HnState state;

    // Event queue data
    mpe_EventQueue queueID;
    void* act;

    uint32_t connectionId;
    char* connectionIdStr;

    // This Player's current session ids
    uint32_t streamSessionId;

    // DTCP/IP session handle
    int dtcpipSessionHandle;

    // Stream parameters for current stream session
    //mpe_HnStreamParamsMediaPlayerHttp* streamParams;

    // Supplied URI for requested content
    char* uri;

    // URI host and port
    char* host;
    uint32_t port;

    // DTCP URI host and port
    char* dtcp_host;
    uint32_t dtcp_port;

    // DLNA profile id to user
    char dlnaProfileId[OS_HN_MAX_DLNA_PROFILE_ID_STR_SIZE];

    // HTTP HEAD request sent to & response received from media server
    char* httpHeadRequestStr;
    char* httpHeadResponseStr;

    // HTTP Get request sent to & response received from media server
    char* httpGetRequestStr;
    char* httpGetResponseStr;

    // Address of media server
    mpe_SocketIPv4SockAddr hostAddr;

    // Pointer to HN Player stream pipeline
    ri_pipeline_t* pipeline;

    // Connection to HTTP Server
    mpe_Socket socket;

#ifdef DEBUG_SAVE_RAW_CONTENT
    FILE* fpRawContent;
#endif

#ifdef DEBUG_SAVE_ENC_CONTENT
    FILE* fpEncContent;
#endif

#ifdef DEBUG_SAVE_CLR_CONTENT
    FILE* fpClrContent;
#endif

    // Thread which receives data on socket
    mpe_ThreadId readThread;

    // Read thread's state
    hn_player_thread_state_e readThreadState;

    // Flag set which indicates playback is displaying last frame when stopped
    // in order to simulate pause
    mpe_Bool holdingFrame;

    // Function to callback when more data needs to be read from socket and
    // sent down to the pipeline
    ri_hn_need_data_callback_f hnPlayer_needDataCB;

    // PIDs which were supplied via playback parameters
    ri_pid_info_t* pids;
    uint32_t pid_cnt;
    uint16_t video_pid;
    uint32_t video_pid_type;
    uint16_t audio_pid;
    uint32_t audio_pid_type;

    // Video device to use for playback
    ri_video_device_t* riVideoDevice;

    // Requested rate.
    float requestedRate;

    // Start media time
    int64_t initialMediaTimeNS;

    // Available Seek Start media time
    int64_t startTimeMS;

    // Available seek end media time
    int64_t endTimeMS;

    // HTTP decoder data
    http_decoder_t http_decoder;

    // Current playback for this player
    os_HnPlaybackSession* activePlayback;

    // Mutex for this HN player
    mpe_Mutex mutex;
    mpe_Mutex readThreadMutex;

    // Current byte position in playback stream
    uint64_t curBytePos;

    // Current media time value in milliseconds
    uint64_t curMediaTimeMS;
    uint64_t prevMediaTimeMS;
    uint64_t segmentOffsetTimeMS;

    // Flag which indicates socket read should be performed since pipeline needs data
    mpe_Bool pipelineNeedsData;

    // Flags which indicate which headers are supported based on response from HEAD request
    mpe_Bool isTimeSeekSupported;
    mpe_Bool isAvailableSeekSupported;

    // Socket read timeout in milliseconds
    int32_t socketReadTimeoutMS;

} HnPlayer;

/**
 * Describes a supported DLNA profile
 */
typedef struct hn_player_dlna_profile
{
    char profileId[OS_HN_MAX_DLNA_PROFILE_ID_STR_SIZE];
    char mimeType[OS_HN_MAX_MIME_TYPE_STR_SIZE];
    int32_t rateFramesPerSec;
    mpe_HnHttpHeaderFrameTypesInTrickMode typeFramesInTrickMode;
    uint32_t playspeedsCnt;
    char** playspeeds;

} hn_player_dlna_profile;


mpe_Error
        hnPlayer_init(HnPlayer* player, ri_pipeline_t* pipeline, uint32_t idx);

void hnPlayer_initEnv();

mpe_Error hnPlayer_streamOpen(HnPlayer* player, uint32_t sessionId,
        mpe_HnStreamParams* openParams, mpe_EventQueue queueId, void* act,
        mpe_HnStreamSession* streamingSession);

mpe_Error hnPlayer_streamGetInfo(HnPlayer* player,
        mpe_HnStreamParams* sessionParams);

mpe_Error hnPlayer_streamClose(HnPlayer* player);

mpe_Error hnPlayer_playbackStart(HnPlayer* player, uint32_t sessionId,
        mpe_HnPlaybackParams * playbackParams, void *act,
        mpe_HnPlaybackSession *playbackSession);

mpe_Error hnPlayer_playbackStop(HnPlayer* player,
        mpe_HnPlaybackSession playbackSession,
        mpe_MediaHoldFrameMode holdFrameMode);

mpe_Error hnPlayer_closeSocket(HnPlayer* player);

mpe_Error hnPlayer_playbackChangePIDs(HnPlayer* player,
        mpe_HnPlaybackSession playbackSession,
        mpe_HnHttpHeaderAVStreamParameters * pids);

mpe_Error hnPlayer_playbackPause(HnPlayer* player,
        mpe_HnPlaybackSession playbackSession);

mpe_Error hnPlayer_playbackResume(HnPlayer* player,
        mpe_HnPlaybackSession playbackSession);

//mpe_Error hnPlayer_playbackSetRate(HnPlayer* player,
//        mpe_HnPlaybackSession playbackSession, float rate, float* actualRate);

//mpe_Error hnPlayer_playbackSetTime(HnPlayer* player,
//        mpe_HnPlaybackSession playbackSession, int64_t mediaTime);

mpe_Error hnPlayer_playbackGetTime(HnPlayer* player,
        mpe_HnPlaybackSession playbackSession, int64_t* mediaTime);

mpe_Error hnPlayer_getDLNAProfileIDsCnt(uint32_t* profileIDCnt);

mpe_Error hnPlayer_getDLNAProfileIDStr(uint32_t idx, char profileIDStr[MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE]);

mpe_Error hnPlayer_getPlayspeedsCnt(char* profileIDStr, char* mimeTypeStr, uint32_t* playspeedCnt);

mpe_Error hnPlayer_getMimeTypesCnt(char * profileIDStr, uint32_t * mimeTypeCnt);

mpe_Error hnPlayer_getMimeTypeStr( char * profileIDStr, uint32_t idx,
                                      char mimeTypeStr[MPE_HN_MAX_MIME_TYPE_STR_SIZE]);

mpe_Error hnPlayer_getPlayspeedStr(char* profileIDStr, char* mimeTypeStr, uint32_t idx,
                                        char playspeedStr[MPE_HN_MAX_PLAYSPEED_STR_SIZE]);

//void hnPlayer_decodeCB(ri_pid_info_t* pids, uint32_t numPids, void* decodeData);

void hnPlayer_needDataCB(void* data);

// Local methods used to support telnet interface menu options
//
char* hnPlayer_getHttpHeadResponse(HnPlayer* player);

char* hnPlayer_getHttpGetResponse(HnPlayer* player);

#ifdef __cplusplus
}
#endif

#endif
