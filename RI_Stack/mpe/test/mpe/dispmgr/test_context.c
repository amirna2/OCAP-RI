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

#ifndef TEST_MPE_CONTEXT_PARAM
#define TEST_MPE_CONTEXT_PARAM 0
#endif

#ifndef MPE_GFX_NEG_CLIP_WH_LEGAL
#define MPE_GFX_NEG_CLIP_WH_LEGAL 0
#endif

void test_gfxContextNew(CuTest *tc);
void test_gfxContextCreate(CuTest *tc);
void test_gfxContextDelete(CuTest *tc);
void test_gfxGetSurface(CuTest *tc);
void test_gfxSetGetColor(CuTest *tc);
void test_gfxGetFont_Default(CuTest *tc);
void test_gfxSetFont_Clear(CuTest *tc);
void test_gfxSetGetFont(CuTest *tc);
void test_gfxSetGetPaintMode(CuTest *tc);
void test_gfxSetGetOrigin(CuTest *tc);
void test_gfxSetGetClipRect(CuTest *tc);
void test_gfxContext_DistinctState(CuTest *tc);
void test_gfxRunContextTests(void);
void test_gfxRunContextNewTest(void);
void test_gfxRunContextCreateTest(void);
void test_gfxRunContextDeleteTest(void);
void test_gfxRunGetSurfaceTest(void);
void test_gfxRunSetGetColorTest(void);
void test_gfxRunSetGetFontTest(void);
void test_gfxRunGetFont_DefaultTest(void);
void test_gfxRunSetFont_ClearTest(void);
void test_gfxRunSetGetPaintModeTest(void);
void test_gfxRunSetGetOriginTest(void);
void test_gfxRunSetGetClipRectTest(void);
void test_gfxRunContext_DistinctStateTest(void);
CuSuite* getTestSuite_gfxContext(void);

static mpe_DispDevice getDefaultGfx(void);

static void setup(mpe_GfxSurface* psurf, mpe_GfxContext* pctx)
{
    mpe_GfxSurface surf = 0;
    mpe_GfxContext ctx = 0;

    if (psurf == NULL)
        return;

    dispGetGfxSurface(getDefaultGfx(), &surf);
    *psurf = surf;

    if (pctx != NULL)
    {
        gfxContextNew(surf, &ctx);
        *pctx = ctx;
    }
    return;
}

#define BAD_CONTEXT ((mpe_GfxContext)-1)

static void teardown(mpe_GfxSurface surf, mpe_GfxContext ctx)
{
    if (ctx != 0 && ctx != BAD_CONTEXT)
        gfxContextDelete(ctx);
    /*
     if (surf != 0)
     gfxSurfaceDelete(surf);
     */
}
#define DO_SETUP                                                            \
    mpe_GfxSurface surf;                                                    \
    mpe_GfxContext ctx;                                                     \
    mpe_Error ec;                                                           \
    CuTestSetup(setup(&surf, &ctx));                                        \
    CuAssert(tc, "A surface is needed to create the context", surf != 0);   \
    CuAssert(tc, "A context is needed to create the context", ctx != 0);

#define DO_CLEANUP                               \
    CuTestCleanup(teardown(surf, ctx));

/**
 * Tests the gfxContextNew() call.
 *
 * @param tc pointer to test case structure
 */
void test_gfxContextNew(CuTest *tc)
{
    mpe_GfxSurface surf;
    mpe_GfxContext ctx = BAD_CONTEXT;
    mpe_Error ec;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\n***gfxContextNew***\n\n");
    CuTestSetup(setup(&surf, NULL));

    CuAssert(tc, "A surface is needed to create the context", surf != 0);

#if TEST_MPE_CONTEXT_PARAM_CHECK
    ec = gfxContextNew(surf, NULL);
    CuAssert(tc, "gfxContextNew() should've failed", ec != MPE_SUCCESS);

    ec = gfxContextNew(0, &ctx);
    CuAssert(tc, "gfxContextNew() should've failed", ec != MPE_SUCCESS);
    CuAssert(tc, "gfxContextNew() should not write context on failure",
            ctx == BAD_CONTEXT);
#endif

    ec = gfxContextNew(surf, &ctx);
    CuAssert(tc, "gfxContextNew() failed", ec == MPE_SUCCESS);
    CuAssert(tc, "gfxContextNew() returned an invalid context", ctx
            != BAD_CONTEXT);

    CuTestCleanup(teardown(surf, ctx));
}

