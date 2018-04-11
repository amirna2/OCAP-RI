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

/* These should be in "test_media.h"  */

extern CuSuite* getTestSuite_mediaMisc(void);

/*
 * Prototypes:
 */
CuSuite* getTestSuite_tuner(void);
CuSuite* getTestSuite_decoder(void);
CuSuite* getTestSuite_audio(void);
CuSuite* getTestSuite_mediaMisc(void);
CuSuite* getTestSuite_mediaNeg(void);

#ifdef WIN32
const uint32_t g_ciDEFAULT_SOURCE = 0x44C;
#else
const uint32_t g_ciDEFAULT_SOURCE = 0x7d2;
#endif

/*
 * Globals:
 */
mpe_Cond g_tuningCond;
mpe_Cond g_tuneSuccess;
mpe_Cond g_tuneThreadExit;
mpe_Cond g_decoderCond;
mpe_Cond g_decodeSuccess;
mpe_Cond g_decodeThreadExit;

#define TUNE_TIMEOUT 5000
#define DECODE_TIMEOUT 5000
#define TEST_THREAD_EXIT 0x99999999

/*
 * Private
 */
static mpe_Bool m_isInit = false;
static int32_t m_sharedTunerData = 0;
static int32_t m_sharedDecoderData = 0;

//static mpe_MediaAsyncTunerStatus* m_pTunerStatus = NULL;
//static mpe_MediaDecoderStatus* m_pDecoderStatus = NULL;
static mpe_EventQueue m_tunerQueue = NULL;
static mpe_EventQueue m_decoderQueue = NULL;
static mpe_ThreadId m_decoderQueueThreadId = 0;
static mpe_ThreadId m_tunerQueueThreadId = 0;

void MediaTest_Init(void);
void MediaTest_Destroy(void);

static void MediaTest_TuningCallbackThread(void* data);
static void MediaTest_DecoderCallbackThread(void* data);

/*
 *  Media system inited flag. If true, indicates that 'mpeos_mediaInit()'
 *  has been called already.
 */

#ifdef TEST_MPEOS
static int g_mediaInited = FALSE; /*  initially indicate that 'mediaInit() has not been called */
#else
#if !defined(MPE_LOG_DISABLE)
static int g_mediaInited = TRUE; /*  if doing MPE testing, 'mediaInit() has already been called */
#endif
#endif /* TEST_MPEOS */

/**
 * Initializes anything that needs to be initialized before begining.
 */
void MediaTest_Init()
{
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "\nInto 'MediaTest_Init()', g_mediaInited == %d\n", g_mediaInited);

#ifdef TEST_MPEOS
    if (!g_mediaInited) /* has media subsystem been inited ? */
    {
        TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "  Calling 'mpeos_MediaInit()'\n" );
        (void)mpeos_mediaInit();

        // Init section filtering since it is required to tune
        // NOTE: This should not be done here! Media should not rely on section filtering!
        // Remove this when/if bug 3351 is resolved
        (void)mpeos_filterInit();

        g_mediaInited = TRUE;
    }
