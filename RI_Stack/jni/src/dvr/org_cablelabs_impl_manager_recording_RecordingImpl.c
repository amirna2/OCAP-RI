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


#include "org_cablelabs_impl_manager_recording_RecordingImpl.h"
#include "jni_util.h"
#include "mpe_dvr.h"
#include "mpe_os.h"
#include "mpe_ed.h"
#include <inttypes.h>
#include <string.h>

void RecordingImplEdCallback(JNIEnv *env, void *listenerObj,
        mpe_EdEventInfo *edHandle, uint32_t eventId, void *optionalData1,
        void *optionalData2, uint32_t eventFlags);

#define NATIVE_SUCCESS 0
#define NATIVE_FAILURE 1

/* !!! Temporary async response queue - ignored! !!! */
mpe_EventQueue g_dvrRecordingQueue;

/*
 * Class:     org_cablelabs_impl_manager_recording_RecordingImpl
 * Method:    nInit
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_recording_RecordingImpl_nInit
(JNIEnv *env, jclass cls)
{
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "JNI(Recording): nInit()\n");

    /* PidMapEntry */
    GET_CLASS(PidMapEntry,"org/cablelabs/impl/util/PidMapEntry");
    GET_FIELD_ID(PidMapEntry_streamType, "streamType", "S");
    GET_FIELD_ID(PidMapEntry_srcElementaryStreamType, "srcElementaryStreamType", "S");
    GET_FIELD_ID(PidMapEntry_recElementaryStreamType, "recElementaryStreamType", "S");
    GET_FIELD_ID(PidMapEntry_srcPID, "srcPID", "I");
    GET_FIELD_ID(PidMapEntry_recPID, "recPID", "I");

    /* PidMapTable */
    GET_CLASS(PidMapTable, "org/cablelabs/impl/util/PidMapTable");
    GET_FIELD_ID(PidMapTable_pidTableSize, "pidTableSize", "I");
    GET_FIELD_ID(PidMapTable_pidMapEntryArray, "pidMapEntryArray", "[Lorg/cablelabs/impl/util/PidMapEntry;");

    /* TimeTable */
    GET_CLASS(TimeTable, "org/cablelabs/impl/util/TimeTable");
    GET_FIELD_ID(TimeTable_m_elements, "m_elements", "[Lorg/cablelabs/impl/util/TimeAssociatedElement;");
    GET_FIELD_ID(TimeTable_m_size, "m_size", "I");

    /* TimeAssociatedElement (base class) */
    FIND_CLASS("org/cablelabs/impl/util/TimeAssociatedElement");
    GET_FIELD_ID(TimeElement_time, "time", "J");

    /* GenericTimeAssociatedElement (derived class) */
    FIND_CLASS("org/cablelabs/impl/util/GenericTimeAssociatedElement");
    GET_FIELD_ID(TimeElement_value, "value", "Ljava/lang/Object;");
}

/*
 * Class:     org_cablelabs_impl_manager_recording_RecordingImpl
 * Method:    nDeleteRecording
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_recording_RecordingImpl_nDeleteRecording
(JNIEnv *env, jclass cls, jstring recName)
{
    char *stringChars;

    if (recName == NULL)
    {
        return;
    }

    if ((stringChars = (char *)(*env)->GetStringUTFChars(env, recName, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        return;
    }

    /* Perform the native stop call for this recording */
    /* TODO: Confirm that mpe dvr won't edit this string */
    (void)mpe_dvrRecordingDelete(stringChars);

    (*env)->ReleaseStringUTFChars(env, recName, stringChars);
}

/*
 * Class:     org_cablelabs_impl_manager_recording_RecordingImpl
 * Method:    nGetRecordedDuration
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_manager_recording_RecordingImpl_nGetRecordedDurationMS(
        JNIEnv *env, jobject obj, jstring recName)
{
    char *stringChars;
    uint64_t length;

    if (recName == NULL)
    {
        return 0;
    }

    if ((stringChars = (char *) (*env)->GetStringUTFChars(env, recName, NULL))
            == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        return 0;
    }

    /* We'll first try to get the millsecond-accurate value */
    if (mpe_dvrRecordingGet(NULL, stringChars, MPE_DVR_RECORDING_LENGTH_MS,
            &length) != MPE_DVR_ERR_NOERR)
    {
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "<<DVR>> nGetRecordedDurationMS(recName %s): "
                "Couldn't retrieve MPE_DVR_RECORDING_LENGTH_MS property - using deprecated MPE_DVR_RECORDING_LENGTH...\n",
                recName);
        if (mpe_dvrRecordingGet(NULL, stringChars, MPE_DVR_RECORDING_LENGTH,
                &length) == MPE_DVR_ERR_NOERR)
        {
            length *= 1000; // Convert to millis
        }
        else
        {
            length = 0;
        }
    }

    (*env)->ReleaseStringUTFChars(env, recName, stringChars);

    return length;
}

