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

#include <org_cablelabs_impl_media_mpe_HNAPIImpl.h>
#include "jni_util.h"
#include "jni_util_hn.h"
#include <jni.h>
#include <string.h>
#include <mpe_dbg.h>
#include <mpe_hn.h>
#include <mpe_ed.h>
#include <mpe_os.h>
#include <mpe_socket.h>
#include <mpeos_dll.h>

#include <inttypes.h> // for PRIx64

// Define the memory to be HN allocated category
#define MPE_MEM_DEFAULT MPE_MEM_HN

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    jniInit
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_jniInit
  (JNIEnv *env, jclass cls)
{
    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "HN jniInit - Enter\n");

    GET_CLASS(String, "java/lang/String");

    GET_CLASS(Socket, "java/net/Socket");

    GET_CLASS(Time, "javax/media/Time");
    GET_METHOD_ID(Time_init, "<init>", "(J)V");

    GET_CLASS(MPEMediaError, "org/cablelabs/impl/media/mpe/MPEMediaError");
    GET_METHOD_ID(MPEMediaError_init, "<init>", "(ILjava/lang/String;)V");

    GET_CLASS(HNAPI_Playback, "org/cablelabs/impl/media/mpe/HNAPI$Playback");
    GET_METHOD_ID(HNAPI_Playback_init, "<init>", "(IFF)V");

    GET_CLASS(HNAPI_LPEWakeUp, "org/cablelabs/impl/media/mpe/HNAPI$LPEWakeUp");
    GET_METHOD_ID(HNAPI_LPEWakeUp_init, "<init>", "(Ljava/lang/String;Ljava/lang/String;II)V");
    
    GET_CLASS(HNStreamParams,
            "org/cablelabs/impl/media/streaming/session/data/HNStreamParams");
    GET_METHOD_ID(HNStreamParams_getStreamType, "getStreamType", "()I");

    GET_CLASS(HNStreamParamsMediaServerHttp,
            "org/cablelabs/impl/media/streaming/session/data/HNStreamParamsMediaServerHttp");
    GET_FIELD_ID(HNStreamParamsMediaServerHttp_connectionId, "m_connectionId", "I");
    GET_FIELD_ID(HNStreamParamsMediaServerHttp_dlnaProfileId, "m_dlnaProfileId", "Ljava/lang/String;");
    GET_FIELD_ID(HNStreamParamsMediaServerHttp_mimeType, "m_mimeType", "Ljava/lang/String;");
    GET_FIELD_ID(HNStreamParamsMediaServerHttp_mpeSocket, "m_mpeSocket", "I");
    GET_FIELD_ID(HNStreamParamsMediaServerHttp_chunkedEncodingMode, "m_chunkedEncodingMode", "I");
    GET_FIELD_ID(HNStreamParamsMediaServerHttp_maxTrickModeBandwidth, "m_maxTrickModeBandwidth", "J");
    GET_FIELD_ID(HNStreamParamsMediaServerHttp_currentDecodePTS, "m_currentDecodePTS", "J");
    GET_FIELD_ID(HNStreamParamsMediaServerHttp_maxGOPsPerChunk, "m_maxGOPsPerChunk", "I");
    GET_FIELD_ID(HNStreamParamsMediaServerHttp_maxFramesPerGOP, "m_maxFramesPerGOP", "I");
    GET_FIELD_ID(HNStreamParamsMediaServerHttp_useServerSidePacing, "m_useServerSidePacing", "Z");
    GET_FIELD_ID(HNStreamParamsMediaServerHttp_frameTypesInTrickModes, "m_frameTypesInTrickModes", "I");
    GET_FIELD_ID(HNStreamParamsMediaServerHttp_connectionStallingTimeoutMS, "m_connectionStallingTimeoutMS", "I");

    GET_CLASS(HNStreamParamsMediaPlayerHttp,
            "org/cablelabs/impl/media/streaming/session/data/HNStreamParamsMediaPlayerHttp");
    GET_FIELD_ID(HNStreamParamsMediaPlayerHttp_connectionId, "m_connectionId", "I");
    GET_FIELD_ID(HNStreamParamsMediaPlayerHttp_uri, "m_uri", "Ljava/lang/String;");
    GET_FIELD_ID(HNStreamParamsMediaPlayerHttp_dlnaProfileId, "m_dlnaProfileId", "Ljava/lang/String;");
    GET_FIELD_ID(HNStreamParamsMediaPlayerHttp_mimeType, "m_mimeType", "Ljava/lang/String;");
    GET_FIELD_ID(HNStreamParamsMediaPlayerHttp_host, "m_host", "Ljava/lang/String;");
    GET_FIELD_ID(HNStreamParamsMediaPlayerHttp_port, "m_port", "I");
    GET_FIELD_ID(HNStreamParamsMediaPlayerHttp_dtcpHost, "m_dtcpHost", "Ljava/lang/String;");
    GET_FIELD_ID(HNStreamParamsMediaPlayerHttp_dtcpPort, "m_dtcpPort", "I");

    // *TODO* - reverted MPEOS change
    /*
    GET_FIELD_ID(HNStreamParamsMediaPlayerHttp_isTimeSeekSupported, "m_isTimeSeekSupported", "Z");
    GET_FIELD_ID(HNStreamParamsMediaPlayerHttp_isRangeSupported, "m_isRangeSupported", "Z");
    GET_FIELD_ID(HNStreamParamsMediaPlayerHttp_isSenderPaced, "m_isSenderPaced", "Z");
    GET_FIELD_ID(HNStreamParamsMediaPlayerHttp_isLimitedTimeSeekSupported, "m_isLimitedTimeSeekSupported", "Z");
    GET_FIELD_ID(HNStreamParamsMediaPlayerHttp_isLimitedByteSeekSupported, "m_isLimitedByteSeekSupported", "Z");
    GET_FIELD_ID(HNStreamParamsMediaPlayerHttp_isPlayContainer, "m_isPlayContainer", "Z");
    GET_FIELD_ID(HNStreamParamsMediaPlayerHttp_isS0Increasing, "m_isS0Increasing", "Z");
    GET_FIELD_ID(HNStreamParamsMediaPlayerHttp_isSnIncreasing, "m_isSnIncreasing", "Z");
    GET_FIELD_ID(HNStreamParamsMediaPlayerHttp_isStreamingMode, "m_isStreamingMode", "Z");
    GET_FIELD_ID(HNStreamParamsMediaPlayerHttp_isInteractiveMode, "m_isInteractiveMode", "Z");
    GET_FIELD_ID(HNStreamParamsMediaPlayerHttp_isBackgroundMode, "m_isBackgroundMode", "Z");
    GET_FIELD_ID(HNStreamParamsMediaPlayerHttp_isHTTPStallingSupported, "m_isHTTPStallingSupported", "Z");
    GET_FIELD_ID(HNStreamParamsMediaPlayerHttp_isDLNAV15, "m_isDLNAV15", "Z");
    GET_FIELD_ID(HNStreamParamsMediaPlayerHttp_isLinkProtected, "m_isLinkProtected", "Z");
    GET_FIELD_ID(HNStreamParamsMediaPlayerHttp_isFullClearByteSeek, "m_isFullClearByteSeek", "Z");
    GET_FIELD_ID(HNStreamParamsMediaPlayerHttp_isLimitedClearByteSeek, "m_isLimitedClearByteSeek", "Z");
    GET_FIELD_ID(HNStreamParamsMediaPlayerHttp_playspeedsCnt, "m_playspeedsCnt", "I");
    GET_FIELD_ID(HNStreamParamsMediaPlayerHttp_playspeeds, "m_playspeeds", "[F");
    */

    GET_CLASS(HNPlaybackParams,
            "org/cablelabs/impl/media/streaming/session/data/HNPlaybackParams");
    GET_METHOD_ID(HNPlaybackParams_getPlaybackType, "getPlaybackType", "()I");

    GET_CLASS(HNPlaybackParamsMediaServerHttp,
            "org/cablelabs/impl/media/streaming/session/data/HNPlaybackParamsMediaServerHttp");
    GET_FIELD_ID(HNPlaybackParamsMediaServerHttp_contentLocation,
            "m_contentLocation","I");
    GET_FIELD_ID(HNPlaybackParamsMediaServerHttp_contentDescription,
            "m_contentDescription",
            "Lorg/cablelabs/impl/media/streaming/session/data/HNStreamContentDescription;");
    GET_FIELD_ID(HNPlaybackParamsMediaServerHttp_playspeedRate, "m_playspeedRate", "F");
    GET_FIELD_ID(HNPlaybackParamsMediaServerHttp_useTimeOffset, "m_useTimeOffsetValues", "Z");
    GET_FIELD_ID(HNPlaybackParamsMediaServerHttp_startBytePosition, "m_startBytePosition", "J");
    GET_FIELD_ID(HNPlaybackParamsMediaServerHttp_endBytePosition, "m_endBytePosition", "J");
    GET_FIELD_ID(HNPlaybackParamsMediaServerHttp_startTimePosition, "m_startTimePosition", "J");
    GET_FIELD_ID(HNPlaybackParamsMediaServerHttp_endTimePosition, "m_endTimePosition", "J");
    GET_FIELD_ID(HNPlaybackParamsMediaServerHttp_cciDescriptors, "m_cciDescriptors",
            "[Lorg/cablelabs/impl/media/streaming/session/data/HNPlaybackCopyControlInfo;");
    GET_FIELD_ID(HNPlaybackParamsMediaServerHttp_transformation, "m_transformation",
            "Lorg/cablelabs/impl/ocap/hn/transformation/NativeContentTransformation;");

    GET_CLASS(HNPlaybackParamsMediaPlayerHttp,
            "org/cablelabs/impl/media/streaming/session/data/HNPlaybackParamsMediaPlayerHttp");
    GET_FIELD_ID(HNPlaybackParamsMediaPlayerHttp_avStreamParameters, "m_avStreamParameters",
            "Lorg/cablelabs/impl/media/streaming/session/data/HNHttpHeaderAVStreamParameters;");
    GET_FIELD_ID(HNPlaybackParamsMediaPlayerHttp_videoDevice, "m_videoDevice", "I");
    GET_FIELD_ID(HNPlaybackParamsMediaPlayerHttp_initialBlockingState, "m_initialBlockingState", "Z");
    GET_FIELD_ID(HNPlaybackParamsMediaPlayerHttp_muted, "m_muted", "Z");
    GET_FIELD_ID(HNPlaybackParamsMediaPlayerHttp_requestedGain, "m_requestedGain", "F");
    GET_FIELD_ID(HNPlaybackParamsMediaPlayerHttp_requestedRate, "m_requestedRate", "F");
    GET_FIELD_ID(HNPlaybackParamsMediaPlayerHttp_initialMediaTimeNS, "m_initialMediaTimeNS", "J");
    GET_FIELD_ID(HNPlaybackParamsMediaPlayerHttp_cciDescriptors, "m_cciDescriptors",
            "[Lorg/cablelabs/impl/media/streaming/session/data/HNPlaybackCopyControlInfo;");

    GET_CLASS(HNPlaybackCopyControlInfo,
            "org/cablelabs/impl/media/streaming/session/data/HNPlaybackCopyControlInfo");
    GET_FIELD_ID(HNPlaybackCopyControlInfo_pid, "m_pid", "S");
    GET_FIELD_ID(HNPlaybackCopyControlInfo_isProgram, "m_isProgram", "Z");
    GET_FIELD_ID(HNPlaybackCopyControlInfo_isAudio, "m_isAudio", "Z");
    GET_FIELD_ID(HNPlaybackCopyControlInfo_cci, "m_cci", "B");

    GET_CLASS(HNStreamContentDescription,
            "org/cablelabs/impl/media/streaming/session/data/HNStreamContentDescription");

    GET_CLASS(HNStreamContentDescriptionLocalSV,
            "org/cablelabs/impl/media/streaming/session/data/HNStreamContentDescriptionLocalSV");
    GET_FIELD_ID(HNStreamContentDescriptionLocalSV_contentName, "m_contentName", "Ljava/lang/String;");
    GET_FIELD_ID(HNStreamContentDescriptionLocalSV_volumeHandle, "m_volumeHandle", "I");

    GET_CLASS(HNStreamContentDescriptionApp,
            "org/cablelabs/impl/media/streaming/session/data/HNStreamContentDescriptionApp");
    GET_FIELD_ID(HNStreamContentDescriptionApp_contentName, "m_contentName", "Ljava/lang/String;");
    GET_FIELD_ID(HNStreamContentDescriptionApp_contentPath, "m_contentPath", "Ljava/lang/String;");

    GET_CLASS(HNStreamContentDescriptionVideoDevice,
            "org/cablelabs/impl/media/streaming/session/data/HNStreamContentDescriptionVideoDevice");
    GET_FIELD_ID(HNStreamContentDescriptionVideoDevice_nativeVideoDeviceHandle, "m_nativeVideoDeviceHandle", "I");

    GET_CLASS(HNStreamContentDescriptionTuner,
            "org/cablelabs/impl/media/streaming/session/data/HNStreamContentDescriptionTuner");
    GET_FIELD_ID(HNStreamContentDescriptionTuner_tunerId, "m_tunerId", "I");
    GET_FIELD_ID(HNStreamContentDescriptionTuner_frequency, "m_frequency", "I");
    GET_FIELD_ID(HNStreamContentDescriptionTuner_ltsid, "m_ltsid", "S");
    GET_FIELD_ID(HNStreamContentDescriptionTuner_pids, "m_Pids", "[I");
    GET_FIELD_ID(HNStreamContentDescriptionTuner_elemStreamTypes, "m_elemStreamTypes", "[S");
    GET_FIELD_ID(HNStreamContentDescriptionTuner_mediaStreamTypes, "m_mediaStreamTypes", "[S");

    GET_CLASS(HNHttpHeaderAVStreamParameters,
            "org/cablelabs/impl/media/streaming/session/data/HNHttpHeaderAVStreamParameters");
    GET_FIELD_ID(HNHttpHeaderAVStreamParameters_videoType, "m_videoType", "I");
    GET_FIELD_ID(HNHttpHeaderAVStreamParameters_videoPID, "m_videoPID", "I");
    GET_FIELD_ID(HNHttpHeaderAVStreamParameters_audioType, "m_audioType", "I");
    GET_FIELD_ID(HNHttpHeaderAVStreamParameters_audioPID, "m_audioPID", "I");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "HN jniInit - Exit\n");
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl_jniInitDVR
 * Method:    jniInitDVR
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_jniInitDVR
  (JNIEnv *env, jclass cls)
{
    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "HN jniInitDVR - Enter\n");

    GET_CLASS(HNStreamContentDescriptionTSB,
            "org/cablelabs/impl/media/streaming/session/data/HNStreamContentDescriptionTSB");
    GET_FIELD_ID(HNStreamContentDescriptionTSB_nativeTSBHandle, "m_nativeTSBHandle", "I");

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "HN jniInitDVR - Exit\n");
}