#endif /* TEST_MPEOS */

    if (true == m_isInit)
    {
        MediaTest_Destroy();
    }

    condNew(TRUE, FALSE, &g_tuningCond);
    condNew(TRUE, FALSE, &g_tuneSuccess);
    condNew(TRUE, FALSE, &g_tuneThreadExit);
    condNew(TRUE, FALSE, &g_decoderCond);
    condNew(TRUE, FALSE, &g_decodeSuccess);
    condNew(TRUE, FALSE, &g_decodeThreadExit);

    // Tuner related things.
    if (MPE_SUCCESS != eventQueueNew(&m_tunerQueue, "TestMediaTuner"))
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "  MediaTest_Init() Could not create a queue.\n");
        return;
    }

    // Thread for the tuner queue to run in.
    if (0 == m_tunerQueueThreadId)
    {
        if (MPE_SUCCESS != threadCreate(MediaTest_TuningCallbackThread,
                &m_sharedTunerData, MPE_THREAD_PRIOR_DFLT,
                MPE_THREAD_STACK_SIZE, &m_tunerQueueThreadId, "tuner"))
        {
            TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                    "  MediaTest_Init(): Could not create thread for decoder.\n");
            return;
        }
    }

    // Decoder related things.
    if (MPE_SUCCESS != eventQueueNew(&m_decoderQueue, "TestMediaDecoder"))
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "  MediaTest_Init(): Could not create new decoder queue.\n");
        return;
    }

    if (0 == m_decoderQueueThreadId)
    {
        if (MPE_SUCCESS != threadCreate(MediaTest_DecoderCallbackThread,
                &m_sharedDecoderData, MPE_THREAD_PRIOR_DFLT,
                MPE_THREAD_STACK_SIZE, &m_decoderQueueThreadId, "decoder"))
        {
            TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                    "  MediaTest_Init(): Could not create thread for decoder.\n");
            return;
        }
    }

    m_isInit = true;
    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "  'MediaTest_Init()' finished\n");
}

/**
 * Destroys anything that needed to be initialized.
 */
void MediaTest_Destroy()
{
    mpe_Error error;

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "Into MediaTest_Destroy()\n");

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
            "  MediaTest_Destroy(): ShutdownThread m_tunerQueueThreadId\n");
    if (MPE_SUCCESS != eventQueueSend(m_tunerQueue, TEST_THREAD_EXIT, NULL,
            NULL, 0))
    {
        TRACE(
                MPE_LOG_FATAL,
                MPE_MOD_TEST,
                "  MediaTest_Destroy(): Could not send TEST_THREAD_EXIT to tuner thread queue\n");
    }

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "    Waiting for TuningThread to exit....\n");
    // wait for the thread to run and exit
    error = condWaitFor(g_tuneThreadExit, TUNE_TIMEOUT);
    if (MPE_SUCCESS != error)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "    TuningCallbackThread failed to exit gracefully 0x%x\n",
                error);
    }
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    TuningThread has exited\n");

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
            "  MediaTest_Destroy(): ShutdownThread m_decoderQueueThreadId\n");
    if (MPE_SUCCESS != eventQueueSend(m_decoderQueue, TEST_THREAD_EXIT, NULL,
            NULL, 0))
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "  MediaTest_Destroy():  Could not send TEST_THREAD_EXIT to decoder queue\n");
    }

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "    Waiting for DecoderThread to exit....\n");
    // wait for the thread to run and exit
    error = condWaitFor(g_decodeThreadExit, TUNE_TIMEOUT);
    if (MPE_SUCCESS != error)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "    DecodeCallbackThread failed to exit gracefully 0x%x\n",
                error);
    }
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    DecoderThread has exited\n");

    m_isInit = false;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "  MediaTest_Destroy: finished.\n");
} // end MediaTest_Destroy()

/**
 */
mpe_Bool MediaTest_TuneSucceeded()
{
    mpe_Error error = condWaitFor(g_tuneSuccess, TUNE_TIMEOUT);
    if (MPE_SUCCESS != error)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "    Tuning failed - TUNE_SYNC not received 0x%x\n", error);
        return false;
    }

    return true;
}

/**
 */
mpe_Bool MediaTest_DecodeSucceeded()
{
    mpe_Error error = condWaitFor(g_decodeSuccess, DECODE_TIMEOUT);
    if (MPE_SUCCESS != error)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "    Decode failed - CONTENT_PRESENTING not received 0x%x\n",
                error);
        return false;
    }

    return true;
}

/**
 */
mpe_EventQueue MediaTest_GetTunerQueue()
{
    return m_tunerQueue;
}

/**
 */
mpe_EventQueue MediaTest_GetDecoderQueue()
{
    return m_decoderQueue;
}

/**
 * Callback for the tuning thread.
 * @param data The data passed to the thread.
 */
