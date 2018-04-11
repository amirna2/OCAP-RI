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

/* set to 1 for infinite run of testLineDance() */
#define SCREEN_SAVER 0

typedef struct _line_t
{
    int32_t x1;
    int32_t y1;
    int32_t x2;
    int32_t y2;
} line_t;

void blitToScreen(CuTest *tc);
void clearSurface(CuTest *tc);
void testDrawLine(CuTest *tc);
static void testDrawEllipse(mpe_Bool filled, CuTest *tc);
void testFilledEllipse(CuTest *tc);
void testUnFilledEllipse(CuTest *tc);
static void testDrawArc(mpe_Bool filled, CuTest *tc);
void testFilledDrawArc(CuTest *tc);
void testUnFilledDrawArc(CuTest *tc);
static void testDrawRect(mpe_Bool filled, CuTest *tc);
void testFilledDrawRect(CuTest *tc);
void testUnFilledDrawRect(CuTest *tc);
void testDrawRoundRect(mpe_Bool filled, CuTest *tc);
void testFilledDrawRoundRect(CuTest *tc);
void testUnFilledDrawRoundRect(CuTest *tc);
static void testDrawString(CuTest *tc);
void test_ClipBlit(CuTest *tc);
void testXorBlit(CuTest *tc);
void testCopySurface(CuTest *tc);
static void testLineDance(CuTest *tc);
void testInit(CuTest *tc);
static void testTerminate(CuTest *tc);
void testRun(void);
CuSuite* getTestSuite_gfxSmoke(void);
void test_gfx_SmokeTest(void);

static mpe_DispDevice getDefaultGfx(void);

/* 
 true  = a single blit per test cases
 false = one blit after each draw
 */
mpe_Bool flush = true;

/* 
 true  = drawing into the screen surface directly (no blit needed)
 false = drawing into a secondary surface, then blit into the screen
 */
mpe_Bool use_screen_surface = true;

const char *dialogfont = "/snfs/sys/fonts/Tires-0_802.pfr";
const char *ameliafont = "/snfs/sys/fonts/Tires-0_802.pfr";

/* global definitions */

#define MY_TEXT "The quick brown fox jumps over the lazy dog"

uint8_t dialog_fontbuffer[13296];
uint8_t amelia_fontbuffer[20000];

static mpe_GfxWchar fontName1[] =
{ 'd', 'i', 'a', 'l', 'o', 'g' };
static mpe_GfxWchar fontName2[] =
{ 'a', 'm', 'e', 'l', 'i', 'a' };

mpe_GfxSurface sf;
mpe_GfxContext ctx;
mpe_GfxSurface screen;
mpe_GfxContext screen_ctx;
mpe_GfxFont dialogH;

mpe_GfxFont amelia;
mpe_GfxFontFactory ff;

void Delay(void);

#define THREAD_SLEEP	MPETEST_GFX(threadSleep)
#define TIME_GET_MILLIS	MPETEST_GFX(timeGetMillis)

void Delay(void)
{
    THREAD_SLEEP(500, 0); // sleep for half a second (500ms)
        }

void blitToScreen(CuTest *tc)
{
    mpe_GfxRectangle sf_rect =
    { 0, 0, 640, 480 };
    mpe_Error result = 0;

    if (!use_screen_surface)
    {
        result = gfxBitBlt(screen_ctx, sf, 0, 0, &sf_rect);
        ASSERT( gfxBitBlt);
    }
}

void clearSurface(CuTest *tc)
{
    mpe_GfxRectangle sf_rect =
    { 0, 0, 640, 480 };

    mpe_Error result = 0;
    result = gfxSetColor(ctx, mpe_gfxArgbToColor(0xff, 0xff, 0xff, 0xff));
    ASSERT( gfxSetColor);

    result = gfxFillRect(ctx, &sf_rect);
    ASSERT( gfxFillRect);

    blitToScreen(tc);
    result = dispFlushGfxSurface(getDefaultGfx());
    ASSERT( gfxScreenFlush);
}

