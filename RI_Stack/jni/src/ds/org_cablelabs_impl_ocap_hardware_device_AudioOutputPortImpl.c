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

#include <org_cablelabs_impl_ocap_hardware_device_AudioOutputPortImpl.h>
#include "jni_util.h"
#include <mpe_disp.h>
#include <mpe_snd.h>

/**
 * Initializes JNI.
 */
JNIEXPORT void JNICALL
Java_org_cablelabs_impl_ocap_hardware_device_AudioOutputPortImpl_nInit(
        JNIEnv *env, jclass cls)
{
    /* Lookup fields for AudioOutputPortImpl */

    /* boolean */
    GET_FIELD_ID(AudioOutputPortImpl_loopThru, "loopThru", "Z");
    GET_FIELD_ID(AudioOutputPortImpl_muted, "muted", "Z");

    /* integer */
    GET_FIELD_ID(AudioOutputPortImpl_audioPortHandle, "audioPortHandle", "I");
    GET_FIELD_ID(AudioOutputPortImpl_compression, "compression", "I");
    GET_FIELD_ID(AudioOutputPortImpl_encoding, "encoding", "I");
    GET_FIELD_ID(AudioOutputPortImpl_stereoMode, "stereoMode", "I");

    /* float */
    GET_FIELD_ID(AudioOutputPortImpl_gain, "gain", "F");
    GET_FIELD_ID(AudioOutputPortImpl_level, "level", "F");
    GET_FIELD_ID(AudioOutputPortImpl_optimalLevel, "optimalLevel", "F");
    GET_FIELD_ID(AudioOutputPortImpl_maxDb, "maxDb", "F");
    GET_FIELD_ID(AudioOutputPortImpl_minDb, "minDb", "F");

    /* string */
    GET_FIELD_ID(AudioOutputPortImpl_uniqueId, "uniqueId", "Ljava/lang/String;");

    /* integer arrays */
    GET_FIELD_ID(AudioOutputPortImpl_supportedCompressions, "supportedCompressions", "[I");
    GET_FIELD_ID(AudioOutputPortImpl_supportedEncodings, "supportedEncodings", "[I");
    GET_FIELD_ID(AudioOutputPortImpl_supportedStereoModes, "supportedStereoModes", "[I");

}

/**
 *
 * @param idx index returned from VideoOutputPort structure
 */
