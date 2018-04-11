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

extern "C"
{
#include <test_sys.h>
#include <vte_agent.h>

/*  Functions defined in this source file  */
NATIVEEXPORT_API void TestRunner(void);
}

#include <stdarg.h>
#include <mpetest_dbg.h>
#include <mpe_error.h>
#include <mpeos_file.h>
#include <mpeTest.h>
#include <MpeDllInterface.h>
#include <WinTVExt.h>

extern "C"
{
/* External test functions called from this source file  */
extern void test_sysRunAllTests(void);
extern void test_sysRunMemTests(void);
extern void test_sysRunThreadTests(void);
extern void test_sysRunTimeTests(void);
extern void test_sysRunSyncTests(void);
extern void test_sysRunEventTests(void);
extern void test_sysRunUtilTests(void);
extern void test_sysRunDbgTests(void);
extern void test_sysRunStressTests(void);

extern void test_sysRunTimeGetTest(void);
extern void test_sysRunTimeToDateTest(void);
extern void test_sysRunTimeClockTest(void);
extern void test_sysRunTimeClockTicksTest(void);
extern void test_sysRunTimeClockToMillisTest(void);
extern void test_sysRunTimeClockToTimeTest(void);
extern void test_sysRunTimeMillisToClockTest(void);
extern void test_sysRunTimeTmToTimeTest(void);

extern void test_sysRunEventQueueNewTest(void);
extern void test_sysRunEventQueueSendTest(void);
extern void test_sysRunEventQueueThreadSendTest(void);
extern void test_sysRunEventQueueThreadReceiveTest(void);
extern void test_sysRunEventPingPongTest(void);
extern void test_sysRunEventQueueNextTest(void);

extern void test_sysRunMemAllocFreeTest(void);
extern void test_sysRunMemReallocFreeTest(void);
extern void test_sysRunMemGetFreeSizeTest(void);
extern void test_sysRunMemLargestFreeTest(void);
extern void test_sysRunMemAllocP(void);
extern void test_sysRunMemFreeP(void);
extern void test_sysRunMemReallocP(void);
extern void test_sysRunMemAllocH(void);
extern void test_sysRunMemGetFreeSize(void);
extern void test_sysRunMemGetLargestFree(void);

extern void test_sysRunSyncMutexNewTest(void);
extern void test_sysRunSyncMutexAcquireTest(void);
extern void test_sysRunSyncMutexAcquierTryTest(void);
extern void test_sysRunSyncCondNewTest(void);
extern void test_sysRunSyncCondGetSetTest(void);
extern void test_sysRunSyncCondPingPongTest(void);
extern void test_sysRunSyncCondWaitForTest(void);

extern void test_gfxRunScreen(void);
extern void test_gfx_SmokeTest(void);
extern void test_mpeos_gfxRunAllTests();

extern void test_mediaRunTunerTests(void);
extern void test_mediaRunMiscTests(void);
extern void test_mediaRunAllTests(void);
extern void test_mediaRunDecoderTests(void);
extern void test_mediaRunNegTests(void);

extern void test_dvr_RunAllTests(void);
extern void test_dvr_RunRecordingTest1(void);
extern void test_dvr_RunRecordingTest2(void);
extern void test_dvr_RunRecordingTest3(void);
extern void test_dvr_RunRecordingTest4(void);
extern void test_dvr_RunRecordingTest5(void);
extern void test_dvr_RunRecordingTest6(void);
extern void test_dvr_RunRecordingTest7(void);
extern void test_dvr_RunRecordingTest8(void);
extern void test_dvr_RunTSBTests(void);
extern void test_dvr_RunTSBTest1(void);
extern void test_dvr_RunTSBTest2(void);
extern void test_dvr_RunTSBTest3(void);
extern void test_dvr_RunTSBTest4(void);

extern void test_netRunAllTests(void);
extern void test_netRunLookupTests(void);

extern void test_sysRunFpTests(void);

extern void test_gfxFillPolygon(void);
extern void test_gfxRunRGB2COLORTest(void);
extern CuSuite* getTestSuite_gfxColor(void);
}

