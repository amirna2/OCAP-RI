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

#include "test_filter_sectionFiltering_include.h"
#include "sectionFiltering_parameters.h"
#include <test_utils.h>
#include <test_media.h>
#include <mpe_os.h>

/** \file
 *
 *  \brief General untilities used by multiple test source files
 *
 * This file contains shared convenience routines for the section filtering
 * API testing source files. Specifically, these general classes of routines.\n
 * -# PAT and PMT handling routines\n
 * -# Value-to-name translation routines\n
 * -# Section filter database routines\n
 *
 * \author Ric Yeates, Vidiom Systems Corp.
 *
 */

/**
 * terminate the section filter database event thread
 *
 * This event ID is used to cause the section filter database event thread to
 * terminate. It's sent implicitly by sfTerm()
 * 
 * \see sfTerm
 */
#define ETHREAD_TERMINATE 1

static void dump(const char *indent, const void *_data, uint32_t count);

/****************************************************************************
 *
 *  PATSectionFilter()
 *
 ***************************************************************************/
/**
 * \brief get a filter for a PAT section
 *
 * Given an initialized mpe_FilterSource, create the filter specification
 * for the next PAT on the source.
 *
 * \param source pointer to the source, generally recently returned by an OS
 * tuning function
 * \param pSpec pointer to pointer to fill with a pointer to an allocated
 * mpe_FilterSpec for the next PAT from the source.
 *
 * \return MPE_SUCCESS if everything went well.
 *
 */
mpe_Error PATSectionFilter(mpe_FilterSource *source, mpe_FilterSpec **pSpec)
{
    mpe_Error err;
    static const uint8_t patMask[] =
    { 0xff };
    static const uint8_t patVals[] =
    { 0x00 };

    // PATs are on PID 0
    source->pid = 0x0000;

    err = memAllocP(MPE_MEM_TEST, sizeof(**pSpec), (void **) pSpec);
    if (err != MPE_SUCCESS)
    {
        return err;
    }

    // PATs are table 0
    (*pSpec)->pos.length = sizeof(patMask);
    (*pSpec)->pos.mask = (uint8_t *) patMask;
    (*pSpec)->pos.vals = (uint8_t *) patVals;

    (*pSpec)->neg.length = 0;

    return MPE_SUCCESS;
}

/****************************************************************************
 *
 *  PMTSectionFilter()
 *
 ***************************************************************************/
/**
 * \brief get a filter for a PMT section
 *
 * Given an initialized mpe_FilterSource (especially the PID) and a program
 * number, create the filter specification for the next PMT on the source.
 * This function cannot be called more than once for each filterSetFilter()
 * due to its use of static data.
 *
 * \param source pointer to the source, generally recently returned by an OS
 * tuning function
 * \param program_number the program number for which to get the PMT for
 * \param pSpec pointer to pointer to fill with a pointer to an allocated
 * mpe_FilterSpec for the next PMT from the source.
 *
 * \return MPE_SUCCESS if everything went well.
 *
 */
mpe_Error PMTSectionFilter(mpe_FilterSource *source, uint16_t program_number,
        mpe_FilterSpec **pSpec)
{
    mpe_Error err;
    static const uint8_t pmtMask[5] =
    { 0xff, 0x00, 0x00, 0xff, 0xff };
    static uint8_t pmtVals[5] =
    { 0x02, 0x00, 0x00 };

    err = memAllocP(MPE_MEM_TEST, sizeof(**pSpec), (void **) pSpec);
    if (err != MPE_SUCCESS)
    {
        return err;
    }

    // PMTs are table 2
    (*pSpec)->pos.length = sizeof(pmtMask);
    (*pSpec)->pos.mask = (uint8_t *) pmtMask;
    (*pSpec)->pos.vals = (uint8_t *) pmtVals;
    pmtVals[3] = program_number >> 8;
    pmtVals[4] = program_number & 0xff;

    (*pSpec)->neg.length = 0;

    return MPE_SUCCESS;
}

typedef struct bitStreamState_t bitStreamState_t;

struct bitStreamState_t
{
    uint8_t *data; // current byte we're on
    uint32_t bitOffset; // current bit we're on (0 = MSB, 7 = LSB)
    uint32_t bitsLeft; // total number of bits left in stream
    uint32_t bitsConsumed; // total number of bits taken out of stream
    mpe_Bool failure; // has there been any failures?
};

static bitStreamState_t state;

/****************************************************************************
 *
 *  bitStreamInit()
 *
 ***************************************************************************/
/**
 * \brief initialize the bitstream
 *
 * Start a bit stream given the data and the size of the data.
 *
 * \param data pointer to the byte array
 * \param size number of bytes in the array
 *
 * \note You can only init one bitstream at a time
 */
static void bitStreamInit(uint8_t *data, uint32_t size)
{
    state.data = data;
    state.bitOffset = 0;
    state.bitsLeft = size * 8;
    state.failure = FALSE;
    state.bitsConsumed = 0;
}

/****************************************************************************
 *
 *  bitStreamGet()
 *
 ***************************************************************************/
/**
 * \brief get bits from a bit stream
 *
 * Extract the next set of bits from a bit stream. If the number of bits to
 * extract is greater than 32, the return value is the last 32 bits extracted.
 *
 * \param bits the number of bits to extract
 *
 * \return value of the bits extracted. Returns 0 if the bit stream runs short.
 * A status flag is set to indicate this failure.
 *
 * \see bitStreamGetInfo, bitStreamInit
 *
 */
static uint32_t bitStreamGet(uint32_t bits)
{
    uint32_t rem;
    uint32_t ret;

    if (bits > state.bitsLeft || state.failure)
    {
        state.failure = TRUE;
        return 0;
    }

    // account for bits we're consuming
    state.bitsLeft -= bits;
    state.bitsConsumed += bits;

    ret = 0;

    // get any odd bits to an 8-bit boundary
    rem = state.bitOffset % 8;
    if (rem != 0)
    {
        rem = 8 - rem;

        // mask off the least significant 'rem' bits
        ret = *state.data & ((1 << rem) - 1);

        // too many for request?
        if (bits < rem)
        {
            ret >>= rem - bits; // toss the bits we don't need
            state.bitOffset += bits; // advance the current offset
            return ret; // and we're out...
        }

        bits -= rem;
        ++state.data;
        state.bitOffset = 0;
    }

    // get any whole bytes */
    while (bits >= 8)
    {
        ret <<= 8;
        ret |= *state.data++;
        bits -= 8;
    }

    // get any bits left over
    if (bits)
    {
        ret <<= bits;
        ret |= *state.data >> (8 - bits);
        state.bitOffset = bits;
    }

    return ret;
}

/****************************************************************************
 *
 *  bitStreamGetBytes()
 *
 ***************************************************************************/
/**
 * \brief get bytes from a bit stream
 *
 * Extract the next bytes from a bit stream. If the current bit offset is not
 * 0, a failure is noted and 0's are returned.
 *
 * \param buffer place to write the bytes extracted
 * \param bytes the number of bytes to extract
 *
 * \return value of the bits extracted. Returns 0 if the bit stream runs short.
 * A status flag is set to indicate this failure.
 *
 * \see bitStreamGet, bitStreamGetInfo
 */
static void bitStreamGetBytes(uint8_t *buffer, uint32_t bytes)
{
    uint32_t bits = bytes * 8;

    if (state.bitOffset != 0 || bits > state.bitsLeft || state.failure)
    {
        state.failure = TRUE;
        memset(buffer, 0, bytes);
        return;
    }

    // account for bits we're consuming
    state.bitsLeft -= bits;
    state.bitsConsumed += bits;

    // copy bytes and move data pointer, bitOffset remains 0
    memcpy(buffer, state.data, bytes);
    state.data += bytes;
}

