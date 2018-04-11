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

#include <mpe_os.h>
#include <mpe_dbg.h>
#include <mpe_types.h>
#include <mpe_pod.h>
#include <mpe_types.h>
#include <mpe_pod.h>
#include <mpeos_mem.h>
#include <mpeos_pod.h>
#include <mpeos_dbg.h>
#include <stdlib.h>
#include <string.h>
#include <simgr.h>
#include <podmgr.h>
#include "pod_util.h"

static mpe_Error sizeAndPackProgramCADescriptors(
        mpe_SiServiceHandle service_handle, uint16_t ca_system_id, uint16_t *program_info_length,
        uint8_t **program_info)
{
    mpe_SiMpeg2DescriptorList *descriptor_entry = NULL;
    mpe_SiMpeg2DescriptorList *saved_descriptor_entry;
    uint32_t num_descriptors = 0;
    uint32_t overall_size = 0;
    uint8_t *workingPtr = NULL;
    uint32_t i;
    mpe_Error err;

    // grab a descriptor set
    err = mpe_siGetOuterDescriptorsForServiceHandle(service_handle,
            &num_descriptors, &descriptor_entry);
    if (err != MPE_SI_SUCCESS)
    {
        goto sizeAndPackPGM_failure;
    }

    // preserve the top-level descriptor entry for use again later
    saved_descriptor_entry = descriptor_entry;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_POD,
            "sizeAndPackProgramCADescriptors: %d program descriptors detected.\n",
            num_descriptors);

    // first walk through the list and determine overall size
    for (i = 0; i < num_descriptors; i++)
    {
        // an unexpected descriptor entry ?
        if (descriptor_entry == NULL)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "sizeAndPackProgramCADescriptors: Unexpected null descriptor_entry !\n");
            break;
        }

        if (descriptor_entry->tag == MPE_SI_DESC_CA_DESCRIPTOR
                && descriptor_entry->descriptor_length > 0)
        {
            uint16_t cas_id = ((uint8_t *) descriptor_entry->descriptor_content)[0] << 8
                               | ((uint8_t *) descriptor_entry->descriptor_content)[1];
            // Match the CA system id
            if(cas_id == ca_system_id)
            {
                overall_size += sizeof(uint8_t); // byte for tag
                overall_size += sizeof(uint8_t); // byte for descriptor content length
                overall_size += descriptor_entry->descriptor_length; // descriptor content
            }
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
                    "    Found descriptor tag %d (len %d) system_id: %d\n",
                    descriptor_entry->tag, descriptor_entry->descriptor_length, cas_id);

        } // END if (CA descriptor)

        descriptor_entry = descriptor_entry->next;
    } // END for (outer descriptors)

    if (overall_size > 0)
    {
        if (overall_size >= 4096)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_POD,
                    "sizeAndPackProgramCADescriptors: program_info_length (%d) too large for 12-bit APDU field\n",
                    overall_size);

            err = MPE_ENOMEM;
            goto sizeAndPackPGM_failure;
        }

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
                "    allocating storage for program-level descriptors\n");

        // Allocate the space required to hold all of the program-level descriptors
        err
                = mpeos_memAllocP(MPE_MEM_POD, overall_size,
                        (void **) program_info);
        if (err != MPE_SI_SUCCESS)
        {
            // log error
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "sizeAndPackProgramCADescriptors: memAlloc failed to allocated program_info!\n");

            goto sizeAndPackPGM_failure;
        }

        // make a working pointer to walk the program info
        workingPtr = *program_info;

        // restore the descriptor entry pointer from before we calculated sizes
        descriptor_entry = saved_descriptor_entry;

        // walk through the descriptor list and pack up the contents
        for (i = 0; i < num_descriptors; i++)
        {
            if (descriptor_entry == NULL)
            {
                MPE_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_POD,
                        "sizeAndPackProgramCADescriptors: Unexpected null descriptor_entry [%d]! (again)\n",
                        i);
                break;
            }

            if (descriptor_entry->tag == MPE_SI_DESC_CA_DESCRIPTOR)
            {
                uint16_t cas_id = ((uint8_t *) descriptor_entry->descriptor_content)[0] << 8
                                   | ((uint8_t *) descriptor_entry->descriptor_content)[1];
                // Match the CA system id
                if(cas_id == ca_system_id)
                {
                    *workingPtr++ = descriptor_entry->tag;
                    *workingPtr++ = descriptor_entry->descriptor_length;

                    memcpy(workingPtr, descriptor_entry->descriptor_content,
                            descriptor_entry->descriptor_length);

                    MPE_LOG(
                            MPE_LOG_DEBUG,
                            MPE_MOD_POD,
                            "    [%d] CA Tag: %d, CA Desc len: %d bytes, CA PID: 0x%x cas_id:%d\n",
                            i, descriptor_entry->tag,
                            descriptor_entry->descriptor_length + 2, *workingPtr, cas_id);

                    workingPtr += descriptor_entry->descriptor_length;
                }
            } // END if (CA descriptor)

            descriptor_entry = descriptor_entry->next;
        } // END for (outer descriptors)
    }

    *program_info_length = overall_size;

    return 0;

    sizeAndPackPGM_failure:
    // clean up and then return
    if (*program_info != NULL)
    {
        mpeos_memFreeP(MPE_MEM_POD, *program_info);
        *program_info = NULL;
    }

    *program_info_length = 0;

    return err;
}

