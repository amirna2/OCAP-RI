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

#include "mpe_types.h"
#include "mpe_objectCarouselSectionParser.h"
#include "mpe_filter.h"
#include "mpe_os.h"
#include "mpe_dbg.h"
#include "mpe_socket.h"
#include "mpe_file.h"

#include <string.h>

#ifndef __GNUCC__
#define inline
#endif

#define OCP_MIN_MODULEINFOLENGTH            (21)                // The module info length must be at LEAST 21 bytes long to
// accomodate BIOP::ModuleInfo structure.

/**
 * Internal function, return the next 32 bit value in the section, translated
 * to native form.  NO ERROR CHECKING.
 */

inline
static uint32_t get_uint32_t(mpe_FilterSectionHandle section, uint32_t offset)
{
    uint32_t value;
    (void) mpe_filterSectionRead(section, offset, sizeof(uint32_t), 0,
            (unsigned char *) &value, NULL);

    return mpe_socketNtoHL(value);
}

/**
 * Internal function, return the next 16 bit value in the section, translated
 * to native form.  NO ERROR CHECKING.
 */
inline
static uint16_t get_uint16_t(mpe_FilterSectionHandle section, uint32_t offset)
{

    uint16_t value;
    (void) mpe_filterSectionRead(section, offset, sizeof(uint16_t), 0,
            (unsigned char *) &value, NULL);

    return mpe_socketNtoHS(value);
}

/**
 * Internal function, return the next 8 bit value in the section, translated
 * to native form.  NO ERROR CHECKING.
 */
inline
static uint8_t get_uint8_t(mpe_FilterSectionHandle section, uint32_t offset)
{
    uint8_t value;
    (void) mpe_filterSectionRead(section, offset, sizeof(uint8_t), 0, &value,
            NULL);
    return value;
}

/**
 * Internal function, check that the next 8-bit value is the expected value.
 */
inline
static mpe_Error checkValue8(mpe_FilterSectionHandle section, uint32_t offset,
        uint8_t value)
{
    uint8_t foundValue = get_uint8_t((section), offset);
    if (foundValue != (value))
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "Expected value %02x -- found %02x\n", (value), foundValue);
        return MPE_FS_ERROR_INVALID_DATA;
    }
    return MPE_SUCCESS;
}

/**
 * Internal function, check that the next 16-bit value is the expected value.
 */
inline
static mpe_Error checkValue16(mpe_FilterSectionHandle section, uint32_t offset,
        uint16_t value)
{
    uint16_t foundValue = get_uint16_t((section), offset);
    if (foundValue != (value))
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "Expected value %04x -- found %04x\n", (value), foundValue);
        return MPE_FS_ERROR_INVALID_DATA;
    }
    return MPE_SUCCESS;
}

/**
 * Free up a DII block, and all it's sub components.  No reference counting is
 * done, so be sure you want to deallocate it.
 *
 * @param dii The DII to free.
 */
void mpe_ocpFreeDII(mpe_OcpDII *dii)
{
    uint32_t i, j; // Counters

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OCP: Freeing DII\n");
    if (dii != NULL)
    {
        if (dii->modules != NULL)
        {
            for (i = 0; i < dii->numModules; i++)
            {
                if (dii->modules[i].hasTextDescriptors)
                {
                    for (j = 0; j < dii->modules[i].numDescriptors; j++)
                    {
                        switch (dii->modules[i].descriptors[j].descType)
                        {
                        case OCP_LABEL_DESCRIPTOR_TAG:
                            if (dii->modules[i].descriptors[j].desc.label.label
                                    != NULL)
                            {
                                mpeos_memFreeP(
                                        MPE_MEM_FILE_CAROUSEL,
                                        dii->modules[i].descriptors[j].desc.label.label);
                            }
                            break;
                        case OCP_NAME_DESCRIPTOR_TAG:
                            if (dii->modules[i].descriptors[j].desc.name.name
                                    != NULL)
                            {
                                mpeos_memFreeP(
                                        MPE_MEM_FILE_CAROUSEL,
                                        dii->modules[i].descriptors[j].desc.name.name);
                            }
                            break;
                        case OCP_INFO_DESCRIPTOR_TAG:
                            if (dii->modules[i].descriptors[j].desc.type.type
                                    != NULL)
                            {
                                mpeos_memFreeP(
                                        MPE_MEM_FILE_CAROUSEL,
                                        dii->modules[i].descriptors[j].desc.type.type);
                            }
                            break;
                        default:
                            break;
                        }
                    }
                }
            }
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "OCP: Freeing DII modules: %p\n", dii->modules);
            mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, dii->modules);
        }
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "OCP: Freeing actual DII: %p\n", dii);
        mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, dii);
    }
}

