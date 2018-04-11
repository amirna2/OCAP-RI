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


/** @author       Amir Nathoo - Vidiom System, Inc.
 **				  Prasanna - added TSB test cases (1/21/05)
 ** 
 ** @strategy     This test suite exercices all MPE/MPEOS DVR Manager (DVR) APIs
 **				  Some tests will require visual checking of expected result.
 **
 *************************************************************************************
 **
 ** HOW TO RUN THESE TESTS 
 **
 **
 **
 **
 **/

// #include <OCAPNativeTest.h> // To export RunAllTests function.

#include "test_dvr.h"
#include "test_media.h"
#include <mpeos_mem.h>
#include <mpeos_event.h>
#include <mpeos_media.h>

// TODO: really need to put these external prototypes in a common header file
CuSuite* getTestSuite_TSB(void);
CuSuite* getTestSuite_TSB1(void);
CuSuite* getTestSuite_TSB2(void);
CuSuite* getTestSuite_TSB3(void);
CuSuite* getTestSuite_TSB4(void);
CuSuite* getTestSuite_Recording1(void);
CuSuite* getTestSuite_Recording2(void);
CuSuite* getTestSuite_Recording3(void);
CuSuite* getTestSuite_Recording4(void);
CuSuite* getTestSuite_Recording5(void);
CuSuite* getTestSuite_Recording6(void);
CuSuite* getTestSuite_Recording7(void);
CuSuite* getTestSuite_Recording8(void);

NATIVEEXPORT_API void test_dvr_RunAllTests(void);
NATIVEEXPORT_API void test_dvr_RunRecordingTest1(void);
NATIVEEXPORT_API void test_dvr_RunRecordingTest2(void);
NATIVEEXPORT_API void test_dvr_RunRecordingTest3(void);
NATIVEEXPORT_API void test_dvr_RunRecordingTest4(void);
NATIVEEXPORT_API void test_dvr_RunRecordingTest5(void);
NATIVEEXPORT_API void test_dvr_RunRecordingTest6(void);
NATIVEEXPORT_API void test_dvr_RunRecordingTest7(void);
NATIVEEXPORT_API void test_dvr_RunRecordingTest8(void);
NATIVEEXPORT_API void test_dvr_RunTSBTests(void);
NATIVEEXPORT_API void test_dvr_RunTSBTest1(void);
NATIVEEXPORT_API void test_dvr_RunTSBTest2(void);
NATIVEEXPORT_API void test_dvr_RunTSBTest3(void);
NATIVEEXPORT_API void test_dvr_RunTSBTest4(void);

/**
 * Will test all the dvr 
 */
NATIVEEXPORT_API void test_dvr_RunAllTests(void)
{
    CuSuite* suite = CuSuiteNew();
    CuSuite* tmpSuite;
    CuString *output;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n############\n#\n#  'test_dvr_RunAllTests()' starting\n#\n");

    // to initialize and prepare for media tuning and decoding
    MediaTest_Init();

    // Define the suite.
    tmpSuite = getTestSuite_Recording1();
    CuSuiteAddSuite(suite, tmpSuite);
    //CuSuiteFree( tmpSuite );

    tmpSuite = getTestSuite_Recording2();
    CuSuiteAddSuite(suite, tmpSuite);
    //CuSuiteFree( tmpSuite );

    tmpSuite = getTestSuite_Recording3();
    CuSuiteAddSuite(suite, tmpSuite);
    //CuSuiteFree( tmpSuite );

    tmpSuite = getTestSuite_Recording4();
    CuSuiteAddSuite(suite, tmpSuite);
    //CuSuiteFree( tmpSuite );

    tmpSuite = getTestSuite_Recording5();
    CuSuiteAddSuite(suite, tmpSuite);
    //CuSuiteFree( tmpSuite );
    tmpSuite = getTestSuite_Recording6();
    CuSuiteAddSuite(suite, tmpSuite);
    //CuSuiteFree( tmpSuite );
    tmpSuite = getTestSuite_Recording7();
    CuSuiteAddSuite(suite, tmpSuite);
    //CuSuiteFree( tmpSuite );
    tmpSuite = getTestSuite_Recording8();
    CuSuiteAddSuite(suite, tmpSuite);
    //CuSuiteFree( tmpSuite );

    tmpSuite = getTestSuite_TSB();
    CuSuiteAddSuite(suite, tmpSuite);
    //CuSuiteFree( tmpSuite );

    // Run all test suites.
    CuSuiteRun(suite);

    // Report results
    output = CuStringNew();
    CuSuiteDetails(suite, output);

    MediaTest_Destroy();

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "Test results: test_dvr_RunAllTests\n%s\n", output->buffer);
    vte_agent_Log("Test results: test_dvr_RunAllTests\n%s\n", output->buffer);
} // end test_dvrRunAllTests()

