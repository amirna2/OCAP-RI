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

#include <org_cablelabs_impl_media_mpe_DVRAPIImpl.h>
#include "jni_util.h"
#include <mpe_dbg.h>
#include <mpe_dvr.h>
#include <mpe_ed.h>
#include <mpe_os.h>
#include <inttypes.h>

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_DVRAPIImpl_jniDecodeTSB(
        JNIEnv *env, jclass cls, jobject edListener, jint decoder, jint tsb,
        jobject pidMapTable, jbyte cci, jlong alarmMediaTime, jintArray dvrArray, jlong start, jfloatArray rateArray,
        jboolean blocked, jboolean muted, jfloatArray gainArray)
{
    mpe_Error err = MPE_SUCCESS;
    uint32_t pidCount = 0;
    mpe_DvrPidInfo *dvrPids = 0;
    mpe_EdEventInfo *edEventInfo = 0;
    mpe_DvrPlayback dvrPlaybackHandle = 0;

    float requestedRate = 0;
    float actualRate = 0;
    float requestedGain = 0;
    float actualGain = 0;

    /* Get the gain from gainArray[0]. */
    (*env)->GetFloatArrayRegion(env, gainArray, 0, 1, &requestedGain);
    /* Get the rate from rateArray[0]. */
    (*env)->GetFloatArrayRegion(env, rateArray, 0, 1, &requestedRate);

    MPE_UNUSED_PARAM(cls);

    pidCount = (*env)->GetIntField(env, pidMapTable,
            jniutil_CachedIds.PidMapTable_pidTableSize);

    if (pidCount > 0 && (err = mpe_memAllocP(MPE_MEM_TEMP,
            sizeof(mpe_DvrPidInfo) * pidCount, (void**) &dvrPids))
            != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "mpe_memAllocP() = %d\n", err);
        return err;
    }
    jniutil_convertToDvrPidInfoArray(env, pidMapTable, dvrPids);

    /* Create the ed handle to pass into the DVR APIs */
    err
            = mpe_edCreateHandle(edListener, MPE_ED_QUEUE_NORMAL, NULL,
                    MPE_ED_TERMINATION_EVCODE, MPE_DVR_EVT_SESSION_CLOSED,
                    &edEventInfo);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "mpe_edCreateHandle() = %d\n", err);
        return err;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jniDecodeTSB(decoder=0x%X, tsb=0x%X, pidCount=%d, start=%"PRIu64")\n",
            (int)decoder, (int)tsb, pidCount, start);

    /* Call MPE to start playing from TSB: By default we start at rate 1.0 and at the live point */
    /* If successful, assign the returned dvrPlaybackHandle to dvrArray[0]; otherwise, log the error. */
    err = mpe_dvrTsbPlayStart((mpe_DvrTsb) tsb, (mpe_DispDevice) decoder,
            dvrPids, pidCount, start, requestedRate, &actualRate, blocked, muted, requestedGain, &actualGain, cci, alarmMediaTime, edEventInfo->eventQ,
            (void *) edEventInfo, &dvrPlaybackHandle);

    if (err == MPE_SUCCESS)
    {
        jint dvrPlayback = (jint) dvrPlaybackHandle;
        (*env)->SetIntArrayRegion(env, dvrArray, 0, 1, &dvrPlayback);
        (*env)->SetFloatArrayRegion(env, rateArray, 0, 1, &actualRate);
        (*env)->SetFloatArrayRegion(env, gainArray, 0, 1, &actualGain);
    }
    else
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "mpe_dvrTsbPlayStart() = %d\n", err);
        mpe_edDeleteHandle(edEventInfo);
        edEventInfo = NULL;
    }

    /* Free the PID array memory if it was allocated */
    if (dvrPids != 0)
        mpe_memFreeP(MPE_MEM_TEMP, dvrPids);

    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_DVRAPIImpl_jniSetCCI(
        JNIEnv *env, jclass cls, jint dvr, jbyte cci)
{
    mpe_Error err = MPE_SUCCESS;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    /* If successful, assign the returned mute to muteArray[0]. */
    err = mpe_dvrPlaybackSetCCI((mpe_DvrPlayback) dvr, cci);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "mpe_dvrPlaybackSetCCI() = %d\n", err);
    }

    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_DVRAPIImpl_jniSetAlarm(
        JNIEnv *env, jclass cls, jint dvr, jlong alarmMediaTime)
{
    mpe_Error err = MPE_SUCCESS;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    err = mpe_dvrPlaybackSetAlarm((mpe_DvrPlayback) dvr, alarmMediaTime);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "mpe_dvrPlaybackSetAlarm() = %d\n", err);
    }

    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_DVRAPIImpl_jniSetMute(
        JNIEnv *env, jclass cls, jint dvr, jboolean mute)
{
    mpe_Error err = MPE_SUCCESS;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    /* If successful, assign the returned mute to muteArray[0]. */
    err = mpe_dvrPlaybackSetMute((mpe_DvrPlayback) dvr, (mpe_Bool)mute);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "mpe_mediaSetMute() = %d\n", err);
    }

    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_DVRAPIImpl_jniSetGain(
        JNIEnv *env, jclass cls, jint dvr, jfloatArray gainArray)
{
    mpe_Error err = MPE_SUCCESS;
    float newGain;
    float actualGain;

    MPE_UNUSED_PARAM(cls);

    /* Get the gain from gainArray[0]. */
    (*env)->GetFloatArrayRegion(env, gainArray, 0, 1, &newGain);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jniSetGain(dvr=0x%X, newGain=%.2f)\n",
            (int)dvr, newGain);

    /* Try to set the gain. */
    /* If successful, assign the returned gain to gainArray[0]. */
    err = mpe_dvrPlaybackSetGain((mpe_DvrPlayback) dvr, (float)newGain, &actualGain);
    if (err == MPE_SUCCESS)
    {
        (*env)->SetFloatArrayRegion(env, gainArray, 0, 1, &actualGain);
    }
    else
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "mpe_dvrSetGain() = %d\n", err);
    }

    return err;
}


JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_DVRAPIImpl_jniPlaybackChangePids(
        JNIEnv *env, jclass cls, jint playback, jobject pidMapTable)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_DvrPidTable dvrPidTables;
    uint32_t pidCount = 0;

    // get the array of pid entries
    pidCount = (*env)->GetIntField(env, pidMapTable,
            jniutil_CachedIds.PidMapTable_pidTableSize);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "JNI: jniPlaybackChangePids(): number of pids is %d\n", pidCount);
    if (pidCount == 0)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "JNI: jniPlaybackChangePids(): ERROR - number of pids is %d\n",
                pidCount);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    jniutil_convertToDvrPidInfoArray(env, pidMapTable, dvrPidTables.pids);
    dvrPidTables.mediaTime = 0;
    dvrPidTables.count = pidCount;
    //pass the single dvrpiddtable in to the playbackchangepids function
    err = mpe_dvrPlaybackChangePids((mpe_DvrPlayback) playback,
            dvrPidTables.pids, pidCount);

    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_DVRAPIImpl_jniDecodeRecording(
        JNIEnv *env, jclass cls, jobject edListener, jint decoder,
        jstring recordingName, jobject pidMapTable, jbyte cci, jlong alarmMediaTime, jintArray dvrArray,
        jlong start, jfloatArray rateArray, jboolean blocked, jboolean muted, jfloatArray gainArray)
{
    mpe_Error err = MPE_SUCCESS;
    const char *recordingNameSz;
    mpe_DvrPidInfo *dvrPids = 0;
    uint32_t pidCount = 0;
    mpe_EdEventInfo *edEventInfo = 0;
    mpe_DvrPlayback dvrPlaybackHandle = 0;
    float requestedRate = 0;
    float actualRate = 0;
    float requestedGain = 0;
    float actualGain = 0;

    /* Get the gain from gainArray[0]. */
    (*env)->GetFloatArrayRegion(env, gainArray, 0, 1, &requestedGain);
    /* Get the rate from rateArray[0]. */
    (*env)->GetFloatArrayRegion(env, rateArray, 0, 1, &requestedRate);

    MPE_UNUSED_PARAM(cls);

    /* Allocate recording name string */
    if ((recordingNameSz = (*env)->GetStringUTFChars(env, recordingName, NULL))
            == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        return err;
    }

    pidCount = (*env)->GetIntField(env, pidMapTable,
            jniutil_CachedIds.PidMapTable_pidTableSize);
    if (pidCount > 0 && (err = mpe_memAllocP(MPE_MEM_TEMP,
            sizeof(mpe_DvrPidInfo) * pidCount, (void**) &dvrPids))
            != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "mpe_memAllocP() = %d\n", err);
        (*env)->ReleaseStringUTFChars(env, recordingName, recordingNameSz);
        return err;
    }
    jniutil_convertToDvrPidInfoArray(env, pidMapTable, dvrPids);

    /* Create the ed handle to pass into the DVR APIs */
    err
            = mpe_edCreateHandle(edListener, MPE_ED_QUEUE_NORMAL, NULL,
                    MPE_ED_TERMINATION_EVCODE, MPE_DVR_EVT_SESSION_CLOSED,
                    &edEventInfo);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "mpe_edCreateHandle() = %d\n", err);
        return err;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jniDecodeRecording(decoder=0x%X, recording=%s, pidCount=%u, start=%"PRId64", requestedRate=%f, requestedGain=%f)\n",
            (int)decoder, recordingNameSz, pidCount, (int64_t)start, requestedRate, requestedGain);

    /* Start asynchronous decoding of the recording */
    /* If successful, assign the returned dvrPlaybackHandle to dvrArray[0] */

    err = mpe_dvrRecordingPlayStart((char*) recordingNameSz,
            (mpe_DispDevice) decoder, dvrPids, pidCount, start, requestedRate, &actualRate, blocked, muted, requestedGain,
            &actualGain, cci, alarmMediaTime, edEventInfo->eventQ, (void *) edEventInfo, &dvrPlaybackHandle);

    if (err == MPE_SUCCESS)
    {
        (*env)->SetIntArrayRegion(env, dvrArray, 0, 1, (jint*) &dvrPlaybackHandle);
        (*env)->SetFloatArrayRegion(env, rateArray, 0, 1, &actualRate);
        (*env)->SetFloatArrayRegion(env, gainArray, 0, 1, &actualGain);
    }
    else
    {
        mpe_edDeleteHandle(edEventInfo);
        edEventInfo = NULL;
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "mpe_dvrRecordingPlayStart() = %d\n", err);
    }

    /* Release the recording name string memory */
    (*env)->ReleaseStringUTFChars(env, recordingName, recordingNameSz);

    /* Free the PID array memory if it was allocated */
    if (dvrPids != 0)
        mpe_memFreeP(MPE_MEM_TEMP, dvrPids);

    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_DVRAPIImpl_jniStop
