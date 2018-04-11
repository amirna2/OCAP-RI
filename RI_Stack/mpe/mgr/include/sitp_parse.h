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
#ifndef _SITP_PARSE_H
#define _SITP_PARSE_H 1

#include "sitp_psi.h"
#include "sitp_si.h"
/*
 *****************************************************************************
 * macros
 *****************************************************************************
 */
//******************************************************************************
//                    GetBits (in: Source, LSBit, MSBit)                       *
//******************************************************************************
//
// Note:
//         1) Bits within an octet or any other unit of storage (word,
//           longword, doubleword, quadword, etc.) range from 1 through N
//           (the maximum number of bits in the unit of storage) .
//
//         2) Bit #1 in any storage unit is the "absolute" least significant
//          bit(LSBit). However, depending upon where a field (a series of
//          bits) starts within a storage unit, the "virtual" LSBit associated
//            with the field may take on a value from 1 thru N.
//
//         3) Bit #N in any storage unit is the "absolute" most significant
//          bit (MSBit). However, depending upon where a field (a series of
//          bits) ends within a storage unit, the "virtual" MSBit associated
//           with the field may take on a value from 1 thru N .
//
//         4) For any field within any storage unit, the following inequality
//          must always be observed:
//
//          a.)  LSBit  <= (less than or equal to) MSBit .
//
//          5) This macro extracts a field of  N bits (starting at LSBit and
//           ending at MSBit) from argument Source, moves the extract field
//          into bits 1 thru N (where, N = 1 + (MSBit - LSBit)) of the register
//           before returning the extracted value to the caller.
//
//         6) This macro does not alter the contents of Source .
//
//
//return   (((((1 << (MSBit - LSBit + 1))-1) << (LSBit -1)) & Source)>> (LSBit -1))
//
//            |----------------------------| |------------| |----------||----------|
//                     Mask Generator          Mask Shift       Field       Right
//                                            Count      Extraction   Justifier
//
//******************************************************************************


#define      GETBITS(Source, MSBit, LSBit)   ( \
            ((((1 << (MSBit - LSBit + 1))-1) << (LSBit -1)) & Source) >>(LSBit -1))

//******************************************************************************
//                    TestBits (in: Source, LSBit, MSBit)                      *
//******************************************************************************
// Note:
//          1) Notes 1 thru 4 from GetBits are applicable .
//
//          2) Source is the storage unit containing the field (defined by
//            LSBit and MSBit) to be tested. The test is a non destructive
//            test to source.
//         3) If 1 or more of the tested bits are set to "1", the value
//            returned from TestBits will be greater than or equal to 1.
//             Otherwise, the value returned is equal to zero.
//
// return    ((((1 << (MSBit - LSBit + 1))-1) << (LSBit -1)) & Source)
//
//             |----------------------------| |------------| |-------|
//                     Mask Generator           Mask Shift     Field
//                                                Count        Extraction
//
//******************************************************************************


#define      TESTBITS(Source, MSBit, LSBit)   (\
         (((1 << (MSBit - LSBit + 1))-1) << (LSBit -1)) & Source)

#define      MIN_TIMEOUT(A,B)         ((A) <= (B) ? (A) : (B))

