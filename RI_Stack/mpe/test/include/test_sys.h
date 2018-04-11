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

#ifndef TEST_SYS_H
#define TEST_SYS_H 1

#include <mpeTest.h>
#include <mpetest_sys.h>

#if defined(WIN32)
# include <windows.h>
# include <stdio.h>
# include <time.h>
# include <stdlib.h>
# include <setjmp.h>
# include <winos.h>
# include <process.h>
# ifndef MPE_EPOCH_DELTA
#  define MPE_EPOCH_DELTA 0
# endif  /* MPE_EPOCH_DELTA */
#endif /* WIN32 */

#ifdef TEST_MPEOS
# include <mpeos_dbg.h>
# include <osmgr.h>
#else
# include <mpe_dbg.h>
# include <mpe_os.h>
# include <mpe_sys.h>
#endif /* TEST_MPEOS */

/*
 * This section of macros defines how specific functionality utilized in the
 * general unit test code gets bound to the target system.  This is code that 
 * will be used in all cases when testing the target, but will require
 * redefinition for values appropriate to the target.
 */

#if defined(WIN32)         /* Win32 simulator  */

# define PLATFORM_TIME (time(NULL) + MPE_EPOCH_DELTA)
# define PLATFORM_LOCAL_TIME(t, l)  localtime(&t)

# define PLATFORM_CLOCK clock
# define PLATFORM_CLOCKTICKS CLOCKS_PER_SEC
# define PLATFORM_TICKSPERSEC (CLOCKS_PER_SEC)
# define PLATFORM_CLOCKTOMILLIS(c) \
                    (((uint32_t)(c & 0xFFFFFFFF)) / \
                    ((uint32_t)(mpe_timeClockToMillis(1) & 0xFFFFFFFF)))
# define PLATFORM_CLOCKTOTIME(c) mpe_timeClockToTime(c)

#elif defined(PTV)         /* Scientific Atlanta PowerTV STB */

# define PLATFORM_TIME (pk_Time() + MPE_EPOCH_DELTA)
# define PLATFORM_LOCAL_TIME(t, l)  pk_TimeDate(l, t-MPE_EPOCH_DELTA, -1, -1)
# define PLATFORM_CLOCK clock
# define PLATFORM_CLOCKTICKS pk_ClockSeconds(1)
# define PLATFORM_TICKSPERSEC (CLOCKS_PER_SEC)
# define PLATFORM_CLOCKTOMILLIS(c) \
                    (((uint32_t)(c & 0xFFFFFFFFLL)) / \
                    ((uint32_t)(pk_ClockMilliseconds(1) & 0xFFFFFFFFLL)))
# define PLATFORM_CLOCKTOTIME(c) pk_ClockToTime(c)

#endif /* STB type  */

/*  Test thread state values  */

#define TESTTHREADENTERED        1
#define TESTTHREADAWAKE          2
#define TESTTHREADDEATH          3
#define TESTTHREADERROR          4
#define TESTTHREADFINISHED       5
#define TESTTHREADDESTROYFAILED  6
#define TESTTHREADINVALID        9999999

NATIVEEXPORT_API void test_sysRunAllTests(void);
NATIVEEXPORT_API void test_sysRunTimeTests(void);
NATIVEEXPORT_API void test_sysRunMemTests(void);
NATIVEEXPORT_API void test_sysRunThreadTests(void);
NATIVEEXPORT_API void test_sysRunSyncTests(void);
NATIVEEXPORT_API void test_sysRunEventTests(void);
NATIVEEXPORT_API void test_sysRunUiEventTests(void);
NATIVEEXPORT_API void test_sysRunUtilTests(void);
NATIVEEXPORT_API void test_sysRunDbgTests(void);
NATIVEEXPORT_API void test_sysRunDllTests(void);
NATIVEEXPORT_API void test_sysRunStressTests(void);
NATIVEEXPORT_API void test_sysRunMathTests(void);

NATIVEEXPORT_API void test_sysRunTimeGetTest(void);
NATIVEEXPORT_API void test_sysRunTimeGetMillisTest(void);
NATIVEEXPORT_API void test_sysRunTimeToDateTest(void);
NATIVEEXPORT_API void test_sysRunTimeClockTest(void);
NATIVEEXPORT_API void test_sysRunTimeClockTicksTest(void);
NATIVEEXPORT_API void test_sysRunTimeClockToMillisTest(void);
NATIVEEXPORT_API void test_sysRunTimeClockToTimeTest(void);
NATIVEEXPORT_API void test_sysRunTimeMillisToClockTest(void);
NATIVEEXPORT_API void test_sysRunTimeSystemClockTest(void);
NATIVEEXPORT_API void test_sysRunTimeTimeToClockTest(void);
NATIVEEXPORT_API void test_sysRunTimeTmToTimeTest(void);

