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

/** \file test_media_tuner.c
 *
 *  \brief Test functions for MPEOS media tune functions
 *
 *  This file contains tests for the following MPEOS functions :\n
 *
 *    -# mpeos_mediaGetTunerFrequency()\n
 *    -# mpeos_mediaFrequencyToTuner()\n
 *    -# mpeos_mediaGetTunerInfo()\n
 *    -# mpeos_mediaTuneXXX()\n
 *  
 */

#include <test_media.h>

/*  Media tune test functions  */
static void test_mediaGetTunerFrequency(CuTest*);
static void test_mediaFrequencyToTuner(CuTest*);
static void test_mediaGetTunerInfoTuningParams(CuTest*);

static void test_mediaTuneByTuningParamsSuccess1(CuTest*);
static void test_mediaTuneByTuningParamsSuccess2(CuTest*);
static void test_mediaTuneByTuningParamsFailure1(CuTest*);
static void test_mediaTuneByTuningParamsFailure2(CuTest*);
static void test_mediaTuneByTuningParamsFailure3(CuTest*);

/*  Function to add tests to a test suite  */
CuSuite* getTestSuite_tuner(void);

/*  Global variables for the tuning related tests */
static mpe_MediaTuneRequestParams requestParams;
static mpe_EventQueue queue;

typedef struct
{
    int state;
    int index;
} TestQueues;

#define MAX_TUNER_QUEUES    50

static TestQueues queues[MAX_TUNER_QUEUES];
static int inited = FALSE;
static int lastQueueId = 0;

#define STATE_NOT_RUN 		0
#define STATE_READY 		1
#define STATE_STARTED 		2
#define STATE_COMPLETED 	3
#define STATE_FAILED 		4
#define STATE_ABORTED 		5
#define STATE_SHUTDOWN 		6
#define STATE_INVALID 		7

/**
 * Explanation for the following array:
 *   This array is used to map the valid transitions between each state of the
 *   tuning queue state machine. The values in this array equal the values of
 *   the constants above.
 *   The columns map (indirectly) to the events received from the tuning system.
 *   e.g. MPE_TUNE_STARTED = column index 0, MPE_TUNE_SYNC = column index 1
 *   The sixth column is for any unexpected events (like decoding events on the
 *   tuning queue). The seventh column is what we'll call the watchdog events.
 *   Future use may dictate extra events being passed to the queue for watchdog
 *   purposes. We can use this column to reset the state to it's current state.
 *   Currently, there are no watchdog events, but this is future-proofing.
 *   Example of usage - 
 *     Current state is STATE_STARTED (2), received a MPE_TUNE_SYNC, which maps
 *     to column index 1. State is moved to STATE_COMPLETED (3).
 *   Also one thing to note, the constants from above are not used because it 
 *   would make for horrendously difficult reading.
 */
int validTrans[][7] =
{
{ 7, 7, 7, 7, 7, 7, 0 }, // STATE_NOT_RUN
        { 2, 7, 7, 7, 7, 7, 1 }, // STATE_READY
        { 7, 3, 4, 5, 7, 7, 2 }, // STATE_STARTED
        { 7, 7, 7, 5, 7, 7, 3 }, // STATE_COMPLETED
        { 7, 7, 7, 7, 6, 7, 4 }, // STATE_FAILED
        { 7, 7, 7, 7, 6, 7, 5 }, // STATE_ABORTED
        { 7, 7, 7, 7, 7, 7, 6 }, // STATE_SHUTDOWN
        { 7, 7, 7, 7, 7, 7, 7 } }; // STATE_INVALID

static mpe_Error init(char*);
static mpe_Error processEvents(char*, mpe_EventQueue, mpe_Bool);
static char* checkState(int);
static char* decodeEvent(mpe_Event);
static mpe_Error checkPreviousQueues(char*, int);

/***************************************************************************
 *
 *   test_mediaTunerTest
 *
 ***************************************************************************/
/*
 * This is a test untility to look at tuning operation at the mpeos layer
 *
 */
static mpe_Error test_mediaTunerTest(mpe_MediaTuneRequestParams* requestParams,
        char* errorBuffer)
{
    mpe_Error error = MPE_SUCCESS;
    int queueId = 1;
    int *ACT = NULL;
    mpe_Event eventId;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "-- Starting test_mediaTunerTest...\n");
    ACT = &queueId;

    //Setup the queue
    error = eventQueueNew(&queue, "TestMediaInit");
    if (error != MPE_SUCCESS)
    {
        sprintf(errorBuffer, "Unable to create tuner event queue, Error = %s",
                decodeError(error));
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, errorBuffer);
        return error;
    }

    // Do the tune
    error = mediaTune(requestParams, queue, ACT);
    if (error != MPE_SUCCESS)
    {
        sprintf(errorBuffer,
                "FAILED: Tune request retuns a failure Received: %d \n", error);
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, errorBuffer);
        return error;
    }

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "Tune Request complete, waiting for events\n");

    // Get back the events untill timeout occurs
    error = eventQueueWaitNext(queue, &eventId, NULL, (void**) &ACT, NULL,
            TEST_DEFAULT_TIMEOUT);
    while (error == MPE_SUCCESS)
    {
        // Print out events received	
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "-- Starting test_mediaTunerTest...\n");
        switch ((int) eventId)
        {
        case MPE_TUNE_STARTED:
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                    "Received MPE_TUNE_STARTED event \n");
            break;
        case MPE_TUNE_SYNC:
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Received MPE_TUNE_SYNC \n");
            break;
        case MPE_TUNE_FAIL:
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Received MPE_TUNE_FAIL \n");
            break;
        case MPE_TUNE_ABORT:
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Received MPE_TUNE_ABORT \n");
            break;
        default:
            sprintf(errorBuffer, "FAILED: Unknown event received, %d  \n",
                    eventId);
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST, errorBuffer);
            error = MPE_EINVAL;
            return error;
            break;
        }

        // Wait for the next event
        error = eventQueueWaitNext(queue, &eventId, NULL, (void**) &ACT, NULL,
                TEST_DEFAULT_TIMEOUT);
    }
    switch (error)
    {
    case MPE_ETIMEOUT:
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "No more events generated within specified time \n");
        break;
    default:
        sprintf(errorBuffer,
                "FAILED: Error in waiting for events Returned:%d \n", error);
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, errorBuffer);
        return error;
        break;
    }

    if ((int) eventId != MPE_TUNE_SYNC)
    {
        sprintf(errorBuffer, "FAILED: Tune not synced \n");
        error = MPE_EINVAL;
    }
    else
    {
        error = MPE_SUCCESS;
    }

    return error;
}

