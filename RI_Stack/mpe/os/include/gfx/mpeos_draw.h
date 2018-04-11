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

#if !defined(_MPEOS_DRAW_H)
#define _MPEOS_DRAW_H

#include <mpe_types.h>		/* Resolve basic type references. */
#include <mpe_error.h>
#include "mpeos_gfx.h"		/* graphics public header */

#ifdef __cplusplus
extern "C"
{
#endif

/***
 * Graphics - Draw support API prototypes:
 */

mpe_Error mpeos_gfxDrawLine(mpe_GfxContext ctx, int32_t x1, int32_t y1,
        int32_t x2, int32_t y2);
mpe_Error mpeos_gfxDrawRect(mpe_GfxContext ctx, mpe_GfxRectangle *rect);
mpe_Error mpeos_gfxFillRect(mpe_GfxContext ctx, mpe_GfxRectangle *rect);
mpe_Error mpeos_gfxClearRect(mpe_GfxContext ctx, mpe_GfxRectangle *rect,
        mpe_GfxColor color);
mpe_Error mpeos_gfxDrawEllipse(mpe_GfxContext ctx, mpe_GfxRectangle *bounds);
mpe_Error mpeos_gfxFillEllipse(mpe_GfxContext ctx, mpe_GfxRectangle *bounds);
mpe_Error mpeos_gfxDrawRoundRect(mpe_GfxContext ctx, mpe_GfxRectangle *rect,
        int32_t arcWidth, int32_t arcHeight);
mpe_Error mpeos_gfxFillRoundRect(mpe_GfxContext ctx, mpe_GfxRectangle *rect,
        int32_t arcWidth, int32_t arcHeight);
mpe_Error mpeos_gfxDrawArc(mpe_GfxContext ctx, mpe_GfxRectangle *bounds,
        int32_t startAngle, int32_t endAngle);
mpe_Error mpeos_gfxFillArc(mpe_GfxContext ctx, mpe_GfxRectangle *bounds,
        int32_t startAngle, int32_t endAngle);
mpe_Error mpeos_gfxDrawPolyline(mpe_GfxContext context, int32_t* xCoords,
        int32_t* yCoords, int32_t nCoords);
mpe_Error mpeos_gfxDrawPolygon(mpe_GfxContext, int32_t* xCoords,
        int32_t* yCoords, int32_t nCoords);
mpe_Error mpeos_gfxFillPolygon(mpe_GfxContext, int32_t* xCoords,
        int32_t* yCoords, int32_t ynCoords);

mpe_Error mpeos_gfxBitBlt(mpe_GfxContext dest, mpe_GfxSurface source,
        int32_t dx, int32_t dy, mpe_GfxRectangle *srect);
mpe_Error mpeos_gfxStretchBlt(mpe_GfxContext dest, mpe_GfxSurface source,
        mpe_GfxRectangle *drect, mpe_GfxRectangle *srect);

mpe_Error mpeos_gfxDrawString(mpe_GfxContext ctx, int32_t x, int32_t y,
        const char* buf, int32_t len);
mpe_Error mpeos_gfxDrawString16(mpe_GfxContext ctx, int32_t x, int32_t y,
        const mpe_GfxWchar* buf, int32_t len);

#ifdef __cplusplus
}
#endif

#endif /* _MPEOS_DRAW_H */

