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

/** \file test_sys_thread.c
 *
 *  \brief Test functions for MPEOS thread functions
 *
 *  This file contains tests for the following MPEOS functions :\n
 *
 *    -# mpeos_threadAttach      - registers/unregisters a thread
 *    -# mpeos_threadCreate      - creates a new thread
 *    -# mpeos_threadDestroy     - terminates a thread
 *    -# mpeos_threadGetCurrent  - gets the identifier of the current thread
 *    -# mpeos_threadGetData     - gets the target thread’s local storage
 *    -# mpeos_threadGetStatus   - gets the target thread’s status.
 *    -# mpeos_threadSetData     - sets the target thread’s local storage.
 *    -# mpeos_threadSetPriority - sets the target thread’s priority.
 *    -# mpeos_threadSetStatus   - sets the target thread’s status.
 *    -# mpeos_threadSleep       - puts the current thread to sleep.
 *    -# mpeos_threadYield       - releases the remainder of the calling thread’s time-slice.
 */

#define _MPE_THREAD
#define TEST_MPE_THREAD

#include <cutest.h>
#include <mpe_sys.h>
#include "test_sys.h"

/*  Test thread state values  */

#define TESTTHREADENTERED        1
#define TESTTHREADAWAKE          2
#define TESTTHREADDEATH          3
#define TESTTHREADERROR          4
#define TESTTHREADFINISHED       5
#define TESTTHREADDESTROYFAILED  6
#define TESTTHREADINVALID        9999999

#define TESTTHREADDATAVALUE      314159265

/* Null ID definition*/
#define NULLID 0

/*
 *  module global variables
 */

/* The following are set by the test thread and examined by the test function  */

static int m_testThreadState; /* test thread state  */
static uint32_t m_testThreadStatus; /* test thread status  */
static uint32_t *m_testThreadDataPointer; /* test thread data pointer  */
static uint32_t m_testThreadData; /* test thread data  */
static void *m_testThreadLocalPointer1; /* test thread local storage pointer  */
static void *m_testThreadLocalPointer2; /* test thread local storage pointer - 2nd version */
static mpe_ThreadId m_testThreadID; /* test thread ID  */
static mpe_TimeMillis m_testThreadStartTime; /* test thread start time */
static CuTest *m_currentTest; /* Current test case structure pointer. */

/*
 * Thread test functions defined in this file
 */
static void vte_test_threadAttach(CuTest*);
static void vte_test_threadCreate(CuTest*);
static void vte_test_threadDestroyID0(CuTest*);
static void vte_test_threadDestroyID(CuTest*);
static void vte_test_threadDestroyOther(CuTest*);
static void vte_test_threadGetCurrent(CuTest*);
static void vte_test_threadGetData(CuTest*);
static void vte_test_threadGetStatus(CuTest*);
static void vte_test_threadSetData(CuTest*);
static void vte_test_threadSetPriority(CuTest*);
static void vte_test_threadSetStatus(CuTest*);
static void vte_test_threadSleep(CuTest*);
static void vte_test_threadYield(CuTest*);
static void vte_test_threadData(CuTest *tc);
static void vte_test_threadStatus(CuTest*);

/*
 * Internal functions, not called directly by the test runner
 */
static void testThread_simple(void *);
static void testThread_donothing(void *);
static void testThread_sleep(void *);
static void testThread_busy(void *);
static void testThread_destroy0(void *);
static void testThread_destroyID(void *);
static long int testThread_wasteTime(long int, long int);

/*
 * Function which adds thread test functions to a CuTest suite
 */

CuSuite* getTestSuite_sysThread(void);

/****************************************************************************
 *
 *  vte_test_threadCreate()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_threadCreate" function 
 *
 * \api mpeos_threadCreate()
 *
 * \strategy Call the "mpeos_threadCreate()" to launch a new thread. The new
 *   thread modifies a shared variable to indicate that the launch was
 *   successful.
 *
 * \assets none
 *
 */

static void vte_test_threadCreate(CuTest *tc)
{
    mpe_Error ec;
    mpe_ThreadId tid;
    uint32_t testData = TESTTHREADDATAVALUE;
    mpe_TimeMillis threadCreateStart;
    mpe_TimeMillis threadCreateFinish;
    char *nullThreadName = "\0"; /* a pointer to a null string  */

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_threadCreate\n");

    m_currentTest = tc;

    /*
     *  First, test with invalid parameters. These should return errors from
     *  the 'threadCreate()' call and not actually create a new thread.
     */

    /**
     * \assertion "MPE_EINVAL" is returned if a NULL function pointer is passed
     */

    ec = threadCreate(NULLID, &m_testThreadData, MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_STACK_SIZE, &tid, "test_threadCreate1");

    CuAssert(tc, "threadCreate() failed : NULL function ptr.", ec == MPE_EINVAL);

    /*
     *  Now test with valid parameters. These test cases should not return
     *  errors from the 'threadCreate()' call and should all create a new
     *  thread.
     */

    /*  Init some stuff  */

    m_testThreadState = TESTTHREADINVALID;
    m_testThreadStatus = TESTTHREADINVALID;
    m_testThreadID = TEST_THREAD_INVALID_THREADID;
    m_testThreadDataPointer = NULLID;
    m_testThreadData = 0L;
    m_testThreadStartTime = (mpe_TimeMillis) 0;
    testData = TESTTHREADDATAVALUE;
    tid = TEST_THREAD_INVALID_THREADID;

    /**
     * \assertion threadCreate() with NULL data pointer should not return an error
     */

    ec = threadCreate(testThread_simple, NULLID, MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_STACK_SIZE, &tid, "test_threadCreate 2");
    CuAssert(tc, "threadCreate() failed : NULL data pointer", ec == MPE_SUCCESS);

    /**
     * \assertion threadCreate() with NULL data pointer should create a new thread
     */

    CuAssert(tc,
            "threadCreate() didn't return thread ID with NULL data pointer",
            TEST_THREAD_INVALID_THREADID != tid);

    threadSleep(100, 0); /* allow new thread time to run  */
    CuAssert(
            tc,
            "threadCreate() doesn't seem to have created a thread when passed a NULL data pointer",
            TESTTHREADENTERED == m_testThreadState);

    /*  Reinit shared variables  */

    m_testThreadState = TESTTHREADINVALID;
    m_testThreadStatus = TESTTHREADINVALID;
    m_testThreadID = TEST_THREAD_INVALID_THREADID;
    m_testThreadDataPointer = NULLID;
    m_testThreadData = 0L;
    tid = TEST_THREAD_INVALID_THREADID;

    /**
     * \assertion threadCreate() with NULL name pointer does not return an error
     */

    ec = threadCreate(testThread_simple, &testData, MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_STACK_SIZE, &tid, NULLID );
    CuAssert(tc, "threadCreate() failed : NULL name pointer", ec == MPE_SUCCESS);

    /**
     * \assertion threadCreate() with NULL name pointer creates a new thread
     */

    CuAssert(tc,
            "threadCreate() didn't return thread ID with NULL name pointer",
            TEST_THREAD_INVALID_THREADID != tid);

    threadSleep(200, 0); /* make sure the new thread has time to run  */
    CuAssert(
            tc,
            "threadCreate() doesn't seem to have created a thread when passed a NULL name pointer",
            TESTTHREADENTERED == m_testThreadState);

    /*  Reinit shared variables  */

    m_testThreadState = TESTTHREADINVALID;
    m_testThreadStatus = TESTTHREADINVALID;
    m_testThreadID = TEST_THREAD_INVALID_THREADID;
    m_testThreadDataPointer = NULLID;
    m_testThreadData = 0L;
    tid = TEST_THREAD_INVALID_THREADID;

    /**
     * \assertion threadCreate() with NULL name string does not return an error
     */

    ec = threadCreate(testThread_simple, &testData, MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_STACK_SIZE, &tid, nullThreadName);

    CuAssert(tc, "threadCreate() failed : NULL name string", ec == MPE_SUCCESS);

    /**
     * \assertion threadCreate() with NULL name string creates a new thread
     */

    CuAssert(tc,
            "threadCreate() didn't return thread ID with NULL name string",
            TEST_THREAD_INVALID_THREADID != tid);

    threadSleep(200, 0); /* make sure the new thread has time to run  */
    CuAssert(
            tc,
            "threadCreate() doesn't seem to have created a thread when passed a NULL name string",
            TESTTHREADENTERED == m_testThreadState);

    /*  Reinit shared variables  */

    m_testThreadState = TESTTHREADINVALID;
    m_testThreadStatus = TESTTHREADINVALID;
    m_testThreadID = TEST_THREAD_INVALID_THREADID;
    m_testThreadDataPointer = NULLID;
    m_testThreadData = 0L;
    testData = TESTTHREADDATAVALUE;
    tid = TEST_THREAD_INVALID_THREADID;

    /**
     * \assertion MPE_SUCCESS is returned if all parameters are valid.
     */

    timeGetMillis(&threadCreateStart); /* get start time  */
    ec = threadCreate(testThread_simple, &testData, MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_STACK_SIZE, &tid, "test_threadCreate 4");
    timeGetMillis(&threadCreateFinish); /* get finished time  */

    CuAssert(tc, "threadCreate() failed", ec == MPE_SUCCESS);

    /**
     * \assertion New thread starts and sets shared data to test values within 1 second
     */

    threadSleep(1000, 0);

    CuAssert(tc, "threadCreate() failed to start a thread", TESTTHREADENTERED
            == m_testThreadState);

    TRACE(
            MPE_LOG_TRACE6,
            MPE_MOD_TEST,
            "  'threadCreate() returned in %d mSec, new thread started in %d mSec\n",
            (int) (threadCreateFinish - threadCreateStart),
            (int) (m_testThreadStartTime - threadCreateStart));

    /**
     * \assertion Test thread got correct data pointer
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  Test thread data pointer == %p (%p)\n", m_testThreadDataPointer,
            &testData);

    CuAssert(tc, "test thread got wrong data pointer", &testData
            == m_testThreadDataPointer);

    /**
     * \assertion Test thread can read from the data pointer
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  Test thread read %08x (%08x)\n",
            m_testThreadData, TESTTHREADDATAVALUE);

    CuAssert(tc, "test thread read wrong data", TESTTHREADDATAVALUE
            == m_testThreadData);

    /**
     * \assertion Test thread can write to data pointer
     */

    CuAssert(tc, "test thread wrote wrong data", TESTTHREADDATAVALUE + 1
            == testData);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  Test thread wrote %08x (%08x)\n",
            testData, TESTTHREADDATAVALUE + 1);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_threadCreate finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_threadAttach()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_threadAttach" function 
 *
 * \api mpeos_threadAttach()
 *
 * \strategy Call the "mpeos_threadAttach()" function.
 *
 * \assets none
 *
 */

