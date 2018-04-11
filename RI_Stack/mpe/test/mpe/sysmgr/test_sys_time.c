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

/** \file test_sys_time.c
 *
 *  \brief Test functions for MPEOS time functions
 *
 *  This file contains tests for the following MPEOS functions :\n
 *    -# mpeos_timeGet()\n
 *    -# mpeos_timeGetMillis()\n
 *    -# mpeos_timeToDate()\n
 *    -# mpeos_timeClock()\n
 *    -# mpeos_timeClockTicks()\n
 *    -# mpeos_timeClockToMillis()\n
 *    -# mpeos_timeClockToTime()\n
 *    -# mpeos_timeMillisToClock()\n
 *    -# mpeos_timeSystemClock()\n
 *    -# mpeos_timeTmToTime()\n
 *    -# mpeos_timeTimeToClock()\n
 */

#define _MPE_TIME                    /*!< a #define which appears to be unused */
#define TEST_MPE_TIME                /*!< another #define which appears to be unused */

#ifdef WIN32
#include <time.h>
#endif /* WIN32 */

#include <limits.h>
#include <string.h>
#include <stddef.h>
#include <stdio.h>

#include <test_sys.h>
#include <mpetest_sys.h>

/*  Time test functions  */

static void vte_test_timeGet(CuTest *tc);
static void vte_test_timeGetMillis(CuTest *tc);
static void vte_test_timeToDate(CuTest *tc);
static void vte_test_timeClock(CuTest *tc);
static void vte_test_timeClockTicks(CuTest *tc);
static void vte_test_timeClockToMillis(CuTest *tc);
static void vte_test_timeClockToTime(CuTest *tc);
static void vte_test_timeMillisToClock(CuTest *tc);
static void vte_test_timeSystemClock(CuTest *tc);
static void vte_test_timeTimeToClock(CuTest *tc);
static void vte_test_timeTmToTime(CuTest *tc);

/*  Function to add time tests to a test suite  */

CuSuite* getTestSuite_sysTime(void);

/*  Internal functions  */

static int testTime_EvaluateResult(CuTest*, int, int, int, char*);
static int testTime_OneTimeToDate(CuTest*, mpe_Time, int);
static void testTime_fmtTime(mpe_TimeMillis, char*);
static void testTime_ShowTypeSize(void);
static void testTime_DeadLoop100MSec(void);

CuSuite* getTestSuite_sysTime(void);

#define  DAYSECONDS           86400L   /*!< number of seconds in a day  */
#define  WEEKSECONDS         604800L   /*!< number of seconds in a week  */
#define  YEARSECONDS       31536000L   /*!< number of seconds in a year (non leap year)  */
#define  POSIX_1986       536457599L   /*!< 23:59:59 31 Dec 1986 (from Posix p1003.1-88)  */
#define  TESTTIMEEARLY   1140000000L   /*!< earliest passing time (15 Feb 2006)  */
#define  TESTMSECEARLY 1140000000000L  /*!< earliest passing time in milliseconds */
#define  TESTTIMELATE    1200000000L   /*!< latest passing time (10 Jan 2008)  */
#define  TESTMSECLATE  1200000000000L  /*!< latest passing time in milliseconds */
#define  TICKSPERSECMIN         100L   /*!< minimum legal ticks per second value  */
#define  TICKSPERSECMAX     5000000L   /*!< maximum legal ticks per second value  */

/*
 * Set STARTTIME to earliest time/date which is representable on the system
 * under test. Typical values are :
 *   - int32_t_MIN, which corresponds to 14:45:53 13 Dec 1901 UTC.
 *   - 0, which corresponds to 00:00:00 01 Jan 1970 UTC.
 *   - 820476000, which is the earliest time/date which converts correctly
 *     on PowerTV, and corresponds to 00:00:00 01 Jan 1996
 * Some systems don't like negative 'mpe_Time' values, some systems (such
 * as PowerTV) have odd restrictions, and it's also possible that 'mpe_Time'
 * is an unsigned type, so you may need to change this to make the tests work.
 * if the system has a 64 bit signed 'mpe_Time' type and you want the tests
 * to take forever, you can change this to "int64_t_MIN".
 */

#if defined (POWERTV)
#  define STARTTIME       820476000L   /*!< Starting 'mpe_Time' test value for PowerTV  */
#elif defined (WIN32)
#  define STARTTIME               0L   /*!< Starting 'mpe_Time' test value for Win32 simulator  */
#endif

/*
 * Set ENDTIME to the latest time/date which is representable on the system
 * under test. It's possible that 'mpe_Time' is unsigned, in which case you
 * can change this to "UINT_MAX". If the system has a 64 bit signed 'mpe_Time'
 * and you want the tests to take forever, you can change this to "int64_t_MAX".
 */

#if defined POWERTV
#  define  ENDTIME         LONG_MAX    /*!< Ending 'time_t' test value for PowerTV */
#elif defined WIN32
#  define  ENDTIME         LONG_MAX    /*!< Ending 'mpe_Time' test value for Win32 simulator */
#endif

#if defined WIN32
#  define MSEC2SEC (1000L)
#else
#  define MSEC2SEC (1000LL)
#endif

