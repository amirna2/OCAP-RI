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

#include "org_cablelabs_impl_havi_MpegBackgroundImage.h"
#include "jni_util.h"
#include <mpe_disp.h>
#include <mpe_dbg.h>

/**
 * Initializes the JNI layer.
 * Called from static initializer.
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_havi_MpegBackgroundImage_nInit
(JNIEnv *env, jclass cls)
{
    /* Dimension */
    GET_CLASS(Dimension, "java/awt/Dimension");
    GET_FIELD_ID(Dimension_width, "width", "I");
    GET_FIELD_ID(Dimension_height, "height", "I");

    /* Rectangle */
    GET_CLASS(Rectangle, "java/awt/Rectangle");
    GET_FIELD_ID(Rectangle_x, "x", "I");
    GET_FIELD_ID(Rectangle_y, "y", "I");
    GET_FIELD_ID(Rectangle_width, "width", "I");
    GET_FIELD_ID(Rectangle_height, "height", "I");
}

/**
 * Creates the native handle and returns the actual size, after
 * verifying that it is a valid I-Frame.
 *
 * @param data the supposed i-frame
 * @param dimension the <code>Dimension</code> object to fill with the
 * width and height of the image
 * @return the native handle or <code>0</code> if invalid
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_havi_MpegBackgroundImage_nCreate(
        JNIEnv *env, jclass cls, jbyteArray data, jobject dimension)
{
    uint32_t length;
    jbyte* buffer;
    mpe_Error err = MPE_SUCCESS;
    mpe_DispBGImage handle = 0;
    mpe_GfxDimensions size;

    MPE_UNUSED_PARAM(cls);

    if (data == NULL || dimension == NULL)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_JNI,
                "MpegBackgroundImage::nCreate Invalid params!\n");
        return 0;
    }

    /* Get the byte array length */
    length = (*env)->GetArrayLength(env, data);

    /* Get the byte array */
    buffer = (*env)->GetByteArrayElements(env, data, NULL);
    if (buffer == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "MpegBackgroundImage::nCreate Could not access data!\n");
        return 0;
    }

    /* Call mpe_dispBGImageNew(buffer, length, &handle) */
    err = mpe_dispBGImageNew((unsigned char *) buffer, length, &handle);
    /* Release array elements */
    (*env)->ReleaseByteArrayElements(env, data, buffer, 0);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "MpegBackgroundImage::nCreate Could not create %d\n", err);
        return 0;
    }

    /* Call mpe_dispBGImageGetSize(handle, &size) */
    err = mpe_dispBGImageGetSize(handle, &size);
    /* Fill in dimensions. */
    if (err == MPE_SUCCESS)
        jniutil_setDimension(env, dimension, size.width, size.height);
    else
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "MpegBackgroundImage::nCreate Could not getSize %d\n", err);
    }

    /* Return the handle */
    return (jint) handle;
}

/**
 * Deletes the native handle and frees up any resources.
 *
 * @param nHandle the native handle
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_havi_MpegBackgroundImage_nDispose
(JNIEnv *env, jclass cls, jint nHandle)
{
    JNI_UNUSED(env);
    JNI_UNUSED(cls);

    if ( nHandle == 0 )
    {
        MPE_LOG( MPE_LOG_ERROR, MPE_MOD_JNI, "MpegBackgroundImage::nDispose invalid nHandle %08x\n", (int)nHandle );
    }
    else
    {
        (void)mpe_dispBGImageDelete((mpe_DispBGImage)nHandle);
    }
}

/**
 * Displays the native image to the given native device.
 * @param nImage the native image
 * @param nDevice the native device
 * @param rectangle on-screen rectangle
 * @return 0 for success, 1 for unsupported operation, 2 for illegal image
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_havi_MpegBackgroundImage_nDisplay(
        JNIEnv *env, jclass cls, jint nImage, jint nDevice, jobject rectangle)
{
    JNI_UNUSED(env);
    JNI_UNUSED(cls);

    if (nImage == 0)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "MpegBackgroundImage::nDisplay invalid nHandle %08x\n", (int)nImage);
        return 2;
    }
    else
    {
        mpe_GfxRectangle *pArea = NULL;
        mpe_GfxRectangle area;

        if (NULL != rectangle)
        {
            jniutil_getRectangle(env, rectangle, (jint*) &area.x,
                    (jint*) &area.y, (jint*) &area.width, (jint*) &area.height);
            pArea = &area;
        }

        switch (mpe_dispDisplayBGImage((mpe_DispDevice) nDevice,
                (mpe_DispBGImage) nImage, pArea))
        {
        case MPE_SUCCESS:
            return 0;
        case MPE_DISP_ERROR_UNIMPLEMENTED:
            return 1;
        default:
        case MPE_DISP_ERROR_BAD_IFRAME:
            return 2;
        }
    }
}
