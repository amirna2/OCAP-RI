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

#ifndef _MPE_MEDIA_H_
#define _MPE_MEDIA_H_

#include "mpe_sys.h"
#include "../mgr/include/mediamgr.h"

/* This macro will extract media manager's function table from the master table */
#define mpe_media_ftable ((mpe_media_ftable_t*)(FTABLE[MPE_MGR_TYPE_MEDIA]))

/* These macros redirect calls to media mgr API to the 
 * function pointers in media mgr function table.
 */
#define  mpe_mediaInit              (*(mpe_media_ftable->mpe_mediaInit_ptr))
#define  mpe_mediaShutdown          (*(mpe_media_ftable->mpe_mediaShutdown_ptr))
#define  mpe_mediaTune              (*(mpe_media_ftable->mpe_mediaTune_ptr))
#define  mpe_mediaDecode            (*(mpe_media_ftable->mpe_mediaDecode_ptr))
#define  mpe_mediaStop              (*(mpe_media_ftable->mpe_mediaStop_ptr))
#define  mpe_mediaFreeze            (*(mpe_media_ftable->mpe_mediaFreeze_ptr))
#define  mpe_mediaResume            (*(mpe_media_ftable->mpe_mediaResume_ptr))
#define  mpe_mediaSetBounds         (*(mpe_media_ftable->mpe_mediaSetBounds_ptr))
#define  mpe_mediaGetBounds         (*(mpe_media_ftable->mpe_mediaGetBounds_ptr))
#define  mpe_mediaCheckBounds       (*(mpe_media_ftable->mpe_mediaCheckBounds_ptr))
#define  mpe_mediaSwapDecoders      (*(mpe_media_ftable->mpe_mediaSwapDecoders_ptr))
#define  mpe_mediaGetScaling        (*(mpe_media_ftable->mpe_mediaGetScaling_ptr))
#define  mpe_mediaGetAspectRatio    (*(mpe_media_ftable->mpe_mediaGetAspectRatio_ptr))
#define  mpe_mediaGetAFD            (*(mpe_media_ftable->mpe_mediaGetAFD_ptr))
#define  mpe_mediaGetInputVideoSize (*(mpe_media_ftable->mpe_mediaGetInputVideoSize_ptr))
#define  mpe_mediaDripFeedStart     (*(mpe_media_ftable->mpe_mediaDripFeedStart_ptr))
#define  mpe_mediaDripFeedRenderFrame (*(mpe_media_ftable->mpe_mediaDripFeedRenderFrame_ptr))
#define  mpe_mediaDripFeedStop      (*(mpe_media_ftable->mpe_mediaDripFeedStop_ptr))
#define  mpe_mediaGetSTC            (*(mpe_media_ftable->mpe_mediaGetSTC_ptr))
#define  mpe_mediaBlockPresentation (*(mpe_media_ftable->mpe_mediaBlockPresentation_ptr))
#define  mpe_mediaChangePids        (*(mpe_media_ftable->mpe_mediaChangePids_ptr))
#define	 mpe_mediaSetMute           (*(mpe_media_ftable->mpe_mediaSetMute_ptr))
#define	 mpe_mediaSetGain           (*(mpe_media_ftable->mpe_mediaSetGain_ptr))
#define  mpe_mediaGet3DConfig       (*(mpe_media_ftable->mpe_mediaGet3DConfig_ptr))
#define  mpe_mediaSetCCI            (*(mpe_media_ftable->mpe_mediaSetCCI_ptr))
#define  mpe_mediaGetInputVideoScanMode   (*(mpe_media_ftable->mpe_mediaGetInputVideoScanMode_ptr))

#endif /* _MPE_MEDIA_H_ */

