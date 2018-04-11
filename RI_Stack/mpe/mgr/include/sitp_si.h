#ifndef _SITP_SI_H
#define _SITP_SI_H
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
#include <mpe_types.h>  /* Resolve basic type references. */
#include <mpe_error.h>  /* Resolve basic error references. */

#include <mpe_dbg.h>   // need during testing as defines mpe_dbgMsg (RR)
#include "mpeos_thread.h"
#include "mpeos_filter.h"
#include "simgr.h"                            // get prototype for function mpe_siReleaseServiceEntry
#include "sitp_parse.h"

/*
 *****************************************************************************
 * #define definitions (RR)
 *****************************************************************************
 */

#define        SA_NON_CONFORMANCE_ISSUE

#define        NETWORK_INFORMATION_TABLE_ID        0xC2    // NIT_ID
#define        NETWORK_TEXT_TABLE_ID               0xC3    // NTT_ID
#define        SF_VIRTUAL_CHANNEL_TABLE_ID         0xC4    // SVCT_ID
#define        SYSTEM_TIME_TABLE_ID                0xC5    // STT_ID
#define        YES                                 1
#define        NO                                  0
//#define        NIT_PID                           0x1FFC  // Network Information Table packet identifier
//#define        SVCT_PID                          0x1FFC  // Short-form Virtual Channel table
// packet identifier
#define        MIN_VALID_PID                       0x0010    // minimum valid value for packet identifier
#define        MAX_VALID_PID                       0x1FFE    // maximum valid value for packet identifier
#define        NO_PCR_PID                          0x1FFF    // Use when no PID carries the PCR
#define        MIN_VALID_PRIVATE_STREAM_TYPE       0x88    // minimum valid value for a private stream type
#define        MAX_VALID_PRIVATE_STREAM_TYPE       0xFF    // maximum valid value for a private stream type
// ECN-1176 added additional stream types supported by OCAP (ex: H.264 etc.)
// Hence the MIN and MAX values for non-private stream types is updated
#define        MIN_VALID_NONPRIVATE_STREAM_TYPE    0x01    // minimum valid value for a non-private stream type
#define        MAX_VALID_NONPRIVATE_STREAM_TYPE    0x87    // maximum valid value for a non-private stream type
#define        MAX_OOB_SI_SECTION_LENGTH           4096    //the maximum length of a NIT or SVCT table section
#define        MAX_IB_SECTION_LENGTH               1021    //the maximum value that can be assigned to a
//PAT or PMT section_length field
#define        MAX_PROGRAM_INFO_LENGTH             1023    //the maximum value that can be assigned to a
//PMT program_info_length field
#define        MAX_ES_INFO_LENGTH                  1023    //the maximum value that can be assigned to a
//PMT es_info_length field
#define        MAX_VERSION_NUMBER                  31

#define        NO_PARSING_ERROR                    MPE_SUCCESS  //Should be equal to zero (0)
#define        LENGTH_CRC_32                       4            //length in bytes of a CRC_32
#define        LEN_OCTET1_THRU_OCTET3              3             //byte count for the 1st 3 byte in a structure,
#define        SIZE_OF_DESCRIPTOR_COUNT            1         //the size of the descriptor_count field in bytes
#define        CARRIER_DEFINITION_SUBTABLE         1
#define        MODULATION_MODE_SUBTABLE            2

#define        SOURCE_NAME_SUBTABLE                6

#define        FREQUENCY_UNITS_10KHz               10000
#define        FREQUENCY_UNITS_125KHz              125000
#define        MAX_CARRIER_FREQUENCY_INDEX         255

//#define        TRANSMISSION_SYSTEM_UNKNOWN                              0
//#define        TRANSMISSION_SYSTEM_RESERVED_ETSI                        1
//#define        TRANSMISSION_SYSTEM_ITUT_ANNEXB                          2
//#define        TRANSMISSION_SYSTEM_RESERVED_FOR_USE_IN_OTHER_SYSTEMS    3
//#define        TRANSMISSION_SYSTEM_ATSC                                 4
//#define        TRANSMISSION_SYSTEM_MIN_RESERVED_SATELLITE               5
//#define        TRANSMISSION_SYSTEM_MAX_RESERVED_SATELLITE               15


#define        INNER_CODING_MODE_5_11               0        // rate 5/11 coding
#define        INNER_CODING_MODE_1_2                1        // rate 1/2 coding
#define        INNER_CODING_MODE_3_5                3        // rate 3/5 coding
#define        INNER_CODING_MODE_2_3                5        // rate 2/3 coding
#define        INNER_CODING_MODE_3_4                7        // rate 3/4 coding
#define        INNER_CODING_MODE_4_5                8        // rate 4/5 coding
#define        INNER_CODING_MODE_5_6                9        // rate 5/6 coding
#define        INNER_CODING_MODE_7_8                11        // rate 7/8 coding
#define        DOES_NOT_USE_CONCATENATED_CODING     15        // rate 5/6 coding
#define        MODULATION_FORMAT_UNKNOWN            0
#define        MODULATION_FORMAT_QAM_1024           24

