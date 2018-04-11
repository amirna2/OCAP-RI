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

#include <mpe_types.h>
#include <sysmgr.h>
#include <simgr.h>
#include <podmgr.h>
#include <filtermgr.h>
#include <mpeos_media.h>
#include <mpeos_filter.h>
#include <../include/sitp_psi.h>
#include <../include/sitp_si.h>
#include <mpe_socket.h>
#include <mpe_error.h>
#include <mpe_media.h>
#include <mpe_dbg.h>
#include <mpe_pod.h>

#include <string.h>

mpe_Error mpe_SI_Database_Init(void);
void mpe_SI_Database_Clear(void);

mpe_si_ftable_t si_ftable =
{ mpe_siInit, mpe_siShutdown,

mpe_siGetPidByAssociationTag, mpe_siGetPidByCarouselID,
        mpe_siGetPidByComponentTag,
        mpe_siGetProgramNumberByDeferredAssociationTag,

        mpe_siLockForRead, mpe_siUnLock,

        mpe_siGetTotalNumberOfTransports, mpe_siGetAllTransports,

        mpe_siGetTransportDeliverySystemType,
        mpe_siGetNumberOfNetworksForTransportHandle,
        mpe_siGetAllNetworksForTransportHandle,
        mpe_siGetNumberOfTransportStreamsForTransportHandle,
        mpe_siGetAllTransportStreamsForTransportHandle,
        mpe_siGetNetworkHandleByTransportHandleAndNetworkId,
        mpe_siGetTransportIdForTransportHandle,
        mpe_siGetTransportHandleByTransportId,

        mpe_siGetNetworkServiceInformationType,
        mpe_siGetNetworkIdForNetworkHandle,
        mpe_siGetNetworkNameForNetworkHandle,
        mpe_siGetNetworkLastUpdateTimeForNetworkHandle,
        mpe_siGetNumberOfTransportStreamsForNetworkHandle,
        mpe_siGetAllTransportStreamsForNetworkHandle,
        mpe_siGetTransportHandleForNetworkHandle,
        mpe_siGetTransportStreamHandleByTransportFrequencyModulationAndTSID,

        mpe_siGetTransportStreamIdForTransportStreamHandle,
        mpe_siGetDescriptionForTransportStreamHandle,
        mpe_siGetTransportStreamNameForTransportStreamHandle,
        mpe_siGetFrequencyForTransportStreamHandle,
        mpe_siGetModulationFormatForTransportStreamHandle,
        mpe_siGetTransportStreamServiceInformationType,
        mpe_siGetTransportStreamLastUpdateTimeForTransportStreamHandle,
        mpe_siGetNumberOfServicesForTransportStreamHandle,
        mpe_siGetAllServicesForTransportStreamHandle,
        mpe_siGetNetworkHandleForTransportStreamHandle,
        mpe_siGetTransportHandleForTransportStreamHandle,

        mpe_siGetServiceHandleByServiceNumber,
        mpe_siGetServiceHandleBySourceId, mpe_siGetServiceHandleByAppId,
        mpe_siGetServiceHandleByFrequencyModulationProgramNumber,
        mpe_siGetServiceHandleByServiceName, mpe_siCreateDynamicServiceHandle,
        mpe_siCreateDSGServiceHandle,

        mpe_siGetTransportStreamHandleForServiceHandle,
        mpe_siGetPMTPidForServiceHandle, mpe_siGetServiceTypeForServiceHandle,
        mpe_siGetNumberOfLongNamesForServiceHandle,
        mpe_siGetLongNamesForServiceHandle,
        mpe_siGetNumberOfNamesForServiceHandle, mpe_siGetNamesForServiceHandle,
        mpe_siGetServiceNumberForServiceHandle,
        mpe_siGetSourceIdForServiceHandle, mpe_siGetFrequencyForServiceHandle,
        mpe_siGetProgramNumberForServiceHandle,
        mpe_siGetModulationFormatForServiceHandle,
        mpe_siGetServiceDetailsLastUpdateTimeForServiceHandle,
        mpe_siGetIsFreeFlagForServiceHandle,
        mpe_siGetNumberOfServiceDescriptionsForServiceHandle,
        mpe_siGetServiceDescriptionsForServiceHandle,
        mpe_siGetServiceDescriptionLastUpdateTimeForServiceHandle,
        mpe_siGetNumberOfComponentsForServiceHandle,
        mpe_siGetAllComponentsForServiceHandle,
        mpe_siGetPcrPidForServiceHandle, mpe_siGetPATVersionForServiceHandle,
        mpe_siGetPMTVersionForServiceHandle, mpe_siGetPATCRCForServiceHandle,
        mpe_siGetPMTCRCForServiceHandle,
        mpe_siGetDeliverySystemTypeForServiceHandle,
        mpe_siGetCASystemIdArrayLengthForServiceHandle,
        mpe_siGetCASystemIdArrayForServiceHandle,
        mpe_siGetMultipleInstancesFlagForServiceHandle,
        mpe_siGetOuterDescriptorsForServiceHandle,
        mpe_siGetNumberOfServiceDetailsForServiceHandle,
        mpe_siGetServiceDetailsForServiceHandle,
        mpe_siGetAppIdForServiceHandle, mpe_siReleaseServiceHandle,
        mpe_siSetSourceId, mpe_siUpdateFreqProgNumByServiceHandle,

        mpe_siGetIsFreeFlagForServiceDetailsHandle,
        mpe_siGetSourceIdForServiceDetailsHandle,
        mpe_siGetFrequencyForServiceDetailsHandle,
        mpe_siGetProgramNumberForServiceDetailsHandle,
        mpe_siGetModulationFormatForServiceDetailsHandle,
        mpe_siGetServiceTypeForServiceDetailsHandle,
        mpe_siGetNumberOfLongNamesForServiceDetailsHandle,
        mpe_siGetLongNamesForServiceDetailsHandle,
        mpe_siGetDeliverySystemTypeForServiceDetailsHandle,
        mpe_siGetServiceInformationTypeForServiceDetailsHandle,
        mpe_siGetServiceDetailsLastUpdateTimeForServiceDetailsHandle,
        mpe_siGetCASystemIdArrayLengthForServiceDetailsHandle,
        mpe_siGetCASystemIdArrayForServiceDetailsHandle,
        mpe_siGetTransportStreamHandleForServiceDetailsHandle,
        mpe_siGetServiceHandleForServiceDetailsHandle,

        mpe_siGetServiceComponentHandleByPid,
        mpe_siGetServiceComponentHandleByName,
        mpe_siGetServiceComponentHandleByTag,
        mpe_siGetServiceComponentHandleByAssociationTag,
        mpe_siGetServiceComponentHandleByCarouselId,
        mpe_siGetServiceComponentHandleForDefaultCarousel,
        mpe_siReleaseServiceComponentHandle,

        mpe_siGetPidForServiceComponentHandle,
        mpe_siGetNumberOfNamesForServiceComponentHandle,
        mpe_siGetNamesForServiceComponentHandle,
        mpe_siGetComponentTagForServiceComponentHandle,
        mpe_siGetAssociationTagForServiceComponentHandle,
        mpe_siGetCarouselIdForServiceComponentHandle,
        mpe_siGetLanguageForServiceComponentHandle,
        mpe_siGetStreamTypeForServiceComponentHandle,
        mpe_siGetComponentLastUpdateTimeForServiceComponentHandle,
        mpe_siGetServiceInformationTypeForServiceComponentHandle,
        mpe_siGetDescriptorsForServiceComponentHandle,
        mpe_siGetServiceDetailsHandleForServiceComponentHandle,
        mpe_siGetServiceHandleForServiceComponentHandle,

        mpe_siGetTotalNumberOfServices, mpe_siGetAllServices,

        mpe_siRegisterForSIEvents, mpe_siUnRegisterForSIEvents,

        mpe_siGetNumberOfSupportedRatingDimensions,
        mpe_siGetSupportedRatingDimensions,
        mpe_siGetNumberOfLevelsForRatingDimensionHandle,
        mpe_siGetNumberOfNamesForRatingDimensionHandle,
        mpe_siGetNamesForRatingDimensionHandle,
        mpe_siGetNumberOfNamesForRatingDimensionHandleAndLevel,
        mpe_siGetDimensionInformationForRatingDimensionHandleAndLevel,
        mpe_siGetRatingDimensionHandleByName, mpe_siRegisterForPSIAcquisition,
        mpe_siUnRegisterForPSIAcquisition,
        mpe_siRegisterForHNPSIAcquisition,
        mpe_siUnRegisterForHNPSIAcquisition,
        mpe_siGetNumberOfServiceEntriesForSourceId,
        mpe_siGetAllServiceHandlesForSourceId
};

void mpe_siSetup()
{
    mpe_sys_install_ftable(&si_ftable, MPE_MGR_TYPE_SI);
}

/* Global SI state */
mpe_SiGlobalState g_si_state = SI_ACQUIRING;

void mpe_siInit(void)
{
    /* Initialization */
    static int inited = 0;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siInit() \n");

    if (inited == 0)
    {
        inited = 1;

        /*
         *  Initialize the filter mgr, so SITP can set filters
         *  and get sections.
         */
        mpe_dbgInit();

        //
        // This init call is required by the simulator but breaks the released code,
        // See bug 3272. Resolving this cleanly will require an architecture
        // powow between simulator, media manager, and SI folks ...
        //
#if defined(MPE_WINDOWS) || defined(MPE_LINUX)
        (void)mpe_mediaInit();
#endif

        mpe_filterInit();

        mpe_podmgrInit();


        /*  Initialize OS layer. This has been added to support OC
         by acquiring PAT/PMT info when requested.
         */
        /* SI DB init */
        (void) mpe_SI_Database_Init();

        /* Call Table parser layer init */
        (void) sitp_si_Start();

        (void) sitp_psi_Start();
    }
}

void mpe_siShutdown(void)
{
    /* Shutdown */
    mpe_SI_Database_Clear();

    /* shutdown the SIPT component */
    sitp_si_ShutDown();
    sitp_psi_ShutDown();
}

/**
 SI DB Methods
 */

/* Points to the top of the si_table_entry linked list */
mpe_SiTableEntry *g_si_entry = NULL;

mpe_siSourceNameEntry *g_si_sourcename_entry = NULL;

/* Points to the top of the SPI si_table_entry linked list */
static mpe_SiTableEntry *g_si_spi_entry = NULL;

/* Points to the top of the transport stream linked list */
static mpe_SiTransportStreamEntry *g_si_ts_entry = NULL;
/* One and only OOB transport stream */
mpe_SiTransportStreamEntry *g_si_oob_ts_entry = NULL;
/* One and only DSG transport stream */
mpe_SiTransportStreamEntry *g_si_dsg_ts_entry = NULL;
/* One and only HN transport stream */
mpe_SiTransportStreamEntry *g_si_HN_ts_entry = NULL;

/*  Frequency and modulation modes populated from NIT_CD and NIT_MM tables
 are stored in the following arrays.  Note that we reference these arrays
 starting at index 1, so make them one location bigger. */
uint32_t g_frequency[MAX_FREQUENCIES + 1];
mpe_SiModulationMode g_mode[MAX_FREQUENCIES + 1];

/*  Following variables are used to determine whether or not to
 enforce frequency range check.
 */
static const char *siOOBEnabled = NULL;
static mpe_Bool g_frequencyCheckEnable = FALSE;
mpe_Bool g_SITPConditionSet = FALSE;

/*  Maximum and minimum frequencies in the NIT_CDS table */
uint32_t g_maxFrequency = 0;
uint32_t g_minFrequency = 0;

/*  Global SI lock used by all readers and writer (SITP)
 Multiple readers can read at the same time, but all
 readers are blocked when writer has the lock.
 */
static mpe_Cond g_global_si_cond;
static uint32_t g_cond_counter = 0;
static mpe_Mutex g_global_si_mutex = NULL; /* sync primitive */

/*  Lock for transport stream list */
static mpe_Mutex g_global_si_list_mutex = NULL; /* sync primitive */

/*  This condition is set by SITP when all OOB tables are discovered */
static mpe_Cond g_SITP_cond;

static mpe_Mutex g_dsg_mutex = NULL; /* DSG sync primitive */

static mpe_Mutex g_HN_mutex = NULL; /* HN sync primitive */

/*  Registered SI DB listeners and queues (queues are used at MPE level only) */
static mpe_SiRegistrationListEntry *g_si_registration_list = NULL;
static mpe_Mutex g_registration_list_mutex = NULL; /* sync primitive */
static mpe_SIQueueListEntry *g_si_queue_list = NULL;

static mpe_SiRegistrationListEntry *g_si_unreg_list = NULL;

/*  System times at which transport streams and networks are created/updated */
static mpe_TimeMillis gtime_transport_stream;
static mpe_TimeMillis gtime_network;

/*  Global variable to indicate the total number of services in the SI DB
 This variable currently only includes those services that are signalled in
 OOB SI (SVCT). */
static uint32_t g_numberOfServices = 0;

/*  Control variables for the various tables */
static mpe_Bool g_frequenciesUpdated = FALSE;
static mpe_Bool g_modulationsUpdated = FALSE;
static mpe_Bool g_channelMapUpdated = FALSE;

/*  Rating region US (50 states + possessions)
 Ratings are currently hard-coded for Region 1.
 Allowed by SCTE 65 */

static mpe_SiRatingValuesDefined g_US_RRT_val_Entire_Audience[6];
static char* g_US_RRT_eng_vals_Entire_Audience[6] =
{ "", "None", "TV-G", "TV-PG", "TV-14", "TV-MA" };
static char* g_US_RRT_eng_abbrev_vals_Entire_Audience[6] =
{ "", "None", "TV-G", "TV-PG", "TV-14", "TV-MA" };
static mpe_SiRatingDimension g_US_RRT_dim_Entire_Audience =
{ NULL, 1, 6, g_US_RRT_val_Entire_Audience };
static char* g_US_RRT_eng_dim_Entire_Audience = "Entire Audience";

static mpe_SiRatingValuesDefined g_US_RRT_val_Dialogue[2];
static char* g_US_RRT_eng_vals_Dialogue[2] =
{ "", "D" };
static char* g_US_RRT_eng_abbrev_vals_Dialogue[2] =
{ "", "D" };
static mpe_SiRatingDimension g_US_RRT_dim_Dialogue =
{ NULL, 0, 2, g_US_RRT_val_Dialogue };
static char* g_US_RRT_eng_dim_Dialogue = "Dialogue";

static mpe_SiRatingValuesDefined g_US_RRT_val_Language[2];
static char* g_US_RRT_eng_vals_Language[2] =
{ "", "L" };
static char* g_US_RRT_eng_abbrev_vals_Language[2] =
{ "", "L" };
static mpe_SiRatingDimension g_US_RRT_dim_Language =
{ NULL, 0, 2, g_US_RRT_val_Language };
static char* g_US_RRT_eng_dim_Language = "Language";

static mpe_SiRatingValuesDefined g_US_RRT_val_Sex[2];
static char* g_US_RRT_eng_vals_Sex[2] =
{ "", "S" };
static char* g_US_RRT_eng_abbrev_vals_Sex[2] =
{ "", "S" };
static mpe_SiRatingDimension g_US_RRT_dim_Sex =
{ NULL, 0, 2, g_US_RRT_val_Sex };
static char* g_US_RRT_eng_dim_Sex = "Sex";

static mpe_SiRatingValuesDefined g_US_RRT_val_Violence[2];
static char* g_US_RRT_eng_vals_Violence[2] =
{ "", "V" };
static char* g_US_RRT_eng_abbrev_vals_Violence[2] =
{ "", "V" };
static mpe_SiRatingDimension g_US_RRT_dim_Violence =
{ NULL, 0, 2, g_US_RRT_val_Violence };
static char* g_US_RRT_eng_dim_Violence = "Violence";

static mpe_SiRatingValuesDefined g_US_RRT_val_Children[3];
static char* g_US_RRT_eng_vals_Children[3] =
{ "", "TV-Y", "TV-Y7" };
static char* g_US_RRT_eng_abbrev_vals_Children[3] =
{ "", "TV-Y", "TV-Y7" };
static mpe_SiRatingDimension g_US_RRT_dim_Children =
{ NULL, 1, 3, g_US_RRT_val_Children };
static char* g_US_RRT_eng_dim_Children = "Children";

static mpe_SiRatingValuesDefined g_US_RRT_val_Fantasy_Violence[2];
static char* g_US_RRT_eng_vals_Fantasy_Violence[2] =
{ "", "FV" };
static char* g_US_RRT_eng_abbrev_vals_Fantasy_Violence[2] =
{ "", "FV" };
static mpe_SiRatingDimension g_US_RRT_dim_Fantasy_Violence =
{ NULL, 0, 2, g_US_RRT_val_Fantasy_Violence };
static char* g_US_RRT_eng_dim_Fantasy_Violence = "Fantasy Violence";

static mpe_SiRatingValuesDefined g_US_RRT_val_MPAA[9];
static char* g_US_RRT_eng_vals_MPAA[9] =
{ "", "MPAA Rating Not Applicable", "Suitable for All Ages",
        "Parental Guidance Suggested", "Parents Strongly Cautioned",
        "Restricted, under 17 must be accompanied by adult",
        "No One 17 and Under Admitted", "No One 17 and Under Admitted",
        "Not Rated by MPAA" };
static char* g_US_RRT_eng_abbrev_vals_MPAA[9] =
{ "", "N/A", "G", "PG", "PG-13", "R", "NC-17", "X", "NR" };
static mpe_SiRatingDimension g_US_RRT_dim_MPAA =
{ NULL, 0, 9, g_US_RRT_val_MPAA };
static char* g_US_RRT_eng_dim_MPAA = "MPAA";

static mpe_SiRatingDimension g_RRT_US_dimension[8];
static mpe_SiRatingRegion g_RRT_US =
{ NULL, 8, g_RRT_US_dimension };
static char* g_RRT_US_eng = "US (50 states + possessions)";

/* *End* Rating region US (50 states + possessions) */

/* DSG application */
#define  MIN_DSG_APPID  1
#define  MAX_DSG_APPID  65535

static char *modulationModes[] =
{ "UNKNOWN", "QPSK", "BPSK", "OQPSK", "VSB8", "VSB16", "QAM16", "QAM32",
        "QAM64", "QAM80", "QAM96", "QAM112", "QAM128", "QAM160", "QAM192",
        "QAM224", "QAM256", "QAM320", "QAM384", "QAM448", "QAM512", "QAM640",
        "QAM768", "QAM896", "QAM1024" };

#define MOD_STRING(mode) ((mode == MPE_SI_MODULATION_QAM_NTSC) ? "NTSC" : (((mode <= 0) || (mode > MPE_SI_MODULATION_QAM1024)) ? "UNKNOWN" : modulationModes[mode]))

/*  Prototypes for Internal Helper Methods */
static void init_si_entry(mpe_SiTableEntry *si_entry);
static void release_si_entry(mpe_SiTableEntry *si_entry);
static void release_descriptors(mpe_SiMpeg2DescriptorList *desc);
static void append_elementaryStream(mpe_SiProgramInfo *pi,
        mpe_SiElementaryStreamList *new_elem_stream);
static mpe_Error remove_elementaryStream(mpe_SiElementaryStreamList **elem_stream_list_head,
        mpe_SiElementaryStreamList *rmv_elem_stream);
static void get_serviceComponentType(mpe_SiElemStreamType stream_type,
        mpe_SiServiceComponentType *comp_type);
static void compare_ts_pat_programs(mpe_SiPatProgramList *new_pat_list,
        mpe_SiPatProgramList *saved_pat_list, mpe_Bool setNewProgramFlag);
static void signal_program_removed_event(mpe_SiTransportStreamEntry *ts_entry,
        mpe_SiPatProgramList *pat_list);
static void send_service_component_event(
        mpe_SiElementaryStreamList *elem_stream, uint32_t changeType);
static void send_si_event(uint32_t siEvent, uint32_t optional_data, uint32_t changeType);
static uint32_t si_state_to_event(mpe_SiGlobalState state);
static void delete_elementary_stream(mpe_SiElementaryStreamList *elem_stream);
void release_elementary_streams(mpe_SiElementaryStreamList *eStream);
static void mark_elementary_stream(mpe_SiElementaryStreamList *elem_stream);
static void notify_registered_queues(mpe_Event event, uint32_t si_handle);
static void delete_service_entry(mpe_SiTableEntry *si_entry);
static mpe_Bool find_program_in_ts_pat_programs(mpe_SiPatProgramList *pat_list,
        uint32_t pn);

// Initialize a brand new mpe_SiTransportStream Structure.
static mpe_Error init_transport_stream_entry(
        mpe_SiTransportStreamEntry *ts_entry, uint32_t Frequency);
// Release mpe_SiTransportStreamEntry that is no longer needed. both programs and streams lists should be empty!
static void release_transport_stream_entry(mpe_SiTransportStreamEntry *ts);
// Called to acquire existing, or new, mpe_SiTransportStreamEntry (returned as opaque handle.)
static mpe_SiTransportStreamHandle create_transport_stream_handle(
        uint32_t frequency, mpe_SiModulationMode mode);
// Called to acquire existing mpe_SiTransportStreamEntry.  NULL if not found.
static uint32_t get_transport_stream_handle(uint32_t freq,
        mpe_SiModulationMode mode);
//static uint32_t get_transport_stream_handle_by_freq_prog(uint32_t freq, uint32_t prog);
static uint32_t get_oob_transport_stream_handle(void);
static uint32_t get_dsg_transport_stream_handle(void);
static uint32_t get_hn_transport_stream_handle(void);

static void update_transport_stream_handles(void);
//static mpe_Bool findFrequencyInCDS (uint32_t frequency);
static mpe_SiProgramInfo *create_new_program_info(
        mpe_SiTransportStreamEntry *ts_entry, uint32_t prog_num);
static void release_program_info_entry(mpe_SiProgramInfo *pi);
static mpe_Error init_program_info(mpe_SiProgramInfo *pi);

//static void mark_si_entries(mpe_SiEntryState new_state);
static void mark_transport_stream_entries(mpe_SiEntryState new_state);

/*
 static mpe_Error check_elementary_stream(mpe_SiServiceHandle service_handle,
 mpe_SiElemStreamType stream_type,
 uint32_t elem_pid);
 */
static void printSortedArray(uint32_t *array, const int arraySize);

/* quicksort helper methods used to sort the frequency array */
static void q_sortElements(uint32_t numbers[], uint32_t left, uint32_t right);
void quickSortArray(uint32_t numbers[], int array_size);

static void init_default_rating_region(void);

static void add_to_unreg_list(mpe_EdEventInfo* edHandle);
static void unregister_clients(void);

/* Functions used to support mpe_dbgStatus() API for SI status information. */
static mpe_Error si_getSiEntryCount(void *);
static mpe_Error si_getSiEntry(uint32_t, uint32_t *, void *, void *);
mpe_Error si_dbgStatus(mpe_DbgStatusId, uint32_t *, void *, void *);

/* internal helper methods for */
static mpe_Error si_getServiceHandleByHNSession(mpe_HnStreamSession session,
        mpe_SiServiceHandle *service_handle);

/**
 * The <i>mpe_SI_Database_Init()</i> function initializes SI DB module.
 *
 * @param  None
 * @return MPE_SI_SUCCESS if initialization succeeded else mpe_Error
 */
mpe_Error mpe_SI_Database_Init(void)
{
    int i = 0;
    mpe_Error err = MPE_SUCCESS;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "mpe_si_Database_Init()\n");

    /* initialize the global SI pointers */
    g_si_entry = g_si_spi_entry = NULL;

    /* Initialize the frequency, mode arrays */
    for (i = 1; i <= MAX_FREQUENCIES; i++)
    {
        g_frequency[i] = 0;
        g_mode[i] = MPE_SI_MODULATION_UNKNOWN;
    }

    /*
     * Initilize the default rating region
     */
    init_default_rating_region();

#ifdef ISV_API
    /* MOT needs to fake the SI info for now... this will be removed when
     section filtering is working.
     */
    g_si_entry = spoofSIInfo();
#endif

    /* Create global mutex */
    err = mpe_mutexNew(&g_global_si_mutex);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                "<mpe_si_Database_Init() Mutex creation failed, returning MPE_EMUTEX\n");
        return MPE_EMUTEX;
    }

    /* Create global list mutex */
    err = mpe_mutexNew(&g_global_si_list_mutex);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                "<mpe_si_Database_Init() Mutex creation failed, returning MPE_EMUTEX\n");
        return MPE_EMUTEX;
    }

    /* Create global registration list mutex */
    err = mpe_mutexNew(&g_registration_list_mutex);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                "<mpe_si_Database_Init() Mutex creation failed, returning MPE_EMUTEX\n");
        return MPE_EMUTEX;
    }

    /* Create dsg mutex */
    err = mpe_mutexNew(&g_dsg_mutex);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                "<mpe_si_Database_Init() Mutex creation failed, returning MPE_EMUTEX\n");
        return MPE_EMUTEX;
    }

    /* Create Hn mutex */
    err = mpe_mutexNew(&g_HN_mutex);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                "<mpe_si_Database_Init() Mutex creation failed, returning MPE_EMUTEX\n");
        return MPE_EMUTEX;
    }

    /*
     Global condition used to lock the SIDB.
     Used by readers and the writer to acquire
     exclusive lock.
     Initial state is set to TRUE. It means the condition is available
     to be acquired.
     'autoReset' is set to TRUE also, to indicate once a lock
     is acquired by a reader/writer it stays in that state until
     the lock is released by that reader/writer.
     */
    if (mpe_condNew(TRUE, TRUE, &g_global_si_cond) != MPE_SI_SUCCESS)
    {
        return MPE_EMUTEX;
    }

    /*  Create global (SITP) condition object
     By setting the initial state of g_SITP_cond to 'FALSE'
     we are blocking all readers until SVCT is acquired and the
     condition object is set by SITP.
     'autoreset' is set to FALSE which means that once the
     condition object is in set (TRUE) state it stays in that
     state implying all the readers will be able to read from
     SI DB.
     */
    if (mpeos_condNew(FALSE, FALSE, &g_SITP_cond) != MPE_SI_SUCCESS)
    {
        return MPE_EMUTEX;
    }

    mpeos_timeGetMillis(&gtime_network);

    /*
     This has been added to determine whether or not to enforce
     a frequency check in mpe_siGetServicehandleByFrequencyModulationProgramNumber()
     API. For dynamic services (ex:VOD) not found in SI DB, a brand new entry
     is created at the time handle is requested. However, we still need to check
     if the frequency is with in the acceptable range. The values for minimum and
     maximum frequencies are extracted from NIT_CDS table. In the case where
     there are no OOB tables (i.e. if OOB is disabled) we don't want to enforce
     the frequency check since minimum and maximum freq values are unknown.
     */
    siOOBEnabled = mpeos_envGet("SITP.SI.ENABLED");
    if ((siOOBEnabled == NULL) || (stricmp(siOOBEnabled, "TRUE") == 0))
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "OOB is enabled by SITP.SI.ENABLED\n");
        g_frequencyCheckEnable = TRUE;
    }
    else
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "OOB is disabled by SITP.SI.ENABLED\n");
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "mpe_SI_Database_Init: SI state: %d \n",
            g_si_state);

    return MPE_SI_SUCCESS;
}

/**
 * The <i>mpe_SI_Database_Clear()</i> function shuts down the SI DB module.
 *
 * @param  None
 * @return MPE_SI_SUCCESS if initialization succeeded else mpe_Error
 */
void mpe_SI_Database_Clear(void)
{
    mpe_SiTableEntry *walker, *next;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_si_Database_Clear()\n");

    /* Clear the database entries */

    /* Use internal method release_si_entry() to delete service entries */
    for (walker = g_si_entry; walker; walker = next)
    {
        release_si_entry(walker);
        next = walker->next;
        mpeos_memFreeP(MPE_MEM_SI, walker);
    }

    /* Use internal method release_si_entry() to delete service entries */
    for (walker = g_si_spi_entry; walker; walker = next)
    {
        release_si_entry(walker);
        next = walker->next;
        mpeos_memFreeP(MPE_MEM_SI, walker);
    }

    /* delete mutex */
    mpe_mutexDelete(g_global_si_mutex);
    mpe_mutexDelete(g_global_si_list_mutex);
    mpe_mutexDelete(g_registration_list_mutex);
    mpe_mutexDelete(g_dsg_mutex);
    mpe_mutexDelete(g_HN_mutex);

    /* delete global condition object */
    mpe_condDelete(g_global_si_cond);

    /* delete SITP condition object */
    mpe_condDelete(g_SITP_cond);
}

mpe_Error mpe_siChanMapReset(void)
{
    mpe_SiTableEntry *walker, *next;
    /* Acquire global mutex */
    mpe_mutexAcquire(g_global_si_mutex);

    /* Use internal method release_si_entry() to delete service entries */
    for (walker = g_si_entry; walker; walker = next)
    {
        release_si_entry(walker);
        next = walker->next;
        mpeos_memFreeP(MPE_MEM_SI, walker);
    }
    g_si_entry = NULL;

    mpe_mutexRelease(g_global_si_mutex);

    return MPE_SI_SUCCESS;
}

/* NIT_CD */
/* Populate carrier frequencies from NIT_CD table */
/*
 Called by SITP when NIT_CDS is read.
 The index into carrier frequency table is 8 bits long.
 Hence the maxium entries are set to 255.
 */
mpe_Error mpe_siSetCarrierFrequencies(uint32_t frequency[], uint8_t offset,
        uint8_t length)
{
    uint8_t i, j;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "mpe_siSetCarrierFrequencies()\n");
    /* Parameter check */
    if ((frequency == NULL) || ((uint16_t) offset + length > MAX_FREQUENCIES
            + 1) || (length == 0))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    /* Set frequencies */
    for (i = offset, j = 0; j < length; i++, j++)
    {
        /*  Store frequencies in the global array to be looked up
         at the time of query. */
        g_frequency[i] = frequency[j];
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_SI,
                "mpe_siSetCarrierFrequencies() g_frequency[%d] = frequency[%d] (%d)\n",
                i, j, g_frequency[i]);
    }

    return MPE_SI_SUCCESS;
}

/* NIT_MM */
/* Populate modulation modes from NIT_MM table */
/*
 Called by SITP when NIT_MMS is read.
 The index into modulation mode table is 8 bits long.
 Hence the maxium entries for this table are set to 255.
 */
mpe_Error mpe_siSetModulationModes(mpe_SiModulationMode mode[], uint8_t offset,
        uint8_t length)
{
    uint8_t i, j;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "mpe_siSetModulationModes()\n");
    /* Parameter check */
    if ((mode == NULL) || ((uint16_t) offset + length > MAX_FREQUENCIES + 1)
            || (length == 0))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    /* Set modulation modes */
    for (i = offset, j = 0; j < length; i++, j++)
    {
        /*  Store modes in the global array to be looked up
         at the time of query. */
        g_mode[i] = mode[j];
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "mpe_siSetModulationModes() g_mode[%d] = mode[%d] (%d)\n", i,
                j, mode[j]);
    }

    return MPE_SI_SUCCESS;
}

/**
 Method called to sort the frequency array
 */
void quickSortArray(uint32_t numbers[], int array_size)
{
    q_sortElements(numbers, 0, array_size - 1);
}

/**
 Called by quickSortArray
 */
static void q_sortElements(uint32_t numbers[], uint32_t left, uint32_t right)
{
    uint32_t pivot, l_hold, r_hold;

    l_hold = left;
    r_hold = right;
    pivot = numbers[left];
    while (left < right)
    {
        while ((numbers[right] >= pivot) && (left < right))
            right--;
        if (left != right)
        {
            numbers[left] = numbers[right];
            left++;
        }
        while ((numbers[left] <= pivot) && (left < right))
            left++;
        if (left != right)
        {
            numbers[right] = numbers[left];
            right--;
        }
    }
    numbers[left] = pivot;
    pivot = left;
    left = l_hold;
    right = r_hold;
    if (left < pivot)
        q_sortElements(numbers, left, pivot - 1);
    if (right > pivot)
        q_sortElements(numbers, pivot + 1, right);
}

static void printSortedArray(uint32_t *array, const int arraySize)
{
#if 0 /* TODO: for debugging? */
    int i = 0;
    for (i=0; i<arraySize; i++)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,"frequency[%d]: %d... \n", i, *array);
        array++;
    }
#else
    MPE_UNUSED_PARAM(array);
    MPE_UNUSED_PARAM(arraySize);
#endif
}

/*
 static mpe_Bool findFrequencyInCDS (uint32_t frequency)
 {
 uint32_t i;

 for(i=0; i<MAX_FREQUENCIES+1; i++)
 {
 if(g_frequency[i] == frequency)
 {
 MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "findFrequencyInCDS() found frequency %d.\n", frequency);
 return true;
 }
 }

 MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "findFrequencyInCDS() Did not find frequency %d in the CDS table.\n", frequency);
 return false;
 }
 */

static char *si_status_strings[] =
{ "Not Available", "Pending", "Available" };

/**
 Initialize a newly created transport stream entry with default values
 */
static mpe_Error init_transport_stream_entry(
        mpe_SiTransportStreamEntry *ts_entry, uint32_t Frequency)
{
    mpe_Error err = MPE_SUCCESS;

    if (ts_entry == NULL)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    ts_entry->state = TSENTRY_NEW;
    ts_entry->frequency = Frequency;
    ts_entry->modulation_mode = MPE_SI_MODULATION_UNKNOWN;
    ts_entry->ts_id = TSID_UNKNOWN;
    ts_entry->ts_name = NULL;
    ts_entry->description = NULL;
    ts_entry->ptime_transport_stream = gtime_transport_stream;
    ts_entry->service_count = 0;
    ts_entry->program_count = 0;
    ts_entry->visible_service_count = 0;
    ts_entry->siStatus = SI_NOT_AVAILABLE;
    ts_entry->pat_version = INIT_TABLE_VERSION; // default version
    ts_entry->pat_crc = 0;
    ts_entry->cvct_version = INIT_TABLE_VERSION; // default version
    ts_entry->cvct_crc = 0;
    ts_entry->check_crc = FALSE;
    ts_entry->pat_reference = 0;
    ts_entry->pat_program_list = NULL;
    ts_entry->next = NULL;
	ts_entry->source_type = MPE_SOURCE_TYPE_UNKNOWN;
    ts_entry->pat_byte_array = NULL;
    ts_entry->services = NULL;
    ts_entry->programs = NULL;
    ts_entry->list_lock = NULL;

    if (Frequency == MPE_SI_OOB_FREQUENCY)
    {
		ts_entry->source_type = MPE_SOURCE_TYPE_OOB;
    }
    else if (Frequency == MPE_SI_HN_FREQUENCY)
    {
        ts_entry->source_type = MPE_SOURCE_TYPE_HN_STREAM;
    }
    else if (Frequency == MPE_SI_DSG_FREQUENCY)
    {
        ts_entry->source_type = MPE_SOURCE_TYPE_DSG;
    }
    else
    {
		ts_entry->source_type = MPE_SOURCE_TYPE_IB;
    }
    ts_entry->services = llist_create();
    ts_entry->programs = llist_create();

    err = mpe_mutexNew(&ts_entry->list_lock);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                "init_transport_stream_entry() Mutex creation failed, returning MPE_EMUTEX\n");

        if (ts_entry->services)
            llist_free(ts_entry->services);
        if (ts_entry->programs)
            llist_free(ts_entry->programs);

        return MPE_EMUTEX;
    }

    return MPE_SI_SUCCESS;
}

static void release_transport_stream_entry(mpe_SiTransportStreamEntry *ts_entry)
{
    mpe_SiTransportStreamEntry **this_list;

    // Make sure all complex data items are accounted for, lest we leak memory.
    if (ts_entry == NULL)
    {
        MPE_LOG(MPE_LOG_FATAL, MPE_MOD_SI,
                "Can't release null TransportStreamEntry!!");
        return;
    }

    if (ts_entry->ts_name != NULL)
        mpeos_memFreeP(MPE_MEM_SI, ts_entry->ts_name);

    if (ts_entry->description != NULL)
        mpeos_memFreeP(MPE_MEM_SI, ts_entry->description);

    if (ts_entry->pat_reference != 0)
    {
        // delete the saved references
        mpe_SiPatProgramList *next, *walker_saved_pat_program =
                (mpe_SiPatProgramList *) ts_entry->pat_reference;
        while (walker_saved_pat_program)
        {
            next = walker_saved_pat_program->next;
            mpeos_memFreeP(MPE_MEM_SI, walker_saved_pat_program);
            walker_saved_pat_program = next;
        }
    }

    if (ts_entry->pat_program_list != NULL)
    {
        // delete the saved references
        mpe_SiPatProgramList *next, *walker_saved_pat_program =
                ts_entry->pat_program_list;
        while (walker_saved_pat_program)
        {
            next = walker_saved_pat_program->next;
            mpeos_memFreeP(MPE_MEM_SI, walker_saved_pat_program);
            walker_saved_pat_program = next;
        }
    }

    if (ts_entry->pat_byte_array != NULL)
    {
        mpeos_memFreeP(MPE_MEM_SI, ts_entry->pat_byte_array);
    }

    if (llist_cnt(ts_entry->programs) != 0)
    {
        MPE_LOG(MPE_LOG_FATAL, MPE_MOD_SI, "deleting non-empty list!!");
    }
    llist_free(ts_entry->programs);

    if (llist_cnt(ts_entry->services) != 0)
    {
        MPE_LOG(MPE_LOG_FATAL, MPE_MOD_SI, "deleting non-empty list!!");
    }
    llist_free(ts_entry->services);

    mpe_mutexDelete(ts_entry->list_lock);

    // Remove from linked list.
    // We should be holding the writerLock at a level above this, so it's
    // safe to do this last.

    if (ts_entry->source_type == MPE_SOURCE_TYPE_OOB)
    {
        this_list = &g_si_oob_ts_entry;
    }
    else if(ts_entry->source_type == MPE_SOURCE_TYPE_HN_STREAM) 
    {
        this_list = &g_si_HN_ts_entry;
    }
    else if(ts_entry->source_type == MPE_SOURCE_TYPE_DSG)
    {
        this_list = &g_si_dsg_ts_entry;
    }
    else
    {
        this_list = &g_si_ts_entry;
    }

    if (*this_list == ts_entry)
    {
        *this_list = ts_entry->next;
        ts_entry->next = NULL;
    }
    else
    {
        mpe_SiTransportStreamEntry *walker = *this_list;
        while ((walker != NULL) && (walker->next != ts_entry))
        { // Find the previous entry on list.
            walker = walker->next;
        }
        if (walker != NULL)
        { // unlink
            walker->next = ts_entry->next;
            ts_entry->next = NULL;
        }
        else
        {
            MPE_LOG(MPE_LOG_FATAL, MPE_MOD_SI,
                    "Transport Stream Entry was NOT in linked list!!");
        }
    }
    MPE_LOG(MPE_LOG_TRACE7, MPE_MOD_SI, "ts freed @ %p\n", ts_entry);
    mpeos_memFreeP(MPE_MEM_SI, ts_entry);
}

/**
 Initialize a newly created program info structure with default values
 */
static mpe_Error init_program_info(mpe_SiProgramInfo *pi)
{
    volatile mpe_Error retVal = MPE_SI_SUCCESS;

    if (pi == NULL)
    {
        retVal = MPE_SI_INVALID_PARAMETER;
    }
    else
    {
        // Start struct in a known state.
        pi->pmt_status = PMT_NOT_AVAILABLE;
        pi->program_number = (uint32_t) - 1;
        pi->pmt_pid = (uint32_t) - 1;
        pi->pmt_version = INIT_TABLE_VERSION; // default version
        pi->crc_check = false;
        pi->pmt_crc = 0;
        pi->saved_pmt_ref = NULL;
        pi->pmt_byte_array = NULL;
        pi->number_outer_desc = 0;
        pi->outer_descriptors = NULL;
        pi->number_elem_streams = 0;
        pi->elementary_streams = NULL;
        pi->pcr_pid = (uint32_t) - 1;
        pi->services = llist_create();
        pi->service_count = 0;
        pi->stream = NULL;
    }
    return retVal;
}

static void release_program_info_entry(mpe_SiProgramInfo *pi)
{
    if (pi->saved_pmt_ref != NULL)
    {
        if (pi->saved_pmt_ref->outer_descriptors_ref != 0)
        {
            release_descriptors(
                    (mpe_SiMpeg2DescriptorList*) pi->outer_descriptors);
            release_descriptors(
                    (mpe_SiMpeg2DescriptorList*) pi->saved_pmt_ref->outer_descriptors_ref);
        }
        while (pi->saved_pmt_ref->elementary_streams_ref != 0)
        {
            mpe_SiElementaryStreamList *elem_stream_list = (mpe_SiElementaryStreamList *)pi->saved_pmt_ref->elementary_streams_ref;
            remove_elementaryStream((mpe_SiElementaryStreamList **)&(pi->saved_pmt_ref->elementary_streams_ref), elem_stream_list);
            delete_elementary_stream(elem_stream_list);
            (pi->saved_pmt_ref->number_elem_streams)--;
        }
        mpeos_memFreeP(MPE_MEM_SI, pi->saved_pmt_ref);
        pi->saved_pmt_ref = NULL;
    }
    if (pi->pmt_byte_array != NULL)
        mpeos_memFreeP(MPE_MEM_SI, pi->pmt_byte_array);
    if (pi->outer_descriptors != NULL)
        release_descriptors(pi->outer_descriptors);
    while (pi->elementary_streams != NULL)
    {
        mpe_SiElementaryStreamList *elem_stream_list = pi->elementary_streams;
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI, "<%s> release elem stream, %p\n", __FUNCTION__, elem_stream_list);
        remove_elementaryStream(&(pi->elementary_streams), elem_stream_list);
        delete_elementary_stream(elem_stream_list);
        (pi->number_elem_streams)--;
    }
    pi->elementary_streams = NULL;
    llist_free(pi->services);
}

/**
 Local method to update transport stream handles when NIT/SVCT are updated.
 */
static void update_transport_stream_handles()
{
    mpe_SiTableEntry *service_walker = NULL;
    mpe_SiTransportStreamEntry *oldTransport;

    // Re-validate where services are attached, since the CDS changed.  This will
    // make sure that services are correctly assigned to transport streams when the
    // freq plan is updated.
    //
    // Changes in in the SVCT will be processed at the time the new SVCT is parsed,
    // so there is no need to call this routine after anything but CDS NIT updates.
    //

    service_walker = g_si_entry;

    while (service_walker != NULL)
    {
        if (service_walker->ts_handle != MPE_SI_INVALID_HANDLE)
        { // Active service, not SPI without actual locator
            if ((!service_walker->isAppType) &&
                (service_walker->hn_stream_session == 0))
            {
                oldTransport
                        = (mpe_SiTransportStreamEntry *) service_walker->ts_handle;
                if ((g_frequency[service_walker->freq_index] != 0)
                        && (oldTransport->frequency
                                != g_frequency[service_walker->freq_index]))
                {
                    (void) mpe_siUpdateFreqProgNumByServiceHandle(
                            (mpe_SiServiceHandle) service_walker,
                            g_frequency[service_walker->freq_index],
                            service_walker->program != NULL ? service_walker->program->program_number
                                    : (uint32_t) - 1,
                            g_mode[service_walker->mode_index]);
                    // Signal SERVICE_DETAILS_UPDATE event
                    {
                    	send_si_event(MPE_SI_EVENT_SERVICE_DETAILS_UPDATE, (mpe_SiServiceHandle)service_walker, MPE_SI_CHANGE_TYPE_MODIFY);
                    }
                    // TODO: Signal Transport stream update event?
                }
            }
        }
        service_walker = service_walker->next;
    }
}

void mpe_siSetTSIdStatusNotAvailable(mpe_SiTransportHandle ts_handle)
{
    mpe_SiTransportStreamEntry *ts_entry =
            (mpe_SiTransportStreamEntry *) ts_handle;
    ts_entry->siStatus = SI_NOT_AVAILABLE;
}

void mpe_siSetTSIdStatusAvailableShortly(mpe_SiTransportHandle ts_handle)
{
    mpe_SiTransportStreamEntry *ts_entry =
            (mpe_SiTransportStreamEntry *) ts_handle;

    if (ts_entry->siStatus == SI_NOT_AVAILABLE)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mpe_siSetTSIdStatusAvailableShortly>  freq: %d \n",
                ts_entry->frequency);
        ts_entry->siStatus = SI_AVAILABLE_SHORTLY;
    }
}

void mpe_siRevertTSIdStatus(mpe_SiTransportHandle ts_handle)
{
    mpe_SiTransportStreamEntry *ts_entry =
            (mpe_SiTransportStreamEntry *) ts_handle;

    // If the status was previously set to available shortly then reset to not available
    if (ts_entry->siStatus == SI_AVAILABLE_SHORTLY)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mpe_siRevertTSIdStatus>  freq: %d \n", ts_entry->frequency);
        ts_entry->siStatus = SI_NOT_AVAILABLE;
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siRevertTSIdStatus>  freq: %d now %s\n", ts_entry->frequency,
            si_status_strings[ts_entry->siStatus]);
}

/**
 This method is called by SITP when PAT has been acquired and parsed
 to set the TSId for a transport stream identified by the frequency.
 */
void mpe_siSetTSIdForTransportStream(mpe_SiTransportStreamHandle ts_handle,
        uint32_t tsId)
{
    mpe_SiTransportStreamEntry *ts_entry =
            (mpe_SiTransportStreamEntry *) ts_handle;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siSetTSIdForTransportStream> freq:%d, tsId:%d\n",
            ts_entry->frequency, tsId);

    ts_entry->ts_id = tsId;
    ts_entry->siStatus = SI_AVAILABLE; /* indicates a valid tsId being set */
}

/**
 Method called by SITP to set the PAT version.
 */
void mpe_siSetPATVersionForTransportStream(
        mpe_SiTransportStreamHandle ts_handle, uint32_t pat_version)
{
    mpe_SiTransportStreamEntry *ts_entry =
            (mpe_SiTransportStreamEntry *) ts_handle;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siSetPATVersionForTransportStream> freq:%d, version:%d\n",
            ts_entry->frequency, pat_version);

    if (ts_entry->pat_version != pat_version)
    {
        // No need to check crc
        ts_entry->check_crc = FALSE;
    }
    else
    {
        // version same, check crc
        ts_entry->check_crc = TRUE;
    }

    // Save exisiting pat program list as a reference
    if (ts_entry->pat_program_list != NULL)
    {
        ts_entry->pat_reference = (uint32_t) ts_entry->pat_program_list;
    }

    // Un-couple pat programs from the transport stream
    // Allow new pat programs to be added
    ts_entry->pat_program_list = NULL;
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siSetPATVersionForTransportStream> done..\n");
    ts_entry->pat_version = pat_version;
}

/**
 *  Method called by SITP to set the CVCT version.
 */
void mpe_siSetCVCTVersionForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle, uint32_t cvct_version)
{
    mpe_SiTransportStreamEntry * ts_entry =
            (mpe_SiTransportStreamEntry*) ts_handle;

    if (ts_handle == MPE_SI_INVALID_HANDLE)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI,
                "<mpe_siSetCVCTVersionForTransportStreamHandle> MPE_SI_INVALID_PARAMETER\n");
    }
    else
    {
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_SI,
                "<mpe_siSetCVCTVersionForTransportStream> ts_handle: 0x%x, version: %d\n",
                ts_handle, cvct_version);
        ts_entry->cvct_version = cvct_version;
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mpe_siSetCVCTVersionForTransportStream> done..\n");
    }
}
/**
 *  Method called by SITP to set the CVCT crc.
 */
void mpe_siSetCVCTCRCForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle, uint32_t cvct_crc)
{
    mpe_SiTransportStreamEntry * ts_entry =
            (mpe_SiTransportStreamEntry*) ts_handle;

    if (ts_handle == MPE_SI_INVALID_HANDLE)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI,
                "<mpe_siSetCVCTCRCForTransportStreamHandle> MPE_SI_INVALID_PARAMETER\n");
    }
    else
    {
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_SI,
                "<mpe_siSetCVCTCRCForTransportStream> ts_handle: 0x%x, crc: %d\n",
                ts_handle, cvct_crc);
        ts_entry->cvct_crc = cvct_crc;
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mpe_siSetCVCTCRCForTransportStream> done..\n");
    }
}

/**
 Method called by SITP to set program_number, pmt_pid pair found in the PAT.
 Each PAT may contain one or more such pairs. Hence they are added in the form
 of linked list.
 */
mpe_Error mpe_siSetPATProgramForTransportStream(
        mpe_SiTransportStreamHandle ts_handle, uint32_t program_number,
        uint32_t pmt_pid)
{
    mpe_SiTransportStreamEntry *ts_entry =
            (mpe_SiTransportStreamEntry *) ts_handle;
    mpe_SiPatProgramList *new_pat_program = NULL;
    mpe_SiPatProgramList *walker_pat_program = NULL;

    if (ts_handle == MPE_SI_INVALID_HANDLE)
    {
        MPE_LOG(MPE_LOG_FATAL, MPE_MOD_SI,
                "<mpe_siSetPATProgramForTransportStream> invalid Transport Stream!!\n");
        return MPE_SI_INVALID_PARAMETER;
    }

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siSetPATProgramForTransportStream> freq:%d program_number:%d pmt_pid:%d\n",
            ts_entry->frequency, program_number, pmt_pid);

    if (MPE_SI_SUCCESS != mpeos_memAllocP(MPE_MEM_SI,
            sizeof(mpe_SiPatProgramList), (void **) &(new_pat_program)))
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                "Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");

        return MPE_SI_OUT_OF_MEM;
    }
    new_pat_program->programNumber = program_number;
    new_pat_program->pmt_pid = pmt_pid;
    new_pat_program->matched = FALSE;
    new_pat_program->next = NULL;

    if (ts_entry->pat_program_list == NULL)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mpe_siSetPATProgramForTransportStream> inserted patprogram at the top\n");
        ts_entry->pat_program_list = new_pat_program;
    }
    else
    {
        walker_pat_program = ts_entry->pat_program_list;
        while (walker_pat_program)
        {
            if (walker_pat_program->next == NULL)
            {
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                        "<mpe_siSetPATProgramForTransportStream> inserted patprogram in the list\n");
                walker_pat_program->next = new_pat_program;
                break;
            }
            walker_pat_program = walker_pat_program->next;
        }
    }

    return MPE_SI_SUCCESS;
}

/**
 Method called by SITP to set PAT CRC for a transport stream identified by frequency.
 */
void mpe_siSetPATCRCForTransportStream(mpe_SiTransportStreamHandle ts_handle,
        uint32_t crc)
{
    mpe_SiTransportStreamEntry *ts_entry =
            (mpe_SiTransportStreamEntry *) ts_handle;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siSetPATCRCForTransportStream> freq:%d, crc:%d\n",
            ts_entry->frequency, crc);

    if ((ts_entry->pat_crc != 0) && (ts_entry->check_crc == TRUE))
    {
        // version same, crc different
        if (ts_entry->pat_crc != crc)
        {
            mpe_SiPatProgramList *walker_saved_pat_program, *next;

            // If a match is found they are marked TRUE in the old PAT list.
            // Left over programs in the old list are 'removed' (signal PMT 'REMOVE' for these)
            // Left over programs in the new list are 'added' (signal PMT 'ADD' - handled by SITP)

            compare_ts_pat_programs(ts_entry->pat_program_list,
                    (mpe_SiPatProgramList *) ts_entry->pat_reference, FALSE);

            // For programs removed from the old PAT, signal event
            signal_program_removed_event(ts_entry,
                    (mpe_SiPatProgramList *) ts_entry->pat_reference);

            // delete the saved PAT reference
            walker_saved_pat_program
                    = (mpe_SiPatProgramList *) ts_entry->pat_reference;
            while (walker_saved_pat_program)
            {
                next = walker_saved_pat_program->next;
                mpeos_memFreeP(MPE_MEM_SI, walker_saved_pat_program);
                walker_saved_pat_program = next;
            }
        }
        else // Version is same, crc is also same
        {
            // Free the newly acquired pat programs, restore the saved list
            // No new events are generated in this case
            mpe_SiPatProgramList *walker_new_pat_program = NULL;
            mpe_SiPatProgramList *next = NULL;
            walker_new_pat_program
                    = (mpe_SiPatProgramList *) ts_entry->pat_program_list;
            while (walker_new_pat_program)
            {
                next = walker_new_pat_program->next;
                mpeos_memFreeP(MPE_MEM_SI, walker_new_pat_program);
                walker_new_pat_program = next;
            }

            ts_entry->pat_program_list
                    = (mpe_SiPatProgramList *) ts_entry->pat_reference;
        }
    }
    else // Version different, check_crc is 'false'
    {
        mpe_SiPatProgramList *walker_saved_pat_program, *next;

        if (ts_entry->pat_reference) // If there is a saved reference
        {
            // Compare newly acquired pat program list with existing program list
            compare_ts_pat_programs(ts_entry->pat_program_list,
                    (mpe_SiPatProgramList *) ts_entry->pat_reference, FALSE);

            // Existing PAT program list
            // For programs removed from the old PAT, signal event
            signal_program_removed_event(ts_entry,
                    (mpe_SiPatProgramList *) ts_entry->pat_reference);

            // delete the saved references
            walker_saved_pat_program
                    = (mpe_SiPatProgramList *) ts_entry->pat_reference;
            while (walker_saved_pat_program)
            {
                next = walker_saved_pat_program->next;
                mpeos_memFreeP(MPE_MEM_SI, walker_saved_pat_program);
                walker_saved_pat_program = next;
            }
        }
        else // Initial acquisition, no saved reference
        {
            // For newly added programs event notification is handled when
            // SITP calls mpe_siNotifyTableChanged() with a table type of PMT
            // and change type of ADD
        }
    }

    ts_entry->pat_crc = crc;
    ts_entry->check_crc = FALSE;
    ts_entry->siStatus = SI_AVAILABLE;
    ts_entry->pat_reference = 0;
}
/*
 Internal method to handle signaling PAT program removed event
 */
static void signal_program_removed_event(mpe_SiTransportStreamEntry *ts_entry,
        mpe_SiPatProgramList *pat_list)
{
    mpe_SiPatProgramList *walker_saved_pat_program =
            (mpe_SiPatProgramList *) pat_list;

    // Existing PAT program list
    while (walker_saved_pat_program)
    {
        if (walker_saved_pat_program->matched == FALSE)
        {
            // program removed from the old PAT, send event
            mpe_SiProgramHandle prog_handle;

            if (mpe_siGetProgramEntryFromTransportStreamEntry(
                    (mpe_SiTransportStreamHandle) ts_entry,
                    walker_saved_pat_program->programNumber, &prog_handle)
                    == MPE_SI_SUCCESS)
            {
                mpe_SiServiceHandle service_handle;
                publicSIIterator iter;
                mpe_SiProgramInfo *pi = (mpe_SiProgramInfo *) prog_handle;
                // reset PMT status, and PMT version
                pi->pmt_status = PMT_NOT_AVAILABLE;
                pi->pmt_version = INIT_TABLE_VERSION;

                // Need to loop through services attached to the program here, and notify each one.
                // Now more than one service can point to a program.
                service_handle = mpe_siFindFirstServiceFromProgram(
                        (mpe_SiProgramHandle) prog_handle, (SIIterator*) &iter);

                while (service_handle != MPE_SI_INVALID_HANDLE)
                {
                    // Signal these program PMTs as removed so as to unblock any waiting callers
                    mpe_siNotifyTableChanged(IB_PMT, MPE_SI_CHANGE_TYPE_REMOVE,
                            service_handle);

                    service_handle = mpe_siGetNextServiceFromProgram(
                            (SIIterator*) &iter);
                }
            }
        }
        walker_saved_pat_program = walker_saved_pat_program->next;
    }
}

/**
 Internal method used to compare an existing PAT program list with a new
 PAT program list. Program numbers found in both PATs are marked as such.
 This is done mainly to identify newly added programs and programs that
 existed in the old PAT but are dropped in the new version.
 */
static void compare_ts_pat_programs(mpe_SiPatProgramList *new_pat_list,
        mpe_SiPatProgramList *saved_pat_list, mpe_Bool setNewProgramFlag)
{
    mpe_SiPatProgramList *walker_saved_pat_program;
    mpe_SiPatProgramList *walker_new_pat_program = new_pat_list;

    if ((new_pat_list == NULL) || (saved_pat_list == NULL))
        return;

    // program numbers in the newly acquired PAT are compared
    // with program numbers in the saved PAT
    while (walker_new_pat_program)
    {
        walker_saved_pat_program = saved_pat_list;
        while (walker_saved_pat_program)
        {
            if ((walker_new_pat_program->programNumber
                    == walker_saved_pat_program->programNumber)
                    && (walker_new_pat_program->matched != TRUE))
            {
                walker_saved_pat_program->matched = TRUE;
                if (setNewProgramFlag)
                    walker_new_pat_program->matched = TRUE;
            }
            walker_saved_pat_program = walker_saved_pat_program->next;
        }
        walker_new_pat_program = walker_new_pat_program->next;
    }
}

/*  OC specific SI query methods */
/**
 * Retrieve the elementary source (Pid, tsId) given the association_tag value.
 *
 * <i>mpe_siGetPidByAssociationTag</i>
 *
 * @param assoc_tag Association tag value found in 'MPE_SI_DESC_ASSOCIATION_TAG' descriptor in the (inner)PMT descriptor loop
 * @param oc_filt_source output param to populate the elementary Pid and transport stream Id
 *
 * @return MPE_SI_SUCCESS if successfully located the tag and retrieved the elementary source, else
 *          return appropriate mpe_Error
 *
 *  This method is called by the Object carousal module to retrieve the elementary stream
 *  source info given (16 bit) association tag value. Search for the given association tag is
 *  performed in the given service entry.
 *
 */
mpe_Error mpe_siGetPidByAssociationTag(mpe_SiServiceHandle service_handle,
        uint16_t assoc_tag, uint32_t *targetPid)
{
    mpe_Error err = MPE_SI_SUCCESS;
    int found = 0;
    uint16_t *data_ptr;
    mpe_SiElementaryStreamList *elem_stream_list = NULL;
    mpe_SiMpeg2DescriptorList *desc_list_walker = NULL;
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;
    mpe_SiProgramInfo *pi = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetPidByAssociationTag> assoc_tag 0x%x\n", assoc_tag);

    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (targetPid == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }
    else
    {
        /* SI DB has not been populated yet */
        if (g_si_entry == NULL)
        {
            err = MPE_SI_NOT_AVAILABLE;
        }
        else
        {
            *targetPid = 0;
            pi = si_entry->program;
            if (pi == NULL)
            {
                mpe_SiTransportStreamEntry *ts =
                        (mpe_SiTransportStreamEntry *) si_entry->ts_handle;
                if (ts != NULL)
                {
                    if (ts->modulation_mode != MPE_SI_MODULATION_QAM_NTSC)
                        err = MPE_SI_INVALID_PARAMETER;
                    else
                        err = MPE_SI_NOT_FOUND;
                }
                else
                {
                    err = MPE_SI_NOT_AVAILABLE; // SPI service?
                }
            }
            else
            {
                /* If PSI is in the process of being acquired return from here
                 until it is updated. */
                if (pi->pmt_status == PMT_NOT_AVAILABLE)
                {
                    err = MPE_SI_NOT_AVAILABLE;
                }
                else if (pi->pmt_status == PMT_AVAILABLE_SHORTLY)
                {
                    err = MPE_SI_NOT_AVAILABLE_YET;
                }
                else
                {

                    /*
                     Search for the given association tag
                     in the elemtary stream list descriptor loop (MPE_SI_DESC_ASSOCIATION_TAG descriptor)
                     for the given service handle.
                     */
                    /*  Search could be made faster if searched only for MPE_SI_COMP_TYPE_DATA
                     type elementary streams. Skip audio, video etc.
                     */
                    elem_stream_list = pi->elementary_streams;
                    while (elem_stream_list && !found)
                    {
                        desc_list_walker = elem_stream_list->descriptors;
                        while (desc_list_walker)
                        {
                            if (desc_list_walker->tag
                                    == MPE_SI_DESC_ASSOCIATION_TAG)
                            {
                                /* First 2 bytes of the descriptor content is the association tag value */
                                data_ptr
                                        = (uint16_t*) desc_list_walker->descriptor_content;
                                if (ntohs(*data_ptr) == assoc_tag)
                                {
                                    /* hardcode streamSourceType to IB, fix?? */
                                    *targetPid
                                            = elem_stream_list->elementary_PID;
                                    MPE_LOG(
                                            MPE_LOG_DEBUG,
                                            MPE_MOD_SI,
                                            "<mpe_siGetPidByAssociationTag> found pid 0x%x\n",
                                            *targetPid);
                                    found = 1;
                                    break;
                                }
                            }

                            desc_list_walker = desc_list_walker->next;
                        }

                        elem_stream_list = elem_stream_list->next;
                    }

                    /* If not found return appropriate error */
                    if (!found)
                    {
                        err = MPE_SI_NOT_FOUND;
                    }
                }
            }
        }
    }
    return err;
}

/**
 * Retrieve the elementary source (Pid, tsId) given the carousal_id value.
 *
 * <i>mpe_siGetPidByCarouselID()</i>
 *
 *
 * @param carousel_id carousel Id value found in 'MPE_SI_DESC_CAROUSEL_ID' descriptor in the (inner)PMT descriptor loop
 * @param oc_filt_source output param to populate the elementary Pid and transport stream Id
 *
 * @return MPE_SI_SUCCESS if successfully located the carousel_id and retrieved the elementary source, else
 *          return appropriate mpe_Error
 *
 *  This method is called by the Object carousal module to retrieve the elementary stream
 *  source info given (32 bit) carousel id value.
 *
 */
mpe_Error mpe_siGetPidByCarouselID(mpe_SiServiceHandle service_handle,
        uint32_t carousel_id, uint32_t *targetPid)
{
    mpe_Error err = MPE_SI_SUCCESS;
    int found = 0;
    uint32_t *data_ptr;
    mpe_SiElementaryStreamList *elem_stream_list = NULL;
    mpe_SiMpeg2DescriptorList *desc_list_walker = NULL;
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;
    mpe_SiProgramInfo *pi = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetPidByCarouselID> for carousel_id:0x%x\n", carousel_id);

    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (targetPid == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }
    else
    {
        /* SI DB has not been populated yet */
        if (g_si_entry == NULL)
        {
            err = MPE_SI_NOT_AVAILABLE;
        }
        else
        {
            *targetPid = 0;
            pi = si_entry->program;
            if (pi == NULL)
            {
                mpe_SiTransportStreamEntry *ts =
                        (mpe_SiTransportStreamEntry *) si_entry->ts_handle;
                if (ts != NULL)
                {
                    if (ts->modulation_mode != MPE_SI_MODULATION_QAM_NTSC)
                        err = MPE_SI_INVALID_PARAMETER;
                    else
                        err = MPE_SI_NOT_FOUND;
                }
                else
                {
                    err = MPE_SI_NOT_AVAILABLE; // SPI service?
                }
            }
            else
            {
                /* If PSI is in the process of being acquired return from here
                 until it is updated. */
                if (pi->pmt_status == PMT_NOT_AVAILABLE)
                {
                    err = MPE_SI_NOT_AVAILABLE;
                }
                else if (pi->pmt_status == PMT_AVAILABLE_SHORTLY)
                {
                    err = MPE_SI_NOT_AVAILABLE_YET;
                }
                else
                {
                    /*
                     Search for the given carousel id tag
                     in the elemtary stream list descriptor loop
                     */
                    elem_stream_list = pi->elementary_streams;
                    while (elem_stream_list && !found)
                    {
                        desc_list_walker = elem_stream_list->descriptors;
                        while (desc_list_walker)
                        {
                            if (desc_list_walker->tag
                                    == MPE_SI_DESC_CAROUSEL_ID)
                            {
                                /* First 4 bytes of the descriptor content is the carousel id value */
                                data_ptr
                                        = (uint32_t*) desc_list_walker->descriptor_content;
                                if (ntohl(*data_ptr) == carousel_id)
                                {
                                    *targetPid
                                            = elem_stream_list->elementary_PID;
                                    MPE_LOG(
                                            MPE_LOG_DEBUG,
                                            MPE_MOD_SI,
                                            "<mpe_siGetPidByCarouselID> found pid: 0x%x\n",
                                            *targetPid);
                                    found = 1;
                                    break;
                                }
                            }
                            desc_list_walker = desc_list_walker->next;
                        }

                        elem_stream_list = elem_stream_list->next;
                    }

                    /* If not found return appropriate error */
                    if (!found)
                    {
                        err = MPE_SI_NOT_FOUND;
                    }
                }
            }
        }
    }
    return err;
}

/**
 * Retrieve component Pid for the given service handle and component tag
 *
 * <i>mpe_siGetPidByComponentTag()</i>
 *
 * @param service_handle si handle to get the component pid from
 * @param component_tag is the input component tag
 * @param component_pid is the output param to populate component pid
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetPidByComponentTag(mpe_SiServiceHandle service_handle,
        int component_tag, uint32_t *component_pid)
{
    mpe_Error err = MPE_SI_SUCCESS;
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;
    mpe_SiProgramInfo *pi;
    mpe_SiElementaryStreamList *elem_stream_list = NULL;
    mpe_SiMpeg2DescriptorList *desc_list_walker = NULL;
    uint8_t *buffer;
    int found = 0;

    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (component_pid == NULL))
    {
        err = MPE_SI_INVALID_PARAMETER;
    }
    else
    {
        /* SI DB has not been populated yet */
        if (g_si_entry == NULL)
        {
            err = MPE_SI_NOT_AVAILABLE;
        }
        else
        {
            *component_pid = 0;
            pi = si_entry->program;
            if (pi == NULL)
            {
                mpe_SiTransportStreamEntry *ts =
                        (mpe_SiTransportStreamEntry *) si_entry->ts_handle;
                if (ts != NULL)
                {
                    if (ts->modulation_mode != MPE_SI_MODULATION_QAM_NTSC)
                        err = MPE_SI_INVALID_PARAMETER;
                    else
                        err = MPE_SI_NOT_FOUND;
                }
                else
                {
                    err = MPE_SI_NOT_AVAILABLE; // SPI service?
                }
            }
            else
            {
                /* If PSI is in the process of being acquired return from here
                 until it is updated. */
                if (pi->pmt_status == PMT_NOT_AVAILABLE)
                {
                    err = MPE_SI_NOT_AVAILABLE;
                }
                else if (pi->pmt_status == PMT_AVAILABLE_SHORTLY)
                {
                    err = MPE_SI_NOT_AVAILABLE_YET;
                }
                else
                {
                    elem_stream_list = pi->elementary_streams;
                    while (elem_stream_list && !found)
                    {
                        desc_list_walker = elem_stream_list->descriptors;
                        while (desc_list_walker)
                        {
                            if (desc_list_walker->tag
                                    == MPE_SI_DESC_STREAM_IDENTIFIER_DESCRIPTOR)
                            {
                                buffer = desc_list_walker->descriptor_content;
                                if (*buffer == component_tag)
                                {
                                    *component_pid
                                            = elem_stream_list->elementary_PID;
                                    MPE_LOG(
                                            MPE_LOG_DEBUG,
                                            MPE_MOD_SI,
                                            "<mpe_siGetPidByComponentTag> found pid: 0x%x\n",
                                            *component_pid);
                                    found = 1;
                                    break;
                                }
                            }
                            desc_list_walker = desc_list_walker->next;
                        }

                        elem_stream_list = elem_stream_list->next;
                    }

                    if (!found)
                    {
                        err = MPE_SI_NOT_FOUND;
                    }
                }
            }
        }
    }
    return err;
}

/**
 * Retrieve the program number for a deferred associtaion tag.
 *
 * <i>mpe_siGetProgramNumberByDeferredAssociationTag()</i>
 *
 * @param service_handle si handle to get the component pid from
 * @param component_tag is the input component tag
 * @param programNumber is buffer for the returned programNumber
 *
 * @return MPE_SI_SUCCESS if successfully located the program number and updated programNumber,
 *         else return appropriate mpe_Error and programNumber is not updated
 *
 */
mpe_Error mpe_siGetProgramNumberByDeferredAssociationTag(
        mpe_SiServiceHandle service_handle, uint16_t tag,
        uint32_t *programNumber)
{
    mpe_Error err = MPE_SI_SUCCESS;
    mpe_SiMpeg2DescriptorList *desc_list;
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;
    mpe_SiProgramInfo *pi = NULL;
    uint8_t *tagPtr;
    uint8_t *contentPtr;
    uint8_t loopLength;
    uint16_t temp;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetProgramNumberByDeferredAssociationTag> %08x\n", tag);

    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (programNumber == NULL))
    {
        err = MPE_SI_INVALID_PARAMETER;
    }
    else
    {
        /* SI DB has not been populated yet */
        if (g_si_entry == NULL)
        {
            err = MPE_SI_NOT_AVAILABLE;
        }
        else
        {
            *programNumber = 0;
            pi = si_entry->program;
            if (pi == NULL)
            {
                mpe_SiTransportStreamEntry *ts =
                        (mpe_SiTransportStreamEntry *) si_entry->ts_handle;
                if (ts != NULL)
                {
                    if (ts->modulation_mode != MPE_SI_MODULATION_QAM_NTSC)
                        err = MPE_SI_INVALID_PARAMETER;
                    else
                        err = MPE_SI_NOT_FOUND;
                }
                else
                {
                    err = MPE_SI_NOT_AVAILABLE; // SPI service?
                }
            }
            else
            {
                /* If PSI is in the process of being acquired return from here
                 until it is updated. */
                if (pi->pmt_status == PMT_NOT_AVAILABLE)
                {
                    MPE_LOG(
                            MPE_LOG_DEBUG,
                            MPE_MOD_SI,
                            "<mpe_siGetProgramNumberByDeferredAssociationTag> pi: 0x%p, PMT status: PMT_NOT_AVAILABLE\n",
                            pi);
                    err = MPE_SI_NOT_AVAILABLE;
                }
                else if (pi->pmt_status == PMT_AVAILABLE_SHORTLY)
                {
                    MPE_LOG(
                            MPE_LOG_DEBUG,
                            MPE_MOD_SI,
                            "<mpe_siGetProgramNumberByDeferredAssociationTag> pi: 0x%p, PMT status: PMT_AVAILABLE_SHORTLY\n",
                            pi);
                    err = MPE_SI_NOT_AVAILABLE_YET;
                }
                else
                {
                    int found = 0;
                    /* Now, walk the list of descriptors */
                    desc_list = pi->outer_descriptors;
                    MPE_LOG(
                            MPE_LOG_DEBUG,
                            MPE_MOD_SI,
                            "<mpe_siGetProgramNumberByDeferredAssociationTag> Looking for tag %d in deferred association tags: %p\n",
                            tag, desc_list);

                    while ((desc_list != NULL) && !found)
                    {
                        // Check to see if it's a deferred association tag.
                        if (desc_list->tag
                                == MPE_SI_DESC_DEFERRED_ASSOCIATION_TAG)
                        {
                            // It's a deferred asssociation tag.  Parse the puppy.
                            contentPtr = desc_list->descriptor_content;
                            loopLength = *contentPtr; // First, pull out the tag length
                            contentPtr++; // Skip the byte.
                            tagPtr = contentPtr; // We're at the tag area, grab it.
                            contentPtr += loopLength; // Skip the tags with the content ptr.
                            while (tagPtr != contentPtr)
                            {
                                // Get the tag to compare.  We have to break it out here because many non-x86 processors will
                                // crash with an unaligned memory address fault if we
                                temp = *tagPtr++;
                                temp = (uint16_t)((temp << 8) | *tagPtr++);

                                // Does it match?
                                if (temp == tag)
                                {
                                    // Found it
                                    // Skip the transport stream ID
                                    contentPtr += 2;

                                    // Copy the program number.
                                    temp = *contentPtr++;
                                    temp
                                            = (uint16_t)((temp << 8)
                                                    | *contentPtr);
                                    *programNumber = temp;
                                    MPE_LOG(
                                            MPE_LOG_DEBUG,
                                            MPE_MOD_SI,
                                            "<mpe_siGetProgramNumberByDeferredAssociationTag> Found Tag %d == program %d\n",
                                            tag, temp);
                                    // Ignore the other stuff, namely the original network ID
                                    found = 1;
                                    break;
                                }
                            }
                        }
                        // Not found on this iteration.  Let's check the next one.
                        desc_list = desc_list->next;
                    }
                    if (!found)
                        err = MPE_SI_NOT_FOUND;
                }
            }
        }
    }
    if (err != MPE_SI_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_SI,
                "<mpe_siGetProgramNumberByDeferredAssociationTag> Deferred association tag %d not found\n",
                tag);
    }
    return err;
}

/*  End of OC specific methods */

mpe_Bool mpe_siCheckCADescriptors(mpe_SiServiceHandle service_handle, uint16_t ca_system_id,
        uint32_t *numStreams, uint16_t *ecmPid, mpe_PODStreamDecryptInfo streamInfo[])
{
    mpe_SiMpeg2DescriptorList *desc_list_walker = NULL;
    mpe_SiElementaryStreamList *elem_stream_list = NULL;
    uint32_t numDescriptors = 0;
    mpe_Bool caDescFound = FALSE;
    mpe_Bool ecmPidFound = FALSE;
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;
    mpe_SiProgramInfo *pi = NULL;
    int i=0;
    // caller has SI DB read lock

    pi = si_entry->program;

    if (pi == NULL) // could be analog
    {
        return FALSE;
    }

    numDescriptors = pi->number_outer_desc;
    desc_list_walker = pi->outer_descriptors;

    if(numDescriptors == 0)
    {
        MPE_LOG(MPE_LOG_DEBUG,
                MPE_MOD_POD,
                "mpe_siCheckCADescriptors: No outer descriptors..\n");
    }

    MPE_LOG(MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "mpe_siCheckCADescriptors - numDescriptors: %d\n", numDescriptors );

    for (i = 0; i < numDescriptors; i++)
    {
        // an unexpected descriptor entry ?
        if (desc_list_walker == NULL)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "mpe_siCheckCADescriptors: Unexpected null descriptor_entry !\n");
            break;
        }

        if (desc_list_walker->tag == MPE_SI_DESC_CA_DESCRIPTOR
                && desc_list_walker->descriptor_length > 0)
        {
            uint16_t cas_id = ((uint8_t *) desc_list_walker->descriptor_content)[0] << 8
                               | ((uint8_t *) desc_list_walker->descriptor_content)[1];
            if(!ecmPidFound)
            {
                *ecmPid = ((uint8_t *) desc_list_walker->descriptor_content)[2] << 8
                                   | ((uint8_t *) desc_list_walker->descriptor_content)[3];

                *ecmPid &= 0x1FFF;
                ecmPidFound = TRUE;
            }

            MPE_LOG(MPE_LOG_DEBUG,
                    MPE_MOD_SI,
                    "mpe_siCheckCADescriptors - ecmPid: 0x%x\n", *ecmPid);

            // Match the CA system id
            if(cas_id == ca_system_id)
            {
                caDescFound = TRUE;
            }
        } // END if (CA descriptor)
        desc_list_walker = desc_list_walker->next;
    } // END for (outer descriptors)

    i=0;
    // Walk inner descriptors
    elem_stream_list = pi->elementary_streams;
    while (elem_stream_list)
    {
        desc_list_walker = elem_stream_list->descriptors;
        while (desc_list_walker)
        {
            if (desc_list_walker->tag == MPE_SI_DESC_CA_DESCRIPTOR)
            {
                uint16_t cas_id = ((uint8_t *) desc_list_walker->descriptor_content)[0] << 8
                                   | ((uint8_t *) desc_list_walker->descriptor_content)[1];

                if(!ecmPidFound)
                {
                    *ecmPid = ((uint8_t *) desc_list_walker->descriptor_content)[2] << 8
                                       | ((uint8_t *) desc_list_walker->descriptor_content)[3];
                    *ecmPid &= 0x1FFF;
                    ecmPidFound = TRUE;
                    MPE_LOG(MPE_LOG_DEBUG,
                            MPE_MOD_SI,
                            "mpe_siCheckCADescriptors - ecmPid: 0x%x\n", *ecmPid);
                }

                // Match the CA system id
                if(cas_id == ca_system_id)
                {
                    caDescFound = TRUE;
                }
            }
            desc_list_walker = desc_list_walker->next;
        }

        streamInfo[i].pid = elem_stream_list->elementary_PID;
        streamInfo[i].status = 0; // CA_ENABLE_NO_VALUE

        MPEOS_LOG(MPE_LOG_INFO,
                MPE_MOD_SI,
                "mpe_siCheckCADescriptors - pid:%d ca_status:%d \n", elem_stream_list->elementary_PID, streamInfo[i].status);
        i++;

        elem_stream_list = elem_stream_list->next;
    }

    *numStreams = i;
    if(caDescFound)
    {
        return TRUE;
    }
    else
    {
        return FALSE;
    }

    // Caller has to unlock SI DB
}

mpe_Error mpe_siFindPidInPMT(mpe_SiServiceHandle service_handle, int pid)
{
    mpe_Error err = MPE_SI_SUCCESS;
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;
    mpe_SiProgramInfo *pi;
    mpe_SiElementaryStreamList *elem_stream_list = NULL;
    int found = 0;

    /* Parameter check */
    if (service_handle == MPE_SI_INVALID_HANDLE)
    {
        err = MPE_SI_INVALID_PARAMETER;
    }
    else
    {
        /* SI DB has not been populated yet */
        if (g_si_entry == NULL)
        {
            err = MPE_SI_NOT_AVAILABLE;
        }
        else
        {
            pi = si_entry->program;
            MPEOS_LOG(MPE_LOG_DEBUG,
                    MPE_MOD_SI,
                    "mpe_siFindPidInPMT - pi:0x%p pid:%d \n", pi, pid);
            if (pi == NULL)
            {
                mpe_SiTransportStreamEntry *ts = (mpe_SiTransportStreamEntry *) si_entry->ts_handle;
                if (ts != NULL)
                {
                    if (ts->modulation_mode != MPE_SI_MODULATION_QAM_NTSC)
                        err = MPE_SI_INVALID_PARAMETER;
                    else
                        err = MPE_SI_NOT_FOUND;
                }
                else
                {
                    err = MPE_SI_NOT_AVAILABLE; // SPI service?
                }
            }
            else
            {
                MPEOS_LOG(MPE_LOG_DEBUG,
                        MPE_MOD_SI,
                        "mpe_siFindPidInPMT - pmt_status:%d \n", pi->pmt_status);
                /* If PSI is in the process of being acquired return from here
                 until it is updated. */
                if (pi->pmt_status == PMT_NOT_AVAILABLE)
                {
                    err = MPE_SI_NOT_AVAILABLE;
                }
                else if (pi->pmt_status == PMT_AVAILABLE_SHORTLY)
                {
                    err = MPE_SI_NOT_AVAILABLE_YET;
                }
                else
                {
                    elem_stream_list = pi->elementary_streams;
                    while (elem_stream_list && !found)
                    {
                        if (elem_stream_list->elementary_PID == pid)
                        {
	                        found = 1;
	                        err = MPE_SI_SUCCESS;
	                        break;
                        }
                        elem_stream_list = elem_stream_list->next;
                    }

                    if (!found)
                    {
                        err = MPE_SI_NOT_FOUND;
                    }
                }
            }
        }
    }
    return err;
}

/*
 @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@

 General SI query methods

 @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
 */

/**
 * Return the number of transports supported by the system. (only 1 for now)
 *
 * <i>mpe_siGetTotalNumberOfTransports()</i>
 *
 * @param num_transports output param to populate the number of transports
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetTotalNumberOfTransports(uint32_t *num_transports)
{
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siGetTotalNumberOfTransports>\n");

    /* Parameter check */
    if (num_transports == NULL)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    /* For now only one transport is supported! */
    *num_transports = MPE_SI_NUM_TRANSPORTS;

    return MPE_SI_SUCCESS;
}

/**
 * Return actual transports supported by the system. (only 1 for now)
 *
 * <i>mpe_siGetAllTransports()</i>
 *
 * @param array_transports output param to populate transport handles
 * @param actual_num_transports Input param: has size of array_transports.
 * Output param: the actual number of transport handles placed in array_transports
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetAllTransports(mpe_SiTransportHandle array_transports[],
        uint32_t* num_transports)
{
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siGetAllTransports>\n");

    /* Parameter check */
    if ((array_transports == NULL) || (num_transports == NULL
            || *num_transports == 0))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    array_transports[0] = MPE_SI_DEFAULT_TRANSPORT_HANDLE;
    *num_transports = MPE_SI_NUM_TRANSPORTS;

    return MPE_SI_SUCCESS;
}

/**
 * Return transport delivery system type.
 *
 * <i>mpe_siGetTransportDeliverySystemType()</i>
 *
 * @param transport_handle input transport handle
 * @param delivery_type output param to populate the transport delivery system type
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetTransportDeliverySystemType(
        mpe_SiTransportHandle transport_handle,
        mpe_SiDeliverySystemType *delivery_type)
{

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetTransportDeliverySystemType>\n");

    /* Parameter check */
    if ((transport_handle != MPE_SI_DEFAULT_TRANSPORT_HANDLE) || (delivery_type
            == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    /* For now only one 'CABLE' type is supported! */
    *delivery_type = MPE_SI_DELIVERY_SYSTEM_TYPE_CABLE;

    return MPE_SI_SUCCESS;
}

/**
 * Return the number of networks supported by the given transport. (only 1 for now)
 *
 * <i>mpe_siGetNumberOfNetworksForTransportHandle()</i>
 *
 * @param transport_handle input transport handle
 * @param num_networks output param to populate the number of networks
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetNumberOfNetworksForTransportHandle(
        mpe_SiTransportHandle transport_handle, uint32_t *num_networks)
{

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetNumberOfNetworksForTransportHandle> \n");

    /* Parameter check */
    if ((transport_handle != MPE_SI_DEFAULT_TRANSPORT_HANDLE) || (num_networks
            == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    /* For now only one network is supported! */
    *num_networks = MPE_SI_NUM_NETWORKS;

    return MPE_SI_SUCCESS;
}

/**
 * Return network handles supported by the transport. (only 1 for now)
 *
 * <i>mpe_siGetAllNetworksForTransportHandle()</i>
 *
 * @param transport_handle input transport handle
 * @param array_network_handle output param to populate network handles
 * @param num_networks Input param: has size of array_network_handle.
 * Output param: the actual number of network handles placed in array_network_handle
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetAllNetworksForTransportHandle(
        mpe_SiTransportHandle transport_handle,
        mpe_SiNetworkHandle array_network_handle[], uint32_t* num_networks)
{

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetAllNetworksForTransportHandle> \n");

    /* Parameter check */
    if ((transport_handle != MPE_SI_DEFAULT_TRANSPORT_HANDLE)
            || (array_network_handle == NULL) || (num_networks == NULL
            || *num_networks == 0))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    array_network_handle[0] = MPE_SI_DEFAULT_NETWORK_HANDLE;
    *num_networks = MPE_SI_NUM_NETWORKS;

    return MPE_SI_SUCCESS;
}

/**
 * Return the number of transport streams supported by the given transport.
 *
 * <i>mpe_siGetNumberOfTransportStreamsForTransportHandle()</i>
 *
 * @param transport_handle input transport handle
 * @param num_transport_streams output param to populate the number of transport streams
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetNumberOfTransportStreamsForTransportHandle(
        mpe_SiTransportHandle transport_handle, uint32_t *num_transport_streams)
{
    mpe_SiTransportStreamEntry *ts_walker = NULL;
    uint32_t numRefedTransportStreams = 0;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetNumberOfTransportStreamsForTransportHandle> \n");

    /* Parameter check */
    if ((transport_handle != MPE_SI_DEFAULT_TRANSPORT_HANDLE)
            || (num_transport_streams == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    // If SI DB is in either of these states return SI_NOT_AVAILABLE_YET
    // SI_ACQUIRING state is not useful for callers
    if ((g_si_state == SI_ACQUIRING) || (g_si_state == SI_NOT_AVAILABLE_YET))
        return MPE_SI_NOT_AVAILABLE_YET;
    else if (g_si_state == SI_DISABLED)
        return MPE_SI_NOT_AVAILABLE;

    /* Walk the transport stream list and only count the entries referenced by a service entry */
    ts_walker = g_si_ts_entry;
    while (ts_walker)
    {
        if (ts_walker->service_count > 0)
        {
            numRefedTransportStreams++;
        }
        ts_walker = ts_walker->next;
    }

    *num_transport_streams = numRefedTransportStreams;

    return MPE_SI_SUCCESS;
}

/**
 * Return transport stream handles supported by the transport.
 *
 * <i>mpe_siGetAllTransportStreamsForTransportHandle()</i>
 *
 * @param transport_handle input transport handle
 * @param array_ts_handle output param to populate transport stream handles
 * @param num_transport_streams Input param: has size of array_ts_handle.
 * Output param: the actual number of transport handles placed in array_ts_handle
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetAllTransportStreamsForTransportHandle(
        mpe_SiTransportHandle transport_handle,
        mpe_SiTransportStreamHandle array_ts_handle[],
        uint32_t* num_transport_streams)
{
    uint32_t i = 0;
    mpe_SiTransportStreamEntry *ts_walker = NULL;
    mpe_Error err = MPE_SI_SUCCESS;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetAllTransportStreamsForTransportHandle> \n");

    /* Parameter check */
    if ((transport_handle != MPE_SI_DEFAULT_TRANSPORT_HANDLE)
            || (array_ts_handle == NULL) || (num_transport_streams == NULL
            || *num_transport_streams == 0))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    // If SI DB is in either of these states return SI_NOT_AVAILABLE_YET
    // SI_ACQUIRING state is not useful for callers
    if ((g_si_state == SI_ACQUIRING) || (g_si_state == SI_NOT_AVAILABLE_YET))
        return MPE_SI_NOT_AVAILABLE_YET;
    else if (g_si_state == SI_DISABLED)
        return MPE_SI_NOT_AVAILABLE;

    /* Walk the transport stream list and only count the entries referenced by a service entry */
    ts_walker = g_si_ts_entry;
    while (ts_walker)
    {
        if (ts_walker->service_count > 0)
        {
            if (i == *num_transport_streams)
            {
                MPE_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_SI,
                        "<mpe_siGetAllTransportStreamsForTransportHandle> array_ts_handle (%d) too small for list\n",
                        *num_transport_streams);
                err = MPE_ENOMEM;
                break;
            }

#if 0
            MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siGetAllTransportStreamsForTransportHandle> Loading TS entry 0x%08x (freq %d) into slot %d\n",
                    ts_walker, ts_walker->frequency, i );
#endif
            array_ts_handle[i++] = (mpe_SiTransportStreamHandle) ts_walker;

        }
        ts_walker = ts_walker->next;
    }

    *num_transport_streams = i;

    return err;
}

/**
 * Return network handle given transport handle and network id
 *
 * <i>mpe_siGetNetworkHandleByTransportHandleAndNetworkId()</i>
 *
 * @param transport_handle input transport handle
 * @param network_id network id
 * @param network_handle output param to populate the network handle
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetNetworkHandleByTransportHandleAndNetworkId(
        mpe_SiTransportHandle transport_handle, uint32_t network_id,
        mpe_SiNetworkHandle* network_handle)
{

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetNetworkHandleByTransportHandleAndNetworkId> \n");

    /* Parameter check */
    if ((transport_handle != MPE_SI_DEFAULT_TRANSPORT_HANDLE) || (network_id
            != MPE_SI_DEFAULT_NETWORK_ID) || (network_handle == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    /* Return the one and only network handle */
    *network_handle = MPE_SI_DEFAULT_NETWORK_HANDLE;

    return MPE_SI_SUCCESS;
}

/**
 * Return transport id given transport handle
 *
 * <i>mpe_siGetTransportIdForTransportHandle()</i>
 *
 * @param transport_handle input transport handle
 * @param transport_id output transport id
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetTransportIdForTransportHandle(
        mpe_SiTransportHandle transport_handle, uint32_t* transport_id)
{

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetTransportIdForTransportHandle> \n");

    /* Parameter check */
    if ((transport_handle != MPE_SI_DEFAULT_TRANSPORT_HANDLE) || (transport_id
            == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    *transport_id = MPE_SI_DEFAULT_TRANSPORT_ID;

    return MPE_SI_SUCCESS;
}

/**
 * Return transport handle given transport id
 *
 * <i>mpe_siGetTransportHandleByTransportId()</i>
 *
 * @param transport_id input transport id
 * @param outHandle output transport handle
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetTransportHandleByTransportId(uint32_t transport_id,
        mpe_SiTransportHandle* outHandle)
{

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetTransportHandleByTransportId> \n");

    /* Parameter check */
    if ((transport_id != MPE_SI_DEFAULT_TRANSPORT_ID) || (outHandle == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    *outHandle = MPE_SI_DEFAULT_TRANSPORT_HANDLE;

    return MPE_SI_SUCCESS;
}

/**
 * Return the service information type.
 *
 * <i>mpe_siGetNetworkServiceInformationType()</i>
 *
 * @param network_handle input network handle
 * @param service_info_type output param to populate the network service info type
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetNetworkServiceInformationType(
        mpe_SiNetworkHandle network_handle,
        mpe_SiServiceInformationType *service_info_type)
{

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetNetworkServiceInformationType> \n");

    /* Parameter check */
    if ((network_handle != MPE_SI_DEFAULT_NETWORK_HANDLE) || (service_info_type
            == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    /* Only SCTE_SI is supported */
    *service_info_type = MPE_SI_SERVICE_INFORMATION_TYPE_SCTE_SI;

    return MPE_SI_SUCCESS;
}

/**
 * Return the network id for the given network handle.
 *
 * <i>mpe_siGetNetworkIdForNetworkHandle()</i>
 *
 * @param network_handle input network handle
 * @param network_id output param to populate the network id
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetNetworkIdForNetworkHandle(
        mpe_SiNetworkHandle network_handle, uint32_t *network_id)
{

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetNetworkIdForNetworkHandle> \n");

    /* Parameter check */
    if ((network_handle != MPE_SI_DEFAULT_NETWORK_HANDLE) || (network_id
            == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    *network_id = MPE_SI_DEFAULT_NETWORK_ID;

    return MPE_SI_SUCCESS;
}

/**
 * Return the network name for the given network handle.
 *
 * <i>mpe_siGetNetworkNameForNetworkHandle()</i>
 *
 * @param network_handle input network handle
 * @param network_name output param to populate the network name
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetNetworkNameForNetworkHandle(
        mpe_SiNetworkHandle network_handle, char **network_name)
{
    /* test only! */
    char *net_name = "DEFAULT_NETWORK_NAME";

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetNetworkNameForNetworkHandle> \n");

    /* Parameter check */
    if ((network_handle != MPE_SI_DEFAULT_NETWORK_HANDLE) || (network_name
            == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    *network_name = net_name;

    return MPE_SI_SUCCESS;
}

/**
 * Return the network last update time for the given network handle.
 *
 * <i>mpe_siGetNetworkLastUpdateTimeForNetworkHandle()</i>
 *
 * @param network_handle input network handle
 * @param time output param to populate the time network was last updated
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetNetworkLastUpdateTimeForNetworkHandle(
        mpe_SiNetworkHandle network_handle, mpe_TimeMillis *pTime)
{

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetNetworkLastUpdateTimeForNetworkHandle> \n");

    /* Parameter check */
    if ((network_handle != MPE_SI_DEFAULT_NETWORK_HANDLE) || (pTime == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    *pTime = gtime_network;

    return MPE_SI_SUCCESS;
}

/**
 * Return the number of transport streams supported by the given network.
 *
 * <i>mpe_siGetNumberOfTransportStreamsForNetworkHandle()</i>
 *
 * @param network_handle input network handle
 * @param num_transport_streams output param to populate the number of transport streams
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetNumberOfTransportStreamsForNetworkHandle(
        mpe_SiNetworkHandle network_handle, uint32_t *num_transport_streams)
{
    uint32_t numRefedTransportStreams = 0;
    mpe_SiTransportStreamEntry *ts_walker = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetNumberOfTransportStreamsForNetworkHandle> \n");

    /* Parameter check */
    if ((network_handle != MPE_SI_DEFAULT_NETWORK_HANDLE)
            || (num_transport_streams == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    // If SI DB is in either of these states return SI_NOT_AVAILABLE_YET
    // SI_ACQUIRING state is not useful for callers
    if ((g_si_state == SI_ACQUIRING) || (g_si_state == SI_NOT_AVAILABLE_YET))
        return MPE_SI_NOT_AVAILABLE_YET;
    else if (g_si_state == SI_DISABLED)
        return MPE_SI_NOT_AVAILABLE;
    //*num_transport_streams = g_numberOfTransportStreams;

    /* Walk the transport stream list and only count the entries referenced by a service entry */
    ts_walker = g_si_ts_entry;
    while (ts_walker)
    {
        if (ts_walker->service_count > 0)
        {
            numRefedTransportStreams++;
        }
        ts_walker = ts_walker->next;
    }

    *num_transport_streams = numRefedTransportStreams;

    return MPE_SI_SUCCESS;
}

/**
 * Return transport stream handles supported by the network.
 *
 * <i>mpe_siGetAllTransportStreamsForNetworkHandle()</i>
 *
 * @param network_handle input network handle
 * @param array_ts_handle output param to populate transport stream handles
 * @param num_transport_streams Input param: has size of array_ts_handle.
 * Output param: the actual number of transport handles placed in array_ts_handle
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetAllTransportStreamsForNetworkHandle(
        mpe_SiNetworkHandle network_handle,
        mpe_SiTransportStreamHandle array_ts_handle[],
        uint32_t* num_transport_streams)
{
    uint32_t i = 0;
    mpe_SiTransportStreamEntry *ts_walker = NULL;
    mpe_Error err = MPE_SI_SUCCESS;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetAllTransportStreamsForNetworkHandle> \n");

    /* Parameter check */
    if ((network_handle != MPE_SI_DEFAULT_NETWORK_HANDLE) || (array_ts_handle
            == NULL) || (num_transport_streams == NULL
            || *num_transport_streams == 0))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    // If SI DB is in either of these states return SI_NOT_AVAILABLE_YET
    // SI_ACQUIRING state is not useful for callers
    if ((g_si_state == SI_ACQUIRING) || (g_si_state == SI_NOT_AVAILABLE_YET))
        return MPE_SI_NOT_AVAILABLE_YET;
    else if (g_si_state == SI_DISABLED)
        return MPE_SI_NOT_AVAILABLE;

    /* Walk the transport stream list and only count the entries referenced by a service entry */
    ts_walker = g_si_ts_entry;
    while (ts_walker)
    {
        if (ts_walker->service_count > 0)
        {
            if (i == *num_transport_streams)
            {
                MPE_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_SI,
                        "<mpe_siGetAllTransportStreamsForTransportHandle> array_ts_handle (%d) too small for list\n",
                        *num_transport_streams);
                err = MPE_ENOMEM;
                break;
            }

#if 0
            MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siGetAllTransportStreamsForTransportHandle> Loading TS entry 0x%08x (freq %d) into slot %d\n",
                    ts_walker, ts_walker->frequency, i );
#endif
            array_ts_handle[i++] = (mpe_SiTransportStreamHandle) ts_walker;

        }
        ts_walker = ts_walker->next;
    }

    *num_transport_streams = i;

    return err;
}

/**
 * Return transport handle corresponding to the network handle.
 *
 * <i>mpe_siGetTransportHandleForNetworkHandle()</i>
 *
 * @param network_handle input network handle
 * @param transport_handle output transport handle
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetTransportHandleForNetworkHandle(
        mpe_SiNetworkHandle network_handle,
        mpe_SiTransportHandle* transport_handle)
{

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetTransportHandleForNetworkHandle> \n");

    /* Parameter check */
    if ((network_handle != MPE_SI_DEFAULT_NETWORK_HANDLE) || (transport_handle
            == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    *transport_handle = MPE_SI_DEFAULT_TRANSPORT_HANDLE;

    return MPE_SI_SUCCESS;
}

/**
 * Return transport stream handle given transport frequency and TSId.
 *
 * <i>mpe_siGetTransportStreamHandleByTransportFrequencyModulationAndTSID()</i>
 *
 * @param network_handle input network handle
 * @param transport_handle output transport handle
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetTransportStreamHandleByTransportFrequencyModulationAndTSID(
        mpe_SiTransportHandle transport_handle, uint32_t frequency,
        uint32_t mode, uint32_t ts_id, mpe_SiTransportStreamHandle* ts_handle)
{
    mpe_Error err = MPE_SI_SUCCESS;
    mpe_SiTransportStreamEntry *walker = NULL;

    /* Parameter check */
    // Check freq?
    if ((transport_handle != MPE_SI_DEFAULT_TRANSPORT_HANDLE) || (ts_handle
            == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }
    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetTransportStreamHandleByTransportFrequencyModulationAndTSID> freq:%d tsId:%d\n",
            frequency, ts_id);

    // This method can be called for OOB (PSI)
    // which is independent of OOB SI.
    // Don't do the g_si_state check here

    *ts_handle = MPE_SI_INVALID_HANDLE;

    if (frequency == MPE_SI_OOB_FREQUENCY)
    {
        walker = g_si_oob_ts_entry;
    }
    else if (frequency == MPE_SI_HN_FREQUENCY)
    {
        walker = g_si_HN_ts_entry;
    }
    else if (frequency == MPE_SI_DSG_FREQUENCY)
    {
        walker = g_si_dsg_ts_entry;
    }
    else
    {
        walker = g_si_ts_entry;

        if(g_si_ts_entry == NULL)
        {
            // If SI DB is in either of these states return SI_NOT_AVAILABLE_YET
            // SI_ACQUIRING state is not useful for callers
            if ((g_si_state == SI_ACQUIRING)
                    || (g_si_state == SI_NOT_AVAILABLE_YET))
                return MPE_SI_NOT_AVAILABLE_YET;
            else if (g_si_state == SI_DISABLED)
                return MPE_SI_NOT_AVAILABLE;
        }
    }

    while (walker != NULL)
    {
        /* If a wildcard tsId is specified, look for a match only of frequency */
        /*        if (walker->frequency == frequency &&
         (ts_id == -1 || walker->ts_id == ts_id))
         {
         *ts_handle = (mpe_SiTransportStreamHandle)walker;
         MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siGetTransportStreamHandleByTransportFrequencyModulationAndTSID> ts_handle found!!");
         break;
         }
         */
        // Its possible there might be multiple transport streams with the same frequency but
        // different modulation modes. In that case a search with frequency alone is not sufficient
        // we'll try to find the best possible match (one with a valid tsId value) for the given frequency
        if ((walker->frequency == frequency) && (walker->modulation_mode
                == mode))
        {
            if ((ts_id == (uint32_t) - 1) || (walker->ts_id == ts_id))
            {
                if (walker->ts_id != (uint32_t) - 1)
                {
                    // Found a ts with valid tsId
                    *ts_handle = (mpe_SiTransportStreamHandle) walker;
                    MPE_LOG(
                            MPE_LOG_DEBUG,
                            MPE_MOD_SI,
                            "<mpe_siGetTransportStreamHandleByTransportFrequencyModulationAndTSID> 1 ts_handle 0x%p\n",
                            walker);
                    break;
                }
                else // walker->ts_id is -1
                {
                    MPE_LOG(
                            MPE_LOG_DEBUG,
                            MPE_MOD_SI,
                            "<mpe_siGetTransportStreamHandleByTransportFrequencyModulationAndTSID> 2 ts_handle 0x%p\n",
                            walker);
                    if (*ts_handle == MPE_SI_INVALID_HANDLE)
                    {
                        // store this but continue search for better match
                        *ts_handle = (mpe_SiTransportStreamHandle) walker;
                    }
                }
            }
        }
        walker = walker->next;
    }

    if (*ts_handle == MPE_SI_INVALID_HANDLE)
    {
        // If a transport stream isn't found, should one be created on demand
        // similar to dynamic service handle.
        // The createTransportStreamHandle should accept frequency and modulation format
        // as parameters
        err = MPE_SI_NOT_FOUND;
    }

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetTransportStreamHandleByTransportFrequencyModulationAndTSID> ts_handle:0x%x\n",
            *ts_handle);
    return err;
}

/**
 * Return the ts id for the given transport stream handle.
 *
 * <i>mpe_siGetTransportStreamIdForTransportStreamHandle()</i>
 *
 * @param ts_handle input transport stream handle
 * @param ts_id output param to populate the ts id
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetTransportStreamIdForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle, uint32_t *ts_id)
{
    mpe_SiTransportStreamEntry *walker = NULL;

    /* Parameter check */
    if ((ts_handle == MPE_SI_INVALID_HANDLE) || (ts_id == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetTransportStreamIdForTransportStreamHandle> ts_handle:0x%x\n",
            ts_handle);

    walker = (mpe_SiTransportStreamEntry *) ts_handle;

    *ts_id = TSID_UNKNOWN;

    // OCAP spec allows a default tsId to be returned if SI is in
    // the process of being acquired
    *ts_id = walker->ts_id;

    if (walker->ts_id == TSID_UNKNOWN
        && walker->siStatus == SI_AVAILABLE_SHORTLY
        && walker->modulation_mode != MPE_SI_MODULATION_QAM_NTSC)
    {
        return MPE_SI_NOT_AVAILABLE_YET; // Only return this for digital streams
    }

    return MPE_SI_SUCCESS;
}

/**
 * Return the description for the given transport stream.
 *
 * <i>mpe_siGetDescriptionForTransportStreamHandle()</i>
 *
 * @param ts_handle input transport stream
 * @param description output param to populate the description
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetDescriptionForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle, char **description)
{
    mpe_SiTransportStreamEntry *walker = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetDescriptionForTransportStreamHandle> ts_handle:0x%x\n",
            ts_handle);

    /* Parameter check */
    if ((ts_handle == MPE_SI_INVALID_HANDLE) || (description == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    walker = (mpe_SiTransportStreamEntry *) ts_handle;
    *description = walker->description;

    return MPE_SI_SUCCESS;
}

/**
 * Return the name for the given transport stream.
 *
 * <i>mpe_siGetTransportStreamNameForTransportStreamHandle()</i>
 *
 * @param ts_handle input transport stream
 * @param ts_name output param to populate the name
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetTransportStreamNameForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle, char **ts_name)
{
    mpe_SiTransportStreamEntry *walker = NULL;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetTransportStreamNameForTransportStreamHandle> ts_handle:0x%x\n",
            ts_handle);

    /* Parameter check */
    if ((ts_handle == MPE_SI_INVALID_HANDLE) || (ts_name == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    walker = (mpe_SiTransportStreamEntry *) ts_handle;
    *ts_name = walker->ts_name;

    return MPE_SI_SUCCESS;
}

/**
 * Retrieve frequency for the given tarnsport stream handle
 *
 * <i>mpe_siGetFrequencyForTransportStreamHandle()</i>
 *
 * @param ts_handle input transport stream
 * @param freq is the output param to populate frequency
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetFrequencyForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle, uint32_t *freq)
{
    mpe_SiTransportStreamEntry *walker = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetFrequencyForTransportStreamHandle> ts_handle:0x%x\n",
            ts_handle);
    /* Parameter check */
    if ((ts_handle == MPE_SI_INVALID_HANDLE) || (freq == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    *freq = 0;

    walker = (mpe_SiTransportStreamEntry *) ts_handle;

    *freq = walker->frequency;
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "mpe_siGetFrequencyForTransportStreamHandle()...freq = %d\n",
            walker->frequency);

    return MPE_SI_SUCCESS;
}
/**
 * Retrieve modulation format for the given transport stream handle
 *
 * <i>mpe_siGetModulationFormatForTransportStreamHandle()</i>
 *
 * @param ts_handle input transport stream
 * @param mode is the output param to populate the mode
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetModulationFormatForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle, mpe_SiModulationMode *mode)
{
    mpe_SiTransportStreamEntry *walker = NULL;

    /* Parameter check */
    if ((ts_handle == MPE_SI_INVALID_HANDLE) || (mode == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    walker = (mpe_SiTransportStreamEntry *) ts_handle;

    *mode = walker->modulation_mode;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetModulationFormatForTransportStreamHandle> returning mode:%d\n",
            *mode);

    return MPE_SI_SUCCESS;
}

/**
 * Return the transport stream service information type.
 *
 * <i>mpe_siGetTransportStreamServiceInformationType()</i>
 *
 * @param ts_handle input transport stream handle
 * @param service_info_type output param to populate the transport stream service info type
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetTransportStreamServiceInformationType(
        mpe_SiTransportStreamHandle ts_handle,
        mpe_SiServiceInformationType* service_info_type)
{
    /* Parameter check */
    if ((ts_handle == MPE_SI_INVALID_HANDLE) || (service_info_type == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetTransportStreamServiceInformationType> ts_handle:0x%x\n",
            ts_handle);
    *service_info_type = MPE_SI_SERVICE_INFORMATION_TYPE_SCTE_SI;

    return MPE_SI_SUCCESS;
}

/**
 * Return the transport stream last update time for the given transport stream handle.
 *
 * <i>mpe_siGetTransportStreamLastUpdateTimeForTransportStreamHandle()</i>
 *
 * @param ts_handle input transport stream handle
 * @param time output param to populate the time transport stream was last updated
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetTransportStreamLastUpdateTimeForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle, mpe_TimeMillis *pTime)
{
    mpe_SiTransportStreamEntry *walker = NULL;

    /* Parameter check */
    if ((ts_handle == MPE_SI_INVALID_HANDLE) || (pTime == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    walker = (mpe_SiTransportStreamEntry *) ts_handle;
    *pTime = walker->ptime_transport_stream;

    return MPE_SI_SUCCESS;
}

/**
 * Return the number of services in the given transport streams.
 *
 * <i>mpe_siGetNumberOfServicesForTransportStreamHandle()</i>
 *
 * @param ts_handle input transport stream handle
 * @param num_services output param to populate the number of services
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetNumberOfServicesForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle, uint32_t *num_services)
{
    mpe_SiTransportStreamEntry *target_ts_entry = NULL;

    /* Parameter check */
    if ((ts_handle == MPE_SI_INVALID_HANDLE) || (num_services == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    // This method can be called for OOB (PAT/PMT) which
    // is independent of OOB SI (SVCT, NIT).
    // Don't check the g_si_state

    *num_services = 0;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetNumberOfServicesForTransportStreamHandle> 0x%x\n",
            ts_handle);

    target_ts_entry = (mpe_SiTransportStreamEntry *) ts_handle;
    *num_services = target_ts_entry->visible_service_count;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetNumberOfServicesForTransportStreamHandle> ts_entry->visible_service_count:%d\n",
            target_ts_entry->visible_service_count);

    return MPE_SI_SUCCESS;
}

/**
 * Return all service handles in the given transport stream.
 *
 * <i>mpe_siGetAllServicesForTransportStreamHandle()</i>
 *
 * @param ts_handle input transport stream handle
 * @param array_service_handle output param to populate service handles
 * @param num_services Input param: has size of array_service_handle.
 * Output param: the actual number of transport handles placed in array_service_handle
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetAllServicesForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle,
        mpe_SiServiceHandle array_service_handle[], uint32_t* num_services)
{
    mpe_Error err = MPE_SI_SUCCESS;
    mpe_SiTableEntry *walker = NULL;
    uint32_t i = 0;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetAllServicesForTransportStreamHandle> ts_handle:0x%x\n",
            ts_handle);
    /* Parameter check */
    if ((ts_handle == MPE_SI_INVALID_HANDLE) || (array_service_handle == NULL)
            || (num_services == NULL || *num_services == 0))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    /* Acquire global list mutex */
    //mpe_mutexAcquire(g_global_si_list_mutex);

    walker = g_si_entry;

    while (walker)
    {
        if ((walker->channel_type == CHANNEL_TYPE_NORMAL)
                && ((walker->state == SIENTRY_MAPPED) || (walker->state
                        == SIENTRY_DEFINED_MAPPED)) && (walker->ts_handle
                == ts_handle))
        {
            if (i == *num_services)
            {
                MPE_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_SI,
                        "<mpe_siGetAllServicesForTransportStreamHandle> array_service_handle (%d) too small for list\n",
                        *num_services);
                err = MPE_ENOMEM;
                break;
            }
            walker->ref_count++;
            array_service_handle[i++] = (mpe_SiServiceHandle) walker;
        }
        walker = walker->next;
    }

    *num_services = i;

    //mpe_mutexRelease(g_global_si_list_mutex);
    /* Release global list mutex */

    return err;
}

/**
 * Return network handle given transport stream handle
 *
 * <i>mpe_siGetNetworkHandleForTransportStreamHandle()</i>
 *
 * @param ts_handle input transport stream handle
 * @param network_handle output param to populate network handle
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetNetworkHandleForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle,
        mpe_SiNetworkHandle* network_handle)
{
    /* Parameter check */
    if ((ts_handle == MPE_SI_INVALID_HANDLE) || (network_handle == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }
    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetNetworkHandleForTransportStreamHandle> ts_handle:0x%x\n",
            ts_handle);

    /* Give the only network handle available! */
    *network_handle = MPE_SI_DEFAULT_NETWORK_HANDLE;

    return MPE_SI_SUCCESS;
}

/**
 * Return transport handle given transport stream handle
 *
 * <i>mpe_siGetTransportHandleForTransportStreamHandle()</i>
 *
 * @param ts_handle input transport stream handle
 * @param transport_handle output param to populate transport handle
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetTransportHandleForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle,
        mpe_SiTransportHandle* transport_handle)
{
    /* Parameter check */
    if ((ts_handle == MPE_SI_INVALID_HANDLE) || (transport_handle == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetTransportHandleForTransportStreamHandle> ts_handle:0x%x\n",
            ts_handle);

    /* Give the only transport handle available! */
    *transport_handle = MPE_SI_DEFAULT_TRANSPORT_HANDLE;

    return MPE_SI_SUCCESS;
}

/**
 Acqure Service handles using one of the three methods below
 1. By sourceId
 2. By freq/prog_num pair
 3. By service name
 4. By service major/minor number
 */

/**
 * Acquire a service handle given major/minor service number
 *
 * <i>mpe_siGetServiceHandleByServiceNumber()</i>
 *
 *
 * @param majorNumber is the service number (major) that uniquely identifies
 * a given service.
 * @param minorNumber is the service number (minor) that uniquely identifies
 * a given service.  This value may be passed as -1 and it will be disregarded
 * @param service_handle output param to populate the service handle
 *
 * @return MPE_SI_SUCCESS if successfully acquired the handle, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetServiceHandleByServiceNumber(uint32_t majorNumber,
        uint32_t minorNumber, mpe_SiServiceHandle *service_handle)
{
    mpe_SiTableEntry *walker = NULL;

    /* Parameter check */
    if (service_handle == NULL)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetServiceHandleByServiceNumber> major=0x%x, minor=0x%x\n",
            majorNumber, minorNumber);

    /* Clear out the value */
    *service_handle = MPE_SI_INVALID_HANDLE;

    // If SI DB is in either of these states return SI_NOT_AVAILABLE_YET
    // SI_ACQUIRING state is not useful for callers
    if (g_si_state == SI_ACQUIRING || g_si_state == SI_NOT_AVAILABLE_YET)
        return MPE_SI_NOT_AVAILABLE_YET;
    else if (g_si_state == SI_DISABLED)
        return MPE_SI_NOT_AVAILABLE;

    /* Walk through the list of services to find a matching service numbers. */
    walker = g_si_entry;
    while (walker)
    {
        if (((walker->state == SIENTRY_MAPPED) || (walker->state
                == SIENTRY_DEFINED_MAPPED)) && (walker->major_channel_number
                == majorNumber) && (minorNumber == (uint32_t) - 1
                || walker->minor_channel_number == minorNumber))
        {
            *service_handle = (mpe_SiServiceHandle) walker;
            walker->ref_count++;
            return MPE_SI_SUCCESS;
        }
        walker = walker->next;
    }

    /* Matching service numbers not found, return error */
    return MPE_SI_NOT_FOUND;
}

/**
 * Acquire a service handle given source Id
 *
 * <i>mpe_siGetServiceHandleBySourceId()</i>
 *
 *
 * @param sourceId is a value that uniquely identifies a given service.
 * @param service_handle output param to populate the service handle
 *
 * @return MPE_SI_SUCCESS if successfully acquired the handle, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetServiceHandleBySourceId(uint32_t sourceId,
        mpe_SiServiceHandle *service_handle)
{
    mpe_Error err = MPE_SI_SUCCESS;
    mpe_SiTableEntry *walker = NULL;
    mpe_SiTransportStreamEntry *ts_entry = NULL;

    /* Parameter check */
    if (service_handle == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI,
                "<mpe_siGetServiceHandleBySourceId> null pointer to service_handle!\n");
        err = MPE_SI_INVALID_PARAMETER;
    }
    else
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mpe_siGetServiceHandleBySourceId> 0x%x\n", sourceId);

        /* Clear out the value */
        *service_handle = MPE_SI_INVALID_HANDLE;

        // If SI DB is in either of these states return SI_NOT_AVAILABLE_YET
        // SI_ACQUIRING state is not useful for callers
        if ((g_si_state == SI_ACQUIRING)
                || (g_si_state == SI_NOT_AVAILABLE_YET))
        {
            err = MPE_SI_NOT_AVAILABLE_YET;
        }
        else
        {
            if (g_si_state == SI_DISABLED)
            {
                err = MPE_SI_NOT_AVAILABLE;
            }
            else
            {
                /* Walk through the list of services to find a matching source Id. */
                walker = g_si_entry;
                while (walker)
                {
                    if (((walker->state == SIENTRY_MAPPED) || (walker->state
                            == SIENTRY_DEFINED_MAPPED)) && (walker->source_id
                            == sourceId))
                    {
                        ts_entry
                                = (mpe_SiTransportStreamEntry *) walker->ts_handle;
                        *service_handle = (mpe_SiServiceHandle) walker;
                        if ((ts_entry != NULL) && (ts_entry->modulation_mode
                                != MPE_SI_MODULATION_QAM_NTSC))
                        {
                            break;
                        }
                    }
                    walker = walker->next;
                }

                /* Matching sourceId is not found, return error */
                if (MPE_SI_INVALID_HANDLE == *service_handle)
                {
                    err = MPE_SI_NOT_FOUND;
                }
                else
                {
                    // Increment the ref count
                    ((mpe_SiTableEntry *) *service_handle)->ref_count++;
                }
            }
        }
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mpe_siGetServiceHandleBySourceId> %d / 0x%8.8x / %d\n",
                sourceId, *service_handle, err);
    }

    return err;
}

/**
 * Acquire a service handle given app Id (for DSG only)
 *
 * <i>mpe_siGetServiceHandleByAppId()</i>
 *
 *
 * @param appId is a value that uniquely identifies a DSG tunnel.
 * @param service_handle output param to populate the service handle
 *
 * @return MPE_SI_SUCCESS if successfully acquired the handle, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetServiceHandleByAppId(uint32_t appId,
        mpe_SiServiceHandle *service_handle)
{
    mpe_Error err = MPE_SI_SUCCESS;
    mpe_SiTableEntry *walker = NULL;
    int found = 0;

    /* Parameter check */
    if (service_handle == NULL)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetServiceHandleByAppId> 0x%x\n", appId);

    /* Clear out the value */
    *service_handle = MPE_SI_INVALID_HANDLE;

    // If SI DB is in either of these states return SI_NOT_AVAILABLE_YET
    // SI_ACQUIRING state is not useful for callers
    if ((g_si_state == SI_ACQUIRING) || (g_si_state == SI_NOT_AVAILABLE_YET))
        return MPE_SI_NOT_AVAILABLE_YET;
    else if (g_si_state == SI_DISABLED)
        return MPE_SI_NOT_AVAILABLE;

    /* Walk through the list of services to find a matching app Id. */
    walker = g_si_entry;
    while (walker)
    {
        if (walker->isAppType && walker->app_id == appId)
        {
            *service_handle = (mpe_SiServiceHandle) walker;
            walker->ref_count++;
            found = 1;
            break;
        }
        walker = walker->next;
    }

    /* Matching appId is not found, return error */
    if (!found)
    {
        err = MPE_SI_NOT_FOUND;
    }

    return err;
}

/**
 * Acquire a service handle given session (for HN Stream session
 * only)
 *
 * <i>mpe_siGetServiceHandleByHNSession()</i>
 *
 *
 * @param session is a value that uniquely identifies a HN
 *                streaming session.
 * @param service_handle output param to populate the service
 *                       handle
 *
 * @return MPE_SI_SUCCESS if successfully acquired the handle, else
 *          return appropriate mpe_Error
 *
 */
static mpe_Error si_getServiceHandleByHNSession(mpe_HnStreamSession session,
        mpe_SiServiceHandle *service_handle)
{
    mpe_Error err = MPE_SI_SUCCESS;
    mpe_SiTableEntry *walker = NULL;
    int found = 0;

    /* Parameter check */
    if (service_handle == NULL)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetServiceHandleByHNSession 0x%x\n", session);

    /* Clear out the value */
    *service_handle = MPE_SI_INVALID_HANDLE;

    // If SI DB is in either of these states return SI_NOT_AVAILABLE_YET
    // SI_ACQUIRING state is not useful for callers
    if ((g_si_state == SI_ACQUIRING) || (g_si_state == SI_NOT_AVAILABLE_YET))
        return MPE_SI_NOT_AVAILABLE_YET;
    else if (g_si_state == SI_DISABLED)
        return MPE_SI_NOT_AVAILABLE;

    /* Walk through the list of services to find a matching session Id. */
    walker = g_si_entry;
    while (walker)
    {
        if (walker->hn_stream_session == session)
        {
            *service_handle = (mpe_SiServiceHandle) walker;
            walker->ref_count++;
            found = 1;
            break;
        }
        walker = walker->next;
    }

    /* Matching sourceId is not found, return error */
    if (!found)
    {
        err = MPE_SI_NOT_FOUND;
    }

    return err;
}

/**
 * Acquire a service handle given frequency, program_number
 *
 * <i>mpe_siGetServiceHandleByFrequencyModulationProgramNumber()</i>
 *
 *
 * @param freq is the carrier frequency that a virtual channel is carried on.
 * @param prog_num is a value that identifies a program with in a service.
 *        (frequency/program_number pair CANNOT uniquely identify a service,
 *          since serveral services can point to the same program.)
 * @param service_handle output param to populate the handle
 *
 * @return MPE_SI_SUCCESS if successfully acquired the handle, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetServiceHandleByFrequencyModulationProgramNumber(
        uint32_t freq, uint32_t mode, uint32_t prog_num,
        mpe_SiServiceHandle *service_handle)
{
    mpe_Error err = MPE_SI_SUCCESS;
    mpe_SiTransportStreamEntry *walker = NULL;
    int found = 0;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetServiceHandleByFrequencyModulationProgramNumber> %d:%d:0x%x\n",
            freq, mode, prog_num);
    /* Parameter check */
    if (service_handle == NULL)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    /* Clear out the value */
    *service_handle = MPE_SI_INVALID_HANDLE;

    /* First, check if the list is empty */
    if (g_si_entry == NULL)
    {
        if ((g_si_state == SI_ACQUIRING)
                || (g_si_state == SI_NOT_AVAILABLE_YET))
            return MPE_SI_NOT_AVAILABLE_YET;
        else if (g_si_state == SI_DISABLED)
            return MPE_SI_NOT_AVAILABLE;
    }

    if (freq == MPE_SI_OOB_FREQUENCY)
    {
        walker = g_si_oob_ts_entry;
    }
    else if (freq == MPE_SI_HN_FREQUENCY)
    {
        walker = g_si_HN_ts_entry;
    }
    else if (freq == MPE_SI_DSG_FREQUENCY)
    {
        walker = g_si_dsg_ts_entry;
    }
    else
    {
        // look up the transport stream based on freq and modulation mode
        walker = (mpe_SiTransportStreamEntry *) get_transport_stream_handle(
                freq, mode);
    }

    if (walker != NULL)
    {
        ListSI svc_list = NULL;
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_SI,
                "<mpe_siGetServiceHandleByFrequencyModulationProgramNumber> ts_handle:0x%p\n",
                walker);
        mpe_mutexAcquire(walker->list_lock);
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_SI,
                "<mpe_siGetServiceHandleByFrequencyModulationProgramNumber> program count:%d\n",
                llist_cnt(walker->programs));

        if(walker == g_si_HN_ts_entry)
        {
            svc_list = walker->services;
        }

        if (llist_cnt(walker->programs) != 0)
        {
            // IB or OOB digital services
            mpe_SiProgramInfo *prog = NULL;
            LINK *lp = llist_first(walker->programs);
            //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siGetServiceHandleByFrequencyModulationProgramNumber> llist_cnt(walker->programs):%d\n", llist_cnt(walker->programs));
            while (lp)
            {
                prog = (mpe_SiProgramInfo *) llist_getdata(lp);
                if (prog && prog->program_number == prog_num)
                {
                    break;
                }
                lp = llist_after(lp);
            }
            if (lp && prog)
                svc_list = prog->services;
        }
        else
        { // Analog services
            svc_list = walker->services;
        }

        if (svc_list != NULL)
        {
            LINK *lp1;
            lp1 = llist_first(svc_list);
            while (lp1 != NULL && !found)
            {
            	mpe_SiTableEntry *si_entry = NULL;
            	si_entry = (mpe_SiTableEntry *)llist_getdata(lp1);
            	// Find the service handle that is FPQ only
            	if(si_entry->source_id == SOURCEID_UNKNOWN)
            	{
                    *service_handle = (mpe_SiServiceHandle) llist_getdata(lp1);
                    found = true;
                }
            	lp1 = llist_after(lp1);
            }
        }
        mpe_mutexRelease(walker->list_lock);
        // break;
    }

    /* Matching freq, prog_num are not found, return error */
    if (!found)
    {
        err = MPE_SI_NOT_FOUND;
    }

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetServiceHandleByFrequencyModulationProgramNumber> done, returning ret: %d service_handle: 0x%x\n",
            err, *service_handle);

    return err;
}

/**
 * Create a DSG service entry given source ID and service name
 *
 * <i>mpe_siCreateDSGServiceHandle()</i>
 *
 *
 * @param appID is the applcaiton Id for the DSG tunnel.
 * @param service_name is a unique name assigned to the tunnel
 * @param service_handle output param to populate the handle
 *
 * @return MPE_SI_SUCCESS if successfully created the handle, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siCreateDSGServiceHandle(uint32_t appID, uint32_t prog_num,
        char* service_name, char* language, mpe_SiServiceHandle *service_handle)
{
    mpe_SiTableEntry *walker = NULL;
    mpe_SiTableEntry *new_si_entry = NULL;
    mpe_SiTransportStreamEntry *ts = NULL;
    int len = 0;
    int found = 0;
    int inTheList = 0;
    mpe_SiProgramInfo *pgm = NULL;
    mpe_Error tRetVal = MPE_SI_SUCCESS;
    char name[256] = "";
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siCreateDSGServiceHandle> appID= %d\n", appID);

    // Parameter check
    if (service_handle == NULL)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    // Clear out the value
    *service_handle = MPE_SI_INVALID_HANDLE;

    // Program number for application tunnels?
    ts = (mpe_SiTransportStreamEntry *) get_dsg_transport_stream_handle();

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siCreateDSGServiceHandle> transport stream entry...0x%p\n",
            ts);

    mpe_mutexAcquire(ts->list_lock);
    {
        LINK *lp = llist_first(ts->programs);
        while (lp && !found)
        {
            pgm = (mpe_SiProgramInfo *) llist_getdata(lp);
            if (pgm)
            {
                LINK *lp1 = llist_first(pgm->services);
                while (lp1)
                {
                    walker = (mpe_SiTableEntry *) llist_getdata(lp1);
                    if (walker != NULL && walker->app_id == appID)
                    {
                        new_si_entry = walker;
                        inTheList = true;
                        found = true;
                        break;
                    }
                    lp1 = llist_after(lp1);
                }
            }
            lp = llist_after(lp);
        }

        if (lp == NULL)
        {
            // Create a place holder to store program info
            pgm = create_new_program_info(ts, prog_num);
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                    "<mpe_siCreateDSGServiceHandle> program Entry...0x%p\n",
                    pgm);
            if (pgm == NULL)
            {
                mpe_mutexRelease(ts->list_lock);
                return MPE_SI_OUT_OF_MEM;
            }
            // newly created pgm is attached to ts by create_new_program_info.
        }
    }

    if (new_si_entry == NULL)
    {
        tRetVal = mpeos_memAllocP(MPE_MEM_SI, sizeof(mpe_SiTableEntry),
                (void **) &(new_si_entry));
        if ((MPE_SI_SUCCESS != tRetVal) || (new_si_entry == NULL))
        {
            MPE_LOG(
                    MPE_LOG_WARN,
                    MPE_MOD_SI,
                    "<mpe_siCreateDSGServiceHandle> Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");
            mpe_mutexRelease(ts->list_lock);
            return MPE_SI_OUT_OF_MEM;
        }

        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_SI,
                "<mpe_siCreateDSGServiceHandle> creating new SI Entry...0x%p\n",
                new_si_entry);

        /* Initialize all the fields (some fields are set to default values as defined in spec) */
        init_si_entry(new_si_entry);
        new_si_entry->isAppType = TRUE;
        new_si_entry->app_id = appID;
        found = true;

        // Link into other structs.
        if (pgm != NULL)
        {
            LINK *lp = llist_mklink((void *) new_si_entry);
            MPE_LOG(MPE_LOG_TRACE7, MPE_MOD_SI,
                    "Add service %p to program %p...\n", new_si_entry, pgm);
            llist_append(pgm->services, lp);
            pgm->service_count++;
        }
        {
            LINK *lp = llist_mklink((void *) new_si_entry);
            llist_append(ts->services, lp);
            ts->service_count++;
            MPE_LOG(MPE_LOG_TRACE7, MPE_MOD_SI,
                    "Add service %p to transport %p...\n", new_si_entry, ts);
        }
    }
    else
    {
        found = true;
    }

    if (service_name != NULL)
    {
        len = strlen(service_name);
        strncpy(name, service_name, len);
    }
    else
    {
        // Since we are creating a DSG entry for the appID
        // Seed it with a name that contains appID
        sprintf(name, "DSGAppTunnel_%d",  appID);
        len = strlen(name);
    }

    if (len > 0)
    {
        //
        // Code modified to look up source name entry as this information moved to another table.
        mpe_siSourceNameEntry *source_name_entry = NULL;

        //
        // Look up, but allow a create, the source name by appID
        mpe_siGetSourceNameEntry(appID, TRUE, &source_name_entry, TRUE);

        if (source_name_entry != NULL)
        {
          mpe_siSetSourceName(source_name_entry, name, language, FALSE);
          mpe_siSetSourceLongName(source_name_entry, name, language);
          new_si_entry->source_name_entry = source_name_entry;
          source_name_entry->mapped = TRUE;   // this source name now mapped to an si db entry
        }
    }
    mpe_mutexRelease(ts->list_lock);

    if (found)
    {
        // ServiceEntry pointers
        new_si_entry->ts_handle = (mpe_SiTransportStreamHandle) ts;
        new_si_entry->program = pgm;
        new_si_entry->source_id = SOURCEID_UNKNOWN;

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mpe_siCreateDSGServiceHandle> Inserting service entry in the list \n");

        if (g_SITPConditionSet == TRUE)
        {
            (ts->service_count)++;
            (ts->visible_service_count)++;
        }
        new_si_entry->ref_count++;

        *service_handle = (mpe_SiServiceHandle) new_si_entry;

        /* append new entry to the dynamic list */
        if(!inTheList)
        {
            if (g_si_entry == NULL)
            {
                g_si_entry = new_si_entry;
            }
            else
            {
                walker = g_si_entry;
                while (walker)
                {
                    if (walker->next == NULL)
                    {
                        walker->next = new_si_entry;
                        break;
                    }
                    walker = walker->next;
                }
            }
        }
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siCreateDSGServiceHandle> done, returning MPE_SI_SUCCESS\n");
    return MPE_SI_SUCCESS;
}

/**
 This method is specifically called when a DSG tunnel needs to be opened thereby
 starting PSI acquisition.
 */
mpe_Error mpe_siRegisterForPSIAcquisition(mpe_SiServiceHandle service_handle)
{
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;
    uint32_t appId = 0;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siRegisterForPSIAcquisition> service_handle:0x%x\n",
            service_handle);

    if (service_handle == MPE_SI_INVALID_HANDLE)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    // Acquire DSG mutex
    mpe_mutexAcquire(g_dsg_mutex);

    // Attach only once when the count goes from 0 to 1
    // all other cases the count is just incremented.
    if (si_entry->isAppType)
        si_entry->dsg_attach_count++;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siRegisterForPSIAcquisition> dsg_attach_count: %d\n",
            si_entry->dsg_attach_count);

    if (si_entry->dsgAttached == FALSE)
    {
        if (si_entry->isAppType)
        {
            mpe_SiProgramInfo *pi = NULL;
            appId = si_entry->app_id;

            // set TSID status
            if (si_entry->ts_handle != MPE_SI_INVALID_HANDLE)
                mpe_siSetTSIdStatusAvailableShortly(si_entry->ts_handle);

            pi = (mpe_SiProgramInfo *) si_entry->program;
            // Is this the right place to set this status?
            if (pi && (pi->pmt_status == PMT_NOT_AVAILABLE))
            {
                pi->pmt_status = PMT_AVAILABLE_SHORTLY;

                MPE_LOG(
                        MPE_LOG_DEBUG,
                        MPE_MOD_SI,
                        "<mpe_siRegisterForPSIAcquisition> PMT status set to available shortly...program entry: 0x%p\n",
                        pi);
            }

            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_SI,
                    "<mpe_siRegisterForPSIAcquisition> calling attachDSGTunnel appId:%d\n",
                    appId);

            // Notify SITP to open the DSG tunnel
            if (attachDSGTunnel(appId) == MPE_SI_SUCCESS)
            {
                si_entry->dsgAttached = TRUE;
            }
            else
            {
                mpe_siRevertTSIdStatus(si_entry->ts_handle);
                pi->pmt_status = PMT_NOT_AVAILABLE;

                si_entry->dsg_attach_count--;
            }
        }
    }

    mpe_mutexRelease(g_dsg_mutex);
    // Release DSG mutex

    return MPE_SI_SUCCESS;
}

/**
 This method is specifically called when a DSG tunnel needs to be closed.
 */
mpe_Error mpe_siUnRegisterForPSIAcquisition(mpe_SiServiceHandle service_handle)
{
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;
    uint32_t appId = 0;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siUnRegisterForPSIAcquisition> service_handle:0x%x\n",
            service_handle);

    if (service_handle == MPE_SI_INVALID_HANDLE)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    // Acquire DSG mutex
    mpe_mutexAcquire(g_dsg_mutex);

    if (si_entry->isAppType && si_entry->dsg_attach_count == 0
            && si_entry->dsgAttached == FALSE)
    {
        mpe_mutexRelease(g_dsg_mutex);
        // This should never happen!!
        return MPE_SI_SUCCESS;
    }

    if (si_entry->isAppType)
        si_entry->dsg_attach_count--;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siUnRegisterForPSIAcquisition> dsg_attach_count: %d\n",
            si_entry->dsg_attach_count);

    if (si_entry->dsg_attach_count == 0 && si_entry->dsgAttached == TRUE)
    {
        if (si_entry->isAppType)
        {
            appId = si_entry->app_id;

            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_SI,
                    "<mpe_siUnRegisterForPSIAcquisition> calling detachDSGTunnel appId:%d\n",
                    appId);

            // Notify SITP to close the DSG tunnel
            (void) detachDSGTunnel(appId);

            si_entry->dsgAttached = FALSE;
        }
    }

    mpe_mutexRelease(g_dsg_mutex);
    // Release DSG mutex

    return MPE_SI_SUCCESS;
}

mpe_Error mpe_siIsServiceHandleAppType(mpe_SiServiceHandle service_handle,
        mpe_Bool *isAppType)
{
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;

    if ((service_handle == MPE_SI_INVALID_HANDLE) || (isAppType == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siIsServiceHandleAppType> service_handle:0x%x isAppType:%d\n",
            service_handle, si_entry->isAppType);

    *isAppType = si_entry->isAppType;

    return MPE_SI_SUCCESS;
}

/**
 * Create a HN streaming service entry given source ID and
 * service name
 *
 * <i>mpe_siCreateHNstreamServiceHandle()</i>
 *
 *
 * @param sessionID is the hn streaming session Id for the DSG
 * @param service_name is a unique name assigned to the tunnel
 * @param service_handle output param to populate the handle
 *
 * @return MPE_SI_SUCCESS if successfully created the handle, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siCreateHNstreamServiceHandle(mpe_HnStreamSession session, uint32_t prog_num,
        char* service_name, char* language, mpe_SiServiceHandle *service_handle)
{
    mpe_SiTableEntry *walker = NULL;
    mpe_SiTableEntry *new_si_entry = NULL;
    mpe_SiTransportStreamEntry *ts = NULL;
    int found = 0;
    int inTheList = 0;
    mpe_SiProgramInfo *pgm = NULL;
    mpe_Error tRetVal = MPE_SI_SUCCESS;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siCreateHNstreamServiceHandle> sessionID= %x\n", session);

    // Parameter check
    if (service_handle == NULL)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    // Clear out the value
    *service_handle = MPE_SI_INVALID_HANDLE;

    // Program number for application tunnels?
    ts = (mpe_SiTransportStreamEntry *) get_hn_transport_stream_handle();

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siCreateHNstreamServiceHandle> transport stream entry...0x%x\n",
            ts);

    mpe_mutexAcquire(ts->list_lock);
    {
        LINK *lp = llist_first(ts->programs);
        while (lp && !found)
        {
            pgm = (mpe_SiProgramInfo *) llist_getdata(lp);
            if (pgm)
            {
                LINK *lp1 = llist_first(pgm->services);
                while (lp1)
                {
                    walker = (mpe_SiTableEntry *) llist_getdata(lp1);
                    if (walker != NULL && walker->hn_stream_session == session)
                    {
                        new_si_entry = walker;
                        inTheList = true;
                        found = true;
                        break;
                    }
                    lp1 = llist_after(lp1);
                }
            }
            lp = llist_after(lp);
        }

        if (lp == NULL)
        {
            // Create a place holder to store program info
            pgm = create_new_program_info(ts, prog_num);
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                    "<mpe_siCreateHNstreamServiceHandle> program Entry...0x%x\n",
                    pgm);
            if (pgm == NULL)
            {
                mpe_mutexRelease(ts->list_lock);
                return MPE_SI_OUT_OF_MEM;
            }
            // newly created pgm is attached to ts by create_new_program_info.
        }
    }

    if (new_si_entry == NULL)
    {
        tRetVal = mpeos_memAllocP(MPE_MEM_SI, sizeof(mpe_SiTableEntry),
                (void **) &(new_si_entry));
        if ((MPE_SI_SUCCESS != tRetVal) || (new_si_entry == NULL))
        {
            MPE_LOG(
                    MPE_LOG_WARN,
                    MPE_MOD_SI,
                    "<mpe_siCreateHNstreamServiceHandle> Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");
            mpe_mutexRelease(ts->list_lock);
            return MPE_SI_OUT_OF_MEM;
        }

        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_SI,
                "<mpe_siCreateHNstreamServiceHandle> creating new SI Entry...0x%x\n",
                new_si_entry);

        /* Initialize all the fields (some fields are set to default values as defined in spec) */
        init_si_entry(new_si_entry);
        new_si_entry->hn_stream_session = session;
        found = true;

        // Link into other structs.
        if (pgm != NULL)
        {
            LINK *lp = llist_mklink((void *) new_si_entry);
            MPE_LOG(MPE_LOG_TRACE7, MPE_MOD_SI,
                    "Add service %p to program %p...\n", new_si_entry, pgm);
            llist_append(pgm->services, lp);
            pgm->service_count++;
        }
        {
            LINK *lp = llist_mklink((void *) new_si_entry);
            llist_append(ts->services, lp);
            ts->service_count++;
            MPE_LOG(MPE_LOG_TRACE7, MPE_MOD_SI,
                    "Add service %p to transport %p...\n", new_si_entry, ts);
        }
    }
    else
    {
        found = true;
    }

    mpe_mutexRelease(ts->list_lock);

    if (found)
    {
        // ServiceEntry pointers
        new_si_entry->ts_handle = (mpe_SiTransportStreamHandle) ts;
        new_si_entry->program = pgm;
        new_si_entry->source_id = SOURCEID_UNKNOWN;

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mpe_siCreateHNstreamServiceHandle> Inserting service entry in the list \n");

        if (g_SITPConditionSet == TRUE)
        {
            (ts->service_count)++;
            (ts->visible_service_count)++;
        }
        new_si_entry->ref_count++;

        *service_handle = (mpe_SiServiceHandle) new_si_entry;

        /* append new entry to the dynamic list */
        if(!inTheList)
        {
            if (g_si_entry == NULL)
            {
                g_si_entry = new_si_entry;
            }
            else
            {
                walker = g_si_entry;
                while (walker)
                {
                    if (walker->next == NULL)
                    {
                        walker->next = new_si_entry;
                        break;
                    }
                    walker = walker->next;
                }
            }
        }
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siCreateHNstreamServiceHandle> done, returning MPE_SI_SUCCESS\n");
    return MPE_SI_SUCCESS;
}

/**
*This method is specifically called  HN Stream session needs to
*be filted on thereby starting PSI acquisition.
 */
mpe_Error mpe_siRegisterForHNPSIAcquisition(mpe_HnStreamSession session,
                                            mpe_SiServiceHandle *service_handle,
                                            mpe_SiTransportStreamHandle *ts_handle)
{
    mpe_Error err = MPE_SI_SUCCESS;
    mpe_SiTableEntry *si_entry = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siRegisterForHNPSIAcquisition> session:0x%x\n",
            session);

    if ((session == MPE_SI_INVALID_HANDLE) || (service_handle == NULL) ||
        (ts_handle == NULL) )
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    // Acquire HN mutex
    mpe_mutexAcquire(g_HN_mutex);

    // Create a new service handle or get an existing one
    err = mpe_siCreateHNstreamServiceHandle(session, PROGRAM_NUMBER_UNKNOWN,
                NULL, NULL, service_handle);

    si_entry = (mpe_SiTableEntry *) (*service_handle);

    if(err == MPE_SUCCESS)
    {
       MPE_LOG(MPE_LOG_DEBUG,
                   MPE_MOD_SI,
                   "<mpe_siRegisterForHNPSIAcquisition> attach count=%d *service_handle=0x%x, service_handle=0x%x, \n",
                        si_entry->hn_attach_count, *service_handle, service_handle);

        // Check if this is the first time we are attaching
        if (si_entry->hn_attach_count == 0)
        {
            mpe_SiProgramInfo *pi = NULL;

            // set TSID status
            if (si_entry->ts_handle != MPE_SI_INVALID_HANDLE)
                mpe_siSetTSIdStatusAvailableShortly(si_entry->ts_handle);

            pi = (mpe_SiProgramInfo *) si_entry->program;
            // Is this the right place to set this status?
            if (pi && (pi->pmt_status == PMT_NOT_AVAILABLE))
            {
                pi->pmt_status = PMT_AVAILABLE_SHORTLY;

                MPE_LOG(
                        MPE_LOG_DEBUG,
                        MPE_MOD_SI,
                        "<mpe_siRegisterForHNPSIAcquisition> PMT status set to available shortly...program entry: 0x%x\n",
                        pi);
            }

            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_SI,
                    "<mpe_siRegisterForHNPSIAcquisition> calling attachHnStreamSession sessionI:0x%x\n",
                    session);

            err = attachHnStreamSession(session);
            // Notify SITP to start filtering on HN Stream
            if (err != MPE_SI_SUCCESS)
            {
                mpe_siRevertTSIdStatus(si_entry->ts_handle);
                pi->pmt_status = PMT_NOT_AVAILABLE;
            }

        }

        if (err == MPE_SUCCESS)
        {
            *ts_handle = si_entry->ts_handle;
            si_entry->hn_attach_count++;
        }
        else
        {
            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_SI,
                    "<mpe_siRegisterForHNPSIAcquisition> FAILED attachHnStreamSession session:0x%x\n",
                    session);
        }
    }

    mpe_mutexRelease(g_HN_mutex);
    // Release HN mutex

    return err;
}

/**
*This method is specifically called when HN Stream session needs
*to be closed.
 */
mpe_Error mpe_siUnRegisterForHNPSIAcquisition(mpe_HnStreamSession session)
{
    mpe_SiServiceHandle service_handle = MPE_SI_INVALID_HANDLE;
    mpe_SiTableEntry *si_entry = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siUnRegisterForHNPSIAcquisition> session:0x%x\n",
            session);

    if (session == MPE_SI_INVALID_HANDLE)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    // Acquire HN mutex
    mpe_mutexAcquire(g_HN_mutex);

    (void) si_getServiceHandleByHNSession(session, &service_handle);

    si_entry = (mpe_SiTableEntry *) service_handle;

    if (si_entry == NULL ||
        si_entry->hn_attach_count == 0)
    {
        mpe_mutexRelease(g_HN_mutex);

            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siUnRegisterForHNPSIAcquisition> This should never happen!!! si_entry: %x\n",
            si_entry);

            if(si_entry)
            {
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mpe_siUnRegisterForHNPSIAcquisition> This should never happen!!! attachcnt: %d\n",
                si_entry->hn_attach_count);
            }

        // This should never happen!!
        return MPE_SI_SUCCESS;
    }

    // decrement the count, if we reach zero remove it from SITP
    si_entry->hn_attach_count--;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siUnRegisterForHNPSIAcquisition> hn_attach_count: %d\n",
            si_entry->hn_attach_count);

    if (si_entry->hn_attach_count == 0)
    {
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_SI,
                "<mpe_siUnRegisterForHNPSIAcquisition> calling detachHnStreamSession session:0x%x\n",
                session);

        // Notify SITP to close down
        (void) detachHnStreamSession(session);

        //TODO
        //Need to establish how/when dynamic MPE Service object is removed
        //short term do it here
        mpe_siReleaseServiceHandle(service_handle);
    }

    mpe_mutexRelease(g_HN_mutex);
    // Release mutex

    return MPE_SI_SUCCESS;
}

/**
 * Create a dynamic service entry given frequency, program_number, qam mode
 *
 * <i>mpe_siCreateDynamicServiceHandle()</i>
 *
 *
 * @param freq is the carrier frequency that a virtual channel is carried on.
 * @param prog_num is a value that identifies a program with in a service.
 * @param modFormat is the modulation format
 * @param service_handle output param to populate the handle
 *
 * @return MPE_SI_SUCCESS if successfully created the handle, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siCreateDynamicServiceHandle(uint32_t freq, uint32_t prog_num,
        mpe_SiModulationMode modFormat, mpe_SiServiceHandle *service_handle)
{
    mpe_SiTableEntry *walker = NULL;
    mpe_SiTableEntry *new_si_entry = NULL;
    mpe_Error err = MPE_SI_SUCCESS;
    mpe_Bool found = false;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siCreateDynamicServiceHandle> %d:0x%x:%d\n", freq, prog_num,
            modFormat);

    /* Parameter check */
    if (service_handle == NULL)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    /* Clear out the value */
    *service_handle = MPE_SI_INVALID_HANDLE;

    if (freq == MPE_SI_OOB_FREQUENCY)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mpe_siCreateDynamicServiceHandle> OOB frequency\n");
    }
    else if (freq == MPE_SI_HN_FREQUENCY)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mpe_siCreateDynamicServiceHandle> HN frequency\n");
    }
    else if (freq == MPE_SI_DSG_FREQUENCY)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mpe_siCreateDynamicServiceHandle> DSG frequency\n");
    }
    else if ((g_frequencyCheckEnable == TRUE) && (g_SITPConditionSet == TRUE))
    {
        /* freq check */
    	/* If for some reason NIT-CDS table is not signaled we will re-open the
    	 * database after the set timeout but frequency check cannot be enforced
    	 * in that case. Can still allow dynamic service entry creation and tuning by fpq. */
        if ( ((g_minFrequency != 0) && (freq < g_minFrequency)) || // check for valid frequency
               ((g_maxFrequency != 0) && (freq > g_maxFrequency)) )
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI,
                    "<mpe_siCreateDynamicServiceHandle> invalid frequency\n");

            return MPE_SI_INVALID_PARAMETER;
        }
    }

    err = mpe_siGetServiceHandleByFrequencyModulationProgramNumber(freq,
            modFormat, prog_num, service_handle);
    if (err == MPE_SI_NOT_FOUND || err == MPE_SI_NOT_AVAILABLE || err
            == MPE_SI_NOT_AVAILABLE_YET)
    {
        mpe_SiTransportStreamEntry *ts = NULL;
        mpe_SiProgramInfo *pgm = NULL;
        mpe_Error tRetVal = MPE_SI_SUCCESS;

        // create_transport_stream_handle() returns existing, if possible.
        if ((ts
                = (mpe_SiTransportStreamEntry *) create_transport_stream_handle(
                        freq, modFormat)) == NULL)
        {
            mpeos_memFreeP(MPE_MEM_SI, new_si_entry);
            return MPE_SI_OUT_OF_MEM;
        }
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_SI,
                "<mpe_siCreateDynamicServiceHandle> transport stream entry...0x%p\n",
                ts);

        mpe_mutexAcquire(ts->list_lock);
        if (modFormat != MPE_SI_MODULATION_QAM_NTSC)
        {
            LINK *lp = llist_first(ts->programs);
            while (lp)
            {
                pgm = (mpe_SiProgramInfo *) llist_getdata(lp);
                if (pgm && pgm->program_number == prog_num)
                {
                    break;
                }
                lp = llist_after(lp);
            }

            if (lp == NULL)
            {
                pgm = create_new_program_info(ts, prog_num);
                if (pgm == NULL)
                {
                    mpe_mutexRelease(ts->list_lock);
                    return MPE_SI_OUT_OF_MEM;
                }
                // newly created pgm is attached to ts by create_new_program_info.
            }

            if (pgm)
            {
            	mpe_SiTableEntry *si_entry = NULL;
                LINK *lp1 = llist_first(pgm->services);
                while (lp1 != NULL)
                {
                	si_entry = (mpe_SiTableEntry *)llist_getdata(lp1);
                	// Find the service handle that is FPQ only
                	if(si_entry->source_id == SOURCEID_UNKNOWN)
                	{
                		new_si_entry = (mpe_SiTableEntry *) llist_getdata(lp1);
                        MPE_LOG(
                                MPE_LOG_INFO,
                                MPE_MOD_SI,
                                "<mpe_siCreateDynamicServiceHandle> found service entry 0x%x\n",
                                new_si_entry);
                		break;
                    }
                	lp1 = llist_after(lp1);
                }
            }
        }
        else
        {
            // Not sure about the need for this block?
            if (new_si_entry == NULL)
            {
                LINK *lp2 = llist_first(ts->services);
                if (lp2 != NULL)
                {
                    new_si_entry = (mpe_SiTableEntry *) llist_getdata(lp2);
                    MPE_LOG(
                            MPE_LOG_DEBUG,
                            MPE_MOD_SI,
                            "<mpe_siCreateDynamicServiceHandle> new_si_entry...0x%p\n",
                            new_si_entry);
                }
            }
        }

        if (new_si_entry == NULL)
        {
            tRetVal = mpeos_memAllocP(MPE_MEM_SI, sizeof(mpe_SiTableEntry),
                    (void **) &(new_si_entry));
            if ((MPE_SI_SUCCESS != tRetVal) || (new_si_entry == NULL))
            {
                MPE_LOG(
                        MPE_LOG_WARN,
                        MPE_MOD_SI,
                        "<mpe_siCreateDynamicServiceHandle> Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");
                mpe_mutexRelease(ts->list_lock);
                return MPE_SI_OUT_OF_MEM;
            }
            /* Initialize all the fields (some fields are set to default values as defined in spec) */
            init_si_entry(new_si_entry);
            // Link into other structs.
            if (pgm != NULL)
            {
                LINK *lp = llist_mklink((void *) new_si_entry);
                llist_append(pgm->services, lp);
                pgm->service_count++;
                //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                //        "Add service %p to program %p...pgm->service_count:%d \n", new_si_entry, pgm, pgm->service_count);
            }
            {
                LINK *lp = llist_mklink((void *) new_si_entry);
                llist_append(ts->services, lp);
                ts->service_count++;
                MPE_LOG(MPE_LOG_TRACE7, MPE_MOD_SI,
                        "Add service %p to transport %p...\n", new_si_entry, ts);
            }
            found = true;
        }
        else
        {
            //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siCreateDynamicServiceHandle> re-using SI Entry...0x%x\n", new_si_entry);
            found = true;
        }
        mpe_mutexRelease(ts->list_lock);

        if (found)
        {
            //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siCreateDynamicServiceHandle> creating new SI Entry...0x%x\n", new_si_entry);

            // ServiceEntry pointers
            new_si_entry->ts_handle = (mpe_SiTransportStreamHandle) ts;
            new_si_entry->program = pgm; // could be null.
            /* For dynamic services (ex: vod) since sourceid is unknown, just default it to SOURCEID_UNKNOWN/-1.*/
            new_si_entry->source_id = SOURCEID_UNKNOWN;

            if (g_SITPConditionSet == TRUE)
            {
                (ts->service_count)++;
                /* Increment the visible service count also
                 (channel type is not known for dynamic services, they are
                 not signaled in OOB VCT) */
                (ts->visible_service_count)++;
            }
            new_si_entry->ref_count++;

            /* append new entry to the dynamic list */
            if (g_si_entry == NULL)
            {
                g_si_entry = new_si_entry;
            }
            else
            {
                walker = g_si_entry;
                while (walker)
                {
                    if (walker->next == NULL)
                    {
                        MPE_LOG(
                                MPE_LOG_DEBUG,
                                MPE_MOD_SI,
                                "<mpe_siCreateDynamicServiceHandle> set %p->next to %p\n",
                                walker, new_si_entry);
                        walker->next = new_si_entry;
                        break;
                    }
                    walker = walker->next;
                }
            }
            /* Increment the number of services */
            g_numberOfServices++;
        }
    }
    *service_handle = (mpe_SiServiceHandle) new_si_entry;
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siCreateDynamicServiceHandle> done, returning handle: 0x%x\n", *service_handle);

    return MPE_SI_SUCCESS;
}

/*  Internal method to retrieve transport stream from the list given a frequency */
static uint32_t get_transport_stream_handle(uint32_t freq,
        mpe_SiModulationMode mode)
{
    mpe_SiTransportStreamEntry *walker = NULL;
    uint32_t retVal = MPE_SI_INVALID_HANDLE;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<get_transport_stream_handle> freq:0x%x\n", freq);
    if (freq == MPE_SI_OOB_FREQUENCY)
    {
        return get_oob_transport_stream_handle();
    }
    else if (freq == MPE_SI_HN_FREQUENCY)
    {
        return get_hn_transport_stream_handle();
    }
    else if (freq == MPE_SI_DSG_FREQUENCY)
    {
        return get_dsg_transport_stream_handle();
    }

    walker = g_si_ts_entry;
    while (walker)
    {
        if (walker->frequency == freq)
        {
            if (mode == MPE_SI_MODULATION_UNKNOWN || walker->modulation_mode
                    == mode)
            {
                retVal = (uint32_t) walker;
                break;
            }
        }
        walker = walker->next;
    }

    return retVal;
}

/*  Internal method to retrieve transport stream from the list given a frequency */
/*
 static uint32_t get_transport_stream_handle_by_freq_prog(uint32_t freq, uint32_t prog)
 {
 mpe_SiTransportStreamEntry *walker = NULL;
 uint32_t retVal = MPE_SI_INVALID_HANDLE;
 mpe_SiProgramInfo *pi = NULL;

 MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<get_transport_stream_handle> freq:0x%x prog:%d\n", freq, prog);
 if(freq == MPE_SI_OOB_FREQUENCY)
 {
 return (get_oob_transport_stream_handle());
 }

 walker = g_si_ts_entry;
 while(walker)
 {
 if(walker->frequency == freq)
 {
 LINK *lp = llist_first(walker->programs);
 while (lp)
 {
 pi = (mpe_SiProgramInfo *) llist_getdata(lp);
 // We have the writelock, so list manipulation is safe.
 if(pi)
 {
 if (pi->program_number == prog)
 {
 retVal = (uint32_t)walker;
 break;
 }
 }
 lp = llist_after(lp);
 }
 }
 walker = walker->next;
 }
 return retVal;
 }
 */

/*  Internal method to create a new transport stream given frequency and modulation
 mode if one is not found.
 */
static mpe_SiTransportStreamHandle create_transport_stream_handle(
        uint32_t freq, mpe_SiModulationMode mode)
{
    mpe_SiTransportStreamEntry *ts_entry = NULL;
    mpe_SiTransportStreamEntry *ts_walker = NULL;
    uint32_t retVal = MPE_SI_INVALID_HANDLE;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<create_transport_stream_handle> freq: %d mode:%s\n", freq,
            MOD_STRING(mode));

    if ((retVal = get_transport_stream_handle(freq, mode))
            == MPE_SI_INVALID_HANDLE)
    {
        //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "create_transport_stream_handle... \n");
        /* Create a new mpe_SiTransportStreamEntry node */
        if (MPE_SI_SUCCESS != mpeos_memAllocP(MPE_MEM_SI,
                sizeof(mpe_SiTransportStreamEntry), (void **) &(ts_entry)))
        {
            MPE_LOG(
                    MPE_LOG_WARN,
                    MPE_MOD_SI,
                    "<create_transport_stream_handle> Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");

            return retVal;
        }
        else
        {
            MPE_LOG(MPE_LOG_TRACE7, MPE_MOD_SI, "ts created @ %p\n", ts_entry);
        }

        if (MPE_SI_SUCCESS != init_transport_stream_entry(ts_entry, freq))
        {
            MPE_LOG(MPE_LOG_TRACE7, MPE_MOD_SI, "ts freed @ %p\n", ts_entry);
            mpeos_memFreeP(MPE_MEM_SI, ts_entry);
            return (uint32_t) NULL;
        }
        ts_entry->modulation_mode = mode;

        /* append new entry to the list */
        if ((g_si_ts_entry == NULL) || (g_si_ts_entry->frequency
                > ts_entry->frequency))
        {
            ts_entry->next = g_si_ts_entry;
            g_si_ts_entry = ts_entry;
        }
        else
        {
            ts_walker = g_si_ts_entry;
            while (ts_walker)
            {
                if ((ts_walker->next == NULL) || (ts_walker->next->frequency
                        > ts_entry->frequency))
                {
                    ts_entry->next = ts_walker->next;
                    ts_walker->next = ts_entry;
                    break;
                }
                ts_walker = ts_walker->next;
            }
        }

        retVal = (uint32_t) ts_entry;
    }
    else
    { // Dynamic entry created by tuning.
        if (mode != MPE_SI_MODULATION_UNKNOWN)
        {
            if (((mpe_SiTransportStreamEntry *) retVal)->modulation_mode
                    == MPE_SI_MODULATION_UNKNOWN)
            {
                ((mpe_SiTransportStreamEntry *) retVal)->modulation_mode = mode;
            }
        }
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<create_transport_stream_handle> exit 0x%x\n", retVal);

    return retVal;
}

/*  Internal method to create a new program info structure given a transport stream and
 program number.
 */

static mpe_SiProgramInfo *create_new_program_info(
        mpe_SiTransportStreamEntry *ts_entry, uint32_t prog_num)
{
    mpe_SiProgramInfo *pi = NULL;

    if (ts_entry == NULL)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                "<create_new_program_info> Cannot create program for NULL transport stream!\n");
    }
    else
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<create_new_program_info> Program number: %d for TS %d!\n",
                prog_num, ts_entry->frequency);

        if (prog_num > 65535)
        {
            MPE_LOG(
                    MPE_LOG_WARN,
                    MPE_MOD_SI,
                    "<create_new_program_info> %d is not a valid program number!!\n",
                    prog_num);
        }
        else
        {
            if (MPE_SI_SUCCESS != mpeos_memAllocP(MPE_MEM_SI,
                    sizeof(mpe_SiProgramInfo), (void **) &(pi)))
            {
                MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                        "<create_new_program_info> Mem allocation failed, returning NULL...\n");
            }
            else
            {
                if (init_program_info(pi) != MPE_SI_SUCCESS)
                {
                    MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                            "<create_new_program_info> Initialization failed, returning NULL...\n");
                    mpeos_memFreeP(MPE_MEM_SI, pi);
                    pi = NULL;
                }
                else
                {
                    LINK *lp;
                    pi->program_number = prog_num;
                    pi->stream = ts_entry;
                    lp = llist_mklink((void *) pi);
                    llist_append(ts_entry->programs, lp);
                    ts_entry->program_count++;
                    MPE_LOG(
                            MPE_LOG_DEBUG,
                            MPE_MOD_SI,
                            "<create_new_program_info> ts_entry->program_count %d\n",
                            ts_entry->program_count);
                }
            }
        }
    }
    return pi;
}

/*  Retrieve OOB transport stream handle. This method creates a new handle
 if one is not already created.
 */
static uint32_t get_oob_transport_stream_handle(void)
{
    uint32_t retVal = 0;

    if (g_si_oob_ts_entry == NULL)
    {
        /* Create a new mpe_SiTransportStreamEntry node */
        if (MPE_SI_SUCCESS != mpeos_memAllocP(MPE_MEM_SI,
                sizeof(mpe_SiTransportStreamEntry),
                (void **) &(g_si_oob_ts_entry)))
        {
            MPE_LOG(
                    MPE_LOG_WARN,
                    MPE_MOD_SI,
                    "<get_oob_transport_stream_handle> Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");

            return retVal;
        }

        if (MPE_SI_SUCCESS != init_transport_stream_entry(g_si_oob_ts_entry,
                MPE_SI_OOB_FREQUENCY))
        {
            mpeos_memFreeP(MPE_MEM_SI, g_si_oob_ts_entry);
            g_si_oob_ts_entry = NULL;
            return 0;
        }

    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<get_oob_transport_stream_handle> g_si_oob_ts_entry:0x%p\n",
            g_si_oob_ts_entry);
    return (uint32_t) g_si_oob_ts_entry;
}

/*  Retrieve DSG transport stream handle. This method creates a new handle
 if one is not already created.
 */
static uint32_t get_dsg_transport_stream_handle(void)
{
    uint32_t retVal = 0;

    if (g_si_dsg_ts_entry == NULL)
    {
        /* Create a new mpe_SiTransportStreamEntry node */
        if (MPE_SI_SUCCESS != mpeos_memAllocP(MPE_MEM_SI,
                sizeof(mpe_SiTransportStreamEntry),
                (void **) &(g_si_dsg_ts_entry)))
        {
            MPE_LOG(
                    MPE_LOG_WARN,
                    MPE_MOD_SI,
                    "<get_dsg_transport_stream_handle> Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");

            return retVal;
        }

        if (MPE_SI_SUCCESS != init_transport_stream_entry(g_si_dsg_ts_entry,
                MPE_SI_DSG_FREQUENCY))
        {
            mpeos_memFreeP(MPE_MEM_SI, g_si_dsg_ts_entry);
            g_si_dsg_ts_entry = NULL;
            return 0;
        }

    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<get_dsg_transport_stream_handle> g_si_dsg_ts_entry:0x%p\n",
            g_si_dsg_ts_entry);
    return (uint32_t) g_si_dsg_ts_entry;
}

/*  Retrieve HN transport stream handle. This method creates a new handle
 if one is not already created.
 */
static uint32_t get_hn_transport_stream_handle(void)
{
    uint32_t retVal = 0;

    if (g_si_HN_ts_entry == NULL)
    {
        /* Create a new mpe_SiTransportStreamEntry node */
        if (MPE_SI_SUCCESS != mpeos_memAllocP(MPE_MEM_SI,
                sizeof(mpe_SiTransportStreamEntry),
                (void **) &(g_si_HN_ts_entry)))
        {
            MPE_LOG(
                    MPE_LOG_WARN,
                    MPE_MOD_SI,
                    "<get_hn_transport_stream_handle> Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");

            return retVal;
        }

        if (MPE_SI_SUCCESS != init_transport_stream_entry(g_si_HN_ts_entry,
                MPE_SI_HN_FREQUENCY))
        {
            mpeos_memFreeP(MPE_MEM_SI, g_si_HN_ts_entry);
            g_si_HN_ts_entry = NULL;
            return 0;
        }

        /* update the source type */
        g_si_HN_ts_entry->source_type = MPE_SOURCE_TYPE_HN_STREAM;

    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<get_hn_transport_stream_handle> g_si_HN_ts_entry:0x%x\n",
            g_si_HN_ts_entry);
    return (uint32_t) g_si_HN_ts_entry;
}

/* Returns the DSG transport stream handle. */
uint32_t mpe_siGetDSGTransportStreamHandle(void)
{
    return get_dsg_transport_stream_handle();
}

/* Returns the HN transport stream handle. */
uint32_t mpe_siGetHNTransportStreamHandle(void)
{
    return get_hn_transport_stream_handle();
}

/**
 * Acquire a service handle given service name
 *
 * <i>mpe_siGetServiceHandleByServiceName()</i>
 *
 *
 * @param service_name is a unique name assigned to a service.
 * @param service_handle output param to populate the handle
 *
 * @return MPE_SI_SUCCESS if successfully acquired the handle, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetServiceHandleByServiceName(char* service_name,
        mpe_SiServiceHandle *service_handle)
{
    mpe_SiTableEntry *walker = NULL;
    mpe_siSourceNameEntry *sn_walker = g_si_sourcename_entry;

    /* Parameter check */
    if ((service_handle == NULL) || (service_name == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    /* Clear out the value */
    *service_handle = MPE_SI_INVALID_HANDLE;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetServiceHandleByServiceName> %s\n", service_name);

    // If SI DB is in either of these states return SI_NOT_AVAILABLE_YET
    // SI_ACQUIRING state is not useful for callers
    if ((g_si_state == SI_ACQUIRING) || (g_si_state == SI_NOT_AVAILABLE_YET))
        return MPE_SI_NOT_AVAILABLE_YET;
    else if (g_si_state == SI_DISABLED)
        return MPE_SI_NOT_AVAILABLE;

    /*  Note: Service name (source name) comes from NTT_SNS.
     This table contains a sourceId-to-sourceName mapping.
     Hence acquiring a handle based on service name will likely
     only be found in the main tree where sourceIds are stored.
     */
    /* Walk through the list of services to find a matching service name (in any language). */
    while (sn_walker)
    {
      mpe_SiLangSpecificStringList *string_list = sn_walker->source_names;
      while (string_list != NULL)
      {
        if (strcmp(string_list->string, service_name) == 0)
        {
          mpe_SiServiceHandle handle = MPE_SI_INVALID_HANDLE;
          if(sn_walker->appType)
          {
              mpe_siGetServiceHandleByAppId(sn_walker->id, &handle);
          }
          else
          {
              // State of the entry is checked in mpe_siGetServiceHandleBySourceId
              // It should be either mapped or defined_mapped
              mpe_siGetServiceHandleBySourceId(sn_walker->id, &handle);
          }

          if (handle != MPE_SI_INVALID_HANDLE)
          {
              walker = (mpe_SiTableEntry *)handle;
              *service_handle = handle;
              walker->ref_count++;
              return MPE_SI_SUCCESS;
          }
        }
        string_list = string_list->next;
      }
      sn_walker = sn_walker->next;
    }

    /* Matching service name is not found, return error */
    return MPE_SI_NOT_FOUND;
}

/**
 * Acquire lock for reading
 *
 * <i>mpe_siLockForRead()</i>
 *
 * @return MPE_SI_SUCCESS if successfully acquired the lock, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siLockForRead()
{
    mpe_Error result = MPE_SUCCESS;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siLockForRead> \n");

    /* Acquire global mutex */
    mpe_mutexAcquire(g_global_si_mutex);

    /*
     For read only purposes acquire the lock if no module is
     currently reading. If some module is reading, just increment
     the counter.
     */
    if (g_cond_counter == 0)
    {
        mpe_condGet(g_global_si_cond);
    }

    /* increment the counter in any case */
    g_cond_counter++;

    mpe_mutexRelease(g_global_si_mutex);
    /* Release global mutex */

    //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siLockForRead> done\n");

    return result;
}

/**
 * Release the read lock
 *
 * <i>mpe_siUnLock()</i>
 *
 * @return MPE_SI_SUCCESS if successfully released the lock, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siUnLock()
{
    mpe_Error result = MPE_SUCCESS;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siUnLock> \n");

    /*  Multiple modules can be reading at the
     same time from any given Si handle. The read
     counter is incremented every time a read
     is requested (by mpe_siLockForRead).
     The same counter is decremented when handle is
     released. If the read counter reaches zero then
     the condition is also released.
     */

    /* Acquire global mutex */
    mpe_mutexAcquire(g_global_si_mutex);

    if (g_cond_counter == 0)
    {
        result = MPE_SI_LOCKING_ERROR;
    }
    else
    {
        g_cond_counter--;

        if (g_cond_counter == 0)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                    "Lock count has hit zero; releasing g_global_si_cond.\n");
            mpe_condSet(g_global_si_cond);
        }
    }

    /* Release global mutex */
    mpe_mutexRelease(g_global_si_mutex);

    return result;
}

#if 0
mpe_Error mpe_siUnLock()
{
    mpe_Error result = MPE_SUCCESS;
    mpe_Bool releaseFlag = FALSE;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siUnLock> \n");

    /*  Multiple modules can be reading at the
     same time from any given Si handle. The read
     counter is incremented every time a read
     is requested (by mpe_siLockForRead).
     The same counter is decremented when handle is
     released. If the read counter reaches zero then
     the condition is also released.
     */

    /* Acquire global mutex */
    mpe_mutexAcquire(g_global_si_mutex);

    if (g_cond_counter > 0)
    {
        g_cond_counter--;
    }

    if (g_cond_counter == 0)
    {
        releaseFlag = TRUE;
    }

    /* Release global mutex */
    mpe_mutexRelease(g_global_si_mutex);

    if (releaseFlag)
    {
        mpe_condSet(g_global_si_cond);
        releaseFlag = FALSE;
    }

    return MPE_SI_SUCCESS;
}
#endif

/*
 This method is called by SITP when all the OOB tables
 have been read atleast once. This condition once set, remains
 in that state. [ no longer true --mlb 12/21/2005
 see mpe_siUnsetGlobalState() ]
 */
void mpe_siSetGlobalState(mpe_SiGlobalState state)
{
    MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "mpe_siSetGlobalState() state: %d\n",
            state);
    /*
     signal the condition to set state
     (allow readers to read from SI DB)
     */
    g_si_state = state;
    if (state == SI_NIT_SVCT_ACQUIRED)
    {
    	// Since at this point SI is still not 'FULLY' acquired
    	// we will keep the internal state as 'ACQUIRING'.
    	// Also with the changes made to improve initial SI acquisition
    	// (setting all OOB SI filters at the same time at start-up)
    	// the state will be seen as 'SI_ACQUIRING' for only a brief time.
    	g_si_state =  SI_ACQUIRING;
    }
    else if (state == SI_FULLY_ACQUIRED)
    {
        mpeos_condSet(g_SITP_cond);
        g_SITPConditionSet = TRUE;
    }

    send_si_event(si_state_to_event(state), 0, 0);
}

/**
 * Register for SI events
 *
 * <i>mpe_siRegisterForSIEvents()</i>
 *
 *
 * @param queue is a queue handle to post SI events to
 * @param edHandle the edHandle pointer that uniquely identifies this
 * request for events.  SIDB uses the termination policy specified in
 * the edHandle to automatically unregister clients when their
 * termination conditions have been reached.
 * @param frequency Some clients only want to receive in-band SI notifications
 * for certain services.  In this case, clients can pass the frequency
 * of the service they are interested in.  Pass 0 if you want notifications
 * for all services.
 * @param programNumber Some clients only want to receive in-band SI notifications
 * for certain services.  In this case, clients can pass the programNumber
 * of the service they are interested in.  Pass 0 if you want notifications
 * for all services or for PAT requests.
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siRegisterForSIEvents(mpe_EdEventInfo *edHandle,
        uint32_t frequency, uint32_t programNumber)
{
    mpe_SiRegistrationListEntry *walker = NULL;
    mpe_SiRegistrationListEntry *new_list_member = NULL;

    if (edHandle == NULL)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    if (mpeos_memAllocP(MPE_MEM_SI, sizeof(mpe_SiRegistrationListEntry),
            (void **) &new_list_member) != MPE_SUCCESS)
    {
        return MPE_SI_OUT_OF_MEM;
    }

    new_list_member->edHandle = edHandle;
    new_list_member->next = NULL;
    new_list_member->terminationEvent = 0;
    new_list_member->frequency = frequency;
    new_list_member->programNumber = programNumber;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siRegisterForSIEvents> called...with edHandle: 0x%p\n",
            edHandle);

    /* Assign this client's termination code.  In order to ensure that ED properly frees the
     edHandle data structure, we have created our own event type (MPE_SI_EVENT_TERMINATE_SESSION)
     that is meant to signal the end of notification for the client.  Once the client's original
     termination condition is met, we send MPE_SI_EVENT_TERMINATED_SESSION, which will cause ED to
     free the edHandle data structure.  The client's original desired behavior is found in the
     edHandle structure passed in to this function.
     ____________________________________________________________________________________________
     -- For ONESHOT termination, the client is indicating that only one event (of any type) is
     to be receieved.  Therefore we do not need to make any modifications.  SIDB will unregister
     this client and the ED mechanism will deallocate the edHandle structure upon receipt of the
     first event.
     -- For EVCODE termination, we will store the client's original requested event code in the
     SIDB registration list and change the edHandle code to our custom SI termination code
     (MPE_SI_EVENT_TERMINATE_SESSION).  SIDB will send the SI termination code (and unregister
     the client) directly after the client's requested event is sent
     -- For OPEN termination, we modify the edHandle termination type to be EVCODE and the edHandle
     termination event to be MPE_SI_EVENT_TERMINATE_SESSION so that ED will free the edHandle data
     structure once we send the SI termination event.  We also set the termination code in the SIDB
     registration structure to be MPE_SI_EVENT_TERMINATE_SESSION -- SITP will never send this event
     to SIDB, therefore we are ensure that this client will not be prematurely unregistered.  The SI
     termination event will be sent only when mpe_siUnRegisterForSIEvents() is called.
     */
    if (edHandle->terminationType == MPE_ED_TERMINATION_EVCODE)
    {
        new_list_member->terminationEvent = edHandle->terminationCode;
        edHandle->terminationCode = MPE_SI_EVENT_TERMINATE_SESSION;
    }
    else if (edHandle->terminationType == MPE_ED_TERMINATION_OPEN)
    {
        edHandle->terminationType = MPE_ED_TERMINATION_EVCODE;
        edHandle->terminationCode = MPE_SI_EVENT_TERMINATE_SESSION;
        new_list_member->terminationEvent = MPE_SI_EVENT_TERMINATE_SESSION;
    }

    /* Acquire registration list mutex */
    mpe_mutexAcquire(g_registration_list_mutex);

    if (g_si_registration_list == NULL)
    {
        g_si_registration_list = new_list_member;
    }
    else
    {
        walker = g_si_registration_list;
        while (walker)
        {
            if (walker->next == NULL)
            {
                walker->next = new_list_member;
                break;
            }
            walker = walker->next;
        }
    }

    mpe_mutexRelease(g_registration_list_mutex);
    /* Release registration list mutex */

    /* Broadcast the global si state */
    send_si_event(si_state_to_event(g_si_state), 0, 0);

    return MPE_SI_SUCCESS;
}

/**
 * Un-register for SI events.  The memroy associated with the client's
 * edHandle will be deallocated automatically.
 *
 * <i>mpe_siUnRegisterForSIEvents()</i>
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siUnRegisterForSIEvents(mpe_EdEventInfo* edHandle)
{
    mpe_SiRegistrationListEntry *walker, *prev;
    mpe_SiRegistrationListEntry *to_delete = NULL;

    if (edHandle == NULL)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    /* Acquire registration list mutex */
    mpe_mutexAcquire(g_registration_list_mutex);

    /* Remove from the list */
    if (g_si_registration_list == NULL)
    {
        mpe_mutexRelease(g_registration_list_mutex);
        return MPE_EINVAL;
    }
    else if (g_si_registration_list->edHandle == edHandle)
    {
        to_delete = g_si_registration_list;
        g_si_registration_list = g_si_registration_list->next;
    }
    else
    {
        prev = walker = g_si_registration_list;
        while (walker)
        {
            if (walker->edHandle == edHandle)
            {
                to_delete = walker;
                prev->next = walker->next;
                break;
            }
            prev = walker;
            walker = walker->next;
        }
    }

    mpe_mutexRelease(g_registration_list_mutex);
    /* Release registration list mutex */

    if (to_delete != NULL)
    {
        // Do not send the SI termination event to ONESHOT ED clients
        if (to_delete->edHandle->terminationType != MPE_ED_TERMINATION_ONESHOT)
        {
            // Post the event that will delete the edHandle
            mpe_eventQueueSend(to_delete->edHandle->eventQ,
                    MPE_SI_EVENT_TERMINATE_SESSION, (void*) NULL,
                    (void*) to_delete->edHandle, 0);
        }

        mpeos_memFreeP(MPE_MEM_SI, to_delete);
    }

    return MPE_SI_SUCCESS;
}

/*
 * Add this edHandle to our list of clients to unregister
 */
static void add_to_unreg_list(mpe_EdEventInfo* edHandle)
{
    // Create list entry and add it to front of global list
    mpe_SiRegistrationListEntry *newUnreg = NULL;
    if (mpeos_memAllocP(MPE_MEM_SI, sizeof(mpe_SiRegistrationListEntry),
            (void**) &newUnreg) == MPE_SUCCESS)
    {
        newUnreg->edHandle = edHandle;
        newUnreg->next = g_si_unreg_list;
        g_si_unreg_list = newUnreg;
    }
}

/*
 * Walk through the current unreg list and remove each handle from
 * the SIDB registration list
 */
static void unregister_clients()
{
    mpe_SiRegistrationListEntry *walker = g_si_unreg_list;

    while (walker)
    {
        (void) mpe_siUnRegisterForSIEvents(walker->edHandle);
        walker = walker->next;
    }

    /* Deallocate all memory in the unreg list */
    while (g_si_unreg_list)
    {
        walker = g_si_unreg_list;
        g_si_unreg_list = g_si_unreg_list->next;
        mpeos_memFreeP(MPE_MEM_SI, walker);
    }
}

/*  This method is currently only used by MPE OC to register queue for SI change events.
 All other clients use the listener mechanism.
 */
mpe_Error mpe_siRegisterQueueForSIEvents(mpe_EventQueue queue)
{
    mpe_SIQueueListEntry *walker = NULL;
    mpe_SIQueueListEntry *new_list_member = NULL;
    mpe_Error retVal = MPE_SI_SUCCESS;

    if (mpeos_memAllocP(MPE_MEM_SI, sizeof(mpe_SIQueueListEntry),
            (void **) &new_list_member) != MPE_SUCCESS)
    {
        retVal = MPE_SI_OUT_OF_MEM;
    }
    else
    {

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mpe_siRegisterQueueForSIEvents> called...with queue: 0x%x\n",
                queue);

        new_list_member->queueId = queue;
        new_list_member->next = NULL;

        /* Acquire registration list mutex */
        mpe_mutexAcquire(g_registration_list_mutex);

        if (g_si_queue_list == NULL)
        {
            g_si_queue_list = new_list_member;
        }
        else
        {
            walker = g_si_queue_list;
            while (walker)
            {
                if (walker->next == NULL)
                {
                    walker->next = new_list_member;
                    break;
                }
                walker = walker->next;
            }
        }

        mpe_mutexRelease(g_registration_list_mutex);
        /* Release registration list mutex */
    }

    return retVal;
}

/*  Method called by MPE OC to unregister queue. */
mpe_Error mpe_siUnRegisterQueue(mpe_EventQueue queue)
{
    mpe_SIQueueListEntry *walker, *prev;
    mpe_SIQueueListEntry *to_delete = NULL;

    /* Acquire registration list mutex */
    mpe_mutexAcquire(g_registration_list_mutex);

    /* Remove from the list */
    if (g_si_queue_list == NULL)
    {
        mpe_mutexRelease(g_registration_list_mutex);
        return MPE_EINVAL;
    }
    else if (g_si_queue_list->queueId == queue)
    {
        to_delete = g_si_queue_list;
        g_si_queue_list = g_si_queue_list->next;
    }
    else
    {
        prev = walker = g_si_queue_list;
        while (walker)
        {
            if (walker->queueId == queue)
            {
                to_delete = walker;
                prev->next = walker->next;
                break;
            }
            prev = walker;
            walker = walker->next;
        }
    }

    mpe_mutexRelease(g_registration_list_mutex);
    /* Release registration list mutex */

    if (to_delete != NULL)
    {
        mpeos_memFreeP(MPE_MEM_SI, to_delete);
    }

    return MPE_SI_SUCCESS;
}

mpe_Error mpe_siValidateTransportHandle(mpe_SiTransportHandle transport_handle)
{
    mpe_SiTransportStreamEntry *walker = NULL;
    mpe_SiTransportStreamEntry *target =
            (mpe_SiTransportStreamEntry *) transport_handle;
    mpe_Error retVal = MPE_SI_NOT_FOUND;

    /* Parameter check */
    if (transport_handle != MPE_SI_INVALID_HANDLE)
    {
        if ((mpe_SiTransportStreamEntry *) transport_handle
                == g_si_oob_ts_entry)
        {
            retVal = MPE_SI_SUCCESS;
        }
        else if ((mpe_SiTransportStreamEntry *) transport_handle
                == g_si_HN_ts_entry)
        {
            retVal = MPE_SI_SUCCESS;
        }
        else if ((mpe_SiTransportStreamEntry *) transport_handle
                == g_si_dsg_ts_entry)
        {
            retVal = MPE_SI_SUCCESS;
        }
        else
        {
            walker = g_si_ts_entry;
            while (walker)
            {
                if (walker == target)
                {
                    retVal = MPE_SI_SUCCESS;
                    break;
                }
                walker = walker->next;
            }
        }
    }
    if (retVal != MPE_SI_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI,
                "mpe_siValidateTransportHandle: Handle %p did not validate!\n",
                (void *) transport_handle);
    }
    return retVal;
}

mpe_Error mpe_siValidateServiceHandle(mpe_SiServiceHandle service_handle)
{
    mpe_SiTableEntry *walker = NULL;
    mpe_SiTableEntry *target = (mpe_SiTableEntry *) service_handle;
    mpe_Error retVal = MPE_SI_NOT_FOUND;

    /* Parameter check */
    if (service_handle != MPE_SI_INVALID_HANDLE)
    {
        walker = g_si_entry;
        while (walker)
        {
            if (walker == target)
            {
                retVal = MPE_SI_SUCCESS;
                break;
            }
            walker = walker->next;
        }
    }
    if (retVal != MPE_SI_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI,
                "mpe_siValidateServiceHandle: Handle %p did not validate!\n",
                (void *) service_handle);
    }
    return retVal;
}

static void notify_registered_queues(mpe_Event event, uint32_t si_handle)
{
    mpe_SIQueueListEntry *walker = NULL;
    walker = g_si_queue_list;

    while (walker)
    {
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_SI,
                "<notify_registered_queues> Sending event 0x%x to queue 0x%x\n",
                event, walker->queueId);
        mpe_eventQueueSend(walker->queueId, event, (void*) si_handle,
                (void*) NULL, 0);

        walker = walker->next;
    }
}

/*
 Called from SITP when a table is acquired/updated. SI DB uses it to
 deliver events to registered callers.
 */
mpe_Error mpe_siNotifyTableChanged(mpe_SiTableType table_type,
        uint32_t changeType, uint32_t optional_data)
{
    mpe_SiRegistrationListEntry *walker = NULL;
    mpe_Event event = 0;
    uint32_t eventFrequency = 0;
    uint32_t eventProgramNumber = 0;
    uint32_t optionalEventData3 = changeType;

    if (table_type == 0) /* unknown table type */
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siNotifyTableChanged> called...with tableType: %d, changeType: %d optional_data: 0x%x\n",
            table_type, changeType, optional_data);

    switch (table_type)
    {
    case OOB_SVCT_VCM:
    {
        MPE_LOG(
                MPE_LOG_INFO,
                MPE_MOD_SI,
                "<mpe_siNotifyTableChanged> called...with tableType: OOB_SVCT (VCM), changeType: %d\n",
                changeType);
        if (changeType == MPE_SI_CHANGE_TYPE_ADD)
        {
            //event = MPE_SI_EVENT_OOB_VCT_ACQUIRED;
        }
        else if (changeType == MPE_SI_CHANGE_TYPE_MODIFY)
        {
            event = MPE_SI_EVENT_OOB_VCT_ACQUIRED;
            break;
        }
        else if (changeType == MPE_SI_CHANGE_TYPE_REMOVE)
        {
            // No support for removal of SVCT
        }
    }
        break;
    case OOB_SVCT_DCM:
    {
        MPE_LOG(
                MPE_LOG_INFO,
                MPE_MOD_SI,
                "<mpe_siNotifyTableChanged> called...with tableType: OOB_SVCT (DCM), changeType: %d\n",
                changeType);
        if (changeType == MPE_SI_CHANGE_TYPE_ADD)
        {
            //event = MPE_SI_EVENT_OOB_VCT_ACQUIRED;
        }
        else if (changeType == MPE_SI_CHANGE_TYPE_MODIFY)
        {
            event = MPE_SI_EVENT_OOB_VCT_ACQUIRED;
            break;
        }
        else if (changeType == MPE_SI_CHANGE_TYPE_REMOVE)
        {
            // No support for removal of SVCT
        }
    }
        break;
    case OOB_NTT_SNS:
    {
        MPE_LOG(
                MPE_LOG_INFO,
                MPE_MOD_SI,
                "<mpe_siNotifyTableChanged> called...with tableType: OOB_NTT_SNS, changeType: %d\n",
                changeType);
        switch (changeType)
        {
        case MPE_SI_CHANGE_TYPE_ADD:
        {
            break;
        }
        case MPE_SI_CHANGE_TYPE_MODIFY:
        {
            break;
        }
        case MPE_SI_CHANGE_TYPE_REMOVE:
        { // No support for removal of SVCT
            break;
        }
        default:
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_SI,
                    "mpe_siNotifyTableChanged: Unknown SVCT changeType (%d)!\n",
                    changeType);
            break;
        }
        } // END switch (changeType)
        break;
    }
    case OOB_NIT_CDS:
    {
        int i = 0, tempCount = 0;
        uint32_t tempArray[255];
        MPE_LOG(
                MPE_LOG_INFO,
                MPE_MOD_SI,
                "<mpe_siNotifyTableChanged> called...with tableType: OOB_NIT_CDS, changeType: %d\n",
                changeType);
        for (i = 0; i < 255; i++)
        {
            tempArray[i] = 0;
        }
        i = 0;
        while (i < 255)
        {
            if (g_frequency[i + 1] != 0)
            {
                tempArray[i] = g_frequency[i + 1];
                tempCount++;
            }
            i++;
        }
        // If NIT-CDS table is not signaled for some reason this method
        // will still be called after the set timeout, but g_frequency
        // array will not be set hence sorting is not needed.
        // g_minFrequency and g_maxFrequency will remain at 0.
        if(tempCount != 0)
        {
            quickSortArray(tempArray, tempCount);
            printSortedArray(tempArray, tempCount);
            g_minFrequency = tempArray[0];
            g_maxFrequency = tempArray[tempCount - 1];
            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_SI,
                    "<mpe_siNotifyTableChanged> g_minFrequency: %d, g_maxFrequency: %d\n",
                    g_minFrequency, g_maxFrequency);
        }

        if (changeType == MPE_SI_CHANGE_TYPE_ADD)
        {
            mpeos_timeGetMillis(&gtime_transport_stream);
        }
        else if (changeType == MPE_SI_CHANGE_TYPE_MODIFY)
        {

            mpeos_timeGetMillis(&gtime_transport_stream);
            update_transport_stream_handles();
        }
        else if (changeType == MPE_SI_CHANGE_TYPE_REMOVE)
        {

        }
        //event = MPE_SI_EVENT_OOB_NIT_ACQUIRED;
    }
        break;
    case OOB_NIT_MMS:
    {
        MPE_LOG(
                MPE_LOG_INFO,
                MPE_MOD_SI,
                "<mpe_siNotifyTableChanged> called...with tableType: OOB_NIT_MMS, changeType: %d\n",
                changeType);
        if (changeType == MPE_SI_CHANGE_TYPE_ADD)
        {
        }
        else if (changeType == MPE_SI_CHANGE_TYPE_MODIFY)
        {
            update_transport_stream_handles();
        }
        else if (changeType == MPE_SI_CHANGE_TYPE_REMOVE)
        {

        }
    }
        break;

    case IB_PAT:
    {
        mpe_SiTransportStreamEntry *ts =
                (mpe_SiTransportStreamEntry*) optional_data;
        if (ts != NULL)
        {
            eventFrequency = ts->frequency;
            MPE_LOG(
                    MPE_LOG_INFO,
                    MPE_MOD_SI,
                    "<mpe_siNotifyTableChanged> IB_PAT event, ts_handle: 0x%p\n",
                    ts);
            if (changeType == MPE_SI_CHANGE_TYPE_ADD)
            {
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                        "<mpe_siNotifyTableChanged> Sending MPE_SI_EVENT_IB_PAT_ACQUIRED event\n");
                event = MPE_SI_EVENT_IB_PAT_ACQUIRED;
            }
            else if (changeType == MPE_SI_CHANGE_TYPE_MODIFY)
            {
                MPE_LOG(
                        MPE_LOG_DEBUG,
                        MPE_MOD_SI,
                        "<mpe_siNotifyTableChanged> Sending MPE_SI_EVENT_IB_PAT_UPDATE event, changeType: MPE_SI_CHANGE_TYPE_MODIFY\n");
                event = MPE_SI_EVENT_IB_PAT_UPDATE;
            }
            else if (changeType == MPE_SI_CHANGE_TYPE_REMOVE)
            {
                MPE_LOG(
                        MPE_LOG_DEBUG,
                        MPE_MOD_SI,
                        "<mpe_siNotifyTableChanged> Sending MPE_SI_EVENT_IB_PAT_UPDATE event, changeType: MPE_SI_CHANGE_TYPE_REMOVE\n");
                event = MPE_SI_EVENT_IB_PAT_UPDATE; //??
            }
        }
        else
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_SI,
                    "<mpe_siNotifyTableChanged> NULL transport_stream handle passed for MPE_SI_EVENT_IB_PAT_UPDATE event!\n");
            event = 0;
        }
    }
        break;
    case IB_PMT:
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mpe_siNotifyTableChanged> IB_PMT event\n");
        if (changeType == MPE_SI_CHANGE_TYPE_ADD)
        {
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,
                    "<mpe_siNotifyTableChanged> Sending MPE_SI_EVENT_IB_PMT_ACQUIRED event\n");
            event = MPE_SI_EVENT_IB_PMT_ACQUIRED;
        }
        else if (changeType == MPE_SI_CHANGE_TYPE_MODIFY)
        {
            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_SI,
                    "<mpe_siNotifyTableChanged> Sending MPE_SI_EVENT_IB_PMT_UPDATE event, changeType: MPE_SI_CHANGE_TYPE_MODIFY\n");
            event = MPE_SI_EVENT_IB_PMT_UPDATE;
        }
        else if (changeType == MPE_SI_CHANGE_TYPE_REMOVE)
        {
            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_SI,
                    "<mpe_siNotifyTableChanged> Sending MPE_SI_EVENT_IB_PMT_UPDATE event, changeType: MPE_SI_CHANGE_TYPE_REMOVE\n");
            event = MPE_SI_EVENT_IB_PMT_UPDATE;
        }
        // Get the frequency and program number associated with the serviceHandle
        // provided by SITP
        {
            mpe_Error err;
            mpe_SiServiceHandle serviceHandle =
                    (mpe_SiServiceHandle) optional_data;

            err = mpe_siGetFrequencyForServiceHandle(serviceHandle,
                    &eventFrequency);
            if (err != MPE_SUCCESS)
            {
                MPE_LOG(
                        MPE_LOG_DEBUG,
                        MPE_MOD_JNI,
                        "<mpe_siNotifyTableChanged> Could not get frequency!  returned: 0x%x\n",
                        err);
                event = 0;
            }
            err = mpe_siGetProgramNumberForServiceHandle(serviceHandle,
                    &eventProgramNumber);
            if (err != MPE_SUCCESS)
            {
                MPE_LOG(
                        MPE_LOG_DEBUG,
                        MPE_MOD_JNI,
                        "<mpe_siNotifyTableChanged> Could not get program number!  returned: 0x%x\n",
                        err);
                event = 0;
            }
        }
    }
        break;
    case IB_CVCT:
    {
        mpe_Error err;
        mpe_SiServiceHandle serviceHandle = (mpe_SiServiceHandle) optional_data;

        MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,
                "<mpe_siNotifyTableUpdated> Sending MPE_SI_EVENT_IB_CVCT_UPDATE event\n");
        if (changeType == MPE_SI_CHANGE_TYPE_ADD)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                    "<mpe_siNotifyTableChanged> Sending MPE_SI_EVENT_IB_CVCT_ACQUIRED event\n");
            event = MPE_SI_EVENT_IB_CVCT_ACQUIRED;
        }
        else if (changeType == MPE_SI_CHANGE_TYPE_MODIFY)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                    "<mpe_siNotifyTableChanged> Sending MPE_SI_EVENT_IB_CVCT_UPDATE event\n");
            event = MPE_SI_EVENT_IB_CVCT_UPDATE;
        }
        else if (changeType == MPE_SI_CHANGE_TYPE_REMOVE)
        {
            event = MPE_SI_EVENT_IB_CVCT_UPDATE;
        }

        // Get the frequency and program number associated with the serviceHandle
        // provided by SITP
        err
                = mpe_siGetFrequencyForServiceHandle(serviceHandle,
                        &eventFrequency);
        if (err != MPE_SUCCESS)
        {
            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_JNI,
                    "<mpe_siNotifyTableChanged> Could not get frequency!  returned: 0x%x\n",
                    err);
            event = 0;
        }

    }
        break;
    case OOB_PAT:
    {
        MPE_LOG(
                MPE_LOG_INFO,
                MPE_MOD_SI,
                "<mpe_siNotifyTableChanged> OOB_PAT event..\n");
        if (changeType == MPE_SI_CHANGE_TYPE_ADD)
        {
            event = MPE_SI_EVENT_OOB_PAT_ACQUIRED;
        }
        else if (changeType == MPE_SI_CHANGE_TYPE_MODIFY)
        {
            event = MPE_SI_EVENT_OOB_PAT_UPDATE;
        }
        else if (changeType == MPE_SI_CHANGE_TYPE_REMOVE)
        {
            event = MPE_SI_EVENT_OOB_PAT_UPDATE;
        }
    }
        break;
    case OOB_PMT:
    {
        MPE_LOG(
                MPE_LOG_INFO,
                MPE_MOD_SI,
                "<mpe_siNotifyTableChanged> OOB_PMT event..\n");
        if (changeType == MPE_SI_CHANGE_TYPE_ADD)
        {
            event = MPE_SI_EVENT_OOB_PMT_ACQUIRED;
        }
        else if (changeType == MPE_SI_CHANGE_TYPE_MODIFY)
        {
            event = MPE_SI_EVENT_OOB_PMT_UPDATE;
        }
        else if (changeType == MPE_SI_CHANGE_TYPE_REMOVE)
        {
            event = MPE_SI_EVENT_OOB_PMT_UPDATE;
        }
    }
        break;
    default:
        event = 0;
        break;
    }

    if (event == 0)
        return MPE_SI_SUCCESS;

    /* Acquire registration list mutex */
    mpe_mutexAcquire(g_registration_list_mutex);

    walker = g_si_registration_list;

    while (walker)
    {
        mpe_Bool notifyWalker = false;

        /* If we received in-band PAT or PMT event, make sure that this client wants
         to receive the event */
        if (event == MPE_SI_EVENT_IB_PAT_ACQUIRED || event
                == MPE_SI_EVENT_IB_CVCT_ACQUIRED || event
                == MPE_SI_EVENT_IB_PAT_UPDATE || event
                == MPE_SI_EVENT_IB_CVCT_UPDATE)
        {
            if (walker->frequency == 0 || (walker->frequency == eventFrequency))
                notifyWalker = true;
        }
        else if (event == MPE_SI_EVENT_IB_PMT_ACQUIRED || event
                == MPE_SI_EVENT_IB_PMT_UPDATE)
        {
            if ((walker->frequency == 0 && walker->programNumber == 0)
                    || (walker->frequency == eventFrequency
                            && walker->programNumber == eventProgramNumber))
                notifyWalker = true;
        }
        else
        {
            // All other events are sent to all clients
            notifyWalker = true;
        }

        if (notifyWalker)
        {
            uint32_t termType = walker->edHandle->terminationType;

            mpe_eventQueueSend(walker->edHandle->eventQ, event,
                    (void*) optional_data, (void*) walker->edHandle,
                    optionalEventData3);

            // Do we need to unregister this client?
            if (termType == MPE_ED_TERMINATION_ONESHOT || (termType
                    == MPE_ED_TERMINATION_EVCODE && event
                    == walker->terminationEvent))
            {
                add_to_unreg_list(walker->edHandle);
            }
        }

        walker = walker->next;
    }

    /* Unregister all clients that have been added to unregistration list */
    unregister_clients();

    /* Notify queues */
    notify_registered_queues(event, optional_data);

    /* Release registration list mutex */
    mpe_mutexRelease(g_registration_list_mutex);

    return MPE_SI_SUCCESS;
}

static uint32_t si_state_to_event(mpe_SiGlobalState state)
{
    if (state == SI_ACQUIRING)
        return MPE_SI_EVENT_SI_ACQUIRING;
    else if (state == SI_NOT_AVAILABLE_YET)
        return MPE_SI_EVENT_SI_NOT_AVAILABLE_YET;
    else if (state == SI_NIT_SVCT_ACQUIRED)
        return MPE_SI_EVENT_NIT_SVCT_ACQUIRED;
    else if (state == SI_FULLY_ACQUIRED)
        return MPE_SI_EVENT_SI_FULLY_ACQUIRED;
    else if (state == SI_DISABLED)
        return MPE_SI_EVENT_SI_DISABLED;
    return 0;
}

static void send_si_event(uint32_t siEvent, uint32_t optional_data, uint32_t changeType)
{
    mpe_SiRegistrationListEntry *walker = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<send_si_event> \n");

    /* Acquire registration list mutex */
    mpe_mutexAcquire(g_registration_list_mutex);

    walker = g_si_registration_list;

    while (walker)
    {
        uint32_t termType = walker->edHandle->terminationType;

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "send_si_event siEvent: 0x%x \n",
                siEvent);

        mpe_eventQueueSend(walker->edHandle->eventQ, siEvent, (void*) optional_data,
                (void*) walker->edHandle, changeType);

        // Do we need to unregister this client?
        if (termType == MPE_ED_TERMINATION_ONESHOT || (termType
                == MPE_ED_TERMINATION_EVCODE && siEvent
                == walker->terminationEvent))
        {
            add_to_unreg_list(walker->edHandle);
        }

        walker = walker->next;
    }

    /* Unregister all clients that have been added to unregistration list */
    unregister_clients();

    notify_registered_queues(siEvent, 0);

    /* Release registration list mutex */
    mpe_mutexRelease(g_registration_list_mutex);
}

/*  Called by SITP when it detects tune away from one frequency to a
 different frequency. This is used to signal SI_NOT_AVAILABLE
 state to registered listeners.
 */
/*
 mpe_Error mpe_siNotifyTunedAway(uint32_t frequency)
 {
 mpe_SiRegistrationListEntry *walker = NULL;
 uint32_t eventFrequency = 0;
 mpe_Event event = MPE_SI_EVENT_TUNED_AWAY;
 mpe_SiTransportStreamEntry *ts_entry = NULL;
 mpe_SiStatus status;

 MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,"<mpe_siNotifyTunedAway> called...with freq: %d \n", frequency);
 eventFrequency = frequency;

 // Notify registered callers only if the current status is available_shortly
 ts_entry = find_transport_stream(frequency);
 if(ts_entry)
 {
 status = ts_entry->si_status;
 MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,"<mpe_siNotifyTunedAway> ..si status: %d \n", status);
 if(status != SI_AVAILABLE_SHORTLY)
 {
 return MPE_SI_SUCCESS; // error?
 }
 }


 // Acquire registration list mutex
 mpe_mutexAcquire(g_registration_list_mutex);

 walker = g_si_registration_list;

 while (walker)
 {
 mpe_Bool notifyWalker = false;

 if (walker->frequency == 0 || (walker->frequency == eventFrequency))
 notifyWalker = true;

 if(notifyWalker)
 {
 uint32_t termType = walker->edHandle->terminationType;

 mpe_eventQueueSend(walker->edHandle->eventQ,
 event,
 (void*)NULL, (void*)walker->edHandle,
 0);

 // Do we need to unregister this client?
 if (termType == MPE_ED_TERMINATION_ONESHOT ||
 (termType == MPE_ED_TERMINATION_EVCODE && event == walker->terminationEvent))
 {
 add_to_unreg_list(walker->edHandle);
 }
 }
 walker = walker->next;
 }

 // Unregister all clients that have been added to unregistration list
 unregister_clients();

 notify_registered_queues(event, 0);

 // Release registration list mutex
 mpe_mutexRelease(g_registration_list_mutex);

 return MPE_SI_SUCCESS;
 }
 */

/*  Called by SITP when it detects tables first but has not yet retrieved them.
 This enabled marking SI entries appropriately in anticipation of new SI
 acquisition.
 */
mpe_Error mpe_siNotifyTableDetected(mpe_SiTableType table_type,
        uint32_t optional_data)
{
    mpe_Error err = MPE_SI_INVALID_PARAMETER;

    /* TODO: should this parameter be used for something, or be taken out of this API? */
    MPE_UNUSED_PARAM(optional_data);

    /* Top-level table type handler */
    switch (table_type)
    {
    case OOB_SVCT_VCM:
        //mark_si_entries(SIENTRY_OLD);
        err = MPE_SI_SUCCESS;
        break;
    case OOB_NIT_CDS:
        mark_transport_stream_entries( TSENTRY_OLD);
        err = MPE_SI_SUCCESS;
        break;
    default:
        break;
    } // END switch (table_type)

    return err;
}

/*  Internal method to mark service entries with correct state */
/*
 static void mark_si_entries(mpe_SiEntryState new_state)
 {

 mpe_SiTableEntry *walker = NULL;

 if (!g_modulationsUpdated || !g_frequenciesUpdated || !g_channelMapUpdated)
 {
 MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,"<mark_si_entries> ..\n");
 }


 //  block here until the condition object is set

 mpe_condGet(g_global_si_cond);

 walker = g_si_entry;
 while (walker)
 {
 walker->state = new_state;
 walker = walker->next;
 }

 //    release global condition

 mpe_condSet(g_global_si_cond);
 }
 */

/*  Internal method to mark transport stream entries with correct state */
static void mark_transport_stream_entries(mpe_SiEntryState new_state)
{
    mpe_SiTransportStreamEntry *walker = NULL;

    if (!g_modulationsUpdated || !g_frequenciesUpdated || !g_channelMapUpdated)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mark_transport_stream_entries> ..\n");
    }

    walker = g_si_ts_entry;
    while (walker)
    {
        walker->state = new_state;
        walker = walker->next;
    }
}

/**
 Accessor methods to retrieve individual fields from service entries
 */
/**
 * Retrieve transport stream handle given service handle
 *
 * <i>mpe_siGetTransportStreamHandleForServiceHandle()</i>
 *
 * @param service_handle si handle to get the tsId from
 * @param ts_handle is the output param to populate transport stream handle
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetTransportStreamHandleForServiceHandle(
        mpe_SiServiceHandle service_handle,
        mpe_SiTransportStreamHandle *ts_handle)
{
    mpe_SiTableEntry *si_entry = NULL;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetTransportStreamHandleForServiceHandle> service_handle:0x%x\n",
            service_handle);

    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (ts_handle == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    si_entry = (mpe_SiTableEntry *) service_handle;

    /* This should never happen? */
    if (si_entry->ts_handle == MPE_SI_INVALID_HANDLE)
        return MPE_SI_NOT_AVAILABLE;
    else
    {
        mpe_SiTransportStreamEntry *handle =
                (mpe_SiTransportStreamEntry*) si_entry->ts_handle;
        if (handle->siStatus == SI_AVAILABLE_SHORTLY && handle->modulation_mode
                != MPE_SI_MODULATION_QAM_NTSC)
            return MPE_SI_NOT_AVAILABLE_YET; // Only return this for digital streams
    }

    *ts_handle = si_entry->ts_handle;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetTransportStreamHandleForServiceHandle> *ts_handle:0x%x\n",
            *ts_handle);

    return MPE_SI_SUCCESS;
}

/**
 * Retrieve PMT PID given service handle
 *
 * <i>mpe_siGetPMTPidForServiceHandle()</i>
 *
 * @param service_handle si handle to get the pmt pid from
 * @param pmt_pid is the output param to populate pmt pid
 *
 * @return MPE_SI_SUCCESS if successfully retrieved pmt pid, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetPMTPidForServiceHandle(mpe_SiServiceHandle service_handle,
        uint32_t* pmt_pid)
{
    mpe_SiTableEntry *si_entry = NULL;
    mpe_SiTransportStreamEntry *ts_entry = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetPMTPidForServiceHandle> service_handle:0x%x\n",
            service_handle);
    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (pmt_pid == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    si_entry = (mpe_SiTableEntry *) service_handle;

    /* If PAT is in the process of being acquired return from here
     until it is updated (disable acquiring lock) */
    if (si_entry->ts_handle != MPE_SI_INVALID_HANDLE)
    {
        ts_entry = (mpe_SiTransportStreamEntry *) si_entry->ts_handle;
        if (ts_entry->siStatus == SI_NOT_AVAILABLE)
        {
            return MPE_SI_NOT_AVAILABLE;
        }
        else if (ts_entry->siStatus == SI_AVAILABLE_SHORTLY)
        {
            return MPE_SI_NOT_AVAILABLE_YET;
        }
    }

    *pmt_pid = si_entry->program->pmt_pid;

    return MPE_SI_SUCCESS;
}

/**
 * Retrieve service type for the given service handle
 *
 * <i>mpe_siGetServiceTypeForServiceHandle()</i>
 *
 * @param service_handle si handle to get the service type from
 * @param service_type is the output param to populate service type
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetServiceTypeForServiceHandle(
        mpe_SiServiceHandle service_handle, mpe_SiServiceType *service_type)
{
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;
    mpe_SiTransportStreamEntry *ts = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetServiceTypeForServiceHandle> service_handle:0x%x\n",
            service_handle);

    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (service_type == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }
    ts = (mpe_SiTransportStreamEntry *) si_entry->ts_handle;

    *service_type = si_entry->service_type;
    // According to OCAP spec return 'UNKNOWN' if service_type is not known
    // However...
    if ((*service_type == MPE_SI_SERVICE_TYPE_UNKNOWN) && ((ts != NULL)
            && (ts->modulation_mode == MPE_SI_MODULATION_QAM_NTSC)))
    {
        // The WatchTvHost app needs to know which services
        // are analog, because it must filter to use only
        // those services.  If we don't know the type, but the
        // modulation mode is analog, then it should be safe
        // to presume it is in fact an analog service, so
        // report it as such.
        *service_type = MPE_SI_SERVICE_ANALOG_TV;
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetServiceTypeForServiceHandle> *service_type:%d\n",
            *service_type);

    return MPE_SI_SUCCESS;
}

/**
 * Retrieve number of language variants for the service long name for the given service handle
 *
 * <i>mpe_siGetNumberOfLongNamesForServiceHandle()</i>
 *
 * @param service_handle si handle to get the number of service names from
 * @param num_long_names output param to populate the number of service long name language variants
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 */
mpe_Error mpe_siGetNumberOfLongNamesForServiceHandle(
        mpe_SiServiceHandle service_handle, uint32_t *num_long_names)
{
    uint32_t count = 0;
    mpe_SiTableEntry *si_entry = NULL;
    mpe_siSourceNameEntry *sn_entry;
    mpe_SiLangSpecificStringList *ln_walker = NULL;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetNumberOfLongNamesForServiceHandle> service_handle:0x%x\n",
            service_handle);

    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (num_long_names == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    si_entry = (mpe_SiTableEntry *) service_handle;
    sn_entry = si_entry->source_name_entry;

    // Count the entries
    if (sn_entry != NULL)
    {
      for (ln_walker = sn_entry->source_long_names; ln_walker != NULL; ln_walker = ln_walker->next, count++)
        ;
    }

    *num_long_names = count;
    return MPE_SI_SUCCESS;
}

/**
 * Retrieve long service names and associated ISO639 langauges for the given service handle.
 * The language at index 0 of the languages array is associated with the name at index 0 of
 * of the names array -- and so on...
 *
 * <i>mpe_siGetLongNamesForServiceHandle()</i>
 *
 * @param service_handle si handle to get the service name from
 * @param languages is the output param to populate long service name languages
 * @param service_long_names is the output param to populate long service name
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 */
mpe_Error mpe_siGetLongNamesForServiceHandle(
        mpe_SiServiceHandle service_handle, char* languages[],
        char* service_long_names[])
{
    mpe_SiLangSpecificStringList *walker = NULL;
    mpe_SiTableEntry *si_entry = NULL;
    mpe_siSourceNameEntry *sn_entry;
    int i = 0;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetLongNamesForServiceHandle> service_handle:0x%x\n",
            service_handle);

    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (languages == NULL)
            || (service_long_names == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    si_entry = (mpe_SiTableEntry *) service_handle;
    sn_entry = si_entry->source_name_entry;

    if (sn_entry != NULL && sn_entry->source_long_names != NULL)
    {

    for (walker = sn_entry->source_long_names; walker != NULL; walker = walker->next, i++)
    {
        languages[i] = walker->language;
        service_long_names[i] = walker->string;

        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_SI,
                "<mpe_siGetNamesForServiceHandle> source_name:%s, language:%s\n",
                  walker->string, walker->language);
      }
    }

    return MPE_SI_SUCCESS;
}

/**
 * Retrieve number of language variants for the service name for the given service handle
 *
 * <i>mpe_siGetNumberOfNamesForServiceHandle()</i>
 *
 * @param service_handle si handle to get the number of service names from
 * @param num_names output param to populate the number of service name language variants
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 */
mpe_Error mpe_siGetNumberOfNamesForServiceHandle(
        mpe_SiServiceHandle service_handle, uint32_t *num_names)
{
    uint32_t count = 0;
    mpe_SiTableEntry *si_entry = NULL;
    mpe_siSourceNameEntry *sn_entry;
    mpe_SiLangSpecificStringList *ln_walker = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetNumberOfNamesForServiceHandle> service_handle:0x%x\n",
            service_handle);

    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (num_names == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    si_entry = (mpe_SiTableEntry *) service_handle;
    sn_entry = si_entry->source_name_entry;

    // Count the entries
    if (sn_entry != NULL)
    {
      for (ln_walker = sn_entry->source_names; ln_walker != NULL; ln_walker = ln_walker->next, count++)
        ;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetNumberOfNamesForServiceHandle> service_handle:0x%x, count=%d\n",
            service_handle, count);
    *num_names = count;
    return MPE_SI_SUCCESS;
}

/**
 * Retrieve language-specfic service names for the given service handle
 * The language at index 0 of the languages array is associated with the name at index 0 of
 * of the names array -- and so on...
 *
 * <i>mpe_siGetLongNamesForServiceHandle()</i>
 *
 * @param service_handle si handle to get the service name from
 * @param languages is the output param to populate long service name languages
 * @param service_names is the output param to populate service names
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetNamesForServiceHandle(mpe_SiServiceHandle service_handle,
        char* languages[], char* service_names[])
{
    mpe_SiLangSpecificStringList *ln_walker = NULL;
    mpe_SiTableEntry *si_entry = NULL;
    mpe_siSourceNameEntry *sn_entry;
    int i = 0;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetNamesForServiceHandle> service_handle:0x%x\n",
            service_handle);

    /* Parameter check */
    if (service_handle == MPE_SI_INVALID_HANDLE || languages == NULL
            || service_names == NULL)
        return MPE_SI_INVALID_PARAMETER;

    si_entry = (mpe_SiTableEntry *) service_handle;
    sn_entry = si_entry->source_name_entry;

    if (sn_entry != NULL && sn_entry->source_names != NULL)
    {

    for (ln_walker = sn_entry->source_names; ln_walker != NULL; ln_walker = ln_walker->next, i++)
    {
        languages[i] = ln_walker->language;
        service_names[i] = ln_walker->string;

        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_SI,
                "<mpe_siGetNamesForServiceHandle> source_name:%s, language:%s\n",
                  ln_walker->string, ln_walker->language);
      }
    }

    return MPE_SI_SUCCESS;
}

/**
 * Retrieve last update time for the given service handle
 *
 * <i>mpe_siGetServiceDescriptionLastUpdateTimeForServiceHandle()</i>
 *
 * @param service_handle input si handle
 * @param time is the output param to populate update time
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetServiceDescriptionLastUpdateTimeForServiceHandle(
        mpe_SiServiceHandle service_handle, mpe_TimeMillis *pTime)
{
    mpe_SiTableEntry *si_entry = NULL;

    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (pTime == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    si_entry = (mpe_SiTableEntry *) service_handle;

    *pTime = si_entry->ptime_service;

    return MPE_SI_SUCCESS;
}

/**
 * Retrieve service number for the given service handle
 *
 * <i>mpe_siGetServiceNumberForServiceHandle()</i>
 *
 * @param service_handle si handle to get the service number from
 * @param service_number is the output param to populate service number
 * @param minor_number is the output param to populate minor service number
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetServiceNumberForServiceHandle(
        mpe_SiServiceHandle service_handle, uint32_t* service_number,
        uint32_t* minor_number)
{
    mpe_SiTableEntry *si_entry = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetServiceNumberForServiceHandle> service_handle:0x%x\n",
            service_handle);

    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (service_number == NULL)
            || (minor_number == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    si_entry = (mpe_SiTableEntry *) service_handle;

    // According to OCAP spec return '-1' is service_numbers are not known
    *service_number = si_entry->major_channel_number;
    *minor_number = si_entry->minor_channel_number;

    return MPE_SI_SUCCESS;
}

/**
 * Retrieve source Id for the given service handle
 *
 * <i>mpe_siGetSourceIdForServiceHandle()</i>
 *
 * @param service_handle si handle to get the service Id from
 * @param sourceId is the output param to populate source id
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetSourceIdForServiceHandle(mpe_SiServiceHandle service_handle,
        uint32_t *sourceId)
{
    mpe_SiTableEntry *si_entry = NULL;
    //mpe_SiTableEntry *walker = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetSourceIdForServiceHandle> service_handle:0x%x\n",
            service_handle);

    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (sourceId == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    si_entry = (mpe_SiTableEntry *) service_handle;

    *sourceId = si_entry->source_id;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetSourceIdForServiceHandle> *sourceId:0x%x\n", *sourceId);

    /*  If this handle was acquired from the main tree then sourceId would not be
     -1. Only when it is acquired from dynamic tree will it be -1. In that case
     we want to check if this freq, prog_num pair has been signalled in SVCT
     also and if so get the sourceId from there. In these cases, the service
     entry in dynamic tree takes precedence. i.e. if a service entry is created in
     the dynamic tree based on freq, prog_num and if this pair gets signalled in the
     SVCT also (with a sourceId) then the service entry in the dynamic tree takes
     precedence. The corresponding entry in the main tree will be accessed to get
     info that's not available in the dynamic tree. (ex: sourceId, modulation mode etc.)
     */
    /*  For 0.95 release, SI DB no longer uses two different trees. Static and dynamic trees
     are merged. And, a service entry can only exist once.

     TODO: Test removing the logic below...
     */
    /*
     if(si_entry->source_id == SOURCEID_UNKNOWN)
     {
     // Walk through the list of services to find a match
     walker = g_si_entry;
     while(walker)
     {
     if( (walker->frequency == si_entry->frequency) && (walker->program_number == si_entry->program_number) )
     {
     *sourceId = walker->source_id;
     break;
     }
     walker = walker->next;
     }
     }
     */

    return MPE_SI_SUCCESS;
}

/**
 * Retrieve appID for the given service handle
 *
 * <i>mpe_siGetAppIdForServiceHandle()</i>
 *
 * @param service_handle si handle to get the appID from
 * @param appId is the output param to populate source id
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetAppIdForServiceHandle(mpe_SiServiceHandle service_handle,
        uint32_t* appId)
{
    mpe_SiTableEntry *si_entry = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetAppIDForServiceHandle> service_handle:0x%x\n",
            service_handle);

    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (appId == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    si_entry = (mpe_SiTableEntry *) service_handle;

    // TODO: There is a separate app_id field in 'si_entry', check which one (source_id/app_id)
    // needs to be returned.
    // if (si_entry->isAppType)
    *appId = si_entry->app_id;
    //else
    //  return MPE_SI_NOT_FOUND;

    return MPE_SI_SUCCESS;
}

/**
 * Retrieve frequency for the given service handle
 *
 * <i>mpe_siGetFrequencyForServiceHandle()</i>
 *
 * @param service_handle si handle to get the freq from
 * @param freq is the output param to populate frequency
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetFrequencyForServiceHandle(
        mpe_SiServiceHandle service_handle, uint32_t *freq)
{
    mpe_SiTableEntry *si_entry = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetFrequencyForServiceHandle> service_handle:0x%x\n",
            service_handle);

    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (freq == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    si_entry = (mpe_SiTableEntry *) service_handle;

    *freq = 0;

    /*  If the handle was acquired based on sourceId, then look up in
     the global array (populated from NIT) based on the index.
     */
    /*  For OOB the frequency is -1 */
    if (si_entry->ts_handle)
    {
        *freq = ((mpe_SiTransportStreamEntry *) si_entry->ts_handle)->frequency;
    }
    else
    {
        // This should never happen..
        *freq = g_frequency[si_entry->freq_index];
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetFrequencyForServiceHandle> freq:%d\n", *freq);

    return MPE_SI_SUCCESS;
}

/**
 * Retrieve program_number for the given service handle
 *
 * <i>mpe_siGetProgramNumberForServiceHandle()</i>
 *
 * @param service_handle si handle to get the prog_num from
 * @param prog_num is the output param to populate program_number
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetProgramNumberForServiceHandle(
        mpe_SiServiceHandle service_handle, uint32_t *prog_num)
{
    mpe_SiTableEntry *si_entry = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetProgramNumberForServiceHandle> service_handle:0x%x\n",
            service_handle);

    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (prog_num == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    si_entry = (mpe_SiTableEntry *) service_handle;

    // Account for dynamic entries also, in which case the service
    // entry's program_number field is not set
    if(si_entry->program)
    {
        *prog_num = si_entry->program->program_number;
    }
    else
    {
        *prog_num = si_entry->program_number;
    }
    return MPE_SI_SUCCESS;
}

/**
 * Retrieve modulation format for the given service handle
 *
 * <i>mpe_siGetModulationFormatForServiceHandle()</i>
 *
 * @param service_handle si handle to get the mode from
 * @param mode is the output param to populate the mode
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetModulationFormatForServiceHandle(
        mpe_SiServiceHandle service_handle, mpe_SiModulationMode *mode)
{
    mpe_SiTableEntry *si_entry = NULL;
    mpe_SiTransportStreamEntry *ts_entry = NULL;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetModulationFormatForServiceHandle> service_handle:0x%x\n",
            service_handle);

    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (mode == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    si_entry = (mpe_SiTableEntry *) service_handle;

    if (si_entry->ts_handle != MPE_SI_INVALID_HANDLE)
    {
        ts_entry = (mpe_SiTransportStreamEntry *) si_entry->ts_handle;
        *mode = ts_entry->modulation_mode;
    }
    else
    {
        *mode = MPE_SI_MODULATION_UNKNOWN;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetModulationMode> returning mode:%d\n", *mode);

    return MPE_SI_SUCCESS;
}

/**
 * Retrieve service details last update time for the given service handle
 *
 * <i>mpe_siGetServiceDetailsLastUpdateTimeForServiceHandle()</i>
 *
 * @param service_handle input si handle
 * @param time is the output param to populate the last update time
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetServiceDetailsLastUpdateTimeForServiceHandle(
        mpe_SiServiceHandle service_handle, mpe_TimeMillis *pTime)
{
    mpe_SiTableEntry *si_entry = NULL;

    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (pTime == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    si_entry = (mpe_SiTableEntry *) service_handle;

    *pTime = si_entry->ptime_service;

    return MPE_SI_SUCCESS;
}

/**
 * Retrieve 'isFree' for the given service handle
 *
 * <i>mpe_siGetIsFreeFlagForServiceHandle()</i>
 *
 * @param service_handle input si handle
 * @param is_free is the output param
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetIsFreeFlagForServiceHandle(
        mpe_SiServiceHandle service_handle, mpe_Bool *is_free)
{
    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (is_free == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    /*  TODO:
     How do we know whether a service is in the clear (free) or encrypted?
     */
    *is_free = TRUE;

    return MPE_SI_SUCCESS;
}

/**
 * Retrieve the number of language-specific descriptions for the given service handle
 *
 * <i>mpe_siGetNumberOfServiceDescriptionsForServiceHandle()</i>
 *
 * @param service_handle input si handle
 * @param num_descriptions is the output param to populate the number of descriptions
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 */
mpe_Error mpe_siGetNumberOfServiceDescriptionsForServiceHandle(
        mpe_SiServiceHandle service_handle, uint32_t *num_descriptions)
{
    mpe_SiTableEntry *si_entry = NULL;
    mpe_SiLangSpecificStringList *walker = NULL;
    uint32_t count = 0;

    /* Parameter check */
    if (service_handle == MPE_SI_INVALID_HANDLE || num_descriptions == NULL)
        return MPE_SI_INVALID_PARAMETER;

    si_entry = (mpe_SiTableEntry *) service_handle;

    // Count the entries
    for (walker = si_entry->descriptions; walker != NULL; walker = walker->next, count++)
        ;

    *num_descriptions = count;
    return MPE_SI_SUCCESS;
}

/**
 * Retrieve language-specific service descriptions for the given service handle
 * The language at index 0 of the languages array is associated with the name at index 0 of
 * of the descriptions array -- and so on...
 *
 * <i>mpe_siGetServiceDescriptionsForServiceHandle()</i>
 *
 * @param service_handle input si handle
 * @param languages is the output param to populate the languages
 * @param descriptions is the output param to populate the descriptions
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetServiceDescriptionsForServiceHandle(
        mpe_SiServiceHandle service_handle, char* languages[],
        char* descriptions[])
{
    mpe_SiTableEntry *si_entry = NULL;
    mpe_SiLangSpecificStringList *walker = NULL;
    int i = 0;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetServiceDescriptionsForServiceHandle> service_handle:0x%x\n",
            service_handle);

    /* Parameter check */
    if (service_handle == MPE_SI_INVALID_HANDLE || languages == NULL
            || descriptions == NULL)
        return MPE_SI_INVALID_PARAMETER;

    si_entry = (mpe_SiTableEntry *) service_handle;

    for (walker = si_entry->descriptions; walker != NULL; walker = walker->next, i++)
    {
        languages[i] = walker->language;
        descriptions[i] = walker->string;

        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_SI,
                "<mpe_siGetServiceDescriptionsForServiceHandle> description:%s, language:%s\n",
                walker->string, walker->language);
    }

    return MPE_SI_SUCCESS;
}

/**
 * Retrieve number of components for the given service handle
 *
 * <i>mpe_siGetNumberOfComponentsForServiceHandle()</i>
 *
 * @param service_handle input si handle
 * @param num_components is the output param to populate number of components
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetNumberOfComponentsForServiceHandle(
        mpe_SiServiceHandle service_handle, uint32_t *num_components)
{
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry*) service_handle;
    mpe_SiProgramInfo *pi = NULL;
    mpe_Error err = MPE_SI_SUCCESS;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetNumberOfComponentsForServiceHandle> service_handle:0x%x\n",
            service_handle);

    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (num_components == NULL))
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<mpe_siGetNumberOfComponentsForServiceHandle> bad service_handle:0x%x or num_components:0x%p\n",
                service_handle, num_components);
        err = MPE_SI_INVALID_PARAMETER;
    }
    else
    {
        *num_components = 0;
        /* SI DB has not been populated yet */
        if (g_si_entry == NULL)
        {
            err = MPE_SI_NOT_AVAILABLE;
        }
        else
        {
            pi = si_entry->program;
            if (pi == NULL)
            {
                mpe_SiTransportStreamEntry *ts =
                        (mpe_SiTransportStreamEntry *) si_entry->ts_handle;
                if (ts != NULL)
                { // Since analog has no components, returning 0 makes sense.
                    if (ts->modulation_mode != MPE_SI_MODULATION_QAM_NTSC)
                        err = MPE_SI_INVALID_PARAMETER;
                }
                else
                {
                    err = MPE_SI_NOT_AVAILABLE; // SPI service?
                }
            }
            else
            {
                /* If PSI is in the process of being acquired return from here
                 until it is updated. */
                if (pi->pmt_status == PMT_NOT_AVAILABLE)
                {
                    err = MPE_SI_NOT_AVAILABLE;
                }
                else if (pi->pmt_status == PMT_AVAILABLE_SHORTLY)
                {
                    err = MPE_SI_NOT_AVAILABLE_YET;
                }
                else
                {
                    /*
                     ** The num_components translates to the number of PMT elementary
                     ** streams at the native level, so set the passed in ptrs val to
                     ** the number of elementary streams.
                     */
                    *num_components = pi->number_elem_streams;
                }
            }
        }
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_SI,
                "<mpe_siGetNumberOfComponentsForServiceHandle> returning number_elem_streams: %d (%d)\n",
                *num_components, err);
    }

    return err;
}

/**
 * Return all service component handles in the given service
 *
 * <i>mpe_siGetAllComponentsForServiceHandle()</i>
 *
 * @param ts_handle input transport stream handle
 * @param comp_handle output param to populate component handles
 * @param num_components Input param: has size of comp_handle.
 * Output param: the actual number of component handles placed in comp_handle
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetAllComponentsForServiceHandle(
        mpe_SiServiceHandle service_handle,
        mpe_SiServiceComponentHandle comp_handle[], uint32_t* num_components)
{
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry*) service_handle;
    mpe_SiProgramInfo *pi = NULL;
    mpe_SiElementaryStreamList *list_ptr = NULL;
    mpe_Error err = MPE_SI_SUCCESS;
    uint32_t i = 0;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetAllComponentsForServiceHandle> \n");

    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (comp_handle == NULL)
            || (num_components == NULL) || (*num_components == 0))
    {
        err = MPE_SI_INVALID_PARAMETER;
    }
    else
    {
        /* SI DB has not been populated yet */
        if (g_si_entry == NULL)
        {
            err = MPE_SI_NOT_AVAILABLE;
        }
        else
        {
            *comp_handle = 0;
            pi = si_entry->program;
            if (pi == NULL)
            {
                mpe_SiTransportStreamEntry *ts =
                        (mpe_SiTransportStreamEntry *) si_entry->ts_handle;
                if (ts != NULL)
                {
                    if (ts->modulation_mode != MPE_SI_MODULATION_QAM_NTSC)
                        err = MPE_SI_INVALID_PARAMETER;
                    else
                        err = MPE_SI_NOT_FOUND;
                }
                else
                {
                    err = MPE_SI_NOT_AVAILABLE; // SPI service?
                }
            }
            else
            {

                list_ptr = pi->elementary_streams;

                while (list_ptr)
                {
                    if (i >= *num_components)
                    {
                        MPE_LOG(
                                MPE_LOG_ERROR,
                                MPE_MOD_SI,
                                "<mpe_siGetAllComponentsForServiceHandle> array_service_handle (%d) (%p) too small for list\n",
                                *num_components, pi->elementary_streams);
                        err = MPE_ENOMEM;
                        break;
                    }

                    comp_handle[i++] = (mpe_SiServiceComponentHandle) list_ptr;
                    list_ptr->ref_count++;
                    MPE_LOG(
                            MPE_LOG_DEBUG,
                            MPE_MOD_SI,
                            "<mpe_siGetAllComponentsForServiceHandle> comp_handle:0x%p, ref_count:%d \n",
                            list_ptr, list_ptr->ref_count);
                    list_ptr = list_ptr->next;
                }

                *num_components = i;
            }
        }
    }
    return err;
}

/**
 * Retrieve pcr pid pId given service handle
 *
 * <i>mpe_siGetPcrPidForServiceHandle()</i>
 *
 * @param service_handle si handle to get the pcr pid from
 * @param pcrPid is the output param to populate pcr pid
 *
 * @return MPE_SI_SUCCESS if successfully retrieved pcr pid, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetPcrPidForServiceHandle(mpe_SiServiceHandle service_handle,
        uint32_t* pcrPid)
{
    mpe_Error err = MPE_SI_SUCCESS;
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;
    mpe_SiProgramInfo *pi = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetPcrPidForServiceHandle> service_handle:0x%x\n",
            service_handle);
    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (pcrPid == NULL))
    {
        err = MPE_SI_INVALID_PARAMETER;
    }
    else
    {
        /* SI DB has not been populated yet */
        if (g_si_entry == NULL)
        {
            err = MPE_SI_NOT_AVAILABLE;
        }
        else
        {
            *pcrPid = 0;
            pi = si_entry->program;
            if (pi == NULL)
            {
                mpe_SiTransportStreamEntry *ts =
                        (mpe_SiTransportStreamEntry *) si_entry->ts_handle;
                if (ts != NULL)
                {
                    if (ts->modulation_mode != MPE_SI_MODULATION_QAM_NTSC)
                        err = MPE_SI_INVALID_PARAMETER;
                    else
                        err = MPE_SI_NOT_AVAILABLE;
                }
                else
                {
                    err = MPE_SI_NOT_AVAILABLE; // SPI service?
                }
            }
            else
            {
                // If PMT is in the process of being acquired
                // return MPE_SI_NOT_AVAILABLE_YET
                if (pi->pmt_status == PMT_AVAILABLE_SHORTLY)
                {
                    err = MPE_SI_NOT_AVAILABLE_YET;
                }
                *pcrPid = pi->pcr_pid;
            }
        }
    }
    return err;
}

/**
 * Retrieve pat version transport stream handle
 *
 * <i>mpe_siGetPATVersionForTransportStreamHandle()</i>
 *
 * @param ts_handle ts handle to get the pat version from
 * @param pcrPid is the output param to populate pat version
 *
 * @return MPE_SI_SUCCESS if successfully retrieved pat version, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetPATVersionForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle, uint32_t* patVersion)
{
    mpe_SiTransportStreamEntry *ts_entry = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetPATVersionForTransportStreamHandle> ts_handle:0x%x\n",
            ts_handle);

    /* Parameter check */
    if ((ts_handle == MPE_SI_INVALID_HANDLE) || (patVersion == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    ts_entry = (mpe_SiTransportStreamEntry *) ts_handle;

    *patVersion = ts_entry->pat_version;

    /* If PAT is in the process of being acquired return from here
     until it is updated (disable acquiring lock) */
    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetPATVersionForTransportStreamHandle> returning patVersion:%d\n",
            *patVersion);

    if (ts_entry->siStatus == SI_NOT_AVAILABLE)
    {
        return MPE_SI_NOT_AVAILABLE;
    }
    else if (ts_entry->siStatus == SI_AVAILABLE_SHORTLY)
    {
        return MPE_SI_NOT_AVAILABLE_YET;
    }

    return MPE_SI_SUCCESS;
}

/**
 * Retrieve pat version given service handle
 *
 * <i>mpe_siGetPATVersionForServiceHandle()</i>
 *
 * @param service_handle si handle to get the pat version from
 * @param pcrPid is the output param to populate pat version
 *
 * @return MPE_SI_SUCCESS if successfully retrieved pat version, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetPATVersionForServiceHandle(
        mpe_SiServiceHandle service_handle, uint32_t* patVersion)
{
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;
    mpe_SiTransportStreamEntry *ts_entry = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetPATVersionForServiceHandle> service_handle:0x%x\n",
            service_handle);
    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (patVersion == NULL)
            || (si_entry->ts_handle == MPE_SI_INVALID_HANDLE))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    ts_entry = (mpe_SiTransportStreamEntry *) si_entry->ts_handle;

    if (ts_entry == NULL)
        return MPE_SI_INVALID_PARAMETER;

    *patVersion = ts_entry->pat_version;

    /* If PAT is in the process of being acquired return from here
     until it is updated (disable acquiring lock) */
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetPATVersionForServiceHandle> returning patVersion:%d\n",
            *patVersion);

    if (ts_entry->siStatus == SI_NOT_AVAILABLE)
    {
        return MPE_SI_NOT_AVAILABLE;
    }
    else if (ts_entry->siStatus == SI_AVAILABLE_SHORTLY)
    {
        return MPE_SI_NOT_AVAILABLE_YET;
    }

    return MPE_SI_SUCCESS;
}

/**
 * Retrieve pmt version given program info handle
 *
 * <i>mpe_siGetPMTVersionForProgramHandle()</i>
 *
 * @param program_handle si handle to get the pmt version from
 * @param pcrPid is the output param to populate pmt version
 *
 * @return MPE_SI_SUCCESS if successfully retrieved pmt version, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetPMTVersionForProgramHandle(
        mpe_SiProgramHandle program_handle, uint32_t* pmtVersion)
{
    mpe_SiProgramInfo *pi = (mpe_SiProgramInfo *) program_handle;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetPMTVersionForProgramHandle> program_handle:0x%x\n",
            program_handle);

    /* Parameter check */
    if ((program_handle == MPE_SI_INVALID_HANDLE) || (pmtVersion == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    *pmtVersion = pi->pmt_version;

    /* If PSI is in the process of being acquired return from here
     until it is updated (disable acquiring lock) */
    if (pi->pmt_status == PMT_NOT_AVAILABLE)
    {
        return MPE_SI_NOT_AVAILABLE;
    }
    else if (pi->pmt_status == PMT_AVAILABLE_SHORTLY)
    {
        return MPE_SI_NOT_AVAILABLE_YET;
    }

    return MPE_SI_SUCCESS;
}

/**
 * Retrieve pmt version given service handle
 *
 * <i>mpe_siGetPMTVersionForServiceHandle()</i>
 *
 * @param service_handle si handle to get the pmt version from
 * @param pcrPid is the output param to populate pmt version
 *
 * @return MPE_SI_SUCCESS if successfully retrieved pmt version, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetPMTVersionForServiceHandle(
        mpe_SiServiceHandle service_handle, uint32_t* pmtVersion)
{
    mpe_Error err = MPE_SI_SUCCESS;
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;
    mpe_SiProgramInfo *pi = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetPMTVersionForServiceHandle> service_handle:0x%x\n",
            service_handle);
    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (pmtVersion == NULL))
    {
        err = MPE_SI_INVALID_PARAMETER;
    }
    else
    {
        /* SI DB has not been populated yet */
        if (g_si_entry == NULL)
        {
            err = MPE_SI_NOT_AVAILABLE;
        }
        else
        {
            *pmtVersion = 0;
            pi = si_entry->program;
            if (pi == NULL)
            {
                mpe_SiTransportStreamEntry *ts =
                        (mpe_SiTransportStreamEntry *) si_entry->ts_handle;
                if (ts != NULL)
                {
                    if (ts->modulation_mode != MPE_SI_MODULATION_QAM_NTSC)
                        err = MPE_SI_INVALID_PARAMETER;
                    else
                        err = MPE_SI_NOT_FOUND;
                }
                else
                {
                    err = MPE_SI_NOT_AVAILABLE; // SPI service?
                }
            }
            else
            {
                MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,
                        "<mpe_siGetPMTVersionForServiceHandle> pi->pmt_status: %d\n", pi->pmt_status);

                if (pi->pmt_status == PMT_AVAILABLE_SHORTLY)
                {
                    err = MPE_SI_NOT_AVAILABLE_YET;
                }
                *pmtVersion = pi->pmt_version;
            }
        }
    }
    return err;
}

/**
 * Retrieve pat crc given transport stream handle
 *
 * <i>mpe_siGetPATCRCForServiceHandle()</i>
 *
 * @param service_handle si handle to get the pat crc from
 * @param crc is the output param to populate pat crc
 *
 * @return MPE_SI_SUCCESS if successfully retrieved pat version, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetPATCRCForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle, uint32_t* pat_crc)
{
    mpe_SiTransportStreamEntry *ts_entry = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetPATCRCForServiceHandle> ts_handle:0x%x\n", ts_handle);
    /* Parameter check */
    if ((ts_handle == MPE_SI_INVALID_HANDLE) || (pat_crc == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    ts_entry = (mpe_SiTransportStreamEntry *) ts_handle;

    *pat_crc = 0;

    /* If PAT is in the process of being acquired return from here
     until it is updated (disable acquiring lock) */
    *pat_crc = ts_entry->pat_crc;

    if (ts_entry->siStatus == SI_NOT_AVAILABLE)
    {
        return MPE_SI_NOT_AVAILABLE;
    }
    else if (ts_entry->siStatus == SI_AVAILABLE_SHORTLY)
    {
        return MPE_SI_NOT_AVAILABLE_YET;
    }

    return MPE_SI_SUCCESS;
}

/**
 * Retrieve pat crc given service handle
 *
 * <i>mpe_siGetPATCRCForServiceHandle()</i>
 *
 * @param service_handle si handle to get the pat crc from
 * @param crc is the output param to populate pat crc
 *
 * @return MPE_SI_SUCCESS if successfully retrieved pat version, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetPATCRCForServiceHandle(mpe_SiServiceHandle service_handle,
        uint32_t* pat_crc)
{
    mpe_SiTableEntry *si_entry = NULL;
    mpe_SiTransportStreamEntry *ts_entry = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetPATCRCForServiceHandle> service_handle:0x%x\n",
            service_handle);
    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (pat_crc == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    si_entry = (mpe_SiTableEntry *) service_handle;

    *pat_crc = 0;

    /* If PAT is in the process of being acquired return from here
     until it is updated (disable acquiring lock) */
    if (si_entry->ts_handle != MPE_SI_INVALID_HANDLE)
    {
        ts_entry = (mpe_SiTransportStreamEntry *) si_entry->ts_handle;

        *pat_crc = ts_entry->pat_crc;

        if (ts_entry->siStatus == SI_NOT_AVAILABLE)
        {
            return MPE_SI_NOT_AVAILABLE;
        }
        else if (ts_entry->siStatus == SI_AVAILABLE_SHORTLY)
        {
            return MPE_SI_NOT_AVAILABLE_YET;
        }

    }

    return MPE_SI_SUCCESS;
}

/**
 * Retrieve the mpe_SiTransportStreamHandle for the given frequency.
 *
 * <i>mpe_siGetTransportStreamHandleByFrequency()</i>
 *
 * @param frequency - the frequency to get the ts handle from
 * @param ts_handle is the output param to populate mpe_SiTransportStreamHandle
 *
 * @return MPE_SI_SUCCESS if successfully retrieved ts_handle, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetTransportStreamHandleByFrequency(uint32_t frequency,
        mpe_SiTransportStreamHandle *ts_handle)
{
    mpe_Error err = MPE_SI_SUCCESS;
    mpe_SiTransportStreamEntry *walker = NULL;

    /* Parameter check */
    if (ts_handle == NULL)
    {
        return MPE_SI_INVALID_PARAMETER;
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetTransportStreamHandleByFrequency> freq:%d\n", frequency);

    // Use the global write lock instead from where this is being called (sitp_parse.c)
    /* Acquire global list mutex */
    //mpe_mutexAcquire(g_global_si_list_mutex);

    if (frequency == MPE_SI_OOB_FREQUENCY)
        walker = g_si_oob_ts_entry;
    else if (frequency == MPE_SI_DSG_FREQUENCY)
        walker = g_si_dsg_ts_entry;
    else if (frequency == MPE_SI_HN_FREQUENCY)
        walker = g_si_HN_ts_entry;
    else
        walker = g_si_ts_entry;

    while (walker != NULL)
    {
        /* If a wildcard tsId is specified, look for a match only of frequency */
        if (walker->frequency == frequency)
        {
            *ts_handle = (mpe_SiTransportStreamHandle) walker;
            break;
        }
        walker = walker->next;
    }

    if (walker == NULL)
        err = MPE_SI_NOT_FOUND;
    else if (walker->siStatus == SI_AVAILABLE_SHORTLY)
        err = MPE_SI_NOT_AVAILABLE_YET;

    //mpe_mutexRelease(g_global_si_list_mutex);
    /* Release global list mutex */

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetTransportStreamHandleByFrequency> transportStreamHandle: 0x%x\n",
            *ts_handle);
    return err;
}

/**
 * Retrieve cvct crc given service handle
 *
 * <i>mpe_siGetCVCTCRCForTransportStreamHandle()</i>
 *
 * @param ts_handle - the transport stream handle to get the pat crc from
 * @param crc is the output param to populate pat crc
 *
 * @return MPE_SI_SUCCESS if successfully retrieved crc, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetCVCTCRCForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle, uint32_t* cvct_crc)
{
    mpe_SiTransportStreamEntry *ts_entry = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetCVCTCRCForTransportStreamHandle> ts_handle:0x%x\n",
            ts_handle);

    /* Parameter check */
    if ((ts_handle == MPE_SI_INVALID_HANDLE) || (cvct_crc == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    ts_entry = (mpe_SiTransportStreamEntry *) ts_handle;

    *cvct_crc = ts_entry->cvct_crc;
    return MPE_SI_SUCCESS;
}

/**
 * Retrieve cvct version given transport stream handle
 *
 * <i>mpe_siGetCVCTVersionForTransportStreamHandle()</i>
 *
 * @param ts_handle transport stream handle to get the cvct version from
 *
 * @return MPE_SI_SUCCESS if successfully retrieved cvct version, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetCVCTVersionForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle, uint32_t* cvct_version)
{
    mpe_SiTransportStreamEntry *ts_entry = NULL;
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetCVCTVersionForTransportStreamHandle> ts_handle:0x%x\n",
            ts_handle);

    /* Parameter check */
    if ((ts_handle == MPE_SI_INVALID_HANDLE) || (cvct_version == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    ts_entry = (mpe_SiTransportStreamEntry *) ts_handle;

    *cvct_version = ts_entry->cvct_version;
    return MPE_SI_SUCCESS;
}

/**
 * Retrieve pmt version given program handle
 *
 * <i>mpe_siGetPMTCRCForProgramHandle()</i>
 *
 * @param program_handle program handle to get the pmt crc from
 * @param crc is the output param to populate pmt crc
 *
 * @return MPE_SI_SUCCESS if successfully retrieved pmt version, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetPMTCRCForProgramHandle(mpe_SiProgramHandle program_handle,
        uint32_t* pmt_crc)
{
    mpe_SiProgramInfo *pi = (mpe_SiProgramInfo *) program_handle;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetPMTCRCForServiceHandle> program_handle:0x%x\n",
            program_handle);

    /* Parameter check */
    if ((program_handle == MPE_SI_INVALID_HANDLE) || (pmt_crc == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    *pmt_crc = pi->pmt_crc;

    /* If PSI is in the process of being acquired return from here
     until it is updated (disable acquiring lock) */
    if (pi->pmt_status == PMT_NOT_AVAILABLE)
    {
        return MPE_SI_NOT_AVAILABLE;
    }
    else if (pi->pmt_status == PMT_AVAILABLE_SHORTLY)
    {
        return MPE_SI_NOT_AVAILABLE_YET;
    }

    return MPE_SI_SUCCESS;
}

/**
 * Retrieve pmt version given service handle
 *
 * <i>mpe_siGetPMTCRCForServiceHandle()</i>
 *
 * @param service_handle si handle to get the pmt crc from
 * @param crc is the output param to populate pmt crc
 *
 * @return MPE_SI_SUCCESS if successfully retrieved pmt version, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetPMTCRCForServiceHandle(mpe_SiServiceHandle service_handle,
        uint32_t* pmt_crc)
{
    mpe_SiProgramInfo *pi = NULL;
    mpe_Error err = MPE_SUCCESS;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetPMTCRCForServiceHandle> service_handle:0x%x\n",
            service_handle);

    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (pmt_crc == NULL))
    {
        err = MPE_SI_INVALID_PARAMETER;
    }
    else
    {
        /* SI DB has not been populated yet */
        if (g_si_entry == NULL)
        {
            err = MPE_SI_NOT_AVAILABLE;
        }
        else
        {
            *pmt_crc = 0;
            pi = g_si_entry->program;
            if (pi == NULL)
            {
                mpe_SiTransportStreamEntry *ts =
                        (mpe_SiTransportStreamEntry *) g_si_entry->ts_handle;
                if (ts != NULL)
                {
                    if (ts->modulation_mode != MPE_SI_MODULATION_QAM_NTSC)
                        err = MPE_SI_INVALID_PARAMETER;
                    else
                        err = MPE_SI_NOT_FOUND;
                }
                else
                {
                    err = MPE_SI_NOT_AVAILABLE; // SPI service?
                }
            }
            else
            {
                *pmt_crc = pi->pmt_crc;
            }
        }
    }
    return err;
}

/**
 * Retrieve delivery system type for the given service handle
 *
 * <i>mpe_siGetDeliverySystemTypeForServiceHandle()</i>
 *
 * @param service_handle si handle
 * @param type is the output param to populate delivery system type
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetDeliverySystemTypeForServiceHandle(
        mpe_SiServiceHandle service_handle, mpe_SiDeliverySystemType* type)
{
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (type == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    *type = MPE_SI_DELIVERY_SYSTEM_TYPE_CABLE;

    return MPE_SI_SUCCESS;
}

/**
 * Returns CA System IDs array lenth associated with this service. This
 * information may be obtained from the CAT MPEG message or a system
 * specific conditional access descriptor (such as defined by Simulcrypt
 * or ATSC).
 *
 * An empty array is returned when no CA System IDs are available.
 */
mpe_Error mpe_siGetCASystemIdArrayLengthForServiceHandle(
        mpe_SiServiceHandle service_handle, uint32_t *length)
{
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (length == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    *length = 0;

    return MPE_SI_SUCCESS;
}

/**
 * Returns CA System IDs array associated with this service. This
 * information may be obtained from the CAT MPEG message or a system
 * specific conditional access descriptor (such as defined by Simulcrypt
 * or ATSC).
 *
 * An empty array is returned when no CA System IDs are available.
 */
mpe_Error mpe_siGetCASystemIdArrayForServiceHandle(
        mpe_SiServiceHandle service_handle, uint32_t* ca_array,
        uint32_t *length)
{
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (ca_array == NULL)
            || (length == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    *ca_array = 0;
    *length = 0;

    return MPE_SI_SUCCESS;
}

/**
 * Retrieve multiple instances flag for the given service handle
 *
 * <i>mpe_siGetMultipleInstancesFlagForServiceHandle()</i>
 *
 * @param service_handle si handle
 * @param hasMultipleInstances is the output param to populate multiple instances flag
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetMultipleInstancesFlagForServiceHandle(
        mpe_SiServiceHandle service_handle, mpe_Bool* hasMultipleInstances)
{
    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (hasMultipleInstances
            == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    *hasMultipleInstances = FALSE;

    return MPE_SI_SUCCESS;
}

/**
 * Get raw descriptor data for the service specified by the given service handle
 *
 * <i>mpe_siGetOuterDescriptorsForServiceHandle()</i>
 *
 * @param service_handle The service handle
 * @param descriptors A descriptor linked list will be returned
 * in this parameter
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetOuterDescriptorsForServiceHandle(
        mpe_SiServiceHandle service_handle, uint32_t* numDescriptors,
        mpe_SiMpeg2DescriptorList** descriptors)
{
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;
    mpe_SiProgramInfo *pi = NULL;
    /* Parameter check */
    if (service_handle == MPE_SI_INVALID_HANDLE || numDescriptors == NULL)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    pi = si_entry->program;

    if (pi == NULL)
    {
        return MPE_SI_NOT_AVAILABLE;
    }

    *numDescriptors = pi->number_outer_desc;
    *descriptors = pi->outer_descriptors;

    return MPE_SI_SUCCESS;
}

/**
 * Get number of service details given service handle
 *
 * <i>mpe_siGetNumberOfServiceDetailsForServiceHandle()</i>
 *
 * @param service_handle si handle to get the service details from
 * @param length is the output param
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetNumberOfServiceDetailsForServiceHandle(
        mpe_SiServiceHandle service_handle, uint32_t* length)
{
    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (length == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetNumberOfServiceDetailsForServiceHandle> \n");
    *length = 1;

    return MPE_SI_SUCCESS;
}

/**
 * Get service details given service handle
 *
 * <i>mpe_siGetServiceDetailsForServiceHandle()</i>
 *
 * @param service_handle si handle to get the service details from
 * @param arr is the output param to populate the service details handles
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetServiceDetailsForServiceHandle(
        mpe_SiServiceHandle service_handle, mpe_SiServiceDetailsHandle arr[],
        uint32_t length)
{
    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (arr == NULL) || (length
            == 0))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    arr[0] = (mpe_SiServiceDetailsHandle) service_handle;
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetServiceDetailsForServiceHandle>  returning MPE_SI_SUCCESS...\n");

    return MPE_SI_SUCCESS;
}

///////////////////////////////////////////////////////////////////////////////////////////////
/*
 Service Details handle specific (transport dependent)

 Note: For now service details handle and service handle are
 interchangeable in the native layer.

 */
///////////////////////////////////////////////////////////////////////////////////////////////

mpe_Error mpe_siGetIsFreeFlagForServiceDetailsHandle(
        mpe_SiServiceDetailsHandle handle, mpe_Bool* is_free)
{
    return mpe_siGetIsFreeFlagForServiceHandle((mpe_SiServiceHandle) handle,
            is_free);
}

mpe_Error mpe_siGetSourceIdForServiceDetailsHandle(
        mpe_SiServiceDetailsHandle handle, uint32_t* sourceId)
{
    return mpe_siGetSourceIdForServiceHandle((mpe_SiServiceHandle) handle,
            sourceId);
}

mpe_Error mpe_siGetFrequencyForServiceDetailsHandle(
        mpe_SiServiceDetailsHandle handle, uint32_t* frequency)
{
    return mpe_siGetFrequencyForServiceHandle((mpe_SiServiceHandle) handle,
            frequency);
}

mpe_Error mpe_siGetProgramNumberForServiceDetailsHandle(
        mpe_SiServiceDetailsHandle handle, uint32_t* progNum)
{
    return mpe_siGetProgramNumberForServiceHandle((mpe_SiServiceHandle) handle,
            progNum);
}

mpe_Error mpe_siGetModulationFormatForServiceDetailsHandle(
        mpe_SiServiceDetailsHandle handle, mpe_SiModulationMode* modFormat)
{
    return mpe_siGetModulationFormatForServiceHandle(
            (mpe_SiServiceHandle) handle, modFormat);
}

mpe_Error mpe_siGetNumberOfLongNamesForServiceDetailsHandle(
        mpe_SiServiceDetailsHandle handle, uint32_t *num_long_names)
{
    return mpe_siGetNumberOfLongNamesForServiceHandle(
            (mpe_SiServiceHandle) handle, num_long_names);
}

mpe_Error mpe_siGetLongNamesForServiceDetailsHandle(
        mpe_SiServiceDetailsHandle handle, char* languages[], char* longNames[])
{
    return mpe_siGetLongNamesForServiceHandle((mpe_SiServiceHandle) handle,
            languages, longNames);
}

mpe_Error mpe_siGetServiceDetailsLastUpdateTimeForServiceDetailsHandle(
        mpe_SiServiceDetailsHandle handle, mpe_TimeMillis *pTime)
{
    mpe_SiTableEntry *si_entry = NULL;
    mpe_SiServiceHandle service_handle = 0;

    /* Parameter check */
    if ((handle == MPE_SI_INVALID_HANDLE) || (pTime == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    service_handle = (mpe_SiServiceHandle) handle;

    si_entry = (mpe_SiTableEntry *) service_handle;

    *pTime = si_entry->ptime_service;

    return MPE_SI_SUCCESS;
}

mpe_Error mpe_siGetServiceTypeForServiceDetailsHandle(
        mpe_SiServiceDetailsHandle handle, mpe_SiServiceType* type)
{
    return mpe_siGetServiceTypeForServiceHandle((mpe_SiServiceHandle) handle,
            type);
}

mpe_Error mpe_siGetDeliverySystemTypeForServiceDetailsHandle(
        mpe_SiServiceDetailsHandle handle, mpe_SiDeliverySystemType* type)
{
    return mpe_siGetDeliverySystemTypeForServiceHandle(
            (mpe_SiServiceHandle) handle, type);
}

mpe_Error mpe_siGetServiceInformationTypeForServiceDetailsHandle(
        mpe_SiServiceDetailsHandle handle, mpe_SiServiceInformationType* type)
{
    /* Parameter check */
    if ((handle == MPE_SI_INVALID_HANDLE) || (type == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    *type = MPE_SI_SERVICE_INFORMATION_TYPE_SCTE_SI;

    return MPE_SI_SUCCESS;
}

mpe_Error mpe_siGetCASystemIdArrayLengthForServiceDetailsHandle(
        mpe_SiServiceDetailsHandle handle, uint32_t* length)
{
    if ((handle == MPE_SI_INVALID_HANDLE) || (length == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    *length = 0;

    return MPE_SI_SUCCESS;
}

mpe_Error mpe_siGetCASystemIdArrayForServiceDetailsHandle(
        mpe_SiServiceDetailsHandle handle, uint32_t* ca_array, uint32_t length)
{
    if ((handle == MPE_SI_INVALID_HANDLE) || (ca_array == NULL)
            || (length == 0))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    *ca_array = 0;
    return MPE_SI_SUCCESS;
}

mpe_Error mpe_siGetTransportStreamHandleForServiceDetailsHandle(
        mpe_SiServiceDetailsHandle handle,
        mpe_SiTransportStreamHandle *ts_handle)
{
    mpe_SiTableEntry *si_entry = NULL;

    /* Parameter check */
    if ((handle == MPE_SI_INVALID_HANDLE) || (ts_handle == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    /*
     * Service details and service entries are the
     * same thing.
     */
    si_entry = (mpe_SiTableEntry *) handle;

    *ts_handle = si_entry->ts_handle;

    return MPE_SI_SUCCESS;
}

mpe_Error mpe_siGetServiceHandleForServiceDetailsHandle(
        mpe_SiServiceDetailsHandle handle, mpe_SiServiceHandle* serviceHandle)
{
    /* MPE layer does not differentiate between service handle and service details handle */
    *serviceHandle = (mpe_SiServiceHandle) handle;

    /* TODO: Increment the ref_count for service handle? */

    return MPE_SI_SUCCESS;
}

///////////////////////////////////////////////////////////////////////////////////////////////
/*
 Rating Dimension specific

 */
///////////////////////////////////////////////////////////////////////////////////////////////
mpe_Error mpe_siGetNumberOfSupportedRatingDimensions(uint32_t *num)
{
    if (num == NULL)
        return MPE_SI_INVALID_PARAMETER;

    /* Temp hard coded per CEA 776A April 2001 pg 2 */
    *num = (uint32_t) g_RRT_US.dimensions_defined;

    return MPE_SI_SUCCESS;
}

mpe_Error mpe_siGetSupportedRatingDimensions(mpe_SiRatingDimensionHandle arr[])
{
    uint8_t dim_index = 0;

    if (arr == NULL)
        return MPE_SI_INVALID_PARAMETER;

    for (dim_index = 0; dim_index < g_RRT_US.dimensions_defined; dim_index++)
    {
        arr[dim_index] = (mpe_SiRatingDimensionHandle)
                & g_RRT_US.dimensions[dim_index];
    }

    return MPE_SI_SUCCESS;
}

mpe_Error mpe_siGetNumberOfLevelsForRatingDimensionHandle(
        mpe_SiRatingDimensionHandle handle, uint32_t* length)
{
    mpe_SiRatingDimension *dimension = NULL;

    if (handle == MPE_SI_INVALID_HANDLE || length == NULL)
        return MPE_SI_INVALID_PARAMETER;

    dimension = (mpe_SiRatingDimension*) handle;
    *length = (uint32_t) dimension->values_defined;

    return MPE_SI_SUCCESS;
}

mpe_Error mpe_siGetNumberOfNamesForRatingDimensionHandle(
        mpe_SiRatingDimensionHandle handle, uint32_t *num_names)
{
    uint32_t count = 0;
    mpe_SiRatingDimension *dimension = NULL;
    mpe_SiLangSpecificStringList *walker = NULL;

    if (handle == MPE_SI_INVALID_HANDLE || num_names == NULL)
        return MPE_SI_INVALID_PARAMETER;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetNumberOfNamesForRatingDimensionHandle> rating dimension handle:0x%x\n",
            handle);

    dimension = (mpe_SiRatingDimension*) handle;

    // Count the entries
    for (walker = dimension->dimension_names; walker != NULL; walker
            = walker->next, count++)
        ;

    *num_names = count;
    return MPE_SI_SUCCESS;
}

mpe_Error mpe_siGetNamesForRatingDimensionHandle(
        mpe_SiRatingDimensionHandle handle, char* languages[],
        char* dimensionNames[])
{
    int i = 0;
    mpe_SiRatingDimension *dimension = NULL;
    mpe_SiLangSpecificStringList *walker = NULL;

    if (handle == MPE_SI_INVALID_HANDLE || languages == NULL || dimensionNames
            == NULL)
        return MPE_SI_INVALID_PARAMETER;

    dimension = (mpe_SiRatingDimension*) handle;

    for (walker = dimension->dimension_names; walker != NULL; walker
            = walker->next, i++)
    {
        languages[i] = walker->language;
        dimensionNames[i] = walker->string;

        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_SI,
                "<mpe_siGetNamesForServiceHandle> source_name:%s, language:%s\n",
                walker->string, walker->language);
    }

    return MPE_SI_SUCCESS;
}

mpe_Error mpe_siGetNumberOfNamesForRatingDimensionHandleAndLevel(
        mpe_SiRatingDimensionHandle handle, uint32_t *num_names,
        uint32_t *num_descriptions, int levelNumber)
{
    uint32_t count = 0;
    mpe_SiRatingDimension *dimension = NULL;
    mpe_SiRatingValuesDefined *ratingValues = NULL;
    mpe_SiLangSpecificStringList *walker = NULL;

    if (handle == MPE_SI_INVALID_HANDLE || num_names == NULL
            || num_descriptions == NULL)
        return MPE_SI_INVALID_PARAMETER;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetNumberOfNamesForRatingDimensionHandle> rating dimension handle:0x%x\n",
            handle);

    dimension = (mpe_SiRatingDimension*) handle;
    ratingValues = &(dimension->rating_value[levelNumber]);

    // Count the entries
    for (walker = ratingValues->abbre_rating_value_text; walker != NULL; walker
            = walker->next, count++)
        ;
    *num_names = count;

    count = 0;

    for (walker = ratingValues->rating_value_text; walker != NULL; walker
            = walker->next, count++)
        ;
    *num_descriptions = count;

    return MPE_SI_SUCCESS;
}

mpe_Error mpe_siGetDimensionInformationForRatingDimensionHandleAndLevel(
        mpe_SiRatingDimensionHandle handle, char* nameLanguages[],
        char* names[], char* descriptionLanguages[], char* descriptions[],
        int levelNumber)
{
    mpe_SiRatingDimension *dimension = NULL;
    mpe_SiRatingValuesDefined *ratingValues = NULL;
    mpe_SiLangSpecificStringList *walker = NULL;
    int i = 0;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetDimensionInformationForRatingDimensionHandleAndLevel> service_handle:0x%x, level:%d\n",
            handle, levelNumber);

    /* Parameter check */
    if (handle == MPE_SI_INVALID_HANDLE || nameLanguages == NULL || names
            == NULL || descriptionLanguages == NULL || descriptions == NULL)
        return MPE_SI_INVALID_PARAMETER;

    dimension = (mpe_SiRatingDimension*) handle;
    ratingValues = &(dimension->rating_value[levelNumber]);

    // Names
    for (walker = ratingValues->abbre_rating_value_text; walker != NULL; walker
            = walker->next, i++)
    {
        nameLanguages[i] = walker->language;
        names[i] = walker->string;

        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_SI,
                "<mpe_siGetDimensionInformationForRatingDimensionHandleAndLevel> name:%s, language:%s\n",
                walker->string, walker->language);
    }

    i = 0;

    // Descriptions
    for (walker = ratingValues->rating_value_text; walker != NULL; walker
            = walker->next, i++)
    {
        descriptionLanguages[i] = walker->language;
        descriptions[i] = walker->string;

        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_SI,
                "<mpe_siGetDimensionInformationForRatingDimensionHandleAndLevel> name:%s, language:%s\n",
                walker->string, walker->language);
    }

    return MPE_SI_SUCCESS;
}

mpe_Error mpe_siGetRatingDimensionHandleByName(char* dimensionName,
        mpe_SiRatingDimensionHandle* outHandle)
{
    uint8_t dim_index = 0;

    if (dimensionName == NULL || outHandle == NULL)
        return MPE_SI_INVALID_PARAMETER;

    for (dim_index = 0; dim_index < g_RRT_US.dimensions_defined; dim_index++)
    {
        mpe_SiLangSpecificStringList *walker =
                g_RRT_US.dimensions[dim_index].dimension_names;

        while (walker != NULL)
        {
            if (strcmp(walker->string, dimensionName) == 0)
            {
                *outHandle = (mpe_SiRatingDimensionHandle)
                        & g_RRT_US.dimensions[dim_index];
                return MPE_SI_SUCCESS;
            }

            walker = walker->next;
        }
    }

    return MPE_SI_NOT_FOUND;
}

/**
 * Retrieve number of services
 *
 * <i>mpe_siGetTotalNumberOfServices()</i>
 *
 * @param num_services is the output param to populate number of services
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetTotalNumberOfServices(uint32_t *num_services)
{
    mpe_SiTableEntry *walker = NULL;
    uint32_t num = 0;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siGetTotalNumberOfServices> \n");
    /* Parameter check */
    if (num_services == NULL)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    // If SI DB is in either of these states return SI_NOT_AVAILABLE_YET
    // SI_ACQUIRING state is not useful for callers
    if ((g_si_state == SI_ACQUIRING) || (g_si_state == SI_NOT_AVAILABLE_YET))
        return MPE_SI_NOT_AVAILABLE_YET;
    else if (g_si_state == SI_DISABLED)
        return MPE_SI_NOT_AVAILABLE;

    walker = g_si_entry;

    while (walker)
    {
        mpe_SiTransportStreamEntry *ts =
                (mpe_SiTransportStreamEntry *) walker->ts_handle;
        if ((walker->channel_type == CHANNEL_TYPE_NORMAL)
                && ((walker->state == SIENTRY_MAPPED) || (walker->state
                        == SIENTRY_DEFINED_MAPPED)) && ((ts == NULL)
                || ((ts->frequency != MPE_SI_OOB_FREQUENCY) && (ts->frequency != MPE_SI_DSG_FREQUENCY) && (ts->frequency != MPE_SI_HN_FREQUENCY))))
        {
            num++;
        }
        walker = walker->next;
    }

    *num_services = num;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetTotalNumberOfServices> num_services:%d\n", *num_services);

    return MPE_SI_SUCCESS;
}

/**
 * Retrieve all the services
 *
 * <i>mpe_siGetAllServices()</i>
 *
 * @param array_services is the output param to populate services
 * @param num_services Input param: has size of array_services.
 * Output param: the actual number of service handles placed in array_services
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetAllServices(mpe_SiServiceHandle array_services[],
        uint32_t* num_services)
{
    mpe_Error err = MPE_SI_SUCCESS;
    mpe_SiTableEntry *walker = NULL;
    uint32_t i = 0;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siGetAllServices> \n");

    /* Parameter check */
    if (array_services == NULL || num_services == NULL || *num_services == 0)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    // If SI DB is in either of these states return SI_NOT_AVAILABLE_YET
    // SI_ACQUIRING state is not useful for callers
    if ((g_si_state == SI_ACQUIRING) || (g_si_state == SI_NOT_AVAILABLE_YET))
        return MPE_SI_NOT_AVAILABLE_YET;
    else if (g_si_state == SI_DISABLED)
        return MPE_SI_NOT_AVAILABLE;

    walker = g_si_entry;

    while (walker)
    {
        mpe_SiTransportStreamEntry *ts =
                (mpe_SiTransportStreamEntry *) walker->ts_handle;
        if ((walker->channel_type == CHANNEL_TYPE_NORMAL)
                && ((walker->state == SIENTRY_MAPPED) || (walker->state
                        == SIENTRY_DEFINED_MAPPED)) && ((ts == NULL)
                || ((ts->frequency != MPE_SI_OOB_FREQUENCY) && (ts->frequency != MPE_SI_DSG_FREQUENCY) && (ts->frequency != MPE_SI_HN_FREQUENCY))))
        {
            //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "mpe_siGetAllServices()...channel type is NORMAL 0x%x \n", walker->source_id);
            if (i == *num_services)
            {
                MPE_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_SI,
                        "<mpe_siGetAllServices> array_services (%d) too small for list\n",
                        *num_services);
                err = MPE_ENOMEM;
                break;
            }

            walker->ref_count++;
            array_services[i++] = (mpe_SiServiceHandle) walker;
        }
        else
        {
            //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "mpe_siGetAllServices()...channel type is not NORMAL 0x%x \n", walker->source_id);
        }
        walker = walker->next;
    }

    *num_services = i;

    return err;
}

/*
 Service Component Handles
 */

/**
 * Retrieve component handle given service handle and component pid
 *
 * <i>mpe_siGetServiceComponentHandleByPid()</i>
 *
 * @param service_handle si handle to get the component handle from
 * @param component_Pid is the input component pid
 * @param comp_handle is the output param to populate component handle
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetServiceComponentHandleByPid(
        mpe_SiServiceDetailsHandle service_handle, uint32_t component_Pid,
        mpe_SiServiceComponentHandle *comp_handle)
{
    mpe_Error err = MPE_SI_SUCCESS;
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;
    mpe_SiElementaryStreamList *elem_stream_list = NULL;
    mpe_SiProgramInfo *pi;
    int found = 0;

    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (component_Pid == 0)
            || (comp_handle == NULL))
    {
        err = MPE_SI_INVALID_PARAMETER;
    }
    else
    {
        /* SI DB has not been populated yet */
        if (g_si_entry == NULL)
        {
            err = MPE_SI_NOT_AVAILABLE;
        }
        else
        {
            *comp_handle = 0;
            pi = si_entry->program;
            if (pi == NULL)
            {
                mpe_SiTransportStreamEntry *ts =
                        (mpe_SiTransportStreamEntry *) si_entry->ts_handle;
                if (ts != NULL)
                {
                    if (ts->modulation_mode != MPE_SI_MODULATION_QAM_NTSC)
                        err = MPE_SI_INVALID_PARAMETER;
                    else
                        err = MPE_SI_NOT_FOUND;
                }
                else
                {
                    err = MPE_SI_NOT_AVAILABLE; // SPI service?
                }
            }
            else
            {
                elem_stream_list = pi->elementary_streams;
                while (elem_stream_list)
                {
                    if (component_Pid == elem_stream_list->elementary_PID)
                    {
                        *comp_handle
                                = (mpe_SiServiceComponentHandle) elem_stream_list;
                        elem_stream_list->ref_count++;
                        MPE_LOG(
                                MPE_LOG_DEBUG,
                                MPE_MOD_SI,
                                "<mpe_siGetServiceComponentHandleByPid> elem_stream_list:0x%p,  ref_count: %d\n",
                                elem_stream_list, elem_stream_list->ref_count);
                        found = 1;
                        break;
                    }
                    elem_stream_list = elem_stream_list->next;
                }

                if (!found)
                {
                    err = MPE_SI_NOT_FOUND;
                }
            }
        }
    }
    return err;
}

/**
 * Retrieve component handle given service handle and component name
 *
 * <i>mpe_siGetServiceComponentHandleByName()</i>
 *
 * @param service_handle si handle to get the component handle from
 * @param component_name is the input component name
 * @param comp_handle is the output param to populate component handle
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetServiceComponentHandleByName(
        mpe_SiServiceDetailsHandle service_handle, char *component_name,
        mpe_SiServiceComponentHandle *comp_handle)
{
    mpe_Error err = MPE_SI_SUCCESS;
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;
    mpe_SiElementaryStreamList *elem_stream_list = NULL;
    mpe_SiProgramInfo *pi;

    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (component_name == NULL)
            || (comp_handle == NULL))
    {
        err = MPE_SI_INVALID_PARAMETER;
    }
    else
    {
        /* SI DB has not been populated yet */
        if (g_si_entry == NULL)
        {
            err = MPE_SI_NOT_AVAILABLE;
        }
        else
        {
            *comp_handle = 0;
            pi = si_entry->program;
            if (pi == NULL)
            {
                mpe_SiTransportStreamEntry *ts =
                        (mpe_SiTransportStreamEntry *) si_entry->ts_handle;
                if (ts != NULL)
                {
                    if (ts->modulation_mode != MPE_SI_MODULATION_QAM_NTSC)
                        err = MPE_SI_INVALID_PARAMETER;
                    else
                        err = MPE_SI_NOT_FOUND;
                }
                else
                {
                    err = MPE_SI_NOT_AVAILABLE; // SPI service?
                }
            }
            else
            {
                int found = 0;
                elem_stream_list = pi->elementary_streams;
                while ((elem_stream_list != NULL) && !found)
                {
                    mpe_SiLangSpecificStringList *walker =
                            elem_stream_list->service_component_names;

                    while (walker != NULL)
                    {
                        if (strcmp(component_name, walker->string) == 0)
                        {
                            *comp_handle
                                    = (mpe_SiServiceComponentHandle) elem_stream_list;
                            elem_stream_list->ref_count++;
                            MPE_LOG(
                                    MPE_LOG_DEBUG,
                                    MPE_MOD_SI,
                                    "<mpe_siGetServiceComponentHandleByName> elem_stream_list:0x%p,  ref_count: %d\n",
                                    elem_stream_list,
                                    elem_stream_list->ref_count);
                            found = 1;
                            break;
                        }

                        walker = walker->next;
                    }
                    elem_stream_list = elem_stream_list->next;
                }
                if (!found)
                    err = MPE_SI_NOT_FOUND;
            }
        }
    }
    return err;
}

/**
 * Retrieve component handle given service handle and component tag
 *
 * <i>mpe_siGetServiceComponentHandleByTag()</i>
 *
 * @param service_handle si handle to get the component handle from
 * @param component_Pid is the input component tag
 * @param comp_handle is the output param to populate component handle
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetServiceComponentHandleByTag(
        mpe_SiServiceDetailsHandle service_handle, int component_tag,
        mpe_SiServiceComponentHandle *comp_handle)
{
    mpe_Error err = MPE_SI_SUCCESS;
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;
    mpe_SiProgramInfo *pi = NULL;
    mpe_SiElementaryStreamList *elem_stream_list = NULL;
    mpe_SiMpeg2DescriptorList *desc_list_walker = NULL;
    uint8_t *buffer = NULL;
    int found = 0;

    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (comp_handle == NULL))
    {
        err = MPE_SI_INVALID_PARAMETER;
    }
    else
    {
        /* SI DB has not been populated yet */
        if (g_si_entry == NULL)
        {
            err = MPE_SI_NOT_AVAILABLE;
        }
        else
        {
            *comp_handle = 0;
            pi = si_entry->program;
            if (pi == NULL)
            {
                mpe_SiTransportStreamEntry *ts =
                        (mpe_SiTransportStreamEntry *) si_entry->ts_handle;
                if (ts != NULL)
                {
                    if (ts->modulation_mode != MPE_SI_MODULATION_QAM_NTSC)
                        err = MPE_SI_INVALID_PARAMETER;
                    else
                        err = MPE_SI_NOT_FOUND;
                }
                else
                {
                    err = MPE_SI_NOT_AVAILABLE; // SPI service?
                }
            }
            else
            {
                elem_stream_list = pi->elementary_streams;
                while (elem_stream_list && !found)
                {
                    desc_list_walker = elem_stream_list->descriptors;
                    while (desc_list_walker)
                    {
                        if (desc_list_walker->tag
                                == MPE_SI_DESC_STREAM_IDENTIFIER_DESCRIPTOR)
                        {
                            buffer = desc_list_walker->descriptor_content;
                            if (*buffer == component_tag)
                            {
                                *comp_handle
                                        = (mpe_SiServiceComponentHandle) elem_stream_list;
                                elem_stream_list->ref_count++;
                                MPE_LOG(
                                        MPE_LOG_DEBUG,
                                        MPE_MOD_SI,
                                        "<mpe_siGetServiceComponentHandleByTag> found component - elem_stream_list:0x%p,  ref_count: %d\n",
                                        elem_stream_list,
                                        elem_stream_list->ref_count);
                                found = 1;
                                break;
                            }
                        }
                        desc_list_walker = desc_list_walker->next;
                    }
                    elem_stream_list = elem_stream_list->next;
                }

                if (!found)
                {
                    err = MPE_SI_NOT_FOUND;
                }
            }
        }
    }
    return err;
}

/**
 * Retrieve component handle given service handle and association tag
 *
 * <i>mpe_siGetServiceComponentHandleByAssociationTag()</i>
 *
 * @param service_handle si handle to get the component handle from
 * @param association_tag is the input association tag
 * @param comp_handle is the output param to populate component handle
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetServiceComponentHandleByAssociationTag(
        mpe_SiServiceHandle service_handle, uint16_t association_tag,
        mpe_SiServiceComponentHandle *comp_handle)
{
    mpe_Error err = MPE_SI_SUCCESS;
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;
    mpe_SiProgramInfo *pi = NULL;
    mpe_SiElementaryStreamList *elem_stream_list = NULL;
    mpe_SiMpeg2DescriptorList *desc_list_walker = NULL;
    uint16_t *data_ptr = NULL;
    int found = 0;

    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (comp_handle == NULL))
    {
        err = MPE_SI_INVALID_PARAMETER;
    }
    else
    {
        /* SI DB has not been populated yet */
        if (g_si_entry == NULL)
        {
            err = MPE_SI_NOT_AVAILABLE;
        }
        else
        {
            *comp_handle = 0;
            pi = si_entry->program;
            if (pi == NULL)
            {
                mpe_SiTransportStreamEntry *ts =
                        (mpe_SiTransportStreamEntry *) si_entry->ts_handle;
                if (ts != NULL)
                {
                    if (ts->modulation_mode != MPE_SI_MODULATION_QAM_NTSC)
                        err = MPE_SI_INVALID_PARAMETER;
                    else
                        err = MPE_SI_NOT_FOUND;
                }
                else
                {
                    err = MPE_SI_NOT_AVAILABLE; // SPI service?
                }
            }
            else
            {
                elem_stream_list = pi->elementary_streams;
                while (elem_stream_list && !found)
                {
                    desc_list_walker = elem_stream_list->descriptors;
                    while (desc_list_walker)
                    {
                        if (desc_list_walker->tag
                                == MPE_SI_DESC_ASSOCIATION_TAG)
                        {
                            data_ptr
                                    = (uint16_t*) desc_list_walker->descriptor_content;
                            if (ntohs(*data_ptr) == association_tag)
                            {
                                *comp_handle
                                        = (mpe_SiServiceComponentHandle) elem_stream_list;
                                elem_stream_list->ref_count++;
                                MPE_LOG(
                                        MPE_LOG_DEBUG,
                                        MPE_MOD_SI,
                                        "<mpe_siGetServiceComponentHandleByAssociationTag> found component - elem_stream_list:0x%p,  ref_count: %d\n",
                                        elem_stream_list,
                                        elem_stream_list->ref_count);
                                found = 1;
                                break;
                            }
                        }
                        desc_list_walker = desc_list_walker->next;
                    }
                    elem_stream_list = elem_stream_list->next;
                }

                if (!found)
                {
                    err = MPE_SI_NOT_FOUND;
                }
            }
        }
    }
    return err;
}

/**
 * Retrieve component handle given service handle and carousel id
 *
 * <i>mpe_siGetServiceComponentHandleByCarouselId()</i>
 *
 * @param service_handle si handle to get the component handle from
 * @param component_Pid is the input carousel id
 * @param comp_handle is the output param to populate component handle
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetServiceComponentHandleByCarouselId(
        mpe_SiServiceDetailsHandle service_handle, uint32_t carousel_id,
        mpe_SiServiceComponentHandle *comp_handle)
{
    mpe_Error err = MPE_SI_SUCCESS;
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;
    mpe_SiProgramInfo *pi = NULL;
    mpe_SiElementaryStreamList *elem_stream_list = NULL;
    mpe_SiMpeg2DescriptorList *desc_list_walker = NULL;
    uint32_t *data_ptr;
    int found = 0;

    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (comp_handle == NULL))
    {
        err = MPE_SI_INVALID_PARAMETER;
    }
    else
    {
        /* SI DB has not been populated yet */
        if (g_si_entry == NULL)
        {
            err = MPE_SI_NOT_AVAILABLE;
        }
        else
        {
            *comp_handle = 0;
            pi = si_entry->program;
            if (pi == NULL)
            {
                mpe_SiTransportStreamEntry *ts =
                        (mpe_SiTransportStreamEntry *) si_entry->ts_handle;
                if (ts != NULL)
                {
                    if (ts->modulation_mode != MPE_SI_MODULATION_QAM_NTSC)
                        err = MPE_SI_INVALID_PARAMETER;
                    else
                        err = MPE_SI_NOT_FOUND;
                }
                else
                {
                    err = MPE_SI_NOT_AVAILABLE; // SPI service?
                }
            }
            else
            {
                /* If PSI is in the process of being acquired return from here
                 until it is updated. */
                if (pi->pmt_status == PMT_NOT_AVAILABLE)
                {
                    err = MPE_SI_NOT_AVAILABLE;
                }
                else if (pi->pmt_status == PMT_AVAILABLE_SHORTLY)
                {
                    err = MPE_SI_NOT_AVAILABLE_YET;
                }
                else
                {
                    elem_stream_list = pi->elementary_streams;
                    while (elem_stream_list && !found)
                    {
                        desc_list_walker = elem_stream_list->descriptors;
                        while (desc_list_walker)
                        {
                            if (desc_list_walker->tag
                                    == MPE_SI_DESC_CAROUSEL_ID)
                            {
                                /* First 4 bytes of the descriptor content is the carousel id value */
                                data_ptr
                                        = (uint32_t*) desc_list_walker->descriptor_content;
                                if (ntohl(*data_ptr) == carousel_id)
                                {
                                    found = 1;
                                    *comp_handle
                                            = (mpe_SiServiceComponentHandle) elem_stream_list;
                                    elem_stream_list->ref_count++;
                                    MPE_LOG(
                                            MPE_LOG_DEBUG,
                                            MPE_MOD_SI,
                                            "<mpe_siGetComponentHandleByCarouselId> found component - elem_stream_list:0x%p,  ref_count: %d\n",
                                            elem_stream_list,
                                            elem_stream_list->ref_count);
                                    break;
                                }
                            }
                            desc_list_walker = desc_list_walker->next;
                        }
                        elem_stream_list = elem_stream_list->next;
                    }

                    if (!found)
                    {
                        err = MPE_SI_NOT_FOUND;
                    }
                }
            }
        }
    }
    return err;
}

/**
 * Retrieve component handle given service handle for the default OC
 *
 * <i>mpe_siGetServiceComponentHandleForDefaultCarousel()</i>
 *
 * @param service_handle si handle to get the component handle from
 * @param comp_handle is the output param to populate component handle
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetServiceComponentHandleForDefaultCarousel(
        mpe_SiServiceDetailsHandle service_handle,
        mpe_SiServiceComponentHandle *comp_handle)
{
    mpe_Error err = MPE_SI_SUCCESS;
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;
    mpe_SiProgramInfo *pi = NULL;
    mpe_SiElementaryStreamList *elem_stream_list = NULL;
    mpe_SiMpeg2DescriptorList *desc_list_walker = NULL;
    int found = 0;
    mpe_SiElementaryStreamList *defaultCarouselStream = NULL;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetServiceComponentHandleForDefaultCarousel> service_handle 0x%x \n",
            service_handle);

    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (comp_handle == NULL))
    {
        err = MPE_SI_INVALID_PARAMETER;
    }
    else
    {
        /* SI DB has not been populated yet */
        if (g_si_entry == NULL)
        {
            err = MPE_SI_NOT_AVAILABLE;
        }
        else
        {
            *comp_handle = 0;
            pi = si_entry->program;
            if (pi == NULL)
            {
                mpe_SiTransportStreamEntry *ts =
                        (mpe_SiTransportStreamEntry *) si_entry->ts_handle;
                if (ts != NULL)
                {
                    if (ts->modulation_mode != MPE_SI_MODULATION_QAM_NTSC)
                        err = MPE_SI_INVALID_PARAMETER;
                    else
                        err = MPE_SI_NOT_FOUND;
                }
                else
                {
                    err = MPE_SI_NOT_AVAILABLE; // SPI service?
                }
            }
            else
            {
                /* If PSI is in the process of being acquired return from here
                 until it is updated. */
                if (pi->pmt_status == PMT_NOT_AVAILABLE)
                {
                    err = MPE_SI_NOT_AVAILABLE;
                }
                else if (pi->pmt_status == PMT_AVAILABLE_SHORTLY)
                {
                    err = MPE_SI_NOT_AVAILABLE_YET;
                }
                else
                {
                    elem_stream_list = pi->elementary_streams;
                    MPE_LOG(
                            MPE_LOG_DEBUG,
                            MPE_MOD_SI,
                            "<mpe_siGetServiceComponentHandleForDefaultCarousel> progam: 0x%p elem_stream_list:0x%p \n",
                            pi, elem_stream_list);
                    while (elem_stream_list)
                    {
                        desc_list_walker = elem_stream_list->descriptors;
                        MPE_LOG(
                                MPE_LOG_DEBUG,
                                MPE_MOD_SI,
                                "<mpe_siGetServiceComponentHandleForDefaultCarousel> desc_list_walker:0x%p \n",
                                desc_list_walker);
                        // Walk the descriptors, looking for Carousel ID Descriptors
                        while (desc_list_walker)
                        {
                            if (desc_list_walker->tag
                                    == MPE_SI_DESC_CAROUSEL_ID)
                            {
                                found++;
                                defaultCarouselStream = elem_stream_list;
                                MPE_LOG(
                                        MPE_LOG_DEBUG,
                                        MPE_MOD_SI,
                                        "<mpe_siGetServiceComponentHandleForDefaultCarousel> elem_stream_list:0x%p,  ref_count: %d\n",
                                        elem_stream_list,
                                        elem_stream_list->ref_count);
                                break;
                            }
                            desc_list_walker = desc_list_walker->next;
                        }
                        elem_stream_list = elem_stream_list->next;
                    }

                    // If we only found one entry, increment it's ref count
                    // otherwise (0 or > 1) we couldn't find a default carousel
                    if ((found == 1) && defaultCarouselStream)
                    {
                        defaultCarouselStream->ref_count++;
                        *comp_handle
                                = (mpe_SiServiceComponentHandle) defaultCarouselStream;
                    }
                    else
                    {
                        err = MPE_SI_NOT_FOUND;
                    }
                }
            }
        }
    }
    return err;
}

/**
 * Release the service component handle
 *
 * <i>mpe_siReleaseServiceComponentHandle()</i>
 *
 * @param comp_handle component handle
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siReleaseServiceComponentHandle(
        mpe_SiServiceComponentHandle comp_handle)
{
    mpe_SiElementaryStreamList *elem_stream = NULL;

    /* Parameter check */
    if (comp_handle == MPE_SI_INVALID_HANDLE)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    elem_stream = (mpe_SiElementaryStreamList *) comp_handle;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siReleaseServiceComponentHandle> ref_count %d\n",
            elem_stream->ref_count);

    if (elem_stream->ref_count <= 1)
    {
        // What does this mean?
        // ref_count should never be less than 1, it means that
        // there may be un-balanced get/release service component
        // handles
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mpe_siReleaseServiceComponentHandle> error ref_count %d\n",
                elem_stream->ref_count);
        return MPE_SI_INVALID_PARAMETER;
    }

    --(elem_stream->ref_count);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siReleaseServiceComponentHandle> ref_count %d\n",
            elem_stream->ref_count);

    if ((elem_stream->ref_count == 1) && (elem_stream->valid == FALSE))
    {
        mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) elem_stream->service_reference;
        mpe_SiProgramInfo *pi = si_entry->program;
        if (remove_elementaryStream(&(pi->elementary_streams), elem_stream) == MPE_SI_SUCCESS)
        {
            (pi->number_elem_streams)--;
        }
        delete_elementary_stream(elem_stream);
    }

    return MPE_SI_SUCCESS;
}

/*
 Service Component accessors
 */

/**
 * Get component pid type given service handle and component handle
 *
 * <i>mpe_siGetPidForServiceComponentHandle()</i>
 *
 * @param comp_handle is the input component handle
 * @param comp_pid is the output param to populate component pid
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetPidForServiceComponentHandle(
        mpe_SiServiceComponentHandle comp_handle, uint32_t *comp_pid)
{
    mpe_SiElementaryStreamList *elem_stream_list = NULL;

    /* Parameter check */
    if ((comp_handle == MPE_SI_INVALID_HANDLE) || (comp_pid == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    *comp_pid = 0;

    elem_stream_list = (mpe_SiElementaryStreamList *) comp_handle;
    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetPidForServiceComponentHandle> comp_handle: 0x%p, PID : 0x%x\n",
            elem_stream_list, elem_stream_list->elementary_PID);
    *comp_pid = elem_stream_list->elementary_PID;

    return MPE_SI_SUCCESS;
}

/**
 * Retrieve number of language variants for the service component name for the given
 * service component handle
 *
 * <i>mpe_siGetNumberOfNamesForServiceComponentHandle()</i>
 *
 * @param comp_handle is the input component handle
 * @param num_names output param to populate the number of service component name
 *                  language variants
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 */
mpe_Error mpe_siGetNumberOfNamesForServiceComponentHandle(
        mpe_SiServiceComponentHandle comp_handle, uint32_t *num_names)
{
    uint32_t count = 0;
    mpe_SiElementaryStreamList *elem_stream_list = NULL;
    mpe_SiLangSpecificStringList *walker = NULL;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetNumberOfNamesForServiceComponentHandle> comp_handle:0x%x\n",
            comp_handle);

    /* Parameter check */
    if (comp_handle == MPE_SI_INVALID_HANDLE || num_names == NULL)
        return MPE_SI_INVALID_PARAMETER;

    elem_stream_list = (mpe_SiElementaryStreamList *) comp_handle;

    // Count the entries
    for (walker = elem_stream_list->service_component_names; walker != NULL; walker
            = walker->next, count++)
        ;

    *num_names = count;
    return MPE_SI_SUCCESS;
}

/**
 * Get arrays of service component name and associated languages given the component handle
 *
 * <i>mpe_siGetNamesForServiceComponentHandle()</i>
 *
 * @param comp_handle is the input component handle
 * @param languages is the output param to populate long service component languages
 * @param comp_names is the output param to populate service component names
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetNamesForServiceComponentHandle(
        mpe_SiServiceComponentHandle comp_handle, char* languages[],
        char* comp_names[])
{
    mpe_SiElementaryStreamList *elem_stream_list = NULL;
    mpe_SiLangSpecificStringList *walker = NULL;
    int i = 0;

    /* Parameter check */
    if (comp_handle == MPE_SI_INVALID_HANDLE || languages == NULL || comp_names
            == NULL)
        return MPE_SI_INVALID_PARAMETER;

    elem_stream_list = (mpe_SiElementaryStreamList*) comp_handle;
    walker = elem_stream_list->service_component_names;

    for (walker = elem_stream_list->service_component_names; walker != NULL; walker
            = walker->next, i++)
    {
        languages[i] = walker->language;
        comp_names[i] = walker->string;

        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_SI,
                "<mpe_siGetNamesForServiceComponentHandle> comp_name:%s, language:%s\n",
                walker->string, walker->language);
    }

    return MPE_SI_SUCCESS;
}

/**
 * Get component tag type given service handle and component handle
 *
 * <i>mpe_siGetComponentTagForServiceComponentHandle()</i>
 *
 * @param comp_handle is the input component handle
 * @param comp_tag is the output param to populate component tag
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetComponentTagForServiceComponentHandle(
        mpe_SiServiceComponentHandle comp_handle, int *comp_tag)
{
    mpe_SiElementaryStreamList *elem_stream_list = NULL;
    mpe_SiMpeg2DescriptorList *desc_list_walker = NULL;
    uint8_t *buffer;
    int found = 0;

    /* Parameter check */
    if ((comp_handle == MPE_SI_INVALID_HANDLE) || (comp_tag == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    /* Clear out the value */
    *comp_tag = 0;

    elem_stream_list = (mpe_SiElementaryStreamList *) comp_handle;

    if (elem_stream_list == NULL)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetComponentTagForServiceComponentHandle> comp_handle: 0x%p, PID : 0x%x\n",
            elem_stream_list, elem_stream_list->elementary_PID);

    desc_list_walker = elem_stream_list->descriptors;
    while ((desc_list_walker != NULL) && !found)
    {
        if (desc_list_walker->tag == MPE_SI_DESC_STREAM_IDENTIFIER_DESCRIPTOR)
        {
            found = 1;
            buffer = desc_list_walker->descriptor_content;
            *comp_tag = *buffer;
            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_SI,
                    "<mpe_siGetComponentTagForServiceComponentHandle> found tag 0x%x\n",
                    *comp_tag);
            break;
        }
        desc_list_walker = desc_list_walker->next;
    }

    /* If not found, return error */
    if (!found)
    {
        return MPE_SI_NOT_FOUND;
    }
    return MPE_SI_SUCCESS;
}

/**
 * Get association tag type given component handle
 *
 * <i>mpe_siGetAssociationTagForServiceComponentHandle()</i>
 *
 * @param comp_handle is the input component handle
 * @param assoc_tag is the output param to populate association tag
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetAssociationTagForServiceComponentHandle(
        mpe_SiServiceComponentHandle comp_handle, uint16_t *assoc_tag)
{
    mpe_SiElementaryStreamList *elem_stream_list = NULL;
    mpe_SiMpeg2DescriptorList *desc_list_walker = NULL;
    uint16_t *data_ptr = NULL;
    int found = 0;

    /* Parameter check */
    if ((comp_handle == MPE_SI_INVALID_HANDLE) || (assoc_tag == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    /* Clear out the value */
    *assoc_tag = 0;

    elem_stream_list = (mpe_SiElementaryStreamList *) comp_handle;

    if (elem_stream_list == NULL)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetAssociationTagForServiceComponentHandle> comp_handle: 0x%p, PID : 0x%x\n",
            elem_stream_list, elem_stream_list->elementary_PID);

    desc_list_walker = elem_stream_list->descriptors;
    while (desc_list_walker)
    {
        if (desc_list_walker->tag == MPE_SI_DESC_ASSOCIATION_TAG)
        {
            data_ptr = (uint16_t*) desc_list_walker->descriptor_content;
            *assoc_tag = ntohs(*data_ptr);
            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_SI,
                    "<mpe_siGetServiceComponentHandleByAssociationTag> found tag  assoc_tag: 0x%x\n",
                    *assoc_tag);
            found = 1;
            break;
        }
        desc_list_walker = desc_list_walker->next;
    }

    /* If not found, return error */
    if (!found)
    {
        return MPE_SI_NOT_FOUND;
    }
    return MPE_SI_SUCCESS;
}

/**
 * Get carousel id type given service handle and component handle
 *
 * <i>mpe_siGetCarouselIdForServiceComponentHandle()</i>
 *
 * @param comp_handle is the input component handle
 * @param carousel_id is the output param to populate carousel id
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetCarouselIdForServiceComponentHandle(
        mpe_SiServiceComponentHandle comp_handle, uint32_t *carousel_id)
{
    mpe_SiElementaryStreamList *elem_stream_list = NULL;
    mpe_SiMpeg2DescriptorList *desc_list_walker = NULL;
    uint32_t *data_ptr;
    int found = 0;

    /* Parameter check */
    if ((comp_handle == MPE_SI_INVALID_HANDLE) || (carousel_id == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    *carousel_id = 0;

    elem_stream_list = (mpe_SiElementaryStreamList *) comp_handle;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetCarouselIdForServiceComponentHandle> comp_handle: 0x%p, elementary_PID : 0x%x\n",
            elem_stream_list, elem_stream_list->elementary_PID);
    desc_list_walker = elem_stream_list->descriptors;
    while (desc_list_walker && !found)
    {
        if (desc_list_walker->tag == MPE_SI_DESC_CAROUSEL_ID)
        {
            /* First 4 bytes of the descriptor content is the carousel id value */
            data_ptr = (uint32_t*) desc_list_walker->descriptor_content;
            *carousel_id = ntohl(*data_ptr);
            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_SI,
                    "<mpe_siGetCarouselIdForServiceComponentHandle> found carousel Id 0x%x\n",
                    *carousel_id);
            found = 1;
            break;
        }
        desc_list_walker = desc_list_walker->next;
    }

    if (!found)
    {
        return MPE_SI_NOT_FOUND;
    }
    return MPE_SI_SUCCESS;

}

/**
 * Get component language type given service handle and component handle
 *
 * <i>mpe_siGetLanguageForServiceComponentHandle()</i>
 *
 * @param comp_handle is the input component handle
 * @param comp_lang is the output param to populate language
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetLanguageForServiceComponentHandle(
        mpe_SiServiceComponentHandle comp_handle, char **comp_lang)
{
    mpe_SiElementaryStreamList *elem_stream_list = NULL;

    /* Parameter check */
    if ((comp_handle == MPE_SI_INVALID_HANDLE) || (comp_lang == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    elem_stream_list = (mpe_SiElementaryStreamList *) comp_handle;
    *comp_lang = elem_stream_list->associated_language;
    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetLanguageForServiceComponentHandle> comp_handle: 0x%p, PID : 0x%x\n",
            elem_stream_list, elem_stream_list->elementary_PID);

    return MPE_SI_SUCCESS;
}

/**
 * Get elementary stream type given service handle and component handle
 *
 * <i>mpe_siGetStreamTypeForServiceComponentHandle()</i>
 *
 * @param comp_handle is the input component handle
 * @param stream_type is the output param to populate stream type
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetStreamTypeForServiceComponentHandle(
        mpe_SiServiceComponentHandle comp_handle,
        mpe_SiElemStreamType *stream_type)
{
    mpe_SiElementaryStreamList *elem_stream_list = NULL;

    /* Parameter check */
    if ((comp_handle == MPE_SI_INVALID_HANDLE) || (stream_type == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    *stream_type = 0;
    elem_stream_list = (mpe_SiElementaryStreamList *) comp_handle;

    *stream_type = elem_stream_list->elementary_stream_type;
    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetStreamTypeForServiceComponentHandle> comp_handle: 0x%p, PID : 0x%x\n",
            elem_stream_list, elem_stream_list->elementary_PID);

    return MPE_SI_SUCCESS;
}

/**
 * Get component last update time given component handle
 *
 * <i>mpe_siGetComponentLastUpdateTimeForServiceComponentHandle()</i>
 *
 * @param comp_handle is the input component handle
 * @param time is the output param to populate update time
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetComponentLastUpdateTimeForServiceComponentHandle(
        mpe_SiServiceComponentHandle comp_handle, mpe_TimeMillis *pTime)
{
    mpe_SiElementaryStreamList *elem_stream_list = NULL;

    /* Parameter check */
    if ((comp_handle == MPE_SI_INVALID_HANDLE) || (pTime == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    elem_stream_list = (mpe_SiElementaryStreamList *) comp_handle;
    *pTime = elem_stream_list->ptime_service_component;

    return MPE_SI_SUCCESS;
}

/**
 * Get service information type given component handle
 *
 * <i>mpe_siGetServiceInformationTypeForServiceComponentHandle()</i>
 *
 * @param comp_handle is the input component handle
 * @param service_info_type is the output param to populate service info type
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetServiceInformationTypeForServiceComponentHandle(
        mpe_SiServiceComponentHandle comp_handle,
        mpe_SiServiceInformationType* service_info_type)
{
    /* Parameter check */
    if ((comp_handle == MPE_SI_INVALID_HANDLE) || (service_info_type == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    *service_info_type = MPE_SI_SERVICE_INFORMATION_TYPE_SCTE_SI;

    return MPE_SI_SUCCESS;
}

/**
 * Get raw descriptor data for a particular service component given a component handle
 *
 * <i>mpe_siGetDescriptorsForServiceComponentHandle()</i>
 *
 * @param comp_handle The service component handle
 * @param descriptors A descriptor linked list will be returned
 * in this parameter
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetDescriptorsForServiceComponentHandle(
        mpe_SiServiceComponentHandle comp_handle, uint32_t* numDescriptors,
        mpe_SiMpeg2DescriptorList** descriptors)
{
    mpe_SiElementaryStreamList* si_esList;

    /* Parameter check */
    if (comp_handle == MPE_SI_INVALID_HANDLE || numDescriptors == NULL)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    si_esList = (mpe_SiElementaryStreamList*) comp_handle;

    *numDescriptors = si_esList->number_desc;
    *descriptors = si_esList->descriptors;

    return MPE_SI_SUCCESS;
}

/**
 * Get service details handle for a particular service component given a component handle
 *
 * <i>mpe_siGetServiceDetailsHandleForServiceComponentHandle()</i>
 *
 * @param comp_handle The service component handle
 * @param serviceDetailsHandle The service details handle will be returned
 * in this parameter
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetServiceDetailsHandleForServiceComponentHandle(
        mpe_SiServiceComponentHandle comp_handle,
        mpe_SiServiceDetailsHandle* serviceDetailsHandle)
{
    mpe_SiElementaryStreamList *elem_stream = NULL;

    /* Parameter check */
    if (comp_handle == MPE_SI_INVALID_HANDLE || serviceDetailsHandle == NULL)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetServiceDetailsHandleForServiceComponentHandle> comp_handle: 0x%x\n",
            comp_handle);

    elem_stream = (mpe_SiElementaryStreamList *) comp_handle;
    *serviceDetailsHandle
            = (mpe_SiServiceDetailsHandle) elem_stream->service_reference;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetServiceDetailsHandleForServiceComponentHandle> elem_stream->service_reference: 0x%x\n",
            elem_stream->service_reference);
    //TODO: Increment the service ref_count??

    return MPE_SI_SUCCESS;
}

/**
 * Get service handle for a particular service component given a component handle
 *
 * <i>mpe_siGetServiceHandleForServiceComponentHandle()</i>
 *
 * @param comp_handle The service component handle
 * @param serviceHandle The service handle will be returned
 * in this parameter
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siGetServiceHandleForServiceComponentHandle(
        mpe_SiServiceComponentHandle comp_handle,
        mpe_SiServiceHandle* serviceHandle)
{
    mpe_SiElementaryStreamList *elem_stream = NULL;
    mpe_SiTableEntry *si_entry = NULL;

    /* Parameter check */
    if (comp_handle == MPE_SI_INVALID_HANDLE || serviceHandle == NULL)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    elem_stream = (mpe_SiElementaryStreamList *) comp_handle;
    *serviceHandle = (mpe_SiServiceHandle) elem_stream->service_reference;

    si_entry = (mpe_SiTableEntry *) elem_stream->service_reference;
    // Do we need to increment ref count??
    si_entry->ref_count++;

    return MPE_SI_SUCCESS;
}

/* End  */

mpe_Error mpe_siGetNumberOfServiceEntriesForSourceId(uint32_t sourceId,
        uint32_t *num_entries)
{
    mpe_SiTableEntry *walker = NULL;
    mpe_Error retVal = MPE_SI_SUCCESS;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetNumberOfServiceEntriesForSourceId> sourceId:0x%x\n",
            sourceId);

    if ((num_entries == NULL) || ((sourceId == 0) || (sourceId > 65535)))
    {
        retVal = MPE_SI_INVALID_PARAMETER;
    }
    else
    {
        *num_entries = 0;

        walker = g_si_entry;

        while (walker)
        {
            if (walker->source_id == sourceId)
            {
                (*num_entries)++;
            }
            walker = walker->next;
        }
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_SI,
                "<mpe_siGetNumberOfServiceEntriesForSourceId> num_entries:%d\n",
                *num_entries);
    }

    return retVal;
}

mpe_Error mpe_siGetAllServiceHandlesForSourceId(uint32_t sourceId,
                              mpe_SiServiceHandle service_handle[],
                              uint32_t* num_entries)
{
    mpe_SiTableEntry *walker = NULL;
    mpe_Error retVal = MPE_SI_SUCCESS;
    uint32_t i=0;

    if ((num_entries == NULL) || (service_handle == NULL)
    || ((sourceId == 0) || (sourceId > 65535)))
    {
        retVal = MPE_SI_INVALID_PARAMETER;
    }
    else
    {
        walker = g_si_entry;

        while (walker)
        {
            if(walker->source_id == sourceId)
            {
                if (i >= *num_entries)
                {
                    // If we filled up the array, just increment the count to find the total
                    i++;
                }
                else
                {
                    service_handle[i++] = (mpe_SiServiceHandle) walker;
                    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
			    			"<mpe_siGetAllServiceHandlesForSourceId> handle:0x%p\n", walker);
                }
            }
            walker = walker->next;
        }
    }

    if(i > *num_entries)
    {
    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI,
    "<mpe_siGetAllServiceHandlesForSourceId> array size: %d\n", i);
    }

    *num_entries = i;

    return retVal;
}

/* Methods called by Table Parser */
/**
 *  This method is called when SVCT is acquired to populated sourceIds.
 *  If an SI entry does not exist this method creates a new entry and appends
 *  it to existing list.
 *
 *  Retrieve service entry handle given a source Id
 *
 * <i>mpe_siGetServiceEntryFromSourceId()</i>
 *
 * @param sourceId is the input param to obtain the service entry from
 * @param si_entry_handle is the output param to populate the service handle
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
/*  TODO:
 When new SVCT is acquired, after updating existing entries, walk the linked list and
 remove stale entries. That is, if any sourceIds are removed from SVCT then remove them from
 SI DB also.
 Currently we are not doing this but should be done as part of optimization.
 */
mpe_Error mpe_siGetServiceEntryFromSourceId(uint32_t sourceId,
        mpe_SiServiceHandle *service_handle)
{
    mpe_SiTableEntry *prevWalker = NULL;
    mpe_SiTableEntry *walker = NULL;
    mpe_Error retVal = MPE_SI_SUCCESS;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetServiceEntryFromSourceId> sourceId:0x%x\n", sourceId);

    if ((service_handle == NULL) || ((sourceId == 0) || (sourceId > 65535)))
    {
        retVal = MPE_SI_INVALID_PARAMETER;
    }
    else
    {
        *service_handle = MPE_SI_INVALID_HANDLE;

        walker = prevWalker = g_si_entry;

        while (walker)
        {
            if (walker->source_id == sourceId)
            {
                *service_handle = (mpe_SiServiceHandle) walker;
                break;
            }
            prevWalker = walker;
            walker = walker->next;
        }
        if (walker == NULL)
        {
            mpeos_memAllocP(MPE_MEM_SI, sizeof(mpe_SiTableEntry),
                    (void **) &(walker));
            if (walker == NULL)
            {
                MPE_LOG(
                        MPE_LOG_WARN,
                        MPE_MOD_SI,
                        "<mpe_siGetServiceEntryFromSourceId> Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");
                retVal = MPE_SI_OUT_OF_MEM;
            }
            else
            {
                init_si_entry(walker);
                walker->source_id = sourceId;
                *service_handle = (mpe_SiServiceHandle) walker;
                if (g_si_entry == NULL)
                {
                    g_si_entry = walker;
                }
                else
                {
                    //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siGetServiceEntryFromSourceId> set %p->next to %p\n", prevWalker, walker);
                    prevWalker->next = walker;
                }
                /* Increment the number of services */
                g_numberOfServices++;
            }

        }

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mpe_siGetServiceEntryFromSourceId> service_handle :0x%x\n",
                *service_handle);
    }

    return retVal;
}

mpe_Error mpe_siGetServiceEntryFromAppIdProgramNumber(uint32_t appId,
        uint32_t prog_num, mpe_SiServiceHandle *service_handle)
{
    mpe_SiTableEntry *walker = NULL;
    mpe_Error retVal = MPE_SI_SUCCESS;
    int found = 0;

    //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siGetServiceEntryFromAppIdProgramNumber> appId:0x%x\n", appId );

    if (service_handle == NULL || appId == 0)
    {
        retVal = MPE_SI_INVALID_PARAMETER;
    }
    else
    {
        *service_handle = MPE_SI_INVALID_HANDLE;
        walker = g_si_entry;
        while (walker)
        {
            if ((walker->isAppType) && (walker->app_id == appId))
            {
                if ((walker->program != NULL)
                        && ((walker->program->program_number
                                == PROGRAM_NUMBER_UNKNOWN)
                                || (walker->program->program_number == prog_num)))
                {
                    walker->program->program_number = prog_num;
                    *service_handle = (mpe_SiServiceHandle) walker;
                    found = 1;
                    break;
                }
            }
            walker = walker->next;
        }
    }

    /* Matching sourceId is not found, return error */
    if (!found)
    {
        retVal = MPE_SI_NOT_FOUND;
    }

    return retVal;
}

mpe_Error mpe_siGetProgramEntryFromTransportStreamAppIdProgramNumber(
        mpe_SiTransportStreamHandle ts_handle, uint32_t appId,
        uint32_t prog_num, mpe_SiProgramHandle *pi_handle)
{
    mpe_SiTransportStreamEntry *ts = (mpe_SiTransportStreamEntry *) ts_handle;
    mpe_SiProgramInfo *pi = NULL;
    mpe_SiTableEntry *walker = NULL;
    mpe_Error retVal = MPE_SI_SUCCESS;
    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetProgramEntryFromTransportStreamAppIdProgramNumber> 0x%x %d %d...\n",
            ts_handle, appId, prog_num);

    if (pi_handle == NULL)
    {
        retVal = MPE_SI_INVALID_PARAMETER;
    }
    else
    {
        *pi_handle = MPE_SI_INVALID_HANDLE;
        /* other Parameter check */
        {
            LINK *lp = llist_first(ts->services);
            while (lp)
            {
                walker = (mpe_SiTableEntry *) llist_getdata(lp);
                if (walker != NULL && walker->app_id == appId)
                {
                    pi = walker->program;
                    if (pi && pi->program_number == prog_num)
                        break;
                }
                lp = llist_after(lp);
            }
            *pi_handle = (mpe_SiProgramHandle) pi;
            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_SI,
                    "<mpe_siGetProgramEntryFromTransportStreamAppIdProgramNumber> pi_handle: 0x%p ...\n",
                    pi_handle);
        }
    }
    return retVal;

}

mpe_Error mpe_siGetServiceEntryFromHNstreamProgramNumber(mpe_HnStreamSession session,
        uint32_t prog_num, mpe_SiServiceHandle *service_handle)
{
    mpe_SiTableEntry *walker = NULL;
    mpe_Error retVal = MPE_SI_SUCCESS;
    int found = 0;

    if (service_handle == NULL || session == 0)
    {
        retVal = MPE_SI_INVALID_PARAMETER;
    }
    else
    {
        *service_handle = MPE_SI_INVALID_HANDLE;
        walker = g_si_entry;
        while (walker)
        {
            if (walker->hn_stream_session == session)
            {
                if ((walker->program != NULL)
                        && ((walker->program->program_number
                                == PROGRAM_NUMBER_UNKNOWN)
                                || (walker->program->program_number == prog_num)))
                {
                    walker->program->program_number = prog_num;
                    *service_handle = (mpe_SiServiceHandle) walker;
                    found = 1;
                    break;
                }
            }
            walker = walker->next;
        }
    }

    /* Matching sourceId is not found, return error */
    if (!found)
    {
        retVal = MPE_SI_NOT_FOUND;
    }

    return retVal;
}

mpe_Error mpe_siGetProgramEntryFromTransportStreamHNstreamProgramNumber(
        mpe_SiTransportStreamHandle ts_handle,mpe_HnStreamSession session,
        uint32_t prog_num, mpe_SiProgramHandle *pi_handle)
{
    mpe_SiTransportStreamEntry *ts = (mpe_SiTransportStreamEntry *) ts_handle;
    mpe_SiProgramInfo *pi = NULL;
    mpe_SiTableEntry *walker = NULL;
    mpe_Error retVal = MPE_SI_SUCCESS;
    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetProgramEntryFromTransportStreamHNstreamProgramNumber> 0x%x 0x%x %d...\n",
            ts_handle, session, prog_num);

    if (pi_handle == NULL)
    {
        retVal = MPE_SI_INVALID_PARAMETER;
    }
    else
    {
        *pi_handle = MPE_SI_INVALID_HANDLE;
        /* other Parameter check */
        {
            LINK *lp = llist_first(ts->services);
            while (lp)
            {
                walker = (mpe_SiTableEntry *) llist_getdata(lp);
                if (walker != NULL && walker->hn_stream_session == session)
                {
                    pi = walker->program;
                    if(pi)
                    {
                        MPE_LOG(
                                MPE_LOG_INFO,
                                MPE_MOD_SI,
                                "<mpe_siGetProgramEntryFromTransportStreamHNstreamProgramNumber> pi->program_number:%d\n", pi->program_number);

                    }
                    //if (pi && pi->program_number == prog_num)
                        break;
                }
                lp = llist_after(lp);
            }
            *pi_handle = (mpe_SiProgramHandle) pi;
            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_SI,
                    "<mpe_siGetProgramEntryFromTransportStreamHNstreamProgramNumber> pi_handle: 0x%x ...\n",
                    pi_handle);
        }
    }
    return retVal;

}

/**
 *  This method is called when SVCT is acquired to populated sourceIds.
 *  If an SI entry does not exist this method creates a new entry and appends
 *  it to existing list.
 *
 *  Retrieve service entry handle given a source Id
 *
 * <i>mpe_siGetServiceEntryFromSourceId()</i>
 *
 * @param sourceId is the input param to obtain the service entry from
 * @param si_entry_handle is the output param to populate the service handle
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
/*  TODO:
 When new SVCT is acquired, after updating existing entries, walk the linked list and
 remove stale entries. That is, if any sourceIds are removed from SVCT then remove them from
 SI DB also.
 Currently we are not doing this but should be done as part of optimization.
 */
mpe_Error mpe_siGetServiceEntryFromSourceIdAndChannel(uint32_t sourceId,
        uint32_t major_number, uint32_t minor_number,
        mpe_SiServiceHandle *service_handle, mpe_SiTransportHandle ts_handle,
        mpe_SiProgramHandle pi_handle)
{
    mpe_SiTableEntry *prevWalker = NULL;
    mpe_SiTableEntry *walker = NULL;
    mpe_Error retVal = MPE_SI_SUCCESS;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetServiceEntryFromSourceIdAndChannel> sourceId:0x%x  major_number:%d minor_number: %d, ts_handle:0x%x, pi_handle: 0x%x\n",
            sourceId, major_number, minor_number, ts_handle, pi_handle);

    /* Parameter check */
    if ((service_handle == NULL) || (ts_handle == MPE_SI_INVALID_HANDLE)
            || ((((mpe_SiTransportStreamEntry *) ts_handle)->modulation_mode
                    != MPE_SI_MODULATION_QAM_NTSC) && (pi_handle
                    == MPE_SI_INVALID_HANDLE)))
    {
        retVal = MPE_SI_INVALID_PARAMETER;
    }
    else
    {

        *service_handle = MPE_SI_INVALID_HANDLE;

        walker = prevWalker = g_si_entry;

        while (walker)
        {
            { // Find by channel #, which must be unique, if not 0.
                if ((walker->major_channel_number == major_number)
                        && (walker->minor_channel_number == minor_number)
                        && (walker->source_id == sourceId))
                {
                    *service_handle = (mpe_SiServiceHandle) walker;
                    break;
                }
            }
            prevWalker = walker;
            walker = walker->next;
        }

        if (walker != NULL)
        {
            if (walker->ts_handle != MPE_SI_INVALID_HANDLE)
            { // exists, or move?
                if (walker->ts_handle != ts_handle)
                { // In writer locked area, so no extra locking necessary.
                    LINK
                            *lp =
                                    llist_linkof(
                                            ((mpe_SiTransportStreamEntry *) walker->ts_handle)->services,
                                            (void *) walker);
                    if (lp != NULL)
                    {
                        llist_rmlink(lp);
                        ((mpe_SiTransportStreamEntry *) walker->ts_handle)->service_count--;
                        ((mpe_SiTransportStreamEntry *) walker->ts_handle)->visible_service_count--;
                    }
                    walker->ts_handle = MPE_SI_INVALID_HANDLE;
                    if (walker->program)
                    {
                        LINK *lp1 = llist_linkof(walker->program->services,
                                (void *) walker);
                        if (lp1 != NULL)
                        {
                            llist_rmlink(lp1);
                            walker->program->service_count--;
                        }
                        walker->program = NULL;
                    }
                }
                else
                { // Same TS, moved programs?
                    if (walker->program != NULL)
                    {
                        if (walker->program != (mpe_SiProgramInfo *) pi_handle)
                        {
                            LINK *lp = llist_linkof(walker->program->services,
                                    (void *) walker);
                            llist_rmlink(lp);
                            walker->program->service_count--;
                            walker->program = NULL;
                        }
                    }
                }
            }
            if (walker->ts_handle == MPE_SI_INVALID_HANDLE)
            {
                LINK *lp;
                walker->ts_handle = ts_handle;
                lp = llist_mklink((void *) walker);
                llist_append(
                        ((mpe_SiTransportStreamEntry *) ts_handle)->services,
                        lp);
                ((mpe_SiTransportStreamEntry *) walker->ts_handle)->service_count++;
                ((mpe_SiTransportStreamEntry *) walker->ts_handle)->visible_service_count++;
            }
            if (pi_handle != MPE_SI_INVALID_HANDLE)
            {
                LINK *lp;
                walker->program = (mpe_SiProgramInfo *) pi_handle;
                lp = llist_mklink((void *) walker);
                llist_append(walker->program->services, lp);
                walker->program->service_count++;
                MPE_LOG(
                        MPE_LOG_DEBUG,
                        MPE_MOD_SI,
                        "<mpe_siGetServiceEntryFromSourceIdAndChannel> program->service_count %d...\n",
                        walker->program->service_count);
            }
        }
        else
        /* If si entry is not found, create a new one append it to list */
        {
            mpeos_memAllocP(MPE_MEM_SI, sizeof(mpe_SiTableEntry),
                    (void **) &(walker));
            if (walker == NULL)
            {
                MPE_LOG(
                        MPE_LOG_WARN,
                        MPE_MOD_SI,
                        "<mpe_siGetServiceEntryFromSourceIdAndChannel> Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");
                retVal = MPE_SI_OUT_OF_MEM;
            }
            else
            {
                LINK *lp;
                init_si_entry(walker);
                walker->source_id = sourceId;
                walker->major_channel_number = major_number;
                walker->minor_channel_number = minor_number;
                *service_handle = (mpe_SiServiceHandle) walker;
                if (g_si_entry == NULL)
                {
                    g_si_entry = walker;
                }
                else
                {
                    MPE_LOG(
                            MPE_LOG_DEBUG,
                            MPE_MOD_SI,
                            "<mpe_siGetServiceEntryFromSourceIdAndChannel> set %p->next to %p\n",
                            prevWalker, walker);
                    prevWalker->next = walker;
                }
                if (pi_handle != MPE_SI_INVALID_HANDLE)
                {
                    LINK *lp1;
                    walker->program = (mpe_SiProgramInfo *) pi_handle;
                    lp1 = llist_mklink((void *) walker);
                    llist_append(walker->program->services, lp1);
                    walker->program->service_count++;
                    MPE_LOG(
                            MPE_LOG_DEBUG,
                            MPE_MOD_SI,
                            "<mpe_siGetServiceEntryFromSourceIdAndChannel> program->service_count %d...\n",
                            walker->program->service_count);
                }
                walker->ts_handle = ts_handle;
                lp = llist_mklink((void *) walker);
                llist_append(
                        ((mpe_SiTransportStreamEntry *) ts_handle)->services,
                        lp);
                ((mpe_SiTransportStreamEntry *) walker->ts_handle)->service_count++;
                MPE_LOG(
                        MPE_LOG_DEBUG,
                        MPE_MOD_SI,
                        "<mpe_siGetServiceEntryFromSourceIdAndChannel> ((mpe_SiTransportStreamEntry *)walker->ts_handle)->service_count %d...\n",
                        ((mpe_SiTransportStreamEntry *) walker->ts_handle)->service_count);
                /* Increment the number of services */
                g_numberOfServices++;
                MPE_LOG(
                        MPE_LOG_DEBUG,
                        MPE_MOD_SI,
                        "<mpe_siGetServiceEntryFromSourceIdAndChannel> g_numberOfServices %d...\n",
                        g_numberOfServices);
            }
        }
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_SI,
                "<mpe_siGetServiceEntryFromSourceIdAndChannel> service_handle:0x%x\n",
                *service_handle);
    }
    return retVal;
}

/*
 * Look up a service entry based on virtual channel number (found in SVCT-DCM, SVCT-VCM)
 * This method creates a new entry if one is not found in the database.
 * This method is called during parsing of SVCT-DCM/SVCT-VCM sub-table(s).
 */
void mpe_siGetServiceEntryFromChannelNumber(uint16_t channel_number,
        mpe_SiServiceHandle *service_handle)
{
    mpe_SiTableEntry *prevWalker = NULL;
    mpe_SiTableEntry *walker = NULL;
    //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siGetServiceEntryFromChannelNumber> channel_number: %d\n", channel_number);

    if ((service_handle == NULL) || (channel_number > 4095))
    {
        //MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "<mpe_siGetServiceEntryFromChannelNumber> MPE_SI_INVALID_PARAMETER channel_number: %d\n", channel_number);
    }
    else
    {
        *service_handle = MPE_SI_INVALID_HANDLE;

        walker = prevWalker = g_si_entry;

        while (walker)
        {
            // If a service entry exists all the members are set to default values
            if (walker->virtual_channel_number == channel_number)
            {
                *service_handle = (mpe_SiServiceHandle) walker;
                //MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "<mpe_siGetServiceEntryFromChannelNumber> found entry for channel_number: %d\n", channel_number);
                break;
            }
            prevWalker = walker;
            walker = walker->next;
        }

        // Create a new entry and append to list if one is not found
        if (walker == NULL)
        {
            mpeos_memAllocP(MPE_MEM_SI, sizeof(mpe_SiTableEntry),
                    (void **) &(walker));
            if (walker == NULL)
            {
                MPE_LOG(MPE_LOG_FATAL, MPE_MOD_SI,
                        "<mpe_siGetServiceEntryFromChannelNumber> MPE_SI_OUT_OF_MEM\n");
            }
            else
            {
                //MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "<mpe_siGetServiceEntryFromChannelNumber> creating new entry for channel_number: %d\n", channel_number);

                init_si_entry(walker);
                // Set the new channel number
                walker->virtual_channel_number = channel_number;
                *service_handle = (mpe_SiServiceHandle) walker;

                if (g_si_entry == NULL)
                {
                    g_si_entry = walker;
                }
                else
                {
                    prevWalker->next = walker;
                }
            }
        }
    }
}

void mpe_siInsertServiceEntryForChannelNumber(uint16_t channel_number,
        mpe_SiServiceHandle *service_handle)
{
    mpe_SiTableEntry *new_entry = NULL;
    mpe_SiTableEntry *walker = NULL;

    if ((service_handle == NULL) || (channel_number > 4095))
    {
        //MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "<mpe_siInsertServiceEntryForChannelNumber> MPE_SI_INVALID_PARAMETER channel_number: %d\n", channel_number);
    }
    else
    {
        *service_handle = MPE_SI_INVALID_HANDLE;

        mpeos_memAllocP(MPE_MEM_SI, sizeof(mpe_SiTableEntry),
                (void **) &(new_entry));
        if (new_entry == NULL)
        {
            MPE_LOG(MPE_LOG_FATAL, MPE_MOD_SI,
                    "<mpe_siInsertServiceEntryForChannelNumber> MPE_SI_OUT_OF_MEM\n");
        }
        else
        {
            init_si_entry(new_entry);
            // Set the new channel number
            new_entry->virtual_channel_number = channel_number;
            *service_handle = (mpe_SiServiceHandle) new_entry;

            if (g_si_entry == NULL)
            {
                g_si_entry = new_entry;
            }
            else
            {
                walker = g_si_entry;
                while(walker)
                {
                    if(walker->next == NULL)
                    {
                        walker->next = new_entry;
                        break;
                    }
                    else
                    {
                        walker = walker->next;
                    }
                }
            }
        }
    }
}

/*
 * This method is called when tables are initially acquired or updated
 * to correctly map service entry's transport stream handle and
 * program info details as well as service name entry info.
 */
void mpe_siUpdateServiceEntries()
{
    mpe_SiTransportStreamHandle ts_handle = MPE_SI_INVALID_HANDLE;
    mpe_SiProgramHandle prog_handle = MPE_SI_INVALID_HANDLE;
    uint32_t frequency_by_cds_ref = 0;
    mpe_SiModulationMode mode_by_mms_ref = MPE_SI_MODULATION_UNKNOWN;
    mpe_SiTableEntry *walker = NULL;
    mpe_siSourceNameEntry *source_name_entry = NULL;

    // Lock should be acquired by the caller

    walker = g_si_entry;
    while (walker)
    {
        frequency_by_cds_ref = 0;
        mode_by_mms_ref = MPE_SI_MODULATION_UNKNOWN;

        // For each service entry
        frequency_by_cds_ref = g_frequency[walker->freq_index];

        if (walker->transport_type == NON_MPEG_2_TRANSPORT)
        {
            mode_by_mms_ref = MPE_SI_MODULATION_QAM_NTSC;
        }
        else
        {
            mode_by_mms_ref = g_mode[walker->mode_index];
        }

        //MPE_LOG(MPE_LOG_TRACE6,
        //        MPE_MOD_SI,
        //        "<mpe_siUpdateServiceEntries> - frequency(%d) = (%d) modulation(%d) = %d.\n",
        //        frequency_by_cds_ref, mode_by_mms_ref);

        if((frequency_by_cds_ref != 0)  && (mode_by_mms_ref != MPE_SI_MODULATION_UNKNOWN))
        {
            mpe_siGetTransportStreamEntryFromFrequencyModulation(frequency_by_cds_ref, mode_by_mms_ref, &ts_handle);

            (void) mpe_siGetProgramEntryFromTransportStreamEntry(ts_handle, walker->program_number, &prog_handle);

            // Update the service entry with transport stream and program info
            mpe_siUpdateServiceEntry((mpe_SiServiceHandle)walker, ts_handle, prog_handle);
        }

        //
        // Now, find the source name entry and update
        source_name_entry = NULL;
        mpe_siGetSourceNameEntry(walker->isAppType ? walker->app_id : walker->source_id, walker->isAppType,
                                 &source_name_entry, FALSE);

        if (source_name_entry != NULL)
        {
            walker->source_name_entry = source_name_entry;
            source_name_entry->mapped = TRUE;
        }
        else
        {
            MPE_LOG(MPE_LOG_TRACE9, MPE_MOD_SI,
                  "<%s::mpe_siUpdateServiceEntries> - ERROR SourceName not found\n",
                  SIMODULE);
        }

        walker = walker->next;
    }

    //
    // Go through the source name table and find all entries that have not been mapped
    source_name_entry = g_si_sourcename_entry;
    while(source_name_entry != NULL)
    {
      if (!source_name_entry->mapped)
      {
        mpe_SiServiceHandle si_database_handle;

        // Call the create DSG Service handle, but no need to pass the soure name stuff, we already have it.
        mpe_siCreateDSGServiceHandle((uint32_t)source_name_entry->id, PROGRAM_NUMBER_UNKNOWN, NULL, NULL, &si_database_handle);

        // Set the newly returned database handle entry's source name entry
        if (si_database_handle != MPE_SI_INVALID_HANDLE)
        {
          ((mpe_SiTableEntry *)si_database_handle)->source_name_entry = source_name_entry;
          source_name_entry->mapped = TRUE;   // this source name now mapped to an si db entry
        }
      }
      source_name_entry = source_name_entry->next;
    }

}

/*
 * Update the given service entry with transport stream info and program info.
 * This method is called during parsing of SVCT-VCM sub-table.
 * */
void mpe_siUpdateServiceEntry(mpe_SiServiceHandle service_handle,
        mpe_SiTransportHandle ts_handle, mpe_SiProgramHandle pi_handle)
{
    mpe_SiTableEntry * walker = NULL;
    mpe_SiTableEntry * si_entry = NULL;

    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (ts_handle
            == MPE_SI_INVALID_HANDLE)
            || ((((mpe_SiTransportStreamEntry *) ts_handle)->modulation_mode
                    != MPE_SI_MODULATION_QAM_NTSC) && (pi_handle
                    == MPE_SI_INVALID_HANDLE)))
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI,
                "<mpe_siUpdateServiceEntry> MPE_SI_INVALID_PARAMETER\n");
    }
    else
    {
        walker = (mpe_SiTableEntry*) service_handle;

        if (walker != NULL)
        {
            if (walker->ts_handle != MPE_SI_INVALID_HANDLE)
            { // exists, or move?
                if (walker->ts_handle != ts_handle)
                { // In writer locked area, so no extra locking necessary.
                    LINK *lp = llist_linkof(((mpe_SiTransportStreamEntry *) walker->ts_handle)->services,
                                            (void *) walker);
                    if (lp != NULL)
                    {
                        llist_rmlink(lp);
                        ((mpe_SiTransportStreamEntry *) walker->ts_handle)->service_count--;
                        ((mpe_SiTransportStreamEntry *) walker->ts_handle)->visible_service_count--;
                    }
                    walker->ts_handle = MPE_SI_INVALID_HANDLE;
                    if (walker->program)
                    {
                        LINK *lp1 = llist_linkof(walker->program->services,
                                (void *) walker);
                        if (lp1 != NULL)
                        {
                            llist_rmlink(lp1);
                            walker->program->service_count--;
                        }
                        walker->program = NULL;
                    }
                    // Signal ServiceDetails update event (MODIFY)
                    {
                        MPE_LOG(
                                MPE_LOG_INFO,
                                MPE_MOD_SI,
                                "<mpe_siUpdateServiceEntry> transport stream changed..: signaling  MPE_SI_EVENT_SERVICE_DETAILS_UPDATE for handle:0x%x sourceId:0x%x\n", walker, walker->source_id);
                    	send_si_event(MPE_SI_EVENT_SERVICE_DETAILS_UPDATE, (mpe_SiServiceHandle)walker, MPE_SI_CHANGE_TYPE_MODIFY);
                    }
                }
                else
                { // Same TS, moved programs?
                    if (walker->program != NULL)
                    {
                        if (walker->program != (mpe_SiProgramInfo *) pi_handle)
                        {
                            LINK *lp = llist_linkof(walker->program->services,
                                    (void *) walker);
                            llist_rmlink(lp);
                            walker->program->service_count--;
                            walker->program = NULL;

                            // Signal ServiceDetails update event (MODIFY)
                            {
                                MPE_LOG(
                                        MPE_LOG_INFO,
                                        MPE_MOD_SI,
                                        "<mpe_siUpdateServiceEntry> program changed..: signaling  MPE_SI_EVENT_SERVICE_DETAILS_UPDATE for handle:0x%x sourceId:0x%x pn:%d\n",
                                        walker, walker->source_id, walker->program_number);
                            	send_si_event(MPE_SI_EVENT_SERVICE_DETAILS_UPDATE, (mpe_SiServiceHandle)walker, MPE_SI_CHANGE_TYPE_MODIFY);
                            }
                        }
                    }
                }
            }

            // Increment the global number of services
            g_numberOfServices++;

            if (pi_handle != MPE_SI_INVALID_HANDLE)
            {
            	LINK *lp;

            	LINK *lp2;
                mpe_SiProgramInfo *pgm = (mpe_SiProgramInfo *) pi_handle;

                lp2 = llist_first(pgm->services);
                while (lp2)
                {
                    si_entry = (mpe_SiTableEntry *) llist_getdata(lp2);
                    // This may be a dynamic entry created prior to acquiring OOB SI.
                    // State would be SIENTRY_UNSPECIFIED
                    if ((si_entry->state == SIENTRY_UNSPECIFIED)
                            && (si_entry->program == pgm)
                            && (si_entry->ts_handle == ts_handle))
                    {
                    	// Nothing to do, just log it
                        MPE_LOG(
                                MPE_LOG_DEBUG,
                                MPE_MOD_SI,
                                "<mpe_siUpdateServiceEntry> Found a previously created entry for this program...si_entry:0x%x state: %d\n",
                                si_entry, si_entry->state);
                        break;
                    }
                    lp2 = llist_after(lp2);
                }
                // If the program handle is not already set
                if ((mpe_SiProgramHandle)walker->program != pi_handle)
                {
                    walker->program = (mpe_SiProgramInfo *) pi_handle;
                    lp = llist_mklink((void *) walker);
                    llist_append(walker->program->services, lp);
                    walker->program->service_count++;
                }
            }

            if (walker->ts_handle == MPE_SI_INVALID_HANDLE)
            {
                LINK *lp;
                walker->ts_handle = ts_handle;
                lp = llist_mklink((void *) walker);
                llist_append(
                        ((mpe_SiTransportStreamEntry *) ts_handle)->services,
                        lp);
                ((mpe_SiTransportStreamEntry *) walker->ts_handle)->service_count++;
                ((mpe_SiTransportStreamEntry *) walker->ts_handle)->visible_service_count++;
            }
        }
    }
}

/*
 * This method returns false if any entry is in DEFINED but UNMAPPED state.
 * Implication is that the database considers initial SI acquisition to be complete if
 * all of the DCM defined virtual channels have been found mapped in the VCM.
 */
mpe_Bool mpe_siIsVCMComplete(void)
{
    mpe_SiTableEntry *walker = NULL;
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siIsVCMComplete> ...g_numberOfServices: %d\n",
            g_numberOfServices);

    /* Walk through the list of services to find defined channels (DCM) that are not yet found in VCM */
    walker = g_si_entry;
    while (walker)
    {
        // This means the entry is defined in the DCM but not yet found in VCM
        if (walker->state == SIENTRY_DEFINED_UNMAPPED)
        {
            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_SI,
                    "<mpe_siIsVCMComplete> ... entry defined in DCM, not yet present in VCM, returning FALSE, source_id:0x%x virtual_channel_number:%d channel_type:%d state:%d \n",
                    walker->source_id, walker->virtual_channel_number,
                    walker->channel_type, walker->state);
            return FALSE;
        }

        walker = walker->next;
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siIsVCMComplete> ...returning TRUE\n");
    return TRUE;
}

/*
 * This method sets the service entry state to 'PRESENT_IN_DCM' and 'DEFINED'
 * Called during parsing of SVCT-DCM sub-table.
 */
void mpe_siSetServiceEntryStateDefined(mpe_SiServiceHandle service_handle)
{
    mpe_SiTableEntry * si_entry = (mpe_SiTableEntry *) service_handle;

    MPE_LOG(MPE_LOG_TRACE7, MPE_MOD_SI,
            "<mpe_siSetServiceEntryStateDefined> ...service_handle:0x%x\n",
            service_handle);

    /* Parameter check */
    if (service_handle == MPE_SI_INVALID_HANDLE)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI,
                "<mpe_siSetServiceEntryStateDefined> MPE_SI_INVALID_PARAMETER\n");
    }
    else
    {
        si_entry->state |= SIENTRY_PRESENT_IN_DCM;
        si_entry->state |= SIENTRY_DEFINED;
    }
}

/*
 * This method sets the service entry state to 'PRESENT_IN_DCM' and 'UNDEFINED'
 * Called during parsing of SVCT-DCM sub-table.
 */
void mpe_siSetServiceEntryStateUnDefined(mpe_SiServiceHandle service_handle)
{
    mpe_SiTableEntry * si_entry = (mpe_SiTableEntry *) service_handle;

    MPE_LOG(MPE_LOG_TRACE7, MPE_MOD_SI,
            "<mpe_siSetServiceEntryStateUnDefined> ...service_handle:0x%x\n",
            service_handle);

    /* Parameter check */
    if (service_handle == MPE_SI_INVALID_HANDLE)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI,
                "<mpe_siSetServiceEntryStateUnDefined> MPE_SI_INVALID_PARAMETER\n");
    }
    else
    {
        si_entry->state |= SIENTRY_PRESENT_IN_DCM;
        si_entry->state &= ~SIENTRY_DEFINED;
    }
}

void mpe_siSetServiceEntryStateMapped(mpe_SiServiceHandle service_handle)
{
    mpe_SiTableEntry * si_entry = (mpe_SiTableEntry *) service_handle;

    MPE_LOG(MPE_LOG_TRACE7, MPE_MOD_SI,
            "<mpe_siSetServiceEntryStateMapped> ...service_handle:0x%x\n",
            service_handle);

    /* Parameter check */
    if (service_handle == MPE_SI_INVALID_HANDLE)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI,
                "<mpe_siSetServiceEntryStateMapped> MPE_SI_INVALID_PARAMETER\n");
    }
    else
    {
    	// (Global SI state is already 'FULLY_ACQUIRED')
    	if(g_si_state == SI_FULLY_ACQUIRED)
    	{
			// This means the Service entry is being added to VCM
    		// Current VCM state is 'UNMAPPED'
    		// and DCM state is 'DEFINED' 
    		// SIENTRY_DEFINED_UNMAPPED
			// Signal a ServiceDetailsUpdate event with changeType ADD

    		if(si_entry->state == SIENTRY_DEFINED_UNMAPPED)
			{
				MPE_LOG(
						MPE_LOG_INFO,
						MPE_MOD_SI,
						"<mpe_siSetServiceEntryStateMapped> service entry state updated..: signaling  MPE_SI_EVENT_SERVICE_DETAILS_UPDATE for handle:0x%x sourceId:0x%x\n", si_entry, si_entry->source_id);
				send_si_event(MPE_SI_EVENT_SERVICE_DETAILS_UPDATE, (mpe_SiServiceHandle)si_entry, MPE_SI_CHANGE_TYPE_ADD);
			}
	    }

        si_entry->state |= SIENTRY_MAPPED;
    }
}

/*
 * Returns the SI Entry state
 */
void mpe_siGetServiceEntryState(mpe_SiServiceHandle service_handle,
        uint32_t *state)
{
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;

    /* Parameter check */
    if (service_handle == MPE_SI_INVALID_HANDLE || state == NULL)
    {
        return;
    }

    *state = si_entry->state;

}

void mpe_siSetServiceEntryState(mpe_SiServiceHandle service_handle,
        uint32_t state)
{
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;

    /* Parameter check */
    if (service_handle == MPE_SI_INVALID_HANDLE)
    {
        return;
    }

    si_entry->state = state;
}

void mpe_siSetServiceEntriesStateDefined(uint16_t channel_number)
{
    mpe_SiTableEntry *walker = NULL;

    // There may be other entries in the SI DB with this
    // virtual channel number. Set the DCM state for those also
    walker = g_si_entry;
    while (walker)
    {
        // If a service entry exists all the members are set to default values
        if (walker->virtual_channel_number == channel_number)
        {
        	// This means the SI is initially acquired and now being updated
        	if(g_si_state == SI_FULLY_ACQUIRED)
        	{
        		// This entry state is changing from 'UNDEFINED' to 'DEFINED'
        		// and if this entry is already 'MAPPED', this implies a 'ADD'        		//
        		//
        		if(walker->state == SIENTRY_UNDEFINED_MAPPED)
        		{
                    MPE_LOG(
                            MPE_LOG_INFO,
                            MPE_MOD_SI,
                            "<mpe_siSetServiceEntryStateDefined> service entry state changed..: signaling  MPE_SI_EVENT_SERVICE_DETAILS_UPDATE (ADD) for handle:0x%x sourceId:0x%x\n", walker, walker->source_id);
                	send_si_event(MPE_SI_EVENT_SERVICE_DETAILS_UPDATE, (mpe_SiServiceHandle)walker, MPE_SI_CHANGE_TYPE_ADD);
        		}
        	}
            walker->state |= SIENTRY_PRESENT_IN_DCM;
            walker->state |= SIENTRY_DEFINED;
        }
        walker = walker->next;
    }
}

void mpe_siSetServiceEntriesStateUnDefined(uint16_t channel_number)
{
    mpe_SiTableEntry *walker = NULL;

    // There may be other entries in the SI DB with this
    // virtual channel number. Set the DCM state for those also
    walker = g_si_entry;
    while (walker)
    {
        // If a service entry exists all the members are set to default values
        if (walker->virtual_channel_number == channel_number)
        {
        	// This means the SI is initially acquired and now being updated
        	if(g_si_state == SI_FULLY_ACQUIRED)
        	{
        		// This entry state is changing from 'DEFINED' to 'UNDEFINED'
        		// and this entry is already 'MAPPED', this implies a 'REMOVE'
        		if(walker->state == SIENTRY_DEFINED_MAPPED)
        		{
                    MPE_LOG(
                            MPE_LOG_INFO,
                            MPE_MOD_SI,
                            "<mpe_siSetServiceEntriesStateUnDefined> service entry state changed..: signaling  MPE_SI_EVENT_SERVICE_DETAILS_UPDATE (REMOVE) for handle:0x%x sourceId:0x%x\n", walker, walker->source_id);
                	send_si_event(MPE_SI_EVENT_SERVICE_DETAILS_UPDATE, (mpe_SiServiceHandle)walker, MPE_SI_CHANGE_TYPE_REMOVE);
        		}
        	}
            walker->state |= SIENTRY_PRESENT_IN_DCM;
            walker->state &= ~SIENTRY_DEFINED;
        }
        walker = walker->next;
    }
}

/**
 *  This method is called when SVCT is acquired to retrieve the corresponding
 *  frequency the 'sourceId' is carried on.
 *
 *  Get frequency given cds_ref index
 *
 * <i>mpe_siGetFrequencyFromCDSRef()</i>
 *
 * @param cds_ref is the index into NIT_CDS table
 * @param frequency is the output param to populate the corresponding frequency
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
void mpe_siGetFrequencyFromCDSRef(uint8_t cds_ref, uint32_t * frequency)
{
    /* Parameter check */
    if ((cds_ref == 0) || (frequency == NULL))
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI,
                "<mpe_siGetFrequencyFromCDSRef> MPE_SI_INVALID_PARAMETER\n");
    }
    else
    {
        *frequency = g_frequency[cds_ref];
    }
}

/**
 *  This method is called when SVCT is acquired to retrieve the corresponding
 *  modulation mode given mms_reference
 *
 * <i>mpe_siGetModulationFromMMSRef()</i>
 *
 * @param mms_ref is the index into NIT_MMS table
 * @param modulation is the output param to populate the corresponding modulation
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
void mpe_siGetModulationFromMMSRef(uint8_t mms_ref,
        mpe_SiModulationMode * modulation)
{
    /* Parameter check */
    if ((mms_ref == 0) || (modulation == NULL))
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI,
                "<mpe_siGetModulationFromMMSRef> MPE_SI_INVALID_PARAMETER\n");
    }
    else
    {
        *modulation = g_mode[mms_ref];
    }
}

void mpe_siLockForWrite()
{
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siLockForWrite> ...\n");

    // SITP makes this call to acquire the global SIDB lock
    // block here until the condition object is set
    mpe_condGet(g_global_si_cond);
}

void mpe_siReleaseWriteLock()
{
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siReleaseWriteLock> ...\n");
    // SITP makes this call to release the global SIDB lock
    // release the lock
    mpe_condSet(g_global_si_cond);
}

mpe_Error mpe_siGetTransportStreamEntryFromFrequencyModulation(uint32_t freq,
        mpe_SiModulationMode mode, mpe_SiTransportStreamHandle *ts_handle)
{
    mpe_SiTransportStreamEntry *ts = NULL;

    /* Parameter check */
    if (ts_handle == NULL)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    // create_transport_stream_handle() will return an existing ts, if it can, create new if it can't.
    ts = (mpe_SiTransportStreamEntry *) create_transport_stream_handle(freq,
            mode);
    if (ts == NULL)
    {
        MPE_LOG(
                MPE_LOG_WARN,
                MPE_MOD_SI,
                "<mpe_siGetTransportStreamEntryFromFrequencyModulation> Mem allocation (ts) failed, returning MPE_SI_OUT_OF_MEM...\n");
        return MPE_SI_OUT_OF_MEM;
    }
    *ts_handle = (mpe_SiTransportStreamHandle) ts;
    return MPE_SI_SUCCESS;
}

mpe_Error mpe_siGetProgramEntryFromFrequencyProgramNumberModulation(
        uint32_t frequency, uint32_t program_number, mpe_SiModulationMode mode,
        mpe_SiProgramHandle *prog_handle)
{
    mpe_SiTransportHandle ts_handle = MPE_SI_INVALID_HANDLE;
    mpe_Error retVal = MPE_SI_SUCCESS;

    if (prog_handle == NULL)
    {
        retVal = MPE_SI_INVALID_PARAMETER;
    }
    else
    {
        *prog_handle = MPE_SI_INVALID_HANDLE;

        if (program_number > 65535)
        {
            MPE_LOG(
                    MPE_LOG_WARN,
                    MPE_MOD_SI,
                    "<mpe_siGetProgramEntryFromFrequencyProgramNumberModulation> Bad parameter! %d %d %d %p...\n",
                    frequency, program_number, mode, prog_handle);
            retVal = MPE_SI_INVALID_PARAMETER;
        }
        else
        {
            // create_transport_stream_handle() will return an existing ts, if it can, create new if it can't.
            ts_handle = (mpe_SiTransportHandle) create_transport_stream_handle(
                    frequency, mode);
            if (ts_handle == MPE_SI_INVALID_HANDLE)
            {
                MPE_LOG(
                        MPE_LOG_WARN,
                        MPE_MOD_SI,
                        "<mpe_siGetProgramEntryFromFrequencyProgramNumberModulation> Mem allocation (ts) failed, returning MPE_SI_OUT_OF_MEM...\n");
                retVal = MPE_SI_OUT_OF_MEM;
            }
            else
            {
                retVal = mpe_siGetProgramEntryFromTransportStreamEntry(
                        ts_handle, program_number, prog_handle);
            }
        }
    }
    return retVal;
}

mpe_Error mpe_siGetProgramEntryFromTransportStreamEntry(
        mpe_SiTransportStreamHandle ts_handle, uint32_t program_number,
        mpe_SiProgramHandle *prog_handle)
{
    mpe_SiTransportStreamEntry *ts = (mpe_SiTransportStreamEntry *) ts_handle;
    mpe_SiProgramInfo *pi = NULL;
    mpe_Error retVal = MPE_SI_SUCCESS;
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetProgramEntryFromTransportStreamEntry> 0x%x %d ...\n",
            ts_handle, program_number);

    if (prog_handle == NULL)
    {
        retVal = MPE_SI_INVALID_PARAMETER;
    }
    else
    {
        *prog_handle = MPE_SI_INVALID_HANDLE;
        /* other Parameter check */
        if ((ts_handle == MPE_SI_INVALID_HANDLE) || (program_number > 65535))
        {
            MPE_LOG(
                    MPE_LOG_WARN,
                    MPE_MOD_SI,
                    "<mpe_siGetProgramEntryFromTransportStreamEntry> Bad parameter! %d %d...\n",
                    ts_handle, program_number);
            retVal = MPE_SI_INVALID_PARAMETER;
        }
        else
        {
            if (ts->modulation_mode != MPE_SI_MODULATION_QAM_NTSC)
            {
                LINK *lp = llist_first(ts->programs);
                while (lp)
                {
                    pi = (mpe_SiProgramInfo *) llist_getdata(lp);
                    // We have the writelock, so list manipulation is safe.
                    if (pi)
                    {
                        //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siGetProgramEntryFromTransportStreamEntry> pi: 0x%x pi->program_number: %d...\n", pi, pi->program_number);
                        if (pi->program_number == program_number)
                        {
                            break;
                        }
                    }
                    lp = llist_after(lp);
                }

                if (lp == NULL)
                {
                    pi = create_new_program_info(ts, program_number);
                }

                if (pi != NULL)
                {
                    *prog_handle = (mpe_SiProgramHandle) pi;
                    MPE_LOG(
                            MPE_LOG_DEBUG,
                            MPE_MOD_SI,
                            "<mpe_siGetProgramEntryFromTransportStreamEntry> prog_handle: 0x%p ...\n",
                            pi);
                }
                else
                {
                    MPE_LOG(
                            MPE_LOG_WARN,
                            MPE_MOD_SI,
                            "<mpe_siGetProgramEntryFromTransportStreamEntry> Mem allocation (pi) failed, returning MPE_SI_OUT_OF_MEM...\n");
                    retVal = MPE_SI_OUT_OF_MEM;
                }
            }
        }
    }
    return retVal;
}

/**
 *  This method will be called by 'Table Parser' layer when it has acquired PAT/PMT.
 *  By this time SVCT should have already been acquired and sourceId-to-program_number
 *  mapping should already exist and also carrier frequency and modulation modes should
 *  have been populated from NIT_CD and NIT_MM tables.
 *  The table parser upon acquiring PAT/PMT will get the corresponding frequency
 *  (current tuned) from the media layer.
 *  The frequency and program_number pair will then be used to uniquely identify a service
 *  in the table. For VOD however, the sourceId will not exist in the table as they are not
 *  signalled in SVCT. In that case a new table entry will be created to represent VOD service.
 *
 *  Retrieve service entry handle given freq, prog_num and modulation
 *
 * <i>mpe_siGetServiceEntryFromFrequencyProgramNumberModulation()</i>
 *
 * @param freq is the input param to obtain the service entry from
 * @param program_number is the input param to obtain the service entry from
 * @param mode is the input modulation mode
 * @param si_entry_handle is the output param to populate the service handle
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
// CALLER MUST BE HOLDING WRITE LOCK WHEN CALLING THIS FUNCTION!
// mpe_siLockForWrite();

//
// No longer a valid method of creating things.  A Transportstream (optionally with program) should be created and
// then a service entry can be associated with in in the next step.
//

mpe_Error mpe_siGetServiceEntryFromFrequencyProgramNumberModulation(
        uint32_t freq, uint32_t program_number, mpe_SiModulationMode mode,
        mpe_SiServiceHandle *si_entry_handle)
{
    /*
     This method is called by the table parser layer when a new VCT or PAT/PMT is detected.
     If an entry is found in the existing list a handle to that is returned. If not found,
     a new entry is created.
     */
    mpe_SiTransportStreamEntry *ts = NULL;
    mpe_SiTableEntry *new_si_entry = NULL;
    mpe_SiProgramInfo *pgm = NULL;
    int found = 0;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siGetServiceEntryFromFrequencyProgramNumberModulation> Freq: %d  Program: %d  Modulation: %d\n",
            freq, program_number, mode);

    /* Parameter check */
    if (si_entry_handle == NULL)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    *si_entry_handle = MPE_SI_INVALID_HANDLE;

    // create_transport_stream_handle() will return an existing ts, if it can, create new if it can't.
    ts = (mpe_SiTransportStreamEntry *) create_transport_stream_handle(freq,
            mode);
    if (ts == NULL)
    {
        MPE_LOG(
                MPE_LOG_WARN,
                MPE_MOD_SI,
                "<mpe_siGetServiceEntryFromFrequencyProgramNumberModulation> Mem allocation (ts) failed, returning MPE_SI_OUT_OF_MEM...\n");
        return MPE_SI_OUT_OF_MEM;
    }

    mpe_mutexAcquire(ts->list_lock);

    if (mode != MPE_SI_MODULATION_QAM_NTSC)
    {
        LINK *lp = llist_first(ts->programs);
        while (lp)
        {
            pgm = (mpe_SiProgramInfo *) llist_getdata(lp);
            if (pgm)
            {
                if (pgm->program_number == program_number)
                    break;
            }
            lp = llist_after(lp);
        }
        if (pgm == NULL)
        {
            pgm = create_new_program_info(ts, program_number);
            if (pgm == NULL)
            {
                MPE_LOG(
                        MPE_LOG_WARN,
                        MPE_MOD_SI,
                        "<mpe_siGetServiceEntryFromFrequencyProgramNumberModulation> Mem allocation (pgm) failed, returning MPE_SI_OUT_OF_MEM...\n");
                mpe_mutexRelease(ts->list_lock);
                return MPE_SI_OUT_OF_MEM;
            }
        }
        {
            lp = llist_first(pgm->services);
            if (lp != NULL)
            {
                new_si_entry = (mpe_SiTableEntry *) llist_getdata(lp);
            }
        }
    }
    else
    {
        {
            LINK *lp = llist_first(ts->services);
            if (lp != NULL)
            {
                new_si_entry = (mpe_SiTableEntry *) llist_getdata(lp);
            }
        }
    }

    if (new_si_entry == NULL)
    {
        mpe_Error tRetVal = mpeos_memAllocP(MPE_MEM_SI,
                sizeof(mpe_SiTableEntry), (void **) &(new_si_entry));
        if ((MPE_SI_SUCCESS != tRetVal) || (new_si_entry == NULL))
        {
            MPE_LOG(
                    MPE_LOG_WARN,
                    MPE_MOD_SI,
                    "<mpe_siGetServiceEntryFromFrequencyProgramNumberModulation> Mem allocation (entry) failed, returning MPE_SI_OUT_OF_MEM...\n");
            mpe_mutexRelease(ts->list_lock);
            return MPE_SI_OUT_OF_MEM;
        }
        if (pgm != NULL)
        {
            LINK *lp = llist_mklink((void *) new_si_entry);
            llist_append(pgm->services, lp);
            pgm->service_count++;
        }

        {
            LINK *lp = llist_mklink((void *) new_si_entry);
            llist_append(ts->services, lp);
            ts->service_count++;
        }
    }
    else
        found = 1;

    mpe_mutexRelease(ts->list_lock);

    /*
     If si entry is not found, create a new one append it to list
     For VOD an si entry will not exist
     */
    if (!found)
    {
        mpe_SiTableEntry *walker = NULL;
        /* Initialize all the fields (some fields are set to default values as defined in spec) */
        init_si_entry(new_si_entry);
        /* set the source id to a known value */
        /* For dynamic services (ex: vod) since sourceid is unknown, just default it to SOURCEID_UNKNOWN/-1.*/
        new_si_entry->source_id = SOURCEID_UNKNOWN;

        new_si_entry->ts_handle = (uint32_t) ts;
        new_si_entry->program = pgm; // Might be null, if Analog service.

        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_SI,
                "<mpe_siGetServiceEntryFromFrequencyProgramNumberModulation> ts_handle:0x%x\n",
                new_si_entry->ts_handle);
        if (freq == MPE_SI_OOB_FREQUENCY || freq == MPE_SI_DSG_FREQUENCY || freq == MPE_SI_HN_FREQUENCY)
        {
            new_si_entry->channel_type = CHANNEL_TYPE_NORMAL;

            MPE_LOG(
                    MPE_LOG_INFO,
                    MPE_MOD_SI,
                    "<mpe_siGetServiceEntryFromFrequencyProgramNumberModulation> got OOB_FREQ si_entry:0x%p ts_handle:0x%x , appID=%u\n",
                    new_si_entry, new_si_entry->ts_handle,
                    new_si_entry->source_id);

        }

        *si_entry_handle = (mpe_SiServiceHandle) new_si_entry;

        /* append new entry to the dynamic list */
        if (g_si_entry == NULL)
        {
            g_si_entry = new_si_entry;
        }
        else
        {
            walker = g_si_entry;
            while (walker)
            {
                if (walker->next == NULL)
                {
                    MPE_LOG(
                            MPE_LOG_DEBUG,
                            MPE_MOD_SI,
                            "<mpe_siGetServiceEntryFromFrequencyProgramNumberModulation> set %p->next to %p\n",
                            walker, new_si_entry);
                    walker->next = new_si_entry;
                    break;
                }
                walker = walker->next;
            }
        }

        /* Increment the number of services */
        g_numberOfServices++;
    }

    *si_entry_handle = (mpe_SiServiceHandle) new_si_entry;

    return MPE_SI_SUCCESS;
}

/* Deprecated */
mpe_Error mpe_siGetServiceEntryFromFrequencyProgramNumberModulationAppId(
        uint32_t freq, uint32_t program_number, mpe_SiModulationMode mode,
        uint32_t app_id, mpe_SiServiceHandle *si_entry_handle)
{
    /*
     mpe_SiTableEntry *walker = NULL;
     mpe_SiTableEntry *prevWalker = NULL;
     mpe_SiTableEntry *new_si_entry = NULL;
     int found = 0;
     uint32_t freq_to_check = 0;

     // Parameter check
     if(si_entry_handle == NULL)
     {
     return MPE_SI_INVALID_PARAMETER;
     }

     *si_entry_handle = MPE_SI_INVALID_HANDLE;

     // TODO - Fix this
     walker = prevWalker = g_si_entry;


     //   This method is called by the table parser layer when a new VCT or PAT/PMT is detected.
     //   If an entry is found in the existing list a handle to that is returned. If not found,
     //   a new entry is created.
     while(walker)
     {
     freq_to_check = (walker->freq_index != 0) ? g_frequency[walker->freq_index] : walker->frequency;
     if( (walker->program_number == program_number) &&
     (freq_to_check == freq) )
     {
     //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siGetServiceEntryFromFrequencyProgramNumberModulation> SI Entry found...\n\n");

     if( ((app_id != 0 ) && (walker->app_id != 0) && (walker->app_id == app_id))
     || (app_id == 0))
     {
     walker->state = UPDATED;
     *si_entry_handle = (mpe_SiServiceHandle)walker;

     found = 1;
     break;
     }

     }
     prevWalker = walker;
     walker = walker->next;
     }


     //   If si entry is not found, create a new one append it to list
     if(!found)
     {
     // Can we assume this is a VOD service here?
     if( MPE_SI_SUCCESS != mpeos_memAllocP(MPE_MEM_SI, sizeof(mpe_SiTableEntry), (void **)&(new_si_entry)) )
     {
     *si_entry_handle = 0 ;

     MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI, "<mpe_siGetServiceEntryFromFrequencyProgramNumberModulation> Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");

     return MPE_SI_OUT_OF_MEM;
     }

     //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siGetServiceEntryFromFrequencyProgramNumberModulation> creating new SI Entry...freq:%d, prog_num:%d\n", freq, program_number);

     // Initialize all the fields (some fields are set to default values as defined in spec)
     init_si_entry(new_si_entry);
     new_si_entry->program_number = program_number;
     new_si_entry->frequency = freq; // For DSG the frequecny should be MPE_SI_OOB_FREQUENCY (set in SITP)
     new_si_entry->modulation_mode = mode;

     if(freq == MPE_SI_OOB_FREQUENCY)
     {
     new_si_entry->channel_type = CHANNEL_TYPE_NORMAL;
     if (app_id > 0)
     {
     new_si_entry->isAppType = true;
     new_si_entry->app_id = app_id;
     }

     MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "<mpe_siGetServiceEntryFromFrequencyProgramNumberModulation> got OOB_FREQ si_entry:0x%x ts_handle:0x%x , appID=%ld\n",new_si_entry, new_si_entry->ts_handle, new_si_entry->source_id);
     }

     if(mode == MPE_SI_MODULATION_QAM_NTSC)
     {
     // We know that an analog service does, and always will, have no components
     new_si_entry->number_elem_streams = 0;
     new_si_entry->pmt_status = PMT_AVAILABLE;

     // For analog the program number is set to -1 (fix for 5724)
     //new_si_entry->program_number = (uint32_t) -1;
     }


     if( (new_si_entry->ts_handle = get_transport_stream_handle(freq, mode)) == 0)
     {
     new_si_entry->ts_handle = create_transport_stream_handle(freq, mode);
     }

     MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siGetServiceEntryFromFrequencyProgramNumberModulation> ts_handle:0x%x\n", new_si_entry->ts_handle);

     {
     mpe_SiTransportStreamEntry *ts_entry = (mpe_SiTransportStreamEntry *)new_si_entry->ts_handle;
     (ts_entry->service_count)++;
     // Increment the visible service count also
     //(channel type is not known for dynamic services, they are
     //not signaled in OOB VCT)
     (ts_entry->visible_service_count)++;
     //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siGetServiceEntryFromFrequencyProgramNumberModulation> ts_entry->visible_service_count...%d\n", ts_entry->visible_service_count);
     }

     *si_entry_handle = (mpe_SiServiceHandle)new_si_entry;

     // append new entry to the dynamic list
     if(g_si_entry == NULL)
     {
     g_si_entry = new_si_entry;
     }
     else
     {
     walker = g_si_entry;
     while(walker)
     {
     if(walker->next == NULL)
     {
     walker->next = new_si_entry;
     break;
     }
     walker = walker->next;
     }
     }

     // Increment the number of services
     g_numberOfServices++;
     }
     */
    return MPE_SI_SUCCESS;
}

/*
 mpe_Error mpe_siGetServiceEntryFromFrequencyProgramNumberModulationSourceId(uint32_t freq, uint32_t program_number, mpe_SiModulationMode mode, uint32_t source_id, mpe_SiServiceHandle *si_entry_handle)
 {
 mpe_SiTableEntry *walker = NULL;
 mpe_SiTableEntry *prevWalker = NULL;
 mpe_SiTableEntry *new_si_entry = NULL;
 mpe_SiTableEntry *saved_walker = NULL;
 int found = 0;
 uint32_t freq_to_check = 0;
 uint32_t appID = 0;

 // Parameter check
 if(si_entry_handle == NULL)
 {
 return MPE_SI_INVALID_PARAMETER;
 }

 *si_entry_handle = MPE_SI_INVALID_HANDLE;

 walker = prevWalker = g_si_entry;


 //   This method is called by the table parser layer when a new VCT or PAT/PMT is detected.
 //   If an entry is found in the existing list a handle to that is returned. If not found,
 //   a new entry is created.
 while(walker)
 {
 freq_to_check = (walker->freq_index != 0) ? g_frequency[walker->freq_index] : walker->frequency;
 if( (walker->program_number == program_number) &&
 (freq_to_check == freq) )
 {
 //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siGetServiceEntryFromFrequencyProgramNumberModulation> SI Entry found...\n\n");

 // If incoming source_id does not match existing source_id
 // means we need to create a new service entry
 // This will have same frequency, program_number, mode triple
 // as this service entry, but a different source_id

 if( (walker->source_id == SOURCEID_UNKNOWN) ||
 ((walker->source_id != SOURCEID_UNKNOWN) && (walker->source_id == source_id) )
 {
 walker->state = UPDATED;
 *si_entry_handle = (mpe_SiServiceHandle)walker;

 found = 1;
 break;
 }
 else
 {
 saved_walker = walker;
 }

 }
 prevWalker = walker;
 walker = walker->next;
 }


 //   If si entry is not found, create a new one append it to list
 //   For VOD an si entry will not exist
 if(!found)
 {
 // Can we assume this is a VOD service here?
 if( MPE_SI_SUCCESS != mpeos_memAllocP(MPE_MEM_SI, sizeof(mpe_SiTableEntry), (void **)&(new_si_entry)) )
 {
 *si_entry_handle = 0 ;

 MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI, "<mpe_siGetServiceEntryFromFrequencyProgramNumberModulation> Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");

 return MPE_SI_OUT_OF_MEM;
 }

 //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siGetServiceEntryFromFrequencyProgramNumberModulation> creating new SI Entry...freq:%d, prog_num:%d\n", freq, program_number);

 // Initialize all the fields (some fields are set to default values as defined in spec)
 init_si_entry(new_si_entry);
 new_si_entry->program_number = program_number;
 new_si_entry->frequency = freq;
 new_si_entry->source_id = source_id;
 new_si_entry->modulation_mode = mode;

 if(mode == MPE_SI_MODULATION_QAM_NTSC)
 {
 // We know that an analog service does, and always will, have no components
 new_si_entry->number_elem_streams = 0;
 new_si_entry->pmt_status = PMT_AVAILABLE;

 // For analog the program number is set to -1 (fix for 5724)
 //new_si_entry->program_number = (uint32_t) -1;
 }


 if( (new_si_entry->ts_handle = get_transport_stream_handle(freq, mode)) == 0)
 {
 new_si_entry->ts_handle = create_transport_stream_handle(freq, mode);
 }

 MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siGetServiceEntryFromFrequencyProgramNumberModulation> ts_handle:0x%x\n", new_si_entry->ts_handle);
 if(freq == MPE_SI_OOB_FREQUENCY)
 {
 new_si_entry->channel_type = CHANNEL_TYPE_NORMAL;
 getDSGAppID(&appID);
 if (appID>0)
 {
 new_si_entry->isAppType = true;
 new_si_entry->source_id = appID;
 }

 MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "<mpe_siGetServiceEntryFromFrequencyProgramNumberModulation> got OOB_FREQ si_entry:0x%x ts_handle:0x%x , appID=%ld\n",new_si_entry, new_si_entry->ts_handle, new_si_entry->source_id);

 }

 {
 mpe_SiTransportStreamEntry *ts_entry = (mpe_SiTransportStreamEntry *)new_si_entry->ts_handle;
 (ts_entry->service_count)++;
 // Increment the visible service count also
 //(channel type is not known for dynamic services, they are
 //not signaled in OOB VCT)
 (ts_entry->visible_service_count)++;
 //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siGetServiceEntryFromFrequencyProgramNumberModulation> ts_entry->visible_service_count...%d\n", ts_entry->visible_service_count);
 }

 *si_entry_handle = (mpe_SiServiceHandle)new_si_entry;

 // append new entry to the dynamic list
 if(g_si_entry == NULL)
 {
 g_si_entry = new_si_entry;
 }
 else
 {
 walker = g_si_entry;
 while(walker)
 {
 if(walker->next == NULL)
 {
 walker->next = new_si_entry;
 break;
 }
 walker = walker->next;
 }
 }

 // Increment the number of services
 g_numberOfServices++;
 }

 return MPE_SI_SUCCESS;
 }
 */

/* NTT_SNS */
/** OCAP spec
 When the LVCT or the CVCT is provided the short_name is returned. When the LVCT is not provided but the
 Source Name Sub-Table is provided a component of the source_name is returned from the MTS string. When
 none of these Tables or sub-Tables are provided, NULL is returned.
 */
mpe_Error mpe_siSetSourceName(mpe_siSourceNameEntry *source_name_entry,
        char *sourceName, char *language, mpe_Bool override_existing)
{
    mpe_SiLangSpecificStringList *entry = NULL;
    char *source_name_language = NULL;

    // Parameter check
    if (sourceName == NULL)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    // If the language is NULL, this source name must have come from the LVCT/CVCT which
    // does not specify the channel name in a language specific way like the NTT_SNS
    if (language == NULL)
        source_name_language = "";
    else
        source_name_language = language;

    entry = source_name_entry->source_names;

    // Search for an existing entry for the given language
    while (entry != NULL)
    {
        // If a previous entry is found for this language
        // (and we are allowed to override the existing entry),
        // free the old string and allocate space for the new string
        if ((strcmp(entry->language, source_name_language) == 0)
                && override_existing)
        {
            // If for this language the sourceName already exists
            if(strcmp(entry->string, sourceName) == 0)
            {
                // there is nothing todo!
                return MPE_SI_SUCCESS;
            }
            // Otherwise replace the name string
            mpeos_memFreeP(MPE_MEM_SI, entry->string);

            if (MPE_SI_SUCCESS != mpeos_memAllocP(MPE_MEM_SI,
                    strlen(sourceName) + 1, (void **) &(entry->string)))
            {
                MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                        "<mpe_siSetSourceName> Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");
                return MPE_SI_OUT_OF_MEM;
            }
            strcpy(entry->string, sourceName);
            MPE_LOG(
                    MPE_LOG_TRACE6,
                    MPE_MOD_SI,
                    "<mpe_siSetSourceName> Replaced previous entry! -- source_name:%s, language:%s\n",
                    entry->string, entry->language);
            return MPE_SI_SUCCESS;
        }
        entry = entry->next;
    }

    // No previous entry found, so allocate a new list entry
    if (MPE_SI_SUCCESS != mpeos_memAllocP(MPE_MEM_SI,
            sizeof(struct mpe_SiLangSpecificStringList), (void **) &entry))
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                "<mpe_siSetSourceName> Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");
        return MPE_SI_OUT_OF_MEM;
    }

    // Set the source_name
    if (MPE_SI_SUCCESS != mpeos_memAllocP(MPE_MEM_SI, strlen(sourceName) + 1,
            (void **) &(entry->string)))
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                "<mpe_siSetSourceName> Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");
        return MPE_SI_OUT_OF_MEM;
    }
    strcpy(entry->string, sourceName);
    strncpy(entry->language, source_name_language, 4);

    // Add new list entry to the front of list for this service
    entry->next = source_name_entry->source_names;
    source_name_entry->source_names = entry;

    MPE_LOG(
            MPE_LOG_TRACE6,
            MPE_MOD_SI,
            "<mpe_siSetSourceName> Created new entry! -- source_name:%s, language:%s\n",
            entry->string, entry->language);

    return MPE_SI_SUCCESS;
}

mpe_Error mpe_siSetSourceLongName(mpe_siSourceNameEntry *source_name_entry,
        char *sourceLongName, char *language)
{
    mpe_SiLangSpecificStringList *entry = NULL;

    // Parameter check
    if ((sourceLongName == NULL)
            || (language == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    entry = source_name_entry->source_long_names;

    // Search for an existing entry for the given language
    while (entry != NULL)
    {
        // If a previous entry is found, free the old string and allocate new space for the
        // given string
        if ((strcmp(entry->language, language) == 0))
        {
            // If for this language the sourceLongName already exists
            if(strcmp(entry->string, sourceLongName) == 0)
            {
                // There is nothing todo!
                return MPE_SI_SUCCESS;
            }
            // Otherwise replace the name string
            mpeos_memFreeP(MPE_MEM_SI, entry->string);

            if (MPE_SI_SUCCESS != mpeos_memAllocP(MPE_MEM_SI, strlen(
                    sourceLongName) + 1, (void **) &(entry->string)))
            {
                MPE_LOG(
                        MPE_LOG_WARN,
                        MPE_MOD_SI,
                        "<mpe_siSetSourceLongName> Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");
                return MPE_SI_OUT_OF_MEM;
            }
            strcpy(entry->string, sourceLongName);

            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_SI,
                    "<mpe_siSetSourceLongName> Replaced previous entry! -- source_name:%s, language:%s\n",
                    entry->string, entry->language);
            // This means the source long name for this entry has changed.
            // Signal MPE_SI_EVENT_SERVICE_DETAILS_UPDATE (MODIFY) event..
            // (Global SI state is already 'FULLY_ACQUIRED')
            if(g_si_state == SI_FULLY_ACQUIRED)
            {
                mpe_SiServiceHandle handle = MPE_SI_INVALID_HANDLE;
                // Get the service handle associated with this source name entry
                mpe_SiTableEntry *walker = NULL;
                if(source_name_entry->appType)
                {
                    mpe_siGetServiceHandleByAppId(source_name_entry->id, &handle);
                }
                else
                {
                    mpe_siGetServiceHandleBySourceId(source_name_entry->id, &handle);
                }

                if(handle != MPE_SI_INVALID_HANDLE)
                {
                    walker = (mpe_SiTableEntry *)handle;
                    if((walker->state == SIENTRY_DEFINED_MAPPED || walker->state == SIENTRY_MAPPED))
                    {
                        MPE_LOG(
                                MPE_LOG_INFO,
                                MPE_MOD_SI,
                                "<mpe_siSetSourceLongName> service name updated..: signaling  MPE_SI_EVENT_SERVICE_DETAILS_UPDATE for handle:0x%x sourceId:0x%x\n", walker, walker->source_id);
                        send_si_event(MPE_SI_EVENT_SERVICE_DETAILS_UPDATE, (mpe_SiServiceHandle)walker, MPE_SI_CHANGE_TYPE_MODIFY);
                    }
                }
            }
            return MPE_SI_SUCCESS;
        }
        entry = entry->next;
    }

    // No previous entry found, so allocate a new list entry
    if (MPE_SI_SUCCESS != mpeos_memAllocP(MPE_MEM_SI,
            sizeof(struct mpe_SiLangSpecificStringList), (void **) &entry))
    {
        MPE_LOG(
                MPE_LOG_WARN,
                MPE_MOD_SI,
                "<mpe_siSetSourceLongName> Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");
        return MPE_SI_OUT_OF_MEM;
    }

    // Set the source_name
    if (MPE_SI_SUCCESS != mpeos_memAllocP(MPE_MEM_SI, strlen(sourceLongName)
            + 1, (void **) &(entry->string)))
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                "<mpe_siSetSourceLongName> Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");
        return MPE_SI_OUT_OF_MEM;
    }
    strcpy(entry->string, sourceLongName);
    strncpy(entry->language, language, 4);

    // Add new list entry to the front of list for this service
    entry->next = source_name_entry->source_long_names;
    source_name_entry->source_long_names = entry;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siSetSourceLongName> Created new entry! -- source_name:%s, language:%s\n",
            entry->string, entry->language);

    return MPE_SI_SUCCESS;
}

/**
 *  MIS
 *
 *  Set the application type for the given service entry
 *
 * <i>mpe_siSetAppType()</i>
 *
 * @param service_handle is the service handle to set the program_number for
 * @param bIsApp is the boolean value to set
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
void mpe_siSetAppType(mpe_SiServiceHandle service_handle, mpe_Bool bIsApp)
{
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;

    /* Parameter check */
    if (service_handle == MPE_SI_INVALID_HANDLE)
    {
        return;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siSetAppType> bIsApp: %d...\n",
            bIsApp);

    /* Set the app type */
    si_entry->isAppType = bIsApp;
    if (si_entry->isAppType == TRUE)
    {
        // This means this is a application_virtual_channel
        // so the SourceId is not really sourceId but application_id
        // Do not set AppID here. It is set by the mpe_siSetAppId() call
        si_entry->source_id = SOURCEID_UNKNOWN;
    }
}

/**
 *  SVCT
 *
 *  Set the sourceId for the given service entry
 *
 * <i>mpe_siSetSourceId()</i>
 *
 * @param service_handle is the service handle to set the program_number for
 * @param sourceId is the sourceId to set
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siSetSourceId(mpe_SiServiceHandle service_handle,
        uint32_t sourceId)
{
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;

    /* Parameter check */
    if (service_handle == MPE_SI_INVALID_HANDLE)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    // This means the SI is initially acquired and now being updated
    if(g_si_state == SI_FULLY_ACQUIRED)
    {
        // This entry sourceId is changing
        if((si_entry->source_id != SOURCEID_UNKNOWN) && (si_entry->source_id != sourceId)&&
                        (si_entry->state == SIENTRY_DEFINED_MAPPED || si_entry->state == SIENTRY_MAPPED))
        {
            MPE_LOG(
                    MPE_LOG_INFO,
                    MPE_MOD_SI,
                    "<mpe_siSetSourceId> service entry sourceId changed..: signaling  MPE_SI_EVENT_SERVICE_DETAILS_UPDATE (MODIFY) for handle:0x%x old sourceId:0x%x new sourceId:0x%x\n", si_entry, si_entry->source_id, sourceId);
            send_si_event(MPE_SI_EVENT_SERVICE_DETAILS_UPDATE, (mpe_SiServiceHandle)si_entry, MPE_SI_CHANGE_TYPE_MODIFY);
        }
    }

    /* Set the sourceId */
    si_entry->source_id = sourceId;

    return MPE_SI_SUCCESS;
}

/**
 *  SVCT
 *
 *  Set the appId for the given service entry
 *
 * <i>mpe_siSetAppId()</i>
 *
 * @param service_handle is the service handle to set the program_number for
 * @param appId is the appId to set
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
void mpe_siSetAppId(mpe_SiServiceHandle service_handle, uint32_t appId)
{
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siSetAppId> appId: 0x%x...\n",
            appId);

    /* Set the sourceId */
    si_entry->app_id = appId;
}
/**
 *  SVCT
 *
 *  Set the frequency, program number and modulation format for the given service entry
 *
 * <i>mpe_siUpdateFreqProgNumByServiceHandle()</i>
 *
 * @param service_handle is the service handle to set
 * @param freq is the  frequency to set
 * @param prog is the  program number to set
 * @param mod is the  modulation format to set
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siUpdateFreqProgNumByServiceHandle(
        mpe_SiServiceHandle service_handle, uint32_t freq, uint32_t prog_num,
        uint32_t mod)
{
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;
    mpe_SiTransportStreamEntry * volatile ts;
    mpe_SiProgramInfo * volatile pi;
    mpe_Bool freqMove = false;
    LINK *lp;

    /* Parameter check */
    if (service_handle == MPE_SI_INVALID_HANDLE)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siUpdateFreqProgNumByServiceHandle> service_handle: 0x%x freq: %d, prg_num: %d, mod: %d \n",
            service_handle, freq, prog_num, mod);

    if ((ts = (mpe_SiTransportStreamEntry *) si_entry->ts_handle) != NULL)
    {
        if (freq != ts->frequency)
            freqMove = true;
    }

    if ((pi = si_entry->program) != NULL)
    {
        if ((pi->program_number != prog_num) || freqMove)
        {
            // Changed program # and/or Frequencies, so delete ourselves from the current programs service list.
            // A program should *always* have a stream!!
            if (pi->stream != NULL)
            {
                mpe_SiTransportStreamEntry *stream = pi->stream;
                mpe_mutexAcquire(stream->list_lock);
                lp = llist_linkof(pi->services, (void *) si_entry);
                if (lp != NULL)
                {
                    llist_rmlink(lp);
                    //list_RemoveData(pi->services, si_entry);
                    pi->service_count--;
                }
                si_entry->program = NULL;
                if (llist_cnt(pi->services) == 0)
                {
                    LINK *lp1 = llist_linkof(stream->programs, (void *) pi);
                    if (lp1 != NULL)
                    {
                        llist_rmlink(lp1);
                        //list_RemoveData(stream->programs, pi);
                        stream->program_count--;
                    }
                    release_program_info_entry(pi);
                }
                mpe_mutexRelease(stream->list_lock);
            }
            else
            {
                MPE_LOG(
                        MPE_LOG_FATAL,
                        MPE_MOD_SI,
                        "<mpe_siUpdateFreqProgNumByServiceHandle> ERROR!! program without a transport stream!! si_entry/program_info (%p, %p)\n",
                        si_entry, pi);
            }
        }
    }

    if (freqMove && ts)
    {
        LINK *lp1;
        mpe_Bool deleteTS = false;
        // Yup, it's a move.
        mpe_mutexAcquire(ts->list_lock);
        lp1 = llist_linkof(ts->services, (void *) si_entry);
        if (lp1 != NULL)
        {
            llist_rmlink(lp1);
            (ts->service_count)--;
            (ts->visible_service_count)--;
        }
        si_entry->ts_handle = MPE_SI_INVALID_HANDLE;
        if ((llist_cnt(ts->services) == 0) && (llist_cnt(ts->programs) == 0))
            deleteTS = true;
        mpe_mutexRelease(ts->list_lock);
        if (deleteTS)
            release_transport_stream_entry(ts);
        ts = NULL;
    }
    if (ts == NULL)
    {
        LINK *lp1;
        ts = (mpe_SiTransportStreamEntry *) (si_entry->ts_handle
                = create_transport_stream_handle(freq, mod));
        if (ts == NULL)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI,
                    "<mpe_siUpdateFreqProgNumByServiceHandle> ERROR!! out of memory! (ts)\n");
            return MPE_SI_OUT_OF_MEM;
        }
        mpe_mutexAcquire(ts->list_lock);
        lp1 = llist_mklink((void *) si_entry);
        llist_append(ts->services, lp1);
        ts->service_count++;
        mpe_mutexRelease(ts->list_lock);
    }

    if ((si_entry->program == NULL) && (ts->modulation_mode
            != MPE_SI_MODULATION_QAM_NTSC))
    {
        mpe_mutexAcquire(ts->list_lock);
        lp = llist_first(ts->programs);
        while (lp)
        {
            pi = (mpe_SiProgramInfo *) llist_getdata(lp);
            if ((pi != NULL) && (pi->program_number == prog_num))
            {
                break;
            }
            lp = llist_after(lp);
        }
        if (pi == NULL)
        {
            pi = create_new_program_info(ts, prog_num);
            if (pi == NULL)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI,
                        "<mpe_siUpdateFreqProgNumByServiceHandle> ERROR!! out of memory! (ts)\n");
                mpe_mutexRelease(ts->list_lock);
                return MPE_SI_OUT_OF_MEM;
            }
        }
        si_entry->program = pi;

        {
            LINK *lp1 = llist_mklink((void *) pi);
            LINK *lp2 = llist_mklink((void *) si_entry);
            llist_append(ts->programs, lp1);
            ts->program_count++;
            llist_append(pi->services, lp2);
            pi->service_count++;
        }

        mpe_mutexRelease(ts->list_lock);
    }

    return MPE_SI_SUCCESS;
}

/**
 *  SVCT
 *
 *  Set the Program_number for the given service entry
 *
 * <i>mpe_siSetProgramNumber()</i>
 *
 * @param service_handle is the service handle to set the program_number for
 * @param prog_num is the program_number to set
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siSetProgramNumber(mpe_SiServiceHandle service_handle,
        uint32_t prog_num)
{
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;

    /* Parameter check */
    if (service_handle == MPE_SI_INVALID_HANDLE)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    /* check for invalid Progam number (max 16 bits) */
    if (prog_num > 0xffff)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    /* Set the program number */
    si_entry->program_number = prog_num;

    return MPE_SI_SUCCESS;
}

/**
 *  SVCT
 *
 *  Set the activation_time for the given service entry
 *
 * <i>mpe_siSetActivationTime()</i>
 *
 * @param service_handle is the service handle to set the activation_time for
 * @param activation_time is the activation_time to set
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
/*
 Notes from SCTE 65-2002:
 Activation_time is a 32 bit unsigned integer field providing the absolute second
 the virtual channel data carried in the table section will be valid. If activation_time
 is in the past, the data in the table section shall be considered valid immediately.
 An activation_time value of zero shall be used to indicate immediate activation.
 A Host may enter a virtual channel recors whose activation times are in the future
 into a queue. Hosts are not required to implement a pending virtual channel queue,
 and may choose to discard any data that is not currently applicable.

 In the current implementation, the SI table parser layer will check if the activation_time
 parsed from SVCT is in the future and if so will start a timer until the activation_time
 becomes current time and it will then start populating from SVCT.
 */
void mpe_siSetActivationTime(mpe_SiServiceHandle service_handle,
        uint32_t activation_time)
{
    mpe_SiTableEntry * si_entry = (mpe_SiTableEntry *) service_handle;

    /* Parameter check */
    if (service_handle == MPE_SI_INVALID_HANDLE)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI,
                "<mpe_siSetActivationTime> MPE_SI_INVALID_PARAMETER\n");
    }
    else
    {
        /* no way to check validity of activation_time. So just accept it */
        /* Set the Activation Time */
        si_entry->activation_time = activation_time;
    }
}

/**
 *  SVCT
 *
 *  Set the virtual channel number for the given service entry
 *
 * <i>mpe_siSetChannelNumber()</i>
 *
 * @param service_handle is the service handle to set the channel number for
 * @param chan_num is the virtual channel number
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siSetChannelNumber(mpe_SiServiceHandle service_handle,
        uint32_t major_channel_number, uint32_t minor_channel_number)
{
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;

    if (service_handle == MPE_SI_INVALID_HANDLE)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siSetChannelNumber> Enter - service_handle: 0x%x, major_channel_number: %d, minor_channel_number: %d\n",
            service_handle, major_channel_number, minor_channel_number);

    /*
     * If this is a one part channel number, then check the value and
     * set in in the minor field.
     */
    if (minor_channel_number == MPE_SI_DEFAULT_CHANNEL_NUMBER)
    {
        if (major_channel_number > 4095)
        {
            return MPE_SI_INVALID_PARAMETER;
        }
        si_entry->major_channel_number = major_channel_number;
        si_entry->minor_channel_number = MPE_SI_DEFAULT_CHANNEL_NUMBER;
        return MPE_SI_SUCCESS;
    }

    /*
     * This is a twor part channel number, so check each param and set the values.
     *
     * Major channel number must be between 1 and 99
     */
    if (major_channel_number > 99 || major_channel_number < 1)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    /* Minor channel number must be between 0 and 999 */
    if (minor_channel_number > 999)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    /* Set the channel numbers */
    si_entry->major_channel_number = major_channel_number;
    si_entry->minor_channel_number = minor_channel_number;

    return MPE_SI_SUCCESS;
}

/**
 *  SVCT
 *
 *  Set the frequency index for the given service entry
 *
 * <i>mpe_siSetCDSRef()</i>
 *
 * @param service_handle is the service handle to set the frequency for
 * @param cd_ref is the index into frequency table to retrieve the frequency
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
void mpe_siSetCDSRef(mpe_SiServiceHandle service_handle, uint8_t cd_ref)
{
    mpe_SiTableEntry * si_entry = (mpe_SiTableEntry *) service_handle;

    /* Parameter check */
    if (service_handle == MPE_SI_INVALID_HANDLE)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI,
                "<mpe_siSetCDSRef> MPE_SI_INVALID_PARAMETER\n");
    }
    else
    {
        /* no way to check validity of CDS_reference. So just accept it */
        //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siSetCDSRef> %d\n", cd_ref);
        /* Set the CDS_reference index */
        si_entry->freq_index = cd_ref;
    }
}

/**
 *  SVCT
 *
 *  Set the modulation mode for the given service entry
 *
 * <i>mpe_siSetMMSRef()</i>
 *
 * @param service_handle is the service handle to set the mode for
 * @param mm_ref is the index into modulation_mode table to retrieve the mode
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
void mpe_siSetMMSRef(mpe_SiServiceHandle service_handle, uint8_t mm_ref)
{
    mpe_SiTableEntry * si_entry = (mpe_SiTableEntry *) service_handle;

    /* Parameter check */
    if (service_handle == MPE_SI_INVALID_HANDLE)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI,
                "<mpe_siSetMMSRef> MPE_SI_INVALID_PARAMETER\n");
    }
    else
    {
        /* no way to check validity of MM_reference. So just accept it */
        /* Set the MM_reference index */
        si_entry->mode_index = mm_ref;
    }
}

/**
 *  SVCT
 *
 * Set the channel_type for the given service entry
 *
 * <i>mpe_siSetChannelType()</i>
 *
 * @param service_handle is the service handle to set the mode for
 * @param channel_type is the value of the channel_type field of the
 *        SVCT VCM record.
 *
 * @return MPE_SI_SUCCESS if successful, else
 *            return appropriate mpe_Error
 *
 */
void mpe_siSetChannelType(mpe_SiServiceHandle service_handle,
        uint8_t channel_type)
{
    mpe_SiTableEntry * si_entry = (mpe_SiTableEntry *) service_handle;

    /* Parameter check */
    if (service_handle == MPE_SI_INVALID_HANDLE)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI,
                "<mpe_siSetChannelType> MPE_SI_INVALID_PARAMETER\n");
    }
    else
    {
        /* Set the MM_reference index */
        si_entry->channel_type = channel_type;
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mpe_siSetChannelType> channel_type: %d...\n", channel_type);
    }
}

/**
 *  SVCT
 *
 *  Set the video standard for the given analog service entry
 *
 * <i>mpe_siSetVideoStandard()</i>
 *
 * @param service_handle is the service handle to set the video standard for
 * @param video_std is the video standard (NTSC, PAL etc.)
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
void mpe_siSetVideoStandard(mpe_SiServiceHandle service_handle,
        mpe_SiVideoStandard video_std)
{
    mpe_SiTableEntry * si_entry = (mpe_SiTableEntry *) service_handle;

    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE))
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI,
                "<mpe_siSetVideoStandard> MPE_SI_INVALID_PARAMETER\n");
    }
    else
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mpe_siSetVideoStandard> video_std: %d\n", video_std);
        si_entry->video_standard = video_std;
        // We know that an analog service does, and always will, have no components
        //si_entry->number_elem_streams = 0;
        //si_entry->pmt_status = PMT_AVAILABLE;
    }
}

/**
 *  SVCT
 *
 *  Set the service type service entry
 *
 * <i>mpe_siSetServiceType()</i>
 *
 * @param service_handle is the service handle to set the service type
 * @param service_type is the service type
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
void mpe_siSetServiceType(mpe_SiServiceHandle service_handle,
        mpe_SiServiceType service_type)
{
    mpe_SiTableEntry * si_entry = (mpe_SiTableEntry *) service_handle;

    /* Parameter check */
    if (service_handle == MPE_SI_INVALID_HANDLE)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI,
                "<mpe_siSetServiceType> MPE_SI_INVALID_PARAMETER\n");
    }
    else
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mpe_siSetServiceType> service_type: %d\n", service_type);
        si_entry->service_type = service_type;
    }
}

/**
 *  SVCT
 *
 *  Set the modulation mode for the given analog service entry
 *
 * <i>mpe_siSetAnalogMode()</i>
 *
 * @param service_handle is the service handle to set the mode for
 * @param mode is the analog modulation mode
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
void mpe_siSetAnalogMode(mpe_SiServiceHandle service_handle,
        mpe_SiModulationMode mode)
{
    mpe_SiTableEntry * si_entry = (mpe_SiTableEntry *) service_handle;

    /* Parameter check */
    if (service_handle == MPE_SI_INVALID_HANDLE)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI,
                "<mpe_siSetAnalogMode> MPE_SI_INVALID_PARAMETER\n");
    }
    else
    {
        /* Set the analog modulation mode (always 255) */
        ((mpe_SiTransportStreamEntry *) (si_entry->ts_handle))->modulation_mode
                = mode;
    }
}

/**
 *  SVCT
 *
 *  Set the descriptor in SVCT
 *
 * <i>mpe_siSetSVCTDescriptor()</i>
 *
 * @param service_handle is the service handle to set the descriptor for
 * @param tag is the descriptor tag field
 * @param length is the length of the descriptor
 * @param content is descriptor content
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siSetSVCTDescriptor(mpe_SiServiceHandle service_handle,
        mpe_SiDescriptorTag tag, uint32_t length, void *content)
{
    mpe_SiTableEntry *si_entry = NULL;
    uint8_t *arr = NULL;

    /* Parameter check */
    if (service_handle == MPE_SI_INVALID_HANDLE)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    /* Set the Descriptor Info */
    si_entry = (mpe_SiTableEntry *) service_handle;

    /*  For now we only care about channel properties descriptor (to retrieve service type)
     and two part channel number descritor for major, minor channel numbers.
     These two are optional descriptors in SVCT, hence may not be there.
     */

    /* No need to store SVCT descriptors in raw format. */

    /* Populate service type from channel_properties_descriptor */
    if (tag == MPE_SI_DESC_CHANNEL_PROPERTIES)
    {
        /* TODO - channel properties descriptor also contains transport stream Id */
        /* First 2 bytes of desc content - channel_TSID */
        //data_ptr = (uint16_t*)content;
        //si_entry->tsId = ntohs(*data_ptr);

        arr = (uint8_t*) content;
        si_entry->service_type = (mpe_SiServiceType) arr[length - 1];
    }
    /* Populate major/minor numbers from two_part_channel_number_descriptor */
    else if (tag == MPE_SI_DESC_TWO_PART_CHANNEL_NUMBER)
    {
        arr = (uint8_t*) content;
        si_entry->major_channel_number = (arr[0] & 0x03) << 8 | (arr[1] & 0xFF);
        si_entry->minor_channel_number = (arr[2] & 0x03) << 8 | (arr[3] & 0xFF);
    }

    return MPE_SI_SUCCESS;
}

mpe_Error mpe_siGetSourceNameEntry(uint32_t id, mpe_Bool isAppId,  mpe_siSourceNameEntry **source_name_entry, mpe_Bool create)
{
    mpe_siSourceNameEntry *sn_walker;
    mpe_Error retVal = MPE_SI_SUCCESS;

    //MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI, "<mpe_siGetSourceNameEntry> id:0x%x (%s)\n",
    //        id, isAppId ? "AppID" : "SourceID");

    if ((source_name_entry == NULL) || ((id == 0) || (id > 65535)))
    {
        retVal = MPE_SI_INVALID_PARAMETER;
    }
    else
    {
        // FOR NOW Assume write lock already obtained!!!!

        *source_name_entry = NULL;
        sn_walker = g_si_sourcename_entry;
        while (sn_walker != NULL)
        {
            if (sn_walker->id == id && (sn_walker->appType == isAppId))
            {
                *source_name_entry = sn_walker;
                break;
            }
            else
            {
              sn_walker = sn_walker->next;
            }
        }

        //
        // If walker is NULL, entry not found
        if (sn_walker==NULL && create)
        {
            mpeos_memAllocP(MPE_MEM_SI, sizeof(mpe_siSourceNameEntry), (void **) &(sn_walker));
            if (sn_walker == NULL)
            {
                MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                        "<mpe_siGetSourceNameEntry> Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");
                retVal = MPE_SI_OUT_OF_MEM;
            }
            else
            {
                sn_walker->appType = isAppId;
                sn_walker->mapped = false;
                sn_walker->id = id;
                sn_walker->source_long_names = NULL;
                sn_walker->source_names = NULL;

                // just put it at the front.
                sn_walker->next = g_si_sourcename_entry;
                g_si_sourcename_entry = sn_walker;
                *source_name_entry = sn_walker;
            }
        }
    }

    //MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,  "<mpe_siGetSourceNameEntry> id:0x%x (%s), *sn_entry=%p\n",
    //        id, isAppId ? "AppID" : "SourceID", *source_name_entry);
    return retVal;
}


mpe_Bool mpe_siIsSourceNameLanguagePresent(mpe_siSourceNameEntry *source_name_entry, char *language)
{
  mpe_SiLangSpecificStringList *ln_walker = NULL;

    //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siGetSourceNameEntry> id:0x%x (%s)\n",
    //        id, isAppId ? "AppID" : "SourceID");

    if ((source_name_entry != NULL) && (language != NULL))
    {
        ln_walker = source_name_entry->source_names;
        while (ln_walker != NULL)
        {
          if (!strcmp(language, ln_walker->language) )
          {
            return(TRUE);
          }
          ln_walker = ln_walker->next;
        }
    }

    return FALSE;
}

mpe_Error mpe_siSetTransportType(mpe_SiServiceHandle service_handle, uint8_t transportType)
{
    mpe_SiTableEntry * si_entry = (mpe_SiTableEntry *) service_handle;

    /* Parameter check */
    if (service_handle == MPE_SI_INVALID_HANDLE)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI,
                "<mpe_siSetTransportType> MPE_SI_INVALID_PARAMETER\n");
    }
    else
    {
        si_entry->transport_type = transportType;
    }
    return MPE_SI_SUCCESS;
}

mpe_Error mpe_siSetScrambled(mpe_SiServiceHandle service_handle, mpe_Bool scrambled)
{
    mpe_SiTableEntry * si_entry = (mpe_SiTableEntry *) service_handle;

    /* Parameter check */
    if (service_handle == MPE_SI_INVALID_HANDLE)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI,
                "<mpe_siSetScrambled> MPE_SI_INVALID_PARAMETER\n");
    }
    else
    {
        si_entry->scrambled = scrambled;
    }
    return MPE_SI_SUCCESS;
}

/**
 *  PAT
 *
 *  Set the ts Id for the given service entry
 *
 * <i>mpe_siSetTSId()</i>
 *
 * @param service_handle is the service handle to set the tsId for
 * @param tsId is the transport stream Id to set
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siSetTSId(mpe_SiServiceHandle service_handle, uint32_t tsId)
{
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;
    mpe_SiTransportStreamEntry *walker = NULL;

    /* Parameter check */
    /* check the validity of tsId (max 16 bits) */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (tsId > 0xffff))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siSetTSId> tsId: %d\n", tsId);

    /* Set Transport stream handle into the SI entry */

    /* Acquire global list mutex */
    mpe_mutexAcquire(g_global_si_list_mutex);

    walker = g_si_ts_entry;

    while (walker)
    {
        if (walker->ts_id == tsId)
        {
            si_entry->ts_handle = (mpe_SiTransportStreamHandle) walker;
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                    "<mpe_siSetTSId>  tsId: %d, ts_handle:0x%x \n", tsId,
                    si_entry->ts_handle);
            break;
        }
        walker = walker->next;
    }

    mpe_mutexRelease(g_global_si_list_mutex);

    return MPE_SI_SUCCESS;
}

/**
 *  PAT
 *
 *  Set the version of PAT for the given service entry
 *
 * <i>mpe_siSetPATVersion()</i>
 *
 * @param ts_handle is the transport stream handle to set the version for
 * @param pat_version is the version of the program association table
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
void mpe_siSetPATVersion(mpe_SiTransportStreamHandle ts_handle,
        uint32_t pat_version)
{
    mpe_SiTransportStreamEntry *ts_entry =
            (mpe_SiTransportStreamEntry *) ts_handle;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siSetPATVersion> ts_handle:0x%x patVersion:%d\n", ts_handle,
            pat_version);

    /* Set the PATVersion */
    ts_entry->pat_version = pat_version;
}

/**
 *  PAT
 *
 *  Set the PAT for the given service entry
 *
 * <i>mpe_siSetPAT()</i>
 *
 * @param service_handle is the service handle to set the PAT for
 * @param length is the length of PAT arry
 * @param pat_array is the PAT byte array
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siSetPAT(mpe_SiServiceHandle service_handle, uint32_t length,
        uint32_t* pat_programs_array)
{
    MPE_UNUSED_PARAM(service_handle);
    MPE_UNUSED_PARAM(length);
    MPE_UNUSED_PARAM(pat_programs_array);
    return MPE_SI_SUCCESS;
}

/**
 *  PAT
 *
 *  Set the PMT Pid for the given program entry
 *
 * <i>mpe_siSetPMTPid()</i>
 *
 * @param program_handle is the program handle to set the PMT Pid for
 * @param pmt_pid is the program map table Pid to set
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
void mpe_siSetPMTPid(mpe_SiProgramHandle program_handle, uint32_t pmt_pid)
{
    mpe_SiProgramInfo *pi = (mpe_SiProgramInfo *) program_handle;
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siSetPMTPid> pmt_pid: %d\n",
            pmt_pid);
    pi->pmt_pid = pmt_pid;
}

/**
 *  PMT
 *
 *  Set the PCR Pid for the given program entry
 *
 * <i>mpe_siSetPCRPid()</i>
 *
 * @param program_handle is the program handle to set the PCR Pid for
 * @param pcr_pid is the program clock reference Pid to set
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
void mpe_siSetPCRPid(mpe_SiProgramHandle program_handle, uint32_t pcr_pid)
{
    mpe_SiProgramInfo *pi = (mpe_SiProgramInfo *) program_handle;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siSetPCRPid> pcr_pid: %d\n",
            pcr_pid);
    /* Set the PCRPid */
    pi->pcr_pid = pcr_pid;
}

/**
 *  PMT
 *
 *  Set the PMT version for the given program entry
 *
 * <i>mpe_siSetPMTVersion()</i>
 *
 * @param program_handle is the program handle to set the PMT version for
 * @param pmt_version is the program map table version to set
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siSetPMTVersion(mpe_SiProgramHandle program_handle,
        uint32_t pmt_version)
{
    mpe_SiProgramInfo *pi = (mpe_SiProgramInfo *) program_handle;

    /* Parameter check */
    /* check the validity of PMT version(max 5 bits) */
    if ((program_handle == MPE_SI_INVALID_HANDLE) || (pmt_version > 0x1f))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siSetPMTVersion>  siEntry version: %d, version: %d\n",
            pi->pmt_version, pmt_version);

    /* There is a older copy of the PMT */

    if (pi->pmt_version != pmt_version)
    {
        mpe_SiElementaryStreamList *elem_stream_list = pi->elementary_streams;
        mpe_SiElementaryStreamList *saved_next = NULL;

        // We have an existing PMT, but received a new version
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siSetPMTVersion>\n");

        // delete outer descriptor loop
        if (pi->outer_descriptors != NULL)
        {
            release_descriptors(pi->outer_descriptors);
            pi->outer_descriptors = NULL;

            pi->number_outer_desc = 0;
        }

        // No need to do crc check
        pi->crc_check = FALSE;

        while (elem_stream_list)
        {
            saved_next = elem_stream_list->next;

            if (elem_stream_list->ref_count == 1)
            {
                remove_elementaryStream(&(pi->elementary_streams), elem_stream_list);
                delete_elementary_stream(elem_stream_list);
                (pi->number_elem_streams)--;
            }
            else if (elem_stream_list->ref_count > 1)
            {
                mark_elementary_stream(elem_stream_list);

                // We are getting new PMT, send events for service existing component handles
                // For handles delivered to Java increment the ref count
                elem_stream_list->ref_count++;
                MPE_LOG(
                        MPE_LOG_DEBUG,
                        MPE_MOD_SI,
                        "<mpe_siSetPMTVersion> Send service comp event - incremented ref count... %d\n",
                        elem_stream_list->ref_count);

                send_service_component_event(elem_stream_list,
                        MPE_SI_CHANGE_TYPE_REMOVE);
            }
            elem_stream_list = saved_next;
        }
    }
    else
    {

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mpe_siSetPMTVersion> PMT version is same, check CRC...\n");

        // PMT version seems to be same, do a crc check
        pi->crc_check = TRUE;

        if (MPE_SI_SUCCESS != mpeos_memAllocP(MPE_MEM_SI,
                sizeof(mpe_SiPMTReference), (void **) &(pi->saved_pmt_ref)))
        {
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                    "<mpe_siSetPMTVersion> Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");
            return MPE_SI_OUT_OF_MEM;
        }
        pi->saved_pmt_ref->pcr_pid = pi->pcr_pid;
        pi->saved_pmt_ref->number_outer_desc = pi->number_outer_desc;
        pi->saved_pmt_ref->outer_descriptors_ref
                = (uint32_t) pi->outer_descriptors;
        pi->saved_pmt_ref->number_elem_streams = pi->number_elem_streams;
        pi->saved_pmt_ref->elementary_streams_ref
                = (uint32_t) pi->elementary_streams;
    }

    // Un-couple exisiting outer descriptors and
    // elementary streams from the service entry
    pi->outer_descriptors = NULL;

    pi->number_outer_desc = 0;

    if (pi->elementary_streams != NULL)
    {
        pi->elementary_streams = NULL;
    }
    pi->number_elem_streams = 0;

    // New PMT is being populated under the lock.
    pi->pmt_status = PMT_AVAILABLE_SHORTLY;

    /* Set the PMTVersion */
    pi->pmt_version = pmt_version;

    return MPE_SI_SUCCESS;
}

static void mark_elementary_stream(mpe_SiElementaryStreamList *elem_stream)
{
    if (elem_stream == NULL)
    {
        return;
    }

    elem_stream->valid = FALSE;
}

static void delete_elementary_stream(mpe_SiElementaryStreamList *elem_stream)
{
    /* Parameter check */
    if (elem_stream == NULL)
    {
        return;
    }

    // No more references left, delete the elementary stream
    //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<delete_elementary_stream> deleting elementary steam 0x%x\n", elem_stream);

    strcpy(elem_stream->associated_language, "");
    elem_stream->es_info_length = 0;
    elem_stream->elementary_PID = 0;
    elem_stream->service_comp_type = 0;

    // De-allocate service component name
    if (elem_stream->service_component_names != NULL)
    {
        mpe_SiLangSpecificStringList *walker =
                elem_stream->service_component_names;

        while (walker != NULL)
        {
            mpe_SiLangSpecificStringList *toBeFreed = walker;

            mpeos_memFreeP(MPE_MEM_SI, walker->string);

            walker = walker->next;

            mpeos_memFreeP(MPE_MEM_SI, toBeFreed);
        }

        elem_stream->service_component_names = NULL;
    }

    // De-allocate descriptors
    if (elem_stream->descriptors != NULL)
    {
        release_descriptors(elem_stream->descriptors);
        elem_stream->descriptors = NULL;
    }
    elem_stream->number_desc = 0;

    mpeos_memFreeP(MPE_MEM_SI, elem_stream);
}

static void send_service_component_event(
        mpe_SiElementaryStreamList *elem_stream, uint32_t changeType)
{
    mpe_Event event = MPE_SI_EVENT_SERVICE_COMPONENT_UPDATE;
    mpe_SiRegistrationListEntry *walker = NULL;

    if (elem_stream == NULL)
    {
        return;
    }

    /* Acquire registration list mutex */
    mpe_mutexAcquire(g_registration_list_mutex);

    walker = g_si_registration_list;

    while (walker)
    {
        uint32_t termType = walker->edHandle->terminationType;

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mpe_siNotifyTableAcquired> notify walker : %p\n", walker);
        mpe_eventQueueSend(walker->edHandle->eventQ, event,
                (void*) elem_stream, (void*) walker->edHandle, changeType);

        // Do we need to unregister this client?
        if (termType == MPE_ED_TERMINATION_ONESHOT || (termType
                == MPE_ED_TERMINATION_EVCODE && event
                == walker->terminationEvent))
        {
            add_to_unreg_list(walker->edHandle);
        }

        walker = walker->next;
    }

    /* Unregister all clients that have been added to unregistration list */
    unregister_clients();

    /* Release registration list mutex */
    mpe_mutexRelease(g_registration_list_mutex);
}

/**
 *  PMT
 *
 *  Set the PMT status to available
 *
 * <i>mpe_siSetPMTStatus()</i>
 *
 * @param program_handle is the program handle to set the PMT status for
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
void mpe_siSetPMTStatus(mpe_SiProgramHandle program_handle)
{
    mpe_SiProgramInfo *pi = (mpe_SiProgramInfo *) program_handle;
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siSetPMTStatus> ...program_handle:0x%x\n", program_handle);

    pi->pmt_status = PMT_AVAILABLE;

    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mpe_siSetPMTStatus>  pi->pmt_status: %d\n", pi->pmt_status);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mpe_siSetPMTStatus>  pi->program_number: %d\n",
                pi->program_number);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mpe_siSetPMTStatus>  pi->number_elem_streams: %d\n",
                pi->number_elem_streams);
    }
}

/*  Set PMT stateus to 'Avialable_shortly' for all programs on transport stream
 identified by 'frequency'
 This is performed under a WRITE lock by SITP
 */
void mpe_siSetPMTStatusAvailableShortly(mpe_SiTransportStreamHandle ts_handle)
{
    mpe_SiTransportStreamEntry *ts_entry =
            (mpe_SiTransportStreamEntry *) ts_handle;
    LINK *lp = NULL;
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siSetPMTStatusAvailableShortly> ...\n");

    mpe_mutexAcquire(ts_entry->list_lock);
    if (ts_entry->modulation_mode != MPE_SI_MODULATION_QAM_NTSC)
    {
        lp = llist_first(ts_entry->programs);
        while (lp)
        {
            mpe_SiProgramInfo *pi = (mpe_SiProgramInfo *) llist_getdata(lp);
            if (pi != NULL)
            {
                if ((PMT_NOT_AVAILABLE == pi->pmt_status))
                {
                    pi->pmt_status = PMT_AVAILABLE_SHORTLY;
                    MPE_LOG(
                            MPE_LOG_DEBUG,
                            MPE_MOD_SI,
                            "<mpe_siSetPMTStatusAvailableShortly> PMT status set to available shortly...(%d/%d)\n",
                            ts_entry->frequency, pi->program_number);
                }
            }
            lp = llist_after(lp);
        }
    }
    mpe_mutexRelease(ts_entry->list_lock);
}

/*  Set PAT status of the given service handle's transport stream.
 TODO: change this to accept frequency as opposed to service_handle!
 This is performed under a lock by SITP
 */
void mpe_siSetPATStatusNotAvailable(
        mpe_SiTransportStreamHandle transport_handle)
{
    mpe_SiTransportStreamEntry *ts_entry =
            (mpe_SiTransportStreamEntry *) transport_handle;
    // If the status was previously set to available shortly then reset to not available
    if (ts_entry->siStatus == SI_AVAILABLE_SHORTLY)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mpe_siSetPATStatusNotAvailable> ...\n");
        ts_entry->siStatus = SI_NOT_AVAILABLE;
    }
}

void mpe_siSetPATStatusAvailable(mpe_SiTransportStreamHandle transport_handle)
{
    mpe_SiTransportStreamEntry *ts_entry =
            (mpe_SiTransportStreamEntry *) transport_handle;
    ts_entry->siStatus = SI_AVAILABLE;
}

static mpe_Bool find_program_in_ts_pat_programs(mpe_SiPatProgramList *pat_list,
        uint32_t pn)
{
    mpe_SiPatProgramList *walker = (mpe_SiPatProgramList *) pat_list;

    while (walker)
    {
        if (walker->programNumber == pn)
        {
            return TRUE;
        }
        walker = walker->next;
    }

    return FALSE;
}

/*  Reset PMT status to 'NOT_AVAILABLE' for all programs on transport stream
 not signaled in the newly acquired PAT
 This is performed under a WRITE lock by SITP
 */
void mpe_siResetPATProgramStatus(mpe_SiTransportHandle ts_handle)
{
    mpe_SiTransportStreamEntry *ts_entry =
            (mpe_SiTransportStreamEntry *) ts_handle;
    uint32_t found = 0;
    LINK *lp = NULL;
    LINK *lp1 = NULL;
    mpe_SiProgramInfo *pi = NULL;
    mpe_SiServiceHandle service_handle = MPE_SI_INVALID_HANDLE;
    SIIterator iter;

    mpe_mutexAcquire(ts_entry->list_lock);
    if (ts_entry->modulation_mode != MPE_SI_MODULATION_QAM_NTSC)
    {
        mpe_SiPatProgramList* pat_list =
                (mpe_SiPatProgramList*) ts_entry->pat_program_list;

        lp = llist_first(ts_entry->programs);

        while (lp)
        {
            pi = (mpe_SiProgramInfo *) llist_getdata(lp);
            // reset PMT status if the program number is not found in the recently
            // acquired PAT and if its status has been previously set to PMT_AVAILABLE_SHORTLY
            // and signal a PMT REMOVE for each of those programs no longer contained in the PAT
            found = find_program_in_ts_pat_programs(pat_list,
                    pi->program_number);

            if (!found && (PMT_AVAILABLE_SHORTLY == pi->pmt_status))
            {
                SIIterator *Iter = NULL;
                mpe_SiProgramInfo *programs = NULL;
                pi->pmt_status = PMT_NOT_AVAILABLE;
                pi->pmt_version = INIT_TABLE_VERSION;

                //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<reset_PATProgram_status> PMT status set to not available ...(%d/%d)\n", ts_entry->frequency, pi->program_number);

                // Need to loop through services attached to the program here, and notify each one.
                // This is new, because now more than one service can point to a program.
                //
                Iter = (SIIterator *) (&iter);

                Iter->program = (mpe_SiProgramInfo *) pi;
                programs = (mpe_SiProgramInfo *) pi;
                lp1 = llist_first(programs->services);
                while (lp1)
                {
                    service_handle = (mpe_SiServiceHandle) llist_getdata(lp1);
                    //MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "<reset_PATProgram_status> service_handle: 0x%x\n", service_handle);
                    Iter->lastReturnedIndex = 0;

                    if (service_handle != MPE_SI_INVALID_HANDLE)
                    {
                        // Signal these program PMTs as removed so as to unblock any waiting callers
                        mpe_siNotifyTableChanged(IB_PMT,
                                MPE_SI_CHANGE_TYPE_REMOVE, service_handle);

                        MPE_LOG(
                                MPE_LOG_INFO,
                                MPE_MOD_SI,
                                "<reset_PATProgram_status> pn: %d, service_handle: 0x%x\n",
                                pi->program_number, service_handle);
                    }
                    lp1 = llist_after(lp1);
                }
            } // End if(!found && (PMT_AVAILABLE_SHORTLY == pi->pmt_status))

            lp = llist_after(lp);
        }
    }
    mpe_mutexRelease(ts_entry->list_lock);
}

/**
 *  PMT
 *
 * Set the PMT status to not available.
 *
 * <i>mpe_mpe_siSetPMTProgramStatusNotAvailable()</i>
 *
 * @param pi_handle is the handle to program info for which the PMT status is set to not available
 *              This is performed under a WRITE lock
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siSetPMTProgramStatusNotAvailable(mpe_SiProgramHandle pi_handle)
{
    mpe_SiProgramInfo *pi = (mpe_SiProgramInfo *) pi_handle;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siSetPMTProgramStatusNotAvailable> pi_handle: 0x%8.8x\n",
            pi_handle);

    /* Parameter check */
    if (pi_handle == MPE_SI_INVALID_HANDLE)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    if (pi != NULL)
    {
        pi->pmt_status = PMT_NOT_AVAILABLE;
        pi->pmt_version = INIT_TABLE_VERSION;
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_SI,
                "<mpe_siSetPMTProgramStatusNotAvailable> PMT status set to not available ..(program_number: %d)\n",
                pi->program_number);
        return MPE_SI_SUCCESS;
    }

    return MPE_SI_NOT_FOUND;
}

/**
 *  PMT
 *
 *  Set the PMT status to not available.
 *
 * <i>mpe_siSetPMTStatusNotAvailable()</i>
 *
 * @param frequency is the transport stream freq used to identify services to set the PMT status for
 *                  This is performed under a lock
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siSetPMTStatusNotAvailable(mpe_SiProgramHandle pi_handle)
{
    mpe_SiProgramInfo *pi = (mpe_SiProgramInfo *) pi_handle;
    mpe_SiTransportStreamEntry *ts = NULL;
    int found = 0;
    LINK *lp = NULL;

    /* Parameter check */
    if ((pi_handle == MPE_SI_INVALID_HANDLE) || ((ts = pi->stream) == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    mpe_mutexAcquire(ts->list_lock);

    lp = llist_first(ts->programs);
    while (lp)
    {
        pi = (mpe_SiProgramInfo *) llist_getdata(lp);
        if (pi != NULL)
        {
            pi->pmt_status = PMT_NOT_AVAILABLE;
            pi->pmt_version = INIT_TABLE_VERSION;
            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_SI,
                    "<mpe_siSetPMTStatusNotAvailable> PMT status set to not available ..(%d/%d)\n",
                    ts->frequency, pi->program_number);
            found = 1;
        }
        lp = llist_after(lp);
    }
    mpe_mutexRelease(ts->list_lock);

    if (!found)
    {
        //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siSetPMTStatusAvailableShortly> did not find any program entries...\n");
        return MPE_SI_NOT_FOUND;
    }
    return MPE_SI_SUCCESS;
}

mpe_Error mpe_siGetPMTProgramStatus(mpe_SiProgramHandle pi_handle, uint8_t *pmt_status)
{
    mpe_SiProgramInfo *pi = (mpe_SiProgramInfo *) pi_handle;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siGetPMTProgramStatus> pi_handle: 0x%8.8x\n",
            pi_handle);

    /* Parameter check */
    if (pi_handle == MPE_SI_INVALID_HANDLE)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    if (pi != NULL)
    {
        *pmt_status = pi->pmt_status;
        return MPE_SI_SUCCESS;
    }

    return MPE_SI_NOT_FOUND;
}

mpe_Error mpe_siRevertPMTStatus(mpe_SiProgramHandle pi_handle)
{
    mpe_SiProgramInfo *pi = (mpe_SiProgramInfo *) pi_handle;
    mpe_SiTransportStreamEntry *ts = NULL;
    int found = 0;
    LINK *lp = NULL;

    /* Parameter check */
    if ((pi_handle == MPE_SI_INVALID_HANDLE) || ((ts = pi->stream) == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    mpe_mutexAcquire(ts->list_lock);

    lp = llist_first(ts->programs);
    while (lp)
    {
        pi = (mpe_SiProgramInfo *) llist_getdata(lp);
        if (pi != NULL)
        {
            if (PMT_AVAILABLE_SHORTLY == pi->pmt_status)
            {
                pi->pmt_status = PMT_NOT_AVAILABLE;
                MPE_LOG(
                        MPE_LOG_DEBUG,
                        MPE_MOD_SI,
                        "<mpe_siRevertPMTStatus> PMT status set to not available ..(%d/%d)\n",
                        ts->frequency, pi->program_number);
                found = 1;
            }
        }
        lp = llist_after(lp);
    }
    mpe_mutexRelease(ts->list_lock);

    if (!found)
    {
        return MPE_SI_NOT_FOUND;
    }
    return MPE_SI_SUCCESS;
}

/**
 *  PMT
 *
 *  Set the PMT CRC
 *
 * <i>mpe_siSetPMTCRC()</i>
 *
 * @param service_handle is the service handle to set the PMT CRC for
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
void mpe_siSetPMTCRC(mpe_SiProgramHandle program_handle, uint32_t new_crc)
{
    mpe_SiProgramInfo *pi = (mpe_SiProgramInfo *) program_handle;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siSetPMTCRC> crc ...0x%x / 0x%x\n", new_crc, pi->pmt_crc);

    if ((pi->pmt_crc != 0) && (pi->crc_check == TRUE))
    {
        // PMT (with same version) and new CRC is received
        if (pi->pmt_crc != new_crc)
        {
            mpe_SiElementaryStreamList *saved_next = NULL;
            mpe_SiPMTReference *pmt_ref = pi->saved_pmt_ref;
            mpe_SiMpeg2DescriptorList* outer_desc =
                    (mpe_SiMpeg2DescriptorList*) pmt_ref->outer_descriptors_ref;
            mpe_SiElementaryStreamList
                    *elem_stream_list =
                            (mpe_SiElementaryStreamList *) pmt_ref->elementary_streams_ref;
            mpe_SiElementaryStreamList *new_elem_stream_list = NULL;

            // We have an existing PMT, but received a new version
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                    "<mpe_siSetPMTCRC> New crc: %d\n", new_crc);

            // delete saved outer descriptor loop
            if (outer_desc != NULL)
            {
                release_descriptors(outer_desc);
            }

            // delete saved elem stream loop
            while (elem_stream_list)
            {
                saved_next = elem_stream_list->next;

                if (elem_stream_list->ref_count == 1)
                {
                    remove_elementaryStream((mpe_SiElementaryStreamList **)(&pmt_ref->elementary_streams_ref), elem_stream_list);
                    delete_elementary_stream(elem_stream_list);
                    (pmt_ref->number_elem_streams)--;
                }
                else if (elem_stream_list->ref_count > 1)
                {
                    mark_elementary_stream(elem_stream_list);

                    // We are getting new PMT, send events for service existing component handles
                    // For handles delivered to Java increment the ref count
                    elem_stream_list->ref_count++;
                    //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siSetPMTVersion> Send service comp event - incremented ref count... %d\n", elem_stream_list->ref_count);

                    send_service_component_event(elem_stream_list,
                            MPE_SI_CHANGE_TYPE_REMOVE);
                }

                elem_stream_list = saved_next;
            }

            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                    "<mpe_siSetPMTCRC> num_elem_streams... %d\n",
                    pi->number_elem_streams);

            // Send 'ADD' event for each new elementary stream
            new_elem_stream_list = pi->elementary_streams;
            while (new_elem_stream_list)
            {
                new_elem_stream_list->ref_count++;
                send_service_component_event(new_elem_stream_list,
                        MPE_SI_CHANGE_TYPE_ADD);

                new_elem_stream_list = new_elem_stream_list->next;
            }
        }
        else
        {
            // CRC is same (version was also same), don't do anything
            // Keeping the old PMT

            mpe_SiPMTReference *pmt_ref = pi->saved_pmt_ref;

            mpe_SiElementaryStreamList *elem_stream_list =
                    pi->elementary_streams;
            mpe_SiElementaryStreamList *saved_next = NULL;

            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                    "<mpe_siSetPMTCRC> CRC hasn't changed...0x%x\n", new_crc);

            // delete newly allocated outer desc and elem streams

            // delete outer descriptor loop
            if (pi->outer_descriptors != NULL)
            {
                release_descriptors(pi->outer_descriptors);
                pi->outer_descriptors = NULL;
            }

            pi->number_outer_desc = 0;

            while (elem_stream_list)
            {
                saved_next = elem_stream_list->next;

                if (elem_stream_list->ref_count == 1)
                {
                    remove_elementaryStream((&pi->elementary_streams), elem_stream_list);
                    delete_elementary_stream(elem_stream_list);
                    (pi->number_elem_streams)--;
                }
                else if (elem_stream_list->ref_count > 1)
                {
                    mark_elementary_stream(elem_stream_list);

                    // We are getting new PMT, send events for service existing component handles
                    // For handles delivered to Java increment the ref count
                    elem_stream_list->ref_count++;
                    //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siSetPMTVersion> Send service comp event - incremented ref count... %d\n", elem_stream_list->ref_count);

                    send_service_component_event(elem_stream_list,
                            MPE_SI_CHANGE_TYPE_REMOVE);
                }

                elem_stream_list = saved_next;
            }

            // Restore old links
            pi->pcr_pid = pmt_ref->pcr_pid;
            pi->number_outer_desc = pmt_ref->number_outer_desc;
            pi->outer_descriptors
                    = (mpe_SiMpeg2DescriptorList*) pmt_ref->outer_descriptors_ref;
            pi->number_elem_streams = pmt_ref->number_elem_streams;
            pi->elementary_streams
                    = (mpe_SiElementaryStreamList *) pmt_ref->elementary_streams_ref;

            // Test only!
            /*
             {
             int i = 0;
             mpe_SiElementaryStreamList *elem_stream_list = pi->elementary_streams;
             MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "Restoring elem streams...\n");
             while(elem_stream_list)
             {
             MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "elem_stream[%d]... 0x%x\n", i++, elem_stream_list);
             elem_stream_list = elem_stream_list->next;
             }
             }
             */

        }

    }
    else if ((pi->pmt_crc == 0) && (pi->crc_check == FALSE))
    {
        // Initial acquisition of PMT
        // Send 'ADD' event for each new elementary stream
        mpe_SiElementaryStreamList *new_elem_stream_list = NULL;
        new_elem_stream_list = pi->elementary_streams;
        while (new_elem_stream_list)
        {
            new_elem_stream_list->ref_count++;
            send_service_component_event(new_elem_stream_list,
                    MPE_SI_CHANGE_TYPE_ADD);

            new_elem_stream_list = new_elem_stream_list->next;
        }

    }

    // update the crc
    pi->pmt_crc = new_crc;

    pi->crc_check = FALSE;

    if (pi->saved_pmt_ref != NULL)
    {
        // Free the memory allocated for pmt reference
        mpeos_memFreeP(MPE_MEM_SI, pi->saved_pmt_ref);
        pi->saved_pmt_ref = NULL;
    }
}

/**
 *  PMT
 *
 *  Set the outer descriptor for the given program entry
 *
 * <i>mpe_siSetOuterDescriptor()</i>
 *
 * @param program_handle is the program handle to set the descriptor for
 * @param tag is the descriptor tag field
 * @param length is the length of the descriptor
 * @param content is descriptor content
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siSetOuterDescriptor(mpe_SiProgramHandle program_handle,
        mpe_SiDescriptorTag tag, uint32_t length, void *content)
{
    mpe_SiProgramInfo *pi = (mpe_SiProgramInfo *) program_handle;
    mpe_SiMpeg2DescriptorList *desc_list = NULL;
    mpe_SiMpeg2DescriptorList *new_desc = NULL;

    /* Parameter check */
    if ((program_handle == MPE_SI_INVALID_HANDLE))
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mpe_siSetOuterDescriptor> Handle is invalid.  Cannot insert\n");
        return MPE_SI_INVALID_PARAMETER;
    }

    /* Store PMT outer descriptors in raw format. */
    /* Append descriptor at the end of existing list */
    if (MPE_SI_SUCCESS != mpeos_memAllocP(MPE_MEM_SI,
            sizeof(mpe_SiMpeg2DescriptorList), (void **) &(new_desc)))
    {
        MPE_LOG(
                MPE_LOG_WARN,
                MPE_MOD_SI,
                "<mpe_siSetOuterDescriptor> Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");
        return MPE_SI_OUT_OF_MEM;
    }
    new_desc->tag = tag;
    new_desc->descriptor_length = (unsigned char) length;
    if (length > 0)
    {
        if (MPE_SUCCESS != mpeos_memAllocP(MPE_MEM_SI, (sizeof(uint8_t)
                * length), (void **) &(new_desc->descriptor_content)))
        {
            mpeos_memFreeP(MPE_MEM_SI, new_desc);
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                    "<mpe_siSetOuterDescriptor> MEM Allocation 2 failed...\n");
            return MPE_SI_OUT_OF_MEM;
        }
        memcpy(new_desc->descriptor_content, content, length);
    }
    else
    {
        new_desc->descriptor_content = NULL;
    }
    new_desc->next = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siSetOuterDescriptor> tag:%d length:%d new_desc:0x%p\n", tag,
            length, new_desc);
    desc_list = pi->outer_descriptors;

    if (desc_list == NULL)
    {
        //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siSetOuterDescriptor> Inserting descriptor in first location\n");
        pi->outer_descriptors = new_desc;
    }
    else
    {
        //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siSetOuterDescriptor> Inserting descriptor at end\n");
        while (desc_list->next != NULL)
        {
            desc_list = desc_list->next;
        }
        desc_list->next = new_desc;
    }
    pi->number_outer_desc++;

    return MPE_SI_SUCCESS;
}

/**
 *  PMT
 *
 *  Set the es info length for the given elementary stream
 *
 * <i>mpe_siSetESInfoLength()</i>
 *
 * @param service_handle is the service handle to set the descriptor for
 * @param program_info_length is the length of es info length
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
void mpe_siSetESInfoLength(mpe_SiProgramHandle program_handle,
        uint32_t elem_pid, mpe_SiElemStreamType type, uint32_t es_info_length)
{
    mpe_SiProgramInfo *pi = (mpe_SiProgramInfo *) program_handle;
    mpe_SiElementaryStreamList *elem_list = NULL;

    /* Implement populating elementary stream info */
    elem_list = pi->elementary_streams;
    while (elem_list)
    {
        if ((elem_list->elementary_PID == elem_pid)
                && (elem_list->elementary_stream_type == type))
        {
            elem_list->es_info_length = es_info_length;
            break;
        }
        elem_list = elem_list->next;
    }
}

/**
 *  PMT
 *
 *  Set the elementary stream descriptor info for the given program entry based on the
 *  elementary stream Pid
 *
 * <i>mpe_siSetDescriptor()</i>
 *
 * @param program_handle is the program handle to set the descriptor info for
 * @param tag is the descriptor tag field
 * @param length is the length of the descriptor
 * @param content is descriptor content
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siSetDescriptor(mpe_SiProgramHandle program_handle,
        uint32_t elem_pid, mpe_SiElemStreamType type, mpe_SiDescriptorTag tag,
        uint32_t length, void *content)
{

    mpe_SiProgramInfo *pi = (mpe_SiProgramInfo *) program_handle;
    mpe_SiMpeg2DescriptorList *desc_list = NULL;
    mpe_SiMpeg2DescriptorList *new_desc = NULL;
    mpe_SiElementaryStreamList *elem_list = NULL;
    int done = 0;

    /* Parameter check */
    if ((program_handle == MPE_SI_INVALID_HANDLE) || (elem_pid == 0))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    /* Populate associated language from language_descriptor */

    /* Populate component name from component_name_descriptor */

    /* Implement populating elementary stream info */
    elem_list = pi->elementary_streams;
    while (elem_list && !done)
    {
        if ((elem_list->elementary_PID == elem_pid)
                && (elem_list->elementary_stream_type == type))
        {
            /* If the descriptor type is language set the associated
             language field of the elementary stream from descriptor
             content
             */
            if (tag == MPE_SI_DESC_ISO_639_LANGUAGE)
            {
                //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siSetDescriptor> language descriptor found...\n");
                /* associated language field id 4 bytes */
                if (length > 0)
                {
                    memset(elem_list->associated_language, 0, 4);
                    memcpy(elem_list->associated_language, (void*) content, 4);
                }
            }
            /* If the descriptor type is component descriptor
             set the component name field of the elementary stream
             from descriptor content
             */
            else if (tag == MPE_SI_DESC_COMPONENT_NAME) // Multiple String Structure
            {
                int i;
                uint8_t numStrings;
                uint8_t* mss_ptr = (uint8_t*) content;

                // Free any previously specified component names
                if (elem_list->service_component_names != NULL)
                {
                    mpe_SiLangSpecificStringList *walker =
                            elem_list->service_component_names;

                    while (walker != NULL)
                    {
                        mpe_SiLangSpecificStringList *toBeFreed = walker;
                        mpeos_memFreeP(MPE_MEM_SI, walker->string);
                        walker = walker->next;
                        mpeos_memFreeP(MPE_MEM_SI, toBeFreed);
                    }

                    elem_list->service_component_names = NULL;
                }

                // MSS -- Number of strings
                numStrings = *mss_ptr;
                mss_ptr += sizeof(uint8_t);

                // MSS -- String loop
                for (i = 0; i < numStrings; ++i)
                {
                    int j;
                    uint8_t numSegments;
                    char lang[4];
                    char mssString[1024];

                    mssString[0] = '\0';

                    // MSS -- ISO639 Language Code
                    lang[0] = *((char*) mss_ptr);
                    mss_ptr += sizeof(char);
                    lang[1] = *((char*) mss_ptr);
                    mss_ptr += sizeof(char);
                    lang[2] = *((char*) mss_ptr);
                    mss_ptr += sizeof(char);
                    lang[3] = '\0';

                    // MSS -- Number of segments
                    numSegments = *((uint8_t*) mss_ptr);
                    mss_ptr += sizeof(uint8_t);

                    // MSS -- Segment loop
                    for (j = 0; j < numSegments; ++j)
                    {
                        uint8_t compressionType;
                        uint8_t mode;
                        uint8_t numBytes;

                        // MSS -- Segment compression type
                        compressionType = *((uint8_t*) mss_ptr);
                        mss_ptr += sizeof(uint8_t);

                        // MSS -- Segment mode
                        mode = *((uint8_t*) mss_ptr);
                        mss_ptr += sizeof(uint8_t);

                        // ONLY Mode 0 and CompressionType 0 supported for now
                        if (compressionType != 0 || mode != 0)
                        {
                            MPE_LOG(
                                    MPE_LOG_ERROR,
                                    MPE_MOD_SI,
                                    "<mpe_siSetDescriptor> Invalid MSS segment mode or compression type!  Mode = %d, Compression = %d\n",
                                    mode, compressionType);
                            break;
                        }

                        // MSS -- Number of segment bytes
                        numBytes = *((uint8_t*) mss_ptr);
                        mss_ptr += sizeof(uint8_t);

                        // Append this segment onto our string
                        strncat(mssString, (const char*) mss_ptr, numBytes);
                    }

                    // Add this new string to our list
                    if (strlen(mssString) > 0)
                    {
                        mpe_SiLangSpecificStringList* new_entry = NULL;

                        // Allocate a new lang-specific string
                        if (MPE_SI_SUCCESS != mpeos_memAllocP(MPE_MEM_SI,
                                sizeof(struct mpe_SiLangSpecificStringList),
                                (void **) &new_entry))
                        {
                            MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                                    "<mpe_siSetDescriptor> Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");
                            return MPE_SI_OUT_OF_MEM;
                        }
                        if (MPE_SI_SUCCESS != mpeos_memAllocP(MPE_MEM_SI,
                                strlen(mssString) + 1,
                                (void **) &(new_entry->string)))
                        {
                            mpeos_memFreeP(MPE_MEM_SI, new_entry);
                            MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                                    "<mpe_siSetDescriptor> Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");
                            return MPE_SI_OUT_OF_MEM;
                        }
                        strcpy(new_entry->string, mssString);
                        strncpy(new_entry->language, lang, 4);

                        // Add new list entry to the front of list for this service component
                        new_entry->next = elem_list->service_component_names;
                        elem_list->service_component_names = new_entry;
                    }
                }
            }

            /*
             Even for the above two descriptor types that are parsed out, store them in raw format
             also to support org.ocap.si package.
             */
            /* Append descriptor at the end of existing list */
            if (MPE_SI_SUCCESS != mpeos_memAllocP(MPE_MEM_SI,
                    sizeof(mpe_SiMpeg2DescriptorList), (void **) &(new_desc)))
            {
                MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                        "<mpe_siSetDescriptor> Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");
                return MPE_SI_OUT_OF_MEM;
            }
            new_desc->tag = tag;
            new_desc->descriptor_length = (uint8_t) length;
            //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siSetDescriptor> new_desc->tag: 0x%x, tag: 0x%x, length: 0x%x, new_descriptor_length: 0x%x\n",
            //      new_desc->tag, tag, length, new_desc->descriptor_length);
            if (length > 0)
            {
                if (MPE_SUCCESS != mpeos_memAllocP(MPE_MEM_SI, (sizeof(uint8_t)
                        * length), (void **) &(new_desc->descriptor_content)))
                {
                    mpeos_memFreeP(MPE_MEM_SI, new_desc);
                    return MPE_SI_OUT_OF_MEM;
                }
                memcpy(new_desc->descriptor_content, content, length);
            }
            else
            {
                new_desc->descriptor_content = NULL;
            }
            new_desc->next = NULL;

            //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siSetDescriptor> created new decriptor 0x%x for elem_list 0x%x \n", new_desc, elem_list);
            {
                uint32_t i = 0;
                for (i = 0; i < length; i++)
                    MPE_LOG(
                            MPE_LOG_DEBUG,
                            MPE_MOD_SI,
                            "<mpe_siSetDescriptor> descriptor_content: 0x%x...\n",
                            *((uint8_t*) new_desc->descriptor_content + i));
            }

            desc_list = elem_list->descriptors;
            if (desc_list == NULL)
            {
                elem_list->descriptors = new_desc;
                elem_list->number_desc++;
                break;
            }
            else
            {
                while (desc_list)
                {
                    if (desc_list->next == NULL)
                    {
                        desc_list->next = new_desc;
                        elem_list->number_desc++;
                        done = 1;
                        break;
                    }
                    desc_list = desc_list->next;
                }
            }
        }

        elem_list = elem_list->next;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siSetDescriptor> tag:0x%x length: %d new_desc:0x%p\n", tag,
            length, new_desc);
    return MPE_SI_SUCCESS;
}

/*
 Internal method to get the service component type based on the elementary
 stream type.
 Service component types are generic types (ex: AUDIO, VIDEO, DATA etc.)
 Elementary stream types are specific (ex: MPEG1_AUDIO, MPEG2_VIDEO, MPE_SI_ELEM_DSM_CC_SECTIONS etc.)
 Hence if the elementary stream type is known the corresponding service
 component type id derived from it.
 */
static void get_serviceComponentType(mpe_SiElemStreamType stream_type,
        mpe_SiServiceComponentType *comp_type)
{

    switch (stream_type)
    {
    case MPE_SI_ELEM_MPEG_1_VIDEO:
    case MPE_SI_ELEM_MPEG_2_VIDEO:
    case MPE_SI_ELEM_VIDEO_DCII:
    case MPE_SI_ELEM_AVC_VIDEO:

        *comp_type = MPE_SI_COMP_TYPE_VIDEO;
        break;

    case MPE_SI_ELEM_MPEG_1_AUDIO:
    case MPE_SI_ELEM_MPEG_2_AUDIO:
    case MPE_SI_ELEM_ATSC_AUDIO:
    case MPE_SI_ELEM_ENHANCED_ATSC_AUDIO:
    case MPE_SI_ELEM_AAC_ADTS_AUDIO:
    case MPE_SI_ELEM_AAC_AUDIO_LATM:

        *comp_type = MPE_SI_COMP_TYPE_AUDIO;
        break;

    case MPE_SI_ELEM_STD_SUBTITLE:

        *comp_type = MPE_SI_COMP_TYPE_SUBTITLES;
        break;

        /* Not sure about these?? */
    case MPE_SI_ELEM_DSM_CC_STREAM_DESCRIPTORS:
    case MPE_SI_ELEM_DSM_CC_UN:
    case MPE_SI_ELEM_MPEG_PRIVATE_DATA:
    case MPE_SI_ELEM_DSM_CC:
    case MPE_SI_ELEM_DSM_CC_MPE:
    case MPE_SI_ELEM_DSM_CC_SECTIONS:
    case MPE_SI_ELEM_METADATA_PES:

        *comp_type = MPE_SI_COMP_TYPE_DATA;
        break;

    case MPE_SI_ELEM_MPEG_PRIVATE_SECTION:
    case MPE_SI_ELEM_SYNCHRONIZED_DOWNLOAD:
    case MPE_SI_ELEM_METADATA_SECTIONS:
    case MPE_SI_ELEM_METADATA_DATA_CAROUSEL:
    case MPE_SI_ELEM_METADATA_OBJECT_CAROUSEL:
    case MPE_SI_ELEM_METADATA_SYNCH_DOWNLOAD:

        *comp_type = MPE_SI_COMP_TYPE_SECTIONS;
        break;

    default:
        *comp_type = MPE_SI_COMP_TYPE_UNKNOWN;
        break;
    }

}

/*
 Internal method to append elementary stream to the end of existing
 elementary stream list within a service
 */
static void append_elementaryStream(mpe_SiProgramInfo *pi,
        mpe_SiElementaryStreamList *new_elem_stream)
{
    mpe_SiElementaryStreamList *elem_list;

    /* Populating elementary stream info */
    elem_list = pi->elementary_streams;

    if (elem_list == NULL)
    {
        pi->elementary_streams = new_elem_stream;
    }
    else
    {
        while (elem_list)
        {
            if (elem_list->next == NULL)
            {
                elem_list->next = new_elem_stream;
                break;
            }
            elem_list = elem_list->next;
        }
    }
    (pi->number_elem_streams)++;
}
/*
 Internal method to remove elementary stream from the existing
 elementary stream list within a service
 */
static mpe_Error remove_elementaryStream(mpe_SiElementaryStreamList **elem_stream_list_head,
        mpe_SiElementaryStreamList *rmv_elem_stream)
{
    int ret = MPE_SI_INVALID_PARAMETER;
    mpe_SiElementaryStreamList *elem_list;

    if (rmv_elem_stream == NULL || *elem_stream_list_head == NULL)
    {
        return ret;
    }

    if (*elem_stream_list_head == rmv_elem_stream)
    {
        *elem_stream_list_head = rmv_elem_stream->next;
        ret = MPE_SI_SUCCESS;
    }
    else
    {
        elem_list = (*elem_stream_list_head)->next;
        while (elem_list)
        {
            if (elem_list != rmv_elem_stream)
            {
                elem_list = elem_list->next;
            }
            else
            {
                elem_list->next = rmv_elem_stream->next;
                ret = MPE_SI_SUCCESS;
                break;
            }
        }
    }

    return ret;
}

/**
 *  PMT
 *
 *  Set the elementary stream info for the given program entry
 *
 * <i>mpe_siSetElementaryStream()</i>
 *
 * @param program_handle is the program handle to set the elementary stream info for
 * @param stream_type is the elementary stream type
 * @param elem_pid is the elementary stream pid
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siSetElementaryStream(mpe_SiProgramHandle program_handle,
        mpe_SiElemStreamType stream_type, uint32_t elem_pid)
{
    mpe_SiProgramInfo *pi = (mpe_SiProgramInfo *) program_handle;
    mpe_SiElementaryStreamList *new_elem_stream = NULL;
    mpe_SiServiceComponentType service_comp_type;
    LINK *lp = NULL;

    /* Parameter check */
    if ((program_handle == MPE_SI_INVALID_HANDLE) || (elem_pid == 0))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    if (MPE_SI_SUCCESS != mpeos_memAllocP(MPE_MEM_SI,
            sizeof(mpe_SiElementaryStreamList), (void **) &(new_elem_stream)))
    {
        MPE_LOG(
                MPE_LOG_WARN,
                MPE_MOD_SI,
                "<mpe_siSetElementaryStream> Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");
        return MPE_SI_OUT_OF_MEM;
    }
    new_elem_stream->elementary_PID = elem_pid;
    new_elem_stream->elementary_stream_type = stream_type;
    new_elem_stream->next = NULL;
    new_elem_stream->number_desc = 0;
    new_elem_stream->ref_count = 1;
    new_elem_stream->valid = TRUE;
    lp = llist_first(pi->services);
    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siSetElementaryStream> pi->services:0x%p service_count: %d\n",
            pi->services, pi->service_count);
    if (lp != NULL)
    {
        new_elem_stream->service_reference = (uint32_t) llist_getdata(lp);
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_SI,
                "<mpe_siSetElementaryStream> new_elem_stream:0x%p new_elem_stream->service_reference:0x%x\n",
                new_elem_stream, new_elem_stream->service_reference);
    }
    new_elem_stream->descriptors = NULL;
    new_elem_stream->service_component_names = NULL;
    new_elem_stream->es_info_length = 0;
    mpeos_timeGetMillis(&(new_elem_stream->ptime_service_component));

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<mpe_siSetElementaryStream> type:0x%x, pid: 0x%x new_elem_stream:0x%p\n",
            stream_type, elem_pid, new_elem_stream);

    /* Populate associated language after reading descriptors (language_descriptor) */
    /* For now set it to null */
    memset(new_elem_stream->associated_language, 0, 4);

    /* Based on the elementary stream type set the service component type */
    get_serviceComponentType(stream_type, &service_comp_type);

    new_elem_stream->service_comp_type = service_comp_type;

    /* Populating elementary stream info */
    append_elementaryStream(pi, new_elem_stream);

    //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siSetElementaryStream> created 0x%x\n", new_elem_stream);

    return MPE_SI_SUCCESS;
}

/*
 static mpe_Error check_elementary_stream(mpe_SiServiceHandle service_handle,
 mpe_SiElemStreamType stream_type,
 uint32_t elem_pid)
 {
 mpe_SiTableEntry *si_entry = NULL;
 mpe_SiElementaryStreamList *elem_stream_list = NULL;

 MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "check_elementary_stream> \n");

 si_entry = (mpe_SiTableEntry *) service_handle;

 //  Identify service component change type - ADD, REMOVE, MODIFY

 //  This function checks whether the elementary stream passed in
 //  already exists for the given service handle.
 //  If it does, the
 elem_stream_list = si_entry->elementary_streams;
 while(elem_stream_list)
 {
 if( (elem_stream_list->elementary_PID == elem_pid) &&
 (elem_stream_list->elementary_stream_type == stream_type) )
 {

 }
 elem_stream_list = elem_stream_list->next;
 }

 return MPE_SI_SUCCESS;
 }
 */

/**
 *  PMT
 *
 *  Set the PMT byte array for the given service entry
 *
 * <i>mpe_siSetPMT()</i>
 *
 * @param service_handle is the service handle to set the PMT info for
 * @param length is the length of array
 * @param pmt_array is the PMT byte array
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
mpe_Error mpe_siSetPMT(mpe_SiServiceHandle service_handle, uint32_t length,
        uint8_t* pmt_array)
{
    mpe_SiTableEntry *si_entry = (mpe_SiTableEntry *) service_handle;
    mpe_SiProgramInfo *pi = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<mpe_siSetPMT> \n");

    /* Parameter check */
    if ((service_handle == MPE_SI_INVALID_HANDLE) || (length == 0)
            || (pmt_array == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    /* Set the PMT */
    si_entry = (mpe_SiTableEntry *) service_handle;
    pi = si_entry->program;

    if (MPE_SI_SUCCESS != mpeos_memAllocP(MPE_MEM_SI, sizeof(uint8_t) * length,
            (void **) &(pi->pmt_byte_array)))
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mpe_siSetPMT> Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");
        return MPE_SI_OUT_OF_MEM;
    }

    memcpy(pi->pmt_byte_array, pmt_array, length);

    return MPE_SI_SUCCESS;
}

/**
 *  Internal Method to initialize service entries to default values
 *
 * <i>init_si_entry()</i>
 *
 * @param si_entry is the service entry to set the default values for
 *
 * @return MPE_SI_SUCCESS if successful, else
 *          return appropriate mpe_Error
 *
 */
static void init_si_entry(mpe_SiTableEntry *si_entry)
{
    // TODO: use static struct initialization

    /* set default values as defined in OCAP spec (annex T) */
    /* These are in the order they appear in the struct, to make it
     easier to verify all have been initialized. --mlb */
    si_entry->ref_count = 1;
    si_entry->activation_time = 0;
    mpeos_timeGetMillis(&(si_entry->ptime_service));
    si_entry->ts_handle = MPE_SI_INVALID_HANDLE;
    si_entry->program = NULL;
    si_entry->next = NULL;
    si_entry->isAppType = FALSE;
    si_entry->source_id = SOURCEID_UNKNOWN;
    si_entry->app_id = 0;
    si_entry->channel_type = CHANNEL_TYPE_HIDDEN; /* */
    si_entry->video_standard = VIDEO_STANDARD_UNKNOWN;
    si_entry->source_name_entry = NULL; /* default value */
    si_entry->descriptions = NULL; /* default value */
    si_entry->major_channel_number = MPE_SI_DEFAULT_CHANNEL_NUMBER; /* default value */
    si_entry->minor_channel_number = MPE_SI_DEFAULT_CHANNEL_NUMBER; /* default value */
    si_entry->service_type = MPE_SI_SERVICE_TYPE_UNKNOWN; /* default value */
    si_entry->freq_index = 0;
    si_entry->mode_index = 0;
    si_entry->dsgAttached = FALSE;
    si_entry->dsg_attach_count = 0; // OC over DSG attach count
    si_entry->state = SIENTRY_UNSPECIFIED;
    si_entry->virtual_channel_number = VIRTUAL_CHANNEL_UNKNOWN;
    si_entry->program_number = 0;
    si_entry->transport_type = 0; // Default is MPEG2
    si_entry->scrambled = FALSE;
    si_entry->hn_stream_session = 0;
    si_entry->hn_attach_count = 0;
}

static void init_rating_dimension(mpe_SiRatingDimension *dimension,
        char* dimName, char* values[], char* abbrevValues[])
{
    int i;

    // Dimension Name
    if (mpeos_memAllocP(MPE_MEM_SI, sizeof(mpe_SiLangSpecificStringList),
            (void **) &(dimension->dimension_names)) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                "Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");
        return;
    }
    dimension->dimension_names->next = NULL;
    strcpy(dimension->dimension_names->language, "eng");
    dimension->dimension_names->string = dimName;

    // Dimension Values
    if (mpeos_memAllocP(MPE_MEM_SI, sizeof(mpe_SiRatingValuesDefined)
            * dimension->values_defined, (void **) &(dimension->rating_value))
            != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                "Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");
        return;
    }

    for (i = 0; i < dimension->values_defined; ++i)
    {
        mpe_SiRatingValuesDefined *rating_values =
                &(dimension->rating_value[i]);

        // Rating Value
        if (mpeos_memAllocP(MPE_MEM_SI, sizeof(mpe_SiLangSpecificStringList),
                (void **) &rating_values->rating_value_text) != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                    "Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");
            return;
        }
        rating_values->rating_value_text->next = NULL;
        strcpy(rating_values->rating_value_text->language, "eng");
        rating_values->rating_value_text->string = values[i];

        // Abbreviated Rating Value
        if (mpeos_memAllocP(MPE_MEM_SI, sizeof(mpe_SiLangSpecificStringList),
                (void **) &rating_values->abbre_rating_value_text)
                != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                    "Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");
            return;
        }
        rating_values->abbre_rating_value_text->next = NULL;
        strcpy(rating_values->abbre_rating_value_text->language, "eng");
        rating_values->abbre_rating_value_text->string = abbrevValues[i];
    }
}

/**
 *  Internal Method to initialize default rating region (US (50 states + possesions))
 *
 * <i>init_default_rating_region(void)</i>
 *
 * @param  NONE
 *
 * @return NONE
 *
 * NOTE:  Future impl may take a default rating region enum type as
 * param.
 *
 */
static void init_default_rating_region(void)
{
    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_SI,
            "<init_default_rating_region> Initializing the default rating region - US (50 states + possessions)\n");

    init_rating_dimension(&g_US_RRT_dim_Entire_Audience,
            g_US_RRT_eng_dim_Entire_Audience,
            g_US_RRT_eng_vals_Entire_Audience,
            g_US_RRT_eng_abbrev_vals_Entire_Audience);
    init_rating_dimension(&g_US_RRT_dim_Dialogue, g_US_RRT_eng_dim_Dialogue,
            g_US_RRT_eng_vals_Dialogue, g_US_RRT_eng_abbrev_vals_Dialogue);
    init_rating_dimension(&g_US_RRT_dim_Language, g_US_RRT_eng_dim_Language,
            g_US_RRT_eng_vals_Language, g_US_RRT_eng_abbrev_vals_Language);
    init_rating_dimension(&g_US_RRT_dim_Sex, g_US_RRT_eng_dim_Sex,
            g_US_RRT_eng_vals_Sex, g_US_RRT_eng_abbrev_vals_Sex);
    init_rating_dimension(&g_US_RRT_dim_Violence, g_US_RRT_eng_dim_Violence,
            g_US_RRT_eng_vals_Violence, g_US_RRT_eng_abbrev_vals_Violence);
    init_rating_dimension(&g_US_RRT_dim_Children, g_US_RRT_eng_dim_Children,
            g_US_RRT_eng_vals_Children, g_US_RRT_eng_abbrev_vals_Children);
    init_rating_dimension(&g_US_RRT_dim_Fantasy_Violence,
            g_US_RRT_eng_dim_Fantasy_Violence,
            g_US_RRT_eng_vals_Fantasy_Violence,
            g_US_RRT_eng_abbrev_vals_Fantasy_Violence);
    init_rating_dimension(&g_US_RRT_dim_MPAA, g_US_RRT_eng_dim_MPAA,
            g_US_RRT_eng_vals_MPAA, g_US_RRT_eng_abbrev_vals_MPAA);

    /*
     * fill the global dimension array ( size = number of US rating region dimensions)
     * with the the US rating region dimensions
     */
    g_RRT_US_dimension[0] = g_US_RRT_dim_Entire_Audience;
    g_RRT_US_dimension[1] = g_US_RRT_dim_Dialogue;
    g_RRT_US_dimension[2] = g_US_RRT_dim_Language;
    g_RRT_US_dimension[3] = g_US_RRT_dim_Sex;
    g_RRT_US_dimension[4] = g_US_RRT_dim_Violence;
    g_RRT_US_dimension[5] = g_US_RRT_dim_Children;
    g_RRT_US_dimension[6] = g_US_RRT_dim_Fantasy_Violence;
    g_RRT_US_dimension[7] = g_US_RRT_dim_MPAA;

    // Region Name
    if (mpeos_memAllocP(MPE_MEM_SI, sizeof(mpe_SiLangSpecificStringList),
            (void **) &(g_RRT_US.rating_region_name_text)) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                "Mem allocation failed, returning MPE_SI_OUT_OF_MEM...\n");
        return;
    }
    g_RRT_US.rating_region_name_text->next = NULL;
    strcpy(g_RRT_US.rating_region_name_text->language, "eng");
    g_RRT_US.rating_region_name_text->string = g_RRT_US_eng;
}

/**
 *  Internal Method to release descriptor list within service entries
 *
 * <i>release_descriptors()</i>
 *
 * @param desc is the descriptor list to delete
 *
 *
 */
static void release_descriptors(mpe_SiMpeg2DescriptorList *desc)
{
    mpe_SiMpeg2DescriptorList *tempDescPtr = desc;
    mpe_SiMpeg2DescriptorList *tempDescNextPtr = NULL;

    while (tempDescPtr != NULL)
    {
        tempDescPtr->tag = 0;
        tempDescPtr->descriptor_length = 0;

        //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<release_descriptors> deleting descriptor 0x%x\n", tempDescPtr);

        if (tempDescPtr->descriptor_content)
        {
            mpeos_memFreeP(MPE_MEM_SI, tempDescPtr->descriptor_content);
            tempDescPtr->descriptor_content = NULL;
        }
        tempDescNextPtr = tempDescPtr->next;
        mpeos_memFreeP(MPE_MEM_SI, tempDescPtr);
        tempDescPtr = tempDescNextPtr;
    }
}

/**
 *  Internal Method to release elementary stream list within service entries
 *
 * <i>release_elementary_streams()</i>
 *
 * @param eStream is the elementary stream list to delete
 *
 *
 */
void release_elementary_streams(mpe_SiElementaryStreamList *eStream)
{
    mpe_SiElementaryStreamList *tempEStreamPtr = eStream;
    mpe_SiElementaryStreamList *tempEStreamNextPtr = NULL;
    mpe_Bool deleteEStrem = FALSE;

    while (tempEStreamPtr != NULL)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<release_elementary_streams> ref_count: %d\n",
                tempEStreamPtr->ref_count);

        tempEStreamNextPtr = tempEStreamPtr->next;

        if (tempEStreamPtr->ref_count == 1)
        {
            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_SI,
                    "<release_elementary_streams> deleting elementary steam 0x%p\n",
                    tempEStreamPtr);

            strcpy(tempEStreamPtr->associated_language, "");
            tempEStreamPtr->es_info_length = 0;
            tempEStreamPtr->elementary_PID = 0;
            tempEStreamPtr->service_comp_type = 0;

            /* De-allocate service component name */
            if (tempEStreamPtr->service_component_names != NULL)
            {
                mpe_SiLangSpecificStringList *walker =
                        tempEStreamPtr->service_component_names;

                while (walker != NULL)
                {
                    mpe_SiLangSpecificStringList *toBeFreed = walker;

                    mpeos_memFreeP(MPE_MEM_SI, walker->string);

                    walker = walker->next;

                    mpeos_memFreeP(MPE_MEM_SI, toBeFreed);
                }

                tempEStreamPtr->service_component_names = NULL;
            }

            /* De-allocate descriptors */
            if (tempEStreamPtr->descriptors != NULL)
            {
                release_descriptors(tempEStreamPtr->descriptors);
                tempEStreamPtr->descriptors = NULL;
            }

            deleteEStrem = TRUE;
            tempEStreamPtr->number_desc = 0;
        }
        else if (tempEStreamPtr->ref_count > 1)
        {
            // Send event and mark it.
            mark_elementary_stream(tempEStreamPtr);

            tempEStreamPtr->ref_count++;

            send_service_component_event(tempEStreamPtr,
                    MPE_SI_CHANGE_TYPE_REMOVE);
        }

        if (deleteEStrem == TRUE)
        {
            mpeos_memFreeP(MPE_MEM_SI, tempEStreamPtr);
            tempEStreamPtr = NULL;
            deleteEStrem = FALSE;
        }
        tempEStreamPtr = tempEStreamNextPtr;
    }
}

/**
 *  Internal Method to release service entries
 *
 * <i>release_si_entry()</i>
 *
 * @param si_entry is the service entry to delete
 *
 *
 */
static void release_si_entry(mpe_SiTableEntry *si_entry)
{
    mpe_SiTransportStreamEntry *ts_entry = NULL;

    if (si_entry == NULL)
    {
        return;
    }

    /* Free service descriptions */
    if (si_entry->descriptions != NULL)
    {
        mpe_SiLangSpecificStringList *walker = si_entry->descriptions;

        while (walker != NULL)
        {
            mpe_SiLangSpecificStringList *toBeFreed = walker;

            mpeos_memFreeP(MPE_MEM_SI, walker->string);

            walker = walker->next;

            mpeos_memFreeP(MPE_MEM_SI, toBeFreed);
        }

        si_entry->descriptions = NULL;
    }
    ts_entry = (mpe_SiTransportStreamEntry *) si_entry->ts_handle;

    if (ts_entry != NULL)
    {
        LINK *lp;
        mpe_mutexAcquire(ts_entry->list_lock);
        lp = llist_linkof(ts_entry->services, si_entry);
        if (lp != NULL)
        {
            llist_rmlink(lp);
            ts_entry->service_count--;
            ts_entry->visible_service_count--;
        }
        si_entry->ts_handle = 0;
        if (si_entry->program)
        {
            LINK *lp1;
            mpe_SiProgramInfo *pi = si_entry->program;
            lp1 = llist_linkof(pi->services, si_entry);
            if (lp1 != NULL)
            {
                llist_rmlink(lp1);
                pi->service_count--;
            }
            si_entry->program = NULL;
            if (pi->service_count == 0)
            {
                LINK *lp2 = llist_linkof(ts_entry->programs, pi);
                if (lp2 != NULL)
                {
                    llist_rmlink(lp2);
                    ts_entry->program_count--;
                }
                release_program_info_entry(pi);
            }
        }
        mpe_mutexRelease(ts_entry->list_lock);

        if ((0 == ts_entry->service_count) && (0 == ts_entry->program_count))
        {
            release_transport_stream_entry(ts_entry);
        }
    }
}

mpe_Error mpe_siReleaseServiceHandle(mpe_SiServiceHandle service_handle)
{
    mpe_SiTableEntry *si_entry = NULL;
    mpe_SiTableEntry *te_walker;

    // Parameter check
    if (service_handle == MPE_SI_INVALID_HANDLE)
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    si_entry = (mpe_SiTableEntry *) service_handle;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siReleaseServicehandle> ref_count %d\n", si_entry->ref_count);

    if (si_entry->ref_count <= 1)
    {
        // ref_count should never be less than 1, it means that
        // there may be un-balanced get/release service handles
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<mpe_siReleaseServiceHandle> error ref_count %d\n",
                si_entry->ref_count);
        return MPE_SI_INVALID_PARAMETER;
    }

    --(si_entry->ref_count);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<mpe_siReleaseServiceHandle> ref_count %d\n", si_entry->ref_count);

    //TODOLIST ,when do we set the state to OLD???
    if (si_entry->ref_count == 1)
    {
        te_walker = g_si_entry;
        //first node of the list
        if (te_walker && (te_walker == si_entry))
        {
            g_si_entry = te_walker->next;
        }
        else
        {
            while (te_walker && te_walker->next)
            {
                if (te_walker->next == si_entry)
                {
                    MPE_LOG(
                            MPE_LOG_DEBUG,
                            MPE_MOD_SI,
                            "<mpe_siReleaseServiceHandle> set %p->next to %p\n",
                            te_walker, te_walker->next->next);
                    te_walker->next = te_walker->next->next;
                    break;
                }
                te_walker = te_walker->next;
            }
        }

        delete_service_entry(si_entry);
    }

    return MPE_SI_SUCCESS;
}

static void delete_service_entry(mpe_SiTableEntry *si_entry)
{
    if (si_entry != NULL)
    {
        release_si_entry(si_entry);

        // decrement the service count
        --g_numberOfServices;

        //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<send_service_update_event>  \n");
        //need to send remove event??

        // Acquire registration list mutex
        //  mpe_mutexAcquire(g_registration_list_mutex);

        //  walker = g_si_registration_list;

        mpeos_memFreeP(MPE_MEM_SI, si_entry);
    }
}

#ifdef SI_DONT_COMMENT_OUT
static mpe_Error delete_marked_service_entries()
{
    mpe_SiTableEntry **walker;
    mpe_SiTableEntry *prev;
    mpe_SiTableEntry *to_delete = NULL;
    mpe_SiTableEntry *si_entry = NULL;
    mpe_Bool endoflist = false;
    mpe_Bool done = false;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "removeMarkedEntries()\n");

    if (!g_modulationsUpdated || !g_frequenciesUpdated || !g_channelMapUpdated)
    {
        return;
    }

    // Get global lock
    mpe_condGet(g_global_si_cond);

    walker = &g_si_entry;

    while (*walker && !done)
    {
        if ( (*walker)->state == SIENTRY_OLD )
        {
            if ((*walker)->ref_count > 1)
            {
                // Send event to Java that this service has been removed!!
                // Delete it when the ref_count reaches 1
                si_entry = *walker;

                // increment the ref_count?
                ++si_entry->ref_count;

                send_service_update_event(si_entry, MPE_SI_CHANGE_TYPE_REMOVE);
            }
            else if ((*walker)->ref_count == 1)
            {
                // First delete the elementary streams and other
                // allocations made on behalf of the service entry.
                // Elementary streams themselves may have references
                // held by the java layer. But, service components can
                // float around un-coupled from the service itself and
                // their references are cleaned up independently.
                // We only need to account for cleaning up of 'service'
                // entries in this method.
                to_delete = *walker;
            }

            *walker = (*walker)->next;
            //break;
        }
        else
        {
            if ((*walker)->next != NULL)
            {
                walker = &((*walker)->next);

                if ((*walker)->next == NULL)
                endoflist = true;
            }
            else
            {
                // *walker = NULL;
                done = 1;
            }
        }

        if (to_delete != NULL)
        {
            delete_service_entry(to_delete);
        }

    }

    mpe_condSet(g_global_si_cond);
    // Release global lock

    return MPE_SI_SUCCESS;
}

static void send_service_update_event(mpe_SiTableEntry *si_entry, uint32_t changeType)
{
    mpe_Event event = MPE_SI_EVENT_SERVICE_UPDATE;
    mpe_SiRegistrationListEntry *walker = NULL;

    if (si_entry == NULL)
    {
        return;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "<send_service_update_event>  \n");

    // Acquire registration list mutex
    mpe_mutexAcquire(g_registration_list_mutex);

    walker = g_si_registration_list;

    while (walker)
    {
        uint32_t termType = walker->edHandle->terminationType;

        mpe_eventQueueSend(walker->edHandle->eventQ,
                event,
                (void*)si_entry, (void*)walker->edHandle,
                changeType);

        // Do we need to unregister this client?
        if (termType == MPE_ED_TERMINATION_ONESHOT ||
                (termType == MPE_ED_TERMINATION_EVCODE && event == walker->terminationEvent))
        {
            if ( (*walker)->deleteSelf )
            {
                to_delete = *walker;
                *walker = (*walker)->next;
                mpeos_memFreeP(MPE_MEM_SI,to_delete);
                to_delete = NULL;
                break;
            }

            walker = walker->next;
        }

        // Unregister all clients that have been added to unregistration list
        unregister_clients();

        // Release registration list mutex
        mpe_mutexRelease(g_registration_list_mutex);
    }
}
#endif /* #ifdef SI_DONT_COMMENT_OUT */

mpe_SiServiceHandle mpe_siFindFirstServiceFromProgram(
        mpe_SiProgramHandle si_program_handle, SIIterator *iter)
{
    SIIterator *Iter = (SIIterator *) iter;
    mpe_SiServiceHandle retSvc = MPE_SI_INVALID_HANDLE;

    if ((si_program_handle != MPE_SI_INVALID_HANDLE) && (Iter != NULL))
    {
        Iter->program = (mpe_SiProgramInfo *) si_program_handle;
        if (Iter->program->stream != NULL) // Should *never* happen, but..
        {
            LINK *lp;
            mpe_mutexAcquire(Iter->program->stream->list_lock);
            lp = llist_first(Iter->program->services);
            if (lp != NULL)
            {
                retSvc = (mpe_SiServiceHandle) llist_getdata(lp);
            }
            Iter->lastReturnedIndex = 0;
            mpe_mutexRelease(Iter->program->stream->list_lock);
        }
    }
    return retSvc;
}

mpe_SiServiceHandle mpe_siGetNextServiceFromProgram(SIIterator *iter)
{
    SIIterator *Iter = (SIIterator *) iter;
    mpe_SiServiceHandle retSvc = MPE_SI_INVALID_HANDLE;

    if ((Iter != NULL) && (Iter->program != NULL))
    {
        if (Iter->program->stream != NULL) // Should *never* happen, but..
        {
            LINK *lp;
            mpe_mutexAcquire(Iter->program->stream->list_lock);
            lp = llist_getNodeAtIndex(Iter->program->services,
                    Iter->lastReturnedIndex + 1);
            if (lp != NULL)
            {
                retSvc = (mpe_SiServiceHandle) llist_getdata(lp);
            }
            if ((char *) retSvc != NULL)
                Iter->lastReturnedIndex++;

            mpe_mutexRelease(Iter->program->stream->list_lock);
        }
    }
    return retSvc;
}

void mpe_SiTakeFromProgram(mpe_SiProgramHandle program_handle,
        mpe_SiServiceHandle service_handle)
{
    mpe_SiTransportStreamEntry *ts;
    mpe_SiTableEntry *se = (mpe_SiTableEntry *) service_handle;
    mpe_SiProgramInfo *pi = (mpe_SiProgramInfo *) program_handle;
    LINK *lp1;
    LINK *lp2;

    ts = (mpe_SiTransportStreamEntry *) pi->stream;

    mpe_mutexAcquire(ts->list_lock);
    lp1 = llist_linkof(ts->services, (mpe_SiTableEntry *) service_handle);
    if (lp1 != NULL)
    {
        llist_rmlink(lp1);
        ts->service_count--;
        if (se->channel_type != CHANNEL_TYPE_HIDDEN)
            ts->visible_service_count--;
    }
    //list_RemoveData(ts->services, (void *)service_handle);
    lp2 = llist_linkof(pi->services, (mpe_SiTableEntry *) service_handle);
    if (lp2 != NULL)
    {
        llist_rmlink(lp2);
        pi->service_count--;
    }
    //list_RemoveData(pi->services, (void *)service_handle);
    mpe_mutexRelease(ts->list_lock);
    se->program = NULL;
    se->ts_handle = MPE_SI_INVALID_HANDLE;
}

void mpe_SiGiveToProgram(mpe_SiProgramHandle program_handle,
        mpe_SiServiceHandle service_handle)
{
    mpe_SiTransportStreamEntry *ts;
    mpe_SiTableEntry *se = (mpe_SiTableEntry *) service_handle;
    mpe_SiProgramInfo *pi = (mpe_SiProgramInfo *) program_handle;
    LINK *lp1, *lp2;

    ts = (mpe_SiTransportStreamEntry *) pi->stream;

    mpe_mutexAcquire(ts->list_lock);
    lp1 = llist_mklink((void *) service_handle);
    llist_append(ts->services, lp1);
    ts->service_count++;
    ts->visible_service_count++;
    lp2 = llist_mklink((void *) service_handle);
    llist_append(pi->services, lp2);
    pi->service_count++;
    mpe_mutexRelease(ts->list_lock);
    se->program = pi;
    se->ts_handle = (mpe_SiTransportHandle) ts;
}

mpe_Error mpe_siGetProgramHandleFromServiceEntry(
        mpe_SiServiceHandle service_handle, mpe_SiProgramHandle *program_handle)
{
    mpe_SiTableEntry *si = (mpe_SiTableEntry *) service_handle;

    if ((service_handle == MPE_SI_INVALID_HANDLE) || (program_handle == NULL))
    {
        return MPE_SI_INVALID_PARAMETER;
    }

    *program_handle = (mpe_SiProgramHandle) si->program;
    return MPE_SI_SUCCESS;
}

/**
 * Debug status function to acquire the number of SI entries in the SIDB.
 *
 * @param status is pointer for returning the count.
 *
 * @return MPE_SUCCESS upon successfully returning the count.
 */
static mpe_Error si_getSiEntryCount(void *status)
{
    mpe_SiTableEntry *walker;
    uint32_t count = 0;

    /* Acquire global mutex */
    mpe_mutexAcquire(g_global_si_mutex);

    /* Use internal method release_si_entry() to delete service entries */
    for (walker = g_si_entry; walker; walker = walker->next)
        count++;

    mpe_mutexRelease(g_global_si_mutex);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "si_getSiEntryCount() - count = %d\n",
            count);

    *(uint32_t*) status = count;
    return MPE_SUCCESS;
}

/**
 * Debug status function to acquire status information for a specific SI entry from the SIDB.
 * The entry to acquire is identified by its index location within the SI entry list.
 *
 * @param index is the index of the entry to acquire.
 * @param size is a pointer to the size of the information buffer used to return the status.
 * @param status is a pointer to the information buffer.
 * @param statBuff_1024 is a pointer to local SIDB buffer used to generate the status information.
 *
 * @return MPE_SUCCESS upon successfully acquiring the status information.
 */
static mpe_Error si_getSiEntry(uint32_t index, uint32_t *size, void *status,
        void *statBuff_1024)
{
    mpe_SiTableEntry *si_entry;
    uint32_t i;
    uint32_t statSz = 0;
    uint32_t maxSize;

    /* Acquire global mutex */
    mpe_mutexAcquire(g_global_si_mutex);

    /* Iterate to the target si entry. */
    for (i = 0, si_entry = g_si_entry; si_entry != NULL; si_entry
            = si_entry->next, ++i)
    {
        if (i == index)
        {
            statSz
                    = sprintf(
                            statBuff_1024,
                            "%3d: CHAN_TYP:%d F:%d MJ:%d MN:%d MOD_IDX:%d MOD_MODE:%d PRG_NUM:%d SRC_ID:%d/0x%x TNR_ID:%d TS_HAN:%u, SVC_TYP:%d, \n",
                            (int) i + 1, si_entry->channel_type,
                            si_entry->freq_index,
                            (int) (g_frequency[si_entry->freq_index]),
                            (int) si_entry->major_channel_number,
                            (int) si_entry->minor_channel_number,
                            (int) si_entry->mode_index,
                            g_mode[si_entry->mode_index],
                            (int) si_entry->source_id,
                            (int) si_entry->source_id,
                            (int) si_entry->tuner_id,
                            (unsigned int) si_entry->ts_handle,
                            si_entry->service_type);
            break;
        }
    }
    /* Get the size of the return buffer. */
    maxSize = *size;

    /* Copy the status information (up to buffer size). */
    memcpy(status, statBuff_1024, (statSz + 1 <= maxSize) ? statSz + 1
            : maxSize);

    /* Release global mutex. */
    mpe_mutexRelease(g_global_si_mutex);

    /* Return the size of information copied. */
    *size = (uint32_t)(statSz + 1 <= maxSize ? statSz + 1 : maxSize);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "si_getSiEntry(%d) - entry = %s\n",
            index, (i == index) ? (char*)statBuff_1024 : "NOT FOUND!");

    return ((statSz + 1 > maxSize) ? MPE_EINVAL : MPE_SUCCESS);
}

/* Internal buffer for status information generation. */
static char statBuff_1024[1024];

/**
 * Acquire the specified status information from the SIDB.  This function is called directly
 * by the MPE debug manager in support of the mpe_dbgStatus() API.
 *
 * @param type is the type of information identifier
 * @param size is a pointer to a size value indicating the maximum size of the buffer, which
 *        will be updated with the actual size returned.
 * @param status is the structure of buffer pointer to use to return the information.
 * @param params is an optional pointer to a structure that may further define/specify
 *        the nature or constraints of the information request.
 *
 * @return MPE_SUCCESS upon success in returning the requested information.
 */
mpe_Error si_dbgStatus(mpe_DbgStatusId type, uint32_t *size, void *status,
        void *params)
{
    switch (type & MPE_DBG_STATUS_TYPEID_MASK)
    {
    case MPE_DBG_STATUS_SI_ENTRY_COUNT:
        if (*size > sizeof(uint32_t))
            return MPE_EINVAL;
        *size = sizeof(uint32_t);
        return si_getSiEntryCount(status);
    case MPE_DBG_STATUS_SI_ENTRY:
        if (*size > sizeof(statBuff_1024))
            return MPE_EINVAL;
        return si_getSiEntry(*(uint32_t*) params, size, status, statBuff_1024);
    default:
        return MPE_EINVAL;
    }
}