/***************************************************************************
 *
 *   test_mediaDigitalTunerTest
 *
 ***************************************************************************/
/*
 * This is a test untility to check tuning by digital service
 *
 */
static void test_mediaDigitalTunerTest(CuTest* tc)
{
    mpe_Error error = MPE_SUCCESS;
    char errorBuffer[256];

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-- Starting test_mediaDigitalTunerTest...\n");

    // Setup the reqeuest
    requestParams.tunerId = TEST_DEFAULT_TUNER;
    requestParams.tuneParams.tuneType = MPE_MEDIA_TUNE_BY_TUNING_PARAMS;
    requestParams.tuneParams.frequency = TEST_FREQUENCY_DIGITAL_1;
    requestParams.tuneParams.programNumber = TEST_PROGRAM_NUMBER_DIGITAL_1;
    requestParams.tuneParams.qamMode = TEST_QAM_MODE_DIGITAL_1;

    error = test_mediaTunerTest(&requestParams, errorBuffer);
    if (error != MPE_SUCCESS)
    {
        CuFail(tc, errorBuffer);
    }

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-- Ending test_mediaDigitalTunerTest...\n");
}

/***************************************************************************
 *
 *   test_mediaDigitalTunerTest2
 *
 ***************************************************************************/
/*
 * This is a test untility to check tuning to another digital service
 *
 */
static void test_mediaDigitalTunerTest2(CuTest* tc)
{
    mpe_Error error = MPE_SUCCESS;
    char errorBuffer[256];

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-- Starting test_mediaDigitalTunerTest2...\n");

    // Setup the reqeuest
    requestParams.tunerId = TEST_DEFAULT_TUNER;
    requestParams.tuneParams.tuneType = MPE_MEDIA_TUNE_BY_TUNING_PARAMS;
    requestParams.tuneParams.frequency = TEST_FREQUENCY_DIGITAL_2;
    requestParams.tuneParams.programNumber = TEST_PROGRAM_NUMBER_DIGITAL_2;
    requestParams.tuneParams.qamMode = TEST_QAM_MODE_DIGITAL_2;

    error = test_mediaTunerTest(&requestParams, errorBuffer);
    if (error != MPE_SUCCESS)
    {
        CuFail(tc, errorBuffer);
    }

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-- Ending test_mediaDigitalTunerTest2...\n");
}

/***************************************************************************
 *
 *   test_mediaAnalogTunerTest
 *
 ***************************************************************************/
/*
 * This is a test untility to check tuning by analog service
 *
 */
static void test_mediaAnalogTunerTest(CuTest* tc)
{
    mpe_Error error = MPE_SUCCESS;
    char errorBuffer[256];

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-- Starting test_mediaAnalogTunerTest...\n");

    // Setup the reqeuest
    requestParams.tunerId = TEST_DEFAULT_TUNER;
    requestParams.tuneParams.tuneType = MPE_MEDIA_TUNE_BY_TUNING_PARAMS;
    requestParams.tuneParams.frequency = TEST_FREQUENCY_ANALOG;
    requestParams.tuneParams.programNumber = TEST_PROGRAM_NUMBER_ANALOG;
    requestParams.tuneParams.qamMode = TEST_QAM_MODE_ANALOG;

    error = test_mediaTunerTest(&requestParams, errorBuffer);
    if (error != MPE_SUCCESS)
    {
        CuFail(tc, errorBuffer);
    }

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-- Ending test_mediaAnalogTunerTest...\n");
}

/****************************************************************************
 *
 *  test_mediaTuneByTuneParamsSuccess()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_mediaTune" function by tuning by sourceID
 * and expecting successful results.
 *
 * \api mpeos_mediaTune()
 *
 * \strategy Call the "mpeos_mediaTune()" function and checks for reasonable
 * return values.
 *
 * \assets none
 *
 */
