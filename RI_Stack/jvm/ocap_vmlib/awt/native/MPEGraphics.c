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

#include "awt.h"
#include "common.h"

#include <mpe_gfx.h>
#include <mpe_types.h>
#include <mpe_error.h>
#include <mpe_os.h>

#include <jni.h>
#include <java_awt_MPEGraphics.h>
#include <java_awt_AlphaComposite.h>

#define NULL_CONTEXT ((mpe_GfxContext)0)
#define NULL_FONT    ((mpe_GfxFont)0)

#if 0
#define DRAW_ERROR(f, str) (*env)->ThrowNew (env, MPECachedIDs.AWTError, str)
#else
#define DRAW_ERROR(f, str) MPE_LOG( MPEAWT_LOGFAIL, MPEAWT_LOG_MOD, f ": " str "\n" )
#endif

/**
 * Create a new graphics context for the given surface.
 *
 * @param _surf the surface to create a graphics context for drawing
 */
JNIEXPORT jint JNICALL Java_java_awt_MPEGraphics_pCreate(JNIEnv *env,
        jclass cls, jint _surf)
{
    mpe_GfxSurface surf = (mpe_GfxSurface) _surf;
    mpe_GfxContext gfx = 0;
    UNUSED(cls);

    MPE_LOG(MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEGraphics::pCreate(%08x)\n",
            surf);

    if (mpe_gfxContextNew(surf, &gfx) != MPE_SUCCESS)
    {
        (*env)->ThrowNew(env, MPECachedIDs.AWTError,
                "Could not allocate context");
    }

    MPE_LOG(MPEAWT_LOGENTRY, MPEAWT_LOG_MOD,
            "MPEGraphics::pCreate - returns %08x\n", gfx);

    return (jint) gfx;
}

/**
 * Clone the given graphics context, creating a duplicate with
 * a distinct state.
 *
 * @param _gfx the graphics context to clone
 */
