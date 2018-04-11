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
#include "MPEImage.h"

#include <mpe_gfx.h>
#include <mpe_types.h>
#include <mpe_error.h>
#include <mpe_os.h>
#include <mpe_disp.h>

#include <jni.h>
#include <java_awt_MPEGraphicsConfiguration.h>
#include <java_awt_image_BufferedImage.h>

#define BAD_SURFACE (mpe_GfxSurface)-1

#ifndef MPE_AWTIMAGE_TYPE
static mpe_GfxSurface getSurfaceInfo(mpe_DispDeviceConfig config,
        mpe_GfxSurfaceInfo *info)
{
    mpe_DispDeviceConfigInfo configInfo;
    mpe_GfxSurface surface = NULL;

    if (MPE_SUCCESS == mpe_dispGetConfigInfo(config, &configInfo)
            && MPE_SUCCESS
                    == mpe_dispGetGfxSurface(configInfo.device, &surface)
            && MPE_SUCCESS == mpe_gfxSurfaceGetInfo(surface, info))
    {
        return surface;
    }
    return NULL;
}
#endif

JNIEXPORT jint JNICALL Java_java_awt_MPEGraphicsConfiguration_createCompatibleImageType
(JNIEnv *env, jobject this, jint width, jint height, jint type, jint rgb)
{
    mpe_GfxSurface surf = BAD_SURFACE;
    mpe_GfxSurfaceInfo info;
#ifndef MPE_AWTIMAGE_TYPE
    mpe_GfxSurface screen;
#endif
    UNUSED(this);
    UNUSED(type);

    /* !!!! Make use of type??? !!!!! */
    /* 
     java_awt_image_BufferedImage_TYPE_INT_ARGB
     java_awt_image_BufferedImage_TYPE_INT_RGB
     java_awt_image_BufferedImage_TYPE_INT_RGB565
     java_awt_image_BufferedImage_TYPE_INT_RGB555
     */
    /* What we return now is what we want... */
    /* Which doesn't match the graphicsConfig's colorModel/type */

    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEGraphicsConfig::createCompatibleImage(%dx%d,%d)\n", width, height, type );

#ifndef MPE_AWTIMAGE_TYPE
    if ( NULL == getSurfaceInfo(&info) )
    {
        (*env)->ThrowNew (env, MPECachedIDs.OutOfMemoryError, "Could not create surface");
    }
#else
    info.format = MPE_AWTIMAGE_TYPE;
    info.bpp = MPE_AWTIMAGE_BPP;
    info.widthbytes = 0;
    info.pixeldata = NULL;
# if MPE_AWTIMAGE==MPE_AWTIMAGE_CLUT8
    info.clut = gOcapClut;
# endif
#endif
    info.dim.width = width;
    info.dim.height = height;
    if (mpe_gfxSurfaceNew(&info, &surf) != MPE_SUCCESS)
    {
        (*env)->ThrowNew (env, MPECachedIDs.OutOfMemoryError, "Could not create surface");
    }
    else
    {
        // Clear the surface to the desired color... 
        mpe_GfxContext ctx;
        if (mpe_gfxContextNew(surf, &ctx) == MPE_SUCCESS)
        {
            mpe_GfxRectangle rect;
            rect.x = 0;
            rect.y = 0;
            rect.width = width;
            rect.height = height;
            // Use clearRect with the given color
            mpe_gfxClearRect(ctx, &rect, rgb);
            // Dispose of the context...
            mpe_gfxContextDelete(ctx);
        }
    }

#if 1
    if (surf != BAD_SURFACE
            && mpe_gfxSurfaceGetInfo(surf, &info) == MPE_SUCCESS)
    {
        char* format;

        switch(info.format)
        {
            case MPE_GFX_RGB888: /* 24bpp no alpha */
            format = "RGB888";
            break;
            case MPE_GFX_RGB565: /* 16bpp no alpha */
            format = "RBG565";
            break;
            case MPE_GFX_ARGB8888: /* 32bpp w/ alpha */
            format = "ARGB32";
            break;
            case MPE_GFX_ARGB1555: /* 16bpp w/ alpha */
            format = "ARGB16";
            break;
            default:
            format = "???";
            break;
        }

        MPE_LOG( MPEAWT_LOGCONFIG, MPEAWT_LOG_MOD, "MPEGraphicsConfig::createCompatibleImageType()\n" );
        MPE_LOG( MPEAWT_LOGCONFIG, MPEAWT_LOG_MOD, "\tsurf    = %08x\n", surf );
        MPE_LOG( MPEAWT_LOGCONFIG, MPEAWT_LOG_MOD, "\tformat  = %s\n", format );
        MPE_LOG( MPEAWT_LOGCONFIG, MPEAWT_LOG_MOD, "\tbpp     = %d\n", info.bpp );
        MPE_LOG( MPEAWT_LOGCONFIG, MPEAWT_LOG_MOD, "\twidth   = %d\n", info.dim.width );
        MPE_LOG( MPEAWT_LOGCONFIG, MPEAWT_LOG_MOD, "\theight  = %d\n", info.dim.height );
    }
#endif
    MPE_LOG( MPEAWT_LOGENTRY, MPEAWT_LOG_MOD, "MPEGraphicsConfig::createCompatibleImage - done\n" );

    return (jint)surf;
}

JNIEXPORT jobject JNICALL Java_java_awt_MPEGraphicsConfiguration_createBufferedImageObject
(JNIEnv *env, jobject this, jobject mpeimage)
{
    UNUSED(this);
    return (*env)->NewObject(env,
            MPECachedIDs.java_awt_image_BufferedImage,
            MPECachedIDs.java_awt_image_BufferedImage_constructor,
            mpeimage);
}

JNIEXPORT jint JNICALL Java_java_awt_MPEGraphicsConfiguration_getScreenWidth
(JNIEnv *env, jobject this, jint cfg)
{
    mpe_Error err;
    mpe_DispDeviceConfigInfo info;
    mpe_DispDeviceConfig config;
    UNUSED(this);

    config = (mpe_DispDeviceConfig)cfg;
    err = mpe_dispGetConfigInfo(config, &info);
    if (err != MPE_SUCCESS)
    {
        (*env)->ThrowNew(env, MPECachedIDs.AWTError, "Error translating config!");
        return 640; /* hard code when error */
    }
    return info.resolution.width;
}

JNIEXPORT jint JNICALL Java_java_awt_MPEGraphicsConfiguration_getScreenHeight
(JNIEnv *env, jobject this, jint cfg)
{
    mpe_Error err;
    mpe_DispDeviceConfigInfo info;
    mpe_DispDeviceConfig config;
    UNUSED(this);

    config = (mpe_DispDeviceConfig)cfg;
    err = mpe_dispGetConfigInfo(config, &info);
    if (err != MPE_SUCCESS)
    {
        (*env)->ThrowNew(env, MPECachedIDs.AWTError, "Error translating config!");
        return 480; /* hard code when error */
    }
    return info.resolution.height;
}

