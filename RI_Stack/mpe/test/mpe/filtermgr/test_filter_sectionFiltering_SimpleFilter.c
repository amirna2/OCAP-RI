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

/**
 * \file
 *
 * \brief Very simple filtering testing (PAT filter)
 *
 * The test in this file simply:\n
 * -# tunes to an in-band source
 * -# creates a PAT filter
 * -# starts the PAT filter
 * -# waits for the LAST_SECTION_FOUND event
 * -# gets the handle for the section
 * -# releases the PAT filter
 * -# parses the PAT from the section handle
 * 
 * Along the way it verifies various requirements.
 *
 * \author Ric Yeates, Vidiom Systems Corp.
 *
 */

#include "test_filter_sectionFiltering_include.h"
#include <test_media.h>
#include "sectionFiltering_parameters.h"
#include <stdio.h>
#include <ctype.h>

void test_filter_sectionFiltering_SimpleFilter(CuTest *tc);

/****************************************************************************
 *
 *  test_filter_sectionFiltering_SimpleFilter()
 *
 ***************************************************************************/
/**
 * \testdescription Tests simple filter functionality by setting up a simple
 * PAT filter.
 *
 * \api mpe[os]_filterSetFilter()\n
 * MPE_SF_EVENT_LAST_SECTION_FOUND\n
 * mpe[os]_filterGetSectionHandle()\n
 * mpe[os]_filterGetSectionSize()\n
 * mpe[os]_filterSectionRead()\n
 * mpe[os]_filterSectionRelease()\n
 * mpe[os]_filterRelease()\n
 *
 * \strategy Go through the process of creating a simple filter, get a
 * section from it, and tear it down. Along the way, validate various
 * requirements.
 *
 * \assets in-band filter source
 *
 */
void test_filter_sectionFiltering_SimpleFilter(CuTest *tc)
{
    mpe_FilterSource *source = NULL;
    uint32_t filter;
    mpe_FilterSpec *spec = NULL;
    mpe_EventQueue que;
    mpe_Event eventId;
    uint32_t eventData;
    mpe_Error err;
    mpe_FilterSectionHandle sect;
    PAT_t *pat;
    uint32_t size;
    uint8_t buffer[32];
    uint32_t bytesRead;
    uint32_t byteCount;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "Start test - test_filter_sectionFiltering_SimpleFilter\n");

    err = eventQueueNew(&que, "TestSFSimple");
    CuAssertIntEquals_Msg(tc, "failed to create event queue", MPE_SUCCESS, err);

    err = GoToInbandChannel(&source);
    if (err != MPE_SUCCESS)
    {
        eventQueueDelete(que);
        CuAssertIntEquals_Msg(tc, "GoToInbandChannel() failed", MPE_SUCCESS,
                err);
        return;
    }

    // filter for a PAT
    err = PATSectionFilter(source, &spec);
    if (err != MPE_SUCCESS)
    {
        memFreeP(MPE_MEM_TEST, source);
        eventQueueDelete(que);
        CuAssertIntEquals_Msg(tc, "failed to make PAT filter", MPE_SUCCESS, err);
        return;
    }

    /**
     * \requirement 1. mpeos_filterSetFilter() should return MPE_SUCCESS if
     * the filter is correctly installed on an in-band filter source.
     */
    // set filter to match 1 PAT at highest priority
    TRACE(
            MPE_LOG_DEBUG,
            MPE_MOD_TEST,
            "test_filter_sectionFiltering_SimpleFilter - calling mpe[os]_filterSetFilter()\n");
    filter = 0;
    err = filterSetFilter(source, spec, que, NULL, 1, 1, 0, &filter);
    TRACE(
            MPE_LOG_DEBUG,
            MPE_MOD_TEST,
            "test_filter_sectionFiltering_SimpleFilter - 'filterSetFilter()' returned %d\n",
            err);

    if (err != MPE_SUCCESS)
    {
        memFreeP(MPE_MEM_TEST, spec);
        memFreeP(MPE_MEM_TEST, source);
        eventQueueDelete(que);
        CuAssertIntEquals_Msg(tc, "failed to set simple filter", MPE_SUCCESS,
                err);
        return;
    }

    memFreeP(MPE_MEM_TEST, spec);
    memFreeP(MPE_MEM_TEST, source);

    TRACE(
            MPE_LOG_DEBUG,
            MPE_MOD_TEST,
            "test_filter_sectionFiltering_SimpleFilter - mpe_setFilter() success, uniqueifier = 0x%x \n",
            filter);

#if defined(FILTERING_SHIM)
    // The filtering shim has a way to make sections arrive
    mpeos_filterGetPAT(filter);
