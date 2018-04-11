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

#include <test_dvr.h>
#include <mpeos_time.h>
#include <inttypes.h>
#define		WAIT_INTERVAL		(2*60*1000)	// 2 minutes in milli sec	
/*
 Each of these test cases run in different contexts, hence 
 cannot use test results from one test case in another.
 As a result, each of the test case(s) is written as a series
 of tests exersizing several DVR TSB APIs. The tests also
 use media APIs for tuning, decoding required for DVR.
 
 @@@@ Important READ ME @@@@: 
 
 The tuning params used in tuning match only the channel map in
 Portland. If these unit tests need to be run any where else, make
 sure the they are updated.

 Still TODO: Negative test cases and other edge case testing.
 */

CuSuite* getTestSuite_TSB(void);
CuSuite* getTestSuite_TSB1(void);
CuSuite* getTestSuite_TSB2(void);
CuSuite* getTestSuite_TSB3(void);
CuSuite* getTestSuite_TSB4(void);

/*
 * Private functions for time shift buffer.
 */

// OLD TESTS
static void test_dvr_tsbStart(CuTest* tc);
static void test_dvr_tsbStop(CuTest* tc);
static void test_dvr_tsbPlay(CuTest* tc);
static void test_dvr_tsbResize(CuTest* tc);
static void test_dvr_tsbGet(CuTest* tc);
static void test_dvr_tsbConvert(CuTest* tc);

// NEW TESTS
static void test_dvrTsbBufferingAndPlayback(CuTest* tc);
static void test_dvrPlaybackBlockPresentationSuccess(CuTest* tc);
static void test_dvrPlaybackBlockPresentationFailure(CuTest* tc);
static void test_dvrPlaybackBlockPresentationMisc(CuTest* tc);

static mpe_DvrTsb buffer = NULL;

//static uint32_t TSBsize = 10*60*(19500*8);	// 10 min at 19500Kbits/sec - in bytes
//static uint32_t newSize = 15*60*(19500*8);	// 15 min at 19500Kbits/sec 	
//static uint32_t bigSize = 6UL*60UL*60UL*(19500UL*8UL);	// 6 hr at 19500Kbits/sec;	
//static uint32_t newSizeRet = 0;

static uint32_t tsbDuration = 0; // in milli-seconds

//static char recordingName[256] = "";
//static mpe_DvrRecording recording;
static int64_t startTime = 0;
//static int64_t endTime = 0;
static float actual_mode = 0;

static uint64_t time1 = 0;
static mpe_TimeMillis ptime;
//static int64_t duration;

static mpe_EventQueue m_dvrQueue1 = NULL;
static void timerThread(void* data);
static mpe_EventQueue m_timerQueue1 = NULL;
static mpe_ThreadId m_timerThreadId1 = 0;
static int32_t m_sharedData1 = 0;

static mpe_EventQueue m_mediaQueue1 = NULL;
static mpe_ThreadId m_mediaThreadId1 = 0;
static void mediaThread(void* data);
static mpe_Bool tuneSuccess = false;
static mpe_Cond dvr_test_cond;
static uint32_t wait_interval1 = 0;
static mpe_MediaDecodeSession session = NULL;

static void tune(uint32_t tunerId, uint32_t freq, uint32_t prog, uint32_t qam,
        mpe_Bool decode);

static mpe_MediaPID pids[2];
static mpe_DvrPlayback playback;
static mpe_MediaRectangle fullSrc =
{ 0, 0, 1, 1 };
static mpe_MediaRectangle fullDest =
{ 0, 0, 1, 1 };
static mpe_MediaRectangle smallDest =
{ 0.125f, 0.125f, 0.5f, 0.5f };

static void test_dvrTsbBufferingAndPlayback(CuTest* tc)
{
    mpe_Error retCode = MPE_SUCCESS;
    char errorBuffer[256];
    int64_t duration = 10 * 60; // 10 min in seconds
    mpe_DvrBitRate bitRate = (19500 * 1000) / 8; // in bytes/sec
    mpe_DvrBuffering tsbSession;
    mpe_MediaDecodeSession decodeSession;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "---------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-- Starting test_dvrTsbBufferingAndPlayback...\n");

    if (MPE_SUCCESS != test_mediaDecodeSetupVideoDevices())
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "Error: Could not setup video devices\n");
        test_mediaDecodeTeardownVideoDevices();
        return;
    }

    if (m_dvrQueue1 == NULL)
    {
        // event queue
        if (MPE_SUCCESS != eventQueueNew(&m_dvrQueue1, "DVR Event Queue"))
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "Error eventQueueNew() Could not create a queue.\n");
            return;
        }
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsb - (Time shift buffer creation)\n");

    // Create a TSB - 10 minutes
    if (buffer == NULL)
    {
        //		CuAssertIntEquals( tc, MPE_SUCCESS, dvrTsbNew( NULL, duration, &buffer ) );
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "dvrTsbNew() returned - buffer 0x%x\n", buffer);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling tune...\n");

    // Do a successful tune
    retCode = test_mediaTunerDoSuccessfulTune(errorBuffer);
    if (retCode != MPE_SUCCESS)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "--   Tune did not succeed! Aborting test!\n");
        return;
    }

    // Send the decode request
    retCode = test_mediaDecodeDoSuccessfulDecode(errorBuffer, NULL,
            &decodeSession);
    if (retCode != MPE_SUCCESS)
    {
        sprintf(errorBuffer, "Decode request failed with error %s",
                decodeError(retCode));
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "--   Decode request failed! Aborting test!\n");
        return;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsb - (Time shift recording start)\n");
    // start time shift buffer recording (give tunerId of 1)
    /*CuAssertIntEquals( tc, MPE_SUCCESS,
     mpe_dvrTsbBufferingStart(   1,              //Tuner ID
     buffer,         //TSBBuffer
     bitRate,        //bitRate
     m_dvrQueue1,    // dvr event queue
     NULL,           //act
     0,              //pidCount    
     NULL,           //pids,
     &tsbSession )); // buffering handle
     */
    // wait for 10 seconds while TSB buffering takes place
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "Sleeping for 10 seconds to allow playback\n");
    test_threadSleep(10000, 0); // Sleep 10 sec

    // Stop the media decode playback
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsb: calling mediaStop()...\n");
    CuAssertIntEquals(tc, MPE_SUCCESS, mediaStop(decodeSession));
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsb: mediaStop() done...\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "\ncalling mediaSetBounds()...\n");
    retCode = mediaSetBounds(test_mediaDecodeGetDefaultVideoDevice(), &fullSrc,
            &smallDest);
    CuAssertIntEquals(tc, MPE_SUCCESS, retCode);

    // do a tsb playback
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "\ntest_dvr_tsb: calling dvrTsbPlayStart()\n");
    //	retCode = dvrTsbPlayStart (buffer, test_mediaDecodeGetDefaultVideoDevice(), NULL, 0, 
    //                              MPE_DVR_POSITIVE_INFINITY, 1.0, m_dvrQueue1, NULL, FALSE, &playback);
    //    CuAssertIntEquals( tc, MPE_SUCCESS, retCode );
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "dvrTsbPlayStart() returned playback: 0x%x\n", playback);

    // Let the dvr playback be visible
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "Sleeping for 5 seconds to allow playback\n");
    test_threadSleep(5000, 0); // Sleep 5 sec

    // Shutdown the dvr playback session
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling dvrPlayBackStop()...\n");
    //	CuAssertIntEquals( tc, MPE_SUCCESS, dvrPlayBackStop (playback) );
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "dvrPlayBackStop()...done\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "\nRestoring bounds with mediaSetBounds()\n");
    retCode = mediaSetBounds(test_mediaDecodeGetDefaultVideoDevice(), &fullSrc,
            &fullDest);
    //   CuAssertIntEquals( tc, MPE_SUCCESS, retCode );

    // Shutdown the tsb buffering session
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling dvrTsbBufferingStop()\n");
    //   CuAssertIntEquals( tc, MPE_SUCCESS, dvrTsbBufferingStop( tsbSession ));
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "dvrTsbStop() returned OK\n");

    // Free up the memory used by the decoding structures
    if (test_mediaDecodeTeardownVideoDevices() != MPE_SUCCESS)
    {
        //		CuFail(tc, errorBuffer);
    }

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-- Finished test_dvrTsbBufferingAndPlayback!\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------\n\n");

} // end test_dvrTsbBufferingAndPlayback (CuTest*)


