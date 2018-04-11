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

#include <org_cablelabs_impl_manager_pod_PodManagerImpl.h>

#include <mpe_ed.h>
#include <mpe_os.h>
#include <mpe_pod.h>
#include "pod_util.h"
#include "jni_util.h"

void PodImplEdCallback(JNIEnv *env, void *listenerObj,
        mpe_EdEventInfo *edHandle, uint32_t *evCode, void** data1,
        void** data2, uint32_t* data3);
/*
 * Class:     org_cablelabs_impl_manager_pod_PodManagerImpl
 * Method:    nInit
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_pod_PodManagerImpl_nInit
(JNIEnv *env, jclass cls)
{
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "JNI(PODManager): nInit()\n");
    GET_CLASS(CAElementaryStreamAuthorization, "org/cablelabs/impl/manager/pod/CAElementaryStreamAuthorization");
    GET_FIELD_ID(CAElementaryStreamAuthorization_pid, "pid", "I");
    GET_FIELD_ID(CAElementaryStreamAuthorization_reason, "reason", "I");
}

/*
 * Class:     org_ocap_hardware_pod_POD
 * Method:    initPOD
 * Signature: (Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_pod_PodManagerImpl_nativeInitPOD(JNIEnv *env, jclass cls, jobject eventListener)
{
    JNI_UNUSED(env);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,"initPOD()\n");

    static mpe_EdEventInfo* edHandle = NULL;

    /* sanity checks */
    if (eventListener == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR,MPE_MOD_JNI,"initPOD() was not passed and eventListener!  initPOD() failed \n");
        return;
    }

    /* Create ED handle for native->java callbacks regarding changes in POD
     generic features or apps */
    if (mpe_edCreateHandle((void*)eventListener, MPE_ED_QUEUE_NORMAL, NULL, MPE_ED_TERMINATION_EVCODE, MPE_POD_EVENT_SHUTDOWN, &edHandle) == MPE_SUCCESS)
    {
        if (mpe_podRegister(edHandle) != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR,MPE_MOD_JNI,"initPOD() failed to initialize POD event notification!\n");
        }
    }
    else
    {
        MPE_LOG(MPE_LOG_ERROR,MPE_MOD_JNI,"initPOD() failed to create edHandle!\n");
    }
}

/**
 * Class:     org_ocap_hardware_pod_POD
 * Method:    getPODHostFeatures
 * Signature: ()[I
 */
JNIEXPORT jintArray JNICALL Java_org_cablelabs_impl_manager_pod_PodManagerImpl_nativeGetPODHostFeatures(
        JNIEnv *env, jclass clazz)
{
    mpe_PODFeatures *features; /* MPE features structure pointer. */
    jintArray intArr; /* Array reference used to return features. */
    jint temp; /* Temporary used to populate int array. */
    int i; /* Loop index. */

    JNI_UNUSED(clazz);

    /* Call MPE to acquire feature identifier list. */
    if (mpe_podGetFeatures(&features) != MPE_SUCCESS)
    {
        return NULL; /* Return null reference on error. */
    }

    /* Allocate int array of size just determined. */
    if ((intArr = (*env)->NewIntArray(env, features->number)) == NULL)
    {
        return NULL; /* out of memory error thrown */
    }

    /*
     *  Now copy the feature list to the int array.
     */

    /* Now do each individual feature identifiers. */
    for (i = 0; i < features->number; ++i)
    {
        temp = features->featureIds[i];
        (*env)->SetIntArrayRegion(env, intArr, i, 1, &temp);
    }

    /* Return int array. */
    return intArr;
}

