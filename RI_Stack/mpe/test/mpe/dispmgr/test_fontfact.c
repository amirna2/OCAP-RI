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
#include "vte_agent.h"

// Wide character string compare.
int wcstringnicmp(const mpe_GfxWchar *ws1, const mpe_GfxWchar *ws2, size_t n1,
        size_t n2);

void test_gfx_FontFactNewDelete(CuTest *tc);
void test_gfx_FontFactNewDifferentInstance(CuTest *tc);
void test_gfx_FontNewDelete(CuTest *tc);
void test_gfx_FontNewDeleteValidFontFactory(CuTest *tc);
void test_gfx_FontNewDeleteInValidFontFactory(CuTest *tc);
void test_gfx_FontFactNeverSystem(CuTest *tc);
void test_gfx_FontNotFoundNull(CuTest *tc);
void test_gfx_FontFactNewDeleteNull(CuTest *tc);
void test_gfx_FontFactoryAdd(CuTest *tc);
void test_gfx_FontGetList(CuTest *tc);
void test_gfx_FontDefaultFFAlwaysReturnSomething(CuTest *tc);
void test_gfx_FontMutiNewSameHandle(CuTest *tc);
void test_gfx_FontFactoryVerifyDistinctiveness(CuTest *tc);
void test_gfx_FontFactorySameFontMultiTimes(CuTest *tc);
void test_gfxRunSingleTest(char *iTestCaseName, char *iTestCaseFuncName);
CuSuite* getTestSuite_gfxfontfact(void);
mpe_Bool getFont(mpe_GfxFontDesc * fd, const char * fileName, CuTest * tc);

char *ameliaFont = "sys/fonts/Tires-o_802.pfr";
char *dialogFont = "sys/fonts/Tires-o_802.pfr";
char *dialoginputFont = "sys/fonts/Tires-o_802.pfr";
char *monospacedFont = "sys/fonts/Tires-o_802.pfr";
char *sansserifFont = "sys/fonts/Tires-o_802.pfr";
char *serifFont = "sys/fonts/Tires-o_802.pfr";
char *tiresiasFont = "sys/fonts/Tires-o_802.pfr";

const int FONTNUMBER = 7;

mpe_GfxWchar dialogFontName[] =
{ 'E', 'n', 'g', 'r', 'a', 'v', 'e', 'r', 's', 'G', 'o', 't', 'h', 'i', 'c',
        ' ', 'B', 'T', '\0' };
const int dialogFontNameSize = (sizeof(dialogFontName)
        / sizeof(dialogFontName[0])) - 1;

mpe_GfxWchar tiresiasFontName[] =
{ 't', 'i', 'r', 'e', 's', 'i', 'a', 's', '\0' };
const int tiresiasFontNameSize = (sizeof(tiresiasFontName)
        / sizeof(tiresiasFontName[0])) - 1;

// The following font names are just made up to stop the tests from crashing.
mpe_GfxWchar ameliaFontName[] =
{ 'a', 'm', 'e', 'l', 'i', 'a', '\0' };
const int ameliaFontNameSize = (sizeof(ameliaFontName)
        / sizeof(ameliaFontName[0])) - 1;

mpe_GfxWchar dialoginputFontName[] =
{ 'd', 'i', 'a', 'l', 'o', 'g', 'i', 'n', 'p', 'u', 't', '\0' };
const int dialoginputFontNameSize = (sizeof(dialoginputFontName)
        / sizeof(dialoginputFontName[0])) - 1;

mpe_GfxWchar monospacedFontName[] =
{ 'm', 'o', 'n', 'o', 's', 'p', 'a', 'c', 'e', 'd', '\0' };
const int monospacedFontNameSize = (sizeof(monospacedFontName)
        / sizeof(monospacedFontName[0])) - 1;

mpe_GfxWchar sansserifFontName[] =
{ 's', 'a', 'n', 's', 's', 'e', 'r', 'i', 'f', '\0' };
const int sansserifFontNameSize = (sizeof(sansserifFontName)
        / sizeof(sansserifFontName[0])) - 1;

