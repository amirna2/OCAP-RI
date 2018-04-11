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

#include <time.h>
#include <mpeos_dbg.h>
#include <mpeos_sync.h>
#include <mpeos_mem.h>
#include "os_gfx.h"
#include "mpeos_draw.h"
#include "mpeos_context.h"
#include <math.h>
#include <ConvertUTF.h>

#define GFX_LOCK(ctx) mpeos_mutexAcquire(((mpeos_GfxContext*)(ctx))->surf->mutex)
#define GFX_UNLOCK(ctx) mpeos_mutexRelease(((mpeos_GfxContext*)(ctx))->surf->mutex)

/**
 *  Internal functions definition
 */

static mpe_Error gfxDrawLine(mpeos_GfxContext *c, int32_t x1, int32_t y1,
        int32_t x2, int32_t y2);
static mpe_Error gfxRectangle(mpeos_GfxContext *c, mpe_GfxRectangle *rect,
        mpe_Bool filled);
static mpe_Error gfxEllipse(mpeos_GfxContext *c, mpe_GfxRectangle *bounds,
        mpe_Bool filled);
static mpe_Error gfxRoundRectangle(mpeos_GfxContext *c, mpe_GfxRectangle *rect,
        int32_t arcWidth, int32_t arcHeight, mpe_Bool filled);
static mpe_Error gfxArc(mpeos_GfxContext *c, mpe_GfxRectangle *bounds,
        int32_t startAngle, int32_t arcAngle, mpe_Bool filled);
static mpe_Error gfxPolyline(mpeos_GfxContext *c, int32_t *xCoords,
        int32_t *yCoords, int32_t nCoords);
static mpe_Error gfxPolygon(mpeos_GfxContext *c, int32_t *xCoords,
        int32_t *yCoords, int32_t nCoords, mpe_Bool filled);
static mpe_Error gfxBitBlt(mpeos_GfxContext *dest, mpeos_GfxSurface *source,
        int32_t dx, int32_t dy, mpe_GfxRectangle *srect);
static mpe_Error gfxStretchBlt(mpeos_GfxContext *dest,
        mpeos_GfxSurface *source, mpe_GfxRectangle *drect,
        mpe_GfxRectangle *srect);
static mpe_Error gfxDrawString(mpeos_GfxContext *c, int32_t x, int32_t y,
        const char *buf, int32_t len);
static mpe_Error gfxDrawString16(mpeos_GfxContext *c, int32_t x, int32_t y,
        const mpe_GfxWchar *buf, int32_t len);

/******************************************************************************
 * Internal functions
 *****************************************************************************/

/**
 * <i>gfxDrawLine()</i>
 * Draws a line with a given graphic context.
 * This function calls DirectFB API.
 *
 * @param c     Pointer to the graphic context
 * @param x1    X-coordinate of the first endpoint that defines the line.
 * @param y1    Y-coordinate of the first endpoint that defines the line.
 * @param x2    X-coordinate of the second endpoint that defines the line.
 * @param y2    Y-coordinate of the second endpoint that defines the line.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise
 */
mpe_Error gfxDrawLine(mpeos_GfxContext *c, int32_t x_1, int32_t y_1,
        int32_t x_2, int32_t y_2)
{
    DFBResult res;
    IDirectFBSurface* s = c->os_ctx.subs;

    /* paint mode is destination - don't draw anything */
    if (c->paintmode == MPE_GFX_DST)
        return MPE_GFX_ERROR_NOERR;

    res = s->DrawLine(s, x_1, y_1, x_2, y_2);

    if (res != DFB_OK)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> gfxDrawLine() - ERROR - call to DirectFB::DrawLine()\n");
        return MPE_GFX_ERROR_OSERR;
    }

    return MPE_GFX_ERROR_NOERR;
}

