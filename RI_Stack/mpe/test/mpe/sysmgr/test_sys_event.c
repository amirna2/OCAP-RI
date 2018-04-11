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

/** \file test_sys_event.c
 *
 *  \brief Test functions for MPEOS event functions
 *
 *  This file contains tests for the following MPEOS functions :\n
 *    -# mpeos_eventQueueDelete(),\n
 *    -# mpeos_eventQueueNew(),\n
 *    -# mpeos_eventQueueNext(),\n
 *    -# mpeos_eventQueueSend(),\n
 *    -# mpeos_eventQueueWaitNext(),\n
 */

#define _MPE_EVENT
#define TEST_MPE_EVENT

#include <cutest.h>
#include <mpe_sys.h>
#include "test_sys.h"

#define DELAY (1)
#define PING_EVENT (0x55)
#define PONG_EVENT (0x22)
#define EVID_INIT  ((mpe_Event)51105)  /*!< nothing magic about this value,
                                            just something to check for */
#define WAITTIMEOUT ((uint32_t)500)      /*!< timeout value for 'WaitNext()' tests */ 

static CuTest *m_mpetest_syncTest; /* Current test object  */
static mpe_EventQueue m_mpetest_pingQ = NULL;
static mpe_EventQueue m_mpetest_pongQ = NULL;
static uint32_t m_ThreadStatus; /* Shared variable for status notification. */

static CuTest *m_mpetest_evtc;
static uint32_t m_mpetest_ping, m_mpetest_pong;
// static unsigned m_mpetest_done;

/*  Function to add event tests to a test suite  */

CuSuite* getTestSuite_sysEvent(void);

/* Event test functions */

static void vte_test_eventQueueNew(CuTest *tc);
static void vte_test_eventQueueDelete(CuTest *tc);
static void vte_test_eventQueueSend(CuTest *tc);
static void vte_test_eventQueueNext(CuTest *tc);
static void vte_test_eventQueueWaitNext(CuTest *tc);
static void vte_test_eventSendThread(CuTest *tc);
static void vte_test_eventPingPong(CuTest *tc);

/*  Internal functions  */

static void testEvent_SendThread(void *);
static void testEvent_EventThread(void *);

/****************************************************************************
 *
 *  vte_test_eventQueueNew()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_eventQueueNew()" function.
 *
 * \api mpeos_eventQueueNew()
 *
 * \strategy Call the "mpeos_eventQueueNew()" function with various legal
 *  and illegal input values and and check for expected results.
 *
 * \assets none
 *
 */

