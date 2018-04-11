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

#include <org_cablelabs_impl_manager_system_SASSystemModule.h>
#include <jni.h>
#include <mpe_pod.h>
#include <mpe_os.h>
#include <mpe_types.h>

/*
 * Class:     org_cablelabs_impl_manager_system_SASSystemModule
 * Method:    podSASConnect
 * Signature: ([B)S
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_system_SASSystemModule_podSASConnect(
        JNIEnv *env, jobject jthis, jbyteArray appId)
{
    jint sessionId = 0;
    uint16_t resourceVersion = 2;
    jbyte cAppId[8];

    MPE_UNUSED_PARAM(jthis);

    // Get the private host application identifier.
    (*env)->GetByteArrayRegion(env, appId, 0, 8, cAppId);

    // Attempt to open the session.
    if (mpe_podSASConnect((uint8_t*) cAppId, (uint32_t*) &sessionId, &resourceVersion)
            != MPE_SUCCESS)
    {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/Error"),
                "Cannot connect to POD application");
    }

    return sessionId;
}

/* Inaccessible static: validTags */
/*
 * Class:     org_cablelabs_impl_manager_system_SASSystemModule
 * Method:    podSASSendAPDU
 * Signature: ([B)V
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_manager_system_SASSystemModule_podSASSendAPDU(
        JNIEnv *env, jobject jthis, jint sessionId, jint apduTag,
        jbyteArray apdu)
{
    jboolean response; /* Update operation response. */
    uint32_t length = 0; /* Length of array. */
    uint8_t *buffer = NULL; /* Pointer to native array allocated. */

    MPE_UNUSED_PARAM(jthis);

    /* If there is APDU data, get size of APDU data array. */
    if ((NULL != apdu) && ((length = (*env)->GetArrayLength(env, apdu)) != 0))
    {
        /* Allocate native buffer for APDU. */
        if (mpe_memAllocP(MPE_MEM_POD, length, (void**) &buffer) != MPE_SUCCESS)
            return false;

        /* Now copy the byte array data to the native buffer. */
        (*env)->GetByteArrayRegion(env, apdu, 0, length, (jbyte*) buffer);
    }

    /* Call MPE to set feature parameter according to type. */
    response = (jboolean)(mpe_podSendAPDU((uint32_t) sessionId, apduTag,
            (uint32_t) length, buffer) == MPE_SUCCESS);

    /* Return temporary buffer. */
    if (NULL != buffer)
        mpe_memFreeP(MPE_MEM_POD, buffer);

    /* Return the response. */
    return response;
}

