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

#include "mpe_dataCarousel.h"
#include "mpe_objectCarouselBIOPParser.h"
#include "mpe_types.h"
#include "mpe_dbg.h"
#include "mpe_file.h"
#include "mpe_socket.h"
#include "mpe_os.h"

#include <string.h>

#define READER_FUNCPTR mpe_Error (*)(void *, uint32_t, uint32_t, uint32_t, uint8_t *, uint32_t *)

// If not on GCC (ie, PowerTV at present), make Inlines go away

#ifndef __GNUC__
#define inline
#endif

// #define TRACE_PARSE

// Forward declarations
void ocpFreeDirEntries(uint32_t, ocpDirEntry *);

// Internal variables

static mpe_Mutex ocpGlobalMutex = NULL;

// Static variable, so we can throw away values we just don't care about.
static uint32_t sink;

inline
static mpe_Error get_uint32_t(ocpDataSource *data, uint32_t offset,
        uint32_t *dest)
{
    uint32_t value;
    mpe_Error retCode;
    retCode = data->reader(data->dataSource, offset, sizeof(uint32_t),
            data->timeout, (uint8_t *) &value, &sink);
    *dest = mpe_socketNtoHL(value);
    return retCode;
}

inline
static mpe_Error get_uint16_t(ocpDataSource *data, uint32_t offset,
        uint16_t *dest)
{
    uint16_t value;
    mpe_Error retCode;
    retCode = data->reader(data->dataSource, offset, sizeof(uint16_t),
            data->timeout, (uint8_t *) &value, &sink);
    *dest = ntohs(value);
    return retCode;
}

inline
static mpe_Error get_uint8_t(ocpDataSource *data, uint32_t offset,
        uint8_t *dest)
{
    return data->reader(data->dataSource, offset, sizeof(uint8_t),
            data->timeout, dest, &sink);
}

/**
 * Internal function, check that the next 8-bit value is the expected value.
 */
inline
static mpe_Error checkValue8(ocpDataSource *source, uint8_t value,
        uint32_t offset)
{
    mpe_Error retCode;
    uint8_t foundValue;

    retCode = get_uint8_t(source, offset, &foundValue);
    if (retCode != MPE_SUCCESS)
    {
        return retCode;
    }
    else if (foundValue != value)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "BIOP: Expected value %02x at offset %d -- found %02x\n",
                (uint32_t) value, offset, foundValue);
        return MPE_FS_ERROR_INVALID_DATA;
    }
    return MPE_SUCCESS;
}

#if 0 /* TODO: this function is not used - should it be removed? */
/**
 * Internal function, check that the next 16-bit value is the expected value.
 */
inline
static
mpe_Error
checkValue16(ocpDataSource *source, uint16_t value, uint32_t offset)
{
    mpe_Error retCode;
    uint16_t foundValue;

    retCode = get_uint16_t(source, offset, &foundValue);
    if (retCode != MPE_SUCCESS)
    {
        return retCode;
    }
    else if (foundValue != value)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS, "BIOP: Expected value %04x at offset %d -- found %04x\n",
                (uint32_t) value, offset, foundValue);
        return MPE_FS_ERROR_INVALID_DATA;
    }
    return MPE_SUCCESS;
}
#endif /* unused function */

/**
 * Internal function, check that the next 32-bit value is the expected value.
 */
inline
static mpe_Error checkValue32(ocpDataSource *source, uint32_t value,
        uint32_t offset)
{
    mpe_Error retCode;
    uint32_t foundValue;

    retCode = get_uint32_t(source, offset, &foundValue);
    if (retCode != MPE_SUCCESS)
    {
        return retCode;
    }
    else if (foundValue != value)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "BIOP: Expected value %08x at offset %d -- found %08x\n",
                (uint32_t) value, offset, foundValue);
        return MPE_FS_ERROR_INVALID_DATA;
    }
    return MPE_SUCCESS;
}

// Macro's to use the get_uint functions and check the results

#define CHECK_RESULT(result)    if ((result) != MPE_SUCCESS) { goto CleanUp; }

#define GET_UINT32(module, offset, dest) \
    CHECK_RESULT(retCode = get_uint32_t(module, offset, &dest));  offset += sizeof(uint32_t);

#define GET_UINT16(module, offset, dest) \
    CHECK_RESULT(retCode = get_uint16_t(module, offset, &dest));  offset += sizeof(uint16_t);

#define GET_UINT8(module, offset, dest) \
    CHECK_RESULT(retCode = get_uint8_t(module, offset, &dest));   offset += sizeof(uint8_t);

// Macros to wrap around the checkValue functions above.  Move the offset as well
// as checking the value.
#define CHECK_UINT8(source, value, offset) \
    CHECK_RESULT(retCode = checkValue8(source, value, offset)); \
    offset += sizeof(uint8_t);

#define CHECK_UINT16(source, value, offset) \
    CHECK_RESULT(retCode = checkValue16(source, value, offset)); \
    offset += sizeof(uint16_t);

#define CHECK_UINT32(source, value, offset) \
    CHECK_RESULT(retCode = checkValue32(source, value, offset)); \
    offset += sizeof(uint32_t);

/**
 * Helper function to read bytes from one array to another.
 */
static mpe_Error readMemBytes(uint8_t *buffer, uint32_t start,
        uint32_t numBytes, uint32_t timeout, uint8_t *result,
        uint32_t *bytesRead)
{
    uint32_t i;

    MPE_UNUSED_PARAM(bytesRead); /* TODO: if this param is not used, should it be removed from this function? */
    MPE_UNUSED_PARAM(timeout); /* TODO: if this param is not used, should it be removed from this function? */

    for (i = 0; i < numBytes; i++)
    {
        result[i] = buffer[start + i];
    }
    return MPE_SUCCESS;
}

/**
 * Helper function to convert a string into a ocpBIOPObjectType
 */
static ocpBIOPObjectType ocpParseObjectType(uint8_t *string)
{
    // Compare the strings.
    // Use ncmp to make sure they give us a null terminated string.
    //
    if (strncmp(BIOP_FILE_STR, (char*) string, BIOP_OBJECTKIND_LENGTH) == 0)
    {
        return BIOP_File;
    }
    else if (strncmp(BIOP_DIRECTORY_STR, (char*) string, BIOP_OBJECTKIND_LENGTH)
            == 0)
    {
        return BIOP_Directory;
    }
    else if (strncmp(BIOP_SERVICEGATEWAY_STR, (char*) string,
            BIOP_OBJECTKIND_LENGTH) == 0)
    {
        return BIOP_ServiceGateway;
    }
    else if (strncmp(BIOP_STREAM_STR, (char*) string, BIOP_OBJECTKIND_LENGTH)
            == 0)
    {
        return BIOP_Stream;
    }
    else if (strncmp(BIOP_STREAMEVENT_STR, (char*) string,
            BIOP_OBJECTKIND_LENGTH) == 0)
    {
        return BIOP_StreamEvent;
    }
    else
    {
        // Error
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "BIOP: Unrecognized Object Kind: %s\n", string);
        return BIOP_UnknownType;
    }
}

/**
 * Helper function to grab the string and parse it out
 */
static mpe_Error ocpGetObjectType(ocpDataSource *source, uint32_t *offset,
        ocpBIOPObjectType *ret)
{
    uint8_t string[BIOP_OBJECTKIND_LENGTH + 1];
    mpe_Error retCode = MPE_SUCCESS;
    uint32_t lOffset = *offset;

    CHECK_UINT32(source, BIOP_OBJECTKIND_LENGTH, lOffset);

    // Get the value
    // MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Getting File Type String: Offset %d\n", lOffset);
    retCode = source->reader(source->dataSource, lOffset,
            BIOP_OBJECTKIND_LENGTH, source->timeout, string, &sink);
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Type String at %d: %s\n",
            lOffset, string);
    lOffset += BIOP_OBJECTKIND_LENGTH;
    CHECK_RESULT(retCode);

    // Do string compares.
    // THIS ASSUMES NAMES ARE IN NULL TERMINATED STRING FORM,
    // which they are supposed to be.
    *ret = ocpParseObjectType(string);
    if (*ret == BIOP_UnknownType)
    {
        retCode = MPE_FS_ERROR_INVALID_DATA;
        goto CleanUp;
    }

    *offset = lOffset;
    return MPE_SUCCESS;

    CleanUp: MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
            "BIOP: ERROR Parsing Object Kind at offset %d\n", *offset);
    return retCode;
}

