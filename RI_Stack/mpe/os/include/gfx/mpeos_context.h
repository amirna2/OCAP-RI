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

#if !defined(_MPEOS_CONTEXT_H)
#define _MPEOS_CONTEXT_H

#include <mpe_types.h>
#include <mpe_error.h>
#include "mpeos_gfx.h"
#include "mpeos_surface.h"

#ifdef __cplusplus
extern "C"
{
#endif

/**
 * <code>mpeos_GfxContext()</code> - Graphic context internal representation.
 * <ul>
 * <li> color      current drawing color.
 * <li> font       handle to the current font.
 * <li> orig       origin from drawing and clipping area.
 * <li> cliprect   current clipping rectangle.
 * <li> paintmode  current paint mode.
 * <li> modedata   data associated with the paintmode: for XOR this is a color and for Porter-Duff it is a constant alpha value.
 * <li> os_ctx     os specific definition of the graphic context
 * </ul>
 */
typedef struct mpeos_GfxContext
{
    mpe_GfxColor color;
    mpe_GfxFont font;
    mpe_GfxPoint orig;
    mpe_GfxRectangle cliprect;
    mpe_GfxPaintMode paintmode;
    uint32_t modedata;
    mpeos_GfxSurface *surf;
    os_GfxContext os_ctx;
} mpeos_GfxContext;

/***
 * Graphics - Context support API prototypes:
 */

/**
 * <i>mpeos_gfxContextNew()</i>
 * Allocates and initializes a new context for the given surface as a destination.
 * This function makes calls to DirectFB function <code>GetSubSurface()</code>
 *
 * @param surface The surface to be drawn to by the new context
 * @param ctx     Address where the context handle is to be stored
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed, 
 *         or MPE_GFX_ERROR_NOERR otherwise
 */
mpe_Error mpeos_gfxContextNew(mpe_GfxSurface surface, mpe_GfxContext *ctx);

/**
 * <i>mpeos_gfxContextCreate()</i>
 * Allocates and initializes a new context based on the given context.
 *
 * @param base    The context on which the new context is to be based
 * @param ctx     Address where the new context handle is to be stored
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed, 
 *         or MPE_GFX_ERROR_NOERR otherwise
 */
mpe_Error mpeos_gfxContextCreate(mpe_GfxContext base, mpe_GfxContext *ctx);

/**
 * <i>mpeos_gfxContextDelete()</i>
 * Deallocates the given context. If a font is been used by the context
 * the font count is updated and deleted if necessary.
 *
 * @param ctx     The context to be deleted
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed, 
 *         or MPE_GFX_ERROR_NOERR otherwise
 */
mpe_Error mpeos_gfxContextDelete(mpe_GfxContext ctx);

/**
 * <i>mpeos_gfxGetSurface()</i>
 * Returns the surface associated with this context.
 *
 * @param ctx The context to query
 * @param surface Address where the surface handle is to be stored
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed, 
 *         or MPE_GFX_ERROR_NOERR otherwise
 */
mpe_Error mpeos_gfxGetSurface(mpe_GfxContext ctx, mpe_GfxSurface *surface);

/**
 * <i>mpeos_gfxGetColor()</i>
 * Returns the color associated with this context.
 *
 * @param ctx The context to query
 * @param color Address where the color value is to be stored
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed, 
 *         or MPE_GFX_ERROR_NOERR otherwise
 */
mpe_Error mpeos_gfxGetColor(mpe_GfxContext ctx, mpe_GfxColor *color);

/**
 * <i>mpeos_gfxSetColor()</i>
 * Sets the color associated with the given context.
 *
 * @param ctx The context to query
 * @param color The context's new color
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed, 
 *         or MPE_GFX_ERROR_NOERR otherwise
 */
mpe_Error mpeos_gfxSetColor(mpe_GfxContext ctx, mpe_GfxColor color);

/**
 * mpeos_gfxGetFont()</i>
 * Returns the font associated with this context.
 *
 * @param ctx The context to query
 * @param font Address where the font handle is to be stored
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed, 
 *         or MPE_GFX_ERROR_NOERR otherwise
 */
mpe_Error mpeos_gfxGetFont(mpe_GfxContext ctx, mpe_GfxFont *font);

/**
 * <i>mpeos_gfxSetFont()</i>
 * Sets the font associated with the given context.
 *
 * @param ctx The context to query
 * @param font The context's new font
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed, 
 *         or MPE_GFX_ERROR_NOERR otherwise
 */
mpe_Error mpeos_gfxSetFont(mpe_GfxContext ctx, mpe_GfxFont font);

/**
 * <i>mpeos_gfxGetPaintMode()</i>
 * Returns the context's current paint mode and the associated data.
 *
 * @param ctx The context to query
 * @param mode Address where the paint mode is to be stored
 * @param data Address where mode-specific data is to be stored
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed, 
 *         or MPE_GFX_ERROR_NOERR otherwise
 */
mpe_Error mpeos_gfxGetPaintMode(mpe_GfxContext ctx, mpe_GfxPaintMode *mode,
        uint32_t *data);

/**
 * <i>mpeos_gfxSetPaint()</i>
 * Sets the paint mode associated with the given context.
 *
 * @param ctx The context to query
 * @param mode The context's new paint mode
 * @param data The paint mode-specific data; for <code>MPE_GFX_XOR</code> this is
 * the XOR color.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed, 
 *         or MPE_GFX_ERROR_NOERR otherwise
 */
mpe_Error mpeos_gfxSetPaintMode(mpe_GfxContext ctx, mpe_GfxPaintMode mode,
        uint32_t data);

/**
 * <i>mpeos_GetOrigin()</i>
 * Returns the translation origin (relative to the surface origin) associated 
 * with this context.
 *
 * @param ctx The context to query
 * @param point Address where the origin coordinate values are to be stored
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed, 
 *         or MPE_GFX_ERROR_NOERR otherwise
 */
mpe_Error mpeos_gfxGetOrigin(mpe_GfxContext ctx, mpe_GfxPoint *point);

/**
 * <i>mpeos_SetOrigin()</i>
 * Sets the translation origin point associated with the given context.
 *
 * @param ctx The context to query
 * @param x The context's new translation origin x-coordinate
 * @param y The context's new translation origin y-coordinate
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed, 
 *         or MPE_GFX_ERROR_NOERR otherwise
 */
mpe_Error mpeos_gfxSetOrigin(mpe_GfxContext ctx, int32_t x, int32_t y);

/**
 * <i>mpeos_gfxGetClipRect()</i>
 * Returns the clipping rectangle associated with this context.
 *
 * @param ctx The context to query
 * @param surface Address where the clipping rectangle values are to be stored
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed, 
 *         or MPE_GFX_ERROR_NOERR otherwise
 */
mpe_Error mpeos_gfxGetClipRect(mpe_GfxContext ctx, mpe_GfxRectangle *rect);

/**
 * <i>mpeos_gfxSetClipRect()</i>
 * Sets the clipping rectangle associated with the given context.
 *
 * @param ctx The context to query
 * @param rect The context's new clipping rectangle
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed, 
 *         or MPE_GFX_ERROR_NOERR otherwise
 */
mpe_Error mpeos_gfxSetClipRect(mpe_GfxContext ctx, mpe_GfxRectangle *rect);

#ifdef __cplusplus
}
#endif

#endif /* _MPEOS_CONTEXT_H */