NATIVEEXPORT_API void test_dvr_RunRecordingTest1(void)
{
    CuSuite * suite;
    CuString * output;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n############\n#\n#  'test_dvr_RunRecordingTest1()' starting\n#\n");

    suite = CuSuiteNew();
    CuSuiteAddSuite(suite, getTestSuite_Recording1());
    CuSuiteRun(suite);

    // Report results
    output = CuStringNew();
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "Test results: test_dvr_RunRecordingTest1\n%s\n", output->buffer);
    vte_agent_Log("Test results: test_dvr_RunRecordingTest1\n%s\n",
            output->buffer);

}

NATIVEEXPORT_API void test_dvr_RunRecordingTest2(void)
{
    CuSuite * suite;
    CuString * output;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n############\n#\n#  'test_dvr_RunRecordingTest2()' starting\n#\n");

    suite = CuSuiteNew();
    CuSuiteAddSuite(suite, getTestSuite_Recording2());
    CuSuiteRun(suite);

    // Report results
    output = CuStringNew();
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "Test results: test_dvr_RunRecordingTest2\n%s\n", output->buffer);
    vte_agent_Log("Test results: test_dvr_RunRecordingTest2\n%s\n",
            output->buffer);

}

NATIVEEXPORT_API void test_dvr_RunRecordingTest3(void)
{
    CuSuite * suite;
    CuString * output;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n############\n#\n#  'test_dvr_RunRecordingTest3()' starting\n#\n");

    suite = CuSuiteNew();
    CuSuiteAddSuite(suite, getTestSuite_Recording3());
    CuSuiteRun(suite);

    // Report results
    output = CuStringNew();
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "Test results: test_dvr_RunRecordingTest3\n%s\n", output->buffer);
    vte_agent_Log("Test results: test_dvr_RunRecordingTest3\n%s\n",
            output->buffer);

}

NATIVEEXPORT_API void test_dvr_RunRecordingTest4(void)
{
    CuSuite * suite;
    CuString * output;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n############\n#\n#  'test_dvr_RunRecordingTest4()' starting\n#\n");

    suite = CuSuiteNew();
    CuSuiteAddSuite(suite, getTestSuite_Recording4());
    CuSuiteRun(suite);

    // Report results
    output = CuStringNew();
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "Test results: test_dvr_RunRecordingTest4\n%s\n", output->buffer);
    vte_agent_Log("Test results: test_dvr_RunRecordingTest4\n%s\n",
            output->buffer);

}

NATIVEEXPORT_API void test_dvr_RunRecordingTest5(void)
{
    CuSuite * suite;
    CuString * output;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n############\n#\n#  'test_dvr_RunRecordingTest5()' starting\n#\n");

    suite = CuSuiteNew();
    CuSuiteAddSuite(suite, getTestSuite_Recording5());
    CuSuiteRun(suite);

    // Report results
    output = CuStringNew();
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "Test results: test_dvr_RunRecordingTest5\n%s\n", output->buffer);
    vte_agent_Log("Test results: test_dvr_RunRecordingTest5\n%s\n",
            output->buffer);

}

NATIVEEXPORT_API void test_dvr_RunRecordingTest6(void)
{
    CuSuite * suite;
    CuString * output;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n############\n#\n#  'test_dvr_RunRecordingTest6()' starting\n#\n");

    suite = CuSuiteNew();
    CuSuiteAddSuite(suite, getTestSuite_Recording6());
    CuSuiteRun(suite);

    // Report results
    output = CuStringNew();
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "Test results: test_dvr_RunRecordingTest6\n%s\n", output->buffer);
    vte_agent_Log("Test results: test_dvr_RunRecordingTest6\n%s\n",
            output->buffer);

}

