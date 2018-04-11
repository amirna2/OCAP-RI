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

#ifndef _TEST_DISP_H_
#define _TEST_DISP_H_ 1

#include <mpeTest.h>
#include <mpetest_file.h>
#include <mpetest_gfx.h>
#include <mpetest_disp.h>
#include <mpetest_dbg.h>
#include <mgrdef.h>
#include <dispmgr.h>
#include <time.h>
#include <gfx/mpeos_screen.h>

#ifdef TEST_MPEOS
# include <mpeos_dbg.h>
# include <mpeos_disp.h>
# include <gfx/mpeos_context.h>
# include <gfx/mpeos_surface.h>
# include <gfx/mpeos_draw.h>
# include <gfx/mpeos_font.h>
# include <gfx/mpeos_uievent.h>
# define GFX_INIT(err) do { err = mpeos_gfxCreateDefaultScreen(); } while(0)
#else
# include <mpe_gfx.h>
# include <mpe_disp.h>
# include <mpe_file.h>
# include <mpe_os.h>
# include <mpe_dbg.h>
# include "mpe_sys.h"
// # define MPE(x) mpe_ ## x
# define GFX_INIT(err) do { err = MPE_SUCCESS; } while(0)
#endif /* TEST_GFX_MPEOS */

extern char *dialogFont;
extern char *ameliaFont;
extern char *dialoginputFont;
extern char *monospacedFont;
extern char *sansserifFont;
extern char *serif;
extern char *tiresiasFont;

extern void test_gfxRunSuite(CuSuite* suite, char* name);
/**
 * Asserts that the given operation was successful.
 */
