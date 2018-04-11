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

#include "org_cablelabs_impl_media_mpe_MediaAPIImpl.h"
#include "org_ocap_media_ClosedCaptioningControl.h"
#include "jni_util.h"
#include <mpe_media.h>
#include <mpe_disp.h>
#include <mpe_dbg.h>
#include <mpe_os.h>
#include <mpe_ed.h>
#include <mpe_caption.h>

JNIEXPORT void JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniInit
(JNIEnv *env, jclass cls)
{
    /* HScreenRectangle */
    GET_CLASS(HScreenRectangle, "org/havi/ui/HScreenRectangle");
    GET_FIELD_ID(HScreenRectangle_x, "x", "F");
    GET_FIELD_ID(HScreenRectangle_y, "y", "F");
    GET_FIELD_ID(HScreenRectangle_width, "width", "F");
    GET_FIELD_ID(HScreenRectangle_height, "height", "F");

    /* MediaDecodeParams */
    GET_CLASS(MediaDecodeParams, "org/cablelabs/impl/media/mpe/MediaDecodeParams");
    GET_FIELD_ID(MediaDecodeParams_listener, "listener", "Lorg/cablelabs/impl/manager/ed/EDListener;");
    GET_FIELD_ID(MediaDecodeParams_videoHandle, "videoHandle", "I");
    GET_FIELD_ID(MediaDecodeParams_tunerHandle, "tunerHandle", "I");
    GET_FIELD_ID(MediaDecodeParams_ltsid, "ltsid", "S");
    GET_FIELD_ID(MediaDecodeParams_pcrPid, "pcrPid", "I");
    GET_FIELD_ID(MediaDecodeParams_streamPids, "streamPids", "[I");
    GET_FIELD_ID(MediaDecodeParams_streamTypes, "streamTypes", "[S");
    GET_FIELD_ID(MediaDecodeParams_blocked, "blocked", "Z");
    GET_FIELD_ID(MediaDecodeParams_muted, "mute", "Z");
    GET_FIELD_ID(MediaDecodeParams_gain, "gain", "[F");
    GET_FIELD_ID(MediaDecodeParams_cci, "cci", "B");

    /* MediaDripFeedParams */
    GET_CLASS(MediaDripFeedParams, "org/cablelabs/impl/media/mpe/MediaDripFeedParams");
    GET_FIELD_ID(MediaDripFeedParams_listener, "listener", "Lorg/cablelabs/impl/manager/ed/EDListener;");
    GET_FIELD_ID(MediaDripFeedParams_videoHandle, "videoHandle", "I");

}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniCheckBounds(
        JNIEnv *env, jclass cls, jint decoder, jobject desiredSrc,
        jobject desiredDst, jobject actualSrc, jobject actualDst)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_MediaRectangle desiredSrcMediaRect, actualSrcMediaRect,
            desiredDstMediaRect, actualDstMediaRect;

    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jniCheckBounds(decoder=0x%X)\n",
            (int)decoder);

    /* get source and destination Recangles from desiredSize */

    jniutil_getRectangleMedia(env, desiredSrc, &desiredSrcMediaRect);
    jniutil_getRectangleMedia(env, desiredDst, &desiredDstMediaRect);

    /* call MPE layer */
    err = mpe_mediaCheckBounds((mpe_DispDevice) decoder, &desiredSrcMediaRect,
            &desiredDstMediaRect, &actualSrcMediaRect, &actualDstMediaRect);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "ERROR in jniCheckBounds - mpe_mediaCheckBounds() = %d\n", err);
    }

    /* get the source and destination Rectangles from actualSize and set them from the returned Gfx rectangles */
    jniutil_setRectangleMedia(env, actualSrc, &actualSrcMediaRect);
    jniutil_setRectangleMedia(env, actualDst, &actualDstMediaRect);

    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniSetMute(
        JNIEnv *env, jclass cls, jint session, jboolean mute)
{
    mpe_Error err = MPE_SUCCESS;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    /* If successful, assign the returned mute to muteArray[0]. */
    err = mpe_mediaSetMute((mpe_MediaDecodeSession) session, (mpe_Bool)mute);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "mpe_mediaSetMute() = %d\n", err);
    }

    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniSetGain(
        JNIEnv *env, jclass cls, jint session, jfloatArray gainArray)
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
    err = mpe_mediaSetGain((mpe_MediaDecodeSession) session, (float)newGain, &actualGain);
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

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniSetCCI(
        JNIEnv *env, jclass cls, jint session, jbyte cci)
{
    mpe_Error err = MPE_SUCCESS;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jniSetCCI %d\n", cci);

    err = mpe_mediaSetCCI((mpe_MediaDecodeSession) session, cci);

    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniSetBounds(
        JNIEnv *env, jclass cls, jint decoder, jobject jSrcRect,
        jobject jDstRect)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_MediaRectangle mediaSrcRect, mediaDstRect;

    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jniSetBounds(decoder=0x%X)\n", (int)decoder);

    /* unpack Java Rectangles into mpe_gfxRectangles */
    jniutil_getRectangleMedia(env, jSrcRect, &mediaSrcRect);
    jniutil_getRectangleMedia(env, jDstRect, &mediaDstRect);

    /* call MPE */
    err = mpe_mediaSetBounds((mpe_DispDevice) decoder, &mediaSrcRect,
            &mediaDstRect);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "ERROR in jniSetBounds: mpe_mediaSetBounds() = %d\n", err);
    }

    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniGetBounds(
        JNIEnv *env, jclass cls, jint decoder, jobject jSrcRect,
        jobject jDstRect)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_MediaRectangle mediaSrcRect, mediaDstRect;

    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jniGetBounds(decoder=0x%X)\n", (int)decoder);

    /* call MPE */
    err = mpe_mediaGetBounds((mpe_DispDevice) decoder, &mediaSrcRect,
            &mediaDstRect);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "ERROR in jniGetBounds: mpe_mediaGetBounds() = %d\n", err);
    }
    else
    {
        jniutil_setRectangleMedia(env, jSrcRect, &mediaSrcRect);
        jniutil_setRectangleMedia(env, jDstRect, &mediaDstRect);
    }

    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniSwapDecoders(
        JNIEnv *env, jclass cls, jint decoder1Id, jint decoder2Id,
        jboolean audioUse)
{
    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    /* Call MPE media swap to perform low-level decoder swap. */
    return (jint) mpe_mediaSwapDecoders((mpe_DispDevice) decoder1Id,
            (mpe_DispDevice) decoder2Id, (mpe_Bool) audioUse);
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniDecode(
        JNIEnv *env, jclass cls, jobject decodeParams,
        jintArray sessionHandleArray)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_EdEventInfo *edHandle;
    mpe_MediaDecodeRequestParams reqParams;

    jobject edListener = 0;
    jint decoder = 0;
    jint tuner = 0;
    jshort ltsid;
    jint pcrPid = 0;
    jintArray pidArray = 0;
    jshortArray typeArray = 0;
    jboolean blocked = false;
    jboolean muted = false;
    jfloatArray gainArray = 0;
    jbyte cci = 0;

    jsize pidCount = 0;
    mpe_MediaPID *mpePids = 0;

    float requestedGain = 0;

    MPE_UNUSED_PARAM(cls);

    /* Unpack parameters from MediaDecodeParams object. */
    edListener = (*env)->GetObjectField(env, decodeParams,
            jniutil_CachedIds.MediaDecodeParams_listener);
    decoder = (*env)->GetIntField(env, decodeParams,
            jniutil_CachedIds.MediaDecodeParams_videoHandle);
    tuner = (*env)->GetIntField(env, decodeParams,
            jniutil_CachedIds.MediaDecodeParams_tunerHandle);
    ltsid = (*env)->GetShortField(env, decodeParams,
            jniutil_CachedIds.MediaDecodeParams_ltsid);
    pcrPid = (*env)->GetIntField(env, decodeParams,
            jniutil_CachedIds.MediaDecodeParams_pcrPid);
    pidArray = (*env)->GetObjectField(env, decodeParams,
            jniutil_CachedIds.MediaDecodeParams_streamPids);
    typeArray = (*env)->GetObjectField(env, decodeParams,
            jniutil_CachedIds.MediaDecodeParams_streamTypes);
    blocked = (*env)->GetBooleanField(env, decodeParams,
            jniutil_CachedIds.MediaDecodeParams_blocked);
    muted = (*env)->GetBooleanField(env, decodeParams,
            jniutil_CachedIds.MediaDecodeParams_muted);
    gainArray = (*env)->GetObjectField(env, decodeParams,
            jniutil_CachedIds.MediaDecodeParams_gain);
    cci = (*env)->GetByteField(env, decodeParams,
             jniutil_CachedIds.MediaDecodeParams_cci);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "jniDecode(decoder=0x%X, tuner=0x%X)\n", (int)decoder, (int)tuner);

    /* Allocate PID array if serviceComponentImpls is not empty */
    pidCount = (*env)->GetArrayLength(env, pidArray);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jniDecode(): %d pids\n", (int)pidCount);
    if (pidCount > 0 && (err = mpe_memAllocP(MPE_MEM_TEMP, sizeof(mpe_MediaPID)
            * pidCount, (void**) &mpePids)) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "ERROR in jniDecode: mpe_memAllocP() = %d\n", err);
        return err;
    }
    /* create array of mpe_MediaPIDs */
    jniutil_createPidArray(env, pidArray, typeArray, pidCount, mpePids);

    /* Get the gain from gainArray[0]. */
    (*env)->GetFloatArrayRegion(env, gainArray, 0, 1, &requestedGain);

    /* Initialize media request parameters */
    reqParams.tunerId = tuner;
    reqParams.ltsid = ltsid;
    reqParams.numPids = pidCount;
    reqParams.videoDevice = (mpe_DispDevice) decoder;
    reqParams.pids = mpePids;
    reqParams.pcrPid = pcrPid;
    reqParams.blocked = blocked;
    reqParams.muted = muted;
    reqParams.requestedGain = requestedGain;
    reqParams.cci = cci;

    err = mpe_edCreateHandle(edListener, MPE_ED_QUEUE_NORMAL, NULL,
            MPE_ED_TERMINATION_EVCODE, MPE_EVENT_SHUTDOWN, &edHandle);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "ERROR in jniDecode: mpe_edCreateHandle() = %d\n", err);
    }
    else
    {
        mpe_MediaDecodeSession session;
        /* Start asynchronous play; if error, free ed handle. */
        err = mpe_mediaDecode(&reqParams, edHandle->eventQ, (void *) edHandle,
                &session);
        if (err != MPE_SUCCESS)
        {
            mpe_edDeleteHandle(edHandle);
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                    "ERROR in jni_Decode: mpe_mediaDecode() = %d\n", err);
        }
        else
        {
            /* Store the returned session handle into the return array. */
            jint sessionHandle = (jint) session;
            (*env)->SetIntArrayRegion(env, sessionHandleArray, 0, 1,
                    &sessionHandle);
            (*env)->SetFloatArrayRegion(env, gainArray, 0, 1, &reqParams.actualGain);
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                    "jniDecode() sessionHandle = 0x%x\n", (int)sessionHandle);
        }

    }

    /* Free up temporary allocated structures. */
    if (mpePids != 0)
        mpe_memFreeP(MPE_MEM_TEMP, mpePids);
    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniStop(
        JNIEnv *env, jclass cls, jint session, jboolean holdFrame)
{
    mpe_Error err = MPE_SUCCESS;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jniStop(decoder=0x%X)\n", (int)session);
    uint32_t holdFrameMode = MPE_MEDIA_STOP_MODE_BLACK;
    if (holdFrame != JNI_FALSE)
    {
        holdFrameMode = MPE_MEDIA_STOP_MODE_HOLD_FRAME;
    }
    err = mpe_mediaStop((mpe_MediaDecodeSession)session, holdFrameMode);
    if (err != MPE_SUCCESS)
    {
        if (err == MPE_ERROR_MEDIA_RESOURCE_NOT_ACTIVE)
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "Media resource not active\n");
        else
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "mpe_mediaStop() = %d\n", err);
    }

    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniChangePids(
        JNIEnv *env, jclass cls, jint sessionHandle, jint pcrPid,
        jintArray pidArray, jshortArray typeArray)
{
    mpe_Error err = MPE_SUCCESS;
    MPE_UNUSED_PARAM(cls);

    jsize pidCount = 0;

    pidCount = (*env)->GetArrayLength(env, pidArray);
    mpe_MediaPID *mpePids = 0;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jniChangePids(): %d pids\n", (int)pidCount);
    if (pidCount > 0 && (err = mpe_memAllocP(MPE_MEM_TEMP, sizeof(mpe_MediaPID)
            * pidCount, (void**) &mpePids)) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "ERROR in jniChangePids: mpe_memAllocP() = %d\n", err);
        return err;
    }
    /* create array of mpe_MediaPIDs */
    jniutil_createPidArray(env, pidArray, typeArray, pidCount, mpePids);
    err = mpe_mediaChangePids((mpe_MediaDecodeSession) sessionHandle, pidCount,
            mpePids, pcrPid);

    /* Free up temporary allocated structures. */
    if (mpePids != 0)
        mpe_memFreeP(MPE_MEM_TEMP, mpePids);

    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniGetAspectRatio(
        JNIEnv *env, jclass cls, jint decoder, jintArray ratioArray)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_MediaAspectRatio ratio;
    jint iRatio = 0;

    MPE_UNUSED_PARAM(cls);

    err = mpe_mediaGetAspectRatio((mpe_DispDevice) decoder, &ratio);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_JNI,
                "ERROR in jniGetAspectRation: mpe_mediaGetAspectRatio() = %d\n",
                err);
    }
    else
    {
        iRatio = ratio;
        (*env)->SetIntArrayRegion(env, ratioArray, 0, 1, &iRatio);
    }
    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniGetAFD(
        JNIEnv *env, jclass cls, jint decoder, jintArray afdArray)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_MediaActiveFormatDescription afd;
    jint iAFD = 0;

    MPE_UNUSED_PARAM(cls);

    err = mpe_mediaGetAFD((mpe_DispDevice) decoder, &afd);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "ERROR in jniGetAFD: mpe_mediaGetAFD() = %d\n", err);
    }
    else
    {
        iAFD = afd;
        (*env)->SetIntArrayRegion(env, afdArray, 0, 1, &iAFD);
    }
    return err;
}

