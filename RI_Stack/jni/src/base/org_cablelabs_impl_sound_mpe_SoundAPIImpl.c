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

#include "org_cablelabs_impl_sound_mpe_SoundAPIImpl.h"
#include <mpe_ed.h>
#include <mpe_snd.h>
#include "jni_util.h"

/******************************************************************************/

// only need to create one edHandle.
static mpe_EdHandle edHandle = NULL;

/*
 * Class:     org_cablelabs_impl_sound_mpe_SoundAPIImpl
 * Method:    jniInit
 * Signature: (Lorg/cablelabs/impl/manager/ed/EDListener;)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_sound_mpe_SoundAPIImpl_jniInit(
        JNIEnv *env, jclass cls, jobject listener)
{
    mpe_Error mpe_result = MPE_SUCCESS;
    JNI_UNUSED(env);
    JNI_UNUSED(cls);

    // if we haven't yet created the mpe_EdHandle, then do it.
    if (edHandle == NULL)
    {
        // Create an ED handle
        mpe_result = mpe_edCreateHandle(listener, // listener object
                MPE_ED_QUEUE_NORMAL, // eventQId
                NULL, // edNativeCallback
                MPE_ED_TERMINATION_OPEN, // terminationType
                0, // terminationCode
                &edHandle); // mpe_EdEventInfo

        if (mpe_result != MPE_SUCCESS)
        {
            edHandle = NULL;
        }
    }
    return mpe_result;
}

/*
 * Class:     org_cablelabs_impl_sound_mpe_SoundAPIImpl
 * Method:    jniGetSoundDeviceCount
 * Signature: ([I)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_sound_mpe_SoundAPIImpl_jniGetSoundDeviceCount(
        JNIEnv *env, jclass cls, jintArray devCount)
{
    jint* devCntPtr = NULL;
    mpe_Error mpe_result = MPE_SUCCESS;
    uint32_t count = 0;
    JNI_UNUSED(cls);

    mpe_result = mpe_sndGetDeviceCount(&count);
    if (mpe_result == MPE_SUCCESS)
    {
        devCntPtr = (*env)->GetIntArrayElements(env, devCount, NULL);
        devCntPtr[0] = count;
        (*env)->ReleaseIntArrayElements(env, devCount, devCntPtr, 0);
    }

    return mpe_result;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_sound_mpe_SoundAPIImpl_jniSetMute(
        JNIEnv *env, jclass cls, jint sndHandle, jboolean mute)
{
    mpe_Error err = MPE_SUCCESS;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    /* If successful, assign the returned mute to muteArray[0]. */
    err = mpe_sndSetMute((mpe_SndPlayback) sndHandle, (mpe_Bool)mute);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "mpe_mediaSetMute() = %d\n", err);
    }

    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_sound_mpe_SoundAPIImpl_jniSetGain(
        JNIEnv *env, jclass cls, jint sndHandle, jfloatArray gainArray)
{
    mpe_Error err = MPE_SUCCESS;
    float newGain;
    float actualGain;

    MPE_UNUSED_PARAM(cls);

    /* Get the gain from gainArray[0]. */
    (*env)->GetFloatArrayRegion(env, gainArray, 0, 1, &newGain);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jniSetGain(newGain=%.2f)\n", newGain);

    /* Try to set the gain. */
    /* If successful, assign the returned gain to gainArray[0]. */
    err = mpe_sndSetGain((mpe_SndPlayback) sndHandle, (float)newGain, &actualGain);
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

/*
 * Class:     org_cablelabs_impl_sound_mpe_SoundAPIImpl
 * Method:    jniGetSoundDevices
 * Signature: ([I)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_sound_mpe_SoundAPIImpl_jniGetSoundDevices___3I(
        JNIEnv *env, jclass cls, jintArray devHandles)
{
    jint* devHndlPtr = NULL;
    mpe_Error mpe_result = MPE_SUCCESS;
    jsize handleCount = 0;
    JNI_UNUSED(cls);

    handleCount = (*env)->GetArrayLength(env, devHandles);
    devHndlPtr = (*env)->GetIntArrayElements(env, devHandles, NULL);

    mpe_result = mpe_sndGetDevices((mpe_SndDevice*) devHndlPtr,
            (uint32_t*) &handleCount);

    (*env)->ReleaseIntArrayElements(env, devHandles, devHndlPtr, 0);
    return mpe_result;
}

/*
 * Class:     org_cablelabs_impl_sound_mpe_SoundAPIImpl
 * Method:    jniGetSoundDevices
 * Signature: (I[I[I)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_sound_mpe_SoundAPIImpl_jniGetSoundDevices__I_3I_3I(
        JNIEnv *env, jclass cls, jint sndHandle, jintArray devHandles,
        jintArray devCount)
{
    jint* devHndlPtr = NULL;
    jint* devCntPtr = NULL;
    mpe_Error mpe_result = MPE_SUCCESS;
    uint32_t actualCount = 0;
    JNI_UNUSED(cls);

    // Get the length of the devHandle array, and obtain a lock on the array itself
    devHndlPtr = (*env)->GetIntArrayElements(env, devHandles, NULL);

    mpe_result = mpe_sndGetDevicesForSound((mpe_SndSound) sndHandle,
            (mpe_SndDevice*) devHndlPtr, &actualCount);

    if (mpe_result == MPE_SUCCESS)
    {
        // Lock the devCount array, set the value, and release it
        devCntPtr = (*env)->GetIntArrayElements(env, devCount, NULL);
        devCntPtr[0] = actualCount;
        (*env)->ReleaseIntArrayElements(env, devCount, devCntPtr, 0);
    }

    // Release the lock on the devHandles array
    (*env)->ReleaseIntArrayElements(env, devHandles, devHndlPtr, 0);
    return mpe_result;
}

/*
 * Class:     org_cablelabs_impl_sound_mpe_SoundAPIImpl
 * Method:    jniCreateSound
 * Signature: (Ljava/lang/String;[BII[I)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_sound_mpe_SoundAPIImpl_jniCreateSound(
        JNIEnv *env, jclass cls, jstring mimeType, jbyteArray sndData,
        jint offset, jint size, jintArray sndHandle)
{
    jint* sndHndlPtr = NULL;
    mpe_Error mpe_result = MPE_SUCCESS;
    jbyte* sndDataPtr = NULL;
    const char *mimeTypePtr = NULL;
    mpe_SndSound sound;
    JNI_UNUSED(cls);

    // parameter check (note that mimeType *can* be null)
    if ((sndData == NULL) || (sndHandle == NULL))
    {
        return MPE_EINVAL;
    }

    // Get the byte array of the mimeType, sound handles, and sound data
    if ((mimeType != NULL) && ((mimeTypePtr = (*env)->GetStringUTFChars(env,
            mimeType, NULL)) == NULL))
    {
        /* GetStringUTFChars threw a memory exception */
        return MPE_EINVAL;
    }
    sndHndlPtr = (*env)->GetIntArrayElements(env, sndHandle, NULL);
    sndDataPtr = (*env)->GetByteArrayElements(env, sndData, NULL);

    mpe_result = mpe_sndCreateSound((char*) mimeTypePtr, (char*) sndDataPtr,
            offset, size, &sound);
    if (mpe_result == MPE_SUCCESS)
    {
        sndHndlPtr[0] = (int) sound;
    }

    (*env)->ReleaseByteArrayElements(env, sndData, sndDataPtr, 0);
    (*env)->ReleaseIntArrayElements(env, sndHandle, sndHndlPtr, 0);
    if (mimeType != NULL)
    {
        (*env)->ReleaseStringUTFChars(env, mimeType, mimeTypePtr);
    }

    return mpe_result;
}