/*
 * Test step size. This is the interval between time test values, in seconds.
 * A values of 1 means test every second, 60 means test every 60 seconds, etc.
 * Select a value, trading execution time against thoroughness.
 *
 * Suggested values are :
 *
 *  #define  TESTSTEP      1L  * smallest step, tests every sec, takes forever to run *
 *  #define  TESTSTEP     13L  * very small step, tests every 13 sec, runs in < 15 min *
 *  #define  TESTSTEP    113L  * small step, tests every 2 minutes, runs in < 1 min *
 *  #define  TESTSTEP   3593L  * larger step, tests every hour, runs in <5 sec *
 *  #define  TESTSTEP  86399L  * very large step, tests every day, runs in <1 sec *
 */
#define  TESTSTEP    3593L

#define TESTCOUNT  ((ENDTIME-STARTTIME)/TESTSTEP) /* number of time values to test */
#define PRINTSTEP        200000L /* initial status print interval */
#define MAXPRINTLINES       500  /* maximum number of status lines to print */
#define MINPRINTLINES        20  /* minimum number of status lines to print */

#define MSGBUFLEN          150   /* length of status message buffers  */
#define TIMEBUFLEN          24   /* length of formatted time buffers  */

/****************************************************************************
 *
 *  vte_test_timeGet()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_timeGet" function
 *
 * \api mpeos_timeGet()
 *
 * \strategy Call the "mpeos_timeGet()" function and checks for reasonable
 * return values.
 *
 * \assets none
 *
 */

static void vte_test_timeGet(CuTest *tc)
{
    mpe_Time theTime;
    mpe_Error ec;
    mpe_TimeTm local;
    char msgbuf[MSGBUFLEN];

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entering 'vte_test_timeGet()'\n");

    testTime_ShowTypeSize(); /*  print sizes of time types on this platform */

    /**
     * \assertion "MPE_EINVAL" is returned if a NULL pointer is passed
     */

    ec = timeGet(NULL);
    CuAssertIntEquals_Msg(tc, "timeGet(NULL) failed", MPE_EINVAL, ec);

    /**
     * \assertion 'timeGet() does not return an error when passed a valid pointer.
     *  This test also prints broken down time for manual evaluation.
     */

    ec = timeGet(&theTime);
    CuAssertIntEquals_Msg(tc, "timeGet() failed", MPE_SUCCESS, ec);

    timeToDate(theTime, &local);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  timeGet() returned %d - %02d:%02d:%02d %02d/%02d/%04d\n",
            theTime, local.tm_hour, local.tm_min, local.tm_sec, local.tm_mon
                    + 1, local.tm_mday, local.tm_year + 1900);

    /**
     * \assertion 'timeGet() returns a value which represents a time/date which
     *  is not too early and not too late.
     */

    if (theTime < TESTTIMEEARLY)
    {
        sprintf(msgbuf,
                "timeGet(&time) result <%ld> is earlier than expected <%ld>",
                theTime, TESTTIMEEARLY);
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  %s\n", msgbuf);
        CuFail(tc, msgbuf);
    }
    else if (theTime > TESTTIMELATE)
    {
        sprintf(msgbuf,
                "timeGet(&time) result <%ld> is later than expected <%ld>",
                theTime, TESTTIMELATE);
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  %s\n", msgbuf);
        CuFail(tc, msgbuf);
    }
    else
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  timeGet() result looks OK\n");
    }
    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_timeGet finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_timeGetMillis()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_timeGetMillis" function
 *
 * \api mpeos_timeGetMillis()
 *
 * \strategy Call the "mpeos_timeGetMillis()" function and checks for reasonable
 * return values.
 *
 * \assets none
 *
 */

