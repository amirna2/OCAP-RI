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

#include <stdlib.h>
#include <string.h>
#include <inttypes.h>
#include <mpe_file.h>
#include "sitp_si.h"
#include "sitp_parse.h"

#include "podmgr.h"
#include "pod_util.h"

/*
 *****************************************************************************
 * static SI related function prototypes.
 *****************************************************************************
 */
static mpe_Error sitp_si_state_wait_table_revision_handler(mpe_Event si_event,
        uint32_t event_data);
static mpe_Error sitp_si_state_wait_table_complete_handler(mpe_Event si_event,
        uint32_t event_data);
static mpe_Error sitp_si_get_table(sitp_si_table_t ** si_table,
        mpe_Event event, uint32_t data);
static void si_Shutdown(void);
static void sitp_siWorkerThread(void * data);
static void sitp_siSTTWorkerThread(void * data);
static mpe_Error createAndSetTableDataArray(
        sitp_si_table_t ** handle_table_array);
static mpe_Error createAndSetSTTDataArray(void);
static mpe_Error get_table_section(mpe_FilterSectionHandle * section_handle,
        uint32_t uniqueID);
static mpe_Error release_table_section(sitp_si_table_t * table_data,
        mpe_FilterSectionHandle section_handle);
static mpe_Error release_filter(sitp_si_table_t * table_data,
        uint32_t unique_id);
static mpe_Error activate_filter(sitp_si_table_t * table_data,
        uint32_t timesToMatch);
static mpe_Error activate_stt_filter(sitp_si_table_t * table_data,
        uint32_t timesToMatch);
static void printFilterSpec(mpe_FilterSpec filterSpec);
static mpe_Bool isTable_acquired(sitp_si_table_t * ptr_table);

static mpe_Bool matchedCRC32(sitp_si_table_t * table_data, uint32_t crc,
        uint32_t * index);
static void set_tablesAcquired(sitp_si_table_t * ptr_table);
static void reset_section_array(sitp_si_revisioning_type_e rev_type);
static mpe_Error findTableIndexByUniqueId(uint32_t * index, uint32_t unique_id);
static void dumpTableData(sitp_si_table_t * table_data);
static mpe_Bool checkForActiveFilters(void);
static mpe_Error
        parseAndUpdateTable(sitp_si_table_t * ptr_table,
                mpe_FilterSectionHandle section_handle, uint8_t * version,
                uint8_t * section_number, uint8_t * last_section_number,
                uint32_t * crc);
static mpe_Bool checkCRCMatchCount(sitp_si_table_t * ptr_table,
        uint32_t section_index);
static void checkForTableAcquisitionTimeouts(void);
static void purgeOldSectionMatches(sitp_si_table_t * table_data, mpe_TimeMillis current_time);

/* debug functions */
/** make these global to prevent compiler warning errors **/
char * sitp_si_tableToString(uint8_t table);
char * sitp_si_eventToString(uint32_t event);
char * sitp_si_stateToString(uint32_t state);

/*
 *******************************************************************************************
 * global variables
 *******************************************************************************************
 */
static mpe_Error
        (*g_si_state_table[])(mpe_Event si_event, uint32_t event_data) =
        {
            sitp_si_state_wait_table_revision_handler,
            sitp_si_state_wait_table_complete_handler
        };

static mpe_ThreadId g_sitp_si_threadID = (mpe_ThreadId) - 1;
static mpe_EventQueue g_sitp_si_queue = 0; // OOB thread event queue
static mpe_TimeMillis g_sitp_sys_time = 0;
static mpe_EventQueue g_sitp_pod_queue = 0; // POD event queue
static int32_t g_sitp_si_status_update_time_interval = 0;
static int32_t g_sitp_pod_status_update_time_interval = 0;
static uint32_t g_sitp_si_dump_tables = 0;
static uint32_t g_sitp_si_update_poll_interval = 1;
static uint32_t g_sitp_si_update_poll_interval_max = 0;
static uint32_t g_sitp_si_update_poll_interval_min = 0;
static uint32_t g_stip_si_process_table_revisions = 0;
static uint32_t g_sitp_si_timeout = 0;
static uint32_t g_sitp_si_profile = PROFILE_CABLELABS_OCAP_1;
static uint32_t g_sitp_si_profile_table_number = 0;
static uint32_t g_sitp_si_versioning_by_crc32 = 0;
static uint32_t g_sitp_si_max_nit_cds_wait_time = 0;
static uint32_t g_sitp_si_max_nit_mms_wait_time = 0;
static uint32_t g_sitp_si_max_svct_dcm_wait_time = 0;
static uint32_t g_sitp_si_max_svct_vcm_wait_time = 0;
static uint32_t g_sitp_si_max_ntt_wait_time = 0;
static uint32_t g_sitp_si_section_retention_time = 0;
static uint32_t g_sitp_si_filter_multiplier = 0;
static uint32_t g_sitp_si_max_nit_section_seen_count = 0;
static uint32_t g_sitp_si_max_vcm_section_seen_count = 0;
static uint32_t g_sitp_si_max_dcm_section_seen_count = 0;
static uint32_t g_sitp_si_max_ntt_section_seen_count = 0;
static uint32_t g_sitp_si_initial_section_match_count = 0;
static uint32_t g_sitp_si_rev_sample_sections = 0;
static sitp_si_table_t * g_table_array = NULL;
static sitp_si_table_t * g_table = NULL;
static int g_shutDown = FALSE; // shutdown flag
static uint32_t g_sitp_si_parse_stt = 0;
static uint32_t g_sitp_si_stt_filter_match_count = 0;
static mpe_TimeMillis g_sitp_stt_time = 0;
static mpe_ThreadId g_sitp_si_stt_threadID = (mpe_ThreadId) - 1;
static mpe_EventQueue g_sitp_si_stt_queue = 0;
static sitp_si_table_t * g_stt_table = NULL;
static uint32_t g_sitp_stt_timeout = 0;
static uint32_t g_sitp_stt_thread_sleep_millis = 20;

/*
 *****************************************************************************
 * Flags to indicate acquisition of CDS, MMS and SVCT.
 * When we have these table signal that SIDB can be accessed.
 *****************************************************************************
 */
static uint32_t g_sitp_svctAcquired = 0;
static uint32_t g_sitp_siTablesAcquired = 0;


/*  Control variables for the si caching*/
static mpe_Bool g_siOOBCacheEnable = FALSE;
static const char  *g_siOOBCacheLocation = NULL;
static const char  *g_siOOBSNSCacheLocation = NULL;

static char g_tempSICacheLocation[250] = "";
static char g_tempSISNSCacheLocation[250] = "";
/*
 * **************************************************************************************************
 * Subroutine/Method:                       sitp_si_Start()
 * ***************************************************************************************************
 *
 * This subroutine activates the SI Table Parsing System/Subsystem . Activation
 * is accomplished by starting the SI (Service Information) Out-Of-Band Table Acquistion Thread
 * and by starting the Inband Table Acquisition Thread.
 *
 * @param None,
 *
 * @return MPE_SUCCESS or other error codes.
 */