static void vte_test_threadAttach(CuTest *tc)
{
    mpe_ThreadId tid = 0;
    mpe_ThreadId cid = 0;
    mpe_Error ec;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_threadAttach\n");

    m_currentTest = tc;

    /* Get current thread ID. */

    ec = threadGetCurrent(&tid);
    CuAssert(tc, "threadGetCurrent() failed", ec == MPE_SUCCESS);
    cid = tid;

    /* Validate attach operation. */

    ec = threadAttach(&tid);
    CuAssert(tc, "threadAttach() failed", ec == MPE_SUCCESS && cid == tid
            && cid != 0);

    /* Now detach the current thread. */

    ec = threadAttach(NULLID);
    CuAssert(tc, "threadAttach() failed", ec == MPE_SUCCESS);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_threadAttach finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_threadDestroyID0()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_threadDestroy" function 
 *
 * \api mpeos_threadDestroy()
 *
 * \strategy Call "mpeos_threadCreate()" to launch a test thread. The test
 *   thread calls "mpeos_threadDestroy() with ID ==0. This function checks
 *   that the test thread actually gets destroyed.
 *
 * \assets none
 *
 */

static void vte_test_threadDestroyID0(CuTest *tc)
{
    mpe_Error ec;
    mpe_ThreadId tid;
    uint32_t testData = TESTTHREADDATAVALUE;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_threadDestroyID0\n");

    m_currentTest = tc;
    m_testThreadID = TEST_THREAD_INVALID_THREADID;
    m_testThreadDataPointer = NULLID;
    m_testThreadData = 0L;
    m_testThreadState = TESTTHREADINVALID;

    /* Start the test thread. */

    ec = threadCreate(testThread_destroy0, &testData, MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_STACK_SIZE, &tid, "mpetest_destroy0TestThread");
    CuAssert(tc, "threadCreate() failed for threadDestroy test", ec
            == MPE_SUCCESS);

    /**
     * \assertion New thread starts and sets shared thread state within 1 second
     */

    threadSleep(1000, 0);

    CuAssert(tc, "threadCreate() failed to start a thread", TESTTHREADINVALID
            != m_testThreadState);

    /**
     * \assertion Test thread sucessfully destroys itself
     */

    CuAssert(tc, "threadDestroy() failed to destroy test thread",
            TESTTHREADENTERED == m_testThreadState);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_threadDestroyID0 finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_threadDestroyID()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_threadDestroy" function 
 *
 * \api mpeos_threadDestroy()
 *
 * \strategy Call "mpeos_threadCreate()" to launch a test thread. The test
 *   thread calls "mpeos_threadDestroy() with ID == ThreadID. This function
 *   checks that the test thread actually gets destroyed.
 *
 * \assets none
 *
 */

static void vte_test_threadDestroyID(CuTest *tc)
{
    mpe_Error ec;
    mpe_ThreadId tid;
    uint32_t testData = TESTTHREADDATAVALUE;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_threadDestroyID\n");

    m_currentTest = tc;
    m_testThreadID = TEST_THREAD_INVALID_THREADID;
    m_testThreadDataPointer = NULLID;
    m_testThreadData = 0L;
    m_testThreadState = TESTTHREADINVALID;

    /* Start the test thread. */

    ec = threadCreate(testThread_destroyID, &testData, MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_STACK_SIZE, &tid, "mpetest_destroyIDTestThread");
    CuAssert(tc, "threadCreate() failed for threadDestroy test", ec
            == MPE_SUCCESS);

    /**
     * \assertion New thread starts and sets shared thread state within 1 second
     */

    threadSleep(1000, 0);

    CuAssert(tc, "threadCreate() failed to start a thread", TESTTHREADINVALID
            != m_testThreadState);

    /**
     * \assertion Test thread sucessfully destroys itself
     */
    threadSleep(3000, 0);

    CuAssert(tc, "threadDestroy() failed to destroy test thread",
            TESTTHREADDEATH == m_testThreadState);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_threadDestroyID finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_threadDestroyOther()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_threadDestroy" function 
 *
 * \api mpeos_threadDestroy()
 *
 * \strategy Call "mpeos_threadCreate()" to launch a test thread, waits
 *   for the test thread to start, then calls "mpeos_threadDestroy() with
 *   ID == test thread ID, then waits for the test thread to exit.
 *   checks that the test thread actually gets destroyed.
 *
 * \assets none
 *
 */

static void vte_test_threadDestroyOther(CuTest *tc)
{
    mpe_Error ec;
    mpe_ThreadId tid;
    uint32_t testData = TESTTHREADDATAVALUE;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_threadDestroyOther\n");

    m_currentTest = tc;
    m_testThreadID = TEST_THREAD_INVALID_THREADID;
    m_testThreadDataPointer = NULLID;
    m_testThreadData = 0L;
    m_testThreadState = TESTTHREADINVALID;

    /* Start the test thread. */

    ec = threadCreate(testThread_donothing, &testData, MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_STACK_SIZE, &tid, "mpetest_destroyTestThread");

    CuAssert(tc, "threadCreate() failed for threadDestroy test", ec
            == MPE_SUCCESS);

    /**
     * \assertion New thread starts and sets shared thread state within 1 second
     */

    threadSleep(1000, 0);

    CuAssert(tc, "threadCreate() failed to start a thread", TESTTHREADENTERED
            == m_testThreadState);

    /**
     * \assertion threadDestroy() on test thread returns MPE_SUCCESS
     */

    ec = threadDestroy(tid);
    CuAssert(tc, "threadDestroy() on test thread failed", ec == MPE_SUCCESS);

    /**
     * \assertion Test thread is destroyed
     */

    threadSleep(3000, 0);
    CuAssert(tc, "threadDestroy() failed to destroy the test thread",
            TESTTHREADDEATH == m_testThreadState);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_threadDestroyOther finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_threadData()
 *
 ***************************************************************************/
/**
 * \testdescription More extensive tests on the "mpeos_threadGetData" and
 *    "mpeos_threadSetData" functions, including doing gets & sets on another
 *    thread.
 *
 *  NOTE : This test must not run before "vte_test_threadGetData()"
 *
 * \api mpeos_threadGetData(), mpeos_threadSetData
 *
 * \strategy Call the "mpeos_threadGetData()" 
 *
 * \assets none
 *
 */

static void vte_test_threadData(CuTest *tc)
{
    mpe_Error ec;
    mpe_ThreadId tid = 0;
    mpe_ThreadId cid = 0;
    void *tls = (void*) (-1);
    void *ctls = (void*) (-1);
    int testLocal = -1;
    int thisLocal;
    uint32_t testData = TESTTHREADDATAVALUE;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_threadData\n");

    /* Get current thread ID. */

    ec = threadGetCurrent(&cid);
    CuAssert(tc, "threadGetCurrent() failed", ec == MPE_SUCCESS);

    /* Set current thread's storage pointer */

    ec = threadSetData(tid, &thisLocal);

    /* Init shared data. */

    m_currentTest = tc;
    m_testThreadID = TEST_THREAD_INVALID_THREADID;
    m_testThreadData = 0;
    m_testThreadState = TESTTHREADINVALID;

    /*  start a test thread  */

    ec = threadCreate(testThread_sleep, &testData, MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_STACK_SIZE, &tid, "testThread_sleep");

    CuAssert(tc, "threadCreate() failed", ec == MPE_SUCCESS);

    threadSleep(100, 0); /* allow new thread time to start  */

    /**
     * \assertion Test thread starts and sets shared thread state within 100 mSec
     */

    CuAssert(tc, "test thread failed to start", TESTTHREADENTERED
            == m_testThreadState);

    /**
     * \assertion threadGetData() on test thread returns MPE_SUCCESS
     */

    ec = threadGetData(tid, &tls);
    CuAssert(tc, "threadGetData(tid, &tls) returned an error", ec
            == MPE_SUCCESS);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  threadGetData(tid, &tls) returned %p, expected %p\n", tls,
            NULLID);
    /**
     * \assertion Test thread's local data pointer is NULL before it is set
     */

    CuAssert(tc, "threadGetData(tid, &tls) returned wrong address", NULLID
            == tls);

    /**
     * \assertion The test thread's local data pointer can be set
     */

    ec = threadSetData(tid, &testLocal);
    CuAssert(tc, "threadSetData() on test thread failed", ec == MPE_SUCCESS);

    /**
     * \assertion Setting the test thread's local storage pointer does not change
     *    the main thread's local storage pointer
     */

    ec = threadGetData(0, &ctls);
    CuAssert(tc, "threadGetData() on main thread failed", ec == MPE_SUCCESS);
    CuAssert(
            tc,
            "threadSetData() on test thread changed the local data pointer for main thread",
            ctls == &thisLocal);

    /**
     * \assertion The new value of the test thread's local storage pointer is
     *    returned by threadGetData()
     */

    ec = threadGetData(tid, &tls);
    CuAssert(tc, "threadGetData(tid, &tls) returned an error", ec
            == MPE_SUCCESS);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  threadGetData(tid, &tls) returned %p, expected %p\n", tls,
            &testLocal);

    CuAssert(tc, "threadGetData(tid, &tls) returned wrong address", &testLocal
            == tls);

    /* Set this thread's local storage pointer */

    ec = threadSetData(0, &thisLocal);
    CuAssert(tc, "threadSetData() on main thread failed", ec == MPE_SUCCESS);

    /**
     * \assertion Setting the main thread's local storage pointer does not alter
     *    the test thread's local storage pointer.
     */

    ec = threadGetData(tid, &tls);
    CuAssert(tc, "threadGetData() on test thread failed", ec == MPE_SUCCESS);
    CuAssert(tc, "threadGetData() on test thread returned wrong value", tls
            == &testLocal);

    /*  Wait a while for the test thread to finish  */

    /**
     * \assertion Test thread exits within 2.5 seconds
     */

    threadSleep(2500, 0);
    CuAssert(tc, "test thread failed to terminate", TESTTHREADFINISHED
            == m_testThreadState);

    /**
     * \assertion Test thread got the correct local storage pointer value
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  test thread got local storage pointer value %p, expected %p\n",
            m_testThreadLocalPointer2, &testLocal);

    CuAssert(tc, "test thread got wrong local storage pointer value",
            m_testThreadLocalPointer2 == &testLocal);

    /**
     * \assertion Test thread is able to write at it's local storage pointer
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  thread's local storage value is %d, expected %d\n", testLocal,
            TESTTHREADDATAVALUE);

    CuAssert(tc, "test thread didn't write at it's local storage pointer",
            TESTTHREADDATAVALUE == testLocal);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_threadData finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_threadGetCurrent()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_threadGetCurrent" function 
 *
 * \api mpeos_threadGetCurrent()
 *
 * \strategy Call the "mpeos_threadGetCurrent()" 
 *
 * \assets none
 *
 */

static void vte_test_threadGetCurrent(CuTest *tc)
{
    mpe_Error ec;
    mpe_ThreadId mainThreadID = TEST_THREAD_INVALID_THREADID;
    mpe_ThreadId tid = TEST_THREAD_INVALID_THREADID;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_threadGetCurrent\n");

    /**
     * \assertion threadGetCurrent() returns MPE_SUCCESS
     */

    ec = threadGetCurrent(&mainThreadID);
    CuAssert(tc, "threadGetCurrent() failed for main test thread", MPE_SUCCESS
            == ec);

    /**
     * \assertion The threadID returned by threadGetCurrent() isn't NULL
     */

    CuAssert(tc, "threadGetCurrent() returned NULL for main test thread",
            NULLID != mainThreadID);

    /*  Create a test thread  */

    m_testThreadID = NULLID;
    m_testThreadState = TESTTHREADINVALID;

    ec = threadCreate(testThread_simple, NULLID, MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_STACK_SIZE, &tid, "test_threadGetCurrent");

    m_testThreadID = tid;

    CuAssert(tc, "threadCreate() returned an error", MPE_SUCCESS == ec);

    threadSleep(10000, 0); /*  give test thread time to run  */
    CuAssert(tc, "threadCreate() failed to start thread", TESTTHREADENTERED
            == m_testThreadState);

    /**
     * \assertion Thread ID returned by 'threadCreate()' matches thread ID
     *    obtained by new thread calling 'threadGetCurrent()'
     */

    CuAssert(tc, "thread ID mismatch", tid == m_testThreadID);

    /**
     * \assertion Test thread and main thread IDs are different.
     */

    CuAssert(tc, "thread ID collision", mainThreadID != m_testThreadID);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_threadGetCurrent finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_threadGetData()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_threadGetData" function.
 *
 *    NOTE : This function needs to run before "vte_test_threadData()
 *           and "vte_test_threadSetData()"
 *
 * \api mpeos_threadGetData()
 *
 * \strategy Call the "mpeos_threadGetData()" 
 *
 * \assets none
 *
 */

static void vte_test_threadGetData(CuTest *tc)
{
    mpe_Error ec;
    mpe_ThreadId tid = 0;
    void *tls = (void*) (-1);

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_threadGetData\n");

    /* Get current thread ID. */

    ec = threadGetCurrent(&tid);
    CuAssert(tc, "threadGetCurrent() fathreadGetCurrent() failediled", ec
            == MPE_SUCCESS);

    /**
     * \assertion MPE_EINVAL is returned if a NULL pointer is passed
     */

    ec = threadGetData(0, NULLID);
    CuAssert(tc, "threadGetData(0, NULL) failed null pointer test", ec
            == MPE_EINVAL);

    /**
     * \assertion MPE_EINVAL is returned if an invalid thread ID is passed
     */

    ec = threadGetData((mpe_ThreadId) - 1, &tls);
    CuAssert(tc, "threadGetData(-1, &tls) failed bad thread ID test", ec
            == MPE_EINVAL);

    /**
     * \assertion threadGetData() with thread ID == 0 returns MPE_SUCCESS
     */

    ec = threadGetData(0, &tls);
    CuAssert(tc, "threadGetData(0, &tls) returned an error", ec == MPE_SUCCESS);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  threadGetData(0, &tls) returned %p, expected %p\n", tls, NULLID);

    /**
     * \assertion Main thread's local data pointer is NULL if hasn't been set
     */

    CuAssert(tc, "threadGetData(0, &tls) returned wrong address", NULLID == tls);

    /**
     * \assertion threadGetData() using thread ID returns MPE_SUCCESS
     */

    ec = threadGetData(tid, &tls);
    CuAssert(tc, "threadGetData(tid, &tls) returned an error", ec
            == MPE_SUCCESS);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  threadGetData(tid, &tls) returned %p, expected %p\n", tls,
            NULLID);

    /**
     * \assertion Main thread's local data pointer is NULL if hasn't been set
     */

    CuAssert(tc, "threadGetData(tid, &tls) returned wrong address", NULLID
            == tls);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_threadGetData finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_threadGetStatus()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_threadGetStatus" function 
 *
 * \api mpeos_thread()
 *
 * \strategy Call the "mpeos_threadGetStatus()" function and
 *
 * \assets none
 *
 */

static void vte_test_threadGetStatus(CuTest *tc)
{
    mpe_Error ec;
    mpe_ThreadId tid = NULLID;
    uint32_t status;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_threadGetStatus\n");

    /* Get current thread ID. */

    ec = threadGetCurrent(&tid);
    CuAssert(tc, "threadGetCurrent() failed", ec == MPE_SUCCESS);

    /**
     * \assertion MPE_EINVAL is returned with ID == 0 and pointer == NULL
     */

    ec = threadGetStatus(0, NULLID);
    CuAssert(
            tc,
            "threadGetStatus(0, NULL) failed thread ID == 0, null pointer test",
            ec == MPE_EINVAL);

    /**
     * \assertion MPE_SUCCESS is returned with ID == 0 and pointer == valid
     */

    status = -1;
    ec = threadGetStatus(0, &status);
    CuAssert(
            tc,
            "threadGetStatus(0, &status) failed thread ID == 0, valid pointer test",
            ec == MPE_SUCCESS);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  threadGetStatus(0, &status) returned %d, expected %d\n", status,
            TEST_THREAD_STAT_DEFAULT);

    /**
     * \assertion Returned thread status is TEST_THREAD_STAT_DEFAULT
     */

    CuAssert(tc, "threadGetStatus(0, &status) returned wrong thread status",
            TEST_THREAD_STAT_DEFAULT == status);

    /**
     * \assertion MPE_EINVAL is returned with ID == current thread ID
     *            and pointer == NULL
     */

    ec = threadGetStatus(tid, NULLID);
    CuAssert(
            tc,
            "threadGetStatus(tid, NULL) failed thread ID == valid, null pointer test",
            ec == MPE_EINVAL);

    /**
     * \assertion MPE_SUCCESS is returned with ID == current thread ID
     *            and pointer == valid
     */

    status = -1;
    ec = threadGetStatus(tid, &status);
    CuAssert(
            tc,
            "threadGetStatus(tid, &status) failed thread ID == valid, valid pointer test",
            ec == MPE_SUCCESS);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  threadGetStatus(tid, &status) returned %d, expected %d\n",
            status, TEST_THREAD_STAT_DEFAULT);

    /**
     * \assertion Returned thread status is TEST_THREAD_STAT_DEFAULT
     */

    CuAssert(tc, "threadGetStatus(tid, &status) returned wrong thread status",
            TEST_THREAD_STAT_DEFAULT == status);

    /**
     * \assertion MPE_EINVAL is returned with ID == invalid and pointer == NULL
     */

    ec = threadGetStatus((mpe_ThreadId) - 1, NULLID);
    CuAssert(
            tc,
            "threadGetStatus(-1, NULL) failed thread ID == invalid, null pointer test",
            ec == MPE_EINVAL);

    /**
     * \assertion MPE_EINVAL is returned with ID == invalid and pointer == valid
     */

    ec = threadGetStatus((mpe_ThreadId) - 1, &status);
    CuAssert(
            tc,
            "threadGetStatus(-1, &status) failed thread ID == invalid, valid pointer test",
            ec == MPE_EINVAL);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_threadGetStatus finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_threadSetData()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_threadSetData" function 
 *
 * \api mpeos_thread()
 *
 * \strategy Call the "mpeos_threadSetData()" function and
 *
 * \assets none
 *
 */

static void vte_test_threadSetData(CuTest *tc)
{
    mpe_Error ec;
    mpe_ThreadId tid = NULLID;
    void *tls = (void*) (-1);

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_threadSetData\n");

    /* Get current thread ID. */

    ec = threadGetCurrent(&tid);
    CuAssert(tc, "threadGetCurrent() failed", ec == MPE_SUCCESS);

    /**
     * \assertion MPE_SUCCESS is returned with ID == 0 and pointer == NULL
     */

    ec = threadSetData(0, NULLID);
    CuAssert(tc,
            "threadSetData(0, NULL) failed thread ID == 0, null pointer test",
            ec == MPE_EINVAL);

    /**
     * \assertion MPE_SUCCESS is returned with ID == 0 and pointer == valid
     */

    ec = threadSetData(0, &tls);
    CuAssert(tc,
            "threadSetData(0, &tls) failed thread ID == 0, valid pointer test",
            ec == MPE_SUCCESS);

    /**
     * \assertion MPE_SUCCESS is returned with ID == current thread ID
     *            and pointer == NULL
     */

    ec = threadSetData(tid, NULLID);
    CuAssert(
            tc,
            "threadSetData(tid, NULL) failed thread ID == valid, null pointer test",
            ec == MPE_EINVAL);

    /**
     * \assertion MPE_SUCCESS is returned with ID == current thread ID
     *            and pointer == valid
     */

    ec = threadSetData(tid, &tls);
    CuAssert(
            tc,
            "threadSetData(tid, &tls) failed thread ID == valid, valid pointer test",
            ec == MPE_SUCCESS);

    /**
     * \assertion MPE_EINVAL is returned with ID == invalid and pointer == NULL
     */

    ec = threadSetData((mpe_ThreadId) - 1, NULLID);
    CuAssert(
            tc,
            "threadSetData(-1, NULL) failed thread ID == invalid, null pointer test",
            ec == MPE_EINVAL);

    /**
     * \assertion MPE_EINVAL is returned with ID == invalid and pointer == valid
     */

    ec = threadSetData((mpe_ThreadId) - 1, &tls);
    CuAssert(
            tc,
            "threadSetData(-1, &tls) failed thread ID == invalid, valid pointer test",
            ec == MPE_EINVAL);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_threadSetData()' finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_SetPriority()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_threadSetPriority" function 
 *
 * \api mpeos_threadSetPriority()
 *
 * \strategy Call the "mpeos_threadSetPriority()" function and
 *
 * \assets none
 *
 */

static void vte_test_threadSetPriority(CuTest *tc)
{
    mpe_Error ec;
    mpe_ThreadId tid = NULLID;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_threadSetPriority\n");

    /* Get current thread ID. */

    ec = threadGetCurrent(&tid);
    CuAssert(tc, "threadGetCurrent() failed", ec == MPE_SUCCESS);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  current thread ID == %d\n", (int) tid);

    /**
     * \assertion MPE_SUCCESS is returned with thread ID == 0 and
     *            priority == MPE_THREAD_PRIOR_DFLT
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  threadSetPriority(0, MPE_THREAD_PRIOR_DFLT)\n");
    ec = threadSetPriority(0, MPE_THREAD_PRIOR_DFLT);
    CuAssert(tc, "threadSetPriority(0, MPE_THREAD_PRIOR_DFLT) failed", ec
            == MPE_SUCCESS);

    /**
     * \assertion MPE_SUCCESS is returned with ID == current thread ID
     *            and priority == MPE_THREAD_PRIOR_MAX
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  threadSetPriority(tid, MPE_THREAD_PRIOR_MAX)\n");
    ec = threadSetPriority(tid, MPE_THREAD_PRIOR_MAX);
    CuAssert(tc, "threadSetPriority(tid, MPE_THREAD_PRIOR_MAX) failed", ec
            == MPE_SUCCESS);

    /**
     * \assertion MPE_SUCCESS is returned with thread ID == 0 and
     *            priority == MPE_THREAD_PRIOR_MIN
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  threadSetPriority(0, MPE_THREAD_PRIOR_DFLT)\n");
    ec = threadSetPriority(0, MPE_THREAD_PRIOR_DFLT);
    CuAssert(tc, "threadSetPriority(0, MPE_THREAD_PRIOR_DFLT) failed", ec
            == MPE_SUCCESS);

    /**
     * \assertion MPE_SUCCESS is returned with ID == current thread ID
     *            and priority == MPE_THREAD_PRIOR_MIN+MPE_THREAD_PRIOR_INC
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  threadSetPriority(tid, MPE_THREAD_PRIOR_MIN+MPE_THREAD_PRIOR_INC)\n");
    ec = threadSetPriority(tid, MPE_THREAD_PRIOR_MIN + MPE_THREAD_PRIOR_INC);
    CuAssert(
            tc,
            "threadSetPriority(tid, MPE_THREAD_PRIOR_MIN+MPE_THREAD_PRIOR_INC) failed",
            ec == MPE_SUCCESS);

#ifndef POWERTV  /*  NOTE : This test crashes on the 8300 STB  */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  threadSetPriority((mpe_ThreadId)-1, MPE_THREAD_PRIOR_DFLT)\n");
    ec = threadSetPriority((mpe_ThreadId) - 1, MPE_THREAD_PRIOR_DFLT);
    CuAssert(
            tc,
            "threadSetPriority((mpe_ThreadId)-1, MPE_THREAD_PRIOR_DFLT) failed",
            ec == MPE_EINVAL);
#endif

    /*  finally, set thread priority back to default before exiting  */

    ec = threadSetPriority(tid, MPE_THREAD_PRIOR_DFLT);
    CuAssert(tc, "threadSetPriority(tid, MPE_THREAD_PRIOR_DFLT) failed", ec
            == MPE_SUCCESS);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_threadSetPriority finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_threadSetStatus()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_threadSetStatus" function 
 *
 * \api mpeos_threadSetStatus()
 *
 * \strategy Call the "mpeos_threadSetStatus()" function and
 *
 * \assets none
 *
 */

static void vte_test_threadSetStatus(CuTest *tc)
{
    mpe_Error ec;
    mpe_ThreadId tid = NULLID;
    uint32_t status;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_threadSetStatus\n");

    /* Get current thread ID. */

    ec = threadGetCurrent(&tid);
    CuAssert(tc, "threadGetCurrent() failed", ec == MPE_SUCCESS);

    /**
     * \assertion MPE_SUCCESS is returned with thread ID == 0
     */

    ec = threadSetStatus(0, TEST_THREAD_STAT_TEST_1);
    CuAssert(
            tc,
            "threadSetStatus(0, TEST_THREAD_STAT_TEST_1) failed thread ID == 0 test",
            ec == MPE_SUCCESS);

    /* Get thread status for current thread  */

    status = -1;
    ec = threadGetStatus(0, &status);
    CuAssert(tc, "threadGetStatus(0, &status) failed", ec == MPE_SUCCESS);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  threadGetStatus(0, &status) returned %d, expected %d\n", status,
            TEST_THREAD_STAT_TEST_1);

    /**
     * \assertion Returned thread status == TEST_THREAD_STAT_TEST_1
     */

    CuAssert(tc, "threadGetStatus(0, &status) returned wrong thread status",
            TEST_THREAD_STAT_TEST_1 == status);

    /**
     * \assertion MPE_SUCCESS is returned with ID == current thread ID
     */

    ec = threadSetStatus(tid, TEST_THREAD_STAT_TEST_2);
    CuAssert(
            tc,
            "threadSetStatus(tid, TEST_THREAD_STAT_TEST_2) failed thread ID == valid test",
            ec == MPE_SUCCESS);

    /* Get thread status for current thread  */

    status = -1;
    ec = threadGetStatus(0, &status);
    CuAssert(tc, "threadGetStatus(0, &status) failed", ec == MPE_SUCCESS);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  threadGetStatus(0, &status) returned %d, expected %d\n", status,
            TEST_THREAD_STAT_TEST_2);

    /**
     * \assertion Returned thread status == TEST_THREAD_STAT_TEST_2
     */

    CuAssert(tc, "threadGetStatus(0, &status) returned wrong thread status",
            TEST_THREAD_STAT_TEST_2 == status);

    /**
     * \assertion MPE_EINVAL is returned with ID == invalid
     */

    ec = threadSetStatus((mpe_ThreadId) - 1, TEST_THREAD_STAT_TEST_1);
    CuAssert(
            tc,
            "threadGetStatus(-1, TEST_THREAD_STAT_TEST_1) failed thread ID == invalid",
            ec == MPE_EINVAL);

    /**
     * \assertion Calling threadSetStatus() with invalid thread ID does not
     *            change current thread's status
     */

    status = -1;
    ec = threadGetStatus(0, &status);
    CuAssert(tc, "threadGetStatus(0, &status) failed", ec == MPE_SUCCESS);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  threadGetStatus(0, &status) returned %d, expected %d\n", status,
            TEST_THREAD_STAT_TEST_2);

    CuAssert(tc, "threadGetStatus(0, &status) returned wrong thread status",
            TEST_THREAD_STAT_TEST_2 == status);

    /*  Finally, set current thread status back to default value  */

    ec = threadSetStatus(0, TEST_THREAD_STAT_DEFAULT);
    CuAssert(tc, "threadSetStatus(0, TEST_THREAD_STAT_DEFAULT) failed", ec
            == MPE_SUCCESS);

    /*  Verify default thread status  */

    status = -1;
    ec = threadGetStatus(0, &status);
    CuAssert(tc, "threadGetStatus(0, &status) failed", ec == MPE_SUCCESS);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  threadGetStatus(0, &status) returned %d, expected %d\n", status,
            TEST_THREAD_STAT_DEFAULT);

    /**
     * \assertion Final thread status == TEST_THREAD_STAT_DEFAULT
     */

    CuAssert(tc, "threadGetStatus(0, &status) returned wrong thread status",
            TEST_THREAD_STAT_DEFAULT == status);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_threadSetStatus finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_threadStatus()
 *
 ***************************************************************************/
/**
 * \testdescription More extensive tests on the "mpeos_threadGetStatus" and
 *    "mpeos_threadSetStatus" functions, including doing gets & sets on another
 *    thread.
 *
 * \api mpeos_threadGetStatus(), mpeos_threadSetStatus
 *
 * \strategy Call "mpeos_threadSetStatus()" and "mpeos_threadGetStatus()" 
 *           with a variety of valid and invalid parameters, on multiple
 *           threads, and make sure everything behaves correctly :
 *
 *            - set main thread status = TEST_THREAD_STAT_TEST_1
 *            - get and verify main thread status = TEST_THREAD_STAT_TEST_1
 *            - launch a test thread, wait 100 mSec to run
 *            - get and verify test thread status = TEST_THREAD_STAT_DEFAULT
 *            - set test thread status = TEST_THREAD_STAT_TEST_2
 *            - get and verify test thread status = TEST_THREAD_STAT_TEST_2
 *            - get and verify main thread status = TEST_THREAD_STAT_TEST_1
 *            - set main thread status = TEST_THREAD_STAT_TEST_3
 *            - get and verify test thread status = TEST_THREAD_STAT_TEST_2
 *            - get and verify main thread status = TEST_THREAD_STAT_TEST_1
 *            - sleep 2.5 aec
 *            - check shared thread status in shared variable = TEST_THREAD_STAT_TEST_2
 *            - set main thread status = TEST_THREAD_STAT_DEFAULT
 *
 *            Meanwhile, the test thread does :
 *            - print entered message
 *            - get thread status, store in shared thread status
 *            - sleep 2000 msec
 *            - get thread status, store in shared thread status
 *            - print exiting message
 *            - exit
 *
 * \assets none
 *
 */

static void vte_test_threadStatus(CuTest *tc)
{
    mpe_Error ec;
    mpe_ThreadId mainID = NULLID;
    mpe_ThreadId testID = NULLID;
    uint32_t mainStatus = -1;
    uint32_t testStatus = -1;
    uint32_t testData = -1;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_threadStatus\n");

    /*  Get main thread ID. */

    ec = threadGetCurrent(&mainID);
    CuAssert(tc, "threadGetCurrent() failed", ec == MPE_SUCCESS);

    /*  set main thread status = TEST_THREAD_STAT_TEST_1  */

    ec = threadSetStatus(mainID, TEST_THREAD_STAT_TEST_1);
    CuAssert(
            tc,
            "threadSetStatus(tid, TEST_THREAD_STAT_TEST_1) failed thread ID == 0 test",
            ec == MPE_SUCCESS);

    /* get and verify main thread status = TEST_THREAD_STAT_TEST_1  */

    mainStatus = TEST_THREAD_STAT_INVALID;
    ec = threadGetStatus(0, &mainStatus);
    CuAssert(tc, "threadGetStatus(0, &mainStatus) failed", ec == MPE_SUCCESS);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  threadGetStatus(0, &mainStatus) returned %d, expected %d\n",
            mainStatus, TEST_THREAD_STAT_TEST_1);

    CuAssert(tc,
            "threadGetStatus(0, &mainStatus) returned wrong thread status",
            TEST_THREAD_STAT_TEST_1 == mainStatus);

    /* Init shared data. */

    m_currentTest = tc;
    m_testThreadID = TEST_THREAD_INVALID_THREADID;
    m_testThreadData = 0;
    m_testThreadState = TESTTHREADINVALID;
    m_testThreadStatus = TESTTHREADINVALID;

    /* launch a test thread */

    ec = threadCreate(testThread_sleep, &testData, MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_STACK_SIZE, &testID, "testThread_sleep");

    CuAssert(tc, "threadCreate() failed", ec == MPE_SUCCESS);
    CuAssert(tc, "test thread ID and main thread ID are the same!", mainID
            != testID);

    threadSleep(100, 0); /* allow new thread time to start  */

    /**
     * \assertion Test thread starts and sets shared thread state within 100 mSec
     */

    CuAssert(tc, "test thread failed to start", TESTTHREADENTERED
            == m_testThreadState);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  test thread's initial status is %d, expected %d\n",
            m_testThreadStatus, TEST_THREAD_STAT_DEFAULT);

    /**
     * \assertion Test thread's initial status is TEST_THREAD_STAT_DEFAULT
     */

    CuAssert(tc, "test thread's initial status is wrong",
            TEST_THREAD_STAT_DEFAULT == m_testThreadStatus);

    /**
     * \assertion threadGetStatus() on test thread returns MPE_SUCCESS
     */

    ec = threadGetStatus(testID, &testStatus);
    CuAssert(tc, "threadGetStatus(testID, &testStatus) returned an error", ec
            == MPE_SUCCESS);

    TRACE(
            MPE_LOG_DEBUG,
            MPE_MOD_TEST,
            "  threadGetStatus(testID, &testStatus) returned %d, expected %d\n",
            testStatus, TEST_THREAD_STAT_DEFAULT);

    /**
     * \assertion threadGetStatus() on test thread returns TEST_THREAD_STAT_DEFAULT
     */

    CuAssert(tc, "threadGetStatus(testID, &testStatus) returned wrong value",
            TEST_THREAD_STAT_DEFAULT == testStatus);

    /**
     * \assertion The test thread's status can be set
     */

    ec = threadSetStatus(testID, TEST_THREAD_STAT_TEST_2);
    CuAssert(tc, "threadSetStatus() on test thread failed", ec == MPE_SUCCESS);

    /*  Verify that test thread's status has changed  */

    testStatus = TEST_THREAD_STAT_INVALID;
    ec = threadGetStatus(testID, &testStatus);
    CuAssert(tc, "threadGetStatus(testID, &testStatus) returned an error", ec
            == MPE_SUCCESS);

    TRACE(
            MPE_LOG_DEBUG,
            MPE_MOD_TEST,
            "  threadGetStatus(testID, &testStatus) returned %d, expected %d\n",
            testStatus, TEST_THREAD_STAT_TEST_2);

    /**
     * \assertion threadGetStatus() on test thread returns TEST_THREAD_STAT_TEST_2
     */

    CuAssert(tc, "threadGetStatus(testID, &testStatus) returned wrong value",
            TEST_THREAD_STAT_TEST_2 == testStatus);

    /**
     * \assertion Setting the test thread's status does not change the main
     *            thread's status.
     */

    mainStatus = TEST_THREAD_STAT_INVALID;
    ec = threadGetStatus(0, &mainStatus);
    CuAssert(tc, "threadGetStatus() on main thread failed", ec == MPE_SUCCESS);
    CuAssert(
            tc,
            "threadSetStatus() on test thread changed the status for the main thread",
            TEST_THREAD_STAT_TEST_1 == mainStatus);

    /**
     * \assertion Setting the main thread's status does not change the test
     *            thread's status.
     */

    ec = threadSetStatus(mainID, TEST_THREAD_STAT_TEST_3);
    CuAssert(tc, "threadSetStatus(mainID, TEST_THREAD_STAT_TEST_3) failed", ec
            == MPE_SUCCESS);

    /* get and verify main thread status = TEST_THREAD_STAT_TEST_1  */

    mainStatus = TEST_THREAD_STAT_INVALID;
    ec = threadGetStatus(0, &mainStatus);
    CuAssert(tc, "threadGetStatus(0, &mainStatus) failed", ec == MPE_SUCCESS);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  threadGetStatus(0, &mainStatus) returned %d, expected %d\n",
            mainStatus, TEST_THREAD_STAT_TEST_3);

    CuAssert(tc,
            "threadGetStatus(0, &mainStatus) returned wrong thread status",
            TEST_THREAD_STAT_TEST_3 == mainStatus);

    /* get and verify test thread status = TEST_THREAD_STAT_TEST_2  */

    testStatus = TEST_THREAD_STAT_INVALID;
    ec = threadGetStatus(testID, &testStatus);
    CuAssert(tc, "threadGetStatus(testID, &testStatus) failed", ec
            == MPE_SUCCESS);

    TRACE(
            MPE_LOG_DEBUG,
            MPE_MOD_TEST,
            "  threadGetStatus(testID, &testStatus) returned %d, expected %d\n",
            testStatus, TEST_THREAD_STAT_TEST_2);

    CuAssert(
            tc,
            "threadGetStatus(testID, &testStatus) returned wrong thread status",
            TEST_THREAD_STAT_TEST_2 == testStatus);

    /*  Wait 2.5 seconds for the test thread to finish  */

    threadSleep(2500, 0);
    CuAssert(tc, "test thread failed to terminate", TESTTHREADFINISHED
            == m_testThreadState);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  test thread's final status is %d, expected %d\n",
            m_testThreadStatus, TEST_THREAD_STAT_TEST_2);

    /**
     * \assertion Test thread's final status is TEST_THREAD_STAT_TEST_2
     */

    CuAssert(tc, "test thread's final status is wrong", TEST_THREAD_STAT_TEST_2
            == m_testThreadStatus);

    /*  set main thread's status back to default before exiting  */

    ec = threadSetStatus(mainID, TEST_THREAD_STAT_DEFAULT);
    CuAssert(tc, "threadSetStatus(mainID, TEST_THREAD_STAT_TEST_3) failed", ec
            == MPE_SUCCESS);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_threadStatus finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_threadSleep()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_threadSleep" function 
 *
 * \api mpeos_threadSleep()
 *
 * \strategy Call the "mpeos_threadSleep()" function and check if it sleeps
 *           for the correct amount of time.
 *
 * \assets none
 *
 */

