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

/*
 * The graphics unit test suite for MPE/MPEOS.
 *
 * @author Aaron Kamienski - Vidiom Systems Corp
 * @author Amir Nathoo - Vidiom Systems Corp
 *
 */

#include <gfx/mpeos_context.h>
#include <mpeos_gfx.h>
#include "test_disp.h"

#define ABS abs

void test_gfxDrawSetup(CuTest *tc);
void dotest_gfxDrawLine(CuTest *tc, int offscreen);
void dotest_gfxDrawRect(CuTest *tc, int offscreen);
void dotest_gfxFillRect(CuTest *tc, int offscreen);
void dotest_gfxClearRect(CuTest *tc, int offscreen);
void dotest_gfxDrawEllipse(CuTest *tc, int offscreen);
void dotest_gfxFillEllipse(CuTest *tc, int offscreen);
void dotest_gfxDrawRoundRect(CuTest *tc, int offscreen);
void dotest_gfxFillRoundRect(CuTest *tc, int offscreen);
void dotest_gfxDrawArc(CuTest *tc, int offscreen);
void dotest_gfxFillArc(CuTest *tc, int offscreen);
void dotest_gfxDrawPolyline(CuTest *tc, int offscreen);
void dotest_gfxDrawPolygon(CuTest *tc, int offscreen);
void dotest_gfxFillPolygon(CuTest *tc, int offscreen);
void dotest_gfxBitBlt(CuTest *tc, int offscreen);
void test_gfxBitBlt_diff(CuTest *tc);
void dotest_gfxStretchBlt(CuTest *tc, int offscreen);
void test_gfxStretchBlt_other(CuTest *tc);
void test_gfxStretchBlt_diff(CuTest *tc);
void dotest_gfxDrawString(CuTest *tc, int offscreen);
void dotest_gfxDrawString16(CuTest *tc, int offscreen);
void dotest_gfxDraw_Clipped(CuTest* tc, int offscreen);
void dotest_gfxDraw_Translated(CuTest* tc, int offscreen);
void dotest_gfxDraw_DistinctState(CuTest* tc, int offscreen);
void test_gfxDraw_DistinctSurface(CuTest *tc);
void test_gfxDraw_DistinctContext(CuTest *tc);
void test_gfxDraw_XOR(CuTest* tc);
void test_gfxBlit_SRCOVER(CuTest* tc);
void test_gfxDraw_SRCOVER(CuTest* tc);
void test_gfxDraw_SRC(CuTest* tc);
void test_gfxDraw_CLR(CuTest* tc);
void test_gfxDraw_OtherPorterDuff(CuTest* tc);
void test_gfxFillPerf(CuTest *tc);
void test_gfxBlitPerf(CuTest *tc);
void test_gfxRunDrawTests(void);
void test_gfxRunDrawSetupTest(void);
void test_gfxDraw_DistinctContextTest(void);
void test_gfxDrawLineTest(void);
void test_gfxDrawLine_offscreenTest(void);
void test_gfxDrawRectTest(void);
void test_gfxDrawRect_offscreenTest(void);
void test_gfxFillRectTest(void);
void test_gfxFillRect_offscreenTest(void);
void test_gfxClearRectTest(void);
void test_gfxClearRect_offscreenTest(void);
void test_gfxDrawEllipseTest(void);
void test_gfxDrawEllipse_offscreenTest(void);
void test_gfxFillEllipseTest(void);
void test_gfxFillEllipse_offscreenTest(void);
void test_gfxDrawRoundRectTest(void);
void test_gfxDrawRoundRect_offscreenTest(void);
void test_gfxFillRoundRectTest(void);
void test_gfxFillRoundRect_offscreenTest(void);
void test_gfxDrawArcTest(void);
void test_gfxDrawArc_offscreenTest(void);
void test_gfxFillArcTest(void);
void test_gfxFillArc_offscreenTest(void);
void test_gfxDrawPolylineTest(void);
void test_gfxDrawPolyline_offscreenTest(void);
void test_gfxDrawPolygonTest(void);
void test_gfxDrawPolygon_offscreenTest(void);
void test_gfxFillPolygonTest(void);
void test_gfxFillPolygon_offscreenTest(void);
void test_gfxBitBltTest(void);
void test_gfxBlit_SRCOVERTest(void);
void test_gfxBitBlt_offscreenTest(void);
void test_gfxBitBlt_diffTest(void);
void test_gfxStretchBltTest(void);
void test_gfxStretchBlt_offscreenTest(void);
void test_gfxStretchBlt_otherTest(void);
void test_gfxStretchBlt_diffTest(void);
void test_gfxDrawStringTest(void);
void test_gfxDrawString_offscreenTest(void);
void test_gfxDrawString16Test(void);
void test_gfxDrawString16_offscreenTest(void);
void test_gfxDraw_ClippedTest(void);
void test_gfxDraw_Clipped_offscreenTest(void);
void test_gfxDraw_TranslatedTest(void);
void test_gfxDraw_Translated_offscreenTest(void);
void test_gfxDraw_DistinctStateTest(void);
void test_gfxDraw_DistinctState_offscreenTest(void);
void test_gfxDraw_DistinctSurfaceTest(void);
void test_gfxDraw_XORTest(void);
void test_gfxDraw_SRCOVERTest(void);
void test_gfxDraw_SRCTest(void);
void test_gfxDraw_CLRTest(void);
void test_gfxDraw_OtherPorterDuffTest(void);
void test_gfxFillPerfTest(void);
void test_gfxBlitPerfTest(void);

void Clear_Rect(CuTest *tc, mpe_GfxContext ctx, mpe_GfxColor bg,
        mpe_GfxRectangle *prect);
void Clear_Bg(CuTest *tc, mpe_GfxContext ctx, mpe_GfxColor bg,
        mpe_GfxSurfaceInfo *info);
void Flush_Screen(CuTest *tc);

void checkLine(CuTest* tc, mpe_GfxSurfaceInfo *info, int x1, int y1, int x2,
        int y2, mpe_GfxColor bg, mpe_GfxColor fg);
void checkRect(CuTest* tc, mpe_GfxSurfaceInfo *info, mpe_GfxRectangle *r,
        mpe_GfxColor bg, mpe_GfxColor fg);
void checkFillRect(CuTest* tc, mpe_GfxSurfaceInfo *info, mpe_GfxRectangle *r,
        mpe_GfxColor bg, mpe_GfxColor fg);
void checkEllipse(CuTest* tc, mpe_GfxSurfaceInfo *info, mpe_GfxRectangle *rect,
        int filled, mpe_GfxColor bg, mpe_GfxColor fg);
void checkRoundRect(CuTest* tc, mpe_GfxSurfaceInfo *info, mpe_GfxRectangle *r,
        int arcWidth, int arcHeight, int filled, mpe_GfxColor bg,
        mpe_GfxColor fg);
static void checkTextBounds(CuTest *tc, mpe_GfxSurfaceInfo *info,
        mpe_GfxRectangle *rect, mpe_GfxColor bg, mpe_GfxColor fg);

void setRect(mpe_GfxRectangle * rect, int x, int y, int width, int height);

static mpe_GfxColor getPixel(mpe_GfxSurfaceInfo *surf, int x, int y);

CuSuite* getTestSuite_gfxDraw(void);
CuSuite* getTest_DrawPolygon(void);

static mpe_DispDevice getDefaultGfx(void);

/*
 * ToDo:
 * - Test paint modes (for each drawing operation; incl SRC, SRCOVER, CLR, XOR)
 * - Correct/finish getPixel
 */

/*
 * Some ways to parameterize these tests...
 * - Paint mode (SRC_OVER, SRC, CLR, XOR)
 * - Different colors
 *
 * !!!!NOTE!!!! we can't just use the tests we have for composition tests.
 * (At least not simply by swapping the expected colors with composePixel()
 * calls on the expected color...)  This is because we may do overlapping
 * calls that will affect the final color where we overlap!
 * We should probably test the overlapping of colors separately.
 *
 * Plan for parameterization/refactoring...
 * 1. rename all test_* functions as dotest_*.
 * 2. add parameter (say offscreen) dotest_* functions
 * 3. for each dotest_* function create a test_*_screen and test_*_offscreen function;
 *    which calls dotest_* with offscreen=false and offscreen=true respectively
 * 4. foreach test_*_screen/test_*_offscreen add a test to the test suite
 *
 * Do similar for paint modes (although a little more complex -- involves changing
 * the way we test colors to at least include composePixel).
 * We're getting into a LOT of tests...
 */

/** Color look up table for RGB565 conversion **/

static unsigned char T5bits[256] =
{ 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3,
        3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6,
        6, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 7, 7, 7, 8, 8, 8, 8, 8, 8, 8, 8, 9, 9,
        9, 9, 9, 9, 9, 9, 10, 10, 10, 10, 10, 10, 10, 10, 11, 11, 11, 11, 11,
        11, 11, 11, 12, 12, 12, 12, 12, 12, 12, 12, 13, 13, 13, 13, 13, 13, 13,
        13, 14, 14, 14, 14, 14, 14, 14, 14, 15, 15, 15, 15, 15, 15, 15, 15, 16,
        16, 16, 16, 16, 16, 16, 16, 17, 17, 17, 17, 17, 17, 17, 17, 18, 18, 18,
        18, 18, 18, 18, 18, 19, 19, 19, 19, 19, 19, 19, 19, 20, 20, 20, 20, 20,
        20, 20, 20, 21, 21, 21, 21, 21, 21, 21, 21, 22, 22, 22, 22, 22, 22, 22,
        22, 23, 23, 23, 23, 23, 23, 23, 23, 24, 24, 24, 24, 24, 24, 24, 24, 25,
        25, 25, 25, 25, 25, 25, 25, 26, 26, 26, 26, 26, 26, 26, 26, 27, 27, 27,
        27, 27, 27, 27, 27, 28, 28, 28, 28, 28, 28, 28, 28, 29, 29, 29, 29, 29,
        29, 29, 29, 30, 30, 30, 30, 30, 30, 30, 30, 31, 31, 31, 31, 31, 31, 31,
        31 };

static unsigned char T6bits[256] =
{ 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6,
        6, 6, 7, 7, 7, 7, 8, 8, 8, 8, 9, 9, 9, 9, 10, 10, 10, 10, 11, 11, 11,
        11, 12, 12, 12, 12, 13, 13, 13, 13, 14, 14, 14, 14, 15, 15, 15, 15, 16,
        16, 16, 16, 17, 17, 17, 17, 18, 18, 18, 18, 19, 19, 19, 19, 20, 20, 20,
        20, 21, 21, 21, 21, 22, 22, 22, 22, 23, 23, 23, 23, 24, 24, 24, 24, 25,
        25, 25, 25, 26, 26, 26, 26, 27, 27, 27, 27, 28, 28, 28, 28, 29, 29, 29,
        29, 30, 30, 30, 30, 31, 31, 31, 31, 32, 32, 32, 32, 33, 33, 33, 33, 34,
        34, 34, 34, 35, 35, 35, 35, 36, 36, 36, 36, 37, 37, 37, 37, 38, 38, 38,
        38, 39, 39, 39, 39, 40, 40, 40, 40, 41, 41, 41, 41, 42, 42, 42, 42, 43,
        43, 43, 43, 44, 44, 44, 44, 45, 45, 45, 45, 46, 46, 46, 46, 47, 47, 47,
        47, 48, 48, 48, 48, 49, 49, 49, 49,

        50, 50, 50, 50, 51, 51, 51, 51, 52, 52, 52, 52, 53, 53, 53, 53, 54, 54,
        54, 54, 55, 55, 55, 55, 56, 56, 56, 56, 57, 57, 57, 57, 58, 58, 58, 58,
        59, 59, 59, 59, 60, 60, 60, 60, 61, 61, 61, 61, 62, 62, 62, 62, 63, 63,
        63, 63 };

/* Convert a 32bits ARGB to 16bits RGB */
#define RGB565(argb32, rgb16)                       \
    do {                                            \
        rgb16 = ( (mpe_gfxGetAlpha(argb32)<<24)       |  \
                  (T5bits[mpe_gfxGetRed(argb32)]<<16) |  \
                  (T6bits[mpe_gfxGetGreen(argb32)]<<8)|  \
                   T5bits[mpe_gfxGetBlue(argb32)] );     \
    } while(0)

/* Return a 32 bits ARGB value depending on the bits format */
#define RGBVALUE(c, format)                          \
    do {                                             \
        switch(format)                               \
      {                                            \
            case MPE_GFX_RGB888:                     \
            case MPE_GFX_ARGB8888:                    \
            break;                               \
            case MPE_GFX_RGB565:                     \
                c = ( (mpe_gfxGetAlpha(c)<<24)|      \
                  (T5bits[mpe_gfxGetRed(c)]<<16)  |  \
                  (T6bits[mpe_gfxGetGreen(c)]<<8) |  \
                   T5bits[mpe_gfxGetBlue(c)] );      \
            break;                         \
            default:                                 \
            break;                               \
      }                                            \
    } while(0)

//#define mpe_gfxArgbToColor(a,r,g,b) ((((a)&0xFF) << 24) | (((r)&0xFF) << 16) | (((g)&0xFF) << 8) | ((b)&0xFF))


/** Extract the color components and premultiply by alpha. */
#define EXTRACT_COLOR(c,a,r,g,b)                \
       do {                                     \
           a = mpe_gfxGetAlpha(c);                   \
           r = mpe_gfxGetRed(c);                     \
           g = mpe_gfxGetGreen(c);                   \
           b = mpe_gfxGetBlue(c);                    \
       } while(0)
/** Premultiply color components by alpha. */
#define PREMULT(a,r,g,b)                        \
       do {                                     \
           r = (a * r)/255;                     \
           g = (a * g)/255;                     \
           b = (a * b)/255;                     \
       } while(0)
/** If alpha is fully transparent, make all black. */
#define TOBLACK(a,r,g,b)                        \
    do {                                        \
           if (a <= 0)                          \
               a = r = g = b = 0;               \
       } while(0)
/** Un-premultiply color components by alpha. */
#define UNPREMULT(a,r,g,b)                      \
    do {                                        \
           if (a > 0 && a < 255)                \
           {                                    \
               r = r*255/a;                     \
               g = g*255/a;                     \
               b = b*255/a;                     \
           }                                    \
       } while(0)

#if 0 /* TODO: unused function */

/*
 *
 All color components listed below refer to color component information pre-multiplied by the corresponding alpha value.  The following identifiers have the attached meaning in the equations below:

 Sc  - The source element color value.
 Sa  - The source element alpha value.
 Dc  - The canvas color value prior to compositing.
 Da  - The canvas alpha value prior to compositing.
 Dc' - The canvas color value post compositing.
 Da' - The canvas alpha value post compositing.


 clear
 Both the color and the alpha of the destination are cleared. Neither the source nor the destination are used as input.

 Dc' = 0
 Da' = 0

 src
 The source is copied to the destination. The destination is not used as input.

 Dc' = Sc
 Da' = Sa

 dst
 The destination is left untouched.

 Dc' = Dc
 Da' = Da

 src_over
 The source is composited over the destination.

 Dc' = Sc + Dc*(1 - Sa)
 Da' = Sa + Da - Sa*Da

 dst_over
 The destination is composited over the source and the result replaces the destination.

 Dc' = Sc*(1 - Da) + Dc
 Da' = Sa + Da - Sa*Da

 src_in
 The part of the source lying inside of the destination replaces the destination.

 Dc' = Sc*Da
 Da' = Sa*Da

 dst_in
 The part of the destination lying inside of the source replaces the destination.

 Dc' = Dc*Sa
 Da' = Sa*Da

 src_out
 The part of the source lying outside of the destination replaces the destination.

 Dc' = Sc*(1 - Da)
 Da' = Sa - Sa*Da

 dst_out
 The part of the destination lying outside of the source replaces the destination.

 Dc' = Dc*(1 - Sa)
 Da' = Da - Sa*Da

 xor
 The part of the source that lies outside of the destination is combined with the part of the destination that lies outside of the source.

 Dc' = Sc*(1 - Da) + Dc*(1 - Sa)
 Da' = Sa + Da - 2*Sa*Da
 */
static mpe_GfxColor composePixel(mpe_GfxColor dest, mpe_GfxColor src,
        mpe_GfxPaintMode mode, uint32_t data)
{
    int sa, sr, sg, sb;
    int da, dr, dg, db;
    int a, r, g, b;
    int ca;
    switch(mode)
    {
        case MPE_GFX_CLR:
        /*
         * Dc' = 0
         * Da' = 0
         */
        return 0;
        case MPE_GFX_DST:
        /*
         * Dc' = Dc
         * Da' = Da
         */
        return dest;
        case MPE_GFX_SRC:
        default:
        /*
         * Dc' = Sc
         * Da' = Sa
         */
        return src;
        /* !!!!NOTE!!!!: the rest of these could use refactoring...
         * they share prolog/epilog!
         */
        case MPE_GFX_SRCOVER:
        /*
         * Dc' = Sc + Dc*(1 - Sa)
         * Da' = Sa + Da - Sa*Da
         */
        // Extract color components
        EXTRACT_COLOR(dest, da, dr, dg, db);
        EXTRACT_COLOR(src, sa, sr, sg, sb);
        // Apply alpha modulation
        sa = (sa * (data & 0xFF))/255; // alpha modulation
        // Pre-multiply color components
        PREMULT(da, dr, dg, db);
        PREMULT(sa, sr, sg, sb);

        // Calculate new color components
        // Alpha = (1-sAlpha)*dAlpha + sAlpha
        // Color = (1-sAlpha)*dColor + sColor
        ca = 255 - sa;

        a = sa + (ca*da)/255;
        r = sr + (ca*dr)/255;
        g = sg + (ca*dg)/255;
        b = sb + (ca*db)/255;

        // go to black if fully transparent
        TOBLACK(a,r,g,b);
        // Un-premultiply the result
        UNPREMULT(a,r,g,b);
        break;
        case MPE_GFX_XOR:
#if 1
        // !!!! NOT CORRECT WHEN TAKING ALPHA INTO ACCOUNT !!!!
        return dest ^ src ^ data;
#else
        // !!!! NOT CORRECT AS DATA VALUE ISN'T USED!!!!!
        // !!!! NOT CORRECT AS USES '+' INSTEAD of '^'!!!!!
        /*
         * Dc' = Sc*(1 - Da) + Dc*(1 - Sa)
         * Da' = Sa + Da - 2*Sa*Da
         */
        // Extract color components
        EXTRACT_COLOR(dest, da, dr, dg, db);
        EXTRACT_COLOR(src, sa, sr, sg, sb);
        // Pre-multiply color components
        PREMULT(da, dr, dg, db);
        PREMULT(sa, sr, sg, sb);

        // Calculate new color components
        // Alpha = (1-sAlpha)*dAlpha + sAlpha
        // Color = (1-sAlpha)*dColor + sColor
        ca = 255 - sa;
        int cda = 255 - da;

        a = sa + da - (2*sa*da)/255;
        r = (sr*cda)/255 + (dr*ca)/255;
        g = (sg*cda)/255 + (dg*ca)/255;
        b = (sb*cda)/255 + (db*ca)/255;

        // go to black if fully transparent
        TOBLACK(a,r,g,b);
        // Un-premultiply the result
        UNPREMULT(a,r,g,b);
        break;
#endif
        case MPE_GFX_DSTOVER:
        /*
         * Dc' = Sc*(1 - Da) + Dc
         * Da' = Sa + Da - Sa*Da
         */
        // Extract color components
        EXTRACT_COLOR(dest, da, dr, dg, db);
        EXTRACT_COLOR(src, sa, sr, sg, sb);
        // Apply alpha modulation
        sa = (sa * (data & 0xFF))/255; // alpha modulation
        // Pre-multiply color components
        PREMULT(da, dr, dg, db);
        PREMULT(sa, sr, sg, sb);

        // Calculate new color components
        // Alpha = sAlpha + dAlpha - (sAlpha*dAlpha)
        // Color = (1-dAlpha)*sColor + dColor
        ca = 255-da;
        a = sa + da - (sa*da)/255;
        r = (ca*sr)/255 + dr;
        g = (ca*sg)/255 + dg;
        b = (ca*sb)/255 + db;

        // go to black if fully transparent
        TOBLACK(a,r,g,b);
        // Un-premultiply the result
        UNPREMULT(a,r,g,b);
        break;
        case MPE_GFX_SRCIN:
        /*
         * Dc' = Sc*Da
         * Da' = Sa*Da
         */
        // Extract color components
        EXTRACT_COLOR(dest, da, dr, dg, db);
        EXTRACT_COLOR(src, sa, sr, sg, sb);
        // Apply alpha modulation
        sa = (sa * (data & 0xFF))/255; // alpha modulation
        // Pre-multiply color components
        PREMULT(da, dr, dg, db);
        PREMULT(sa, sr, sg, sb);

        // Calculate new color components
        // Alpha = sAlpha*dAlpha
        // Color = sColor*dAlpha
        a = (sa*da)/255;
        r = (sr*da)/255;
        g = (sg*da)/255;
        b = (sb*da)/255;

        // go to black if fully transparent
        TOBLACK(a,r,g,b);
        // Un-premultiply the result
        UNPREMULT(a,r,g,b);
        break;
        case MPE_GFX_DSTIN:
        /*
         * Dc' = Dc*Sa
         * Da' = Sa*Da
         */
        // Extract color components
        EXTRACT_COLOR(dest, da, dr, dg, db);
        EXTRACT_COLOR(src, sa, sr, sg, sb);
        // Apply alpha modulation
        sa = (sa * (data & 0xFF))/255; // alpha modulation
        // Pre-multiply color components
        PREMULT(da, dr, dg, db);
        PREMULT(sa, sr, sg, sb);

        // Calculate new color components
        // Alpha = sAlpha*dAlpha
        // Color = dColor*sAlpha
        a = (sa*da)/255;
        r = (dr*sa)/255;
        g = (dg*sa)/255;
        b = (db*sa)/255;

        // go to black if fully transparent
        TOBLACK(a,r,g,b);
        // Un-premultiply the result
        UNPREMULT(a,r,g,b);
        break;
        case MPE_GFX_SRCOUT:
        /*
         * Dc' = Sc*(1 - Da)
         * Da' = Sa - Sa*Da
         */
        // Extract color components
        EXTRACT_COLOR(dest, da, dr, dg, db);
        EXTRACT_COLOR(src, sa, sr, sg, sb);
        // Apply alpha modulation
        sa = (sa * (data & 0xFF))/255; // alpha modulation
        // Pre-multiply color components
        PREMULT(da, dr, dg, db);
        PREMULT(sa, sr, sg, sb);

        // Calculate new color components
        // Alpha = sAlpha - (sAlpha*dAlpha)
        // Color = (1-dAlpha)*sColor
        ca = 255 - da;
        a = sa - (sa*da)/255;
        r = (ca*sr)/255;
        g = (ca*sg)/255;
        b = (ca*sb)/255;

        // go to black if fully transparent
        TOBLACK(a,r,g,b);
        // Un-premultiply the result
        UNPREMULT(a,r,g,b);
        break;
        case MPE_GFX_DSTOUT:
        /*
         * Dc' = Dc*(1 - Sa)
         * Da' = Da - Sa*Da
         */
        // Extract color components
        EXTRACT_COLOR(dest, da, dr, dg, db);
        EXTRACT_COLOR(src, sa, sr, sg, sb);
        // Apply alpha modulation
        sa = (sa * (data & 0xFF))/255; // alpha modulation
        // Pre-multiply color components
        PREMULT(da, dr, dg, db);
        PREMULT(sa, sr, sg, sb);

        // Calculate new color components
        // Alpha = dAlpha - (sAlpha*dAlpha)
        // Color = (1-sAlpha)*dColor
        ca = 255 - sa;
        a = da - (sa*da)/255;
        r = (ca*dr)/255;
        g = (ca*dg)/255;
        b = (ca*db)/255;

        // go to black if fully transparent
        TOBLACK(a,r,g,b);
        // Un-premultiply the result
        UNPREMULT(a,r,g,b);
        break;
    }

    return mpe_gfxArgbToColor(a, r, g, b);
}
#endif

