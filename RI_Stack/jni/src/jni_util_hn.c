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

#include "jni_util.h"
#include "jni_util_hn.h"

#include <inttypes.h> // for PRIx64
#include <string.h> // strcpy

#include <mpe_dbg.h>

/*****************************************************************************/
/***                        Common helper functions                        ***/
/*****************************************************************************/

static mpe_Error allocAndCopyStringFromObject(JNIEnv *env,
    jobject obj, jfieldID fid, char** targetNativeString)
{
    mpe_Error err = MPE_SUCCESS;

    jstring javaString = 0;
    jsize javaStrLen = 0;
    const char* nativeString = NULL;

    (*env)->ExceptionClear(env);

    javaString = (*env)->GetObjectField(env, obj, fid);
    if ((*env)->ExceptionOccurred(env))
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() exception ocurred "
                "while accessing string field\n", __FUNCTION__);
        err = MPE_EINVAL;
    }
    else if (javaString == NULL || (javaStrLen = (*env)->GetStringUTFLength(env, javaString)) == 0)
    {
        *targetNativeString = NULL;
    }
    else if ((nativeString = (*env)->GetStringUTFChars(env, javaString, NULL)) == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() exception ocurred "
                "while getting native string\n", __FUNCTION__);
        err = MPE_EINVAL;
    }
    else
    {
        if ((err = mpe_memAllocP(MPE_MEM_TEMP, javaStrLen + 1,
                (void **) targetNativeString)) != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() unable to allocate "
                    "memory for native string\n", __FUNCTION__);
        }
        else
        {
            strcpy(*targetNativeString, nativeString);
        }

        (*env)->ReleaseStringUTFChars(env, javaString, nativeString);
    }

    return err;
}

static mpe_Error copyStringFromObject(JNIEnv *env,
    jobject obj, jfieldID fid, char* targetNativeString, int stringLength)
{
    mpe_Error err = MPE_SUCCESS;

    jstring javaString = 0;
    jsize javaStrLen = 0;
    const char* nativeString = NULL;

    (*env)->ExceptionClear(env);

    javaString = (*env)->GetObjectField(env, obj, fid);
    if ((*env)->ExceptionOccurred(env))
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() exception occurred "
                "while accessing string field\n", __FUNCTION__);
        err = MPE_EINVAL;
    }
    else if (javaString == NULL || (javaStrLen = (*env)->GetStringUTFLength(env, javaString)) == 0)
    {
        targetNativeString[0] = '\0';
    }
    else if ((nativeString = (*env)->GetStringUTFChars(env, javaString, NULL)) == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() exception occurred "
                "while getting native string\n", __FUNCTION__);
        err = MPE_EINVAL;
    }
    else
    {
        strncpy(targetNativeString, nativeString, stringLength);

        (*env)->ReleaseStringUTFChars(env, javaString, nativeString);
    }

    return err;
}

static mpe_Error buildTransportCCIFromObject(JNIEnv *env, jobjectArray jCciDescriptors,
        uint32_t* cciDescSize, mpe_HnPlaybackTransportCCI** cciDescData)
{
    mpe_Error err = MPE_SUCCESS;

    if (jCciDescriptors == NULL)
    {
        *cciDescSize = 0;
        *cciDescData = NULL;
    }
    else
    {
        jsize numCCIs = (*env)->GetArrayLength(env, jCciDescriptors);
        if ((err = mpe_memAllocP(MPE_MEM_TEMP, numCCIs * sizeof(mpe_HnPlaybackTransportCCI),
                (void **) cciDescData)) != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() unable to allocate "
                    "memory for mpe_HnPlaybackTransportCCI\n", __FUNCTION__);
            err = MPE_ENOMEM;  
        }
        else
        {
            int i = 0;
            for (i = 0; i < numCCIs; i++)
            {
                jobject jcci = (*env)->GetObjectArrayElement(env, jCciDescriptors, i);
                if (jcci == NULL)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpe_HnPlaybackTransportCCI object "
                            " is null at index %d\n", __FUNCTION__, i);
                    if (mpe_memFreeP(MPE_MEM_TEMP, (void*) *cciDescData) != MPE_SUCCESS)
                    {
                        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to deallocate "
                                "mpe_HnPlaybackTransportCCI\n", __FUNCTION__);
                    }
                    err = MPE_EINVAL;
                    break;
                }
                else
                {
                    (*cciDescData)[i].pid = (uint16_t) (*env)->GetShortField(env, jcci,
                            jniutil_CachedIds.HNPlaybackCopyControlInfo_pid);
                    (*cciDescData)[i].isProgram = (*env)->GetBooleanField(env, jcci,
                            jniutil_CachedIds.HNPlaybackCopyControlInfo_isProgram);
                    (*cciDescData)[i].isAudio = (*env)->GetBooleanField(env, jcci,
                            jniutil_CachedIds.HNPlaybackCopyControlInfo_isAudio);
                    (*cciDescData)[i].cci = (uint8_t) (*env)->GetByteField(env, jcci,
                            jniutil_CachedIds.HNPlaybackCopyControlInfo_cci);
                }
            }
            *cciDescSize = numCCIs;
        }
    }

    return err;
}

mpe_Error buildContentTransformationFromObject(JNIEnv *env, jobject jTransformation,
                                                      mpe_hnContentTransformation** transformationPtr)
{
    mpe_Error err = MPE_SUCCESS;

    if (jTransformation == NULL)
    {
        *transformationPtr = NULL;
    }
    else
    {
        err = mpe_memAllocP( MPE_MEM_TEMP, sizeof(mpe_hnContentTransformation),
                             (void**) transformationPtr );
        if (err != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() getNativeContentTransformations(): Error %d allocating capabilities array (%d bytes)\n",
                    __FUNCTION__, err, sizeof(mpe_hnContentTransformation) );
        }
        else
        {
            (*transformationPtr)->id
                = (*env)->GetIntField( env, jTransformation,
                                       jniutil_CachedIds.NativeContentTransformation_id);

            if ( ( err = copyStringFromObject(env, jTransformation,
                    jniutil_CachedIds.NativeContentTransformation_sourceProfile,
                    (*transformationPtr)->sourceProfile, MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE) )
                 != MPE_SUCCESS )
            {
                return err;
            }

            if ( ( err = copyStringFromObject(env, jTransformation,
                    jniutil_CachedIds.NativeContentTransformation_transformedProfile,
                    (*transformationPtr)->transformedProfile, MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE) )
                 != MPE_SUCCESS )
            {
                return err;
            }

            (*transformationPtr)->bitrate
                = (*env)->GetIntField( env, jTransformation,
                                       jniutil_CachedIds.NativeContentTransformation_bitrate);
            (*transformationPtr)->width
                = (*env)->GetIntField( env, jTransformation,
                                       jniutil_CachedIds.NativeContentTransformation_width);
            (*transformationPtr)->height
                = (*env)->GetIntField( env, jTransformation,
                                       jniutil_CachedIds.NativeContentTransformation_height);
            (*transformationPtr)->progressive
                = (*env)->GetBooleanField( env, jTransformation,
                                       jniutil_CachedIds.NativeContentTransformation_progressive);

        }
    }

    return err;
}

/*****************************************************************************/
/***                        Server helper functions                        ***/
/*****************************************************************************/

static void deallocateServerStreamParams(mpe_HnStreamParamsMediaServerHttp *serverStreamParams)
{
    if (serverStreamParams == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() serverStreamParams is NULL\n", __FUNCTION__);
    }
    else
    {
        if (serverStreamParams->dlnaProfileId != NULL)
        {
            if (mpe_memFreeP(MPE_MEM_TEMP, (void*) serverStreamParams->dlnaProfileId) != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to deallocate "
                        "mpe_HnStreamParamsMediaServerHttp.profileId\n", __FUNCTION__);
            }
            serverStreamParams->dlnaProfileId = NULL;
        }

        if (serverStreamParams->mimeType != NULL)
        {
            if (mpe_memFreeP(MPE_MEM_TEMP, (void*) serverStreamParams->mimeType) != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to deallocate "
                        "mpe_HnStreamParamsMediaServerHttp.mimeType\n", __FUNCTION__);
            }
            serverStreamParams->mimeType = NULL;
        }
    }
}