/*
 * Class:     org_cablelabs_impl_manager_recording_RecordingImpl
 * Method:    nGetRecordedSize
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_manager_recording_RecordingImpl_nGetRecordedSize(
        JNIEnv *env, jobject obj, jstring recName)
{
    char *stringChars;
    int64_t length;

    if (recName == NULL)
    {
        return 0;
    }

    if ((stringChars = (char *) (*env)->GetStringUTFChars(env, recName, NULL))
            == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        return 0;
    }

    /* Perform the native stop call for this recording */
    /* TODO: Confirm that mpe dvr won't edit this string */
    if (mpe_dvrRecordingGet(NULL, stringChars, MPE_DVR_RECORDING_SIZE, &length)
            != MPE_DVR_ERR_NOERR)
    {
        length = 0;
    }

    (*env)->ReleaseStringUTFChars(env, recName, stringChars);

    return length;

}

/*
 * Class:     org_cablelabs_impl_manager_recording_RecordingImpl
 * Method:    nConvertTimeShiftToRecording
 * Signature: (IIJ)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_recording_RecordingImpl_nConvertTimeShiftToRecording(
        JNIEnv *env, jobject obj, jint tsbHandle, jobject listenerObj, jlong startTime,
        jlong duration, jlong bitRate, jobject timeTable,
        jint nativeVolumeHandle)
{
    mpe_Error error = MPE_SUCCESS;
    int64_t duration64;
    int64_t startTime64;
    mpe_EdEventInfo *edHandle;
    char recordingName[MPE_DVR_MAX_NAME_SIZE];
    mpe_DvrConversion recording = 0;
    uint32_t pidTableCount = 0;
    mpe_DvrPidTable *dvrPidTables = NULL;
    jobjectArray elements;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "<<DVR>> ConvertTimeShift...startTime : %"PRIu64"\n", startTime);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "<<DVR>> ConvertTimeShift...duration : %"PRIu64"\n", duration);

    // Create the ed handle to pass into the DVR APIs
    error = mpe_edCreateHandle(listenerObj, MPE_ED_QUEUE_NORMAL, NULL,
            MPE_ED_TERMINATION_EVCODE, MPE_DVR_EVT_SESSION_CLOSED, &edHandle);
    if (error != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "nConvertTimeShiftToRecording() ERROR - cannot create a handle\n");
        return error;
    }

    // 1. Allocate PidTable array
    pidTableCount = (*env)->GetIntField(env, timeTable,
            jniutil_CachedIds.TimeTable_m_size);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "JNI(TSB): nTSBBufferingStart: number of pid tables is %d\n",
            pidTableCount);

    if (pidTableCount)
    {
        error = mpe_memAllocP(MPE_MEM_TEMP, sizeof(mpe_DvrPidTable)
                * pidTableCount, (void**) &dvrPidTables);
        if (error != MPE_SUCCESS)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_JNI,
                    "nConvertTimeShiftToRecording() ERROR - cannot allocate array of pid table of \n");
            mpe_edDeleteHandle(edHandle);
            return error;
        }

        // 2. Given a java Time Table object - construct an array native DVR pid table (mpe_DvrPidTable)

        // get the TimeTable array from TimeTable object
        elements = (*env)->GetObjectField(env, timeTable,
                jniutil_CachedIds.TimeTable_m_elements);
        // populate the dvr pid tables
        jniutil_createDvrPidTable(env, elements, pidTableCount, dvrPidTables);

        duration64 = duration;
        startTime64 = startTime;

        error = mpe_dvrTsbConvertStart((mpe_DvrTsb) tsbHandle,
                (mpe_MediaVolume) nativeVolumeHandle, &startTime64, duration64,
                (mpe_DvrBitRate) bitRate, edHandle->eventQ, (void *) edHandle,
                pidTableCount, dvrPidTables, &recording, recordingName);
        startTime = (jlong) startTime64;

        if (error != MPE_SUCCESS)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_JNI,
                    "nConvertTimeShiftToRecording: mpe_dvrTsbConvertStart returned error %d (0x%x)\n",
                    error, error );
            mpe_edDeleteHandle(edHandle);
            edHandle = NULL;
        }
        else
        {
            // Successful recording start, fill in the recording name and handle
            jobject string = (*env)->NewStringUTF(env, recordingName);
            jclass cls = (*env)->GetObjectClass(env, obj);
            uint32_t t;
            error = NATIVE_SUCCESS;

            // update the TimeTable element array with actual recorded pids information

            for (t = 0; t < pidTableCount; t++)
            {
                jobject elt;
                jobject value;
                jobjectArray pidMapEntryArray;

                elt = (*env)->GetObjectArrayElement(env, elements, t);

                value = (*env)->GetObjectField(env, elt,
                        jniutil_CachedIds.TimeElement_value);
                pidMapEntryArray = (*env)->GetObjectField(env, value,
                        jniutil_CachedIds.PidMapTable_pidMapEntryArray);

                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                        "nConvertTimeShiftToRecording() updating %d pids \n",
                        dvrPidTables[t].count);
                jniutil_updatePidMapTable(env, dvrPidTables[t].count,
                        dvrPidTables[t].pids, pidMapEntryArray);
            }

            /* Free up allocated structures */
            mpe_memFreeP(MPE_MEM_TEMP, dvrPidTables);

            if (cls != NULL)
            {
                jmethodID mid = (*env)->GetMethodID(env, cls,
                        "setNativeRecordingName", "(Ljava/lang/String;)V");

                if (mid != NULL)
                    (*env)->CallVoidMethod(env, obj, mid, string);
                else
                    error = NATIVE_FAILURE;

                mid = (*env)->GetMethodID(env, cls, "setNativeRecordingHandle",
                        "(I)V");

                if (mid != NULL)
                    (*env)->CallVoidMethod(env, obj, mid, (jint) recording);
                else
                    error = NATIVE_FAILURE;

                // If the start time was in the past the time should reflect actual
                // time the content was recorded in the time-shift buffer..
                mid = (*env)->GetMethodID(env, cls, "setActualStartTime",
                        "(J)V");

                if (mid != NULL)
                    (*env)->CallVoidMethod(env, obj, mid, (jlong) startTime);
                else
                    error = NATIVE_FAILURE;
            }
            else
            {
                error = NATIVE_FAILURE;
            }
        }

        // if we've failed to update the Java recording object, stop the native recording
        if (error == NATIVE_FAILURE)
        {
            if (recording != NULL)
                // Stop the conversion immediately!
                (void) mpe_dvrTsbConvertStop((mpe_DvrConversion) recording,
                        true);

            if (!(strcmp(recordingName, "") == 0))
                (void) mpe_dvrRecordingDelete(recordingName);
        }
        return error;
    }

    return error;
}