mpe_GfxWchar serifFontName[] =
{ 's', 'e', 'r', 'i', 'f', '\0' };
const int serifFontNameSize =
        (sizeof(serifFontName) / sizeof(serifFontName[0])) - 1;

/**
 *
 * @param tc pointer to test case structure
 */
mpe_Bool getFont(mpe_GfxFontDesc * fd, const char * fileName, CuTest * tc)
{
    mpe_File h;
    mpe_Error rc;

    int64_t fs = 0;
    int64_t offset = 0;
    int64_t oldPos = 0;
    int64_t eofPos = 0;

    /*
     ** Concern:  If I can create a font just by passing the data and datasize, 
     ** why do I have to null the name?  Why can't the rest of the structure be
     ** garbage.
     */
    memset(fd, 0, sizeof(mpe_GfxFontDesc));

    // initialize the font descriptor
    fd->fnt_format = GFX_FONT_PFR;
    fd->style = MPE_GFX_PLAIN;
    fd->minsize = 0;
    fd->maxsize = 65535;

    rc = fileOpen(fileName, MPE_FS_OPEN_READ, &h);
    if (MPE_SUCCESS == rc)
    {
        CuAssertIntEquals_Msg(tc, "fileOpen - MPE_FS_OPEN_READ",
                MPE_FS_ERROR_SUCCESS, rc);

        rc = fileSeek(h, MPE_FS_SEEK_CUR, &offset);
        CuAssertIntEquals_Msg(tc, "fileSeek", MPE_FS_ERROR_SUCCESS, rc);
        oldPos = offset;
        offset = 0;

        rc = fileSeek(h, MPE_FS_SEEK_END, &offset);
        CuAssertIntEquals_Msg(tc, "fileSeek", MPE_FS_ERROR_SUCCESS, rc);
        eofPos = offset;
        offset = 0;

        rc = fileSeek(h, MPE_FS_SEEK_SET, &oldPos);
        CuAssertIntEquals_Msg(tc, "fileSeek", MPE_FS_ERROR_SUCCESS, rc);
        fs = eofPos - oldPos;

        fd->datasize = (uint32_t) fs;
        rc = memAllocP(MPE_MEM_TEST, fd->datasize, (void**) &fd->data);
        CuAssert(tc, "memAlloc failed", MPE_SUCCESS == rc);

        rc = fileRead(h, &(fd->datasize), fd->data);
        //CuAssertIntEquals_Msg(tc,"fileRead",MPE_FS_ERROR_SUCCESS,rc);	

        rc = fileClose(h);
        CuAssertIntEquals_Msg(tc, "fileClose", MPE_FS_ERROR_SUCCESS, rc);
        return TRUE;
    }
    return FALSE;
}

/*
 ** Signature test for New and Delete 
 */
void test_gfx_FontFactNewDelete(CuTest *tc)
{
    mpe_GfxFontFactory ff;
    mpe_Error result = 0;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "test_gfx_FontFactNewDelete() Enter...\n");
    result = gfxFontFactoryNew(&ff);
    ASSERT( gfxFontFactoryNew);

    result = gfxFontFactoryDelete(ff);
    ASSERT(gfxFontFactoryNew);
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "test_gfx_FontFactNewDelete() Exit...\n");
}

/* 
 ** Multiple calls return different instances
 */
void test_gfx_FontFactNewDifferentInstance(CuTest *tc)
{
    mpe_GfxFontFactory ff1;
    mpe_GfxFontFactory ff2;
    mpe_Error result = 0;

    result = gfxFontFactoryNew(&ff1);
    ASSERT( gfxFontFactoryNew);

    result = gfxFontFactoryNew(&ff2);
    ASSERT(gfxFontFactoryNew);

    CuAssert(tc, "Created Identical instances of a Font Factory : FAIL", ff1
            != ff2);

    result = gfxFontFactoryDelete(ff1);
    ASSERT(gfxFontFactoryNew);
    result = gfxFontFactoryDelete(ff2);
    ASSERT(gfxFontFactoryNew);
}