mpe_Error sitp_si_Start(void)
{
    mpe_Error retCode = MPE_SUCCESS;
    const char *sitp_si_max_nit_cds_wait_time = NULL;
    const char *sitp_si_max_nit_mms_wait_time = NULL;
    const char *sitp_si_max_svct_dcm_wait_time = NULL;
    const char *sitp_si_max_svct_vcm_wait_time = NULL;
    const char *sitp_si_max_ntt_wait_time = NULL;
    const char *sitp_si_section_retention_time = NULL;
    const char *sitp_si_enabled = NULL;
    const char *sitp_si_update_poll_interval_max = NULL;
    const char *sitp_si_status_update_time_interval = NULL;
    const char *sitp_si_update_poll_interval_min = NULL;
    const char *sitp_si_dump_tables = NULL;
    const char *sitp_si_versioning_by_crc = NULL;
    const char *sitp_si_filter_multiplier = NULL;
    const char *sitp_si_max_nit_section_seen_count = NULL;
    const char *sitp_si_max_vcm_section_seen_count = NULL;
    const char *sitp_si_max_dcm_section_seen_count = NULL;
    const char *sitp_si_max_ntt_section_seen_count = NULL;
    const char *sitp_si_initial_section_match_count = NULL;
    const char *sitp_si_rev_sample_sections = NULL;
    const char *sitp_si_process_table_revisions = NULL;
    const char *sitp_si_parse_stt = NULL;
    const char *sitp_si_stt_filter_match_count = NULL;
    const char *siOOBCacheEnabled = NULL;
    /*
     * Get all of the config variables from the ini.
     */
    sitp_si_parse_stt = mpeos_envGet("SITP.SI.STT.ENABLED");
    if ((sitp_si_parse_stt != NULL)
            && (stricmp(sitp_si_parse_stt, "TRUE") == 0))
    {
        g_sitp_si_parse_stt = TRUE;
    }
    else
    {
        g_sitp_si_parse_stt = FALSE;
    }
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::sitp_si_Start> - Parse STT is %s\n", SIMODULE,
            (g_sitp_si_parse_stt ? "ON" : "OFF"));

    sitp_si_stt_filter_match_count = mpeos_envGet(
            "SITP.SI.STT.FILTER.MATCH.COUNT");
    if (sitp_si_stt_filter_match_count != NULL)
    {
        g_sitp_si_stt_filter_match_count = atoi(sitp_si_stt_filter_match_count);
    }
    else
    {
        // Default match count is 1
        g_sitp_si_stt_filter_match_count = 1;
    }
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::sitp_si_Start> - STT filter match count is %d\n", SIMODULE,
            g_sitp_si_stt_filter_match_count);

    if (g_sitp_si_parse_stt == TRUE)
    {
        // First create STT thread
        retCode = mpeos_threadCreate(sitp_siSTTWorkerThread, NULL,
                MPE_THREAD_PRIOR_SYSTEM, MPE_THREAD_STACK_SIZE,
                &g_sitp_si_stt_threadID, "mpeSitpSiSTTWorkerThread");
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_SI,
                    "<%s::sitp_si_Start> - failed to create sitp_siSTTWorkerThread, error: %d\n",
                    SIMODULE, retCode);

            return retCode;
        }
    }

    sitp_si_status_update_time_interval = mpeos_envGet(
            "SITP.SI.STATUS.UPDATE.TIME.INTERVAL");
    if (sitp_si_status_update_time_interval != NULL)
    {
        g_sitp_si_status_update_time_interval = atoi(
                sitp_si_status_update_time_interval);
    }
    else
    {
        g_sitp_si_status_update_time_interval = 15000;
    }
    MPE_LOG(
            MPE_LOG_TRACE4,
            MPE_MOD_SI,
            "<%s::sitp_si_Start> - SI status update time interval set to %d ms\n",
            SIMODULE, g_sitp_si_status_update_time_interval);

    sitp_si_update_poll_interval_max = mpeos_envGet(
            "SITP.SI.MAX.UPDATE.POLL.INTERVAL");
    if (sitp_si_update_poll_interval_max != NULL)
    {
        g_sitp_si_update_poll_interval_max = atoi(
                sitp_si_update_poll_interval_max);
    }
    else
    {
        g_sitp_si_update_poll_interval_max = 30000;
    }
    MPE_LOG(
            MPE_LOG_TRACE4,
            MPE_MOD_SI,
            "<%s::sitp_si_Start> - SI update max polling interval set to %d ms\n",
            SIMODULE, g_sitp_si_update_poll_interval_max);

    sitp_si_update_poll_interval_min = mpeos_envGet(
            "SITP.SI.MIN.UPDATE.POLL.INTERVAL");
    if (sitp_si_update_poll_interval_min != NULL)
    {
        g_sitp_si_update_poll_interval_min = atoi(
                sitp_si_update_poll_interval_min);
    }
    else
    {
        g_sitp_si_update_poll_interval_min = 25000;
    }
    MPE_LOG(
            MPE_LOG_TRACE4,
            MPE_MOD_SI,
            "<%s::sitp_si_Start> - SI update min polling interval set to %d ms\n",
            SIMODULE, g_sitp_si_update_poll_interval_min);

    if (g_sitp_si_update_poll_interval_min > g_sitp_si_update_poll_interval_max)
    {
        MPE_LOG(
                MPE_LOG_TRACE4,
                MPE_MOD_SI,
                "<%s::sitp_si_Start> - min polling interval greater than max polling interval - using default values\n",
                SIMODULE);
        g_sitp_si_update_poll_interval_min = 25000;
        g_sitp_si_update_poll_interval_max = 30000;
    }

    /*
     ** ***********************************************************************************************
     **  Acquire NIT/SVCT acquisition policy from the environment.
     **  If it is not defined, the default value is for SVCT/NIT acquisition
     **  to be continuous.
     ** ************************************************************************************************
     */
    sitp_si_process_table_revisions = mpeos_envGet(
            "SITP.SI.PROCESS.TABLE.REVISIONS");
    if ((NULL == sitp_si_process_table_revisions) || (stricmp(
            sitp_si_process_table_revisions, "TRUE") == 0))
    {
        g_stip_si_process_table_revisions = 1;
    }
    else
    {
        g_stip_si_process_table_revisions = 0;
    }
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::sitp_si_Start> - table revision processing is %s\n",
            SIMODULE, (g_stip_si_process_table_revisions ? "ON" : "OFF"));

    sitp_si_rev_sample_sections = mpeos_envGet("SITP.SI.REV.SAMPLE.SECTIONS");
    if ((sitp_si_rev_sample_sections != NULL) && (stricmp(
            sitp_si_rev_sample_sections, "TRUE") == 0))
    {
        g_sitp_si_rev_sample_sections = 1;
    }
    else
    {
        g_sitp_si_rev_sample_sections = 0;
    }
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::sitp_si_Start> - sample for table revisions %s\n", SIMODULE,
            (g_sitp_si_rev_sample_sections ? "ON" : "OFF"));

    sitp_si_dump_tables = mpeos_envGet("SITP.SI.DUMP.CHANNEL.TABLES");
    if ((sitp_si_dump_tables != NULL) && (stricmp(sitp_si_dump_tables, "TRUE")
            == 0))
    {
        g_sitp_si_dump_tables = 1;
    }
    else
    {
        g_sitp_si_dump_tables = 0;
    }
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::sitp_si_Start> - SI channel map dumping is %s\n", SIMODULE,
            (g_sitp_si_dump_tables ? "ON" : "OFF"));

    sitp_si_versioning_by_crc = mpeos_envGet("SITP.SI.VERSION.BY.CRC");
    if ((sitp_si_versioning_by_crc == NULL) || (stricmp(
            sitp_si_versioning_by_crc, "FALSE") == 0))
    {
        g_sitp_si_versioning_by_crc32 = 0;
    }
    else
    {
        g_sitp_si_versioning_by_crc32 = 1;
    }
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::sitp_si_Start> - SI revisioning by crc is %s\n", SIMODULE,
            (g_sitp_si_versioning_by_crc32 ? "ON" : "OFF"));

    sitp_si_filter_multiplier = mpeos_envGet("SITP.SI.FILTER.MULTIPLIER");
    if (sitp_si_filter_multiplier != NULL)
    {
        if (0 == atoi(sitp_si_filter_multiplier))
            g_sitp_si_filter_multiplier = 0;
        g_sitp_si_filter_multiplier = atoi(sitp_si_filter_multiplier);
    }
    else
    {
        g_sitp_si_filter_multiplier = 2;
    }
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::sitp_si_Start> - SI filter multiplier: %d\n", SIMODULE,
            g_sitp_si_filter_multiplier);

    sitp_si_max_nit_section_seen_count = mpeos_envGet(
            "SITP.SI.MAX.NIT.SECTION.SEEN.COUNT");
    if (sitp_si_max_nit_section_seen_count != NULL)
    {
        /*
         * Set this value to at least 2 b/c, the sections need minimal
         * verification before the table can be marked as found
         */
        if (atoi(sitp_si_max_nit_section_seen_count) < 2)
            g_sitp_si_max_nit_section_seen_count = 2;
        else
            g_sitp_si_max_nit_section_seen_count = atoi(
                    sitp_si_max_nit_section_seen_count);
    }
    else
    {
        g_sitp_si_max_nit_section_seen_count = 2;
    }
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::sitp_si_Start> - NIT section seen count: %d\n", SIMODULE,
            g_sitp_si_max_nit_section_seen_count);

    sitp_si_max_vcm_section_seen_count = mpeos_envGet(
            "SITP.SI.MAX.VCM.SECTION.SEEN.COUNT");
    if (sitp_si_max_vcm_section_seen_count != NULL)
    {
        /*
         * Set this value to at least 2 b/c, the sections need minimal
         * verification before the table can be marked as found
         */
        if (atoi(sitp_si_max_vcm_section_seen_count) < 2)
            g_sitp_si_max_vcm_section_seen_count = 2;
        else
            g_sitp_si_max_vcm_section_seen_count = atoi(
                    sitp_si_max_vcm_section_seen_count);
    }
    else
    {
        g_sitp_si_max_vcm_section_seen_count = 4;
    }
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::sitp_si_Start> - SVCT-VCM section seen count: %d\n",
            SIMODULE, g_sitp_si_max_vcm_section_seen_count);

    sitp_si_max_dcm_section_seen_count = mpeos_envGet(
            "SITP.SI.MAX.DCM.SECTION.SEEN.COUNT");
    if (sitp_si_max_dcm_section_seen_count != NULL)
    {
        g_sitp_si_max_dcm_section_seen_count = atoi(
                sitp_si_max_dcm_section_seen_count);
    }
    else
    {
        g_sitp_si_max_dcm_section_seen_count = 2;
    }
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::sitp_si_Start> - SVCT-DCM section seen count: %d\n",
            SIMODULE, g_sitp_si_max_dcm_section_seen_count);

    sitp_si_max_ntt_section_seen_count = mpeos_envGet(
            "SITP.SI.MAX.NTT.SECTION.SEEN.COUNT");
    if (sitp_si_max_ntt_section_seen_count != NULL)
    {
        /*
         * Set this value to at least 2 b/c, the sections need minimal
         * verification before the table can be marked as found
         */
        if (atoi(sitp_si_max_ntt_section_seen_count) < 2)
            g_sitp_si_max_ntt_section_seen_count = 2;
        else
            g_sitp_si_max_ntt_section_seen_count = atoi(
                    sitp_si_max_ntt_section_seen_count);
    }
    else
    {
        g_sitp_si_max_ntt_section_seen_count = 3;
    }
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::sitp_si_Start> - SI NTT section seen count: %d\n", SIMODULE,
            g_sitp_si_max_ntt_section_seen_count);

    sitp_si_initial_section_match_count = mpeos_envGet(
            "SITP.SI.INITIAL.SECTION.MATCH.COUNT");
    if (sitp_si_initial_section_match_count != NULL)
    {
        g_sitp_si_initial_section_match_count = atoi(
                sitp_si_initial_section_match_count);
    }
    else
    {
        g_sitp_si_initial_section_match_count = 50;
    }
    MPE_LOG(
            MPE_LOG_TRACE4,
            MPE_MOD_SI,
            "<%s::sitp_si_Start> - Initial acquisition section match count: %d\n",
            SIMODULE, g_sitp_si_initial_section_match_count);

    sitp_si_max_nit_cds_wait_time = mpeos_envGet(
            "SITP.SI.MAX.NIT.CDS.WAIT.TIME");
    if (sitp_si_max_nit_cds_wait_time != NULL)
    {
        g_sitp_si_max_nit_cds_wait_time = atoi(sitp_si_max_nit_cds_wait_time);
    }
    else
    {
        // default wait time for NIT CDS - 1 minute
        g_sitp_si_max_nit_cds_wait_time = 60000;
    }
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::sitp_si_Start> - Max NIT CDS wait time: %d\n", SIMODULE,
            g_sitp_si_max_nit_cds_wait_time);

    sitp_si_max_nit_mms_wait_time = mpeos_envGet(
            "SITP.SI.MAX.NIT.MMS.WAIT.TIME");
    if (sitp_si_max_nit_mms_wait_time != NULL)
    {
        g_sitp_si_max_nit_mms_wait_time = atoi(sitp_si_max_nit_mms_wait_time);
    }
    else
    {
        // default wait time for NIT MMS - 1 minute
        g_sitp_si_max_nit_mms_wait_time = 60000;
    }
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::sitp_si_Start> - Max NIT MMS wait time: %d\n", SIMODULE,
            g_sitp_si_max_nit_mms_wait_time);

    sitp_si_max_svct_dcm_wait_time = mpeos_envGet(
            "SITP.SI.MAX.SVCT.DCM.WAIT.TIME");
    if (sitp_si_max_svct_dcm_wait_time != NULL)
    {
        g_sitp_si_max_svct_dcm_wait_time = atoi(sitp_si_max_svct_dcm_wait_time);
    }
    else
    {
        // default wait time for DCM - 2 and a half minutes
        g_sitp_si_max_svct_dcm_wait_time = 150000;
    }
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::sitp_si_Start> - Max SVCT DCM wait time: %d\n", SIMODULE,
            g_sitp_si_max_svct_dcm_wait_time);

    sitp_si_max_svct_vcm_wait_time = mpeos_envGet(
            "SITP.SI.MAX.SVCT.VCM.WAIT.TIME");
    if (sitp_si_max_svct_vcm_wait_time != NULL)
    {
        g_sitp_si_max_svct_vcm_wait_time = atoi(sitp_si_max_svct_vcm_wait_time);
    }
    else
    {
        // default wait time for VCM - 4 minutes
        g_sitp_si_max_svct_vcm_wait_time = 240000;
    }
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::sitp_si_Start> - Max SVCT VCM wait time: %d\n", SIMODULE,
            g_sitp_si_max_svct_vcm_wait_time);

    sitp_si_max_ntt_wait_time = mpeos_envGet("SITP.SI.MAX.NTT.WAIT.TIME");
    if (sitp_si_max_ntt_wait_time != NULL)
    {
        g_sitp_si_max_ntt_wait_time = atoi(sitp_si_max_ntt_wait_time);
    }
    else
    {
        // default wait time 2 and a half minutes
        g_sitp_si_max_ntt_wait_time = 150000;
    }
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::sitp_si_Start> - Max NTT wait time: %d\n", SIMODULE,
            g_sitp_si_max_ntt_wait_time);

    sitp_si_section_retention_time = mpeos_envGet("SITP.SI.SECTION.RETENTION.TIME");
    if (sitp_si_section_retention_time != NULL)
    {
        g_sitp_si_section_retention_time = atoi(sitp_si_section_retention_time);
    }
    else
    {
        // default section retention threshold is 2 and a half minutes
        g_sitp_si_section_retention_time = 150000;
    }
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::sitp_si_Start> - Section retention threshold: %dms\n", SIMODULE,
            g_sitp_si_section_retention_time);

    // Initialize the CRC calculator
    init_mpeg2_crc();

    //************************************************************************************************
    // Launch the OUT_OF-BAND (OOB)thread which will periodically acquire the 2 NIT subtables
    // (CDS, MMS) and SVCT (VCM, DCM subtables) and NTT-SNS table.
    //************************************************************************************************

    sitp_si_enabled = mpeos_envGet("SITP.SI.ENABLED");
    if ((sitp_si_enabled == NULL) || (stricmp(sitp_si_enabled, "TRUE") == 0))
    {
        retCode = mpeos_threadCreate(sitp_siWorkerThread, NULL,
                MPE_THREAD_PRIOR_SYSTEM, MPE_THREAD_STACK_SIZE,
                &g_sitp_si_threadID, "mpeSitpSiWorkerThread");
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_SI,
                    "<%s::sitp_si_Start> - failed to create sitp_siWorkerThread, error: %d\n",
                    SIMODULE, retCode);

            return retCode;
        }
        else
        {
            MPE_LOG(
                    MPE_LOG_TRACE4,
                    MPE_MOD_SI,
                    "<%s::sitp_si_Start> - sitp_siWorkerThread thread started successfully\n",
                    SIMODULE);
        }
    }
    else
    {
        MPE_LOG(
                MPE_LOG_TRACE4,
                MPE_MOD_SI,
                "<%s::sitp_si_Start> - sitp_siWorkerThread thread disabled by SITP.SI.ENABLED\n",
                SIMODULE);

        // The OOB thread is disabled so we will not be getting a SVCT.
        // Signal that it's ok to access the SI database.
        MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
                "<%s::sitp_si_Start> - Calling mpe_siSetGlobalState.\n",
                SIMODULE);

        mpe_siSetGlobalState ( SI_DISABLED);
    }

    // MPE level SI caching is enabled/disabled via ini variable setting (SITP.SI.CACHE.ENABLED)
    // Cache setting is independent of whether OOB SI is enabled or disabled.
    {
        /*
           This has been added to determine whether or not to cache the g_si_entry on persistent
           storage. If cached, the next boot will use the cache to populate g_si_entry immediately.
           Later acquired SI data can be used to update SI DB entries.
         */
        siOOBCacheEnabled = mpeos_envGet("SITP.SI.CACHE.ENABLED");
        if ( (siOOBCacheEnabled == NULL) || (stricmp(siOOBCacheEnabled,"FALSE") == 0))
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,"OOB Caching is disabled by SITP.SI.CACHE.ENABLED\n");
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,"OOB Caching is enabled by SITP.SI.CACHE.ENABLED\n");
            g_siOOBCacheEnable = TRUE;
        }
        if (g_siOOBCacheEnable)
        {
            g_siOOBCacheLocation = mpeos_envGet("SITP.SI.CACHE.LOCATION");
            if ( (g_siOOBCacheLocation == NULL))
            {
                g_siOOBCacheLocation = "/syscwd/persistent/si/si_table_cache.bin";
            }
            g_siOOBSNSCacheLocation = mpeos_envGet("SITP.SNS.CACHE.LOCATION");
            if ( (g_siOOBSNSCacheLocation == NULL))
            {
                g_siOOBSNSCacheLocation = "/syscwd/persistent/si/sns_table_cache.bin";
            }

            memset (g_tempSISNSCacheLocation, '\0', sizeof(g_tempSISNSCacheLocation));
            memset (g_tempSICacheLocation, '\0', sizeof(g_tempSICacheLocation));
            strcpy (g_tempSISNSCacheLocation, g_siOOBSNSCacheLocation);
            strcat (g_tempSISNSCacheLocation, ".tmp");
            strcpy (g_tempSICacheLocation, g_siOOBCacheLocation);
            strcat (g_tempSICacheLocation, ".tmp");

            MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,"OOB SI Caching Location is [%s]\n", g_siOOBCacheLocation);
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,"OOB SNS Caching Location is [%s]\n", g_siOOBSNSCacheLocation);
        }
    }

    // If caching is enabled load the cache first
    if (g_siOOBCacheEnable)
    {
        mpe_siLockForWrite();
        // Check the CRC of SI cache files
        if ((MPE_SUCCESS == verify_si_cache_files_exist(g_siOOBCacheLocation, g_siOOBSNSCacheLocation))
              &&  verify_version_and_crc(g_siOOBCacheLocation, g_siOOBSNSCacheLocation) == 0)
        {
            // CRC verification failed!!
            /* Rename the file as .tmp for later verification */
            mpe_fileRename (g_siOOBCacheLocation, g_tempSICacheLocation);
            mpe_fileRename (g_siOOBSNSCacheLocation, g_tempSISNSCacheLocation);

            /* The original file shouldn't be there but still, just in case, we delete the original file */
            mpe_fileDelete(g_siOOBCacheLocation);
            mpe_fileDelete(g_siOOBSNSCacheLocation);

            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "Cache files are moved as .tmp files and start acquiring fresh SI...\n");
        }
        else
        {
            // CRC validation succeeded, continue loading from cache
            if (load_sns_entries(g_siOOBSNSCacheLocation) == MPE_SUCCESS)
            {
                if (load_si_entries(g_siOOBCacheLocation) == MPE_SUCCESS)
                {
                    mpe_siSetGlobalState (SI_NIT_SVCT_ACQUIRED);
                    mpe_siSetGlobalState (SI_FULLY_ACQUIRED);
                    /* Let SITP module know that caching is on */
                    sitp_si_cache_enabled();
                }
            }
        }
        mpe_siReleaseWriteLock();
    }

    /*if ((sitp_si_enabled == NULL) || (atoi(sitp_si_enabled) != 0))
     {
     MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,"<%s::sitp_si_Start> - sitp_siWorkerThread thread set to be started?\n",
     SIMODULE);
     }
     else
     {
     MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,"<%s::sitp_si_Start> - sitp_siWorkerThread thread disabled by SITP.SI.ENABLED\n",
     SIMODULE);

     // The OOB thread is disabled so we will not be getting a SVCT.
     // Signal that it's ok to access the SI database.
     MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,"<%s::sitp_si_Start> - Calling mpe_siSetGlobalState.\n",
     SIMODULE);

     mpe_siSetGlobalState (SI_NOT_AVAILABLE_YET);
     }
     */
    return retCode;
}

/**
 * Tells the SI parser that the data is coming from a local
 * cache, not an actual headend
 *
 */
void sitp_si_cache_enabled(void)
{
    g_sitp_si_max_nit_cds_wait_time = ((g_sitp_si_max_nit_cds_wait_time > 60000) ?  g_sitp_si_max_svct_dcm_wait_time : 60000);
    g_sitp_si_max_nit_mms_wait_time = ((g_sitp_si_max_nit_mms_wait_time > 60000) ?  g_sitp_si_max_svct_vcm_wait_time : 60000);
    g_sitp_si_max_svct_dcm_wait_time = ((g_sitp_si_max_svct_dcm_wait_time > 1800000) ?  g_sitp_si_max_svct_dcm_wait_time : 1800000);
    g_sitp_si_max_svct_vcm_wait_time = ((g_sitp_si_max_svct_vcm_wait_time > 1800000) ?  g_sitp_si_max_svct_vcm_wait_time : 1800000);
    g_sitp_si_max_ntt_wait_time  = ((g_sitp_si_max_ntt_wait_time  > 1800000) ?  g_sitp_si_max_ntt_wait_time  : 1800000);
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI, "CACHE: Set SI Wait time to [dcm=%d][vcm=%d][ntt=%d]\r\n", g_sitp_si_max_svct_dcm_wait_time, g_sitp_si_max_svct_vcm_wait_time,g_sitp_si_max_ntt_wait_time);
}
//***************************************************************************************************
//  Subroutine/Method:                       sitpShutDown
//***************************************************************************************************
/**
 * This subroutine deactivates the SI Table Parsing System/Subsystem.
 *
 * @param None,
 *
 * @return MPE_SUCCESS or other error codes.
 */

void sitp_si_ShutDown(void)
{
    g_shutDown = TRUE; // signal the shutdown via the shutdown flag
    mpeos_eventQueueSend(g_sitp_si_stt_queue, MPE_ETHREADDEATH, NULL, NULL, 0); // send the MPE_ETHREADDEATH event

    mpeos_eventQueueSend(g_sitp_si_queue, MPE_ETHREADDEATH, NULL, NULL, 0); // send the MPE_ETHREADDEATH event

    podImplUnregisterMPELevelQueueForPodEvents(g_sitp_pod_queue);
    mpeos_eventQueueSend(g_sitp_pod_queue, MPE_ETHREADDEATH, NULL, NULL, 0); // send the MPE_ETHREADDEATH event
    MPE_LOG(
            MPE_LOG_TRACE4,
            MPE_MOD_SI,
            "<%s::sitpShutDown> - SI shutdown message/invocation received and successfully processed.\n",
            SIMODULE);
}

//***************************************************************************************************
//  Subroutine/Method:                       si_Shutdown
//***************************************************************************************************
/**
 *
 * @param None,
 *
 * @return MPE_SUCCESS or other error codes.
 */

static void si_Shutdown(void)
{
    /*
     * delete the table data array and release all of the memory.
     */
    mpe_Error retCode = mpeos_memFreeP(MPE_MEM_SI, g_table_array);
    if (SITP_SUCCESS != retCode)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::si_Shutdown> - Error - Failed to Free SI table array: %d\n",
                SIMODULE, retCode);
    }

    mpe_eventQueueDelete(g_sitp_si_queue); // delete the OOB global queue

    if (g_sitp_pod_queue)
    {
        podImplUnregisterMPELevelQueueForPodEvents(g_sitp_pod_queue);
        mpe_eventQueueDelete(g_sitp_pod_queue);
        g_sitp_pod_queue = 0;
    }
}

mpe_TimeMillis getSTTStartTime(void)
{
    return g_sitp_stt_time;
}

//***************************************************************************************************
//  Thread:                                     sitp_siSTTWorkerThread ()
//***************************************************************************************************
/**
 *  sitp_siSTTWorkerThread entry point (This thread is dedicated to setting the System
 *  Time Table filter and parsing it)
 *
 * @param None,
 *
 * @return MPE_SUCCESS or other error codes.
 */
