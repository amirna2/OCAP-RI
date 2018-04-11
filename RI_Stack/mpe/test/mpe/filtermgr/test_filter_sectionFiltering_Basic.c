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
 * \brief Very basic filtering testing, no actual filter happens.
 *
 * The tests in this file simply test very basic requirements of the section
 * filtering API.
 *
 * \author Ric Yeates, Vidiom Systems Corp.
 *
 */

#include "test_filter_sectionFiltering_include.h"
#include <test_media.h>
#include "sectionFiltering_parameters.h"
#include <stdio.h>
#include <ctype.h>

#define TERMINATE 1

typedef struct threadData_t
{
    mpe_EventQueue que;
    mpe_Bool terminate;
    mpe_Bool terminated;
} threadData_t;

static threadData_t sharedData;

static mpe_ThreadId eventThreadID;

static void eventThread(void *_data)
{
    threadData_t *data = (threadData_t *) _data;
    mpe_Event eventId;
    void *eventData;
    mpe_Error err;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "eventThread: start\n");
    while (!data->terminate)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "eventThread: wait for event\n");
        err
                = eventQueueWaitNext(data->que, &eventId, &eventData, NULL,
                        NULL, 0);
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "eventThread: got one (err=%u, id=%s, data=%x)\n", err,
                translateEvent(eventId), eventData);
        if (err != MPE_SUCCESS)
        {
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                    "error getting next event in event thread\n");
            data->terminated = TRUE;
            return;
        }

        switch (eventId)
        {
        case MPE_SF_EVENT_FILTER_CANCELLED:
        case MPE_SF_EVENT_FILTER_PREEMPTED:
        case MPE_SF_EVENT_OUT_OF_MEMORY:
        case MPE_SF_EVENT_SOURCE_CLOSED:
        case MPE_SF_EVENT_SECTION_FOUND:
        case MPE_SF_EVENT_LAST_SECTION_FOUND:
            break;
        case TERMINATE:
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "got TERMINATE\n");
            data->terminated = TRUE;
            return;
        default:
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "unknown event received (%d)\n",
                    eventId);
            break;
        }
    }
}

void test_filter_sectionFiltering_Basic(CuTest *tc);

/****************************************************************************
 *
 *  test_filter_sectionFiltering_Basic()
 *
 ***************************************************************************/
/**
 * \testdescription Tests basic filter API functionality.
 *
 * \api mpe[os]_filterSetFilter()\n
 * mpe[os]_filterRelease()\n
 * mpe[os]_filterGetSectionHandle()\n
 * mpe[os]_filterSectionRelease()\n
 * mpe[os]_filterCancelFilter()\n
 *
 * \strategy Go through the process of creating a simple filter, get a
 * section from it, and tear it down. Along the way, validate various
 * requirements.
 *
 * \assets out-of-band filter source, in-band filter source
 *
 */
