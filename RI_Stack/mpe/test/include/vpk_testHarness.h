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

#ifndef _GENERICTESTHARNESS_H
#define _GENERICTESTHARNESS_H 1
/**
 * File: GenericTestHarness.h
 *
 * Header for generic test runner.  Use this header to include public functions
 * for running the test runner, and using tools for handling the status of the
 * tests.
 *
 * vpk_testID.h is included in this file, and should only contain the TestID
 * typedef enum TestID.  We did this so we could autogenerate that header file
 * as part of maintaining our test suites.  Each TestID enumeration should be
 * connected to a callable test (or suite of tests).  It will be then up to the
 * QA team to maintain this header file to match any new or deleted tests from
 * the test suite.  Thus, the user should call this header file, and expect
 * the TestID enumerations to included.
 *
 */
#include <vpk_testID.h>

/**
 * GTR_MAX_STRLEN  Maximum string length for GenericTestHarness Global strings.
 * VPK_TEST_FUNC_POINTER_CAST Will cast the function pointer to tests.
 */
#define GTH_MAX_GLOBAL_STRLEN 2048
#define GTH_MAX_STRLEN 2048

#define VPK_TEST_FUNC_POINTER_CAST  void (*)(void)

/**
 * Prototypes **
 *
 */
int vpk_TestRunner(TestID testID, char** buffer, int bufferSize);
int vpk_TestHarness(TestID* aTestIDs, int testSize, char** buffer,
        int bufferSize);
void* vpk_AddTest(TestID iTestID);

#endif /* #ifndef _GENERICTESTHARNESS_H */
