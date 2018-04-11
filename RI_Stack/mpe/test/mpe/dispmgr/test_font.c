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

void test_gfxFontNew(CuTest *tc);
void test_gfxFontDelete(CuTest *tc);
void test_gfxGetFontMetrics(CuTest *tc);
void test_gfxGetStringWidth(CuTest *tc);
void test_gfxGetString16Width(CuTest *tc);
void test_gfxGetCharWidth(CuTest *tc);
void test_gfxFontHasCode(CuTest *tc);
void test_gfxRunFontTests(void);
void test_gfxRunFontNewTest(void);
void test_gfxRunFontDeleteTest(void);
void test_gfxRunGetFontMetricsTest(void);
void test_gfxRunGetStringWidthTest(void);
void test_gfxRunGetString16WidthTest(void);
void test_gfxRunGetCharWidthTest(void);
void test_gfxRunFontHasCodeTest(void);
CuSuite* getTestSuite_gfxFont(void);

/* utf16 string */
static mpe_GfxWchar wstring[] =
{ 'M', 'M', 'M', 'M', 'M', '\0' };
static mpe_GfxWchar wstring2[3] =
{ 400, 257, 258 };

#define WSTRING_LEN ((sizeof(wstring)-sizeof(mpe_GfxWchar))/sizeof(mpe_GfxWchar))

static char string[] =
{ "MMMMM" };
#define STRING_LEN ((sizeof(string)-sizeof(char))/sizeof(char))

static mpe_GfxWchar names[][32] =
{
{ 'T', 'i', 'r', 'e', 's', 'i', 'a', 's' },
// Only Tiresias needs to be supported, but these logical names should also
        { 'S', 'e', 'r', 'i', 'f' },
        { 'S', 'a', 'n', 's', 'S', 'e', 'r', 'i', 'f' },
        { 'D', 'i', 'a', 'l', 'o', 'g' },
        { 'M', 'o', 'n', 'o', 's', 'p', 'a', 'c', 'e', 'd' },
        { 'D', 'i', 'a', 'l', 'o', 'g', 'I', 'n', 'p', 'u', 't' },
        // The following names probably won't be supported...
        { 'H', 'e', 'l', 'v', 'e', 't', 'i', 'c', 'a' },
        { 'T', 'i', 'm', 'e', 's', 'R', 'o', 'm', 'a', 'n' },
        { 'C', 'o', 'u', 'r', 'i', 'e', 'r' },
        { 'Z', 'a', 'p', 'f', 'D', 'i', 'n', 'g', 'b', 'a', 't', 's' }, };
#define NNAMES (sizeof(names)/(sizeof(mpe_GfxWchar)*32))
static mpe_GfxFontStyle styles[] =
{ MPE_GFX_PLAIN, MPE_GFX_BOLD, MPE_GFX_ITALIC, MPE_GFX_BOLD_ITALIC, };
#define NSTYLES (sizeof(styles)/sizeof(mpe_GfxFontStyle))
static int32_t sizes[] =
{ 24, /* Footnote */
26, /* Body - default */
31, /* Subtitle */
36 /* Heading/Large Subtitle */
};
#define NSIZES (sizeof(sizes)/sizeof(int32_t))
static int32_t points[] =
{ /* conversion of points to pixels */
33, 36, 43, 49 };

#define BAD_FONT ((mpe_GfxFont)-1)

static mpe_Error setup_font(mpe_GfxFont *pfont, mpe_GfxFontMetrics *pmetrics,
        mpe_GfxWchar *name, mpe_GfxFontStyle style, int size)
{
    mpe_GfxFont font;
    mpe_Error ec;

    //    if ((ec = gfxFontNew(0, names[0], sizeof(names), styles[0], sizes[1], &font)) == MPE_SUCCESS)
    if ((ec = gfxFontNew(0, name, sizeof(name), MPE_GFX_PLAIN, size, &font))
            == MPE_SUCCESS)
    {
        *pfont = font;

        if (pmetrics != NULL)
            ec = gfxGetFontMetrics(font, pmetrics);
    }
    return ec;
}
static mpe_Error setup(mpe_GfxFont *pfont, mpe_GfxFontMetrics *pmetrics)
{
    return setup_font(pfont, pmetrics, names[0], styles[0], sizes[1]);
}
static void teardown(mpe_GfxFont font)
{
    if (font != (mpe_GfxFont) 0 && font != BAD_FONT)
        gfxFontDelete(font);
}