/**
 * <i>gfxRectangle()</i>
 * Draws a rectangle with a given graphic context.
 * This function calls DirectFB API.
 *
 * @param c      Pointer to the graphic context.
 * @param rect   The bounds of the rectangle to draw.
 * @param filled True if this is a filled rectangle.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error gfxRectangle(mpeos_GfxContext *c, mpe_GfxRectangle *rect,
        mpe_Bool filled)
{
    DFBResult res;
    IDirectFBSurface* s = c->os_ctx.subs;

    if (c->paintmode == MPE_GFX_DST)
        return MPE_GFX_ERROR_NOERR;

    if (filled == TRUE)
    {
        res = s->FillRectangle(s, rect->x, rect->y, rect->width, rect->height);
        if (res != DFB_OK)
        {
            MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                    "<<GFX>> gfxRectangle() - ERROR - call to DirectFB::FillRectangle()\n");
            return MPE_GFX_ERROR_OSERR;
        }
    }
    else
    {
        res = s->DrawRectangle(s, rect->x, rect->y, rect->width, rect->height);
        if (res != DFB_OK)
        {
            MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                    "<<GFX>> gfxRectangle() - ERROR - call to DirectFB::DrawRectangle()\n");
            return MPE_GFX_ERROR_OSERR;
        }
    }

    return MPE_GFX_ERROR_NOERR;
}

/**
 * <i>gfxEllipse()</i>
 * Draws an ellipse with a given graphic context.
 *
 * @param c      Pointer to the graphic context.
 * @param bounds The bounds of the ellispe to draw.
 * @param filled True if this is a filled ellipse.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error gfxEllipse(mpeos_GfxContext *c, mpe_GfxRectangle *bounds,
        mpe_Bool filled)
{
    DFBResult res = DFB_OK;
    IDirectFBSurface* s = c->os_ctx.subs;

    if (c->paintmode == MPE_GFX_DST)
        return MPE_GFX_ERROR_NOERR;

    if (filled == TRUE)
    {
        res = s->FillOval(s, bounds->x, bounds->y, bounds->width,
                bounds->height);
    }
    else
    {
        res = s->DrawOval(s, bounds->x, bounds->y, bounds->width,
                bounds->height);
    }

    if (res != DFB_OK)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> gfxEllipse() - ERROR - call to DirectFB::()\n");
        return MPE_GFX_ERROR_OSERR;
    }

    return MPE_GFX_ERROR_NOERR;
}

/**
 * <i>gfxRoundRectangle()</i>
 * Draws a round rectangle with a given graphic context.
 * This function calls DirectFB API.
 *
 * @param c         Pointer to the graphic context.
 * @param rect      The bounds of the round rectangle to draw.
 * @param arcWidth  width the horizontal diameter of the arc at the four corners
 * @param arcHeight height the vertical diameter of the arc at the four corners
 * @param filled    True if this is a filled ellipse.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error gfxRoundRectangle(mpeos_GfxContext *c, mpe_GfxRectangle *rect,
        int32_t arcWidth, int32_t arcHeight, mpe_Bool filled)
{
    DFBResult res = DFB_OK;
    IDirectFBSurface* s = c->os_ctx.subs;

    if (c->paintmode == MPE_GFX_DST)
        return MPE_GFX_ERROR_NOERR;

    if (filled == TRUE)
    {
        res = s->FillRoundRect(s, rect->x, rect->y, rect->width, rect->height,
                arcWidth, arcHeight);
    }
    else
    {
        res = s->DrawRoundRect(s, rect->x, rect->y, rect->width, rect->height,
                arcWidth, arcHeight);
    }

    if (res != DFB_OK)
    {
        MPEOS_LOG(
                MPE_LOG_WARN,
                MPE_MOD_GFX,
                "<<GFX>> gfxRoundRectangle() - ERROR - call to DirectFB::Draw[Fill]RoundRect()\n");
        return MPE_GFX_ERROR_OSERR;
    }

    return MPE_GFX_ERROR_NOERR;
}

/**
 * <i>gfxArc()</i>
 * Draws an arc with a given graphic context.
 * This function calls DirectFB API.
 *
 * @param c          Pointer to the graphic context.
 * @param rect       The bounds of the arc to draw.
 * @param startAngle Start of the arc's angle.
 * @param arcAngle   End of the arc's angle.
 * @param filled     True if this is a filled ellipse.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error gfxArc(mpeos_GfxContext *c, mpe_GfxRectangle *bounds,
        int32_t startAngle, int32_t arcAngle, mpe_Bool filled)
{
    DFBResult res = DFB_OK;
    IDirectFBSurface* s = c->os_ctx.subs;

    if (c->paintmode == MPE_GFX_DST)
        return MPE_GFX_ERROR_NOERR;

    if (filled == TRUE)
    {
        res = s->FillArc(s, bounds->x, bounds->y, bounds->width,
                bounds->height, startAngle, arcAngle);
    }
    else
    {
        res = s->DrawArc(s, bounds->x, bounds->y, bounds->width,
                bounds->height, startAngle, arcAngle);
    }

    if (res != DFB_OK)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> gfxArc() - ERROR - call to DirectFB::Draw[Fill]Arc()\n");
        return MPE_GFX_ERROR_OSERR;
    }

    return MPE_GFX_ERROR_NOERR;
}

/**
 * <i>gfxPolyline()</i>
 * Draws multiple lines with a given graphic context.
 * This function calls DirectFB API.
 *
 * @param c          Pointer to the graphic context.
 * @param xCoords    The x values of each coordinate.
 * @param yCoords    The y values of each coordinate.
 * @param nCoords    Number of coordinates.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error gfxPolyline(mpeos_GfxContext *c, int32_t *xCoords, int32_t *yCoords,
        int32_t nCoords)
{
    DFBResult res = DFB_OK;
    IDirectFBSurface *s = c->os_ctx.subs;
    int i;

    if (c->paintmode == MPE_GFX_DST)
        return MPE_GFX_ERROR_NOERR;

    for (i = 0; i < nCoords - 1; ++i)
    {
        res = s->DrawLine(s, c->orig.x + xCoords[i], c->orig.y + yCoords[i],
                c->orig.x + xCoords[i + 1], c->orig.y + yCoords[i + 1]);
        if (res != DFB_OK)
            break;
    }

    if (res != DFB_OK)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> gfxPolyline() - ERROR - call to DirectFB::DrawLine()\n");
        return MPE_GFX_ERROR_OSERR;
    }

    return MPE_GFX_ERROR_NOERR;
}

/**
 * <i>gfxPolygon()</i>
 * Draws a polygon with a given graphic context.
 * This function calls directfb API.
 *
 * @param c          Pointer to the graphic context.
 * @param xCoords    The x values of each coordinate.
 * @param yCoords    The y values of each coordinate.
 * @param nCoords    Number of coordinates.
 * @param filled     True if this is a filled polygon.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error gfxPolygon(mpeos_GfxContext *c, int32_t *xCoords, int32_t *yCoords,
        int32_t nCoords, mpe_Bool filled)
{
    DFBResult res = DFB_OK;
    IDirectFBSurface *s = c->os_ctx.subs;

    if (c->paintmode == MPE_GFX_DST)
        return MPE_GFX_ERROR_NOERR;

    if (filled == true)
    {
        res = s->FillPolygon(s, (int*) xCoords, (int*) yCoords, nCoords);
    }
    else
    {
        if (gfxPolyline(c, xCoords, yCoords, nCoords) == MPE_GFX_ERROR_NOERR
                && ((xCoords[nCoords - 1] == xCoords[0] && yCoords[nCoords - 1]
                        == yCoords[0]) || gfxDrawLine(c, c->orig.x
                        + xCoords[nCoords - 1], c->orig.y
                        + yCoords[nCoords - 1], c->orig.x + xCoords[0],
                        c->orig.y + yCoords[0]) == MPE_GFX_ERROR_NOERR))
        {
            res = DFB_OK;
        }
        else
        {
            res = DFB_FAILURE;
        }
    }
    if (res != DFB_OK)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> gfxPolygon() - ERROR - call to DirectFB::()\n");
        return MPE_GFX_ERROR_OSERR;
    }

    return MPE_GFX_ERROR_NOERR;
}

/**
 * <i>gfxBlit()</i>
 * Performs a blit operation.
 * This function calls directfb API.
 *
 * @param dest      Pointer to the destination's graphic context.
 * @param source    The surface to blit from.
 * @param dx        The destination x coordinate.
 * @param dy        The destinationtion y coordinate.
 * @param srect     The blitting area from the source.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error gfxBitBlt(mpeos_GfxContext *dest, mpeos_GfxSurface *source,
        int32_t dx, int32_t dy, mpe_GfxRectangle *srect)
{
    DFBResult res = DFB_OK;

    IDirectFBSurface* des = dest->os_ctx.subs;
    IDirectFBSurface* src = source->os_data.os_s;

    DFBRectangle srec;
    srec.x = srect->x;
    srec.y = srect->y;
    srec.w = srect->width;
    srec.h = srect->height;

    if (dest->paintmode == MPE_GFX_DST)
        return MPE_GFX_ERROR_NOERR;

    res = des->Blit(des, src, &srec, dx + dest->orig.x, dy + dest->orig.y);

    if (res != DFB_OK)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> gfxBitBlt() - ERROR - call to DirectFB::Blit()\n");
        return MPE_GFX_ERROR_OSERR;
    }

    return MPE_GFX_ERROR_NOERR;
}

/**
 * <i>gfxStretchBlt()</i>
 * Performs a stretch blit operation.
 * This function calls directfb API.
 *
 * @param dest      Pointer to the destination's graphic context.
 * @param source    The surface to blit from.
 * @param drect     The destination area.
 * @param srect     The source area.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error gfxStretchBlt(mpeos_GfxContext *dest, mpeos_GfxSurface *source,
        mpe_GfxRectangle *drect, mpe_GfxRectangle *srect)
{
    DFBResult res = DFB_OK;
    IDirectFBSurface* des = dest->os_ctx.subs;
    IDirectFBSurface* src = source->os_data.os_s;

    // If a width/height of a rectangle is negative, this implies that 
    // the contents of that rectangle are "flipped" by the blt.  But, 
    // it appears to be unclear what the x/y rectangle component
    // should be in that case.  The RI assumes, for example, that if 
    // the width is negative, then the contents are flipped, but lie 
    // to the left of the x value, rather than the right.  DirectFB appears
    // to assume the opposite: that the contents are flipped, but still lie 
    // to the right of the x value.  I can't find explicit confirmation of 
    // this in the DirectFB documentation, but experimentation shows it to 
    // be the case.  To resolve the discrepancy between the way the RI and
    // DirectFB treat this, we perform the following transformation.
    if (srect->width < 0)
    {
        srect->x += srect->width;
    }
    if (srect->height < 0)
    {
        srect->y += srect->height;
    }
    if (drect->width < 0)
    {
        drect->x += drect->width;
    }
    if (drect->height < 0)
    {
        drect->y += drect->height;
    }

    DFBRectangle srec;
    DFBRectangle drec;
    srec.x = srect->x;
    srec.y = srect->y;
    srec.w = srect->width;
    srec.h = srect->height;

    drec.x = drect->x + dest->orig.x;
    drec.y = drect->y + dest->orig.y;
    drec.w = drect->width;
    drec.h = drect->height;

    if (dest->paintmode == MPE_GFX_DST)
        return MPE_GFX_ERROR_NOERR;

    res = des->StretchBlit(des, src, &srec, &drec);

    if (res != DFB_OK)
    {
        MPEOS_LOG(
                MPE_LOG_WARN,
                MPE_MOD_GFX,
                "<<GFX>> gfxBitBlt() - ERROR - call to DirectFB::StretchBlit() src=0x%p, dest=0x%p,srect = %d,%d,%d,%d -> drect = %d,%d,%d,%d\n",
                src, des, srect->x, srect->y, srect->width, srect->height,
                drect->x + dest->orig.x, drect->y + dest->orig.y, drect->width,
                drect->height);
        return MPE_GFX_ERROR_OSERR;
    }

    return MPE_GFX_ERROR_NOERR;
}

/**
 * <i>gfxDrawString()</i>
 * Draws a character string with a given context.
 * This function calls directfb API.
 *
 * @param x         X position of the text.
 * @param y         Y position of the text.
 * @param buf       A buffer that contains the string to draw.
 * @param len       Length of the string in bytes.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error gfxDrawString(mpeos_GfxContext *c, int32_t x, int32_t y,
        const char* buf, int32_t len)
{
    DFBResult res = DFB_OK;
    IDirectFBSurface* s = c->os_ctx.subs;

    if (c->paintmode == MPE_GFX_DST)
        return MPE_GFX_ERROR_NOERR;

    res = s->DrawString(s, buf, len, x, y, DSTF_LEFT);

    if (res != DFB_OK)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> gfxDrawString() - ERROR - call to DirectFB::DrawString()\n");
        return MPE_GFX_ERROR_OSERR;
    }

    return MPE_GFX_ERROR_NOERR;
}

/**
 * <i>gfxDrawString()</i>
 * Draws a UTF-16 formatted string with a given context.
 * This function calls directfb API.
 *
 * @param x         X position of the text.
 * @param y         Y position of the text.
 * @param buf       A buffer that contains the UTF-16 string to draw.
 * @param len       Length of the UTF-16 buffer.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error gfxDrawString16(mpeos_GfxContext *c, int32_t x, int32_t y,
        const mpe_GfxWchar *buf, int32_t len)
{
    DFBResult res = DFB_OK;
    IDirectFBSurface* s = c->os_ctx.subs;
    mpe_Error error = MPE_GFX_ERROR_NOERR;
    ConversionResult err;
    char *newBuf = NULL;
    int32_t totalBytes;

    if (c->paintmode == MPE_GFX_DST)
        return MPE_GFX_ERROR_NOERR;

    /* first do the conversion with a null target to get the total number
     * of bytes needed for the target buffer
     */
    err = SizeUTF16toUTF8((const UTF16**) &buf, (UTF16*) (buf + len),
            (ConversionFlags) strictConversion, (long*) &totalBytes);

    if (err == conversionOK)
    {
        /* allocate buffer totalBytes long */
        if (mpeos_memAllocP(MPE_MEM_TEMP, totalBytes, (void**) &newBuf)
                != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                    "<<GFX>> gfxDrawString16() - mpeos_memAllocP failed\n");
            res = (DFBResult) - 1;
        }
        else
        {
            /* do the conversion for real this time */
            err = ConvertUTF16toUTF8((const UTF16**) &buf,
                    (UTF16*) (buf + len), (UTF8**) &newBuf, (UTF8*) (newBuf
                            + totalBytes), (ConversionFlags) strictConversion);
            if (err == conversionOK)
            {
                res = s->DrawString(s, newBuf, totalBytes, x, y, DSTF_LEFT);
            }
            mpeos_memFreeP(MPE_MEM_TEMP, newBuf);
        }
    }

    if (res != DFB_OK || err != conversionOK)
    {
        MPEOS_LOG(
                MPE_LOG_WARN,
                MPE_MOD_GFX,
                "<<GFX>> gfxDrawString16() - ERROR - call to DirectFB::() or UTF-16 to UTF-8 error\n");
        error = MPE_GFX_ERROR_OSERR;
    }

    return error;
}