static void test_mediaTuneByTuningParamsSuccess1(CuTest* tc)
{
    uint32_t
            frequencies[] =
            { TEST_FREQUENCY_ANALOG, TEST_FREQUENCY_DIGITAL_1,
                    TEST_FREQUENCY_DIGITAL_2 };
    uint32_t programNumbers[] =
    { TEST_PROGRAM_NUMBER_ANALOG, TEST_PROGRAM_NUMBER_DIGITAL_1,
            TEST_PROGRAM_NUMBER_DIGITAL_2 };
    uint32_t qamModes[] =
    { TEST_QAM_MODE_ANALOG, TEST_QAM_MODE_DIGITAL_1, TEST_QAM_MODE_DIGITAL_2 };
    int i, queueId141516 = 14; // Yes, it's a silly name, but this queueId will refer to queues 14, 15, and 16
    mpe_Error error = MPE_SUCCESS;
    char errorBuffer[256];

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-- Starting test_mediaTuneByTuningParamsSuccess1...\n");

    if (!inited)
        if (init(errorBuffer) != MPE_SUCCESS)
        {
            CuFail(tc, errorBuffer);
            return;
        }

    requestParams.tunerId = TEST_DEFAULT_TUNER;
    requestParams.tuneParams.tuneType = MPE_MEDIA_TUNE_BY_TUNING_PARAMS;

    // Loop through the array of sourceIds to test a variety of values (digital and analog)
    for (i = 1; i < 3; i++)
    {
        queues[queueId141516].state = STATE_READY;

        // Set the sourceId in our tune request
        requestParams.tuneParams.frequency = frequencies[i];
        requestParams.tuneParams.programNumber = programNumbers[i];
        requestParams.tuneParams.qamMode = qamModes[i];

        // Attempt to tune using a valid sourceID
        error = test_mediaTunerTuneAndProcessEvents(errorBuffer,
                &requestParams, &(queues[queueId141516].index));
        if (error != MPE_SUCCESS)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, errorBuffer);
            CuFail(tc, errorBuffer);
            return;
        }
        queueId141516++;
    }

    error = checkPreviousQueues(errorBuffer, queueId141516);
    if (error != MPE_SUCCESS)
    {
        TRACE(MPE_LOG_ERROR, MPE_MOD_TEST, errorBuffer);
        CuFail(tc, errorBuffer);
        return;
    }

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-- Finished test_mediaTuneByTuningParamsSuccess1!\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-----------------------------------------------------\n\n");

    return;
}

static void test_mediaTuneByTuningParamsSuccess2(CuTest* tc)
{
    mpe_MediaTuneRequestParams requestParams2;
    mpe_Error error;
    int queueId17 = 17, queueId18 = 18;
    char errorBuffer[256];

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-- Starting test_mediaTuneByTuningParamsSuccess2...\n");

    if (!inited)
        if (init(errorBuffer) != MPE_SUCCESS)
        {
            CuFail(tc, errorBuffer);
            return;
        }

    queues[queueId17].state = STATE_READY;
    queues[queueId18].state = STATE_READY;

    // This final test will involve a tune abort, followed by a tune sync
    // Create two tuner queues for the last test

    requestParams.tunerId = TEST_DEFAULT_TUNER;
    requestParams.tuneParams.tuneType = MPE_MEDIA_TUNE_BY_TUNING_PARAMS;
    requestParams.tuneParams.frequency = TEST_FREQUENCY_DIGITAL_1;
    requestParams.tuneParams.programNumber = TEST_PROGRAM_NUMBER_DIGITAL_1;
    requestParams.tuneParams.qamMode = TEST_QAM_MODE_DIGITAL_1;

    requestParams2.tunerId = TEST_DEFAULT_TUNER;
    requestParams2.tuneParams.frequency = TEST_FREQUENCY_DIGITAL_2;
    requestParams2.tuneParams.programNumber = TEST_PROGRAM_NUMBER_DIGITAL_2;
    requestParams2.tuneParams.qamMode = TEST_QAM_MODE_DIGITAL_2;
    requestParams2.tuneParams.tuneType = MPE_MEDIA_TUNE_BY_TUNING_PARAMS;

    // Next step is to send two tune requests to the same tuner.
    // This will cause an abort on the first request.

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Sending first tune request...\n");
    // Next step is to send two tune requests to the same tuner.
    // This will cause an abort on the first request.
    error = mediaTune(&requestParams, queue, &(queues[queueId17].index));
    if (MPE_SUCCESS != error)
    {
        sprintf(errorBuffer, "  Attempt to tune failed!!! Error = %s",
                decodeError(error));
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, errorBuffer);
        CuFail(tc, errorBuffer);
        return;
    }
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--   First tune request successfully sent!\n");
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Sending second tune request...\n");
    error = mediaTune(&requestParams2, queue, &(queues[queueId18].index));
    if (MPE_SUCCESS != error)
    {
        sprintf(errorBuffer,
                "  Attempt to tune a second time failed!!! Error = %s",
                decodeError(error));
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, errorBuffer);
        CuFail(tc, errorBuffer);
        return;
    }
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--   Second tune request successfully sent!\n");

    // Process all the events and check for invalid state transitions
    error = processEvents(errorBuffer, queue, FALSE);
    if (error != MPE_SUCCESS)
    {
        sprintf(errorBuffer, "  Error processing events!!! Error = %s",
                errorBuffer);
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, errorBuffer);
        CuFail(tc, errorBuffer);
        return;
    }

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Starting event testing...\n");

    // Testing the first queue
    // Queue 6 should be shutdown
    if (queues[queueId17].state != STATE_SHUTDOWN)
    {
        sprintf(errorBuffer,
                "  Queue %i is not in STATE_SHUTDOWN, instead in %s",
                queueId17, checkState(queues[queueId17].state));
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, errorBuffer);
        CuFail(tc, errorBuffer);
        return;
    }

    // Testing the second queue
    // Queue 7 should be completed
    if (queues[queueId18].state != STATE_COMPLETED)
    {
        sprintf(errorBuffer,
                "  Queue %i is not in STATE_COMPLETED, instead in %s",
                queueId18, checkState(queues[queueId18].state));
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, errorBuffer);
        CuFail(tc, errorBuffer);
        return;
    }
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Event testing complete!\n");

    error = checkPreviousQueues(errorBuffer, queueId18);
    if (error != MPE_SUCCESS)
    {
        TRACE(MPE_LOG_ERROR, MPE_MOD_TEST, errorBuffer);
        CuFail(tc, errorBuffer);
        return;
    }

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-- Finished test_mediaTuneByTuningParamsSuccess2!\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-----------------------------------------------------\n\n");

    return;
}

