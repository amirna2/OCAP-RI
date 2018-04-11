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

#include "org_cablelabs_impl_storage_MediaStorageVolumeImpl.h"
#include "jni_util.h"

#include <mgrdef.h>
#include <mpe_dvr.h>
#include <mpe_dbg.h>
#include <mpe_file.h>
#include <mpe_storage.h>
#include <inttypes.h>

/*
 * Class:     MediaStorageVolumeImpl
 * Method:    nGetFreeSpace
 * Signature: (I)V
 */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_storage_MediaStorageVolumeImpl_nGetFreeSpace(
        JNIEnv *AEnv, jobject AObj, jint handle)
{
    mpe_Error err = MPE_DVR_ERR_NOERR;
    uint64_t space = 0;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "Java_org_cablelabs_impl_storage_MediaStorageVolumeImpl_JNI:nGetFreeSpace\n");

    JNI_UNUSED(AEnv);
    JNI_UNUSED(AObj);

    err = mpe_dvrMediaVolumeGetInfo((mpe_MediaVolume) handle,
            MPE_DVR_MEDIA_VOL_FREE_SPACE, (void *) &space);

    if (err == MPE_DVR_ERR_NOERR)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "<<DVR>> Java_org_cablelabs_impl_storage_MediaStorageVolumeImpl_nGetFreeSpace returned storage space = %"PRIu64" bytes\n", space);
        return (jlong) space;
    }

    return 0;
}

/*
 * Class:     org_cablelabs_impl_storage_MediaStorageVolumeImpl
 * Method:    nGetVolumes
 * Signature: (ILjava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_storage_MediaStorageVolumeImpl_nGetVolumes
(JNIEnv *env, jclass cls, jint handle, jobject vec)
{
    uint32_t numVolumes = 0;
    jclass clsVector, clsInteger;
    jobject integer;
    jmethodID midVector, midInteger;
    mpe_MediaVolume *volumeArray = NULL;
    uint32_t i=0;

    JNI_UNUSED(cls);
    /* Get the max device count and allocate buffer. */
    (void)mpe_dvrMediaVolumeGetCount((mpe_StorageHandle)handle, &numVolumes);
    (void)mpe_memAllocP(MPE_MEM_STORAGE, numVolumes*sizeof(mpe_MediaVolume), (void**)&volumeArray);

    if (volumeArray != NULL)
    {
        if (mpe_dvrMediaVolumeGetList((mpe_StorageHandle)handle, &numVolumes, volumeArray) ==
                MPE_STORAGE_ERR_NOERR)
        {
            clsVector =(*env)->GetObjectClass(env, vec);

            if (clsVector == NULL)
            {
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                        "MediaStorageVolumeImpl JNI: nGetVolumes clsVector == NULL!\n");
            }
            else
            {
                midVector = (*env)->GetMethodID(env, clsVector,
                        "add", "(Ljava/lang/Object;)Z");
                if (midVector == NULL)
                {
                    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                            "MediaStorageVolumeImpl JNI: nGetVolumes midVector == NULL!\n");
                }
                else
                {
                    clsInteger = (*env)->FindClass(env, "java/lang/Integer");
                    if (clsInteger == NULL)
                    {
                        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                                "MediaStorageVolumeImpl JNI: nGetVolumes clsInteger == NULL!\n");
                    }
                    else
                    {
                        /* Get the Integer constructor method. */
                        midInteger = (*env)->GetMethodID(env, clsInteger,
                                "<init>", "(I)V");
                        if (midInteger == NULL)
                        {
                            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                                    "MediaStorageVolumeImpl JNI: nGetVolumes midInteger == NULL!\n");
                        }
                        else
                        {
                            for (i = 0; i < numVolumes; i++)
                            {
                                /* Create an integer object and add it to vector */
                                integer = (*env)->NewObject(env, clsInteger,
                                        midInteger, volumeArray[i]);
                                if (integer != NULL)
                                {
                                    (void)(*env)->CallBooleanMethod(env, vec,
                                            midVector, integer);
                                }
                            } // end-for
                        }
                    } // end-if (clsInteger)
                }
            } // end-if (clsVector)
        }
        mpe_memFreeP(MPE_MEM_STORAGE, volumeArray);
    }
}