/*
 * Param scanModeArray - By convention, scanMode is element 0.  This are filled 
 * in by this routine.
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniGetVideoScanMode(
        JNIEnv *env, jclass cls, jint sessionHandle, jintArray scanModeArray)
{
    mpe_Error err = MPE_SUCCESS;

    mpe_MediaScanMode scanMode;

    err = mpe_mediaGetInputVideoScanMode((mpe_MediaDecodeSession) sessionHandle, &scanMode);
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
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniGetS3DConfiguration(
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

    err = mpe_mediaGet3DConfig((mpe_MediaDecodeSession) sessionHandle, &stereoscopicMode, &payloadType, payload, &payloadSz);
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
 * Param dfcArray - By convention, applicationDfc is element 0 and platformDfc
 * is element 1 in the dfcArray.  These are filled in by this routine.
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniGetDFC(
        JNIEnv *env, jclass cls, jint decoder, jintArray dfcArray)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_DispDfcAction applicationDfc;
    mpe_DispDfcAction platformDfc;
    jint iDFC = 0;

    MPE_UNUSED_PARAM(cls);

    err = mpe_dispGetDFC((mpe_DispDevice) decoder, &applicationDfc,
            &platformDfc);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "MediaAPIImpl: jniGetDFC(%d, %d)\n",
            applicationDfc, platformDfc);

    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "ERROR in jni_GetDFC: mpe_dispGetDFC() = %d\n", err);
    }
    else
    {
        // fill in the DFC return array, element 0 is the applicationDfc
        // element 1 is the platformDfc
        iDFC = applicationDfc;
        (*env)->SetIntArrayRegion(env, dfcArray, 0, 1, &iDFC);

        iDFC = platformDfc;
        (*env)->SetIntArrayRegion(env, dfcArray, 1, 1, &iDFC);
    }
    return err;
}

/**
 * TODO, TODO_DS:  logging, javadoc
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniGetSupportedDFCCount(
        JNIEnv *env, jclass cls, jint decoder, jintArray countArray)
{
    mpe_Error err = MPE_SUCCESS;
    jint count = 0;

    MPE_UNUSED_PARAM(cls);

    err = mpe_dispGetSupportedDFCCount((mpe_DispDevice) decoder,
            (uint32_t*) &count);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "MediaAPIImpl: jniGetSupportedDFCCount count = %d\n", (int)count);

    (*env)->SetIntArrayRegion(env, countArray, 0, 1, &count);

    return err;
}

/*
 * TODO, TODO_DS:  Fill out method comments
 * JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniGetSupportedDFCs
 (JNIEnv *, jclass, jint, jintArray);
 *
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniGetSupportedDFCs(
        JNIEnv *env, jclass cls, jint decoder, jintArray dfcArray)
{
    mpe_Error err = MPE_SUCCESS;
    jint* supportedDfcs;
    jint len = 0;
    //jint                idx;
    //jint                buf[] = { 0,1,2 };

    MPE_UNUSED_PARAM(cls);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "MediaAPIImpl: jniGetSupportedDFCs\n");

    err = mpe_dispGetSupportedDFCCount((mpe_DispDevice) decoder,
            (uint32_t*) &len);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_JNI,
                "ERROR in jniGetSupportedDFCs: mpe_dispGetSupportedDFCCount() = %x\n",
                err);
        return err;
    }

    if (len == 0)
    {
        return MPE_SUCCESS;
    }

    err = mpe_dispGetSupportedDFCs((mpe_DispDevice) decoder,
            (mpe_DispDfcAction**) &supportedDfcs);

    if (err != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_JNI,
                "ERROR in jniGetSupportedDFCs: mpe_dispGetSupportedDFCs() = %x\n",
                err);
    }
    else
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "MediaAPIImpl: jniGetSupportedDFCs\n");
        (*env)->SetIntArrayRegion(env, dfcArray, 0, len, supportedDfcs);
    }

    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniCheckDFC(
        JNIEnv *env, jclass cls, jint decoder, jint dfc)
{

    mpe_Error err;
    mpe_DispDfcAction d;

    MPE_UNUSED_PARAM(env);

    d = (mpe_DispDfcAction) dfc;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "MediaAPIImpl: jniCheckDFC(0x%X)\n", d);

    err = mpe_dispCheckDFC((mpe_DispDevice) decoder, d);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "ERROR in jniCheckDFC: mpe_dispCheckDFC() = %d\n", err);
    }
    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniSetDFC(
        JNIEnv *env, jclass cls, jint decoder, jint dfc)
{

    mpe_Error err;
    mpe_DispDfcAction d;

    MPE_UNUSED_PARAM(env);

    d = (mpe_DispDfcAction) dfc;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "MediaAPIImpl: jniSetDFC(0x%X)\n", d);

    err = mpe_dispSetDFC((mpe_DispDevice) decoder, d);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "ERROR in jniSetDFC: mpe_dispSetDFC() = %d\n", err);
    }
    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniFreeze(
        JNIEnv *env, jclass cls, jint decoder)
{
    mpe_Error err = MPE_SUCCESS;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jniFreeze(decoder=0x%X)\n", (int)decoder);

    err = mpe_mediaFreeze((mpe_DispDevice) decoder);

    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "ERROR in jniFreeze: mpe_mediaFreeze() = %d\n", err);
    }

    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniResume(
        JNIEnv *env, jclass cls, jint decoder)
{
    mpe_Error err = MPE_SUCCESS;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jniResume(decoder=0x%X)\n", (int)decoder);

    err = mpe_mediaResume((mpe_DispDevice) decoder);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "ERROR in jniResume: mpe_mediaResume() = %d\n", err);
    }

    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniSupportsComponentVideo(
        JNIEnv *env, jclass cls, jint decoder,
        jbooleanArray supportsComponentVideoArray)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_MediaPositioningCapabilities mpcdummy = 0;
    float *fdummy = 0;
    mpe_Bool bdummy;
    mpe_Bool supportsComponent;
    jboolean bSupportsComponent;

    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "jniSupportsComponentVideo(decoder=0x%X)\n", (int)decoder);

    /* Call MPE to get scaling information for target display device (decoder). */
    err = mpe_mediaGetScaling((mpe_DispDevice) decoder, &mpcdummy, &fdummy,
            &fdummy, &bdummy, &bdummy, &bdummy, &supportsComponent);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_JNI,
                "ERROR in jniSupportsComponentVideo: mpe_mediaGetScaling() = %d\n",
                err);
    }
    else
    {
        bSupportsComponent = supportsComponent;
        (*env)->SetBooleanArrayRegion(env, supportsComponentVideoArray, 0, 1,
                &bSupportsComponent);
    }

    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniGetCCState(
        JNIEnv *env, jclass cls, jintArray stateArray)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_CcState state = MPE_CC_STATE_OFF;
    jint iState = 0;

    MPE_UNUSED_PARAM(cls);

    err = mpe_ccGetClosedCaptioning(&state);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "ERROR in jniGetCCState: mpe_ccGetClosedCaptioning() = %d\n",
                err);
    }
    else
    {
        iState = state;
        (*env)->SetIntArrayRegion(env, stateArray, 0, 1, &iState);
    }
    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniSetCCState(
        JNIEnv *env, jclass cls, jint state)
{

    mpe_Error err;
    mpe_CcState s;

    MPE_UNUSED_PARAM(env);

    s = (mpe_CcState) state;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "MediaAPIImpl: jniSetCCState(0x%X)\n",
            s);

    err = mpe_ccSetClosedCaptioning(s);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "ERROR in jniSetCCState: mpe_ccSetClosedCaptioning() = %d\n",
                err);
    }
    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniSetCCServiceNumbers(
        JNIEnv *env, jclass cls, jint analogService, jint digitalService)
{
    mpe_Error err;
    uint32_t analog_s = 0;
    uint32_t digital_s = 0;

#define NO_SERVICE (-1)

    MPE_UNUSED_PARAM(env);

    /* set the analog closed captioning service */

    switch (analogService)
    {
    case org_ocap_media_ClosedCaptioningControl_CC_ANALOG_SERVICE_CC1:
        analog_s = MPE_CC_ANALOG_SERVICE_CC1;
        break;
    case org_ocap_media_ClosedCaptioningControl_CC_ANALOG_SERVICE_CC2:
        analog_s = MPE_CC_ANALOG_SERVICE_CC2;
        break;
    case org_ocap_media_ClosedCaptioningControl_CC_ANALOG_SERVICE_CC3:
        analog_s = MPE_CC_ANALOG_SERVICE_CC3;
        break;
    case org_ocap_media_ClosedCaptioningControl_CC_ANALOG_SERVICE_CC4:
        analog_s = MPE_CC_ANALOG_SERVICE_CC4;
        break;
    case org_ocap_media_ClosedCaptioningControl_CC_ANALOG_SERVICE_T1:
        analog_s = MPE_CC_ANALOG_SERVICE_T1;
        break;
    case org_ocap_media_ClosedCaptioningControl_CC_ANALOG_SERVICE_T2:
        analog_s = MPE_CC_ANALOG_SERVICE_T2;
        break;
    case org_ocap_media_ClosedCaptioningControl_CC_ANALOG_SERVICE_T3:
        analog_s = MPE_CC_ANALOG_SERVICE_T3;
        break;
    case org_ocap_media_ClosedCaptioningControl_CC_ANALOG_SERVICE_T4:
        analog_s = MPE_CC_ANALOG_SERVICE_T4;
        break;
    case NO_SERVICE:
        analog_s = MPE_CC_ANALOG_SERVICE_NONE;
        break;
    default:
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "jniSetCCServiceNumbers(): Illegal Analog CC service\n");
        return MPE_CC_ERROR_INVALID_PARAM;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "jniSetCCServiceNumbers(): set analog service # 0%d\n", analog_s);

    err = mpe_ccSetAnalogServices(analog_s);

    if (err != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_JNI,
                "ERROR in jniSetCCServiceNumbers: mpe_ccSetAnalogServices() = %d\n",
                err);
        return err;
    }

    /* set the digital closed captioning service */

    if (digitalService == NO_SERVICE)
    {
        digital_s = MPE_CC_DIGITAL_SERVICE_NONE;
    }
    else
    {
        digital_s = (uint32_t) digitalService;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "jniSetCCServiceNumbers(): set digital service (%d)\n", digital_s);

    err = mpe_ccSetDigitalServices(digital_s);

    if (err != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_JNI,
                "ERROR in jniSetCCServiceNumbers: mpe_ccSetDigitalServices() = %d\n",
                err);
        return err;
    }

    return MPE_SUCCESS;
}

