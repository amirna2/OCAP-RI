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
 * \brief OOB SCTE 65 table change filtering
 *
 * This file, making heavy use of the sf database support in Utils.c.
 * It acquires SVCT sections and waits for a version change.
 * 
 * \author Ric Yeates, Vidiom Systems Corp.
 *
 */

#include "test_filter_sectionFiltering_include.h"
#include "sectionFiltering_parameters.h"
#include <stdio.h>
#include <ctype.h>

void test_filter_sectionFiltering_OOBChange(CuTest *tc);
void test_getTestSuite_sectionFiltering_OOBChange(CuSuite* suite);

static sfData_t sfSharedData;

#define REV_DET_DESC_ID			0x93	// revision detection descriptor ID
#define REV_DET_DESC_SIZE		5		// revision detection descriptor size
#define DESC_LENGTH_OFFSET		1		// offset to descriptor length within descriptor
#define SVCT_STID_OFFSET		4		// SVCT table subtype ID offset
#define SVCT_STRUCT_OFFSET		7		// SVCT offset to start of structures
#define SVCT_DESC_INC_OFFSET	0		// SVCT offset to descriptors_included in VCM structure
#define SVCT_NOVR_OFFSET		6		// SVCT offset to number_of_VC_records in VCM structure
#define SVCT_VC_OFFSET			7		// SVCT offset to virual channel records in VCM structure
#define SVCT_VC_DESC_OFFSET		9		// SVCT offset to possible descriptors

/****************************************************************************
 *
 *  test_filter_sectionFiltering_OOBChange()
 *
 ***************************************************************************/
/**
 * \testdescription Tests revision detection descriptors and SCTE 65 table
 * change support in the OOB SF code.
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
void test_filter_sectionFiltering_OOBChange(CuTest *tc)
{
    mpe_FilterSource *source = NULL;
    SectionFilter_t *svct_filter;
    mpe_FilterSpec spec;
    mpe_Error err;
    sfData_t *sfData = &sfSharedData;
    char *msg = "";
    uint8_t posMask[8];
    uint8_t posVals[8];
    mpe_Bool first;
    mpe_FilterSectionHandle sect;
    mpe_Bool changeSeen;
    uint8_t version;
    uint8_t seenVersion = -1;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "Start test - test_filter_sectionFiltering_OOBChange\n");

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

    // SVCT VCM
    spec.pos.length = 5;
    memcpy(spec.pos.mask, "\xff\x00\x00\x00\x0f", 5);
    memcpy(spec.pos.vals, "\xc4\x00\x00\x00\x00", 5);

    first = TRUE;
    changeSeen = FALSE;
    while (changeSeen == FALSE)
    {
        char data[4096];
        uint32_t sectSize;
        uint8_t number_of_VC_records;
        uint8_t descriptors_included;
        uint8_t *place;

        if (!first)
            threadSleep(5000, 0);

        err = sfNew(sfData, /* shared data */
        source, /* filterSource */
        &spec, /* filterSpec */
        1, /* priority */
        1, /* times to match */
        0, /* flags */
        &svct_filter); /* returned filter pointer */
        if (err != MPE_SUCCESS)
        {
            msg = "failed to create SVCT VCM filter";
            goto return_err;
        }

        // start the single-shot
        err = sfStart(sfData, svct_filter);
        if (err != MPE_SUCCESS)
        {
            msg = "failed to start SVCT filter";
            goto return_err;
        }

        // get a section from it
        err = sfGetSection(sfData, svct_filter, &sect);
        if (err != MPE_SUCCESS)
        {
            msg = "dumpFilters: failed to get a section for SVCT filter";
            goto return_err;
        }

        err = filterSectionRead(sect, 0, sizeof(data), 0,
                (unsigned char *) data, &sectSize);
        if (err != MPE_SUCCESS)
        {
            msg = "failed to read section";
            goto return_err;
        }

        // seek the revision detection descriptor
        place = (unsigned char *) data + SVCT_STRUCT_OFFSET;

        descriptors_included = place[SVCT_DESC_INC_OFFSET] & 0x20;
        number_of_VC_records = place[SVCT_NOVR_OFFSET];

        place += SVCT_VC_OFFSET;

        while (number_of_VC_records--)
        {
            place += SVCT_VC_DESC_OFFSET;

            if (descriptors_included)
            {
                uint8_t descriptors_included = *place++;

                while (descriptors_included--)
                    place += place[DESC_LENGTH_OFFSET];
            }
        }

        // revision detection descriptor better be at place now
        if (place[0] != REV_DET_DESC_ID)
        {
            msg = "revision detection descriptor not found";
            goto return_err;
        }

        if (place[1] != REV_DET_DESC_SIZE)
        {
            msg = "wrong size detection descriptor found";
            goto return_err;
        }

        version = place[2] & 0x1f;

        if (first)
        {
            // remember the version information
            seenVersion = version;
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                    "Saw version %u, waiting for change.\n", version);
            first = FALSE;
        }
        else
        {
            // we already know a version, check for change
            if (version != seenVersion)
            {
                TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                        "\nSaw version new %u, done waiting for change.\n",
                        version);
                changeSeen = TRUE;
            }
            else
                TRACE(MPE_LOG_INFO, MPE_MOD_TEST, ".");
        }

        err = filterSectionRelease(sect);
        if (err != MPE_SUCCESS)
        {
            msg = "failed to release section";
            goto return_err;
        }

        err = sfDelete(sfData, svct_filter);
        if (err != MPE_SUCCESS)
        {
            msg = "failed to delete filter";
            goto return_err;
        }
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
 *  test_getTestSuite_sectionFiltering_OOBChange()
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
void test_getTestSuite_sectionFiltering_OOBChange(CuSuite* suite)
{
    SUITE_ADD_TEST(suite, test_filter_sectionFiltering_OOBChange);
}

