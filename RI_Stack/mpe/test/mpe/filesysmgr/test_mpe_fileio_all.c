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

#include "test_file.h"

void test_mpe_fileioRunAllTests(void);
void test_mpe_fileioRunAllTests_sim(void);
void test_mpe_fileioRunAllTests_bfs(void);
void test_mpe_fileioRunAllTests_snfs(void);
void test_mpe_fileioRunAllTests_romfs(void);
void test_mpe_fileioRunAllTests_itfs(void);
void test_mpe_recurseDir_romfs(void);
void test_mpe_recurseDir_snfs(void);
void test_mpe_recurseDir_sim(void);
void test_mpe_recurseDir_itfs(void);
void test_mpe_fileChange_bfs(void);
void test_mpe_odn(void);

/*
 ** Run all the tests and suites with one call
 */
static CuSuite* test_fileio(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, test_mpe_fileOpenClose);
    SUITE_ADD_TEST(suite, test_mpe_fileRead);
    SUITE_ADD_TEST(suite, test_mpe_fileWrite);
    SUITE_ADD_TEST(suite, test_mpe_fileSeek);
    SUITE_ADD_TEST(suite, test_mpe_fileSync);
    SUITE_ADD_TEST(suite, test_mpe_fileDelete);
    SUITE_ADD_TEST(suite, test_mpe_fileRename);
    SUITE_ADD_TEST(suite, test_mpe_dirOpenClose);
    SUITE_ADD_TEST(suite, test_mpe_dirRead);
    SUITE_ADD_TEST(suite, test_mpe_dirRenameDelete);
    SUITE_ADD_TEST(suite, mpe_test_openclose);
    SUITE_ADD_TEST(suite, mpe_test_read);
    SUITE_ADD_TEST(suite, mpe_test_seek);
    SUITE_ADD_TEST(suite, mpe_test_stat);
    SUITE_ADD_TEST(suite, mpe_test_write);
    SUITE_ADD_TEST(suite, mpe_test_dir_openclose);
    SUITE_ADD_TEST(suite, mpe_test_dirread);
    SUITE_ADD_TEST(suite, mpe_test_loadunload);

    return suite;
} /* end test_fileio() */

void test_mpe_fileioRunAllTests(void)
{
    CuString* output;
    CuSuite* pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileio());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_mpe_fileioRunAllTests\n%s\n",
            output->buffer);
} /* end test_mpe_fileioRunAllTests(void) */

void test_mpe_fileioRunAllTests_sim(void)
{
    CuSuite* pSuite;
    CuString* output;
    SETFS( SIM);

    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileio());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_mpe_fileioRunAllTests\n%s\n",
            output->buffer);
} /* end test_mpe_fileioRunAllTests(void) */

void test_mpe_fileioRunAllTests_bfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( BFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileio());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_mpe_fileioRunAllTests\n%s\n",
            output->buffer);
} /* end test_mpe_fileioRunAllTests(void) */

void test_mpe_fileioRunAllTests_snfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( SNFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileio());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_mpe_fileioRunAllTests\n%s\n",
            output->buffer);
} /* end test_mpe_fileioRunAllTests(void) */

void test_mpe_fileioRunAllTests_romfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( ROMFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileio());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_mpe_fileioRunAllTests\n%s\n",
            output->buffer);
} /* end test_mpe_fileioRunAllTests(void) */

void test_mpe_fileioRunAllTests_itfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( ITFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileio());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_mpe_fileioRunAllTests\n%s\n",
            output->buffer);
} /* end test_mpe_fileioRunAllTests(void) */

/*
 ** The following two functions sets up and runs the fileOpenClose test
 */
static CuSuite* test_fileOpenCloseSuite(void)
{
    CuSuite* suite;

    suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, test_mpe_fileOpenClose);

    return suite;
} /* end test_fileOpenCloseSuite() */

void test_fileOpenClose_snfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( SNFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileOpenCloseSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileOpenClose_snfs\n%s\n", output->buffer);
} /* end test_fileOpenClose_snfs(void) */

void test_fileOpenClose_romfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( ROMFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileOpenCloseSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileOpenClose_romfs\n%s\n",
            output->buffer);
} /* end test_fileOpenClose_romfs(void) */