void test_filter_sectionFiltering_Basic(CuTest *tc)
{
    mpe_FilterSource *source = NULL;
    uint32_t filter = 0;
    mpe_FilterSpec *spec = NULL;
    mpe_Error err;
    mpe_Error expectedError = MPE_SUCCESS;
    mpe_FilterSectionHandle sect;
    const char *msg;
    struct
    {
        uint8_t posMask[LONG_SIZE];
        uint8_t posVals[LONG_SIZE];
        uint8_t negMask[LONG_SIZE];
        uint8_t negVals[LONG_SIZE];
    } ba; // byte arrays, in struct for memset conveneience
    uint32_t i;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "Start test - test_filter_sectionFiltering_Basic\n");

    err = eventQueueNew(&sharedData.que, "TestSFBasic");
    CuAssertIntEquals_Msg(tc, "failed to create event queue", MPE_SUCCESS, err);

    // start a thread to handle the event queue for all these filters
    sharedData.terminate = FALSE;
    err = threadCreate(eventThread, &sharedData, MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_STACK_SIZE, &eventThreadID, "BasicEventThread");
    if (err != expectedError)
    {
        msg = "failed to create event listener thread\n";
        goto return_err;
    }

    // go to OOB channel
    err = GoToOOBChannel(&source);
    if (err != expectedError)
    {
        msg = "GoToOOBChannel() failed";
        goto return_err;
    }

    // set a filter on it
    source->pid = OOB_PID;

    err = memAllocP(MPE_MEM_TEST, sizeof(*spec), (void **) &spec);
    if (err != MPE_SUCCESS)
    {
        msg = "memory allocation failure";
        goto return_err;
    }

    // filter for some available table ID
    memset(&ba, 0, sizeof(ba));
    spec->pos.length = 1;
    ba.posMask[0] = 0xff;
    spec->pos.mask = ba.posMask;
    ba.posVals[0] = OOB_TABLEID;
    spec->pos.vals = ba.posVals;

    spec->neg.length = 0;

    /**
     * \requirement 2. mpeos_filterSetFilter() should return MPE_SUCCESS if the filter is
     * correctly installed on an out-of-band filter source.
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "'test_filter_sectionFiltering_Basic()' - testing OOB filter source\n");

    err = filterSetFilter(source, spec, sharedData.que, NULL, 1, 1, 0, &filter);
    if (err != expectedError)
    {
        msg = "failed to set PAT filter on OOB source";
        goto return_err;
    }

    memFreeP(MPE_MEM_TEST, source);
    memFreeP(MPE_MEM_TEST, spec);
    source = NULL;
    spec = NULL;

    // remove OOB filter
    err = filterRelease(filter);
    if (err != expectedError)
    {
        msg = "failed to release filter on OOB source";
        goto return_err;
    }
    filter = 0;

    // go to in-band channel for the remainder of the testing
    err = GoToInbandChannel(&source);
    if (err != expectedError)
    {
        msg = "failed to tune to inband source";
        goto return_err;
    }

    err = memAllocP(MPE_MEM_TEST, sizeof(*spec), (void **) &spec);
    if (err != expectedError)
    {
        msg = "memory allocation error\n";
        goto return_err;
    }

    spec->neg.mask = ba.posMask;
    spec->neg.vals = ba.posVals;
    spec->pos.mask = ba.posMask;
    spec->pos.vals = ba.posVals;

    /**
     * \requirement 3. mpeos_filterSetFilter() should accept filters as short as 1 byte.
     */
    // set a short filter (both positive and negative)

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "'test_filter_sectionFiltering_Basic()' - testing short filter\n");

    memset(&ba, 0, sizeof(ba));
    spec->pos.length = 1;
    ba.posMask[0] = 0xff;
    ba.posVals[0] = 0x7f;

    spec->neg.length = 1;
    ba.negMask[0] = 0xff;
    ba.negVals[0] = 0x7f;

    err = filterSetFilter(source, spec, sharedData.que, NULL, 1, 1, 0, &filter);
    if (err != expectedError)
    {
        msg = "failed to set offset varied, 1 byte filter to inband source";
        goto return_err;
    }

    /**
     * \requirement 24. mpeos_filterGetSectionHandle() should return MPE_INVAL if an input
     * parameter is incorrect.
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "'test_filter_sectionFiltering_Basic()' - testing invalid parameters\n");

    expectedError = MPE_EINVAL;
    err = filterGetSectionHandle(0, 0, &sect);
    if (err != expectedError)
    {
        msg = "failed to get MPE_EINVAL for bad uniqueID to GetSectionHandle";
        goto return_err;
    }
    err = filterGetSectionHandle(filter, 0, NULL);
    if (err != expectedError)
    {
        msg = "failed to get MPE_EINVAL for NULL section * to GetSectionHandle";
        goto return_err;
    }
    err = filterGetSectionHandle(filter, ~0, &sect);
    if (err != expectedError)
    {
        msg = "failed to get MPE_EINVAL for bad flags to GetSectionHandle";
        goto return_err;
    }
    expectedError = MPE_SUCCESS;

    // remove filter
    err = filterRelease(filter);
    if (err != expectedError)
    {
        msg = "failed to release filter on inband source";
        goto return_err;
    }
    filter = 0;

    /**
     * \requirement 4. mpeos_filterSetFilter() should accept filters as long as 32 bytes.
     *
     *   set a long filter (both positive and negative)
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "'test_filter_sectionFiltering_Basic()' - testing long filter\n");

    memset(&ba, 0, sizeof(ba));
    spec->pos.length = LONG_SIZE;
    spec->neg.length = LONG_SIZE;
    for (i = 0; i < LONG_SIZE; ++i)
    {
        ba.posMask[0] = (i % 1 == 0 ? 0xff : 0x00);
        ba.posVals[0] = (i % 1 == 0 ? 0xaa : 0x00);
        ba.negMask[0] = (i % 1 == 0 ? 0xf0 : 0x0f);
        ba.negVals[0] = (i % 1 == 0 ? 0xb0 : 0x0b);
    }

    err = filterSetFilter(source, spec, sharedData.que, NULL, 1, 1, 0, &filter);
    if (err != expectedError)
    {
        msg = "failed to set offset varied, long filter to inband source";
        goto return_err;
    }

    // remove filter
    err = filterRelease(filter);
    if (err != expectedError)
    {
        msg = "failed to release filter on inband source";
        goto return_err;
    }
    filter = 0;

    /**
     * \requirement 5. mpeos_filterSetFilter() should accept filters for both positive and
     * negative masks.
     */
    // set an 8-byte filter (positive and negative)
    spec->pos.length = 8;
    spec->neg.length = 8;

    err = filterSetFilter(source, spec, sharedData.que, NULL, 1, 1, 0, &filter);
    if (err != expectedError)
    {
        msg
                = "failed to set 0 offset, 8-byte, both pos and neg on inband source";
        goto return_err;
    }

    // remove filter
    err = filterRelease(filter);
    if (err != expectedError)
    {
        msg = "failed to release filter on inband source";
        goto return_err;
    }
    filter = 0;

    /**
     * \requirement 6. mpeos_filterSetFilter() should accept filters for positive-only
     * masks.
     */
    // set an 8-byte filter (positive only)
    spec->pos.length = 8;
    spec->neg.length = 0;

    err = filterSetFilter(source, spec, sharedData.que, NULL, 1, 1, 0, &filter);
    if (err != expectedError)
    {
        msg = "failed to set non-table-id, 8-byte, pos-only on inband source";
        goto return_err;
    }

    // remove filter
    err = filterRelease(filter);
    if (err != expectedError)
    {
        msg = "failed to release filter on inband source";
        goto return_err;
    }
    filter = 0;

    /**
     * \requirement 7. mpeos_filterSetFilter() should accept filters for negative-only
     * masks.
     */
    // set an 8-byte filter (negative only)
    spec->pos.length = 0;
    spec->neg.length = 8;

    err = filterSetFilter(source, spec, sharedData.que, NULL, 1, 1, 0, &filter);
    if (err != expectedError)
    {
        msg = "failed to set non-table-id, 8-byte, neg-only on inband source";
        goto return_err;
    }

    // remove filter
    err = filterRelease(filter);
    if (err != expectedError)
    {
        msg = "failed to release filter on inband source";
        goto return_err;
    }

    /**
     * \requirement 39. mpeos_filterCancelFilter() should return MPE_EINVAL if an invalid
     * filter ID is specified.
     */
    expectedError = MPE_EINVAL;
    err = filterCancelFilter(filter);
    if (err != expectedError)
    {
        msg
                = "failed to get MPE_INVAL for already released filter to CancelFilter\n";
        filter = 0;
        goto return_err;
    }
    err = filterCancelFilter(filter + 1);
    if (err != expectedError)
    {
        filter = 0;
        msg = "failed to get MPE_INVAL for old filter plus 1 to CancelFilter\n";
        goto return_err;
    }

    filter = 0;
    err = filterCancelFilter(0);
    if (err != expectedError)
    {
        msg = "failed to get MPE_INVAL for invalid filter ID to CancelFilter\n";
        goto return_err;
    }

    /**
     * \requirement 8. mpeos_filterSetFilter() should return MPE_EINVAL if a parameter has an
     * invalid value.
     */
    // try invalid parameters to filterSetFilter one at a time
    err = filterSetFilter(NULL, spec, sharedData.que, NULL, 1, 1, 0, &filter);
    if (err != MPE_EINVAL)
    {
        msg = "failed to get EINVAL for NULL source\n";
        goto return_err;
    }
    err = filterSetFilter(source, NULL, sharedData.que, NULL, 1, 1, 0, &filter);
    if (err != MPE_EINVAL)
    {
        msg = "failed to get EINVAL for NULL spec\n";
        goto return_err;
    }
    err = filterSetFilter(source, spec, sharedData.que, NULL, 1, 1, ~0, NULL);
    if (err != MPE_EINVAL)
    {
        msg = "failed to get EINVAL for bogus flags\n";
        goto return_err;
    }
    err = filterSetFilter(source, spec, sharedData.que, NULL, 1, 1, 0, NULL);
    if (err != MPE_EINVAL)
    {
        msg = "failed to get EINVAL for NULL return parameter\n";
        goto return_err;
    }

    TRACE(MPE_LOG_DEBUG, MPE_MOD_FILTER,
            "End test - test_filter_sectionFiltering_Basic\n");

    return;

    return_err: if (filter)
    {
        filterCancelFilter(filter);
        filterRelease(filter);
        filter = 0;
    }

    if (spec)
    {
        memFreeP(MPE_MEM_TEST, spec);
        spec = NULL;
    }

    if (source)
    {
        memFreeP(MPE_MEM_TEST, source);
        source = NULL;
    }

    if (eventThreadID)
    {
        eventQueueSend(sharedData.que, TERMINATE, NULL, NULL, 0);
        while (!sharedData.terminated)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_FILTER,
                    "waiting for event thread to terminate\n");
            threadSleep(100, 0);
        }
        threadDestroy(eventThreadID);
        eventThreadID = (mpe_ThreadId) 0;
    }

    if (sharedData.que)
    {
        eventQueueDelete(sharedData.que);
        sharedData.que = (mpe_EventQueue) 0;
    }

    CuAssertIntEquals_Msg(tc, msg, expectedError, err);
}

/****************************************************************************
 *
 *  test_getTestSuite_sectionFiltering_Basic()
 *
 ***************************************************************************/
/**
 * \brief adds basic filter test(s) to a suite
 *
 * Given a CuSuite object, this function adds all the basic filter tests to
 * it.
 *
 * \param suite the suite to add the tests to
 *
 */
void test_getTestSuite_sectionFiltering_Basic(CuSuite* suite)
{
    SUITE_ADD_TEST(suite, test_filter_sectionFiltering_Basic);
}
