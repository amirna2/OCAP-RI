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

#include <ctype.h>
#include <stdlib.h>

#include "mpe_objectCarouselUtils.h"
#include "mpe_os.h"
#include "mpe_dbg.h"
#include "mpe_file.h"
#include "mpe_si.h"
#include "mpe_filter.h"

#include "mpeos_media.h"

#include <string.h>

// If not on GCC (ie, PowerTV at present), make Inlines go away

#ifndef __GNUC__
#define inline
#endif

// Remove/comment out this next line to disable printing of each created filter for debugging.
// #define PRINT_FILTERS

#ifdef  PRINT_FILTERS
#define OCU_PRINT_FILTER(x)     ocuPrintFilter(x)
#else
#define OCU_PRINT_FILTER(x)
#endif

// Get SI per the function, and if it's not available, wait around a bit.
#define GET_SI_WITH_WAIT(siHandle, function)                                    \
    retCode = function;                                                         \
    if (retCode == MPE_SI_NOT_AVAILABLE_YET || retCode == MPE_SI_NOT_AVAILABLE) \
    {                                                                           \
        retCode = ocuWaitForSI(siHandle);                                       \
        if (retCode == MPE_SUCCESS)                                             \
        {                                                                       \
            retCode = function;                                                 \
        }                                                                       \
    }

/*
 * Forward declarations.
 */

/*
 * Private values
 */
static const uint8_t diiMessageDescriminator[] = DII_MESSAGE_DESC;
static const uint8_t dsiMessageDescriminator[] = DSI_MESSAGE_DESC;
static const uint8_t ddbMessageDescriminator[] = DDB_MESSAGE_DESC;

#define DDB_POS_LENGTH                  (16)
#define DDB_NEG_LENGTH                  (0)

#define DSI_POS_LENGTH                  (12)
#define DSI_NEG_LENGTH                  (0)

#ifdef PTV
// If we're on PowerTV, we have to enable putting the carousel ID into the downloadID field of
// the DII for OOB Carousels..   Ugly

#define DII_POS_LENGTH                  (24)
#define DII_NEG_LENGTH                  (0)

#define DII_CHANGE_POS_LENGTH           (24)
#define DII_CHANGE_NEG_LENGTH           (16)

#else
// Standard, ie, non-PowerTV, filtering
#define DII_POS_LENGTH                  (16)
#define DII_NEG_LENGTH                  (0)

#define DII_CHANGE_POS_LENGTH           (16)
#define DII_CHANGE_NEG_LENGTH           (16)

#endif

#define DII_SELECTOR_BITS_MASK          (0x0000fffe)
#define DII_SELECTOR_VERSION_BITS_MASK  (0x3fff0000)

#define DII_SELECTOR_FULL_MASK          (DII_SELECTOR_BITS_MASK | DII_SELECTOR_VERSION_BITS_MASK)

#define DSMCC_TRANSACTION_ID_POS        (12)

#define SI_TIMEOUT                      (5 * 1000)

#define DECRYPT_SESSION_PRIORITY        (40)

// This is the amount of time the OC will wait for transitory CA conditions
//  (e.g. MMI dialogs) to resolve before giving up
#define DECRYPT_SESSION_TIMEOUT         (60 * 1000)

/*
 * Forward declarations.
 */

/*
 * Code begins here
 */

/*
 * Internal debugging functions.  Disabled for most builds.
 */

#ifdef PRINT_FILTERS
static
void
makeString(int length, uint8_t *vals, char *buffer)
{
    int i;
    char buf2[8];

    buffer[0] = '\0';
    for (i = 0; i < length; i++)
    {
        sprintf(buf2, "%02x ", ((int) vals[i]) & 0xff);
        strcat(buffer, buf2);
    }
}

static
void
ocuPrintFilter(mpe_FilterSpec *filter)
{
    char buffer[512];
    // Wouldn't it be great if there was a debugWillPrint?

    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_FILESYS, "OCU: Postive Len: %d\n", filter->pos.length);
    makeString(filter->pos.length, filter->pos.vals, buffer);
    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_FILESYS, "OCU: Positive values: %s\n", buffer);
    makeString(filter->pos.length, filter->pos.mask, buffer);
    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_FILESYS, "OCU: Positive mask  : %s\n", buffer);

    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_FILESYS, "OCU: Negative Len: %d\n", filter->neg.length);
    makeString(filter->neg.length, filter->neg.vals, buffer);
    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_FILESYS, "OCU: Negative values: %s\n", buffer);
    makeString(filter->neg.length, filter->neg.mask, buffer);
    MPE_LOG(MPE_LOG_TRACE6, MPE_MOD_FILESYS, "OCU: Negative mask  : %s\n", buffer);
}
#endif

/**
 * Get a section from the carousel.
 */
