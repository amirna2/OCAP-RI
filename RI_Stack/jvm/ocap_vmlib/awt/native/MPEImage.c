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

#include "MPEImage.h"
#include "awt.h"
#include "common.h"

#include <mpe_gfx.h>
#include <mpe_types.h>
#include <mpe_error.h>
#include <mpe_os.h>

#include <jni.h>
#include <java_awt_MPEImage.h>

#if defined(MPE_AWTIMAGE_TYPE) && MPE_AWTIMAGE==MPE_AWTIMAGE_CLUT8
static const uint8_t opaqueReds[] =
{
    0, 63, 127, 191, 255,
};
static const uint8_t opaqueGreens[] =
{
    0, 31, 63, 95, 127, 159, 191, 223, 255,
};
static const uint8_t opaqueBlues[] =
{
    0, 127, 255
};
static const uint8_t opaqueGreys[] =
{
    42, 85, 170, 212
};
static const uint8_t transReds[] =
{
    0, 85, 170, 255
};
static const uint8_t transGreens[] =
{
    0, 51, 102, 153, 204, 255
};
static const uint8_t transBlues[] =
{
    0, 255
};
static mpe_GfxColor ocapColors[189];

/** Global OCAP CLUT. */
mpe_GfxPalette gOcapClut;
#endif

JNIEXPORT void JNICALL Java_java_awt_MPEImage_initIDs (JNIEnv * env, jclass cls)
{
    GET_FIELD_ID (MPEImage_widthFID, "width", "I");
    GET_FIELD_ID (MPEImage_heightFID, "height", "I");

    FIND_CLASS ("java/awt/image/ColorModel");
    GET_METHOD_ID (java_awt_image_ColorModel_getRGBMID, "getRGB", "(I)I");

    FIND_CLASS ("java/awt/image/IndexColorModel");
    GET_FIELD_ID (java_awt_image_IndexColorModel_rgbFID, "rgb", "[I");

    FIND_CLASS ("java/awt/image/DirectColorModel");
    GET_FIELD_ID (java_awt_image_DirectColorModel_red_maskFID, "red_mask", "I");
    GET_FIELD_ID (java_awt_image_DirectColorModel_red_offsetFID, "red_offset", "I");
    GET_FIELD_ID (java_awt_image_DirectColorModel_red_scaleFID, "red_scale", "I");
    GET_FIELD_ID (java_awt_image_DirectColorModel_green_maskFID, "green_mask", "I");
    GET_FIELD_ID (java_awt_image_DirectColorModel_green_offsetFID, "green_offset", "I");
    GET_FIELD_ID (java_awt_image_DirectColorModel_green_scaleFID, "green_scale", "I");
    GET_FIELD_ID (java_awt_image_DirectColorModel_blue_maskFID, "blue_mask", "I");
    GET_FIELD_ID (java_awt_image_DirectColorModel_blue_offsetFID, "blue_offset", "I");
    GET_FIELD_ID (java_awt_image_DirectColorModel_blue_scaleFID, "blue_scale", "I");
    GET_FIELD_ID (java_awt_image_DirectColorModel_alpha_maskFID, "alpha_mask", "I");
    GET_FIELD_ID (java_awt_image_DirectColorModel_alpha_offsetFID, "alpha_offset", "I");
    GET_FIELD_ID (java_awt_image_DirectColorModel_alpha_scaleFID, "alpha_scale", "I");

#if defined(MPE_AWTIMAGE_TYPE) && MPE_AWTIMAGE==MPE_AWTIMAGE_CLUT8
    {
        int n = 0;
        int i;
        int r, g, b;

        /* Opaque colors */
        /* Greys */
        for(i = 0; i < sizeof(opaqueGreys)/sizeof(uint8_t); ++i)
        ocapColors[n++] = mpe_gfxRgbToColor(opaqueGreys[i], opaqueGreys[i], opaqueGreys[i]);
        /* Colors */
        for(r = 0; r < sizeof(opaqueReds)/sizeof(uint8_t); ++r)
        for(g = 0; g < sizeof(opaqueGreens)/sizeof(uint8_t); ++g)
        for(b = 0; b < sizeof(opaqueBlues)/sizeof(uint8_t); ++b)
        ocapColors[n++]
        = mpe_gfxRgbToColor(opaqueReds[r], opaqueGreens[g], opaqueBlues[b]);
        /* Transluscent colors */
        for(r = 0; r < sizeof(transReds)/sizeof(uint8_t); ++r)
        for(g = 0; g < sizeof(transGreens)/sizeof(uint8_t); ++g)
        for(b = 0; b < sizeof(transBlues)/sizeof(uint8_t); ++b)
        ocapColors[n++]
        = mpe_gfxArgbToColor(77, transReds[r], transGreens[g], transBlues[b]);
        /* Transparent color */
        ocapColors[n++] = mpe_gfxArgbToColor(0, 0, 0, 0);

        /* Now, create the color palette */
        if (MPE_SUCCESS != mpe_gfxPaletteNew(n, &gOcapClut))
        (*env)->ThrowNew(env, MPECachedIDs.AWTError, "Could not create OCAP clut");
        else if (MPE_SUCCESS != mpe_gfxPaletteSet(gOcapClut, ocapColors, n, 0))
        (*env)->ThrowNew (env, MPECachedIDs.AWTError, "Could not init OCAP clut");
    }
#endif
}