/******************************************************************************
 * Public functions
 *****************************************************************************/

/**
 * <i>mpeos_gfxDrawLine()</i>
 * Draws a single-pixel line from one end-point to another using the current color.
 * This operation is subject to the current paint mode.
 *
 * @param ctx The context with which to render.
 * @param x1  X-coordinate of the first endpoint that defines the line.
 * @param y1  Y-coordinate of the first endpoint that defines the line.
 * @param x2  X-coordinate of the second endpoint that defines the line.
 * @param y2  Y-coordinate of the second endpoint that defines the line.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error mpeos_gfxDrawLine(mpe_GfxContext ctx, int32_t x_1, int32_t y_1,
        int32_t x_2, int32_t y_2)
{
    mpe_Error err;
    mpeos_GfxContext *c = (mpeos_GfxContext *) ctx;

    GFX_LOCK(c);

    err = gfxDrawLine(c, c->orig.x + x_1, c->orig.y + y_1, c->orig.x + x_2,
            c->orig.y + y_2);

    GFX_UNLOCK(c);

    return err;
}

/**
 * <i>mpeos_gfxDrawString()</i>
 * Draws a String at a given position with the base line as a reference point.
 *
 * @param ctx     The context from which the text is drawn.
 * @param x       The x position of the text.
 * @param y       The y position of the text.
 * @param buf     Pointer to the text buffer.
 * @param len     Number of characters to draw from the text.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error mpeos_gfxDrawString(mpe_GfxContext ctx, int32_t x, int32_t y,
        const char* buf, int32_t len)
{
    mpe_Error err = MPE_GFX_ERROR_NOERR;
    mpeos_GfxContext *c = (mpeos_GfxContext *) ctx;

    GFX_LOCK(c);

    err = gfxDrawString(c, c->orig.x + x, c->orig.y + y, buf, len);

    GFX_UNLOCK(c);

    return err;
}

/**
 * <i>mpeos_gfxDrawString16()</i>
 * Draws a UTF-16 String at a given position with the base line as a reference point.
 *
 * @param ctx     The context from which the text is drawn.
 * @param x       The x position of the text.
 * @param y       The y position of the text.
 * @param buf     Pointer to the UTF-16 text buffer.
 * @param len     Number of characters to draw from the text.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error mpeos_gfxDrawString16(mpe_GfxContext ctx, int32_t x, int32_t y,
        const mpe_GfxWchar *buf, int32_t len)
{
    mpe_Error err = MPE_GFX_ERROR_NOERR;
    mpeos_GfxContext *c = (mpeos_GfxContext *) ctx;

    GFX_LOCK(c);

    err = gfxDrawString16(c, c->orig.x + x, c->orig.y + y, buf, len);

    GFX_UNLOCK(c);

    return err;
}

/**
 * <i>mpeos_gfxDrawRect()</i>
 * Draws the outline of the specified rectangle.
 * The left and right edges of the rectangle are at x and x + width.
 * The top and bottom edges are at y and y + height. The rectangle is
 * drawn using the graphics context's current color.
 * Note that means that the drawn rectangle bounds are +1 in both the
 * horizontal and vertical directions.
 *
 * This operation is subject to the current paint mode.
 *
 * @param ctx The context with which to render.
 * @param rect Defines the path taken in rendering the outline of the rectangle.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error mpeos_gfxDrawRect(mpe_GfxContext ctx, mpe_GfxRectangle *rect)
{
    mpe_Error err = MPE_GFX_ERROR_NOERR;
    mpeos_GfxContext *c = (mpeos_GfxContext *) ctx;
    mpe_GfxRectangle r;

    r.x = c->orig.x + rect->x;
    r.y = c->orig.y + rect->y;
    r.width = rect->width + 1;
    r.height = rect->height + 1;

    GFX_LOCK(c);

    err = gfxRectangle(c, &r, FALSE);

    GFX_UNLOCK(c);

    return err;
}

/**
 * <i>mpeos_gfxFillRect()</i>
 * Fills the specified rectangle.
 * The left and right edges of the rectangle are at x and x + width - 1.
 * The top and bottom edges are at y and y + height - 1. The resulting
 * rectangle covers an area width pixels wide by height pixels tall.
 * The rectangle is filled using the graphics context's current color.
 *
 * This operation is subject to the current paint mode.
 *
 * @param ctx The context with which to render.
 * @param rect Defines the path taken in rendering the interior of the rectangle.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error mpeos_gfxFillRect(mpe_GfxContext ctx, mpe_GfxRectangle *rect)
{
    mpe_Error err = MPE_GFX_ERROR_NOERR;
    mpeos_GfxContext *c = (mpeos_GfxContext *) ctx;

    mpe_GfxRectangle r;

    r.x = c->orig.x + rect->x;
    r.y = c->orig.y + rect->y;
    r.width = rect->width;
    r.height = rect->height;

    GFX_LOCK(c);

    err = gfxRectangle(c, &r, TRUE);
    GFX_UNLOCK(c);

    //printtime("mpeos_gfxFillRect");

    return err;
}

/**
 * <i>mpeos_gfxClearRect()</i>
 * Fills the specified rectangle with the given color.
 * The left and right edges of the rectangle are at x and x + width - 1.
 * The top and bottom edges are at y and y + height - 1. The resulting
 * rectangle covers an area width pixels wide by height pixels tall.
 * The rectangle is filled using the given color rather than the
 * graphics context's current color.
 *
 * This operation is NOT subject to the current paint mode.
 * An implicit paint mode of SRC is used.
 *
 * @param ctx The context with which to render.
 * @param rect Defines the path taken in rendering the interior of the rectangle.
 * @param color The color used to fill the rectangle
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error mpeos_gfxClearRect(mpe_GfxContext ctx, mpe_GfxRectangle *rect,
        mpe_GfxColor color)
{
    mpe_Error err = MPE_GFX_ERROR_NOERR;
    mpeos_GfxContext *c = (mpeos_GfxContext *) ctx;
    mpe_GfxColor saveColor;

    mpe_GfxRectangle r;

    r.x = c->orig.x + rect->x;
    r.y = c->orig.y + rect->y;
    r.width = rect->width;
    r.height = rect->height;

    GFX_LOCK(c);

    /* Save current color */
    saveColor = c->color;
    if ((err = mpeos_gfxSetColor(ctx, color)) == MPE_GFX_ERROR_NOERR)
    {
        mpe_GfxPaintMode saveMode;
        uint32_t saveData;

        /* Save current paint mode/data */
        saveMode = c->paintmode;
        saveData = c->modedata;
        if ((err = mpeos_gfxSetPaintMode(ctx, MPE_GFX_SRC, 255))
                == MPE_GFX_ERROR_NOERR)
        {
            /* Fill rectangle w/ new color. */
            err = gfxRectangle(c, &r, TRUE);

            /* Restore previous mode. */
            (void) mpeos_gfxSetPaintMode(ctx, saveMode, saveData);
        }
        /* Restore previous color. */
        (void) mpeos_gfxSetColor(ctx, saveColor);
    }

    GFX_UNLOCK(c);

    return err;
}