/****************************************************************************
 *
 *  test_dvrPlaybackBlockPresentationSuccess()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpe_dvrPlaybackBlockPresentation" function 
 *
 * \api mpe_dvrPlaybackBlockPresentationSuccess()
 *
 * \strategy 
 *  Test valid cases for usage of mpe_dvrPlaybackBlockPresentation
 *  
 *  Call mpe_dvrPlaybackBlockPresentation after a successful tune and decode.
 *  Perform the following for the default video device
 *      1 - Create a TSB buffer
 *      2 - Tune
 *      3 - Decode
 *      4 - Start a TSB buffering session
 *      5 - Delay so presentation can be viewed and TSB buffering can complete
 *      6 - Stop TSB buffering
 *      7 - Stop media decode
 *      8 - Playback TSB buffered data
 *      9 - Delay so presentation can be viewed
 *      10 - Loop for 3 iterations to block / unblock the presentation with 
 *          a delay in between calls so output can be validated by end user
 * \assets none
 *
 */
static void test_dvrPlaybackBlockPresentationSuccess(CuTest* tc)
{
    mpe_Error error = 0;
    mpe_MediaDecodeSession decodeSession;
    uint32_t i;
    char errorBuffer[256];
    int64_t duration = 10 * 60; // 10 min in seconds
    mpe_DvrBitRate bitRate = (19500 * 1000) / 8; // in bytes/sec
    mpe_DvrBuffering tsbSession;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "---------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-- Starting test_dvrPlaybackBlockPresentationSuccess...\n");

    if (test_mediaDecodeSetupVideoDevices() != MPE_SUCCESS)
    {
        CuFail(tc, errorBuffer);
        return;
    }

    if (m_dvrQueue1 == NULL)
    {
        // event queue
        if (MPE_SUCCESS != eventQueueNew(&m_dvrQueue1, "DVR Event Queue"))
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "Error eventQueueNew() Could not create a queue.\n");
            return;
        }
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "-- (Time shift buffer creation)\n");
    // Create a TSB - 10 minutes
    if (buffer == NULL)
    {
        //		CuAssertIntEquals( tc, MPE_SUCCESS, dvrTsbNew( NULL, duration, &buffer ) );
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "dvrTsbNew() returned - buffer 0x%x\n", buffer);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling tune...\n");

    // Do a successful tune
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Tuning...\n");
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

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsb - (Time shift recording start)\n");
    // start time shift buffer recording (give tunerId of 1)
    /*	CuAssertIntEquals( tc, MPE_SUCCESS,
     mpe_dvrTsbBufferingStart(   1,              //Tuner ID
     buffer,         //TSBBuffer
     bitRate,        //bitRate
     m_dvrQueue1,    // dvr event queue
     NULL,           //act
     0,              //pidCount    
     NULL,           //pids,
     &tsbSession )); // buffering handle
     */
    // wait for 10 seconds while TSB buffering takes place
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "Sleeping for 10 seconds to allow playback\n");
    test_threadSleep(10000, 0); // Sleep 10 sec

    // Stop the media decode playback
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsb: calling mediaStop()...\n");
    //  CuAssertIntEquals( tc, MPE_SUCCESS, mediaStop(decodeSession));
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsb: mediaStop() done...\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "\ncalling mediaSetBounds()...\n");
    error = mediaSetBounds(test_mediaDecodeGetDefaultVideoDevice(), &fullSrc,
            &smallDest);
    //   CuAssertIntEquals( tc, MPE_SUCCESS, error );

    // do a tsb playback
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "\ntest_dvr_tsb: calling dvrTsbPlayStart()\n");
    //	error = dvrTsbPlayStart (buffer, test_mediaDecodeGetDefaultVideoDevice(), NULL, 0, 
    //                              MPE_DVR_POSITIVE_INFINITY, 1.0, m_dvrQueue1, NULL, FALSE, &playback);
    //  CuAssertIntEquals( tc, MPE_SUCCESS, error );
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "dvrTsbPlayStart() returned playback: 0x%x\n", playback);

    // now loop for a couple of iterations to test block / unblock of media
    for (i = 0; i < MAX_BLOCK_UNBLOCK_TRIES; i++)
    {
        TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
                "--    Looping for block/unblock (iteration=%d)\n", i);

        // Let the decode results be visible / audible to the end user
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "--   Sleeping for decode display (Unblocked)...\n");
        threadSleep(DECODE_PERIOD_SHORT, 0);

        /* error = dvrPlaybackBlockPresentation( playback, TRUE );
         if(error != MPE_SUCCESS)
         {
         TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   dvrPlaybackBlockPresentation failed %s\n",
         decodeError(error));
         }
         */
        // Let the decode results be blocked to the end user
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "--   Sleeping for decode display (Blocked)...\n");
        threadSleep(DECODE_PERIOD_SHORT, 0);

        /* error = dvrPlaybackBlockPresentation( playback, FALSE );
         if(error != MPE_SUCCESS)
         {
         TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   dvrPlaybackBlockPresentation failed %s\n",
         decodeError(error));
         }
         */
    }

    // Shutdown the dvr playback session
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling dvrPlayBackStop()...\n");
    //	CuAssertIntEquals( tc, MPE_SUCCESS, dvrPlayBackStop (playback) );
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "dvrPlayBackStop()...done\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "\nRestoring bounds with mediaSetBounds()\n");
    //	error = mediaSetBounds(test_mediaDecodeGetDefaultVideoDevice(), &fullSrc, &fullDest);
    //    CuAssertIntEquals( tc, MPE_SUCCESS, error );

    // Shutdown the tsb buffering session
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling dvrTsbBufferingStop()\n");
    //   CuAssertIntEquals( tc, MPE_SUCCESS, dvrTsbBufferingStop( tsbSession ));
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "dvrTsbStop() returned OK\n");

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--   Calling test_mediaDecodeTeardownVideoDevices()...\n");
    // Free up the memory used by the decoding structures
    if (test_mediaDecodeTeardownVideoDevices() != MPE_SUCCESS)
    {
        //	CuFail(tc, errorBuffer);
        return;
    }

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--   Call to test_mediaDecodeTeardownVideoDevices() returned success!\n");

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-- Finished test_dvrPlaybackBlockPresentationSuccess!\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------\n\n");

    return;

    test_failure: test_mediaDecodeTeardownVideoDevices();
    //	CuFail(tc, errorBuffer);
    return;
}

/****************************************************************************
 *
 *  test_dvrPlaybackBlockPresentationFailure()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpe_dvrPlaybackBlockPresentation" function 
 *
 * \api mpe_dvrPlaybackBlockPresentationFailure()
 *
 * \strategy 
 *  Test invalid usage of mpe_dvrPlaybackBlockPresentation
 *  
 *  Call mpe_mediaBlockPresentation with invalid parameters and check
 *  that proper error codes are returned
 *      1 - Test null session handle
 */
