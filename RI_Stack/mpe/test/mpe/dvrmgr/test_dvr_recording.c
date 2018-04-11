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
// #include "test_dvr_include.h"


#define		WAIT_TIME1 (60*1000)	// 60secs in milli sec	
/*
 * Private functions for dvr recordings
 */
static void test_dvr_recording1(CuTest* tc);
static void test_dvr_recording2(CuTest* tc);
static void test_dvr_recording3(CuTest* tc);
static void test_dvr_recording4(CuTest* tc);
static void test_dvr_recording5(CuTest* tc);
static void test_dvr_recording6(CuTest* tc);
static void test_dvr_recording7(CuTest* tc);
static void test_dvr_recording8(CuTest* tc);

/* timer related functions */
static void timerThread(void* data);
static mpe_EventQueue m_timerQueue1 = NULL;
static mpe_ThreadId m_timerThreadId1 = 0;

mpe_EventQueue m_mediaQueue1 = NULL;
static int32_t m_sharedData1 = 0;
static mpe_ThreadId m_mediaThreadId1 = 0;

static mpe_EventQueue m_dvrQueue1 = NULL;
static mpe_EventQueue m_dvrQueue2 = NULL;
static mpe_ThreadId m_dvrThreadId1 = 0;
static mpe_ThreadId m_dvrThreadId2 = 0;

static mpe_Bool tuneSuccess = false;
static mpe_Cond dvr_test_cond;

char recName1[32];
char recName2[32];

//mpe_DvrRecording recording1;
//mpe_DvrRecording recording2;

mpe_DvrPlayback playback1;
mpe_DvrPlayback playback2;

mpe_MediaPID pids1[2] =
{
{ MPE_SI_ELEM_MPEG_2_VIDEO, 0x1E },
{ MPE_SI_ELEM_ATSC_AUDIO, 0x1F } };

CuSuite* getTestSuite_Recording1(void);
CuSuite* getTestSuite_Recording2(void);
CuSuite* getTestSuite_Recording3(void);
CuSuite* getTestSuite_Recording4(void);
CuSuite* getTestSuite_Recording5(void);
CuSuite* getTestSuite_Recording6(void);
CuSuite* getTestSuite_Recording7(void);
CuSuite* getTestSuite_Recording8(void);

static void tune(uint32_t freq, uint32_t prog, uint32_t qam, mpe_Bool decode,
        uint32_t tunerID);
static void mediaThread(void* data);
static void dvrThread1(void* data);
static void dvrThread2(void* data);

static void test_dvr_recording1(CuTest* tc)
{

#if 0
    mpe_Error res=MPE_SUCCESS;
    mpe_Error retCode = MPE_SUCCESS;
    mpe_Event event;
    void *poptional_eventdata = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_recording1() - Test case 1\n" );

    // Timer related things.
    if( MPE_SUCCESS != eventQueueNew( &m_mediaQueue1 ) )
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "Error: Could not create a queue.\n" );
        return;
    }

    // Thread for the media queue to run in.
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

    /* create timer Queue */
    if( MPE_SUCCESS != eventQueueNew( &m_timerQueue1 ) )
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "Error test_dvr_recording1() Could not create a queue.\n" );
        return;
    }

    if( 0 == m_timerThreadId1 )
    {
        if( MPE_SUCCESS != threadCreate( timerThread,
                        &m_sharedData1,
                        MPE_THREAD_PRIOR_DFLT,
                        MPE_THREAD_STACK_SIZE,
                        &m_timerThreadId1,
                        "timerThread") )
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "Error: Could not create tread\n" );
            return;
        }
    }

    if(m_dvrQueue1 == NULL)
    {
        // event queue
        if( MPE_SUCCESS != eventQueueNew( &m_dvrQueue1 ) )
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_recording1() Could not create a queue.\n" );
            return;
        }
    }

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
    if(condNew(TRUE, FALSE, &dvr_test_cond) != MPE_SI_SUCCESS)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "Error: Could not create condition obj\n" );
        return;
    }

    tune(TEST_FREQUENCY_ANALOG,TEST_PROGRAM_NUMBER_ANALOG,TEST_QAM_MODE_ANALOG, false,1); /* no media decoding */

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_recording1() calling Start Recording 1()\n" );

#if 0
    res = mpe_dvrRecordingStart( 1, //tuner ID
            0, //storage, 
            0, //bit_rate,
            NULL, //pids
            0, //pidCount,
            m_dvrQueue1, // event info
            NULL,
            &recording1, //recording handle 
            recName1 //recording name
    );