static void vte_test_timeGetMillis(CuTest *tc)
{
    mpe_TimeMillis testMSeconds1;
    mpe_TimeMillis testMSeconds2;
    mpe_Time testSeconds;
    mpe_Time milisToSeconds;
    mpe_Error ec;
    char msgbuf[MSGBUFLEN];
    char cb1[TIMEBUFLEN];
    char cb2[TIMEBUFLEN];

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_timeGetMillis\n");

    //#ifndef WIN32  /* the following crashes the Win32 simulator - Bugzilla 2734  */
    /**
     * \assertion "MPE_EINVAL" is returned if a NULL pointer is passed
     */

    ec = timeGetMillis(NULL);
    CuAssertIntEquals_Msg(tc, "timeGetMillis(NULL) failed", MPE_EINVAL, ec);
    //#else
    //  CuFail(tc, "timeGetMillis(NULL) crashes - Bugzilla 2734");
    //#endif

    /**
     * \assertion 'timeGetMillis()' does not return an error when passed a valid
     *  pointer.
     */

    ec = timeGetMillis(&testMSeconds1);
    CuAssertIntEquals_Msg(tc, "timeGetMillis() failed", MPE_SUCCESS, ec);

    testTime_fmtTime(testMSeconds1, cb1);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  timeGetMillis() returned %s\n", cb1);

    /*  Get time in seconds  */

    ec = timeGet(&testSeconds);
    CuAssertIntEquals_Msg(tc, "timeGet() failed", MPE_SUCCESS, ec);

    /**
     * \assertion The time returned by 'timeGetMillis()' matches the time
     *  returned by 'timeGet()' within one second.
     */

    milisToSeconds = (mpe_Time)(testMSeconds1 / MSEC2SEC); /* convert millis to seconds, with truncation  */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  milisToSeconds' == %ld\n",
            milisToSeconds);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  testSeconds'    == %ld\n",
            testSeconds);

    CuAssert(tc,
            "timeGetMillis() - milliseconds time doesn't match seconds time",
            ((milisToSeconds == testSeconds) || (milisToSeconds == testSeconds
                    - 1)));

    /**
     * \assertion Calling 'timeGetMillis()' a second time returns a value which is
     *  not earlier than the first time.
     */

    ec = timeGetMillis(&testMSeconds2);
    CuAssertIntEquals_Msg(tc, "timeGetMillis(&testMSeconds) failed",
            MPE_SUCCESS, ec);

    testTime_fmtTime(testMSeconds2, cb2);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  timeGetMillis() returned %s\n", cb2);

    CuAssert(tc, "timeGetMillis() - second time is earlier than first",
            (testMSeconds2 >= testMSeconds1));

    CuAssert(tc, "timeGetMillis() - second time is too much later than first",
            (testMSeconds2 <= (testMSeconds1 + 100L)));

    /**
     * \assertion 'timeGetMillis() returns a value which represents a time/date
     *  which is not too early and not too late.
     */

    if (testMSeconds1 < (TESTMSECEARLY))
    {
        testTime_fmtTime((TESTMSECEARLY), cb2);
        sprintf(msgbuf,
                "timeGetMillis() result <%s> is earlier than expected <%s>",
                cb1, cb2);
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  %s\n", msgbuf);
        CuFail(tc, msgbuf);
    }
    else if (testMSeconds1 > (TESTMSECLATE))
    {
        testTime_fmtTime((TESTMSECLATE), cb2);
        sprintf(msgbuf,
                "timeGetMillis() result <%s> is later than expected <%s>", cb1,
                cb2);
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  %s\n", msgbuf);
        CuFail(tc, msgbuf);
    }
    else
    {
        testTime_fmtTime(testMSeconds2, cb2);
        TRACE(
                MPE_LOG_DEBUG,
                MPE_MOD_TEST,
                "  timeGetMillis() results look OK :\n                        %s,\n                        %s\n",
                cb1, cb2);
    }
    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_timeGetMillis finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_timeToDate()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_timeToDate" function
 *
 * \api mpeos_timeToDate()
 *
 * \strategy Call the "mpeos_timeToDate()" function and checks for reasonable
 * return values.
 *
 * \assets none
 *
 */

static void vte_test_timeToDate(CuTest *tc)
{
    mpe_Error ec;
    mpe_Time ttime;
    int errs = 0;
    unsigned long i;
    unsigned long printInterval;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_timeToDate\n");

    /**
     * \assertion "MPE_EINVAL" is returned if a NULL pointer is passed
     */

    ec = timeToDate((mpe_Time) STARTTIME, NULL);
    CuAssertIntEquals_Msg(tc, "timeToDate(time, NULL) failed", MPE_EINVAL, ec);

    /*
     *  Calculate how often to print status message, based on test loop count
     */

    if ((TESTCOUNT / PRINTSTEP) > MAXPRINTLINES)
        printInterval = MAXPRINTLINES;
    else if ((TESTCOUNT / PRINTSTEP) < MINPRINTLINES)
        printInterval = MINPRINTLINES;
    else
        printInterval = TESTCOUNT / PRINTSTEP;
    printInterval = TESTCOUNT / printInterval;

    /**
     * \assertion "MPE_SUCCESS" is returned if valid pointer and mpe_Time
     *  values are passed
     */

    /**
     * \assertion 'timeToDate' returns in-range date and time values for all
     *  valid 'mpe_Time' values.
     */

    for (i = 0, ttime = STARTTIME; i < TESTCOUNT; i++, ttime += TESTSTEP)
    {
        errs += testTime_OneTimeToDate(tc, ttime, (i % printInterval));
        if (errs > 10)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "\n  'vte_test_timeToDate()' - Too many errors, aborting.\n");
            break;
        }
    }
    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_timeToDate finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_timeClock()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_timeClock" function
 *
 * \api mpeos_timeClock()
 *
 * \strategy Call the "mpeos_timeClock()" function and check for reasonable
 * return value. Delay for a while and call again, check that the second call
 * returns a value which is greater than first by the expected amount.
 *
 * \assets none
 *
 */