/**
 * Reads a pixel from the surface at the given location.
 */
static mpe_GfxColor getPixel(mpe_GfxSurfaceInfo *surf, int x, int y)
{
    uint16_t pix16;
    uint8_t a = 0xFF, r, g, b;
    char * p = (char*) surf->pixeldata + (x * surf->bpp) / 8 + y
            * surf->widthbytes;
    mpe_GfxColor *pGfxColor = (mpe_GfxColor *) p;
    r = g = b = 0;
    //y = surf->dim.height - y - 1; // WINDOWSism!
    //char* alpha = surf->alpha.data;

    switch (surf->format)
    {

    case MPE_GFX_RGB888: // 24bpp - No alpha value in color
    case MPE_GFX_ARGB8888: // 32bpp - Alpha value in color
        // !!!! Actually BGRx8888 for RGBQUAD
#if 0
        r = p[2];
        g = p[1];
        b = p[0];
        // a? !!!!
#endif

        r = mpe_gfxGetRed(*pGfxColor);
        g = mpe_gfxGetGreen(*pGfxColor);
        b = mpe_gfxGetBlue(*pGfxColor);
        break;

    case MPE_GFX_RGB565: // 16bpp R:5 G:6 B:5 - no alpha in color
        pix16 = *(uint16_t*) p;
        r = (pix16 >> 11) & 0x1F;
        g = (pix16 >> 5) & 0x3F;
        b = (pix16 & 0x1F);
        // a? !!!!
        break;
    default:
        break;
    }
    return mpe_gfxArgbToColor(a, r, g, b);
}

#if 0 /* TODO: unused function */
/**
 * Sets a pixel on the surface at the given location.
 * !!! Doesn't work !!! Probably don't need....
 */
static void setPixel(mpe_GfxSurfaceInfo *surf,
        int x, int y,
        mpe_GfxColor color)
{
    // uint8_t a = mpe_gfxGetAlpha(color);
    uint8_t r = mpe_gfxGetRed(color);
    uint8_t g = mpe_gfxGetGreen(color);
    uint8_t b = mpe_gfxGetBlue(color);
    char* p = (char*)surf->pixeldata + (x*surf->bpp)/8 + y * surf->widthbytes;
    uint16_t pix16;
    switch(surf->format)
    {
        case MPE_GFX_RGB888: // 24bpp - No alpha value in color
        case MPE_GFX_ARGB8888: // 32bpp - Alpha value in color
        // !!!! Actually BGRx8888 for RGBQUAD
        p[2] = r;
        p[1] = g;
        p[0] = b;
        // a? !!!!
        break;

        case MPE_GFX_RGB565: // 16bpp R:5 G:6 B:5 - no alpha in color
        pix16 = ((r & 0x1F) << 11)
        | ((g & 0x3F) << 5)
        | (b & 0x1F);
        *(uint16_t*)p = pix16;
        // a? !!!!
        break;
        default:
        break;
    }
    return;
}
#endif

/**
 * Sets up a test.
 * Accesses/creates a surface, creates a context, and gets surface info.
 */
static mpe_Error setup(mpe_GfxSurface* psurf, mpe_GfxContext* pctx,
        mpe_GfxSurfaceInfo* pinfo, int offscreen)
{
    mpe_Error ec;
    mpe_GfxContext ctx = 0;
    mpe_GfxSurface surf;

    if ((ec = dispGetGfxSurface(getDefaultGfx(), &surf)) == MPE_SUCCESS)
    {
        if (offscreen)
        {
            mpe_GfxSurface screen = surf;
            if ((ec = gfxSurfaceCreate(screen, &surf)) != MPE_SUCCESS)
                return ec;
        }

        *psurf = surf;
        if ((ec = gfxContextNew(surf, &ctx)) != MPE_SUCCESS)
            return ec;

        *pctx = ctx;
        if (pinfo != NULL && (ec = gfxSurfaceGetInfo(surf, pinfo))
                != MPE_SUCCESS)
            return ec;
    }
    return ec;
}

/**
 * Tears down a test.
 * Deletes a context and deletes the offscreen surface (if applicable).
 */
static void teardown(mpe_GfxSurface surf, mpe_GfxContext ctx, int offscreen)
{
    if (ctx != (mpe_GfxContext) 0)
        gfxContextDelete(ctx);
    if (surf != (mpe_GfxSurface) 0 && offscreen)
        gfxSurfaceDelete(surf);
    return;
}

/**
 * Performs common test setup.
 */
#define DO_SETUP                                            \
       mpe_GfxContext ctx = (mpe_GfxContext)0;           \
       mpe_GfxSurface surf = (mpe_GfxSurface)0;          \
       mpe_GfxSurfaceInfo info;                              \
       mpe_Error ec;                                           \
       CuTestSetup(ec = setup(&surf, &ctx, &info, offscreen)); \
       CuAssert(tc, "Test setup failed", ec == MPE_SUCCESS);
/**
 * Performs common test cleanup.
 */
#define DO_CLEANUP                                  \
       CuTestCleanup(teardown(surf, ctx, offscreen));

void Clear_Rect(CuTest *tc, mpe_GfxContext ctx, mpe_GfxColor bg,
        mpe_GfxRectangle *prect)
{
    mpe_GfxColor save;

    CuAssert(tc, "Get color failed", gfxGetColor(ctx, &save) == MPE_SUCCESS);
    CuAssert(tc, "Set color failed", gfxSetColor(ctx, bg) == MPE_SUCCESS);
    CuAssert(tc, "Background fill failed", gfxFillRect(ctx, prect)
            == MPE_SUCCESS);
    CuAssert(tc, "Set color failed", gfxSetColor(ctx, save) == MPE_SUCCESS);
    CuAssert(tc, "Screen Flush failed", dispFlushGfxSurface(getDefaultGfx())
            == MPE_SUCCESS);
}
;

#define CLEAR_RECT(ctx, bg, prect) Clear_Rect(tc, ctx, bg, prect)

void Clear_Bg(CuTest *tc, mpe_GfxContext ctx, mpe_GfxColor bg,
        mpe_GfxSurfaceInfo *info)
{
    mpe_GfxRectangle rect;

    rect.x = 0;
    rect.y = 0;
    rect.width = info->dim.width;
    rect.height = info->dim.height;

    CLEAR_RECT(ctx, bg, &rect);
}

#define CLEAR_BG(ctx, bg) Clear_Bg(tc, ctx, bg, &info)

void Flush_Screen(CuTest *tc)
{
    CuAssert(tc, "Screen Flush failed", dispFlushGfxSurface(getDefaultGfx())
            == MPE_SUCCESS);
}

#define FLUSH_SCREEN() Flush_Screen(tc);

#define THREAD_SLEEP   MPETEST_GFX(threadSleep)
#define TIME_GET_MILLIS   MPETEST_GFX(timeGetMillis)

/**
 * Tests whether we can actually do the pixel testing that
 * we expect to do.
 *
 * @param tc pointer to test case structure
 */
void test_gfxDrawSetup(CuTest *tc)
{
    int offscreen = 0;
    int w, h, x, y;

    mpe_GfxColor fg = mpe_gfxRgbToColor(0xFF, 0x0, 0x0);
    mpe_GfxColor bg = mpe_gfxRgbToColor('R', 'G', 'B');
    DO_SETUP;
    ASSERT_SUCCESS(gfxSetColor, (ctx, fg));

    // White
    CLEAR_BG(ctx, bg);

    w = 30;
    h = 30;
    x = (640 - w / 2);
    y = (480 - h / 2);

    RGBVALUE(fg, info.format);
RGBVALUE(bg, info.format);

CuAssertIntEquals_Msg(tc, "Should read a pixel of the expected color", bg,
    (int) getPixel(&info, x, y));

gfxDrawLine(ctx, 0, 0, 0, 1);
CuAssertIntEquals_Msg(tc, "Should read a pixel of the expected color", fg,
    (int) getPixel(&info, 0, 0));

DO_CLEANUP;

}

/**
 * Verifies that the given line was drawn correctly.
 */
void checkLine(CuTest* tc, mpe_GfxSurfaceInfo *info, int x1, int y1, int x2,
int y2, mpe_GfxColor bg, mpe_GfxColor fg)
{
int fg2 = fg;
int bg2 = bg;
int x, y;
RGBVALUE(fg2, info->format);
RGBVALUE(bg2, info->format);
#if 0
if (x1 > x2)
{
int tmp = x2;
x2 = x1;
x1 = tmp;
}
if (y1 > y2)
{
int tmp = y2;
y2 = y1;
y1 = tmp;
}
#endif

CuAssertColorEquals(tc, "Expected start-point of line to be set", fg2,
getPixel(info, x1, y1));
CuAssertColorEquals(tc, "Expected end-point of line to be set", fg2, getPixel(
info, x2, y2));

#ifndef MAX
#define MAX(a,b) (((a) > (b)) ? (a) : (b))
#endif
#ifndef MIN
#define MIN(a,b) (((a) < (b)) ? (a) : (b))
#endif
if (x1 == x2) // Vertical
{

CuAssertColorEquals(tc, "Expected no additional pixels above end", bg,
getPixel(info, x1, MIN(y1,y2) - 1));
for (y = y1; y <= y2; ++y)
{
CuAssertColorEquals(tc, "Expected no additional pixels to left", bg2, getPixel(
    info, x1 - 1, y));
CuAssertColorEquals(tc, "Expected points in between (vertical) to be set", fg2,
    getPixel(info, x1, y));
CuAssertColorEquals(tc, "Expected no additional pixels to right", bg2,
    getPixel(info, x1 + 1, y));
}
CuAssertColorEquals(tc, "Expected no additional pixels below end", bg2,
getPixel(info, x1, MAX(y1,y2) + 1));
}
else if (y1 == y2) // Horizontal
{
CuAssertColorEquals(tc, "Expected no additional pixels left of end", bg2,
getPixel(info, MIN(x1,x2) - 1, y1));
for (x = x1; x <= x2; ++x)
{
CuAssertColorEquals(tc, "Expected no additional pixels to above", bg2,
    getPixel(info, x, y1 - 1));
CuAssertColorEquals(tc, "Expected points in between (horizontal) to be set",
    fg2, getPixel(info, x, y1));
CuAssertColorEquals(tc, "Expected no additional pixels to left", bg2, getPixel(
    info, x, y1 + 1));
}
CuAssertColorEquals(tc, "Expected no additional pixels right of end", bg2,
getPixel(info, MAX(x1,x2) + 1, y1));

}
else
{
// Ick !!!!!
}
}

/**
 * Tests the gfxDrawLine() call.
 *
 * @param tc pointer to test case structure
 */
void dotest_gfxDrawLine(CuTest *tc, int offscreen)
{
mpe_GfxColor fg = mpe_gfxRgbToColor(0xFF, 0, 0);
mpe_GfxColor bg = mpe_gfxRgbToColor(0, 0, 0);
int x = 30, y = 30;
int w = 10, h = 10;
DO_SETUP;

ASSERT_SUCCESS(gfxSetColor, (ctx, fg));

// 1:30
CLEAR_BG(ctx, bg);
ASSERT_SUCCESS(gfxDrawLine, (ctx, x, y, x + w, y - h));
FLUSH_SCREEN();

// Verify that all pixels (inclusive) are set
checkLine(tc, &info, x, y, x + w, y - h, bg, fg);

// 3:00
CLEAR_BG(ctx, bg);
ASSERT_SUCCESS(gfxDrawLine, (ctx, x, y, x + w, y));
FLUSH_SCREEN();

CuAssert(tc, "gfxDrawLine failed", ec == MPE_SUCCESS);
// Verify that all pixels (inclusive) are set
checkLine(tc, &info, x, y, x + w, y, bg, fg);

// 4:30
CLEAR_BG(ctx, bg);
ASSERT_SUCCESS(gfxDrawLine, (ctx, x, y, x + w, y + h));
FLUSH_SCREEN();
// Verify that all pixels (inclusive) are set
checkLine(tc, &info, x, y, x + w, y + h, bg, fg);

// 6:00
CLEAR_BG(ctx, bg);
ASSERT_SUCCESS(gfxDrawLine, (ctx, x, y, x, y + h));
FLUSH_SCREEN();
// Verify that all pixels (inclusive) are set
checkLine(tc, &info, x, y, x, y + h, bg, fg);

// 7:30
CLEAR_BG(ctx, bg);
ASSERT_SUCCESS(gfxDrawLine, (ctx, x, y, x - w, y + h));
FLUSH_SCREEN();
// Verify that all pixels (inclusive) are set
checkLine(tc, &info, x, y, x - w, y + h, bg, fg);

// 9:00
CLEAR_BG(ctx, bg);
ASSERT_SUCCESS(gfxDrawLine, (ctx, x, y, x - w, y));
FLUSH_SCREEN();
// Verify that all pixels (inclusive) are set
checkLine(tc, &info, x, y, x - w, y, bg, fg);

// 11:30
CLEAR_BG(ctx, bg);
ASSERT_SUCCESS(gfxDrawLine, (ctx, x, y, x - w, y - h));
FLUSH_SCREEN();
// Verify that all pixels (inclusive) are set
checkLine(tc, &info, x, y, x - w, y - h, bg, fg);

// 12:00
CLEAR_BG(ctx, bg);
ASSERT_SUCCESS(gfxDrawLine, (ctx, x, y, x, y - h));
FLUSH_SCREEN();
// Verify that all pixels (inclusive) are set
checkLine(tc, &info, x, y, x, y - h, bg, fg);

DO_CLEANUP;
}

/**
 * Verifies that the given rectangle was drawn correctly.
 * Note that the width of the rect is w+1.
 * Note that the height of the rect is h+1.
 */
void checkRect(CuTest* tc, mpe_GfxSurfaceInfo *info, mpe_GfxRectangle *r,
mpe_GfxColor bg, mpe_GfxColor fg)
{
int x = r->x, y = r->y, w = r->width, h = r->height;
int endX = x + w;
int endY = y + h;

int32_t bg2 = bg;
int32_t fg2 = fg;

int X, Y;

RGBVALUE(fg2, info->format);
RGBVALUE(bg2, info->format);

// Check four corners
CuAssertColorEquals(tc, "Expected NW corner to be set", fg2, getPixel(info, x,
y));
CuAssertColorEquals(tc, "Expected NE corner to be set", fg2, getPixel(info,
endX, y));
CuAssertColorEquals(tc, "Expected SE corner to be set", fg2, getPixel(info,
endX, endY));
CuAssertColorEquals(tc, "Expected SW corner to be set", fg2, getPixel(info, x,
endY));

// Check outside four corners
CuAssertColorEquals(tc, "Expected outside NW corner NOT to be set", bg2,
getPixel(info, x - 1, y));
CuAssertColorEquals(tc, "Expected outside NW corner NOT to be set", bg2,
getPixel(info, x, y - 1));
CuAssertColorEquals(tc, "Expected outside NE corner NOT to be set", bg2,
getPixel(info, endX + 1, y));
CuAssertColorEquals(tc, "Expected outside NE corner NOT to be set", bg2,
getPixel(info, endX, y - 1));
CuAssertColorEquals(tc, "Expected outside SE corner NOT to be set", bg2,
getPixel(info, endX + 1, endY));
CuAssertColorEquals(tc, "Expected outside SE corner NOT to be set", bg2,
getPixel(info, endX, endY + 1));
CuAssertColorEquals(tc, "Expected outside SW corner NOT to be set", bg2,
getPixel(info, x - 1, endY));
CuAssertColorEquals(tc, "Expected outside SW corner NOT to be set", bg2,
getPixel(info, x, endY + 1));

// Horizontal lines... Covers corners as well...
for (X = x + 1; X < endX; ++X)
{
CuAssertColorEquals(tc, "Expected pixels set on top edge", fg2, getPixel(info,
X, y));
CuAssertColorEquals(tc, "Expected pixels set on bottom edge", fg2, getPixel(
info, X, endY));
//if (X > x && X < endX)
{
CuAssertColorEquals(tc, "Expected pixels NOT to be set on above top edge", bg2,
getPixel(info, X, y - 1));
CuAssertColorEquals(tc, "Expected pixels NOT to be set on below top edge", bg2,
getPixel(info, X, y + 1));
CuAssertColorEquals(tc, "Expected pixels NOT to be set on above bottom edge",
bg2, getPixel(info, X, endY - 1));
CuAssertColorEquals(tc, "Expected pixels NOT to be set on below bottom edge",
bg2, getPixel(info, X, endY + 1));
}
}
// Vertical lines... Covers corners as well...
for (Y = y + 1; Y < endY; ++Y)
{
CuAssertColorEquals(tc, "Expected pixels set on left edge", fg2, getPixel(info,
x, Y));
CuAssertColorEquals(tc, "Expected pixels set on right edge", fg2, getPixel(
info, endX, Y));
//if (Y > y && Y < endY)
{
CuAssertColorEquals(tc, "Expected pixels NOT to be set left of left edge", bg2,
getPixel(info, x - 1, Y));
CuAssertColorEquals(tc, "Expected pixels NOT to be set right of left edge",
bg2, getPixel(info, x + 1, Y));
CuAssertColorEquals(tc, "Expected pixels NOT to be set left of right edge",
bg2, getPixel(info, endX - 1, Y));
CuAssertColorEquals(tc, "Expected pixels NOT to be set right of right edge",
bg2, getPixel(info, endX + 1, Y));
}
}
}

/**
 * Tests the gfxDrawRect() call.
 *
 * @param tc pointer to test case structure
 */
void dotest_gfxDrawRect(CuTest *tc, int offscreen)
{
mpe_GfxColor fg = mpe_gfxRgbToColor(0, 0xFF, 0);
mpe_GfxColor bg = mpe_gfxRgbToColor(0, 0, 0);

int x = 100, y = 100;
int w = 30, h = 20;

mpe_GfxRectangle rect;
DO_SETUP;

rect.x = x;
rect.y = y;
rect.width = w;
rect.height = h;

ASSERT_SUCCESS(gfxSetColor, (ctx, fg));

CLEAR_BG(ctx, bg);
rect.width = w;
rect.height = h;
ASSERT_SUCCESS(gfxDrawRect, (ctx, &rect));
FLUSH_SCREEN();
// Verify that rectangle was drawn correctly
checkRect(tc, &info, &rect, bg, fg);

#if 0
CLEAR_BG(ctx, bg);
rect.width = -w;
rect.height = h;
ASSERT_SUCCESS(gfxDrawRect, (ctx, &rect));
FLUSH_SCREEN();
// Verify that rectangle was drawn correctly
// !!!!checkRect doesn't work with negatives...
checkRect(tc, &info, &rect, bg, fg);

CLEAR_BG(ctx, bg);
rect.width = w;
rect.height = -h;
ASSERT_SUCCESS(gfxDrawRect, (ctx, &rect));
FLUSH_SCREEN();
// Verify that rectangle was drawn correctly
// !!!!checkRect doesn't work with negatives...
checkRect(tc, &info, &rect, bg, fg);

CLEAR_BG(ctx, bg);
rect.width = -w;
rect.height = -h;
ASSERT_SUCCESS(gfxDrawRect, (ctx, &rect));
FLUSH_SCREEN();
// Verify that rectangle was drawn correctly
// !!!!checkRect doesn't work with negatives...
checkRect(tc, &info, &rect, bg, fg);
#endif

DO_CLEANUP;
}

/**
 * Verifies that the given rectangle was drawn correctly.
 * Note that the width of the rect is w.
 * Note that the height of the rect is h.
 */
void checkFillRect(CuTest* tc, mpe_GfxSurfaceInfo *info, mpe_GfxRectangle *r,
mpe_GfxColor bg, mpe_GfxColor fg)
{
int x = r->x, y = r->y, w = r->width, h = r->height;
int endX = x + w - 1;
int endY = y + h - 1;
int X, Y;

int32_t bg2 = bg;
int32_t fg2 = fg;

RGBVALUE(fg2, info->format);
RGBVALUE(bg2, info->format);
for (X = x; X <= endX; ++X)
for (Y = y; Y <= endY; ++Y)
CuAssertColorEquals(tc, "interior of rect should be filled", fg2, getPixel(
info, X, Y));
for (X = x - 1; X <= endX + 1; ++X)
{
CuAssertColorEquals(tc, "exterior of rect should NOT be filled", bg2, getPixel(
info, X, y - 1));
CuAssertColorEquals(tc, "exterior of rect should NOT be filled", bg2, getPixel(
info, X, endY + 1));
}
for (Y = y - 1; Y <= endY + 1; ++Y)
{
CuAssertColorEquals(tc, "exterior of rect should NOT be filled", bg2, getPixel(
info, x - 1, Y));
CuAssertColorEquals(tc, "exterior of rect should NOT be filled", bg2, getPixel(
info, endX + 1, Y));
}
}

/**
 * Tests the gfxFillRect() call.
 *
 * @param tc pointer to test case structure
 */
void dotest_gfxFillRect(CuTest *tc, int offscreen)
{
mpe_GfxColor fg = mpe_gfxRgbToColor(0, 0xFF, 0);
mpe_GfxColor bg = mpe_gfxRgbToColor(0, 0, 0);

int x = 300, y = 300;
int w = 20, h = 30;

mpe_GfxRectangle rect;
DO_SETUP;

rect.x = x;
rect.y = y;
rect.width = w;
rect.height = h;

ASSERT_SUCCESS(gfxSetColor, (ctx, fg));

CLEAR_BG(ctx, bg);
rect.width = w;
rect.height = h;
ASSERT_SUCCESS(gfxFillRect, (ctx, &rect));
FLUSH_SCREEN();
// Verify that rectangle was drawn correctly
checkFillRect(tc, &info, &rect, bg, fg);

#if 0
CLEAR_BG(ctx, bg);
rect.width = -w;
rect.height = h;
ASSERT_SUCCESS(gfxFillRect, (ctx, &rect));
FLUSH_SCREEN();
// Verify that rectangle was drawn correctly
// !!!!checkFillRect doesn't work with negatives...
checkFillRect(tc, info, rect, bg, fg);

CLEAR_BG(ctx, bg);
rect.width = w;
rect.height = -h;
ASSERT_SUCCESS(gfxFillRect, (ctx, &rect));
FLUSH_SCREEN();
// Verify that rectangle was drawn correctly
// !!!!checkFillRect doesn't work with negatives...
checkFillRect(tc, info, rect, bg, fg);

CLEAR_BG(ctx, bg);
rect.width = -w;
rect.height = -h;
ASSERT_SUCCESS(gfxFillRect, (ctx, &rect));
FLUSH_SCREEN();
// Verify that rectangle was drawn correctly
// !!!!checkFillRect doesn't work with negatives...
checkFillRect(tc, info, rect, bg, fg);
#endif

DO_CLEANUP;
}

/**
 * Tests the gfxClearRect() call.
 * This tests the pixels for the color set (different than default green = RED).
 * Also, it verifies that the color in the current context remains the same
 * before and after the clear. Also, check that mode and alpha (modedata) are
 * the same as before.
 *
 * @param tc pointer to test case structure
 */
