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
#ifndef _SITP_PSI_NEW_H
#define _SITP_PSI_NEW_H 1
/*
 ******************************************************************************
 *                              Includes
 ******************************************************************************
 */

#ifdef __cplusplus
extern "C"
{
#endif

/*
 *****************************************************************************
 * Include files for this component
 *****************************************************************************
 */
#include <mpe_types.h>    /* Resolve basic type references. */
#include <mpe_error.h>    /* Resolve error type references. */
#include <mpe_os.h>       /* Resolve mem api references. */
#include <mpe_dbg.h>
#include <mpeos_media.h>  /* Resolve media  references. */
#include <mpeos_thread.h>
#include <mpeos_util.h>
#include <mpeos_filter.h>
#include <simgr.h>
#include <mpe_filter.h>
#include "filter_support.h"
#include <mpe_filterevents.h>

/*
 *****************************************************************************
 *  global function prototypes (invoke by the outside world)
 *****************************************************************************
 */
mpe_Error sitp_psi_Start(void);
void sitp_psi_ShutDown(void);

/*
 ******************************************************************************
 *                               defines
 ******************************************************************************
 */

/* Follow the previous sitp debug statement model */
#define PSIMODULE                         "SITP_PSI"

/* Events to start the OOB/DSG/HN table acquisition */
#define OOB_PSI_ACQUISITION               999
#define OOB_DSG_PSI_ACQUISITION           998
#define OOB_HN_STREAM_PSI_ACQUISITION     997

/* Event to start the OOB table acquisition */
#define OOB_PSI_DISABLED               996

/* Event to shutdown the psi thread */
#define SITP_PSI_EVENT_SHUTDOWN           9999


/* Initial table version default value */
#define INIT_TABLE_VERSION                32

/*
 ** index into the FilterSpec value array for the
 ** version  number.
 */
#define PSI_VERSION_INDEX                5

//*****************************************************************************
//                               enumerations
//*****************************************************************************

/* SITP component return codes */
typedef enum _sitp_error_e
{
    SITP_SUCCESS = 0,
    SITP_FAILURE,
    SITP_TUNER_PARAM_FAILURE,
    SITP_RESOURCE_FAILURE
} sitp_error_e;

typedef enum _sitp_table_type_e
{
    TABLE_TYPE_PAT = 0,
    TABLE_TYPE_PMT,
    TABLE_TYPE_NIT,
    TABLE_TYPE_SVCT,
    TABLE_TYPE_NTT,
    TABLE_TYPE_CVCT,
    TABLE_TYPE_NONE,
    TABLE_TYPE_UNKNOWN = TABLE_TYPE_NONE
} sitp_table_type_e;

/* SITP PSI state */
typedef enum _sitp_psi_state_e
{
    SITP_PSI_IDLE = 0,
    SITP_PSI_WAIT_INITIAL_PAT,
    SITP_PSI_WAIT_INITIAL_PRIMARY_PMT,
    SITP_PSI_WAIT_INITIAL_SECONDARY_PMT,
    SITP_PSI_WAIT_REVISION
} sitp_psi_state_e;

/* SITP PSI tuner type */
typedef enum _sitp_psi_tuner_type_e
{
    SITP_PSI_TUNER_TYPE_OOB = 0,
    SITP_PSI_TUNER_TYPE_IB,
    SITP_PSI_TUNER_TYPE_DSG,
    SITP_PSI_TUNER_TYPE_HN
} sitp_psi_tuner_type_e;

/* SITP PSI tuner state */
typedef enum _sitp_psi_tuner_state_e
{
    SITP_PSI_TUNER_STATE_IDLE = 0,
    SITP_PSI_TUNER_STATE_TUNED
} sitp_psi_tuner_state_e;

/* SITP PSI filter priority */
typedef enum _sitp_psi_filter_priority_e
{
    // Initial PAT and initial primary PMT have the same priority
    // One should not pre-empt the other
    SITP_PSI_FILTER_PRIORITY_INITIAL_PAT = 40,
    SITP_PSI_FILTER_PRIORITY_INITIAL_PRIMARY_PMT = 40,
    SITP_PSI_FILTER_PRIORITY_INITIAL_SECONDARY_PMT = 30,
    SITP_PSI_FILTER_PRIORITY_REVISION_PAT = 20,
    SITP_PSI_FILTER_PRIORITY_REVISION_PMT = 20
} sitp_psi_filter_priority_e;

//*****************************************************************************
//                              typedefs
//*****************************************************************************/

typedef struct _sitp_psi_last_tune_info_t
{
    uint32_t frequency;
    uint32_t program_number;
    mpe_SiModulationMode modulation_mode;
    uint32_t cleanup_frequency;
    uint32_t cleanup_program_number;
    mpe_SiModulationMode cleanup_modulation_mode;
} sitp_psi_last_tune_info_t;

typedef struct _sitp_psi_filter_t
{
    sitp_table_type_e table_type;
    uint32_t pid;
    uint32_t version_number;
    uint32_t transport_stream_id;
    mpe_Bool filter_set;
    uint16_t program_number;
    sitp_psi_filter_priority_e  filter_priority;
    mpe_SharedFilter shared_filterId;
    struct _sitp_psi_fsm_data_t *fsm_data;
} sitp_psi_filter_t;

typedef struct _sitp_psi_fsm_data_t
{
    sitp_psi_state_e            psi_state;
    mpe_FilterSource            filter_source;
    mpe_SiTransportStreamHandle ts_handle;
    uint32_t                    sitp_psi_pat_timeout_val;
    uint32_t                    sitp_psi_pmt_timeout_val;
    uint32_t                    sitp_psi_round_robin_val;
    sitp_psi_filter_t           shared_filter_PAT;
    sitp_psi_filter_t           shared_filter_primary_PMT;
    ListSI                      shared_filter_secondary_PMTs; // this is a list
    mpe_FilterGroup             filter_group_initial_PAT;
    mpe_FilterGroup             filter_group_initial_primary_PMT;
    mpe_FilterGroup             filter_group_revision_PAT;
    mpe_FilterGroup             filter_group_revision_primary_PMT;
    mpe_FilterGroup             filter_group_initial_secondary_PMT;
    mpe_FilterGroup             filter_group_revision_secondary_PMT;
    struct _sitp_psi_data_t     *psi_data;
} sitp_psi_fsm_data_t;

typedef struct _sitp_ib_tune_info_t
{
    uint32_t tuner_id;
    uint32_t frequency;
    uint32_t program_number;
    mpe_SiModulationMode modulation_mode;
    sitp_psi_last_tune_info_t last_tune_info;
} sitp_ib_tune_info_t;

// This is for legacy OOB
typedef struct _sitp_oob_info_t
{
    uint32_t ts_id;
} sitp_oob_info_t;

typedef struct _sitp_dsg_tunnel_info_t
{
    uint32_t dsg_app_id;
} sitp_dsg_tunnel_info_t;

typedef struct _sitp_hn_stream_info_t
{
    mpe_HnStreamSession hn_stream_session;
} sitp_hn_stream_info_t;

typedef union _sitp_psi_params_t
{
    sitp_ib_tune_info_t p_ib_tune_info;
    sitp_oob_info_t p_oob_info;
    sitp_dsg_tunnel_info_t p_dsg_info;
    sitp_hn_stream_info_t p_hn_stream_info;
} sitp_psi_params_t;

typedef struct _sitp_psi_data_t
{
    sitp_psi_tuner_type_e tuner_type;
    sitp_psi_tuner_state_e tuner_state;
    sitp_psi_params_t psi_params;
    sitp_psi_fsm_data_t *fsm_data;
} sitp_psi_data_t;

/*
 ******************************************************************************
 *                               Public functions
 ******************************************************************************
 */
mpe_Error add_pmt_entry(sitp_psi_fsm_data_t *fsm_data, uint16_t program_number,
        mpe_Bool primary, uint16_t program_map_pid, uint16_t version);
mpe_Error set_pmt_version(sitp_psi_fsm_data_t *fsm_data, uint16_t program_number, uint16_t version);
void add_pat_entry(sitp_psi_fsm_data_t *fsm_data, uint16_t version);
void print_psi_data(sitp_psi_data_t *psi_data);
void print_fsm_data(sitp_psi_fsm_data_t *fsm_data);

void tune_initiated(mpe_MediaTuneRequestParams *tuneRequest);
mpe_Error attachDSGTunnel(uint32_t appID);
mpe_Error detachDSGTunnel(uint32_t appID);

mpe_Error attachHnStreamSession(mpe_HnStreamSession session);
mpe_Error detachHnStreamSession(mpe_HnStreamSession session);

#ifdef __cplusplus
}
;
#endif

#endif /* _SITP_PSI_H */
