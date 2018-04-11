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

#include <stdlib.h>        /* atoi(3) */
#include <string.h>        /* strcasecmp(3) */

#include "sitp_psi.h"
#include "sitp_parse.h"
#include "filter_support.h"
#include <inttypes.h>

/*
 * Private Functions
 */
static void sitp_psiWorkerThread(void * threadData);

static void printFilterSpec(mpe_FilterSpec filterSpec);

static mpe_Error set_filter(sitp_psi_data_t *psi_data, mpe_FilterGroup filterGroup, uint8_t priority, uint32_t timeout, sitp_psi_filter_t *psi_filter);
static mpe_Error set_tuner_info(sitp_psi_data_t *psi_data);
static mpe_Bool isAnalogChannel(sitp_psi_data_t *psi_data);

static mpe_Error process_tune_sync(sitp_psi_data_t *psi_data);
static void process_tune_start(uint8_t index);
static void process_transport_stream_unavailable(uint8_t index);
static void process_oob_event(sitp_psi_data_t *psi_data);

static uint32_t sitp_psi_getNumActiveDSGChannels(void);
mpe_Error addDSGTunnelToStateMachine(uint32_t appID);
mpe_Error removeDSGTunnelFromStateMachine(uint32_t appID);

mpe_Error addHNSessionToStateMachine(mpe_HnStreamSession session);
mpe_Error removeHNSessionFromStateMachine(mpe_HnStreamSession session);
static uint32_t sitp_psi_getNumActiveHnStreams(void);

static mpe_Error createAndSetTunerPSIDataArray(void);
static mpe_Error createAndSetOOBPSIDataArray(void);
static mpe_Error resetTunerPSIData(uint8_t tuner_index);
static mpe_Error resetPSIData(sitp_psi_tuner_type_e tuner_type, ListSI psi_data_array);
static mpe_Error init_fsm_data(sitp_psi_fsm_data_t *fsm_data);
static mpe_Error clear_fsm_data(sitp_psi_fsm_data_t *fsm_data);
static void cancel_all_filters(sitp_psi_data_t *psi_data);
static mpe_Error clear_secondary_PMT_list(sitp_psi_fsm_data_t *fsm_data);
static mpe_Error init_filter_mode(void);
static mpe_Error process_pat_timeout(sitp_psi_data_t *psi_data);
static mpe_Error process_pmt_timeout(sitp_psi_data_t *psi_data, uint16_t program_number);
static mpe_Bool isFirstPMTInTransportStream(sitp_psi_fsm_data_t *fsm_data, uint16_t program_number);

// Callback function
static uint32_t mpe_sitpSharedFilterCallback( mpe_SharedFilter sharedFilter,
                                              mpe_FilterGroup filterGroup,
                                              void * userPointer,
                                              uint32_t userData,
                                              mpe_SharedFilterEvent event,
                                              uint16_t sectionSize,
                                              uint8_t sectionData[],
                                              mpe_Bool isLast );

/* debug functions */
char * sitp_psi_eventToString(uint32_t event);
char * sitp_psi_stateToString(uint32_t state);
void printPSIStates(void);

/*
 * Global variables
 */
static mpe_EventQueue g_sitp_psi_queue;
static uint32_t g_sitp_psi_num_tuners = 0;

// Length of g_tuner_psi_data_array is equal to number of in-band tuners
static sitp_psi_data_t *g_IB_psi_data_array = NULL;
// OOB PSI data (allocated at startup)
static sitp_psi_data_t *g_OOB_psi_data = NULL;
// DSG PSI data (dynamically allocated/removed when DSG tunnels are mounted/unmounted)
static ListSI g_DSG_psi_data_array = NULL;
static uint32_t g_sitp_psi_DSG_count = 0;
// HN PSI data (dynamically allocated/removed when HN remote playback starts/stops)
static ListSI g_HN_psi_data_array = NULL;
static uint32_t g_sitp_psi_HN_count = 0;

// Global filtering mode (specified in the ini file)
static uint32_t g_sitp_psi_filtering_mode = 0;
// Timeout values used for IB PSI
static uint32_t g_sitp_ib_pat_timeout_interval = 0;
static uint32_t g_sitp_ib_pmt_timeout_interval = 0;
static mpe_Bool g_sitp_ib_psi_process_table_revision = TRUE;
static uint32_t g_sitp_ib_psi_round_robin_interval = 0;
// Timeout values used for OOB/DSG PSI
static uint32_t g_sitp_oob_pat_timeout_interval = 0;
static uint32_t g_sitp_oob_pmt_timeout_interval = 0;
static mpe_Bool g_sitp_oob_psi_process_table_revision = TRUE;
// Timeout values used for HN streams
static uint32_t g_sitp_hn_pat_timeout_interval = 0;
static uint32_t g_sitp_hn_pmt_timeout_interval = 0;
static mpe_Bool g_sitp_hn_psi_process_table_revision = TRUE;

// Flag to enable/disable OOB PSI acquisition (Off by default)
static mpe_Bool g_sitp_enable_oob_psi = FALSE;

// PSI thread id
static mpe_ThreadId g_inb_threadID = (mpe_ThreadId) -1;
// Mutex for thread
static mpe_Mutex g_sitp_psi_mutex = NULL;
static mpe_Bool g_shutDown = 0;

static char *ocStbHostSystemLoggingEventTable_oid = "1.3.6.1.4.1.4491.2.3.1.1.4.3.5.5";

/* ***************************************************************************************************
 **  Subroutine/Method:                       sitp_psi_Start()
 ** ***************************************************************************************************
 ** This subroutine activates the PSI Table Parsing sub component. Activation
 ** is accomplished by reading the related environment variables, setting
 ** corresponding global flags and starting the PSI table acquisition thread.
 **
 ** @param None,
 **
 ** @return SITP_SUCCESS or other error codes.
 ** **************************************************************************************************/
mpe_Error sitp_psi_Start(void)
{
    mpe_Error retCode = SITP_SUCCESS;
    const char *sitp_enable_ib_psi = NULL;
    const char *sitp_enable_oob_psi = NULL;
    const char *sitp_psi_num_tuners = NULL;
    const char *sitp_psi_filtering_mode = NULL;
    const char *sitp_ib_pat_timeout_interval = NULL;
    const char *sitp_ib_pmt_timeout_interval = NULL;
    const char *sitp_ib_psi_process_table_revision = NULL;
    const char *sitp_ib_psi_round_robin_interval = NULL;
    const char *sitp_oob_pat_timeout_interval = NULL;
    const char *sitp_oob_pmt_timeout_interval = NULL;
    const char *sitp_oob_psi_process_table_revision = NULL;
    const char *sitp_hn_pat_timeout_interval = NULL;
    const char *sitp_hn_pmt_timeout_interval = NULL;
    const char *sitp_hn_psi_process_table_revision = NULL;
    uint32_t numTuners = 0;

    /* Create global registration list mutex */
    retCode = mpe_mutexNew(&g_sitp_psi_mutex);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                "<sitp_psi_Start() sipt psi mutex creation failed, returning MPE_EMUTEX\n");
        return MPE_EMUTEX;
    }

     /************************************************************************************************
     ** OOB PSI environment variables
     **************************************************************************************************/
    /*
     ** ***********************************************************************************************
     ** Enable oob PSI(PAT/PMT) acquisition. Off by default
     ** ************************************************************************************************
     */
    sitp_enable_oob_psi = mpeos_envGet("SITP.ENABLE.OOB.PSI");

    if ((NULL == sitp_enable_oob_psi) || (stricmp(sitp_enable_oob_psi, "FALSE") == 0))
    {
        g_sitp_enable_oob_psi = FALSE;
    }
    else
    {
        g_sitp_enable_oob_psi = TRUE;
    }

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::sitp_psi_Start> - g_sitp_enable_oob_psi: %s\n",
            PSIMODULE, (g_sitp_enable_oob_psi ? "TRUE" : "FALSE"));

    /*
     ** ************************************************************************************************
     ** Process the OOB/DSG PAT/PMT revisions. (table revisions for OOB/DSG) On by default
     ** ************************************************************************************************
     */
    sitp_oob_psi_process_table_revision = mpeos_envGet("SITP.PSI.PROCESS.OOB.TABLE.REVISIONS");

    if ((NULL == sitp_oob_psi_process_table_revision) || (stricmp(sitp_oob_psi_process_table_revision, "TRUE") == 0))
    {
        g_sitp_oob_psi_process_table_revision = TRUE;
    }
    else
    {
        g_sitp_oob_psi_process_table_revision = FALSE;
    }

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::sitp_psi_Start> - oob table revision processing is %s\n",
            PSIMODULE, (g_sitp_oob_psi_process_table_revision ? "ON" : "OFF"));
    /*
     ** ************************************************************************************************
     * Get the OOB/DSG PAT timeout value. Default 2 sec
     ** ************************************************************************************************
     */
    sitp_oob_pat_timeout_interval = mpeos_envGet("SITP.OOB.PAT.TIMEOUT.INTERVAL");
    if (sitp_oob_pat_timeout_interval != NULL)
    {
        g_sitp_oob_pat_timeout_interval = atoi(sitp_oob_pat_timeout_interval);
    }
    else
    {
        g_sitp_oob_pat_timeout_interval = 2000; // two seconds
    }

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::sitp_psi_Start> - OOB/DSG PAT timeout interval set to %d ms\n",
            PSIMODULE, g_sitp_oob_pat_timeout_interval);

    /*
     ** ************************************************************************************************
     * Get the OOB/DSG PMT timeout value. Default 4 sec
     ** ************************************************************************************************
     */
    sitp_oob_pmt_timeout_interval = mpeos_envGet("SITP.OOB.PMT.TIMEOUT.INTERVAL");
    if (sitp_oob_pmt_timeout_interval != NULL)
    {
        g_sitp_oob_pmt_timeout_interval = atoi(sitp_oob_pmt_timeout_interval);
    }
    else
    {
        g_sitp_oob_pmt_timeout_interval = 4000; // 4 seconds
    }

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::sitp_psi_Start> - OOB PMT timeout interval set to %d ms\n",
            PSIMODULE, g_sitp_oob_pmt_timeout_interval);

    /*
     ** ************************************************************************************************
     ** Process the HN PAT/PMT revisions. On by default
     ** ************************************************************************************************
     */
    sitp_hn_psi_process_table_revision = mpeos_envGet("SITP.PSI.PROCESS.HN.TABLE.REVISIONS");

    if ((NULL == sitp_hn_psi_process_table_revision) || (stricmp(sitp_hn_psi_process_table_revision, "TRUE") == 0))
    {
        g_sitp_hn_psi_process_table_revision = TRUE;
    }
    else
    {
        g_sitp_hn_psi_process_table_revision = FALSE;
    }

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::sitp_psi_Start> - hn table revision processing is %s\n",
            PSIMODULE, (g_sitp_hn_psi_process_table_revision ? "ON" : "OFF"));
    /*
     ** ************************************************************************************************
     * Get the HN stream PAT timeout value. Default 2 sec
     ** ************************************************************************************************
     */
    sitp_hn_pat_timeout_interval = mpeos_envGet("SITP.HN.PAT.TIMEOUT.INTERVAL");
    if (sitp_hn_pat_timeout_interval != NULL)
    {
        g_sitp_hn_pat_timeout_interval = atoi(sitp_hn_pat_timeout_interval);
    }
    else
    {
        g_sitp_hn_pat_timeout_interval = 2000; // one second
    }

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::sitp_psi_Start> - HN PAT timeout interval set to %d ms\n",
            PSIMODULE, g_sitp_hn_pat_timeout_interval);

    /*
     ** ************************************************************************************************
     * Get the HN stream PMT timeout value. Default 4 sec
     ** ************************************************************************************************
     */
    sitp_hn_pmt_timeout_interval = mpeos_envGet("SITP.HN.PMT.TIMEOUT.INTERVAL");
    if (sitp_hn_pmt_timeout_interval != NULL)
    {
        g_sitp_hn_pmt_timeout_interval = atoi(sitp_hn_pmt_timeout_interval);
    }
    else
    {
        g_sitp_hn_pmt_timeout_interval = 4000; // 4 seconds
    }

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::sitp_psi_Start> - HN PMT timeout interval set to %d ms\n",
            PSIMODULE, g_sitp_hn_pmt_timeout_interval);

    /************************************************************************************************
    ** IB PSI environment variables
    **************************************************************************************************/

    sitp_psi_num_tuners = mpeos_envGet("MPE.SYS.NUM.TUNERS");
    if (sitp_psi_num_tuners != NULL)
    {
        numTuners = atoi(sitp_psi_num_tuners);
    }

    if (numTuners == 0)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                "<sitp_psi_Start()> Number of tuners invalid!(%d)\n", numTuners);
    }
    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<sitp_psi_Start()> Number of tuners: %d\n", numTuners);

    // Total physical tuners in the box (IB)
    g_sitp_psi_num_tuners = numTuners;

    /*
     ** ************************************************************************************************
     * Get the IB PAT timeout value. Default 1 sec
     ** ************************************************************************************************
     */
    sitp_ib_pat_timeout_interval = mpeos_envGet("SITP.IB.PAT.TIMEOUT.INTERVAL");
    if (sitp_ib_pat_timeout_interval != NULL)
    {
        g_sitp_ib_pat_timeout_interval = atoi(sitp_ib_pat_timeout_interval);
    }
    else
    {
        g_sitp_ib_pat_timeout_interval = 1000; // one second
    }

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::sitp_psi_Start> - IB PAT timeout interval set to %d ms\n",
            PSIMODULE, g_sitp_ib_pat_timeout_interval);

    /*
     ** ************************************************************************************************
     * Get the IB PMT timeout value. Default 2 sec
     ** ************************************************************************************************
     */
    sitp_ib_pmt_timeout_interval = mpeos_envGet("SITP.IB.PMT.TIMEOUT.INTERVAL");
    if (sitp_ib_pmt_timeout_interval != NULL)
    {
        g_sitp_ib_pmt_timeout_interval = atoi(sitp_ib_pmt_timeout_interval);
    }
    else
    {
        g_sitp_ib_pmt_timeout_interval = 2000; // one seconds
    }

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::sitp_psi_Start> - IB PMT timeout interval set to %d ms\n",
            PSIMODULE, g_sitp_ib_pmt_timeout_interval);

    /*
     ** ************************************************************************************************
     ** Process IB PAT/PMT revisions. On by default
     ** ************************************************************************************************
     */
    sitp_ib_psi_process_table_revision = mpeos_envGet("SITP.PSI.PROCESS.TABLE.REVISIONS");

    if ((NULL == sitp_ib_psi_process_table_revision) || (stricmp(sitp_ib_psi_process_table_revision, "TRUE") == 0))
    {
        g_sitp_ib_psi_process_table_revision = TRUE;
    }
    else
    {
        g_sitp_ib_psi_process_table_revision = FALSE;
    }

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::sitp_psi_Start> - ib table revision processing is %s\n",
            PSIMODULE, (g_sitp_ib_psi_process_table_revision ? "ON" : "OFF"));

    /*
     ** ************************************************************************************************
     ** Get the IB round-robin interval. Default 2 sec
     ** ************************************************************************************************
     */
    sitp_ib_psi_round_robin_interval = mpeos_envGet("SITP.PSI.ROUND.ROBIN.INTERVAL");
    if (sitp_ib_psi_round_robin_interval != NULL)
    {
        g_sitp_ib_psi_round_robin_interval = atoi(sitp_ib_psi_round_robin_interval);
    }
    else
    {
        g_sitp_ib_psi_round_robin_interval = 2000; // two seconds
    }

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::sitp_psi_Start> - filter roundRobin interval set to %d ms\n",
            PSIMODULE, g_sitp_ib_psi_round_robin_interval);

    /*
     ** ***********************************************************************************************
     ** Get the filtering mode to use on this platform.
     **
     ** Mode 1: Legacy mode (use one section filter for all In-band PSI)
     **
     ** Mode 2: Dedicated filter per tuner
     **
     ** Mode 3: Dedicated two filters per tuner without secondary PMT acquisition
     **
     ** Mode 4: Dedicated per-filter tuner for PAT and selected PMT with �wandering�
     **         PSI pre-fetch filter that scans non-selected PSI on all other tuners
     **
     ** Mode 5: Mode 3 plus wandering PSI pre-fetch filter
     **
     ** Mode X: Dedicated filters for all acquisition (for a box with unbounded filtering resources)
     ** ************************************************************************************************
     */
    sitp_psi_filtering_mode = mpeos_envGet("SITP.PSI.FILTER.MODE");

    if (sitp_psi_filtering_mode != NULL)
    {
        g_sitp_psi_filtering_mode = atoi(sitp_psi_filtering_mode);
    }
    else
    {
        g_sitp_psi_filtering_mode = 1; // legacy mode (1 filter)
    }

    if(g_sitp_psi_filtering_mode > 5)
    {
        // Only 5 modes are supported for now.
        g_sitp_psi_filtering_mode = 1; // legacy mode (1 filter)
    }

    //************************************************************************************************
    // Launch the IN-BAND(INB)Thread which will periodically acquire the PAT table and an unknown
    // number of PMT table sections, if not disabled.
    //************************************************************************************************

    sitp_enable_ib_psi = mpeos_envGet("SITP.PSI.ENABLED");

    if ((sitp_enable_ib_psi == NULL) || (stricmp(sitp_enable_ib_psi, "TRUE") == 0))
    {
        retCode = mpeos_threadCreate(sitp_psiWorkerThread, NULL,
                MPE_THREAD_PRIOR_SYSTEM, MPE_THREAD_STACK_SIZE,
                &g_inb_threadID, "mpePsiWorkerThread");
        if (SITP_SUCCESS != retCode)
        {
            MPE_LOG(MPE_LOG_ERROR,
                    MPE_MOD_SI,
                    "<%s::sitp_psi_Start> - ERROR: failed to create psiWorkerThread, error: %d\n",
                    PSIMODULE, retCode);
        }
        else
        {
            MPE_LOG(MPE_LOG_TRACE1,
                    MPE_MOD_SI,
                    "<%s::sitp_psi_Start> - psiWorkerThread thread started successfully\n",
                    PSIMODULE);
        }
    }
    else
    {
        MPE_LOG(MPE_LOG_TRACE1,
                MPE_MOD_SI,
                "<%s::sitp_psi_Start> - psiWorkerThread thread disabled by SITP.PSI.ENABLED\n",
                PSIMODULE);
    }

    return retCode;
}

/* ***************************************************************************************************
 **  Subroutine/Method:                       sitp_psi_ShutDown()
 ** ***************************************************************************************************
 ** This subroutine shuts down the SI Table Parsing sub component.
 **
 ** @param None,
 **
 ** @return SITP_SUCCESS or other error codes.
 ** **************************************************************************************************/
void sitp_psi_ShutDown(void)
{
    int i=0;
    // cleanup OOB/DSG/HN psi data array
    if(resetPSIData(SITP_PSI_TUNER_TYPE_DSG, g_DSG_psi_data_array) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_TRACE1,
                MPE_MOD_SI,
                "<%s::sitp_psi_ShutDown> - Error releasing DSG PSI data..\n",
                PSIMODULE);
    }
    if(resetPSIData(SITP_PSI_TUNER_TYPE_HN, g_HN_psi_data_array)  != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_TRACE1,
                MPE_MOD_SI,
                "<%s::sitp_psi_ShutDown> - Error releasing HN PSI data..\n",
                PSIMODULE);
    }
    // cleanup IB tuner psi data array
    for(i=0; i<g_sitp_psi_num_tuners; i++)
    {
        // reset PSI data
        resetTunerPSIData(i);
    }

    //sent Shutdown Event to worker thread.
    mpeos_eventQueueSend(g_sitp_psi_queue, SITP_PSI_EVENT_SHUTDOWN, NULL, NULL, 0);

    //Follow up the SHUTDOWN event with the shutdown flag.
    g_shutDown = TRUE;

    if (0 != g_sitp_psi_mutex)
    {
        mpe_mutexRelease(g_sitp_psi_mutex);
        g_sitp_psi_mutex = 0;
    }
}

/* ***************************************************************************
 ** **** Thread:                 void psiWorkerThread (void * threadData)
 ** ***************************************************************************
 **
 ** The entry point of the worker thread.
 **
 ** @param data Receives the thread data passed to the function.
 ** **************************************************************************/