/**
 * Update CAElementaryStreamAuthorizations with authorization reasons for each pid
 *
 * @param env the JNI environment handle
 * @param pidCount number of pids in the array
 * @param pidArray int[] containing requested pids
 * @param statusArray int [] that will be updated with CA denial reasons
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_pod_PodManagerImpl_nativeGetDecryptStreamAuthorizations(
JNIEnv *env, jclass clazz, jint sessionHandle, jobjectArray caElementaryStreamAuthorizations)
{
    JNI_UNUSED(clazz);
    mpe_PODStreamDecryptInfo *decryptInfo;
    jint temp; /* Temporary used to set pids. */
    int i; /* Loop index. */
    int retCode = MPE_SUCCESS;
    jobject caAuthorizationEntry;

    jint pidCount = (*env)->GetArrayLength(env, caElementaryStreamAuthorizations);

    if ((retCode = mpe_memAllocP(MPE_MEM_POD, (sizeof(decryptInfo) * pidCount), (void**) &decryptInfo)) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR,MPE_MOD_JNI,"nativeGetDecryptStreamAuthorizations() failed to allocate memory!\n");
        return MPE_EINVAL;
    }

    for (i=0;i<pidCount;++i)
    {
        caAuthorizationEntry = (*env)->GetObjectArrayElement(env, caElementaryStreamAuthorizations, i);
        temp  = (*env)->GetIntField(env, caAuthorizationEntry, jniutil_CachedIds.CAElementaryStreamAuthorization_pid);
        decryptInfo[i].pid = temp;
        //default to NO_VALUE and check prior to returning
        decryptInfo[i].status = CA_ENABLE_NO_VALUE;
    }

    /* Call MPE to acquire streamInfo array. */
    if ((retCode = mpe_podGetDecryptStreamStatus((mpe_PODDecryptSessionHandle)sessionHandle, (uint8_t)pidCount, decryptInfo)) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR,MPE_MOD_JNI,"podGetDecryptStreamStatus() failed - %d\n", retCode);
        goto cleanupAndExit;
    }

    for (i=0;i<pidCount;++i)
    {
        caAuthorizationEntry = (*env)->GetObjectArrayElement(env, caElementaryStreamAuthorizations, i);
        if (decryptInfo[i].status != CA_ENABLE_NO_VALUE)
        {
            MPE_LOG(MPE_LOG_DEBUG,MPE_MOD_JNI,"updating - pid %d with status: %d\n", decryptInfo[i].pid, decryptInfo[i].status);
            (*env)->SetIntField(env, caAuthorizationEntry, jniutil_CachedIds.CAElementaryStreamAuthorization_reason, (int) decryptInfo[i].status);
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG,MPE_MOD_JNI,"status for pid %d was not provided - not updating\n", decryptInfo[i].pid);
        }
    }

    cleanupAndExit:
    mpe_memFreeP(MPE_MEM_POD, decryptInfo);
    return retCode;
}

/**
 * Class:     org_ocap_hardware_pod_POD
 * Method:    isPODReady
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_manager_pod_PodManagerImpl_nativeIsPODReady(
        JNIEnv *env, jclass clazz)
{
    JNI_UNUSED(env);
    JNI_UNUSED(clazz);

    /* Call MPE support to determine Pod status. */
    return (jboolean)(mpe_podIsReady() == MPE_SUCCESS);
}

/**
 * Class:     org_cablelabs_impl_manager_pod_PodManagerImpl
 * Method:    isPODPresent
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_manager_pod_PodManagerImpl_nativeIsPODPresent(
        JNIEnv *env, jclass clazz)
{
    JNI_UNUSED(env);
    JNI_UNUSED(clazz);

    /* Call MPE support to determine Pod status. */
    return (jboolean)(mpe_podIsPresent() == MPE_SUCCESS);
}

/**
 * Class:     org_ocap_hardware_pod_POD
 * Method:    getPODHostParam
 * Signature: (I)[B
 */