#define        VIRTUAL_CHANNEL_MAP                  0
#define        DEFINED_CHANNEL_MAP                  1
#define        INVERSE_CHANNEL_MAP                  2

#define        PATH_1_SELECTED                      0
#define        PATH_2_SELECTED                      1

#define        MPEG_2_TRANSPORT                     0
#define        NON_MPEG_2_TRANSPORT                 1

#define        NORMAL_CHANNEL                       0
#define        HIDDEN_CHANNEL                       1

#define        SNS_TABLE_SUBTYPE                    6

#define        NTSC_VIDEO_STANDARD                  0
#define        PAL625_VIDEO_STANDARD                1
#define        PAL525_VIDEO_STANDARD                2
#define        SECAM_VIDEO_STANDARD                 3
#define        MAC_VIDEO_STANDARD                   4
#define        MIN_RESERVED_VIDEO_STANDARD          5
#define        MAX_RESERVED_VIDEO_STANDARD          15
#define        IMMEDIATE_ACTIVATION                 0

#define        SIMODULE                             "SITP_SI"// module name
#define        OOB_START_UP_EVENT                   750

#define        SITP_SI_INIT_VERSION                 32            // range is 0-31
#define        SITP_SI_INIT_CRC_32                  0xFFFFFFFF    // -1
#define        NOT_USED                             0
#define        SYSTEM_STARTUP                       0
#define        TBS                                  0             // To Be Supplied
#define        MAX_TABLE_SUBTYPE                    10
#define        MAX_SECTION_NUMBER                   64

#define        CDS_ACQUIRED                         0x0001
#define        MMS_ACQUIRED                         0x0002
#define        NIT_ACQUIRED                         0x0003
#define        VCM_ACQUIRED                         0x0004
#define        DCM_ACQUIRED                         0x0008
#define        SVCT_ACQUIRED                        0x000C
#define        NIT_SVCT_ACQUIRED                    0x000F
#define        NTT_ACQUIRED                         0x0010
#define        TABLES_ACQUIRED                      0x001F
#define        SIDB_SIGNALED                        0x0020
#define        OOB_TABLES_ACQUIRED                  0x003F
#define        WAIT_FOREVER                         0
#define        PROFILE_CABLELABS_OCAP_1             1

#define        NUM_INIT_TABLES                      2

#define        SHORT_TIMER                          100

/*
 ** ***************************************************************************
 **                               enumerations
 ** ***************************************************************************
 */

typedef enum _sitp_si_profile_e
{
    SITP_SI_PROFILE_1 = 1,
    SITP_SI_PROFILE_2,
    SITP_SI_PROFILE_3,
    SITP_SI_PROFILE_4,
    SITP_SI_PROFILE_5,
    SITP_SI_PROFILE_6,
    SITP_SI_PROFILE_NONE = 0

} sitp_si_profile_e;

typedef enum _sitp_si_state_e
{
    SITP_SI_STATE_WAIT_TABLE_REVISION = 0,
    SITP_SI_STATE_WAIT_TABLE_COMPLETE,
    SITP_SI_STATE_NONE

} sitp_si_state_e;

typedef enum _sitp_si_revisioning_type_e
{
    SITP_SI_REV_TYPE_UNKNOWN = 0, SITP_SI_REV_TYPE_RDD, SITP_SI_REV_TYPE_CRC

} sitp_si_revisioning_type_e;

/*
 ** ***************************************************************************
 **                               typedefs
 ** ***************************************************************************
 ** The information needed to set filters and get sections.
 ** ***************************************************************************
 */

/*
 * A table section
 */
typedef struct _sitp_si_table_section_t
{
    mpe_Bool section_acquired;
    uint32_t crc_32;
    uint8_t crc_section_match_count;
    mpe_TimeMillis last_seen_time;
} sitp_si_table_section_t;

/*
 * structure describing a si table to be acquired
 */
typedef struct _sitp_si_table_t
{
    uint8_t table_id;
    uint8_t subtype;
    sitp_si_state_e table_state;
    sitp_si_revisioning_type_e rev_type;
    uint32_t rev_sample_count;
    mpe_Bool table_acquired;
    uint32_t table_unique_id;
    uint8_t version_number;
    uint8_t number_sections;
    mpe_TimeMillis filter_start_time;
    sitp_si_table_section_t section[MAX_SECTION_NUMBER];
} sitp_si_table_t;

/*
 *****************************************************************************
 *  global function prototypes (invoke by the outside world)
 *****************************************************************************
 */
mpe_Error sitp_si_Start(void);
void      sitp_si_cache_enabled(void);
void sitp_si_ShutDown(void);
uint32_t getTablesAcquired(void);
mpe_TimeMillis getSTTStartTime(void);
void setTablesAcquired(uint32_t tables_acquired);
uint32_t getSVCTAcquired(void);
void setSVCTAcquired(uint32_t svct_acquired);
uint32_t getDumpTables(void);
mpe_Bool isVersioningByCRC32(void);

#ifdef __cplusplus
}
;
#endif

#endif /* _SITP_SI_H */