/****************************************************************************
 *
 *  bitStreamGetInfo()
 *
 ***************************************************************************/
/**
 * \brief get information about a bitstream
 *
 * Get various pieces of information related to the current state of the bit stream. Any
 * of the parameters can be passed as NULL if that information is not required.
 *
 * \param bitsConsumed location to fill with the number of bits consumed so far
 * \param bitsLeft location to fill with the number of bits remaining in the bit stream
 * \param failure location to fill with boolean indicating whether or not a request for
 * bits or bytes was ever invalid.
 *
 * \see bitStreamGet, bitStreamGetBytes
 */
static void bitStreamGetInfo(uint32_t *bitsConsumed, uint32_t *bitsLeft,
        mpe_Bool *failure)
{
    if (bitsConsumed)
        *bitsConsumed = state.bitsConsumed;
    if (bitsLeft)
        *bitsLeft = state.bitsLeft;
    if (failure)
        *failure = state.failure;
}

/****************************************************************************
 *
 *  bitStreamTerm()
 *
 ***************************************************************************/
/**
 * \brief terminate the use of the bit stream
 *
 * Terminates the use of the bit stream. Calling this function optional.
 *
 */
static void bitStreamTerm(void)
{
    memset(&state, 0, sizeof(state));
    state.failure = TRUE;
}

/****************************************************************************
 *
 *  PATSectionParse()
 *
 ***************************************************************************/
/**
 * \brief given a section handle, parse the PAT out of it
 *
 * PATSectionParse() returns a C structure for the PAT information from a
 * filtered section.
 *
 * \param sect handle for section to parse from
 * \param returnPAT pointer to pointer to PAT_t to fill with parsed PAT
 *
 * \return any error encountered parsing the PAT,
 * MPE_SF_ERROR_INVALID_SECTION_HANDLE if the section does not appear to be a
 * PAT
 * 
 * \note CRC is not checked
 *
 */
mpe_Error PATSectionParse(mpe_FilterSectionHandle sect, PAT_t **returnPAT)
{
    uint8_t *sectData = NULL;
    uint32_t sectSize;
    uint32_t sectRead;
    mpe_Error err;
    PAT_t *pat = NULL;
    ProgramPIDMap_t *add, *last;
    uint32_t length_start; // consumed bits when section_length started
    uint32_t consumed;
    mpe_Bool failure;
    uint32_t n;

    err = filterGetSectionSize(sect, &sectSize);
    if (err != MPE_SUCCESS)
        goto return_err;

    if (sectSize < 4)
    {
        err = MPE_SF_ERROR_INVALID_SECTION_HANDLE;
        goto return_err;
    }

    // strip off CRC
    sectSize -= 4;

    err = memAllocP(MPE_MEM_TEST, sectSize, (void **) &sectData);
    if (err != MPE_SUCCESS)
        goto return_err;

    err = filterSectionRead(sect, 0, sectSize, 0, sectData, &sectRead);
    if (err != MPE_SUCCESS)
        goto return_err;

    if (sectRead != sectSize)
    {
        err = MPE_SF_ERROR_INVALID_SECTION_HANDLE;
        goto return_err;
    }

    err = memAllocP(MPE_MEM_TEST, sizeof(*pat), (void **) &pat);
    if (err != MPE_SUCCESS)
        goto return_err;

    pat->map = NULL;

    bitStreamInit(sectData, sectSize);

    pat->header.table_id = bitStreamGet(8);
    pat->header.section_syntax_indicator = bitStreamGet(1);
    (void) bitStreamGet(1 + 2);
    pat->header.section_length = bitStreamGet(12);

    bitStreamGetInfo(&length_start, NULL, NULL);

    pat->transport_stream_id = bitStreamGet(16);
    (void) bitStreamGet(2);
    pat->version_number = bitStreamGet(5);
    pat->current_next_indicator = bitStreamGet(1);
    pat->section_number = bitStreamGet(8);
    pat->last_section_number = bitStreamGet(8);

    bitStreamGetInfo(&consumed, NULL, NULL);

    //
    // number of mappings = (remaining length - CRC) / size of each mapping
    //
    n = ((pat->header.section_length - 4) - ((consumed - length_start) / 8))
            / ((16 + 3 + 13) / 8);
    last = NULL;
    while (n--)
    {
        err = memAllocP(MPE_MEM_TEST, sizeof(*pat->map), (void **) &add);
        if (err != MPE_SUCCESS)
            goto return_err;

        add->program_number = bitStreamGet(16);
        (void) bitStreamGet(3);
        add->pid = bitStreamGet(13);
        add->next = NULL;
        if (last)
            last->next = add;
        else
            pat->map = add;
        last = add;
    }

    // did we run out of bits along the way?
    bitStreamGetInfo(NULL, NULL, &failure);
    if (failure)
    {
        err = MPE_SF_ERROR_INVALID_SECTION_HANDLE;
        goto return_err;
    }

    bitStreamTerm();

    *returnPAT = pat;

    return MPE_SUCCESS;

    return_err: if (pat)
        PATSectionFree(pat);
    if (sectData)
        memFreeP(MPE_MEM_TEST, sectData);
    return err;
}

/****************************************************************************
 *
 *  PATSectionDump()
 *
 ***************************************************************************/
/**
 * \brief print a PAT
 *
 * Given a C structure for a PAT, prints everything via TRACE().
 *
 * \param pat pointer to the PAT
 *
 */
void PATSectionDump(PAT_t *pat)
{
    ProgramPIDMap_t *scan;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "PAT: (table_id = %u, section_length = %u)\n",
            pat->header.table_id, pat->header.section_length);
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "    transport_stream_id = %u (0x%x)\n",
            pat->transport_stream_id, pat->transport_stream_id);
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "    version_number = %u (%s)\n",
            pat->version_number, pat->current_next_indicator ? "current"
                    : "next");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "    section = %u, last = %u\n",
            pat->section_number, pat->last_section_number);
    for (scan = pat->map; scan; scan = scan->next)
    {
        TRACE(
                MPE_LOG_INFO,
                MPE_MOD_TEST,
                "    mapping - progam_number = %u (0x%x) ==> PID = %u (0x%x)\n",
                scan->program_number, scan->program_number, scan->pid,
                scan->pid);
    }
}

/****************************************************************************
 *
 *  PATSectionFree()
 *
 ***************************************************************************/
/**
 * \brief release a PAT section structure to the OS
 *
 * Frees all memory allocated to express a PAT as a C structure
 *
 * \param pat PAT structure to free
 *
 */
void PATSectionFree(PAT_t *pat)
{
    ProgramPIDMap_t *next;
    for (; pat->map; pat->map = next)
    {
        next = pat->map->next;
        memFreeP(MPE_MEM_TEST, pat->map);
    }
    memFreeP(MPE_MEM_TEST, pat);
}

/****************************************************************************
 *
 *  PMTSectionParse()
 *
 ***************************************************************************/
/**
 * \brief given a section handle, parse the PMT out of it
 *
 * PMTSectionParse() returns a C structure for the PMT information from a
 * filtered section.
 *
 * \param sect handle for section to parse from
 * \param returnPMT pointer to pointer to PMT_t to fill with parsed PMT
 *
 * \return any error encountered parsing the PMT,
 * MPE_SF_ERROR_INVALID_SECTION_HANDLE if the section does not appear to be a
 * PMT
 * 
 * \note CRC is not checked
 *
 */
