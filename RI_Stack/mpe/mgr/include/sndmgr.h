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

#ifndef _SNDMGR_H_
#define _SNDMGR_H_

#ifdef __cplusplus
extern "C"
{
#endif

#include <mpeos_snd.h>

void mpe_sndSetup(void);
void mpe_sndInit(void);

typedef struct
{
    void (*mpe_sndInit_ptr)(void);

    mpe_Error (*mpe_sndGetDeviceCount_ptr)(uint32_t *count);
    mpe_Error (*mpe_sndGetDevices_ptr)(mpe_SndDevice *devs, uint32_t *count);
    mpe_Error (*mpe_sndGetMaxPlaybacks_ptr)(mpe_SndDevice dev,
            int32_t *maxPlaybacks);
    mpe_Error (*mpe_sndGetDevicesForSound_ptr)(mpe_SndSound sound,
            mpe_SndDevice *devices, uint32_t *count);
    mpe_Error (*mpe_sndCreateSound_ptr)(const char *type, const char *data,
            uint32_t offset, uint32_t size, mpe_SndSound *sound);
    mpe_Error (*mpe_sndDeleteSound_ptr)(mpe_SndSound sound);
    mpe_Error (*mpe_sndPlay_ptr)(mpe_SndDevice device, mpe_SndSound sound,
            mpe_EdHandle handle, int64_t start, mpe_Bool loop, mpe_Bool muted, float requestedGain, float *actualGain,
            mpe_SndPlayback *playback);
    mpe_Error (*mpe_sndStop_ptr)(mpe_SndPlayback playback, int64_t *time);
    mpe_Error (*mpe_sndSetTime_ptr)(mpe_SndPlayback playback, int64_t *time);
    mpe_Error (*mpe_sndGetTime_ptr)(mpe_SndPlayback playback, int64_t *time);
    mpe_Error (*mpe_sndSetMute_ptr)(mpe_SndPlayback playback, mpe_Bool mute);
    mpe_Error (*mpe_sndSetGain_ptr)(mpe_SndPlayback playback, float gain, float *actualGain);

    /* DSExt routines */
    mpe_Error (*mpe_sndAddAudioOutputPort_ptr)(mpe_SndDevice device,
            mpe_SndAudioPort port);
    mpe_Error (*mpe_sndRemoveAudioOutputPort_ptr)(mpe_SndDevice device,
            mpe_SndAudioPort port);
    mpe_Error (*mpe_sndGetAudioOutputPortInfo_ptr)(mpe_SndAudioPort handle,
            mpe_SndAudioOutputPortInfo* info);
    mpe_Error (*mpe_sndSetAudioOutputPortValue_ptr)(mpe_SndAudioPort handle,
            int32_t valueId, void* valuePtr, void* actualValuePtr);

} mpe_snd_ftable_t;

#ifdef __cplusplus
}
;
#endif

#endif /* _SNDMGR_H_ */

