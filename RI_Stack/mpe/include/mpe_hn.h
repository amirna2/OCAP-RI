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

#ifndef _MPE_HN_MGR_BINDINGS_H_
#define _MPE_HN_MGR_BINDINGS_H_

#include "mpe_sys.h"
#include "../mgr/include/mgrdef.h"
#include "../mgr/include/hnmgr.h"

#define mpe_hn_mgr_ftable ((mpe_hn_ftable_t*)(FTABLE[MPE_MGR_TYPE_HN]))

/*** Shared player/server ***/
#define mpe_hnInit (mpe_hn_mgr_ftable->mpeos_hnInit_ptr)
#define mpe_hnStreamClose (mpe_hn_mgr_ftable->mpeos_hnStreamClose_ptr)
#define mpe_hnStreamOpen (mpe_hn_mgr_ftable->mpeos_hnStreamOpen_ptr)
#define mpe_hnPlaybackStart (mpe_hn_mgr_ftable->mpeos_hnPlaybackStart_ptr)
#define mpe_hnPlaybackStop (mpe_hn_mgr_ftable->mpeos_hnPlaybackStop_ptr)
#define mpe_hnGetMacAddress (mpe_hn_mgr_ftable->mpeos_hnGetMacAddress_ptr)
#define mpe_hnGetNetworkInterfaceType (mpe_hn_mgr_ftable->mpeos_hnGetNetworkInterfaceType_ptr)
#define mpe_hnPing (mpe_hn_mgr_ftable->mpeos_hnPing_ptr)
#define mpe_hnTraceroute (mpe_hn_mgr_ftable->mpeos_hnTraceroute_ptr)
#define mpe_hnNSLookup (mpe_hn_mgr_ftable->mpeos_hnNSLookup_ptr)
#define mpe_hnCancelTest (mpe_hn_mgr_ftable->mpeos_hnCancelTest_ptr)

/*** Server only ***/
#define mpe_hnServerUpdateEndPosition (mpe_hn_mgr_ftable->mpeos_hnServerUpdateEndPosition_ptr)
#define mpe_hnServerGetNetworkContentItemSize (mpe_hn_mgr_ftable->mpeos_hnServerGetNetworkContentItemSize_ptr)
#define mpe_hnServerGetNetworkBytePosition (mpe_hn_mgr_ftable->mpeos_hnServerGetNetworkBytePosition_ptr)
#define mpe_hnServerGetNetworkBytePositionForMediaTimeNS (mpe_hn_mgr_ftable->mpeos_hnServerGetNetworkBytePositionForMediaTimeNS_ptr)
#define mpe_hnServerGetDLNAProfileIDsCnt (mpe_hn_mgr_ftable->mpeos_hnServerGetDLNAProfileIDsCnt_ptr)
#define mpe_hnServerGetDLNAProfileIDStr (mpe_hn_mgr_ftable->mpeos_hnServerGetDLNAProfileIDStr_ptr)
#define mpe_hnServerGetMimeTypesCnt (mpe_hn_mgr_ftable->mpeos_hnServerGetMimeTypesCnt_ptr)
#define mpe_hnServerGetMimeTypeStr (mpe_hn_mgr_ftable->mpeos_hnServerGetMimeTypeStr_ptr)
#define mpe_hnServerGetPlayspeedsCnt (mpe_hn_mgr_ftable->mpeos_hnServerGetPlayspeedsCnt_ptr)
#define mpe_hnServerGetPlayspeedStr (mpe_hn_mgr_ftable->mpeos_hnServerGetPlayspeedStr_ptr)
#define mpe_hnServerGetFrameTypesInTrickMode (mpe_hn_mgr_ftable->mpeos_hnServerGetFrameTypesInTrickMode_ptr)
#define mpe_hnServerGetFrameRateInTrickMode (mpe_hn_mgr_ftable->mpeos_hnServerGetFrameRateInTrickMode_ptr)
#define mpe_hnServerGetConnectionStallingFlag (mpe_hn_mgr_ftable->mpeos_hnServerGetConnectionStallingFlag_ptr)
#define mpe_hnServerGetServerSidePacingRestampFlag (mpe_hn_mgr_ftable->mpeos_hnServerGetServerSidePacingRestampFlag_ptr)

/*** Player only ***/
#define mpe_hnPlayerStreamGetInfo (mpe_hn_mgr_ftable->mpeos_hnPlayerStreamGetInfo_ptr)
#define mpe_hnPlayerPlaybackChangePIDs (mpe_hn_mgr_ftable->mpeos_hnPlayerPlaybackChangePIDs_ptr)
#define mpe_hnPlayerPlaybackUpdateCCI (mpe_hn_mgr_ftable->mpeos_hnPlayerPlaybackUpdateCCI_ptr)
#define	mpe_hnPlayerPlaybackGetTime (mpe_hn_mgr_ftable->mpeos_hnPlayerPlaybackGetTime_ptr)
#define	mpe_hnPlayerPlaybackGetRate (mpe_hn_mgr_ftable->mpeos_hnPlayerPlaybackGetRate_ptr)
#define mpe_hnPlayerPlaybackBlockPresentation (mpe_hn_mgr_ftable->mpeos_hnPlayerPlaybackBlockPresentation_ptr)
#define	mpe_hnPlayerPlaybackSetMute (mpe_hn_mgr_ftable->mpeos_hnPlayerPlaybackSetMute_ptr)
#define	mpe_hnPlayerPlaybackSetGain (mpe_hn_mgr_ftable->mpeos_hnPlayerPlaybackSetGain_ptr)
#define mpe_hnPlayerPlaybackGet3DConfig (mpe_hn_mgr_ftable->mpeos_hnPlayerPlaybackGet3DConfig_ptr)
#define mpe_hnPlayerGetDLNAProfileIDsCnt (mpe_hn_mgr_ftable->mpeos_hnPlayerGetDLNAProfileIDsCnt_ptr)
#define mpe_hnPlayerGetDLNAProfileIDStr (mpe_hn_mgr_ftable->mpeos_hnPlayerGetDLNAProfileIDStr_ptr)
#define mpe_hnPlayerGetMimeTypesCnt (mpe_hn_mgr_ftable->mpeos_hnPlayerGetMimeTypesCnt_ptr)
#define mpe_hnPlayerGetMimeTypeStr (mpe_hn_mgr_ftable->mpeos_hnPlayerGetMimeTypeStr_ptr)
#define mpe_hnPlayerGetPlayspeedsCnt (mpe_hn_mgr_ftable->mpeos_hnPlayerGetPlayspeedsCnt_ptr)
#define mpe_hnPlayerGetPlayspeedStr (mpe_hn_mgr_ftable->mpeos_hnPlayerGetPlayspeedStr_ptr)
#define mpe_hnPlayerPlaybackPause (mpe_hn_mgr_ftable->mpeos_hnPlayerPlaybackPause_ptr)
#define mpe_hnPlayerPlaybackResume (mpe_hn_mgr_ftable->mpeos_hnPlayerPlaybackResume_ptr)
#define mpe_hnPlayerPlaybackGetVideoScanMode (mpe_hn_mgr_ftable->mpeos_hnPlayerPlaybackGetVideoScanMode_ptr)

#endif // _MPE_HN_MGR_BINDINGS_H_