#endif
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_recording1() called mpe_dvrRecordingStart() - wait for %d seconds\n",WAIT_TIME1/1000 );
    retCode = eventQueueWaitNext(m_timerQueue1, &event, &poptional_eventdata, NULL, NULL,
            WAIT_TIME1);

    if(retCode == MPE_ETIMEOUT)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nTime up...\n");
        event = MPE_ETIMEOUT;

        retCode = eventQueueSend(m_timerQueue1, MPE_ETIMEOUT, (void *) NULL, (void *)NULL, 0);

        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nProblem sending MPE_ETIMEOUT event...\n\n");
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nSent MPE_ETIMEOUT event...\n\n");
        }
    }

#if 0
    res = mpe_dvrRecordingStop(recording1);
#endif

    // tune again using tuner 2
#ifdef POWERTV	
    tune(TEST_FREQUENCY_DIGITAL_1,TEST_PROGRAM_NUMBER_DIGITAL_1,TEST_QAM_MODE_DIGITAL_1, false,2); /* no media decoding */

#endif

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_recording1() Start Recording 2()\n" );

#if 0
    res = mpe_dvrRecordingStart( 2, //tuner ID
            0, //storage, 
            0, //bit_rate,
            NULL, //pids
            0, //pidCount,
            m_dvrQueue2, // event info
            NULL,
            &recording2, //recording handle 
            recName2 //recording name
    );

    retCode = eventQueueWaitNext(m_timerQueue1, &event, &poptional_eventdata, NULL, NULL,
            WAIT_TIME1);

    if(retCode == MPE_ETIMEOUT)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nTime up...\n");
        event = MPE_ETIMEOUT;

        retCode = eventQueueSend(m_timerQueue1, MPE_ETIMEOUT, (void *) NULL, (void *) NULL, 0);

        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nProblem sending MPE_ETIMEOUT event...\n\n");
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nSent MPE_ETIMEOUT event...\n\n");
        }
    }

    res = mpe_dvrRecordingStop(recording2);
#endif

#endif
}

static void test_dvr_recording2(CuTest* tc)
{
    mpe_Error res;
    mpe_Error retCode = MPE_SUCCESS;
    mpe_Event event;
    mpe_DvrString_t* recname = NULL;
    uint32_t count = 0;
    uint32_t i;
    float mode = 0.0;
    int64_t mediaTime;

    float actualMode;

    void *poptional_eventdata = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_recording2() - Test case 2\n");

    //	retCode = dvrGetRecordingList(0,&count, &recname);

    if (recname == NULL)
        return;

    /* create timer Queue */
    if (MPE_SUCCESS != eventQueueNew(&m_timerQueue1, "DVR Timer Queue"))
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "Error test_dvr_recording1() Could not create a queue.\n");
        return;
    }

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

    if (m_dvrQueue1 == NULL)
    {
        // event queue
        if (MPE_SUCCESS != eventQueueNew(&m_dvrQueue1, "DVR Event Queue"))
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "test_dvr_recording2() Could not create a queue.\n");
            return;
        }
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_recording2() calling mpe_dvrRecordingStart()\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_recording2() Normal rate 1.0 \n");
    /*    res = mpe_dvrRecordingPlayStart(  recname[0],
     0x0,      // video device
     NULL,
     0,        // pid count
     0,        // media Time
     1.0,
     m_dvrQueue1,      // event dispatcher
     NULL,
     &playback1 );
     */
    /* pause for 3 secs */
    /*   MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_recording2() Paused at startTime 0\n" );
     mode = 0.0;
     res = mpe_dvrSetTrickMode( playback1,
     mode, 
     &actualMode);

     */
    for (i = 0; i < 10; i++)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "test_dvr_recording2() Single step forward %d\n", i);
        //        mpe_dvrPlaybackGetTime( playback1, &mediaTime);
        //       mpe_dvrPlaybackSetTime( playback1, mediaTime+(80000000));

        /* wait 2 seconds */
        retCode = eventQueueWaitNext(m_timerQueue1, &event,
                &poptional_eventdata, NULL, NULL, 2000);
        if (retCode == MPE_ETIMEOUT)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "\nTime up...\n");
            event = MPE_ETIMEOUT;

            retCode = eventQueueSend(m_timerQueue1, MPE_ETIMEOUT,
                    (void *) NULL, (void *) NULL, 0);

            if (retCode != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                        "\nProblem sending MPE_ETIMEOUT event...\n\n");
            }
            else
            {
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                        "\nSent MPE_ETIMEOUT event...\n\n");
            }
        }
    }

    // dvrPlaybackSetTime( playback1, mediaTime+(8000UL*1000000UL));
    /* wait 2 seconds */
    retCode = eventQueueWaitNext(m_timerQueue1, &event, &poptional_eventdata,
            NULL, NULL, 2000);
    if (retCode == MPE_ETIMEOUT)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "\nTime up...\n");
        event = MPE_ETIMEOUT;

        retCode = eventQueueSend(m_timerQueue1, MPE_ETIMEOUT, (void *) NULL,
                (void *) NULL, 0);

        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "\nProblem sending MPE_ETIMEOUT event...\n\n");
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "\nSent MPE_ETIMEOUT event...\n\n");
        }
    }

    for (i = 0; i < 10; i++)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "test_dvr_recording2() Single step backward %d\n", i);
        //        mpe_dvrPlaybackGetTime( playback1, &mediaTime);
        //      mpe_dvrPlaybackSetTime( playback1, mediaTime-(80000000));

        /* wait 2 seconds */
        retCode = eventQueueWaitNext(m_timerQueue1, &event,
                &poptional_eventdata, NULL, NULL, 2000);
        if (retCode == MPE_ETIMEOUT)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "\nTime up...\n");
            event = MPE_ETIMEOUT;

            retCode = eventQueueSend(m_timerQueue1, MPE_ETIMEOUT,
                    (void *) NULL, (void *) NULL, 0);

            if (retCode != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                        "\nProblem sending MPE_ETIMEOUT event...\n\n");
            }
            else
            {
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                        "\nSent MPE_ETIMEOUT event...\n\n");
            }
        }
    }

