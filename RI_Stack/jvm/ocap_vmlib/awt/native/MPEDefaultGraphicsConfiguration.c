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

#include <jni.h>
#include <java_awt_MPEDefaultGraphicsConfiguration.h>
#include <java_awt_image_BufferedImage.h>

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

JNIEXPORT jobject JNICALL Java_java_awt_MPEDefaultGraphicsConfiguration_createColorModel(
        JNIEnv *env, jclass cls, jint configHandle)
{
    jobject colorModel;
    jint redMask;
    jint greenMask;
    jint blueMask;
    jint alphaMask = 0;
    jint bpp;
    UNUSED(cls);

    MPE_LOG(MPEAWT_LOGENTRY, MPEAWT_LOG_MOD,
            "MPEDefaultGraphicsConfig::createColorModel()\n");
#ifdef MPE_AWTIMAGE_TYPE
    UNUSED(configHandle);
    bpp = MPE_AWTIMAGE_BPP;
    redMask = PIXEL_FORMAT_ARGB(0, 0xFF, 0, 0);
    greenMask = PIXEL_FORMAT_ARGB(0, 0, 0xFF, 0);
    blueMask = PIXEL_FORMAT_ARGB(0, 0, 0, 0xFF);
    alphaMask = PIXEL_FORMAT_ARGB(0xFF, 0, 0, 0);
#else
    {
        mpe_GfxSurfaceInfo info;
        if (NULL == getSurfaceInfo((mpe_DispDeviceConfig) configHandle, &info))
            return NULL;

        bpp = info.bpp;
        switch (info.format)
        {
        case MPE_GFX_RGB888: /* 24bpp no alpha */
            /* BPP24 */
            MPE_LOG(MPEAWT_LOGCONFIG, MPEAWT_LOG_MOD,
                    "MPEGraphicsConfig: RGB888\n");
            redMask = 0xFF0000;
            greenMask = 0xFF00;
            blueMask = 0xFF;
            break;
        case MPE_GFX_RGB565: /* 16bpp no alpha */
            MPE_LOG(MPEAWT_LOGCONFIG, MPEAWT_LOG_MOD,
                    "MPEGraphicsConfig: RGB565\n");
            redMask = 0xF800;
            greenMask = 0x7e0;
            blueMask = 0x1F;
            break;
        case MPE_GFX_ARGB8888: /* 32bpp w/ alpha */
            MPE_LOG(MPEAWT_LOGCONFIG, MPEAWT_LOG_MOD,
                    "MPEGraphicsConfig: ARGB32\n");
            redMask = 0xFF0000;
            greenMask = 0xFF00;
            blueMask = 0xFF;
            alphaMask = 0xFF000000;
            break;
        case MPE_GFX_ARGB1555: /* 16bpp w/ 1 bit alpha */
            MPE_LOG(MPEAWT_LOGCONFIG, MPEAWT_LOG_MOD,
                    "MPEGraphicsConfig: ARGB16\n");
            redMask = 0x7c00;
            greenMask = 0x3e0;
            blueMask = 0x1f;
            alphaMask = 0x1000;
            break;
        default:
            MPE_LOG(MPEAWT_LOGFAIL, MPEAWT_LOG_MOD,
                    "MPEGraphicsConfig: unknown screen format\n");
            return NULL;
        }
    }
#endif

    MPE_LOG(MPEAWT_LOGCONFIG, MPEAWT_LOG_MOD, "\tbpp  = %d\n", bpp);
    MPE_LOG(MPEAWT_LOGCONFIG, MPEAWT_LOG_MOD, "\tred  = %08x\n", redMask);
    MPE_LOG(MPEAWT_LOGCONFIG, MPEAWT_LOG_MOD, "\tblue = %08x\n", redMask);
    MPE_LOG(MPEAWT_LOGCONFIG, MPEAWT_LOG_MOD, "\tgreen= %08x\n", redMask);
    MPE_LOG(MPEAWT_LOGCONFIG, MPEAWT_LOG_MOD, "\talpha= %08x\n", redMask);

    colorModel = (*env)->NewObject(env, MPECachedIDs.DirectColorModel,
            MPECachedIDs.DirectColorModel_constructor, bpp, redMask, greenMask,
            blueMask, alphaMask);
    return colorModel;
}

JNIEXPORT jint JNICALL Java_java_awt_MPEDefaultGraphicsConfiguration_getMPECompatibleImageType(
        JNIEnv *env, jclass cls, jint configHandle)
{
    /* Return desired type for offscreens (32-bit ARGB) */
    UNUSED(env);
    UNUSED(cls);

    MPE_LOG(MPEAWT_LOGENTRY, MPEAWT_LOG_MOD,
            "MPEDefaultGraphicsConfig::getMPECompatibleImageType()\n");

    return java_awt_image_BufferedImage_TYPE_INT_ARGB;
}