NATIVEEXPORT_API void test_sysRunEventPingPongTest(void);
NATIVEEXPORT_API void test_sysRunEventQueueNewTest(void);
NATIVEEXPORT_API void test_sysRunEventQueueSendTest(void);
NATIVEEXPORT_API void test_sysRunEventQueueWaitNextTest(void);
NATIVEEXPORT_API void test_sysRunEventQueueDeleteTest(void);
NATIVEEXPORT_API void test_sysRunEventQueueNextTest(void);
NATIVEEXPORT_API void test_sysRunEventSendThreadTest(void);

NATIVEEXPORT_API void test_sysRunMemAllocHTest(void);
NATIVEEXPORT_API void test_sysRunMemAllocPTest(void);
NATIVEEXPORT_API void test_sysRunMemCompactTest(void);
NATIVEEXPORT_API void test_sysRunMemFreeHTest(void);
NATIVEEXPORT_API void test_sysRunMemFreePTest(void);
NATIVEEXPORT_API void test_sysRunMemGetFreeSizeTest(void);
NATIVEEXPORT_API void test_sysRunMemGetLargestFreeTest(void);
NATIVEEXPORT_API void test_sysRunMemGetStatsTest(void);
NATIVEEXPORT_API void test_sysRunMemLockHTest(void);
NATIVEEXPORT_API void test_sysRunMemPurgeTest(void);
NATIVEEXPORT_API void test_sysRunMemReallocHTest(void);
NATIVEEXPORT_API void test_sysRunMemReallocPTest(void);
NATIVEEXPORT_API void test_sysRunMemStatsTest(void);

NATIVEEXPORT_API void test_sysRunSyncMutexNewTest(void);
NATIVEEXPORT_API void test_sysRunSyncMutexAcquireTest(void);
NATIVEEXPORT_API void test_sysRunSyncMutexAcquierTryTest(void);
NATIVEEXPORT_API void test_sysRunSyncCondNewTest(void);
NATIVEEXPORT_API void test_sysRunSyncCondGetSetTest(void);
NATIVEEXPORT_API void test_sysRunSyncCondPingPongTest(void);
NATIVEEXPORT_API void test_sysRunSyncCondWaitForTest(void);
NATIVEEXPORT_API void test_sysRunSyncCondDeleteTest(void);

NATIVEEXPORT_API void test_sysRunThreadTests(void);
NATIVEEXPORT_API void test_sysRunThreadAttachTest(void);
NATIVEEXPORT_API void test_sysRunThreadCreateTest(void);
NATIVEEXPORT_API void test_sysRunThreadDestroyID0Test(void);
NATIVEEXPORT_API void test_sysRunThreadDestroyIDTest(void);
NATIVEEXPORT_API void test_sysRunThreadDestroyOtherTest(void);
NATIVEEXPORT_API void test_sysRunThreadGetCurrentTest(void);
NATIVEEXPORT_API void test_sysRunThreadGetDataTest(void);
NATIVEEXPORT_API void test_sysRunThreadGetStatusTest(void);
NATIVEEXPORT_API void test_sysRunThreadSetDataTest(void);
NATIVEEXPORT_API void test_sysRunThreadSetPriorityTest(void);
NATIVEEXPORT_API void test_sysRunThreadSetStatusTest(void);
NATIVEEXPORT_API void test_sysRunThreadSleepTest(void);
NATIVEEXPORT_API void test_sysRunThreadYieldTest(void);
NATIVEEXPORT_API void test_sysRunThreadDataTest(void);
NATIVEEXPORT_API void test_sysRunThreadStatusTest(void);

NATIVEEXPORT_API void test_sysRunUtilEnvGet(void);
NATIVEEXPORT_API void test_sysRunUtilEnvInit(void);
NATIVEEXPORT_API void test_sysRunUtilEnvSet(void);
NATIVEEXPORT_API void test_sysRunUtilIniSetPath(void);
NATIVEEXPORT_API void test_sysRunUtilLongJmp(void);
NATIVEEXPORT_API void test_sysRunUtilRegisterForPowerKey(void);
NATIVEEXPORT_API void test_sysRunUtilSetJmp(void);
NATIVEEXPORT_API void test_sysRunUtilStbBoot(void);
NATIVEEXPORT_API void test_sysRunUtilStbBootStatus(void);
NATIVEEXPORT_API void test_sysRunUtilStbGetAcOutletState(void);
NATIVEEXPORT_API void test_sysRunUtilStbGetPowerStatus(void);
NATIVEEXPORT_API void test_sysRunUtilStbGetRootCerts(void);
NATIVEEXPORT_API void test_sysRunUtilStbSetAcOutletStateTest(void);

#endif /* TEST_SYS_H */