// Prototype of a parsing function
// Cut and paste from here.
#ifdef NDEF
static
mpe_Error
ocpParsePrototype(
        ocpDataSource *source,
        uint32_t *offset,
        ocpBIOPMessage *message)
{
    mpe_Error retCode = MPE_SUCCESS;
    uint32_t lOffset = *offset;

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Parsing Prototype at %d\n", lOffset);
#endif

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Done parsing Prototype: Offset: %d Bytes: %d\n", *offset, lOffset - *offset);
#endif
    *offset = lOffset;
    return retCode;

    CleanUp:
    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS, "BIOP: ERROR parsing Prototype at offset %d\n", *offset);
    return retCode;
}
#endif

//------------------------------------------------------------------
// Parsing functions
//------------------------------------------------------------------

/**
 * Parse a TAP structure, as defined in the BIOP structures in TAM 232r31.
 *
 * @param source A data source object containing the data to parse, and the function to read it.
 * @param offset [in/out] A pointer to the offset to start reading at.  If a successful parse takes
 *               place, the offset will be changed to point to the byte just past the parsed data.
 * @param gateway The object to fill with the parsed data.  May contain partial data if an error
 *                occurred during parsing.
 *
 * @return MPE_SUCCESS if the function parsed correctly, error codes otherwise.
 */
static mpe_Error ocpParseTAP(ocpDataSource *source, uint32_t *offset,
        ocpTAP *tap)
{
    mpe_Error retCode = MPE_SUCCESS;
    uint32_t lOffset = *offset;
    uint8_t length;
#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Parsing TAP at offset %d\n", lOffset);
#endif

    // Parse a TAP, per TAM 232r31 Page 278-279
    GET_UINT16(source, lOffset, tap->id);
    GET_UINT16(source, lOffset, tap->use);
    GET_UINT16(source, lOffset, tap->assocTag);

    // Get the selector length
    GET_UINT8(source, lOffset, length);

    // If the total selector length >= 10, we will have a
    // Selector type, and at least the transaction and timeout values
    if (length >= 10)
    {
        GET_UINT16(source, lOffset, tap->selectorType);
        GET_UINT32(source, lOffset, tap->transactionID);
        GET_UINT32(source, lOffset, tap->timeOut);

        lOffset += length - 10; // Don't add the 10 bytes we just parsed out
    }
    else
    {
        // Descriptor length is too short, we can't cope.
        // Just skip the whole thing
        lOffset += length;
        // Null out the fields.
        tap->selectorType = 0;
        tap->transactionID = 0;
        tap->timeOut = 0;
    }

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Done parsing TAP: Offset: %d Bytes: %d\n", *offset, lOffset - *offset);
#endif
    *offset = lOffset;
    return MPE_SUCCESS;

    CleanUp: MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
            "BIOP: ERROR parsing TAP at offset %d\n", *offset);
    return retCode;
}

/**
 * Parse the BIOP::ObjectLocation object.
 *
 * @param source A data source object containing the data to parse, and the function to read it.
 * @param offset [in/out] A pointer to the offset to start reading at.  If a successful parse takes
 *               place, the offset will be changed to point to the byte just past the parsed data.
 * @param profile The object to fill with the parsed data.  May contain partial data if an error
 *                occurred during parsing.
 *
 * @return MPE_SUCCESS if the function parsed correctly, error codes otherwise.
 */
static mpe_Error ocpParseBIOPObjectLocation(ocpDataSource *source,
        uint32_t *offset, ocpBIOPObjectLocation *objectLoc)
{
    mpe_Error retCode = MPE_SUCCESS;
    uint32_t lOffset = *offset;
    uint8_t componentLength;
    uint8_t objectKeyLength;
    uint8_t temp;
    uint32_t objectKey = 0;
    uint32_t i;

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Parsing BIOP::ObjectLoctaion at %d\n", lOffset);
#endif

    GET_UINT8(source, lOffset, componentLength);

    GET_UINT32(source, lOffset, objectLoc->carouselID);
    GET_UINT16(source, lOffset, objectLoc->moduleID);

    CHECK_UINT8(source, BIOP_VERSION_MAJOR, lOffset);
    CHECK_UINT8(source, BIOP_VERSION_MINOR, lOffset);
    GET_UINT8(source, lOffset, objectKeyLength);

    for (i = 0; i < objectKeyLength; i++)
    {
        GET_UINT8(source, lOffset, temp);
        objectKey = (objectKey << 8) | temp;
    }
    objectLoc->objectKey = objectKey;

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Done parsing BIOP::ObjectLocation: Offset: %d Bytes: %d\n", *offset, lOffset - *offset);
#endif
    *offset = lOffset;
    return retCode;

    CleanUp: MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
            "BIOP: ERROR parsing BIOP::ObjectLocation at offset %d\n", *offset);
    return retCode;
}

/**
 * Parse the BIOPProfileBody structure, as defined in TAM 232r31, Pages 278-279.
 *
 * @param source A data source object containing the data to parse, and the function to read it.
 * @param offset [in/out] A pointer to the offset to start reading at.  If a successful parse takes
 *               place, the offset will be changed to point to the byte just past the parsed data.
 * @param profile The object to fill with the parsed data.  May contain partial data if an error
 *                occurred during parsing.
 *
 * @return MPE_SUCCESS if the function parsed correctly, error codes otherwise.
 */
static mpe_Error ocpParseBIOPProfileBody(ocpDataSource *source,
        uint32_t *offset, ocpBIOPProfileBody *profile)
{
    uint32_t lOffset = *offset;
    uint32_t profileDataLength;
    uint8_t liteComponentCount;
    uint8_t componentLength;
    uint32_t i;
    uint8_t tapsCount;
    mpe_Error retCode = MPE_SUCCESS;

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Parsing BIOP Profile Body at %d\n", lOffset);
#endif

    // Parse the BIOP Profile Header, per TAM 232r31, Page 278-279
    // Check the header.
    CHECK_UINT32(source, TAG_BIOP, lOffset);

    GET_UINT32(source, lOffset, profileDataLength);

    CHECK_UINT8(source, BIOP_BYTE_ORDER, lOffset);

    GET_UINT8(source, lOffset, liteComponentCount);

    // Parse the BIOP::ObjectLocation block
    // Per tam232r31 must be first component
    CHECK_UINT32(source, TAG_BIOP_OBJECTLOC, lOffset);

    retCode = ocpParseBIOPObjectLocation(source, &lOffset,
            &(profile->objectLoc));
    CHECK_RESULT(retCode);

    // Now parse the DSM::ConnBinder component
    CHECK_UINT32(source, TAG_CONNBINDER, lOffset);
    GET_UINT8(source, lOffset, componentLength);

    GET_UINT8(source, lOffset, tapsCount);
    if (tapsCount < 1)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS, "BIOP: No taps\n");
        retCode = MPE_FS_ERROR_INVALID_DATA;
        goto CleanUp;
    }

    // Get the first TAP
    retCode = ocpParseTAP(source, &lOffset, &(profile->tap));
    CHECK_RESULT(retCode);

    // Ignore any further taps
    // Technically, we can skip everything after here, except that we
    // want to calculate the length correctly
    // Use the calculation specified on page 279
    lOffset += componentLength - 18;

    // Now, skip any further
    for (i = 2; i < liteComponentCount; i++)
    {
        lOffset += 4; // Skip the tag
        GET_UINT8(source, lOffset, componentLength);
        lOffset += componentLength;
    }

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Done parsing BIOP Profile Body: Offset: %d Bytes: %d\n", *offset, lOffset - *offset);
#endif
    *offset = lOffset;
    return MPE_SUCCESS;

    CleanUp: MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
            "BIOP: ERROR parsing BIOP Profile Body at offset %d\n", *offset);
    return retCode;
}

/**
 * Parse the BIOPProfileBody structure, as defined in TAM 232r31, Pages 278-279.
 *
 * @param source A data source object containing the data to parse, and the function to read it.
 * @param offset [in/out] A pointer to the offset to start reading at.  If a successful parse takes
 *               place, the offset will be changed to point to the byte just past the parsed data.
 * @param gateway The object to fill with the parsed data.  May contain partial data if an error
 *                occurred during parsing.
 *
 * @return MPE_SUCCESS if the function parsed correctly, error codes otherwise.
 */
