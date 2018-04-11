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
#ifndef _MPE_HNMGR_H_
#define _MPE_HNMGR_H_

#include <mpeos_hn.h>

#ifdef __cplusplus
extern "C"
{
#endif

void mpe_hn_setup(void);

/******************************************
 * Home Networking Manager function prototypes
 *****************************************/
typedef struct
{
    /*** Shared player/server ***/
    mpe_Error (*mpeos_hnInit_ptr)(void);
    mpe_Error (*mpeos_hnStreamClose_ptr)(mpe_HnStreamSession);
    mpe_Error (*mpeos_hnStreamOpen_ptr)(mpe_HnStreamParams *, mpe_EventQueue, void *, mpe_HnStreamSession *);
    mpe_Error (*mpeos_hnPlaybackStart_ptr)(mpe_HnStreamSession, mpe_HnPlaybackParams *, void *, mpe_HnPlaybackSession *);
    mpe_Error (*mpeos_hnPlaybackStop_ptr)(mpe_HnPlaybackSession, mpe_MediaHoldFrameMode);
    mpe_Error (*mpeos_hnGetMacAddress_ptr)(char *, char [MPE_HN_MAX_MAC_ADDRESS_STR_SIZE]);
    mpe_Error (*mpeos_hnGetNetworkInterfaceType_ptr)(char *, int32_t *);
    mpe_Error (*mpeos_hnPing_ptr)(int32_t, char *, int32_t, int32_t, int32_t, int32_t, int32_t, char *, char *, int32_t *, int32_t *,int32_t *,int32_t *,int32_t *);
    mpe_Error (*mpeos_hnTraceroute_ptr)(int32_t, char *, int32_t, int32_t, int32_t, int32_t, char *, char *, int32_t *,char * );
    mpe_Error (*mpeos_hnNSLookup_ptr)(int32_t, char *, char *, int32_t, char *, char *, char*, char *, char *, char *, int *);
    mpe_Error (*mpeos_hnCancelTest_ptr)(int32_t);

    /*** Server only ***/
    mpe_Error (*mpeos_hnServerUpdateEndPosition_ptr)(mpe_HnPlaybackSession, int64_t);
    mpe_Error (*mpeos_hnServerGetNetworkContentItemSize_ptr)(mpe_HnStreamContentLocation, void *, char *, char *, mpe_hnContentTransformation *, int64_t *);
    mpe_Error (*mpeos_hnServerGetNetworkBytePosition_ptr)(mpe_HnStreamContentLocation, void *, char *, char *, mpe_hnContentTransformation *, int64_t, int64_t *);
    mpe_Error (*mpeos_hnServerGetNetworkBytePositionForMediaTimeNS_ptr)(mpe_HnStreamContentLocation, void *, char *, char *, mpe_hnContentTransformation *, int64_t, int64_t *);
    mpe_Error (*mpeos_hnServerGetDLNAProfileIDsCnt_ptr)(mpe_HnStreamContentLocation, void *, uint32_t *);
    mpe_Error (*mpeos_hnServerGetDLNAProfileIDStr_ptr)(mpe_HnStreamContentLocation, void *, uint32_t, char [MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE]);
    mpe_Error (*mpeos_hnServerGetMimeTypesCnt_ptr)(mpe_HnStreamContentLocation, void *, char *, uint32_t *);
    mpe_Error (*mpeos_hnServerGetMimeTypeStr_ptr)(mpe_HnStreamContentLocation, void *, char *, uint32_t, char [MPE_HN_MAX_MIME_TYPE_STR_SIZE]);
    mpe_Error (*mpeos_hnServerGetPlayspeedsCnt_ptr)(mpe_HnStreamContentLocation, void *, char *, char *, mpe_hnContentTransformation *, uint32_t *);
    mpe_Error (*mpeos_hnServerGetPlayspeedStr_ptr)(mpe_HnStreamContentLocation, void *, char *, char *, mpe_hnContentTransformation *, uint32_t, char [MPE_HN_MAX_PLAYSPEED_STR_SIZE]);
    mpe_Error (*mpeos_hnServerGetFrameTypesInTrickMode_ptr)(mpe_HnStreamContentLocation, void *, char *, char *, mpe_hnContentTransformation *, float, mpe_HnHttpHeaderFrameTypesInTrickMode *);
    mpe_Error (*mpeos_hnServerGetFrameRateInTrickMode_ptr)(mpe_HnStreamContentLocation, void *, char *, char *, mpe_hnContentTransformation *, float, int32_t *);
    mpe_Error (*mpeos_hnServerGetConnectionStallingFlag_ptr)(mpe_HnStreamContentLocation, void *, char *, char *, mpe_hnContentTransformation *, mpe_Bool *);
    mpe_Error (*mpeos_hnServerGetServerSidePacingRestampFlag_ptr)(mpe_HnStreamContentLocation, void *, char *, char *, mpe_hnContentTransformation *, mpe_Bool *);

    /*** Player only ***/
    mpe_Error (*mpeos_hnPlayerStreamGetInfo_ptr)(mpe_HnStreamSession, mpe_HnStreamParams *);
    mpe_Error (*mpeos_hnPlayerPlaybackChangePIDs_ptr)(mpe_HnPlaybackSession,mpe_HnHttpHeaderAVStreamParameters *);
    mpe_Error (*mpeos_hnPlayerPlaybackUpdateCCI_ptr)(mpe_HnPlaybackSession, uint32_t, mpe_HnPlaybackTransportCCI *);
    mpe_Error (*mpeos_hnPlayerPlaybackGetTime_ptr)(mpe_HnPlaybackSession, int64_t *);
    mpe_Error (*mpeos_hnPlayerPlaybackGetRate_ptr)(mpe_HnPlaybackSession, float *);
    mpe_Error (*mpeos_hnPlayerPlaybackBlockPresentation_ptr)(mpe_HnPlaybackSession, mpe_Bool);
    mpe_Error (*mpeos_hnPlayerPlaybackSetMute_ptr)(mpe_HnPlaybackSession, mpe_Bool);
    mpe_Error (*mpeos_hnPlayerPlaybackSetGain_ptr)(mpe_HnPlaybackSession, float, float *);
    mpe_Error (*mpeos_hnPlayerPlaybackGet3DConfig_ptr)(mpe_HnPlaybackSession, mpe_DispStereoscopicMode *, mpe_Media3DPayloadType *, uint8_t *, uint32_t *);
    mpe_Error (*mpeos_hnPlayerGetDLNAProfileIDsCnt_ptr)(uint32_t *);
    mpe_Error (*mpeos_hnPlayerGetDLNAProfileIDStr_ptr)(uint32_t idx, char [MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE]);
    mpe_Error (*mpeos_hnPlayerGetMimeTypesCnt_ptr)(char *, uint32_t *);
    mpe_Error (*mpeos_hnPlayerGetMimeTypeStr_ptr)(char *, uint32_t, char [MPE_HN_MAX_MIME_TYPE_STR_SIZE]);
    mpe_Error (*mpeos_hnPlayerGetPlayspeedsCnt_ptr)(char *, char *, uint32_t *);
    mpe_Error (*mpeos_hnPlayerGetPlayspeedStr_ptr)(char *, char *, uint32_t, char [MPE_HN_MAX_PLAYSPEED_STR_SIZE]);
    mpe_Error (*mpeos_hnPlayerPlaybackPause_ptr)(mpe_HnPlaybackSession);
    mpe_Error (*mpeos_hnPlayerPlaybackResume_ptr)(mpe_HnPlaybackSession);
    mpe_Error (*mpeos_hnPlayerPlaybackGetVideoScanMode_ptr)(mpe_HnPlaybackSession, mpe_MediaScanMode*);
}
mpe_hn_ftable_t;

#ifdef __cplusplus
}
;
#endif

#endif /* _MPE_HNMGR_H_ */