/****************************************************************************
 *
 *  test_mediaTuneByTuningParamsFailure()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_mediaTune" function by tuning by tuning
 * params and expecting failures.
 *
 * \api mpeos_mediaTune()
 *
 * \strategy Call the "mpeos_mediaTune()" function and checks for failed
 * return values. Will test invalid values as parameters to the functions and 
 * then values that are valid, but cause a tune failure (e.g. a sourceId that
 * doesn't map to a service, but *could* be a valid sourceId)
 *
 * \assets none
 *
 */
static void test_mediaTuneByTuningParamsFailure1(CuTest* tc)
{
    mpe_Error error;
    int queueId19 = 19, queueId20 = 20, queueId21 = 21, queueId22 = 22,
            queueId23 = 23;
    char errorBuffer[256];

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-- Starting test_mediaTuneByTuningParamsFailure1...\n");

    if (!inited)
        if (init(errorBuffer) != MPE_SUCCESS)
        {
            CuFail(tc, errorBuffer);
            return;
        }

    queues[queueId19].state = STATE_SHUTDOWN;
    queues[queueId20].state = STATE_SHUTDOWN;
    queues[queueId21].state = STATE_SHUTDOWN;
    queues[queueId22].state = STATE_SHUTDOWN;
    queues[queueId23].state = STATE_SHUTDOWN;

    requestParams.tuneParams.tuneType = MPE_MEDIA_TUNE_BY_TUNING_PARAMS;

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--   Testing with an invalid qamMode...\n");
    // Attempt to tune using an invalid qamMode
    requestParams.tunerId = TEST_DEFAULT_TUNER;
    requestParams.tuneParams.frequency = TEST_FREQUENCY_DIGITAL_1;
    requestParams.tuneParams.programNumber = TEST_PROGRAM_NUMBER_DIGITAL_1;
    requestParams.tuneParams.qamMode = 1600;
    error = mediaTune(&requestParams, queue, &(queues[queueId22].index));
    if (MPE_EINVAL != error)
    {
        sprintf(
                errorBuffer,
                "  Should've received MPE_EINVAL error, but instead received error code 0x%x",
                (int) error);
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, errorBuffer);
        CuFail(tc, errorBuffer);
        return;
    }
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Invalid qamMode test complete!\n");

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--   Testing with an invalid tuneType...\n");
    // Attempt to tune using an invalid tuneType in the TuneParams
    requestParams.tunerId = TEST_DEFAULT_TUNER;
    requestParams.tuneParams.tuneType = 3;
    requestParams.tuneParams.frequency = TEST_FREQUENCY_DIGITAL_1;
    requestParams.tuneParams.programNumber = TEST_PROGRAM_NUMBER_DIGITAL_1;
    requestParams.tuneParams.qamMode = TEST_QAM_MODE_DIGITAL_1;
    error = mediaTune(&requestParams, queue, &(queues[queueId23].index));
    if (MPE_EINVAL != error)
    {
        sprintf(
                errorBuffer,
                "  Should've received MPE_EINVAL error, but instead received error code 0x%x",
                (int) error);
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, errorBuffer);
        CuFail(tc, errorBuffer);
        return;
    }
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Invalid tuneType test complete!\n");

    error = checkPreviousQueues(errorBuffer, queueId23);
    if (error != MPE_SUCCESS)
    {
        TRACE(MPE_LOG_ERROR, MPE_MOD_TEST, errorBuffer);
        CuFail(tc, errorBuffer);
        return;
    }

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-- Finished test_mediaTuneByTuningParamsFailure1!\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-----------------------------------------------------\n\n");

    return;
}