static void sitp_psiWorkerThread(void * threadData)
{
    mpe_Error retCode = SITP_SUCCESS;
    void *poptional_eventdata1 = NULL;
    void *poptional_eventdata2 = NULL;
    uint32_t event_data1 = 0;
    uint32_t event_tuner_number = 0;
    uint32_t event_data2 = 0;
    uint32_t event_data3 = 0;
    mpe_Event psi_event = 0;

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI, "<%s::sitp_psiWorkerThread> - Enter\n",
            PSIMODULE);

    /*
     ** *************************************************************************
     ** Remove warning for unused threadData params
     ** *************************************************************************
     */
    MPE_UNUSED_PARAM(threadData);

    /*
     ** *************************************************************************
     ** **** Create a global event queue for the Thread
     ** *************************************************************************
     */
    if ((retCode = mpeos_eventQueueNew(&g_sitp_psi_queue, "MpeSitpPsi")) != SITP_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::sitp_psiWorkerThread> - unable to create event queue,... terminating thread.\n",
                PSIMODULE);
        return;
    }

    /*
     ** *************************************************************************
     ** **** Register for events with the Media Manager.
     ** *************************************************************************
     */
    if (SITP_SUCCESS != mpeos_mediaRegisterQueueForTuneEvents(g_sitp_psi_queue))
    {
        MPE_LOG(MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::sitp_psiWorkerThread> - ERROR: Unable to register for Media Mgr events, ...terminating thread.\n",
                PSIMODULE);
        return;
    }
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
            "<%s::sitp_psiWorkerThread> - Registered for Media Mgr events.\n",
            PSIMODULE);

    // Create/Initialize IB tuner data structures
    if (SITP_SUCCESS != createAndSetTunerPSIDataArray())
    {
        MPE_LOG(MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::sitp_psiWorkerThread> - ERROR: Unable to create tuner PSI data array, ...terminating thread.\n",
                PSIMODULE);
        return;
    }

    if (FALSE == g_sitp_enable_oob_psi)
    {
        psi_event = OOB_PSI_DISABLED;
    }
    else
    {
        // If OOB is enabled
        // Create/Initialize OOB data structures
        if (SITP_SUCCESS != createAndSetOOBPSIDataArray())
        {
            MPE_LOG(MPE_LOG_ERROR,
                    MPE_MOD_SI,
                    "<%s::sitp_psiWorkerThread> - ERROR: Unable to create OOB PSI data array, ...terminating thread.\n",
                    PSIMODULE);
            return;
        }
        psi_event = OOB_PSI_ACQUISITION;
        poptional_eventdata1 = SITP_PSI_TUNER_TYPE_OOB;
        event_data3 = SITP_PSI_TUNER_TYPE_OOB;
    }

    MPE_LOG(MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<%s::sitp_psiWorkerThread> - psi_event:%d\n",
            PSIMODULE, psi_event);

    /*
     ** ***********************************************************************************************
     ** *** Loop indefinitely until function/method  the Shutdown is invoked by by a higher authority.
     ** ***********************************************************************************************
     */
    while (FALSE == g_shutDown)
    {
        /*
         ** Acquire the global mutex.
         */
        mpe_mutexAcquire(g_sitp_psi_mutex);

        printPSIStates();

        event_data1 = (uint32_t) poptional_eventdata1; // set the optional event data
        event_data2 = (uint32_t) poptional_eventdata2; // set the optional event data

        switch (psi_event)
        {
            case MPE_TUNE_SYNC:
            {
                event_tuner_number = event_data1;
                MPE_LOG(MPE_LOG_TRACE1,
                        MPE_MOD_SI,
                        "<%s::sitp_psiWorkerThread> - Matched event: TUNE_SYNC tunerId:%d.\n",
                        PSIMODULE, event_tuner_number);
                // reset existing PSI data
                resetTunerPSIData(event_tuner_number-1);

                // g_IB_psi_data_array index starts at 0
                if((retCode = process_tune_sync(&g_IB_psi_data_array[event_tuner_number-1])) != MPE_SUCCESS)
                {
                    MPE_LOG(MPE_LOG_TRACE1,
                            MPE_MOD_SI,
                            "<%s::sitp_psiWorkerThread> - process_tune_sync returned error %d.\n",
                            PSIMODULE, retCode);
                }
                break;
            }
            case OOB_PSI_ACQUISITION:
            {
                MPE_LOG(MPE_LOG_TRACE1,
                        MPE_MOD_SI,
                        "<%s::sitp_psiWorkerThread> - Matched event: OOB_PSI_ACQUISITION.\n",
                        PSIMODULE);
                // Activate OOB PSI filters
                MPE_LOG(MPE_LOG_TRACE1,
                        MPE_MOD_SI,
                        "<%s::sitp_psiWorkerThread> - OOB_PSI_ACQUISITION..\n",
                        PSIMODULE);
                process_oob_event(g_OOB_psi_data);
            }
            break;
            case OOB_DSG_PSI_ACQUISITION:
            {
                uint32_t appId = 0;

                appId = event_data2;

                MPE_LOG(MPE_LOG_TRACE1,
                        MPE_MOD_SI,
                        "<%s::sitp_psiWorkerThread> - Matched event: OOB_DSG_PSI_ACQUISITION appId:%d\n",
                        PSIMODULE, appId);
                // activate DSG filter if the event corresponds to DSG tunnel open
                if(llist_cnt(g_DSG_psi_data_array) > 0)
                {
                    LINK *lp = llist_first(g_DSG_psi_data_array);
                    while (lp)
                    {
                        sitp_psi_data_t *dsg_psi_data = (sitp_psi_data_t *) llist_getdata(lp);
                        if(dsg_psi_data
                            && dsg_psi_data->psi_params.p_dsg_info.dsg_app_id == appId)
                        {
                            MPE_LOG(MPE_LOG_TRACE1,
                                    MPE_MOD_SI,
                                    "<%s::sitp_psiWorkerThread> - OOB_DSG_PSI_ACQUISITION  found appId:%d\n",
                                    PSIMODULE, appId);
                            process_oob_event(dsg_psi_data);
                            break;
                        }
                        lp = llist_after(lp);
                    }
                }
            }
            break;
            case OOB_HN_STREAM_PSI_ACQUISITION:
            {
                mpe_HnStreamSession hn_stream = NULL;

                hn_stream = (mpe_HnStreamSession) event_data2;

                MPE_LOG(MPE_LOG_TRACE1,
                        MPE_MOD_SI,
                        "<%s::sitp_psiWorkerThread> - Matched event: OOB_HN_STREAM_PSI_ACQUISITION hn_stream:0x%x\n",
                        PSIMODULE, hn_stream);
                // HN stream is also following similar to DSG
                // so make sure if the returned values failure to also search for HN session
                if(llist_cnt(g_HN_psi_data_array) > 0)
                {
                    LINK *lp = llist_first(g_HN_psi_data_array);
                    while (lp)
                    {
                        sitp_psi_data_t *hn_psi_data = (sitp_psi_data_t *) llist_getdata(lp);
                        if(hn_psi_data
                            && hn_psi_data->psi_params.p_hn_stream_info.hn_stream_session == hn_stream)
                        {
                            MPE_LOG(MPE_LOG_TRACE1,
                                    MPE_MOD_SI,
                                    "<%s::sitp_psiWorkerThread> - OOB_HN_STREAM_PSI_ACQUISITION found hn_stream_session:0x%x\n",
                                    PSIMODULE, hn_stream);
                            process_oob_event(hn_psi_data);
                            break;
                        }
                        lp = llist_after(lp);
                    }
                }
            }
            break;
            case OOB_PSI_DISABLED:
            {
                MPE_LOG(MPE_LOG_DEBUG,
                        MPE_MOD_SI,
                        "<%s::sitp_psiWorkerThread> - received event OOB_PSI_DISABLED.\n",
                        PSIMODULE);
            }
            break;
            case MPE_TUNE_STARTED:
            {
                event_tuner_number = event_data1;
                MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
                        "<%s::sitp_psiWorkerThread> - Received %s Event...tuner_number: %d\n",
                        PSIMODULE, sitp_psi_eventToString(psi_event), event_tuner_number);

                process_tune_start(event_tuner_number-1);
            }
            break;
            case MPE_TUNE_UNSYNC:
            {
                event_tuner_number = event_data1;
                MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
                        "<%s::sitp_psiWorkerThread> - Received %s Event...tuner_number: %d\n",
                        PSIMODULE, sitp_psi_eventToString(psi_event), event_tuner_number);

                process_transport_stream_unavailable(event_tuner_number-1);
            }
            break;
            case MPE_TUNE_FAIL:
            {
                event_tuner_number = event_data1;
                MPE_LOG(MPE_LOG_TRACE1,
                        MPE_MOD_SI,
                        "<%s::sitp_psiWorkerThread> - Received MPE_TUNE_FAIL Event...tuner_number: %d\\n",
                        PSIMODULE, event_tuner_number);

                process_transport_stream_unavailable(event_tuner_number-1);
            }
            break;

            default:
                MPE_LOG(MPE_LOG_TRACE2,
                        MPE_MOD_SI,
                        "<%s::sitp_psiWorkerThread> - received unknown event...\n",
                        PSIMODULE);
                break;
        }

        /*
         ** Release global mutex
         */
        mpe_mutexRelease(g_sitp_psi_mutex);

        // Wait until the next event
        retCode = mpeos_eventQueueWaitNext(g_sitp_psi_queue, &psi_event,
                &poptional_eventdata1, &poptional_eventdata2, &event_data3, 0);

        if (SITP_PSI_EVENT_SHUTDOWN == psi_event)
        {
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                    "<%s::sitp_psiWorkerThread> - PSI Thread shutting down.\n",
                    PSIMODULE);
            //delete the psi queue
            (void) mpeos_mediaUnregisterQueue(g_sitp_psi_queue);
            (void) mpeos_eventQueueDelete(g_sitp_psi_queue);
            g_sitp_psi_queue = 0;
            g_shutDown = TRUE;
        }

        MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                "<%s::sitp_psiWorkerThread> ...\n\n\n", PSIMODULE);
    } /* End while( FALSE == g_shutDown ) */

    if (0 != g_sitp_psi_queue)
    {
        MPE_LOG(
                MPE_LOG_WARN,
                MPE_MOD_SI,
                "<%s::sitp_psiWorkerThread> - PSI Thread shutting down - global shutdown flag set to TRUE.\n",
                PSIMODULE);
        (void) mpeos_mediaUnregisterQueue(g_sitp_psi_queue);
        (void) mpeos_eventQueueDelete(g_sitp_psi_queue);
        g_sitp_psi_queue = 0;
    }
}

// Initialize filter groups based on ini settings
// The modes are described in the mpeenv.ini file
static mpe_Error init_filter_mode()
{
    int i=0;
    mpe_Error retCode = MPE_SUCCESS;
    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::init_filter_mode> - init_filter_mode enter..%d\n", PSIMODULE, g_sitp_psi_filtering_mode);

    switch(g_sitp_psi_filtering_mode)
    {
        // TODO: Name the modes appropriately..
        case 1: // one_filter_for_all_mode (legacy mode)
        {
            // Create filter group (only one)
            mpe_FilterGroup filterGroup = MPE_FILTERGROUP_INVALID_GROUP;

            retCode = filter_createFilterGroup( g_sitp_ib_psi_round_robin_interval,
                                                "MPE_legacy_filter_group_mode_1",
                                                &filterGroup);
            if(retCode == MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
                        "<%s::init_filter_mode> - filter_createFilterGroup returned success. filterGroup:0x%x\n", PSIMODULE, filterGroup);
                // Filter group is started when tune is initiated
                for(i=0; i< g_sitp_psi_num_tuners; i++)
                {
                    // Assign to members
                    g_IB_psi_data_array[i].fsm_data->filter_group_initial_PAT = filterGroup;
                    g_IB_psi_data_array[i].fsm_data->filter_group_initial_primary_PMT = filterGroup;
                    g_IB_psi_data_array[i].fsm_data->filter_group_initial_secondary_PMT = filterGroup;
                    if(g_sitp_ib_psi_process_table_revision == TRUE)
                    {
                        g_IB_psi_data_array[i].fsm_data->filter_group_revision_PAT = filterGroup;
                        g_IB_psi_data_array[i].fsm_data->filter_group_revision_primary_PMT = filterGroup;
                        g_IB_psi_data_array[i].fsm_data->filter_group_revision_secondary_PMT = filterGroup;
                    }
                }
            }
        }
        break;
        case 2: // one_filter_per_tuner_mode
        {
            // Create one filter group per tuner
            for(i=0; i< g_sitp_psi_num_tuners; i++)
            {
                // Create filter group (one per tuner in this mode)
                char filterGroupName[256] = "";
                mpe_FilterGroup filterGroup = MPE_FILTERGROUP_INVALID_GROUP;

                sprintf(filterGroupName, "MPE_filter_group_mode_2_tuner_%d", i+1);
                MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,
                        "<%s::init_filter_mode> - filterGroupName:%s\n", PSIMODULE, filterGroupName);

                retCode = filter_createFilterGroup( g_sitp_ib_psi_round_robin_interval,
                                                    filterGroupName,
                                                    &filterGroup);
                if(retCode == MPE_SUCCESS)
                {
                    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
                            "<%s::init_filter_mode> - filter_createFilterGroup returned success. filterGroup:0x%x\n", PSIMODULE, filterGroup);
                }
                // Filter group is started when tune is initiated

                // Assign to members
                g_IB_psi_data_array[i].fsm_data->filter_group_initial_PAT = filterGroup;
                g_IB_psi_data_array[i].fsm_data->filter_group_initial_primary_PMT = filterGroup;
                g_IB_psi_data_array[i].fsm_data->filter_group_initial_secondary_PMT = filterGroup;

                if(g_sitp_ib_psi_process_table_revision == TRUE)
                {
                    g_IB_psi_data_array[i].fsm_data->filter_group_revision_PAT = filterGroup;
                    g_IB_psi_data_array[i].fsm_data->filter_group_revision_primary_PMT = filterGroup;
                    g_IB_psi_data_array[i].fsm_data->filter_group_revision_secondary_PMT = filterGroup;
                }
            }
        }
        break;
        case 3: // two_filters_per_tuner_no_secondary_pmt
        {
            // Create one filter group per tuner
            for(i=0; i< g_sitp_psi_num_tuners; i++)
            {
                // Create filter group (two per tuner in this mode)
                char filterGroupName[256] = "";
                mpe_FilterGroup filterGroup1 = MPE_FILTERGROUP_INVALID_GROUP;
                mpe_FilterGroup filterGroup2 = MPE_FILTERGROUP_INVALID_GROUP;
                sprintf(filterGroupName, "MPE_filter_group_mode_3_tuner_%d_PAT", i+1);
                MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,
                        "<%s::init_filter_mode> - filterGroupName:%s\n", PSIMODULE, filterGroupName);

                retCode = filter_createFilterGroup( g_sitp_ib_psi_round_robin_interval,
                                                    filterGroupName,
                                                    &filterGroup1);
                if(retCode == MPE_SUCCESS)
                {
                    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
                            "<%s::init_filter_mode> - filter_createFilterGroup returned success. filterGroup1:0x%x\n", PSIMODULE, filterGroup1);
                }
                // Filter group is started when tune is initiated

                sprintf(filterGroupName, "MPE_filter_group_mode_3_tuner_%d_PMT", i+1);
                MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,
                        "<%s::init_filter_mode> - filterGroupName:%s\n", PSIMODULE, filterGroupName);

                retCode = filter_createFilterGroup( g_sitp_ib_psi_round_robin_interval,
                                                    filterGroupName,
                                                    &filterGroup2);
                if(retCode == MPE_SUCCESS)
                {
                    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
                            "<%s::init_filter_mode> - filter_createFilterGroup returned success. filterGroup2:0x%x\n", PSIMODULE, filterGroup2);
                }
                // Assign to members
                g_IB_psi_data_array[i].fsm_data->filter_group_initial_PAT = filterGroup1;
                g_IB_psi_data_array[i].fsm_data->filter_group_initial_primary_PMT = filterGroup2;
                g_IB_psi_data_array[i].fsm_data->filter_group_initial_secondary_PMT = NULL;

                if(g_sitp_ib_psi_process_table_revision == TRUE)
                {
                    g_IB_psi_data_array[i].fsm_data->filter_group_revision_PAT = filterGroup1;
                    g_IB_psi_data_array[i].fsm_data->filter_group_revision_primary_PMT = filterGroup2;
                    g_IB_psi_data_array[i].fsm_data->filter_group_revision_secondary_PMT = NULL;
                }
            }
        }
        break;
        case 4: // one_filter_per_tuner_plus_one_secondary_pmt_filter
        {
            // We create only one secondary filter to be shared between
            // all tuners
            mpe_FilterGroup filterGroupSec = MPE_FILTERGROUP_INVALID_GROUP;
            char filterGroupName[256] = "";
            {
                sprintf(filterGroupName, "MPE_filter_group_mode_4_sec");
                MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,
                        "<%s::init_filter_mode> - filterGroupName:%s\n", PSIMODULE, filterGroupName);

                retCode = filter_createFilterGroup( g_sitp_ib_psi_round_robin_interval,
                                                    filterGroupName,
                                                    &filterGroupSec);
                if(retCode == MPE_SUCCESS)
                {
                    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
                            "<%s::init_filter_mode> - filter_createFilterGroup returned success. filterGroupSec:0x%x\n", PSIMODULE, filterGroupSec);
                }
                // Filter group is started when tune is initiated
            }

            // Create one filter group per tuner
            for(i=0; i< g_sitp_psi_num_tuners; i++)
            {
                // Create filter group (one per tuner in this mode)
                mpe_FilterGroup filterGroup = MPE_FILTERGROUP_INVALID_GROUP;

                sprintf(filterGroupName, "MPE_filter_group_mode_4_tuner_%d", i+1);
                MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,
                        "<%s::init_filter_mode> - filterGroupName:%s\n", PSIMODULE, filterGroupName);

                retCode = filter_createFilterGroup( g_sitp_ib_psi_round_robin_interval,
                                                    filterGroupName,
                                                    &filterGroup);
                if(retCode == MPE_SUCCESS)
                {
                    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
                            "<%s::init_filter_mode> - filter_createFilterGroup returned success. filterGroup:0x%x\n", PSIMODULE, filterGroup);
                }
                // Filter group is started when tune is initiated

                // Assign to members
                g_IB_psi_data_array[i].fsm_data->filter_group_initial_PAT = filterGroup;
                g_IB_psi_data_array[i].fsm_data->filter_group_initial_primary_PMT = filterGroup;
                g_IB_psi_data_array[i].fsm_data->filter_group_initial_secondary_PMT = filterGroupSec;

                if(g_sitp_ib_psi_process_table_revision == TRUE)
                {
                    g_IB_psi_data_array[i].fsm_data->filter_group_revision_PAT = filterGroup;
                    g_IB_psi_data_array[i].fsm_data->filter_group_revision_primary_PMT = filterGroup;
                    g_IB_psi_data_array[i].fsm_data->filter_group_revision_secondary_PMT = filterGroupSec;
                }
            }
        }
        break;
        case 5: // two_filters_per_tuner_plus_one_secondary_pmt_filter
        {
            char filterGroupName[256] = "";
            mpe_FilterGroup filterGroupSec = MPE_FILTERGROUP_INVALID_GROUP;
            // We create only one secondary filter to be shared between
            // all tuners
            {
                sprintf(filterGroupName, "MPE_filter_group_mode_5_sec");
                MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,
                        "<%s::init_filter_mode> - filterGroupName:%s\n", PSIMODULE, filterGroupName);

                retCode = filter_createFilterGroup( g_sitp_ib_psi_round_robin_interval,
                                                    filterGroupName,
                                                    &filterGroupSec);
                if(retCode == MPE_SUCCESS)
                {
                    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
                            "<%s::init_filter_mode> - filter_createFilterGroup returned success. filterGroupSec:0x%x\n", PSIMODULE, filterGroupSec);
                }
                // Filter group is started when tune is initiated
            }
            // Create two filter groups per tuner
            for(i=0; i< g_sitp_psi_num_tuners; i++)
            {
                // Create filter group (two per tuner in this mode)
                mpe_FilterGroup filterGroup1 = MPE_FILTERGROUP_INVALID_GROUP;
                mpe_FilterGroup filterGroup2 = MPE_FILTERGROUP_INVALID_GROUP;
                sprintf(filterGroupName, "MPE_filter_group_mode_3_tuner_%d_PAT", i+1);
                MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,
                        "<%s::init_filter_mode> - filterGroupName:%s\n", PSIMODULE, filterGroupName);

                retCode = filter_createFilterGroup( g_sitp_ib_psi_round_robin_interval,
                                                    filterGroupName,
                                                    &filterGroup1);
                if(retCode == MPE_SUCCESS)
                {
                    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
                            "<%s::init_filter_mode> - filter_createFilterGroup returned success. filterGroup1:0x%x\n", PSIMODULE, filterGroup1);
                }
                // Filter group is started when tune is initiated

                sprintf(filterGroupName, "MPE_filter_group_mode_5_tuner_%d_PMT", i+1);
                MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,
                        "<%s::init_filter_mode> - filterGroupName:%s\n", PSIMODULE, filterGroupName);

                retCode = filter_createFilterGroup( g_sitp_ib_psi_round_robin_interval,
                                                    filterGroupName,
                                                    &filterGroup2);
                if(retCode == MPE_SUCCESS)
                {
                    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
                            "<%s::init_filter_mode> - filter_createFilterGroup returned success. filterGroup2:0x%x\n", PSIMODULE, filterGroup2);
                }

                // Assign to members
                g_IB_psi_data_array[i].fsm_data->filter_group_initial_PAT = filterGroup1;
                g_IB_psi_data_array[i].fsm_data->filter_group_initial_primary_PMT = filterGroup2;
                g_IB_psi_data_array[i].fsm_data->filter_group_initial_secondary_PMT = filterGroupSec;

                if(g_sitp_ib_psi_process_table_revision == TRUE)
                {
                    g_IB_psi_data_array[i].fsm_data->filter_group_revision_PAT = filterGroup1;
                    g_IB_psi_data_array[i].fsm_data->filter_group_revision_primary_PMT = filterGroup2;
                    g_IB_psi_data_array[i].fsm_data->filter_group_revision_secondary_PMT = filterGroupSec;
                }
            }
        }
        break;
        default: // one_filter_for_each_type (platforms with unlimited filter resources)
            break;
    }
    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::init_filter_mode> - init_filter_mode exit..\n", PSIMODULE);

    return retCode;
}

/*
 * Initialize IB tuner PSI data
 */