void dotest_gfxClearRect(CuTest *tc, int offscreen)
{
mpe_GfxColor fg = mpe_gfxRgbToColor(0, 0xFF, 0); // Green
mpe_GfxColor bg = mpe_gfxRgbToColor(0, 0, 0); // Black
mpe_GfxColor newColor = mpe_gfxRgbToColor(0xFF, 0, 0); // Red

mpeos_GfxContext *c;

int x = 300, y = 300; // Define rectangle to use
int w = 20, h = 30;

mpe_GfxColor saveColor;
mpe_GfxPaintMode saveMode;
int32_t saveData;

mpe_GfxRectangle rect;
DO_SETUP;

c = (mpeos_GfxContext *) ctx; // Get Context
saveColor = fg; // Save color, mode and mode data.
saveMode = c->paintmode;
saveData = c->modedata;

rect.x = x;
rect.y = y;
rect.width = w;
rect.height = h;

// Initially set context foreground and background colors
// Clear the background (full screen) first.
ASSERT_SUCCESS(gfxSetColor, (ctx, fg));
CLEAR_BG(ctx, bg);

rect.width = w;
rect.height = h;
ASSERT_SUCCESS(gfxClearRect, (ctx, &rect, newColor));
FLUSH_SCREEN();
// Verify that rectangle was drawn correctly
checkFillRect(tc, &info, &rect, bg, newColor);

// Now verify paint mode, modedata and context color.
CuAssertIntEquals_Msg(tc, "Paint mode not as expected", saveMode, c->paintmode);
CuAssertIntEquals_Msg(tc, "Mode data not as expected", saveData, c->modedata);
CuAssertIntEquals_Msg(tc, "Final color not as expected", saveColor, c->color);

DO_CLEANUP;
}

/**
 * Verifies the bounds of an ellipse.
 */
void checkEllipse(CuTest* tc, mpe_GfxSurfaceInfo *info, mpe_GfxRectangle *rect,
int filled, mpe_GfxColor bg, mpe_GfxColor fg)
{
int x = rect->x;
int y = rect->y;
int width = rect->width;
int height = rect->height;
int hdiam = filled ? (width) : width + 1;
int vdiam = filled ? (height) : height + 1;
//int hdiam = filled ? (width-1) : width;
//int vdiam = filled ? (height-1) : height;


int hrad = hdiam / 2;
int vrad = vdiam / 2;

int32_t bg2 = bg;
int32_t fg2 = fg;
RGBVALUE(fg2, info->format);
RGBVALUE(bg2, info->format);

// Verify 4 corners
CuAssertColorEquals(tc, "N corner should be set", fg2, getPixel(info, x + hrad
- 1, y));
CuAssertColorEquals(tc, "E corner should be set", fg2, getPixel(info, x + hdiam
- 1, y + vrad - 1));
CuAssertColorEquals(tc, "S corner should be set", fg2, getPixel(info, x + hrad
- 1, y + vdiam - 1));
CuAssertColorEquals(tc, "W corner should be set", fg2, getPixel(info, x, y
+ vrad));

// Verify beyond 4 corners
CuAssertColorEquals(tc, "beyond N corner should NOT be set", bg2, getPixel(
info, x + hrad, y - 1));
CuAssertColorEquals(tc, "beyond E corner should NOT be set", bg2, getPixel(
info, x + hdiam + 1, y + vrad));
CuAssertColorEquals(tc, "beyond S corner should NOT be set", bg2, getPixel(
info, x + hrad, y + vdiam + 1));
CuAssertColorEquals(tc, "beyond W corner should NOT be set", bg2, getPixel(
info, x - 1, y + vrad));

// Verify interior
if (filled)
CuAssertColorEquals(tc, "Interior should've been set", fg2, getPixel(info, x
+ hrad, y + vrad));
else
CuAssertColorEquals(tc, "Interior should NOT have been set", bg2, getPixel(
info, x + hrad, y + vrad));

return;
}

/**
 * Tests the gfxDrawEllipse() call.
 *
 * @param tc pointer to test case structure
 */
void dotest_gfxDrawEllipse(CuTest *tc, int offscreen)
{
mpe_GfxColor fg = mpe_gfxRgbToColor(0, 0, 0xFF);
mpe_GfxColor bg = mpe_gfxRgbToColor(0, 0, 0);

int x = 400, y = 400;
int w = 30, h = 50;

mpe_GfxRectangle rect;
DO_SETUP;

rect.x = x;
rect.y = y;
rect.width = w;
rect.height = h;

ASSERT_SUCCESS(gfxSetColor, (ctx, fg));

CLEAR_BG(ctx, bg);
rect.width = w;
rect.height = h;
ASSERT_SUCCESS(gfxDrawEllipse, (ctx, &rect));
FLUSH_SCREEN();
// Verify that ellipse was drawn correctly
checkEllipse(tc, &info, &rect, 0, bg, fg);

#if 0
CLEAR_BG(ctx, bg);
rect.width = -w;
rect.height = h;
ASSERT_SUCCESS(gfxDrawEllipse, (ctx, &rect));
FLUSH_SCREEN();
// Verify that ellipse was drawn correctly

CLEAR_BG(ctx, bg);
rect.width = w;
rect.height = -h;
ASSERT_SUCCESS(gfxDrawEllipse, (ctx, &rect));
FLUSH_SCREEN();
// Verify that ellipse was drawn correctly

CLEAR_BG(ctx, bg);
rect.width = -w;
rect.height = -h;
ASSERT_SUCCESS(gfxDrawEllipse, (ctx, &rect));
FLUSH_SCREEN();
// Verify that ellipse was drawn correctly
#endif
DO_CLEANUP;
}

/**
 * Tests the gfxFillEllipse() call.
 *
 * @param tc pointer to test case structure
 */
void dotest_gfxFillEllipse(CuTest *tc, int offscreen)
{
mpe_GfxColor fg = mpe_gfxRgbToColor(0, 0, 0xFF);
mpe_GfxColor bg = mpe_gfxRgbToColor(0, 0, 0);

int x = 250, y = 250;
int w = 50, h = 30;

mpe_GfxRectangle rect;
DO_SETUP;

rect.x = x;
rect.y = y;
rect.width = w;
rect.height = h;

ASSERT_SUCCESS(gfxSetColor, (ctx, fg));

CLEAR_BG(ctx, bg);
rect.width = w;
rect.height = h;
ASSERT_SUCCESS(gfxFillEllipse, (ctx, &rect));
FLUSH_SCREEN();
// Verify that rectangle was drawn correctly
checkEllipse(tc, &info, &rect, 1, bg, fg);

#if 0
CLEAR_BG(ctx, bg);
rect.width = -w;
rect.height = h;
ASSERT_SUCCESS(gfxFillEllipse, (ctx, &rect));
FLUSH_SCREEN();
// Verify that rectangle was drawn correctly

CLEAR_BG(ctx, bg);
rect.width = w;
rect.height = -h;
ASSERT_SUCCESS(gfxFillEllipse, (ctx, &rect));
FLUSH_SCREEN();
// Verify that rectangle was drawn correctly

CLEAR_BG(ctx, bg);
rect.width = -w;
rect.height = -h;
ASSERT_SUCCESS(gfxFillEllipse, (ctx, &rect));
FLUSH_SCREEN();
// Verify that rectangle was drawn correctly
#endif

DO_CLEANUP;
}

/**
 * Verifies a rendered roundRect.
 */
void checkRoundRect(CuTest* tc, mpe_GfxSurfaceInfo *info, mpe_GfxRectangle *r,
int arcWidth, int arcHeight, int filled, mpe_GfxColor bg, mpe_GfxColor fg)
{
int x = r->x;
int y = r->y;
int w = 0;
int h = 0;
//int endX = x + (filled ? (w ) : w-1);
//int endY = y + (filled ? (h ) : h-1);

int endX;
int endY;

// int wRad = arcWidth/2;
// int hRad = arcHeight/2;
int midX;
int midY;

int32_t fg2;
int32_t bg2;
if (filled)
{
w = r->width;
h = r->height;
}
else
{
w = r->width + 1;
h = r->height + 1;
}
endX = x + w - 1;
endY = y + h - 1;
midX = (x + endX) / 2;
midY = (y + endY) / 2;

fg2 = fg;
bg2 = bg;

RGBVALUE(fg2, info->format);
RGBVALUE(bg2, info->format);

// Verify 4 edges
CuAssertColorEquals(tc, "N edge should be set", fg2, getPixel(info, midX, y));
CuAssertColorEquals(tc, "E edge should be set", fg2, getPixel(info, endX, midY));
if (filled)
CuAssertColorEquals(tc, "S edge should be set", fg2, getPixel(info, midX, endY
- 1));
else
CuAssertColorEquals(tc, "S edge should be set", fg2, getPixel(info, midX, endY));

CuAssertColorEquals(tc, "W edge should be set", fg2, getPixel(info, x, midY));

// Verify beyond 4 edges
CuAssertColorEquals(tc, "beyond N edge should NOT be set", bg2, getPixel(info,
midX, y - 1));
CuAssertColorEquals(tc, "beyond E edge should NOT be set", bg2, getPixel(info,
endX + 1, midY));
CuAssertColorEquals(tc, "beyond S edge should NOT be set", bg2, getPixel(info,
midX, endY + 1));
CuAssertColorEquals(tc, "beyond W edge should NOT be set", bg2, getPixel(info,
x - 1, midY));

// Verify 4 corners
CuAssertColorEquals(tc, "NE corner should NOT be set", bg2, getPixel(info,
endX, y));
CuAssertColorEquals(tc, "SE corner should NOT be set", bg2, getPixel(info,
endX, endY));
CuAssertColorEquals(tc, "SW corner should NOT be set", bg2, getPixel(info, x,
endY));
CuAssertColorEquals(tc, "NW corner should NOT be set", bg2,
getPixel(info, x, y));

// Verify arcs...
}

/**
 * Tests the gfxDrawRoundRect() call.
 *
 * @param tc pointer to test case structure
 */
void dotest_gfxDrawRoundRect(CuTest *tc, int offscreen)
{
mpe_GfxColor fg = mpe_gfxRgbToColor(0, 0xFF, 0xFF);
mpe_GfxColor bg = mpe_gfxRgbToColor(0, 0, 0);

int x = 500, y = 200;
int w = 50, h = 70;

mpe_GfxRectangle rect;
DO_SETUP;

rect.x = x;
rect.y = y;
rect.width = w;
rect.height = h;

ASSERT_SUCCESS(gfxSetColor, (ctx, fg));

CLEAR_BG(ctx, bg);

TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "before DrawRoundRect Test\n");
rect.width = w;
rect.height = h;
ASSERT_SUCCESS(gfxDrawRoundRect, (ctx, &rect, 0, 0));
TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "after DrawRoundRect Test\n");

ASSERT_SUCCESS(gfxDrawRoundRect, (ctx, &rect, w / 4, h / 4));
FLUSH_SCREEN();
// Verify that round rect was drawn correctly
checkRoundRect(tc, &info, &rect, w / 4, h / 4, 0, bg, fg);

#if 0
CLEAR_BG(ctx, bg);
rect.width = -w;
rect.height = h;
ASSERT_SUCCESS(gfxDrawRoundRect, (ctx, &rect, w/4, h/4));
FLUSH_SCREEN();
// Verify that round rect was drawn correctly

CLEAR_BG(ctx, bg);
rect.width = w;
rect.height = -h;
ASSERT_SUCCESS(gfxDrawRoundRect, (ctx, &rect, w/4, h/4));
FLUSH_SCREEN();
// Verify that round rect was drawn correctly

CLEAR_BG(ctx, bg);
rect.width = -w;
rect.height = -h;
ASSERT_SUCCESS(gfxDrawRoundRect, (ctx, &rect, w/4, h/4));
FLUSH_SCREEN();
// Verify that round rect was drawn correctly
#endif

DO_CLEANUP;
}

/**
 * Tests the gfxFillRoundRect() call.
 *
 * @param tc pointer to test case structure
 */
void dotest_gfxFillRoundRect(CuTest *tc, int offscreen)
{
mpe_GfxColor fg = mpe_gfxRgbToColor(0, 0xFF, 0xFF);
mpe_GfxColor bg = mpe_gfxRgbToColor(0, 0, 0);

int x = 400, y = 200;
int w = 70, h = 50;

mpe_GfxRectangle rect;
DO_SETUP;

rect.x = x;
rect.y = y;
rect.width = w;
rect.height = h;

ASSERT_SUCCESS(gfxSetColor, (ctx, fg));

CLEAR_BG(ctx, bg);
rect.width = w;
rect.height = h;
ASSERT_SUCCESS(gfxFillRoundRect, (ctx, &rect, w / 4, h / 4));
FLUSH_SCREEN();
// Verify that round rect was drawn correctly
checkRoundRect(tc, &info, &rect, w / 4, h / 4, 1, bg, fg);

#if 0
CLEAR_BG(ctx, bg);
rect.width = -w;
rect.height = h;
ASSERT_SUCCESS(gfxFillRoundRect, (ctx, &rect, w/4, h/4));
FLUSH_SCREEN();
// Verify that round rect was drawn correctly

CLEAR_BG(ctx, bg);
rect.width = w;
rect.height = -h;
ASSERT_SUCCESS(gfxFillRoundRect, (ctx, &rect, w/4, h/4));
FLUSH_SCREEN();
// Verify that round rect was drawn correctly

CLEAR_BG(ctx, bg);
rect.width = -w;
rect.height = -h;
ASSERT_SUCCESS(gfxFillRoundRect, (ctx, &rect, w/4, h/4));
FLUSH_SCREEN();
// Verify that round rect was drawn correctly

#endif

DO_CLEANUP;
}

/**
 * Tests the gfxDrawArc() call.
 *
 * @param tc pointer to test case structure
 */
void dotest_gfxDrawArc(CuTest *tc, int offscreen)
{
mpe_Error result = 0;
mpe_GfxColor fg = mpe_gfxRgbToColor(0xFF, 0xFF, 0);
mpe_GfxColor bg = mpe_gfxRgbToColor(0, 0, 0);

int32_t sa = 45;
int32_t aa = 90;

int x = 100, y = 100;
int w = 40, h = 60;

mpe_GfxRectangle rect;
#ifdef INTERACTIVE
int ok = 0;
#endif

DO_SETUP;

rect.x = x;
rect.y = y;
rect.width = w;
rect.height = h;

//Set the color of the arc to be created
result = gfxSetColor(ctx, fg);
ASSERT( gfxSetColor);

CLEAR_BG(ctx, bg);

rect.width = w;
rect.height = h;

gfxDrawArc(ctx, &rect, sa, aa);
ASSERT( gfxDrawArc);

FLUSH_SCREEN();
// Verify that arc was drawn correctly

#ifdef INTERACTIVE
USER_YESNO("Was an arc drawn?", "Arc?", ok);
CuAssert(tc, "Did not draw a good arc", ok);
#endif

DO_CLEANUP;
}

/**
 * Tests the gfxFillArc() call.
 *
 * @param tc pointer to test case structure
 */
void dotest_gfxFillArc(CuTest *tc, int offscreen)
{
mpe_GfxColor fg = mpe_gfxRgbToColor(0xFF, 0x80, 0x80);
mpe_GfxColor bg = mpe_gfxRgbToColor(0, 0, 0);

int x = 100, y = 100;
int w = 40, h = 60;

mpe_GfxRectangle rect;
#ifdef INTERACTIVE
int ok = 0;
#endif

DO_SETUP;

rect.x = x;
rect.y = y;
rect.width = w;
rect.height = h;

ASSERT_SUCCESS(gfxSetColor, (ctx, fg));

CLEAR_BG(ctx, bg);
rect.width = w;
rect.height = h;
ASSERT_SUCCESS(gfxFillArc, (ctx, &rect, 30, 200));
FLUSH_SCREEN();
// Verify that arc was drawn correctly

#ifdef INTERACTIVE
USER_YESNO("Was an arc drawn filled?", "Filled Arc?", ok);
CuAssert(tc, "Did not draw a good arc", ok);
#endif

DO_CLEANUP;
}

/**
 * Tests the gfxDrawPolyline() call.
 *
 * @param tc pointer to test case structure
 */
void dotest_gfxDrawPolyline(CuTest *tc, int offscreen)
{
mpe_GfxColor fg = mpe_gfxRgbToColor(0x80, 0xFF, 0x80);
mpe_GfxColor bg = mpe_gfxRgbToColor(0, 0, 0);
int32_t xCoords[] =
{ 1, 0, 3, 0, 2, 3, 2 };
int32_t yCoords[] =
{ 0, 1, 4, 4, 2, 1, 0 };
static const int SCALE = 20;
static const int TRANS = 100;
static const int XSIZE = (sizeof(xCoords) / sizeof(int32_t));
static const int YSIZE = (sizeof(yCoords) / sizeof(int32_t));
int32_t fg2 = fg;
int32_t bg2 = bg;
int i, x;
DO_SETUP;

ASSERT_SUCCESS(gfxSetColor, (ctx, fg));

CuAssert(tc, "Test is broken - xCoords.length != yCoords.length", XSIZE
== YSIZE);

RGBVALUE(fg2, info.format);
RGBVALUE(bg2, info.format);

for (i = 0; i < XSIZE; ++i)
{
xCoords[i] = xCoords[i] * SCALE + TRANS;
yCoords[i] = yCoords[i] * SCALE + TRANS;
}

CLEAR_BG(ctx, bg);
ASSERT_SUCCESS(gfxDrawPolyline, (ctx, xCoords, yCoords, XSIZE));
FLUSH_SCREEN();

// Verify that all points/lines have been drawn
for (i = 0; i < XSIZE; ++i)
CuAssertColorEquals(tc, "Expected each end point to be drawn", fg2, getPixel(
&info, xCoords[i], yCoords[i]));

// Verify that the 1st/last points have NOT been connected
CuAssert(tc, "Test is broken, expected first/last endpoints to be at same y",

yCoords[0] == yCoords[YSIZE - 1]);
for (x = xCoords[0] + 1; x < xCoords[XSIZE - 1] - 1; ++x)
CuAssertColorEquals(tc, "first and last points should be unconnected", bg2,
getPixel(&info, x, yCoords[0]));

DO_CLEANUP;
}

/**
 * Tests the gfxDrawPolygon() call.
 *
 * @param tc pointer to test case structure
 */
void dotest_gfxDrawPolygon(CuTest *tc, int offscreen)
{
mpe_GfxColor fg = mpe_gfxRgbToColor(0x80, 0x80, 0xFF);
mpe_GfxColor bg = mpe_gfxRgbToColor(0, 0, 0);
int32_t xCoords[] =
{ 1, 0, 1, 3, 0, 3, 2 };
int32_t yCoords[] =
{ 0, 1, 2, 4, 4, 1, 0 };
static const int SCALE = 20;
static const int TRANS = 200;
static const int XSIZE = (sizeof(xCoords) / sizeof(int32_t));
static const int YSIZE = (sizeof(yCoords) / sizeof(int32_t));
int i, x;
DO_SETUP;

ASSERT_SUCCESS(gfxSetColor, (ctx, fg));

CuAssert(tc, "Test is broken - xCoords.length != yCoords.length", XSIZE
== YSIZE);

for (i = 0; i < XSIZE; ++i)
{
xCoords[i] = xCoords[i] * SCALE + TRANS;
yCoords[i] = yCoords[i] * SCALE + TRANS;
}

CLEAR_BG(ctx, bg);
ASSERT_SUCCESS(gfxDrawPolygon, (ctx, xCoords, yCoords, XSIZE));
FLUSH_SCREEN();

// Verify that all points/lines have been drawn
for (i = 0; i < XSIZE; ++i)
CuAssertColorEquals(tc, "Expected each end point to be drawn", fg, getPixel(
&info, xCoords[i], yCoords[i]));

// Verify that the 1st/last points have been connected
CuAssert(tc, "Test is broken, expected first/last endpoints to be at same y",
yCoords[0] == yCoords[YSIZE - 1]);
for (x = xCoords[0] + 1; x < xCoords[XSIZE - 1] - 1; ++x)
CuAssertColorEquals(tc, "first and last points should be connected", fg,
getPixel(&info, x, yCoords[0]));
DO_CLEANUP;
}

/**
 * Tests the gfxFillPolygon() call.
 *
 * @param tc pointer to test case structure
 */
void dotest_gfxFillPolygon(CuTest *tc, int offscreen)
{
mpe_GfxColor fg = mpe_gfxRgbToColor(0x80, 0x80, 0xFF);
mpe_GfxColor bg = mpe_gfxRgbToColor(0, 0, 0);

//int32_t xCoords[] = { 1, 3, 3, 1, 1, 4, 4, 0, 0 };
//int32_t yCoords[] = { 1, 1, 3, 3, 1, 4, 0, 4, 0 };
int32_t xCoords[] =
{ 3, 3, 1, 1, 4, 4, 0, 0, 1 };
int32_t yCoords[] =
{ 1, 3, 3, 1, 4, 0, 4, 0, 1 };
static const int SCALE = 20;
static const int TRANS = 300;
static const int XSIZE = (sizeof(xCoords) / sizeof(int32_t));
static const int YSIZE = (sizeof(yCoords) / sizeof(int32_t));
int i;
DO_SETUP;

ASSERT_SUCCESS(gfxSetColor, (ctx, fg));

CuAssert(tc, "Test is broken - xCoords.length != yCoords.length", XSIZE
== YSIZE);

for (i = 0; i < XSIZE; ++i)
{
xCoords[i] = xCoords[i] * SCALE + TRANS;
yCoords[i] = yCoords[i] * SCALE + TRANS;
}

CLEAR_BG(ctx, bg);
ASSERT_SUCCESS(gfxFillPolygon, (ctx, xCoords, yCoords, XSIZE));
FLUSH_SCREEN();
// Verify that all points/lines have been drawn
#if 0
// This is not necessarily gonna happen
// Because the even/odd filling may cover the points, giving us bg on the points.
for(i = 0; i < XSIZE; ++i)
CuAssertColorEquals(tc, "Expected each end point to be drawn",
fg, getPixel(&info, xCoords[i], yCoords[i]));
#endif
// Verify that the 1st/last points have been connected
CuAssertColorEquals(tc, "Expected final points to be connected", fg, getPixel(
&info, (xCoords[0] + xCoords[XSIZE - 1]) / 2, (yCoords[0] + yCoords[YSIZE - 1])
/ 2));
// Verify fill (this is dependent on the path taken above!
// So don't change it without fixing!!!
//   |\__/|
//   ||\/||  All should be filled but the two inside triangles
//   ||/\||  that are pointing toward the center, horizontally.
//   |/--\|
CuAssertColorEquals(tc, "Interior should be filled", fg, getPixel(&info,
xCoords[0] + 1, yCoords[0]));
CuAssertColorEquals(tc, "Double interior should NOT be filled", bg, getPixel(
&info, xCoords[0] - 1, (yCoords[0] + yCoords[1]) / 2));

DO_CLEANUP;
}

/**
 * Tests the gfxBitBlt() call.
 *
 * @param tc pointer to test case structure
 */
void dotest_gfxBitBlt(CuTest *tc, int offscreen)
{
mpe_GfxColor fg = mpe_gfxRgbToColor(0x22, 0x70, 0x43);
mpe_GfxColor bg = mpe_gfxRgbToColor(0, 0, 0);
// Let's draw an initial figure in a location
int32_t xCoords[] =
{ 1, 2, 2, 1 };
int32_t yCoords[] =
{ 1, 1, 2, 2 };
static const int SCALE = 10;
static const int TRANS = 20;
static const int XSIZE = (sizeof(xCoords) / sizeof(int32_t));
static const int YSIZE = (sizeof(yCoords) / sizeof(int32_t));
int maxX = 0, maxY = 0;
int x, i, y, dx, dy, endX, endY;
mpe_GfxRectangle source;
DO_SETUP;

ASSERT_SUCCESS(gfxSetColor, (ctx, fg));

CuAssert(tc, "Test is broken - xCoords.length != yCoords.length", XSIZE
== YSIZE);
for (i = 0; i < XSIZE; ++i)
{
xCoords[i] = xCoords[i] * SCALE + TRANS;
yCoords[i] = yCoords[i] * SCALE + TRANS;

maxX = MAX(xCoords[i], maxX);
maxY = MAX(yCoords[i], maxY);
}

CLEAR_BG(ctx, bg);
ASSERT_SUCCESS(gfxDrawPolyline, (ctx, xCoords, yCoords, XSIZE));
FLUSH_SCREEN();
// Assume it was drawn okay...

// BitBlt
dx = TRANS + 200;
dy = TRANS + 200;
source.x = TRANS;
source.y = TRANS;
source.width = maxX - TRANS + SCALE;
source.height = maxY - TRANS + SCALE;

ASSERT_SUCCESS(gfxBitBlt, (ctx, surf, dx, dy, &source));

// Now compare source rect with dest rect
endX = source.x + source.width;
endY = source.y + source.height;
dx = dx - source.x; // get difference in x
dy = dy - source.y; // get different in y
for (x = source.x; x < endX; ++x)
for (y = source.y; y < endY; ++y)
{
CuAssertColorEquals(tc, "Expected same pixels at destination", getPixel(&info,
x, y), getPixel(&info, x + dx, y + dy));
}

DO_CLEANUP;
}