static void MediaTest_TuningCallbackThread(void* data)
{
    mpe_Event eventId;
    void* eventData;
    //   uint32_t*   sd = (uint32_t *)data;

    do
    {
        // wait forever until the next event
        if (MPE_SUCCESS != eventQueueWaitNext(m_tunerQueue, &eventId,
                &eventData, NULL, NULL, 0))
        {
            TRACE(
                    MPE_LOG_FATAL,
                    MPE_MOD_TEST,
                    "*****  MediaTest_TuningCallbackThread exiting: eventQueueWaitNext failed!  *****\n");
            return;
        }

        // If shutdown event comes thru, exit the thread
        if (eventId == TEST_THREAD_EXIT)
        {
            TRACE(
                    MPE_LOG_DEBUG,
                    MPE_MOD_TEST,
                    "*****  MediaTest_TuningCallbackThread: TEST_THREAD_EXIT event received.  *****\n");
            eventQueueDelete(m_tunerQueue);
            m_tunerQueue = NULL;
            m_tunerQueueThreadId = 0;
            condSet(g_tuneThreadExit);

            // exit thread
            return;
        }

        switch (eventId)
        {
        case MPE_TUNE_SYNC:
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "*****  TUNE_SYNC event received in TuningCallbackThread  *****\n");
            condSet(g_tuningCond);
            condSet(g_tuneSuccess);
        }
            break;
        case MPE_TUNE_FAIL:
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "*****  TUNE_FAIL event received in TuningCallbackThread  *****\n");
            condSet(g_tuningCond); // DEBUG
        }
            break;
        case MPE_TUNE_ABORT:
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "*****  TUNE_ABORT event received in TuningCallbackThread  *****\n");
            condSet(g_tuningCond); // DEBUG
        }
            break;
        case MPE_TUNE_STARTED:
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "*****  TUNE_STARTED event received in TuningCallbackThread  *****\n");
            condSet(g_tuningCond);
        }
            break;
            /*         case MPE_EVENT_SHUTDOWN:
             {
             TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
             "*****  EVENT_SHUTDOWN received in TuningCallbackThread  *****\n");
             condSet( g_tuningCond );
             }
             break; */
        default:
        {
            TRACE(
                    MPE_LOG_DEBUG,
                    MPE_MOD_TEST,
                    "*****  TuningCallbackThread: Unknown event [%d].  *****\n",
                    eventId);
            condSet(g_tuningCond);
        }
            break;
        } // switch

    } while (true);

} // end MediaTest_TuningCallbackThread(void*)


/**
 * Callback for the decoder thread.
 * @param data The data passed to the thread.
 */
static void MediaTest_DecoderCallbackThread(void* data)
{
    mpe_Event eventId;
    void* eventData;

    do
    {
        if (MPE_SUCCESS != eventQueueWaitNext(m_decoderQueue, &eventId,
                &eventData, NULL, NULL, 0))
        {
            TRACE(
                    MPE_LOG_FATAL,
                    MPE_MOD_TEST,
                    "*****  MediaTest_DecoderCallbackThread exiting: eventQueueWaitNext failed!  *****\n");
            return;
        }

        // If shutdown event comes thru, exit the thread
        if (eventId == TEST_THREAD_EXIT)
        {
            TRACE(
                    MPE_LOG_DEBUG,
                    MPE_MOD_TEST,
                    "*****  MediaTest_DecoderCallbackThread: TEST_THREAD_EXIT event received.  *****\n");
            eventQueueDelete(m_decoderQueue);
            m_decoderQueue = NULL;
            m_decoderQueueThreadId = 0;
            condSet(g_decodeThreadExit);

            // exit thread
            return;
        }

        switch (eventId)
        {
        case MPE_CONTENT_PRESENTING:
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "*****  CONTENT_PRESENTING event received in DecoderCallbackThread  *****\n");

            condSet(g_decoderCond);
            condSet(g_decodeSuccess);
        }
            break;
        case MPE_STILL_FRAME_DECODED:
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "*****  STILL_FRAME_DECODED event received in DecoderCallbackThread  *****\n");
            condSet(g_decoderCond);
        }
            break;
            /*       case MPE_EVENT_SHUTDOWN:
             {
             TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
             "*****  EVENT_SHUTDOWN received in DecoderCallbackThread  *****\n");
             condSet( g_decoderCond );
             }
             break; */
        default:
        {
            TRACE(
                    MPE_LOG_DEBUG,
                    MPE_MOD_TEST,
                    "*****  DecoderCallbackThread: Unknown event [%d]  *****\n",
                    eventId);
            condSet(g_decoderCond);
        }
            break;
        } // switch
    } while (true);
} // end MediaTest_DecoderCallbackThread(void*)


