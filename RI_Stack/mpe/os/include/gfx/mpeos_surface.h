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

#if !defined(_MPEOS_SURFACE_H)
#define _MPEOS_SURFACE_H

#include <mpe_types.h>		/* Resolve basic type references. */
#include <mpeos_sync.h>		/* for mutex */
#include "mpeos_gfx.h"
#include "os_gfx.h"

#ifdef __cplusplus
extern "C"
{
#endif

/**
 * Graphic surface internal representation.
 */
typedef struct mpeos_GfxSurface
{
    os_Mutex mutex; /**< surface is thread safe */
    int32_t width; /**< width of surface in pixels */
    int32_t height; /**< height of surface in pixels */
    int32_t bpl; /**< bytes per line */
    mpe_GfxBitDepth bpp; /**< bit depth (bits per pixel) */
    mpe_GfxColorFormat colorFormat; /**< color format */
    void* pixel_data; /**< pixel data */
    mpe_Bool primary; /**< true if on-screen surface */
    mpe_GfxPalette clut; /**< color palette used (if colorFormat == MPE_GFX_CLUT8) */
    os_GfxSurface os_data; /**< os-specific surface info */
} mpeos_GfxSurface;

/***
 * Graphics - Surface support API prototypes:
 */

/**
 * <i>mpeos_gfxSurfaceNew()</i>
 * Creates a new surface from a given surface description.
 *
 * @param desc   the surface description.
 * @param code	 address where the surface object is stored.
 *
 * @return		 MPE_GFX_ERROR_NOERR if success, a Gfx Error otherwise.
 */
mpe_Error
        mpeos_gfxSurfaceNew(mpe_GfxSurfaceInfo *desc, mpe_GfxSurface* surface);

/**
 * <i>mpeos_gfxSurfaceCreate()</i>
 * Creates a new surface from a given surface.
 * 
 * @param base   the surface from which a new surface is to be created.
 * @param surce	 address where the new surface object is stored.
 * @return		 MPE_GFX_ERROR_NOERR if success, a Gfx Error otherwise.
 */
mpe_Error mpeos_gfxSurfaceCreate(mpe_GfxSurface base, mpe_GfxSurface* surface);

/**
 *<i>mpeos_gfxSurfaceDelete</i>
 * Deletes a surface.
 * 
 * @param		surface handle to the surface to be deleted.
 * @return		MPE_GFX_ERROR_NOERR if success, a Gfx Error otherwise.
 */
mpe_Error mpeos_gfxSurfaceDelete(mpe_GfxSurface surface);

/**
 * <i>mpeos_gfxSurfaceGetInfo()</i>
 * Retreives information on a given surface.
 * 
 * @param surface handle to a surface.
 * @param info    address where the surface information are stored.
 * @return		 MPE_GFX_ERROR_NOERR if success, a Gfx Error otherwise.
 */
mpe_Error mpeos_gfxSurfaceGetInfo(mpe_GfxSurface surface,
        mpe_GfxSurfaceInfo *info);

/**
 * Creates a new color palette capable of holding the specified number of
 * colors.  
 *
 * @param nColors the desired size of the new palette in number of colors
 * @param palette address where the new palette handle will be stored
 * @return        MPE_GFX_ERROR_NOERR if success; an error is returned otherwise
 */
mpe_Error mpeos_gfxPaletteNew(int nColors, mpe_GfxPalette* palette);

/**
 * Deletes a previously created color palette.
 *
 * @param palette the color palette to delete
 * @return        MPE_GFX_ERROR_NOERR if success; an error is returned otherwise
 */
mpe_Error mpeos_gfxPaletteDelete(mpe_GfxPalette palette);

/**
 * Writes color entries to the given color palette.
 *
 * @param palette the palette to update with new color entries
 * @param colors pointer to an array of new color entries
 * @param nColors the number of color entries in <i>colors</i> to copy into
 *    the palette
 * @param offset the offset of the initial color entry within the 
 *    palette to start copying to
 * @param return MPE_GFX_ERROR_NOERR if success; an error is returned otherwise
 */
mpe_Error mpeos_gfxPaletteSet(mpe_GfxPalette palette, mpe_GfxColor *colors,
        int nColors, int offset);

/**
 * Retrieves color entries from the given color palette.
 *
 * @param palette the palette to read color entries from
 * @param nColors the number of color entries to be copied from the palette
 *    into the <i>colors</i> array
 * @param offset the offset of the initial color entry to copy into
 *    the <i>colors</i> array
 * @param colors pointer to an array that palette color entries should be 
 *    copied into
 * @param return MPE_GFX_ERROR_NOERR if success; an error is returned otherwise
 */
mpe_Error mpeos_gfxPaletteGet(mpe_GfxPalette palette, int nColors, int offset,
        mpe_GfxColor *colors);

/**
 * Queries the palette for the closest match for the given color.
 * The match is returned as an index into the color palette.
 *
 * @param palette the color palette to query
 * @param color the color for which a matching index is desired
 * @param index address where the color index is to be written
 * @param return MPE_GFX_ERROR_NOERR if success; an error is returned otherwise
 */
mpe_Error mpeos_gfxPaletteMatch(mpe_GfxPalette palette, mpe_GfxColor color,
        int *index);

/**
 * <i>mpeos_gfxSurface()</i>
 * Creates a new <code>mpeos_GfxSurface</code> surface from a given 
 * <code>mpe_GfxSurfaceInfo</code> surface description.
 * This function calls directfb API.
 *
 * @param desc    the surface description.
 * @param primary set to true if a screen surface is created.
 * 
 * @return        the address of the new surface or NULL if the operation failed.
 */
mpeos_GfxSurface* mpeos_gfxSurface(mpe_GfxSurfaceInfo *desc, mpe_Bool primary);

/**
 * <i>gfxSurfaceResize()</i>
 * Resizes a mpeos_GfxSurface surface to the new dimensions specified
 * Memory re-allocation takes place at the DirectFB level.  
 *
 * NOTES:
 *   This function calls directfb API.
 *   This function creates and deletes the main screen surface. If this approach
 *   is deemed innefficient then the main surface can be sized to the largest possible
 *   config in the mpeos_gfxCreateDefaultScreen method and simply have the dimensions
 *   updated here.  The current approach leaves open the possibility of changing other
 *   surface attributes besides the dimensions.
 *
 * @param s       the surface to resize
 * @param width   the surface width
 * @param height  the surface height
 * 
 * @return        the address of the updated surface or NULL if the operation failed.
 */
mpeos_GfxSurface* gfxSurfaceResize(mpeos_GfxSurface *s, int32_t width,
        int32_t height);

#ifdef __cplusplus
}
#endif

#endif /* _MPEOS_SURFACE_H */