#if 0

    /* jump 20s sec */
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_recording2() Jump forward 20 secs\n" );
    res = mpe_dvrPlaybackSetTime( playback1, (20000 * 1000000L));

    /* pause for 3 secs */
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_recording2() Pause for 3 seconds\n" );
    mode = 0.0;
    res = mpe_dvrSetTrickMode( playback1,
            mode,
            &actualMode);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_recording2() get the Media Time\n" );
    mpe_dvrPlaybackGetTime( playback1, &mediaTime);

    retCode = eventQueueWaitNext(m_timerQueue1, &event, &poptional_eventdata, NULL, NULL,
            3000);
    if(retCode == MPE_ETIMEOUT)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nTime up...\n");
        event = MPE_ETIMEOUT;

        retCode = eventQueueSend(m_timerQueue1, MPE_ETIMEOUT, (void *) NULL, (void *) NULL, 0);

        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nProblem sending MPE_ETIMEOUT event...\n\n");
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nSent MPE_ETIMEOUT event...\n\n");
        }
    }

    /* move forward by 1ms -*/
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_recording2() 50 Single steps forward\n" );

    for(i=1; i < 50; i++)
    {
        res = mpe_dvrPlaybackSetTime( playback1, i*(1 * 1000000L));
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_recording2() get the Media Time\n" );
    mpe_dvrPlaybackGetTime( playback1, &mediaTime);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_recording2() 50 Single steps reverse\n" );
    for(i=49; i > 0; i--)
    {
        res = mpe_dvrPlaybackSetTime( playback1, i*(1 * 1000000L));
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_recording2() get the Media Time\n" );
    mpe_dvrPlaybackGetTime( playback1, &mediaTime);

    /* jump to start */
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_recording2() Jump to start\n" );
    res = mpe_dvrPlaybackSetTime( playback1, 0 );

    /* pause for 10 secs */
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_recording2() Pause for 3 seconds\n" );
    mode = 0.0;
    res = mpe_dvrSetTrickMode( playback1,
            mode,
            &actualMode);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_recording2() - returned actual mode = %f\n",actualMode );

    retCode = eventQueueWaitNext(m_timerQueue1, &event, &poptional_eventdata, NULL, NULL,
            3000);

    if(retCode == MPE_ETIMEOUT)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nTime up...\n");
        event = MPE_ETIMEOUT;

        retCode = eventQueueSend(m_timerQueue1, MPE_ETIMEOUT, (void *) NULL, (void *) NULL, 0);

        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nProblem sending MPE_ETIMEOUT event...\n\n");
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nSent MPE_ETIMEOUT event...\n\n");
        }
    }

    /* fast forward for 10 seconds */
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_recording2() Fast forward 2 \n" );
    mode = 2;
    res = mpe_dvrSetTrickMode( playback1,
            mode,
            &actualMode);

    retCode = eventQueueWaitNext(m_timerQueue1, &event, &poptional_eventdata, NULL, NULL,
            10000);

    if(retCode == MPE_ETIMEOUT)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nTime up...\n");
        event = MPE_ETIMEOUT;

        retCode = eventQueueSend(m_timerQueue1, MPE_ETIMEOUT, (void *) NULL, (void *) NULL, 0);

        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nProblem sending MPE_ETIMEOUT event...\n\n");
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nSent MPE_ETIMEOUT event...\n\n");
        }
    }
    /* fast reverse */
    mode = -2.0;
    res = mpe_dvrSetTrickMode( playback1,
            mode,
            &actualMode);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_recording2() - returned actual mode = %f\n",actualMode );

    retCode = eventQueueWaitNext(m_timerQueue1, &event, &poptional_eventdata, NULL, NULL,
            10000);
    if(retCode == MPE_ETIMEOUT)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nTime up...\n");
        event = MPE_ETIMEOUT;

        retCode = eventQueueSend(m_timerQueue1, MPE_ETIMEOUT, (void *) NULL, (void *) NULL, 0);

        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nProblem sending MPE_ETIMEOUT event...\n\n");
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,"\nSent MPE_ETIMEOUT event...\n\n");
        }
    }