JNIEXPORT jintArray JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniGetSupportedServiceNumbers(
        JNIEnv *env, jclass cls)
{
    mpe_Error err;
    uint32_t *HostArray;
    uint32_t HostArraySz;
    jintArray jArray;
    jint *cArray;
    uint32_t i;

    JNI_UNUSED(cls);

    /* Call MPE to get the count of platform-supported closed captioning services (analog & digital) */
    err = mpe_ccGetSupportedServiceNumbersCount(&HostArraySz);
    if (err != MPE_SUCCESS)
    {
        return NULL;
    }

    err = mpe_memAllocP(MPE_MEM_CC, sizeof(uint32_t)*HostArraySz,(void**)&HostArray);
    if (err != MPE_SUCCESS)
    {
        return NULL;
    }

    /* Call MPE to get the platform-supported closed captioning services (analog & digital) */
    err = mpe_ccGetSupportedServiceNumbers(HostArray, &HostArraySz);
    if (err != MPE_SUCCESS)
    {
        return NULL;
    }

    /* Allocate returnable Java array */
    jArray = (*env)->NewIntArray(env, HostArraySz);
    cArray = (*env)->GetIntArrayElements(env, jArray, NULL);

    /* Copy Host array into Java array */
    for (i = 0; i < HostArraySz; i++)
    {
        cArray[i] = (jint) HostArray[i];
    }

    /* Release lock on the Java array */
    (*env)->ReleaseIntArrayElements(env, jArray, cArray, 0);

    /* Free up temporary allocated structures. */
    if (HostArray != 0)
    {
        mpe_memFreeP(MPE_MEM_CC, HostArray);
    }

    /* Return Java array */
    return jArray;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniGetVideoInputSize(
        JNIEnv *env, jclass cls, jint decoderId, jobject jSource)
{
    mpe_Error retCode;
    mpe_GfxDimensions dim;
    MPE_UNUSED_PARAM(cls);

    retCode = mpe_mediaGetInputVideoSize((mpe_DispDevice) decoderId, &dim);
    if (retCode == MPE_SUCCESS)
    {
        jniutil_setDimension(env, jSource, dim.width, dim.height);
    }

    return retCode;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniSupportsClipping(
        JNIEnv *env, jclass cls, jint decoder,
        jbooleanArray supportsClippingArray)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_Bool supportsClipping;
    jboolean bSupportsClipping;

    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jniSupportsClipping(decoder=0x%X)\n",
            (int)decoder);

    /* Call MPE to get scaling information for target display device (decoder). */
    err = mpe_mediaGetScaling((mpe_DispDevice) decoder, NULL, NULL, NULL, NULL,
            NULL, &supportsClipping, NULL);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "ERROR in jniSupportsClipping: mpe_mediaGetScaling() = %d\n",
                err);
    }
    else
    {
        bSupportsClipping = supportsClipping;
        (*env)->SetBooleanArrayRegion(env, supportsClippingArray, 0, 1,
                &bSupportsClipping);
    }

    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniGetPositioningCapability(
        JNIEnv *env, jclass cls, jint decoder, jbyteArray posCapsArray)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_MediaPositioningCapabilities posCaps;
    jbyte jposCaps = 0;
    //  uint8_t pcaps = 0;

    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "jniGetPositioningCapability(decoder=0x%X)\n", (int)decoder);

    /* Call MPE to get positioning information for target display device (decoder). */
    err = mpe_mediaGetScaling((mpe_DispDevice) decoder, &posCaps, NULL, NULL,
            NULL, NULL, NULL, NULL);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_JNI,
                "ERROR in jniGetPositioningCapability: mpe_mediaGetScaling() = %d\n",
                err);
    }
    else
    {
        jposCaps = (jbyte) posCaps;
        (*env)->SetByteArrayRegion(env, posCapsArray, 0, 1, &jposCaps);
    }

    return err;
}

JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniGetScalingCaps(
        JNIEnv *env, jclass cls, jint decoder, jobject jScalingCaps)
{
    mpe_Error err = MPE_SUCCESS;
    float *horiz = NULL;
    float *vert = NULL;
    mpe_Bool isHArb = TRUE;
    mpe_Bool isVArb = TRUE;
    jfloatArray jfaHoriz;
    jfloatArray jfaVert;
    jclass clsCaps = NULL;

    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jniGetScalingCaps(decoder=0x%X)\n",
            (int)decoder);

    clsCaps = (*env)->GetObjectClass(env, jScalingCaps);
    if (clsCaps == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "ERROR: jniGetScalingCaps failed to retrieve clsCaps\n");
        return MPE_ENODATA;
    }

    /* Call MPE to get scaling information for target display device (decoder). */
    /* Get horizontal arbitrary scaling. */
    err = mpe_mediaGetScaling((mpe_DispDevice) decoder, NULL, &horiz, // horiz
            &vert,// vert
            &isHArb,// hrange
            &isVArb,// vrange
            NULL, NULL);
    if (err == MPE_SUCCESS)
    {
        int size = 0;
        jmethodID mid;

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "mpe_mediaGetScaling returned isHArb - %d  isVArb %d\n",
                isHArb, isVArb);

        // set flag denoting arbitrary scaling support.
        mid = (*env)->GetMethodID(env, clsCaps, "setIsArbitraryFlag", "(Z)V");
        if (mid != NULL)
        {
            (*env)->CallVoidMethod(env, jScalingCaps, mid, isHArb);
        }
        else
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_JNI,
                    "ERROR: jniGetScalingCaps could not call ScalingCaps::setIsArbitraryFlag - mid = %d\n",
                    (int) mid);
            return MPE_ENODATA;
        }

        /* For the Horiz scaling factors, create new jni array, copy native array
         and finally invoke java method.*/
        while (horiz[size] != -1)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                    "\tHoriz Scale Factor - %d is %f\n", size, horiz[size]);
            size++;
        }

        jfaHoriz = (*env)->NewFloatArray(env, size);
        if (jfaHoriz == NULL)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                    "ERROR: jniGetScalingCaps failed to allocate float array\n");
            return MPE_ENODATA;
        }

        // copy array
        (*env)->SetFloatArrayRegion(env, jfaHoriz, (jsize) 0, (jsize)(size),
                horiz);

        // Get and invoke method.
        mid = (*env)->GetMethodID(env, clsCaps, "setHorizCaps", "([F)V");
        if (mid != NULL)
        {
            (*env)->CallVoidMethod(env, jScalingCaps, mid, jfaHoriz);
        }
        else
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_JNI,
                    "ERROR: jniGetScalingCaps could not call ScalingCaps::SetHorizCaps - methodID = %d\n",
                    (int) mid);
            return MPE_ENODATA;
        }

        // Ditto for vertical caps
        size = 0;
        while (vert[size] != -1)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                    "\tVertical Scale Factor - %d is %f\n", size, vert[size]);
            size++;
        }

        jfaVert = (*env)->NewFloatArray(env, size);
        if (jfaVert == NULL)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                    "ERROR: jniGetScalingCaps failed to allocate float array\n");
            return MPE_ENODATA;
        }

        (*env)->SetFloatArrayRegion(env, jfaVert, (jsize) 0, (jsize)(size),
                vert);

        mid = (*env)->GetMethodID(env, clsCaps, "setVertCaps", "([F)V");
        if (mid != NULL)
        {
            (*env)->CallVoidMethod(env, jScalingCaps, mid, jfaVert);
        }
        else
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_JNI,
                    "ERROR: jniGetScalingCaps could not call ScalingCaps::SetVertCaps - mid = %d\n",
                    (int) mid);
            return MPE_ENODATA;
        }
    }
    else
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_JNI,
                "ERROR in jniGetScalingCaps: mpe_mediaGetScaling returned = %d\n",
                err);
    }

    return err;
}

