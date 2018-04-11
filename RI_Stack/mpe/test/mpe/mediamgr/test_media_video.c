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

/**
 * This will test media_video_getDestinationBounds. It must be run first
 * the first test assert assumes the STB has been initialized.
 *
 * @purpose 1. Given the default value of the destination rect, we should be
 *          able to get the dest rect (being 640x480) and it should return
 *          MPE_SUCCESS.
 * @purpose 2. Set the destination rectangle to an invalid rectangle, it should
 *          not be able to set the rectangle, therefore the result should be
 *          a rectangle of 640x480, and return MPE_SUCCESS.
 * @purpose 3. Set the destination to a normal setting, 100x32. It should return
 *          a rectanlge of 100x32, and return MPE_SUCCESS.
 * @purpose 4. Send NULL as the rectanlge pointer, should return MPE_EINVAL.
 *
 * @assert 1. Get dest, test dist 640x480 and return MPE_SUCCESS.
 * @assert 2. Set invalid dest, test return MPE_SUCCESS and = 640x480.
 * @assert 3. Set dest to rect 100x32, test return MPE_SUCCESS and = 100x32.
 * @assert 4. Send NULL as rectangle, should return MPE_EINVAL.
 */
static void test_video_one(CuTest* tc)
{
#if 0
    mpe_MediaAsyncTunerStatus* pTunerStatus = MediaTest_GetTunerStatus();
    mpe_MediaDecoderStatus* pDecoderStatus = MediaTest_GetDecoderStatus();
    mpe_EventQueue tunerQueue = MediaTest_GetTunerQueue();
    mpe_EventQueue decoderQueue = MediaTest_GetDecoderQueue();
    uint32_t decoderId = 0;
    uint32_t tunerId = 1;
    uint32_t sourceId = g_ciDEFAULT_SOURCE;
    mpe_MediaRectangle rect;
    uint32_t x = 0;
    uint32_t y = 0;
    uint32_t width;
    uint32_t height;

    // Init width and height for test (bug 1178).
    CuAssertIntEquals( tc, MPE_SUCCESS,
            media_getDestinationBounds( &rect ) );

    width = rect.width;
    height = rect.height;

    // Tune to tunerId and play video to be used in tests.
    media_tuner_selectServiceUsingSourceId( tunerId,
            sourceId,
            tunerQueue,
            pTunerStatus );

    CuAssertIntEquals( tc, MPE_SUCCESS, condGet( g_tuningCond ) );
    CuAssertIntEquals( tc, MPE_SUCCESS, pTunerStatus->error );

    media_decoder_playVideo( decoderId, decoderQueue, pDecoderStatus );
    test_threadSleep( 5000, 0 ); // Pause for 2 sec

    // Assert 1. Get dest, should result in 640x480 and SUCCESS
    CuAssertIntEquals( tc,
            MPE_SUCCESS,
            media_getDestinationBounds( &rect ) );
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "media_getDestinationBounds() returned %d %d %d %d...\n", rect.x, rect.y, rect.width, rect.height);

    CuAssertIntEquals( tc, x, rect.x );
    CuAssertIntEquals( tc, y, rect.y );
    CuAssertIntEquals( tc, width, rect.width );
    CuAssertIntEquals( tc, height, rect.height );

    test_threadSleep( 3000, 0 ); // Pause for 2 sec
    // Assert 2. Set invalid dest, test return SUCCESS and = 640x480.
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "set bounds to NULL...\n");
    CuAssertIntEquals( tc,
            MPE_EINVAL,
            media_setDestinationBounds( NULL ) );
    test_threadSleep( 3000, 0 ); // Pause for 2 sec

    CuAssertIntEquals( tc, MPE_SUCCESS,
            media_getDestinationBounds( &rect ) );
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "media_getDestinationBounds() returned %d %d %d %d...\n", rect.x, rect.y, rect.width, rect.height);

    CuAssertIntEquals( tc, x, rect.x );
    CuAssertIntEquals( tc, y, rect.y );
    CuAssertIntEquals( tc, width, rect.width );
    CuAssertIntEquals( tc, height, rect.height );

    // Assert 3. Set dest to 100x32, test returns SUCCESS and = 100x32.

    // Set both test and source values.
    rect.x = x = 0;
    rect.y = y = 0;
    rect.width = width = 100;
    rect.height = height = 132;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "set bounds to 0, 0, 100, 132...\n");
    CuAssertIntEquals( tc,
            MPE_SUCCESS,
            media_setDestinationBounds( &rect ) );
    test_threadSleep( 3000, 0 ); // Pause for 2 sec

    CuAssertIntEquals( tc,
            MPE_SUCCESS,
            media_getDestinationBounds( &rect ) );

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "media_getDestinationBounds() returned %d %d %d %d...\n",
            rect.x, rect.y, rect.width, rect.height);

    CuAssertIntEquals( tc, x, rect.x );
    CuAssertIntEquals( tc, y, rect.y );
    CuAssertIntEquals( tc, width, rect.width );
    CuAssertIntEquals( tc, height, rect.height );
    test_threadSleep( 3000, 0 ); // Pause for 2 sec

    // Assert 4. Send NULL pointer for rectangle should return MPE_EINVAL.
    CuAssertIntEquals( tc,
            MPE_EINVAL,
            media_getDestinationBounds( NULL ) );