JNIEXPORT jint JNICALL Java_java_awt_MPEGraphics_pClone(JNIEnv *env,
        jclass cls, jint _gfx)
{
    mpe_GfxContext gfx = (mpe_GfxContext) _gfx;
    mpe_GfxContext clone = 0;
    UNUSED(cls);

    MPE_LOG(MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEGraphics::pClone(%08x)\n", gfx);

    if (mpe_gfxContextCreate(gfx, &clone) != MPE_SUCCESS)
    {
        (*env)->ThrowNew(env, MPECachedIDs.AWTError,
                "Could not allocate context");
    }

    MPE_LOG(MPEAWT_LOGENTRY, MPEAWT_LOG_MOD,
            "MPEGraphics::pClone - returns %08x\n", clone);

    return (jint) clone;
}

/**
 * Dispose of the given graphics context.
 * Any use of the graphics context hereafter shall result
 * in undefined behavior.
 *
 * @param _gfx the graphics context to dispose of
 */
JNIEXPORT void JNICALL Java_java_awt_MPEGraphics_pDispose
(JNIEnv *env, jclass cls, jint _gfx)
{
    mpe_GfxContext gfx = (mpe_GfxContext)_gfx;
    UNUSED(env);
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEGraphics::pDispose(%08x)\n", gfx );

    if (gfx != NULL_CONTEXT)
    mpe_gfxContextDelete(gfx);
}

/**
 * Set the current font for the given graphics context.
 *
 * @param _gfx the graphics context to modify
 * @param _font the native font peer to use
 */
JNIEXPORT void JNICALL Java_java_awt_MPEGraphics_pSetFont
(JNIEnv * env, jclass cls, jint _gfx, jint _font)
{
    mpe_GfxContext gfx = (mpe_GfxContext)_gfx;
    mpe_GfxFont font = (mpe_GfxFont)_font;
    UNUSED(env);
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEGraphics::pSetFont(%08x, %08x)\n", gfx, font );

    mpe_gfxSetFont(gfx, font);
}

/**
 * Set the current draw/fill color for the given graphics context.
 *
 * @param _gfx the graphics context to modify
 * @param rgb the color to use
 */
JNIEXPORT void JNICALL Java_java_awt_MPEGraphics_pSetColor
(JNIEnv * env, jclass cls, jint _gfx, jint rgb)
{
    mpe_GfxContext gfx = (mpe_GfxContext)_gfx;
    UNUSED(env);
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEGraphics::pSetColor(%08x)\n", rgb );
    mpe_gfxSetColor(gfx, rgb);
}

/**
 * Set the current draw/fill/blit composition mode for the given graphics
 * context.
 *
 * @param _gfx the graphics context to modify
 * @param rule the new alpha composite rule
 * @param alpha the alpha transparency value
 *
 * @see org_cablelabs_impl_awt_MPEGraphics_pSetPaintMode
 * @see org_cablelabs_impl_awt_MPEGraphics_pSetXORMode
 */
JNIEXPORT void JNICALL Java_java_awt_MPEGraphics_pSetComposite
(JNIEnv * env, jclass cls, jint _gfx, jint rule, jfloat alpha)
{
    mpe_GfxContext gfx = (mpe_GfxContext)_gfx;
    mpe_GfxPaintMode composite;
    unsigned char extraAlpha = (unsigned char)(alpha * 255);
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEGraphics::pSetComposite(%d, %d)\n", rule, extraAlpha );

    switch (rule)
    {
        case java_awt_AlphaComposite_SRC:
        composite = MPE_GFX_SRC;
        break;

        case java_awt_AlphaComposite_SRC_OVER:
        composite = MPE_GFX_SRCOVER;
        break;

        case java_awt_AlphaComposite_CLEAR:
        composite = MPE_GFX_CLR;
        break;

        case java_awt_AlphaComposite_DST_OVER:
        composite = MPE_GFX_DSTOVER;
        break;

        case java_awt_AlphaComposite_SRC_IN:
        composite = MPE_GFX_SRCIN;
        break;

        case java_awt_AlphaComposite_DST_IN:
        composite = MPE_GFX_DSTIN;
        break;

        case java_awt_AlphaComposite_SRC_OUT:
        composite = MPE_GFX_SRCOUT;
        break;

        case java_awt_AlphaComposite_DST_OUT:
        composite = MPE_GFX_DSTOUT;
        break;

        default:
        (*env)->ThrowNew (env, MPECachedIDs.AWTError, "Unknown composite rule");
        return;
    }

    if (mpe_gfxSetPaintMode(gfx, composite, extraAlpha) != MPE_SUCCESS)
    {
        (*env)->ThrowNew (env, MPECachedIDs.AWTError, "Could not set composite rule");
        return;
    }
}

/**
 * Sets the current paint mode for the given graphics context.
 * Equivalent to a call to pSetComposite(SRCOVER, 1.0).
 *
 * @param _gfx the graphics context to modify
 *
 * @see org_cablelabs_impl_awt_MPEGraphics_pSetComposite
 * @see org_cablelabs_impl_awt_MPEGraphics_pSetXORMode
 */
JNIEXPORT void JNICALL Java_java_awt_MPEGraphics_pSetPaintMode
(JNIEnv * env, jclass cls, jint _gfx)
{
    mpe_GfxContext gfx = (mpe_GfxContext)_gfx;
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEGraphics::pSetPaintMode()\n" );

    if (mpe_gfxSetPaintMode(gfx, MPE_GFX_SRCOVER, 255) != MPE_SUCCESS)
    {
        (*env)->ThrowNew (env, MPECachedIDs.AWTError, "Could not set paint mode");
        return;
    }
}

/**
 * Sets the current composite/paint mode to XOR for the given graphics
 * context.
 * Almost equivalent to a call to pSetComposite(XOR, 1.0).
 *
 * @param _gfx the graphics context to modify
 * @param rgb the color to use in XOR calculations
 *
 * @see org_cablelabs_impl_awt_MPEGraphics_pSetComposite
 * @see org_cablelabs_impl_awt_MPEGraphics_pSetPaintMode
 */
JNIEXPORT void JNICALL Java_java_awt_MPEGraphics_pSetXORMode
(JNIEnv * env, jclass cls, jint _gfx, jint rgb)
{
    mpe_GfxContext gfx = (mpe_GfxContext)_gfx;
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEGraphics::pSetXORMode(%08x)\n", rgb );

    if (mpe_gfxSetPaintMode(gfx, MPE_GFX_XOR, rgb) != MPE_SUCCESS)
    {
        (*env)->ThrowNew (env, MPECachedIDs.AWTError, "Could not set xor mode");
        return;
    }
}

/**
 * Implements Graphics.fillRect().
 *
 * @param _gfx the graphics context to use
 */
JNIEXPORT void JNICALL Java_java_awt_MPEGraphics_pFillRect
(JNIEnv * env, jclass cls, jint _gfx, jint x, jint y, jint w, jint h)
{
    mpe_GfxContext gfx = (mpe_GfxContext)_gfx;
    mpe_GfxRectangle rect;
    UNUSED(env);
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEGraphics::pfillRect(%d,%d,%d,%d)\n", x, y, w, h );

    /* Do nothing with invalid width/height */
    if ((w <= 0) || (h <= 0))
    return;

    rect.x = x;
    rect.y = y;
    rect.width = w;
    rect.height = h;
    if (mpe_gfxFillRect(gfx, &rect) != MPE_SUCCESS)
    {
        DRAW_ERROR( "pFillRect", "Could not fill rect" );
    }
}

/**
 * Implements Graphics.clearRect().
 *
 * @param _gfx the graphics context to use
 */
JNIEXPORT void JNICALL Java_java_awt_MPEGraphics_pClearRect
(JNIEnv * env, jclass cls, jint _gfx, jint x, jint y, jint w, jint h, jint rgb)
{
    mpe_GfxContext gfx = (mpe_GfxContext)_gfx;
    mpe_GfxRectangle rect;
    UNUSED(env);
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEGraphics::pClearRect(%d,%d,%d,%d,%08x)\n", x, y, w, h, rgb );

    /* Do nothing with invalid width/height */
    if ((w <= 0) || (h <= 0))
    return;

    rect.x = x;
    rect.y = y;
    rect.height = h;
    rect.width = w;

    /* Clear background with given color. */
    if (mpe_gfxClearRect(gfx, &rect, rgb) != MPE_SUCCESS)
    {
        DRAW_ERROR( "pClearRect", "Could not clear rect");
    }
}

/**
 * Implements Graphics.drawRect().
 *
 * @param _gfx the graphics context to use
 */
JNIEXPORT void JNICALL Java_java_awt_MPEGraphics_pDrawRect
(JNIEnv * env, jclass cls, jint _gfx, jint x, jint y, jint w, jint h)
{
    mpe_GfxContext gfx = (mpe_GfxContext)_gfx;
    mpe_GfxRectangle rect;
    UNUSED(env);
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEGraphics::pDrawRect(%d,%d,%d,%d)\n", x, y, w, h );

    /* Do nothing with invalid width/height */
    if ((w < 0) || (h < 0))
    return;

    rect.x = x;
    rect.y = y;
    rect.width = w;
    rect.height = h;

    if (mpe_gfxDrawRect(gfx, &rect) != MPE_SUCCESS)
    {
        DRAW_ERROR( "pDrawRect", "Could not draw rect");
        return;
    }
}

/**
 * Implements Graphics.copyArea().
 *
 * @param _gfx the graphics context to use
 */
JNIEXPORT void JNICALL Java_java_awt_MPEGraphics_pCopyArea
(JNIEnv * env, jclass cls, jint _gfx, jint x, jint y, jint w, jint h, jint dx, jint dy)
{
    mpe_GfxContext gfx = (mpe_GfxContext)_gfx;
    mpe_GfxSurface surf;
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEGraphics::pCopyArea(%d,%d,%d,%d)\n", x, y, w, h, dx, dy );

    /* Do nothing with invalid width/height */
    if ((w <= 0) || (h <= 0))
    return;

    if (mpe_gfxGetSurface(gfx, &surf) != MPE_SUCCESS)
    {
        (*env)->ThrowNew (env, MPECachedIDs.AWTError, "Could not access surface");
        return;
    }
    else
    {
        mpe_GfxRectangle rect;

        rect.x = x;
        rect.y = y;
        rect.width = w;
        rect.height = h;
        if (mpe_gfxBitBlt(gfx, surf, x+dx, y+dy, &rect) != MPE_SUCCESS)
        {
            DRAW_ERROR( "pCopyArea", "Could not copy area");
            return;
        }
    }
}

/**
 * Implements Graphics.drawLine().
 *
 * @param _gfx the graphics context to use
 */
JNIEXPORT void JNICALL Java_java_awt_MPEGraphics_pDrawLine
(JNIEnv * env, jclass cls, jint _gfx, jint x1, jint y1, jint x2, jint y2)
{
    mpe_GfxContext gfx = (mpe_GfxContext)_gfx;
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEGraphics::pDrawLine(%d,%d,%d,%d)\n", x1, y1, x2, y2 );

    if (mpe_gfxDrawLine(gfx, x1, y1, x2, y2) != MPE_SUCCESS)
    {
        DRAW_ERROR( "pDrawLine", "Could not draw line");
        return;
    }
}

/**
 * Implements Graphics.drawPolygon().
 *
 * @param _gfx the graphics context to use
 */
JNIEXPORT void JNICALL Java_java_awt_MPEGraphics_pDrawPolygon
(JNIEnv * env, jclass cls, jint _gfx, jint originX, jint originY,
        jintArray xp, jintArray yp, jint nPoints,
        jboolean close)
{
    mpe_GfxContext gfx = (mpe_GfxContext)_gfx;
    mpe_Error err;
    jint *xpoints, *ypoints;
    int i;
    jboolean xIsCopy, yIsCopy;
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD,
            "MPEGraphics::pDraw%s(%d,%d,%d)\n",
            close ? "Polygon" : "Polyline",
            originX, originY, nPoints );

    if ( NULL == (xpoints = (*env)->GetIntArrayElements (env, xp, &xIsCopy)) )
    return; // exception thrown
    if ( NULL == (ypoints = (*env)->GetIntArrayElements (env, yp, &yIsCopy)) )
    {
        (*env)->ReleaseIntArrayElements (env, xp, xpoints, JNI_ABORT);
        return; // exception thrown
    }

    /*
     * Because we currently don't keep the origin in the native context,
     * we need to update each value with the origin here.
     * Because the array elements retrieved might not be copies,
     * we also need to revert the points to their original values
     * when done.
     */
    for (i = 0; i < nPoints; ++i)
    {
        xpoints[i] += originX;
        ypoints[i] += originY;
    }

    if (close)
    err = mpe_gfxDrawPolygon(gfx, xpoints, ypoints, nPoints);
    else
    err = mpe_gfxDrawPolyline(gfx, xpoints, ypoints, nPoints);

    if (1 || xIsCopy || yIsCopy)
    for (i = 0; i < nPoints; ++i)
    {
        xpoints[i] -= originX;
        ypoints[i] -= originY;
    }
    (*env)->ReleaseIntArrayElements (env, xp, xpoints, JNI_ABORT);
    (*env)->ReleaseIntArrayElements (env, yp, ypoints, JNI_ABORT);

    if (err != MPE_SUCCESS)
    {
        DRAW_ERROR( "pDrawPolygon", "Could not draw polygon/polyline");
        return;
    }
}