/**
 * TestRunner
 *
 * This is the entry point from main.c.  This function provides a
 * simple compile-time selection of test cases to execute.
 *
 */
NATIVEEXPORT_API void TestRunner(void)
{
    const char *errmsg = NULL;

    // vte_agent_Log("\n\n##########\n##########\n#\n#   Into 'TestRunner()'\n#\n#\n");

    /*
     *  The following controls logging when running tests. Adjust the "TEST"
     *  logging level to get the desired amount of test log output. INFO
     *  and FATAL should always be enabled for testing. Also enable DEBUG and
     *  TRACE1 if you want to see all possible output.
     */

#ifdef TEST_MPEOS
    /* Start by turning off all TEST logging, so we start from a known place  */
    errmsg = mpeos_dbgLogControlOpSysIntf("TEST", "NONE");
    if (errmsg == NULL)
    {
        vte_agent_Log("'mpeos_dbgLogControlOpSysIntf()' returned NULL");
    }
    else if (!strcmp (errmsg, " OK"))
    {
        vte_agent_Log("'mpeos_dbgLogControlOpSysIntf()' returned '%s'\n", errmsg);
    }

    /*
     *  Turn on desired test logging. For normal test runs use "INFO FATAL"
     *  When doing test development use "INFO FATAL DEBUG TRACE1" or
     *  "ALL DEBUG TRACE" to see all test output
     */

    errmsg = mpeos_dbgLogControlOpSysIntf("TEST", "INFO FATAL DEBUG TRACE1 TRACE2 TRACE3 TRACE5");
    //    errmsg = mpeos_dbgLogControlOpSysIntf("TEST", "FATAL DEBUG INFO TRACE1 TRACE2 TRACE3 TRACE4 TRACE5 TRACE6 TRACE7 TRACE8 TRACE9");
    if (errmsg == NULL)
    {
        vte_agent_Log("'mpeos_dbgLogControlOpSysIntf()' returned NULL");
    }
    else if (!strcmp (errmsg, " OK"))
    {
        vte_agent_Log("'mpeos_dbgLogControlOpSysIntf()' returned '%s'\n", errmsg);
    }
    else
    {
        TRACE (MPE_LOG_INFO, MPE_MOD_TEST, "\n'TestRunner()' - Logging enabled :\n");
    }

    /*
     *  For debugging you can enable logging here for other modules, for example :
     *
     *     errmsg = mpeos_dbgLogControlOpSysIntf("MEDIA", "FATAL ERROR WARN INFO DEBUG");
     */

    /*

     mpeos_dbgLogControlOpSysIntf("GFX", "ALL DEBUG TRACE");

     mpeos_dbgLogControlOpSysIntf("DIRECTFB", "ALL DEBUG TRACE");

     mpeos_dbgLogControlOpSysIntf("DISP", "ALL DEBUG TRACE");

     */
#endif

    /*
     *  The following prints the test logging levels. Comment out if you
     *  don't want to see the messages.
     *
     */

    /*  TRACE (MPE_LOG_FATAL,  MPE_MOD_TEST, "  MPE_MOD_TEST - MPE_LOG_FATAL enabled\n");
     TRACE (MPE_LOG_ERROR,  MPE_MOD_TEST, "  MPE_MOD_TEST - MPE_LOG_ERROR enabled\n");
     TRACE (MPE_LOG_WARN,   MPE_MOD_TEST, "  MPE_MOD_TEST - MPE_LOG_WARN enabled\n");
     TRACE (MPE_LOG_INFO,   MPE_MOD_TEST, "  MPE_MOD_TEST - MPE_LOG_INFO enabled\n");
     TRACE (MPE_LOG_DEBUG,  MPE_MOD_TEST, "  MPE_MOD_TEST - MPE_LOG_DEBUG enabled\n");
     TRACE (MPE_LOG_TRACE1, MPE_MOD_TEST, "  MPE_MOD_TEST - MPE_LOG_TRACE1 enabled\n");
     TRACE (MPE_LOG_TRACE2, MPE_MOD_TEST, "  MPE_MOD_TEST - MPE_LOG_TRACE2 enabled\n");
     TRACE (MPE_LOG_TRACE3, MPE_MOD_TEST, "  MPE_MOD_TEST - MPE_LOG_TRACE3 enabled\n");
     TRACE (MPE_LOG_TRACE4, MPE_MOD_TEST, "  MPE_MOD_TEST - MPE_LOG_TRACE4 enabled\n");
     TRACE (MPE_LOG_TRACE5, MPE_MOD_TEST, "  MPE_MOD_TEST - MPE_LOG_TRACE5 enabled\n");
     TRACE (MPE_LOG_TRACE6, MPE_MOD_TEST, "  MPE_MOD_TEST - MPE_LOG_TRACE6 enabled\n");
     TRACE (MPE_LOG_TRACE7, MPE_MOD_TEST, "  MPE_MOD_TEST - MPE_LOG_TRACE7 enabled\n");
     TRACE (MPE_LOG_TRACE8, MPE_MOD_TEST, "  MPE_MOD_TEST - MPE_LOG_TRACE8 enabled\n");
     TRACE (MPE_LOG_TRACE9, MPE_MOD_TEST, "  MPE_MOD_TEST - MPE_LOG_TRACE9 enabled\n");
     */
    /*
     *  If you enable logging for other modules and want to know if it's
     *  really working, you can add similar calls for the other modules here :
     *
     *
     *    TRACE (MPE_LOG_FATAL,  MPE_MOD_MEDIA, "MPE_MOD_MEDIA - MPE_LOG_FATAL enabled\n");
     *    TRACE (MPE_LOG_ERROR,  MPE_MOD_MEDIA, "MPE_MOD_MEDIA - MPE_LOG_ERROR enabled\n");
     *    TRACE (MPE_LOG_WARN,   MPE_MOD_MEDIA, "MPE_MOD_MEDIA - MPE_LOG_WARN enabled\n");
     *    TRACE (MPE_LOG_INFO,   MPE_MOD_MEDIA, "MPE_MOD_MEDIA - MPE_LOG_INFO enabled\n");
     *    TRACE (MPE_LOG_DEBUG,  MPE_MOD_MEDIA, "MPE_MOD_MEDIA - MPE_LOG_DEBUG enabled\n");
     */

    /*
     The following lists of tests should be edited to include tests you wish excuted.

     If a test is not to be run, list it in a trace statement, such as:
     TRACE(MPE_LOG_INFO,MPE_MOD_TEST, "[TEST_RESULT],NOT_RUN, test_sysNotRun()\n");
     To include a suite header in the Excel generated output include a TRACE statement like:
     TRACE(MPE_LOG_INFO,MPE_MOD_TEST, "[TEST_RESULT], ,SUITE: test_sysRunMemTests\n");
     */

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "[TEST_RESULT], ,SUITE: test_sysRunMemTests\n");
    test_sysRunMemTests();

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "[TEST_RESULT], ,SUITE:  test_sysRunTimeTests\n");
    test_sysRunTimeTests();

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "[TEST_RESULT], ,SUITE:  Thread Tests\n");
    test_sysRunThreadAttachTest();
    test_sysRunThreadCreateTest();
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "[TEST_RESULT],NOT_RUN, test_sysRunThreadDestroyID0Test()\n");
    test_sysRunThreadDestroyIDTest();
    test_sysRunThreadDestroyOtherTest();
    test_sysRunThreadGetCurrentTest();
    test_sysRunThreadGetDataTest();
    test_sysRunThreadGetStatusTest();
    test_sysRunThreadSetDataTest();
    test_sysRunThreadSetPriorityTest();
    test_sysRunThreadSetStatusTest();
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "[TEST_RESULT],NOT_RUN, test_sysRunThreadSleepTest()\n");
    test_sysRunThreadYieldTest();
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "[TEST_RESULT],NOT_RUN, test_sysRunThreadDataTest()\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "[TEST_RESULT],NOT_RUN, test_sysRunThreadStatusTest()\n");

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "[TEST_RESULT], ,SUITE:  Cond and Mutex\n");
    test_sysRunSyncCondNewTest();
    test_sysRunSyncCondGetSetTest();
    test_sysRunSyncCondWaitForTest();
    test_sysRunSyncCondPingPongTest();

    test_sysRunSyncMutexNewTest();
    test_sysRunSyncMutexAcquireTest();
    test_sysRunSyncMutexAcquierTryTest();

    test_sysRunEventQueueNewTest();
    test_sysRunEventQueueDeleteTest();
    test_sysRunEventQueueNextTest();
    test_sysRunEventQueueSendTest();
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "[TEST_RESULT],NOT_RUN, test_sysRunEventQueueWaitNextTest()\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "[TEST_RESULT],NOT_RUN, test_sysRunEventQueueSendThreadTest()\n");
    test_sysRunEventPingPongTest();

    test_sysRunMathTests();

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "[TEST_RESULT],NOT_RUN, test_dbgMsg5()\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "[TEST_RESULT],NOT_RUN, test_dbgMsg67()\n");

    test_mediaRunMiscTests();
    test_mediaRunTunerTests();
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "[TEST_RESULT],NOT_RUN, test_mediaRunDecoderTests()\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "[TEST_RESULT], NOT_RUN, test_mediaRunNegTests()\n");

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "[TEST_RESULT], NOT_RUN, test_video_one\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "[TEST_RESULT], NOT_RUN, test_video_two\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "[TEST_RESULT], NOT_RUN, test_video_three\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "[TEST_RESULT], NOT_RUN, test_video_four\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "[TEST_RESULT], NOT_RUN, test_video_five\n");

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "[TEST_RESULT], ,SUITE:  Sockets\n");
    test_netRunAllTests();
    test_netRunLookupTests();

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "[TEST_RESULT], ,SUITE:  DVR\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "[TEST_RESULT], NOT_RUN, test_dvr_RunAllTests()\n");

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "[TEST_RESULT], ,SUITE:  Closed Captioning\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "[TEST_RESULT], NOT_RUN, test_cc_RunAllTests()\n");

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "[TEST_RESULT], ,SUITE:  SI\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "[TEST_RESULT], NOT_RUN, test_simgr_sidb_extract_all()\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "[TEST_RESULT], NOT_RUN, test_si_neg_getServiceId()\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "[TEST_RESULT], NOT_RUN, test_si_neg_getComponent()\n");

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "[TEST_RESULT], ,SUITE:  Utilities\n");
    test_sysRunUtilEnvGet();
    test_sysRunUtilEnvInit();
    test_sysRunUtilEnvSet();
    test_sysRunUtilIniSetPath();
    test_sysRunUtilLongJmp();
    test_sysRunUtilRegisterForPowerKey();
    test_sysRunUtilSetJmp();
    test_sysRunUtilStbBoot();
    test_sysRunUtilStbBootStatus();
    test_sysRunUtilStbGetAcOutletState();
    test_sysRunUtilStbGetPowerStatus();
    test_sysRunUtilStbGetRootCerts();
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "[TEST_RESULT], NOT_RUN, test_sysRunUtilStbIsHdCapable()\n");
    test_sysRunUtilStbSetAcOutletStateTest();

    /*
     Below are references to suites you may want to include.  There are suites to run all tests, all tests
     for a specific API, or an individual test.
     */

    //    test_netRunAllTests();
    //    test_sysRunAllTests();        /* execute all of the system tests  */
    //    test_mpeos_gfxRunAllTests();

    /*
     * To execute just an individual group of the media tests use
     * one of the following functions:
     */
    //   test_mediaRunMiscTests();
    //	 test_mediaRunTunerTests();
    //   test_mediaRunDecoderTests();
    //   test_mediaRunNegTests();

    /*
     * To execute just an individual group of the dvrtests use
     * one of the following functions:
     */
    //    test_dvr_RunAllTests( );
    //    test_dvr_RunRecordingTest1( );
    //    test_dvr_RunRecordingTest2( );
    //    test_dvr_RunRecordingTest3( );
    //    test_dvr_RunRecordingTest4( );
    //    test_dvr_RunRecordingTest5( );
    //    test_dvr_RunRecordingTest6( );
    //    test_dvr_RunRecordingTest7( );
    //    test_dvr_RunRecordingTest8( );
    //    test_dvr_RunTSBTests( );
    //    test_dvr_RunTSBTest1( );
    //    test_dvr_RunTSBTest2( );
    //    test_dvr_RunTSBTest3( );
    //    test_dvr_RunTSBTest4( );

    /*
     * To execute just an individual group of the graphics tests use
     * one of the following functions:
     */

    //    test_gfx_SmokeTest();
    //    test_gfxRunSurfaceTests();
    //    test_gfxRunDrawTests();

    /*
     * To execute just an individual group of the system tests use
     * one of the following functions:
     */

    /* to run suites
     test_sysRunSyncTests();
     test_sysRunMemTests();
     test_sysRunEventTests();
     test_sysRunMathTests();
     test_sysRunThreadTests(); 
     test_sysRunDbgTests();
     test_sysRunStressTests();
     */

    /*
     * To execute an individual test within one of the system test 
     * groups use one of the following functions:
     *

     test_sysRunTimeGetTest();
     test_sysRunTimeGetMillisTest();
     test_sysRunTimeToDateTest();
     test_sysRunTimeClockTest();
     test_sysRunTimeClockTicksTest();
     test_sysRunTimeClockToMillisTest();
     test_sysRunTimeClockToTimeTest();
     test_sysRunTimeMillisToClockTest();
     test_sysRunTimeSystemClockTest();
     test_sysRunTimeTimeToClockTest();
     test_sysRunTimeTmToTimeTest();

     test_sysRunEventQueueNewTest();
     test_sysRunEventQueueDeleteTest();
     test_sysRunEventQueueNextTest();
     test_sysRunEventQueueSendTest();
     test_sysRunEventQueueWaitNextTest();
     test_sysRunEventQueueSendThreadTest();
     test_sysRunEventPingPongTest();

     test_sysRunThreadAttachTest();
     test_sysRunThreadCreateTest();
     test_sysRunThreadDestroyID0Test();
     test_sysRunThreadDestroyIDTest();
     test_sysRunThreadDestroyOtherTest();
     test_sysRunThreadGetCurrentTest();
     test_sysRunThreadGetDataTest();
     test_sysRunThreadGetStatusTest();
     test_sysRunThreadSetDataTest();
     test_sysRunThreadSetPriorityTest();
     test_sysRunThreadSetStatusTest();
     test_sysRunThreadSleepTest();
     test_sysRunThreadYieldTest();
     test_sysRunThreadDataTest();
     test_sysRunThreadStatusTest();

     test_sysRunMemAllocPTest();
     test_sysRunMemFreePTest();
     test_sysRunMemReallocPTest();
     test_sysRunMemAllocHTest();
     test_sysRunMemFreeHTest();
     test_sysRunMemLockHTest();
     test_sysRunMemReallocHTest();
     test_sysRunMemCompactTest();
     test_sysRunMemGetFreeSizeTest();
     test_sysRunMemGetLargestFreeTest();
     test_sysRunMemGetStatsTest();
     test_sysRunMemPurgeTest();
     test_sysRunMemStatsTest();

     test_sysRunSyncMutexNewTest();
     test_sysRunSyncMutexAcquireTest();
     test_sysRunSyncMutexAcquierTryTest();
     test_sysRunSyncCondNewTest();
     test_sysRunSyncCondGetSetTest();
     test_sysRunSyncCondPingPongTest();
     test_sysRunSyncCondWaitForTest();
     test_sysRunUtilEnvGet();
     test_sysRunUtilEnvInit();
     test_sysRunUtilEnvSet();
     test_sysRunUtilIniSetPath();
     test_sysRunUtilLongJmp();
     test_sysRunUtilRegisterForPowerKey();
     test_sysRunUtilSetJmp();
     test_sysRunUtilStbBoot();
     test_sysRunUtilStbBootStatus();
     test_sysRunUtilStbGetAcOutletState();
     test_sysRunUtilStbGetPowerStatus();
     test_sysRunUtilStbGetRootCerts();
     test_sysRunUtilStbIsHdCapable();
     test_sysRunUtilStbSetAcOutletStateTest();
     */

    /*    */

    TRACE(
            MPE_LOG_INFO,
            MPE_MOD_TEST,
            "\n\n#\n#\n#   'TestRunner()' is finished.\n#\n################\n################\n\n");

    return;
}

