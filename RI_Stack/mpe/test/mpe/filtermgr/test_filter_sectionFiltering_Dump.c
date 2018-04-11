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
 * \brief PAT and PMT filtering.
 *
 * This file, making heavy use of the sf database support in Utils.c, tunes
 * to an in-band source and prints out the PAT and first N PMTs.
 * 
 * Along the way it verifies various requirements.
 *
 * \author Ric Yeates, Vidiom Systems Corp.
 *
 */

#include "test_filter_sectionFiltering_include.h"
#include "sectionFiltering_parameters.h"
#include <stdio.h>
#include <ctype.h>

void test_filter_sectionFiltering_Dump(CuTest *tc);

static sfData_t sfSharedData;

/****************************************************************************
 *
 *  test_filter_sectionFiltering_Dump()
 *
 ***************************************************************************/
/**
 * \testdescription Tests section filtering by setting up a PAT filter and
 * then DUMP_MAX_PMTS PMT filters.
 *
 * \api mpe[os]_filterSetFilter()\n
 * MPE_SF_EVENT_LAST_SECTION_FOUND\n
 * mpe[os]_filterGetSectionHandle()\n
 * mpe[os]_filterGetSectionSize()\n
 * mpe[os]_filterSectionRead()\n
 * mpe[os]_filterSectionRelease()\n
 * mpe[os]_filterRelease()\n
 *
 * \strategy Tunes to an in-band source, sets up a PAT filter, prints the PAT
 * found there, releases PAT filter, sets up DUMP_MAX_PMTS PMT filters, prints
 * PMTs found, and releases PMT filters.
 *
 * \assets in-band filter source
 * 
 * \note This test was written with the intention that one day it might
 * want to be expanded to do more filtering from object carousel streams or
 * whatever other streams the PMTs might reveal.
 * 
 * \note Note that this test is not specifically written to satisfy any
 * requirements. That does not mean it's not valueable, it is the most
 * "useful" test in this suite.
 */
void test_filter_sectionFiltering_Dump(CuTest *tc)
{
    mpe_FilterSource *source = NULL;
    SectionFilter_t *pat_filter;
    mpe_FilterSpec *spec = NULL;
    mpe_Error err;
    mpe_FilterSectionHandle sect;
    PAT_t *pat;
    sfData_t *sfData = &sfSharedData;
    char *msg;
    SectionFilter_t *pmt_filter[DUMP_MAX_PMTS];
    uint32_t pmt_count;
    ProgramPIDMap_t *map;
    PMT_t *pmt;
    uint32_t i;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "Start test - test_filter_sectionFiltering_Dump\n");

    err = sfInit(sfData, tc);
    if (err != MPE_SUCCESS)
    {
        msg = "failed to initialize section filter DB";
        goto return_err;
    }

    // go to inband channel
    err = GoToInbandChannel(&source);
    if (err != MPE_SUCCESS)
    {
        msg = "GoToInbandChannel() failed";
        goto return_err;
    }

    // filter for the PAT
    err = PATSectionFilter(source, &spec);
    if (err != MPE_SUCCESS)
    {
        msg = "failed to create PAT section filter spec";
        goto return_err;
    }

    err = sfNew(sfData, source, spec, 1, 1, 0, &pat_filter);
    if (err != MPE_SUCCESS)
    {
        msg = "failed to create filter DB entry for PAT section filter";
        goto return_err;
    }

    memFreeP(MPE_MEM_TEST, spec);
    spec = NULL;

    err = sfStart(sfData, pat_filter);
    if (err != MPE_SUCCESS)
    {
        msg = "failed to start filter for PAT section filter";
        goto return_err;
    }

#if defined(FILTERING_SHIM)
    // The filtering shim has a way to make sections arrive
    mpeos_filterGetPAT(pat_filter->uid);
