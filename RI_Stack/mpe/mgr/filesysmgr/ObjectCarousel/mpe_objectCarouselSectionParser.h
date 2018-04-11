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

#ifndef _MPE_OBJECTCAROUSELPARSER_H_
#define _MPE_OBJECTCAROUSELPARSER_H_

#ifdef __cplusplus
extern "C"
{
#endif

#include "mpe_filter.h"

#define OCP_DSI_SERVERID_LENGTH                 (20)            // Number of bytes in the ServerID tag
#define OCP_LANGUAGE_CODE_LENGTH                (3)             // Number of bytes in the language code length
#define OCP_DDB_HEADER_LENGTH                   (26)            // Number of bytes in the header of a DDB
// Not counting the adaptation length

/*
 #define OCP_DII_MODULE_INFO_LENGTH              (21)            // HACK: FIXME: TODO: Assume the 
 // BIOP::ModuleInfo blocks are 21 bytes
 */

#define OCP_BIOP_OBJECT_USE                     (0x0017)

#define OCP_DSMCC_HEADER_VERSION_MASK           (0x3fff0000)
#define OCP_DSMCC_HEADER_VERSION_SHIFT          (16)

// Tags for the descriptors below.

#define OCP_TYPE_DESCRIPTOR_TAG                 (0x01)          // Per ETSI 301 192, page 20
#define OCP_NAME_DESCRIPTOR_TAG                 (0x02)          // Per ETSI 301 192, page 20
#define OCP_INFO_DESCRIPTOR_TAG                 (0x03)          // Per ETSI 301 192, page 20
#define OCP_MODULE_LINK_DESCRIPTOR_TAG          (0x04)          // Per ETSI 301 192, page 20
#define OCP_CRC32_DESCRIPTOR_TAG                (0x05)          // Per ETSI 301 192, page 20
#define OCP_LOCATION_DESCRIPTOR_TAG             (0x06)          // Per ETSI 301 192, page 20
#define OCP_EST_DOWNLOAD_TIME_DESCRIPTOR_TAG    (0x07)          // Per ETSI 301 192, page 20
#define OCP_GROUP_LINK_DESCRIPTOR_TAG           (0x08)          // Per ETSI 301 192, page 20
#define OCP_COMPRESSED_MODULE_DESCRIPTOR_TAG    (0x09)          // Per ETSI 301 192, page 20
#define OCP_LABEL_DESCRIPTOR_TAG                (0x70)          // Per TAM 232r31, page 271
#define OCP_CACHING_PRIORITY_DESCRIPTOR_TAG     (0x71)          // Per TAM 232r31, page 271
#define OCP_MODULE_POS_FIRST                    (0)
#define OCP_MODULE_POS_MIDDLE                   (1)
#define OCP_MODULE_POS_LAST                     (2)

#define OCP_CACHING_LEVEL_TRANSPARENT           (1)             // Per TAM 232r31, page 271
#define OCP_CACHING_LEVEL_SEMITRANSPARENT       (2)             // Per TAM 232r31, page 271
#define OCP_CACHING_LEVEL_STATIC                (3)             // Per TAM 232r31, page 271
#define OCP_MAX_MOD_DESCRIPTORS                 (16)            // Maximum number of descriptors we support
// Since there are only 8 types of descriptors
// specified in the SPEC, this should be fine, right?

// Per MHP, Section B.2.7, the compression method is signalled in the low
// nybble of the compression flag
#define OCP_COMPRESSION_NONE                    (0x00)
#define OCP_COMPRESSION_ZLIB                    (0x08)
#define OCP_COMPRESSION_MASK                    (0x0f)

// Various descriptors that can be in a DSI or DII, per
// ETSI EN 301 192, page 20-30ish.

typedef struct
{
    char *type;
} mpe_OcpTypeDescriptor;

typedef struct
{
    char *name;
} mpe_OcpNameDescriptor;

typedef struct
{
    uint8_t languageCode[OCP_LANGUAGE_CODE_LENGTH];
    char *name;
} mpe_OcpInfoDescriptor;

typedef struct
{
    uint32_t crc32;
} mpe_OcpCRC32Descriptor;

typedef struct
{
    uint8_t location;
} mpe_OcpLocationDescriptor;

typedef struct
{
    uint32_t estDownloadTime;
} mpe_OcpEstDnldTimeDescriptor;

typedef struct
{
    uint8_t position;
    uint32_t groupId;
} mpe_OcpGroupLinkDescriptor;

typedef struct
{
    uint8_t position;
    uint16_t nextModule;
} mpe_OcpModuleLinkDescriptor;

typedef struct
{
    uint8_t compressionMethod;
    uint32_t originalSize;
} mpe_OcpCompressedModuleDescriptor;

typedef struct
{
    uint8_t priorityValue;
    uint8_t transparencyLevel;
} mpe_OcpCachingPriorityDescriptor;

typedef struct
{
    char *label;
} mpe_OcpLabelDescriptor;

typedef union
{
    mpe_OcpModuleLinkDescriptor moduleLink;
    mpe_OcpCRC32Descriptor crc32;
    mpe_OcpInfoDescriptor info;
    mpe_OcpEstDnldTimeDescriptor downloadTime;
    mpe_OcpNameDescriptor name;
    mpe_OcpLabelDescriptor label;
    mpe_OcpTypeDescriptor type;
    mpe_OcpGroupLinkDescriptor groupLink;
    mpe_OcpCompressedModuleDescriptor compressedModule;
    mpe_OcpLocationDescriptor location;
    mpe_OcpCachingPriorityDescriptor cachingPriority;
} mpe_OcpDescriptorUnion;

typedef struct
{
    uint8_t descType;
    mpe_OcpDescriptorUnion desc;
} mpe_OcpDescriptor;

typedef struct
{
    uint16_t moduleID;
    uint32_t moduleSize;
    uint8_t moduleVersion;
    // The following information is from the BIOP::ModuleInfo structure, per IEC 13818-6, page 343
    uint32_t moduleTimeout;
    uint32_t blockTimeout;
    uint32_t minBlockTime;
    // The following information is from the first TAP structure.
    uint16_t associationTag;
    // Descriptors
    mpe_Bool hasTextDescriptors; // Are there any text descriptors which need to be deallocated?
    uint32_t numDescriptors;
    mpe_OcpDescriptor descriptors[OCP_MAX_MOD_DESCRIPTORS];
} mpe_OcpModInfo;

typedef struct
{
    uint16_t messageID;
    uint32_t transactionID;
    uint8_t adaptationLength;
    uint16_t messageLength;
    uint32_t version;
} mpe_OcpDsmccMessageHeader;

typedef struct
{
    mpe_OcpDsmccMessageHeader header;
    uint32_t downloadID;
    uint16_t blockSize;
    uint8_t windowSize;
    uint8_t ackPeriod;
    uint32_t tCDownloadWindow;
    uint32_t tCDownloadScenario;
    uint16_t numModules;
    mpe_OcpModInfo *modules;
} mpe_OcpDII;

typedef struct
{
    mpe_OcpDsmccMessageHeader header;
    uint8_t serverID[OCP_DSI_SERVERID_LENGTH];
    uint32_t numPrivateDataBytes;
    uint8_t *privateDataBytes;
} mpe_OcpDSI;

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
extern mpe_Error
mpe_ocpParseDII(mpe_FilterSectionHandle section, mpe_OcpDII **retDII);
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
extern mpe_Error
mpe_ocpParseDSI(mpe_FilterSectionHandle section, mpe_OcpDSI **retDSI);

/**
 * Free up a DII block, and all it's sub components.  No reference counting is
 * done, so be sure you want to deallocate it.
 * 
 * @param dii The DII to free.
 */
extern
void
mpe_ocpFreeDII(mpe_OcpDII *dii);

/**
 * Free up a DSI block, and all it's sub components.  No reference counting is
 * done, so be sure you want to deallocate it.
 * 
 * @param dsi The DSI to free.
 */
extern
void
mpe_ocpFreeDSI(mpe_OcpDSI *dsi);

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
extern mpe_Error
mpe_ocpParseDDBHeader(mpe_FilterSectionHandle section, uint32_t *retDdbNumber,
        uint32_t *retOffset);
#ifdef __cplusplus
}
#endif

#endif