#define NULL_CONTEXT ((mpe_GfxContext)0)
#define NULL_SURFACE ((mpe_GfxSurface)0)

JNIEXPORT void JNICALL Java_java_awt_MPEImage_pSetColorModelBytePixels
(JNIEnv * env, jclass cls, jint _surf, jint x, jint y, jint w, jint h,
        jobject colorModel, jbyteArray pixels, jint offset, jint scansize)
{
    uint32_t rgb;
    jint m, n; /* x, y position in pixel array - same variable names as Javadoc comment for setIntPixels. */
    uint8_t pixel;
    uint8_t lastPixel;
    jint index = offset;
    jbyte *pixelArray = NULL;
    mpe_GfxSurface surf = (mpe_GfxSurface)_surf;
    mpe_GfxSurfaceInfo info;
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEImage::pSetColorModelBytePixels ()\n" );

    if (surf == NULL_SURFACE) return;
    if (mpe_gfxSurfaceGetInfo(surf, &info) != MPE_SUCCESS)
    return;

    pixelArray = (*env)->GetByteArrayElements (env, pixels, NULL);
    if (pixelArray == NULL)
    return;

    /* Make sure first time round the loop we actually get the Microwindows pixel value. */
    lastPixel = (uint8_t)~(pixelArray[index]);
    rgb = 0;

    /* For each pixel in the supplied pixel array look up its rgb value from the colorModel. */
    for (n = 0; n < h; n++)
    {
        for (m = 0; m < w; m++)
        {
            /* Get the pixel at m, n. */

            pixel = (uint8_t) pixelArray[index + m];

            if (lastPixel != pixel)
            {
                rgb = (uint32_t)
                (*env)->CallIntMethod (env, colorModel, MPECachedIDs.java_awt_image_ColorModel_getRGBMID,
                        (jint) pixel);

                if ((*env)->ExceptionCheck (env))
                goto finish;

                /* Remember last pixel value for optimisation purposes. */
                lastPixel = pixel;
            }

#if MPEAWT_DBGPIXELS
            if ( !(y+n) )
            MPE_LOG( MPEAWT_LOGPIXELS, MPEAWT_LOG_MOD, "[%d,%d] = %08x\n", x+m, y+n, rgb );
#endif

            /* Set the pixel according to the surface format */
            SET_PIXEL(info, x+m, y+n, rgb);
        }

        index += scansize;
    }

    finish:
    (*env)->ReleaseByteArrayElements (env, pixels, pixelArray, JNI_ABORT);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEImage::pSetColorModelBytePixels - done\n" );
}