mpe_Error PMTSectionParse(mpe_FilterSectionHandle sect, PMT_t **returnPMT)
{
    uint8_t *sectData = NULL;
    uint32_t sectSize;
    uint32_t sectRead;
    mpe_Error err;
    PMT_t *pmt = NULL;
    StreamInfo_t *add, *last;
    uint32_t length_start; // consumed bits when section_length started
    uint32_t consumed;
    mpe_Bool failure;
    uint32_t bytesLeft;

    err = filterGetSectionSize(sect, &sectSize);
    if (err != MPE_SUCCESS)
        goto return_err;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "PMTSectionParse: sectSize = %u\n",
            sectSize);

    if (sectSize < 4)
    {
        err = MPE_SF_ERROR_INVALID_SECTION_HANDLE;
        goto return_err;
    }

    // strip off CRC
    sectSize -= 4;

    err = memAllocP(MPE_MEM_TEST, sectSize, (void **) &sectData);
    if (err != MPE_SUCCESS)
        goto return_err;

    err = filterSectionRead(sect, 0, sectSize, 0, sectData, &sectRead);
    if (err != MPE_SUCCESS)
        goto return_err;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "PMTSectionParse: sectRead = %u, dump follows\n", sectRead);
    dump("  ", sectData, sectRead);
    if (sectRead != sectSize)
    {
        err = MPE_SF_ERROR_INVALID_SECTION_HANDLE;
        goto return_err;
    }

    err = memAllocP(MPE_MEM_TEST, sizeof(*pmt), (void **) &pmt);
    if (err != MPE_SUCCESS)
        goto return_err;

    pmt->program_info = NULL;

    bitStreamInit(sectData, sectSize);

    pmt->header.table_id = bitStreamGet(8);
    pmt->header.section_syntax_indicator = bitStreamGet(1);
    (void) bitStreamGet(1 + 2);
    pmt->header.section_length = bitStreamGet(12);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "PMTSectionParse: section_length = %u\n",
            pmt->header.section_length);

    bitStreamGetInfo(&length_start, NULL, NULL);

    pmt->program_number = bitStreamGet(16);
    (void) bitStreamGet(2);
    pmt->version_number = bitStreamGet(5);
    pmt->current_next_indicator = bitStreamGet(1);
    pmt->section_number = bitStreamGet(8);
    pmt->last_section_number = bitStreamGet(8);
    (void) bitStreamGet(3);
    pmt->PCR_PID = bitStreamGet(13);
    (void) bitStreamGet(4);
    pmt->program_info_length = bitStreamGet(12);
    if (pmt->program_info_length)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "PMTSectionParse: program_info_length = %u\n",
                pmt->program_info_length);
        err = memAllocP(MPE_MEM_TEST, pmt->program_info_length,
                (void **) &pmt->program_info);
        if (err != MPE_SUCCESS)
            goto return_err;

        bitStreamGetBytes(pmt->program_info, pmt->program_info_length);

    }

    //
    // number of bytes left = (section_length - CRC) - consumed_so_far;
    //
    bitStreamGetInfo(&consumed, NULL, NULL);
    bytesLeft = ((pmt->header.section_length - 4) - ((consumed - length_start)
            / 8));

    last = NULL;
    while (bytesLeft)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "PMTSectionParse: bytesLeft = %u\n",
                bytesLeft);
        // keep bytesLeft up-to-date
        err = memAllocP(MPE_MEM_TEST, sizeof(*pmt->streams), (void **) &add);
        if (err != MPE_SUCCESS)
            goto return_err;
        add->stream_info = NULL;
        add->next = NULL;

        if (last)
            last->next = add;
        else
            pmt->streams = add;
        last = add;

        add->stream_type = bitStreamGet(8);
        (void) bitStreamGet(3);
        add->elementary_PID = bitStreamGet(13);
        (void) bitStreamGet(4);
        add->ES_info_length = bitStreamGet(12);

        bytesLeft -= (8 + 3 + 13 + 4 + 12) / 8;

        if (add->ES_info_length)
        {
            err = memAllocP(MPE_MEM_TEST, add->ES_info_length,
                    (void **) &add->stream_info);
            if (err != MPE_SUCCESS)
                goto return_err;

            bitStreamGetBytes(add->stream_info, add->ES_info_length);

            bytesLeft -= add->ES_info_length;
        }
    }

    // did we run out of bits along the way?
    bitStreamGetInfo(NULL, NULL, &failure);
    if (failure)
    {
        err = MPE_SF_ERROR_INVALID_SECTION_HANDLE;
        goto return_err;
    }

    bitStreamTerm();

    *returnPMT = pmt;

    return MPE_SUCCESS;

    return_err: if (pmt)
        PMTSectionFree(pmt);
    if (sectData)
        memFreeP(MPE_MEM_TEST, sectData);
    return err;
}

/****************************************************************************
 *
 *  PMTSectionDump()
 *
 ***************************************************************************/
/**
 * \brief print a PMT
 *
 * Given a C structure for a PMT, prints everything via TRACE().
 *
 * \param pmt pointer to the PMT
 *
 */
void PMTSectionDump(PMT_t *pmt)
{
    StreamInfo_t *scan;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "PMT: (table_id = %u, section_length = %u)\n",
            pmt->header.table_id, pmt->header.section_length);
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "    program_number = %u (0x%x)\n",
            pmt->program_number, pmt->program_number);
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "    version_number = %u (%s)\n",
            pmt->version_number, pmt->current_next_indicator ? "current"
                    : "next");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "    section = %u, last = %u\n",
            pmt->section_number, pmt->last_section_number);
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "    PCR_PID = %u (0x%x)\n",
            pmt->PCR_PID, pmt->PCR_PID);
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "    program_info_length = %u (0x%x)\n",
            pmt->program_info_length, pmt->program_info_length);
    if (pmt->program_info_length)
        dump("    ", pmt->program_info, pmt->program_info_length);
    for (scan = pmt->streams; scan; scan = scan->next)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "    stream - type = %u (%s)\n",
                scan->stream_type, translateStreamType(scan->stream_type));
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "        elementary_PID = %u (0x%x)\n", scan->elementary_PID,
                scan->elementary_PID);
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "        ES_info_length = %u (0x%x)\n", scan->ES_info_length,
                scan->ES_info_length);
        if (scan->ES_info_length)
            dump("        ", scan->stream_info, scan->ES_info_length);
    }
}

/****************************************************************************
 *
 *  dump()
 *
 ***************************************************************************/
/**
 * \brief dump binary data
 *
 * Prints binary data as a series of 16-byte lines with each 4 bytes 
 * separated with a space and each line having a given indentation.
 *
 * \param indent string to preface each line with
 * \param _data data to dump
 * \param count number of bytes to display
 *
 * \return desc
 *
 */
static void dump(const char *indent, const void *_data, uint32_t count)
{
    uint8_t *data = (uint8_t *) _data;
    uint8_t show[16];
    char fmt[128];
    uint32_t len;
    uint32_t i;
    uint32_t offset;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "%s Offset   0 1 2 3  4 5 6 7  8 9 a b  c d e f\n", indent);
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "%s-------- -------- -------- -------- --------\n", indent);
    offset = 0;
    while (count)
    {
        len = count >= 16 ? 16 : count;
        memcpy(show, data, len);

        strcpy(fmt, indent);
        strcat(fmt, "%08x ");
        for (i = 1; i <= len; ++i)
        {
            strcat(fmt, "%02x");
            if ((i % 4) == 0 && i != len)
                strcat(fmt, " ");
        }
        strcat(fmt, "\n");
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, fmt, offset, show[0], show[1],
                show[2], show[3], show[4], show[5], show[6], show[7], show[8],
                show[9], show[10], show[11], show[12], show[13], show[14],
                show[15]);

        count -= len;
        data += len;
        offset += 0x10;
    }
}