static void vte_test_threadSleep(CuTest *tc)
{
    mpe_TimeMillis endTime, startTime;
    mpe_ThreadId tid = 0;
    mpe_Error ec;
    uint32_t testData;
    int sleepTime;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_threadSleep\n");

    m_currentTest = tc;
    m_testThreadID = NULLID;
    m_testThreadData = 0;
    m_testThreadState = TESTTHREADINVALID;
    testData = 0;

    /*  start the test thread. */// does this really need another thread ??????

    ec = threadCreate(testThread_sleep, &testData, MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_STACK_SIZE, &tid, "mpetest_sleepTestThread");

    CuAssert(tc, "threadCreate() failed", ec == MPE_SUCCESS);
    threadSleep(100, 0); /* make sure the new thread has time to run  */
    CuAssert(tc, "test thread doesn't appear to have started",
            TESTTHREADENTERED == m_testThreadState);

    /*  get start time  */

    ec = timeGetMillis(&startTime);

    ec = threadSleep(2500, 0);
    CuAssert(tc, "threadSleep() returned an error", ec == MPE_SUCCESS);

    /* Get the ending time */

    ec = timeGetMillis(&endTime);
    sleepTime = (int) (endTime - startTime);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  slept %d milliSeconds in main thread\n", sleepTime);

    CuAssert(m_currentTest,
            "threadSleep() did not sleep correct time in main thread",
            ((sleepTime >= 2500) && (sleepTime < 2700)));

    while (TESTTHREADENTERED == m_testThreadState)
        ;

    /* Make sure test thread finished. */

    CuAssert(tc, "test thread doesn't appear to have finished",
            TESTTHREADFINISHED == m_testThreadState || m_testThreadState
                    == TESTTHREADDEATH);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_threadSleep finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_threadYield()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_threadYield" function 
 *
 * \api mpeos_threadYield()
 *
 * \strategy Call the "mpeos_threadYield()" function and
 *
 * \assets none
 *
 */

static void vte_test_threadYield(CuTest *tc)
{
    mpe_Error ec;
    mpe_TimeMillis startTime;
    mpe_TimeMillis endTime;
    mpe_TimeClock startCPU;
    mpe_TimeClock endCPU;
    mpe_ThreadId tid = 0;
    long int msec;
    long int ticks;
    uint32_t testData = TESTTHREADDATAVALUE;
    int i;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_threadYield\n");

    m_currentTest = tc;
    m_testThreadID = TEST_THREAD_INVALID_THREADID;
    m_testThreadData = 0;
    m_testThreadState = TESTTHREADINVALID;

    /*  Start "busy" thread so we're
     sure there's another runnable thread  */

    ec = threadCreate(testThread_busy, &testData, MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_STACK_SIZE, &tid, "mpetest_BusyThread");
    CuAssert(tc, "failed to create busy thread", ec == MPE_SUCCESS);

    ec = timeGetMillis(&startTime);
    startCPU = timeClock();

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  busy thread ID == %d\n", (int) tid);

    threadSleep(100, 0); /* sleep a while to allow busy thread to start  */

    CuAssert(tc, "busy thread didn't start", TESTTHREADENTERED
            == m_testThreadState);

    for (i = 0; i < 5000; i++)
    {
        threadYield();
    }

    endCPU = timeClock();
    ec = timeGetMillis(&endTime);

    ticks = (long) (endCPU - startCPU);
    msec = (long) (endTime - startTime);

    TRACE(
            MPE_LOG_DEBUG,
            MPE_MOD_TEST,
            "  vte_test_threadYield() - used %ld ticks in %ld mSec for %d loops, %ld ticks/mSec\n",
            ticks, msec, i, ticks / msec);

    ec = threadDestroy(tid); /* kill the busy thread  */

    threadSleep(100, 0); /* sleep a while to allow busy thread to die  */

    for (i = 0; i < 2000; i++)
    {
        if (TESTTHREADFINISHED == m_testThreadState)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "  busy thread finished in %d mSec\n", i);
            break;
        }
        threadSleep(1, 0);
    }
    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_threadYield finished.\n\n");
}