/**
 * Free up a DSI block, and all it's sub components.  No reference counting is
 * done, so be sure you want to deallocate it.
 *
 * @param dsi The DSI to free.
 */
void mpe_ocpFreeDSI(mpe_OcpDSI *dsi)
{

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OCP: Freeing DSI\n");
    if (dsi != NULL)
    {
        if (dsi->privateDataBytes != NULL)
        {
            mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, dsi->privateDataBytes);
        }
        mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, dsi);
    }
}

#define GET_UINT8(value, section, offset) \
    value = get_uint8_t(section, offset); offset += (sizeof(uint8_t));

#define GET_UINT16(value, section, offset) \
    value = get_uint16_t(section, offset); offset += (sizeof(uint16_t));

#define GET_UINT32(value, section, offset) \
    value = get_uint32_t(section, offset); offset += (sizeof(uint32_t));

#define CHECKVALUE8(section, value, offset) \
    if ((retCode = checkValue8(section, offset, value)) != MPE_SUCCESS) { goto CleanUp; } ; offset += (sizeof(uint8_t));

#define CHECKVALUE16(section, value, offset) \
    if ((retCode = checkValue16(section, offset, value)) != MPE_SUCCESS) { goto CleanUp; } ; offset += (sizeof(uint16_t));

/**
 * Parse the bytes in a dsmcc message header.
 * Common to  DSI's and DII's.
 * Defined in TR 101 202 Annex A (page 51)
 */
static mpe_Error ocpParseDsmccMessageHeader(mpe_FilterSectionHandle section,
        mpe_OcpDsmccMessageHeader *header, uint32_t *offset)
{
    mpe_Error retCode;

    // Begin parsing the record.
    // Check the table ID
    CHECKVALUE8(section, 0x3b, (*offset));

    // Skip the next several fields, up to start of the DSMCC DownloadHeader
    (*offset) += 7;

    CHECKVALUE16(section, 0x1103, (*offset));

    GET_UINT16(header->messageID, section, (*offset));
    GET_UINT32(header->transactionID, section, (*offset));

    // check that the reserved field is 0xff
    CHECKVALUE8(section, 0xff, (*offset));

    GET_UINT8(header->adaptationLength, section, (*offset));
    GET_UINT16(header->messageLength, section, (*offset));

    if (header->adaptationLength != 0)
    {
        (*offset) += header->adaptationLength;
    }
    // Done parsing the header

    // Grab the version number from the transactionID
    header->version = (header->transactionID & OCP_DSMCC_HEADER_VERSION_MASK)
            >> OCP_DSMCC_HEADER_VERSION_SHIFT;

    return MPE_SUCCESS;

    // Error handling
    CleanUp: return retCode;
}

// Descriptor parsing routines.
static
inline
void ocpParseModuleLinkDescriptor(mpe_FilterSectionHandle section,
        uint32_t descriptorLength, mpe_OcpModInfo *module, uint32_t *offset)
{
    uint32_t pos = module->numDescriptors;

    MPE_UNUSED_PARAM(descriptorLength); /* TODO: if this param is not used, should it be removed from this function? */

    GET_UINT8(module->descriptors[pos].desc.moduleLink.position, section, (*offset));
    GET_UINT16(module->descriptors[pos].desc.moduleLink.nextModule, section, (*offset));
    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_FILESYS,
            "OCP: Parsed module link descriptor: Position: %d: Next Module %04x\n",
            module->descriptors[pos].desc.moduleLink.position,
            module->descriptors[pos].desc.moduleLink.nextModule);
}

static
inline
void ocpParseCompressedModuleDescriptor(mpe_FilterSectionHandle section,
        uint32_t descriptorLength, mpe_OcpModInfo *module, uint32_t *offset)
{
    uint32_t pos = module->numDescriptors;

    MPE_UNUSED_PARAM(descriptorLength); /* TODO: if this param is not used, should it be removed from this function? */

    GET_UINT8(module->descriptors[pos].desc.compressedModule.compressionMethod, section, (*offset));
    GET_UINT32(module->descriptors[pos].desc.compressedModule.originalSize, section, (*offset));
    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_FILESYS,
            "OCP: Parsed compression descriptor: Method: %02x: Original Size: %d\n",
            module->descriptors[pos].desc.compressedModule.compressionMethod,
            module->descriptors[pos].desc.compressedModule.originalSize);
}