JNIEXPORT void JNICALL Java_java_awt_MPEImage_pSetColorModelIntPixels
(JNIEnv * env, jclass cls, jint _surf, jint x, jint y, jint w, jint h,
        jobject colorModel, jintArray pixels, jint offset, jint scansize)
{
    uint32_t rgb;
    jint m, n; /* x, y position in pixel array - same variable names as Javadoc comment for setIntPixels. */
    uint32_t pixel;
    uint32_t lastPixel;
    jint index = offset;
    jint *pixelArray = NULL;
    mpe_GfxSurface surf = (mpe_GfxSurface)_surf;
    mpe_GfxSurfaceInfo info;
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEImage::pSetColorModelIntPixels()\n" );

    if (surf == NULL_SURFACE) return;
    if (mpe_gfxSurfaceGetInfo(surf, &info) != MPE_SUCCESS)
    return;

    pixelArray = (*env)->GetIntArrayElements (env, pixels, NULL);
    if (pixelArray == NULL)
    return;

    /* Make sure first time round the loop we actually get the Microwindows pixel value. */
    lastPixel = ~(pixelArray[index]);
    rgb = 0;

    /* For each pixel in the supplied pixel array look up its rgb value from the colorModel. */
    for (n = 0; n < h; n++)
    {
        for (m = 0; m < w; m++)
        {
            /* Get the pixel at m, n. */
            pixel = (uint32_t) pixelArray[index + m];

            if (lastPixel != pixel)
            {
                rgb = (uint32_t)
                (*env)->CallIntMethod (env, colorModel, MPECachedIDs.java_awt_image_ColorModel_getRGBMID,
                        pixel);

                if ((*env)->ExceptionCheck (env))
                goto finish;

                /* Remember last pixel value for optimisation purposes. */
                lastPixel = pixel;
            }

#if MPEAWT_DBGPIXELS
            if ( !(y+n) )
            MPE_LOG( MPEAWT_LOGPIXELS, MPEAWT_LOG_MOD, "[%d,%d] = %08x\n", x+m, y+n, rgb );
#endif

            /* Set the pixel according to the surface format */
            SET_PIXEL(info, x+m, y+n, rgb);
        }

        index += scansize;
    }

    finish:
    (*env)->ReleaseIntArrayElements (env, pixels, pixelArray, JNI_ABORT);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEImage::pSetColorModelIntPixels - done\n" );
}

JNIEXPORT void JNICALL Java_java_awt_MPEImage_pSetIndexColorModelBytePixels
(JNIEnv * env, jclass cls, jint _surf, jint x, jint y, jint w, jint h,
        jobject colorModel, jbyteArray pixels, jint offset, jint scansize)
{
    uint32_t rgb;
    jintArray rgbs;
    jint *rgbArray; /* Array of RGB values retrieved from IndexColorModel. */
    jint m, n; /* x, y position in pixel array - same variable names as Javadoc comment for setIntPixels. */
    uint8_t pixel;
    uint8_t lastPixel;
    jint index = offset;
    jbyte *pixelArray = NULL;
    mpe_GfxSurface surf = (mpe_GfxSurface)_surf;
    mpe_GfxSurfaceInfo info;
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEImage::pSetIndexColorModelBytePixels()\n" );

    if (surf == NULL_SURFACE) return;
    if (mpe_gfxSurfaceGetInfo(surf, &info) != MPE_SUCCESS)
    return;

    rgbs = (*env)->GetObjectField (env, colorModel, MPECachedIDs.java_awt_image_IndexColorModel_rgbFID);
    rgbArray = (*env)->GetIntArrayElements (env, rgbs, NULL);

    if (rgbArray == NULL)
    return;

    pixelArray = (*env)->GetByteArrayElements (env, pixels, NULL);

    if (pixelArray == NULL)
    goto finish;

    /* Make sure first time round the loop we actually get the Microwindows pixel value. */
    lastPixel = (uint8_t)~(pixelArray[index]);
    rgb = 0;

    /* For each pixel in the supplied pixel array look up its rgb value from the colorModel. */
    for (n = 0; n < h; n++)
    {
        for (m = 0; m < w; m++)
        {
            /* Get the pixel at m, n. */
            pixel = (uint8_t) pixelArray[index + m];

            /* Get and store red, green, blue values. */
            if (pixel != lastPixel)
            {
                rgb = (uint32_t) rgbArray[pixel];

                /* Remember last pixel value for optimisation purposes. */
                lastPixel = pixel;
            }

#if MPEAWT_DBGPIXELS
            if ( !(y+n) )
            MPE_LOG( MPEAWT_LOGPIXELS, MPEAWT_LOG_MOD, "[%d,%d] = %08x\n", x+m, y+n, rgb );
#endif

            /* Set the pixel according to the surface format */
            SET_PIXEL(info, x+m, y+n, rgb);
        }

        index += scansize;
    }

    finish:
    if (pixelArray != NULL)
    (*env)->ReleaseByteArrayElements (env, pixels, pixelArray, JNI_ABORT);

    (*env)->ReleaseIntArrayElements (env, rgbs, rgbArray, JNI_ABORT);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEImage::pSetIndexColorModelBytePixels - done\n" );
}