/****************************************************************************
 ****************************************************************************

 A whole bunch of thread test "helper" functions which get launched in
 a new thread.


 - testThread_simple
 - testThread_sleep()


 testThread_busy()

 Gets current time and puts it in "m_testThreadStartTime"
 Sets global "m_testThreadState" to TESTTHREADENTERED,
 Gets it's thread ID and puts it in global "m_testThreadID",
 Saves it's data pointer parameter in global "m_testThreadData",
 Busy loops until 2 seconds have elapsed,
 Sets global "m_testThreadState" to TESTTHREADFINISHED,
 Exits.

 testThread_destroyID

 Sets global "m_testThreadState" to TESTTHREADENTERED,
 Gets it's thread ID and puts it in global "m_testThreadID",
 Does "threadDestroy(id)
 Sets global "m_testThreadState" to TESTTHREADDESTROYFAILED,
 Exits.


 testThread_destroyID0

 Sets global "m_testThreadState" to TESTTHREADENTERED,
 Does "threadDestroy(0)
 Sets global "m_testThreadState" to TESTTHREADDESTROYFAILED,
 Exits.

 */

/****************************************************************************
 *
 *  testThread_simple() - simple test thread function.
 *
 *    This function gets launched in a new thread to verify 'threadCreate()'
 *
 *       Gets current time and puts it in "m_testThreadStartTime"
 *       Sets global "m_testThreadState" to TESTTHREADENTERED,
 *       Saves it's data pointer parameter in global "m_testThreadData",
 *       If data pointer parameter != NULL, increment what it points at,
 *       Gets it's thread ID and puts it in global "m_testThreadID",
 *       Exits.
 *
 */