/**
 * Implements Graphics.fillPolygon().
 *
 * @param _gfx the graphics context to use
 */
JNIEXPORT void JNICALL Java_java_awt_MPEGraphics_pFillPolygon
(JNIEnv * env, jclass cls, jint _gfx, jint originX, jint originY,
        jintArray xp, jintArray yp, jint nPoints)
{
    mpe_GfxContext gfx = (mpe_GfxContext)_gfx;
    mpe_Error err;
    jint *xpoints, *ypoints;
    int i;
    jboolean xIsCopy, yIsCopy;
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEGraphics::pFillPolygon(%d,%d,%d)\n", originX, originY, nPoints );

    if ( NULL == (xpoints = (*env)->GetIntArrayElements (env, xp, &xIsCopy)) )
    return; // exception thrown
    if ( NULL == (ypoints = (*env)->GetIntArrayElements (env, yp, &yIsCopy)) )
    {
        (*env)->ReleaseIntArrayElements (env, xp, xpoints, JNI_ABORT);
        return; // exception thrown
    }

    /*
     * Because we currently don't keep the origin in the native context,
     * we need to update each value with the origin here.
     * Because the array elements retrieved might not be copies,
     * we also need to revert the points to their original values
     * when done.
     */
    for (i = 0; i < nPoints; ++i)
    {
        xpoints[i] += originX;
        ypoints[i] += originY;
    }

    err = mpe_gfxFillPolygon(gfx, xpoints, ypoints, nPoints);

    if (1 || xIsCopy || yIsCopy)
    for (i = 0; i < nPoints; ++i)
    {
        xpoints[i] -= originX;
        ypoints[i] -= originY;
    }
    (*env)->ReleaseIntArrayElements (env, xp, xpoints, JNI_ABORT);
    (*env)->ReleaseIntArrayElements (env, yp, ypoints, JNI_ABORT);

    if (err != MPE_SUCCESS)
    {
        DRAW_ERROR( "pFillPolygon", "Could not fill polygon");
        return;
    }
}