//
// Draw lines
//
void testDrawLine(CuTest *tc)
{
    mpe_Error result = 0;
    uint8_t red;
    uint8_t green;
    uint8_t blue;
    uint8_t alpha;
    int32_t x1;
    int32_t x2;
    int32_t y1;
    int32_t y2;
    int count = 50;

    clearSurface(tc);
    // Loop to draw random lines
    while (count--)
    {
        // Set a random color
        red = rand() & 0xff;
        green = rand() & 0xff;
        blue = rand() & 0xff;
        alpha = rand() & 0xff;
        result = gfxSetColor(ctx, mpe_gfxArgbToColor(alpha, red, green, blue));
        ASSERT( gfxSetColor);

        // Draw a random line
        x1 = rand() % 640;
        x2 = rand() % 640;
        y1 = rand() % 480;
        y2 = rand() % 480;

        result = gfxDrawLine(ctx, x1, y1, x2, y2);
        ASSERT( gfxDrawLine);

        if (!flush)
        {
            blitToScreen(tc);
            result = dispFlushGfxSurface(getDefaultGfx());
            ASSERT( gfxScreenFlush);
        }
    }
    if (flush)
    {
        blitToScreen(tc);
        result = dispFlushGfxSurface(getDefaultGfx());
        ASSERT( gfxScreenFlush);
    }

    Delay();
}

//
// Test Draw ellipse
//

static void testDrawEllipse(mpe_Bool filled, CuTest *tc)
{

    int count = 10;
    mpe_GfxRectangle r;
    mpe_Error result = 0;
    uint8_t red, green, blue, alpha;
    int32_t x1, x2, y1, y2;

    clearSurface(tc);

    while (count--)
    {
        // Set a random color
        red = rand() & 0xff;
        green = rand() & 0xff;
        blue = rand() & 0xff;
        alpha = rand() & 0xff;
        result = gfxSetColor(ctx, mpe_gfxArgbToColor(alpha, red, green, blue));
        ASSERT( gfxSetColor);

        // Draw a random rectangle
        x1 = rand() % 640;
        x2 = rand() % 640;
        y1 = rand() % 480;
        y2 = rand() % 480;
        if (x1 > x2)
        {
            int32_t temp = x1;
            x1 = x2;
            x2 = temp;
        }
        if (y1 > y2)
        {
            int32_t temp = y1;
            y1 = y2;
            y2 = temp;
        }

        r.x = x1;
        r.y = y1;
        r.width = x2 - x1 + 1;
        r.height = y2 - y1 + 1;

        if (filled)
        {
            result = gfxFillEllipse(ctx, &r);
            ASSERT( gfxFillEllipse);

        }
        else
        {
            result = gfxDrawEllipse(ctx, &r);
            ASSERT( gfxDrawEllipse);

        }
        if (!flush)
        {
            blitToScreen(tc);
            result = dispFlushGfxSurface(getDefaultGfx());
            ASSERT( gfxScreenFlush);

        }
    }
    if (flush)
    {
        blitToScreen(tc);
        result = dispFlushGfxSurface(getDefaultGfx());
        ASSERT( gfxScreenFlush);
    }

}

void testFilledEllipse(CuTest *tc)
{
    testDrawEllipse(true, tc);

    Delay();
}

void testUnFilledEllipse(CuTest *tc)
{
    testDrawEllipse(false, tc);

    Delay();
}

//
// Draw arcs
//


static void testDrawArc(mpe_Bool filled, CuTest *tc)
{

    mpe_GfxRectangle r;
    mpe_Error result = 0;

    // Loop to draw random arcs
    int count = 10;
    uint8_t red, green, blue, alpha;
    int32_t x1, x2, y1, y2, sa, aa, temp;

    while (count--)
    {
        // Set a random color
        red = rand() & 0xff;
        green = rand() & 0xff;
        blue = rand() & 0xff;
        alpha = rand() & 0xff;
        result = gfxSetColor(ctx, mpe_gfxArgbToColor(alpha, red, green, blue));
        ASSERT( gfxSetColor);

        // Draw a random arc
        x1 = rand() % 640;
        x2 = rand() % 640;
        y1 = rand() % 480;
        y2 = rand() % 480;
        sa = rand() % 360;
        aa = rand() % 360;
        if (x1 > x2)
        {
            temp = x1;
            x1 = x2;
            x2 = temp;
        }
        if (y1 > y2)
        {
            temp = y1;
            y1 = y2;
            y2 = temp;
        }

        r.x = x1;
        r.y = y1;
        r.width = x2 - x1 + 1;
        r.height = y2 - y1 + 1;

        if (filled)
        {
            result = gfxFillArc(ctx, &r, sa, aa);
            ASSERT( gfxFillArc);
        }
        else
        {
            result = gfxDrawArc(ctx, &r, sa, aa);
            ASSERT( gfxDrawArc);
        }

        // blit to the screen surface to make visible
        if (!flush)
        {
            blitToScreen(tc);
            result = dispFlushGfxSurface(getDefaultGfx());
            ASSERT( gfxScreenFlush);
        }
    }
    if (flush)
    {
        blitToScreen(tc);
        result = dispFlushGfxSurface(getDefaultGfx());
        ASSERT( gfxScreenFlush);
    }

}
void testFilledDrawArc(CuTest *tc)
{
    testDrawArc(true, tc);

    Delay();
}