static void vte_test_eventQueueNew(CuTest *tc)
{
    mpe_Error ec;
    mpe_EventQueue evq;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_eventQueueNew\n");

    /**
     * \assertion "MPE_EINVAL" is returned if a NULL pointer is passed to 
     *  'mpeos_eventQueueNew()'.
     */

    ec = eventQueueNew(NULL, "TestEventQNew1");
    CuAssert(tc, "eventQueueNew(NULL) failed NULL queue pointer test", ec
            == MPE_EINVAL);

    /**
     * \assertion "MPE_SUCCESS" is returned if a valid pointer is passed to 
     * 'mpeos_eventQueueNew()'.
     */

    ec = eventQueueNew(&evq, "TestEventQNew2");
    CuAssert(tc, "eventQueueNew(&q) failed", ec == MPE_SUCCESS);

    /* Now delete it... */
    ec = eventQueueDelete(evq);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_eventQueueNew finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_eventQueueDelete()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_eventQueueDelete()" function.
 *
 * \api mpeos_eventQueueNew()
 *
 * \strategy Call the "mpeos_eventQueueDelete()" function with various legal
 *  and illegal input values and and check for expected results.
 *
 * \assets none
 *
 */

static void vte_test_eventQueueDelete(CuTest *tc)
{
    mpe_Error ec;
    mpe_EventQueue evq;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_eventQueueDelete\n");

    /**
     * \assertion "MPE_EINVAL" is returned if a NULL pointer is passed to 
     *  'mpeos_eventQueueDelete()'.
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  vte_test_eventQueueDelete() - deleting NULL queue pointer\n");

    ec = eventQueueDelete(NULL);
    CuAssert(tc, "eventQueueDelete(NULL) failed NULL queue pointer test", ec
            == MPE_EINVAL);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  vte_test_eventQueueDelete() - creating event queue\n");

    /* Now create an event queue... */
    ec = eventQueueNew(&evq, "TestEventQDelete1");
    CuAssert(tc, "eventQueueNew(&q) failed", ec == MPE_SUCCESS);

    /**
     * \assertion "MPE_SUCCESS" is returned if a valid pointer is passed to 
     *  'mpeos_eventQueueDelete()'.
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  vte_test_eventQueueDelete() - deleting valid queue\n");

    ec = eventQueueDelete(evq);
    CuAssert(tc, "eventQueueDelete(q) failed valid queue pointer test", ec
            == MPE_SUCCESS);

    /**
     * \assertion An error is returned if an event queue is deleted twice.
     */

#ifndef POWERTV  /* this crashes the SA box.  Maybe it's not a valid test . . .*/
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  vte_test_eventQueueDelete() - deleting previously deleted valid queue\n");

    ec = eventQueueDelete(evq);
    CuAssert(tc, "eventQueueDelete(q) failed", ec == MPE_SUCCESS);
#endif  /* #ifndef POWERTV */

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_eventQueueDelete finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_eventQueueSend()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_eventQueueSend()" function within a
 *  single thread.
 *
 * \api mpeos_eventQueueSend()
 *
 * \strategy Call the "mpeos_eventQueueSend()" function with various legal
 *  and illegal input values and and check for expected results. Also calls
 *  "mpeos_eventQueueNext()" and checks that the expected event was sent.
 *
 *  This could be enhanced by creating multiple event queues and sending
 *  multiple events to each of them, then verifying that the expected
 *  events are sent to the right event queue in the right order.
 *
 * \assets none
 *
 */

static void vte_test_eventQueueSend(CuTest *tc)
{
    mpe_Error ec;
    mpe_EventQueue evq;
    mpe_Event evId;
    void *evdata;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_eventQueueSend\n");

    /* Do setup... */

    if ((ec = eventQueueNew(&evq, "TestEventQSend1")) == MPE_SUCCESS)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  vte_test_eventQueueSend() - setup OK\n");
    }
    else
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "  vte_test_eventQueueSend() - setup failed\n");
        CuFail(tc, "eventQueueNew(&q) failed");
    }

    /**
     * \assertion "mpeos_eventQueueSend()" returns MPE_EINVAL
     *  if a NULL event queue is passed.
     */

    ec = eventQueueSend(NULL, PING_EVENT, evq, NULL, 0);
    CuAssert(tc, "eventQueueSend(NULL) with NULL event queue failed",
            MPE_EINVAL == ec);

    /**
     * \assertion "mpeos_eventQueueSend()" returns MPE_SUCCESS when passed
     *  valid arguments.
     */

    ec = eventQueueSend(evq, PING_EVENT, evq, NULL, 0);
    CuAssert(tc, "eventQueueSend() failed", ec == MPE_SUCCESS);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  vte_test_eventQueueSend() - eventQueueSend complete\n");

    /**
     * \assertion The event previously sent via "mpeos_eventQueueSend()" is
     *  returned by a call to "mpeos_eventQueueNext()"
     */

    ec = eventQueueNext(evq, &evId, &evdata, NULL, NULL);
    CuAssert(tc, "eventQueueNext() failed", MPE_SUCCESS == ec);
    CuAssert(tc, "eventQueueNext() delivered wrong event", PING_EVENT == evId);
    CuAssert(tc, "eventQueueNext() delivered wrong data", evq == evdata);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  vte_test_eventQueueSend() - eventQueueNext complete\n");

    /* Delete test event */

    ec = eventQueueDelete(evq);
    CuAssert(tc, "eventQueueDelete(q) failed", ec == MPE_SUCCESS);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_eventQueueSend finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_eventQueueNext()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_eventQueueNext()" function.
 *
 * \api mpeos_eventQueueNext()
 *
 * \strategy Call the "mpeos_eventQueueNext()" function with various legal
 *  and illegal input values and and check for expected results.
 *
 * \assets none
 *
 */