mpe_Error ocuGetSection(mpe_FilterSource *source, mpe_FilterSpec *filter,
        uint32_t timeout, mpe_FilterSectionHandle *section)
{
    uint32_t requestID = 0;
    mpe_EventQueue queue;
    mpe_Error retCode = MPE_SUCCESS;
    mpe_Event event;
    uint32_t eventData;

    // Allocate up a queue.
    // We have to do this here, just in case we have multiple outstanding calls.
    // We need to make sure we can get the right request back to the right thread.
    // Since threads don't come with their own queue, and we don't have pool of available
    // queues to pool from, we'll just cons one up here.
    // This assumes that queues are cheap to make.

    // MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OCU: Allocate ocuGetSection temporary queue\n");
    retCode = mpe_eventQueueNew(&queue, "MpeOcGetSection");
    if (retCode != MPE_SUCCESS)
    {
        return retCode;
    }

    // Set a filter
    // MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OCU: Setting ocuGetSection filter\n");
    retCode = mpe_filterSetFilter(source, filter, queue, NULL,
            MPE_SF_FILTER_PRIORITY_OC, 1, 0, &requestID);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "OCU: Filter set: %s: RequestID: %08x\n", TRUEFALSE(retCode
                    == MPE_SUCCESS), requestID);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "OCU: Filter details: %p Tuner: %d Frequency: %d\n", source,
            source->parm.p_INB.tunerId, source->parm.p_INB.freq);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCU: Unable to set filter.  Error code: %x\n", retCode);
        mpe_eventQueueDelete(queue);
        return retCode;
    }

    // Wait for the section to return.
    retCode = mpe_eventQueueWaitNext(queue, &event, (void **) &eventData, NULL,
            NULL, timeout);

    // Check to make sure we waited correctly
    if (retCode != MPE_SUCCESS)
    {
        if (retCode == MPE_ETIMEOUT)
        {
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                    "OCU: GetSection timed out\n");
        }
        else
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_FILESYS,
                    "OCU: GetSection returned error code on eventQueueWait: %04x\n",
                    retCode);
        }
        mpe_filterRelease(requestID);
        mpe_eventQueueDelete(queue);
        return retCode;
    }

    // Make sure we got the right event type
    if (event != MPE_SF_EVENT_SECTION_FOUND && event
            != MPE_SF_EVENT_LAST_SECTION_FOUND)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCU: GetSection received non-success event %04x\n", event);
        mpe_filterRelease(requestID);
        mpe_eventQueueDelete(queue);
        // TODO: Determine a better error code?
        return MPE_FS_ERROR_FAILURE;
    }

    if (eventData != requestID)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCU: Received unexpected uniquifier: %08x: Expecting %08x\n",
                eventData, requestID);
    }

    // Get the section.
    retCode = mpe_filterGetSectionHandle(requestID, 0, section);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "OCU: Got Section: %s (%04x) Section addr: %p\n", TRUEFALSE(
                    retCode == MPE_SUCCESS), retCode, section);
    mpe_filterRelease(requestID);

    // Free up the queue we were using, and leave.
    mpe_eventQueueDelete(queue);
    return retCode;
}

//
// Table definitions, per ETSI TR 101 202, v1.2.1, Page 57, Table B.1
#define WIDTH_TABLE_ID                  (8)             // 8 bits total
#define WIDTH_SECTION_SYNTAX            (1)             // 9
#define WIDTH_PRIVATE_SECTION           (1)             // 10
#define WIDTH_RESERVED_1                (2)             // 12
#define WIDTH_DSMCC_SECTION_LENGTH      (12)            // 24
#define WIDTH_TABLE_ID_EXTENSION_0      (8)             // 32
#define WIDTH_TABLE_ID_EXTENSION_1      (8)             // 8 bits total, second word
#define WIDTH_RESERVED_2                (2)             // 10
#define WIDTH_VERSION_NUMBER            (5)             // 15
#define WIDTH_CURRENT_NEXT_INDICATOR    (1)             // 16
#define WIDTH_SECTION_NUMBER            (8)             // 24
#define WIDTH_LAST_SECTION_NUMBER       (8)             // 32
#define WIDTH_DSMCC_PROTOCOL_DISC       (8)
#define WIDTH_DSMCC_DSMCC_TYPE          (8)
#define WIDTH_DSMCC_MESSAGE_ID          (16)
#define WIDTH_DSMCC_DOWNLOAD_ID         (32)

#define WIDTH_TRANSACTION_ID            (32)

#define SECTION_SYNTAX                  (1)
#define PRIVATE_SECTION                 (0)

static
inline
void ocuSetDescriminator(mpe_FilterSpec *filter, const uint8_t *descriminator)
{
    int i;
    for (i = 0; i < DSMCC_MESSAGE_DESC_LEN; i++)
    {
        filter->pos.vals[DSMCC_MESSAGE_DESC_POS + i] = descriminator[i];
        filter->pos.mask[DSMCC_MESSAGE_DESC_POS + i] = 0xff;
    }
}

/**
 * Create a filter to grab a DDB out of a transport stream.
 *
 * @param module            The module from which we want the DDB.
 * @param matchDDB          Do we want to match against a specific DDB?
 * @param ddb               The DDB within this filter (starting at 0).
 * @param version           Which version of this DDB are we looking for?
 * @param downloadID
 * @param retFilter [out]   The filter we'll be returning. [output]
 *
 * @return MPE_SUCCESS if we create the filter, error codes otherwise.
 */
mpe_Error ocuMakeDDBFilter(uint32_t moduleId, mpe_Bool matchDDB, uint32_t ddb,
        uint32_t version, uint32_t downloadID, mpe_FilterSpec **retFilter)
{
    mpe_Error retCode = MPE_SUCCESS;
    mpe_FieldPolarity ddbMatchStat = MPE_DONT_CARE;

    // Just in case.
    *retFilter = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "OCU: ocuMakeDDBFilter(%04x, %d, %d, %d)\n", moduleId, ddb,
            version, downloadID);

    retCode = mpe_filterCreateFilterSpec(DDB_POS_LENGTH, DDB_NEG_LENGTH,
            retFilter);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCU: Could not create filter spec: %04x\n", retCode);
        return retCode;
    }

    if (matchDDB)
    {
        ddbMatchStat = MPE_MATCH;
    }

    // Create the table ID mask per ETSI TR 101 202 v1.2.1 (page 18)
    // tableID              = 0x3c
    // table_id_extension   = moduleID
    // version_number       = version number % 32
    // section_number       = blockNumber % 256
    retCode = mpe_filterSetFilterSpec(*retFilter, DDB_TABLE_ID, MPE_MATCH,
            moduleId, MPE_MATCH, version, ddbMatchStat, ddb);
    // Add in the message descriminator
    ocuSetDescriminator(*retFilter, ddbMessageDescriminator);

    (*retFilter)->pos.vals[12] = (uint8_t)((downloadID >> 24) & 0xff);
    (*retFilter)->pos.vals[13] = (uint8_t)((downloadID >> 16) & 0xff);
    (*retFilter)->pos.vals[14] = (uint8_t)((downloadID >> 8) & 0xff);
    (*retFilter)->pos.vals[15] = (uint8_t)((downloadID) & 0xff);

    (*retFilter)->pos.mask[12] = (uint8_t) 0xff;
    (*retFilter)->pos.mask[13] = (uint8_t) 0xff;
    (*retFilter)->pos.mask[14] = (uint8_t) 0xff;
    (*retFilter)->pos.mask[15] = (uint8_t) 0xff;

    OCU_PRINT_FILTER(*retFilter);

    return retCode;
}