void testUnFilledDrawArc(CuTest *tc)
{
    testDrawArc(false, tc);

    Delay();
}

/*
 ** Draw rectangles
 */
static void testDrawRect(mpe_Bool filled, CuTest *tc)
{
    // Loop to draw random rectangles
    int count = 10;
    mpe_GfxRectangle r;
    mpe_Error result = 0;
    uint8_t red, green, blue, alpha;
    int32_t x1, x2, y1, y2;

    clearSurface(tc);

    while (count--)
    {
        // Set a random color
        red = rand() & 0xff;
        green = rand() & 0xff;
        blue = rand() & 0xff;
        alpha = rand() & 0xff;
        result = gfxSetColor(ctx, mpe_gfxArgbToColor(alpha, red, green, blue));
        ASSERT( gfxSetColor);

        // Draw a random rectangle
        x1 = rand() % 640;
        x2 = rand() % 640;
        y1 = rand() % 480;
        y2 = rand() % 480;
        if (x1 > x2)
        {
            int32_t temp = x1;
            x1 = x2;
            x2 = temp;
        }
        if (y1 > y2)
        {
            int32_t temp = y1;
            y1 = y2;
            y2 = temp;
        }

        r.x = x1;
        r.y = y1;
        r.width = x2 - x1 + 1;
        r.height = y2 - y1 + 1;

        if (filled)
        {
            result = gfxFillRect(ctx, &r);
            ASSERT( gfxFillRect);
        }
        else
        {
            result = gfxDrawRect(ctx, &r);
            ASSERT( gfxDrawRect);
        }

        // blit to the screen surface to make visible
        if (!flush)
        {
            blitToScreen(tc);
            result = dispFlushGfxSurface(getDefaultGfx());
            ASSERT( gfxScreenFlush);
        }
    }
    if (flush)
    {
        blitToScreen(tc);
        result = dispFlushGfxSurface(getDefaultGfx());
        ASSERT( gfxScreenFlush);
    }
}
void testFilledDrawRect(CuTest *tc)
{
    testDrawRect(true, tc);

    Delay();
}

void testUnFilledDrawRect(CuTest *tc)
{
    testDrawRect(false, tc);

    Delay();
}

/*
 ** Draw round rectangles
 */
void testDrawRoundRect(mpe_Bool filled, CuTest *tc)
{
    // Loop to draw random rectangles
    int count = 20;
    mpe_GfxRectangle r;
    mpe_Error result = 0;
    uint8_t red, green, blue, alpha;
    int32_t x1, x2, y1, y2, ah, aw;

    clearSurface(tc);

    while (count--)
    {
        // Set a random color
        red = rand() & 0xff;
        green = rand() & 0xff;
        blue = rand() & 0xff;
        alpha = rand() & 0xff;
        result = gfxSetColor(ctx, mpe_gfxArgbToColor(alpha, red, green, blue));
        ASSERT( gfxSetColor);

        // Draw a random rectangle
        x1 = rand() % 640;
        x2 = rand() % 640;
        y1 = rand() % 480;
        y2 = rand() % 480;

        if (x1 > x2)
        {
            int32_t temp = x1;
            x1 = x2;
            x2 = temp;
        }
        if (y1 > y2)
        {
            int32_t temp = y1;
            y1 = y2;
            y2 = temp;
        }

        r.x = x1;
        r.y = y1;
        r.width = x2 - x1 + 1;
        r.height = y2 - y1 + 1;

        ah = rand() % r.height / 2;
        aw = rand() % r.width / 2;

        /*
         ** This is to protect against bad parameters being 
         ** passed to the mpe funcs
         */
        if (ah == 0)
            ah = ah + 1;
        if (aw == 0)
            aw = aw + 1;

        if (filled)
        {

            result = gfxFillRoundRect(ctx, &r, aw, ah);
            if (result != MPE_SUCCESS)
            {
                TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                        "gfxFillRoundRect: count: %d\n", count);
                TRACE(
                        MPE_LOG_INFO,
                        MPE_MOD_TEST,
                        "gfxFillRoundRect: r.x = %d, r.y = %d, r.width = %d, r.height = %d, aw = %d, ah = %d\n",
                        r.x, r.y, r.width, r.height, aw, ah);
            }

            ASSERT( gfxFillRoundRect);
        }
        else
        {
            result = gfxDrawRoundRect(ctx, &r, aw, ah);
            if (result != MPE_SUCCESS)
            {
                TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                        "gfxDrawRoundRect: count: %d\n", count);
                TRACE(
                        MPE_LOG_INFO,
                        MPE_MOD_TEST,
                        "gfxDrawRoundRect: r.x = %d, r.y = %d, r.width = %d, r.height = %d, aw = %d, ah = %d\n",
                        r.x, r.y, r.width, r.height, aw, ah);
            }

            ASSERT( gfxDrawRoundRect);
        }

        // blit to the screen surface to make visible
        if (!flush)
        {
            blitToScreen(tc);
            result = dispFlushGfxSurface(getDefaultGfx());
            ASSERT( gfxScreenFlush);
        }
    }
    if (flush)
    {
        blitToScreen(tc);
        result = dispFlushGfxSurface(getDefaultGfx());
        ASSERT( gfxScreenFlush);
    }
}