/**
 * Create a new drip feed session and return the session to Java.
 *
 * @param videoDevice the video device on which to display the MPEG-2 frame
 * @param sessionArray the returned decode session upon success
 * @return a code indicating success or failure - 0 for success
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniDripFeedStart(
        JNIEnv *env, jclass cls, jobject dripFeedParams, jintArray sessionArray)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_EdEventInfo *edHandle;
    mpe_MediaDripFeedRequestParams reqParams;
    mpe_MediaDecodeSession session;

    jobject edListener = 0;
    jint videoDevice = 0;

    MPE_UNUSED_PARAM(cls);

    /* Unpack parameters from MediaDripFeedParams object. */
    edListener = (*env)->GetObjectField(env, dripFeedParams,
            jniutil_CachedIds.MediaDripFeedParams_listener);
    videoDevice = (*env)->GetIntField(env, dripFeedParams,
            jniutil_CachedIds.MediaDripFeedParams_videoHandle);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jniDripFeedStart(videoDevice=0x%X)\n",
            (int)videoDevice);

    /* Initialize drip feed request parameters */
    reqParams.videoDevice = (mpe_DispDevice) videoDevice;

    /* create queue for async events passed back to Java */
    err = mpe_edCreateHandle(edListener, MPE_ED_QUEUE_NORMAL, NULL,
            MPE_ED_TERMINATION_EVCODE, MPE_EVENT_SHUTDOWN, &edHandle);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "ERROR in jniDripFeedStart - mpe_edCreateHandle() = %d\n", err);
    }
    else
    {
        /* Start drip feed play; if error, free ed handle. */
        err = mpe_mediaDripFeedStart(&reqParams, edHandle->eventQ,
                (void *) edHandle, &session);
        if (err != MPE_SUCCESS)
        {
            mpe_edDeleteHandle(edHandle);
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_JNI,
                    "ERROR in jniDripFeedStart: mpe_mediaDripFeedStart() = %d\n",
                    err);
        }
        else
        {
            jint dripFeed = (jint) session;
            (*env)->SetIntArrayRegion(env, sessionArray, 0, 1, &dripFeed);
        }

    }

    return err;
}