#endif

    err = sfGetSection(sfData, pat_filter, &sect);
    if (err != MPE_SUCCESS)
    {
        msg = "failed to get a section for PAT section filter";
        goto return_err;
    }

    err = PATSectionParse(sect, &pat);
    if (err != MPE_SUCCESS)
    {
        msg = "failed to parse PAT section from PAT section filter";
        goto return_err;
    }

    PATSectionDump(pat);

    err = filterSectionRelease(sect);
    if (err != MPE_SUCCESS)
    {
        msg = "failed to release PAT section from PAT section filter";
        goto return_err;
    }
    sect = 0;

    err = sfDelete(sfData, pat_filter);
    if (err != MPE_SUCCESS)
    {
        msg = "failed to delete PAT section filter";
        goto return_err;
    }
    pat_filter = NULL;

    // create filters for the first DUMP_MAX_PMTS PMT's
    pmt_count = 0;
    map = pat->map;
    while (map && pmt_count < DUMP_MAX_PMTS)
    {
        if (map->program_number)
        {
            err = PMTSectionFilter(source, map->program_number, &spec);
            if (err != MPE_SUCCESS)
            {
                msg = "failed to create PMT section filter spec";
                goto return_err;
            }
            source->pid = map->pid;

            err = sfNew(sfData, source, spec, 1, 1, 0, &pmt_filter[pmt_count]);
            if (err != MPE_SUCCESS)
            {
                msg = "failed to create PMT section filter";
                goto return_err;
            }

            ++pmt_count;
        }

        map = map->next;
    }

#if defined(FILTERING_SHIM)
    map = pat->map;
#endif

    // start PMT filters
    for (i = 0; i < pmt_count; ++i)
    {
        err = sfStart(sfData, pmt_filter[i]);
        if (err != MPE_SUCCESS)
        {
            msg = "failed to start a PMT filter";
            goto return_err;
        }

#if defined(FILTERING_SHIM)
        // The filtering shim has a way to make sections arrive
        mpeos_filterGetPMT(pmt_filter[i]->uid, map->program_number);
        threadSleep(100, 0);
        map = map->next;
#endif
    }

    // we're done with the PAT structure
    PATSectionFree(pat);
    pat = NULL;

    for (i = 0; i < pmt_count; ++i)
    {
        // wait for the PMT to show up
        err = sfGetSection(sfData, pmt_filter[i], &sect);
        if (err != MPE_SUCCESS)
        {
            msg = "failed to get a section from a PMT filter";
            goto return_err;
        }

        err = PMTSectionParse(sect, &pmt);
        if (err != MPE_SUCCESS)
        {
            msg = "failed to parse PMT section from PMT section filter";
            goto return_err;
        }

        // print all the information from the PMT
        PMTSectionDump(pmt);

        PMTSectionFree(pmt);
        pmt = NULL;

        err = filterSectionRelease(sect);
        if (err != MPE_SUCCESS)
        {
            msg = "failed to release PMT section from PMT section filter";
            goto return_err;
        }
        sect = 0;

        err = sfDelete(sfData, pmt_filter[i]);
        if (err != MPE_SUCCESS)
        {
            msg = "failed to delete PMT section filter";
            goto return_err;
        }
        pmt_filter[i] = NULL;
    }

    msg = "";
    err = MPE_SUCCESS;

    return_err: if (pat)
        PATSectionFree(pat);

    if (pmt)
        PMTSectionFree(pmt);

    if (spec)
        memFreeP(MPE_MEM_TEST, spec);

    if (source)
        memFreeP(MPE_MEM_TEST, source);

    sfTerm(sfData);

    CuAssertIntEquals_Msg(tc, msg, MPE_SUCCESS, err);
}

/****************************************************************************
 *
 *  test_getTestSuite_sectionFiltering_Dump()
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
void test_getTestSuite_sectionFiltering_Dump(CuSuite* suite)
{
    SUITE_ADD_TEST(suite, test_filter_sectionFiltering_Dump);
}