static void vte_test_timeClock(CuTest *tc)
{
    mpe_TimeClock clk1;
    mpe_TimeClock clk2;
    long actualDelta;
    long minimumDelta;
    char msgbuf[MSGBUFLEN];
    char cb1[TIMEBUFLEN];
    char cb2[TIMEBUFLEN];

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_timeClock\n");

    minimumDelta = (long) (timeClockTicks() / 10); /* number of clock ticks in 100 mSec */

    minimumDelta *= 4; /* expect the test thread to */
    minimumDelta /= 5; /*   get 80% of the CPU time */

    clk1 = timeClock();

    /*
     * \note If the target platform does not contain support for this call,
     *       'mpeos_timeClock()' should return -1.
     *
     */

    if (-1 == clk1)
    {
        TRACE(
                MPE_LOG_DEBUG,
                MPE_MOD_TEST,
                "  timeClock() returned -1. This means that it is not supported on this platform.\n");
    }
    else
    {
        int i;
        for (i = 0; i < 100; i++)
        {
            testTime_DeadLoop100MSec(); /* delay a while  */
        }

        clk2 = timeClock();
        CuAssert(tc, "second call to 'timeClock()' failed, result <= 0", (clk2
                > 0));

        testTime_fmtTime((mpe_TimeMillis) clk1, cb1);
        testTime_fmtTime((mpe_TimeMillis) clk2, cb2);

        actualDelta = (long) (clk2 - clk1);

        /**
         * \assertion After delaying for 10 mSec, "timeClock()" returns a value
         *  which is greater than the previous return value.
         */

        if (0 >= actualDelta)
        {
            sprintf(
                    msgbuf,
                    "timeClock() doesn't appear to be incrementing, first call returned %s, second returned %s",
                    cb1, cb2);
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  %s\n", msgbuf);
            CuFail(tc, msgbuf);
        }

        /**
         * \assertion After delaying for 100 mSec, "timeClock()" returns a value
         *  which is greater than the previous return value by at least the
         *  number of ticks in 10 mSec.
         */
#ifdef POWERTV
        if (actualDelta >= minimumDelta)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "  first call returned  %s\n  second call returned %s\n", cb1, cb2);
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "  delta         == %ld ticks\n  minimum delta == %ld ticks - pass\n", actualDelta, minimumDelta);
        }
        else
        {
            sprintf (msgbuf,
                    "timeClock() didn't return expected value, first call returned %s, second returned %s, delta is %ld, expected delta to be at least %ld",
                    cb1, cb2, actualDelta, minimumDelta);
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  %s\n", msgbuf);
            CuFail(tc, msgbuf);
        }
#endif
    }
    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_timeClock finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_timeClockTicks()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_ClockTicks" function
 *
 * \api mpeos_ClockTicks()
 *
 * \strategy Call the "mpeos_ClockTicks()" function to get the number of
 * clock ticks per second and check for reasonable return values.
 *
 * \assets none
 *
 */

static void vte_test_timeClockTicks(CuTest *tc)
{
    mpe_TimeClock ticksPerSecond;
    char msgbuf[MSGBUFLEN];
    char tpsbuf[TIMEBUFLEN];

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_timeClockTicks\n");

    ticksPerSecond = timeClockTicks();

    /**
     * \assertion 'timeClockTicks()' function returns with no error
     */

    CuAssert(tc, "timeClockTicks() failed", (ticksPerSecond != -1));

    testTime_fmtTime((mpe_TimeMillis) ticksPerSecond, tpsbuf);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  tick rate is %s ticks per second\n",
            tpsbuf);

    /**
     * \assertion Returned ticks-per-second value is within legal range
     */

    if (ticksPerSecond > TICKSPERSECMAX)
    {
        sprintf(
                msgbuf,
                "timeClockTicks() returned value <%s> is larger than expected <less than %ld>",
                tpsbuf, (long) TICKSPERSECMAX);
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  %s\n", msgbuf);
        CuFail(tc, msgbuf);
    }
    else if (ticksPerSecond < TICKSPERSECMIN)
    {
        sprintf(
                msgbuf,
                "timeClockTicks() returned value <%s> is smaller than expected <greater than %ld>",
                tpsbuf, TICKSPERSECMIN);
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  %s\n", msgbuf);
        CuFail(tc, msgbuf);
    }

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_timeClockTicks finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_timeClockToMillis()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_timeClockToMillis" function
 *
 * \api mpeos_timeClockToMillis()
 *
 * \strategy Call the "mpeos_timeClockToMillis()" function with a variety
 *  of different values. In all cases, mpeos_timeClockToMillis(n) should
 *  return ((n*1000)/mpeos_timeClockTicks()), rounded appropriately.
 *
 * \assets none
 *
 */

static void vte_test_timeClockToMillis(CuTest *tc)
{
    mpe_TimeClock testVals[] =
    { 0L, 1L, 2L, 7L, 31L, 99L, 100L, 101L, 511L, 999L, 1000L, 1001L, 2048L,
            8193L, 32769L, 131072L, 524287L, 999999L, 1000000L, 2097153L,
            /*8388608L, */9999999L, 10000000L, 10000001L, /*  33554433L,*/
            134217728L, /* 199999999L, */214748364L, 214748365L, 234881024L,
            299999999L, 300000000L, 300000001L, 335544320L, 399999999L,
            400000000L, 400000001L, 429496729L
#if defined (POWERTV)
            ,536870911L, 999999999L, 1000000000L, 2147483647L, 1000000000L
#endif  /* POWERTV  */
}            ;

            mpe_TimeMillis millis;
            mpe_TimeMillis expected;
            mpe_TimeClock ticksPerSecond;
            int i;
            char msgbuf[MSGBUFLEN];
            char clockbuf[TIMEBUFLEN];
            char millisbuf[TIMEBUFLEN];
            char expectbuf[TIMEBUFLEN];

            TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,"Entered vte_test_timeClockToMillis\n");

	for(i=0; i< 36; i++){
		TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Test Values : %d \n", testVals[i]);
	}

    ticksPerSecond = timeClockTicks();