static void test_dvrPlaybackBlockPresentationFailure(CuTest* tc)
{
    mpe_Error error = 0;
    char errorBuffer[256];

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "---------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-- Starting test_dvrPlaybackBlockPresentationFailure...\n");

    if (test_mediaDecodeSetupVideoDevices() != MPE_SUCCESS)
    {
        CuFail(tc, errorBuffer);
        return;
    }

    // check for MPE_EINVAL returned when passed a NULL decode session
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Testing NULL decodeSession...\n");
    //CuAssertIntEquals( tc, MPE_DVR_ERR_INVALID_PARAM, dvrPlaybackBlockPresentation(NULL, TRUE) );


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

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-- Finished test_dvrPlaybackBlockPresentationFailure!\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------\n\n");

    return;
}

/****************************************************************************
 *
 *  test_dvrPlaybackBlockPresentationMisc()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpe_dvrPlaybackBlockPresentation" function 
 *
 * \api mpe_dvrPlaybackBlockPresentationMisc()
 *
 * \strategy 
 *  Test less common cases for usage of mpe_dvrPlaybackBlockPresentation
 *  
 *  Call mpe_dvrPlaybackBlockPresentation after a successful tune and decode.
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
static void test_dvrPlaybackBlockPresentationMisc(CuTest* tc)
{
    mpe_Error error = 0;
    mpe_MediaDecodeSession decodeSession;
    char errorBuffer[256];
    int64_t duration = 10 * 60; // 10 min in seconds
    mpe_DvrBitRate bitRate = (19500 * 1000) / 8; // in bytes/sec
    mpe_DvrBuffering tsbSession;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "---------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-- Starting test_dvrPlaybackBlockPresentationMisc...\n");

    if (test_mediaDecodeSetupVideoDevices() != MPE_SUCCESS)
    {
        CuFail(tc, errorBuffer);
        return;
    }

    if (m_dvrQueue1 == NULL)
    {
        // event queue
        if (MPE_SUCCESS != eventQueueNew(&m_dvrQueue1, "DVR Event Queue"))
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "Error eventQueueNew() Could not create a queue.\n");
            return;
        }
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "-- (Time shift buffer creation)\n");
    // Create a TSB - 10 minutes
    if (buffer == NULL)
    {
        //		CuAssertIntEquals( tc, MPE_SUCCESS, dvrTsbNew( NULL, duration, &buffer ) );
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "dvrTsbNew() returned - buffer 0x%x\n", buffer);

    // Do a successful tune
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Tuning...\n");
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

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsb - (Time shift recording start)\n");
    // start time shift buffer recording (give tunerId of 1)
    /*CuAssertIntEquals( tc, MPE_SUCCESS,
     mpe_dvrTsbBufferingStart(   1,              //Tuner ID
     buffer,         //TSBBuffer
     bitRate,        //bitRate
     m_dvrQueue1,    // dvr event queue
     NULL,           //act
     0,              //pidCount    
     NULL,           //pids,
     &tsbSession )); // buffering handle
     */
    // wait for 10 seconds while TSB buffering takes place
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "Sleeping for 10 seconds to allow playback\n");
    test_threadSleep(10000, 0); // Sleep 10 sec

    // Stop the media decode playback
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsb: calling mediaStop()...\n");
    CuAssertIntEquals(tc, MPE_SUCCESS, mediaStop(decodeSession));
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsb: mediaStop() done...\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "\ncalling mediaSetBounds()...\n");
    error = mediaSetBounds(test_mediaDecodeGetDefaultVideoDevice(), &fullSrc,
            &smallDest);
    CuAssertIntEquals(tc, MPE_SUCCESS, error);

    // block the presentation initially!!!!!

    // do a tsb playback that is initially blocked
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "\ntest_dvr_tsb: calling dvrTsbPlayStart()\n");
    //	error = dvrTsbPlayStart (buffer, test_mediaDecodeGetDefaultVideoDevice(), NULL, 0, 
    //                              MPE_DVR_POSITIVE_INFINITY, 1.0, m_dvrQueue1, NULL, TRUE, &playback);
    CuAssertIntEquals(tc, MPE_SUCCESS, error);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "dvrTsbPlayStart() returned playback: 0x%x\n", playback);

    // Let the dvr playback be visible
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "Sleeping for 5 seconds to allow playback (blocked)\n");
    test_threadSleep(5000, 0); // Sleep 5 sec

    // Shutdown the dvr playback session
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling dvrPlayBackStop()...\n");
    //	CuAssertIntEquals( tc, MPE_SUCCESS, dvrPlayBackStop (playback) );
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "dvrPlayBackStop()...done\n");

    // do a tsb playback that is initially unblocked
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "\ntest_dvr_tsb: calling dvrTsbPlayStart()\n");
    //	error = dvrTsbPlayStart (buffer, test_mediaDecodeGetDefaultVideoDevice(), NULL, 0, 
    //                               MPE_DVR_POSITIVE_INFINITY, 1.0, m_dvrQueue1, NULL, FALSE, &playback);
    //   CuAssertIntEquals( tc, MPE_SUCCESS, error );
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "dvrTsbPlayStart() returned playback: 0x%x\n", playback);

    // Let the dvr playback be visible
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "Sleeping for 5 seconds to allow playback (unblocked)\n");
    test_threadSleep(5000, 0); // Sleep 5 sec

    // Shutdown the dvr playback session
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling dvrPlayBackStop()...\n");
    //	CuAssertIntEquals( tc, MPE_SUCCESS, dvrPlayBackStop (playback) );
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "dvrPlayBackStop()...done\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "\nRestoring bounds with mediaSetBounds()\n");
    error = mediaSetBounds(test_mediaDecodeGetDefaultVideoDevice(), &fullSrc,
            &fullDest);
    //   CuAssertIntEquals( tc, MPE_SUCCESS, error );

    // Shutdown the tsb buffering session
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling dvrTsbBufferingStop()\n");
    //    CuAssertIntEquals( tc, MPE_SUCCESS, dvrTsbBufferingStop( tsbSession ));
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "dvrTsbStop() returned OK\n");

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

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-- Finished test_mediaBlockPresentationMisc!\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------\n\n");

    return;

    test_failure: test_mediaDecodeTeardownVideoDevices();
    CuFail(tc, errorBuffer);
    return;
}

/****************************************************************************/

/*********---------<<<<<<<<<<<< OLD TESTS >>>>>>>>>>>------------************/

/****************************************************************************/

/*	
 Names of these test cases don't necessarily mean what they say.
 They were named before we realized that individual test case 
 results can't be shared.
 */
