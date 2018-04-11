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

#define ABS abs

static void setup(mpe_GfxSurface *psurf, mpe_GfxSurfaceInfo *info);
static void teardown(mpe_GfxSurface surf);
void test_gfxSurface(CuTest *tc);
void test_gfxSurfaceNew(CuTest *tc);
void test_gfxSurfaceCreate(CuTest *tc);
void test_gfxSurfaceDelete(CuTest *tc);
void test_gfxSurfaceGetInfo(CuTest *tc);
void test_gfxPaletteNew(CuTest *tc);
void test_gfxPaletteDelete(CuTest *tc);
void test_gfxPaletteGet(CuTest *tc);
void test_gfxPaletteSet(CuTest *tc);
void test_gfxPaletteMatch(CuTest *tc);
CuSuite* getTestSuite_gfxSurface(void);
void test_gfxRunSurfaceTests(void);
void test_gfxRunSurfaceTest(void);
void test_gfxRunSurfaceNewTest(void);
void test_gfxRunSurfaceCreateTest(void);
void test_gfxRunSurfaceDeleteTest(void);
void test_gfxRunSurfaceGetInfoTest(void);
void test_gfxRunPaletteNewTest(void);
void test_gfxRunPaletteDeleteTest(void);
void test_gfxRunPaletteGetTest(void);
void test_gfxRunPaletteSetTest(void);
void test_gfxRunPaletteMatchTest(void);

static mpe_DispDevice getDefaultGfx(void);

#define CuAssertInfoEquals(tc, ms, ex, ac)                                  \
do {                                                                        \
    CuAssertIntEquals_Msg(tc, ms " - color format",                         \
                          ex.format, ac.format);                            \
    CuAssertIntEquals_Msg(tc, ms " - bpp",                                  \
                          ex.bpp, ac.bpp);                                  \
    CuAssertIntEquals_Msg(tc, ms " - dim.width",                            \
                          ex.dim.width, ac.dim.width);                      \
    CuAssertIntEquals_Msg(tc, ms " - dim.height",                           \
                          ex.dim.height, ac.dim.height);                    \
    CuAssertIntEquals_Msg(tc, ms " - lineWidth",                            \
                          ex.widthbytes, ac.widthbytes);                    \
    CuAssert(tc, ms " - distinct pixel data expected",                      \
                ex.pixeldata != ac.pixeldata);                              \
} while(0)

static void setup(mpe_GfxSurface *psurf, mpe_GfxSurfaceInfo *info)
{
    mpe_GfxSurface surf;
    if (dispGetGfxSurface(getDefaultGfx(), &surf) != MPE_SUCCESS)
        *psurf = 0;
    else
    {
        *psurf = surf;
        if (info != NULL)
            gfxSurfaceGetInfo(surf, info);
    }
}

#define BAD_SURFACE (mpe_GfxSurface)-1

static void teardown(mpe_GfxSurface surf)
{
    if (surf != 0 && surf != BAD_SURFACE)
        gfxSurfaceDelete(surf);
}

static void teardownPalette(mpe_GfxPalette palette)
{
    if (palette != NULL)
    {
        (void) gfxPaletteDelete(palette);
    }
}

/**
 * Tests the mpeos_gfxSurface() call.
 *
 * @param tc pointer to test case structure
 */