/**
 * Create a filter to find a DSI.
 *
 * @param retFilter             [out] Pointer to where to put the created filter.
 *
 * @return MPE_SUCCESS if the filter is successfully created, error codes otherwise.
 */
mpe_Error ocuMakeDSIFilter(mpe_FilterSpec **retFilter)
{
    mpe_Error retCode = MPE_SUCCESS;

    // Just in case.
    *retFilter = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OCU: ocuMakeDSIFilter\n");

    retCode = mpe_filterCreateFilterSpec(DSI_POS_LENGTH, DSI_NEG_LENGTH,
            retFilter);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCU: Could not create filter spec: %04x\n", retCode);
        return retCode;
    }

    // Create the table ID mask per ETSI TR 101 202 v1.2.1 (page 18)
    // tableID              = 0x3c
    // table_id_extension   = lower 2 bytes of transaction ID
    // version_number       = 0
    // section_number       = 0
    // last_section_number  = 0

    retCode = mpe_filterSetFilterSpec(*retFilter, DSI_TABLE_ID, MPE_DONT_CARE,
            0, MPE_MATCH, 0, MPE_MATCH, 0);
    // Add in the message descriminator
    ocuSetDescriminator(*retFilter, dsiMessageDescriminator);

    OCU_PRINT_FILTER(*retFilter);

    return retCode;
}

/**
 * Create a filter to find a particular DII.
 *
 * @param selector              The selector to look for.
 * @param retFilter             [out] Pointer to where to put the created filter.
 *
 * The next two parameters are hacks to support PowerTV's nonstandard filtering for DII's.
 * @param oob                   Is this an OOB filter?
 * @param carouselID            For OOB, what is the carouselID of this DII
 *
 * @return MPE_SUCCESS if the filter is successfully created, error codes otherwise.
 */
mpe_Error ocuMakeDIIFilter(uint32_t selector,
#ifdef PTV
        mpe_Bool oob,
        uint32_t carouselID,
#endif
        mpe_FilterSpec **retFilter)
{
    mpe_Error retCode = MPE_SUCCESS;

    // Just in case.
    *retFilter = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OCU: ocuMakeDIIFilter(%08x)\n",
            selector);

    retCode = mpe_filterCreateFilterSpec(DII_POS_LENGTH, DII_NEG_LENGTH,
            retFilter);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCU: Could not create filter spec: %04x\n", retCode);
        return retCode;
    }

    // Create the table ID mask per ETSI TR 101 202 v1.2.1 (page 18)
    // tableID              = 0x3c
    // table_id_extension   = lower 2 bytes of transaction ID
    // version_number       = 0
    // section_number       = 0
    // last_section_number  = 0

    retCode = mpe_filterSetFilterSpec(*retFilter, DII_TABLE_ID, MPE_DONT_CARE,
            0, MPE_DONT_CARE, 0, MPE_DONT_CARE, 0);

    // Add in the selector bits.
    // Have to do this explicitly, rather than above, as we don't necessarily want
    // to set ALL dii selector bits.
    selector = (selector & DII_SELECTOR_FULL_MASK);
    (*retFilter)->pos.vals[3] = (uint8_t)((selector >> 8) & 0xff);
    (*retFilter)->pos.vals[4] = (uint8_t)((selector) & 0xff);

    (*retFilter)->pos.mask[3] = (uint8_t)((DII_SELECTOR_BITS_MASK >> 8) & 0xff);
    (*retFilter)->pos.mask[4] = (uint8_t)((DII_SELECTOR_BITS_MASK) & 0xff);

    // Put the full selector we wish to change in the later bits.
    // HACK: Technically we don't need this.  We can filter on this using only the earlier
    // bits.  However, PowerTV requires that we fill in these bits on the OOB OC, so we'll
    // do it.
    (*retFilter)->pos.vals[14] = (uint8_t)((selector >> 8) & 0xff);
    (*retFilter)->pos.vals[15] = (uint8_t)((selector) & 0xff);

    (*retFilter)->pos.mask[14]
            = (uint8_t)((DII_SELECTOR_BITS_MASK >> 8) & 0xff);
    (*retFilter)->pos.mask[15] = (uint8_t)((DII_SELECTOR_BITS_MASK) & 0xff);

#ifdef PTV
    // HACK:
    // PowerTV requires that the downloadID field be filled in for the OOB filtering
    // However, this assumes/forces the adaptationHeaderLength to be 0, otherwise
    // you can't reliably filter on this field.
    // Thus, this code is non-portable.
    if (oob)
    {
        (*retFilter)->pos.vals[20] = (uint8_t) ((carouselID >> 24) & 0xff);
        (*retFilter)->pos.vals[21] = (uint8_t) ((carouselID >> 16) & 0xff);
        (*retFilter)->pos.vals[22] = (uint8_t) ((carouselID >> 8) & 0xff);
        (*retFilter)->pos.vals[23] = (uint8_t) ((carouselID) & 0xff);

        (*retFilter)->pos.mask[20] = (uint8_t) 0xff;
        (*retFilter)->pos.mask[21] = (uint8_t) 0xff;
        (*retFilter)->pos.mask[22] = (uint8_t) 0xff;
        (*retFilter)->pos.mask[23] = (uint8_t) 0xff;
    }
#endif

#ifdef NDEF
    // Right now, let's try not putting in the selector version bits.
    // PowerTV may require that we set these, but we'll just ignore them for now.
    (*retFilter)->pos.vals[12] = (uint8_t) ((selector >> 24) & 0xff);
    (*retFilter)->pos.vals[13] = (uint8_t) ((selector >> 16) & 0xff);

    (*retFilter)->pos.mask[12] = (uint8_t) ((DII_SELECTOR_VERSION_BITS_MASK >> 24) & 0xff);
    (*retFilter)->pos.mask[13] = (uint8_t) ((DII_SELECTOR_VERSION_BITS_MASK >> 16) & 0xff);
#endif

    // Add in the message descriminator
    ocuSetDescriminator(*retFilter, diiMessageDescriminator);

    OCU_PRINT_FILTER(*retFilter);

    return retCode;
}

/**
 * Create a filter to when a DII changes.
 *
 * @param selector              The selector for this DII.
 This will change when the actual version changes.
 * @param retFilter             [out] Pointer to where to put the created filter.
 *
 * The next two parameters are hacks to support PowerTV's nonstandard filtering for DII's.
 * @param oob                   Is this an OOB filter?
 * @param carouselID            For OOB, what is the carouselID of this DII.
 *
 * @return MPE_SUCCESS if the filter is successfully created, error codes otherwise.
 */