static mpe_Error sizeAndPackStreamCADescriptors(
        mpe_SiServiceHandle service_handle, uint8_t ca_pmt_cmd_id, uint16_t ca_system_id,
        uint16_t *stream_info_length, uint8_t **stream_info)
{
    // We'll handle up to 512 components
    mpe_SiServiceComponentHandle comp_handle[512];
    uint16_t comp_es_length[512];
    uint32_t component_count = sizeof(comp_handle);

    mpe_SiMpeg2DescriptorList *descriptor_entry = NULL;
    uint32_t num_descriptors = 0;

    uint32_t overall_size = 0;
    uint8_t *workingPtr = NULL;

    uint32_t i, j;
    mpe_Error err;
    mpe_SiElemStreamType stream_type;
    uint32_t pid;

    err = mpe_siGetAllComponentsForServiceHandle(service_handle, comp_handle,
            &component_count);
    if (err != MPE_SI_SUCCESS)
    {
        // log error
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "sizeAndPackStreamCADescriptor: failed to obtain SI Components\n");
        goto sizeAndPackSTRM_failure;
    }

    if (component_count > 512)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_POD,
                "sizeAndPackStreamCADescriptor: more ES components than the 512 supported (%d)\n",
                component_count);

        goto sizeAndPackSTRM_failure;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
            "sizeAndPackStreamCADescriptor: %d ES components detected.\n",
            component_count);

    // initialize our storage to zero
    memset(comp_es_length, 0, sizeof(comp_es_length));

    // Loop through all the components and compute the storage required to hold everything in one blob
    for (i = 0; i < component_count; i++)
    {
        // stream_type(1 byte) + elementary_pid(2 bytes) + ES_info_length(2 bytes)
        overall_size += 5;

        // now see if there is any actual ES_INFO for this component
        err = mpe_siGetDescriptorsForServiceComponentHandle(comp_handle[i],
                &num_descriptors, &descriptor_entry);
        if (err != MPE_SI_SUCCESS)
        {
            // log error
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_POD,
                    "sizeAndPackStreamCADescriptor: failed to obtain descriptor set for component handle\n");
            goto sizeAndPackSTRM_failure;
        }

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
                "    ES Comp [%d] has %d descriptor(s).\n", i, num_descriptors);

        for (j = 0; j < num_descriptors; j++)
        {
            if (descriptor_entry == NULL)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                        "sizeAndPackStreamCADescriptor: unexpectecd null descriptor entry\n");
                // log error
                break;
            }

            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
                    "    Descriptor tag: %d, descriptor length: %d.\n",
                    descriptor_entry->tag, descriptor_entry->descriptor_length);

            if (descriptor_entry->tag == MPE_SI_DESC_CA_DESCRIPTOR
                    && descriptor_entry->descriptor_length > 0)
            {
                uint16_t cas_id = ((uint8_t *) descriptor_entry->descriptor_content)[0] << 8
                                   | ((uint8_t *) descriptor_entry->descriptor_content)[1];
                // Match the CA system id
                if(cas_id == ca_system_id)
                {
                    /* 1 byte for the descriptor tag, 1 byte for the length field
                      * plus the rest of the descriptor
                      */
                    comp_es_length[i] += 1 + 1
                            + descriptor_entry->descriptor_length;
                }
            }
            descriptor_entry = descriptor_entry->next;
        }

        // 1 byte for the ca_pmt_cmd_id
        comp_es_length[i] += 1;

        overall_size += comp_es_length[i];
    }

    // Changes to correctly generate the ca_pmt APDU. The
    // The elementary stream loop information will be included in the ca_pmt APDU even
    // if there are no CA descriptors defined.
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_POD,
                "sizeAndPackStreamCADescriptor: allocating storage for stream info\n");

        // allocate storage based on what was calculated as the overall size required
        err = mpeos_memAllocP(MPE_MEM_POD, overall_size, (void **) stream_info);

        workingPtr = *stream_info;

        // start filling it in
        for (i = 0; i < component_count; i++)
        {
            err = mpe_siGetStreamTypeForServiceComponentHandle(comp_handle[i],
                    &stream_type);
            if (err != MPE_SI_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                        "sizeAndPackStreamCADescriptor: failed to obtain stream type\n");
                goto sizeAndPackSTRM_failure;
            }

            err = mpe_siGetPidForServiceComponentHandle(comp_handle[i], &pid);
            if (err != MPE_SI_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                        "sizeAndPackStreamCADescriptor: failed to obtain PID\n");
                goto sizeAndPackSTRM_failure;
            }

            *workingPtr++ = (uint8_t) stream_type;
            *workingPtr++ = (pid >> 8) & 0x1F; // Top 3 bits are reserved
            *workingPtr++ = pid & 0xFF;
            *workingPtr++ = (comp_es_length[i] >> 8) & 0xF; // top 4 bits are reserved
            *workingPtr++ = comp_es_length[i] & 0xFF;

            //
            // Loop through all the descriptors on the component
            //

            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_POD,
                    "     ES Comp [%d], Stream type: 0x%x(%d), ES_info_length: %d,  ES PID: 0x%x(%d)\n",
                    i, stream_type, stream_type, comp_es_length[i], pid, pid);

            if (comp_es_length[i] > 0)
            {
                err = mpe_siGetDescriptorsForServiceComponentHandle(
                        comp_handle[i], &num_descriptors, &descriptor_entry);
                if (err != MPE_SI_SUCCESS)
                {
                    MPE_LOG(
                            MPE_LOG_ERROR,
                            MPE_MOD_POD,
                            "sizeAndPackStreamCADescriptor: failed to obtain descriptor set for component handle\n");
                    goto sizeAndPackSTRM_failure;
                }

                MPE_LOG(
                        MPE_LOG_DEBUG,
                        MPE_MOD_POD,
                        "sizeAndPackStreamCADescriptor: ES Comp [%d] has %d CA Descriptors.\n",
                        i, num_descriptors);

                *workingPtr++ = ca_pmt_cmd_id;

                for (j = 0; j < num_descriptors; j++)
                {
                    if (descriptor_entry == NULL)
                    {
                        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                                "podmgrCreateCAPMT_APDU: Unexpected null descriptor_entry!\n");
                        break;
                    }

                    if (descriptor_entry->tag == MPE_SI_DESC_CA_DESCRIPTOR
                            && descriptor_entry->descriptor_length > 0)
                    {
                        uint16_t cas_id = ((uint8_t *) descriptor_entry->descriptor_content)[0] << 8
                                           | ((uint8_t *) descriptor_entry->descriptor_content)[1];

                        // Match the CA system id
                        if(cas_id == ca_system_id)
                        {
                            *workingPtr++ = descriptor_entry->tag;
                            *workingPtr++ = descriptor_entry->descriptor_length;

                            uint16_t
                                    cas_pid =
                                            (((uint8_t *) descriptor_entry->descriptor_content)[2])
                                                    << 8
                                                    | ((uint8_t *) descriptor_entry->descriptor_content)[3];

                            MPE_LOG(
                                    MPE_LOG_DEBUG,
                                    MPE_MOD_POD,
                                    "CA SYSTEM ID: 0x%x(%d), CA PID: 0x%x(%d) PAYLOAD: %d bytes, total CA descriptor size: %d bytes.\n",
                                    cas_id, cas_id, cas_pid, cas_pid,
                                    descriptor_entry->descriptor_length - 4,
                                    descriptor_entry->descriptor_length);

                            *workingPtr++
                                    = ((uint8_t *) descriptor_entry->descriptor_content)[0];
                            *workingPtr++
                                    = ((uint8_t *) descriptor_entry->descriptor_content)[1];
                            *workingPtr++
                                    = ((uint8_t *) descriptor_entry->descriptor_content)[2];
                            *workingPtr++
                                    = ((uint8_t *) descriptor_entry->descriptor_content)[3];

                            memcpy(
                                    workingPtr,
                                    &((uint8_t *) descriptor_entry->descriptor_content)[4],
                                    descriptor_entry->descriptor_length - 4);

                            workingPtr += descriptor_entry->descriptor_length - 4;

                        }
                    } // END if (CA descriptor)

                    descriptor_entry = descriptor_entry->next;

                } // END for (inner descriptors)
            }
        }

        *stream_info_length = overall_size;
    }

    return MPE_SUCCESS;

    sizeAndPackSTRM_failure: if (*stream_info != NULL)
    {
        mpeos_memFreeP(MPE_MEM_POD, *stream_info);
        *stream_info = NULL;
    }

    *stream_info_length = 0;

    return err;
}

