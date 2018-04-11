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

#include <mpe_error.h>
#include <mpeos_dbg.h>
#include <mpeos_mem.h>
#include "os_gfx.h"         /* Resolve platform specific definitions */
#include "mpeos_screen.h"
#include "mpeos_surface.h"

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
mpeos_GfxSurface* mpeos_gfxSurface(mpe_GfxSurfaceInfo *desc, mpe_Bool primary)
{
    mpeos_GfxSurface *s = NULL;
    DFBSurfaceDescription dsc;

    IDirectFBSurface *fbsurf = NULL; /* The direct fb surface */
    IDirectFB *dfb = mpeos_gfxGetScreen()->osScr.dfb; /* The DirectFB interface */

    if (desc->format == MPE_GFX_CLUT8 && NULL == desc->clut)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxSurface() - ERROR - No CLUT specified\n");
        return NULL;
    }

    /* allocate the mpeos surface */
    if (MPE_SUCCESS != mpeos_memAllocP(MPE_MEM_GFX, sizeof(mpeos_GfxSurface),
            (void**) &s))
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxSurface() - ERROR - Failed to allocate mpeos_GfxSurface\n");
        return NULL;
    }

    /* allocate a mutex for thread safe surface */
    if (mpeos_mutexNew(&(s->mutex)) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxSurface() - ERROR - Failed to create Mutex\n");
        mpeos_memFreeP(MPE_MEM_GFX, s);
        return NULL;
    }

    /* create the directfb surface */
    /* attributes are capabilities, width, height and pixel format */

    dsc.flags = (DFBSurfaceDescriptionFlags)(DSDESC_CAPS | DSDESC_WIDTH
            | DSDESC_HEIGHT | DSDESC_PIXELFORMAT);

    dsc.width = desc->dim.width;
    dsc.height = desc->dim.height;

    /* default format is 32bpp with alpha in color value */
    switch (desc->format)
    {
    case MPE_GFX_ARGB8888:
        dsc.pixelformat = DSPF_ARGB;
        break;
    case MPE_GFX_RGB888:
        dsc.pixelformat = DSPF_RGB24;
        break;
    case MPE_GFX_RGB565:
        dsc.pixelformat = DSPF_RGB16;
        break;
    case MPE_GFX_ARGB1555:
        dsc.pixelformat = DSPF_ARGB1555;
        break;
    case MPE_GFX_CLUT8:
        dsc.pixelformat = DSPF_LUT8;
        break;
    default:
        dsc.pixelformat = DSPF_ARGB;
        break;
    }

    if (primary)
    {
        dsc.caps = (DFBSurfaceCapabilities)(DSCAPS_PRIMARY | DSCAPS_FLIPPING);
        if (dfb->CreateSurface(dfb, &dsc, &fbsurf) != DFB_OK)
        {
            mpeos_mutexDelete(s->mutex);
            mpeos_memFreeP(MPE_MEM_GFX, s);
            return NULL;
        }
    }
    else
    {
        dsc.caps = (DFBSurfaceCapabilities)(DSCAPS_VIDEOONLY);
        if (dfb->CreateSurface(dfb, &dsc, &fbsurf) != DFB_OK)
        {
            dsc.caps = (DFBSurfaceCapabilities)(DSCAPS_NONE);
            if (dfb->CreateSurface(dfb, &dsc, &fbsurf) != DFB_OK)
            {
                mpeos_mutexDelete(s->mutex);
                mpeos_memFreeP(MPE_MEM_GFX, s);
                return NULL;
            }
        }
    }

    if (desc->format == MPE_GFX_CLUT8 && (DFB_OK != fbsurf->SetPalette(fbsurf,
            (IDirectFBPalette*) desc->clut)))
    {
        fbsurf->Release(fbsurf);
        mpeos_mutexDelete(s->mutex);
        mpeos_memFreeP(MPE_MEM_GFX, s);
        return NULL;
    }

    /* init the mpeos surface */
    s->width = desc->dim.width;
    s->height = desc->dim.height;
    s->bpp = desc->bpp;
    s->colorFormat = desc->format;
    s->clut = desc->clut;
    /* retrieve the surface pixel data */
    if ((fbsurf->Lock(fbsurf, (DFBSurfaceLockFlags)(DSLF_READ | DSLF_WRITE),
            &(s->pixel_data), (int*) &(s->bpl))) != DFB_OK)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxSurface() - WARNING - Failed get frame buffer address\n");
        s->pixel_data = NULL;
    }

    fbsurf->Unlock(fbsurf);

    /* no separate alpha channel supported */
    s->primary = primary;

    /* store the surface pointer in the mpeos surface descriptor */
    s->os_data.os_s = fbsurf;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_GFX,
            "<<GFX>> mpeos_gfxSurface() - new surface %p (%d x %d)\n", s,
            s->width, s->height);

    return s;
}

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
        int32_t height)
{
    DFBSurfaceDescription dsc;

    IDirectFBSurface *fbsurf = NULL; /* The direct fb surface */
    IDirectFB *dfb = mpeos_gfxGetScreen()->osScr.dfb; /* The DirectFB interface */

    /* validate the input parameter */
    if (s == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_GFX,
                "<<GFX>> gfxSurfaceResize() - ERROR - Null surface provided\n");
        return NULL;
    }

    /* Only update the surface if the dimensions have changed */
    if (width == s->width && height == s->height)
    {
        return s;
    }

    /* create the directfb surface */
    /* attributes are capabilities, width, height and pixel format */
    dsc.flags = (DFBSurfaceDescriptionFlags)(DSDESC_CAPS | DSDESC_WIDTH
            | DSDESC_HEIGHT | DSDESC_PIXELFORMAT);

    dsc.width = width;
    dsc.height = height;

    /* default format is 32bpp with alpha in color value */
    switch (s->colorFormat)
    {
    case MPE_GFX_ARGB8888:
        dsc.pixelformat = DSPF_ARGB;
        break;
    case MPE_GFX_RGB888:
        dsc.pixelformat = DSPF_RGB24;
        break;
    case MPE_GFX_RGB565:
        dsc.pixelformat = DSPF_RGB16;
        break;
    case MPE_GFX_ARGB1555:
        dsc.pixelformat = DSPF_ARGB1555;
        break;
    case MPE_GFX_CLUT8:
        dsc.pixelformat = DSPF_LUT8;
        break;
    default:
        dsc.pixelformat = DSPF_ARGB;
        break;
    }

    // Create the new DirectFB surface with the correct size
    if (s->primary)
    {
        dsc.caps = (DFBSurfaceCapabilities)(DSCAPS_PRIMARY | DSCAPS_FLIPPING);
        if (dfb->CreateSurface(dfb, &dsc, &fbsurf) != DFB_OK)
        {
            return NULL;
        }
    }
    else
    {
        dsc.caps = (DFBSurfaceCapabilities)(DSCAPS_VIDEOONLY);
        if (dfb->CreateSurface(dfb, &dsc, &fbsurf) != DFB_OK)
        {
            dsc.caps = (DFBSurfaceCapabilities)(DSCAPS_NONE);
            if (dfb->CreateSurface(dfb, &dsc, &fbsurf) != DFB_OK)
            {
                return NULL;
            }
        }
    }

    if (s->colorFormat == MPE_GFX_CLUT8 && (DFB_OK != fbsurf->SetPalette(
            fbsurf, (IDirectFBPalette*) s->clut)))
    {
        fbsurf->Release(fbsurf);
        return NULL;
    }

    mpeos_mutexAcquire(s->mutex);

    /* Success, update the relevant members of the surface */
    s->width = width;
    s->height = height;

    /* retrieve the surface pixel data */
    if ((fbsurf->Lock(fbsurf, (DFBSurfaceLockFlags)(DSLF_READ | DSLF_WRITE),
            &(s->pixel_data), (int*) &(s->bpl))) != DFB_OK)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> gfxSurfaceResize() - WARNING - Failed get frame buffer address\n");
        s->pixel_data = NULL;
    }

    fbsurf->Unlock(fbsurf);

    /* free the old surface */
    s->os_data.os_s->Release(s->os_data.os_s);

    /* store the surface pointer in the mpeos surface descriptor */
    s->os_data.os_s = fbsurf;

    mpeos_mutexRelease(s->mutex);

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_GFX,
            "<<GFX>> gfxSurfaceResize() - new surface %p (%d x %d)\n", s,
            s->width, s->height);

    return s;
}