static mpe_Error buildServerStreamParamsFromObject(JNIEnv *env,
    jobject jStreamParams, mpe_HnStreamParamsMediaServerHttp** serverStreamParams)
{
    mpe_Error err = MPE_SUCCESS;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s()\n", __FUNCTION__);

    if ((err = mpe_memAllocP(MPE_MEM_TEMP, sizeof (mpe_HnStreamParamsMediaServerHttp),
            ((void **) serverStreamParams))) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to allocate "
                "mpe_HnStreamParamsMediaServerHttp with error = %d\n", __FUNCTION__, err);
    }
    else
    {
        //
        // mpe_HnStreamParamsMediaServerHttp.connectionId
        //
        (*serverStreamParams)->connectionId = (uint32_t) (*env)->GetIntField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaServerHttp_connectionId);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() connectionId: %u\n",
                __FUNCTION__, (*serverStreamParams)->connectionId);

        //
        // mpe_HnStreamParamsMediaServerHttp.dlnaProfileId
        //
        if ((err = allocAndCopyStringFromObject(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaServerHttp_dlnaProfileId,
                &((*serverStreamParams)->dlnaProfileId))) != MPE_SUCCESS)
        {
            return err;
        }
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() dlnaProfileId: %s\n",
                __FUNCTION__, (*serverStreamParams)->dlnaProfileId);
        
        //
        // mpe_HnStreamParamsMediaServerHttp.mimeType
        //
        if ((err = allocAndCopyStringFromObject(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaServerHttp_mimeType,
                &((*serverStreamParams)->mimeType))) != MPE_SUCCESS)
        {
            deallocateServerStreamParams(*serverStreamParams);
            return err;
        }
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() mimeType: %s\n",
                __FUNCTION__, (*serverStreamParams)->mimeType);

        //
        // mpe_HnStreamParamsMediaServerHttp.mpeSocket
        //
        (*serverStreamParams)->socket = (mpe_Socket) (*env)->GetIntField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaServerHttp_mpeSocket);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() socket: 0x%X\n",
                __FUNCTION__, (*serverStreamParams)->socket);

        //
        // mpe_HnStreamParamsMediaServerHttp.chunkedEncodingMode
        //
        (*serverStreamParams)->chunkedEncodingMode = (uint32_t) (*env)->GetIntField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaServerHttp_chunkedEncodingMode);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() chunkedEncodingMode: %u\n",
                __FUNCTION__, (*serverStreamParams)->chunkedEncodingMode);

        //
        // mpe_HnStreamParamsMediaServerHttp.maxTrickModeBandwidth
        //
        (*serverStreamParams)->maxTrickModeBandwidth = (int64_t) (*env)->GetLongField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaServerHttp_maxTrickModeBandwidth);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() maxTrickModeBandwidth: %"PRId64"\n",
                __FUNCTION__, (*serverStreamParams)->maxTrickModeBandwidth);

        //
        // mpe_HnStreamParamsMediaServerHttp.currentDecodePTS
        //
        (*serverStreamParams)->currentDecodePTS = (int64_t) (*env)->GetLongField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaServerHttp_currentDecodePTS);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() currentDecodePTS: %"PRId64"\n",
                __FUNCTION__, (*serverStreamParams)->currentDecodePTS);

        //
        // mpe_HnStreamParamsMediaServerHttp.maxGOPsPerChunk
        //
        (*serverStreamParams)->maxGOPsPerChunk = (int8_t) (*env)->GetByteField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaServerHttp_maxGOPsPerChunk);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() maxGOPsPerChunk: %hhd\n",
                __FUNCTION__, (*serverStreamParams)->maxGOPsPerChunk);

        //
        // mpe_HnStreamParamsMediaServerHttp.maxFramesPerGOP
        //
        (*serverStreamParams)->maxFramesPerGOP = (int8_t) (*env)->GetByteField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaServerHttp_maxFramesPerGOP);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() maxFramesPerGOP: %hhd\n",
                __FUNCTION__, (*serverStreamParams)->maxFramesPerGOP);

        //
        // mpe_HnStreamParamsMediaServerHttp.useServerSidePacing
        //
        (*serverStreamParams)->useServerSidePacing = (*env)->GetBooleanField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaServerHttp_useServerSidePacing);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() useServerSidePacing: %u\n",
                __FUNCTION__, (*serverStreamParams)->useServerSidePacing);

        //
        // mpe_HnStreamParamsMediaServerHttp.frameTypesInTrickModes
        //
        (*serverStreamParams)->frameTypesInTrickModes = (uint32_t) (*env)->GetIntField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaServerHttp_frameTypesInTrickModes);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() frameTypesInTrickModes: %u\n",
                __FUNCTION__, (*serverStreamParams)->frameTypesInTrickModes);

        //
        // mpe_HnStreamParamsMediaServerHttp.connectionStallingTimeoutMS
        //
        (*serverStreamParams)->connectionStallingTimeoutMS = (uint32_t) (*env)->GetIntField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaServerHttp_connectionStallingTimeoutMS);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() connectionStallingTimeoutMS: %d\n",
                __FUNCTION__, (*serverStreamParams)->connectionStallingTimeoutMS);

        // set QOS
        if ((err = mpeos_socketSetDLNAQOS((*serverStreamParams)->socket,
            MPE_SOCKET_DLNA_QOS_2)) != MPE_SUCCESS)
        {
            // non fatal error,,IP will set to default which is still in spec
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_HN,
                "%s() - error failed to set QOS on socket err = \n", __FUNCTION__, err);
        }

    }

    return err;
}

/**
 * Create the C content description structure for recording content types
 * based on java content description object.
 *
 * @param env   JNI environment
 * @param jContentLocationDescriptionObject java content description object
 * @param contentDescription                created C content description structure based on content location type
 *
 * @return MPE_SUCCESS if no problems encountered, MPE_EINVAL for unsupported types
 */
static mpe_Error buildContentDescriptionMSV(JNIEnv *env, jobject jContentLocationDescriptionObject,
                                            void** contentDescription)
{
    mpe_Error err = MPE_SUCCESS;

    if (jContentLocationDescriptionObject == NULL)
    {
        *contentDescription = NULL;
    }
    else
    {
        mpe_HnStreamLocalSVContentDescription* contentDescriptionLSV = 0;
        jfieldID fid = 0;
        jstring currentString = 0;
        const char* contentIDCharArray = 0;
        uint32_t length = 0;
        jint volumeHandle = 0;

        MPE_LOG(MPE_LOG_DEBUG,MPE_MOD_JNI,
                "buildContentDescriptionMSV() - allocating content location for type: MPE_HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT\n");
        if ((err = mpe_memAllocP(MPE_MEM_TEMP, sizeof(mpe_HnStreamLocalSVContentDescription), contentDescription))
                != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                    "buildContentDescriptionMSV() - failed to alloc content location for type: MPE_HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT: error  = %d\n",err);
            return err;
        }

        contentDescriptionLSV = *contentDescription;

        /* Use LSV content description object to parse the description fields */
        fid = jniutil_CachedIds.HNStreamContentDescriptionLocalSV_contentName;
        currentString = (*env)->GetObjectField(env, jContentLocationDescriptionObject, fid);
        contentIDCharArray = (*env)->GetStringUTFChars(env, currentString, NULL);
        if (NULL == contentIDCharArray)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "buildContentDescriptionMSV() - contentIDCharArray  == NULL\n");
            err = MPE_EINVAL;
            goto FINAL;
        }
        else
        {
            length = (*env)->GetStringUTFLength(env, currentString);

            // Allocate memory to store content name for platform
            //            if((err = mpe_memAllocP(MPE_MEM_TEMP, length,
            if ((err = mpe_memAllocP(MPE_MEM_TEMP, length + 1,
                    (void**) &(contentDescriptionLSV->contentName)))
                    != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                        "buildContentDescriptionMSV() - failed to alloc content name: MPE_HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT: error  = %d\n", err);
                return err;
            }

            // Copy string into storage
            strcpy(contentDescriptionLSV->contentName, contentIDCharArray);
        }
        (*env)->ReleaseStringUTFChars(env, currentString, contentIDCharArray);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "buildContentDescriptionMSV() - msv content name string  = %s\n",
                contentDescriptionLSV->contentName);

        /* Get the content identifier string field */
        fid = jniutil_CachedIds.HNStreamContentDescriptionLocalSV_volumeHandle;
        volumeHandle = (*env)->GetIntField(env, jContentLocationDescriptionObject, fid);
        if (NON_SPECIFIED_INT == volumeHandle)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                    "buildContentDescriptionMSV() -  volumeHandle is invalid\n");
            err = MPE_EINVAL;
            goto FINAL;
        }
        else
        {
            contentDescriptionLSV->volumeHandle = (mpe_MediaVolume) volumeHandle;
            MPE_LOG(MPE_LOG_DEBUG,MPE_MOD_JNI,
                    "buildContentDescriptionMSV() -  volumeHandle = %x\n", (int)volumeHandle);
        }

        FINAL: if (err != MPE_SUCCESS && 0 != *contentDescription)
        {
            if ((err = mpe_memFreeP(MPE_MEM_TEMP, (void*)(*contentDescription))) != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                        "buildContentDescriptionMSV() - failed to free allocated memory -  err  = 0x%x\n",
                        err);
            }
            *contentDescription = NULL;
        }
    }

    return err;
}

/**
 * Create the C content description structure for local file type content
 * based on java content description object.
 *
 * @param env   JNI environment
 * @param jContentLocationDescriptionObject java content description object
 * @param contentDescription                created C content description structure based on content location type
 *
 * @return MPE_SUCCESS if no problems encountered, MPE_EINVAL for unsupported types
 */
