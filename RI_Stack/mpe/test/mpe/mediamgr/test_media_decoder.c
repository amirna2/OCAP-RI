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

#define IS_INTERACTIVE 1

#include <test_media.h>
#include <test_disp.h>
#include <mpeos_mem.h>

/* 
 * Private functions to test_media_decoder.cpp.
 */
static void test_decoder_one(CuTest* tc);
static void test_decoder_two(CuTest* tc);
static void test_decoder_three(CuTest* tc);
static void test_decoder_four(CuTest* tc);
CuSuite* getTestSuite_decoder(void);

static void test_mediaDecodeSuccess(CuTest*);
static void test_mediaDecodeFailure(CuTest*);

static void test_mediaFreeze(CuTest*);
static void test_mediaPause(CuTest*);

static void test_mediaBlockPresentationSuccess(CuTest* tc);
static void test_mediaBlockPresentationFailure(CuTest* tc);
static void test_mediaBlockPresentationMisc(CuTest* tc);

//static void test_mediaStop(CuTest*);
//static void test_mediaResume(CuTest*);

/**
 * Global variables for decoder testing
 */
static uint32_t numScreens;
static uint32_t numVideoDevices = 0;
static mpe_DispDeviceType deviceType = MPE_DISPLAY_VIDEO_DEVICE;
static mpe_DispScreen* screens;
static mpe_DispDevice* videoDevices;
static char errorBuffer[256];
static mpe_MediaPID defaultPids[2] =
{
{ TEST_PID_VIDEO_TYPE_DIGITAL_1, TEST_PID_VIDEO_DIGITAL_1 },
{ TEST_PID_AUDIO_TYPE_DIGITAL_1, TEST_PID_AUDIO_DIGITAL_1 } };
static mpe_MediaDecodeRequestParams defaultDecodeRequest =
{ TEST_DEFAULT_TUNER, // tunerId
        0, // videoDevice to be filled in later
#ifdef POWERTV
        sizeof(defaultPids) /
        sizeof(mpe_MediaPID), // number Pids
        defaultPids, // pidArray
        0 // pcrPid
#elif WIN32
        0, // no pids on the simulator for now
        NULL, // no pids on the simulator for now
        0 // pcrPid
#endif
        //FALSE                   // blocked or not blocked
        };

/****************************************************************************
 *
 *  test_mediaDecodeSuccess()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_mediaDecode" function 
 *
 * \api mpeos_mediaDecode()
 *
 * \strategy Call the "mpeos_mediaDecode()" function and checks for reasonable
 * return values.
 *
 * \assets none
 *
 */
static void test_mediaDecodeSuccess(CuTest* tc)
{
    mpe_Error error = 0;
    mpe_MediaDecodeSession decodeSession;

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "---------------------------------------------\n");
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "-- Starting test_mediaDecodeSuccess...\n");

    if (test_mediaDecodeSetupVideoDevices() != MPE_SUCCESS)
    {
        CuFail(tc, errorBuffer);
        return;
    }

    // Do a successful tune
    error = test_mediaTunerDoSuccessfulTune(errorBuffer);
    if (error != MPE_SUCCESS)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "--   Tune did not succeed! Aborting test!\n");
        goto test_failure;
    }

    // Send the decode request
    error = test_mediaDecodeDoSuccessfulDecode(errorBuffer, NULL,
            &decodeSession);
    if (error != MPE_SUCCESS)
    {
        sprintf(errorBuffer, "Decode request failed with error %s",
                decodeError(error));
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "--   Decode request failed! Aborting test!\n");
        goto test_failure;
    }

    // Let the decode results be visible / audible to the end user
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Sleeping for decode display...\n");
    threadSleep(DECODE_PERIOD_LONG, 0);

    // shut down the decode
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Sending decoder stop request...\n");
    error = mediaStop(decodeSession);
    if (error != MPE_SUCCESS)
    {
        sprintf(errorBuffer, "Called to mediaStop failed, error = %s",
                decodeError(error));
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "--   Called to mediaStop failed! Aborting test!\n");
        goto test_failure;
    }
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Decoder successfully stopped!\n");

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--   Calling test_mediaDecodeTeardownVideoDevices()...\n");
    // Free up the memory used by the decoding structures
    if (test_mediaDecodeTeardownVideoDevices() != MPE_SUCCESS)
    {
        CuFail(tc, errorBuffer);
        return;
    }

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--   Call to test_mediaDecodeTeardownVideoDevices() returned success!\n");

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "-- Finished test_mediaDecodeSuccess!\n");
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "-------------------------------------------\n\n");

    return;

    test_failure: test_mediaDecodeTeardownVideoDevices();
    CuFail(tc, errorBuffer);
    return;
}