(JNIEnv *env, jclass cls, jint dvr, jboolean holdFrame)
{
    mpe_Error err = MPE_SUCCESS;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jniStop(dvr=0x%X)", (int)dvr);

    uint32_t holdFrameMode = MPE_MEDIA_STOP_MODE_BLACK;
    if (holdFrame != JNI_FALSE)
    {
        holdFrameMode = MPE_MEDIA_STOP_MODE_HOLD_FRAME;
    }
    err = mpe_dvrPlayBackStop((mpe_DvrPlayback)dvr, holdFrameMode);
    if (err == MPE_SUCCESS)
    {
        ;
    }
    else
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "mpe_dvrPlayBackStop() = %d\n", err);
    }

    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_DVRAPIImpl_jniSetRate(
        JNIEnv *env, jclass cls, jint dvr, jfloatArray rateArray)
{
    mpe_Error err = MPE_SUCCESS;
    float newRate = 0.0;
    float actualRate = 0.0;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    /* Get the rate from rateArray[0]. */
    (*env)->GetFloatArrayRegion(env, rateArray, 0, 1, &newRate);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jniSetRate(dvr=0x%X, newRate=%.2f)\n",
            (int)dvr, newRate);

    /* Try to set the rate. */
    /* If successful, assign the returned rate to rateArray[0]. */
    err = mpe_dvrSetTrickMode((mpe_DvrPlayback) dvr, (float) newRate,
            &actualRate);
    if (err == MPE_SUCCESS)
    {
        (*env)->SetFloatArrayRegion(env, rateArray, 0, 1, &actualRate);
    }
    else
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "mpe_dvrSetTrickMode() = %d\n", err);
    }

    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_DVRAPIImpl_jniGetRate(
        JNIEnv *env, jobject cls, jint dvr, jfloatArray rateArray)
{
    mpe_Error err = MPE_SUCCESS;
    float currentRate = 0.0;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jniGetRate(dvr=0x%X)\n", (int)dvr);

    /* Get playback rate.  If successful, assign the returned rate to rateArray[0]. */
    err = mpe_dvrGetTrickMode((mpe_DvrPlayback) dvr, &currentRate);
    if (err == MPE_SUCCESS)
    {
        (*env)->SetFloatArrayRegion(env, rateArray, 0, 1, &currentRate);
    }
    else
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "mpe_dvrGetTrickMode() = %d\n", err);
    }

    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_DVRAPIImpl_jniSetMediaTime(
        JNIEnv *env, jobject cls, jint dvr, jlong dvrTime)
{
    mpe_Error err;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "jniSetMediaTime(dvr=0x%X, time=%"PRId64")\n", (int)dvr, (int64_t)dvrTime);

    err = mpe_dvrPlaybackSetTime((mpe_DvrPlayback) dvr, dvrTime);
    if (err == MPE_SUCCESS)
    {
        ;
    }
    else
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "mpe_dvrPlaybackSetTime( () = %d\n", err);
    }

    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_DVRAPIImpl_jniGetMediaTime(
        JNIEnv *env, jobject cls, jint dvr, jlongArray timeArray)
{
    mpe_Error err;
    int64_t dvrTime = 0;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jniGetMediaTime(dvr=0x%X)\n", (int)dvr);

    /** Get native media time and stuff into timeArray[0]. */
    err = mpe_dvrPlaybackGetTime((mpe_DvrPlayback) dvr, &dvrTime);
    if (err == MPE_SUCCESS)
    {
        (*env)->SetLongArrayRegion(env, timeArray, 0, 1, &dvrTime);
    }
    else
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "mpe_dvrPlaybackGetTime() = %d\n",
                err);
    }

    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_DVRAPIImpl_jniGetRecordingMediaTimeForFrame(
        JNIEnv *env, jobject cls, jstring recordingName, jlong mediaTime,
        jlongArray timeArray, jint direction)
{
    mpe_Error err = MPE_SUCCESS;
    int64_t frameTime = 0;
    const char *name;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "jniGetRecordingMediaTimeForFrame(mediaTime= %"PRId64" direction=%d)\n",
            (int64_t)mediaTime, (int)direction);

    /* Allocate recording name string */
    if ((name = (*env)->GetStringUTFChars(env, recordingName, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        return err;
    }
    (*env)->ReleaseStringUTFChars(env, recordingName, name);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "jniGetRecordingMediaTimeForFrame(recordingName=%s\n", name);

    /** Get native media time and stuff into timeArray[0]. */
    err = mpe_dvrRecordingMediaTimeForFrame((char*) name, (int64_t) mediaTime,
            direction, &frameTime);
    if (err == MPE_SUCCESS)
    {
        (*env)->SetLongArrayRegion(env, timeArray, 0, 1, &frameTime);
    }
    else
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "jniGetRecordingMediaTimeForFrame() Error= %d\n", err);
    }

    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_DVRAPIImpl_jniGetTsbMediaTimeForFrame(
        JNIEnv *env, jobject cls, jint tsbHandle, jlong mediaTime,
        jlongArray timeArray, jint direction)
{
    mpe_Error err = MPE_SUCCESS;
    int64_t frameTime = 0;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "jniGetTsbMediaTimeForFrame(mediaTime= %"PRId64" direction=%d)\n",
            (int64_t)mediaTime, (int)direction);

    /** Get native media time and stuff into timeArray[0]. */
    err = mpe_dvrTsbMediaTimeForFrame((mpe_DvrTsb) tsbHandle,
            (int64_t) mediaTime, direction, &frameTime);
    if (err == MPE_SUCCESS)
    {
        (*env)->SetLongArrayRegion(env, timeArray, 0, 1, &frameTime);
    }
    else
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "jniGetTsbMediaTimeForFrame() Error= %d\n", err);
    }

    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_DVRAPIImpl_jniStepFrame(
        JNIEnv *env, jobject cls, jint dvr, jint direction)
{
    mpe_Error err;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "jniStepFrame(dvr=0x%X direction=%d)\n", (int)dvr, (int)direction);

    /** Get native media time and stuff into timeArray[0]. */
    err = mpe_dvrPlaybackStepFrame((mpe_DvrPlayback) dvr,
            (mpe_DvrDirection) direction);
    if (err == MPE_SUCCESS)
    {
        ;
    }
    else
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "jniStepFrame() Error= %d\n", err);
    }

    return err;
}