static mpe_Error buildContentDescriptionApp(JNIEnv *env, jobject jContentLocationDescriptionObject,
                                            void** contentDescription)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_HnStreamAppContentDescription* contentDescriptionApp = 0;
    jfieldID fid = 0;
    jstring currentString = 0;
    const char* contentIDCharArray = 0;
    uint32_t length = 0;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "buildContentDescriptionApp() - allocating content location for type: MPE_HN_CONTENT_LOCATION_LOCAL_FILE_CONTENT\n");
    if ((err = mpe_memAllocP(MPE_MEM_TEMP,
            sizeof(mpe_HnStreamAppContentDescription), contentDescription))
            != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR,MPE_MOD_JNI,
                "buildContentDescriptionApp() - failed to alloc content location for type: MPE_HN_CONTENT_LOCATION_LOCAL_FILE_CONTENT: error  = %d\n", err);
        return err;
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "buildContentDescriptionApp() - getting id for content description\n");

    contentDescriptionApp = *contentDescription;

    /* Use the content description object to parse the description fields */
    /* Get the content name string field */
    fid = jniutil_CachedIds.HNStreamContentDescriptionApp_contentName;
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "buildContentDescriptionApp() - getting current string\n");
    currentString = (*env)->GetObjectField(env, jContentLocationDescriptionObject, fid);
    if (NULL == currentString)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "buildContentDescriptionApp() - current string == NULL\n");
        err = MPE_EINVAL;
        goto FINAL;
    }

    contentIDCharArray = (*env)->GetStringUTFChars(env, currentString, NULL);
    if (NULL == contentIDCharArray)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "buildContentDescriptionApp() - contentIDCharArray  == NULL\n");
        err = MPE_EINVAL;
        goto FINAL;
    }
    else
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "buildContentDescriptionApp() - getting length");
        length = (*env)->GetStringUTFLength(env, currentString);

        // Allocate memory to store content name for platform
        if ((err = mpe_memAllocP(MPE_MEM_TEMP, length + 1,
                (void**) &(contentDescriptionApp->contentName)))
                != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                    "buildContentDescriptionApp() - failed to alloc content name: MPE_HN_CONTENT_LOCATION_LOCAL_FILE_CONTENT: error  = %d\n", err);
            return err;
        }

        // Copy string into storage
        strcpy(contentDescriptionApp->contentName, contentIDCharArray);
    }
    (*env)->ReleaseStringUTFChars(env, currentString, contentIDCharArray);
    MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI,
            "buildContentDescriptionApp() - local file content name string  = %s\n",
            contentDescriptionApp->contentName);

    /* Get the content path string field */
    fid = jniutil_CachedIds.HNStreamContentDescriptionApp_contentPath;
    currentString = (*env)->GetObjectField(env, jContentLocationDescriptionObject, fid);
    contentIDCharArray = (*env)->GetStringUTFChars(env, currentString, NULL);
    if (NULL == contentIDCharArray)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "buildContentDescriptionApp() - contentIDCharArray  == NULL\n");
        err = MPE_EINVAL;
        goto FINAL;
    }
    else
    {
        length = (*env)->GetStringUTFLength(env, currentString);

        // Allocate memory to store content name for platform
        if ((err = mpe_memAllocP(MPE_MEM_TEMP, length + 1,
                (void**) &(contentDescriptionApp->pathName)))
                != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                    "buildContentDescriptionApp() - failed to alloc path name: MPE_HN_CONTENT_LOCATION_LOCAL_FILE_CONTENT: error  = %d\n", err);
            return err;
        }

        // Copy string into storage
        strcpy(contentDescriptionApp->pathName, contentIDCharArray);
    }
    (*env)->ReleaseStringUTFChars(env, currentString, contentIDCharArray);

    FINAL: if (err != MPE_SUCCESS && 0 != *contentDescription)
    {
        if ((err = mpe_memFreeP(MPE_MEM_TEMP, (void*)(*contentDescription))) != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                    "buildContentDescriptionApp() - failed to free allocated memory -  err  = 0x%x\n",
                    err);
        }
        *contentDescription = NULL;
        return err;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "buildContentDescriptionApp() - path name string  = %s\n",
            contentDescriptionApp->pathName);

    return err;
}

/**
 * Create the C content description structure for tsb type content
 * based on java content description object.
 *
 * @param env   JNI environment
 * @param jContentLocationDescriptionObject java content description object
 * @param contentDescription                created C content description structure based on content location type
 *
 * @return MPE_SUCCESS if no problems encountered
 */
static mpe_Error buildContentDescriptionTSB(JNIEnv *env, jobject jContentLocationDescriptionObject,
                                            void** contentDescription)
{
    mpe_Error err = MPE_SUCCESS;

    if (jContentLocationDescriptionObject == NULL)
    {
        *contentDescription = NULL;
    }
    else
    {
        mpe_HnStreamTSBContentDescription* contentDescriptionTSB = 0;
        jfieldID fid = 0;
        jint tsbHandle = 0;

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "buildContentDescriptionTSB() - allocating content location for type: MPE_HN_CONTENT_LOCATION_LOCAL_TSB\n");
        if ((err = mpe_memAllocP(MPE_MEM_TEMP,
                sizeof(mpe_HnStreamTSBContentDescription), contentDescription))
                != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                    "buildContentDescriptionTSB() - failed to alloc content location for type: MPE_HN_CONTENT_LOCATION_LOCAL_TSB: error  = %d\n", err);
            return err;
        }
        contentDescriptionTSB = *contentDescription;

        // Use the content description object and parse the description fields
        fid = jniutil_CachedIds.HNStreamContentDescriptionTSB_nativeTSBHandle;
        tsbHandle = (*env)->GetIntField(env, jContentLocationDescriptionObject, fid);
        contentDescriptionTSB->tsb = (mpe_DvrTsb)tsbHandle;

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "buildContentDescriptionTSB() - tsb handle  = %x\n", tsbHandle);
    }

    return err;
}

/**
 * Create the C content description structure for video device type content
 * based on java content description object.
 *
 * @param env   JNI environment
 * @param jContentLocationDescriptionObject java content description object
 * @param contentDescription                created C content description structure based on content location type
 *
 * @return MPE_SUCCESS if no problems encountered
 */
static mpe_Error buildContentDescriptionVideoDevice(JNIEnv *env, jobject jContentLocationDescriptionObject,
                                            void** contentDescription)
{
    mpe_Error err = MPE_SUCCESS;

    if (jContentLocationDescriptionObject == NULL)
    {
        *contentDescription = NULL;
    }
    else
    {
        mpe_HnStreamVideoDeviceContentDescription* contentDescriptionVideoDevice = 0;
        jfieldID fid = 0;
        jint vdHandle = 0;

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "buildContentDescriptionVideoDevice() - allocating content location for type: MPE_HN_CONTENT_LOCATION_LOCAL_VIDEO_DEVICE\n");
        if ((err = mpe_memAllocP(MPE_MEM_TEMP,
                sizeof(mpe_HnStreamVideoDeviceContentDescription), contentDescription))
                != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                    "buildContentDescriptionVideoDevice() - failed to alloc content location for type: MPE_HN_CONTENT_LOCATION_LOCAL_VIDEO_DEVICE: error  = %d\n", err);
            return err;
        }
        contentDescriptionVideoDevice = *contentDescription;

        // Use the content description object and parse the description fields
        fid = jniutil_CachedIds.HNStreamContentDescriptionVideoDevice_nativeVideoDeviceHandle;
        vdHandle = (*env)->GetIntField(env, jContentLocationDescriptionObject, fid);
        contentDescriptionVideoDevice->videoDevice = (mpe_DispDevice)vdHandle;

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "buildContentDescriptionVideoDevice() - video device handle = %x\n", vdHandle);
    }

    return err;
}

/**
 * Create the C content description structure for tuner content
 * based on java content description object.
 *
 * @param env   JNI environment
 * @param jContentLocationDescriptionObject java content description object
 * @param contentDescription                created C content description structure based on content location type
 *
 * @return MPE_SUCCESS if no problems encountered
 */
static mpe_Error buildContentDescriptionTuner(JNIEnv *env, jobject jContentLocationDescriptionObject,
                                            void** contentDescription)
{
    mpe_Error err = MPE_SUCCESS;

    if (jContentLocationDescriptionObject == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() contentLocationDescriptionObject is NULL\n", __FUNCTION__);
        *contentDescription = NULL;
    }
    else
    {
        mpe_HnStreamTunerContentDescription* contentDescriptionTuner = 0;
        jfieldID fid = 0;
        jint tunerId = 0;
        jint frequency = 0;        
        jshort ltsid = 0;
        jintArray pidArray = 0;
        jshortArray elemTypeArray = 0;
        jshortArray mediaTypeArray = 0;
        jsize pidCount = 0;
        mpe_HnPidInfo *hnPids = 0;

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                    "buildContentDescriptionTuner() - allocating content location for type: MPE_HN_CONTENT_LOCATION_LOCAL_TUNER\n");
        if ((err = mpe_memAllocP(MPE_MEM_TEMP,
                sizeof(mpe_HnStreamTunerContentDescription), contentDescription))
                != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                    "buildContentDescriptionTuner() - failed to alloc content location for type: MPE_HN_CONTENT_LOCATION_LOCAL_TUNER: error  = %d\n", err);
            return err;
        }
        contentDescriptionTuner = *contentDescription;

        // Use the content description object and parse the description fields
        fid = jniutil_CachedIds.HNStreamContentDescriptionTuner_tunerId;
        tunerId = (*env)->GetIntField(env, jContentLocationDescriptionObject, fid);
        contentDescriptionTuner->tunerId = (uint32_t)tunerId;

        fid = jniutil_CachedIds.HNStreamContentDescriptionTuner_frequency;
        frequency = (*env)->GetIntField(env, jContentLocationDescriptionObject, fid);
        contentDescriptionTuner->freq = (uint32_t)frequency;

        fid = jniutil_CachedIds.HNStreamContentDescriptionTuner_ltsid;
        ltsid = (*env)->GetShortField(env, jContentLocationDescriptionObject, fid);
        contentDescriptionTuner->ltsid = (uint8_t)ltsid;

        pidArray = (*env)->GetObjectField(env, jContentLocationDescriptionObject, jniutil_CachedIds.HNStreamContentDescriptionTuner_pids);
        elemTypeArray = (*env)->GetObjectField(env, jContentLocationDescriptionObject,jniutil_CachedIds.HNStreamContentDescriptionTuner_elemStreamTypes);
        mediaTypeArray = (*env)->GetObjectField(env, jContentLocationDescriptionObject,jniutil_CachedIds.HNStreamContentDescriptionTuner_mediaStreamTypes);
        /* Allocate PID array if serviceComponentImpls is not empty */
        pidCount = (*env)->GetArrayLength(env, pidArray);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "buildContentDescriptionTuner(): %d pids\n", (int)pidCount);
        if (pidCount > 0 && (err = mpe_memAllocP(MPE_MEM_TEMP, sizeof(mpe_HnPidInfo) * pidCount, (void**) &hnPids)) != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                    "ERROR in buildContentDescriptionTuner(): mpe_memAllocP() = %d\n", err);
            return err;
        }
        /* create array of mpe_HnPidInfos */
        jniutil_createHnPidArray(env, pidArray, elemTypeArray, mediaTypeArray, pidCount, hnPids);
        contentDescriptionTuner->pidCount = pidCount;
        contentDescriptionTuner->pids = hnPids;

        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI,
                "buildContentDescriptionTuner() - tuner id = %d, frequency = %d, ltsid = %d pidCount = %d\n", tunerId, frequency, ltsid, pidCount);
    }

    return err;
}

