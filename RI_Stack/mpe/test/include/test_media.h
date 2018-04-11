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

#ifndef _TEST_MEDIA_INCLUDE_H_
#define _TEST_MEDIA_INCLUDE_H_ 1

#include <mpeTest.h>
#include <vte_agent.h>
#include <test_utils.h>
#include <mpetest_media.h>
#include <mpeos_thread.h>
#include <mpeos_media.h>

/*
 * Global variables that are used by all the media tests.
 */

extern mpe_Cond g_tuningCond;
extern mpe_Cond g_decoderCond;
extern int g_mediaInited; /* indicates that mpeos_mediaInit() has been called */

#ifdef WIN32
extern const uint32_t g_ciDEFAULT_SOURCE;
#else
extern const uint32_t g_ciDEFAULT_SOURCE;
#endif

#define MAX_BLOCK_UNBLOCK_TRIES 3
#define DECODE_PERIOD_LONG  5000
#define DECODE_PERIOD_SHORT 2000

/* Defined in test_media_all.c
 */
mpe_EventQueue MediaTest_GetTunerQueue(void);
mpe_EventQueue MediaTest_GetDecoderQueue(void);
mpe_Bool MediaTest_TuneSucceeded(void);
mpe_Bool MediaTest_DecodeSucceeded(void);
void MediaTest_Init(void);
void MediaTest_Destroy(void);

NATIVEEXPORT_API void test_mpe_basicMedia(void);
NATIVEEXPORT_API void test_mediaRunAllTests(void);
NATIVEEXPORT_API void test_mediaRunTunerTests(void);
NATIVEEXPORT_API void test_mediaRunAudioTests(void);
NATIVEEXPORT_API void test_mediaRunDecoderTests(void);
NATIVEEXPORT_API void test_mediaRunVideoTests(void);
NATIVEEXPORT_API void test_mediaRunVideoOneTest(void);
NATIVEEXPORT_API void test_mediaRunVideoTwoTest(void);
NATIVEEXPORT_API void test_mediaRunVideoThreeTest(void);
NATIVEEXPORT_API void test_mediaRunVideoFourTest(void);
NATIVEEXPORT_API void test_mediaRunVideoFiveTest(void);
NATIVEEXPORT_API void test_mediaRunMiscTests(void);
NATIVEEXPORT_API void test_mediaRunNegTests(void);
NATIVEEXPORT_API void test_mediaRunAudioOneTest(void);
NATIVEEXPORT_API void test_mediaRunAudioTwoTest(void);
NATIVEEXPORT_API void test_mediaRunAudioThreeTest(void);
NATIVEEXPORT_API void test_mediaRunAudioFourTest(void);
NATIVEEXPORT_API void test_mediaRunAudioFiveTest(void);
NATIVEEXPORT_API void test_mediaRunAudioSixTest(void);
NATIVEEXPORT_API void test_mediaRunSetSAPTest(void);
NATIVEEXPORT_API void test_mediaRunGetSAPTest(void);
NATIVEEXPORT_API void test_mediaRunPlayStillTest(void);
NATIVEEXPORT_API void test_mediaRunDecoderOneTest(void);
NATIVEEXPORT_API void test_mediaRunDecoderTwoTest(void);
NATIVEEXPORT_API void test_mediaRunDecoderThreeTest(void);
NATIVEEXPORT_API void test_mediaRunDecoderFourTest(void);
NATIVEEXPORT_API void test_mediaRunNegAudioTest(void);
NATIVEEXPORT_API void test_mediaRunNegDecoderTest(void);
NATIVEEXPORT_API void test_mediaRunNegTunerTest(void);
NATIVEEXPORT_API void test_mediaRunNegVideoTest(void);

/*
 * Defined in test_media_tuner.c
 */
mpe_Error test_mediaTunerDoSuccessfulTune(char* errorBuffer);
mpe_Error test_mediaTunerTuneAndProcessEvents(char* errorBuffer,
        mpe_MediaTuneRequestParams* requestParams, int* ACT);

/*
 * Defined in test_media_decoder.c
 */
mpe_Error test_mediaDecodeDoSuccessfulDecode(char *errorBuffer,
        mpe_MediaDecodeRequestParams *decodeRequest,
        mpe_MediaDecodeSession *decodeSession);
mpe_Error test_mediaDecodeSetupVideoDevices();
mpe_Error test_mediaDecodeTeardownVideoDevices();
mpe_DispDevice test_mediaDecodeGetDefaultVideoDevice();

#endif /* _TEST_MEDIA_INCLUDE_H_ */