static void sitp_siSTTWorkerThread(void* data)
{
    mpe_Error retCode = MPE_SUCCESS;
    mpe_Event si_event = 0;
    void *poptional_eventdata = NULL;

    mpe_FilterSectionHandle section_handle = 0;
    uint8_t current_section_number = 0;
    uint8_t last_section_number = 0;
    uint32_t unique_id = 0;
    uint32_t crc2 = 0;
    static uint32_t stt_section_count = 0;

    MPE_UNUSED_PARAM(data);

    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::sitp_siSTTWorkerThread> - Enter.\n", SIMODULE);

    //***********************************************************************************************
    //***********************************************************************************************

    //***********************************************************************************************
    //**** Create a global event queue for the Thread
    //***********************************************************************************************
    if ((retCode = mpeos_eventQueueNew(&g_sitp_si_stt_queue, "MpeSitpSiSTT"))
            != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::sitp_siSTTWorkerThread> - unable to create event queue,... terminating thread.\n",
                SIMODULE);

        return;
    }
    /*
     ** *************************************************************************
     ** **** Create and set the STT FSM structure.
     ** *************************************************************************
     */
    if (SITP_SUCCESS != createAndSetSTTDataArray())
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::sitp_siSTTWorkerThread> - ERROR: Unable to create table data array, ...terminating thread.\n",
                SIMODULE);
        return;
    }

    // Activate STT filter
    retCode
            = activate_stt_filter(g_stt_table, g_sitp_si_stt_filter_match_count);

    if (SITP_SUCCESS != retCode)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::sitp_siSTTWorkerThread> - failed to set a filter for table: %d(0x%x)\n",
                SIMODULE, g_stt_table->table_id, g_stt_table->table_id);
    }
    else
    {
        MPE_LOG(
                MPE_LOG_TRACE5,
                MPE_MOD_SI,
                "<%s::sitp_siSTTWorkerThread> - successfully set a filter for table: %s(0x%x), subtype: %d, unique_id: %d\n",
                SIMODULE, sitp_si_tableToString(g_stt_table->table_id),
                g_stt_table->table_id, g_stt_table->subtype,
                g_stt_table->table_unique_id);
    }

    while (FALSE == g_shutDown)
    {
        retCode = mpeos_eventQueueWaitNext(g_sitp_si_stt_queue, &si_event,
                &poptional_eventdata, NULL, NULL, g_sitp_stt_timeout);

        MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
                "<%s::sitp_siSTTWorkerThread> - Received EVENT: %s: %d\n",
                SIMODULE, sitp_si_eventToString(si_event), si_event);

        if (MPE_ETIMEOUT == retCode)
        {
            si_event = MPE_ETIMEOUT;
        }

        unique_id = (uint32_t) poptional_eventdata;

        switch (si_event)
        {
        case MPE_SF_EVENT_SECTION_FOUND:
        case MPE_SF_EVENT_LAST_SECTION_FOUND:
        {
            // Capture the STT time
            mpeos_timeGetMillis(&g_sitp_stt_time);

            stt_section_count++;
            MPE_LOG(MPE_LOG_TRACE5, MPE_MOD_SI,
                    "<%s::sitp_siSTTWorkerThread> - stt_section_count = %d\n",
                    SIMODULE, stt_section_count);
            if (si_event == MPE_SF_EVENT_LAST_SECTION_FOUND)
            {
                // reset count if MPE_SF_EVENT_LAST_SECTION_FOUND
                stt_section_count = 0;
            }

            retCode = get_table_section(&section_handle, unique_id);

            if (SITP_SUCCESS != retCode)
            {
                MPE_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_SI,
                        "<%s::sitp_siSTTWorkerThread> - failed to get the section handle: 0x%x, unique_id: %d\n",
                        SIMODULE, g_stt_table->table_id, unique_id);

                release_filter(g_stt_table, unique_id); // table, unquie_id
                g_sitp_stt_timeout = 0;
                break;
            }

            if (si_event == MPE_SF_EVENT_LAST_SECTION_FOUND)
            {
                release_filter(g_stt_table, unique_id); // table, unquie_id
            }

            if (SITP_SUCCESS != parseAndUpdateTable(g_stt_table,
                    section_handle, &g_stt_table->version_number,
                    &current_section_number, &last_section_number, &crc2))
            {
                release_table_section(g_stt_table, section_handle);
                g_sitp_stt_timeout = 0;
                break;
            }

            g_stt_table->section[current_section_number].section_acquired
                    = true;
            release_table_section(g_stt_table, section_handle);

            if (si_event == MPE_SF_EVENT_LAST_SECTION_FOUND)
            {
                // Sleep for g_sitp_stt_thread_sleep_millis (20 msec) to prevent thrashing
                mpeos_threadSleep(g_sitp_stt_thread_sleep_millis, 0);

                // Re-activate filter
                retCode = activate_stt_filter(g_stt_table,
                        g_sitp_si_stt_filter_match_count);

                if (SITP_SUCCESS != retCode)
                {
                    MPE_LOG(
                            MPE_LOG_ERROR,
                            MPE_MOD_SI,
                            "<%s::sitp_siSTTWorkerThread> - failed to set a filter for table: %d(0x%x)\n",
                            SIMODULE, g_stt_table->table_id,
                            g_stt_table->table_id);
                }
                else
                {
                    MPE_LOG(
                            MPE_LOG_TRACE5,
                            MPE_MOD_SI,
                            "<%s::sitp_siSTTWorkerThread> - successfully set a filter for table: %s(0x%x), subtype: %d, unique_id: %d\n",
                            SIMODULE, sitp_si_tableToString(
                                    g_stt_table->table_id),
                            g_stt_table->table_id, g_stt_table->subtype,
                            g_stt_table->table_unique_id);
                }
            }
        }
            break;

        default:
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_SI,
                    "<%s::sitp_siSTTWorkerThread> - event not handled in this state: %s\n",
                    SIMODULE, sitp_si_eventToString(si_event));
            retCode = SITP_FAILURE;
            break;
        }
        }

        MPE_LOG(MPE_LOG_TRACE5, MPE_MOD_SI,
                "<%s::sitp_siSTTWorkerThread> - \n\n\n", SIMODULE);
    } // End while( FALSE == g_shutDown )
}

//***************************************************************************************************
//  Thread:                                     sitp_siWorkerThread ()
//***************************************************************************************************
/**
 *  sitp_siWorkerThread entry point
 *
 * @param None,
 *
 * @return MPE_SUCCESS or other error codes.
 */
static void sitp_siWorkerThread(void* data)
{
    mpe_Error retCode = MPE_SUCCESS;
    mpe_Event si_event = 0;
    mpe_Event pod_event = 0;
    uint32_t event_data = 0;
    void *poptional_eventdata = NULL;
    mpe_Bool podReady = false;

    MPE_UNUSED_PARAM(data);

    //***********************************************************************************************
    //***********************************************************************************************

    /*
     * Seed for the random poll interval time.
     */
    srand((unsigned) time(NULL));

    /*
     *  Get the status timer started.
     */
    mpeos_timeGetMillis(&g_sitp_sys_time);
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,"<%s::sitp_siWorkerThread> - Start time = %"PRIu64"\n", SIMODULE, g_sitp_sys_time);

    //***********************************************************************************************
    //**** Create a global event queue for the Thread
    //***********************************************************************************************
    if ((retCode = mpeos_eventQueueNew(&g_sitp_si_queue, "MpeSitpSi"))
            != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::sitp_siWorkerThread> - unable to create event queue,... terminating thread.\n",
                SIMODULE);

        return;
    }

    /*
     ** *************************************************************************
     ** **** Create and set the FSM structure.
     ** *************************************************************************
     */
    if (SITP_SUCCESS != createAndSetTableDataArray(&g_table_array))
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::sitp_siWorkerThread> - ERROR: Unable to create table data array, ...terminating thread.\n",
                SIMODULE);
        return;
    }
    /*
     ** *************************************************************************
     ** **** Set the current tuner to the OOB tuner( tuner index 0 ) at startup,
     ** **** the global index(0), the initial event(OOB_SI_ACQUISITION) to move the
     ** **** OOB tuner state to start acquiring the OOB PAT and PMT.
     ** *************************************************************************
     */
    si_event = OOB_START_UP_EVENT;
    pod_event = MPE_POD_EVENT_NO_EVENT;
    g_sitp_si_timeout = g_sitp_si_status_update_time_interval; // set default timeout period to wait forever
    g_table = &g_table_array[0];

    /*
     * Do not start SI acquisition until the POD is considered READY
     */
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::sitp_siWorkerThread> - checking mpe_podmgrIsReady\n",
            SIMODULE);

    if ((retCode = mpeos_eventQueueNew(&g_sitp_pod_queue, "MpeSitpPOD"))
            != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::sitp_siWorkerThread> - unable to create POD event queue,... terminating thread.\n",
                SIMODULE);

        return;
    }

    podImplRegisterMPELevelQueueForPodEvents(g_sitp_pod_queue);

    if (MPE_SUCCESS == mpe_podmgrIsReady())
    {
        MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
                "<%s::sitp_siWorkerThread> - mpe_podmgrIsReady == POD READY\n",
                SIMODULE);
        podReady = TRUE;
    }
    else
    {
        /* Wait until the POD is ready.
         *
         * There are a lot of pod events that are not ready events.  Ignore them and wait for ready event
         */
        podReady = FALSE;

        while (!podReady)
        {
            MPE_LOG(
                    MPE_LOG_INFO,
                    MPE_MOD_SI,
                    "<%s::sitp_siWorkerThread> - mpe_podmgrIsReady != POD READY. Waiting for MPE_POD_EVENT_READY...\n",
                    SIMODULE);

            mpeos_eventQueueWaitNext(g_sitp_pod_queue, &pod_event,
                    &poptional_eventdata, NULL, NULL,
                    g_sitp_pod_status_update_time_interval);

            MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
                    "<%s::sitp_siWorkerThread> - recv'd podEvent = %d\n",
                    SIMODULE, pod_event);

            if (pod_event == MPE_POD_EVENT_READY)
            {
                podReady = TRUE;
            }
        }
        MPE_LOG(
                MPE_LOG_INFO,
                MPE_MOD_SI,
                "<%s::sitp_siWorkerThread> - Received MPE_POD_EVENT_READY. Continuing...\n",
                SIMODULE);
        podImplUnregisterMPELevelQueueForPodEvents(g_sitp_pod_queue);
        mpe_eventQueueDelete(g_sitp_pod_queue);
        g_sitp_pod_queue = 0;
    }

    // Register the SI queue to receive POD events
    podImplRegisterMPELevelQueueForPodEvents(g_sitp_si_queue);
    //MPE_LOG(
    //        MPE_LOG_INFO,
    //        MPE_MOD_SI,
    //        "<%s::sitp_siWorkerThread> - Sleeping 30 sec...\n",
    //        SIMODULE);
    //mpeos_threadSleep(30000, 0);

    //***********************************************************************************************
    //*** Loop indefinitely until function/method  sipShutdown is invoked by a higher authority.
    //*** The invocation will set g_shutDown to "TRUE" and place a "MPE_ETHREADDEATH" event in the
    //*** thread's event queue.
    //***********************************************************************************************
    while (FALSE == g_shutDown)
    {
        event_data = (uint32_t) poptional_eventdata;

        /*
         ** ***********************************************************************************************
         ** Get the table associated to the event.
         ** ***********************************************************************************************
         */
        retCode = sitp_si_get_table(&g_table, si_event, event_data);
        MPE_LOG(
                MPE_LOG_TRACE4,
                MPE_MOD_SI,
                "<%s::sitp_siWorkerThread> - Received EVENT: %s while in state: %s table_id: %s, subtype: %d.\n",
                SIMODULE, sitp_si_eventToString(si_event),
                sitp_si_stateToString(g_table->table_state), sitp_si_tableToString(g_table->table_id),
                g_table->subtype);

        if (SITP_SUCCESS == retCode)
        {
            /*
             ** ***********************************************************************************************
             **  Index into the state function pointer array to the index
             **  of the current state and execute the handler.
             ** ***********************************************************************************************
             */
            retCode = g_si_state_table[g_table->table_state](si_event,
                    event_data);
            if (SITP_FAILURE == retCode)
            {
                /*
                 ** ********************************************************************************************
                 ** There is no way of handling this error, other than printing
                 ** a error msg.
                 ** ********************************************************************************************
                 */
                MPE_LOG(
                        MPE_LOG_TRACE6,
                        MPE_MOD_SI,
                        "<%s::sitp_siWorkerThread> - handler failed for event: %d, state: %d\n",
                        SIMODULE, si_event, g_table->table_state);
            }
            dumpTableData(g_table);
        }
        else
        {
            /*
             ** ***********************************************************************************************
             ** There is no way of handling this error, other than printing
             ** a error msg and going on to get the next event.
             ** ***********************************************************************************************
             */
            MPE_LOG(
                    MPE_LOG_TRACE4,
                    MPE_MOD_SI,
                    "<%s::sitp_siWorkerThread> - sitp_si_get_table did not get an table for event: %s, state: %s\n",
                    SIMODULE, sitp_si_eventToString(si_event),
                    sitp_si_stateToString(g_table->table_state));
        }

        // Check if table timers have expired
        // Each table records an initial time when the table filters are set and
        // periodically compares if max time (set via ini) has elapsed before
        // moving on to the next table.
        // Mainly to accommodate speedy acquisition of multi-section SVCT and NTT
        // tables.
        checkForTableAcquisitionTimeouts();

        if (g_sitp_si_timeout == g_sitp_si_update_poll_interval
                && TABLES_ACQUIRED <= getTablesAcquired())
        {
            //Assert:  SI thread is polling for changes and it has already found all of the required tables for profile 2
            //so calculate the timeout based on a random time between the max and min poll interval.
            g_sitp_si_timeout = (g_sitp_si_update_poll_interval_max
                    - (((uint32_t) rand())
                            % (g_sitp_si_update_poll_interval_max
                                    - g_sitp_si_update_poll_interval_min)));
        }
        MPE_LOG(
                MPE_LOG_TRACE4,
                MPE_MOD_SI,
                "<%s::sitp_siWorkerThread> - Sleeping (mpeos_eventQueueWaitNext with g_sitp_si_timeout = 0x%x(%d)).\n",
                SIMODULE, g_sitp_si_timeout, g_sitp_si_timeout);

        retCode = mpeos_eventQueueWaitNext(g_sitp_si_queue, &si_event,
                &poptional_eventdata, NULL, NULL, g_sitp_si_timeout);
        if (MPE_ETIMEOUT == retCode)
        {
            si_event = MPE_ETIMEOUT;
        }
        // This event would have been preceded by a POD_EVENT_RESET_PENDING
        else if(MPE_POD_EVENT_READY == si_event)
        {
            MPE_LOG(MPE_LOG_INFO,
                    MPE_MOD_SI,
                    "<%s::sitp_siWorkerThread> - Received MPE_POD_EVENT_READY...\n",
                    SIMODULE);

            si_event = OOB_START_UP_EVENT;
        }

        MPE_LOG(MPE_LOG_TRACE5, MPE_MOD_SI,
                "<%s::sitp_siWorkerThread> - \n\n\n", SIMODULE);
    } /* End while( FALSE == g_shutDown ) */

    /*
     * Cleanup what SITP SI has created.
     */
    si_Shutdown();

    return;
}

char *sitp_si_tableToString(uint8_t table)
{
    switch (table)
    {
    case NETWORK_INFORMATION_TABLE_ID:
        return "NETWORK_INFORMATION_TABLE";
    case NETWORK_TEXT_TABLE_ID:
        return "NETWORK_TEXT_TABLE";
    case SF_VIRTUAL_CHANNEL_TABLE_ID:
        return "SF_VIRTUAL_CHANNEL_TABLE";
    case SYSTEM_TIME_TABLE_ID:
        return "SYSTEM_TIME_TABLE";
    default:
        return "UNKNOWN TABLE";
    }
}

/*
 ** Translate an event number to a string.
 */
char *sitp_si_eventToString(uint32_t event)
{
    switch (event)
    {
    case MPE_SF_EVENT_SECTION_FOUND:
        return "MPE_SF_EVENT_SECTION_FOUND";
    case MPE_SF_EVENT_LAST_SECTION_FOUND:
        return "MPE_SF_EVENT_LAST_SECTION_FOUND";
    case MPE_ETIMEOUT:
        return "MPE_ETIMEOUT";
    case MPE_SF_EVENT_SOURCE_CLOSED:
        return "MPE_SF_EVENT_SOURCE_CLOSED";
    case MPE_ETHREADDEATH:
        return "MPE_ETHREADDEATH";
    case OOB_START_UP_EVENT:
        return "OOB_START_UP_EVENT";
    default:
        return "UNKNOWN EVENT";
    }
}

/*
 ** Translate a state enum to a string.
 */
char *sitp_si_stateToString(uint32_t state)
{
    switch (state)
    {
    case SITP_SI_STATE_WAIT_TABLE_REVISION:
        return "SITP_SI_STATE_WAIT_TABLE_REVISION";
    case SITP_SI_STATE_WAIT_TABLE_COMPLETE:
        return "SITP_SI_STATE_WAIT_TABLE_COMPLETE";
    case SITP_SI_STATE_NONE:
    default:
        return "SITP_SI_STATE_NONE";
    }
}