void testFilledDrawRoundRect(CuTest *tc)
{
    testDrawRoundRect(true, tc);

    Delay();
}

void testUnFilledDrawRoundRect(CuTest *tc)
{
    testDrawRoundRect(false, tc);

    Delay();
}

//
// Draw strings
//

static void testDrawString(CuTest *tc)
{
    int count = 30;
    mpe_Error result = 0;
    uint8_t red, green, blue, alpha;
    int32_t x, y;

    clearSurface(tc);
    while (count--)
    {
        // Set a random color
        red = rand() & 0xff;
        green = rand() & 0xff;
        blue = rand() & 0xff;
        alpha = rand() & 0xff;
        result = gfxSetColor(ctx, mpe_gfxArgbToColor(alpha, red, green, blue));
        ASSERT( gfxSetColor);

        // random text coordinates
        x = rand() % 640;
        y = rand() % 480;

        // draw a string
        result = gfxDrawString(ctx, x, y, MY_TEXT, 43);
        ASSERT( gfxDrawString);

        // blit to the screen surface to make visible
        if (!flush)
        {
            blitToScreen(tc);
            result = dispFlushGfxSurface(getDefaultGfx());
            ASSERT( gfxScreenFlush);
        }
    }
    if (flush)
    {
        blitToScreen(tc);
        result = dispFlushGfxSurface(getDefaultGfx());
        ASSERT( gfxScreenFlush);
    }

    Delay();
}

void test_ClipBlit(CuTest *tc)
{
    mpe_GfxRectangle sf_rect =
    { 0, 0, 640, 480 };

    mpe_GfxRectangle r;
    mpe_Error result = 0;
    mpe_GfxRectangle drect =
    { 50, 50, 400, 200 };

    r.x = 60;
    r.y = 60;
    r.width = 420;
    r.height = 220;

    result = gfxSetColor(ctx, mpe_gfxArgbToColor(0xff, 0xc0, 0xc0, 0xfe));
    ASSERT( gfxSetColor);

    result = gfxFillRect(ctx, &sf_rect);
    ASSERT( gfxFillRect);

    result = gfxSetClipRect(screen_ctx, &r);
    ASSERT( gfxSetClipRect);

    //blitToScreen();

    result = gfxStretchBlt(screen_ctx, sf, &drect, &sf_rect);
    ASSERT( gfxStretchBlt);

    result = dispFlushGfxSurface(getDefaultGfx());
    ASSERT( gfxScreenFlush);
}

void testXorBlit(CuTest *tc)
{
    /* blit a red surface to the screen surface with a white xor color */
    /* result should be a cyan (green + blue ) rectangle on the screen */
    mpe_Error result = 0;
    mpe_GfxSurfaceInfo s1;
    mpe_GfxSurface sf1;
    mpe_GfxContext ctx1;
    mpe_GfxRectangle r =
    { 0, 0, 255, 100 };

    s1.bpp = MPE_GFX_32BPP;
    s1.dim.width = r.width;
    s1.dim.height = r.height;
    s1.format = MPE_GFX_ARGB8888;
    s1.pixeldata = NULL;
    s1.widthbytes = (s1.dim.width * s1.bpp) / 8;

    result = gfxSurfaceNew(&s1, &sf1);
    if (result != MPE_GFX_ERROR_NOERR)
    {
        CuFail(tc, "gfxSurfaceNew failed...");
        return;
    }

    if ((result = gfxContextNew(sf1, &ctx1)) != MPE_GFX_ERROR_NOERR)
    {
        result = gfxSurfaceDelete(sf1);
        ASSERT( gfxSurfaceDelete);
        CuFail(tc, "gfxContextNew failed...");
        return;
    }

    // filling surface 1 with non-opaque pixels
    result = gfxSetPaintMode(ctx1, MPE_GFX_SRC, 255);
    ASSERT( gfxSetPaintMode);
    /* red */
    result = gfxSetColor(ctx1, mpe_gfxArgbToColor(0xff, 0xff, 0x00, 0x00));
    ASSERT( gfxSetColor);
    result = gfxFillRect(ctx1, &r);
    ASSERT( gfxFillRect);

    // blit surface 1 into screen
    result = gfxSetPaintMode(screen_ctx, MPE_GFX_XOR, mpe_gfxArgbToColor(0xff,
            0xff, 0xff, 0xff));
    ASSERT(gfxSetPaintMode);

    result = gfxBitBlt(screen_ctx, sf1, 50, 50, &r);
    ASSERT( gfxBitBlt);
    result = dispFlushGfxSurface(getDefaultGfx());
    ASSERT( gfxScreenFlush);

    result = gfxSetPaintMode(screen_ctx, MPE_GFX_SRCOVER, 255);
    ASSERT(gfxSetPaintMode);

    Delay();
}