JNIEXPORT jboolean JNICALL
Java_org_cablelabs_impl_ocap_hardware_device_AudioOutputPortImpl_nInitInfo(
        JNIEnv *env, jobject obj, jint videoPortHandle)
{
    jintArray jArray;
    jstring uniqueId;
    mpe_DispOutputPortInfo videoInfo;
    mpe_SndAudioOutputPortInfo audioInfo;
    mpe_DispOutputPort port = (mpe_DispOutputPort) videoPortHandle;

    if (MPE_SUCCESS!= mpe_dispGetOutputPortInfo(port, &videoInfo))
    {
        return JNI_FALSE;
    }

    if (MPE_SUCCESS != mpe_sndGetAudioOutputPortInfo(videoInfo.audioPort, &audioInfo))
    {
        return JNI_FALSE;
    }
    else
    {
        /* update the index into the audio port structure */
        (*env)->SetIntField(env, obj, jniutil_CachedIds.AudioOutputPortImpl_audioPortHandle, (jint) videoInfo.audioPort);

        /* update the unique id */
        uniqueId = (*env)->NewStringUTF(env, audioInfo.idString);
        (*env)->SetObjectField(env, obj, jniutil_CachedIds.AudioOutputPortImpl_uniqueId, uniqueId);

        /* Update the boolean fields. */
        (*env)->SetBooleanField(env, obj, jniutil_CachedIds.AudioOutputPortImpl_loopThru, JNI_ISTRUE(audioInfo.loopThru));
        (*env)->SetBooleanField(env, obj, jniutil_CachedIds.AudioOutputPortImpl_muted, JNI_ISTRUE(audioInfo.muted));

        /* Update integer fields. */
        (*env)->SetIntField(env, obj, jniutil_CachedIds.AudioOutputPortImpl_compression, audioInfo.compression);
        (*env)->SetIntField(env, obj, jniutil_CachedIds.AudioOutputPortImpl_encoding, audioInfo.encoding);
        (*env)->SetIntField(env, obj, jniutil_CachedIds.AudioOutputPortImpl_stereoMode, audioInfo.stereoMode);

        /* Update float fields */
        (*env)->SetFloatField(env, obj, jniutil_CachedIds.AudioOutputPortImpl_gain, audioInfo.gain);
        (*env)->SetFloatField(env, obj, jniutil_CachedIds.AudioOutputPortImpl_level, audioInfo.level);
        (*env)->SetFloatField(env, obj, jniutil_CachedIds.AudioOutputPortImpl_optimalLevel, audioInfo.optimalLevel);
        (*env)->SetFloatField(env, obj, jniutil_CachedIds.AudioOutputPortImpl_maxDb, audioInfo.maxDb);
        (*env)->SetFloatField(env, obj, jniutil_CachedIds.AudioOutputPortImpl_minDb, audioInfo.minDb);

        /*
         * update integer arrays
         */
        /* supported compressions */
        jArray = (*env)->NewIntArray(env, audioInfo.supportedCompressionsSize);
        jint *elems = (*env)->GetIntArrayElements(env,jArray,NULL);
        int i = 0;
        for (i = 0; i < audioInfo.supportedCompressionsSize; i++)
        {
            elems[i] = audioInfo.supportedCompressions[i];
        }
        (*env)->SetObjectField(env, obj, jniutil_CachedIds.AudioOutputPortImpl_supportedCompressions, jArray);
        (*env)->ReleaseIntArrayElements(env, jArray, elems, 0);

        /* supportedEncodings */
        jArray = (*env)->NewIntArray(env, audioInfo.supportedEncodingsSize);
        elems = (*env)->GetIntArrayElements(env,jArray,NULL);
        for (i = 0; i < audioInfo.supportedEncodingsSize; i++)
        {
            elems[i] = audioInfo.supportedEncodings[i];
        }
        (*env)->SetObjectField(env, obj, jniutil_CachedIds.AudioOutputPortImpl_supportedEncodings, jArray);
        (*env)->ReleaseIntArrayElements(env, jArray, elems, 0);

        /* supportedStereoModes  */
        jArray = (*env)->NewIntArray(env, audioInfo.supportedStereoModesSize);
        elems = (*env)->GetIntArrayElements(env,jArray,NULL);
        for (i = 0; i < audioInfo.supportedStereoModesSize; i++)
        {
            elems[i] = audioInfo.supportedStereoModes[i];
        }
        (*env)->SetObjectField(env, obj, jniutil_CachedIds.AudioOutputPortImpl_supportedStereoModes, jArray);
        (*env)->ReleaseIntArrayElements(env, jArray, elems, 0);
    }
    return JNI_TRUE;
}

static jclass actualValueCls = NULL;
static jfieldID actualValueIntField = NULL;
static jfieldID actualValueFloatField = NULL;

jboolean setupActualValueJni(JNIEnv *env, jobject actualValueObj)
{
    if (NULL == actualValueCls)
    {
        actualValueCls = (*env)->GetObjectClass(env, actualValueObj);
        actualValueIntField = (*env)->GetFieldID(env, actualValueCls,
                "intValue", "I");
        if (actualValueIntField == NULL)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                    "Couldn't find field ActualValue.integerValue\n");
            return JNI_FALSE;
        }

        actualValueFloatField = (*env)->GetFieldID(env, actualValueCls,
                "floatValue", "F");
        if (actualValueFloatField == NULL)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                    "Couldn't find field ActualValue.floatValue\n");
            return JNI_FALSE;
        }
    }
    return JNI_TRUE;
}

jboolean setIntPortValue(JNIEnv *env, jint audioPortHandle, jint value,
        jobject actualValueObj, mpe_SndAudioValueId valueId)
{
    jint actualValue;

    if (JNI_TRUE != setupActualValueJni(env, actualValueObj))
    {
        return JNI_FALSE;
    }

    if (MPE_SUCCESS != mpe_sndSetAudioOutputPortValue(
            (mpe_SndAudioPort) audioPortHandle, valueId, (void*) &value,
            (void*) &actualValue))
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "mpe_sndSetAudioOutputPortValue failed\n");
        return JNI_FALSE;
    }

    (*env)->SetIntField(env, actualValueObj, actualValueIntField, actualValue);

    return JNI_TRUE;
}

