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

#include <org_cablelabs_impl_ocap_hardware_VideoOutputPortImpl.h>
#include "jni_util.h"
#include <mpe_disp.h>

/**
 * Initializes JNI.
 */
JNIEXPORT void JNICALL
Java_org_cablelabs_impl_ocap_hardware_VideoOutputPortImpl_nInit(JNIEnv *env,
        jclass cls)
{
    /* Lookup fields in VideoOutputPortImpl */
    GET_FIELD_ID(VideoOutputPortImpl_type, "type", "I");
    GET_FIELD_ID(VideoOutputPortImpl_hdcp, "hdcp", "Z");
    GET_FIELD_ID(VideoOutputPortImpl_dtcp, "dtcp", "Z");
    GET_FIELD_ID(VideoOutputPortImpl_restrictedResolution, "restrictedResolution", "I");
    GET_FIELD_ID(VideoOutputPortImpl_pixelResolution, "pixelResolution", "Ljava/awt/Dimension;");

    /* Dimension */
    GET_CLASS(Dimension, "java/awt/Dimension");
    GET_FIELD_ID(Dimension_width, "width", "I");
    GET_FIELD_ID(Dimension_height, "height", "I");
}

/**
 * Creates a new array containing native interface handles for the
 * known video output ports.
 *
 * @return a new array containing native interface handles for the
 * known video output ports
 */
JNIEXPORT jintArray JNICALL
Java_org_cablelabs_impl_ocap_hardware_VideoOutputPortImpl_nGetVideoOutputPorts(JNIEnv *env,
        jclass cls)
{
    uint32_t n;
    mpe_Error err;
    JNI_UNUSED(cls);

    /* Figure number of ports */
    if (MPE_SUCCESS == (err = mpe_dispGetOutputPortCount(&n)))
    {
        /* Allocate an array of sufficient size */
        jintArray jArray = (*env)->NewIntArray(env, n);
        jint* array;

        if (jArray == NULL)
        {
            return NULL;
        }

        /* Get access to the array */
        array = (*env)->GetIntArrayElements(env, jArray, NULL);

        /* Make the MPE call */
        err = mpe_dispGetOutputPorts((mpe_DispOutputPort*)array);
        (*env)->ReleaseIntArrayElements(env, jArray, array, 0);

        /* Return array on success */
        if (MPE_SUCCESS == err)
        {
            return jArray;
        }
    }

    /* Return NULL on failure */
    return NULL;
}

/**
 * Enables or disables the given video output port.
 *
 * @param handle native video output port handle
 * @param enable if <code>true</code> then enable the port;
 * if <code>false</code> then disable the port
 */
JNIEXPORT jint JNICALL
Java_org_cablelabs_impl_ocap_hardware_VideoOutputPortImpl_nEnable(JNIEnv *env,
        jclass cls,
        jint handle,
        jboolean enable)
{
    mpe_DispOutputPort port = (mpe_DispOutputPort)handle;

    JNI_UNUSED(env);
    JNI_UNUSED(cls);

    return (jint)mpe_dispEnableOutputPort(port, (mpe_Bool)enable);
}

/**
 * Retrieves the status of the video output port.
 *
 * @param handle native video output port handle
 * @return <code>true</code> if the port is enabled; <code>false</code>
 * otherwise
 */
JNIEXPORT jboolean JNICALL
Java_org_cablelabs_impl_ocap_hardware_VideoOutputPortImpl_nGetStatus(JNIEnv *env,
        jclass cls,
        jint handle)
{
    mpe_DispOutputPort port = (mpe_DispOutputPort)handle;
    mpe_DispOutputPortInfo info;
    JNI_UNUSED(env);
    JNI_UNUSED(cls);

    if (MPE_SUCCESS != mpe_dispGetOutputPortInfo(port, &info))
    {
        return JNI_FALSE;
    }
    return (jboolean)(info.enabled ? JNI_TRUE : JNI_FALSE);
}

/**
 * Initializes this <code>VideoOutputPortImpl</code> with information
 * about the native video output port.
 * This includes the following (which may be expanded as necessary):
 * <ul>
 * <li> type
 * <li> dtcp
 * <li> hdcp
 * <li> restrictedResolution
 * </ul>
 *
 * @param handle native video output port handle
 * @return <code>true</code> if successful; <code>false</code> otherwise
 */