void test_gfxSurface(CuTest *tc)
{
    /* mpe_GfxSurface base;
     mpeos_GfxSurface *pNewSurface;
     mpe_GfxSurfaceInfo info = {(mpe_GfxColorFormat)-1};
     mpe_GfxSurfaceInfo savedInfo;
     mpe_Error ec;

     TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "entering gfxSurface Test\n");


     CuTestSetup(setup(&base, &info));
     CuAssert(tc, "Could not get base surface/info", base != 0 && info.format != -1);

     savedInfo = info;
     info.clut = NULL;
     info.format = MPE_GFX_CLUT8;
     pNewSurface = gfxSurface( &info, false );
     CuAssert(tc, "gfxSurface should have failed with the null CLUT", pNewSurface == NULL );

     // Restore the saved surface info
     info = savedInfo;

     // Test creation of a surface with primary false
     pNewSurface = gfxSurface( &info, false );
     CuAssert(tc, "Surface creation should have succeeded", pNewSurface != NULL );
     CuAssert(tc, "Surface width should have matched", pNewSurface->width == info.dim.width );
     CuAssert(tc, "Surface height should have matched", pNewSurface->height == info.dim.height );
     CuAssert(tc, "Surface BPP should have matched", pNewSurface->bpp == info.bpp );
     CuAssert(tc, "Surface color format should have matched", pNewSurface->colorFormat == info.format );
     CuAssert(tc, "Surface color palette clut should have matched", pNewSurface->clut == info.clut );

     ec = gfxSurfaceDelete( (mpe_GfxSurface) pNewSurface );
     CuAssert(tc, "gfxSurfaceDelete failed", ec == MPE_SUCCESS);

     // Test creation of a surface with primary true
     pNewSurface = gfxSurface( &info, true );
     CuAssert(tc, "Surface creation should have succeeded", pNewSurface != NULL );
     CuAssert(tc, "Surface width should have matched", pNewSurface->width == info.dim.width );
     CuAssert(tc, "Surface height should have matched", pNewSurface->height == info.dim.height );
     CuAssert(tc, "Surface BPP should have matched", pNewSurface->bpp == info.bpp );
     CuAssert(tc, "Surface color format should have matched", pNewSurface->colorFormat == info.format );
     CuAssert(tc, "Surface color palette clut should have matched", pNewSurface->clut == info.clut );

     ec = gfxSurfaceDelete( (mpe_GfxSurface) pNewSurface );
     CuAssert(tc, "gfxSurfaceDelete failed", ec == MPE_SUCCESS); 

     TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "exiting gfxSurface Test\n"); */

    CuAssert(tc, "Due to linkage issues this test cannot be run", 0);

}

/**
 * Tests the mpeos_gfxSurfaceNew() call.
 *
 * @param tc pointer to test case structure
 */
void test_gfxSurfaceNew(CuTest *tc)
{
    mpe_GfxSurface base;
    mpe_GfxSurface surface = BAD_SURFACE;
    mpe_GfxSurfaceInfo info =
    { (mpe_GfxColorFormat) - 1 };
    mpe_Error ec;

    CuTestSetup(setup(&base, &info));
    CuAssert(tc, "Could not get base surface/info", base != 0 && info.format
            != -1);

    ec = gfxSurfaceNew(&info, &surface);
    CuAssert(tc, "gfxSurfaceNew failed", ec == MPE_SUCCESS);
    CuAssert(tc, "gfxSurfaceNew returned an invalid surface", surface
            != BAD_SURFACE);

    /* Test surface construction from an arbitrary description */
    gfxSurfaceDelete(surface);
    surface = BAD_SURFACE;
    info.dim.width = 100;
    info.dim.width = 100;
    ec = gfxSurfaceNew(&info, &surface);
    CuAssert(tc, "gfxSurfaceNew failed", ec == MPE_SUCCESS);
    CuAssert(tc, "gfxSurfaceNew returned an invalid surface", surface
            != BAD_SURFACE);

    /* !!!! Test failure for unsupported parameters !!!! */

    CuTestCleanup(teardown(surface));
}

/**
 * Tests the gfxSurfaceCreate() call.
 *
 * @param tc pointer to test case structure
 */
void test_gfxSurfaceCreate(CuTest *tc)
{
    mpe_GfxSurface base;
    mpe_GfxSurface surface = BAD_SURFACE;
    mpe_Error ec;

    CuTestSetup(setup(&base, NULL));
    CuAssert(tc, "Could not get base surface/info", base != 0);

    ec = gfxSurfaceCreate(base, &surface);
    CuAssert(tc, "gfxSurfaceCreate failed", ec == MPE_SUCCESS);
    CuAssert(tc, "gfxSurfaceCreate returned an invalid surface", surface
            != BAD_SURFACE);
    CuAssert(tc, "gfxSurfaceCreate should NOT return the base surface", surface
            != base);

    CuTestCleanup(teardown(surface));
}

/**
 * Tests the gfxSurfaceDelete() call.
 *
 * @param tc pointer to test case structure
 */