/**
 *  \assertion 'timeClockToMillis()' correctly converts a variety of different
 *   clock values to milliseconds.
 */

    for (i=0; i < sizeof testVals / sizeof (testVals[0]); i++)
    {
        millis = (mpe_TimeMillis)timeClockToMillis(testVals[i]);
        expected = (testVals[i]* (MSEC2SEC / ticksPerSecond));

        testTime_fmtTime ((mpe_TimeMillis)testVals[i], clockbuf);
        testTime_fmtTime (millis, millisbuf);
        testTime_fmtTime ((mpe_TimeMillis)expected, expectbuf);

        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
              "  clock == %10s, mSec == %8s, expected == %8s - ",
              clockbuf, millisbuf, expectbuf);

        if (expected != millis)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, " FAILED\n");
            sprintf (msgbuf, "  timeClockToMillis() failed - clock == %10s, mSec == %9s, expected == %9s",
                      clockbuf, millisbuf, expectbuf);
            CuFail(tc, msgbuf);
        }
        else
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, " OK\n");
        }
    }

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
          "  vte_test_timeClockToMillis finished.\n\n");
}

    /****************************************************************************
     *
     *  vte_test_timeClockToTime()
     *
     ***************************************************************************/
    /**
     * \testdescription Tests the "mpeos_timeClockToTime" function
     *
     * \api mpeos_timeClockToTime()
     *
     * \strategy Call the "mpeos_timeClockToTime()" function with a variety
     *  of different values. In all cases, mpeos_timeClockToTime(n) should
     *  return (n/mpeos_timeClockTicks()), rounded appropriately.
     *
     * \assets none
     *
     */

static void vte_test_timeClockToTime(CuTest *tc)
{
    /* Test times, in units of clock ticks  */

    mpe_TimeClock testVals[] =
    { 0, 1, 16383, 65535, 1048575, 3375000, 3375001, 4194303, 6750000,
            10000000, 13500000, 27000000, 123456789, 357954528 };

    mpe_Time seconds;
    mpe_Time expected;
    mpe_TimeClock ticksPerSecond;
    int i;
    char clockbuf[TIMEBUFLEN];
    char secsbuf[TIMEBUFLEN];
    char expectbuf[TIMEBUFLEN];

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_timeClockToTime\n");

    ticksPerSecond = timeClockTicks();
    testTime_fmtTime((mpe_TimeMillis) ticksPerSecond, clockbuf);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  Ticks per second == %8s\n", clockbuf);

    /**
     *  \assertion 'timeClockToTime()' correctly converts a variety of different
     *   clock values to seconds.
     */

    for (i = 0; i < sizeof testVals / sizeof(testVals[0]); i++)
    {
        seconds = timeClockToTime(testVals[i]);
        expected = testVals[i] / ticksPerSecond;

        testTime_fmtTime((mpe_TimeMillis) testVals[i], clockbuf);
        testTime_fmtTime((mpe_TimeMillis) seconds, secsbuf);
        testTime_fmtTime((mpe_TimeMillis) expected, expectbuf);
        CuAssert(tc, "timeClockToTime() failed", seconds == expected);

        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  clock == %10s, seconds == %8s, expected == %8s", clockbuf,
                secsbuf, expectbuf);
        if (seconds == expected)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, " OK\n");
        }
        else
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, " # FAILED #\n");
        }
    }

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_timeClockToTime finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_timeMillisToClock()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_timeMillisToClock" function
 *
 * \api mpeos_timeMillisToClock()
 *
 * \strategy Call the "mpeos_timeMillisToClock()" function with a variety
 *  of different values. In all cases, mpeos_timeMillisToClock(n) should
 *  return (n*mpeos_timeClockTicks())/1000, rounded appropriately.
 *
 * \assets none
 *
 */

static void vte_test_timeMillisToClock(CuTest *tc)
{
    uint32_t testVals[] =
    { 0, 1, 16383, 65535, 1048575, 3375000, 3375001, 4194303, 6750000,
            10000000, 13500000, 27000000, 536870911, 999999999, 1073741823,
            2147483647 };
    mpe_TimeClock clk;
    mpe_TimeClock expected;
    mpe_TimeClock ticksPerSecond;
    char clkbuf[TIMEBUFLEN];
    char expectedbuf[TIMEBUFLEN];
    char ticksbuf[TIMEBUFLEN];

    int i;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_timeMillisToClock\n");

    ticksPerSecond = timeClockTicks();
    testTime_fmtTime((mpe_TimeMillis) ticksPerSecond, ticksbuf);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  Tick rate is %s ticks per second\n",
            ticksbuf);

    /**
     *  \assertion 'timeMillisToClock()' correctly converts a variety of different
     *   millisecond time values to clock ticks.
     */

    for (i = 0; i < sizeof testVals / sizeof(testVals[0]); i++)
    {
        clk = timeMillisToClock(testVals[i]);
        testTime_fmtTime((mpe_TimeMillis) clk, clkbuf);

        expected = (testVals[i] * ticksPerSecond) / MSEC2SEC;
        testTime_fmtTime((mpe_TimeMillis) expected, expectedbuf);

        CuAssert(tc, "timeMillisToClock() failed", (clk == expected));

        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  mSec == %12ld, ticks == %14s, expected == %14s",
                testVals[i], clkbuf, expectedbuf);
        if (clk == expected)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, " OK\n");
        }
        else
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, " # FAILED #\n");
        }
    }

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_timeMillisToClock finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_timeSystemClock()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_timeSystemClock" function
 *
 * \api mpeos_timeClock()
 *
 * \strategy Call the "mpeos_timeSystemClock()" function and check for a
 * reasonable return value. Delay for a while and call again, check that the
 * second call returns a value which is greater than first by the expected
 * amount.
 *
 * \assets none
 *
 */