void testThread_simple(void *tls)
{
    mpe_Error ec;

    ec = timeGetMillis(&m_testThreadStartTime); /* log test thread start time */

    /*  Indicate that test thread started  */

    m_testThreadState = TESTTHREADENTERED;

    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "    >>>> entered 'testThread_simple()', data pointer == %p\n", tls);

    /*  save data pointer  */

    m_testThreadDataPointer = (uint32_t*) tls;

    /* if data pointer isn't NULL, save what it points at  */

    if (NULLID != tls)
    {
        m_testThreadData = *((uint32_t*) tls); /* read from data pointer  */
        *((uint32_t*) tls) = m_testThreadData + 1; /* write to data pointer  */
    }

    /* Get test thread ID. */

    ec = threadGetCurrent(&m_testThreadID);

    // TODO: Exception occuring here, need workaround
    //CuAssert(m_currentTest, "threadGetCurrent() failed", ec == MPE_SUCCESS);

    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "    <<<< 'testThread_simple()' finished.\n");

    // should this do : "m_testThreadState = TESTTHREADFINISHED;" before exiting ????

}
/*************************************************************************
 *
 * testThread_donothing() - Test thread that does not do anything
 *
 *		Basically the thread once started goes to sleep for 10 seconds
 *		 It is designed to be killed before it is woken up.
 */