/******************************************************************************
 mpeos public functions
 ******************************************************************************/

mpe_Error mpeos_gfxSurfaceNew(mpe_GfxSurfaceInfo *desc, mpe_GfxSurface* surface)
{
    mpeos_GfxSurface *s = NULL;

    if (!desc)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxSurfaceNew() - ERROR - Param [*desc] Invalid\n");
        return MPE_GFX_ERROR_INVALID;
    }

    /* create a secondary surface */
    s = mpeos_gfxSurface(desc, false);
    if (!s)
    {
        return MPE_GFX_ERROR_OSERR;
    }

    *surface = (mpe_GfxSurface) s;
    return MPE_GFX_ERROR_NOERR;
}

mpe_Error mpeos_gfxSurfaceCreate(mpe_GfxSurface base, mpe_GfxSurface* surface)
{
    mpeos_GfxSurface *source = (mpeos_GfxSurface *) base;
    mpeos_GfxSurface *s = NULL;

    mpe_GfxSurfaceInfo desc;

    if (!base || !surface)
    {
        return MPE_GFX_ERROR_INVALID;
    }

    desc.bpp = source->bpp;

    desc.dim.width = source->width;
    desc.dim.height = source->height;
    desc.format = source->colorFormat;
    desc.clut = source->clut;
    desc.pixeldata = NULL;

    /* create a new surface based on the description from source surface */

    s = mpeos_gfxSurface(&desc, false);
    if (!s)
    {
        return MPE_GFX_ERROR_OSERR;
    }

    *surface = (mpe_GfxSurface) s;

    return MPE_GFX_ERROR_NOERR;
}