jboolean setFloatPortValue(JNIEnv *env, jint audioPortHandle, jfloat value,
        jobject actualValueObj, mpe_SndAudioValueId valueId)
{
    jfloat actualValue;

    if (JNI_TRUE != setupActualValueJni(env, actualValueObj))
    {
        return JNI_FALSE;
    }

    if (MPE_SUCCESS != mpe_sndSetAudioOutputPortValue(
            (mpe_SndAudioPort) audioPortHandle, valueId, (void*) &value,
            (void*) &actualValue))
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "mpe_sndSetAudioOutputPortValue failed\n");
        return JNI_FALSE;
    }

    (*env)->SetFloatField(env, actualValueObj, actualValueFloatField,
            actualValue);

    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_org_cablelabs_impl_ocap_hardware_device_AudioOutputPortImpl_nSetCompression(
        JNIEnv *env, jclass cls, jint audioPortHandle, jint value,
        jobject actualValueObj)
{
    MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI,"nSetCompression 0x%x\n", (int) value );
    return setIntPortValue(env, audioPortHandle, value, actualValueObj,
            AUDIO_PORT_COMPRESSION_VALUE_ID);
}

JNIEXPORT jboolean JNICALL
Java_org_cablelabs_impl_ocap_hardware_device_AudioOutputPortImpl_nSetGain(
        JNIEnv *env, jclass cls, jint audioPortHandle, jfloat value,
        jobject actualValueObj)
{
    return setFloatPortValue(env, audioPortHandle, value, actualValueObj,
            AUDIO_PORT_GAIN_VALUE_ID);
}

JNIEXPORT jboolean JNICALL
Java_org_cablelabs_impl_ocap_hardware_device_AudioOutputPortImpl_nSetLevel(
        JNIEnv *env, jclass cls, jint audioPortHandle, jfloat value,
        jobject actualValueObj)
{
    return setFloatPortValue(env, audioPortHandle, value, actualValueObj,
            AUDIO_PORT_LEVEL_VALUE_ID);
}

JNIEXPORT jboolean JNICALL
Java_org_cablelabs_impl_ocap_hardware_device_AudioOutputPortImpl_nSetEncoding(
        JNIEnv *env, jclass cls, jint audioPortHandle, jint value,
        jobject actualValueObj)
{
    return setIntPortValue(env, audioPortHandle, value, actualValueObj,
            AUDIO_PORT_ENCODING_VALUE_ID);
}

JNIEXPORT jboolean JNICALL
Java_org_cablelabs_impl_ocap_hardware_device_AudioOutputPortImpl_nSetLoopThru(
        JNIEnv *env, jclass cls, jint audioPortHandle, jboolean value)
{
    if (MPE_SUCCESS!= mpe_sndSetAudioOutputPortValue((mpe_SndAudioPort)audioPortHandle, AUDIO_PORT_LOOP_THRU_VALUE_ID, (void*)&value, NULL))
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,"Java_org_cablelabs_impl_ocap_hardware_device_AudioOutputPortImpl_nSetLoopThru failed\n");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_org_cablelabs_impl_ocap_hardware_device_AudioOutputPortImpl_nSetMuting(
        JNIEnv *env, jclass cls, jint audioPortHandle, jboolean value)
{
    mpe_Bool mute_value = value;
    if (MPE_SUCCESS!= mpe_sndSetAudioOutputPortValue((mpe_SndAudioPort)audioPortHandle, AUDIO_PORT_MUTED_VALUE_ID, (void*)&mute_value, NULL))
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,"Java_org_cablelabs_impl_ocap_hardware_device_AudioOutputPortImpl_nSetMuting failed\n");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_org_cablelabs_impl_ocap_hardware_device_AudioOutputPortImpl_nSetStereoMode(
        JNIEnv *env, jclass cls, jint audioPortHandle, jint value)
{
    if (MPE_SUCCESS!= mpe_sndSetAudioOutputPortValue((mpe_SndAudioPort)audioPortHandle, AUDIO_PORT_STEREO_MODE_VALUE_ID, (void*)&value, NULL))
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,"Java_org_cablelabs_impl_ocap_hardware_device_AudioOutputPortImpl_nSetStereoMode failed\n");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}
