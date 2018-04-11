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

#include "org_cablelabs_impl_storage_MediaStorageOptionImpl.h"
#include "mgrdef.h"
#include "mpe_dvr.h"
#include <inttypes.h>
#include "mpe_dbg.h"
#include "jni_util.h"
#include "mpe_storage.h"
/*
 * Class:     org_cablelabs_impl_storage_MediaStorageOptionImpl
 * Method:    nInit
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_storage_MediaStorageOptionImpl_nInit
(JNIEnv *AEnv, jclass cls)
{

}

/*
 * Class:     org_cablelabs_impl_storage_MediaStorageOptionImpl
 * Method:    nGetTotalMediaCapacity
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_storage_MediaStorageOptionImpl_nGetTotalMediaCapacity(
        JNIEnv *AEnv, jobject AObj, jint handle)
{
    mpe_Error err = MPE_DVR_ERR_NOERR;
    uint64_t capacity = 0;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_JNI,
            "Java_org_cablelabs_impl_storage_MediaStorageOptionImpl_nGetTotalMediaCapacity: \n");

    JNI_UNUSED(AEnv);
    JNI_UNUSED(AObj);

    err = mpe_dvrGet(MPE_DVR_STORAGE_MEDIAFS_CAPACITY, (void *) (&handle),
            (void *) &capacity);

    if (err == MPE_DVR_ERR_NOERR)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "<<DVR>> Java_org_cablelabs_impl_storage_MediaStorageOptionImpl_nGetTotalMediaCapacity returned storage space = %"PRIu64" bytes\n", capacity);
        return (jlong) capacity;
    }

    return 0;
}

/*
 * Class:     org_cablelabs_impl_storage_MediaStorageOptionImpl
 * Method:    nGetRecordingBandwidth
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_storage_MediaStorageOptionImpl_nGetRecordingBandwidth(
        JNIEnv *AEnv, jobject AObj, jint handle)
{
    mpe_Error err = MPE_DVR_ERR_NOERR;
    uint64_t bandwidth = 0;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_JNI,
            "Java_org_cablelabs_impl_storage_MediaStorageOptionImpl_nGetRecordingBandwidth: \n");

    JNI_UNUSED(AEnv);
    JNI_UNUSED(AObj);

    err = mpe_dvrGet(MPE_DVR_MAX_RECORDING_BANDWIDTH, (void *) (&handle),
            (void *) &bandwidth);

    if (err == MPE_DVR_ERR_NOERR)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "<<DVR>> Java_org_cablelabs_impl_storage_MediaStorageOptionImpl_nGetRecordingBandwidth returned = %"PRIu64" bits/sec\n", bandwidth);
        return (jlong) bandwidth;
    }

    return 0;
}

/*
 * Class:     org_cablelabs_impl_storage_MediaStorageOptionImpl
 * Method:    nGetPlaybackBandwidth
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_storage_MediaStorageOptionImpl_nGetPlaybackBandwidth(
        JNIEnv *AEnv, jobject AObj, jint handle)
{
    mpe_Error err = MPE_DVR_ERR_NOERR;
    uint64_t bandwidth = 0;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_JNI,
            "Java_org_cablelabs_impl_storage_MediaStorageOptionImpl_nGetPlaybackBandwidth: \n");

    JNI_UNUSED(AEnv);
    JNI_UNUSED(AObj);

    err = mpe_dvrGet(MPE_DVR_MAX_PLAYBACK_BANDWIDTH, (void *) (&handle),
            (void *) &bandwidth);

    if (err == MPE_DVR_ERR_NOERR)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "<<DVR>> Java_org_cablelabs_impl_storage_MediaStorageOptionImpl_nGetPlaybackBandwidth returned = %"PRIu64" bits/sec\n", bandwidth);
        return (jlong) bandwidth;
    }

    return 0;
}

/*
 * Class:     org_cablelabs_impl_storage_MediaStorageOptionImpl
 * Method:    nGetPlaybackBandwidth
 * Signature: (I)J
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_storage_MediaStorageOptionImpl_nSimultaneousPlayAndRecord(
        JNIEnv *AEnv, jobject AObj, jint handle)
{
    mpe_Error err = MPE_DVR_ERR_NOERR;
    uint32_t flag = 0;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_JNI,
            "Java_org_cablelabs_impl_storage_MediaStorageOptionImpl_nSimultaneousPlayAndRecord: \n");

    JNI_UNUSED(AEnv);
    JNI_UNUSED(AObj);

    err = mpe_dvrGet(MPE_DVR_SIMULTANEOUS_PLAY_RECORD, (void *) (&handle),
            (void *) &flag);

    if (err == MPE_DVR_ERR_NOERR)
    {
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_JNI,
                "<<DVR>> Java_org_cablelabs_impl_storage_MediaStorageOptionImpl_nSimultaneousPlayAndRecord returned = %d\n",
                flag);
        return (jboolean) flag;
    }

    return false;
}

/*
 * Class:     org_cablelabs_impl_storage_MediaStorageOptionImpl
 * Method:    nGetAllocatableMediaStorage
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_storage_MediaStorageOptionImpl_nGetAllocatableMediaStorage(
        JNIEnv *AEnv, jobject AObj, jint handle)
{
    jlong allocatableMediaStorage = 0;
    JNI_UNUSED(AEnv);
    JNI_UNUSED(AObj);

    (void) mpe_dvrGet(MPE_DVR_STORAGE_MEDIAFS_ALLOCATABLE_SPACE,
            (void *) (&handle), (void *) &allocatableMediaStorage);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "<<DVR>> Java_org_cablelabs_impl_storage_MediaStorageOptionImpl_nGetAllocatableMediaStorage returned = %"PRIu64"\n", allocatableMediaStorage);
    return allocatableMediaStorage;
}

/*
 * Class:     org_cablelabs_impl_storage_MediaStorageOptionImpl
 * Method:    nRepartition
 * Signature: (IJ)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_storage_MediaStorageOptionImpl_nRepartition
(JNIEnv *AEnv, jobject AObj, jint handle, jlong mediafsSize)
{
    mpe_StorageError err = MPE_DVR_ERR_NOERR;
    mpe_Bool authorized = true; //Need to identify from where this value creeps in.
    uint64_t sz = mediafsSize;

    JNI_UNUSED(AEnv);
    JNI_UNUSED(AObj);

    err = mpe_storageInitializeDevice((void *)(handle), authorized,(uint64_t*)&sz);

    switch (err)
    {
        case MPE_STORAGE_ERR_INVALID_PARAM :
        jniutil_throwByName(AEnv,"java/lang/IllegalArgumentException","Invalid Handle or Requested Size greater than available space");
        break;
        case MPE_STORAGE_ERR_OUT_OF_STATE :
        jniutil_throwByName(AEnv,"java/lang/IllegalStateException","Device is either busy or offline or not present");
        break;
        case MPE_STORAGE_ERR_DEVICE_ERR :
        case MPE_STORAGE_ERR_UNSUPPORTED :
        jniutil_throwByName(AEnv,"java/lang/UnsupportedOperationException","");
        break;
        case MPE_STORAGE_ERR_BUSY :
        jniutil_throwByName(AEnv,"java/lang/IllegalStateException","Device is busy");
        break;
        default :
        break;
    }

}

/*
 * Class:     org_cablelabs_impl_storage_MediaStorageOptionImpl
 * Method:    nIsCrossMsvTsbConversionSupported
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_storage_MediaStorageOptionImpl_nIsCrossMsvTsbConversionSupported(
        JNIEnv *AEnv, jobject AObj, jint handle)
{
    //  STORAGE-MGR PHASE I stub
    uint32_t crossMsvTsbSupported = 0;

    JNI_UNUSED(AEnv);
    JNI_UNUSED(AObj);

    (void) mpe_dvrGet(MPE_DVR_SUPPORTS_CROSS_MSV_TSB_CONVERT,
            (void *) (&handle), (void *) &crossMsvTsbSupported);
    return (crossMsvTsbSupported == 0) ? JNI_FALSE : JNI_TRUE;
}