static mpe_Error createAndSetSTTDataArray(void)
{
    mpe_Error retCode = SITP_SUCCESS;
    uint32_t jj = 0;

    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::createAndSetSTTDataArray> - Enter\n", SIMODULE);

    // STT
    /*
     ** Allocate the array table array.
     */
    retCode = mpeos_memAllocP(MPE_MEM_SI, sizeof(sitp_si_table_t),
            (void **) &(g_stt_table));
    if (SITP_SUCCESS != retCode)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::createAndSetSTTDataArray> - Error - Failed to allocate SI table array: %d\n",
                SIMODULE, retCode);
        return SITP_FAILURE;
    }
    MPE_LOG(MPE_LOG_TRACE5, MPE_MOD_SI,
            "<%s::createAndSetSTTDataArray> - Allocate Success: %d\n",
            SIMODULE, retCode);

    g_stt_table->table_id = SYSTEM_TIME_TABLE_ID;
    g_stt_table->subtype = 0; // Unknown?

    /*
     ** Set non-specific structure members to default values.
     */
    g_stt_table->table_state = SITP_SI_STATE_NONE;
    g_stt_table->rev_type = SITP_SI_REV_TYPE_UNKNOWN;
    g_stt_table->rev_sample_count = 0;
    g_stt_table->table_acquired = false;
    g_stt_table->table_unique_id = 0;
    g_stt_table->version_number = SITP_SI_INIT_VERSION;
    g_stt_table->number_sections = 0;

    for (jj = 0; jj < MAX_SECTION_NUMBER; jj++)
    {
        g_stt_table->section[jj].section_acquired = false;
        g_stt_table->section[jj].crc_32 = SITP_SI_INIT_CRC_32;
        g_stt_table->section[jj].crc_section_match_count = 0;
        g_stt_table->section[jj].last_seen_time = 0;
    }

    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::createAndSetSTTDataArray> - Exit\n", SIMODULE);
    return MPE_SUCCESS;
}

/*
 * create the table data structures and initialize the values.
 *
 *
 */
static mpe_Error createAndSetTableDataArray(
        sitp_si_table_t ** handle_table_array)
{
    mpe_Error retCode = SITP_SUCCESS;
    sitp_si_table_t * ptr_table_array;
    uint32_t ii = 0, jj = 0;

    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::createAndSetTableDataArray> - Enter\n", SIMODULE);

    ptr_table_array = *handle_table_array;

    // 1.NIT_CDS
    // 2.NIT_MMS
    // 3.SVCT_DCM
    // 4.SVCT_VCM
    // 5.NTT_SNS
    g_sitp_si_profile_table_number = 5;
    /*
     ** Allocate the array table array.
     */
    retCode = mpeos_memAllocP(MPE_MEM_SI, (sizeof(sitp_si_table_t)
            * g_sitp_si_profile_table_number), (void **) &(ptr_table_array));
    if (SITP_SUCCESS != retCode)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::createAndSetTableDataArray> - Error - Failed to allocate SI table array: %d\n",
                SIMODULE, retCode);
        return SITP_FAILURE;
    }
    MPE_LOG(MPE_LOG_TRACE5, MPE_MOD_SI,
            "<%s::createAndSetTableDataArray> - Allocate Success: %d\n",
            SIMODULE, retCode);

    ptr_table_array[0].table_id = NETWORK_INFORMATION_TABLE_ID; // NIT - Network Information Table
    ptr_table_array[0].subtype = CARRIER_DEFINITION_SUBTABLE; // CDS - Carrier Definition Subtable

    ptr_table_array[1].table_id = NETWORK_INFORMATION_TABLE_ID; // NIT - Network Information Table
    ptr_table_array[1].subtype = MODULATION_MODE_SUBTABLE; // MMS - Modulation Mode Subtable

    ptr_table_array[2].table_id = SF_VIRTUAL_CHANNEL_TABLE_ID; // SVCT - Short-form Virtual Channel Table
    ptr_table_array[2].subtype = VIRTUAL_CHANNEL_MAP; // VCM - Virtual Channel Map

    ptr_table_array[3].table_id = SF_VIRTUAL_CHANNEL_TABLE_ID; // SVCT - Short-form Virtual Channel Table
    ptr_table_array[3].subtype = DEFINED_CHANNEL_MAP; // DCM - Defined Channels Map

    ptr_table_array[4].table_id = NETWORK_TEXT_TABLE_ID; // NTT - Network Text Table
    ptr_table_array[4].subtype = SOURCE_NAME_SUBTABLE; // SNS - Source Name Subtable

    if (g_sitp_si_profile == PROFILE_CABLELABS_OCAP_1)
        goto finish_initialization;

    finish_initialization:
    /*
     ** Set non-specific structure members to default values.
     */
    for (ii = 0; ii < g_sitp_si_profile_table_number; ii++)
    {
        ptr_table_array[ii].table_state = SITP_SI_STATE_NONE;
        ptr_table_array[ii].rev_type = SITP_SI_REV_TYPE_UNKNOWN;
        ptr_table_array[ii].rev_sample_count = 0;
        ptr_table_array[ii].table_acquired = false;
        ptr_table_array[ii].table_unique_id = 0;
        ptr_table_array[ii].version_number = SITP_SI_INIT_VERSION;
        ptr_table_array[ii].number_sections = 0;

        for (jj = 0; jj < MAX_SECTION_NUMBER; jj++)
        {
            ptr_table_array[ii].section[jj].section_acquired = false;
            ptr_table_array[ii].section[jj].crc_32 = SITP_SI_INIT_CRC_32;
            ptr_table_array[ii].section[jj].crc_section_match_count = 0;
            ptr_table_array[ii].section[jj].last_seen_time = 0;
        }
        MPE_LOG(
                MPE_LOG_TRACE5,
                MPE_MOD_SI,
                "<%s::createAndSetTableDataArray> - Finish init - table: %d table_id: 0x%x(%d)\n",
                SIMODULE, ii, ptr_table_array[ii].table_id,
                ptr_table_array[ii].table_id);
    }

    *handle_table_array = ptr_table_array;

    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::createAndSetTableDataArray> - Exit\n", SIMODULE);
    return MPE_SUCCESS;
}

/*
 ** ***********************************************************************************************
 ** Description:
 ** Event handler for the state SITP_SI_STATE_WAIT_TABLE_REVISION.
 ** Handles the SF events:
 ** MPE_SF_EVENT_SECTION_FOUND, MPE_SF_EVENT_LAST_SECTION_FOUND.
 **
 ** MPE_SF_EVENT_SECTION_FOUND, MPE_SF_EVENT_LAST_SECTION_FOUND - .
 **
 ** All other events are handled in the function sitp_si_get_table().
 ** ***********************************************************************************************
 */
static mpe_Error sitp_si_state_wait_table_revision_handler(mpe_Event si_event,
        uint32_t event_data)
{
    mpe_Error retCode = SITP_SUCCESS;
    mpe_FilterSectionHandle section_handle = 0;
    uint8_t current_section_number = 0;
    uint8_t last_section_number = 0;
    uint8_t version = 0;
    uint32_t crc = 0;
    uint32_t crc2 = 0;
    uint32_t unique_id = 0;
    uint32_t section_index = 0;

    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::sitp_si_state_wait_table_revision_handler> - Enter\n",
            SIMODULE);

    switch (si_event)
    {
    case MPE_SF_EVENT_SECTION_FOUND:
    case MPE_SF_EVENT_LAST_SECTION_FOUND:
    {
        /*
         * The event data is the unique_id.  This was
         * verified before being passed to the handler
         */
        unique_id = event_data;

        /* get section */
        retCode = get_table_section(&section_handle, unique_id);
        if (SITP_SUCCESS != retCode)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_SI,
                    "<%s::sitp_si_state_wait_table_revision_handler> - failed to get the section handle: 0x%x, unique_id: %d\n",
                    SIMODULE, g_table->table_id, event_data);

            /*
             * Release the filter, reset the section array, and go back
             * to sitp_si_state_wait_table_revision.
             */
            release_filter(g_table, unique_id); // table, unquie_id
            reset_section_array(g_table->rev_type);
            g_table->table_state = SITP_SI_STATE_WAIT_TABLE_REVISION;
            g_sitp_si_timeout = g_sitp_si_update_poll_interval;
            retCode = SITP_FAILURE;
            break;
        }

        /*
         * Parse for version and number of sections
         * or the crc
         */
        retCode = get_revision_data(section_handle, &version,
                &current_section_number, &last_section_number, &crc);
        if (SITP_SUCCESS != retCode)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_SI,
                    "<%s::sitp_si_state_wait_table_revision_handler> - failed to parse RDD - table_id: 0x%x\n",
                    SIMODULE, g_table->table_id);

            reset_section_array(g_table->rev_type);
            g_table->table_state = SITP_SI_STATE_WAIT_TABLE_REVISION;
            g_sitp_si_timeout = g_sitp_si_update_poll_interval;
            break;
        }

        /*
         * determine the type of versioning that sitp_si
         * is going to use for this table
         */
        if (version == NO_VERSION)
        {
            g_table->rev_type = SITP_SI_REV_TYPE_CRC;
        }
        else
        {
            g_table->rev_type = SITP_SI_REV_TYPE_RDD;
        }
        MPE_LOG(
                MPE_LOG_TRACE5,
                MPE_MOD_SI,
                "<%s::sitp_si_state_wait_table_revision_handler> - Revision type = %s\n",
                SIMODULE, g_table->rev_type == SITP_SI_REV_TYPE_RDD ? "RDD"
                        : "CRC");

        switch (g_table->rev_type)
        {
        case SITP_SI_REV_TYPE_RDD:
        {
            release_filter(g_table, unique_id); // table, unquie_id

            /* compare version to parsed version */
            if (version == g_table->version_number)
            {
                /*
                 * versions are the same, so release_section
                 * the section and set state to wait_table_rev
                 */
                release_table_section(g_table, section_handle);
                MPE_LOG(
                        MPE_LOG_TRACE5,
                        MPE_MOD_SI,
                        "<%s::sitp_si_state_wait_table_revision_handler> - Version matched for table: 0x%x, subtype: %d, table version: %d rdd_version: %d\n",
                        SIMODULE, g_table->table_id, g_table->subtype,
                        g_table->version_number, version);
                /* set poll interval timer */
                /* no state change */
                g_table->table_state = SITP_SI_STATE_WAIT_TABLE_REVISION;
                g_sitp_si_timeout = g_sitp_si_update_poll_interval;
                break;
            }
            else
            {
                /*
                 * The versions are are different, so
                 * set version and number of sections in the table
                 * data struct (last_section_number is 0 based)
                 * and parse the section.
                 */
                g_table->version_number = version;

                /*
                 */
                g_table->number_sections = (uint8_t)(last_section_number + 1);

                if (SITP_SUCCESS != parseAndUpdateTable(g_table,
                        section_handle, &g_table->version_number,
                        &current_section_number, &last_section_number, &crc2))
                {
                    release_table_section(g_table, section_handle);
                    reset_section_array(g_table->rev_type);
                    g_table->table_state = SITP_SI_STATE_WAIT_TABLE_REVISION;
                    g_sitp_si_timeout = g_sitp_si_update_poll_interval;
                    break;
                }

                /*
                 * Mark the section as acquired and release the
                 * section handle.
                 */
                g_table->section[current_section_number].section_acquired
                        = true;

                release_table_section(g_table, section_handle);

                /*
                 * Set filter for the number of sections. Use the 'g_sitp_si_filter_multiplier'
                 * value multiplied by the number of sections to make sure we get all of the
                 * sections.
                 */
                if (true != (g_table->table_acquired
                        = isTable_acquired(g_table)))
                {
                    retCode = activate_filter(g_table,
                            (g_table->number_sections
                                    * g_sitp_si_filter_multiplier));
                    if (SITP_SUCCESS != retCode)
                    {
                        MPE_LOG(
                                MPE_LOG_ERROR,
                                MPE_MOD_SI,
                                "<%s::sitp_si_state_wait_table_revision_handler> - Error - Failed to activate filter. event: %s\n",
                                SIMODULE, sitp_si_eventToString(si_event));
                        reset_section_array(g_table->rev_type);
                    }
                    /* transition to wait_table_complete */
                    g_table->table_state = SITP_SI_STATE_WAIT_TABLE_COMPLETE;
                    g_sitp_si_timeout = g_sitp_si_status_update_time_interval;
                }
            }
            break;
        }
        case SITP_SI_REV_TYPE_CRC:
        {
            mpe_TimeMillis current_time = 0;
            mpeos_timeGetMillis(&current_time);

            /*
             * rev_sample_count holds the value of the times
             * this section has matched.  When this value ==
             * number_sections (timesToMatch in activate_filter call),
             * release the filter and try to rev again on the timeout.
             */
            
            g_table->rev_sample_count++;

            MPE_LOG(
                    MPE_LOG_TRACE5,
                    MPE_MOD_SI,
                    "<%s::sitp_si_state_wait_table_revision_handler> rev_sample_count = %d\n",
                    SIMODULE, g_table->rev_sample_count);

            // For multi-section tables check if the section event is LAST_SECTION_FOUND
            // forcing the filter to release correctly and be re-set on the next timeout.
            if ((g_table->rev_sample_count >= g_table->number_sections)
            	  || (si_event == MPE_SF_EVENT_LAST_SECTION_FOUND))
            {
                MPE_LOG(
                        MPE_LOG_TRACE5,
                        MPE_MOD_SI,
                        "<%s::sitp_si_state_wait_table_revision_handler> release filt, rev_count = 0\n",
                        SIMODULE);
                release_filter(g_table, unique_id); // table, unquie_id
                g_table->rev_sample_count = 0;
            }
            
            /* purge old section matches when doing CRC-based revisioning */
            purgeOldSectionMatches(g_table, current_time);

            /*
             * compare crc to CRCs saved in the table data struct.
             */
            if (true == matchedCRC32(g_table, crc, &section_index))
            {
                /*
                 * Matched CRC, so release_section
                 * and stay in the revisioning state
                 */
                retCode = release_table_section(g_table, section_handle);
                MPE_LOG(
                        MPE_LOG_TRACE5,
                        MPE_MOD_SI,
                        "<%s::sitp_si_state_wait_table_revision_handler> - CRC matched for table: 0x%x, subtype: %d, crc: 0x%08x, section_array index: %d \n",
                        SIMODULE, g_table->table_id, g_table->subtype, crc,
                        section_index);
                g_table->table_state = SITP_SI_STATE_WAIT_TABLE_REVISION;
                g_sitp_si_timeout = g_sitp_si_update_poll_interval;
                g_table->section[section_index].last_seen_time = current_time;
                break;
            }
            else
            {
                /*
                 * CRC didn't match, so this must be a new
                 * section.  Parse the new section.
                 */
                if (SITP_SUCCESS != parseAndUpdateTable(g_table,
                        section_handle, &g_table->version_number,
                        &current_section_number, &last_section_number, &crc2))
                {
                    release_table_section(g_table, section_handle);

                    /*
                     * FAIL: set poll interval timer - no state change
                     */
                    reset_section_array(g_table->rev_type);
                    g_table->table_state = SITP_SI_STATE_WAIT_TABLE_REVISION;
                    g_sitp_si_timeout = g_sitp_si_update_poll_interval;
                    retCode = SITP_FAILURE;
                    break;
                }

                /* release_section */
                release_table_section(g_table, section_handle);

                if (g_table->number_sections == MAX_SECTION_NUMBER)
                {
                    MPE_LOG(
                            MPE_LOG_WARN,
                            MPE_MOD_SI,
                            "<%s::sitp_si_state_wait_table_revision_handler> -  g_table->number_sections: %d reached max section number..\n",
                            SIMODULE, g_table->number_sections);
                }
                else
                {
                    /*
                     * Mark the section as acquired, if seen_count is matched after
                     * setting crc and increment number of sections in data struct
                     */
                    g_table->section[g_table->number_sections].crc_32 = crc;
                    g_table->section[g_table->number_sections].crc_section_match_count = 1;
                    g_table->section[g_table->number_sections].last_seen_time = current_time;

                    MPE_LOG(
                            MPE_LOG_TRACE5,
                            MPE_MOD_SI,
                            "<%s::sitp_si_state_wait_table_revision_handler> -  crc_section_match_count: %d\n",
                            SIMODULE,
                            g_table->section[g_table->number_sections].crc_section_match_count);

                    if (checkCRCMatchCount(g_table, g_table->number_sections)
                            == true)
                    {
                        g_table->section[g_table->number_sections].section_acquired
                                = true;
                    }
                    g_table->number_sections++;
                }

                /*
                 * set filter for the number of sections and transition to WAIT_TABLE_COMPLETE.
                 */
                retCode = activate_filter(g_table,
                        g_sitp_si_initial_section_match_count);
                if (MPE_SUCCESS != retCode)
                {
                    MPE_LOG(
                            MPE_LOG_ERROR,
                            MPE_MOD_SI,
                            "<%s::sitp_si_state_wait_table_revision_handler> - Failed to activate filter. event: %s\n",
                            SIMODULE, sitp_si_eventToString(si_event));
                }
                /* transition to wait_table_complete */
                g_table->table_state = SITP_SI_STATE_WAIT_TABLE_COMPLETE;
                g_sitp_si_timeout = g_sitp_si_status_update_time_interval;
            }
            break;
        }
        case SITP_SI_REV_TYPE_UNKNOWN:
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_SI,
                    "<%s::sitp_si_state_wait_table_revision_handler> - Error - revision type unknown-event: %s\n",
                    SIMODULE, sitp_si_eventToString(si_event));
            break;
        }
        }
        break;
    }
    default:
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::sitp_si_state_wait_table_revision_handler> - event not handled in this state: %s\n",
                SIMODULE, sitp_si_eventToString(si_event));
        retCode = SITP_FAILURE;
        break;
    }
    }
    return retCode;
}