#endif

    //    mpe_dvrPlayBackStop(playback1);


    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_recording2() - End of test case 2\n");

}

static void test_dvr_recording3(CuTest* tc)
{
    uint32_t count;
    mpe_DvrString_t* recname = NULL;
    mpe_Error retCode = MPE_SUCCESS;
    uint32_t i;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_recording3() - Test case 3\n");

    //	retCode = mpe_dvrGetRecordingList(0,&count, &recname);

    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "test_dvr_recording3() ERROR cannot get recording list\n");
    }

    if (count != 0)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "test_dvr_recording3() Record Count is %d\n", count);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "test_dvr_recording3() --------- FILE LIST ---------\n");
        for (i = 0; i < count; i++)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "File Name : %s\n", recname[i]);
        }
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "test_dvr_recording3() -----------------------------\n");

        //        mpe_dvrFreeRecordingList(); 
    }

}

static void test_dvr_recording4(CuTest* tc)
{
    mpe_DvrString_t* recname = NULL;
    mpe_Error retCode = MPE_SUCCESS;
    uint32_t count;
    mpe_StorageHandle storageHandle = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_recording4() - Test case 4 START\n");

    //	retCode = dvrGetRecordingList(0,&count, &recname);

    if (count != 0)
    {
        uint64_t size, length;
        uint32_t value, i;
        mpe_MediaPID pids[5];

        // TODO: Init the storage handle before calling these methods
        //        retCode = dvrRecordingGet(storageHandle, recname[0],MPE_DVR_RECORDING_SIZE, &size);        
        //        retCode = dvrRecordingGet(storageHandle, recname[0],MPE_DVR_RECORDING_LENGTH, &length);
        //        retCode = dvrRecordingGet(storageHandle, recname[0],MPE_DVR_PID_COUNT, &value);
        //        retCode = dvrRecordingGet(storageHandle, recname[0],MPE_DVR_PID_INFO, pids);

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "- Recording Name       = %s\n",
                recname[0]);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "- Recording Size       = %d bytes\n", size);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "- Recording Length     = %d seconds\n", length);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "- Recording Pid count  = %d \n",
                value);

        for (i = 0; i < value; i++)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "- Pid #%d  = 0x%X\n", i,
                    pids[i].pid);
        }

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "test_dvr_recording3() --------- FILE LIST ---------\n");
        for (i = 0; i < count; i++)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "deleting %s\n", recname[i]);
            //            mpe_dvrRecordingDelete(recname[i]);
        }
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "test_dvr_recording3() -----------------------------\n");

        //        mpe_dvrFreeRecordingList();
    }
}

