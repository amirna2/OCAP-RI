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

#include "sitp_parse.h"
#include "time_util.h"
#include <mpeos_time.h>
#include <string.h>

mpe_Error parseAndUpdatePAT(uint32_t section_size, uint8_t *section_data,
        sitp_psi_tuner_type_e tuner_type, sitp_psi_params_t *psi_params,
        sitp_psi_fsm_data_t *fsm_data)
{
    pat_table_struc1 *ptr_table_struc1;
    pat_table_struc2 *ptr_table_struc2;

    mpe_SiServiceHandle si_database_handle = 0; // a handle into ths SI database
    mpe_SiTransportStreamHandle ts_handle = MPE_SI_INVALID_HANDLE;

    uint8_t *ptr_input_buffer = NULL;

    uint16_t version_number = 0;
    uint16_t current_next_indicator = 0;
    uint16_t unknown_pid = 0;
    uint16_t transport_stream_id = 0;
    uint32_t frequency = 0;
    uint16_t program_number = PROGRAM_NUMBER_UNKNOWN;

    uint32_t section_length = 0;
    uint32_t number_of_PMTs = 0;
    uint32_t crc = 0;
    mpe_Error pat_error = NO_PARSING_ERROR;
    mpe_Error retCode = 0;
    uint32_t sidbVersionNumber = INIT_TABLE_VERSION;
    uint32_t sidbCrc32 = 0;
    mpe_Bool reset = FALSE;
    mpe_Bool primary_program = FALSE;
    uint32_t dsg_app_id = 0;
    mpe_HnStreamSession hn_stream_session = NULL;
    uint32_t pn = PROGRAM_NUMBER_UNKNOWN;
    uint8_t mode = MPE_SI_MODULATION_UNKNOWN;
    mpe_SiTableType table_type = OOB_PAT;

    //***********************************************************************************************
    //**********************************************************************************************
    // Get,parse and validate the first 64 bytes (Table_ID thru Last_Section_Number)of the PAT
    // table (see document ISO/IEC 13818-1:2000(E),pg.43) .
    //**********************************************************************************************

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::parseAndUpdatePAT> - Entered method parseAndUpdatePAT.\n",
            PSIMODULE);

    ts_handle = fsm_data->ts_handle;

    /*for(i=0;i<section_size;i++)
    {
        MPE_LOG(MPE_LOG_TRACE3, MPE_MOD_SI,
                "<%s::parseAndUpdatePAT> - section_data[%d] = 0x%x \n", PSIMODULE, i, section_data[i]);
    }
    */

    MPE_LOG(MPE_LOG_TRACE3, MPE_MOD_SI,
            "<%s::parseAndUpdatePAT> - ts_handle = 0x%x \n", PSIMODULE, ts_handle);

    MPE_LOG(MPE_LOG_TRACE3, MPE_MOD_SI,
            "<%s::parseAndUpdatePAT> - section_size = %d \n", PSIMODULE,
            section_size);

    ptr_input_buffer = (uint8_t *)section_data;

    // QUESTION: Does the section_data include header??
    // If it does not we don't need to parse the section_length
    // field to validate

    ptr_table_struc1 = (pat_table_struc1 *) ptr_input_buffer;

    //**** validate table Id field ****
    if (ptr_table_struc1->table_id_octet1 != PROGRAM_ASSOCIATION_TABLE_ID)
    {
        pat_error = 1;
        goto PARSING_DONE;
    }

    //**** extract and validate section_length field (octets 2 & 3)       ****
    //**** note, bits 12 and 11 must be set to zero and max value is 1021.****

    section_length = ((GETBITS(ptr_table_struc1->section_length_octet2, 4,
            1)) << 8) | ptr_table_struc1->section_length_octet3;

    if (section_length > MAX_IB_SECTION_LENGTH)
    {
        // error, section_length too.
        pat_error = 2;
        goto PARSING_DONE;
    }

    MPE_LOG(MPE_LOG_TRACE3, MPE_MOD_SI,
            "<%s::parseAndUpdatePAT> - section_length = %d \n", PSIMODULE,
            section_length);

    // Subtract the tableId and section_length field's sizes
    // Should match the section_length parsed from PAT
    if(section_size-LEN_SECTION_HEADER != section_length)
    {
        // error, section_length invalid.
        pat_error = 3;
        goto PARSING_DONE;
    }

    //**** extract and validate transport_stream_id field (octets 4 & 5) *****/
    transport_stream_id = (uint16_t)(((ptr_table_struc1->transport_stream_id_octet4) << 8)
                    | ptr_table_struc1->transport_stream_id_octet5);

    MPE_LOG(MPE_LOG_TRACE3, MPE_MOD_SI,
            "<%s::parseAndUpdatePAT> - transport_stream_id = %d \n", PSIMODULE,
            transport_stream_id);

    //**** validate 2 reserved bits ****
    if (GETBITS(ptr_table_struc1->current_next_indic_octet6, 8, 7) != 3)
    {
        // bits 8 and 7 are not set to 1 per ISO/IEC 13818-1:2000(E),pg.4, para 2.1.46)
        //pat_error = 4 ;
        MPE_LOG(MPE_LOG_TRACE3,
                MPE_MOD_SI,
                "<%s::parseAndUpdatePAT> - *** WARNING *** 2-Bit-Reserved Field #3 not set to all ones(1)\n.",
                PSIMODULE);
    }

    //**** extract version_number (0-31) field ****

    version_number = (uint16_t)(GETBITS(ptr_table_struc1->current_next_indic_octet6, 6, 2));

    MPE_LOG(MPE_LOG_TRACE3, MPE_MOD_SI,
            "<%s::parseAndUpdatePAT> - version_number = %d \n", PSIMODULE,
            version_number);
    //**** populate the table info array with the new PAT data ****

    add_pat_entry(fsm_data, version_number);

    //**** extract current_next_indicator field ****
    current_next_indicator = (uint16_t)(GETBITS(ptr_table_struc1->current_next_indic_octet6, 1, 1));
    MPE_LOG(MPE_LOG_TRACE3, MPE_MOD_SI,
            "<%s::parseAndUpdatePAT> - current_next_indicator = %d\n.",
            PSIMODULE, current_next_indicator);

    //**** section_length identifies the total number of bytes/octets remaining   ****
    //**** in the PAT after octet3. Before performing program number, network pid ****
    //**** and program map pid processing ,we must adjust section_length to       ****
    //**** account for having processed  octets4 thru octet8                      ****.
    section_length -= (sizeof(pat_table_struc1) - LEN_OCTET1_THRU_OCTET3);

    // It is possible that an empty PAT (no programs) is signaled sometimes. In
    // that case all we see is a header and a CRC.
    // Check if the length is less than LENGTH_CRC_32, then its an error.
    if (section_length < LENGTH_CRC_32)
    {
        // error, either a portion of header is missing (section_length <0) or
        // no data after header (section_length = 0) or  section_length is not
        // large enough to hold a CRC_2 and a program_number and pid pair.
        pat_error = 5;
        goto PARSING_DONE;
    }

    switch(tuner_type)
    {
        case SITP_PSI_TUNER_TYPE_OOB:
        {
            frequency = MPE_SI_OOB_FREQUENCY;
        }
            break;
        case SITP_PSI_TUNER_TYPE_DSG:
        {
            sitp_dsg_tunnel_info_t* dsg_info = (sitp_dsg_tunnel_info_t*) psi_params;
            frequency = MPE_SI_DSG_FREQUENCY;
            dsg_app_id = dsg_info->dsg_app_id;
        }
            break;
        case SITP_PSI_TUNER_TYPE_HN:
        {
            sitp_hn_stream_info_t* hn_info = (sitp_hn_stream_info_t*) psi_params;
            frequency = MPE_SI_HN_FREQUENCY;
            hn_stream_session = hn_info->hn_stream_session;
        }
            break;
        case SITP_PSI_TUNER_TYPE_IB:
        {
            sitp_ib_tune_info_t* tune_info = (sitp_ib_tune_info_t*) psi_params;
            frequency = tune_info->frequency;
            pn = tune_info->program_number; // Tuned program
            mode = tune_info->modulation_mode;
            table_type = IB_PAT;
            reset = TRUE;
        }
            break;
        default:
            break;
    }

    mpe_siSetTSIdForTransportStream(ts_handle, transport_stream_id);
    sidbVersionNumber = getSidbVersionNumber(table_type, ts_handle);
    sidbCrc32 = getSidbCRC(table_type, ts_handle);
    mpe_siSetPATVersionForTransportStream(ts_handle, version_number);
    MPE_LOG(MPE_LOG_TRACE3,
            MPE_MOD_SI,
            "<%s::parseAndUpdatePAT> - sidb version = %d, sidb crc = 0x%x.\n",
            PSIMODULE, sidbVersionNumber, sidbCrc32);
    MPE_LOG(MPE_LOG_TRACE3, MPE_MOD_SI,
            "<%s::parseAndUpdatePAT> - new version = %d.\n", PSIMODULE,
            version_number);

    //**********************************************************************************************
    // Parse the remaining bytes/octets in the PAT.
    //**********************************************************************************************
    ptr_input_buffer = (uint8_t *) (&section_data[sizeof(pat_table_struc1)]);

    //*****************************************************************************************
    //**** This is the "FOR N Loop" in spec (ISO/IEC 13818-1:2000(E),pg.43, table 2-25)
    //**** Within this loop we will extract and validate program_number and the
    //**** network_pid/program_map_pid.
    //*****************************************************************************************
    while (section_length > LENGTH_CRC_32)
    {
        ptr_table_struc2 = (pat_table_struc2 *) ptr_input_buffer;

        /*
         ** extract the program_number field (octets 1 & 2) and unknown_pid field
         ** ( octetes 3 & 4 ).
         */
        program_number = (uint16_t)(((ptr_table_struc2->program_number_octet1)
                << 8) | ptr_table_struc2->program_number_octet2);
        unknown_pid = (uint16_t)(((ptr_table_struc2->unknown_pid_octet3) << 8)
                | ptr_table_struc2->unknown_pid_octet4);

        if (GETBITS(unknown_pid, 16, 14) != 0x07)
        {
            // error, the 3 reserve bits (16-14, in program_map_pid/network_pid)
            // are not set to "1" // per ISO/IEC 13818-1:2000(E),pg.4, para 2.1.46)
            // pat_error = 6 ;
            MPE_LOG(MPE_LOG_TRACE3,
                    MPE_MOD_SI,
                    "<%s::parseAndUpdatePAT> - *** WARNING *** 3-Bit-Reserved Field #4 not set to all ones(1)\n.",
                    PSIMODULE);
        }

        unknown_pid = (uint16_t)(unknown_pid & 0x1FFF);

        if (!((unknown_pid >= MIN_VALID_PID) && (unknown_pid <= MAX_VALID_PID)))
        {
            // error, invalid  unknown_pid (program_map_pid/network_pid) .
            pat_error = 7;
            goto PARSING_DONE;
        }

        if (program_number != 0)
        {
            MPE_LOG(MPE_LOG_TRACE1,
                    MPE_MOD_SI,
                    "<%s::parseAndUpdatePAT> - Processing Program_Number = %d, tuned program_number = %d.\n",
                    PSIMODULE, program_number, pn);

            MPE_LOG(MPE_LOG_TRACE1,
                    MPE_MOD_SI,
                    "<%s::parseAndUpdatePAT> - ProgramMap_PID = 0x%x(%d).\n",
                    PSIMODULE, unknown_pid, unknown_pid);

            // Process each program found in PAT
            {
                mpe_SiProgramHandle prog_handle = MPE_SI_INVALID_HANDLE;
                //**** unknown_pid is a "program_map_pid", deposit the other
                //**** data items into the appropriate SI Database file/table .
                if ( (frequency == MPE_SI_DSG_FREQUENCY) && dsg_app_id > 0)
                {
                    // This is PSI for DSG app tunnel
                    // Get DSG service entry
                    retCode = mpe_siGetServiceEntryFromAppIdProgramNumber(
                            dsg_app_id, program_number,
                            &si_database_handle);
                    if (retCode == MPE_SI_NOT_FOUND)
                    {
                        retCode = mpe_siCreateDSGServiceHandle((uint32_t) dsg_app_id,
                                        program_number, NULL, NULL,
                                        &si_database_handle);
                    }
                    (void) mpe_siGetProgramEntryFromTransportStreamAppIdProgramNumber(
                            ts_handle, dsg_app_id,
                            program_number, &prog_handle);

                    mpe_siSetAppId(si_database_handle, dsg_app_id);
                    mpe_siSetAppType(si_database_handle, true);
                }
                else if ((frequency == MPE_SI_HN_FREQUENCY) && hn_stream_session > 0)
                {
                    // Get HN service entry
                    (void) mpe_siGetProgramEntryFromTransportStreamHNstreamProgramNumber(
                            ts_handle, hn_stream_session,
                            program_number, &prog_handle);
                }
                else if (frequency == MPE_SI_OOB_FREQUENCY)
                {
                        (void) mpe_siGetProgramEntryFromTransportStreamEntry(
                                ts_handle, program_number, &prog_handle);
                        (void) mpe_siGetServiceEntryFromFrequencyProgramNumberModulation(
                                frequency, program_number,
                                mode, &si_database_handle);
                }
                else
                {
                    (void) mpe_siGetProgramEntryFromTransportStreamEntry(
                            ts_handle, program_number, &prog_handle);
                    (void) mpe_siGetServiceEntryFromFrequencyProgramNumberModulation(
                            frequency, program_number,
                            mode, &si_database_handle);
                }
                MPE_LOG(MPE_LOG_DEBUG,
                        MPE_MOD_SI,
                        "<%s::parseAndUpdatePAT> - frequency = %d, program_number = %d, modulation_mode = %d \n",
                        PSIMODULE, frequency,
                        program_number, mode);

                (void) mpe_siSetPATProgramForTransportStream(ts_handle,
                        program_number, unknown_pid);
                mpe_siSetPMTPid(prog_handle, unknown_pid);

                /* increment Program_Number/PMT count */
                number_of_PMTs++;

                if( ((tuner_type == SITP_PSI_TUNER_TYPE_IB) && (program_number == pn))
                        || (tuner_type == SITP_PSI_TUNER_TYPE_OOB)
                        || (tuner_type == SITP_PSI_TUNER_TYPE_DSG)
                        || (tuner_type == SITP_PSI_TUNER_TYPE_HN))
                {
                    MPE_LOG(MPE_LOG_TRACE1,
                            MPE_MOD_SI,
                            "<%s::parseAndUpdatePAT> - primary program number = %d.\n",
                            PSIMODULE, program_number);

                    // Identify the primary program number
                    // (for DSG/HN/OOB there is only one program)
                    primary_program = TRUE;
                }
                else
                {
                    primary_program = FALSE;
                }

                (void) add_pmt_entry(fsm_data, /* the filter info array */
                program_number, /* the program # of the PMT */
                primary_program,
                unknown_pid, /* the program map pid of the PMT */
                INIT_TABLE_VERSION); /* the version number - INIT version since  we haven't seen this PMT yet */

                MPE_LOG(MPE_LOG_INFO,
                        MPE_MOD_SI,
                        "<%s::parseAndUpdatePAT> - number_of_PMTS = %d\n",
                        PSIMODULE, number_of_PMTs);
            }
        }
        else
        {
            //**** unknown_pid is a "network_pid", ignore it.
            // do nothing
            MPE_LOG(MPE_LOG_TRACE3,
                    MPE_MOD_SI,
                    "<%s::parseAndUpdatePAT> - Ignoring Program_Number = %d, Network_PID = x%x.\n",
                    PSIMODULE, program_number, unknown_pid);
        }

        //**** prepare to process the next program_number and
        //**** unknown_pid (program_map_pid/network_pid) pair.
        if (section_length >= (sizeof(pat_table_struc2) + LENGTH_CRC_32))
        {
            section_length -= sizeof(pat_table_struc2);
            ptr_input_buffer += sizeof(pat_table_struc2);
        }

    } // End while(section_length > LENGTH_CRC_32)


    //*************************************************************************************************
    //* At this point, the code either terminated prematurely or successfully processed the PMT.
    //* We need to validate the present of the CRC_32 field and cleanup before returning to the caller.
    //*************************************************************************************************
    if ((section_length != LENGTH_CRC_32))
    {
        // error, either the last program_number and program_map_pid/network_pid) pair
        // had an incorrect number of bytes or the CRC_32 had an incorrect number of bytes.
        MPE_LOG(MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::parseAndUpdatePAT> - section length(%d) != LENGHT_CRC_32(%d) - error  - %d\n",
                PSIMODULE, section_length, LENGTH_CRC_32, pat_error);
        pat_error = 13;
        goto PARSING_DONE;
    }

    MPE_LOG(MPE_LOG_TRACE3, MPE_MOD_SI,
            "<%s::parseAndUpdatePAT> - section_length = %u\n", PSIMODULE,
            section_length);
    if (section_length == LENGTH_CRC_32)
    {
        crc = ((*ptr_input_buffer) << 24 | (*(ptr_input_buffer + 1)) << 16
                | (*(ptr_input_buffer + 2)) << 8 | (*(ptr_input_buffer + 3)));
        MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
                "<%s::parseAndUpdatePAT> - CRC32 = 0x%08x\n", PSIMODULE, crc);
    }

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::parseAndUpdatePAT> - ts_handle = 0x%x\n", PSIMODULE,
            ts_handle);

    mpe_siSetPATCRCForTransportStream(ts_handle, crc);

PARSING_DONE:

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::parseAndUpdatePAT> - Exiting with ErrorCode = %d\n",
            PSIMODULE, pat_error);

    if (pat_error != 0)
    {
        mpe_siSetPATStatusNotAvailable(ts_handle);
    }
    else
    {
        MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
                "<%s::parseAndUpdatePAT> - Calling mpe_siSetPATStatus\n",
                PSIMODULE);
        mpe_siSetPATStatusAvailable(ts_handle);
        notifySIDB(table_type, sidbVersionNumber == INIT_TABLE_VERSION, sidbVersionNumber != version_number,
                sidbCrc32 != crc, ts_handle);

        /*
         * Reset PMT status to 'NOT_AVAILABLE' for all programs on transport stream
         * not signaled in the newly acquired PAT
         */
        if (reset)
        {
            MPE_LOG(MPE_LOG_TRACE1,
                    MPE_MOD_SI,
                    "<%s::parseAndUpdatePAT> - Calling mpe_siResetPATProgramStatus\n",
                    PSIMODULE);
            mpe_siResetPATProgramStatus(ts_handle);
        }
    }
    return pat_error;
}

//***************************************************************************************************
//  Subroutine/Method:                       parseAndUpdatePMT()
//***************************************************************************************************
/**
 * Retrieve PMT and update SI database.
 *
 * @param sectionHandle The section for retrieving PMT data.
 * @return MPE_ECOND if the table id is incorrect;
 *         MPE_SUCCESS if succeeds;
 *         other error code on error condition.
 */