#define      PROGRAM_ASSOCIATION_TABLE_ID       0x00    // PMT_ID
#define      PROGRAM_MAP_TABLE_ID               0x02    // PAT_ID
#define      CABLE_VIRTUAL_CHANNEL_TABLE_ID     0xC9    // CVCT_ID
#define      CAT_PID                            0x0001    // conditional acess table packet identifier
#define      MIN_VALID_PID                      0x0010   // minimum valid value for packet identifier
#define      MAX_VALID_PID                      0x1FFE   // maximum valid value for packet identifier
#define      NO_PCR_PID                         0x1FFF   // Use when no PID carries the PCR
#define      MIN_VALID_PRIVATE_STREAM_TYPE      0x88   // minimum valid value for a private stream type
#define      MAX_VALID_PRIVATE_STREAM_TYPE      0xFF   // maximum valid value for a private stream type
// ECN-1176 added additional stream types supported by OCAP (ex: H.264 etc.)
// Hence the MIN and MAX values for non-private stream types is updated
#define      MIN_VALID_NONPRIVATE_STREAM_TYPE   0x01   // minimum valid value for a non-private stream type
#define      MAX_VALID_NONPRIVATE_STREAM_TYPE   0x87   // maximum valid value for a non-private stream type
#define      MAX_IB_SECTION_LENGTH              1021   //the maximum value that can be assigned to a
//PAT or PMT section_length field
#define      MAX_OOB_SMALL_TABLE_SECTION_LENGTH 1021   //the maximum value that can be assigned to a
// oob table other than MGT, L-VCT, AEIT, AETT
#define      MAX_OOB_LARGE_TABLE_SECTION_LENGTH 4093   //the maximum value that can be assigned to a
// oob table MGT, L-VCT, AEIT, AETT
#define      MAX_PROGRAM_INFO_LENGTH            1023   //the maximum value that can be assigned to a
//PMT program_info_length field
#define      MAX_ES_INFO_LENGTH                 1023   //the maximum value that can be assigned to a
//PMT es_info_length field
#define      MAX_VERSION_NUMBER                 31

#define      NO_PARSING_ERROR                   MPE_SUCCESS  //Should be equal to zero (0)
#define      LENGTH_CRC_32                      4           //length in bytes of a CRC_32
#define      LEN_OCTET1_THRU_OCTET3             3             //byte count for the 1st 3 byte in a structure,
#define      LEN_SECTION_HEADER                 3             //byte count for the 1st 3 byte in PAT/PMT
#define      SA_NON_CONFORMANCE_ISSUE

#define      NO_VERSION                         0xFF
#define      SIDB_DEFAULT_CHANNEL_NUMBER        0xFF

/*
 *****************************************************************************
 * structure definitions and typedefs (RR)
 *****************************************************************************
 */
typedef struct
{ // see document   ISO/IEC 13818-1:2000(E), pg.43
    uint8_t table_id_octet1;
    uint8_t section_length_octet2;
    uint8_t section_length_octet3;
    uint8_t transport_stream_id_octet4;
    uint8_t transport_stream_id_octet5;
    uint8_t current_next_indic_octet6;
    uint8_t section_number_octet7;
    uint8_t last_section_number_octet8;
} pat_table_struc1;

typedef struct
{ // see document ISO/IEC 13818-1:2000(E),pg.43
    uint8_t program_number_octet1;
    uint8_t program_number_octet2;
    uint8_t unknown_pid_octet3;
    uint8_t unknown_pid_octet4;
} pat_table_struc2;

typedef struct
{ // see document   ISO/IEC 13818-1:2000(E), pg.46
    uint8_t table_id_octet1;
    uint8_t section_length_octet2;
    uint8_t section_length_octet3;
    uint8_t program_number_octet4;
    uint8_t program_number_octet5;
    uint8_t current_next_indic_octet6;
    uint8_t section_number_octet7;
    uint8_t last_section_number_octet8;
    uint8_t pcr_pid_octet9;
    uint8_t pcr_pid_octet10;
    uint8_t program_info_length_octet11;
    uint8_t program_info_length_octet12;
} pmt_table_struc1;

typedef struct
{ // see document ISO/IEC 13818-1:2000(E),pg.46
    uint8_t stream_type_octet1;
    uint8_t elementary_pid_octet2;
    uint8_t elementary_pid_octet3;
    uint8_t es_info_length_octet4;
    uint8_t es_info_length_octet5;
} pmt_table_struc3;

typedef struct
{ // see document ANSI/SCTE 65-2002 (formerly DVS 234), chap 5.1,pg.14
    uint8_t table_id_octet1;
    uint8_t section_length_octet2;
    uint8_t section_length_octet3;
    uint8_t protocol_version_octet4;
    uint8_t first_index_octet5;
    uint8_t number_of_records_octet6;
    uint8_t table_subtype_octet7;
} nit_table_struc1;

typedef struct
{ // see document ISO/IEC 13818-1:2000(E),pg.46 and pg. 112
    uint8_t descriptor_tag_octet1;
    uint8_t descriptor_length_octet2;
} generic_descriptor_struc;