/*****************************************************************************/
/***                                                                       ***/
/***                         Shared player/server                          ***/
/***                                                                       ***/
/*****************************************************************************/

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativeStreamClose
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativeStreamClose
        (JNIEnv *env, jclass cls, jint nativeStreamSession)
{
    mpe_Error err = MPE_SUCCESS;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() enter\n", __FUNCTION__);

    err = mpe_hnStreamClose((mpe_HnStreamSession) nativeStreamSession);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnStreamClose(0x%X) returned %d\n",
                __FUNCTION__, nativeStreamSession, err);
        throwMPEMediaError(env, err, "mpe_hnStreamClose()");
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() exit\n", __FUNCTION__);
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativeStreamOpen
 * Signature: (Lorg/cablelabs/impl/manager/ed/EDListener;Lorg/cablelabs/impl/media/streaming/session/data/HNStreamParams;)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativeStreamOpen
  (JNIEnv *env, jclass cls, jobject edListener, jobject streamParams)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_HnStreamParams *mpeStreamParams = 0;
    mpe_EdEventInfo *edEventInfo = 0;
    mpe_HnStreamSession nativeStreamSession = (mpe_HnStreamSession) -1;

    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() enter\n", __FUNCTION__);

    if ((err = buildStreamParamsFromObject(env, streamParams, &mpeStreamParams)) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to build "
                "mpe_HnStreamParams with error %d\n", __FUNCTION__, err);
        throwMPEMediaError(env, err, "buildStreamParamsFromObject()");
    }
    else if ((err = mpe_edCreateHandle(edListener, MPE_ED_QUEUE_NORMAL, NULL,
            MPE_ED_TERMINATION_EVCODE, MPE_HN_EVT_SESSION_CLOSED, &edEventInfo)) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to create "
                "mpe_EdEventInfo with error %d\n", __FUNCTION__, err);
        deallocateStreamParams(mpeStreamParams);
        throwMPEMediaError(env, err, "mpe_edCreateHandle()");
    }
    else if ((err = mpe_hnStreamOpen(mpeStreamParams, edEventInfo->eventQ,
            (void *) edEventInfo, &nativeStreamSession)) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to open "
                "HN streaming session - %d\n", __FUNCTION__, err);
        mpe_edDeleteHandle(edEventInfo);
        deallocateStreamParams(mpeStreamParams);
        throwMPEMediaError(env, err, "mpe_hnStreamOpen()");
    }
    else
    {
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() mpe_hnStreamOpen succeeded; "
                "nativeStreamSession = 0x%X\n", __FUNCTION__, nativeStreamSession);
        deallocateStreamParams(mpeStreamParams);
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() exit\n", __FUNCTION__);

    return (jint) nativeStreamSession;
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativePlaybackStart
 * Signature: (ILorg/cablelabs/impl/media/streaming/session/data/HNPlaybackParams;FZZF)Lorg/cablelabs/impl/media/mpe/HNAPI$Playback;
 */
JNIEXPORT jobject JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativePlaybackStart
  (JNIEnv *env, jclass cls, jint nativeStreamSession, jobject playbackParams,
   jfloat rate, jboolean blocked, jboolean mute, jfloat gain)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_HnPlaybackParams *mpePlaybackParams = NULL;
    mpe_HnPlaybackSession nativePlaybackSession = (mpe_HnPlaybackSession) -1;
    jobject playbackObject = (jobject) 0;

    MPE_UNUSED_PARAM(cls);

    MPE_UNUSED_PARAM(rate);
    MPE_UNUSED_PARAM(blocked);
    MPE_UNUSED_PARAM(mute);
    MPE_UNUSED_PARAM(gain);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() enter\n", __FUNCTION__);

    MPE_LOG(MPE_LOG_WARN, MPE_MOD_JNI, "%s() unused paramter rate is %f\n", __FUNCTION__, rate);
    MPE_LOG(MPE_LOG_WARN, MPE_MOD_JNI, "%s() unused paramter blocked is %u\n", __FUNCTION__, blocked);
    MPE_LOG(MPE_LOG_WARN, MPE_MOD_JNI, "%s() unused paramter mute is %u\n", __FUNCTION__, mute);
    MPE_LOG(MPE_LOG_WARN, MPE_MOD_JNI, "%s() unused paramter gain is %f\n", __FUNCTION__, gain);

    if ((err = buildPlaybackParamsFromObject(env, playbackParams, &mpePlaybackParams)) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to build "
                "mpe_HnPlaybackParams with error %d\n", __FUNCTION__, err);
        throwMPEMediaError(env, err, "buildPlaybackParamsFromObject()");
    }
    else if ((err = mpe_hnPlaybackStart((mpe_HnStreamSession) nativeStreamSession,
            mpePlaybackParams, NULL, &nativePlaybackSession)) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to open "
                "HN playback session - %d\n", __FUNCTION__, err);
        deallocatePlaybackParams(mpePlaybackParams);
        throwMPEMediaError(env, err, "mpe_hnPlaybackStart()");
    }
    else
    {
        float gain = NON_SPECIFIED_FLOAT;
        float rate = NON_SPECIFIED_FLOAT;

        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() mpe_hnPlaybackStart succeeded for "
                "nativeStreamSession 0x%X; nativePlaybackSession = 0x%X\n",
                __FUNCTION__, nativeStreamSession, nativePlaybackSession);

        if (mpePlaybackParams->playbackType == MPE_HNPLAYBACK_MEDIA_PLAYER_HTTP)
        {
            mpe_HnPlaybackParamsMediaPlayerHttp *playerPlaybackParams =
                (mpe_HnPlaybackParamsMediaPlayerHttp *) mpePlaybackParams->playbackParams;
            gain = playerPlaybackParams->requestedGain;
            rate = playerPlaybackParams->requestedRate;
        }

        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() HNAPI$Playback(0x%X, %f, %f)\n",
                __FUNCTION__, nativePlaybackSession, gain, rate);
        playbackObject = (*env)->NewObject(env, jniutil_CachedIds.HNAPI_Playback,
                jniutil_CachedIds.HNAPI_Playback_init, nativePlaybackSession, rate, gain);
        deallocatePlaybackParams(mpePlaybackParams);
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() exit\n", __FUNCTION__);

    return playbackObject;
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativePlaybackStop
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativePlaybackStop
  (JNIEnv *env, jclass cls, jint nativePlaybackSession, jint holdFrameMode)
{
    mpe_Error err = MPE_SUCCESS;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() enter\n", __FUNCTION__);

    err = mpe_hnPlaybackStop((mpe_HnPlaybackSession) nativePlaybackSession,
                             (mpe_MediaHoldFrameMode) holdFrameMode);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnPlaybackStop(0x%X) returned %d\n",
                __FUNCTION__, nativePlaybackSession, err);
        throwMPEMediaError(env, err, "mpe_hnPlaybackStop()");
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() exit\n", __FUNCTION__);
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativeGetMacAddress
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativeGetMacAddress
  (JNIEnv *env, jclass cls, jstring displayName)
{
    const char *displayNameSz = NULL;
    jstring jMacAddress = (jstring) NULL;

    MPE_UNUSED_PARAM(cls);

    /* Allocate display name string */
    if (displayName != NULL && (displayNameSz = (*env)->GetStringUTFChars(env, displayName, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() - GetStringUTFChars(displayName) "
                "returned NULL\n", __FUNCTION__);
    }
    else
    {
        char buffer[MPE_HN_MAX_MAC_ADDRESS_STR_SIZE];

        (void) memset((void *) buffer, 0, MPE_HN_MAX_MAC_ADDRESS_STR_SIZE);
        mpe_Error retCode = mpe_hnGetMacAddress((char *) displayNameSz, buffer);
        if (retCode == MPE_HN_ERR_NOERR && strlen(buffer) > 0)
        {
            jMacAddress = (*env)->NewStringUTF(env, buffer);
        }
        else
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() unable to retrieve mac address for "
                " network interface %s\n", __FUNCTION__, displayNameSz);
            throwMPEMediaError(env, retCode, "mpe_hnGetMacAddress()");
        }

        if (displayName != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, displayName, displayNameSz);
        }
    }

    return jMacAddress;
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativeGetNetworkInterfaceType
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativeGetNetworkInterfaceType
  (JNIEnv *env, jclass cls, jstring displayName)
{
    const char *displayNameSz = NULL;
    int32_t hnType = -1;

    MPE_UNUSED_PARAM(cls);

    /* Allocate display name string */
    if (displayName != NULL && (displayNameSz = (*env)->GetStringUTFChars(env, displayName, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() - GetStringUTFChars(displayName) "
                "returned NULL\n", __FUNCTION__);
    }
    else
    {
        mpe_Error retCode = mpe_hnGetNetworkInterfaceType((char *) displayNameSz, &hnType);
        if (retCode != MPE_HN_ERR_NOERR)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() - unable to retrieve network "
                    "interface type for %s\n", __FUNCTION__, displayNameSz);
            throwMPEMediaError(env, retCode, "mpe_hnGetNetworkInterfaceType()");
        }

        if (displayName != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, displayName, displayNameSz);
        }
    }

    return hnType;
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativePing
 * Signature: (Ljava/lang/String;)(I)V
 */