static void deallocateServerPlaybackParams(mpe_HnPlaybackParamsMediaServerHttp *serverPlaybackParams)
{
    if (serverPlaybackParams == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() serverPlaybackParams is NULL\n", __FUNCTION__);
    }
    else
    {
        if (serverPlaybackParams->contentDescription != NULL)
        {
            deallocateContentDescription(serverPlaybackParams->contentLocation,
                    serverPlaybackParams->contentDescription);
            serverPlaybackParams->contentDescription = NULL;
        }

        if (serverPlaybackParams->cciDescData != NULL)
        {
            if (mpe_memFreeP(MPE_MEM_TEMP, (void*) serverPlaybackParams->cciDescData) != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to deallocate "
                        "mpe_HnPlaybackParamsMediaServerHttp.cciDescData\n", __FUNCTION__);
            }
            serverPlaybackParams->cciDescData = NULL;
        }
    }
}

static mpe_Error buildServerPlaybackParamsFromObject(JNIEnv *env,
    jobject jPlaybackParams, mpe_HnPlaybackParamsMediaServerHttp** serverPlaybackParams)
{
    mpe_Error err = MPE_SUCCESS;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s()\n", __FUNCTION__);

    if ((err = mpe_memAllocP(MPE_MEM_TEMP, sizeof (mpe_HnPlaybackParamsMediaServerHttp),
            ((void **) serverPlaybackParams))) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to allocate "
                "mpe_HnPlaybackParamsMediaServerHttp with error = %d\n", __FUNCTION__, err);
    }
    else
    {
        jint   jContentLocationType = 0;
        jobject jContentDescription = 0;
        jobject     jCciDescriptors = 0;
        jobject     jTransformation = 0;

        //
        // mpe_HnPlaybackParamsMediaServerHttp.contentLocation
        //
        jContentLocationType = (*env)->GetIntField(env, jPlaybackParams,
                jniutil_CachedIds.HNPlaybackParamsMediaServerHttp_contentLocation);
        (*serverPlaybackParams)->contentLocation = (mpe_HnStreamContentLocation) jContentLocationType;
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() contentLocation: %u\n",
                __FUNCTION__, (*serverPlaybackParams)->contentLocation);

        //
        // mpe_HnPlaybackParamsMediaServerHttp.contentDescription
        //
        jContentDescription = (*env)->GetObjectField(env, jPlaybackParams,
                jniutil_CachedIds.HNPlaybackParamsMediaServerHttp_contentDescription);
        if ((err = buildContentDescriptionFromObject(env, jContentLocationType, jContentDescription,
                &(*serverPlaybackParams)->contentDescription)) != MPE_SUCCESS)
        {
            return err;
        }
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() contentDescription: 0x%X\n",
                __FUNCTION__, (*serverPlaybackParams)->contentDescription);

        //
        // mpe_HnPlaybackParamsMediaServerHttp.playspeedRate
        //
        (*serverPlaybackParams)->playspeedRate = (float) (*env)->GetFloatField(env, jPlaybackParams,
                jniutil_CachedIds.HNPlaybackParamsMediaServerHttp_playspeedRate);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() playspeedRate: %f\n",
                __FUNCTION__, (*serverPlaybackParams)->playspeedRate);

        //
        // mpe_HnPlaybackParamsMediaServerHttp.useTimeOffset
        //
        (*serverPlaybackParams)->useTimeOffset = (mpe_Bool) (*env)->GetBooleanField( env, jPlaybackParams,
                jniutil_CachedIds.HNPlaybackParamsMediaServerHttp_useTimeOffset);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() useTimeOffset: %d\n",
                __FUNCTION__, (*serverPlaybackParams)->useTimeOffset);

        //
        // mpe_HnPlaybackParamsMediaServerHttp.startBytePosition
        //
        (*serverPlaybackParams)->startBytePosition = (int64_t) (*env)->GetLongField(env, jPlaybackParams,
                jniutil_CachedIds.HNPlaybackParamsMediaServerHttp_startBytePosition);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() startBytePosition: %"PRId64"\n",
                __FUNCTION__, (*serverPlaybackParams)->startBytePosition);

        //
        // mpe_HnPlaybackParamsMediaServerHttp.endBytePosition
        //
        (*serverPlaybackParams)->endBytePosition = (int64_t) (*env)->GetLongField(env, jPlaybackParams,
                jniutil_CachedIds.HNPlaybackParamsMediaServerHttp_endBytePosition);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() endBytePosition: %"PRId64"\n",
                __FUNCTION__, (*serverPlaybackParams)->endBytePosition);

        //
        // mpe_HnPlaybackParamsMediaServerHttp.startTimePosition
        //
        (*serverPlaybackParams)->startTimePosition = (int64_t) (*env)->GetLongField(env, jPlaybackParams,
                jniutil_CachedIds.HNPlaybackParamsMediaServerHttp_startTimePosition);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() startTimePosition: %"PRId64"\n",
                __FUNCTION__, (*serverPlaybackParams)->startTimePosition);

        //
        // mpe_HnPlaybackParamsMediaServerHttp.endTimePosition
        //
        (*serverPlaybackParams)->endTimePosition = (int64_t) (*env)->GetLongField(env, jPlaybackParams,
                jniutil_CachedIds.HNPlaybackParamsMediaServerHttp_endTimePosition);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() endTimePosition: %"PRId64"\n",
                __FUNCTION__, (*serverPlaybackParams)->endTimePosition);

        //
        // mpe_HnPlaybackParamsMediaServerHttp.cciDescSize
        // mpe_HnPlaybackParamsMediaServerHttp.cciDescData
        //
        jCciDescriptors = (*env)->GetObjectField(env, jPlaybackParams, 
                jniutil_CachedIds.HNPlaybackParamsMediaServerHttp_cciDescriptors);
        if ((err = buildTransportCCIFromObject(env, jCciDescriptors,
                &(*serverPlaybackParams)->cciDescSize, &(*serverPlaybackParams)->cciDescData)) != MPE_SUCCESS)
        {
            deallocateServerPlaybackParams(*serverPlaybackParams);
            return err;
        }
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() cciDescSize: %u\n",
                __FUNCTION__, (*serverPlaybackParams)->cciDescSize);

        //
        // mpe_HnPlaybackParamsMediaServerHttp.transformation
        //
        jTransformation = (*env)->GetObjectField(env, jPlaybackParams,
                jniutil_CachedIds.HNPlaybackParamsMediaServerHttp_transformation);
        if ((err = buildContentTransformationFromObject(env, jTransformation,
                &((*serverPlaybackParams)->transformation))) != MPE_SUCCESS)
        {
            deallocateServerPlaybackParams(*serverPlaybackParams);
            return err;
        }
        if (((*serverPlaybackParams)->transformation) != NULL)
        {
            mpe_hnContentTransformation * transformation = ((*serverPlaybackParams)->transformation);
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() transformation: id %d, source %s, dest %s, width %d, height %d, bitrate %d, progressive %d\n",
                    __FUNCTION__, transformation->id, transformation->sourceProfile,
                    transformation->transformedProfile, transformation->width,
                    transformation->height, transformation->bitrate, transformation->progressive );
        }
    }

    return err;
}

/*****************************************************************************/
/***                        Player helper functions                        ***/
/*****************************************************************************/