void testCopySurface(CuTest *tc)
{
    mpe_Error result = 0;
    mpe_GfxSurfaceInfo s1, s2;
    mpe_GfxSurface sf1, sf2;
    mpe_GfxContext ctx1, ctx2;
    mpe_GfxRectangle r =
    { 0, 0, 255, 100 };
    mpe_GfxRectangle r2;
    int i;

    s1.bpp = MPE_GFX_32BPP;
    s1.dim.width = r.width;
    s1.dim.height = r.height;
    s1.format = MPE_GFX_ARGB8888;
    s1.pixeldata = NULL;
    s1.widthbytes = (s1.dim.width * s1.bpp) / 8;

    result = gfxSurfaceNew(&s1, &sf1);
    if (result != MPE_GFX_ERROR_NOERR)
    {
        CuFail(tc, "gfxSurfaceNew failed...");
        return;
    }

    if ((result = gfxContextNew(sf1, &ctx1)) != MPE_GFX_ERROR_NOERR)
    {
        result = gfxSurfaceDelete(sf1);
        ASSERT( gfxSurfaceDelete);
        CuFail(tc, "gfxContextNew failed...");
        return;
    }

    // filling surface 1 with non-opaque pixels
    result = gfxSetPaintMode(ctx1, MPE_GFX_SRC, 255);
\
    ASSERT( gfxSetPaintMode);

    for (i = 0; i < r.height; i += 20)
    {
        r2.x = 0;
        r2.y = i;
        r2.width = r.width;
        r2.height = 20;

        result = gfxSetColor(ctx1, mpe_gfxArgbToColor(i, 0xff, 0xff, 0x00));
        ASSERT( gfxSetColor);

        result = gfxFillRect(ctx1, &r2);
        ASSERT( gfxFillRect);
    }

    result = gfxSetColor(ctx1, mpe_gfxArgbToColor(128, 0xff, 0xff, 0x00));
    ASSERT( gfxSetColor);
    result = gfxSetPaintMode(ctx1, MPE_GFX_SRCOVER, 255);
    ASSERT(gfxSetPaintMode);

    // create surface 2
    s2.bpp = MPE_GFX_32BPP;
    s2.dim.width = r.width;
    s2.dim.height = r.height;
    s2.format = MPE_GFX_ARGB8888;
    s2.pixeldata = NULL;
    s2.widthbytes = (s2.dim.width * s2.bpp) / 8;

    if (gfxSurfaceNew(&s2, &sf2) != MPE_GFX_ERROR_NOERR)
    {
        result = gfxContextDelete(ctx1);
        ASSERT( gfxContextDelete);
        result = gfxSurfaceDelete(sf1);
        ASSERT( gfxSurfaceDelete);
        CuFail(tc, "<<GFX TEST>> gfxSurfaceNew failed\n");
        return;

    }

    if (gfxContextNew(sf2, &ctx2) != MPE_GFX_ERROR_NOERR)
    {
        result = gfxContextDelete(ctx1);
        ASSERT( gfxContextDelete);

        result = gfxSurfaceDelete(sf1);
        ASSERT( gfxSurfaceDelete);

        result = gfxSurfaceDelete(sf2);
        ASSERT(gfxSurfaceDelete);
        CuFail(tc, "<<GFX TEST>> mpeos_gfxContextNew() failed\n");
        return;
    }

    // filling surface 2 with opaque pixels
    // and write an opaque text (red) in it
    result = gfxSetPaintMode(ctx2, MPE_GFX_SRC, 255);
    ASSERT(gfxSetPaintMode);
    result = gfxSetColor(ctx2, mpe_gfxArgbToColor(255, 0x00, 0x00, 0xff));
    ASSERT(gfxSetColor);
    result = gfxFillRect(ctx2, &r);
    ASSERT( gfxFillRect);

    result = gfxSetColor(ctx2, mpe_gfxArgbToColor(255, 0xff, 0x00, 0x00));
    ASSERT(gfxSetColor);
    result = gfxSetFont(ctx2, dialogH);
    ASSERT( gfxSetFont);
    result = gfxDrawString(ctx2, 10, 45, "HELLO", 5);
    ASSERT( gfxDrawString);
    result = gfxSetPaintMode(ctx2, MPE_GFX_SRCOVER, 255);
    ASSERT(gfxSetPaintMode);
    // blit surface 1 into surface 2 - we should see the text thru the non-opaque pixels
    // of surface 1
    result = gfxBitBlt(ctx2, sf1, 0, 0, &r);
    ASSERT( gfxBitBlt);
    // blit surface 2 into screen
    result = gfxBitBlt(screen_ctx, sf2, 50, 50, &r);
    ASSERT(gfxBitBlt);
    result = dispFlushGfxSurface(getDefaultGfx());
    ASSERT( gfxScreenFlush);
}