JNIEXPORT jobjectArray JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativePing
(JNIEnv *env, jclass cls, jint testID, jstring host, jint reps, jint interval, jint blocksize, jint dscp)
{
    const char *hostSz = NULL;
    jobjectArray retObject = (jobjectArray) NULL;

    MPE_UNUSED_PARAM(cls);

    /* Allocate host string */
    if (host != NULL && (hostSz = (*env)->GetStringUTFChars(env, host, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() - GetStringUTFChars(host) "
                "returned NULL\n", __FUNCTION__);
    }
    else
    {
        retObject = (*env)->NewObjectArray(env, MPE_SOCKET_MAX_PING_RESULT_SIZE,
            jniutil_CachedIds.String, NULL);
        if (retObject != NULL)
        {
            int success = 0; 
            int fails = 0; 
            int min = 0; 
            int max = 0; 
            int avg = 0;
            char info[MPE_SOCKET_ADDITIONAL_INFO_SIZE]="";
            char status[MPE_SOCKET_STATUS_SIZE]="";
            char tmp[32] = "";

            mpe_Error err = mpe_hnPing(testID, (char *)hostSz, reps, interval,MPE_SOCKET_PING_DEFAULT_TIMEOUT,
                blocksize, dscp, status, info, &success, &fails, &avg, &min, &max);
 
            if (err != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnPing "
                        "returned %d\n", __FUNCTION__, err);
                throwMPEMediaError(env, err, "mpe_hnPing()");
            }
            else
            {
                // Set status
                jstring arrayElem = (*env)->NewStringUTF(env, status );
                if (arrayElem == NULL)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, " NewStringUTF(%s) returned NULL\n",
                        __FUNCTION__ );
                }
                else
                {
                    (*env)->SetObjectArrayElement(env, retObject, 0, arrayElem);
                }
                // set addition info field
                arrayElem = (*env)->NewStringUTF(env, info);
                if (arrayElem == NULL)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, " NewStringUTF(%s) returned NULL\n",
                        __FUNCTION__ );
                }
                else
                {
                    (*env)->SetObjectArrayElement(env, retObject, 1, arrayElem);
                }
                // set number of successes
                sprintf(tmp,"%d", success); 
                arrayElem = (*env)->NewStringUTF(env, tmp);
                if (arrayElem == NULL)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, " NewStringUTF(%s) returned NULL\n",
                        __FUNCTION__ );
                }
                else
                {
                    (*env)->SetObjectArrayElement(env, retObject, 2, arrayElem);
                }
                // set number of fails 
                memset(&tmp,0,sizeof(tmp));
                sprintf(tmp,"%d", fails); 
                arrayElem = (*env)->NewStringUTF(env, tmp);
                if (arrayElem == NULL)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, " NewStringUTF(%s) returned NULL\n",
                        __FUNCTION__ );
                }
                else
                {
                    (*env)->SetObjectArrayElement(env, retObject, 3, arrayElem);
                }
                // set avg 
                memset(&tmp,0,sizeof(tmp));
                sprintf(tmp,"%d", avg); 
                arrayElem = (*env)->NewStringUTF(env, tmp);
                if (arrayElem == NULL)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, " NewStringUTF(%s) returned NULL\n",
                        __FUNCTION__ );
                }
                else
                {
                    (*env)->SetObjectArrayElement(env, retObject, 4, arrayElem);
                }
                // set min
                memset(&tmp,0,sizeof(tmp));
                sprintf(tmp,"%d", min); 
                arrayElem = (*env)->NewStringUTF(env, tmp);
                if (arrayElem == NULL)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, " NewStringUTF(%s) returned NULL\n",
                        __FUNCTION__ );
                }
                else
                {
                    (*env)->SetObjectArrayElement(env, retObject, 5, arrayElem);
                }
                // set max 
                memset(&tmp,0,sizeof(tmp));
                sprintf(tmp,"%d", max); 
                arrayElem = (*env)->NewStringUTF(env, tmp);
                if (arrayElem == NULL)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, " NewStringUTF(%s) returned NULL\n",
                        __FUNCTION__ );
                }
                else
                {
                    (*env)->SetObjectArrayElement(env, retObject, 6, arrayElem);
                }
            }
        } 
        else
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() NewObjectArray(%u) returned NULL\n",
                    __FUNCTION__, MPE_SOCKET_MAX_PING_RESULT_SIZE);
        }

    }

    return retObject;
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativeTraceroute
 * Signature: (Ljava/lang/String;)(I)V
 */
JNIEXPORT jobjectArray JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativeTraceroute(JNIEnv *env, jclass cls, jint testID, jstring host, jint hops, jint timeout, jint blocksize, jint dscp)
{
    const char *hostSz = NULL;
    jobjectArray retObject = (jobjectArray) NULL;

    MPE_UNUSED_PARAM(cls);

    /* Allocate host string */
    if (host != NULL && (hostSz = (*env)->GetStringUTFChars(env, host, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() - GetStringUTFChars(host) "
                "returned NULL\n", __FUNCTION__);
    }
    else
    {
        retObject = (*env)->NewObjectArray(env, MPE_SOCKET_TRACEROUTE_RESULT_SIZE,
            jniutil_CachedIds.String, NULL);
        if (retObject != NULL)
        {
            int avgresp = 0; 
            char info[MPE_SOCKET_ADDITIONAL_INFO_SIZE]="";
            char status[MPE_SOCKET_STATUS_SIZE]="";
            char hopHosts[MPE_SOCKET_MAX_TRACEROUTE_HOSTS]="";
            char tmp[32] = "";

            mpe_Error err = mpe_hnTraceroute(testID, (char *)hostSz, hops, timeout,
                blocksize, dscp, status, info, &avgresp, hopHosts);
            if (err != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnTraceroute "
                        "returned %d\n", __FUNCTION__, err);
                throwMPEMediaError(env, err, "mpe_hnTraceroute()");
            }
            else
            {
                // Set status
                jstring arrayElem = (*env)->NewStringUTF(env, status);
                if (arrayElem == NULL)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                        " NewStringUTF(%s) returned NULL\n", __FUNCTION__ );
                }
                else
                {
                    (*env)->SetObjectArrayElement(env, retObject, 0, arrayElem);
                }
                // set addition info field
                arrayElem = (*env)->NewStringUTF(env, info );
                if (arrayElem == NULL)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, " NewStringUTF(%s) returned NULL\n",
                        __FUNCTION__ );
                }
                else
                {
                    (*env)->SetObjectArrayElement(env, retObject, 1, arrayElem);
                }
                // set average response time 
                sprintf(tmp,"%d", avgresp); 
                arrayElem = (*env)->NewStringUTF(env, tmp);
                if (arrayElem == NULL)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, " NewStringUTF(%s) returned NULL\n",
                        __FUNCTION__ );
                }
                else
                {
                    (*env)->SetObjectArrayElement(env, retObject, 2, arrayElem);
                }
                // set hopHosts 
                arrayElem = (*env)->NewStringUTF(env, hopHosts);
                if (arrayElem == NULL)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, " NewStringUTF(%s) returned NULL\n",
                        __FUNCTION__ );
                }
                else
                {
                    (*env)->SetObjectArrayElement(env, retObject, 3, arrayElem);
                }
            }
        } 
        else
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() NewObjectArray(%u) returned NULL\n",
                    __FUNCTION__, MPE_SOCKET_TRACEROUTE_RESULT_SIZE);
        }

    }

    return retObject;
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativeNSLookup
 * Signature: (Ljava/lang/String;)(I)V
 */
JNIEXPORT jobjectArray JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativeNSLookup(JNIEnv *env, jclass cls, jint testID, jstring host, jstring server, jint timeout)
{
    const char *hostSz = NULL;
    const char *serverSz = NULL;
    jobjectArray retObject = (jobjectArray) NULL;

    MPE_UNUSED_PARAM(cls);

    if (server != NULL &&
        (serverSz = (*env)->GetStringUTFChars(env, server, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() - GetStringUTFChars(host) "
                "returned NULL\n", __FUNCTION__);
        return retObject;
    }
    
    /* Allocate host string */
    if (host != NULL && (hostSz = (*env)->GetStringUTFChars(env, host, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() - GetStringUTFChars(host) "
                "returned NULL\n", __FUNCTION__);
    }
    else
    {
        retObject = (*env)->NewObjectArray(env, MPE_SOCKET_MAX_NSLOOKUP_RESULT_ARRAY_SIZE,
            jniutil_CachedIds.String, NULL);
        if (retObject != NULL)
        {
            char resultAnswer[MPE_SOCKET_MAX_NSLOOKUP_ANSWER_RESULT_SIZE] = "";
            char resultName[MPE_SOCKET_MAX_NSLOOKUP_NAME_RESULT_SIZE] = "";
            char resultIPS[MPE_SOCKET_MAX_NSLOOKUP_IPS_RESULT_SIZE] = "";
            char resultServer[MPE_SOCKET_MAX_NSLOOKUP_SERVER_RESULT_SIZE] = "";
            int resultTime = 0;
            char info[MPE_SOCKET_ADDITIONAL_INFO_SIZE]="";
            char status[MPE_SOCKET_STATUS_SIZE]="";
            char tmp[32] = "";

            mpe_Error err = mpe_hnNSLookup(testID, (char *)hostSz, (char *)serverSz,
                timeout, status, info, resultAnswer, resultName, resultIPS, resultServer,
                &resultTime); 
            if (err != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnNSLookup"
                        "returned %d\n", __FUNCTION__, err);
                throwMPEMediaError(env, err, "mpe_hnNSLookup()");
            }
            else
            {
                // Set status
                jstring arrayElem = (*env)->NewStringUTF(env, status);
                if (arrayElem == NULL)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, " NewStringUTF(%s) returned NULL\n",
                        __FUNCTION__ );
                }
                else
                {
                    (*env)->SetObjectArrayElement(env, retObject, 0, arrayElem);
                }
                // set addition info field
                arrayElem = (*env)->NewStringUTF(env, info );
                if (arrayElem == NULL)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, " NewStringUTF(%s) returned NULL\n",
                        __FUNCTION__ );
                }
                else
                {
                    (*env)->SetObjectArrayElement(env, retObject, 1, arrayElem);
                }
                // set resultAnswer 
                arrayElem = (*env)->NewStringUTF(env, resultAnswer);
                if (arrayElem == NULL)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, " NewStringUTF(%s) returned NULL\n",
                        __FUNCTION__ );
                }
                else
                {
                    (*env)->SetObjectArrayElement(env, retObject, 2, arrayElem);
                }
                // set resultName 
                arrayElem = (*env)->NewStringUTF(env, resultName);
                if (arrayElem == NULL)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, " NewStringUTF(%s) returned NULL\n",
                        __FUNCTION__ );
                }
                else
                {
                    (*env)->SetObjectArrayElement(env, retObject, 3, arrayElem);
                }
                // set resultIPS
                arrayElem = (*env)->NewStringUTF(env, resultIPS);
                if (arrayElem == NULL)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, " NewStringUTF(%s) returned NULL\n",
                        __FUNCTION__ );
                }
                else
                {
                    (*env)->SetObjectArrayElement(env, retObject, 4, arrayElem);
                }
                // set resultServer
                arrayElem = (*env)->NewStringUTF(env, resultServer);
                if (arrayElem == NULL)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, " NewStringUTF(%s) returned NULL\n",
                        __FUNCTION__ );
                }
                else
                {
                    (*env)->SetObjectArrayElement(env, retObject, 5, arrayElem);
                }
                // set resultTime
                sprintf(tmp,"%d", resultTime); 
                arrayElem = (*env)->NewStringUTF(env, tmp);
                if (arrayElem == NULL)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, " NewStringUTF(%s) returned NULL\n",
                        __FUNCTION__ );
                }
                else
                {
                    (*env)->SetObjectArrayElement(env, retObject, 6, arrayElem);
                }
            }
        } 
        else
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() NewObjectArray(%u) returned NULL\n",
                    __FUNCTION__, MPE_SOCKET_MAX_NSLOOKUP_RESULT_ARRAY_SIZE);
        }

    }

    return retObject;
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativeCancelTest
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativeCancelTest
  (JNIEnv *env, jclass cls, jint testID)
{


    MPE_UNUSED_PARAM(cls);
    mpe_hnCancelTest(testID);
}