/**
 * Tests the gfxContextCreate() call.
 *
 * @param tc pointer to test case structure
 */
void test_gfxContextCreate(CuTest *tc)
{
    mpe_GfxSurface surf;
    mpe_GfxContext ctx, ctx2 = (mpe_GfxContext) - 1;
    mpe_Error ec;
    CuTestSetup(setup(&surf, &ctx));

    CuAssert(tc, "A surface is needed to create the context", surf != 0);
    CuAssert(tc, "A context is needed to create the context", ctx != 0);

#if TEST_MPE_CONTEXT_PARAM_CHECK
    ec = gfxContextCreate(ctx, NULL);
    CuAssert(tc, "gfxContextCreate() should've failed", ec != MPE_SUCCESS);

    ec = gfxContextNew(0, &ctx2);
    CuAssert(tc, "gfxContextCreate() should've failed", ec != MPE_SUCCESS);
    CuAssert(tc, "gfxContextCreate() should not write context on failure",
            ctx2 == BAD_CONTEXT);
#endif    

    ec = gfxContextCreate(ctx, &ctx2);
    gfxContextDelete(ctx); // cleanup up now
    ctx = ctx2; // So that on failure ctx2 gets cleaned-up instead
    CuAssert(tc, "gfxContextCreate() failed", ec == MPE_SUCCESS);
    CuAssert(tc, "gfxContextCreate() returned an invalid context", ctx
            != BAD_CONTEXT);

    /* Test that created context has same parameters as base context */
    {
        mpe_GfxColor color, ncolor = mpe_gfxArgbToColor('A', 'r', 'G', 'B');
        mpe_GfxPoint origin, norigin =
        { 320, 240 };
        mpe_GfxRectangle clip, nclip =
        { 0, 0, 30, 45 };
        mpe_GfxPaintMode oldmode, mode, nmode = MPE_GFX_XOR;
        uint32_t oldmodeData, modeData, nmodeData = mpe_gfxRgbToColor(0xff,
                0xff, 0xff);
        mpe_GfxFont font, nfont;
        mpe_GfxWchar name[] =
        { 'T', 'i', 'r', 'e', 's', 'i', 'a', 's', '\0' };

        CuTestSetup(ec = gfxFontNew((mpe_GfxFontFactory) 0, name, sizeof(name),
                MPE_GFX_PLAIN, 36, &nfont));

        /* Modify ctx's attributes */
        gfxSetColor(ctx, ncolor);
        gfxSetOrigin(ctx, norigin.x, norigin.y);
        gfxSetClipRect(ctx, &nclip);
        gfxSetPaintMode(ctx, nmode, nmodeData);
        gfxSetFont(ctx, nfont);

        gfxGetPaintMode(ctx, &oldmode, &oldmodeData);

        /* Create a new ctx2 */
        ctx2 = BAD_CONTEXT;
        ec = gfxContextCreate(ctx, &ctx2);
        /* Delete ctx, ctx = ctx2 (as above) */
        gfxContextDelete(ctx);
        ctx = ctx2;
        CuAssert(tc, "gfxContextCreate() failed", ec == MPE_SUCCESS);
        CuAssert(tc, "gfxContextCreate() returned an invalid context", ctx
                != BAD_CONTEXT);

        /* Test saved attributes to ctx's */
        ec = gfxGetColor(ctx, &color);
        CuAssert(tc, "gfxGetColor() failed", ec == MPE_SUCCESS);
        CuAssertColorEquals(tc,
                "gfxGetColor() did not reflect base value on create", ncolor,
                color);
        ec = gfxGetOrigin(ctx, &origin);
        CuAssert(tc, "gfxGetOrigin() failed", ec == MPE_SUCCESS);
        CuAssertPointEquals(tc,
                "gfxGetOrigin() did not reflect base value on create", norigin,
                origin);
        ec = gfxGetClipRect(ctx, &clip);
        CuAssert(tc, "gfxGetClipRect() failed", ec == MPE_SUCCESS);
        CuAssertRectangleEquals(tc,
                "gfxGetClipRect() did not reflect base value on create", nclip,
                clip);
        ec = gfxGetFont(ctx, &font);
        CuAssert(tc, "gfxGetFont() failed", ec == MPE_SUCCESS);
        CuAssertFontEquals(tc,
                "gfxGetFont() did not reflect base value on create", nfont,
                font);
        ec = gfxGetPaintMode(ctx, &mode, &modeData);
        CuAssert(tc, "gfxGetPaintMode() failed", ec == MPE_SUCCESS);
        CuAssertModeEquals(tc,
                "gfxGetPaintMode() did not reflect base value on create",
                oldmode, mode);

        CuAssertIntEquals_Msg(tc,
                "gfxGetPaintMode() did not reflect base data value on create",
                oldmodeData, modeData);

        CuTestCleanup(gfxFontDelete(nfont));
    }

    CuTestCleanup(teardown(surf, ctx));
}