static void deallocatePlayerStreamParams(mpe_HnStreamParamsMediaPlayerHttp *playerStreamParams)
{
    if (playerStreamParams == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() playerStreamParams is NULL\n", __FUNCTION__);
    }
    else
    {
        if (playerStreamParams->uri != NULL)
        {
            if (mpe_memFreeP(MPE_MEM_TEMP, (void*) playerStreamParams->uri) != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to deallocate "
                        "mpe_HnStreamParamsMediaPlayerHttp.uri\n", __FUNCTION__);
            }
            playerStreamParams->uri = NULL;
        }

        // *TODO* - reverted MPEOS change
        //if (playerStreamParams->protocolInfo.fourthField.pn_param.dlnaProfileId != NULL)
        if (playerStreamParams->dlnaProfileId != NULL)
        {
            // *TODO* - reverted MPEOS change
            //if (mpe_memFreeP(MPE_MEM_TEMP, (void*) playerStreamParams->protocolInfo.fourthField.pn_param.dlnaProfileId)
            //        != MPE_SUCCESS)
            if (mpe_memFreeP(MPE_MEM_TEMP, (void*) playerStreamParams->dlnaProfileId) != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to deallocate dlnaProfileId\n", __FUNCTION__);
            }
            // *TODO* - reverted MPEOS change
            //playerStreamParams->protocolInfo.fourthField.pn_param.dlnaProfileId = NULL;
            playerStreamParams->dlnaProfileId = NULL;

        }

        // *TODO* - reverted MPEOS change
        //if (playerStreamParams->protocolInfo.mimeType != NULL)
        if (playerStreamParams->mimeType != NULL)
        {
            // *TODO* - reverted MPEOS change
            //if (mpe_memFreeP(MPE_MEM_TEMP, (void*) playerStreamParams->protocolInfo.mimeType) != MPE_SUCCESS)
            if (mpe_memFreeP(MPE_MEM_TEMP, (void*) playerStreamParams->mimeType) != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to deallocate mimeType\n", __FUNCTION__);
            }
            // *TODO* - reverted MPEOS change
            //playerStreamParams->protocolInfo.mimeType = NULL;
            playerStreamParams->mimeType = NULL;
        }

        if (playerStreamParams->host != NULL)
        {
            if (mpe_memFreeP(MPE_MEM_TEMP, (void*) playerStreamParams->host) != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to deallocate "
                        "mpe_HnStreamParamsMediaPlayerHttp.host\n", __FUNCTION__);
            }
            playerStreamParams->host = NULL;
        }

        // *TODO* - reverted MPEOS change
        //if (playerStreamParams->protocolInfo.dtcpHost != NULL)
        if (playerStreamParams->dtcp_host != NULL)
        {
            // *TODO* - reverted MPEOS change
            //if (mpe_memFreeP(MPE_MEM_TEMP, (void*) playerStreamParams->protocolInfo.dtcpHost) != MPE_SUCCESS)
            if (mpe_memFreeP(MPE_MEM_TEMP, (void*) playerStreamParams->dtcp_host) != MPE_SUCCESS)

            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to deallocate dtcp host\n", __FUNCTION__);
            }
            // *TODO* - reverted MPEOS change
            //playerStreamParams->protocolInfo.dtcpHost = NULL;
            playerStreamParams->dtcp_host = NULL;
        }

        // *TODO* - reverted MPEOS change
        /*
        if (playerStreamParams->protocolInfo.fourthField.ps_param.playspeeds != NULL)
        {
            if (mpe_memFreeP(MPE_MEM_TEMP, (void*) playerStreamParams->protocolInfo.fourthField.ps_param.playspeeds)
                    != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to deallocate "
                        "mpe_HnStreamParamsMediaPlayerHttp.protocolInfo.fourthField.ps_param.playspeeds\n",
                        __FUNCTION__);
            }
            playerStreamParams->protocolInfo.fourthField.ps_param.playspeeds = NULL;
        }
        */
    }
}

static mpe_Error buildPlayerStreamParamsFromObject(JNIEnv *env,
    jobject jStreamParams, mpe_HnStreamParamsMediaPlayerHttp** playerStreamParams)
{
    mpe_Error err = MPE_SUCCESS;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s()\n", __FUNCTION__);

    if ((err = mpe_memAllocP(MPE_MEM_TEMP, sizeof (mpe_HnStreamParamsMediaPlayerHttp),
            ((void **) playerStreamParams))) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to allocate "
                "mpe_HnStreamParamsMediaPlayerHttp with error = %d\n", __FUNCTION__, err);
    }
    else
    {
        //
        // mpe_HnStreamParamsMediaPlayerHttp.connectionId
        //
        (*playerStreamParams)->connectionId = (uint32_t) (*env)->GetIntField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaPlayerHttp_connectionId);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() connectionId: %u\n",
                __FUNCTION__, (*playerStreamParams)->connectionId);

        //
        // mpe_HnStreamParamsMediaPlayerHttp.uri
        //
        if ((err = allocAndCopyStringFromObject(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaPlayerHttp_uri,
                &((*playerStreamParams)->uri))) != MPE_SUCCESS)
        {
            deallocatePlayerStreamParams(*playerStreamParams);
            return err;
        }
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() uri: %s\n",
                __FUNCTION__, (*playerStreamParams)->uri);

        //
        // mpe_HnStreamParamsMediaPlayerHttp.protocolInfo.fourthField.pn_param.dlnaProfileId
        // mpe_HnStreamParamsMediaPlayerHttp.dlnaProfileId
        //
        if ((err = allocAndCopyStringFromObject(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaPlayerHttp_dlnaProfileId,
                &((*playerStreamParams)->dlnaProfileId))) != MPE_SUCCESS)
        // *TODO* - reverted MPEOS change
        //      &((*playerStreamParams)->protocolInfo.fourthField.pn_param.dlnaProfileId))) != MPE_SUCCESS)
        {
            deallocatePlayerStreamParams(*playerStreamParams);
            return err;
        }
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() dlnaProfileId: %s\n",
                __FUNCTION__, (*playerStreamParams)->dlnaProfileId);
        // *TODO* - reverted MPEOS change
        //        __FUNCTION__, (*playerStreamParams)->protocolInfo.fourthField.pn_param.dlnaProfileId);

        //
        // mpe_HnStreamParamsMediaPlayerHttp.protocolInfo.mimeType
        // mpe_HnStreamParamsMediaPlayerHttp.mimeType
        //
        if ((err = allocAndCopyStringFromObject(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaPlayerHttp_mimeType,
                &((*playerStreamParams)->mimeType))) != MPE_SUCCESS)
        //      &((*playerStreamParams)->protocolInfo.mimeType))) != MPE_SUCCESS)
        // *TODO* - reverted MPEOS change
        {
            deallocatePlayerStreamParams(*playerStreamParams);
            return err;
        }
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() mimeType: %s\n",
                __FUNCTION__, (*playerStreamParams)->mimeType);
        // *TODO* - reverted MPEOS change
        // __FUNCTION__, (*playerStreamParams)->protocolInfo.mimeType);

        //
        // mpe_HnStreamParamsMediaPlayerHttp.host
        //
        if ((err = allocAndCopyStringFromObject(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaPlayerHttp_host,
                &((*playerStreamParams)->host))) != MPE_SUCCESS)
        {
            deallocatePlayerStreamParams(*playerStreamParams);
            return err;
        }
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() host: %s\n",
                __FUNCTION__, (*playerStreamParams)->host);

        //
        // mpe_HnStreamParamsMediaPlayerHttp.port
        //
        (*playerStreamParams)->port = (uint32_t) (*env)->GetIntField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaPlayerHttp_port);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() port: %u\n",
                __FUNCTION__, (*playerStreamParams)->port);

        //
        // mpe_HnStreamParamsMediaPlayerHttp.dtcp_host
        // mpe_HnStreamParamsMediaPlayerHttp.protocolInfo.dtcpHost
        //
        if ((err = allocAndCopyStringFromObject(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaPlayerHttp_dtcpHost,
                &((*playerStreamParams)->dtcp_host))) != MPE_SUCCESS)
        // *TODO* - reverted MPEOS change
        //      &((*playerStreamParams)->protocolInfo.dtcpHost))) != MPE_SUCCESS)
        {
            deallocatePlayerStreamParams(*playerStreamParams);
            return err;
        }
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() dtcp_host: %s\n",
                __FUNCTION__, (*playerStreamParams)->dtcp_host);
        // *TODO* - reverted MPEOS change
        //      __FUNCTION__, (*playerStreamParams)->protocolInfo.dtcpHost);

        //
        // mpe_HnStreamParamsMediaPlayerHttp.protocolInfo.dtcpPort
        // mpe_HnStreamParamsMediaPlayerHttp.dtcp_port
        //
        // *TODO* - reverted MPEOS change
        //(*playerStreamParams)->protocolInfo.dtcpPort = (uint32_t) (*env)->GetIntField(env, jStreamParams,
        (*playerStreamParams)->dtcp_port = (uint32_t) (*env)->GetIntField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaPlayerHttp_dtcpPort);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() dtcp_port: %u\n",
                __FUNCTION__, (*playerStreamParams)->dtcp_port);
        // *TODO* - reverted MPEOS change
        //        __FUNCTION__, (*playerStreamParams)->protocolInfo.dtcpPort);

        // *TODO* - reverted MPEOS change
        /*
        //
        // mpe_HnStreamParamsMediaPlayerHttp.protocolInfo.fourthField.op_param.isTimeSeekSupported
        //
        (*playerStreamParams)->protocolInfo.fourthField.op_param.isTimeSeekSupported =
                (*env)->GetBooleanField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaPlayerHttp_isTimeSeekSupported);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() isTimeSeekSupported: %u\n", __FUNCTION__,
                (*playerStreamParams)->protocolInfo.fourthField.op_param.isTimeSeekSupported);

        //
        // mpe_HnStreamParamsMediaPlayerHttp.protocolInfo.fourthField.op_param.isRangeSupported
        //
        (*playerStreamParams)->protocolInfo.fourthField.op_param.isRangeSupported =
                (*env)->GetBooleanField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaPlayerHttp_isRangeSupported);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() isRangeSupported: %u\n", __FUNCTION__,
                (*playerStreamParams)->protocolInfo.fourthField.op_param.isRangeSupported);

        //
        // mpe_HnStreamParamsMediaPlayerHttp.protocolInfo.fourthField.flags_param.isSenderPaced
        //
        (*playerStreamParams)->protocolInfo.fourthField.flags_param.isSenderPaced =
                (*env)->GetBooleanField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaPlayerHttp_isSenderPaced);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() isSenderPaced: %u\n", __FUNCTION__,
                (*playerStreamParams)->protocolInfo.fourthField.flags_param.isSenderPaced);

        //
        // mpe_HnStreamParamsMediaPlayerHttp.protocolInfo.isLimitedTimeSeekSupported
        //
        (*playerStreamParams)->protocolInfo.fourthField.flags_param.isLimitedTimeSeekSupported =
                (*env)->GetBooleanField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaPlayerHttp_isLimitedTimeSeekSupported);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() isLimitedTimeSeekSupported: %u\n", __FUNCTION__,
                (*playerStreamParams)->protocolInfo.fourthField.flags_param.isLimitedTimeSeekSupported);

        //
        // mpe_HnStreamParamsMediaPlayerHttp.protocolInfo.fourthField.flags_param.isLimitedByteSeekSupported
        //
        (*playerStreamParams)->protocolInfo.fourthField.flags_param.isLimitedByteSeekSupported =
                (*env)->GetBooleanField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaPlayerHttp_isLimitedByteSeekSupported);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() isLimitedByteSeekSupported: %u\n", __FUNCTION__,
                (*playerStreamParams)->protocolInfo.fourthField.flags_param.isLimitedByteSeekSupported);

        //
        // mpe_HnStreamParamsMediaPlayerHttp.protocolInfo.fourthField.flags_param.isPlayContainer
        //
        (*playerStreamParams)->protocolInfo.fourthField.flags_param.isPlayContainer =
                (*env)->GetBooleanField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaPlayerHttp_isPlayContainer);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() isPlayContainer: %u\n", __FUNCTION__,
                (*playerStreamParams)->protocolInfo.fourthField.flags_param.isPlayContainer);

        //
        // mpe_HnStreamParamsMediaPlayerHttp.protocolInfo.fourthField.flags_param.isS0Increasing
        //
        (*playerStreamParams)->protocolInfo.fourthField.flags_param.isS0Increasing =
                (*env)->GetBooleanField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaPlayerHttp_isS0Increasing);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() isS0Increasing: %u\n", __FUNCTION__,
                (*playerStreamParams)->protocolInfo.fourthField.flags_param.isS0Increasing);

        //
        // mpe_HnStreamParamsMediaPlayerHttp.protocolInfo.fourthField.flags_param.isSnIncreasing
        //
        (*playerStreamParams)->protocolInfo.fourthField.flags_param.isSnIncreasing =
                (*env)->GetBooleanField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaPlayerHttp_isSnIncreasing);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() isSnIncreasing: %u\n", __FUNCTION__,
                (*playerStreamParams)->protocolInfo.fourthField.flags_param.isSnIncreasing);

        //
        // mpe_HnStreamParamsMediaPlayerHttp.protocolInfo.fourthField.flags_param.isStreamingMode
        //
        (*playerStreamParams)->protocolInfo.fourthField.flags_param.isStreamingMode =
                (*env)->GetBooleanField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaPlayerHttp_isStreamingMode);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() isStreamingMode: %u\n", __FUNCTION__,
                (*playerStreamParams)->protocolInfo.fourthField.flags_param.isStreamingMode);

        //
        // mpe_HnStreamParamsMediaPlayerHttp.protocolInfo.fourthField.flags_param.isInteractiveMode
        //
        (*playerStreamParams)->protocolInfo.fourthField.flags_param.isInteractiveMode =
                (*env)->GetBooleanField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaPlayerHttp_isInteractiveMode);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() isInteractiveMode: %u\n",__FUNCTION__,
                (*playerStreamParams)->protocolInfo.fourthField.flags_param.isInteractiveMode);

        //
        // mpe_HnStreamParamsMediaPlayerHttp.protocolInfo.fourthField.flags_param.isBackgroundMode
        //
        (*playerStreamParams)->protocolInfo.fourthField.flags_param.isBackgroundMode =
                (*env)->GetBooleanField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaPlayerHttp_isBackgroundMode);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() isBackgroundMode: %u\n", __FUNCTION__,
                (*playerStreamParams)->protocolInfo.fourthField.flags_param.isBackgroundMode);

        //
        // mpe_HnStreamParamsMediaPlayerHttp.protocolInfo.fourthField.flags_param.isHTTPStallingSupported
        //
        (*playerStreamParams)->protocolInfo.fourthField.flags_param.isHTTPStallingSupported =
                (*env)->GetBooleanField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaPlayerHttp_isHTTPStallingSupported);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() isHTTPStallingSupported: %u\n", __FUNCTION__,
                (*playerStreamParams)->protocolInfo.fourthField.flags_param.isHTTPStallingSupported);

        //
        // mpe_HnStreamParamsMediaPlayerHttp.protocolInfo.fourthField.flags_param.isDLNAV15
        //
        (*playerStreamParams)->protocolInfo.fourthField.flags_param.isDLNAV15 =
                (*env)->GetBooleanField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaPlayerHttp_isDLNAV15);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() isDLNAV15: %u\n", __FUNCTION__,
                (*playerStreamParams)->protocolInfo.fourthField.flags_param.isDLNAV15);

        //
        // mpe_HnStreamParamsMediaPlayerHttp.protocolInfo.isLinkProtected
        //
        (*playerStreamParams)->protocolInfo.fourthField.flags_param.isLinkProtected =
                (*env)->GetBooleanField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaPlayerHttp_isLinkProtected);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() isLinkProtected: %u\n", __FUNCTION__,
                (*playerStreamParams)->protocolInfo.fourthField.flags_param.isLinkProtected);

        //
        // mpe_HnStreamParamsMediaPlayerHttp.protocolInfo.fourthField.flags_param.isFullClearByteSeek
        //
        (*playerStreamParams)->protocolInfo.fourthField.flags_param.isFullClearByteSeek =
                (*env)->GetBooleanField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaPlayerHttp_isFullClearByteSeek);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() isFullClearByteSeek: %u\n", __FUNCTION__,
                (*playerStreamParams)->protocolInfo.fourthField.flags_param.isFullClearByteSeek);

        //
        // mpe_HnStreamParamsMediaPlayerHttp.protocolInfo.fourthField.flags_param.isLimitedClearByteSeek
        //
        (*playerStreamParams)->protocolInfo.fourthField.flags_param.isLimitedClearByteSeek =
                (*env)->GetBooleanField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaPlayerHttp_isLimitedClearByteSeek);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() isLimitedClearByteSeek: %u\n", __FUNCTION__,
                (*playerStreamParams)->protocolInfo.fourthField.flags_param.isLimitedClearByteSeek);

        //
        // mpe_HnStreamParamsMediaPlayerHttp.protocolInfo.fourthField.ps_param.playspeedsCnt
        //
        (*playerStreamParams)->protocolInfo.fourthField.ps_param.playspeedsCnt =
                (uint32_t) (*env)->GetIntField(env, jStreamParams,
                jniutil_CachedIds.HNStreamParamsMediaPlayerHttp_playspeedsCnt);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() playspeedsCnt: %u\n", __FUNCTION__,
                (*playerStreamParams)->protocolInfo.fourthField.ps_param.playspeedsCnt);

        //
        // mpe_HnStreamParamsMediaPlayerHttp.protocolInfo.fourthField.ps_param.playspeeds
        //
        if ((*playerStreamParams)->protocolInfo.fourthField.ps_param.playspeedsCnt > 0)
        {
            if ((err = populatePlayspeedsFromObject(env, jStreamParams, *playerStreamParams))
                    != MPE_SUCCESS)
            {
                deallocatePlayerStreamParams(*playerStreamParams);
                return err;
            }
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() playspeeds array created\n", __FUNCTION__);
        }
        */
    }

    return err;
}

