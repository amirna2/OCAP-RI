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

#include <mpeTest.h>
#include <test_sys.h>

#include <stdio.h>
#include <mpe_sys.h>

// What are these defines for??????????
/* #define TEST_SYS_DLL  */
/* #define TEST_SYS_MPEOS */

/* Extern functions that are used to build the test suite.
 */
CuSuite* getTestSuite_sysMem(void);
CuSuite* getTestSuite_sysEvent(void);
CuSuite* getTestSuite_gfxEvent(void);
CuSuite* getTestSuite_sysSync(void);
CuSuite* getTestSuite_sysThread(void);
CuSuite* getTestSuite_sysTime(void);
#ifdef TEST_SYS_DLL
CuSuite* getTestSuite_sysDll(void);
#endif /* TEST_SYS_DLL */
#ifdef TEST_SYS_MPEOS
CuSuite* getTestSuite_sysUtil(void);
#endif /* TEST_SYS_MPEOS */
CuSuite* getTestSuite_sysDbg(void);
CuSuite* getTestSuite_sysMath(void);
CuSuite* getTestSuite_sysStress(void);

/****************************************************************************

 'test_sysRunAllTests()' - Runs all the system tests.

 ****/

NATIVEEXPORT_API void test_sysRunAllTests(void)
{
    CuString* output;
    CuSuite* suiteToRun;
    CuSuite* suiteToAdd;

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "\n############\n#\n#   'test_sysRunAllTests()' starting\n#\n");

    suiteToRun = CuSuiteNew();

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "\n    'test_sysRunAllTests()' - adding tests\n");

    /*  add memory tests  */

    suiteToAdd = getTestSuite_sysMem();
    CuSuiteAddSuite(suiteToRun, suiteToAdd);
    CU_FREE(suiteToAdd);

    /*  add time tests  */

    suiteToAdd = getTestSuite_sysTime();
    CuSuiteAddSuite(suiteToRun, suiteToAdd);
    CU_FREE(suiteToAdd);

    /*  add thread tests  */

    suiteToAdd = getTestSuite_sysThread();
    CuSuiteAddSuite(suiteToRun, suiteToAdd);
    CU_FREE(suiteToAdd);

    /*  add sync tests  */

    suiteToAdd = getTestSuite_sysSync();
    CuSuiteAddSuite(suiteToRun, suiteToAdd);
    CU_FREE(suiteToAdd);

    /*  add event tests  */

    suiteToAdd = getTestSuite_sysEvent();
    CuSuiteAddSuite(suiteToRun, suiteToAdd);
    CU_FREE(suiteToAdd);

#ifdef TEST_SYS_MPEOS

    /*  add util tests  */

    suiteToAdd = getTestSuite_sysUtil();
    CuSuiteAddSuite(suiteToRun, suiteToAdd);
    CU_FREE(suiteToAdd);

#endif /* TEST_SYS_MPEOS  */

#ifdef TEST_SYS_DLL

    /*  add dll tests  */

    suiteToAdd = getTestSuite_sysDll();
    CuSuiteAddSuite(suiteToRun, suiteToAdd);
    CU_FREE(suiteToAdd);

#endif /* TEST_SYS_DLL */

    /*  add dbg tests  */

    suiteToAdd = getTestSuite_sysDbg();
    //    CuSuiteAddSuite(suiteToRun, suiteToAdd);
    CU_FREE(suiteToAdd);

    /*  add math tests  */

    suiteToAdd = getTestSuite_sysMath();
    CuSuiteAddSuite(suiteToRun, suiteToAdd);
    CU_FREE(suiteToAdd);

    /*  add system stress tests  */

    suiteToAdd = getTestSuite_sysStress();
    CuSuiteAddSuite(suiteToRun, suiteToAdd);
    CU_FREE(suiteToAdd);

    /****
     *
     *  Run the tests
     *
     */

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "\n'test_sysRunAllTests()' - Running tests . . .\n");

    CuSuiteRun(suiteToRun);

    /****
     *
     *  Report results
     *
     */

    output = CuStringNew();
    CuSuiteSummary(suiteToRun, output);
    CuSuiteDetails(suiteToRun, output);

    /*    PowerTV has a (rather low) limit on length of log messages, so until
     that gets fixed, we just do 'printf()' to print test results

     TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
     "\ntest_sysRunAllTests() results :\n%s\n", output->buffer );
     */
    fflush( stdout);
    fflush( stderr);
    printf("\ntest_sysRunAllTests() results :\n%s\n", output->buffer);
    fflush(stdout);
    fflush(stderr);
    /*  End of hack for PowerTV limit on log message length  */

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "\ntest_sysRunAllTests() - freeing suite\n");
    CuSuiteFree(suiteToRun);

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "\ntest_sysRunAllTests() - freeing string\n");
    CuStringFree(output);

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "\n#\n#'test_sysRunAllTests()' - Tests complete\n#\n");
}

/****************************************************************************
 *
 *  test_sysRunTimeTests()
 *
 *    Runs all the time tests from "test_sys_time.c"
 *
 */

NATIVEEXPORT_API void test_sysRunTimeTests(void)
{
    CuSuite* suite;
    CuString* output;

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "\n#  'test_sysRunTimeTests()' starting\n");

    suite = CuSuiteNew();

    CuSuiteAddSuite(suite, getTestSuite_sysTime());
    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunTimeTests() results :\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunMemTests()
 *
 *    Runs all the memory tests from "test_sys_mem.c"
 *
 */

