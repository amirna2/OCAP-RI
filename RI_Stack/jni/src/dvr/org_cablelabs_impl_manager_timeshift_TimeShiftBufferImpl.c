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

#include "org_cablelabs_impl_manager_timeshift_TimeShiftBufferImpl.h"
#include "jni_util.h"
#include <mgrdef.h>
#include <mpe_dvr.h>
#include <mpe_dbg.h>
#include <mpe_ed.h>
#include <mpe_os.h>
#include <inttypes.h>
extern int64_t getTSBEndMediaTime(jint tsbHandle);

////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////

/** See TimeShiftBufferImpl Javadoc for details */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_timeshift_TimeShiftBufferImpl_nInit
(JNIEnv *env, jclass cls)
{
    // Assert: Macro references env and cls

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "JNI(TSB): nInit()\n");

    GET_FIELD_ID(TimeShiftBufferImpl_tsbHandle, "nativeTSBHandle", "I");
    GET_FIELD_ID(TimeShiftBufferImpl_bufferingHandle, "nativeBufferingHandle", "I");
    GET_FIELD_ID(TimeShiftBufferImpl_effectiveMediaTime, "nativeEffectiveMediaTime", "J");

    // NOTE: These are also initialized by Java_org_cablelabs_impl_manager_recording_RecordingImpl_nInit */

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
}

////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////

/** See TimeShiftBufferImpl Javadoc for details */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_timeshift_TimeShiftBufferImpl_nTSBCreate(
        JNIEnv *env, jobject obj, jlong size)
{
    mpe_Error err;
    mpe_DvrTsb newTSBHandle;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "JNI(TSB): nTSBCreate(size %"PRId64")\n", size);

    err = mpe_dvrTsbNew(NULL, size, &newTSBHandle);

    if (err == MPE_SUCCESS)
    {
        // Store the TSB handle in the TimeShiftBufferImpl object
        (*env)->SetIntField(env, obj,
                jniutil_CachedIds.TimeShiftBufferImpl_tsbHandle,
                (int) newTSBHandle);
    }

    return err;
}
////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////

/*
 * Class:     org_cablelabs_impl_manager_timeshift_TimeShiftBufferImpl
 * Method:    nTSBGetStartTime
 * Signature: (I)J
 */
/** See TimeShiftBufferImpl Javadoc for details */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_manager_timeshift_TimeShiftBufferImpl_nTSBGetStartTime(
        JNIEnv *env, jobject obj, jint tsbHandle)
{
    mpe_Error err;
    jlong startTime;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(obj);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "JNI(TSB): nTSBGetStartTime(tsb %d)",
            (int)tsbHandle);

    err = mpe_dvrTsbGet((mpe_DvrTsb) tsbHandle, MPE_DVR_TSB_START_TIME,
            &startTime);

    if (err != MPE_SUCCESS)
    {
        startTime = -1;
    }

    return startTime;
}

////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////

/*
 * Class:     org_cablelabs_impl_manager_timeshift_TimeShiftBufferImpl
 * Method:    nTSBGetEndTime
 * Signature: (I)J
 */
/** See TimeShiftBufferImpl Javadoc for details */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_manager_timeshift_TimeShiftBufferImpl_nTSBGetEndTime(
        JNIEnv *env, jobject obj, jint tsbHandle)
{
    //    mpe_Error err;
    jlong endTime;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(obj);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "JNI(TSB): nTSBGetEndTime(tsb %d)",
            (int)tsbHandle);

    endTime = (jlong) getTSBEndMediaTime(tsbHandle);

    return endTime;
}

////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////

/*
 * Class:     org_cablelabs_impl_manager_timeshift_TimeShiftBufferImpl
 * Method:    nTSBGetDuration
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_manager_timeshift_TimeShiftBufferImpl_nTSBGetDuration(
        JNIEnv *env, jobject obj, jint tsbHandle)
{
    mpe_Error err;
    jlong startTime;
    jlong endTime;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(obj);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "JNI(TSB): nTSBGetDuration(tsb %d)",
            (int)tsbHandle);

    err = mpe_dvrTsbGet((mpe_DvrTsb) tsbHandle, MPE_DVR_TSB_START_TIME,
            &startTime);
    if (err != MPE_SUCCESS)
    {
        return -1;
    }

    err = mpe_dvrTsbGet((mpe_DvrTsb) tsbHandle, MPE_DVR_TSB_END_TIME, &endTime);
    if (err != MPE_SUCCESS)
    {
        return -2;
    }

    return (endTime - startTime);
}

////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////

/** See TimeShiftBufferImpl Javadoc for details */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_timeshift_TimeShiftBufferImpl_nTSBChangeSize(
        JNIEnv *env, jobject obj, jint tsbHandle, jlong size)
{
    mpe_Error err;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(obj);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "JNI(TSB): nTSBChangeSize(tsb %d, size %ld)", (int)tsbHandle, (long)size);

    err = mpe_dvrTsbChangeDuration((mpe_DvrTsb) tsbHandle, size);

    return err;
}

