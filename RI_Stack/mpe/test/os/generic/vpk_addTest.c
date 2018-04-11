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

/**
 * GenericAddTest.c
 *
 * This file is autogenerated by scripts, do not modify.
 */
#include <vpk_testHarness.h>
#include "mpeTest.h"
#include "test_sys.h"
#include "test_disp.h"

/**
 * This function should only be called interally from the test harness.
 * Note: all the tests should have the same signiture.
 * @param iTestID The test id you wish to run.
 * @return void*  A function pointer to the function that should be run.
 */
void* vpk_AddTest(TestID iTestID)
{
    /* When tests have different function signiture, update func to match.
     */
    void (*func)(void);

    /*
     */
    switch (iTestID)
    {
    case eTestSysRunAllTests:
        func = test_sysRunAllTests;
    case eTestSysRunTimeTests:
        func = test_sysRunTimeTests;
        break;
    case eTestSysRunMemTests:
        func = test_sysRunMemTests;
        break;
    case eTestSysRunThreadTests:
        func = test_sysRunThreadTests;
        break;
    case eTestSysRunSyncTests:
        func = test_sysRunSyncTests;
        break;
    case eTestSysRunEventTests:
        func = test_sysRunEventTests;
        break;
    case eTestSysRunUtilTests:
        func = test_sysRunUtilTests;
        break;
    case eTestSysRunTimerTests:
        func = test_sysRunTimerTests;
        break;
    case eTestSysRunTimeGetTest:
        func = test_sysRunTimeGetTest;
        break;
    case eTestSysRunTimeToDateTest:
        func = test_sysRunTimeToDateTest;
        break;
    case eTestSysRunTimeClockTest:
        func = test_sysRunTimeClockTest;
        break;
    case eTestSysRunTimeClockTicksTest:
        func = test_sysRunTimeClockTicksTest;
        break;
    case eTestSysRunTimeClockToMillisTest:
        func = test_sysRunTimeClockToMillisTest;
        break;
    case eTestSysRunTimeClockToTimeTest:
        func = test_sysRunTimeClockToTimeTest;
        break;
    case eTestSysRunTimeMillisToClockTest:
        func = test_sysRunTimeMillisToClockTest;
        break;
    case eTestSysRunTimeTmToTimeTest:
        func = test_sysRunTimeTmToTimeTest;
        break;
    case eTestSysRunEventQueueNewTest:
        func = test_sysRunEventQueueNewTest;
        break;
    case eTestSysRunEventQueueSendTest:
        func = test_sysRunEventQueueSendTest;
        break;
    case eTestSysRunEventQueueThreadSendTest:
        func = test_sysRunEventQueueThreadSendTest;
        break;
    case eTestSysRunEventQueueThreadReceiveTest:
        func = test_sysRunEventQueueThreadReceiveTest;
        break;
    case eTestSysRunEventPingPongTest:
        func = test_sysRunEventPingPongTest;
        break;
    case eTestSysRunEventQueueNextTest:
        func = test_sysRunEventQueueNextTest;
        break;
    case eTestSysRunMemAllocFreeTest:
        func = test_sysRunMemAllocFreeTest;
        break;
    case eTestSysRunMemReallocFreeTest:
        func = test_sysRunMemReallocFreeTest;
        break;
    case eTestSysRunMemGetFreeSizeTest:
        func = test_sysRunMemGetFreeSizeTest;
        break;
    case eTestSysRunMemLargestFreeTest:
        func = test_sysRunMemLargestFreeTest;
        break;
    case eTestSysRunSyncMutexNewTest:
        func = test_sysRunSyncMutexNewTest;
        break;
    case eTestSysRunSyncMutexAcquireTest:
        func = test_sysRunSyncMutexAcquireTest;
        break;
    case eTestSysRunSyncMutexAcquierTryTest:
        func = test_sysRunSyncMutexAcquierTryTest;
        break;
    case eTestSysRunSyncCondNewTest:
        func = test_sysRunSyncCondNewTest;
        break;
    case eTestSysRunSyncCondGetSetTest:
        func = test_sysRunSyncCondGetSetTest;
        break;
    case eTestSysRunSyncCondPingPongTest:
        func = test_sysRunSyncCondPingPongTest;
        break;
    case eTestSysRunSyncCondWaitForTest:
        func = test_sysRunSyncCondWaitForTest;
        break;
    case eAllTests:
    default:
        break;
    }

    return (void*) func;
} /* end GenericAddTest(TestID) */