/****************************************************************************
 *
 *  PMTSectionFree()
 *
 ***************************************************************************/
/**
 * \brief release a PMT section structure to the OS
 *
 * Frees all memory allocated to express a PMT as a C structure
 *
 * \param pmt PMT structure to free
 *
 */
void PMTSectionFree(PMT_t *pmt)
{
    StreamInfo_t *next;

    if (pmt->program_info)
        memFreeP(MPE_MEM_TEST, pmt->program_info);
    for (; pmt->streams; pmt->streams = next)
    {
        next = pmt->streams->next;
        if (pmt->streams->stream_info)
            memFreeP(MPE_MEM_TEST, pmt->streams->stream_info);
        memFreeP(MPE_MEM_TEST, pmt->streams);
    }
    memFreeP(MPE_MEM_TEST, pmt);
}

typedef struct map_t map_t;

struct map_t
{
    char *name;
    uint32_t value;
};

#define MAP(x) { #x, x }
#define ENDMAP { NULL, 0 }

static map_t errorMap[] =
{
MAP(MPE_SUCCESS),
MAP(MPE_EINVAL),
MAP(MPE_ENOMEM),
MAP(MPE_SF_ERROR_FILTER_NOT_AVAILABLE),
MAP(MPE_SF_ERROR_INVALID_SECTION_HANDLE),
MAP(MPE_SF_ERROR_SECTION_NOT_AVAILABLE),
MAP(MPE_SF_ERROR_TUNER_NOT_TUNED),
MAP(MPE_SF_ERROR_TUNER_NOT_AT_FREQUENCY),
ENDMAP };

static map_t eventMap[] =
{
MAP(ETHREAD_TERMINATE),
MAP(MPE_SF_EVENT_FILTER_CANCELLED),
MAP(MPE_SF_EVENT_FILTER_PREEMPTED),
MAP(MPE_SF_EVENT_LAST_SECTION_FOUND),
MAP(MPE_SF_EVENT_OUT_OF_MEMORY),
MAP(MPE_SF_EVENT_SECTION_FOUND),
MAP(MPE_SF_EVENT_SOURCE_CLOSED),
ENDMAP };

static map_t
        streamMap[] =
        {
                { "ITU-T I ISO/IEC reserved", 0x00 },
                { "ISO/IEC 1 I 172-2 Video", 0x01 },
                {
                        "ITU-T Rec. H.262 I ISO/IEC 138 18-2 Video or ISO/IEC 11172-2 constrained parameter video stream",
                        0x02 },
                { "ISO/IEC 1 I 172-3 Audio", 0x03 },
                { "ISO/IEC 138 18-3 Audio", 0x04 },
                { "ITU-T Rec. H.222.0 I ISO/IEC 138 l8- 1 private-sections",
                        0x05 },
                {
                        "ITU-T Rec. H.222.0 I ISO/IEC 13818-l PES packets containing private data",
                        0x06 },
                { "ISO/IEC 13522 MHEG", 0x07 },
                { "Annex A - DSM CC", 0x08 },
                { "ITU-T Rec. H.222.1", 0x09 },
                { "ISO/IEC 138 18-6 type A", 0x0A },
                { "ISO/IEC 13818-6 type B", 0x0B },
                { "ISO/IEC 13818-6 type C", 0x0c },
                { "ISO/IEC 13818-6 type D", 0x0d },
                { "ISO/IEC 13818-1 auxiliary", 0x0e },
                ENDMAP };

/****************************************************************************
 *
 *  translate()
 *
 ***************************************************************************/
/**
 * \brief translate a uint32_t into a string
 *
 * Common, internal function used to translate a uint32_t into a string.
 *
 * \param map the map to translate the value against
 * \param value the value to translate
 *
 * \return string for the value
 *
 */
static char *translate(map_t *map, uint32_t value)
{
    static char buf[100];

    while (map->name)
    {
        if (value == map->value)
            return map->name;
        ++map;
    }

    // FIX: not a very safe way to do this. This case should not happen often,
    // but would cause problems for multiple calls from the same or
    // different threads
    sprintf(buf, "#%lu/0x%lx", value, value);

    return buf;
}

/****************************************************************************
 *
 *  translateError()
 *
 ***************************************************************************/
/**
 * \brief translate an error number to a string
 *
 * Translates the specified error number into the string for the name of the
 * error.
 *
 * \param err error number to translate
 *
 * \return string for the error
 *
 * \note This function does not work right if called more than once to
 * translate unknown values and the prior return values have not been used
 * yet.
 */
char *translateError(mpe_Error err)
{
    return translate(errorMap, (uint32_t) err);
}

/****************************************************************************
 *
 *  translateEvent()
 *
 ***************************************************************************/
/**
 * \brief translate an event number to a string
 *
 * Translates the specified event number into the string for the name of the
 * event.
 *
 * \param evt event number to translate
 *
 * \return string for the event
 *
 * \note This function does not work right if called more than once to
 * translate unknown values and the prior return values have not been used
 * yet.
 */
char *translateEvent(mpe_Event evt)
{
    return translate(eventMap, (uint32_t) evt);
}

/****************************************************************************
 *
 *  translateStreamType()
 *
 ***************************************************************************/
/**
 * \brief translate a stream type to a string
 *
 * Translates the specified stream type into the string for the name of the
 * stream type.
 *
 * \param stream_type stream type to translate
 *
 * \return string for the stream type
 *
 * \note This function does not work right if called more than once to
 * translate unknown values and the prior return values have not been used
 * yet.
 */
char *translateStreamType(uint8_t stream_type)
{
    return translate(streamMap, (uint32_t) stream_type);
}

static void sfEventThread(void *data);
static mpe_Error sfCopyByteArray(uint8_t **array, uint32_t length);
static mpe_Error sfPutSection(sfData_t *sfData, SectionFilter_t *filter,
        mpe_FilterSectionHandle sect);
static mpe_Error sfCheckSection(SectionFilter_t *filter,
        mpe_FilterSectionHandle sect);

/****************************************************************************
 *
 *  sfCopyByteArray()
 *
 ***************************************************************************/
/**
 * \brief copy a uint8_t array into allocated memory
 *
 * Allocates an array of the specified length and copies existing data into it.
 *
 * \param array pointer to the original pointer to the array to copy and the
 * return location for the new array pointer
 * \param length number of bytes in the array
 *
 * \return any error encountered making the new array
 *
 */
static mpe_Error sfCopyByteArray(uint8_t **array, uint32_t length)
{
    uint8_t *orig = *array;
    mpe_Error err;

    if (length == 0)
    {
        *array = NULL;
        return MPE_SUCCESS;
    }

    err = memAllocP(MPE_MEM_TEST, length, (void **) array);
    if (err != MPE_SUCCESS)
    {
        *array = orig;
        return err;
    }

    memcpy(*array, orig, length);

    return MPE_SUCCESS;
}

/****************************************************************************
 *
 *  sfInit()
 *
 ***************************************************************************/
/**
 * \brief initialize the section filter database
 *
 * sfInit() initialized a section filter database. A section filter database
 * is used to maintain list of outstanding filters. There are a number of
 * convenience functions related to these section filters. The database is
 * responsible for event handling.
 * 
 * \param sfData pointer to the shared data structure
 * \param tc CuTest case for the database. Any failed assertions will be
 * accounted to this test case.
 * 
 * \see sfTerm
 */
