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

/** \file
 *
 * \brief PowerTV port-specific code
 *
 * This file contains function that are PowerTV port specific.
 * 
 * \author Ric Yeates, Vidiom Systems Corp.
 *
 */

#if defined(POWERTV) && defined(TEST_MPEOS)

#include <test_media.h>
#include <test_utils.h>
#include <mpetest_dbg.h>
#include <mpe_filterevents.h>
#include "sectionFiltering_parameters.h"
#include "test_filter_sectionFiltering_include.h"

static mpe_Error TuneToChannel_init(void);
static void TuneToChannel_Destroy(void);
static mpe_Error TuneToAChannel(uint32_t sourceId);
static mpe_Error TuneToOCChannel(uint32_t freq, uint32_t progId);

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

static void TuneToChannel_TuningCallbackThread( void* data );

static mpe_Bool tuneSuccess = false;

/****************************************************************************
 *
 *  GoToOOBChannel()
 *
 ***************************************************************************/
/**
 * \brief move the tuner to the out-of-band channel
 *
 * This function moves the tuner to the out-of-band channel, if necessary.
 * Further, it returns an mpe_FilterSource object that expresses the
 * out-of-band filter source.
 *
 * \param pFilterSource pointer to location to fill with a pointer to an
 * allocated mpe_FilterSource object for the out-of-band source
 *
 * \return any error encountered getting the out-of-band source
 *
 */
mpe_Error GoToOOBChannel(mpe_FilterSource **pFilterSource)
{
    mpe_Error err;

    TRACE (MPE_LOG_DEBUG, MPE_MOD_TEST, "Into 'GoToOOBChannel()'\n");

#if 0	// taking out until I really need to tune
    if ((err = TuneToChannel_init()) != MPE_SUCCESS)
    {
        TRACE (MPE_LOG_FATAL, MPE_MOD_TEST, "'GoToOOBChannel()' - TuneToChannel_Init() failed\nEnd test\n");
        return err;
    }

    TRACE (MPE_LOG_DEBUG, MPE_MOD_TEST, "TuneToOCChannel\n");

    if(MPE_SUCCESS != (err = TuneToOCChannel(FREQUENCY1,PROGRAM1)))
    {
        TRACE (MPE_LOG_FATAL, MPE_MOD_TEST, "'GoToOOBChannel()' - TuneToOCChannel(%d) failed\nEnd test\n",FREQUENCY1);

    }

    TRACE (MPE_LOG_DEBUG, MPE_MOD_TEST, "'GoToOOBChannel()' - calling 'TuneToChannel_Destroy()'\n");

    TuneToChannel_Destroy();

    if (err != MPE_SUCCESS)
    return err;
#endif // 0
    err = memAllocP(MPE_MEM_TEST, sizeof(**pFilterSource), (void **)pFilterSource);
    if (err != MPE_SUCCESS)
    return err;

    (*pFilterSource)->sourceType = MPE_FILTER_SOURCE_OOB;
    (*pFilterSource)->pid = 0; // caller must fill
    (*pFilterSource)->parm.p_OOB.tsid = 0; // caller must change if desired

    TRACE (MPE_LOG_DEBUG, MPE_MOD_TEST, "'GoToOOBChannel()' - returning MPE_SUCCESS\n");

    return MPE_SUCCESS;
}

/****************************************************************************
 *
 *  GoToInbandChannel()
 *
 ***************************************************************************/
/**
 * \brief move the tuner to an in-band channel
 *
 * This function moves a tuner to an inband channel. Further, it returns an
 * mpe_FilterSource object that expresses the in-band filter source.
 *
 * \param pFilterSource pointer to location to fill with a pointer to an
 * allocated mpe_FilterSource object for the in-band source
 *
 * \return any error encountered getting the in-band source
 *
 */