//
// line animation test
//
static void testLineDance(CuTest *tc)
{
    mpe_Error result = 0;
    int16_t step = 10;
    int32_t colorDir = 1;
    int32_t colorIndex = 0;
    uint32_t colorElement = (rand() % 7) + 1;
    uint8_t red = 0, green = 0, blue = 0;
    int16_t dirX1, dirY1, dirX2, dirY2;
    int16_t i;
    int32_t count = 400;

    line_t line[20];
    mpe_GfxColor erase = mpe_gfxArgbToColor(0xff, 0xff, 0xff, 0xff);
    mpe_GfxColor color = mpe_gfxArgbToColor(0xff, 0x88, 0x88, 0x88);

    // Set up initial line directions

    dirX1 = rand() % step + step - 1;
    dirX2 = rand() % step + step - 1;
    dirY1 = rand() % step - 1;
    dirY2 = rand() % step - 1;

    // Set up initial line end positions

    line[0].x1 = rand() % 640 + 40;
    line[0].y1 = rand() % 496 + 40;
    line[0].x2 = rand() % 640 + 50;
    line[0].y2 = rand() % 496 + 50;

    // Offset the series of lines

    for (i = 1; i < 19; i++)
    {
        line[i].x1 = line[0].x1 - (dirX1 * i);
        line[i].y1 = line[0].y1 - (dirY1 * i);
        line[i].x2 = line[0].x2 - (dirX2 * i);
        line[i].y2 = line[0].y2 - (dirY2 * i);
    }

    // Draw the first set

    for (i = 0; i < 20; i++)
    {
        result = gfxSetColor(ctx, color);
        ASSERT( gfxSetColor);
        result = gfxDrawLine(ctx, line[i].x1, line[i].y1, line[i].x2,
                line[i].y2);
        ASSERT( gfxDrawLine);
        blitToScreen(tc);
        result = dispFlushGfxSurface(getDefaultGfx());
        ASSERT( gfxScreenFlush);
    }
    // Enter our main loop
#if SCREEN_SAVER	
    while(1)
#else
    while (--count)
#endif
    {
        result = gfxSetColor(ctx, erase);
        ASSERT( gfxSetColor);
        result = gfxDrawLine(ctx, line[19].x1, line[19].y1, line[19].x2,
                line[19].y2);
        ASSERT( gfxDrawLine);
        blitToScreen(tc);
        result = dispFlushGfxSurface(getDefaultGfx());
        ASSERT( gfxScreenFlush);

        // Update line position
        for (i = 19; i > 0; i--)
        {
            line[i].x1 = line[i - 1].x1;
            line[i].y1 = line[i - 1].y1;
            line[i].x2 = line[i - 1].x2;
            line[i].y2 = line[i - 1].y2;
        }

        line[0].x1 += dirX1;
        line[0].y1 += dirY1;
        line[0].x2 += dirX2;
        line[0].y2 += dirY2;

        if (line[0].x1 > 680)
            dirX1 = rand() % step - step + 1;

        if (line[0].x2 > 680)
            dirX2 = rand() % step - step + 1;

        if (line[0].y1 > 536)
            dirY1 = rand() % step - step + 1;

        if (line[0].y2 > 536)
            dirY2 = rand() % step - step + 1;

        if (line[0].x1 < 40)
            dirX1 = rand() % step + step - 1;

        if (line[0].x2 < 40)
            dirX2 = rand() % step + step - 1;

        if (line[0].y1 < 40)
            dirY1 = rand() % step + step - 1;

        if (line[0].y2 < 40)
            dirY2 = rand() % step + step - 1;

        colorIndex += colorDir;

        if (colorIndex > 31)
        {
            colorIndex = 30;
            colorDir = -1;
        }
        else if (colorIndex < 0)
        {
            colorIndex = 1;
            colorDir = 1;

            colorElement = (rand() % 7) + 1;
            red = green = blue = 0;
        }

        if (colorElement & 1)
            red = colorIndex * 0x08;
        if (colorElement & 2)
            green = colorIndex * 0x08;
        if (colorElement & 4)
            blue = colorIndex * 0x08;

        color = mpe_gfxArgbToColor(0xff, red, green, blue);
        result = gfxSetColor(ctx, color);
        ASSERT(gfxSetColor);
        result = gfxDrawLine(ctx, line[0].x1, line[0].y1, line[0].x2,
                line[0].y2);
        ASSERT(gfxDrawLine);
        blitToScreen(tc);
        result = dispFlushGfxSurface(getDefaultGfx());
        ASSERT(gfxScreenFlush);

    }
}