typedef struct
{ // see document SCTE-65, pg 60
    uint8_t descriptor_tag_octet1;
    uint8_t descriptor_length_octet2;
    uint8_t DS_status;
    uint8_t DS_hour;
} daylight_savings_descriptor_struc;

typedef struct
{
    // see doc ANSI/SCTE 65 2002, pg. 55.
    uint8_t descriptor_tag_octet1;
    uint8_t descriptor_length_octet2;
    uint8_t table_version_number_octet3;
    uint8_t section_number_octet4;
    uint8_t last_section_number_octet5;
} revision_detection_descriptor_struc;

typedef enum
{
    NIT_CDS_VERSION = 0,
    NIT_MMS_VERSION,
    SVCT_VCM_VERSION,
    SVCT_DCM_VERSION,
    SVCT_ICM_VERSION
} oob_version_indices_t;

typedef struct
{ // see document ANSI/SCTE 65-2002 (formerly DVS 234), chap 5.1.1,pg.16
    uint8_t number_of_carriers_octet1;
    uint8_t frequency_spacing_octet2;
    uint8_t frequency_spacing_octet3;
    uint8_t first_carrier_frequency_octet4;
    uint8_t first_carrier_frequency_octet5;
} cds_record_struc2;

typedef struct
{ // see document ANSI/SCTE 65-2002 (formerly DVS 234), chap 5.1.2,pg.18
    uint8_t inner_coding_mode_octet1;
    uint8_t modulation_format_octet2;
    uint8_t modulation_symbol_rate_octet3;
    uint8_t modulation_symbol_rate_octet4;
    uint8_t modulation_symbol_rate_octet5;
    uint8_t modulation_symbol_rate_octet6;
} mms_record_struc2;

typedef struct
{ // see document ANSI/SCTE 65-2002 (formerly DVS 234), chap 5.3,pg.23
    uint8_t table_id_octet1;
    uint8_t section_length_octet2;
    uint8_t section_length_octet3;
    uint8_t protocol_version_octet4;
    uint8_t table_subtype_octet5;
    uint8_t vct_id_octet6;
    uint8_t vct_id_octet7;
} svct_table_struc1;

typedef struct
{ // see document ANSI/SCTE 65-2002 (formerly DVS 234), chap 5.3.1,pg.24
    uint8_t first_virtual_chan_octet1;
    uint8_t first_virtual_chan_octet2;
    uint8_t dcm_data_length_octet_3;
} dcm_record_struc2;

typedef struct
{ // see document ANSI/SCTE 65-2002 (formerly DVS 234), chap 5.3.1,pg.24
    uint8_t channel_count_octet1;
} dcm_record_struc3;

typedef struct
{ // see document ANSI/SCTE 65-2002 (formerly DVS 234), chap 5.3.2,pg.25
    uint8_t descriptors_included_octet1;
    uint8_t splice_octet2;
    uint8_t activation_time_octet3;
    uint8_t activation_time_octet4;
    uint8_t activation_time_octet5;
    uint8_t activation_time_octet6;
    uint8_t number_vc_records_octet7;
} vcm_record_struc2;

typedef struct
{ // see document ANSI/SCTE 65-2002 (formerly DVS 234), chap 5.3.2,pg.26
    uint8_t virtual_chan_number_octet1;
    uint8_t virtual_chan_number_octet2;
    uint8_t chan_type_octet3;
    uint8_t app_or_source_id_octet4;
    uint8_t app_or_source_id_octet5;
    uint8_t cds_reference_octet6;
} vcm_virtual_chan_record3;

typedef struct
{ // see document ANSI/SCTE 65-2002 (formerly DVS 234), chap 5.3.2,pg.26
    uint8_t program_number_octet1;
    uint8_t program_number_octet2;
    uint8_t mms_number_octet3;
} mpeg2_virtual_chan_struc4;

typedef struct
{ // see document ANSI/SCTE 65-2002 (formerly DVS 234), chap 5.3.2,pg.26
    uint8_t video_standard_octet1;
    uint8_t eight_zeroes_octet2;
    uint8_t eight_zeroes_octet3;
} nmpeg2_virtual_chan_struc5;

