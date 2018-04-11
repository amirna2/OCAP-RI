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
 * \brief Test filter cancel related requirements
 *
 * The tests in this file validate the functionality of the section filtering
 * API as it relates to filter cancellation.
 *
 * \author Ric Yeates, Vidiom Systems Corp.
 *
 */

#include "test_filter_sectionFiltering_include.h"
#include "sectionFiltering_parameters.h"
#include <stdio.h>
#include <ctype.h>

void test_filter_sectionFiltering_Cancel(CuTest *tc);

static sfData_t sfSharedData;

/****************************************************************************
 *
 *  test_filter_sectionFiltering_Cancel()
 *
 ***************************************************************************/
/**
 * \testdescription Tests cancel related features of section filtering API
 *
 * \api mpe[os]_filterSetFilter()\n
 * mpe[os]_filterGetSectionHandle()
 * mpe[os]_filterRelease()\n
 * mpe[os]_filterCancelFilter()\n
 *
 * \strategy The strategy involves setting up various filters that will not
 * match any sections and then using cancel at various times on them.
 *
 * \assets in-band filter source
 *
 */
void test_filter_sectionFiltering_Cancel(CuTest *tc)
{
    mpe_FilterSource *source = NULL;
    mpe_FilterSpec localSpec;
    uint8_t posMask[LONG_SIZE];
    uint8_t posVals[LONG_SIZE];
    mpe_FilterSpec *spec = NULL;
    mpe_Error err;
    mpe_Error expectedError;
    sfData_t *sfData = &sfSharedData;
    char *msg;
    SectionFilter_t *sf;
    mpe_FilterSectionHandle sect = 0;
    PAT_t *pat = NULL;
    uint32_t cnt;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "Start test - test_filter_sectionFiltering_Cancel\n");

    expectedError = MPE_SUCCESS;
    err = sfInit(sfData, tc);
    if (err != expectedError)
    {
        msg = "failed to initialize section filter DB";
        goto return_err;
    }

    // go to inband channel
    err = GoToInbandChannel(&source);
    if (err != expectedError)
    {
        msg = "GoToInbandChannel() failed";
        goto return_err;
    }

    // create filter localSpec for something we won't likely see
    localSpec.pos.length = LONG_SIZE;
    localSpec.pos.mask = posMask;
    localSpec.pos.vals = posVals;
    memset(posMask, 0xff, sizeof(posMask));
    memset(posVals, 0xa5, sizeof(posVals));
    posVals[0] = 0x00; // PAT
    posVals[1] = 0x4f; // syntax bit not set, high bits of length set
    posVals[2] = 0x00; // start at 0xf00 for section length
    localSpec.neg.length = 0;
    source->pid = 0x01; // hopefully no PATs here

    // start filter that won't get any sections
    err = sfNew(sfData, source, &localSpec, 1, 0, 0, &sf);
    if (err != expectedError)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "failed to create filter, err = %s\n", translateError(err));
        msg = "failed to create filter in DB";
        goto return_err;
    }

    err = sfStart(sfData, sf);
    if (err != expectedError)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "failed to start empty filter, err = %s\n", translateError(err));
        msg = "failed to start empty filter in DB";
        goto return_err;
    }

    /*
     * 26. mpeos_filterGetSectionHandle() should return
     *    MPE_SF_ERROR_SECTION_NOT_AVAILABLE if no sections are available or
     *    MPE_SF_OPTION_IF_NOT_CANCELLED is indicated and the filter is in the
     *   cancelled state.
     */
    // try to get a section and expect error
    expectedError = MPE_SF_ERROR_SECTION_NOT_AVAILABLE;
    err = filterGetSectionHandle(sf->uid, 0, &sect);
    if (err != expectedError)
    {
        TRACE(
                MPE_LOG_INFO,
                MPE_MOD_TEST,
                "expected %s, got %s when getting from empty filter, no flag\n",
                translateError(expectedError), translateError(err));
        msg
                = "failed to get error when GetSectionHandle called with no sections, no flag";
        goto return_err;
    }
    err
            = filterGetSectionHandle(sf->uid, MPE_SF_OPTION_IF_NOT_CANCELLED,
                    &sect);
    if (err != expectedError)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "expected %s, got %s when getting from empty filter, flag\n",
                translateError(expectedError), translateError(err));
        msg
                = "failed to get error when GetSectionHandle called with no sections, flag";
        goto return_err;
    }
    expectedError = MPE_SUCCESS;

    // cancel the filter
    err = sfCancel(sfData, sf);
    if (err != expectedError)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "expected %s, got %s cancelling infinite, empty filter\n",
                translateError(expectedError), translateError(err));
        msg = "failed to cancel empty filter";
        goto return_err;
    }

    // make sure it still has no sections
    expectedError = MPE_SF_ERROR_SECTION_NOT_AVAILABLE;
    err = filterGetSectionHandle(sf->uid, 0, &sect);
    if (err != expectedError)
    {
        TRACE(
                MPE_LOG_INFO,
                MPE_MOD_TEST,
                "expected %s, got %s when getting from empty, cancelled filter, no flag\n",
                translateError(expectedError), translateError(err));
        msg
                = "failed to get error when GetSectionHandle called with no sections, no flag, cancelled filter";
        goto return_err;
    }
    err
            = filterGetSectionHandle(sf->uid, MPE_SF_OPTION_IF_NOT_CANCELLED,
                    &sect);
    if (err != expectedError)
    {
        TRACE(
                MPE_LOG_INFO,
                MPE_MOD_TEST,
                "expected %s, got %s when getting from empty, cancelled filter, flag\n",
                translateError(expectedError), translateError(err));
        msg
                = "failed to get error when GetSectionHandle called with no sections, flag, cancelled filter";
        goto return_err;
    }
    expectedError = MPE_SUCCESS;

    // delete filter
    err = sfDelete(sfData, sf);
    if (err != expectedError)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "expected %s, got %s deleting infinite, empty filter\n",
                translateError(expectedError), translateError(err));
        msg = "failed to delete empty filter";
        goto return_err;
    }

    // create infinite PAT filter (manual section getting)
    err = PATSectionFilter(source, &spec);
    if (err != MPE_SUCCESS)
    {
        msg = "failed to create PAT section filter spec";
        goto return_err;
    }

    err = sfNew(sfData, source, spec, 1, 0, SFF_MANUAL_GET | 0, &sf);
    if (err != expectedError)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "failed to create PAT filter, err = %s\n", translateError(err));
        msg = "failed to create PAT filter in DB";
        goto return_err;
    }

    err = sfStart(sfData, sf);
    if (err != expectedError)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "failed to start PAT filter, err = %s\n", translateError(err));
        msg = "failed to start PAT filter in DB";
        goto return_err;
    }

    // wait for section(s) to arrive
    cnt = 0;
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "waiting for PAT section");
    while (sf->eventId != MPE_SF_EVENT_SECTION_FOUND)
    {
        if (++cnt == 10)
        {
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST, ".");
            cnt = 0;
        }
        threadSleep(100, 0);
