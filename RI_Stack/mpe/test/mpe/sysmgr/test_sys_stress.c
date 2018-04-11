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

/** \file test_sys_stress.c
 *
 *  \brief Stress tests MPEOS system functions
 *
 *  This file contains stress tests for the following MPEOS functions :\n
 *
 *    -# mpeos_memAllocH()\n
 *    -# mpeos_memAllocP()\n
 *    -# mpeos_memCompact()\n
 *    -# mpeos_memFreeH()\n
 *    -# mpeos_memFreeP()\n
 *    -# mpeos_memGetFreeSize()\n
 *    -# mpeos_memGetLargestFree()\n
 *    -# mpeos_memGetStats()\n
 *    -# mpeos_memInit()\n
 *    -# mpeos_memLockH()\n
 *    -# mpeos_memPurge()\n
 *    -# mpeos_memReallocH()\n
 *    -# mpeos_memReallocP()\n
 *    -# mpeos_memStats()\n
 */

#include <test_sys.h>
#include <stdlib.h>
#include <mpe_sys.h>
#include <mpeos_mem.h>
#include <mpeos_gfx.h>
#include <mpe_gfx.h>
#include <mpetest_gfx.h>
#include <mpeos_uievent.h>

CuSuite* getTestSuite_sysStress(void);

#ifdef TEST_MPEOS
# define initMem mpeos_memInit         /* need to call 'mpeos_memInit()' if testing MPEOS  */
#else
# define initMem memInitNull           /* 'mpeos_memInit()' has already been called if testing MPE */
static void memInitNull(void);
static void memInitNull()
{
    return;
}
#endif /* TEST_MPEOS */

#define THREAD_B_DATA_1 (0xCAFE)
#define THREAD_B_DATA_2 (0xBABE)
#define EQ_THREAD_C_ID (0x85)

#define MAIN_THREAD_LOOPS   100   /* loop this many times in main thread  */

/*
 *  Thread local storage struct.  One of these is passed to each test thread
 *  when the thread is created. This allows creating multiple thread instances
 *  running the same thread code.
 */

typedef struct
{
    CuTest* tls_tc; /* pointer to the CuTest object  */
    long tls_rngState; /* thread unique random number generator state  */
    int tls_threadNum; /* thread identifier  */
} threadLocal;

#define PATTERN (0xB16B00B5)
#define ALLOCSIZE (32*1024)

#define FRAGFRAGS 17                   /* Number of allocations used to force fragmentation  */
#define FRAGSIZE  (ALLOCSIZE/2)+1      /* Default size of fragmentation allocations  */

/*  Minimum and maximum memory sizes. Right now these are guesses as
 *  to what are reasonable limits.
 */

#define MEMORY_MIN    8388608  /* 8 Mbytes minimum  */
#define MEMORY_MAX  268435456  /* 256 Mbytes maximum */

/*
 *  Test function declarations
 */
static void vte_test_sysStress(CuTest*);
void test_sysRunStressTests(void);

// static void vte_test_threadStress(CuTest*);


/*
 *  Internal utility functions
 */

/*
 *  Memory system inited flag. If true, indicates that 'mpeos_memInit()'
 *  has been called already.
 */

#ifdef TEST_MPEOS
static int g_memInited = 0; /*  initially indicate that 'memInit() has not been called */
#else
static int g_memInited = 1; /*  if doing MPE testing, 'memInit() has already been called */
#endif /* TEST_MPEOS */

/*
 *
 *  Module global Data
 *
 */
static mpe_Mutex gMutex;
static mpe_Cond gCv;
static mpe_EventQueue gEq;
static int32_t m_threadB = THREAD_B_DATA_1;
static int32_t gKeyCount;

/*
 *  Thread functions. These get launched in separate threads
 */

void keyCaptureThread(void *);
void condVarThread(void*);
void eventQueueThread(void*);
void memStressThread(void*);