JNIEXPORT jbyteArray JNICALL Java_org_cablelabs_impl_manager_pod_PodManagerImpl_nativeGetPODHostParam(
        JNIEnv *env, jclass clazz, jint featureId)
{
    jbyteArray byteArr; /* byte array to populate with param values. */
    uint8_t *param; /* Pointer to feature parameter value(s). */
    int length = 0; /* Length of byte stream to return. */

    JNI_UNUSED(clazz);

    /* Call MPE to acquire feature parameter value. */
    if (mpe_podGetFeatureParam(featureId, &param, (uint32_t*) &length)
            != MPE_SUCCESS)
        return NULL;

    /* Allocate a byte array of size just determined. */
    if ((byteArr = (*env)->NewByteArray(env, length)) == NULL)
    {
        return NULL; /* Return null reference on error. */
    }

    /* Now copy the application info to the byte array. */
    (*env)->SetByteArrayRegion(env, byteArr, 0, length, (const jbyte*) param);

    /* Return the byte array. */
    return byteArr;
}

/**
 * Class:     org_ocap_hardware_pod_POD
 * Method:    updatePODHostParam
 * Signature: (I[B)Z
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_manager_pod_PodManagerImpl_nativeUpdatePODHostParam(
        JNIEnv *env, jclass clazz, jint featureId, jbyteArray value)
{
    jboolean response; /* Update operation response. */
    uint32_t length; /* Length of array. */
    jbyte *buffer; /* Pointer to native array allocated. */

    JNI_UNUSED(clazz);

    /* Get size of array. */
    length = (*env)->GetArrayLength(env, value);

    /* Allocate buffer for parental control values and count of values. */
    if (mpe_memAllocP(MPE_MEM_POD, length, (void**) &buffer) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "ERROR in nativeUpdatePODHostParam: mpe_memAllocP() failed\n");
        return FALSE;
    }

    /* Now copy the byte array data to the native buffer. */
    (*env)->GetByteArrayRegion(env, value, 0, length, buffer);

    /* Call MPE to set feature parameter according to type. */
    response = (jboolean)(mpe_podSetFeatureParam(featureId, (uint8_t*) buffer,
            length) == MPE_SUCCESS);

    /* Return temporary buffer. */
    mpe_memFreeP(MPE_MEM_POD, buffer);

    /* Return the response. */
    return response;
}

/**
 * Class:     org_ocap_hardware_pod_POD
 * Method:    startDecrypt
 *
 * @return -    1 if decryption is needed for this decode.  If 1 and sessionHandle == {@link NativeHandle#NULL_HANDLE}, then resource is not available.
 *              0 if decryption is not needed.
 *            < 0 if error (for example (-MPE_EINVAL)
 */