static mpe_Error ocpParseBIOPLiteOptionsProfileBody(ocpDataSource *source,
        uint32_t *offset, ocpLiteOptionsProfileBody *liteOptionsBody)
{
    mpe_Error retCode = MPE_SUCCESS;
    uint32_t lOffset = *offset;
    uint32_t i;

    uint8_t liteComponentCount;
    uint8_t componentDataLength;
    uint32_t profileDataLength;
    uint32_t initialContextLength;
    uint32_t kindLength;
    uint32_t idLength;
    uint32_t nameComponentsCount;
    char *string;

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Parsing LiteOptionsProfileBody at %d\n", lOffset);
#endif

    // Parse the BIOP Profile Header, per TAM 232r31, Page 278-279
    // Check the header.
    CHECK_UINT32(source, TAG_LITE_OPTIONS, lOffset);

    GET_UINT32(source, lOffset, profileDataLength);

    CHECK_UINT8(source, BIOP_BYTE_ORDER, lOffset);

    GET_UINT8(source, lOffset, liteComponentCount);
    if (liteComponentCount < 1)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "BIOP: LiteOptionsProfileBody liteComponentCount must be >= 1\n");
        retCode = MPE_FS_ERROR_INVALID_DATA;
        goto CleanUp;
    }
    // Parse the first DSM::ServiceLocation component
    CHECK_UINT32(source, TAG_SERVICE_LOC, lOffset);
    lOffset++; // Skip the serviceDataLength
    CHECK_UINT8(source, NSAP_ADDRESS_LENGTH, lOffset); // Make sure the NSAP address is reasonable.

    retCode = source->reader(source->dataSource, lOffset, NSAP_ADDRESS_LENGTH,
            source->timeout, liteOptionsBody->addressNSAP, &sink);
    lOffset += NSAP_ADDRESS_LENGTH;

    GET_UINT32(source, lOffset, nameComponentsCount);

    if (nameComponentsCount < 1)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "BIOP: LiteOptionsProfileBody DSM::Location nameComponentsCount must be >= 1\n");
        retCode = MPE_FS_ERROR_INVALID_DATA;
        goto CleanUp;
    }

    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, nameComponentsCount
            * sizeof(char *), (void **) &(liteOptionsBody->nameComps));

    CHECK_RESULT(retCode);

    liteOptionsBody->numNameComps = nameComponentsCount;

    // Clear it out.  Just for good measure, and error checking.
    memset(liteOptionsBody->nameComps, 0, nameComponentsCount * sizeof(char *));

    // Read each component, and add it to the end of String
    for (i = 0; i < nameComponentsCount; i++)
    {
        // Read the ID length
        GET_UINT32(source, lOffset, idLength);

        // Increase the string length
        retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, idLength + 1,
                (void **) &string);
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_FILESYS,
                    "BIOP: Unable to allocate %d bytes for LiteOptionsBodyProfile file name\n",
                    idLength + 1);
            goto CleanUp;
        }

        // Put it in the array
        liteOptionsBody->nameComps[i] = string;

        // Read the name component out of the buffer
        retCode = source->reader(source->dataSource, lOffset, idLength,
                source->timeout, (unsigned char *) string, &sink);
        CHECK_RESULT(retCode);
        lOffset += idLength;

        // Just for safety.
        string[idLength] = '\0';

        // Skip the kind component.  Not needed
        // HACK: TODO: Could check to make sure it's a directory, but we don't.
        GET_UINT32(source, lOffset, kindLength);
        lOffset += kindLength;
    }

    // Skip the inital context field
    GET_UINT32(source, lOffset, initialContextLength);
    lOffset += initialContextLength;

    // Skip remaining DSM::ServiceLocation components.
    for (i = 1; i < liteComponentCount; i++)
    {
        lOffset += 4; // Skip the tag.
        GET_UINT8(source, lOffset, componentDataLength);
        lOffset += componentDataLength;
    }

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Done parsing LiteOptionsProfileBody: Offset: %d Bytes: %d\n", *offset, lOffset - *offset);
#endif
    *offset = lOffset;
    return retCode;

    CleanUp: MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
            "BIOP: ERROR parsing LiteOptionsProfileBody at offset %d\n",
            *offset);
    return retCode;
}

/**
 * Parse the IOR structure, as defined in TAM 232r31, page 277.
 *
 * @param source A data source object containing the data to parse, and the function to read it.
 * @param offset [in/out] A pointer to the offset to start reading at.  If a successful parse takes
 *               place, the offset will be changed to point to the byte just past the parsed data.
 * @param gateway The object to fill with the parsed data.  May contain partial data if an error
 *                occurred during parsing.
 *
 * @return MPE_SUCCESS if the function parsed correctly, error codes otherwise.
 */
static mpe_Error ocpParseIOR(ocpDataSource *source, uint32_t *offset,
        ocpIOR *ior)
{
    uint32_t lOffset = *offset;
    uint32_t profileCount;
    uint32_t profileTag;
    uint32_t profileLength;
    uint32_t i;
    mpe_Error retCode = MPE_SUCCESS;

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Parsing IOP::IOR at %d\n", lOffset);
#endif

    // Get the object type
    retCode = ocpGetObjectType(source, &lOffset, &ior->typeID);
    CHECK_RESULT(retCode);

    GET_UINT32(source, lOffset, profileCount);
    if (profileCount == 0)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "BIOP: Invalid number of profiles: 0\n");
        retCode = MPE_FS_ERROR_INVALID_DATA;
        goto CleanUp;
    }
    // Get the first profile only.
    // Just read the tag, don't bump the pointer.  We'll read it again.
    retCode = get_uint32_t(source, lOffset, &profileTag);
    CHECK_RESULT(retCode);

    ior->profile.profileTag = profileTag;
    switch (profileTag)
    {
    case TAG_BIOP:
        retCode = ocpParseBIOPProfileBody(source, &lOffset,
                &(ior->profile.body.biopProfile));
        break;
    case TAG_LITE_OPTIONS:
        retCode = ocpParseBIOPLiteOptionsProfileBody(source, &lOffset,
                &(ior->profile.body.liteOptionsProfile));
        break;
    default:
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "BIOP: Unsupported IOP::taggedProfile tag: %08x\n", profileTag);
        retCode = MPE_FS_ERROR_INVALID_DATA;
        break;
    }
    CHECK_RESULT(retCode);
    // Skip any other profile tags
    for (i = 1; i < profileCount; i++)
    {
        GET_UINT32(source, lOffset, profileTag);
        GET_UINT32(source, lOffset, profileLength);
        // Add the profile length to skip additional profiles.
        // Subtract 8 bytes for the 2 dWords just read.
        lOffset += profileLength - 8;
    }

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Done parsing IOP::IOR: Offset: %d Bytes: %d\n", *offset, lOffset - *offset);
#endif
    *offset = lOffset;
    return retCode;

    CleanUp: MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
            "BIOP: ERROR parsing IOP::IOR at offset %d\n", *offset);
    return retCode;
}

/**
 * Parse a ServiceContext structure.  Currently only recognizes the GIOP_CodeSet service context,
 * ignores all others.
 *
 * @param source A data source object containing the data to parse, and the function to read it.
 * @param offset [in/out] A pointer to the offset to start reading at.  If a successful parse takes
 *               place, the offset will be changed to point to the byte just past the parsed data.
 * @param file   The object to fill with the parsed data.  May contain partial data if an error
 *                occurred during parsing.
 *
 * @return MPE_SUCCESS if the function parsed correctly, error codes otherwise.
 */
static mpe_Error ocpParseServiceContext(ocpDataSource *source,
        uint32_t *offset, ocpServiceContext *serviceContext)
{
    mpe_Error retCode = MPE_SUCCESS;
    uint32_t lOffset = *offset;
    uint32_t contextID = 0;
    uint16_t contextDataLength = 0;

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Parsing ServiceContext at %d\n", lOffset);
#endif

    GET_UINT32(source, lOffset, contextID);
    GET_UINT16(source, lOffset, contextDataLength);

    serviceContext->contextTag = contextID;

    switch (contextID)
    {
    case TAG_GIOP_CODESET:
        // Check that the length is right.  Must be 2 32-bit integers
        if (contextDataLength != 8) // had better be 8
        {
            retCode = MPE_FS_ERROR_INVALID_DATA;
            goto CleanUp;
        }
        GET_UINT32(source, lOffset, serviceContext->sc.codeSet.charData)
        ;
        GET_UINT32(source, lOffset, serviceContext->sc.codeSet.wCharData)
        ;
        break;
    default:
        // Skip anything else
        lOffset += contextDataLength;
        break;
    }

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Done parsing ServiceContext: Offset: %d Bytes: %d\n", *offset, lOffset - *offset);
#endif
    *offset = lOffset;
    return retCode;

    CleanUp: MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
            "BIOP: ERROR parsing ServiceContext at offset %d\n", *offset);
    return retCode;
}

