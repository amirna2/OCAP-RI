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
 FILE:           mpeos_context.cpp
 OVERVIEW:       This module contains platform dependent implementation of a graphics context
 A context provides the context through which rendering operations take
 place. A context defines and manages the attributes that affect rendering.
 Synonyms include display context, graphics context, or device context.

 DEPENDENCIES:   mpeos_font.cpp          - to update the font count when font attributes is set
 mpeos_fontfactory.cpp   - to delete a font when its no longer referenced

 NOTES:          Like most of the source files in the MPE Graphics this module contains
 both private and public functions.

 Private functions under "Intenal functions" section are declare as static in
 the form of static mpe_Error gfxMyFunction(). They mostly contains
 the platform dependent code (ie. the mpe graphics private functions make calls
 to DirectFB API).
 The following functions will have to be updated if the MPE graphic needs
 to be ported to a different platform than DirectFB.

 gfxSetProperties()
 gfxSetColor()
 gfxSetPaintMode()
 gfxSetClipRect()
 gfxSetFont()

 Public functions are defined in the include file of this module.
 if the form of mpe_Error mpeos_gfxMyFunction().
 These functions are called by the JNI layer of the OCAP stack and should
 be (and remain) platform independent.

 */

#include <mpe_error.h>
#include <mpeos_dbg.h>
#include <mpeos_mem.h>

#include "os_gfx.h"
#include "mpeos_screen.h"
#include "mpeos_font.h"
#include "mpeos_context.h" /* include file of this module */

extern mpe_GfxFont sys_font;

/******************************************************************************
 *  Imported functions
 *
 *****************************************************************************/
extern int gfxFontUpdateCount(mpe_GfxFont font, int value);
extern void gfxFactoryRemoveFont(mpe_GfxFontFactory ff, mpeos_GfxFont *font);

/******************************************************************************
 *  Internal functions
 *
 *****************************************************************************/
static mpe_Error gfxSetProperties(mpeos_GfxContext *c, mpe_GfxColor color,
        mpe_GfxFont font, mpe_GfxPaintMode mode, uint32_t data,
        mpe_GfxRectangle *clip);

static mpe_Error gfxSetColor(mpeos_GfxContext *c, mpe_GfxColor color);
static mpe_Error gfxSetPaintMode(mpeos_GfxContext *c, mpe_GfxPaintMode mode,
        uint32_t data);
static mpe_Error gfxSetClipRect(mpeos_GfxContext *c, mpe_GfxRectangle *clip);
static mpe_Error gfxSetFont(mpeos_GfxContext *c, mpe_GfxFont font);

#define DRAWING_FLAGS (DFBSurfaceDrawingFlags)(DSDRAW_BLEND | DSDRAW_SRC_PREMULTIPLY | DSDRAW_DST_PREMULTIPLY | DSDRAW_DEMULTIPLY)
#define BLITTING_FLAGS (DFBSurfaceDrawingFlags)(DSBLIT_BLEND_ALPHACHANNEL | DSBLIT_SRC_PREMULTIPLY | DSBLIT_DST_PREMULTIPLY | DSBLIT_DEMULTIPLY)

/**
 * <i>gfxSetProperties()</i>
 * Set surface properties when a new context is created
 * This function makes calls to DirectFB API
 *
 * @param c     Pointer to a graphic context
 * @param color Drawing color
 * @param font  Font
 * @param mode  Paint mode
 * @param data  Paint mode data
 * @param clip  Clip rectangle
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise
 */