mpe_Error parseAndUpdatePMT(uint32_t section_size, uint8_t *section_data,
        sitp_psi_tuner_type_e tuner_type, sitp_psi_params_t *psi_params,
        sitp_psi_fsm_data_t *fsm_data)
{
    pmt_table_struc1 *ptr_table_struc1 = NULL;
    pmt_table_struc3 *ptr_table_struc3 = NULL;
    generic_descriptor_struc *ptr_descriptor_struc = NULL;
    mpe_SiTransportStreamHandle ts_handle = MPE_SI_INVALID_HANDLE;
    mpe_SiProgramHandle si_program_handle = MPE_SI_INVALID_HANDLE; // a handle into this SI database element
    mpe_SiServiceHandle si_service_handle = MPE_SI_INVALID_HANDLE;
    uint8_t *ptr_input_buffer = NULL;

    uint32_t es_info_length = 0, section_length = 0;
    uint32_t crc = 0;
    uint16_t program_number = PROGRAM_NUMBER_UNKNOWN, version_number = 0, pcr_pid = 0,
            program_info_length = 0, elementary_pid = 0, stream_type = 0,
            descriptor_tag = 0, descriptor_length = 0;

    mpe_Error pmt_error = NO_PARSING_ERROR;
    mpe_Error retCode = 0;
    uint32_t sidbVersionNumber = INIT_TABLE_VERSION;
    uint32_t sidbCrc32 = 0;
    uint32_t frequency = 0;
    uint32_t dsg_app_id = 0;
    mpe_HnStreamSession hn_stream_session = NULL;
    uint8_t mode = MPE_SI_MODULATION_UNKNOWN;
    mpe_SiTableType table_type = OOB_PMT;

    //***********************************************************************************************
    //**********************************************************************************************
    // Get,parse and validate the first 96 bytes (Table_ID thru Program_Info_length)of the PMT
    // table (see document ISO/IEC 13818-1:2000(E),pg.46) .
    //**********************************************************************************************
    MPE_LOG(MPE_LOG_TRACE1,
            MPE_MOD_SI,
            "<%s::parseAndUpdatePMT> - Entered method parseAndUpdatePMT..\n",
            PSIMODULE);
    ptr_input_buffer = (uint8_t *)section_data;

    ts_handle = fsm_data->ts_handle;
    MPE_LOG(MPE_LOG_TRACE1,
            MPE_MOD_SI,
            "<%s::parseAndUpdatePMT> - parseAndUpdatePMT: ts_handle: 0x%x\n",
            PSIMODULE, ts_handle);

    ptr_table_struc1 = (pmt_table_struc1 *) ptr_input_buffer;

    //**** validate table Id field ****
    if (ptr_table_struc1->table_id_octet1 != PROGRAM_MAP_TABLE_ID)
    {
        pmt_error = 2;
        goto PARSING_DONE;
    }

    //**** validate section_syntax_indicator field ****
    if (!TESTBITS(ptr_table_struc1->section_length_octet2, 8, 8))
    {
        // bit not set to 1
        pmt_error = 3;
        goto PARSING_DONE;
    }

    //**** validate '0' bit field ****
    if (TESTBITS(ptr_table_struc1->section_length_octet2, 7, 7))
    {
        // bit not set to 0
        //pmt_error = 4 ;
        MPE_LOG(MPE_LOG_TRACE3,
                MPE_MOD_SI,
                "<%s::parseAndUpdatePMT> - *** WARNING *** 1-Bit-Zero Field #1 not set to all zeroes(0)\n.",
                PSIMODULE);
    }

    //**** validate 2 reserved bits ****
    if (GETBITS(ptr_table_struc1->section_length_octet2, 6, 5) != 3)
    {
        // bits 5 and 6 are not set to 1 per ISO/IEC 13818-1:2000(E),pg.4, para 2.1.46)
        //pmt_error = 5 ;
        MPE_LOG(MPE_LOG_TRACE3,
                MPE_MOD_SI,
                "<%s::parseAndUpdatePMT> - *** WARNING *** 2-Bit-Reserved Field #2 not set to all ones(1)\n.",
                PSIMODULE);
    }

    //**** extract and validate section_length field (octets 2 & 3)       ****
    //**** note, bits 12 and 11 must be set to zero and max value is 1021.****
    section_length = ((GETBITS(ptr_table_struc1->section_length_octet2, 4, 1))
            << 8) | ptr_table_struc1->section_length_octet3;

    if ((section_length > MAX_IB_SECTION_LENGTH) || (section_length
            < (sizeof(pmt_table_struc1) - LEN_OCTET1_THRU_OCTET3)))
    { // error, section_length too long or PMT header is too short.
        pmt_error = 6;
        goto PARSING_DONE;
    }

    // Subtract the tableId and section_length field's sizes
    // Should match the section_length parsed from PMT
    if(section_size-LEN_SECTION_HEADER != section_length)
    {
        // Length fields don't match
        pmt_error = 7;
        goto PARSING_DONE;
    }

    //**** extract and validate program_number field (octets 4 & 5) *****
    program_number = (uint16_t)(((ptr_table_struc1->program_number_octet4) << 8)
                    | ptr_table_struc1->program_number_octet5);

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::parseAndUpdatePMT> - PMT's Program_Number = %d.\n",
            PSIMODULE, program_number);

    //**** validate 2 reserved bits ****
    if (GETBITS(ptr_table_struc1->current_next_indic_octet6, 8, 7) != 3)
    {
        // bits 8 and 7 are not set to 1 per ISO/IEC 13818-1:2000(E),pg.4, para 2.1.46)
        //pmt_error = 8 ;
        MPE_LOG(MPE_LOG_TRACE3,
                MPE_MOD_SI,
                "<%s::parseAndUpdatePMT> - *** WARNING *** 2-Bit-Reserved Field #3 not set to all ones(1)\n.",
                PSIMODULE);
    }

    //**** extract version_number (0-31) field ****
    version_number = (uint16_t)(GETBITS(
            ptr_table_struc1->current_next_indic_octet6, 6, 2));
    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,

            "<%s::parseAndUpdatePMT> - PMT's version_number = %d.\n",
            PSIMODULE, version_number);
    /*
     * set the pmt version in the filter_info array of the state machine data.
     */
    retCode = set_pmt_version(fsm_data, program_number, version_number);
    if (retCode != MPE_SUCCESS)
    {
        //**** We received a PMT with an unexpected program_number (fundamental PAT/PMT disparity!)
        pmt_error = 28;
        goto PARSING_DONE;
    }

    //**** validate section_number and last_section_number ****
    if (ptr_table_struc1->section_number_octet7 != 0)
    {
        pmt_error = 10;
        goto PARSING_DONE;
    }

    if (ptr_table_struc1->last_section_number_octet8 != 0)
    {
        pmt_error = 11;
        goto PARSING_DONE;
    }

    //**** validate 3 reserved bits ****
    if (GETBITS(ptr_table_struc1->pcr_pid_octet9, 8, 6) != 0x07)
    {
        // bits 8,7 and 6 are not set to 1 per ISO/IEC 13818-1:2000(E),pg.4, para 2.1.46)
        //  pmt_error = 12;
        MPE_LOG(MPE_LOG_TRACE3,
                MPE_MOD_SI,
                "<%s::parseAndUpdatePMT> - *** WARNING *** 3-Bit-Reserved Field #4 not set to all ones(1)\n.",
                PSIMODULE);
    }

    //**** extract and validate 13 bit pcr_pid from octets 9 and 10 ****
    pcr_pid = (uint16_t)(((ptr_table_struc1->pcr_pid_octet9 & 0x1f) << 8)
            | ptr_table_struc1->pcr_pid_octet10);

    MPE_LOG(MPE_LOG_TRACE3, MPE_MOD_SI,
            "<%s::parseAndUpdatePMT> - Pcr_Pid= x%x.\n", PSIMODULE, pcr_pid);

    if ((pcr_pid > CAT_PID) && (pcr_pid < MIN_VALID_PID))
    {
        //*** valid pcr pids are PMT_PID thru CAT_PID,MIN_VALID_PID thru MAX_VALID_PID
        //*** and NO_PCR_PID.  see ISO/IEC 13818-1:2000(E),pg.47, para 2.4.4.9,
        //*** pg. 19, table 2-3
        pmt_error = 13;
        goto PARSING_DONE;
    }

    //**** validate 4 reserved bits ****
    if (GETBITS(ptr_table_struc1-> program_info_length_octet11, 8, 5) != 0x0f)
    {
        // bits 8 thru are not set to 1 per ISO/IEC 13818-1:2000(E),pg.4, para 2.1.46)
        //pmt_error = 14 ;
        MPE_LOG(MPE_LOG_TRACE3,
                MPE_MOD_SI,
                "<%s::parseAndUpdatePMT> - *** WARNING *** 4-Bit-Reserved Field #5 not set to all ones(1)\n.",
                PSIMODULE);
    }

    //**** extract and validate the 12 bit program_info_length field from octets 11 and 12 ****
    //**** note, bits 12 and 11 must be set to zero,hence, max value is (2**10) -1 = 1023. ****
    program_info_length = (uint16_t)(
            ((ptr_table_struc1->program_info_length_octet11 & 0x0f) << 8)
                    | ptr_table_struc1->program_info_length_octet12);

    MPE_LOG(MPE_LOG_TRACE3,
            MPE_MOD_SI,
            "<%s::parseAndUpdatePMT> program_info_length octet11 = %d, program_info_length_octet12 = %d\n.",
            PSIMODULE, ptr_table_struc1->program_info_length_octet11,
            ptr_table_struc1->program_info_length_octet12);

    if (program_info_length > MAX_PROGRAM_INFO_LENGTH)
    {
        pmt_error = 15;
        goto PARSING_DONE;
    }

    //**** section_length identifies the total number of bytes/octets remaining ****
    //**** in the PMT after octet3. Before processing the descriptor section    ****
    //**** below,we must adjust section_length to account for having processed  ****
    //**** octets4 thru octet12.
    section_length -= (sizeof(pmt_table_struc1) - LEN_OCTET1_THRU_OCTET3);

    if ((section_length <= 0) || (program_info_length > section_length))
    { // error, section_length too short or program_info_length too long.
        pmt_error = 16;
        goto PARSING_DONE;
    }
    MPE_LOG(MPE_LOG_TRACE3,
            MPE_MOD_SI,
            "<%s::parseAndUpdatePMT> - Outer Descriptor Loop Length(program_info_length): %d\n",
            PSIMODULE, program_info_length);

    //**********************************************************************************************
    // Get the descriptor octets which immediately following the program_info_length field.
    // The total number of descriptor bytes/octets is defined by program_info_length.
    // Note: for the june-07-2004 release, we will not process the block of descriptors.Hence,
    // it is necessary to jump over/ignore the descriptor block.
    //**********************************************************************************************

    section_length -= program_info_length;
    ptr_input_buffer = &section_data[sizeof(pmt_table_struc1)];

    switch(tuner_type)
    {
        case SITP_PSI_TUNER_TYPE_OOB:
        {
            frequency = MPE_SI_OOB_FREQUENCY;
        }
            break;
        case SITP_PSI_TUNER_TYPE_DSG:
        {
            sitp_dsg_tunnel_info_t* dsg_info = (sitp_dsg_tunnel_info_t*) psi_params;
            frequency = MPE_SI_DSG_FREQUENCY;
            dsg_app_id = dsg_info->dsg_app_id;
        }
            break;
        case SITP_PSI_TUNER_TYPE_HN:
        {
            sitp_hn_stream_info_t* hn_info = (sitp_hn_stream_info_t*) psi_params;
            frequency = MPE_SI_HN_FREQUENCY;
            hn_stream_session = hn_info->hn_stream_session;
        }
            break;
        case SITP_PSI_TUNER_TYPE_IB:
        {
            sitp_ib_tune_info_t* tune_info = (sitp_ib_tune_info_t*) psi_params;
            frequency = tune_info->frequency;
            mode = tune_info->modulation_mode;
            table_type = IB_PMT;
        }
            break;
        default:
            break;
    }

    // legacy OOB case
    if(frequency == MPE_SI_OOB_FREQUENCY)
    {
        (void) mpe_siGetProgramEntryFromTransportStreamEntry(
                ts_handle, program_number, &si_program_handle);

        sidbVersionNumber = getSidbVersionNumber(table_type, si_program_handle);
        sidbCrc32 = getSidbCRC(table_type, si_program_handle);
        MPE_LOG(MPE_LOG_TRACE3,
            MPE_MOD_SI,
            "<%s::parseAndUpdatePMT> - OOB sidbVersionNumber = %d, sidbCrc32 = 0x%x\n",
			PSIMODULE,
            sidbVersionNumber,
            sidbCrc32);

        (void) mpe_siSetPMTVersion(si_program_handle, version_number);
        mpe_siSetPCRPid(si_program_handle, pcr_pid);
    }
    else if ((frequency == MPE_SI_DSG_FREQUENCY) && (dsg_app_id > 0))
    {
        // If DSG get program entry pertaining to the appId
        mpe_Bool isApp = false;
        mpe_SiProgramHandle other_program;
        // This is PSI for DSG app tunnel
        // Get DSG service entry
        retCode = mpe_siGetServiceEntryFromAppIdProgramNumber(dsg_app_id, program_number,
                &si_service_handle);
        if (retCode == MPE_SI_NOT_FOUND)
        {
            retCode = mpe_siCreateDSGServiceHandle(
                    (uint32_t) dsg_app_id, program_number,
                    NULL, NULL, &si_service_handle);
        }
        (void) mpe_siGetProgramEntryFromTransportStreamAppIdProgramNumber(
                ts_handle, dsg_app_id, program_number,
                &si_program_handle);

        isApp = true;
        if (si_service_handle != MPE_SI_INVALID_HANDLE)
        {
            if (mpe_siGetProgramHandleFromServiceEntry(si_service_handle,
                    &other_program) == MPE_SI_SUCCESS)
            {
                if (other_program != si_program_handle)
                {
                    mpe_SiTakeFromProgram(other_program, si_service_handle);
                    mpe_SiGiveToProgram(si_program_handle, si_service_handle);
                }
            }
        }
        mpe_siSetAppId(si_service_handle, dsg_app_id);
        mpe_siSetAppType(si_service_handle, isApp);
        sidbVersionNumber = getSidbVersionNumber(table_type, si_program_handle);
        sidbCrc32 = getSidbCRC(table_type, si_program_handle);
        MPE_LOG(MPE_LOG_TRACE3,
            MPE_MOD_SI,
            "<%s::parseAndUpdatePMT> - DSG sidbVersionNumber = %d, sidbCrc32 = 0x%x\n",
			PSIMODULE,
            sidbVersionNumber,
            sidbCrc32);

        (void) mpe_siSetPMTVersion(si_program_handle, version_number);
        mpe_siSetPCRPid(si_program_handle, pcr_pid);
    }
    else if ((frequency == MPE_SI_HN_FREQUENCY) &&  (hn_stream_session > 0))
    {
        // Get service entry
        retCode = mpe_siGetServiceEntryFromHNstreamProgramNumber(hn_stream_session,
                program_number, &si_service_handle);
        if (retCode == MPE_SI_NOT_FOUND)
        {
            MPE_LOG(MPE_LOG_WARN,
                    MPE_MOD_SI,
                    "<%s::parseAndUpdatePMT> - Unable to find HN Stream Service Entry for SessionId = %d, Program = %d\n",
                    PSIMODULE,
                    hn_stream_session,
                    program_number);
        }
        else
        {
            mpe_SiProgramHandle other_program;
            (void) mpe_siGetProgramEntryFromTransportStreamHNstreamProgramNumber(
                    ts_handle, hn_stream_session, program_number,
                    &si_program_handle);

            if (si_service_handle != MPE_SI_INVALID_HANDLE)
            {
                if (mpe_siGetProgramHandleFromServiceEntry(si_service_handle,
                        &other_program) == MPE_SI_SUCCESS)
                {
                    if (other_program != si_program_handle)
                    {
                        mpe_SiTakeFromProgram(other_program, si_service_handle);
                        mpe_SiGiveToProgram(si_program_handle,
                                si_service_handle);
                    }
                }
            }

			sidbVersionNumber = getSidbVersionNumber(table_type, si_program_handle);
            sidbCrc32 = getSidbCRC(table_type, si_program_handle);

            MPE_LOG(MPE_LOG_TRACE3,
                    MPE_MOD_SI,
                    "<%s::parseAndUpdatePMT> - HN sidbVersionNumber = %d, sidbCrc32 = 0x%x\n",
			        PSIMODULE,
                    sidbVersionNumber,
                    sidbCrc32);

            (void) mpe_siSetPMTVersion(si_program_handle, version_number);
            mpe_siSetPCRPid(si_program_handle, pcr_pid);
        }
    }
    else
    {
        // Fetch program handle from service handle, to ensure there's at least 1 service associated with the program.
        if (mpe_siGetProgramEntryFromFrequencyProgramNumberModulation(
                frequency, program_number, mode,
                &si_program_handle)== MPE_SI_SUCCESS)
        {
		    sidbVersionNumber = getSidbVersionNumber(table_type, si_program_handle);
            sidbCrc32 = getSidbCRC(table_type, si_program_handle);
            MPE_LOG(MPE_LOG_TRACE3,
                    MPE_MOD_SI,
                    "<%s::parseAndUpdatePMT> - IB sidbVersionNumber = %d, sidbCrc32 = 0x%x\n",
			        PSIMODULE,
                    sidbVersionNumber,
                    sidbCrc32);

            (void) mpe_siSetPMTVersion(si_program_handle, version_number);
            mpe_siSetPCRPid(si_program_handle, pcr_pid);
        }
    }

    // Parse the program element  outer descriptors.
    while (program_info_length)
    {
        // Get the descriptor tag and length and point to the contents.
        ptr_descriptor_struc = (generic_descriptor_struc *) ptr_input_buffer;
        descriptor_tag = ptr_descriptor_struc->descriptor_tag_octet1;
        descriptor_length = ptr_descriptor_struc->descriptor_length_octet2;
        ptr_input_buffer += sizeof(generic_descriptor_struc);

        MPE_LOG(MPE_LOG_TRACE3,
                MPE_MOD_SI,
                "<%s::parseAndUpdatePMT> - Call mpe_siSetOuterDescriptor, setting outer tag: %d, length: %d.\n",
                PSIMODULE, descriptor_tag, descriptor_length);
        if (si_program_handle != MPE_SI_INVALID_HANDLE)
        {
            // Set the descriptor in the SIDB.
            (void) mpe_siSetOuterDescriptor(si_program_handle,
                    (mpe_SiDescriptorTag) descriptor_tag,
                    (uint32_t) descriptor_length, (void *) ptr_input_buffer);
        }

        // Adjust the program info length for the tag, length and contents.
        // Point the input buffer to the next descriptor.
        program_info_length = (uint16_t)(program_info_length
                - sizeof(generic_descriptor_struc));
        program_info_length = (uint16_t)(program_info_length
                - descriptor_length);
        ptr_input_buffer = ptr_input_buffer + descriptor_length;
    }

    //
    // TEST CODE - REMOVE ME - TEST CODE - REMOVE ME - TEST CODE - REMOVE ME - TEST CODE - REMOVE ME -
    // v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v

    if (FALSE)
    {
        // Set an outer CA descriptor

        // Length of this array should be length+2
        uint8_t test_ca_descriptor[6] = { /* tag */ 0x09,
                                           /* length */ 0x04,
                                           /* CA_system_ID */ 0x00, 0x02,
                                           /* reserved + CA_PID */ 0x00, 0x42 };

        uint8_t test_ca_descriptor_content[4] = {/* CA_system_ID */ 0x00, 0x02,
                                           /* reserved + CA_PID */ 0x00, 0x42 };
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "<%s::parseAndUpdatePMT> - SETTING TEST CA DESCRIPTOR: %02x %02x %02x %02x %02x %02x ...\n",
                PSIMODULE, test_ca_descriptor[0], test_ca_descriptor[1],test_ca_descriptor[2],test_ca_descriptor[3],
                test_ca_descriptor[4],test_ca_descriptor[5]) ;

        // Set the descriptor in the SIDB.
        mpe_siSetOuterDescriptor(si_program_handle, (mpe_SiDescriptorTag)9,
                                      sizeof(test_ca_descriptor_content),  (void *)test_ca_descriptor_content);

        uint8_t test_ca_descriptor_2_content[7] = {/* CA_system_ID */ 0x00, 0x02,
												 /* reserved + CA_PID */ 0x00, 0x43 ,
												 /* private_data_bytes */ 0xde, 0xee, 0xed};
        if (FALSE)
            // Set the descriptor in the SIDB.
            mpe_siSetOuterDescriptor(si_program_handle, (mpe_SiDescriptorTag)9,
                                          sizeof(test_ca_descriptor_2_content),  (void *)test_ca_descriptor_2_content);
    }

    // ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^
    // TEST CODE - REMOVE ME - TEST CODE - REMOVE ME - TEST CODE - REMOVE ME - TEST CODE - REMOVE ME -
    //

    MPE_LOG(MPE_LOG_TRACE3,
            MPE_MOD_SI,
            "<%s::parseAndUpdatePMT> - Setting length past descriptor data: %d, adjust section_length.\n",
            PSIMODULE, section_length);

    //*****************************************************************************************
    //* This is the "FOR N1 Loop" in spec (ISO/IEC 13818-1:2000(E),pg.46, table 2-28)
    //* Note: the "- LENGTH_CRC_32" in the line immediately below is required to prevent
    //* the CRC_32 from being parsed as elementary stream header (aka: pmt_table_struc3) .
    //*****************************************************************************************

    for (; ((section_length - LENGTH_CRC_32) >= sizeof(pmt_table_struc3));)
    {
        ptr_table_struc3 = (pmt_table_struc3 *) ptr_input_buffer;

        //**** extract and validate the stream_type ****
        stream_type = ptr_table_struc3->stream_type_octet1;

        MPE_LOG(MPE_LOG_TRACE3, MPE_MOD_SI,
                "<%s::parseAndUpdatePMT> - Stream_Type = x%2x.\n", PSIMODULE,
                stream_type);

        if (!(((stream_type >= MIN_VALID_NONPRIVATE_STREAM_TYPE)
                && (stream_type <= MAX_VALID_NONPRIVATE_STREAM_TYPE))
                || ((stream_type >= MIN_VALID_PRIVATE_STREAM_TYPE)
                        && (stream_type <= MAX_VALID_PRIVATE_STREAM_TYPE))))
        {
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_SI,
                    "<%s::parseAndUpdatePMT> - Invalid Stream_Type = x%2x.\n",
                    PSIMODULE, stream_type);
        }

#ifndef SA_NON_CONFORMANCE_ISSUE
        if ((pcr_pid == NO_PCR_PID) && (stream_type
                >= MIN_VALID_NONPRIVATE_STREAM_TYPE) && (stream_type
                <= MAX_VALID_NONPRIVATE_STREAM_TYPE))
        { //**** error,NO_PCR_PID may only be used with "private streams. ****
            //**** see ISO/IEC 13818-1:2000(E),pg.47, PCR_PID.              ****
            pmt_error = 20;
            goto PARSING_DONE;
        }
#endif

        //**** validate 3 reserved bits ****
        if (GETBITS(ptr_table_struc3->elementary_pid_octet2, 8, 6) != 0x07)
        {
            // bits 8,7 & 6 are not set to 1 per ISO/IEC 13818-1:2000(E),pg.4, para 2.1.46)
            //pmt_error = 21;
            //break ;
            MPE_LOG(MPE_LOG_TRACE3,
                    MPE_MOD_SI,
                    "<%s::parseAndUpdatePMT> - *** WARNING *** 3-Bit-Reserved Field #5 not set to all ones(1)\n.",
                    PSIMODULE);
        }

        //**** get elementary_pid (bits 5-1 in octet2 and 8-1 in octet3 ****
        elementary_pid = (uint16_t)(((ptr_table_struc3->elementary_pid_octet2
                & 0x1f) << 8) | ptr_table_struc3->elementary_pid_octet3);

        MPE_LOG(MPE_LOG_TRACE3, MPE_MOD_SI,
                "<%s::parseAndUpdatePMT> - Elementary_Pid= x%x.\n", PSIMODULE,
                elementary_pid);

        //**** validate 4 reserved bits ****
        if (GETBITS(ptr_table_struc3->es_info_length_octet4, 8, 5) != 0x0f)
        {
            // bits 8,7,6 & 5 are not set to 1 per ISO/IEC 13818-1:2000(E),pg.4, para 2.1.46)
            //pmt_error = 22;
            //break ;
            MPE_LOG(MPE_LOG_TRACE3,
                    MPE_MOD_SI,
                    "<%s::parseAndUpdatePMT> - *** WARNING *** 4-Bit-Reserved Field #6 not set to all ones(1)\n.",
                    PSIMODULE);
        }

        //**** extract and validate the 12 bit es_info_length field from octets 4 & 5 ****
        //**** note, bits 12 and 11 must be set to zero,hence, max value is           ****
        //**** (2**10) -1 = 1023.                                                     ****
        es_info_length
                = ((ptr_table_struc3->es_info_length_octet4 & 0x0f) << 8)
                        | ptr_table_struc3->es_info_length_octet5;

        MPE_LOG(MPE_LOG_TRACE3, MPE_MOD_SI,
                "<%s::parseAndUpdatePMT> - es_info_length=%d.\n", PSIMODULE,
                es_info_length);

        if (es_info_length > MAX_ES_INFO_LENGTH)
        {
            MPE_LOG(MPE_LOG_TRACE3,
                    MPE_MOD_SI,
                    "<%s::parseAndUpdatePMT> - *** WARNING *** bits 11 & 12 not 0\n",
                    PSIMODULE);
            //pmt_error = 23 ;
            //goto PARSING_DONE;

            /* removing reserve bits */
            es_info_length = es_info_length & 0x03ff;
        }

        //**** update section_length and ptr to input buffer to account for the last ****
        //**** sizeof(pmt_table_struc3) octets (i.e.,stream_type thru es_info_len)   ****
        //**** and get the descriptor block associated with the elementary stream.   ****
        section_length -= sizeof(pmt_table_struc3);
        ptr_input_buffer += sizeof(pmt_table_struc3);

        if (es_info_length > section_length)
        { // error, descriptor(s) are outside of the PMT .
            MPE_LOG(MPE_LOG_TRACE3,
                    MPE_MOD_SI,
                    "<%s::parseAndUpdatePMT> - *** ERROR *** es_info_length(%d) > section_length(%d)\n",
                    PSIMODULE, es_info_length, section_length);
            pmt_error = 24;
            goto PARSING_DONE;
        }

        if (si_program_handle != MPE_SI_INVALID_HANDLE)
        {
            // allocates memory to hold the stream information, hence,
            // we only need to do it once per strea_type.
            (void) mpe_siSetElementaryStream(si_program_handle, stream_type,
                    elementary_pid);
            //*** RR added on 09-09-04 in support of org.ocap.si
            mpe_siSetESInfoLength(si_program_handle, elementary_pid,
                    stream_type, es_info_length);
        }

        //*****************************************************************************************
        //* This is the "FOR N2 Loop" in spec (ISO/IEC 13818-1:2000(E),pg.46, table 2-28)
        //* Note: if es_info_length = 0, the N2 Loop won't be entered.Hence, no data associated
        //* with the elementary_stream defined by elementary_pid (in pmt_table_struc3) will be
        //* written to the SIDB.
        //*****************************************************************************************
        for (; es_info_length >= sizeof(generic_descriptor_struc);)
        {
            ptr_descriptor_struc
                    = (generic_descriptor_struc *) ptr_input_buffer;
            descriptor_tag = ptr_descriptor_struc->descriptor_tag_octet1;
            descriptor_length = ptr_descriptor_struc->descriptor_length_octet2;

            //**** account for the "N" (sizeof(generic_descriptor_struc) bytes/octet ****
            //**** that we just acquired/processed.                                  ****
            section_length -= sizeof(generic_descriptor_struc);
            ptr_input_buffer += sizeof(generic_descriptor_struc);
            es_info_length -= sizeof(generic_descriptor_struc);

            if (descriptor_length > es_info_length)
            { // error, descriptor is outside of the elementary stream block .
                pmt_error = 25;
                goto PARSING_DONE;
            }

            //**** Move the descriptor data into the SI database.             ****
            //**** Note: if descriptor_length is zero (0), the descriptor tag ****
            //**** and length will still be passed to the SIDB and zero bytes ****
            //**** will be written to the database.                           ****
            if (si_program_handle != MPE_SI_INVALID_HANDLE)
            {
                (void) mpe_siSetDescriptor(si_program_handle, elementary_pid,
                        stream_type, descriptor_tag, descriptor_length,
                        ptr_input_buffer);

            }
            //**** prepare to process the next descriptor ****
            section_length -= descriptor_length;
            ptr_input_buffer += descriptor_length;
            es_info_length = es_info_length - descriptor_length;

            if ((es_info_length > 0) && (es_info_length
                    < sizeof(generic_descriptor_struc)))
            { // error, remaining octets/bytes can't hold a complete generic_descriptor_struc .
                pmt_error = 26;
                goto PARSING_DONE;
            }
        } //end of N2 Loop

        //
        // TEST CODE - REMOVE ME - TEST CODE - REMOVE ME - TEST CODE - REMOVE ME - TEST CODE - REMOVE ME -
        // v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v  v

        if (FALSE)
        {
            // Set inner CA descriptor(s)

            // Length of this array should be length+2
            uint8_t test_ca_descriptor[6] = { /* tag */ 0x09,
                                              /* length */ 0x04,
                                              /* CA_system_ID */ 0x00, 0x02,
                                              /* reserved + CA_PID */ 0x00, 0x42 };

            uint8_t test_ca_descriptor_content[4] = { /* CA_system_ID */ 0x00, 0x02,
                                              /* reserved + CA_PID */ 0x00, 0x42 };

            MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "<%s::parseAndUpdatePMT> - SETTING INNER TEST CA DESCRIPTOR: %02x %02x %02x %02x %02x %02x ...\n",
                    PSIMODULE, test_ca_descriptor[0], test_ca_descriptor[1],test_ca_descriptor[2],test_ca_descriptor[3],
                    test_ca_descriptor[4],test_ca_descriptor[5] ) ;

            // Set the descriptor in the SIDB.
            mpe_siSetDescriptor( si_program_handle, elementary_pid, stream_type, (mpe_SiDescriptorTag)9,
                                 sizeof(test_ca_descriptor_content),  (void *)test_ca_descriptor_content) ;

            uint8_t test_ca_descriptor_2_content[7] = { /* CA_system_ID */ 0x00, 0x02,
                                                 /* reserved + CA_PID */ 0x00, 0x43 ,
                                                 /* private_data_bytes */ 0xde, 0xee, 0xed};

            if (FALSE)
                // Set the descriptor in the SIDB.
                mpe_siSetDescriptor( si_program_handle, elementary_pid, stream_type, (mpe_SiDescriptorTag)9,
                                     sizeof(test_ca_descriptor_2_content),  (void *)test_ca_descriptor_2_content) ;
        }

        // ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^
        // TEST CODE - REMOVE ME - TEST CODE - REMOVE ME - TEST CODE - REMOVE ME - TEST CODE - REMOVE ME -
        //
    } //end_of N1 Loop

    //*************************************************************************************************
    //* At this point, the code either terminated prematurely or successfully processed the PMT.
    //* We need to validate the present of the CRC_32 field and cleanup before returning to the caller.
    //*************************************************************************************************
    MPE_LOG(MPE_LOG_TRACE3, MPE_MOD_SI,
            "<%s::parseAndUpdatePMT> - section_length = %d.\n", PSIMODULE,
            section_length);

    if (section_length > LENGTH_CRC_32)
    {
        /*
         ** error, more data (pmt_table_struct3) remaining than just the CRC_32,
         ** see beginning of the "FOR N2 LOOp" above.
         */
        pmt_error = 27;
    }

    MPE_LOG(MPE_LOG_TRACE3, MPE_MOD_SI,
            "<%s::parseAndUpdatePMT> - ptr_input_buffer = 0x%p\n", PSIMODULE,
            ptr_input_buffer);
    if (section_length == LENGTH_CRC_32)
    {
        crc = ((*ptr_input_buffer) << 24 | (*(ptr_input_buffer + 1)) << 16
                | (*(ptr_input_buffer + 2)) << 8 | (*(ptr_input_buffer + 3)));
        MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
                "<%s::parseAndUpdatePMT> - CRC32 = 0x%08x\n", PSIMODULE, crc);
    }

    if (si_program_handle != MPE_SI_INVALID_HANDLE)
    {
        mpe_siSetPMTCRC(si_program_handle, crc);
    }

    PARSING_DONE:

    if (si_program_handle != MPE_SI_INVALID_HANDLE)
    {
        mpe_SiServiceHandle service_handle;
        publicSIIterator iter;

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<%s::parseAndUpdatePMT> - pmt_error: %d\n", PSIMODULE,
                pmt_error);

        if (pmt_error != 0)
        {
            (void) mpe_siSetPMTStatusNotAvailable(si_program_handle);
            return pmt_error;
        }

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
                "<%s::parseAndUpdatePMT> - Calling mpe_siSetPMTStatus\n",
                PSIMODULE);
        mpe_siSetPMTStatus(si_program_handle);
        //
        // Need to loop through services attached to the program here, and notify each one.
        // This is new, because now more than one service can point to a program.
        //
        service_handle = mpe_siFindFirstServiceFromProgram(si_program_handle,
                (SIIterator*) &iter);

        while (service_handle != MPE_SI_INVALID_HANDLE)
        {
            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_SI,
                    "<%s::parseAndUpdatePMT> - Calling notifySIDB for service_handle: 0x%x\n",
                    PSIMODULE, service_handle);

            notifySIDB(table_type, sidbVersionNumber == INIT_TABLE_VERSION,
                    sidbVersionNumber != version_number,
                    sidbCrc32 != crc, service_handle);
            service_handle = mpe_siGetNextServiceFromProgram((SIIterator*) &iter);
        }
    }
    //Print the pmt entries

    MPE_LOG(MPE_LOG_TRACE1,
            MPE_MOD_SI,
            "<%s::parseAndUpdatePMT> - Exiting, Program# = %d  had ErrorCode = %d.\n",
            PSIMODULE, program_number, pmt_error);

    return pmt_error;
}