void testInit(CuTest *tc)
{
    mpe_File h;
    mpe_Error result = 0;

    mpe_GfxFontDesc fd;
    mpe_GfxFontDesc *sys_fd;
    mpe_GfxRectangle cliprect =
    { 5, 5, 630, 470 };

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s", "'testInit()' - starting\n");

    gfxFontGetList(&sys_fd);

    result = gfxFontFactoryNew(&ff);
    ASSERT( gfxFontFactoryNew);

    if ((result = fileOpen(dialogfont, MPE_FS_OPEN_READ, &h)) == MPE_SUCCESS)
    {
        ASSERT( fileOpen);

        fd.datasize = 13296;
        fileRead(h, &(fd.datasize), dialog_fontbuffer);
        fileClose(h);

        fd.data = dialog_fontbuffer;
        fd.fnt_format = (mpe_GfxFontFormat) 0;
        fd.minsize = 12;
        fd.maxsize = 40;
        fd.name = fontName1;
        fd.namelength = 6;
        fd.style = (mpe_GfxFontStyle) 0;
        result = gfxFontFactoryAdd(ff, &fd);
        ASSERT( gfxFontFactoryAdd);
    }

    if ((result = fileOpen(ameliafont, MPE_FS_OPEN_READ, &h)) == MPE_SUCCESS)
    {
        ASSERT( fileOpen);
        fd.datasize = 20000;
        fileRead(h, &(fd.datasize), amelia_fontbuffer);
        fileClose(h);

        fd.data = amelia_fontbuffer;
        fd.fnt_format = (mpe_GfxFontFormat) 0;
        fd.minsize = 12;
        fd.maxsize = 40;
        fd.name = fontName2;
        fd.namelength = 6;
        fd.style = (mpe_GfxFontStyle) 0;

        result = gfxFontFactoryAdd(ff, &fd);
        ASSERT( gfxFontFactoryAdd);
    }

    /* create the font in the system font factory */
    if (gfxFontNew(NULL, (mpe_GfxWchar*) fontName1, 6, (mpe_GfxFontStyle) 0,
            64, &dialogH) != MPE_GFX_ERROR_NOERR)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "<<GFX TEST>> testInit() Cannot create Font\n");
        return;
    }

    /* create a font in the user factory */
    if (gfxFontNew(NULL, (mpe_GfxWchar*) fontName2, 6, (mpe_GfxFontStyle) 0,
            26, &amelia) != MPE_GFX_ERROR_NOERR)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "<<GFX TEST>> testInit() Cannot create Font\n");
        return;
    }

    if (dispGetGfxSurface(getDefaultGfx(), &screen) != MPE_GFX_ERROR_NOERR)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "<<GFX TEST>> testInit() Cannot get surface from screen\n");
        return;
    }

    if (gfxContextNew(screen, &screen_ctx) != MPE_GFX_ERROR_NOERR)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "<<GFX TEST>> testInit() Cannot create context\n");
        gfxSurfaceDelete(sf);
        return;
    }

    if (use_screen_surface == false)
    {
        mpe_GfxSurfaceInfo s;
        s.bpp = MPE_GFX_32BPP;
        s.dim.width = 640;
        s.dim.height = 480;
        s.format = MPE_GFX_ARGB8888;
        s.pixeldata = NULL;
        s.widthbytes = (s.dim.width * s.bpp) / 8;

        if (gfxSurfaceNew(&s, &sf) != MPE_GFX_ERROR_NOERR)
        {
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                    "<<GFX TEST>> testInit() Cannot create surface\n");
            return;
        }

        if (gfxContextNew(sf, &ctx) != MPE_GFX_ERROR_NOERR)
        {
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                    "<<GFX TEST>> testInit() Cannot create context\n");
            gfxSurfaceDelete(sf);
            return;
        }
    }
    else
    {
        ctx = screen_ctx;
        sf = screen;
    }

    gfxSetFont(ctx, dialogH);
    gfxSetClipRect(ctx, &cliprect);
}

