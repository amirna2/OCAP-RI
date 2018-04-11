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
 * \brief OOB SCTE 65 section filtering
 *
 * This file, making heavy use of the sf database support in Utils.c.
 * It acquires XAITs, EASs, NITs, and SVCTs from the OOB source. It dumps
 * them for inspection by the user.
 * 
 * \author Ric Yeates, Vidiom Systems Corp.
 *
 */

#include "test_filter_sectionFiltering_include.h"
#include "sectionFiltering_parameters.h"
#include <stdio.h>
#include <ctype.h>

void test_filter_sectionFiltering_OOBDump(CuTest *tc);
void test_getTestSuite_sectionFiltering_OOBDump(CuSuite* suite);

static sfData_t sfSharedData;

static void dumpFilters(CuTest *tc, // test case
        sfData_t *sfData, // shared data
        SectionFilter_t **filter, // array of filters
        uint32_t n, // number in array
        uint32_t timesToMatch) // filter times to match
{
    uint32_t i;
    uint32_t ttm;
    mpe_FilterSectionHandle sect;
    mpe_Error err = MPE_SUCCESS;
    char *msg = "dumpFilters: no errors";

    for (i = 0; i < n; ++i)
    {
        err = sfStart(sfData, filter[i]);
        if (err != MPE_SUCCESS)
        {
            msg = "dumpFilters: failed to start filter";
            goto return_err;
        }
    }

    for (i = 0; i < n; ++i)
    {
        for (ttm = 0; ttm < timesToMatch; ++ttm)
        {
            err = sfGetSection(sfData, filter[i], &sect);
            if (err != MPE_SUCCESS)
            {
                msg = "dumpFilters: failed to get a section for filter";
                goto return_err;
            }

            TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Filter #%u, match #%u:\n",
                    i + 1, ttm + 1);
            err = sfDumpSection(sfData, sect);
            if (err != MPE_SUCCESS)
            {
                msg = "dumpFilters: failed to dump section from filter";
                goto return_err;
            }

            err = filterSectionRelease(sect);
            if (err != MPE_SUCCESS)
            {
                msg = "dumpFilters: failed to release section from filter";
                goto return_err;
            }
        }
    }

    return_err: CuAssertIntEquals_Msg(tc, msg, MPE_SUCCESS, err);
}

/****************************************************************************
 *
 *  test_filter_sectionFiltering_OOBDump()
 *
 ***************************************************************************/
/**
 * \testdescription Tests section filtering by setting up various OOB filters
 * and then dumping the arriving sections.
 *
 * \api mpe[os]_filterSetFilter()\n
 * MPE_SF_EVENT_SECTION_FOUND\n
 * MPE_SF_EVENT_LAST_SECTION_FOUND\n
 * mpe[os]_filterGetSectionHandle()\n
 * mpe[os]_filterGetSectionSize()\n
 * mpe[os]_filterSectionRead()\n
 * mpe[os]_filterSectionRelease()\n
 * mpe[os]_filterRelease()\n
 *
 * \strategy Sets up various filters and dumps the resulting sections.
 *
 * \assets out-of-band filter source
 * 
 */