//***************************************************************************************************
//  Subroutine/Method:                       parseAndUpdateNIT()
//***************************************************************************************************
/**
 * Parse NIT table section and update SI database.
 *
 * @param  ptr_TableSection,  a pointer to the start of the NIT table section.
 * @param  sectiontable_size, The number of bytes in the table section per the PowerTV OS.
 *
 * @return MPE_SUCCESS if table section was successfully processed/parsed .Otherwise an error code
 *         greater than zero (MPE_SUCCESS).
 */
mpe_Error parseAndUpdateNIT(mpe_FilterSectionHandle section_handle,
        uint8_t *version, uint8_t *section_number,
        uint8_t *last_section_number, uint32_t *crc)
{

    mpe_Error retCode = SITP_SUCCESS;
    nit_table_struc1 *ptr_nit_table_struc1;
    generic_descriptor_struc *ptr_descriptor_struc;
    revision_detection_descriptor_struc *ptr_rdd_table_struc1 = NULL;

    uint16_t number_of_records = 0, table_subtype = 0, first_index = 0;
    uint32_t section_length = 0, number_bytes_copied = 0;
    mpe_Error nit_error = NO_PARSING_ERROR;
    uint8_t input_buffer[MAX_OOB_SMALL_TABLE_SECTION_LENGTH];
    uint8_t *ptr_input_buffer = NULL;

    //***********************************************************************************************
    //***********************************************************************************************

    //**********************************************************************************************
    // Parse and validate the first 56 bytes (Table_ID thru Last_Section_Number) of the NIT
    // table (see document ANSI/SCTE 65-2002 (formerly DVS 234), chap 5.1.1,pg.15) .
    //**********************************************************************************************
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::parseAndUpdateNIT> - Entered method parseAndUpdateNIT.\n",
            SIMODULE);

    retCode = mpeos_filterSectionRead(section_handle, 0,
            sizeof(nit_table_struc1), 0, input_buffer,
            (uint32_t *) &number_bytes_copied);

    if ((MPE_SUCCESS != retCode) || (number_bytes_copied
            != sizeof(nit_table_struc1)))
    {
        nit_error = 1;
        goto PARSING_DONE;
    }
    else
    {
        ptr_input_buffer = input_buffer;
        ptr_nit_table_struc1 = (nit_table_struc1 *) ptr_input_buffer;

        //**** validate table Id field ****
        if (ptr_nit_table_struc1->table_id_octet1
                != NETWORK_INFORMATION_TABLE_ID)
        {
            nit_error = 2;
        }
        //**** validate "Zero" (2-bits) field ****
        if (TESTBITS(ptr_nit_table_struc1->section_length_octet2, 8, 7))
        {
            // bits not set to 0.
            //nit_error = 3 ;
            MPE_LOG(
                    MPE_LOG_WARN,
                    MPE_MOD_SI,
                    "<%s::parseAndUpdateNIT> - *** WARNING *** 2-Bit-Zero Field #1 not set to all zeroes(0)\n.",
                    SIMODULE);
        }
        //**** validate 2 reserved bits ****
        if (GETBITS(ptr_nit_table_struc1->section_length_octet2, 6, 5) != 3)
        {
            // bits 5 and 6 are not set to 1 per ANSI/SCTE 65-2002,pg.12, para 4.3
            //nit_error = 4 ;
            MPE_LOG(
                    MPE_LOG_WARN,
                    MPE_MOD_SI,
                    "<%s::parseAndUpdateNIT> - *** WARNING *** 2-Bit-Reserve Field #2 not set to all ones(1)\n.",
                    SIMODULE);
        }
        //**** extract and validate section_length field (octets 2 & 3)         ****
        //**** note, the maximum size of a NIT excluding octet 1 thru 3 is 1021.****
        //**** and occupies only bits 1 thru of the 12 bits                     ****

        section_length = ((GETBITS(ptr_nit_table_struc1->section_length_octet2,
                4, 1)) << 8) | ptr_nit_table_struc1->section_length_octet3;
        MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                "<%s::parseAndUpdateNIT> - Section_Length = (%d)\n.", SIMODULE,
                section_length);

        if (section_length > 1021)
        { // warning, section_length too large.
            MPE_LOG(
                    MPE_LOG_WARN,
                    MPE_MOD_SI,
                    "<%s::parseAndUpdateNIT> - *** WARNING *** Section length larger than 1021\n.",
                    SIMODULE);
        }

        //**** verify that the "Zero" (3-bits) and the Protocol Version fields ****
        //**** are set to zero.                                                ****

        if (ptr_nit_table_struc1->protocol_version_octet4 != 0)
        {
            // one or more of the eight bits are set to 1.
            nit_error = 6;

        }
        //**** validate the first_index field) *****

        if ((first_index = ptr_nit_table_struc1->first_index_octet5) == 0)
        {
            // error, must be > zero .
            nit_error = 7;
        }

        //**** number_of_records field ****

        number_of_records = ptr_nit_table_struc1->number_of_records_octet6;

        // Fix for bug 3251 (empty NIT table is allowed)
        /*
         if (number_of_records == 0)
         {
         // error, number_of_records must be > zero .
         nit_error = 8 ;
         }
         */
        MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
                "<%s::parseAndUpdateNIT> - Number_Records = (%d).\n", SIMODULE,
                number_of_records);

        //**** extract and validate the transmission_medium ****

        if (TESTBITS(ptr_nit_table_struc1->table_subtype_octet7, 8, 5) != 0)
        {
            // bits 8 thru 5 shall be set to zero (0) per ANSI/SCTE 65-2002,pg.14, para 5.1
            nit_error = 9;
        }

        //**** extract and validate the table_subtype field ****

        table_subtype = (uint16_t)(GETBITS(
                ptr_nit_table_struc1->table_subtype_octet7, 4, 1));
        MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
                "<%s::parseAndUpdateNIT> - Table_Subtype = (%d).\n", SIMODULE,
                table_subtype);

        if ((table_subtype < CARRIER_DEFINITION_SUBTABLE) || (table_subtype
                > MODULATION_MODE_SUBTABLE))
        {
            // error, invalid table_subtype value.
            nit_error = 10;
        }

        /*
         ** ** section_length identifies the total number of bytes/octets remaining **
         ** ** in the NIT after octet3. Before processing CDS/MMS records ,we must  **
         ** ** adjust section_length to account for having  processed octets4 thru  **
         ** ** octet7 and octet1 thru octet7, respectively.                         **
         */
        section_length -= (sizeof(nit_table_struc1) - LEN_OCTET1_THRU_OCTET3);
        MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                "<%s::parseAndUpdateNIT> - section_length = (%d).\n", SIMODULE,
                section_length);

        /*
         ** read the rest of the table section
         */
        retCode = mpeos_filterSectionRead(section_handle, 7, section_length, 0,
                input_buffer, (uint32_t *) &number_bytes_copied);

        if ((MPE_SUCCESS != retCode) || (number_bytes_copied != section_length))
        {
            nit_error = 11;
            goto PARSING_DONE;
        }
    }

    ptr_input_buffer = input_buffer;

    //**********************************************************************************************
    // Parse the remaining bytes/octets in the NIT.
    //**********************************************************************************************

    if (nit_error == NO_PARSING_ERROR)
    {
        //****************************************************************************************
        //**** Process each CDS or MMS Record
        //****************************************************************************************

        if (table_subtype == CARRIER_DEFINITION_SUBTABLE)
        {
            nit_error = parseCDSRecords(number_of_records, first_index,
                    &ptr_input_buffer, &section_length);
        }
        else
        {
            nit_error = parseMMSRecords(number_of_records, first_index,
                    &ptr_input_buffer, &section_length);
        }
    }

    //*************************************************************************************************
    //* At this point, there may be a series a descriptors present.However, we are not required to
    //* process them for the 06-07-04 PowerTV release. If there are descriptors present, we will
    //* "ignore" them.
    //*************************************************************************************************
    if ((nit_error == NO_PARSING_ERROR) && (section_length > LENGTH_CRC_32))
    {
        ptr_rdd_table_struc1
                = (revision_detection_descriptor_struc *) ptr_input_buffer;

        if (ptr_rdd_table_struc1->descriptor_tag_octet1 == 0x93)
        {
            MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                    "<%s::parseAndUpdateNIT> - ptr to version: 0x%p\n",
                    SIMODULE,
                    &(ptr_rdd_table_struc1->table_version_number_octet3));
            *version = (uint8_t)(
                    ptr_rdd_table_struc1->table_version_number_octet3 & 0x1F);
            *section_number = ptr_rdd_table_struc1->section_number_octet4;
            *last_section_number
                    = ptr_rdd_table_struc1->last_section_number_octet5;
            MPE_LOG(
                    MPE_LOG_TRACE4,
                    MPE_MOD_SI,
                    "<%s::parseAndUpdateNIT> - version: %d, section_number: %d, last_section_number: %d\n",
                    SIMODULE, *version, *section_number, *last_section_number);
            ptr_input_buffer += sizeof(revision_detection_descriptor_struc);
            section_length -= sizeof(revision_detection_descriptor_struc);
        }
    }

    if (section_length > LENGTH_CRC_32)
    {
        do
        {
            uint32_t tempSize;

            ptr_descriptor_struc
                    = (generic_descriptor_struc *) ptr_input_buffer;
            tempSize = sizeof(generic_descriptor_struc)
                    + ptr_descriptor_struc->descriptor_length_octet2;
            ptr_input_buffer += tempSize;

            if (section_length < tempSize)
            { //error, section_length not large enough to accommodate descriptor(s) .
                nit_error = 90;
                break;
            }

            section_length -= tempSize;

        } while (section_length > LENGTH_CRC_32);
    }

    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
            "<%s::parseAndUpdateNIT> - ptr = 0x%p\n", SIMODULE,
            ptr_input_buffer);
    if (section_length == LENGTH_CRC_32)
    {
        //MPE_LOG (MPE_LOG_TRACE6, MPE_MOD_SI,"<%s::parseAndUpdateNIT> - crc bytes= %x = %d,%x = %d,%x = %d,%x = %d\n",
        //     SIMODULE,
        //    );

        *crc = ((*ptr_input_buffer) << 24 | (*(ptr_input_buffer + 1)) << 16
                | (*(ptr_input_buffer + 2)) << 8 | (*(ptr_input_buffer + 3)));
        MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
                "<%s::parseAndUpdateNIT> - CRC32 = 0x%08x\n", SIMODULE, *crc);
    }

    /*
     ** **************************************************************************
     ** At this point, if section_length = 0, the CRC_32 is present and the table
     ** section has been processed successfully. A section_length > 0 or
     ** section_length < 0  implies that an error has occurred and nit_error should
     ** identify the error condition.
     ** **************************************************************************
     */
    PARSING_DONE: MPE_LOG(
            MPE_LOG_TRACE4,
            MPE_MOD_SI,
            "<%s::parseAndUpdateNIT> - Exiting with a Nit_Error_Code = (%d).\n",
            SIMODULE, nit_error);

    return nit_error;
}

/*
 ** **************************************************************************************************
 **  Subroutine/Method:                       parseCDSRecords()
 ** **************************************************************************************************
 ** Parses CDS (Carriers Defination Subtable) records, validates the fileds within a CDS record,
 ** generates/decompresses the carrier frequencies and stores them in the SI database .
 **
 ** @param  number_of_records,  identifies the number of CDS records to be processed.
 ** @param  first_index,        identifies the index to be associated with the ist CDS record
 **                             to be processed.
 ** @param  pptr_TableSection,  the address of memeory location which contains a pointer to the NIT
 **                             table section.
 ** @param  sectiontable_size,  The number of bytes in the table section per the PowerTV OS.
 ** @param  ptr_section_length, a pointer to the caller's section_length variable.
 **
 ** @return MPE_SUCCESS if table section was successfully processed/parsed .Otherwise an error code
 **         greater than zero (MPE_SUCCESS).
 ** **************************************************************************************************
 */
mpe_Error parseCDSRecords(uint16_t number_of_records, uint16_t first_index,
        uint8_t ** handle_section_data, uint32_t * ptr_section_length)
{
    uint32_t i, j, descriptor_count;

    uint16_t number_of_carrier_frequencies = 0, carrier_frequency_spacing = 0,
            first_frequency = 0, carrier_frequency_count = 0,
            carrier_frequency_record = first_index;

    uint32_t carrier_frequency_spacing_units = 0, first_frequency_units = 0,
            array_of_frequencies[255], *ptr_frequency_array =
                    array_of_frequencies, base_frequency, delta_frequency;

    cds_record_struc2 *ptr_cds_record_struc2 = NULL;
    generic_descriptor_struc *ptr_descriptor_struc = NULL;
    mpe_Error nit_error = NO_PARSING_ERROR;

    //***********************************************************************************************
    //***********************************************************************************************
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::parseCDSRecords> - Entered method parseCDSRecords.\n",
            SIMODULE);

    /*
     ** read the rest of the table section
     */
    for (i = 0; (i < number_of_records) && (nit_error == NO_PARSING_ERROR); ++i)
    {
        //*******************************************************************************************
        // Extract and validate data from the specified cds records per table 5.3, pg. 16 in document
        // ANSI/SCTE 65-2002 (formerly DVS 234)
        //*******************************************************************************************

        ptr_cds_record_struc2 = (cds_record_struc2 *) *handle_section_data;

        // range is 1 thru 255
        number_of_carrier_frequencies
                = ptr_cds_record_struc2->number_of_carriers_octet1;
        MPE_LOG(
                MPE_LOG_TRACE4,
                MPE_MOD_SI,
                "<%s::parseCDSRecords> - number_of_carrier_frequencies(%d) = %d.\n",
                SIMODULE, i, number_of_carrier_frequencies);

        if (number_of_carrier_frequencies == 0)
        { // error, number_of_carriers should be > 0
            nit_error = 14;
            break;
        }

        if ((carrier_frequency_count + number_of_carrier_frequencies
                + first_index - 1) > MAX_CARRIER_FREQUENCY_INDEX)
        {
            /*
             ** error, will generate an index to a carrier frequence which is greater than 255.
             ** see spec ANSI/SCTE 65-2002 (formerly DVS 234), para 5.1.1, para. 3, pg. 16
             */
            nit_error = 15;
            break;
        }

        carrier_frequency_spacing_units = FREQUENCY_UNITS_10KHz;

        if (TESTBITS(ptr_cds_record_struc2->frequency_spacing_octet2, 8, 8))
        {
            carrier_frequency_spacing_units = FREQUENCY_UNITS_125KHz;
        }
        MPE_LOG(
                MPE_LOG_TRACE6,
                MPE_MOD_SI,
                "<%s::parseCDSRecords> - carrier_frequency_spacing_units(%d) = %d.\n",
                SIMODULE, i, carrier_frequency_spacing_units);

        if (TESTBITS(ptr_cds_record_struc2->frequency_spacing_octet2, 7, 7))
        { // error, "zero field" is set to "1"
            //nit_error = 16 ;
            //break ;
            MPE_LOG(
                    MPE_LOG_TRACE6,
                    MPE_MOD_SI,
                    "<%s::parseCDSRecords> - *** WARNING *** 1-Bit-Zero Field #1 not set to all zeroes(0)\n.",
                    SIMODULE);
        }

        //range is 1 thru 16,383 ((2**14) -1)
        carrier_frequency_spacing = (uint16_t)(TESTBITS(
                ptr_cds_record_struc2->frequency_spacing_octet2, 6, 1) << 8)
                | ptr_cds_record_struc2->frequency_spacing_octet3;
        MPE_LOG(
                MPE_LOG_TRACE6,
                MPE_MOD_SI,
                "<%s::parseCDSRecords> - carrier_frequency_spacing(%d) = %d.\n",
                SIMODULE, i, carrier_frequency_spacing);

        if ((number_of_carrier_frequencies != 1) && (carrier_frequency_spacing
                == 0))
        { // error, carrier_frequency_spacing must be > 0
            nit_error = 17;
            break;
        }

        first_frequency_units = FREQUENCY_UNITS_10KHz;

        if (TESTBITS(ptr_cds_record_struc2->first_carrier_frequency_octet4, 8,
                8))
        {
            first_frequency_units = FREQUENCY_UNITS_125KHz;
        }
        MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                "<%s::parseCDSRecords> - first_frequency_units(%d) = %d.\n",
                SIMODULE, i, first_frequency_units);

        // range is 0 thru 32,767 ((2**15) -1)
        first_frequency = (uint16_t)(TESTBITS(
                ptr_cds_record_struc2->first_carrier_frequency_octet4, 7, 1)
                << 8) | ptr_cds_record_struc2->first_carrier_frequency_octet5;
        MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                "<%s::parseCDSRecords> - first_frequency(%d) = %d.\n",
                SIMODULE, i, first_frequency);

        //****************************************************************************************************
        // Generate/uncompress the specified number of frequencies.
        //*****************************************************************************************************
        base_frequency = first_frequency * first_frequency_units;
        delta_frequency = carrier_frequency_spacing
                * carrier_frequency_spacing_units;

        if (getDumpTables())
            MPE_LOG(
                    MPE_LOG_INFO,
                    MPE_MOD_SI,
                    "<%s::parseCDSRecords> - Ent %-3u[#Carriers:%u SU:%u FirstCF:%ukHz FreqU:%u FreqS:%ukHz]\n",
                    SIMODULE, i, number_of_carrier_frequencies,
                    carrier_frequency_spacing_units, base_frequency / 1000, // Note: Is unit-adjusted first_frequency value
                    first_frequency_units, delta_frequency / 1000); // Note: Is unit-adjusted carrier_frequency_spacing value

        for (j = 0; j < number_of_carrier_frequencies; ++j, ++ptr_frequency_array, carrier_frequency_record++)
        {
            // compute and store the carrier frequency

            *ptr_frequency_array = base_frequency + (j * delta_frequency);
            if (getDumpTables())
            {
                MPE_LOG(
                        MPE_LOG_INFO,
                        MPE_MOD_SI,
                        "<%s::parseCDSRecords> -   CDS Record %3u: %ukHz channel at %ukHz\n",
                        SIMODULE, carrier_frequency_record, delta_frequency
                                / 1000, (*ptr_frequency_array) / 1000);
                MPE_LOG(
                        MPE_LOG_INFO,
                        MPE_MOD_SI,
                        "<%s::parseCDSRecords> - Record(%d)   Frequency(%d) = %u.\n",
                        SIMODULE, i, j + 1, *ptr_frequency_array);
            }
        }

        carrier_frequency_count = (uint16_t)(carrier_frequency_count
                + number_of_carrier_frequencies);
        MPE_LOG(
                MPE_LOG_TRACE6,
                MPE_MOD_SI,
                "<%s::parseCDSRecords> - carrier_frequency_count = %d, first_index = %d.\n",
                SIMODULE, carrier_frequency_count, first_index);

        //****************************************************************************************************
        // Prepare to process the descriptors if they are present.
        //****************************************************************************************************
        *handle_section_data = (*handle_section_data)
                + sizeof(cds_record_struc2);
        *ptr_section_length = (*ptr_section_length) - sizeof(cds_record_struc2);

        descriptor_count = **handle_section_data; // get 8-bit descriptor count and advance pointer
        *handle_section_data = ((*handle_section_data) + 1);
        *ptr_section_length = (*ptr_section_length) - 1;
        MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                "<%s::parseCDSRecords> - descriptor count = %d\n", SIMODULE,
                descriptor_count);

        while (descriptor_count > 0)
        { // ignore all descriptors .

            ptr_descriptor_struc
                    = (generic_descriptor_struc *) *handle_section_data;
            *handle_section_data += sizeof(generic_descriptor_struc)
                    + ptr_descriptor_struc->descriptor_length_octet2;

            //*** decrement ptr_section_length value as we did not account for descriptor tag, length
            //*** and data fields length and data fields prior to calling this subroutine .

            --descriptor_count;
            *ptr_section_length -= (sizeof(generic_descriptor_struc)
                    + ptr_descriptor_struc->descriptor_length_octet2);

            if ((int32_t) * ptr_section_length < 0)
            { //error, section_length not large enough to account for descriptor tags, length and data fields
                nit_error = 18;
                break;
            }
        } //end_of_while_loop
    } //end_of_for_loop


    //****************************************************************************************************
    //    Update the SI database with the frequencies.
    //****************************************************************************************************

    if (nit_error == NO_PARSING_ERROR)
    {
        mpe_siLockForWrite();
        nit_error = mpe_siSetCarrierFrequencies(array_of_frequencies,
                (uint8_t) first_index, (uint8_t) carrier_frequency_count);
        mpe_siReleaseWriteLock();

        if (nit_error != NO_PARSING_ERROR)
        {
            MPE_LOG(
                    MPE_LOG_TRACE6,
                    MPE_MOD_SI,
                    "<%s::parseCDSRecords> - Call to mpe_siSetCarrierFrequencies failed,ErrorCode = %d\n",
                    SIMODULE, nit_error);
        }
        else
        {
            MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                    "<%s::parseCDSRecords> - CDS records parsed, set flag.\n",
                    SIMODULE);

            //setTablesAcquired(getTablesAcquired() | CDS_ACQUIRED) ;
            // SI DB re-factor changes (7/11/05)
            //mpe_siNotifyTableAcquired(OOB_NIT_CDS, 0);
        }
    }

    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
            "<%s::parseCDSRecords> - section_length = %d.\n", SIMODULE,
            *ptr_section_length);
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::parseCDSRecords> - Exiting with a Nit_Error_Code = %d.\n",
            SIMODULE, nit_error);

    return nit_error;
}

//***************************************************************************************************
//  Subroutine/Method:                       parseMMSRecords()
//***************************************************************************************************
/**
 * Parses MMS(Modulation Mode Subtable) records, validates the fields within a MMS record
 * and stores the data in the SI database .
 *
 * @param  number_of_records,  identifies the number of MMS records to be processed.
 * @param  first_index,        identifies the index to be associated with the ist CDS record
 *                             to be processed.
 * @param  pptr_TableSection,  the address of memeory location which contains a pointer to the NIT
 *                             table section.
 * @param  sectiontable_size,  The number of bytes in the table section per the PowerTV OS.
 * @param  ptr_section_length, a pointer to the caller's section_length variable.
 *
 * @return MPE_SUCCESS if table section was successfully processed/parsed .Otherwise an error code
 *         greater than zero (MPE_SUCCESS).
 */