NATIVEEXPORT_API void test_sysRunMemTests(void)
{
    CuSuite* suite;
    CuString* output;

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "\n#  'test_sysRunMemTests()' starting\n");

    suite = CuSuiteNew();

    CuSuiteAddSuite(suite, getTestSuite_sysMem());
    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunMemTests() results :\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunThreadTests()
 *
 *    Runs all the thread tests from "test_sys_thread.c"
 *
 */

NATIVEEXPORT_API void test_sysRunThreadTests(void)
{
    CuSuite* suite;
    CuString* output;

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "\n#  'test_sysRunThreadTests()' starting\n");

    suite = CuSuiteNew();

    CuSuiteAddSuite(suite, getTestSuite_sysThread());
    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunThreadTests() results :\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunSyncTests()
 *
 *    Runs all the sync tests from "test_sys_sync.c"
 *
 */

NATIVEEXPORT_API void test_sysRunSyncTests(void)
{
    CuSuite* suite;
    CuString* output;

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "\n#  'test_sysRunSyncTests()' starting\n");

    suite = CuSuiteNew();

    CuSuiteAddSuite(suite, getTestSuite_sysSync());
    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunSyncTests() results :\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunEventTests()
 *
 *    Runs all the event tests from "test_sys_event.c"
 *
 */

NATIVEEXPORT_API void test_sysRunEventTests(void)
{
    CuSuite* suite;
    CuString* output;

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "\n#  'test_sysRunEventTests()' starting\n");

    suite = CuSuiteNew();

    CuSuiteAddSuite(suite, getTestSuite_sysEvent());
    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunEventTests() results :\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunUtilTests()
 *
 *    Runs all the utility tests from "test_sys_util.c"
 *
 *  NOTE : This can only run in MPEOS, there is no MPE equivelent.
 */

NATIVEEXPORT_API void test_sysRunUtilTests(void)
{
#ifdef TEST_SYS_MPEOS
    CuSuite* suite;
    CuString* output;

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST, "\n#  'test_sysRunUtilTests()' starting\n");

    suite = CuSuiteNew();

    CuSuiteAddSuite(suite, getTestSuite_sysUtil());
    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\ntest_sysRunUtilTests() results :\n%s\n", output->buffer );

    CuSuiteFree(suite);
    CuStringFree(output);
#else
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n\n#\n####  'test_sysRunUtilTests()' did not run\n#\n\n");
#endif /* TEST_SYS_MPEOS */
}

/****************************************************************************
 *
 *  test_sysRunDllTests()
 *
 *    Runs all the DLL tests from "test_sys_dll.c"
 *
 */

NATIVEEXPORT_API void test_sysRunDllTests(void)
{
#ifdef TEST_SYS_DLL
    CuSuite* suite;
    CuString* output;

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST, "\n#  'test_sysRunDllTests()' starting\n");

    suite = CuSuiteNew();

    //    CuSuiteAddSuite(suite, getTestSuite_sysDll());
    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\ntest_sysRunDllTests() results :\n%s\n", output->buffer );

    CuSuiteFree(suite);
    CuStringFree(output);
#else
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n\n#\n####  'test_sysRunDllTests()' did not run\n#\n\n");
#endif
}

/****************************************************************************
 *
 *  test_sysRunDbgTests()
 *
 *    Runs all the dbg tests from "test_sys_dbg.c"
 *
 */

NATIVEEXPORT_API void test_sysRunDbgTests(void)
{
    CuSuite* suite;
    CuString* output;

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "\n#  'test_sysRunDbgTests()' starting\n");

    suite = CuSuiteNew();

    CuSuiteAddSuite(suite, getTestSuite_sysDbg());
    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunDbgTests() results :\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunStressTests()
 *
 *    Runs the system stress tests from "test_sys_stress"
 *
 */

NATIVEEXPORT_API void test_sysRunStressTests(void)
{
    CuSuite* suite;
    CuString* output;

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "\n#  'test_sysRunStressTests()' starting\n");

    suite = CuSuiteNew();

    CuSuiteAddSuite(suite, getTestSuite_sysStress());
    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunStressTests() results :\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunMathTests()
 *
 *    Runs the math tests from "test_sys_math.c"
 *
 */

NATIVEEXPORT_API void test_sysRunMathTests(void)
{
    CuSuite* suite;
    CuString* output;

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "\n#  'test_sysRunMathTests()' starting\n");

    suite = CuSuiteNew();

    CuSuiteAddSuite(suite, getTestSuite_sysMath());
    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunMathTests() results :\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuStringFree(output);
}

/****************************************************************************

 Runners for individual Time tests

 **/

NATIVEEXPORT_API void test_sysRunTimeGetTest()
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysTime();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_timeGet");
    CuSuiteFree(timeSuite);

    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunTimeGetTest() - Can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunTimeGetTest() results :\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuStringFree(output);
}

NATIVEEXPORT_API void test_sysRunTimeGetMillisTest()
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysTime();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_timeGetMillis");
    CuSuiteFree(timeSuite);

    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunTimeGetMillisTest() - Can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunTimeGetMillisTest() results :\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuStringFree(output);
}

NATIVEEXPORT_API void test_sysRunTimeToDateTest()
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysTime();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_timeToDate");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunTimeToDateTest() - Can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunTimeToDateTest() results :\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}