/****************************************************************************
 *
 *  vte_test_sysStress()
 *
 ****************************************************************************/
/**
 * \test Exercises most features in the kernel portion of mpeos.
 *
 * \note This thread initializes all global data needed by the subsequent
 * test threads.
 */

void vte_test_sysStress(CuTest* tc)
{
    mpe_Error ec = MPE_SUCCESS;
    mpe_ThreadId keyCapTID = 0;
    mpe_ThreadId condVarTID = 0;
    mpe_ThreadId eventQTID = 0;
    mpe_ThreadId memStressTID = 0;

    /*  Thread local storage structs. Each test thread gets a different one  */

    threadLocal tlsKey =
    { tc, 123, 1 };
    threadLocal tlsCond =
    { tc, 7123, 2 };
    threadLocal tlsEvent =
    { tc, 37123, 3 };
    threadLocal tlsMem =
    { tc, 97123, 4 };

    int loops;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_sysStress()\n");

    if (!g_memInited)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  Calling 'mpeos_memInit()'\n");
        initMem();
        g_memInited = 1;
    }

    /* Allocate global data */
    ec = mutexNew(&gMutex);
    CuAssert(tc, "MainThread: Creating a mutex failed", ec == MPE_SUCCESS);

    ec = condNew(TRUE, TRUE, &gCv);
    CuAssert(tc, "MainThread: Creating a condition variable failed", ec
            == MPE_SUCCESS);

    ec = eventQueueNew(&gEq, "mpeosStressTestQ");
    CuAssert(tc, "MainThread: Creating an event queue failed", ec
            == MPE_SUCCESS);

    /* Create subordinate threads */
    /* The key capture thread runs best at MAX priority. At lower priority,
     ** key presses are not always detected.
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    Launching keyCaptureThread\n");
    ec = threadCreate(keyCaptureThread, &tlsKey,
            //MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_PRIOR_MAX, MPE_THREAD_STACK_SIZE, &keyCapTID,
            "keyCaptureThread");
    threadSleep(10, 0);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    Launching condVarThread\n");
    ec = threadCreate(condVarThread, &tlsCond, MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_STACK_SIZE, &condVarTID, "condVarThread");
    threadSleep(10, 0);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    Launching eventQueueThread\n");
    ec = threadCreate(eventQueueThread, &tlsEvent, MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_STACK_SIZE, &eventQTID, "eventQueueThread");
    threadSleep(10, 0);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    Launching memStressThread1\n");
    ec = threadCreate(memStressThread, &tlsMem, MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_STACK_SIZE, &memStressTID, "memStressThread1");
    threadSleep(10, 0);

    m_threadB = THREAD_B_DATA_1;

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "MainThread: Sleep 1000ms. Letting other threads spin up.\n");
    /* Sleep for 1 second */
    ec = threadSleep(1000, 0);

    /* Generate event */
    ec = eventQueueSend(gEq, EQ_THREAD_C_ID, NULL, NULL, 0);
    CuAssert(tc, "MainThread: Generating event failed.", ec == MPE_SUCCESS);

    /*  Loop here MAIN_THREAD_LOOPS times  */

    for (loops = 0; loops < MAIN_THREAD_LOOPS; loops++)
    {
        /* Grab Mutex */
        ec = mutexAcquire(gMutex);
        CuAssert(tc, "MainThread: Acquiring mutex failed.", ec == MPE_SUCCESS);

        /* Modify threadB data */
        m_threadB = THREAD_B_DATA_2;

        /* Release Mutex */
        ec = mutexRelease(gMutex);
        CuAssert(tc, "MainThread: Releasing mutex failed.", MPE_SUCCESS == ec);

        /* Post to CV */
        ec = condSet(gCv);
        CuAssert(tc, "MainThread: Setting CV failed.", MPE_SUCCESS == ec);

        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "MainThread: sleeping\n");
        threadSleep(100, 0);
    }

    /*  Now do cleanup  */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  Main thread sleeping before cleanup\n");
    threadSleep(100, 0);

    ec = threadDestroy(keyCapTID);
    CuAssert(tc, "MainThread: Thread(keyCap) destroy failed.", MPE_SUCCESS
            == ec);
    threadSleep(10, 0);

    ec = threadDestroy(condVarTID);
    CuAssert(tc, "MainThread: Thread(condV) destroy failed.", MPE_SUCCESS == ec);
    threadSleep(10, 0);

    ec = threadDestroy(eventQTID);
    CuAssert(tc, "MainThread: Thread(eventQ) destroy failed.", MPE_SUCCESS
            == ec);
    threadSleep(10, 0);

    ec = threadDestroy(memStressTID);
    CuAssert(tc, "MainThread: Thread(memStress) destroy failed.", MPE_SUCCESS
            == ec);
    threadSleep(10, 0);

    /*  Make sure no one is waiting on resources  */

    ec = condSet(gCv);
    CuAssert(tc, "MainThread: condSet(gCv) failed.", MPE_SUCCESS == ec);

    ec = eventQueueSend(gEq, EQ_THREAD_C_ID, NULL, NULL, 0);
    CuAssert(tc, "MainThread: eventQueueSend() failed.", MPE_SUCCESS == ec);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  Main thread sleeping while other threads exit\n");
    threadSleep(500, 0);

    ec = condDelete(gCv);
    CuAssert(tc, "MainThread: Condition Variable destroy failed.", MPE_SUCCESS
            == ec);

    ec = mutexDelete(gMutex);
    CuAssert(tc, "MainThread: mutexDelete() failed.", MPE_SUCCESS == ec);

    ec = eventQueueDelete(gEq);
    CuAssert(tc, "MainThread: eventQueueDelete() failed.", MPE_SUCCESS == ec);

    /* Wait a while to make sure other threads have time to exit */

    (mpe_Error) threadSleep(100, 0);
    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "\n  vte_test_sysStress() finished\n\n");
}

