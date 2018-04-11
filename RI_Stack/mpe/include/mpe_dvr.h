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

#ifndef _MPE_DVR_MGR_BINDINGS_H_
#define _MPE_DVR_MGR_BINDINGS_H_

#include "mpe_sys.h"
#include "../mgr/include/mgrdef.h"
#include "../mgr/include/dvr_mgr.h"

#define mpe_dvr_mgr_ftable ((mpe_dvr_ftable_t*)(FTABLE[MPE_MGR_TYPE_DVR]))

#define mpe_dvrInit (mpe_dvr_mgr_ftable->mpe_dvr_init_ptr)

#define mpe_dvrGet (mpe_dvr_mgr_ftable->mpeos_dvrGet_ptr)
#define mpe_dvrGetRecordingList (mpe_dvr_mgr_ftable->mpeos_dvrGetRecordingList_ptr)
#define mpe_dvrFreeRecordingList (mpe_dvr_mgr_ftable->mpeos_dvrFreeRecordingList_ptr)
#define mpe_dvrGetPlayScales (mpe_dvr_mgr_ftable->mpeos_dvrGetPlayScales_ptr)
#define mpe_dvrGetLowPowerResumeTime (mpe_dvr_mgr_ftable->mpeos_dvrGetLowPowerResumeTime_ptr)
#define mpe_dvrResumeFromLowPower (mpe_dvr_mgr_ftable->mpeos_dvrResumeFromLowPower_ptr)

#define mpe_dvrRecordingDelete (mpe_dvr_mgr_ftable->mpeos_dvrRecordingDelete_ptr)
#define mpe_dvrRecordingGet (mpe_dvr_mgr_ftable->mpeos_dvrRecordingGet_ptr)

#define mpe_dvrRecordingPlayStart (mpe_dvr_mgr_ftable->mpeos_dvrRecordingPlayStart_ptr)
#define mpe_dvrTsbPlayStart (mpe_dvr_mgr_ftable->mpeos_dvrTsbPlayStart_ptr)
#define mpe_dvrPlayBackStop (mpe_dvr_mgr_ftable->mpeos_dvrPlayBackStop_ptr)
#define mpe_dvrPlaybackGetTime (mpe_dvr_mgr_ftable->mpeos_dvrPlaybackGetTime_ptr)
#define mpe_dvrPlaybackSetTime (mpe_dvr_mgr_ftable->mpeos_dvrPlaybackSetTime_ptr)
#define mpe_dvrPlaybackChangePids (mpe_dvr_mgr_ftable->mpeos_dvrPlaybackChangePids_ptr)
#define mpe_dvrPlaybackBlockPresentation (mpe_dvr_mgr_ftable->mpeos_dvrPlaybackBlockPresentation_ptr)

#define mpe_dvrSetTrickMode (mpe_dvr_mgr_ftable->mpeos_dvrSetTrickMode_ptr)
#define mpe_dvrGetTrickMode (mpe_dvr_mgr_ftable->mpeos_dvrGetTrickMode_ptr)

#define mpe_dvrTsbNew (mpe_dvr_mgr_ftable->mpeos_dvrTsbNew_ptr)
#define mpe_dvrTsbBufferingStart (mpe_dvr_mgr_ftable->mpeos_dvrTsbBufferingStart_ptr)
#define mpe_dvrTsbBufferingStop (mpe_dvr_mgr_ftable->mpeos_dvrTsbBufferingStop_ptr)
#define mpe_dvrTsbStopRecording (mpe_dvr_mgr_ftable->mpeos_dvrTsbStopRecording_ptr)
#define mpe_dvrTsbDelete (mpe_dvr_mgr_ftable->mpeos_dvrTsbDelete_ptr)
#define mpe_dvrTsbGet (mpe_dvr_mgr_ftable->mpeos_dvrTsbGet_ptr)
#define mpe_dvrTsbBufferingChangePids (mpe_dvr_mgr_ftable->mpeos_dvrTsbBufferingChangePids_ptr)