mpe_Error gfxSetProperties(mpeos_GfxContext *c, mpe_GfxColor color,
        mpe_GfxFont font, mpe_GfxPaintMode mode, uint32_t data,
        mpe_GfxRectangle *clip)
{
    mpe_Error err;

    if ((err = gfxSetClipRect(c, clip)) != MPE_GFX_ERROR_NOERR)
        return err;

    if ((err = gfxSetColor(c, color)) != MPE_GFX_ERROR_NOERR)
        return err;

    /* Drawings in the Surface will be blended according to color alpha value */
    c->os_ctx.subs->SetDrawingFlags(c->os_ctx.subs, DRAWING_FLAGS);
    /* blitting operation will use alpha value in color as an alpha constant */
    c->os_ctx.subs->SetBlittingFlags(c->os_ctx.subs, BLITTING_FLAGS);

    if ((err = gfxSetPaintMode(c, mode, data)) != MPE_GFX_ERROR_NOERR)
        return err;

    if ((err = gfxSetFont(c, font)) != MPE_GFX_ERROR_NOERR)
        return err;

    return MPE_GFX_ERROR_NOERR;

}

/**
 * Utility function used to set a color on a DFB Surface.
 * Adjusts for CLUT-based colors if necessary.
 *
 * @param s the surface to modify
 * @param color the MPE color to use
 */
static DFBResult dfbSetColor(IDirectFBSurface* s, mpe_GfxColor color)
{
    DFBResult res;
    DFBSurfacePixelFormat format;
    IDirectFBPalette* p = NULL;
    int index = 0;

    /* Set ColorIndex if CLUT */
    if (DFB_OK == s->GetPixelFormat(s, &format) && format == DSPF_LUT8
            && DFB_OK == s->GetPalette(s, &p) && p != NULL && DFB_OK
            == p->FindBestMatch(p, (uint8_t) mpe_gfxGetRed(color),
                    (uint8_t) mpe_gfxGetGreen(color), (uint8_t) mpe_gfxGetBlue(
                            color), (uint8_t) mpe_gfxGetAlpha(color),
                    (unsigned int*) &index))
    {
        DFBColor dfbColor;

        res = s->SetColorIndex(s, index);

        /* Lookup matched color so we know actual color */
        if (DFB_OK == p->GetEntries(p, &dfbColor, 1, index))
        {
        }
    }
    /* Set Color if non-CLUT */
    else
    {
        res = s->SetColor(s, (uint8_t) mpe_gfxGetRed(color),
                (uint8_t) mpe_gfxGetGreen(color), (uint8_t) mpe_gfxGetBlue(
                        color), (uint8_t) mpe_gfxGetAlpha(color));
    }

    return res;
}

/**
 * <i>gfxSetColor()</i>
 * Set the drawing color in a given garphic context.
 * This function makes calls to DirectFB API
 *
 * @param c     Pointer to the graphic context to update
 * @param color A drawing color
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise
 */
mpe_Error gfxSetColor(mpeos_GfxContext *c, mpe_GfxColor color)
{
    IDirectFBSurface* s = c->os_ctx.subs;

    if (DFB_OK != dfbSetColor(s, color))
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> gfxSetColor() - ERROR - call to DirectFB::SetColor() failed\n");
        return MPE_GFX_ERROR_OSERR;
    }
    c->color = color;

    return MPE_GFX_ERROR_NOERR;
}

/**
 * <i>gfxSetPaintMode()</i>
 * Set drawing mode in a given graphic context
 * This function makes calls to DirectFB API
 *
 * @param c     Pointer to the graphic context to update
 * @param mode  Painting mode
 * @param data  data associated with the paint mode: for XOR mode data is a color value
 *              for the other mode data is the constant alpha value
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise
 */