mpe_Error parseMMSRecords(uint16_t number_of_records, uint16_t first_index,
        uint8_t **handle_section_data, uint32_t *ptr_section_length)
{
    mms_record_struc2 *ptr_mms_record_struc2;
    uint16_t transmission_system = 0, inner_coding_mode = 0,
            split_bitstream_mode;
    uint32_t i = 0, descriptor_count = 0;
    uint32_t symbol_rate;

    mpe_SiModulationMode array_of_modulation_format[255],
            *ptr_modulation_array = array_of_modulation_format,
            modulation_format = 0;
    generic_descriptor_struc *ptr_descriptor_struc;
    mpe_Error nit_error = NO_PARSING_ERROR;
    char *modformatstring;

    //***********************************************************************************************
    //***********************************************************************************************
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::parseMMSRecords> - Entered method parseMMSRecords.\n",
            SIMODULE);

    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
            "<%s::parseMMSRecords> - first_index = %d.\n", SIMODULE,
            first_index);

    for (i = 0; (i < number_of_records) && (nit_error == NO_PARSING_ERROR); ++i)
    {
        //*******************************************************************************************
        // Extract and validate data from the specified cds records per table 5.6, pg. 18 in document
        // ANSI/SCTE 65-2002 (formerly DVS 234) //.
        //*******************************************************************************************

        ptr_mms_record_struc2 = (mms_record_struc2 *) *handle_section_data;

        transmission_system = (uint16_t)(GETBITS(
                ptr_mms_record_struc2->inner_coding_mode_octet1, 8, 5));
        MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                "<%s::parseMMSRecords> - transmission_system(%d) = (%d).\n",
                SIMODULE, i, transmission_system);

        /*        if (!((transmission_system == TRANSMISSION_SYSTEM_ITUT_ANNEXB)||
         (transmission_system == TRANSMISSION_SYSTEM_ATSC)         ) )
         {  // error,  invalid transmission_system value .
         nit_error = 30 ;
         break ;
         }
         */
        inner_coding_mode = (uint16_t)(GETBITS(
                ptr_mms_record_struc2->inner_coding_mode_octet1, 4, 1));
        MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                "<%s::parseMMSRecords> - inner_coding_mode(%d) = (%d).\n",
                SIMODULE, i, inner_coding_mode);

        if (!((inner_coding_mode <= INNER_CODING_MODE_1_2)
                || (inner_coding_mode == INNER_CODING_MODE_3_5)
                || (inner_coding_mode == INNER_CODING_MODE_2_3)
                || ((inner_coding_mode >= INNER_CODING_MODE_3_4)
                        && (inner_coding_mode <= INNER_CODING_MODE_5_6))
                || (inner_coding_mode == INNER_CODING_MODE_7_8)
                || (inner_coding_mode == DOES_NOT_USE_CONCATENATED_CODING)))
        { // error, inner_coding_mode.
            nit_error = 31;
            break;
        }

        split_bitstream_mode = NO;

        if (GETBITS(ptr_mms_record_struc2->modulation_format_octet2, 8, 8))
        {
            split_bitstream_mode = YES;
        }
        MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                "<%s::parseMMSRecords> - split_bitstream_mode(%d) = (%d).\n",
                SIMODULE, i, split_bitstream_mode);

        if (TESTBITS(ptr_mms_record_struc2->modulation_format_octet2, 7, 6))
        { // error, "one or more bits are set to "1".
            //nit_error = 32 ;
            //break ;
            MPE_LOG(
                    MPE_LOG_TRACE6,
                    MPE_MOD_SI,
                    "<%s::parseMMSRecords> - *** WARNING *** 2-Bit-Zero Field #1 not set to all zeroes(0)\n.",
                    SIMODULE);
        }

        modulation_format = GETBITS(
                ptr_mms_record_struc2->modulation_format_octet2, 5, 1);
        MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                "<%s::parseMMSRecords> - modulation_format(%d) = (%d).\n",
                SIMODULE, i, modulation_format);

        if (modulation_format > MODULATION_FORMAT_QAM_1024)
        { // error, invalid modulation_format .
            nit_error = 33;
            break;
        }

        if (TESTBITS(ptr_mms_record_struc2->modulation_symbol_rate_octet3, 8, 5))
        { // error, "one or more bits are set to "1"
            //nit_error = 34 ;
            //break ;
            MPE_LOG(
                    MPE_LOG_TRACE6,
                    MPE_MOD_SI,
                    "<%s::parseMMSRecords> - *** WARNING *** 4-Bit-Zero Field #2 not set to all zeroes(0)\n.",
                    SIMODULE);
        }

        symbol_rate = ((TESTBITS(
                ptr_mms_record_struc2->modulation_symbol_rate_octet3, 4, 1))
                << 24) | (ptr_mms_record_struc2->modulation_symbol_rate_octet4
                << 16) | (ptr_mms_record_struc2->modulation_symbol_rate_octet5
                << 8) | ptr_mms_record_struc2->modulation_symbol_rate_octet6;
        MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                "<%s::parseMMSRecords> - symbol_rate(%d) = (%d).\n", SIMODULE,
                i, symbol_rate);

        //****************************************************************************************************
        // Move Modulation mode/format into the modulation array.
        //****************************************************************************************************
        if (getDumpTables())
        {
            switch (modulation_format)
            {
            case MPE_SI_MODULATION_UNKNOWN:
                modformatstring = "unknown";
                break;
            case MPE_SI_MODULATION_QPSK:
                modformatstring = "QPSK";
                break;
            case MPE_SI_MODULATION_BPSK:
                modformatstring = "BPSK";
                break;
            case MPE_SI_MODULATION_OQPSK:
                modformatstring = "OQPSK";
                break;
            case MPE_SI_MODULATION_VSB8:
                modformatstring = "VSB-8";
                break;
            case MPE_SI_MODULATION_VSB16:
                modformatstring = "VSB-16";
                break;
            case MPE_SI_MODULATION_QAM16:
                modformatstring = "QAM-16";
                break;
            case MPE_SI_MODULATION_QAM32:
                modformatstring = "QAM-32";
                break;
            case MPE_SI_MODULATION_QAM64:
                modformatstring = "QAM-64";
                break;
            case MPE_SI_MODULATION_QAM80:
                modformatstring = "QAM-80";
                break;
            case MPE_SI_MODULATION_QAM96:
                modformatstring = "QAM-96";
                break;
            case MPE_SI_MODULATION_QAM112:
                modformatstring = "QAM-112";
                break;
            case MPE_SI_MODULATION_QAM128:
                modformatstring = "QAM-128";
                break;
            case MPE_SI_MODULATION_QAM160:
                modformatstring = "QAM-160";
                break;
            case MPE_SI_MODULATION_QAM192:
                modformatstring = "QAM-192";
                break;
            case MPE_SI_MODULATION_QAM224:
                modformatstring = "QAM-224";
                break;
            case MPE_SI_MODULATION_QAM256:
                modformatstring = "QAM-256";
                break;
            case MPE_SI_MODULATION_QAM320:
                modformatstring = "QAM-320";
                break;
            case MPE_SI_MODULATION_QAM384:
                modformatstring = "QAM-384";
                break;
            case MPE_SI_MODULATION_QAM448:
                modformatstring = "QAM-448";
                break;
            case MPE_SI_MODULATION_QAM512:
                modformatstring = "QAM-512";
                break;
            case MPE_SI_MODULATION_QAM640:
                modformatstring = "QAM-640";
                break;
            case MPE_SI_MODULATION_QAM768:
                modformatstring = "QAM-768";
                break;
            case MPE_SI_MODULATION_QAM896:
                modformatstring = "QAM-896";
                break;
            case MPE_SI_MODULATION_QAM1024:
                modformatstring = "QAM-1024";
                break;
            case MPE_SI_MODULATION_QAM_NTSC:
                modformatstring = "Analog";
                break;
            default:
                modformatstring = "RESERVED";
                break;
            }

            MPE_LOG(
                    MPE_LOG_INFO,
                    MPE_MOD_SI,
                    "<%s::parseMMSRecords> - Ent %-3d[TranSys:%d ICM:%d SBM:%d Mod:%d(%s) SymRate:%d]\n",
                    SIMODULE, i + first_index, transmission_system,
                    inner_coding_mode, split_bitstream_mode, modulation_format,
                    modformatstring, symbol_rate);
        }

        *ptr_modulation_array = modulation_format;

        MPE_LOG(
                MPE_LOG_TRACE6,
                MPE_MOD_SI,
                "<%s::parseMMSRecords> - Record(%d)   Modulation_Format(%d) = %d.\n",
                SIMODULE, i, i, *ptr_modulation_array);

        ++ptr_modulation_array;

        //****************************************************************************************************
        // Prepare to process the descriptors if they are present.
        //****************************************************************************************************
        *handle_section_data += sizeof(mms_record_struc2);
        *ptr_section_length -= sizeof(mms_record_struc2);

        descriptor_count = **handle_section_data; // get 8-bit descriptor count and advance pointer
        ++(*handle_section_data);
        *ptr_section_length -= 1;

        while (descriptor_count > 0)
        { // ignore all descriptors .

            ptr_descriptor_struc
                    = (generic_descriptor_struc *) *handle_section_data;
            *handle_section_data += sizeof(generic_descriptor_struc)
                    + ptr_descriptor_struc->descriptor_length_octet2;

            //*** decrement section_length as we did not account for descriptor tag, length and data fields
            //*** length and data fields prior to calling subroutine .

            --descriptor_count;
            *ptr_section_length -= (sizeof(generic_descriptor_struc)
                    + ptr_descriptor_struc->descriptor_length_octet2);

            if ((int32_t) * ptr_section_length < 0)
            { //error, section_length not large enough to account for descriptor tags, length and data fields
                nit_error = 35;
                break;
            }
        }

    } //end_of_outer_loop

    //*******************************************************************************************************
    //    Update the SI database with the frequencies.
    //*******************************************************************************************************

    if (nit_error == NO_PARSING_ERROR)
    {
        mpe_siLockForWrite();
        nit_error = mpe_siSetModulationModes(array_of_modulation_format,
                (uint8_t) first_index, (uint8_t) number_of_records);
        mpe_siReleaseWriteLock();

        if (nit_error != NO_PARSING_ERROR)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_SI,
                    "<%s::parseMMSRecords> - Call to mpe_siSetModulationModes failed,ErrorCode = %d\n",
                    SIMODULE, nit_error);
        }
        else
        {
            MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                    "<%s::parseMMSRecords> - MMS records parsed, set flag.\n",
                    SIMODULE);

            //setTablesAcquired(getTablesAcquired() | MMS_ACQUIRED) ;
            // SI DB re-factor changes (7/11/05)
            //mpe_siNotifyTableAcquired(OOB_NIT_MMS, 0);
        }
    }

    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::parseMMSRecords> - Exiting with a Nit_Error_Code = (%d).\n",
            SIMODULE, nit_error);

    return nit_error;
}

/*
 * TODO: Add Comments
 */
mpe_Error get_revision_data(mpe_FilterSectionHandle section_handle,
        uint8_t * version, uint8_t * section_number,
        uint8_t * last_section_number, uint32_t * crc)
{
    mpe_Error retCode = MPE_SUCCESS;
    uint8_t *table_id = NULL;
    uint32_t number_bytes_copied = 0;
    uint8_t input_buffer[1];

    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI, "<%s::get_revision_data> - Enter\n",
            SIMODULE);

    retCode = mpeos_filterSectionRead(section_handle, 0, sizeof(uint8_t), 0,
            input_buffer, &number_bytes_copied);
    /*
     ** get the table_id.
     */
    table_id = (uint8_t *) input_buffer;
    switch (*table_id)
    {
    case NETWORK_INFORMATION_TABLE_ID:
        retCode = parseNIT_revision_data(section_handle, version,
                section_number, last_section_number, crc);
        break;
    case SF_VIRTUAL_CHANNEL_TABLE_ID:
        retCode = parseSVCT_revision_data(section_handle, version,
                section_number, last_section_number, crc);
        break;
    case NETWORK_TEXT_TABLE_ID:
        retCode = parseNTT_revision_data(section_handle, version,
                section_number, last_section_number, crc);
        break;
    case SYSTEM_TIME_TABLE_ID:
        retCode = parseSTT_revision_data(section_handle, version,
                section_number, last_section_number, crc);
        break;
    default:
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::get_revision_data> - matching version parser not found for table_id: 0x%x\n",
                SIMODULE, *table_id);
        return SITP_FAILURE;
    }

    if (true == isVersioningByCRC32())
    {
        MPE_LOG(
                MPE_LOG_TRACE4,
                MPE_MOD_SI,
                "<%s::get_revision_data> - Versioning by CRC only. Resetting version value- crc: 0x%08x\n",
                SIMODULE, *crc);
        *version = NO_VERSION;
        *section_number = NO_VERSION;
        *last_section_number = NO_VERSION;
    }
    else
    {
        MPE_LOG(
                MPE_LOG_TRACE4,
                MPE_MOD_SI,
                "<%s::get_revision_data> - version: %d, section_number: %d, last_section_number: %d, crc = 0x%08x\n",
                SIMODULE, *version, *section_number, *last_section_number, *crc);
    }
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI, "<%s::get_revision_data> - Exit\n",
            SIMODULE);

    return retCode;
}

/*
 ** *****************************************************************************
 ** Subroutine/Method:                       parseNIT_revision_data()
 ** *****************************************************************************
 **
 ** Parse the RDD descriptor in the NIT table section.
 **
 ** @param  section_handle    -  the section handle for the NIT table section.
 ** @param  * version         -  ptr to the version uint8_t to be filled in.
 ** @param  * section_number  -  ptr to the index of this section in the total
 **                                number of sections.
 ** @param  * number_sections -  ptr to the the total number of sections to be
 **                              filled out in this function.
 **
 ** @return SITP_SUCCESS if RDD section was successfully processed/parsed .
 ** Otherwise an error code greater than zero (SITP_FAILURE).
 ** *****************************************************************************
 */
mpe_Error parseNIT_revision_data(mpe_FilterSectionHandle section_handle,
        uint8_t * version, uint8_t * section_number, uint8_t * number_sections,
        uint32_t * crc)
{
    mpe_Error retCode = SITP_SUCCESS;

    nit_table_struc1 * ptr_nit_table_struc1 = NULL;
    revision_detection_descriptor_struc * ptr_rdd_table_struc1 = NULL;
    generic_descriptor_struc * ptr_gen_desc_struc = NULL;

    uint8_t record_size = 0;
    uint8_t descriptor_count = 0;
    uint8_t number_of_records = 0;
    uint8_t table_subtype = 0;
    uint16_t section_length = 0;
    uint32_t number_bytes_copied = 0;
    uint32_t offset = 0;
    size_t ii;

    uint8_t input_buffer[MAX_OOB_SMALL_TABLE_SECTION_LENGTH];

    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::parseNIT_revision_data> - Enter\n", SIMODULE);

    retCode = mpeos_filterSectionRead(section_handle, 0,
            MAX_OOB_SMALL_TABLE_SECTION_LENGTH, 0, input_buffer,
            &number_bytes_copied);
    if (retCode != MPE_SUCCESS)
        return SITP_FAILURE;

    ptr_nit_table_struc1 = (nit_table_struc1 *) (input_buffer + offset);

    section_length = ((GETBITS(ptr_nit_table_struc1->section_length_octet2, 4,
            1)) << 8) | ptr_nit_table_struc1->section_length_octet3;

    number_of_records = ptr_nit_table_struc1->number_of_records_octet6;
    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
            "<%s::parseNIT_revision_data> - Number_Records = (%d).\n",
            SIMODULE, number_of_records);

    //**** extract and validate the table_subtype field ****
    table_subtype = GETBITS(ptr_nit_table_struc1->table_subtype_octet7, 4, 1);
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::parseNIT_revision_data> - Table_Subtype = (%d).\n", SIMODULE,
            table_subtype);

    if ((table_subtype < CARRIER_DEFINITION_SUBTABLE) || (table_subtype
            > MODULATION_MODE_SUBTABLE))
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::parseNIT_revision_data> - Error Table_Subtype not handled: (%d).\n",
                SIMODULE, table_subtype);
        return SITP_FAILURE;
    }

    offset += sizeof(nit_table_struc1);

    /* subtract the header and the CRC */
    section_length -= (sizeof(nit_table_struc1) - LEN_OCTET1_THRU_OCTET3);

    record_size
            = (table_subtype == CARRIER_DEFINITION_SUBTABLE) ? sizeof(cds_record_struc2)
                    : sizeof(mms_record_struc2);

    MPE_LOG(
            MPE_LOG_TRACE6,
            MPE_MOD_SI,
            "<%s::parseNIT_revision_data> - section length after header - section_length: %d\n",
            SIMODULE, section_length);
    while (number_of_records--)
    {
        /* skip the record */
        offset += record_size;
        section_length -= record_size;

        MPE_LOG(
                MPE_LOG_TRACE6,
                MPE_MOD_SI,
                "<%s::parseNIT_revision_data> - record count: %d - section length: %d\n",
                SIMODULE, number_of_records, section_length);

        descriptor_count = *(input_buffer + offset);
        MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                "<%s::parseNIT_revision_data> - descriptor_count: %d\n",
                SIMODULE, descriptor_count);

        offset++;
        section_length--;
        ptr_gen_desc_struc = (generic_descriptor_struc *) (input_buffer
                + offset);

        /* skip the inner descriptors */
        while (descriptor_count--)
        {
            offset += (ptr_gen_desc_struc->descriptor_length_octet2
                    + sizeof(generic_descriptor_struc));
            section_length -= (ptr_gen_desc_struc->descriptor_length_octet2
                    + sizeof(generic_descriptor_struc));
        }
    }

    /*
     * This is the beginning of the outer descriptor.  The RDD should
     * be the first one.  Verify that the remaining section_length is
     * >= the RDD descriptor before we attempt to parse it.
     */
    MPE_LOG(
            MPE_LOG_TRACE6,
            MPE_MOD_SI,
            "<%s::parseNIT_revision_data> - section_length: %d, sizeof(rdd):%d\n",
            SIMODULE, section_length,
            sizeof(revision_detection_descriptor_struc));

    /*
     * Parse and return the RDD values.
     */
    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
            "<%s::parseNIT_revision_data> - number bytes read: %d, offset: %d",
            SIMODULE, number_bytes_copied, offset);

    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
            "<%s::parseNIT_revision_data> - RDD and crc bytes: ", SIMODULE);
    for (ii = 0; ii < (sizeof(revision_detection_descriptor_struc)
            + LENGTH_CRC_32); ii++)
        MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI, "0x%x ", input_buffer[offset + ii]);
    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI, "\n");

    /*
     * Get the RDD or skip the descriptors and then get the CRC
     */
    if (section_length > LENGTH_CRC_32)
    {
        ptr_rdd_table_struc1
                = (revision_detection_descriptor_struc *) (input_buffer
                        + offset);

        if (ptr_rdd_table_struc1->descriptor_tag_octet1 != 0x93)
        {
            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_SI,
                    "<%s::parseNIT_revision_data> - descriptor not a revision descriptor - tag: 0x%x - skip all descriptors\n",
                    SIMODULE, ptr_rdd_table_struc1->descriptor_tag_octet1);

            /*
             * Set the RDD values to the NO_VERSION macro
             */
            *version = NO_VERSION;
            *section_number = NO_VERSION;
            *number_sections = NO_VERSION;

            while (section_length > LENGTH_CRC_32)
            {
                //There must be a descriptor other than the RDD
                ptr_gen_desc_struc = (generic_descriptor_struc *) (input_buffer
                        + offset);
                offset += sizeof(generic_descriptor_struc)
                        + ptr_gen_desc_struc->descriptor_length_octet2;
                section_length -= (sizeof(generic_descriptor_struc)
                        + ptr_gen_desc_struc->descriptor_length_octet2);

                if (section_length <= 0)
                {
                    MPE_LOG(
                            MPE_LOG_ERROR,
                            MPE_MOD_SI,
                            "<%s::parseNIT_revision_data> - error, section_length not large enough to accommodate descriptor(s) and crc.\n",
                            SIMODULE);
                    break;
                }
            }
        }
        else
        {
            *version = (ptr_rdd_table_struc1->table_version_number_octet3
                    & 0x1F);
            *section_number = ptr_rdd_table_struc1->section_number_octet4;
            *number_sections = ptr_rdd_table_struc1->last_section_number_octet5;
            MPE_LOG(
                    MPE_LOG_TRACE4,
                    MPE_MOD_SI,
                    "<%s::parseNIT_revision_data> - version: %d, section_number: %d, last_section_number: %d\n",
                    SIMODULE, *version, *section_number, *number_sections);
            offset += sizeof(revision_detection_descriptor_struc);
            section_length -= sizeof(revision_detection_descriptor_struc);
        }

        if (section_length == LENGTH_CRC_32)
        {
            *crc = ((*(input_buffer + offset)) << 24 | (*(input_buffer + offset
                    + 1)) << 16 | (*(input_buffer + offset + 2)) << 8
                    | (*(input_buffer + offset + 3)));
            MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
                    "<%s::parseNIT_revision_data> - CRC32 = 0x%08x\n",
                    SIMODULE, *crc);

            if ((*crc == 0xFFFFFFFF) || (*crc == 0x00000000))
            {
                MPE_LOG(
                        MPE_LOG_TRACE4,
                        MPE_MOD_SI,
                        "<%s::parseNIT_revision_data> - Invalid CRC value (0x%08x) - calculating CRC\n",
                        SIMODULE, *crc);
                // Note: We don't really need to use a particular CRC here - just a decent one - since we're (presumably) going to be checking
                //       against our calculated CRCs in this case (unless a crazy plant *and* POD send us some with and some without CRCs...)
                *crc = calc_mpeg2_crc(input_buffer, offset + 4); // Need to account for the 4 bytes of CRC in the length
            }
        }
    }
    else if (section_length == LENGTH_CRC_32)
    {
        /*
         * Set the RDD values to the NO_VERSION macro
         */
        *version = NO_VERSION;
        *section_number = NO_VERSION;
        *number_sections = NO_VERSION;

        *crc = ((*(input_buffer + offset)) << 24 | (*(input_buffer + offset
                + 1)) << 16 | (*(input_buffer + offset + 2)) << 8
                | (*(input_buffer + offset + 3)));
        MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
                "<%s::parseNIT_revision_data> - CRC32 = 0x%08x\n",
                SIMODULE, *crc);

        if ((*crc == 0xFFFFFFFF) || (*crc == 0x00000000))
        {
            MPE_LOG(
                    MPE_LOG_TRACE4,
                    MPE_MOD_SI,
                    "<%s::parseNIT_revision_data> - Invalid CRC value (0x%08x) - calculating CRC\n",
                    SIMODULE, *crc);
            // Note: We don't really need to use a particular CRC here - just a decent one - since we're (presumably) going to be checking
            //       against our calculated CRCs in this case (unless a crazy plant *and* POD send us some with and some without CRCs...)
            *crc = calc_mpeg2_crc(input_buffer, offset + 4); // Need to account for the 4 bytes of CRC in the length
        }
    }
    else
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::parseNIT_revision_data> - Error - section not long enough for CRC32\n",
                SIMODULE);
    }
    return retCode;
}

/*
 ** *****************************************************************************
 ** Subroutine/Method:                       parseSVCT_revision_data()
 ** *****************************************************************************
 **
 ** Parse the RDD descriptor in the SVCT table section.
 **
 ** @param  section_handle    -  the section handle for the SVCT table section.
 ** @param  * version         -  ptr to the version uint8_t to be filled in.
 ** @param  * section_number  -  ptr to the index of this section in the total
 **                                number of sections.
 ** @param  * number_sections -  ptr to the the total number of sections to be
 **                              filled out in this function.
 **
 ** @return SITP_SUCCESS if RDD section was successfully processed/parsed .
 ** Otherwise an error code greater than zero (SITP_FAILURE).
 ** *****************************************************************************
 */