static void deallocatePlayerPlaybackParams(mpe_HnPlaybackParamsMediaPlayerHttp *playerPlaybackParams)
{
    if (playerPlaybackParams == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() playerPlaybackParams is NULL\n", __FUNCTION__);
    }
    else
    {
        if (playerPlaybackParams->cciDescData != NULL)
        {
            if (mpe_memFreeP(MPE_MEM_TEMP, (void*) playerPlaybackParams->cciDescData) != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to deallocate "
                        "mpe_HnPlaybackParamsMediaPlayerHttp.cciDescData\n", __FUNCTION__);
            }
            playerPlaybackParams->cciDescData = NULL;
        }
    }
}

static mpe_Error buildPlayerPlaybackParamsFromObject(JNIEnv *env,
    jobject jPlaybackParams, mpe_HnPlaybackParamsMediaPlayerHttp** playerPlaybackParams)
{
    mpe_Error err = MPE_SUCCESS;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s()\n", __FUNCTION__);

    if ((err = mpe_memAllocP(MPE_MEM_TEMP, sizeof (mpe_HnPlaybackParamsMediaPlayerHttp),
            ((void **) playerPlaybackParams))) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to allocate "
                "mpe_HnPlaybackParamsMediaPlayerHttp with error = %d\n", __FUNCTION__, err);
    }
    else
    {
        jobject jAVStreamParams = 0;
        jint       jVideoDevice = 0;
        jobject jCciDescriptors = 0;

        //
        // mpe_HnParamsParamsMediaPlayerHttp.avStreamParameters
        //
        jAVStreamParams = (*env)->GetObjectField(env, jPlaybackParams,
                jniutil_CachedIds.HNPlaybackParamsMediaPlayerHttp_avStreamParameters);
        if (jAVStreamParams == NULL)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                    "%s() m_avStreamParams object == NULL\n", __FUNCTION__);
            return MPE_EINVAL;
        }

        populateAVStreamParametersFromObject(env, jAVStreamParams, 
                &(*playerPlaybackParams)->avStreamParameters);

        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() avStreamParameters.videoPID: %hu\n",
                __FUNCTION__, (*playerPlaybackParams)->avStreamParameters.videoPID);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() avStreamParameters.videoType: %u\n",
                __FUNCTION__, (*playerPlaybackParams)->avStreamParameters.videoType);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() avStreamParameters.audioPID: %hu\n",
                __FUNCTION__, (*playerPlaybackParams)->avStreamParameters.audioPID);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() avStreamParameters.audioType: %u\n",
                __FUNCTION__, (*playerPlaybackParams)->avStreamParameters.audioType);

        //
        // mpe_HnPlaybackParamsMediaPlayerHttp.videoDevice
        //
        jVideoDevice = (*env)->GetIntField(env, jPlaybackParams,
                jniutil_CachedIds.HNPlaybackParamsMediaPlayerHttp_videoDevice);
        if (jVideoDevice == NON_SPECIFIED_INT || jVideoDevice == -1)
        {
            (*playerPlaybackParams)->videoDevice = (mpe_DispDevice) NULL;
        }
        else
        {
            (*playerPlaybackParams)->videoDevice = (mpe_DispDevice) jVideoDevice;
        }
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() videoDevice: 0x%X\n",
                __FUNCTION__, (*playerPlaybackParams)->videoDevice);

        //
        // mpe_HnPlaybackParamsMediaPlayerHttp.initialBlockingState
        //
        (*playerPlaybackParams)->initialBlockingState = (mpe_Bool) (*env)->GetBooleanField(env, jPlaybackParams,
                jniutil_CachedIds.HNPlaybackParamsMediaPlayerHttp_initialBlockingState);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() initialBlockingState: %u\n",
                __FUNCTION__, (*playerPlaybackParams)->initialBlockingState);

        //
        // mpe_HnPlaybackParamsMediaPlayerHttp.muted
        //
        (*playerPlaybackParams)->muted = (mpe_Bool) (*env)->GetBooleanField(env, jPlaybackParams,
                jniutil_CachedIds.HNPlaybackParamsMediaPlayerHttp_muted);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() muted: %u\n",
                __FUNCTION__, (*playerPlaybackParams)->muted);

        //
        // mpe_HnPlaybackParamsMediaPlayerHttp.requestedGain
        //
        (*playerPlaybackParams)->requestedGain = (float) (*env)->GetFloatField(env, jPlaybackParams,
                jniutil_CachedIds.HNPlaybackParamsMediaPlayerHttp_requestedGain);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() requestedGain: %f\n",
                __FUNCTION__, (*playerPlaybackParams)->requestedGain);

        //
        // mpe_HnPlaybackParamsMediaPlayerHttp.requestedRate
        //
        (*playerPlaybackParams)->requestedRate = (float) (*env)->GetFloatField(env, jPlaybackParams,
                jniutil_CachedIds.HNPlaybackParamsMediaPlayerHttp_requestedRate);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() requestedRate: %f\n",
                __FUNCTION__, (*playerPlaybackParams)->requestedRate);

        //
        // mpe_HnPlaybackParamsMediaPlayerHttp.initialMediaTimeNS
        //
        (*playerPlaybackParams)->initialMediaTimeNS = (int64_t) (*env)->GetLongField(env, jPlaybackParams,
                jniutil_CachedIds.HNPlaybackParamsMediaPlayerHttp_initialMediaTimeNS);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() initialMediaTimeNS: %"PRId64"\n",
                __FUNCTION__, (*playerPlaybackParams)->initialMediaTimeNS);

        //
        // mpe_HnPlaybackMediaPlayerHttp.cciDescSize
        // mpe_HnPlaybackMediaPlayerHttp.cciDescData
        //
        jCciDescriptors = (*env)->GetObjectField(env, jPlaybackParams, 
                jniutil_CachedIds.HNPlaybackParamsMediaPlayerHttp_cciDescriptors);
        if ((err = buildTransportCCIFromObject(env, jCciDescriptors,
                &(*playerPlaybackParams)->cciDescSize, &(*playerPlaybackParams)->cciDescData)) != MPE_SUCCESS)
        {
            deallocatePlayerPlaybackParams(*playerPlaybackParams);
            return err;
        }
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() cciDescSize: %u\n",
                __FUNCTION__, (*playerPlaybackParams)->cciDescSize);

    }

    return err;
}

