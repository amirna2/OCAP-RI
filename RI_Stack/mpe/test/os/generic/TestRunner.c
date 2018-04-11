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

#include <stdarg.h>
#include <mpetest_dbg.h>
#include <test_sys.h>
#include <test_net.h>
#include <mpe_error.h>
#include <mpeos_file.h>

/*
 #include <mpeos_dbg.h>
 #include <dbgmgr.h>
 */

void **mpe_ftable; /* Dummy reference to resolve bogus dependency. */

/*  Functions defined in this source file  */

void TestRunner(void);
mpe_Bool vte_agent_Log(const char* format, ...);

/* External functions used in this source file  */

extern void mpeos_envInit(void);
extern void mpeos_dbgInit(void);
extern void mpeos_dbgLogControlInit(void);

extern void test_sysRunFpTests(void);

/**
 * TestRunner
 *
 * This is the entry point from main.c.  This function provides a
 * simple compile-time selection of test cases to execute.
 *
 */
void TestRunner(void)
{
    const char *errmsg = NULL;

    vte_agent_Log(
            "\n\n##########\n##########\n#\n#   Into 'TestRunner()'\n#\n#\n");

    /*
     *  Init logging system.
     *
     *  WARNING : Don't try to use logging system before it's init'ed!
     */

    mpeos_envInit();
    mpeos_dbgInit();
    mpeos_dbgLogControlInit();

    /*
     *  The following controls logging when running tests. Adjust the "TEST"
     *  logging level to get the desired amount of test log output. INFO
     *  and FATAL should always be enabled for testing. Also enable DEBUG and
     *  TRACE1 if you want to see all possible output.
     */

    /* Start by turning off all TEST logging, so we start from a known place  */

    errmsg = mpeos_dbgLogControlOpSysIntf("TEST", "NONE");
    if (errmsg == NULL)
    {
        vte_agent_Log("'mpeos_dbgLogControlOpSysIntf()' returned NULL");
    }
    else if (!strcmp(errmsg, " OK"))
    {
        vte_agent_Log("'mpeos_dbgLogControlOpSysIntf()' returned '%s'\n",
                errmsg);
    }

    /*
     *  Turn on desired test logging. For normal test runs use "INFO FATAL"
     *  When doing test development use "INFO FATAL DEBUG TRACE1" or
     *  "ALL DEBUG TRACE" to see all test output
     */

    errmsg = mpeos_dbgLogControlOpSysIntf("TEST", "INFO FATAL TRACE1 TRACE5");
    //    errmsg = mpeos_dbgLogControlOpSysIntf("TEST", "FATAL DEBUG INFO TRACE1 TRACE2 TRACE3 TRACE4 TRACE5 TRACE6 TRACE7 TRACE8 TRACE9");
    if (errmsg == NULL)
    {
        vte_agent_Log("'mpeos_dbgLogControlOpSysIntf()' returned NULL");
    }
    else if (!strcmp(errmsg, " OK"))
    {
        vte_agent_Log("'mpeos_dbgLogControlOpSysIntf()' returned '%s'\n",
                errmsg);
    }
    else
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_TEST,
                "\n'TestRunner()' - Logging enabled :\n");
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
    /*
     *  The following print the test logging levels. Comment out if you
     *  don't want to see the messages.
     *
     */

    MPEOS_LOG(MPE_LOG_FATAL, MPE_MOD_TEST,
            "  MPE_MOD_TEST - MPE_LOG_FATAL enabled\n");
    MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_TEST,
            "  MPE_MOD_TEST - MPE_LOG_ERROR enabled\n");
    MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_TEST,
            "  MPE_MOD_TEST - MPE_LOG_WARN enabled\n");
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_TEST,
            "  MPE_MOD_TEST - MPE_LOG_INFO enabled\n");
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  MPE_MOD_TEST - MPE_LOG_DEBUG enabled\n");
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_TEST,
            "  MPE_MOD_TEST - MPE_LOG_TRACE1 enabled\n");
    MPEOS_LOG(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  MPE_MOD_TEST - MPE_LOG_TRACE2 enabled\n");
    MPEOS_LOG(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "  MPE_MOD_TEST - MPE_LOG_TRACE3 enabled\n");
    MPEOS_LOG(MPE_LOG_TRACE4, MPE_MOD_TEST,
            "  MPE_MOD_TEST - MPE_LOG_TRACE4 enabled\n");
    MPEOS_LOG(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "  MPE_MOD_TEST - MPE_LOG_TRACE5 enabled\n");
    MPEOS_LOG(MPE_LOG_TRACE6, MPE_MOD_TEST,
            "  MPE_MOD_TEST - MPE_LOG_TRACE6 enabled\n");
    MPEOS_LOG(MPE_LOG_TRACE7, MPE_MOD_TEST,
            "  MPE_MOD_TEST - MPE_LOG_TRACE7 enabled\n");
    MPEOS_LOG(MPE_LOG_TRACE8, MPE_MOD_TEST,
            "  MPE_MOD_TEST - MPE_LOG_TRACE8 enabled\n");
    MPEOS_LOG(MPE_LOG_TRACE9, MPE_MOD_TEST,
            "  MPE_MOD_TEST - MPE_LOG_TRACE9 enabled\n");

    /*
     *  If you enable logging for other modules and want to know if it's
     *  really working, you can add similar calls for the other modules here :
     *
     *
     *    MPEOS_LOG (MPE_LOG_FATAL,  MPE_MOD_MEDIA, "MPE_MOD_MEDIA - MPE_LOG_FATAL enabled\n");
     *    MPEOS_LOG (MPE_LOG_ERROR,  MPE_MOD_MEDIA, "MPE_MOD_MEDIA - MPE_LOG_ERROR enabled\n");
     *    MPEOS_LOG (MPE_LOG_WARN,   MPE_MOD_MEDIA, "MPE_MOD_MEDIA - MPE_LOG_WARN enabled\n");
     *    MPEOS_LOG (MPE_LOG_INFO,   MPE_MOD_MEDIA, "MPE_MOD_MEDIA - MPE_LOG_INFO enabled\n");
     *    MPEOS_LOG (MPE_LOG_DEBUG,  MPE_MOD_MEDIA, "MPE_MOD_MEDIA - MPE_LOG_DEBUG enabled\n");
     */

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n  'TestRunner()' - launching tests\n\n");

    test_netRunAllTests();
    test_sysRunAllTests(); /* execute all of the system tests  */

    //    test_sysRunFpTests();

    /*
     * To execute just an individual group of the system tests use
     * one of the following functions:
     *

     test_sysRunTimeTests();
     test_sysRunEventTests();
     test_sysRunThreadTests();
     test_sysRunMemTests();
     test_sysRunSyncTests();
     test_sysRunUtilTests();

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

     test_sysRunEventPingPongTest();
     test_sysRunEventQueueDeleteTest();
     test_sysRunEventQueueNewTest();
     test_sysRunEventQueueNextTest();
     test_sysRunEventQueueSendTest();
     test_sysRunEventQueueWaitNextTest();
     test_sysRunEventSendThreadTest();

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
     test_sysRunUtilStbSetAcOutletStateTest();

     */

    /*
     * To execute just an individual group of the network tests use
     * one of the following functions:
     *

     test_netRunLookupTests();
     test_netRunConnectTests();
     test_netRunOptionsTests();
     test_netRunSelectTests();
     test_mpe_netReadWriteTests();

     */

    MPEOS_LOG(
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

mpe_Bool vte_agent_Log(const char* format, ...)
{
    va_list arg;
    va_start(arg, format);
    vprintf(format, arg);
    va_end(arg);
    return TRUE;
}

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
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_TEST, "\nfileRomfsName() - '%s'\n", name);
}

