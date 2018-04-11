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
//        Â·Redistributions of source code must retain the above copyright notice, this list 
//             of conditions and the following disclaimer.
//        Â·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
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

#define _DUMP_APDU_C "$Rev: 141 $"

// NOTES:
//    next_* gets the next item
//    dump_* gets and dumps the next item
//    grab_* gets, dumps and returns the next item
//    test_* gets, dumps and tests the next item
//    send_* actual output functions
//
// OC-SP-BOCR-D02-080523, Bi-directional Receiver:
//    [DRI]      OC-SP-DRI2.0-D02-080523       OpenCable Digital Receiver Interface 2.0 Specification,
//                                             May 23, 2008, Cable Television Laboratories, Inc.
//    [CCCP]     OC-SP-CCCP2.0-I08-071113      OpenCable CableCARD Copy Protection 2.0 Specification,
//                                             November 13, 2007, Cable Television Laboratories, Inc.
//    [CCIF]     OC-SP-CCIF2.0-I14-080404      OpenCable CableCARD Interface 2.0 Specification,
//                                             April 4, 2008, Cable Television Laboratories, Inc.
//    [OCSEC]    OC-SP-SEC-I07-061031          OpenCable System Security Specification,
//                                             October 31, 2006, Cable Television Laboratories, Inc.
//
//    [BPI]      CM-SP-BPI+-I12-050812         Data-Over-Cable Service Interface Specifications,
//                                             Baseline Privacy Plus Interface Specification,
//                                             August 12, 2005, Cable Television Laboratories, Inc.
//    [CHILA]                                  CableLabs CableCARD-Host Interface License Agreement.
//    [DSG]      CM-SP-DSG-I11-071206          DOCSIS Set-top Gateway (DSG) Interface Specification,
//                                             December 6, 2007, Cable Television Laboratories, Inc.
//    [HOST]     OC-SP-HOST 2.1-CFR-I04-080404 OpenCable Host Device 2.1 Core Functional Requirements,
//                                             April 4, 2008, Cable Television Laboratories, Inc.
//    [MIB-HOST] OC-SP-MIB-HOST2.X-I04-080328  OpenCable Host Device 2.X MIB Specification,
//                                             March 28, 2008, Cable Television Laboratories, Inc.
//    [OCAP]     OC-SP-OCAP1.0.2-080314        OpenCable Application Platform Specification (OCAP) 1.0,
//                                             March 14, 2008, Cable Television Laboratories, Inc.
//    [OCCD]     OC-SP-CDL2.0-I06-080118       OpenCable Common Download Specification,
//                                             January 18, 2008, Cable Television Laboratories, Inc.
//    [OCUR]     OC-SP-OCUR-I06-071113         OpenCable Unidirectional Receiver Specification,
//                                             November 13, 2007, Cable Television Laboratories, Inc.
//    [OSSI]     SP-OSSIv2.0-I10-070803        Data-Over-Cable Service Interface Specifications,
//                                             Operations Support System Interface Specification,
//                                             August 3, 2007, Cable Television Laboratories, Inc.
//    [RFI]      CM-SP-RFIv2.0-I13-080215      Data-Over-Cable Service Interface Specifications,
//                                             Radio Frequency Interface Specification,
//                                             February 15, 2008, Cable Television Laboratories, Inc.
//    [eDOCSIS]  CM-SP-eDOCSIS-I14-080215      Data-Over-Cable Service Interface Specifications,
//                                             eDOCSIS Specification, February 15, 2008,
//                                             Cable Television Laboratories, Inc.
//
// OC-SP-DRI2.0-D02-080523, Digital Receiver Interface:
//    [BOCR]     OC-SP-BOCR-D02-080523         Bi-directional OpenCable Receiver,
//                                             May 23, 2008, Cable Television Laboratories, Inc.
//    [CCCP]     OC-SP-CCCP2.0-I08-071113      CableCARD Copy Protection 2.0 Specification,
//                                             November 13, 2007, Cable Television Laboratories, Inc.
//    [CCIF]     OC-SP-CCIF2.0-I14-080404      CableCARD Interface 2.0 Specification,
//                                             April 4, 2008, Cable Television Laboratories, Inc.
//
//    [DSG]      CM-SP-DSG-I11-071206          DOCSIS Set-top Gateway Interface Specification,
//                                             December 6, 2007, Cable Television Laboratories, Inc.
//    [HOST]     OC-SP-HOST2.1-CFR-I04-080404  OpenCable Host Device 2.1 Core Functional Requirements,
//                                             April 4, 2008, Cable Television Laboratories, Inc.
//    [MIB]      OC-SP-MIB-HOST2.X-I03-071113  OpenCable Host Device 2.X MIB Specification,
//                                             November 13, 2007, Cable Television Laboratories, Inc.
//    [OCAP]     OC-SP-OCAP1.0.2-080314        OpenCable Application Platform,
//                                             March 14, 2008, Cable Television Laboratories, Inc.
//    [OCUR]     OC-SP-OCUR-I06-071113         OpenCable Unidirectional Receiver,
//                                             November 13, 2007, Cable Television Laboratories, Inc.
//
// OC-SP-CCCP2.0-I08-071113, CableCARD Copy Protection:
//    [CCIF]     OC-SP-CCIF2.0-I12-071113      OpenCable CableCARD Interface 2.0 Specification,
//                                             November 13, 2007, Cable Television Laboratories, Inc.
//
// OC-SP-CCIF2.0-I14-080404, CableCARD Interface:
//    [CCCP]     OC-SP-CCCP2.0-I08-071113      OpenCable CableCARD™ Copy Protection 2.0 Specification,
//                                             November 13, 2007, Cable Television Laboratories, Inc.
//    [OCSEC]    OC-SP-SEC-I07-061031          OpenCable System Security Specification,
//                                             October 31, 2006, Cable Television Laboratories, Inc.
//
//    [CDL]      OC-SP-CDL2.0-I06-080118       OpenCable Common Download 2.0 Specification,
//                                             January 18, 2008, Cable Television Laboratories, Inc.
//    [DOCSIS]   CM-SP-RFIv2.0-I13-080215      Data-Over-Cable Service Interface Specification DOCSIS 2.0,
//                                             February 15, 2008, Cable Television Laboratories, Inc.
//    [DSG]      CM-SP-DSG-I11-071206          DOCSIS® Set-top Gateway (DSG) Interface Specification,
//                                             December 6, 2007, Cable Television Laboratories, Inc.
//    [OCAP]     OC-SP-OCAP1.0.2-080314        OpenCable Application Platform Specification,
//                                             OCAP 1.0 Profile, March 14, 2008,
//                                             Cable Television Laboratories, Inc.
//    [OCHD]     OC-SP-HOST2.1-CFR-I04-080404  OpenCable Host Device 2.1 Core Functional Requirements,
//                                             April 4, 2008, Cable Television Laboratories, Inc.
//
// OC-SP-SEC-I07-061031, System Security Specification:
//    [CCCP]     OC-SP-CCCP2.0-I04-060803      OpenCable CableCARD Copy Protection Interface Specification,
//                                             August 3, 2006, Cable Television Laboratories, Inc.
//    [CCIF]     OC-SP-CCIF2.0-I08-061031      OpenCable CableCARD 2.0 Interface Specification,
//                                             October 31, 2006, Cable Television Laboratories, Inc.
//
//    [BPI]      CM-SP-BPI+-I12-050812         Data-Over-Cable Service Interface Specifications,
//                                             Baseline Privacy Plus Interface Specification,
//                                             August 12, 2005, Cable Television Laboratories, Inc.
//    [DOCSIS]   CM-SP-RFIv2.0-I11-060602      Data-Over-Cable Service Interface Specifications,
//                                             Radio Frequency Interface Specification,
//                                             June 2, 2006, Cable Television Laboratories, Inc.
//    [OCAP]     OC-SP-OCAP1.0-I16-050803      OCAP 1.0 Specification, August 3, 2005,
//                                             Cable Television Laboratories, Inc.
//    [OCHD]     OC-SP-HOST2.0-CFR-I11-061031  OpenCable Host Device 2.0 Core Functional Requirements,
//                                             October 31, 2006, Cable Television Laboratories, Inc.

#include "DumpApdu.h"

#ifndef STAND_ALONE
#define RILOG_CATEGORY cc_RILogCategory
static log4c_category_t * cc_RILogCategory;
#define CHECK_LOGGER() {if (NULL == cc_RILogCategory) cc_RILogCategory = log4c_category_get("RI.Cablecard.utils"); }
#else
#define CHECK_LOGGER()
#endif

#define TABSET 30
#define call(f) {Error e = (f); if (e) return e; }

//   Table 9.3.3 - Application Object Tag Values
//               + OC-SP-CCCP2.0-I08-071113
//     Notes:
//         * Messages defined in EIA 679-B Part B [NRSSB]
//        ** Reserved
//        () Originally had () in the name for some reason TBD
//       *** These DSG-related APDUs are not used in Version 5 of the Extended
//           Channel Resource and are redefined in the DSG resource. For
//           versions 2-4, definitions of these DSG-related APDUs are found in
//           Annex E.
//
#define profile_inq_tag               0x9F8010 // <->     Resource Manager
#define profile_reply_tag             0x9F8011 // <->     Resource Manager
#define profile_changed_tag           0x9F8012 // <->     Resource Manager
#define application_info_req_tag      0x9F8020 // -->     Application Info
#define application_info_cnf_tag      0x9F8021 // <--     Application Info
#define server_query_tag              0x9F8022 // -->     Application Info
#define server_reply_tag              0x9F8023 // <--     Application Info
#define ca_info_inq_tag               0x9F8030 // -->     CA Support
#define ca_info_tag                   0x9F8031 // <--     CA Support
#define ca_pmt_tag                    0x9F8032 // -->     CA Support
#define ca_pmt_reply_tag              0x9F8033 // <--     CA Support
#define ca_update_tag                 0x9F8034 // <--     CA Support
#define oob_tx_tune_req_tag           0x9F8404 // <--     Host Control
#define oob_tx_tune_cnf_tag           0x9F8405 // -->     Host Control
#define oob_rx_tune_req_tag           0x9F8406 // <--     Host Control
#define oob_rx_tune_cnf_tag           0x9F8407 // -->     Host Control
#define inband_tune_req_tag           0x9F8408 // <--     Host Control
#define inband_tune_cnf_tag           0x9F8409 // -->     Host Control
#define system_time_inq_tag           0x9F8442 // <--     System Time
#define system_time_tag               0x9F8443 // -->     System Time
#define open_mmi_req_tag              0x9F8820 // <--     MMI
#define open_mmi_cnf_tag              0x9F8821 // -->     MMI
#define close_mmi_req_tag             0x9F8822 // <--     MMI
#define close_mmi_cnf_tag             0x9F8823 // -->     MMI
#define comms_cmd_tag                 0x9F8C00 // <--     Low speed comms *
#define connection_descriptor_tag     0x9F8C01 // <--     Low speed comms *
#define comms_reply_tag               0x9F8C02 // -->     Low speed comms *
#define comms_send_last_tag           0x9F8C03 // <--     Low speed comms *
#define comms_send_more_tag           0x9F8C04 // <--     Low speed comms *
#define comms_rcv_last_tag            0x9F8C05 // -->     Low speed comms *
#define comms_rcv_more_tag            0x9F8C06 // -->     Low speed comms *
#define new_flow_req_tag              0x9F8E00 // <-> --> Extended Channel Support
#define new_flow_cnf_tag              0x9F8E01 // <-> <-- Extended Channel Support
#define delete_flow_req_tag           0x9F8E02 // <-> --> Extended Channel Support
#define delete_flow_cnf_tag           0x9F8E03 // <-> <-- Extended Channel Support
#define lost_flow_ind_tag             0x9F8E04 // <-> <-- Extended Channel Support
#define lost_flow_cnf_tag             0x9F8E05 // <-> --> Extended Channel Support
#define old_inquire_dsg_mode_tag      0x9F8E06 // --> --> Extended Channel Support ***
#define old_set_dsg_mode_tag          0x9F8E07 // <-- <-- Extended Channel Support ***
#define old_dsg_error_tag             0x9F8E08 // <-- N/A Extended Channel Support ***
#define old_dsg_message_tag           0x9F8E09 // --> N/A Extended Channel Support ***
#define old_configure_advanced_tag    0x9F8E0A // <-- N/A Extended Channel Support ***
#define old_send_dcd_info_tag         0x9F8E0B // --> N/A Extended Channel Support ***
#define program_req_tag               0x9F8F00 // -->     Generic IPPV Support (Deprecated)
#define program_cnf_tag               0x9F8F01 // <--     Generic IPPV Support (Deprecated)
#define purchase_req_tag              0x9F8F02 // -->     Generic IPPV Support (Deprecated)
#define purchase_cnf_tag              0x9F8F03 // <--     Generic IPPV Support (Deprecated)
#define cancel_req_tag                0x9F8F04 // -->     Generic IPPV Support (Deprecated)
#define cancel_cnf_tag                0x9F8F05 // <--     Generic IPPV Support (Deprecated)
#define history_req_tag               0x9F8F06 // -->     Generic IPPV Support (Deprecated)
#define history_cnf_tag               0x9F8F07 // <--     Generic IPPV Support (Deprecated)
#define inquire_dsg_mode_tag          0x9F9100 // --> --> DSG
#define set_dsg_mode_tag              0x9F9101 // <-- <-- DSG
#define dsg_error_tag                 0x9F9102 // <-- N/A DSG
#define dsg_message_tag               0x9F9103 // --> N/A DSG
#define dsg_directory_tag             0x9F9104 // <-- N/A DSG
#define send_dcd_info_tag             0x9F9105 // --> N/A DSG
#define feature_list_req_tag          0x9F9802 // <->     Generic Feature Control
#define feature_list_tag              0x9F9803 // <->     Generic Feature Control
#define feature_list_cnf_tag          0x9F9804 // <->     Generic Feature Control
#define feature_list_changed_tag      0x9F9805 // <->     Generic Feature Control
#define feature_parameters_req_tag    0x9F9806 // <--     Generic Feature Control
#define feature_parameters_tag        0x9F9807 // <->     Generic Feature Control
#define features_parameters_cnf_tag   0x9F9808 // <->     Generic Feature Control
#define open_homing_tag               0x9F9990 // -->     Homing
#define homing_cancelled_tag          0x9F9991 // -->     Homing
#define open_homing_reply_tag         0x9F9992 // <--     Homing
#define homing_active_tag             0x9F9993 // -->     Homing
#define homing_complete_tag           0x9F9994 // <--     Homing
#define firmware_upgrade_tag          0x9F9995 // <--     Homing
#define firmware_upgrade_reply_tag    0x9F9996 // -->     Homing
#define firmware_upgrade_complete_tag 0x9F9997 // <--     Homing
#define sas_connect_rqst_tag          0x9F9A00 // -->     Specific Application Support
#define sas_connect_cnf_tag           0x9F9A01 // <--     Specific Application Support
#define sas_data_rqst_tag             0x9F9A02 // <->     Specific Application Support
#define sas_data_av_tag               0x9F9A03 // <->     Specific Application Support
#define sas_data_cnf_tag              0x9F9A04 // <->     Specific Application Support
#define sas_server_query_tag          0x9F9A05 // <->     Specific Application Support
#define sas_server_reply_tag          0x9F9A06 // <->     Specific Application Support
#define sas_async_msg_tag             0x9F9A07 // <->     Specific Application Support ()
#define stream_profile_tag            0x9FA010 // <--     CARD RES ()
#define stream_profile_cnf_tag        0x9FA011 // -->     CARD RES ()
#define program_profile_tag           0x9FA012 // <--     CARD RES ()
#define program_profile_cnf_tag       0x9FA013 // -->     CARD RES ()
#define es_profile_tag                0x9FA014 // <--     CARD RES ()
#define es_profile_cnf_tag            0x9FA015 // -->     CARD RES ()
#define request_pids_tag              0x9FA016 // -->     CARD RES ()
#define request_pids_cnf_tag          0x9FA017 // <--     CARD RES ()
#define asd_registration_req_tag      0x9FA200 // -->     Authorized Service Domain **
#define asd_challenge_tag             0x9FA201 // <--     Authorized Service Domain **
#define asd_challenge_rsp_tag         0x9FA202 // -->     Authorized Service Domain **
#define asd_registration_grant_tag    0x9FA203 // <--     Authorized Service Domain **
#define asd_dvr_record_req_tag        0x9FA204 // -->     Authorized Service Domain **
#define asd_dvr_record_reply_tag      0x9FA205 // <--     Authorized Service Domain **
#define asd_dvr_playback_req_tag      0x9FA206 // -->     Authorized Service Domain **
#define asd_dvr_playback_reply_tag    0x9FA207 // <--     Authorized Service Domain **
#define asd_dvr_release_req_tag       0x9FA208 // -->     Authorized Service Domain **
#define asd_dvr_release_reply_tag     0x9FA209 // <--     Authorized Service Domain **
#define asd_server_playback_req_tag   0x9FA20A // -->     Authorized Service Domain **
#define asd_server_playback_reply_tag 0x9FA20B // <--     Authorized Service Domain **
#define asd_client_playback_req_tag   0x9FA20C // -->     Authorized Service Domain **
#define asd_client_playback_reply_tag 0x9FA20D // <--     Authorized Service Domain **
#define host_info_request_tag         0x9F9C00 // <--     System Control
#define host_info_response_tag        0x9F9C01 // -->     System Control
#define code_version_table_tag        0x9F9C02 // <--     System Control
#define code_version_table_reply_tag  0x9F9C03 // -->     System Control
#define host_download_control_tag     0x9F9C04 // -->     System Control
#define host_download_command_tag     0x9F9C05 // <--     System Control (Deprecated)
#define diagnostic_req_tag            0x9FDF00 // <--     Generic Diagnostic Support
#define diagnostic_cnf_tag            0x9FDF01 // -->     Generic Diagnostic Support
#define host_reset_vector_tag         0x9F9E00 // <--     Headend Communication
#define host_reset_vector_ack_tag     0x9F9E01 // -->     Headend Communication
#define host_properties_req_tag       0x9F9F01 // -->     Host Addressable Properties
#define host_properties_reply_tag     0x9F9F02 // <--     Host Addressable Properties
#define cp_open_req_tag               0x9F9000 //         OC-SP-CCCP2.0-I08-071113
#define cp_open_cnf_tag               0x9F9001 //         OC-SP-CCCP2.0-I08-071113
#define cp_data_req_tag               0x9F9002 //         OC-SP-CCCP2.0-I08-071113 also see E.2.1.3, E.2.1.10
#define cp_data_cnf_tag               0x9F9003 //         OC-SP-CCCP2.0-I08-071113 also see E.2.1.4, E.2.1.7
#define cp_sync_req_tag               0x9F9004 //         OC-SP-CCCP2.0-I08-071113
#define cp_sync_cnf_tag               0x9F9005 //         OC-SP-CCCP2.0-I08-071113
#define cp_valid_req_tag              0x9F9006 //         OC-SP-CCCP2.0-I08-071113
#define cp_valid_cnf_tag              0x9F9007 //         OC-SP-CCCP2.0-I08-071113
#define profile_inq_num               1
#define profile_reply_num             0
#define profile_changed_num           1
#define application_info_req_num      2
#define application_info_cnf_num      0
#define server_query_num              0
#define server_reply_num              0
#define ca_info_inq_num               1
#define ca_info_num                   0
#define ca_pmt_num                    0
#define ca_pmt_reply_num              0
#define ca_update_num                 0
#define oob_tx_tune_req_num           1
#define oob_tx_tune_cnf_num           1
#define oob_rx_tune_req_num           1
#define oob_rx_tune_cnf_num           1
#define inband_tune_req_num           2
#define inband_tune_cnf_num           2
#define system_time_inq_num           1
#define system_time_num               1
#define open_mmi_req_num              0
#define open_mmi_cnf_num              1
#define close_mmi_req_num             1
#define close_mmi_cnf_num             1
#define comms_cmd_num                 0
#define connection_descriptor_num     0
#define comms_reply_num               0
#define comms_send_last_num           0
#define comms_send_more_num           0
#define comms_rcv_last_num            0
#define comms_rcv_more_num            0
#define new_flow_req_num              0
#define new_flow_cnf_num              0
#define delete_flow_req_num           1
#define delete_flow_cnf_num           1
#define lost_flow_ind_num             1
#define lost_flow_cnf_num             1
#define old_inquire_dsg_mode_num      1
#define old_set_dsg_mode_num          0
#define old_dsg_error_num             1
#define old_dsg_message_num           0
#define old_configure_advanced_num    0
#define old_send_dcd_info_num         0
#define program_req_num               0
#define program_cnf_num               0
#define purchase_req_num              0
#define purchase_cnf_num              0
#define cancel_req_num                0
#define cancel_cnf_num                0
#define history_req_num               0
#define history_cnf_num               0
#define inquire_dsg_mode_num          1
#define set_dsg_mode_num              0
#define dsg_error_num                 1
#define dsg_message_num               2
#define dsg_directory_num             0
#define send_dcd_info_num             0
#define feature_list_req_num          1
#define feature_list_num              0
#define feature_list_cnf_num          1
#define feature_list_changed_num      1
#define feature_parameters_req_num    1
#define feature_parameters_num        0
#define features_parameters_cnf_num   0
#define open_homing_num               1
#define homing_cancelled_num          1
#define open_homing_reply_num         1
#define homing_active_num             1
#define homing_complete_num           1
#define firmware_upgrade_num          0
#define firmware_upgrade_reply_num    1
#define firmware_upgrade_complete_num 1
#define sas_connect_rqst_num          1
#define sas_connect_cnf_num           1
#define sas_data_rqst_num             1
#define sas_data_av_num               1
#define sas_data_cnf_num              1
#define sas_server_query_num          1
#define sas_server_reply_num          0
#define sas_async_msg_num             0
#define stream_profile_num            1
#define stream_profile_cnf_num        1
#define program_profile_num           1
#define program_profile_cnf_num       1
#define es_profile_num                1
#define es_profile_cnf_num            1
#define request_pids_num              1
#define request_pids_cnf_num          0
#define asd_registration_req_num      0
#define asd_challenge_num             0
#define asd_challenge_rsp_num         0
#define asd_registration_grant_num    0
#define asd_dvr_record_req_num        0
#define asd_dvr_record_reply_num      0
#define asd_dvr_playback_req_num      0
#define asd_dvr_playback_reply_num    0
#define asd_dvr_release_req_num       0
#define asd_dvr_release_reply_num     0
#define asd_server_playback_req_num   0
#define asd_server_playback_reply_num 0
#define asd_client_playback_req_num   0
#define asd_client_playback_reply_num 0
#define host_info_request_num         1
#define host_info_response_num        0
#define code_version_table_num        0
#define code_version_table_reply_num  0
#define host_download_control_num     0
#define host_download_command_num     2
#define diagnostic_req_num            0
#define diagnostic_cnf_num            0
#define host_reset_vector_num         1
#define host_reset_vector_ack_num     1
#define host_properties_req_num       0
#define host_properties_reply_num     0
#define cp_open_req_num               0
#define cp_open_cnf_num               0
#define cp_data_req_num               0
#define cp_data_cnf_num               0
#define cp_sync_req_num               0
#define cp_sync_cnf_num               0
#define cp_valid_req_num              0
#define cp_valid_cnf_num              0