/**
 * Tests the gfxBitBlt() call between surfaces.
 *
 * @param tc pointer to test case structure
 */
void test_gfxBitBlt_diff(CuTest *tc)
{
/* if I am understanding this correctly, I should assert
 ** that if I create a shape on a surface other than the main
 ** surface, then when I bitBlt it to the main surface and flush
 ** the screen, I should see the shape or verify that it is there.
 */
int offscreen = 0;
mpe_Error result = 0;

mpe_GfxSurfaceInfo subSurfInfo; //This is the info of the surface that will
// be created for this test.

mpe_GfxColor fg = mpe_gfxRgbToColor(0x22, 0x70, 0x43);
mpe_GfxColor bg = mpe_gfxRgbToColor(0, 0, 0);
int x = 10, y = 10;
int w = 60, h = 30;
mpe_GfxRectangle rect;
uint32_t dx = 50;
uint32_t dy = 50;
mpe_GfxRectangle testRect;
mpe_GfxSurface newSurf = 0;
mpe_GfxContext myCtx;
mpe_GfxRectangle subSurfRect;
DO_SETUP;

rect.x = x;
rect.y = y;
rect.width = w;
rect.height = h;

testRect.x = 60;
testRect.y = 60;
testRect.width = 60;
testRect.height = 30;

subSurfRect.x = 0;
subSurfRect.y = 0;
subSurfRect.width = 100;
subSurfRect.height = 100;

CLEAR_BG(ctx, bg);

/*
 ** First, populate surface info struct with info of
 ** main surface info struct
 */
memcpy(&subSurfInfo, &info, sizeof(info));
subSurfInfo.dim.height = 100;
subSurfInfo.dim.width = 100;
subSurfInfo.widthbytes = 100;

/*
 ** Create a new surface to draw on.
 */
result = gfxSurfaceNew(&subSurfInfo, &newSurf);
ASSERT( gfxSurfaceNew);

/*
 ** Now get the context to my new surface
 */
result = gfxContextNew(newSurf, &myCtx);
ASSERT( gfxContextNew);

/*
 ** clear the new surfaces bg
 */
gfxSetColor(myCtx, bg);
gfxFillRect(myCtx, &subSurfRect);

/*
 ** New surface rect to be bitblt'ed
 */
gfxSetColor(myCtx, fg);
result = gfxFillRect(myCtx, &rect);
ASSERT( gfxFillRect);

/*
 ** Now BitBlt the new surface to the screen
 */
result = gfxBitBlt(ctx, newSurf, dx, dy, &subSurfRect);
ASSERT( gfxBitBlt);

FLUSH_SCREEN();

/*
 ** Check that everything is ok
 */
checkFillRect(tc, &info, &testRect, bg, fg);

FLUSH_SCREEN();

DO_CLEANUP;

TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
"after new TEST*****************************************\n");

}

/*
 Matrix Transformations

 [ Sx  0  0 ]
 Scale M =   [  0 Sy  0 ]
 [  0  0  1 ]

 [ +-1   0   0 ]
 Reflect M = [   0 +-1   0 ]
 [   0   0   1 ]

 Transformation is performed by:


 [x']   [ m00 m01 m02 ] [x]   [ m00*x + m01*y + m02 ]
 [y'] = [ m10 m11 m12 ]*[y] = [ m10*x + m11*y + m12 ]
 [1 ]   [  0   0   1  ] [1]   [          1          ]

 Because the 0,0 position is the pixel to the lower right of
 the origin, we must additionally translate by -1 in whatever
 direction we are reflecting.  So that the 0,0 pixel will get
 copied to -1,-1 (for a x/y reflection).

 This isn't completely sufficient.  We must perform a translation
 to get things back in the box we want (in addition to the translation
 that we do anyhow); so that it's reflected but still found in the
 destination rectangle.  This requires the addition of the width/
 height for each axis of reflection.

 */

/**
 * Tests the gfxStretchBlt() call.
 *
 * @param tc pointer to test case structure
 */
void dotest_gfxStretchBlt(CuTest *tc, int offscreen)
{
// Declare vars to do our own Stretchblt test for bug 338
// We'll use a long rectangle so we can see unequal dimensions
// in the two directions, for showing off flipping, etc..

mpe_GfxColor fg = mpe_gfxRgbToColor(0, 0xff, 0); // FG is green
mpe_GfxColor bg = mpe_gfxRgbToColor(0, 0, 0); // BG is Black
int32_t xCoords[] =
{ 1, 2, 2 }; // 3 points of square
int32_t yCoords[] =
{ 1, 1, 2 }; // Square 1 pixel wide.
static const int SCALE = 20; // Resize by factor of 20
static const int TRANS = 40; // Translate by 40
static const int XSIZE = (sizeof(xCoords) / sizeof(int32_t)); // array size
//static const int YSIZE = (sizeof(yCoords) / sizeof(int32_t));
int maxX = 0;
int maxY = 0;
int i;
mpe_GfxRectangle srect;
mpe_GfxRectangle drect;
//int endX, endY, dx, dy, x, y, X, Y;
DO_SETUP; // Set ctx, surf offscreen.

ASSERT_SUCCESS(gfxSetColor, (ctx, fg)); // set foreground color

for (i = 0; i < XSIZE; ++i) // Scale and translate coords
{
xCoords[i] = xCoords[i] * SCALE + TRANS;
yCoords[i] = yCoords[i] * SCALE + TRANS; // 300,300 - 400,400
maxX = MAX(xCoords[i], maxX);
maxY = MAX(yCoords[i], maxY);
}

CLEAR_BG(ctx,bg); // Clear background

ASSERT_SUCCESS(gfxDrawPolyline, (ctx, xCoords, yCoords, XSIZE)); // draw triang
//FLUSH_SCREEN();      // Flip to place image on visible screen

// STRAIGHT BITBLIT
//
// First set up source and destination rectangles.
srect.x = TRANS;
srect.y = TRANS;
srect.width = maxX - TRANS + SCALE;
srect.height = maxX - TRANS + SCALE;

drect.x = TRANS + 200;
drect.y = TRANS + 200;
drect.width = srect.width * -3; // should reverse it and elongate it horiz.
drect.height = srect.height;
srect.height = -(srect.height);

// Blit
ASSERT_SUCCESS(gfxStretchBlt, (ctx, surf, &drect, &srect));
FLUSH_SCREEN();

/* Check that everything is ok */
checkFillRect(tc, &info, &drect, bg, fg);

DO_CLEANUP;
}

#if 0 /* TODO: DONM   */
mpe_GfxColor fg = mpe_gfxRgbToColor(0xFF, 0xFF, 0xFF);
mpe_GfxColor bg = mpe_gfxRgbToColor(0, 0, 0);
int32_t xCoords[] =
{1, 2, 2};
int32_t yCoords[] =
{1, 1, 2};
static const int SCALE = 10;
static const int TRANS = 20;
static const int XSIZE = (sizeof(xCoords)/sizeof(int32_t));
static const int YSIZE = (sizeof(yCoords)/sizeof(int32_t));
int maxX = 0, maxY = 0;
int i;
mpe_GfxRectangle srect;
mpe_GfxRectangle drect;
int endX, endY, dx, dy, x, y, X, Y;

DO_SETUP;

ASSERT_SUCCESS(gfxSetColor, (ctx, fg));

// Let's draw an initial figure in a location
CuAssert(tc, "Test is broken - xCoords.length != yCoords.length",
XSIZE == YSIZE);
for(i = 0; i < XSIZE; ++i)
{
xCoords[i] = xCoords[i] * SCALE + TRANS;
yCoords[i] = yCoords[i] * SCALE + TRANS;

maxX = MAX(xCoords[i], maxX);
maxY = MAX(yCoords[i], maxY);
}

CLEAR_BG(ctx, bg); // Clear background
ASSERT_SUCCESS(gfxDrawPolyline, (ctx, xCoords, yCoords, XSIZE));
FLUSH_SCREEN();
// Assume it was drawn okay...

// The following tests should probably be broken out on their own...

/*************** Let's do a straight copy as for BitBlt ******************/
srect.x = TRANS;
srect.y = TRANS;
srect.width = maxX-TRANS+SCALE;
srect.height = maxY-TRANS+SCALE;

drect.x = TRANS+200;
drect.y = TRANS+200;
drect.width = srect.width;
drect.height = srect.height;
{
ASSERT_SUCCESS(gfxStretchBlt, (ctx, surf, &drect, &srect));

// Now compare source rect with dest rect
endX = srect.x + srect.width;
endY = srect.y + srect.height;
dx = drect.x - srect.x; // get difference in x
dy = drect.y - srect.y; // get different in y
for(x = srect.x; x < endX; ++x)
for(y = srect.y; y < endY; ++y)
{
CuAssertColorEquals(tc, "Expected same pixels at destination",
getPixel(&info, x, y),
getPixel(&info, x+dx, y+dy));
}
}
CLEAR_RECT(ctx, bg, &drect);

/************** Flip along both axes ***************/
{
mpe_GfxRectangle srect2;
mpe_GfxRectangle drect2;

ASSERT_SUCCESS(gfxStretchBlt, (ctx, surf, &drect2, &srect2));

srect2.x = TRANS;
srect2.y = TRANS;
srect2.width = maxX-TRANS+SCALE;
srect2.height = maxY-TRANS+SCALE;

drect2.x = TRANS+200+srect2.width;
drect2.y = TRANS+200+srect2.height;
drect2.width = -srect2.width;
drect2.height = -srect2.height;

endX = srect.x + srect.width;
endY = srect.y + srect.height;
dx = drect.x - srect.x; // get difference in x
dy = drect.y - srect.y; // get different in y
for(x = srect.x; x < endX; ++x)
for(y = srect.y; y < endY; ++y)
{
// x-srect.x will undo translation
// -(x-srect.x) - 1 will perform reflection
// + srect.x will redo translation
// + srect.width gets us back to our bounding box
// + dx performs translation to the destination box
X = -(x-srect.x) - 1 + srect.width + srect.x + dx;
Y = -(y-srect.y) - 1 + srect.height + srect.y + dy;

CuAssertColorEquals(tc, "Expected same pixels at destination (flip x/y)",
getPixel(&info, x, y),
getPixel(&info, X, Y));
}

// No compare source rect2 with dest rect2
}
CLEAR_RECT(ctx, bg, &drect);

/************** Flip along y axis ***************/
{
mpe_GfxRectangle srect2;
mpe_GfxRectangle drect2;

ASSERT_SUCCESS(gfxStretchBlt, (ctx, surf, &drect2, &srect2));

srect2.x = TRANS;
srect2.y = TRANS;
srect2.width = maxX-TRANS+SCALE;
srect2.height = maxY-TRANS+SCALE;

drect2.x = TRANS+200;
drect2.y = TRANS+200+srect2.height;
drect2.width = srect2.width;
drect2.height = -srect2.height;

endX = srect.x + srect.width;
endY = srect.y + srect.height;
dx = drect.x - srect.x; // get difference in x
dy = drect.y - srect.y; // get different in y
for(x = srect.x; x < endX; ++x)
for(y = srect.y; y < endY; ++y)
{
X = x + dx;
Y = -(y-srect.y) - 1 + srect.height + srect.y + dy;

CuAssertColorEquals(tc, "Expected same pixels at destination (flip x/y)",
getPixel(&info, x, y),
getPixel(&info, X, Y));
}
}
CLEAR_RECT(ctx, bg, &drect);

/************** Flip along x axis ***************/
{
mpe_GfxRectangle srect2;
mpe_GfxRectangle drect2;

ASSERT_SUCCESS(gfxStretchBlt, (ctx, surf, &drect2, &srect2));

srect2.x = TRANS;
srect2.y = TRANS;
srect2.width = maxX-TRANS+SCALE;
srect2.height = maxY-TRANS+SCALE;

drect2.x = TRANS+200+srect2.width;
drect2.y = TRANS+200;
drect2.width = -srect2.width;
drect2.height = srect2.height;

endX = srect.x + srect.width;
endY = srect.y + srect.height;
dx = drect.x - srect.x; // get difference in x
dy = drect.y - srect.y; // get different in y
for(x = srect.x; x < endX; ++x)
for(y = srect.y; y < endY; ++y)
{
X = -(x-srect.x) - 1 + srect.width + srect.x + dx;
Y = y + dy;

CuAssertColorEquals(tc, "Expected same pixels at destination (flip x/y)",
getPixel(&info, x, y),
getPixel(&info, X, Y));
}
}

CLEAR_RECT(ctx, bg, &drect);

DO_CLEANUP;
}

#endif /* TODO: DONM */

/**
 * Tests the gfxStretchBlt() call.
 * - stretch along x/y axes
 * - shrink along  x/y axes
 *
 * @param tc pointer to test case structure
 */
void test_gfxStretchBlt_other(CuTest *tc)
{
mpe_GfxColor fg = mpe_gfxRgbToColor(0, 0xff, 0); // FG is green
mpe_GfxColor bg = mpe_gfxRgbToColor(0, 0, 0); // BG is Black
mpe_GfxColor drawColor = mpe_gfxRgbToColor(0xff, 0, 0); // drawing color is red
mpe_GfxColor drawColor2 = mpe_gfxRgbToColor(0, 0, 0xff); // drawing color2 is blue
mpe_GfxRectangle rectangle;
mpe_GfxRectangle fill1, fill2;// checkRect;
mpe_GfxRectangle srect;
mpe_GfxRectangle drect;
int offscreen = 0;
mpe_Error result = 0;
int endX = 0, endY = 0, dx, dy;
int x, y; //,i,j;
mpe_GfxColor sourcePixel, sp2;
DO_SETUP; // Set ctx, surf offscreen.

TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "entering gfxStetchBlt_other Test");

ASSERT_SUCCESS(gfxSetColor, (ctx, fg)); // set foreground color

CLEAR_BG(ctx,bg); // Clear background

rectangle.x = 40;
rectangle.y = 40;
rectangle.width = 20;
rectangle.height = 20;
ASSERT_SUCCESS(gfxDrawRect, (ctx, &rectangle));
result = gfxFillRect(ctx, &rectangle);

fill1 = rectangle;
fill1.x = 70;
ASSERT_SUCCESS(gfxSetColor, (ctx, drawColor)); // set fill color
ASSERT_SUCCESS(gfxDrawRect, (ctx, &fill1));
result = gfxFillRect(ctx, &fill1);

fill2 = rectangle;
fill2.y = 70;
ASSERT_SUCCESS(gfxSetColor, (ctx, drawColor2)); // set fill color
ASSERT_SUCCESS(gfxDrawRect, (ctx, &fill2));
result = gfxFillRect(ctx, &fill2);
ASSERT( gfxFillRect);
FLUSH_SCREEN();

// STRAIGHT BITBLIT
//
// First set up source and destination rectangles for source and
// destination, same sizes, same orientation
srect.x = 40;
srect.y = 40;
srect.width = 50;
srect.height = 50;

drect.x = 240;
drect.y = 240;
drect.width = srect.width;
drect.height = srect.height;

// Blit
ASSERT_SUCCESS(gfxStretchBlt, (ctx, surf, &drect, &srect));
FLUSH_SCREEN();

// Now compare source rect with dest rect (this also confirms orientation)
endX = srect.x + srect.width;
endY = srect.y + srect.height;
dx = drect.x - srect.x; // get difference in x
dy = drect.y - srect.y; // get different in y
for (x = srect.x; x < endX; ++x)
{
for (y = srect.y; y < endY; ++y)
{
CuAssertColorEquals(tc, "Expected same pixels at destination", getPixel(&info,
x, y), getPixel(&info, x + dx, y + dy));
}
}

// Set up destination rectangle for destination five times larger
drect.x = 100;
drect.y = 100;
drect.width = 5 * srect.width;
drect.height = 5 * srect.height;

// Blit
ASSERT_SUCCESS(gfxStretchBlt, (ctx, surf, &drect, &srect));
FLUSH_SCREEN();

// Now compare source rect with dest rect (this also confirms orientation)
endX = srect.x + srect.width;
endY = srect.y + srect.height;
dx = drect.x - srect.x; // get difference in x
dy = drect.y - srect.y; // get different in y
for (x = srect.x; x < endX; ++x)
{
for (y = srect.y; y < endY; ++y)
{
//
// Each pixel in the original source rectangle was multiplied by 5,
// so that we have 25 new pixels/per orig pixel...(upsampling)
// We add 2 to the offset into the blt'ed destination rectangle area so
// that we are in the middle of the 5x5 region based on the orig pixel
//
sourcePixel = getPixel(&info, x, y);
sp2 = getPixel(&info, 40 + dx + 5 * (x - 40) + 2, 40 + dy + 5 * (y - 40) + 2);
CuAssertColorEquals(tc, "Expected same pixels at destination glob",
sourcePixel, sp2);
}
}

// Now set up for 5 times smaller...use our current destination rectangle as source
srect = drect;
drect.x = 400;
drect.y = 400;
drect.width = 50;
drect.height = 50;

ASSERT_SUCCESS(gfxStretchBlt, (ctx, surf, &drect, &srect));
FLUSH_SCREEN();

//
// Now compare dest rect with original source rect to see if we have preserved shape and
// orientation with our stretching and shrinking.  NB that the stretching/shrinking messes up
// the edge values of our rectangles, so we'll only check the interiors against each other.
// Here's the comparison of the interiors of the first (green) rectangles:
srect.x = 41;
srect.y = 41;
srect.width = 19;
srect.height = 19;
drect.x = 401;
drect.y = 401;
drect.width = srect.width;
drect.height = srect.height;
endX = srect.x + srect.width;
endY = srect.y + srect.height;
dx = drect.x - srect.x; // get difference in x
dy = drect.y - srect.y; // get different in y
for (x = srect.x; x < endX; ++x)
{
for (y = srect.y; y < endY; ++y)
{
sourcePixel = getPixel(&info, x, y);
sp2 = getPixel(&info, x + dx, y + dy);
if (sourcePixel != sp2)
{
CuAssertColorEquals(tc, "Expected same pixels at 1st destination", sourcePixel,
sp2);
}
}
}

// Here's the comparison of the interiors of the second (blue) rectangles
srect.x = 41;
srect.y = 71;
drect.x = 401;
drect.y = 431;
endX = srect.x + srect.width;
endY = srect.y + srect.height;
dx = drect.x - srect.x; // get difference in x
dy = drect.y - srect.y; // get different in y
for (x = srect.x; x < endX; ++x)
{
for (y = srect.y; y < endY; ++y)
{
sourcePixel = getPixel(&info, x, y);
sp2 = getPixel(&info, x + dx, y + dy);
if (sourcePixel != sp2)
{
CuAssertColorEquals(tc, "Expected same pixels at 2nd destination", sourcePixel,
sp2);
}
}
}

// And here's the third comparison, of the interiors of the red rectangles
srect.x = 71;
srect.y = 41;
drect.x = 431;
drect.y = 401;
endX = srect.x + srect.width;
endY = srect.y + srect.height;
dx = drect.x - srect.x; // get difference in x
dy = drect.y - srect.y; // get different in y
for (x = srect.x; x < endX; ++x)
{
for (y = srect.y; y < endY; ++y)
{
sourcePixel = getPixel(&info, x, y);
sp2 = getPixel(&info, x + dx, y + dy);
if (sourcePixel != sp2)
{
CuAssertColorEquals(tc, "Expected same pixels at 3rd destination", sourcePixel,
sp2);
}
}
}

DO_CLEANUP;

TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "exiting gfxStetchBlt_other Test");
}

/**
 ** Tests the gfxStretchBlt() call between surfaces.
 **
 ** @param tc pointer to test case structure
 */
void test_gfxStretchBlt_diff(CuTest *tc)
{
/*
 * This code is largely copied from test_gfxBitBlt_diff() which
 * creates a rectangle on a new surface, and then blts it on to the main surface.
 * We simply make the rectangle warp to a new shaped rectangle...
 */
int offscreen = 0;
mpe_Error result = 0;

mpe_GfxSurfaceInfo subSurfInfo; //This is the info of the surface that will
// be created for this test.

mpe_GfxColor fg = mpe_gfxRgbToColor(0x22, 0x70, 0x43);
mpe_GfxColor bg = mpe_gfxRgbToColor(0, 0, 0);
int x = 10, y = 10;
int w = 60, h = 30;
mpe_GfxRectangle sRect;
mpe_GfxRectangle dTestRect;
mpe_GfxSurface newSurf = 0;
mpe_GfxContext myCtx;
mpe_GfxRectangle subSurfRect;
DO_SETUP;

sRect.x = x;
sRect.y = y;
sRect.width = w;
sRect.height = h;

dTestRect.x = 60;
dTestRect.y = 60;
dTestRect.width = 60;
dTestRect.height = 60;

subSurfRect.x = 0;
subSurfRect.y = 0;
subSurfRect.width = 100;
subSurfRect.height = 100;

CLEAR_BG(ctx, bg);

/*
 ** First, populate surface info struct with info of
 ** main surface info struct
 */
memcpy(&subSurfInfo, &info, sizeof(info));
subSurfInfo.dim.height = 100;
subSurfInfo.dim.width = 100;
subSurfInfo.widthbytes = 100;

/* Create a new surface on which to draw. */
result = gfxSurfaceNew(&subSurfInfo, &newSurf);
ASSERT( gfxSurfaceNew);

/* Now get the context to my new surface */
result = gfxContextNew(newSurf, &myCtx);
ASSERT( gfxContextNew);

/* Clear the new surface's bg */
gfxSetColor(myCtx, bg);
gfxFillRect(myCtx, &subSurfRect);

/* New source surface rect to be stretch bitblt'ed */
gfxSetColor(myCtx, fg);
result = gfxFillRect(myCtx, &sRect);
ASSERT( gfxFillRect);

/*
 ** Now Stretch BitBlt the new surface's rectangle to the screen,
 ** warping it to the new shape
 */
ASSERT_SUCCESS(gfxStretchBlt, (ctx, newSurf, &dTestRect, &sRect));

FLUSH_SCREEN();

/* Check that everything is ok */
checkFillRect(tc, &info, &dTestRect, bg, fg);

FLUSH_SCREEN();

DO_CLEANUP;
}

/*
 * Some data used for drawString tests.
 */
static char string[] =
{ "Hello, world!" };
#define STRING_LEN ((sizeof(string)-sizeof(char))/sizeof(char))
static mpe_GfxWchar wstring[] =
{ 'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', 0 };
#define WSTRING_LEN ((sizeof(wstring)-sizeof(mpe_GfxWchar))/sizeof(mpe_GfxWchar))

static mpe_GfxWchar fontName[] =
{ 'T', 'i', 'r', 'e', 's', 'i', 'a', 's', 0 };
static int32_t fontSize = 26;
static mpe_GfxFontStyle fontStyle = MPE_GFX_PLAIN;