NATIVEEXPORT_API void test_sysRunTimeClockTest()
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysTime();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_timeClock");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunTimeClockTest() - Can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunTimeClockTest() results: \n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}

void test_sysRunTimeClockTicksTest()
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysTime();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_timeClockTicks");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunTimeClockTicksTest() - Can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunTimeClockTicks() results :\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}

NATIVEEXPORT_API void test_sysRunTimeClockToMillisTest()
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysTime();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_timeClockToMillis");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunTimeClockToMillisTest() - Can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunTimeClockToMillis() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}

NATIVEEXPORT_API void test_sysRunTimeClockToTimeTest()
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysTime();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_timeClockToTime");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunTimeClockToTimeTest() - Can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "test_sysRunTimeClockToTime() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}

NATIVEEXPORT_API void test_sysRunTimeMillisToClockTest()
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysTime();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_timeMillisToClock");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunTimeMillisToClockTest() - can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "test_sysRunTimeMillisToClockTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}

NATIVEEXPORT_API void test_sysRunTimeSystemClockTest()
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysTime();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_timeSystemClock");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunTimeSystemClock() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunTimeSystemClock() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}

NATIVEEXPORT_API void test_sysRunTimeTimeToClockTest()
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysTime();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_timeTimeToClock");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunTimeTimeToClock() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunTimeTimeToClock() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}

void test_sysRunTimeTmToTimeTest()
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysTime();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_timeTmToTime");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunTimeTmToTimeTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunTimeTmToTimeTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}

/************************************************************************
 ************************************************************************

 Runners for individual Event tests

 ****/

NATIVEEXPORT_API void test_sysRunEventQueueNewTest(void)
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "Entered 'test_sysRunEventQueueNewTest()'\n");

    timeSuite = getTestSuite_sysEvent();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_eventQueueNew");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunEventQueueNewTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunEventQueueNewTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}

NATIVEEXPORT_API void test_sysRunEventQueueSendTest(void)
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysEvent();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_eventQueueSend");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunEventQueueSendTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunEventQueueSendTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}

NATIVEEXPORT_API void test_sysRunEventSendThreadTest(void)
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysEvent();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_eventSendThread");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunEventSendThreadTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunEventSendThreadTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}

NATIVEEXPORT_API void test_sysRunEventPingPongTest(void)
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysEvent();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_eventPingPong");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunEventPingPongTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunEventPingPongTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}

NATIVEEXPORT_API void test_sysRunEventQueueWaitNextTest(void)
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysEvent();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_eventQueueWaitNext");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunEventQueueWaitNextTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunEventQueueWaitNextTest() results:\n%s\n",
            output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}

NATIVEEXPORT_API void test_sysRunEventQueueDeleteTest(void)
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysEvent();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_eventQueueDelete");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunEventQueueDeleteTest()() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunEventQueueDeleteTest() results:\n%s\n",
            output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}

NATIVEEXPORT_API void test_sysRunEventQueueNextTest(void)
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysEvent();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_eventQueueNext");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunEventQueueNextTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunEventQueueNextTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}

/************************************************************************
 ************************************************************************

 Runners for individual Memory tests

 ****/

/****************************************************************************
 *
 *  test_sysRunMemAllocPTest()
 *
 *    Runs 'test_memAllocP' from "test_sys_mem.c"
 *
 */