/**
 * Implements Graphics.drawArc().
 *
 * @param _gfx the graphics context to use
 */
JNIEXPORT void JNICALL Java_java_awt_MPEGraphics_pDrawArc
(JNIEnv * env, jclass cls, jint _gfx, jint x, jint y, jint width,
        jint height, jint start, jint end)
{
    mpe_GfxContext gfx = (mpe_GfxContext)_gfx;
    mpe_GfxRectangle rect;
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEGraphics::pDrawArc(%d,%d,%d,%d, %d,%d)\n", x, y, width, height, start, end );

    rect.x = x;
    rect.y = y;
    rect.width = width;
    rect.height = height;
    if (mpe_gfxDrawArc(gfx, &rect, start, end) != MPE_SUCCESS)
    {
        DRAW_ERROR( "pDrawArc", "Could not draw arc");
        return;
    }
}

/**
 * Implements Graphics.fillArc().
 *
 * @param _gfx the graphics context to use
 */
JNIEXPORT void JNICALL Java_java_awt_MPEGraphics_pFillArc
(JNIEnv * env, jclass cls, jint _gfx, jint x, jint y, jint width,
        jint height, jint start, jint end)
{
    mpe_GfxContext gfx = (mpe_GfxContext)_gfx;
    mpe_GfxRectangle rect;
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEGraphics::pFillArc(%d,%d,%d,%d, %d,%d)\n", x, y, width, height, start, end );

    rect.x = x;
    rect.y = y;
    rect.width = width;
    rect.height = height;
    if (mpe_gfxFillArc(gfx, &rect, start, end) != MPE_SUCCESS)
    {
        DRAW_ERROR( "pFillArc", "Could not fill arc");
        return;
    }
}