static void test_mediaDecodeFailure(CuTest* tc)
{
    mpe_Error error = 0;
    mpe_MediaDecodeRequestParams decodeRequest;
    mpe_EventQueue decoderQueue = MediaTest_GetDecoderQueue();
    mpe_MediaDecodeSession decodeSession;

    mpe_MediaPID pids[2] =
    {
    { TEST_PID_VIDEO_TYPE_DIGITAL_1, TEST_PID_VIDEO_DIGITAL_1 },
    { TEST_PID_AUDIO_TYPE_DIGITAL_1, TEST_PID_AUDIO_DIGITAL_1 } };

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "---------------------------------------------\n");
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "-- Starting test_mediaDecodeFailure...\n");

    // Allocate the memory for our decoding structures and queues
    if (test_mediaDecodeSetupVideoDevices() != MPE_SUCCESS)
    {
        CuFail(tc, errorBuffer);
        return;
    }

    // Fill this in since it is passed to future calls to mediaDecode
    decodeRequest.tunerId = TEST_DEFAULT_TUNER;
    decodeRequest.videoDevice = videoDevices[0];
    decodeRequest.numPids = 2;
    decodeRequest.pids = pids;
    //decodeRequest.blocked = FALSE;

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Tuning...\n");
    // Do a successful tune
    error = test_mediaTunerDoSuccessfulTune(errorBuffer);
    if (error != MPE_SUCCESS)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "--   Tune did not succeed! Aborting test!\n");
        goto test_failure;
    }

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Tune complete!\n");

    // Testing with NULL as the decode request
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--   Testing decode request using NULL decodeRequest struct...\n");
    error = mediaDecode(NULL, decoderQueue, NULL, &decodeSession);
    if (error != MPE_EINVAL)
    {
        sprintf(
                errorBuffer,
                "Decode request should've failed with MPE_EINVAL when passing NULL decodeRequest struct, instead returned %s",
                decodeError(error));
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "--   Decode request test failed! Aborting test!\n");
        goto test_failure;
    }
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Decode request test succeeded!\n");

    // Testing with NULL as the queue
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--   Testing decode request using NULL event queue...\n");
    error = mediaDecode(&decodeRequest, NULL, NULL, &decodeSession);
    if (error != MPE_EINVAL)
    {
        sprintf(
                errorBuffer,
                "Decode request should've failed with MPE_EINVAL when passing NULL event queue, instead returned %s",
                decodeError(error));
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "--   Decode request test failed! Aborting test!\n");
        goto test_failure;
    }
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Decode request test succeeded!\n");

    // Testing with a bogus tunerId
    decodeRequest.tunerId = -1;
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--   Testing decode request using invalid tunerId...\n");
    error = mediaDecode(&decodeRequest, decoderQueue, NULL, &decodeSession);
    if (error != MPE_ERROR_MEDIA_INVALID_ID)
    {
        sprintf(
                errorBuffer,
                "Decode request should've failed with MPE_EINVAL when using invalid tunerId, instead returned %s",
                decodeError(error));
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "--   Decode request test failed! Aborting test!\n");
        goto test_failure;
    }
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Decode request test succeeded!\n");

    // restore a valid tunerId
    decodeRequest.tunerId = TEST_DEFAULT_TUNER;

    // Testing with a NULL display device
#if 0
    decodeRequest.videoDevice = NULL;

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Testing decode request using invalid display device...\n");
    error = mediaDecode(&decodeRequest, decoderQueue, NULL, decodeSession);
    if(error != MPE_EINVAL)
    {
        sprintf(errorBuffer, "Decode request should've failed with MPE_EINVAL when using bad display device, instead returned %s",
                decodeError(error));
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Decode request test failed! Aborting test!\n");
        goto test_failure;
    }
#endif	

    // Testing with a NULL PID array
    decodeRequest.pids = NULL;
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--   Testing decode request using NULL PIDs...\n");
    error = mediaDecode(&decodeRequest, decoderQueue, NULL, &decodeSession);
    if (error != MPE_EINVAL)
    {
        sprintf(
                errorBuffer,
                "Decode request should've failed with MPE_EINVAL when using bad PIDs, instead returned %s",
                decodeError(error));
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "--   Decode request test failed! Aborting test!\n");
        goto test_failure;
    }
    // restore the PID array
    decodeRequest.pids = pids;

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--   Calling test_mediaDecodeTeardownVideoDevices()...\n");
    // Free up the memory used by the decoding structures
    if (test_mediaDecodeTeardownVideoDevices() != MPE_SUCCESS)
    {
        CuFail(tc, errorBuffer);
        return;
    }

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--   Call to test_mediaDecodeTeardownVideoDevices() returned success!\n");

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "-- Finished test_mediaDecodeFailure!\n");
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "-------------------------------------------\n\n");

    return;

    test_failure: test_mediaDecodeTeardownVideoDevices();
    CuFail(tc, errorBuffer);
    return;
}

static void test_mediaFreeze(CuTest* tc)
{

}

static void test_mediaPause(CuTest* tc)
{

}

/**
 * This test will test the basic useage of starting and stopping a decoder
 * works.
 * 
 * @assert 1. All parameters correct should return MPE_SUCCESS.
 * @assert 2. Valid call to stop video should return MPE_SUCCESS.
 * @assert 3. Correct parameters yeilding MPE_SUCCESS should show TV.
 * @assert 4. Pausing should pause and return MPE_SUCCESS.
 * @assert 5. Invalid call to stop video should return MPE_EINVAL. (wrong ID)
 * @assert 6. An invalid decoder ID should return MPE_EINVAL.
 * @assert 7. An invalid queue ID should return MPE_EINVAL. (???)
 * @assert 8. NULL for status should not hang, and just return.
 * @assert 9. Pausing an invalid decoder ID should do nothing, return false.
 */
