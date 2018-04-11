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

/** \file test_sys_sync.c
 *
 *  \brief Test functions for MPEOS synchronization functions
 *
 *  This file contains tests for the following MPEOS functions :
 *
 *    -# mpeos_condDelete()
 *    -# mpeos_condGet()
 *    -# mpeos_condNew()
 *    -# mpeos_condSet()
 *    -# mpeos_condUnset()
 *    -# mpeos_condWaitFor()
 *    -# mpeos_mutexAcquire()
 *    -# mpeos_mutexAcquireTry()
 *    -# mpeos_mutexDelete()
 *    -# mpeos_mutexNew()
 *    -# mpeos_mutexRelease()
 *
 */

#define _MPE_SYNC
#define TEST_MPE_SYNC

#include <cutest.h>
#include <mpe_sys.h>
#include "test_sys.h"

static CuTest *m_mpetest_syncTest; /* Current test case structure pointer. */

static uint32_t m_ThreadStatus; /* Shared variable for status notification. */
static int32_t m_syncData; /* Shared variable protected by mutex */
static int32_t m_testThreadAcquired = 0; /* number of times test thread acquired the mutex */
static int32_t m_testThreadState; /* current state of test thread  */
static mpe_Mutex syncMutex;
static mpe_Cond m_sharedCond;
static mpe_Cond pingCond;
static mpe_Cond pongCond;
static uint32_t ping, pong;
static mpe_Error m_testThreadError; /* test thread error status  */

/*  Sync test functions  */

static void vte_test_condDelete(CuTest*);
static void vte_test_condGetSet(CuTest*);
static void vte_test_condNew(CuTest*);
//static void vte_test_condSet(CuTest*);
//static void vte_test_condUnset(CuTest*);
static void vte_test_condWaitFor(CuTest*);
static void vte_test_condPingPong(CuTest*);
static void vte_test_mutexAcquire(CuTest*);
static void vte_test_mutexAcquireTry(CuTest*);
//static void vte_test_mutexDelete(CuTest*);
static void vte_test_mutexNew(CuTest*);
//static void vte_test_mutexRelease(CuTest*);
//static void vte_test_mutexStress(CuTest*);


/*  Function to add sync tests to a test suite  */

CuSuite* getTestSuite_sysSync(void);

/*  Internal functions  */

static void mpetest_mutexThread(void*);
//static void mpetest_condThread(void*);
static void mpetest_pongThread(void*);
static void mpetest_condWaitForThread(void*);
static void mpetest_condGetterThread(void *);

/****************************************************************************
 *
 *  vte_test_condDelete()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_condDelete" function
 *
 * \api mpeos_condDelete()
 *
 * \strategy Create a cond, call "mpeos_condDelete()" to delete it
 *
 * \assets none
 *
 */