/**
 * Tests the mpe_gfxFontNew() call.
 *
 * @param tc pointer to test case structure
 */
void test_gfxFontNew(CuTest *tc)
{
    int i;
    for (i = 0; i < NNAMES; ++i)
    {
        mpe_GfxFont font = BAD_FONT;
        mpe_Error ec;
        mpe_GfxWchar *name = names[i];
        mpe_GfxFontStyle style = styles[i % NSTYLES];
        int size = sizes[i % NSIZES];

        CuTestSetup(ec = gfxFontNew(0, name, sizeof(names), style, size, &font));
        CuAssert(tc, "gfxFontNew failed", ec == MPE_GFX_ERROR_NOERR);

        CuTestCleanup(teardown(font));
    }
}

/**
 * Tests the mpe_gfxFontDelete() call.
 *
 * @param tc pointer to test case structure
 */
void test_gfxFontDelete(CuTest *tc)
{
    mpe_Error ec;
    mpe_GfxFont font;
    CuTestSetup(ec = setup(&font, NULL));
    CuAssert(tc, "Setup failed", ec == MPE_SUCCESS);

    ec = gfxFontDelete(font);
    font = (mpe_GfxFont) 0;
    CuAssert(tc, "gfxFontDelete failed", ec == MPE_SUCCESS);

    CuTestCleanup(teardown(font));
}

/**
 * Tests the gfxGetFontMetrics() call.
 *
 * @param tc pointer to test case structure
 */
void test_gfxGetFontMetrics(CuTest *tc)
{
    int i;
    for (i = 0; i < NNAMES; ++i)
    {
        mpe_Error ec;
        mpe_GfxFont font = 0;
        mpe_GfxFontMetrics metrics =
        { 0 };
        mpe_GfxWchar *name = names[i];
        mpe_GfxFontStyle style = styles[i % NSTYLES];
        int size = sizes[i % NSIZES]; // Requested size in points
        int pxsize = points[i % NSIZES]; // Expected size in pixels
        CuTestSetup(ec = setup_font(&font, NULL, name, style, size));
        CuAssert(tc, "setup failed", ec == MPE_SUCCESS);

        // Get the metrics
        ec = gfxGetFontMetrics(font, &metrics);
        CuAssert(tc, "gfxGetFontMetrics failed", ec == MPE_SUCCESS);

        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "font h=%d a=%d, d=%d, l=%d\n",
                metrics.height, metrics.ascent, metrics.descent,
                metrics.leading);

        CuAssert(tc, "Ascent should be less than height", metrics.ascent
                < metrics.height);

        CuAssert(tc, "Descent should be less than height", metrics.descent
                < metrics.height);

        CuAssert(tc, "Leading should be less than height", metrics.leading
                < metrics.height);

        CuAssertIntEquals_Msg(tc, "Height should be ascent+descent+leading",
                metrics.ascent + metrics.descent + metrics.leading,
                metrics.height);

        CuAssertIntEquals_Msg(tc,
                "The metrics height should be the size of the font in pixels",
                pxsize, metrics.height);

        /*
         * removed this part as these values are 0 right now.
         CuAssert(tc, "maxAscent should be greater than or equal ascent",
         metrics.maxascent >= metrics.ascent);

         CuAssert(tc, "maxDescent should be greater than or equal descent",
         metrics.maxdescent >= metrics.descent);
         */

        CuAssert(tc, "maxadvance should > 0", metrics.maxadvance > 0);

        CuAssert(tc, "first_char should be in range 0-0xFFFF",
                metrics.first_char >= 0 && metrics.first_char <= 0xFFFF);

        CuAssert(tc, "last_char should be in range 0-0xFFFF", metrics.last_char
                >= 0 && metrics.last_char <= 0xFFFF);

        CuAssert(tc, "last_char should be >= first_char", metrics.last_char
                >= metrics.first_char);

        CuTestCleanup(teardown(font));

    }
}