static mpe_FileError fake_mpe_fileRomfsName(const char* name)
{
    MPEOS_LOG(MPE_LOG_TRACE3, MPE_MOD_TEST, "\nfileRomfsName() - '%s'\n", name);
}

static mpe_FileError fake_mpe_fileRomfsOpen(const char* name,
        mpe_FileOpenMode openMode, mpe_File* returnHandle)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_TEST, "\nfileRomfsOpen() - '%s'\n", name);
}

static mpe_FileError fake_mpe_fileRomfs_handle(mpe_File handle)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_TEST, "\nfake_mpe_fileRomfs_handle()\n");
}

static mpe_FileError fake_mpe_fileRomfs_RW(mpe_File handle, uint32_t* count,
        void* buffer)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_TEST, "\nfake_mpe_fileRomfs_RW()\n");
}

static mpe_FileError fake_mpe_fileRomfs_seek(mpe_File handle,
        mpe_FileSeekMode seekMode, int64_t* offset)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_TEST, "\nfake_mpe_fileRomfs_seek()\n");
}

static mpe_FileError fake_mpe_fileRomfs_stat(const char* fileName,
        mpe_FileStatMode mode, mpe_FileInfo *info)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_TEST, "\nfake_mpe_fileRomfs_stat()\n");
}

static mpe_FileError fake_mpe_fileRomfs_fstat(mpe_File handle,
        mpe_FileStatMode mode, mpe_FileInfo *info)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_TEST, "\nfake_mpe_fileRomfs_fstat()\n");
}

static mpe_FileError fake_mpe_fileRomfsReName(const char* oldName,
        const char* newName)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_TEST, "\nfileRomfsReName() - '%s'\n",
            oldName);
}

static mpe_FileError fake_mpe_fileRomfs_dopen(const char* name,
        mpe_Dir* returnHandle)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_TEST, "\nfileRomfsDirOpen() - '%s'\n", name);
}

static mpe_FileError fake_mpe_fileRomfs_dread(mpe_Dir handle,
        mpe_DirEntry* dirEnt)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_TEST, "\nfileRomfsDirRead()\n");
}

static mpe_FileError fake_mpe_fileRomfs_dclose(mpe_Dir handle)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_TEST, "\nfileRomfsDirClose()\n");
}

static mpe_FileError fake_mpe_fileRomfs_dirMount(const mpe_DirUrl *dirUrl)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_TEST, "\nfileRomfsDirMount()\n");
}

static mpe_FileError fake_mpe_fileRomfs_dirUStat(const mpe_DirUrl *dirUrl,
        mpe_DirStatMode mode, mpe_DirInfo *info)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_TEST, "\nfileRomfsDirUStat()\n");
}