#endif

    eventId = 0;
    eventData = 0;

    // wait forever for the next event. 
    err
            = eventQueueWaitNext(que, &eventId, (void **) &eventData, NULL,
                    NULL, 0);
    if (err != MPE_SUCCESS)
    {
        filterCancelFilter(filter);
        filterRelease(filter);
        eventQueueDelete(que);
        CuAssertIntEquals_Msg(tc, "failed to get event from filter",
                MPE_SUCCESS, err);
        return;
    }

    switch (eventId)
    {
    case MPE_SF_EVENT_FILTER_CANCELLED:
    case MPE_SF_EVENT_FILTER_PREEMPTED:
    case MPE_SF_EVENT_OUT_OF_MEMORY:
    case MPE_SF_EVENT_SOURCE_CLOSED:
    case MPE_SF_EVENT_SECTION_FOUND:
    default:
        err = !MPE_SUCCESS;
        break;
    case MPE_SF_EVENT_LAST_SECTION_FOUND:
        err = MPE_SUCCESS;
        break;
    }

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "event %s event received\n",
            translateEvent(eventId));

    if (err != MPE_SUCCESS)
    {
        filterCancelFilter(filter);
        filterRelease(filter);
        eventQueueDelete(que);
        CuAssertIntEquals_Msg(tc, "wrong event value",
                MPE_SF_EVENT_LAST_SECTION_FOUND, eventId);
        return;
    }

    /**
     * \requirement 22. mpeos_filterGetSectionHandle() should return
     * MPE_SUCCESS if a valid section handle is returned.
     */
    err = filterGetSectionHandle(filter, 0, &sect);
    if (err != MPE_SUCCESS)
    {
        filterCancelFilter(filter);
        filterRelease(filter);
        eventQueueDelete(que);
        CuAssertIntEquals_Msg(tc, "failed to get section handle", MPE_SUCCESS,
                err);
        return;
    }

    /**
     * \requirement 43. mpeos_filterRelease() should return MPE_SUCCESS if the
     * filter can be released.
     */
    err = filterRelease(filter);
    if (err != MPE_SUCCESS)
    {
        filterSectionRelease(sect);
        eventQueueDelete(que);
        CuAssertIntEquals_Msg(tc, "failed to release filter", MPE_SUCCESS, err);
        return;
    }

    /**
     * \requirement 44. mpeos_filterRelease() should return MPE_EINVAL if uniqueID is
     * invalid.
     */
    err = filterRelease(filter);
    if (err != MPE_EINVAL)
    {
        filterSectionRelease(sect);
        eventQueueDelete(que);
        CuAssertIntEquals_Msg(tc,
                "failed to get EINVAL for double release filter", MPE_EINVAL,
                err);
        return;
    }
    err = filterRelease(filter + 1);
    if (err != MPE_EINVAL)
    {
        filterSectionRelease(sect);
        eventQueueDelete(que);
        CuAssertIntEquals_Msg(tc,
                "failed to get EINVAL for (filter + 1) release", MPE_EINVAL,
                err);
        return;
    }

    eventQueueDelete(que);

    /**
     * \requirement 27. mpeos_filterGetSectionSize() should return MPE_SUCCESS if the size of
     * the section is returned.
     */
    /**
     * \requirement 30. mpeos_filterSectionRead() should return MPE_SUCCESS if the data is
     * returned.
     */
    /**
     * \requirement 46. After mpeos_filterRelease(), sections that have already been returned
     * by mpeos_filterGetSectionHandle() should still be readable.
     */
    err = PATSectionParse(sect, &pat);
    if (err != MPE_SUCCESS)
    {
        filterSectionRelease(sect);
        CuAssertIntEquals_Msg(tc, "PAT section/table does not check out",
                MPE_SUCCESS, err);
        return;
    }

    PATSectionFree(pat);

    /**
     * \requirement 28. mpeos_filterGetSectionSize() should return MPE_EINVAL if the size
     * parameter is NULL.
     */
    err = filterGetSectionSize(sect, NULL);
    if (err != MPE_EINVAL)
    {
        filterSectionRelease(sect);
        CuAssertIntEquals_Msg(tc,
                "failed to get MPE_EINVAL for NULL size to GetSectionSize",
                MPE_EINVAL, err);
    }

    /**
     * \requirement 29. mpeos_filterGetSectionSize() should return
     * MPE_SF_ERROR_INVALID_SECTION_HANDLE if the section handle is invalid.
     */
    err = filterGetSectionSize(0, &size);
    if (err != MPE_SF_ERROR_INVALID_SECTION_HANDLE)
    {
        filterSectionRelease(sect);
        CuAssertIntEquals_Msg(
                tc,
                "failed to get MPE_SF_ERROR_INVALID_SECTION_HANDLE for bad section ID to GetSectionSize",
                MPE_SF_ERROR_INVALID_SECTION_HANDLE, err);
    }