/*
 ** ***********************************************************************************************
 ** Description:
 ** Event handler for the state SITP_SI_STATE_WAIT_TABLE_COMPLETE.
 ** Handles the SF events:
 ** MPE_SF_EVENT_SECTION_FOUND, MPE_SF_EVENT_LAST_SECTION_FOUND.
 **
 ** MPE_SF_EVENT_SECTION_FOUND, MPE_SF_EVENT_LAST_SECTION_FOUND - .
 **
 ** All other events are handled in the function sitp_si_get_table().
 ** ***********************************************************************************************
 */
static mpe_Error sitp_si_state_wait_table_complete_handler(mpe_Event si_event,
        uint32_t event_data)
{
    mpe_Error retCode = SITP_SUCCESS;
    mpe_FilterSectionHandle section_handle = 0;
    uint8_t current_section_number = 0;
    uint8_t last_section_number = 0;
    uint8_t version = 0;
    uint32_t crc = 0;
    uint32_t crc2 = 0;
    uint32_t unique_id = 0;
    uint32_t section_index = 0;

    MPE_LOG(
            MPE_LOG_TRACE4,
            MPE_MOD_SI,
            "<%s::sitp_si_state_wait_table_complete_handler> - Enter\n",
            SIMODULE);

    switch (si_event)
    {
    case MPE_SF_EVENT_SECTION_FOUND:
    case MPE_SF_EVENT_LAST_SECTION_FOUND:
    {
        unique_id = event_data;

        /*
         * get the new table section
         */
        retCode = get_table_section(&section_handle, event_data);
        if (SITP_SUCCESS != retCode)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_SI,
                    "<%s::sitp_si_state_wait_table_complete_handler> - failed to get the section handle: 0x%x, unique_id: %d\n",
                    SIMODULE, g_table->table_id, event_data);
            /*
             * Release the filter, reset the section array, and
             * go back to sitp_si_state_wait_table_revision.
             */
            release_filter(g_table, unique_id); // table, unquie_id
            reset_section_array(g_table->rev_type);
            g_table->table_state = SITP_SI_STATE_WAIT_TABLE_REVISION;
            g_sitp_si_timeout = g_sitp_si_update_poll_interval;
            break;
        }

        /*
         * parse for the revisioning data
         */
        retCode = get_revision_data(section_handle, &version,
                &current_section_number, &last_section_number, &crc);
        if (SITP_SUCCESS != retCode)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_SI,
                    "<%s::sitp_si_state_wait_table_complete_handler> - failed to parse RDD - table_id: 0x%x\n",
                    SIMODULE, g_table->table_id);
            reset_section_array(g_table->rev_type);
            g_table->table_state = SITP_SI_STATE_WAIT_TABLE_REVISION;
            g_sitp_si_timeout = g_sitp_si_update_poll_interval;
            break;
        }

        MPE_LOG(
                MPE_LOG_TRACE5,
                MPE_MOD_SI,
                "<%s::sitp_si_state_wait_table_complete_handler> - successfully parsed revision data - table_id: %s, version: %d, current_section_number: %d, last_section_number: %d, crc: 0x%08x\n",
                SIMODULE, sitp_si_tableToString(g_table->table_id), version,
                current_section_number, last_section_number, crc);

        /*
         * determine the type of versioning that sitp_si
         * is going to use for this table
         */
        if (version == NO_VERSION)
        {
            g_table->rev_type = SITP_SI_REV_TYPE_CRC;
        }
        else
        {
            g_table->rev_type = SITP_SI_REV_TYPE_RDD;
        }
        MPE_LOG(
                MPE_LOG_TRACE5,
                MPE_MOD_SI,
                "<%s::sitp_si_state_wait_table_complete_handler> - Revision type = %s\n",
                SIMODULE, g_table->rev_type == SITP_SI_REV_TYPE_RDD ? "RDD"
                        : "CRC");

        switch (g_table->rev_type)
        {
        case SITP_SI_REV_TYPE_RDD:
        {
            /*
             * If sitp already parsed this table section, release the
             * section and wait for the next event.
             */
            if (true
                    == g_table->section[current_section_number].section_acquired)
            {
                /* release_section */
                release_table_section(g_table, section_handle);
                /* transition to wait_table_complete */
                g_table->table_state = SITP_SI_STATE_WAIT_TABLE_COMPLETE;
                g_sitp_si_timeout = g_sitp_si_status_update_time_interval;
                break;
            }

            if (SITP_SUCCESS != parseAndUpdateTable(g_table, section_handle,
                    &(g_table->version_number), &current_section_number,
                    &last_section_number, &crc2))
            {
                /* release_section */
                release_table_section(g_table, section_handle);
                /* transition to wait_table_revision  to try again*/
                reset_section_array(g_table->rev_type);
                g_table->table_state = SITP_SI_STATE_WAIT_TABLE_REVISION;
                g_sitp_si_timeout = g_sitp_si_update_poll_interval;
                break;
            }

            /*
             * set version and number of sections in the table
             * data struct (last_section_number is 0 based)
             * and parse the section.
             */
            if(g_table->version_number == SITP_SI_INIT_VERSION)
            {
                g_table->version_number = version;
            }

            /*
             */
            g_table->number_sections = (uint8_t)(last_section_number + 1);
            /*
             * Mark the section as acquired.
             */
            g_table->section[current_section_number].section_acquired = true;

            /* release_section */
            release_table_section(g_table, section_handle);

            if (true == (g_table->table_acquired = isTable_acquired(g_table)))
            {
                set_tablesAcquired(g_table);
                release_filter(g_table, unique_id); // table, unquie_id
                /*
                 * Reset the section array, and go to sitp_si_state_wait_table_revision.
                 */
                reset_section_array(g_table->rev_type);
                g_table->table_state = SITP_SI_STATE_WAIT_TABLE_REVISION;
                if (g_stip_si_process_table_revisions)
                {
                    g_sitp_si_timeout = g_sitp_si_update_poll_interval;
                }
                else
                {
                    g_sitp_si_timeout = g_sitp_si_status_update_time_interval;
                }
                break;

            }
            else // Table not acquired
            {
                /* Stay in wait_table_complete */
                g_table->table_state = SITP_SI_STATE_WAIT_TABLE_COMPLETE;
                g_sitp_si_timeout = g_sitp_si_update_poll_interval;
                if (si_event == MPE_SF_EVENT_LAST_SECTION_FOUND)
                {
                    MPE_LOG(
                            MPE_LOG_TRACE5,
                            MPE_MOD_SI,
                            "<%s::sitp_si_state_wait_table_complete_handler> - Processed MPE_SF_EVENT_LAST_SECTION_FOUND without completing acquisition - resetting filter for TID 0x%x, subtype: %d\n",
                            SIMODULE, g_table->table_id, g_table->subtype);
                    g_table->table_unique_id = 0;
                    retCode = activate_filter(g_table,
                                  (g_table->number_sections * g_sitp_si_filter_multiplier));
                    if (MPE_SUCCESS != retCode)
                    {
                        MPE_LOG(
                                MPE_LOG_ERROR,
                                MPE_MOD_SI,
                                "<%s::sitp_si_state_wait_table_complete_handler> - Failed to activate filter. event: %s\n",
                                SIMODULE, sitp_si_eventToString(si_event));
                    }
                }
            }
            break;
        }
        case SITP_SI_REV_TYPE_CRC:
        {
            mpe_TimeMillis current_time = 0;
            mpeos_timeGetMillis(&current_time);

            /* purge old section matches when doing CRC-based revisioning */
            purgeOldSectionMatches(g_table, current_time);

            /*
             * compare crc to CRCs saved in the table data struct.
             */
            if (true == matchedCRC32(g_table, crc, &section_index))
            {
                /*
                 * Matched CRC, so release_section
                 * and stay in the revisioning state
                 */
                retCode = release_table_section(g_table, section_handle);
                MPE_LOG(
                        MPE_LOG_TRACE5,
                        MPE_MOD_SI,
                        "<%s::sitp_si_state_wait_table_complete_handler> - CRC matched for table: 0x%x, subtype: %d, crc: 0x%08x, section_array index: %d \n",
                        SIMODULE, g_table->table_id, g_table->subtype, crc,
                        section_index);
                /*
                 * increment the match count, so sitp can bail when it is
                 * confident that all of the table is acquired.
                 */
                g_table->section[section_index].crc_section_match_count++;
                g_table->section[section_index].last_seen_time = current_time;
                MPE_LOG(
                        MPE_LOG_TRACE5,
                        MPE_MOD_SI,
                        "<%s::sitp_si_state_wait_table_complete_handler> -  crc_section_match_count: %d\n",
                        SIMODULE,
                        g_table->section[section_index].crc_section_match_count);
                if (checkCRCMatchCount(g_table, section_index) == true)
                {
                    g_table->section[section_index].section_acquired = true;
                }
            }
            else
            {
                /*
                 * no match for the crc, so parse the new section
                 */
                if (SITP_SUCCESS != parseAndUpdateTable(g_table,
                        section_handle, &g_table->version_number,
                        &current_section_number, &last_section_number, &crc2))
                {
                    release_table_section(g_table, section_handle);
                    /* set poll interval timer */
                    /* no state change */
                    reset_section_array(g_table->rev_type);
                    g_table->table_state = SITP_SI_STATE_WAIT_TABLE_REVISION;
                    g_sitp_si_timeout = g_sitp_si_update_poll_interval;
                    retCode = SITP_FAILURE;
                    break;
                }

                /* release_section */
                release_table_section(g_table, section_handle);

                if (g_table->number_sections == MAX_SECTION_NUMBER)
                {
                    MPE_LOG(
                            MPE_LOG_WARN,
                            MPE_MOD_SI,
                            "<%s::sitp_si_state_wait_table_complete_handler> -  g_table->number_sections: %d reached max section number..\n",
                            SIMODULE, g_table->number_sections);
                }
                else
                {
                    /*
                     * Mark the section as acquired.
                     * set crc, increment number of sections, and record the time in data struct
                     */
                    g_table->section[g_table->number_sections].crc_32 = crc;
                    g_table->section[g_table->number_sections].crc_section_match_count = 1;
                    g_table->section[g_table->number_sections].last_seen_time = current_time;

                    if (checkCRCMatchCount(g_table, g_table->number_sections)
                            == true)
                    {
                        g_table->section[g_table->number_sections].section_acquired
                                = true;
                    }
                    g_table->number_sections++;
                }
            }

            /*
             * Release the filter and notify SIDB if the table is acquired.
             * Else, just go wait for the next SECTION_FOUND event.
             */
            if (true == (g_table->table_acquired = isTable_acquired(g_table)))
            {
                set_tablesAcquired(g_table);
                release_filter(g_table, unique_id); // table, unquie_id
                /*
                 * Reset the section array, and go back to sitp_si_state_wait_table_revision.
                 */
                reset_section_array(g_table->rev_type);
                g_table->table_state = SITP_SI_STATE_WAIT_TABLE_REVISION;
                if (g_stip_si_process_table_revisions)
                {
                    /*
                     ** If the revisioning is by CRC and THE NIT has just been acquired, then
                     ** set the timer to SHORT_TIMER to expedite the acquisition of the SVCT.
                     */
                    g_sitp_si_timeout = (getTablesAcquired()
                            <= OOB_TABLES_ACQUIRED) ? SHORT_TIMER
                            : g_sitp_si_update_poll_interval;
                }
                else
                {
                    g_sitp_si_timeout = g_sitp_si_status_update_time_interval;
                }
            }
            else
            {
                /*
                 * Not done yet. Stay in this state - wait_table_complete
                 */
                g_table->table_state = SITP_SI_STATE_WAIT_TABLE_COMPLETE;
                g_sitp_si_timeout = g_sitp_si_update_poll_interval;
                if (si_event == MPE_SF_EVENT_LAST_SECTION_FOUND)
                {
                    MPE_LOG(
                            MPE_LOG_TRACE5,
                            MPE_MOD_SI,
                            "<%s::sitp_si_state_wait_table_complete_handler> - Processed MPE_SF_EVENT_LAST_SECTION_FOUND without completing acquisition - resetting filter for TID 0x%x, subtype: %d\n",
                            SIMODULE, g_table->table_id, g_table->subtype);
                    g_table->table_unique_id = 0;
                    retCode = activate_filter(g_table,
                            g_sitp_si_initial_section_match_count);
                    if (MPE_SUCCESS != retCode)
                    {
                        MPE_LOG(
                                MPE_LOG_ERROR,
                                MPE_MOD_SI,
                                "<%s::sitp_si_state_wait_table_complete_handler> - Failed to activate filter. event: %s\n",
                                SIMODULE, sitp_si_eventToString(si_event));
                    }
                }
            }
            break;
        }
        case SITP_SI_REV_TYPE_UNKNOWN:
        default:
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_SI,
                    "<%s::sitp_si_state_wait_table_complete_handler> - error  revision type is unknown\n",
                    SIMODULE);
            retCode = SITP_FAILURE;
            break;
        }
        }
        break;
    }
    default:
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::sitp_si_state_wait_table_complete_handler> - event not handled in this state: %s\n",
                SIMODULE, sitp_si_eventToString(si_event));
        retCode = SITP_FAILURE;
    }
        break;
    }
    return retCode;
}

static mpe_Error get_table_section(mpe_FilterSectionHandle * section_handle,
        uint32_t unique_id)
{
    mpe_Error retCode = SITP_SUCCESS;
    mpe_FilterSectionHandle sect_h = 0;
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI, "<%s::get_table_section> - Enter\n",
            SIMODULE);

    sect_h = *section_handle;

    retCode = mpeos_filterGetSectionHandle(unique_id, 0, &sect_h);

    if (retCode == MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_TRACE5,
                MPE_MOD_SI,
                "<%s::get_table_section> - got section handle - section_handle: 0x%x(%d) unique_id: 0x%x\n",
                SIMODULE, sect_h, sect_h, unique_id);
        retCode = SITP_SUCCESS;
    }
    else
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::get_table_section> - ERROR: Problem getting table section - retCode: %d, unique_id: %d.\n",
                SIMODULE, retCode, unique_id);
        retCode = SITP_FAILURE;
    }

    *section_handle = sect_h;
    MPE_LOG(MPE_LOG_TRACE5, MPE_MOD_SI, "<%s::get_table_section> - Exit\n",
            SIMODULE);

    return retCode;
}

static mpe_Error release_table_section(sitp_si_table_t * table_data,
        mpe_FilterSectionHandle section_handle)
{
    mpe_Error retCode = SITP_SUCCESS;
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::release_table_section> - Enter\n", SIMODULE);

    retCode = mpeos_filterSectionRelease(section_handle);
    if (retCode == MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_TRACE5,
                MPE_MOD_SI,
                "<%s::release_table_section> - released section handle - section_handle: 0x%x, table_id: 0x%x, subtype: %d\n",
                SIMODULE, section_handle, table_data->table_id,
                table_data->subtype);
        retCode = SITP_SUCCESS;
    }
    else
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::release_table_section> - ERROR: Problem releasing table section - retCode: %d, section_handle: 0x%x, table_id: 0x%x, subtype: %d\n",
                SIMODULE, retCode, section_handle, table_data->table_id,
                table_data->subtype);
        retCode = SITP_FAILURE;
    }
    MPE_LOG(MPE_LOG_TRACE5, MPE_MOD_SI, "<%s::release_table_section> - Exit\n",
            SIMODULE);

    return retCode;
}

/*
 * release the filter for this table and cleanup the struct data.
 */
static mpe_Error release_filter(sitp_si_table_t * table_data,
        uint32_t unique_id)
{
    mpe_Error retCode = SITP_SUCCESS;

    MPE_LOG(
            MPE_LOG_TRACE4,
            MPE_MOD_SI,
            "<%s::release_filter> - Releasing filter - unique_id: %d,table_unique_id: %d, table: %s\n",
            SIMODULE, unique_id, table_data->table_unique_id,
            sitp_si_tableToString(table_data->table_id));

    retCode = mpeos_filterRelease(table_data->table_unique_id);
    if (MPE_SUCCESS == retCode)
    {
        /*
         * reset my active_filter unique_id and filter start time
         */
        table_data->table_unique_id = 0;
        table_data->filter_start_time = 0;
        retCode = SITP_SUCCESS;
    }
    else
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::release_filter> - Problem releasing filter - unique_id: %d, table_id: %d\n",
                SIMODULE, table_data->table_unique_id, table_data->table_id);
        retCode = SITP_FAILURE;
    }
    return retCode;
}

/*
 * activate the filter associated with the table_data.
 */