void test_gfxSurfaceDelete(CuTest *tc)
{
    mpe_GfxSurface base;
    mpe_GfxSurface surface = BAD_SURFACE;
    mpe_Error ec;

    CuTestSetup(setup(&base, NULL));
    ec = gfxSurfaceCreate(base, &surface);
    CuAssert(tc, "gfxSurfaceCreate failed", ec == MPE_SUCCESS);

    ec = gfxSurfaceDelete(surface);
    surface = 0; // if delete failed, don't do again in cleanup...
    CuAssert(tc, "gfxSurfaceDelete failed", ec == MPE_SUCCESS);

    /* Can we test that it WAS destroyed??? */

    CuTestCleanup(teardown(surface));
}

/**
 * Tests the gfxSurfaceGetInfo() call.
 *
 * @param tc pointer to test case structure
 */
void test_gfxSurfaceGetInfo(CuTest *tc)
{
    mpe_GfxSurface base;
    mpe_GfxSurface surface = BAD_SURFACE;
    mpe_GfxSurfaceInfo info =
    { (mpe_GfxColorFormat) - 1 };
    mpe_GfxSurfaceInfo info2 =
    { (mpe_GfxColorFormat) - 1 };
    mpe_Error ec;

    CuTestSetup(setup(&base, &info));
    CuAssert(tc, "Could not get base surface/info", base != 0 && info.format
            != -1);

    // Create with create
    ec = gfxSurfaceCreate(base, &surface);
    CuAssert(tc, "gfxSurfaceCreate failed", ec == MPE_SUCCESS);
    CuAssert(tc, "gfxSurfaceCreate returned an invalid surface", surface
            != BAD_SURFACE);
    CuAssert(tc, "gfxSurfaceCreate should NOT return the base surface", surface
            != base);

    // Get info and compare
    memset(&info2, -1, sizeof info2);
    ec = gfxSurfaceGetInfo(surface, &info2);
    CuAssert(tc, "gfxSurfaceGetInfo failed", ec == MPE_SUCCESS);
    CuAssertInfoEquals(tc, "Expected same info as from base on create", info, info2);

    // Since all based on screen verify as for screen
    // NOTE: this code is near identical to code in test_gfxScreenGetSurface()!!!
    CuAssertIntEquals_Msg(tc, "Unexpected screen surface width", 640,
            info2.dim.width);
    CuAssertIntEquals_Msg(tc, "Unexpected screen surface height", 480,
            info2.dim.height);
    switch (info2.bpp)
    {
    case MPE_GFX_1BPP:
    case MPE_GFX_8BPP:
    case MPE_GFX_16BPP:
    case MPE_GFX_24BPP:
    case MPE_GFX_32BPP:
        break;
    default:
        CuFail(tc, "Unknown screen surface pixel depth");
    }
    CuAssertIntEquals_Msg(tc, "Unexpected line width in bytes, given depth",
            (info2.dim.width * info2.bpp) / 8, info2.widthbytes);
    CuAssertPtrNotNullMsg(tc, "Pixel data should be non-null", info2.pixeldata);
    switch (info2.format)
    {
    case MPE_GFX_RGB888:
    case MPE_GFX_RGB565:
    case MPE_GFX_ARGB1555:
    case MPE_GFX_ARGB8888:
        break;
    default:
        CuFail(tc, "Unknown screen surface pixel format");
    }

    // Reset
    gfxSurfaceDelete(surface);
    surface = BAD_SURFACE;

    // Create with New, using different dimensions
    info.dim.width = 1000;
    info.dim.height = 30;
    ec = gfxSurfaceNew(&info, &surface);
    CuAssert(tc, "gfxSurfaceNew failed", ec == MPE_SUCCESS);
    CuAssert(tc, "gfxSurfaceNew returned an invalid surface", surface
            != BAD_SURFACE);

    info.widthbytes = (info.dim.width * info.bpp) / 8; // expected to be set!!

    // Get info and compare
    memset(&info2, 0, sizeof info2);
    ec = gfxSurfaceGetInfo(surface, &info2);
    CuAssert(tc, "gfxSurfaceGetInfo failed", ec == MPE_SUCCESS);
    CuAssertInfoEquals(tc, "Expected same info as passed to new", info, info2);

    CuTestCleanup(teardown(surface));
}

