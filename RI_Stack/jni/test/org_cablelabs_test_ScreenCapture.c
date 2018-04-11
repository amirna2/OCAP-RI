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

#include "org_cablelabs_test_ScreenCapture.h"
#include <mpe_gfx.h>
#include <mpe_disp.h>
#include <mpe_os.h>
#include <mpe_types.h>
#include <mpe_error.h>

static mpe_DispDevice getDefaultGfx(void)
{
    static volatile int tried; /**< Avoid trying more than once */
    static volatile mpe_DispDevice gfx;
    if (!tried && NULL == (void*) gfx)
    {
        /* Get "default" screen */
        uint32_t nScreens = 0;
        mpe_DispScreen *screens = NULL;

        if (MPE_SUCCESS == mpe_dispGetScreenCount(&nScreens) && nScreens > 0
                && MPE_SUCCESS == mpe_memAllocP(MPE_MEM_TEMP, nScreens
                        * sizeof(mpe_DispScreen), (void**) &screens))
        {
            uint32_t nDevs = 0;
            mpe_DispDevice *devices = NULL;
            if (MPE_SUCCESS == mpe_dispGetScreens(screens) && MPE_SUCCESS
                    == mpe_dispGetDeviceCount(screens[0],
                            MPE_DISPLAY_GRAPHICS_DEVICE, &nDevs) && nDevs > 0
                    && MPE_SUCCESS == mpe_memAllocP(MPE_MEM_TEMP, nDevs
                            * sizeof(mpe_DispDevice), (void**) &devices))
            {
                if (MPE_SUCCESS == mpe_dispGetDevices(screens[0],
                        MPE_DISPLAY_GRAPHICS_DEVICE, devices))
                {
                    gfx = devices[0];
                }
            }
            if (devices != NULL)
                mpe_memFreeP(MPE_MEM_TEMP, devices);
        }
        if (screens != NULL)
            mpe_memFreeP(MPE_MEM_TEMP, screens);
        tried = 1;
    }
    return gfx;
}

static mpe_Error getDefaultGfxSurface(mpe_GfxSurface* surface)
{
    mpe_DispDevice gfx = getDefaultGfx();

    if (NULL == gfx)
        return MPE_DISP_ERROR_UNKNOWN;
    return mpe_dispGetGfxSurface(gfx, surface);
}

/**
 *  Screen capture API intended to be used by Havi test framework.
 *  Create an offscreen surface, do a blit copy from the screen surface to the offscreen,
 *  and use SurfaceGetInfo to get at the pixels.
 */
JNIEXPORT jbyteArray JNICALL Java_org_cablelabs_test_ScreenCapture_nativeCapture(
        JNIEnv *env, jobject obj, jint startX, jint startY, jint width,
        jint height)
{
    mpe_GfxSurface onScreenSurf = NULL;
    mpe_GfxSurface offScreenSurf;
    mpe_GfxContext ctx;
    mpe_GfxRectangle rect;
    mpe_GfxSurfaceInfo info;
    jbyteArray pixelArray;
    jint sz;
    jboolean isCopy;
    jbyte* pixelElements;
    mpe_Error mpeError;

    MPE_UNUSED_PARAM(obj);

    mpeError = getDefaultGfxSurface(&onScreenSurf);
    if ((onScreenSurf == NULL) || (mpeError != MPE_DISP_ERROR_NO_ERROR))
    {
        return NULL;
    }

    rect.x = startX;
    rect.y = startY;
    rect.width = width;
    rect.height = height;

    mpeError = mpe_gfxSurfaceCreate(onScreenSurf, &offScreenSurf);
    if ((offScreenSurf == NULL) || (mpeError != MPE_DISP_ERROR_NO_ERROR))
        return NULL;

    mpeError = mpe_gfxContextNew(offScreenSurf, &ctx);
    if ((ctx == NULL) || (mpeError != MPE_SUCCESS))
        return NULL;

    if (mpe_gfxBitBlt(ctx, onScreenSurf, startX, startY, &rect) != MPE_SUCCESS)
        return NULL;

    if (mpe_gfxSurfaceGetInfo(offScreenSurf, &info) != MPE_SUCCESS)
        return NULL;

#if 0 /* Enable these printf's for debugging only: */
    printf("info.bpp = %d\n", info.bpp);
    printf("info.dim.width = %d\n", info.dim.width);
    printf("info.dim.height= %d\n", info.dim.height);
    printf("info.format = %d\n", info.format);
    printf("info.widthbytes = %d\n", info.widthbytes );
#endif

    sz = info.dim.width * info.dim.height * (info.bpp / 8);
    pixelArray = (*env)->NewByteArray(env, sz + 2); // 2 bytes - 1 for bpp and 1 for format
    pixelElements = (*env)->GetByteArrayElements(env, pixelArray, &isCopy);
    pixelElements[0] = (jbyte)(info.bpp & 0xFF);
    pixelElements[1] = (jbyte)(info.format & 0xFF);
    if (isCopy == JNI_TRUE)
        (*env)->ReleaseByteArrayElements(env, pixelArray, pixelElements, 0);

    (*env)->SetByteArrayRegion(env, pixelArray, 2, sz, (jbyte*) info.pixeldata);

    return pixelArray;
}
