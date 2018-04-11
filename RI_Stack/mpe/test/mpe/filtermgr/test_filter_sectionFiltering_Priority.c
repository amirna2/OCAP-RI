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
 * \brief Test handling of filter priorities.
 *
 * The tests in the file ensure that the basics of filter priorities are
 * working.
 *
 * \author Ric Yeates, Vidiom Systems Corp.
 *
 */
#include "test_filter_sectionFiltering_include.h"
#include "sectionFiltering_parameters.h"
#include <stdio.h>
#include <ctype.h>

void test_filter_sectionFiltering_Priority(CuTest *tc);

static sfData_t sfSharedData;

/****************************************************************************
 *
 *  test_filter_sectionFiltering_Priority()
 *
 ***************************************************************************/
/**
 * \testdescription Tests basic filter priority functionality.
 *
 * \api mpe[os]_filterSetFilter()\n
 * mpe[os]_filterRelease()\n
 * mpe[os]_filterCancelFilter()\n
 *
 * \strategy Create filters from lowest to highest priority until one of them
 * is preempted. Create another one at the preempted priority and ensure it
 * cannot be created. Make sure no filters of lower priority are kept in favor
 * filters at a higher priority.
 *
 * \assets in-band filter source
 *
 */
void test_filter_sectionFiltering_Priority(CuTest *tc)
{
    mpe_FilterSource *source = NULL;
    mpe_FilterSpec spec;
    uint8_t posMask[LONG_SIZE];
    uint8_t posVals[LONG_SIZE];
    mpe_Error err;
    mpe_Error expectedError;
    sfData_t *sfData = &sfSharedData;
    char *msg;
    uint8_t priority;
    uint8_t highestCancelled; // highest priority of the cancelled filters
    uint8_t lowestActive; // lowest priority of remaining active filters
    SectionFilter_t *sf;
    SectionFilter_t *sfNext;
    mpe_Bool firstPreempt = TRUE;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "Start test - test_filter_sectionFiltering_Priority\n");

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

    // create filter spec for something we won't likely see
    spec.pos.length = LONG_SIZE;
    spec.pos.mask = posMask;
    spec.pos.vals = posVals;
    memset(posMask, 0xff, sizeof(posMask));
    memset(posVals, 0xa5, sizeof(posVals));
    posVals[0] = 0x00; // PAT
    posVals[1] = 0x4f; // syntax bit not set, high bits of length set
    posVals[2] = 0x00; // start at 0xf00 for section length
    spec.neg.length = 0;
    source->pid = 0x01; // hopefully no PATs here