static void test_mediaTuneByTuningParamsFailure2(CuTest* tc)
{
    mpe_Error error;
    int queueId24 = 24, queueId25 = 25, queueId26 = 26;
    char errorBuffer[256], anotherBuffer[256];

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-- Starting test_mediaTuneByTuningParamsFailure2...\n");

    if (!inited)
        if (init(errorBuffer) != MPE_SUCCESS)
        {
            CuFail(tc, errorBuffer);
            return;
        }

    queues[queueId24].state = STATE_READY;
    queues[queueId25].state = STATE_READY;
    queues[queueId26].state = STATE_READY;

    // Now we're going to attempt a tune using a potentially valid frequency,
    // but this frequency is not being broadcast.
    requestParams.tunerId = TEST_DEFAULT_TUNER;
    requestParams.tuneParams.tuneType = MPE_MEDIA_TUNE_BY_TUNING_PARAMS;
    requestParams.tuneParams.frequency = 55000000;
    requestParams.tuneParams.programNumber = TEST_PROGRAM_NUMBER_DIGITAL_1;
    requestParams.tuneParams.qamMode = TEST_QAM_MODE_DIGITAL_1;

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--   Starting test with invalid frequency\n");
    // There are two possible valid results:
    // 1) The tune proceeds with invalid information and subsequently fails asynchronously
    // 2) The tune fails immediately because the system recognizes the data as invalid
    error = mediaTune(&requestParams, queue, &(queues[queueId24].index));
    if (MPE_SUCCESS == error)
    {
        error = processEvents(anotherBuffer, queue, FALSE);
        if (error != MPE_SUCCESS)
        {
            sprintf(errorBuffer, "  Error processing events!!! Error = %s",
                    anotherBuffer);
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, errorBuffer);
            queues[queueId24].state = STATE_SHUTDOWN;
            queues[queueId25].state = STATE_SHUTDOWN;
            queues[queueId26].state = STATE_SHUTDOWN;
            CuFail(tc, errorBuffer);
            return;
        }
        if (queues[queueId24].state != STATE_SHUTDOWN)
        {
            sprintf(errorBuffer,
                    "  Queue %i is not in STATE_SHUTDOWN, instead in %s",
                    queueId24, checkState(queues[queueId24].state));
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, errorBuffer);
            queues[queueId24].state = STATE_SHUTDOWN;
            queues[queueId25].state = STATE_SHUTDOWN;
            queues[queueId26].state = STATE_SHUTDOWN;
            CuFail(tc, errorBuffer);
            return;
        }
    }
    else
    {
        queues[queueId24].state = STATE_SHUTDOWN;
        queues[queueId25].state = STATE_SHUTDOWN;
        queues[queueId26].state = STATE_SHUTDOWN;

        if (MPE_EINVAL != error)
        {
            sprintf(
                    errorBuffer,
                    "  Should've received MPE_EINVAL error with invalid frequency, but instead received error code %s",
                    decodeError(error));
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, errorBuffer);
            CuFail(tc, errorBuffer);
            return;
        }
    }

    //-------------------------------------------------------------------------

    //-------------------------------------------------------------------------
    // Now we're going to attempt a tune using a potentially valid programNumber,
    // but there are not services on this programNumber.
    requestParams.tunerId = TEST_DEFAULT_TUNER;
    requestParams.tuneParams.tuneType = MPE_MEDIA_TUNE_BY_TUNING_PARAMS;
    requestParams.tuneParams.frequency = TEST_FREQUENCY_DIGITAL_1;
    requestParams.tuneParams.programNumber = 20;
    requestParams.tuneParams.qamMode = TEST_QAM_MODE_DIGITAL_1;

    // There are two possible valid results:
    // 1) The tune proceeds with invalid information and subsequently fails asynchronously
    // 2) The tune fails immediately because the system recognizes the data as invalid
    error = mediaTune(&requestParams, queue, &(queues[queueId25].index));
    if (MPE_SUCCESS == error)
    {
        error = processEvents(anotherBuffer, queue, FALSE);
        if (error != MPE_SUCCESS)
        {
            sprintf(errorBuffer, "  Error processing events!!! Error = %s",
                    anotherBuffer);
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, errorBuffer);
            queues[queueId25].state = STATE_SHUTDOWN;
            queues[queueId26].state = STATE_SHUTDOWN;
            CuFail(tc, errorBuffer);
            return;
        }
        if (queues[queueId25].state != STATE_SHUTDOWN)
        {
            sprintf(errorBuffer,
                    "  Queue %i is not in STATE_SHUTDOWN, instead in %s",
                    queueId25, checkState(queues[queueId25].state));
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, errorBuffer);
            queues[queueId25].state = STATE_SHUTDOWN;
            queues[queueId26].state = STATE_SHUTDOWN;
            CuFail(tc, errorBuffer);
            return;
        }
    }
    else
    {
        queues[queueId25].state = STATE_SHUTDOWN;
        queues[queueId26].state = STATE_SHUTDOWN;

        if (MPE_EINVAL != error)
        {
            sprintf(
                    errorBuffer,
                    "  Should've received MPE_EINVAL error with invalid frequency, but instead received error code %s",
                    decodeError(error));
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, errorBuffer);
            CuFail(tc, errorBuffer);
            return;
        }
    }

    //-------------------------------------------------------------------------

    //-------------------------------------------------------------------------
    // Now we're going to attempt a tune using a potentially valid qamMode,
    // but this qamMode incorrect for this frequency.
    requestParams.tunerId = TEST_DEFAULT_TUNER;
    requestParams.tuneParams.tuneType = MPE_MEDIA_TUNE_BY_TUNING_PARAMS;
    requestParams.tuneParams.frequency = TEST_FREQUENCY_DIGITAL_1;
    requestParams.tuneParams.programNumber = TEST_PROGRAM_NUMBER_DIGITAL_1;
    requestParams.tuneParams.qamMode = MPE_SI_MODULATION_QPSK;

    // There are two possible valid results:
    // 1) The tune proceeds with invalid information and subsequently fails asynchronously
    // 2) The tune fails immediately because the system recognizes the data as invalid
    error = mediaTune(&requestParams, queue, &(queues[queueId26].index));
    if (MPE_SUCCESS == error)
    {
        error = processEvents(anotherBuffer, queue, FALSE);
        if (error != MPE_SUCCESS)
        {
            sprintf(errorBuffer, "  Error processing events!!! Error = %s",
                    anotherBuffer);
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, errorBuffer);
            CuFail(tc, errorBuffer);
            return;
        }
        if (queues[queueId26].state != STATE_SHUTDOWN)
        {
            sprintf(errorBuffer,
                    "  Queue %i is not in STATE_SHUTDOWN, instead in %s",
                    queueId26, checkState(queues[queueId26].state));
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, errorBuffer);
            queues[queueId26].state = STATE_SHUTDOWN;
            CuFail(tc, errorBuffer);
            return;
        }
    }
    else
    {
        queues[queueId26].state = STATE_SHUTDOWN;

        if (MPE_EINVAL != error)
        {
            sprintf(
                    errorBuffer,
                    "  Should've received MPE_EINVAL error with invalid frequency, but instead received error code %s",
                    decodeError(error));
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, errorBuffer);
            CuFail(tc, errorBuffer);
            return;
        }
    }

    error = checkPreviousQueues(errorBuffer, queueId26);
    if (error != MPE_SUCCESS)
    {
        TRACE(MPE_LOG_ERROR, MPE_MOD_TEST, errorBuffer);
        CuFail(tc, errorBuffer);
        return;
    }

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-- Finished test_mediaTuneByTuningParamsFailure2!\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-----------------------------------------------------\n\n");

    return;
}