/**
 * Tests the gfxContextDelete() call.
 *
 * @param tc pointer to test case structure
 */
void test_gfxContextDelete(CuTest *tc)
{
    DO_SETUP;

    ec = gfxContextDelete(ctx);
    ctx = 0; // if delete failed, don't do again in cleanup...
    CuAssert(tc, "gfxContextDelete should succeed", ec == MPE_SUCCESS);

    /* Can we test that it WAS destroyed??? */

    DO_CLEANUP;
}

/**
 * Tests the gfxGetSurface() call.
 *
 * @param tc pointer to test case structure
 */
void test_gfxGetSurface(CuTest *tc)
{
    mpe_GfxSurface surf2;
    DO_SETUP;

    /* Should be the surface it was created with. */
    ec = gfxGetSurface(ctx, &surf2);
    CuAssert(tc, "gfxGetSurface() failed", ec == MPE_SUCCESS);
    CuAssert(tc, "gfxGetSurface() returned the wrong surface", surf == surf2);

    DO_CLEANUP;
}

/**
 * Tests the mpe_gfxSet/GetColor() call.
 *
 * @param tc pointer to test case structure
 */
void test_gfxSetGetColor(CuTest *tc)
{
    mpe_GfxColor black = mpe_gfxRgbToColor(0, 0, 0);
    static mpe_GfxColor
            colors[] =
            { mpe_gfxArgbToColor('a', 'r', 'g', 'b'), mpe_gfxRgbToColor('R',
                    'G', 'B') };
    mpe_GfxColor color;
    int i;
    DO_SETUP;

#define NCOLORS (sizeof(colors)/sizeof(mpe_GfxColor))

    ec = gfxGetColor(ctx, &color);
    CuAssert(tc, "gfxGetColor failed", ec == MPE_SUCCESS);
    CuAssertColorEquals(tc, "Context's default color should be black", black,
            color);

    // Set color should be returned color
    for (i = 0; i < NCOLORS; ++i)
    {
        ec = gfxSetColor(ctx, colors[i]);
        CuAssert(tc, "gfxSetColor() failed", ec == MPE_SUCCESS);
        ec = gfxGetColor(ctx, &color);
        CuAssert(tc, "gfxGetColor() failed", ec == MPE_SUCCESS);
        CuAssertColorEquals(tc, "Context's get color should be set color",
                colors[i], color);
    }

#if TEST_CONTEXT_PARAM_CHECK
    ec = gfxGetColor(0, &color);
    CuAssert(tc, "gfxGetColor should've failed",
            ec != MPE_SUCCESS);
    ec = gfxGetColor(ctx, NULL);
    CuAssert(tc, "gfxGetColor should've failed",
            ec != MPE_SUCCESS);
    ec = gfxSetColor(0, color);
    CuAssert(tc, "gfxSetColor should've failed",
            ec != MPE_SUCCESS);
#endif

    DO_CLEANUP;
}

/**
 * Tests the default font setting for a new context.
 *
 * @param tc pointer to test case structure
 */
void test_gfxGetFont_Default(CuTest *tc)
{
    mpe_GfxFont font, font2;
    DO_SETUP;

    ec = gfxGetFont(ctx, &font);
    CuAssert(tc, "gfxGetFont failed (default)", ec == MPE_SUCCESS);
    CuAssert(tc, "The context's default font should be set", font != 0);
    ASSERT_SUCCESS(gfxGetFont, (ctx, &font2));
    CuAssertFontEquals(tc, "Successive calls to gfxGetFont should return same",
            font, font2);
    /* !!!!! Should verify default system font - Tiresias PLAIN 26 */

    DO_CLEANUP;
}