#endif
} // test_video_one(CuTest*)

/**
 * This will test_video_setDestinationBounds.
 *
 * @assert 1. Set destination rect to 320x240, should return MPE_SUCCESS.
 * @assert 2. Set destination rect to 100x32, should return MPE_SUCCESS.
 * @assert 4. Set destination rect after player started, return MPE_SUCCESS.
 * @assert 5. Set destination rect before player started, return MPE_SUCCESS.
 * @assert 6. Set destination rect to 320x240, player should work with it.
 * @assert 7. NULL rectangle should return MPE_EINVAL.
 * @assert 8. Invalid destination rect, should return FALSE.
 */
static void test_video_two(CuTest* tc)
{
#if 0
    mpe_MediaAsyncTunerStatus* pTunerStatus = MediaTest_GetTunerStatus();
    mpe_MediaDecoderStatus* pDecoderStatus = MediaTest_GetDecoderStatus();
    mpe_EventQueue tunerQueue = MediaTest_GetTunerQueue();
    mpe_EventQueue decoderQueue = MediaTest_GetDecoderQueue();
    uint32_t decoderId = 0;
    uint32_t tunerId = 1;
    uint32_t sourceId = g_ciDEFAULT_SOURCE;
    mpe_MediaRectangle rect;

    // Tune to tunerId and play video to be used in tests.
    media_tuner_selectServiceUsingSourceId( tunerId,
            sourceId,
            tunerQueue,
            pTunerStatus );

    CuAssertIntEquals( tc, MPE_SUCCESS, condGet( g_tuningCond ) );
    CuAssertIntEquals( tc, MPE_SUCCESS, pTunerStatus->error );

    media_decoder_playVideo( decoderId, decoderQueue, pDecoderStatus );
    test_threadSleep( 10000, 0 ); // Pause for 2 sec

    // Assert 1. Set destination rect to 320x240, should return TRUE.
    rect.x = 0;
    rect.y = 0;
    rect.width = 320;
    rect.height = 240;

    CuAssertIntEquals( tc, MPE_SUCCESS,
            media_setDestinationBounds( &rect ) );

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "setting video bounds to 320X240...\n");

    test_threadSleep( 10000, 0 ); // Pause for 2 sec

    // Assert 2. Set destination rect to 100x32, should return TRUE.
    rect.x = 100;
    rect.y = 100;
    rect.width = 200;
    rect.height = 300;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "setting video bounds to 100, 100, 200X300...\n");

    CuAssertIntEquals( tc, MPE_SUCCESS,
            media_setDestinationBounds( &rect ) );

    test_threadSleep( 10000, 0 ); // Pause for 2 sec

    // Assert 4. Set destination rect after player started, return TRUE.
    pDecoderStatus->error = MPE_SUCCESS;
    test_threadSleep(5000,0);

    rect.x = 10;
    rect.y = 10;
    rect.width = 320;
    rect.height = 240;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "setting video bounds to 10, 10, 320X240...\n");
    CuAssertIntEquals( tc, MPE_SUCCESS,
            media_setDestinationBounds( &rect ) );

    // Assert 5. Set destination rect before player started, return TRUE.
    rect.x = -10;
    rect.y = -10;
    rect.width = 320;
    rect.height = 240;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "setting video bounds to -10, -10, 320X240...\n");
    CuAssertIntEquals( tc, MPE_EINVAL,
            media_setDestinationBounds( &rect ) );

    test_threadSleep(5000,0);

    // Assert 6. Set destination rect to 320x240, player should work w/ it.
    rect.x = 0;
    rect.y = 0;
    rect.width = 320;
    rect.height = 240;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "setting video bounds to 320X240...\n");
    CuAssertIntEquals( tc, MPE_SUCCESS,
            media_setDestinationBounds( &rect ) );

    test_threadSleep(2000,0);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "setting video bounds to NULL...\n");

    // Assert 7. NULL destination rect should return MPE_EINVAL.
    CuAssertIntEquals( tc,
            MPE_EINVAL,
            media_setDestinationBounds( NULL ) );

    // Assert 8. Invalid destination rect, should return FALSE.
    rect.x = -100;
    rect.y = -100;
    rect.width = -100;
    rect.height = -100;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "setting video bounds to -100, -100, -100, -100...\n");
    CuAssertIntEquals( tc,
            MPE_EINVAL,
            media_setDestinationBounds( &rect ) );