/****************************************************************************
 *
 *  test_mediaRunAllTests() - Run all the media tests
 *
 ***************************************************************************/

NATIVEEXPORT_API void test_mediaRunAllTests(void)
{
    CuSuite* suite = CuSuiteNew();
    CuSuite* tmpSuite;
    CuString *output;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n############\n#\n#  'test_mediaRunAllTests()' starting\n#\n");

    MediaTest_Init();

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "\n   Adding tests\n");

    tmpSuite = getTestSuite_tuner();
    CuSuiteAddSuite(suite, tmpSuite);
    CuSuiteFree(tmpSuite);

    tmpSuite = getTestSuite_decoder();
    CuSuiteAddSuite(suite, tmpSuite);
    CuSuiteFree(tmpSuite);

    tmpSuite = getTestSuite_mediaMisc();
    CuSuiteAddSuite(suite, tmpSuite);
    CuSuiteFree(tmpSuite);

    tmpSuite = getTestSuite_mediaNeg();
    CuSuiteAddSuite(suite, tmpSuite);
    CuSuiteFree(tmpSuite);

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "\n  Running tests . . .\n");

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_mediaRunAllTests() results :\n%s\n", output->buffer);

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "\n  freeing suite\n");
    CuSuiteFree(suite);

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "\n  freeing string\n");
    CuStringFree(output);

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "\n  calling MediaTest_Destroy()\n");
    MediaTest_Destroy();

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n#\n#'test_mediaRunAllTests()' - Tests complete\n#\n\n");
}

/****************************************************************************
 *
 *  test_mediaRunTunerTests() - Run the media tuner tests
 *
 ***************************************************************************/

NATIVEEXPORT_API void test_mediaRunTunerTests(void)
{
    CuSuite * suite;
    CuString * output;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n############\n#\n#  'test_mediaRunTunerTests()' starting\n#\n");

    MediaTest_Init();

    suite = CuSuiteNew();
    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "\n   Adding tests\n");

    CuSuiteAddSuite(suite, getTestSuite_tuner());

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "\n  Running tests . . .\n");

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_mediaRunTunerTests() results :\n%s\n", output->buffer);

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "\nt  freeing suite\n");
    CuSuiteFree(suite);

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "\n  freeing string\n");
    CuStringFree(output);

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "\n  calling MediaTest_Destroy()\n");
    MediaTest_Destroy();

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n#\n#'test_mediaRunTunerTests()' - Tests complete\n#\n\n");
}

/****************************************************************************
 *
 *  test_mediaRunDecoderTests() - Run the media decoder tests
 *
 ***************************************************************************/

NATIVEEXPORT_API void test_mediaRunDecoderTests(void)
{
    CuSuite * suite;
    CuString * output;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n############\n#\n#  'test_mediaRunDecoderTests()' starting\n#\n");

    MediaTest_Init();

    suite = CuSuiteNew();
    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "\n   Adding tests\n");

    CuSuiteAddSuite(suite, getTestSuite_decoder());

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "\n  Running tests . . .\n");

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_mediaRunDecoderTests() results :\n%s\n", output->buffer);

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "\n  freeing suite\n");
    CuSuiteFree(suite);

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "\n  freeing string\n");
    CuStringFree(output);

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "\n  calling MediaTest_Destroy()\n");
    MediaTest_Destroy();

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n#\n#'test_mediaRunDecoderTests()' - Tests complete\n#\n\n");
}