static uint8_t *assemble_CA_PMT_APDU(uint32_t *apdu_size,
        uint8_t program_index, uint8_t transaction_id, uint8_t ltsid,
        uint32_t program_number, uint32_t source_id, uint8_t ca_pmt_cmd_id,
        uint16_t ca_system_id, uint16_t program_info_length, uint8_t *program_info,
        uint16_t stream_info_length, uint8_t *stream_info)
{
#define PUT_NEXT_BYTE( xx_byteValue ) apdu_buffer[index++] = (xx_byteValue)

    uint8_t *apdu_buffer = NULL;
    uint32_t payload = 0;
    uint32_t index = 0;

    if (*apdu_size < 127)
    {
        /* we can handle up to 127, so we need to insure that there is
         * room for the single byte storing the length
         */

        *apdu_size += 1;
    }
    else
    {
        /* adjusted to handle the 3 bytes required to convey the length of
         * this message (using ASN.1)
         */
        *apdu_size += 3;
    }

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_POD,
            "assemble_CA_PMT_APDU: allocating storage for actual APDU (%d bytes)\n",
            *apdu_size);

    (void) mpeos_memAllocP(MPE_MEM_POD, *apdu_size, (void **) &apdu_buffer);
    if (apdu_buffer == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "assemble_CA_PMT_APDU: failed to allocate memory.\n");
        return NULL;
    }

    // ca_pmt_tag
    PUT_NEXT_BYTE( (CA_PMT_TAG >> 16) & 0xFF );
    PUT_NEXT_BYTE( (CA_PMT_TAG >> 8) & 0xFF );
    PUT_NEXT_BYTE( CA_PMT_TAG & 0xFF );

    // handle APDU size appropriately
    if (*apdu_size <= 127)
    {
        payload = *apdu_size - 4;
        PUT_NEXT_BYTE( payload );
    }
    else
    {
        PUT_NEXT_BYTE( 0x82 ); // size indicator == 1 (0x80) and length value == 2

        payload = *apdu_size - 6;

        PUT_NEXT_BYTE( payload >> 8 ); // high-order byte
        PUT_NEXT_BYTE( payload & 0xFF ); // low-order byte
    }

    PUT_NEXT_BYTE( program_index );

    // transaction_id
    PUT_NEXT_BYTE( transaction_id );

    // ltsid
    PUT_NEXT_BYTE( ltsid );

    PUT_NEXT_BYTE( program_number >> 8 );
    PUT_NEXT_BYTE( program_number & 0xFF );

    PUT_NEXT_BYTE( source_id >> 8 );
    PUT_NEXT_BYTE( source_id & 0xFF );

    // ca_pmt_cmd_id
    PUT_NEXT_BYTE( ca_pmt_cmd_id );

    // program_info_length
    PUT_NEXT_BYTE( (program_info_length >> 8) & 0xF); // top 4 bits are reserved
    PUT_NEXT_BYTE( program_info_length & 0xFF);

    if ((program_info_length & 0xFFF) > 0)
    {
        memcpy(&apdu_buffer[index], program_info, program_info_length);
        index += program_info_length;
    }

    // now add the stream information
    if (stream_info_length > 0)
    {
        memcpy(&apdu_buffer[index], stream_info, stream_info_length);
        index += stream_info_length;
    }

    return apdu_buffer;
}