static mpe_Error activate_filter(sitp_si_table_t * table_data,
        uint32_t timesToMatch)
{
    mpe_Error retCode = SITP_SUCCESS;
    mpe_FilterSource filterSource;
    mpe_FilterSpec filterSpec;

    /*
     * SI table filter component values.
     */
    uint8_t tid_mask[] =
    { 0xFF, 0, 0, 0, 0, 0, 0x0F };
    uint8_t tid_val[] =
    { table_data->table_id, 0, 0, 0, 0, 0, 0x00 | table_data->subtype };
    uint8_t svct_tid_mask[] =
    { 0xFF, 0, 0, 0, 0x0F };
    uint8_t svct_tid_val[] =
    { table_data->table_id, 0, 0, 0, 0x00 | table_data->subtype };
    uint8_t ntt_tid_mask[] =
    { 0xFF, 0, 0, 0, 0, 0, 0, 0x0F };
    uint8_t ntt_tid_val[] =
    { table_data->table_id, 0, 0, 0, 0, 0, 0, 0x00 | table_data->subtype };
    uint8_t neg_mask[] =
    { 0 };
    uint8_t neg_val[] =
    { 0 };

    /* Set the filterspec values */
    switch (table_data->table_id)
    {
    case SF_VIRTUAL_CHANNEL_TABLE_ID:
    {
        filterSpec.pos.length = 5;
        filterSpec.pos.mask = svct_tid_mask;
        filterSpec.pos.vals = svct_tid_val;
        break;
    }
    case NETWORK_INFORMATION_TABLE_ID:
    {
        filterSpec.pos.length = 7;
        filterSpec.pos.mask = tid_mask;
        filterSpec.pos.vals = tid_val;
        break;
    }
    case NETWORK_TEXT_TABLE_ID:
    {
        filterSpec.pos.length = 8;
        filterSpec.pos.mask = ntt_tid_mask;
        filterSpec.pos.vals = ntt_tid_val;
        break;
    }
    default:
        break;
    }
    filterSpec.neg.length = 0;
    filterSpec.neg.mask = neg_mask;
    filterSpec.neg.vals = neg_val;

    /* Set the INB filter source params */
    filterSource.sourceType = MPE_FILTER_SOURCE_OOB;
    filterSource.parm.p_OOB.tsId = 0;
    filterSource.pid = 0x1FFC; //Special PID value for OOB

    /* print the filter spec */
    printFilterSpec(filterSpec);

    /* Set the filter for tables */
    retCode = mpeos_filterSetFilter(&filterSource, &filterSpec,
            g_sitp_si_queue, NULL, MPE_SF_FILTER_PRIORITY_SITP, timesToMatch,
            0, &table_data->table_unique_id);

    if (retCode == MPE_SUCCESS)
    {
        // Record filter start time
        mpeos_timeGetMillis(&table_data->filter_start_time);

        MPE_LOG(
                MPE_LOG_TRACE4,
                MPE_MOD_SI,
                "<%s::activate_filter> - table: %s, times to match: %d, unique_id: %d(0x%x), start_time:%"PRIu64".\n",
                SIMODULE, sitp_si_tableToString(table_data->table_id),
                timesToMatch, table_data->table_unique_id,
                table_data->table_unique_id, table_data->filter_start_time);
        return SITP_SUCCESS;
    }

    /*
     ** Failure - now reset the active filter to default values and
     ** set the correct return value for handling with a state transition
     */
    table_data->table_unique_id = 0;

    MPE_LOG(
            MPE_LOG_ERROR,
            MPE_MOD_SI,
            "<%s::activate_filter> - FAILED to set a filter for table: 0x%x, error: %d\n",
            SIMODULE, table_data->table_id, retCode);

    retCode = SITP_FAILURE;

    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI, "<%s::activate_filter> - Exit\n",
            SIMODULE);
    return retCode;
}

/*
 * activate the filter associated with the table_data.
 */
static mpe_Error activate_stt_filter(sitp_si_table_t * table_data,
        uint32_t timesToMatch)
{
    mpe_Error retCode = SITP_SUCCESS;
    mpe_FilterSource filterSource;
    mpe_FilterSpec filterSpec;

    uint8_t stt_tid_mask[] =
    { 0xFF };
    uint8_t stt_tid_val[] =
    { table_data->table_id };
    uint8_t neg_mask[] =
    { 0 };
    uint8_t neg_val[] =
    { 0 };

    filterSpec.pos.length = 1;
    filterSpec.pos.mask = stt_tid_mask;
    filterSpec.pos.vals = stt_tid_val;

    filterSpec.neg.length = 0;
    filterSpec.neg.mask = neg_mask;
    filterSpec.neg.vals = neg_val;

    /* Set the INB filter source params */
    filterSource.sourceType = MPE_FILTER_SOURCE_OOB;
    filterSource.parm.p_OOB.tsId = 0;
    filterSource.pid = 0x1FFC; //Special PID value for OOB

    /* print the filter spec */
    printFilterSpec(filterSpec);

    /* Set the filter for STT */
    retCode = mpeos_filterSetFilter(&filterSource, &filterSpec,
            g_sitp_si_stt_queue, NULL, MPE_SF_FILTER_PRIORITY_SITP,
            timesToMatch, 0, &table_data->table_unique_id);

    if (retCode == MPE_SUCCESS)
    {
        // Record filter start time
        mpeos_timeGetMillis(&table_data->filter_start_time);

        MPE_LOG(
                MPE_LOG_TRACE4,
                MPE_MOD_SI,
                "<%s::activate_stt_filter> - table: %s, times to match: %d, unique_id: %d(0x%x), start_time:%"PRIu64".\n",
                SIMODULE, sitp_si_tableToString(table_data->table_id),
                timesToMatch, table_data->table_unique_id,
                table_data->table_unique_id, table_data->filter_start_time);
        return SITP_SUCCESS;
    }

    /*
     ** Failure - now reset the active filter to default values and
     ** set the correct return value for handling with a state transition
     */
    table_data->table_unique_id = 0;

    MPE_LOG(
            MPE_LOG_ERROR,
            MPE_MOD_SI,
            "<%s::activate_stt_filter> - FAILED to set a filter for table: 0x%x, error: %d\n",
            SIMODULE, table_data->table_id, retCode);

    retCode = SITP_FAILURE;

    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI, "<%s::activate_stt_filter> - Exit\n",
            SIMODULE);
    return retCode;
}

/*
 *
 *  Print the Section Filter filter spec struct for debugging.
 */
static void printFilterSpec(mpe_FilterSpec filterSpec)
{
    uint32_t index;
    char spec[1024];

    for (index = 0; index < filterSpec.pos.length; index++)
    {
        snprintf(spec, 1024, "<%s::printFilterSpec> - Positive length: %u ",
                SIMODULE, filterSpec.pos.length);
        if (filterSpec.pos.length > 0)
        {
            snprintf(spec, 1024, "%s Mask: 0x", spec);
            for (index = 0; index < filterSpec.pos.length; index++)
            {
                snprintf(spec, 1024, "%s%2.2x", spec,
                        filterSpec.pos.mask[index]);
            }
            snprintf(spec, 1024, "%s Value: 0x", spec);
            for (index = 0; index < filterSpec.pos.length; index++)
            {
                snprintf(spec, 1024, "%s%2.2x", spec,
                        filterSpec.pos.vals[index]);
            }
        }
    }

    MPE_LOG(MPE_LOG_TRACE5, MPE_MOD_SI, "%s\n", spec);

    snprintf(spec, 1024, "<%s::printFilterSpec> - Negative length: %u ",
            SIMODULE, filterSpec.neg.length);
    if (filterSpec.neg.length > 0)
    {
        snprintf(spec, 1024, "%s Mask: 0x", spec);
        for (index = 0; index < filterSpec.neg.length; index++)
        {
            snprintf(spec, 1024, "%s%2.2x", spec, filterSpec.neg.mask[index]);
        }
        snprintf(spec, 1024, "%s Value: 0x", spec);
        for (index = 0; index < filterSpec.neg.length; index++)
        {
            snprintf(spec, 1024, "%s%2.2x", spec, filterSpec.neg.vals[index]);
        }
    }
    MPE_LOG(MPE_LOG_TRACE5, MPE_MOD_SI, "%s\n", spec);
}

/*
 * Select and parse the correct table.
 */
static mpe_Error parseAndUpdateTable(sitp_si_table_t * ptr_table,
        mpe_FilterSectionHandle section_handle, uint8_t *version,
        uint8_t *section_number, uint8_t *last_section_number, uint32_t *crc)
{

    mpe_Error retCode = MPE_SUCCESS;

    switch (ptr_table->table_id)
    {
    case NETWORK_INFORMATION_TABLE_ID:
    {
        retCode = parseAndUpdateNIT(section_handle, version, section_number,
                last_section_number, crc);
        break;
    }
    case NETWORK_TEXT_TABLE_ID:
    {
        retCode = parseAndUpdateNTT(section_handle, version, section_number,
                last_section_number, crc);
        break;
    }
    case SF_VIRTUAL_CHANNEL_TABLE_ID:
    {
        retCode = parseAndUpdateSVCT(section_handle, version, section_number,
                last_section_number, crc);
        break;
    }
    case SYSTEM_TIME_TABLE_ID:
    {
        retCode = parseAndUpdateSTT(section_handle, version, section_number,
                last_section_number, crc);
        break;
    }
    default:
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::parseAndUpdateTable> - Error parsing - tableid: 0x%x(%d), subtype: %d, section_handle: %d\n",
                SIMODULE, ptr_table->table_id, ptr_table->table_id,
                ptr_table->subtype, section_handle);
        retCode = SITP_FAILURE;

    }
        break;
    }

    if (SITP_SUCCESS != retCode)
    {
        MPE_LOG(
                MPE_LOG_TRACE6,
                MPE_MOD_SI,
                "<%s::parseAndUpdateTable> - Error parsing - tableid: 0x%x(%d), subtype: %d, section_handle: %d\n",
                SIMODULE, ptr_table->table_id, ptr_table->table_id,
                ptr_table->subtype, section_handle);
    }
    MPE_LOG(
            MPE_LOG_TRACE4,
            MPE_MOD_SI,
            "<%s::parseAndUpdateTable> - Successfully parsed table - tableid: %s, subtype: %d\n",
            SIMODULE, sitp_si_tableToString(ptr_table->table_id),
            ptr_table->subtype);
    return retCode;
}

static mpe_Bool checkCRCMatchCount(sitp_si_table_t * ptr_table,
        uint32_t section_index)
{
    uint32_t section_seen_count = 0;
    switch (ptr_table->table_id)
    {
    case NETWORK_INFORMATION_TABLE_ID:
        section_seen_count = g_sitp_si_max_nit_section_seen_count;
        break;
    case SF_VIRTUAL_CHANNEL_TABLE_ID:
    {
        if (ptr_table->subtype == VIRTUAL_CHANNEL_MAP)
            section_seen_count = g_sitp_si_max_vcm_section_seen_count;
        else if (ptr_table->subtype == DEFINED_CHANNEL_MAP)
            section_seen_count = g_sitp_si_max_dcm_section_seen_count;
    }
        break;
    case NETWORK_TEXT_TABLE_ID:
        section_seen_count = g_sitp_si_max_ntt_section_seen_count;
        break;
    default:
        break;
    }

    if (ptr_table->section[section_index].crc_section_match_count
            == section_seen_count)
    {
        return true;
    }
    else
    {
        return false;
    }
}

/*
 ** Iterate through the section list to see if
 ** all of the sections associated with this table have been
 ** acquired.
 */
static mpe_Bool isTable_acquired(sitp_si_table_t * ptr_table)
{
    uint32_t ii = 0;
    mpe_Bool done = true;

    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::isTable_acquired> - number_of_sections: %d\n", SIMODULE,
            ptr_table->number_sections);

    for (ii = 0; ii < ptr_table->number_sections; ii++)
    {
        MPE_LOG(
                MPE_LOG_TRACE4,
                MPE_MOD_SI,
                "<%s::isTable_acquired> - ptr_table->table_id: 0x%x ptr_table->subtype: %d index: %d, acquired: %s\n",
                SIMODULE, ptr_table->table_id, ptr_table->subtype, ii, true
                        == ptr_table->section[ii].section_acquired ? "true"
                        : "false");

        if (false == ptr_table->section[ii].section_acquired)
        {
            done = false;
        }
    }

    if (done)
        return true;

    // This is an optimization to expedite SVCT (VCM) acquisition.
    // If DCM is already fully acquired and if all of the 'defined'
    // channels in DCM are found 'mapped' in the VCM sections acquired
    // so far we don't need to wait for any more sections of VCM.
    // VCM can be marked as acquired.
    switch (ptr_table->table_id)
    {
    case SF_VIRTUAL_CHANNEL_TABLE_ID:
    {
        if (ptr_table->subtype == VIRTUAL_CHANNEL_MAP)
        {
            // If DCM is acquired and all of the defined channels are processed from VCM,
            // mark VCM sections as acquired
            if (((getSVCTAcquired() & DCM_ACQUIRED) == DCM_ACQUIRED) && mpe_siIsVCMComplete())
            {
                MPE_LOG(
                        MPE_LOG_TRACE5,
                        MPE_MOD_SI,
                        "<%s::isTable_acquired> -  DCM already acquired and mpe_siIsVCMComplete() returned true, marking VCM acquired!\n",
                        SIMODULE);
                ptr_table->table_acquired = true;
                return true;
            }
            else
            {
                if (((getSVCTAcquired() & DCM_ACQUIRED) != DCM_ACQUIRED))
                {
                     MPE_LOG(MPE_LOG_TRACE5, MPE_MOD_SI, "<%s::isTable_acquired> -  DCM is not yet acquired \n", SIMODULE);
                }
                else if (((getSVCTAcquired() & DCM_ACQUIRED) == DCM_ACQUIRED) && !mpe_siIsVCMComplete())
                {
                    MPE_LOG(MPE_LOG_TRACE5, MPE_MOD_SI, "<%s::isTable_acquired> -  DCM is DONE but mpe_siIsVCMComplete() returned false\n", SIMODULE);
                }
            }
        }
        break;
    }
    default:
        break;
    }

    return false;
}

/*
 * Notify or Update SIDB of a complete table
 * acquisition.
 */