JNIEXPORT void JNICALL Java_java_awt_MPEImage_pSetIndexColorModelIntPixels
(JNIEnv * env, jclass cls, jint _surf, jint x, jint y, jint w, jint h,
        jobject colorModel, jintArray pixels, jint offset, jint scansize)
{
    uint32_t rgb;
    jintArray rgbs;
    jint *rgbArray; /* Array of RGB values retrieved from IndexColorModel. */
    jint m, n; /* x, y position in pixel array - same variable names as Javadoc comment for setIntPixels. */
    uint32_t pixel;
    uint32_t lastPixel;
    jint *pixelArray = NULL;
    jint index = offset;
    mpe_GfxSurface surf = (mpe_GfxSurface)_surf;
    mpe_GfxSurfaceInfo info;
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEImage::pSetIndexColorModelIntPixels()\n" );

    if (surf == NULL_SURFACE) return;
    if (mpe_gfxSurfaceGetInfo(surf, &info) != MPE_SUCCESS)
    return;

    rgbs = (*env)->GetObjectField (env, colorModel, MPECachedIDs.java_awt_image_IndexColorModel_rgbFID);
    rgbArray = (*env)->GetIntArrayElements (env, rgbs, NULL);
    if (rgbArray == NULL)
    return;

    pixelArray = (*env)->GetIntArrayElements (env, pixels, NULL);
    if (pixelArray == NULL)
    goto finish;

    /* Make sure first time round the loop we actually get the Microwindows pixel value. */
    lastPixel = ~(pixelArray[index]);
    rgb = 0;

    /* For each pixel in the supplied pixel array look up its rgb value from the colorModel. */
    for (n = 0; n < h; n++)
    {
        for (m = 0; m < w; m++)
        {
            /* Get the pixel at m, n. */
            pixel = (uint32_t) pixelArray[index + m];

            /* Get and store red, green, blue values. */
            if (pixel != lastPixel)
            {
                rgb = (uint32_t) rgbArray[pixel];

                /* Remember last pixel value for optimisation purposes. */
                lastPixel = pixel;
            }

#if MPEAWT_DBGPIXELS
            if ( !(y+n) )
            MPE_LOG( MPEAWT_LOGPIXELS, MPEAWT_LOG_MOD, "[%d,%d] = %08x\n", x+m, y+n, rgb );
#endif

            /* Set the pixel according to the surface format */
            SET_PIXEL(info, x+m, y+n, rgb);
        }

        index += scansize;
    }

    finish:
    if (pixelArray != NULL)
    (*env)->ReleaseIntArrayElements (env, pixels, pixelArray, JNI_ABORT);

    (*env)->ReleaseIntArrayElements (env, rgbs, rgbArray, JNI_ABORT);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEImage::pSetIndexColorModelIntPixels - done\n" );
}