static void test_dvr_tsbStart(CuTest* tc)
{
    mpe_Error retCode = MPE_SUCCESS;
    mpe_Event event;
    void *poptional_eventdata = NULL;
    int64_t startTime = 0;
    char errorBuffer[256];
    int64_t duration = 10 * 60; // 10 min in seconds
    mpe_DvrBitRate bitRate = (19500 * 1000) / 8; // in bytes/sec
    mpe_DvrBuffering tsbSession;

    // Timer related things.
    if (MPE_SUCCESS != eventQueueNew(&m_timerQueue1, "DVR Timer Queue"))
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "Error test_dvr_tsbStart() Could not create a queue.\n");
        return;
    }

    // Thread for the tuner queue to run in.
    if (0 == m_timerThreadId1)
    {
        if (MPE_SUCCESS != threadCreate(timerThread, &m_sharedData1,
                MPE_THREAD_PRIOR_DFLT, MPE_THREAD_STACK_SIZE,
                &m_timerThreadId1, "timerThread"))
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "Error: Could not create tread\n");
            return;
        }
    }

    if (MPE_SUCCESS != test_mediaDecodeSetupVideoDevices())
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "Error: Could not setup video devices\n");
        test_mediaDecodeTeardownVideoDevices();
        return;
    }

    /*
     // Timer related things.
     if( MPE_SUCCESS != eventQueueNew( &m_mediaQueue1, "DVR Media Queue" ) )
     {
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "Error: Could not create a queue.\n" );
     return;
     }

     // Thread for the tuner queue to run in.
     if( 0 == m_mediaThreadId1 )
     {
     if( MPE_SUCCESS != threadCreate( mediaThread,
     &m_sharedData1,
     MPE_THREAD_PRIOR_DFLT,
     MPE_THREAD_STACK_SIZE,
     &m_mediaThreadId1,
     "dvrMediaThread") )
     {
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "Error: Could not create tread\n" );
     return;
     }
     }
     */
    /* Create global condition */
    // If autoReset is 'TRUE' it means that when one thread
    // gets hold of the condition object all other thread
    // waiting for that condition object will be blocked
    // until the first thread release it (CondSet())
    // The initial state is set to 'FALSE' which means
    // all the thread will be blocked until it is set
    // by some master thread. It's same as doing a
    // condGet() right after creating it.
    // first param: autoReset (TRUE), second param : initial state (FALSE)

    if (condNew(TRUE, FALSE, &dvr_test_cond) != MPE_SI_SUCCESS)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "Error: Could not create condition obj\n");
        return;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsb: condNew() returned 0x%x\n", dvr_test_cond);

    if (m_dvrQueue1 == NULL)
    {
        // event queue
        if (MPE_SUCCESS != eventQueueNew(&m_dvrQueue1, "DVR Event Queue"))
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "Error eventQueueNew() Could not create a queue.\n");
            return;
        }
    }

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_TEST,
            "\n\n @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ 1 start @@@@@@@@@@@@@@@@@@@@@@@@@@@\n\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsb - Test case 1 (Time shift buffer creation)\n");

    // Create a TSB - 10 minutes
    if (buffer == NULL)
    {
        //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "creating new TSB...size: %d\n", TSBsize );
        //		dvrTsbNew( NULL, duration, &buffer );
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "done creating new TSB...\n");
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "dvrTsbNew() returned - buffer 0x%x\n", buffer);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "Test case 1 (Time shift buffer created!)\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling tune...\n");

    // Tune to digital, decode == true
    //	tune(1, TEST_FREQUENCY_DIGITAL_1,TEST_PROGRAM_NUMBER_DIGITAL_1,TEST_QAM_MODE_DIGITAL_1, true);
    // Do a successful tune
    retCode = test_mediaTunerDoSuccessfulTune(errorBuffer);
    if (retCode != MPE_SUCCESS)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "--   Tune did not succeed! Aborting test!\n");
        return;
    }

    // Send the decode request
    retCode = test_mediaDecodeDoSuccessfulDecode(errorBuffer, NULL, &session);
    if (retCode != MPE_SUCCESS)
    {
        sprintf(errorBuffer, "Decode request failed with error %s",
                decodeError(retCode));
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "--   Decode request failed! Aborting test!\n");
        return;
    }

    //    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling dvrTsbSetSize()...\n" );

    // Change TSB size to 15 minutes
    //	dvrTsbSetSize( buffer, newSize, &newSizeRet);	

    //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "dvrTsbSetSize() returned...%d\n", newSizeRet );

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsb - Test case 2 (Time shift recording start)\n");

    // start time shift buffer recording (give tunerId of 1)
    // bit_rate is not applicable now (pass '0' for now)
    /*	CuAssertIntEquals( tc, MPE_SUCCESS,
     mpe_dvrTsbBufferingStart(   1,              //Tuner ID
     buffer,         //TSBBuffer
     bitRate,        //bitRate
     m_dvrQueue1,    // dvr event queue
     NULL,           //act
     0,              //pidCount    
     NULL,           //pids,
     &tsbSession )); // buffering handle
     */
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsb - End of test case 2 (time shift recording started)\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsb start timer for 10 seconds...\n");

    wait_interval1 = 1 * 10 * 1000;
    retCode = eventQueueWaitNext(m_timerQueue1, &event, &poptional_eventdata,
            NULL, NULL,
            /*WAIT_INTERVAL*/wait_interval1);

    if (retCode == MPE_ETIMEOUT)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "Time up...1\n");
        event = MPE_ETIMEOUT;

        retCode = eventQueueSend(m_timerQueue1, MPE_ETIMEOUT, (void *) NULL,
                (void *) NULL, 0);

        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "Problem sending MPE_ETIMEOUT event...1\n\n");
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "Sent MPE_ETIMEOUT event...1\n\n");
        }
    }

    /*
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "dvrTsbGet(starttime) - Test case 1\n" );
     
     // Get TSB start time
     //CuAssertIntEquals( tc,
     //                 MPE_SUCCESS,
     //                 dvrTsbGet( buffer, MPE_DVR_TSB_START_TIME, &startTime));
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "dvrTsbGet - startTime %llu\n", startTime );
     
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "dvrTsbGet(starttime) - End of test case 1\n" );
     
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "dvrTsbGet(endtime) - Test case 2\n" );
     
     // Get TSB end time
     //CuAssertIntEquals( tc,
     //                 MPE_SUCCESS,
     //                 dvrTsbGet( buffer, MPE_DVR_TSB_END_TIME, &endTime));
     
     //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "dvrTsbGet(endtime)  - endTime %llu\n", endTime );
     
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "dvrTsbGet(endtime)  - End of test case 3\n" );
     */
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsb: calling mediaStop()...\n");

    mediaStop(session);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsb: mediaStop() done...\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsb: calling dvrTsbPlayStart()...\n");

    // hardcode pids for now!! (change!!!)
    pids[0].pid = 0x1E;
    pids[0].pidType = 0x80;
    pids[1].pid = 0x1F;
    pids[1].pidType = 0x81;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "\n\ncalling mediaSetBounds()...\n\n");

    mediaSetBounds(test_mediaDecodeGetDefaultVideoDevice(), &fullSrc,
            &smallDest);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "\ntest_dvr_tsb: calling dvrTsbPlayStart()...NULL pids\n\n");

    //dvrTsbPlayStart (buffer, 0x22000021, pids, 2, ,MPE_DVR_POSITIVE_INFINITY, 1.0, m_dvrQueue1, &playback);
    //	dvrTsbPlayStart (buffer, test_mediaDecodeGetDefaultVideoDevice(), NULL, 0, 
    //                     MPE_DVR_POSITIVE_INFINITY, 1.0, m_dvrQueue1, NULL, FALSE, &playback);
    //dvrTsbPlayStart (buffer, 0x22000021, NULL, 0, ,0, 1,0, m_dvrQueue1, &playback);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "dvrTsbPlayStart() returned playback: 0x%x\n", playback);

    /*
     timeGetMillis(&ptime);
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling dvrSetTrickMode()...\n" );

     // Pause the TSB playback
     dvrSetTrickMode( playback, 0.0, &actual_mode );

     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "dvrSetTrickMode() returned %f\n", actual_mode );

     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "Start timer for 2 minutes...2\n" );

     retCode = eventQueueWaitNext(m_timerQueue1, &event, &poptional_eventdata, NULL, NULL,
     WAIT_INTERVAL);

     if(retCode == MPE_ETIMEOUT)
     {
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"Time up...2\n");
     event = MPE_ETIMEOUT ;
     
     retCode = eventQueueSend(m_timerQueue1, MPE_ETIMEOUT, (void *) NULL, (void *) NULL, 0) ;
     
     if (retCode != MPE_SUCCESS)
     {
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"Problem sending MPE_ETIMEOUT event...2\n\n");
     }
     else
     {
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"Sent MPE_ETIMEOUT event...2\n\n");
     }
     }
     
     //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling dvrPlayBackStop()...\n" );

     //dvrPlayBackStop (playback);

     //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "dvrPlayBackStop()...done\n" );

     // Playback speed to normal
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling dvrSetTrickMode()...\n" );

     dvrSetTrickMode( playback, 1.0, &actual_mode );

     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "dvrSetTrickMode() returned %f\n", actual_mode );

     //test_threadSleep( 2000, 0 ); // Sleep 2 sec

     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling dvrTsbGet()...1\n" );
     
     //CuAssertIntEquals( tc,
     //                 MPE_SUCCESS,
     //                 dvrTsbGet( buffer, MPE_DVR_TSB_START_TIME, &startTime));
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsbGet() - startTime %llu\n", startTime );
     
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsbGet() - End 1\n" );
     
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsbGet() - 2\n" );
     
     //CuAssertIntEquals( tc,
     //                 MPE_SUCCESS,
     //                 dvrTsbGet( buffer, MPE_DVR_TSB_END_TIME, &endTime));
     
     //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsbGet() - endTime %llu\n", endTime );
     
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsbGet() - End 2\n" );
     
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsb: calling tsbConvert()...(2 minutes)\n" );
     
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling  dvrTsbConvertStart- duration: 2*60*1000\n");

     //test_threadSleep( 2000, 0 ); // Sleep 2 sec
     // start: systemTime, end: 1*60*1000 (1 minutes in milli sec)
     // convert 2 minutes from TSB
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling  dvrTsbConvertStart- ptime: %llu\n", ptime);

     // Convert 2 minutes worth of TSB recording
     time1 = 2*60*1000;
     startTime = ptime * 1000000L;
     //dvrTsbConvertStart( buffer, &startTime, time1, m_dvrQueue1, NULL, &recording, recordingName);

     //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsbConvert() - recordingName: %s\n", recordingName );
     //	MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsbConvert() - recording: 0x%x\n", recording );

     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsbConvert() - End of test\n" );
     
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "Start timer for 2 minutes...3\n" );

     event = 0;
     retCode = MPE_SUCCESS;

     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "eventQueueWaitNext - event: 0x%x\n", event );
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "eventQueueWaitNext - retCode: 0x%x\n", retCode );

     retCode = eventQueueWaitNext(m_timerQueue1, &event, &poptional_eventdata, NULL, NULL,
     WAIT_INTERVAL);

     if(retCode == MPE_ETIMEOUT)
     {
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nTime up...3\n");
     event = MPE_ETIMEOUT ;
     
     retCode = eventQueueSend(m_timerQueue1, MPE_ETIMEOUT, (void *) NULL, (void *) NULL, 0) ;
     
     if (retCode != MPE_SUCCESS)
     {
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nProblem sending MPE_ETIMEOUT event...3\n\n");
     }
     else
     {
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nSent MPE_ETIMEOUT event...3\n\n");
     }
     }

     // rewind TSB playback @ -4.0
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling dvrSetTrickMode()...(-4)\n" );

     dvrSetTrickMode( playback, -4, &actual_mode );

     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "dvrSetTrickMode() returned %f\n", actual_mode );
     
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "\nStart timer for 5 minutes...4\n" );

     wait_interval1 = 5*60*1000;
     retCode = eventQueueWaitNext(m_timerQueue1, &event, &poptional_eventdata, NULL, NULL,
     wait_interval1);

     if(retCode == MPE_ETIMEOUT)
     {
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nTime up...4\n");
     event = MPE_ETIMEOUT ;
     
     retCode = eventQueueSend(m_timerQueue1, MPE_ETIMEOUT, (void *) NULL, (void *) NULL, 0) ;
     
     if (retCode != MPE_SUCCESS)
     {
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nProblem sending MPE_ETIMEOUT event...4\n\n");
     }
     else
     {
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nSent MPE_ETIMEOUT event...4\n\n");
     }
     }
     
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling dvrSetTrickMode()...\n" );

     // Pause
     dvrSetTrickMode( playback, 0.0, &actual_mode );

     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "dvrSetTrickMode() returned %f\n", actual_mode );

     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "\nStart timer for 3 minutes...4\n" );

     wait_interval1 = 3*60*1000;
     retCode = eventQueueWaitNext(m_timerQueue1, &event, &poptional_eventdata, NULL, NULL,
     wait_interval1);

     if(retCode == MPE_ETIMEOUT)
     {
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nTime up...4\n");
     event = MPE_ETIMEOUT ;
     
     retCode = eventQueueSend(m_timerQueue1, MPE_ETIMEOUT, (void *) NULL, (void *) NULL, 0) ;
     
     if (retCode != MPE_SUCCESS)
     {
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nProblem sending MPE_ETIMEOUT event...4\n\n");
     }
     else
     {
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nSent MPE_ETIMEOUT event...4\n\n");
     }
     }
     
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling dvrSetTrickMode()...\n" );

     // normal playback
     dvrSetTrickMode( playback, 1.0, &actual_mode );

     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "dvrSetTrickMode() returned %f\n", actual_mode );
     
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling tsbConvertStop()\n" );
     
     //dvrTsbConvertStop( buffer);

     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "tsbConvertStop() done...\n" );
     */
    test_threadSleep(5000, 0); // Sleep 5 sec

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling dvrPlayBackStop()...\n");

    //	dvrPlayBackStop (playback);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "dvrPlayBackStop()...done\n");

    //test_threadSleep( 3000, 0 ); // Sleep 2 sec

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling dvrTsbStop()\n");

    //	CuAssertIntEquals( tc, MPE_SUCCESS, dvrTsbBufferingStop( tsbSession ));

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "dvrTsbStop() returned OK\n");

    /*
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "\nDEBUG: calling mediaStop()...\n" );

     mediaStop(session);
     
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "\n\n @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ 1 end @@@@@@@@@@@@@@@@@@@@@@@@@@@\n\n" );

     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "Start timer for 2 minutes...\n" );

     retCode = eventQueueWaitNext(m_timerQueue1, &event, &poptional_eventdata, NULL, NULL,
     WAIT_INTERVAL);

     if(retCode == MPE_ETIMEOUT)
     {
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nTime up...\n");
     event = MPE_ETIMEOUT ;
     
     retCode = eventQueueSend(m_timerQueue1, MPE_ETIMEOUT, (void *) NULL, (void *) NULL, 0) ;
     
     if (retCode != MPE_SUCCESS)
     {
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nProblem sending MPE_ETIMEOUT event...\n\n");
     }
     else
     {
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nSent MPE_ETIMEOUT event...\n\n");
     }
     }
     */
} // end test_dvr_tsbStart(CuTest*)