#endif

} // test_video_two(CuTest*)

/**
 * This will test media_video_getSourceBounds().
 *
 * @assert 1. Set source in bounds then get, should =, + return MPE_SUCCESS.
 * @assert 2. Set source at max bounds then get, should =, + return MPE_SUCCESS.
 * @assert 3. Set source to small value then get, should =, + return MPE_SUCCES.
 * @assert 4. Set source > max value, returns MPE_EINVAL.
 * @assert 5. Set source to NULL pointer, returns MPE_EINVAL.
 */
static void test_video_three(CuTest* tc)
{
#if 0
    mpe_MediaAsyncTunerStatus* pTunerStatus = MediaTest_GetTunerStatus();
    mpe_MediaDecoderStatus* pDecoderStatus = MediaTest_GetDecoderStatus();
    mpe_EventQueue tunerQueue = MediaTest_GetTunerQueue();
    mpe_EventQueue decoderQueue = MediaTest_GetDecoderQueue();
    uint32_t decoderId = 0;
    uint32_t tunerId = 1;
    uint32_t sourceId = g_ciDEFAULT_SOURCE;
    mpe_MediaRectangle rect;
    uint32_t x, y, width, height;

    // Tune to tunerId and play video to be used in tests.
    media_tuner_selectServiceUsingSourceId( tunerId,
            sourceId,
            tunerQueue,
            pTunerStatus );

    CuAssertIntEquals( tc, MPE_SUCCESS, condGet( g_tuningCond ) );

    // Assert 1. Set source in bounds then get, should =, + return MPE_SUCCESS.
    rect.x = x = 0;
    rect.y = y = 0;
    rect.width = width = 320;
    rect.height = height = 240;

    CuAssertIntEquals( tc, MPE_SUCCESS, media_setSourceBounds( &rect ) );
    CuAssertIntEquals( tc,
            MPE_SUCCESS,
            media_getSourceBounds( &rect ) );

    CuAssertIntEquals( tc, x, rect.x );
    CuAssertIntEquals( tc, y, rect.y );
    CuAssertIntEquals( tc, width, rect.width );
    CuAssertIntEquals( tc, height, rect.height );

    // Play video
    media_decoder_playVideo( decoderId, decoderQueue, pDecoderStatus );
    test_threadSleep( 2000, 0 );
    CuAssertIntEquals( tc, MPE_SUCCESS, media_decoder_stopVideo(decoderId) );

    // Assert 2. Set source at bounds, should get same and return MPE_SUCCESS.
    rect.x = x = 0;
    rect.y = y = 0;
    rect.width = width = 640;
    rect.height = height = 480;

    CuAssertIntEquals( tc, MPE_SUCCESS, media_setSourceBounds( &rect ) );
    CuAssertIntEquals( tc, MPE_SUCCESS, media_getSourceBounds(&rect) );

    CuAssertIntEquals( tc, x, rect.x );
    CuAssertIntEquals( tc, y, rect.y );
    CuAssertIntEquals( tc, width, rect.width );
    CuAssertIntEquals( tc, height, rect.height );

    // Play video
    media_decoder_playVideo( decoderId, decoderQueue, pDecoderStatus );
    test_threadSleep( 2000, 0 );
    CuAssertIntEquals( tc, MPE_SUCCESS, media_decoder_stopVideo(decoderId) );

    // Assert 3. Set source to small value, then get check = + return value.
    rect.x = x = (uint32_t) -10;
    rect.y = y = (uint32_t) -10;
    rect.width = width = (uint32_t) -100;
    rect.height = height = (uint32_t) -100;

    CuAssertIntEquals( tc, MPE_SUCCESS, media_setSourceBounds( &rect ) );
    CuAssertIntEquals( tc, MPE_SUCCESS, media_getSourceBounds(&rect) );

    CuAssertIntEquals( tc, x, rect.x );
    CuAssertIntEquals( tc, y, rect.y );
    CuAssertIntEquals( tc, width, rect.width );
    CuAssertIntEquals( tc, height, rect.height );

    // Play video
    media_decoder_playVideo( decoderId, decoderQueue, pDecoderStatus );
    test_threadSleep( 2000, 0 );
    CuAssertIntEquals( tc, MPE_SUCCESS, media_decoder_stopVideo(decoderId) );

    // Assert 4. Set source > max value, then get source. Set should return
    // MPE_EINVAL, but get source should return MPE_SUCCESS.
    rect.x = x = 0;
    rect.y = y = 0;
    rect.width = width = 1921;
    rect.height = height = 1081;

    CuAssertIntEquals( tc,
            MPE_ERROR_MEDIA_OS,
            media_setSourceBounds( &rect ) );
    CuAssertIntEquals( tc, MPE_SUCCESS, media_getSourceBounds(&rect) );

    // Play video
    media_decoder_playVideo( decoderId, decoderQueue, pDecoderStatus );
    test_threadSleep( 2000, 0 );
    CuAssertIntEquals( tc, MPE_SUCCESS, media_decoder_stopVideo(decoderId) );

    // Assert 5. Set source to NULL then get, set should MPE_EINVAL, but get 
    // should work find.
    CuAssertIntEquals( tc, MPE_EINVAL, media_setSourceBounds( NULL ) );
    CuAssertIntEquals( tc, MPE_SUCCESS, media_getSourceBounds(&rect) );

    // Play video
    media_decoder_playVideo( decoderId, decoderQueue, pDecoderStatus );
    test_threadSleep( 2000, 0 );
    CuAssertIntEquals( tc, MPE_SUCCESS, media_decoder_stopVideo(decoderId) );
#endif

} // end test_video_three(CuTest*)