mpe_Error ocuMakeDIIChangeFilter(uint32_t selector,
#ifdef PTV
        mpe_Bool oob,
        uint32_t carouselID,
#endif
        mpe_FilterSpec **retFilter)
{
    uint32_t lSelector;
    mpe_Error retCode = MPE_SUCCESS;

    // Just in case.
    *retFilter = NULL;
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "OCU: ocuMakeDIIChangeFilter(%08x)\n", selector);

    retCode = mpe_filterCreateFilterSpec(DII_CHANGE_POS_LENGTH,
            DII_CHANGE_NEG_LENGTH, retFilter);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCU: Could not create filter spec: %04x\n", retCode);
        return retCode;
    }

    // Create the table ID mask per ETSI TR 101 202 v1.2.1 (page 18)
    // tableID              = 0x3c
    // table_id_extension   = lower 2 bytes of transaction ID
    // version_number       = 0
    // section_number       = 0
    // last_section_number  = 0
    retCode = mpe_filterSetFilterSpec(*retFilter, DII_TABLE_ID, MPE_DONT_CARE,
            0, MPE_MATCH, 0, MPE_MATCH, 0);

    // Add in the selector bits.
    lSelector = (selector & DII_SELECTOR_BITS_MASK);
    // TODO: Change the magic numbers here.
    (*retFilter)->pos.vals[3] = (uint8_t)((lSelector >> 8) & 0xff);
    (*retFilter)->pos.vals[4] = (uint8_t)((lSelector) & 0xff);

    (*retFilter)->pos.mask[3] = (uint8_t)((DII_SELECTOR_BITS_MASK >> 8) & 0xff);
    (*retFilter)->pos.mask[4] = (uint8_t)((DII_SELECTOR_BITS_MASK) & 0xff);

    // Put the full selector we wish to change in the later bits.
    (*retFilter)->pos.vals[14] = (uint8_t)((lSelector >> 8) & 0xff);
    (*retFilter)->pos.vals[15] = (uint8_t)((lSelector) & 0xff);

    (*retFilter)->pos.mask[14]
            = (uint8_t)((DII_SELECTOR_BITS_MASK >> 8) & 0xff);
    (*retFilter)->pos.mask[15] = (uint8_t)((DII_SELECTOR_BITS_MASK) & 0xff);

    // We don't want to match the version, so it goes in negative bit land.
    lSelector = (selector & DII_SELECTOR_VERSION_BITS_MASK);
    (*retFilter)->neg.vals[12] = (uint8_t)((lSelector >> 24) & 0xff);
    (*retFilter)->neg.vals[13] = (uint8_t)((lSelector >> 16) & 0xff);

    (*retFilter)->neg.mask[12] = (uint8_t)((DII_SELECTOR_VERSION_BITS_MASK
            >> 24) & 0xff);
    (*retFilter)->neg.mask[13] = (uint8_t)((DII_SELECTOR_VERSION_BITS_MASK
            >> 16) & 0xff);
#ifdef PTV
    // HACK:
    // PowerTV requires that the downloadID field be filled in for the OOB filtering
    // However, this assumes/forces the adaptationHeaderLength to be 0, otherwise
    // you can't reliably filter on this field.
    // Thus, this code is non-portable.
    if (oob)
    {
        (*retFilter)->pos.vals[20] = (uint8_t) ((carouselID >> 24) & 0xff);
        (*retFilter)->pos.vals[21] = (uint8_t) ((carouselID >> 16) & 0xff);
        (*retFilter)->pos.vals[22] = (uint8_t) ((carouselID >> 8) & 0xff);
        (*retFilter)->pos.vals[23] = (uint8_t) ((carouselID) & 0xff);

        (*retFilter)->pos.mask[20] = (uint8_t) 0xff;
        (*retFilter)->pos.mask[21] = (uint8_t) 0xff;
        (*retFilter)->pos.mask[22] = (uint8_t) 0xff;
        (*retFilter)->pos.mask[23] = (uint8_t) 0xff;
    }
#endif

    // Add in the message descriminator
    ocuSetDescriminator(*retFilter, diiMessageDescriminator);

    OCU_PRINT_FILTER(*retFilter);

    return retCode;
}

/**
 * Wait for an SI Change event to occur on the current SI Handle.
 *
 * @param siHandle      The handle we're interested in.
 */
static mpe_Error ocuWaitForSI(mpe_SiServiceHandle siHandle)
{
    mpe_Error retCode;
    mpe_EventQueue queue;
    mpe_Event event;
    uint32_t eventData;
    mpe_TimeMillis now;
    mpe_TimeMillis timeoutTime;

    retCode = mpe_eventQueueNew(&queue, "MpeOcWaitForSI");
    if (retCode != MPE_SUCCESS)
    {
        return retCode;
    }
    retCode = mpe_siRegisterQueueForSIEvents(queue);
    if (retCode != MPE_SUCCESS)
    {
        mpe_eventQueueDelete(queue);
        return retCode;
    }
    mpe_siUnLock();

    // Figure out the timeout
    mpe_timeGetMillis(&now);
    // TODO: Find a better way to specify the timeout.
    timeoutTime = now + SI_TIMEOUT;

    // Wait for an event on this service until the timeout.
    while (now < timeoutTime)
    {
        retCode = mpe_eventQueueWaitNext(queue, &event, (void **) &eventData,
                NULL, NULL, (uint32_t)(timeoutTime - now));
        if (retCode != MPE_SUCCESS)
        {
            goto CleanUp;
        }
        // TODO: Check event type????  MPE_PMT_ACQUISITION????
        if (siHandle == (mpe_SiServiceHandle) eventData)
        {
            goto CleanUp;
        }
        mpe_timeGetMillis(&now);
    }
    CleanUp: (void) mpe_siUnRegisterQueue(queue);
    (void) mpe_siLockForRead();
    // Purge the queue of any events that might be there.  It's possible that events piled up after we got the
    // we cared about, so we need to delete them, just in case mpe_eventQueueDelete() doesn't.
    while (mpe_eventQueueNext(queue, &event, (void **) &eventData, NULL, NULL)
            == MPE_SUCCESS)
        ;
    mpe_eventQueueDelete(queue);
    return retCode;
}