/*
 * Class:     org_cablelabs_impl_manager_recording_RecordingImpl
 * Method:    nativeConversionChangeComponents
 * Signature: (ILorg/cablelabs/impl/util/PidMapTable;)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_recording_RecordingImpl_nConversionChangeComponents(
        JNIEnv *env, jobject obj, jint handle, jobject pidMapTable)
{

    JNI_UNUSED(env);
    JNI_UNUSED(obj);
    JNI_UNUSED(handle);
    JNI_UNUSED(pidMapTable);

    // Convert pidMapTable to native form
    // TODO: add the native call...
    return mpe_dvrTsbConvertChangePids((mpe_DvrConversion) handle, NULL, 0);
}

/*
 * Class:     org_cablelabs_impl_manager_recording_RecordingImpl
 * Method:    nStopRecording
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_recording_RecordingImpl_nStopTimeShiftConversion(
        JNIEnv *env, jobject obj, jint handle, jboolean immediate)
{
    /* Perform the native stop call for this recording */

    return mpe_dvrTsbConvertStop((mpe_DvrConversion) handle, immediate);
}

/**
 *  The RecordingImpl's native ED event callback function
 *  This function is currently implemented for debugging purposes only!
 */
void RecordingImplEdCallback(JNIEnv *env, void *listenerObj,
        mpe_EdEventInfo *edHandle, uint32_t eventId, void *optionalData1,
        void *optionalData2, uint32_t eventFlags)
{
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "RecordingImplEdCallback called!\n");
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "<%p %p>\n", listenerObj, edHandle);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "<%x %p %p %x>\n", eventId,
            optionalData1, optionalData2, eventFlags);
}