mpe_Error parseSVCT_revision_data(mpe_FilterSectionHandle section_handle,
        uint8_t * version, uint8_t * section_number, uint8_t * number_sections,
        uint32_t * crc)
{
    mpe_Error retCode = SITP_SUCCESS;

    svct_table_struc1 * ptr_svct_table_struc1 = NULL;
    vcm_record_struc2 * ptr_vcm_structure = NULL;
    dcm_record_struc2 * ptr_dcm_record_struc = NULL;
    icm_record_struc2 * ptr_icm_record_struc = NULL;
    revision_detection_descriptor_struc * ptr_rdd_table_struc1 = NULL;
    generic_descriptor_struc * ptr_gen_desc_struc = NULL;

    uint8_t descriptor_count = 0;
    uint8_t number_of_VC_records = 0;
    uint8_t DCM_data_length = 0;
    uint8_t icm_record_count = 0;
    uint8_t table_subtype = 0;
    uint8_t descriptors_included = 0;
    uint8_t input_buffer[MAX_OOB_SMALL_TABLE_SECTION_LENGTH];
    uint16_t section_length = 0;
    uint32_t number_bytes_copied = 0;
    uint32_t offset = 0;
    uint32_t ii = 0;

    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::parseSVCT_revision_data> - Enter\n", SIMODULE);
    retCode = mpeos_filterSectionRead(section_handle, 0,
            MAX_OOB_SMALL_TABLE_SECTION_LENGTH, 0, input_buffer,
            &number_bytes_copied);
    if (retCode != MPE_SUCCESS)
        return SITP_FAILURE;

    ptr_svct_table_struc1 = (svct_table_struc1 *) input_buffer;

    section_length = ((GETBITS(ptr_svct_table_struc1->section_length_octet2, 4,
            1)) << 8) | ptr_svct_table_struc1->section_length_octet3;
    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
            "<%s::parseSVCT_revision_data> - section_length = (%d).\n",
            SIMODULE, section_length);

    //**** extract and validate the table_subtype field ****
    table_subtype = GETBITS(ptr_svct_table_struc1->table_subtype_octet5, 4, 1);
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::parseSVCT_revision_data> - Table_Subtype = (%d).\n",
            SIMODULE, table_subtype);

    offset += sizeof(svct_table_struc1);

    /* subtract the header */
    section_length -= (sizeof(svct_table_struc1) - LEN_OCTET1_THRU_OCTET3);

    switch (table_subtype)
    {
    case VIRTUAL_CHANNEL_MAP:
    {
        ptr_vcm_structure = (vcm_record_struc2 *) (input_buffer + offset);

        descriptors_included = ((ptr_vcm_structure->descriptors_included_octet1
                & 0x04) >> 2);
        number_of_VC_records = ptr_vcm_structure->number_vc_records_octet7;

        /*
         * increment section/offset past vcm_record_struc2
         */
        offset += sizeof(vcm_record_struc2);
        section_length -= sizeof(vcm_record_struc2);

        for (ii = 0; ii < number_of_VC_records; ii++)
        {
            /* skip the record */
            offset += (sizeof(vcm_virtual_chan_record3)
                    + sizeof(mpeg2_virtual_chan_struc4));
            section_length -= (sizeof(vcm_virtual_chan_record3)
                    + sizeof(mpeg2_virtual_chan_struc4));

            if (descriptors_included)
            {
                descriptor_count = *(input_buffer + offset);
                offset++;
                section_length--;

                ptr_gen_desc_struc = (generic_descriptor_struc *) (input_buffer
                        + offset);

                /* skip the inner descriptors */
                while (descriptor_count--)
                {
                    offset += (ptr_gen_desc_struc->descriptor_length_octet2
                            + sizeof(generic_descriptor_struc));
                    section_length
                            -= (ptr_gen_desc_struc->descriptor_length_octet2
                                    + sizeof(generic_descriptor_struc));
                }
            }
        }//end for ii < numb_of_VC_records
        break;
    }
    case DEFINED_CHANNEL_MAP:
        ptr_dcm_record_struc = (dcm_record_struc2 *) (input_buffer + offset);
        DCM_data_length = ptr_dcm_record_struc->dcm_data_length_octet_3 & 0x7F;
        offset += ((sizeof(dcm_record_struc3) * DCM_data_length)
                + sizeof(dcm_record_struc2));
        section_length -= ((sizeof(dcm_record_struc3) * DCM_data_length)
                + sizeof(dcm_record_struc2));
        break;
    case INVERSE_CHANNEL_MAP:
        ptr_icm_record_struc = (icm_record_struc2 *) (input_buffer + offset);
        icm_record_count = ptr_icm_record_struc->record_count_octet3 & 0x7F;
        offset += ((sizeof(icm_record_struc3) * icm_record_count)
                + sizeof(icm_record_struc2));
        section_length -= ((sizeof(icm_record_struc3) * icm_record_count)
                + sizeof(icm_record_struc2));
        break;
    default:
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::parseSVCT_revision_data> - Error Table_Subtype not handled: (%d).\n",
                SIMODULE, table_subtype);
        return SITP_FAILURE;
    }

    /*
     * This is the beginning of the outer descriptor.  The RDD should
     * be the first one.  Verify that the remaining section_length is
     * >= the RDD descriptor before we attempt to parse it.
     */
    MPE_LOG(
            MPE_LOG_TRACE6,
            MPE_MOD_SI,
            "<%s::parseSVCT_revision_data> - section_length: %d, sizeof(rdd):%d\n",
            SIMODULE, section_length,
            sizeof(revision_detection_descriptor_struc));

    /*
     * Parse and return the RDD values.
     */
    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
            "<%s::parseSVCT_revision_data> - number bytes read: %d\n",
            SIMODULE, number_bytes_copied);

    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
            "<%s::parseSVCT_revision_data> - RDD bytes: ", SIMODULE);
    for (ii = 0; ii < sizeof(revision_detection_descriptor_struc); ii++)
        MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI, "0x%x ", input_buffer[offset + ii]);
    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI, "\n");

    /*
     * Now skip the other descriptors and get the CRC
     */
    MPE_LOG(
            MPE_LOG_TRACE4,
            MPE_MOD_SI,
            "<%s::parseSVCT_revision_data> - section_length = %d, crc length = %d\n",
            SIMODULE, section_length, LENGTH_CRC_32);
    if (section_length > LENGTH_CRC_32)
    {
        ptr_rdd_table_struc1
                = (revision_detection_descriptor_struc *) (input_buffer
                        + offset);

        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_SI,
                "<%s::parseSVCT_revision_data> - Found descriptor tag: 0x%x (length %d)\n",
                SIMODULE, ptr_rdd_table_struc1->descriptor_tag_octet1,
                ptr_rdd_table_struc1->descriptor_length_octet2);

        if (ptr_rdd_table_struc1->descriptor_tag_octet1 != 0x93)
        {
            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_SI,
                    "<%s::parseSVCT_revision_data> - descriptor not a revision descriptor - tag: 0x%x - skip all descriptors\n",
                    SIMODULE, ptr_rdd_table_struc1->descriptor_tag_octet1);

            /*
             * Set the RDD values to the NO_VERSION macro
             */
            *version = NO_VERSION;
            *section_number = NO_VERSION;
            *number_sections = NO_VERSION;
            while (section_length > LENGTH_CRC_32)
            {
                ptr_gen_desc_struc = (generic_descriptor_struc *) (input_buffer
                        + offset);
                offset += sizeof(generic_descriptor_struc)
                        + ptr_gen_desc_struc->descriptor_length_octet2;
                section_length -= (sizeof(generic_descriptor_struc)
                        + ptr_gen_desc_struc->descriptor_length_octet2);

                MPE_LOG(
                        MPE_LOG_INFO,
                        MPE_MOD_SI,
                        "<%s::parseSVCT_revision_data> - Found descriptor tag: 0x%x (length %d)\n",
                        SIMODULE, ptr_gen_desc_struc->descriptor_tag_octet1,
                        ptr_gen_desc_struc->descriptor_length_octet2);

                if (section_length <= 0)
                {
                    MPE_LOG(
                            MPE_LOG_ERROR,
                            MPE_MOD_SI,
                            "<%s::parseSVCT_revision_data> - error, section_length not large enough to accommodate descriptor(s) and crc.\n",
                            SIMODULE);
                    break;
                }
            }
        }
        else
        {
            *version = (ptr_rdd_table_struc1->table_version_number_octet3
                    & 0x1F);
            *section_number = ptr_rdd_table_struc1->section_number_octet4;
            *number_sections = ptr_rdd_table_struc1->last_section_number_octet5;
            MPE_LOG(
                    MPE_LOG_TRACE4,
                    MPE_MOD_SI,
                    "<%s::parseSVCT_revision_data> - version: %d, section_number: %d, last_section_number: %d\n",
                    SIMODULE, *version, *section_number, *number_sections);
            offset += sizeof(revision_detection_descriptor_struc);
            section_length -= sizeof(revision_detection_descriptor_struc);
        }

        MPE_LOG(
                MPE_LOG_TRACE4,
                MPE_MOD_SI,
                "<%s::parseSVCT_revision_data> - before CRC section_length = %d\n",
                SIMODULE, section_length);

        if (section_length == LENGTH_CRC_32)
        {
            *crc = ((*(input_buffer + offset)) << 24 | (*(input_buffer + offset
                    + 1)) << 16 | (*(input_buffer + offset + 2)) << 8
                    | (*(input_buffer + offset + 3)));
            MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
                    "<%s::parseSVCT_revision_data> - CRC32 = 0x%08x\n",
                    SIMODULE, *crc);

            if ((*crc == 0xFFFFFFFF) || (*crc == 0x00000000))
            {
                MPE_LOG(
                        MPE_LOG_TRACE4,
                        MPE_MOD_SI,
                        "<%s::parseSVCT_revision_data> - Invalid CRC value (0x%08x) - calculating CRC\n",
                        SIMODULE, *crc);
                // Note: We don't really need to use a particular CRC here - just a decent one - since we're (presumably) going to be checking
                //       against our calculated CRCs in this case (unless a crazy plant *and* POD send us some with and some without CRCs...)
                *crc = calc_mpeg2_crc(input_buffer, offset + 4); // Need to account for the 4 bytes of CRC in the length
            }
        }
    }
    else if (section_length == LENGTH_CRC_32)
    {
        /*
         * Set the RDD values to the NO_VERSION macro
         */
        *version = NO_VERSION;
        *section_number = NO_VERSION;
        *number_sections = NO_VERSION;

        *crc = ((*(input_buffer + offset)) << 24 | (*(input_buffer + offset
                + 1)) << 16 | (*(input_buffer + offset + 2)) << 8
                | (*(input_buffer + offset + 3)));
        MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
                "<%s::parseSVCT_revision_data> - CRC32 = 0x%08x\n",
                SIMODULE, *crc);

        if ((*crc == 0xFFFFFFFF) || (*crc == 0x00000000))
        {
            MPE_LOG(
                    MPE_LOG_TRACE4,
                    MPE_MOD_SI,
                    "<%s::parseSVCT_revision_data> - Invalid CRC value (0x%08x) - calculating CRC\n",
                    SIMODULE, *crc);
            // Note: We don't really need to use a particular CRC here - just a decent one - since we're (presumably) going to be checking
            //       against our calculated CRCs in this case (unless a crazy plant *and* POD send us some with and some without CRCs...)
            *crc = calc_mpeg2_crc(input_buffer, offset + 4); // Need to account for the 4 bytes of CRC in the length
        }
    }
    else
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::parseSVCT_revision_data> - No enough bytes left for a CRC32\n",
                SIMODULE);
    }
    return retCode;
}

/*
 ** *****************************************************************************
 ** Subroutine/Method:                       parseNTT_revision_data()
 ** *****************************************************************************
 **
 ** Parse the RDD descriptor in the NTT table section.
 **
 ** @param  section_handle    -  the section handle for the NTT table section.
 ** @param  * version         -  ptr to the version uint8_t to be filled in.
 ** @param  * section_number  -  ptr to the index of this section in the total
 **                                number of sections.
 ** @param  * number_sections -  ptr to the the total number of sections to be
 **                              filled out in this function.
 **
 ** @return SITP_SUCCESS if RDD section was successfully processed/parsed .
 ** Otherwise an error code greater than zero (SITP_FAILURE).
 ** *****************************************************************************
 */
mpe_Error parseNTT_revision_data(mpe_FilterSectionHandle section_handle,
        uint8_t * version, uint8_t * section_number, uint8_t * number_sections,
        uint32_t * crc)
{
    mpe_Error retCode = SITP_SUCCESS;

    revision_detection_descriptor_struc * ptr_rdd_table_struc1 = NULL;
    ntt_record_struc1 * ptr_ntt_record_struc1 = NULL;
    sns_record_struc1 * ptr_sns_record_struc1 = NULL;
    sns_record_struc2 * ptr_sns_record_struc2 = NULL;
    generic_descriptor_struc * ptr_generic_descriptor_struc = NULL;

    uint8_t input_buffer[MAX_OOB_SMALL_TABLE_SECTION_LENGTH];
    uint8_t *ptr_input_buffer = NULL;
    uint8_t number_of_SNS_records = 0;
    uint8_t name_length = 0;
    uint8_t table_subtype = 0;
    uint8_t SNS_descriptor_count = 0;
    uint8_t descriptor_length = 0;
    uint16_t section_length = 0;
    uint32_t number_bytes_copied = 0;
    uint32_t offset = 0;

    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::parseNTT_revision_data> - Enter\n", SIMODULE);
    retCode = mpeos_filterSectionRead(section_handle, 0,
            MAX_OOB_SMALL_TABLE_SECTION_LENGTH, 0, input_buffer,
            &number_bytes_copied);
    if (retCode != MPE_SUCCESS)
        return SITP_FAILURE;

    ptr_ntt_record_struc1 = (ntt_record_struc1 *) input_buffer;

    section_length = ((GETBITS(ptr_ntt_record_struc1->section_length_octet2, 4,
            1)) << 8) | ptr_ntt_record_struc1->section_length_octet3;
    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
            "<%s::parseNTT_revision_data> - section_length = (%d).\n",
            SIMODULE, section_length);

    //**** extract and validate the table_subtype field ****
    table_subtype = GETBITS(ptr_ntt_record_struc1->table_subtype_octet8, 4, 1);
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::parseNTT_revision_data> - Table_Subtype = (%d).\n", SIMODULE,
            table_subtype);

    offset += sizeof(ntt_record_struc1);

    /* subtract the header */
    section_length -= (sizeof(ntt_record_struc1) - LEN_OCTET1_THRU_OCTET3);

    /* fail if this doesn't have an SNS subtable */
    if (table_subtype != SNS_TABLE_SUBTYPE)
        return SITP_FAILURE;

    ptr_input_buffer = input_buffer + offset;
    ptr_sns_record_struc1 = (sns_record_struc1 *) ptr_input_buffer;
    number_of_SNS_records = ptr_sns_record_struc1->number_of_SNS_records_octet1;

    /* Calculate new offset and section length */
    ptr_input_buffer += sizeof(sns_record_struc1);
    section_length -= sizeof(sns_record_struc1);

    MPE_LOG(
            MPE_LOG_TRACE4,
            MPE_MOD_SI,
            "<%s::parseNTT_revision_data> - number of sns records = (%d), offset = %d.\n",
            SIMODULE, number_of_SNS_records, offset);

    while (number_of_SNS_records--)
    {
        ptr_sns_record_struc2 = (sns_record_struc2 *) ptr_input_buffer;
        name_length = ptr_sns_record_struc2->name_length_octet4;
        MPE_LOG(
                MPE_LOG_TRACE6,
                MPE_MOD_SI,
                "<%s::parseNTT_revision_data> - name_length of sns record %d = (%d).\n",
                SIMODULE, number_of_SNS_records, name_length);
        ptr_input_buffer += (sizeof(sns_record_struc2) + name_length);
        section_length -= (sizeof(sns_record_struc2) + name_length);

        SNS_descriptor_count = *ptr_input_buffer;
        MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                "<%s::parseNTT_revision_data> - SNS_descriptor_count %d.\n",
                SIMODULE, SNS_descriptor_count);
        ptr_input_buffer++;
        section_length--;

        while (SNS_descriptor_count--)
        {
            ptr_generic_descriptor_struc
                    = (generic_descriptor_struc *) ptr_input_buffer;
            descriptor_length
                    = ptr_generic_descriptor_struc->descriptor_length_octet2;
            MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                    "<%s::parseNTT_revision_data> - descriptor length %d.\n",
                    SIMODULE, SNS_descriptor_count);

            ptr_input_buffer += (descriptor_length
                    + sizeof(generic_descriptor_struc));
            section_length -= (descriptor_length
                    + sizeof(generic_descriptor_struc));
        }
    }

    /*
     * This is the beginning of the outer descriptor.  The RDD should
     * be the first one.  Verify that the remaining section_length is
     * >= the RDD descriptor before we attempt to parse it.
     */
    MPE_LOG(
            MPE_LOG_TRACE6,
            MPE_MOD_SI,
            "<%s::parseNTT_revision_data> - section_length: %d, sizeof(rdd):%d\n",
            SIMODULE, section_length,
            sizeof(revision_detection_descriptor_struc));

    /*
     * Now skip the other descriptors and get the CRC
     */
    MPE_LOG(
            MPE_LOG_TRACE4,
            MPE_MOD_SI,
            "<%s::parseNTT_revision_data> - section_length = %d, crc length = %d\n",
            SIMODULE, section_length, LENGTH_CRC_32);
    if (section_length > LENGTH_CRC_32)
    {
        ptr_rdd_table_struc1
                = (revision_detection_descriptor_struc *) ptr_input_buffer;

        if (ptr_rdd_table_struc1->descriptor_tag_octet1 != 0x93)
        {
            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_SI,
                    "<%s::parseNTT_revision_data> - descriptor not a revision descriptor - tag: 0x%x - skip all descriptors\n",
                    SIMODULE, ptr_rdd_table_struc1->descriptor_tag_octet1);

            /*
             * Set the RDD values to the NO_VERSION macro
             */
            *version = NO_VERSION;
            *section_number = NO_VERSION;
            *number_sections = NO_VERSION;
            while (section_length > LENGTH_CRC_32)
            {
                ptr_generic_descriptor_struc
                        = (generic_descriptor_struc *) ptr_input_buffer;
                ptr_input_buffer
                        += sizeof(generic_descriptor_struc)
                                + ptr_generic_descriptor_struc->descriptor_length_octet2;
                section_length
                        -= (sizeof(generic_descriptor_struc)
                                + ptr_generic_descriptor_struc->descriptor_length_octet2);

                if (section_length <= 0)
                {
                    MPE_LOG(
                            MPE_LOG_ERROR,
                            MPE_MOD_SI,
                            "<%s::parseNTT_revision_data> - error, section_length not large enough to accommodate descriptor(s) and crc.\n",
                            SIMODULE);
                    break;
                }
            }
        }
        else
        {
            *version = (ptr_rdd_table_struc1->table_version_number_octet3
                    & 0x1F);
            *section_number = ptr_rdd_table_struc1->section_number_octet4;
            *number_sections = ptr_rdd_table_struc1->last_section_number_octet5;
            MPE_LOG(
                    MPE_LOG_TRACE4,
                    MPE_MOD_SI,
                    "<%s::parseNTT_revision_data> - version: %d, section_number: %d, last_section_number: %d\n",
                    SIMODULE, *version, *section_number, *number_sections);
            ptr_input_buffer += sizeof(revision_detection_descriptor_struc);
            section_length -= sizeof(revision_detection_descriptor_struc);
        }

        MPE_LOG(
                MPE_LOG_TRACE4,
                MPE_MOD_SI,
                "<%s::parseNTT_revision_data> - before CRC section_length = %d\n",
                SIMODULE, section_length);

        if (section_length == LENGTH_CRC_32)
        {

            *crc = ((*ptr_input_buffer) << 24 | (*(ptr_input_buffer + 1)) << 16
                    | (*(ptr_input_buffer + 2)) << 8
                    | (*(ptr_input_buffer + 3)));
            MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
                    "<%s::parseNTT_revision_data> - CRC32 = 0x%08x\n",
                    SIMODULE, *crc);

            if ((*crc == 0xFFFFFFFF) || (*crc == 0x00000000))
            {
                MPE_LOG(
                        MPE_LOG_TRACE4,
                        MPE_MOD_SI,
                        "<%s::parseNTT_revision_data> - Invalid CRC value (0x%08x) - calculating CRC\n",
                        SIMODULE, *crc);
                // Note: We don't really need to use a particular CRC here - just a decent one - since we're (presumably) going to be checking
                //       against our calculated CRCs in this case (unless a crazy plant *and* POD send us some with and some without CRCs...)
                *crc = calc_mpeg2_crc(input_buffer, offset + 4); // Need to account for the 4 bytes of CRC in the length
            }
        }
    }
    else if (section_length == LENGTH_CRC_32)
    {
        /*
         * Set the RDD values to the NO_VERSION macro
         */
        *version = NO_VERSION;
        *section_number = NO_VERSION;
        *number_sections = NO_VERSION;

        *crc = ((*ptr_input_buffer) << 24 | (*(ptr_input_buffer + 1)) << 16
                | (*(ptr_input_buffer + 2)) << 8
                | (*(ptr_input_buffer + 3)));
        MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
                "<%s::parseNTT_revision_data> - CRC32 = 0x%08x\n",
                SIMODULE, *crc);

        if ((*crc == 0xFFFFFFFF) || (*crc == 0x00000000))
        {
            MPE_LOG(
                    MPE_LOG_TRACE4,
                    MPE_MOD_SI,
                    "<%s::parseNTT_revision_data> - Invalid CRC value (0x%08x) - calculating CRC\n",
                    SIMODULE, *crc);
            // Note: We don't really need to use a particular CRC here - just a decent one - since we're (presumably) going to be checking
            //       against our calculated CRCs in this case (unless a crazy plant *and* POD send us some with and some without CRCs...)
            *crc = calc_mpeg2_crc(input_buffer, offset + 4); // Need to account for the 4 bytes of CRC in the length
        }
    }
    else
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_SI,
                "<%s::parseNTT_revision_data> - No enough bytes left for a CRC32\n",
                SIMODULE);
    }
    return retCode;
}

/*
 ** *****************************************************************************
 ** Subroutine/Method:                       parseSTT_revision_data()
 ** *****************************************************************************
 **
 ** Parse the RDD descriptor in the STT table section.
 **
 ** @param  section_handle    -  the section handle for the STT table section.
 ** @param  * version         -  ptr to the version uint8_t to be filled in.
 ** @param  * section_number  -  ptr to the index of this section in the total
 **                                number of sections.
 ** @param  * number_sections -  ptr to the the total number of sections to be
 **                              filled out in this function.
 **
 ** @return SITP_SUCCESS if RDD section was successfully processed/parsed .
 ** Otherwise an error code greater than zero (SITP_FAILURE).
 ** *****************************************************************************
 */
mpe_Error parseSTT_revision_data(mpe_FilterSectionHandle section_handle,
        uint8_t * version, uint8_t * section_number, uint8_t * number_sections,
        uint32_t * crc)
{
    mpe_Error retCode = SITP_SUCCESS;
    stt_table_struc1 * ptr_stt_table_struc1 = NULL;
    uint8_t input_buffer[MAX_OOB_SMALL_TABLE_SECTION_LENGTH];
    uint16_t section_length = 0;
    uint32_t number_bytes_copied = 0;

    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::parseSTT_revision_data> - Enter\n", SIMODULE);
    retCode = mpeos_filterSectionRead(section_handle, 0,
            MAX_OOB_SMALL_TABLE_SECTION_LENGTH, 0, input_buffer,
            &number_bytes_copied);
    if (retCode != MPE_SUCCESS)
        return SITP_FAILURE;

    ptr_stt_table_struc1 = (stt_table_struc1 *) input_buffer;

    section_length = ((GETBITS(ptr_stt_table_struc1->section_length_octet2, 4,
            1)) << 8) | ptr_stt_table_struc1->section_length_octet3;
    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
            "<%s::parseSTT_revision_data> - section_length = (%d).\n",
            SIMODULE, section_length);

    *version = NO_VERSION;
    *section_number = NO_VERSION;
    *number_sections = NO_VERSION;

    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI,
            "<%s::parseSTT_revision_data> - Exit.\n", SIMODULE);
    return retCode;
}

/*
 ** *****************************************************************************
 ** Subroutine/Method:                       parseAndUpdateSTT()
 ** *****************************************************************************
 **
 ** Parse STT table section and update SI database.
 **
 ** @param  ptr_TableSection,  a pointer to the start of the NTT table section.
 ** @param  sectiontable_size, The number of bytes in the table section per
 **
 **
 ** @return MPE_SUCCESS if table section was successfully processed/parsed .
 ** Otherwise an error code greater than zero (MPE_SUCCESS).
 ** *****************************************************************************
 */