JNIEXPORT void JNICALL Java_java_awt_MPEImage_pSetDirectColorModelPixels
(JNIEnv * env, jclass cls, jint _surf, jint x, jint y, jint w, jint h,
        jobject colorModel, jintArray pixels, jint offset, jint scansize)
{
    jint m, n; /* x, y position in pixel array - same variable names as Javadoc comment for setIntPixels. */
    uint32_t pixel;
    uint32_t lastPixel;
    jint *pixelArray = NULL;
    uint32_t red_mask, red_offset, red_scale;
    uint32_t green_mask, green_offset, green_scale;
    uint32_t blue_mask, blue_offset, blue_scale;
    uint32_t alpha_mask, alpha_offset, alpha_scale;
    uint32_t alpha, red, green, blue;
    jint index = offset;
    mpe_GfxSurface surf = (mpe_GfxSurface)_surf;
    mpe_GfxSurfaceInfo info;
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEImage::pSetDirectColorModelPixels()\n" );

    if (surf == NULL_SURFACE) return;
    if (mpe_gfxSurfaceGetInfo(surf, &info) != MPE_SUCCESS)
    return;

    pixelArray = (*env)->GetIntArrayElements (env, pixels, NULL);
    if (pixelArray == NULL)
    return;

    red_mask =
    (uint32_t) (*env)->GetIntField (env, colorModel,
            MPECachedIDs.java_awt_image_DirectColorModel_red_maskFID);
    red_offset =
    (uint32_t) (*env)->GetIntField (env, colorModel,
            MPECachedIDs.java_awt_image_DirectColorModel_red_offsetFID);
    red_scale =
    (uint32_t) (*env)->GetIntField (env, colorModel,
            MPECachedIDs.java_awt_image_DirectColorModel_red_scaleFID);

    green_mask =
    (uint32_t) (*env)->GetIntField (env, colorModel,
            MPECachedIDs.java_awt_image_DirectColorModel_green_maskFID);
    green_offset =
    (uint32_t) (*env)->GetIntField (env, colorModel,
            MPECachedIDs.java_awt_image_DirectColorModel_green_offsetFID);
    green_scale =
    (uint32_t) (*env)->GetIntField (env, colorModel,
            MPECachedIDs.java_awt_image_DirectColorModel_green_scaleFID);

    blue_mask =
    (uint32_t) (*env)->GetIntField (env, colorModel,
            MPECachedIDs.java_awt_image_DirectColorModel_blue_maskFID);
    blue_offset =
    (uint32_t) (*env)->GetIntField (env, colorModel,
            MPECachedIDs.java_awt_image_DirectColorModel_blue_offsetFID);
    blue_scale =
    (uint32_t) (*env)->GetIntField (env, colorModel,
            MPECachedIDs.java_awt_image_DirectColorModel_blue_scaleFID);

    alpha_mask =
    (uint32_t) (*env)->GetIntField (env, colorModel,
            MPECachedIDs.java_awt_image_DirectColorModel_alpha_maskFID);
    alpha_offset =
    (uint32_t) (*env)->GetIntField (env, colorModel,
            MPECachedIDs.java_awt_image_DirectColorModel_alpha_offsetFID);
    alpha_scale =
    (uint32_t) (*env)->GetIntField (env, colorModel,
            MPECachedIDs.java_awt_image_DirectColorModel_alpha_scaleFID);

    /* Make sure first time round the loop we actually get the Microwindows pixel value. */
    lastPixel = ~(pixelArray[index]);
    red = green = blue = alpha = 0;

    /* For each pixel in the supplied pixel array calculate its rgb value from the colorModel. */
    for (n = 0; n < h; n++)
    {
        for (m = 0; m < w; m++)
        {
            /* Get the pixel at m, n. */
            pixel = (uint32_t) pixelArray[index + m];

            /* If pixel value is not the same as the last one then get red, green, blue
             and set the foreground for drawing the point in the image. */
            if (pixel != lastPixel)
            {
                red = ((pixel & red_mask) >> red_offset);

                if (red_scale != 0)
                red = red * 255 / red_scale;

                green = ((pixel & green_mask) >> green_offset);

                if (green_scale != 0)
                green = green * 255 / green_scale;

                blue = ((pixel & blue_mask) >> blue_offset);

                if (blue_scale != 0)
                blue = blue * 255 / blue_scale;

                if (alpha_mask == 0)
                alpha = 255;

                else
                {
                    alpha = ((pixel & alpha_mask) >> alpha_offset);

                    if (alpha_scale != 0)
                    alpha = alpha * 255 / alpha_scale;
                }

                /* Remember last pixel value for optimisation purposes. */
                lastPixel = pixel;
            }

#if MPEAWT_DBGPIXELS
            if ( !(y+n) )
            MPE_LOG( MPEAWT_LOGPIXELS, MPEAWT_LOG_MOD, "[%d,%d] = %02x%02x%02x%02x\n", x+m, y+n, alpha, red, green, blue );
#endif

            /* Set the pixel according to the surface format */
            SET_PIXEL_ARGB(info, x+m, y+n, alpha, red, green, blue);
        }

        index += scansize;
    }

    if (pixelArray != NULL)
    (*env)->ReleaseIntArrayElements (env, pixels, pixelArray, JNI_ABORT);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEImage::pSetIndexColorModelIntPixels - done\n" );
}