/**
 * vte_agent_Log - Print raw mwssages on console. Used before logging system
 * is inited. Adjust as needed to do raw text output on STB console.
 *
 *   NOTE : Since this does things differently for different STBs, it probably
 *          should live somewhere besides in the generic 'TestRunner'.
 *
 */
/*
 mpe_Bool vte_agent_Log( const char* format, ... )
 {
 va_list arg;
 va_start( arg, format );
 vprintf(format, arg);
 va_end( arg );
 return TRUE;
 }
 */

/****
 *
 *  Fake ROMFS stuff, to allow linking of MPEOS tests
 *
 */

static void fake_mpe_fileRomfsNamev(const char*);
static mpe_FileError fake_mpe_fileRomfsName(const char*);
static mpe_FileError fake_mpe_fileRomfsOpen(const char*, mpe_FileOpenMode,
        mpe_File*);
static mpe_FileError fake_mpe_fileRomfs_handle(mpe_File);
static mpe_FileError fake_mpe_fileRomfs_RW(mpe_File, uint32_t*, void*);
static mpe_FileError fake_mpe_fileRomfs_stat(const char*, mpe_FileStatMode,
        mpe_FileInfo *);
static mpe_FileError fake_mpe_fileRomfs_fstat(mpe_File, mpe_FileStatMode,
        mpe_FileInfo*);
