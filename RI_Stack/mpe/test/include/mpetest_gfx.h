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


/**
 * Functions defined in file mpetest_gfx.h will be re-defined here using macros.
 * This will make it easy to support MPE or MPEOS tests.  A simple
 * recompilation will be required to compile a set of tests for MPE or MPEOS.
 *
 * If #define TEST_MPEOS is defined, then tests will be for MPEOS, else MPE.
 */
#ifndef _MPETEST_GFX_H_
#define _MPETEST_GFX_H_ 1

#include <mpetest_dbg.h>

/* OS Dependant stuff goes here.
 */
#ifdef WIN32
#endif
#ifdef POWERTV
#endif

#ifdef TEST_MPEOS
# include <mpeos_dbg.h>
//# include "mpeos_sys.h"
# include "../mgr/include/mgrdef.h"
# include "../mgr/include/dispmgr.h"

# define MPETEST_GFX(x)  mpeos_ ## x

#else
# include <mpe_dbg.h>
# include "mpe_sys.h"
# include "../mgr/include/mgrdef.h"
# include "../mgr/include/dispmgr.h"

# define MPETEST_GFX(x)  mpe_ ## x

#endif /* TEST_MPEOS */

#define gfxWaitNextEvent  MPETEST_GFX(gfxWaitNextEvent)
#define gfxContextNew  MPETEST_GFX(gfxContextNew)
#define gfxContextCreate  MPETEST_GFX(gfxContextCreate)
#define gfxContextDelete  MPETEST_GFX(gfxContextDelete)
#define gfxGetSurface  MPETEST_GFX(gfxGetSurface)
#define gfxGetColor  MPETEST_GFX(gfxGetColor)
#define gfxSetColor  MPETEST_GFX(gfxSetColor)
#define gfxGetFont  MPETEST_GFX(gfxGetFont)
#define gfxSetFont  MPETEST_GFX(gfxSetFont)
#define gfxGetPaintMode  MPETEST_GFX(gfxGetPaintMode)
#define gfxSetPaintMode  MPETEST_GFX(gfxSetPaintMode)
#define gfxGetOrigin  MPETEST_GFX(gfxGetOrigin)
#define gfxSetOrigin  MPETEST_GFX(gfxSetOrigin)
#define gfxGetClipRect  MPETEST_GFX(gfxGetClipRect)
#define gfxSetClipRect  MPETEST_GFX(gfxSetClipRect)
#define gfxDrawLine  MPETEST_GFX(gfxDrawLine)
#define gfxDrawRect  MPETEST_GFX(gfxDrawRect)
#define gfxFillRect  MPETEST_GFX(gfxFillRect)
#define gfxClearRect  MPETEST_GFX(gfxClearRect)
#define gfxDrawEllipse  MPETEST_GFX(gfxDrawEllipse)
#define gfxFillEllipse  MPETEST_GFX(gfxFillEllipse)
#define gfxDrawRoundRect  MPETEST_GFX(gfxDrawRoundRect)
#define gfxFillRoundRect  MPETEST_GFX(gfxFillRoundRect)
#define gfxDrawArc  MPETEST_GFX(gfxDrawArc)
#define gfxFillArc  MPETEST_GFX(gfxFillArc)
#define gfxDrawPolyline  MPETEST_GFX(gfxDrawPolyline)
#define gfxDrawPolygon  MPETEST_GFX(gfxDrawPolygon)
#define gfxFillPolygon  MPETEST_GFX(gfxFillPolygon)
#define gfxBitBlt  MPETEST_GFX(gfxBitBlt)
#define gfxStretchBlt  MPETEST_GFX(gfxStretchBlt)
#define gfxDrawString  MPETEST_GFX(gfxDrawString)
#define gfxDrawString16  MPETEST_GFX(gfxDrawString16)
#define gfxSurfaceNew  MPETEST_GFX(gfxSurfaceNew)
#define gfxSurfaceCreate  MPETEST_GFX(gfxSurfaceCreate)
#define gfxSurfaceDelete  MPETEST_GFX(gfxSurfaceDelete)
#define gfxSurfaceGetInfo  MPETEST_GFX(gfxSurfaceGetInfo)
#define gfxFontNew  MPETEST_GFX(gfxFontNew)
#define gfxFontDelete  MPETEST_GFX(gfxFontDelete)
#define gfxGetFontMetrics  MPETEST_GFX(gfxGetFontMetrics)
#define gfxGetStringWidth  MPETEST_GFX(gfxGetStringWidth)
#define gfxGetString16Width  MPETEST_GFX(gfxGetString16Width)
#define gfxGetCharWidth  MPETEST_GFX(gfxGetCharWidth)
#define gfxFontHasCode  MPETEST_GFX(gfxFontHasCode)
#define gfxFontGetList  MPETEST_GFX(gfxFontGetList)
#define gfxFontFactoryNew  MPETEST_GFX(gfxFontFactoryNew)
#define gfxFontFactoryDelete  MPETEST_GFX(gfxFontFactoryDelete)
#define gfxFontFactoryAdd  MPETEST_GFX(gfxFontFactoryAdd)
#define gfxPaletteMatch MPETEST_GFX(gfxPaletteMatch)
#define gfxPaletteGet MPETEST_GFX(gfxPaletteGet)
#define gfxPaletteSet MPETEST_GFX(gfxPaletteSet)
#define gfxPaletteNew MPETEST_GFX(gfxPaletteNew)
#define gfxPaletteDelete MPETEST_GFX(gfxPaletteDelete)
#define gfxSurface MPETEST_GFX(gfxSurface)

#endif /* _MPETEST_GFX_H_ */ 