static void test_mediaTuneByTuningParamsFailure3(CuTest* tc)
{
    mpe_MediaTuneRequestParams requestParams2;
    mpe_Error error, error2;
    int queueId27 = 27, queueId28 = 28;
    char errorBuffer[256], anotherBuffer[256];

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-- Starting test_mediaTuneByTuningParamsFailure3...\n");

    if (!inited)
        if (init(errorBuffer) != MPE_SUCCESS)
        {
            CuFail(tc, errorBuffer);
            return;
        }

    queues[queueId27].state = STATE_READY;
    queues[queueId28].state = STATE_READY;

    // This last test will involve a tune abort, followed by a tune failure
    requestParams.tunerId = TEST_DEFAULT_TUNER;
    requestParams.tuneParams.tuneType = MPE_MEDIA_TUNE_BY_TUNING_PARAMS;
    requestParams.tuneParams.frequency = TEST_FREQUENCY_DIGITAL_1;
    requestParams.tuneParams.programNumber = TEST_PROGRAM_NUMBER_DIGITAL_1;
    requestParams.tuneParams.qamMode = TEST_QAM_MODE_DIGITAL_1;

    requestParams2.tunerId = TEST_DEFAULT_TUNER;
    requestParams2.tuneParams.tuneType = MPE_MEDIA_TUNE_BY_TUNING_PARAMS;
    requestParams2.tuneParams.frequency = 615000000;
    requestParams2.tuneParams.programNumber = TEST_PROGRAM_NUMBER_DIGITAL_1;
    requestParams2.tuneParams.qamMode = TEST_QAM_MODE_DIGITAL_1;

    error = mediaTune(&requestParams, queue, &(queues[queueId27].index));
    if (MPE_SUCCESS != error)
    {
        sprintf(errorBuffer, "  Attempt to tune failed!!! Error = %s",
                decodeError(error));
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, errorBuffer);
        queues[queueId27].state = STATE_SHUTDOWN;
        queues[queueId28].state = STATE_SHUTDOWN;
        CuFail(tc, errorBuffer);
        return;
    }
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--   First tune request successfully sent!\n");
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Sending second tune request...\n");
    error2 = mediaTune(&requestParams2, queue, &(queues[queueId28].index));
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--   Second tune request successfully sent!\n");

    // Process all the events and check for invalid state transitions
    error = processEvents(anotherBuffer, queue, FALSE);
    if (error != MPE_SUCCESS)
    {
        sprintf(errorBuffer, "  Error processing events!!! Error = %s",
                anotherBuffer);
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, errorBuffer);
        queues[queueId27].state = STATE_SHUTDOWN;
        queues[queueId28].state = STATE_SHUTDOWN;
        CuFail(tc, errorBuffer);
        return;
    }

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Starting event testing...\n");

    // Testing the first queue
    // Queue 27 should be shutdown
    if (queues[queueId27].state != STATE_SHUTDOWN)
    {
        sprintf(errorBuffer,
                "  Queue %i is not in STATE_SHUTDOWN, instead in %s",
                queueId27, checkState(queues[queueId27].state));
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, errorBuffer);
        queues[queueId27].state = STATE_SHUTDOWN;
        queues[queueId28].state = STATE_SHUTDOWN;
        CuFail(tc, errorBuffer);
        return;
    }

    // Testing the second queue
    if (error2 == MPE_SUCCESS)
    {
        if (queues[queueId28].state != STATE_SHUTDOWN)
        {
            sprintf(errorBuffer,
                    "  Queue %i is not in STATE_SHUTDOWN, instead in %s",
                    queueId28, checkState(queues[queueId28].state));
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, errorBuffer);
            queues[queueId28].state = STATE_SHUTDOWN;
            CuFail(tc, errorBuffer);
            return;
        }
    }
    else
    {
        queues[queueId28].state = STATE_SHUTDOWN;

        if (error2 != MPE_EINVAL)
        {
            sprintf(
                    errorBuffer,
                    "  Should've received MPE_EINVAL error with invalid sourceId, but instead received error code %s",
                    decodeError(error));
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, errorBuffer);
            CuFail(tc, errorBuffer);
            return;
        }
    }

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--   Event testing complete!\n");

    error = checkPreviousQueues(errorBuffer, queueId28);
    if (error != MPE_SUCCESS)
    {
        TRACE(MPE_LOG_ERROR, MPE_MOD_TEST, errorBuffer);
        CuFail(tc, errorBuffer);
        return;
    }

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-- Finished test_mediaTuneByTuningParamsFailure3!\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-----------------------------------------------------\n\n");

    return;
}