/**
 * Tests the mpe_gfxGetStringWidth() call.
 *
 * @param tc pointer to test case structure
 */
void test_gfxGetStringWidth(CuTest *tc)
{
    int i;
    for (i = 0; i < NNAMES; ++i)
    {
        mpe_Error ec;
        mpe_GfxFont font = 0;
        mpe_GfxWchar *name = names[i];
        mpe_GfxFontStyle style = styles[i % NSTYLES];
        int size = sizes[i % NSIZES];
        int32_t width, nwidth;
        CuTestSetup(ec = setup_font(&font, NULL, name, style, size));

        CuAssert(tc, "setup failed", ec == MPE_SUCCESS);

        // Determine expected width
        width = -1;
        nwidth = -1;
        CuAssert(tc, "Could not get character width", gfxGetCharWidth(font,
                wstring[0], &width) == MPE_SUCCESS);
        width *= STRING_LEN;

        // Query width
        ec = gfxGetStringWidth(font, string, STRING_LEN, &nwidth);
        CuAssert(tc, "gfxGetStringWidth failed", ec == MPE_SUCCESS);
        CuAssert(tc, "gfxGetStringWidth did not return a good value", nwidth
                != -1);
        CuAssertIntEquals_Msg(tc,
                "gfxGetStringWidth did not return the expected length", width,
                nwidth);

        CuTestCleanup(teardown(font));
    }
}

/**
 * Tests the mpe_gfxGetString16Width() call.
 *
 * @param tc pointer to test case structure
 */
void test_gfxGetString16Width(CuTest *tc)
{
    int i;

    for (i = 0; i < NNAMES; ++i)
    {

        mpe_Error ec;
        mpe_GfxFont font = 0;
        mpe_GfxWchar *name = names[i];
        mpe_GfxFontStyle style = styles[i % NSTYLES];
        int size = sizes[i % NSIZES];
        int32_t width, nwidth;
        CuTestSetup(ec = setup_font(&font, NULL, name, style, size));
        CuAssert(tc, "setup failed", ec == MPE_SUCCESS);

        // Determine expected width
        width = -1;
        nwidth = -1;
        CuAssert(tc, "Could not get character width", gfxGetCharWidth(font,
                wstring[0], &width) == MPE_SUCCESS);

        width *= STRING_LEN;

        // Query width
        ec = gfxGetString16Width(font, wstring, WSTRING_LEN, &nwidth);
        CuAssert(tc, "gfxGetString16Width failed", ec == MPE_SUCCESS);
        CuAssert(tc, "gfxGetString16Width did not return a good value", nwidth
                != -1);
        CuAssertIntEquals_Msg(tc,
                "gfxGetString16Width did not return the expected length",
                width, nwidth);

        /* test with a utf16 string that contains a multi-byte character */
        /* returned value should be MPE_SUCCESS */
        ec = gfxGetString16Width(font, wstring2, 3, &nwidth);
        CuAssert(tc, "gfxGetString16Width failed", ec == MPE_SUCCESS);

        CuTestCleanup(teardown(font));
    }
}

/**
 * Tests the mpe_gfxGetCharWidth() call.
 *
 * @param tc pointer to test case structure
 */
void test_gfxGetCharWidth(CuTest *tc)
{
    mpe_GfxFont font = (mpe_GfxFont) 0;
    mpe_GfxFontMetrics metrics;
    mpe_Error ec;
    // Measure char width of '|'
    int32_t width = -1;
    int32_t width2 = -1;

    CuTestSetup(ec = setup(&font, &metrics));
    CuAssert(tc, "setup failed", ec == MPE_SUCCESS);

    ec = gfxGetCharWidth(font, '|', &width);
    CuAssert(tc, "gfxGetCharWidth failed", ec == MPE_SUCCESS);
    CuAssert(tc, "gfxGetCharWidth returned a bad value", width != -1);
    CuAssert(tc, "character width should be > 0", width > 0);

    // Measure char width of 'M'
    ec = gfxGetCharWidth(font, 'M', &width2);
    CuAssert(tc, "gfxGetCharWidth failed", ec == MPE_SUCCESS);
    CuAssert(tc, "gfxGetCharWidth returned a bad value", width2 != -1);
    CuAssert(tc, "character width should be > 0", width2 > 0);

    // Assume '|' skinnier than 'M'
    CuAssert(tc, "width('|') < width('M')", width < width2);

    CuTestCleanup(teardown(font));
}