static void test_decoder_one(CuTest* tc)
{
#if 0
    mpe_EventQueue tunerQueue;
    mpe_EventQueue decoderQueue;
    uint32_t decoderId = 0;
    uint32_t tunerId = 1;
    uint32_t sourceId = g_ciDEFAULT_SOURCE;

    mpe_MediaDecodeRequestParams reqParamsDec;
    mpe_MediaTuneRequestParams reqParams;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Entering 'test_decoder_one()' x x x\n");

    tunerQueue = MediaTest_GetTunerQueue();
    decoderQueue = MediaTest_GetDecoderQueue();

    reqParams.tunerId = tunerId;
    reqParams.tuneParams.tuneType = MPE_MEDIA_TUNE_BY_SOURCEID;
    reqParams.tuneParams.sourceId = sourceId;
    // Assert 1. Valid player.
    // Tune to tunerId

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  Calling 'mediaTune()'\n");

    mediaTune( &reqParams,
            tunerQueue, NULL);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  Back from 'mediaTune()'\n");

    //  CuAssertIntEquals( tc, MPE_SUCCESS, condGet( g_tuningCond ) );

    decoderId = 0;
    reqParamsDec.tunerId = tunerId;
    reqParamsDec.numPids = 2;
    reqParamsDec.videoDevice = NULL;

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  Calling 'mediaDecode()'\n");

    mediaDecode( &reqParamsDec, decoderQueue, NULL);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  Back from 'mediaDecode()'\n");

    test_threadSleep( 2000, 0 ); // Sleep 2 sec, ifdef UTILS_INTERACTIVE.

    // Assert 2. Valid call to stop
    decoderId = 0;
    CuAssertIntEquals( tc, MPE_SUCCESS, mediaStop(NULL) );

    // Assert 3. Valid player.
    decoderId = 0;
    mediaDecode( &reqParamsDec, decoderQueue, NULL);

    // Assert 4. Pausing should work.
    test_threadSleep( 2000, 0 ); // Sleep so that I can see it stop. 2 sec
    CuAssertIntEquals( tc, MPE_SUCCESS, mediaPause(NULL) );
    test_threadSleep( 2000, 0 ); // Pause for 2 sec

    CuAssertIntEquals( tc, MPE_SUCCESS, mediaStop(NULL) );

    // DEBUG: Invalid tests are not supported, and may never be.
    // Assert 5. Invalid decoderId call to mpe_deocder_stopVideo.
    // Setup correct player.
    decoderId = 0;
    pDecoderStatus->error = MPE_SUCCESS;
    media_decoder_playVideo( decoderId, decoderQueue, pDecoderStatus );

    decoderId += 1;
    CuAssertIntEquals( tc, MPE_ERROR_MEDIA_OS,
            media_decoder_stopVideo( decoderId ) );

    // Assert 6. Invalid decoder
    decoderId = -1;
    pDecoderStatus->error = MPE_SUCCESS;
    media_decoder_playVideo( decoderId, decoderQueue, pDecoderStatus );

    // Assert 7. Invalid queue ID should do nothing
    decoderId = 0;
    pDecoderStatus->error = MPE_SUCCESS;
    media_decoder_playVideo( decoderId, NULL, pDecoderStatus );

    // Assert 8. NULL for status
    decoderId = 0;
    pDecoderStatus->error = MPE_SUCCESS;
    media_decoder_playVideo( decoderId, decoderQueue, NULL );

    // Assert 9. Pause an invalid decoder ID should do nothing, are return false.
    media_decoder_playVideo( decoderId, decoderQueue, pDecoderStatus );
    CuAssertIntEquals( tc, MPE_ERROR_MEDIA_OS,
            media_decoder_pauseVideo( decoderId - 1 ) );
    test_threadSleep( 2000, 0 ); // Pause for 2 sec
#endif // #if 0
} // end test_decoder_one(CuTest*)


/**
 * Volume tests. Will test mute, unmute, set volume, get volume, and toggle
 * audio.
 *
 * @assert 1. Valid player, and mute the sound, should return TRUE.
 * @assert 2. Valid player, and un mute the sound, should return TRUE.
 * @assert 3. Stop player, try to mute it, should return TRUE.
 * @assert 4. Stop player, try to un mute it, should return TRUE.
 * @assert 5. Invalid player, mute it, should return FALSE.
 * @assert 6. Invalid player, un mute it, should return FALSE.
 */
static void test_decoder_two(CuTest* tc)
{
#if 0 
    mpe_EventQueue tunerQueue;
    mpe_EventQueue decoderQueue;
    uint32_t decoderId;
    uint32_t tunerId;
    uint32_t sourceId;
    mpe_MediaDecodeRequestParams reqParamsDec;
    mpe_MediaTuneRequestParams reqParams;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", "Entering 'test_decoder_two()'");

    tunerQueue = MediaTest_GetTunerQueue();
    decoderQueue = MediaTest_GetDecoderQueue();
    decoderId = 0;
    tunerId = 1;
    sourceId = g_ciDEFAULT_SOURCE;

    reqParams.tunerId = tunerId;
    reqParams.tuneParams.tuneType = MPE_MEDIA_TUNE_BY_SOURCEID;
    reqParams.tuneParams.sourceId = sourceId;
    // Tune to tunerId and play video to be used in tests.


    mediaTune( &reqParams,
            tunerQueue, NULL);

    CuAssertIntEquals( tc, MPE_SUCCESS, condGet( g_tuningCond ) );
    reqParamsDec.tunerId = tunerId;
    reqParamsDec.numPids = 2;
    reqParamsDec.videoDevice = NULL;

    mediaDecode( &reqParamsDec, decoderQueue, NULL);

    test_threadSleep( 2000, 0 ); // Pause for 2 sec

    /* commenting out until MuteState is returned to media APIs */
    // Assert 1. Valid player, try to mute.
    CuAssertIntEquals( tc, MPE_SUCCESS, mediaSetMuteState( TRUE ) );
    test_threadSleep( 2000, 0 ); // Pause for 2 sec

    // Assert 2. Valid player, try to un mute.
    CuAssertIntEquals( tc, MPE_SUCCESS, mediaSetMuteState( FALSE ) );
    test_threadSleep( 2000, 0 ); // Pause for 2 sec


    // Stop player for Assert tests 3 and 4.
    CuAssertIntEquals( tc, MPE_SUCCESS, mediaStop(NULL) );

    /* commenting out until MuteState is returned to media APIs */
    // Assert 3. Stop player, try to mute.
    CuAssertIntEquals( tc, MPE_SUCCESS, mediaSetMuteState( TRUE ) );

    // Assert 4. Stop player, try to un mute.
    CuAssertIntEquals( tc, MPE_SUCCESS, mediaSetMuteState( FALSE ) );

    // DEBUG
    // Assert 5. Invalid player, try to mute.
    decoderId = -100; // invalid decoder ID.
    media_decoder_playVideo( decoderId, decoderQueue, pDecoderStatus );
    CuAssertIntEquals( tc, MPE_EINVAL, pDecoderStatus->error );
    CuAssertIntEquals( tc,
            MPE_ERROR_MEDIA_OS,
            media_decoder_mute( decoderId ) );

    // Assert 6. Invalid player, try to un mute.
    decoderId = -100; // invalid decoder ID.
    media_decoder_playVideo( decoderId, decoderQueue, pDecoderStatus );
    CuAssertIntEquals( tc, MPE_EINVAL, pDecoderStatus->error );
    CuAssertIntEquals( tc,
            MPE_ERROR_MEDIA_OS,
            media_decoder_unMute( decoderId ) );
#endif // #if 0 // DEBUG
} // end test_decoder_two(CuTest*)