/*
 * Class:     org_cablelabs_impl_storage_MediaStorageVolumeImpl
 * Method:    nNewVolume
 * Signature: (ILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_storage_MediaStorageVolumeImpl_nNewVolume(
        JNIEnv *AEnv, jobject AObj, jint handle, jstring path)
{
    mpe_MediaVolume newVolume = 0;
    const char *str = NULL;
    mpe_Error err = MPE_DVR_ERR_NOERR;
    JNI_UNUSED(AObj);

    /* Acquire the volume name string. */
    if (NULL != path)
    {
        if ((str = (*AEnv)->GetStringUTFChars(AEnv, path, NULL)) == NULL)
        {
            /* GetStringUTFChars threw a memory exception */
            err = MPE_DVR_ERR_DEVICE_ERR;
        }
    }

    if (err == MPE_DVR_ERR_NOERR)
    {
        err = mpe_dvrMediaVolumeNew((mpe_StorageHandle) handle, (char*) str,
                &newVolume);
    }

    switch (err)
    {
    case MPE_DVR_ERR_INVALID_PARAM:
    case MPE_DVR_ERR_NOT_ALLOWED:
        (*AEnv)->ThrowNew(AEnv, (*AEnv)->FindClass(AEnv,
                "java/lang/IllegalArgumentException"),
                "Invalid Parameter or the MSV already exists");
        break;
    case MPE_DVR_ERR_DEVICE_ERR:
        (*AEnv)->ThrowNew(AEnv,
                (*AEnv)->FindClass(AEnv, "java/io/IOException"), "Device error");
        break;
    case MPE_DVR_ERR_UNSUPPORTED:
        (*AEnv)->ThrowNew(AEnv, (*AEnv)->FindClass(AEnv,
                "java/lang/UnsupportedOperationException"),
                "Operation not supported");
        break;
    case MPE_DVR_ERR_NOERR:
    default:
        break;
    }
    return (jint) newVolume;
}