mpe_Error GoToInbandChannel(mpe_FilterSource **pFilterSource)
{
    mpe_Error err;

    TRACE (MPE_LOG_DEBUG, MPE_MOD_TEST, "Into 'GoToInbandChannel()'\n");

#if 0	// taking out until I really need to tune
    if ((err = TuneToChannel_init()) != MPE_SUCCESS)
    {
        TRACE (MPE_LOG_FATAL, MPE_MOD_TEST, "'GoToInbandChannel()' - TuneToChannel_Init() failed \nEnd test\n");
        return err;
    }

    if(MPE_SUCCESS != (err = TuneToOCChannel(FREQUENCY1,PROGRAM1)))
    {
        TRACE (MPE_LOG_FATAL, MPE_MOD_TEST, "'GoToInbandChannel()' - TuneToOCChannel(%d) failed \nEnd test\n",FREQUENCY1);
    }

    TuneToChannel_Destroy();

    if (err != MPE_SUCCESS)
    return err;
#endif // 0
    err = memAllocP(MPE_MEM_TEST, sizeof(**pFilterSource), (void **)pFilterSource);
    if (err != MPE_SUCCESS)
    return err;

    (*pFilterSource)->sourceType = MPE_FILTER_SOURCE_INB;
    (*pFilterSource)->pid = 0; // caller must fill
    (*pFilterSource)->parm.p_INB.tunerID = 1; // FIXME: use params header for this
    (*pFilterSource)->parm.p_INB.freq = 1000; // FIXME: use params header for this
    (*pFilterSource)->parm.p_INB.tsid = 0; // caller must change if desired

    TRACE (MPE_LOG_DEBUG, MPE_MOD_TEST, "'GoToInbandChannel()' - returning MPE_SUCCESS\n");

    return MPE_SUCCESS;
}

static mpe_Error TuneToChannel_init(void)
{

    // NOTE: This call is required to delete TV resource. This is required when 
    // running the unit tests one after the other in a single batch file.
    // it doesn't harm if you run them separately.
    //  mpe_media_decoder_stopVideo(0);

    if( true == m_isInit1 )
    {
        TuneToChannel_Destroy();
        return MPE_SF_ERROR;
    }

    // Init the cond's
    condNew( TRUE, FALSE, &g_tuningCond1 );

    // Tuner related things.
    if( MPE_SUCCESS != eventQueueNew( &m_tunerQueue1, "TestSFTune" ) )
    {
        TRACE (MPE_LOG_FATAL, MPE_MOD_TEST, "'TuneToChannel_Init()' - could not create a queue.\n" );
        return MPE_SF_ERROR;
    }

    // Thread for the tuner queue to run in.
    if( 0 == m_tunerQueueThreadId1 )
    {
        if( MPE_SUCCESS != threadCreate( TuneToChannel_TuningCallbackThread,
                        &m_sharedTunerData1,
                        MPE_THREAD_PRIOR_DFLT,
                        MPE_THREAD_STACK_SIZE,
                        &m_tunerQueueThreadId1,
                        "TuningCallbackThread") )
        {
            TRACE (MPE_LOG_FATAL, MPE_MOD_TEST, "'TuneToChannel_Init()' - could not create thread for tuner.\n" );
            return MPE_SF_ERROR;
        }
    }

    m_isInit1 = true;

    return MPE_SUCCESS;
} // end init()

static void TuneToChannel_Destroy(void)
{
    TRACE (MPE_LOG_DEBUG, MPE_MOD_TEST, "Into 'TuneToChannel_Destroy()'" );

    TRACE (MPE_LOG_DEBUG, MPE_MOD_TEST, "'TuneToChannel_Destroy()' - calling 'threadDestroy()'\n");
    if( (MPE_SUCCESS != threadDestroy( m_tunerQueueThreadId1 )) )
    {
        TRACE (MPE_LOG_FATAL, MPE_MOD_TEST, "'TuneToChannel_Destroy()' - could not destory threads\n" );
    }
    m_tunerQueueThreadId1 = 0;

    if( (MPE_SUCCESS != eventQueueDelete( m_tunerQueue1 )) )
    {
        TRACE (MPE_LOG_FATAL, MPE_MOD_TEST, "'TuneToChannel_Destroy()' - could not delete queues.\n" );
    }
    m_tunerQueue1 = 0;

    TRACE (MPE_LOG_TRACE1, MPE_MOD_TEST, "'TuneToChannel_Destroy()' - Status cleanup\n" );

    TRACE (MPE_LOG_TRACE1, MPE_MOD_TEST, "DEBUG: Cond cleanup TuneToChannel_Destroy()" );
    condDelete( g_tuningCond1 );
    g_tuningCond1 = 0;
    m_isInit1 = false;
    TRACE (MPE_LOG_TRACE1, MPE_MOD_TEST, "DEBUG: End TuneToChannel_Destroy()" );
} // end TuneToChannel_Destroy()