////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////

/** See TimeShiftBufferImpl Javadoc for details */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_timeshift_TimeShiftBufferImpl_nTSBDelete(
        JNIEnv *env, jobject obj, jint tsbHandle)
{
    mpe_Error err;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(obj);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "JNI(TSB): nTSBDelete(tsb %d)\n",
            (int)tsbHandle);

    err = mpe_dvrTsbDelete((mpe_DvrTsb) tsbHandle);

    return err;
}

////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_timeshift_TimeShiftBufferImpl_nTSBBufferingStart(
        JNIEnv *env, jobject obj, jint tunerID, jshort ltsid, jint tsbHandle,
        jobject listenerObj, jobject pidMapTable, jint bitrate,
        jlong desiredDuration, jlong maxDuration )
{
    mpe_Error err;
    mpe_DvrBuffering tsbBufferingHandle;
    uint32_t pidCount = 0;
    mpe_DvrPidInfo *dvrPids = NULL;
    mpe_EdEventInfo *edHandle;
    jobjectArray pidMapEntryArray;

    if (pidMapTable == 0)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "JNI(TSB): nTSBBufferingStart: ERROR - NULL PidMapTable\n");
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "JNI(TSB): nTSBBufferingStart(tuner %d, ltsid %d, tsb %d, bitrate %d)\n",
            (int)tunerID, (int)ltsid, (int)tsbHandle, (int)bitrate);

    // get the array of pid entries
    pidMapEntryArray = (*env)->GetObjectField(env, pidMapTable,
            jniutil_CachedIds.PidMapTable_pidMapEntryArray);
    pidCount = (*env)->GetIntField(env, pidMapTable,
            jniutil_CachedIds.PidMapTable_pidTableSize);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "JNI(TSB): nTSBBufferingStart: number of pids is %d\n", pidCount);
    if (pidCount == 0)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "JNI(TSB): nTSBBufferingStart: ERROR - number of pids is %d\n",
                pidCount);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    // allocate dvr pidInfo table
    err = mpe_memAllocP(MPE_MEM_TEMP, sizeof(mpe_DvrPidInfo) * pidCount,
            (void**) &dvrPids);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "JNI(TSB): nTSBBufferingStart: ERROR - failed to allocate pid table\n");
        return err;
    }

    // Create the ed handle to pass into the DVR APIs
    err = mpe_edCreateHandle(listenerObj, MPE_ED_QUEUE_NORMAL, NULL,
            MPE_ED_TERMINATION_EVCODE, MPE_DVR_EVT_SESSION_CLOSED, &edHandle);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "JNI(TSB): nTSBBufferingStart: ERROR - failed to create handle\n");
        return err;
    }

    jniutil_convertToDvrPidInfoArray(env, pidMapTable, dvrPids);

    err = mpe_dvrTsbBufferingStart(tunerID, (uint8_t)ltsid, (mpe_DvrTsb) tsbHandle, bitrate,
            desiredDuration, maxDuration,
            edHandle->eventQ, (void *) edHandle, dvrPids, pidCount,
            &tsbBufferingHandle);

    if (err == MPE_SUCCESS)
    {
        // Store the resulting TSB buffering handle
        (*env)->SetIntField(env, obj,
                jniutil_CachedIds.TimeShiftBufferImpl_bufferingHandle,
                (int) tsbBufferingHandle);

        // update the PidMapTable with buffered pids
        jniutil_updatePidMapTable(env, pidCount, dvrPids, pidMapEntryArray);
    }
    else
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_JNI,
                "JNI(TSB): nTSBBufferingStart: mpe_dvrTsbBufferingStart() returned error %d (0x%x)\n",
                err, err );
        mpe_edDeleteHandle(edHandle);
        edHandle = NULL;
    }

    // Free DVR PIDs
    if (dvrPids != NULL)
    {
        mpe_memFreeP(MPE_MEM_TEMP, dvrPids);
    }

    return err;
}

////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////