/*
 * Class:     org_cablelabs_impl_sound_mpe_SoundAPIImpl
 * Method:    jniDestroySound
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_sound_mpe_SoundAPIImpl_jniDestroySound(
        JNIEnv *env, jclass cls, jint sndHandle)
{
    JNI_UNUSED(cls);
    JNI_UNUSED(env);
    return mpe_sndDeleteSound((mpe_SndSound) sndHandle);
}

/*
 * Class:     org_cablelabs_impl_sound_mpe_SoundAPIImpl
 * Method:    jniPlaySound
 * Signature: (IILorg/cablelabs/impl/manager/ed/EDListener;JZ[I)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_sound_mpe_SoundAPIImpl_jniPlaySound(
        JNIEnv *env, jclass cls, jint devHandle, jint sndHandle, jlong start,
        jboolean loop, jboolean muted, jfloatArray gainArray, jintArray pbHandle)
{
    jint* pbHndlPtr = NULL;
    mpe_Error mpe_result = MPE_SUCCESS;
    mpe_SndPlayback playback;
    JNI_UNUSED(cls);
    float requestedGain = 0;
    float actualGain = 0;

    /* Get the gain from gainArray[0]. */
    (*env)->GetFloatArrayRegion(env, gainArray, 0, 1, &requestedGain);

    mpe_result = mpe_sndPlay((mpe_SndDevice) devHandle,
            (mpe_SndSound) sndHandle, edHandle, (int64_t) start, loop, muted, requestedGain, &actualGain,
            &playback);
    if (mpe_result == MPE_SUCCESS)
    {
        pbHndlPtr = (*env)->GetIntArrayElements(env, pbHandle, NULL);
        pbHndlPtr[0] = (jint) playback;
        (*env)->ReleaseIntArrayElements(env, pbHandle, pbHndlPtr, 0);
        (*env)->SetFloatArrayRegion(env, gainArray, 0, 1, &actualGain);
    }
    return mpe_result;
}