static void TuneToChannel_TuningCallbackThread( void* data )
{
    mpe_Event eventId;
    void* eventData;

    do
    {

        // wait forever until the next event
        if( MPE_SUCCESS != eventQueueWaitNext(m_tunerQueue1,
                        &eventId,
                        &eventData,
                        NULL,
                        NULL,
                        0) )
        {
            return;
        }

        // If shutdown event comes thru, exit the thread		
        if(eventId == SHUTDOWN_THREAD)
        {
            TRACE (MPE_LOG_TRACE1, MPE_MOD_TEST, "SHUTDOWN_THREAD event received in "
                    "TuningCallbackThread...\n");
            if( MPE_SUCCESS != eventQueueDelete(m_tunerQueue1) )
            {
                return;
            }
            // Leave loop
            break;
        }

        switch(eventId)
        {
            case TUNE_SYNC:
            {
                TRACE (MPE_LOG_TRACE1, MPE_MOD_TEST, "TUNE_SYNC event received in "
                        "TuningCallbackThread...\n");
                tuneSuccess = true;
                condSet( g_tuningCond1 );
            }
            break;

            case TUNE_FAIL:
            {
                TRACE (MPE_LOG_TRACE1, MPE_MOD_TEST, "TUNE_FAIL event received in TuningCallbackThread...\n");
                tuneSuccess = false;
                condSet( g_tuningCond1 ); // DEBUG
            }
            break;

            case TUNE_ABORT:
            {
                TRACE (MPE_LOG_TRACE1, MPE_MOD_TEST, "TUNE_ABORT event received in TuningCallbackThread...\n");
                tuneSuccess = false;
                condSet( g_tuningCond1 ); // DEBUG
            }
            break;

            default:
            {
                TRACE (MPE_LOG_TRACE1, MPE_MOD_TEST, "Tuner error: DEBUG not sure [%d].\n", eventId );
                tuneSuccess = false;
                condSet( g_tuningCond1 ); // DEBUG
            }
            break;

        } // switch
        break;
    }while( true );
}

static mpe_Error TuneToAChannel(uint32_t sourceId)
{
    // Create async tuner status used to test if tuning passed.
    //    mpe_MediaAsyncTunerStatus  pTunerStatus;
    mpe_EventQueue tunerQueue = m_tunerQueue1;

    // Create a thread for the tuner.
    uint32_t tunerId;
    tuneSuccess = false;

    tunerId = 1;
    //pTunerStatus.error = MPE_SUCCESS;

    //    media_tuner_selectServiceUsingSourceId ( tunerId, sourceId,
    //                                          tunerQueue, &pTunerStatus );
    if( MPE_SUCCESS != condGet( g_tuningCond1 ) )
    {
        TRACE (MPE_LOG_FATAL, MPE_MOD_TEST, "TuneToAChannel(%x) - failed \n",sourceId);
        return MPE_SF_ERROR;
    }
    if(tuneSuccess == true)
    {
        TRACE (MPE_LOG_TRACE1, MPE_MOD_TEST, "TuneToAChannel(%x) - Tuning success\n",sourceId);
        return MPE_SUCCESS;
    }
    else
    return MPE_SF_ERROR;
}

static mpe_Error TuneToOCChannel(uint32_t freq, uint32_t progId)
{
    // Create async tuner status used to test if tuning passed.
    //    mpe_MediaAsyncTunerStatus  pTunerStatus;
    mpe_EventQueue tunerQueue = m_tunerQueue1;

    tuneSuccess = false;
    //pTunerStatus.error = MPE_SUCCESS;

    //    media_tuner_selectServiceUsingTuningParams(0, freq, progId, kTv_QAM64, tunerQueue, &pTunerStatus);
    if( MPE_SUCCESS != condGet( g_tuningCond1 ) )
    {
        TRACE (MPE_LOG_FATAL, MPE_MOD_TEST, "TuneToOCChannel(%d, %d) - failed\n",freq,progId);
        return MPE_SF_ERROR;
    }

    if(tuneSuccess == true)
    {
        TRACE (MPE_LOG_TRACE1, MPE_MOD_TEST, "TuneToOCChannel(%d, %d) - Tuning success\n",freq,progId);
        return MPE_SUCCESS;
    }
    else
    return MPE_SF_ERROR;
}

#endif /* defined(POWERTV) && defined(TEST_MPEOS) */