/*****************************************************************************/
/***                                                                       ***/
/***                         Shared player/server                          ***/
/***                                                                       ***/
/*****************************************************************************/

void throwMPEMediaError(JNIEnv *env, int nativeErrCode, const char *nativeErrMsg)
{
    jint errCode = (jint) nativeErrCode;
    jstring errMsg = (*env)->NewStringUTF(env, nativeErrMsg);
    
    jthrowable mpeMediaError = (*env)->NewObject(env,
            jniutil_CachedIds.MPEMediaError, jniutil_CachedIds.MPEMediaError_init,
            errCode, errMsg);

    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s(%d, %s)\n", __FUNCTION__,
            nativeErrCode, nativeErrMsg);

    (*env)->Throw(env, mpeMediaError);

    (*env)->DeleteLocalRef(env, errMsg);
    (*env)->DeleteLocalRef(env, mpeMediaError);
}

mpe_Error buildStreamParamsFromObject(JNIEnv *env,
    jobject jStreamParams, mpe_HnStreamParams** streamParams)
{
    mpe_Error err = MPE_SUCCESS;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() enter\n", __FUNCTION__);

    if (streamParams == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() passed "
                "mpe_HnStreamParams** is NULL\n", __FUNCTION__);
        err = MPE_EINVAL;
    }
    else if ((err = mpe_memAllocP(MPE_MEM_TEMP, sizeof (mpe_HnStreamParams),
            ((void **) streamParams))) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to allocate "
                "mpe_HnStreamParams with error %d\n", __FUNCTION__, err);
    }
    else
    {
        jmethodID mid = jniutil_CachedIds.HNStreamParams_getStreamType;

        (*streamParams)->streamParams = NULL;

        jint type = (*env)->CallIntMethod(env, jStreamParams, mid);
        switch ((mpe_HnStreamType) type)
        {
            case MPE_HNSTREAM_MEDIA_SERVER_HTTP:
            {
                (*streamParams)->requestType = MPE_HNSTREAM_MEDIA_SERVER_HTTP;
                err = buildServerStreamParamsFromObject(env, jStreamParams,
                        (mpe_HnStreamParamsMediaServerHttp **) &(*streamParams)->streamParams);
                break;
            }
            case MPE_HNSTREAM_MEDIA_PLAYER_HTTP:
            {
                (*streamParams)->requestType = MPE_HNSTREAM_MEDIA_PLAYER_HTTP;
                err = buildPlayerStreamParamsFromObject(env, jStreamParams,
                        (mpe_HnStreamParamsMediaPlayerHttp **) &(*streamParams)->streamParams);
                break;
            }
            default:
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() getStreamType() "
                        "call failed, type = %d\n", __FUNCTION__, (int32_t) type);
                err = MPE_EINVAL;
                break;
            }
        }

        if (err != MPE_SUCCESS)
        {
            deallocateStreamParams(*streamParams);
        }
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() exit\n", __FUNCTION__);

    return err;
}

void deallocateStreamParams(mpe_HnStreamParams* params)
{
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() enter params = 0x%p\n", __FUNCTION__, params);

    if (params->requestType == MPE_HNSTREAM_MEDIA_SERVER_HTTP)
    {
        mpe_HnStreamParamsMediaServerHttp* serverStreamParams = 
                (mpe_HnStreamParamsMediaServerHttp *) params->streamParams;
        deallocateServerStreamParams(serverStreamParams);
    }
    else if (params->requestType == MPE_HNSTREAM_MEDIA_PLAYER_HTTP)
    {
        mpe_HnStreamParamsMediaPlayerHttp* playerStreamParams = 
                (mpe_HnStreamParamsMediaPlayerHttp *) params->streamParams;
        deallocatePlayerStreamParams(playerStreamParams);
    }
    else
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() unrecognized "
                "mpe_HnStreamParams structure!\n", __FUNCTION__);
    }

    if (mpe_memFreeP(MPE_MEM_TEMP, (void*) (params->streamParams)) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to deallocate "
                "mpe_HnStreamParams\n", __FUNCTION__);
    }
    params->streamParams = NULL;

    if (mpe_memFreeP(MPE_MEM_TEMP, (void*) params) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to deallocate "
                "params\n", __FUNCTION__);
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() exit\n", __FUNCTION__);
}

mpe_Error buildPlaybackParamsFromObject(JNIEnv *env,
    jobject jPlaybackParams, mpe_HnPlaybackParams** playbackParams)
{
    mpe_Error err = MPE_SUCCESS;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() enter\n", __FUNCTION__);

    if (playbackParams == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() passed "
                "mpe_HnPlaybackParams** is NULL\n", __FUNCTION__);
        err = MPE_EINVAL;
    }
    else if ((err = mpe_memAllocP(MPE_MEM_TEMP, sizeof (mpe_HnPlaybackParams),
            ((void **) playbackParams))) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to allocate "
                "mpe_HnPlaybackParams with error %d\n", __FUNCTION__, err);
    }
    else
    {
        jmethodID mid = jniutil_CachedIds.HNPlaybackParams_getPlaybackType;

        (*playbackParams)->playbackParams = NULL;

        jint type = (*env)->CallIntMethod(env, jPlaybackParams, mid);
        switch ((mpe_HnPlaybackType) type)
        {
            case MPE_HNPLAYBACK_MEDIA_SERVER_HTTP:
            {
                (*playbackParams)->playbackType = MPE_HNPLAYBACK_MEDIA_SERVER_HTTP;
                err = buildServerPlaybackParamsFromObject(env, jPlaybackParams,
                        (mpe_HnPlaybackParamsMediaServerHttp **) &((*playbackParams)->playbackParams));
                break;
            }
            case MPE_HNPLAYBACK_MEDIA_PLAYER_HTTP:
            {
                (*playbackParams)->playbackType = MPE_HNPLAYBACK_MEDIA_PLAYER_HTTP;
                err = buildPlayerPlaybackParamsFromObject(env, jPlaybackParams,
                        (mpe_HnPlaybackParamsMediaPlayerHttp **) &((*playbackParams)->playbackParams));
                break;
            }
            default:
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() getPlaybackType() "
                        "call failed, type = %d\n", __FUNCTION__, (int32_t) type);
                err = MPE_EINVAL;
                break;
            }
        }

        if (err != MPE_SUCCESS)
        {
            deallocatePlaybackParams(*playbackParams);
        }
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() exit\n", __FUNCTION__);

    return err;
}