/*
 ** Signature 'FontNewDelete()' Test from system fontfactory
 */
void test_gfx_FontNewDelete(CuTest *tc)
{
    static mpe_GfxWchar fontName1[] =
    { 'd', 'i', 'a', 'l', 'o', 'g' };
    mpe_GfxFont dialogH;
    mpe_Error result = 0;

    result = gfxFontNew(NULL, (mpe_GfxWchar*) fontName1, 6,
            (mpe_GfxFontStyle) 0, 64, &dialogH);
    ASSERT( gfxFontNew);

    result = gfxFontDelete(dialogH);
    ASSERT( gfxFontDelete);
}

/*
 **  Verify font new from a empty non-system font factory
 */
void test_gfx_FontNewDeleteValidFontFactory(CuTest *tc)
{
    mpe_GfxFontFactory ff;
    mpe_GfxFont dialogH;
    mpe_Error result = 0;
    mpe_GfxFontDesc fd;

    // Create a new font factory
    result = gfxFontFactoryNew(&ff);
    ASSERT( gfxFontFactoryNew);

    // Create a font description
    result = getFont(&fd, dialogFont, tc);
    if (result)
    {
        // save the pointer to the name of the font
        fd.name = &dialogFontName[0];
        fd.namelength = dialogFontNameSize;

        // Add a font to the font factory
        result = gfxFontFactoryAdd(ff, &fd);
        ASSERT( gfxFontFactoryAdd);

        // Using the created font factory, make a new refernce to the font
        result = gfxFontNew(0, (mpe_GfxWchar*) fd.name, fd.namelength,
                (mpe_GfxFontStyle) fd.style, fd.maxsize, &dialogH);
        ASSERT( gfxFontNew);

        // Delete the font reference (in turn deleting the font)
        result = gfxFontDelete(dialogH);
        ASSERT( gfxFontDelete);

        // Delete the font factory
        result = gfxFontFactoryDelete(ff);
        ASSERT( gfxFontFactoryDelete);
    }
}

/*
 ** FontNew()  null font factory
 */
void test_gfx_FontNewDeleteInValidFontFactory(CuTest *tc)
{
    mpe_GfxFontFactory ff = NULL;
    mpe_GfxFont dialogH;
    mpe_Error result = 0;
    mpe_GfxFontDesc fd;

    // Create a font description
    result = getFont(&fd, dialogFont, tc);
    if (result)
    {
        // store the pointer to the name
        fd.name = &dialogFontName[0];

        // try to create a font with a null font factory
        result = gfxFontFactoryAdd(ff, &fd);
        ASSERTFAIL( gfxFontFactoryAdd);

    }
}
/*
 ** Verify that a system font is never returned from
 ** Verified by adding a non-system font to a new font
 ** factory(ff) and then querying attempting to create 
 ** a system font from that factory.  
 ** May also verify that only the one added font exists 
 ** in the new font factory by verifying next and prev 
 ** ptrs equal each other.
 */
void test_gfx_FontFactNeverSystem(CuTest *tc)
{
    mpe_Error result = 0;
    mpe_GfxFontFactory ff;
    mpe_GfxFont fontH;
    mpe_GfxFontDesc fd; /* added fd */
    mpe_GfxFontDesc *fontList;

    result = gfxFontFactoryNew(&ff);
    ASSERT( gfxFontFactoryNew);

    result = getFont(&fd, ameliaFont, tc);

    if (result)
    {
        fd.name = &ameliaFontName[0];

        result = gfxFontFactoryAdd(ff, &fd);
        ASSERT( gfxFontFactoryAdd);

        CuAssert(tc, "Verify this is the only font in the list", fd.prev
                == fd.next);

        result = gfxFontGetList(&fontList);
        ASSERT( gfxFontGetList);

        result = gfxFontNew(ff, (mpe_GfxWchar*) fontList->name,
                fontList->namelength, (mpe_GfxFontStyle) fontList->style,
                fontList->maxsize, &fontH);
        ASSERTFAIL( gfxFontNew);

        result = gfxFontFactoryDelete(ff);
        ASSERT( gfxFontFactoryDelete);
    }
}