/**
 * Tests the mpe_gfxFontHasCode() call.
 * This test is pretty useless since invalid codes cannot be determined
 *
 * @param tc pointer to test case structure
 */
void test_gfxFontHasCode(CuTest *tc)
{
    int i;
    for (i = 0; i < NNAMES; ++i)
    {
        mpe_Error ec;
        mpe_GfxFont font = 0;
        mpe_GfxFontMetrics metrics =
        { 0 };
        mpe_GfxWchar *name = names[i];
        mpe_GfxFontStyle style = styles[i % NSTYLES];
        int size = sizes[i % NSIZES]; /* Size requested in points */

        //initialize to defualt font
        CuTestSetup(ec = setup_font(&font, &metrics, name, style, size));
        CuAssert(tc, "setup failed", ec == MPE_SUCCESS);

        //See if code exists
        ec = gfxFontHasCode(font, metrics.first_char);
        CuAssert(tc, "Should 'have' the first_char", ec == MPE_SUCCESS);

    }
}

/**
 * Create and return the test suite for the mpe_gfx APIs.
 * @return a pointer to the new test suite.
 */
CuSuite* getTestSuite_gfxFont(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_STOP_TEST(suite, test_gfxFontNew);
    SUITE_STOP_TEST(suite, test_gfxFontDelete);
    SUITE_STOP_TEST(suite, test_gfxGetFontMetrics);
    SUITE_STOP_TEST(suite, test_gfxGetStringWidth);
    SUITE_STOP_TEST(suite, test_gfxGetString16Width);
    SUITE_STOP_TEST(suite, test_gfxGetCharWidth);
    SUITE_STOP_TEST(suite, test_gfxFontHasCode);

    return suite;
}

void test_gfxRunFontTests(void)
{
    test_gfxRunSuite(getTestSuite_gfxFont(), "FontTests");
}

void test_gfxRunFontNewTest(void)
{
    CuSuite* suite;

    suite = CuSuiteNewCloneTest(getTestSuite_gfxFont(), "test_gfxFontNew");
    test_gfxRunSuite(suite, "gfxFontNew");

    CuSuiteFree(suite);
}

void test_gfxRunFontDeleteTest(void)
{
    CuSuite* suite;

    suite = CuSuiteNewCloneTest(getTestSuite_gfxFont(), "test_gfxFontDelete");
    test_gfxRunSuite(suite, "gfxFontDelete");

    CuSuiteFree(suite);
}

void test_gfxRunGetFontMetricsTest(void)
{
    CuSuite* suite;

    suite = CuSuiteNewCloneTest(getTestSuite_gfxFont(),
            "test_gfxGetFontMetrics");
    test_gfxRunSuite(suite, "gfxGetFontMetrics");

    CuSuiteFree(suite);
}

void test_gfxRunGetStringWidthTest(void)
{
    CuSuite* suite;

    suite = CuSuiteNewCloneTest(getTestSuite_gfxFont(),
            "test_gfxGetStringWidth");
    test_gfxRunSuite(suite, "gfxGetStringWidth");

    CuSuiteFree(suite);
}

void test_gfxRunGetString16WidthTest(void)
{
    CuSuite* suite;

    suite = CuSuiteNewCloneTest(getTestSuite_gfxFont(),
            "test_gfxGetString16Width");
    test_gfxRunSuite(suite, "gfxGetString16Width");

    CuSuiteFree(suite);
}

void test_gfxRunGetCharWidthTest(void)
{
    CuSuite* suite;

    suite = CuSuiteNewCloneTest(getTestSuite_gfxFont(), "test_gfxGetCharWidth");
    test_gfxRunSuite(suite, "gfxGetCharWidth");

    CuSuiteFree(suite);
}

void test_gfxRunFontHasCodeTest(void)
{
    CuSuite* suite;

    suite = CuSuiteNewCloneTest(getTestSuite_gfxFont(), "test_gfxFontHasCode");
    test_gfxRunSuite(suite, "gfxFontHasCode");

    CuSuiteFree(suite);
}