#define profile_inq_off               0
#define profile_reply_off             (profile_inq_off               + profile_inq_num              )
#define profile_changed_off           (profile_reply_off             + profile_reply_num            )
#define application_info_req_off      (profile_changed_off           + profile_changed_num          )
#define application_info_cnf_off      (application_info_req_off      + application_info_req_num     )
#define server_query_off              (application_info_cnf_off      + application_info_cnf_num     )
#define server_reply_off              (server_query_off              + server_query_num             )
#define ca_info_inq_off               (server_reply_off              + server_reply_num             )
#define ca_info_off                   (ca_info_inq_off               + ca_info_inq_num              )
#define ca_pmt_off                    (ca_info_off                   + ca_info_num                  )
#define ca_pmt_reply_off              (ca_pmt_off                    + ca_pmt_num                   )
#define ca_update_off                 (ca_pmt_reply_off              + ca_pmt_reply_num             )
#define oob_tx_tune_req_off           (ca_update_off                 + ca_update_num                )
#define oob_tx_tune_cnf_off           (oob_tx_tune_req_off           + oob_tx_tune_req_num          )
#define oob_rx_tune_req_off           (oob_tx_tune_cnf_off           + oob_tx_tune_cnf_num          )
#define oob_rx_tune_cnf_off           (oob_rx_tune_req_off           + oob_rx_tune_req_num          )
#define inband_tune_req_off           (oob_rx_tune_cnf_off           + oob_rx_tune_cnf_num          )
#define inband_tune_cnf_off           (inband_tune_req_off           + inband_tune_req_num          )
#define system_time_inq_off           (inband_tune_cnf_off           + inband_tune_cnf_num          )
#define system_time_off               (system_time_inq_off           + system_time_inq_num          )
#define open_mmi_req_off              (system_time_off               + system_time_num              )
#define open_mmi_cnf_off              (open_mmi_req_off              + open_mmi_req_num             )
#define close_mmi_req_off             (open_mmi_cnf_off              + open_mmi_cnf_num             )
#define close_mmi_cnf_off             (close_mmi_req_off             + close_mmi_req_num            )
#define comms_cmd_off                 (close_mmi_cnf_off             + close_mmi_cnf_num            )
#define connection_descriptor_off     (comms_cmd_off                 + comms_cmd_num                )
#define comms_reply_off               (connection_descriptor_off     + connection_descriptor_num    )
#define comms_send_last_off           (comms_reply_off               + comms_reply_num              )
#define comms_send_more_off           (comms_send_last_off           + comms_send_last_num          )
#define comms_rcv_last_off            (comms_send_more_off           + comms_send_more_num          )
#define comms_rcv_more_off            (comms_rcv_last_off            + comms_rcv_last_num           )
#define new_flow_req_off              (comms_rcv_more_off            + comms_rcv_more_num           )
#define new_flow_cnf_off              (new_flow_req_off              + new_flow_req_num             )
#define delete_flow_req_off           (new_flow_cnf_off              + new_flow_cnf_num             )
#define delete_flow_cnf_off           (delete_flow_req_off           + delete_flow_req_num          )
#define lost_flow_ind_off             (delete_flow_cnf_off           + delete_flow_cnf_num          )
#define lost_flow_cnf_off             (lost_flow_ind_off             + lost_flow_ind_num            )
#define old_inquire_dsg_mode_off      (lost_flow_cnf_off             + lost_flow_cnf_num            )
#define old_set_dsg_mode_off          (old_inquire_dsg_mode_off      + old_inquire_dsg_mode_num     )
#define old_dsg_error_off             (old_set_dsg_mode_off          + old_set_dsg_mode_num         )
#define old_dsg_message_off           (old_dsg_error_off             + old_dsg_error_num            )
#define old_configure_advanced_off    (old_dsg_message_off           + old_dsg_message_num          )
#define old_send_dcd_info_off         (old_configure_advanced_off    + old_configure_advanced_num   )
#define program_req_off               (old_send_dcd_info_off         + old_send_dcd_info_num        )
#define program_cnf_off               (program_req_off               + program_req_num              )
#define purchase_req_off              (program_cnf_off               + program_cnf_num              )
#define purchase_cnf_off              (purchase_req_off              + purchase_req_num             )
#define cancel_req_off                (purchase_cnf_off              + purchase_cnf_num             )
#define cancel_cnf_off                (cancel_req_off                + cancel_req_num               )
#define history_req_off               (cancel_cnf_off                + cancel_cnf_num               )
#define history_cnf_off               (history_req_off               + history_req_num              )
#define inquire_dsg_mode_off          (history_cnf_off               + history_cnf_num              )
#define set_dsg_mode_off              (inquire_dsg_mode_off          + inquire_dsg_mode_num         )
#define dsg_error_off                 (set_dsg_mode_off              + set_dsg_mode_num             )
#define dsg_message_off               (dsg_error_off                 + dsg_error_num                )
#define dsg_directory_off             (dsg_message_off               + dsg_message_num              )
#define send_dcd_info_off             (dsg_directory_off             + dsg_directory_num            )
#define feature_list_req_off          (send_dcd_info_off             + send_dcd_info_num            )
#define feature_list_off              (feature_list_req_off          + feature_list_req_num         )
#define feature_list_cnf_off          (feature_list_off              + feature_list_num             )
#define feature_list_changed_off      (feature_list_cnf_off          + feature_list_cnf_num         )
#define feature_parameters_req_off    (feature_list_changed_off      + feature_list_changed_num     )
#define feature_parameters_off        (feature_parameters_req_off    + feature_parameters_req_num   )
#define features_parameters_cnf_off   (feature_parameters_off        + feature_parameters_num       )
#define open_homing_off               (features_parameters_cnf_off   + features_parameters_cnf_num  )
#define homing_cancelled_off          (open_homing_off               + open_homing_num              )
#define open_homing_reply_off         (homing_cancelled_off          + homing_cancelled_num         )
#define homing_active_off             (open_homing_reply_off         + open_homing_reply_num        )
#define homing_complete_off           (homing_active_off             + homing_active_num            )
#define firmware_upgrade_off          (homing_complete_off           + homing_complete_num          )
#define firmware_upgrade_reply_off    (firmware_upgrade_off          + firmware_upgrade_num         )
#define firmware_upgrade_complete_off (firmware_upgrade_reply_off    + firmware_upgrade_reply_num   )
#define sas_connect_rqst_off          (firmware_upgrade_complete_off + firmware_upgrade_complete_num)
#define sas_connect_cnf_off           (sas_connect_rqst_off          + sas_connect_rqst_num         )
#define sas_data_rqst_off             (sas_connect_cnf_off           + sas_connect_cnf_num          )
#define sas_data_av_off               (sas_data_rqst_off             + sas_data_rqst_num            )
#define sas_data_cnf_off              (sas_data_av_off               + sas_data_av_num              )
#define sas_server_query_off          (sas_data_cnf_off              + sas_data_cnf_num             )
#define sas_server_reply_off          (sas_server_query_off          + sas_server_query_num         )
#define sas_async_msg_off             (sas_server_reply_off          + sas_server_reply_num         )
#define stream_profile_off            (sas_async_msg_off             + sas_async_msg_num            )
#define stream_profile_cnf_off        (stream_profile_off            + stream_profile_num           )
#define program_profile_off           (stream_profile_cnf_off        + stream_profile_cnf_num       )
#define program_profile_cnf_off       (program_profile_off           + program_profile_num          )
#define es_profile_off                (program_profile_cnf_off       + program_profile_cnf_num      )
#define es_profile_cnf_off            (es_profile_off                + es_profile_num               )
#define request_pids_off              (es_profile_cnf_off            + es_profile_cnf_num           )
#define request_pids_cnf_off          (request_pids_off              + request_pids_num             )
#define asd_registration_req_off      (request_pids_cnf_off          + request_pids_cnf_num         )
#define asd_challenge_off             (asd_registration_req_off      + asd_registration_req_num     )
#define asd_challenge_rsp_off         (asd_challenge_off             + asd_challenge_num            )
#define asd_registration_grant_off    (asd_challenge_rsp_off         + asd_challenge_rsp_num        )
#define asd_dvr_record_req_off        (asd_registration_grant_off    + asd_registration_grant_num   )
#define asd_dvr_record_reply_off      (asd_dvr_record_req_off        + asd_dvr_record_req_num       )
#define asd_dvr_playback_req_off      (asd_dvr_record_reply_off      + asd_dvr_record_reply_num     )
#define asd_dvr_playback_reply_off    (asd_dvr_playback_req_off      + asd_dvr_playback_req_num     )
#define asd_dvr_release_req_off       (asd_dvr_playback_reply_off    + asd_dvr_playback_reply_num   )
#define asd_dvr_release_reply_off     (asd_dvr_release_req_off       + asd_dvr_release_req_num      )
#define asd_server_playback_req_off   (asd_dvr_release_reply_off     + asd_dvr_release_reply_num    )
#define asd_server_playback_reply_off (asd_server_playback_req_off   + asd_server_playback_req_num  )
#define asd_client_playback_req_off   (asd_server_playback_reply_off + asd_server_playback_reply_num)
#define asd_client_playback_reply_off (asd_client_playback_req_off   + asd_client_playback_req_num  )
#define host_info_request_off         (asd_client_playback_reply_off + asd_client_playback_reply_num)
#define host_info_response_off        (host_info_request_off         + host_info_request_num        )
#define code_version_table_off        (host_info_response_off        + host_info_response_num       )
#define code_version_table_reply_off  (code_version_table_off        + code_version_table_num       )
#define host_download_control_off     (code_version_table_reply_off  + code_version_table_reply_num )
#define host_download_command_off     (host_download_control_off     + host_download_control_num    )
#define diagnostic_req_off            (host_download_command_off     + host_download_command_num    )
#define diagnostic_cnf_off            (diagnostic_req_off            + diagnostic_req_num           )
#define host_reset_vector_off         (diagnostic_cnf_off            + diagnostic_cnf_num           )
#define host_reset_vector_ack_off     (host_reset_vector_off         + host_reset_vector_num        )
#define host_properties_req_off       (host_reset_vector_ack_off     + host_reset_vector_ack_num    )
#define host_properties_reply_off     (host_properties_req_off       + host_properties_req_num      )
#define cp_open_req_off               (host_properties_reply_off     + host_properties_reply_num    )
#define cp_open_cnf_off               (cp_open_req_off               + cp_open_req_num              )
#define cp_data_req_off               (cp_open_cnf_off               + cp_open_cnf_num              )
#define cp_data_cnf_off               (cp_data_req_off               + cp_data_req_num              )
#define cp_sync_req_off               (cp_data_cnf_off               + cp_data_cnf_num              )
#define cp_sync_cnf_off               (cp_sync_req_off               + cp_sync_req_num              )
#define cp_valid_req_off              (cp_sync_cnf_off               + cp_sync_cnf_num              )
#define cp_valid_cnf_off              (cp_valid_req_off              + cp_valid_req_num             )

typedef struct
{
    const uint8_t * data;
    size_t next;
    size_t size;
    uint32_t length;
} Apdu;

static uint8_t legal_length_fields[] =
{ 0, // profile_inq_num               1
        // (none)  profile_reply_num             0
        0, // profile_changed_num           1
        9, 14, // application_info_req_num      2
        // (none)  application_info_cnf_num      0
        // (none)  server_query_num              0
        // (none)  server_reply_num              0
        0, // ca_info_inq_num               1
        // (none)  ca_info_num                   0
        // (none)  ca_pmt_num                    0
        // (none)  ca_pmt_reply_num              0
        // (none)  ca_update_num                 0
        4, // oob_tx_tune_req_num           1
        1, // oob_tx_tune_cnf_num           1
        3, // oob_rx_tune_req_num           1
        1, // oob_rx_tune_cnf_num           1
        3, 4, // inband_tune_req_num           2
        1, 2, // inband_tune_cnf_num           2
        1, // system_time_inq_num           1
        5, // system_time_num               1
        // (none)  open_mmi_req_num              0
        2, // open_mmi_cnf_num              1
        1, // close_mmi_req_num             1
        1, // close_mmi_cnf_num             1
        // (none)  comms_cmd_num                 0
        // (none)  connection_descriptor_num     0
        // (none)  comms_reply_num               0
        // (none)  comms_send_last_num           0
        // (none)  comms_send_more_num           0
        // (none)  comms_rcv_last_num            0
        // (none)  comms_rcv_more_num            0
        // (none)  new_flow_req_num              0
        // (none)  new_flow_cnf_num              0
        3, // delete_flow_req_num           1
        4, // delete_flow_cnf_num           1
        4, // lost_flow_ind_num             1
        4, // lost_flow_cnf_num             1
        0, // old_inquire_dsg_mode_num      1
        // (none)  old_set_dsg_mode_num          0
        1, // old_dsg_error_num             1
        // (none)  old_dsg_message_num           0
        // (none)  old_configure_advanced_num    0
        // (none)  old_send_dcd_info_num         0
        // (none)  program_req_num               0
        // (none)  program_cnf_num               0
        // (none)  purchase_req_num              0
        // (none)  purchase_cnf_num              0
        // (none)  cancel_req_num                0
        // (none)  cancel_cnf_num                0
        // (none)  history_req_num               0
        // (none)  history_cnf_num               0
        0, // inquire_dsg_mode_num          1
        // (none)  set_dsg_mode_num              0
        1, // dsg_error_num                 1
        1, 2, // dsg_message_num               2
        // (none)  dsg_directory_num             0
        // (none)  send_dcd_info_num             0
        0, // feature_list_req_num          1
        // (none)  feature_list_num              0
        0, // feature_list_cnf_num          1
        0, // feature_list_changed_num      1
        0, // feature_parameters_req_num    1
        // (none)  feature_parameters_num        0
        // (none)  features_parameters_cnf_num   0
        0, // open_homing_num               1
        0, // homing_cancelled_num          1
        0, // open_homing_reply_num         1
        0, // homing_active_num             1
        0, // homing_complete_num           1
        // (none)  firmware_upgrade_num          0
        0, // firmware_upgrade_reply_num    1
        1, // firmware_upgrade_complete_num 1
        8, // sas_connect_rqst_num          1
        9, // sas_connect_cnf_num           1
        0, // sas_data_rqst_num             1
        2, // sas_data_av_num               1
        1, // sas_data_cnf_num              1
        1, // sas_server_query_num          1
        // (none)  sas_server_reply_num          0
        // (none)  sas_async_msg_num             0
        1, // stream_profile_num            1
        1, // stream_profile_cnf_num        1
        1, // program_profile_num           1
        0, // program_profile_cnf_num       1
        1, // es_profile_num                1
        0, // es_profile_cnf_num            1
        2, // request_pids_num              1
        // (none)  request_pids_cnf_num          0
        // (none)  asd_registration_req_num      0
        // (none)  asd_challenge_num             0
        // (none)  asd_challenge_rsp_num         0
        // (none)  asd_registration_grant_num    0
        // (none)  asd_dvr_record_req_num        0
        // (none)  asd_dvr_record_reply_num      0
        // (none)  asd_dvr_playback_req_num      0
        // (none)  asd_dvr_playback_reply_num    0
        // (none)  asd_dvr_release_req_num       0
        // (none)  asd_dvr_release_reply_num     0
        // (none)  asd_server_playback_req_num   0
        // (none)  asd_server_playback_reply_num 0
        // (none)  asd_client_playback_req_num   0
        // (none)  asd_client_playback_reply_num 0
        1, // host_info_request_num         1 see section 6.2.1 of [CDL2]
        // (none)  host_info_response_num        0 see section 6.2.2 of [CDL2]
        // (none)  code_version_table_num        0 see section 6.2.3 of [CDL2]
        // (none)  code_version_table_reply_num  0 see section 6.2.4 of [CDL2]
        // (none)  host_download_control_num     0 see section 6.2.3 of [CDL2]
        4, 8, // host_download_command_num     2 (Deprecated)
        // (none)  diagnostic_req_num            0
        // (none)  diagnostic_cnf_num            0
        17, // host_reset_vector_num         1
        1, // host_reset_vector_ack_num     1
        // (none)  host_properties_req_num       0
        // (none)  host_properties_reply_num     0
        // (none)  cp_open_req_num               0
        // (none)  cp_open_cnf_num               0
        // (none)  cp_data_req_num               0
        // (none)  cp_data_cnf_num               0
        // (none)  cp_sync_req_num               0
        // (none)  cp_sync_cnf_num               0
        // (none)  cp_valid_req_num              0
        // (none)  cp_valid_cnf_num              0
        };

