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

#include <org_ocap_hardware_Host.h>

#include <mpe_ed.h>
#include <mpe_os.h>
#include <mpe_disp.h>
#include "jni_util.h"

/*
 * Class:     org_ocap_hardware_Host
 * Method:    hostReboot
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_ocap_hardware_Host_hostReboot(JNIEnv *env, jclass clazz)
{
    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(clazz);

    /* Initiate reboot of STB. */
    (void)mpe_stbBoot(MPE_BOOTMODE_RESET);
}

/*
 * Class:     org_ocap_hardware_Host
 * Method:    getHostPowerMode
 * Signature: (Lorg/cablelabs/impl/manager/ed/EDListener;)I
 */
JNIEXPORT jint JNICALL Java_org_ocap_hardware_Host_getHostPowerMode(
        JNIEnv *env, jclass clazz, jobject caller)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_EdEventInfo *edHandle;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(clazz);

    /* if we are asked to register for power change events, create a handle and register it */
    if (caller != NULL)
    {
        err = mpe_edCreateHandle(caller, MPE_ED_QUEUE_NORMAL, NULL,
                MPE_ED_TERMINATION_OPEN, 0, &edHandle);
        if (err != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                    "mpe_edCreateHandle() FAILED! -- returned: 0x%x\n", err);
        }
        else
        {
            err = mpe_registerForPowerKey(edHandle->eventQ, (void*) edHandle);
            if (err != MPE_SUCCESS)
            {
                mpe_edDeleteHandle(edHandle);
                MPE_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_JNI,
                        "mpe_registerForPowerKey() FAILED! -- returned: 0x%x\n",
                        err);
            }
        }
    }

    /* Return current power status */
    return (jint) mpe_stbGetPowerStatus();
}

JNIEXPORT jboolean JNICALL Java_org_ocap_hardware_Host_setHostPowerMode(
        JNIEnv *env, jclass clazz, jint mode)
{
    if (MPE_SUCCESS != mpe_stbSetPowerStatus((mpe_PowerStatus) mode))
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "mpe_stbSetPowerStatus failed\n");
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/*
 * Class:     org_ocap_hardware_Host
 * Method:    getHostACOutlet
 * Signature: ()
 */
JNIEXPORT jboolean JNICALL Java_org_ocap_hardware_Host_getHostACOutlet(
        JNIEnv *env, jclass clazz)
{
    mpe_Bool status = FALSE;

    JNI_UNUSED(clazz);

    /* Return current AC outlet status. */
    if (mpe_stbGetAcOutletState(&status) != MPE_SUCCESS)
    {
        (*env)->ThrowNew(env, (*env)->FindClass(env,
                "java/lang/IllegalStateException"),
                "Cannot get the AC outlet state");
    }

    return (status != FALSE) ? (jboolean) JNI_TRUE : (jboolean) JNI_FALSE;
}

/*
 * Class:     org_ocap_hardware_Host
 * Method:    setHostACOutlet
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_ocap_hardware_Host_setHostACOutlet(JNIEnv *env, jclass clazz, jboolean enable)
{
    JNI_UNUSED(clazz);

    /* set the AC outlet status. */
    if (mpe_stbSetAcOutletState(enable) != MPE_SUCCESS)
    {
        (*env)->ThrowNew(env, (*env)->FindClass(env,
                        "java/lang/IllegalStateException"),
                "Cannot set the AC outlet state");
    }
}

/*
 * Class:     org_ocap_hardware_Host
 * Method:    getHostRFBypass
 * Signature: ()V
 *
 * Retrieves the status of RF bypass.
 *
 * @return <code>true</code> if RF bypass is enabled; <code>false</code>
 * otherwise
 */
JNIEXPORT jboolean JNICALL Java_org_ocap_hardware_Host_getHostRFBypass(
        JNIEnv *env, jclass cls)
{
    mpe_Bool state = FALSE;

    JNI_UNUSED(env);
    JNI_UNUSED(cls);

    if (mpe_dispGetRFBypassState(&state) != MPE_SUCCESS)
    {
        state = FALSE;
    }

    return (state != FALSE) ? (jboolean) JNI_TRUE : (jboolean) JNI_FALSE;
}

/*
 * Class:     org_ocap_hardware_Host
 * Method:    setHostRFBypass
 * Signature: ()V
 *
 * Enables or disables RF bypass.
 * @param enable if <code>true</code> RF bypass is enabled,
 * if <code>false</code> RF bypass is disabled
 */
JNIEXPORT void JNICALL Java_org_ocap_hardware_Host_setHostRFBypass(
        JNIEnv *env, jclass cls, jboolean enable)
{
    JNI_UNUSED(cls);
    JNI_UNUSED(env);

    if (mpe_dispSetRFBypassState(enable) != MPE_SUCCESS)
    {
        (*env)->ThrowNew(env, (*env)->FindClass(env,
                        "java/lang/IllegalStateException"),
                "Cannot set the RF-Bypass state");
    }
}
