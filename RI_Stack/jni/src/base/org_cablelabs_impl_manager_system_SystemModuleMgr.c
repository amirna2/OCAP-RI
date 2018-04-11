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

#include "org_cablelabs_impl_manager_system_SystemModuleMgr.h"
#include "jni_util.h"
#include <mpe_pod.h>

/*
 * Class:     org_cablelabs_impl_manager_system_SystemModuleMgr
 * Method:    nInit
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_system_SystemModuleMgr_nInit
(JNIEnv *env, jclass cls)
{
    GET_CLASS(PodAPDU, "org/cablelabs/impl/manager/system/PodAPDU");
    GET_METHOD_ID(PodAPDU_init, "<init>", "(I[B)V");
}


/**
 * Native method to get the next APDU. This method blocks until data is
 * available.  Note, the session identifier is appended to the tail of the
 * java byte array APDU and passed up so the dispatcher can deliver the APDU
 * to the correct handler.
 *
 * @return the APDU received from the POD
 * @throws Error on any error
 */
JNIEXPORT jobject JNICALL Java_org_cablelabs_impl_manager_system_SystemModuleMgr_podGetNextAPDU(
        JNIEnv *env, jclass clazz)
{
    mpe_Error result; /* Error condition code. */
    jobject apdu = NULL; /* PodAPDU object to return */
    uint32_t sessionId; /* Session identifier for received APDU. */
    uint8_t *cAPDU; /* Pointer to native APDU buffer. */
    jbyteArray jAPDU; /* Byte array to populate with APDU data. */
    int32_t len; /* Length of APDU. */

    MPE_UNUSED_PARAM(clazz);

    /* Get the next available APDU */
    result = mpe_podReceiveAPDU(&sessionId, &cAPDU, &len);
    if (MPE_SUCCESS != result)
    {
        /* Check for notification of a failed APDU send operation. */
        if (MPE_ENODATA == result)
        {
            /* TODO: call sendAPDUFailed... */
        }
        goto error;
    }

    /* Copy APDU data to a new Java array */
    jAPDU = (*env)->NewByteArray(env, len);
    if (NULL != jAPDU)
    {
        (*env)->SetByteArrayRegion(env, jAPDU, 0, len, (jbyte*)cAPDU);
    }

    mpe_podReleaseAPDU(cAPDU);
    cAPDU = NULL;

    if (NULL == jAPDU)
    {
       goto error;
    }

    apdu = (*env)->NewObject(env, jniutil_CachedIds.PodAPDU, jniutil_CachedIds.PodAPDU_init,
                             (jint)sessionId, jAPDU);
    if (NULL == apdu)
    {
        goto error;
    }

    return apdu;

  error:
    (*env)->ThrowNew(env,(*env)->FindClass(env,"java/lang/Error"),
                     "Error retrieving APDU from POD");
    return NULL;
}