#if LONG_SIZE < 6
#error LONG_SIZE too short
#endif
#define CHANGESPEC() (++posVals[2], ++posVals[LONG_SIZE / 2], --posVals[LONG_SIZE - 6])

    /**
     * \requirement 16. Lower priority numbered filters supercede higher priority numbered
     * filters given the same filter source. The highest priority is 1 and the
     * lowest priority is 255.
     */
    // for each priority from lowest to highest
    for (priority = 255; priority > 0; --priority)
    {
        // put an infinite filter of that priority on the source
        err = sfNew(sfData, source, &spec, priority, 0, 0, &sf);
        if (err != expectedError)
        {
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                    "failed to create filter of priority %u, err = %s\n",
                    priority, translateError(err));
            msg = "failed to create filter in DB";
            goto return_err;
        }

        err = sfStart(sfData, sf);
        if (err != expectedError)
        {
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                    "failed to start filter of priority %u, err = %s\n",
                    priority, translateError(err));
            msg = "failed to start filter from DB";
            goto return_err;
        }

        // give events some time to happen
        threadSleep(100, 0);

        // for each filter already put on the source
        highestCancelled = 0;
        lowestActive = 0;
        for (sf = sfData->filters; sf; sf = sfNext)
        {
            sfNext = sf->next;

            // has the filter been cancelled
            if (sf->state == STATE_CANCELLED)
            {
                /**
                 * \requirement 17. Send MPE_SF_EVENT_FILTER_PREEMPTED only when a filter is overridden
                 * with a higher priority filter. The supporting data is the filter�s
                 * uniqueID.
                 */
                // make sure we got the right event
                if (sf->eventId != MPE_SF_EVENT_FILTER_PREEMPTED)
                {
                    TRACE(
                            MPE_LOG_INFO,
                            MPE_MOD_TEST,
                            "got wrong event for preempted filter: expected MPE_SF_EVENT_FILTER_PREEMPTED, got %s\n",
                            translateEvent(sf->eventId));
                    msg
                            = "got wrong event for preempted filter: expected MPE_SF_EVENT_FILTER_PREEMPTED\n";
                    err = MPE_EINVAL;
                    goto return_err;
                }

                // remember priority has highestCancelled if so
                if (highestCancelled == 0 || sf->priority < highestCancelled)
                    highestCancelled = sf->priority;

                // delete the filter from the database
                err = sfDelete(sfData, sf);
                if (err != expectedError)
                {
                    msg = "failed to delete filter from DB";
                    goto return_err;
                }

                // start the scan all over again
                sfNext = sfData->filters;
            }
            else if (lowestActive == 0 || sf->priority > lowestActive)
                lowestActive = sf->priority;
        }

        // scan remaining non-cancelled filters for any lower than the highest
        // priority cancelled filter (if one was cancelled at all)
        if (highestCancelled != 0)
        {
            for (sf = sfData->filters; sf; sf = sf->next)
            {
                if (sf->priority > highestCancelled)
                {
                    err = MPE_EINVAL;
                    TRACE(
                            MPE_LOG_INFO,
                            MPE_MOD_TEST,
                            "filter of priority %u not preempted after filter of priority %u preempted!\n",
                            sf->priority, highestCancelled);
                    msg = "non-lowest priority filter cancelled\n";
                    goto return_err;
                }
            }

            //
            // was this the first preempted filter so
            // far and was there at least one active filter?
            //
            if (firstPreempt && lowestActive != 0)
            {
                // should fail to put another filter at same priority as lowest active
                CHANGESPEC();

                err = sfNew(sfData, source, &spec, lowestActive, 0, 0, &sf);
                if (err != expectedError)
                {
                    TRACE(
                            MPE_LOG_INFO,
                            MPE_MOD_TEST,
                            "failed to create second filter of priority %u, err = %s\n",
                            lowestActive, translateError(err));
                    msg = "failed to create duplicate filter in DB";
                    goto return_err;
                }

                /**
                 * \requirement 10. mpeos_filterSetFilter() should return MPE_SF_ERROR_FILTER_NOT_AVAILABLE
                 * if there are not enough filter resources to satisfy the request.
                 */
                expectedError = MPE_SF_ERROR_FILTER_NOT_AVAILABLE;
                err = sfStart(sfData, sf);
                if (err != expectedError)
                {
                    TRACE(
                            MPE_LOG_INFO,
                            MPE_MOD_TEST,
                            "failed to get right error for priority tie, got %s expected MPE_SF_ERROR_FILTER_NOT_AVAILABLE\n",
                            translateError(err));
                    msg = "didn't fail to start priority tie filter";
                    goto return_err;
                }
                expectedError = MPE_SUCCESS;

                err = sfDelete(sfData, sf);
                if (err != expectedError)
                {
                    msg = "failed to delete priority tie filter";
                    goto return_err;
                }

                firstPreempt = FALSE;
            }
        }

        CHANGESPEC();
    }

    // try a filter at priority 0, should be an error?
    err = sfNew(sfData, source, &spec, 0, 0, 0, &sf);
    if (err != expectedError)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "failed to create filter of priority 0, err = %s\n",
                translateError(err));
        msg = "failed to create filter in DB";
        goto return_err;
    }

    expectedError = MPE_EINVAL;
    err = sfStart(sfData, sf);
    if (err != expectedError)
    {
        TRACE(
                MPE_LOG_INFO,
                MPE_MOD_TEST,
                "failed to get %s for start of filter of priority 0, err = %s\n",
                translateError(err), translateError(err));
        msg = "failed to get error for priority 0 filter";
        goto return_err;
    }
    expectedError = MPE_SUCCESS;

    err = sfDelete(sfData, sf);
    if (err != expectedError)
    {
        msg = "failed to delete filter priority 0 filter from DB";
        goto return_err;
    }

    msg = "";
    err = expectedError = MPE_SUCCESS;

    return_err: if (source)
        memFreeP(MPE_MEM_TEST, source);

    sfTerm(sfData);

    CuAssertIntEquals_Msg(tc, msg, expectedError, err);
}

/****************************************************************************
 *
 *  test_getTestSuite_sectionFiltering_Priority()
 *
 ***************************************************************************/
/**
 * \brief adds priority related tests to a suite
 *
 * Given a CuSuite object, this function adds all the priority related tests to
 * it.
 *
 * \param suite the suite to add the tests to
 *
 */
void test_getTestSuite_sectionFiltering_Priority(CuSuite* suite)
{
    SUITE_ADD_TEST(suite, test_filter_sectionFiltering_Priority);
}