mpe_Error sfInit(sfData_t *sfData, CuTest *tc)
{
    mpe_Error err;
    const char *msg;

    if (sfData->inized)
        return MPE_SUCCESS;

    memset(sfData, 0, sizeof(*sfData));

    sfData->inized = TRUE;
    sfData->tc = tc;

    err = eventQueueNew(&sfData->que, "TestSFUtilsInit");
    if (err != MPE_SUCCESS)
    {
        msg = "failed to create event queue";
        goto return_err;
    }

    err = mutexNew(&sfData->mutex);
    if (err != MPE_SUCCESS)
    {
        msg = "failed to create mutex";
        goto return_err;
    }

    // start a thread to handle the event queue for all these filters
    sfData->terminate = FALSE;
    err = threadCreate(sfEventThread, sfData, MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_STACK_SIZE, &sfData->eventThreadID, "sfEventThread");
    if (err != MPE_SUCCESS)
    {
        msg = "failed to create event listener thread\n";
        goto return_err;
    }

    return MPE_SUCCESS;

    return_err: (void) sfTerm(sfData);

    CuAssertIntEquals_Msg(sfData->tc, msg, MPE_SUCCESS, err);
    return err;
}

/****************************************************************************
 *
 *  sfTerm()
 *
 ***************************************************************************/
/**
 * \brief terminate the section filter database
 *
 * Call sfTerm() once for each call to sfInit() with the same sfData value.
 * sfTerm() deletes all the existing section filters, terminates the event
 * handling thread, deletes the message queue, and deletes the mutex.
 * 
 * \param sfData pointer to the shared data structure
 * 
 * \return first error that occurs terminating the database
 * 
 * \see sfDelete, sfInit
 */
mpe_Error sfTerm(sfData_t *sfData)
{
    mpe_Error err;
    mpe_Error ret = MPE_SUCCESS;

    while (sfData->filters)
    {
        err = sfDelete(sfData, sfData->filters);
        if (ret == MPE_SUCCESS)
            ret = err;
    }

    if (sfData->eventThreadID)
    {
        err = eventQueueSend(sfData->que, ETHREAD_TERMINATE, NULL, NULL, 0);
        if (ret == MPE_SUCCESS)
            ret = err;

        while (!sfData->terminated)
            (void) threadSleep(100, 0);

        err = threadDestroy(sfData->eventThreadID);
        if (ret == MPE_SUCCESS)
            ret = err;
    }

    if (sfData->que)
    {
        err = eventQueueDelete(sfData->que);
        if (ret == MPE_SUCCESS)
            ret = err;
    }

    if (sfData->mutex)
    {
        err = mutexDelete(sfData->mutex);
        if (ret == MPE_SUCCESS)
            ret = err;
    }

    memset(sfData, 0, sizeof(*sfData));

    return ret;
}

/****************************************************************************
 *
 *  sfNew()
 *
 ***************************************************************************/
/**
 * \brief create a new section filter in the database
 *
 * sfNew() creates an unstarted filter in the database. All pointer parameters
 * are copied for the databases use and can be freed upon return from sfNew().
 * 
 * \param sfData pointer to the shared data structure
 * \param source pointer to the section source for the filter
 * \param spec pointer to the specification for the section filter
 * \param priority priority of the filter (1 = highest, 255 = lowest)
 * \param timesToMatch number of sections to match (0 = infinite)
 * \param flags flags to pass on to mpe[os]_filterSetFilter and flags for the
 * section filter database
 * \param filter pointer to location to fill with pointer to the new section
 * filter
 * 
 * \see sfStart, sfDelete
 * \see SFF_ defines in test_filter_sectionFiltering_include.h
 */
mpe_Error sfNew(sfData_t *sfData, mpe_FilterSource *source,
        mpe_FilterSpec *spec, uint8_t priority, uint32_t timesToMatch,
        uint32_t flags, SectionFilter_t **filter)
{
    mpe_Error err;
    SectionFilter_t *add;

    LOCK();

    err = memAllocP(MPE_MEM_TEST, sizeof(*add) + sizeof(*source)
            + sizeof(*spec), (void **) &add);
    if (err != MPE_SUCCESS)
        goto return_err;

    memset(add, 0, sizeof(*add));

    add->source = (mpe_FilterSource *) (add + 1);
    memcpy(add->source, source, sizeof(*source));

    add->spec = (mpe_FilterSpec *) ((uint8_t *) (add + 1) + sizeof(*source));
    memcpy(add->spec, spec, sizeof(*spec));
    sfCopyByteArray(&add->spec->pos.mask, add->spec->pos.length);
    sfCopyByteArray(&add->spec->pos.vals, add->spec->pos.length);
    sfCopyByteArray(&add->spec->neg.mask, add->spec->neg.length);
    sfCopyByteArray(&add->spec->neg.vals, add->spec->neg.length);

    add->priority = priority;
    add->timesToMatch = timesToMatch;
    if (timesToMatch == 0)
        add->infinite = TRUE;
    add->flags = flags;
    add->state = STATE_INIT;

    add->next = sfData->filters;
    sfData->filters = add;

    *filter = add;

    return_err: UNLOCK();

    return err;
}

/****************************************************************************
 *
 *  sfStart()
 *
 ***************************************************************************/
/**
 * \brief initiate filtering for a filter
 *
 * Once a filter exists in the database, it can be started with this function.
 * Sections are either accumlated by this filter or by the mpe[os] layer
 * depending on the state of the SFF_MANUAL_GET flag.
 * 
 * \param sfData pointer to the shared data structure
 * \param filter pointer to the filter to start
 *
 * \return any error starting the filter
 * 
 * \see sfNew, SFF_MANUAL_GET, sfGetSection
 */
mpe_Error sfStart(sfData_t *sfData, SectionFilter_t *filter)
{
    mpe_Error err;

    LOCK();

    // since events can happen while still in filterSetFilter
    filter->state = STATE_READY;

    err = filterSetFilter(filter->source, filter->spec, sfData->que, NULL,
            filter->priority, filter->timesToMatch, filter->flags
                    & SFF_RESERVED, // only pass reserved flags
            &filter->uid);
    if (err != MPE_SUCCESS)
    {
        filter->state = STATE_INIT;
        goto return_err;
    }

    return_err: UNLOCK();

    return err;
}

/****************************************************************************
 *
 *  sfCancel()
 *
 ***************************************************************************/
/**
 * \brief cancel a running database section filter
 * 
 * Calls mpe[os]_filterCancelFilter() for a running filter. If the filter has
 * already been cancelled or has never been started, MPE_EINVAL is returned.
 * Also validates that MPE_SF_EVENT_FILTER_CANCELLED is received after the
 * filter is cancelled.
 * 
 * \param sfData pointer to the shared data structure
 * \param filter pointer to the filter to cancel
 *
 * \return any error cancelling the filter
 * 
 * \see sfNew, SFF_MANUAL_GET
 */
mpe_Error sfCancel(sfData_t *sfData, SectionFilter_t *filter)
{
    mpe_Error err;
    uint32_t cnt;

    LOCK();

    // don't cancel a non-ready filter
    if (filter->state != STATE_READY)
    {
        err = MPE_EINVAL;
        goto return_err;
    }

    /*
     * 40. mpeos_filterCancelFilter() should return MPE_SUCCESS if the specified
     *   filter is successfully cancelled.
     */
    filter->flags |= SFF_CANCEL; // prepare for any CANCELLED events
    err = filterCancelFilter(filter->uid);
    if (err != MPE_SUCCESS)
    {
        goto return_err;
    }

    // make sure CANCELLED event arrives
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "waiting for CANCELLED event");
    cnt = 0;
    while (filter->eventId != MPE_SF_EVENT_FILTER_CANCELLED)
    {
        if (++cnt == 10)
        {
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST, ".");
            cnt = 0;
        }
        UNLOCK();
        threadSleep(100, 0);
        LOCK();
    }
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\n");

    return_err: UNLOCK();

    return err;
}