/**
 * Tests the mpeos_gfxPaletteNew() call.
 *
 * @param tc pointer to test case structure
 */
void test_gfxPaletteNew(CuTest *tc)
{
    mpe_Error ec;
    mpe_GfxPalette * pNullPalette = NULL;
    mpe_GfxPalette palette;
    int illegalNumColors = -1;
    int legalNumColors = 255;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "entering PaletteNew Test\n");

    ec = gfxPaletteNew(legalNumColors, pNullPalette);
    CuAssert(tc, "gfxPaletteNew should have failed - null pPalette", ec
            != MPE_GFX_ERROR_NOERR);

    ec = gfxPaletteNew(illegalNumColors, &palette);
    CuAssert(tc, "gfxPaletteNew should have failed - illegal num colors ", ec
            != MPE_GFX_ERROR_NOERR);

    // Now try for a successful new palette
    ec = gfxPaletteNew(legalNumColors, &palette);
    CuAssert(tc, "gfxPaletteNew should have succeeded ", ec
            == MPE_GFX_ERROR_NOERR);

    // Delete the new palette that we have created
    CuTestCleanup(teardownPalette(palette));

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "exiting PaletteNew Test\n");
}

/**
 * Tests the mpeos_gfxPaletteDelete() call.
 *
 * @param tc pointer to test case structure
 */
void test_gfxPaletteDelete(CuTest *tc)
{
    mpe_Error ec;
    mpe_GfxPalette palette;
    int legalNumColors = 255;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "entering PaletteDelete Test\n");

    // Now get a new palette so that we can delete it
    ec = gfxPaletteNew(legalNumColors, &palette);
    CuAssert(tc, "gfxPaletteNew should have succeeded ", ec
            == MPE_GFX_ERROR_NOERR);

    ec = gfxPaletteDelete((mpe_GfxPalette) NULL);
    CuAssert(tc, "gfxPaletteDelete should have failed - null Palette", ec
            != MPE_GFX_ERROR_NOERR);

    ec = gfxPaletteDelete(palette);
    CuAssert(tc, "gfxPaletteDelete should have succeeded ", ec
            == MPE_GFX_ERROR_NOERR);

    // Delete the new palette that we have created if it's not null
    CuTestCleanup(teardownPalette(palette));

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "exiting PaletteDelete Test\n");
}

/**
 * Tests the mpeos_gfxPaletteGet() call.
 *
 * @param tc pointer to test case structure
 */
void test_gfxPaletteGet(CuTest *tc)
{
    mpe_Error ec;
    mpe_GfxPalette palette = 0;
    int legalNumColors = 256;
    mpe_GfxColor colors[256];
    mpe_GfxColor outColors[256];
    int offset = 0;
    int i;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "entering PaletteGet Test\n");

    ec = gfxPaletteGet((mpe_GfxPalette) NULL, legalNumColors, offset, colors);
    CuAssert(tc, "gfxPaletteGet should have failed - null Palette", ec
            != MPE_GFX_ERROR_NOERR);

    // Get a real palette to point to 
    ec = gfxPaletteNew(legalNumColors, &palette);
    CuAssert(tc, "gfxPaletteNew should have succeeded ", ec
            == MPE_GFX_ERROR_NOERR);

    ec = gfxPaletteGet(palette, legalNumColors, offset, NULL);
    CuAssert(tc, "gfxPaletteGet should have failed - null colors array", ec
            != MPE_GFX_ERROR_NOERR);

    ec = gfxPaletteGet(palette, legalNumColors, -5, colors);
    CuAssert(tc, "gfxPaletteGet should have failed - negative offset", ec
            != MPE_GFX_ERROR_NOERR);

    ec = gfxPaletteGet(palette, -5, offset, colors);
    CuAssert(tc, "gfxPaletteGet should have failed - negative number colors",
            ec != MPE_GFX_ERROR_NOERR);

    /* 
     * Set the palette with known colors, then get the colors back, they should be the same 
     */
    // We need to load the colors array values, 8bits|8bits|8bits|8bits, then check what will be in the palette.
    for (i = 0; i < legalNumColors; ++i)
    {
        colors[i] = ((uint8_t) i << 24) | ((uint8_t) i << 16) | ((uint8_t) i
                << 8) | ((uint8_t) i);
    }

    ec = gfxPaletteSet(palette, colors, legalNumColors, offset);
    CuAssert(tc, "gfxPaletteSet should have succeeded", ec
            == MPE_GFX_ERROR_NOERR);

    ec = gfxPaletteGet(palette, legalNumColors, offset, outColors);
    CuAssert(tc, "gfxPaletteGet should have succeeded", ec
            == MPE_GFX_ERROR_NOERR);

    for (i = 0; i < legalNumColors; ++i)
    {
        CuAssert(tc, "Input colors should match output colors", colors[i]
                == outColors[i]);
    }

    // Delete the new palette that we have created if it's not null
    CuTestCleanup(teardownPalette(palette));

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "exiting PaletteGet Test\n");
}