static void test_dvr_recording5(CuTest* tc)
{
#if 0   
    mpe_Error res;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_recording5() - Test case 5\n" );

    // Timer related things.
    if( MPE_SUCCESS != eventQueueNew( &m_mediaQueue1 ) )
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "Error: Could not create a queue.\n" );
        return;
    }

    // Thread for the media queue to run in.
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

    /* create timer Queue */
    if( MPE_SUCCESS != eventQueueNew( &m_timerQueue1 ) )
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "Error test_dvr_recording1() Could not create a queue.\n" );
        return;
    }

    if( 0 == m_timerThreadId1 )
    {
        if( MPE_SUCCESS != threadCreate( timerThread,
                        &m_sharedData1,
                        MPE_THREAD_PRIOR_DFLT,
                        MPE_THREAD_STACK_SIZE,
                        &m_timerThreadId1,
                        "timerThread") )
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "Error: Could not create tread\n" );
            return;
        }
    }

    if(m_dvrQueue1 == NULL)
    {
        // event queue
        if( MPE_SUCCESS != eventQueueNew( &m_dvrQueue1 ) )
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_recording1() Could not create a queue.\n" );
            return;
        }
    }

    if(condNew(TRUE, FALSE, &dvr_test_cond) != MPE_SI_SUCCESS)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "Error: Could not create condition obj\n" );
        return;
    }

    tune(TEST_FREQUENCY_DIGITAL_1,TEST_PROGRAM_NUMBER_DIGITAL_1,TEST_QAM_MODE_DIGITAL_1, true,1); /* with media decoding */

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_recording5() calling mpe_dvrRecordingStart()\n" );

    res = mpe_dvrRecordingStart( 1, //tuner ID
            0, //storage, 
            0, //bit_rate,
            NULL, //pids
            0, //pidCount,
            m_dvrQueue1, // event info
            NULL,
            &recording1, //recording handle 
            recName1 //recording name
    );

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_recording5() called mpe_dvrRecordingStart() - wait for %d seconds\n",WAIT_TIME1/1000 );

    /*
     retCode = eventQueueWaitNext(m_timerQueue1, &event, &poptional_eventdata, NULL, NULL, 
     WAIT_TIME1);

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

     res = mpe_dvrRecordingStop(recording1);

     mediaStop(0x22000001);
     */
#endif

}

static void test_dvr_recording6(CuTest* tc)
{

    mpe_Error res;
    mpe_Error retCode = MPE_SUCCESS;
    mpe_Event event;
    mpe_DvrString_t* recname = NULL;
    uint32_t count = 0;
    mpe_MediaRectangle src =
    { 0, 0, 1, 1 };
    mpe_MediaRectangle dst =
    { 0.8333f, 0.1111f, 0.5f, 0.5f };
    void *poptional_eventdata = NULL;

    //	retCode = mpe_dvrGetRecordingList(0,&count, &recname);

    if (recname == NULL)
        return;

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

    // create a media thread

    if (MPE_SUCCESS != eventQueueNew(&m_mediaQueue1, "DVR Media Queue"))
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "Error: Could not create a queue.\n");
        return;
    }

    // Thread for the media queue to run in.
    if (0 == m_mediaThreadId1)
    {
        if (MPE_SUCCESS != threadCreate(mediaThread, &m_sharedData1,
                MPE_THREAD_PRIOR_DFLT, MPE_THREAD_STACK_SIZE,
                &m_mediaThreadId1, "dvrMediaThread"))
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "Error: Could not create tread\n");
            return;
        }
    }

    // Thread for the dvr queue1 to run in.
    if (0 == m_dvrThreadId1)
    {
        if (MPE_SUCCESS != threadCreate(dvrThread1, &m_sharedData1,
                MPE_THREAD_PRIOR_DFLT, MPE_THREAD_STACK_SIZE, &m_dvrThreadId1,
                "dvrThread1"))
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "Error: Could not create tread\n");
            return;
        }
    }

    // Thread for the dvr queue1 to run in.
    if (0 == m_dvrThreadId2)
    {
        if (MPE_SUCCESS != threadCreate(dvrThread2, &m_sharedData1,
                MPE_THREAD_PRIOR_DFLT, MPE_THREAD_STACK_SIZE, &m_dvrThreadId2,
                "dvrThread2"))
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "test_dvr_recording6() Error: Could not create tread\n");
            return;
        }
    }

    /* create timer Queue */
    if (MPE_SUCCESS != eventQueueNew(&m_timerQueue1, "DVR Timer Queue"))
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "Error test_dvr_recording6() Could not create a queue.\n");
        return;
    }
    if (0 == m_timerThreadId1)
    {
        if (MPE_SUCCESS != threadCreate(timerThread, &m_sharedData1,
                MPE_THREAD_PRIOR_DFLT, MPE_THREAD_STACK_SIZE,
                &m_timerThreadId1, "timerThread"))
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "test_dvr_recording6() Error: Could not create tread\n");
            return;
        }
    }

    if (m_dvrQueue1 == NULL)
    {
        // event queue
        if (MPE_SUCCESS != eventQueueNew(&m_dvrQueue1, "DVR Event Queue1"))
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "test_dvr_recording6() Could not create a queue.\n");
            return;
        }
    }

    if (m_dvrQueue2 == NULL)
    {
        // event queue
        if (MPE_SUCCESS != eventQueueNew(&m_dvrQueue2, "DVR Event Queue2"))
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "test_dvr_recording6() Could not create a queue.\n");
            return;
        }
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_recording6() calling mpe_dvrRecordingStart()\n");

    /*
     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_dvr_recording6() START PLAYBACK 1 ON DECODER 0 \n" );
     res = mpe_dvrRecordingPlayStart(  recname[0],
     0x22000001,// video device
     NULL,
     0,        // pid count
     0,        // media Time
     1.0
     m_dvrQueue1,      // event dispatcher
     NULL,
     &playback1 );

     */
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_recording6() PLAY LIVE ON DECODER 0 \n");
    tune(TEST_FREQUENCY_ANALOG, TEST_PROGRAM_NUMBER_ANALOG,
            TEST_QAM_MODE_ANALOG, true, 1);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_recording6() START PLAYBACK 2 ON DECODER 1 \n");
    mpe_mediaSetBounds((mpe_DispDevice) 0x22000021, &src, &dst);

    /*    res = mpe_dvrRecordingPlayStart(  recname[1],
     (mpe_DispDevice)0x22000021,      // video device
     NULL,
     0,        // pid count
     0,        // media Time
     1.0,
     m_dvrQueue2,      // event dispatcher
     NULL,
     &playback2 ); 
     */

    /* let video play for 3 seconds */
    retCode = eventQueueWaitNext(m_timerQueue1, &event, &poptional_eventdata,
            NULL, NULL, 3000);
    if (retCode == MPE_ETIMEOUT)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "\nTime up...\n");
        event = MPE_ETIMEOUT;

        retCode = eventQueueSend(m_timerQueue1, MPE_ETIMEOUT, (void *) NULL,
                (void *) NULL, 0);

        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "\nProblem sending MPE_ETIMEOUT event...\n\n");
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "\nSent MPE_ETIMEOUT event...\n\n");
        }
    }

    /* Swap playbacks */

    /* this is not the real API..I had to use a public API to test
     but didn't want to create a new one */

    /* Eventually the media decoder swap API should be called here */

    //mpe_dvrGet( (mpe_DvrInfoParam)1000 , NULL, &dummy);
    mediaSwapDecoders((mpe_DispDevice) 0x22000001, (mpe_DispDevice) 0x22000021,
            true);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_recording6() - End of test case 6\n");

}