/*
 * Class:     org_cablelabs_impl_manager_pod_PodManagerImpl
 * Method:    nativeStartDecrypt
 * Signature: (ISLorg/cablelabs/impl/manager/ed/EDListener;[I[SI[I[I)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_pod_PodManagerImpl_nativeStartDecrypt(
        JNIEnv *env, jclass cls,
        jshort handleType,
        jint handle,
        jint tuner,
        jobject edListener,
        jintArray pidArray, jshortArray typeArray,
        jint priority,
        jintArray sessionHandleArray )
{
    mpe_Error retCode = MPE_SUCCESS;
    mpe_EdEventInfo *edHandle;
    mpe_PodDecryptRequestParams reqParams;

    jsize pidCount = 0;
    mpe_MediaPID* mpePids = 0;
    jint intDecryptSessionHandle = 0;

    JNI_UNUSED(cls);

    /* Allocate PID array if serviceComponentImpls is not empty */
    pidCount = (*env)->GetArrayLength(env, pidArray);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "nativeStartDecrypt(): %d pids\n",
            (int)pidCount);

    if (pidCount <= 0)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "ERROR in nativeStartDecrypt: Invalid pid count (%d)\n", (int)pidCount);
        return MPE_EINVAL;
    }
    if ( (retCode = mpe_memAllocP(MPE_MEM_TEMP, sizeof(mpe_MediaPID) * pidCount, (void**) &mpePids))
         != MPE_SUCCESS )
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "ERROR in nativeStartDecrypt: mpe_memAllocP() = %d\n", retCode);
        goto startDecryptReturn;
    }

    /* create array of mpe_MediaPIDs */
    jniutil_createPidArray(env, pidArray, typeArray, pidCount, mpePids);

    /* Initialize media request parameters */
    reqParams.handleType = handleType;
    reqParams.handle = handle;
    reqParams.tunerId = tuner;
    reqParams.numPids = pidCount;
    reqParams.pids = mpePids;
    reqParams.priority = priority;
    reqParams.mmiEnable = true;

   /* retCode = mpe_edCreateHandle( edListener, MPE_ED_QUEUE_NORMAL, PodImplEdCallback,
                                     MPE_ED_TERMINATION_EVCODE,
                                     MPE_POD_DECRYPT_EVENT_SESSION_SHUTDOWN,
                                     &edHandle );
   */
    retCode = mpe_edCreateHandle( edListener, MPE_ED_QUEUE_SPECIAL1, NULL,
                                     MPE_ED_TERMINATION_EVCODE,
                                     MPE_POD_DECRYPT_EVENT_SESSION_SHUTDOWN,
                                     &edHandle );
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG( MPE_LOG_ERROR, MPE_MOD_JNI,
                 "ERROR in nativeStartDecrypt: mpe_edCreateHandle(): %d\n",
                 retCode);
        goto startDecryptReturn;
    }

    mpe_PODDecryptSessionHandle decryptSessionHandle;

    /* Start the session */
    retCode = mpe_podStartDecrypt( &reqParams, edHandle->eventQ, edHandle,
                                   &decryptSessionHandle );

    if ((retCode != MPE_SUCCESS) || (decryptSessionHandle == NULL))
    {
        mpe_edDeleteHandle(edHandle);

        if(retCode != MPE_SUCCESS)
        {
            MPE_LOG( MPE_LOG_ERROR, MPE_MOD_JNI,
                    "ERROR in nativeStartDecrypt: mpe_startDecrypt(): %d\n", retCode);
        }
        else
        {
            MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_JNI,
                    "nativeStartDecrypt returned: %d, CA session is not required..\n", retCode);
        }

        goto startDecryptReturn;
    }

    /* Store the returned session handle into the handle return array. */
    intDecryptSessionHandle = (jint) decryptSessionHandle;
    (*env)->SetIntArrayRegion( env, sessionHandleArray, 0, 1,
                               &intDecryptSessionHandle);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "nativeStartDecrypt -- decryptSessionHandle = 0x%08x\n",
            (int)intDecryptSessionHandle);

    startDecryptReturn:
    /* Free up temporary allocated structures. */
    if (mpePids != 0)
        mpe_memFreeP(MPE_MEM_TEMP, mpePids);

    return retCode;
}

/**
 * Class:     org_cablelabs_impl_manager_pod_PodManagerImpl
 * Method:    nativeStopDecrypt
 * Signature: (I)I
 *
 * @return MPE_SUCCESS or error code
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_pod_PodManagerImpl_nativeStopDecrypt(
        JNIEnv *env, jclass clazz, jint sessionHandle)
{
    JNI_UNUSED(env);
    JNI_UNUSED(clazz);

    return (jint) mpe_podStopDecrypt(
            (mpe_PODDecryptSessionHandle) sessionHandle);
}


void PodImplEdCallback(JNIEnv *env, void *listenerObj,
        mpe_EdEventInfo *edHandle, uint32_t *evCode, void** data1,
        void** data2, uint32_t* data3)
{
    MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "PodImplEdCallback called!\n");
    MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "PodImplEdCallback edHandle:%p\n", edHandle);
    MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "PodImplEdCallback <evCode:%x data1:%p data2:%p data3:%p>\n", *evCode,
    		data1, data2, data3);
}