static void vte_test_eventQueueNext(CuTest *tc)
{
    mpe_Error ec;
    mpe_EventQueue evq = NULL;
    mpe_Event evId = EVID_INIT;
    //  mpe_ThreadId   tid = 0;
    void *evdata;
    char myChar = 'z';

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_eventQueueNext\n");

    m_mpetest_evtc = tc;
    evdata = &myChar;

    /*
     *  Create a queue for testing
     */

    ec = eventQueueNew(&evq, "TestEventNext1");
    CuAssert(tc, "eventQueueNew() failed to create test event queue",
            MPE_SUCCESS == ec);

    /**
     * \assertion "MPE_EINVAL" is returned if a NULL event queue is passed to 
     *  'mpeos_eventQueueNext()'.
     */

    ec = eventQueueNext(NULL, &evId, &evdata, NULL, NULL);
    CuAssert(tc, "eventQueueNext() with NULL event queue failed", MPE_EINVAL
            == ec);

    /**
     * \assertion The event ID and event data are not altered if a NULL event
     *  queue is passed to 'mpeos_eventQueueNext()'
     */

    CuAssert(tc, "eventQueueNext() with NULL event queue altered event ID",
            EVID_INIT == evId);
    CuAssert(tc, "eventQueueNext() with NULL event queue altered event data",
            &myChar == evdata);

    /**
     * \assertion "MPE_EINVAL" is returned if a NULL event ID pointer is passed
     *  to 'mpeos_eventQueueNext()'.
     */

    ec = eventQueueNext(evq, NULL, &evdata, NULL, NULL);
    CuAssert(tc, "eventQueueNext() with NULL event ID pointer failed",
            MPE_EINVAL == ec);

    /**
     * \assertion The event data is not altered if a NULL event ID pointer is
     *  passed to 'mpeos_eventQueueNext()'
     */

    CuAssert(tc,
            "eventQueueNext() with NULL event ID pointer altered event data",
            &myChar == evdata);

    /**
     * \assertion "MPE_EINVAL" is returned if both a NULL event queue and a NULL
     *  event ID pointer are passed to 'mpeos_eventQueueNext()'.
     */

    ec = eventQueueNext(NULL, NULL, &evdata, NULL, NULL);
    CuAssert(
            tc,
            "eventQueueNext() with NULL event queue and NULL event ID pointer failed",
            MPE_EINVAL == ec);

    /**
     * \assertion The event data is not altered if both a NULL event queue and a
     *  NULL event ID pointer are passed to 'mpeos_eventQueueNext()'
     */

    CuAssert(
            tc,
            "eventQueueNext() with NULL event queue and NULL event ID pointer altered event data",
            &myChar == evdata);

    /**
     * \assertion "MPE_ENODATA" is returned by 'mpeos_eventQueueNext()' if no
     *  events are available.
     */

    ec = eventQueueNext(evq, &evId, &evdata, NULL, NULL);
    CuAssert(
            tc,
            "eventQueueNext() on empty event queue failed to return MPE_ETIMEOUT",
            MPE_ETIMEOUT == ec);

    /**
     * \assertion The event ID and event data are not altered if no events are available
     */

    CuAssert(tc, "eventQueueNext() with no events available altered event ID",
            EVID_INIT == evId);
    CuAssert(tc,
            "eventQueueNext() with no events available altered event data",
            &myChar == evdata);

    /*
     * Now post an event to the queue.
     */

    ec = eventQueueSend(evq, PING_EVENT, evq, NULL, 0);
    CuAssert(tc, "eventQueueSend() failed", ec == MPE_SUCCESS);

    /**
     * \assertion The event previously sent via "mpeos_eventQueueSend()" is
     *  returned by a call to "mpeos_eventQueueNext()"
     */

    ec = eventQueueNext(evq, &evId, &evdata, NULL, NULL);
    CuAssert(tc, "eventQueueNext() failed", MPE_SUCCESS == ec);
    CuAssert(tc, "eventQueueNext() delivered wrong event", PING_EVENT == evId);
    CuAssert(tc, "eventQueueNext() delivered wrong data", evq == evdata);

    /**
     * \assertion After calling "mpeos_eventQueueNext()", the queue is empty
     *  again and "MPE_ETIMEOUT" is returned by the next "mpeos_eventQueueNext()".
     *  call.
     */

    ec = eventQueueNext(evq, &evId, &evdata, NULL, NULL);
    CuAssert(
            tc,
            "eventQueueNext() on empty event queue failed to return MPE_ENODATA",
            MPE_ETIMEOUT == ec);

    /* Delete test event */

    ec = eventQueueDelete(evq);
    CuAssert(tc, "eventQueueDelete(q) failed", ec == MPE_SUCCESS);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_eventQueueNext finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_eventQueueWaitNext()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_eventQueueWaitNext()" function.
 *
 * \api mpeos_eventQueueNext()
 *
 * \strategy Call the "mpeos_eventQueueWaitNext()" function with various legal
 *  and illegal input values and and check for expected results.
 *
 * \assets none
 *
 */

static void vte_test_eventQueueWaitNext(CuTest *tc)
{
    mpe_Error ec;
    mpe_EventQueue evq = NULL;
    mpe_Event evId = EVID_INIT;
    //  mpe_ThreadId   tid = 0;
    void *evdata;
    char myChar = 'z';

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_eventQueueWaitNext\n");

    m_mpetest_evtc = tc;
    evdata = &myChar;

    /*
     *  Create a queue for testing
     */

    ec = eventQueueNew(&evq, "TestEventWaitNext1");
    CuAssert(tc, "eventQueueNew() failed to create test event queue",
            MPE_SUCCESS == ec);

    /**
     * \assertion "MPE_EINVAL" is returned if a NULL event queue is passed to 
     *  'mpeos_eventQueueNext()'.
     */

    ec = eventQueueWaitNext(NULL, &evId, &evdata, NULL, NULL, WAITTIMEOUT);
    CuAssert(tc, "eventQueueWaitNext() with NULL event queue failed",
            MPE_EINVAL == ec);

    /**
     * \assertion The event ID and event data are not altered if a NULL event
     *  queue is passed to 'mpeos_eventQueueWaitNext()'
     */

    CuAssert(tc, "eventQueueWaitNext() with NULL event queue altered event ID",
            EVID_INIT == evId);
    CuAssert(tc,
            "eventQueueWaitNext() with NULL event queue altered event data",
            &myChar == evdata);

    /**
     * \assertion "MPE_EINVAL" is returned if a NULL event ID pointer is passed
     *  to 'mpeos_eventQueueWaitNext()'.
     */

    ec = eventQueueWaitNext(evq, NULL, &evdata, NULL, NULL, WAITTIMEOUT);
    CuAssert(tc, "eventQueueWaitNext() with NULL event ID pointer failed",
            MPE_EINVAL == ec);

    /**
     * \assertion The event data is not altered if a NULL event ID pointer is
     *  passed to 'mpeos_eventQueueWaitNext()'
     */

    CuAssert(
            tc,
            "eventQueueWaitNext() with NULL event ID pointer altered event data",
            &myChar == evdata);

    /**
     * \assertion "MPE_EINVAL" is returned if both a NULL event queue and a NULL
     *  event ID pointer are passed to 'mpeos_eventQueueWaitNext()'.
     */

    ec = eventQueueWaitNext(NULL, NULL, &evdata, NULL, NULL, WAITTIMEOUT);
    CuAssert(
            tc,
            "eventQueueWaitNext() with NULL event queue and NULL event ID pointer failed",
            MPE_EINVAL == ec);

    /**
     * \assertion The event data is not altered if both a NULL event queue and a
     *  NULL event ID pointer are passed to 'mpeos_eventQueueWaitNext()'
     */

    CuAssert(
            tc,
            "eventQueueWaitNext() with NULL event queue and NULL event ID pointer altered event data",
            &myChar == evdata);

    /**
     * \assertion "MPE_ETIMEOUT" is returned by 'mpeos_eventQueueWaitNext()' if no
     *  events are available.
     */

    ec = eventQueueWaitNext(evq, &evId, &evdata, NULL, NULL, WAITTIMEOUT);
    CuAssert(
            tc,
            "eventQueueWaitNext() on empty event queue failed to return MPE_ETIMEOUT",
            MPE_ETIMEOUT == ec);

    /**
     * \assertion The event ID and event data are not altered if no events are available
     */

    CuAssert(tc,
            "eventQueueWaitNext() with no events available altered event ID",
            EVID_INIT == evId);
    CuAssert(tc,
            "eventQueueWaitNext() with no events available altered event data",
            &myChar == evdata);

    /*
     * Now post an event to the queue.
     */

    ec = eventQueueSend(evq, PING_EVENT, evq, NULL, 0);
    CuAssert(tc, "eventQueueSend() failed", ec == MPE_SUCCESS);

    /**
     * \assertion The event previously sent via "mpeos_eventQueueSend()" is
     *  returned by a call to "mpeos_eventQueueWaitNext()"
     */

    ec = eventQueueWaitNext(evq, &evId, &evdata, NULL, NULL, WAITTIMEOUT);
    CuAssert(tc, "eventQueueWaitNext() failed", MPE_SUCCESS == ec);
    CuAssert(tc, "eventQueueWaitNext() delivered wrong event", PING_EVENT
            == evId);
    CuAssert(tc, "eventQueueWaitNext() delivered wrong data", evq == evdata);

    /**
     * \assertion After calling "mpeos_eventQueueWaitNext()", the queue is
     *  empty again and "MPE_ETIMEOUT" is returned by the next
     *  "mpeos_eventQueueWaitNext()" call.
     */

    ec = eventQueueWaitNext(evq, &evId, &evdata, NULL, NULL, WAITTIMEOUT);
    CuAssert(
            tc,
            "eventQueueWaitNext() on empty event queue failed to return MPE_ETIMEOUT",
            MPE_ETIMEOUT == ec);

    /* Delete test event */

    ec = eventQueueDelete(evq);
    CuAssert(tc, "eventQueueDelete(q) failed", ec == MPE_SUCCESS);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_eventQueueWaitNext finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_eventSendThread()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_eventQueueSend()" function between
 *  two threads.
 *
 * \api mpeos_eventQueueSend()
 *
 * \strategy Fork a thread which calls "mpeos_eventQueueSend()", wait a
 *  second, then call "mpeos_eventQueueNext()" and check that the expected
 *  event was sent by the other thread.
 *
 *   This could be enhanced by creating multiple event queues and multiple
 *   threads and sending multiple events to each different queue, then
 *   verifying that the expected events can be retrieved by the different
 *   threads from the correct event queue in the right order.
 *
 * \assets none
 *
 */

static void vte_test_eventSendThread(CuTest *tc)
{
    mpe_ThreadId tid = 0;
    mpe_EventQueue evq;
    mpe_Event evId;
    mpe_Error ec;
    void *evdata;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_eventSendThread\n");

    m_mpetest_evtc = tc; /* put CuTest pointer in global for test thread */

    /* Create a test event queue. */

    ec = eventQueueNew(&evq, "TestEventSendThread1");
    CuAssert(tc, "eventQueueNew(&q) failed", ec == MPE_SUCCESS);

    m_mpetest_pingQ = evq; /* put event queue pointer in global for test thread */

    /* Create the event thread to ping this thread. */

    ec = threadCreate(testEvent_SendThread, &m_ThreadStatus,
            MPE_THREAD_PRIOR_DFLT, MPE_THREAD_STACK_SIZE, &tid, "foo");

    CuAssert(tc, "threadCreate() failed", MPE_SUCCESS == ec);

    /* Wait a second for the other thread to post an event */

    threadSleep(1000, 0);

    /**
     * \assertion The event sent by the other thread is returned by a call
     * to "mpeos_eventQueueWaitNext()"
     */

    ec = eventQueueNext(evq, &evId, &evdata, NULL, NULL);
    CuAssert(tc, "eventQueueNext() failed", MPE_SUCCESS == ec);
    CuAssert(tc, "eventQueueNext() delivered wrong event", PING_EVENT == evId);
    CuAssert(tc, "eventQueueNext() delivered wrong data", evq == evdata);

    /* Delete test event queue */

    ec = eventQueueDelete(evq);
    CuAssert(tc, "eventQueueDelete(q) failed", ec == MPE_SUCCESS);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_eventSendThread finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_eventPingPong()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_eventQueueSend()" function between
 *  two threads, sending multiple events in each direction.
 *
 * \api mpeos_eventQueueSend()
 *
 * \strategy Fork another thread. The initial thread and the other thread
 *  ping-pong events back and forth to verify synchronization.
 *
 * \assets none
 *
 */

static void vte_test_eventPingPong(CuTest *tc)
{
    mpe_ThreadId tid = 0;
    mpe_Error ec;
    uint32_t i;
    mpe_Event evId;
    void *evdata;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_eventPingPong\n");

    m_mpetest_syncTest = tc;

    /* Init shared data. */
    m_mpetest_ping = m_mpetest_pong = 0;
    m_mpetest_pingQ = m_mpetest_pongQ = NULL;

    /**
     * \assertion Two different, unique event queues can be created.
     */

    ec = eventQueueNew(&m_mpetest_pingQ, "TestEventPingPong1");
    CuAssert(tc, "eventQueueNew() failed", MPE_SUCCESS == ec);

    ec = eventQueueNew(&m_mpetest_pongQ, "TestEventPingPong2");
    CuAssert(tc, "eventQueueNew() failed", MPE_SUCCESS == ec);

    CuAssert(tc, "eventQueueNew() returned duplicate event queues",
            m_mpetest_pingQ != m_mpetest_pongQ);

    /* Fork the "pong" event thread. */
    ec = threadCreate(testEvent_EventThread, &m_ThreadStatus,
            MPE_THREAD_PRIOR_DFLT, MPE_THREAD_STACK_SIZE, &tid, "foo");
    CuAssert(tc, "threadCreate() failed", ec == MPE_SUCCESS);

    /**
     * \assertion Multiple events can be sent between the two test threads, in
     *  each direction and be received in the correct order.
     */

    for (i = 0; i < 1000; ++i)
    {
        /* Get the next event... */
        if ((ec = eventQueueWaitNext(m_mpetest_pingQ, &evId, &evdata, NULL,
                NULL, 10000)) == MPE_SUCCESS)
        {
            CuAssert(
                    tc,
                    "eventQueueWaitNext(m_mpetest_pingQ) failed, event Id wrong",
                    evId == PING_EVENT);
            CuAssert(
                    tc,
                    "eventQueueWaitNext(m_mpetest_pingQ) failed, event data wrong",
                    evdata == (void*) &m_ThreadStatus);

            m_mpetest_ping++;

            CuAssert(
                    tc,
                    "eventQueueWaitNext(m_mpetest_pingQ) failed to maintain synchronization",
                    m_mpetest_ping == m_mpetest_pong + 1);

            /* Now set condition for pong thread... */
            ec = eventQueueSend(m_mpetest_pongQ, PONG_EVENT,
                    (void*) &m_ThreadStatus, NULL, 0);
            CuAssert(tc, "eventQueueSend() failed", ec == MPE_SUCCESS);
        }
        else
            CuAssert(
                    tc,
                    "eventQueueWaitNext(m_mpetest_pingQ) timed out, wrong error returned",
                    ec == MPE_ENODATA);
    }

    /* Wait for the other thread to exit. */

    threadSleep(1000, 0);

    /* Now delete events... */
    ec = eventQueueDelete(m_mpetest_pingQ);
    CuAssert(tc, "eventQueueDelete() failed", ec == MPE_SUCCESS);

    ec = eventQueueDelete(m_mpetest_pongQ);
    CuAssert(tc, "eventQueueDelete() failed", ec == MPE_SUCCESS);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_eventPingPong finished.\n\n");
}

#if 0

/****************************************************************************
 *
 *  testEvent_PollEventThread() -  Internal function, this code gets launched
 *      in a new thread to verify "mpeos_eventQueueNext()" between threads.
 *      This function polls an event queue until an event is posted to the
 *      queue.
 *
 *  Passed : A pointer to the shared variable.
 *
 ****/

static void testEvent_PollEventThread(void *tls)
{
    mpe_Error ec;
    mpe_EventQueue evq = (mpe_EventQueue)tls;
    mpe_Event evId;
    void *evdata;
    mpe_Time start, end;
    uint32_t i = 0;

    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST, "    >>>> entered 'testEvent_PollEventThread()'\n");

    m_mpetest_done = 0;
    timeGet(&start);
    do
    {
        /* Check for event. */
        ec = eventQueueNext(evq, &evId, &evdata, NULL, NULL);
        CuAssert(m_mpetest_evtc, "eventQueueNext() failed", ec == MPE_SUCCESS
                || ec == MPE_ENODATA);
        ++i;
    }while (ec == MPE_ENODATA);
    timeGet(&end);

    CuAssert(m_mpetest_evtc, "testEvent_PollEventThread() terminated loop incorrectly"
            , ec == MPE_SUCCESS && i > 1);

    CuAssertIntEquals_Msg(m_mpetest_evtc, "testEvent_PollEventThread() failed, wrong event"
            , PING_EVENT, evId);
    CuAssertPtrEquals_Msg(m_mpetest_evtc, "testEvent_PollEventThread() failed, wrong event data"
            , evq, evdata);
    CuAssertIntEquals_Msg(m_mpetest_evtc, "testEvent_PollEventThread() failed, wrong time"
            , start+DELAY, end);
    ++m_mpetest_done;

    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST, "    <<<< 'testEvent_PollEventThread()' finished.\n");

}

#endif

/****************************************************************************
 *
 *  testEvent_SendThread() -  Internal function, this code gets launched in
 *      a new thread to verify mpe_mpetest_eventSend between threads.
 *      This function posts an event to a global event queue.
 *
 *  Passed : A pointer to a global variable which is not used.
 *
 ****/

static void testEvent_SendThread(void *tls)
{
    mpe_Error ec;

    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "    >>>> entered 'testEvent_SendThread()'\n");

    /* Send an event */

    ec = eventQueueSend(m_mpetest_pingQ, PING_EVENT, m_mpetest_pingQ, NULL, 0);
    CuAssert(m_mpetest_evtc, "eventQueueSend(q) failed", ec == MPE_SUCCESS);

    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "    <<<< 'testEvent_SendThread()' finished.\n");
}