static mpe_Error createAndSetTunerPSIDataArray()
{
    mpe_Error retCode = SITP_SUCCESS;
    uint32_t tuner_index;

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::createAndSetTunerPSIDataArray> - Number of Tuners: %d\n", PSIMODULE, g_sitp_psi_num_tuners);

    /*
     * allocate the tuner array.
     */
    retCode = mpeos_memAllocP(MPE_MEM_SI, (sizeof(sitp_psi_data_t) * g_sitp_psi_num_tuners),
                              (void **) &(g_IB_psi_data_array));
    if (SITP_SUCCESS != retCode)
    {
        MPE_LOG(MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::createAndSetTunePSIDataArray> - ERROR: Failed to allocate tuner PSI array . retCode: %d\n",
                PSIMODULE, retCode);
        return SITP_FAILURE;
    }

    /*
     ** Initialize tuner PSI data array
     */
    for (tuner_index = 0; tuner_index < g_sitp_psi_num_tuners; tuner_index++)
    {
        g_IB_psi_data_array[tuner_index].tuner_type = SITP_PSI_TUNER_TYPE_IB;
        g_IB_psi_data_array[tuner_index].tuner_state = SITP_PSI_TUNER_STATE_IDLE;

        // Tuner index starts at 1
        g_IB_psi_data_array[tuner_index].psi_params.p_ib_tune_info.tuner_id = tuner_index+1;
        g_IB_psi_data_array[tuner_index].psi_params.p_ib_tune_info.frequency = 0;
        g_IB_psi_data_array[tuner_index].psi_params.p_ib_tune_info.program_number = 0;
        g_IB_psi_data_array[tuner_index].psi_params.p_ib_tune_info.modulation_mode = MPE_SI_MODULATION_UNKNOWN;

        /* The PSI data struct last tune info struct */
        g_IB_psi_data_array[tuner_index].psi_params.p_ib_tune_info.last_tune_info.frequency = 0;
        g_IB_psi_data_array[tuner_index].psi_params.p_ib_tune_info.last_tune_info.program_number = 0;
        g_IB_psi_data_array[tuner_index].psi_params.p_ib_tune_info.last_tune_info.modulation_mode = MPE_SI_MODULATION_UNKNOWN;
        g_IB_psi_data_array[tuner_index].psi_params.p_ib_tune_info.last_tune_info.cleanup_frequency = 0;
        g_IB_psi_data_array[tuner_index].psi_params.p_ib_tune_info.last_tune_info.cleanup_program_number  = 0;
        g_IB_psi_data_array[tuner_index].psi_params.p_ib_tune_info.last_tune_info.cleanup_modulation_mode = MPE_SI_MODULATION_UNKNOWN;

        retCode = mpeos_memAllocP(MPE_MEM_SI, sizeof(sitp_psi_fsm_data_t),
                                  (void **) &(g_IB_psi_data_array[tuner_index].fsm_data));
        if (SITP_SUCCESS != retCode)
        {
            MPE_LOG(MPE_LOG_ERROR,
                    MPE_MOD_SI,
                    "<%s::createAndSetTunePSIDataArray> - ERROR: Failed to allocate tuner FSM data . retCode: %d\n",
                    PSIMODULE, retCode);
            return SITP_FAILURE;
        }

        init_fsm_data(g_IB_psi_data_array[tuner_index].fsm_data);

        // 'psi_data' is set after this call returns
        g_IB_psi_data_array[tuner_index].fsm_data->shared_filter_PAT.fsm_data = (void *)g_IB_psi_data_array[tuner_index].fsm_data;
        g_IB_psi_data_array[tuner_index].fsm_data->shared_filter_primary_PMT.fsm_data = (void *)g_IB_psi_data_array[tuner_index].fsm_data;

        g_IB_psi_data_array[tuner_index].fsm_data->psi_data = (void *)(&g_IB_psi_data_array[tuner_index]);

        g_IB_psi_data_array[tuner_index].fsm_data->sitp_psi_pat_timeout_val = g_sitp_ib_pat_timeout_interval;
        g_IB_psi_data_array[tuner_index].fsm_data->sitp_psi_pmt_timeout_val = g_sitp_ib_pmt_timeout_interval;
        g_IB_psi_data_array[tuner_index].fsm_data->sitp_psi_round_robin_val = g_sitp_ib_psi_round_robin_interval;

        //MPE_LOG(MPE_LOG_TRACE1,
        //        MPE_MOD_SI,
        //        "<%s::createAndSetTunePSIDataArray> - fsm_data: 0x%x fsm_data->psi_data: 0x%x\n",
        //        PSIMODULE, g_IB_psi_data_array[tuner_index].fsm_data->shared_filter_PAT.fsm_data,
        //        g_IB_psi_data_array[tuner_index].fsm_data->psi_data);

        g_IB_psi_data_array[tuner_index].fsm_data->filter_source.sourceType = MPE_FILTER_SOURCE_INB;
        // pid will be set at the time the filter is set (PAT/PMT)
        g_IB_psi_data_array[tuner_index].fsm_data->filter_source.pid = -1;
        g_IB_psi_data_array[tuner_index].fsm_data->filter_source.parm.p_INB.tunerId = tuner_index+1;
        g_IB_psi_data_array[tuner_index].fsm_data->filter_source.parm.p_INB.freq = 0;
        g_IB_psi_data_array[tuner_index].fsm_data->filter_source.parm.p_INB.tsId = 1; // this is always 1
    }

    init_filter_mode();

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::createAndSetTunePSIDataArray> - Exit\n", PSIMODULE);

    return retCode;
}

/*
 * Reset IB tuner PSI data
 */
static mpe_Error resetTunerPSIData(uint8_t index)
{
    mpe_Error retCode = SITP_SUCCESS;

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::resetTunerPSIDataArray> - tuner_index: %d\n", PSIMODULE, index+1);

    g_IB_psi_data_array[index].tuner_state = SITP_PSI_TUNER_STATE_IDLE;

    g_IB_psi_data_array[index].psi_params.p_ib_tune_info.frequency = 0;
    g_IB_psi_data_array[index].psi_params.p_ib_tune_info.program_number = 0;
    g_IB_psi_data_array[index].psi_params.p_ib_tune_info.modulation_mode = MPE_SI_MODULATION_UNKNOWN;

    /* The PSI data struct last tune info struct
    g_IB_psi_data_array[tuner_index].psi_params.p_ib_tune_info.last_tune_info.frequency = 0;
    g_IB_psi_data_array[tuner_index].psi_params.p_ib_tune_info.last_tune_info.program_number = 0;
    g_IB_psi_data_array[tuner_index].psi_params.p_ib_tune_info.last_tune_info.modulation_mode = MPE_SI_MODULATION_UNKNOWN;
    g_IB_psi_data_array[tuner_index].psi_params.p_ib_tune_info.last_tune_info.cleanup_frequency = 0;
    g_IB_psi_data_array[tuner_index].psi_params.p_ib_tune_info.last_tune_info.cleanup_program_number  = 0;
    g_IB_psi_data_array[tuner_index].psi_params.p_ib_tune_info.last_tune_info.cleanup_modulation_mode = MPE_SI_MODULATION_UNKNOWN;
    */

    clear_fsm_data(g_IB_psi_data_array[index].fsm_data);

    g_IB_psi_data_array[index].fsm_data->filter_source.sourceType = MPE_FILTER_SOURCE_INB;
    // pid will be set at the time the filter is set (PAT/PMT)
    g_IB_psi_data_array[index].fsm_data->filter_source.pid = -1;
    g_IB_psi_data_array[index].fsm_data->filter_source.parm.p_INB.tunerId = index+1;
    g_IB_psi_data_array[index].fsm_data->filter_source.parm.p_INB.freq = 0;
    g_IB_psi_data_array[index].fsm_data->filter_source.parm.p_INB.tsId = 1; // this is always 1

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::resetTunerPSIDataArray> - Exit\n", PSIMODULE);

    return retCode;
}

/*
 * Reset PSI data
 */
static mpe_Error resetPSIData(sitp_psi_tuner_type_e tuner_type, ListSI psi_data_array)
{
    mpe_Error retCode = SITP_SUCCESS;
    int i=0, count = 0;
    MPE_LOG(MPE_LOG_TRACE1,
            MPE_MOD_SI,
            "<%s::resetPSIData> - type: %d\n",
            PSIMODULE, tuner_type);

    if(tuner_type == SITP_PSI_TUNER_TYPE_DSG)
    {
        count = g_sitp_psi_DSG_count;
    }
    else if(tuner_type == SITP_PSI_TUNER_TYPE_HN)
    {
        count = g_sitp_psi_HN_count;
    }

    for(i=0; i<count; i++)
    {
        if(psi_data_array)
        {
            LINK *lp1 = llist_first(psi_data_array);
            while (lp1)
            {
                sitp_psi_data_t *psi_data = (sitp_psi_data_t *) llist_getdata(lp1);
                if(psi_data)
                {
                    // De-allocate memory associated with this node (fsm_data etc.)
                    llist_rmlink(lp1);

                    if(psi_data->fsm_data->shared_filter_secondary_PMTs)
                    {
                        LINK *lp2 = llist_first(psi_data->fsm_data->shared_filter_secondary_PMTs);
                        while(lp2)
                        {
                            // de-allocate psi_filter allocations
                            sitp_psi_filter_t *psi_filter = llist_getdata(lp2);
                            if (psi_filter)
                            {
                                llist_rmlink(lp2);
                                mpeos_memFreeP(MPE_MEM_SI, psi_filter);
                            }
                            lp2 = llist_after(lp2);
                        }
                    }
                    retCode = mpeos_memFreeP(MPE_MEM_SI, psi_data);
                    if (SITP_SUCCESS != retCode)
                    {
                        MPE_LOG(MPE_LOG_ERROR,
                                MPE_MOD_SI,
                                "<%s::resetPSIData> - ERROR: Failed to free PSI data retCode: %d\n",
                                PSIMODULE, retCode);
                    }
                }
                lp1 = llist_after(lp1);
            }
        }
    }
    return retCode;
}

/*
 * Initialize FSM data array
 */
static mpe_Error init_fsm_data(sitp_psi_fsm_data_t *fsm_data)
{
    mpe_Error retCode = SITP_SUCCESS;

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,
            "<%s::init_fsm_data> - Enter fsm_data:0x%x\n", PSIMODULE, fsm_data);

    fsm_data->psi_state = SITP_PSI_IDLE;
    fsm_data->ts_handle = MPE_SI_INVALID_HANDLE;

    fsm_data->shared_filter_PAT.table_type = TABLE_TYPE_PAT;
    fsm_data->shared_filter_PAT.pid = 0x0;
    fsm_data->shared_filter_PAT.version_number = INIT_TABLE_VERSION;
    fsm_data->shared_filter_PAT.transport_stream_id = 1;
    fsm_data->shared_filter_PAT.filter_set = FALSE;
    fsm_data->shared_filter_PAT.filter_priority = SITP_PSI_FILTER_PRIORITY_INITIAL_PAT;
    fsm_data->shared_filter_PAT.program_number = PROGRAM_NUMBER_UNKNOWN;
    fsm_data->shared_filter_PAT.shared_filterId = MPE_FILTERGROUP_INVALID_FILTER;
    fsm_data->shared_filter_PAT.fsm_data = NULL;

    fsm_data->shared_filter_primary_PMT.table_type = TABLE_TYPE_PMT;
    fsm_data->shared_filter_primary_PMT.pid = -1;
    fsm_data->shared_filter_primary_PMT.version_number = INIT_TABLE_VERSION;
    fsm_data->shared_filter_primary_PMT.transport_stream_id = 1;
    fsm_data->shared_filter_primary_PMT.filter_set = FALSE;
    fsm_data->shared_filter_primary_PMT.filter_priority = SITP_PSI_FILTER_PRIORITY_INITIAL_PRIMARY_PMT;
    fsm_data->shared_filter_primary_PMT.program_number = PROGRAM_NUMBER_UNKNOWN;
    fsm_data->shared_filter_primary_PMT.shared_filterId = MPE_FILTERGROUP_INVALID_FILTER;
    fsm_data->shared_filter_primary_PMT.fsm_data = NULL;

    // This is a list of secondary PMT filters
    // Filters are created at the time PAT is parsed
    // depending on the number of PMTs. List will be
    // populated at that time.
    // TODO: Initialize secondary PMT filter fields
    fsm_data->shared_filter_secondary_PMTs = llist_create();

    fsm_data->filter_group_initial_PAT = MPE_FILTERGROUP_INVALID_GROUP;
    fsm_data->filter_group_initial_primary_PMT = MPE_FILTERGROUP_INVALID_GROUP;
    fsm_data->filter_group_revision_PAT = MPE_FILTERGROUP_INVALID_GROUP;
    fsm_data->filter_group_revision_primary_PMT = MPE_FILTERGROUP_INVALID_GROUP;
    fsm_data->filter_group_initial_secondary_PMT = MPE_FILTERGROUP_INVALID_GROUP;
    fsm_data->filter_group_revision_secondary_PMT = MPE_FILTERGROUP_INVALID_GROUP;

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::init_fsm_data> - Exit\n", PSIMODULE);

    return retCode;
}

/*
 * Reset FSM data array
 */
static mpe_Error clear_fsm_data(sitp_psi_fsm_data_t *fsm_data)
{
    mpe_Error retCode = SITP_SUCCESS;

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::clear_fsm_data> - Enter\n", PSIMODULE);

    fsm_data->psi_state = SITP_PSI_IDLE;
    fsm_data->ts_handle = MPE_SI_INVALID_HANDLE;

    fsm_data->shared_filter_PAT.table_type = TABLE_TYPE_PAT;
    fsm_data->shared_filter_PAT.pid = 0x0;
    fsm_data->shared_filter_PAT.version_number = INIT_TABLE_VERSION;
    fsm_data->shared_filter_PAT.transport_stream_id = 1;
    fsm_data->shared_filter_PAT.filter_set = FALSE;
    fsm_data->shared_filter_PAT.filter_priority = SITP_PSI_FILTER_PRIORITY_INITIAL_PAT;
    fsm_data->shared_filter_PAT.program_number = PROGRAM_NUMBER_UNKNOWN;
    fsm_data->shared_filter_PAT.shared_filterId = MPE_FILTERGROUP_INVALID_FILTER;

    fsm_data->shared_filter_primary_PMT.table_type = TABLE_TYPE_PMT;
    fsm_data->shared_filter_primary_PMT.pid = -1;
    fsm_data->shared_filter_primary_PMT.version_number = INIT_TABLE_VERSION;
    fsm_data->shared_filter_primary_PMT.transport_stream_id = 1;
    fsm_data->shared_filter_primary_PMT.filter_set = FALSE;
    fsm_data->shared_filter_primary_PMT.filter_priority = SITP_PSI_FILTER_PRIORITY_INITIAL_PRIMARY_PMT;
    fsm_data->shared_filter_primary_PMT.program_number = PROGRAM_NUMBER_UNKNOWN;
    fsm_data->shared_filter_primary_PMT.shared_filterId = MPE_FILTERGROUP_INVALID_FILTER;

    // This is a list of secondary PMT filters
    clear_secondary_PMT_list(fsm_data);

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::clear_fsm_data> - Exit\n", PSIMODULE);

    return retCode;
}

/*
 * Clear the secondary PMT filter list
 */
static mpe_Error clear_secondary_PMT_list(sitp_psi_fsm_data_t *fsm_data)
{
    mpe_Error retCode = SITP_SUCCESS;

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::clear_secondary_PMT_list> - llist_cnt(fsm_data->shared_filter_secondary_PMTs): %d\n", PSIMODULE, llist_cnt(fsm_data->shared_filter_secondary_PMTs));

    // This is a list of secondary PMT filters
    if(llist_cnt(fsm_data->shared_filter_secondary_PMTs) > 0)
    {
        LINK *lp = llist_first(fsm_data->shared_filter_secondary_PMTs);
        while(lp)
        {
            // de-allocate psi_filter allocations
            sitp_psi_filter_t *psi_filter = llist_getdata(lp);
            if (psi_filter)
            {
                // llist_free() below removes all nodes in the list
                // so free the memory allocated for psi_filter
                mpeos_memFreeP(MPE_MEM_SI, psi_filter);
                psi_filter = NULL;
            }
            lp = llist_after(lp);
        }
    }
    llist_free(fsm_data->shared_filter_secondary_PMTs);

    // Create empty list
    fsm_data->shared_filter_secondary_PMTs = llist_create();

    return retCode;
}

/*
 * Initialize OOB PSI data array
 */
static mpe_Error createAndSetOOBPSIDataArray()
{
    mpe_Error retCode = SITP_SUCCESS;

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::createAndSetOOBPSIDataArray> - Enter\n", PSIMODULE);

    /*
     * allocate the tuner array. (one for OOB)
     */
    retCode = mpeos_memAllocP(MPE_MEM_SI,
            (sizeof(sitp_psi_data_t)), (void **) &g_OOB_psi_data);
    if (SITP_SUCCESS != retCode)
    {
        MPE_LOG(MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::createAndSetOOBTransportStreamDataArray> - ERROR: Failed to allocate transport stream array . retCode: %d\n",
                PSIMODULE, retCode);
        return SITP_FAILURE;
    }

    /*
     ** Add the appropriate data to the each FSM data array
     */
    g_OOB_psi_data->tuner_type = SITP_PSI_TUNER_TYPE_OOB;
    g_OOB_psi_data->tuner_state = SITP_PSI_TUNER_STATE_TUNED;
    g_OOB_psi_data->psi_params.p_oob_info.ts_id = 1; // ??

    retCode = mpeos_memAllocP(MPE_MEM_SI,
            sizeof(sitp_psi_fsm_data_t), (void **) &(g_OOB_psi_data->fsm_data));
    if (SITP_SUCCESS != retCode)
    {
        MPE_LOG(MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::createAndSetOOBTransportStreamDataArray> - ERROR: Failed to allocate transport stream array . retCode: %d\n",
                PSIMODULE, retCode);
        return SITP_FAILURE;
    }

    init_fsm_data(g_OOB_psi_data->fsm_data);

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::createAndSetOOBPSIDataArray> - g_OOB_psi_data->fsm_data: 0x%x\n", PSIMODULE, g_OOB_psi_data->fsm_data);

    // 'psi_data' is set after this call returns
    g_OOB_psi_data->fsm_data->shared_filter_PAT.fsm_data = (void *)g_OOB_psi_data->fsm_data;
    g_OOB_psi_data->fsm_data->shared_filter_primary_PMT.fsm_data = (void *)g_OOB_psi_data->fsm_data;

    g_OOB_psi_data->fsm_data->psi_data = (void *)(g_OOB_psi_data);

    g_OOB_psi_data->fsm_data->sitp_psi_pat_timeout_val = g_sitp_oob_pat_timeout_interval;
    g_OOB_psi_data->fsm_data->sitp_psi_pmt_timeout_val = g_sitp_oob_pmt_timeout_interval;
    g_OOB_psi_data->fsm_data->sitp_psi_round_robin_val = 0;

    // Set the filter source fields here
    g_OOB_psi_data->fsm_data->filter_source.sourceType = MPE_FILTER_SOURCE_OOB;
    g_OOB_psi_data->fsm_data->filter_source.pid = -1;
    g_OOB_psi_data->fsm_data->filter_source.parm.p_INB.tunerId = 0; // ??
    g_OOB_psi_data->fsm_data->filter_source.parm.p_INB.freq = MPE_SI_OOB_FREQUENCY;
    g_OOB_psi_data->fsm_data->filter_source.parm.p_INB.tsId = 1; // this is always 1

    // Set the filter group parameters to correct values
    // For OB create six discrete filter groups and assign
    g_OOB_psi_data->fsm_data->shared_filter_PAT.shared_filterId = MPE_FILTERGROUP_INVALID_FILTER;
    g_OOB_psi_data->fsm_data->shared_filter_primary_PMT.shared_filterId = MPE_FILTERGROUP_INVALID_FILTER;

    // Secondary PMT filter fields are set up when PAT is parsed
    g_OOB_psi_data->fsm_data->shared_filter_secondary_PMTs = llist_create();

    // OOB filter groups are created with no timeout. no TDM is performed in this case
    retCode = filter_createFilterGroup( 0,
                                        "MPE_oob_filter_group_init_pat",
                                        &g_OOB_psi_data->fsm_data->filter_group_initial_PAT);
    retCode = filter_createFilterGroup( 0,
                                        "MPE_oob_filter_group_init_primary_pmt",
                                        &g_OOB_psi_data->fsm_data->filter_group_initial_primary_PMT);
    retCode = filter_createFilterGroup( 0,
                                        "MPE_oob_filter_group_init_secondary_pmt",
                                        &g_OOB_psi_data->fsm_data->filter_group_initial_secondary_PMT );
    retCode = filter_startFilterGroup(g_OOB_psi_data->fsm_data->filter_group_initial_PAT);
    retCode = filter_startFilterGroup(g_OOB_psi_data->fsm_data->filter_group_initial_primary_PMT);
    retCode = filter_startFilterGroup(g_OOB_psi_data->fsm_data->filter_group_initial_secondary_PMT);
    if(retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
                "<%s::createAndSetOOBTransportStreamDataArray> - OOB filter groups failed to be created..\n", PSIMODULE);
    }

    if(g_sitp_oob_psi_process_table_revision == TRUE)
    {
        retCode = filter_createFilterGroup( 0,
                                            "MPE_oob_filter_group_pat_rev",
                                            &g_OOB_psi_data->fsm_data->filter_group_revision_PAT);
        retCode = filter_createFilterGroup( 0,
                                            "MPE_oob_filter_group_primary_pmt_rev",
                                            &g_OOB_psi_data->fsm_data->filter_group_revision_primary_PMT);
        retCode = filter_createFilterGroup( 0,
                                            "MPE_oob_filter_group_secondary_pmt_rev",
                                            &g_OOB_psi_data->fsm_data->filter_group_revision_secondary_PMT);

        retCode = filter_startFilterGroup(g_OOB_psi_data->fsm_data->filter_group_revision_PAT);
        retCode = filter_startFilterGroup(g_OOB_psi_data->fsm_data->filter_group_revision_primary_PMT);
        retCode = filter_startFilterGroup(g_OOB_psi_data->fsm_data->filter_group_revision_secondary_PMT);
    }

    if(retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
                "<%s::createAndSetOOBTransportStreamDataArray> - OOB filter groups failed to be started..\n", PSIMODULE);
    }

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::createAndSetOOBTransportStreamDataArray> - Exit\n", PSIMODULE);

    return retCode;
}