/****************************************************************************
 *
 *  sfGetSection()
 *
 ***************************************************************************/
/**
 * \brief get a section from a running database filter
 * 
 * sfGetSection() returns the next available section from a running database
 * section filter. If none is available, sfGetSection() will await the arrival
 * of a section. If the SFF_MANUAL_GET flag was used in sfNew() for the
 * specified filter, then this function returns
 * MPE_SF_ERROR_SECTION_NOT_AVAILABLE.
 * 
 * \param sfData pointer to the shared data structure
 * \param filter pointer to the filter to get a section from
 * \param sect pointer to the location to fill with a section handle
 *
 * \return any error cancelling the filter
 * 
 * \see sfStart, SFF_MANUAL_GET
 */
mpe_Error sfGetSection(sfData_t *sfData, SectionFilter_t *filter,
        mpe_FilterSectionHandle *sect)
{
    mpe_Error err = MPE_SUCCESS;
    SectionContainer_t *next;
    uint32_t cnt;

    if (filter->flags & SFF_MANUAL_GET)
        return MPE_SF_ERROR_SECTION_NOT_AVAILABLE;

    LOCK();

    cnt = 0;
    while (filter->sects == NULL)
    {
        if (++cnt == 10)
        {
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\nwaiting for sects on %x",
                    filter);
            cnt = 0;
        }
        UNLOCK();
        threadSleep(100, 0);
        LOCK();
    }

    *sect = filter->sects->handle;

    next = filter->sects->next;
    memFreeP(MPE_MEM_TEST, filter->sects);
    filter->sects = next;

    // return_err:
    UNLOCK();
    return err;
}

/****************************************************************************
 *
 *  sfCheckSection()
 *
 ***************************************************************************/
/**
 * \brief check a section for matching a filter's spec
 * 
 * sfCheckSection() checks that the passed section (handle) matches the
 * passed filter's specification. If it does not match,
 * MPE_SF_ERROR_INVALID_SECTION_HANDLE is returned.
 * 
 * \param filter pointer to filter to check against
 * \param sect section handle to check
 *
 * \return any error encounted (memory allocation) or
 * MPE_SF_ERROR_INVALID_SECTION_HANDLE if the section does not match the
 * specification.
 * 
 * \see sfPutSection
 */
static mpe_Error sfCheckSection(SectionFilter_t *filter,
        mpe_FilterSectionHandle sect)
{
    uint8_t *data = NULL;
    uint8_t *scan;
    uint32_t size;
    uint32_t len;
    uint32_t i;
    mpe_Error err;

    /*
     * 13. Sections generated by a filter should match the filter specification
     *   used when setting the filter.
     */
    err = filterGetSectionSize(sect, &size);
    if (err != MPE_SUCCESS)
        goto return_err;

    err = memAllocP(MPE_MEM_TEST, size, (void **) &data);
    if (err != MPE_SUCCESS)
        goto return_err;

    err = filterSectionRead(sect, 0, size, 0, data, &len);
    if (err != MPE_SUCCESS)
        goto return_err;
    if (len != size)
    {
        err = MPE_SF_ERROR_INVALID_SECTION_HANDLE;
        goto return_err;
    }

    // section too short for either the positive or negative masks?
    if (size < filter->spec->pos.length || size < filter->spec->neg.length)
    {
        err = MPE_SF_ERROR_INVALID_SECTION_HANDLE;
        goto return_err;
    }

    // positive match?
    for (scan = data, i = 0; i < filter->spec->pos.length; ++i, ++scan)
    {
        if (filter->spec->pos.mask[i])
        {
            if ((*scan & filter->spec->pos.mask[i])
                    != filter->spec->pos.vals[i])
            {
                err = MPE_SF_ERROR_INVALID_SECTION_HANDLE;
                goto return_err;
            }
        }
    }

    // negative match?
    for (scan = data, i = 0; i < filter->spec->neg.length; ++i, ++scan)
    {
        if (filter->spec->neg.mask[i])
        {
            if ((*scan & filter->spec->neg.mask[i])
                    == filter->spec->neg.vals[i])
            {
                err = MPE_SF_ERROR_INVALID_SECTION_HANDLE;
                goto return_err;
            }
        }
    }

    err = MPE_SUCCESS;

    return_err: if (data)
        memFreeP(MPE_MEM_TEST, data);

    return err;
}

/****************************************************************************
 *
 *  sfPutSection()
 *
 ***************************************************************************/
/**
 * \brief hang a section handle off a database filter
 * 
 * sfPutSection() is called by the event handler thread to attribute a section
 * to a section filter. These sections can then be retrieved by the database
 * user with sfGetSection(). This function will not be called if SFF_MANUAL_GET
 * was used on the sfNew() call for the specified filter.
 * 
 * \param sfData pointer to the shared data structure
 * \param filter pointer to filter to associate the section with
 * \param sect section handle to associate with the filter
 *
 * \return any error encounted (memory allocation) or
 * MPE_SF_ERROR_INVALID_SECTION_HANDLE if the section does not match the
 * specification.
 * 
 * \see sfPutSection
 */
static mpe_Error sfPutSection(sfData_t *sfData, SectionFilter_t *filter,
        mpe_FilterSectionHandle sect)
{
    mpe_Error err;
    SectionContainer_t *add = NULL;
    SectionContainer_t *scan;

    LOCK();

    // make sure section matches filter
    err = sfCheckSection(filter, sect);
    if (err != MPE_SUCCESS)
        goto return_err;

    err = memAllocP(MPE_MEM_TEST, sizeof(*add), (void **) &add);
    if (err != MPE_SUCCESS)
        goto return_err;

    add->handle = sect;
    add->next = NULL;
    for (scan = filter->sects; scan && scan->next; scan = scan->next)
        ;
    if (scan)
        scan->next = add;
    else
        filter->sects = add;
    add = NULL; // don't free add on the way out

    //	TRACE(MPE_LOG_INFO, MPE_MOD_TEST,"setting sects on %x to %x\n", filter, filter->sects);

    return_err: if (add)
        memFreeP(MPE_MEM_TEST, add);

    UNLOCK();

    return err;
}

/****************************************************************************
 *
 *  sfDumpSection()
 *
 ***************************************************************************/
/**
 * \brief dump the raw data from a section
 * 
 * \param sfData pointer to the shared data structure
 * \param sect section handle to dump
 *
 * \return Any error getting the size of the section, allocating memory for
 * copy of section, or reading the section's data.
 *
 */
mpe_Error sfDumpSection(sfData_t *sfData, mpe_FilterSectionHandle sect)
{
    mpe_Error err;
    uint32_t size;
    uint8_t *buf = NULL;
    uint32_t len;

    err = filterGetSectionSize(sect, &size);
    if (err != MPE_SUCCESS)
        goto return_err;

    err = memAllocP(MPE_MEM_TEST, size, (void **) &buf);
    if (err != MPE_SUCCESS)
        goto return_err;

    err = filterSectionRead(sect, 0, size, 0, buf, &len);
    if (err != MPE_SUCCESS)
        goto return_err;

    if (len != size)
    {
        err = MPE_EINVAL;
        goto return_err;
    }

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Section 0x%x, %u bytes\n", sect, size);

    dump("  ", buf, size);

    err = MPE_SUCCESS;

    return_err: if (buf)
        memFreeP(MPE_MEM_TEST, buf);

    return err;
}