/****************************************************************************
 *
 *  Thread A - keyCaptureThread
 *
 ****************************************************************************/
/**
 * \brief Monitors the key input functionality.
 *
 * \note This thread loops infinitely recording and displaying keys that
 * are pressed by a tester/user. 
 *
 */

void keyCaptureThread(void *data)
{
    mpe_GfxEvent event =
    { 0, 0, 0, /*{0} */};
    mpe_Error ec = MPE_SUCCESS;
    mpe_ThreadStat tstat;
    threadLocal *tls = data;

    TRACE(
            MPE_LOG_TRACE5,
            MPE_MOD_TEST,
            "    >>>> entered keyCaptureThread()   tls == %p threadNum == %d'\n",
            tls, tls->tls_threadNum);

    //    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "keyCaptureThread: Hit any key on the front of the STB.\n");
    //    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "keyCaptureThread: Hit more than 10 keystrokes to end the test.\n");

    gKeyCount = 0;
    ec = threadGetStatus(0, &tstat);
    while (!(tstat & MPE_THREAD_STAT_DEATH))
    {
        //        ec = gfxWaitNextEvent(&event, MPE_GFX_WAIT_INFINITE);

        CuAssert(tls->tls_tc, "keyCaptureThread: gfxWaitNextEvent failed", ec
                == MPE_SUCCESS);

        CuAssert(tls->tls_tc,
                "keyCaptureThread: gfxWaitNextEvent returned a bad value", true);
        //             event.eventId != -1);
        ec = threadGetStatus(0, &tstat);
        ++gKeyCount;

        threadSleep(100, 0);
        ec = threadGetStatus(0, &tstat);
    }
    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "    <<<< 'keyCaptureThread()' finished.\n");
}

/****************************************************************************
 *
 *  Thread B - condVarThread
 *
 ****************************************************************************/