/**
 * Determines the expected bounds of the given text.
 */
static mpe_GfxRectangle* getTextBounds(CuTest *tc, mpe_GfxContext ctx,
char* buf, int len, int x, int y, mpe_GfxRectangle* rect)
{
mpe_GfxFont font;
mpe_GfxFontMetrics metrics;

ASSERT_SUCCESS(gfxGetFont, (ctx, &font));
ASSERT_SUCCESS(gfxGetFontMetrics, (font, &metrics));

rect->x = x;
rect->y = y - metrics.ascent;
rect->height = metrics.height;

ASSERT_SUCCESS(gfxGetStringWidth, (font, buf, len, &rect->width));

#if 1
// For fun, let's draw the expected bounds
{
mpe_GfxRectangle bounds;
mpe_GfxColor save, color = mpe_gfxRgbToColor(0xFF, 0, 0);
bounds.x = rect->x;
bounds.y = rect->y;
bounds.width = rect->width - 1;
bounds.height = rect->height - 1;
ASSERT_SUCCESS(gfxGetColor, (ctx, &save));
ASSERT_SUCCESS(gfxSetColor, (ctx, color));
ASSERT_SUCCESS(gfxDrawRect, (ctx, &bounds));
FLUSH_SCREEN();
ASSERT_SUCCESS(gfxSetColor, (ctx, save));
}
#endif

return rect;
}

/**
 * Checks that text is rendered ONLY inside the bounds.
 * This is done by checking that *something* was drawn in the foreground color
 * within the bounds, and nothing was drawn without the bounds.
 */
static void checkTextBounds(CuTest *tc, mpe_GfxSurfaceInfo *info,
mpe_GfxRectangle *rect, mpe_GfxColor bg, mpe_GfxColor fg)
{
// Verify that SOMETHING was drawn inside the bounds
{
int fg2 = fg;
int inside = 0;
int endX = rect->x + rect->width;
int endY = rect->y + rect->height;
int x, y;
RGBVALUE(fg2, info->format);

for (x = rect->x; x < endX; ++x)
for (y = rect->y; y < endY; ++y)
{
inside = inside || fg2 == (int) getPixel(info, x, y);
}
CuAssert(tc, "Text should've been drawn inside the expected bounds", inside);
}

// For now, simply verify that outside the bounds, nothing is drawn
// Outside the bounds for 2 pixels
/*
 * !!!! See errors here for "world"!!!! (On windows)
 * Definitely it drawing outside of bounding box.
 * Problem is in width of string not being quite right... unsure why.
 */
{
int outside = 1;
const int EXTRA = 2;
int oldEndX = rect->x + rect->width;
int oldEndY = rect->y + rect->height;
int endX = oldEndX + EXTRA;
int endY = oldEndY + EXTRA;
int x, y;
// Not the most efficient loop, but gets the job done.
for (x = rect->x - EXTRA; x < endX; ++x)
for (y = rect->y - EXTRA; y < endY; ++y)
{
if (x >= rect->x && x < oldEndX && y >= rect->y && y < oldEndY)
continue;

outside = outside && (int) bg == (int) getPixel(info, x, y);
}
//USER_OK("?", "?");
CuAssert(tc, "No text should be drawn outside the expected bounds", outside);
}
}

/**
 * Set the font on the context.
 * This is necessary in case the default font is not set correctly.
 * Returns the current font.
 */
static void setupFont(CuTest *tc, mpe_GfxContext ctx, mpe_GfxFont *save)
{
mpe_GfxFont font;
ASSERT_SUCCESS(gfxGetFont, (ctx, save));
ASSERT_SUCCESS(gfxFontNew,
(0, fontName, sizeof(fontName), fontStyle, fontSize, &font));
ASSERT_SUCCESS(gfxSetFont, (ctx, font));
}

/**
 * Cleans up the font allocated in setupFont.
 */
static void cleanupFont(mpe_GfxContext ctx, mpe_GfxFont save)
{
mpe_GfxFont font = 0;

gfxGetFont(ctx, &font);
gfxSetFont(ctx, save);
if (font != 0)
gfxFontDelete(font);
}

/**
 * Tests the gfxDrawString() call.
 *
 * @param tc pointer to test case structure
 */