#define mpe_dvrTsbChangeDuration (mpe_dvr_mgr_ftable->mpeos_dvrTsbChangeDuration_ptr)
#define mpe_dvrTsbConvertStart (mpe_dvr_mgr_ftable->mpeos_dvrTsbConvertStart_ptr)
#define mpe_dvrTsbConvertStop (mpe_dvr_mgr_ftable->mpeos_dvrTsbConvertStop_ptr)
#define mpe_dvrTsbConvertChangePids (mpe_dvr_mgr_ftable->mpeos_dvrTsbConvertChangePids_ptr)
#define	mpe_dvrTsbMediaTimeForFrame (mpe_dvr_mgr_ftable->mpeos_dvrTsbMediaTimeForFrame_ptr)

#define mpe_dvrIsDecodable (mpe_dvr_mgr_ftable->mpeos_dvrIsDecodable_ptr)
#define mpe_dvrIsDecryptable (mpe_dvr_mgr_ftable->mpeos_dvrIsDecryptable_ptr)

#define	mpe_dvrMediaVolumeGetCount (mpe_dvr_mgr_ftable->mpeos_dvrMediaVolumeGetCount_ptr)
#define	mpe_dvrMediaVolumeGetList (mpe_dvr_mgr_ftable->mpeos_dvrMediaVolumeGetList_ptr)
#define	mpe_dvrMediaVolumeRegisterQueue (mpe_dvr_mgr_ftable->mpeos_dvrMediaVolumeRegisterQueue_ptr)
#define	mpe_dvrMediaVolumeAddAlarm (mpe_dvr_mgr_ftable->mpeos_dvrMediaVolumeAddAlarm_ptr)
#define	mpe_dvrMediaVolumeRemoveAlarm (mpe_dvr_mgr_ftable->mpeos_dvrMediaVolumeRemoveAlarm_ptr)
#define	mpe_dvrMediaVolumeGetInfo (mpe_dvr_mgr_ftable->mpeos_dvrMediaVolumeGetInfo_ptr)
#define	mpe_dvrMediaVolumeNew (mpe_dvr_mgr_ftable->mpeos_dvrMediaVolumeNew_ptr)
#define	mpe_dvrMediaVolumeDelete (mpe_dvr_mgr_ftable->mpeos_dvrMediaVolumeDelete_ptr)
#define	mpe_dvrMediaVolumeSetInfo (mpe_dvr_mgr_ftable->mpeos_dvrMediaVolumeSetInfo_ptr)
#define	mpe_dvrRecordingMediaTimeForFrame (mpe_dvr_mgr_ftable->mpeos_dvrRecordingMediaTimeForFrame_ptr)
#define	mpe_dvrPlaybackStepFrame (mpe_dvr_mgr_ftable->mpeos_dvrPlaybackStepFrame_ptr)

#define	mpe_dvrPlaybackSetMute (mpe_dvr_mgr_ftable->mpeos_dvrPlaybackSetMute_ptr)
#define	mpe_dvrPlaybackSetGain (mpe_dvr_mgr_ftable->mpeos_dvrPlaybackSetGain_ptr)
#define mpe_dvrPlaybackGet3DConfig (mpe_dvr_mgr_ftable->mpeos_dvrPlaybackGet3DConfig_ptr)
#define mpe_dvrPlaybackSetCCI (mpe_dvr_mgr_ftable->mpeos_dvrPlaybackSetCCI_ptr)
#define mpe_dvrPlaybackSetAlarm (mpe_dvr_mgr_ftable->mpeos_dvrPlaybackSetAlarm_ptr)
#define mpe_dvrPlaybackGetVideoScanMode (mpe_dvr_mgr_ftable->mpeos_dvrPlaybackGetVideoScanMode_ptr)

#endif