NATIVEEXPORT_API void test_sysRunMemAllocPTest(void)
{
    CuSuite* suite;
    CuSuite* memSuite;
    CuString* output;

    memSuite = getTestSuite_sysMem();
    suite = CuSuiteNewCloneTest(memSuite, "vte_test_memAllocP");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunMemAllocPTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunMemAllocPTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(memSuite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunMemFreePTest()
 *
 *    Runs 'vte_test_memFreeP' from "test_sys_mem.c"
 *
 */

NATIVEEXPORT_API void test_sysRunMemFreePTest(void)
{
    CuSuite* suite;
    CuSuite* memSuite;
    CuString* output;

    memSuite = getTestSuite_sysMem();
    suite = CuSuiteNewCloneTest(memSuite, "vte_test_memFreeP");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunMemFreePTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunMemFreePTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(memSuite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunMemReallocPTest()
 *
 *    Runs 'vte_test_memReallocP' from "test_sys_mem.c"
 *
 */

NATIVEEXPORT_API void test_sysRunMemReallocPTest(void)
{
    CuSuite* suite;
    CuSuite* memSuite;
    CuString* output;

    memSuite = getTestSuite_sysMem();
    suite = CuSuiteNewCloneTest(memSuite, "vte_test_memReallocP");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunMemReallocPTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunMemReallocPTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(memSuite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunMemAllocHTest()
 *
 *    Runs 'vte_test_memAllocH()' from "test_sys_mem.c"
 *
 */

NATIVEEXPORT_API void test_sysRunMemAllocHTest(void)
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysMem();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_memAllocH");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunMemAllocHTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunMemAllocHTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunMemFreeHTest()
 *
 *    Runs 'vte_test_memFreeH' from "test_sys_mem.c"
 *
 */

NATIVEEXPORT_API void test_sysRunMemFreeHTest(void)
{
    CuSuite* suite;
    CuSuite* memSuite;
    CuString* output;

    memSuite = getTestSuite_sysMem();
    suite = CuSuiteNewCloneTest(memSuite, "vte_test_memFreeH");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunMemFreeHTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunMemFreeHTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(memSuite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunMemLockHTest()
 *
 *    Runs the 'test_memLockH()' test from "test_sys_mem.c"
 *
 */

NATIVEEXPORT_API void test_sysRunMemLockHTest(void)
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysMem();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_memLockH");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunMemLockHTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunMemLockHTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunMemReallocH()
 *
 *    Runs the 'test_memReallocH()' test from "test_sys_mem.c"
 *
 */

NATIVEEXPORT_API void test_sysRunMemReallocHTest(void)
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysMem();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_memReallocH");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunMemReallocHTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunMemReallocHTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunMemCompactTest()
 *
 *    Runs 'vte_test_memCompact' from "test_sys_mem.c"
 *
 */

NATIVEEXPORT_API void test_sysRunMemCompactTest(void)
{
    CuSuite* suite;
    CuSuite* memSuite;
    CuString* output;

    memSuite = getTestSuite_sysMem();
    suite = CuSuiteNewCloneTest(memSuite, "vte_test_memCompact");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunMemCompactTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunMemCompactTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(memSuite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunMemGetFreeSizeTest()
 *
 *    Runs 'vte_test_memGetFreeSize' from "test_sys_mem.c"
 *
 */

NATIVEEXPORT_API void test_sysRunMemGetFreeSizeTest(void)
{
    CuSuite* suite;
    CuSuite* memSuite;
    CuString* output;

    memSuite = getTestSuite_sysMem();
    suite = CuSuiteNewCloneTest(memSuite, "vte_test_memGetFreeSize");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunMemGetFreeSizeTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunMemGetFreeSizeTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(memSuite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunMemGetLargestFreeTest()
 *
 *    Runs the 'test_memGetLargestFree()' test from "test_sys_mem.c"
 *
 */

NATIVEEXPORT_API void test_sysRunMemGetLargestFreeTest(void)
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysMem();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_memGetLargestFree");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunMemGetLargestFreeTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunMemGetLargestFreeTest() results:\n%s\n",
            output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunMemGetStatsTest()
 *
 *    Runs the 'vte_test_memGetStats' test from "test_sys_mem.c"
 *
 */

NATIVEEXPORT_API void test_sysRunMemGetStatsTest(void)
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysMem();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_memGetStats");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunMemGetStatsTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunMemGetStatsTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}
/****************************************************************************
 *
 *  test_sysRunMemPurgeTest()
 *
 *    Runs the 'vte_test_memPurge' test from "test_sys_mem.c"
 *
 */

NATIVEEXPORT_API void test_sysRunMemPurgeTest(void)
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysMem();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_memPurge");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunMemPurge() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\ntest_sysRunMemPurge() results:\n%s\n",
            output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunMemStatsTest()
 *
 *    Runs the 'vte_test_memStats()' test from "test_sys_mem.c"
 *
 */

NATIVEEXPORT_API void test_sysRunMemStatsTest(void)
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysMem();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_memStats");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunMemStatsTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunMemStatsTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}

/************************************************************************
 ************************************************************************

 Runners for individual Thread tests

 ****/

/****************************************************************************
 *
 *  test_sysRunThreadAttachTest()
 *
 *    Runs the 'vte_test_threadAttach()' test from "test_sys_thread.c"
 *
 */

NATIVEEXPORT_API void test_sysRunThreadAttachTest(void)
{
    CuSuite* suite;
    CuSuite* tempSuite;
    CuString* output;

    tempSuite = getTestSuite_sysThread();
    suite = CuSuiteNewCloneTest(tempSuite, "vte_test_threadAttach");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunThreadAttachTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunThreadAttachTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(tempSuite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunThreadCreateTest()
 *
 *    Runs the 'vte_test_threadCreate()' test from "test_sys_thread.c"
 *
 */

NATIVEEXPORT_API void test_sysRunThreadCreateTest(void)
{
    CuSuite* suite;
    CuSuite* tempSuite;
    CuString* output;

    tempSuite = getTestSuite_sysThread();
    suite = CuSuiteNewCloneTest(tempSuite, "vte_test_threadCreate");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunThreadCreateTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunThreadCreateTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(tempSuite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunThreadDestroyID0Test()
 *
 *    Runs the 'vte_test_threadDestroyID0()' test from "test_sys_thread.c"
 *
 */

NATIVEEXPORT_API void test_sysRunThreadDestroyID0Test(void)
{
    CuSuite* suite;
    CuSuite* tempSuite;
    CuString* output;

    tempSuite = getTestSuite_sysThread();
    suite = CuSuiteNewCloneTest(tempSuite, "vte_test_threadDestroyID0");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunThreadDestroyID0Test() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunThreadDestroyID0Test() results:\n%s\n",
            output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(tempSuite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunThreadDestroyIDTest()
 *
 *    Runs the 'vte_test_threadDestroyID()' test from "test_sys_thread.c"
 *
 */

NATIVEEXPORT_API void test_sysRunThreadDestroyIDTest(void)
{
    CuSuite* suite;
    CuSuite* tempSuite;
    CuString* output;

    tempSuite = getTestSuite_sysThread();
    suite = CuSuiteNewCloneTest(tempSuite, "vte_test_threadDestroyID");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunThreadDestroyIDTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunThreadDestroyIDTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(tempSuite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunThreadDestroyOtherTest()
 *
 *    Runs the 'vte_test_threadDestroyOther()' test from "test_sys_thread.c"
 *
 */

NATIVEEXPORT_API void test_sysRunThreadDestroyOtherTest(void)
{
    CuSuite* suite;
    CuSuite* tempSuite;
    CuString* output;

    tempSuite = getTestSuite_sysThread();
    suite = CuSuiteNewCloneTest(tempSuite, "vte_test_threadDestroyOther");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunThreadDestroyOtherTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunThreadDestroyOtherTest() results:\n%s\n",
            output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(tempSuite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunThreadGetCurrentTest()
 *
 *    Runs the 'vte_test_threadGetCurrent()' test from "test_sys_thread.c"
 *
 */

NATIVEEXPORT_API void test_sysRunThreadGetCurrentTest(void)
{
    CuSuite* suite;
    CuSuite* tempSuite;
    CuString* output;

    tempSuite = getTestSuite_sysThread();
    suite = CuSuiteNewCloneTest(tempSuite, "vte_test_threadGetCurrent");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunThreadGetCurrentTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunThreadGetCurrentTest() results:\n%s\n",
            output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(tempSuite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunThreadGetDataTest()
 *
 *    Runs the 'vte_test_threadGetData()' test from "test_sys_thread.c"
 *
 *    NOTE : This test needs to run before "test_sysRunThreadDataTest()
 *
 */

NATIVEEXPORT_API void test_sysRunThreadGetDataTest(void)
{
    CuSuite* suite;
    CuSuite* tempSuite;
    CuString* output;

    tempSuite = getTestSuite_sysThread();
    suite = CuSuiteNewCloneTest(tempSuite, "vte_test_threadGetData");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunThreadGetDataTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunThreadGetDataTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(tempSuite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunThreadGetStatusTest()
 *
 *    Runs the 'vte_test_threadGetStatus()' test from "test_sys_thread.c"
 *
 */

NATIVEEXPORT_API void test_sysRunThreadGetStatusTest(void)
{
    CuSuite* suite;
    CuSuite* tempSuite;
    CuString* output;

    tempSuite = getTestSuite_sysThread();
    suite = CuSuiteNewCloneTest(tempSuite, "vte_test_threadGetStatus");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunThreadGetStatusTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunThreadGetStatusTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(tempSuite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunThreadSetDataTest()
 *
 *    Runs the 'vte_test_threadSetData()' test from "test_sys_thread.c"
 *
 */

NATIVEEXPORT_API void test_sysRunThreadSetDataTest(void)
{
    CuSuite* suite;
    CuSuite* tempSuite;
    CuString* output;

    tempSuite = getTestSuite_sysThread();
    suite = CuSuiteNewCloneTest(tempSuite, "vte_test_threadSetData");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunThreadSetDataTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunThreadSetDataTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(tempSuite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunThreadSetPriorityTest()
 *
 *    Runs the 'vte_test_threadSetPriority()' test from "test_sys_thread.c"
 *
 */

NATIVEEXPORT_API void test_sysRunThreadSetPriorityTest(void)
{
    CuSuite* suite;
    CuSuite* tempSuite;
    CuString* output;

    tempSuite = getTestSuite_sysThread();
    suite = CuSuiteNewCloneTest(tempSuite, "vte_test_threadSetPriority");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunThreadSetPriorityTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunThreadSetPriorityTest() results:\n%s\n",
            output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(tempSuite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunThreadSetStatusTest()
 *
 *    Runs the 'vte_test_threadSetStatus()' test from "test_sys_thread.c"
 *
 */

NATIVEEXPORT_API void test_sysRunThreadSetStatusTest(void)
{
    CuSuite* suite;
    CuSuite* tempSuite;
    CuString* output;

    tempSuite = getTestSuite_sysThread();
    suite = CuSuiteNewCloneTest(tempSuite, "vte_test_threadSetStatus");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunThreadSetStatusTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunThreadSetStatusTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(tempSuite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunThreadSleepTest()
 *
 *    Runs the 'vte_test_threadSleep()' test from "test_sys_thread.c"
 *
 */

NATIVEEXPORT_API void test_sysRunThreadSleepTest(void)
{
    CuSuite* suite;
    CuSuite* tempSuite;
    CuString* output;

    tempSuite = getTestSuite_sysThread();
    suite = CuSuiteNewCloneTest(tempSuite, "vte_test_threadSleep");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunThreadSleepTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunThreadSleepTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(tempSuite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunThreadYieldTest()
 *
 *    Runs the 'vte_test_threadYield()' test from "test_sys_thread.c"
 *
 */

NATIVEEXPORT_API void test_sysRunThreadYieldTest(void)
{
    CuSuite* suite;
    CuSuite* tempSuite;
    CuString* output;

    tempSuite = getTestSuite_sysThread();
    suite = CuSuiteNewCloneTest(tempSuite, "vte_test_threadYield");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunThreadYieldTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunThreadYieldTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(tempSuite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunThreadDataTest()
 *
 *    Runs the 'vte_test_threadData()' test from "test_sys_thread.c"
 *
 *  NOTE : This test must not run before "vte_test_threadGetData()"
 *
 */

NATIVEEXPORT_API void test_sysRunThreadDataTest(void)
{
    CuSuite* suite;
    CuSuite* tempSuite;
    CuString* output;

    tempSuite = getTestSuite_sysThread();
    suite = CuSuiteNewCloneTest(tempSuite, "vte_test_threadData");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunThreadDataTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunThreadDataTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(tempSuite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunThreadStatusTest()
 *
 *    Runs the 'vte_test_threadStatus()' test from "test_sys_thread.c"
 *
 */

NATIVEEXPORT_API void test_sysRunThreadStatusTest(void)
{
    CuSuite* suite;
    CuSuite* tempSuite;
    CuString* output;

    tempSuite = getTestSuite_sysThread();
    suite = CuSuiteNewCloneTest(tempSuite, "vte_test_threadStatus");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunThreadStatusTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunThreadStatusTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(tempSuite);
    CuStringFree(output);
}

/************************************************************************
 ************************************************************************

 Runners for individual Sync tests

 ****/

NATIVEEXPORT_API void test_sysRunSyncMutexNewTest()
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysSync();
    suite = CuSuiteNewCloneTest(timeSuite, "test_mutexNew");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunSyncMutexNewTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunSyncMutexNewTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}

NATIVEEXPORT_API void test_sysRunSyncMutexAcquireTest()
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysSync();
    suite = CuSuiteNewCloneTest(timeSuite, "test_mutexAcquire");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunSyncMutexAcquireTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunSyncMutexAcquireTest() results:\n%s\n",
            output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}

NATIVEEXPORT_API void test_sysRunSyncMutexAcquierTryTest()
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysSync();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_mutexAcquireTry");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunSyncMutexAcquierTryTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunSyncMutexAcquierTryTest() results:\n%s\n",
            output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunSyncCondDeleteTest()
 *
 *    Runs the 'vte_test_condDelete()' test from "test_sys_sync.c"
 *
 */

NATIVEEXPORT_API void test_sysRunSyncCondDeleteTest()
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysSync();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_condDelete");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunSyncCondDeleteTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunSyncCondDeleteTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  test_sysRunSyncCondNewTest()
 *
 *    Runs the 'vte_test_condNew()' test from "test_sys_sync.c"
 *
 */

NATIVEEXPORT_API void test_sysRunSyncCondNewTest()
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysSync();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_condNew");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunSyncCondNewTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunSyncCondNewTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}

NATIVEEXPORT_API void test_sysRunSyncCondGetSetTest()
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysSync();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_condGetSet");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunSyncCondGetSetTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunSyncCondGetSetTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}

NATIVEEXPORT_API void test_sysRunSyncCondPingPongTest()
{
    CuSuite* suite;
    CuSuite* timeSuite;
    CuString* output;

    timeSuite = getTestSuite_sysSync();
    suite = CuSuiteNewCloneTest(timeSuite, "vte_test_condPingPong");
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunSyncCondPingPongTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunSyncCondPingPongTest() results:\n%s\n",
            output->buffer);

    CuSuiteFree(suite);
    CuSuiteFree(timeSuite);
    CuStringFree(output);
}

NATIVEEXPORT_API void test_sysRunSyncCondWaitForTest()
{
    CuSuite* suite;
    CuSuite* tmpSuite;
    CuString* output;

    tmpSuite = getTestSuite_sysSync();
    suite = CuSuiteNewCloneTest(tmpSuite, "vte_test_condWaitFor");
    CuSuiteFree(tmpSuite);
    if (NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "test_sysRunSyncCondWaitForTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_sysRunSyncCondWaitForTest() results:\n%s\n", output->buffer);

    CuSuiteFree(suite);
    CuStringFree(output);
}

/************************************************************************
 ************************************************************************

 Runners for individual Util tests

 ****/

/****************************************************************************
 *
 *  test_sysRunUtilEnvGet()
 *
 *    Runs the 'vte_test_util_envGet()' test from "test_sys_util.c"
 *
 */

NATIVEEXPORT_API void test_sysRunUtilEnvGet(void)
{
#ifdef TEST_SYS_MPEOS

    CuSuite* suite;
    CuSuite* tempSuite;
    CuString* output;

    tempSuite = getTestSuite_sysUtil();
    suite = CuSuiteNewCloneTest(tempSuite, "vte_test_util_envGet");
    if( NULL == suite )
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST, "test_sysRunUtilEnvGet() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\ntest_sysRunUtilEnvGet() results:\n%s\n", output->buffer );

    CuSuiteFree(suite);
    CuSuiteFree(tempSuite);
    CuStringFree(output);
#else
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n\n#\n####  'test_sysRunUtilEnvGet' did not run\n#\n\n");
#endif
}

/****************************************************************************
 *
 *  test_sysRunUtilEnvInit()
 *
 *    Runs the 'vte_test_util_envInit()' test from "test_sys_util.c"
 *
 */

NATIVEEXPORT_API void test_sysRunUtilEnvInit(void)
{
#ifdef TEST_SYS_MPEOS

    CuSuite* suite;
    CuSuite* tempSuite;
    CuString* output;

    tempSuite = getTestSuite_sysUtil();
    suite = CuSuiteNewCloneTest(tempSuite, "vte_test_util_envInit");
    if( NULL == suite )
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST, "test_sysRunUtilEnvInit() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\ntest_sysRunUtilEnvInit() results:\n%s\n", output->buffer );

    CuSuiteFree(suite);
    CuSuiteFree(tempSuite);
    CuStringFree(output);

#else
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n\n#\n####  'test_sysRunUtilEnvInit' did not run\n#\n\n");
#endif
}

/****************************************************************************
 *
 *  test_sysRunUtilEnvSet()
 *
 *    Runs the 'vte_test_util_envSet()' test from "test_sys_util.c"
 *
 */

NATIVEEXPORT_API void test_sysRunUtilEnvSet(void)
{
#ifdef TEST_SYS_MPEOS

    CuSuite* suite;
    CuSuite* tempSuite;
    CuString* output;

    tempSuite = getTestSuite_sysUtil();
    suite = CuSuiteNewCloneTest(tempSuite, "vte_test_util_envSet");
    if( NULL == suite )
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST, "test_sysRunUtilEnvSet() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\ntest_sysRunUtilEnvSet() results:\n%s\n", output->buffer );

    CuSuiteFree(suite);
    CuSuiteFree(tempSuite);
    CuStringFree(output);
#else
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n\n#\n####  'test_sysRunUtilEnvSet' did not run\n#\n\n");
#endif
}

/****************************************************************************
 *
 *  test_sysRunUtilIniSetPath(()
 *
 *    Runs the 'vte_test_util_iniSetPath()' test from "test_sys_util.c"
 *
 */

NATIVEEXPORT_API void test_sysRunUtilIniSetPath(void)
{
#ifdef TEST_SYS_MPEOS

    CuSuite* suite;
    CuSuite* tempSuite;
    CuString* output;

    tempSuite = getTestSuite_sysUtil();
    suite = CuSuiteNewCloneTest(tempSuite, "vte_test_util_iniSetPath");
    if( NULL == suite )
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST, "test_sysRunUtilIniSetPath() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\ntest_sysRunUtilIniSetPath() results:\n%s\n", output->buffer );

    CuSuiteFree(suite);
    CuSuiteFree(tempSuite);
    CuStringFree(output);
#else
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n\n#\n####  'test_sysRunUtilIniSetPath' did not run\n#\n\n");
#endif
}

/****************************************************************************
 *
 *  test_sysRunUtilLongJmp(()
 *
 *    Runs the 'vte_test_util_longJmp()' test from "test_sys_util.c"
 *
 */

NATIVEEXPORT_API void test_sysRunUtilLongJmp(void)
{
#ifdef TEST_SYS_MPEOS

    CuSuite* suite;
    CuSuite* tempSuite;
    CuString* output;

    tempSuite = getTestSuite_sysUtil();
    suite = CuSuiteNewCloneTest(tempSuite, "vte_test_util_longJmp");
    if( NULL == suite )
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST, "test_sysRunUtilLongJmp() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\ntest_sysRunUtilLongJmp() results:\n%s\n", output->buffer );

    CuSuiteFree(suite);
    CuSuiteFree(tempSuite);
    CuStringFree(output);
#else
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n\n#\n####  'test_sysRunUtilLongJmp' did not run\n#\n\n");
#endif
}

/****************************************************************************
 *
 *  test_sysRunUtilRegisterForPowerKey()
 *
 *    Runs the 'vte_test_util_registerForPowerKey()' test from "test_sys_util.c"
 *
 */

NATIVEEXPORT_API void test_sysRunUtilRegisterForPowerKey(void)
{
#ifdef TEST_SYS_MPEOS

    CuSuite* suite;
    CuSuite* tempSuite;
    CuString* output;

    tempSuite = getTestSuite_sysUtil();
    suite = CuSuiteNewCloneTest(tempSuite, "vte_test_util_registerForPowerKey");
    if( NULL == suite )
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST, "test_sysRunUtilRegisterForPowerKey() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\ntest_sysRunUtilRegisterForPowerKey() results:\n%s\n", output->buffer );

    CuSuiteFree(suite);
    CuSuiteFree(tempSuite);
    CuStringFree(output);
#else
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n\n#\n####  'test_sysRunUtilRegisterForPowerKey' did not run\n#\n\n");
#endif
}

/****************************************************************************
 *
 *  test_sysRunUtilSetJmp(()
 *
 *    Runs the 'vte_test_util_setJmp()' test from "test_sys_util.c"
 *
 */

NATIVEEXPORT_API void test_sysRunUtilSetJmp(void)
{
#ifdef TEST_SYS_MPEOS

    CuSuite* suite;
    CuSuite* tempSuite;
    CuString* output;

    tempSuite = getTestSuite_sysUtil();
    suite = CuSuiteNewCloneTest(tempSuite, "vte_test_util_setJmp");
    if( NULL == suite )
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST, "test_sysRunUtilSetJmp() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\ntest_sysRunUtilSetJmp() results:\n%s\n", output->buffer );

    CuSuiteFree(suite);
    CuSuiteFree(tempSuite);
    CuStringFree(output);

#else
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n\n#\n####  'test_sysRunUtilSetJmp' did not run\n#\n\n");
#endif
}

/****************************************************************************
 *
 *  test_sysRunUtilStbBoot()
 *
 *    Runs the 'vte_test_util_stbBoot()' test from "test_sys_util.c"
 *
 */

NATIVEEXPORT_API void test_sysRunUtilStbBoot(void)
{
#ifdef TEST_SYS_MPEOS

    CuSuite* suite;
    CuSuite* tempSuite;
    CuString* output;

    tempSuite = getTestSuite_sysUtil();
    suite = CuSuiteNewCloneTest(tempSuite, "vte_test_util_stbBoot");
    if( NULL == suite )
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST, "test_sysRunUtilStbBoot() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\ntest_sysRunUtilStbBoot() results:\n%s\n", output->buffer );

    CuSuiteFree(suite);
    CuSuiteFree(tempSuite);
    CuStringFree(output);
#else
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n\n#\n####  'test_sysRunUtilStbBoot' did not run\n#\n\n");
#endif
}

/****************************************************************************
 *
 *  test_sysRunUtilStbBootStatus()
 *
 *    Runs the 'vte_test_util_stbBootStatus()' test from "test_sys_util.c"
 *
 */

NATIVEEXPORT_API void test_sysRunUtilStbBootStatus(void)
{
#ifdef TEST_SYS_MPEOS
    CuSuite* suite;
    CuSuite* tempSuite;
    CuString* output;

    tempSuite = getTestSuite_sysUtil();
    suite = CuSuiteNewCloneTest(tempSuite, "vte_test_util_stbBootStatus");
    if( NULL == suite )
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST, "test_sysRunUtilStbBootStatus() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\ntest_sysRunUtilStbBootStatus() results:\n%s\n", output->buffer );

    CuSuiteFree(suite);
    CuSuiteFree(tempSuite);
    CuStringFree(output);
#else
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n\n#\n####  'test_sysRunUtilStbBootStatus' did not run\n#\n\n");
#endif
}

/****************************************************************************
 *
 *  test_sysRunUtilStbGetAcOutletState()
 *
 *    Runs the 'vte_test_util_stbGetAcOutletState()' test from "test_sys_util.c"
 *
 */

NATIVEEXPORT_API void test_sysRunUtilStbGetAcOutletState(void)
{
#ifdef TEST_SYS_MPEOS
    CuSuite* suite;
    CuSuite* tempSuite;
    CuString* output;

    tempSuite = getTestSuite_sysUtil();
    suite = CuSuiteNewCloneTest(tempSuite, "vte_test_util_stbGetAcOutletState");
    if( NULL == suite )
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST, "test_sysRunUtilStbGetAcOutletState() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\ntest_sysRunUtilStbGetAcOutletState() results:\n%s\n", output->buffer );

    CuSuiteFree(suite);
    CuSuiteFree(tempSuite);
    CuStringFree(output);
#else
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n\n#\n####  'test_sysRunUtilStbGetAcOutletState' did not run\n#\n\n");
#endif
}

/****************************************************************************
 *
 *  test_sysRunUtilStbGetPowerStatus()
 *
 *    Runs the 'vte_test_util_stbGetPowerStatus()' test from "test_sys_util.c"
 *
 */

NATIVEEXPORT_API void test_sysRunUtilStbGetPowerStatus(void)
{
#ifdef TEST_SYS_MPEOS
    CuSuite* suite;
    CuSuite* tempSuite;
    CuString* output;

    tempSuite = getTestSuite_sysUtil();
    suite = CuSuiteNewCloneTest(tempSuite, "vte_test_util_stbGetPowerStatus");
    if( NULL == suite )
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST, "test_sysRunUtilStbGetPowerStatus() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\ntest_sysRunUtilStbGetPowerStatus() results:\n%s\n", output->buffer );

    CuSuiteFree(suite);
    CuSuiteFree(tempSuite);
    CuStringFree(output);
#else
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n\n#\n####  'test_sysRunUtilStbGetPowerStatus' did not run\n#\n\n");
#endif
}

/****************************************************************************
 *
 *  test_sysRunUtilStbGetRootCerts()
 *
 *    Runs the 'vte_test_util_stbGetRootCerts()' test from "test_sys_util.c"
 *
 */

NATIVEEXPORT_API void test_sysRunUtilStbGetRootCerts(void)
{
#ifdef TEST_SYS_MPEOS
    CuSuite* suite;
    CuSuite* tempSuite;
    CuString* output;

    tempSuite = getTestSuite_sysUtil();
    suite = CuSuiteNewCloneTest(tempSuite, "vte_test_util_stbGetRootCerts");
    if( NULL == suite )
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST, "test_sysRunUtilStbGetRootCerts() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\ntest_sysRunUtilStbGetRootCerts() results:\n%s\n", output->buffer );

    CuSuiteFree(suite);
    CuSuiteFree(tempSuite);
    CuStringFree(output);
#else
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n\n#\n####  'test_sysRunUtilStbGetRootCerts' did not run\n#\n\n");
#endif
}

/****************************************************************************
 *
 *  test_sysRunUtilStbSetAcOutletStateTest()
 *
 *    Runs the 'vte_test_util_stbSetAcOutletState()' test from "test_sys_util.c"
 *
 */

NATIVEEXPORT_API void test_sysRunUtilStbSetAcOutletStateTest(void)
{
#ifdef TEST_SYS_MPEOS
    CuSuite* suite;
    CuSuite* tempSuite;
    CuString* output;

    tempSuite = getTestSuite_sysUtil();
    suite = CuSuiteNewCloneTest(tempSuite, "vte_test_util_stbSetAcOutletState");
    if( NULL == suite )
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST, "test_sysRunUtilStbSetAcOutletStateTest() -  can not create new suite\n");
        return;
    }

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\ntest_sysRunUtilStbSetAcOutletStateTest() results:\n%s\n", output->buffer );

    CuSuiteFree(suite);
    CuSuiteFree(tempSuite);
    CuStringFree(output);
#else
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n\n#\n####  'test_sysRunUtilStbSetAcOutletStateTest' did not run\n#\n\n");
#endif
}