/**
 * <i>mpeos_gfxDrawEllipse()</i>
 * Draws an outlined ellipse.
 * The horizontal and vertical diameters are bounds.width+1 pixels and
 * bounds.height+1 pixels respectively
 *
 * @param ctx The context with which to render.
 * @param bounds bounds of the rendered ellipse
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error mpeos_gfxDrawEllipse(mpe_GfxContext ctx, mpe_GfxRectangle *bounds)
{
    mpe_Error err = MPE_GFX_ERROR_NOERR;
    mpeos_GfxContext *c = (mpeos_GfxContext *) ctx;

    mpe_GfxRectangle r;

    r.x = c->orig.x + bounds->x;
    r.y = c->orig.y + bounds->y;
    r.width = bounds->width + 1;
    r.height = bounds->height + 1;

    GFX_LOCK(c);

    err = gfxEllipse(c, &r, FALSE);

    GFX_UNLOCK(c);

    return err;
}

/**
 * <i>mpeos_gfxFillEllipse()</i>
 * Draws a filled ellipse.
 * The horizontal and vertical diameters are bounds.width and bounds.height respectively.
 *
 * @param ctx The context with which to render.
 * @param bounds bounds of the rendered ellipse.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error mpeos_gfxFillEllipse(mpe_GfxContext ctx, mpe_GfxRectangle *bounds)
{
    mpe_Error err = MPE_GFX_ERROR_NOERR;
    mpeos_GfxContext *c = (mpeos_GfxContext *) ctx;

    mpe_GfxRectangle r;

    r.x = c->orig.x + bounds->x;
    r.y = c->orig.y + bounds->y;
    r.width = bounds->width;
    r.height = bounds->height;

    GFX_LOCK(c);

    err = gfxEllipse(c, &r, TRUE);

    GFX_UNLOCK(c);

    return err;
}

/**
 * <i>mpeos_gfxDrawRoundRect()</i>
 * Draws an outlined rectangle with rounded corners.
 * The bounds parameter is interpreted as for DrawRect.
 * The arcWidth and arcHeight parameters specify the horizontal and
 * vertical diameters (respectively) of the arcs drawn at each of the four corners.
 *
 * @param ctx The context with which to render.
 * @param rect Defines the bounds of the rendered round rectangle
 * @param arcWidth The horizontal diameter of the arc at the four corners
 * @param arcHeight The vertical diameter of the arc at the four corners
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error mpeos_gfxDrawRoundRect(mpe_GfxContext ctx, mpe_GfxRectangle *rect,
        int32_t arcWidth, int32_t arcHeight)
{
    mpe_Error err = MPE_GFX_ERROR_NOERR;
    mpeos_GfxContext *c = (mpeos_GfxContext *) ctx;
    mpe_GfxRectangle r;

    r.x = c->orig.x + rect->x;
    r.y = c->orig.y + rect->y;
    r.width = rect->width + 1;
    r.height = rect->height + 1;

    GFX_LOCK(c);

    err = gfxRoundRectangle(c, &r, arcWidth, arcHeight, false);

    GFX_UNLOCK(c);

    return err;
}

/**
 * Draws a filled rectangle with rounded corners.
 * The bounds parameter is interpreted as for FillRect.
 * The arcWidth and arcHeight parameters specify the horizontal and
 * vertical diameters (respectively) of the arcs drawn at each of the four corners.
 *
 * @param ctx The context with which to render.
 * @param rect Defines the bounds of the rendered round rectangle
 * @param arcWidth The horizontal diameter of the arc at the four corners
 * @param arcHeight The vertical diameter of the arc at the four corners
 */