static void test_dvr_tsbStop(CuTest* tc)
{

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsbStop() - Test case 1\n");

    // Change the size of static time shift buffer
    //CuAssertIntEquals( tc,
    //                 MPE_SUCCESS,
    //                 dvrTsbStop( buffer ));

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsbStop() returned OK - End of test case 1\n");

} // end test_dvr_tsbStop(CuTest*)


/* 
 This test case exercizes the PID selection capability of TSB playback.
 TSB recording will record all audio pids present in the stream.
 We picked a source here which has english and spanish
 audio streams present in it. We first try playing back the default audio
 which happens to be english. Then after a brief time period stop the playback
 and then switch the playback to play spanish audio. 
 It also tests trick modes.

 PID values here are hardcoded to facilitate testing. Make sure the audio PID values
 match before running this test.
 */
static void test_dvr_tsbPlay(CuTest* tc)
{
    mpe_Error retCode = MPE_SUCCESS;
    mpe_Event event;
    void *poptional_eventdata = NULL;
    char errorBuffer[256];

    // Timer related things.
    if (MPE_SUCCESS != eventQueueNew(&m_timerQueue1, "DVR Timer Queue"))
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "Error: Could not create a queue.\n");
        return;
    }

    // Thread for the tuner queue to run in.
    if (0 == m_timerThreadId1)
    {
        if (MPE_SUCCESS != threadCreate(timerThread, &m_sharedData1,
                MPE_THREAD_PRIOR_DFLT, MPE_THREAD_STACK_SIZE,
                &m_timerThreadId1, "timerThread"))
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "Error: Could not create tread\n");
            return;
        }
    }

    /*
     // Timer related things.
     if( MPE_SUCCESS != eventQueueNew( &m_mediaQueue1, "DVR Media Queue" ) )
     {
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "Error: Could not create a queue.\n" );
     return;
     }

     // Thread for the tuner queue to run in.
     if( 0 == m_mediaThreadId1 )
     {
     if( MPE_SUCCESS != threadCreate( mediaThread,
     &m_sharedData1,
     MPE_THREAD_PRIOR_DFLT,
     MPE_THREAD_STACK_SIZE,
     &m_mediaThreadId1,
     "dvrMediaThread") )
     {
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "Error: Could not create tread\n" );
     return;
     }
     }
     */
    /* Create global condition */
    // If autoReset is 'TRUE' it means that when one thread
    // gets hold of the condition object all other thread
    // waiting for that condition object will be blocked
    // until the first thread release it (CondSet())
    // The initial state is set to 'FALSE' which means
    // all the thread will be blocked until it is set
    // by some master thread. It's same as doing a
    // condGet() right after creating it.
    // first param: autoReset (TRUE), second param : initial state (FALSE)

    if (condNew(TRUE, FALSE, &dvr_test_cond) != MPE_SI_SUCCESS)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "Error: Could not create condition obj\n");
        return;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsb: condNew() returned 0x%x\n", dvr_test_cond);

    if (m_dvrQueue1 == NULL)
    {
        // event queue
        if (MPE_SUCCESS != eventQueueNew(&m_dvrQueue1, "DVR Event Queue"))
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "Error eventQueueNew() Could not create a queue.\n");
            return;
        }
    }

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_TEST,
            "\n\n @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ 1 start @@@@@@@@@@@@@@@@@@@@@@@@@@@\n\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsb - Test case 1 (Time shift buffer creation)\n");

    if (buffer == NULL)
    {
        //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "creating new TSB...size: %d\n", TSBsize );
        //dvrTsbNew( TSBsize, NULL, &buffer, &duration);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "done creating new TSB...\n");
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "dvrTsbNew() returned - buffer 0x%x\n", buffer);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "Test case 1 (Time shift buffer created!)\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling tune...\n");
    //	tune(1, 597000000,8,MPE_SI_MODULATION_QAM256, true);
    retCode = test_mediaTunerDoSuccessfulTune(errorBuffer);
    if (retCode != MPE_SUCCESS)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "--   Tune did not succeed! Aborting test!\n");
        return;
    }

    // Send the decode request
    retCode = test_mediaDecodeDoSuccessfulDecode(errorBuffer, NULL, &session);
    if (retCode != MPE_SUCCESS)
    {
        sprintf(errorBuffer, "Decode request failed with error %s",
                decodeError(retCode));
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "--   Decode request failed! Aborting test!\n");
        return;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsb - Test case 2 (Time shift recording start)\n");

    // start time shift buffer recording (give tunerId of 1)
    //CuAssertIntEquals( tc,
    //                 MPE_SUCCESS,
    //                 dvrTsbStart( 1, buffer, 0,
    //							  m_dvrQueue1, NULL));

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsb - End of test case 2 (time shift recording started)\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsb start timer for 2 minutes...1\n");

    timeGetMillis(&ptime);

    wait_interval1 = 3 * 60 * 1000;
    retCode = eventQueueWaitNext(m_timerQueue1, &event, &poptional_eventdata,
            NULL, NULL,
            /*WAIT_INTERVAL*/wait_interval1);

    if (retCode == MPE_ETIMEOUT)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "Time up...1\n");
        event = MPE_ETIMEOUT;

        retCode = eventQueueSend(m_timerQueue1, MPE_ETIMEOUT, (void *) NULL,
                (void *) NULL, 0);

        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "Problem sending MPE_ETIMEOUT event...1\n\n");
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "Sent MPE_ETIMEOUT event...1\n\n");
        }
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsb: calling mediaStop()...\n");

    // hardcoding mpe_dispDevice for now.. (required to start tsbPlayStart)
    mediaStop(session);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsb: mediaStop() done...\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsb: calling dvrTsbPlayStart()...\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "\ntest_dvr_tsb: calling dvrTsbPlayStart()...NULL pids\n\n");

    // hardcode pids for now!! (change!!!)
    pids[0].pid = 0x4B; // video
    pids[0].pidType = 0x80;
    pids[1].pid = 0x4D; // audio
    pids[1].pidType = 0x81;

    //	dvrTsbPlayStart (buffer, (mpe_DispDevice)0x22000001, NULL, 0, MPE_DVR_POSITIVE_INFINITY, 1.0, m_dvrQueue1, NULL, FALSE, &playback);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "1 - dvrTsbPlayStart()...playback: 0x%x\n", playback);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "Start timer for 2 minutes...2\n");

    //	retCode = eventQueueWaitNext(m_timerQueue1, &event, &poptional_eventdata, NULL, NULL,
    //					                   WAIT_INTERVAL);

    if (retCode == MPE_ETIMEOUT)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "Time up...2\n");
        event = MPE_ETIMEOUT;

        retCode = eventQueueSend(m_timerQueue1, MPE_ETIMEOUT, (void *) NULL,
                (void *) NULL, 0);

        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "Problem sending MPE_ETIMEOUT event...2\n\n");
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "Sent MPE_ETIMEOUT event...2\n\n");
        }
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "dvrPlayBackStart()...done\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling dvrPlayBackStop()...\n");

    //	dvrPlayBackStop (playback);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "dvrPlayBackStop()...done\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "Start timer for 2 minutes...2\n");

    //	retCode = eventQueueWaitNext(m_timerQueue1, &event, &poptional_eventdata, NULL, NULL,
    //					                   WAIT_INTERVAL);

    if (retCode == MPE_ETIMEOUT)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "Time up...2\n");
        event = MPE_ETIMEOUT;

        retCode = eventQueueSend(m_timerQueue1, MPE_ETIMEOUT, (void *) NULL,
                (void *) NULL, 0);

        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "Problem sending MPE_ETIMEOUT event...2\n\n");
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "Sent MPE_ETIMEOUT event...2\n\n");
        }
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "\ncalling dvrTsbPlayStart with pids (spanish audio!!)\n");

    // TODO: Use a dvrPidInfo structure instead of a PID array
    //dvrTsbPlayStart (buffer, (mpe_DispDevice)0x22000001, pids, 2, MPE_DVR_POSITIVE_INFINITY, 1.0,m_dvrQueue1, NULL, &playback);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "2 - dvrTsbPlayStart()...playback: 0x%x\n", playback);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling dvrTsbGet()...1\n");

    //CuAssertIntEquals( tc,
    //                 MPE_SUCCESS,
    //                 dvrTsbGet( buffer, MPE_DVR_TSB_START_TIME, &startTime));
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsbGet() - startTime %"PRIu64"\n", startTime );

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsbGet() - End 1\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsbGet() - 2\n");

    //CuAssertIntEquals( tc,
    //                 MPE_SUCCESS,
    //                 dvrTsbGet( buffer, MPE_DVR_TSB_END_TIME, &endTime));

    //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsbGet() - endTime %llu\n", endTime );

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsbGet() - End 2\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsb: calling tsbConvert()...(3 minutes)\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "calling  dvrTsbConvertStart- duration: 3*60*1000\n");

    //test_threadSleep( 2000, 0 ); // Sleep 2 sec
    // start: systemTime, end: 1*60*1000 (1 minutes in milli sec)
    // convert 1 minutes from TSB
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling  dvrTsbConvertStart- ptime: %"PRIu64"\n", ptime);

    time1 = 3 * 60 * 1000;
    startTime = ptime * 1000000L;
    //dvrTsbConvertStart( buffer, &startTime, time1, m_dvrQueue1, NULL, &recording, recordingName);

    //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsbConvert() - recordingName: %s\n", recordingName );
    //	MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsbConvert() - recording: 0x%x\n", recording );

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsbConvert() - End of test\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "Start timer for 2 minutes...3\n");

    retCode = eventQueueWaitNext(m_timerQueue1, &event, &poptional_eventdata,
            NULL, NULL, WAIT_INTERVAL);

    if (retCode == MPE_ETIMEOUT)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "\nTime up...3\n");
        event = MPE_ETIMEOUT;

        retCode = eventQueueSend(m_timerQueue1, MPE_ETIMEOUT, (void *) NULL,
                (void *) NULL, 0);

        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "\nProblem sending MPE_ETIMEOUT event...3\n\n");
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "\nSent MPE_ETIMEOUT event...3\n\n");
        }
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling dvrSetTrickMode()...(-4)\n");

    //	dvrSetTrickMode( playback, -4, &actual_mode );

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "dvrSetTrickMode() returned %f\n",
            actual_mode);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "\nStart timer for 5 minutes...4\n");

    wait_interval1 = 5 * 60 * 1000;
    //	retCode = eventQueueWaitNext(m_timerQueue1, &event, &poptional_eventdata, NULL, NULL,
    //					                   wait_interval1);

    if (retCode == MPE_ETIMEOUT)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "\nTime up...4\n");
        event = MPE_ETIMEOUT;

        retCode = eventQueueSend(m_timerQueue1, MPE_ETIMEOUT, (void *) NULL,
                (void *) NULL, 0);

        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "\nProblem sending MPE_ETIMEOUT event...4\n\n");
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "\nSent MPE_ETIMEOUT event...4\n\n");
        }
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling dvrSetTrickMode()...\n");

    //	dvrSetTrickMode( playback, 1.0, &actual_mode );

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "dvrSetTrickMode() returned %f\n",
            actual_mode);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "\nStart timer for 3 minutes...4\n");

    wait_interval1 = 3 * 60 * 1000;
    //	retCode = eventQueueWaitNext(m_timerQueue1, &event, &poptional_eventdata, NULL, NULL,
    //					                   /*WAIT_INTERVAL*/ wait_interval1);

    if (retCode == MPE_ETIMEOUT)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "\nTime up...4\n");
        event = MPE_ETIMEOUT;

        retCode = eventQueueSend(m_timerQueue1, MPE_ETIMEOUT, (void *) NULL,
                (void *) NULL, 0);

        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "\nProblem sending MPE_ETIMEOUT event...4\n\n");
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "\nSent MPE_ETIMEOUT event...4\n\n");
        }
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling tsbConvertStop()\n");

    //dvrTsbConvertStop( buffer);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "tsbConvertStop() done...\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling dvrPlayBackStop()...\n");

    //	dvrPlayBackStop (playback);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "dvrPlayBackStop()...done\n");

    test_threadSleep(3000, 0); // Sleep 2 sec

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling dvrTsbStop()\n");

    //CuAssertIntEquals( tc,
    //                 MPE_SUCCESS,
    //                 dvrTsbStop( buffer ));

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "dvrTsbStop() returned OK - End of test case 1\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "\nDEBUG: calling mediaStop()...\n");

    // hardcoding mpe_dispDevice for now..
    mediaStop(session);

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_TEST,
            "\n\n @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ 1 end @@@@@@@@@@@@@@@@@@@@@@@@@@@\n\n");
} // end test_dvr_tsbPlay(CuTest*)