JNIEXPORT jboolean JNICALL
Java_org_cablelabs_impl_ocap_hardware_VideoOutputPortImpl_nInitInfo(JNIEnv *env,
        jobject obj,
        jint handle)
{
    mpe_DispOutputPort port = (mpe_DispOutputPort)handle;
    mpe_DispOutputPortInfo info;
    mpe_DispVideoDisplayAttrInfo displayAttributes;

    int i=0, j=0;

    unsigned int fixedConfigInfoCount;
    unsigned int dynamicConfigInfoCount;
    unsigned int curConfigHandle;
    mpe_Bool configFound = false;
    mpe_Bool displayAttributesAvailable = true;
    mpe_Error err;

    if (MPE_SUCCESS != mpe_dispGetOutputPortInfo(port, &info))
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "jni:InitInfo--mpe_dispGetOutputPortInfo failed--port = %p\n", port);
        return JNI_FALSE;
    }

    err = mpe_dispGetDisplayAttributes(port, &displayAttributes);
    if (MPE_SUCCESS != err && MPE_DISP_ERROR_NOT_AVAILABLE != err)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "jni:InitInfo--mpe_dispGetDisplayAttributes failed--port = %p\n", port);
        return JNI_FALSE;
    }
    
    if (MPE_DISP_ERROR_NOT_AVAILABLE == err)
    {
        displayAttributesAvailable = false;
    }

    jint type;
    jobject field;

    /* Update the boolean fields. */
    (*env)->SetBooleanField(env, obj, jniutil_CachedIds.VideoOutputPortImpl_hdcp, JNI_ISTRUE(info.hdcpSupported));
    (*env)->SetBooleanField(env, obj, jniutil_CachedIds.VideoOutputPortImpl_dtcp, JNI_ISTRUE(info.dtcpSupported));

    /* Update integer fields. */
    (*env)->SetIntField(env, obj, jniutil_CachedIds.VideoOutputPortImpl_restrictedResolution, info.restrictedResolution);

    /* Update the pixelResolution Dimension */
    field = (*env)->GetObjectField(env, obj, jniutil_CachedIds.VideoOutputPortImpl_pixelResolution);
    if (field == NULL)
    {
        return JNI_FALSE;
    }

    if (MPE_SUCCESS != mpe_dispGetSupportedFixedVideoOutputConfigurationCount(port, &fixedConfigInfoCount))
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "jni:InitInfo--mpe_dispGetSupportedFixedVideoOutputConfigurationCount failed--port = %p\n", port);
        return JNI_FALSE;
    } 

    // Get count of number of dynamic vid ouput configs
    if (MPE_SUCCESS != mpe_dispGetSupportedDynamicVideoOutputConfigurationCount(port, &dynamicConfigInfoCount))
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "jni:InitInfo--mpe_dispGetSupportedDynamicVideoOutputConfigurationCount failed--port = %p\n", port);
        return JNI_FALSE;
    }

                
    curConfigHandle = (unsigned int) (info.fixedConfigInfo->curConfig);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jni:InitInfo: curConfig = %x\n", curConfigHandle);

    // the following code determined whether the curConfig is a fixedConfig or a dynamicConfig.  There must be an easier way to do this...
    for (i=0; i<fixedConfigInfoCount; i++)
    {
        unsigned int fixedConfigHandle = (unsigned int) (info.fixedConfigInfo->fixedConfigs + i);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jni:InitInfo: fixedConfig[%d] = %x\n", i, fixedConfigHandle);
        if (curConfigHandle == fixedConfigHandle)
        {
            mpe_DispFixedVideoOutputConfigInfo* pVideoOutputConfig = (mpe_DispFixedVideoOutputConfigInfo *)info.fixedConfigInfo->curConfig;
            jniutil_setDimension(env, field,
                    (jint) pVideoOutputConfig->resolution->pixelResolution.width,
                    (jint) pVideoOutputConfig->resolution->pixelResolution.height);
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jni:InitInfo--pixelResolution set to (%d, %d) (fixed config)\n", 
                pVideoOutputConfig->resolution->pixelResolution.width, pVideoOutputConfig->resolution->pixelResolution.height);
            configFound = true;
            break;
        }
    }

    if (!configFound)
    {
        for (i=0; i<dynamicConfigInfoCount; i++)
        {
            // GORP: what to do here if the display attribute is not available?
            float currentDisplayAR = 0.0;
            if (displayAttributesAvailable)
            {
                currentDisplayAR = displayAttributes.aspectRatio.width / displayAttributes.aspectRatio.height;
            }

            unsigned int dynamicConfigHandle = (unsigned int) (info.fixedConfigInfo->dynamicConfigs + i*sizeof(mpe_DispDynamicVideoOutputConfigInfo*));
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jni:InitInfo: dynamicConfigInfoArray = %x\n", dynamicConfigHandle);
            if (curConfigHandle == dynamicConfigHandle)
            {
                configFound = true;
                mpe_DispDynamicVideoOutputConfigInfo* pVideoOutputConfig = (mpe_DispDynamicVideoOutputConfigInfo *)info.fixedConfigInfo->curConfig;
                for (j=0; j<pVideoOutputConfig->mappingsCount; j++)
                {
                    mpe_GfxDimensions displayAspectRatio = pVideoOutputConfig->mappings[j].inputResolution->aspectRatio;
                    float displayAR = displayAspectRatio.width / displayAspectRatio.height;
                    if (currentDisplayAR == displayAR)
                    {
                        mpe_DispFixedVideoOutputConfigInfo* pFixedVideoOutputConfig = pVideoOutputConfig->mappings[j].outputResolution;
                        jniutil_setDimension(env, field,
                                (jint) pFixedVideoOutputConfig->resolution->pixelResolution.width,
                                (jint) pFixedVideoOutputConfig->resolution->pixelResolution.height);
                        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jni:InitInfo--pixelResolution set to (%d, %d) (dynamic config)\n", 
                            pFixedVideoOutputConfig->resolution->pixelResolution.width, pFixedVideoOutputConfig->resolution->pixelResolution.height);
                        break;
                    }
                }
                break;
            }
        }
    }

    if (!configFound)
    {
        return JNI_FALSE;
    }


    switch(info.type)
    {
        case MPE_DISPLAY_RF_PORT:
            type = org_cablelabs_impl_ocap_hardware_VideoOutputPortImpl_AV_OUTPUT_PORT_TYPE_RF;
            break;
        case MPE_DISPLAY_BASEBAND_PORT:
            type = org_cablelabs_impl_ocap_hardware_VideoOutputPortImpl_AV_OUTPUT_PORT_TYPE_BB;
            break;
        case MPE_DISPLAY_SVIDEO_PORT:
            type = org_cablelabs_impl_ocap_hardware_VideoOutputPortImpl_AV_OUTPUT_PORT_TYPE_SVIDEO;
            break;
        case MPE_DISPLAY_1394_PORT:
            type = org_cablelabs_impl_ocap_hardware_VideoOutputPortImpl_AV_OUTPUT_PORT_TYPE_1394;
            break;
        case MPE_DISPLAY_DVI_PORT:
            type = org_cablelabs_impl_ocap_hardware_VideoOutputPortImpl_AV_OUTPUT_PORT_TYPE_DVI;
            break;
        case MPE_DISPLAY_COMPONENT_PORT:
            type = org_cablelabs_impl_ocap_hardware_VideoOutputPortImpl_AV_OUTPUT_PORT_TYPE_COMPONENT_VIDEO;
            break;
        case MPE_DISPLAY_HDMI_PORT:
            type = org_cablelabs_impl_ocap_hardware_VideoOutputPortImpl_AV_OUTPUT_PORT_TYPE_HDMI;
            break;
        case MPE_DISPLAY_INTERNAL_PORT:
            type = org_cablelabs_impl_ocap_hardware_VideoOutputPortImpl_AV_OUTPUT_PORT_TYPE_INTERNAL;
            break;
        default:
            return JNI_FALSE;
    }
    (*env)->SetIntField(env, obj, jniutil_CachedIds.VideoOutputPortImpl_type, type);


    return JNI_TRUE;
}