/**
 * Parse the file version BIOPProfileBody structure, as defined in TAM 232r31, Pages 273-274.
 * Start parsing after the objectKind field.
 *
 * @param source A data source object containing the data to parse, and the function to read it.
 * @param offset [in/out] A pointer to the offset to start reading at.  If a successful parse takes
 *               place, the offset will be changed to point to the byte just past the parsed data.
 * @param file   The object to fill with the parsed data.  May contain partial data if an error
 *                occurred during parsing.
 *
 * @return MPE_SUCCESS if the function parsed correctly, error codes otherwise.
 */
static mpe_Error ocpParseFileObjectInfo(ocpDataSource *source,
        uint32_t *offset, ocpFileData *file)
{
    mpe_Error retCode = MPE_SUCCESS;
    uint32_t lOffset = *offset;
    uint16_t objectInfoLength;
    uint32_t currOffset;
    uint32_t msw, lsw;
    uint8_t descriptorTag;
    uint8_t descriptorLen;

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Parsing File Object Info at %d\n", lOffset);
#endif

    GET_UINT16(source, lOffset, objectInfoLength);
    currOffset = lOffset;
    // First, a DSM::ContentSize field.
    GET_UINT32(source, lOffset, msw);
    GET_UINT32(source, lOffset, lsw);
    objectInfoLength -= 8;
    // Build the content size field
    file->contentSize = (((uint64_t) msw) << 32) | ((uint64_t) lsw);

    // Now, parse out any descriptors
    while (objectInfoLength > 0)
    {
        GET_UINT8(source, lOffset, descriptorTag);
        GET_UINT8(source, lOffset, descriptorLen);
        objectInfoLength -= 2;
        currOffset = lOffset;

        switch (descriptorTag)
        {
        case TAG_CONTENT_TYPE_DESC:
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "BIOP: Parsing content type descriptor at %d.  %d bytes\n",
                    lOffset, (uint32_t) descriptorLen);
            if (file->contentType != NULL)
            {
                MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                        "BIOP: Multiple content type descriptors found.  Ignoring subsequent\n");
                break;
            }
            retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL,
                    (((uint32_t) descriptorLen) + 1) * sizeof(char),
                    (void **) (&file->contentType));
            CHECK_RESULT(retCode)
            ;
            retCode = source->reader(source->dataSource, lOffset,
                    (uint32_t) descriptorLen, source->timeout,
                    file->contentType, &sink);
            CHECK_RESULT(retCode)
            ;
            break;
        default:
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "BIOP: Ignoring descriptor tag %02x.  %d bytes\n",
                    (uint32_t) descriptorTag, (uint32_t) descriptorLen);

            break;
        }
        objectInfoLength -= (uint16_t) descriptorLen;
        lOffset = currOffset + (uint32_t) descriptorLen;
    }

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Done parsing File Object Info: Offset: %d Bytes: %d\n", *offset, lOffset - *offset);
#endif
    *offset = lOffset;
    return retCode;

    CleanUp: MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
            "BIOP: ERROR parsing File Object Info at offset %d\n", *offset);
    return retCode;
}

static mpe_Error ocpParseFileData(ocpDataSource *source, uint32_t *offset,
        ocpFileData *file)
{
    mpe_Error retCode = MPE_SUCCESS;
    uint32_t lOffset = *offset;

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Parsing File Data at %d\n", lOffset);
#endif

    // Now, get the length of the data, followed by the data itself
    // Instead of actually fetching the data, we just record it's offset, and let
    // the object carousel code read it directly from the data carousel
    GET_UINT32(source, lOffset, file->dataLength);
    file->dataOffset = lOffset;

    // Now, skip the entire data chunk.
    lOffset += file->dataLength;

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Done parsing File Data: Offset: %d Bytes: %d\n", *offset, lOffset - *offset);
#endif
    *offset = lOffset;
    return retCode;

    CleanUp: MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
            "BIOP: ERROR parsing File Data at offset %d\n", *offset);
    return retCode;
}

/**
 * Parse an individual directory entry field in a BIOP Directory message.
 * Defined in the middle of the BIOP directory message, in TAM 232r31, page 276
 *
 * @param source        A data source object containing the data to parse, and the function to read it.
 * @param offset        [in/out] A pointer to the offset to start reading at.  If a successful parse takes
 *                      place, the offset will be changed to point to the byte just past the parsed data.
 * @param dirEntry      The object to fill with the parsed data.  May contain partial data if an error
 *                      occurred during parsing.
 *
 * @return MPE_SUCCESS if the function parsed correctly, error codes otherwise.
 */
static mpe_Error ocpParseDirEntry(ocpDataSource *source, uint32_t *offset,
        ocpDirEntry *dirEntry)
{
    mpe_Error retCode = MPE_SUCCESS;
    uint32_t lOffset = *offset;
    uint8_t nameLength;
    uint16_t objectInfoLength;
    uint8_t typeBuffer[5];
    uint32_t msw, lsw;
    uint8_t descriptorTag;
    uint8_t descriptorLen;
    uint32_t currOffset;

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Parsing Directory Entry at %d\n", lOffset);
#endif

    // Parse the BIOP::Name structures
    // Make sure there's only one name
    // TAM 232r31, Table B.19, Page 276
    CHECK_UINT8(source, 1, lOffset);
    GET_UINT8(source, lOffset, nameLength);
    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, nameLength
            * sizeof(uint8_t), (void **) &(dirEntry->name));
    CHECK_RESULT(retCode);

    retCode = source->reader(source->dataSource, lOffset, nameLength,
            source->timeout, dirEntry->name, &sink);
    CHECK_RESULT(retCode);

    lOffset += nameLength;

    // Get the file type
    // Can't use the normal routine, as the size is only 1 byte here, rather
    // than 4 everyplace else.  Gag.
    CHECK_UINT8(source, 4, lOffset);
    retCode = source->reader(source->dataSource, lOffset, 4, source->timeout,
            typeBuffer, &sink);
    lOffset += 4;
    // Convert the name to the enum.
    dirEntry->objectType = ocpParseObjectType(typeBuffer);
    if (dirEntry->objectType == BIOP_UnknownType)
    {
        retCode = MPE_FS_ERROR_INVALID_DATA;
        goto CleanUp;
    }

    // FIXME: TODO: BUG: Is this acceptable?
    // Skip the binding type
    lOffset++;

    retCode = ocpParseIOR(source, &lOffset, &(dirEntry->ior));
    CHECK_RESULT(retCode);

    GET_UINT16(source, lOffset, objectInfoLength);

    // If the object is a file, grab the file size
    if (dirEntry->objectType == BIOP_File && objectInfoLength >= 8)
    {
        // First, a DSM::ContentSize field.
        GET_UINT32(source, lOffset, msw);
        GET_UINT32(source, lOffset, lsw);
        objectInfoLength -= 8;
        // Build the content size field
        dirEntry->fileSize = (((uint64_t) msw) << 32) | ((uint64_t) lsw);

        // Now, parse out any descriptors
        while (objectInfoLength > 0)
        {
            GET_UINT8(source, lOffset, descriptorTag);
            GET_UINT8(source, lOffset, descriptorLen);
            objectInfoLength -= 2;
            currOffset = lOffset;

            switch (descriptorTag)
            {
            case TAG_CONTENT_TYPE_DESC:
                MPE_LOG(
                        MPE_LOG_TRACE2,
                        MPE_MOD_FILESYS,
                        "BIOP: Parsing content type descriptor at %d.  %d bytes\n",
                        lOffset, (uint32_t) descriptorLen);
                if (dirEntry->contentType != NULL)
                {
                    MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                            "BIOP: Multiple content type descriptors found.  Ignoring subsequent\n");
                    break;
                }
                retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL,
                        (((uint32_t) descriptorLen) + 1) * sizeof(char),
                        (void **) (&dirEntry->contentType));
                CHECK_RESULT(retCode)
                ;
                retCode = source->reader(source->dataSource, lOffset,
                        (uint32_t) descriptorLen, source->timeout,
                        dirEntry->contentType, &sink);
                CHECK_RESULT(retCode)
                ;
                break;
            default:
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                        "BIOP: Ignoring descriptor tag %02x.  %d bytes\n",
                        (uint32_t) descriptorTag, (uint32_t) descriptorLen);

                break;
            }
            objectInfoLength -= (uint16_t) descriptorLen;
            lOffset = currOffset + (uint32_t) descriptorLen;
        }
    }
    else
    {
        // Didn't get a filesize.  Clear it out.
        dirEntry->fileSize = 0;
        // Skip the object info structure entirely
        // FIXME: TODO: There could be descriptors here.
        // We can't do anything with them, but they could be here.
        lOffset += objectInfoLength;
    }

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Done Parsing Directory Entry\n");
#endif
    *offset = lOffset;
    return retCode;

    CleanUp: MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
            "BIOP: ERROR parsing Directory Entry at offset %d\n", *offset);
    return retCode;
}