void testThread_donothing(void *tls)
{
    mpe_Error ec;

    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "         >>>> entered 'testThread_donothing \n");

    /*  Indicate that test thread started  */

    m_testThreadState = TESTTHREADENTERED;

    threadSleep(2000, 0);

    /* get status of current thread, if possible */

    ec = threadGetStatus(0, &m_testThreadStatus);
    CuAssert(m_currentTest, "threadGetStatus(0) failed", ec == MPE_SUCCESS);

    /*  Validate through the change in status with the thread */

    if (MPE_THREAD_STAT_DEATH == m_testThreadStatus)
    {
        m_testThreadState = TESTTHREADDEATH;
    }
    else
    {
        m_testThreadState = TESTTHREADDESTROYFAILED; /* flag error for main thread  */
        CuAssert(m_currentTest, "threadDestroy() failed", m_testThreadState
                == TESTTHREADDESTROYFAILED);
        TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
                "         testThread_destroyID() failed to destroy thread ! ! !\n");
    }
}

/****************************************************************************
 *
 *  testThread_sleep() - test thread function.
 *
 *    This function gets launched in a new thread. It sets some global
 *    variables, sleeps two seconds, checks that it actually slept for about
 *    two seconds, gets it's thread-specific data pointer and puts it in a
 *    global variable, sets a gloabal variable to indicates that it's
 *    finished and exits.

 Gets current time and puts it in "m_testThreadStartTime"
 Sets global "m_testThreadState" to TESTTHREADENTERED,
 Gets it's thread ID and puts it in global "m_testThreadID",
 Saves it's data pointer parameter in global "m_testThreadData",
 Calls 'threadGetData()' and saves it's "thread local storage" pointer,
 Sleeps 2 seconds
 Calls 'threadGetData()' again and saves it in a different location,
 Sets global "m_testThreadState" to TESTTHREADFINISHED,
 Exits.

 */