#if defined(FILTERING_SHIM)
        // make a PAT show up
        mpeos_filterGetPAT(sf->uid);
#endif
    }
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\n");

    // cancel the filter (doesn't return until event arrives)
    err = sfCancel(sfData, sf);

    // ask for a section only if filter is not cancelled (expect to not get one)
    expectedError = MPE_SF_ERROR_SECTION_NOT_AVAILABLE;
    err
            = filterGetSectionHandle(sf->uid, MPE_SF_OPTION_IF_NOT_CANCELLED,
                    &sect);
    if (err != expectedError)
    {
        TRACE(
                MPE_LOG_INFO,
                MPE_MOD_TEST,
                "expected %s, got %s when getting from non-empty, cancelled filter, flag\n",
                translateError(expectedError), translateError(err));
        msg
                = "failed to get error when GetSectionHandle called with sections, flag, cancelled filter";
        goto return_err;
    }
    expectedError = MPE_SUCCESS;

    /**
     * \requirement 41. mpeos_filterCancelFilter() should not discard any matched section data
     * (regardless of whether or not a handle has been issued).
     */
    // ask for a section without the flag set (expect to get one)
    err = filterGetSectionHandle(sf->uid, 0, &sect);
    if (err != expectedError)
    {
        TRACE(
                MPE_LOG_INFO,
                MPE_MOD_TEST,
                "expected %s, got %s when getting from empty, cancelled filter, no flag\n",
                translateError(expectedError), translateError(err));
        msg
                = "failed to get error when GetSectionHandle called with no sections, no flag, cancelled filter";
        goto return_err;
    }

    err = PATSectionParse(sect, &pat);
    if (err != expectedError)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "invalid PAT section encountered?\n");
        msg = "failed to parse PAT section";
        goto return_err;
    }

    PATSectionDump(pat);
    PATSectionFree(pat);
    pat = NULL;

    /**
     * \requirement 35. mpeos_filterSectionRead() should release the section if
     * MPE_SF_OPTION_RELEASE_WHEN_COMPLETE is specified and MPE_SUCCESS is
     * returned.
     */
    // release section via filterSectionRead
    err = filterSectionRead(sect, 0, 1, MPE_SF_OPTION_RELEASE_WHEN_COMPLETE,
            posVals, &cnt);
    if (err != expectedError)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "failed to read a byte from the PAT section (err = %s)\n",
                translateError(err));
        msg = "failed to read a byte from the PAT section";
        goto return_err;
    }