#if 0
    /* unrealistic call */
    err = filterGetSectionSize(sect + 1, &size);
    if (err != MPE_SF_ERROR_INVALID_SECTION_HANDLE)
    {
        filterSectionRelease(sect);
        CuAssertIntEquals_Msg(tc, "failed to get MPE_SF_ERROR_INVALID_SECTION_HANDLE for bad section ID to GetSectionSize", MPE_SF_ERROR_INVALID_SECTION_HANDLE, err);
    }
#endif

    /**
     * \requirement 31. mpeos_filterSectionRead() should return MPE_EINVAL if buffer is NULL,
     * byteCount is 0, offset >= the section size, or invalid flags are
     * specified.
     */
    // really get the size for later use
    err = filterGetSectionSize(sect, &size);
    if (err != MPE_SUCCESS)
    {
        filterSectionRelease(sect);
        CuAssertIntEquals_Msg(tc, "failed to get section size", MPE_SUCCESS,
                err);
    }

    err = filterSectionRead(sect, // sectionHandle
            0, // offset
            1, // byteCount
            0, // flags
            NULL, // buffer
            &bytesRead); // bytesRead
    if (err != MPE_EINVAL)
    {
        filterSectionRelease(sect);
        CuAssertIntEquals_Msg(
                tc,
                "failed to get MPE_EINVAL for NULL buffer to filterSectionRead",
                MPE_EINVAL, err);
    }

    err = filterSectionRead(sect, // sectionHandle
            0, // offset
            0, // byteCount
            0, // flags
            buffer, // buffer
            &bytesRead); // bytesRead
    if (err != MPE_EINVAL)
    {
        filterSectionRelease(sect);
        CuAssertIntEquals_Msg(
                tc,
                "failed to get MPE_EINVAL for byteCount 0 to filterSectionRead",
                MPE_EINVAL, err);
    }

    err = filterSectionRead(sect, // sectionHandle
            size, // offset
            1, // byteCount
            0, // flags
            buffer, // buffer
            &bytesRead); // bytesRead
    if (err != MPE_EINVAL)
    {
        filterSectionRelease(sect);
        CuAssertIntEquals_Msg(
                tc,
                "failed to get MPE_EINVAL for offset >= size to filterSectionRead",
                MPE_EINVAL, err);
    }

    err = filterSectionRead(sect, // sectionHandle
            size, // offset
            1, // byteCount
            ~0, // flags
            buffer, // buffer
            &bytesRead); // bytesRead
    if (err != MPE_EINVAL)
    {
        filterSectionRelease(sect);
        CuAssertIntEquals_Msg(tc,
                "failed to get MPE_EINVAL for bad flags to filterSectionRead",
                MPE_EINVAL, err);
    }

    /**
     * \requirement 32. mpeos_filterSectionRead() should return
     * MPE_SF_ERROR_INVALID_SECTION_HANDLE if an invalid section handle is
     * specified.
     */
    err = filterSectionRead(0, // sectionHandle
            size, // offset
            1, // byteCount
            0, // flags
            buffer, // buffer
            &bytesRead); // bytesRead
    if (err != MPE_SF_ERROR_INVALID_SECTION_HANDLE)
    {
        filterSectionRelease(sect);
        CuAssertIntEquals_Msg(
                tc,
                "failed to get MPE_SF_ERROR_INVALID_SECTION_HANDLE for NULL section handle to filterSectionRead",
                MPE_SF_ERROR_INVALID_SECTION_HANDLE, err);
    }

#if 0
    /* unrealistic call */
    err = filterSectionRead(sect + 1, // sectionHandle
            size, // offset
            1, // byteCount
            0, // flags
            buffer, // buffer
            &bytesRead); // bytesRead
    if (err != MPE_SF_ERROR_INVALID_SECTION_HANDLE)
    {
        filterSectionRelease(sect);
        CuAssertIntEquals_Msg(tc, "failed to get MPE_SF_ERROR_INVALID_SECTION_HANDLE for (sect + 1) section handle to filterSectionRead", MPE_SF_ERROR_INVALID_SECTION_HANDLE, err);
    }