/*
 ** Null should be returned if the font doesn't exist in a created
 ** font factory.
 */
void test_gfx_FontNotFoundNull(CuTest *tc)
{
    mpe_Error result;
    mpe_GfxFontFactory ff;
    mpe_GfxFont fontH;
    mpe_GfxWchar name[] =
    { '\0' };

    // create font factory
    result = gfxFontFactoryNew(&ff);
    CuAssert(tc, "gfxFontFactoryNew() - font factory creation failed.", result
            == MPE_GFX_ERROR_NOERR);

    // invalid parameters - should fail
    result = gfxFontNew(ff, NULL, 0, MPE_GFX_PLAIN, 18, NULL);
    CuAssert(
            tc,
            "gfxFontNew() - font creation with invalid parameters succeeded (should fail).",
            result != MPE_GFX_ERROR_NOERR);

    // invalid parameters - should fail
    result = gfxFontNew(ff, name, 0, MPE_GFX_PLAIN, 18, NULL);
    CuAssert(
            tc,
            "gfxFontNew() - font creation with invalid parameters succeeded (should fail).",
            result != MPE_GFX_ERROR_NOERR);

    // invalid parameters - should fail
    fontH = (mpe_GfxFont) 0x01234567;
    result = gfxFontNew(ff, NULL, 0, MPE_GFX_PLAIN, 18, &fontH);
    CuAssert(
            tc,
            "gfxFontNew() - font creation with invalid parameters succeeded (should fail).",
            result != MPE_GFX_ERROR_NOERR);

    // valid parameters, but font not found - should fail, and set font handle to NULL
    fontH = (mpe_GfxFont) 0x01234567;
    result = gfxFontNew(ff, name, 0, MPE_GFX_PLAIN, 18, &fontH);
    CuAssert(
            tc,
            "gfxFontNew() - font creation with font not found succeeded (should fail).",
            result != MPE_GFX_ERROR_NOERR);

    // the primary test condition for this test function - the font handle must have been set NULL
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "test_gfx_FontNotFoundNull = fonthandle: 0x%x\n", fontH);
    CuAssert(tc, "Font Handle 'NULL' verification: FAILED", fontH == NULL);

    // delete font factory
    result = gfxFontFactoryDelete(ff);
    CuAssert(tc, "gfxFontFactoryDelete() - font factory deletion failed.",
            result == MPE_GFX_ERROR_NOERR);
}

/*
 ** Check for Null parameters
 */
void test_gfx_FontFactNewDeleteNull(CuTest *tc)
{
    mpe_Error result = 0;

    result = gfxFontFactoryNew(NULL);
    ASSERTFAIL( gfxFontFactoryNew);

    result = gfxFontFactoryDelete(NULL);
    ASSERTFAIL( gfxFontFactoryDelete);
}

/*
 ** Add many fonts then delete the fonts
 */