NATIVEEXPORT_API void test_dvr_RunRecordingTest7(void)
{
    CuSuite * suite;
    CuString * output;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n############\n#\n#  'test_dvr_RunRecordingTest7()' starting\n#\n");

    suite = CuSuiteNew();
    CuSuiteAddSuite(suite, getTestSuite_Recording7());
    CuSuiteRun(suite);

    // Report results
    output = CuStringNew();
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "Test results: test_dvr_RunRecordingTest7\n%s\n", output->buffer);
    vte_agent_Log("Test results: test_dvr_RunRecordingTest7\n%s\n",
            output->buffer);

}

NATIVEEXPORT_API void test_dvr_RunRecordingTest8(void)
{
    CuSuite * suite;
    CuString * output;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n############\n#\n#  'test_dvr_RunRecordingTest8()' starting\n#\n");

    suite = CuSuiteNew();
    CuSuiteAddSuite(suite, getTestSuite_Recording8());
    CuSuiteRun(suite);

    // Report results
    output = CuStringNew();
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "Test results: test_dvr_RunRecordingTest8\n%s\n", output->buffer);
    vte_agent_Log("Test results: test_dvr_RunRecordingTest8\n%s\n",
            output->buffer);

}

NATIVEEXPORT_API void test_dvr_RunTSBTests(void)
{
    CuSuite * suite;
    CuString * output;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n############\n#\n#  'test_dvr_RunTSBTests()' starting\n#\n");

    // to initialize and prepare for media tuning and decoding
    MediaTest_Init();

    suite = CuSuiteNew();
    CuSuiteAddSuite(suite, getTestSuite_TSB());
    CuSuiteRun(suite);

    // Report results
    output = CuStringNew();
    CuSuiteDetails(suite, output);

    MediaTest_Destroy();

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "Test results: test_dvr_RunTSBTests\n%s\n", output->buffer);
    vte_agent_Log("Test results: test_dvr_RunTSBTests\n%s\n", output->buffer);

}

NATIVEEXPORT_API void test_dvr_RunTSBTest1(void)
{
    CuSuite * suite;
    CuString * output;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n############\n#\n#  'test_dvr_RunTSBTest1()' starting\n#\n");

    // to initialize and prepare for media tuning and decoding
    MediaTest_Init();

    suite = CuSuiteNew();
    CuSuiteAddSuite(suite, getTestSuite_TSB1());
    CuSuiteRun(suite);

    // Report results
    output = CuStringNew();
    CuSuiteDetails(suite, output);

    MediaTest_Destroy();

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "Test results: test_dvr_RunTSBTest1\n%s\n", output->buffer);
    //  vte_agent_Log( "Test results: test_dvr_RunTSBTests\n%s\n",
    //                 output->buffer );

}

NATIVEEXPORT_API void test_dvr_RunTSBTest2(void)
{
    CuSuite * suite;
    CuString * output;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n############\n#\n#  'test_dvr_RunTSBTest2()' starting\n#\n");

    suite = CuSuiteNew();
    CuSuiteAddSuite(suite, getTestSuite_TSB2());
    CuSuiteRun(suite);

    // Report results
    output = CuStringNew();
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "Test results: test_dvr_RunTSBTests\n%s\n", output->buffer);
    vte_agent_Log("Test results: test_dvr_RunTSBTest2\n%s\n", output->buffer);

}

NATIVEEXPORT_API void test_dvr_RunTSBTest3(void)
{
    CuSuite * suite;
    CuString * output;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n############\n#\n#  'test_dvr_RunTSBTest3()' starting\n#\n");

    suite = CuSuiteNew();
    CuSuiteAddSuite(suite, getTestSuite_TSB3());
    CuSuiteRun(suite);

    // Report results
    output = CuStringNew();
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "Test results: test_dvr_RunTSBTest3\n%s\n", output->buffer);
    vte_agent_Log("Test results: test_dvr_RunTSBTests\n%s\n", output->buffer);

}

NATIVEEXPORT_API void test_dvr_RunTSBTest4(void)
{
    CuSuite * suite;
    CuString * output;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n############\n#\n#  'test_dvr_RunTSBTest4()' starting\n#\n");

    suite = CuSuiteNew();
    CuSuiteAddSuite(suite, getTestSuite_TSB4());
    CuSuiteRun(suite);

    // Report results
    output = CuStringNew();
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "Test results: test_dvr_RunTSBTest4\n%s\n", output->buffer);
    vte_agent_Log("Test results: test_dvr_RunTSBTests\n%s\n", output->buffer);

}