/**
 * @assert 1. Valid player, set volume, should return TRUE.
 * @assert 2. Valid player, get volume, should return TRUE.
 * @assert 3. Stop player, get volume, should return TRUE.
 * @assert 4. Stop player, set volume, should return TRUE.
 * @assert 5. Invalid player, set volume, should return FALSE.
 * @assert 6. Invalid player, get volume, should return FALSE.
 */
static void test_decoder_three(CuTest* tc)
{
#if 0
    mpe_EventQueue tunerQueue;
    mpe_EventQueue decoderQueue;
    uint32_t decoderId;
    uint32_t tunerId;
    uint32_t sourceId;
    mpe_MediaDecodeRequestParams reqParamsDec;
    mpe_MediaTuneRequestParams reqParams;

    /* TODO */
    uint32_t setVolume;
    int32_t getVolume;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", "Entering 'test_decoder_three()'");

    tunerQueue = MediaTest_GetTunerQueue();
    decoderQueue = MediaTest_GetDecoderQueue();
    decoderId = 0;
    tunerId = 1;
    sourceId = g_ciDEFAULT_SOURCE;

    reqParams.tunerId = tunerId;
    reqParams.tuneParams.tuneType = MPE_MEDIA_TUNE_BY_SOURCEID;
    reqParams.tuneParams.sourceId = sourceId;

    // Tune to tunerId and play video to be used in tests.

    mediaTune( &reqParams,
            tunerQueue, NULL);

    CuAssertIntEquals( tc, MPE_SUCCESS, condGet( g_tuningCond ) );
    reqParamsDec.tunerId = tunerId;
    reqParamsDec.numPids = 2;
    reqParamsDec.videoDevice = NULL;

    mediaDecode( &reqParamsDec, decoderQueue, NULL);

    test_threadSleep( 2000, 0 ); // Pause for 2 sec

    /* TODO: commenting out until the mediaXXXVolume APIs return */
    // Assert 1. Valid player, set volume
    setVolume = 80; // Decrease max sound (100) by 20%.
    CuAssertIntEquals( tc,
            MPE_SUCCESS,
            mediaSetVolume(setVolume ) );

    test_threadSleep( 2000, 0 ); // Pause for 2 sec

    // Assert 2. Valid player, get volume
    getVolume = 0;
    CuAssertIntEquals( tc,
            MPE_SUCCESS,
            mediaGetVolume(&getVolume) );

    CuAssertIntEquals( tc, setVolume, getVolume );

    // Stop player for Assert tests 3 and 4.
    CuAssertIntEquals( tc,
            MPE_SUCCESS,
            mediaStop( NULL ) );

    /* TODO: commenting out until the mediaXXXVolume APIs return */
    // Assert 3. Stop the player, and then try to set the volume.
    setVolume = 80; // Decrease the max sound 20%.
    CuAssertIntEquals( tc,
            MPE_SUCCESS,
            mediaSetVolume(setVolume ) );

    // Assert 4. Stop the player, and then try to get the volume.
    CuAssertIntEquals( tc,
            MPE_SUCCESS,
            mediaGetVolume(&getVolume) );

    CuAssertIntEquals( tc, setVolume, getVolume );

    // TODO
    // Assert 5. Invalid player, and then try to set the volume.
    setVolume = 80; // Decrease the max sound 20%.
    decoderId = -100; // invalid decoder ID.
    media_decoder_playVideo( decoderId, queueId, pDecoderStatus );
    CuAssertIntEquals( tc, MPE_ERROR_MEDIA_OS, pDecoderStatus->error );
    CuAssertIntEquals( tc,
            MPE_SUCCESS,
            media_decoder_setVolume(decoderId,setVolume) );

    // Assert 6. Invalid player, and then try to get the volume.
    decoderId = -100; // invalid decoder ID.
    media_decoder_playVideo( decoderId, queueId, pDecoderStatus );
    CuAssertIntEquals( tc, MPE_EINVAL, pDecoderStatus->error );
    CuAssertIntEquals( tc,
            MPE_SUCCESS,
            media_decoder_getVolume(decoderId,&getVolume) );

    CuAssertIntEquals( tc, 100, getVolume ); // Unknown if this is correct.
#endif // #if 0 // DEBUG
} // end test_decoder_three(CuTest*)

/**
 * @assert 1. A valid player and valid prog ID, toggle should return TRUE.
 * @assert 2. Invalid prog ID and valid player, toggle should return FALSE.
 * @assert 3. Invalid player and valid prog ID, toggle should return FALSE.
 * @assert 4. Invalid prog ID and invalid player, toggle should return FALSE.
 */