static mpe_Error set_filter(sitp_psi_data_t *psi_data, mpe_FilterGroup filterGroup, uint8_t priority, uint32_t timeout, sitp_psi_filter_t *psi_filter)
{
    mpe_Error retCode = SITP_SUCCESS;
    mpe_FilterSource filterSource;
    mpe_FilterSpec filterSpec;

    uint32_t userData = 0x0;

    /*
     * PAT/PMT table filter component values - last bit reviewed
     * (0x3F instead of 0x3E)so as to ignore a current_next_indicator
     * of '0'.
     * From ISO/IEC 13818-1
     * Table 2-25 - Program association section - current_next_indicator -
     * A 1-bit indicator, which when set to '1' indicates that the
     * Program Association Table sent is currently applicable.
     *
     * Table 2-28 - Transport Stream program map section -
     * current_next_indicator - A 1-bit field, which when set
     * to '1' indicates that the TS_program_map_section sent is
     * currently applicable.
     *
     */
    uint8_t tid_mask[] = { 0xFF, 0, 0, 0, 0, 0x01 };
    uint8_t pat_val[]  = { 0,    0, 0, 0, 0, 0x01 };
    uint8_t pmt_mask[] = { 0xFF, 0, 0, 0xFF, 0xFF, 0x01 };
    uint8_t pmt_val[]  = { 0x02, 0, 0, 0, 0, 0x01 };
    uint8_t ver_mask[] = { 0,    0, 0, 0, 0, 0x3E };
    uint8_t ver_val[]  = { 0,    0, 0, 0, 0, (INIT_TABLE_VERSION << 1) };

    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI, "<%s::activate_filter> - Enter - table_type = %d\n", PSIMODULE,
            psi_filter->table_type);

    /* Set the filterspec values */
    filterSpec.pos.length = 6;
    filterSpec.neg.mask = ver_mask;
    filterSpec.neg.vals = ver_val;

    switch (psi_filter->table_type)
    {
        case TABLE_TYPE_PAT:
            MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                    "<%s::activate_filter> - Setting a PAT table filter\n",
                    PSIMODULE);
            filterSpec.pos.vals = pat_val;
            filterSpec.pos.mask = tid_mask;
            break;
        case TABLE_TYPE_PMT:
        {
            /* Set the filterspec values */
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,
                    "<%s::activate_filter> - Setting a PMT table filter for pn: %d (0x%x) pid:0x%x\n",
                    PSIMODULE, psi_filter->program_number, psi_filter->program_number, psi_filter->pid);
            pmt_val[3] = psi_filter->program_number >> 8;
            pmt_val[4] = psi_filter->program_number;
            //MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,
            //        "<%s::activate_filter> - Setting PMT table filter for pn: 0x%x %x\n",
            //        PSIMODULE,  pmt_val[3], pmt_val[4]);
            filterSpec.pos.mask = pmt_mask;
            filterSpec.pos.vals = pmt_val;
        }
            break;
        default:
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI,
                    "<%s::activate_filter> - ERROR: Unknown table type\n",
                    PSIMODULE);
        break;
    }

    if (psi_filter->version_number == INIT_TABLE_VERSION)
    {
        filterSpec.neg.length = 0;
        MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                "<%s::activate_filter> - NOT setting a neg version filter\n",
                PSIMODULE);
    }
    else
    {
        /* 6 elements in the neg array*/
        filterSpec.neg.length = 6;
        /* Set the version Number value at the version index*/
        filterSpec.neg.vals[PSI_VERSION_INDEX] = (uint8_t)(psi_filter->version_number << 1);

        MPE_LOG(MPE_LOG_TRACE2,
                MPE_MOD_SI,
                "<%s::activate_filter> - Setting a neg version filter: version == %d\n",
                PSIMODULE, psi_filter->version_number);
    }

    /* Set the remaining filter Source values */
    /* Set the remaining filter Source values */
    if (SITP_PSI_TUNER_TYPE_DSG == psi_data->tuner_type)
    {
        /* Set the OOB DSG filter source params */
        filterSource.sourceType = MPE_FILTER_SOURCE_DSG_APPID;
        filterSource.parm.p_DSGA.appId = psi_data->psi_params.p_dsg_info.dsg_app_id;
        MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                "<%s::activate_filter> - source type is DSG, appId: %d\n",
                PSIMODULE, psi_data->psi_params.p_dsg_info.dsg_app_id);

        userData =  ( (psi_filter->table_type << 8)
                     | (psi_filter->version_number) );
    }
    else if (SITP_PSI_TUNER_TYPE_HN == psi_data->tuner_type)
    {
        /* Set the HN filter source params */
        filterSource.sourceType = MPE_FILTER_SOURCE_HN_STREAM;
        filterSource.parm.p_HNS.hn_stream_session = psi_data->psi_params.p_hn_stream_info.hn_stream_session;
        MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                "<%s::activate_filter> - source type is HN_STREAM, session: %x\n",
                PSIMODULE, psi_data->psi_params.p_hn_stream_info.hn_stream_session);

        userData =  ( (psi_filter->table_type << 8)
                     | (psi_filter->version_number) );
    }
    else if (SITP_PSI_TUNER_TYPE_OOB == psi_data->tuner_type)
    {
        /* Set the OOB filter source params */
        filterSource.sourceType = MPE_FILTER_SOURCE_OOB;
        filterSource.parm.p_OOB.tsId = psi_data->psi_params.p_oob_info.ts_id;
        MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                "<%s::activate_filter> - source type is OOB\n", PSIMODULE);

        userData =  ( (psi_filter->table_type << 8)
                     | (psi_filter->version_number) );
    }
    else
    {

        MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                "<%s::activate_filter> - source type is INB\n", PSIMODULE);
        /* Set the INB filter source params */
        filterSource.sourceType = MPE_FILTER_SOURCE_INB;
        filterSource.parm.p_INB.tunerId = psi_data->psi_params.p_ib_tune_info.tuner_id;
        filterSource.parm.p_INB.freq = psi_data->psi_params.p_ib_tune_info.frequency;
        // Transport_stream_id field is not known when setting initial PAT filter
        // Is it used by section filter module?
        filterSource.parm.p_INB.tsId = psi_data->fsm_data->filter_source.parm.p_INB.tsId;

        userData = (  (psi_filter->program_number << 16)
                     | (psi_filter->table_type << 8)
                     | (psi_filter->version_number) );

        MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                "<%s::activate_filter> - psi_filter->program_number:0x%x\n", PSIMODULE, psi_filter->program_number);
        MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                "<%s::activate_filter> - psi_filter->table_type:0x%x\n", PSIMODULE, psi_filter->table_type);
        MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                "<%s::activate_filter> - psi_filter->version_number:0x%x\n", PSIMODULE, psi_filter->version_number);
        MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                "<%s::activate_filter> - userData:0x%x\n", PSIMODULE, userData);
    }
    filterSource.pid = psi_filter->pid;
    /* Set the source type independent filterSource values */
    MPE_LOG(MPE_LOG_TRACE2,
            MPE_MOD_SI,
            "<%s::activate_filter> - tuner: %d, table_type: %d, pid: 0x%x(%d) shared_filterId: 0x%x\n",
            PSIMODULE, psi_data->psi_params.p_ib_tune_info.tuner_id, psi_filter->table_type,
            psi_filter->pid,
            psi_filter->pid,
            psi_filter->shared_filterId);

    /* print the filter spec */
    printFilterSpec(filterSpec);

    {
        sitp_psi_fsm_data_t *fsm = (sitp_psi_fsm_data_t *)psi_filter->fsm_data;
        sitp_psi_data_t *psi_data = (sitp_psi_data_t *) fsm->psi_data;
        MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                "<%s::activate_filter> - userData psi_filter:0x%x\n", PSIMODULE, psi_filter);
        MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                "<%s::activate_filter> - userData fsm:0x%x\n", PSIMODULE, fsm);
        MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                "<%s::activate_filter> - userData psi_data:0x%x\n", PSIMODULE, psi_data);
    }
    /* Set the filter */
    retCode = filter_addSharedFilter( filterGroup,
                                      &filterSource,
                                      &filterSpec,
                                      1,                           // match count
                                      priority,                    // specific to table_type, filter type (initial/rev)
                                      timeout,                     // specific to table_type, filter type (initial/rev)
                                      (void*) psi_filter,          // userPointer
                                      userData,                    // userData
                                      mpe_sitpSharedFilterCallback,
                                      &(psi_filter->shared_filterId));

    if (retCode == MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_TRACE1,
                MPE_MOD_SI,
                "<%s::activate_filter> - filter_addSharedFilter returned success..\n",
                PSIMODULE);

        /* Set the remaining filter info fields.  The unique_id will be set in the callback function */
        psi_filter->filter_set =  TRUE;
        return SITP_SUCCESS;
    }

    if ( (MPE_SF_ERROR_TUNER_NOT_TUNED == retCode)
            || (MPE_SF_ERROR_TUNER_NOT_AT_FREQUENCY == retCode)
            || (MPE_EINVAL == retCode) )
    {
        /*
         ** Error. Print fatal msg and try to recover.
         */
        if (MPE_EINVAL == retCode)
        {
            MPE_LOG(MPE_LOG_ERROR,
                    MPE_MOD_SI,
                    "<%s::activate_filter> - Error - passed wrong values to activate filter\n",
                    PSIMODULE);
        }
        else
        {
            /*
             ** All other errors result in a NO_OP
             */
            MPE_LOG(MPE_LOG_ERROR,
                    MPE_MOD_SI,
                    "<%s::activate_filter> - Unable to activate filter - tuner_not_tuned or not @ freq - error = %d(0x%x).\n",
                    PSIMODULE, retCode, retCode);
            retCode = SITP_TUNER_PARAM_FAILURE;
        }
    }
    else
    {
        /*
         ** This may be recoverable, Handle it after the state transition.
         */
        MPE_LOG(MPE_LOG_WARN,
                MPE_MOD_SI,
                "<%s::activate_filter> - Unable to activate filter because of resource failure: error = %d(0x%x)\n",
                PSIMODULE, retCode, retCode);
        retCode = SITP_RESOURCE_FAILURE;
    }

    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI, "<%s::activate_filter> - Exit\n",
            PSIMODULE);
    return retCode;
}

/*
 *  Tests to see if the tuner is tuned to and analog channel.
 */
static mpe_Bool isAnalogChannel(sitp_psi_data_t *psi_data)
{
    mpe_SiServiceHandle si_database_handle = 0;
    /*
     ** If the qam mode outside of the modulation range, then it is analog.
     */
    if (psi_data->psi_params.p_ib_tune_info.modulation_mode == MPE_SI_MODULATION_QAM_NTSC)
    {
        MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
                "<%s::isAnalogChannel> - analog channel qamMode: %d.\n",
                PSIMODULE, psi_data->psi_params.p_ib_tune_info.modulation_mode);
        mpe_siLockForWrite();
        (void) mpe_siGetServiceEntryFromFrequencyProgramNumberModulation(
                psi_data->psi_params.p_ib_tune_info.frequency, 0, MPE_SI_MODULATION_QAM_NTSC,
                &si_database_handle); /* get a SIDB handle   */
        mpe_siSetAnalogMode(si_database_handle, MPE_SI_MODULATION_QAM_NTSC); /* set the analog mode */
        mpe_siReleaseWriteLock();
        return TRUE;
    }
    return FALSE;
}

/*
 *  Print the Section Filter filter spec struct for debugging.
 */
static void printFilterSpec(mpe_FilterSpec filterSpec)
{
    /*
     char spec[1024];
     uint32_t index = 0;

     for ( index = 0; index < filterSpec.pos.length; index++ )
     {
     snprintf(spec, 1024, "<%s::printFilterSpec> - Positive length: %u ",
     SIMODULE, filterSpec.pos.length);
     if (filterSpec.pos.length > 0)
     {
     snprintf(spec, 1024, "%s Mask: 0x", spec);
     for ( index = 0; index < filterSpec.pos.length; index++ )
     {
     snprintf(spec, 1024, "%s%2.2x",spec, filterSpec.pos.mask[index]);
     }
     snprintf(spec, 1024, "%s Value: 0x", spec);
     for ( index = 0; index < filterSpec.pos.length; index++ )
     {
     snprintf(spec, 1024, "%s%2.2x",spec, filterSpec.pos.vals[index]);
     }
     }
     }

     MPE_LOG(MPE_LOG_TRACE5, MPE_MOD_SI, "%s\n",spec);

     snprintf(spec, 1024, "<%s::printFilterSpec> - Negative length: %u ",
     SIMODULE, filterSpec.neg.length);
     if (filterSpec.neg.length > 0)
     {
     snprintf(spec, 1024, "%s Mask: 0x", spec);
     for ( index = 0; index < filterSpec.neg.length; index++ )
     {
     snprintf(spec, 1024, "%s%2.2x", spec, filterSpec.neg.mask[index]);
     }
     snprintf(spec, 1024, "%s Value: 0x", spec);
     for ( index = 0; index < filterSpec.neg.length; index++ )
     {
     snprintf(spec, 1024, "%s%2.2x", spec, filterSpec.neg.vals[index]);
     }
     }
     MPE_LOG(MPE_LOG_TRACE5, MPE_MOD_SI, "%s\n",spec);
     */
}

/*
 *  Call to the media manager and get the tuner information
 *  for the specified tuner.
 */
static mpe_Error set_tuner_info(sitp_psi_data_t *psi_data)
{
    mpe_MediaTuneParams tuneParams;

    /*
     * It is IB tuner, so attempt to get the
     * media info and set the tuner info accordingly.
     */
    if (MPE_SUCCESS != mpeos_mediaGetTunerInfo(psi_data->psi_params.p_ib_tune_info.tuner_id,
            &tuneParams))
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                "<%s::set_tuner_info> - unable to get tuner frequency.\n",
                PSIMODULE);
        return SITP_FAILURE;
    }

    psi_data->tuner_state = SITP_PSI_TUNER_STATE_TUNED;
    psi_data->psi_params.p_ib_tune_info.frequency = tuneParams.frequency;
    psi_data->psi_params.p_ib_tune_info.program_number = tuneParams.programNumber;
    psi_data->psi_params.p_ib_tune_info.modulation_mode = tuneParams.qamMode;

    MPE_LOG(MPE_LOG_TRACE1,
            MPE_MOD_SI,
            "<%s::set_tuner_info> - Exit tuner_id : %d, frequency: %d, program_number: %d, mode: %d\n",
            PSIMODULE, psi_data->psi_params.p_ib_tune_info.tuner_id, psi_data->psi_params.p_ib_tune_info.frequency,
            psi_data->psi_params.p_ib_tune_info.program_number, psi_data->psi_params.p_ib_tune_info.modulation_mode);


    return SITP_SUCCESS;
}

/********** Private functions **********/
void printPSIStates(void)
{
    uint32_t index = 0;

    MPE_LOG(MPE_LOG_TRACE7, MPE_MOD_SI,
            "<%s::printPSIStates> - ***********************************\n",
            PSIMODULE);
    for (index = 0; index < g_sitp_psi_num_tuners; index++)
    {
        MPE_LOG(MPE_LOG_TRACE7, MPE_MOD_SI,
                "<%s::printPSIStates> - Tuner index: %d psi state: %s.\n",
                PSIMODULE, g_IB_psi_data_array[index].psi_params.p_ib_tune_info.tuner_id,
                sitp_psi_stateToString(g_IB_psi_data_array[index].fsm_data->psi_state));
    }
    MPE_LOG(MPE_LOG_TRACE7, MPE_MOD_SI,
            "<%s::printPSIStates> - ***********************************\n",
            PSIMODULE);
}

/*
 ** Translate an event number to a string.
 */
char *sitp_psi_eventToString(uint32_t event)
{
    switch (event)
    {
        case MPE_TUNE_STARTED:
            return "TUNE_STARTED";
        case MPE_TUNE_SYNC:
            return "TUNE_SYNC";
        case MPE_TUNE_UNSYNC:
            return "TUNE_UNSYNC";
        case MPE_TUNE_ABORT:
            return "TUNE_ABORT";
        case MPE_TUNE_FAIL:
            return "TUNE_FAIL";
        case MPE_CONTENT_PRESENTING:
            return "CONTENT_PRESENTING";
        case MPE_FAILURE_UNKNOWN:
            return "FAILURE_UNKNOWN";
        case MPE_STILL_FRAME_DECODED:
            return "STILL_FRAME_DECODED";
        case OOB_PSI_ACQUISITION:
            return "OOB_PSI_ACQUISITION";
        case OOB_PSI_DISABLED:
            return "OOB_PSI_DISABLED";
        case MPE_SF_EVENT_LAST_SECTION_FOUND:
            return "MPE_SF_EVENT_LAST_SECTION_FOUND";
        case MPE_ETIMEOUT:
            return "MPE_ETIMEOUT";
        case MPE_SF_EVENT_SOURCE_CLOSED:
            return "MPE_SF_EVENT_SOURCE_CLOSED";
        case MPE_ETHREADDEATH:
            return "MPE_ETHREADDEATH";
        case MPE_SF_EVENT_FILTER_CANCELLED:
            return "MPE_SF_EVENT_FILTER_CANCELLED";
        case MPE_SF_EVENT_FILTER_PREEMPTED:
            return "MPE_SF_EVENT_FILTER_PREEMPTED";
        case MPE_SF_EVENT_OUT_OF_MEMORY:
            return "MPE_SF_EVENT_OUT_OF_MEMORY";
        case MPE_SF_EVENT_FILTER_AVAILABLE:
            return "MPE_SF_EVENT_FILTER_AVAILABLE";
        default:
            return "UNKNOWN EVENT";
    }
}

/*
 ** Translate a state enum to a string.
 */
char *sitp_psi_stateToString(uint32_t state)
{
    switch (state)
    {
        case SITP_PSI_IDLE:
            return "SITP_PSI_IDLE";
        case SITP_PSI_WAIT_INITIAL_PAT:
            return "SITP_PSI_WAIT_INITIAL_PAT";
        case SITP_PSI_WAIT_INITIAL_PRIMARY_PMT:
            return "SITP_PSI_WAIT_INITIAL_PRIMARY_PMT";
        case SITP_PSI_WAIT_INITIAL_SECONDARY_PMT:
            return "SITP_PSI_WAIT_INITIAL_SECONDARY_PMT";
        case SITP_PSI_WAIT_REVISION:
            return "SITP_PSI_WAIT_REVISION";
        default:
            return "UNKNOWN STATE";
    }
}

/*
 * This method is called during parsing of PAT.
 * Set the PAT version field
 */
void add_pat_entry(sitp_psi_fsm_data_t *fsm_data, uint16_t version)
{
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI, "<%s::add_pat_entry> - Enter.\n",
            PSIMODULE);

    fsm_data->shared_filter_PAT.table_type = TABLE_TYPE_PAT;
    fsm_data->shared_filter_PAT.pid = 0x0;
    fsm_data->shared_filter_PAT.version_number = version;
    fsm_data->shared_filter_PAT.filter_priority = SITP_PSI_FILTER_PRIORITY_INITIAL_PAT;

    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI, "<%s::add_pat_entry> - Exit.\n",
            PSIMODULE);
}

/*
 * This method is called during parsing of PAT.
 * Set the PMT entries (version number, program map pid for each program
 * found in the PAT)
 */
mpe_Error add_pmt_entry(sitp_psi_fsm_data_t *fsm_data, uint16_t program_number,
        mpe_Bool primary, uint16_t program_map_pid, uint16_t version)
{
    mpe_Error retCode = SITP_SUCCESS;

    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
            "<%s::add_pmt_entry> - program_map_pid: %d, program_number: %d, primary: %d, version: %d.\n",
            PSIMODULE,
            program_map_pid,
            program_number,
            primary,
            version);

    // If the program_number is the currently tuned program
    // set the filter priority as higher than the other
    // filter priorities
    if(primary == TRUE)
    {
        // Initialize primary PMT filter fields
        fsm_data->shared_filter_primary_PMT.table_type = TABLE_TYPE_PMT;
        fsm_data->shared_filter_primary_PMT.pid = program_map_pid;
        fsm_data->shared_filter_primary_PMT.version_number = version;
        fsm_data->shared_filter_primary_PMT.transport_stream_id = 1;
        fsm_data->shared_filter_primary_PMT.filter_set = FALSE;
        fsm_data->shared_filter_primary_PMT.filter_priority = SITP_PSI_FILTER_PRIORITY_INITIAL_PRIMARY_PMT;
        fsm_data->shared_filter_primary_PMT.program_number = program_number;
        fsm_data->shared_filter_primary_PMT.shared_filterId = MPE_FILTERGROUP_INVALID_FILTER;
        fsm_data->shared_filter_primary_PMT.fsm_data = (void*)fsm_data;
    }
    else
    {
        // Secondary programs in the PAT
        // will have lower filter priority
        sitp_psi_filter_t *psi_filter = NULL;
        LINK *lp = NULL;

        retCode = mpeos_memAllocP(MPE_MEM_SI, sizeof(sitp_psi_filter_t), (void **) &(psi_filter));
        if (MPE_SUCCESS != retCode)
        {
            MPE_LOG(MPE_LOG_ERROR,
                    MPE_MOD_SI,
                    "<%s::reset_table_info> - ERROR: Failed to allocate filter info, retCode: %d\n",
                    PSIMODULE, retCode);
            return SITP_FAILURE;
        }
        // Initialize secondary PMT filter fields
        psi_filter->table_type = TABLE_TYPE_PMT;
        psi_filter->pid = program_map_pid;
        psi_filter->version_number = version;
        psi_filter->transport_stream_id = 1;
        psi_filter->filter_set = FALSE;
        psi_filter->filter_priority = SITP_PSI_FILTER_PRIORITY_INITIAL_SECONDARY_PMT;
        psi_filter->program_number = program_number;
        psi_filter->shared_filterId = MPE_FILTERGROUP_INVALID_FILTER;
        psi_filter->fsm_data = (void*)fsm_data;

        // fsm_data->shared_filter_secondary_PMTs is a list of secondary PMT filters
        // Filters are created at the time PAT is parsed depending on the number of PMTs.
        // List is populated at that time.
        lp = llist_mklink((void *) psi_filter);
        if(fsm_data->shared_filter_secondary_PMTs == NULL)
        {
            fsm_data->shared_filter_secondary_PMTs = llist_create();
        }
        llist_append(fsm_data->shared_filter_secondary_PMTs, lp);
    }

    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI, "<%s::add_pmt_entry> - Exit.\n",
            PSIMODULE);

    return retCode;
}

/*
 * This method is called during parsing of PMT.
 * Set the version number for the given program's PMT
 */
mpe_Error set_pmt_version(sitp_psi_fsm_data_t *fsm_data, uint16_t program_number, uint16_t version)
{
    sitp_psi_filter_t *psi_filter = NULL;
    LINK *lp = NULL;
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
            "<%s::set_pmt_version> - Enter program_number: %d version: %d.\n", PSIMODULE, program_number, version);

    if(fsm_data->shared_filter_primary_PMT.program_number == program_number)
    {
        // Set the primary PMT version number
        fsm_data->shared_filter_primary_PMT.version_number = version;
    }
    else
    {
        // Set the secondary PMT version number
        lp = llist_first(fsm_data->shared_filter_secondary_PMTs);
        while (lp)
        {
            psi_filter = (sitp_psi_filter_t *) llist_getdata(lp);
            if (psi_filter && (psi_filter->program_number == program_number))
            {
                psi_filter->version_number = version;
                break;
            }
            lp = llist_after(lp);
        }

        if (lp==NULL)
        {
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "<%s::set_pmt_version> - Did not find program_number %d (0x%x)\n",
                    PSIMODULE, program_number, program_number );
        }
    }

    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI, "<%s::set_pmt_version> - Exit.\n",
            PSIMODULE);

    return SITP_SUCCESS;
}