void test_gfx_FontFactoryAdd(CuTest *tc)
{
    mpe_Error result = 0;
    mpe_GfxFontDesc fd;
    mpe_GfxFontFactory ff;

    result = gfxFontFactoryNew(&ff);
    ASSERT( gfxFontFactoryNew);

    if ((result = getFont(&fd, dialogFont, tc)) == TRUE)
    {
        fd.name = &dialogFontName[0];

        result = gfxFontFactoryAdd(ff, &fd);
        ASSERT( gfxFontFactoryAdd);
    }

    if ((result = getFont(&fd, ameliaFont, tc)) == TRUE)
    {
        fd.name = &ameliaFontName[0];

        result = gfxFontFactoryAdd(ff, &fd);
        ASSERT( gfxFontFactoryAdd);
    }

    if ((result = getFont(&fd, dialoginputFont, tc)) == TRUE)
    {
        fd.name = &dialoginputFontName[0];

        result = gfxFontFactoryAdd(ff, &fd);
        ASSERT( gfxFontFactoryAdd);
    }

    if ((result = getFont(&fd, monospacedFont, tc)) == TRUE)
    {
        fd.name = &monospacedFontName[0];

        result = gfxFontFactoryAdd(ff, &fd);
        ASSERT( gfxFontFactoryAdd);
    }
    if ((result = getFont(&fd, sansserifFont, tc)) == TRUE)
    {
        fd.name = &sansserifFontName[0];

        result = gfxFontFactoryAdd(ff, &fd);
        ASSERT( gfxFontFactoryAdd);
    }
    if ((result = getFont(&fd, serifFont, tc)) == TRUE)
    {
        fd.name = &serifFontName[0];

        result = gfxFontFactoryAdd(ff, &fd);
        ASSERT( gfxFontFactoryAdd);
    }
    if ((result = getFont(&fd, tiresiasFont, tc)) == TRUE)
    {
        fd.name = &tiresiasFontName[0];

        result = gfxFontFactoryAdd(ff, &fd);
        ASSERT( gfxFontFactoryAdd);
    }
    result = gfxFontFactoryDelete(ff);
    ASSERT( gfxFontFactoryDelete);
}

/*
 ** Test mpeos_gfxFontGetList() function. This function returns the head of the
 font list in the system wide font factory (sys_fontfactory). This should
 point to the default system font (Tiresias). This is the easiest way to 
 test this function.
 */
void test_gfx_FontGetList(CuTest *tc)
{
    mpe_Error resulta = 0;
    mpe_Error resultb = 0;
    mpe_GfxFontDesc *fd;

    resulta = gfxFontGetList(&fd);
    resultb = wcstringnicmp(tiresiasFontName, fd->name, // wide-compare fonts
            tiresiasFontNameSize, fd->namelength); // passed if result 0
    CuAssert(tc, "Get System Font List Head Failed", (resulta == 0 && resultb
            == 0));
}

/*
 ** If nothing matches for NewFont() from a system font, the default will
 ** be  returned.
 ** concern: Why can't fd->name be null if the default font is always 
 ** returned if there is no match?
 */
void test_gfx_FontDefaultFFAlwaysReturnSomething(CuTest *tc)
{
    mpe_Error result = 0;
    mpe_GfxFont fontH;
    mpe_GfxFontDesc fd;
    char * foo = "foo";

    result = getFont(&fd, ameliaFont, tc);
    if (result)
    {
        fd.name = (unsigned short *) foo;

        result = gfxFontNew(NULL, (mpe_GfxWchar*) fd.name, fd.namelength,
                (mpe_GfxFontStyle) fd.style, fd.maxsize, &fontH);
        ASSERT( gfxFontNew);

        CuAssert(tc, "Font Handle 'NULL' verification: FAILED", fontH != NULL);

        result = gfxFontDelete(fontH);
        ASSERT( gfxFontDelete);
    }
}

/*
 ** Verify that calling FontNew() twice for the same font returns the same handle.  The
 ** font isn't created twice.  Verify for System and Created Font Factories.
 */