/**
 * This will test media_setSourceBounds().
 *
 * @assert 1. Set source to HDTV 1920x1080, should return MPE_SUCCESS.
 * @assert 2. Set source to 100x32, should return MPE_SUCCESS.
 * @assert 3. Set source to 0x0, should return MPE_SUCCESS.
 * @assert 4. Set source with a NULL pointer, should return MPE_EINVAL.
 * @assert 5. Set source check play video works, return MPE_SUCCESS.
 */
static void test_video_four(CuTest* tc)
{
#if 0
    // Tune and the play video.
    mpe_MediaRectangle rect;
    mpe_MediaAsyncTunerStatus* pTunerStatus = MediaTest_GetTunerStatus();
    mpe_MediaDecoderStatus* pDecoderStatus = MediaTest_GetDecoderStatus();
    mpe_EventQueue tunerQueue = MediaTest_GetTunerQueue();
    mpe_EventQueue decoderQueue = MediaTest_GetDecoderQueue();
    uint32_t decoderId = 0;
    uint32_t tunerId = 1;
    uint32_t sourceId = g_ciDEFAULT_SOURCE;

    // Assert 1. Set source to HDTV 1920x1080, check for MPE_SUCCESS.
    rect.x = 0;
    rect.y = 0;
#ifdef WIN32
    rect.width = 640;
    rect.height = 480;
#else
    rect.width = 1920;
    rect.height = 1080;
#endif // WIN32
    CuAssertIntEquals( tc, MPE_SUCCESS, media_setSourceBounds( &rect ) );

    // Assert 2. Set source 100x320, should return MPE_SUCCESS.
    rect.x = 0;
    rect.y = 0;
    rect.width = 100;
    rect.height = 32;

    CuAssertIntEquals( tc, MPE_SUCCESS, media_setSourceBounds( &rect ) );

    // Assert 3. Set source to 0x0, should return MPE_SUCCESS.
    rect.x = 0;
    rect.y = 0;
    rect.width = 0;
    rect.height = 0;

    CuAssertIntEquals( tc, MPE_SUCCESS, media_setSourceBounds( &rect ) );

    // Assert 4. Set source with a NULL pointer, should return MPE_EINVAL.
    CuAssertIntEquals( tc, MPE_EINVAL, media_setSourceBounds( NULL ) );

    // Assert 5. Source to 320x160 and see if you can play video, check return.
    rect.x = 0;
    rect.y = 0;
    rect.width = 320;
    rect.height = 160;

    CuAssertIntEquals( tc, MPE_SUCCESS, media_setSourceBounds( &rect ) );

    media_tuner_selectServiceUsingSourceId( tunerId,
            sourceId,
            tunerQueue,
            pTunerStatus );

    CuAssertIntEquals( tc, MPE_SUCCESS, condGet( g_tuningCond ) );
    media_decoder_playVideo( decoderId, decoderQueue, pDecoderStatus );
#endif
} // end test_video_four(CuTest*)