/**
 * Tests the ability to clear the current font.
 * Should result in failed rendering.
 *
 * @param tc pointer to test case structure
 */
void test_gfxSetFont_Clear(CuTest *tc)
{
    DO_SETUP;

    ec = gfxSetFont(ctx, 0);
    CuAssert(tc, "gfxSetFont(0) failed; should be able to clear font", ec
            == MPE_SUCCESS);

    DO_CLEANUP;
}

/**
 * Tests the mpe_gfxSet/GetFont() call.
 *
 * @param tc pointer to test case structure
 */
void test_gfxSetGetFont(CuTest *tc)
{
    mpe_GfxFont font, font2;
    int i;

    typedef struct
    {
        mpe_GfxWchar name[32];
        mpe_GfxFontStyle style;
        int32_t size;
    } fontDesc;
    static fontDesc
            fonts[] =
            {
                    {
                    { 'T', 'i', 'r', 'e', 's', 'i', 'a', 's' }, MPE_GFX_PLAIN,
                            31 },
                    // Only Tiresias needs to be supported, but these logical names should also
                    {
                    { 'S', 'e', 'r', 'i', 'f' }, MPE_GFX_ITALIC, 36 },
                    {
                    { 'S', 'a', 'n', 's', 'S', 'e', 'r', 'i', 'f' },
                            MPE_GFX_PLAIN, 24 },
                    {
                    { 'D', 'i', 'a', 'l', 'o', 'g' }, MPE_GFX_BOLD_ITALIC, 26 },
                    {
                    { 'M', 'o', 'n', 'o', 's', 'p', 'a', 'c', 'e', 'd' },
                            MPE_GFX_BOLD_ITALIC, 31 },
                    {
                    { 'D', 'i', 'a', 'l', 'o', 'g', 'I', 'n', 'p', 'u', 't' },
                            MPE_GFX_ITALIC, 31 },
                    // The following names probably won't be supported...
                    {
                    { 'H', 'e', 'l', 'v', 'e', 't', 'i', 'c', 'a' },
                            MPE_GFX_BOLD, 24 },
                    {
                    { 'T', 'i', 'm', 'e', 's', 'R', 'o', 'm', 'a', 'n' },
                            MPE_GFX_BOLD_ITALIC, 24 },
                    {
                    { 'C', 'o', 'u', 'r', 'i', 'e', 'r' }, MPE_GFX_BOLD, 26 },
                    {
                    { 'Z', 'a', 'p', 'f', 'D', 'i', 'n', 'g', 'b', 'a', 't',
                            's' }, MPE_GFX_PLAIN, 36 }, };
    DO_SETUP;
#define NFONTS (sizeof(fonts)/sizeof(fontDesc))

    for (i = 0; i < NFONTS; ++i)
    {
        CuTestSetup(ec = gfxFontNew(0, fonts[i].name, sizeof(fonts[i].name),
                fonts[i].style, fonts[i].size, &font2));
        CuAssert(tc, "gfxFontNew failed", ec == MPE_SUCCESS);

        ASSERT_SUCCESS(gfxSetFont, (ctx, font2));
        ASSERT_SUCCESS(gfxGetFont, (ctx, &font));
        CuAssertFontEquals(tc, "The context's set font should be the get font",
                font2, font);
        ASSERT_SUCCESS(gfxGetFont, (ctx, &font));
        CuAssertFontEquals(tc,
                "Successive calls to get font should return same", font2, font);

        CuTestCleanup(gfxFontDelete(font2));
    }

    DO_CLEANUP;
}

/**
 * Tests the mpe_gfxSet/GetPaintMode() call.
 *
 * @param tc pointer to test case structure
 */