void test_gfx_FontMutiNewSameHandle(CuTest *tc)
{
    mpe_Error result = 0;
    mpe_GfxFontDesc fd;
    mpe_GfxFontFactory ff;
    mpe_GfxFont fontH1;
    mpe_GfxFont fontH2;
    mpe_GfxFont fontH3;
    mpe_GfxFont fontH4;

    // create font factory
    result = gfxFontFactoryNew(&ff);
    ASSERT( gfxFontFactoryNew);

    if (getFont(&fd, dialogFont, tc) == TRUE)
    {
        fd.name = &dialogFontName[0];
        fd.namelength = dialogFontNameSize;

        // add a font to the font factory
        result = gfxFontFactoryAdd(ff, &fd);
        ASSERT( gfxFontFactoryAdd);

        // test system font factory (use pre-defined system font)
        result = gfxFontNew(NULL, tiresiasFontName, tiresiasFontNameSize,
                MPE_GFX_PLAIN, 24, &fontH1);
        ASSERT( gfxFontNew);

        result = gfxFontNew(NULL, tiresiasFontName, tiresiasFontNameSize,
                MPE_GFX_PLAIN, 24, &fontH2);
        ASSERT(gfxFontNew);

        TRACE(
                MPE_LOG_INFO,
                MPE_MOD_TEST,
                "test_gfx_FontMutiNewSameHandle (System FF) - fontH1: 0x%x fontH2: 0x%x\n",
                fontH1, fontH2);
        CuAssert(
                tc,
                "Multiple calls to new for same font return diff font Handles - System FF",
                fontH1 == fontH2);

        // test created font factory (use created and added font)
        result = gfxFontNew(ff, dialogFontName, dialogFontNameSize,
                MPE_GFX_PLAIN, 24, &fontH3);
        ASSERT(gfxFontNew);

        result = gfxFontNew(ff, dialogFontName, dialogFontNameSize,
                MPE_GFX_PLAIN, 24, &fontH4);
        ASSERT(gfxFontNew);

        TRACE(
                MPE_LOG_INFO,
                MPE_MOD_TEST,
                "test_gfx_FontMutiNewSameHandle (Created FF) - fontH3: 0x%x fontH4: 0x%x\n",
                fontH3, fontH4);
        CuAssert(
                tc,
                "Multiple calls to new for same font return diff font Handles - Created FF",
                fontH3 == fontH4);

        // delete the fonts
        gfxFontDelete(fontH1);
        gfxFontDelete(fontH2);
        gfxFontDelete(fontH3);
        gfxFontDelete(fontH4);
    }

    // delete font factory
    result = gfxFontFactoryDelete(ff);
    ASSERT( gfxFontFactoryDelete);
}

void test_gfx_FontFactoryVerifyDistinctiveness(CuTest *tc)
{
    mpe_Error result = 0;
    mpe_GfxFontDesc fd1;
    mpe_GfxFontDesc fd2;
    mpe_GfxFontDesc fd3;
    mpe_GfxFontFactory ff1;
    mpe_GfxFontFactory ff2;
    mpe_GfxFont fontH1;
    mpe_GfxFont fontH2;

    result = gfxFontFactoryNew(&ff1);
    ASSERT( gfxFontFactoryNew);

    result = gfxFontFactoryNew(&ff2);
    ASSERT(gfxFontFactoryNew);

    if ((result = getFont(&fd1, dialogFont, tc)) == TRUE)
    {
        fd1.name = &dialogFontName[0];
        fd1.namelength = dialogFontNameSize;

        result = gfxFontFactoryAdd(ff1, &fd1);
        ASSERT( gfxFontFactoryAdd);
    }

    if ((result = getFont(&fd2, dialogFont, tc)) == TRUE)
    {
        fd2.name = &dialogFontName[0];
        fd2.namelength = dialogFontNameSize;

        result = gfxFontFactoryAdd(ff2, &fd2);
        ASSERT( gfxFontFactoryAdd);
    }

    if ((result = getFont(&fd3, ameliaFont, tc)) == TRUE)
    {
        fd3.name = &ameliaFontName[0];

        result = gfxFontFactoryAdd(ff1, &fd3);
        ASSERT( gfxFontFactoryAdd);
    }

    /* ff1 contains two fonts and ff2 has one font.  Verify this by making sure
     ** ff2s font has only one element.
     */
    CuAssert(tc, "Verify that ff2 has only one font: FAILED", fd3.prev
            == fd3.next);

    /*
     ** Now create two fonts, one from ff1 and one from ff2 and verify that the handles are 
     ** different.  This will verify that the font handles are not being returned from the 
     ** same font factory, because multiple create calls to a font factory for the same 
     ** font returns the same font handle.
     */

    result = gfxFontNew(ff1, (mpe_GfxWchar*) fd1.name, fd1.namelength,
            (mpe_GfxFontStyle) fd1.style, fd1.maxsize, &fontH1);
    ASSERT( gfxFontNew);

    result = gfxFontNew(ff2, (mpe_GfxWchar*) fd2.name, fd2.namelength,
            (mpe_GfxFontStyle) fd2.style, fd2.maxsize, &fontH2);
    ASSERT(gfxFontNew);

    CuAssert(
            tc,
            "Same font created from different font fact-s returns diff handles: FAIL",
            fontH1 != fontH2);

    gfxFontDelete(fontH1);
    ASSERT( gfxFontDelete);
    gfxFontDelete(fontH2);
    ASSERT(gfxFontDelete);

    result = gfxFontFactoryDelete(ff1);
    ASSERT( gfxFontFactoryDelete);

    result = gfxFontFactoryDelete(ff2);
    ASSERT(gfxFontFactoryDelete);
}