void dotest_gfxDrawString(CuTest *tc, int offscreen)
{
mpe_GfxContext context;
mpe_GfxSurface surface;
mpe_GfxSurfaceInfo surfaceInfo;
//   mpe_GfxColor      backColor = mpe_gfxRgbToColor(191, 191, 191);
mpe_GfxColor backColor = mpe_gfxArgbToColor(255, 0, 230, 0);
mpe_GfxColor textColor = mpe_gfxRgbToColor(230, 230, 230);
//   mpe_GfxColor      textColor = mpe_gfxArgbToColor(0, 230, 230, 230);
mpe_Error errorCode;
mpe_GfxFont font;
mpe_GfxFont save;
mpe_GfxRectangle rect;

#if 0
static mpe_GfxWchar fontName[] =
{'S', 'a', 'n', 'S', 'e', 'r', 'i', 'f', '\0'};
#else
static mpe_GfxWchar fontName[] =
{ 'T', 'i', 'r', 'e', 's', 'i', 'a', 's', '\0' };
#endif
static const int fontNameSize = (sizeof(fontName) / sizeof(fontName[0])) - 1;

static char upperCase[] =
{ 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O',
'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '\0' };
static char lowerCase[] =
{ 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '\0' };
static char numerical[] =
{ '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '\0' };

//   char message[256];
//
//   sprintf(message, "color = (%02x, %02x, %02x, %02x)\n", mpe_gfxGetAlpha(color), mpe_gfxGetRed(color), mpe_gfxGetGreen(color), mpe_gfxGetBlue(color));
//   TRACE(MPE_LOG_INFO, MPE_MOD_TEST, message);
//

CuTestSetup(errorCode = setup(&surface, &context, &surfaceInfo, offscreen));
CuAssert(tc, "setup(&surface, &context, &surfaceInfo, offscreen)", errorCode
== MPE_SUCCESS);

// get surface size
rect.x = 0;
rect.y = 0;
rect.width = surfaceInfo.dim.width;
rect.height = surfaceInfo.dim.height;

// fill surface with backColor
CuAssert(tc, "gfxSetPaintMode(context, MPE_GFX_SRC, 255)", gfxSetPaintMode(
context, MPE_GFX_SRC, 255) == MPE_SUCCESS);
CuAssert(tc, "gfxSetColor(context, backColor)", gfxSetColor(context, backColor)
== MPE_SUCCESS);
CuAssert(tc, "gfxFillRect(context, &rect)", gfxFillRect(context, &rect)
== MPE_SUCCESS);
CuAssert(tc, "dispFlushGfxSurface(getDefaultGfx())", dispFlushGfxSurface(
getDefaultGfx()) == MPE_SUCCESS);

// create font
CuAssert(tc, "gfxFontNew(0, fontName, fontNameSize, MPE_GFX_BOLD, 24, &font)",
gfxFontNew(0, fontName, fontNameSize, MPE_GFX_BOLD, 24, &font) == MPE_SUCCESS);

// save old font, set new font
CuAssert(tc, "gfxGetFont(context, &save)", gfxGetFont(context, &save)
== MPE_SUCCESS);
CuAssert(tc, "gfxSetFont(context, font)", gfxSetFont(context, font)
== MPE_SUCCESS);

// alpha const = 255

// draw text, SRC_OVER mode
CuAssert(tc, "gfxSetPaintMode(context, MPE_GFX_SRCOVER, 255)", gfxSetPaintMode(
context, MPE_GFX_SRCOVER, 255) == MPE_SUCCESS);
CuAssert(tc, "gfxSetColor(context, textColor)", gfxSetColor(context, textColor)
== MPE_SUCCESS);
CuAssert(tc, "gfxDrawString(context, 100, 100, \"KRMA (SrcOver)\", 14)",
gfxDrawString(context, 100, 100, "KRMA (SrcOver)", 14) == MPE_SUCCESS);
CuAssert(tc, "dispFlushGfxSurface(getDefaultGfx())", dispFlushGfxSurface(
getDefaultGfx()) == MPE_SUCCESS);

// draw text, SRC mode
CuAssert(tc, "gfxSetPaintMode(context, MPE_GFX_SRC, 255)", gfxSetPaintMode(
context, MPE_GFX_SRC, 255) == MPE_SUCCESS);
CuAssert(tc, "gfxSetColor(context, textColor)", gfxSetColor(context, textColor)
== MPE_SUCCESS);
CuAssert(tc, "gfxDrawString(context, 100, 140, \"KRMA (Src)\", 10)",
gfxDrawString(context, 100, 140, "KRMA (Src)", 10) == MPE_SUCCESS);
CuAssert(tc, "dispFlushGfxSurface(getDefaultGfx())", dispFlushGfxSurface(
getDefaultGfx()) == MPE_SUCCESS);

// draw text, CLR mode
CuAssert(tc, "gfxSetPaintMode(context, MPE_GFX_CLR, 255)", gfxSetPaintMode(
context, MPE_GFX_CLR, 255) == MPE_SUCCESS);
CuAssert(tc, "gfxSetColor(context, textColor)", gfxSetColor(context, textColor)
== MPE_SUCCESS);
CuAssert(tc, "gfxDrawString(context, 100, 180, \"KRMA (Clr)\", 10)",
gfxDrawString(context, 100, 180, "KRMA (Clr)", 10) == MPE_SUCCESS);
CuAssert(tc, "dispFlushGfxSurface(getDefaultGfx())", dispFlushGfxSurface(
getDefaultGfx()) == MPE_SUCCESS);

// draw text, XOR mode
CuAssert(tc, "gfxSetPaintMode(context, MPE_GFX_XOR, 255)", gfxSetPaintMode(
context, MPE_GFX_XOR, 255) == MPE_SUCCESS);
CuAssert(tc, "gfxSetColor(context, textColor)", gfxSetColor(context, textColor)
== MPE_SUCCESS);
CuAssert(tc, "gfxDrawString(context, 100, 220, \"KRMA (Xor)\", 10)",
gfxDrawString(context, 100, 220, "KRMA (Xor)", 10) == MPE_SUCCESS);
CuAssert(tc, "dispFlushGfxSurface(getDefaultGfx())", dispFlushGfxSurface(
getDefaultGfx()) == MPE_SUCCESS);

// draw text, XOR mode (twice)
CuAssert(tc, "gfxSetPaintMode(context, MPE_GFX_XOR, 255)", gfxSetPaintMode(
context, MPE_GFX_XOR, 255) == MPE_SUCCESS);
CuAssert(tc, "gfxSetColor(context, textColor)", gfxSetColor(context, textColor)
== MPE_SUCCESS);
CuAssert(tc, "gfxDrawString(context, 100, 260, \"KRMA (Xor, twice)\", 17)",
gfxDrawString(context, 100, 260, "KRMA (Xor, twice)", 17) == MPE_SUCCESS);
CuAssert(tc, "dispFlushGfxSurface(getDefaultGfx())", dispFlushGfxSurface(
getDefaultGfx()) == MPE_SUCCESS);
CuAssert(tc, "gfxDrawString(context, 100, 260, \"KRMA (Xor, twice)\", 17)",
gfxDrawString(context, 100, 260, "KRMA (Xor, twice)", 17) == MPE_SUCCESS);
CuAssert(tc, "dispFlushGfxSurface(getDefaultGfx())", dispFlushGfxSurface(
getDefaultGfx()) == MPE_SUCCESS);

// alpha const = 127

// draw text, SRC_OVER mode
CuAssert(tc, "gfxSetPaintMode(context, MPE_GFX_SRCOVER, 127)", gfxSetPaintMode(
context, MPE_GFX_SRCOVER, 127) == MPE_SUCCESS);
CuAssert(tc, "gfxSetColor(context, textColor)", gfxSetColor(context, textColor)
== MPE_SUCCESS);
CuAssert(tc, "gfxDrawString(context, 200, 100, \"KRMA (SrcOver)\", 14)",
gfxDrawString(context, 400, 100, "KRMA (SrcOver)", 14) == MPE_SUCCESS);
CuAssert(tc, "dispFlushGfxSurface(getDefaultGfx())", dispFlushGfxSurface(
getDefaultGfx()) == MPE_SUCCESS);

// draw text, SRC mode
CuAssert(tc, "gfxSetPaintMode(context, MPE_GFX_SRC, 127)", gfxSetPaintMode(
context, MPE_GFX_SRC, 127) == MPE_SUCCESS);
CuAssert(tc, "gfxSetColor(context, textColor)", gfxSetColor(context, textColor)
== MPE_SUCCESS);
CuAssert(tc, "gfxDrawString(context, 200, 140, \"KRMA (Src)\", 10)",
gfxDrawString(context, 400, 140, "KRMA (Src)", 10) == MPE_SUCCESS);
CuAssert(tc, "dispFlushGfxSurface(getDefaultGfx())", dispFlushGfxSurface(
getDefaultGfx()) == MPE_SUCCESS);

// draw text, CLR mode
CuAssert(tc, "gfxSetPaintMode(context, MPE_GFX_CLR, 127)", gfxSetPaintMode(
context, MPE_GFX_CLR, 127) == MPE_SUCCESS);
CuAssert(tc, "gfxSetColor(context, textColor)", gfxSetColor(context, textColor)
== MPE_SUCCESS);
CuAssert(tc, "gfxDrawString(context, 200, 180, \"KRMA (Clr)\", 10)",
gfxDrawString(context, 400, 180, "KRMA (Clr)", 10) == MPE_SUCCESS);
CuAssert(tc, "dispFlushGfxSurface(getDefaultGfx())", dispFlushGfxSurface(
getDefaultGfx()) == MPE_SUCCESS);

// draw text, XOR mode
CuAssert(tc, "gfxSetPaintMode(context, MPE_GFX_XOR, 127)", gfxSetPaintMode(
context, MPE_GFX_XOR, 127) == MPE_SUCCESS);
CuAssert(tc, "gfxSetColor(context, textColor)", gfxSetColor(context, textColor)
== MPE_SUCCESS);
CuAssert(tc, "gfxDrawString(context, 200, 220, \"KRMA (Xor)\", 10)",
gfxDrawString(context, 400, 220, "KRMA (Xor)", 10) == MPE_SUCCESS);
CuAssert(tc, "dispFlushGfxSurface(getDefaultGfx())", dispFlushGfxSurface(
getDefaultGfx()) == MPE_SUCCESS);

// draw text, XOR mode (twice)
CuAssert(tc, "gfxSetPaintMode(context, MPE_GFX_XOR, 127)", gfxSetPaintMode(
context, MPE_GFX_XOR, 127) == MPE_SUCCESS);
CuAssert(tc, "gfxSetColor(context, textColor)", gfxSetColor(context, textColor)
== MPE_SUCCESS);
CuAssert(tc, "gfxDrawString(context, 200, 260, \"KRMA (Xor, twice)\", 17)",
gfxDrawString(context, 400, 260, "KRMA (Xor, twice)", 17) == MPE_SUCCESS);
CuAssert(tc, "dispFlushGfxSurface(getDefaultGfx())", dispFlushGfxSurface(
getDefaultGfx()) == MPE_SUCCESS);
CuAssert(tc, "gfxDrawString(context, 200, 260, \"KRMA (Xor, twice)\", 17)",
gfxDrawString(context, 400, 260, "KRMA (Xor, twice)", 17) == MPE_SUCCESS);
CuAssert(tc, "dispFlushGfxSurface(getDefaultGfx())", dispFlushGfxSurface(
getDefaultGfx()) == MPE_SUCCESS);

// alpha const = 255

// draw text, SRC mode
CuAssert(tc, "gfxSetPaintMode(context, MPE_GFX_SRC, 255)", gfxSetPaintMode(
context, MPE_GFX_SRC, 255) == MPE_SUCCESS);
CuAssert(tc, "gfxSetColor(context, textColor)", gfxSetColor(context, textColor)
== MPE_SUCCESS);
CuAssert(tc, "gfxDrawString(context, 100, 300, upperCase, 26)", gfxDrawString(
context, 100, 300, upperCase, 26) == MPE_SUCCESS);
CuAssert(tc, "gfxDrawString(context, 100, 340, lowerCase, 26)", gfxDrawString(
context, 100, 340, lowerCase, 26) == MPE_SUCCESS);
CuAssert(tc, "gfxDrawString(context, 100, 380, numerical, 10)", gfxDrawString(
context, 100, 380, numerical, 10) == MPE_SUCCESS);
CuAssert(tc, "dispFlushGfxSurface(getDefaultGfx())", dispFlushGfxSurface(
getDefaultGfx()) == MPE_SUCCESS);

// restore old font, delete new font
CuAssert(tc, "gfxSetFont(context, save)", gfxSetFont(context, save)
== MPE_SUCCESS);
CuAssert(tc, "gfxFontDelete(font)", gfxFontDelete(font) == MPE_SUCCESS);

CuTestCleanup(teardown(surface, context, offscreen));
}

/**
 * Tests the gfxDrawString16() call.
 *
 * @param tc pointer to test case structure
 */
void dotest_gfxDrawString16(CuTest *tc, int offscreen)
{
// Set the font because context may be broken -- and default font not set
mpe_GfxFont saveFont;
mpe_GfxRectangle rect =
{ 0 };
mpe_GfxColor fg = mpe_gfxRgbToColor(0x80, 0x80, 0x80);
mpe_GfxColor bg = mpe_gfxRgbToColor(0, 0, 0);
int x, y;
int ok;
DO_SETUP;

TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered gfxDrawString16");

CuTestSetup(setupFont(tc, ctx, &saveFont));

ASSERT_SUCCESS(gfxSetColor, (ctx, fg));

CLEAR_BG(ctx, bg);
x = 100;
y = 100;
ASSERT_SUCCESS(gfxDrawString16, (ctx, x, y, wstring, WSTRING_LEN));
FLUSH_SCREEN();
checkTextBounds(tc, &info, getTextBounds(tc, ctx, string, STRING_LEN, x, y,
&rect), bg, fg);
if (!offscreen)
{
ok = 0;
#if 0
//USER_YESNO("Was 'Hello, world!' rendered on screen", "text?", ok);
CuAssert(tc, "Text should've been rendered on screen", ok);
#endif
}

CLEAR_BG(ctx, bg);
x = 100;
y = 200;
ASSERT_SUCCESS(gfxDrawString16, (ctx, x, y, wstring + 7, 5));
FLUSH_SCREEN();
checkTextBounds(tc, &info, getTextBounds(tc, ctx, string + 7, 5, x, y, &rect),
bg, fg);
if (!offscreen)
{
ok = 0;
#if 0
//USER_YESNO("Was 'world' rendered on screen", "text?", ok);
CuAssert(tc, "Text should've been rendered on screen", ok);
#endif
}

CuTestCleanup(cleanupFont(ctx, saveFont));

DO_CLEANUP;
TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "Exiting gfxDrawString16");
}

/**
 * Does a LOT of operations.
 * Should probably do one of each.
 * Currently missing arcs and rounded rectangles.
 */
static void bunchOfDrawing(CuTest *tc, mpe_GfxContext ctx,
mpe_GfxRectangle *rect)
{
mpe_GfxRectangle r;
mpe_GfxColor save;
ASSERT_SUCCESS(gfxGetColor, (ctx, &save));

ASSERT_SUCCESS(gfxSetColor, (ctx, mpe_gfxRgbToColor(0, 0xff, 0)));
ASSERT_SUCCESS(gfxFillRect, (ctx, rect));

ASSERT_SUCCESS(gfxSetColor, (ctx, mpe_gfxRgbToColor(0xff, 0, 0)));
r.width = 60;
r.height = rect->height;
r.y = rect->y;
r.x = rect->x + (rect->width + r.width) / 2;
ASSERT_SUCCESS(gfxFillEllipse, (ctx, &r));

#if 0

// Fill bg
// Ellipses
ASSERT_SUCCESS(gfxSetColor, (ctx, mpe_gfxRgbToColor(0, 0xff, 0)));
r.width = 30;
r.height = rect->height;
r.y = rect->y;
r.x = rect->x+(rect->width+r.width)/2;
ASSERT_SUCCESS(gfxFillEllipse, (ctx, &r));
FLUSH_SCREEN();

// Rectangles
ASSERT_SUCCESS(gfxSetColor, (ctx, mpe_gfxRgbToColor(0xff, 0, 0xff)));
r.width = 100;
r.height = 15;
r.x = rect->x+40;
r.y = rect->y+120;
ASSERT_SUCCESS(gfxFillRect, (ctx, &r));
FLUSH_SCREEN();
// Polygon

{
ASSERT_SUCCESS(gfxSetColor, (ctx, mpe_gfxRgbToColor(0xff, 0xff, 0)));
int32_t xCoords[] =
{rect->x+120,
rect->x+150,
rect->x+0, rect->x+rect->width-1,
rect->x+0};
int32_t yCoords[] =
{rect->y+90, rect->y+150,
rect->y+100, rect->y+100,
rect->y+150};
ASSERT_SUCCESS(gfxFillPolygon, (ctx, xCoords, yCoords, (sizeof(xCoords)/sizeof(int))));
FLUSH_SCREEN();
}

// Ellipse
ASSERT_SUCCESS(gfxSetColor, (ctx, mpe_gfxRgbToColor(0, 0, 0xff)));
r.height = 20;
r.width = rect->width;
r.x = rect->x;
r.y = rect->y+(rect->height+r.height)/2;
ASSERT_SUCCESS(gfxDrawEllipse, (ctx, &r));
FLUSH_SCREEN();

// Rect
ASSERT_SUCCESS(gfxSetColor, (ctx, mpe_gfxRgbToColor(0, 0xff, 0xff)));
r.width = rect->width/3;
r.height = rect->height/4;
r.x = rect->x+70;
r.y = rect->y+90;
ASSERT_SUCCESS(gfxDrawRect, (ctx, &r));
FLUSH_SCREEN();

// Polygon

{
ASSERT_SUCCESS(gfxSetColor, (ctx, mpe_gfxRgbToColor(0x44, 0x44, 0x0)));
int32_t xCoords[] =
{rect->x+rect->width/2, rect->x+rect->width-1,
rect->x+rect->width/2, rect->x};
int32_t yCoords[] =
{rect->y, rect->y+rect->height/2,
rect->y+rect->height-1, rect->y+rect->height/2};
ASSERT_SUCCESS(gfxDrawPolygon, (ctx, xCoords, yCoords, (sizeof(xCoords)/sizeof(int32_t))));
FLUSH_SCREEN();
}
// Polyline

{
ASSERT_SUCCESS(gfxSetColor, (ctx, mpe_gfxRgbToColor(0x44, 0, 0x44)));
int32_t xCoords[30];
int32_t yCoords[30];
int y = rect->y, yAdv = rect->height/30;
for(int i = 0; i < 30; ++i, y += yAdv)
{
xCoords[i] = (i % 2) ? rect->x : (rect->x + rect->width-1);
yCoords[i] = y;
}
ASSERT_SUCCESS(gfxDrawPolygon, (ctx, xCoords, yCoords, 30));
FLUSH_SCREEN();
}
// Some lines
ASSERT_SUCCESS(gfxSetColor, (ctx, mpe_gfxRgbToColor(0xff, 0, 0)));
ASSERT_SUCCESS(gfxDrawLine, (ctx, rect->x, rect->y,
rect->x+rect->width-1, rect->y+rect->height-1));
FLUSH_SCREEN();
ASSERT_SUCCESS(gfxDrawLine, (ctx, rect->x+rect->width-1, rect->y,
rect->x, rect->y+rect->height-1));
FLUSH_SCREEN();
ASSERT_SUCCESS(gfxDrawLine, (ctx, rect->x+rect->width/2, rect->y,
rect->x+rect->width/2, rect->y+rect->height-1));
FLUSH_SCREEN();
ASSERT_SUCCESS(gfxDrawLine, (ctx, rect->x, rect->y+rect->height/2,
rect->x+rect->width-1, rect->y+rect->height/2));
FLUSH_SCREEN();

//USER_OK("bunch of drawing?", "Ok");
#endif

ASSERT_SUCCESS(gfxSetColor, (ctx, save));
}

/**
 * Tests clipped drawing.
 */
void dotest_gfxDraw_Clipped(CuTest* tc, int offscreen)
{
mpe_GfxColor fg = mpe_gfxRgbToColor(0xFF, 0, 0);
mpe_GfxColor bg = mpe_gfxRgbToColor(0, 0, 0);
mpe_GfxRectangle rect =
{ 0, 0, 320, 240 };
mpe_GfxRectangle clip =
{ 100, 100, 100, 100 };
mpe_GfxRectangle rect2;
mpe_GfxRectangle clip2;
int endX, endY, x, y;
DO_SETUP;

ASSERT_SUCCESS(gfxSetColor, (ctx, fg));

CLEAR_BG(ctx, bg);

ASSERT_SUCCESS(gfxSetClipRect, (ctx, &clip));
bunchOfDrawing(tc, ctx, &rect);

// Clear beyond bounds of clipping
{
mpe_GfxRectangle clr;

#if 0
this is commented out because it removes the shape with which we want to compare
clr.width = rect.width;
clr.height = rect.height;
CLEAR_RECT(ctx, bg, &clr);
#endif

clr.x = rect.x;
clr.y = rect.y;
clr.width = clip.x;
clr.height = rect.height;
CLEAR_RECT(ctx, bg, &clr);

clr.x = clip.x + clip.width;
clr.y = rect.y;
clr.width = rect.width - clr.x;
clr.height = rect.height;
CLEAR_RECT(ctx, bg, &clr);

clr.x = rect.x;
clr.y = clip.y + clip.height;
clr.width = rect.width;
clr.height = rect.height - clr.y;
CLEAR_RECT(ctx, bg, &clr);
//USER_OK("Erased outer areas?", "");
//USER_OK("Erased outer areas?", "");
}
rect2 = rect;
clip2 = clip;

rect2.x += rect.width;
rect2.y += rect.height;
clip2.x += rect.width;
clip2.y += rect.height;
ASSERT_SUCCESS(gfxSetClipRect, (ctx, &clip2));
bunchOfDrawing(tc, ctx, &rect2);
//USER_OK("Clipped drawing?", "");

endX = rect.x + rect.width;
endY = rect.y + rect.height;
for (x = rect.x; x < endX; ++x)
{
for (y = rect.y; y < endY; ++y)
{
CuAssertColorEquals(tc, "Clipped drawing not as expected",
getPixel(&info, x, y), getPixel(&info, x + rect.width, y + rect.height));
}
}

DO_CLEANUP;
}

/**
 * Tests translated drawing.
 */
void dotest_gfxDraw_Translated(CuTest* tc, int offscreen)
{
mpe_GfxColor fg = mpe_gfxRgbToColor(0xFF, 0, 0);
mpe_GfxColor bg = mpe_gfxRgbToColor(0, 0, 0);

mpe_GfxRectangle rect;
mpe_GfxPoint trans;

int endX, endY, x, y;
DO_SETUP;

rect.x = 0;
rect.y = 0;
rect.width = 320;
rect.height = 240;

trans.x = rect.width;
trans.y = rect.height;

ASSERT_SUCCESS(gfxSetColor, (ctx, fg));

CLEAR_BG(ctx, bg);

bunchOfDrawing(tc, ctx, &rect);
ASSERT_SUCCESS(gfxSetOrigin, (ctx, trans.x, trans.y));
bunchOfDrawing(tc, ctx, &rect);

endX = rect.x + rect.width;
endY = rect.y + rect.height;
for (x = rect.x; x < endX; ++x)
for (y = rect.y; y < endY; ++y)
CuAssertColorEquals(tc, "Translated drawing not as expected", getPixel(&info,
x, y), getPixel(&info, x + trans.x, y + trans.y));

DO_CLEANUP;
}

/**
 * Tests that each context has a distinct state.
 */
void dotest_gfxDraw_DistinctState(CuTest* tc, int offscreen)
{
mpe_GfxColor bg = mpe_gfxRgbToColor(0, 0, 0);
mpe_GfxContext ctx2;
mpe_GfxColor color = mpe_gfxArgbToColor('A', 'r', 'G', 'B');
mpe_GfxPoint origin;
mpe_GfxRectangle clip;
mpe_GfxPaintMode mode = MPE_GFX_XOR;
uint32_t modeData = mpe_gfxRgbToColor(0xff, 0xff, 0xff);
mpe_GfxFont font;
mpe_GfxWchar name[] =
{ 'T', 'i', 'r', 'e', 's', 'i', 'a', 's', '\0' };
int x = 100, y = 100;
mpe_GfxColor color2;
mpe_GfxPoint origin2;
mpe_GfxRectangle clip2;
mpe_GfxPaintMode mode2;
uint32_t modeData2;
mpe_GfxFont font2;
uint32_t p1;
uint32_t p2;
int endX = 0, endY = 0;
DO_SETUP;

origin.x = 10;
origin.y = 10;

clip.x = 0;
clip.y = 0;
clip.width = 640;
clip.height = 480;

CLEAR_BG(ctx, bg);

/* Modify ctx's attributes */

CuTestSetup(ec = gfxFontNew((mpe_GfxFontFactory) 0, name, sizeof(name),
MPE_GFX_PLAIN, 36, &font));
ASSERT_SUCCESS(gfxSetColor, (ctx, color));
ASSERT_SUCCESS(gfxSetOrigin, (ctx, origin.x, origin.y));
ASSERT_SUCCESS(gfxSetClipRect, (ctx, &clip));
ASSERT_SUCCESS(gfxSetPaintMode, (ctx, mode, modeData));
ASSERT_SUCCESS(gfxSetFont, (ctx, font));

// Do initial drawing
ASSERT_SUCCESS(gfxDrawString,
(ctx, x, y, "The quick brown fox... ", sizeof("The quick brown fox... ") - 1));
FLUSH_SCREEN();

// Create second context
CuTestSetup(ec = gfxContextCreate(ctx, &ctx2));
CuAssert(tc, "gfxContextCreate failed", ec == MPE_SUCCESS);

/* Modify ctx2's attributes */
color2 = mpe_gfxArgbToColor('a', 'R', 'g', 'b');
origin2.x = 0;
origin2.y = 0;
clip2.x = 0;
clip2.y = 0;
clip2.width = 1;
clip2.height = 1;
mode2 = MPE_GFX_SRCOVER;
modeData2 = 0xff;

CuTestSetup(ec = gfxFontNew((mpe_GfxFontFactory) 0, name, sizeof(name),
MPE_GFX_BOLD, 26, &font2));
ASSERT_SUCCESS(gfxSetColor, (ctx2, color2));
ASSERT_SUCCESS(gfxSetOrigin, (ctx2, origin2.x, origin2.y));
ASSERT_SUCCESS(gfxSetClipRect, (ctx2, &clip2));
ASSERT_SUCCESS(gfxSetPaintMode, (ctx2, mode2, modeData2));
ASSERT_SUCCESS(gfxSetFont, (ctx2, font2));

// Do 2ndary drawing
ASSERT_SUCCESS(
gfxDrawString,
(ctx, x + 320, y + 240, "The quick brown fox... ", sizeof("The quick brown fox... ")
- 1));
FLUSH_SCREEN();

// Compare
for (x = 0; x < endX; ++x)
for (y = 0; y < endY; ++y)
CuAssertColorEquals(tc, "Translated drawing not as expected", getPixel(&info,
x, y), getPixel(&info, x + endX, y + endY));

{
p1 = getPixel(&info, x, y);
p2 = getPixel(&info, x + endX, y + endY);

RGBVALUE(p1,info.format);
RGBVALUE(p2,info.format);
CuAssertColorEquals(tc, "Translated drawing not as expected", p1, p2);
}
CuTestCleanup(gfxFontDelete(font2));
CuTestCleanup(gfxContextDelete(ctx2));
CuTestCleanup(gfxFontDelete(font));

DO_CLEANUP;
}

/**
 * Tests that creating a surface from another surface creates a
 * distinct surface!  That they don't draw onto each other!
 */
void test_gfxDraw_DistinctSurface(CuTest *tc)
{
int offscreen = 0; // initial surface is screen

mpe_GfxColor fg = mpe_gfxRgbToColor(0xFF, 0, 0);
mpe_GfxColor bg = mpe_gfxRgbToColor(0, 0, 0);
mpe_GfxSurface surf2 = (mpe_GfxSurface) 0;
mpe_GfxContext ctx2 = (mpe_GfxContext) 0;
mpe_GfxSurfaceInfo info2;

mpe_GfxRectangle rect;

int endX;
int endY;
int x, y;

//DO_SETUP;

mpe_GfxContext ctx = (mpe_GfxContext) 0;
mpe_GfxSurface surf = (mpe_GfxSurface) 0;
mpe_GfxSurfaceInfo info;
mpe_Error ec;
//   write(1, "\n++++++++entered test\n", 22);
CuTestSetup(ec = setup(&surf, &ctx, &info, offscreen));
CuAssert(tc, "Test setup failed", ec == MPE_SUCCESS);

rect.x = 30;
rect.y = 30;
rect.width = 10;
rect.height = 10;

endX = rect.x + rect.width;
endY = rect.y + rect.height;

ASSERT_SUCCESS(gfxSetColor, (ctx, fg));
CLEAR_BG(ctx, bg);

//   write(1, "\n+++++++Cleared background\n", 27);

// Create 2nd surface
CuTestSetup(ec = setup(&surf2, &ctx2, &info2, 1));
//   write(1, "\n++++++++Created 2nd Surface\n", 29);
CuAssert(tc, "Creation of 2nd compatible offscreen surface failed", ec
== MPE_SUCCESS);

// Draw to 1st surface
ASSERT_SUCCESS(gfxFillRect, (ctx, &rect));
//   write(1, "\n+++++++++Filled first rect\n", 28);
FLUSH_SCREEN();
//   write(1, "\n++++++++Flush screen\n", 22);

// Make sure 2nd surface wasn't drawn to
for (x = rect.x; x < endX; ++x)
for (y = rect.y; y < endY; ++y)
CuAssertColorEquals(tc, "Did not expect pixel to be set on other surface", bg,
getPixel(&info2, x, y));
// Now draw to 2nd surface
CLEAR_BG(ctx, bg);
ASSERT_SUCCESS(gfxFillRect, (ctx2, &rect));
FLUSH_SCREEN();

// Make sure 1st surface wasn't drawn to
for (x = rect.x; x < endX; ++x)
for (y = rect.y; y < endY; ++y)
CuAssertColorEquals(tc, "Did not expect pixel to be set on other surface", bg,
getPixel(&info, x, y));

CuTestCleanup(teardown(surf2, ctx2, 1));
DO_CLEANUP;
}

/**
 * Like test_gfxContext_DistinctState, but verifies the "internal" state of
 * context by verifying that changes made to the cloned context doesn't affect
 * drawing done by the original context.
 * This is located here to take advantage of pixel reading routines.
 */
void test_gfxDraw_DistinctContext(CuTest *tc)
{
int offscreen = 0; // initial surface is screen

mpe_GfxColor fg = mpe_gfxRgbToColor(0x80, 0x80, 0x80);
mpe_GfxColor bg = mpe_gfxRgbToColor(0, 0, 0);
mpe_GfxColor other = mpe_gfxRgbToColor(0x40, 0x40, 0x40);
mpe_GfxContext ctx2;
DO_SETUP;
ASSERT_SUCCESS(gfxSetColor, (ctx, fg));
CLEAR_BG(ctx, bg);

// Create 2nd context
CuTestSetup(ec = gfxContextCreate(ctx, &ctx2));
ASSERT_SUCCESS(gfxSetColor, (ctx2, other));

// Do drawing
ASSERT_SUCCESS(gfxDrawLine, (ctx, 0, 0, 1, 1));
FLUSH_SCREEN();

// Compare
RGBVALUE(fg, info.format);
CuAssertColorEquals(tc, "Modifying cloned state affect original", fg, getPixel(
&info, 0, 0));

CuTestCleanup(gfxContextDelete(ctx2));

DO_CLEANUP;
}

/*
 ** Draw three shapes with one overlapping
 ** the other two and verify that the overlapping
 ** shapes overlapping sections color's are XORed
 ** with the color that they overlapp
 */
void test_gfxDraw_XOR(CuTest* tc)
{
#if 0
/* this test doesn't work after the cpp to c conversion.
 ** and I don't currently have time to fix it.
 */
mpe_GfxColor reddish = mpe_gfxRgbToColor(0xE5, 0x4C, 0x4C);
mpe_GfxColor yellowish = mpe_gfxRgbToColor(0xFF, 0xF2, 0x70);
mpe_GfxColor greenish = mpe_gfxRgbToColor(0x8C, 0xBD, 0x47);
mpe_GfxColor bluish = mpe_gfxRgbToColor(0x4B, 0x4B, 0xCC);

int offscreen = 0; // initial surface is screen
mpe_Error result = 0;
mpe_GfxColor bg = mpe_gfxRgbToColor(0, 0, 0);

/*
 ** Create three rectangles, the last overlapping
 ** the previously drawn two.
 */
mpe_GfxRectangle rectHor1 =
{100, 100, 120, 60};
mpe_GfxRectangle rectHor2 =
{240, 100, 120, 60};
mpe_GfxRectangle rectVert1 =
{200, 40, 60, 180};
mpe_GfxPaintMode mode = MPE_GFX_XOR;
uint32_t modeData = (uint32_t)yellowish;
mpe_GfxColor a = yellowish;
mpe_GfxColor d = bluish;
mpe_GfxColor b = getPixel(&info, 210, 130);
mpe_GfxColor c = getPixel(&info, 250, 130);
RGBVALUE( a, MPE_GFX_RGB565 );
RGBVALUE( d, MPE_GFX_RGB565 );
mpe_GfxColor e = yellowish ^ bluish;
mpe_GfxColor f = yellowish ^ reddish;

DO_SETUP;

CLEAR_BG(ctx, bg);

/*
 ** New surface rects to be bitblt'ed
 */
gfxSetColor( ctx, bluish );
result = gfxFillRect(ctx, &rectHor1);
ASSERT(gfxFillRect);

gfxSetColor( ctx, reddish );
result = gfxFillRect(ctx, &rectHor2);
ASSERT(gfxFillRect);

gfxSetPaintMode( ctx, mode, modeData );
result = gfxFillRect(ctx, &rectVert1);
ASSERT(gfxFillRect);

FLUSH_SCREEN();
CuAssertColorEquals(tc, "Expected different XORed color",
( yellowish ^ bluish ), getPixel(&info, 210, 130));
CuAssertColorEquals(tc, "Expected different XORed color",
( yellowish ^ reddish ), getPixel(&info, 250, 130));

DO_CLEANUP;
#endif

}

void test_gfxBlit_SRCOVER(CuTest* tc)
{
/* if I am understanding this correctly, I should assert
 ** that if I create a shape on a surface other than the main
 ** surface, then when I bitBlt it to the main surface and flush
 ** the screen, I should see the shape or verify that it is there.
 */
int offscreen = 0;
mpe_Error result = 0;

mpe_GfxSurfaceInfo subSurfInfo; //This is the info of the surface that will
// be created for this test.

mpe_GfxColor fg = mpe_gfxRgbToColor(0xff, 0x00, 0x00);
mpe_GfxColor bg = mpe_gfxRgbToColor(0, 0, 0);
int x = 10, y = 10;
int w = 60, h = 30;
mpe_GfxRectangle rect;
uint32_t dx = 50;
uint32_t dy = 50;
mpe_GfxRectangle testRect;
mpe_GfxSurface newSurf = 0;
mpe_GfxContext myCtx;
mpe_GfxRectangle subSurfRect;
DO_SETUP;

rect.x = x;
rect.y = y;
rect.width = w;
rect.height = h;

testRect.x = 60;
testRect.y = 60;
testRect.width = 60;
testRect.height = 30;

subSurfRect.x = 0;
subSurfRect.y = 0;
subSurfRect.width = 100;
subSurfRect.height = 100;

CLEAR_BG(ctx, bg);

/*
 ** First, populate surface info struct with info of
 ** main surface info struct
 */
memcpy(&subSurfInfo, &info, sizeof(info));
subSurfInfo.dim.height = 100;
subSurfInfo.dim.width = 100;
subSurfInfo.widthbytes = 100;

/*
 ** Create a new surface to draw on.
 */
result = gfxSurfaceNew(&subSurfInfo, &newSurf);
ASSERT( gfxSurfaceNew);

/*
 ** Now get the context to my new surface
 */
result = gfxContextNew(newSurf, &myCtx);
ASSERT( gfxContextNew);

/*
 ** clear the new surfaces bg
 */
gfxSetColor(myCtx, bg);
gfxFillRect(myCtx, &subSurfRect);

/*
 ** New surface rect to be bitblt'ed
 */
gfxSetColor(myCtx, fg);
result = gfxFillRect(myCtx, &rect);
ASSERT( gfxFillRect);

/*
 ** Now BitBlt the new surface to the screen with SRC_OVER paint mode
 */

gfxSetPaintMode(ctx, MPE_GFX_SRCOVER, 255);

result = gfxBitBlt(ctx, newSurf, dx, dy, &subSurfRect);
ASSERT( gfxBitBlt);

FLUSH_SCREEN();

/*
 ** Check that everything is ok
 */
checkFillRect(tc, &info, &testRect, bg, fg);

FLUSH_SCREEN();

DO_CLEANUP;

}

void test_gfxDraw_SRCOVER(CuTest* tc)
{
mpe_GfxColor reddish = mpe_gfxRgbToColor(0xE5, 0x4C, 0x4C);
mpe_GfxColor yellowish = mpe_gfxRgbToColor(0xFF, 0xF2, 0x70);
mpe_GfxColor bluish = mpe_gfxRgbToColor(0x4B, 0x4B, 0xCC);
//mpe_GfxColor greenish  = mpe_gfxRgbToColor(0x8C, 0xBD, 0x47);

uint32_t modeData = 0x80; // for alpha constant modulation

int offscreen = 0; // initial surface is screen
mpe_Error result = 0;
mpe_GfxColor bg = mpe_gfxRgbToColor(0, 0, 0);
mpe_GfxRectangle rectHor1;
mpe_GfxRectangle rectHor2;
mpe_GfxRectangle rectVert1;
mpe_GfxPaintMode mode;
int bAlpha, bRed, bGreen, bBlue;
int rAlpha, rRed, rGreen, rBlue;
int yAlpha, yRed, yGreen, yBlue;
int mAlpha, mRed, mGreen, mBlue;
int readAlpha, readRed, readGreen, readBlue;
mpe_GfxColor readColor;
DO_SETUP;

TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entering test_gfxDraw_SRCOVER");

rectHor1.x = 100;
rectHor1.y = 100;
rectHor1.width = 120;
rectHor1.height = 60;

rectHor2.x = 240;
rectHor2.y = 100;
rectHor2.width = 120;
rectHor2.height = 60;

rectVert1.x = 200;
rectVert1.y = 40;
rectVert1.width = 60;
rectVert1.height = 180;

CLEAR_BG(ctx, bg);

/*
 ** Create three rectangles, the last overlapping
 ** the previously drawn two.
 */
gfxSetColor(ctx, bluish);
result = gfxFillRect(ctx, &rectHor1);
ASSERT( gfxFillRect);

gfxSetColor(ctx, reddish);
result = gfxFillRect(ctx, &rectHor2);
ASSERT(gfxFillRect);

mode = MPE_GFX_SRCOVER;
gfxSetPaintMode(ctx, mode, modeData);
gfxSetColor(ctx, yellowish);
result = gfxFillRect(ctx, &rectVert1);
ASSERT(gfxFillRect);
FLUSH_SCREEN();

// Check the resultant mixtures
// We know that src over (A over B) is computed
//    A * 1 + B * (1-alphaofA), where A, B are premultiplied
//
// Extract color components
EXTRACT_COLOR( bluish, bAlpha, bRed, bGreen, bBlue );
EXTRACT_COLOR( reddish, rAlpha, rRed, rGreen, rBlue );
EXTRACT_COLOR( yellowish, yAlpha, yRed, yGreen, yBlue );

// Pre-multiply color components by alpha
PREMULT( bAlpha, bRed, bGreen, bBlue );
PREMULT( rAlpha, rRed, rGreen, rBlue );

// The paint mode SRCOVER causes us to have an alpha of modeData for src
PREMULT( modeData, yRed, yGreen, yBlue );

// Calculate new color components
// The left hand mixture, yellowish over bluish:
mAlpha = yAlpha + (modeData * (0xFF - modeData)) / 0xFF;
mRed = yRed + (bRed * (0xFF - modeData)) / 0xFF;
mGreen = yGreen + (bGreen * (0xFF - modeData)) / 0xFF;
mBlue = yBlue + (bBlue * (0xFF - modeData)) / 0xFF;

// Un-premultiply the result
UNPREMULT( mAlpha, mRed, mGreen, mBlue );

// Get a pixel from smack dab in the middle of the zone
readColor = getPixel(&info, 210, 130);
EXTRACT_COLOR( readColor, readAlpha, readRed, readGreen, readBlue );

// Compare our prediction to the colors we read
// We compare the components separately because they differ slightly from
// what was calculated, but if we compare the colors, they wouldn't match
CuAssert(tc, "(yob)Reds should match between what we predict and what we read",
abs(readRed - mRed) < 2);
CuAssert(tc,
"(yob)Greens should match between what we predict and what we read", abs(
readGreen - mGreen) < 2);
CuAssert(tc,
"(yob)Blues should match between what we predict and what we read", abs(
readBlue - mBlue) < 2);

// Now the right hand mixture, yellowish over reddish
mRed = yRed + (rRed * (0xFF - modeData)) / 0xFF;
mGreen = yGreen + (rGreen * (0xFF - modeData)) / 0xFF;
mBlue = yBlue + (rBlue * (0xFF - modeData)) / 0xFF;

// Un-premultiply the result
UNPREMULT( mAlpha, mRed, mGreen, mBlue );

// Get a pixel from smack dab in the middle of the other zone
readColor = getPixel(&info, 250, 130);
EXTRACT_COLOR( readColor, readAlpha, readRed, readGreen, readBlue );

// Compare our prediction to the colors we read
// We compare the components separately because they differ slightly from
// what was calculated, but if we compare the colors, they wouldn't match
CuAssert(tc, "(yor)Reds should match between what we predict and what we read",
abs(readRed - mRed) < 2);
CuAssert(tc,
"(yor)Greens should match between what we predict and what we read", abs(
readGreen - mGreen) < 2);
CuAssert(tc,
"(yor)Blues should match between what we predict and what we read", abs(
readBlue - mBlue) < 2);

TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "Exiting test_gfxDraw_SRCOVER");

DO_CLEANUP;
THREAD_SLEEP(200,0);
}