#endif

    /**
     * \requirement 33. mpeos_filterSectionRead() should return appropriate bytesRead values
     * for various offsets and byteCounts.
     */
    // read from the start
    byteCount = size;
    if (byteCount > sizeof(buffer))
        byteCount = sizeof(buffer);
    err = filterSectionRead(sect, // sectionHandle
            0, // offset
            byteCount, // byteCount
            0, // flags
            buffer, // buffer
            &bytesRead); // bytesRead
    if (err != MPE_SUCCESS)
    {
        filterSectionRelease(sect);
        CuAssertIntEquals_Msg(tc, "failed to get MPE_SUCCESS for start read\n",
                MPE_SUCCESS, err);
    }
    if (bytesRead != byteCount)
    {
        filterSectionRelease(sect);
        CuAssertIntEquals_Msg(tc, "wrong byteCount for start read\n",
                byteCount, bytesRead);
    }

    // read the middle
    byteCount = 4;
    err = filterSectionRead(sect, // sectionHandle
            4, // offset
            byteCount, // byteCount
            0, // flags
            buffer, // buffer
            &bytesRead); // bytesRead
    if (err != MPE_SUCCESS)
    {
        filterSectionRelease(sect);
        CuAssertIntEquals_Msg(tc,
                "failed to get MPE_SUCCESS for middle read\n", MPE_SUCCESS, err);
    }
    if (bytesRead != byteCount)
    {
        filterSectionRelease(sect);
        CuAssertIntEquals_Msg(tc, "wrong byteCount for middle read\n",
                byteCount, bytesRead);
    }

    // read near the end for extra
    byteCount = sizeof(buffer);
    err = filterSectionRead(sect, // sectionHandle
            size - 4, // offset
            byteCount, // byteCount
            0, // flags
            buffer, // buffer
            &bytesRead); // bytesRead
    if (err != MPE_SUCCESS)
    {
        filterSectionRelease(sect);
        CuAssertIntEquals_Msg(tc, "failed to get MPE_SUCCESS for end read\n",
                MPE_SUCCESS, err);
    }
    if (bytesRead != 4)
    {
        filterSectionRelease(sect);
        CuAssertIntEquals_Msg(tc, "wrong byteCount for end read\n", byteCount,
                bytesRead);
    }

    /**
     * \requirement 34. Mpeos_filterSectionRead() should allow byteCount to be NULL.
     */
    byteCount = sizeof(buffer);
    err = filterSectionRead(sect, // sectionHandle
            size - 4, // offset
            byteCount, // byteCount
            0, // flags
            buffer, // buffer
            NULL); // bytesRead
    if (err != MPE_SUCCESS)
    {
        filterSectionRelease(sect);
        CuAssertIntEquals_Msg(tc,
                "failed to get MPE_SUCCESS for end read, NULL bytesRead\n",
                MPE_SUCCESS, err);
    }

    /**
     * \requirement 36. mpeos_filterSectionRelease() should return MPE_SUCCESS if the section
     * handle is released.
     */
    err = filterSectionRelease(sect);
    CuAssertIntEquals_Msg(tc, "could not release PAT section", MPE_SUCCESS, err);

    /**
     * \requirement 37. mpeos_filterSectionRelease() should return
     * MPE_SF_ERROR_INVALID_SECTION_HANDLE if the section handle is invalid.
     */
    err = filterSectionRelease(0);
    CuAssertIntEquals_Msg(tc,
            "incorrect error for NULL section handle to SectionRelease",
            MPE_SF_ERROR_INVALID_SECTION_HANDLE, err);

#if 0
    /* unrealistic call */
    err = filterSectionRelease(sect + 1);
    CuAssertIntEquals_Msg(tc, "incorrect error for (sect + 1) section handle to SectionRelease", MPE_SF_ERROR_INVALID_SECTION_HANDLE, err);
#endif

#if 0
    /* unrealistic situation */
    /**
     * \requirement 38. mpeos_filterSectionRelease() should return
     * MPE_SF_ERROR_INVALID_SECTION_HANDLE if the section handle is a section
     * handle that has already been released.
     */
    err = filterSectionRelease(sect);
    CuAssertIntEquals_Msg(tc, "incorrect error for double section handle release", MPE_SF_ERROR_INVALID_SECTION_HANDLE, err);
#endif

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
            "End test - test_filter_sectionfiltering_SimpleFilter\n");
}

/****************************************************************************
 *
 *  test_getTestSuite_sectionFiltering_SimpleFilter()
 *
 ***************************************************************************/
/**
 * \brief adds simple filter test(s) to a suite
 *
 * Given a CuSuite object, this function adds all the simple filter tests to
 * it.
 *
 * \param suite the suite to add the tests to
 *
 */
void test_getTestSuite_sectionFiltering_SimpleFilter(CuSuite* suite)
{
    SUITE_ADD_TEST(suite, test_filter_sectionFiltering_SimpleFilter);
}