static void vte_test_timeSystemClock(CuTest *tc)
{
    mpe_TimeClock clk1;
    mpe_TimeClock clk2;
    long actualDelta;
    long minimumDelta;
    char msgbuf[MSGBUFLEN];
    char cb1[TIMEBUFLEN];
    char cb2[TIMEBUFLEN];

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_timeSystemClock\n");

    minimumDelta = (long) (timeClockTicks() / 10); /* number of clock ticks in 100 mSec */

    clk1 = timeSystemClock();

    /*
     * \note If the target platform does not contain support for this call,
     *       'mpeos_timeSystemClock()' should return -1.
     *
     */

    if (-1 == clk1)
    {
        TRACE(
                MPE_LOG_DEBUG,
                MPE_MOD_TEST,
                "  timeSystemClock() returned -1. This means that it is not supported on this platform.\n");
    }
    else
    {
        threadSleep(100, 0); /* sleep 100 mSecs  */

        clk2 = timeSystemClock();
        CuAssert(tc, "second call to 'timeClock()' failed, result <= 0", (clk2
                > 0));

        testTime_fmtTime((mpe_TimeMillis) clk1, cb1);
        testTime_fmtTime((mpe_TimeMillis) clk2, cb2);

        actualDelta = (long) (clk2 - clk1);

        /**
         * \assertion After sleeping for 100 mSec, "timeClock()" returns a value
         *  which is greater than the previous return value.
         */

        if (0 >= actualDelta)
        {
            sprintf(
                    msgbuf,
                    "timeSystemClock() doesn't appear to be incrementing, first call returned %s, second returned %s",
                    cb1, cb2);
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  %s\n", msgbuf);
            CuFail(tc, msgbuf);
        }

        /**
         * \assertion After sleeping for 100 mSec, "timeClock()" returns a value
         *  which is greater than the previous return value by at least the
         *  number of ticks in 100 mSec.
         */

        if (actualDelta >= minimumDelta)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "  first call returned  %s, second call returned %s\n",
                    cb1, cb2);
            TRACE(
                    MPE_LOG_DEBUG,
                    MPE_MOD_TEST,
                    "  delta         == %ld ticks, minimum delta == %ld ticks - OK\n",
                    actualDelta, minimumDelta);
        }
        else
        {
            sprintf(
                    msgbuf,
                    "timeClock() didn't return expected value, first call returned %s, second returned %s, delta is %ld, expected delta to be at least %ld",
                    cb1, cb2, actualDelta, minimumDelta);
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  %s\n", msgbuf);
            CuFail(tc, msgbuf);
        }
    }

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_timeSystemClock finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_timeTimeToClock()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_timeTimeToClock" function
 *
 * \api mpeos_timeTimeToClock()
 *
 * \strategy Call the "mpeos_timeTimeToClock()" function with a variety
 *  of different values. In all cases, mpeos_timeTimeToClock(n) should
 *  return (n*mpeos_timeClockTicks()).
 *
 * \assets none
 *
 */

static void vte_test_timeTimeToClock(CuTest *tc)
{
    mpe_Time testVals[] =
    { 0, 1, 16383, 65535, 1048575, 3375000, 3375001, 4194303, 6750000,
            10000000, 13500000, 27000000, 536870911, 1073741823, 2147483647,
            4294967295U };
    mpe_TimeClock clk;
    mpe_TimeClock expected;
    mpe_TimeClock ticksPerSecond;
    int i;
    char clockbuf[TIMEBUFLEN];
    char timebuf[TIMEBUFLEN];
    char expectbuf[TIMEBUFLEN];

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_timeTimeToClock\n");

    ticksPerSecond = timeClockTicks();

    /**
     *  \assertion 'timeTimeToClock()' correctly converts a variety of different
     *   time values to clock ticks.
     */

    for (i = 0; i < sizeof testVals / sizeof(testVals[0]); i++)
    {
        clk = timeTimeToClock(testVals[i]);
        expected = (mpe_TimeClock)(testVals[i] * ticksPerSecond);
        testTime_fmtTime((mpe_TimeMillis) testVals[i], timebuf);
        testTime_fmtTime((mpe_TimeMillis) clk, clockbuf);
        testTime_fmtTime((mpe_TimeMillis) expected, expectbuf);

        CuAssert(tc, "timeTimeToClock() failed", clk == expected);

        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  time == %12s, clock == %16s, expected == %16s", timebuf,
                clockbuf, expectbuf);
        if (clk == expected)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, " OK\n");
        }
        else
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, " # FAILED #\n");
        }
    }

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_timeTimeToClock finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_timeTmToTime()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_timeTmToTime" function
 *
 * \api mpeos_timeTmToTime()
 *
 * \strategy Call the "mpeos_timeTmToTime()" function with a
 *  variety of different values. - - - - - - -
 *
 * \assets none
 *
 */