static
inline
void ocpParseCachingPriorityDescriptor(mpe_FilterSectionHandle section,
        uint32_t descriptorLength, mpe_OcpModInfo *module, uint32_t *offset)
{
    uint32_t pos = module->numDescriptors;

    MPE_UNUSED_PARAM(descriptorLength); /* TODO: if this param is not used, should it be removed from this function? */

    GET_UINT8(module->descriptors[pos].desc.cachingPriority.priorityValue, section, (*offset));
    GET_UINT8(module->descriptors[pos].desc.cachingPriority.transparencyLevel, section, (*offset));
    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_FILESYS,
            "OCP: Parsed caching priority descriptor: Method: %02x: Original Size: %d\n",
            module->descriptors[pos].desc.cachingPriority.priorityValue,
            module->descriptors[pos].desc.cachingPriority.transparencyLevel);
}

static inline mpe_Error ocpParseLabelDescriptor(
        mpe_FilterSectionHandle section, uint32_t descriptorLength,
        mpe_OcpModInfo *module, uint32_t *offset)
{
    mpe_Error retCode;
    uint32_t pos = module->numDescriptors;
    char *text = NULL;
    uint32_t bytesRead;

    module->hasTextDescriptors = true;

    // Allocate the buffer
    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, (descriptorLength + 1)
            * sizeof(char), (void **) &text);
    if (retCode != MPE_SUCCESS)
    {
        return retCode;
    }
    // Fill it.
    retCode = mpe_filterSectionRead(section, *offset, (int) descriptorLength,
            0, (unsigned char *) text, &bytesRead);
    if (retCode != MPE_SUCCESS || (bytesRead != descriptorLength))
    {
        mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, text);
        return MPE_ENODATA;
    }
    text[descriptorLength] = '\0';

    module->descriptors[pos].desc.label.label = text;

    *offset += descriptorLength;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "OCP: Parsed Label descriptor: Method: %s\n",
            module->descriptors[pos].desc.label.label);

    return MPE_SUCCESS;
}

/**
 *  Parse the descriptors which make up the
 */
static mpe_Error ocpParseUserDescriptors(mpe_FilterSectionHandle section,
        uint32_t length, mpe_OcpModInfo *module, uint32_t offset)
{
    mpe_Error retCode;
    uint8_t descriptorTag;
    uint8_t descriptorLength;

    // Clear out the number of descriptors
    module->numDescriptors = 0;
    module->hasTextDescriptors = false;

    // While we have descriptors left, parse them
    while (length > 0)
    {
        if (length < 2)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "OCP: Need at least 2 bytes for a descriptor\n");
            goto CleanUp;
        }
        // Get the tag and the length
        GET_UINT8(descriptorTag, section, offset);
        GET_UINT8(descriptorLength, section, offset);
        // decrement the length by the 2 bytes we just read
        length -= 2;
        if (module->numDescriptors >= OCP_MAX_MOD_DESCRIPTORS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "OCP: Too many descriptors.  Skipping: %02x: %d bytes\n",
                    descriptorTag, descriptorLength);
            offset += descriptorLength;
        }
        else
        {
            // Assign the tag into the descriptor;
            module->descriptors[module->numDescriptors].descType
                    = descriptorTag;
            switch (descriptorTag)
            {
            case OCP_MODULE_LINK_DESCRIPTOR_TAG:
                ocpParseModuleLinkDescriptor(section, descriptorLength, module,
                        &offset);
                module->numDescriptors++;
                break;
            case OCP_COMPRESSED_MODULE_DESCRIPTOR_TAG:
                ocpParseCompressedModuleDescriptor(section, descriptorLength,
                        module, &offset);
                module->numDescriptors++;
                break;
            case OCP_CACHING_PRIORITY_DESCRIPTOR_TAG:
                ocpParseCachingPriorityDescriptor(section, descriptorLength,
                        module, &offset);
                module->numDescriptors++;
                break;
            case OCP_LABEL_DESCRIPTOR_TAG:
                retCode = ocpParseLabelDescriptor(section, descriptorLength,
                        module, &offset);
                if (retCode != MPE_SUCCESS)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                            "OCP: Error parsing text descriptor\n");
                    goto CleanUp;
                }
                module->numDescriptors++;
                break;
            case OCP_NAME_DESCRIPTOR_TAG:
            case OCP_TYPE_DESCRIPTOR_TAG:
            case OCP_INFO_DESCRIPTOR_TAG:
            case OCP_CRC32_DESCRIPTOR_TAG:
            case OCP_LOCATION_DESCRIPTOR_TAG:
            case OCP_EST_DOWNLOAD_TIME_DESCRIPTOR_TAG:
            case OCP_GROUP_LINK_DESCRIPTOR_TAG:
                MPE_LOG(
                        MPE_LOG_DEBUG,
                        MPE_MOD_FILESYS,
                        "OCP: Skipping individual descriptor: %02x: %d bytes\n",
                        descriptorTag, descriptorLength);
                offset += descriptorLength;

                break;
            default:
                MPE_LOG(
                        MPE_LOG_WARN,
                        MPE_MOD_FILESYS,
                        "OCP: Unrecognized descriptor tag: %02x: %d bytes long\n",
                        descriptorTag, descriptorLength);
                offset += descriptorLength;
                break;
            }
        }
        length -= descriptorLength;
    }

    return MPE_SUCCESS;

    CleanUp: return MPE_FS_ERROR_INVALID_DATA;
}