/**
 * Convert a carousel ID into the filter source object which can be used to
 * set filters on the stream.
 *
 * @param siHandle          The SI DB handle
 *                          to the current service.
 * @param carouselID        The carousel ID to translate.
 * @param ts                [out] Pointer to an object to fill.  Note, must be preallocated.
 *
 * @returns MPE_SUCCESS if the translation succeeds, errors otherwise.
 */
mpe_Error ocuTranslateCarouselID(mpe_SiServiceHandle siHandle,
        uint32_t carouselID, uint32_t *pid)
{
    mpe_Error retCode;

    // Get the PID of the DSI from the carousel.
    retCode = mpe_siLockForRead();
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "OCU: Could not get access to SI DB Handle to translate Carousel ID %d\n",
                carouselID);
        return retCode;
    }

    GET_SI_WITH_WAIT(siHandle, mpe_siGetPidByCarouselID(siHandle, carouselID, pid));

    mpe_siUnLock();
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "OCU: Unable to get transport stream corresponding to carouselID %08x\n",
                carouselID);
        return retCode;
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "OCU: Translated carouselID %04x to PID %04x\n", carouselID, *pid);
    return MPE_SUCCESS;
}

/**
 * Convert a association tag into the filter source object which can be used to
 * set filters on the stream.
 * This uses the algorithm specified in TAM 232r31, Annex B.3, namely:
 * 1: Check to see if the tag is in the deferred association tags link to another program.
 *    If so, repeat this algorithm in the linked program's PMT.
 * 2: Look in this PMT to determine if the association tag matches any of the association
 *    tag descriptors in the elementary streams.
 * 3: If no match, look in streamIdentifierDescriptors for all the elementary streams in this
 *    program.
 *
 * Note: The last two steps are handled by the SIDB, internally.
 *
 * @param siHandle          The SI DB handle
 *                          to the current service.
 * @param assocTag          The tag to translate.
 * @param ts                [out] Pointer to an object to fill.  Note, must be preallocated.
 *
 * @returns MPE_SUCCESS if the translation succeeds, errors otherwise.
 */
mpe_Error ocuTranslateAssociationTag(mpe_SiServiceHandle siHandle,
        uint16_t assocTag, uint32_t *pid)
{
    mpe_Error retCode;
    uint32_t progNum;
    uint32_t frequency;
    uint32_t sink;
    mpe_SiServiceHandle newSiHandle;
    mpe_SiModulationMode mode;

    // Get the PID of the DSI from the carousel.
    retCode = mpe_siLockForRead();
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "OCU: Could not get access to SI DB Handle 0x%x to translate association tag %d\n",
                siHandle, assocTag);
        return retCode;
    }

    // First, attempt to translate
    GET_SI_WITH_WAIT(siHandle, mpe_siGetProgramNumberByDeferredAssociationTag(siHandle, assocTag, &progNum));

    // Did we get a target?
    // If not, let's check in this program.
    if (retCode == MPE_SI_NOT_FOUND)
    {
        // Nope, let's see if it's an association tag.
        GET_SI_WITH_WAIT(siHandle, mpe_siGetPidByAssociationTag(siHandle, assocTag, pid));
        if (retCode == MPE_SI_NOT_FOUND)
        {
            // Still not found, how about a component tag?
            retCode = mpe_siGetPidByComponentTag(siHandle, (assocTag & 0xff),
                    pid);
        }
    }
    else if (retCode == MPE_SUCCESS)
    {
        // Get the frequency and program number for this entry
        // We only really care about the frequency.
        GET_SI_WITH_WAIT(siHandle, mpe_siGetFrequencyForServiceHandle(siHandle, &frequency));
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "OCU: Unable to get frequency for siHandle 0x%x\n",
                    siHandle);
            goto CleanUp;
        }
        GET_SI_WITH_WAIT(siHandle, mpe_siGetProgramNumberForServiceHandle(siHandle, &sink));
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "OCU: Unable to get program number for siHandle 0x%x\n",
                    siHandle);
            goto CleanUp;
        }
        GET_SI_WITH_WAIT(siHandle, mpe_siGetModulationFormatForServiceHandle(siHandle, &mode));
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "OCU: Unable to get mode for siHandle 0x%x\n", siHandle);
            goto CleanUp;
        }
        // Get the siHandle for the target program
        GET_SI_WITH_WAIT(siHandle, mpe_siGetServiceHandleByFrequencyModulationProgramNumber(frequency, mode, progNum, &newSiHandle));
        if (retCode == MPE_SI_NOT_FOUND
        		|| retCode == MPE_SI_NOT_AVAILABLE
        		|| retCode == MPE_SI_NOT_AVAILABLE_YET)
        {
    			retCode = mpe_siCreateDynamicServiceHandle(frequency, progNum, mode,
    					&newSiHandle);
        }
        mpe_siUnLock();
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "OCU: Could not Release SI Handle for: %d %d\n", frequency,
                    progNum);
            goto CleanUp;
        }
        // And translate it over there.
        retCode = ocuTranslateAssociationTag(newSiHandle, assocTag, pid);

        // Don't check the result here.  It's really checked below.  Also, the handle will be correctly
        // released below.
    }

    CleanUp: mpe_siUnLock();
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "OCU: Unable to get transport stream corresponding to Association Tag %04x\n",
                assocTag);
        return retCode;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "OCU: Translated Association Tag %04x to PID %04x\n", assocTag,
            *pid);
    return MPE_SUCCESS;
}

/**
 * Translate an association tag into a program number via a deferred association tag descriptor.
 * This is used to translate a BIOP_PROGRAM_USE TAP into a program number.  Will only return success
 * if a deferred association tag exists with the appropriate tag.  Does not confirm that the target
 * program exists.
 *
 * @param siHandle          The SI DB handle
 * @param assocTag          The tag to translate.
 * @param program           [out] The program number of the target program.
 *
 * @returns MPE_SUCCESS if a deferred association tag is found and translated, error codes otherwise.
 *
 */