/**
 * Will return the suite information describing the tuner tests.
 *
 * @return a CuSuite* that describes the suite for tuner tests.
 */
CuSuite* getTestSuite_tuner(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, test_mediaAnalogTunerTest);
    SUITE_ADD_TEST(suite, test_mediaDigitalTunerTest);
    SUITE_ADD_TEST(suite, test_mediaDigitalTunerTest2);

    SUITE_ADD_TEST(suite, test_mediaTuneByTuningParamsSuccess1);
    SUITE_ADD_TEST(suite, test_mediaTuneByTuningParamsSuccess2);
    SUITE_ADD_TEST(suite, test_mediaTuneByTuningParamsFailure1);
    SUITE_ADD_TEST(suite, test_mediaTuneByTuningParamsFailure2);
    SUITE_ADD_TEST(suite, test_mediaTuneByTuningParamsFailure3);

    return suite;
}

/**
 * Simple utility function to perform a successful tune and validate the
 * state machine transitions during tuning.
 */
mpe_Error test_mediaTunerTuneAndProcessEvents(char* errorBuffer,
        mpe_MediaTuneRequestParams* requestParams, int* ACT)
{
    mpe_Error error;
    char anotherBuffer[256];

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--     In test_mediaTunerTuneAndProcessEvents()...\n");

    // Attempt to tune using the requestParams
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--       Attempting to tune...\n");
    error = mediaTune(requestParams, queue, ACT);
    if (MPE_SUCCESS != error)
    {
        sprintf(errorBuffer, "  Attempt to tune failed!!! Error = %s",
                decodeError(error));
        return error;
    }

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--       Tune request successfully sent!\n");

    // now validate that the tune request proceeds through expected states
    error = processEvents(anotherBuffer, queue, TRUE);
    if (error != MPE_SUCCESS)
    {
        sprintf(errorBuffer, "  Error occured processing events!!! Error = %s",
                anotherBuffer);
        return error;
    }

    // make sure the tune completed successfully
    if (queues[*ACT].state != STATE_COMPLETED)
    {
        sprintf(errorBuffer,
                " Queue %i is not in STATE_COMPLETED, instead is in %s", *ACT,
                checkState(queues[*ACT].state));
        return MPE_EINVAL;
    }

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--     Returning from test_mediaTunerTuneAndProcessEvents()\n");

    return error;
}

/**
 * Simple utility function to perform a successful tune.
 */
mpe_Error test_mediaTunerDoSuccessfulTune(char* errorBuffer)
{
    mpe_Error error;
    mpe_MediaTuneRequestParams tuneRequest;
    mpe_EventQueue tunerQueue;

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--     In test_mediaTunerDoSuccessfulTune()...\n");

    // Set up our tune request object
    tuneRequest.tunerId = TEST_DEFAULT_TUNER;
    tuneRequest.tuneParams.tuneType = MPE_MEDIA_TUNE_BY_TUNING_PARAMS;
    tuneRequest.tuneParams.frequency = TEST_FREQUENCY_DIGITAL_1;
    tuneRequest.tuneParams.programNumber = TEST_PROGRAM_NUMBER_DIGITAL_1;
    tuneRequest.tuneParams.qamMode = TEST_QAM_MODE_DIGITAL_1;

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--       Attempting to tune...\n");
    // grab the tuner queue created in MediaTest_Init, this tuner queue
    // is monitored by the tuner thread
    tunerQueue = MediaTest_GetTunerQueue();
    if (tunerQueue == NULL)
    {
        sprintf(errorBuffer, "  Tuner Queue is NULL, cannot tune!");
        return MPE_EINVAL;
    }

    // Attempt to tune using the requestParams
    error = mediaTune(&tuneRequest, tunerQueue, 0);
    if (MPE_SUCCESS != error)
    {
        sprintf(errorBuffer, "  Tune request failed!!! Error = %s",
                decodeError(error));
        return error;
    }
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--       Tune request successfully sent!\n");

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "--       Waiting for tune completion\n");
    if (!MediaTest_TuneSucceeded())
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "    Tuning failed - TUNE_SYNC not received\n");
        return MPE_EINVAL;
    }

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "--     Tune Success: Returning from test_mediaTunerDoSuccessfulTune()\n");

    return error;
}