static void test_dvr_tsbResize(CuTest* tc)
{
    //uint32_t newSizeRet = 0;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsbResize() - Test case 1\n");

    if (buffer == NULL)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "creating new TSB...\n");
        //dvrTsbNew( TSBsize, NULL, &buffer, &duration);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "done creating new TSB...\n");
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsbResize() - End of test case 1\n");

    test_threadSleep(3000, 0); // Sleep 2 sec

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsbResize() - buffer 0x%x\n", buffer);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsbResize() - Test case 2\n");

    // Change the size of static time shift buffer
    //CuAssertIntEquals( tc,
    //                 MPE_SUCCESS,
    //                 dvrTsbSetSize( buffer, newSize, &newSizeRet));

    //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsbResize() dvrTsbSetSize() returned new size %d bytes\n", newSizeRet);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsbResize() - End of test case 2\n");

    test_threadSleep(3000, 0); // Sleep 2 sec

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsbResize() - Test case 3 (really big TSB (6hrs!!))\n");

    // Change the size of static time shift buffer
    //CuAssertIntEquals( tc,
    //                 MPE_SUCCESS,
    //                 dvrTsbSetSize( buffer, bigSize, &newSizeRet));

    //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsbResize() dvrTsbSetSize() returned new size %d bytes\n", newSizeRet);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsbResize() - End of test case 3\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsbResize() - Test case 4 - _dvrTsbSetDuration()\n");

    // Change the size of static time shift buffer to 15 minutes
    //CuAssertIntEquals( tc,
    //                 MPE_SUCCESS,
    //                 dvrTsbSetSize( buffer, newSize, &newSizeRet));

    //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsbResize() dvrTsbSetSize() returned new size %d bytes\n", newSizeRet);

    // Change the duration to 5 minutes - 300,000ms
    //CuAssertIntEquals(tc,
    //                 MPE_SUCCESS,
    //                 mpe_dvrTsbSetDuration( buffer, 300000, &tsbDuration));

    CuAssertIntEquals(tc, 600000, tsbDuration);

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_TEST,
            "test_dvr_tsbResize() _dvrTsbSetDuration() returned new duration %dms\n",
            tsbDuration);

    // Change the duration to 15 minutes - 900,000ms
    // should return 10 minutes
    //CuAssertIntEquals(tc,
    //                 MPE_SUCCESS,
    //                 mpe_dvrTsbSetDuration( buffer, 300000, &tsbDuration));

    CuAssertIntEquals(tc, 600000, tsbDuration);

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_TEST,
            "test_dvr_tsbResize() _dvrTsbSetDuration() returned new duration %dms\n",
            tsbDuration);

} // end test_dvr_tsbResize(CuTest*)