/**
 * \brief Manipulates a conditional variable and mutex.
 *
 * \note This thread has the following behavior:
 *      o  Thread starts and blocks on the condition variable 'gCV'
 *      o  When it acquires the CV, it prints a message to the console, then
 *         releases or grabs the mutex depending on the value of the 'm_threadB'
 *          global variable.
 *         If m_threadB is 0xCAFE, then the mutex is grabbed.
 *         If m_threadB is 0xBABE, then the mutex is released.
 */

void condVarThread(void *data)
{
    mpe_Error ec = MPE_SUCCESS;
    mpe_ThreadStat stat;
    threadLocal *tls = data;
    long loopCount = 0;

    TRACE(
            MPE_LOG_TRACE5,
            MPE_MOD_TEST,
            "    >>>> entered condVarThread()      tls == %p threadNum == %d'\n",
            tls, tls->tls_threadNum);

    ec = threadGetStatus(0, &stat);
    while (!(stat & MPE_THREAD_STAT_DEATH))
    {
        /* Block on Condition var. */
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "         'condVarThread() atempting to acquire cond var\n");
        ec = condGet(gCv);
        CuAssert(tls->tls_tc,
                "condVarThread: Acquiring conditional variable failed.", ec
                        == MPE_SUCCESS);

        if (MPE_SUCCESS != ec)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "    condVarThread: Error getting conidition variable.\n");
            threadYield();
            //threadDestroy(0);
        }
        else
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "         'condVarThread() acquired cond var\n");
        }
        switch (m_threadB)
        {
        case THREAD_B_DATA_1:
            /* do nothing */
            break;

        case THREAD_B_DATA_2:
            /* grab mutex */
            ec = mutexAcquire(gMutex);
            CuAssert(tls->tls_tc, "condVarThread: Acquiring mutex failed.", ec
                    == MPE_SUCCESS);

            ec = mutexRelease(gMutex);
            CuAssert(tls->tls_tc, "condVarThread: Releaseing mutex failed.", ec
                    == MPE_SUCCESS);

            /* Generate event */
            ec = eventQueueSend(gEq, EQ_THREAD_C_ID, NULL, NULL, 0);
            CuAssert(tls->tls_tc, "MainThread: Generating event failed.", ec
                    == MPE_SUCCESS);
            break;

        default:
            TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
                    "      condVarThread: Error. Invalid value for threadB "
                        "global data.");
            break;
        }
        loopCount++;
        ec = threadGetStatus(0, &stat);
    }
    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "    <<<< 'condVarThread()' finished, looped %ld times\n",
            loopCount);
}

/****************************************************************************
 *
 *  Thread D - eventQueueThread
 *
 ****************************************************************************/
/**
 * \brief Manipulates an event queue.
 *
 * \note This thread has the following behavior:
 *      o  Thread starts and blocks on the event queue (EQ)
 *      o  When the event occurs, the thread wakes up and posts to the 
 *         CV that threadB is pending on.
 */

void eventQueueThread(void *data)
{
    mpe_Error ec = MPE_SUCCESS;
    mpe_Event eventId = 0;
    void *pEventData = 0;
    mpe_ThreadStat stat;
    threadLocal *tls = data;
    long loopCount = 0;

    TRACE(
            MPE_LOG_TRACE5,
            MPE_MOD_TEST,
            "    >>>> entered eventQueueThread()   tls == %p threadNum == %d'\n",
            tls, tls->tls_threadNum);

    ec = threadGetStatus(0, &stat);
    while (!(stat & MPE_THREAD_STAT_DEATH))
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "         'eventQueueThread' waiting for event\n");
        ec = eventQueueWaitNext(gEq, &eventId, pEventData, NULL, NULL, 0);
        TRACE(
                MPE_LOG_DEBUG,
                MPE_MOD_TEST,
                "         'eventQueueThread' received event, ID == %d, data == %p\n",
                (int) eventId, pEventData);
        CuAssert(tls->tls_tc, "eventQueueThread: Getting next event failed.",
                ec == MPE_SUCCESS);

        CuAssert(tls->tls_tc, "eventQueueThread: Event ID is wrong!", eventId
                == EQ_THREAD_C_ID);

        m_threadB = THREAD_B_DATA_1;

        CuAssert(tls->tls_tc, "eventQueueThread: Setting CV failed.", ec
                == MPE_SUCCESS);
        loopCount++;
        ec = threadGetStatus(0, &stat);
    }
    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "    <<<< 'eventQueueThread()' finished, looped %ld times\n",
            loopCount);
}