mpe_Error parseAndUpdateSTT(mpe_FilterSectionHandle section_handle,
        uint8_t *version, uint8_t *section_number,
        uint8_t *last_section_number, uint32_t *crc)
{
    mpe_Error retCode = SITP_SUCCESS;
    stt_table_struc1 * ptr_stt_table_struc1 = NULL;
    uint8_t input_buffer[MAX_OOB_SMALL_TABLE_SECTION_LENGTH];
    uint8_t *ptr_input_buffer = NULL;
    uint16_t section_length = 0;
    uint32_t number_bytes_copied = 0;
    uint32_t gps_utc_offset = 0;
    uint32_t system_time = 0;
    uint32_t offset = 0;
    uint32_t protocol_version = 0;
    uint32_t crc2 = 0;
    uint32_t GPSTimeInUnixEpoch = 0;

    /*
     uint16_t DS_status                                           = FALSE;
     uint8_t DS_hour                                              = 0;
     generic_descriptor_struc *ptr_generic_descriptor_struc       = NULL;
     daylight_savings_descriptor_struc *ds_descriptor_struc       = NULL;
     uint16_t ds_desc_length                                      = 0;
     */
    mpe_Error stt_error = NO_PARSING_ERROR;

    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI, "<%s::parseAndUpdateSTT> - Enter\n",
            SIMODULE);

    retCode = mpeos_filterSectionRead(section_handle, 0,
            MAX_OOB_SMALL_TABLE_SECTION_LENGTH, 0, input_buffer,
            (uint32_t *) &number_bytes_copied);

    if (MPE_SUCCESS != retCode)
    {
        stt_error = 1;
        goto PARSING_DONE;
    }
    else
    {
        ptr_stt_table_struc1 = (stt_table_struc1 *) input_buffer;

        //**** validate table Id field ****
        if (ptr_stt_table_struc1->table_id_octet1 != SYSTEM_TIME_TABLE_ID)
        {
            stt_error = 2;
            goto PARSING_DONE;
        }

        if (TESTBITS(ptr_stt_table_struc1->section_length_octet2, 8, 7))
        {
            MPE_LOG(
                    MPE_LOG_TRACE6,
                    MPE_MOD_SI,
                    "<%s::parseAndUpdateSTT> - *** WARNING *** 2-Bit-Zero Field #1 not set to all zeroes(0)\n.",
                    SIMODULE);
        }

        //**** validate 2 reserved bits ****
        if (GETBITS(ptr_stt_table_struc1->section_length_octet2, 6, 5) != 3)
        {
            MPE_LOG(
                    MPE_LOG_TRACE6,
                    MPE_MOD_SI,
                    "<%s::parseAndUpdateSTT> - *** WARNING *** 2-Bit-Reserved Field #2 not set to all ones(1)\n.",
                    SIMODULE);
        }
        section_length = ((GETBITS(ptr_stt_table_struc1->section_length_octet2,
                4, 1)) << 8) | ptr_stt_table_struc1->section_length_octet3;

        MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                "<%s::parseAndUpdateSTT> - section_length = (%d).\n", SIMODULE,
                section_length);

        protocol_version = ptr_stt_table_struc1->protocol_version_octet4;
        if (protocol_version != 0)
        {
            MPE_LOG(
                    MPE_LOG_TRACE6,
                    MPE_MOD_SI,
                    "<%s::parseAndUpdateSTT> - *** WARNING *** 8-Bit-Zero Field #4 not set to all zeroes(0)\n.",
                    SIMODULE);
        }
        MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
                "<%s::parseAndUpdateSTT> - protocol_version = %d.\n", SIMODULE,
                protocol_version);

        if (ptr_stt_table_struc1->zeroes_octet5 != 0)
        {
            MPE_LOG(
                    MPE_LOG_TRACE6,
                    MPE_MOD_SI,
                    "<%s::parseAndUpdateSTT> - *** WARNING *** 8-Bit-Zero Field #5 not set to all zeroes(0)\n.",
                    SIMODULE);
        }

        // Extract system time
        system_time = (ptr_stt_table_struc1->system_time_octet6 << 24)
                | (ptr_stt_table_struc1->system_time_octet7 << 16)
                | (ptr_stt_table_struc1->system_time_octet8 << 8)
                | (ptr_stt_table_struc1->system_time_octet9);

        // Extract GPS_UTC_Offset
        gps_utc_offset = ptr_stt_table_struc1->GPS_UTC_offset_octet10;

        MPE_LOG(
                MPE_LOG_TRACE4,
                MPE_MOD_SI,
                "<%s::parseAndUpdateSTT> - system_time = %d gps_utc_offset = %d.\n",
                SIMODULE, system_time, gps_utc_offset);

        /*
         * STT time -  Seconds since 0UTC 1980-01-06 ("GPS time")
         * Convert to UNIX time (seconds since 0UTC 1970-01-01)
         *              t(unix) = t + 315964800
         */
        GPSTimeInUnixEpoch = (system_time - gps_utc_offset) + 315964800;

        // Set the stt time
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,
                "<%s::parseAndUpdateSTT, setSTTTime(%u, %lld)\n", SIMODULE,
                GPSTimeInUnixEpoch, getSTTStartTime());

        setSTTTime(GPSTimeInUnixEpoch, getSTTStartTime());

        // Parse the remaining descriptors
        offset += sizeof(stt_table_struc1);

        ptr_input_buffer = input_buffer + offset;
        section_length -= (sizeof(stt_table_struc1) - LEN_OCTET1_THRU_OCTET3);

        if (section_length < LENGTH_CRC_32)
        {
            stt_error = 3;
            goto PARSING_DONE;
        }

        if (section_length == LENGTH_CRC_32)
        {
            crc2 = ((*ptr_input_buffer) << 24 | (*(ptr_input_buffer + 1)) << 16
                    | (*(ptr_input_buffer + 2)) << 8
                    | (*(ptr_input_buffer + 3)));
            MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
                    "<%s::parseAndUpdateSTT> - CRC32 = 0x%08x\n", SIMODULE,
                    crc2);
            goto PARSING_DONE;
        }

        // Un-comment the following block if descriptor parsing is enabled
        /*
         while (section_length > LENGTH_CRC_32)
         {
         ptr_generic_descriptor_struc = (generic_descriptor_struc *) ptr_input_buffer ;

         if ( ptr_generic_descriptor_struc->descriptor_tag_octet1 == 0x96 )
         {
         // Extract the daylight savings info
         ds_descriptor_struc = (daylight_savings_descriptor_struc *) ptr_input_buffer ;
         ds_desc_length = ds_descriptor_struc->descriptor_length_octet2;

         if (TESTBITS(ds_descriptor_struc->DS_status,8,8))
         {
         DS_status = YES ;
         }
         else
         {
         DS_status = NO ;
         }
         DS_hour = ds_descriptor_struc->DS_hour;

         MPE_LOG (MPE_LOG_TRACE4, MPE_MOD_SI,"<%s::parseAndUpdateSTT> - DS_status = %d  DS_hour = %d.\n",
         SIMODULE, DS_status, DS_hour);

         ptr_input_buffer     +=  sizeof(ds_descriptor_struc) + ds_desc_length ;
         section_length       -= (sizeof(ds_descriptor_struc) + ds_desc_length) ;
         }
         else
         {
         ptr_input_buffer     +=  sizeof(generic_descriptor_struc) + ptr_generic_descriptor_struc->descriptor_length_octet2 ;
         section_length       -= (sizeof(generic_descriptor_struc) + ptr_generic_descriptor_struc->descriptor_length_octet2) ;
         }


         if (section_length <= 0)
         {
         MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI,"<%s::parseSTT_revision_data> - error, section_length not large enough to accommodate descriptor(s) and crc.\n",
         SIMODULE);
         break ;
         }

         }

         if ( section_length == LENGTH_CRC_32 )
         {
         crc2 = ((*ptr_input_buffer)     << 24
         | (*(ptr_input_buffer + 1))<< 16
         | (*(ptr_input_buffer + 2))<< 8
         | (*(ptr_input_buffer + 3)));

         MPE_LOG (MPE_LOG_TRACE4, MPE_MOD_SI,"<%s::parseNTT_revision_data> - CRC32 = 0x%08x\n", SIMODULE, crc2);
         }
         */
    }

    PARSING_DONE:

    MPE_LOG(
            MPE_LOG_TRACE4,
            MPE_MOD_SI,
            "<%s::parseAndUpdateSTT> - Exiting with a STT_Error_Code = (%d).\n",
            SIMODULE, stt_error);

    return stt_error;
}

/*
 ** *****************************************************************************
 ** Subroutine/Method:                       parseAndUpdateSVCT()
 ** *****************************************************************************
 **
 ** Parse SVCT table section and update SI database.
 **
 ** @param  ptr_TableSection,  a pointer to the start of the NIT table section.
 ** @param  sectiontable_size, The number of bytes in the table section per
 **         the PowerTV OS.
 **
 ** @return MPE_SUCCESS if table section was successfully processed/parsed .
 ** Otherwise an error code greater than zero (MPE_SUCCESS).
 ** *****************************************************************************
 */
mpe_Error parseAndUpdateSVCT(mpe_FilterSectionHandle section_handle,
        uint8_t *version, uint8_t *section_number,
        uint8_t *last_section_number, uint32_t *crc)
{
    mpe_Error retCode = SITP_SUCCESS;
    svct_table_struc1 *ptr_table_struc1 = NULL;
    revision_detection_descriptor_struc * ptr_rdd_table_struc1 = NULL;
    int32_t section_length = 0;
    uint16_t table_subtype = 0, vct_id = 0;

    uint32_t number_bytes_copied = 0;
    generic_descriptor_struc *ptr_descriptor_struc;
    mpe_Error svct_error = NO_PARSING_ERROR;
    uint8_t input_buffer[MAX_OOB_SMALL_TABLE_SECTION_LENGTH];
    uint8_t *ptr_input_buffer = NULL;

    //***********************************************************************************************
    //***********************************************************************************************

    //**********************************************************************************************
    // Parse and validate the first 56 bytes (Table_ID thru Last_Section_Number)of the scvt
    // table (see document ANSI/SCTE 65-2002 (formerly DVS 234), chap 5.1.1,pg.15) .
    //**********************************************************************************************
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::parseAndUpdateSVCT> - Entered method parseAndUpdateSVCT.\n",
            SIMODULE);
    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
            "<%s::parseAndUpdateSVCT> - section_handle = (%u).\n", SIMODULE,
            section_handle);
    //    Dump_Utility("parseAndUpdateSVCT", sectiontable_size, ptr_TableSection) ;

    retCode = mpeos_filterSectionRead(section_handle, 0,
            sizeof(svct_table_struc1), 0, input_buffer,
            (uint32_t *) &number_bytes_copied);

    if ((MPE_SUCCESS != retCode) || (number_bytes_copied
            != sizeof(svct_table_struc1)))
    {
        svct_error = 1;
        goto PARSING_DONE;
    }
    else
    {
        ptr_input_buffer = input_buffer;
        ptr_table_struc1 = (svct_table_struc1 *) ptr_input_buffer;

        //**** validate table Id field ****
        if (ptr_table_struc1->table_id_octet1 != SF_VIRTUAL_CHANNEL_TABLE_ID)
        {
            svct_error = 2;
        }
        //**** validate "Zero" (2-bits) field ****
        if (TESTBITS(ptr_table_struc1->section_length_octet2, 8, 7))
        {
            // bits not set to 0.
            //svct_error = 3 ;
            MPE_LOG(
                    MPE_LOG_TRACE6,
                    MPE_MOD_SI,
                    "<%s::parseAndUpdateSVCT> - *** WARNING *** 2-Bit-Zero Field #1 not set to all zeroes(0)\n.",
                    SIMODULE);
        }
        //**** validate 2 reserved bits ****
        if (GETBITS(ptr_table_struc1->section_length_octet2, 6, 5) != 3)
        {
            // bits 5 and 6 are not set to 1 per ANSI/SCTE 65-2002,pg.12, para 4.3
            //svct_error = 4 ;
            MPE_LOG(
                    MPE_LOG_TRACE6,
                    MPE_MOD_SI,
                    "<%s::parseAndUpdateSVCT> - *** WARNING *** 2-Bit-Reserved Field #2 not set to all ones(1)\n.",
                    SIMODULE);
        }
        //**** extract and validate section_length field (octets 2 & 3)         ****
        //**** note, the maximum size of a scvt excluding octet 1 thru 3 is 1021.****
        //**** and occupies only bits 1 thru of the 12 bits                     ****

        section_length = ((GETBITS(ptr_table_struc1->section_length_octet2, 4,
                1)) << 8) | ptr_table_struc1->section_length_octet3;
        MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                "<%s::parseAndUpdateSVCT> - section_length2 = (%d).\n",
                SIMODULE, section_length);

        if (section_length > 1021)
        { // warning, section_length too large.
            // NOTE: SVCTs larger than 1024 have been seen on test networks.
            // Since length is 12 bits - and we have no dependancy on it being smaller - just warn
            MPE_LOG(
                    MPE_LOG_TRACE6,
                    MPE_MOD_SI,
                    "<%s::parseAndUpdateSVCT> - *** WARNING *** Section length larger than 1021\n.",
                    SIMODULE);
        }

        //**** verify that the "Zero" (3-bits) and the Protocol Version fields ****
        //**** (5-bits) are set to zero. -                                     ****
        if (ptr_table_struc1->protocol_version_octet4 != 0)
        {
            // one or more of the eight bits are set to 1.
            //svct_error = 6 ;
            MPE_LOG(
                    MPE_LOG_TRACE6,
                    MPE_MOD_SI,
                    "<%s::parseAndUpdateSVCT> - *** WARNING *** 8-Bit-Zero Field #3 not set to all zeroes(0)\n.",
                    SIMODULE);
        }

        //**** extract and validate the transmission_medium ****
        if (TESTBITS(ptr_table_struc1->table_subtype_octet5, 8, 5) != 0)
        {
            // bits 8 thru 5 shall be set to zero (0) per ANSI/SCTE 65-2002,pg.14, para 5.1
            svct_error = 9;
        }

        //**** extract and validate the table_subtype field ****
        table_subtype = (uint16_t)(GETBITS(
                ptr_table_struc1->table_subtype_octet5, 4, 1));
        MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                "<%s::parseAndUpdateSVCT> - table_subtype = (%d).\n", SIMODULE,
                table_subtype);

        if (table_subtype > INVERSE_CHANNEL_MAP)
        {
            // error, invalid table_subtype value.
            svct_error = 10;
        }

        //**** extract the 16-bit vct_id ****
        vct_id = (uint16_t)((ptr_table_struc1->vct_id_octet6 << 8)
                | ptr_table_struc1->vct_id_octet7);

        MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                "<%s::parseAndUpdateSVCT> - vct_id = (%d).\n", SIMODULE, vct_id);

        //**** section_length identifies the total number of bytes/octets remaining ****
        //**** in the scvt after octet3. Before processing the VCM/DCM/ICM record,  ****
        //**** adjust section_length to account for having processed octets4 thru   ****
        //**** octet7 and account for the future processing of the CRC_32 field/    ****
        //**** octets. Adjust ptr_TableSection to account for the header processed  ****
        //**** data.                                                                ****

        section_length -= (sizeof(svct_table_struc1) - LEN_OCTET1_THRU_OCTET3);
        ptr_input_buffer += sizeof(svct_table_struc1);

        MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                "<%s::parseAndUpdateSVCT> - section_length3 = (%d).\n",
                SIMODULE, section_length);
        MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                "<%s::parseAndUpdateSVCT> - ptr_input_buffer = (%p).\n",
                SIMODULE, ptr_input_buffer);

        if ((svct_error == NO_PARSING_ERROR)
                && (section_length < LENGTH_CRC_32))
        { // error, either a portion of header is missing, or all or a portion of the
            // CRC_32 value is missing.
            svct_error = 11;
        }
    }

    //**********************************************************************************************
    // Parse the remaining bytes/octets in the scvt.
    //**********************************************************************************************

    if (svct_error == NO_PARSING_ERROR)
    {
        retCode = mpeos_filterSectionRead(section_handle, 7, section_length, 0,
                input_buffer, (uint32_t *) &number_bytes_copied);

        if ((MPE_SUCCESS != retCode) || (number_bytes_copied != section_length))
        {
            svct_error = 12;
            goto PARSING_DONE;
        }
        //****************************************************************************************
        //**** Process the VCM/DCM/ICM record .
        //****************************************************************************************

        ptr_input_buffer = input_buffer;
        MPE_LOG(
                MPE_LOG_TRACE4,
                MPE_MOD_SI,
                "<%s::parseAndUpdateSVCT> - before vcm/dcm parse call - ptr_input_buffer = (0x%p bytes copied: %d\n",
                SIMODULE, ptr_input_buffer, number_bytes_copied);
        if (table_subtype == VIRTUAL_CHANNEL_MAP)
        {
            svct_error = parseVCMStructure(&ptr_input_buffer, &section_length);
            // if svct_error == NO_PARSING_ERROR, ptr_input_buffer has been properly updated
        }
        else if (table_subtype == DEFINED_CHANNEL_MAP)
        {
            // table_subtype is DEFINED_CHANNEL_MAP
            svct_error = parseDCMStructure(&ptr_input_buffer, &section_length);
        }
        else
        {
            // table_subtype is INVERSE_CHANNEL_MAP,
            // ignore this type for now
            svct_error = 13;
        }

    }

    MPE_LOG(
            MPE_LOG_TRACE4,
            MPE_MOD_SI,
            "<%s::parseAndUpdateSVCT> - sectionlength after exit from parseVCMStructure records: %d, ptr: 0x%p.\n",
            SIMODULE, section_length, ptr_input_buffer);

    //*************************************************************************************************
    //* At this point, there may be a series a descriptors present.However, we are not required to
    //* process them for the 06-07-04 PowerTV release. If there are descriptors present, we will
    //* "ignore" them.
    //*************************************************************************************************
    if ((svct_error == NO_PARSING_ERROR) && (section_length > LENGTH_CRC_32))
    {
        ptr_rdd_table_struc1
                = (revision_detection_descriptor_struc *) ptr_input_buffer;

        if (ptr_rdd_table_struc1->descriptor_tag_octet1 == 0x93)
        {
            *version = (ptr_rdd_table_struc1->table_version_number_octet3
                    & 0x1F);
            *section_number = ptr_rdd_table_struc1->section_number_octet4;
            *last_section_number
                    = ptr_rdd_table_struc1->last_section_number_octet5;
            MPE_LOG(
                    MPE_LOG_TRACE4,
                    MPE_MOD_SI,
                    "<%s::parseAndUpdateSVCT> - version: %d, section_number: %d, last_section_number: %d\n",
                    SIMODULE, *version, *section_number, *last_section_number);
            ptr_input_buffer += sizeof(revision_detection_descriptor_struc);
            section_length -= sizeof(revision_detection_descriptor_struc);
        }
    }

    if (section_length > LENGTH_CRC_32)
    {
        do
        {
            ptr_descriptor_struc
                    = (generic_descriptor_struc *) ptr_input_buffer;
            ptr_input_buffer += sizeof(generic_descriptor_struc)
                    + ptr_descriptor_struc->descriptor_length_octet2;
            section_length -= (sizeof(generic_descriptor_struc)
                    + ptr_descriptor_struc->descriptor_length_octet2);

            if (section_length <= 0)
            {
                if (section_length < 0)
                { //error, section_length not large enough to accommodate descriptor(s) .
                    svct_error = 90;
                }
                break;
            }
        } while (section_length > LENGTH_CRC_32);
    }

    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::parseAndUpdateSVCT> - ptr_input_buffer = 0x%p\n", SIMODULE,
            ptr_input_buffer);
    if (section_length == LENGTH_CRC_32)
    {
        *crc = ((*ptr_input_buffer) << 24 | (*(ptr_input_buffer + 1)) << 16
                | (*(ptr_input_buffer + 2)) << 8 | (*(ptr_input_buffer + 3)));
        MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
                "<%s::parseAndUpdateSVCT> - CRC32 = 0x%08x\n", SIMODULE, *crc);
    }

    PARSING_DONE:

    if (svct_error == NO_PARSING_ERROR)
    {
        MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                "<%s::parseAndUpdateSVCT> - SVCT records parsed, set flag.\n",
                SIMODULE);

        //setTablesAcquired(getTablesAcquired() | SVCT_ACQUIRED) ;
    }

    /*
     ** **************************************************************************
     ** * At this point, if section_length = 0, the CRC_32 is present and the table section has been
     ** * processed successfully. A section_length > 0 or section_length < 0  implies that an error has
     ** * occurred and svct_error should identify the error condition.
     ** **************************************************************************
     */
    MPE_LOG(
            MPE_LOG_TRACE4,
            MPE_MOD_SI,
            "<%s::parseAndUpdateSVCT> - Exiting with a SVCT_Error_Code = (%d).\n",
            SIMODULE, svct_error);

    return svct_error;
}

/*
 ** ****************************************************************************
 **  Subroutine/Method:     parseDCMStructure()
 ** ****************************************************************************
 **
 ** Parses Defined Channels Map(DCM) structure, validates the fields within the structure
 **
 ** @param  pptr_TableSection,  the address of memeory location which contains a pointer to the SVCT
 **                             table section.
 ** @param  ptr_section_length, a pointer to the caller's section_length variable.
 **
 ** @return MPE_SUCCESS if table section was successfully processed/parsed .Otherwise an error code
 **         greater than zero (MPE_SUCCESS).
 **
 */

mpe_Error parseDCMStructure(uint8_t ** pptr_TableSection,
        int32_t * ptr_section_length)
{
    dcm_record_struc2 * ptr_dcm_record_struc2;
    dcm_record_struc3 * ptr_dcm_virtual_chan_record3;
    uint16_t first_virtual_channel = 0, dcm_range_count = 0, j = 0, range_defined = NO,
            channels_count = 0;
    uint8_t dcm_data_length = 0;
    mpe_Error svct_error = NO_PARSING_ERROR;
    mpe_Error saved_svct_error = NO_PARSING_ERROR;
    uint16_t virtual_channel_count = 0, virtual_channel = 0;
    mpe_SiServiceHandle si_database_handle = 0;
    uint32_t total_count = 0;
    //***********************************************************************************************
    //***********************************************************************************************

    //***********************************************************************************************
    //**** Parse the  DCM Structure.
    //***********************************************************************************************
    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
            "<%s::parseDCMStructure> - ptr_TableSection = (%p).\n", SIMODULE,
            *pptr_TableSection);

    ptr_dcm_record_struc2 = (dcm_record_struc2 *) *pptr_TableSection;

    //**** validate "Zero" (2-bits) field ****
    if (TESTBITS(ptr_dcm_record_struc2->first_virtual_chan_octet1, 8, 5))
    {
        // error, 1 or more bits are set to 1.
        //svct_error = 26;
        MPE_LOG(
                MPE_LOG_TRACE6,
                MPE_MOD_SI,
                "<%s::parseDCMStructure> - *** WARNING (%d) *** 4-Bit-Zero Field #4 not set to all zeroes(0)\n.",
                SIMODULE, 26);
    }

    // range 0 thru 4095 ((2**12)-1)
    first_virtual_channel = (uint16_t)((GETBITS(
            ptr_dcm_record_struc2->first_virtual_chan_octet1, 4, 1) << 8)
            | ptr_dcm_record_struc2->first_virtual_chan_octet2);
    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
            "<%s::parseDCMStructure> - first_virtual_channel = (%d).\n",
            SIMODULE, first_virtual_channel);
    dcm_data_length = ptr_dcm_record_struc2->dcm_data_length_octet_3;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<%s::parseDCMStructure> - dcm_data_length = (%d).\n", SIMODULE,
            dcm_data_length);

    if (dcm_data_length == 0)
    {
        // error,must be > 0.
        svct_error = 24;
    }

    if (getDumpTables())
    {
        MPE_LOG(
                MPE_LOG_INFO,
                MPE_MOD_SI,
                "<%s::parseDCMStructure> - %d channel ranges starting at channel %d (0x%x)\n",
                SIMODULE, dcm_data_length, first_virtual_channel, first_virtual_channel);
    }

    virtual_channel = first_virtual_channel;

    //***********************************************************************************************
    //*** Adjust pointers to reflect the parsing of the DCM Structure (dcm_record_struc2).
    //***********************************************************************************************
    *pptr_TableSection += sizeof(dcm_record_struc2);
    *ptr_section_length -= sizeof(dcm_record_struc2);
    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
            "<%s::parseDCMStructure> - section_length11 = (%d).\n", SIMODULE,
            *ptr_section_length);
    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
            "<%s::parseDCMStructure> - ptr_TableSection11 = (%p).\n", SIMODULE,
            *pptr_TableSection);

    if (*ptr_section_length <= 0)
    {
        // error, either DCM structure was too short (*ptr_section_length < 0) or
        //        there is no virtual channel record (*ptr_section_length = 0).
        svct_error = 25;
    }

    //***********************************************************************************************
    //**** Parse DCM data section
    //***********************************************************************************************
    if (svct_error == NO_PARSING_ERROR)
    {
        do
        {
            ptr_dcm_virtual_chan_record3
                    = (dcm_record_struc3 *) *pptr_TableSection;

            if (TESTBITS(ptr_dcm_virtual_chan_record3->channel_count_octet1, 8,
                    8))
            {
                range_defined = YES;
            }
            else
            {
                range_defined = NO;
            }

            channels_count = GETBITS(
                    ptr_dcm_virtual_chan_record3->channel_count_octet1, 7, 1);

            if (getDumpTables())
            {
                MPE_LOG(
                        MPE_LOG_INFO,
                        MPE_MOD_SI,
                        "<%s::parseDCMStructure> - Range %-3u[%d channels %s (%d to %d)/(0x%x-0x%x)]\n",
                        SIMODULE, dcm_range_count, channels_count,
                        (range_defined ? "defined" : "undefined"),
                        virtual_channel, virtual_channel+channels_count-1,
                        virtual_channel, virtual_channel+channels_count-1 );
            }
            total_count += channels_count;
            MPE_LOG(
                    MPE_LOG_TRACE6,
                    MPE_MOD_SI,
                    "<%s::parseDCMStructure> - range_defined = %d, channels_count = %d, total_count = %d\n",
                    SIMODULE, range_defined, channels_count, total_count);

            mpe_siLockForWrite();
            for (j = 0; j < channels_count; j++)
            {
                // Look up SI DB handle for each virtual channel starting at first channel
                // If a match is not found this will create a new entry
                mpe_siGetServiceEntryFromChannelNumber(virtual_channel,
                        &si_database_handle);

                if (range_defined)
                {
                    MPE_LOG(
                            MPE_LOG_TRACE6,
                            MPE_MOD_SI,
                            "<%s::parseDCMStructure> - virtual_channel(%d) = %d\n",
                            SIMODULE, virtual_channel_count, virtual_channel);
                    // If 'range_defined' is set, mark this channel as defined
                    // If duplicate entries exist in the database with this channel number
                    // set the defined state
                    mpe_siSetServiceEntriesStateDefined(virtual_channel);

                    virtual_channel_count++;
                }
                else
                {
                    // If 'range_defined' is NOT set, mark this channel as undefined
                    // If duplicate entries exist in the database with this channel number
                    // set the undefined state
                    mpe_siSetServiceEntriesStateUnDefined(virtual_channel);
                }
                // Increment the channel counter
                virtual_channel++;
            }
            mpe_siReleaseWriteLock();

            *pptr_TableSection += sizeof(dcm_record_struc3);
            *ptr_section_length -= sizeof(dcm_record_struc3);

            dcm_data_length--;
            dcm_range_count++;

            if (*ptr_section_length <= 0)
            {
                // error, either DCM structure was too short (*ptr_section_length < 0) or
                //        there is no virtual channel record (*ptr_section_length = 0).
                // not used svct_error = 26 ;
            }
        } while (dcm_data_length != 0);
    }
    else
    {
        saved_svct_error = svct_error;
    }

    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::parseDCMStructure> - Exiting with a SVCT_Error_Code = %d.\n",
            SIMODULE, saved_svct_error);
    return saved_svct_error;
}

/*
 ** ****************************************************************************
 **  Subroutine/Method:     parseVCMStructure()
 ** ****************************************************************************
 **
 ** Parses Virtual Channel Map(VCM) structure, validates the fields within the structure
 **
 ** @param  pptr_TableSection,  the address of memeory location which contains a pointer to the SVCT
 **                             table section.
 ** @param  ptr_section_length, a pointer to the caller's section_length variable.
 **
 ** @return MPE_SUCCESS if table section was successfully processed/parsed .Otherwise an error code
 **         greater than zero (MPE_SUCCESS).
 **
 */