// Find the program number corresponding to the filterId
// Called when processing PMT timeout
void findProgramNumber(mpe_SharedFilter filterId, uint16_t *program_number)
{
    uint32_t ii = 0;
    LINK *lp = NULL;
    sitp_psi_filter_t *psi_filter = NULL;
    for (ii = 0; ii < g_sitp_psi_num_tuners; ii++)
    {
        if(g_IB_psi_data_array[ii].fsm_data->shared_filter_primary_PMT.shared_filterId == filterId)
        {
            *program_number = g_IB_psi_data_array[ii].psi_params.p_ib_tune_info.program_number;
            break;
        }

        if(llist_cnt(g_IB_psi_data_array[ii].fsm_data->shared_filter_secondary_PMTs) > 0)
        {
            lp = llist_first(g_IB_psi_data_array[ii].fsm_data->shared_filter_secondary_PMTs);
            while (lp)
            {
                psi_filter = (sitp_psi_filter_t *) llist_getdata(lp);
                if (psi_filter && (psi_filter->shared_filterId == filterId))
                {
                    *program_number = psi_filter->program_number;
                    break;
                }
                lp = llist_after(lp);
            }
        }
    }
}
// Find the program number corresponding to the filterId
// Called when processing PMT timeout
static mpe_Bool isFirstPMTInTransportStream(sitp_psi_fsm_data_t *fsm_data, uint16_t program_number)
{
    sitp_psi_filter_t *psi_filter = NULL;
    LINK *lp = NULL;
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
            "<%s::isFirstPMTInTransportStream> - Enter program_number: %d.\n", PSIMODULE, program_number);

    // This is a list of secondary PMT filters
    if(llist_cnt(fsm_data->shared_filter_secondary_PMTs) > 0)
    {
        // This is a transport stream tune, check secondary PMT filters
        lp = llist_first(fsm_data->shared_filter_secondary_PMTs);
        while (lp)
        {
            psi_filter = (sitp_psi_filter_t *) llist_getdata(lp);
            if (psi_filter && (psi_filter->version_number != INIT_TABLE_VERSION))
            {
                // If at least one other PMT in the list has been acquired
                return FALSE;
            }
            lp = llist_after(lp);
        }

        if (lp==NULL)
        {
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "<%s::isFirstPMTInTransportStream> - Did not find program_number %d (0x%x)\n",
                    PSIMODULE, program_number, program_number );
        }
    }
    return TRUE;
}


// Cancel all existing filters
// Check if the filter is set and the filterId is non-NULL
// The corresponding filter groups are paused before
// removing filters and re-started later
static void cancel_all_filters(sitp_psi_data_t *psi_data)
{
    MPE_LOG(MPE_LOG_TRACE2,
            MPE_MOD_SI,
            "<%s::cancel_all_filters> - enter..\n",
            PSIMODULE);
    {
        // Pause all the groups to not create chaos
        // Note: Some of these may be the same group, but that's OK...
        filter_pauseFilterGroup(psi_data->fsm_data->filter_group_initial_PAT);
        filter_pauseFilterGroup(psi_data->fsm_data->filter_group_initial_primary_PMT);

        if (psi_data->fsm_data->filter_group_revision_PAT != NULL)
        {
            filter_pauseFilterGroup(psi_data->fsm_data->filter_group_revision_PAT);
        }
        if (psi_data->fsm_data->filter_group_revision_primary_PMT != NULL)
        {
            filter_pauseFilterGroup(psi_data->fsm_data->filter_group_revision_primary_PMT);
        }
        if (psi_data->fsm_data->filter_group_initial_secondary_PMT != NULL)
        {
            filter_pauseFilterGroup(psi_data->fsm_data->filter_group_initial_secondary_PMT);
        }
        if (psi_data->fsm_data->filter_group_revision_secondary_PMT != NULL)
        {
            filter_pauseFilterGroup(psi_data->fsm_data->filter_group_revision_secondary_PMT);
        }

        // Start canceling filters from lowest priority to highest
        // Cancel secondary PMT filters
        if(llist_cnt(psi_data->fsm_data->shared_filter_secondary_PMTs) > 0)
        {
            LINK *lp = llist_first(psi_data->fsm_data->shared_filter_secondary_PMTs);
            while (lp)
            {
                sitp_psi_filter_t *psi_filter = (sitp_psi_filter_t *) llist_getdata(lp);
                if( (psi_filter->shared_filterId != MPE_FILTERGROUP_INVALID_FILTER) && (psi_filter->filter_set == TRUE))
                {
                    if(psi_filter->version_number == INIT_TABLE_VERSION)
                    {
                        MPE_LOG(MPE_LOG_TRACE2,
                                MPE_MOD_SI,
                                "<%s::cancel_all_filters> - remove secondary PMT (pn: %d) initial filter:%p\n",
                                PSIMODULE, psi_filter->program_number, psi_filter->shared_filterId);
                    }
                    else
                    {
                        MPE_LOG(MPE_LOG_TRACE2,
                                MPE_MOD_SI,
                                "<%s::cancel_all_filters> - remove secondary PMT (pn: %d) revision filter:%p\n",
                                PSIMODULE, psi_filter->program_number, psi_filter->shared_filterId);
                    }
                    filter_removeSharedFilter(psi_filter->shared_filterId);
                    psi_filter->filter_set = FALSE;
                }
                lp = llist_after(lp);
            }
        }

        // Remove PMT filter from group (cancel filter)
        if( (psi_data->fsm_data->shared_filter_primary_PMT.shared_filterId != MPE_FILTERGROUP_INVALID_FILTER)
                && (psi_data->fsm_data->shared_filter_primary_PMT.filter_set == TRUE))
        {
            if(psi_data->fsm_data->shared_filter_primary_PMT.version_number == INIT_TABLE_VERSION)
            {
                MPE_LOG(MPE_LOG_TRACE2,
                        MPE_MOD_SI,
                        "<%s::cancel_all_filters> - remove primary PMT (pn: %d) initial filter:%p\n",
                        PSIMODULE, psi_data->fsm_data->shared_filter_primary_PMT.program_number, psi_data->fsm_data->shared_filter_primary_PMT.shared_filterId);
            }
            else
            {
                MPE_LOG(MPE_LOG_TRACE2,
                        MPE_MOD_SI,
                        "<%s::cancel_all_filters> - remove primary PMT (pn: %d) revision filter:%p\n",
                        PSIMODULE, psi_data->fsm_data->shared_filter_primary_PMT.program_number, psi_data->fsm_data->shared_filter_primary_PMT.shared_filterId);
            }
            filter_removeSharedFilter(psi_data->fsm_data->shared_filter_primary_PMT.shared_filterId);
            psi_data->fsm_data->shared_filter_primary_PMT.filter_set = FALSE;

        }

        // Remove PAT filter from group (cancel filter)
        if( (psi_data->fsm_data->shared_filter_PAT.shared_filterId != MPE_FILTERGROUP_INVALID_FILTER)
                && (psi_data->fsm_data->shared_filter_PAT.filter_set == TRUE))
        {
            if(psi_data->fsm_data->shared_filter_PAT.version_number == INIT_TABLE_VERSION)
            {
                MPE_LOG(MPE_LOG_TRACE2,
                        MPE_MOD_SI,
                        "<%s::cancel_all_filters> - remove PAT initial filter:%p\n",
                        PSIMODULE, psi_data->fsm_data->shared_filter_PAT.shared_filterId);
            }
            else
            {
                MPE_LOG(MPE_LOG_TRACE2,
                        MPE_MOD_SI,
                        "<%s::cancel_all_filters> - remove PAT revision filter:%p\n",
                        PSIMODULE, psi_data->fsm_data->shared_filter_PAT.shared_filterId);
            }
            filter_removeSharedFilter(psi_data->fsm_data->shared_filter_PAT.shared_filterId);
            psi_data->fsm_data->shared_filter_PAT.filter_set = FALSE;
        }
    }

    // Resume the filter groups
    // Note: Some of these may be the same group, but that's OK...
    filter_startFilterGroup(psi_data->fsm_data->filter_group_initial_PAT);
    filter_startFilterGroup(psi_data->fsm_data->filter_group_initial_primary_PMT);

    if (psi_data->fsm_data->filter_group_revision_PAT != NULL)
    {
        filter_startFilterGroup(psi_data->fsm_data->filter_group_revision_PAT);
    }
    if (psi_data->fsm_data->filter_group_revision_primary_PMT != NULL)
    {
        filter_startFilterGroup(psi_data->fsm_data->filter_group_revision_primary_PMT);
    }
    if (psi_data->fsm_data->filter_group_initial_secondary_PMT != NULL)
    {
        filter_startFilterGroup(psi_data->fsm_data->filter_group_initial_secondary_PMT);
    }
    if (psi_data->fsm_data->filter_group_revision_secondary_PMT != NULL)
    {
        filter_startFilterGroup(psi_data->fsm_data->filter_group_revision_secondary_PMT);
    }
    MPE_LOG(MPE_LOG_TRACE2,
            MPE_MOD_SI,
            "<%s::cancel_all_filters> - exit..\n",
            PSIMODULE);
}

/*
 * Print PSI data for IB tuners
 */
void print_psi_data(sitp_psi_data_t *psi_data)
{
    uint32_t ii = 0;
    MPE_LOG(MPE_LOG_TRACE2,
            MPE_MOD_SI,
            "<%s::print_psi_data> - **********************************************\n",
            PSIMODULE);
    for (ii = 0; ii < g_sitp_psi_num_tuners; ii++)
    {
        MPE_LOG(MPE_LOG_TRACE2,
                MPE_MOD_SI,
                "<%s::print_psi_data> - Entry: %d, Tuner: %d, frequency: %d, program_number: %d, modulation_mode: %d.\n",
                PSIMODULE, ii, g_IB_psi_data_array[ii].psi_params.p_ib_tune_info.tuner_id,
                g_IB_psi_data_array[ii].psi_params.p_ib_tune_info.frequency,
                g_IB_psi_data_array[ii].psi_params.p_ib_tune_info.program_number,
                g_IB_psi_data_array[ii].psi_params.p_ib_tune_info.modulation_mode);
        print_fsm_data(g_IB_psi_data_array[ii].fsm_data);
    }

    MPE_LOG(MPE_LOG_TRACE2,
            MPE_MOD_SI,
            "<%s::print_psi_data> - **********************************************\n",
            PSIMODULE);
}

/*
 * Print FSM data fields
 */
void print_fsm_data(sitp_psi_fsm_data_t *fsm_data)
{
    int ii=0;
    sitp_psi_filter_t *psi_filter = NULL;
    MPE_LOG(MPE_LOG_TRACE2,
            MPE_MOD_SI,
            "<%s::print_fsm_data> - psi_state: %d, ts_handle: 0x%x, shared_filter_PAT: 0x%x, shared_filter_primary_PMT: 0x%x ",
            PSIMODULE, fsm_data->psi_state,
            fsm_data->ts_handle,
            fsm_data->shared_filter_PAT,
            fsm_data->shared_filter_primary_PMT);
    for (ii = 0; ii < llist_cnt(fsm_data->shared_filter_secondary_PMTs); ii++)
    {
        LINK *lp = llist_first(fsm_data->shared_filter_secondary_PMTs);
        while (lp)
        {
            psi_filter = (sitp_psi_filter_t *) llist_getdata(lp);
            MPE_LOG(MPE_LOG_TRACE2,
                    MPE_MOD_SI,
                    "shared_filter_PMT:0x%x.\n", psi_filter);
            lp = llist_after(lp);
        }
    }
}

/********** Private functions **********/
/*************************************************************************
 * NAME:
 *     tune_initiated - sets the service entry status in the sidb prior
 *     to receiving the tune_sync event.  Called by mediaMgr.
 *
 * SYNOPSIS:
 *    mpe_Error tune_initiated(mpe_MediaTuneRequestParams *tuneRequest)
 *
 *
 * INPUTS:
 *  Parameters:
 *   mpe_MediaTuneRequestParams *tuneRequest - the tune request params so
 *   the correct service entry can be found in the sidb by freq,
 *   program number.
 *
 * OUTPUTS:
 *  None
 *
 *  Return:
 *    MPE_SUCCESS - always
 *
 * DESCRIPTION:
 * Called by the media mgr to notify the sitp that a tune is
 * initiated and that it can then update the sidb service entry
 * status to 'available_shorty'.
 *
 * CALLED BY:
 *  SITP
 *
 *************************************************************************/
void tune_initiated(mpe_MediaTuneRequestParams *tuneRequest)
{
    mpe_SiTransportStreamHandle ts_handle = MPE_SI_INVALID_HANDLE; // a handle into ths SI database
    uint32_t freq = 0;
    uint32_t progNum = 0;
    uint32_t tuner_number = 0;
    mpe_SiModulationMode modMode = 0;
    uint8_t index = 0;

    /*
     ** Notify SIDB that a tune is about to happen for
     ** the reqParams related service entry so the state
     ** can be changed to PMT_AVAILABLE_SHORTLY.  All
     ** failure and cleanup state changes are handled
     ** in the sitp module state machine.
     ** First get the freq and program number from the current media info.
     */
    tuner_number = tuneRequest->tunerId;
    freq = tuneRequest->tuneParams.frequency;
    progNum = tuneRequest->tuneParams.programNumber;
    modMode = tuneRequest->tuneParams.qamMode;

    index = tuner_number-1; // Index starts at 0

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI, "<%s::tune_initiated> - Enter.. tuner number:%d\n",
            PSIMODULE, tuner_number);
    /*
     ** Acquire the global mutex.
     */
    mpe_mutexAcquire(g_sitp_psi_mutex);

    /*
     ** Does the mediaRequestInfo frequency == the current tuner frequency?? If not,
     ** set the previous SIDB Transport stream info to NOT_AVAILABLE (will be done at TUNE_STARTED)
     ** and set the new SIDB transport Stream info to AVAILABLE_SHORTLY.
     */
    if (freq != g_IB_psi_data_array[index].psi_params.p_ib_tune_info.frequency)
    {
        /*
         ** Next, If I haven't tuned yet,
         ** just set the SIDB state for the transport stream to AVAILABLE_SHORTLY.
         */
        if (0 == g_IB_psi_data_array[index].psi_params.p_ib_tune_info.last_tune_info.frequency)
        {
            MPE_LOG(MPE_LOG_TRACE2,
                    MPE_MOD_SI,
                    "<%s::tune_initiated> - Setting state to SI_AVAILABLE_SHORTLY...freq: %d\n",
                    PSIMODULE, freq);

            mpe_siLockForWrite();
            mpe_siGetTransportStreamEntryFromFrequencyModulation(freq, modMode,
                    &ts_handle);
            mpe_siSetTSIdStatusAvailableShortly(ts_handle);
            mpe_siSetPMTStatusAvailableShortly(ts_handle);
            mpe_siReleaseWriteLock();
        }
        else
        {
            /*
             ** Otherwise, set the tmp info SIDB transport stream entries to
             ** NOT_AVAILABLE and set the new info to AVAILABLE_SHORTLY.
             ** First set tmp member to be reset in SIDB to the NOT_AVAILBLE
             ** state of the old info.  Do this when the MPE_TUNE_START
             */
            g_IB_psi_data_array[index].psi_params.p_ib_tune_info.last_tune_info.cleanup_frequency
                    = g_IB_psi_data_array[index].psi_params.p_ib_tune_info.last_tune_info.frequency;
            g_IB_psi_data_array[index].psi_params.p_ib_tune_info.last_tune_info.cleanup_program_number
                    = g_IB_psi_data_array[index].psi_params.p_ib_tune_info.last_tune_info.program_number;
            g_IB_psi_data_array[index].psi_params.p_ib_tune_info.last_tune_info.cleanup_modulation_mode
                    = g_IB_psi_data_array[index].psi_params.p_ib_tune_info.last_tune_info.modulation_mode;

            /*
             ** Now set the AVAILBLE_SHORTLY state of the new info.
             */
            MPE_LOG(MPE_LOG_TRACE2,
                    MPE_MOD_SI,
                    "<%s::tune_initiated> - tuning to different frequency...prev_freq:%d, cur_freq:%d\n",
                    PSIMODULE,
                    g_IB_psi_data_array[index].psi_params.p_ib_tune_info.last_tune_info.frequency,
                    freq);
            ts_handle = MPE_SI_INVALID_HANDLE;

            mpe_siLockForWrite();
            mpe_siGetTransportStreamEntryFromFrequencyModulation(freq, modMode,
                    &ts_handle);
            mpe_siSetTSIdStatusAvailableShortly(ts_handle);
            mpe_siSetPMTStatusAvailableShortly(ts_handle);
            mpe_siReleaseWriteLock();
        }
    }
    /*
     ** Now set the new last tune_info structure.
     */
    g_IB_psi_data_array[index].psi_params.p_ib_tune_info.last_tune_info.frequency = freq;
    g_IB_psi_data_array[index].psi_params.p_ib_tune_info.last_tune_info.program_number = progNum;
    g_IB_psi_data_array[index].psi_params.p_ib_tune_info.last_tune_info.modulation_mode = modMode;

    // Clean all outstanding filters (array index starts at 0)
    cancel_all_filters(&g_IB_psi_data_array[index]);

    /*
     ** Release the global mutex.
     */
    mpe_mutexRelease(g_sitp_psi_mutex);

    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
            "<%s::tune_initiated> - released mutex..\n", PSIMODULE);

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI, "<%s::tune_initiated> - Exit..\n",
            PSIMODULE);
}

// This method is called when PAT acquisition times out
// Reset SI DB states, signal PAT/PMT REMOVE events
static mpe_Error process_pat_timeout(sitp_psi_data_t *psi_data)
{
    mpe_SiServiceHandle si_database_handle = MPE_SI_INVALID_HANDLE;
    mpe_SiTransportStreamHandle ts_handle = MPE_SI_INVALID_HANDLE;
    uint32_t frequency = 0;
    mpe_SiModulationMode mode = MPE_SI_MODULATION_UNKNOWN;
    uint16_t pn = 0;
    mpe_SiTableType table_type = TABLE_UNKNOWN;
    mpe_Error retCode = MPE_SUCCESS;

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::process_pat_timeout>...\n", PSIMODULE);

    mpe_siLockForWrite();

    switch(psi_data->tuner_type)
    {
        case SITP_PSI_TUNER_TYPE_OOB:
            frequency = MPE_SI_OOB_FREQUENCY;
            table_type = OOB_PAT;
            (void) mpe_siGetServiceEntryFromFrequencyProgramNumberModulation(
                    frequency, PROGRAM_NUMBER_UNKNOWN,
                    mode, &si_database_handle);
            break;
        case SITP_PSI_TUNER_TYPE_DSG:
        {
            frequency = MPE_SI_DSG_FREQUENCY;
            table_type = OOB_PAT;
            // Get DSG service entry
            retCode = mpe_siGetServiceEntryFromAppIdProgramNumber(
                    psi_data->psi_params.p_dsg_info.dsg_app_id, PROGRAM_NUMBER_UNKNOWN,
                    &si_database_handle);
            if (retCode == MPE_SI_NOT_FOUND)
            {
                retCode = mpe_siCreateDSGServiceHandle((uint32_t) psi_data->psi_params.p_dsg_info.dsg_app_id,
                        PROGRAM_NUMBER_UNKNOWN, NULL, NULL,
                                &si_database_handle);
            }
        }
            break;
        case SITP_PSI_TUNER_TYPE_HN:
        {
            frequency = MPE_SI_HN_FREQUENCY;
            table_type = OOB_PAT;

            // Get HN service entry
            if(mpe_siGetServiceEntryFromHNstreamProgramNumber(psi_data->psi_params.p_hn_stream_info.hn_stream_session,
                    PROGRAM_NUMBER_UNKNOWN, &si_database_handle) != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_TRACE2,
                        MPE_MOD_SI,
                        "<process_pat_timeout> - SITP_PSI_TUNER_TYPE_HN, si_database_handle lookup failed..\n");
            }

        }
            break;
        case SITP_PSI_TUNER_TYPE_IB:
            frequency = psi_data->psi_params.p_ib_tune_info.frequency;
            pn = psi_data->psi_params.p_ib_tune_info.program_number;
            mode = psi_data->psi_params.p_ib_tune_info.modulation_mode;

            table_type = IB_PAT;
            retCode = mpe_siGetServiceHandleByFrequencyModulationProgramNumber(
                    frequency, mode, pn,
                    &si_database_handle);
            if (retCode == MPE_SI_NOT_FOUND
            		|| retCode == MPE_SI_NOT_AVAILABLE
            		|| retCode == MPE_SI_NOT_AVAILABLE_YET)
            {
        			retCode = mpe_siCreateDynamicServiceHandle(frequency, pn, mode,
        					&si_database_handle);

                    MPE_LOG(MPE_LOG_TRACE2,
                        MPE_MOD_SI,
                        "<process_pat_timeout> - SITP_PSI_TUNER_TYPE_IB, si_database_handle:%0x%x\n", si_database_handle);
            }
            break;
        default:
            break;
    }

    ts_handle = psi_data->fsm_data->ts_handle;
    if(ts_handle != MPE_SI_INVALID_HANDLE)
    {
        MPE_LOG(MPE_LOG_TRACE2,
                MPE_MOD_SI,
                "<process_pat_timeout> - MPE_ETIMEOUT: set TsID status to SI_NOT_AVAILABLE\n");
        mpe_siSetTSIdStatusNotAvailable(ts_handle);
        if(table_type == OOB_PAT)
        {
            mpe_siNotifyTableChanged(OOB_PAT, MPE_SI_CHANGE_TYPE_REMOVE, ts_handle);
            MPE_LOG(MPE_LOG_TRACE2,
                    MPE_MOD_SI,
                    "<process_pat_timeout> - MPE_ETIMEOUT: Signaling PMT Table update changeType: MPE_SI_CHANGE_TYPE_REMOVE\n");
            mpe_siNotifyTableChanged(OOB_PMT, MPE_SI_CHANGE_TYPE_REMOVE, si_database_handle);
        }
        else
        {
            // Fix for PAT acquisition failure (leading to lock-up) issue
            // When there is a timeout acquiring PAT, signal a 'REMOVE' changeType
            // and reset all states (PAT/PMT) in SI DB that are currently set to 'AVIALABLE_SHORTLY'
            // to 'NOT_AVAILABLE'
            mpe_siNotifyTableChanged(IB_PAT, MPE_SI_CHANGE_TYPE_REMOVE, ts_handle);

            MPE_LOG(MPE_LOG_TRACE2,
                    MPE_MOD_SI,
                    "<process_pat_timeout> - MPE_ETIMEOUT: Signaling PMT Table update changeType: MPE_SI_CHANGE_TYPE_REMOVE\n");

            MPE_LOG(MPE_LOG_TRACE2,
                    MPE_MOD_SI,
                    "<%s::process_pat_timeout> - Calling mpe_siResetPATProgramStatus\n",
                    PSIMODULE);
            // This will reset the PMT status for all programs and signal PMT REMOVE events
            mpe_siResetPATProgramStatus(ts_handle);
        }
    }
    mpe_siReleaseWriteLock();

    return retCode;
}