/**
 * Passes an MPEG-2 video frame to native for decode and display.
 *
 * @param decoder the video device on which to display the MPEG-2 frame
 * @param data the supposed MPEG2 I-frame or P-frame
 * @return a code indicating success or failure - 0 for success
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniDripFeedRenderFrame(
        JNIEnv *env, jclass cls, jint session, jbyteArray data)
{
    /* Get length of data */
    uint32_t size = (uint32_t)(*env)->GetArrayLength(env, data);
    /* Get data from byte array. */
    uint8_t* dripFeedFrame = (uint8_t*) (*env)->GetByteArrayElements(env, data,
            false);
    mpe_Error err = MPE_SUCCESS;

    MPE_UNUSED_PARAM(cls);

    /* Make sure data acquired ok. */
    if (NULL != dripFeedFrame)
    {
        /* Call MPE API to get drip feed data frame decoded and displayed. */
        err = mpe_mediaDripFeedRenderFrame((mpe_MediaDecodeSession) session,
                dripFeedFrame, size);

        if (err != MPE_SUCCESS)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_JNI,
                    "ERROR in jniDripFeedRenderFrame: mpe_mediaDripFeedRenderFrame() = %d\n",
                    err);
        }
        /* Release the data source buffer. */
        (*env)->ReleaseByteArrayElements(env, data, (jbyte*) dripFeedFrame, 0);
    }
    else
    {
        /* bad data parameter */
        err = MPE_EINVAL;
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "jniDripFeedRenderFrame(session=0x%X) result: %d\n", (int)session, err);

    return err;
}