JNIEXPORT jint JNICALL Java_java_awt_MPEImage_pGetRGB(JNIEnv *env, jclass cls,
        jint _surf, jint x, jint y)
{
    mpe_GfxSurface surf = (mpe_GfxSurface) _surf;
    mpe_GfxSurfaceInfo info;
    UNUSED(env);
    UNUSED(cls);

    if (surf == NULL_SURFACE || mpe_gfxSurfaceGetInfo(surf, &info)
            != MPE_SUCCESS)
    {
        return 0;
    }
    else
    {
        uint32_t argb;
        /* Get the pixel according to the surface format */
        GET_PIXEL(info, x, y, argb);
        return argb;
    }
}

JNIEXPORT void JNICALL Java_java_awt_MPEImage_pGetRGBArray
(JNIEnv *env, jclass cls, jint _surf, jint startX, jint startY,
        jint width, jint height, jintArray pixelArray, jint offset, jint scansize)
{
    mpe_GfxSurface surf = (mpe_GfxSurface)_surf;
    mpe_GfxSurfaceInfo info;
    int x, y;
    jint *pixelArrayElements;
    UNUSED(env);
    UNUSED(cls);

    if (surf == NULL_SURFACE
            || mpe_gfxSurfaceGetInfo(surf, &info) != MPE_SUCCESS)
    return;

    pixelArrayElements = (*env)->GetIntArrayElements(env, pixelArray, NULL);
    if (pixelArrayElements == NULL)
    return;

    for (y = startY; y < startY + height; y++)
    {
        for (x = startX; x < startX + width; x++)
        {
            uint32_t argb;
            /* Get the pixel according to the surface format */
            GET_PIXEL(info, x, y, argb);
            pixelArrayElements[offset + (y-startY)*scansize + (x-startX)] = argb;
        }
    }

    (*env)->ReleaseIntArrayElements(env, pixelArray, pixelArrayElements, 0);
}

JNIEXPORT void JNICALL Java_java_awt_MPEImage_pSetRGB
(JNIEnv *env, jclass cls, jint _surf, jint x, jint y, jint rgb)
{
    mpe_GfxSurface surf = (mpe_GfxSurface)_surf;
    mpe_GfxSurfaceInfo info;
    UNUSED(env);
    UNUSED(cls);

    if (surf == NULL_SURFACE
            || mpe_gfxSurfaceGetInfo(surf, &info) != MPE_SUCCESS)
    {
        return;
    }

    /* Set the pixel according to the surface format */
    SET_PIXEL(info, x, y, rgb);
}