#if 0
    /* can't test this because section handle is not checked for validity */
    expectedError = MPE_SF_ERROR_INVALID_SECTION_HANDLE;
    err = filterSectionRelease(sect);
    sect = 0;
    if (err != expectedError)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,"failed to get error freeing PAT section\n");
        msg = "failed to get error freeing PAT section";
        goto return_err;
    }
    expectedError = MPE_SUCCESS;
#endif

    // delete filter
    err = sfDelete(sfData, sf);
    if (err != expectedError)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "failed to delete PAT filter\n");
        msg = "failed to delete PAT filter";
        goto return_err;
    }

    // start another PAT filter (manual section getting)
    err = sfNew(sfData, source, spec, 1, 0, SFF_MANUAL_GET | 0, &sf);
    if (err != expectedError)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "failed to create PAT filter second time, err = %s\n",
                translateError(err));
        msg = "failed to create PAT filter second time in DB";
        goto return_err;
    }

    err = sfStart(sfData, sf);
    if (err != expectedError)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "failed to start PAT filter second time, err = %s\n",
                translateError(err));
        msg = "failed to start PAT filter second time in DB";
        goto return_err;
    }

    /**
     * \requirement 45. mpeos_filterRelease() should send MPE_EVENT_FILTER_CANCELLED if the
     * specified filter is in the ready state.
     */
    // release filter while it's still active
    sf->flags |= SFF_CANCEL;
    err = filterRelease(sf->uid);
    if (err != expectedError)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "failed to release running PAT filter, err = %s\n",
                translateError(err));
        msg = "failed to release running PAT filter";
        goto return_err;
    }

    // wait for cancelled event
    cnt = 0;
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "waiting for CANCELLED event due to filterRelease");
    while (sf->eventId != MPE_SF_EVENT_FILTER_CANCELLED)
    {
        if (++cnt == 10)
        {
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST, ".");
            cnt = 0;
        }
        threadSleep(100, 0);
    }
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\n");

    // delete filter
    err = sfDelete(sfData, sf);
    if (err != expectedError)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "failed to delete PAT filter second time, err = %s\n",
                translateError(err));
        msg = "failed to delete PAT filter second time";
        goto return_err;
    }

    msg = "";
    err = expectedError = MPE_SUCCESS;

    return_err: if (pat)
        PATSectionFree(pat);

    if (source)
        memFreeP(MPE_MEM_TEST, source);

    if (spec)
        memFreeP(MPE_MEM_TEST, spec);

    if (sect)
        filterSectionRelease(sect);

    sfTerm(sfData);

    CuAssertIntEquals_Msg(tc, msg, expectedError, err);
}

/****************************************************************************
 *
 *  test_getTestSuite_sectionFiltering_Cancel()
 *
 ***************************************************************************/
/**
 * \brief adds cancel related test(s) to a suite
 *
 * Given a CuSuite object, this function adds all the cancel related tests to
 * it.
 *
 * \param suite the suite to add the tests to
 *
 */
void test_getTestSuite_sectionFiltering_Cancel(CuSuite* suite)
{
    SUITE_ADD_TEST(suite, test_filter_sectionFiltering_Cancel);
}
