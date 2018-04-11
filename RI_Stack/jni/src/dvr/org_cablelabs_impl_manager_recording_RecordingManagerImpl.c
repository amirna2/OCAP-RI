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


#include <org_cablelabs_impl_manager_recording_RecordingManagerImpl.h>
#include "jni_util.h"
#include <mpe_dvr.h>
#include <mpe_os.h>
#include <mpe_ed.h>

/* Inaccessible static: m_instance */
/* Inaccessible static: class_00024org_00024cablelabs_00024impl_00024manager_00024RecordingDBManager */
/*
 * Class:     org_cablelabs_impl_manager_recording_RecordingManagerImpl
 * Method:    nRetrieveRecordingList
 * Signature: (Ljava/util/Vector;)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_recording_RecordingManagerImpl_nRetrieveRecordingList
(JNIEnv *env, jobject recmgr, jint nativeStorageHandle, jobject vec)
{
    jobject string;
    jclass cls;
    jmethodID mid;
    uint32_t i, count;
    mpe_Error error = 0;
    mpe_DvrString_t *recordingNames = NULL;

    MPE_UNUSED_PARAM(recmgr);

    error = mpe_dvrGetRecordingList((mpe_StorageHandle)nativeStorageHandle,
            &count,
            (mpe_DvrString_t **)(&recordingNames));

    if (error == MPE_SUCCESS)
    {
        cls =(*env)->GetObjectClass(env, vec);

        if (cls != NULL)
        {
            mid = (*env)->GetMethodID(env, cls, "add", "(Ljava/lang/Object;)Z");
            if (mid != NULL)
            {
                for (i=0; i<count;i++)
                {
                    string = (*env)->NewStringUTF(env, (char *)recordingNames[i]);
                    (void)(*env)->CallBooleanMethod(env, vec, mid, string);
                }
            }
            else
            {
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "RecordingManagerImpl JNI: get recordings mid == NULL!\n");
            }
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "RecordingManagerImpl JNI: get recordings cls == NULL!\n");
        }
    }
}

/*
 * Class:     org_cablelabs_impl_manager_recording_RecordingManagerImpl
 * Method:    nGetLowPowerResumeTime
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_recording_RecordingManagerImpl_nGetLowPowerResumeTime(
        JNIEnv *env, jclass cls)
{
    JNI_UNUSED(env);
    JNI_UNUSED(cls);
    return mpe_dvrGetLowPowerResumeTime();
}

/*
 * Class:     org_cablelabs_impl_manager_recording_RecordingManagerImpl
 * Method:    nGetLowPowerResumeTime
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_recording_RecordingManagerImpl_nResumeFromLowPower(
        JNIEnv *env, jclass cls)
{
    JNI_UNUSED(env);
    JNI_UNUSED(cls);
    return mpe_dvrResumeFromLowPower();
}

/*
 * Class:     org_cablelabs_impl_manager_recording_RecordingManagerImpl
 * Method:    nGetMaxBitRate
 */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_manager_recording_RecordingManagerImpl_nGetMaxBitRate(
        JNIEnv *env, jclass cls)
{
    uint32_t bitRate = 0;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    /* get maximum supported bit rate from native layer */
    /* should always return MPE_SUCCESS */

    (void) mpe_dvrGet(MPE_DVR_MAX_BITRATE, NULL, &bitRate);

    return bitRate;
}

/*
 * Class:     org_cablelabs_impl_manager_recording_RecordingManagerImpl
 * Method:    nGetSmallestTimeShiftDuration
 */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_manager_recording_RecordingManagerImpl_nGetSmallestTimeShiftDuration(
        JNIEnv *env, jclass cls)
{
    uint32_t tsbSize = 0;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    /* get smallest time shift size from native layer */
    /* should always return MPE_SUCCESS */

    (void) mpe_dvrGet(MPE_DVR_TSB_MIN_BUF_SIZE, NULL, &tsbSize);

    return tsbSize;
}