static void vte_test_condDelete(CuTest *tc)
{
    mpe_Error ec;
    mpe_Cond cond;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_condDelete\n");

#ifndef POWERTV    /*  The following test case crashes on the SA 8300HD / PowerTV  */
    /**
     * \assertion "MPE_EINVAL" is returned if a NULL mpe_Cond is passed
     */

    ec = condDelete(NULL);
    CuAssert(tc, "condDelete(NULL) failed", MPE_EINVAL == ec);
#endif  /* #ifndef POWERTV  */

    /*  Create a test condition variable */

    ec = condNew(FALSE, FALSE, &cond);
    CuAssert(tc, "condNew(&cond) failed", MPE_SUCCESS == ec);

    /**
     * \assertion "MPE_SUCCESS" is returned if a valid mpe_Cond is passed
     */

    ec = condDelete(cond);
    CuAssert(tc, "condDelete(cond) failed", MPE_SUCCESS == ec);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_condDelete finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_condGet()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_condGet" function
 *
 * \api mpeos_condGet()
 *
 * \strategy  Create a test cond, initial state TRUE, fork 'getterThread', 
 *            sleep a while, check that getterThread got cond.
 *            Create a cond, initial state FALSE, fork 'getterThread', sleep a
 *            while, check that getterThread hasn't gotten cond, do condSet,
 *            sleep, check that getterThread got cond
 *
 * \assets none
 *

 waiterThread does :
 set shared variable to indicate started
 do condWaitFor (500) on shared cond
 set shared variable to indicate result - MPE_SUCCESS or MPE_EBUSY

 */

static void vte_test_condGetSet(CuTest *tc)
{
    mpe_ThreadId tid = 0;
    mpe_Error ec;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_condGetSet\n");

    m_mpetest_syncTest = tc;

    /* Init shared data. */
    m_syncData = 0;

#ifndef POWERTV    /*  The following test case crashes on the SA 8300HD / PowerTV  */
    /**
     * \assertion "MPE_EINVAL" is returned if a NULL mpe_Cond is passed
     */

    ec = condGet(NULL);
    CuAssert(tc, "condGet(NULL) failed to return expected error", ec
            == MPE_EINVAL);
#endif  /* #ifndef POWERTV  */

    /*
     * Create a condition variable with auto-reset enabled
     * and initial state set (TRUE)...
     */

    CuTestSetup((ec = condNew(TRUE, TRUE, &m_sharedCond)));
    CuAssert(tc, "condNew(&cond) failed", MPE_SUCCESS == ec);

    /*  Init shared vars  */

    m_testThreadState = TESTTHREADINVALID;
    m_testThreadError = MPE_SUCCESS;

    /*  Launch test thread  */

    ec = threadCreate(mpetest_condGetterThread, &m_ThreadStatus,
            MPE_THREAD_PRIOR_DFLT, MPE_THREAD_STACK_SIZE, &tid,
            "mpetest_condGetterThread");
    CuAssert(tc, "threadCreate() failed", MPE_SUCCESS == ec);

    threadSleep(100, 0); /*  allow test thread time to run  */

    /**
     * \assertion Test thread acquired the condition
     */

    CuAssert(tc, "Test thread failed to get cond", TESTTHREADFINISHED
            == m_testThreadState);

    /**
     * \assertion 'condGet()' in test thread did not return an error
     */

    CuAssert(tc, "Test thread received an error on 'condGet()'", MPE_SUCCESS
            == m_testThreadError);

    /*   Delete the test cond  */
    ec = condDelete(m_sharedCond);
    CuAssert(tc, "condDelete(cond) failed", MPE_SUCCESS == ec);

    /*
     * Create a condition variable with auto-reset enabled, initial state set to FALSE
     */

    CuTestSetup((ec = condNew(TRUE, FALSE, &m_sharedCond)));
    CuAssert(tc, "condNew(&cond) failed", ec == MPE_SUCCESS);

    /*  Init shared vars  */

    m_testThreadState = TESTTHREADINVALID;
    m_testThreadError = MPE_SUCCESS;

    /*  Launch test thread  */

    ec = threadCreate(mpetest_condGetterThread, &m_ThreadStatus,
            MPE_THREAD_PRIOR_DFLT, MPE_THREAD_STACK_SIZE, &tid,
            "mpetest_condGetterThread");
    CuAssert(tc, "threadCreate() failed", MPE_SUCCESS == ec);

    threadSleep(100, 0); /*  allow test thread time to start  */

    /**
     * \assertion Test thread started and is waiting on the condition
     */

    CuAssert(tc, "Test thread failed to start", TESTTHREADINVALID
            != m_testThreadState);
    CuAssert(tc, "Test thread failed to wait on cond", TESTTHREADENTERED
            == m_testThreadState);

    /**
     * \assertion 'condGet()' in test thread did not return an error
     */

    CuAssert(tc, "Test thread received an error on 'condGet()'", MPE_SUCCESS
            == m_testThreadError);

    /*  Do 'condSet() on test cond  */

    ec = condSet(m_sharedCond);

    /**
     * \assertion 'condSet()' did not return an error
     */

    CuAssert(tc, "Error on 'condSet()'", MPE_SUCCESS == ec);

    threadSleep(100, 0); /*  allow test thread time to run  */

    /**
     * \assertion Test thread acquired the condition
     */

    CuAssert(tc, "Test thread failed to get cond", TESTTHREADFINISHED
            == m_testThreadState);

    /**
     * \assertion 'condGet()' in test thread did not return an error
     */

    CuAssert(tc, "Test thread received an error on 'condGet()'", MPE_SUCCESS
            == m_testThreadError);

    /*   Delete the test cond  */
    ec = condDelete(m_sharedCond);
    CuAssert(tc, "condDelete(cond) failed", MPE_SUCCESS == ec);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_mutexCondGetSet finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_condNew()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_condNew" function
 *
 * \api mpeos_condNew()
 *
 * \strategy Call the "mpeos_condNew()" function and checks for reasonable
 * return values.
 *
 * \assets none
 *
 */

static void vte_test_condNew(CuTest *tc)
{
    mpe_Error ec;
    mpe_Cond cond1 = NULL;
    mpe_Cond cond2 = NULL;
    mpe_Cond cond3 = NULL;
    mpe_Cond cond4 = NULL;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_condNew\n");

    /**
     * \assertion CondNew returns MPE_EINVAL when passed a NULL mpe_Cond pointer
     */

    ec = condNew(FALSE, FALSE, NULL);
    CuAssert(tc, "condNew(NULL) failed NULL function check", MPE_EINVAL == ec);

    /**
     * \assertion CondNew returns MPE_SUCCESS when passed valid arguments
     */

    ec = condNew(FALSE, FALSE, &cond1);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    cond1 == 0x%08x\n", (int) cond1);
    CuAssert(tc, "condNew(&cond1) failed", MPE_SUCCESS == ec);

    /**
     * \assertion CondNew returns non-null mpe_Cond when passed valid arguments
     */

    CuAssert(tc, "condNew(&cond1) returned NULL mpe_Cond", NULL != cond1);

    /**
     * \assertion CondNew returns MPE_SUCCESS when passed valid arguments again
     */

    ec = condNew(TRUE, FALSE, &cond2);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    cond2 == 0x%08x\n", (int) cond2);
    CuAssert(tc, "condNew(TRUE, FALSE, &cond2) failed", MPE_SUCCESS == ec);

    /**
     * \assertion CondNew returns MPE_SUCCESS when passed valid arguments again
     */

    ec = condNew(FALSE, TRUE, &cond3);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    cond3 == 0x%08x\n", (int) cond3);
    CuAssert(tc, "condNew(FALSE, TRUE, &cond3) failed", MPE_SUCCESS == ec);

    /**
     * \assertion CondNew returns MPE_SUCCESS when passed valid arguments again
     */

    ec = condNew(TRUE, TRUE, &cond4);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    cond4 == 0x%08x\n", (int) cond4);
    CuAssert(tc, "condNew(TRUE, TRUE, &cond4) failed", MPE_SUCCESS == ec);

    /**
     * \assertion Calling CondNew multiple times returns unique mpe_Cond's
     *  for each call.
     */

    CuAssert(tc, "condNew() returned duplicate mpe_cond (cond1 == cond2)",
            cond1 != cond2);
    CuAssert(tc, "condNew() returned duplicate mpe_cond (cond1 == cond3)",
            cond1 != cond3);
    CuAssert(tc, "condNew() returned duplicate mpe_cond (cond1 == cond4)",
            cond1 != cond4);
    CuAssert(tc, "condNew() returned duplicate mpe_cond (cond2 == cond3)",
            cond2 != cond3);
    CuAssert(tc, "condNew() returned duplicate mpe_cond (cond2 == cond4)",
            cond2 != cond4);
    CuAssert(tc, "condNew() returned duplicate mpe_cond (cond3 == cond4)",
            cond3 != cond4);

    /*  Cleanup - delete test mpe_Cond's  */

    ec = condDelete(cond1);
    CuAssert(tc, "condDelete(cond1) failed", MPE_SUCCESS == ec);
    ec = condDelete(cond2);
    CuAssert(tc, "condDelete(cond2) failed", MPE_SUCCESS == ec);
    ec = condDelete(cond3);
    CuAssert(tc, "condDelete(cond3) failed", MPE_SUCCESS == ec);
    ec = condDelete(cond4);
    CuAssert(tc, "condDelete(cond4) failed", MPE_SUCCESS == ec);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_mutexCondNew finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_mutexNew()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mutexNew" function
 *
 * \api mutexNew()
 *
 * \strategy Call the "mutexNew()" function and checks for reasonable
 * return values.
 *
 * \assets none
 *
 */

static void vte_test_mutexNew(CuTest *tc)
{
    mpe_Error ec;
    mpe_Mutex mutex;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_mutexNew\n");

    /**
     * \assertion "MPE_EINVAL" is returned if a NULL pointer is passed
     */

    ec = mutexNew(NULL);
    CuAssert(tc, "mutexNew(NULL) failed NULL function check", MPE_EINVAL == ec);

    /**
     * \assertion mutexNew sucessfully creates a new mutex
     */

    ec = mutexNew(&mutex);
    CuAssert(tc, "mutexNew(&mutex) failed", MPE_SUCCESS == ec);

    /**
     * \assertion mutexDelete sucessfully deletes the mutex
     */

    ec = mutexDelete(mutex);
    CuAssert(tc, "mutexDelete(mutex) failed", ec == MPE_SUCCESS);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_mutexNew finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_mutexAcquire()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mutexAcquire" function
 *
 * \api mutexAcquire()
 *
 * \strategy Call the "mutexAcquire()" function and checks for reasonable
 * return values.
 *
 * \assets none
 *
 */

static void vte_test_mutexAcquire(CuTest *tc)
{
    mpe_ThreadId tid = 0;
    mpe_Error ec;
    uint32_t i;
    int32_t mainThreadAcquired = 0;
    char msgbuf[200];

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_mutexAcquire\n");

    m_mpetest_syncTest = tc;

    /* Init shared data. */
    m_syncData = 0;
    m_testThreadAcquired = 0;

    CuTestSetup((ec = mutexNew(&syncMutex)));
    CuAssert(tc, "mutexNew(&mutex) failed", MPE_SUCCESS == ec);

    /* Create a test thread for mutex competition. */
    m_ThreadStatus = 0;
    ec = threadCreate(mpetest_mutexThread, &m_ThreadStatus,
            MPE_THREAD_PRIOR_DFLT, MPE_THREAD_STACK_SIZE, &tid, "mutexAcquire");
    CuAssert(tc, "threadCreate() failed", MPE_SUCCESS == ec);

    /* Wait for test thread to start */

    for (i = 0; i < 100; i++)
    {
        if (99 == m_ThreadStatus)
        {
            break;
        }
        threadSleep(1, 0);
    }
    CuAssert(tc, "threadCreate() failed to start a thread", 99
            == m_ThreadStatus);

    for (i = 0; i < 100; ++i)
    {
        /**
         * \assertion mutexAcquire sucessfully acquires the mutex
         */
        ec = mutexAcquire(syncMutex);
        CuAssert(tc, "mutexAcquire() failed", MPE_SUCCESS == ec);
        mainThreadAcquired++;

        m_syncData |= (0x0000FFFF);

        /**
         * \assertion mutexAcquire acquires exclusive access to the mutex
         */

        CuAssert(tc, "mutexAcquire() failed to maintain exclusivity",
                m_syncData != (-1));

        threadSleep(2, 0);

        /**
         * \assertion mutexAcquire acquires exclusive access to the mutex
         */

        CuAssert(tc, "mutexAcquire() failed to maintain exclusivity",
                m_syncData != (-1));

        m_syncData &= (0xFFFF0000);

        /**
         * \assertion mutexRelease sucessfully releases the mutex
         */
        ec = mutexRelease(syncMutex);
        CuAssert(tc, "mutexRelease() failed", MPE_SUCCESS == ec);

        threadSleep(3, 0);
    }

    /*  Wait for test thread to exit */

    while (99 == m_ThreadStatus) /* Wait for thread to exit. */
    {
        threadSleep(100, 0);
    }

    /* Now delete mutex... */
    CuTestCleanup((ec = mutexDelete(syncMutex)));
    CuAssert(tc, "mutexDelete(mutex) failed", MPE_SUCCESS == ec);

    sprintf(
            msgbuf,
            "vte_test_mutexAcquire - main thread acguired mutex %ld times, test thread acquired mutex %ld times\n",
            mainThreadAcquired, m_testThreadAcquired);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, msgbuf);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_mutexAcquire finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_mutexAcquireTry()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mutexAcquireTry" function
 *
 * \api mutexAcquire()
 *
 * \strategy Call the "mutexAcquireTry()" function and checks for reasonable
 * return values.
 *
 * \assets none
 *
 */

static void vte_test_mutexAcquireTry(CuTest *tc)
{
    mpe_ThreadId tid = 0;
    mpe_Error ec;
    uint32_t i;
    uint32_t failedTries = 0;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_mutexAcquireTry\n");

    m_mpetest_syncTest = tc;

    /* Init shared data. */
    m_syncData = 0;

    /* Now create a mutex... */
    CuTestSetup((ec = mutexNew(&syncMutex)));
    CuAssert(tc, "mutexNew(&mutex) failed", MPE_SUCCESS == ec);

    /* Create a thread for mutex competition. */
    ec = threadCreate(mpetest_mutexThread, &m_ThreadStatus,
            MPE_THREAD_PRIOR_DFLT, MPE_THREAD_STACK_SIZE, &tid,
            "mutexAcquireTry");
    CuAssert(tc, "threadCreate() failed", ec == MPE_SUCCESS);

    for (i = 0; i < 100; ++i)
    {
        /* Do an acquire... */
        if (MPE_SUCCESS == (ec = mutexAcquireTry(syncMutex)))
        {
            m_syncData |= (0x0000FFFF);

            CuAssert(tc, "mutexAcquireTry() failed to maintain exclusivity",
                    m_syncData != (-1));

            threadSleep(1, 0);

            CuAssert(tc, "mutexAcquire() failed to maintain exclusivity",
                    m_syncData != (-1));

            m_syncData &= (0xFFFF0000);

            /* Do a release... */
            ec = mutexRelease(syncMutex);
            CuAssert(tc, "mutexRelease() failed", MPE_SUCCESS == ec);
        }
        else
        {
            failedTries++;
            CuAssert(tc, "mutexAcquireTry() failed to return correct error", ec
                    == MPE_EMUTEX);

            threadSleep(0, 100);
        }
    }
    /* Make sure thread did the right thing. */
    CuAssert(tc, "threadCreate() failed to start a thread", m_ThreadStatus
            == 99 || m_ThreadStatus == 100);

    while (m_ThreadStatus == 99) /* Wait for thread to exit. */
    {
        threadSleep(100, 0);
    }

    /* Now delete mutex... */
    CuTestCleanup((ec = mutexDelete(syncMutex)));
    CuAssert(tc, "mutexDelete(mutex) failed", ec == MPE_SUCCESS);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_mutexAcquireTry finished.\n\n");
}

static void vte_test_condPingPong(CuTest *tc)
{
    mpe_ThreadId tid = 0;
    mpe_Error ec, ec1, ec2;
    uint32_t i;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_condPingPong\n");

    m_mpetest_syncTest = tc;

    /* Init shared data. */
    ping = pong = m_syncData = 0;

    /*
     * Create a th "ping" & "pong" condition variables. The ping has auto-reset
     * disabled and initial state set (TRUE), and the pong has auto-reset
     * disabled and initial state set (FALSE)...
     */
    CuTestSetup(((ec1 = condNew(FALSE, TRUE, &pingCond)), (ec2 = condNew(FALSE,
            FALSE, &pongCond))));

    CuAssert(tc, "condNew(&cond) failed", ec1 == MPE_SUCCESS);
    CuAssert(tc, "condNew(&cond) failed", ec2 == MPE_SUCCESS);

    /* Create the "pong" thread. */
    ec = threadCreate(mpetest_pongThread, &m_ThreadStatus,
            MPE_THREAD_PRIOR_DFLT, MPE_THREAD_STACK_SIZE, &tid, "foo");
    CuAssert(tc, "threadCreate() failed", ec == MPE_SUCCESS);

    for (i = 0; i < 100; ++i)
    {
        /* Do a get... */
        ec = condGet(pingCond);
        CuAssert(tc, "condGet(ping) failed", ec == MPE_SUCCESS);

        /* Reset my condition to false... */
        ec = condUnset(pingCond);
        CuAssert(m_mpetest_syncTest, "condUnset(ping) failed", MPE_SUCCESS
                == ec);

        ping++;

        CuAssert(tc, "condGet(ping) failed to maintain synchronization", ping
                == pong + 1);

        /* Now set condition for pong thread... */
        ec = condSet(pongCond);
        CuAssert(tc, "condGet() failed", MPE_SUCCESS == ec);
    }

    /* Make sure thread did the right thing. */
    CuAssert(tc, "threadCreate() failed to start a thread", m_ThreadStatus
            == 99);

    /* Wait for thread to exit. */

    threadSleep(1000, 0);

    /* Now delete conditions... */
    CuTestCleanup(((ec1 = condDelete(pingCond)), (ec2 = condDelete(pongCond))));
    CuAssert(tc, "condDelete(cond) failed", (ec1 == MPE_SUCCESS) && (ec2
            == MPE_SUCCESS));

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_mutexCondPingPong finished.\n\n");
}

/**
 * <i>vte_test_condWaitFor<i/>
 *
 * Validate condWaitFor.
 *
 * @param tc is a pointer to the test case structure.
 */
static void vte_test_condWaitFor(CuTest *tc)
{
    mpe_Time current, start;
    mpe_Error ec;
    mpe_Cond cond;
    mpe_ThreadId tid;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_condWaitFor\n");

    m_mpetest_syncTest = tc;

    /* Now create a condition variable... */
    CuTestSetup((ec = condNew(FALSE, FALSE, &cond)));
    CuAssert(tc, "condNew(&cond) failed", MPE_SUCCESS == ec);

    /* Get the current time. */
    timeGet(&start);

    /* Wait 1000 milliseconds (1 sec)... */
    ec = condWaitFor(cond, 1000);
    timeGet(&current);
    CuAssert(tc, "condWaitFor() failed", MPE_SUCCESS == ec);
    CuAssertIntEquals_Msg(tc, "condWaitFor() failed to wait correct timeout",
            start + 1, current);

    /* Create a thread that will wait on the condition "forever". */
    ec = threadCreate(mpetest_condWaitForThread, &cond, MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_STACK_SIZE, &tid, "mpetest_condWaitForThread");
    CuAssert(tc, "threadCreate() failed", ec == MPE_SUCCESS);

    /* Wait on the same condition as the "wait forever" thread but
     * with a time out. This has the advantage of double-checking the
     * condition variable during the test. */
    ec = condUnset(cond);
    CuAssert(tc, "condSet() failed", ec == MPE_SUCCESS);
    ec = condWaitFor(cond, 2000);
    CuAssert(tc, "condWaitFor() failed", ec == MPE_EBUSY);

    /* Give the thread plenty of opportunity to exit. */
    threadSleep(1000, 0);

    /* Now delete it... */
    CuTestCleanup((ec = condDelete(cond)));
    CuAssert(tc, "condDelete(cond) failed", ec == MPE_SUCCESS);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_mutexCondWaitFor finished.\n\n");
}

/****************************************************************************
 ****************************************************************************

 Test "helper" functions which get launched in a new thread.

 - condGetterThread()
 - testThread_sleep()

 getterThread does :
 set shared variable to indicate started
 do condGet on shared cond
 set shared variable to indicate got cond
 exit

 */

/****************************************************************************
 *
 *  condGetterThread() - Attempts to get a cond, sets shared variable to
 *                       indicate progress
 *
 *    This function gets launched in a new thread to verify 'condGet()'
 *
 */

static void mpetest_condGetterThread(void *tls)
{
    m_testThreadState = TESTTHREADENTERED; /*  indicate test thread started  */
    m_testThreadError = condGet(m_sharedCond); /*  attempt to get cond  */
    m_testThreadState = TESTTHREADFINISHED; /*  indicate test thread finished  */
}

/**
 * <i>mpetest_pongThread<i/>
 *
 * This is the code for that is launched to verify cond APIs when
 * auto reset is false.
 *
 * @param tls is a pointer to the shared variable.
 */
static void mpetest_pongThread(void *tls)
{
    mpe_Error ec;
    uint32_t *sd = (uint32_t*) tls;
    uint32_t i;

    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "    >>>> entered 'mpetest_pongThread()', tls == %p\n", tls);

    /* Validate shared data pointer & set result. */
    if (sd == &m_ThreadStatus)
        *sd = 99;

    for (i = 0; i < 100; ++i)
    {
        /* Do an acquire... */
        ec = condGet(pongCond);
        CuAssert(m_mpetest_syncTest, "condGet(pong) failed", ec == MPE_SUCCESS);

        /* Reset my condition to false... */
        ec = condUnset(pongCond);
        CuAssert(m_mpetest_syncTest, "condUnset(pong) failed", ec
                == MPE_SUCCESS);

        pong++;

        CuAssert(m_mpetest_syncTest,
                "condGet(pong) failed to maintain synchronization", ping
                        == pong);

        /* Now set condition for ping thread... */
        ec = condSet(pingCond);
        CuAssert(m_mpetest_syncTest, "condSet(ping) failed", ec == MPE_SUCCESS);
    }
    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "    <<<< 'mpetest_pongThread()' exiting\n");
}

/**
 * <i>mpetest_mutexThread<i/>
 *
 * This is the code for that is launched to verify mutex APIs.
 *
 * @param tls is a pointer to the shared variable.
 */
static void mpetest_mutexThread(void *tls)
{
    mpe_Error ec;
    uint32_t *sd = (uint32_t*) tls;
    uint32_t i;

    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "    >>>> entered 'mpetest_mutexThread()', tls == %p\n", tls);

    /* Validate shared data pointer & set result. */
    if (sd == &m_ThreadStatus)
        *sd = 99;

    for (i = 0; i < 100; ++i)
    {
        /* Do an acquire... */
        ec = mutexAcquire(syncMutex);
        CuAssert(m_mpetest_syncTest, "mutexAcquire() failed", ec == MPE_SUCCESS);
        m_testThreadAcquired++;

        m_syncData |= (0xFFFF0000);

        CuAssert(m_mpetest_syncTest,
                "mutexAcquire() failed to maintain exclusivity", m_syncData
                        != (-1));

        threadSleep(1, 0);

        CuAssert(m_mpetest_syncTest,
                "mutexAcquire() failed to maintain exclusivity", m_syncData
                        != (-1));

        m_syncData &= (0x0000FFFF);

        /* Do a release... */
        ec = mutexRelease(syncMutex);
        CuAssert(m_mpetest_syncTest, "mutexRelease() failed", ec == MPE_SUCCESS);

        threadSleep(0, 200);
    }

    *sd = 100; /*  tell main thread we're done  */

    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "    <<<< 'mpetest_mutexThread()' exiting\n");
}