mpe_Error gfxSetPaintMode(mpeos_GfxContext *c, mpe_GfxPaintMode mode,
        uint32_t data)
{
    DFBResult res;
    IDirectFBSurface* s = c->os_ctx.subs;
    DFBSurfacePorterDuffRule rule;
    uint32_t xordata;
    uint8_t alphaConst = (uint8_t)(data);

    //MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_GFX, "gfxSetPaintMode() - ALPHA CONSTANT = %d\n",alphaConst);

    switch (mode)
    {
    case MPE_GFX_XOR:
        /* set drawing and blitting flags to XOR */
        res = s->SetDrawingFlags(s, (DFBSurfaceDrawingFlags)(DSDRAW_XOR));
        res = s->SetBlittingFlags(s, (DFBSurfaceBlittingFlags)(DSBLIT_XOR));

        s->SetPorterDuff(s, DSPD_XOR);
        if (res != DFB_OK)
            return MPE_GFX_ERROR_OSERR;

        /* mix current color with XOR color */
        /* mask off alpha */
        xordata = (data & mpe_gfxArgbToColor(0, 0xFF, 0xFF, 0xFF)) ^ c->color;

        res = dfbSetColor(s, xordata);
        if (res != DFB_OK)
            return MPE_GFX_ERROR_OSERR;

        c->paintmode = mode;
        c->modedata = data;
        return MPE_GFX_ERROR_NOERR;

    case MPE_GFX_SRC:
        rule = DSPD_SRC;
        break;
    case MPE_GFX_SRCOVER:
        rule = DSPD_SRC_OVER;
        break;
    case MPE_GFX_CLR:
        rule = DSPD_CLEAR;
        break;
    case MPE_GFX_DSTOVER:
        rule = DSPD_DST_OVER;
        break;
    case MPE_GFX_SRCIN:
        rule = DSPD_SRC_IN;
        break;
    case MPE_GFX_DSTIN:
        rule = DSPD_DST_IN;
        break;
    case MPE_GFX_SRCOUT:
        rule = DSPD_SRC_OUT;
        break;
    case MPE_GFX_DSTOUT:
        rule = DSPD_DST_OUT;
        break;

    case MPE_GFX_DST:
        s->SetDrawingFlags(s, (DFBSurfaceDrawingFlags)(DSDRAW_BLEND
                | DSDRAW_SRC_PREMULTIPLY));
        (void) gfxSetColor(c, c->color);
        c->paintmode = mode;
        c->modedata = data;
        return MPE_GFX_ERROR_NOERR;

    default:
        return MPE_GFX_ERROR_NOT_SUPPORTED;
    }

    /* reset color and drawing mode */
    s->SetDrawingFlags(s, DRAWING_FLAGS);
    s->SetBlittingFlags(s, BLITTING_FLAGS);
    s->SetAlphaConstant(s, alphaConst);
    (void) gfxSetColor(c, c->color);

    res = s->SetPorterDuff(s, rule);

    if (res != DFB_OK)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> gfxSetPaintMode() - ERROR - call to DirectFB::gfxSetPaintMode()\n");
        return MPE_GFX_ERROR_OSERR;
    }

    c->paintmode = mode;
    c->modedata = data;

    return res;
}

/**
 * <i>gfxSetClipRect()</i>
 * Set clip rectangle in a given graphic context
 * This function makes calls to DirecFB API
 *
 * @param c    Pointer to the graphic context to update
 * @param clip A clip rectangle
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise
 */
mpe_Error gfxSetClipRect(mpeos_GfxContext *c, mpe_GfxRectangle *clip)
{
    DFBResult res;
    DFBRegion reg;
    IDirectFBSurface* s = c->os_ctx.subs;

    reg.x1 = clip->x;
    reg.y1 = clip->y;
    reg.x2 = (reg.x1 + clip->width) - 1;
    reg.y2 = (reg.y1 + clip->height) - 1;

    res = s->SetClip(s, &reg);

    if (res != DFB_OK && res != DFB_INVAREA && res != DFB_INVARG)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> gfxSetClipRect() - ERROR - call to DirectFB::SetClip()\n");
        return MPE_GFX_ERROR_OSERR;
    }

    c->cliprect = *clip;

    return MPE_GFX_ERROR_NOERR;
}

/**
 * <i>gfxSetFont()</i>
 * Set the font ina given graphic context
 * This function makes calls to DirectFB API
 *
 * @param c     Pointer to the graphic context to update
 * @param font  A handle to a font
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise
 */