/**
 * Tests the mpeos_gfxPaletteSet() call.
 *
 * @param tc pointer to test case structure
 */
void test_gfxPaletteSet(CuTest *tc)
{
    mpe_Error ec;
    mpe_GfxPalette palette = 0;
    int legalNumColors = 256;
    mpe_GfxColor colors[256];
    mpe_GfxColor outColors[256];
    int offset = 0;
    int i;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "entering PaletteSet Test\n");

    ec = gfxPaletteSet((mpe_GfxPalette) NULL, colors, legalNumColors, offset);
    CuAssert(tc, "gfxPaletteSet should have failed - null Palette", ec
            != MPE_GFX_ERROR_NOERR);

    // Get a real palette to point to 
    ec = gfxPaletteNew(legalNumColors, &palette);
    CuAssert(tc, "gfxPaletteNew should have succeeded ", ec
            == MPE_GFX_ERROR_NOERR);

    ec = gfxPaletteSet(palette, NULL, legalNumColors, offset);
    CuAssert(tc, "gfxPaletteSet should have failed - null colors array", ec
            != MPE_GFX_ERROR_NOERR);

    ec = gfxPaletteSet(palette, colors, legalNumColors, -5);
    CuAssert(tc, "gfxPaletteSet should have failed - negative offset", ec
            != MPE_GFX_ERROR_NOERR);

    ec = gfxPaletteSet(palette, colors, -5, offset);
    CuAssert(tc, "gfxPaletteSet should have failed - negative number colors",
            ec != MPE_GFX_ERROR_NOERR);

    // We need to load the colors array values, 8bits|8bits|8bits|8bits, then check what will be in the palette.
    for (i = 0; i < legalNumColors; ++i)
    {
        colors[i] = ((uint8_t) i << 24) | ((uint8_t) i << 16) | ((uint8_t) i
                << 8) | ((uint8_t) i);
    }

    ec = gfxPaletteSet(palette, colors, legalNumColors, offset);
    CuAssert(tc, "gfxPaletteSet should have succeeded", ec
            == MPE_GFX_ERROR_NOERR);

    //  What should be in the palette - I guess we don't care, compare apples to apples..
    ec = gfxPaletteGet(palette, legalNumColors, offset, outColors);
    CuAssert(tc, "gfxPaletteGet should have succeeded", ec
            == MPE_GFX_ERROR_NOERR);

    for (i = 0; i < legalNumColors; ++i)
    {
        CuAssert(tc, "Input colors should match output colors", colors[i]
                == outColors[i]);
    }

    // Delete the new palette that we have created if it's not null
    CuTestCleanup(teardownPalette(palette));

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "exiting PaletteSet Test\n");
}

/**
 * Tests the mpeos_gfxPaletteMatch() call.
 *
 * @param tc pointer to test case structure
 */