static mpe_FileError fake_mpe_fileRomfs_seek(mpe_File, mpe_FileSeekMode,
        int64_t*);
static mpe_FileError fake_mpe_fileRomfsReName(const char*, const char*);
static mpe_FileError fake_mpe_fileRomfs_dopen(const char*, mpe_Dir*);
static mpe_FileError fake_mpe_fileRomfs_dread(mpe_Dir, mpe_DirEntry*);
static mpe_FileError fake_mpe_fileRomfs_dclose(mpe_Dir);
static mpe_FileError fake_mpe_fileRomfs_dirMount(const mpe_DirUrl*);
static mpe_FileError fake_mpe_fileRomfs_dirUStat(const mpe_DirUrl*,
        mpe_DirStatMode, mpe_DirInfo*);

mpeos_filesys_ftable_t mpe_fileRomfsFTable =
{ fake_mpe_fileRomfsNamev, // mpe_fileRomfsInit, 
        fake_mpe_fileRomfsOpen, // mpe_fileRomfsFileOpen, 
        fake_mpe_fileRomfs_handle, // mpe_fileRomfsFileClose, 
        fake_mpe_fileRomfs_RW, // mpe_fileRomfsFileRead, 
        fake_mpe_fileRomfs_RW, // mpe_fileRomfsFileWrite, 
        fake_mpe_fileRomfs_seek, // mpe_fileRomfsFileSeek, 
        fake_mpe_fileRomfs_handle, // mpe_fileRomfsFileSync, 
        fake_mpe_fileRomfs_stat, // mpe_fileRomfsFileGetStat, 
        fake_mpe_fileRomfs_stat, // mpe_fileRomfsFileSetStat, 
        fake_mpe_fileRomfs_fstat, // mpe_fileRomfsFileGetFStat, 
        fake_mpe_fileRomfs_fstat, // mpe_fileRomfsFileSetFStat, 
        fake_mpe_fileRomfsName, // mpe_fileRomfsFileDelete, 
        fake_mpe_fileRomfsReName, // mpe_fileRomfsFileRename, 
        fake_mpe_fileRomfs_dopen, // mpe_fileRomfsDirOpen, 
        fake_mpe_fileRomfs_dread, // mpe_fileRomfsDirRead, 
        fake_mpe_fileRomfs_dclose, // mpe_fileRomfsDirClose, 
        fake_mpe_fileRomfsName, // mpe_fileRomfsDirDelete, 
        fake_mpe_fileRomfsReName, // mpe_fileRomfsDirRename, 
        fake_mpe_fileRomfsName, // mpe_fileRomfsDirCreate, 
        fake_mpe_fileRomfs_dirMount, // mpe_fileRomfsDirMount, 
        fake_mpe_fileRomfs_dirMount, // mpe_fileRomfsDirUnmount, 
        fake_mpe_fileRomfs_dirUStat, // mpe_fileRomfsDirGetUStat, 
        fake_mpe_fileRomfs_dirUStat, // mpe_fileRomfsDirSetUStat 
        };