mpe_Error parseVCMStructure(uint8_t **pptr_TableSection,
        int32_t *ptr_section_length)
{
    vcm_record_struc2 *ptr_vcm_record_struc2;
    vcm_virtual_chan_record3 *ptr_vcm_virtual_chan_record3;
    mpeg2_virtual_chan_struc4 *ptr_mpeg2_virtual_chan_struc4;
    nmpeg2_virtual_chan_struc5 *ptr_nmpeg2_virtual_chan_struc5;
    generic_descriptor_struc *ptr_descriptor_struc;
    mpe_SiServiceHandle si_database_handle = 0; // a handle into ths SI database

    uint32_t i, descriptor_count = 0;
    uint16_t program_number = 0, number_of_vc_records = 0,
            virtual_channel_number = 0, channel_type = 0, application_id = 0,
            source_id = 0, video_standard = 0, size_struc_used = 0,
            application_virtual_channel = NO, path_select = PATH_1_SELECTED,
            transport_type = MPEG_2_TRANSPORT, descriptors_included = NO,
            scrambled = NO, splice = NO;
    uint8_t cds_reference = 0, mms_reference = 0;

    uint32_t activation_time = 0;
    mpe_Error svct_error = NO_PARSING_ERROR, saved_svct_error =
            NO_PARSING_ERROR;

    //***********************************************************************************************
    //***********************************************************************************************

    //***********************************************************************************************
    //**** Parse the  VCM Structure.
    //***********************************************************************************************
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::parseVCMStructure> - Entered method parseVCMStructure.\n",
            SIMODULE);
    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
            "<%s::parseVCMStructure> - ptr_TableSection = (%p).\n", SIMODULE,
            *pptr_TableSection);

    ptr_vcm_record_struc2 = (vcm_record_struc2 *) *pptr_TableSection;

    //**** validate "Zero" (2-bits) field ****
    if (TESTBITS(ptr_vcm_record_struc2->descriptors_included_octet1, 8, 7))
    {
        // error, bits not set to 0.
        //svct_error = 20 ;
        MPE_LOG(
                MPE_LOG_TRACE4,
                MPE_MOD_SI,
                "<%s::parseVCMStructure> - *** WARNING *** 2-Bit-Zero Field #1 not set to all zeroes(0)\n.",
                SIMODULE);
    }

    if (TESTBITS(ptr_vcm_record_struc2->descriptors_included_octet1, 6, 6))
    {
        descriptors_included = YES;
    }

    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::parseVCMStructure> - descriptors_included = (%d).\n",
            SIMODULE, descriptors_included);

    //**** validate "Zero" (5-bits) field ****
    if (TESTBITS(ptr_vcm_record_struc2->descriptors_included_octet1, 5, 1))
    {
        // error, bits not set to 0.
        //svct_error = 21 ;
        MPE_LOG(
                MPE_LOG_TRACE4,
                MPE_MOD_SI,
                "<%s::parseVCMStructure> - *** WARNING *** 5-Bit-Zero Field #2 not set to all zeroes(0)\n.",
                SIMODULE);
    }

    if (TESTBITS(ptr_vcm_record_struc2->splice_octet2, 8, 8))
    {
        splice = YES;
    }

    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::parseVCMStructure> - splice = (%d).\n", SIMODULE, splice);

    //**** validate "Zero" (7-bits) field ****
    if (TESTBITS(ptr_vcm_record_struc2->splice_octet2, 7, 1))
    {
        // error, 1 or more bits are set to 1.
        //svct_error = 22 ;
        MPE_LOG(
                MPE_LOG_TRACE4,
                MPE_MOD_SI,
                "<%s::parseVCMStructure> - *** WARNING *** 7-Bit-Zero Field #3 not set to all zeroes(0)\n.",
                SIMODULE);
    }

    activation_time = (ptr_vcm_record_struc2->activation_time_octet3 << 24)
            | (ptr_vcm_record_struc2->activation_time_octet4 << 16)
            | (ptr_vcm_record_struc2->activation_time_octet5 << 8)
            | ptr_vcm_record_struc2->activation_time_octet6;
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::parseVCMStructure> - activation_time = (%d).\n", SIMODULE,
            activation_time);

    /*
     if (activation_time != IMMEDIATE_ACTIVATION)
     {
     // per Debra H. for 06-07-04 release.
     svct_error = 23 ;
     }
     */

    number_of_vc_records = ptr_vcm_record_struc2->number_vc_records_octet7;

    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::parseVCMStructure> - number_of_vc_records = (%d).\n",
            SIMODULE, number_of_vc_records);

    if (number_of_vc_records == 0)
    {
        // error,must be > 0.
        svct_error = 24;
    }

    //***********************************************************************************************
    //*** Adjust pointers to reflect the parsing of the VCM Structure (vcm_record_struc2).
    //***********************************************************************************************
    *pptr_TableSection += sizeof(vcm_record_struc2);
    *ptr_section_length -= sizeof(vcm_record_struc2);
    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
            "<%s::parseVCMStructure> - section_length11 = (%d).\n", SIMODULE,
            *ptr_section_length);
    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
            "<%s::parseVCMStructure> - ptr_TableSection11 = (%p).\n", SIMODULE,
            *pptr_TableSection);

    if (*ptr_section_length <= 0)
    {
        // error, either VCM structure was too short (*ptr_section_length < 0) or
        //        there is no virtual channel record (*ptr_section_length = 0).
        svct_error = 25;
    }

    //***********************************************************************************************
    //**** Parse each Virtual Channel Record
    //***********************************************************************************************
    if (svct_error == NO_PARSING_ERROR)
    {
        for (i = 0; i < number_of_vc_records; ++i)
        {
            application_virtual_channel = NO;

            MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                    "<%s::parseVCMStructure> - index = %d.\n", SIMODULE, i);

            ptr_vcm_virtual_chan_record3
                    = (vcm_virtual_chan_record3 *) *pptr_TableSection;

            //**** validate "Zero" (4-bits) field ****
            if (TESTBITS(
                    ptr_vcm_virtual_chan_record3->virtual_chan_number_octet1,
                    8, 5))
            {
                // error, 1 or more bits are set to 1.
                //svct_error = 26;
                MPE_LOG(
                        MPE_LOG_TRACE6,
                        MPE_MOD_SI,
                        "<%s::parseVCMStructure> - *** WARNING (%d) *** 4-Bit-Zero Field #4 not set to all zeroes(0)\n.",
                        SIMODULE, i);
            }

            // range 0 thru 4095 ((2**12)-1)
            virtual_channel_number = (uint16_t)((GETBITS(
                    ptr_vcm_virtual_chan_record3->virtual_chan_number_octet1,
                    4, 1) << 8)
                    | ptr_vcm_virtual_chan_record3->virtual_chan_number_octet2);

            if (TESTBITS(ptr_vcm_virtual_chan_record3->chan_type_octet3, 8, 8))
            {
                application_virtual_channel = YES;
            }

            //MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,"<%s::parseVCMStructure> - application_virtual_channel(%d) = (%d).\n",
            //      SIMODULE,i, application_virtual_channel);

            //**** validate "Zero" (1-bit) field ****
            if (TESTBITS(ptr_vcm_virtual_chan_record3->chan_type_octet3, 7, 7))
            {
                // error, bits not set to 0.
                //svct_error = 27;
                MPE_LOG(
                        MPE_LOG_TRACE6,
                        MPE_MOD_SI,
                        "<%s::parseVCMStructure> - *** WARNING (%d) *** 1-Bit-Zero Field #5 not set to all zeroes(0)\n.",
                        SIMODULE, i);
            }
            if (TESTBITS(ptr_vcm_virtual_chan_record3->chan_type_octet3, 6, 6))
            {
                path_select = PATH_2_SELECTED;
            }

            MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                    "<%s::parseVCMStructure> - path_select(%d) = (%d).\n",
                    SIMODULE, i, path_select);
            MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                    "\n<parseVCMStructure> - transport_type = %d\n",
                    transport_type);

            if (TESTBITS(ptr_vcm_virtual_chan_record3->chan_type_octet3, 5, 5))
            {
                transport_type = NON_MPEG_2_TRANSPORT;
                // svct_error     = 28;        //per Prasanna, remove when we want to process this type
                program_number = 0; // HACK - need to grok non-MPEG2 types, need set-er on SIDB
                mms_reference = 0;

                MPE_LOG(
                        MPE_LOG_TRACE6,
                        MPE_MOD_SI,
                        "<%s::parseVCMStructure> - *** WARNING *** VCM record %i is non-MPEG_2 transport_type - entering as program 0/mms 0.\n",
                        SIMODULE, i);
            }
            else
            {
                transport_type = MPEG_2_TRANSPORT;
            }

            MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                    "<%s::parseVCMStructure> - transport_type(%d) = (%d).\n",
                    SIMODULE, i, transport_type);

            channel_type = (uint16_t)(GETBITS(
                    ptr_vcm_virtual_chan_record3->chan_type_octet3, 4, 1));

            MPE_LOG(MPE_LOG_TRACE3, MPE_MOD_SI,
                    "<%s::parseVCMStructure> - channel_type(%d) = (%d).\n",
                    SIMODULE, i, channel_type);

            if (channel_type > HIDDEN_CHANNEL)
            {
                // error, reserved channel type.
                //svct_error = 29;
                MPE_LOG(
                        MPE_LOG_TRACE6,
                        MPE_MOD_SI,
                        "<%s::parseVCMStructure> - *** WARNING (%d) *** Reserved Channel_Type Value (2...15) used.\n",
                        SIMODULE, i);
            }

            MPE_LOG(
                    MPE_LOG_TRACE6,
                    MPE_MOD_SI,
                    "<%s::parseVCMStructure> - virtual_channel_number(%d) = %d channel_type(%d) = %d.\n",
                    SIMODULE, i, virtual_channel_number, i, channel_type);

            source_id
                    = (uint16_t)(
                            (ptr_vcm_virtual_chan_record3->app_or_source_id_octet4
                                    << 8)
                                    | ptr_vcm_virtual_chan_record3->app_or_source_id_octet5);
            application_id = 0;

            if (application_virtual_channel)
            {
                application_id = source_id;
                MPE_LOG(
                        MPE_LOG_TRACE6,
                        MPE_MOD_SI,
                        "<%s::parseVCMStructure> - application_id(%d)(0x%x) = (%d).\n",
                        SIMODULE, i, application_id, application_id);

                if (application_id == 0)
                {
                    // error, must be > 0 and <= 0xFFFF.
                    svct_error = 30;
                }
                // application virtual channels are supported (not considered error)
                MPE_LOG(
                        MPE_LOG_TRACE6,
                        MPE_MOD_SI,
                        "<%s::parseVCMStructure> - application_id(%d) = (%d).\n",
                        SIMODULE, i, application_id);
            }
            else
            {
                MPE_LOG(
                        MPE_LOG_TRACE6,
                        MPE_MOD_SI,
                        "<%s::parseVCMStructure> - source_id(%d)(0x%x) = (%d).\n",
                        SIMODULE, i, source_id, source_id);
            }

            cds_reference = ptr_vcm_virtual_chan_record3->cds_reference_octet6;

            //         MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,"<%s::parseVCMStructure> - cds_reference(%d) = (%d).\n", SIMODULE,i, cds_reference);

            if ((transport_type == MPEG_2_TRANSPORT) && (cds_reference == 0))
            {
                // error, must be >=1
                svct_error = 32;
            }

            //         MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,"\n<parseVCMStructure> - transport_type = %d\n", transport_type);
            //         MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,"\n<parseVCMStructure> - program_number = %d\n", program_number);

            //***********************************************************************************************
            //**** Adjust section length and pointer to table section to account for having parsed
            //**** the vcm_virtual_chan_record3. Now parse either the mpeg2_virtual_chan_struc4
            //**** or nmpeg2_virtual_chan_struc5 portion of the the Virtual Channel Record .
            //***********************************************************************************************
            *pptr_TableSection += sizeof(vcm_virtual_chan_record3);
            *ptr_section_length -= sizeof(vcm_virtual_chan_record3);

            MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                    "<%s::parseVCMStructure> - section_length12 = (%d).\n",
                    SIMODULE, *ptr_section_length);
            MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                    "<%s::parseVCMStructure> - ptr_TableSection12 = (%p).\n",
                    SIMODULE, *pptr_TableSection);

            if (transport_type == MPEG_2_TRANSPORT)
            {
                ptr_mpeg2_virtual_chan_struc4
                        = (mpeg2_virtual_chan_struc4 *) *pptr_TableSection;
                size_struc_used = sizeof(mpeg2_virtual_chan_struc4);

                program_number
                        = (uint16_t)(
                                (ptr_mpeg2_virtual_chan_struc4->program_number_octet1
                                        << 8)
                                        | ptr_mpeg2_virtual_chan_struc4->program_number_octet2);
                MPE_LOG(
                        MPE_LOG_TRACE6,
                        MPE_MOD_SI,
                        "<%s::parseVCMStructure> - program_number(%d) = (%d).\n",
                        SIMODULE, i, program_number);

                mms_reference
                        = ptr_mpeg2_virtual_chan_struc4->mms_number_octet3;

                MPE_LOG(
                        MPE_LOG_TRACE6,
                        MPE_MOD_SI,
                        "<%s::parseVCMStructure> - mms_reference(%d) = (%d).\n",
                        SIMODULE, i, mms_reference);

                if (mms_reference == 0)
                {
                    // error, must be >=1
                    svct_error = 33;
                }
            }
            else
            {
                ptr_nmpeg2_virtual_chan_struc5
                        = (nmpeg2_virtual_chan_struc5 *) *pptr_TableSection;
                size_struc_used = sizeof(nmpeg2_virtual_chan_struc5);

                if (TESTBITS(
                        ptr_nmpeg2_virtual_chan_struc5->video_standard_octet1,
                        8, 8))
                {
                    scrambled = YES;
                }

                MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                        "<%s::parseVCMStructure> - scrambled(%d) = (%d).\n",
                        SIMODULE, i, scrambled);

                //**** validate "Zero" (3-bit) field ****
                if (TESTBITS(
                        ptr_nmpeg2_virtual_chan_struc5->video_standard_octet1,
                        7, 5))
                {
                    // error, 1 or more bits are set to 1 .
                    //svct_error = 34;
                    MPE_LOG(
                            MPE_LOG_WARN,
                            MPE_MOD_SI,
                            "<%s::parseVCMStructure> - *** WARNING (%d) *** 3-Bit-Zero Field #6 not set to all zeroes(0)\n.",
                            SIMODULE, i);
                }

                video_standard = (uint16_t)(GETBITS(
                        ptr_nmpeg2_virtual_chan_struc5->video_standard_octet1,
                        4, 1));

                MPE_LOG(
                        MPE_LOG_TRACE6,
                        MPE_MOD_SI,
                        "<%s::parseVCMStructure> - video_standard(%d) = (%d).\n",
                        SIMODULE, i, video_standard);

                if (video_standard > MAC_VIDEO_STANDARD)
                {
                    // error, range is NTSC_VIDEO_STANDARD (0) thru MAC_VIDEO_STANDARD (4) .
                    svct_error = 35;
                }

                if ((ptr_nmpeg2_virtual_chan_struc5->eight_zeroes_octet2)
                        || (ptr_nmpeg2_virtual_chan_struc5->eight_zeroes_octet3))
                {
                    // error, 1 or more bits are set to 1.
                    //svct_error = 36;
                    MPE_LOG(
                            MPE_LOG_TRACE6,
                            MPE_MOD_SI,
                            "<%s::parseVCMStructure> - *** WARNING (%d) *** 16-Bit-Zero Field #7 not set to all zeroes(0)\n.",
                            SIMODULE, i);
                }
            }

            //***********************************************************************************************
            //**** Adjust section length and pointer to table section buffer to account for having parsed
            //**** the mpeg2_virtual_chan_struc4/mpeg2_virtual_chan_struc5 stucture and prepare to
            //**** to process descriptors.
            //***********************************************************************************************
            *pptr_TableSection += size_struc_used;
            *ptr_section_length -= size_struc_used;

            MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                    "<%s::parseVCMStructure> - section_length13 = (%d).\n",
                    SIMODULE, *ptr_section_length);
            MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                    "<%s::parseVCMStructure> - ptr_TableSection13 = (%p).\n",
                    SIMODULE, *pptr_TableSection);

            if (*ptr_section_length <= 0)
            {
                if (*ptr_section_length < 0)
                {
                    // error, section length was shorter than the number of octets/bytes parsed.
                    svct_error = 37;
                    break;
                }
                else if (descriptors_included == YES)
                {
                    // error, section length (0) is not large enough to hold any descriptors
                    svct_error = 38;
                    break;
                }
            }

            //*******************************************************************************************
            //    Process descriptors.
            //*******************************************************************************************
            if (descriptors_included == YES)
            {
                descriptor_count = **pptr_TableSection; // get 8-bit descriptor count
                ++(*pptr_TableSection);
                --(*ptr_section_length);

                MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                        "<%s::parseVCMStructure> - section_length14 = (%d).\n",
                        SIMODULE, *ptr_section_length);
                MPE_LOG(
                        MPE_LOG_TRACE6,
                        MPE_MOD_SI,
                        "<%s::parseVCMStructure> - ptr_TableSection14 = (%p).\n",
                        SIMODULE, *pptr_TableSection);
                MPE_LOG(
                        MPE_LOG_TRACE6,
                        MPE_MOD_SI,
                        "<%s::parseVCMStructure> - descriptor_count14 = (%d).\n",
                        SIMODULE, descriptor_count);

                while (descriptor_count > 0)
                { // ignore all descriptors .

                    ptr_descriptor_struc
                            = (generic_descriptor_struc *) *pptr_TableSection;
                    *pptr_TableSection += sizeof(generic_descriptor_struc)
                            + ptr_descriptor_struc->descriptor_length_octet2;

                    //*** decrement section_length as we did not account for descriptor tag, length and data fields
                    //*** length and data fields prior to calling subroutine .

                    --descriptor_count;
                    *ptr_section_length -= (sizeof(generic_descriptor_struc)
                            + ptr_descriptor_struc->descriptor_length_octet2);

                    MPE_LOG(
                            MPE_LOG_TRACE6,
                            MPE_MOD_SI,
                            "<%s::parseVCMStructure> - section_length15 = (%d).\n",
                            SIMODULE, *ptr_section_length);
                    MPE_LOG(
                            MPE_LOG_TRACE6,
                            MPE_MOD_SI,
                            "<%s::parseVCMStructure> - ptr_TableSection15 = (%p).\n",
                            SIMODULE, *pptr_TableSection);
                    MPE_LOG(
                            MPE_LOG_TRACE6,
                            MPE_MOD_SI,
                            "<%s::parseVCMStructure> - descriptor_count14 = (%d).\n",
                            SIMODULE, descriptor_count);

                    if (*ptr_section_length < 0)
                    { //error, section_length not large enough to account for descriptor tags, length and data fields
                        svct_error = 40;
                        break;
                    }
                } // end_of_while loop
            }

            //*******************************************************************************************
            //    Update the SI database with the reqired data for this Virtual Channel Record.
            //*******************************************************************************************
            MPE_LOG(
                    MPE_LOG_TRACE6,
                    MPE_MOD_SI,
                    "<%s::parseVCMStructure> - Virtual Channel Record (%d) parse %s.\n",
                    SIMODULE, i, svct_error == 0 ? "complete - no errors"
                            : "complete - errors");

            // TODO - Add digital only processing here (skip analog channels)

            if (svct_error == NO_PARSING_ERROR)
            {
                si_database_handle = MPE_SI_INVALID_HANDLE;
                uint32_t si_state = 0;

                MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                        "<%s::parseVCMStructure> - source_id(%d) = (%d).\n",
                        SIMODULE, i, source_id);

                if (getDumpTables())
                {
                    if (transport_type == MPEG_2_TRANSPORT)
                    {
                        //printf
                        MPE_LOG(
                                MPE_LOG_INFO,
                                MPE_MOD_SI,
                                "<%s::parseVCMStructure> - Ent %-3u[VC:%u(0x%x) AVC:%d PS:%d CT:%d %s:%d(0x%x) "
                                    "TT:MPEG2(dig) CDS:%d Prog#:%d(0x%x) MMS:%d]\n",
                                SIMODULE, i, virtual_channel_number,
                                virtual_channel_number,
                                application_virtual_channel, path_select,
                                channel_type,
                                (application_virtual_channel ? "AID" : "SID"),
                                (application_virtual_channel ? application_id
                                        : source_id),
                                (application_virtual_channel ? application_id
                                        : source_id), cds_reference,
                                program_number, program_number, mms_reference);
                        ;
                    }
                    else
                    {
                        //printf
                        MPE_LOG(
                                MPE_LOG_INFO,
                                MPE_MOD_SI,
                                "<%s::parseVCMStructure> - Ent %-3u[VC:%d(0x%x) AVC:%d PS:%d CT:%d %s:%d(0x%x) "
                                    "TT:non-MPEG2(analog) CDS:%d scram:%d VS:%d]\n",
                                SIMODULE, i, virtual_channel_number,
                                virtual_channel_number,
                                application_virtual_channel, path_select,
                                channel_type,
                                (application_virtual_channel ? "AID" : "SID"),
                                (application_virtual_channel ? application_id
                                        : source_id),
                                (application_virtual_channel ? application_id
                                        : source_id), cds_reference, scrambled,
                                video_standard);
                        ;
                    }
                }

                mpe_siLockForWrite();

                // Look up SI DB handle based on channel number
                // This creates a new handle if one is not found
                mpe_siGetServiceEntryFromChannelNumber(virtual_channel_number,
                        &si_database_handle);
                if (si_database_handle != MPE_SI_INVALID_HANDLE)
                {
                    uint32_t tmpSourceId=0;
                    uint32_t tmp_app_id=0;
                    uint32_t tmp_prog_num=0;

                    mpe_siGetServiceEntryState(si_database_handle, &si_state);
                    MPE_LOG(MPE_LOG_TRACE6,
                            MPE_MOD_SI,
                            "<%s::parseVCMStructure> - si_database_handle:0x%x si_state:%d\n",
                            SIMODULE, si_database_handle, si_state);
                    if((si_state & SIENTRY_MAPPED) == SIENTRY_MAPPED)
                    {
                        mpe_siGetSourceIdForServiceHandle(si_database_handle, &tmpSourceId);
                        mpe_siGetAppIdForServiceHandle(si_database_handle, &tmp_app_id);
                        mpe_siGetProgramNumberForServiceHandle(si_database_handle, &tmp_prog_num);

                        if(application_virtual_channel == NO)
                        {
                            MPE_LOG(MPE_LOG_TRACE6,
                                    MPE_MOD_SI,
                                    "<%s::parseVCMStructure> - virtual_channel_number:%d source_id:%d tmpSourceId:%d  program_number:%d tmp_prog_num:%d\n",
                                    SIMODULE, virtual_channel_number, source_id, tmpSourceId, program_number, tmp_prog_num);
                        }
                        else
                        {
                            MPE_LOG(MPE_LOG_TRACE6,
                                    MPE_MOD_SI,
                                    "<%s::parseVCMStructure> - virtual_channel_number:%d appId:%d tmp_app_id:%d \n",
                                    SIMODULE, virtual_channel_number, application_id, tmp_app_id);
                        }

                        // Clone the entry only if the virtual channel number is 0
                        // (It was observed on RI that virtual channel 0 appeared many times
                        // in the VCM with diff sourceId and/or program number)
                        // This may be a special virtual channel number which the cable
                        // operators use to signal discrete sourceIds that are still discoverable
                        // by apps.
                        // In all other cases (virtual_channel_number != 0) if an existing SI DB entry
                        // is found with this virtual channel number it is updated
                        if( ((application_virtual_channel == YES) && (tmp_app_id != application_id))
                            || ( (application_virtual_channel == NO)
                                 &&  (virtual_channel_number == 0)
                                 && ((tmpSourceId != source_id)
                                      || (tmp_prog_num != program_number)) ) )
                        {

                            MPE_LOG(
                            		MPE_LOG_TRACE6,
                                    MPE_MOD_SI,
                                    "<%s::parseVCMStructure> - Found an mapped entry with this virtual channel number (but diff sourceId, fpq), cloning.. \n",
                                    SIMODULE);

                            (void)mpe_siInsertServiceEntryForChannelNumber(virtual_channel_number, &si_database_handle);
                            (void)mpe_siSetServiceEntryState(si_database_handle, si_state);
                        }
                    }
                    if(application_virtual_channel == NO)
                    {
                        (void) mpe_siSetSourceId(si_database_handle, source_id);
                    }
                    else
                    {
                        (void) mpe_siSetAppId(si_database_handle, application_id);
                    }

                    mpe_siSetAppType(si_database_handle, application_virtual_channel);
                    mpe_siSetActivationTime(si_database_handle, activation_time);
                    //Since we do not have the two part channel number, use the virtual as the the major and the DEFAULT as
                    //the minor
                    (void) mpe_siSetChannelNumber(si_database_handle,
                            virtual_channel_number,
                            MPE_SI_DEFAULT_CHANNEL_NUMBER);
                    mpe_siSetCDSRef(si_database_handle, (uint8_t) cds_reference);
                    mpe_siSetMMSRef(si_database_handle, (uint8_t) mms_reference);
                    mpe_siSetChannelType(si_database_handle,
                            (uint8_t) channel_type);
                    /* We don't want to merge this Axiom fix, but rather allow multiple transport streams
                     be created with same frequency but different modulation modes.*/
                    /*
                     if (channel_type != CHANNEL_TYPE_HIDDEN)
                     {   // Ensure non-hidden sources 'win' if there's a modulation conflict.
                     mpe_SiTransportStreamEntry *ts = (mpe_SiTransportStreamEntry *)((mpe_SiTableEntry *)si_database_handle)->ts_handle;
                     mpe_SiModulationMode modulation = (transport_type == NON_MPEG_2_TRANSPORT) ? MPE_SI_MODULATION_QAM_NTSC : mode_by_mms_ref;
                     if (ts != NULL)
                     {
                     if (ts->modulation_mode != modulation)
                     {
                     MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,"<%s::parseVCMStructure> - reset modulation mode for frequency %d from %d to %d\n", SIMODULE, ts->frequency, ts->modulation_mode, mode_by_mms_ref);
                     ts->modulation_mode = modulation;
                     }
                     }
                     }
                     */
                    mpe_siSetProgramNumber(si_database_handle, program_number);
                    mpe_siSetTransportType(si_database_handle, transport_type);
                    mpe_siSetScrambled(si_database_handle, scrambled);
                    if (transport_type == NON_MPEG_2_TRANSPORT)
                    {
                        MPE_LOG(
                                MPE_LOG_TRACE6,
                                MPE_MOD_SI,
                                "<%s::parseVCMStructure> - setting analog modulation mode.\n",
                                SIMODULE);
                        mpe_siSetVideoStandard(si_database_handle,
                                video_standard);

                        // Kludge!!! (Since we cannot determine service_type from profile-1 SI
                        // we are just going to use the 'transport_type' field in SVCT to
                        // determine the service type.
                        //mpe_siSetServiceType(si_database_handle, MPE_SI_SERVICE_ANALOG_TV);
                    }
                    else if (transport_type == MPEG_2_TRANSPORT)
                    {
                        // Not sure if we can assume that if transport_type is
                        // MPEG2 that the service is digital.
                        // How to differentiate between DIGITAL_TV, DIGITAL_RADIO
                        // and DATA_BROADCAST etc.

                        // Kludge!!! (Since we cannot determine service_type from profile-1 SI
                        // we are just going to use the 'transport_type' field in SVCT to
                        // determine the service type.  It is important to set to unknown
                        // here, in case we're doing an update and the si element was previously
                        // used by an analog service.
                        mpe_siSetServiceType(si_database_handle,
                                MPE_SI_SERVICE_TYPE_UNKNOWN);
                    }
                    mpe_siSetServiceEntryStateMapped(si_database_handle);
                }
                mpe_siReleaseWriteLock();
            }
            else
            {
                saved_svct_error = svct_error;
                svct_error = NO_PARSING_ERROR;
                if (saved_svct_error == 40)
                { //error, section_length not large enough To account for descriptor tags, length and data fields
                    break;
                }
            }
        } //end_of_for_loop
    }

    (void) svct_error;

    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::parseVCMStructure> - Exiting with a SVCT_Error_Code = %d.\n",
            SIMODULE, saved_svct_error);
    return saved_svct_error;
}