/**
 * This function will create a CA_PMT APDU as defined in CCIF-2.0-I18 for the given service
 * with key values provided by the caller.
 *
 * @param service_handle: A read-locked SIDB service handle
 *
 * @param session:
 *
 * @param apdu_buffer:    A pointer to pointer to contain the address of an allocated memory buffer allocated by this method.
 *                        It is the callers responsibility to free with mpeos_memFreeP().
 *
 * @param apdu_size:      A pointer to an uint32_t varible to contain the size of apdu_buffer, in bytes.
 *                        Upon return, this function will write the number of bytes in the
 *                        generated APDU to this location.
 *
 * @return MPE_SUCCESS if a CA_PMT APDU is created, MPE_ENODATA if no CA descriptors were found
 *         in the service, and a type-specific error code otherwise.
 */

mpe_Error podmgrCreateCAPMT_APDU(mpe_SiServiceHandle service_handle,
        uint8_t programIdx, uint8_t transactionId, uint8_t ltsid, uint16_t ca_system_id,
        uint8_t ca_pmt_cmd_id, uint8_t **apdu_buffer, uint32_t *apdu_buffer_size)
{
    uint8_t *program_info = NULL;
    uint8_t *stream_info = NULL;

    uint16_t program_info_length;
    uint16_t stream_info_length;

    uint32_t program_number;
    uint32_t sourceId;

    mpe_Error err;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_POD,
            "podmgrCreateCAPMT_APDU(service 0x%08x, pgm_index %d, trans %d, ltsid %d, ca_pmt_cmd_id %d) - Enter...\n",
            service_handle, programIdx, transactionId,
            ltsid, ca_pmt_cmd_id);

    // program_number
    err = mpe_siGetProgramNumberForServiceHandle(service_handle, &program_number);
    if (err != MPE_SI_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "podmgrCreateCAPMT_APDU: failed to obtain program_number for service handle\n");
        err = MPE_ENOMEM;
        goto FAILED_CA_PMT_CREATION;
    }

    // sourceId
    err = mpe_siGetSourceIdForServiceHandle(service_handle, &sourceId);
     if (err != MPE_SI_SUCCESS)
     {
         MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                 "podmgrCreateCAPMT_APDU: failed to obtain sourceId for service handle\n");
         err = MPE_ENOMEM;
         goto FAILED_CA_PMT_CREATION;
     }

    err = sizeAndPackProgramCADescriptors(service_handle, ca_system_id, &program_info_length,
            &program_info);
    if (err != MPE_SI_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "podmgrCreateCAPMT_APDU: failed to sizeAndPackProgramCADescriptors\n");
        goto FAILED_CA_PMT_CREATION;
    }

    err = sizeAndPackStreamCADescriptors(service_handle,
            ca_pmt_cmd_id, ca_system_id, &stream_info_length, &stream_info);
    if (err != MPE_SI_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                "podmgrCreateCAPMT_APDU: failed to sizeAndPackStreamCADescriptors\n");
        goto FAILED_CA_PMT_CREATION;
    }

    if (stream_info_length == 0 && program_info_length == 0)
    {
        err = MPE_ENODATA;
        goto FAILED_CA_PMT_CREATION;
    }

    /*
     * The APDU size is computed from a 3 byte TAG, followed by a 1 or 3 byte
     * length (computed by the called method below) plus 3 bytes for program_index,
     * transaction_id, and ltsid respectively plus 2 bytes for program_number plus
     * 2 bytes for source_id plus 1 byte for the CA_PMT_Command_id plus
     * program_info_length plus program info plus stream_info.
     */

    *apdu_buffer_size = 3 + 3 + 2 + 2 + 1 + 2 + program_info_length
            + stream_info_length;

    *apdu_buffer = assemble_CA_PMT_APDU(apdu_buffer_size, programIdx,
            transactionId, ltsid, program_number,
            sourceId, ca_pmt_cmd_id, ca_system_id, program_info_length,
            program_info, stream_info_length, stream_info);

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_POD,
            "podmgrCreateCAPMT_APDU: Successfully created %d byte CA_PMT APDU\n",
            *apdu_buffer_size);

    if (program_info != NULL)
    {
        mpeos_memFreeP(MPE_MEM_POD, program_info);
        program_info = NULL;
    }

    if (stream_info != NULL)
    {
        mpeos_memFreeP(MPE_MEM_POD, stream_info);
        stream_info = NULL;
    }

    return MPE_SUCCESS;

    //
    // Error recovery/cleanup
    //

    FAILED_CA_PMT_CREATION:

    // Assert: err set by the caller

    if (program_info != NULL)
    {
        mpeos_memFreeP(MPE_MEM_POD, program_info);
        program_info = NULL;
    }

    if (stream_info != NULL)
    {
        mpeos_memFreeP(MPE_MEM_POD, stream_info);
        stream_info = NULL;
    }

    return err;

} // END podmgrCreateCAPMT_APDU()