/**
 * Implements Graphics.drawOval().
 *
 * @param _gfx the graphics context to use
 */
JNIEXPORT void JNICALL Java_java_awt_MPEGraphics_pDrawOval
(JNIEnv * env, jclass cls, jint _gfx, jint x, jint y, jint width, jint height)
{
    mpe_GfxContext gfx = (mpe_GfxContext)_gfx;
    mpe_GfxRectangle rect;
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEGraphics::pDrawOval(%d,%d,%d,%d)\n", x, y, width, height );

    /* Do nothing with invalid width/height */
    if ((width < 0) || (height < 0))
    return;

    rect.x = x;
    rect.y = y;
    rect.width = width;
    rect.height = height;
    if (mpe_gfxDrawEllipse(gfx, &rect) != MPE_SUCCESS)
    {
        DRAW_ERROR( "pDrawOval", "Could not draw oval");
        return;
    }
}

/**
 * Implements Graphics.fillOval().
 *
 * @param _gfx the graphics context to use
 */
JNIEXPORT void JNICALL Java_java_awt_MPEGraphics_pFillOval
(JNIEnv * env, jclass cls, jint _gfx, jint x, jint y, jint width, jint height)
{
    mpe_GfxContext gfx = (mpe_GfxContext)_gfx;
    mpe_GfxRectangle rect;
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEGraphics::pFillOval(%d,%d,%d,%d)\n", x, y, width, height );

    /* Do nothing with invalid width/height */
    if ((width <= 0) || (height <= 0))
    return;

    rect.x = x;
    rect.y = y;
    rect.width = width;
    rect.height = height;
    if (mpe_gfxFillEllipse(gfx, &rect) != MPE_SUCCESS)
    {
        DRAW_ERROR( "pFillOval", "Could not fill oval");
        return;
    }
}

