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


/**
 * Functions defined in file mpetest_media.h will be re-defined here using macros.
 * This will make it easy to support MPE or MPEOS tests.  A simple
 * recompilation will be required to compile a set of tests for MPE or MPEOS.
 *
 * If #define TEST_MPEOS is defined, then tests will be for MPEOS, else MPE.
 */
#ifndef _MPETEST_MEDIA_H_
#define _MPETEST_MEDIA_H_ 1

#include <mpetest_dbg.h>

/* OS Dependant stuff goes here.
 */
#ifdef WIN32
#endif
#ifdef POWERTV
#endif

#ifdef TEST_MPEOS
# include <mpeos_dbg.h>
//# include "mpeos_sys.h"
# include "mpeos_media.h"
# include "../mgr/include/mediamgr.h"

# define MPETEST_MEDIA(x)  mpeos_ ## x

#else
# include <mpe_dbg.h>
# include "mpe_sys.h"
# include "mpe_media.h"
# include "../mgr/include/mediamgr.h"

# define MPETEST_MEDIA(x)  mpe_ ## x

#endif /* TEST_MPEOS */

/**
 * These functions differ from MPE to MPEOS.  Notice the video_ in MPEOS.
 */
/*

 #ifdef TEST_MPEOS
 # define media_setDestinationBounds MPETEST_MEDIA(video_setDestinationBounds)
 # define media_getDestinationBounds MPETEST_MEDIA(video_getDestinationBounds)
 # define media_setSourceBounds      MPETEST_MEDIA(video_setSourceBounds)
 # define media_getSourceBounds      MPETEST_MEDIA(video_getSourceBounds)
 #else
 # define media_setDestinationBounds MPETEST_MEDIA(media_setDestinationBounds)
 # define media_getDestinationBounds MPETEST_MEDIA(media_getDestinationBounds)
 # define media_setSourceBounds      MPETEST_MEDIA(media_setSourceBounds)
 # define media_getSourceBounds      MPETEST_MEDIA(media_getSourceBounds)
 #endif // TEST_MPEOS 

 #define media_shutdown                                MPETEST_MEDIA(media_shutdown)
 #define media_tuner_selectServiceUsingSourceId        MPETEST_MEDIA(media_tuner_selectServiceUsingSourceId)
 #define media_tuner_selectAnalogUsingEIA            MPETEST_MEDIA(media_tuner_selectAnalogUsingEIA)
 #define media_tuner_selectServiceUsingTuningParams  MPETEST_MEDIA(media_tuner_selectServiceUsingTuningParams)
 #define media_decoder_playVideo                        MPETEST_MEDIA(media_decoder_playVideo)
 #define media_decoder_playStill                        MPETEST_MEDIA(media_decoder_playStill)
 #define media_decoder_stopVideo                        MPETEST_MEDIA(media_decoder_stopVideo)
 #define media_decoder_toggleAudio                    MPETEST_MEDIA(media_decoder_toggleAudio)
 #define media_decoder_pauseVideo                    MPETEST_MEDIA(media_decoder_pauseVideo)
 #define media_decoder_setVolume                        MPETEST_MEDIA(media_decoder_setVolume)
 #define media_decoder_getVolume                        MPETEST_MEDIA(media_decoder_getVolume)
 #define media_decoder_mute                            MPETEST_MEDIA(media_decoder_mute)
 #define media_decoder_unMute                        MPETEST_MEDIA(media_decoder_unMute)
 #define media_setSAP                                MPETEST_MEDIA(media_setSAP)
 #define media_getSAP                                MPETEST_MEDIA(media_getSAP)

 */

// New

#define mediaCheckBounds             MPETEST_MEDIA(mediaCheckBounds)
#define mediaDecode                  MPETEST_MEDIA(mediaDecode)
#define mediaBlockPresentation       MPETEST_MEDIA(mediaBlockPresentation)
#define mediaDripFeedStart           MPETEST_MEDIA(mediaDripFeedStart)
#define mediaDripFeedRenderFrame     MPETEST_MEDIA(mediaDripFeedRenderFrame)
#define mediaDripFeedStop            MPETEST_MEDIA(mediaDripFeedStop)
#define mediaFreeze                  MPETEST_MEDIA(mediaFreeze)
#define mediaFrequencyToTuner        MPETEST_MEDIA(mediaFrequencyToTuner)
#define mediaGetBounds               MPETEST_MEDIA(mediaGetBounds)
#define mediaGetMuteState            MPETEST_MEDIA(mediaGetMuteState)
#define mediaGetScaling              MPETEST_MEDIA(mediaGetScaling)
#define mediaGetTunerFrequency       MPETEST_MEDIA(mediaGetTunerFrequency)
#define mediaGetTunerInfo            MPETEST_MEDIA(mediaGetTunerInfo)
#define mediaGetVolume               MPETEST_MEDIA(mediaGetVolume)
#define mediaPause                   MPETEST_MEDIA(mediaPause)
#define mediaRegisterQueueForTuneEvents  MPETEST_MEDIA(mediaRegisterQueueForTuneEvents)
#define mediaResume                  MPETEST_MEDIA(mediaResume)
#define mediaSetBounds               MPETEST_MEDIA(mediaSetBounds)
#define mediaSetMuteState            MPETEST_MEDIA(mediaSetMuteState)
#define mediaSetVolume               MPETEST_MEDIA(mediaSetVolume)
#define mediaShutdown                MPETEST_MEDIA(mediaShutdown)
#define mediaStop                    MPETEST_MEDIA(mediaStop)
#define mediaSwapDecoders            MPETEST_MEDIA(mediaSwapDecoders)
#define mediaTune                    MPETEST_MEDIA(mediaTune)
#define mediaUnregisterQueue         MPETEST_MEDIA(mediaUnregisterQueue)

#endif /* _MPETEST_MEDIA_H_ */ 