void test_fileOpenClose_bfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( BFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileOpenCloseSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileOpenClose_bfs\n%s\n", output->buffer);
} /* end test_fileOpenClose_bfs(void) */

void test_fileOpenClose_sim(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( SIM);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileOpenCloseSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileOpenClose_sim\n%s\n", output->buffer);
} /* end test_fileOpenClose_sim(void) */

void test_fileOpenClose_itfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( ITFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileOpenCloseSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileOpenClose_itfs\n%s\n", output->buffer);
} /* end test_fileOpenClose_itfs(void) */

/*
 ** The next two functions sets up and runs the file Read tests
 */
static CuSuite* test_fileReadSuite(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, test_mpe_fileRead);

    return suite;
} /* end test_fileReadSuite() */

void test_fileRead_sim(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( SIM);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileReadSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileRead_sim\n%s\n", output->buffer);
} /* end test_fileRead_sim(void) */

void test_fileRead_romfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( ROMFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileReadSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileRead_romfs\n%s\n", output->buffer);
} /* end test_fileRead_romfs(void) */

void test_fileRead_snfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( SNFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileReadSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileRead_snfs\n%s\n", output->buffer);
} /* end test_fileRead_snfs(void) */

void test_fileRead_bfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( BFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileReadSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileRead_bfs\n%s\n", output->buffer);
} /* end test_fileRead_bfs(void) */

void test_fileRead_itfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( ITFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileReadSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileRead_itfs\n%s\n", output->buffer);
} /* end test_fileRead_itfs(void) */

/*
 ** The next two functions sets up and runs the file Write tests
 */
static CuSuite* test_fileWriteSuite(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, test_mpe_fileWrite);

    return suite;
} /* end test_fileWriteSuite() */

void test_fileWrite_sim(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( SIM);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileWriteSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileWrite_sim\n%s\n", output->buffer);
} /* end test_fileWrite_sim(void) */

void test_fileWrite_snfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( SNFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileWriteSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileWrite_snfs\n%s\n", output->buffer);
} /* end test_fileWrite_sfns(void) */

void test_fileWrite_itfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( ITFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileWriteSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileWrite_itfs\n%s\n", output->buffer);
} /* end test_fileWrite_itfs(void) */

/*
 ** The next two functions sets up and runs the file seek tests
 */
static CuSuite* test_fileSeekSuite(void)
{
    CuSuite* suite;

    suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, test_mpe_fileSeek);

    return suite;
} /* end test_fileSeekSuite() */

void test_fileSeek_snfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( SNFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileSeekSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileSeek_snfs\n%s\n", output->buffer);
} /* end test_fileSeek_snfs(void) */

void test_fileSeek_romfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( ROMFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileSeekSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileSeek_romfs\n%s\n", output->buffer);
} /* end test_fileSeek_romfs(void) */

void test_fileSeek_bfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( BFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileSeekSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileSeek_bfs\n%s\n", output->buffer);
} /* end test_fileSeek_bfs(void) */

void test_fileSeek_sim(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( SIM);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileSeekSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileSeek_sim\n%s\n", output->buffer);
} /* end test_fileSeek_sim(void) */

void test_fileSeek_itfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( ITFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileSeekSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileSeek_itfs\n%s\n", output->buffer);
} /* end test_fileSeek_itfs(void) */

/*
 ** The next two functions sets up and runs the file sync tests
 */
static CuSuite* test_fileSyncSuite(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, test_mpe_fileSync);

    return suite;
} /* end test_fileSyncSuite() */

void test_fileSync_sim(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( SIM);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileSyncSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileSync_sim\n%s\n", output->buffer);
} /* end test_fileSync_sim(void) */

void test_fileSync_snfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( SNFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileSyncSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileSync_snfs\n%s\n", output->buffer);
} /* end test_fileSync_snfs(void) */

void test_fileSync_bfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( BFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileSyncSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileSync_bfs\n%s\n", output->buffer);
} /* end test_fileSync_bfs(void) */

void test_fileSync_romfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( ROMFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileSyncSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileSync_romfs\n%s\n", output->buffer);
} /* end test_fileSync_romfs(void) */

void test_fileSync_itfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( ITFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileSyncSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileSync_itfs\n%s\n", output->buffer);
} /* end test_fileSync_itfs(void) */