void test_gfxDraw_SRC(CuTest* tc)
{
    mpe_GfxColor reddish = mpe_gfxRgbToColor(0xE5, 0x4C, 0x4C);
    mpe_GfxColor yellowish = mpe_gfxRgbToColor(0xFF, 0xF2, 0x70);
    //mpe_GfxColor greenish  = mpe_gfxRgbToColor(0x8C, 0xBD, 0x47);
    mpe_GfxColor bluish = mpe_gfxRgbToColor(0x4B, 0x4B, 0xCC);
    uint32_t modeData = 0x80; // for alpha constant modulation

    int offscreen = 0; // initial surface is screen
    mpe_Error result = 0;

    mpe_GfxColor bg = mpe_gfxRgbToColor(0, 0, 0);
    mpe_GfxRectangle rectHor1;
    mpe_GfxRectangle rectHor2;
    mpe_GfxRectangle rectVert1;
    mpe_GfxPaintMode mode;
    mpe_GfxColor readColor;
    DO_SETUP;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entering test_gfxDraw_SRC");

    rectHor1.x = 100;
    rectHor1.y = 100;
    rectHor1.width = 120;
    rectHor1.height = 60;

    rectHor2.x = 240;
    rectHor2.y = 100;
    rectHor2.width = 120;
    rectHor2.height = 60;

    rectVert1.x = 200;
    rectVert1.y = 40;
    rectVert1.width = 60;
    rectVert1.height = 180;

    CLEAR_BG(ctx, bg);

    /*
     ** Create three rectangles, the last overlapping
     ** the previously drawn two.
     */
    gfxSetColor(ctx, bluish);
    result = gfxFillRect(ctx, &rectHor1);
    ASSERT( gfxFillRect);

    gfxSetColor(ctx, reddish);
    result = gfxFillRect(ctx, &rectHor2);
    ASSERT(gfxFillRect);

    mode = MPE_GFX_SRC;

    gfxSetPaintMode(ctx, mode, modeData);
    gfxSetColor(ctx, yellowish);
    result = gfxFillRect(ctx, &rectVert1);
    ASSERT(gfxFillRect);
    FLUSH_SCREEN();

    // Check the resultant mixtures
    // We know that src (A) is computed
    //    A * 1
    //
    // Get a pixel from smack dab in the middle of the zone
    readColor = getPixel(&info, 210, 130);

    // Compare our prediction to the colors we read
    CuAssert(tc, "(yob) Colors should match ", yellowish == readColor);

    // Get a pixel from smack dab in the middle of the other zone
    readColor = getPixel(&info, 250, 130);

    // Compare our prediction to the colors we read
    CuAssert(tc, "(yor) Colors should match", yellowish == readColor);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "Exiting test_gfxDraw_SRC");
    DO_CLEANUP;
    THREAD_SLEEP(200,0);
}

void setRect(mpe_GfxRectangle * rect, int x, int y, int width, int height)
{
    rect->x = x;
    rect->y = y;
    rect->width = width;
    rect->height = height;
}

void test_gfxDraw_CLR(CuTest* tc)
{
    mpe_GfxColor reddish = mpe_gfxRgbToColor(0xE5, 0x4C, 0x4C);
    mpe_GfxColor yellowish = mpe_gfxRgbToColor(0xFF, 0xF2, 0x70);
    //mpe_GfxColor greenish  = mpe_gfxRgbToColor(0x8C, 0xBD, 0x47);
    mpe_GfxColor bluish = mpe_gfxRgbToColor(0x4B, 0x4B, 0xCC);
    uint32_t modeData = 0x80; // for alpha constant modulation
    int offscreen = 0; // initial surface is screen
    mpe_Error result = 0;
    mpe_GfxRectangle rectHor1;
    mpe_GfxRectangle rectHor2;
    mpe_GfxRectangle rectVert1;
    mpe_GfxPaintMode mode;
    mpe_GfxColor bg, readColor;
    DO_SETUP;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entering test_gfxDraw_CLR");

    bg = mpe_gfxRgbToColor(0, 0, 0);
    CLEAR_BG(ctx, bg);

    /*
     ** Create three rectangles, the last overlapping
     ** the previously drawn two.
     */
    setRect(&rectHor1, 100, 100, 120, 60);
    setRect(&rectHor2, 240, 100, 120, 60);
    setRect(&rectVert1, 200, 40, 60, 180);

    gfxSetColor(ctx, bluish);
    result = gfxFillRect(ctx, &rectHor1);
    ASSERT( gfxFillRect);

    gfxSetColor(ctx, reddish);
    result = gfxFillRect(ctx, &rectHor2);
    ASSERT(gfxFillRect);

    mode = MPE_GFX_CLR;

    gfxSetPaintMode(ctx, mode, modeData);

    gfxSetColor(ctx, yellowish);
    result = gfxFillRect(ctx, &rectVert1);
    ASSERT(gfxFillRect);

    FLUSH_SCREEN();

    // Check the resultant mixtures
    // We know that CLR is computed
    //    no source and no destination
    //    => we expect only background color

    // Get a pixel from smack dab in the middle of the zone
    readColor = getPixel(&info, 210, 130);

    // Compare our prediction to the colors we read
    CuAssert(tc, "(yob) Colors should match ", bg == readColor);

    // Get a pixel from smack dab in the middle of the other zone
    readColor = getPixel(&info, 250, 130);

    // Compare our prediction to the colors we read
    CuAssert(tc, "(yor) Colors should match", bg == readColor);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "Exiting test_gfxDraw_CLR");

    DO_CLEANUP;
    THREAD_SLEEP(200,0);
}

void test_gfxDraw_OtherPorterDuff(CuTest* tc)
{
    mpe_GfxColor reddish = mpe_gfxRgbToColor(0xE5, 0x4C, 0x4C);
    mpe_GfxColor yellowish = mpe_gfxRgbToColor(0xFF, 0xF2, 0x70);
    //mpe_GfxColor greenish  = mpe_gfxRgbToColor(0x8C, 0xBD, 0x47);
    mpe_GfxColor bluish = mpe_gfxRgbToColor(0x4B, 0x4B, 0xCC);

    int offscreen = 0; // initial surface is screen
    mpe_Error result = 0;
    mpe_GfxColor bg;
    mpe_GfxPaintMode mode;
    uint32_t modeData = 0x90;
    mpe_GfxRectangle rectHor1;
    mpe_GfxRectangle rectHor2;
    mpe_GfxRectangle rectVert1;
    volatile int bAlpha, bRed, bGreen, bBlue;
    volatile int rAlpha, rRed, rGreen, rBlue;
    volatile int yAlpha, yRed, yGreen, yBlue;
    volatile int mAlpha, mRed, mGreen, mBlue;
    volatile int readAlpha, readRed, readGreen, readBlue;
    volatile mpe_GfxColor readColor;
    DO_SETUP;
    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entering test_gfxDraw_OtherPorterDuff");

    bg = mpe_gfxRgbToColor(0, 0, 0);
    CLEAR_BG(ctx, bg);

    /*
     ** Create three rectangles, the last overlapping
     ** the previously drawn two.
     */
    setRect(&rectHor1, 100, 100, 120, 60);
    setRect(&rectHor2, 240, 100, 120, 60);
    setRect(&rectVert1, 200, 40, 60, 180);

    gfxSetColor(ctx, bluish);
    result = gfxFillRect(ctx, &rectHor1);
    ASSERT( gfxFillRect);

    gfxSetColor(ctx, reddish);
    result = gfxFillRect(ctx, &rectHor2);
    ASSERT(gfxFillRect);

    mode = MPE_GFX_XOR;
    //modeData = (uint32_t)yellowish;
    gfxSetPaintMode(ctx, mode, modeData);

    gfxSetColor(ctx, yellowish);
    result = gfxFillRect(ctx, &rectVert1);
    ASSERT(gfxFillRect);
    FLUSH_SCREEN();

    // Check the resultant mixtures
    // We know that (A XOR B) is computed
    //    A * (1-alphaofB) + B * (1-alphaofA), where A, B are premultiplied
    //
    //
    // Extract color components
    EXTRACT_COLOR( bluish, bAlpha, bRed, bGreen, bBlue );
    EXTRACT_COLOR( reddish, rAlpha, rRed, rGreen, rBlue );
    EXTRACT_COLOR( yellowish, yAlpha, yRed, yGreen, yBlue );

    // Pre-multiply color components by alpha
    PREMULT( modeData, bRed, bGreen, bBlue );
    PREMULT( modeData, rRed, rGreen, rBlue );
    PREMULT( modeData, yRed, yGreen, yBlue );

    // Calculate new color components
    // The left hand mixture, yellowish over bluish:
    mAlpha = modeData + modeData - 2 * (modeData * modeData) / 0xFF;

    mRed = yRed * (0xFF - mAlpha) / 0xFF + bRed * (0xFF - mAlpha) / 0xFF;
    mGreen = yGreen * (0xFF - mAlpha) / 0xFF + bGreen * (0xFF - mAlpha) / 0xFF;
    mBlue = yBlue * (0xFF - mAlpha) / 0xFF + bBlue * (0xFF - mAlpha) / 0xFF;

    // Un-premultiply the result
    UNPREMULT( mAlpha, mRed, mGreen, mBlue );

    // Get a pixel from smack dab in the middle of the (yellowish PorterDuffXOR bluish) zone
    readColor = getPixel(&info, 210, 130);
    EXTRACT_COLOR( readColor, readAlpha, readRed, readGreen, readBlue );

    // Compare our prediction to the colors we read
    // The fairest comparision is with color components... My model isn't exactly right,
    // so I'm allowing matching within 10...
    CuAssert(tc, "(yob) Red components should match", ABS(mRed - readRed) < 10);
    CuAssert(tc, "(yob) Green components should match", ABS(mGreen - readGreen)
            < 10);
    CuAssert(tc, "(yob) Blue components should match", ABS(mBlue - readBlue)
            < 10);

    // Now for the other side
    mRed = yRed * (0xFF - mAlpha) / 0xFF + rRed * (0xFF - mAlpha) / 0xFF;
    mGreen = yGreen * (0xFF - mAlpha) / 0xFF + rGreen * (0xFF - mAlpha) / 0xFF;
    mBlue = yBlue * (0xFF - mAlpha) / 0xFF + rBlue * (0xFF - mAlpha) / 0xFF;

    // Un-premultiply the result
    UNPREMULT( mAlpha, mRed, mGreen, mBlue );

    // Get a pixel from smack dab in the middle of the (yellowish PorterDuffXOR reddish) zone
    readColor = getPixel(&info, 250, 130);
    EXTRACT_COLOR( readColor, readAlpha, readRed, readGreen, readBlue );

    // Compare our prediction from this side to the colors we read
    CuAssert(tc, "(yor) Red components should match", abs(mRed - readRed) < 10);
    CuAssert(tc, "(yor) Green components should match", abs(mGreen - readGreen)
            < 10);
    CuAssert(tc, "(yor) Blue components should match", abs(mBlue - readBlue)
            < 10);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "Exiting test_gfxDraw_OtherPorterDuff");

    DO_CLEANUP;
}

/**
 * Tests performance of fills.
 *
 * @param tc pointer to test case structure
 */
void test_gfxFillPerf(CuTest *tc)
{
    mpe_GfxContext context;
    mpe_GfxSurface surface;
    mpe_GfxSurfaceInfo surfaceInfo;
    mpe_GfxColor color = mpe_gfxRgbToColor(0, 0, 0);
    mpe_Error errorCode;
    mpe_GfxRectangle rect;
    int32_t alpha;
    int32_t index;
    mpe_TimeMillis enterTime;
    mpe_TimeMillis leaveTime;
    mpe_TimeMillis elapsed, fps;
    char message[256];

    CuTestSetup(errorCode = setup(&surface, &context, &surfaceInfo, 0));
    CuAssert(tc, "setup(&surface, &context, &surfaceInfo, 0)", errorCode
            == MPE_SUCCESS);

    // initialize surface

    rect.x = 0;
    rect.y = 0;
    rect.width = surfaceInfo.dim.width;
    rect.height = surfaceInfo.dim.height;

    color = mpe_gfxRgbToColor(255, 0, 0);

    CuAssert(tc, "gfxSetPaintMode(context, MPE_GFX_SRC, 255)", gfxSetPaintMode(
            context, MPE_GFX_SRC, 255) == MPE_SUCCESS);
    CuAssert(tc, "gfxSetColor(context, color)", gfxSetColor(context, color)
            == MPE_SUCCESS);
    CuAssert(tc, "gfxFillRect(context, &rect)", gfxFillRect(context, &rect)
            == MPE_SUCCESS);
    CuAssert(tc, "dispFlushGfxSurface(getDefaultGfx())", dispFlushGfxSurface(
            getDefaultGfx()) == MPE_SUCCESS);

    CuAssert(tc, "gfxSetPaintMode(context, MPE_GFX_SRC, 255)", gfxSetPaintMode(
            context, MPE_GFX_SRC, 255) == MPE_SUCCESS);
    //   CuAssert(tc, "gfxSetPaintMode(context, MPE_GFX_SRC, 127)", gfxSetPaintMode(context, MPE_GFX_SRC, 127) == MPE_SUCCESS);
    //   CuAssert(tc, "gfxSetPaintMode(context, MPE_GFX_SRC,   0)", gfxSetPaintMode(context, MPE_GFX_SRC,   0) == MPE_SUCCESS);
    //   CuAssert(tc, "gfxSetPaintMode(context, MPE_GFX_SRCOVER, 255)", gfxSetPaintMode(context, MPE_GFX_SRCOVER, 255) == MPE_SUCCESS);
    //   CuAssert(tc, "gfxSetPaintMode(context, MPE_GFX_SRCOVER, 127)", gfxSetPaintMode(context, MPE_GFX_SRCOVER, 127) == MPE_SUCCESS);
    //   CuAssert(tc, "gfxSetPaintMode(context, MPE_GFX_SRCOVER,   0)", gfxSetPaintMode(context, MPE_GFX_SRCOVER,   0) == MPE_SUCCESS);


    alpha = 255;
    //   alpha = 128;


    // fill

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "test_gfxFillPerf(), SrcOver, fill - enter benchmark section\n");
    TIME_GET_MILLIS(&enterTime);

    for (index = 0; index < 256; index += 1)
    {
        //      color = mpe_gfxRgbToColor(0, index, 0);
            color = mpe_gfxArgbToColor(alpha, 0, index, 0);

            CuAssert(tc, "gfxSetColor(context, color)", gfxSetColor(context, color) == MPE_SUCCESS);
            CuAssert(tc, "gfxFillRect(context, &rect)", gfxFillRect(context, &rect) == MPE_SUCCESS);
            //      CuAssert(tc, "dispFlushGfxSurface(getDefaultGfx())", dispFlushGfxSurface(getDefaultGfx()) == MPE_SUCCESS);
        }

        TIME_GET_MILLIS(&leaveTime);

        elapsed = leaveTime - enterTime;

        fps = ((mpe_TimeMillis)256 * (mpe_TimeMillis)1000) / elapsed;

#if 0
            sprintf(message, "test_gfxFillPerf(), SrcOver, fill - leave benchmark section: elapsed time = %dms, fps = %d\n", elapsed, fps);
#else
            sprintf(message, "test_gfxFillPerf(), SrcOver, fill - leave benchmark section: elapsed time = %ldms", (int32_t)elapsed);
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST, message);
            sprintf(message, ", fps = %ld\n", (int32_t)fps);
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST, message);
#endif

            // copy

            TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "test_gfxFillPerf(), SrcOver, copy - enter benchmark section\n");

            TIME_GET_MILLIS(&enterTime);

            for (index = 0; index < 256; index += 1)
            {
                //      color = mpe_gfxRgbToColor(0, index, 0);
                color = mpe_gfxArgbToColor(alpha, 0, index, 0);

                CuAssert(tc, "gfxSetColor(context, color)", gfxSetColor(context, color) == MPE_SUCCESS);
                //      CuAssert(tc, "gfxFillRect(context, &rect)", gfxFillRect(context, &rect) == MPE_SUCCESS);
                CuAssert(tc, "dispFlushGfxSurface(getDefaultGfx())", dispFlushGfxSurface(getDefaultGfx()) == MPE_SUCCESS);
            }

            TIME_GET_MILLIS(&leaveTime);

            elapsed = leaveTime - enterTime;

            fps = ((mpe_TimeMillis)256 * (mpe_TimeMillis)1000) / elapsed;

#if 0
            sprintf(message, "test_gfxFillPerf(), SrcOver, copy - leave benchmark section: elapsed time = %dms, fps = %d\n", elapsed, fps);
#else
            sprintf(message, "test_gfxFillPerf(), SrcOver, copy - leave benchmark section: elapsed time = %ldms", (int32_t)elapsed);
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST, message);
            sprintf(message, ", fps = %ld\n", (int32_t)fps);
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST, message);
#endif

            // both

            TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "test_gfxFillPerf(), SrcOver, both - enter benchmark section\n");

            TIME_GET_MILLIS(&enterTime);

            for (index = 0; index < 256; index += 1)
            {
                //      color = mpe_gfxRgbToColor(0, index, 0);
                color = mpe_gfxArgbToColor(alpha, 0, index, 0);

                CuAssert(tc, "gfxSetColor(context, color)", gfxSetColor(context, color) == MPE_SUCCESS);
                CuAssert(tc, "gfxFillRect(context, &rect)", gfxFillRect(context, &rect) == MPE_SUCCESS);
                CuAssert(tc, "dispFlushGfxSurface(getDefaultGfx())", dispFlushGfxSurface(getDefaultGfx()) == MPE_SUCCESS);
            }

            TIME_GET_MILLIS(&leaveTime);

            elapsed = leaveTime - enterTime;

            fps = ((mpe_TimeMillis)256 * (mpe_TimeMillis)1000) / elapsed;

#if 0
            sprintf(message, "test_gfxFillPerf(), SrcOver, both - leave benchmark section: elapsed time = %dms, fps = %d\n", elapsed, fps);
#else
            sprintf(message, "test_gfxFillPerf(), SrcOver, both - leave benchmark section: elapsed time = %ldms", (int32_t)elapsed);
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST, message);
            sprintf(message, ", fps = %ld\n", (int32_t)fps);
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST, message);
#endif

            color = mpe_gfxRgbToColor(0, 0, 255);

            CuAssert(tc, "gfxSetPaintMode(context, MPE_GFX_SRC, 255)", gfxSetPaintMode(context, MPE_GFX_SRC, 255) == MPE_SUCCESS);
            CuAssert(tc, "gfxSetColor(context, color)", gfxSetColor(context, color) == MPE_SUCCESS);
            CuAssert(tc, "gfxFillRect(context, &rect)", gfxFillRect(context, &rect) == MPE_SUCCESS);
            CuAssert(tc, "dispFlushGfxSurface(getDefaultGfx())", dispFlushGfxSurface(getDefaultGfx()) == MPE_SUCCESS);

            CuTestCleanup(teardown(surface, context, 0));
        }

    /**
     * Tests performance of blits.
     *
     * @param tc pointer to test case structure
     */
void test_gfxBlitPerf(CuTest *tc)
{
    mpe_GfxContext context;
    mpe_GfxSurface surface;
    mpe_GfxSurfaceInfo surfaceInfo;
    mpe_GfxContext context2;
    mpe_GfxSurface surface2;
    mpe_GfxSurfaceInfo surface2Info;
    mpe_GfxColor color = mpe_gfxRgbToColor(0, 0, 0);
    mpe_Error errorCode;
    mpe_GfxRectangle rect;
    int32_t alpha;
    int32_t index;
    mpe_TimeMillis enterTime;
    mpe_TimeMillis leaveTime;
    mpe_TimeMillis elapsed, fps;
    char message[256];
    int32_t frameCount;

    frameCount = 64;

    CuTestSetup(errorCode = setup(&surface, &context, &surfaceInfo, 0));
    CuAssert(tc, "setup(&surface, &context, &surfaceInfo, 0)", errorCode
            == MPE_SUCCESS);

    // initialize surface2

    memcpy(&surface2Info, &surfaceInfo, sizeof(mpe_GfxSurfaceInfo));
#if 0
    surface2Info.format = MPE_GFX_RGB888;
    surface2Info.bpp = MPE_GFX_24BPP;
    surface2Info.widthbytes = surface2Info.dim.width * 3;
#endif
    CuAssert(tc, "gfxSurfaceNew(&surface2Info, &surface2)", gfxSurfaceNew(
            &surface2Info, &surface2) == MPE_SUCCESS);
    CuAssert(tc, "gfxContextNew(surface2, &context2)", gfxContextNew(surface2,
            &context2) == MPE_SUCCESS);

    rect.x = 0;
    rect.y = 0;
    rect.width = surface2Info.dim.width;
    rect.height = surface2Info.dim.height;

    color = mpe_gfxArgbToColor(255, 255, 0, 255);
    //   color = mpe_gfxArgbToColor(128, 255, 0, 255);

    CuAssert(tc, "gfxSetPaintMode(context2, MPE_GFX_SRC, 255)",
            gfxSetPaintMode(context2, MPE_GFX_SRC, 255) == MPE_SUCCESS);
    CuAssert(tc, "gfxSetColor(context2, color)", gfxSetColor(context2, color)
            == MPE_SUCCESS);
    CuAssert(tc, "gfxFillRect(context2, &rect)", gfxFillRect(context2, &rect)
            == MPE_SUCCESS);

    // initialize surface

    rect.x = 0;
    rect.y = 0;
    rect.width = surfaceInfo.dim.width;
    rect.height = surfaceInfo.dim.height;

    color = mpe_gfxRgbToColor(255, 0, 0);

    CuAssert(tc, "gfxSetPaintMode(context, MPE_GFX_SRC, 255)", gfxSetPaintMode(
            context, MPE_GFX_SRC, 255) == MPE_SUCCESS);
    CuAssert(tc, "gfxSetColor(context, color)", gfxSetColor(context, color)
            == MPE_SUCCESS);
    CuAssert(tc, "gfxFillRect(context, &rect)", gfxFillRect(context, &rect)
            == MPE_SUCCESS);
    CuAssert(tc, "dispFlushGfxSurface(getDefaultGfx())", dispFlushGfxSurface(
            getDefaultGfx()) == MPE_SUCCESS);

    CuAssert(tc, "gfxSetPaintMode(context, MPE_GFX_SRC, 255)", gfxSetPaintMode(
            context, MPE_GFX_SRC, 255) == MPE_SUCCESS);
    //   CuAssert(tc, "gfxSetPaintMode(context, MPE_GFX_SRC, 127)", gfxSetPaintMode(context, MPE_GFX_SRC, 127) == MPE_SUCCESS);
    //   CuAssert(tc, "gfxSetPaintMode(context, MPE_GFX_SRC,   0)", gfxSetPaintMode(context, MPE_GFX_SRC,   0) == MPE_SUCCESS);
    //   CuAssert(tc, "gfxSetPaintMode(context, MPE_GFX_SRCOVER, 255)", gfxSetPaintMode(context, MPE_GFX_SRCOVER, 255) == MPE_SUCCESS);
    //   CuAssert(tc, "gfxSetPaintMode(context, MPE_GFX_SRCOVER, 127)", gfxSetPaintMode(context, MPE_GFX_SRCOVER, 127) == MPE_SUCCESS);
    //   CuAssert(tc, "gfxSetPaintMode(context, MPE_GFX_SRCOVER,   0)", gfxSetPaintMode(context, MPE_GFX_SRCOVER,   0) == MPE_SUCCESS);


    alpha = 255;

    // blit

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "test_gfxBlitPerf(), SrcOver, blit - enter benchmark section\n");
    TIME_GET_MILLIS(&enterTime);

    for (index = 0; index < frameCount; index += 1)
    {
        CuAssert(tc, "gfxSetColor(context, color)", gfxSetColor(context, color)== MPE_SUCCESS);
      CuAssert(tc, "gfxBitBlt(context, surface2, 0, 0, &rect)", gfxBitBlt(context, surface2, 0, 0, &rect) == MPE_SUCCESS);
//      CuAssert(tc, "dispFlushGfxSurface(getDefaultGfx())", dispFlushGfxSurface(getDefaultGfx()) == MPE_SUCCESS);
   }

   TIME_GET_MILLIS(&leaveTime);

   elapsed = leaveTime - enterTime;

   fps = ((mpe_TimeMillis)frameCount * (mpe_TimeMillis)1000) / elapsed;