/*
 * Class:     org_cablelabs_impl_storage_MediaStorageVolumeImpl
 * Method:    nGetAllocatedSpace
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_storage_MediaStorageVolumeImpl_nGetAllocatedSpace(
        JNIEnv *AEnv, jobject AObj, jint handle)
{
    // STORAGE-MGR PHASE I stub
    uint64_t allocatedSpace = 0;
    JNI_UNUSED(AEnv);
    JNI_UNUSED(AObj);

    (void) mpe_dvrMediaVolumeGetInfo((mpe_MediaVolume) handle,
            MPE_DVR_MEDIA_VOL_SIZE, &allocatedSpace);
    return allocatedSpace;
}

/*
 * Class:     org_cablelabs_impl_storage_MediaStorageVolumeImpl
 * Method:    nAllocate
 * Signature: (IJ)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_storage_MediaStorageVolumeImpl_nAllocate
(JNIEnv *AEnv, jobject AObj, jint handle, jlong sizeBytes)
{
    mpe_Error err = MPE_SUCCESS;
    JNI_UNUSED(AEnv);
    JNI_UNUSED(AObj);

    err = mpe_dvrMediaVolumeSetInfo((mpe_MediaVolume)handle, MPE_DVR_MEDIA_VOL_SIZE, (void *) &sizeBytes);

    switch (err)
    {
        case MPE_DVR_ERR_DEVICE_ERR :
        jniutil_throwByName(AEnv,"java/lang/IllegalArgumentException","Device Error");
        break;
        case MPE_DVR_ERR_INVALID_PARAM :
        jniutil_throwByName(AEnv,"java/lang/IllegalArgumentException","Invalid Parameter");
        break;
        default :
        //No exception to be thrown for the other scenarios
        break;
    }
}
/*
 * Class:     org_cablelabs_impl_storage_MediaStorageVolumeImpl
 * Method:    nGetPath
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_cablelabs_impl_storage_MediaStorageVolumeImpl_nGetPath(
        JNIEnv *AEnv, jclass cls, jint handle)
{
    jchar path[MPE_DVR_MEDIA_VOL_MAX_PATH_SIZE];
    jstring jStrReturn = NULL;

    JNI_UNUSED(cls);

    if (mpe_dvrMediaVolumeGetInfo((mpe_MediaVolume) handle,
            MPE_DVR_MEDIA_VOL_PATH, path) == MPE_DVR_ERR_NOERR)
    {
        jStrReturn = (*AEnv)->NewStringUTF(AEnv, (char *) path);
    }

    return jStrReturn;
}

/*
 * Class:     org_cablelabs_impl_storage_MediaStorageVolumeImpl
 * Method:    nAddAlarm
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_storage_MediaStorageVolumeImpl_nAddAlarm
(JNIEnv *AEnv, jobject AObj, jint handle, jint level)
{
    JNI_UNUSED(AObj);

    switch (mpe_dvrMediaVolumeAddAlarm((mpe_MediaVolume)handle, (uint8_t)level))
    {
        case MPE_DVR_ERR_INVALID_PARAM:
        case MPE_DVR_ERR_NOT_ALLOWED:
        (*AEnv)->ThrowNew(AEnv, (*AEnv)->FindClass(AEnv,
                        "java/lang/IllegalArgumentException"),
                "Invalid handle or alarm entry already exists");
        break;
        case MPE_DVR_ERR_NOERR:
        default:
        break;
    }
}

/*
 * Class:     org_cablelabs_impl_storage_MediaStorageVolumeImpl
 * Method:    nRemoveAlarm
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_storage_MediaStorageVolumeImpl_nRemoveAlarm
(JNIEnv *AEnv, jobject AObj, jint handle, jint level)
{
    // STORAGE-MGR PHASE I stub
    JNI_UNUSED(AEnv);
    JNI_UNUSED(AObj);

    (void)mpe_dvrMediaVolumeRemoveAlarm((mpe_MediaVolume)handle, (uint8_t)level);
}

/*
 * Class:     org_cablelabs_impl_storage_MediaStorageVolumeImpl
 * Method:    nDeleteVoulme
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_storage_MediaStorageVolumeImpl_nDeleteVolume
(JNIEnv *AEnv, jobject AObj, jint handle)
{
    JNI_UNUSED(AEnv);
    JNI_UNUSED(AObj);

    (void)mpe_dvrMediaVolumeDelete((mpe_MediaVolume)handle);
}

/*
 * Class:     org_cablelabs_impl_storage_MediaStorageVolumeImpl
 * Method:    nGetCreateDate
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_storage_MediaStorageVolumeImpl_nGetCreateDate
(JNIEnv *AEnv, jobject AObj, jint handle)
{
    uint32_t createDate = 0;
    jlong retVal = 0;
    
    JNI_UNUSED(AEnv);
    JNI_UNUSED(AObj);

    (void) mpe_dvrMediaVolumeGetInfo((mpe_MediaVolume) handle,
            MPE_DVR_MEDIA_VOL_CREATE_TIME, &createDate);
    retVal += createDate;
    
    return retVal;
}

JNIEXPORT void JNICALL Java_org_cablelabs_impl_storage_MediaStorageVolumeImpl_nSetMinimumTSBSize(
        JNIEnv *AEnv, jobject AObj, jint handle, jlong sizeBytes)
{
    JNI_UNUSED(AEnv);
    JNI_UNUSED(AObj);

    (void) mpe_dvrMediaVolumeSetInfo((mpe_MediaVolume)handle, MPE_DVR_MEDIA_VOL_TSB_MIN_SIZE_BYTES, (void *) &sizeBytes);
}

JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_storage_MediaStorageVolumeImpl_nGetMinimumTSBSize(
        JNIEnv *AEnv, jobject AObj, jint handle)
{
    uint64_t sizeBytes = 0;

    JNI_UNUSED(AEnv);
    JNI_UNUSED(AObj);

    (void) mpe_dvrMediaVolumeGetInfo((mpe_MediaVolume) handle, MPE_DVR_MEDIA_VOL_TSB_MIN_SIZE_BYTES, (void *) &sizeBytes);

    return (jlong) sizeBytes;
}

