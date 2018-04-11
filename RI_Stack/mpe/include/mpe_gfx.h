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

#ifndef _MPE_DISPMGR_BINDINGS_H_
#define _MPE_DISPMGR_BINDINGS_H_

#include "mpe_sys.h"
#include "../mgr/include/mgrdef.h"
#include "../mgr/include/dispmgr.h"

#define mpe_dispmgr_ftable ((mpe_disp_ftable_t*)(FTABLE[MPE_MGR_TYPE_DISP]))

#define mpe_dispInit (mpe_dispmgr_ftable->mpe_disp_init_ptr)

/* UIEvent */
#define mpe_gfxWaitNextEvent (mpe_dispmgr_ftable->mpe_gfxWaitNextEvent_ptr)
#define mpe_gfxGeneratePlatformKeyEvent (mpe_dispmgr_ftable->mpe_gfxGeneratePlatformKeyEvent_ptr)

/* Context */
#define mpe_gfxContextNew (mpe_dispmgr_ftable->mpe_gfxContextNew_ptr)
#define mpe_gfxContextCreate (mpe_dispmgr_ftable->mpe_gfxContextCreate_ptr)
#define mpe_gfxContextDelete (mpe_dispmgr_ftable->mpe_gfxContextDelete_ptr)
#define mpe_gfxGetSurface (mpe_dispmgr_ftable->mpe_gfxGetSurface_ptr)
#define mpe_gfxGetColor (mpe_dispmgr_ftable->mpe_gfxGetColor_ptr)
#define mpe_gfxSetColor (mpe_dispmgr_ftable->mpe_gfxSetColor_ptr)
#define mpe_gfxGetFont (mpe_dispmgr_ftable->mpe_gfxGetFont_ptr)
#define mpe_gfxSetFont (mpe_dispmgr_ftable->mpe_gfxSetFont_ptr)
#define mpe_gfxGetPaintMode (mpe_dispmgr_ftable->mpe_gfxGetPaintMode_ptr)
#define mpe_gfxSetPaintMode (mpe_dispmgr_ftable->mpe_gfxSetPaintMode_ptr)
#define mpe_gfxGetOrigin (mpe_dispmgr_ftable->mpe_gfxGetOrigin_ptr)
#define mpe_gfxSetOrigin (mpe_dispmgr_ftable->mpe_gfxSetOrigin_ptr)
#define mpe_gfxGetClipRect (mpe_dispmgr_ftable->mpe_gfxGetClipRect_ptr)
#define mpe_gfxSetClipRect (mpe_dispmgr_ftable->mpe_gfxSetClipRect_ptr)
/* Draw */
#define mpe_gfxDrawLine (mpe_dispmgr_ftable->mpe_gfxDrawLine_ptr)
#define mpe_gfxDrawRect (mpe_dispmgr_ftable->mpe_gfxDrawRect_ptr)
#define mpe_gfxFillRect (mpe_dispmgr_ftable->mpe_gfxFillRect_ptr)
#define mpe_gfxClearRect (mpe_dispmgr_ftable->mpe_gfxClearRect_ptr)
#define mpe_gfxDrawEllipse (mpe_dispmgr_ftable->mpe_gfxDrawEllipse_ptr)
#define mpe_gfxFillEllipse (mpe_dispmgr_ftable->mpe_gfxFillEllipse_ptr)
#define mpe_gfxDrawRoundRect (mpe_dispmgr_ftable->mpe_gfxDrawRoundRect_ptr)
#define mpe_gfxFillRoundRect (mpe_dispmgr_ftable->mpe_gfxFillRoundRect_ptr)
#define mpe_gfxDrawArc (mpe_dispmgr_ftable->mpe_gfxDrawArc_ptr)
#define mpe_gfxFillArc (mpe_dispmgr_ftable->mpe_gfxFillArc_ptr)
#define mpe_gfxDrawPolyline (mpe_dispmgr_ftable->mpe_gfxDrawPolyline_ptr)
#define mpe_gfxDrawPolygon (mpe_dispmgr_ftable->mpe_gfxDrawPolygon_ptr)
#define mpe_gfxFillPolygon (mpe_dispmgr_ftable->mpe_gfxFillPolygon_ptr)
#define mpe_gfxBitBlt (mpe_dispmgr_ftable->mpe_gfxBitBlt_ptr)
#define mpe_gfxStretchBlt (mpe_dispmgr_ftable->mpe_gfxStretchBlt_ptr)
#define mpe_gfxDrawString (mpe_dispmgr_ftable->mpe_gfxDrawString_ptr)
#define mpe_gfxDrawString16 (mpe_dispmgr_ftable->mpe_gfxDrawString16_ptr)
/* Surface */
#define mpe_gfxSurfaceNew (mpe_dispmgr_ftable->mpe_gfxSurfaceNew_ptr)
#define mpe_gfxSurfaceCreate (mpe_dispmgr_ftable->mpe_gfxSurfaceCreate_ptr)
#define mpe_gfxSurfaceDelete (mpe_dispmgr_ftable->mpe_gfxSurfaceDelete_ptr)
#define mpe_gfxSurfaceGetInfo (mpe_dispmgr_ftable->mpe_gfxSurfaceGetInfo_ptr)
/* Font */
#define mpe_gfxFontNew (mpe_dispmgr_ftable->mpe_gfxFontNew_ptr)
#define mpe_gfxFontDelete (mpe_dispmgr_ftable->mpe_gfxFontDelete_ptr)
#define mpe_gfxGetFontMetrics (mpe_dispmgr_ftable->mpe_gfxGetFontMetrics_ptr)
#define mpe_gfxGetStringWidth (mpe_dispmgr_ftable->mpe_gfxGetStringWidth_ptr)
#define mpe_gfxGetString16Width (mpe_dispmgr_ftable->mpe_gfxGetString16Width_ptr)
#define mpe_gfxGetCharWidth (mpe_dispmgr_ftable->mpe_gfxGetCharWidth_ptr)
#define mpe_gfxFontHasCode (mpe_dispmgr_ftable->mpe_gfxFontHasCode_ptr)
#define mpe_gfxFontGetList (mpe_dispmgr_ftable->mpe_gfxFontGetList_ptr)
/* Font Factory */
#define mpe_gfxFontFactoryNew (mpe_dispmgr_ftable->mpe_gfxFontFactoryNew_ptr)
#define mpe_gfxFontFactoryDelete (mpe_dispmgr_ftable->mpe_gfxFontFactoryDelete_ptr)
#define mpe_gfxFontFactoryAdd (mpe_dispmgr_ftable->mpe_gfxFontFactoryAdd_ptr)
/* Palette */
#define mpe_gfxPaletteNew (mpe_dispmgr_ftable->mpe_gfxPaletteNew_ptr)
#define mpe_gfxPaletteDelete (mpe_dispmgr_ftable->mpe_gfxPaletteDelete_ptr)
#define mpe_gfxPaletteSet (mpe_dispmgr_ftable->mpe_gfxPaletteSet_ptr)
#define mpe_gfxPaletteGet (mpe_dispmgr_ftable->mpe_gfxPaletteGet_ptr)
#define mpe_gfxPaletteMatch (mpe_dispmgr_ftable->mpe_gfxPaletteMatch_ptr)

#endif

