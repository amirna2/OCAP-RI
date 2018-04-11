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

#include "test_disp.h"

void test_ARGB2COLOR(CuTest *tc);
void test_RGB2COLOR(CuTest *tc);
void test_REDVALUE(CuTest *tc);
void test_GREENVALUE(CuTest *tc);
void test_BLUEVALUE(CuTest *tc);
void test_ALPHAVALUE(CuTest *tc);
void test_gfxRunARGB2COLORTest(void);
void test_gfxRunRGB2COLORTest(void);
void test_gfxRunALPHAVALUETest(void);
void test_gfxRunREDVALUETest(void);
void test_gfxRunGREENVALUETest(void);
void test_gfxRunBLUEVALUETest(void);
CuSuite* getTestSuite_gfxColor(void);

/**
 * Tests the mpe_gfxArgbToColor() call.
 *
 * @param tc pointer to test case structure
 */
void test_ARGB2COLOR(CuTest *tc)
{
    mpe_GfxColor color = mpe_gfxArgbToColor('a', 'r', 'g', 'b');

    CuAssertIntEquals_Msg(tc, "Most significant 8 bits should be ALPHA", 'a',
            (color >> 24) & 0xFF);
    CuAssertIntEquals_Msg(tc, "2nd most significant 8 bits should be RED", 'r',
            (color >> 16) & 0xFF);
    CuAssertIntEquals_Msg(tc, "2nd least significant 8 bits should be GREEN",
            'g', (color >> 8) & 0xFF);
    CuAssertIntEquals_Msg(tc, "Least significant 8 bits should be BLUE", 'b',
            color & 0xFF);
}

/**
 * Tests the mpe_gfxRgbToColor() call.
 *
 * @param tc pointer to test case structure
 */
void test_RGB2COLOR(CuTest *tc)
{
    mpe_GfxColor color = mpe_gfxRgbToColor('r', 'g', 'b');

    CuAssertIntEquals_Msg(tc, "Alpha should be 0xFF", 0xFF, (color >> 24)
            & 0xFF);
    CuAssertIntEquals_Msg(tc, "2nd most significant 8 bits should be RED", 'r',
            (color >> 16) & 0xFF);
    CuAssertIntEquals_Msg(tc, "2nd least significant 8 bits should be GREEN",
            'g', (color >> 8) & 0xFF);
    CuAssertIntEquals_Msg(tc, "Least significant 8 bits should be BLUE", 'b',
            color & 0xFF);
}

/**
 * Tests the mpe_gfxGetRed() call.
 *
 * @param tc pointer to test case structure
 */
void test_REDVALUE(CuTest *tc)
{
    mpe_GfxColor color = mpe_gfxArgbToColor('a', 'r', 'g', 'b');

    CuAssertIntEquals_Msg(tc,
            "mpe_gfxGetRed should return 2nd most significant 8 bits", 'r',
            mpe_gfxGetRed(color));
}

/**
 * Tests the mpe_gfxGetGreen() call.
 *
 * @param tc pointer to test case structure
 */
void test_GREENVALUE(CuTest *tc)
{
    mpe_GfxColor color = mpe_gfxArgbToColor('a', 'r', 'g', 'b');

    CuAssertIntEquals_Msg(tc,
            "mpe_gfxGetGreen should return 2nd least significant 8 bits", 'g',
            mpe_gfxGetGreen(color));
}

/**
 * Tests the mpe_gfxGetBlue() call.
 *
 * @param tc pointer to test case structure
 */
void test_BLUEVALUE(CuTest *tc)
{
    mpe_GfxColor color = mpe_gfxArgbToColor('a', 'r', 'g', 'b');

    CuAssertIntEquals_Msg(tc,
            "mpe_gfxGetBlue should return least significant 8 bits", 'b',
            mpe_gfxGetBlue(color));
}

/**
 * Tests the mpe_gfxGetAlpha() call.
 *
 * @param tc pointer to test case structure
 */
void test_ALPHAVALUE(CuTest *tc)
{
    mpe_GfxColor color = mpe_gfxArgbToColor('a', 'r', 'g', 'b');

    CuAssertIntEquals_Msg(tc,
            "mpe_gfxGetAlpha should return most significant 8 bits", 'a',
            mpe_gfxGetAlpha(color));
}

/**
 * Create and return the test suite for the mpe_gfx APIs.
 * @return a pointer to the new test suite.
 */
CuSuite* getTestSuite_gfxColor(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_STOP_TEST(suite, test_ARGB2COLOR);
    SUITE_STOP_TEST(suite, test_RGB2COLOR);
    SUITE_STOP_TEST(suite, test_ALPHAVALUE);
    SUITE_STOP_TEST(suite, test_REDVALUE);
    SUITE_STOP_TEST(suite, test_GREENVALUE);
    SUITE_STOP_TEST(suite, test_BLUEVALUE);

    return suite;
}

void test_gfxRunARGB2COLORTest(void)
{
    CuSuite* suite;

    suite = CuSuiteNewCloneTest(getTestSuite_gfxColor(), "test_ARGB2COLOR");
    test_gfxRunSuite(suite, "mpe_gfxArgbToColor");

    CuSuiteFree(suite);
}

void test_gfxRunRGB2COLORTest(void)
{
    CuSuite* suite;

    suite = CuSuiteNewCloneTest(getTestSuite_gfxColor(), "test_RGB2COLOR");
    test_gfxRunSuite(suite, "mpe_gfxRgbToColor");

    CuSuiteFree(suite);
}

void test_gfxRunALPHAVALUETest(void)
{
    CuSuite* suite;

    suite = CuSuiteNewCloneTest(getTestSuite_gfxColor(), "test_ALPHAVALUE");
    test_gfxRunSuite(suite, "mpe_gfxGetAlpha");

    CuSuiteFree(suite);
}

void test_gfxRunREDVALUETest(void)
{
    CuSuite* suite;

    suite = CuSuiteNewCloneTest(getTestSuite_gfxColor(), "test_REDVALUE");
    test_gfxRunSuite(suite, "mpe_gfxGetRed");

    CuSuiteFree(suite);
}

void test_gfxRunGREENVALUETest(void)
{
    CuSuite* suite;

    suite = CuSuiteNewCloneTest(getTestSuite_gfxColor(), "test_GREENVALUE");
    test_gfxRunSuite(suite, "mpe_gfxGetGreen");

    CuSuiteFree(suite);
}

void test_gfxRunBLUEVALUETest(void)
{
    CuSuite* suite;

    suite = CuSuiteNewCloneTest(getTestSuite_gfxColor(), "test_BLUEVALUE");
    test_gfxRunSuite(suite, "mpe_gfxGetBlue");

    CuSuiteFree(suite);
}