mpe_Error ocuGetProgramFromAssociationTag(mpe_SiServiceHandle siHandle,
        uint16_t assocTag, uint32_t *program)
{
    mpe_Error retCode;

    // Get the PID of the DSI from the carousel.
    retCode = mpe_siLockForRead();
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCU: Could not get access to SI DB Handle\n");
        return retCode;
    }

    GET_SI_WITH_WAIT(siHandle, mpe_siGetProgramNumberByDeferredAssociationTag(siHandle, assocTag, program));

    mpe_siUnLock();
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "OCU: Unable to get Program number corresponding to Association Tag %04x\n",
                assocTag);
        return retCode;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "OCU: Translated Association Tag %04x to Program %04x\n", assocTag,
            *program);
    return retCode;
}

/**
 * Get the frequency and program number for a given si handle.
 *
 * @param siHandle          The SI DB handle.
 * @param params         [out]
 *
 * @returns MPE_SUCCESS if the translation succeeds, errors otherwise.
 */
mpe_Error ocuSetTuningParams(mpe_SiServiceHandle siHandle,
        mpe_OcTuningParams *params)
{
    mpe_Error retCode;
    uint32_t frequency;
    uint32_t program;
    uint32_t appId;

    // Get the SI Handle
    retCode = mpe_siLockForRead();
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCU: Could not get access to SI DB Handle\n");
        return retCode;
    }

    // Get the frequency & program number
    GET_SI_WITH_WAIT(siHandle, mpe_siGetFrequencyForServiceHandle(siHandle, &frequency));

    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCU: Unable to get Frequency for SI Handle\n");
        goto CleanUp;
    }

    GET_SI_WITH_WAIT(siHandle, mpe_siGetProgramNumberForServiceHandle(siHandle, &program));
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCU: Unable to get Program Number for SI Handle\n");
        goto CleanUp;
    }
    // Check the frequency.  If it was -1, we're either OOB or DSG
    if (frequency == (uint32_t) - 1) // TODO: What's the correct value?
    {
        GET_SI_WITH_WAIT(siHandle, mpe_siGetAppIdForServiceHandle(siHandle, &appId));
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "OCU: Unable to get AppID for SI Handle\n");
            goto CleanUp;
        }
        if (appId > 0)
        {
            params->transport = MPE_OC_DSG;
            params->t.dsg.appId = appId;
            params->t.dsg.prog = program;
        }
        else
        {
            params->transport = MPE_OC_OOB;
            params->t.oob.prog = program;
        }
    }
    else
    {
        // Must be IB.
        params->transport = MPE_OC_IB;
        params->t.ib.frequency = frequency;
        params->t.ib.prog = program;
    }

    CleanUp:
    // Release the lock
    mpe_siUnLock();
    return retCode;
}

/**
 * Get the Source ID for a given si handle.
 *
 * @param siHandle          The SI DB handle.
 * @param sourceID          [out] Pointer to where to put the Source ID.
 *
 * @returns MPE_SUCCESS if the translation succeeds, errors otherwise.
 */
mpe_Error ocuGetSourceID(mpe_SiServiceHandle siHandle, uint32_t *sourceID)
{
    mpe_Error retCode;

    retCode = mpe_siLockForRead();
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCU: Could not get access to SI DB Handle\n");
        return retCode;
    }

    // Get the SI Handle
    GET_SI_WITH_WAIT(siHandle, mpe_siGetSourceIdForServiceHandle(siHandle, sourceID));
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "OCU: Could not get SI DB Handle based on SI handle %08x for SourceID lookup\n",
                siHandle);
        // Release the lock
        mpe_siUnLock();

        return retCode;
    }

    // Release the lock
    mpe_siUnLock();

    // See 'ya
    //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "OCU: Got SourceID: %d\n", *sourceID);
    return MPE_SUCCESS;
}

/**
 * Get the app ID for a given si handle.
 *
 * @param siHandle          The SI DB handle.
 * @param appID          [out] Pointer to where to put the appID.
 *
 * @returns MPE_SUCCESS if the translation succeeds, errors otherwise.
 */
mpe_Error ocuGetAppID(mpe_SiServiceHandle siHandle, uint32_t *appID)
{
    mpe_Error retCode;

    retCode = mpe_siLockForRead();
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCU: Could not get access to SI DB Handle\n");
        return retCode;
    }

    // Get the SI Handle
    GET_SI_WITH_WAIT(siHandle, mpe_siGetAppIdForServiceHandle(siHandle, appID));
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "OCU: Could not get SI DB Handle based on SI handle %08x for appID lookup\n",
                siHandle);
    }

    // Release the lock
    mpe_siUnLock();

    return retCode;
}

/**
 * Convert a string from UTF-8 encoding to ISO 8859-1 (standard Latin-1).
 *
 * @param in    The input string.
 * @param out   [out] The output string to fill.  Must be at long enough to hold the entire string.
 *
 * @return MPE_SUCCESS if the string is sucessfully translated, MPE_EINVAL if it contains characters
 *                     which are not in ISO 8859-1.
 */
mpe_Error ocuUtf8ToIso88591(uint8_t *in, uint8_t *out)
{
    uint8_t highBits = 0;
    uint8_t inChar = *in;

    while (inChar != '\0')
    {
        if (inChar < 0x80)
        {
            *out = inChar;
            out++;
        }
        else if ((inChar == 0xc2) || (inChar == 0xc3))
        {
            highBits = (inChar & 0x03) << 6;
        }
        else if ((inChar & 0x80) == 0x80)
        {
            *out = (highBits | (inChar & 0x3f));
            out++;
        }
        else
        {
            /* Not a ISO 8859-1 character */
            return MPE_EINVAL;
        }
        // Move along to the next character
        in++;
        inChar = *in;
    }
    *out = '\0';

    return MPE_SUCCESS;
}

/**
 * Translate a string from ISO 8859-1 to UTF-8.
 *
 * @param in        The input string to translate.
 * @param out       [out] Where to put the translated string.
 *
 * @return MPE_SUCCESS
 */
mpe_Error ocuIso88591ToUtf8(uint8_t *in, uint8_t *out)
{
    uint8_t inChar = *in;

    while (inChar != '\0')
    {
        if ((inChar & 0x80) == 0x00)
        {
            *out++ = inChar;
        }
        else
        {
            *out++ = 0xc0 | (inChar >> 6);
            *out++ = 0x80 | (inChar & 0x3f);
        }
        in++;
        inChar = *in;
    }
    *out = '\0';
    return MPE_SUCCESS;
}