static void set_tablesAcquired(sitp_si_table_t * ptr_table)
{
    mpe_siLockForWrite();
    switch (ptr_table->table_id)
    {
    case NETWORK_INFORMATION_TABLE_ID:
    {
        switch (ptr_table->subtype)
        {
        case CARRIER_DEFINITION_SUBTABLE:
        {
            MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
                    "<%s::getTablesAcquired()> - 0x%x\n", SIMODULE,
                    getTablesAcquired());
            if (getTablesAcquired() & CDS_ACQUIRED)
            {
                MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
                        "<%s::set_tableAcquired> - Setting CDS updated.\n",
                        SIMODULE);
                mpe_siNotifyTableChanged(OOB_NIT_CDS,
                        MPE_SI_CHANGE_TYPE_MODIFY, 0);
            }
            else
            {
                MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
                        "<%s::set_tableAcquired> - Setting CDS acquired.\n",
                        SIMODULE);
                setTablesAcquired(getTablesAcquired() | CDS_ACQUIRED);
                mpe_siNotifyTableChanged(OOB_NIT_CDS, MPE_SI_CHANGE_TYPE_ADD, 0);
            }

            if((getTablesAcquired() & MMS_ACQUIRED)
                && (getTablesAcquired() & DCM_ACQUIRED)
                && (getTablesAcquired() & VCM_ACQUIRED)
                && (getTablesAcquired() & NTT_ACQUIRED))
            {
                // Update Service entries
                mpe_siUpdateServiceEntries();
            }

            break;
        }
        case MODULATION_MODE_SUBTABLE:
        {
            MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
                    "<%s::getTablesAcquired()> - 0x%x\n", SIMODULE,
                    getTablesAcquired());
            if (getTablesAcquired() & MMS_ACQUIRED)
            {
                MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
                        "<%s::set_tableAcquired> - Setting MMS updated.\n",
                        SIMODULE);
                mpe_siNotifyTableChanged(OOB_NIT_MMS,
                        MPE_SI_CHANGE_TYPE_MODIFY, 0);
            }
            else
            {
                MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
                        "<%s::set_tableAcquired> - Setting MMS acquired.\n",
                        SIMODULE);
                setTablesAcquired(getTablesAcquired() | MMS_ACQUIRED);
                mpe_siNotifyTableChanged(OOB_NIT_MMS, MPE_SI_CHANGE_TYPE_ADD, 0);
            }

            if((getTablesAcquired() & CDS_ACQUIRED)
                && (getTablesAcquired() & VCM_ACQUIRED)
                && (getTablesAcquired() & DCM_ACQUIRED)
                && (getTablesAcquired() & NTT_ACQUIRED))
            {
                // Update Service entries
                mpe_siUpdateServiceEntries();
            }

            break;
        }
        default:
        {
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                    "<%s::set_tableAcquired> - no match for subtype - %d.\n",
                    SIMODULE, ptr_table->subtype);
            break;
        }
        }
        break;
    } // NETWORK_INFORMATION_TABLE_ID
    case NETWORK_TEXT_TABLE_ID:
    {
        if (getTablesAcquired() & NTT_ACQUIRED)
        {
            MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
                    "<%s::set_tableAcquired> - Setting NTT updated.\n",
                    SIMODULE);
            mpe_siNotifyTableChanged(OOB_NTT_SNS, MPE_SI_CHANGE_TYPE_MODIFY, 0);
        }
        else
        {
            MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
                    "<%s::set_tableAcquired> - Setting NTT acquired.\n",
                    SIMODULE);
            setTablesAcquired(getTablesAcquired() | NTT_ACQUIRED);
            mpe_siNotifyTableChanged(OOB_NTT_SNS, MPE_SI_CHANGE_TYPE_ADD, 0);
        }

        if((getTablesAcquired() & CDS_ACQUIRED)
            && (getTablesAcquired() & MMS_ACQUIRED)
            && (getTablesAcquired() & DCM_ACQUIRED)
            && (getTablesAcquired() & VCM_ACQUIRED) )
        {
            // Update Service names
            mpe_siUpdateServiceEntries();
        }

        break;
    } // NETWORK_TEXT_TABLE_ID
    case SF_VIRTUAL_CHANNEL_TABLE_ID:
    {
        switch (ptr_table->subtype)
        {
        case DEFINED_CHANNEL_MAP:
        {
            // If DCM is acquired and all the entries acquired so far in DCM
            // are found mapped in VCM then mark the VCM also acquired.

            MPE_LOG(
                    MPE_LOG_TRACE4,
                    MPE_MOD_SI,
                    "<%s::set_tableAcquired> - DCM acquired, check VCM state..\n",
                    SIMODULE);
            if (mpe_siIsVCMComplete())
            {
                if (getTablesAcquired() & VCM_ACQUIRED)
                {
                    mpe_siNotifyTableChanged(OOB_SVCT_VCM,
                            MPE_SI_CHANGE_TYPE_MODIFY, 0);
                }
                else
                {
                    setSVCTAcquired(getSVCTAcquired() | VCM_ACQUIRED);
                    setTablesAcquired(getTablesAcquired() | VCM_ACQUIRED);
                    MPE_LOG(
                            MPE_LOG_TRACE4,
                            MPE_MOD_SI,
                            "<%s::set_tableAcquired> - DCM acquired, mpe_siIsVCMComplete() returned true, setting VCM acquired\n",
                            SIMODULE);
                    mpe_siNotifyTableChanged(OOB_SVCT_VCM,
                            MPE_SI_CHANGE_TYPE_ADD, 0);
                }
            }

            // set DCM state, signal SIDB with DCM events
            if (getTablesAcquired() & DCM_ACQUIRED)
            {
                // TODO: signal event
                // Currently un-used
            }
            else
            {
                MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
                        "<%s::set_tableAcquired> - Setting DCM acquired.\n",
                        SIMODULE);
                setSVCTAcquired(getSVCTAcquired() | DCM_ACQUIRED);
                setTablesAcquired(getTablesAcquired() | DCM_ACQUIRED);
                // TODO: signal event
                // Currently un-used
                //mpe_siNotifyTableChanged(OOB_SVCT_DCM, MPE_SI_CHANGE_TYPE_ADD, 0);
            }
        }

        if((getTablesAcquired() & CDS_ACQUIRED)
            && (getTablesAcquired() & MMS_ACQUIRED)
            && (getTablesAcquired() & VCM_ACQUIRED)
            && (getTablesAcquired() & NTT_ACQUIRED))
        {
            // Update Service entries
            mpe_siUpdateServiceEntries();
        }

        break;
        case VIRTUAL_CHANNEL_MAP:
        {
            if (getTablesAcquired() & VCM_ACQUIRED)
            {
                mpe_siNotifyTableChanged(OOB_SVCT_VCM,
                        MPE_SI_CHANGE_TYPE_MODIFY, 0);
            }
            else
            {
                MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
                        "<%s::set_tableAcquired> - Setting VCM acquired.\n",
                        SIMODULE);
                setSVCTAcquired(getSVCTAcquired() | VCM_ACQUIRED);
                setTablesAcquired(getTablesAcquired() | VCM_ACQUIRED);
                mpe_siNotifyTableChanged(OOB_SVCT_VCM, MPE_SI_CHANGE_TYPE_ADD,
                        0);
            }

            if((getTablesAcquired() & CDS_ACQUIRED)
                && (getTablesAcquired() & MMS_ACQUIRED)
                && (getTablesAcquired() & DCM_ACQUIRED)
                && (getTablesAcquired() & NTT_ACQUIRED))
            {
                // Update Service entries
                mpe_siUpdateServiceEntries();
            }

            break;
        }
        default:
        {
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                    "<%s::set_tableAcquired> - no match for subtype - %d.\n",
                    SIMODULE, ptr_table->subtype);
            break;
        }
        }
        MPE_LOG(
                MPE_LOG_TRACE4,
                MPE_MOD_SI,
                "<%s::set_tableAcquired> - getTablesAcquired(): 0x%x getSVCTAcquired: 0x%x \n",
                SIMODULE, getTablesAcquired(), getSVCTAcquired());
        break;
    } //SF_VIRTUAL_CHANNEL_TABLE_ID

    default:
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                "<%s::set_tableAcquired> - no match for table - 0x%x.\n",
                SIMODULE, ptr_table->table_id);
        break;
    }
    }

    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::set_tableAcquired> - getTablesAcquired(): 0x%x \n", SIMODULE,
            getTablesAcquired());

    if (NIT_SVCT_ACQUIRED == getTablesAcquired())
    {
        MPE_LOG(
                MPE_LOG_TRACE4,
                MPE_MOD_SI,
                "<%s::set_tableAcquired> - NIT, SVCT tables acquired. Calling mpe_siSetGlobalState.\n",
                SIMODULE);
        mpe_siSetGlobalState ( SI_NIT_SVCT_ACQUIRED);
    }
    else if (TABLES_ACQUIRED == getTablesAcquired())
    {
        uint32_t num_services = 0;
        MPE_LOG(
                MPE_LOG_TRACE4,
                MPE_MOD_SI,
                "<%s::set_tableAcquired> - All OOB tables acquired. Calling mpe_siSetGlobalState.\n",
                SIMODULE);
        mpe_siSetGlobalState ( SI_FULLY_ACQUIRED);

        setTablesAcquired(getTablesAcquired() | SIDB_SIGNALED);
        (void) mpe_siGetTotalNumberOfServices(&num_services);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,
                "<%s::set_tableAcquired> - Number of Services: %d\n", SIMODULE,
                num_services);
    }

    // If all tables are acquired update cache
	// This will also be triggered when new sections are
	// processed for an already acquired table
    if((getTablesAcquired() & CDS_ACQUIRED)
        && (getTablesAcquired() & MMS_ACQUIRED)
        && (getTablesAcquired() & DCM_ACQUIRED)
        && (getTablesAcquired() & NTT_ACQUIRED))
    {
        /* Cache g_si_sourceName_entry, g_si_entry */
        if (g_siOOBCacheEnable && g_siOOBCacheLocation && g_siOOBSNSCacheLocation)
        {
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,"SI_FULLY_ACQUIRED, START CACHING NOW\n");
            // Cache NTT-SNS entries
            if (cache_sns_entries(g_siOOBSNSCacheLocation) == MPE_SUCCESS)
            {
                // Cache SVCT entries
                if(cache_si_entries(g_siOOBCacheLocation) == MPE_SUCCESS)
                {
                    /* Write the CRC to the SI Cache file */
                    write_crc_for_si_and_sns_cache(g_siOOBCacheLocation, g_siOOBSNSCacheLocation);
                    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,"CACHING DONE.\n");
                }
            }
        }
    }
    mpe_siReleaseWriteLock();
}

/*
 * reset the table data structure section array and all values
 * that control the revisioning and table acquisition actions
 * of this state machine.
 */
static void reset_section_array(sitp_si_revisioning_type_e rev_type)
{
    uint32_t ii;
    MPE_LOG(MPE_LOG_TRACE5, MPE_MOD_SI, "<%s::reset_section_array> - Enter\n",
            SIMODULE);

    if (SITP_SI_REV_TYPE_RDD == rev_type)
    {
        for (ii = 0; ii < g_table->number_sections; ii++)
        {
            g_table->section[ii].section_acquired = false;
            g_table->section[ii].crc_32 = 0;
            g_table->section[ii].crc_section_match_count = 0;
        }

        g_table->table_acquired = false;
        g_table->number_sections = 0;
    }
    else
    {
        for (ii = 0; ii < MAX_SECTION_NUMBER; ii++)
        {
            g_table->section[ii].section_acquired = false;
            g_table->section[ii].crc_section_match_count = 0;
        }
        g_table->table_acquired = false;
    }
}

/*
 * find the table in the table array with the events associated unique_id.
 */
static mpe_Error findTableIndexByUniqueId(uint32_t * index, uint32_t unique_id)
{
    uint32_t table_index = 0;
    for (table_index = 0; table_index < g_sitp_si_profile_table_number; table_index++)
    {
        MPE_LOG(
                MPE_LOG_TRACE5,
                MPE_MOD_SI,
                "<%s::findTableIndexByUniqueId> - uniqueId: %d, g_table_array[%d].table_unique_id: %d.\n",
                SIMODULE, unique_id, table_index,
                g_table_array[table_index].table_unique_id);
        if (g_table_array[table_index].table_unique_id == unique_id)
        {
            MPE_LOG(
                    MPE_LOG_TRACE5,
                    MPE_MOD_SI,
                    "<%s::findTableIndexByUniqueId> - FOUND - uniqueId: %d, g_table_array[%d].table_unique_id: %d.\n",
                    SIMODULE, unique_id, table_index,
                    g_table_array[table_index].table_unique_id);
            *index = table_index;
            return SITP_SUCCESS;
        }
    }

    /* couldn't find active filter - error */
    return SITP_FAILURE;
}

static void checkForTableAcquisitionTimeouts(void)
{
    uint32_t table_index = 0;
    mpe_TimeMillis current_time = 0;
    sitp_si_table_t *table = NULL;

    // Record the current time
    mpeos_timeGetMillis(&current_time);

    // Loop thru all the tables
    for (table_index = 0; table_index < g_sitp_si_profile_table_number; table_index++)
    {
        table = &g_table_array[table_index];

        MPE_LOG(
                MPE_LOG_TRACE5,
                MPE_MOD_SI,
                "<%s::checkForTableAcquisitionTimeouts> - index: %d, table_unique_id: %d.\n",
                SIMODULE, table_index, table->table_unique_id);

        // If no filters are currently set or if the rev type is RDD, the timers
        // are not utilized. This is an optimization used only for Profile-1
        // SI (CRC rev type) where he number of sections forming a table is unknown.
        if (table->table_unique_id == 0 || table->rev_type
                == SITP_SI_REV_TYPE_RDD)
            continue;

        // Check if the time elapsed since the first time filter was set is more than the set timeout.
        switch (table->table_id)
        {
        case NETWORK_INFORMATION_TABLE_ID:
        {
            if (table->subtype == CARRIER_DEFINITION_SUBTABLE)
            {
                if ((current_time - table->filter_start_time)
                        < g_sitp_si_max_nit_cds_wait_time)
                    continue;
            }
            else if (table->subtype == MODULATION_MODE_SUBTABLE)
            {
                if ((current_time - table->filter_start_time)
                        < g_sitp_si_max_nit_mms_wait_time)
                    continue;
            }
        }
            break;
        case SF_VIRTUAL_CHANNEL_TABLE_ID:
        {
            if (table->subtype == VIRTUAL_CHANNEL_MAP)
            {
                if ((current_time - table->filter_start_time)
                        < g_sitp_si_max_svct_vcm_wait_time)
                    continue;
            }
            else if (table->subtype == DEFINED_CHANNEL_MAP)
            {
                if ((current_time - table->filter_start_time)
                        < g_sitp_si_max_svct_dcm_wait_time)
                    continue;
            }
        }
            break;
        case NETWORK_TEXT_TABLE_ID:
            if ((current_time - table->filter_start_time)
                    < g_sitp_si_max_ntt_wait_time)
                continue;
            break;
        default:
            continue;
        }
        MPE_LOG(
                MPE_LOG_TRACE5,
                MPE_MOD_SI,
                "<%s::checkForTableAcquisitionTimeouts> - table timer expired for table_id: 0x%x subtype: %d unique_id: 0x%x\n",
                SIMODULE, table->table_id, table->subtype,
                table->table_unique_id);

        set_tablesAcquired(table);
        release_filter(table, table->table_unique_id);

        //Reset the section array, and go back to sitp_si_state_wait_table_revision.
        reset_section_array(table->rev_type);
        table->table_state = SITP_SI_STATE_WAIT_TABLE_REVISION;
        if (g_stip_si_process_table_revisions)
        {
            // If the revisioning is by CRC set the timer to SHORT_TIMER
            // to expedite the acquisition of next table.
            g_sitp_si_timeout
                    = (getTablesAcquired() <= OOB_TABLES_ACQUIRED) ? SHORT_TIMER
                            : g_sitp_si_update_poll_interval;
        }
        else
        {
            g_sitp_si_timeout = g_sitp_si_status_update_time_interval;
        }
    } // for table_index
}

/*
 *  Select the appropriate by priority, abased on the event and event data.  Also, handle any
 *  general events that aren't associated with any particular table. ie ETIMEOUT.
 */