/****************************************************************************
 *
 *  sfDelete()
 *
 ***************************************************************************/
/**
 * \brief remove a filter from the database
 * 
 * sfDelete() removes a filter from the database. If the filter is still
 * running, it is cancelled via mpe[os]_filterCancelFilter(). The filter is
 * released via mpe[os]_filterReleaseFilter().
 * 
 * \param sfData pointer to the shared data structure
 * \param filter pointer to filter to delete
 * \param sect section handle to associate with the filter
 *
 * \return any error encounted removing the filter
 * 
 * \see sfNew
 */
mpe_Error sfDelete(sfData_t *sfData, SectionFilter_t *filter)
{
    mpe_Error err = MPE_SUCCESS;
    SectionFilter_t *prev, *curr;
    SectionContainer_t *curr_sect, *next_sect;

    LOCK();

    for (prev = NULL, curr = sfData->filters; curr && curr != filter; prev
            = curr, curr = curr->next)
        ;

    if (curr == NULL)
    {
        err = MPE_SF_ERROR_FILTER_NOT_AVAILABLE;
        goto return_err;
    }

    // cancel filter
    if (curr->state == STATE_READY)
    {
        curr->flags |= SFF_CANCEL;
        filterCancelFilter(curr->uid);
        while (curr->state == STATE_READY)
        {
            UNLOCK();
            threadSleep(100, 0);
            LOCK();
        }
    }

    // should be cancelled state by now
    if (curr->state != STATE_INIT)
    {
        // don't check for errors on account of
        // test_filter_sectionFiltering_Cancel.c which has already released the
        // filter in one case
        curr->flags |= SFF_CANCEL; // prepare for any CANCELLED events
        (void) filterRelease(curr->uid);
    }

    curr->uid = 0;

    // unlink from list of filters
    if (prev)
        prev->next = curr->next;
    else
        sfData->filters = curr->next;
    curr->next = NULL;

    // release all accumulated sections
    for (curr_sect = filter->sects; curr_sect; curr_sect = next_sect)
    {
        next_sect = curr_sect->next;
        (void) filterSectionRelease(curr_sect->handle);
        memFreeP(MPE_MEM_TEST, curr_sect);
    }
    filter->sects = NULL;

    // free up all pointed-at resources
    if (filter->spec->pos.length)
    {
        memFreeP(MPE_MEM_TEST, filter->spec->pos.mask);
        memFreeP(MPE_MEM_TEST, filter->spec->pos.vals);
    }
    if (filter->spec->neg.length)
    {
        memFreeP(MPE_MEM_TEST, filter->spec->neg.mask);
        memFreeP(MPE_MEM_TEST, filter->spec->neg.vals);
    }

    // free up filter itself (also frees filter->spec and filter->source)
    memFreeP(MPE_MEM_TEST, filter);

    return_err: UNLOCK();

    return err;
}

/****************************************************************************
 *
 *  sfEventThread()
 *
 ***************************************************************************/
/**
 * \brief event thread for section filter database filters
 *
 * The event thread runs asynchronous with the users of the section filter
 * database and manages arriving events. The most recent event ID is recorded
 * in the filter's entry in the database. The following are the additional
 * operations performed for each arriving event:\n
 * - ETHREAD_TERMINATE - indicate that the event thread is terminated and
 * then the thread terminates\n
 * - MPE_SF_EVENT_FILTER_CANCELLED - make sure we're expecting a cancel event
 * and then mark the filter cancelled\n
 * - MPE_SF_EVENT_FILTER_PREEMPTED - mark the filter as cancelled\n
 * - MPE_SF_EVENT_OUT_OF_MEMORY - mark the filter as cancelled\n
 * - MPE_SF_EVENT_SOURCE_CLOSED - mark the filter as cancelled\n
 * - MPE_SF_EVENT_LAST_SECTION_FOUND - make sure the filter's not infinite,
 * make sure timeToMatch is properly observed, mark filter as cancelled, and
 * then, if SFF_MANUAL_GET was not specified in sfNew(), get the section's
 * handle and associate it with the filter.\n
 * - MPE_SF_EVENT_SECTION_FOUND - if the filter's not infinite, make sure
 * timesToMatch is properly observed, and then, if SFF_MANUAL_GET was not
 * specified in sfNew(), get the section's handle and associate it with the
 * filter.\n
 *
 * \param data pointer to the shared data structure, shared with database
 * caller and passed to virtually all "sf" API functions.
 *
 */