/**
 * This will test media_setSourceBounds() and some artifacts that I was seeing
 * when croping the video to less then 1/2 the full screen resolution (see
 * tests test_video_one, test_video_two, and test_video_three).
 *
 * Seems to be a hardware problem that causes a transparent block over the
 * video.
 *
 * @assert 1. Set source to full screen 640x480
 * @assert 2. Set source to more then 1/2 screen 400x400
 * @assert 3. Set source back to full screen 640x480
 */
static void test_video_five(CuTest* tc)
{
#if 0
    mpe_MediaRectangle rect;
    mpe_MediaAsyncTunerStatus* pTunerStatus = MediaTest_GetTunerStatus();
    mpe_MediaDecoderStatus* pDecoderStatus = MediaTest_GetDecoderStatus();
    mpe_EventQueue tunerQueue = MediaTest_GetTunerQueue();
    mpe_EventQueue decoderQueue = MediaTest_GetDecoderQueue();
    uint32_t decoderId = 0;
    uint32_t tunerId = 1;
    uint32_t sourceId = g_ciDEFAULT_SOURCE;

    /* Tune */
    media_tuner_selectServiceUsingSourceId( tunerId,
            sourceId,
            tunerQueue,
            pTunerStatus );

    CuAssertIntEquals( tc, MPE_SUCCESS, condGet( g_tuningCond ) );
    CuAssertIntEquals( tc, MPE_SUCCESS, pTunerStatus->error );

    /* Assert 1. Set source to 640x480, check for MPE_SUCCESS. */
    rect.x = 0;
    rect.y = 0;
    rect.width = 640;
    rect.height = 480;

    CuAssertIntEquals( tc, MPE_SUCCESS, media_setSourceBounds( &rect ) );
    media_decoder_playVideo( decoderId, decoderQueue, pDecoderStatus );
    test_threadSleep( 3000, 0 );

    /* Assert 2. Set source 100x320, should return MPE_SUCCESS. */
    rect.x = 0;
    rect.y = 0;
    rect.width = 400;
    rect.height = 400;

    CuAssertIntEquals( tc, MPE_SUCCESS, media_setSourceBounds( &rect ) );
    test_threadSleep( 3000, 0 );

    /* Assert 3. Set source to 0x0, should return MPE_SUCCESS. */
    rect.x = 0;
    rect.y = 0;
    rect.width = 640;
    rect.height = 480;

    CuAssertIntEquals( tc, MPE_SUCCESS, media_setSourceBounds( &rect ) );
    test_threadSleep( 3000, 0 );
#endif
} /* end test_video_five(CuTest*) */

/**
 * Will setup the test suite for the video tests.
 */
CuSuite* getTestSuite_video(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, test_video_one);
    SUITE_ADD_TEST(suite, test_video_two);
    SUITE_ADD_TEST(suite, test_video_three);
    SUITE_ADD_TEST(suite, test_video_four);
    SUITE_ADD_TEST(suite, test_video_five);
    return suite;
} // getTestSuite_video()