static void test_decoder_four(CuTest* tc)
{
#if 0
    mpe_EventQueue tunerQueue = MediaTest_GetTunerQueue();
    mpe_EventQueue decoderQueue = MediaTest_GetDecoderQueue();
    uint32_t tunerId = 1;
    uint32_t sourceId = g_ciDEFAULT_SOURCE;
    mpe_MediaDecodeRequestParams reqParamsDec;
    mpe_MediaTuneRequestParams reqParams;

    // NOTE: As of 8/27/2003 media_decoder_toggleAudio() not supported.
    // media_toggleAudio is supported by PowerTV, but not the simulator. 10/13/03
    // Assert 1. valid player and prog ID, toggle should return true.
    /* TODO */
    uint32_t progId = 1;
    uint32_t decoderId = 0;
    uint32_t setVolume;
    int32_t getVolume;
#ifdef WIN32
    uint32_t expected = MPE_ERROR_MEDIA_API_NOT_SUPPORTED;
#else
    uint32_t expected = MPE_SUCCESS;
#endif

    reqParams.tunerId = tunerId;
    reqParams.tuneParams.tuneType = MPE_MEDIA_TUNE_BY_SOURCEID;
    reqParams.tuneParams.sourceId = sourceId;
    // Tune to tunerId and play video to be used in tests.
    mediaTune( &reqParams,
            tunerQueue, NULL);

    CuAssertIntEquals( tc, MPE_SUCCESS, condGet( g_tuningCond ) );
    reqParamsDec.tunerId = tunerId;
    reqParamsDec.numPids = 2;
    reqParamsDec.videoDevice = NULL;

    mediaDecode( &reqParamsDec, decoderQueue, NULL);

    test_threadSleep( 2000, 0 ); // Pause for 2 sec

    /*
     CuAssertIntEquals( tc,
     expected,
     media_decoder_toggleAudio( decoderId, progId ) );

     test_threadSleep( 2000, 0 ); // Pause for 2 sec

     progId = 9;
     CuAssertIntEquals( tc,
     expected,
     media_decoder_toggleAudio( decoderId, progId ) );

     test_threadSleep( 2000, 0 ); // Pause for 2 sec
     */
    // Invalid commands are not supported. Only NULL
    // Assert 2. Invalid prog ID, valid player should return FALSE.
    progId = -1;
    CuAssertIntEquals( tc,
            MPE_ERROR_MEDIA_OS,
            media_decoder_toggleAudio(decoderId,progId) );

    test_threadSleep( 2000, 0 ); // Pause for 2 sec

    // Assert 3. Invalid player and valid prog ID, should return FALSE.
    // Create a player with invalid decoder.
    decoderId = -1;
    media_decoder_playVideo( decoderId, g_decoderQueue, pDecoderStatus );

    // There is no player to toggle audio (not sure what should happen)?
    progId = 1;
    CuAssertIntEquals( tc,
            MPE_ERROR_MEDIA_OS,
            media_decoder_toggleAudio(decoderId,progId) );

    // Assert 4. Invalid prog ID and invalid player, should return FALSE.
    progId = -1;
    CuAssertIntEquals( tc,
            MPE_ERROR_MEDIA_OS,
            media_decoder_toggleAudio(decoderId,progId) );
#endif // #if 0
} // end test_decoder_four(CuTest*)


/****************************************************************************
 *
 *  test_mediaBlockPresentationSuccess()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpe_mediaBlockPresentation" function 
 *
 * \api mpe_mediaBlockPresentationSuccess()
 *
 * \strategy 
 *  Test valid cases for usage of mpe_mediaBlockPresentation
 *  
 *  Call mpe_mediaBlockPresentation after a successful tune and decode.
 *  Perform the following for each video device in the system
 *      1 - Tune
 *      2 - Decode
 *      3 - Delay so presentation can be viewed
 *      4 - Loop for 3 iterations to block / unblock the presentation with 
 *          a delay in between calls so output can be validated by end user
 * \assets none
 *
 */
static void test_mediaBlockPresentationSuccess(CuTest* tc)
{
    mpe_Error error = 0;
    mpe_MediaDecodeSession decodeSession;
    uint32_t i, devs;
    mpe_MediaDecodeRequestParams decodeRequest = defaultDecodeRequest;

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "---------------------------------------------\n");
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "-- Starting test_mediaBlockPresentationSuccess...\n");

    if (test_mediaDecodeSetupVideoDevices() != MPE_SUCCESS)
    {
        CuFail(tc, errorBuffer);
        return;
    }

    for (devs = 0; devs < numVideoDevices; devs++)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "\n--  TESTING VIDEO DEVICE %d %x\n\n", devs,
                videoDevices[devs]);

        // Do a successful tune
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Tuning...\n");
        error = test_mediaTunerDoSuccessfulTune(errorBuffer);
        if (error != MPE_SUCCESS)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "--   Tune did not succeed! Aborting test!\n");
            goto test_failure;
        }

        decodeRequest.videoDevice = videoDevices[devs];
        // Send the decode request
        error = test_mediaDecodeDoSuccessfulDecode(errorBuffer, &decodeRequest,
                &decodeSession);
        if (error != MPE_SUCCESS)
        {
            sprintf(errorBuffer, "Decode request failed with error %s",
                    decodeError(error));
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "--   Decode request failed! Aborting test!\n");
            goto test_failure;
        }

        // now loop for a couple of iterations to test block / unblock of media
        for (i = 0; i < MAX_BLOCK_UNBLOCK_TRIES; i++)
        {
            TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
                    "--    Looping for block/unblock (iteration=%d)\n", i);

            // Let the decode results be visible / audible to the end user
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "--   Sleeping for decode display (Unblocked)...\n");
            threadSleep(DECODE_PERIOD_SHORT, 0);

            //            error = mediaBlockPresentation( decodeSession, TRUE );
            if (error != MPE_SUCCESS)
            {
                TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                        "--   mediaBlockPresentation failed %s\n", decodeError(
                                error));
            }

            // Let the decode results be blocked to the end user
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "--   Sleeping for decode display (Blocked)...\n");
            threadSleep(DECODE_PERIOD_SHORT, 0);

            //  error = mediaBlockPresentation( decodeSession, FALSE );
            if (error != MPE_SUCCESS)
            {
                TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                        "--   mediaBlockPresentation failed %s\n", decodeError(
                                error));
            }
        }

        // shut down the decode
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "--   Sending decoder stop request...\n");
        error = mediaStop(decodeSession);
        if (error != MPE_SUCCESS)
        {
            sprintf(errorBuffer, "Called to mediaStop failed, error = %s",
                    decodeError(error));
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "--   Called to mediaStop failed! Aborting test!\n");
            goto test_failure;
        }
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "--   Decoder successfully stopped!\n");
    }

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--   Calling test_mediaDecodeTeardownVideoDevices()...\n");
    // Free up the memory used by the decoding structures
    if (test_mediaDecodeTeardownVideoDevices() != MPE_SUCCESS)
    {
        CuFail(tc, errorBuffer);
        return;
    }

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--   Call to test_mediaDecodeTeardownVideoDevices() returned success!\n");

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "-- Finished test_mediaBlockPresentationSuccess!\n");
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "-------------------------------------------\n\n");

    return;

    test_failure: test_mediaDecodeTeardownVideoDevices();
    CuFail(tc, errorBuffer);
    return;
}