/*****************************************************************************/
/***                                                                       ***/
/***                              Server only                              ***/
/***                                                                       ***/
/*****************************************************************************/

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativeServerUpdateEndPosition
 * Signature: (IJ)J
 */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativeServerUpdateEndPosition
  (JNIEnv *env, jclass cls, jint nativePlaybackSession, jlong endBytePosition)
{
    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);
    return mpe_hnServerUpdateEndPosition((mpe_HnPlaybackSession) nativePlaybackSession, (int64_t)endBytePosition);
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativeServerGetNetworkContentItemSize
 * Signature: (ILorg/cablelabs/impl/media/streaming/session/data/HNStreamContentDescription;Ljava/lang/String;Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativeServerGetNetworkContentItemSize
  (JNIEnv *env, jclass cls, jint contentLocationType, jobject contentDescription,
   jstring profileId, jstring mimeType, jobject jTransformation)
{
    const char *profileIdSz = NULL;
    const char *mimeTypeSz = NULL;
    int64_t size = -1;

    MPE_UNUSED_PARAM(cls);

    /* Allocate string */
    if (profileId != NULL && (profileIdSz = (*env)->GetStringUTFChars(env, profileId, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() GetStringUTFChars(profileId) "
                "returned NULL\n", __FUNCTION__);
    }
    else if (mimeType != NULL && (mimeTypeSz = (*env)->GetStringUTFChars(env, mimeType, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() GetStringUTFChars(mimeType) "
                "returned NULL\n", __FUNCTION__);
        if (profileId != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, profileId, profileIdSz);
        }
    }
    else
    {
        void *mpeContentDescription = NULL;
        mpe_Error err = buildContentDescriptionFromObject(env,
                contentLocationType, contentDescription, &mpeContentDescription);
        if (err != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() buildContentDescriptionFromObject() "
                    "returned %d\n", __FUNCTION__, err);
            throwMPEMediaError(env, err, "buildContentDescriptionFromObject()");
        }
        else
        {
            mpe_HnStreamContentLocation mpeContentLocation =
                    (mpe_HnStreamContentLocation) contentLocationType;

            err = buildContentDescriptionFromObject(env,
                    contentLocationType, contentDescription, &mpeContentDescription);

            mpe_hnContentTransformation *mpeContentTransformation = NULL;
            {
                //
                // Transformation
                //
                if ((err = buildContentTransformationFromObject(env, jTransformation, &mpeContentTransformation)) != MPE_SUCCESS)
                {
                    throwMPEMediaError(env, err, "buildContentTransformationFromObject() failed.");
                }
                if (mpeContentTransformation != NULL)
                {
                    mpe_hnContentTransformation * transformation = mpeContentTransformation;
                    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() transformation: id %d, source %s, dest %s, width %d, height %d, bitrate %d, progressive %d\n",
                            __FUNCTION__, transformation->id, transformation->sourceProfile,
                            transformation->transformedProfile, transformation->width,
                            transformation->height, transformation->bitrate, transformation->progressive );
                }
            }
            err = mpe_hnServerGetNetworkContentItemSize(mpeContentLocation, mpeContentDescription,
                    (char *) profileIdSz, (char *) mimeTypeSz, mpeContentTransformation, &size);
            if (err != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnServerGetNetworkContentItemSize() "
                        "returned %d\n", __FUNCTION__, err);
                throwMPEMediaError(env, err, "mpe_hnServerGetNetworkContentItemSize()");
            }
            deallocateContentDescription(mpeContentLocation, mpeContentDescription);
            deallocateContentTransformation(mpeContentTransformation);
        }

        if (profileId != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, profileId, profileIdSz);
        }
        if (mimeType != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, mimeType, mimeTypeSz);
        }
    }

    return (jlong) size;
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativeServerGetNetworkBytePosition
 * Signature: (ILorg/cablelabs/impl/media/streaming/session/data/HNStreamContentDescription;Ljava/lang/String;Ljava/lang/String;J)J
 */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativeServerGetNetworkBytePosition
  (JNIEnv *env, jclass cls, jint contentLocationType, jobject contentDescription,
   jstring profileId, jstring mimeType, jobject jTransformation, jlong localBytePosition)
{
    const char *profileIdSz = NULL;
    const char *mimeTypeSz = NULL;
    int64_t networkBytePosition = -1;

    MPE_UNUSED_PARAM(cls);

    /* Allocate string */
    if (profileId != NULL && (profileIdSz = (*env)->GetStringUTFChars(env, profileId, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() GetStringUTFChars(profileId) "
                "returned NULL\n", __FUNCTION__);
    }
    else if (mimeType != NULL && (mimeTypeSz = (*env)->GetStringUTFChars(env, mimeType, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() GetStringUTFChars(mimeType) "
                "returned NULL\n", __FUNCTION__);
        if (profileId != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, profileId, profileIdSz);
        }
    }
    else
    {
        void *mpeContentDescription = NULL;
        mpe_Error err = buildContentDescriptionFromObject(env,
                contentLocationType, contentDescription, &mpeContentDescription);
        if (err != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() buildContentDescriptionFromObject() "
                    "returned %d\n", __FUNCTION__, err);
            throwMPEMediaError(env, err, "buildContentDescriptionFromObject()");
        }
        else
        {
            mpe_HnStreamContentLocation mpeContentLocation =
                    (mpe_HnStreamContentLocation) contentLocationType;

            mpe_hnContentTransformation *mpeContentTransformation = NULL;
            {
                //
                // Transformation
                //
                if ((err = buildContentTransformationFromObject(env, jTransformation, &mpeContentTransformation)) != MPE_SUCCESS)
                {
                    throwMPEMediaError(env, err, "buildContentTransformationFromObject() failed.");
                }
                if (mpeContentTransformation != NULL)
                {
                    mpe_hnContentTransformation * transformation = mpeContentTransformation;
                    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() transformation: id %d, source %s, dest %s, width %d, height %d, bitrate %d, progressive %d\n",
                            __FUNCTION__, transformation->id, transformation->sourceProfile,
                            transformation->transformedProfile, transformation->width,
                            transformation->height, transformation->bitrate, transformation->progressive );
                }
            }
            err = mpe_hnServerGetNetworkBytePosition(mpeContentLocation, mpeContentDescription,
                    (char *) profileIdSz, (char *) mimeTypeSz, mpeContentTransformation, (int64_t) localBytePosition,
                    &networkBytePosition);
            if (err != MPE_SUCCESS)
            {

                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnServerGetNetworkBytePosition() "
                        "returned %d\n", __FUNCTION__, err);
                throwMPEMediaError(env, err, "mpe_hnServerGetNetworkBytePosition()");
            }
            deallocateContentDescription(mpeContentLocation, mpeContentDescription);
            deallocateContentTransformation(mpeContentTransformation);
        }

        if (profileId != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, profileId, profileIdSz);
        }
        if (mimeType != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, mimeType, mimeTypeSz);
        }
    }

    return (jlong) networkBytePosition;
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativeServerGetNetworkBytePositionForMediaTimeNS
 * Signature: (ILorg/cablelabs/impl/media/streaming/session/data/HNStreamContentDescription;Ljava/lang/String;Ljava/lang/String;J)J
 */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativeServerGetNetworkBytePositionForMediaTimeNS
  (JNIEnv *env, jclass cls, jint contentLocationType, jobject contentDescription,
   jstring profileId, jstring mimeType, jobject jTransformation, jlong mediaTimeNS)
{
    const char *profileIdSz = NULL;
    const char *mimeTypeSz = NULL;
    int64_t bytePos = -1;

    MPE_UNUSED_PARAM(cls);

    /* Allocate string */
    if (profileId != NULL && (profileIdSz = (*env)->GetStringUTFChars(env, profileId, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() GetStringUTFChars(profileId) "
                "returned NULL\n", __FUNCTION__);
    }
    else if (mimeType != NULL && (mimeTypeSz = (*env)->GetStringUTFChars(env, mimeType, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() GetStringUTFChars(mimeType) "
                "returned NULL\n", __FUNCTION__);
        if (profileId != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, profileId, profileIdSz);
        }
    }
    else
    {
        void *mpeContentDescription = NULL;
        mpe_Error err = buildContentDescriptionFromObject(env,
                contentLocationType, contentDescription, &mpeContentDescription);
        if (err != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() buildContentDescriptionFromObject() "
                    "returned %d\n", __FUNCTION__, err);
            throwMPEMediaError(env, err, "buildContentDescriptionFromObject()");
        }
        else
        {
            mpe_HnStreamContentLocation mpeContentLocation =
                    (mpe_HnStreamContentLocation) contentLocationType;

            mpe_hnContentTransformation *mpeContentTransformation = NULL;
            {
                //
                // Transformation
                //
                if ((err = buildContentTransformationFromObject(env, jTransformation, &mpeContentTransformation)) != MPE_SUCCESS)
                {
                    throwMPEMediaError(env, err, "buildContentTransformationFromObject() failed.");
                }
                if (mpeContentTransformation != NULL)
                {
                    mpe_hnContentTransformation * transformation = mpeContentTransformation;
                    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() transformation: id %d, source %s, dest %s, width %d, height %d, bitrate %d, progressive %d\n",
                            __FUNCTION__, transformation->id, transformation->sourceProfile,
                            transformation->transformedProfile, transformation->width,
                            transformation->height, transformation->bitrate, transformation->progressive );
                }
            }
            err = mpe_hnServerGetNetworkBytePositionForMediaTimeNS(mpeContentLocation, mpeContentDescription,
                    (char *) profileIdSz, (char *) mimeTypeSz, mpeContentTransformation, (int64_t) mediaTimeNS, &bytePos);
            if (err != MPE_SUCCESS)
            {

                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnServerGetNetworkBytePositionForMediaTimeNS() "
                        "returned %d\n", __FUNCTION__, err);
                throwMPEMediaError(env, err, "mpe_hnServerGetNetworkBytePositionForMediaTimeNS()");
            }
            deallocateContentDescription(mpeContentLocation, mpeContentDescription);
            deallocateContentTransformation(mpeContentTransformation);
        }

        if (profileId != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, profileId, profileIdSz);
        }
        if (mimeType != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, mimeType, mimeTypeSz);
        }
    }

    return (jlong) bytePos;
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativeServerGetDLNAProfileIds
 * Signature: (ILorg/cablelabs/impl/media/streaming/session/data/HNStreamContentDescription;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativeServerGetDLNAProfileIds
  (JNIEnv *env, jclass cls, jint contentLocationType, jobject contentDescription)
{
    mpe_Error err = MPE_SUCCESS;
    void *mpeContentDescription = NULL;
    jobjectArray retObject = (jobjectArray) NULL;

    MPE_UNUSED_PARAM(cls);

    err = buildContentDescriptionFromObject(env,
            contentLocationType, contentDescription, &mpeContentDescription);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() buildContentDescriptionFromObject() "
                "returned %d\n", __FUNCTION__, err);
        throwMPEMediaError(env, err, "buildContentDescriptionFromObject()");
    }
    else
    {
        uint32_t count = 0;
        mpe_HnStreamContentLocation mpeContentLocation =
                (mpe_HnStreamContentLocation) contentLocationType;
        err = mpe_hnServerGetDLNAProfileIDsCnt(mpeContentLocation, mpeContentDescription, &count);
        if (err != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnServerGetDLNAProfileIDsCnt() "
                    "returned %d\n", __FUNCTION__, err);
            throwMPEMediaError(env, err, "mpe_hnServerGetDLNAProfileIDsCnt()");
        }
        else
        {
            retObject = (*env)->NewObjectArray(env, count, jniutil_CachedIds.String, NULL);
            if (retObject != NULL)
            {
                int i = 0;
                char profileId[MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE];

                for (i = 0; i < count; i++)
                {
                    err = mpe_hnServerGetDLNAProfileIDStr(
                            mpeContentLocation, mpeContentDescription, i, profileId);
                    if (err != MPE_SUCCESS)
                    {
                        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnServerGetDLNAProfileIDStr() "
                                "returned %d\n", __FUNCTION__, err);
                        throwMPEMediaError(env, err, "mpe_hnServerGetDLNAProfileIDStr()");
                        break;
                    }
                    else
                    {
                        jstring arrayElem = (*env)->NewStringUTF(env, profileId);
                        if (arrayElem == NULL)
                        {
                            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() NewStringUTF(%s) returned NULL\n",
                                    __FUNCTION__, profileId);
                            break;
                        }
                        else
                        {
                            (*env)->SetObjectArrayElement(env, retObject, i, arrayElem);
                        }
                    }
                }
            }
            else
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() NewObjectArray(%u) returned NULL\n",
                        __FUNCTION__, count);
            }
        }
        deallocateContentDescription(mpeContentLocation, mpeContentDescription);
    }

    return retObject;
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativeServerGetMimeTypes
 * Signature: (ILorg/cablelabs/impl/media/streaming/session/data/HNStreamContentDescription;Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativeServerGetMimeTypes
  (JNIEnv *env, jclass cls, jint contentLocationType, jobject contentDescription, jstring profileId)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_HnStreamContentLocation mpeContentLocation = (mpe_HnStreamContentLocation) contentLocationType;
    void *mpeContentDescription = NULL;
    const char* nativeProfileId = NULL;
    jobjectArray retObject = (jobjectArray) NULL;

    MPE_UNUSED_PARAM(cls);

    err = buildContentDescriptionFromObject(env,
            contentLocationType, contentDescription, &mpeContentDescription);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() buildContentDescriptionFromObject() "
                "returned %d\n", __FUNCTION__, err);
        throwMPEMediaError(env, err, "buildContentDescriptionFromObject()");
    }
    else if (profileId != NULL && (nativeProfileId = (*env)->GetStringUTFChars(env, profileId, NULL)) == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() - GetStringUTFChars(profileId) "
                "returned NULL\n", __FUNCTION__);
        deallocateContentDescription(mpeContentLocation, mpeContentDescription);
    }
    else
    {
        uint32_t count = 0;
        err = mpe_hnServerGetMimeTypesCnt(mpeContentLocation, mpeContentDescription,
                (char *) nativeProfileId, &count);
        if (err != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnServerGetMimeTypesCnt() "
                    "returned %d\n", __FUNCTION__, err);
            throwMPEMediaError(env, err, "mpe_hnServerGetMimeTypesCnt()");
        }
        else
        {
            retObject = (*env)->NewObjectArray(env, count, jniutil_CachedIds.String, NULL);
            if (retObject != NULL)
            {
                int i = 0;
                char mimeType[MPE_HN_MAX_MIME_TYPE_STR_SIZE];

                for (i = 0; i < count; i++)
                {
                    err = mpe_hnServerGetMimeTypeStr(mpeContentLocation, mpeContentDescription,
                            (char *) nativeProfileId, i, mimeType);
                    if (err != MPE_SUCCESS)
                    {
                        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnServerGetMimeTypeStr(%s) "
                                "returned %d\n", __FUNCTION__, nativeProfileId, err);
                        throwMPEMediaError(env, err, "mpe_hnServerGetMimeTypeStr()");
                        break;
                    }
                    else
                    {
                        jstring arrayElem = (*env)->NewStringUTF(env, mimeType);
                        if (arrayElem == NULL)
                        {
                            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() NewStringUTF(%s) returned NULL\n",
                                    __FUNCTION__, mimeType);
                            break;
                        }
                        else
                        {
                            (*env)->SetObjectArrayElement(env, retObject, i, arrayElem);
                        }
                    }
                }
            }
            else
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() NewObjectArray(%u) returned NULL\n",
                        __FUNCTION__, count);
            }
        }

        if (profileId != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, profileId, nativeProfileId);
        }
        deallocateContentDescription(mpeContentLocation, mpeContentDescription);
    }

    return retObject;
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativeServerGetPlayspeeds
 * Signature: (ILorg/cablelabs/impl/media/streaming/session/data/HNStreamContentDescription;Ljava/lang/String;Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativeServerGetPlayspeeds
  (JNIEnv *env, jclass cls, jint contentLocationType, jobject contentDescription,
   jstring profileId, jstring mimeType, jobject jTransformation)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_HnStreamContentLocation mpeContentLocation = (mpe_HnStreamContentLocation) contentLocationType;
    void *mpeContentDescription = NULL;
    const char* nativeProfileId = NULL;
    const char* nativeMimeType = NULL;
    jobjectArray retObject = (jobjectArray) NULL;

    MPE_UNUSED_PARAM(cls);

    err = buildContentDescriptionFromObject(env,
            contentLocationType, contentDescription, &mpeContentDescription);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() buildContentDescriptionFromObject() "
                "returned %d\n", __FUNCTION__, err);
        throwMPEMediaError(env, err, "buildContentDescriptionFromObject()");
    }
    else if (profileId != NULL && (nativeProfileId = (*env)->GetStringUTFChars(env, profileId, NULL)) == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() - GetStringUTFChars(profileId) "
                "returned NULL\n", __FUNCTION__);
        deallocateContentDescription(mpeContentLocation, mpeContentDescription);
    }
    else if (mimeType != NULL && (nativeMimeType = (*env)->GetStringUTFChars(env, mimeType, NULL)) == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() - GetStringUTFChars(mimeType) "
                "returned NULL\n", __FUNCTION__);
        if (profileId != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, profileId, nativeProfileId);
        }
        deallocateContentDescription(mpeContentLocation, mpeContentDescription);
    }
    else
    {
        uint32_t count = 0;

        mpe_hnContentTransformation *mpeContentTransformation = NULL;
        {
            //
            // Transformation
            //
            if ((err = buildContentTransformationFromObject(env, jTransformation, &mpeContentTransformation)) != MPE_SUCCESS)
            {
                throwMPEMediaError(env, err, "buildContentTransformationFromObject() failed.");
            }
            if (mpeContentTransformation != NULL)
            {
                mpe_hnContentTransformation * transformation = mpeContentTransformation;
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() transformation: id %d, source %s, dest %s, width %d, height %d, bitrate %d, progressive %d\n",
                        __FUNCTION__, transformation->id, transformation->sourceProfile,
                        transformation->transformedProfile, transformation->width,
                        transformation->height, transformation->bitrate, transformation->progressive );
            }
        }

        err = mpe_hnServerGetPlayspeedsCnt(mpeContentLocation, mpeContentDescription,
                (char *) nativeProfileId, (char *) nativeMimeType, mpeContentTransformation, &count);
        if (err != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnServerGetPlayspeedsCnt() "
                    "returned %d\n", __FUNCTION__, err);
            throwMPEMediaError(env, err, "mpe_hnServerGetPlayspeedsCnt()");
        }
        else
        {
            if (count > 0)
            {
                retObject = (*env)->NewObjectArray(env, count, jniutil_CachedIds.String, NULL);
                if (retObject != NULL)
                {
                    int i = 0;
                    char playspeed[MPE_HN_MAX_PLAYSPEED_STR_SIZE];

                    for (i = 0; i < count; i++)
                    {
                        // TODO: This takes a 'transformation' parameter.
                        err = mpe_hnServerGetPlayspeedStr(mpeContentLocation, mpeContentDescription,
                                (char *) nativeProfileId, (char *) nativeMimeType, mpeContentTransformation, i, playspeed);
                        if (err != MPE_SUCCESS)
                        {
                            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnServerGetPlayspeedStr(%s, %s) "
                                    "returned %d\n", __FUNCTION__, nativeProfileId, nativeMimeType, err);
                            throwMPEMediaError(env, err, "mpe_hnServerGetPlayspeedStr()");
                            break;
                        }
                        else
                        {
                            jstring arrayElem = (*env)->NewStringUTF(env, playspeed);
                            if (arrayElem == NULL)
                            {
                                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() NewStringUTF(%s) returned NULL\n",
                                        __FUNCTION__, playspeed);
                                break;
                            }
                            else
                            {
                                (*env)->SetObjectArrayElement(env, retObject, i, arrayElem);
                            }
                        }
                    }
                }
                else
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() NewObjectArray(%u) returned NULL\n",
                            __FUNCTION__, count);
                }
            }
        }

        if (profileId != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, profileId, nativeProfileId);
        }
        if (mimeType != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, mimeType, nativeMimeType);
        }
        deallocateContentDescription(mpeContentLocation, mpeContentDescription);
        deallocateContentTransformation(mpeContentTransformation);
    }

    return retObject;
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativeServerGetFrameTypesInTrickMode
 * Signature: (ILorg/cablelabs/impl/media/streaming/session/data/HNStreamContentDescription;Ljava/lang/String;Ljava/lang/String;F)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativeServerGetFrameTypesInTrickMode
  (JNIEnv *env, jclass cls, jint contentLocationType, jobject contentDescription,
   jstring profileId, jstring mimeType, jobject jTransformation, jfloat playspeedRate)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_HnStreamContentLocation mpeContentLocation = (mpe_HnStreamContentLocation) contentLocationType;
    void *mpeContentDescription = NULL;
    const char* nativeProfileId = NULL;
    const char* nativeMimeType = NULL;
    mpe_HnHttpHeaderFrameTypesInTrickMode frameTypes = MPE_HN_TRICK_MODE_FRAME_TYPE_NONE;

    MPE_UNUSED_PARAM(cls);

    err = buildContentDescriptionFromObject(env,
            contentLocationType, contentDescription, &mpeContentDescription);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() buildContentDescriptionFromObject() "
                "returned %d\n", __FUNCTION__, err);
        throwMPEMediaError(env, err, "buildContentDescriptionFromObject()");
    }
    else if (profileId != NULL && (nativeProfileId = (*env)->GetStringUTFChars(env, profileId, NULL)) == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() - GetStringUTFChars(profileId) "
                "returned NULL\n", __FUNCTION__);
        deallocateContentDescription(mpeContentLocation, mpeContentDescription);
    }
    else if (mimeType != NULL && (nativeMimeType = (*env)->GetStringUTFChars(env, mimeType, NULL)) == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() - GetStringUTFChars(mimeType) "
                "returned NULL\n", __FUNCTION__);
        if (profileId != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, profileId, nativeProfileId);
        }
        deallocateContentDescription(mpeContentLocation, mpeContentDescription);
    }
    else
    {
        mpe_hnContentTransformation *mpeContentTransformation = NULL;
        {
            //
            // Transformation
            //
            if ((err = buildContentTransformationFromObject(env, jTransformation, &mpeContentTransformation)) != MPE_SUCCESS)
            {
                throwMPEMediaError(env, err, "buildContentTransformationFromObject() failed.");
            }
            if (mpeContentTransformation != NULL)
            {
                mpe_hnContentTransformation * transformation = mpeContentTransformation;
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() transformation: id %d, source %s, dest %s, width %d, height %d, bitrate %d, progressive %d\n",
                        __FUNCTION__, transformation->id, transformation->sourceProfile,
                        transformation->transformedProfile, transformation->width,
                        transformation->height, transformation->bitrate, transformation->progressive );
            }
        }
        err = mpe_hnServerGetFrameTypesInTrickMode(mpeContentLocation, mpeContentDescription,
                (char *) nativeProfileId, (char *) nativeMimeType, mpeContentTransformation, playspeedRate, &frameTypes);
        if (err != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnServerGetFrameTypesInTrickMode(%s, %s, %f) "
                    "returned %d\n", __FUNCTION__, nativeProfileId, nativeMimeType, playspeedRate, err);
            throwMPEMediaError(env, err, "mpe_hnServerGetFrameTypesInTrickMode()");
        }

        if (profileId != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, profileId, nativeProfileId);
        }
        if (mimeType != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, mimeType, nativeMimeType);
        }
        deallocateContentDescription(mpeContentLocation, mpeContentDescription);
        deallocateContentTransformation(mpeContentTransformation);
    }

    return (jint) frameTypes;
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativeServerGetFrameRateInTrickMode
 * Signature: (ILorg/cablelabs/impl/media/streaming/session/data/HNStreamContentDescription;Ljava/lang/String;Ljava/lang/String;F)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativeServerGetFrameRateInTrickMode
  (JNIEnv *env, jclass cls, jint contentLocationType, jobject contentDescription,
   jstring profileId, jstring mimeType, jobject jTransformation, jfloat playspeedRate)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_HnStreamContentLocation mpeContentLocation = (mpe_HnStreamContentLocation) contentLocationType;
    void *mpeContentDescription = NULL;
    const char* nativeProfileId = NULL;
    const char* nativeMimeType = NULL;
    int32_t frameRate = -1;

    MPE_UNUSED_PARAM(cls);

    err = buildContentDescriptionFromObject(env,
            contentLocationType, contentDescription, &mpeContentDescription);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() buildContentDescriptionFromObject() "
                "returned %d\n", __FUNCTION__, err);
        throwMPEMediaError(env, err, "buildContentDescriptionFromObject()");
    }
    else if (profileId != NULL && (nativeProfileId = (*env)->GetStringUTFChars(env, profileId, NULL)) == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() - GetStringUTFChars(profileId) "
                "returned NULL\n", __FUNCTION__);
        deallocateContentDescription(mpeContentLocation, mpeContentDescription);
    }
    else if (mimeType != NULL && (nativeMimeType = (*env)->GetStringUTFChars(env, mimeType, NULL)) == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() - GetStringUTFChars(mimeType) "
                "returned NULL\n", __FUNCTION__);
        if (profileId != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, profileId, nativeProfileId);
        }
        deallocateContentDescription(mpeContentLocation, mpeContentDescription);
    }
    else
    {
        mpe_hnContentTransformation *mpeContentTransformation = NULL;
        {
            //
            // Transformation
            //
            if ((err = buildContentTransformationFromObject(env, jTransformation, &mpeContentTransformation)) != MPE_SUCCESS)
            {
                throwMPEMediaError(env, err, "buildContentTransformationFromObject() failed.");
            }
            if (mpeContentTransformation != NULL)
            {
                mpe_hnContentTransformation * transformation = mpeContentTransformation;
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() transformation: id %d, source %s, dest %s, width %d, height %d, bitrate %d, progressive %d\n",
                        __FUNCTION__, transformation->id, transformation->sourceProfile,
                        transformation->transformedProfile, transformation->width,
                        transformation->height, transformation->bitrate, transformation->progressive );
            }
        }
        err = mpe_hnServerGetFrameRateInTrickMode(mpeContentLocation, mpeContentDescription,
                (char *) nativeProfileId, (char *) nativeMimeType, mpeContentTransformation, playspeedRate, &frameRate);
        if (err != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnServerGetFrameRateInTrickMode(%s, %s, %f) "
                    "returned %d\n", __FUNCTION__, nativeProfileId, nativeMimeType, playspeedRate, err);
            throwMPEMediaError(env, err, "mpe_hnServerGetFrameRateInTrickMode()");
        }

        if (profileId != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, profileId, nativeProfileId);
        }
        if (mimeType != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, mimeType, nativeMimeType);
        }
        deallocateContentDescription(mpeContentLocation, mpeContentDescription);
        deallocateContentTransformation(mpeContentTransformation);
    }

    return (jint) frameRate;
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativeServerGet
 * Signature: (ILorg/cablelabs/impl/media/streaming/session/data/HNStreamContentDescription;Ljava/lang/String;Ljava/lang/String;F)I
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativeServerGetConnectionStallingFlag
  (JNIEnv *env, jclass cls, jint contentLocationType, jobject contentDescription,
   jstring profileId, jstring mimeType, jobject jTransformation)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_HnStreamContentLocation mpeContentLocation = (mpe_HnStreamContentLocation) contentLocationType;
    void *mpeContentDescription = NULL;
    const char* nativeProfileId = NULL;
    const char* nativeMimeType = NULL;
    mpe_Bool connectionStallingSupportedFlag = FALSE;

    MPE_UNUSED_PARAM(cls);

    err = buildContentDescriptionFromObject(env,
            contentLocationType, contentDescription, &mpeContentDescription);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() buildContentDescriptionFromObject() "
                "returned %d\n", __FUNCTION__, err);
        throwMPEMediaError(env, err, "buildContentDescriptionFromObject()");
    }
    else if (profileId != NULL && (nativeProfileId = (*env)->GetStringUTFChars(env, profileId, NULL)) == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() - GetStringUTFChars(profileId) "
                "returned NULL\n", __FUNCTION__);
        deallocateContentDescription(mpeContentLocation, mpeContentDescription);
    }
    else if (mimeType != NULL && (nativeMimeType = (*env)->GetStringUTFChars(env, mimeType, NULL)) == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() - GetStringUTFChars(mimeType) "
                "returned NULL\n", __FUNCTION__);
        if (profileId != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, profileId, nativeProfileId);
        }
        deallocateContentDescription(mpeContentLocation, mpeContentDescription);
    }
    else
    {
        mpe_hnContentTransformation *mpeContentTransformation = NULL;
        {
            //
            // Transformation
            //
            if ((err = buildContentTransformationFromObject(env, jTransformation, &mpeContentTransformation)) != MPE_SUCCESS)
            {
                throwMPEMediaError(env, err, "buildContentTransformationFromObject() failed.");
            }
            if (mpeContentTransformation != NULL)
            {
                mpe_hnContentTransformation * transformation = mpeContentTransformation;
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() transformation: id %d, source %s, dest %s, width %d, height %d, bitrate %d, progressive %d\n",
                        __FUNCTION__, transformation->id, transformation->sourceProfile,
                        transformation->transformedProfile, transformation->width,
                        transformation->height, transformation->bitrate, transformation->progressive );
            }
        }
        err = mpe_hnServerGetConnectionStallingFlag(mpeContentLocation, mpeContentDescription,
                (char *) nativeProfileId, (char *) nativeMimeType, mpeContentTransformation,
                &connectionStallingSupportedFlag);
        if (err != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnServerGetConnectionStallingFlag(%s, %s) "
                    "returned %d\n", __FUNCTION__, nativeProfileId, nativeMimeType, err);
            throwMPEMediaError(env, err, "mpe_hnServerGetConnectionStallingFlag()");
        }

        if (profileId != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, profileId, nativeProfileId);
        }
        if (mimeType != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, mimeType, nativeMimeType);
        }
        deallocateContentDescription(mpeContentLocation, mpeContentDescription);
        deallocateContentTransformation(mpeContentTransformation);
    }

    return (jboolean) connectionStallingSupportedFlag;
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativeServerGet
 * Signature: (ILorg/cablelabs/impl/media/streaming/session/data/HNStreamContentDescription;Ljava/lang/String;Ljava/lang/String;F)I
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativeServerGetServerSidePacingRestampFlag
  (JNIEnv *env, jclass cls, jint contentLocationType, jobject contentDescription,
   jstring profileId, jstring mimeType, jobject jTransformation)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_HnStreamContentLocation mpeContentLocation = (mpe_HnStreamContentLocation) contentLocationType;
    void *mpeContentDescription = NULL;
    const char* nativeProfileId = NULL;
    const char* nativeMimeType = NULL;
    mpe_Bool serverSidePacingRestampFlag = FALSE;

    MPE_UNUSED_PARAM(cls);

    err = buildContentDescriptionFromObject(env,
            contentLocationType, contentDescription, &mpeContentDescription);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() buildContentDescriptionFromObject() "
                "returned %d\n", __FUNCTION__, err);
        throwMPEMediaError(env, err, "buildContentDescriptionFromObject()");
    }
    else if (profileId != NULL && (nativeProfileId = (*env)->GetStringUTFChars(env, profileId, NULL)) == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() - GetStringUTFChars(profileId) "
                "returned NULL\n", __FUNCTION__);
        deallocateContentDescription(mpeContentLocation, mpeContentDescription);
    }
    else if (mimeType != NULL && (nativeMimeType = (*env)->GetStringUTFChars(env, mimeType, NULL)) == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() - GetStringUTFChars(mimeType) "
                "returned NULL\n", __FUNCTION__);
        if (profileId != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, profileId, nativeProfileId);
        }
        deallocateContentDescription(mpeContentLocation, mpeContentDescription);
    }
    else
    {
        mpe_hnContentTransformation *mpeContentTransformation = NULL;
        {
            //
            // Transformation
            //
            if ((err = buildContentTransformationFromObject(env, jTransformation, &mpeContentTransformation)) != MPE_SUCCESS)
            {
                throwMPEMediaError(env, err, "buildContentTransformationFromObject() failed.");
            }
            if (mpeContentTransformation != NULL)
            {
                mpe_hnContentTransformation * transformation = mpeContentTransformation;
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() transformation: id %d, source %s, dest %s, width %d, height %d, bitrate %d, progressive %d\n",
                        __FUNCTION__, transformation->id, transformation->sourceProfile,
                        transformation->transformedProfile, transformation->width,
                        transformation->height, transformation->bitrate, transformation->progressive );
            }
        }
        err = mpe_hnServerGetServerSidePacingRestampFlag(mpeContentLocation, mpeContentDescription,
                (char *) nativeProfileId, (char *) nativeMimeType, mpeContentTransformation, &serverSidePacingRestampFlag);
        if (err != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnServerGetServerSidePacingRestampFlag(%s, %s) "
                    "returned %d\n", __FUNCTION__, nativeProfileId, nativeMimeType, err);
            throwMPEMediaError(env, err, "mpe_hnServerGetServerSidePacingRestampFlag()");
        }

        if (profileId != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, profileId, nativeProfileId);
        }
        if (mimeType != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, mimeType, nativeMimeType);
        }
        deallocateContentDescription(mpeContentLocation, mpeContentDescription);
        deallocateContentTransformation(mpeContentTransformation);
    }

    return (jboolean) serverSidePacingRestampFlag;
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativeSetLinkLocalAddress
 * Signature: (Ljava/lang/String;)(I)V
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativeSetLinkLocalAddress(JNIEnv *env, jclass cls, jstring interface)
{
    mpe_Bool linkLocalAddressSet = FALSE;
    const char *interfaceSz = NULL;

    MPE_UNUSED_PARAM(cls);

    /* Allocate interface string */
    if (interface != NULL && (interfaceSz = (*env)->GetStringUTFChars(env, interface, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() - GetStringUTFChars(interface) "
                "returned NULL\n", __FUNCTION__);
        return (jboolean) linkLocalAddressSet;
    }
    mpe_Error err = mpe_socketSetLinkLocalAddress((char *)interfaceSz);
 
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() mpe_hnSetLinkLocalAddress"
               "returned %d\n", __FUNCTION__, err);
    }
    else
    {
        linkLocalAddressSet = TRUE;
    }

    return (jboolean) linkLocalAddressSet;
}