void test_filter_sectionFiltering_OOBDump(CuTest *tc)
{
    mpe_FilterSource *source = NULL;
    SectionFilter_t *oob_filter[10];
    mpe_FilterSpec spec;
    mpe_Error err;
    sfData_t *sfData = &sfSharedData;
    char *msg;
    uint32_t i;
    uint8_t posMask[8];
    uint8_t posVals[8];

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "Start test - test_filter_sectionFiltering_OOBDump\n");

    err = sfInit(sfData, tc);
    if (err != MPE_SUCCESS)
    {
        msg = "failed to initialize section filter DB";
        goto return_err;
    }

    // go to inband channel
    err = GoToOOBChannel(&source);
    if (err != MPE_SUCCESS)
    {
        msg = "GoToOOBChannel() failed";
        goto return_err;
    }

    // everything's on PID 0x1ffc */
    source->pid = 0x1ffc;

    spec.neg.length = 0;
    spec.pos.mask = posMask;
    spec.pos.vals = posVals;

    // Filter for all the kinds of NIT filters we know at one time, 2 per filter
    // NIT tableID-only
    spec.pos.length = 1;
    spec.pos.mask[0] = 0xff;
    spec.pos.vals[0] = 0xc2;

    err = sfNew(sfData, /* shared data */
    source, /* filterSource */
    &spec, /* filterSpec */
    1, /* priority */
    2, /* times to match */
    0, /* flags */
    &oob_filter[0]); /* returned filter pointer */
    if (err != MPE_SUCCESS)
    {
        msg = "failed to create NIT tableID-only filter";
        goto return_err;
    }

    // NIT CDS
    spec.pos.length = 7;
    memcpy(spec.pos.mask, "\xff\x00\x00\x00\x00\x00\x0f", 7);
    memcpy(spec.pos.vals, "\xc2\x00\x00\x00\x00\x00\x01", 7);

    err = sfNew(sfData, /* shared data */
    source, /* filterSource */
    &spec, /* filterSpec */
    1, /* priority */
    2, /* times to match */
    0, /* flags */
    &oob_filter[1]); /* returned filter pointer */
    if (err != MPE_SUCCESS)
    {
        msg = "failed to create NIT CDS filter";
        goto return_err;
    }

    // NIT MMS
    spec.pos.length = 7;
    memcpy(spec.pos.mask, "\xff\x00\x00\x00\x00\x00\x0f", 7);
    memcpy(spec.pos.vals, "\xc2\x00\x00\x00\x00\x00\x02", 7);

    err = sfNew(sfData, /* shared data */
    source, /* filterSource */
    &spec, /* filterSpec */
    1, /* priority */
    2, /* times to match */
    0, /* flags */
    &oob_filter[2]); /* returned filter pointer */
    if (err != MPE_SUCCESS)
    {
        msg = "failed to create NIT MMS filter";
        goto return_err;
    }

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Filter #1: NIT (table ID only)\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Filter #2: NIT CDS\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Filter #3: NIT MMS\n");

    dumpFilters(tc, sfData, oob_filter, 3 /* number of filters */, 2 /* times to match */);

    for (i = 0; i < 3; ++i)
    {
        err = sfDelete(sfData, oob_filter[i]);
        if (err != MPE_SUCCESS)
        {
            msg = "failed to delete filter";
            goto return_err;
        }
        oob_filter[i] = NULL;
    }

    // Filter for all the kinds of SVCT filters we know at one time, 2 per filter
    // SVCT tableID-only
    spec.pos.length = 1;
    spec.pos.mask[0] = 0xff;
    spec.pos.vals[0] = 0xc4;

    err = sfNew(sfData, /* shared data */
    source, /* filterSource */
    &spec, /* filterSpec */
    1, /* priority */
    2, /* times to match */
    0, /* flags */
    &oob_filter[0]); /* returned filter pointer */
    if (err != MPE_SUCCESS)
    {
        msg = "failed to create SVCT tableID-only filter";
        goto return_err;
    }

    // SVCT VCM
    spec.pos.length = 5;
    memcpy(spec.pos.mask, "\xff\x00\x00\x00\x0f", 5);
    memcpy(spec.pos.vals, "\xc4\x00\x00\x00\x00", 5);

    err = sfNew(sfData, /* shared data */
    source, /* filterSource */
    &spec, /* filterSpec */
    1, /* priority */
    2, /* times to match */
    0, /* flags */
    &oob_filter[1]); /* returned filter pointer */
    if (err != MPE_SUCCESS)
    {
        msg = "failed to create SVCT VCM filter";
        goto return_err;
    }

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Filter #1: SVCT (table ID only)\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Filter #2: SVCT VCM\n");

    dumpFilters(tc, sfData, oob_filter, 2 /* number of filters */, 2 /* times to match */);

    for (i = 0; i < 2; ++i)
    {
        err = sfDelete(sfData, oob_filter[i]);
        if (err != MPE_SUCCESS)
        {
            msg = "failed to delete filter";
            goto return_err;
        }
        oob_filter[i] = NULL;
    }

    msg = "";
    err = MPE_SUCCESS;

    return_err: if (source)
        memFreeP(MPE_MEM_TEST, source);

    sfTerm(sfData);

    CuAssertIntEquals_Msg(tc, msg, MPE_SUCCESS, err);
}

/****************************************************************************
 *
 *  test_getTestSuite_sectionFiltering_OOBDump()
 *
 ***************************************************************************/
/**
 * \brief adds dump test(s) to a suite
 *
 * Given a CuSuite object, this function adds all the dump tests to it.
 *
 * \param suite the suite to add the tests to
 *
 */
void test_getTestSuite_sectionFiltering_OOBDump(CuSuite* suite)
{
    SUITE_ADD_TEST(suite, test_filter_sectionFiltering_OOBDump);
}