mpe_Error gfxSetFont(mpeos_GfxContext *c, mpe_GfxFont font)
{
    DFBResult res;
    IDirectFBSurface* s = c->os_ctx.subs;

    mpeos_GfxFont *fnt = (mpeos_GfxFont*) (font);

    mpe_Error err = MPE_GFX_ERROR_NOERR;

    if (fnt == NULL)
    {
        return MPE_GFX_ERROR_INVALID;
    }

    res = s->SetFont(s, fnt->osf.dfb_fnt);

    if (res != DFB_OK)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> gfxSetFont() - ERROR - call to DirectFB::SetFont()\n");
        err = MPE_GFX_ERROR_OSERR;
    }
    if (c->font)
        (void) gfxFontUpdateCount(c->font, -1);
    if (font)
        (void) gfxFontUpdateCount(font, 1);
    c->font = font;

    return err;
}

/******************************************************************************
 *  Public functions
 *
 *****************************************************************************/

mpe_Error mpeos_gfxContextNew(mpe_GfxSurface surface, mpe_GfxContext *ctx)
{
    mpe_Error err;
    DFBRectangle rect;
    mpeos_GfxContext *c = NULL;
    mpeos_GfxSurface *s = (mpeos_GfxSurface*) surface;
    DFBResult res;

    /* check parameters */
    if (!s || !ctx)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxContextNew() - ERROR - Invalid Parameter(s)\n");
        return MPE_GFX_ERROR_INVALID;
    }

    if (MPE_SUCCESS != (err = mpeos_memAllocP(MPE_MEM_GFX, sizeof *c,
            (void**) &c)))
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxContextNew() - ERROR - memAllocP() failed\n");
        return err;
    }

    memset(c, '\0', sizeof(*c));

    // we dont want the surface to be resized in the middle of the following, so get the mutex
    mpeos_mutexAcquire(s->mutex);

    c->cliprect.width = s->width;
    c->cliprect.height = s->height;

    c->surf = (mpeos_GfxSurface*) s;

    rect.x = 0;
    rect.y = 0;
    rect.w = s->width;
    rect.h = s->height;

    /* get a sub surface from DirectFB surface */
    res = c->surf->os_data.os_s->GetSubSurface(c->surf->os_data.os_s, &rect,
            &(c->os_ctx.subs));
    if (res != DFB_OK)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxContextNew() - ERROR - call to DirectFB::GetSubSurface(): %d\n", res);
        mpeos_memFreeP(MPE_MEM_GFX, (void*) c);
        mpeos_mutexRelease(s->mutex);
        return MPE_GFX_ERROR_OSERR;
    }
    mpeos_mutexRelease(s->mutex);

    /* set default paintmode */
    c->paintmode = MPE_GFX_SRCOVER;
    /* alpha constant - fully opaque */
    c->modedata = 0xff;

    /* default system font */
    //c->font= (mpe_GfxFont)&_sysfont;
    c->font = sys_font;
    /* default color RGB(255,0,0,0); Black - fully opaque */
    c->color = (mpe_GfxColor) 0xff000000;

    *ctx = (mpe_GfxContext) c;

    /* set context to OCAP defaults */
    return gfxSetProperties(c, c->color, c->font, c->paintmode, c->modedata,
            &(c->cliprect));
}

