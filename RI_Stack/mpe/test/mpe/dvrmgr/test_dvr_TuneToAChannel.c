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

/*
 * Globals:
 */
static mpe_Cond g_tuningCond1 = NULL;

/*
 * Private
 */
static mpe_Bool m_isInit1 = false;
static int32_t m_sharedTunerData1 = 0;

static mpe_EventQueue m_tunerQueue1 = NULL;
static mpe_ThreadId m_tunerQueueThreadId1 = 0;

static void Tune_CallbackThread(void* data);

static mpe_Bool tuneSuccess = false;

/**
 * Initializes anything that needs to be initialized before begining.
 */
mpe_Error Tune_Init(void)
{

#ifdef POWERTV

    if( true == m_isInit1 )
    {
        Tune_Destroy();
        return MPE_DVR_ERR_OS_FAILURE;
    }

    // Init the cond's
    condNew( TRUE, FALSE, &g_tuningCond1 );

    // Tuner related things.
    if( MPE_SUCCESS != eventQueueNew( &m_tunerQueue1 ) )
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "Error Tune_Init() Could not create a queue.\n" );
        return MPE_DVR_ERR_OS_FAILURE;
    }

    // Thread for the tuner queue to run in.
    if( 0 == m_tunerQueueThreadId1 )
    {
        if( MPE_SUCCESS != threadCreate( Tune_CallbackThread,
                        &m_sharedTunerData1,
                        MPE_THREAD_PRIOR_DFLT,
                        MPE_THREAD_STACK_SIZE,
                        &m_tunerQueueThreadId1,
                        "TuneCallbackThread") )
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "Error test_mpeos_basicMedia: Could not create tread for tuner.\n");
            return MPE_DVR_ERR_OS_FAILURE;
        }
    }

    m_isInit1 = true;
#endif
    return MPE_SUCCESS;
} // end Init()

/**
 * Destroys anything that needed to be initialized.
 */
void Tune_Destroy(void)
{
#ifdef POWERTV
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "DEBUG: Start Tune_Destroy()" );

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "DEBUG: Thread cleanup Tune_Destroy()\n" );
    if( (MPE_SUCCESS != threadDestroy( m_tunerQueueThreadId1 )) )
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "ERROR destroy: Could not destory threads\n" );
    }
    m_tunerQueueThreadId1 = 0;

    if( (MPE_SUCCESS != eventQueueDelete( m_tunerQueue1 )) )
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "ERROR destroy: Could not delete queues.\n" );
    }
    m_tunerQueue1 = 0;

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "DEBUG: Cond cleanup Tune_Destroy()\n" );
    condDelete( g_tuningCond1 );
    g_tuningCond1 = 0;
    m_isInit1 = false;
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "DEBUG: End Tune_Destroy()" );
#endif
} // end Tune_Destroy()

/**
 * Callback for the tuning thread.
 * @param data The data passed to the thread.
 */
static void Tune_CallbackThread(void* data)
{
    mpe_Event eventId;
    void* eventData;

    do
    {

        // wait forever until the next event
        if (MPE_SUCCESS != eventQueueWaitNext(m_tunerQueue1, &eventId,
                &eventData, NULL, NULL, 0))
        {
            return;
        }

        // If shutdown event comes thru, exit the thread		
        /*        if(eventId == MPE_EVENT_SHUTDOWN)
         {
         TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
         "MPE_EVENT_SHUTDOWN event received in TuneCallbackThread...\n");
         if( MPE_SUCCESS != eventQueueDelete(m_tunerQueue1) )
         {
         return;
         }
         // Leave loop
         break;
         }
         */

        switch (eventId)
        {
        case MPE_TUNE_SYNC:
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "TUNE_SYNC event received in TuneCallbackThread...\n");
            tuneSuccess = true;
            condSet(g_tuningCond1);
        }
            break;

        case MPE_TUNE_FAIL:
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "TUNE_FAIL event received in TuneCallbackThread...\n");
            tuneSuccess = false;
            condSet(g_tuningCond1); // DEBUG
        }
            break;

        case MPE_TUNE_ABORT:
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "TUNE_ABORT event received in TuneCallbackThread...\n");
            tuneSuccess = false;
            condSet(g_tuningCond1); // DEBUG
        }
            break;

        default:
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "Tuner error: DEBUG not sure [%d].\n", eventId);
            tuneSuccess = false;
            condSet(g_tuningCond1); // DEBUG
        }
            break;

        } // switch
        break;
    } while (true);
} // end Tune_CallbackThread(void*)

mpe_Error TuneToChannel(uint32_t tunerId, uint32_t freq, uint32_t prog,
        uint32_t qam, mpe_Bool decode)
{
    mpe_EventQueue tunerQueue = m_tunerQueue1;
    mpe_Error err = MPE_SUCCESS;
    //uint32_t tunerId = 1;

    mpe_MediaTuneRequestParams tunerRequestParams;

    mpe_MediaDecodeRequestParams decoderRequestParams;

    tunerRequestParams.tunerId = tunerId;
    tunerRequestParams.tuneParams.tuneType = MPE_MEDIA_TUNE_BY_TUNING_PARAMS;
    tunerRequestParams.tuneParams.frequency = TEST_FREQUENCY_DIGITAL_1;
    tunerRequestParams.tuneParams.programNumber = TEST_PROGRAM_NUMBER_DIGITAL_1;
    tunerRequestParams.tuneParams.qamMode = TEST_QAM_MODE_DIGITAL_1;

    tuneSuccess = false;

#ifdef POWERTV
    err = mediaTune( &tunerRequestParams,
            tunerQueue,
            NULL);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "mediaTune() - End of tune \n" );

    if( MPE_SUCCESS != condGet( g_tuningCond1 ) )
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "TuneToAChannel(%d %d %d) - failed \n", TEST_FREQUENCY_DIGITAL_1,TEST_PROGRAM_NUMBER_DIGITAL_1,TEST_QAM_MODE_DIGITAL_1);
        return MPE_DVR_ERR_OS_FAILURE;
    }

    if(tuneSuccess == true)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "TuneToAChannel(%d %d %d) - Tuning success\n", TEST_FREQUENCY_DIGITAL_1,TEST_PROGRAM_NUMBER_DIGITAL_1,TEST_QAM_MODE_DIGITAL_1);
    }
    else
    {
        return MPE_DVR_ERR_OS_FAILURE;
    }

    if(decode)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "TuneToAChannel(%d %d %d) - calling mediaDecode()\n", TEST_FREQUENCY_DIGITAL_1,TEST_PROGRAM_NUMBER_DIGITAL_1,TEST_QAM_MODE_DIGITAL_1);
        decoderRequestParams.tunerId = tunerId;
        decoderRequestParams.numPids = 2;
        decoderRequestParams.videoDevice = NULL; // Fix this!!!!

        mediaDecode( &decoderRequestParams, tunerQueue, NULL);
    }

    return MPE_SUCCESS;

#else
    return MPE_SUCCESS;
#endif
}

mpe_Error TuneToOC(uint32_t freq, uint32_t progId)
{
    return MPE_SUCCESS;
}