/**
 * Thread function used for testing condWaitFor semantics.
 * @param tls This *must* be a pointer to a mpe_Cond variable.
 */
static void mpetest_condWaitForThread(void *tls)
{
    mpe_Error ec = 0;
    mpe_Cond *cond = (mpe_Cond *) tls;

    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "    >>>> entered 'mpetest_condWaitForThread()', tls == %p\n", tls);
    ec = condWaitFor(*cond, 0);
    CuAssert(m_mpetest_syncTest, "condWaitFor() failed", ec == MPE_SUCCESS);

    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "    <<<< 'mpetest_condWaitForThread()' exiting\n");

}

/**
 * getTestSuite_sysSync
 *
 * Create and return the test suite for the mutex and
 * cond APIs.
 *
 * @return a pointer to the new test suite.
 */
CuSuite* getTestSuite_sysSync(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, vte_test_condNew);
    SUITE_ADD_TEST(suite, vte_test_condDelete);
    SUITE_ADD_TEST(suite, vte_test_condGetSet);
    //    SUITE_ADD_TEST(suite, vte_test_condSet);
    //    SUITE_ADD_TEST(suite, vte_test_condUnSet);
    SUITE_ADD_TEST(suite, vte_test_condWaitFor);
    SUITE_ADD_TEST(suite, vte_test_condPingPong);

    SUITE_ADD_TEST(suite, vte_test_mutexNew);
    //    SUITE_ADD_TEST(suite, vte_test_mutexDelete);
    SUITE_ADD_TEST(suite, vte_test_mutexAcquire);
    SUITE_ADD_TEST(suite, vte_test_mutexAcquireTry);
    //    SUITE_ADD_TEST(suite, vte_test_mutexRelease);
    //    SUITE_ADD_TEST(suite, vte_test_mutexStress);

    return suite;
}