static void fake_mpe_fileRomfsNamev(const char* name)
{
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\nfileRomfsName() - '%s'\n", name);
}

static mpe_FileError fake_mpe_fileRomfsName(const char* name)
{
    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST, "\nfileRomfsName() - '%s'\n", name);
    return MPE_FS_ERROR_SUCCESS;
}

static mpe_FileError fake_mpe_fileRomfsOpen(const char* name,
        mpe_FileOpenMode openMode, mpe_File* returnHandle)
{
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\nfileRomfsOpen() - '%s'\n", name);
    return MPE_FS_ERROR_SUCCESS;
}

static mpe_FileError fake_mpe_fileRomfs_handle(mpe_File handle)
{
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\nfake_mpe_fileRomfs_handle()\n");
    return MPE_FS_ERROR_SUCCESS;
}

static mpe_FileError fake_mpe_fileRomfs_RW(mpe_File handle, uint32_t* count,
        void* buffer)
{
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\nfake_mpe_fileRomfs_RW()\n");
    return MPE_FS_ERROR_SUCCESS;
}

static mpe_FileError fake_mpe_fileRomfs_seek(mpe_File handle,
        mpe_FileSeekMode seekMode, int64_t* offset)
{
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\nfake_mpe_fileRomfs_seek()\n");
    return MPE_FS_ERROR_SUCCESS;
}