#if MPEAWT_DBGIMGDUMP
static void dumpPixels(mpe_GfxSurfaceInfo *info, int x, int y, char* name)
{
    MPE_LOG( MPEAWT_LOGIMGDUMP, MPEAWT_LOG_MOD, "Surface %s %08x\n", (name == NULL) ? "" : name, info->pixeldata );

    if (info->pixeldata != NULL)
    {
        unsigned char* ptr = (unsigned char*)info->pixeldata;
        int ofs;
        switch(info->format)
        {
            case MPE_GFX_RGB565:
            case MPE_GFX_ARGB8888:
            ofs = ((x)*info->bpp)/8 + (y)*info->widthbytes;
            break;
            default:
            ofs = 0;
            break;
        }
        ptr += ofs;
        MPE_LOG( MPEAWT_LOGIMGDUMP, MPEAWT_LOG_MOD, "  pixeldata = %02x %02x %02x %02x %02x %02x %02x %02x\n",
                ptr[0], ptr[1], ptr[2], ptr[3], ptr[4], ptr[5], ptr[6], ptr[7] );
        ptr += 8;
        MPE_LOG( MPEAWT_LOGIMGDUMP, MPEAWT_LOG_MOD, "  pixeldata = %02x %02x %02x %02x %02x %02x %02x %02x\n",
                ptr[0], ptr[1], ptr[2], ptr[3], ptr[4], ptr[5], ptr[6], ptr[7] );
        ptr += 8;
        MPE_LOG( MPEAWT_LOGIMGDUMP, MPEAWT_LOG_MOD, "  pixeldata = %02x %02x %02x %02x %02x %02x %02x %02x\n",
                ptr[0], ptr[1], ptr[2], ptr[3], ptr[4], ptr[5], ptr[6], ptr[7] );
        ptr += 8;
        MPE_LOG( MPEAWT_LOGIMGDUMP, MPEAWT_LOG_MOD, "  pixeldata = %02x %02x %02x %02x %02x %02x %02x %02x\n",
                ptr[0], ptr[1], ptr[2], ptr[3], ptr[4], ptr[5], ptr[6], ptr[7] );
    }
}
#endif /* MPEAWT_DBGIMGDUMP */

JNIEXPORT void JNICALL Java_java_awt_MPEImage_pDrawImage
(JNIEnv * env, jclass cls, jint _gfx, jint _surf, jint x, jint y, jint bg)
{
    mpe_GfxContext gfx = (mpe_GfxContext)_gfx;
    mpe_GfxSurface surf = (mpe_GfxSurface)_surf;
    mpe_GfxSurfaceInfo info;
    mpe_GfxRectangle bounds;
    MPEAWT_TIME_INIT();
    UNUSED(env);
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEImage::pDrawImage(%d,%d)\n", x, y );

    if (surf == NULL_SURFACE
            || mpe_gfxSurfaceGetInfo(surf, &info) != MPE_SUCCESS)
    return;

    bounds.width = info.dim.width;
    bounds.height = info.dim.height;
    if (bg != 0)
    {
        /* Clear bg */
        bounds.x = x;
        bounds.y = y;
        mpe_gfxClearRect(gfx, &bounds, bg);
    }

#if MPEAWT_DBGIMGDUMP
    dumpPixels(&info, 0, 0, "Src");
    {
        mpe_GfxSurface dest;
        mpe_GfxSurfaceInfo dinfo;
        if (mpe_gfxGetSurface(gfx, &dest) == MPE_SUCCESS &&
                mpe_gfxSurfaceGetInfo(dest, &dinfo) == MPE_SUCCESS)
        {
            dumpPixels(&dinfo, x, y, "Dest");
        }
    }
#endif

    bounds.x = bounds.y = 0;

    MPEAWT_TIME_START();

    if (mpe_gfxBitBlt(gfx, surf, x, y, &bounds) != MPE_SUCCESS)
    {
        /* Don't REALLY want to throw anything... but should log error */
        MPE_LOG( MPEAWT_LOGFAIL, MPEAWT_LOG_MOD, "MPEImage::pDrawImage(%d,%d) FAILED!\n", x, y );
    }

    MPEAWT_TIME_END();
#if MPEAWT_DBGTIME
    MPE_LOG( MPEAWT_LOGTIME, MPEAWT_LOG_MOD, "MPEImage::drawImage elapsed %d ms\n", time_elapsed );
#endif
#if MPEAWT_DBGIMGDUMP
    {
        mpe_GfxSurface dest;
        mpe_GfxSurfaceInfo dinfo;
        if (mpe_gfxGetSurface(gfx, &dest) == MPE_SUCCESS &&
                mpe_gfxSurfaceGetInfo(dest, &dinfo) == MPE_SUCCESS)
        {
            dumpPixels(&dinfo, x, y, "Result");
        }
    }
#endif
    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEImage::pDrawImage() - done\n" );
}