/****************************************************************************
 *
 *  test_mediaBlockPresentationFailure()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpe_mediaBlockPresentation" function 
 *
 * \api mpe_mediaBlockPresentationSuccess()
 *
 * \strategy 
 *  Test invalid usage of mpe_mediaBlockPresentation
 *  
 *  Call mpe_mediaBlockPresentation with invalid parameters and check
 *  that proper error codes are returned
 *      1 - Test null session handle
 *      2 - Test drip feed session handle  
 *          (blockPresentation not supported for drip feed)
 */
static void test_mediaBlockPresentationFailure(CuTest* tc)
{
    mpe_Error error = 0;
    mpe_MediaDecodeSession decodeSession;
    mpe_MediaDripFeedRequestParams dripFeedRequestParams;

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "---------------------------------------------\n");
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "-- Starting test_mediaBlockPresentationFailure...\n");

    if (test_mediaDecodeSetupVideoDevices() != MPE_SUCCESS)
    {
        CuFail(tc, errorBuffer);
        return;
    }

    // check for MPE_EINVAL returned when passed a NULL decode session
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Testing NULL decodeSession...\n");
    //    CuAssertIntEquals( tc, MPE_EINVAL, mediaBlockPresentation(NULL, TRUE) );

    // create a drip feed session and try to block its presentation
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--   Testing mediaBlockPresentation on a dripFeed session...\n");
    dripFeedRequestParams.videoDevice = videoDevices[0];
    error = mediaDripFeedStart(&dripFeedRequestParams,
            MediaTest_GetDecoderQueue(), NULL, &decodeSession);
    CuAssertIntEquals(tc, MPE_SUCCESS, error);

    // pass the drip feed session to test invalid blocking of dripFeed 
    //    CuAssertIntEquals( tc, MPE_EINVAL, mediaBlockPresentation(decodeSession, TRUE) );

    error = mediaDripFeedStop(decodeSession);
    CuAssertIntEquals(tc, MPE_SUCCESS, error);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--   Calling test_mediaDecodeTeardownVideoDevices()...\n");
    // Free up the memory used by the decoding structures
    if (test_mediaDecodeTeardownVideoDevices() != MPE_SUCCESS)
    {
        CuFail(tc, errorBuffer);
        return;
    }

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--   Call to test_mediaDecodeTeardownVideoDevices() returned success!\n");

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "-- Finished test_mediaBlockPresentationFailure!\n");
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "-------------------------------------------\n\n");

    return;
}

/****************************************************************************
 *
 *  test_mediaBlockPresentationMisc()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpe_mediaBlockPresentation" function 
 *
 * \api mpe_mediaBlockPresentationMisc()
 *
 * \strategy 
 *  Test less common cases for usage of mpe_mediaBlockPresentation
 *  
 *  Call mpe_mediaBlockPresentation after a successful tune and decode.
 *  Then stop and restart the decode to check the block status
 *      1 - Tune
 *      2 - Decode with presentation initially blocked
 *      3 - Delay so blocked presentation can be verified
 *      4 - Stop the media decode
 *      5 - Start the media decode with presentation unblocked
 *      6 - Delay so presentation can be viewed
 *      7 - Stop the media decode 
 * \assets none
 *
 */
static void test_mediaBlockPresentationMisc(CuTest* tc)
{
    mpe_Error error = 0;
    mpe_MediaDecodeSession decodeSession;
    mpe_MediaDecodeRequestParams decodeRequest = defaultDecodeRequest;

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "---------------------------------------------\n");
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "-- Starting test_mediaBlockPresentationMisc...\n");

    if (test_mediaDecodeSetupVideoDevices() != MPE_SUCCESS)
    {
        CuFail(tc, errorBuffer);
        return;
    }

    // Do a successful tune
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Tuning...\n");
    error = test_mediaTunerDoSuccessfulTune(errorBuffer);
    if (error != MPE_SUCCESS)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "--   Tune did not succeed! Aborting test!\n");
        goto test_failure;
    }

    // block the presentation initially
    //    decodeRequest.blocked = TRUE;

    // Send the decode request
    error = test_mediaDecodeDoSuccessfulDecode(errorBuffer, &decodeRequest,
            &decodeSession);
    if (error != MPE_SUCCESS)
    {
        sprintf(errorBuffer, "Decode request failed with error %s",
                decodeError(error));
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "--   Decode request failed! Aborting test!\n");
        goto test_failure;
    }

    // Let the decode results be verified by the end user
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--   Sleeping for decode display (Blocked)...\n");
    threadSleep(DECODE_PERIOD_LONG, 0);

    // shut down the decode
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Sending decoder stop request...\n");
    error = mediaStop(decodeSession);
    if (error != MPE_SUCCESS)
    {
        sprintf(errorBuffer, "Called to mediaStop failed, error = %s",
                decodeError(error));
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "--   Called to mediaStop failed! Aborting test!\n");
        goto test_failure;
    }
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Decoder successfully stopped!\n");

    // unblock the presentation initially
    //    decodeRequest.blocked = FALSE;

    // Send the decode request
    error = test_mediaDecodeDoSuccessfulDecode(errorBuffer, &decodeRequest,
            &decodeSession);
    if (error != MPE_SUCCESS)
    {
        sprintf(errorBuffer, "Decode request failed with error %s",
                decodeError(error));
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "--   Decode request failed! Aborting test!\n");
        goto test_failure;
    }

    // Let the decode results be visible / audible to the end user
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--   Sleeping for decode display (Unblocked)...\n");
    threadSleep(DECODE_PERIOD_LONG, 0);

    // shut down the decode
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Sending decoder stop request...\n");
    error = mediaStop(decodeSession);
    if (error != MPE_SUCCESS)
    {
        sprintf(errorBuffer, "Called to mediaStop failed, error = %s",
                decodeError(error));
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "--   Called to mediaStop failed! Aborting test!\n");
        goto test_failure;
    }
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Decoder successfully stopped!\n");

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--   Calling test_mediaDecodeTeardownVideoDevices()...\n");
    // Free up the memory used by the decoding structures
    if (test_mediaDecodeTeardownVideoDevices() != MPE_SUCCESS)
    {
        CuFail(tc, errorBuffer);
        return;
    }

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--   Call to test_mediaDecodeTeardownVideoDevices() returned success!\n");

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "-- Finished test_mediaBlockPresentationMisc!\n");
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "-------------------------------------------\n\n");

    return;

    test_failure: test_mediaDecodeTeardownVideoDevices();
    CuFail(tc, errorBuffer);
    return;
}

