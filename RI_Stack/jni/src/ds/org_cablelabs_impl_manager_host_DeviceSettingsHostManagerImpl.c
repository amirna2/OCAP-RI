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

#include <org_cablelabs_impl_manager_host_DeviceSettingsHostManagerImpl.h>
#include "jni_util.h"
#include <mpe_disp.h>
#include <mpe_snd.h>

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_host_DeviceSettingsHostManagerImpl_nRegisterAsync(
        JNIEnv *env, jclass cls, jobject jEdListenerObj)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_EdEventInfo *edHandle = NULL;

    MPE_UNUSED_PARAM(env);

    /* register for async display events */
    if (jEdListenerObj != NULL)
    {
        /* create an ED handle for this listener */
        err = mpe_edCreateHandle(jEdListenerObj, MPE_ED_QUEUE_NORMAL, NULL,
                MPE_ED_TERMINATION_EVCODE, MPE_DISP_EVENT_SHUTDOWN, &edHandle);
        if (err != MPE_SUCCESS)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_JNI,
                    "DeviceSettingsHostManagerImpl_nRegisterAsync couldn't create edHandle: err 0x%x\n",
                    err);
        }
        else if (NULL != edHandle)
        {
            err = mpe_dispRegister(edHandle->eventQ, edHandle);
            if (err != MPE_SUCCESS)
            {
                MPE_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_JNI,
                        "DeviceSettingsHostManagerImpl_nRegisterAsync couldn't register async listener: err 0x%x\n",
                        err);
                MPE_LOG(
                        MPE_LOG_DEBUG,
                        MPE_MOD_JNI,
                        "DeviceSettingsHostManagerImpl_nRegisterAsync: edHandle = 0x%p\n",
                        edHandle);

                mpe_edDeleteHandle(edHandle);
                edHandle = NULL;
            }
        }
    }
    else
    {
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_JNI,
                "DeviceSettingsHostManagerImpl_nRegisterAsync bad jEdListnerObj paramter: %p\n",
                jEdListenerObj);
    }

    /* return the ED Handle to Java */
    return (jint) edHandle;
}

JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_host_DeviceSettingsHostManagerImpl_nUnregisterAsync
(JNIEnv *env, jclass cls, jint jEdHandle)
{
    mpe_Error err;
    mpe_EdEventInfo *edHandle = (mpe_EdEventInfo*)jEdHandle;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    /* call MPE to unregister from async events */
    if (edHandle != NULL)
    {
        err = mpe_dispUnregister(edHandle->eventQ, edHandle);
        if (err != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "DeviceSettingsHostManagerImpl_nUnregisterAsync unregister async listener: err 0x%x\n", err);
        }
    }
    else
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "DeviceSettingsHostManagerImpl_nUnregisterAsync bad jEdHandle paramter: %p\n", edHandle);
    }
}