/****************************************************************************
 *
 *  testEvent_EventThread() -  Internal function, this code gets launched in
 *      a new thread to verify mpe_mpetest_eventSend between threads.
 *
 *  Passed : A pointer to a global variable which appears to not be necessary.
 *
 ****/

static void testEvent_EventThread(void *tls)
{
    mpe_Error ec;
    uint32_t *sd = (uint32_t*) tls;
    uint32_t i;
    mpe_Event evId;
    void *evdata;

    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "    >>>> entered 'testEvent_EventThread()'\n");

    /* Validate shared data pointer & set result. */
    if (sd == &m_ThreadStatus)
        *sd = 99;

    /* Get things started by sending a ping event... */

    ec = eventQueueSend(m_mpetest_pingQ, PING_EVENT, tls, NULL, 0);
    CuAssert(m_mpetest_evtc, "eventQueueSend(m_mpetest_pingQ) failed", ec
            == MPE_SUCCESS);

    for (i = 0; i < 1000; ++i)
    {
        /* Wait for the other thread */
        if ((ec = eventQueueWaitNext(m_mpetest_pongQ, &evId, &evdata, NULL,
                NULL, 10000)) == MPE_SUCCESS)
        {
            CuAssert(
                    m_mpetest_evtc,
                    "eventQueueWaitNext(m_mpetest_pongQ) failed, event Id wrong",
                    evId == PONG_EVENT);
            CuAssert(
                    m_mpetest_evtc,
                    "eventQueueWaitNext(m_mpetest_pongQ) failed, event data wrong",
                    evdata == (void*) &m_ThreadStatus);
            m_mpetest_pong++;

            CuAssert(
                    m_mpetest_syncTest,
                    "eventQueueWaitNext(pong) failed to maintain synchronization",
                    m_mpetest_ping == m_mpetest_pong);

            /* Now send and event to the ping thread... */
            ec = eventQueueSend(m_mpetest_pingQ, PING_EVENT, tls, NULL, 0);
            CuAssert(m_mpetest_evtc, "eventQueueSend(m_mpetest_pingQ) failed",
                    ec == MPE_SUCCESS);
        }
        else
            CuAssert(
                    m_mpetest_evtc,
                    "eventQueueWaitNext(m_mpetest_pongQ) timed out, wrong error returned",
                    ec == MPE_ENODATA);
    }

    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "    <<<< 'testEvent_EventThread()' finished\n");
}

/**
 * getTestSuite_sysEvent
 *
 * Create and return the test suite for the mpe_event APIs.
 *
 * @return a pointer to the new test suite.
 */
CuSuite* getTestSuite_sysEvent(void)
{
    CuSuite* suite = CuSuiteNew();

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST, "Entered 'getTestSuite_sysEvent()'\n");

    SUITE_ADD_TEST(suite, vte_test_eventQueueNew);
    SUITE_ADD_TEST(suite, vte_test_eventQueueDelete);
    SUITE_ADD_TEST(suite, vte_test_eventQueueNext);
    SUITE_ADD_TEST(suite, vte_test_eventQueueSend);
    SUITE_ADD_TEST(suite, vte_test_eventQueueWaitNext);
    SUITE_ADD_TEST(suite, vte_test_eventSendThread);
    SUITE_ADD_TEST(suite, vte_test_eventPingPong);

    return suite;
}