static void test_dvr_tsbGet(CuTest* tc)
{
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsbGet() - Test case 1\n");

    //CuAssertIntEquals( tc,
    //                 MPE_SUCCESS,
    //                 dvrTsbGet( buffer, MPE_DVR_TSB_START_TIME, &startTime));
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsbGet() - startTime %d\n",
            startTime);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsbGet() - End of test case 1\n");

    test_threadSleep(2000, 0); // Sleep 2 sec

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsbGet() - Test case 2\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsbGet() - buffer 0x%x\n",
            buffer);

    //CuAssertIntEquals( tc,
    //                 MPE_SUCCESS,
    //                 dvrTsbGet( buffer, MPE_DVR_TSB_END_TIME, &endTime));

    //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsbGet() - startTime %d\n", endTime );

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsbGet() - End of test case 3\n");

} // end test_dvr_tsbResize(CuTest*)

static void test_dvr_tsbConvert(CuTest* tc)
{
    int64_t startTime;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsbConvert() - Test case 1\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsbConvert() - buffer 0x%x\n", buffer);

    // convert 1 min of TSB
    startTime = 0;
    //CuAssertIntEquals( tc,
    //                 MPE_SUCCESS,
    //                 dvrTsbConvertStart( buffer, &startTime, 60, m_dvrQueue1, NULL, NULL, recordingName));

    //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_tsbConvert() - recordingName: %s\n", recordingName );

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_tsbConvert() - End of test case 1\n");

} // end test_dvr_tsbConvert(CuTest*)