void test_gfxSetGetPaintMode(CuTest *tc)
{
    mpe_GfxPaintMode mode;
    uint32_t datum;
    mpe_GfxPaintMode original = MPE_GFX_SRCOVER;
    mpe_GfxPaintMode modes[] =
    { MPE_GFX_XOR, MPE_GFX_CLR, MPE_GFX_SRC, MPE_GFX_SRCOVER, MPE_GFX_DSTOVER,
            MPE_GFX_SRCIN, MPE_GFX_DSTIN, MPE_GFX_SRCOUT, MPE_GFX_DSTOUT,
            MPE_GFX_DST,

    };
    uint32_t data[] =
    { mpe_gfxRgbToColor(0x80, 0x80, 0x80), 0xFF, 0x80, 0x40 };
    int i;
    DO_SETUP;

    ec = gfxGetPaintMode(ctx, &mode, &datum);
    CuAssert(tc, "gfxGetPaintMode failed", ec == MPE_SUCCESS);
    CuAssertModeEquals(tc, "Context's default paint mode should be SRCOVER",
            original, mode);
    CuAssertIntEquals_Msg(tc,
            "Default paint mode data should be 0xFF for fully opaque", 0xFF,
            datum);

    for (i = 0; i < MPE_GFX_MODE_MAX; ++i)
    {
        ec = gfxSetPaintMode(ctx, modes[i], data[i]);
        CuAssert(tc, "gfxSetPaintMode failed", ec == MPE_SUCCESS);
        ec = gfxGetPaintMode(ctx, &mode, &datum);
        CuAssertModeEquals(tc,
                "Context's get paint mode should be set paint mode", modes[i],
                mode);
        CuAssertIntEquals_Msg(tc,
                "Context's get paint mode data should be set data", data[i],
                datum);
    }

#if TEST_CONTEXT_PARAM_CHECK
    ec = gfxGetPaintMode(0, &mode, &datum);
    CuAssert(tc, "gfxGetPaintMode should've failed",
            ec != MPE_SUCCESS);
    ec = gfxGetPaintMode(ctx, NULL, &datum);
    CuAssert(tc, "gfxGetPaintMode should've failed",
            ec != MPE_SUCCESS);
    ec = gfxSetPaintMode(0, mode, datum);
    CuAssert(tc, "gfxSetPaintMode should've failed",
            ec != MPE_SUCCESS);
    ec = gfxSetPaintMode(ctx, -1, datum);
    CuAssert(tc, "gfxSetPaintMode should've failed",
            ec != MPE_SUCCESS);
    ec = gfxSetPaintMode(ctx, MPE_GFX_SRC, 0x8000);
    CuAssert(tc, "gfxSetPaintMode should've failed",
            ec != MPE_SUCCESS);
#endif

    DO_CLEANUP;
}

/**
 * Tests the mpe_gfxSetGetOrigin() call.
 *
 * @param tc pointer to test case structure
 */
void test_gfxSetGetOrigin(CuTest *tc)
{
    mpe_GfxPoint origin;
    mpe_GfxPoint original =
    { 0, 0 };
    static mpe_GfxPoint origins[] =
    {
    { 0, 0 },
    { 1, 1 },
    { 0, 100 },
    { 100, 0 },
    { -10, 10 },
    { 20, -30 },
    { -1, -1 },
    { 1000, 1000 } };

    int i;
    DO_SETUP;

#define NPOINTS (sizeof(origins)/sizeof(mpe_GfxPoint))

    ec = gfxGetOrigin(ctx, &origin);
    CuAssert(tc, "gfxGetOrigin failed", ec == MPE_SUCCESS);
    CuAssertPointEquals(tc, "Context's default origin should be {0,0}",
            original, origin);

    for (i = 0; i < NPOINTS; ++i)
    {
        ec = gfxSetOrigin(ctx, origins[i].x, origins[i].y);
        CuAssert(tc, "gfxSetOrigin failed", ec == MPE_SUCCESS);
        ec = gfxGetOrigin(ctx, &origin);
        CuAssertPointEquals(tc, "Context's get origin should be set origin",
                origins[i], origin);
    }

#if TEST_CONTEXT_PARAM_CHECK
    ec = gfxGetOrigin(0, &origin);
    CuAssert(tc, "gfxGetOrigin should've failed",
            ec != MPE_SUCCESS);
    ec = gfxGetOrigin(ctx, NULL);
    CuAssert(tc, "gfxGetOrigin should've failed",
            ec != MPE_SUCCESS);
    ec = gfxSetOrigin(0, origin.x, origin.y);
    CuAssert(tc, "gfxSetOrigin should've failed",
            ec != MPE_SUCCESS);
#endif

    DO_CLEANUP;
}