// BOZO make these all static later
Error dump_uint8_t_dec(Apdu * const pApdu, const char * const label,
        const char * const comment);
Error dump_uint16_t_dec(Apdu * const pApdu, const char * const label,
        const char * const comment);
Error dump_uint24_t_dec(Apdu * const pApdu, const char * const label,
        const char * const comment);
Error dump_uint32_t_dec(Apdu * const pApdu, const char * const label,
        const char * const comment);
Error dump_uint8_t_hex(Apdu * const pApdu, const char * const label,
        const char * const comment);
Error dump_uint16_t_hex(Apdu * const pApdu, const char * const label,
        const char * const comment);
Error dump_uint24_t_hex(Apdu * const pApdu, const char * const label,
        const char * const comment);
Error dump_uint32_t_hex(Apdu * const pApdu, const char * const label,
        const char * const comment);
Error dump_uint64_t_hex(Apdu * const pApdu, const char * const label,
        const char * const comment);
Error dump_uint8_t_bool(Apdu * const pApdu, const char * const label,
        const char * const comment);
Error dump_uint16_t_bool(Apdu * const pApdu, const char * const label,
        const char * const comment);
Error dump_uint24_t_bool(Apdu * const pApdu, const char * const label,
        const char * const comment);
Error dump_uint32_t_bool(Apdu * const pApdu, const char * const label,
        const char * const comment);
Error dump_uint8_t_double(Apdu * const pApdu, const char * const label,
        const char * const comment, const double mult, const double add);
Error dump_uint16_t_double(Apdu * const pApdu, const char * const label,
        const char * const comment, const double mult, const double add);
Error dump_uint24_t_double(Apdu * const pApdu, const char * const label,
        const char * const comment, const double mult, const double add);
Error dump_uint32_t_double(Apdu * const pApdu, const char * const label,
        const char * const comment, const double mult, const double add);

Error grab_resource_identifier(Apdu * const pApdu, uint32_t * const pValue);

static Error next_uint8_t(Apdu * const pApdu, uint8_t * const pUint8_t)
{
    if (pApdu->next == pApdu->size)
        return IsError;
    *pUint8_t = pApdu->data[pApdu->next++];
    return NoError;
}
static Error next_uint16_t(Apdu * const pApdu, uint16_t * const pUint16_t)
{
    uint8_t temp;
    uint16_t value = 0;
    call(next_uint8_t(pApdu, &temp));
    value = (value << 8) | temp;
    call(next_uint8_t(pApdu, &temp));
    value = (value << 8) | temp;
    *pUint16_t = value;
    return NoError;
}
static Error next_uint24_t(Apdu * const pApdu, uint24_t * const pUint24_t)
{
    uint8_t temp;
    uint24_t value = 0;
    call(next_uint8_t(pApdu, &temp));
    value = (value << 8) | temp;
    call(next_uint8_t(pApdu, &temp));
    value = (value << 8) | temp;
    call(next_uint8_t(pApdu, &temp));
    value = (value << 8) | temp;
    *pUint24_t = value;
    return NoError;
}
static Error next_uint32_t(Apdu * const pApdu, uint32_t * const pUint32_t)
{
    uint8_t temp;
    uint32_t value = 0;
    call(next_uint8_t(pApdu, &temp));
    value = (value << 8) | temp;
    call(next_uint8_t(pApdu, &temp));
    value = (value << 8) | temp;
    call(next_uint8_t(pApdu, &temp));
    value = (value << 8) | temp;
    call(next_uint8_t(pApdu, &temp));
    value = (value << 8) | temp;
    *pUint32_t = value;
    return NoError;
}
static Error next_uint64_t(Apdu * const pApdu, uint64_t * const pUint64_t)
{
    uint8_t temp;
    uint64_t value = 0;
    call(next_uint8_t(pApdu, &temp));
    value = (value << 8) | temp;
    call(next_uint8_t(pApdu, &temp));
    value = (value << 8) | temp;
    call(next_uint8_t(pApdu, &temp));
    value = (value << 8) | temp;
    call(next_uint8_t(pApdu, &temp));
    value = (value << 8) | temp;
    call(next_uint8_t(pApdu, &temp));
    value = (value << 8) | temp;
    call(next_uint8_t(pApdu, &temp));
    value = (value << 8) | temp;
    call(next_uint8_t(pApdu, &temp));
    value = (value << 8) | temp;
    call(next_uint8_t(pApdu, &temp));
    value = (value << 8) | temp;
    *pUint64_t = value;
    return NoError;
}

static void send_dec(const char * const label, const uint32_t value,
        const char * const comment)
{
    if (comment)
        RILOG_INFO("  %-*s %u %s\n", TABSET, label, value, comment);
    else
        RILOG_INFO("  %-*s %u\n", TABSET, label, value);
}
static void send_hex(const char * const label, const uint64_t value,
        const char * const comment, const uint8_t size)
{
    if (comment)
        switch (size)
        {
        case 1:
            RILOG_INFO("  %-*s 0x%02llX %s\n", TABSET, label, value, comment);
            break;
        case 2:
            RILOG_INFO("  %-*s 0x%04llX %s\n", TABSET, label, value, comment);
            break;
        case 3:
            RILOG_INFO("  %-*s 0x%06llX %s\n", TABSET, label, value, comment);
            break;
        case 4:
            RILOG_INFO("  %-*s 0x%08llX %s\n", TABSET, label, value, comment);
            break;
        case 8:
            RILOG_INFO("  %-*s 0x%016llX %s\n", TABSET, label, value, comment);
            break;
        }
    else
        switch (size)
        {
        case 1:
            RILOG_INFO("  %-*s 0x%02llX\n", TABSET, label, value);
            break;
        case 2:
            RILOG_INFO("  %-*s 0x%04llX\n", TABSET, label, value);
            break;
        case 3:
            RILOG_INFO("  %-*s 0x%06llX\n", TABSET, label, value);
            break;
        case 4:
            RILOG_INFO("  %-*s 0x%08llX\n", TABSET, label, value);
            break;
        case 8:
            RILOG_INFO("  %-*s 0x%016llX\n", TABSET, label, value);
            break;
        }
}
#define send_warning(label, value) send_hex(label, value, "WARNING - Reserved", 1);
static void send_double(const char * const label, const double value,
        const char * const comment)
{
    if (comment)
        RILOG_INFO("  %-*s %f %s\n", TABSET, label, value, comment);
    else
        RILOG_INFO("  %-*s %f\n", TABSET, label, value);
}
static void send_bool(const char * const label, const uint32_t value,
        const char * const comment)
{
    if (comment)
        switch (value)
        {
        case 0:
            RILOG_INFO("  %-*s no %s\n", TABSET, label, comment);
            break;
        case 1:
            RILOG_INFO("  %-*s yes %s\n", TABSET, label, comment);
            break;
        default:
            RILOG_INFO(
                    "  %-*s yes %s WARNING - value (%d) is not zero or one\n",
                    TABSET, label, comment, value);
        }
    else
        switch (value)
        {
        case 0:
            RILOG_INFO("  %-*s no\n", TABSET, label);
            break;
        case 1:
            RILOG_INFO("  %-*s yes\n", TABSET, label);
            break;
        default:
            RILOG_INFO("  %-*s yes WARNING - value (%d) is not zero or one\n",
                    TABSET, label, value);
        }
}
static void send_string(const char * const label, const char * const value)
{
    if (value)
        RILOG_INFO("  %-*s %s\n", TABSET, label, value);
    else
        RILOG_INFO("  %-*s\n", TABSET, label);
}
static void send_tag(const char * const label)
{
    RILOG_INFO("%s\n", label);
}

Error dump_uint8_t_dec(Apdu * const pApdu, const char * const label,
        const char * const comment)
{
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    send_dec(label, value, comment);
    return NoError;
}
Error dump_uint16_t_dec(Apdu * const pApdu, const char * const label,
        const char * const comment)
{
    uint16_t value;
    call(next_uint16_t(pApdu, &value));
    send_dec(label, value, comment);
    return NoError;
}
Error dump_uint24_t_dec(Apdu * const pApdu, const char * const label,
        const char * const comment)
{
    uint24_t value;
    call(next_uint24_t(pApdu, &value));
    send_dec(label, value, comment);
    return NoError;
}
Error dump_uint32_t_dec(Apdu * const pApdu, const char * const label,
        const char * const comment)
{
    uint32_t value;
    call(next_uint32_t(pApdu, &value));
    send_dec(label, value, comment);
    return NoError;
}

Error dump_uint8_t_hex(Apdu * const pApdu, const char * const label,
        const char * const comment)
{
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    send_hex(label, value, comment, 1);
    return NoError;
}
Error dump_uint16_t_hex(Apdu * const pApdu, const char * const label,
        const char * const comment)
{
    uint16_t value;
    call(next_uint16_t(pApdu, &value));
    send_hex(label, value, comment, 2);
    return NoError;
}
Error dump_uint24_t_hex(Apdu * const pApdu, const char * const label,
        const char * const comment)
{
    uint24_t value;
    call(next_uint24_t(pApdu, &value));
    send_hex(label, value, comment, 3);
    return NoError;
}
Error dump_uint32_t_hex(Apdu * const pApdu, const char * const label,
        const char * const comment)
{
    uint32_t value;
    call(next_uint32_t(pApdu, &value));
    send_hex(label, value, comment, 4);
    return NoError;
}
Error dump_uint64_t_hex(Apdu * const pApdu, const char * const label,
        const char * const comment)
{
    uint64_t value;
    call(next_uint64_t(pApdu, &value));
    send_hex(label, value, comment, 8);
    return NoError;
}

Error dump_uint8_t_bool(Apdu * const pApdu, const char * const label,
        const char * const comment)
{
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    send_bool(label, value, comment);
    return NoError;
}
Error dump_uint16_t_bool(Apdu * const pApdu, const char * const label,
        const char * const comment)
{
    uint16_t value;
    call(next_uint16_t(pApdu, &value));
    send_bool(label, value, comment);
    return NoError;
}
Error dump_uint24_t_bool(Apdu * const pApdu, const char * const label,
        const char * const comment)
{
    uint24_t value;
    call(next_uint24_t(pApdu, &value));
    send_bool(label, value, comment);
    return NoError;
}
Error dump_uint32_t_bool(Apdu * const pApdu, const char * const label,
        const char * const comment)
{
    uint32_t value;
    call(next_uint32_t(pApdu, &value));
    send_bool(label, value, comment);
    return NoError;
}

Error dump_uint8_t_double(Apdu * const pApdu, const char * const label,
        const char * const comment, const double mult, const double add)
{
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    send_double(label, value * mult + add, comment);
    return NoError;
}
Error dump_uint16_t_double(Apdu * const pApdu, const char * const label,
        const char * const comment, const double mult, const double add)
{
    uint16_t value;
    call(next_uint16_t(pApdu, &value));
    send_double(label, value * mult + add, comment);
    return NoError;
}
Error dump_uint24_t_double(Apdu * const pApdu, const char * const label,
        const char * const comment, const double mult, const double add)
{
    uint24_t value;
    call(next_uint24_t(pApdu, &value));
    send_double(label, value * mult + add, comment);
    return NoError;
}
Error dump_uint32_t_double(Apdu * const pApdu, const char * const label,
        const char * const comment, const double mult, const double add)
{
    uint32_t value;
    call(next_uint32_t(pApdu, &value));
    send_double(label, value * mult + add, comment);
    return NoError;
}

///////////////////////////////////////////////////
//                                               //
// OC-SP-CCIF2.0-I14-080404, CableCARD Interface //
//                                               //
///////////////////////////////////////////////////

//   Table 7.8–1 - Length field used by all PDUs at Transport, Session and Application Layers
//     Length_field() {
//       size_indicator              1 bslbf
//       if (size_indicator ==0)
//         length_value              7 uimsbf
//       else if size_indicator==1) {
//         length_field_size         7 uimsbf
//         for (i=0;
//              i<length_field_size;
//              i++) {
//           Length_value_byte       8 bslbf
//         }
//       }
//     }
static Error next_length_field(Apdu * const pApdu, uint32_t * const pValue)
{
    uint8_t temp08;
    uint16_t temp16;
    uint24_t temp24;
    uint32_t temp32;

    call(next_uint8_t(pApdu, &temp08));
    if (temp08 & 0x80)
    {
        switch (temp08 & 0x7F)
        {
        case 0:
            *pValue = 0;
            break;
        case 1:
            call(next_uint8_t (pApdu, &temp08))
            ;
            *pValue = temp08;
            break;
        case 2:
            call(next_uint16_t(pApdu, &temp16))
            ;
            *pValue = temp16;
            break;
        case 3:
            call(next_uint24_t(pApdu, &temp24))
            ;
            *pValue = temp24;
            break;
        case 4:
            call(next_uint32_t(pApdu, &temp32))
            ;
            *pValue = temp32;
            break;
        default:
            return IsError;
        }
    }
    else
        *pValue = temp08;
    return NoError;
}
static Error test_length_field(Apdu * const pApdu, const uint32_t offset,
        const uint32_t number)
{
    // If there are entries in the legal length table assume IsError else assume NoError
    Error error = number ? IsError : NoError;
    uint32_t i;
    call(next_length_field(pApdu, &pApdu->length));
    if (pApdu->length > pApdu->size - pApdu->next)
        return IsError;
    for (i = 0; i < number; i++)
    {
        if (legal_length_fields[offset + i] == pApdu->length)
        {
            error = NoError; // the length is legal
            break;
        }
    }
    if (error)
        send_dec("length_field", pApdu->length,
                (pApdu->length == 1 ? "byte, ERROR - invalid length"
                        : "bytes, ERROR - invalid length"));
    else
        send_dec("length_field", pApdu->length, (pApdu->length == 1 ? "byte"
                : "bytes"));
    return error;
}

//   Table 7.8–4 - Command TPDU (C_TPDU)
//     C_TPDU() {
//       c_tpdu_tag          8 uimsbf
//       length_field()
//       t_c_id              8 uimsbf
//       for (i=0;
//            i<length_value;
//            i++) {
//         data_byte         8 uimsbf
//       }
//     }
//
//   Table 7.8–5 - Response TPDU (R_TPDU)
//     R_TPDU() {
//       r_tpdu_tag          8 uimsbf
//       length_field()
//       t_c_id              8 uimsbf
//       for (i=0;
//            i<length_value;
//            i++) {
//         data_byte         8 uimsbf
//       }
//       SB_tag              8 uimsbf
//       length_field()        =2
//       t_c_id              8 uimsbf
//       SB_value            8 uimsbf
//     }
//
//   Table 7.8–8 - Create Transport Connection (Create_T_C)
//   Table 7.8–9 - Create Transport Connection Reply (C_T_C_Reply)
//   Table 7.8–10 - Delete Transport Connection (Delete_T_C)
//   Table 7.8–11 - Delete Transport Connection Reply (D_T_C Reply)
//   Table 7.8–12 - Request Transport Connection (Request_T_C )
//   Table 7.8–13 - New Transport Connection (New_T_C)
//   Table 7.8–14 - Transport Connection Error (T_C_Error)
//
//   Table 9.1–1 - SPDU Structure Syntax
//     SPDU() {
//       spdu_tag                    8 uimsbf
//       length_field()
//       for (i=0;
//            i<length_value;
//            i++) {
//         session_object_value_byte 8 uimsbf
//       }
//       for (i=0;
//            i<N;
//            i++) {
//         data_byte                 8 uimsbf
//       }
//     }
//
//   Table 9.1–2 - open_session_request() Syntax
//   Table 9.1–3 - open_session_response() Syntax
//   Table 9.1–4 - close_session_request() Syntax
//   Table 9.1–5 - close_session_response() Syntax
//   Table 9.1–6 - session_number() Syntax
//
//   Table 9.2–3 - resource_identifier() Syntax
//     resource_identifier() {
//       resource_id_type               2 uimsbf
//       if (resource_id_type != 0x3) {
//         resource_class              14 uimsbf
//         resource_type               10 uimsbf
//         resource_version             6 uimsbf
//       } else {
//         private_resource_definer    10 uimsbf
//         private_resource_identity   20 uimsbf
//       }
//     }
static void send_resource_identifier(const uint32_t value)
{
    send_hex("resource_identifier", value, NULL, 4);
    if ((value & 0xC0000000) != 0xC0000000)
    {
        send_hex("  resource_class", (value & 0x3FFF0000) >> 16, NULL, 2); // 14 uimsbf
        send_hex("  resource_type", (value & 0x0000FFC0) >> 6, NULL, 2); // 10 uimsbf
        send_hex("  resource_version", (value & 0x0000003F) >> 0, NULL, 1); //  6 uimsbf
    }
    else
    {
        send_hex("  private_resource_definer", (value & 0x3FF00000) >> 20,
                NULL, 2); // 10 uimsbf
        send_hex("  private_resource_identity", (value & 0x000FFFFF) >> 0,
                NULL, 2); // 20 uimsbf
    }
}
static Error next_resource_identifier(Apdu * const pApdu,
        uint32_t * const pValue)
{
    return next_uint32_t(pApdu, pValue);
}
static Error dump_resource_identifier(Apdu * const pApdu)
{
    uint32_t value;
    call(next_resource_identifier(pApdu, &value));
    send_resource_identifier(value);
    return NoError;
}
Error grab_resource_identifier(Apdu * const pApdu, uint32_t * const pValue)
{
    call(next_resource_identifier(pApdu, pValue));
    send_resource_identifier(*pValue);
    return NoError;
}