/****************************************************************************
 *
 *  'memStressThread()' - Allocate some memory, write to it, check contents,
 *                        free it
 *
 ****************************************************************************/
/**
 * \brief Manipulates memory.
 *
 */
void memStressThread(void *data)
{
    mpe_ThreadStat stat;

    int count = 500;
    mpe_Error ec = MPE_SUCCESS;
    char *pMem = NULL;
    uint32_t size;
    uint32_t memIndex = 0;
    threadLocal *tls = data;
    long loopCount = 0;
    char errMsg[100];

    TRACE(
            MPE_LOG_TRACE5,
            MPE_MOD_TEST,
            "    >>>> entered memStressThread()    tls == %p threadNum == %d'\n",
            tls, tls->tls_threadNum);

    ec = threadGetStatus(0, &stat);
    while (!(stat & MPE_THREAD_STAT_DEATH))
    {
        size = count * 1024;

        /* Allocate some memory */

        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "      allocating %d bytes of memory\n", size);
        ec = memAllocP(MPE_MEM_TEST, size, (void*) &pMem);
        CuAssert(tls->tls_tc, "memStressThread: error allocating memory",
                MPE_SUCCESS == ec);
        threadYield();

        if (MPE_SUCCESS == ec)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "      clearing memory\n");
            memset(pMem, 0, size);

            threadYield();
            /* Write to the memory */
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "      writing memory\n");

            for (memIndex = 0; memIndex < (size); memIndex++)
            {
                *(pMem + memIndex) = (memIndex & 0x000000ff);

                if ((memIndex % 1000) == 0)
                {
                    threadYield();
                }
                memIndex += 500;
            }

            /* Verify memory writes */
            for (memIndex = 0; memIndex < (size); memIndex++)
            {
                if (*(pMem + memIndex) != (memIndex & 0x000000ff))
                {
                    sprintf(errMsg,
                            "Memory mismatch, expected %ld, found %d at %p\n",
                            (memIndex & 0x000000ff), *(pMem + memIndex), (pMem
                                    + memIndex));
                    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, errMsg);
                    CuFail(tls->tls_tc, errMsg);
                }

                if ((memIndex % 10000) == 0)
                {
                    threadYield();
                }

                memIndex += 500;
            }

            threadYield();

            /* Free Memory */
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "      freeing memory\n");
            ec = memFreeP(MPE_MEM_TEST, pMem);
            CuAssert(tls->tls_tc,
                    "memStressThread: Memory(normal size) not freed.", ec
                            == MPE_SUCCESS);

            pMem = NULL;
            threadSleep(100, 0);
        }
        else
        {
            TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
                    "Memory size (%d) not allocated.\n", (size));
            threadSleep(500, 0);
        }

        count += 500;
        if (5000 <= count)
        {
            count = 500;
        }

        /*Yield the processor*/
        threadYield();

        loopCount++;
        ec = threadGetStatus(0, &stat);
    }
    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "    <<<< 'memStressThread()' finished, looped %ld times\n",
            loopCount);
}

/****************************************************************************
 *
 *   getTestSuite_sysStress - Create and return the system stress test suite
 *
 ***************************************************************************/

CuSuite* getTestSuite_sysStress(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, vte_test_sysStress);
    return suite;
}