/**
 * Parse the directory entries in a directory.  Expects to be pointed at the start of the first
 * entry.
 *
 * @param source A data source object containing the data to parse, and the function to read it.
 * @param offset [in/out] A pointer to the offset to start reading at.  If a successful parse takes
 *               place, the offset will be changed to point to the byte just past the parsed data.
 * @param dir    The object to fill with the parsed data.  May contain partial data if an error
 *                occurred during parsing.
 *
 * @return MPE_SUCCESS if the function parsed correctly, error codes otherwise.
 */
mpe_Error ocpParseDirEntries(ocpDataSource *source, uint32_t *offset,
        ocpDirData *dir)
{
    mpe_Error retCode = MPE_SUCCESS;
    uint32_t lOffset = *offset;
    uint16_t i;
    ocpDirEntry *dirEntries = NULL; // Local copy
    mpe_Bool freeEntries = false;

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Parsing Directory Entries at %d\n", lOffset);
#endif

    // Make sure the data makes sense.
    if (lOffset != dir->dataOffset)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "BIOP: Data offset (%d) should equal parse offset (%d)\n",
                lOffset, dir->dataOffset);
        retCode = MPE_FS_ERROR_INVALID_DATA;
        goto CleanUp;
    }

    // Now, we're running this thing

    // Allocate up space for that many files
    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, dir->numDirEntries
            * sizeof(ocpDirEntry), (void **) &(dirEntries));
    CHECK_RESULT(retCode);

    // Clear it out.
    memset(dirEntries, 0, dir->numDirEntries * sizeof(ocpDirEntry));

    // Parse each directory entry
    for (i = 0; i < dir->numDirEntries; i++)
    {
        retCode = ocpParseDirEntry(source, &lOffset, &(dirEntries[i]));
        CHECK_RESULT(retCode);
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_FILESYS,
                "BIOP: Adding directory entry %d: Name: %s Type: %d Size: %d\n",
                i, dirEntries[i].name, dirEntries[i].objectType,
                (uint32_t) dirEntries[i].fileSize);
    }

    // Fill in the directory entries chunck.
    // BEGIN CRITICAL SECTION
    mpe_mutexAcquire(ocpGlobalMutex);

    if (dir->dirEntries == NULL)
    {
        dir->dirEntries = dirEntries;
    }
    else
    {
        freeEntries = true;
    }

    mpe_mutexRelease(ocpGlobalMutex);
    // END CRITICAL SECTION

    if (freeEntries)
    {
        ocpFreeDirEntries(dir->numDirEntries, dirEntries);
    }
#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Done parsing Directory Entries: Offset: %d Bytes: %d\n", *offset, lOffset - *offset);
#endif
    *offset = lOffset;
    return retCode;

    CleanUp: MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
            "BIOP: ERROR parsing Directory Entries at offset %d\n", *offset);
    ocpFreeDirEntries(dir->numDirEntries, dirEntries);
    return retCode;
}

/**
 * Parse the directory version BIOPProfileBody structure, as defined in TAM 232r31, Pages 275-276.
 * Start parsing after the objectKind field.
 *
 * @param source A data source object containing the data to parse, and the function to read it.
 * @param offset [in/out] A pointer to the offset to start reading at.  If a successful parse takes
 *               place, the offset will be changed to point to the byte just past the parsed data.
 * @param dir    The object to fill with the parsed data.  May contain partial data if an error
 *                occurred during parsing.
 *
 * @return MPE_SUCCESS if the function parsed correctly, error codes otherwise.
 */
static mpe_Error ocpParseDirObjectInfo(ocpDataSource *source, uint32_t *offset,
        ocpDirData *dir)
{
    mpe_Error retCode = MPE_SUCCESS;
    uint16_t objectInfoLength;
    uint32_t lOffset = *offset;

    MPE_UNUSED_PARAM(dir); /* TODO: if this param is not used, should it be removed from this function? */

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Parsing Directory ObjectInfo at %d\n", lOffset);
#endif

    // Skip all ObjectInfo stuff in the directory
    GET_UINT16(source, lOffset, objectInfoLength);

    lOffset += objectInfoLength;

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Done parsing Directory ObjectInfo: Offset: %d Bytes: %d\n", *offset, lOffset - *offset);
#endif
    *offset = lOffset;
    return retCode;

    CleanUp: MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
            "BIOP: ERROR parsing Directory ObjectInfo at offset %d\n", *offset);
    return retCode;
}

static mpe_Error ocpParseDirData(ocpDataSource *source, uint32_t *offset,
        ocpDirData *dir)
{
    mpe_Error retCode = MPE_SUCCESS;
    uint32_t lOffset = *offset;

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Parsing Directory Data at %d\n", lOffset);
#endif

    // Next the bindings_count, ie, the number of files in the directory
    GET_UINT16(source, lOffset, dir->numDirEntries);

    // Clear out the dirEntries field.  We leave this until the directory entries are loaded.
    dir->dirEntries = NULL;

    // Record the current position
    dir->dataOffset = lOffset;

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Done parsing Directory Data: Offset: %d Bytes: %d\n", *offset, lOffset - *offset);
#endif
    *offset = lOffset;
    return retCode;

    CleanUp: MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
            "BIOP: ERROR parsing Directory Data at offset %d\n", *offset);
    return retCode;
}

/**
 * Parse the Stream Info (DSM::Stream::Info_T) structure that makes up the core of
 * both Stream and Stream Event objects
 *
 * @param source A data source object containing the data to parse, and the function to read it.
 * @param offset [in/out] A pointer to the offset to start reading at.  If a successful parse takes
 *               place, the offset will be changed to point to the byte just past the parsed data.
 * @param sink   Where to sink the broken data.
 *
 * @return MPE_SUCCESS if the function parsed correctly, error codes otherwise.
 */
static mpe_Error ocpParseStreamInfo(ocpDataSource *source, uint32_t *offset,
        ocpStreamInfo *info)
{
    mpe_Error retCode = MPE_SUCCESS;
    uint32_t lOffset = *offset;
    uint8_t descLength;

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Parsing Stream Info_T at %d\n", lOffset);
#endif
    // Get the description length, and skip it.
    GET_UINT8(source, lOffset, descLength);
    lOffset += descLength;
    // Now, pull out the relevant data
    GET_UINT32(source, lOffset ,info->durationSeconds);
    GET_UINT32(source, lOffset ,info->durationMicroSeconds);
    GET_UINT8(source, lOffset, info->audio);
    GET_UINT8(source, lOffset, info->video);
    GET_UINT8(source, lOffset, info->data);

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Done parsing Stream Info_T: Offset: %d Bytes: %d\n", *offset, lOffset - *offset);
#endif
    *offset = lOffset;
    return retCode;

    CleanUp: MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
            "BIOP: ERROR parsing Stream Info_T at offset %d\n", *offset);
    return retCode;
}

/**
 * Parse the stream version BIOPProfileBody structure, as defined in TAM 232r31, Pages 282-283
 * Start parsing after the objectKind field.
 *
 * @param source A data source object containing the data to parse, and the function to read it.
 * @param offset [in/out] A pointer to the offset to start reading at.  If a successful parse takes
 *               place, the offset will be changed to point to the byte just past the parsed data.
 * @param sink   Where to sink the broken data.
 *
 * @return MPE_SUCCESS if the function parsed correctly, error codes otherwise.
 */
static mpe_Error ocpParseStreamObjectInfo(ocpDataSource *source,
        uint32_t *offset, ocpStreamData *stream)
{
    mpe_Error retCode = MPE_SUCCESS;
    uint16_t objectInfoLength;
    uint32_t lOffset = *offset;
    uint32_t currPos;

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Parsing Stream Data at %d\n", lOffset);
#endif

    GET_UINT16(source, lOffset, objectInfoLength);

    // Save the current position
    currPos = lOffset;

    retCode = ocpParseStreamInfo(source, &lOffset, &(stream->sInfo));
    CHECK_RESULT(retCode);

    // Skip the object info bytes
    lOffset = currPos + objectInfoLength;

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Done parsing Stream Data: Offset: %d Bytes: %d\n", *offset, lOffset - *offset);
#endif
    *offset = lOffset;
    return retCode;

    CleanUp: MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
            "BIOP: ERROR parsing Stream Data at offset %d\n", *offset);
    return retCode;
}