// When a PMT timeout happens signal SI DB
static mpe_Error process_pmt_timeout(sitp_psi_data_t *psi_data, uint16_t program_number)
{
    // Fix for PMT acquisition failure (timeout) issue
    // When there is a timeout acquiring PMT, signal a 'REMOVE' changeType
    // and reset PMT state for the current program in SI DB that is currently set to 'AVIALABLE_SHORTLY'
    // to 'NOT_AVAILABLE'
    mpe_SiServiceHandle si_database_handle = MPE_SI_INVALID_HANDLE;
    mpe_SiTransportStreamHandle ts_handle = MPE_SI_INVALID_HANDLE;
    mpe_SiProgramHandle pi_handle = MPE_SI_INVALID_HANDLE;
    uint32_t frequency = 0;
    mpe_SiModulationMode mode = MPE_SI_MODULATION_UNKNOWN;
    mpe_SiTableType table_type = TABLE_UNKNOWN;
    publicSIIterator iter;
    mpe_Error retCode = MPE_SUCCESS;

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::process_PMT_timeout> program_number:%d\n", PSIMODULE, program_number);

    ts_handle = psi_data->fsm_data->ts_handle;
    if(ts_handle == MPE_SI_INVALID_HANDLE)
    {
        MPE_LOG(MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<process_PMT_timeout> - ts_handle invalid..\n");
        return MPE_EINVAL;
    }

    mpe_siLockForWrite();

    switch(psi_data->tuner_type)
    {
        case SITP_PSI_TUNER_TYPE_OOB:
        {
            frequency = MPE_SI_OOB_FREQUENCY;
            table_type = OOB_PMT;

            (void) mpe_siGetProgramEntryFromTransportStreamEntry(
                    ts_handle, program_number, &pi_handle);
            (void) mpe_siGetServiceEntryFromFrequencyProgramNumberModulation(
                    frequency, PROGRAM_NUMBER_UNKNOWN,
                    mode, &si_database_handle);
        }
            break;
        case SITP_PSI_TUNER_TYPE_DSG:
        {
            frequency = MPE_SI_DSG_FREQUENCY;
            table_type = OOB_PMT;
            // Get DSG service entry
            retCode = mpe_siGetServiceEntryFromAppIdProgramNumber(
                    psi_data->psi_params.p_dsg_info.dsg_app_id, PROGRAM_NUMBER_UNKNOWN,
                    &si_database_handle);
            if (retCode == MPE_SI_NOT_FOUND)
            {
                retCode = mpe_siCreateDSGServiceHandle((uint32_t) psi_data->psi_params.p_dsg_info.dsg_app_id,
                        PROGRAM_NUMBER_UNKNOWN, NULL, NULL,
                                &si_database_handle);
            }
            (void) mpe_siGetProgramEntryFromTransportStreamAppIdProgramNumber(
                    ts_handle, psi_data->psi_params.p_dsg_info.dsg_app_id, PROGRAM_NUMBER_UNKNOWN,
                    &pi_handle);
        }
            break;
        case SITP_PSI_TUNER_TYPE_HN:
        {
            frequency = MPE_SI_HN_FREQUENCY;
            table_type = OOB_PMT;

            // Get HN service entry
            if(mpe_siGetServiceEntryFromHNstreamProgramNumber(psi_data->psi_params.p_hn_stream_info.hn_stream_session,
                    PROGRAM_NUMBER_UNKNOWN, &si_database_handle) != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_TRACE2,
                        MPE_MOD_SI,
                        "<process_PMT_timeout> - SITP_PSI_TUNER_TYPE_HN, si_database_handle lookup failed..\n");
            }
            (void) mpe_siGetProgramEntryFromTransportStreamHNstreamProgramNumber(
                    ts_handle, psi_data->psi_params.p_hn_stream_info.hn_stream_session, PROGRAM_NUMBER_UNKNOWN,
                    &pi_handle);

        }
            break;
        case SITP_PSI_TUNER_TYPE_IB:
            frequency = psi_data->psi_params.p_ib_tune_info.frequency;
            // This is the tuned program number
            // This may be different than the program that timed out
            //uint16_t pn = 0;
            //pn = psi_data->psi_params.p_ib_tune_info.program_number;
            mode = psi_data->psi_params.p_ib_tune_info.modulation_mode;

            table_type = IB_PMT;
            (void) mpe_siGetProgramEntryFromTransportStreamEntry(ts_handle,
                    program_number, // 'program_number' which timed out
                    &pi_handle);

            retCode = mpe_siGetServiceHandleByFrequencyModulationProgramNumber(
                    frequency, mode, program_number,
                    &si_database_handle);
            if (retCode == MPE_SI_NOT_FOUND
            		|| retCode == MPE_SI_NOT_AVAILABLE
            		|| retCode == MPE_SI_NOT_AVAILABLE_YET)
            {
        			retCode = mpe_siCreateDynamicServiceHandle(frequency, program_number, mode,
        					&si_database_handle);

                    MPE_LOG(MPE_LOG_TRACE2,
                        MPE_MOD_SI,
                        "<process_PMT_timeout> - SITP_PSI_TUNER_TYPE_IB, si_database_handle:%0x%x\n", si_database_handle);
            }

            break;
        default:
            break;
    }

    if(table_type == OOB_PMT)
    {
        MPE_LOG(MPE_LOG_TRACE2,
                MPE_MOD_SI,
                "<process_PMT_timeout> - MPE_ETIMEOUT: Signaling PMT Table update changeType: MPE_SI_CHANGE_TYPE_REMOVE\n");
        mpe_siNotifyTableChanged(OOB_PMT, MPE_SI_CHANGE_TYPE_REMOVE, si_database_handle);
    }
    else
    {
        // signal PMT 'REMOVE' event
        MPE_LOG(MPE_LOG_TRACE2,
                MPE_MOD_SI,
                "<%s::process_PMT_timeout> - MPE_ETIMEOUT: prog_handle:0x%x \n",
                PSIMODULE, pi_handle);

        // SET PMT status to NOT_AVAILABLE for this program only (if the previous state was
        // PMT_AVAILABLE_SHORTLY)...
        if (mpe_siSetPMTProgramStatusNotAvailable(pi_handle) == MPE_SI_SUCCESS)
        {
            // Need to loop through services attached to the program here, and notify each one.
            // This is new, because now more than one service can point to a program.

            si_database_handle = mpe_siFindFirstServiceFromProgram((mpe_SiProgramHandle) pi_handle, (SIIterator*) &iter);

            MPE_LOG(MPE_LOG_TRACE2,
                    MPE_MOD_SI,
                    "<%s::process_PMT_timeout> - MPE_ETIMEOUT: si_database_handle:0x%x \n",
                    PSIMODULE, si_database_handle);

            while (si_database_handle != MPE_SI_INVALID_HANDLE)
            {
                // Signal these program PMTs as removed so as to unblock any waiting callers
                MPE_LOG(MPE_LOG_TRACE2,
                        MPE_MOD_SI,
                        "<%s::process_PMT_timeout> - MPE_ETIMEOUT: Signaling PMT Table update changeType: MPE_SI_CHANGE_TYPE_REMOVE, si_database_handle:0x%x \n",
                        PSIMODULE, si_database_handle);

                mpe_siNotifyTableChanged(IB_PMT, MPE_SI_CHANGE_TYPE_REMOVE, si_database_handle);
                si_database_handle = mpe_siGetNextServiceFromProgram((SIIterator*) &iter);
            }
        }
    }
    mpe_siReleaseWriteLock();

    // End Of PMT timeout/signal "REMOVE'
    return retCode;
}

static void process_tune_start(uint8_t index)
{
    // Set SI DB states
    uint32_t tmpFreq = 0;
    uint32_t tmpProgNum = 0;
    uint32_t tmpModMode = 0;
    uint32_t newFreq = 0;
    uint32_t newProgNum = 0;
    uint32_t newModMode = 0;

    // Index starts at 0
    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::process_tune_start> - ...tuner_number: %d\n",
            PSIMODULE, index+1);

    tmpFreq = g_IB_psi_data_array[index].psi_params.p_ib_tune_info.last_tune_info.cleanup_frequency;
    tmpProgNum = g_IB_psi_data_array[index].psi_params.p_ib_tune_info.last_tune_info.cleanup_program_number;
    tmpModMode = g_IB_psi_data_array[index].psi_params.p_ib_tune_info.last_tune_info.cleanup_modulation_mode;

    newFreq = g_IB_psi_data_array[index].psi_params.p_ib_tune_info.last_tune_info.frequency;
    newProgNum = g_IB_psi_data_array[index].psi_params.p_ib_tune_info.last_tune_info.program_number;
    newModMode = g_IB_psi_data_array[index].psi_params.p_ib_tune_info.last_tune_info.modulation_mode;

    if ((0 != tmpFreq) && (tmpFreq != newFreq))
    {
        // reset the SI status for previous frequency
        mpe_SiProgramHandle pi_handle = MPE_SI_INVALID_HANDLE;
        mpe_SiTransportHandle ts_handle = MPE_SI_INVALID_HANDLE;

        mpe_siLockForWrite();
        if (mpe_siGetTransportStreamEntryFromFrequencyModulation(tmpFreq, tmpModMode, &ts_handle) == MPE_SI_SUCCESS)
        {
            mpe_siRevertTSIdStatus(ts_handle);
            if (mpe_siGetProgramEntryFromTransportStreamEntry(ts_handle, tmpProgNum, &pi_handle) == MPE_SI_SUCCESS)
            {
                (void) mpe_siRevertPMTStatus(pi_handle);
            }
            else
            {
                MPE_LOG(MPE_LOG_INFO,
                        MPE_MOD_SI,
                        "<%s::process_tune_start> - Failed to create program %d on Frequency %d!\n",
                        PSIMODULE, tmpProgNum, tmpFreq);
            }
        }
        else
        {
            MPE_LOG(MPE_LOG_INFO,
                    MPE_MOD_SI,
                    "<%s::process_tune_start> - Failed to create transport stream for Frequency %d!\n",
                    PSIMODULE, tmpFreq);
        }
        mpe_siReleaseWriteLock();
    }

    g_IB_psi_data_array[index].psi_params.p_ib_tune_info.last_tune_info.cleanup_frequency = newFreq;
    g_IB_psi_data_array[index].psi_params.p_ib_tune_info.last_tune_info.cleanup_program_number = newProgNum;
    g_IB_psi_data_array[index].psi_params.p_ib_tune_info.last_tune_info.cleanup_modulation_mode = newModMode;

    MPE_LOG(MPE_LOG_TRACE1,
            MPE_MOD_SI,
            "<%s::process_tune_start> - setting prev_freq: %d, prev_progNum:%d, pre_ModMode:%d \n",
            PSIMODULE,
            g_IB_psi_data_array[index].psi_params.p_ib_tune_info.last_tune_info.cleanup_frequency,
            g_IB_psi_data_array[index].psi_params.p_ib_tune_info.last_tune_info.cleanup_program_number,
            g_IB_psi_data_array[index].psi_params.p_ib_tune_info.last_tune_info.cleanup_modulation_mode);
}

// This method is called when TUNE_UNSYNC or TUNE_FAIL is received.
static void process_transport_stream_unavailable(uint8_t index)
{
    // A TUNE_UNSYNC means the tuner is tuned but sync NOT acquired.
    // But we need to treat it as a (temporary) failure to acquire PAT
    // and therefore all the PMTs and signal appropriate events
    // to unblock any waiting callers and reset states in SI DB.
    // The TUNE_UNSYNC can be signaled before/after TUNE_SYNC.
    // This event is considered recoverable.
    // The TUNE_FAIL is only expected when platform is unable to achieve
    // tuner lock. This can only be signaled once per tune and is considered terminal.
    // This is a non-recoverable failure. Treat it as failure to
    // acquire PAT and therefore all the PMTs and signal appropriate events
    // to unblock any waiting callers and reset states in SI DB.
    mpe_SiTransportHandle ts_handle = MPE_SI_INVALID_HANDLE;
    uint32_t lastFreq = 0;
    uint32_t lastModMode = 0;
    // Index starts at 0
    MPE_LOG(MPE_LOG_TRACE1,
            MPE_MOD_SI,
            "<%s::process_transport_stream_unavailable> - ...tuner_number: %d\n",
            PSIMODULE, index+1);
    // Received UNSYNC, Cancel any existing filters.
    // New filters will be set after receiving SYNC.
    // If there are no existing filters, it is a no-op.
    cancel_all_filters(&g_IB_psi_data_array[index]);

    lastFreq = g_IB_psi_data_array[index].psi_params.p_ib_tune_info.last_tune_info.frequency;
    lastModMode = g_IB_psi_data_array[index].psi_params.p_ib_tune_info.last_tune_info.modulation_mode;

    if (lastFreq != 0) // 0 == untuned.
    {
        MPE_LOG(MPE_LOG_TRACE2,
                MPE_MOD_SI,
                "<%s::process_transport_stream_unavailable> - Setting tsId state to NOT_AVAILABLE...freq: %d\n\n",
                PSIMODULE, lastFreq);
        mpe_siLockForWrite();
        if (mpe_siGetTransportStreamEntryFromFrequencyModulation(lastFreq, lastModMode, &ts_handle) == MPE_SI_SUCCESS)
        {
            if(ts_handle != MPE_SI_INVALID_HANDLE)
            {
				// Reset status to NOT_AVAILABLE
				mpe_siRevertTSIdStatus(ts_handle);

				mpe_siNotifyTableChanged(IB_PAT, MPE_SI_CHANGE_TYPE_REMOVE, ts_handle);

                MPE_LOG(MPE_LOG_TRACE2,
                    MPE_MOD_SI,
                    "<%s::process_transport_stream_unavailable> - Calling mpe_siResetPATProgramStatus\n",
                    PSIMODULE);

                // This will reset the PMT status for all programs and signal PMT REMOVE events
                mpe_siResetPATProgramStatus(ts_handle);
            }
        }
        mpe_siReleaseWriteLock();
    }
}

/*
 * Acquire the tuner info, set the appropriate filter and handle the errors and state transitions.
 */
static mpe_Error process_tune_sync(sitp_psi_data_t *psi_data)
{
    mpe_Error retCode = SITP_SUCCESS;

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::process_tune_sync> - for tuner_id: %d\n", PSIMODULE,
            psi_data->psi_params.p_ib_tune_info.tuner_id);

    retCode = set_tuner_info(psi_data);
    if (SITP_SUCCESS != retCode)
    {
        MPE_LOG(MPE_LOG_TRACE2,
                MPE_MOD_SI,
                "<%s::process_tune_sync> - set_tuner_info returned error\n",
                PSIMODULE);
        psi_data->fsm_data->psi_state = SITP_PSI_IDLE;
        return retCode;
    }

    // Log (SNMP) when Tune completed!
    if(psi_data->tuner_type == SITP_PSI_TUNER_TYPE_IB)
    {
        mpe_TimeMillis current_time = 0;
        char timeStampString[256] = " ";
        char message[256] = " ";

        // Record the current time
        mpeos_timeGetMillis(&current_time);
        // Start time = %"PRIu64"
        sprintf(timeStampString, "%"PRIu64, (uint64_t)current_time);
        sprintf(message, "(Performance.Tuning-INFO) QAM Lock: Tuner %d", psi_data->psi_params.p_ib_tune_info.tuner_id);
        mpeos_dbgAddLogEntry(ocStbHostSystemLoggingEventTable_oid, timeStampString, message);
    }

    {   // Get transport stream handle
        mpe_siLockForWrite();
        if (mpe_siGetTransportStreamEntryFromFrequencyModulation(psi_data->psi_params.p_ib_tune_info.frequency,
                psi_data->psi_params.p_ib_tune_info.modulation_mode, &(psi_data->fsm_data->ts_handle)) != MPE_SI_SUCCESS)
        {
            MPE_LOG(MPE_LOG_TRACE2,
                    MPE_MOD_SI,
                    "<%s::process_tune_sync> - Error getting transport stream handle..\n",
                    PSIMODULE);
        }
        mpe_siReleaseWriteLock();
    }
    if (TRUE == isAnalogChannel(psi_data))
    {
        MPE_LOG(MPE_LOG_TRACE2,
                MPE_MOD_SI,
                "<%s::process_tune_sync> - analog channel - return to idle state\n",
                PSIMODULE);
        /*
         ** SITP failed to get the tuner info from media mgr or the
         ** channel tuned is analog, so go back to the idle
         ** state for the tuner that received the tune_sync
         ** and reset the tuner_info values. Set the timer to SHORT
         ** here to allow for any other state machines to be tested
         ** for needed handling.
         */
        psi_data->fsm_data->psi_state = SITP_PSI_IDLE;
        return retCode;
    }

    if(psi_data->fsm_data->filter_group_initial_PAT != NULL)
    {
        retCode = set_filter(psi_data,
                psi_data->fsm_data->filter_group_initial_PAT,
                SITP_PSI_FILTER_PRIORITY_INITIAL_PAT,
                psi_data->fsm_data->sitp_psi_pat_timeout_val,
                &psi_data->fsm_data->shared_filter_PAT);
        if (SITP_SUCCESS != retCode)
        {
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                    "<%s::process_tune_sync> - activate_filter failed..\n",
                    PSIMODULE);
            psi_data->fsm_data->psi_state = SITP_PSI_IDLE;
        }
        else
        {
            psi_data->fsm_data->psi_state = SITP_PSI_WAIT_INITIAL_PAT;
        }
    }
    return retCode;
}

static void process_oob_event(sitp_psi_data_t *psi_data)
{
    mpe_Error retCode = SITP_SUCCESS;
    uint32_t frequency = 0;
    uint8_t mode = MPE_SI_MODULATION_UNKNOWN;

    switch(psi_data->tuner_type)
    {
        case SITP_PSI_TUNER_TYPE_OOB:
        {
            frequency = MPE_SI_OOB_FREQUENCY;
        }
        break;
        case SITP_PSI_TUNER_TYPE_DSG:
        {
            frequency = MPE_SI_DSG_FREQUENCY;
        }
        break;
        case SITP_PSI_TUNER_TYPE_HN:
        {
            frequency = MPE_SI_HN_FREQUENCY;
        }
        break;
        default:
            break;
    }

    {   // Get OOB/DSG/HN transport stream handle
        mpe_siLockForWrite();
        if (mpe_siGetTransportStreamEntryFromFrequencyModulation(frequency,
                mode, &(psi_data->fsm_data->ts_handle)) != MPE_SI_SUCCESS)
        {
            MPE_LOG(MPE_LOG_TRACE2,
                    MPE_MOD_SI,
                    "<%s::process_oob_event> - Error getting transport stream handle..\n",
                    PSIMODULE);
        }
        mpe_siReleaseWriteLock();
    }

    if(psi_data->fsm_data->filter_group_initial_PAT != NULL)
    {
        retCode = set_filter(psi_data,
                psi_data->fsm_data->filter_group_initial_PAT,
                SITP_PSI_FILTER_PRIORITY_INITIAL_PAT,
                psi_data->fsm_data->sitp_psi_pat_timeout_val,
                &psi_data->fsm_data->shared_filter_PAT);
        if (SITP_SUCCESS != retCode)
        {
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                    "<%s::process_oob_event> - activate_filter failed..\n",
                    PSIMODULE);
            psi_data->fsm_data->psi_state = SITP_PSI_IDLE;
        }
        else
        {
            psi_data->fsm_data->psi_state = SITP_PSI_WAIT_INITIAL_PAT ;
        }
    }
}