/**
 * Get an environment variable, if it exists, otherwise return the default.
 * NOTE: Assume that all env variables are integers.
 *       Hide the fact that some are "TRUE"/"FALSE" booleans within this function.
 *
 * @param name          The name of the variable to read.
 * @param defValue      The default value to use if the variable is not
 *                      in the environment.
 *
 * @returns The value, either from the environment, or from the default
 * provided.
 */
uint32_t ocuGetEnv(char *name, uint32_t defValue)
{
    const char *envVar;
    int envNum;

    envVar = mpeos_envGet(name);
    if (envVar != NULL)
    {
        if (isdigit(envVar[0]))
        {
            // if this env variable is a number, convert the string into an integer
            envNum = atoi(envVar);
        }
        else
        {
            // else assume this is a boolean, convert "TRUE"/"FALSE" to an integer '1'/'0'
            envNum = (stricmp(envVar, "TRUE") == 0);
        }

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "OCU: Getting environment variable %s with value %d\n", name,
                envNum);
        return (envNum);
    }
    else
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "OCU: Setting environment variable %s with default value %d\n",
                name, defValue);
        return defValue;
    }
}

/**
 * Set up a mpe_FilterSource appropriately for a filtering.
 * Note: Does not yet set the PID.  That's handled separately.
 *
 * @param dc        The DC we're filtering on.
 * @param stream    The mpe_FilterSource we wish to fill in.
 */
void ocuSetStreamParams(mpe_OcTuningParams *transport, mpe_FilterSource *stream)
{
    // Set up for filtering on the appropriate carousel type.

    switch (transport->transport)
    {
    case MPE_OC_IB:
        stream->sourceType = MPE_FILTER_SOURCE_INB;
        stream->parm.p_INB.freq = transport->t.ib.frequency;
        stream->parm.p_INB.tunerId = transport->t.ib.tunerId;
        stream->parm.p_INB.tsId = 1;
        break;
    case MPE_OC_OOB:
        stream->sourceType = MPE_FILTER_SOURCE_OOB;
        stream->parm.p_OOB.tsId = 1;
        break;
    case MPE_OC_DSG:
        stream->sourceType = MPE_FILTER_SOURCE_DSG_APPID;
        stream->parm.p_DSGA.appId = transport->t.dsg.appId;
        break;
    default:
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "OCU: Unexpected value for transport type: %d\n",
                transport->transport);
        break;
    }
}

/**
 * Set the tuner ID.  Only valid for Inband streams, nop otherwise.
 * @param transport     The Tuning Parameters block to set the tuner in.
 *
 * @return MPE_SUCCESS if the tuner could be set (or not IB), false if the frequency was
 *         not found.
 */
mpe_Error ocuSetTuner(mpe_OcTuningParams *transport)
{
    mpe_Error retCode = MPE_SUCCESS;
    if (transport->transport != MPE_OC_IB)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "OCU: Not setting tuner.  Not Inband");
    }
    else
    {
        retCode = mpeos_mediaFrequencyToTuner(transport->t.ib.frequency,
                &transport->t.ib.tunerId);
    }
    return retCode;
}


/**
 * Start a CA decrypt session. When MPE_SUCCESS is returned, caHandle will
 * be 0 if CA authorization is not required. When non-zero, changes in the
 * CA session will be signaled via the mpe_EventQueue.
 *
 * @param tunerId       The tuner to be used for mounting the carousel
 * @param siHandle      The SI handle for the associated Service
 * @param pid           The PID carrying the carousel
 * @param queue         Queue to register for CA session updates
 * @param caHandle      Pointer to store the created CA session handle
 *
 * @return MPE_SUCCESS when the session is successfully created or no
 *         session is required or an appopriate error if the session
 *         cannot be created
 */
mpe_Error ocuStartCASession( uint32_t tunerId,
                             mpe_SiServiceHandle siHandle,
                             uint32_t pid,
                             mpe_EventQueue queue,
                             mpe_PODDecryptSessionHandle * caHandle )
{
    mpe_Error retCode = MPE_SUCCESS;
    mpe_MediaPID pids[1];
    mpe_PodDecryptRequestParams decryptParams;

    decryptParams.handleType = MPE_POD_SERVICE_DETAILS_HANDLE;
    decryptParams.handle = siHandle;
    decryptParams.tunerId = tunerId;
    decryptParams.numPids = 1;
    pids[0].pid = pid;
    pids[0].pidType = MPE_SI_ELEM_DSM_CC_SECTIONS;
    decryptParams.pids = pids;
    decryptParams.priority = DECRYPT_SESSION_PRIORITY;
    decryptParams.mmiEnable = TRUE;

    retCode = mpe_podStartDecrypt(&decryptParams, queue, NULL, caHandle);

    return retCode;
} // END ocuStartCASession()


/**
 * Wait on the designated queue for a terminal CA condition (fully
 * authorized or unauthorized) or timeout.
 *
 * @param caHandle      Handle to the CA decrypt session
 * @param queue         The queue associated with the decrypt session
 * @param status        Pointer to store the terminal CA condition/event
 *                      (only set when MPE_SUCCESS is returned)
 *
 * @return MPE_SUCCESS if a terminal event is received in the timeout
 *         period or MPE_ETIMEOUT if no terminal event is received in the
 *         timeout period
 */