#define ASSERT_SUCCESS(x, parms)                          \
	CuAssert(tc, #x " failed", (x parms) == MPE_SUCCESS);

#define ASSERT(x)                                         \
	CuAssert(tc, #x " failed", result == MPE_SUCCESS);
#define ASSERTFAIL(x)                                     \
	CuAssert(tc, #x " failed", result != MPE_SUCCESS);

#ifdef WIN32
/*# define USER_OK(msg,title) MessageBox(0, msg, title, MB_OK)*/
/*# define USER_YESNO(msg,title,ok) do { ok = (MessageBox(0, msg, title, MB_YESNO) == IDYES); } while(0)*/
#else /* not WIN32 */
/*# error USER_OK/USER_YESNO not defined for this platform!*/
# define USER_OK(msg,title)                                             \
do {                                                                    \
    int c;                                                              \
    printf("=========== %s ===========\n%s [hit return]", title, msg);  \
    fflush(stdout);                                                     \
    sscanf("%c", &c);                                                   \
} while(0)
# define USER_YESNO(msg,title)                                      \
do {                                                                \
    int c;                                                          \
    printf("=========== %s ===========\n%s [Y|N]", title, msg);     \
    fflush(stdout);                                                 \
    sscanf("%c", &c);                                               \
    ok = c == 'y' || c == 'Y';                                      \
} while(0)
#endif /* WIN32 */

#define CuAssertPointEquals(tc, ms, ex, ac)         \
do {                                                \
	CuAssertIntEquals_Msg(tc, ms " - x", ex.x, ac.x);    \
	CuAssertIntEquals_Msg(tc, ms " - y", ex.y, ac.y);    \
} while(0)
#define CuAssertRectangleEquals(tc, ms, ex, ac)                    \
do {                                                               \
	CuAssertIntEquals_Msg(tc, ms " - x", ex.x, ac.x);                  \
	CuAssertIntEquals_Msg(tc, ms " - y", ex.y, ac.y);                  \
	CuAssertIntEquals_Msg(tc, ms " - width", ex.width, ac.width);      \
	CuAssertIntEquals_Msg(tc, ms " - height", ex.height, ac.height);   \
} while(0)
#define CuAssertColorEquals(tc, ms, ex, ac) \
	CuAssertIntEquals_Msg(tc, ms, (int)ex, (int)ac)
#define CuAssertFontEquals(tc, ms, ex, ac) \
	CuAssertIntEquals_Msg(tc, ms, (int)ex, (int)ac)
#define CuAssertModeEquals(tc, ms, ex, ac) \
	CuAssertIntEquals_Msg(tc, ms, ex, ac)

/**
 * Asserts that the given operation was successful.
 */
#define ASSERT_SUCCESS(x, parms)                          \
	CuAssert(tc, #x " failed", (x parms) == MPE_SUCCESS);

#define ASSERT(x)                                         \
	CuAssert(tc, #x " failed", result == MPE_SUCCESS);
#define ASSERTFAIL(x)                                     \
	CuAssert(tc, #x " failed", result != MPE_SUCCESS);

#ifdef WIN32
/*# define USER_OK(msg,title) MessageBox(0, msg, title, MB_OK)*/
/*# define USER_YESNO(msg,title,ok) do { ok = (MessageBox(0, msg, title, MB_YESNO) == IDYES); } while(0)*/
#else /* not WIN32 */
/*# error USER_OK/USER_YESNO not defined for this platform!*/
# define USER_OK(msg,title)                                             \
do {                                                                    \
    int c;                                                              \
    printf("=========== %s ===========\n%s [hit return]", title, msg);  \
    fflush(stdout);                                                     \
    sscanf("%c", &c);                                                   \
} while(0)
# define USER_YESNO(msg,title)                                      \
do {                                                                \
    int c;                                                          \
    printf("=========== %s ===========\n%s [Y|N]", title, msg);     \
    fflush(stdout);                                                 \
    sscanf("%c", &c);                                               \
    ok = c == 'y' || c == 'Y';                                      \
} while(0)
#endif /* WIN32 */

#define CuAssertPointEquals(tc, ms, ex, ac)         \
do {                                                \
	CuAssertIntEquals_Msg(tc, ms " - x", ex.x, ac.x);    \
	CuAssertIntEquals_Msg(tc, ms " - y", ex.y, ac.y);    \
} while(0)
#define CuAssertRectangleEquals(tc, ms, ex, ac)                    \
do {                                                               \
	CuAssertIntEquals_Msg(tc, ms " - x", ex.x, ac.x);                  \
	CuAssertIntEquals_Msg(tc, ms " - y", ex.y, ac.y);                  \
	CuAssertIntEquals_Msg(tc, ms " - width", ex.width, ac.width);      \
	CuAssertIntEquals_Msg(tc, ms " - height", ex.height, ac.height);   \
} while(0)
#define CuAssertColorEquals(tc, ms, ex, ac) \
	CuAssertIntEquals_Msg(tc, ms, (int)ex, (int)ac)
#define CuAssertFontEquals(tc, ms, ex, ac) \
	CuAssertIntEquals_Msg(tc, ms, (int)ex, (int)ac)
#define CuAssertModeEquals(tc, ms, ex, ac) \
	CuAssertIntEquals_Msg(tc, ms, ex, ac)

NATIVEEXPORT_API void test_mpeos_gfxRunAllTests(void);
NATIVEEXPORT_API void test_drawPolygon(void);
NATIVEEXPORT_API void test_gfxRunARGB2COLORTest(void);
NATIVEEXPORT_API void test_gfxRunRGB2COLORTest(void);
NATIVEEXPORT_API void test_gfxRunALPHAVALUETest(void);
NATIVEEXPORT_API void test_gfxRunREDVALUETest(void);
NATIVEEXPORT_API void test_gfxRunGREENVALUETest(void);
NATIVEEXPORT_API void test_gfxRunBLUEVALUETest(void);
NATIVEEXPORT_API void test_gfxRunContextTests(void);
NATIVEEXPORT_API void test_gfxRunContextNewTest(void);
NATIVEEXPORT_API void test_gfxRunContextCreateTest(void);
NATIVEEXPORT_API void test_gfxRunContextDeleteTest(void);
NATIVEEXPORT_API void test_gfxRunGetSurfaceTest(void);
NATIVEEXPORT_API void test_gfxRunSetGetColorTest(void);
NATIVEEXPORT_API void test_gfxRunSetGetFontTest(void);
NATIVEEXPORT_API void test_gfxRunGetFont_DefaultTest(void);
NATIVEEXPORT_API void test_gfxRunSetFont_ClearTest(void);
NATIVEEXPORT_API void test_gfxRunSetGetPaintModeTest(void);
NATIVEEXPORT_API void test_gfxRunSetGetOriginTest(void);
NATIVEEXPORT_API void test_gfxRunSetGetClipRectTest(void);
NATIVEEXPORT_API void test_gfxRunContext_DistinctStateTest(void);
NATIVEEXPORT_API void test_gfxRunDrawTests(void);
NATIVEEXPORT_API void test_gfxRunDrawSetupTest(void);
NATIVEEXPORT_API void test_gfxDraw_DistinctContextTest(void);
NATIVEEXPORT_API void test_gfxDrawLineTest(void);
NATIVEEXPORT_API void test_gfxDrawLine_offscreenTest(void);
NATIVEEXPORT_API void test_gfxDrawRectTest(void);
NATIVEEXPORT_API void test_gfxDrawRect_offscreenTest(void);
NATIVEEXPORT_API void test_gfxFillRectTest(void);
NATIVEEXPORT_API void test_gfxFillRect_offscreenTest(void);
NATIVEEXPORT_API void test_gfxClearRectTest(void);
NATIVEEXPORT_API void test_gfxClearRect_offscreenTest(void);
NATIVEEXPORT_API void test_gfxDrawEllipseTest(void);
NATIVEEXPORT_API void test_gfxDrawEllipse_offscreenTest(void);
NATIVEEXPORT_API void test_gfxFillEllipseTest(void);
NATIVEEXPORT_API void test_gfxFillEllipse_offscreenTest(void);
NATIVEEXPORT_API void test_gfxDrawRoundRectTest(void);
NATIVEEXPORT_API void test_gfxDrawRoundRect_offscreenTest(void);
NATIVEEXPORT_API void test_gfxFillRoundRectTest(void);
NATIVEEXPORT_API void test_gfxFillRoundRect_offscreenTest(void);
NATIVEEXPORT_API void test_gfxDrawArcTest(void);
NATIVEEXPORT_API void test_gfxDrawArc_offscreenTest(void);
NATIVEEXPORT_API void test_gfxFillArcTest(void);
NATIVEEXPORT_API void test_gfxFillArc_offscreenTest(void);
NATIVEEXPORT_API void test_gfxDrawPolylineTest(void);
NATIVEEXPORT_API void test_gfxDrawPolyline_offscreenTest(void);
NATIVEEXPORT_API void test_gfxDrawPolygonTest(void);
NATIVEEXPORT_API void test_gfxDrawPolygon_offscreenTest(void);
NATIVEEXPORT_API void test_gfxFillPolygonTest(void);
NATIVEEXPORT_API void test_gfxFillPolygon_offscreenTest(void);
NATIVEEXPORT_API void test_gfxBitBltTest(void);
NATIVEEXPORT_API void test_gfxBlit_SRCOVERTest(void);
NATIVEEXPORT_API void test_gfxBitBlt_offscreenTest(void);
NATIVEEXPORT_API void test_gfxBitBlt_diffTest(void);
NATIVEEXPORT_API void test_gfxStretchBltTest(void);
NATIVEEXPORT_API void test_gfxStretchBlt_offscreenTest(void);
NATIVEEXPORT_API void test_gfxStretchBlt_otherTest(void);
NATIVEEXPORT_API void test_gfxStretchBlt_diffTest(void);
NATIVEEXPORT_API void test_gfxDrawStringTest(void);
NATIVEEXPORT_API void test_gfxDrawString_offscreenTest(void);
NATIVEEXPORT_API void test_gfxDrawString16Test(void);
NATIVEEXPORT_API void test_gfxDrawString16_offscreenTest(void);
NATIVEEXPORT_API void test_gfxDraw_ClippedTest(void);
NATIVEEXPORT_API void test_gfxDraw_Clipped_offscreenTest(void);
NATIVEEXPORT_API void test_gfxDraw_TranslatedTest(void);
NATIVEEXPORT_API void test_gfxDraw_Translated_offscreenTest(void);
NATIVEEXPORT_API void test_gfxDraw_DistinctStateTest(void);
NATIVEEXPORT_API void test_gfxDraw_DistinctState_offscreenTest(void);
NATIVEEXPORT_API void test_gfxDraw_DistinctSurfaceTest(void);
NATIVEEXPORT_API void test_gfxDraw_XORTest(void);
NATIVEEXPORT_API void test_gfxDraw_SRCOVERTest(void);
NATIVEEXPORT_API void test_gfxDraw_SRCTest(void);
NATIVEEXPORT_API void test_gfxDraw_CLRTest(void);
NATIVEEXPORT_API void test_gfxDraw_OtherPorterDuffTest(void);
NATIVEEXPORT_API void test_gfxFillPerfTest(void);
NATIVEEXPORT_API void test_gfxBlitPerfTest(void);
NATIVEEXPORT_API void test_gfxRunFontTests(void);
NATIVEEXPORT_API void test_gfxRunFontNewTest(void);
NATIVEEXPORT_API void test_gfxRunFontDeleteTest(void);
NATIVEEXPORT_API void test_gfxRunGetFontMetricsTest(void);
NATIVEEXPORT_API void test_gfxRunGetStringWidthTest(void);
NATIVEEXPORT_API void test_gfxRunGetString16WidthTest(void);
NATIVEEXPORT_API void test_gfxRunGetCharWidthTest(void);
NATIVEEXPORT_API void test_gfxRunFontHasCodeTest(void);
NATIVEEXPORT_API void test_gfxRunSurfaceTests(void);
NATIVEEXPORT_API void test_gfxRunSurfaceNewTest(void);
NATIVEEXPORT_API void test_gfxRunSurfaceCreateTest(void);
NATIVEEXPORT_API void test_gfxRunSurfaceDeleteTest(void);
NATIVEEXPORT_API void test_gfxRunSurfaceGetInfoTest(void);
NATIVEEXPORT_API void test_gfx_FontNotFoundNullTest(void);
NATIVEEXPORT_API void test_gfx_FontMutiNewSameHandleTest(void);

NATIVEEXPORT_API void test_gfx_FontFactNewDeleteTest(void);
NATIVEEXPORT_API void test_gfx_FontFactNewDeleteNullTest(void);
NATIVEEXPORT_API void test_gfx_FontFactoryAddTest(void);
NATIVEEXPORT_API void test_gfx_FontGetListTest(void);
NATIVEEXPORT_API void test_gfx_FontFactNewDifferentInstanceTest(void);
NATIVEEXPORT_API void test_gfx_FontNewDeleteTest(void);
NATIVEEXPORT_API void test_gfx_FontNewDeleteValidFontFactoryTest(void);
NATIVEEXPORT_API void test_gfx_FontNewDeleteInValidFontFactoryTest(void);
NATIVEEXPORT_API void test_gfx_FontFactNeverSystemTest(void);
//NATIVEEXPORT_API void test_gfx_FontNotFoundNullTest(void);
NATIVEEXPORT_API void test_gfx_FontDefaultFFAlwaysReturnSomethingTest(void);
//NATIVEEXPORT_API void test_gfx_FontMutiNewSameHandleTest(void);
NATIVEEXPORT_API void test_gfx_FontFactoryVerifyDistinctivenessTest(void);
NATIVEEXPORT_API void test_gfx_FontFactorySameFontMultiTimesTest(void);

NATIVEEXPORT_API void test_gfx_SmokeTest(void);

#endif /* _TEST_DISP_H_ */