static void testThread_sleep(void *tls)
{
    mpe_TimeMillis endTime;
    mpe_Error ec;
    int sleepTime;

    ec = timeGetMillis(&m_testThreadStartTime); /* log test thread start time */

    /*  Indicate that test thread started  */

    m_testThreadState = TESTTHREADENTERED;

    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "    >>>>> entered 'testThread_sleep()', data pointer == %p\n", tls);

    /*  save data pointer  */

    m_testThreadDataPointer = (uint32_t*) tls;

    /* if data pointer isn't NULLID, save what it points at  */

    if (NULLID != tls)
    {
        m_testThreadData = *((uint32_t*) tls); /* read from data pointer  */
        *((uint32_t*) tls) = m_testThreadData + 1; /* write to data pointer  */
    }

    /* Get test thread ID. */

    ec = threadGetCurrent(&m_testThreadID);
    CuAssert(m_currentTest, "threadGetCurrent() failed", ec == MPE_SUCCESS);

    /* get local storage pointer for this thread  */

    ec = threadGetData(0, &m_testThreadLocalPointer1);
    CuAssert(m_currentTest,
            "threadGetData() failed in 'testThread_sleep()' (1)", ec
                    == MPE_SUCCESS);

    /* get status of current thread. */

    ec = threadGetStatus(0, &m_testThreadStatus);
    CuAssert(m_currentTest, "threadGetStatus(0) failed", ec == MPE_SUCCESS);

    /* Sleep 2 seconds  */

    ec = threadSleep(2000, 0);
    CuAssert(m_currentTest,
            "threadSleep() returned an error in 'testThread_sleep()'", ec
                    == MPE_SUCCESS);

    /* Get the current time. */

    ec = timeGetMillis(&endTime);
    sleepTime = (int) (endTime - m_testThreadStartTime);

    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "         'testThread_sleep()' slept %d milliSeconds\n", sleepTime);

    CuAssert(m_currentTest,
            "threadSleep() did not sleep correct time in 'testThread_sleep()'",
            ((sleepTime > 2000) && (sleepTime <= 2200)));

    /* get local storage pointer for this thread again  */

    ec = threadGetData(0, &m_testThreadLocalPointer2);
    CuAssert(m_currentTest,
            "threadGetData() failed in 'testThread_sleep()' (2)", ec
                    == MPE_SUCCESS);

    /*  if local storage pointer isn't NULLID, write something there  */

    if (NULLID != m_testThreadLocalPointer2)
    {
        *((int*) m_testThreadLocalPointer2) = TESTTHREADDATAVALUE;
    }

    /* get status of current thread. */

    ec = threadGetStatus(0, &m_testThreadStatus);
    CuAssert(m_currentTest, "threadGetStatus(0) failed", ec == MPE_SUCCESS);

    /*  Indicate that test thread finished  */

    if (MPE_THREAD_STAT_DEATH == m_testThreadStatus)
    {
        m_testThreadState = TESTTHREADDEATH;
    }
    else
    {
        m_testThreadState = TESTTHREADFINISHED;
    }

    TRACE(
            MPE_LOG_TRACE5,
            MPE_MOD_TEST,
            "    <<<< 'testThread_sleep()' finished, final state == %d, final status == %d\n",
            m_testThreadState, m_testThreadStatus);

}

