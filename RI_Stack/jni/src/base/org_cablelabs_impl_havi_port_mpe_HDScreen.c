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

#include "org_cablelabs_impl_havi_port_mpe_HDScreen.h"
#include "jni_util.h"
#include <mpe_disp.h>
#include <platform.h>
#include <mpe_os.h>

/**
 * Perform any necessary JNI initialization.
 */
JNIEXPORT void JNICALL
Java_org_cablelabs_impl_havi_port_mpe_HDScreen_nInit(JNIEnv *env,
        jclass cls)
{
    /* Dimension */
    GET_CLASS(Dimension, "java/awt/Dimension");
    GET_FIELD_ID(Dimension_width, "width", "I");
    GET_FIELD_ID(Dimension_height, "height", "I");
}

/**
 * Returns a new array which contains <code>int</code>s representing
 * the native screen handles for the system.
 *
 * @return a new array which contains <code>int</code>s representing
 * the native screen handles for the system.
 */
JNIEXPORT jintArray JNICALL
Java_org_cablelabs_impl_havi_port_mpe_HDScreen_nGetScreens(JNIEnv *env,
        jclass cls)
{
    uint32_t n;
    mpe_Error err;
    JNI_UNUSED(cls);

    /* Figure number of screens */
    if (MPE_SUCCESS == (err = mpe_dispGetScreenCount(&n)))
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
        err = mpe_dispGetScreens((mpe_DispScreen*)array);
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
 * Returns a new array which contains <code>int</code>s representing
 * the native device handles for the given screen of the given type
 *
 * @param screen the screen
 * @param type device type; one of {@link #TYPE_GFX}, {@link #TYPE_BG},
 * {@link #TYPE_VID}
 * @return a new array which contains <code>int</code>s representing
 * the native device handles for the given screen of the given type
 */
JNIEXPORT jintArray JNICALL
Java_org_cablelabs_impl_havi_port_mpe_HDScreen_nGetDevices(JNIEnv *env,
        jclass cls,
        jint screen,
        jint type)
{
    mpe_DispScreen hscreen = (mpe_DispScreen)screen;
    uint32_t n;
    mpe_Error err;
    mpe_DispDeviceType dtype;
    JNI_UNUSED(cls);

    /* Figure proper type being requested. */
    switch (type)
    {
        case org_cablelabs_impl_havi_port_mpe_HDScreen_TYPE_GFX:
        dtype = MPE_DISPLAY_GRAPHICS_DEVICE;
        break;
        case org_cablelabs_impl_havi_port_mpe_HDScreen_TYPE_BG:
        dtype = MPE_DISPLAY_BACKGROUND_DEVICE;
        break;
        case org_cablelabs_impl_havi_port_mpe_HDScreen_TYPE_VID:
        dtype = MPE_DISPLAY_VIDEO_DEVICE;
        break;
        default:
        return NULL; /* error condition */
    }

    /* Figure number of devices. */
    if (MPE_SUCCESS == (err = mpe_dispGetDeviceCount(hscreen, dtype, &n)))
    {
        /* Allocate an array of sufficient size */
        jintArray jArray = (*env)->NewIntArray(env, n);
        jint* array;

        if (jArray == NULL)
        {
            return NULL;
        }
        else if (n == 0)
        {
            return jArray;
        }

        /* Get access to the array */
        array = (*env)->GetIntArrayElements(env, jArray, NULL);

        /* Make the MPE call */
        err = mpe_dispGetDevices(hscreen, dtype, (mpe_DispDevice*)array);
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
 * Returns the id string for the given device.
 * Called by <code>HDGraphicsDevice</code>, <code>HDVideoDevice</code>,
 * <code>HDBackgroundDevice</code>.
 *
 * @param device the native device handle
 */
JNIEXPORT jstring JNICALL
Java_org_cablelabs_impl_havi_port_mpe_HDScreen_nGetDeviceIdString(JNIEnv *env,
        jclass cls,
        jint device)
{
    mpe_DispDevice hdevice = (mpe_DispDevice)device;
    mpe_DispDeviceInfo info;
    JNI_UNUSED(cls);

    if (MPE_SUCCESS == mpe_dispGetDeviceInfo(hdevice, &info))
    {
        /* Create a new string and return it. */
        return(*env)->NewStringUTF(env, (char*)info.idString);
    }
    return NULL;
}

/**
 * Returns the screen aspect ratio of the given device.  This is the aspect ratio of the current
 * configuration and is only valid when mpe_dispGetDeviceInfo is called.
 *
 * Records the screen aspect ratio in the given <code>Dimension</code> object
 * and returns the same <code>Dimension</code> object.
 *
 * @param device the native device handle
 * @param dimension the dimension object to fill with the screen aspect ratio
 * @return the <code>Dimension</code> object that was passed in as <i>dim</i>
 */
JNIEXPORT jobject JNICALL
Java_org_cablelabs_impl_havi_port_mpe_HDScreen_nGetDeviceScreenAspectRatio(JNIEnv *env,
        jclass cls,
        jint device,
        jobject dimension)
{
    mpe_DispDevice hdevice = (mpe_DispDevice)device;
    mpe_DispDeviceInfo info;
    JNI_UNUSED(cls);

    if (MPE_SUCCESS == mpe_dispGetDeviceInfo(hdevice, &info))
    {
        jniutil_setDimension(env, dimension,
                info.screenAspectRatio.width, info.screenAspectRatio.height);
        return dimension;
    }
    return NULL;
}

/**
 * Returns the destination of the given device, i.e., TV or PIP.
 *
 * @param device the native device handle
 */
JNIEXPORT jint JNICALL
Java_org_cablelabs_impl_havi_port_mpe_HDScreen_nGetDeviceDest(JNIEnv *env,
        jclass cls,
        jint device)
{
    mpe_DispDevice hdevice = (mpe_DispDevice)device;
    mpe_DispDeviceDest dest;

    JNI_UNUSED(cls);

    if (MPE_SUCCESS == mpe_dispGetDeviceDest(hdevice, &dest))
    {
        return dest;
    }
    return -1;
}

/**
 * Returns a new array which contains <code>int</code>s representing
 * the native configuration handles for the given device.
 *
 * @param device the device
 * @return a new array which contains <code>int</code>s representing
 * the native configuration handles for the given device
 */
JNIEXPORT jintArray JNICALL
Java_org_cablelabs_impl_havi_port_mpe_HDScreen_nGetDeviceConfigs(JNIEnv *env,
        jclass cls,
        jint device)
{
    mpe_DispDevice hdevice = (mpe_DispDevice)device;
    uint32_t n;
    mpe_Error err;
    JNI_UNUSED(cls);

    /* Figure number of configs. */
    if (MPE_SUCCESS == (err = mpe_dispGetConfigCount(hdevice, &n)))
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
        err = mpe_dispGetConfigs(hdevice, (mpe_DispDeviceConfig*)array);
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
 * Attempts to set the current configuration for <i>device</i> to
 * <i>config</i>.
 *
 * @param device native device handle
 * @param config native configuration handle
 * @return <code>true</code> if the operation failed because it
 * would conflict with another device's configuration;
 * <code>false</code> if the operation was successful
 */
JNIEXPORT jboolean JNICALL
Java_org_cablelabs_impl_havi_port_mpe_HDScreen_nSetDeviceConfig(JNIEnv *env,
        jclass cls,
        jint device,
        jint config)
{
    mpe_DispDevice hdevice = (mpe_DispDevice)device;
    mpe_DispDeviceConfig hconfig = (mpe_DispDeviceConfig)config;
    JNI_UNUSED(env);
    JNI_UNUSED(cls);

    if (MPE_SUCCESS != mpe_dispSetCurrConfig(hdevice, hconfig))
    {
        return JNI_TRUE;
    }
    else
    {
        return JNI_FALSE;
    }
}

/**
 * Returns the current configuration set on the given device.
 *
 * @param device native device handle
 * @return native device handle representing the current configuration
 * for the given device
 */
JNIEXPORT jint JNICALL
Java_org_cablelabs_impl_havi_port_mpe_HDScreen_nGetDeviceConfig(JNIEnv *env, jclass cls, jint device)
{
    mpe_DispDevice hdevice = (mpe_DispDevice)device;
    mpe_DispDeviceConfig hconfig;
    JNI_UNUSED(env);
    JNI_UNUSED(cls);

    if (MPE_SUCCESS != mpe_dispGetCurrConfig(hdevice, &hconfig))
    {
        return(jint)0;
    }
    else
    {
        return(jint)hconfig;
    }
}

/**
 * Returns whether the two pairs of device/configurations are
 * incompatible or not.
 *
 * @param device native device handle representing device to be changed
 * @param config native configuration handle representing new configuration
 * @param device2 native device handle for other device
 * @param config2 native configuration handle for current configuration of
 * <i>device2</i>
 */
JNIEXPORT jboolean JNICALL
Java_org_cablelabs_impl_havi_port_mpe_HDScreen_nWouldImpact(JNIEnv *env,
        jclass cls,
        jint device,
        jint config,
        jint device2,
        jint config2)
{
    mpe_DispDevice hdevice = (mpe_DispDevice)device;
    mpe_DispDeviceConfig hconfig = (mpe_DispDeviceConfig)config;
    mpe_DispDevice hdevice2 = (mpe_DispDevice)device2;
    mpe_DispDeviceConfig hconfig2 = (mpe_DispDeviceConfig)config2;
    mpe_Bool impact;
    JNI_UNUSED(cls);
    JNI_UNUSED(env);

    if (MPE_SUCCESS != mpe_dispWouldImpact(hdevice, hconfig,
                    hdevice2, hconfig2,
                    &impact))
    {
        return JNI_TRUE;
    }
    else
    {
        /*
         * To avoid problems truncating true/false (e.g., if impact==0x10000000),
         * evaluate to determine whether to return true or false.
         */
        return JNI_ISTRUE(impact);
    }
}

/**
 * Returns the native coherent configuration handles supported
 * by the given screen.
 *
 * @param nScreen native screen handle
 */
JNIEXPORT jintArray JNICALL
Java_org_cablelabs_impl_havi_port_mpe_HDScreen_nGetCoherentConfigs(JNIEnv *env,
        jclass cls,
        jint screen)
{
    mpe_DispScreen hscreen = (mpe_DispScreen)screen;
    uint32_t n;
    mpe_Error err;
    JNI_UNUSED(cls);

    /* Figure number of devices. */
    if (MPE_SUCCESS == (err = mpe_dispGetCoherentConfigCount(hscreen, &n)))
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
        err = mpe_dispGetCoherentConfigs(hscreen, (mpe_DispCoherentConfig*)array);
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
 * Sets the background device's single color, replacing any previous
 * color or displayed background image.
 * Should only ever be used for background devices.
 *
 * @param device native bg device
 * @param rgb RGB888 color
 * @return 0 for success, 1 for unsupported operation, 2 for illegal color
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_havi_port_mpe_HDScreen_nSetDeviceBGColor(
        JNIEnv *env, jclass cls, jint device, jint rgb)
{
    JNI_UNUSED(env);
    JNI_UNUSED(cls);

    switch (mpe_dispSetBGColor((mpe_DispDevice) device, (mpe_GfxColor) rgb))
    {
    case MPE_SUCCESS:
        return 0;
    default:
    case MPE_DISP_ERROR_INVALID_PARAM:
        return 1;
    case MPE_DISP_ERROR_BGCOLOR:
        return 2;
    }
}

/**
 * Retrieves the currently set background color.
 * Should only ever be used for background devices.
 *
 * @param device native bg device
 * @return currently set background color (may be different from that
 * passed to {@link #nSetDeviceBGColor}) as RGB888
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_havi_port_mpe_HDScreen_nGetDeviceBGColor(
        JNIEnv *env, jclass cls, jint device)
{
    mpe_Error mpeError;
    mpe_GfxColor color;

    JNI_UNUSED(env);
    JNI_UNUSED(cls);

    mpeError = mpe_dispGetBGColor((mpe_DispDevice) device, &color);
    if (mpeError != MPE_SUCCESS)
        return 0;

    return color;
}

/**
 * Returns the "not contributing" video configuration (which is used
 * when background stills are enabled), if there is one.
 * If there isn't one, zero is returned.
 *
 * @return the "not contributing" video configuration or zero
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_havi_port_mpe_HDScreen_nGetVideoNotContributing(
        JNIEnv *env, jclass cls)
{
    uint32_t n;
    mpe_Error err = (mpe_Error) - 1;
    mpe_DispScreenInfo info =
    { 0 };
    JNI_UNUSED(env);
    JNI_UNUSED(cls);

    /* getScreenInfo requires a screen, so we'll just pick one. */
    /* the notContributing configuration is global, so which one doesn't matter. */

    /* Figure number of screens */
    if (MPE_SUCCESS == mpe_dispGetScreenCount(&n) && n > 0)
    {
        mpe_DispScreen *screens;

        /* Allocate an array of sufficient size */
        if (MPE_SUCCESS != mpe_memAllocP(MPE_MEM_TEMP, n * sizeof(*screens),  (void*) &screens))
        {
            return 0;
        }

        /* Make the MPE call */
        err = mpe_dispGetScreens(screens);
        if (MPE_SUCCESS == err)
        {
            err = mpe_dispGetScreenInfo(screens[0], &info);
        }
        mpe_memFreeP(MPE_MEM_TEMP, screens);
    }
    return (MPE_SUCCESS == err) ? ((jint) info.notContributing) : 0;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_havi_port_mpe_HDScreen_nSetDefaultPlatformDFC(
        JNIEnv *env, jclass cls, jint deviceHandle, jint dfcAction)
{

    JNI_UNUSED(env);
    JNI_UNUSED(cls);

    return mpe_dispSetDefaultPlatformDFC((mpe_DispDevice) deviceHandle,
            (mpe_DispDfcAction) dfcAction);

}