static void testTerminate(CuTest *tc)
{
    gfxFontDelete(dialogH);
    gfxFontDelete(amelia);
    gfxFontFactoryDelete(ff);
    if (!use_screen_surface)
        gfxContextDelete(ctx);
    gfxContextDelete(screen_ctx);
    gfxSurfaceDelete(sf);
}

#if 0 /* TODO: unused function */
void testRun(void)
{
    //  srand( (unsigned)time( NULL ) );

    testInit();
    clearSurface();

#if 1

    int count = 2;
    while(count--)
    {
        srand( (unsigned)time( NULL ) );

        testDrawLine();
        testDrawRect(true);
        testDrawRoundRect(false);
        testDrawRoundRect(true);
        testDrawArc(false);
        testDrawArc(true);
        testDrawRect(false);
        testDrawEllipse(false);
        testDrawEllipse(true);

        testDrawString();
        testCopySurface();
    }
#endif

#if 0   
    mpe_GfxRectangle r=
    {   40,40,560,400};

    clearSurface();
    /* draw a string */
    mpeos_gfxSetColor(ctx,mpe_gfxArgbToColor(0xff,0xff,0x00,0x00));
    /* using the default context font to draw this */
    mpeos_gfxDrawString(ctx,150,150,"LOADING...",10);

    blitToScreen();
    dispFlushGfxSurface(getDefaultGfx());

    // play the video
    //initMpeosVideo( );
    //playVideo(&r);

    //mpeos_gfxSetColor(ctx,mpe_gfxArgbToColor(0xff,0x00,0xff,0x00));
    //mpeos_gfxFillRect(ctx,&r);
    //blitToScreen();
    //dispFlushGfxSurface(getDefaultGfx());   

    // draw a string
    //mpeos_gfxSetColor(ctx,mpe_gfxArgbToColor(0x00,0xff,0x00,0x00));
    //mpeos_gfxDrawString(ctx,70,70,"HELLO!!",7);
    //blitToScreen();
    //dispFlushGfxSurface(getDefaultGfx());

    //mpeos_gfxSetColor(ctx,mpe_gfxArgbToColor(0x10,0x00,0x00,0x00));
    //mpeos_gfxFillRect(ctx,&r);
    //blitToScreen();
    //dispFlushGfxSurface(getDefaultGfx());   


    //testDrawEllipse(true);

    //cleanMpeosVideo( );
#endif

    testTerminate();
}

#endif

CuSuite* getTestSuite_gfxSmoke(void)
{
    CuSuite* suite = CuSuiteNew();

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s",
            "'getTestSuite_gfxSmoke()' - Adding tests . . .\n");

    SUITE_STOP_TEST(suite, testInit);
    SUITE_STOP_TEST(suite, clearSurface);
    SUITE_STOP_TEST(suite, testXorBlit);
    SUITE_STOP_TEST(suite, testLineDance);
    SUITE_STOP_TEST(suite, testDrawLine);
    SUITE_STOP_TEST(suite, testFilledDrawRect);
    SUITE_STOP_TEST(suite, testUnFilledDrawRect);
    SUITE_STOP_TEST(suite, testFilledDrawRoundRect);
    SUITE_STOP_TEST(suite, testUnFilledDrawRoundRect);
    SUITE_STOP_TEST(suite, testFilledDrawArc);
    SUITE_STOP_TEST(suite, testUnFilledDrawArc);
    SUITE_STOP_TEST(suite, testFilledEllipse);
    SUITE_STOP_TEST(suite, testUnFilledEllipse);
    SUITE_STOP_TEST(suite, testDrawString);
    SUITE_STOP_TEST(suite, testCopySurface);
    SUITE_STOP_TEST(suite, testTerminate);

    return suite;
}

void test_gfx_SmokeTest(void)
{
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s", "'test_gfx_SmokeTest()' starting\n");
    test_gfxRunSuite(getTestSuite_gfxSmoke(), "test_gfx_SmokeTest");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s",
            "'test_gfx_SmokeTest()' - Tests complete\n");
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