/****************************************************************************
 *
 *  test_mediaRunMiscTests() - Run misc media tests
 *
 ***************************************************************************/

NATIVEEXPORT_API void test_mediaRunMiscTests(void)
{
    CuSuite * suite;
    CuString * output;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n############\n#\n#  'test_mediaRunMiscTests()' starting\n#\n");

    MediaTest_Init();

    suite = CuSuiteNew();
    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "\n   Adding tests\n");

    CuSuiteAddSuite(suite, getTestSuite_mediaMisc());

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "  Running tests . . .\n");

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_mediaRunMiscTests() results :\n%s\n", output->buffer);

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "\n  freeing suite\n");
    CuSuiteFree(suite);

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "  freeing string\n");
    CuStringFree(output);

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "  calling MediaTest_Destroy()\n");
    MediaTest_Destroy();

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n#\n#  test_mediaRunMiscTests() - Tests complete\n#\n\n");
}

/****************************************************************************
 *
 *  test_mediaRunNegTests() - Run the media negative tests
 *
 ***************************************************************************/

NATIVEEXPORT_API void test_mediaRunNegTests(void)
{
    CuSuite * suite;
    CuString * output;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n############\n#\n#  'test_mediaRunNegTests()' starting\n#\n");

    MediaTest_Init();

    suite = CuSuiteNew();
    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "\n  Adding tests\n");

    CuSuiteAddSuite(suite, getTestSuite_mediaNeg());

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "\n  Running tests . . .\n");

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_mediaRunNegTests() results :\n%s\n", output->buffer);

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "\n  freeing suite\n");
    CuSuiteFree(suite);

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "\n  freeing string\n");
    CuStringFree(output);

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "\n  calling MediaTest_Destroy()\n");
    MediaTest_Destroy();

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n#\n#'test_mediaRunNegTests()' - Tests complete\n#\n\n");
}

NATIVEEXPORT_API void test_mediaRunDecoderOneTest()
{
    CuSuite * origSuite;
    CuSuite * cloneSuite;
    CuString * output;
    char msg[] = "Error: Could not create new suite for DecoderOne.";

    MediaTest_Init();

    origSuite = getTestSuite_decoder();
    cloneSuite = CuSuiteNewCloneTest(origSuite, "test_decoder_one");
    CuSuiteFree(origSuite);

    if (NULL == cloneSuite)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s", msg);
        vte_agent_Log(msg);
        return;
    }

    CuSuiteRun(cloneSuite);

    // Format output string.
    output = CuStringNew();
    CuSuiteSummary(cloneSuite, output);
    CuSuiteDetails(cloneSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_mediaRunDecoderOneTest\n%s\n",
            output->buffer);

    // Free the memory
    CuSuiteFree(cloneSuite);
    CuStringFree(output);

    MediaTest_Destroy();
}

NATIVEEXPORT_API void test_mediaRunDecoderTwoTest()
{
    CuSuite * origSuite;
    CuSuite * cloneSuite;
    CuString * output;
    char msg[] = "Error: Could not create new suite for DecoderTwo.";

    MediaTest_Init();

    origSuite = getTestSuite_decoder();
    cloneSuite = CuSuiteNewCloneTest(origSuite, "test_decoder_two");
    CuSuiteFree(origSuite);

    if (NULL == cloneSuite)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s", msg);
        vte_agent_Log(msg);
        return;
    }

    CuSuiteRun(cloneSuite);

    // Format output string.
    output = CuStringNew();
    CuSuiteSummary(cloneSuite, output);
    CuSuiteDetails(cloneSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_mediaRunDecoderTwoTest\n%s\n",
            output->buffer);

    // Free the memory
    CuSuiteFree(cloneSuite);
    CuStringFree(output);

    MediaTest_Destroy();
}