mpe_Error mpeos_gfxContextCreate(mpe_GfxContext base, mpe_GfxContext *ctx)
{
    mpeos_GfxContext *c = NULL;
    mpeos_GfxContext *base_ctx = (mpeos_GfxContext*) base;
    mpe_Error err;
    DFBRectangle rect;
    DFBResult res;

    if (!ctx || !base)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxContextCreate() - ERROR - Invalid Parameter(s)\n");
        return MPE_GFX_ERROR_INVALID;
    }

    if (MPE_SUCCESS != (err = mpeos_memAllocP(MPE_MEM_GFX, sizeof(*c),
            (void**) &c)))
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxContextCreate() - ERROR - mpeos_memAllocP() failed\n");
        return err;
    }

    /* initialize the new context with the base context values */
    memcpy(c, base_ctx, sizeof(mpeos_GfxContext));

    /* get a new sub surface from DirectFB surface :
     the new context has to have a distinct surface to draw into */

    // we dont want the surface to be resized in the middle of the following, so get the mutex
    mpeos_mutexAcquire(c->surf->mutex);

    rect.x = 0;
    rect.y = 0;
    rect.w = c->surf->width;
    rect.h = c->surf->height;

    res = c->surf->os_data.os_s->GetSubSurface(c->surf->os_data.os_s, &rect,
            &(c->os_ctx.subs));
    if (res != DFB_OK)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxContextCreate() - ERROR - call to DirectFB::GetSubSurface(): %d\n", res);
        mpeos_memFreeP(MPE_MEM_GFX, (void*) c);
        mpeos_mutexRelease(c->surf->mutex);
        return MPE_GFX_ERROR_OSERR;
    }
    mpeos_mutexRelease(c->surf->mutex);

    /* return handle to the new context */
    *ctx = (mpe_GfxContext) c;

    /* set the attributes for the new context*/
    c->font = NULL; /* Ensure that font reference count is updated. */

    return gfxSetProperties(c, c->color, base_ctx->font, c->paintmode,
            c->modedata, &(c->cliprect));
}

mpe_Error mpeos_gfxContextDelete(mpe_GfxContext ctx)
{
    if (!ctx)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxContextDelete() - ERROR - Invalid Parameter\n");
        return MPE_GFX_ERROR_INVALID;
    }
    else
    {
        mpeos_GfxContext *c = (mpeos_GfxContext *) ctx;
        IDirectFBSurface *dfbs;

        if (c->font)
        {
            mpeos_GfxFont *font = (mpeos_GfxFont*) c->font;

            if (gfxFontUpdateCount(c->font, -1) == 0)
            { /* we were the last context using this font - we can remove it */
                MPEOS_LOG(
                        MPE_LOG_INFO,
                        MPE_MOD_GFX,
                        "<<GFX>> mpeos_gfxContextDelete() - Updated font 0x%p count \n",
                        c->font);
                gfxFactoryRemoveFont(font->ff, font);
            }
        }

        dfbs = c->os_ctx.subs;
        if (NULL != dfbs)
        {
            dfbs->Release(dfbs);
        }

        mpeos_memFreeP(MPE_MEM_GFX, (void*) c);
    }

    return MPE_GFX_ERROR_NOERR;
}

mpe_Error mpeos_gfxGetSurface(mpe_GfxContext ctx, mpe_GfxSurface *surface)
{
    if (!ctx || !surface)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxContextDelete() - ERROR - Invalid Parameter(s)\n");
        return MPE_GFX_ERROR_INVALID;
    }
    else
    {
        mpeos_GfxContext *c = (mpeos_GfxContext *) ctx;
        *surface = (mpe_GfxSurface) c->surf;
    }
    return MPE_GFX_ERROR_NOERR;
}

mpe_Error mpeos_gfxGetColor(mpe_GfxContext ctx, mpe_GfxColor *color)
{
    if (!ctx || !color)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxGetColor() - ERROR - Invalid Parameter(s)\n");
        return MPE_GFX_ERROR_INVALID;
    }
    else
    {
        mpeos_GfxContext *c = (mpeos_GfxContext *) ctx;
        *color = c->color;
    }
    return MPE_GFX_ERROR_NOERR;
}

mpe_Error mpeos_gfxSetColor(mpe_GfxContext ctx, mpe_GfxColor color)
{

    mpe_Error err = MPE_GFX_ERROR_NOERR;

    if (!ctx)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxSetColor() - ERROR - Invalid Parameter(s)\n");
        return MPE_GFX_ERROR_INVALID;
    }
    else
    {
        mpeos_GfxContext *c = (mpeos_GfxContext *) ctx;
        err = gfxSetColor(c, color);
    }
    return err;
}