/*****************************************************************************/
/***                                                                       ***/
/***                              Player only                              ***/
/***                                                                       ***/
/*****************************************************************************/

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativePlayerGetConnectionId
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativePlayerGetConnectionId
  (JNIEnv *env, jclass cls, jint nativeStreamSession)
{
    jint retConnectionId = (jint) -1;
    mpe_Error err = MPE_SUCCESS;
    mpe_HnStreamSession playerStreamSession = (mpe_HnStreamSession) nativeStreamSession;
    mpe_HnStreamParams streamParams;
    mpe_HnStreamParamsMediaPlayerHttp playerStreamParams;

    MPE_UNUSED_PARAM(cls);

    streamParams.requestType = MPE_HNSTREAM_MEDIA_PLAYER_HTTP;
    streamParams.streamParams = &playerStreamParams;
    playerStreamParams.connectionId = (uint32_t) -1;
    playerStreamParams.uri = NULL;
    // *TODO* - reverted MPEOS change
    //playerStreamParams.protocolInfo.fourthField.pn_param.dlnaProfileId = NULL;
    playerStreamParams.dlnaProfileId = NULL;
    // *TODO* - reverted MPEOS change
    //playerStreamParams.protocolInfo.mimeType = NULL;
    playerStreamParams.mimeType = NULL;
    playerStreamParams.host = NULL;
    playerStreamParams.port = (uint32_t) -1;
    // *TODO* - reverted MPEOS change
    //playerStreamParams.protocolInfo.dtcpHost = NULL;
    playerStreamParams.dtcp_host = NULL;
    // *TODO* - reverted MPEOS change
    //playerStreamParams.protocolInfo.dtcpPort = (uint32_t) -1;
    playerStreamParams.dtcp_port = (uint32_t) -1;

    err = mpe_hnPlayerStreamGetInfo(playerStreamSession, &streamParams);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnPlayerStreamGetInfo(0x%X) "
                    "returned %d\n", __FUNCTION__, playerStreamSession, err);
        throwMPEMediaError(env, err, "mpe_hnPlayerStreamGetInfo()");
    }
    else
    {
        retConnectionId = (jint) playerStreamParams.connectionId;
    }

    return retConnectionId;
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativePlayerPlaybackChangePIDs
 * Signature: (ILorg/cablelabs/impl/media/streaming/session/data/HNHttpHeaderAVStreamParameters;)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativePlayerPlaybackChangePIDs
  (JNIEnv *env, jclass cls, jint nativePlaybackSession, jobject avsParams)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_HnPlaybackSession playbackSession = (mpe_HnPlaybackSession) nativePlaybackSession;
    mpe_HnHttpHeaderAVStreamParameters pids;

    MPE_UNUSED_PARAM(cls);

    populateAVStreamParametersFromObject(env, avsParams, &pids);

    err = mpe_hnPlayerPlaybackChangePIDs(playbackSession, &pids);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnPlayerPlaybackChangePIDs(0x%X) "
                    "returned %d\n", __FUNCTION__, playbackSession, err);
        throwMPEMediaError(env, err, "mpe_hnPlayerPlaybackChangePIDs()");
    }
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativePlayerPlaybackUpdateCCI
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativePlayerPlaybackUpdateCCI
  (JNIEnv *env, jclass cls, jint nativePlaybackSession, jobjectArray jCciDescriptors)
{
    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);
    MPE_UNUSED_PARAM(nativePlaybackSession);
    MPE_UNUSED_PARAM(jCciDescriptors);
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativePlayerPlaybackGetTime
 * Signature: (I)Ljavax/media/Time;
 */