/** See TimeShiftBufferImpl Javadoc for details */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_timeshift_TimeShiftBufferImpl_nTSBBufferingChangePids(
        JNIEnv *env, jobject obj, jint tsbHandle, jint bufferingHandle,
        jobject pidMapTable)
{
    mpe_Error err;
    jobjectArray pidMapEntryArray;
    uint32_t p;
    uint32_t pidCount = 0;
    mpe_DvrPidInfo *dvrPids = NULL;
    jobject pidMapEntry;
    int64_t effectiveMediaTime = -1;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(obj);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "JNI(TSB): nTSBBufferingChangePids(bufsession %d)\n",
            (int)bufferingHandle);

    // get the array of pid entries
    pidMapEntryArray = (*env)->GetObjectField(env, pidMapTable,
            jniutil_CachedIds.PidMapTable_pidMapEntryArray);
    pidCount = (*env)->GetIntField(env, pidMapTable,
            jniutil_CachedIds.PidMapTable_pidTableSize);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "JNI(TSB): nTSBBufferingChangePids: number of pids is %d\n",
            pidCount);

    err = mpe_memAllocP(MPE_MEM_TEMP, sizeof(mpe_DvrPidInfo) * pidCount,
            (void**) &dvrPids);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "JNI(TSB): nTSBBufferingChangePids: ERROR - failed to allocate pid table\n");
        return err;
    }

    // walk through the pidEntryArray to create the DVR PidInfo table
    for (p = 0; p < pidCount; p++)
    {
        // get pidMapEntryArray[p] (a PidMapEntry object)
        pidMapEntry = (*env)->GetObjectArrayElement(env, pidMapEntryArray, p);

        if (pidMapEntry != NULL)
        {
            dvrPids[p].srcPid = (uint16_t)(*env)->GetIntField(env, pidMapEntry,
                    jniutil_CachedIds.PidMapEntry_srcPID);
            dvrPids[p].srcEltStreamType = (*env)->GetShortField(env,
                    pidMapEntry,
                    jniutil_CachedIds.PidMapEntry_srcElementaryStreamType);

            dvrPids[p].recPid = (uint16_t)(*env)->GetIntField(env, pidMapEntry,
                    jniutil_CachedIds.PidMapEntry_recPID);
            dvrPids[p].recEltStreamType = (*env)->GetShortField(env,
                    pidMapEntry,
                    jniutil_CachedIds.PidMapEntry_recElementaryStreamType);

            dvrPids[p].streamType = (*env)->GetShortField(env, pidMapEntry,
                    jniutil_CachedIds.PidMapEntry_streamType);
        }
    }

    // Passing NULL as an array of mpe_DvrPidInfo for now */
    err = mpe_dvrTsbBufferingChangePids((mpe_DvrBuffering) bufferingHandle,
            dvrPids, pidCount, &effectiveMediaTime);

    if (err == MPE_SUCCESS)
    {
        // update the PidMapTable with buffered pids
        jniutil_updatePidMapTable(env, pidCount, dvrPids, pidMapEntryArray);

        if (effectiveMediaTime == -1)
        {
            // get the time through other means.  Work around for mpeos not fully implementing mpe_dvrTsbBufferingChangePids
            effectiveMediaTime = getTSBEndMediaTime(tsbHandle);
MPE_LOG        (MPE_LOG_DEBUG, MPE_MOD_JNI, "JNI(TSB): nTSBBufferingChangePids: getTSBEndMediaTime returned effectiveMediaTime=%"PRId64"\n", effectiveMediaTime);
    }
    else
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "JNI(TSB): nTSBBufferingChangePids: jniutil_updatePidMapTable returned effectiveMediaTime=%"PRId64"\n", effectiveMediaTime);
    }

    // set the java level variable
    (*env)->SetLongField( env, obj,
            jniutil_CachedIds.TimeShiftBufferImpl_effectiveMediaTime,
            (jlong) effectiveMediaTime);
}
// Free DVR PIDs
if (dvrPids != NULL)
{
    mpe_memFreeP(MPE_MEM_TEMP, dvrPids);
}

return err;
} // nTSBBufferingChangePids()

////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////

/** See TimeShiftBufferImpl Javadoc for details */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_timeshift_TimeShiftBufferImpl_nTSBBufferingChangeDuration(
        JNIEnv *env, jobject obj,
        jint bufferingHandle, jlong desiredDuration, jlong maxDuration )
{
    mpe_Error err;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(obj);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "JNI(TSB): nTSBBufferingChangeDuration(bufsession %d (0x%x),desiredDur %ld, maxDur %ld)\n",
            (int)bufferingHandle, (int)bufferingHandle,
            desiredDuration, maxDuration );

    err = mpeos_dvrTsbBufferingChangeDuration( (mpe_DvrBuffering) bufferingHandle,
                                               desiredDuration,
                                               maxDuration );

    return err;
}

////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////

/** See TimeShiftBufferImpl Javadoc for details */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_timeshift_TimeShiftBufferImpl_nTSBBufferingStop(
        JNIEnv *env, jobject obj, jint bufferingHandle)
{
    mpe_Error err;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(obj);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "JNI(TSB): nTSBBufferingStop(bufsession %d)\n", (int)bufferingHandle);

    err = mpe_dvrTsbBufferingStop((mpe_DvrBuffering) bufferingHandle);

    return err;
}

////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////

int64_t getTSBEndMediaTime(jint tsbHandle)
{
    mpe_Error err;
    int64_t endMediaTime = -1;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "JNI(TSB): getTSBEndTime(tsb %d)",
            (int)tsbHandle);

    err = mpe_dvrTsbGet((mpe_DvrTsb) tsbHandle, MPE_DVR_TSB_END_TIME,
            &endMediaTime);

    if (err != MPE_SUCCESS)
    {
        endMediaTime = -1;
    }

    return endMediaTime;
}

////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////