static mpe_Error ocpParseStreamData(ocpDataSource *source, uint32_t *offset,
        ocpStreamData *stream)
{
    mpe_Error retCode = MPE_SUCCESS;
    uint32_t lOffset = *offset;
    uint8_t tapsCount;
    uint16_t i;

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Parsing Prototype at %d\n", lOffset);
#endif

    GET_UINT8(source, lOffset, tapsCount);
    stream->tapsCount = tapsCount;
    if (tapsCount > 0)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "BIOP: Taps Count: %d\n",
                tapsCount);

        retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, (stream->tapsCount
                * sizeof(ocpTAP)), (void **) &(stream->taps));
        CHECK_RESULT(retCode);
        //
        for (i = 0; i < tapsCount; i++)
        {
            retCode = ocpParseTAP(source, &lOffset, &(stream->taps[i]));
            CHECK_RESULT(retCode);
        }
    }

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Done parsing Prototype: Offset: %d Bytes: %d\n", *offset, lOffset - *offset);
#endif
    *offset = lOffset;
    return retCode;

    CleanUp: MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
            "BIOP: ERROR parsing Prototype at offset %d\n", *offset);
    return retCode;
}

/**
 * Parse the stream event version BIOPProfileBody structure, as defined in TAM 232r31, Pages 282-283
 * Start parsing after the objectKind field.
 *
 * @param source A data source object containing the data to parse, and the function to read it.
 * @param offset [in/out] A pointer to the offset to start reading at.  If a successful parse takes
 *               place, the offset will be changed to point to the byte just past the parsed data.
 * @param sink   Where to sink the broken data.
 *
 * @return MPE_SUCCESS if the function parsed correctly, error codes otherwise.
 */
static mpe_Error ocpParseStreamEventObjectInfo(ocpDataSource *source,
        uint32_t *offset, ocpStreamEventData *stream)
{
    mpe_Error retCode = MPE_SUCCESS;
    uint16_t objectInfoLength;
    uint32_t lOffset = *offset;
    uint16_t i;
    uint16_t numEventNames;
    uint8_t eventNameLength;
    uint32_t currPos;

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Parsing StreamEvent ObjectInfo at %d\n", lOffset);
#endif

    GET_UINT16(source, lOffset, objectInfoLength);

    // Save the current position
    currPos = lOffset;

    // Parse the StreamInfo structure
    retCode = ocpParseStreamInfo(source, &lOffset, &(stream->sInfo));
    CHECK_RESULT(retCode);

    // Parse the DSM::Event::EventList_T structure
    // Parse the event names
    GET_UINT16(source, lOffset, numEventNames);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "BIOP: Events %d\n", numEventNames);

    stream->eventCount = numEventNames;

    // Allocate up an array of event names
    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, (numEventNames
            * sizeof(char *)), (void **) &(stream->eventNames));
    CHECK_RESULT(retCode);
    memset(stream->eventNames, 0, (numEventNames * sizeof(char *)));

    // Now, actually parse the event names.
    for (i = 0; i < numEventNames; i++)
    {
        // Get the length of the string
        GET_UINT8(source, lOffset, eventNameLength);
        retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, (eventNameLength
                * sizeof(char)), (void **) &(stream->eventNames[i]));
        CHECK_RESULT(retCode);

        // Copy the string out
        retCode
                = source->reader(source->dataSource, lOffset, eventNameLength,
                        source->timeout,
                        (unsigned char *) stream->eventNames[i], &sink);
        CHECK_RESULT(retCode);
        lOffset += eventNameLength;

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "BIOP: Event Name %d: %s\n", i,
                stream->eventNames[i]);
    }

    // Skip the object info bytes
    lOffset = currPos + objectInfoLength;

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Done parsing StreamEvent ObjectInfo: Offset: %d Bytes: %d\n", *offset, lOffset - *offset);
#endif
    *offset = lOffset;
    return retCode;

    CleanUp: MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
            "BIOP: ERROR parsing StreamEvent ObjectInfo at offset %d\n",
            *offset);
    return retCode;
}

static mpe_Error ocpParseStreamEventData(ocpDataSource *source,
        uint32_t *offset, ocpStreamEventData *stream)
{
    mpe_Error retCode = MPE_SUCCESS;
    uint32_t lOffset = *offset;
    uint8_t numEventIDs;
    uint8_t tapsCount;
    uint32_t i;

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Parsing StreamEvent Data at %d\n", lOffset);
#endif

    GET_UINT8(source, lOffset, tapsCount);
    stream->tapsCount = tapsCount;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "BIOP: Taps %d\n", tapsCount);

    if (tapsCount > 0)
    {
        retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, (stream->tapsCount
                * sizeof(ocpTAP)), (void **) &(stream->taps));
        CHECK_RESULT(retCode);
        //
        for (i = 0; i < stream->tapsCount; i++)
        {
            retCode = ocpParseTAP(source, &lOffset, &(stream->taps[i]));
            CHECK_RESULT(retCode);
        }
    }

    // Get the number of events.
    // Note, it's only 1 byte this time, 2 bytes above.
    // Ugly.
    GET_UINT8(source, lOffset, numEventIDs);

    // Make sure it makes sense, ie, is the same as the number of names
    if (numEventIDs != stream->eventCount)
    {
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_FILESYS,
                "BIOP: Number of event IDs (%d) does not match number of event names (%d)\n",
                (uint32_t) numEventIDs, (uint32_t) stream->eventCount);
        retCode = MPE_FS_ERROR_INVALID_DATA;
        goto CleanUp;
    }

    // Allocate up the event IDs array while we're at it
    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, (numEventIDs
            * sizeof(uint16_t)), (void **) &(stream->eventIDs));
    CHECK_RESULT(retCode);

    // Get the actual event IDs
    for (i = 0; i < stream->eventCount; i++)
    {
        GET_UINT16(source, lOffset, stream->eventIDs[i]);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "BIOP: EventID %d: %d\n", i,
                stream->eventIDs[i]);
    }

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Done parsing StreamEvent Data: Offset: %d Bytes: %d\n", *offset, lOffset - *offset);
#endif
    *offset = lOffset;
    return retCode;

    CleanUp: MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
            "BIOP: ERROR parsing StreamEvent Data at offset %d\n", *offset);
    return retCode;
}

/**
 * Parse a BIOP object.  Can be a directory, file, service gateway, stream, or stream event.
 * Defined in TAM 232r31, pages 273-274 (file), 275-276 (directory/service gateway),
 * 277 (service gateway), 282-283 (Stream), and 284-285 (Stream Event).
 *
 * @param source A data source object containing the data to parse, and the function to read it.
 * @param offset [in/out] A pointer to the offset to start reading at.  If a successful parse takes
 *               place, the offset will be changed to point to the byte just past the parsed data.
 * @param gateway The object to fill with the parsed data.  May contain partial data if an error
 *                occurred during parsing.
 *
 * @return MPE_SUCCESS if the function parsed correctly, error codes otherwise.
 */