static void test_dvr_recording7(CuTest* tc)
{
    mpe_Error res;
    mpe_Error retCode = MPE_SUCCESS;
    mpe_Event event;
    mpe_DvrString_t* recname = NULL;
    uint32_t count = 0;
    void *poptional_eventdata = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_recording7() - Test case 7\n");

    //	retCode = mpe_dvrGetRecordingList(0,&count, &recname);

    if (recname == NULL)
        return;

    /* create timer Queue */
    if (MPE_SUCCESS != eventQueueNew(&m_timerQueue1, "DVR Timer Queue"))
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "Error test_dvr_recording7() Could not create a queue.\n");
        return;
    }

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

    if (m_dvrQueue1 == NULL)
    {
        // event queue
        if (MPE_SUCCESS != eventQueueNew(&m_dvrQueue1, "DVR Event Queue"))
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "test_dvr_recording7() Could not create a queue.\n");
            return;
        }
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_recording7() calling mpe_dvrRecordingStart()\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_recording7() Normal rate 1.0 \n");
    /*    res = mpe_dvrRecordingPlayStart(  recname[0],
     0x0,      // video device
     NULL,
     0,        // pid count
     0,        // media Time
     1.0,
     m_dvrQueue1,      // event dispatcher
     NULL,
     &playback1 );
     */
    /* play for 10 seconds */
    // retCode = eventQueueWaitNext(m_timerQueue1, &event, &poptional_eventdata, NULL, NULL,
    //					                   10000);
    if (retCode == MPE_ETIMEOUT)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "\nTime up...\n");
        event = MPE_ETIMEOUT;

        retCode = eventQueueSend(m_timerQueue1, MPE_ETIMEOUT, (void *) NULL,
                (void *) NULL, 0);

        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "\nProblem sending MPE_ETIMEOUT event...\n\n");
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "\nSent MPE_ETIMEOUT event...\n\n");
        }
    }

    /* Freeze the video */
    mpe_mediaFreeze((mpe_DispDevice) 0x22000001);

    /* wait for 10 secs */
    retCode = eventQueueWaitNext(m_timerQueue1, &event, &poptional_eventdata,
            NULL, NULL, 10000);

    if (retCode == MPE_ETIMEOUT)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "\nTime up...\n");
        event = MPE_ETIMEOUT;

        retCode = eventQueueSend(m_timerQueue1, MPE_ETIMEOUT, (void *) NULL,
                (void *) NULL, 0);

        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "\nProblem sending MPE_ETIMEOUT event...\n\n");
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "\nSent MPE_ETIMEOUT event...\n\n");
        }
    }

    /* Resume the video */
    mpe_mediaResume((mpe_DispDevice) 0x22000001);

    retCode = eventQueueWaitNext(m_timerQueue1, &event, &poptional_eventdata,
            NULL, NULL, 10000);
    if (retCode == MPE_ETIMEOUT)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "\nTime up...\n");
        event = MPE_ETIMEOUT;

        retCode = eventQueueSend(m_timerQueue1, MPE_ETIMEOUT, (void *) NULL,
                (void *) NULL, 0);

        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "\nProblem sending MPE_ETIMEOUT event...\n\n");
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "\nSent MPE_ETIMEOUT event...\n\n");
        }
    }
    /* stop playback */
    //    mpe_dvrPlayBackStop(playback1);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_recording7() - End of test case 7\n");
}