static void vte_test_timeTmToTime(CuTest *tc)
{
    mpe_Time timeNow;
    mpe_Error ec;
    mpe_TimeTm local;
    /*
     int tz_offsetHrs;
     int tz_offsetMins;
     */
    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_timeTmToTime\n");

    ec = timeGet(&timeNow);
    CuAssert(tc, "timeTmToTime() failed, can't get current time", ec
            == MPE_SUCCESS);

    ec = timeToDate(timeNow, &local);
    CuAssert(tc, "timeTmToTime() failed, can't convert time to date", ec
            == MPE_SUCCESS);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  timeTmToTime(0x%x): %02d:%02d:%02d %02d/%02d/%04d\n", time,
            local.tm_hour, local.tm_min, local.tm_sec, local.tm_mon + 1,
            local.tm_mday, local.tm_year + 1900);

    /**
     *  \assertion 'timeTmToTime()' correctly converts the current broken down
     *   time back to the correct mpe_Time value.
     */

    CuAssertIntEquals_Msg(tc, "timeTmToTime() failed", (int) timeNow,
            (int) timeTmToTime(&local));

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_timeTmToTime finished.\n\n");
}

/***************************************************************************
 *
 * 'testTime_fmtTime()' - based on 'itoa()', K&R second edition, page 64
 *
 *  Converts an 'mpe_TimeClock' or 'mpe_TimeMillis' value to a string
 *  representation. Normally this would be done using 'sprintf()', however
 *  there are a couple of problems with this. First, the underlying type
 *  definition of 'mpe_TimeClock' or 'mpe_TimeMillis' may change, so that
 *  they no longer match the 'sprintf()' format string. This could be dealt
 *  with some ugly casts, however the bigger problem is that on some platforms
 *  'mpe_TimeClock' and/or 'mpe_TimeMillis' are typedef'ed to "long long",
 *  which is not a standard ANSI C type, and not all 'sprintf()'
 *  implementations know how to deal with it, nor do they all use the same
 *  format specifier when they can deal with it.
 *
 *  NOTE : The 'os' pointer passed in must point to a sufficiently large
 *    buffer to hold the maximum length converted string. For a 64 bit type,
 *    this can be as long as 19 digits plus a minus symbol plus the null
 *    terminator, for a total of 21 bytes. A constant, "TIMEBUFLEN" is
 *    defined for setting the buffer size :
 *
 *        char tbuf[TIMEBUFLEN];
 *
 */

static void testTime_fmtTime(mpe_TimeMillis ll, char* os)
{
    char *cp;
    char c;

    if (NULL == os)
    {
        return; /* don't do anything if NULL char ptr */
    }

    if (0 == ll)
    {
        strcpy(os, "0"); /* handle 0 as a special case */
    }
    else
    {
#ifndef WIN32                  /* 'mpe_TimeMillis' is unsigned on Win32 */
        if (ll < 0)
        {
            *os++ = '-'; /* stick in minus symbol  */
            ll = -ll; /* two's complement input  */
        }
#endif /* WIN32 */
        cp = os; /* save pointer to numeric string */

        do /* this builds string in reverse order */
        {
            *os++ = (char) (ll % 10) + '0';
        } while ((ll /= 10) > 0);

        *os-- = '\0'; /* terminate string & point to last digit */

        while (os > cp) /* reverse string */
        {
            c = *cp;
            *cp++ = *os;
            *os-- = c;
        }
    }
}

/****************************************************************************
 *
 *  testTime_OneTimeToDate() - internal function which converts one mpe_Time
 *                             value to an mpe_TimeTm struct and checks the
 *                             results.
 *
 *    Returns : 0 if no error,
 *              1 if conversion fails or a date/time value is out of range.
 *
 ****/

static int testTime_OneTimeToDate(CuTest *tc, mpe_Time testTime, int printFlag)
{
    int result = 0;
    char buf[200];
    mpe_TimeTm local;
    mpe_Time t2;

    if (timeToDate(testTime, &local) != MPE_SUCCESS)
    {
        sprintf(buf, "timeToDate() failed, time == %ld ", (long) testTime);
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  %s\n", buf);
        CuFail(tc, buf);
        result = 1;
    }
    else /* conversion appeared to succeed, now check result */
    {
        result += testTime_EvaluateResult(tc, local.tm_sec, 0, 59, "seconds");
        result += testTime_EvaluateResult(tc, local.tm_min, 0, 59, "minutes");
        result += testTime_EvaluateResult(tc, local.tm_hour, 0, 24, "hours");
        result += testTime_EvaluateResult(tc, local.tm_mday, 1, 31,
                "day of month");
        result += testTime_EvaluateResult(tc, local.tm_mon, 0, 11, "month");
        result += testTime_EvaluateResult(tc, local.tm_year, 1, 255, "year");
        result += testTime_EvaluateResult(tc, local.tm_wday, 0, 6,
                "day of week");
        result += testTime_EvaluateResult(tc, local.tm_yday, 0, 365,
                "day of year");
    }

    /*  Optionally print converted time and date  */

    if (0 == printFlag)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  %02d:%02d:%02d %02d/%02d/%04d %s\n", local.tm_hour,
                local.tm_min, local.tm_sec, local.tm_mon + 1, local.tm_mday,
                local.tm_year + 1900, 1 == local.tm_isdst ? "DST" : "   ");
        threadSleep(1L, 0L);
    }

    /*
     *  Now, call "mpeos_timeTmToTime()" to convert the "mpe_TimeTm" value
     *  back to an mpe_Time" value and compare to the original value passed
     *  and log an error if different.
     */

    t2 = timeTmToTime(&local);
    if ((mpe_Time) - 1 == t2) /* timeTmToTime() returned an error */
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "timeTmToTime() - conversion failed (-1)\n");
        CuFail(tc, "timeTmToTime() - conversion failed");
    }
    if (testTime != t2) /* double converted time doesn't match original value  */
    {
        TRACE(
                MPE_LOG_DEBUG,
                MPE_MOD_TEST,
                "   conv error - %10ld -> %10ld delta = %ld %02d:%02d:%02d %02d/%02d/%04d %d %3d %s # FAILED #\n",
                (long) testTime, (long) t2, (long) t2 - testTime,
                local.tm_hour, local.tm_min, local.tm_sec, local.tm_mon + 1,
                local.tm_mday, local.tm_year + 1900, local.tm_wday,
                local.tm_yday, 1 == local.tm_isdst ? "DST" : "   ");
        sprintf(buf, "Conversion error - expected %ld, got %ld, delta = %ld",
                (long) testTime, (long) t2, (long) t2 - testTime);
        CuFail(tc, buf);
    }

    if (0 == result)
    {
        return (0);
    }
    else
    {
        return (1);
    }
}