/****************************************************************************

 'testThread_busy()' - Thread code which is launched to waste some CPU
 time while other stuff is happening.  Just dead loops for 2 seconds,
 repeatedly calling 'timeGetMillis()'.

 ****/

static void testThread_busy(void *tls)
{
    mpe_Error ec;
    long int loops = 0;
    long int i = 1;
    mpe_TimeMillis delay = 2000;
    mpe_TimeMillis start;
    mpe_TimeMillis now;
    mpe_TimeClock startCPU;
    mpe_TimeClock endCPU;
    long int msec;
    long int ticks;

    ec = timeGetMillis(&m_testThreadStartTime); /* log test thread start time */

    /*  Indicate that test thread started  */

    m_testThreadState = TESTTHREADENTERED;

    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "    >>>> entered 'testThread_busy()', data pointer == %p\n", tls);

    /*  save data pointer  */

    m_testThreadDataPointer = (uint32_t*) tls;

    /* if data pointer isn't NULLID, save what it points at  */

    if (NULLID != tls)
    {
        m_testThreadData = *((uint32_t*) tls); /* read from data pointer  */
        *((uint32_t*) tls) = m_testThreadData + 1; /* write to data pointer  */
    }

    /* Get test thread ID. */

    ec = threadGetCurrent(&m_testThreadID);
    CuAssert(m_currentTest, "threadGetCurrent() failed", ec == MPE_SUCCESS);

    /* get initial CPU time */

    startCPU = timeClock();

    if ((ec = timeGetMillis(&start)) != MPE_SUCCESS)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "      testThread_busy() - timeGetMillis() failed (1)\n");
        return;
    }

    /* dead loop for 2 seconds  */

    do
    {
        if ((ec = timeGetMillis(&now)) != MPE_SUCCESS)
        {
            TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                    "       testThread_busy() - timeGetMillis() failed (2)\n");
            break;
        }
        loops++;
        i += testThread_wasteTime(loops, i);
    } while (now < start + delay);

    endCPU = timeClock();
    ticks = (long) (endCPU - startCPU);
    msec = (long) (now - start);

    threadSleep(100, 0); /* sleep a while */

    TRACE(
            MPE_LOG_DEBUG,
            MPE_MOD_TEST,
            "    <<<<< testThread_busy()  - loops == %ld, msec == %ld, ticks == %ld\n",
            (long) loops, msec, ticks);

    m_testThreadState = TESTTHREADFINISHED; /* indicate test thread finished  */
}

/****************************************************************************

 'testThread_wasteTime()' - Function which gets called to waste some time.
 Don't try to understand what the code does (I don't). Hopefully, this
 doesn't get optimized away (or inlined).

 ****/

static long int testThread_wasteTime(long int x, long int y)
{
    mpe_Error ec;
    static long int i = 53; /* just some value  */
    static long int j = 17; /* just some other value  */

    ec = MPE_SUCCESS;
    j += (long int) ec + y;
    i += j + x;
    j += i;
    j -= x * i + y;
    if (0 == x)
    {
        i += y;
    }
    else
    {
        i += j / x;
    }

    //    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,"i = %10ld, j = %10ld\n", i, j);

    return (i * j + 3);
}

/****************************************************************************
 *
 *  testThread_destroyID() - test 'threadDestroy(ID)
 *
 *    This function gets launched in a new thread to verify 'threadDestroy()'
 *    by ID. It sets it state and puts it's thread ID into global variables,
 *    does 'threadDestroy()' by ID, then sets a different global state.
 *
 */

void testThread_destroyID(void *tls)
{
    mpe_Error ec;

    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "    >>>> entered 'testThread_destroyID()', data pointer == %p\n",
            tls);

    /*  Indicate that test thread started  */

    m_testThreadState = TESTTHREADENTERED;

    /* Get test thread ID. */

    ec = threadGetCurrent(&m_testThreadID);
    CuAssert(m_currentTest, "threadGetCurrent() failed", ec == MPE_SUCCESS);

    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "         testThread_destroyID() calling threadDestroy(ID)\n");

    ec = threadDestroy(m_testThreadID);

    threadSleep(1000, 0);

    /* get status of current thread, if possible */

    ec = threadGetStatus(0, &m_testThreadStatus);
    CuAssert(m_currentTest, "threadGetStatus(0) failed", ec == MPE_SUCCESS);

    /*  Validate through the change in status with the thread */

    if (MPE_THREAD_STAT_DEATH == m_testThreadStatus)
    {
        m_testThreadState = TESTTHREADDEATH;
    }
    else
    {
        m_testThreadState = TESTTHREADDESTROYFAILED; /* flag error for main thread  */
        CuAssert(m_currentTest, "threadDestroy() failed", m_testThreadState
                == TESTTHREADDESTROYFAILED);
        TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
                "         testThread_destroyID() failed to destroy thread ! ! !\n");
    }
}

/****************************************************************************
 *
 *  testThread_destroy0() - test 'threadDestroy(0)
 *
 *    This function gets launched in a new thread to verify 'threadDestroy()'
 *    with ID == 0. It sets a global variable to indicate the test thread
 *    started, does 'threadDestroy(0)', then sets a different global state.
 */

void testThread_destroy0(void *tls)
{
    mpe_Error ec;

    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "    >>>> entered 'testThread_destroy0()', data pointer == %p\n",
            tls);

    /*  Indicate that test thread started  */

    m_testThreadState = TESTTHREADENTERED;

    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "         'testThread_destroy0()' calling 'threadDestroy(0)'\n");

    ec = threadDestroy(0);

    /* get status of current thread, if possible */

    ec = threadGetStatus(0, &m_testThreadStatus);
    CuAssert(m_currentTest, "threadGetStatus(0) failed", ec == MPE_SUCCESS);

    /*  Validate through the change in status with the thread */

    if (MPE_THREAD_STAT_DEATH == m_testThreadStatus)
    {
        m_testThreadState = TESTTHREADDEATH;
    }
    else
    {
        m_testThreadState = TESTTHREADDESTROYFAILED; /* flag error for main thread  */
        CuAssert(m_currentTest, "threadDestroy() failed", m_testThreadState
                == TESTTHREADDESTROYFAILED);
        TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
                "         testThread_destroyID() failed to destroy thread ! ! !\n");
    }
}

/**
 * getTestSuite_sysThread
 *
 * Create and return the test suite for the mpe_mem APIs.
 *
 * @return a pointer to the new test suite.
 */
CuSuite* getTestSuite_sysThread(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, vte_test_threadAttach);
    SUITE_ADD_TEST(suite, vte_test_threadCreate);
    SUITE_ADD_TEST(suite, vte_test_threadDestroyID0);
    SUITE_ADD_TEST(suite, vte_test_threadDestroyID);
    SUITE_ADD_TEST(suite, vte_test_threadDestroyOther);
    SUITE_ADD_TEST(suite, vte_test_threadGetCurrent);
    SUITE_ADD_TEST(suite, vte_test_threadGetData);
    SUITE_ADD_TEST(suite, vte_test_threadGetStatus);
    SUITE_ADD_TEST(suite, vte_test_threadSetData);
    SUITE_ADD_TEST(suite, vte_test_threadSetPriority);
    SUITE_ADD_TEST(suite, vte_test_threadSetStatus);
    SUITE_ADD_TEST(suite, vte_test_threadSleep);
    SUITE_ADD_TEST(suite, vte_test_threadYield);
    SUITE_ADD_TEST(suite, vte_test_threadData);
    SUITE_ADD_TEST(suite, vte_test_threadStatus);

    return suite;
}