static void test_dvr_recording8(CuTest* tc)
{

    mpe_Error res;
    mpe_Error retCode = MPE_SUCCESS;
    mpe_Event event;
    mpe_DvrString_t* recname = NULL;
    uint32_t count = 0;

    float mode = 1.0;
    float actualMode = 1.0;

    void *poptional_eventdata = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_recording8() - Test case 8\n");

    //	retCode = mpe_dvrGetRecordingList(0,&count, &recname);

    if (recname == NULL)
        return;

    /* create timer Queue */
    if (MPE_SUCCESS != eventQueueNew(&m_timerQueue1, "DVR Timer Queue"))
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "Error test_dvr_recording8() Could not create a queue.\n");
        return;
    }

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

    if (m_dvrQueue1 == NULL)
    {
        // event queue
        if (MPE_SUCCESS != eventQueueNew(&m_dvrQueue1, "DVR Event Queue"))
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "test_dvr_recording8() Could not create a queue.\n");
            return;
        }
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_recording8() calling mpe_dvrRecordingStart()\n");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_recording8() Normal rate 1.0 \n");
    /*   res = mpe_dvrRecordingPlayStart(  recname[0],
     0x0,      // video device
     NULL,
     0,        // pid count
     0,        // media Time
     1.0,
     m_dvrQueue1,      // event dispatcher
     NULL,
     &playback1 );
     /*
     /* jump at 26 seconds */
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_recording8() Jump near the end of playback stream\n");
    //   res = mpe_dvrPlaybackSetTime( playback1, 26000 );

    /* pause for 10 secs */
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_recording8() Pause for 10 seconds\n");
    mode = 0.0;
    //   res = mpe_dvrSetTrickMode( playback1,
    //							   mode, 
    //							   &actualMode);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_recording2() - returned actual mode = %f\n", actualMode);

    //  retCode = eventQueueWaitNext(m_timerQueue1, &event, &poptional_eventdata, NULL, NULL,
    //					                   10000);

    if (retCode == MPE_ETIMEOUT)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "\nTime up...\n");
        event = MPE_ETIMEOUT;

        //		retCode = eventQueueSend(m_timerQueue1, MPE_ETIMEOUT, (void *) NULL, (void *) NULL, 0) ;

        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "\nProblem sending MPE_ETIMEOUT event...\n\n");
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "\nSent MPE_ETIMEOUT event...\n\n");
        }
    }

    /* resume play at normal rate */
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_recording2() resume play at normal rate \n");
    mode = 1.0;
    /*    res = mpe_dvrSetTrickMode( playback1,
     mode, 
     &actualMode);
     */

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "test_dvr_recording8() - End of test case 8\n");

}

/**
 * Will return the suite information describing the dvr tests.
 *
 * @return Will return a CuSuite* that describes the suite for dvr recording tests.
 */
CuSuite* getTestSuite_Recording1()
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, test_dvr_recording1);
    return suite;
}

/**
 * Will return the suite information describing the dvr tests.
 *
 * @return Will return a CuSuite* that describes the suite for dvr recording tests.
 */