/**
 * Tests the mpe_gfxGet/SetClipRect() call.
 *
 * @param tc pointer to test case structure
 */
void test_gfxSetGetClipRect(CuTest *tc)
{
    mpe_GfxRectangle clip;
    mpe_GfxRectangle original =
    { 0, 0, 640, 480 };
    mpe_GfxRectangle clips[] =
    {
    { 0, 0, 640, 480 },
    { 0, 0, 0, 0 },
    { 1, 1, 1, 1 },
    { 0, 0, 100, 100 },
    { 30, 40, 100, 100 },
    { 10, -10, 20, 200 },
    { -10, 10, 200, 10 },
    { -1000, -2000, 3000, 5000 },
#if 0
            {   640, 480, 1, 1}, // That's a failure case !! x and y are out of bounds
#endif
#if MPE_GFX_NEG_CLIP_WH_LEGAL
            {   320, 0, -320, 240},
            {   0, 240, 320, -240},
            {   320, 240, -320, -240},
#endif
            };
#define NRECTS (sizeof(clips)/sizeof(mpe_GfxRectangle))
    int i;
    DO_SETUP;

    ec = gfxGetClipRect(ctx, &clip);
    CuAssert(tc, "gfxGetClipRect failed", ec == MPE_SUCCESS);
    CuAssertRectangleEquals(tc, "Context's default clip should be {0,0}",
            original, clip);

    for (i = 0; i < NRECTS; ++i)
    {
        ec = gfxSetClipRect(ctx, &clips[i]);
        CuAssert(tc, "gfxSetClipRect failed", ec == MPE_SUCCESS);
        ec = gfxGetClipRect(ctx, &clip);
        CuAssertRectangleEquals(tc, "Context's get clip should be set clip",
                clips[i], clip);
    }

#if MPE_GFX_NEG_CLIP_WH_ILLEGAL
    // Negative width/height doesn't make any sense... 
    // although, I'm not sure if it needs to be explicitly an error...
    // Could just mean same as { x-width, y-height, -width, -height }
    mpe_GfxRectangle bad1 =
    {   0, 0, 10, 20};
    bad1.width = -bad1.width;
    ec = gfxSetClipRect(ctx, &bad1);
    CuAssert(tc, "gfxSetClipRect should've failed with negative width",
            ec != MPE_SUCCESS);
    bad1.width = -bad1.width;
    bad1.height = -bad1.height;
    ec = gfxSetClipRect(ctx, &bad1);
    CuAssert(tc, "gfxSetClipRect should've failed with negative height",
            ec != MPE_SUCCESS);
#endif

#if TEST_CONTEXT_PARAM_CHECK
    ec = gfxGetClipRect(0, &clip);
    CuAssert(tc, "gfxGetClipRect should've failed",
            ec != MPE_SUCCESS);
    ec = gfxGetClipRect(ctx, NULL);
    CuAssert(tc, "gfxGetClipRect should've failed",
            ec != MPE_SUCCESS);
    ec = gfxSetClipRect(0, &clip);
    CuAssert(tc, "gfxSetClipRect should've failed",
            ec != MPE_SUCCESS);
    ec = gfxSetClipRect(ctx, NULL);
    CuAssert(tc, "gfxSetClipRect should've failed",
            ec != MPE_SUCCESS);
#endif

    DO_CLEANUP;
}

/**
 * Tests that two contexts have distinct states.
 *
 * @param tc pointer to test case structure
 */