#if 0
   sprintf(message, "test_gfxBlitPerf(), SrcOver, blit - leave benchmark section: elapsed time = %dms, fps = %d\n", elapsed, fps);
#else
   sprintf(message, "test_gfxBlitPerf(), SrcOver, blit - leave benchmark section: elapsed time = %ldms", (int32_t)elapsed);
   TRACE(MPE_LOG_INFO, MPE_MOD_TEST, message);
   sprintf(message, ", fps = %ld\n", (int32_t)fps);
   TRACE(MPE_LOG_INFO, MPE_MOD_TEST, message);
#endif


   // copy

   TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "test_gfxBlitPerf(), SrcOver, copy - enter benchmark section\n");

   TIME_GET_MILLIS(&enterTime);

   for (index = 0; index < frameCount; index += 1)
   {
      CuAssert(tc, "gfxSetColor(context, color)", gfxSetColor(context, color) == MPE_SUCCESS);
//      CuAssert(tc, "gfxBitBlt(context, surface2, 0, 0, &rect)", gfxBitBlt(context, surface2, 0, 0, &rect) == MPE_SUCCESS);
      CuAssert(tc, "dispFlushGfxSurface(getDefaultGfx())", dispFlushGfxSurface(getDefaultGfx()) == MPE_SUCCESS);
   }

   TIME_GET_MILLIS(&leaveTime);

   elapsed = leaveTime - enterTime;

   fps = ((mpe_TimeMillis)frameCount * (mpe_TimeMillis)1000) / elapsed;

#if 0
   sprintf(message, "test_gfxBlitPerf(), SrcOver, copy - leave benchmark section: elapsed time = %dms, fps = %d\n", elapsed, fps);
#else
   sprintf(message, "test_gfxBlitPerf(), SrcOver, copy - leave benchmark section: elapsed time = %ldms", (int32_t)elapsed);
   TRACE(MPE_LOG_INFO, MPE_MOD_TEST, message);
   sprintf(message, ", fps = %ld\n", (int32_t)fps);
   TRACE(MPE_LOG_INFO, MPE_MOD_TEST, message);
#endif


   // both

   TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "test_gfxBlitPerf(), SrcOver, both - enter benchmark section\n");

   TIME_GET_MILLIS(&enterTime);

   for (index = 0; index < frameCount; index += 1)
   {
      CuAssert(tc, "gfxSetColor(context, color)", gfxSetColor(context, color) == MPE_SUCCESS);
      CuAssert(tc, "gfxBitBlt(context, surface2, 0, 0, &rect)", gfxBitBlt(context, surface2, 0, 0, &rect) == MPE_SUCCESS);
      CuAssert(tc, "dispFlushGfxSurface(getDefaultGfx())", dispFlushGfxSurface(getDefaultGfx()) == MPE_SUCCESS);
   }

   TIME_GET_MILLIS(&leaveTime);

   elapsed = leaveTime - enterTime;

   fps = ((mpe_TimeMillis)frameCount * (mpe_TimeMillis)1000) / elapsed;

#if 0
   sprintf(message, "test_gfxBlitPerf(), SrcOver, both - leave benchmark section: elapsed time = %dms, fps = %d\n", elapsed, fps);
#else
   sprintf(message, "test_gfxBlitPerf(), SrcOver, both - leave benchmark section: elapsed time = %ldms", (int32_t)elapsed);
   TRACE(MPE_LOG_INFO, MPE_MOD_TEST, message);
   sprintf(message, ", fps = %ld\n", (int32_t)fps);
   TRACE(MPE_LOG_INFO, MPE_MOD_TEST, message);
#endif


   color = mpe_gfxRgbToColor(0, 0, 255);

   CuAssert(tc, "gfxSetPaintMode(context, MPE_GFX_SRC, 255)", gfxSetPaintMode(context, MPE_GFX_SRC, 255) == MPE_SUCCESS);
   CuAssert(tc, "gfxSetColor(context, color)", gfxSetColor(context, color) == MPE_SUCCESS);
   CuAssert(tc, "gfxFillRect(context, &rect)", gfxFillRect(context, &rect) == MPE_SUCCESS);
   CuAssert(tc, "dispFlushGfxSurface(getDefaultGfx())", dispFlushGfxSurface(getDefaultGfx()) == MPE_SUCCESS);

   CuAssert(tc, "gfxContextDelete(context2)", gfxContextDelete(context2) == MPE_SUCCESS);
   CuAssert(tc, "gfxSurfaceDelete(surface2)", gfxSurfaceDelete(surface2) == MPE_SUCCESS);

   CuTestCleanup(teardown(surface, context, 0));
}

    /*============================================================================
     * The DEF_TEST/DEF_TEST_OFFSCREEN macros are used to generate test functions
     * that call the above dotest_* functions.
     *============================================================================*/
    /** Used to define a test that calls dotest_* for onscreen. */
#define DEF_TEST(what)                          \
   void test_ ## what (CuTest *tc);              \
   void test_ ## what (CuTest *tc)              \
   {                                            \
       dotest_ ## what (tc, 0);                 \
   }
    /** Used to define a test that calls dotest_* for offscreen. */
#define DEF_TEST_OFFSCREEN(what)                \
   void test_ ## what ## _offscreen(CuTest *tc); \
   void test_ ## what ## _offscreen(CuTest *tc) \
   {                                            \
       dotest_ ## what (tc, 1);                 \
   }

    /*============================================================================
     * The following define test functions that call the above dotest_* functions.
     *============================================================================*/
DEF_TEST(gfxDrawLine)
DEF_TEST_OFFSCREEN(gfxDrawLine)
DEF_TEST(gfxDrawRect)
DEF_TEST_OFFSCREEN(gfxDrawRect)
DEF_TEST(gfxFillRect)
DEF_TEST_OFFSCREEN(gfxFillRect)
DEF_TEST(gfxClearRect)
DEF_TEST_OFFSCREEN(gfxClearRect)
DEF_TEST(gfxDrawEllipse)
DEF_TEST_OFFSCREEN(gfxDrawEllipse)
DEF_TEST(gfxFillEllipse)
DEF_TEST_OFFSCREEN(gfxFillEllipse)
DEF_TEST(gfxDrawRoundRect)
DEF_TEST_OFFSCREEN(gfxDrawRoundRect)
DEF_TEST(gfxFillRoundRect)
DEF_TEST_OFFSCREEN(gfxFillRoundRect)
DEF_TEST(gfxDrawArc)
DEF_TEST_OFFSCREEN(gfxDrawArc)
DEF_TEST(gfxFillArc)
DEF_TEST_OFFSCREEN(gfxFillArc)
DEF_TEST(gfxDrawPolyline)
DEF_TEST_OFFSCREEN(gfxDrawPolyline)
DEF_TEST(gfxDrawPolygon)
DEF_TEST_OFFSCREEN(gfxDrawPolygon)
DEF_TEST(gfxFillPolygon)
DEF_TEST_OFFSCREEN(gfxFillPolygon)
DEF_TEST(gfxBitBlt)
DEF_TEST_OFFSCREEN(gfxBitBlt)
DEF_TEST(gfxStretchBlt)
DEF_TEST_OFFSCREEN(gfxStretchBlt)
DEF_TEST(gfxDrawString)
DEF_TEST_OFFSCREEN(gfxDrawString)
DEF_TEST(gfxDrawString16)
DEF_TEST_OFFSCREEN(gfxDrawString16)
DEF_TEST(gfxDraw_Clipped)
DEF_TEST_OFFSCREEN(gfxDraw_Clipped)
DEF_TEST(gfxDraw_Translated)
DEF_TEST_OFFSCREEN(gfxDraw_Translated)
DEF_TEST(gfxDraw_DistinctState)
DEF_TEST_OFFSCREEN(gfxDraw_DistinctState)

/**
 * Create and return the test suite for the mpe_gfx APIs.
 * @return a pointer to the new test suite.
 */
CuSuite* getTestSuite_gfxDraw(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_STOP_TEST(suite, test_gfxDrawSetup);
    SUITE_STOP_TEST(suite, test_gfxDraw_DistinctContext);
    SUITE_STOP_TEST(suite, test_gfxDrawLine);
    SUITE_STOP_TEST(suite, test_gfxDrawLine_offscreen);
    SUITE_STOP_TEST(suite, test_gfxDrawRect);
    SUITE_STOP_TEST(suite, test_gfxDrawRect_offscreen);
    SUITE_STOP_TEST(suite, test_gfxFillRect);
    SUITE_STOP_TEST(suite, test_gfxFillRect_offscreen);
    SUITE_STOP_TEST(suite, test_gfxClearRect);
    SUITE_STOP_TEST(suite, test_gfxClearRect_offscreen);
    SUITE_STOP_TEST(suite, test_gfxDrawEllipse);
    SUITE_STOP_TEST(suite, test_gfxDrawEllipse_offscreen);
    SUITE_STOP_TEST(suite, test_gfxFillEllipse);
    SUITE_STOP_TEST(suite, test_gfxFillEllipse_offscreen);
    SUITE_STOP_TEST(suite, test_gfxDrawRoundRect);
    SUITE_STOP_TEST(suite, test_gfxDrawRoundRect_offscreen);
    SUITE_STOP_TEST(suite, test_gfxFillRoundRect);
    SUITE_STOP_TEST(suite, test_gfxFillRoundRect_offscreen);
    SUITE_STOP_TEST(suite, test_gfxDrawArc);
    SUITE_STOP_TEST(suite, test_gfxDrawArc_offscreen);
    SUITE_STOP_TEST(suite, test_gfxFillArc);
    SUITE_STOP_TEST(suite, test_gfxFillArc_offscreen);
    SUITE_STOP_TEST(suite, test_gfxDrawPolyline);
    SUITE_STOP_TEST(suite, test_gfxDrawPolyline_offscreen);
    SUITE_STOP_TEST(suite, test_gfxDrawPolygon);
    SUITE_STOP_TEST(suite, test_gfxDrawPolygon_offscreen);
    SUITE_STOP_TEST(suite, test_gfxFillPolygon);
    SUITE_STOP_TEST(suite, test_gfxFillPolygon_offscreen);
    SUITE_STOP_TEST(suite, test_gfxBitBlt);
    SUITE_STOP_TEST(suite, test_gfxBlit_SRCOVER);
    SUITE_STOP_TEST(suite, test_gfxBitBlt_offscreen);
    SUITE_STOP_TEST(suite, test_gfxBitBlt_diff);
    SUITE_STOP_TEST(suite, test_gfxStretchBlt);
    SUITE_STOP_TEST(suite, test_gfxStretchBlt_offscreen);
    SUITE_STOP_TEST(suite, test_gfxStretchBlt_other);
    SUITE_STOP_TEST(suite, test_gfxStretchBlt_diff);
    SUITE_STOP_TEST(suite, test_gfxDrawString);
    SUITE_STOP_TEST(suite, test_gfxDrawString_offscreen);
    SUITE_STOP_TEST(suite, test_gfxDrawString16);
    SUITE_STOP_TEST(suite, test_gfxDrawString16_offscreen);
    SUITE_STOP_TEST(suite, test_gfxDraw_Clipped);
    SUITE_STOP_TEST(suite, test_gfxDraw_Clipped_offscreen);
    SUITE_STOP_TEST(suite, test_gfxDraw_Translated);
    SUITE_STOP_TEST(suite, test_gfxDraw_Translated_offscreen);
    SUITE_STOP_TEST(suite, test_gfxDraw_DistinctState);
    SUITE_STOP_TEST(suite, test_gfxDraw_DistinctState_offscreen);

    // SUITE_STOP_TEST(suite, test_gfxDraw_DistinctSurface);

    SUITE_STOP_TEST(suite, test_gfxDraw_XOR);
    SUITE_STOP_TEST(suite, test_gfxDraw_SRCOVER);
    SUITE_STOP_TEST(suite, test_gfxDraw_SRC);
    SUITE_STOP_TEST(suite, test_gfxDraw_CLR);
    SUITE_STOP_TEST(suite, test_gfxDraw_OtherPorterDuff);

    // SUITE_STOP_TEST(suite, test_gfxFillPerf);
    // SUITE_STOP_TEST(suite, test_gfxBlitPerf);

    return suite;
}

CuSuite* getTest_DrawPolygon(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_STOP_TEST(suite, test_gfxDrawPolygon);

    return suite;
}

void test_gfxRunDrawTests(void)
{
    test_gfxRunSuite(getTestSuite_gfxDraw(), "DrawTests");
}

void test_gfxRunDrawSetupTest(void)
{
    CuSuite* suite;

    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(), "test_gfxDrawSetup");
    test_gfxRunSuite(suite, "gfxDrawSetup");

    CuSuiteFree(suite);
}

void test_gfxDraw_DistinctContextTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(),
            "test_gfxDraw_DistinctContext");
    test_gfxRunSuite(suite, "gfxDraw_DistinctContext");
    CuSuiteFree(suite);
}

void test_gfxDrawLineTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(), "test_gfxDrawLine");
    test_gfxRunSuite(suite, "test_gfxDrawLine");
    CuSuiteFree(suite);
}

void test_gfxDrawLine_offscreenTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(),
            "test_gfxDrawLine_offscreen");
    test_gfxRunSuite(suite, "test_gfxDrawLine_offscreen");
    CuSuiteFree(suite);
}

void test_gfxDrawRectTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(), "test_gfxDrawRect");
    test_gfxRunSuite(suite, "test_gfxDrawRect");
    CuSuiteFree(suite);
}

void test_gfxDrawRect_offscreenTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(),
            "test_gfxDrawRect_offscreen");
    test_gfxRunSuite(suite, "test_gfxDrawRect_offscreen");
    CuSuiteFree(suite);
}

void test_gfxFillRectTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(), "test_gfxFillRect");
    test_gfxRunSuite(suite, "test_gfxFillRect");
    CuSuiteFree(suite);
}

void test_gfxFillRect_offscreenTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(),
            "test_gfxFillRect_offscreen");
    test_gfxRunSuite(suite, "test_gfxFillRect_offscreen");
    CuSuiteFree(suite);
}

void test_gfxClearRectTest(void)
{
    CuSuite *suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(), "test_gfxClearRect");
    test_gfxRunSuite(suite, "test_gfxClearRect");
    CuSuiteFree(suite);
}

void test_gfxClearRect_offscreenTest(void)
{
    CuSuite *suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(),
            "test_gfxClearRect_offscreen");
    test_gfxRunSuite(suite, "test_gfxClearRect_offscreen");
    CuSuiteFree(suite);
}

void test_gfxDrawEllipseTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(), "test_gfxDrawEllipse");
    test_gfxRunSuite(suite, "test_gfxDrawEllipse");
    CuSuiteFree(suite);
}

void test_gfxDrawEllipse_offscreenTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(),
            "test_gfxDrawEllipse_offscreen");
    test_gfxRunSuite(suite, "test_gfxDrawEllipse_offscreen");
    CuSuiteFree(suite);
}

void test_gfxFillEllipseTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(), "test_gfxFillEllipse");
    test_gfxRunSuite(suite, "test_gfxFillEllipse");
    CuSuiteFree(suite);
}

void test_gfxFillEllipse_offscreenTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(),
            "test_gfxFillEllipse_offscreen");
    test_gfxRunSuite(suite, "test_gfxFillEllipse_offscreen");
    CuSuiteFree(suite);
}

void test_gfxDrawRoundRectTest(void)
{
    CuSuite* suite;
    suite
            = CuSuiteNewCloneTest(getTestSuite_gfxDraw(),
                    "test_gfxDrawRoundRect");
    test_gfxRunSuite(suite, "test_gfxDrawRoundRect");
    CuSuiteFree(suite);
}

void test_gfxDrawRoundRect_offscreenTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(),
            "test_gfxDrawRoundRect_offscreen");
    test_gfxRunSuite(suite, "test_gfxDrawRoundRect_offscreen");
    CuSuiteFree(suite);
}

void test_gfxFillRoundRectTest(void)
{
    CuSuite* suite;
    suite
            = CuSuiteNewCloneTest(getTestSuite_gfxDraw(),
                    "test_gfxFillRoundRect");
    test_gfxRunSuite(suite, "test_gfxFillRoundRect");
    CuSuiteFree(suite);
}

void test_gfxFillRoundRect_offscreenTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(),
            "test_gfxFillRoundRect_offscreen");
    test_gfxRunSuite(suite, "test_gfxFillRoundRect_offscreen");
    CuSuiteFree(suite);
}

void test_gfxDrawArcTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(), "test_gfxDrawArc");
    test_gfxRunSuite(suite, "test_gfxDrawArc");
    CuSuiteFree(suite);
}

void test_gfxDrawArc_offscreenTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(),
            "test_gfxDrawArc_offscreen");
    test_gfxRunSuite(suite, "test_gfxDrawArc_offscreen");
    CuSuiteFree(suite);
}

void test_gfxFillArcTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(), "test_gfxFillArc");
    test_gfxRunSuite(suite, "test_gfxFillArc");
    CuSuiteFree(suite);
}

void test_gfxFillArc_offscreenTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(),
            "test_gfxFillArc_offscreen");
    test_gfxRunSuite(suite, "test_gfxFillArc_offscreen");
    CuSuiteFree(suite);
}

void test_gfxDrawPolylineTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(), "test_gfxDrawPolyline");
    test_gfxRunSuite(suite, "test_gfxDrawPolyline");
    CuSuiteFree(suite);
}

void test_gfxDrawPolyline_offscreenTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(),
            "test_gfxDrawPolyline_offscreen");
    test_gfxRunSuite(suite, "test_gfxDrawPolyline_offscreen");
    CuSuiteFree(suite);
}

void test_gfxDrawPolygonTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(), "test_gfxDrawPolygon");
    test_gfxRunSuite(suite, "test_gfxDrawPolygon");
    CuSuiteFree(suite);
}

void test_gfxDrawPolygon_offscreenTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(),
            "test_gfxDrawPolygon_offscreen");
    test_gfxRunSuite(suite, "test_gfxDrawPolygon_offscreen");
    CuSuiteFree(suite);
}

void test_gfxFillPolygonTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(), "test_gfxFillPolygon");
    test_gfxRunSuite(suite, "test_gfxFillPolygon");
    CuSuiteFree(suite);
}

void test_gfxFillPolygon_offscreenTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(),
            "test_gfxFillPolygon_offscreen");
    test_gfxRunSuite(suite, "test_gfxFillPolygon_offscreen");
    CuSuiteFree(suite);
}

void test_gfxBitBltTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(), "test_gfxBitBlt");
    test_gfxRunSuite(suite, "test_gfxBitBlt");
    CuSuiteFree(suite);
}

void test_gfxBlit_SRCOVERTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(), "test_gfxBlit_SRCOVER");
    test_gfxRunSuite(suite, "test_gfxBlit_SRCOVER");
    CuSuiteFree(suite);
}

void test_gfxBitBlt_offscreenTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(),
            "test_gfxBitBlt_offscreen");
    test_gfxRunSuite(suite, "test_gfxBitBlt_offscreen");
    CuSuiteFree(suite);
}

void test_gfxBitBlt_diffTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(), "test_gfxBitBlt_diff");
    test_gfxRunSuite(suite, "test_gfxBitBlt_diff");
    CuSuiteFree(suite);
}

void test_gfxStretchBltTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(), "test_gfxStretchBlt");
    test_gfxRunSuite(suite, "test_gfxStretchBlt");
    CuSuiteFree(suite);
}

void test_gfxStretchBlt_offscreenTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(),
            "test_gfxStretchBlt_offscreen");
    test_gfxRunSuite(suite, "test_gfxStretchBlt_offscreen");
    CuSuiteFree(suite);
}

void test_gfxStretchBlt_otherTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(),
            "test_gfxStretchBlt_other");
    test_gfxRunSuite(suite, "test_gfxStretchBlt_other");
    CuSuiteFree(suite);
}

void test_gfxStretchBlt_diffTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(),
            "test_gfxStretchBlt_diff");
    test_gfxRunSuite(suite, "test_gfxStretchBlt_diff");
    CuSuiteFree(suite);
}

void test_gfxDrawStringTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(), "test_gfxDrawString");
    test_gfxRunSuite(suite, "test_gfxDrawString");
    CuSuiteFree(suite);
}

void test_gfxDrawString_offscreenTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(),
            "test_gfxDrawString_offscreen");
    test_gfxRunSuite(suite, "test_gfxDrawString_offscreen");
    CuSuiteFree(suite);
}

void test_gfxDrawString16Test(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(), "test_gfxDrawString16");
    test_gfxRunSuite(suite, "test_gfxDrawString16");
    CuSuiteFree(suite);
}

void test_gfxDrawString16_offscreenTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(),
            "test_gfxDrawString16_offscreen");
    test_gfxRunSuite(suite, "test_gfxDrawString16_offscreen");
    CuSuiteFree(suite);
}

void test_gfxDraw_ClippedTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(), "test_gfxDraw_Clipped");
    test_gfxRunSuite(suite, "test_gfxDraw_Clipped");
    CuSuiteFree(suite);
}

void test_gfxDraw_Clipped_offscreenTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(),
            "test_gfxDraw_Clipped_offscreen");
    test_gfxRunSuite(suite, "test_gfxDraw_Clipped_offscreen");
    CuSuiteFree(suite);
}

void test_gfxDraw_TranslatedTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(),
            "test_gfxDraw_Translated");
    test_gfxRunSuite(suite, "test_gfxDraw_Translated");
    CuSuiteFree(suite);
}

void test_gfxDraw_Translated_offscreenTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(),
            "test_gfxDraw_Translated_offscreen");
    test_gfxRunSuite(suite, "test_gfxDraw_Translated_offscreen");
    CuSuiteFree(suite);
}

void test_gfxDraw_DistinctStateTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(),
            "test_gfxDraw_DistinctState");
    test_gfxRunSuite(suite, "test_gfxDraw_DistinctState");
    CuSuiteFree(suite);
}

void test_gfxDraw_DistinctState_offscreenTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(),
            "test_gfxDraw_DistinctState_offscreen");
    test_gfxRunSuite(suite, "test_gfxDraw_DistinctState_offscreen");
    CuSuiteFree(suite);
}

void test_gfxDraw_DistinctSurfaceTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(),
            "test_gfxDraw_DistinctSurface");
    test_gfxRunSuite(suite, "test_gfxDraw_DistinctSurface");
    CuSuiteFree(suite);
}

void test_gfxDraw_XORTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(), "test_gfxDraw_XOR");
    test_gfxRunSuite(suite, "test_gfxDraw_XOR");
    CuSuiteFree(suite);
}

void test_gfxDraw_SRCOVERTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(), "test_gfxDraw_SRCOVER");
    test_gfxRunSuite(suite, "test_gfxDraw_SRCOVER");
    CuSuiteFree(suite);
}

void test_gfxDraw_SRCTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(), "test_gfxDraw_SRC");
    test_gfxRunSuite(suite, "test_gfxDraw_SRC");
    CuSuiteFree(suite);
}

void test_gfxDraw_CLRTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(), "test_gfxDraw_CLR");
    test_gfxRunSuite(suite, "test_gfxDraw_CLR");
    CuSuiteFree(suite);
}

void test_gfxDraw_OtherPorterDuffTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(),
            "test_gfxDraw_OtherPorterDuff");
    test_gfxRunSuite(suite, "test_gfxDraw_OtherPorterDuff");
    CuSuiteFree(suite);
}

void test_gfxFillPerfTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(), "test_gfxFillPerf");
    test_gfxRunSuite(suite, "test_gfxFillPerf");
    CuSuiteFree(suite);
}

void test_gfxBlitPerfTest(void)
{
    CuSuite* suite;
    suite = CuSuiteNewCloneTest(getTestSuite_gfxDraw(), "test_gfxBlitPerf");
    test_gfxRunSuite(suite, "test_gfxBlitPerf");
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