static mpe_Error sitp_si_get_table(sitp_si_table_t **si_table, mpe_Event event,
        uint32_t data)
{
    uint32_t unique_id = 0;
    mpe_Error retCode = SITP_SUCCESS;
    uint32_t index = 0;
    uint32_t ii = 0;
    uint32_t jj = 0;
    uint32_t tables_to_set = 0;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<%s::sitp_si_get_table> - Matched event: %s\n", SIMODULE,
            sitp_si_eventToString(event));

    switch (event)
    {
    case OOB_START_UP_EVENT:
    case MPE_ETIMEOUT:
    {
        MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
                "<%s::sitp_si_get_table> - Matched event: %s\n", SIMODULE,
                sitp_si_eventToString(event));

        /*
         * Decide which table to set by testing which tables have already been
         * acquired. The NIT-MMS, NIT-CDS, SVCT-DCM and SVCT-VCM are
         * acquired first.  Based on the table acquired #define values
         * ( ie CDS_AQUIRED 0x0001 ), calculate the number
         * of tables in the g_table_array to iterate through and set.  If no tables have
         * been acquired yet, just set NIT CDS and NIT MMS filter, if < NIT_ACQUIRED (0x003)
         * tables have been acquired, don't set any filters, if >= NIT_ACQUIRED, set all filters
         * that aren't already set.
         */
        if ((getTablesAcquired() == 0) && (event == OOB_START_UP_EVENT))
        {
            /*
             ** Set filters for NIT_CDS, NIT_MMS, SVCT_DCM, SVCT_VCM, NTT_SNS
             */
            tables_to_set = g_sitp_si_profile_table_number;
            MPE_LOG(
                    MPE_LOG_INFO,
                    MPE_MOD_SI,
                    "<%s::sitp_si_get_table> - setting NIT, SVCT and NTT table filters\n",
                    SIMODULE);
        }
        else if (getTablesAcquired() < NIT_ACQUIRED)
        {
            /*
             ** Must have received a E_TIMEOUT while waiting for the initial NIT,
             ** SVCT sub-tables. These need to be acquired before anything else can
             ** happen, so just return and skip the handlers.
             */
            retCode = SITP_FAILURE;
            g_sitp_si_timeout = g_sitp_si_status_update_time_interval; // set default timeout period to wait forever
            MPE_LOG(
                    MPE_LOG_TRACE5,
                    MPE_MOD_SI,
                    "<%s::sitp_si_get_table> - Do nothing, still waiting for the NIT subtables\n",
                    SIMODULE);
            return retCode;
        }
        /*else if (getTablesAcquired() == NIT_ACQUIRED)
        {

             //NIT_CDS and NIT_MMS are the first two tables in the list ,
             //now get the SVCT_VCM, SVCT_DCM before getting anything else.
            tables_to_set = 4;
            MPE_LOG(
                    MPE_LOG_TRACE5,
                    MPE_MOD_SI,
                    "<%s::sitp_si_get_table> - Now Only adding SVCT/VCM filters\n",
                    SIMODULE);
        }
        */
        /*
        // Un-comment this block to test the case of setting VCM filter before DCM filter
        // and change the above line 3003 (block of (getTablesAcquired() == NIT_ACQUIRED)
        // to tables_to_set = 3. TODO: Make this configurable via ini
        else if (getTablesAcquired() == 0x07)
        {
            //
             // NIT_CDS and NIT_MMS are the first two tables in the list ,
             // now get the SVCT_VCM, SVCT_DCM before getting anything else.
             //
            tables_to_set = 4;
            MPE_LOG(
                    MPE_LOG_TRACE5,
                    MPE_MOD_SI,
                    "<%s::sitp_si_get_table> - Now Only adding SVCT (DCM, VCM) filters\n",
                    SIMODULE);
        }*/
        else
        {
            if (true == checkForActiveFilters())
            {
                /*
                 * Timed out during normal polling with filters set.  Keep filters set, but tell PSI thread to look for PSIP(CVCT)
                 * Tell the PSI thread to start acquiring the CVCT.
                 */
                tables_to_set = 0; //Don't set new filters. Just wait for the filters already set to be filled.
            }
            /*
             ** E_TIMEOUT - normal operation.  Acquire all tables that have not yet been acquired.
             */
            // this is set to 5 (includes CDS, MMS, DCM, VCM, SNS)
            tables_to_set = g_sitp_si_profile_table_number;
            //MPE_LOG(MPE_LOG_TRACE5, MPE_MOD_SI,"<%s::sitp_si_get_table> - REQUIRED OOB TABLES ACQUIRED - NORMAL ACQUISTION START...\n",
            //        SIMODULE);
        }

        /*
         * release filters if there are any set.
         * Since we are not handling single tables in this state, check
         * to make sure we only act on those tables in the IDLE state.
         */
        for (ii = 0; ii < tables_to_set; ii++)
        {
            /*
             * activate filters for the tables that are not already set.
             */
            if(g_table_array[ii].table_state == SITP_SI_STATE_NONE)
            {
                /*
                 * Now Activate new filters
                 */
                MPE_LOG(
                		MPE_LOG_TRACE5,
                        MPE_MOD_SI,
                        "<%s::sitp_si_get_table> - Setting a filter for table: %d(0x%x), timesToMatch: %d\n",
                        SIMODULE, g_table_array[ii].table_id,
                        g_table_array[ii].table_id, g_sitp_si_initial_section_match_count);
                // tableToMatch, timesToMatch - initially set to 50, configurable via ini
                retCode = activate_filter(&(g_table_array[ii]), g_sitp_si_initial_section_match_count);
                if (SITP_SUCCESS != retCode)
                {
                    MPE_LOG(
                            MPE_LOG_ERROR,
                            MPE_MOD_SI,
                            "<%s::sitp_si_get_table> - failed to set a filter for table: %d(0x%x)\n",
                            SIMODULE, g_table_array[ii].table_id,
                            g_table_array[ii].table_id);
                }
                else
                {
                    MPE_LOG(
                            MPE_LOG_TRACE5,
                            MPE_MOD_SI,
                            "<%s::sitp_si_get_table> - successfully set a filter for table: %s(0x%x), subtype: %d, unique_id: %d\n",
                            SIMODULE, sitp_si_tableToString(
                                    g_table_array[ii].table_id),
                            g_table_array[ii].table_id,
                            g_table_array[ii].subtype,
                            g_table_array[ii].table_unique_id);
                }
                g_table_array[ii].table_state = SITP_SI_STATE_WAIT_TABLE_COMPLETE;
                g_sitp_si_timeout = g_sitp_si_status_update_time_interval;
            }
            else if ( (g_table_array[ii].table_state != SITP_SI_STATE_NONE) &&
                      (g_table_array[ii].table_unique_id == 0) )
            {
                /*
                 * Now Activate the revision filters
                 */
                if ((g_table_array[ii].rev_type == SITP_SI_REV_TYPE_CRC)
                        && (true == g_sitp_si_rev_sample_sections) && (0
                        != g_table_array[ii].number_sections))
                {
                    /*
                     *   If the revision type is CRC and sample_sections flag == True and number_sections != 0
                     *   and this is a timeout event.
                     */
                    MPE_LOG(
                            MPE_LOG_TRACE5,
                            MPE_MOD_SI,
                            "<%s::sitp_si_get_table> - Setting a filter for table: %d(0x%x), timesToMatch: %d\n",
                            SIMODULE, g_table_array[ii].table_id,
                            g_table_array[ii].table_id, g_table_array[ii].number_sections);

                    retCode = activate_filter(&(g_table_array[ii]),
                            g_table_array[ii].number_sections); // tableToMatch, timesToMatch
                }
                else
                {
                    MPE_LOG(
                    		MPE_LOG_TRACE5,
                            MPE_MOD_SI,
                            "<%s::sitp_si_get_table> - Setting a filter for table: %d(0x%x), timesToMatch: 1\n",
                            SIMODULE, g_table_array[ii].table_id,
                            g_table_array[ii].table_id);

                    retCode = activate_filter(&(g_table_array[ii]), 1); // tableToMatch, timesToMatch
                }

                if (SITP_SUCCESS != retCode)
                {
                    /*
                     * go to the revision state to retry on the MPE_ETIMEOUT
                     */
                    MPE_LOG(
                            MPE_LOG_ERROR,
                            MPE_MOD_SI,
                            "<%s::sitp_si_get_table> - failed to set a filter for table: %d(0x%x)\n",
                            SIMODULE, g_table_array[ii].table_id,
                            g_table_array[ii].table_id);
                }
                else
                {
                    MPE_LOG(
                            MPE_LOG_TRACE5,
                            MPE_MOD_SI,
                            "<%s::sitp_si_get_table> - successfully set a filter for table: %s(0x%x), subtype: %d, unique_id: %d\n",
                            SIMODULE, sitp_si_tableToString(
                                    g_table_array[ii].table_id),
                            g_table_array[ii].table_id,
                            g_table_array[ii].subtype,
                            g_table_array[ii].table_unique_id);
                }
                g_table_array[ii].table_state = SITP_SI_STATE_WAIT_TABLE_REVISION;
                g_sitp_si_timeout = g_sitp_si_update_poll_interval;
            }
            else
            {
                MPE_LOG(
                        MPE_LOG_TRACE5,
                        MPE_MOD_SI,
                        "<%s::sitp_si_get_table> - No action - table_id: %s, subtype: %d has filters set.\n",
                        SIMODULE, sitp_si_tableToString(
                                g_table_array[ii].table_id),
                        g_table_array[ii].subtype);
            }
        }
        /*
         * skip the handlers
         */
        retCode = SITP_FAILURE;
        break;
    }
    case MPE_SF_EVENT_SOURCE_CLOSED:
    {
        /*
         * Cleanup associated filters and set that table back to wait table revision
         * First, release filter.
         */
        retCode = release_filter(g_table, g_table->table_unique_id);
        if (SITP_SUCCESS != retCode)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_SI,
                    "<%s::sitp_si_get_table> - failed to release filter: 0x%x, unique_id: %d\n",
                    SIMODULE, g_table->table_id, g_table->table_unique_id);
            retCode = SITP_FAILURE;
        }
        g_table->table_state = SITP_SI_STATE_WAIT_TABLE_REVISION;
        g_sitp_si_timeout = g_sitp_si_update_poll_interval;
        /*
         * skip the handlers
         */
        retCode = SITP_FAILURE;
        break;
    }

    case MPE_POD_EVENT_RESET_PENDING:
    {
        /*
         * Cleanup associated filters and set that table back to wait table revision
         * First, release filter.
         */
        MPE_LOG(MPE_LOG_INFO,
                MPE_MOD_SI,
                "<%s::sitp_si_get_table> - Received MPE_POD_EVENT_RESET_PENDING, resetting table filters..\n",
                SIMODULE);

        for (ii = 0; ii < g_sitp_si_profile_table_number; ii++)
        {
            /*
             * release filters for the tables that are set.
             */
            if ((g_table_array[ii].table_unique_id != 0))
            {
                retCode = release_filter(&g_table_array[ii], g_table_array[ii].table_unique_id);
                if (SITP_SUCCESS != retCode)
                {
                    MPE_LOG(
                            MPE_LOG_ERROR,
                            MPE_MOD_SI,
                            "<%s::sitp_si_get_table> - failed to release filter: 0x%x, unique_id: %d\n",
                            SIMODULE, g_table_array[ii].table_id, g_table_array[ii].table_unique_id);
                    retCode = SITP_FAILURE;
                }
            }
            g_table_array[ii].table_state = SITP_SI_STATE_NONE;
            g_table_array[ii].rev_type = SITP_SI_REV_TYPE_UNKNOWN;
            g_table_array[ii].rev_sample_count = 0;
            g_table_array[ii].table_acquired = false;
            g_table_array[ii].table_unique_id = 0;
            g_table_array[ii].version_number = SITP_SI_INIT_VERSION;
            g_table_array[ii].number_sections = 0;

            for (jj = 0; jj < MAX_SECTION_NUMBER; jj++)
            {
                g_table_array[ii].section[jj].section_acquired = false;
                g_table_array[ii].section[jj].crc_32 = SITP_SI_INIT_CRC_32;
                g_table_array[ii].section[jj].crc_section_match_count = 0;
                g_table_array[ii].section[jj].last_seen_time = 0;
            }
        }
        // Reset the global tables acquired variable
        g_sitp_siTablesAcquired = 0;
        g_sitp_si_timeout = g_sitp_si_status_update_time_interval;
        /*
         * skip the handlers
         */
        retCode = SITP_FAILURE;
        break;
    } // End MPE_POD_EVENT_RESET_PENDING
    case MPE_SF_EVENT_SECTION_FOUND:
    case MPE_SF_EVENT_LAST_SECTION_FOUND:
    {

        MPE_LOG(MPE_LOG_TRACE5, MPE_MOD_SI,
                "<%s::sitp_si_get_table> - Matched event: %s, unique_id: %d\n",
                SIMODULE, sitp_si_eventToString(event), data);
        unique_id = data;
        retCode = findTableIndexByUniqueId(&index, unique_id);
        if (SITP_SUCCESS == retCode)
        {
            *si_table = &(g_table_array[index]);
        }
        else
        {

            MPE_LOG(
                    MPE_LOG_TRACE5,
                    MPE_MOD_SI,
                    "<%s::sitp_si_get_table> - No table Matched event: %s, with unique_id: %d - SKIP HANDLER - tables acquired: %d\n",
                    SIMODULE, sitp_si_eventToString(event), data,
                    getTablesAcquired());
            /* no matching table - skip handler */
            retCode = SITP_FAILURE;
            g_sitp_si_timeout
                    = (getTablesAcquired() <= OOB_TABLES_ACQUIRED) ? SHORT_TIMER
                            : g_sitp_si_update_poll_interval;
        }
        break;
    }
    default:
    {
        MPE_LOG(
                MPE_LOG_TRACE5,
                MPE_MOD_SI,
                "<%s::sitp_si_get_table> - No Matched for event: %s - SKIP HANDLER\n",
                SIMODULE, sitp_si_eventToString(event));
        /* no matching table - skip handler */
        retCode = SITP_FAILURE;
        break;
    }
    }
    return retCode;
}

/*
 * interate through the section array for the table param and look for the crc.  If a match is found,
 * return the index of the section in the section array with the matching crc.
 */
static mpe_Bool matchedCRC32(sitp_si_table_t * table_data, uint32_t crc,
        uint32_t * index)
{
    uint32_t ii;

    for (ii = 0; ii < table_data->number_sections; ii++)
    {
        MPE_LOG(
                MPE_LOG_TRACE5,
                MPE_MOD_SI,
                "<%s::matchedCRC32> - looking for crc: 0x%08x for table_id: 0x%x, subtype: %d, section[%d] crc: 0x%08x last_seen %"PRIu64"\n",
                SIMODULE, crc, table_data->table_id, table_data->subtype, ii,
                table_data->section[ii].crc_32, table_data->section[ii].last_seen_time);
        if (table_data->section[ii].crc_32 == crc)
        {
            MPE_LOG(MPE_LOG_TRACE5, MPE_MOD_SI,
                    "<%s::matchedCRC32> - matched crc: 0x%08x\n", SIMODULE, crc);
            *index = ii;
            return true;
        }
    }
    return false;
}

static void purgeOldSectionMatches(sitp_si_table_t * table_data, mpe_TimeMillis current_time)
{
    uint32_t ii, jj;

    if (g_sitp_si_section_retention_time == 0)
    {
        MPE_LOG(
                MPE_LOG_TRACE5,
                MPE_MOD_SI,
                "<%s::purgeOldSectionMatches> - No section retention threshold defined - purging is disabled\n");
        return;
    }

    for (ii = 0; ii < table_data->number_sections; ii++)
    {
        mpe_TimeMillis curLastSeenTime = table_data->section[ii].last_seen_time;
        MPE_LOG( MPE_LOG_TRACE5, MPE_MOD_SI,
                 "<%s::purgeOldSectionMatches> - Checking section for purge: table_id/subtype: 0x%x/%d, section[%d] with crc 0x%08x last_seen %"PRIu64" (%dms ago)\n",
                 SIMODULE, table_data->table_id, table_data->subtype, ii,
                 table_data->section[ii].crc_32, curLastSeenTime, current_time - curLastSeenTime );

        if ((current_time - curLastSeenTime) > g_sitp_si_section_retention_time )
        { // Section ii is old, forget about it
            MPE_LOG( MPE_LOG_TRACE4, MPE_MOD_SI,
                     "<%s::purgeOldSectionMatches> - Purging table_id/subtype 0x%x/%d, section [%d] with crc 0x%08x (it's %dms old)\n",
                     SIMODULE, table_data->table_id, table_data->subtype, ii,
                     table_data->section[ii].crc_32, (int32_t)(current_time - curLastSeenTime) );

            // Move all the other records down...
            table_data->number_sections--;
            for (jj=ii; jj<table_data->number_sections;jj++)
            {
                table_data->section[jj] = table_data->section[jj+1];
            }
            ii--; // The ii element needs to be re-checked (since things have shifted)
        }
    }
} // END purgeOldSectionMatches(0

/*
 * Dump the table data.  for debug.
 */
static void dumpTableData(sitp_si_table_t * table_data)
{
    uint32_t ii = 0;

    MPE_LOG(
            MPE_LOG_TRACE5,
            MPE_MOD_SI,
            "<%s::dumpTableData> - table_id: %s, subtype: %d,  state: %s, rev_type: %s, table_acquired: %s, unique_id: %d, version: %d, number_sections: %d\n",
            SIMODULE, sitp_si_tableToString(table_data->table_id),
            table_data->subtype,
            sitp_si_stateToString(table_data->table_state),
            table_data->rev_type == SITP_SI_REV_TYPE_RDD ? "RDD" : "CRC",
            table_data->table_acquired ? "TRUE" : "FALSE",
            table_data->table_unique_id, table_data->version_number,
            table_data->number_sections);
    for (ii = 0; ii < table_data->number_sections; ii++)
    {
        if (table_data->rev_type == SITP_SI_REV_TYPE_RDD)
        {
            MPE_LOG(MPE_LOG_TRACE5, MPE_MOD_SI,
                    "<%s::dumpTableData> - index: %d, section_acquired: %s\n",
                    SIMODULE, ii,
                    table_data->section[ii].section_acquired ? "YES" : "NO");
        }
        else
        {
            MPE_LOG(
                    MPE_LOG_TRACE5,
                    MPE_MOD_SI,
                    "<%s::dumpTableData> - index: %d, section_acquired: %s, crc: 0x%08x, match_count: %d, last_seen %"PRIu64"\n",
                    SIMODULE, ii,
                    table_data->section[ii].section_acquired ? "YES" : "NO",
                    table_data->section[ii].crc_32,
                    table_data->section[ii].crc_section_match_count,
                    table_data->section[ii].last_seen_time);
        }
    }
}

static mpe_Bool checkForActiveFilters(void)
{
    uint32_t table_index = 0;
    MPE_LOG(MPE_LOG_TRACE5, MPE_MOD_SI,
            "<%s::checkForActiveFilters> - Enter\n", SIMODULE);
    for (table_index = 0; table_index < g_sitp_si_profile_table_number; table_index++)
    {
        if (g_table_array[table_index].table_unique_id != 0)
        {
            MPE_LOG(MPE_LOG_TRACE5, MPE_MOD_SI,
                    "<%s::checkForActiveFilters> - found filter!\n", SIMODULE);
            return TRUE;
        }
    }
    MPE_LOG(MPE_LOG_TRACE5, MPE_MOD_SI,
            "<%s::checkForActiveFilters> - no filters!\n", SIMODULE);
    return FALSE;
}

/*
 * ****************************************************************************
 *                          Public functions
 * ****************************************************************************
 */

uint32_t getTablesAcquired(void)
{
    return g_sitp_siTablesAcquired;
}

void setTablesAcquired(uint32_t tables_acquired)
{
    g_sitp_siTablesAcquired = tables_acquired;
}

void setSVCTAcquired(uint32_t svct_acquired)
{
    g_sitp_svctAcquired = svct_acquired;
}

uint32_t getSVCTAcquired(void)
{
    return g_sitp_svctAcquired;
}

uint32_t getDumpTables(void)
{
    return g_sitp_si_dump_tables;
}

mpe_Bool isVersioningByCRC32(void)
{
    return g_sitp_si_versioning_by_crc32;
}