CuSuite* getTestSuite_decoder(void)
{
    CuSuite* suite = CuSuiteNew();

    //  SUITE_ADD_TEST(suite, test_decoder_one );
    //  SUITE_ADD_TEST(suite, test_decoder_two );
    //  SUITE_ADD_TEST(suite, test_decoder_three );
    //  SUITE_ADD_TEST(suite, test_decoder_four );

    SUITE_ADD_TEST(suite, test_mediaDecodeSuccess);
    SUITE_ADD_TEST(suite, test_mediaDecodeFailure);
    SUITE_ADD_TEST(suite, test_mediaBlockPresentationSuccess);
    SUITE_ADD_TEST(suite, test_mediaBlockPresentationFailure);
    SUITE_ADD_TEST(suite, test_mediaBlockPresentationMisc);
    //  SUITE_ADD_TEST(suite, test_mediaFreeze);
    //  SUITE_ADD_TEST(suite, test_mediaPause);

    return suite;
}

mpe_Error test_mediaDecodeSetupVideoDevices()
{
    mpe_Error error;

    videoDevices = NULL;
    screens = NULL;

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--     In test_mediaDecodeSetupVideoDevices()...\n");

    ///////////////////////////////////////////////////////////////////////////
    // This block of code gets the number of screens, allocates memory for
    //   those screens, and finally fetches them into the memory.
    ///////////////////////////////////////////////////////////////////////////
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--       Getting the display screen count...\n");
    error = dispGetScreenCount(&numScreens);
    if (error != MPE_SUCCESS)
    {
        sprintf(
                errorBuffer,
                "## test_mediaDecodeSetupVideoDevices Error!!! Error getting display screen count = %s",
                decodeError(error));
        TRACE(
                MPE_LOG_DEBUG,
                MPE_MOD_TEST,
                "--       Error getting display screen count!  Aborting test_mediaDecodeSetupVideoDevices!\n");
        return TEST_SETUP_ERROR;
    }
    if (numScreens <= 0)
    {
        sprintf(
                errorBuffer,
                "## test_mediaDecodeSetupVideoDevices Error!!! No display screens are available");
        TRACE(
                MPE_LOG_DEBUG,
                MPE_MOD_TEST,
                "--       No display screens are available! Aborting test_mediaDecodeSetupVideoDevices!\n");
        return TEST_SETUP_ERROR;
    }
    sprintf(errorBuffer, "--       Number of display screens found = %lu\n",
            numScreens);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, errorBuffer);

    // Allocating memory...
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--       Allocating memory for display screens...\n");
    error = memAllocP(MPE_MEM_TEST, sizeof(mpe_DispScreen) * numScreens,
            (void**) &screens);
    if (error != MPE_SUCCESS)
    {
        sprintf(
                errorBuffer,
                "## test_mediaDecodeSetupVideoDevices Error!!! Unable to allocate memory for display screens = %s",
                decodeError(error));
        TRACE(
                MPE_LOG_DEBUG,
                MPE_MOD_TEST,
                "--       Unable to allocate memory for display screens! Aborting test_mediaDecodeSetupVideoDevices!\n");
        return TEST_SETUP_ERROR;
    }
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--       Memory allocation was successful!\n");

    // Getting the display screens...
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--       Getting the display screens...\n");
    error = dispGetScreens(screens);
    if (error != MPE_SUCCESS)
    {
        sprintf(
                errorBuffer,
                "## test_mediaDecodeSetupVideoDevices Error!!! Unable to get the display screens = %s",
                decodeError(error));
        TRACE(
                MPE_LOG_DEBUG,
                MPE_MOD_TEST,
                "--       Unable to get the display screens! Aborting test_mediaDecodeSetupVideoDevices!\n");
        // Setup failed, clean up the memory
        goto cleanup;
    }
    if (*screens == NULL)
    {
        sprintf(errorBuffer,
                "## test_mediaDecodeSetupVideoDevices Error!!! Display screens pointer is NULL");
        TRACE(
                MPE_LOG_DEBUG,
                MPE_MOD_TEST,
                "--       Display screens pointer is NULL! Aborting test_mediaDecodeSetupVideoDevices!\n");
        // Setup failed, clean up the memory
        goto cleanup;
    }
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--       Display screens successfully acquired!\n");

    ///////////////////////////////////////////////////////////////////////////
    // This block of code gets the number of devices on the first screen,
    //   allocates memory for those devices, and fetches them into the memory.
    ///////////////////////////////////////////////////////////////////////////
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--       Getting the display device count...\n");
    error = dispGetDeviceCount(screens[TEST_DEFAULT_SCREEN], deviceType,
            &numVideoDevices);
    if (error != MPE_SUCCESS)
    {
        sprintf(
                errorBuffer,
                "## test_mediaDecodeSetupVideoDevices Error!!! Error getting display device count = %s",
                decodeError(error));
        TRACE(
                MPE_LOG_DEBUG,
                MPE_MOD_TEST,
                "--       Error getting display device count!  Aborting test_mediaDecodeSetupVideoDevices!\n");
        // Setup failed, clean up the memory
        goto cleanup;
    }
    if (numVideoDevices <= 0)
    {
        sprintf(
                errorBuffer,
                "## test_mediaDecodeSetupVideoDevices Error!!! No display devices are available");
        TRACE(
                MPE_LOG_DEBUG,
                MPE_MOD_TEST,
                "--       No display devices are available! Aborting test_mediaDecodeSetupVideoDevices!\n");
        // Setup failed, clean up the memory
        goto cleanup;
    }
    sprintf(errorBuffer, "--       Number of display devices found = %lu\n",
            numVideoDevices);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, errorBuffer);

    // Allocating memory...
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--       Allocating memory for display devices...\n");
    error = memAllocP(MPE_MEM_TEST, sizeof(mpe_DispDevice) * numVideoDevices,
            (void**) &videoDevices);
    if (error != MPE_SUCCESS)
    {
        sprintf(
                errorBuffer,
                "## test_mediaDecodeSetupVideoDevices Error!!! Unable to allocate memory for display devices = %s",
                decodeError(error));
        TRACE(
                MPE_LOG_DEBUG,
                MPE_MOD_TEST,
                "--       Unable to allocate memory for display devices! Aborting test_mediaDecodeSetupVideoDevices!\n");
        // Setup failed, clean up the memory
        goto cleanup;
    }
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--       Memory allocation was successful!\n");

    // Getting the display devices...
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--       Getting the display devices...\n");
    error = dispGetDevices(screens[TEST_DEFAULT_SCREEN], deviceType,
            videoDevices);
    if (error != MPE_SUCCESS)
    {
        sprintf(
                errorBuffer,
                "## test_mediaDecodeSetupVideoDevices Error!!! Unable to get the display devices = %s",
                decodeError(error));
        TRACE(
                MPE_LOG_DEBUG,
                MPE_MOD_TEST,
                "--       Unable to get the display devices! Aborting test_mediaDecodeSetupVideoDevices!\n");
        // Setup failed, clean up the memory
        goto cleanup;
    }
    if (*videoDevices == NULL)
    {
        sprintf(errorBuffer,
                "## test_mediaDecodeSetupVideoDevices Error!!! Display devices pointer is NULL");
        TRACE(
                MPE_LOG_DEBUG,
                MPE_MOD_TEST,
                "--       Display devices pointer is NULL! Aborting test_mediaDecodeSetupVideoDevices!\n");
        // Setup failed, clean up the memory
        goto cleanup;
    }

    // fill in video device in the decodeRequest structure
    defaultDecodeRequest.videoDevice = videoDevices[0];

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--       Display devices successfully acquired!\n");
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--     Returning from test_mediaDecodeSetupVideoDevices()\n");

    return MPE_SUCCESS;

    cleanup: if (screens != NULL)
        memFreeP(MPE_MEM_TEST, (void*) screens);
    if (videoDevices != NULL)
        memFreeP(MPE_MEM_TEST, (void*) videoDevices);
    return TEST_SETUP_ERROR;
}