/**
 * Implements Graphics.drawRoundRect().
 *
 * @param _gfx the graphics context to use
 */
JNIEXPORT void JNICALL Java_java_awt_MPEGraphics_pDrawRoundRect
(JNIEnv * env, jclass cls, jint _gfx, jint x, jint y, jint w, jint h, jint arcW, jint arcH)
{
    mpe_GfxContext gfx = (mpe_GfxContext)_gfx;
    mpe_GfxRectangle rect;
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEGraphics::pDrawRoundRect(%d,%d,%d,%d,%d,%d)\n", x, y, w, h, arcW, arcH );

    /* Do nothing with invalid width/height */
    if ((w < 0) || (h < 0))
    return;

    rect.x = x;
    rect.y = y;
    rect.width = w;
    rect.height = h;
    if (mpe_gfxDrawRoundRect(gfx, &rect, arcW, arcH) != MPE_SUCCESS)
    {
        DRAW_ERROR( "pDrawRoundRect", "Could not draw round rect");
        return;
    }
}

/**
 * Implements Graphics.fillRoundRect().
 *
 * @param _gfx the graphics context to use
 */
JNIEXPORT void JNICALL Java_java_awt_MPEGraphics_pFillRoundRect
(JNIEnv * env, jclass cls, jint _gfx, jint x, jint y, jint w, jint h, jint arcW, jint arcH)
{
    mpe_GfxContext gfx = (mpe_GfxContext)_gfx;
    mpe_GfxRectangle rect;
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEGraphics::pFillRoundRect(%d,%d,%d,%d,%d,%d)\n", x, y, w, h, arcW, arcH );

    /* Do nothing with invalid width/height */
    if ((w <= 0) || (h <= 0))
    return;

    rect.x = x;
    rect.y = y;
    rect.width = w;
    rect.height = h;
    if (mpe_gfxFillRoundRect(gfx, &rect, arcW, arcH) != MPE_SUCCESS)
    {
        DRAW_ERROR( "pFillRoundRect", "Could not draw round rect");
        return;
    }
}

/**
 * Implements Graphics.drawString().
 *
 * @param _gfx the graphics context to use
 */
JNIEXPORT void JNICALL Java_java_awt_MPEGraphics_pDrawString
(JNIEnv * env, jclass cls, jint _gfx, jstring string, jint x, jint y)
{
    mpe_GfxContext gfx = (mpe_GfxContext)_gfx;
    const jchar *chars;
    jboolean isCopy;
    jsize length;
    UNUSED(cls);

    length = (*env)->GetStringLength (env, string);
    if (length == 0)
    return;

    chars = (*env)->GetStringChars (env, string, &isCopy);
    if (chars == NULL)
    return;

    if (mpe_gfxDrawString16(gfx, x, y, (mpe_GfxWchar*)chars, length) != MPE_SUCCESS)
    {
        (*env)->ReleaseStringChars (env, string, chars);
        DRAW_ERROR( "pDrawString", "Could not draw string");
        return;
    }
    (*env)->ReleaseStringChars (env, string, chars);
}

/**
 * Implements Graphics.drawChars().
 *
 * @param _gfx the graphics context to use
 */