/**
 * Stop a drip feed session previously start by dripFeedStart
 *
 * @param session the drip feed decode session to stop
 * @return a code indicating success or failure - 0 for success
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniDripFeedStop(
        JNIEnv *env, jclass cls, jint session)
{
    mpe_Error err = MPE_SUCCESS;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jniDripFeedStop(session=0x%X)\n",
            (int)session);

    err = mpe_mediaDripFeedStop((mpe_MediaDecodeSession) session);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "ERROR in jniDripFeedStop: mpe_mediaStop() = %d\n", err);
    }

    return err;
}

/**
 * Block / unblock the presentation (audio and video) for the specified decode session
 *
 * @param session - The media decode session to block / unblock
 * @param block - boolean indicating whether to block (true) or unblock (false)
 * @return a code indicating success or failure - 0 for success
 * Signature: (IZ)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniBlockPresentation(
        JNIEnv *env, jclass cls, jint session, jboolean block)
{
    mpe_Error err = MPE_SUCCESS;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jniBlockPresentation(session=0x%X)\n",
            (int)session);

    err = mpe_mediaBlockPresentation((mpe_MediaDecodeSession) session,
            (mpe_Bool) block);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_JNI,
                "ERROR in jniBlockPresentation: mpe_mediaBlockPresentation() = %d\n",
                err);
    }

    return err;
}

/**
 * Generate a system key event which appears to have originated in the platform.
 *
 * @param   type    key event type such as KEY_TYPED, KEY_PRESSED, etc.
 * @param   code    key event code such as VK_1, VK_ENTER, etc.
 * @param   return MPE_SUCCESS if no problems, otherwise appropriate error code
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_media_mpe_MediaAPIImpl_jniGeneratePlatformKeyEvent(
        JNIEnv *env, jclass cls, jint type, jint code)
{
    mpe_Error err = MPE_SUCCESS;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jniGeneratePlatformKeyEvent\n");

    err = mpe_gfxGeneratePlatformKeyEvent(type, code);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "ERROR in jniGeneratePlatformKeyEvent: mpe_generatePlatformKeyEvent() = %d\n",
                err);
    }

    return err;
}