mpe_Error ocuWaitForCASessionInit( mpe_PODDecryptSessionHandle caHandle,
                                   mpe_EventQueue queue,
                                   mpe_PodDecryptSessionEvent * status)
{
    mpe_Error retCode = MPE_SUCCESS;
    mpe_Event event;
    uint32_t eventData;
    mpe_Bool initCompleted = FALSE;
    uint32_t timeout = DECRYPT_SESSION_TIMEOUT;
    mpe_TimeMillis timeAtStart, now;

    if (caHandle == 0)
    {
        return MPE_SUCCESS;
    }

    mpe_timeGetMillis(&timeAtStart);
    now = timeAtStart;

    do
    {
        event = 0;
        eventData = 0;
        timeout = DECRYPT_SESSION_TIMEOUT - (now-timeAtStart);
        // Wait for an event signaling session startup or timeout
        MPE_LOG( MPE_LOG_DEBUG,
                 MPE_MOD_FILESYS,
                 "OCU: Waiting %d ms for CA session startup...\n",
                 timeout );

        retCode = mpe_eventQueueWaitNext( queue, &event, (void **) &eventData,
                                          NULL, NULL, timeout);

        if (retCode != MPE_SUCCESS)
        {
            if (retCode == MPE_ETIMEOUT)
            {
                MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILESYS,
                         "OCU: CA session setup timed out\n" );
                initCompleted = TRUE;
            }
            else
            {
                MPE_LOG( MPE_LOG_ERROR,
                         MPE_MOD_FILESYS,
                         "OCU: ocuWaitForCASessionInit returned error code on eventQueueWait: %04x\n",
                         retCode );
            }
        }
        else
        {
            MPE_LOG( MPE_LOG_DEBUG,
                     MPE_MOD_FILESYS,
                     "OCU: Received event 0x%x (data 0x%08x) waiting for CA session startup\n",
                     event, eventData );

            switch (event)
            {
                case MPE_POD_DECRYPT_EVENT_CANNOT_DESCRAMBLE_ENTITLEMENT:
                case MPE_POD_DECRYPT_EVENT_CANNOT_DESCRAMBLE_RESOURCES:
                case MPE_POD_DECRYPT_EVENT_FULLY_AUTHORIZED:
                case MPE_POD_DECRYPT_EVENT_SESSION_SHUTDOWN:
                case MPE_POD_DECRYPT_EVENT_POD_REMOVED:
                case MPE_POD_DECRYPT_EVENT_RESOURCE_LOST:
                {
                    // None of these are transitory - consider ourselves done
                    *status = event;
                    initCompleted = TRUE;
                    break;
                }

                case MPE_POD_DECRYPT_EVENT_MMI_PURCHASE_DIALOG:
                case MPE_POD_DECRYPT_EVENT_MMI_TECHNICAL_DIALOG:
                {
                    // These should be transitory - give them a chance to resolve
                    // Continue waiting for the MMI to resolve...
                    break;
                }

                default:
                {
                    MPE_LOG( MPE_LOG_WARN,
                             MPE_MOD_FILESYS,
                             "OCU: ocuWaitForCASessionInit received unexpected event: 0x%x\n",
                             event );
                    // Continue
                }
            }
        }
        mpe_timeGetMillis(&now);
    } while (!initCompleted);

    return retCode;
} // END ocuWaitForCASessionInit()

/**
 * Issue a close on the CA session and wait for the shutdown acknowledgement
 * or timeout.
 *
 * @param caHandle      Handle to the CA decrypt session
 * @param queue         The queue associated with the decrypt session
 *
 * @return MPE_SUCCESS if the shutdown event is received in the timeout
 *         period or MPE_ETIMEOUT if no terminal event is received in the
 *         timeout period
 */
mpe_Error ocuCloseCASession( mpe_PODDecryptSessionHandle caHandle,
                             mpe_EventQueue queue )
{
    mpe_Error retCode = MPE_SUCCESS;
    mpe_Event event;
    uint32_t eventData;
    mpe_Bool closeCompleted = FALSE;
    uint32_t timeout = DECRYPT_SESSION_TIMEOUT;
    mpe_TimeMillis timeAtStart, now;

    if (caHandle == 0)
    {
        return MPE_SUCCESS;
    }

    mpe_timeGetMillis(&timeAtStart);
    now = timeAtStart;

    retCode = mpe_podStopDecrypt(caHandle);

    do
    {
        event = 0;
        eventData = 0;
        timeout = DECRYPT_SESSION_TIMEOUT - (now-timeAtStart);
        // Wait for an event signaling session startup or timeout
        MPE_LOG( MPE_LOG_DEBUG,
                 MPE_MOD_FILESYS,
                 "OCU: Waiting %d ms for CA session shutdown...\n",
                 timeout );

        retCode = mpe_eventQueueWaitNext( queue, &event, (void **) &eventData,
                                          NULL, NULL, timeout);

        if (retCode != MPE_SUCCESS)
        {
            if (retCode == MPE_ETIMEOUT)
            {
                MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILESYS,
                         "OCU: CA session shutdown timed out\n" );
                closeCompleted = TRUE;
            }
            else
            {
                MPE_LOG( MPE_LOG_ERROR,
                         MPE_MOD_FILESYS,
                         "OCU: ocuCloseCASession returned error code on eventQueueWait: %04x\n",
                         retCode );
            }
        }
        else
        {
            MPE_LOG( MPE_LOG_DEBUG,
                     MPE_MOD_FILESYS,
                     "OCU: Received event 0x%x (data 0x%08x) waiting for CA session shutdown\n",
                     event, eventData );

            switch (event)
            {
                case MPE_POD_DECRYPT_EVENT_SESSION_SHUTDOWN:
                {
                    // By contract, no more events are sent for the associated
                    //  CA session after this event
                    closeCompleted = TRUE;
                    break;
                }

                case MPE_POD_DECRYPT_EVENT_CANNOT_DESCRAMBLE_ENTITLEMENT:
                case MPE_POD_DECRYPT_EVENT_CANNOT_DESCRAMBLE_RESOURCES:
                case MPE_POD_DECRYPT_EVENT_FULLY_AUTHORIZED:
                case MPE_POD_DECRYPT_EVENT_MMI_PURCHASE_DIALOG:
                case MPE_POD_DECRYPT_EVENT_MMI_TECHNICAL_DIALOG:
                case MPE_POD_DECRYPT_EVENT_POD_REMOVED:
                case MPE_POD_DECRYPT_EVENT_RESOURCE_LOST:
                {
                    // Continue waiting for the shutdown event...
                    break;
                }

                default:
                {
                    MPE_LOG( MPE_LOG_WARN,
                             MPE_MOD_FILESYS,
                             "OCU: ocuCloseCASession received unexpected event: 0x%x\n",
                             event );
                    // Continue
                }
            }
        }
        mpe_timeGetMillis(&now);
    } while (!closeCompleted);

    return retCode;
} // END ocuCloseCASession()