// Callback function
static uint32_t mpe_sitpSharedFilterCallback( mpe_SharedFilter sharedFilter,
                                              mpe_FilterGroup filterGroup,
                                              void * userPointer,
                                              uint32_t userData,
                                              mpe_SharedFilterEvent event,
                                              uint16_t sectionSize,
                                              uint8_t sectionData[],
                                              mpe_Bool isLast )
{
    mpe_Error retCode = MPE_SUCCESS;
    uint32_t data = userData;
    sitp_psi_filter_t *psi_filter = (sitp_psi_filter_t *) userPointer;
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
            "<%s::mpe_sitpSharedFilterCallback> - userData psi_filter:0x%x\n", PSIMODULE, psi_filter);
    sitp_psi_fsm_data_t *fsm = NULL;
    sitp_psi_data_t *psi_data = NULL;
    uint16_t program_number = PROGRAM_NUMBER_UNKNOWN;
    uint8_t version = 0;
    uint8_t table_type = TABLE_TYPE_UNKNOWN;
    mpe_SharedFilterState filterState = MPE_FILTERGROUP_FILTERSTATE_INVALID;

    /*
     ** Acquire Mutex
     */
    mpe_mutexAcquire(g_sitp_psi_mutex);

    retCode =  filter_getSharedFilterState( sharedFilter, &filterState);
    if ( (retCode != MPE_SUCCESS) || (filterState == MPE_FILTERGROUP_FILTERSTATE_DELETED) )
    {
        MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                "<%s::mpe_sitpSharedFilterCallback> - Filter was invalid/deleted before we could process event.\n", PSIMODULE);
    }
    else
    {
        // Remove the filter
        filter_removeSharedFilter(sharedFilter);
        psi_filter->filter_set = FALSE;
        sharedFilter = MPE_FILTERGROUP_INVALID_FILTER;
    }

    switch (event)
    {
        case MPE_FILTERGROUP_EVENT_MATCHED:
        {
            // Check the state of the filter
            // event: MATCHED state: matched
            // If its not matched bail out
            MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                    "<%s::mpe_sitpSharedFilterCallback> - received MPE_FILTERGROUP_EVENT_MATCHED..\n",
                    PSIMODULE);

            if( filterState != MPE_FILTERGROUP_FILTERSTATE_MATCHED)
            {
                MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                        "<%s::mpe_sitpSharedFilterCallback> - Received MATCHED event, but filter not in MATCHED state - ignoring event\n", PSIMODULE);
                break;
            }

            // Filter is in correct state
            {
                fsm = (sitp_psi_fsm_data_t *)psi_filter->fsm_data;
                MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                        "<%s::mpe_sitpSharedFilterCallback> - userData fsm:0x%x\n", PSIMODULE, fsm);
                psi_data = (sitp_psi_data_t *) fsm->psi_data;
                MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                        "<%s::mpe_sitpSharedFilterCallback> - userData psi_data:0x%x\n", PSIMODULE, psi_data);
                /*
                 * userData (msb to lsb): ((program_number (16 bits) | tabl_Type (8 bits) | version_number (8 bits))
                 */
                program_number = PROGRAM_NUMBER_UNKNOWN;
                version = data & 0xFF;
                MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                        "<%s::mpe_sitpSharedFilterCallback> - userData version:0x%x\n", PSIMODULE, version);
                table_type = (data >> 8) & 0xFF;
                MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                        "<%s::mpe_sitpSharedFilterCallback> - userData table_type:0x%x\n", PSIMODULE, table_type);
                if(psi_data->tuner_type == SITP_PSI_TUNER_TYPE_IB)
                {
                    program_number = (data >> 16) & 0xFFFF;
                    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                            "<%s::mpe_sitpSharedFilterCallback> - tunerId:%d program_number:0x%x\n", PSIMODULE, psi_data->psi_params.p_ib_tune_info.tuner_id, program_number);
                }
            }

            switch (table_type)
            {
                case TABLE_TYPE_PAT:
                {
                    // If this is initial PAT then there should not be
                    // any existing filters, so it is a no-op.
    	            // If its a new revision of PAT, then we need to cancel
                    // existing PMT filters. New PMT filters will be set
                    // after parsing the PAT.
                    cancel_all_filters(psi_data);

                    // If this is a revision of PAT, clear the secondary
                    // PMT list to make way for new list.
                    // For initial PAT this is a no-op.
                    clear_secondary_PMT_list(fsm);

                    // Log (SNMP) when initial PAT is acquired!
                    // We want to only log when initial PAT is acquired (not revisions)
                    if( (psi_data->tuner_type == SITP_PSI_TUNER_TYPE_IB) &&
                        (psi_filter->version_number == INIT_TABLE_VERSION) )
                    {
                        mpe_TimeMillis current_time = 0;
                        char timeStampString[256] = " ";
                        char message[256] = " ";

                        // Record the current time
                        mpeos_timeGetMillis(&current_time);
                        // Time = %"PRIu64"
                        sprintf(timeStampString, "%"PRIu64, (uint64_t)current_time);
                        sprintf(message, "(Performance.Tuning-INFO) PAT Acquired: Tuner %d", psi_data->psi_params.p_ib_tune_info.tuner_id);
                        mpeos_dbgAddLogEntry(ocStbHostSystemLoggingEventTable_oid, timeStampString, message);
                    }

                    // Get lock
                    mpe_siLockForWrite();
                    // Parse PAT section
                    // Signal SI DB events (done in parse code)
                    retCode = parseAndUpdatePAT(sectionSize, sectionData,
                            psi_data->tuner_type,
                            &psi_data->psi_params,
                            psi_data->fsm_data);
                    // release lock
                    mpe_siReleaseWriteLock();

                    if (SITP_SUCCESS != retCode)
                    {
                        MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                                "<%s::mpe_sitpSharedFilterCallback> - parseAndUpdatePAT returned error %d\n",
                                PSIMODULE, retCode);
                        break;
                    }

                    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                            "<%s::mpe_sitpSharedFilterCallback> - psi state: %s\n",
                            PSIMODULE, sitp_psi_stateToString(psi_data->fsm_data->psi_state));
                    {
                        // Add Primary PMT filter to shared filter group
                        // PAT parsing has to complete before setting PMT filters
                        // Need PMT Pid values from the PAT

                        // If the primary program number is known set that PMT filter
                        // If the tune is to a transport stream there will not be
                        // a primary program. All programs are secondary
                        if(psi_data->fsm_data->shared_filter_primary_PMT.program_number == PROGRAM_NUMBER_UNKNOWN)
                        {
                            MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                                    "<%s::mpe_sitpSharedFilterCallback> - Primary program number is not set.\n",
                                    PSIMODULE);
                        }
                        else
                        {
                            if(psi_data->fsm_data->filter_group_initial_primary_PMT != NULL)
                            {
                                retCode = set_filter(psi_data,
                                        psi_data->fsm_data->filter_group_initial_primary_PMT,
                                        SITP_PSI_FILTER_PRIORITY_INITIAL_PRIMARY_PMT,
                                        psi_data->fsm_data->sitp_psi_pmt_timeout_val,
                                        &psi_data->fsm_data->shared_filter_primary_PMT);
                                if (retCode != SITP_SUCCESS)
                                {
                                    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                                            "<%s::mpe_sitpSharedFilterCallback> - activate_filter failed with %d\n",
                                            PSIMODULE, retCode);
                                    break;
                                }
                            }
                            else
                            {
                                MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                                        "<%s::mpe_sitpSharedFilterCallback> - filter_group_initial_primary_PMT is NULL.\n",
                                        PSIMODULE);
                            }
                        }

                        if(psi_data->fsm_data->filter_group_revision_PAT != NULL)
                        {
                            // Set PAT revision filter
                            retCode = set_filter(psi_data,
                                    psi_data->fsm_data->filter_group_revision_PAT,
                                    SITP_PSI_FILTER_PRIORITY_REVISION_PAT,
                                    0, // no timeout for revision filters
                                    &psi_data->fsm_data->shared_filter_PAT);
                            if (retCode != SITP_SUCCESS)
                            {
                                MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                                        "<%s::mpe_sitpSharedFilterCallback> - activate_filter failed with error %d\n",
                                        PSIMODULE, retCode);
                                break;
                            }
                        }
                        else
                        {
                            MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                                    "<%s::mpe_sitpSharedFilterCallback> - filter_group_revision_PAT is NULL.\n",
                                    PSIMODULE);
                        }

                        // Add multiple secondary PMT filter(s) to shared filter group
                        // Lower priority than primary PMT filter
                        if (llist_cnt(psi_data->fsm_data->shared_filter_secondary_PMTs) > 0)
                        {
                            LINK *lp = llist_first(psi_data->fsm_data->shared_filter_secondary_PMTs);
                            while (lp)
                            {
                                sitp_psi_filter_t *psi_filter = (sitp_psi_filter_t *) llist_getdata(lp);
                                if (psi_filter)
                                {
                                    if(psi_data->fsm_data->filter_group_initial_secondary_PMT != NULL)
                                    {
                                        retCode = set_filter(psi_data,
                                                psi_data->fsm_data->filter_group_initial_secondary_PMT,
                                                SITP_PSI_FILTER_PRIORITY_INITIAL_SECONDARY_PMT,
                                                psi_data->fsm_data->sitp_psi_pmt_timeout_val,
                                                psi_filter);
                                        if (retCode != SITP_SUCCESS)
                                        {
                                            MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                                                    "<%s::mpe_sitpSharedFilterCallback> - activate_filter returned  %d\n",
                                                    PSIMODULE, retCode);
                                        }
                                    }
                                    else
                                    {
                                        MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                                                "<%s::mpe_sitpSharedFilterCallback> - filter_group_initial_secondary_PMT is NULL.\n",
                                                PSIMODULE);
                                    }
                                }
                                lp = llist_after(lp);
                            }
                        }

                        if(psi_data->fsm_data->shared_filter_primary_PMT.program_number == PROGRAM_NUMBER_UNKNOWN)
                        {
                            // If tuned to a transport stream set state to SITP_PSI_WAIT_REVISION
                            psi_data->fsm_data->psi_state = SITP_PSI_WAIT_REVISION;
                        }
                        else
                        {
                            // set state to SITP_PSI_WAIT_INITIAL_PRIMARY_PMT
                            psi_data->fsm_data->psi_state = SITP_PSI_WAIT_INITIAL_PRIMARY_PMT;
                        }
                    }
                }
                break;

                case TABLE_TYPE_PMT:
                {
                    // Log (SNMP) when PMT is acquired
                    // Include program number
                    // The Program Map Table (PMT) for the currently-selected program is completely
                    // acquired from the network. In the case of a transport stream tune, this will be
                    // triggered by acquisition of the first PMT.
                    //static mpe_Bool isFirstPMTInTransportStream(sitp_psi_fsm_data_t *fsm_data, uint16_t program_number)
                    if( (psi_data->tuner_type == SITP_PSI_TUNER_TYPE_IB) &&
                        (psi_filter->version_number == INIT_TABLE_VERSION) &&
                         ( (psi_data->psi_params.p_ib_tune_info.program_number == program_number) ||
                           // Transport stream tune
                          ((psi_data->fsm_data->shared_filter_primary_PMT.program_number == PROGRAM_NUMBER_UNKNOWN) &&
                          (isFirstPMTInTransportStream(psi_data->fsm_data, program_number)))) )
                    {
                        mpe_TimeMillis current_time = 0;
                        char timeStampString[256] = " ";
                        char message[256] = " ";

                        // Record the current time
                        mpeos_timeGetMillis(&current_time);
                        // Start time = %"PRIu64"
                        sprintf(timeStampString, "%"PRIu64, (uint64_t)current_time);
                        sprintf(message, "(Performance.Tuning-INFO) PMT Acquired: Tuner %d, Program %d", psi_data->psi_params.p_ib_tune_info.tuner_id,
                                                                                                         program_number);
                        // Log when PAT is acquired
                        // When initial rev is acquired (not revisions)
                        mpeos_dbgAddLogEntry(ocStbHostSystemLoggingEventTable_oid, timeStampString, message);
                    }

                    // Get lock
                    mpe_siLockForWrite();
                    // Parse PMT section
                    retCode = parseAndUpdatePMT(sectionSize, sectionData,
                            psi_data->tuner_type,
                            &psi_data->psi_params,
                            psi_data->fsm_data);
                    // release lock
                    mpe_siReleaseWriteLock();

                    if (SITP_SUCCESS != retCode)
                    {
                        MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                                "<%s::mpe_sitpSharedFilterCallback> - parseAndUpdatePMT returned error %d\n",
                                PSIMODULE, retCode);
                        break;
                    }

                    // If PMT is for the primary program number
                    if(psi_data->psi_params.p_ib_tune_info.program_number == program_number)
                    {
                        // If this is the primary PMT set revision filter
                        if(psi_data->fsm_data->filter_group_revision_primary_PMT != NULL)
                        {
                            // Set primary PMT revision filter
                            retCode = set_filter(psi_data,
                                    psi_data->fsm_data->filter_group_revision_primary_PMT,
                                    SITP_PSI_FILTER_PRIORITY_REVISION_PMT,
                                    0, // no timeout for revision filters
                                    &psi_data->fsm_data->shared_filter_primary_PMT);
                            if (retCode != SITP_SUCCESS)
                            {
                                MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                                        "<%s::mpe_sitpSharedFilterCallback> - activate_filter failed with error %d\n",
                                        PSIMODULE, retCode);
                                break;
                            }
                        }
                        else
                        {
                            MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                                    "<%s::mpe_sitpSharedFilterCallback> - filter_group_revision_primary_PMT is NULL.\n",
                                    PSIMODULE);
                        }
                    }
                    else
                    {
                        // set secondary PMT revision filter
                        if(psi_data->fsm_data->filter_group_revision_secondary_PMT != NULL)
                        {
                            retCode = set_filter(psi_data,
                                    psi_data->fsm_data->filter_group_revision_secondary_PMT,
                                    SITP_PSI_FILTER_PRIORITY_REVISION_PMT,
                                    0,
                                    psi_filter);
                        }
                        else
                        {
                            MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                                    "<%s::mpe_sitpSharedFilterCallback> - filter_group_revision_secondary_PMT is NULL.\n",
                                    PSIMODULE);
                        }
                    }

                    // set state to revision
                    psi_data->fsm_data->psi_state = SITP_PSI_WAIT_REVISION;
                }
                break;

                default:
                    // Unknown table_type (flag error)
                    MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                            "<%s::mpe_sitpSharedFilterCallback> - unknown table_type..\n",
                            PSIMODULE);
                    break;
            }
        }
        break;
        case MPE_FILTERGROUP_EVENT_TIMEOUT:
        {
            // Check the state of the filter
            // event: Timeout state: 'timed out'
            // If state is not 'timedOut' bail out
            MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                    "<%s::mpe_sitpSharedFilterCallback> - received MPE_FILTERGROUP_EVENT_TIMEOUT..\n",
                    PSIMODULE);

            if(filterState != MPE_FILTERGROUP_FILTERSTATE_TIMEDOUT)
            {
                MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                        "<%s::mpe_sitpSharedFilterCallback> - Received timeout event, but filter not in timeout state - ignoring event\n", PSIMODULE);
                 break;
            }

            // Filter is in correct state
            {
                fsm = (sitp_psi_fsm_data_t *)psi_filter->fsm_data;
                MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                        "<%s::mpe_sitpSharedFilterCallback> - userData fsm:0x%x\n", PSIMODULE, fsm);
                psi_data = (sitp_psi_data_t *) fsm->psi_data;
                MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                        "<%s::mpe_sitpSharedFilterCallback> - userData psi_data:0x%x\n", PSIMODULE, psi_data);

                /*
                 * userData (msb to lsb): ((program_number (16 bits) | tabl_Type (8 bits) | version_number (8 bits))
                 */
                program_number = PROGRAM_NUMBER_UNKNOWN;
                version = data & 0xFF;
                MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                        "<%s::mpe_sitpSharedFilterCallback> - userData version:0x%x\n", PSIMODULE, version);
                table_type = (data >> 8) & 0xFF;
                MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                        "<%s::mpe_sitpSharedFilterCallback> - userData table_type:0x%x\n", PSIMODULE, table_type);
                if(psi_data->tuner_type == SITP_PSI_TUNER_TYPE_IB)
                {
                    program_number = (data >> 16) & 0xFFFF;
                    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                            "<%s::mpe_sitpSharedFilterCallback> - tunerId:%d program_number:0x%x\n", PSIMODULE, psi_data->psi_params.p_ib_tune_info.tuner_id, program_number);
                }
            }

            // Signal timeout events
            // Process PAT/PMT timeout
            switch(table_type)
            {
                case TABLE_TYPE_PAT:
                {
                    MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,
                            "<%s::mpe_sitpSharedFilterCallback> - received MPE_FILTERGROUP_EVENT_TIMEOUT..for PAT\n",
                            PSIMODULE);
                    if ((retCode = process_pat_timeout(psi_data)) != MPE_SUCCESS)
                    {
                        MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                                "<%s::mpe_sitpSharedFilterCallback> - process_pat_timeout returned error %d\n",
                                PSIMODULE, retCode);
                    }

                    if(psi_data->fsm_data->filter_group_initial_PAT != NULL)
                    {
                        // Since this filter timed out once set new filter at a lower priority
                        // such that it does not need to time share with other high priority filters
                        retCode = set_filter(psi_data,
                                psi_data->fsm_data->filter_group_initial_PAT,
                                SITP_PSI_FILTER_PRIORITY_REVISION_PAT,
                                0, // don't set a timeout in this case
                                &psi_data->fsm_data->shared_filter_PAT);
                        if (retCode != SITP_SUCCESS)
                        {
                            MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                                    "<%s::mpe_sitpSharedFilterCallback> - activate_filter after timeout failed.. with %d\n",
                                    PSIMODULE, retCode);
                        }
                    }
                    else
                    {
                        MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                                "<%s::mpe_sitpSharedFilterCallback> - filter_group_initial_PAT is NULL.\n",
                                PSIMODULE);
                    }
                }
                break;
                case TABLE_TYPE_PMT:
                {
                    MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,
                            "<%s::mpe_sitpSharedFilterCallback> - received MPE_FILTERGROUP_EVENT_TIMEOUT..for program_number:%d\n",
                            PSIMODULE, program_number);
                    if ((retCode = process_pmt_timeout(psi_data, program_number)) != MPE_SUCCESS)
                    {
                        MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                                "<%s::mpe_sitpSharedFilterCallback> - process_pmt_timeout returned error %d\n",
                                PSIMODULE, retCode);
                    }

                    // Timeout is for primary PMT
                    if(psi_data->psi_params.p_ib_tune_info.program_number == program_number)
                    {
                        if(psi_data->fsm_data->filter_group_initial_primary_PMT != NULL)
                        {
                            // Since this filter timed out once set new filter at a lower priority
                            // such that it does not need to time share with other high priority filters
                            retCode = set_filter(psi_data,
                                    psi_data->fsm_data->filter_group_initial_primary_PMT,
                                    SITP_PSI_FILTER_PRIORITY_REVISION_PMT,
                                    0, // don't set a timeout in this case
                                    &psi_data->fsm_data->shared_filter_primary_PMT);
                            if (retCode != SITP_SUCCESS)
                            {
                                MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                                        "<%s::mpe_sitpSharedFilterCallback> - activate_filter after timeout failed.. with %d\n",
                                        PSIMODULE, retCode);
                            }
                        }
                        else
                        {
                            MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                                    "<%s::mpe_sitpSharedFilterCallback> - filter_group_initial_primary_PMT is NULL.\n",
                                    PSIMODULE);
                        }
                   }
                   else // timeout is for secondary programs
                   {
                        if(psi_data->fsm_data->filter_group_initial_secondary_PMT != NULL)
                        {
                            // Since this filter timed out once set new filter at a lower priority
                            // such that it does not need to time share with other high priority filters
                            retCode = set_filter(psi_data,
                                    psi_data->fsm_data->filter_group_initial_secondary_PMT,
                                    SITP_PSI_FILTER_PRIORITY_REVISION_PMT,
                                    0, // don't set a timeout in this case
                                    psi_filter);
                            if (retCode != SITP_SUCCESS)
                            {
                                MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                                        "<%s::mpe_sitpSharedFilterCallback> - activate_filter after timeout failed.. with %d\n",
                                        PSIMODULE, retCode);
                            }
                        }
                        else
                        {
                            MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                                    "<%s::mpe_sitpSharedFilterCallback> - filter_group_initial_secondary_PMT is NULL.\n",
                                    PSIMODULE);
                        }
                    }
                }
                break;
                default:
                    break;
            }
        }
        break;
        case MPE_FILTERGROUP_EVENT_CANCELLED:
        {
            MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                    "<%s::mpe_sitpSharedFilterCallback> - received MPE_FILTERGROUP_EVENT_CANCELLED..\n",
                    PSIMODULE);

            if(filterState != MPE_FILTERGROUP_FILTERSTATE_CANCELLED)
            {
                MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
                        "<%s::mpe_sitpSharedFilterCallback> - Received CANCEL event, but filter not in CANCEL state - ignoring event\n", PSIMODULE);
                 break;
            }
        }
        break;
        default:
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                    "<%s::mpe_sitpSharedFilterCallback> - Unknown event..\n",
                    PSIMODULE);
        break;
    }
    // Start filter group
    retCode = filter_startFilterGroup(filterGroup);
    if(retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                "<%s::mpe_sitpSharedFilterCallback> - FilterGroup restart failed with error %d (0x%x)\n",
                PSIMODULE, retCode, retCode);
    }

    /*
     ** Release global mutex
     */
    mpe_mutexRelease(g_sitp_psi_mutex);

    return MPE_SUCCESS;
}

// Called from SI DB when a caller registers for PSI acquisition
// on DSG tunnel associated with the given appId
mpe_Error attachDSGTunnel(uint32_t appID)
{
    //mpe_Event eventCode;
    //void *eventData;
    mpe_Error retCode = SITP_SUCCESS;

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "<%s::attachDSGTunnel> appID = %u\n",
            PSIMODULE, appID);

    // Check the argument for validity
    if (0 == appID)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI,
                "<%s::attachDSGTunnel> ERROR Bad appID = %u\n", PSIMODULE,
                appID);
        return SITP_FAILURE;
    }

    //Acquire the mutex
    mpe_mutexAcquire(g_sitp_psi_mutex);

    //Add the new DSG Tunnel to the state machine.
    retCode = addDSGTunnelToStateMachine(appID);
    if (SITP_SUCCESS != retCode)
    {
        MPE_LOG(MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::attachDSGTunnel> ERROR failed to add DSG Tunnel machine Data for appID = %u\n",
                PSIMODULE, appID);
        retCode = SITP_FAILURE;
        goto attach_cleanup;
    }

    //Send an event to start the acquisition.
    if (0 != g_sitp_psi_queue)
    {
        mpeos_eventQueueSend(g_sitp_psi_queue, OOB_DSG_PSI_ACQUISITION,
                NULL, (void*) appID, 0);
    }

    mpe_mutexRelease(g_sitp_psi_mutex);
    return retCode;

attach_cleanup:

    //Failure - Cleanup the state machine.
    retCode = removeDSGTunnelFromStateMachine(appID);
    if (SITP_SUCCESS != retCode)
    {
        MPE_LOG(MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::detachDSGTunnel> - ERROR!!! - Failed to remove DSG tunnel FSM from state Machine\n",
                PSIMODULE);
    }
    //release the mutex and return.
    mpe_mutexRelease(g_sitp_psi_mutex);
    return retCode;
}