mpe_Error mpeos_gfxFillRoundRect(mpe_GfxContext ctx, mpe_GfxRectangle *rect,
        int32_t arcWidth, int32_t arcHeight)
{
    mpe_Error err = MPE_GFX_ERROR_NOERR;
    mpeos_GfxContext *c = (mpeos_GfxContext *) ctx;
    mpe_GfxRectangle r;
    r.x = c->orig.x + rect->x;
    r.y = c->orig.y + rect->y;
    r.width = rect->width;
    r.height = rect->height;

    GFX_LOCK(c);

    err = gfxRoundRectangle(c, &r, arcWidth, arcHeight, true);

    GFX_UNLOCK(c);

    return err;
}

/**
 * <i>mpeos_gfxDrawArc()</i>
 * Draws the outline corresponding to the elliptical arc.
 * The draw operation draws the single-pixel outer edge of the arc.
 * Angles are interpreted such that 0 degrees corresponds to the 3 o'clock position.
 * A positive value indicates a counter-clockwise rotation; negative indicates clockwise.
 * The resulting arc covers an area bounds.width+1 pixels by bounds.height+1 pixels.
 * The center of the arc is the center of the bounding rectangle.
 * A 45 degree always points to a corner of the rectangle (such that, if the rectangle
 * is longer in one direction, the angles will be skewed along the longer axis).
 *
 * @param ctx The context with which to render.
 * @param bounds Defines the bounds of the rendered arc
 * @param startAngle The beginning angle.
 * @param arcAngle The angular extent of the arc, relative to the startAngle.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error mpeos_gfxDrawArc(mpe_GfxContext ctx, mpe_GfxRectangle *bounds,
        int32_t startAngle, int32_t arcAngle)
{
    mpe_Error err = MPE_GFX_ERROR_NOERR;
    mpeos_GfxContext *c = (mpeos_GfxContext *) ctx;

    mpe_GfxRectangle r;

    r.x = c->orig.x + bounds->x;
    r.y = c->orig.y + bounds->y;
    r.width = bounds->width + 1;
    r.height = bounds->height + 1;

    GFX_LOCK(c);

    err = gfxArc(c, &r, startAngle, arcAngle, FALSE);

    GFX_UNLOCK(c);

    return err;

}

/**
 * <i>mpeos_gfxFillArc()</i>
 * Draws a pie corresponding to the elliptical arc.
 * The fill operation fills the internal area of the arc.
 * Angles are interpreted such that 0 degrees corresponds to the 3 o'clock position.
 * A positive value indicates a counter-clockwise rotation; negative indicates clockwise.
 * The resulting arc covers an area bounds.width+1 pixels by bounds.height+1 pixels.
 * The center of the arc is the center of the bounding rectangle.
 * A 45 degree always points to a corner of the rectangle (such that, if the rectangle
 * is longer in one direction, the angles will be skewed along the longer axis).
 *
 * @param ctx The context with which to render.
 * @param bounds Defines the bounds of the rendered arc
 * @param startAngle The beginning angle.
 * @param arcAngle The angular extent of the arc, relative to the startAngle.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error mpeos_gfxFillArc(mpe_GfxContext ctx, mpe_GfxRectangle *bounds,
        int32_t startAngle, int32_t arcAngle)
{
    mpe_Error err = MPE_GFX_ERROR_NOERR;
    mpeos_GfxContext *c = (mpeos_GfxContext *) ctx;

    mpe_GfxRectangle r;

    r.x = c->orig.x + bounds->x;
    r.y = c->orig.y + bounds->y;
    r.width = bounds->width + 1;
    r.height = bounds->height + 1;

    GFX_LOCK(c);

    err = gfxArc(c, &r, startAngle, arcAngle, TRUE);

    GFX_UNLOCK(c);

    return err;

}

/**
 * <i>mpeos_gfxDrawPolyline()</i>
 * Draws several single-pixels as defined by the xCoords and yCoords arrays using
 * the current color.  This operation is equivalent to performing several
 * individual DrawLine calls, as demonstrated below:
 * <pre>
 * for(int i = 0; i < nCoords-1; ++i)
 *     mpeos_gfxDrawLine(context,
 *                       xCoords[i], yCoords[i],
 *                       xCoords[i+1], yCoords[i+1]);
 * </pre>
 *
 * @param ctx the context used to render
 * @param xCoords Location of an array of size nCoords specifying the x-coordinates
 *                of line endpoints.
 * @param yCoords Location of an array of size nCoords specifying the y-coordinates
 *                of line endpoints.
 * @param nCoords Number of coordinates.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error mpeos_gfxDrawPolyline(mpe_GfxContext ctx, int32_t *xCoords,
        int32_t *yCoords, int32_t nCoords)
{
    mpe_Error err = MPE_GFX_ERROR_NOERR;
    mpeos_GfxContext *c = (mpeos_GfxContext *) ctx;

    GFX_LOCK(c);

    err = gfxPolyline(c, xCoords, yCoords, nCoords);

    GFX_UNLOCK(c);

    return err;

}

/**
 * <i>mpeos_gfxDrawPolygon()</i>
 * Draws an outlined polygon using the path defined with the (x,y) coordinates as
 * specified by xCoords and yCoords.
 * The first and last endpoints ((xCoords[0],yCoords[0]) and
 * (xCoords[nCoords-1],yCoords[nCoords-1]) respectively) are connected if not
 * already specified.
 *
 * @param ctx the context to use in translating the points
 * @param xCoords Location of an array of size nCoords specifying the x-coordinates
 *                of line endpoints.
 * @param yCoords Location of an array of size nCoords specifying the y-coordinates
 *                of line endpoints.
 * @param nCoords Number of coordinates
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error mpeos_gfxDrawPolygon(mpe_GfxContext ctx, int32_t *xCoords,
        int32_t *yCoords, int32_t nCoords)
{
    mpe_Error err = MPE_GFX_ERROR_NOERR;
    mpeos_GfxContext *c = (mpeos_GfxContext *) ctx;

    GFX_LOCK(c);

    err = gfxPolygon(c, xCoords, yCoords, nCoords, FALSE);

    GFX_UNLOCK(c);

    return err;

}

/**
 * <i>mpeos_gfxFillPolygon()</i>
 * Draws an outlined or filled polygon using the path defined with the (x,y) coordinates as
 * specified by xCoords and yCoords.
 * The first and last endpoints ((xCoords[0],yCoords[0]) and
 * (xCoords[nCoords-1],yCoords[nCoords-1]) respectively) are connected if not
 * already specified.
 * The area filled by the fill operation is defined by an even-odd (also known as
 * the alternating) rule.  Such that areas where the polygon intersects itself may be
 * filled or unfilled.
 *
 * @param ctx the context to use in translating the points
 * @param xCoords Location of an array of size nCoords specifying the x-coordinates
 *                of line endpoints.
 * @param yCoords Location of an array of size nCoords specifying the y-coordinates
 *                of line endpoints.
 * @param nCoords Number of coordinates.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error mpeos_gfxFillPolygon(mpe_GfxContext ctx, int32_t *xCoords,
        int32_t *yCoords, int32_t nCoords)
{
    mpe_Error err = MPE_GFX_ERROR_NOERR;
    mpeos_GfxContext *c = (mpeos_GfxContext *) ctx;

    GFX_LOCK(c);

    err = gfxPolygon(c, xCoords, yCoords, nCoords, TRUE);

    GFX_UNLOCK(c);

    return err;
}

/**
 * <i>mpeos_gfxBlitBlt()</i>
 * Copies a portion of the given surface to the destination coordinates within the
 * given context.
 *
 * @param dest the context to copy to.
 * @param source the surface to copy from.
 * @param dx the destination x-coordinate (subject to origin translation).
 * @param dy the destination y-coordinate (subject to origin translation).
 * @param srect address of the source rectangle.
 *
 * @note I think that this should be implemented by the surfaces itself,
 *              because it depends more on the surface implementation than the context.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error mpeos_gfxBitBlt(mpe_GfxContext dest, mpe_GfxSurface source,
        int32_t dx, int32_t dy, mpe_GfxRectangle *srect)
{
    mpe_Error err = MPE_GFX_ERROR_NOERR;
    mpeos_GfxContext *dest_c = (mpeos_GfxContext *) dest;
    mpeos_GfxSurface *src = (mpeos_GfxSurface *) source;

    GFX_LOCK(dest_c);
    err = gfxBitBlt(dest_c, src, dx, dy, srect);
    GFX_UNLOCK(dest_c);

    return err;

}

/**
 * <i>mpeos_gfxStretchBlt()</i>
 * Copies a portion of the given surface to the destination coordinates within the
 * given context, stretching/compressing the selected pixels as necessary.
 *
 * @param dest the context to copy to .
 * @param source the surface to copy from.
 * @param drect address of the destination rectangle.
 * @param srect address of the source rectangle.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed,
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error mpeos_gfxStretchBlt(mpe_GfxContext dest, mpe_GfxSurface source,
        mpe_GfxRectangle *drect, mpe_GfxRectangle *srect)
{
    mpe_Error err = MPE_GFX_ERROR_NOERR;
    mpeos_GfxContext *dest_c = (mpeos_GfxContext *) dest;
    mpeos_GfxSurface *src = (mpeos_GfxSurface *) source;

    GFX_LOCK(dest_c);
    err = gfxStretchBlt(dest_c, src, drect, srect);
    GFX_UNLOCK(dest_c);

    return err;
}