void test_gfxPaletteMatch(CuTest *tc)
{
    mpe_Error ec;
    mpe_GfxPalette palette = 0;
    int legalNumColors = 256;
    mpe_GfxColor colors[256];
    mpe_GfxColor colorToMatch;
    int offset = 0;
    int i;
    int index;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "entering PaletteMatch Test\n");

    // Get a real palette to point to 
    ec = gfxPaletteNew(legalNumColors, &palette);
    CuAssert(tc, "gfxPaletteNew should have succeeded ", ec
            == MPE_GFX_ERROR_NOERR);

    // We need to load the colors array values, 8bits|8bits|8bits|8bits, then set the palette.
    for (i = legalNumColors - 1; i >= 0; --i)
    {
        colors[i] = ((uint8_t) i << 24) | ((uint8_t) i << 16) | ((uint8_t) i
                << 8) | ((uint8_t) i);
    }

    ec = gfxPaletteSet(palette, colors, legalNumColors, offset);
    CuAssert(tc, "gfxPaletteSet should have succeeded", ec
            == MPE_GFX_ERROR_NOERR);

    // Test some scattered instances, that should match within one or two of what we expect
    colorToMatch = mpe_gfxArgbToColor(0x7F, 0x7E, 0x7F, 0x7E);
    ec = gfxPaletteMatch(palette, colorToMatch, &index);
    CuAssert(tc, "gfxPaletteMatch should have succeeded", ec
            == MPE_GFX_ERROR_NOERR);
    CuAssert(tc, "gfxPaletteMatch should have returned closer match", ABS(127
            - index) <= 2);

    colorToMatch = mpe_gfxArgbToColor(0x1, 0x2, 0x1, 0x2);
    ec = gfxPaletteMatch(palette, colorToMatch, &index);
    CuAssert(tc, "gfxPaletteMatch should have succeeded", ec
            == MPE_GFX_ERROR_NOERR);
    CuAssert(tc, "gfxPaletteMatch should have returned closer match", ABS(2
            - index) <= 2);

    colorToMatch = mpe_gfxArgbToColor(0xFF, 0xFF, 0xFE, 0xFE);
    ec = gfxPaletteMatch(palette, colorToMatch, &index);
    CuAssert(tc, "gfxPaletteMatch should have succeeded", ec
            == MPE_GFX_ERROR_NOERR);
    CuAssert(tc, "gfxPaletteMatch should have returned closer match", ABS(255
            - index) <= 2);

    // Delete the new palette that we have created if it's not null
    CuTestCleanup(teardownPalette(palette));

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "exiting PaletteMatch Test\n");
}

/**
 * Create and return the test suite for the mpe_gfx APIs.
 * @return a pointer to the new test suite.
 */
CuSuite* getTestSuite_gfxSurface(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_STOP_TEST(suite, test_gfxSurface);
    SUITE_STOP_TEST(suite, test_gfxSurfaceNew);
    SUITE_STOP_TEST(suite, test_gfxSurfaceCreate);
    SUITE_STOP_TEST(suite, test_gfxSurfaceDelete);
    SUITE_STOP_TEST(suite, test_gfxSurfaceGetInfo);
    SUITE_STOP_TEST(suite, test_gfxPaletteNew);
    SUITE_STOP_TEST(suite, test_gfxPaletteDelete);
    SUITE_STOP_TEST(suite, test_gfxPaletteGet);
    SUITE_STOP_TEST(suite, test_gfxPaletteSet);
    SUITE_STOP_TEST(suite, test_gfxPaletteMatch);

    return suite;
}

void test_gfxRunSurfaceTests(void)
{
    test_gfxRunSuite(getTestSuite_gfxSurface(), "SurfaceTests");
}

void test_gfxRunSurfaceTest(void)
{
    CuSuite* suite;

    suite = CuSuiteNewCloneTest(getTestSuite_gfxSurface(), "test_gfxSurface");
    test_gfxRunSuite(suite, "gfxSurface");

    CuSuiteFree(suite);
}

void test_gfxRunSurfaceNewTest(void)
{
    CuSuite* suite;

    suite
            = CuSuiteNewCloneTest(getTestSuite_gfxSurface(),
                    "test_gfxSurfaceNew");
    test_gfxRunSuite(suite, "gfxSurfaceNew");

    CuSuiteFree(suite);
}