/*
 ** The next two functions sets up and runs the file delete tests
 */
static CuSuite* test_fileDeleteSuite(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, test_mpe_fileDelete);

    return suite;
} /* end test_fileSyncSuite() */

void test_fileDelete_sim(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( SIM);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileDeleteSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileDelete_sim\n%s\n", output->buffer);
} /* end test_fileDelete_sim(void)*/

void test_fileDelete_snfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( SNFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileDeleteSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileDelete_snfs\n%s\n", output->buffer);
} /* end test_fileDelete_snfs(void)*/

void test_fileDelete_itfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( ITFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileDeleteSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileDelete_itfs\n%s\n", output->buffer);
} /* end test_fileDelete_itfs(void)*/

/*
 ** The next two functions sets up and runs the file rename tests
 */
static CuSuite* test_fileRenameSuite(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, test_mpe_fileRename);

    return suite;
} /* end test_fileRenameSuite() */

void test_fileRename_sim(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( SIM);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileRenameSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileRename_sim\n%s\n", output->buffer);
} /* end test_fileRename_sim(void) */

void test_fileRename_itfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( ITFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileRenameSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileRename_itfs\n%s\n", output->buffer);
} /* end test_fileRename_itfs(void) */

void test_fileRename_snfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( SNFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileRenameSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileRename_snfs\n%s\n", output->buffer);
} /* end test_fileRename_snfs(void) */

/*
 ** The next two functions sets up and runs the file dirOpenClose tests
 */
static CuSuite* test_fileDirOpenCloseSuite(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, test_mpe_dirOpenClose);

    return suite;
} /* end test_fileDirOpenCloseSuite() */

void test_fileDirOpenClose_sim(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( SIM);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileDirOpenCloseSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileDirOpenClose_sim\n%s\n",
            output->buffer);
} /* end test_fileDirOpenClose_sim(void) */

void test_fileDirOpenClose_romfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( ROMFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileDirOpenCloseSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileDirOpenClose_romfs\n%s\n",
            output->buffer);
} /* end test_fileDirOpenClose_romfs(void) */

void test_fileDirOpenClose_snfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( SNFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileDirOpenCloseSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileDirOpenClose_snfs\n%s\n",
            output->buffer);
} /* end test_fileDirOpenClose_snfs(void) */

void test_fileDirOpenClose_itfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( ITFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileDirOpenCloseSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileDirOpenClose_itfs\n%s\n",
            output->buffer);
} /* end test_fileDirOpenClose_itfs(void) */

/*
 ** The next two functions sets up and runs the file DirRead tests
 */
static CuSuite* test_fileDirReadSuite(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, test_mpe_dirRead);

    return suite;
} /* end test_fileDirOpenCloseSuite() */

void test_fileDirRead_sim(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( SIM);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileDirReadSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileDirRead_sim\n%s\n", output->buffer);
} /* end test_fileDirRead_sim(void) */

void test_fileDirRead_snfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( SNFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileDirReadSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileDirRead_snfs\n%s\n", output->buffer);
} /* end test_fileDirRead_snfs(void) */

void test_fileDirRead_itfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( ITFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileDirReadSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileDirRead_itfs\n%s\n", output->buffer);
} /* end test_fileDirRead_itfs(void) */

/*
 ** The next two functions sets up and runs the file DirRenameDelete tests
 */
static CuSuite* test_fileDirRenameDeleteSuite(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, test_mpe_dirRenameDelete);

    return suite;
} /* end test_fileDirRenameDeleteSuite() */

void test_fileDirRenameDelete_sim(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( SIM);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileDirRenameDeleteSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileDirRenameDelete_sim\n%s\n",
            output->buffer);
} /* end test_fileDirRenameDelete_sim(void) */

void test_fileDirRenameDelete_snfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( SNFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileDirRenameDeleteSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileDirRenameDelete_snfs\n%s\n",
            output->buffer);
} /* end test_fileDirRenameDelete_snfs(void) */

void test_fileDirRenameDelete_itfs(void)
{
    CuSuite* pSuite;
    CuString* output;

    SETFS( ITFS);
    pSuite = CuSuiteNew();

    CuSuiteAddSuite(pSuite, test_fileDirRenameDeleteSuite());
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);

    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);
    vte_agent_Log("Test results: test_fileDirRenameDelete_itfs\n%s\n",
            output->buffer);
} /* end test_fileDirRenameDelete_itfs(void) */

