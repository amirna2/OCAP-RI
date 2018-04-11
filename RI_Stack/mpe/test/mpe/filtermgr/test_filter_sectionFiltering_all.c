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

/*
 * @author       Ric Yeates
 * 
 * @strategy     Test section filtering APIs
 */
#include "test_filter_sectionFiltering_include.h"
#include <test_utils.h>

void test_sectionFiltering_RunAllTests(void);
void test_sectionFiltering_RunSimple(void);
void test_sectionFiltering_RunBasic(void);
void test_sectionFiltering_RunDump(void);
void test_sectionFiltering_RunPriority(void);
void test_sectionFiltering_RunCancel(void);
void test_sectionFiltering_RunOOBDump(void);
void test_sectionFiltering_RunNegative(void);
void test_sectionFiltering_RunOOBChange(void);

// TODO: put these external prototypes into a common header file
extern void test_getTestSuite_sectionFiltering_OOBDump(CuSuite* suite);
extern void test_getTestSuite_sectionFiltering_OOBChange(CuSuite* suite);

void test_sectionFiltering_RunAllTests(void)
{
    CuSuite *suite;
    CuString *output;

    suite = CuSuiteNew();

    test_getTestSuite_sectionFiltering_SimpleFilter(suite);
    test_getTestSuite_sectionFiltering_Basic(suite);
    test_getTestSuite_sectionFiltering_Dump(suite);
    test_getTestSuite_sectionFiltering_Negative(suite);
    test_getTestSuite_sectionFiltering_Priority(suite);
    test_getTestSuite_sectionFiltering_Cancel(suite);
    test_getTestSuite_sectionFiltering_OOBDump(suite);
    // add more here

    // Run all test suites.
    CuSuiteRun(suite);

    // Report results
    output = CuStringNew();
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "Test results: test_sectionFilteringRunAllTests: %s\n",
            output->buffer);
    vte_agent_Log("Test results: test_sectionFilteringRunAllTests\n%s\n",
            output->buffer);

    VTE_CuSuiteFree(suite);
}

void test_sectionFiltering_RunSimple(void)
{
    CuSuite *suite;
    CuString *output;

    suite = CuSuiteNew();

    test_getTestSuite_sectionFiltering_SimpleFilter(suite);

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "test_sectionFiltering_RunSimple - Test results -%s\n",
            output->buffer);
    vte_agent_Log("test_sectionFiltering_RunSimple - Test results -%s\n",
            output->buffer);

    VTE_CuSuiteFree(suite);
}

void test_sectionFiltering_RunBasic(void)
{
    CuSuite *suite;
    CuString *output;

    suite = CuSuiteNew();

    test_getTestSuite_sectionFiltering_Basic(suite);

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "test_sectionFiltering_Basic - Test results -%s\n", output->buffer);
    vte_agent_Log("test_sectionFiltering_Basic - Test results -%s\n",
            output->buffer);

    VTE_CuSuiteFree(suite);
}

void test_sectionFiltering_RunDump(void)
{
    CuSuite *suite;
    CuString *output;

    suite = CuSuiteNew();

    test_getTestSuite_sectionFiltering_Dump(suite);

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "test_sectionFiltering_Dump - Test results -%s\n", output->buffer);
    vte_agent_Log("test_sectionFiltering_Dump - Test results -%s\n",
            output->buffer);

    VTE_CuSuiteFree(suite);
}

void test_sectionFiltering_RunNegative(void)
{
    CuSuite *suite;
    CuString *output;

    suite = CuSuiteNew();

    test_getTestSuite_sectionFiltering_Negative(suite);

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "test_sectionFiltering_Negative - Test results -%s\n",
            output->buffer);
    vte_agent_Log("test_sectionFiltering_Negative - Test results -%s\n",
            output->buffer);

    VTE_CuSuiteFree(suite);
}

void test_sectionFiltering_RunPriority(void)
{
    CuSuite *suite;
    CuString *output;

    suite = CuSuiteNew();

    test_getTestSuite_sectionFiltering_Priority(suite);

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "test_sectionFiltering_Priority - Test results -%s\n",
            output->buffer);
    vte_agent_Log("test_sectionFiltering_Priority - Test results -%s\n",
            output->buffer);

    VTE_CuSuiteFree(suite);
}

void test_sectionFiltering_RunCancel(void)
{
    CuSuite *suite;
    CuString *output;

    suite = CuSuiteNew();

    test_getTestSuite_sectionFiltering_Cancel(suite);

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "test_sectionFiltering_Cancel - Test results -%s\n", output->buffer);
    vte_agent_Log("test_sectionFiltering_Cancel - Test results -%s\n",
            output->buffer);

    VTE_CuSuiteFree(suite);
}

void test_sectionFiltering_RunOOBDump(void)
{
    CuSuite *suite;
    CuString *output;

    suite = CuSuiteNew();

    test_getTestSuite_sectionFiltering_OOBDump(suite);

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "test_sectionFiltering_OOBDump - Test results -%s\n",
            output->buffer);
    vte_agent_Log("test_sectionFiltering_OOBDump - Test results -%s\n",
            output->buffer);

    VTE_CuSuiteFree(suite);
}

void test_sectionFiltering_RunOOBChange(void)
{
    CuSuite *suite;
    CuString *output;

    suite = CuSuiteNew();

    test_getTestSuite_sectionFiltering_OOBChange(suite);

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "test_sectionFiltering_OOBChange - Test results -%s\n",
            output->buffer);
    vte_agent_Log("test_sectionFiltering_OOBChange - Test results -%s\n",
            output->buffer);

    VTE_CuSuiteFree(suite);
}