void test_gfxRunSurfaceCreateTest(void)
{
    CuSuite* suite;

    suite = CuSuiteNewCloneTest(getTestSuite_gfxSurface(),
            "test_gfxSurfaceCreate");
    test_gfxRunSuite(suite, "gfxSurfaceCreate");

    CuSuiteFree(suite);
}

void test_gfxRunSurfaceDeleteTest(void)
{
    CuSuite* suite;

    suite = CuSuiteNewCloneTest(getTestSuite_gfxSurface(),
            "test_gfxSurfaceDelete");
    test_gfxRunSuite(suite, "gfxSurfaceDelete");

    CuSuiteFree(suite);
}

void test_gfxRunSurfaceGetInfoTest(void)
{
    CuSuite* suite;

    suite = CuSuiteNewCloneTest(getTestSuite_gfxSurface(),
            "test_gfxSurfaceGetInfo");
    test_gfxRunSuite(suite, "gfxSurfaceGetInfo");

    CuSuiteFree(suite);

}

void test_gfxRunPaletteNewTest(void)
{
    CuSuite* suite;

    suite
            = CuSuiteNewCloneTest(getTestSuite_gfxSurface(),
                    "test_gfxPaletteNew");
    test_gfxRunSuite(suite, "gfxPaletteNew");

    CuSuiteFree(suite);
}

void test_gfxRunPaletteDeleteTest(void)
{
    CuSuite* suite;

    suite = CuSuiteNewCloneTest(getTestSuite_gfxSurface(),
            "test_gfxPaletteDelete");
    test_gfxRunSuite(suite, "gfxPaletteDelete");

    CuSuiteFree(suite);
}

void test_gfxRunPaletteGetTest(void)
{
    CuSuite* suite;

    suite
            = CuSuiteNewCloneTest(getTestSuite_gfxSurface(),
                    "test_gfxPaletteGet");
    test_gfxRunSuite(suite, "gfxPaletteGet");

    CuSuiteFree(suite);
}

void test_gfxRunPaletteSetTest(void)
{
    CuSuite* suite;

    suite
            = CuSuiteNewCloneTest(getTestSuite_gfxSurface(),
                    "test_gfxPaletteSet");
    test_gfxRunSuite(suite, "gfxPaletteSet");

    CuSuiteFree(suite);
}

void test_gfxRunPaletteMatchTest(void)
{
    CuSuite* suite;

    suite = CuSuiteNewCloneTest(getTestSuite_gfxSurface(),
            "test_gfxPaletteMatch");
    test_gfxRunSuite(suite, "gfxPaletteMatch");

    CuSuiteFree(suite);
}

static mpe_DispDevice getDefaultGfx(void)
{
    static volatile int tried; /**< Avoid trying more than once */
    static volatile mpe_DispDevice gfx;
    if (!tried && NULL == (void*) gfx)
    {
        /* Get "default" screen */
        uint32_t nScreens = 0;
        mpe_DispScreen *screens = NULL;

        if (MPE_SUCCESS == dispGetScreenCount(&nScreens) && nScreens > 0
                && MPE_SUCCESS == memAllocP(MPE_MEM_TEMP, nScreens
                        * sizeof(mpe_DispScreen), (void**) &screens))
        {
            uint32_t nDevs = 0;
            mpe_DispDevice *devices = NULL;
            if (MPE_SUCCESS == dispGetScreens(screens) && MPE_SUCCESS
                    == dispGetDeviceCount(screens[0],
                            MPE_DISPLAY_GRAPHICS_DEVICE, &nDevs) && nDevs > 0
                    && MPE_SUCCESS == memAllocP(MPE_MEM_TEMP, nDevs
                            * sizeof(mpe_DispDevice), (void**) &devices))
            {
                if (MPE_SUCCESS == dispGetDevices(screens[0],
                        MPE_DISPLAY_GRAPHICS_DEVICE, devices))
                {
                    gfx = devices[0];
                }
            }
            if (devices != NULL)
                memFreeP(MPE_MEM_TEMP, devices);
        }
        if (screens != NULL)
            memFreeP(MPE_MEM_TEMP, screens);
        tried = 1;
    }
    return gfx;
}