NATIVEEXPORT_API void test_mediaRunDecoderThreeTest()
{
    CuSuite * origSuite;
    CuSuite * cloneSuite;
    CuString * output;
    char msg[] = "Error: Could not create new suite for DecoderThree.";

    MediaTest_Init();

    origSuite = getTestSuite_decoder();
    cloneSuite = CuSuiteNewCloneTest(origSuite, "test_decoder_three");
    CuSuiteFree(origSuite);

    if (NULL == cloneSuite)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s", msg);
        vte_agent_Log(msg);
        return;
    }

    CuSuiteRun(cloneSuite);

    // Format output string.
    output = CuStringNew();
    CuSuiteSummary(cloneSuite, output);
    CuSuiteDetails(cloneSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_mediaRunDecoderThreeTest\n%s\n",
            output->buffer);

    // Free the memory
    CuSuiteFree(cloneSuite);
    CuStringFree(output);

    MediaTest_Destroy();
}

NATIVEEXPORT_API void test_mediaRunDecoderFourTest()
{
    CuSuite * origSuite;
    CuSuite * cloneSuite;
    CuString * output;
    char msg[] = "Error: Could not create new suite for DecoderFour.";

    MediaTest_Init();

    origSuite = getTestSuite_decoder();
    cloneSuite = CuSuiteNewCloneTest(origSuite, "test_decoder_four");
    CuSuiteFree(origSuite);

    if (NULL == cloneSuite)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s", msg);
        vte_agent_Log(msg);
        return;
    }

    CuSuiteRun(cloneSuite);

    // Format output string.
    output = CuStringNew();
    CuSuiteSummary(cloneSuite, output);
    CuSuiteDetails(cloneSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_mediaRunDecoderFourTest\n%s\n",
            output->buffer);

    // Free the memory
    CuSuiteFree(cloneSuite);
    CuStringFree(output);

    MediaTest_Destroy();
}

NATIVEEXPORT_API void test_mediaRunNegDecoderTest()
{
    CuSuite * origSuite;
    CuSuite * cloneSuite;
    CuString * output;
    char msg[] = "Error: Could not create new suite for MediaNegDecoder.";

    MediaTest_Init();

    origSuite = getTestSuite_mediaNeg();
    cloneSuite = CuSuiteNewCloneTest(origSuite, "test_media_neg_decoder");
    CuSuiteFree(origSuite);

    if (NULL == cloneSuite)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s", msg);
        vte_agent_Log(msg);
        return;
    }

    CuSuiteRun(cloneSuite);

    // Format output string.
    output = CuStringNew();
    CuSuiteSummary(cloneSuite, output);
    CuSuiteDetails(cloneSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_mediaRunNegDecoderTest\n%s\n",
            output->buffer);

    // Free the memory
    CuSuiteFree(cloneSuite);
    CuStringFree(output);

    MediaTest_Destroy();
}

NATIVEEXPORT_API void test_mediaRunNegTunerTest()
{
    CuSuite * origSuite;
    CuSuite * cloneSuite;
    CuString * output;
    char msg[] = "Error: Could not create new suite for MediaNegTuner.";

    MediaTest_Init();

    origSuite = getTestSuite_mediaNeg();
    cloneSuite = CuSuiteNewCloneTest(origSuite, "test_media_neg_tuner");
    CuSuiteFree(origSuite);

    if (NULL == cloneSuite)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s", msg);
        vte_agent_Log(msg);
        return;
    }

    CuSuiteRun(cloneSuite);

    // Format output string.
    output = CuStringNew();
    CuSuiteSummary(cloneSuite, output);
    CuSuiteDetails(cloneSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_mediaRunNegTunerTest\n%s\n",
            output->buffer);

    // Free the memory
    CuSuiteFree(cloneSuite);
    CuStringFree(output);

    MediaTest_Destroy();
}