void deallocatePlaybackParams(mpe_HnPlaybackParams* params)
{
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() enter params = 0x%p\n", __FUNCTION__, params);

    if (params->playbackType == MPE_HNPLAYBACK_MEDIA_SERVER_HTTP)
    {
        mpe_HnPlaybackParamsMediaServerHttp* serverPlaybackParams =
                (mpe_HnPlaybackParamsMediaServerHttp *) params->playbackParams;
        deallocateServerPlaybackParams(serverPlaybackParams);
    }
    else if (params->playbackType == MPE_HNPLAYBACK_MEDIA_PLAYER_HTTP)
    {
        mpe_HnPlaybackParamsMediaPlayerHttp* playerPlaybackParams =
                (mpe_HnPlaybackParamsMediaPlayerHttp *) params->playbackParams;
        deallocatePlayerPlaybackParams(playerPlaybackParams);
    }
    else
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() unrecognized "
                "mpe_HnPlaybackParams structure!\n", __FUNCTION__);
    }

    if (mpe_memFreeP(MPE_MEM_TEMP, (void*) (params->playbackParams)) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to deallocate "
                "mpe_HnPlaybackParams\n", __FUNCTION__);
    }
    params->playbackParams = NULL;

    if (mpe_memFreeP(MPE_MEM_TEMP, (void*) params) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to deallocate "
                "params\n", __FUNCTION__);
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() exit\n", __FUNCTION__);
}

/*****************************************************************************/
/***                                                                       ***/
/***                             Server only                               ***/
/***                                                                       ***/
/*****************************************************************************/

mpe_Error buildContentDescriptionFromObject(JNIEnv *env,
        jint jContentLocationType, jobject jContentDescription,
        void** contentDescription)
{
    mpe_Error err = MPE_SUCCESS;

    mpe_HnStreamContentLocation contentLocation =
            (mpe_HnStreamContentLocation) jContentLocationType;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() - buildContentDescriptionFromObject - content location type: %d\n",
            __FUNCTION__, (int32_t) jContentLocationType);
    switch (contentLocation)
    {
        case MPE_HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT:
        {
            err = buildContentDescriptionMSV(env, jContentDescription, contentDescription);
            break;
        }
        case MPE_HN_CONTENT_LOCATION_LOCAL_FILE_CONTENT:
        {
            err = buildContentDescriptionApp(env, jContentDescription, contentDescription);
            break;
        }
        case MPE_HN_CONTENT_LOCATION_LOCAL_TSB:
        {
            err = buildContentDescriptionTSB(env, jContentDescription, contentDescription);
            break;
        }
        case MPE_HN_CONTENT_LOCATION_LOCAL_VIDEO_DEVICE:
        {
            err = buildContentDescriptionVideoDevice(env, jContentDescription, contentDescription);
            break;
        }
        case MPE_HN_CONTENT_LOCATION_LOCAL_TUNER:
        {
            err = buildContentDescriptionTuner(env, jContentDescription, contentDescription);
            break;
        }
        case MPE_HN_CONTENT_LOCATION_UNKNOWN:
        default:
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() - unhandled content location type: %d\n",
                    __FUNCTION__, (int32_t) jContentLocationType);
            err = MPE_EINVAL;
        }
    }

    return err;
}

void deallocateContentDescription(mpe_HnStreamContentLocation contentLocation,
        void *contentDescription)
{
    if (contentDescription == NULL)
    {
        return ;
    }

    switch (contentLocation)
    {
        case MPE_HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT:
        {
            mpe_HnStreamLocalSVContentDescription *lsv =
                    (mpe_HnStreamLocalSVContentDescription *) contentDescription;
            if (mpe_memFreeP(MPE_MEM_TEMP, (void*) lsv->contentName) != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to deallocate "
                        "mpe_HnStreamLocalSVContentDescription.contentName\n", __FUNCTION__);
            }
            lsv->contentName = NULL;
            break;
        }
        case MPE_HN_CONTENT_LOCATION_LOCAL_FILE_CONTENT:
        {
            mpe_HnStreamAppContentDescription *app =
                    (mpe_HnStreamAppContentDescription *) contentDescription;
            if (mpe_memFreeP(MPE_MEM_TEMP, (void*) app->contentName) != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to deallocate "
                        "mpe_HnStreamAppContentDescription.contentName\n", __FUNCTION__);
            }
            app->contentName = NULL;
            if (mpe_memFreeP(MPE_MEM_TEMP, (void*) app->pathName) != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to deallocate "
                        "mpe_HnStreamAppContentDescription.pathName\n", __FUNCTION__);
            }
            app->pathName = NULL;
            break;
        }
        case MPE_HN_CONTENT_LOCATION_LOCAL_TSB:
        case MPE_HN_CONTENT_LOCATION_LOCAL_VIDEO_DEVICE:
        {
            // Nothing to de-allocate for these two types
            break;
        }
        case MPE_HN_CONTENT_LOCATION_LOCAL_TUNER:
        {
            //Free pids
            mpe_HnStreamTunerContentDescription *tunerDesc = (mpe_HnStreamTunerContentDescription *)contentDescription;
            if (mpe_memFreeP(MPE_MEM_TEMP, (void*) tunerDesc->pids) != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to deallocate "
                        "mpe_HnStreamTunerContentDescription.pids\n", __FUNCTION__);
            }
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() deallocated "
                    "mpe_HnStreamTunerContentDescription.pids\n", __FUNCTION__);
            tunerDesc->pids = NULL;

            break;
        }
        case MPE_HN_CONTENT_LOCATION_UNKNOWN:
        default:
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() - unhandled content location type: %u\n",
                    __FUNCTION__, contentLocation);
        }
    }

    if (mpe_memFreeP(MPE_MEM_TEMP, contentDescription) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to deallocate "
                "mpe_HnStreamContentDescription\n", __FUNCTION__);
    }
}

void deallocateContentTransformation(mpe_hnContentTransformation *contentTransformation)
{
    if (contentTransformation == NULL)
    {
        return ;
    }
    if (mpe_memFreeP(MPE_MEM_TEMP, (void*) contentTransformation) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to deallocate "
                "deallocateContentTransformation.\n", __FUNCTION__);
    }
    contentTransformation = NULL;
}

/*****************************************************************************/
/***                                                                       ***/
/***                             Player only                               ***/
/***                                                                       ***/
/*****************************************************************************/

void populateAVStreamParametersFromObject(JNIEnv *env,
        jobject jAVStreamParams, mpe_HnHttpHeaderAVStreamParameters *avsParams)
{
    if (avsParams == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() - avsParams is NULL\n", __FUNCTION__);
    }
    else
    {
        avsParams->videoPID = (uint16_t) (*env)->GetIntField(env,
                jAVStreamParams, jniutil_CachedIds.HNHttpHeaderAVStreamParameters_videoPID);
        avsParams->videoType = (uint32_t) (*env)->GetIntField(env,
                jAVStreamParams, jniutil_CachedIds.HNHttpHeaderAVStreamParameters_videoType);
        avsParams->audioPID = (uint16_t) (*env)->GetIntField(env,
                jAVStreamParams, jniutil_CachedIds.HNHttpHeaderAVStreamParameters_audioPID);
        avsParams->audioType = (uint32_t) (*env)->GetIntField(env,
                jAVStreamParams, jniutil_CachedIds.HNHttpHeaderAVStreamParameters_audioType);
    }
}

/*
mpe_Error populatePlayspeedsFromObject(JNIEnv *env, jobject jStreamParams,
        mpe_HnStreamParamsMediaPlayerHttp* playerStreamParams)
{
    mpe_Error err = MPE_SUCCESS;

    int cnt = playerStreamParams->protocolInfo.fourthField.ps_param.playspeedsCnt;

    if ((err = mpe_memAllocP(MPE_MEM_TEMP, cnt * sizeof(float),
            (void **)&(playerStreamParams->protocolInfo.fourthField.ps_param.playspeeds))) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() unable to allocate "
                "memory for playspeeds\n", __FUNCTION__);
        err = MPE_ENOMEM;
    }
    else
    {
        jfloatArray jarr = (jfloatArray)(*env)->GetObjectField(env, jStreamParams,
                                jniutil_CachedIds.HNStreamParamsMediaPlayerHttp_playspeeds);
        if ((*env)->ExceptionOccurred(env))
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() exception occurred "
                    "while accessing float array field\n", __FUNCTION__);
            err = MPE_EINVAL;
        }
        else if (jarr != NULL)
        {
            (*env)->GetFloatArrayRegion(env, jarr, 0, cnt,
                    playerStreamParams->protocolInfo.fourthField.ps_param.playspeeds);

            if (playerStreamParams->protocolInfo.fourthField.ps_param.playspeeds != NULL)
            {
                float* playspeeds = playerStreamParams->protocolInfo.fourthField.ps_param.playspeeds;
                int i = 0;
                for (i = 0; i < cnt; i++)
                {
                    MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "%s() playspeed %d: %f\n",
                            __FUNCTION__, i, *(playspeeds + i));
                }
            }
            else
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                        "%s() playerStreamParams->protocolInfo.fourthField.ps_param.playspeeds object is null\n",
                        __FUNCTION__);
                if (mpe_memFreeP(MPE_MEM_TEMP, (void*)playerStreamParams->protocolInfo.fourthField.ps_param.playspeeds)
                        != MPE_SUCCESS)
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() failed to deallocate "
                            "playerStreamParams->protocolInfo.fourthField.ps_param.playspeeds\n", __FUNCTION__);
                }
                err = MPE_EINVAL;
            }
        }
    }

    return err;
}
*/