/**
 * Parse DII out of a section.  Assumes the section read pointer is set to point
 * to the table ID field in the section.  Creates a mpe_OcpDII record and returns
 * it to the caller.  Does minimal error checking.  Assumes that the section you're
 * pointing to is a DII.
 *
 * @param section The section containing the DII.
 * @param retDII  [out] A Pointer to where to return the DII.
 *
 * @return MPE_SUCCESS if successful, error codes if the DII cannot be parsed,
 * or if memory cannot be allocated to hold the record.
 *
 */
mpe_Error mpe_ocpParseDII(mpe_FilterSectionHandle section, mpe_OcpDII **retDII)
{
    mpe_Error retCode = MPE_SUCCESS;
    mpe_OcpDII *dii;
    uint8_t compatibilityDescriptorLength;
    uint32_t i;
    uint8_t moduleInfoLength;
    uint8_t selectorLength;
    uint8_t userInfoLength;
    uint32_t offset = 0;
    uint8_t numTAPs;
    uint8_t j;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OCP: Parsing DII\n");

    // Set an error condition in the return, make it NULL
    *retDII = NULL;

    // Allocate space for the return structure.
    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, sizeof(mpe_OcpDII),
            (void **) &dii);
    if (retCode != MPE_SUCCESS)
    {
        return retCode;
    }
    // Clear it out
    memset(dii, 0, sizeof(mpe_OcpDII));

    retCode = ocpParseDsmccMessageHeader(section, &(dii->header), &offset);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCP: Could not parse DSMCC Header in DII\n");
        goto CleanUp;
    }

    GET_UINT32(dii->downloadID, section, offset);
    GET_UINT16(dii->blockSize, section, offset);
    GET_UINT8(dii->windowSize, section, offset);
    GET_UINT8(dii->ackPeriod, section, offset);
    GET_UINT32(dii->tCDownloadWindow, section, offset);
    GET_UINT32(dii->tCDownloadScenario, section, offset);

    // Skip the compatibility descriptor

    offset += 1;
    GET_UINT8(compatibilityDescriptorLength, section, offset);
    offset += compatibilityDescriptorLength;

    // Get the number of modules
    GET_UINT16(dii->numModules, section, offset);

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_FILESYS,
            "OCP: Got DII header: MessageID: %08x TransactionID %08x DownloadID %08x blockSize %5d Mods %d\n",
            dii->header.messageID, dii->header.transactionID, dii->downloadID,
            dii->blockSize, dii->numModules);

    // Create an array of the number of modules.
    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, dii->numModules
            * sizeof(mpe_OcpModInfo), (void **) &(dii->modules));
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCP: Could not allocate space for %d modules\n",
                dii->numModules);
        goto CleanUp;
    }
    // Initialize all modules in DII.
    memset(dii->modules, 0, dii->numModules * sizeof(mpe_OcpModInfo));

    for (i = 0; i < dii->numModules; i++)
    {
        GET_UINT16(dii->modules[i].moduleID, section, offset);
        GET_UINT32(dii->modules[i].moduleSize, section, offset);
        GET_UINT8(dii->modules[i].moduleVersion, section, offset);

        GET_UINT8(moduleInfoLength, section, offset);
        // Per MHP (TAM 232r31, B2.2.4, Page 270), there must be a BIOP::ModuleInfo structure in the
        // module Info Bytes field.
        // FIXME: TODO: What if we're not in an MHP object carosuel?  How do we determine which structure to
        // expect here?  How do we communicate this in?
        if (moduleInfoLength < OCP_MIN_MODULEINFOLENGTH)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_FILESYS,
                    "OCP: Module Info Length field for module %d (ID: %04x) is too short: %d\n",
                    i, dii->modules[i].moduleID, moduleInfoLength);
            retCode = MPE_FS_ERROR_INVALID_DATA;
            goto CleanUp;
        }

        // TODO: This is a BIOP object.  In the BIOP parser?  Probably not, but....
        GET_UINT32(dii->modules[i].moduleTimeout, section, offset);
        GET_UINT32(dii->modules[i].blockTimeout, section, offset);
        GET_UINT32(dii->modules[i].minBlockTime, section, offset);

        GET_UINT8(numTAPs, section, offset);
        if (numTAPs < 1)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "OCP: Num Taps in the BIOP::ModuleInfo structure must be at least one\n");
            retCode = MPE_FS_ERROR_INVALID_DATA;
            goto CleanUp;
        }

        // Start of the first TAP
        // HACK: TODO: Probably should have the BIOP parser handle this.  It's already got a TAP parser.
        offset += 2; // Skip the ID field
        CHECKVALUE16(section, OCP_BIOP_OBJECT_USE, offset); // Check that we're type BIOP_OBJECT_USE
        GET_UINT16(dii->modules[i].associationTag, section, offset);
        GET_UINT8(selectorLength, section, offset); // Get the selecetor length
        offset += selectorLength; // Skip it.
        // End of the TAP

        // Skip the remaining TAPs.
        for (j = 1; j < numTAPs; j++)
        {
            offset += 6; // Skip the ID, type, and association tag values.
            GET_UINT8(selectorLength, section, offset); // Get the selecetor length
            offset += selectorLength; // Skip it.
        }

        GET_UINT8(userInfoLength, section, offset); // Get the userInfoLength
        // Parse the descriptors
        retCode = ocpParseUserDescriptors(section, userInfoLength,
                &(dii->modules[i]), offset);

        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "OCP: Error parsing descriptors for module %04x\n",
                    dii->modules[i].moduleID);
        }
        offset += userInfoLength;

        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_FILESYS,
                "OCP: Parsed module %2d: ID: %04x Size: %6d Version: %2d Module TO: %x Block TO: %x MinBlockTime: %x Tag: %04x\n",
                i, dii->modules[i].moduleID, dii->modules[i].moduleSize,
                dii->modules[i].moduleVersion, dii->modules[i].moduleTimeout,
                dii->modules[i].blockTimeout, dii->modules[i].minBlockTime,
                dii->modules[i].associationTag);

    }

    // Ignore the private data section at the end.

    *retDII = dii;
    return retCode;

    // Error Cleanup Routine
    CleanUp: mpe_ocpFreeDII(dii);
    return retCode;
}