JNIEXPORT void JNICALL Java_java_awt_MPEImage_pDrawImageScaled
(JNIEnv * env, jclass cls, jint _gfx, jint dx1, jint dy1, jint dx2, jint dy2,
        jint _surf, jint sx1, jint sy1, jint sx2, jint sy2, jint bg)
{
    mpe_GfxContext gfx = (mpe_GfxContext)_gfx;
    mpe_GfxSurface surf = (mpe_GfxSurface)_surf;
    mpe_GfxSurfaceInfo info;
    mpe_GfxRectangle srect, drect;
    MPEAWT_TIME_INIT();
    UNUSED(env);
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD,
            "MPEImage::pDrawImageScaled(%d,%d,%d,%d,%d,%d,%d,%d,%08x)\n",
            dx1, dy1, dx2, dy2,
            sx1, sy1, sx2, sy2,
            bg );

    if (surf == NULL_SURFACE
            || mpe_gfxSurfaceGetInfo(surf, &info) != MPE_SUCCESS)
    return;

    if (bg != 0)
    {
        /* Clear bg */
        if (dx2 < dx1)
        {
            drect.x = dx2;
            drect.width = dx1 - dx2 + 1;
        }
        else
        {
            drect.x = dx1;
            drect.width = dx2 - dx1 + 1;
        }

        if (dy2 < dy1)
        {
            drect.y = dy2;
            drect.height = dy1 - dy2 + 1;
        }
        else
        {
            drect.y = dy1;
            drect.height = dy2 - dy1 + 1;
        }

        mpe_gfxClearRect(gfx, &drect, bg);
    }
#if MPEAWT_DBGIMGDUMP
    dumpPixels(&info, 0, 0, "Src");
    {
        mpe_GfxSurface dest;
        mpe_GfxSurfaceInfo dinfo;
        if (mpe_gfxGetSurface(gfx, &dest) == MPE_SUCCESS &&
                mpe_gfxSurfaceGetInfo(dest, &dinfo) == MPE_SUCCESS)
        {
            dumpPixels(&dinfo, dx1, dy1, "Dest");
        }
    }
#endif

    drect.x = dx1;
    drect.y = dy1;
    drect.width = dx2 - dx1;
    drect.height = dy2 - dy1;
    srect.x = sx1;
    srect.y = sy1;
    srect.width = sx2 - sx1;
    srect.height = sy2 - sy1;

    MPEAWT_TIME_START();

    if (mpe_gfxStretchBlt(gfx, surf, &drect, &srect))
    {
        /* Trace errors, but don't throw anything */
        MPE_LOG( MPEAWT_LOGFAIL, MPEAWT_LOG_MOD, "MPEImage::pDrawImageScaled(%d,%d,%d,%d,%d,%d,%d,%d,%08x) FAILED!\n",
                dx1, dy1, dx2, dy2,
                sx1, sy1, sx2, sy2,
                bg );
    }

    MPEAWT_TIME_END();
#if MPEAWT_DBGTIME
    MPE_LOG( MPEAWT_LOGTIME, MPEAWT_LOG_MOD, "MPEImage::drawScaledImage elapsed %d ms\n", time_elapsed );
#endif

#if MPEAWT_DBGIMGDUMP
    {
        mpe_GfxSurface dest;
        mpe_GfxSurfaceInfo dinfo;
        if (mpe_gfxGetSurface(gfx, &dest) == MPE_SUCCESS &&
                mpe_gfxSurfaceGetInfo(dest, &dinfo) == MPE_SUCCESS)
        {
            dumpPixels(&dinfo, dx1, dy1, "Result");
        }
    }
#endif

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEImage::pDrawImageScaled - done\n" );
}

JNIEXPORT void JNICALL Java_java_awt_MPESurface_pDispose
(JNIEnv * env, jclass cls, jint _surf)
{
    mpe_GfxSurface surf = (mpe_GfxSurface)_surf;
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEImage::pDispose(%08x)\n", surf );

    if (mpe_gfxSurfaceDelete(surf) != MPE_SUCCESS)
    {
        (*env)->ThrowNew (env, MPECachedIDs.AWTError, "Could not delete surface");
    }
}