mpe_Error ocpParseBIOPObject(ocpDataSource *source, uint32_t *offset,
        ocpBIOPObject *object)
{
    mpe_Error retCode = MPE_SUCCESS;
    uint32_t lOffset = *offset;

    uint8_t objectKeyLength;
    uint32_t objectKey = 0;
    uint8_t i;
    uint8_t temp;
    uint8_t serviceContextListCount;
    uint32_t currOffset;
    uint32_t messageBodyLength;

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Parsing BIOP::Object Message at %d\n", lOffset);
#endif
    memset(object, 0, sizeof(ocpBIOPObject));

    // Start parsing the message
    // First several fields are all constant
    CHECK_UINT32(source, BIOP_STRING, lOffset);
    CHECK_UINT8(source, BIOP_VERSION_MAJOR, lOffset);
    CHECK_UINT8(source, BIOP_VERSION_MINOR, lOffset);
    CHECK_UINT8(source, BIOP_BYTE_ORDER, lOffset);
    CHECK_UINT8(source, BIOP_MESSAGE_TYPE, lOffset);

    GET_UINT32(source, lOffset, object->messageSize);

    // Get the object key.  Usually 4 bytes, always less than or equal
    GET_UINT8(source, lOffset, objectKeyLength);
    for (i = 0; i < objectKeyLength; i++)
    {
        GET_UINT8(source, lOffset, temp);
        objectKey = (objectKey << 8) | temp;
    }
    object->objectKey = objectKey;

    // Get the object type
    retCode = ocpGetObjectType(source, &lOffset, &(object->objectType));
    CHECK_RESULT(retCode);

    // Parse the object info field.
    switch (object->objectType)
    {
    case BIOP_File:
        retCode = ocpParseFileObjectInfo(source, &lOffset,
                &(object->payload.file));
        break;
    case BIOP_Directory:
    case BIOP_ServiceGateway:
        retCode = ocpParseDirObjectInfo(source, &lOffset,
                &(object->payload.dir));
        break;
    case BIOP_Stream:
        retCode = ocpParseStreamObjectInfo(source, &lOffset,
                &(object->payload.stream));
        break;
    case BIOP_StreamEvent:
        retCode = ocpParseStreamEventObjectInfo(source, &lOffset,
                &(object->payload.streamEvent));
        break;
    case BIOP_UnknownType:
    default:
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "BIOP: Unable to parse unknown type\n");
        retCode = MPE_FS_ERROR_INVALID_DATA;
        break;
    }
    CHECK_RESULT(retCode);

    // Next, Parse the service context structures
    GET_UINT8(source, lOffset, serviceContextListCount);
    if (serviceContextListCount > 0)
    {
        retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL,
                serviceContextListCount * sizeof(ocpServiceContext),
                (void **) &(object->serviceContexts));
        CHECK_RESULT(retCode);

        object->numServiceContexts = serviceContextListCount;

        for (i = 0; i < serviceContextListCount; i++)
        {
            retCode = ocpParseServiceContext(source, &lOffset,
                    &(object->serviceContexts[i]));
        }
    }

    // Get the message body length
    GET_UINT32(source, lOffset, messageBodyLength);

    // Record the length of the total structure, just in case lOffset does not comeback
    // done, as it might in the case of a directory object
    currOffset = lOffset;

    // Finally, Parse the data payload
    switch (object->objectType)
    {
    case BIOP_File:
        retCode = ocpParseFileData(source, &lOffset, &(object->payload.file));
        break;
    case BIOP_Directory:
    case BIOP_ServiceGateway:
        retCode = ocpParseDirData(source, &lOffset, &(object->payload.dir));
        break;
    case BIOP_Stream:
        retCode = ocpParseStreamData(source, &lOffset,
                &(object->payload.stream));
        break;
    case BIOP_StreamEvent:
        retCode = ocpParseStreamEventData(source, &lOffset,
                &(object->payload.streamEvent));
        break;
    case BIOP_UnknownType:
    default:
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "BIOP: Unable to parse unknown type\n");
        retCode = MPE_FS_ERROR_INVALID_DATA;
        break;
    }
    CHECK_RESULT(retCode);

    // And put lOffset in the right place.
    lOffset = currOffset + messageBodyLength;

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Done parsing BIOP::Object Message: Offset: %d Bytes: %d\n", *offset, lOffset - *offset);
#endif
    *offset = lOffset;
    return retCode;

    CleanUp: MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
            "BIOP: ERROR parsing BIOP::Object Message at %d\n", *offset);
    // Purge the object to free up any allocated memory
    ocpPurgeBIOPObject(object);
    return retCode;
}

/**
 * Parse the ServiceGatewayInfo object from the private data section
 * of a DSI.  The ServiceGatewayInfo object is defined in TAM 232r31, page 272.
 *
 * @param source A data source object containing the data to parse, and the function to read it.
 * @param offset [in/out] A pointer to the offset to start reading at.  If a successful parse takes
 *               place, the offset will be changed to point to the byte just past the parsed data.
 * @param gateway The object to fill with the parsed data.  May contain partial data if an error
 *                occurred during parsing.
 *
 * @return MPE_SUCCESS if the function parsed correctly, error codes otherwise.
 */
mpe_Error ocpParseServiceGatewayLocation(ocpDataSource *source,
        uint32_t *offset, ocpGatewayInfo *gateway)
{
    mpe_Error retCode = MPE_SUCCESS;
    uint32_t lOffset = *offset;
    uint8_t count;
    uint16_t length;
    uint8_t length_8;
    uint8_t i;

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Parsing ServiceGatewayInfo Message at %d\n", lOffset);
#endif

    *offset = lOffset;
    retCode = ocpParseIOR(source, &lOffset, &(gateway->ior));

    CHECK_RESULT(retCode);

    if (gateway->ior.profile.profileTag != TAG_BIOP)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "BIOP: Invalid tag type in service gateway: %08x\n",
                gateway->ior.profile.profileTag);
        retCode = MPE_FS_ERROR_INVALID_DATA;
        goto CleanUp;
    }

    // Get the number of taps to ignore
    GET_UINT8(source, lOffset, count);

    // Skip any remaining taps
    for (i = 0; i < count; i++)
    {
        lOffset += sizeof(uint16_t); // Skip a 16 bit tag
        lOffset += sizeof(uint16_t); // Skip a 16 bit use field
        lOffset += sizeof(uint16_t); // Skip a 16 assocTag
        GET_UINT8(source, lOffset, length_8); // num selector bytes
        lOffset += length_8; // Skip the selector bytes
    }

    GET_UINT16(source, lOffset, length); // User Info length
    lOffset += length; // Just Skip it

#ifdef TRACE_PARSE
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOP: Done parsing ServiceGatewayInfo Message: Offset: %d Bytes: %d\n", *offset, lOffset - *offset);
#endif
    *offset = lOffset;
    return retCode;

    CleanUp: MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
            "BIOP: ERROR parsing ServiceGatewayInfo Message at offset %d\n",
            *offset);
    return retCode;
}

/**
 * Get the ServiceGatewayInfo object from the private data section
 * of a DSI.  The ServiceGatewayInfo object is defined in TAM 232r31, page 272. Performs the
 * gruntwork necessary to call ocpParseServiceGatewayLocation, above.
 * Note, this doesn't quite follow the format of everthing above.
 *
 * @param buffer A buffer of bytes containing the data in the private section of a DSI.
 * @param numBytes The number of bytes in the structure.
 * @param retValue Pointer to a pointer to a returned structure.
 *
 * @return MPE_SUCCESS if successful, error codes otherwise.
 */
mpe_Error ocpGetServiceGatewayLocation(uint8_t *buffer, uint32_t numBytes,
        ocpGatewayInfo *gateway)
{
    mpe_Error retCode = MPE_SUCCESS;
    ocpDataSource dSource;
    uint32_t offset = 0;

    // Build the data source.
    dSource.dataSource = (void *) buffer;
    dSource.reader = (READER_FUNCPTR) readMemBytes;
    dSource.timeout = 0; // Don't care, this reads out of an array

    // Do the actual parse
    retCode = ocpParseServiceGatewayLocation(&dSource, &offset, gateway);
    if (retCode != MPE_SUCCESS)
    {
        return retCode;
    }

    // Make sure we parsed a reasonable number.
    if (offset > numBytes)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "BIOP: Parsed too many bytes in the ServiceGatewayLocation record: parsed %d - max %d\n",
                offset, numBytes);
        return MPE_FS_ERROR_INVALID_DATA;
    }

    // And we're outta here.
    return retCode;
}

/**
 * Free up any directory entries being used.
 *
 * @param numDirEntries
 */
void ocpFreeDirEntries(uint32_t numDirEntries, ocpDirEntry *dirEntries)
{
    uint32_t i, j, limit;

    if (dirEntries == NULL)
        return;

    for (i = 0; i < numDirEntries; i++)
    {
        mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, dirEntries[i].name);
        // If it's a lite options body profile
        if (dirEntries[i].ior.profile.profileTag == TAG_LITE_OPTIONS)
        {
            limit
                    = dirEntries[i].ior.profile.body.liteOptionsProfile.numNameComps;
            for (j = 0; j < limit; j++)
            {
                mpeos_memFreeP(
                        MPE_MEM_FILE_CAROUSEL,
                        dirEntries[i].ior.profile.body.liteOptionsProfile.nameComps[j]);
            }
            mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL,
                    dirEntries[i].ior.profile.body.liteOptionsProfile.nameComps);
        }
        if (dirEntries[i].contentType != NULL)
        {
            mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, dirEntries[i].contentType);
        }
    }
    mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, dirEntries);
}