/**
 * Block / unblock the presentation (audio and video) for the specified dvr playback session
 *
 * @param session - The dvr playback session to block / unblock
 * @param block - boolean indicating whether to block (true) or unblock (false)
 * @return a code indicating success or failure - 0 for success
 * Signature: (IZ)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_DVRAPIImpl_jniBlockPresentation(
        JNIEnv *env, jclass cls, jint session, jboolean block)
{
    mpe_Error err = MPE_SUCCESS;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "DVR: jniBlockPresentation(session=0x%X)\n", (int)session);

    err = mpeos_dvrPlaybackBlockPresentation((mpe_DvrPlayback) session,
            (mpe_Bool) block);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_JNI,
                "ERROR in DVR: jniBlockPresentation: mpeos_dvrPlaybackBlockPresentation() = %d\n",
                err);
    }

    return err;
}

/*
 * Param scanModeArray - By convention, scanMode is element 0.  This are filled 
 * in by this routine.
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_DVRAPIImpl_jniGetVideoScanMode(
        JNIEnv *env, jclass cls, jint sessionHandle, jintArray scanModeArray)
{
    mpe_Error err = MPE_SUCCESS;

    mpe_MediaScanMode scanMode;

    err = mpe_dvrPlaybackGetVideoScanMode((mpe_DvrPlayback) sessionHandle, &scanMode);
    if (err != 0)
    {
        return err;
    }

    (*env)->SetIntArrayRegion(env, scanModeArray, 0, 1, (jint *)(&scanMode));

    return err;
}

/*
 * Param specifierArray - By convention, payloadType is element 0, formatType
 * is element 1, and payloadSz is element 2 in the specifierArray.  These are filled in by this routine.
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_DVRAPIImpl_jniGetS3DConfiguration(
        JNIEnv *env, jclass cls, jint sessionHandle, jintArray specifierArray, jbyteArray payloadArray, jint payloadArraySz)
{
    mpe_Error err = MPE_SUCCESS;

    mpe_Media3DPayloadType payloadType;
    mpe_DispStereoscopicMode stereoscopicMode;

    uint32_t payloadSz = payloadArraySz;
    uint8_t *payload;
    err = mpe_memAllocP(MPE_MEM_TEMP, payloadArraySz, (void**) &payload);
    if (err != 0)
    {
        return err;
    }

    err = mpe_dvrPlaybackGet3DConfig((mpe_DvrPlayback) sessionHandle, &stereoscopicMode, &payloadType, payload, &payloadSz);
    if (err != 0)
    {
        mpe_memFreeP(MPE_MEM_TEMP, payload);
        return err;
    }


    (*env)->SetIntArrayRegion(env, specifierArray, 0, 1, (jint *)(&stereoscopicMode));
    (*env)->SetIntArrayRegion(env, specifierArray, 1, 1, (jint *)(&payloadType));
    (*env)->SetIntArrayRegion(env, specifierArray, 2, 1, (jint *)(&payloadSz));

    (*env)->SetByteArrayRegion(env, payloadArray, 0, payloadSz, (jbyte *)payload);

    mpe_memFreeP(MPE_MEM_TEMP, payload);

    return err;
}