JNIEXPORT jobject JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativePlayerPlaybackGetTime
  (JNIEnv *env, jclass cls, jint nativePlaybackSession)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_HnPlaybackSession playbackSession = (mpe_HnPlaybackSession) nativePlaybackSession;
    int64_t mediaTime = (int64_t) -1;
    jobject retObject = (jobject) NULL;

    MPE_UNUSED_PARAM(cls);

    err = mpe_hnPlayerPlaybackGetTime(playbackSession, &mediaTime);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnPlayerPlaybackGetTime(0x%X) "
                    "returned %d\n", __FUNCTION__, playbackSession, err);
        throwMPEMediaError(env, err, "mpe_hnPlayerPlaybackGetTime()");
    }
    else
    {
        retObject = (*env)->NewObject(env, jniutil_CachedIds.Time,
                jniutil_CachedIds.Time_init, (jlong) mediaTime);
    }
    
    return retObject;
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativePlayerPlaybackGetRate
 * Signature: (I)F
 */
JNIEXPORT jfloat JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativePlayerPlaybackGetRate
  (JNIEnv *env, jclass cls, jint nativePlaybackSession)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_HnPlaybackSession playbackSession = (mpe_HnPlaybackSession) nativePlaybackSession;
    float rate = 0.;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    err = mpe_hnPlayerPlaybackGetRate(playbackSession, &rate);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnPlayerPlaybackGetRate(0x%X) "
                    "returned %d\n", __FUNCTION__, playbackSession, err);
        throwMPEMediaError(env, err, "mpe_hnPlayerPlaybackGetRate()");
    }
    
    return (jfloat) rate;
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativePlayerPlaybackBlockPresentation
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativePlayerPlaybackBlockPresentation
  (JNIEnv *env, jclass cls, jint nativePlaybackSession, jboolean blockPresentation)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_HnPlaybackSession playbackSession = (mpe_HnPlaybackSession) nativePlaybackSession;
    mpe_Bool block = (mpe_Bool) blockPresentation;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    err = mpe_hnPlayerPlaybackBlockPresentation(playbackSession, block);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnPlayerPlaybackBlockPresentation(0x%X) "
                    "returned %d\n", __FUNCTION__, playbackSession, err);
        throwMPEMediaError(env, err, "mpe_hnPlayerPlaybackBlockPresentation()");
    }
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativePlayerPlaybackSetMute
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativePlayerPlaybackSetMute
  (JNIEnv *env, jclass cls, jint nativePlaybackSession, jboolean mute)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_HnPlaybackSession playbackSession = (mpe_HnPlaybackSession) nativePlaybackSession;
    mpe_Bool nativeMute = (mpe_Bool) mute;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    err = mpe_hnPlayerPlaybackSetMute(playbackSession, nativeMute);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnPlayerPlaybackSetMute(0x%X) "
                    "returned %d\n", __FUNCTION__, playbackSession, err);
        throwMPEMediaError(env, err, "mpe_hnPlayerPlaybackSetMute()");
    }
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativePlayerPlaybackSetGain
 * Signature: (IF)F
 */