//   Table 9.4–3 - profile_inq() APDU Syntax
//     profile_inq() {
//       profile_inq_tag 24 uimsbf
//       length_field()  always = 0x00
//     }
static Error dump_profile_inq(Apdu * const pApdu)
{
    (void) pApdu;
    return NoError;
}

//   Table 9.4–4 - profile_reply() APDU Syntax
//     profile_reply() {
//       profile_reply_tag 24 uimsbf
//       length_field()
//       for (i=0; i<N; i++) {
//         resource_identifier()
//       }
//     }
static Error dump_profile_reply(Apdu * const pApdu)
{
    size_t end = pApdu->next + pApdu->length;
    while (pApdu->next < end)
    {
        call(dump_resource_identifier(pApdu));
    }
    return NoError;
}

//   Table 9.4–5 - profile_changed() APDU Syntax
//     profile_changed() {
//       profile_changed_tag 24 uimsbf
//       length_field()      always = 0x00
//     }
static Error dump_profile_changed(Apdu * const pApdu)
{
    (void) pApdu;
    return NoError;
}

//   Table 9.5–3 - application_info_req() APDU Syntax
//     application_info_req() {
//       application_info_req_tag 24 uimsbf
//       length_field()
//       display_rows             16 uimsbf
//       display_columns          16 uimsbf
//       vertical_scrolling        8 uimsbf
//       horizontal_scrolling      8 uimsbf
//       display_type_support      8 uimsbf
//       data_entry_support        8 uimsbf
//       HTML_support              8 uimsbf
//       if (HTML_support == 1) {
//         link_support            8 uimsbf
//         form_support            8 uimsbf
//         table_support           8 uimsbf
//         list_support            8 uimsbf
//         image_support           8 uimsbf
//       }
//     }
static Error dump_display_rows(Apdu * const pApdu)
{
    uint16_t value;
    call(next_uint16_t(pApdu, &value));
    send_dec("display_rows", value, (value == 1 ? "row" : "rows"));
    return NoError;
}
static Error dump_display_columns(Apdu * const pApdu)
{
    uint16_t value;
    call(next_uint16_t(pApdu, &value));
    send_dec("display_columns", value, (value == 1 ? "column" : "columns"));
    return NoError;
}

#define dump_vertical_scrolling(pApdu)   dump_uint8_t_bool(pApdu, "vertical_scrolling", NULL)
#define dump_horizontal_scrolling(pApdu) dump_uint8_t_bool(pApdu, "horizontal_scrolling", NULL)
static Error dump_display_type_support(Apdu * const pApdu)
{
    const char * const label = "display_type_support";
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    if (value == 0x00)
        send_string(label, "Full screen");
    else if (value == 0x01)
        send_string(label, "Overlay");
    else if (value <= 0x6F)
        send_dec(label, value, (value == 1 ? "window" : "windows"));
    else
        send_warning(label, value);
    return NoError;
}
static Error dump_data_entry_support(Apdu * const pApdu)
{
    const char * const label = "data_entry_support";
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    switch (value)
    {
    case 0x00:
        send_string(label, "None");
        break;
    case 0x01:
        send_string(label, "Last/Next");
        break;
    case 0x02:
        send_string(label, "Numeric Pad");
        break;
    case 0x03:
        send_string(label, "Alpha keyboard with mouse");
        break;
    default:
        send_warning(label, value)
        ;
    }
    return NoError;
}
static Error grab_HTML_support(Apdu * const pApdu,
        uint8_t * const pHTML_support)
{
    const char * const label = "HTML_support";
    call(next_uint8_t(pApdu, pHTML_support));
    switch (*pHTML_support)
    {
    case 0x00:
        send_string(label, "Baseline Profile");
        break;
    case 0x01:
        send_string(label, "Custom Profile");
        break;
    case 0x02:
        send_string(label, "HTML 3.2");
        break;
    case 0x03:
        send_string(label, "XHTML 1.0");
        break;
    default:
        send_warning(label, *pHTML_support)
        ;
    }
    return NoError;
}
static Error dump_link_support(Apdu * const pApdu)
{
    const char * const label = "link_support";
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    switch (value)
    {
    case 0x00:
        send_string(label, "One link");
        break;
    case 0x01:
        send_string(label, "Multiple links");
        break;
    default:
        send_warning(label, value)
        ;
    }
    return NoError;
}
static Error dump_form_support(Apdu * const pApdu)
{
    const char * const label = "form_support";
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    switch (value)
    {
    case 0x00:
        send_string(label, "None");
        break;
    case 0x01:
        send_string(label, "HTML 3.2 w/o POST method");
        break;
    case 0x02:
        send_string(label, "HTML 3.2");
        break;
    default:
        send_warning(label, value)
        ;
    }
    return NoError;
}
static Error dump_table_support(Apdu * const pApdu)
{
    const char * const label = "table_support";
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    switch (value)
    {
    case 0x00:
        send_string(label, "None");
        break;
    case 0x01:
        send_string(label, "HTML 3.2");
        break;
    default:
        send_warning(label, value)
        ;
    }
    return NoError;
}
static Error dump_list_support(Apdu * const pApdu)
{
    const char * const label = "list_support";
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    switch (value)
    {
    case 0x00:
        send_string(label, "None");
        break;
    case 0x01:
        send_string(label, "HTML 3.2 w/o Descriptive Lists");
        break;
    case 0x02:
        send_string(label, "HTML 3.2");
        break;
    default:
        send_warning(label, value)
        ;
    }
    return NoError;
}
static Error dump_image_support(Apdu * const pApdu)
{
    const char * const label = "image_support";
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    switch (value)
    {
    case 0x00:
        send_string(label, "None");
        break;
    case 0x01:
        send_string(label, "HTML 3.2 – PNG Picture under RGB w/o resizing");
        break;
    case 0x02:
        send_string(label, "HTML 3.2");
        break;
    default:
        send_warning(label, value)
        ;
    }
    return NoError;
}
static Error dump_application_info_req(Apdu * const pApdu)
{
    uint8_t HTML_support;
    call(dump_display_rows(pApdu));
    call(dump_display_columns(pApdu));
    call(dump_vertical_scrolling(pApdu));
    call(dump_horizontal_scrolling(pApdu));
    call(dump_display_type_support(pApdu));
    call(dump_data_entry_support(pApdu));
    call(grab_HTML_support(pApdu, &HTML_support));
    if (HTML_support == 1)
    {
        call(dump_link_support(pApdu));
        call(dump_form_support(pApdu));
        call(dump_table_support(pApdu));
        call(dump_list_support(pApdu));
        call(dump_image_support(pApdu));
    }
    return NoError;
}