/**
 * Parse DSI out of a section.  Assumes the section read pointer is set to point
 * to the table ID field in the section.  Creates a mpe_ocpDSI record and returns
 * it to the caller.  Does minimal error checking.  Assumes that the section you're
 * pointing to is a DSI.
 *
 * @param section The section containing the DSI.
 * @param retDII  [out] A Pointer to where to return the DSI.
 *
 * @return MPE_SUCCESS if successful, error codes if the DII cannot be parsed,
 * or if memory cannot be allocated to hold the record.
 *
 */
mpe_Error mpe_ocpParseDSI(mpe_FilterSectionHandle section, mpe_OcpDSI **retDSI)
{

    mpe_Error retCode = MPE_SUCCESS;
    mpe_OcpDSI *dsi;
    uint32_t offset = 0;
    uint32_t bytesRead;

    // Set an error condition in the return, make it NULL
    *retDSI = NULL;

    // Allocate space for the return structure.
    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, sizeof(mpe_OcpDSI),
            (void **) &dsi);
    if (retCode != MPE_SUCCESS)
    {
        return retCode;
    }
    // Clear it out
    memset(dsi, 0, sizeof(mpe_OcpDSI));

    // Parse the section header
    retCode = ocpParseDsmccMessageHeader(section, &(dsi->header), &offset);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCP: Could not parse DSI DSMCC Header\n");
        mpe_ocpFreeDSI(dsi);
        return retCode;
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "OCP: Parsing header complete: Offset: %d\n", offset);

    retCode = mpe_filterSectionRead(section, offset, OCP_DSI_SERVERID_LENGTH,
            0, dsi->serverID, &bytesRead);
    if (retCode != MPE_SUCCESS || (bytesRead != OCP_DSI_SERVERID_LENGTH))
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCP: Could not parse DSI DSMCC Header\n");
        mpe_ocpFreeDSI(dsi);
        return retCode;
    }
    offset += OCP_DSI_SERVERID_LENGTH;

    // Get size of private data section
    GET_UINT32(dsi->numPrivateDataBytes, section, offset);

    // Allocate the private data section
    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, dsi->numPrivateDataBytes,
            (void **) &(dsi->privateDataBytes));
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCP: Cannot allocate %d private data bytes\n",
                dsi->numPrivateDataBytes);
        mpe_ocpFreeDSI(dsi);
        return retCode;
    }

    // And read it.
    retCode = mpe_filterSectionRead(section, offset, dsi->numPrivateDataBytes,
            0, dsi->privateDataBytes, &bytesRead);
    if (retCode != MPE_SUCCESS || (bytesRead != dsi->numPrivateDataBytes))
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCP: Could not parse DSI DSMCC Header\n");
        mpe_ocpFreeDSI(dsi);
        return retCode;
    }

    *retDSI = dsi;
    return retCode;
}