mpe_Error mpeos_gfxGetFont(mpe_GfxContext ctx, mpe_GfxFont *font)
{
    if (!ctx || !font)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxGetFont() - ERROR - Invalid Parameter(s)\n");
        return MPE_GFX_ERROR_INVALID;
    }
    else
    {
        mpeos_GfxContext *c = (mpeos_GfxContext *) ctx;
        *font = c->font;
    }
    return MPE_GFX_ERROR_NOERR;
}

mpe_Error mpeos_gfxSetFont(mpe_GfxContext ctx, mpe_GfxFont font)
{
    mpe_Error err;
    if (!ctx)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxSetFont() - ERROR - Invalid Paramter\n");
        return MPE_GFX_ERROR_INVALID;
    }
    else
    {
        mpeos_GfxContext *c = (mpeos_GfxContext *) ctx;
        err = gfxSetFont(c, font);

    }
    return err;
}

mpe_Error mpeos_gfxGetPaintMode(mpe_GfxContext ctx, mpe_GfxPaintMode *mode,
        uint32_t *data)
{
    if (!ctx || !mode || !data)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxGetPaintMode() - ERROR - Invalid Paramter(s)\n");
        return MPE_GFX_ERROR_INVALID;
    }
    else
    {
        mpeos_GfxContext *c = (mpeos_GfxContext *) ctx;
        *mode = c->paintmode;
        *data = c->modedata;
    }
    return MPE_GFX_ERROR_NOERR;
}

mpe_Error mpeos_gfxSetPaintMode(mpe_GfxContext ctx, mpe_GfxPaintMode mode,
        uint32_t data)
{

    mpe_Error err;
    if (!ctx)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxSetPaintMode() - ERROR - Invalid Parameter(s)\n");
        return MPE_GFX_ERROR_INVALID;
    }
    else
    {
        mpeos_GfxContext *c = (mpeos_GfxContext *) ctx;
        err = gfxSetPaintMode(c, mode, data);
    }
    return err;
}

mpe_Error mpeos_gfxGetOrigin(mpe_GfxContext ctx, mpe_GfxPoint *point)
{
    if (!ctx || !point)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxGetOrigin() - ERROR - Invalid Parameter(s)\n");
        return MPE_GFX_ERROR_INVALID;
    }
    else
    {
        mpeos_GfxContext *c = (mpeos_GfxContext *) ctx;
        point->x = c->orig.x;
        point->y = c->orig.y;
    }
    return MPE_GFX_ERROR_NOERR;
}

mpe_Error mpeos_gfxSetOrigin(mpe_GfxContext ctx, int32_t x, int32_t y)
{
    if (!ctx)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxSetOrigin() - ERROR - Invalid Parameter\n");
        return MPE_GFX_ERROR_INVALID;
    }
    else
    {
        mpeos_GfxContext *c = (mpeos_GfxContext *) ctx;
        c->orig.x = x;
        c->orig.y = y;
    }
    return MPE_GFX_ERROR_NOERR;
}

mpe_Error mpeos_gfxGetClipRect(mpe_GfxContext ctx, mpe_GfxRectangle *rect)
{
    if (!ctx || !rect)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxGetClipRect() - ERROR - Invalid Parameter(s)\n");
        return MPE_GFX_ERROR_INVALID;
    }
    else
    {
        mpeos_GfxContext *c = (mpeos_GfxContext *) ctx;
        *rect = c->cliprect;
    }
    return MPE_GFX_ERROR_NOERR;
}

mpe_Error mpeos_gfxSetClipRect(mpe_GfxContext ctx, mpe_GfxRectangle *rect)
{

    mpe_Error err;
    if (!ctx || !rect)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxSetClipRect() - ERROR - Invalid Parameter(s)\n");
        return MPE_GFX_ERROR_INVALID;
    }
    else
    {
        mpeos_GfxContext *c = (mpeos_GfxContext *) ctx;
        err = gfxSetClipRect(c, rect);
    }
    return err;
}