CuSuite* getTestSuite_Recording2()
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, test_dvr_recording2);

    return suite;
}

/**
 * Will return the suite information describing the dvr tests.
 *
 * @return Will return a CuSuite* that describes the suite for dvr recording tests.
 */
CuSuite* getTestSuite_Recording3()
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, test_dvr_recording3);

    return suite;
}

/**
 * Will return the suite information describing the dvr tests.
 *
 * @return Will return a CuSuite* that describes the suite for dvr recording tests.
 */
CuSuite* getTestSuite_Recording4()
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, test_dvr_recording4);

    return suite;
}

CuSuite* getTestSuite_Recording5()
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, test_dvr_recording5);
    return suite;
}

CuSuite* getTestSuite_Recording6()
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, test_dvr_recording6);
    return suite;
}

CuSuite* getTestSuite_Recording7()
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, test_dvr_recording7);
    return suite;
}

CuSuite* getTestSuite_Recording8()
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, test_dvr_recording8);
    return suite;
}

static void tune(uint32_t freq, uint32_t prog, uint32_t qam, mpe_Bool decode,
        uint32_t tunerID)
{

    mpe_EventQueue tunerQueue = m_mediaQueue1;
    mpe_Error err = MPE_SUCCESS;

    mpe_MediaTuneRequestParams tunerRequestParams;

    tunerRequestParams.tuneParams.tuneType = MPE_MEDIA_TUNE_BY_TUNING_PARAMS;
    tunerRequestParams.tuneParams.frequency = freq;
    tunerRequestParams.tuneParams.programNumber = prog;
    tunerRequestParams.tuneParams.qamMode = qam;

    tuneSuccess = false;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "tune() - start of tune \n");

#ifdef POWERTV
    err = mediaTune( &tunerRequestParams,
            tunerQueue, NULL);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "tune() - End of tune \n" );

    condGet(dvr_test_cond);

    if(tuneSuccess == true)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "tune(%d %d %d) - Tuning success\n", freq,prog,qam);
        if(decode)
        {
            mpe_MediaDecodeRequestParams decoderRequestParams;
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "calling mediaDecode()...\n");
            decoderRequestParams.tunerId = tunerID;
            decoderRequestParams.numPids = 0;
            // just hardcode the value for now
            decoderRequestParams.videoDevice = (mpe_DispDevice)0x22000001; // TODO: Fix this so it isn't hardcoded!!!!

            mediaDecode( &decoderRequestParams, m_mediaQueue1, NULL);
        }

    }
#endif

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
        if (MPE_SUCCESS != eventQueueWaitNext(m_mediaQueue1, &eventId,
                &eventData, NULL, NULL, 0))
        {
            return;
        }

        switch (eventId)
        {
        case MPE_TUNE_SYNC:
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "TUNE_SYNC event received in "
                "mediaThread...\n");
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
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "Tuner error: DEBUG not sure [%d].\n", eventId);
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

    do
    {
        // wait forever until the next event
        if (MPE_SUCCESS != eventQueueWaitNext(m_timerQueue1, &eventId,
                &eventData, NULL, NULL, 0))
        {
            return;
        }

        switch (eventId)
        {
        case MPE_ETIMEOUT:
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "MPE_ETIMEOUT event received in timerThread..\n");
        }
            break;

        default:
            break;

        } // switch

    } while (true);
} // end timerThread(void*)


static void dvrThread1(void* data)
{
    mpe_Event eventId;
    void* eventData;

    do
    {
        // wait forever until the next event
        if (MPE_SUCCESS != eventQueueWaitNext(m_dvrQueue1, &eventId,
                &eventData, NULL, NULL, 0))
        {
            return;
        }

        switch (eventId)
        {
        case MPE_DVR_EVT_END_OF_FILE:
        {
            //                mpe_dvrPlayBackStop( playback1 );
        }
            break;

        default:
            break;

        } // switch

    } while (true);
} // end dvrThread1(void*)


static void dvrThread2(void* data)
{
    mpe_Event eventId;
    void* eventData;

    do
    {
        // wait forever until the next event
        if (MPE_SUCCESS != eventQueueWaitNext(m_dvrQueue2, &eventId,
                &eventData, NULL, NULL, 0))
        {
            return;
        }

        switch (eventId)
        {
        case MPE_DVR_EVT_END_OF_FILE:
        {
            //                mpe_dvrPlayBackStop( playback2 );
        }
            break;

        default:
            break;

        } // switch

    } while (true);
} // end dvrThread1(void*)