/**
 * Parse a section, making sure it contains a DDB, and return the section number and
 * where the data begins.
 * @param section The section which contains the DII.  Read pointer is assumed to point
 *        to the start of the section.
 * @param retDdbNumber  [out] A pointer to where to return the DDB number that this was.
 * @param retOffset     [out] A pointer to where to return the offset.
 *
 * @return MPE_SUCCESS if the block contains a valid DDB and the data offset is found,
 *         MPE_EINVAL otherwise.
 */
mpe_Error mpe_ocpParseDDBHeader(mpe_FilterSectionHandle section,
        uint32_t *retDdbNumber, uint32_t *retOffset)
{
    mpe_Error retCode = MPE_SUCCESS;
    uint8_t adaptationLength;
    uint16_t messageLength;
    uint8_t sectionNumber;
    uint16_t ddbNumber;
    uint32_t offset = 0;

    // Begin parsing the record.
    // Check the table ID
    CHECKVALUE8(section, 0x3c, offset);

    // Skip the next several fields, up to start of the DSMCC DownloadHeader
    offset += 5;

    GET_UINT8(sectionNumber, section, offset);
    (void) sectionNumber;

    // Skip the last section number
    offset += 1;

    CHECKVALUE16(section, 0x1103, offset);

    // Skip the MessageID and DownloadID fields
    offset += 6;

    // check that the reserved field is 0xff
    CHECKVALUE8(section, 0xff, offset);

    GET_UINT8(adaptationLength, section, offset);
    GET_UINT16(messageLength, section, offset);
    (void) messageLength;

    // Skip the adaptation header, module ID, version, and reserved bytes.
    offset += 4 + adaptationLength;

    // Make sure the block number is the same as the section number.
    // Note, this removes any high order bits in the section
    // Restriction per DVB
    GET_UINT16(ddbNumber, section, offset);
    (void) offset;

    // 26 is default
    // Section Header is 8 bytes.
    // DSMCC DDB Header is 18 bytes.
    *retOffset = OCP_DDB_HEADER_LENGTH + adaptationLength;
    *retDdbNumber = (uint32_t) ddbNumber;

    return retCode;

    CleanUp: MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
            "OCP: Unable to parse DDB\n");
    *retOffset = 0;
    *retDdbNumber = (uint32_t) - 1;
    return MPE_FS_ERROR_INVALID_DATA;
}
