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
 * Local HN related method and type definitions to support mpeos_hn.c
 */

#ifndef _HN_SERVER_H_
#define _HN_SERVER_H_

#ifdef __cplusplus
extern "C"
{
#endif

#include "os_hn.h"
#include <ri_pipeline.h>
#include "mpeos_event.h"
#include "mpeos_hn.h"
#include "mpeos_socket.h"

// Define the memory to be HN allocated category
#define MPE_MEM_DEFAULT MPE_MEM_HN

//#define DEBUG_SAVE_PIPE_CONTENT
//#define DEBUG_SAVE_DTCP_CONTENT
//#define DEBUG_SAVE_RNGE_CONTENT
//#define DEBUG_SAVE_CHNK_CONTENT
//#define DEBUG_SAVE_SOCK_CONTENT

// Structure to maintain data for HN Stream Server
typedef struct
{
    // Platform/mpeos Server's tuner ID
    int tunerId;

    // Platform/mpeos Server's ID
    uint32_t serverId;

    /// This Server's state.
    HnState state;

    // Event queue data
    mpe_EventQueue queueID;
    void* act;

    // DLNA SCID associated with this session
    uint32_t connectionId;
    char* connectionIdStr;

    // Value returned to stack to be used as streaming session handle
    uint32_t streamSessionId;

    // DTCP/IP session handle
    int dtcpipSessionHandle;

    // Current playback for this server
    os_HnPlaybackSession* activePlayback;

    // Pointer to HN stream pipeline
    ri_pipeline_t* pipeline;

    // Pointer to platforn Video Device to obtain VPOP bin
    ri_video_device_t* riVideoDevice;

#ifdef DEBUG_SAVE_PIPE_CONTENT
    FILE* fpPipeContent;
#endif
#ifdef DEBUG_SAVE_DTCP_CONTENT
    FILE* fpDtcpContent;
#endif
#ifdef DEBUG_SAVE_RNGE_CONTENT
    FILE* fpRngeContent;
#endif
#ifdef DEBUG_SAVE_CHNK_CONTENT
    FILE* fpChnkContent;
#endif
#ifdef DEBUG_SAVE_SOCK_CONTENT
    FILE* fpSockContent;
#endif

    // Stream/session parameters for this server
    mpe_Socket socket;
    mpe_HnHttpHeaderChunkedEncodingMode chunkedEncodingMode;
    char dlnaProfileId[OS_HN_MAX_DLNA_PROFILE_ID_STR_SIZE];
    char mimeType[OS_HN_MAX_MIME_TYPE_STR_SIZE];
    mpe_hnContentTransformation transformation;
    int32_t connectionStallingTimeoutMS;

    // Current playback parameters for this server
    float playspeedRate;            // The play-speed rate of the requested stream
    int32_t frameRate;              // The frame rate of the requested stream
    mpe_Bool useTimeOffset;         // Flag to indicate if time offset values should be used
    int64_t startBytePosition;      // The first byte to begin streaming
    int64_t startSkipBytes;         // The number of bytes to skip streaming from this segment
    int64_t endBytePosition;        // The last byte to stream
    int64_t startTimePosition;      // The time at which to begin streaming
    int64_t endTimePosition;        // The time at which to end streaming
    int64_t rangeByteCnt;           // The number of content bytes expected to be streamed
    uint64_t totByteCnt;            // The total content bytes actually sent
    uint64_t totDataByteCnt;        // The total number of bytes sent including PCP data if necessary
    uint64_t nptNS;                 // Set by the gstreamer code when retrieving the stream buffer prior to sending over the wire
    uint64_t bytePos;               // The current byte position used during streaming
    mpe_Bool excludeExtraChunkHdrs; // Flag to determine whether or not to include the extra chunk headers required by the RI DMP

    // Path location and name of file to stream
    //mpe_HnStreamContentInfo contentInfo;
    uint32_t contentLocationType;
    char path[OS_DVR_MEDIA_VOL_MAX_PATH_SIZE];
    char platformFileName[RI_MAX_RECORDING_NAME_LENGTH];

    // Thread which receives data on socket
    mpe_ThreadId streamThread;

    // Flag set which indicates thread should exit
    mpe_Bool streamThreadExitPending;

    // Flag set when all data has been sent, used to indicate if zero length chunk should be sent
    mpe_Bool hadEOS;

    // Mutex for this server
    mpe_Mutex mutex;

} HnServer;

mpe_Error hnServer_init(HnServer* server, ri_pipeline_t* pipeline, uint32_t idx);

void hnServer_initEnv();

mpe_Error hnServer_streamOpen(HnServer* server, uint32_t sessionId,
        mpe_HnStreamParams* openParams, mpe_EventQueue queueId, void* act,
        mpe_HnStreamSession* streamingSession);

mpe_Error hnServer_streamGetInfo(HnServer* server,
        mpe_HnStreamParams* sessionParams);

mpe_Error hnServer_streamClose(HnServer* server);

mpe_Error hnServer_playbackStart(HnServer* server, uint32_t sessionId,
        mpe_HnPlaybackParams* playbackParams, void *act,
        mpe_HnPlaybackSession* playbackSession);

mpe_Error hnServer_playbackStop(HnServer* server,
        mpe_HnPlaybackSession playbackSession);

mpe_Error hnServer_playbackUpdateEndPosition(HnServer* server, uint64_t endBytePosition);

mpe_Error hnServer_getNetworkContentItemSize(HnServer* server,
        mpe_HnStreamContentLocation contentLocation,
        void* contentDescription,
        char* profileIdStr,
        char* mimeTypeStr,
        mpe_hnContentTransformation* transformation,
        int64_t* fileSizeBytes);

mpe_Error hnServer_getNetworkBytePosition(HnServer* server,
        mpe_HnStreamContentLocation contentLocation, void * contentDescription,
        char * profileIDStr, char * mimeTypeStr, mpe_hnContentTransformation* transformation,
        int64_t localBytePosition, int64_t * networkBytePosition);

mpe_Error hnServer_getNetworkBytePositionForMediaTimeNS(HnServer* server,
                                                        mpe_HnStreamContentLocation contentLocation,
                                                        void* contentDescription,
                                                        char* profileIDStr,
                                                        char* mimeTypeStr,
                                                        mpe_hnContentTransformation* transformation,
                                                        int64_t mediaTimeNS,
                                                        int64_t* bytePostion);

mpe_Error hnServer_getDLNAProfileIDsCnt(mpe_HnStreamContentLocation contentLocation,
                                        void* contentDescription,
                                        uint32_t* profileIDCnt);

mpe_Error hnServer_getDLNAProfileIDStr(mpe_HnStreamContentLocation contentLocation,
                                       void* contentDescription,
                                       uint32_t idx,
                                       char* profileIDStr);

mpe_Error hnServer_getMimeTypesCnt(mpe_HnStreamContentLocation contentLocation,
                                   void* contentDescription,
                                   char* profileIdStr,
                                   uint32_t* mimeTypeCnt);

mpe_Error hnServer_getMimeTypeStr(mpe_HnStreamContentLocation contentLocation,
                                  void* contentDescription,
                                  char* profileIdStr,
                                  uint32_t idx,
                                  char mimeTypeStr[MPE_HN_MAX_MIME_TYPE_STR_SIZE]);

mpe_Error hnServer_getPlayspeedsCnt(mpe_HnStreamContentLocation contentLocation,
                                    void* contentDescription,
                                    char* profileIDStr,
                                    char* mediaTypeStr,
                                    uint32_t* playspeedCnt);

mpe_Error hnServer_getPlayspeedStr(mpe_HnStreamContentLocation contentLocation,
                                   void* contentDescription,
                                   char* profileIDStr,
                                   char* mimeTypeStr,
                                   uint32_t idx,
                                   char* playspeedStr);

mpe_Error hnServer_getFrameTypesInTrickMode(mpe_HnStreamContentLocation contentLocation,
                                            void* contentDescription,
                                            char* profileIDStr,
                                            char* mimeTypeStr,
                                            float playspeedRate,
                                            mpe_HnHttpHeaderFrameTypesInTrickMode* frameType);

mpe_Error hnServer_getFrameRateInTrickMode(mpe_HnStreamContentLocation contentLocation,
                                            void* contentDescription,
                                            char* profileIDStr,
                                            char* mimeTypeStr,
                                            float playspeedRate,
                                            int32_t* framesPerSec);

mpe_Error hnServer_getConnectionStallingFlag(mpe_HnStreamContentLocation contentLocation,
                                             void * contentDescription,
                                             char * profileIDStr,
                                             char * mimeTypeStr,
                                             mpe_hnContentTransformation* transformation,
                                             mpe_Bool * connectionStallingSupported);

mpe_Error hnServer_getServerSidePacingRestampFlag(mpe_HnStreamContentLocation contentLocation,
                                             void * contentDescription,
                                             char * profileIDStr,
                                             char * mimeTypeStr,
                                             mpe_Bool * willRestamp);

#ifdef __cplusplus
}
#endif

#endif