/****************************************************************************
 *
 *  testTime_EvaluateResult() - internal function which performs a range check
 *                              and logs a CuTest error if value is out of range
 *
 *    Returns : 0 if no error,
 *              1 if a date/time value is out of range.
 *
 ****/

static int testTime_EvaluateResult(CuTest *tc, int test, int min, int max,
        char *msg)
{
    char buf[200];

    if ((test < min) || (test > max))
    {
        sprintf(buf, "'vte_test_timeToDate()' - %s out of range : %d\n", msg,
                test);
        CuFail(tc, buf);
        return (1);
    }
    else
    {
        return (0);
    }
}

/****************************************************************************
 *
 * 'testTime_ShowTypeSize()' -
 *
 *    Internal function which prints the sizes of the ANSI time types
 *    ('clock_t', 'time_t', etc.), and the MPE equivalent types
 *    ('mpe_TimeClock', 'mpe_Time', etc).
 */

void testTime_ShowTypeSize(void)
{

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  Time type sizes :\n");

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "   'clock_t'         size == %d bytes\n", (int) (sizeof(clock_t)));

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "   'time_t'          size == %d bytes\n", (int) (sizeof(time_t)));

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "   'struct tm'       size == %d bytes\n",
            (int) (sizeof(struct tm)));

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "   'struct timeval'  size == %d bytes\n",
            (int) (sizeof(struct timeval)));

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "   'mpe_TimeClock'   size == %d bytes\n",
            (int) (sizeof(mpe_TimeClock)));

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "   'mpe_Time'        size == %d bytes\n", (int) (sizeof(mpe_Time)));

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "   'mpe_TimeTm'      size == %d bytes\n",
            (int) (sizeof(mpe_TimeTm)));

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "   'mpe_TimeVal'     size == %d bytes\n",
            (int) (sizeof(mpe_TimeVal)));

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "   'mpe_TimeMillis'  size == %d bytes\n",
            (int) (sizeof(mpe_TimeMillis)));
    return;
}

/****************************************************************************
 *
 *  testTime_DeadLoop100MSec()
 *
 *      Internal function which wastes some time, in order to test the
 *      "mpeos_timeClock()" function. It does this by repeatedly calling
 *      "timeGetMillis()" in a loop until 100 mSec has elapsed, or for 100,000
 *      loop iterations, which ever comes first.
 *
 *    Returns : nuthin'
 *
 ****/

static void testTime_DeadLoop100MSec(void)
{
    mpe_TimeMillis now;
    mpe_TimeMillis end;
    char cb1[TIMEBUFLEN];
    char cb2[TIMEBUFLEN];

    uint32_t i;

    if (MPE_SUCCESS != timeGetMillis(&end))
    {
        return;
    }

    testTime_fmtTime(end, cb1);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    started at  %s\n", cb1);

    end += 100; /*  want to loop for 100 mSec, or for 100,000 loop iterations  */

    for (i = 0; i < 100000; i++)
    {
        if (MPE_SUCCESS != timeGetMillis(&now))
        {
            return; /*  bail out on error  */
        }
        if (now >= end)
        {
            break;
        }
    }

    testTime_fmtTime(now, cb2);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    finished at %s, looped %d times\n",
            cb2, i);
}

/****************************************************************************
 *
 *  getTestSuite_sysTime
 *
 * Create and return the test suite for the time APIs.
 *
 */

CuSuite* getTestSuite_sysTime(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, vte_test_timeGet);
    SUITE_ADD_TEST(suite, vte_test_timeGetMillis);
    SUITE_ADD_TEST(suite, vte_test_timeToDate);
    SUITE_ADD_TEST(suite, vte_test_timeClock);
    SUITE_ADD_TEST(suite, vte_test_timeClockTicks);
    SUITE_ADD_TEST(suite, vte_test_timeClockToMillis);
    SUITE_ADD_TEST(suite, vte_test_timeClockToTime);
    SUITE_ADD_TEST(suite, vte_test_timeMillisToClock);
    SUITE_ADD_TEST(suite, vte_test_timeSystemClock);
    SUITE_ADD_TEST(suite, vte_test_timeTmToTime);
    SUITE_ADD_TEST(suite, vte_test_timeTimeToClock);

    return suite;
}
