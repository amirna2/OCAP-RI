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

#include <org_cablelabs_impl_manager_system_mmi_MmiSystemModule.h>
#include <jni.h>
#include <mpe_pod.h>
#include <mpe_dbg.h>

static uint32_t g_mmi_sessionID;
static uint16_t g_mmi_resourceVersion = 1;

static uint32_t g_appinfo_sessionID;
static uint16_t g_appinfo_resourceVersion = 2;

/*
 * Class:     org_cablelabs_impl_manager_system_MMISystemModule
 * Method:    podMMISendAPDU
 * Signature: (II[B)V
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_manager_system_mmi_MmiSystemModule_podMMIAppInfoSendAPDU(
        JNIEnv *env, jobject jthis, jint apduTag, jbyteArray apdu)
{
    mpe_Error  result;
    jbyte     *buffer;
    jsize      length;
    uint32_t   sessionID;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(jthis);

    if (NULL == apdu)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "MmiSystemModule::podMMISendAPDU - apdu is NULL.");
        return false;
    }

    length = (*env)->GetArrayLength(env, apdu);
    if (0 == length)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "MmiSystemModule::podMMISendAPDU - apdu is empty.");
        return false;
    }

    buffer = (*env)->GetByteArrayElements(env, apdu, NULL);
    if (NULL == buffer)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "MmiSystemModule::podMMISendAPDU - could not get apdu data.");
        return false;
    }

    // Select correct session based on tag (MMI or AppInfo)
    sessionID = ((apduTag & 0x0000FF00) == 0x8800) ? g_mmi_sessionID : g_appinfo_sessionID;

    result = mpe_podSendAPDU(sessionID, apduTag, length, (uint8_t*)buffer);
    (*env)->ReleaseByteArrayElements(env, apdu, buffer, 0);
    return (jboolean)(MPE_SUCCESS == result);
}

/*
 * Class:     org_cablelabs_impl_manager_system_MMISystemModule
 * Method:    podMMIConnect
 * Signature: ()I
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_manager_system_mmi_MmiSystemModule_podMMIAppInfoConnect(
        JNIEnv *env, jobject jthis)
{
    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(jthis);

    /* Call mpeos connect method for MMI resource */
    if (MPE_SUCCESS != mpe_podMMIConnect(&g_mmi_sessionID, &g_mmi_resourceVersion))
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "MmiSystemModule::podMMIAppInfoConnect - mpe_podMMIConnect failed.");
        return false;
    }

    /* Call mpeos connect method for App Info resource */
    if (MPE_SUCCESS != mpe_podAIConnect(&g_appinfo_sessionID, &g_appinfo_resourceVersion))
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "MmiSystemModule::podMMIAppInfoConnect - mpe_podAIConnect failed.");
        return false;
    }

    return true;
}

/*
 * Class:     org_cablelabs_impl_manager_system_mmi_MmiSystemModule
 * Method:    getAppInfoResourceVersion
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_system_mmi_MmiSystemModule_getAppInfoResourceVersion
(JNIEnv *env, jclass clazz)
{
    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(clazz);
    
    return g_appinfo_resourceVersion;
}

