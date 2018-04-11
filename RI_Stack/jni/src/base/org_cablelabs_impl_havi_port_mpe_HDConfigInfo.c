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

#include "org_cablelabs_impl_havi_port_mpe_HDConfigInfo.h"
#include "jni_util.h"
#include <mpe_disp.h>
#include <mpe_dbg.h>

JNIEXPORT void JNICALL
Java_org_cablelabs_impl_havi_port_mpe_HDConfigInfo_nInit(JNIEnv *env, jclass cls)
{
    /* HDConfig */
    GET_CLASS(HDConfigInfo, "org/cablelabs/impl/havi/port/mpe/HDConfigInfo");
    GET_FIELD_ID(HDConfigInfo_interlacedDisplay, "interlacedDisplay", "Z");
    GET_FIELD_ID(HDConfigInfo_flickerFilter, "flickerFilter", "Z");
    GET_FIELD_ID(HDConfigInfo_stillImage, "stillImage", "Z");
    GET_FIELD_ID(HDConfigInfo_changeableSingleColor, "changeableSingleColor", "Z");
    GET_FIELD_ID(HDConfigInfo_pixelResolution, "pixelResolution", "Ljava/awt/Dimension;");
    GET_FIELD_ID(HDConfigInfo_pixelAspectRatio, "pixelAspectRatio", "Ljava/awt/Dimension;");
    GET_FIELD_ID(HDConfigInfo_screenArea, "screenArea", "Lorg/havi/ui/HScreenRectangle;");
    GET_FIELD_ID(HDConfigInfo_screenAspectRatio, "screenAspectRatio", "Ljava/awt/Dimension;");
    //GET_FIELD_ID(HDConfigInfo_platformDfc,              "platformDfc",           "I");

    /* Dimension */
    GET_CLASS(Dimension, "java/awt/Dimension");
    GET_FIELD_ID(Dimension_width, "width", "I");
    GET_FIELD_ID(Dimension_height, "height", "I");

    /* HScreenRectangle */
    GET_CLASS(HScreenRectangle, "org/havi/ui/HScreenRectangle");
    GET_FIELD_ID(HScreenRectangle_x, "x", "F");
    GET_FIELD_ID(HScreenRectangle_y, "y", "F");
    GET_FIELD_ID(HScreenRectangle_width, "width", "F");
    GET_FIELD_ID(HScreenRectangle_height, "height", "F");
}

/**
 * Initializes the contents of this <code>HDConfigInfo</code>
 * using information discovered at the native level about the
 * native device configuration handle.
 *
 * @param nConfig the native configuration handle
 * @return <code>true</code> for success; <code>false</code> for failure
 */
JNIEXPORT jboolean JNICALL
Java_org_cablelabs_impl_havi_port_mpe_HDConfigInfo_nInitConfigInfo(JNIEnv *env, jobject infoObj, jint config)
{
    mpe_DispDeviceConfig hconfig = (mpe_DispDeviceConfig)config;
    mpe_DispDeviceConfigInfo info;

    /* Get the configuration information */
    if ( MPE_SUCCESS != mpe_dispGetConfigInfo(hconfig, &info) )
    return JNI_FALSE;
    else
    {
        jobject field;

        /* Update the boolean fields */
        (*env)->SetBooleanField(env, infoObj, jniutil_CachedIds.HDConfigInfo_interlacedDisplay, JNI_ISTRUE(info.interlaced));
        (*env)->SetBooleanField(env, infoObj, jniutil_CachedIds.HDConfigInfo_flickerFilter, JNI_ISTRUE(info.flickerFilter));
        (*env)->SetBooleanField(env, infoObj, jniutil_CachedIds.HDConfigInfo_stillImage, JNI_ISTRUE(info.mpegStills));
        (*env)->SetBooleanField(env, infoObj, jniutil_CachedIds.HDConfigInfo_changeableSingleColor, JNI_ISTRUE(info.changeableColor));

        /* update the int fields */
        //(*env)->SetIntField(env, infoObj, jniutil_CachedIds.HDConfigInfo_platformDfc, info.platformDfc);


        /* Update the pixelResolution Dimension */
        field = (*env)->GetObjectField(env, infoObj, jniutil_CachedIds.HDConfigInfo_pixelResolution);
        if (field == NULL)
        return JNI_FALSE;
        jniutil_setDimension(env, field,
                info.resolution.width, info.resolution.height);

        /* Update the pixelAspectRatio Dimension */
        field = (*env)->GetObjectField(env, infoObj, jniutil_CachedIds.HDConfigInfo_pixelAspectRatio);
        if (field == NULL)
        return JNI_FALSE;
        jniutil_setDimension(env, field,
                info.pixelAspectRatio.width, info.pixelAspectRatio.height);

        /* Update the screenArea HScreenRectangle */
        field = (*env)->GetObjectField(env, infoObj, jniutil_CachedIds.HDConfigInfo_screenArea);
        if (field == NULL)
        return JNI_FALSE;
        jniutil_setHScreenRectangle(env, field,
                info.area.x, info.area.y, info.area.width, info.area.height);

        /* Update the screenAspectRatio Dimension */
        field = (*env)->GetObjectField(env, infoObj, jniutil_CachedIds.HDConfigInfo_screenAspectRatio);
        if (field == NULL)
        return JNI_FALSE;
        jniutil_setDimension(env, field,
                info.screenAspectRatio.width, info.screenAspectRatio.height);

        return JNI_TRUE;
    }
}