void mpe_test_openclose_snfs(void)
{
    RUN_FILE_TEST(SNFS, mpe_test_openclose, mpe_test_openclose_snfs);
} /* end mpe_test_openclose_snfs(void) */

void mpe_test_openclose_romfs(void)
{
    RUN_FILE_TEST(ROMFS, mpe_test_openclose, mpe_test_openclose_romfs);
} /* end mpe_test_openclose_romfs(void) */

void mpe_test_openclose_bfs(void)
{
    RUN_FILE_TEST(BFS, mpe_test_openclose, mpe_test_openclose_bfs);
} /* end mpe_test_openclose_bfs(void) */

void mpe_test_openclose_sim(void)
{
    RUN_FILE_TEST(SIM, mpe_test_openclose, mpe_test_openclose_sim);
} /* end mpe_test_openclose_sim(void) */

void mpe_test_openclose_itfs(void)
{
    RUN_FILE_TEST(ITFS, mpe_test_openclose, mpe_test_openclose_itfs);
} /* end mpe_test_openclose_itfs(void) */

void mpe_test_read_sim(void)
{
    RUN_FILE_TEST(SIM, mpe_test_read, mpe_test_read_sim);
} /* end mpe_test_read_sim(void) */

void mpe_test_read_bug(void)
{
    RUN_FILE_TEST(SNFS, mpe_test_bug, mpe_test_bug);
} /* mpe_test_read_bug() */

void mpe_test_read_hiroyo_bfs(void)
{
    RUN_FILE_TEST(BFS, mpe_test_read_hiroyo, mpe_test_read_hiroyo_bfs);
} /* end mpe_test_read_hiroyo_bfs(void) */

void mpe_test_read_bfs(void)
{
    RUN_FILE_TEST(BFS, mpe_test_read, mpe_test_read_bfs);
} /* end mpe_test_read_bfs(void) */

void mpe_test_read_romfs(void)
{
    RUN_FILE_TEST(ROMFS, mpe_test_read, mpe_test_read_romfs);
} /* end mpe_test_read_romfs(void) */

void mpe_test_read_snfs(void)
{
    RUN_FILE_TEST(SNFS, mpe_test_read, mpe_test_read_snfs);
} /* end mpe_test_read_snfs(void) */

void mpe_test_read_itfs(void)
{
    RUN_FILE_TEST(ITFS, mpe_test_read, mpe_test_read_itfs);
} /* end mpe_test_read_itfs(void) */

void mpe_test_seek_sim(void)
{
    RUN_FILE_TEST(SIM, mpe_test_seek, mpe_test_seek_sim);
} /* end mpe_test_seek_sim(void) */

void mpe_test_seek_snfs(void)
{
    RUN_FILE_TEST(SNFS, mpe_test_seek, mpe_test_seek_snfs);
} /* end mpe_test_seek_snfs(void) */

void mpe_test_seek_romfs(void)
{
    RUN_FILE_TEST(ROMFS, mpe_test_seek, mpe_test_seek_romfs);
} /* end mpe_test_seek_romfs(void) */
void mpe_test_seek_bfs(void)
{
    RUN_FILE_TEST(BFS, mpe_test_seek, mpe_test_seek_bfs);
} /* end mpe_test_seek_bfs(void) */

void mpe_test_seek_itfs(void)
{
    RUN_FILE_TEST(ITFS, mpe_test_seek, mpe_test_seek_itfs);
} /* end mpe_test_seek_itfs(void) */

void mpe_test_stat_sim(void)
{
    RUN_FILE_TEST(SIM, mpe_test_stat, mpe_test_stat_sim);
} /* end mpe_test_stat_sim(void) */
void mpe_test_stat_snfs(void)
{
    RUN_FILE_TEST(SNFS, mpe_test_stat, mpe_test_stat_snfs);
} /* end mpe_test_stat_snfs(void) */

void mpe_test_stat_romfs(void)
{
    RUN_FILE_TEST(ROMFS, mpe_test_stat, mpe_test_stat_romfs);
} /* end mpe_test_stat_romfs(void) */
void mpe_test_stat_bfs(void)
{
    RUN_FILE_TEST(BFS, mpe_test_stat, mpe_test_stat_bfs);
} /* end mpe_test_stat_bfs(void) */

