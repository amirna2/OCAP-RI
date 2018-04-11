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

#ifndef MPE_OBJECTCAROUSELBIOPPARSER_H
#define MPE_OBJECTCAROUSELBIOPPARSER_H

#ifdef __cplusplus
extern "C"
{
#endif

// DONT DEFINE THIS FOR PRODUCTION CODE ENABLES DUMP ROUTINES TO BE COMPILED IN
// #define OCP_DUMP_OBJECTS

#define BIOP_STRING             (0x42494f50)    // 'BIOP'
#define BIOP_VERSION_MAJOR      (1)             // DSMCC BIOP Version
#define BIOP_VERSION_MINOR      (0)             // DSMCC BIOP Version
#define BIOP_BYTE_ORDER         (0)             // Big Endian.  Always used
#define BIOP_MESSAGE_TYPE       (0)             // BIOP Message.
#define BIOP_OBJECTKIND_LENGTH  (4)             // Length of the strings which specifie types (below).
// Number of characters + null terminator

// TAP "Use" Flags
// From IEC 13818-6 Page 213.  Most are not used in the carousel
#define TAP_UNKNOWN_USE             (0x0000)
#define MPEG_TS_UP_USE              (0x0001)
#define MPEG_TS_DOWN_USE            (0x0002)
#define MPEG_ES_UP_USE              (0x0003)
#define MPEG_ES_DOWN_USE            (0x0004)
#define DOWNLOAD_CTRL_USE           (0x0005)
#define DOWNLOAD_CTRL_UP_USE        (0x0006)
#define DOWNLOAD_CTRL_DOWN_USE      (0x0007)
#define DOWNLOAD_DATA_USE           (0x0008)
#define DOWNLOAD_DATA_UP_USE        (0x0009)
#define DOWNLOAD_DATA_DOWN_USE      (0x000a)
#define STR_NPT_USE                 (0x000b)
#define STR_STATUS_AND_EVENT_USE    (0x000c)
#define STR_EVENT_USE               (0x000d)
#define STR_STATUS_USE              (0x000e)
#define RPC_USE                     (0x000f)
#define IP_USE                      (0x0010)
#define SDB_CTRL_USE                (0x0011)
#define T120_TAP1                   (0x0012)
#define T120_TAP2                   (0x0013)
#define T120_TAP3                   (0x0014)
#define T120_TAP4                   (0x0015)
#define BIOP_DELIVERY_PARA_USE      (0x0016)
#define BIOP_OBJECT_USE             (0x0017)
#define BIOP_ES_USE                 (0x0018)
#define BIOP_PROGRAM_USE            (0x0019)
#define BIOP_DNL_CTRL_USE           (0x0020)

// BIOP tags.  Taken from TAM 232r31, various locations in Annex B.
#define TAG_BIOP                    (0x49534f06)
#define TAG_LITE_OPTIONS            (0x49534f05)
#define TAG_CONNBINDER              (0x49534f40)
#define TAG_BIOP_OBJECTLOC          (0x49534f50)
#define TAG_SERVICE_LOC             (0x49534f46)

#define TAG_GIOP_CODESET            (0x00000001)

#define TAG_CODESET_ISO88591        (0x00010001)
#define TAG_CODESET_UTF8            (0x05010001)

#define TAG_CONTENT_TYPE_DESC       (0x72)

#define USER_PRIVATE                (0)

#define NSAP_ADDRESS_LENGTH         (20)

#define BIOP_FILE_STR               "fil"       // Key for a file object
#define BIOP_DIRECTORY_STR          "dir"       // Key for a directory object
#define BIOP_SERVICEGATEWAY_STR     "srg"       // Key for a service gateway object
#define BIOP_STREAM_STR             "str"       // Key for a stream object
#define BIOP_STREAMEVENT_STR        "ste"       // Key for a stream event object
// The types of objects which can be encoded in a 
typedef enum
{
    BIOP_UnknownType = 0,
    BIOP_File,
    BIOP_Directory,
    BIOP_ServiceGateway,
    BIOP_Stream,
    BIOP_StreamEvent
} ocpBIOPObjectType;

typedef struct
{
    uint32_t carouselID;
    uint16_t moduleID;
    uint32_t objectKey;
} ocpBIOPObjectLocation;

typedef struct
{
    uint16_t id;
    uint16_t assocTag;
    uint16_t use;
    // This is the standard selector data.
    // Included here because no other selector bytes are currently defined.
    uint16_t selectorType;
    uint32_t transactionID;
    uint32_t timeOut;
} ocpTAP;

typedef struct
{
    // BIOP::ObjectLocation data
    ocpBIOPObjectLocation objectLoc;

    // DSM::ConnBinder data
    // Note, we ignore all but the first tap in OC cases.
    ocpTAP tap;
} ocpBIOPProfileBody;

typedef struct
{
    uint8_t addressNSAP[NSAP_ADDRESS_LENGTH];
    uint32_t numNameComps;
    char **nameComps;
} ocpLiteOptionsProfileBody;

typedef struct
{
    uint32_t profileTag;
    union
    {
        ocpBIOPProfileBody biopProfile;
        ocpLiteOptionsProfileBody liteOptionsProfile;
    } body;
} ocpTaggedProfile;

typedef struct
{
    ocpBIOPObjectType typeID;
    ocpTaggedProfile profile;
} ocpIOR;

typedef struct
{
    uint32_t charData;
    uint32_t wCharData;
} ocpGIOPCodeSet;

typedef struct
{
    uint32_t contextTag;
    union
    {
        ocpGIOPCodeSet codeSet;
    } sc;
} ocpServiceContext;

// Structure representing a DSM::Stream::Info_T structure.

typedef struct
{
    uint32_t durationSeconds;
    uint32_t durationMicroSeconds;
    uint8_t audio;
    uint8_t video;
    uint8_t data;
} ocpStreamInfo;

typedef struct
{
    uint8_t *name;
    ocpBIOPObjectType objectType;
    ocpIOR ior;
    uint64_t fileSize; // Valid only if objectType == file
    uint8_t *contentType; // File type descriptor, if it exists.  Valid only for file.
} ocpDirEntry;

typedef struct
{
    uint64_t contentSize;
    uint8_t *contentType; // File type descriptor, if it exists
    uint32_t dataLength; // From the content length
    uint32_t dataOffset;
} ocpFileData;

// The data section of a BIOP directory message.
// 
typedef struct
{
    uint16_t numDirEntries;
    ocpDirEntry *dirEntries;
    uint32_t dataOffset;
} ocpDirData;

// The data section of a BIOP Stream message.
//
typedef struct
{
    ocpStreamInfo sInfo;
    uint8_t tapsCount;
    ocpTAP *taps;
    uint16_t eventCount;
    char **eventNames;
    uint16_t *eventIDs;
} ocpStreamEventData;

typedef struct
{
    ocpStreamInfo sInfo;
    uint8_t tapsCount;
    ocpTAP *taps;
} ocpStreamData;

// A BIOP Object.  This object can contain either a directory, a file, or a stream object
// in the payload component.  Determined by the objectType field.
typedef struct
{
    uint32_t messageSize;
    ocpBIOPObjectType objectType;
    uint32_t objectKey;
    union
    {
        ocpFileData file;
        ocpDirData dir;
        ocpStreamData stream;
        ocpStreamEventData streamEvent;
    } payload;
    uint8_t numServiceContexts;
    ocpServiceContext *serviceContexts;
    uint32_t dataOffset;
    uint32_t dataLength;
    uint32_t moduleOffset; // How far into the module is this located?
} ocpBIOPObject;

// The ServiceGatewayInfo object from a DSI.
// Only the relevant sections are preserved.
typedef struct
{
    ocpIOR ior;
} ocpGatewayInfo;

// A Datasource object.  Basically, a pair of a pointer to some structure containing the
// data, and function which will read the data from the structure.
// Frequently this will point at the module, and dcReadModuleBytes, but sometimes will be
// used to point to a memory buffer and a simple reading function.
typedef struct
{
    void *dataSource;
    uint32_t timeout;
    mpe_Error (*reader)(void *, uint32_t, uint32_t, uint32_t, uint8_t *,
            uint32_t *);
} ocpDataSource;

//------------------------------------------------------------------
// Parsing functions
//------------------------------------------------------------------


/**
 * Parse the directory entries in a directory.  Expects to be pointed at the start of the first
 * entry.  This should be called after the object is parsed via ocpParseBIOPObject below, to load
 * data into the object.
 *
 * @param source A data source object containing the data to parse, and the function to read it.
 * @param offset [in/out] A pointer to the offset to start reading at.  If a successful parse takes
 *               place, the offset will be changed to point to the byte just past the parsed data.
 * @param dir    The object to fill with the parsed data.  May contain partial data if an error 
 *                occurred during parsing.
 *
 * @return MPE_SUCCESS if the function parsed correctly, error codes otherwise.
 */
extern mpe_Error ocpParseDirEntries(ocpDataSource *source, uint32_t *offset,
        ocpDirData *dir);

/**
 * Parse a BIOP object.  Can be a directory, file, service gateway, stream, or stream event.
 * Defined in TAM 232r31, pages 273-274 (file), 275-276 (directory/service gateway),
 * 277 (service gateway), 282-283 (Stream), and 284-285 (Stream Event).
 *
 * Stream and stream event are not yet implemented.  LiteOptionsProfileBody structure is not yet
 * supported.
 *
 * @param source A data source object containing the data to parse, and the function to read it.
 * @param offset [in/out] A pointer to the offset to start reading at.  If a successful parse takes
 *               place, the offset will be changed to point to the byte just past the parsed data.
 * @param gateway The object to fill with the parsed data.  May contain partial data if an error 
 *                occurred during parsing.
 *
 * @return MPE_SUCCESS if the function parsed correctly, error codes otherwise.
 */
extern mpe_Error ocpParseBIOPObject(ocpDataSource *source, uint32_t *offset,
        ocpBIOPObject *object);

/**
 * Parse the ServiceGatewayInfo object from the private data section
 * of a DSI.  The ServiceGatewayInfo object is defined in TAM 232r31, page 272.
 *
 * @param source    A data source object containing the data to parse, and the function to read it.
 * @param offset    [in/out] A pointer to the offset to start reading at.  If a successful parse takes
 *                  place, the offset will be changed to point to the byte just past the parsed data.
 * @param gateway   The object to fill with the parsed data.  May contain partial data if an error 
 *                  occurred during parsing.
 *
 * @return MPE_SUCCESS if the function parsed correctly, error codes otherwise.
 */
extern mpe_Error ocpParseServiceGatewayLocation(ocpDataSource *source,
        uint32_t *offset, ocpGatewayInfo *gateway);

/**
 * Get the ServiceGatewayInfo object from the private data section
 * of a DSI.  The ServiceGatewayInfo object is defined in TAM 232r31, page 272. Performs the
 * gruntwork necessary to call ocpParseServiceGatewayLocation, above.
 *
 * @param buffer        A buffer of bytes containing the data in the private section of a DSI.
 * @param numBytes      The number of bytes in the structure.
 * @param gatewayLoc    Pointer to structure to fill in.
 *
 * @return MPE_SUCCESS if successful, error codes otherwise.
 */
extern mpe_Error ocpGetServiceGatewayLocation(uint8_t *buffer,
        uint32_t numBytes, ocpGatewayInfo *gatewayLoc);

/**
 * Purge a BIOP Object.  Basically frees any underlying allocated memory structures
 * in the object, and resets it's type to Unknown.  Purge should be called before
 * attempting to use a BIOP object as the target of a second parse, such as when
 * searching in a module for a given object key.
 *
 * @param object The object to purge.
 */
extern void ocpPurgeBIOPObject(ocpBIOPObject *object);

/**
 * Free a BIOP object and all it's underlying memory.
 *
 * @param object The object to be freed.
 */
extern void ocpFreeBIOPObject(ocpBIOPObject *object);

/**
 * Allocate an empty BIOP object.
 *
 * @param retObject [out] A pointer to a pointer where the object will be returned.
 *
 * @return MPE_SUCCESS if the object is successfully allocated, MPE_NOMEM if it is not.
 */
extern mpe_Error ocpAllocBIOPObject(ocpBIOPObject **retObject);

/**
 * Initialize the BIOP parser.
 *
 * @return MPE_SUCCESS if we succeeded, errors otherwise.
 */
extern mpe_Error ocpInit(void);

#ifdef OCP_DUMP_OBJECTS
/**
 * Debugging routine.  Dumps an object to the console.  Do not call unless OCP_DUMP_OBJECTS
 * is defined.
 * 
 * @param object        The object to dump.
 */
extern void ocpDumpBIOPObject(ocpBIOPObject *object);
#endif

#ifdef __cplusplus
}
#endif

#endif /* MPE_OBJECTCAROUSELBIOPPARSER_H */
