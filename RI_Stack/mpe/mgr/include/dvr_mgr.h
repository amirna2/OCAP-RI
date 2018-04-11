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

#ifndef _DVR_MGR_H_
#define _DVR_MGR_H_

#ifdef __cplusplus
extern "C"
{
#endif

#include <mpeos_dvr.h>

/***************************************************************
 * DVR Manager function table definition
 ***************************************************************/

void mpe_dvrSetup(void);
void mpe_dvrInit(void);

typedef struct
{
    void (*mpe_dvr_init_ptr)(void);

    mpe_Error (*mpeos_dvrGet_ptr)(mpe_DvrInfoParam param, void *input,
            void *output);
    mpe_Error (*mpeos_dvrGetRecordingList_ptr)(mpe_StorageHandle device,
            uint32_t *count, mpe_DvrString_t **recordingNames);
    mpe_Error (*mpeos_dvrFreeRecordingList_ptr)(void);
    mpe_Error (*mpeos_dvrGetPlayScales_ptr)(mpe_StorageHandle device,
            float **playScales, uint32_t *num);
    uint32_t (*mpeos_dvrGetLowPowerResumeTime_ptr)(void);
    mpe_Error (*mpeos_dvrResumeFromLowPower_ptr)(void);
    mpe_Error (*mpeos_dvrRecordingDelete_ptr)(char *recordingName);
    mpe_Error (*mpeos_dvrRecordingGet_ptr)(mpe_StorageHandle device,
            char *recordingName, mpe_DvrInfoParam param, void *output);
    mpe_Error (*mpeos_dvrRecordingPlayStart_ptr)(char *recordingName,
            mpe_DispDevice videoDevice, mpe_DvrPidInfo *pids,
            uint32_t pidCount, int64_t mediaTime, float requestedRate, float *actualRate, mpe_Bool blocked,
            mpe_Bool muted, float requestedGain, float *actualGain,
            uint8_t cci, int64_t alarmMediaTime,
            mpe_EventQueue queueId, void *act,
            mpe_DvrPlayback *playback);
    mpe_Error (*mpeos_dvrTsbPlayStart_ptr)(mpe_DvrTsb buffer,
            mpe_DispDevice videoDevice, mpe_DvrPidInfo *pids,
            uint32_t pidCount, int64_t mediaTime, float requestedRate, float *actualRate, mpe_Bool blocked,
            mpe_Bool muted, float requestedGain, float *actualGain,
            uint8_t cci, int64_t alarmMediaTime,
            mpe_EventQueue queueId, void *act,
            mpe_DvrPlayback *playback);
    mpe_Error (*mpeos_dvrPlayBackStop_ptr)(mpe_DvrPlayback playback, uint32_t holdFrameMode);
    mpe_Error (*mpeos_dvrPlaybackGetTime_ptr)(mpe_DvrPlayback playback,
            int64_t *mediaTime);
    mpe_Error (*mpeos_dvrPlaybackSetTime_ptr)(mpe_DvrPlayback playback,
            int64_t mediaTime);
    mpe_Error (*mpeos_dvrPlaybackChangePids_ptr)(mpe_DvrPlayback playback,
            mpe_DvrPidInfo *pids, uint32_t pidCount);
    mpe_Error (*mpeos_dvrPlaybackBlockPresentation_ptr)(
            mpe_DvrPlayback playback, mpe_Bool block);
    mpe_Error (*mpeos_dvrSetTrickMode_ptr)(mpe_DvrPlayback playback,
            float mode, float *actualMode);
    mpe_Error (*mpeos_dvrGetTrickMode_ptr)(mpe_DvrPlayback playback,
            float *mode);
    mpe_Error (*mpeos_dvrTsbNew_ptr)(mpe_StorageHandle device,
            int64_t duration, mpe_DvrTsb *buffer);
    mpe_Error (*mpeos_dvrTsbBufferingStart_ptr)(uint32_t tunerID, uint8_t ltsid,
            mpe_DvrTsb buffer, mpe_DvrBitRate bitRate, int64_t desiredDuration,
            int64_t maxDuration, mpe_EventQueue queueId,
            void *act, mpe_DvrPidInfo *pids, uint32_t pidCount,
            mpe_DvrBuffering *tsbSession);
    mpe_Error (*mpeos_dvrTsbBufferingStop_ptr)(mpe_DvrBuffering tsbSession);
    mpe_Error (*mpeos_dvrTsbConvertStart_ptr)(mpe_DvrTsb buffer,
            mpe_MediaVolume volume, int64_t *startTime, int64_t duration,
            mpe_DvrBitRate bitRate, mpe_EventQueue queueId, void *act,
            uint32_t pidTableCount, mpe_DvrPidTable *pidTable,
            mpe_DvrConversion *conversion, char *recordingName);
    mpe_Error (*mpeos_dvrTsbConvertStop_ptr)(mpe_DvrConversion conversion,
            mpe_Bool immediate);
    mpe_Error (*mpeos_dvrTsbConvertChangePids_ptr)(
            mpe_DvrConversion conversion, mpe_DvrPidInfo *pids,
            uint32_t pidCount);
    mpe_Error (*mpeos_dvrTsbDelete_ptr)(mpe_DvrTsb buffer);
    mpe_Error (*mpeos_dvrTsbGet_ptr)(mpe_DvrTsb buffer, mpe_DvrInfoParam param,
            int64_t *time);
    mpe_Error (*mpeos_dvrTsbMediaTimeForFrame_ptr)(mpe_DvrTsb tsb,
            int64_t mediaTime, mpe_DvrDirection direction, int64_t * frameTime);
    mpe_Error (*mpeos_dvrTsbBufferingChangePids_ptr)(
            mpe_DvrBuffering tsbSession, mpe_DvrPidInfo *pids,
            uint32_t pidCount, int64_t *mediaTime);
    mpe_Error (*mpeos_dvrTsbChangeDuration_ptr)(mpe_DvrTsb buffer,
            int64_t duration);
    mpe_Bool (*mpeos_dvrIsDecodable_ptr)(char *recName);
    mpe_Bool (*mpeos_dvrIsDecryptable_ptr)(char *recName);
    mpe_Error (*mpeos_dvrMediaVolumeGetCount_ptr)(mpe_StorageHandle device,
            uint32_t* count);
    mpe_Error (*mpeos_dvrMediaVolumeGetList_ptr)(mpe_StorageHandle device,
            uint32_t* count, mpe_MediaVolume *volumes);
    mpe_Error (*mpeos_dvrMediaVolumeRegisterQueue_ptr)(mpe_EventQueue queueId,
            void* act);
    mpe_Error (*mpeos_dvrMediaVolumeAddAlarm_ptr)(mpe_MediaVolume volume,
            uint8_t level);
    mpe_Error (*mpeos_dvrMediaVolumeRemoveAlarm_ptr)(mpe_MediaVolume volume,
            uint8_t level);
    mpe_Error (*mpeos_dvrMediaVolumeGetInfo_ptr)(mpe_MediaVolume volume,
            mpe_MediaVolumeInfoParam param, void* output);
    mpe_Error (*mpeos_dvrMediaVolumeNew_ptr)(mpe_StorageHandle device,
            char* path, mpe_MediaVolume* volume);
    mpe_Error (*mpeos_dvrMediaVolumeDelete_ptr)(mpe_MediaVolume volume);
    mpe_Error (*mpeos_dvrMediaVolumeSetInfo_ptr)(mpe_MediaVolume volume,
            mpe_MediaVolumeInfoParam param, void* value);
    mpe_Error (*mpeos_dvrRecordingMediaTimeForFrame_ptr)(char * recordingName,
            int64_t mediaTime, mpe_DvrDirection direction, int64_t * frameTime);
    mpe_Error (*mpeos_dvrPlaybackStepFrame_ptr)(mpe_DvrPlayback playback,
            mpe_DvrDirection stepDirection);
    mpe_Error (*mpeos_dvrPlaybackSetMute_ptr)(mpe_DvrPlayback  playback, mpe_Bool mute);
    mpe_Error (*mpeos_dvrPlaybackSetGain_ptr)(mpe_DvrPlayback  playback, float gain, float *actualGain);
    mpe_Error (*mpeos_dvrPlaybackGet3DConfig_ptr)(mpe_DvrPlayback playback, mpe_DispStereoscopicMode* stereoscopicMode,
            mpe_Media3DPayloadType* payloadType, uint8_t* payload, uint32_t* payloadSz);
    mpe_Error (*mpeos_dvrPlaybackSetCCI_ptr)(mpe_DvrPlayback playback, uint8_t cci);
    mpe_Error (*mpeos_dvrPlaybackSetAlarm_ptr)(mpe_DvrPlayback playback, int64_t alarmMediaTime);
    mpe_Error (*mpeos_dvrPlaybackGetVideoScanMode_ptr)(mpe_DvrPlayback playback, mpe_MediaScanMode* scanMode);
} mpe_dvr_ftable_t;

#ifdef __cplusplus
}
;
#endif

#endif /* _DVR_MGR_H_ */