void mpe_test_stat_itfs(void)
{
    RUN_FILE_TEST(ITFS, mpe_test_stat, mpe_test_stat_itfs);
} /* end mpe_test_stat_itfs(void) */

void mpe_test_write_sim(void)
{
    RUN_FILE_TEST(SIM, mpe_test_write, mpe_test_write_sim);
} /* end mpe_test_write_sim(void) */

void mpe_test_write_snfs(void)
{
    RUN_FILE_TEST(SNFS, mpe_test_write, mpe_test_write_snfs);
} /* end mpe_test_write_snfs(void) */

void mpe_test_write_itfs(void)
{
    RUN_FILE_TEST(ITFS, mpe_test_write, mpe_test_write_itfs);
} /* end mpe_test_write_itfs(void) */

void mpe_test_dir_openclose_sim(void)
{
    RUN_FILE_TEST(SIM, mpe_test_dir_openclose, mpe_test_dir_openclose_sim);
} /* end mpe_test_dir_openclose_sim(void) */

void mpe_test_dir_openclose_snfs(void)
{
    RUN_FILE_TEST(SNFS, mpe_test_dir_openclose, mpe_test_dir_openclose_snfs);
} /* end mpe_test_dir_openclose_snfs(void) */

void mpe_test_dir_openclose_romfs(void)
{
    RUN_FILE_TEST(ROMFS, mpe_test_dir_openclose, mpe_test_dir_openclose_romfs);
} /* end mpe_test_dir_openclose_romfs(void) */

void mpe_test_dir_openclose_itfs(void)
{
    RUN_FILE_TEST(ITFS, mpe_test_dir_openclose, mpe_test_dir_openclose_itfs);
} /* end mpe_test_dir_openclose_itfs(void) */

void mpe_test_dirread_sim(void)
{
    RUN_FILE_TEST(SIM, mpe_test_dirread, mpe_test_dirread_sim);
} /* end mpe_test_dirread_sim(void) */

void mpe_test_dirread_snfs(void)
{
    RUN_FILE_TEST(SNFS, mpe_test_dirread, mpe_test_dirread_snfs);
} /* end mpe_test_dirread_snfs(void) */

void mpe_test_dirread_romfs(void)
{
    RUN_FILE_TEST(ROMFS, mpe_test_dirread, mpe_test_dirread_romfs);
} /* end mpe_test_dirread_romfs(void) */

void mpe_test_dirread_itfs(void)
{
    RUN_FILE_TEST(ITFS, mpe_test_dirread, mpe_test_dirread_itfs);
} /* end mpe_test_dirread_itfs(void) */

void mpe_test_loadunload_bfs(void)
{
    RUN_FILE_TEST(BFS, mpe_test_loadunload, mpe_test_loadunload_bfs);
} /* end mpe_test_loadunload_bfs(void) */

void test_mpe_recurseDir_romfs(void)
{
    RUN_FILE_TEST(ROMFS, test_mpe_recurseDir, test_mpe_recurseDir_romfs);
} /* end mpe_test_recurseDir_romfs(void) */

void test_mpe_recurseDir_snfs(void)
{
    RUN_FILE_TEST(SNFS, test_mpe_recurseDir, test_mpe_recurseDir_snfs);
} /* end mpe_test_recurseDir_romfs(void) */

void test_mpe_recurseDir_sim(void)
{
    RUN_FILE_TEST(SIM, test_mpe_recurseDir, test_mpe_recurseDir_sim);
} /* end mpe_test_recurseDir_romfs(void) */

void test_mpe_recurseDir_itfs(void)
{
    RUN_FILE_TEST(ITFS, test_mpe_recurseDir, test_mpe_recurseDir_itfs);
} /* end mpe_test_recurseDir_itfs(void) */

void test_mpe_fileChange_bfs(void)
{
    setFileSystem( BFS);
    RUN_FILE_TEST(BFS, test_mpe_fileChange, test_mpe_fileChange_bfs);
}

void test_mpe_odn(void)
{
    setFileSystem( BFS);
    RUN_FILE_TEST(BFS, test_mpe_odn_bfs, test_mpe_odn_bfs);
}