mpe_DispDevice test_mediaDecodeGetDefaultVideoDevice()
{
    if (videoDevices == NULL)
        return NULL;

    return videoDevices[0];
}

mpe_Error test_mediaDecodeTeardownVideoDevices()
{
    mpe_Error error;
    mpe_Error result = MPE_SUCCESS;

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--     In test_mediaDecodeTeardownVideoDevices()...\n");

    // Releasing the memory allocated for the screen and display objects
    //   created in the test_mediaDecodeSetupVideoDevices().
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--       Releasing memory allocated for display devices...\n");
    error = memFreeP(MPE_MEM_TEST, (void*) videoDevices);
    if (error != MPE_SUCCESS)
    {
        sprintf(
                errorBuffer,
                "## test_mediaDecodeTeardownVideoDevices Error!!! Unable to free memory used by the display devices = %s",
                decodeError(error));
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "--       Unable to free memory used by the display devices!\n");
        result = TEST_TEARDOWN_ERROR;
    }
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--       Memory allocated for display devices successfully released!\n");

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--       Releasing memory allocated for display screens...\n");
    error = memFreeP(MPE_MEM_TEST, (void*) screens);
    if (error != MPE_SUCCESS)
    {
        sprintf(
                errorBuffer,
                "## test_mediaDecodeTeardownVideoDevices Error!!! Unable to free memory used by the display screens = %s",
                decodeError(error));
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "--       Unable to free memory used by the display screens!\n");
        result = TEST_TEARDOWN_ERROR;
    }
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--       Memory allocated for display screens successfully released!\n");

    videoDevices = NULL;
    screens = NULL;

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--     Returning from test_mediaDecodeTeardownVideoDevices()\n");

    return result;
}

mpe_Error test_mediaDecodeDoSuccessfulDecode(char *errorBuffer,
        mpe_MediaDecodeRequestParams *decodeRequest,
        mpe_MediaDecodeSession *decodeSession)
{
    mpe_Error error = 0;
    mpe_EventQueue decodeQueue;

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "-- Entered test_mediaDecodeDoSuccessfulDecode..\n");

    // grab the decode queue created in MediaTest_Init, this decode queue
    // is monitored by the decode thread
    decodeQueue = MediaTest_GetDecoderQueue();
    if (decodeQueue == NULL)
    {
        sprintf(errorBuffer, "  ERROR: Decode Queue is NULL, cannot tune!");
        return MPE_EINVAL;
    }

    // use default decodeRequest if none was provided
    if (decodeRequest == NULL)
    {
        decodeRequest = &defaultDecodeRequest;
    }

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Sending decode request...\n");
    // Send the decode request
    error = mediaDecode(decodeRequest, decodeQueue, NULL, decodeSession);
    if (error != MPE_SUCCESS)
    {
        sprintf(errorBuffer, "Decode request failed with error %s",
                decodeError(error));
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "--   ERROR: Decode request failed!\n");
        return error;
    }
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--   Decode request successfully sent!\n");

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--   Waiting for MPE_CONTENT_PRESENTING event...\n");
    if (!MediaTest_DecodeSucceeded())
    {
        sprintf(errorBuffer,
                "Decode request failed - MPE_CONTENT_PRESENTING not received");
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "    Decode failed - MPE_CONTENT_PRESENTING not received\n");
        return MPE_EINVAL;
    }
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--   Received MPE_CONTENT_PRESENTING event successfully!\n");

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "-- Leaving test_mediaDecodeDoSuccessfulDecode\n");
    return error;
}
