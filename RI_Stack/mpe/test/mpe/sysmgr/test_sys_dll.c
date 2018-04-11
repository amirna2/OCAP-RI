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

#define _MPE_DLL
#define TEST_MPE_DLL		/* Force resolution of test mem bindings. */

#include <cutest.h>
#include <mpe_sys.h>
#include "test_sys.h"

// Prototypes:
static void test_dlModOpen(CuTest *tc);

// Test data:
static void *testPtr;

/**
 * <i>test_dlModOpen()</i>
 *
 * Validate dlmodOpen().
 *
 * @param tc is a pointer to the test case structure.
 */
static void test_dlmodOpen(CuTest *tc)
{
    mpe_Error ec;
    mpe_Dlmod modHandle;

    CuAssert(tc, "dlmodOpen not defined", NULL != dlmodClose);

    /* Check null pointer case: */
    ec = dlmodOpen("mpeostest", NULL);
    CuAssert(tc, "dlmodOpen(\"mpeostest\", NULL) failed", ec == MPE_EINVAL);

    /* Check case of library not found */
    ec = dlmodOpen("mpeostestnothere", &modHandle);
    CuAssert(tc, "dlmodOpen(\"mpeostestnothere\", &modHandle) failed", ec
            == MPE_EINVAL && modHandle == NULL);

    /* Set up the function table pointer for testing */
    // TODO fix this link issue
    //mpeos_dlmodInit((void **)0xdeadbeef);

    testPtr = NULL;
    ec = dlmodOpen("mpeostest", &modHandle);
    CuAssert(tc, "dlmodOpen didn't return valid handle", ec == MPE_SUCCESS
            && modHandle != NULL);
    CuAssert(tc, "dlmodOpen didn't execute init function", testPtr
            == (void *) 0xdeadbeef);
}

/**
 * <i>test_dlModClose()</i>
 *
 * Validate dlmodClose().
 *
 * @param tc is a pointer to the test case structure.
 */
static void test_dlmodClose(CuTest *tc)
{
    mpe_Error ec;
    mpe_Dlmod modHandle;

    CuAssert(tc, "dlmodClose not defined", NULL != dlmodClose);

    /* Check null pointer case: */
    ec = dlmodClose(NULL);
    CuAssert(tc, "dlmodClose(NULL) failed", ec == MPE_EINVAL);

    ec = dlmodOpen("mpeostest", &modHandle);
    CuAssert(tc, "dlmodOpen didn't return valid handle", ec == MPE_SUCCESS
            && modHandle != NULL);

    testPtr = NULL;
    ec = dlmodClose(modHandle);
    CuAssert(tc, "dlmodClose didn't execute term function", ec == MPE_SUCCESS
            && testPtr == (void *) 0xdcdcdcdc);
}

/**
 * <i>test_dlModGetSymbol()</i>
 *
 * Validate dlmodGetSymbol().
 *
 * @param tc is a pointer to the test case structure.
 */
static void test_dlmodGetSymbol(CuTest *tc)
{
    mpe_Error ec;
    mpe_Dlmod modHandle;
    void (*testfunc)(void); /* Test func entry point */

    CuAssert(tc, "dlmodClose not defined", NULL != dlmodGetSymbol);

    /* Check null pointer case: */
    ec = dlmodGetSymbol(NULL, "mpeostestHelper", (void**) &testfunc);
    CuAssert(tc, "dlmodGetSymbol(NULL, ..., ...) failed", ec == MPE_EINVAL);

    ec = dlmodOpen("mpeostest", &modHandle);
    CuAssert(tc, "dlmodOpen didn't return valid handle", ec == MPE_SUCCESS
            && modHandle != NULL);

    /* Check null funcptr case: */
    ec = dlmodGetSymbol(modHandle, "mpeostestHelper", NULL);
    CuAssert(tc, "dlmodClose(..., ..., NULL) failed", ec == MPE_EINVAL);

    /* Check string not found case: */
    ec
            = dlmodGetSymbol(modHandle, "mpeostestHelpernothere",
                    (void**) &testfunc);
    CuAssert(tc, "dlmodClose(..., mpeostestHelpernothere, ...) failed", ec
            == MPE_ENODATA);

    /* Check valid case */
    ec = dlmodGetSymbol(modHandle, "mpeostestHelper", (void**) &testfunc);
    CuAssert(tc, "dlmodClose failed to return func pointer", ec == MPE_SUCCESS
            && *testfunc != NULL);

    /* Test testfunc execution */
    testPtr = (void *) 0xdeadbeef;
    (*testfunc)();
    CuAssert(tc, "dlmodClose returned function failed to execute", testPtr
            == (void *) 0x12345678);
}

/**
 * <i>mpeostestHelper()</i>
 *
 *
 * NOTE: This function must be included in the exports file in order for
 *       this test to work!!
 */
void mpeostestHelper(void)
{
    testPtr = (void *) 0x12345678;
    return;
}

/**
 * <i>mpelib_init_mpeostest()</i>
 *
 * Validate dlmodOpen().
 *
 * @param ftable is a pointer to the MPE global function table.
 *
 * NOTE: This function must be included in the exports file in order for
 *       this test to work!!
 */
void mpelib_init_mpeostest(void **mpe_ftable)
{
    testPtr = (void *) mpe_ftable;
    return;
}

/**
 * <i>mpelib_init_mpeostest()</i>
 *
 * NOTE: This function must be included in the exports file in order for
 *       this test to work!!
 */
void mpelib_term_mpeostest(void)
{
    testPtr = (void *) 0xdcdcdcdc;
    return;
}

/**
 * getTestSuite_sysDll
 *
 * Create and return the test suite for the mpe_dll APIs.
 *
 * @return a pointer to the new test suite.
 */
CuSuite* getTestSuite_sysDll(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, test_dlmodOpen);
    SUITE_ADD_TEST(suite, test_dlmodClose);
    SUITE_ADD_TEST(suite, test_dlmodGetSymbol);

    return suite;
}