/*
 **  Add the same font multiple times to the same font factory
 */
void test_gfx_FontFactorySameFontMultiTimes(CuTest *tc)
{
    mpe_Error result = 0;
    mpe_GfxFontDesc fd;
    mpe_GfxFontFactory ff;
    mpe_GfxFont fontH;
    int ii;

    result = gfxFontFactoryNew(&ff);
    ASSERT( gfxFontFactoryNew);

    if ((result = getFont(&fd, ameliaFont, tc)) == TRUE)
    {
        fd.name = &ameliaFontName[0];
        fd.namelength = ameliaFontNameSize;

        for (ii = 0; ii < 2; ii++)
        {
            TRACE(
                    MPE_LOG_INFO,
                    MPE_MOD_TEST,
                    "test_gfx_FontFactorySameFontMultiTimes - fontFact add count: %d\n",
                    ii);
            result = gfxFontFactoryAdd(ff, &fd);
            if (ii == 0)
            {
                ASSERT( gfxFontFactoryAdd);
                result = gfxFontNew(ff, (mpe_GfxWchar*) fd.name, fd.namelength,
                        (mpe_GfxFontStyle) fd.style, fd.maxsize, &fontH);
                ASSERT( gfxFontNew);
            }
            if (ii == 1)
                CuAssert(tc, "Added the same font multiple times to a FF\n",
                        result != MPE_SUCCESS);
        }
    }
    result = gfxFontFactoryDelete(ff);
    ASSERT( gfxFontFactoryDelete);
}

/**
 * Create and return the test suite for the mpe_gfx APIs.
 * @return a pointer to the new test suite.
 */
CuSuite* getTestSuite_gfxfontfact(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_STOP_TEST(suite, test_gfx_FontFactNewDelete);
    SUITE_STOP_TEST(suite, test_gfx_FontFactNewDeleteNull);
    SUITE_STOP_TEST(suite, test_gfx_FontFactoryAdd);
    SUITE_STOP_TEST(suite, test_gfx_FontGetList);
    SUITE_STOP_TEST(suite, test_gfx_FontFactNewDifferentInstance);
    SUITE_STOP_TEST(suite, test_gfx_FontNewDelete);
    SUITE_STOP_TEST(suite, test_gfx_FontNewDeleteValidFontFactory);
    SUITE_STOP_TEST(suite, test_gfx_FontNewDeleteInValidFontFactory);
    SUITE_STOP_TEST(suite, test_gfx_FontFactNeverSystem);
    SUITE_STOP_TEST(suite, test_gfx_FontNotFoundNull);
    SUITE_STOP_TEST(suite, test_gfx_FontDefaultFFAlwaysReturnSomething);
    SUITE_STOP_TEST(suite, test_gfx_FontMutiNewSameHandle);
    SUITE_STOP_TEST(suite, test_gfx_FontFactoryVerifyDistinctiveness);
    SUITE_STOP_TEST(suite, test_gfx_FontFactorySameFontMultiTimes);

    return suite;
}