static mpe_Error processEvents(char* errorBuffer, mpe_EventQueue tunerQueue,
        mpe_Bool exitOnComplete)
{
    int* ACT = NULL;
    mpe_Error error, result = MPE_SUCCESS;
    mpe_Event eventId;
    int curState, colIndex;
    char logBuffer[256];

    error = eventQueueWaitNext(tunerQueue, &eventId, NULL, (void**) &ACT, NULL,
            TEST_DEFAULT_TIMEOUT);
    while (error != MPE_ETIMEOUT)
    {
        if (ACT != NULL)
        {
            sprintf(logBuffer, "--       ACT = %i, EventId = %s\n", *ACT,
                    decodeEvent(eventId));
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, logBuffer);

            curState = queues[*ACT].state;
            switch ((int) eventId)
            {
            case MPE_TUNE_STARTED:
                colIndex = 0;
                break;
            case MPE_TUNE_SYNC:
                colIndex = 1;
                break;
            case MPE_TUNE_FAIL:
                colIndex = 2;
                break;
            case MPE_TUNE_ABORT:
                colIndex = 3;
                break;
                /*				case MPE_EVENT_SHUTDOWN:
                 colIndex = 4;
                 break;*/
                // Right now any events not expected goes to invalid
                // This should be changed if watchdog events are added
            default:
                colIndex = 5;
                break;
            }

            queues[*ACT].state = validTrans[curState][colIndex];

            if (queues[*ACT].state == STATE_INVALID)
            {
                sprintf(
                        errorBuffer,
                        " Queue %i unexpectedly received event %s, moving from state %s to STATE_INVALID",
                        *ACT, decodeEvent(eventId), checkState(curState));
                result = MPE_EINVAL;
            }
            else
            {
                sprintf(
                        logBuffer,
                        "--       Queue %i received event %s, moving from state %s to %s\n",
                        *ACT, decodeEvent(eventId), checkState(curState),
                        checkState(queues[*ACT].state));
                TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, logBuffer);

            }

            if (queues[*ACT].state == STATE_COMPLETED && exitOnComplete)
            {
                sprintf(logBuffer,
                        "--       Exiting ProcessEvents after receiving TUNE_SYNC\n");
                TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, logBuffer);
                return result;
            }

        }
        else
        {
            sprintf(errorBuffer,
                    " ACT returned does not match valid values! EventId = %s",
                    decodeEvent(eventId));
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, errorBuffer);
            result = MPE_EINVAL;
        }

        error = eventQueueWaitNext(tunerQueue, &eventId, NULL, (void**) &ACT,
                NULL, TEST_DEFAULT_TIMEOUT);
    }

    return result;
}

static mpe_Error init(char* errorBuffer)
{
    int i;
    mpe_Error error = MPE_SUCCESS;

    for (i = 0; i < MAX_TUNER_QUEUES; i++)
    {
        queues[i].state = STATE_NOT_RUN;
        queues[i].index = i;
    }

    error = eventQueueNew(&queue, "TestMediaInit");
    if (error != MPE_SUCCESS)
        sprintf(errorBuffer, "Unable to create tuner event queue, Error = %s",
                decodeError(error));

    inited = TRUE;
    return error;
}

static char* checkState(int state)
{
    switch (state)
    {
    case STATE_NOT_RUN:
        return "STATE_NOT_RUN";
        break;
    case STATE_READY:
        return "STATE_READY";
    case STATE_STARTED:
        return "STATE_STARTED";
    case STATE_COMPLETED:
        return "STATE_COMPLETED";
    case STATE_FAILED:
        return "STATE_FAILED";
    case STATE_ABORTED:
        return "STATE_ABORTED";
    case STATE_SHUTDOWN:
        return "STATE_SHUTDOWN";
    case STATE_INVALID:
        return "STATE_INVALID";
    default:
        return "UNKNOWN VALUE";
    }
}

static char* decodeEvent(mpe_Event eventId)
{
    switch (eventId)
    {
    case MPE_TUNE_STARTED:
        return "MPE_TUNE_STARTED";
        break;
    case MPE_TUNE_FAIL:
        return "MPE_TUNE_FAIL";
        break;
    case MPE_TUNE_ABORT:
        return "MPE_TUNE_ABORT";
        break;
    case MPE_TUNE_SYNC:
        return "MPE_TUNE_SYNC";
        break;
        /*		case MPE_EVENT_SHUTDOWN:
         return "MPE_EVENT_SHUTDOWN"; break; */
    default:
        return "UNKNOWN VALUE";
        break;
    }
}

static mpe_Error checkPreviousQueues(char* buffer, int queueId)
{
    int i, state;
    mpe_Error result = MPE_SUCCESS;

    if (queueId > lastQueueId)
        lastQueueId = queueId;

    for (i = lastQueueId; i >= 0; i--)
    {
        state = queues[i].state;
        // A silly, inefficient state machine check to see that the queue ended in a valid state
        if (state != STATE_SHUTDOWN && state != STATE_NOT_RUN && state
                != STATE_INVALID && state != STATE_COMPLETED)
        {
            sprintf(
                    buffer,
                    " Queue %i is not in valid end state, instead is in %s. Moving to STATE_INVALID.",
                    i, checkState(state));
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, buffer);
            queues[i].state = STATE_INVALID;
            result = MPE_EINVAL;
        }
    }
    return result;
}