static void sfEventThread(void *data)
{
    sfData_t *sfData = (sfData_t *) data;
    mpe_Event eventId;
    uint32_t eventData;
    mpe_Error err;
    SectionFilter_t *filter;
    mpe_FilterSectionHandle sect;

    //	TRACE(MPE_LOG_INFO, MPE_MOD_TEST,"sfEventThread: start\n");
    while (!sfData->terminate)
    {
        //		TRACE(MPE_LOG_INFO, MPE_MOD_TEST,"sfEventThread: wait for event\n");
        err = eventQueueWaitNext(sfData->que, &eventId, (void **) &eventData,
                NULL, NULL, 0);
        //		TRACE(MPE_LOG_INFO, MPE_MOD_TEST,"sfEventThread: got one (err=%u, id=%s, data=%x)\n", err, translateEvent(eventId), eventData);
        if (err != MPE_SUCCESS)
        {
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                    "error getting next event in event thread\n");
            sfData->terminated = TRUE;
            return;
        }

        LOCK();

        filter = NULL;

        if (eventId != ETHREAD_TERMINATE)
        {
            // find the filter
            for (filter = sfData->filters; filter && filter->uid != eventData; filter
                    = filter->next)
                ;

            if (filter == NULL)
            {
                TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                        "failed to find filter %x for %s\n", eventData,
                        translateEvent(eventId));
                UNLOCK();
                continue;
            }

            filter->eventId = eventId;

            if (filter->state != STATE_READY)
            {
                TRACE(
                        MPE_LOG_INFO,
                        MPE_MOD_TEST,
                        "got an event (%s) for filter in non-ready state (%u)\n",
                        translateEvent(eventId), filter->state);
                CuAssertIntEquals_Msg(sfData->tc,
                        "got an event for filter in non-ready state\n",
                        STATE_READY, filter->state);
                UNLOCK();
                continue;
            }
        }

        switch (eventId)
        {
        /*
         * 42. Send MPE_SF_EVENT_FILTER_CANCELLED only when a filter is explicitly
         * cancelled or the filter is released before reaching the cancelled state.
         *   The supporting data is the filter’s uniqueID.
         */
        case MPE_SF_EVENT_FILTER_CANCELLED:
            if ((filter->flags & SFF_CANCEL) == 0)
            {
                TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                        "got a (%s) for filter not expecting it\n",
                        translateEvent(eventId));
                CuAssertIntEquals_Msg(sfData->tc,
                        "got a CANCEL event unexpectedly\n", SFF_CANCEL,
                        filter->flags & ~SFF_RESERVED);
            }
        case MPE_SF_EVENT_FILTER_PREEMPTED:
        case MPE_SF_EVENT_OUT_OF_MEMORY:
        case MPE_SF_EVENT_SOURCE_CLOSED:
            filter->state = STATE_CANCELLED;
            break;

        case MPE_SF_EVENT_LAST_SECTION_FOUND:
            /*
             * 18. Send MPE_SF_EVENT_LAST_SECTION_FOUND only when the last section is
             *   encountered. The supporting data is the filter’s uniqueID.
             */
            if (filter->infinite)
            {
                TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                        "FAILURE: %s received for infinite filter\n",
                        translateEvent(eventId));
                CuAssertIntEquals_Msg(sfData->tc,
                        "infinite filter got LAST_SECTION_FOUND\n", FALSE,
                        filter->infinite);
                // keep going
            }
            else
            {
                /*
                 * 15. No more than timesToMatch sections should be returned if timesToMatch
                 *   is non-zero.
                 */
                if (filter->timesToMatch == 0)
                {
                    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                            "FAILURE: too many sections matched for multi-match filter\n");
                    CuAssert(sfData->tc,
                            "too many sections for finite filter\n",
                            filter->timesToMatch != 0);
                    // keep going
                }
                else
                {
                    if (filter->timesToMatch != 1)
                    {
                        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                                "FAILURE: not enough sections matched for multi-match filter\n");
                        CuAssert(sfData->tc,
                                "not enough sections for finite filter\n",
                                filter->timesToMatch == 1);
                    }
                    --filter->timesToMatch;
                }
            }
            filter->state = STATE_CANCELLED;
            goto pull_section;

        case MPE_SF_EVENT_SECTION_FOUND:
            /*
             * 20. Send MPE_SF_EVENT_SECTION_FOUND only when a non-last section is
             *   encountered. The supporting data is the filter’s uniqueID.
             */
            if (!filter->infinite)
            {
                /*
                 * 15. No more than timesToMatch sections should be returned if timesToMatch
                 *   is non-zero.
                 */
                if (filter->timesToMatch == 0)
                {
                    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                            "FAILURE: too many sections matched for multi-match filter\n");
                    CuAssert(sfData->tc,
                            "too many sections for finite filter\n",
                            filter->timesToMatch != 0);
                    // keep going
                }
                else if (filter->timesToMatch == 1)
                {
                    TRACE(
                            MPE_LOG_INFO,
                            MPE_MOD_TEST,
                            "FAILURE: wrong event sent (%s) for last section on finite filter\n",
                            translateEvent(eventId));
                    CuAssert(sfData->tc,
                            "wrong event sent for finite filter\n",
                            filter->timesToMatch != 1);
                    --filter->timesToMatch;
                    // keep going
                }
                else
                    --filter->timesToMatch;
            }
            pull_section: if ((filter->flags & SFF_MANUAL_GET) == 0)
            {
                err = filterGetSectionHandle(eventData, 0, &sect);
                if (err != MPE_SUCCESS)
                {
                    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                            "failed to get section after %s (error = %s)\n",
                            translateEvent(eventId), translateError(err));
                    break;
                }
                err = sfPutSection(sfData, filter, sect);
                if (err != MPE_SUCCESS)
                {
                    TRACE(
                            MPE_LOG_INFO,
                            MPE_MOD_TEST,
                            "failed to put section into filter DB after %s (error = %s)\n",
                            translateEvent(eventId), translateError(err));
                    break;
                }
            }
            break;

        case ETHREAD_TERMINATE:
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "got ETHREAD_TERMINATE\n");
            sfData->terminated = TRUE;
            UNLOCK();
            return;

        default:
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "unknown event received (%d)\n",
                    eventId);
            break;
        }

        UNLOCK();
    }
}

#if !defined(TEST_MPEOS)
/* MPE tuning related functions */

static mpe_Bool checkErr(mpe_Error status, char *fmt, ...)
{
    va_list ap;
    static char buffer[4096];

    if (status != MPE_SUCCESS)
    {
        va_start(ap, fmt);

        vsprintf(buffer, fmt, ap);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_TEST, "**********************\n");
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_TEST,
                "********************** FAILED (%d): %s\n", status, buffer);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_TEST, "**********************\n");

        va_end(ap);
    }

    return (status != MPE_SUCCESS);
}

static mpe_Bool waitTune(mpe_EventQueue queue)
{
    mpe_Bool tuned = FALSE;
    mpe_Event event;
    mpe_Error rc;

    while (!tuned)
    {
        rc = mpe_eventQueueWaitNext(queue, &event, NULL, NULL, NULL, 60000);
        // Check for timeout
        if (checkErr(rc, "Tune Time out"))
            break;

        if (event == MPE_TUNE_ABORT || event == MPE_TUNE_FAIL)
        {
            checkErr(MPE_EINVAL, "Tune");
            break;
        }
        else if (event == MPE_TUNE_SYNC)
        {
            tuned = TRUE;
        }
    }

    if (tuned)
        mpe_threadSleep(5 * 1000, 0);

    return tuned;
}

static mpe_Bool tuneByFreqProg(uint32_t tunerId, uint32_t frequency,
        uint32_t program, uint32_t qamMode)
{
    mpe_MediaTuneRequestParams tuneParams;
    mpe_EventQueue queue;
    mpe_Bool ret;

    MPE_LOG(
            MPE_LOG_INFO,
            MPE_MOD_TEST,
            "Tuning by FreqProg - tuner = %u, freq = %u, prog = %u, qam = %u\n",
            tunerId, frequency, program, qamMode);
    tuneParams.tunerId = tunerId;
    tuneParams.tuneParams.frequency = frequency;
    tuneParams.tuneParams.programNumber = program;
    tuneParams.tuneParams.qamMode = qamMode;

    tuneParams.tuneParams.tuneType = MPE_MEDIA_TUNE_BY_TUNING_PARAMS;

    // Make a queue
    if (checkErr(mpe_eventQueueNew(&queue, "TestSFUtilsTuneByFreqProg"),
            "Event Queue New"))
        return FALSE;

    // Tune
    if (checkErr(mpe_mediaTune(&tuneParams, queue, NULL), "Tune"))
    {
        mpe_eventQueueDelete(queue);
        return FALSE;
    }

    // Wait for the tune to complete
    ret = waitTune(queue);

    mpe_eventQueueDelete(queue);

    // Get the handle
    return ret;
}

mpe_Error GoToOOBChannel(mpe_FilterSource **pFilterSource)
{
    mpe_Error err;

    /* tests don't currently actually get anything from the OOB source */

    err = memAllocP(MPE_MEM_TEST, sizeof(**pFilterSource),
            (void **) pFilterSource);
    if (err != MPE_SUCCESS)
        return err;

    (*pFilterSource)->sourceType = MPE_FILTER_SOURCE_OOB;
    (*pFilterSource)->pid = 0; // caller must fill
    (*pFilterSource)->parm.p_OOB.tsId = 0; // caller must change if desired

    return MPE_SUCCESS;
}

mpe_Error GoToInbandChannel(mpe_FilterSource **pFilterSource)
{
    mpe_Error err;

    if (tuneByFreqProg(INB_TUNER, INB_FREQ, INB_PROG, INB_QAM) == FALSE)
        return MPE_EINVAL;

    err = memAllocP(MPE_MEM_TEST, sizeof(**pFilterSource),
            (void **) pFilterSource);
    if (err != MPE_SUCCESS)
        return err;

    (*pFilterSource)->sourceType = MPE_FILTER_SOURCE_INB;
    (*pFilterSource)->pid = 0; // caller must fill upon return
    //	(*pFilterSource)->parm.p_INB.tunerID = INB_TUNER;
    (*pFilterSource)->parm.p_INB.freq = INB_FREQ;
    (*pFilterSource)->parm.p_INB.tsId = 0; // caller must change if desired

    return MPE_SUCCESS;
}

#endif /* !defined(TEST_MPEOS) */

