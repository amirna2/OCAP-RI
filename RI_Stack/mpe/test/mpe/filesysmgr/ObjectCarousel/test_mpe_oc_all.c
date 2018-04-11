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

#include <stdio.h>
// #include <cutest.h>
// #include <VTE_CuTest.h>
// #include <vte_agent.h>
#include <test_oc.h>

CuSuite* vpk_suite_oc_all(void);
CuSuite* vpk_suite_oc_fileRead(void);
CuSuite* vpk_suite_oc_genCRC(void);
CuSuite* vpk_suite_oc_mountUnmount(void);
CuSuite* vpk_suite_oc_multiMount(void);
CuSuite* vpk_suite_oc_multiThreadCRC(void);
CuSuite* vpk_suite_oc_printDirTree(void);
CuSuite* vpk_suite_oc_repeatedMount(void);
CuSuite* vpk_suite_oc_walkDirTree(void);

void test_mpe_fileocRunAllTests(void)
{
    CuSuite* suite;
    CuString* output;

    suite = CuSuiteNew();

    CuSuiteAddSuite(suite, vpk_suite_oc_all());

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_mpe_fileocRunAllTests\n%s\n",
            output->buffer);

    // Free the memory
    CuSuiteFree(suite);
    CuStringFree(output);
} /* end test_mpe_fileioRunAllTests(void) */

void test_mpe_fileocFileRead(void)
{
    CuSuite* suite;
    CuString* output;

    suite = CuSuiteNew();

    CuSuiteAddSuite(suite, vpk_suite_oc_fileRead());

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_mpe_fileocFileRead\n%s\n", output->buffer);

    // Free the memory
    CuSuiteFree(suite);
    CuStringFree(output);
} /* end test_mpe_fileocFileRead(void) */

void test_mpe_fileocGenCRC(void)
{
    CuSuite* suite;
    CuString* output;

    suite = CuSuiteNew();

    CuSuiteAddSuite(suite, vpk_suite_oc_genCRC());

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_mpe_fileocGenCRC\n%s\n", output->buffer);

    // Free the memory
    CuSuiteFree(suite);
    CuStringFree(output);
} /* end test_mpe_fileocGenCRC(void) */

void test_mpe_fileocMountUnmount(void)
{
    CuSuite* suite;
    CuString* output;

    suite = CuSuiteNew();

    CuSuiteAddSuite(suite, vpk_suite_oc_mountUnmount());

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_mpe_fileocMountUnmount\n%s\n",
            output->buffer);

    // Free the memory
    CuSuiteFree(suite);
    CuStringFree(output);
} /* end test_mpe_fileocMountUnmount(void) */

void test_mpe_fileocMultiMount(void)
{
    CuSuite* suite;
    CuString* output;

    suite = CuSuiteNew();

    CuSuiteAddSuite(suite, vpk_suite_oc_multiMount());

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_mpe_fileocMultiMount\n%s\n",
            output->buffer);

    // Free the memory
    CuSuiteFree(suite);
    CuStringFree(output);
} /* end test_mpe_fileocMultiMount(void) */

void test_mpe_fileocMultiThreadCRC(void)
{
    CuSuite* suite;
    CuString* output;

    suite = CuSuiteNew();

    CuSuiteAddSuite(suite, vpk_suite_oc_multiThreadCRC());

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_mpe_fileocMultiThreadCRC\n%s\n",
            output->buffer);

    // Free the memory
    CuSuiteFree(suite);
    CuStringFree(output);
} /* end test_mpe_fileocMultiThreadCRC(void) */

void test_mpe_fileocPrintDirTreeCRC(void)
{
    CuSuite* suite;
    CuString* output;

    suite = CuSuiteNew();

    CuSuiteAddSuite(suite, vpk_suite_oc_printDirTree());

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_mpe_fileocPrintDirTreeCRC\n%s\n",
            output->buffer);

    // Free the memory
    CuSuiteFree(suite);
    CuStringFree(output);
} /* end test_mpe_fileocPrintDirTreeCRC(void) */

void test_mpe_fileocRepeatedMount(void)
{
    CuSuite* suite;
    CuString* output;

    suite = CuSuiteNew();

    CuSuiteAddSuite(suite, vpk_suite_oc_repeatedMount());

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_mpe_fileocRepeatedMount\n%s\n",
            output->buffer);

    // Free the memory
    CuSuiteFree(suite);
    CuStringFree(output);
} /* end test_mpe_fileocRepeatedMount(void) */

void test_mpe_fileocWalkDirTree(void)
{
    CuSuite* suite;
    CuString* output;

    suite = CuSuiteNew();

    CuSuiteAddSuite(suite, vpk_suite_oc_walkDirTree());

    CuSuiteRun(suite);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_mpe_fileocWalkDirTree\n%s\n",
            output->buffer);

    // Free the memory
    CuSuiteFree(suite);
    CuStringFree(output);
} /* end test_mpe_fileocWalkDirTree(void) */