void test_gfxContext_DistinctState(CuTest *tc)
{
    mpe_GfxContext ctx2;

    /* Modify ctx's attributes */
    mpe_GfxColor color = mpe_gfxArgbToColor('A', 'r', 'G', 'B');
    mpe_GfxPoint origin =
    { 320, 240 };
    mpe_GfxRectangle clip =
    { 0, 0, 30, 45 };
    mpe_GfxPaintMode oldmode, mode = MPE_GFX_XOR;
    uint32_t oldmodeData, modeData = mpe_gfxRgbToColor(0xff, 0xff, 0xff);
    mpe_GfxFont font;
    mpe_GfxWchar name[] =
    { 'T', 'i', 'r', 'e', 's', 'i', 'a', 's', '\0' };

    /* Modify ctx2's attributes */
    mpe_GfxColor color2 = mpe_gfxArgbToColor('a', 'R', 'g', 'b');
    mpe_GfxPoint origin2 =
    { 30, 0 };
    mpe_GfxRectangle clip2 =
    { 0, 0, 300, 450 };
    mpe_GfxPaintMode mode2 = MPE_GFX_SRCOVER;
    uint32_t modeData2 = 0xff;
    mpe_GfxFont font2;
    mpe_GfxColor color3;
    mpe_GfxPoint origin3;
    mpe_GfxRectangle clip3;
    mpe_GfxPaintMode mode3;
    uint32_t modeData3;
    mpe_GfxFont font3;
    DO_SETUP;

    CuTestSetup(ec = gfxFontNew((mpe_GfxFontFactory) 0, name, sizeof(name),
            MPE_GFX_PLAIN, 36, &font));
    ASSERT_SUCCESS(gfxSetColor, (ctx, color));
    ASSERT_SUCCESS(gfxSetOrigin, (ctx, origin.x, origin.y));
    ASSERT_SUCCESS(gfxSetClipRect, (ctx, &clip));
    ASSERT_SUCCESS(gfxSetPaintMode, (ctx, mode, modeData));
    ASSERT_SUCCESS(gfxSetFont, (ctx, font));

    gfxGetPaintMode(ctx, &oldmode, &oldmodeData);

    // Create second context
    CuTestSetup(ec = gfxContextCreate(ctx, &ctx2));
    CuAssert(tc, "gfxContextCreate failed", ec == MPE_SUCCESS);

    CuTestSetup(ec = gfxFontNew((mpe_GfxFontFactory) 0, name, sizeof(name),
            MPE_GFX_BOLD, 26, &font2));
    ASSERT_SUCCESS(gfxSetColor, (ctx2, color2));
    ASSERT_SUCCESS(gfxSetOrigin, (ctx2, origin2.x, origin2.y));
    ASSERT_SUCCESS(gfxSetClipRect, (ctx2, &clip2));
    ASSERT_SUCCESS(gfxSetPaintMode, (ctx2, mode2, modeData2));
    ASSERT_SUCCESS(gfxSetFont, (ctx2, font2));

    //modeData2 =   (modeData & mpe_gfxArgbToColor(0, 0xFF, 0xFF, 0xFF)) ^ color;

    modeData2 = modeData;

    // Make sure that ctx hasn't changed
    ASSERT_SUCCESS(gfxGetColor, (ctx, &color3));
    CuAssertColorEquals(tc, "Color changed unexpectedly", color, color3);
    ASSERT_SUCCESS(gfxGetOrigin, (ctx, &origin3));
    CuAssertPointEquals(tc, "Origin changed unexpectedly", origin, origin3);
    ASSERT_SUCCESS(gfxGetClipRect, (ctx, &clip3));
    CuAssertRectangleEquals(tc, "Clipping changed unexpectedly", clip, clip3);
    ASSERT_SUCCESS(gfxGetPaintMode, (ctx, &mode3, &modeData3));
    CuAssertModeEquals(tc, "Paint mode changed unexpectedly", mode, mode3);
    CuAssertIntEquals_Msg(tc, "Paint mode data changed unexpectedly",
            modeData2, modeData3);
    ASSERT_SUCCESS(gfxGetFont, (ctx, &font3));
    CuAssertFontEquals(tc, "Font changed unexpectedly", font, font3);

    CuTestCleanup(gfxFontDelete(font2));
    CuTestCleanup(gfxContextDelete(ctx2));
    CuTestCleanup(gfxFontDelete(font));

    DO_CLEANUP;
}

/**
 * Create and return the test suite for the mpe_gfx APIs.
 * @return a pointer to the new test suite.
 */
CuSuite* getTestSuite_gfxContext(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_STOP_TEST(suite, test_gfxContextNew);
    SUITE_STOP_TEST(suite, test_gfxContextCreate);
    SUITE_STOP_TEST(suite, test_gfxContextDelete);
    SUITE_STOP_TEST(suite, test_gfxGetSurface);
    SUITE_STOP_TEST(suite, test_gfxSetGetColor);
    SUITE_STOP_TEST(suite, test_gfxSetGetFont);
    SUITE_STOP_TEST(suite, test_gfxGetFont_Default);
    SUITE_STOP_TEST(suite, test_gfxSetFont_Clear);
    SUITE_STOP_TEST(suite, test_gfxSetGetPaintMode);
    SUITE_STOP_TEST(suite, test_gfxSetGetOrigin);
    SUITE_STOP_TEST(suite, test_gfxSetGetClipRect);
    SUITE_STOP_TEST(suite, test_gfxContext_DistinctState);

    return suite;
}