JNIEXPORT void JNICALL Java_java_awt_MPEGraphics_pDrawChars
(JNIEnv * env, jclass cls, jint _gfx, jcharArray charArray, jint offset, jint length, jint x, jint y)
{
    mpe_GfxContext gfx = (mpe_GfxContext)_gfx;
    jchar *chars;
    UNUSED(cls);

    if (mpe_memAllocP(MPE_MEM_TEMP, length * sizeof(jchar), (void**)&chars) != MPE_SUCCESS)
    {
        (*env)->ThrowNew (env, MPECachedIDs.OutOfMemoryError, NULL);
        return;
    }

    (*env)->GetCharArrayRegion (env, charArray, offset, length, chars);

    if (!(*env)->ExceptionCheck(env)
            &&
            mpe_gfxDrawString16(gfx, x, y, (mpe_GfxWchar*)chars, length) != MPE_SUCCESS)
    {
        mpe_memFreeP(MPE_MEM_TEMP, chars);
        DRAW_ERROR( "pDrawChars", "Could not draw string");
        return;
    }
    mpe_memFreeP(MPE_MEM_TEMP, chars);
}

/**
 * Changes the clipping rectangle of the given graphics context.
 *
 * @param _gfx the graphics context to modify
 */
JNIEXPORT void JNICALL Java_java_awt_MPEGraphics_pChangeClipRect
(JNIEnv * env, jclass cls, jint _gfx, jint x, jint y, jint w, jint h)
{
    mpe_GfxContext gfx = (mpe_GfxContext)_gfx;
    mpe_GfxRectangle rect;
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEGraphics::pChangeClipRect(%d,%d,%d,%d)\n", x, y, w, h );

    /*
     * Width and height can be negative when the setClip mthod is used.
     * Negative values imply an empty rectangle in Java so we set these
     * to a width and height of zero.
     *
     * X and y can be negative when the setClip method is used as well.
     * Since Java code handles offset/translation, the x/y values that
     * we have are absolute.  As such, a negative x/y value really isn't
     * valid, so we'll just map these to zero as well.
     */
    if (x < 0)
    {
        w += x;
        x = 0;
    }
    if (y < 0)
    {
        h += y;
        y = 0;
    }
    rect.x = x;
    rect.y = y;
    rect.width = (w < 0) ? 0 : w;
    rect.height = (h < 0) ? 0 : h;
    if (mpe_gfxSetClipRect(gfx, &rect) != MPE_SUCCESS)
    {
        MPE_LOG( MPEAWT_LOGFAIL, MPEAWT_LOG_MOD,
                "MPEGraphics::pChangeClipRect(%d,%d,%d,%d) - FAILED!\n", x, y, w, h );

        DRAW_ERROR("pChangeClipRect", "Could not change clipping rectangle");
        return;
    }
}

/**
 * Removes the clipping rectangle from the given graphics context.
 *
 * @param _gfx the graphics context to modify
 */
JNIEXPORT void JNICALL Java_java_awt_MPEGraphics_pRemoveClip
(JNIEnv * env, jclass cls, jint _gfx)
{
    mpe_GfxContext gfx = (mpe_GfxContext)_gfx;
    mpe_GfxSurface surf;
    mpe_GfxSurfaceInfo info;
    mpe_GfxRectangle rect =
    {   0};
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEGraphics::pRemoveClip()\n" );

    if (mpe_gfxGetSurface(gfx, &surf) != MPE_SUCCESS
            || mpe_gfxSurfaceGetInfo(surf, &info) != MPE_SUCCESS
            || (rect.width = info.dim.width, rect.height = info.dim.height,
                    (mpe_gfxSetClipRect(gfx, &rect) != MPE_SUCCESS)))
    {
        DRAW_ERROR("pRemoveClip", "Could not clear clipping rectangle");
        return;
    }
}

/**
 * Returns the BufferedImage peer for a given BufferedImage.
 * This is here because the peer attribute is package private.
 *
 * @param BufferedImage the BufferedImage to return the peer for
 */
JNIEXPORT jobject JNICALL Java_java_awt_MPEGraphics_getBufferedImagePeer(
        JNIEnv *env, jclass cls, jobject BufferedImage)
{
    UNUSED(cls);

    MPE_LOG(MPEAWT_LOGENTRY, MPEAWT_LOG_MOD,
            "MPEGraphics::getBufferedImagePeer()\n");

    return (*env)->GetObjectField(env, BufferedImage,
            MPECachedIDs.java_awt_image_BufferedImage_peerFID);
}