JNIEXPORT jfloat JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativePlayerPlaybackSetGain
  (JNIEnv *env, jclass cls, jint nativePlaybackSession, jfloat gain)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_HnPlaybackSession playbackSession = (mpe_HnPlaybackSession) nativePlaybackSession;
    float nativeGain = (float) gain;
    float actualGain = 0.;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    err = mpe_hnPlayerPlaybackSetGain(playbackSession, nativeGain, &actualGain);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnPlayerPlaybackSetGain(0x%X) "
                    "returned %d\n", __FUNCTION__, playbackSession, err);
        throwMPEMediaError(env, err, "mpe_hnPlayerPlaybackSetGain()");
    }

    return (jfloat) actualGain;
}

/*
 * Param scanModeArray - By convention, scanMode is element 0.  This are filled 
 * in by this routine.
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativePlayerGetVideoScanMode(
        JNIEnv *env, jclass cls, jint nativePlaybackSession, jintArray scanModeArray)
{
    mpe_Error err = MPE_SUCCESS;

    mpe_MediaScanMode scanMode;

    err = mpe_hnPlayerPlaybackGetVideoScanMode((mpe_HnPlaybackSession) nativePlaybackSession, 
        &scanMode);
    if (err != 0)
    {
        return err;
    }

    (*env)->SetIntArrayRegion(env, scanModeArray, 0, 1, (jint *)(&scanMode));

    return err;
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativePlayerPlaybackGetS3DConfiguration
 * Signature: (I[I[BI)I
 *
 * Param specifierArray - By convention, payloadType is element 0, formatType
 * is element 1, and payloadSz is element 2 in the specifierArray.  These are filled in by this routine.
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativePlayerPlaybackGetS3DConfiguration
  (JNIEnv *env, jclass cls, jint nativePlaybackSession,
   jintArray specifierArray, jbyteArray payloadArray, jint payloadArraySz)
{
    mpe_Error err = MPE_SUCCESS;

    mpe_Media3DPayloadType payloadType;
    mpe_DispStereoscopicMode stereoscopicMode;

    uint32_t payloadSz = payloadArraySz;
    uint8_t *payload;

    MPE_UNUSED_PARAM(cls);

    err = mpe_memAllocP(MPE_MEM_TEMP, payloadArraySz, (void**) &payload);
    if (err != 0)
    {
        return err;
    }

    err = mpe_hnPlayerPlaybackGet3DConfig((mpe_HnPlaybackSession) nativePlaybackSession,
            &stereoscopicMode, &payloadType, payload, &payloadSz);
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

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativePlayerGetDLNAProfileIds
 * Signature: ()[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativePlayerGetDLNAProfileIds
  (JNIEnv *env, jclass cls)
{
    jobjectArray retObject = (jobjectArray) NULL;
    uint32_t count = 0;
    mpe_Error err = mpe_hnPlayerGetDLNAProfileIDsCnt(&count);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnPlayerGetDLNAProfileIDsCnt() "
                "returned %d\n", __FUNCTION__, err);
        throwMPEMediaError(env, err, "mpe_hnPlayerGetDLNAProfileIDsCnt()");
    }
    else
    {
        retObject = (*env)->NewObjectArray(env, count, jniutil_CachedIds.String, NULL);
        if (retObject != NULL)
        {
            int i = 0;
            char profileId[MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE];

            for (i = 0; i < count; i++)
            {
                err = mpe_hnPlayerGetDLNAProfileIDStr(i, profileId);
                if (err != MPE_SUCCESS)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnPlayerGetDLNAProfileIDStr() "
                            "returned %d\n", __FUNCTION__, err);
                    throwMPEMediaError(env, err, "mpe_hnPlayerGetDLNAProfileIDStr()");
                    break;
                }
                else
                {
                    jstring arrayElem = (*env)->NewStringUTF(env, profileId);
                    if (arrayElem == NULL)
                    {
                        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() NewStringUTF(%s) returned NULL\n",
                                __FUNCTION__, profileId);
                        break;
                    }
                    else
                    {
                        (*env)->SetObjectArrayElement(env, retObject, i, arrayElem);
                    }
                }
            }
        }
        else
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() NewObjectArray(%u) returned NULL\n",
                    __FUNCTION__, count);
        }
    }

    return retObject;
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativePlayerGetMimeTypes
 * Signature: (Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativePlayerGetMimeTypes
  (JNIEnv *env, jclass cls, jstring profileId)
{
    jobjectArray retObject = (jobjectArray) NULL;
    const char* nativeProfileId = NULL;

    if (profileId != NULL && (nativeProfileId = (*env)->GetStringUTFChars(env, profileId, NULL)) == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() - GetStringUTFChars(profileId) "
                "returned NULL\n", __FUNCTION__);
    }
    else
    {
        uint32_t count = 0;
        mpe_Error err = mpe_hnPlayerGetMimeTypesCnt((char *) nativeProfileId, &count);
        if (err != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnPlayerGetMimeTypesCnt(%s) "
                    "returned %d\n", __FUNCTION__, nativeProfileId, err);
            throwMPEMediaError(env, err, "mpe_hnPlayerGetMimeTypesCnt()");
        }
        else
        {
            retObject = (*env)->NewObjectArray(env, count, jniutil_CachedIds.String, NULL);
            if (retObject != NULL)
            {
                int i = 0;
                char mimeType[MPE_HN_MAX_MIME_TYPE_STR_SIZE];

                for (i = 0; i < count; i++)
                {
                    err = mpe_hnPlayerGetMimeTypeStr((char *) nativeProfileId, i, mimeType);
                    if (err != MPE_SUCCESS)
                    {
                        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnPlayerGetMimeTypeStr(%s) "
                                "returned %d\n", __FUNCTION__, nativeProfileId, err);
                        throwMPEMediaError(env, err, "mpe_hnPlayerGetMimeTypeStr()");
                        break;
                    }
                    else
                    {
                        jstring arrayElem = (*env)->NewStringUTF(env, mimeType);
                        if (arrayElem == NULL)
                        {
                            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() NewStringUTF(%s) returned NULL\n",
                                    __FUNCTION__, mimeType);
                            break;
                        }
                        else
                        {
                            (*env)->SetObjectArrayElement(env, retObject, i, arrayElem);
                        }
                    }
                }
            }
            else
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() NewObjectArray(%u) returned NULL\n",
                        __FUNCTION__, count);
            }
        }

        if (profileId != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, profileId, nativeProfileId);
        }
    }

    return retObject;
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativePlayerGetPlayspeeds
 * Signature: (Ljava/lang/String;Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativePlayerGetPlayspeeds
  (JNIEnv *env, jclass cls, jstring profileId, jstring mimeType)
{
    jobjectArray retObject = (jobjectArray) NULL;
    const char* nativeProfileId = NULL;
    const char* nativeMimeType = NULL;

    if (profileId != NULL && (nativeProfileId = (*env)->GetStringUTFChars(env, profileId, NULL)) == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() - GetStringUTFChars(profileId) "
                "returned NULL\n", __FUNCTION__);
    }
    else if (mimeType != NULL && (nativeMimeType = (*env)->GetStringUTFChars(env, mimeType, NULL)) == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() - GetStringUTFChars(mimeType) "
                "returned NULL\n", __FUNCTION__);
        if (profileId != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, profileId, nativeProfileId);
        }
    }
    else
    {
        uint32_t count = 0;
        mpe_Error err = mpe_hnPlayerGetPlayspeedsCnt((char *) nativeProfileId, (char *) nativeMimeType, &count);
        if (err != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnPlayerGetPlayspeedsCnt(%s, %s) "
                    "returned %d\n", __FUNCTION__, nativeProfileId, nativeMimeType, err);
            throwMPEMediaError(env, err, "mpe_hnPlayerGetPlayspeedsCnt()");
        }
        else
        {
            retObject = (*env)->NewObjectArray(env, count, jniutil_CachedIds.String, NULL);
            if (retObject != NULL)
            {
                int i = 0;
                char playspeed[MPE_HN_MAX_PLAYSPEED_STR_SIZE];

                for (i = 0; i < count; i++)
                {
                    err = mpe_hnPlayerGetPlayspeedStr((char *) nativeProfileId, (char *) nativeMimeType, i, playspeed);
                    if (err != MPE_SUCCESS)
                    {
                        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnPlayerGetPlayspeedStr(%s, %s) "
                                "returned %d\n", __FUNCTION__, nativeProfileId, nativeMimeType, err);
                        throwMPEMediaError(env, err, "mpe_hnPlayerGetPlayspeedStr()");
                        break;
                    }
                    else
                    {
                        jstring arrayElem = (*env)->NewStringUTF(env, playspeed);
                        if (arrayElem == NULL)
                        {
                            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() NewStringUTF(%s) returned NULL\n",
                                    __FUNCTION__, playspeed);
                            break;
                        }
                        else
                        {
                            (*env)->SetObjectArrayElement(env, retObject, i, arrayElem);
                        }
                    }
                }
            }
            else
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() NewObjectArray(%u) returned NULL\n",
                        __FUNCTION__, count);
            }
        }

        if (profileId != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, profileId, nativeProfileId);
        }
        if (mimeType != NULL)
        {
            (*env)->ReleaseStringUTFChars(env, mimeType, nativeMimeType);
        }
    }

    return retObject;
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativePlayerPlaybackPause
 * Signature: (ILorg/cablelabs/impl/media/streaming/session/data/HNPlaybackParams;FZZF)Lorg/cablelabs/impl/media/mpe/HNAPI$Playback;
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativePlayerPlaybackPause
  (JNIEnv *env, jclass cls, jint nativePlaybackSession)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_HnPlaybackSession playbackSession = (mpe_HnPlaybackSession) nativePlaybackSession;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    err = mpe_hnPlayerPlaybackPause(playbackSession);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnPlayerPlaybackPause(0x%X) "
                    "returned %d\n", __FUNCTION__, playbackSession, err);
        throwMPEMediaError(env, err, "mpe_hnPlayerPlaybackPause()");
    }
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativePlayerPlaybackResume
 * Signature: (ILorg/cablelabs/impl/media/streaming/session/data/HNPlaybackParams;FZZF)Lorg/cablelabs/impl/media/mpe/HNAPI$Playback;
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativePlayerPlaybackResume
  (JNIEnv *env, jclass cls, jint nativePlaybackSession)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_HnPlaybackSession playbackSession = (mpe_HnPlaybackSession) nativePlaybackSession;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    err = mpe_hnPlayerPlaybackResume(playbackSession);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_hnPlayerPlaybackResume(0x%X) "
                    "returned %d\n", __FUNCTION__, playbackSession, err);
        throwMPEMediaError(env, err, "mpe_hnPlayerPlaybackResume()");
    }
}

/*
 * Class:     org_cablelabs_impl_media_mpe_HNAPIImpl
 * Method:    nativeGetLPEWakeUpVariables
 * Signature: (ILorg/cablelabs/impl/media/streaming/session/data/HNPlaybackParams;FZZF)Lorg/cablelabs/impl/media/mpe/HNAPI$Playback;
 */