/*
 * Class:     org_cablelabs_impl_sound_mpe_SoundAPIImpl
 * Method:    jniStopSoundPlayback
 * Signature: (I[J)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_sound_mpe_SoundAPIImpl_jniStopSoundPlayback(
        JNIEnv *env, jclass cls, jint pbHandle, jlongArray stopTime)
{
    jlong* timePtr = NULL;
    mpe_Error mpe_result = MPE_SUCCESS;
    JNI_UNUSED(cls);

    timePtr = (*env)->GetLongArrayElements(env, stopTime, NULL);

    mpe_result = mpe_sndStop((mpe_SndPlayback) pbHandle, (int64_t*) timePtr);
    (*env)->ReleaseLongArrayElements(env, stopTime, timePtr, 0);
    return mpe_result;
}

/*
 * Class:     org_cablelabs_impl_sound_mpe_SoundAPIImpl
 * Method:    jniGetSoundPlaybackTime
 * Signature: (I[J)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_sound_mpe_SoundAPIImpl_jniGetSoundPlaybackTime(
        JNIEnv *env, jclass cls, jint pbHandle, jlongArray pbTime)
{
    jlong* timePtr = NULL;
    mpe_Error mpe_result = MPE_SUCCESS;
    JNI_UNUSED(cls);

    timePtr = (*env)->GetLongArrayElements(env, pbTime, NULL);

    mpe_result = mpe_sndGetTime((mpe_SndPlayback) pbHandle, (int64_t*) timePtr);
    (*env)->ReleaseLongArrayElements(env, pbTime, timePtr, 0);
    return mpe_result;
}

/*
 * Class:     org_cablelabs_impl_sound_mpe_SoundAPIImpl
 * Method:    jniSetSoundPlaybackTime
 * Signature: (I[J)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_sound_mpe_SoundAPIImpl_jniSetSoundPlaybackTime(
        JNIEnv *env, jclass cls, jint pbHandle, jlongArray pbTime)
{
    jlong* timePtr = NULL;
    mpe_Error mpe_result = MPE_SUCCESS;
    JNI_UNUSED(cls);

    timePtr = (*env)->GetLongArrayElements(env, pbTime, NULL);

    mpe_result = mpe_sndSetTime((mpe_SndPlayback) pbHandle, (int64_t*) timePtr);
    (*env)->ReleaseLongArrayElements(env, pbTime, timePtr, 0);
    return mpe_result;
}

/*
 * Class:     org_cablelabs_impl_sound_mpe_SoundAPIImpl
 * Method:    jniGetDeviceMaxPlaybacks
 * Signature: (I[I)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_sound_mpe_SoundAPIImpl_jniGetDeviceMaxPlaybacks(
        JNIEnv *env, jclass cls, jint devHandle, jintArray maxPB)
{
    jint* maxPBPtr = NULL;
    mpe_Error mpe_result = MPE_SUCCESS;
    int32_t maxPlaybacks = 0;
    JNI_UNUSED(cls);

    mpe_result = mpe_sndGetMaxPlaybacks((mpe_SndDevice) devHandle,
            &maxPlaybacks);
    if (mpe_result == MPE_SUCCESS)
    {
        maxPBPtr = (*env)->GetIntArrayElements(env, maxPB, NULL);
        maxPBPtr[0] = (jint) maxPlaybacks;
        (*env)->ReleaseIntArrayElements(env, maxPB, maxPBPtr, 0);
    }

    return mpe_result;
}