//***************************************************************************************************
//  Subroutine/Method:                       parseAndUpdateNIT()
//***************************************************************************************************
/**
 * Parse NTT table section and update SI database.
 *
 * @param  ptr_TableSection,  a pointer to the start of the NIT table section.
 * @param  sectiontable_size, The number of bytes in the table section per the PowerTV OS.
 *
 * @return MPE_SUCCESS if table section was successfully processed/parsed .Otherwise an error code
 *         greater than zero (MPE_SUCCESS).
 */
mpe_Error parseAndUpdateNTT(mpe_FilterSectionHandle section_handle,
        uint8_t *version, uint8_t *section_number,
        uint8_t *last_section_number, uint32_t *crc)
{

    revision_detection_descriptor_struc * ptr_rdd_table_struc1 = NULL;
    mpe_Error ntt_error = NO_PARSING_ERROR;
    mpe_Error retCode = MPE_SUCCESS;
    ntt_record_struc1 *ptr_table_struc1 = NULL;
    generic_descriptor_struc *ptr_descriptor_struc = NULL;
    int32_t section_length = 0;
    char language_code[4];
    uint16_t table_subtype = 0;
    uint32_t number_bytes_copied = 0;
    uint8_t input_buffer[MAX_IB_SECTION_LENGTH];
    uint8_t *ptr_input_buffer = NULL;

    /*
     ** ********************************************************************************************
     ** Parse and validate the first 8 bytes (Table_ID thru table_subtype) of the NTT
     ** table (see document ANSI/SCTE 65-2002 (formerly DVS 234), chap 5.2,pg.20) .
     ** *********************************************************************************************
     */
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::parseAndUpdateNTT> - Entered method parseAndUpdateNTT.\n",
            SIMODULE);

    retCode = mpeos_filterSectionRead(section_handle, 0,
            sizeof(ntt_record_struc1), 0, input_buffer, &number_bytes_copied);
    if (retCode != MPE_SUCCESS || number_bytes_copied
            != sizeof(ntt_record_struc1))
    {
        ntt_error = 1;
        goto PARSING_DONE;
    }
    ptr_table_struc1 = (ntt_record_struc1 *) input_buffer;

    // **** validate table Id field ****
    if (ptr_table_struc1->table_id_octet1 != NETWORK_TEXT_TABLE_ID)
    {
        ntt_error = 2;
    }

    // **** extract and validate section_length field (octets 2 & 3)         ****
    // **** note, the maximum size of a NTT excluding octet 1 thru 3 is 1021.****
    // **** and occupies only bits 1 thru of the 12 bits                     ****

    section_length = ((GETBITS(ptr_table_struc1->section_length_octet2, 4, 1))
            << 8) | ptr_table_struc1->section_length_octet3;
    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
            "<%s::parseAndUpdateNTT> - Section_Length = (%d)\n.", SIMODULE,
            section_length);
    //the following check will fail since the actual section_length will be 1024 during test
    // need to follow up on this issue
    /* if (section_length > MAX_IB_SECTION_LENGTH)
     {
     //error, sectionTable is not large enough to hold a section of size section_length
     ntt_error = 3 ;
     goto PARSING_DONE;
     }
     */

    // get the language_code
    language_code[0] = (char) (ptr_table_struc1->ISO_639_language_code_octet5);
    language_code[1] = (char) (ptr_table_struc1->ISO_639_language_code_octet6);
    language_code[2] = (char) (ptr_table_struc1->ISO_639_language_code_octet7);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI,
            "<%s::parseAndUpdateNTT> - language code = 0x%x 0x%x 0x%x\n.",
            SIMODULE, (unsigned char) language_code[0],
            (unsigned char) language_code[1], (unsigned char) language_code[2]);

    // SCTE65 Section 5.2 indicates that the ISO639 language code may be specified as
    // 0xFFFFFF if the NTT is specified in only a single language
    if (language_code[0] == (char) 0xFF && language_code[1] == (char) 0xFF
            && language_code[2] == (char) 0xFF)
        language_code[0] = '\0';
    else
        language_code[3] = '\0';

    if (TESTBITS(ptr_table_struc1->table_subtype_octet8, 8, 5) != 0)
    {
        // bits 8 thru 5 shall be set to zero (0) per ANSI/SCTE 65-2002,pg.14, para 5.1
        ntt_error = 5;
    }

    //**** extract and validate the table_subtype field ****

    table_subtype = GETBITS(ptr_table_struc1->table_subtype_octet8, 4, 1);

    if (table_subtype != SOURCE_NAME_SUBTABLE)
    {
        // error, invalid table_subtype value.
        ntt_error = 6;
    }
    section_length -= (sizeof(ntt_record_struc1) - LEN_OCTET1_THRU_OCTET3);
    ptr_table_struc1 += sizeof(ntt_record_struc1);
    MPE_LOG(
            MPE_LOG_TRACE6,
            MPE_MOD_SI,
            "<%s::parseAndUpdateNTT> - Table_Subtype = (%d) section_length: %d, ptr: 0x%p\n",
            SIMODULE, table_subtype, section_length, ptr_table_struc1);

    if ((ntt_error == NO_PARSING_ERROR) && (section_length < LENGTH_CRC_32))
    { // error, either a portion of header is missing, or all or a portion of the
        // CRC_32 value is missing.
        ntt_error = 7;
    }

    //**********************************************************************************************
    // Parse the remaining bytes/octets in the NTT.
    //**********************************************************************************************

    retCode = mpeos_filterSectionRead(section_handle, 8, section_length, 0,
            input_buffer, (uint32_t *) &number_bytes_copied);

    if ((MPE_SUCCESS != retCode) || (number_bytes_copied
            != (uint32_t) section_length))
    {
        ntt_error = 8;
        goto PARSING_DONE;
    }

    ptr_input_buffer = input_buffer;
    MPE_LOG(
            MPE_LOG_TRACE6,
            MPE_MOD_SI,
            "<%s::parseAndUpdateNTT> - before sns parse call - ptr_table_struc1 = (0x%p bytes copied: %d\n",
            SIMODULE, ptr_input_buffer, number_bytes_copied);

    //****************************************************************************************
    //**** Process the SNS subtable
    //****************************************************************************************
    ntt_error = parseSNSSubtable(&ptr_input_buffer, &section_length,
            language_code);

    // if ntt_error == NO_PARSING_ERROR, ptr_TableSection has been properly updated
    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
            "<%s::parseAndUpdateNTT> - section_length: %d, ptr: 0x%p\n",
            SIMODULE, section_length, ptr_input_buffer);

    //*************************************************************************************************
    //* At this point, there may be a series a descriptors present.However, we are not required to
    //* process them for the 06-07-04 PowerTV release. If there are descriptors present, we will
    //* "ignore" them.
    //*************************************************************************************************
    if ((ntt_error == NO_PARSING_ERROR) && (section_length > LENGTH_CRC_32))
    {
        ptr_rdd_table_struc1
                = (revision_detection_descriptor_struc *) ptr_input_buffer;

        if (ptr_rdd_table_struc1->descriptor_tag_octet1 == 0x93)
        {
            *version = (ptr_rdd_table_struc1->table_version_number_octet3
                    & 0x1F);
            *section_number = ptr_rdd_table_struc1->section_number_octet4;
            *last_section_number
                    = ptr_rdd_table_struc1->last_section_number_octet5;
            MPE_LOG(
                    MPE_LOG_TRACE4,
                    MPE_MOD_SI,
                    "<%s::parseAndUpdateNTT> - version: %d, section_number: %d, last_section_number: %d\n",
                    SIMODULE, *version, *section_number, *last_section_number);
            ptr_input_buffer += sizeof(revision_detection_descriptor_struc);
            section_length -= sizeof(revision_detection_descriptor_struc);
        }
    }

    if (section_length > LENGTH_CRC_32)
    {
        do
        {
            ptr_descriptor_struc
                    = (generic_descriptor_struc *) ptr_input_buffer;
            ptr_input_buffer += sizeof(generic_descriptor_struc)
                    + ptr_descriptor_struc->descriptor_length_octet2;
            section_length -= (sizeof(generic_descriptor_struc)
                    + ptr_descriptor_struc->descriptor_length_octet2);

            if (section_length <= 0)
            {
                if (section_length < 0)
                { //error, section_length not large enough to accommodate descriptor(s) .
                    ntt_error = 90;
                }
                break;
            }
        } while (section_length > LENGTH_CRC_32);
    }

    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
            "<%s::parseAndUpdateNTT> - ptr_input_buffer = 0x%p\n", SIMODULE,
            ptr_input_buffer);
    if (section_length == LENGTH_CRC_32)
    {
        *crc = ((*ptr_input_buffer) << 24 | (*(ptr_input_buffer + 1)) << 16
                | (*(ptr_input_buffer + 2)) << 8 | (*(ptr_input_buffer + 3)));
        MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
                "<%s::parseAndUpdateNTT> - CRC32 = %u\n", SIMODULE, *crc);
    }

    PARSING_DONE:

    if (ntt_error == NO_PARSING_ERROR)
    {
        MPE_LOG(MPE_LOG_TRACE3, MPE_MOD_SI,
                "<%s::parseAndUpdateNTT> - SVCT records parsed, set flag.\n",
                SIMODULE);
    }

    /*
     ** **************************************************************************
     ** * At this point, if section_length = 0, the CRC_32 is present and the table section has been
     ** * processed successfully. A section_length > 0 or section_length < 0  implies that an error has
     ** * occurred and ntt_error should identify the error condition.
     ** **************************************************************************
     */
    MPE_LOG(
            MPE_LOG_TRACE4,
            MPE_MOD_SI,
            "<%s::parseAndUpdateNTT> - Exiting with a NTT_Error_Code = (%d).\n",
            SIMODULE, ntt_error);
    return ntt_error;
}

//***************************************************************************************************
//  Subroutine/Method:                       parseSNSSubtable()
//***************************************************************************************************
/**
 * Parses SNS (Source Name Subtable) records, validates the fileds within a SNS record.
 *
 * @param  pptr_TableSection,  the address of memeory location which contains a pointer to the NIT
 *                             table section.
 * @param  ptr_section_length, a pointer to the caller's section_length variable.
 *
 * @param  language_code, the 3-character ISO 639 language code associated with this SNS
 *
 * @return MPE_SUCCESS if table section was successfully processed/parsed .Otherwise an error code
 *         greater than zero (MPE_SUCCESS).
 */
mpe_Error parseSNSSubtable(uint8_t **pptr_TableSection,
        int32_t *ptr_section_length, char* language_code)
{
    int i, descriptor_count;

    uint8_t number_of_SNS_records = 0;
    uint8_t segment[256] =
    { 0 };
    uint8_t mode = 0;
    uint8_t segment_length = 0;
    uint8_t name_length = 0;
    uint16_t source_ID = 0;
    mpe_Bool bIsAppType;
    sns_record_struc1 *ptr_sns_record_struc1 = NULL;
    sns_record_struc2 *ptr_sns_record_struc2 = NULL;
    generic_descriptor_struc *ptr_descriptor_struc = NULL;
    mpe_Error ntt_error = NO_PARSING_ERROR;

    //***********************************************************************************************
    //***********************************************************************************************
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::parseSNSSubtable> - Entered method parseSNSSubtable.\n",
            SIMODULE);

    /*
     ** first get the number of SNS records
     */
    ptr_sns_record_struc1 = (sns_record_struc1 *) *pptr_TableSection;

    number_of_SNS_records = ptr_sns_record_struc1->number_of_SNS_records_octet1;
    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
            "<%s::parseSNSSubtable> - number of sns records: %d\n", SIMODULE,
            number_of_SNS_records);

    /* increment the ptr in the table sectio to the next struct*/
    *pptr_TableSection += sizeof(sns_record_struc1);
    *ptr_section_length -= sizeof(sns_record_struc1);

    MPE_LOG(
            MPE_LOG_TRACE6,
            MPE_MOD_SI,
            "<%s::parseSNSSubtable> - section_length: %d, sizeof(sns_record_struc1): %d \n",
            SIMODULE, *ptr_section_length, sizeof(sns_record_struc1));

    for (i = 0; (i < number_of_SNS_records) && (ntt_error == NO_PARSING_ERROR); ++i)
    {
        //*******************************************************************************************
        // Extract and validate data from the specified sns records per table 5.12, pg. 22 in document
        // ANSI/SCTE 65-2002 (formerly DVS 234)
        //*******************************************************************************************

        // overlay the second sns structure
        ptr_sns_record_struc2 = (sns_record_struc2 *) *pptr_TableSection;
        bIsAppType = false;

        if (TESTBITS(ptr_sns_record_struc2->application_type_octet1, 8, 8))
        {
            bIsAppType = true;
            MPE_LOG(MPE_LOG_TRACE3, MPE_MOD_SI,
                    "<%s::parseSNSSubtable> - name is for app id \n", SIMODULE);
        }
        source_ID = (uint16_t)(((ptr_sns_record_struc2->source_ID_octet2) << 8)
                | ptr_sns_record_struc2->source_ID_octet3);
        MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                "<%s::parseSNSSubtable> - sourceId: 0x%x(%d).\n", SIMODULE,
                source_ID, source_ID);

        name_length = ptr_sns_record_struc2->name_length_octet4;

        /* Move the table section ptr to the beginning of the name */
        *pptr_TableSection += sizeof(sns_record_struc2);
        *ptr_section_length -= (sizeof(sns_record_struc2)
                + ptr_sns_record_struc2->name_length_octet4);
        MPE_LOG(
                MPE_LOG_TRACE6,
                MPE_MOD_SI,
                "<%s::parseSNSSubtable> - name length: %d, sectionLength: %d.\n",
                SIMODULE, name_length, *ptr_section_length);

        /* parse the multilingual text string NOTE: this method will advance the section ptr*/
        parseMTS(pptr_TableSection, name_length, &mode, &segment_length,
                segment);
        MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                "<%s::parseSNSSubtable> - MTS mode: 0x%x segment_length: %d, ",
                SIMODULE, mode, segment_length);
        mpe_siLockForWrite();
        if (segment_length != 0)
        {
            segment[segment_length] = '\0';

            MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                    "<%s::parseSNSSubtable> - id: 0x%x(%d), bIsAppType: %d name: %s \n",
                    SIMODULE, source_ID, source_ID, bIsAppType, (char*) segment);
            {
                mpe_siSourceNameEntry *entry = NULL;

                // see if this entry exists (ie don't create one if it doesn't
                mpe_siGetSourceNameEntry(source_ID, bIsAppType, &entry, FALSE);  // try to find w/o a create

                //
                // If entry (name) exists for this language, replace the name
                if (entry != NULL)
                {
                    mpe_siSetSourceName(entry, (char*)segment, language_code, TRUE);
                    mpe_siSetSourceLongName(entry, (char*)segment, language_code);
                }
                // Entry was not found, go create one
                else
                {
                    mpe_siGetSourceNameEntry(source_ID, bIsAppType, &entry, TRUE);   // create the entry

                    if (entry != NULL)
                    {
                        mpe_siSetSourceName(entry, (char*)segment, language_code, FALSE);
                        mpe_siSetSourceLongName(entry, (char*)segment, language_code);
                    }
                    else
                    {
                        ntt_error = 18;   // error detected, could not create entry
                    }
                }
            }
        }
        else
        {
            MPE_LOG(MPE_LOG_TRACE3, MPE_MOD_SI, "%s\n", segment);
        }

        mpe_siReleaseWriteLock();

        //****************************************************************************************************
        // Prepare to process the descriptors if they are present.
        //****************************************************************************************************

        descriptor_count = **pptr_TableSection; // get 8-bit descriptor count and advance pointer
        MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_SI,
                "<%s::parseSNSSubtable> - descriptor_count: %d\n", SIMODULE,
                descriptor_count);
        ++(*pptr_TableSection);
        *ptr_section_length -= 1;

        while (descriptor_count > 0)
        { // ignore all descriptors .

            ptr_descriptor_struc
                    = (generic_descriptor_struc *) *pptr_TableSection;
            *pptr_TableSection += sizeof(generic_descriptor_struc)
                    + ptr_descriptor_struc->descriptor_length_octet2;

            MPE_LOG(
                    MPE_LOG_TRACE6,
                    MPE_MOD_SI,
                    "<%s::parseSNSSubtable> - ptr: 0x%p, descriptor_tag: 0x%x, descriptor_length: %d\n",
                    SIMODULE, ptr_descriptor_struc,
                    ptr_descriptor_struc->descriptor_tag_octet1,
                    ptr_descriptor_struc->descriptor_length_octet2);

            //*** decrement section_length as we did not account for descriptor tag, length
            //*** and data fields length and data fields prior to calling this subroutine .

            --descriptor_count;
            *ptr_section_length -= (sizeof(generic_descriptor_struc)
                    + ptr_descriptor_struc->descriptor_length_octet2);

            if (*ptr_section_length < 0)
            { //error, section_length not large enough to account for descriptor tags, length and data fields
                ntt_error = 18;
                break;
            }
        } //end_of_while_loop
    } //end_of_for_loop


    //****************************************************************************************************
    //    Update the SI database with the frequencies.
    //****************************************************************************************************

    if (ntt_error == NO_PARSING_ERROR)
    {
        //call sidb here
    }
    MPE_LOG(MPE_LOG_TRACE4, MPE_MOD_SI,
            "<%s::parseSNSRecords> - Exiting with a NTT_Error_Code = %d.\n",
            SIMODULE, ntt_error);

    return ntt_error;
}

void parseMTS(uint8_t **ptr_TableSection, uint8_t name_lenth, uint8_t * mode,
        uint8_t * length, uint8_t * segment)
{
    mts_format_struc1 * ptr_mts_format_struc;
    int ii = 0;

    MPE_UNUSED_PARAM(name_lenth); /* TODO: if this param is not used, should it be removed from this function? */

    ptr_mts_format_struc = (mts_format_struc1 *) *ptr_TableSection;

    /* parse the mode and set the value */
    *mode = ptr_mts_format_struc->mode_octet1;

    /*
     ** If 'mode' is in the range 0x40 to 0x9F, then the 'length/segment portion
     ** is omitted. Formate effector codes in the range 0x40 to 0x9F involve no
     ** associated parametric data; hence the ommision of the 'length/segment portion.
     ** Format effector codes in the range 0xA0 through 0xFF include one or more
     ** parameter specific to the particular format effector function.
     **
     ** So set the length to 0 and return if the value is above the 0x40 range.
     */
    if (*mode >= 0x40)
    {
        *length = 0;
        return;
    }

    /* parse the length and set the value */
    *length = ptr_mts_format_struc->length_octet2;

    MPE_LOG(MPE_LOG_TRACE3, MPE_MOD_SI,
            "<%s::parseMTS> - mode: 0x%x length: 0x%x\n", SIMODULE, *mode,
            *length);
    /* recalc the ptr position past the mode and length fields */
    *ptr_TableSection += sizeof(mts_format_struc1);

    MPE_LOG(MPE_LOG_TRACE3, MPE_MOD_SI,
            "<%s::parseMTS> - segment_length: %d\n", SIMODULE, *length);
    /* Copy the name to the segment param */
    for (ii = 0; ii < *length; ii++)
    {
        segment[ii] = **ptr_TableSection;
        *ptr_TableSection = *ptr_TableSection + 1;
    }
}

/*
 ** Notify SIDB base on the versioning and crc values of the table acquired.
 **
 ** If the version is different then the current version stored in the SIDB, and the current version
 ** in the SIDB is the default version call table acquired.
 **
 ** If the version is different then the current version stored in the SIDB, and the current version
 ** is not the default version call table updated.
 **
 ** If the version is the same and the crc32 value is different, then call table updated.
 **
 ** If the version is the same and the crc32 is the same - then error - should parse a table that is
 ** already acquired.
 **
 */
void notifySIDB(mpe_SiTableType table_type,
        mpe_Bool init_version, mpe_Bool new_version, mpe_Bool new_crc,
        mpe_SiServiceHandle service_handle)
{
    MPE_LOG(MPE_LOG_TRACE1,
            MPE_MOD_SI,
            "<%s::notifySIDB> Enter - table_type: %d, init_version=%s, new_version=%s, new_crc=%s\n",
            PSIMODULE, table_type, (init_version == true ? "true" : "false"),
            (new_version == true ? "true" : "false"), (new_crc == true ? "true" : "false"));

    if (true == new_version)
    {
        if (true == init_version)
        {
            /*
             ** SIDB doesn't have this table, so tell sidb that the table has
             ** been acquired and set the version and crc.
             */
            MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
                    "<%s::notifySIDB> - call mpe_siNotifyTableChanged\n",
                    PSIMODULE);

            mpe_siNotifyTableChanged(table_type, MPE_SI_CHANGE_TYPE_ADD, service_handle);
        }
        else
        {
            /*
             ** SIDB doesn't have this table, so tell sidb that the table has
             ** been acquired and set the version and crc.
             */
            mpe_siNotifyTableChanged(table_type, MPE_SI_CHANGE_TYPE_MODIFY,
                    service_handle);
        }
    }
    else
    {
        /*
         ** Assert: Version has lapped itself
         */
        if (true == new_crc)
        {
            MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
                    "<%s::notifySIDB> - call mpe_siNotifyTableChanged\n",
                    PSIMODULE);
            mpe_siNotifyTableChanged(table_type, MPE_SI_CHANGE_TYPE_MODIFY,
                    service_handle);
        }
    }
}

uint32_t getSidbVersionNumber(mpe_SiTableType table_type,
        mpe_SiGenericHandle handle)
{
    mpe_SiTableType tt = table_type;
    uint32_t version = INIT_TABLE_VERSION;
    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::getSidbVersionNumber> - Enter tableType: %d, handle: 0x%x\n",
            PSIMODULE, table_type, handle);
    if (tt == OOB_PAT || tt == IB_PAT)
    {
        (void) mpe_siGetPATVersionForTransportStreamHandle(
                (mpe_SiTransportStreamHandle) handle, &version);
    }
    else
    {
        (void) mpe_siGetPMTVersionForProgramHandle(
                (mpe_SiProgramHandle) handle, &version);
    }

    return version;
}

uint32_t getSidbCRC(mpe_SiTableType table_type, mpe_SiGenericHandle handle)
{
    mpe_SiTableType tt = table_type;
    uint32_t crc32 = 0;
    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_SI,
            "<%s::getSidbCRC> - Enter tableType: %d, handle: 0x%x\n",
            PSIMODULE, table_type, handle);
    if (tt == OOB_PAT || tt == IB_PAT)
    {
        (void) mpe_siGetPATCRCForTransportStreamHandle(
                (mpe_SiTransportStreamHandle) handle, &crc32);
    }
    else
    {
        (void) mpe_siGetPMTCRCForProgramHandle((mpe_SiProgramHandle) handle,
                &crc32);
    }
    return crc32;
}