/*------------------------------------------------------------------------------
 Runs a single test case.
 ------------------------------------------------------------------------------*/
void test_gfxRunSingleTest(char *iTestCaseName, char *iTestCaseFuncName)
{
    CuSuite *suite;
    CuString* output;

    suite = CuSuiteRunTestCase(getTestSuite_gfxfontfact, iTestCaseFuncName);

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n%s\n", iTestCaseFuncName,
            output->buffer);
    vte_agent_Log("Test results: %s\n%s\n", iTestCaseFuncName, output->buffer);

    CuSuiteFree(suite);
}

/*------------------------------------------------------------------------------
 Define a test case.  Prepends the test case name with "test_" to generate 
 the test case function name.

 Example:
 DEFINE_TEST_CASE(gfx_FontFactoryDoSomething);

 Expands to:
 void test_gfx_FontFactoryDoSomethingTest(void)
 {
 test_gfxRunSingleTest("gfx_FontFactoryDoSomething", "test_gfx_FontFactoryDoSomething");
 }
 ------------------------------------------------------------------------------*/
#define DEFINE_TEST_CASE(iTestCaseName)											\
	void test_ ## iTestCaseName ## Test(void)									\
	{																			\
		test_gfxRunSingleTest(#iTestCaseName, "test_" #iTestCaseName);			\
	}

/*------------------------------------------------------------------------------
 Define all font factory test cases here.
 ------------------------------------------------------------------------------*/
DEFINE_TEST_CASE(gfx_FontFactNewDelete)
DEFINE_TEST_CASE(gfx_FontFactNewDeleteNull)
DEFINE_TEST_CASE(gfx_FontFactoryAdd)
DEFINE_TEST_CASE(gfx_FontGetList)
DEFINE_TEST_CASE(gfx_FontFactNewDifferentInstance)
DEFINE_TEST_CASE(gfx_FontNewDelete)
DEFINE_TEST_CASE(gfx_FontNewDeleteValidFontFactory)
DEFINE_TEST_CASE(gfx_FontNewDeleteInValidFontFactory)
DEFINE_TEST_CASE(gfx_FontFactNeverSystem)
DEFINE_TEST_CASE(gfx_FontNotFoundNull)
DEFINE_TEST_CASE(gfx_FontDefaultFFAlwaysReturnSomething)
DEFINE_TEST_CASE(gfx_FontMutiNewSameHandle)
DEFINE_TEST_CASE(gfx_FontFactoryVerifyDistinctiveness)
DEFINE_TEST_CASE(gfx_FontFactorySameFontMultiTimes)

/**
 * Compares 2 wide character strings upto a given number of characters.
 * The case does not matter.
 * @param   ws1     the first string
 * @param   ws2     the second string
 * @param   n1      the number of characters in ws1
 * @param   n2      the number of characters in ws2
 * @return          
 *          < 0 ws1 lesser than ws2 
 *            0 ws1 identical to ws2 
 *          > 0 ws1 greater than ws2 
 */
int wcstringnicmp(const mpe_GfxWchar *ws1, const mpe_GfxWchar *ws2, size_t n1,
        size_t n2)
{
    int cmp;
    mpe_GfxWchar *end;

    int count = n1;

    if (n1 != n2)
        return (n1 - n2);

    /* should check parameters here !!! */
    end = (mpe_GfxWchar *) ws1 + count;
    do
    {
        cmp = tolower((int) *ws1) - tolower((int) *ws2);
    } while (*ws1++ && *ws2++ && cmp == 0 && ws1 < end);
    return (cmp);
}