mpe_Error mpeos_gfxSurfaceDelete(mpe_GfxSurface surface)
{
    mpeos_GfxSurface *s = (mpeos_GfxSurface*) surface;

    if (s)
    {
        /* we cannot delete the screen primary surface */
        if (s == _screen.surf)
            return MPE_GFX_ERROR_NOERR;

        /* delete the mutex */
        if (s->mutex)
            mpeos_mutexDelete(s->mutex);

        /* release directfb surface */
        s->os_data.os_s->Release(s->os_data.os_s);

        mpeos_memFreeP(MPE_MEM_GFX, s);
    }
    else
    {
        return MPE_GFX_ERROR_INVALID;
    }
    return MPE_GFX_ERROR_NOERR;
}

mpe_Error mpeos_gfxSurfaceGetInfo(mpe_GfxSurface surface,
        mpe_GfxSurfaceInfo *info)
{
    mpeos_GfxSurface *s = (mpeos_GfxSurface*) surface;

    if (!surface || !info)
    {
        return MPE_GFX_ERROR_INVALID;
    }

    info->bpp = s->bpp;
    info->dim.width = s->width;
    info->dim.height = s->height;
    info->format = s->colorFormat;
    info->pixeldata = s->pixel_data;
    info->widthbytes = s->bpl;
    info->clut = s->clut;

    return MPE_GFX_ERROR_NOERR;
}

/**
 * Creates an IDirectFBPalette with the given number of colors.
 * The IDirectFBPalette is returned as the mpe_GfxPalette.
 */
mpe_Error mpeos_gfxPaletteNew(int nColors, mpe_GfxPalette* palette)
{
    IDirectFB *dfb;
    DFBPaletteDescription desc;
    IDirectFBPalette *dfbPalette;

    /* Check for valid parameters */
    if (nColors <= 0 || nColors > 256 || NULL == palette)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX, "<<GFX>> Invalid parameters\n");
        return MPE_GFX_ERROR_INVALID;
    }

    /* Create the DFB palette */
    dfb = mpeos_gfxGetScreen()->osScr.dfb;
    desc.flags = DPDESC_SIZE;
    desc.size = nColors;
    if (DFB_OK != dfb->CreatePalette(dfb, &desc, &dfbPalette))
    {
        return MPE_GFX_ERROR_OSERR;
    }

    *palette = (void*) dfbPalette;

    return MPE_GFX_ERROR_NOERR;
}

/**
 * Deletes a previously created IDirectFBPalette, currently cast
 * as an mpe_GfxPalette.
 */
mpe_Error mpeos_gfxPaletteDelete(mpe_GfxPalette palette)
{
    IDirectFBPalette *dfbPalette;

    /* Check for valid parameters */
    if (NULL == palette)
    {
        return MPE_GFX_ERROR_INVALID;
    }

    /* Delete the DFB palette */
    dfbPalette = (IDirectFBPalette*) palette;
    dfbPalette->Release(dfbPalette);

    return MPE_GFX_ERROR_NOERR;
}

/**
 * Copies the given mpe_GfxColor's (cast to DFBColors) into a previously
 * created IDirectFBPalette, currently cast as an mpe_GfxPalette.
 */