// Called from SI DB when a caller un-registers for PSI acquisition
// on DSG tunnel associated with the given appId
// reset the appID for filtering the DSG tunnel
mpe_Error detachDSGTunnel(uint32_t appID)
{
    mpe_Error retCode = MPE_SUCCESS;

    mpe_mutexAcquire(g_sitp_psi_mutex);

    //nothing to detach
    if (0 == sitp_psi_getNumActiveDSGChannels())
    {
        mpe_mutexRelease(g_sitp_psi_mutex);
        return SITP_FAILURE;
    }

    // TODO: Cancel any outstanding filters and destroy filter groups
    // Or do this inside the removeDSGSessionFromStateMachine method
    if(g_DSG_psi_data_array)
    {
        LINK *lp = llist_first(g_DSG_psi_data_array);
        while (lp)
        {
            sitp_psi_data_t *dsg_psi_data = (sitp_psi_data_t *) llist_getdata(lp);
            if(dsg_psi_data
                && dsg_psi_data->psi_params.p_dsg_info.dsg_app_id == appID)
            {
                cancel_all_filters(dsg_psi_data);
                // TODO: destroy DSG filter groups
                break;
            }
            lp = llist_after(lp);
        }
    }

    retCode = removeDSGTunnelFromStateMachine(appID);
    if (SITP_SUCCESS != retCode)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::detachDSGTunnel> - ERROR!!! - Failed to remove DSG tunnel FSM from state Machine\n",
                PSIMODULE);
        mpe_mutexRelease(g_sitp_psi_mutex);
        return SITP_FAILURE;
    }

    mpe_mutexRelease(g_sitp_psi_mutex);
    return SITP_SUCCESS;
}

static uint32_t sitp_psi_getNumActiveDSGChannels(void)
{
    return g_sitp_psi_DSG_count;
}

mpe_Error addDSGTunnelToStateMachine(uint32_t appID)
{
    //Add the new DSG machine to the end of the list.
    mpe_Error retCode = SITP_SUCCESS;
    LINK *lp = NULL;
    sitp_psi_data_t *dsg_psi_data = NULL;
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
            "<%s::addDSGTunnelToStateMachine> - Enter 1\n", PSIMODULE);

    /*
     * ASSERT: Caller has the dsg tunnel mutex
     */
    retCode = mpeos_memAllocP(MPE_MEM_SI, sizeof(sitp_psi_data_t),
            (void **) &(dsg_psi_data));
    if (SITP_SUCCESS != retCode)
    {
        MPE_LOG(MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::addDSGTunnelToStateMachine> - ERROR: Failed to allocate dsg psi data. retCode: %d\n",
                PSIMODULE, retCode);
        return SITP_FAILURE;
    }

    /*
     ** The fsm data struct state - this follows the same state machine as the OOB path where
     ** there is no round robin revisioning.
     */
    dsg_psi_data->tuner_type = SITP_PSI_TUNER_TYPE_DSG;
    dsg_psi_data->tuner_state = SITP_PSI_TUNER_STATE_TUNED;

    dsg_psi_data->psi_params.p_dsg_info.dsg_app_id = appID;

    retCode = mpeos_memAllocP(MPE_MEM_SI,
            sizeof(sitp_psi_fsm_data_t), (void **) &(dsg_psi_data->fsm_data));
    if (SITP_SUCCESS != retCode)
    {
        MPE_LOG(MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::createAndSetOOBTransportStreamDataArray> - ERROR: Failed to allocate fsm data . retCode: %d\n",
                PSIMODULE, retCode);
        return SITP_FAILURE;
    }

    init_fsm_data(dsg_psi_data->fsm_data);

    dsg_psi_data->fsm_data->shared_filter_PAT.fsm_data = (void *)dsg_psi_data->fsm_data;
    dsg_psi_data->fsm_data->shared_filter_primary_PMT.fsm_data = (void *)dsg_psi_data->fsm_data;

    dsg_psi_data->fsm_data->psi_data = (void *)(dsg_psi_data);

    dsg_psi_data->fsm_data->sitp_psi_pat_timeout_val = g_sitp_oob_pat_timeout_interval;
    dsg_psi_data->fsm_data->sitp_psi_pmt_timeout_val = g_sitp_oob_pmt_timeout_interval;
    dsg_psi_data->fsm_data->sitp_psi_round_robin_val = 0;

    // Set the filter source fields here
    dsg_psi_data->fsm_data->filter_source.sourceType = MPE_FILTER_SOURCE_DSG_APPID;
    dsg_psi_data->fsm_data->filter_source.parm.p_DSGA.appId = appID;
    dsg_psi_data->fsm_data->filter_source.pid = -1;

    // Create 6 discrete filter groups with no time out
    // For DSG no time division multiplexing is performed
    retCode = filter_createFilterGroup( 0,
                                        "MPE_dsg_filter_group_init_pat",
                                        &dsg_psi_data->fsm_data->filter_group_initial_PAT);
    retCode = filter_createFilterGroup( 0,
                                        "MPE_dsg_filter_group_init_primary_pmt",
                                        &dsg_psi_data->fsm_data->filter_group_initial_primary_PMT);
    retCode = filter_createFilterGroup( 0,
                                        "MPE_dsg_filter_group_init_secondary_pmt",
                                        &dsg_psi_data->fsm_data->filter_group_initial_secondary_PMT);
    retCode = filter_startFilterGroup(dsg_psi_data->fsm_data->filter_group_initial_PAT);
    retCode = filter_startFilterGroup(dsg_psi_data->fsm_data->filter_group_initial_primary_PMT);
    retCode = filter_startFilterGroup(dsg_psi_data->fsm_data->filter_group_initial_secondary_PMT);

    if(g_sitp_oob_psi_process_table_revision == TRUE)
    {
        retCode = filter_createFilterGroup( 0,
                                            "MPE_dsg_filter_group_pat_rev",
                                            &dsg_psi_data->fsm_data->filter_group_revision_PAT);
        retCode = filter_createFilterGroup( 0,
                                            "MPE_dsg_filter_group_primary_pmt_rev",
                                            &dsg_psi_data->fsm_data->filter_group_revision_primary_PMT);
        retCode = filter_createFilterGroup( 0,
                                            "MPE_dsg_filter_group_secondary_pmt_rev",
                                            &dsg_psi_data->fsm_data->filter_group_revision_secondary_PMT);

        retCode = filter_startFilterGroup(dsg_psi_data->fsm_data->filter_group_revision_PAT);
        retCode = filter_startFilterGroup(dsg_psi_data->fsm_data->filter_group_revision_primary_PMT);
        retCode = filter_startFilterGroup(dsg_psi_data->fsm_data->filter_group_revision_secondary_PMT);
    }

    if(retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
                "<%s::addDSGTunnelToStateMachine> - DSG filter groups failed to be created/started..\n", PSIMODULE);
    }
    /*
     * allocate the DSG structure.
     */
    if(g_DSG_psi_data_array == NULL)
    {
        g_DSG_psi_data_array = llist_create();
    }
    lp = llist_mklink((void *) dsg_psi_data);
    llist_append(g_DSG_psi_data_array, lp);

    //Increment the DSG counter
    g_sitp_psi_DSG_count += 1;
    MPE_LOG(MPE_LOG_INFO,
            MPE_MOD_SI,
            "<%s::addDSGTunnelToStateMachine> - g_sitp_psi_DSG_count: %d\n",
            PSIMODULE, g_sitp_psi_DSG_count);
    MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,
            "<%s::addDSGTunnelToStateMachine> - done retCode: %d\n", PSIMODULE,
            retCode);
    return retCode;
}

mpe_Error removeDSGTunnelFromStateMachine(uint32_t appID)
{
    //Add the new DSG machine to the end of the list.
    mpe_Error retCode = SITP_SUCCESS;
    LINK *lp = NULL;
    sitp_psi_data_t *dsg_psi_data = NULL;

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::removeDSGTunnelFromStateMachine> - Enter\n", PSIMODULE);

    /*
     * ASSERT: Caller has the dsg tunnel mutex
     */
    MPE_LOG(MPE_LOG_TRACE2,
            MPE_MOD_SI,
            "<%s::removeDSGTunnelFromStateMachine> - g_sitp_psi_DSG_count: %d\n",
            PSIMODULE, g_sitp_psi_DSG_count);

    if(g_DSG_psi_data_array)
    {
        lp = llist_first(g_DSG_psi_data_array);
        while (lp)
        {
            dsg_psi_data = (sitp_psi_data_t *) llist_getdata(lp);
            if(dsg_psi_data
                && dsg_psi_data->psi_params.p_dsg_info.dsg_app_id == appID)
            {
                // De-allocate memory associated with this node (fsm_data etc.)
                llist_rmlink(lp);
                break;
            }
            lp = llist_after(lp);
        }
    }

    if(dsg_psi_data->fsm_data->shared_filter_secondary_PMTs)
    {
        lp = llist_first(dsg_psi_data->fsm_data->shared_filter_secondary_PMTs);
        while(lp)
        {
            // de-allocate psi_filter allocations
            sitp_psi_filter_t *psi_filter = llist_getdata(lp);
            if (psi_filter)
            {
                llist_rmlink(lp);
                mpeos_memFreeP(MPE_MEM_SI, psi_filter);
            }
            lp = llist_after(lp);
        }
    }

    //destroy filter groups
    retCode = filter_destroyFilterGroup(dsg_psi_data->fsm_data->filter_group_initial_PAT);
    retCode = filter_destroyFilterGroup(dsg_psi_data->fsm_data->filter_group_initial_primary_PMT);
    retCode = filter_destroyFilterGroup(dsg_psi_data->fsm_data->filter_group_initial_secondary_PMT);
    if(dsg_psi_data->fsm_data->filter_group_revision_PAT != NULL)
    {
        retCode = filter_destroyFilterGroup(dsg_psi_data->fsm_data->filter_group_revision_PAT);
    }
    if(dsg_psi_data->fsm_data->filter_group_revision_primary_PMT != NULL)
    {
        retCode = filter_destroyFilterGroup(dsg_psi_data->fsm_data->filter_group_revision_primary_PMT);
    }
    if(dsg_psi_data->fsm_data->filter_group_revision_secondary_PMT != NULL)
    {
        retCode = filter_destroyFilterGroup(dsg_psi_data->fsm_data->filter_group_revision_secondary_PMT);
    }
    if(retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
                "<%s::removeDSGTunnelFromStateMachine> - DSG filter groups failed to be destroyed..\n", PSIMODULE);
    }

    retCode = mpeos_memFreeP(MPE_MEM_SI, dsg_psi_data);
    if (SITP_SUCCESS != retCode)
    {
        MPE_LOG(MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::removeDSGTunnelFromStateMachine> - ERROR: Failed to free DSG PSI datak retCode: %d\n",
                PSIMODULE, retCode);
    }
    //decrement the global DSG count
    g_sitp_psi_DSG_count -= 1;

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::removeDSGTunnelFromStateMachine> - Exit\n", PSIMODULE);
    return retCode;
}

mpe_Error attachHnStreamSession(mpe_HnStreamSession session)
{
    mpe_Error retCode = SITP_SUCCESS;

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "<%s::attachHnStreamSession> session = %x \n",
            PSIMODULE, session);

    // Check the argument for validity
    if (session == 0)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI,
                "<%s::attachHnStreamSession> ERROR Bad session = %x \n", PSIMODULE,
                session);
        return SITP_FAILURE;
    }

    //Acquire the mutex
    mpe_mutexAcquire(g_sitp_psi_mutex);

    //Add session to the state machine.
    retCode = addHNSessionToStateMachine(session);
    if (SITP_SUCCESS != retCode)
    {
        MPE_LOG(MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::attachHnStreamSession> ERROR failed to add state machine Data for session = %x \n",
                PSIMODULE, session);
        retCode = SITP_FAILURE;
        goto attach_cleanup;
    }

    //Send an event to start the acquisition.
    if (0 != g_sitp_psi_queue)
    {
        mpeos_eventQueueSend(g_sitp_psi_queue, OOB_HN_STREAM_PSI_ACQUISITION,
                NULL, (void*) session, 0);
    }

    mpe_mutexRelease(g_sitp_psi_mutex);
    return retCode;

attach_cleanup:

    //Failure - Cleanup the state machine.
    retCode = removeHNSessionFromStateMachine(session);
    if (SITP_SUCCESS != retCode)
    {
        MPE_LOG(MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::attachHnStreamSession> - ERROR!!! - Failed to remove DSG tunnel FSM from state Machine\n",
                PSIMODULE);
    }
    //release the mutex and return.
    mpe_mutexRelease(g_sitp_psi_mutex);
    return retCode;
}

/* reset the session for filtering the HN stream */
mpe_Error detachHnStreamSession(mpe_HnStreamSession session)
{
    mpe_Error retCode = MPE_SUCCESS;

    mpe_mutexAcquire(g_sitp_psi_mutex);

    //nothing to detach
    if (0 == sitp_psi_getNumActiveHnStreams())
    {
        mpe_mutexRelease(g_sitp_psi_mutex);
        return SITP_FAILURE;
    }

    // TODO: Cancel any outstanding filters and destroy filter groups
    // Or do this inside the removeHNSessionFromStateMachine method
    if(g_HN_psi_data_array)
    {
        LINK *lp = llist_first(g_HN_psi_data_array);
        while (lp)
        {
            sitp_psi_data_t *hn_psi_data = (sitp_psi_data_t *) llist_getdata(lp);
            if(hn_psi_data
                && hn_psi_data->psi_params.p_hn_stream_info.hn_stream_session == session)
            {
                cancel_all_filters(hn_psi_data);
                // TODO: destroy HN filter groups is done inside removeHNSessionFromStateMachine
                break;
            }
            lp = llist_after(lp);
        }
    }

    retCode = removeHNSessionFromStateMachine(session);
    if (SITP_SUCCESS != retCode)
    {
        MPE_LOG(MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::detachHnStreamSession> - ERROR!!! - Failed to remove HN Stream FSM from state Machine\n",
                PSIMODULE);
        mpe_mutexRelease(g_sitp_psi_mutex);
        return SITP_FAILURE;
    }

    mpe_mutexRelease(g_sitp_psi_mutex);
    return SITP_SUCCESS;
}

mpe_Error addHNSessionToStateMachine(mpe_HnStreamSession session)
{
    mpe_Error retCode = SITP_SUCCESS;
    LINK *lp = NULL;
    sitp_psi_data_t *hn_psi_data = NULL;
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_SI,
            "<%s::addHNSessionToStateMachine> - Enter \n", PSIMODULE);

    retCode = mpeos_memAllocP(MPE_MEM_SI, sizeof(sitp_psi_data_t),
            (void **) &(hn_psi_data));
    if (SITP_SUCCESS != retCode)
    {
        MPE_LOG(MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::addHNSessionToStateMachine> - ERROR: Failed to allocate hn psi data. retCode: %d\n",
                PSIMODULE, retCode);
        return SITP_FAILURE;
    }

    /*
     ** The fsm data struct state - this follows the same state machine as the OOB path where
     ** there is no round robin revisioning.
     */
    hn_psi_data->tuner_type = SITP_PSI_TUNER_TYPE_HN;
    hn_psi_data->tuner_state = SITP_PSI_TUNER_STATE_TUNED;

    hn_psi_data->psi_params.p_hn_stream_info.hn_stream_session = session;

    retCode = mpeos_memAllocP(MPE_MEM_SI,
            sizeof(sitp_psi_fsm_data_t), (void **) &(hn_psi_data->fsm_data));
    if (SITP_SUCCESS != retCode)
    {
        MPE_LOG(MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::addHNSessionToStateMachine> - ERROR: Failed to allocate fsm data . retCode: %d\n",
                PSIMODULE, retCode);
        return SITP_FAILURE;
    }

    init_fsm_data(hn_psi_data->fsm_data);

    hn_psi_data->fsm_data->shared_filter_PAT.fsm_data = (void *)hn_psi_data->fsm_data;
    hn_psi_data->fsm_data->shared_filter_primary_PMT.fsm_data = (void *)hn_psi_data->fsm_data;

    hn_psi_data->fsm_data->psi_data = (void *)(hn_psi_data);

    hn_psi_data->fsm_data->sitp_psi_pat_timeout_val = g_sitp_hn_pat_timeout_interval;
    hn_psi_data->fsm_data->sitp_psi_pmt_timeout_val = g_sitp_hn_pmt_timeout_interval;
    hn_psi_data->fsm_data->sitp_psi_round_robin_val = 0;

    // Set the filter source fields here
    hn_psi_data->fsm_data->filter_source.sourceType = MPE_FILTER_SOURCE_HN_STREAM;
    hn_psi_data->fsm_data->filter_source.parm.p_HNS.hn_stream_session = session;
    hn_psi_data->fsm_data->filter_source.pid = -1;

    // Create 6 discrete filter groups with no time out
    // For HN remote playback no time division multiplexing is performed
    retCode = filter_createFilterGroup( 0,
                                        "MPE_hn_filter_group_init_pat",
                                        &hn_psi_data->fsm_data->filter_group_initial_PAT);
    retCode = filter_createFilterGroup( 0,
                                        "MPE_hn_filter_group_init_primary_pmt",
                                        &hn_psi_data->fsm_data->filter_group_initial_primary_PMT);
    retCode = filter_createFilterGroup( 0,
                                        "MPE_hn_filter_group_init_secondary_pmt",
                                        &hn_psi_data->fsm_data->filter_group_initial_secondary_PMT);

    retCode = filter_startFilterGroup(hn_psi_data->fsm_data->filter_group_initial_PAT);
    retCode = filter_startFilterGroup(hn_psi_data->fsm_data->filter_group_initial_primary_PMT);
    retCode = filter_startFilterGroup(hn_psi_data->fsm_data->filter_group_initial_secondary_PMT);

    if(g_sitp_hn_psi_process_table_revision == TRUE)
    {
        retCode = filter_createFilterGroup( 0,
                                            "MPE_hn_filter_group_pat_rev",
                                            &hn_psi_data->fsm_data->filter_group_revision_PAT);
        retCode = filter_createFilterGroup( 0,
                                            "MPE_hn_filter_group_primary_pmt_rev",
                                            &hn_psi_data->fsm_data->filter_group_revision_primary_PMT);
        retCode = filter_createFilterGroup( 0,
                                            "MPE_hn_filter_group_secondary_pmt_rev",
                                            &hn_psi_data->fsm_data->filter_group_revision_secondary_PMT);

        retCode = filter_startFilterGroup(hn_psi_data->fsm_data->filter_group_revision_PAT);
        retCode = filter_startFilterGroup(hn_psi_data->fsm_data->filter_group_revision_primary_PMT);
        retCode = filter_startFilterGroup(hn_psi_data->fsm_data->filter_group_revision_secondary_PMT);
    }

    if(retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
                "<%s::addDSGTunnelToStateMachine> - HN filter groups failed to be created/started..\n", PSIMODULE);
    }
    /*
     * allocate the HN structure.
     */
    if(g_HN_psi_data_array == NULL)
    {
        g_HN_psi_data_array = llist_create();
    }
    lp = llist_mklink((void *) hn_psi_data);
    llist_append(g_HN_psi_data_array, lp);

    //Increment the HN counter
    g_sitp_psi_HN_count += 1;

    MPE_LOG(MPE_LOG_INFO,
            MPE_MOD_SI,
            "<%s::addHNSessionToStateMachine> - g_sitp_psi_HN_count: %d\n",
            PSIMODULE, g_sitp_psi_HN_count);
    MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,
            "<%s::addHNSessionToStateMachine> - done retCode: %d\n", PSIMODULE,
            retCode);
    return retCode;
}

mpe_Error removeHNSessionFromStateMachine(mpe_HnStreamSession session)
{
    //Remove the HN session from the list.
    mpe_Error retCode = SITP_SUCCESS;
    LINK *lp = NULL;
    sitp_psi_data_t *hn_psi_data = NULL;

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::removeHNTunnelFromStateMachine> - Enter\n", PSIMODULE);

    MPE_LOG(MPE_LOG_TRACE2,
            MPE_MOD_SI,
            "<%s::removeHNTunnelFromStateMachine> - g_sitp_psi_HN_count: %d\n",
            PSIMODULE, g_sitp_psi_DSG_count);

    if(g_HN_psi_data_array)
    {
        lp = llist_first(g_HN_psi_data_array);
        while (lp)
        {
            hn_psi_data = (sitp_psi_data_t *) llist_getdata(lp);
            if(hn_psi_data
                && hn_psi_data->psi_params.p_hn_stream_info.hn_stream_session == session)
            {
                // De-allocate memory associated with this node (fsm_data etc.)
                llist_rmlink(lp);
                break;
            }
            lp = llist_after(lp);
        }
    }

    if(hn_psi_data->fsm_data->shared_filter_secondary_PMTs)
    {
        lp = llist_first(hn_psi_data->fsm_data->shared_filter_secondary_PMTs);
        while(lp)
        {
            // de-allocate psi_filter allocations
            sitp_psi_filter_t *psi_filter = llist_getdata(lp);
            if (psi_filter)
            {
                llist_rmlink(lp);
                mpeos_memFreeP(MPE_MEM_SI, psi_filter);
            }
            lp = llist_after(lp);
        }
    }

    //destroy filter groups
    retCode = filter_destroyFilterGroup(hn_psi_data->fsm_data->filter_group_initial_PAT);
    retCode = filter_destroyFilterGroup(hn_psi_data->fsm_data->filter_group_initial_primary_PMT);
    retCode = filter_destroyFilterGroup(hn_psi_data->fsm_data->filter_group_initial_secondary_PMT);
    if(hn_psi_data->fsm_data->filter_group_revision_PAT != NULL)
    {
        retCode = filter_destroyFilterGroup(hn_psi_data->fsm_data->filter_group_revision_PAT);
    }
    if(hn_psi_data->fsm_data->filter_group_revision_primary_PMT)
    {
        retCode = filter_destroyFilterGroup(hn_psi_data->fsm_data->filter_group_revision_primary_PMT);
    }
    if(hn_psi_data->fsm_data->filter_group_revision_secondary_PMT)
    {
        retCode = filter_destroyFilterGroup(hn_psi_data->fsm_data->filter_group_revision_secondary_PMT);
    }
    if(retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
                "<%s::removeHNSessionFromStateMachine> - HN filter groups failed to be destroyed..\n", PSIMODULE);
    }

    retCode = mpeos_memFreeP(MPE_MEM_SI, hn_psi_data);
    if (SITP_SUCCESS != retCode)
    {
        MPE_LOG(MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::removeHNSessionFromStateMachine> - ERROR: Failed to free HN PSI data retCode: %d\n",
                PSIMODULE, retCode);
    }

    //decrement the global HN count
    g_sitp_psi_HN_count -= 1;

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::removeDSGTunnelFromStateMachine> - Exit\n", PSIMODULE);
    return retCode;
}

static uint32_t sitp_psi_getNumActiveHnStreams(void)
{
    return g_sitp_psi_HN_count;
}