typedef struct
{ // see document ANSI/SCTE 65-2002 (formerly DVS 234), chap 5.3.3,pg.29
    uint8_t first_map_index_octet1;
    uint8_t first_map_index_octet2;
    uint8_t record_count_octet3;
} icm_record_struc2;

typedef struct
{ // see document ANSI/SCTE 65-2002 (formerly DVS 234), chap 5.3.3,pg.29
    uint8_t source_id_octet1;
    uint8_t source_id_octet2;
    uint8_t virtual_chan_number_octet3;
    uint8_t virtual_chan_number_octet4;
} icm_record_struc3;

typedef struct
{ // see document   ATSC Doc.A/65B - PSIP for Terrestrial Broadcast and Cable,
    // section 6.3.2, pg.35
    uint8_t table_id_octet1;
    uint8_t section_length_octet2;
    uint8_t section_length_octet3;
    uint8_t transport_stream_id_octet4;
    uint8_t transport_stream_id_octet5;
    uint8_t current_next_indic_octet6;
    uint8_t section_number_octet7;
    uint8_t last_section_number_octet8;
    uint8_t protocol_version_octet9;
    uint8_t num_channels_in_section_octet10;
} cvct_table_struc1;

typedef struct
{ // see document   ATSC Doc.A/65B - PSIP for Terrestrial Broadcast and Cable,
    // section 6.3.2, pg.35
    uint8_t major_channel_number_octet1;
    uint8_t major_channel_number_octet2;
    uint8_t minor_channel_number_octet3;
    uint8_t modulation_mode_octet4;
    uint8_t carrier_frequency_octet5;
    uint8_t carrier_frequency_octet6;
    uint8_t carrier_frequency_octet7;
    uint8_t carrier_frequency_octet8;
    uint8_t channel_TSID_octet9;
    uint8_t channel_TSID_octet10;
    uint8_t program_number_octet11;
    uint8_t program_number_octet12;
    uint8_t access_controlled_hidden_octet13;
    uint8_t service_type_octet14;
    uint8_t source_id_octet15;
    uint8_t source_id_octet16;
    uint8_t descriptors_length_octet17;
    uint8_t descriptors_length_octet18;
} cvct_table_struc2;

typedef struct
{
    // see document   ATSC Doc.A/65B - PSIP for Terrestrial Broadcast and Cable,
    // section 6.3.2, pg.35
    uint8_t additional_descriptor_length_octet1;
    uint8_t additional_descriptor_length_octet2;
} cvct_table_struc3;

//System Time Table Structure
typedef struct
{ // see document ANSI/SCTE 65-2002 (formerly DVS 234), chap 5.4,pg.30
    uint8_t table_id_octet1;
    uint8_t section_length_octet2;
    uint8_t section_length_octet3;
    uint8_t protocol_version_octet4;
    uint8_t zeroes_octet5;
    uint8_t system_time_octet6;
    uint8_t system_time_octet7;
    uint8_t system_time_octet8;
    uint8_t system_time_octet9;
    uint8_t GPS_UTC_offset_octet10;
} stt_table_struc1;

//Network Text Table Structure
typedef struct
{ // see document ANSI/SCTE 65-2002 (formerly DVS 234), chap 5.2,pg.20
    uint8_t table_id_octet1;
    uint8_t section_length_octet2;
    uint8_t section_length_octet3;
    uint8_t protocol_version_octet4;
    uint8_t ISO_639_language_code_octet5;
    uint8_t ISO_639_language_code_octet6;
    uint8_t ISO_639_language_code_octet7;
    uint8_t table_subtype_octet8;
} ntt_record_struc1;

//Source Name Subtable Structure
typedef struct
{ // see document ANSI/SCTE 65-2002 (formerly DVS 234), chap 5.2,pg.22
    uint8_t number_of_SNS_records_octet1;
} sns_record_struc1;

typedef struct
{ // see document ANSI/SCTE 65-2002 (formerly DVS 234), chap 5.2,pg.22
    uint8_t application_type_octet1;
    uint8_t source_ID_octet2;
    uint8_t source_ID_octet3;
    uint8_t name_length_octet4;
} sns_record_struc2;