void test_gfxRunContextTests(void)
{
    test_gfxRunSuite(getTestSuite_gfxContext(), "ContextTests");
}

void test_gfxRunContextNewTest(void)
{
    CuSuite* suite;

    suite
            = CuSuiteNewCloneTest(getTestSuite_gfxContext(),
                    "test_gfxContextNew");
    test_gfxRunSuite(suite, "gfxContextNew");

    CuSuiteFree(suite);
}

void test_gfxRunContextCreateTest(void)
{
    CuSuite* suite;

    suite = CuSuiteNewCloneTest(getTestSuite_gfxContext(),
            "test_gfxContextCreate");
    test_gfxRunSuite(suite, "gfxContextCreate");

    CuSuiteFree(suite);
}

void test_gfxRunContextDeleteTest(void)
{
    CuSuite* suite;

    suite = CuSuiteNewCloneTest(getTestSuite_gfxContext(),
            "test_gfxContextDelete");
    test_gfxRunSuite(suite, "gfxContextDelete");

    CuSuiteFree(suite);
}

void test_gfxRunGetSurfaceTest(void)
{
    CuSuite* suite;

    suite
            = CuSuiteNewCloneTest(getTestSuite_gfxContext(),
                    "test_gfxGetSurface");
    test_gfxRunSuite(suite, "gfxGetSurface");

    CuSuiteFree(suite);
}

void test_gfxRunSetGetColorTest(void)
{
    CuSuite* suite;

    suite = CuSuiteNewCloneTest(getTestSuite_gfxContext(),
            "test_gfxSetGetColor");
    test_gfxRunSuite(suite, "gfxSetGetColor");

    CuSuiteFree(suite);
}

void test_gfxRunSetGetFontTest(void)
{
    CuSuite* suite;

    suite
            = CuSuiteNewCloneTest(getTestSuite_gfxContext(),
                    "test_gfxSetGetFont");
    test_gfxRunSuite(suite, "gfxSetGetFont");

    CuSuiteFree(suite);
}

void test_gfxRunGetFont_DefaultTest(void)
{
    CuSuite* suite;

    suite = CuSuiteNewCloneTest(getTestSuite_gfxContext(),
            "test_gfxGetFont_Default");
    test_gfxRunSuite(suite, "gfxGetFont_Default");

    CuSuiteFree(suite);
}

void test_gfxRunSetFont_ClearTest(void)
{
    CuSuite* suite;

    suite = CuSuiteNewCloneTest(getTestSuite_gfxContext(),
            "test_gfxSetFont_Clear");
    test_gfxRunSuite(suite, "gfxSetFont_Clear");

    CuSuiteFree(suite);
}

void test_gfxRunSetGetPaintModeTest(void)
{
    CuSuite* suite;

    suite = CuSuiteNewCloneTest(getTestSuite_gfxContext(),
            "test_gfxSetGetPaintMode");
    test_gfxRunSuite(suite, "gfxSetGetPaintMode");

    CuSuiteFree(suite);
}

void test_gfxRunSetGetOriginTest(void)
{
    CuSuite* suite;

    suite = CuSuiteNewCloneTest(getTestSuite_gfxContext(),
            "test_gfxSetGetOrigin");
    test_gfxRunSuite(suite, "gfxSetGetOrigin");

    CuSuiteFree(suite);
}

void test_gfxRunSetGetClipRectTest(void)
{
    CuSuite* suite;

    suite = CuSuiteNewCloneTest(getTestSuite_gfxContext(),
            "test_gfxSetGetClipRect");
    test_gfxRunSuite(suite, "gfxSetGetClipRect");

    CuSuiteFree(suite);
}

void test_gfxRunContext_DistinctStateTest(void)
{
    CuSuite* suite;

    suite = CuSuiteNewCloneTest(getTestSuite_gfxContext(),
            "test_gfxContext_DistinctState");
    test_gfxRunSuite(suite, "gfxContext_DistinctState");

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