static mpe_FileError fake_mpe_fileRomfs_stat(const char* fileName,
        mpe_FileStatMode mode, mpe_FileInfo *info)
{
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\nfake_mpe_fileRomfs_stat()\n");
    return MPE_FS_ERROR_SUCCESS;
}

static mpe_FileError fake_mpe_fileRomfs_fstat(mpe_File handle,
        mpe_FileStatMode mode, mpe_FileInfo *info)
{
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\nfake_mpe_fileRomfs_fstat()\n");
    return MPE_FS_ERROR_SUCCESS;
}

static mpe_FileError fake_mpe_fileRomfsReName(const char* oldName,
        const char* newName)
{
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\nfileRomfsReName() - '%s'\n", oldName);
    return MPE_FS_ERROR_SUCCESS;
}

static mpe_FileError fake_mpe_fileRomfs_dopen(const char* name,
        mpe_Dir* returnHandle)
{
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\nfileRomfsDirOpen() - '%s'\n", name);
    return MPE_FS_ERROR_SUCCESS;
}

static mpe_FileError fake_mpe_fileRomfs_dread(mpe_Dir handle,
        mpe_DirEntry* dirEnt)
{
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\nfileRomfsDirRead()\n");
    return MPE_FS_ERROR_SUCCESS;
}

static mpe_FileError fake_mpe_fileRomfs_dclose(mpe_Dir handle)
{
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\nfileRomfsDirClose()\n");
    return MPE_FS_ERROR_SUCCESS;
}

static mpe_FileError fake_mpe_fileRomfs_dirMount(const mpe_DirUrl *dirUrl)
{
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\nfileRomfsDirMount()\n");
    return MPE_FS_ERROR_SUCCESS;
}

static mpe_FileError fake_mpe_fileRomfs_dirUStat(const mpe_DirUrl *dirUrl,
        mpe_DirStatMode mode, mpe_DirInfo *info)
{
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\nfileRomfsDirUStat()\n");
    return MPE_FS_ERROR_SUCCESS;
}