/****************************************************************************/

/******---------<<<<<<<<<<<< END OF OLD TESTS >>>>>>>>>>>----------**********/

/****************************************************************************/

/**
 * Will return the suite information describing the TSB tests.
 *
 * @return Will return a CuSuite* that describes the suite for time shift buffer tests.
 */
CuSuite* getTestSuite_TSB(void)
{
    CuSuite* suite = CuSuiteNew();

    // NEW TESTS
    SUITE_ADD_TEST(suite, test_dvrTsbBufferingAndPlayback);
    SUITE_ADD_TEST(suite, test_dvrPlaybackBlockPresentationSuccess);
    SUITE_ADD_TEST(suite, test_dvrPlaybackBlockPresentationFailure);
    SUITE_ADD_TEST(suite, test_dvrPlaybackBlockPresentationMisc);

    // OLD TESTS
    //  SUITE_ADD_TEST(suite, test_dvr_tsbStart );
    //SUITE_ADD_TEST(suite, test_dvr_tsbPlay );
    //SUITE_ADD_TEST(suite, test_dvr_tsbResize );
    //  SUITE_ADD_TEST(suite, test_dvr_tsbGet );
    //  SUITE_ADD_TEST(suite, test_dvr_tsbConvert);
    //SUITE_ADD_TEST(suite, test_dvr_tsbStop );

    return suite;
}

CuSuite* getTestSuite_TSB1(void)
{
    CuSuite* suite = CuSuiteNew();

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "getTestSuite_TSB test suite test_dvr_tsb1.\n");
    //SUITE_ADD_TEST(suite, test_dvr_tsbStart );
    SUITE_ADD_TEST(suite, test_dvr_tsbPlay);

    return suite;
}

CuSuite* getTestSuite_TSB2(void)
{
    CuSuite* suite = CuSuiteNew();

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "getTestSuite_TSB test suite test_dvr_tsb2.\n");
    //SUITE_ADD_TEST(suite, test_dvr_tsbConvert );
    SUITE_ADD_TEST(suite, test_dvr_tsbResize);
    return suite;
}

CuSuite* getTestSuite_TSB3(void)
{
    CuSuite* suite = CuSuiteNew();

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "getTestSuite_TSB test suite test_dvr_tsb3.\n");
    SUITE_ADD_TEST(suite, test_dvr_tsbGet);

    return suite;
}

CuSuite* getTestSuite_TSB4(void)
{
    CuSuite* suite = CuSuiteNew();

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "getTestSuite_TSB test suite test_dvr_tsb4.\n");
    SUITE_ADD_TEST(suite, test_dvr_tsbStop);

    return suite;
}

static void tune(uint32_t tunerId, uint32_t freq, uint32_t prog, uint32_t qam,
        mpe_Bool decode)
{

    mpe_EventQueue tunerQueue = m_mediaQueue1;

    mpe_MediaTuneRequestParams tunerRequestParams;

    tunerRequestParams.tunerId = tunerId;
    tunerRequestParams.tuneParams.frequency = freq;
    tunerRequestParams.tuneParams.programNumber = prog;
    tunerRequestParams.tuneParams.qamMode = qam;
    tunerRequestParams.tuneParams.tuneType = MPE_MEDIA_TUNE_BY_TUNING_PARAMS;

    tuneSuccess = false;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "tune() - start of tune \n");

    mediaTune(&tunerRequestParams, tunerQueue, NULL);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "tune() - End of tune \n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "tune() - calling condGet()\n");

    condGet(dvr_test_cond);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "tune() - condGet() OK!!\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "tune() - tuneSuccess: %d\n",
            tuneSuccess);

    if (tuneSuccess == true)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "tune(%d %d %d) - Tuning success\n", freq, prog, qam);
        if (decode == true)
        {
            mpe_MediaDecodeRequestParams decoderRequestParams;
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling mediaDecode()...\n");
            decoderRequestParams.tunerId = 1;
            decoderRequestParams.numPids = 0;
            // just hardcode the value for now
            decoderRequestParams.videoDevice = (mpe_DispDevice) 0x22000001; // Fix this!!!!

            mediaDecode(&decoderRequestParams, m_mediaQueue1, NULL, &session);
        }

    }

    condUnset(dvr_test_cond);

    tuneSuccess = false;
}

static void mediaThread(void* data)
{
    mpe_Event eventId;
    void* eventData;

    while (1)
    {
        // wait forever until the next event
        eventQueueWaitNext(m_mediaQueue1, &eventId, &eventData, NULL, NULL, 0);

        switch (eventId)
        {
        case MPE_TUNE_SYNC:
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "TUNE_SYNC event received in mediaThread...\n");
            tuneSuccess = true;
            condSet(dvr_test_cond);
        }
            break;

        case MPE_TUNE_FAIL:
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "TUNE_FAIL event received in mediaThread...\n");
            condSet(dvr_test_cond);
            tuneSuccess = false;
        }
            break;

        case MPE_TUNE_ABORT:
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "TUNE_ABORT event received in mediaThread...\n");
            condSet(dvr_test_cond);
            tuneSuccess = false;
        }
            break;

        default:
        {
            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_TEST,
                    "Unknown event received in mediaThread (TUNE_STARTED??)...%d\n",
                    eventId);
            //condSet(dvr_test_cond);
            tuneSuccess = false;
        }
            break;

        } // switch
    }
}

static void timerThread(void* data)
{
    mpe_Event eventId;
    void* eventData;

    while (1)
    {
        // wait forever until the next event
        eventQueueWaitNext(m_timerQueue1, &eventId, &eventData, NULL, NULL, 0);
        switch (eventId)
        {
        case MPE_ETIMEOUT:
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "MPE_ETIMEOUT event received in timerThread...(timeout up)\n");
        }
            break;

        default:
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "Unknown event received in timerThread...%d\n", eventId);
        }
            break;

        } // switch
    }

} // end timerThread(void*)