/**
 * Purge a BIOP Object.  Basically frees any underlying allocated memory structures
 * in the object, and resets it's type to Unknown.  Purge should be called before
 * attempting to use a BIOP object as the target of a second parse, such as when
 * searching in a module for a given object key.
 *
 * @param object The object to purge.
 */
void ocpPurgeBIOPObject(ocpBIOPObject *object)
{
    uint32_t i;

    if (object != NULL)
    {
        // MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "BIOP: Purging BIOP Object %p\n", object);
        // Free up any subfields that we've allocated.
        // Note, we don't need to clear this out, as the next call to ocpParseBiopObject will
        // purge everything.
        switch (object->objectType)
        {
        case BIOP_Directory:
        case BIOP_ServiceGateway:
            // Free the array of directory entries
            if (object->payload.dir.numDirEntries != 0
                    && object->payload.dir.dirEntries != NULL)
            {
                // Free the name values
                ocpFreeDirEntries(object->payload.dir.numDirEntries,
                        object->payload.dir.dirEntries);
            }
            object->payload.dir.numDirEntries = 0;
            object->payload.dir.dirEntries = NULL;
            break;
        case BIOP_File:
            if (object->payload.file.contentType != NULL)
            {
                mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL,
                        object->payload.file.contentType);
            }
            ;
            break;
        case BIOP_Stream:
            if (object->payload.stream.tapsCount != 0
                    && object->payload.stream.taps != NULL)
            {
                mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL,
                        object->payload.stream.taps);
            }
            break;
        case BIOP_StreamEvent:
            if (object->payload.streamEvent.tapsCount != 0
                    && object->payload.streamEvent.taps != NULL)
            {
                mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL,
                        object->payload.stream.taps);
            }
            if (object->payload.streamEvent.eventCount != 0
                    && object->payload.streamEvent.eventNames != NULL)
            {
                for (i = 0; i < object->payload.streamEvent.eventCount; i++)
                {
                    mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL,
                            object->payload.streamEvent.eventNames[i]);
                }
                mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL,
                        object->payload.streamEvent.eventNames);
                mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL,
                        object->payload.streamEvent.eventIDs);
            }
            break;

        default:
            break;
        }
        if (object->serviceContexts != NULL)
        {
            mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, object->serviceContexts);
            object->serviceContexts = NULL;
        }
        object->numServiceContexts = 0;
        object->objectType = BIOP_UnknownType;
        object->objectKey = 0xdeadbeef;
    }
}

/**
 * Free a BIOP object and all it's underlying memory.
 *
 * @param object The object to be freed.
 */
void ocpFreeBIOPObject(ocpBIOPObject *object)
{
    if (object != NULL)
    {
        // MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "BIOP: Freeing object %p\n", object);
        ocpPurgeBIOPObject(object);
        mpeos_memFreeP(MPE_MEM_FILE_CAROUSEL, object);
    }
}

/**
 * Allocate an empty BIOP object.
 *
 * @param retObject [out] A pointer to a pointer where the object will be returned.
 *
 * @return MPE_SUCCESS if the object is successfully allocated, MPE_NOMEM if it is not.
 */
mpe_Error ocpAllocBIOPObject(ocpBIOPObject **retObject)
{
    mpe_Error retCode = MPE_SUCCESS;
    ocpBIOPObject *object;

    retCode = mpeos_memAllocP(MPE_MEM_FILE_CAROUSEL, sizeof(ocpBIOPObject),
            (void **) &object);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "BIOP: Cannot allocate object\n");
        return retCode;
    }

    // Ensure it is safe to immediately purge this object.
    object->objectType = BIOP_UnknownType;
    object->serviceContexts = NULL;
    object->numServiceContexts = 0;

    *retObject = object;
    return retCode;
}

/**
 * Initialize the BIOP parser.
 *
 * @return MPE_SUCCESS if we succeeded, errors otherwise.
 */
mpe_Error ocpInit(void)
{
    mpe_Error retCode = MPE_SUCCESS;

    // Create the global things.
    retCode = mpe_mutexNew(&ocpGlobalMutex);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "BIOP: Could not initialize global lock: Error code %04x\n",
                retCode);
    }
    return retCode;
}

// DEBUGGING ROUTINES

#ifdef OCP_DUMP_OBJECTS
static
char *
ocpObjectTypeName(ocpBIOPObjectType ot)
{
    switch (ot)
    {
        case BIOP_File: return "File"; break;
        case BIOP_ServiceGateway: return "Service Gateway"; break;
        case BIOP_Directory: return "Directory"; break;
        case BIOP_Stream: return "Stream"; break;
        case BIOP_StreamEvent: return "Strem Event"; break;
        default: return "Unknown"; break;
    }
}

static
void
ocpDumpObjectLocation(ocpBIOPObjectLocation *ol)
{
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: Carousel ID     : %x\n", ol->carouselID);
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: Module ID       : %x\n", ol->moduleID);
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: Object Key      : %x\n", ol->objectKey);
}

static
void
ocpDumpTAP(ocpTAP *tap)
{
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: ID              : %x\n", tap->id);
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: Association Tag : %x\n", tap->assocTag);
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: Use             : %x\n", tap->use);
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: Selector Type   : %x\n", tap->selectorType);
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: Transaction ID  : %x\n", tap->transactionID);
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: Timeout         : %x\n", tap->timeOut);
}

static
void
ocpDumpBIOPProfileBody(ocpBIOPProfileBody *body)
{
    ocpDumpObjectLocation(&body->objectLoc);
    ocpDumpTAP(&body->tap);
}

static
void
ocpDumpTaggedProfile(ocpTaggedProfile *profile)
{
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: Profile Tag     : %x\n", profile->profileTag);
    switch (profile->profileTag)
    {
        case TAG_LITE_OPTIONS:
        MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: Lite Options Body Profile NYI\n");
        break;
        case TAG_BIOP:
        ocpDumpBIOPProfileBody(&profile->body.biopProfile);
        break;
        default:
        MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: Unknown type\n");
        break;
    }
}

static
void
ocpDumpIOR(ocpIOR *ior)
{
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: Object Type     : %s (%x)\n", ocpObjectTypeName(ior->typeID), ior->typeID);
    ocpDumpTaggedProfile(&ior->profile);
}

static
void
ocpDumpFileObject(ocpFileData *file)
{
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: Data Offset     : %d\n", file->dataOffset);
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: Data Length     : %d\n", file->dataLength);
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: Content Type    : %s\n", (file->contentType) ? file->contentType : "<undefined>");
}

static
void
ocpDumpDirEntry(ocpDirEntry *entry)
{
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: Filename        : %s\n", entry->name);
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: Object Type     : %s (%x)\n", ocpObjectTypeName(entry->objectType), entry->objectType);
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: Filesize        : %d\n", entry->fileSize);
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: Content Type    : %s\n", (entry->contentType) ? entry->contentType : "<undefined>");
    ocpDumpIOR(&entry->ior);
}

static
void
ocpDumpDirObject(ocpDirData *dir)
{
    int i;
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: Data Offset     : %d\n", dir->dataOffset);
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: Num Entries     : %d\n", dir->numDirEntries);
    if (dir->dirEntries != NULL)
    {
        for (i = 0; i < dir->numDirEntries; i++)
        {
            MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: Entry %d:\n", i);
            ocpDumpDirEntry(&dir->dirEntries[i]);
        }
    }
    else
    {
        MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: Entries not yet parsed\n");
    }
}

void
ocpDumpBIOPObject(ocpBIOPObject *object)
{
    if (object == NULL)
    {
        return;
    }

    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: BIOP Object: %p\n", object);
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: Message Size    : %d\n", object->messageSize);
    // Figure out a name
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: Object Type     : %s (%x)\n", ocpObjectTypeName(object->objectType), object->objectType);
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: Object Key      : %08x\n", object->objectKey);
    MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: ServiceContext  : %d\n", object->numServiceContexts);
    switch (object->objectType)
    {
        case BIOP_File:
        ocpDumpFileObject(&object->payload.file);
        break;
        case BIOP_ServiceGateway:
        case BIOP_Directory:
        ocpDumpDirObject(&object->payload.dir);
        break;
        case BIOP_Stream:
        case BIOP_StreamEvent:
        MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: Stream objects NYI\n");
        // ocpDumpStreamObject(&object->payload.stream);
        break;
        default:
        MPE_LOG(MPE_LOG_TRACE2, MPE_MOD_FILESYS, "BIOPDUMP: Unknown object type\n");
        break;

    }
}

#endif