mpe_Error mpeos_gfxPaletteSet(mpe_GfxPalette palette, mpe_GfxColor *colors,
        int nColors, int offset)
{
    IDirectFBPalette *dfbPalette;
    DFBResult err;
    DFBColor *entries;
    int size, i;

    /* Check for valid parameters */
    if (NULL == (dfbPalette = (IDirectFBPalette*) palette) || NULL == colors
            || (DFB_OK
                    != dfbPalette->GetSize(dfbPalette, (unsigned int*) &size))
            || offset < 0 || nColors <= 0 || (nColors + offset > size))
    {
        return MPE_GFX_ERROR_INVALID;
    }

    /* Allocate space for entries. */
    if (MPE_SUCCESS != mpeos_memAllocP(MPE_MEM_TEMP,
            sizeof(DFBColor) * nColors, (void*) &entries))
    {
        return MPE_GFX_ERROR_NOMEM;
    }

    /* Copy colors into entries array */
    for (i = 0; i < nColors; ++i)
    {
        mpe_GfxColor color = colors[i];
        entries[i].a = (uint8_t) mpe_gfxGetAlpha(color);
        entries[i].r = (uint8_t) mpe_gfxGetRed(color);
        entries[i].g = (uint8_t) mpe_gfxGetGreen(color);
        entries[i].b = (uint8_t) mpe_gfxGetBlue(color);
    }

    /* Set Entries on DFB Palette */
    err = dfbPalette->SetEntries(dfbPalette, entries, nColors, offset);
    if (err != DFB_OK)
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>>: Failed to set DFB Palette entries [%d]\n", err);

    /* Release temporary entries array. */
    mpeos_memFreeP(MPE_MEM_TEMP, entries);

    return (err == DFB_OK) ? MPE_GFX_ERROR_NOERR : MPE_GFX_ERROR_OSERR;
}

/**
 * Copies DFBColors (to be cast to mpeH_GfxColor_t) from a previously
 * created IDirectFBPalette, currently cast as an mpe_GfxPalette.
 */
mpe_Error mpeos_gfxPaletteGet(mpe_GfxPalette palette, int nColors, int offset,
        mpe_GfxColor *colors)
{
    IDirectFBPalette *dfbPalette;
    DFBResult err;
    DFBColor *entries;
    int size;

    /* Check for valid parameters */
    if (NULL == (dfbPalette = (IDirectFBPalette*) palette) || NULL == colors
            || (DFB_OK
                    != dfbPalette->GetSize(dfbPalette, (unsigned int*) &size))
            || offset < 0 || nColors <= 0 || (nColors + offset > size))
    {
        return MPE_GFX_ERROR_INVALID;
    }

    /* Allocate space for entries. */
    if (MPE_SUCCESS != mpeos_memAllocP(MPE_MEM_TEMP,
            sizeof(DFBColor) * nColors, (void*) &entries))
    {
        return MPE_GFX_ERROR_NOMEM;
    }

    /* Get Entries from DFB Palette */
    if (DFB_OK == (err = dfbPalette->GetEntries(dfbPalette, entries, nColors,
            offset)))
    {
        /* Copy colors from entries array */
        int i;
        for (i = 0; i < nColors; ++i)
        {
            colors[i] = mpe_gfxArgbToColor(entries[i].a, entries[i].r,
                    entries[i].g, entries[i].b);
        }
    }

    /* Release temporary entries array. */
    mpeos_memFreeP(MPE_MEM_TEMP, entries);

    return (err == DFB_OK) ? MPE_GFX_ERROR_NOERR : MPE_GFX_ERROR_OSERR;
}

/**
 * Calls FindBestMatch on (IDirectFBPalette)palette and returns the
 * result in index.
 */
mpe_Error mpeos_gfxPaletteMatch(mpe_GfxPalette palette, mpe_GfxColor color,
        int *index)
{
    IDirectFBPalette *dfbPalette;

    /* Check for valid parameters */
    if (NULL == (dfbPalette = (IDirectFBPalette*) palette) || index == NULL)
    {
        return MPE_GFX_ERROR_INVALID;
    }

    /* Find closest match */
    return (DFB_OK == dfbPalette->FindBestMatch(dfbPalette,
            mpe_gfxGetRed(color), mpe_gfxGetGreen(color),
            mpe_gfxGetBlue(color), mpe_gfxGetAlpha(color),
            (unsigned int*) index)) ? MPE_GFX_ERROR_NOERR : MPE_GFX_ERROR_OSERR;
}
