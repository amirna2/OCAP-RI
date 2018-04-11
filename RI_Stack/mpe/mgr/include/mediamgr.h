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
#ifndef _MPE_MEDIAMGR_H_
#define _MPE_MEDIAMGR_H_

#include "mpe_types.h"
#include "mpeos_media.h"

#ifdef __cplusplus
extern "C"
{
#endif

void mpe_media_setup(void);
mpe_Error mpe_mediaInit(void);
mpe_Error mpe_mediaShutdown(void);
mpe_Error mpe_mediaTune(mpe_MediaTuneRequestParams *tuneRequest,
        mpe_EventQueue queueId, void *act);

/******************************************
 * Media Manager function prototypes
 *****************************************/
typedef struct
{
    mpe_Error (*mpe_mediaInit_ptr)(void);
    mpe_Error (*mpe_mediaShutdown_ptr)(void);
    mpe_Error (*mpe_mediaTune_ptr)(mpe_MediaTuneRequestParams *tuneRequest,
            mpe_EventQueue queueId, void *act);
    mpe_Error (*mpe_mediaDecode_ptr)(
            mpe_MediaDecodeRequestParams *decodeRequest,
            mpe_EventQueue queueId, void *act, mpe_MediaDecodeSession *session);
    mpe_Error (*mpe_mediaStop_ptr)(mpe_MediaDecodeSession session, uint32_t holdFrameMode);
    mpe_Error (*mpe_mediaFreeze_ptr)(mpe_DispDevice videoDevice);
    mpe_Error (*mpe_mediaResume_ptr)(mpe_DispDevice videoDevice);
    mpe_Error (*mpe_mediaSetBounds_ptr)(mpe_DispDevice videoDevice,
            mpe_MediaRectangle *srcRect, mpe_MediaRectangle *destRect);
    mpe_Error (*mpe_mediaGetBounds_ptr)(mpe_DispDevice videoDevice,
            mpe_MediaRectangle *srcRect, mpe_MediaRectangle *destRect);
    mpe_Error (*mpe_mediaCheckBounds_ptr)(mpe_DispDevice videoDevice,
            mpe_MediaRectangle *desiredSrc, mpe_MediaRectangle *desiredDst,
            mpe_MediaRectangle *actualSrc, mpe_MediaRectangle *actualDst);
    mpe_Error (*mpe_mediaSwapDecoders_ptr)(mpe_DispDevice decoder1,
            mpe_DispDevice decoder2, mpe_Bool useAudio);
    mpe_Error (*mpe_mediaGetScaling_ptr)(mpe_DispDevice decoder,
            mpe_MediaPositioningCapabilities* positioning, float** horiz,
            float** vert, mpe_Bool* hRange, mpe_Bool* vRange,
            mpe_Bool* canClip, mpe_Bool* supportsComponent);
    mpe_Error (*mpe_mediaGetAspectRatio_ptr)(mpe_DispDevice decoder,
            mpe_MediaAspectRatio *ratio);
    mpe_Error (*mpe_mediaGetAFD_ptr)(mpe_DispDevice decoder,
            mpe_MediaActiveFormatDescription *afd);
    mpe_Error (*mpe_mediaGetInputVideoSize_ptr)(mpe_DispDevice dev,
            mpe_GfxDimensions *dim);
    mpe_Error (*mpe_mediaDripFeedStart_ptr)(
            mpe_MediaDripFeedRequestParams *dripFeedRequest,
            mpe_EventQueue queueId, void *act, mpe_MediaDecodeSession *session);
    mpe_Error (*mpe_mediaDripFeedRenderFrame_ptr)(
            mpe_MediaDecodeSession session, uint8_t *buffer, size_t length);
    mpe_Error (*mpe_mediaDripFeedStop_ptr)(mpe_MediaDecodeSession session);
    mpe_Error (*mpe_mediaGetSTC_ptr)(uint32_t tuner, uint32_t pcrPid,
            uint64_t *stc);
    mpe_Error (*mpe_mediaBlockPresentation_ptr)(mpe_MediaDecodeSession session,
            mpe_Bool block);
    mpe_Error (*mpe_mediaChangePids_ptr)(mpe_MediaDecodeSession session,
            uint32_t numPids, mpe_MediaPID *pids, uint32_t pcrPid);
    mpe_Error (*mpe_mediaSetMute_ptr)(mpe_MediaDecodeSession session, mpe_Bool mute);
    mpe_Error (*mpe_mediaSetGain_ptr)(mpe_MediaDecodeSession session, float gain, float *actualGain);
    mpe_Error (*mpe_mediaGet3DConfig_ptr)(mpe_MediaDecodeSession session, mpe_DispStereoscopicMode* stereoscopicMode,
            mpe_Media3DPayloadType* payloadType, uint8_t* payload, uint32_t* payloadSz);
    mpe_Error (*mpe_mediaSetCCI_ptr)(mpe_MediaDecodeSession session, uint8_t cci);
    mpe_Error (*mpe_mediaGetInputVideoScanMode_ptr)(mpe_MediaDecodeSession session, mpe_MediaScanMode* scanMode);
} mpe_media_ftable_t;

#ifdef __cplusplus
}
;
#endif

#endif /* _MPE_MEDIAMGR_H_ */
