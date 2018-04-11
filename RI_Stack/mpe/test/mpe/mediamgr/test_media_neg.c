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

#include <test_media.h>

static void test_media_neg_decoder(CuTest* tc);
static void test_media_neg_tuner(CuTest* tc);
static void test_media_neg_video(CuTest* tc);

CuSuite* getTestSuite_mediaNeg(void);

/**
 * Test decoder API's that could recieve NULL.
 *
 * @assert 1. Set volumn in getVolume to NULL.
 * @assert 2. Set EventQueue as NULL in media_decoder_playVideo.
 * @assert 3. Set status as NULL in media_decoder_playVideo.
 * @assert 4. Set iFrameBuf as NULL in media_decoder_playStill.
 * @assert 5. Set EventQueue as NULL in media_decoder_playStill.
 * @assert 6. Set status as NULL in media_decoder_playStill.
 *
 * Asserts 2, 3, 4, 5, and 6 require media_getLastError() to get error results.
 * media_decoder_playStill is not supported yet, so it's difficult to determine
 * how it should work.
 */
static void test_media_neg_decoder(CuTest* tc)
{

    // uiZero is an invalid decoderId.
#if 0
    uint32_t uiDecoderId = 0;
    mpe_MediaTuneResponseParams* pTunerResponse = MediaTest_GetTunerResponse();
    mpe_EventQueue decoderQueue = MediaTest_GetDecoderQueue();
    mpe_MediaDecodeResponseParams* pDecoderResponse = MediaTest_GetDecoderResponse();

#if 0 /* removing until media volume APIs return (if they ever do) */
    // Assert 1.
    CuAssertIntEquals( tc,
            MPE_EINVAL,
            mediaGetVolume( NULL ) );
#endif

    // The following asserts 2-6 require a way of getting the last error.
    // Also, it requires understanding if conditions are required to trap the
    // error that comes from the decoder thread.....DEBUG

    // Assert 2.
    media_decoder_playVideo( uiDecoderId, NULL, &pDecoderStatus );
    CuAssertIntEquals( tc, MPE_EINVAL, pStatus->error );

    // Assert 3.
    media_decoder_playVideo( uiDecoderId, decoderQueue, NULL );
    CuAssertIntEquals( tc, MPE_EINVAL, pStatus->error );

    // Assert 4.
    uint8_t iFrameBuf[100];
    media_decoder_playStill( uiDecoderId, NULL, 0, decoderQueue, pDecoderStatus );
    CuAssertIntEquals( tc, MPE_EINVAL, pStatus->error );

    // Assert 5.
    media_decoder_playStill( uiDecoderId, iFrameBuf, 0, NULL, pDecoderStatus );
    CuAssertIntEquals( tc, MPE_EINVAL, pStatus->error );

    // Assert 6.
    media_decoder_playStill( uiDecoderId, iFrameBuf, 0, decoderQueue, NULL );
    CuAssertIntEquals( tc, MPE_EINVAL, pStatus->error );
#endif // #if 0
} // end test_media_neg_decoder(CuTest*)

/**
 * Test audio API's that could recieve NULL.
 *
 * @assert 1. 
 * @assert 2. selectService w/ NULL status.
 * @assert 3. selectService w/ NULL queue.
 * @assert 4. selectService w/ NULL status.
 * @assert 5. selectService w/ NULL queue.
 * @assert 6. selectService w/ NULL status.
 */
static void test_media_neg_tuner(CuTest* tc)
{
    // The following asserts 1-6 require a way of getting the last error.
    // Also, it requires understanding if conditions are required to trap the
    // error that comes from the tuner thread.....DEBUG

    uint32_t tunerId = 1;
    mpe_EventQueue queueId = MediaTest_GetTunerQueue();
    mpe_MediaTuneRequestParams reqParams;
    reqParams.tunerId = tunerId;
    reqParams.tuneParams.tuneType = MPE_MEDIA_TUNE_BY_TUNING_PARAMS;
    reqParams.tuneParams.frequency = TEST_FREQUENCY_DIGITAL_1;
    reqParams.tuneParams.programNumber = TEST_PROGRAM_NUMBER_DIGITAL_1;
    reqParams.tuneParams.qamMode = TEST_QAM_MODE_DIGITAL_1;

    // Invalid values do send and event to tuner callback.

    // Assert 1. mediaTune w/ NULL queue id.
    CuAssertIntEquals(tc, MPE_EINVAL, mediaTune(&reqParams, NULL, NULL));

    // Assert 2. mediaTune w/ NULL pStatus.
    CuAssertIntEquals(tc, MPE_EINVAL, mediaTune(&reqParams, queueId, NULL));

    // Assert 6. mediaTune w/ NULL request.
    CuAssertIntEquals(tc, MPE_EINVAL, mediaTune(NULL, queueId, NULL));

} // end test_media_neg_tuner(CuTest*)

/**
 * Test video API's that could recieve NULL.
 *
 * @assert 1. getDestinationBounds w/ NULL rectangle.
 * @assert 2. getSourceBounds w/ NULL rectangle.
 * @assert 3. setDestinationBounds w/ NULL rectangle.
 * @assert 4. setSourceBounds w/ NULL rectanlge.
 */
static void test_media_neg_video(CuTest* tc)
{
    mpe_MediaRectangle srcRect;
    mpe_MediaRectangle destRect;
    destRect.x = 100;
    destRect.y = 100;
    destRect.width = 300;
    destRect.height = 300;

    srcRect.x = 0;
    srcRect.y = 0;
    srcRect.width = 640;
    srcRect.height = 480;

    // Assert 1. setBounds w/ NULL dest rectangle.
    CuAssertIntEquals(tc, MPE_EINVAL, mediaGetBounds(NULL, &srcRect, NULL));

    // Assert 2. getBounds w/ NULL src rectangle.
    CuAssertIntEquals(tc, MPE_EINVAL, mediaSetBounds(NULL, NULL, &destRect));

    /*
     // Assert 3. setDestinationBounds w/ NULL rectangle.
     CuAssertIntEquals( tc, MPE_EINVAL, media_setDestinationBounds( NULL ) );

     // Assert 4. setSourceBounds w/ NULL rectanlge.
     CuAssertIntEquals( tc, MPE_EINVAL, media_setSourceBounds( NULL ) );
     */

} // end test_media_neg_video(CuTest*)

CuSuite* getTestSuite_mediaNeg(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, test_media_neg_decoder);
    SUITE_ADD_TEST(suite, test_media_neg_tuner);
    SUITE_ADD_TEST(suite, test_media_neg_video);

    return suite;
} // end getTestSuite_mediaNeg()