//Multilingual Text String (MTS) format Structure
typedef struct
{ // see document ANSI/SCTE 65-2002 (formerly DVS 234), chap 7.1,pg.62
    uint8_t mode_octet1;
    uint8_t length_octet2;
} mts_format_struc1;

mpe_Error parseAndUpdatePAT(uint32_t section_size, uint8_t *section_data,
        sitp_psi_tuner_type_e tuner_type, sitp_psi_params_t *psi_params,
        sitp_psi_fsm_data_t *fsm_data);
mpe_Error parseAndUpdatePMT(uint32_t section_size, uint8_t *section_data,
        sitp_psi_tuner_type_e tuner_type, sitp_psi_params_t *psi_params,
        sitp_psi_fsm_data_t *fsm_data);

mpe_Error parseAndUpdateSVCT(mpe_FilterSectionHandle section_handle,
        uint8_t *version, uint8_t *section_number,
        uint8_t *last_section_number, uint32_t *crc);
mpe_Error parseAndUpdateNIT(mpe_FilterSectionHandle section_handle,
        uint8_t *version, uint8_t *section_number,
        uint8_t *last_section_number, uint32_t *crc);
mpe_Error parseCDSRecords(uint16_t number_of_records, uint16_t first_index,
        uint8_t ** handle_section_data, uint32_t * section_length);
mpe_Error parseMMSRecords(uint16_t number_of_records, uint16_t first_index,
        uint8_t ** handle_section_data, uint32_t *section_length);
mpe_Error
        parseVCMStructure(uint8_t **ptr_TableSection, int32_t *section_length);
mpe_Error
        parseDCMStructure(uint8_t **ptr_TableSection, int32_t *section_length);
mpe_Error get_revision_data(mpe_FilterSectionHandle section_handle,
        uint8_t * version, uint8_t * section_number, uint8_t * number_sections,
        uint32_t * crc);
mpe_Error parseNIT_revision_data(mpe_FilterSectionHandle section_handle,
        uint8_t * version, uint8_t * section_number, uint8_t * number_sections,
        uint32_t * crc);
mpe_Error parseNTT_revision_data(mpe_FilterSectionHandle section_handle,
        uint8_t * version, uint8_t * section_number, uint8_t * number_sections,
        uint32_t * crc);
mpe_Error parseSVCT_revision_data(mpe_FilterSectionHandle section_handle,
        uint8_t * version, uint8_t * section_number, uint8_t * number_sections,
        uint32_t * crc);
mpe_Error parseSTT_revision_data(mpe_FilterSectionHandle section_handle,
        uint8_t * version, uint8_t * section_number, uint8_t * number_sections,
        uint32_t * crc);
mpe_Error parseAndUpdateSTT(mpe_FilterSectionHandle section_handle,
        uint8_t *version, uint8_t *section_number,
        uint8_t *last_section_number, uint32_t *crc);
mpe_Error parseAndUpdateNTT(mpe_FilterSectionHandle section_handle,
        uint8_t *version, uint8_t *section_number,
        uint8_t *last_section_number, uint32_t *crc);

mpe_Error parseSNSSubtable(uint8_t **pptr_TableSection,
        int32_t *ptr_section_length, char *language);

void parseMTS(uint8_t **ptr_TableSection, uint8_t name_lenth, uint8_t * mode,
        uint8_t * length, uint8_t * segment);
/*
void getSidbVersionAndCrc(sitp_psi_transport_stream_data_t *fsm_data,
        int table_type, uint32_t *version, uint32_t *crc,
        mpe_SiServiceHandle service_handle);
*/
void getSidbVersionAndCrc(sitp_psi_fsm_data_t * fsm_data,
        int table_type, uint32_t *version, uint32_t *crc,
        mpe_SiGenericHandle handle);

uint32_t getSidbVersionNumber(mpe_SiTableType table_type,
        mpe_SiGenericHandle handle);

uint32_t getSidbCRC(mpe_SiTableType table_type,
        mpe_SiServiceHandle service_handle);

void notifySIDB(mpe_SiTableType table_type,
        mpe_Bool init_version, mpe_Bool new_version, mpe_Bool new_crc,
        mpe_SiServiceHandle service_handle);
#endif /*SITP_PARSE_H*/