static Error dump_application_info_cnf(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_server_query(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_server_reply(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

//   Table 9.7–3 - ca_info_inquiry() APDU Syntax
//     ca_info_inquiry() {
//       ca_info_inquiry_tag 24 uimsbf
//       length_field()         always = 0
//   }
static Error dump_ca_info_inq(Apdu * const pApdu)
{
    (void) pApdu;
    return NoError;
}

//   Table 9.7–4 - ca_info() APDU Syntax
//     ca_info() {
//       ca_info_tag    24 uimsbf
//       length_field()
//       for (i=0;
//            i<N;
//            i++) {
//         CA_system_id 16 uimsbf
//       }
//     }
static Error dump_ca_info(Apdu * const pApdu)
{
    size_t end = pApdu->next + pApdu->length;
    while (pApdu->next < end)
    {
        call(dump_uint16_t_hex(pApdu, "CA_system_id", NULL));
    }
    return NoError;
}

static Error dump_ca_pmt(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_ca_pmt_reply(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_ca_update(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

//   Table 9.8–3 - OOB_TX_tune_req() APDU Syntax
//     OOB_TX_tune_req() {
//       OOB_TX_tune_req_tag   24 uimsbf
//       Length_field()
//       RF_TX_frequency_value 16 uimsbf
//       RF_TX_power_level      8 uimsbf
//       RF_TX_rate_value       8 uimsbf
//     }
#define dump_RF_TX_frequency_value(pApdu) dump_uint16_t_dec(pApdu, "RF_TX_frequency_value", "kHz")
#define dump_RF_TX_power_level(pApdu)     dump_uint8_t_double(pApdu, "RF_TX_power_level", "dBmV", 0.5, 0)
static Error dump_RF_TX_rate_value(Apdu * const pApdu)
{
    const char * const label = "RF_TX_rate_value";
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    switch (value)
    {
    case 0x00:
        send_string(label, "256 kbps");
        break;
    case 0x40:
        send_string(label, "Reserved");
        break;
    case 0x80:
        send_string(label, "1544 kbps");
        break;
    case 0xC0:
        send_string(label, "3088 kbps");
        break;
    default:
        send_warning(label, value)
        ;
    }
    return NoError;
}
static Error dump_oob_tx_tune_req(Apdu * const pApdu)
{
    call(dump_RF_TX_frequency_value(pApdu));
    call(dump_RF_TX_power_level(pApdu));
    call(dump_RF_TX_rate_value(pApdu));
    return NoError;
}

//   Table 9.8–7 - OOB_TX_tune_cnf() APDU Syntax
//     OOB_TX_tune_cnf() {
//       OOB_TX_tune_cnf_tag 24 uimsbf
//       length_field()
//       status_field         8 uimsbf
//     }
static Error dump_TX_status_field(Apdu * const pApdu)
{
    const char * const label = "status_field";
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    switch (value)
    {
    case 0x00:
        send_string(label, "Tuning granted");
        break;
    case 0x01:
        send_string(label,
                "Tuning denied – RF transmitter not physically available");
        break;
    case 0x02:
        send_string(label, "Tuning denied – RF transmitter busy");
        break;
    case 0x03:
        send_string(label, "Tuning denied – Invalid parameters");
        break;
    case 0x05:
        send_string(label, "Tuning denied – Other reasons");
        break;
    default:
        send_warning(label, value)
        ;
    }
    return NoError;
}
static Error dump_oob_tx_tune_cnf(Apdu * const pApdu)
{
    call(dump_TX_status_field(pApdu));
    return NoError;
}

//   Table 9.8–8 - OOB_RX_tune_req() APDU Syntax
//     OOB_RX_tune_req() {
//       OOB_RX_tune_req_tag   24 uimsbf
//       length_field()
//       RF_RX_frequency_value 16 uimsbf
//       RF_RX_data_rate        8 uimsbf
//     }
#define dump_RF_RX_frequency_value(pApdu) dump_uint16_t_double(pApdu, "RF_RX_frequency_value", "MHz", 0.05, 50)
static Error dump_RF_RX_data_rate(Apdu * const pApdu)
{
    const char * const label = "RF_RX_data_rate";
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    switch (value)
    {
    case 0x00:
        send_string(label, "2,048 kbps, non-inverted");
        break;
    case 0x40:
        send_string(label, "2,048 kbps, non-inverted");
        break;
    case 0x80:
        send_string(label, "1,544 kbps, non-inverted");
        break;
    case 0xC0:
        send_string(label, "3,088 kbps, non-inverted");
        break;
    case 0x01:
        send_string(label, "2,048 kbps, inverted");
        break;
    case 0x41:
        send_string(label, "2,048 kbps, inverted");
        break;
    case 0x81:
        send_string(label, "1,544 kbps, inverted");
        break;
    case 0xC1:
        send_string(label, "3,088 kbps, inverted");
        break;
    default:
        send_warning(label, value)
        ;
    }
    return NoError;
}
static Error dump_oob_rx_tune_req(Apdu * const pApdu)
{
    call(dump_RF_RX_frequency_value(pApdu));
    call(dump_RF_RX_data_rate(pApdu));
    return NoError;
}

//   Table 9.8–11 - OOB_RX_tune_cnf() APDU Syntax
//     OOB_RX_tune_cnf() {
//       OOB_RX_tune_cnf_tag 24 uimsbf
//       length_field()
//       status_field         8 uimsbf
//     }
static Error dump_RX_status_field(Apdu * const pApdu)
{
    const char * const label = "status_field";
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    switch (value)
    {
    case 0x00:
        send_string(label, "Tuning granted");
        break;
    case 0x01:
        send_string(label,
                "Tuning denied – RF receiver not physically available");
        break;
    case 0x02:
        send_string(label, "Tuning denied – RF receiver busy");
        break;
    case 0x03:
        send_string(label, "Tuning denied – Invalid parameters");
        break;
    case 0x05:
        send_string(label, "Tuning denied – Other reasons");
        break;
    default:
        send_warning(label, value)
        ;
    }
    return NoError;
}
static Error dump_oob_rx_tune_cnf(Apdu * const pApdu)
{
    call(dump_RX_status_field(pApdu));
    return NoError;
}

//   Table 9.8–12 - inband_tune_req() APDU Syntax
//     inband_tune_req() {
//       inband_tune_req_tag            24 uimsbf
//       length_field()
//       tune_type                       8 uimsbf
//       if (tune_type == 0x00) {
//         source_id                    16 uimsbf
//       } else if (tune_type == 0x01) {
//         tune_frequency_value         16 uimsbf
//         modulation_value              8 uimsbf
//       }
//     }
static Error grab_tune_type(Apdu * const pApdu, uint8_t * const pTune_type)
{
    const char * const label = "tune_type";
    call(next_uint8_t(pApdu, pTune_type));
    switch (*pTune_type)
    {
    case 0x00:
        send_string(label, "Source ID");
        break;
    case 0x01:
        send_string(label, "Frequency");
        break;
    default:
        send_warning(label, *pTune_type)
        ;
    }
    return NoError;
}
#define dump_source_id(pApdu) dump_uint16_t_hex(pApdu, "source_id", NULL)
#define dump_tune_frequency_value(pApdu) dump_uint16_t_double(pApdu, "tune_frequency_value", "MHz", 0.05, 0)
static Error dump_modulation_value(Apdu * const pApdu)
{
    const char * const label = "modulation_value";
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    switch (value)
    {
    case 0x00:
        send_string(label, "64QAM");
        break;
    case 0x01:
        send_string(label, "256QAM");
        break;
    default:
        send_warning(label, value)
        ;
    }
    return NoError;
}
static Error dump_inband_tune_req(Apdu * const pApdu)
{
    uint8_t tune_type;
    call(grab_tune_type(pApdu, &tune_type));
    switch (tune_type)
    {
    case 0x00:
        call(dump_source_id(pApdu))
        ;
        break;
    case 0x01:
        call(dump_tune_frequency_value(pApdu))
        ;
        call(dump_modulation_value(pApdu))
        ;
        break;
    default:
        return IsError;
    }
    return NoError;
}

//   Table 9.8–14 - S-Mode - inband_tune_cnf() APDU Syntax (Resource Type 1 Version 3)
//     inband_tuning_cnf() {
//       inband_tuning_cnf_tag 24 uimsbf
//       length_field()
//       tune_status            8 uimsbf
//     }
//   Table 9.8–15 - M-Mode - inband_tune_cnf() APDU Syntax (Resource Type 1 Version 3)
//     inband_tuning_cnf() {
//       inband_tuning_cnf_tag 24 uimsbf
//       length_field()
//       ltsid                  8 uimsbf
//       tune_status            8 uimsbf
//     }
#define dump_ltsid(pApdu) dump_uint8_t_hex(pApdu, "ltsid", NULL)
static Error dump_tune_status(Apdu * const pApdu)
{
    const char * const label = "tune_status";
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    switch (value)
    {
    case 0x00:
        send_string(label, "Tuning accepted");
        break;
    case 0x01:
        send_string(label,
                "Invalid frequency (Host does not support this frequency)");
        break;
    case 0x02:
        send_string(label,
                "Invalid modulation (Host does not support this modulation type)");
        break;
    case 0x03:
        send_string(label, "Hardware failure (Host has hardware failure)");
        break;
    case 0x04:
        send_string(label,
                "Tuner busy (Host is not relinquishing control of inband tuner)");
        break;
    default:
        send_warning(label, value)
        ;
    }
    return NoError;
}
static Error dump_inband_tune_cnf(Apdu * const pApdu)
{
    switch (pApdu->length)
    {
    case 2:
        call(dump_ltsid(pApdu))
        ;
    case 1:
        call(dump_tune_status(pApdu))
        ;
    }
    return NoError;
}

//   Table 9.10–3 - Transmission of system_time_inq
//     system_time_inq () {
//       system_time_inq_tag 24 uimsbf
//       length_field()
//       response_interval    8 uimsbf
//     }
#define dump_response_interval(pApdu) dump_uint8_t_dec(pApdu, "response_interval", "seconds")
static Error dump_system_time_inq(Apdu * const pApdu)
{
    call(dump_response_interval(pApdu));
    return NoError;
}

//   Table 9.10–4 - system_time APDU
//     system_time() {
//       system_time_tag 24 uimsbf
//       length_field()
//       system_time     32 uimsbf
//       GPS_UTC_offset   8 uimsbf
//     }
#define dump_epoch_time(pApdu)     dump_uint32_t_hex(pApdu, "system_time", "seconds")
#define dump_GPS_UTC_offset(pApdu) dump_uint8_t_dec(pApdu, "GPS_UTC_offset", "hours (?)")
static Error dump_system_time(Apdu * const pApdu)
{
    call(dump_epoch_time(pApdu));
    call(dump_GPS_UTC_offset(pApdu));
    return NoError;
}

//   Table 9.11–3 - open_mmi_req()
//     open_mmi_req() {
//       open_mmi_req_tag  24 uimsbf
//       length_field()
//       display_type       8 uimsbf
//       url_length        16 uimsbf
//       for (i=0;
//            i<url_length;
//            i++) {
//         url_byte         8 uimsbf
//       }
//     }
static Error dump_display_type(Apdu * const pApdu)
{
    const char * const label = "display_type";
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    switch (value)
    {
    case 0x00:
        send_string(label, "Full screen");
        break;
    case 0x01:
        send_string(label, "Overlay");
        break;
    case 0x02:
        send_string(label, "New window");
        break;
    default:
        send_warning(label, value)
        ;
    }
    return NoError;
}
static Error grab_url_length(Apdu * const pApdu, uint16_t * const pUrl_length)
{
    call(next_uint16_t(pApdu, pUrl_length));
    send_dec("url_length", *pUrl_length, (*pUrl_length == 1 ? "byte" : "bytes"));
    return NoError;
}
static Error dump_url(Apdu * const pApdu, const uint16_t url_length)
{
    // TODO
    uint16_t i;
    uint8_t temp;
    for (i = 0; i < url_length; i++)
    {
        call(next_uint8_t(pApdu, &temp));
    }
    send_string("url", "(TODO)");
    return NoError;
}
static Error dump_open_mmi_req(Apdu * const pApdu)
{
    uint16_t url_length;
    call(dump_display_type(pApdu));
    call(grab_url_length(pApdu, &url_length));
    call(dump_url(pApdu, url_length));
    return NoError;
}

//   Table 9.11–4 - open_mmi_cnf
//     open_mmi_cnf() {
//       open_mmi_cnf_tag 24 uimsbf
//       length_field()
//       dialog_number     8 uimsbf
//       open_status       8 uimsbf
//     }
#define dump_dialog_number(pApdu) dump_uint8_t_dec(pApdu, "dialog_number", NULL)
static Error dump_open_status(Apdu * const pApdu)
{
    const char * const label = "open_status";
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    switch (value)
    {
    case 0x00:
        send_string(label, "OK - Dialog opened");
        break;
    case 0x01:
        send_string(label, "Request denied – Host busy");
        break;
    case 0x02:
        send_string(label, "Request denied – Display type not supported");
        break;
    case 0x03:
        send_string(label, "Request denied – No video signal");
        break;
    case 0x04:
        send_string(label, "Request denied – No more windows available");
        break;
    default:
        send_warning(label, value)
        ;
    }
    return NoError;
}
static Error dump_open_mmi_cnf(Apdu * const pApdu)
{
    call(dump_dialog_number(pApdu));
    call(dump_open_status(pApdu))
    return NoError;
}

//   Table 9.11–5 - close_mmi_req
//     close_mmi_req() {
//       close_mmi_req_tag 24 uimsbf
//       length_field()
//       dialog_number      8 uimsbf
//     }
static Error dump_close_mmi_req(Apdu * const pApdu)
{
    call(dump_dialog_number(pApdu));
    return NoError;
}

//   Table 9.11–6 - close_mmi_cnf
//     close_mmi_cnf() {
//       close_mmi_cnf_tag 24 uimsbf
//       length_field()
//       dialog_number      8 uimsbf
//     }
static Error dump_close_mmi_cnf(Apdu * const pApdu)
{
    call(dump_dialog_number(pApdu));
    return NoError;
}

static Error dump_comms_cmd(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_connection_descriptor(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_comms_reply(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_comms_send_last(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_comms_send_more(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_comms_rcv_last(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_comms_rcv_more(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_new_flow_req(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_new_flow_cnf(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

//   Table 9.14–8 - delete_flow_req APDU Syntax
//     delete_flow_req() {
//       delete_flow_req_tag 24 uimsbf
//       length_field()
//       flow_id             24 uimsbf
//     }
#define dump_flow_id(pApdu) dump_uint24_t_hex(pApdu, "flow_id", NULL)
static Error dump_delete_flow_req(Apdu * const pApdu)
{
    call(dump_flow_id(pApdu));
    return NoError;
}

//   Table 9.14–9 - delete_flow_cnf APDU Syntax
//     delete_flow_cnf() {
//       delete_flow_cnf_tag 24 uimsbf
//       length_field()
//       flow_id             24 uimsbf
//       status_field         8 uimsbf
//     }
static Error dump_delete_status_field(Apdu * const pApdu)
{
    const char * const label = "status_field";
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    switch (value)
    {
    case 0x00:
        send_string(label, "Request granted, flow deleted");
        break;
    case 0x03:
        send_string(label,
                "Request denied, network unavailable or not responding");
        break;
    case 0x04:
        send_string(label, "Request denied, network busy");
        break;
    case 0x05:
        send_string(label, "Request denied, flow_id does not exist");
        break;
    case 0x06:
        send_string(label, "Request denied, not authorized");
        break;
    default:
        send_warning(label, value)
        ;
    }
    return NoError;
}
static Error dump_delete_flow_cnf(Apdu * const pApdu)
{
    call(dump_flow_id(pApdu));
    call(dump_delete_status_field(pApdu));
    return NoError;
}

//   Table 9.14–10 - lost_flow_ind APDU Syntax
//     lost_flow_ind() {
//       lost_flow_ind_tag 24 uimsbf
//       length_field()
//       flow_id           24 uimsbf
//       reason_field       8 uimsbf
//     }
static Error dump_reason_field(Apdu * const pApdu)
{
    const char * const label = "reason_field";
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    switch (value)
    {
    case 0x00:
        send_string(label, "Unknown or unspecified reason");
        break;
    case 0x01:
        send_string(label, "IP address expiration");
        break;
    case 0x02:
        send_string(label, "Network down or busy");
        break;
    case 0x03:
        send_string(label, "Lost or revoked authorization");
        break;
    case 0x04:
        send_string(label, "Remote TCP socket closed");
        break;
    case 0x05:
        send_string(label, "Socket read error");
        break;
    case 0x06:
        send_string(label, "Socket write error");
        break;
    default:
        send_warning(label, value)
        ;
    }
    return NoError;
}
static Error dump_lost_flow_ind(Apdu * const pApdu)
{
    call(dump_flow_id(pApdu));
    call(dump_reason_field(pApdu));
    return NoError;
}

//   Table 9.14–11 - lost_flow_cnf APDU Syntax
//     lost_flow_cnf() {
//       lost_flow_cnf_tag 24 uimsbf
//       length_field()
//       flow_id           24 uimsbf
//       status_field       8 uimsbf
//     }
static Error dump_lost_status_field(Apdu * const pApdu)
{
    const char * const label = "status_field";
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    switch (value)
    {
    case 0x00:
        send_string(label, "Indication acknowledged");
        break;
    default:
        send_warning(label, value)
        ;
    }
    return NoError;
}
static Error dump_lost_flow_cnf(Apdu * const pApdu)
{
    call(dump_flow_id(pApdu));
    call(dump_lost_status_field(pApdu));
    return NoError;
}

//   Table E.8–1 - inquire_DSG_mode APDU Syntax (Type 1 Versions 2, 3, and 4)
//     inquire_DSG_mode() {
//       inquire_DSG_mode_tag 24 uimsbf
//       length_field()
//     }
static Error dump_old_inquire_dsg_mode(Apdu * const pApdu)
{
    (void) pApdu;
    return NoError;
}

static Error dump_old_set_dsg_mode(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_old_dsg_error(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_old_dsg_message(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_old_configure_advanced(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_old_send_dcd_info(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_program_req(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_program_cnf(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_purchase_req(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_purchase_cnf(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_cancel_req(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_cancel_cnf(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_history_req(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_history_cnf(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

//   Table 9.20–3 - inquire_DSG_mode APDU Syntax
//     inquire_DSG_mode() {
//       inquire_DSG_mode_tag 24 uimsbf
//       length_field()          = 0x00
//     }
static Error dump_inquire_dsg_mode(Apdu * const pApdu)
{
    (void) pApdu;
    return NoError;
}

static Error dump_set_dsg_mode(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_dsg_error(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_dsg_message(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_dsg_directory(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_send_dcd_info(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

//   Table 9.15–4 - feature_list_req APDU Syntax
//     feature_list_req() {
//       feature_list_req_tag 24 uimsbf
//       length_field()
//     }
static Error dump_feature_list_req(Apdu * const pApdu)
{
    (void) pApdu;
    return NoError;
}

//   Table 9.15–5 - feature_list APDU Syntax
//     feature_list() {
//       feature_list_tag         24 uimsbf
//       length_field()
//       number_of_features        8 uimsbf
//       for (i=0;
//            i<number_of_features;
//            i++) {
//         feature_id              8 uimsbf
//       }
//     }
static Error grab_number_of_features(Apdu * const pApdu,
        uint8_t * const pNumber_of_features)
{
    call(next_uint8_t(pApdu, pNumber_of_features));
    send_dec("number_of_features", *pNumber_of_features, (*pNumber_of_features
            == 1 ? "feature" : "features"));
    return NoError;
}
static Error dump_feature_id(Apdu * const pApdu)
{
    const char * const label = "feature_id";
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    switch (value)
    {
    case 0x01:
        send_string(label, "RF Output Channel");
        break;
    case 0x02:
        send_string(label, "Parental Control PIN");
        break;
    case 0x03:
        send_string(label, "Parental Control Settings");
        break;
    case 0x04:
        send_string(label, "IPPV PIN");
        break;
    case 0x05:
        send_string(label, "Time Zone");
        break;
    case 0x06:
        send_string(label, "Daylight Savings Control");
        break;
    case 0x07:
        send_string(label, "AC Outlet");
        break;
    case 0x08:
        send_string(label, "Language");
        break;
    case 0x09:
        send_string(label, "Rating Region");
        break;
    case 0x0A:
        send_string(label, "Reset PINS");
        break;
    case 0x0B:
        send_string(label, "Cable URL");
        break;
    case 0x0C:
        send_string(label, "EAS location code");
        break;
    case 0x0D:
        send_string(label, "VCT ID");
        break;
    case 0x0E:
        send_string(label, "Turn-on Channel");
        break;
    case 0x0F:
        send_string(label, "Terminal Association");
        break;
    case 0x10:
        send_string(label, "Download Group-ID");
        break;
    case 0x11:
        send_string(label, "Zip Code");
        break;
    default:
        send_warning(label, value)
        ;
    }
    return NoError;
}
static Error dump_feature_list(Apdu * const pApdu)
{
    uint8_t i, number_of_features;
    call(grab_number_of_features(pApdu, &number_of_features));
    for (i = 0; i < number_of_features; i++)
        call(dump_feature_id(pApdu));
    return NoError;
}

//   Table 9.15–6 - feature_list_cnf APDU Syntax
//     feature_list_cnf() {
//       feature_list_cnf_tag 24 uimsbf
//       length_field()
//     }
static Error dump_feature_list_cnf(Apdu * const pApdu)
{
    (void) pApdu;
    return NoError;
}

//   Table 9.15–7 - feature_list_changed APDU Syntax
//     feature_list_changed() {
//       feature_list_changed_tag 24 uimsbf
//       length_field()
//     }
static Error dump_feature_list_changed(Apdu * const pApdu)
{
    (void) pApdu;
    return NoError;
}

//   Table 9.15–8 - feature_parameters_req APDU Syntax
//     feature_paramters_req() {
//       feature_paramters_req_tag 24 uimsbf
//       length_field()
//     }
static Error dump_feature_parameters_req(Apdu * const pApdu)
{
    (void) pApdu;
    return NoError;
}

static Error dump_feature_parameters(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

//   Table 9.15–11 - Feature Parameters Confirm Object Syntax
//     Feature_parameters_cnf() {
//       feature_parameters_cnf_tag 24 uimsbf
//       length_field()
//       number_of_features          8 uimsbf
//       for (i=0;
//            i<number_of_features;
//            i++){
//         feature_id                8 uimsbf
//         status                    8 uimsbf
//       }
//     }
static Error dump_status(Apdu * const pApdu)
{
    const char * const label = "status";
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    switch (value)
    {
    case 0x00:
        send_string(label, "Accepted");
        break;
    case 0x01:
        send_string(label, "Denied – feature not supported");
        break;
    case 0x02:
        send_string(label, "Denied – invalid parameter");
        break;
    case 0x03:
        send_string(label, "Denied – other reason");
        break;
    default:
        send_warning(label, value)
        ;
    }
    return NoError;
}
static Error dump_features_parameters_cnf(Apdu * const pApdu)
{
    uint8_t i, number_of_features;
    call(grab_number_of_features(pApdu, &number_of_features));
    for (i = 0; i < number_of_features; i++)
    {
        call(dump_feature_id(pApdu));
        call(dump_status(pApdu));
    }
    return NoError;
}

//   Table 9.18–3 - Open Homing Object Syntax
//     open_homing() {
//       open_homing_tag 24 uimsbf
//       length_field()
//     }
static Error dump_open_homing(Apdu * const pApdu)
{
    (void) pApdu;
    return NoError;
}

//   Table 9.18–6 - Homing Cancelled Object Syntax
//     homing_cancelled() {
//       homing_cancelled_tag 24 uimsbf
//       length_field()
//     }
static Error dump_homing_cancelled(Apdu * const pApdu)
{
    (void) pApdu;
    return NoError;
}

//   Table 9.18–4 - Open Homing Reply Object Syntax
//     open_homing_reply() {
//       open_homing_reply_tag 24 uimsbf
//       length_field()
//     }
static Error dump_open_homing_reply(Apdu * const pApdu)
{
    (void) pApdu;
    return NoError;
}

//   Table 9.18–5 - Homing Active Object Syntax
//     homing_active() {
//       homing_active_tag 24 uimsbf
//       length_field()
//     }
static Error dump_homing_active(Apdu * const pApdu)
{
    (void) pApdu;
    return NoError;
}

//   Table 9.18–7 - Homing Complete Object Syntax
//     homing_complete() {
//       homing_complete_tag 24 uimsbf
//       length_field()
//     }
static Error dump_homing_complete(Apdu * const pApdu)
{
    (void) pApdu;
    return NoError;
}

static Error dump_firmware_upgrade(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

//   Table 9.18–9 - Firmware Upgrade Reply Object Syntax
//     firmware_upgrade_reply() {
//       firmware_upgrade_reply_tag 24 uimsbf
//       length_field()
//     }
static Error dump_firmware_upgrade_reply(Apdu * const pApdu)
{
    (void) pApdu;
    return NoError;
}

//   Table 9.18–10 - Firmware Upgrade Complete Object Syntax
//     Firmware_upgrade_complete() {
//       firmware_upgrade_complete_tag 24 uimsbf
//       length_field()
//       reset_request_status           8 uimsbf
//     }
static Error dump_reset_request_status(Apdu * const pApdu)
{
    const char * const label = "reset_request_status";
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    switch (value)
    {
    case 0x00:
        send_string(
                label,
                "PCMCIA reset requested – The HOST will bring RESET signal active then inactive");
        break;
    case 0x01:
        send_string(
                label,
                "Card reset requested – Host will set RS flag and begin interface initialization (S-Mode only)");
        break;
    case 0x02:
        send_string(label, "No reset required – Normal Operation continues");
        break;
    default:
        send_warning(label, value)
        ;
    }
    return NoError;
}
static Error dump_firmware_upgrade_complete(Apdu * const pApdu)
{
    call(dump_reset_request_status(pApdu));
    return NoError;
}

//   Table 9.17–3 - SAS_connect_rqst APDU Syntax
//     SAS_connect_rqst () {
//       SAS_connect_rqst_tag        24 uimsbf
//       length_field()
//       private_host_application_ID 64 uimsbf
//     }
#define dump_private_host_application_ID(pApdu) dump_uint64_t_hex(pApdu, "private_host_application_ID", NULL)
static Error dump_sas_connect_rqst(Apdu * const pApdu)
{
    call(dump_private_host_application_ID(pApdu));
    return NoError;
}

//   Table 9.17–4 - SAS_connect_cnf APDU Syntax
//     SAS_connect_cnf() {
//       SAS_connect_cnf_tag         24 uimsbf
//       length_field()
//       private_host_application_ID 64 uimsbf
//       SAS_session_status           8 uimsbf
//     }
static Error dump_SAS_session_status(Apdu * const pApdu)
{
    const char * const label = "SAS_session_status";
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    switch (value)
    {
    case 0x00:
        send_string(label, "Connection established");
        break;
    case 0x01:
        send_string(label,
                "Connection denied – no associated vendor-specific Card application found");
        break;
    case 0x02:
        send_string(label, "Connection denied – no more connections available");
        break;
    default:
        send_warning(label, value)
        ;
    }
    return NoError;
}
static Error dump_sas_connect_cnf(Apdu * const pApdu)
{
    call(dump_private_host_application_ID(pApdu));
    call(dump_SAS_session_status(pApdu));
    return NoError;
}

//   Table 9.17–5 - SAS_data_rqst APDU Syntax
//     SAS_data_rqst() {
//       SAS_data_rqst_tag 24 uimsbf
//       length_field()
//     }
static Error dump_sas_data_rqst(Apdu * const pApdu)
{
    (void) pApdu;
    return NoError;
}

//   Table 9.17–6 - SAS_data_av APDU Syntax
//     SAS_data_av() {
//       SAS_data_av_tag 24 uimsbf
//       length_field()
//       SAS_data_status 8 uimsbf
//       transaction_nb  8 uimsbf
//     }
//   SAS_data_av_tag 0x9F9A03
//   SAS_data_status Status of the available data.
//   0x00 Data available
//   0x01 Data not available
//   0x02-0xFF Reserved
static Error dump_SAS_data_status(Apdu * const pApdu)
{
    const char * const label = "SAS_data_status";
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    switch (value)
    {
    case 0x00:
        send_string(label, "Data available");
        break;
    case 0x01:
        send_string(label, "Data not available");
        break;
    default:
        send_warning(label, value)
        ;
    }
    return NoError;
}
#define dump_transaction_nb(pApdu) dump_uint8_t_dec(pApdu, "transaction_nb", NULL)
static Error dump_sas_data_av(Apdu * const pApdu)
{
    call(dump_SAS_data_status(pApdu));
    call(dump_transaction_nb(pApdu));
    return NoError;
}

//   Table 9.17–7 - SAS_data_cnf APDU Syntax
//     SAS_data_av_cnf() {
//       SAS_data_av_cnf_tag 24 uimsbf
//       length_field()
//       transaction_nb       8 uimsbf
//     }
static Error dump_sas_data_cnf(Apdu * const pApdu)
{
    call(dump_transaction_nb(pApdu));
    return NoError;
}

//   Table 9.17–8 - SAS_server_query APDU Syntax
//     SAS_server_query () {
//       SAS_server_query_tag 24 uimsbf
//       length_field()
//       transaction_nb        8 uimsbf
//     }
static Error dump_sas_server_query(Apdu * const pApdu)
{
    call(dump_transaction_nb(pApdu));
    return NoError;
}

//   Table 9.17–9 - SAS_server_reply APDU Syntax
//     SAS_server_reply() {
//       SAS_server_reply_tag  24 uimsbf
//       length_field()
//       transaction_nb         8 uimsbf
//       message_length        16 uimsbf
//       for (i=0;
//            i<message_length;
//            i++) {
//         message_byte         8 uimsbf
//       }
//     }
static Error grab_message_length(Apdu * const pApdu,
        uint16_t * const pMessage_length)
{
    call(next_uint16_t(pApdu, pMessage_length));
    send_dec("message_length", *pMessage_length,
            (*pMessage_length == 1 ? "byte" : "bytes"));
    return NoError;
}
static Error dump_message(Apdu * const pApdu, const uint16_t message_length)
{
    // TODO
    uint16_t i;
    uint8_t temp;
    for (i = 0; i < message_length; i++)
    {
        call(next_uint8_t(pApdu, &temp));
    }
    send_string("message", "(TODO)");
    return NoError;
}
static Error dump_sas_server_reply(Apdu * const pApdu)
{
    uint16_t message_length;
    call(dump_transaction_nb(pApdu));
    call(grab_message_length(pApdu, &message_length));
    call(dump_message(pApdu, message_length));
    return NoError;
}

//   Table 9.17–10 - SAS_Async Message APDU Syntax
//     SAS_async_msg() {
//       SAS_async_msg_tag     24 uimsbf
//       length_field()
//       message_nb             8 uimsbf
//       message_length        16 uimsbf
//       for (i=0;
//            i<message_length;
//            i++) {
//         message_byte         8 uimsbf
//       }
//     }
#define dump_message_nb(pApdu) dump_uint8_t_dec(pApdu, "message_nb", NULL)
static Error dump_sas_async_msg(Apdu * const pApdu)
{
    uint16_t message_length;
    call(dump_message_nb(pApdu));
    call(grab_message_length(pApdu, &message_length));
    call(dump_message(pApdu, message_length));
    return NoError;
}

//   Table 9.12–3 - stream_profile APDU Syntax
//     stream_profile() {
//       stream_profile_tag   24 uimsbf
//       length_field()
//       max_number_of_streams 8 uimsbf
//     }
static Error dump_max_number_of_streams(Apdu * const pApdu)
{
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    send_dec("max_number_of_streams", value,
            (value == 1 ? "stream" : "streams"));
    return NoError;
}
static Error dump_stream_profile(Apdu * const pApdu)
{
    call(dump_max_number_of_streams(pApdu));
    return NoError;
}

//   Table 9.12–4 - stream_profile_cnf APDU
//     stream_profile_cnf() {
//       stream_profile_cnf_tag 24 uimsbf
//       length_field()
//       number_of_streams_used  8 uimsbf
//     }
static Error dump_number_of_streams_used(Apdu * const pApdu)
{
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    send_dec("number_of_streams_used", value, (value == 1 ? "stream"
            : "streams"));
    return NoError;
}
static Error dump_stream_profile_cnf(Apdu * const pApdu)
{
    call(dump_number_of_streams_used(pApdu));
    return NoError;
}

//   Table 9.12–5 - program_profile APDU
//     program_profile() {
//       program_profile_tag   24 uimsbf
//       length_field()
//       max_number_of_programs 8 uimsbf
//     }
static Error dump_max_number_of_programs(Apdu * const pApdu)
{
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    send_dec("max_number_of_programs", value, (value == 1 ? "program"
            : "programs"));
    return NoError;
}
static Error dump_program_profile(Apdu * const pApdu)
{
    call(dump_max_number_of_programs(pApdu));
    return NoError;
}

//   Table 9.12–6 - program_profile_cnf APDU
//     program_profile_cnf() {
//       program_profile_cnf_tag 24 uimsbf
//       length_field()
//     }
static Error dump_program_profile_cnf(Apdu * const pApdu)
{
    (void) pApdu;
    return NoError;
}

//   Table 9.12–7 - es_profile APDU Syntax
//     es_profile() {
//       es_profile_tag  24 uimsbf
//       length_field()
//       max_number_of_es 8 uimsbf
//     }
static Error dump_max_number_of_es(Apdu * const pApdu)
{
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    send_dec("max_number_of_es", value, (value == 1 ? "stream" : "streams"));
    return NoError;
}
static Error dump_es_profile(Apdu * const pApdu)
{
    call(dump_max_number_of_es(pApdu));
    return NoError;
}

//   Table 9.12–8 - es_profile_cnf APDU Syntax
//     es_profile_cnf() {
//       es_profile_cnf_tag 24 uimsbf
//       length_field()
//     }
static Error dump_es_profile_cnf(Apdu * const pApdu)
{
    (void) pApdu;
    return NoError;
}

//   Table 9.12–9 - request_pids APDU
//     request_pids() {
//       request_pids_tag    24 uimsbf
//       length_field()
//       ltsid                8 uimsbf
//       pid_filtering_status 8 uimsbf
//     }
static Error dump_pid_filtering_status(Apdu * const pApdu)
{
    const char * const label = "pid_filtering_status";
    uint8_t value;
    call(next_uint8_t(pApdu, &value));
    switch (value)
    {
    case 0x00:
        send_string(label, "Host not filtering PIDs");
        break;
    case 0x01:
        send_string(label, "Host filtering PIDs");
        break;
    default:
        send_warning(label, value)
        ;
    }
    return NoError;
}
static Error dump_request_pids(Apdu * const pApdu)
{
    call(dump_ltsid(pApdu));
    call(dump_pid_filtering_status(pApdu));
    return NoError;
}

//   Table 9.12–10 - request_pids_cnf APDU
//     request_pids_cnf() {
//       request_pids_cnf_tag  24 uimsbf
//       length_field()
//       ltsid                  8 uimsbf
//       number_of_pids         8 uimsbf
//       for (i=0;
//            i<number_of_pids;
//            i++) {
//         zero                 3 uimsbf
//         pid                 13 uimsbf
//       }
//     }
static Error grab_number_of_pids(Apdu * const pApdu,
        uint8_t * const pNumber_of_pids)
{
    call(next_uint8_t(pApdu, pNumber_of_pids));
    send_dec("number_of_pids", *pNumber_of_pids, (*pNumber_of_pids == 1 ? "PID"
            : "PIDs"));
    return NoError;
}
#define dump_pid(pApdu) dump_uint16_t_hex(pApdu, "PID", NULL)
static Error dump_request_pids_cnf(Apdu * const pApdu)
{
    uint8_t i, number_of_pids;
    call(dump_ltsid(pApdu));
    call(grab_number_of_pids(pApdu, &number_of_pids));
    for (i = 0; i < number_of_pids; i++)
        call(dump_pid(pApdu));
    return NoError;
}

static Error dump_asd_registration_req(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_asd_challenge(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_asd_challenge_rsp(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_asd_registration_grant(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_asd_dvr_record_req(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_asd_dvr_record_reply(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_asd_dvr_playback_req(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_asd_dvr_playback_reply(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_asd_dvr_release_req(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_asd_dvr_release_reply(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_asd_server_playback_req(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_asd_server_playback_reply(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_asd_client_playback_req(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_asd_client_playback_reply(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_host_info_request(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_host_info_response(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_code_version_table(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_code_version_table_reply(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_host_download_control(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_host_download_command(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_diagnostic_req(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_diagnostic_cnf(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_host_reset_vector(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

//   Table 9.21–4 - host_reset_vector_ack (Type 1, Version 1)
//     host_reset_vector_ack() {
//       host_reset_vector_ack_tag 24 uimsbf
//       length_field()
//       transaction_id             8 uimsbf
//     }
#define dump_transaction_id(pApdu) dump_uint8_t_dec(pApdu, "transaction_id", NULL)
static Error dump_host_reset_vector_ack(Apdu * const pApdu)
{
    call(dump_transaction_id(pApdu));
    return NoError;
}

static Error dump_host_properties_req(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

static Error dump_host_properties_reply(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

/////////////////////////////////////////////////////////
//                                                     //
// OC-SP-CCCP2.0-I08-071113, CableCARD Copy Protection //
//                                                     //
/////////////////////////////////////////////////////////

//   11.3.1.1 CP_open_req( ) Syntax
//     This CP_open_req() APDU is issued by the Card to query the Host's ability
//     to support various copy protection systems.
//     Table 11.3-4 - Card's CP Support Request Message Syntax
//       CP_open_req () {
//         CP_open_req_tag 24 3 0x9F 9000
//         length_field()   8 1 0x00
//       }
static Error dump_cp_open_req(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

//   11.3.1.2 CP_open_cnf( ) Syntax
//     The CP_open_cnf() APDU is issued by the Host in response to the
//     CP_open_req() APDU. If System 2 is not supported, the Card will treat the
//     Host as if its Device Certificate was invalid.
//     Table 11.3-5 - Host's CP Support Confirm Message Syntax
//       CP_open_cnf () {
//         CP_open_cnf_tag      24 3 0x9F 9001
//         length_field()        8 1 0x04
//         CP_system_id_bitmask 32 4 Values are listed in Table 11.3-6
//       }
//     Table 11.3-6 - CP_system_id_bitmask Values
//       CP_system_id_bitmask Bit Number Description
//       -------------------- ---------- -------------------
//       System 1             0          reserved
//       System 2             1          CableCARD-CP System
//       System 3             2          reserved
//       System 4             3          reserved
//       System 5             4          reserved
//       For an example, if bit number 0, 1 and 3 are set to 1, it means that
//       Host has the capability of supporting System 1, System 2, and System 4.
static Error dump_cp_open_cnf(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

//   11.4.1.1 Card Authentication Data Message
//     The Card issues the CP_data_req() APDU to send Card_DevCert,
//     Card_ManCert, DH_pubKeyC and SIGNC to the Host and request the Host's
//     authentication data.
//     Table 11.4-2 - Card Authentication Data Message Syntax
//       CP_data_req () {
//         CP_data_req_tag                 24      3 0x9F 9002
//         length_field()                  24      3 0x82 1113
//         CP_system_id                     8      1 0x02
//         Send_datatype_nbr                8      1 0x04
//         for (i=0;
//              i<Send_datatype_nbr;
//              i++) {                    (96)   (12)
//           Datatype_id                    8      1 i = 0, Datatype_id = 16 (Card_DevCert)
//                                          8      1 i = 1, Datatype_id =  8 (Card_ManCert)
//                                          8      1 i = 2, Datatype_id = 14 (DH_pubKeyC)
//                                          8      1 i = 3, Datatype_id = 18 (SIGNC)
//           Datatype_length               16      2 i = 0, Datatype_length = 2048
//                                         16      2 i = 1, Datatype_length = 2048
//                                         16      2 i = 2, Datatype_length =  128
//                                         16      2 i = 3, Datatype_length =  128
//           for (j=0;
//                j<Datatype_length;
//                j++) {               (34816) (4352)
//             Data_type                16384   2048 i = 0, Data_type = Card_DevCert
//                                      16384   2048 i = 1, Data_type = Card _ManCert
//                                       1024    128 i = 2, Data_type = DH_pubKeyC
//                                       1024    128 i = 3, Data_type = SIGNC
//           }
//         }
//         Request_datatype_nbr             8      1 0x04
//         for (i=0;
//              i<Request_datatype_nbr;
//              i++) {                    (32)    (4)
//           Datatype_id                    8      1 i = 0, Datatype_id = 15 (Host_DevCert)
//                                          8      1 i = 1, Datatype_id =  7 (Host_ManCert)
//                                          8      1 i = 2, Datatype_id = 13 (DH_pubKeyH)
//                                          8      1 i = 3, Datatype_id = 17 (SIGNH)
//         }
//       }
//   11.4.2.1 Card Request for Host AuthKey Message
//     The Card issues the CP_data_req() APDU with Datatype_id = 22 to request
//     the Host's authentication key (AuthKeyH).
//     Table 11.4-5 - Card Request for Host AuthKey Message Syntax
//       CP_data_req () {
//         CP_data_req_tag             24 3 0x9F 9002
//         length_field()               8 1 0x04
//         CP_system_id                 8 1 0x02
//         Send_datatype_nbr            8 1 0x00
//         Request_datatype_nbr         8 1 0x01
//         for (i=0;
//              i<Request_datatype_nbr;
//              i++) {
//           Datatype_id                8 1 Datatype_id = 22 (AuthKeyH)
//         }
//       }
//   11.5.2 Card CPKey Generation Message
//     The Card issues the CP_data_req() APDU to send Card_ID and N_Card and
//     request Host_ID and N_Host.
//     Table 11.5-2 - Card CPKey Generation Message Syntax
//       CP_data_req () {
//         CP_data_req_tag               24    3 0x9F 9002
//         length_field()                 8    1 0x1B
//         CP_system_id                   8    1 0x02
//         Send_datatype_nbr              8    1 0x02
//         for (i=0;
//              i<Send_datatype_nbr;
//              i++) {                  (48)  (6)
//           Datatype_id                  8    1 i = 0, Datatype_id = 6 (Card_ID)
//                                        8    1 i = 1, Datatype_id = 12 (N_Card)
//           Datatype_length             16    2 i = 0, Datatype_length = 8
//                                       16    2 i = 1, Datatype_length = 8
//           for (j=0;
//                j<Datatype_length;
//                j++) {               (128) (16)
//             Data_type                 64    8 j = 0, Data_type = Card_ID
//                                       64    8 j = 1, Data_type = N_Card;
//           }
//         }
//         Request_datatype_nbr           8    1 0x02
//         for (i=0;
//              i<Request_datatype_nbr;
//              i++) {                  (16)  (2)
//           Datatype_id                  8    1 i = 0, Datatype_id = 5 (Host_ID)
//                                        8    1 i = 1, Datatype_id = 11 (N_Host)
//         }
//       }
//   11.7.2 Card CCI Challenge Message
//     The Card generates a nonce (CCI_N_Card) and sends the CP_data_req() APDU
//     to the Host along with program_number (and LTSID if operating in M-Mode).
//     Table 11.7-2 - Card's CCI Challenge Message Syntax
//       CP_data_req() {
//         CP_data_req_tag             24 3 0x9F 9002.
//         length_field()               8 1 For S-Mode value = 0x15
//                                          For M-Mode value = 0x1A
//         CP_system_id                 8 1 0x02
//         Send_datatype_nbr            8 1 For S-Mode value = 2
//                                          For M-Mode value = 3
//         for (i=0;
//              i<Send_datatype_nbr;
//              i++) {
//           Datatype_id                8 1 i = 0, Datatype_id = 24 (CCI_N_Card)
//                                      8 1 i = 1, Datatype_id = 26 (program_number)
//                                      8 1 i = 2, Datatype_id = 29 (LTSID) [Only for M-Mode]
//           Datatype_length           16 2 i = 0, Datatype_length = 8
//                                     16 2 i = 1, Datatype_length = 2
//                                     16 2 i = 2, Datatype_length = 1 [Only for M-Mode]
//           for (j=0;
//                j<Datatype_length;
//                j++) {
//             Data_type               64 8 i = 0, Data_type = CCI_N_Card
//                                     16 2 i = 1, Data_type = program_number
//                                      8 1 i = 2, Data_type = f LTSID [Only for M-Mode]
//           }
//         }
//         Request_datatype_nbr         8 1 For S-Mode value = 2
//                                          For M-Mode value = 3
//         for (i=0;
//              i<Request_datatype_nbr;
//              i++) {
//           Datatype_id                8 1 i=0, Datatype_id = 19 (CCI_N_Host)
//                                      8 1 i=1, Datatype_id = 26 (program_number)
//                                      8 1 i=2, Datatype_id = 29 (LTSID) [Only for M-Mode]
//         }
//       }
//   11.7.4 CCI Delivery Message
//     The Card calculates a message digest (CCI_auth) using the CCI byte,
//     CPKey(s), program number, CCI_N_Card, CCI_N_Host (and LTSID if operating
//     in M-Mode) and sends it to the Host with the CCI byte using the
//     CP_data_req() APDU.
//     Table 11.7-4 - CCI Delivery Message Syntax
//       CP_data_req() {
//         CP_data_req_tag              24  3 0x9F 9002.
//         length_field()                8  1 For S-Mode value = 0x25
//                                            For M-Mode value = 0x2A
//         CP_system_id                  8  1 0x02
//         Send_datatype_nbr             8  1 For S-Mode value = 3
//                                            For M-Mode value = 4
//         for (i=0;
//              i<Send_datatype_nbr;
//              i++) {
//           Datatype_id                 8  1 i = 0, Datatype_id = 25 (CCI_data)
//                                       8  1 i = 1, Datatype_id =  2 (program_number)
//                                       8  1 i = 2, Datatype_id = 27 (CCI_auth)
//                                       8  1 i = 3, Datatype_id = 29 (LTSID) [Only for M-Mode]
//           Datatype_length            16  2 i = 0, Datatype_length =  1
//                                      16  2 i = 1, Datatype_length =  2
//                                      16  2 i = 2, Datatype_length = 20
//                                      16  2 i = 3, Datatype_length =  1 [Only for M-Mode]
//           for (j=0;
//                j<Datatype_length;
//                j++) {
//             Data_type                 8  1 i = 0, Data_type = CCI_data
//                                      16  2 i = 1, Data_type = program_number
//                                     160 20 i = 2, Data_type = CCI_auth
//                                       8  1 i = 3, Data_type = LTSID [Only for M-Mode]
//           }
//         }
//         Request_datatype_nbr          8  1 For S-Mode value = 2
//                                            For M-Mode value = 3
//         for (i=0;
//              i<Request_datatype_nbr;
//              i++) {
//           Datatype_id                 8  1 i=0, Datatype_id = 28 (CCI_ack)
//                                       8  1 i=1, Datatype_id = 26 (program_number)
//                                       8  1 i=2, Datatype_id = 29 (LTSID) [Only for M-Mode]
//         }
//       }
static Error dump_cp_data_req(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

//   11.4.1.2 Host Authentication Data Message
//     The Host issues the CP_data_cnf() APDU in response to the CP_data_req()
//     APDU to send Host_DevCert, Host_ManCert, DH_pubKeyH and SIGNH to the Card.
//       Table 11.4-3 - Host Authentication Data Message Syntax
//         CP_data_cnf () {
//           CP_data_cnf_tag              24      3 0x9F 9003
//           length_field()               24      3 0x82 110E
//           CP_system_id                  8      1 0x02
//           Send_datatype_nbr             8      1 0x04
//           for (i=0;
//                i<Send_datatype_nbr;
//                i++) {                 (96)   (12)
//             Datatype_id                 8      1 i = 0, Datatype_id = 15 (Host_DevCert)
//                                         8      1 i = 1, Datatype_id =  7 (Host_ManCert)
//                                         8      1 i = 2, Datatype_id = 13 (DH_pubKeyH )
//                                         8      1 i = 3, Datatype_id = 17 (SIGNH )
//             Datatype_length            16      2 i = 0, Datatype_length = 2048
//                                        16      2 i = 1, Datatype_length = 2048
//                                        16      2 i = 2, Datatype_length =  128
//                                        16      2 i = 3, Datatype_length =  128
//             for (j=0;
//                  j<Datatype_length;
//                  j++) {            (34816) (4352)
//               Data_type             16384   2048 i = 0, Data_type = Host_DevCert
//                                     16384   2048 i = 1, Data_type = Host_ManCert
//                                      1024    128 i = 2, Data_type = DH_pubKeyH
//                                      1024    128 i = 3, Data_type = SIGNH
//             }
//           }
//         }
//   11.4.2.2 Reply Message with Host AuthKey
//     The Host issues the CP_data_cnf() APDU with Datatype_id = 22 to send its
//     authentication key (AuthKeyH) to the Card.
//     Table 11.4-6 - Host Reply with AuthKey Message Syntax
//       CP_data_cnf () {
//         CP_data_cnf_tag            24    3 0x9F 9003
//         length_field()              8    1 0x19
//         CP_system_id                8    1 0x02
//         Send_datatype_nbr           8    1 0x01
//         for (i=0;
//              i<Send_datatype_nbr;
//              i++) {              (184) (23)
//           Datatype_id               8    1 Datatype_id = 22 (AuthKeyH)
//           Datatype_length          16    2 Datatype_length = 20
//           for (j=0;
//                j<Datatype_length;
//                j++) {
//             Data_type             160   20 Data_type = AuthKeyH
//           }
//         }
//       }
//   11.5.3 Host CPKey Generation Message
//     The Host issues the CP_data_cnf() APDU in response to the CP_data_req()
//     APDU to send Host_ID and N_Host.
//     Table 11.5-3 - Host's CPKey Generation Message Syntax
//       CP_data_cnf () {
//         CP_data_cnf_tag            24    3 0x9F 9003
//         length_field()              8    1 0x15
//         CP_system_id                8    1 0x02
//         Send_datatype_nbr           8    1 0x02
//         for (i=0;
//              i<Send_datatype_nbr;
//              i++) {               (48)  (6)
//           Datatype_id               8    1 i = 0, Datatype_id= 5 (Host_ID)
//                                     8    1 i = 1, Datatype_id=11 (N_Host)
//           Datatype_length          16    2 i = 0, Datatype_length = 5
//                                    16    2 i = 1, Datatype_length = 8
//           for (j=0;
//                j<Datatype_length;
//                j++) {            (104) (13)
//             Data_type              40    5 i = 0, Data_type = Host_ID
//                                    64    8 i = 1, Data_type = N_Host
//           }
//         }
//       }
//   11.7.3 Host CCI Response Message
//     The Host generates a nonce (CCI_N_Host) and sends the CP_data_cnf() APDU
//     to the Card along with program number (and LTSID if operating in M-Mode).
//     Table 11.7-3 - Host's CCI Response Message Syntax
//       CP_data_cnf() {
//         CP_data_cnf_tag          24 3 0x9F 9003
//         length_field()            8 1 For S-Mode value = 0x12
//                                       For M-Mode value = 0x16
//         CP_system_id              8 1 0x02.
//         Send_datatype_nbr         8 1 For S-Mode value = 2
//                                       For M-Mode value = 3
//         for (i=0;
//              i<Send_datatype_nbr;
//              i++) {
//           Datatype_id             8 1 i = 0, Datatype_id = 19 (CCI_N_Host)
//                                   8 1 i = 1, Datatype_id = 26 (program_number)
//                                   8 1 i = 2, Datatype_id = 29 (LTSID) [Only for M-Mode]
//           Datatype_length        16 2 i = 0, Datatype_length = 8
//                                  16 2 i = 1, Datatype_length = 2
//                                  16 2 i = 2, Datatype_length = 1 [Only for M-Mode]
//           for (j=0;
//                j<Datatype_length;
//                j++) {
//             Data_type            64 8 i = 0, Data_type = CCI_N_Host
//                                  16 2 i = 1, Data_type = program_number
//                                   8 1 i = 2, Data_type = LTSID [Only for M-Mode]
//           }
//         }
//       }
//   11.7.5 CCI Acknowledgement Message
//     The Host authenticates the Card's message digest (CCI_auth), calculates
//     a new message digest (CCI_ack), and sends it to the Card using the
//     CP_data_cnf() APDU.
//     Table 11.7-5 - CCI Acknowledgement Message Syntax
//       CP_data_cnf(){
//         CP_data_cnf_tag           24  3 0x9F 9003.
//         length_field()             8  1 For S-Mode value = 0x1E
//                                         For M-Mode value = 0x22
//         CP_system_id               8  1 0x02
//         Send_datatype_nbr          8  1 For S-Mode value = 2
//                                         For M-Mode value = 3
//         for (i=0;
//              i<Send_datatype_nbr;
//              i++) {
//           Datatype_id              8  1 i = 0, Datatype_id = 28 (CCI_ack)
//                                    8  1 i = 1, Datatype_id = 26 (program_number)
//                                    8  1 i = 2, Datatype_id = 29 (LTSID) [For M-Mode only]
//           Datatype_length         16  2 i = 0, Datatype_length = 20
//                                   16  2 i = 1, Datatype_length =  2
//                                   16  2 i = 2, Datatype_length =  1 [For M-Mode only]
//           for (j=0;
//                j<Datatype_length;
//                j++) {
//             Data_type            160 20 i = 0, Data_type = CCI_ack
//                                   16  2 i = 1, Data_type = program_number
//                                    8  1 i = 2, Data_type = LTSID [For M-Mode only]
//           }
//         }
//       }
static Error dump_cp_data_cnf(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

//   11.6.2 Card CPKey Ready Message
//     The Card issues the CP_sync_req() APDU to notify the Host of its
//     intention to start CP-encryption of protected MPEG programs based on the
//     derived CPKey.
//     Table 11.6-2 - Card CPKey Ready Message Syntax
//       CP_sync_ req () {
//         CP_sync_req_tag 24 3 0x9F 9004
//         length_field()   8 1 0x00
//       }
static Error dump_cp_sync_req(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

//   11.6.3 Host CPKey Ready Message
//     The Host sends the CP_sync_cnf() APDU in response to the CP_sync_req()
//     APDU.
//     Table 11.6-3 - Host CPKey Ready Message Syntax
//       CP_sync_cnf () {
//         CP_sync_cnf_tag 24 3 0x9F 9005
//         length_field()   8 1 0x01
//         Status_field     8 1 Values are listed in Table 11.6-4
//       }
//     Status_field reports the status of the CP_sync_req(). The Host will set
//     status-field to 0x00 when it is ready to receive the incoming stream or
//     otherwise as indicated in Table 11.6-4.
//     Table 11.6-4 - Host Status_field Value
//       Status_field         Value
//       ------------         -----
//       OK                   0x00
//       Error, No CP support 0x01
//       Error, Host Busy     0x02
//       Reserved             0x03 to 0xFF
static Error dump_cp_sync_cnf(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

//   Table 11.8-2 - Host Validation Status Request Message Syntax (type 4 ver 3)
//     CP_valid req () {
//       CP_valid_req_tag 24 3 0x9F 9006
//       length_field() 8 1 0x00
//     }
static Error dump_cp_valid_req(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

//   Table 11.8-3 - Card Validation Status Reply Message Syntax (type 4 ver 3)
//     CP_valid_cnf () {
//       CP_valid_cnf_tag 24 3 0x9F 9007
//       length_field() 8 1 0x01
//       Status_field 8 1 Values are listed in Table 11.8-4
//     }
//     Table 11.8-4 - Card Validation Status_field Value
//       Status_field                          Value
//       ------------------------------------- -----
//       Card is busy with binding
//         authentication process              0x00
//       Not bound for Card reasons            0x01
//       Not bound,
//         Host Certificate Invalid            0x02
//       Not bound,
//         failed to verify Host's SIGNH      0x03
//       Not bound,
//         failed to match AuthKey from Host   0x04
//       Binding Failed, other reasons         0x05
//       Not Validated,
//         Binding Authentication Complete,
//         Validation message not received yet 0x07
//       Validated,
//         validation message is received,
//         authenticated,
//         and the IDs match those in
//         the current binding                 0x06
//       Not Validated,
//         validation revoked                  0x08
//       Reserved                              0x09 to 0xFF
static Error dump_cp_valid_cnf(Apdu * const pApdu)
{
    uint8_t temp;
    size_t now = pApdu->next;
    while (pApdu->next < now + pApdu->length)
        call(next_uint8_t(pApdu, &temp));
    return NoError;
}

//   Table 9.3–1 - APDU Structure Syntax
//     APDU() {
//       apdu_tag            24 uimsbf
//       length_field()
//       for (i=0;
//            i<length_value;
//            i++) {
//         data_byte          8 uimsbf
//       }
//     }
static Error dump_one_apdu(Apdu * const pApdu)
{
    uint24_t apdu_tag;
    call(next_uint24_t(pApdu, &apdu_tag));
    switch (apdu_tag)
    {
    case profile_inq_tag:
        send_tag("profile_inq              ");
        call(test_length_field(pApdu, profile_inq_off , profile_inq_num ))
        ;
        call(dump_profile_inq (pApdu))
        ;
        break;
    case profile_reply_tag:
        send_tag("profile_reply            ");
        call(test_length_field(pApdu, profile_reply_off , profile_reply_num ))
        ;
        call(dump_profile_reply (pApdu))
        ;
        break;
    case profile_changed_tag:
        send_tag("profile_changed          ");
        call(test_length_field(pApdu, profile_changed_off , profile_changed_num ))
        ;
        call(dump_profile_changed (pApdu))
        ;
        break;
    case application_info_req_tag:
        send_tag("application_info_req     ");
        call(test_length_field(pApdu, application_info_req_off , application_info_req_num ))
        ;
        call(dump_application_info_req (pApdu))
        ;
        break;
    case application_info_cnf_tag:
        send_tag("application_info_cnf     ");
        call(test_length_field(pApdu, application_info_cnf_off , application_info_cnf_num ))
        ;
        call(dump_application_info_cnf (pApdu))
        ;
        break;
    case server_query_tag:
        send_tag("server_query             ");
        call(test_length_field(pApdu, server_query_off , server_query_num ))
        ;
        call(dump_server_query (pApdu))
        ;
        break;
    case server_reply_tag:
        send_tag("server_reply             ");
        call(test_length_field(pApdu, server_reply_off , server_reply_num ))
        ;
        call(dump_server_reply (pApdu))
        ;
        break;
    case ca_info_inq_tag:
        send_tag("ca_info_inq              ");
        call(test_length_field(pApdu, ca_info_inq_off , ca_info_inq_num ))
        ;
        call(dump_ca_info_inq (pApdu))
        ;
        break;
    case ca_info_tag:
        send_tag("ca_info                  ");
        call(test_length_field(pApdu, ca_info_off , ca_info_num ))
        ;
        call(dump_ca_info (pApdu))
        ;
        break;
    case ca_pmt_tag:
        send_tag("ca_pmt                   ");
        call(test_length_field(pApdu, ca_pmt_off , ca_pmt_num ))
        ;
        call(dump_ca_pmt (pApdu))
        ;
        break;
    case ca_pmt_reply_tag:
        send_tag("ca_pmt_reply             ");
        call(test_length_field(pApdu, ca_pmt_reply_off , ca_pmt_reply_num ))
        ;
        call(dump_ca_pmt_reply (pApdu))
        ;
        break;
    case ca_update_tag:
        send_tag("ca_update                ");
        call(test_length_field(pApdu, ca_update_off , ca_update_num ))
        ;
        call(dump_ca_update (pApdu))
        ;
        break;
    case oob_tx_tune_req_tag:
        send_tag("oob_tx_tune_req          ");
        call(test_length_field(pApdu, oob_tx_tune_req_off , oob_tx_tune_req_num ))
        ;
        call(dump_oob_tx_tune_req (pApdu))
        ;
        break;
    case oob_tx_tune_cnf_tag:
        send_tag("oob_tx_tune_cnf          ");
        call(test_length_field(pApdu, oob_tx_tune_cnf_off , oob_tx_tune_cnf_num ))
        ;
        call(dump_oob_tx_tune_cnf (pApdu))
        ;
        break;
    case oob_rx_tune_req_tag:
        send_tag("oob_rx_tune_req          ");
        call(test_length_field(pApdu, oob_rx_tune_req_off , oob_rx_tune_req_num ))
        ;
        call(dump_oob_rx_tune_req (pApdu))
        ;
        break;
    case oob_rx_tune_cnf_tag:
        send_tag("oob_rx_tune_cnf          ");
        call(test_length_field(pApdu, oob_rx_tune_cnf_off , oob_rx_tune_cnf_num ))
        ;
        call(dump_oob_rx_tune_cnf (pApdu))
        ;
        break;
    case inband_tune_req_tag:
        send_tag("inband_tune_req          ");
        call(test_length_field(pApdu, inband_tune_req_off , inband_tune_req_num ))
        ;
        call(dump_inband_tune_req (pApdu))
        ;
        break;
    case inband_tune_cnf_tag:
        send_tag("inband_tune_cnf          ");
        call(test_length_field(pApdu, inband_tune_cnf_off , inband_tune_cnf_num ))
        ;
        call(dump_inband_tune_cnf (pApdu))
        ;
        break;
    case system_time_inq_tag:
        send_tag("system_time_inq          ");
        call(test_length_field(pApdu, system_time_inq_off , system_time_inq_num ))
        ;
        call(dump_system_time_inq (pApdu))
        ;
        break;
    case system_time_tag:
        send_tag("system_time              ");
        call(test_length_field(pApdu, system_time_off , system_time_num ))
        ;
        call(dump_system_time (pApdu))
        ;
        break;
    case open_mmi_req_tag:
        send_tag("open_mmi_req             ");
        call(test_length_field(pApdu, open_mmi_req_off , open_mmi_req_num ))
        ;
        call(dump_open_mmi_req (pApdu))
        ;
        break;
    case open_mmi_cnf_tag:
        send_tag("open_mmi_cnf             ");
        call(test_length_field(pApdu, open_mmi_cnf_off , open_mmi_cnf_num ))
        ;
        call(dump_open_mmi_cnf (pApdu))
        ;
        break;
    case close_mmi_req_tag:
        send_tag("close_mmi_req            ");
        call(test_length_field(pApdu, close_mmi_req_off , close_mmi_req_num ))
        ;
        call(dump_close_mmi_req (pApdu))
        ;
        break;
    case close_mmi_cnf_tag:
        send_tag("close_mmi_cnf            ");
        call(test_length_field(pApdu, close_mmi_cnf_off , close_mmi_cnf_num ))
        ;
        call(dump_close_mmi_cnf (pApdu))
        ;
        break;
    case comms_cmd_tag:
        send_tag("comms_cmd                ");
        call(test_length_field(pApdu, comms_cmd_off , comms_cmd_num ))
        ;
        call(dump_comms_cmd (pApdu))
        ;
        break;
    case connection_descriptor_tag:
        send_tag("connection_descriptor    ");
        call(test_length_field(pApdu, connection_descriptor_off , connection_descriptor_num ))
        ;
        call(dump_connection_descriptor (pApdu))
        ;
        break;
    case comms_reply_tag:
        send_tag("comms_reply              ");
        call(test_length_field(pApdu, comms_reply_off , comms_reply_num ))
        ;
        call(dump_comms_reply (pApdu))
        ;
        break;
    case comms_send_last_tag:
        send_tag("comms_send_last          ");
        call(test_length_field(pApdu, comms_send_last_off , comms_send_last_num ))
        ;
        call(dump_comms_send_last (pApdu))
        ;
        break;
    case comms_send_more_tag:
        send_tag("comms_send_more          ");
        call(test_length_field(pApdu, comms_send_more_off , comms_send_more_num ))
        ;
        call(dump_comms_send_more (pApdu))
        ;
        break;
    case comms_rcv_last_tag:
        send_tag("comms_rcv_last           ");
        call(test_length_field(pApdu, comms_rcv_last_off , comms_rcv_last_num ))
        ;
        call(dump_comms_rcv_last (pApdu))
        ;
        break;
    case comms_rcv_more_tag:
        send_tag("comms_rcv_more           ");
        call(test_length_field(pApdu, comms_rcv_more_off , comms_rcv_more_num ))
        ;
        call(dump_comms_rcv_more (pApdu))
        ;
        break;
    case new_flow_req_tag:
        send_tag("new_flow_req             ");
        call(test_length_field(pApdu, new_flow_req_off , new_flow_req_num ))
        ;
        call(dump_new_flow_req (pApdu))
        ;
        break;
    case new_flow_cnf_tag:
        send_tag("new_flow_cnf             ");
        call(test_length_field(pApdu, new_flow_cnf_off , new_flow_cnf_num ))
        ;
        call(dump_new_flow_cnf (pApdu))
        ;
        break;
    case delete_flow_req_tag:
        send_tag("delete_flow_req          ");
        call(test_length_field(pApdu, delete_flow_req_off , delete_flow_req_num ))
        ;
        call(dump_delete_flow_req (pApdu))
        ;
        break;
    case delete_flow_cnf_tag:
        send_tag("delete_flow_cnf          ");
        call(test_length_field(pApdu, delete_flow_cnf_off , delete_flow_cnf_num ))
        ;
        call(dump_delete_flow_cnf (pApdu))
        ;
        break;
    case lost_flow_ind_tag:
        send_tag("lost_flow_ind            ");
        call(test_length_field(pApdu, lost_flow_ind_off , lost_flow_ind_num ))
        ;
        call(dump_lost_flow_ind (pApdu))
        ;
        break;
    case lost_flow_cnf_tag:
        send_tag("lost_flow_cnf            ");
        call(test_length_field(pApdu, lost_flow_cnf_off , lost_flow_cnf_num ))
        ;
        call(dump_lost_flow_cnf (pApdu))
        ;
        break;
    case old_inquire_dsg_mode_tag:
        send_tag("old_inquire_dsg_mode     ");
        call(test_length_field(pApdu, old_inquire_dsg_mode_off , old_inquire_dsg_mode_num ))
        ;
        call(dump_old_inquire_dsg_mode (pApdu))
        ;
        break;
    case old_set_dsg_mode_tag:
        send_tag("old_set_dsg_mode         ");
        call(test_length_field(pApdu, old_set_dsg_mode_off , old_set_dsg_mode_num ))
        ;
        call(dump_old_set_dsg_mode (pApdu))
        ;
        break;
    case old_dsg_error_tag:
        send_tag("old_dsg_error            ");
        call(test_length_field(pApdu, old_dsg_error_off , old_dsg_error_num ))
        ;
        call(dump_old_dsg_error (pApdu))
        ;
        break;
    case old_dsg_message_tag:
        send_tag("old_dsg_message          ");
        call(test_length_field(pApdu, old_dsg_message_off , old_dsg_message_num ))
        ;
        call(dump_old_dsg_message (pApdu))
        ;
        break;
    case old_configure_advanced_tag:
        send_tag("old_configure_advanced   ");
        call(test_length_field(pApdu, old_configure_advanced_off , old_configure_advanced_num ))
        ;
        call(dump_old_configure_advanced (pApdu))
        ;
        break;
    case old_send_dcd_info_tag:
        send_tag("old_send_dcd_info        ");
        call(test_length_field(pApdu, old_send_dcd_info_off , old_send_dcd_info_num ))
        ;
        call(dump_old_send_dcd_info (pApdu))
        ;
        break;
    case program_req_tag:
        send_tag("program_req              ");
        call(test_length_field(pApdu, program_req_off , program_req_num ))
        ;
        call(dump_program_req (pApdu))
        ;
        break;
    case program_cnf_tag:
        send_tag("program_cnf              ");
        call(test_length_field(pApdu, program_cnf_off , program_cnf_num ))
        ;
        call(dump_program_cnf (pApdu))
        ;
        break;
    case purchase_req_tag:
        send_tag("purchase_req             ");
        call(test_length_field(pApdu, purchase_req_off , purchase_req_num ))
        ;
        call(dump_purchase_req (pApdu))
        ;
        break;
    case purchase_cnf_tag:
        send_tag("purchase_cnf             ");
        call(test_length_field(pApdu, purchase_cnf_off , purchase_cnf_num ))
        ;
        call(dump_purchase_cnf (pApdu))
        ;
        break;
    case cancel_req_tag:
        send_tag("cancel_req               ");
        call(test_length_field(pApdu, cancel_req_off , cancel_req_num ))
        ;
        call(dump_cancel_req (pApdu))
        ;
        break;
    case cancel_cnf_tag:
        send_tag("cancel_cnf               ");
        call(test_length_field(pApdu, cancel_cnf_off , cancel_cnf_num ))
        ;
        call(dump_cancel_cnf (pApdu))
        ;
        break;
    case history_req_tag:
        send_tag("history_req              ");
        call(test_length_field(pApdu, history_req_off , history_req_num ))
        ;
        call(dump_history_req (pApdu))
        ;
        break;
    case history_cnf_tag:
        send_tag("history_cnf              ");
        call(test_length_field(pApdu, history_cnf_off , history_cnf_num ))
        ;
        call(dump_history_cnf (pApdu))
        ;
        break;
    case inquire_dsg_mode_tag:
        send_tag("inquire_dsg_mode         ");
        call(test_length_field(pApdu, inquire_dsg_mode_off , inquire_dsg_mode_num ))
        ;
        call(dump_inquire_dsg_mode (pApdu))
        ;
        break;
    case set_dsg_mode_tag:
        send_tag("set_dsg_mode             ");
        call(test_length_field(pApdu, set_dsg_mode_off , set_dsg_mode_num ))
        ;
        call(dump_set_dsg_mode (pApdu))
        ;
        break;
    case dsg_error_tag:
        send_tag("dsg_error                ");
        call(test_length_field(pApdu, dsg_error_off , dsg_error_num ))
        ;
        call(dump_dsg_error (pApdu))
        ;
        break;
    case dsg_message_tag:
        send_tag("dsg_message              ");
        call(test_length_field(pApdu, dsg_message_off , dsg_message_num ))
        ;
        call(dump_dsg_message (pApdu))
        ;
        break;
    case dsg_directory_tag:
        send_tag("dsg_directory            ");
        call(test_length_field(pApdu, dsg_directory_off , dsg_directory_num ))
        ;
        call(dump_dsg_directory (pApdu))
        ;
        break;
    case send_dcd_info_tag:
        send_tag("send_dcd_info            ");
        call(test_length_field(pApdu, send_dcd_info_off , send_dcd_info_num ))
        ;
        call(dump_send_dcd_info (pApdu))
        ;
        break;
    case feature_list_req_tag:
        send_tag("feature_list_req         ");
        call(test_length_field(pApdu, feature_list_req_off , feature_list_req_num ))
        ;
        call(dump_feature_list_req (pApdu))
        ;
        break;
    case feature_list_tag:
        send_tag("feature_list             ");
        call(test_length_field(pApdu, feature_list_off , feature_list_num ))
        ;
        call(dump_feature_list (pApdu))
        ;
        break;
    case feature_list_cnf_tag:
        send_tag("feature_list_cnf         ");
        call(test_length_field(pApdu, feature_list_cnf_off , feature_list_cnf_num ))
        ;
        call(dump_feature_list_cnf (pApdu))
        ;
        break;
    case feature_list_changed_tag:
        send_tag("feature_list_changed     ");
        call(test_length_field(pApdu, feature_list_changed_off , feature_list_changed_num ))
        ;
        call(dump_feature_list_changed (pApdu))
        ;
        break;
    case feature_parameters_req_tag:
        send_tag("feature_parameters_req   ");
        call(test_length_field(pApdu, feature_parameters_req_off , feature_parameters_req_num ))
        ;
        call(dump_feature_parameters_req (pApdu))
        ;
        break;
    case feature_parameters_tag:
        send_tag("feature_parameters       ");
        call(test_length_field(pApdu, feature_parameters_off , feature_parameters_num ))
        ;
        call(dump_feature_parameters (pApdu))
        ;
        break;
    case features_parameters_cnf_tag:
        send_tag("features_parameters_cnf  ");
        call(test_length_field(pApdu, features_parameters_cnf_off , features_parameters_cnf_num ))
        ;
        call(dump_features_parameters_cnf (pApdu))
        ;
        break;
    case open_homing_tag:
        send_tag("open_homing              ");
        call(test_length_field(pApdu, open_homing_off , open_homing_num ))
        ;
        call(dump_open_homing (pApdu))
        ;
        break;
    case homing_cancelled_tag:
        send_tag("homing_cancelled         ");
        call(test_length_field(pApdu, homing_cancelled_off , homing_cancelled_num ))
        ;
        call(dump_homing_cancelled (pApdu))
        ;
        break;
    case open_homing_reply_tag:
        send_tag("open_homing_reply        ");
        call(test_length_field(pApdu, open_homing_reply_off , open_homing_reply_num ))
        ;
        call(dump_open_homing_reply (pApdu))
        ;
        break;
    case homing_active_tag:
        send_tag("homing_active            ");
        call(test_length_field(pApdu, homing_active_off , homing_active_num ))
        ;
        call(dump_homing_active (pApdu))
        ;
        break;
    case homing_complete_tag:
        send_tag("homing_complete          ");
        call(test_length_field(pApdu, homing_complete_off , homing_complete_num ))
        ;
        call(dump_homing_complete (pApdu))
        ;
        break;
    case firmware_upgrade_tag:
        send_tag("firmware_upgrade         ");
        call(test_length_field(pApdu, firmware_upgrade_off , firmware_upgrade_num ))
        ;
        call(dump_firmware_upgrade (pApdu))
        ;
        break;
    case firmware_upgrade_reply_tag:
        send_tag("firmware_upgrade_reply   ");
        call(test_length_field(pApdu, firmware_upgrade_reply_off , firmware_upgrade_reply_num ))
        ;
        call(dump_firmware_upgrade_reply (pApdu))
        ;
        break;
    case firmware_upgrade_complete_tag:
        send_tag("firmware_upgrade_complete");
        call(test_length_field(pApdu, firmware_upgrade_complete_off, firmware_upgrade_complete_num))
        ;
        call(dump_firmware_upgrade_complete(pApdu))
        ;
        break;
    case sas_connect_rqst_tag:
        send_tag("sas_connect_rqst         ");
        call(test_length_field(pApdu, sas_connect_rqst_off , sas_connect_rqst_num ))
        ;
        call(dump_sas_connect_rqst (pApdu))
        ;
        break;
    case sas_connect_cnf_tag:
        send_tag("sas_connect_cnf          ");
        call(test_length_field(pApdu, sas_connect_cnf_off , sas_connect_cnf_num ))
        ;
        call(dump_sas_connect_cnf (pApdu))
        ;
        break;
    case sas_data_rqst_tag:
        send_tag("sas_data_rqst            ");
        call(test_length_field(pApdu, sas_data_rqst_off , sas_data_rqst_num ))
        ;
        call(dump_sas_data_rqst (pApdu))
        ;
        break;
    case sas_data_av_tag:
        send_tag("sas_data_av              ");
        call(test_length_field(pApdu, sas_data_av_off , sas_data_av_num ))
        ;
        call(dump_sas_data_av (pApdu))
        ;
        break;
    case sas_data_cnf_tag:
        send_tag("sas_data_cnf             ");
        call(test_length_field(pApdu, sas_data_cnf_off , sas_data_cnf_num ))
        ;
        call(dump_sas_data_cnf (pApdu))
        ;
        break;
    case sas_server_query_tag:
        send_tag("sas_server_query         ");
        call(test_length_field(pApdu, sas_server_query_off , sas_server_query_num ))
        ;
        call(dump_sas_server_query (pApdu))
        ;
        break;
    case sas_server_reply_tag:
        send_tag("sas_server_reply         ");
        call(test_length_field(pApdu, sas_server_reply_off , sas_server_reply_num ))
        ;
        call(dump_sas_server_reply (pApdu))
        ;
        break;
    case sas_async_msg_tag:
        send_tag("sas_async_msg            ");
        call(test_length_field(pApdu, sas_async_msg_off , sas_async_msg_num ))
        ;
        call(dump_sas_async_msg (pApdu))
        ;
        break;
    case stream_profile_tag:
        send_tag("stream_profile           ");
        call(test_length_field(pApdu, stream_profile_off , stream_profile_num ))
        ;
        call(dump_stream_profile (pApdu))
        ;
        break;
    case stream_profile_cnf_tag:
        send_tag("stream_profile_cnf       ");
        call(test_length_field(pApdu, stream_profile_cnf_off , stream_profile_cnf_num ))
        ;
        call(dump_stream_profile_cnf (pApdu))
        ;
        break;
    case program_profile_tag:
        send_tag("program_profile          ");
        call(test_length_field(pApdu, program_profile_off , program_profile_num ))
        ;
        call(dump_program_profile (pApdu))
        ;
        break;
    case program_profile_cnf_tag:
        send_tag("program_profile_cnf      ");
        call(test_length_field(pApdu, program_profile_cnf_off , program_profile_cnf_num ))
        ;
        call(dump_program_profile_cnf (pApdu))
        ;
        break;
    case es_profile_tag:
        send_tag("es_profile               ");
        call(test_length_field(pApdu, es_profile_off , es_profile_num ))
        ;
        call(dump_es_profile (pApdu))
        ;
        break;
    case es_profile_cnf_tag:
        send_tag("es_profile_cnf           ");
        call(test_length_field(pApdu, es_profile_cnf_off , es_profile_cnf_num ))
        ;
        call(dump_es_profile_cnf (pApdu))
        ;
        break;
    case request_pids_tag:
        send_tag("request_pids             ");
        call(test_length_field(pApdu, request_pids_off , request_pids_num ))
        ;
        call(dump_request_pids (pApdu))
        ;
        break;
    case request_pids_cnf_tag:
        send_tag("request_pids_cnf         ");
        call(test_length_field(pApdu, request_pids_cnf_off , request_pids_cnf_num ))
        ;
        call(dump_request_pids_cnf (pApdu))
        ;
        break;
    case asd_registration_req_tag:
        send_tag("asd_registration_req     ");
        call(test_length_field(pApdu, asd_registration_req_off , asd_registration_req_num ))
        ;
        call(dump_asd_registration_req (pApdu))
        ;
        break;
    case asd_challenge_tag:
        send_tag("asd_challenge            ");
        call(test_length_field(pApdu, asd_challenge_off , asd_challenge_num ))
        ;
        call(dump_asd_challenge (pApdu))
        ;
        break;
    case asd_challenge_rsp_tag:
        send_tag("asd_challenge_rsp        ");
        call(test_length_field(pApdu, asd_challenge_rsp_off , asd_challenge_rsp_num ))
        ;
        call(dump_asd_challenge_rsp (pApdu))
        ;
        break;
    case asd_registration_grant_tag:
        send_tag("asd_registration_grant   ");
        call(test_length_field(pApdu, asd_registration_grant_off , asd_registration_grant_num ))
        ;
        call(dump_asd_registration_grant (pApdu))
        ;
        break;
    case asd_dvr_record_req_tag:
        send_tag("asd_dvr_record_req       ");
        call(test_length_field(pApdu, asd_dvr_record_req_off , asd_dvr_record_req_num ))
        ;
        call(dump_asd_dvr_record_req (pApdu))
        ;
        break;
    case asd_dvr_record_reply_tag:
        send_tag("asd_dvr_record_reply     ");
        call(test_length_field(pApdu, asd_dvr_record_reply_off , asd_dvr_record_reply_num ))
        ;
        call(dump_asd_dvr_record_reply (pApdu))
        ;
        break;
    case asd_dvr_playback_req_tag:
        send_tag("asd_dvr_playback_req     ");
        call(test_length_field(pApdu, asd_dvr_playback_req_off , asd_dvr_playback_req_num ))
        ;
        call(dump_asd_dvr_playback_req (pApdu))
        ;
        break;
    case asd_dvr_playback_reply_tag:
        send_tag("asd_dvr_playback_reply   ");
        call(test_length_field(pApdu, asd_dvr_playback_reply_off , asd_dvr_playback_reply_num ))
        ;
        call(dump_asd_dvr_playback_reply (pApdu))
        ;
        break;
    case asd_dvr_release_req_tag:
        send_tag("asd_dvr_release_req      ");
        call(test_length_field(pApdu, asd_dvr_release_req_off , asd_dvr_release_req_num ))
        ;
        call(dump_asd_dvr_release_req (pApdu))
        ;
        break;
    case asd_dvr_release_reply_tag:
        send_tag("asd_dvr_release_reply    ");
        call(test_length_field(pApdu, asd_dvr_release_reply_off , asd_dvr_release_reply_num ))
        ;
        call(dump_asd_dvr_release_reply (pApdu))
        ;
        break;
    case asd_server_playback_req_tag:
        send_tag("asd_server_playback_req  ");
        call(test_length_field(pApdu, asd_server_playback_req_off , asd_server_playback_req_num ))
        ;
        call(dump_asd_server_playback_req (pApdu))
        ;
        break;
    case asd_server_playback_reply_tag:
        send_tag("asd_server_playback_reply");
        call(test_length_field(pApdu, asd_server_playback_reply_off, asd_server_playback_reply_num))
        ;
        call(dump_asd_server_playback_reply(pApdu))
        ;
        break;
    case asd_client_playback_req_tag:
        send_tag("asd_client_playback_req  ");
        call(test_length_field(pApdu, asd_client_playback_req_off , asd_client_playback_req_num ))
        ;
        call(dump_asd_client_playback_req (pApdu))
        ;
        break;
    case asd_client_playback_reply_tag:
        send_tag("asd_client_playback_reply");
        call(test_length_field(pApdu, asd_client_playback_reply_off, asd_client_playback_reply_num))
        ;
        call(dump_asd_client_playback_reply(pApdu))
        ;
        break;
    case host_info_request_tag:
        send_tag("host_info_request        ");
        call(test_length_field(pApdu, host_info_request_off , host_info_request_num ))
        ;
        call(dump_host_info_request (pApdu))
        ;
        break;
    case host_info_response_tag:
        send_tag("host_info_response       ");
        call(test_length_field(pApdu, host_info_response_off , host_info_response_num ))
        ;
        call(dump_host_info_response (pApdu))
        ;
        break;
    case code_version_table_tag:
        send_tag("code_version_table       ");
        call(test_length_field(pApdu, code_version_table_off , code_version_table_num ))
        ;
        call(dump_code_version_table (pApdu))
        ;
        break;
    case code_version_table_reply_tag:
        send_tag("code_version_table_reply ");
        call(test_length_field(pApdu, code_version_table_reply_off , code_version_table_reply_num ))
        ;
        call(dump_code_version_table_reply (pApdu))
        ;
        break;
    case host_download_control_tag:
        send_tag("host_download_control    ");
        call(test_length_field(pApdu, host_download_control_off , host_download_control_num ))
        ;
        call(dump_host_download_control (pApdu))
        ;
        break;
    case host_download_command_tag:
        send_tag("host_download_command    ");
        call(test_length_field(pApdu, host_download_command_off , host_download_command_num ))
        ;
        call(dump_host_download_command (pApdu))
        ;
        break;
    case diagnostic_req_tag:
        send_tag("diagnostic_req           ");
        call(test_length_field(pApdu, diagnostic_req_off , diagnostic_req_num ))
        ;
        call(dump_diagnostic_req (pApdu))
        ;
        break;
    case diagnostic_cnf_tag:
        send_tag("diagnostic_cnf           ");
        call(test_length_field(pApdu, diagnostic_cnf_off , diagnostic_cnf_num ))
        ;
        call(dump_diagnostic_cnf (pApdu))
        ;
        break;
    case host_reset_vector_tag:
        send_tag("host_reset_vector        ");
        call(test_length_field(pApdu, host_reset_vector_off , host_reset_vector_num ))
        ;
        call(dump_host_reset_vector (pApdu))
        ;
        break;
    case host_reset_vector_ack_tag:
        send_tag("host_reset_vector_ack    ");
        call(test_length_field(pApdu, host_reset_vector_ack_off , host_reset_vector_ack_num ))
        ;
        call(dump_host_reset_vector_ack (pApdu))
        ;
        break;
    case host_properties_req_tag:
        send_tag("host_properties_req      ");
        call(test_length_field(pApdu, host_properties_req_off , host_properties_req_num ))
        ;
        call(dump_host_properties_req (pApdu))
        ;
        break;
    case host_properties_reply_tag:
        send_tag("host_properties_reply    ");
        call(test_length_field(pApdu, host_properties_reply_off , host_properties_reply_num ))
        ;
        call(dump_host_properties_reply (pApdu))
        ;
        break;
    case cp_open_req_tag:
        send_tag("cp_open_req              ");
        call(test_length_field(pApdu, cp_open_req_off , cp_open_req_num ))
        ;
        call(dump_cp_open_req (pApdu))
        ;
        break;
    case cp_open_cnf_tag:
        send_tag("cp_open_cnf              ");
        call(test_length_field(pApdu, cp_open_cnf_off , cp_open_cnf_num ))
        ;
        call(dump_cp_open_cnf (pApdu))
        ;
        break;
    case cp_data_req_tag:
        send_tag("cp_data_req              ");
        call(test_length_field(pApdu, cp_data_req_off , cp_data_req_num ))
        ;
        call(dump_cp_data_req (pApdu))
        ;
        break;
    case cp_data_cnf_tag:
        send_tag("cp_data_cnf              ");
        call(test_length_field(pApdu, cp_data_cnf_off , cp_data_cnf_num ))
        ;
        call(dump_cp_data_cnf (pApdu))
        ;
        break;
    case cp_sync_req_tag:
        send_tag("cp_sync_req              ");
        call(test_length_field(pApdu, cp_sync_req_off , cp_sync_req_num ))
        ;
        call(dump_cp_sync_req (pApdu))
        ;
        break;
    case cp_sync_cnf_tag:
        send_tag("cp_sync_cnf              ");
        call(test_length_field(pApdu, cp_sync_cnf_off , cp_sync_cnf_num ))
        ;
        call(dump_cp_sync_cnf (pApdu))
        ;
        break;
    case cp_valid_req_tag:
        send_tag("cp_valid_req             ");
        call(test_length_field(pApdu, cp_valid_req_off , cp_valid_req_num ))
        ;
        call(dump_cp_valid_req (pApdu))
        ;
        break;
    case cp_valid_cnf_tag:
        send_tag("cp_valid_cnf             ");
        call(test_length_field(pApdu, cp_valid_cnf_off , cp_valid_cnf_num ))
        ;
        call(dump_cp_valid_cnf (pApdu))
        ;
        break;

    default:
        return IsError;
    }
    return NoError;
}

Error dumpApdu(const uint8_t * const pBuffer, size_t size)
{
    Apdu apdu;
    apdu.data = pBuffer;
    apdu.next = 0;
    apdu.size = size;

    CHECK_LOGGER();

    return dump_one_apdu(&apdu);
}

void testDumpApdu(const uint8_t * const pBuffer, size_t size)
{
    uint32_t total = 0, failed = 0;
    Apdu apdu;
    apdu.data = pBuffer;
    apdu.next = 0;
    apdu.size = size;

    CHECK_LOGGER();

    while (apdu.next < apdu.size)
    {
        total++;
        if (dump_one_apdu(&apdu))
        {
            RILOG_INFO("Got an error...\n");
            failed++;
        }
    }
    RILOG_INFO("\n%u failed out of %u tests\n\n", failed, total);
}