JNIEXPORT jobject JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativeGetLPEWakeUpVariables
  (JNIEnv *env, jclass cls, jstring interfaceName)
{
    mpe_Error err = MPE_SUCCESS;
    jobject LPEWakeUpObject = (jobject) 0;
    mpeos_LpeDlnaNetworkInterfaceInfo tempNetworkIfInfo;
    const char* nativeInterfaceName = NULL;
    jstring jWakeOnPattern = (jstring) NULL;
    jstring jWakeSupportedTransport = (jstring) NULL;
    
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() enter\n", __FUNCTION__);
    if (interfaceName == NULL)
    {    
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() jstring interfaceName "
                "returned NULL\n", __FUNCTION__);
    }
    else
    {
        if ((nativeInterfaceName = (*env)->GetStringUTFChars(env, interfaceName, 
            NULL)) == NULL)
        {
            /* GetStringUTFChars threw a memory exception */
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() GetStringUTFChars(interfaceName) "
                "returned NULL\n", __FUNCTION__);
        }
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() unused parameter interfaceName is %s\n", 
        __FUNCTION__, nativeInterfaceName);

    err = mpe_getDLNANetworkInterfaceInfo((char*) nativeInterfaceName, &tempNetworkIfInfo);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_getDLNANetworkInterfaceInfo "
                    "returned %d as Error\n", __FUNCTION__, err);
    }
    else
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() buildLPEWakeUpParamsFromObject "
                "returned %s, %d, %d, %d\n", __FUNCTION__, tempNetworkIfInfo.wakeOnPattern,
                tempNetworkIfInfo.wakeSupportedTransport, tempNetworkIfInfo.maxWakeOnDelay, 
                tempNetworkIfInfo.dozeDuration);

        jWakeOnPattern = (*env)->NewStringUTF(env, tempNetworkIfInfo.wakeOnPattern);
        jWakeSupportedTransport = (*env)->NewStringUTF(env, tempNetworkIfInfo.wakeSupportedTransport);
        LPEWakeUpObject = (*env)->NewObject(env, jniutil_CachedIds.HNAPI_LPEWakeUp, 
            jniutil_CachedIds.HNAPI_LPEWakeUp_init, jWakeOnPattern, jWakeSupportedTransport,
            tempNetworkIfInfo.maxWakeOnDelay, tempNetworkIfInfo.dozeDuration);
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() exit\n", __FUNCTION__);

    return LPEWakeUpObject;
}

/**
 * Gets the DLNA LPE NetworkInterfaceMode information, after providing the interfaceName
 * NetworkInterfaceMode can be: :Unimplemented", "IP-up", "IP-up-Periodic", "IP-down-no-Wake",
 * "IP-down-with-WakeOn", "IP-down-with-WakeAuto", "IP-Down-with-WakeOnAuto"
 * @return Upon successful completion, returns the string which describes the NetworkInterfaceMode currently.
 *
 */
JNIEXPORT jstring JNICALL Java_org_cablelabs_impl_media_mpe_HNAPIImpl_nativeGetLPENetworkInterfaceMode
  (JNIEnv *env, jclass cls, jstring interfaceName)
{
    mpe_Error err = MPE_SUCCESS;
    const char* nativeInterfaceName = NULL;
    char* networkIfMode = NULL;
    jstring jNetworkInterfaceMode = (jstring) NULL;
    
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() enter\n", __FUNCTION__);
    if (interfaceName == NULL)
    {    
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() jstring interfaceName "
                "returned NULL\n", __FUNCTION__);
    }
    else
    {
        if ((nativeInterfaceName = (*env)->GetStringUTFChars(env, interfaceName, 
            NULL)) == NULL)
        {
            /* GetStringUTFChars threw a memory exception */
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() GetStringUTFChars(interfaceName) "
                "returned NULL\n", __FUNCTION__);
        }
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() unused parameter interfaceName is %s\n", 
        __FUNCTION__, nativeInterfaceName);

    err = mpe_getDLNANetworkInterfaceMode((char*) nativeInterfaceName, &networkIfMode);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_getDLNANetworkInterfaceMode "
                    "returned %d\n", __FUNCTION__, err);
    }
    else
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() mpe_getDLNANetworkInterfaceMode "
                "returned %s", __FUNCTION__, networkIfMode);

        jNetworkInterfaceMode = (*env)->NewStringUTF(env, networkIfMode);
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() exit\n", __FUNCTION__);

    return jNetworkInterfaceMode;
}
