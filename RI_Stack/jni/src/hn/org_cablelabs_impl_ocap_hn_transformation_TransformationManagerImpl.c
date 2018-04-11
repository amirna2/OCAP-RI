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

#include <org_cablelabs_impl_ocap_hn_transformation_TransformationManagerImpl.h>
#include "jni_util.h"
#include "jni_util_hn.h"
#include <jni.h>
#include <string.h>
#include <mpe_dbg.h>
#include <mpe_hn.h>
#include <mpe_ed.h>
#include <mpe_os.h>
#include <mpeos_dll.h>

#include <inttypes.h> // for PRIx64

// Define the memory to be HN allocated category
#define MPE_MEM_DEFAULT MPE_MEM_HN

/*
 * Class:     org_cablelabs_impl_ocap_hn_transformation_TransformationManagerImpl
 * Method:    jniInit
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_ocap_hn_transformation_TransformationManagerImpl_jniInit
  (JNIEnv *env, jclass cls)
{
    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "HN TransformationManagerImpl.jniInit - Enter\n");

    /* NativeContentTransformation */
    GET_CLASS(NativeContentTransformation,"org/cablelabs/impl/ocap/hn/transformation/NativeContentTransformation");
    GET_METHOD_ID(NativeContentTransformation_init, "<init>", "(ILjava/lang/String;Ljava/lang/String;IIIZ)V");
    GET_FIELD_ID(NativeContentTransformation_id, "id", "I"); // int
    GET_FIELD_ID(NativeContentTransformation_sourceProfile, "sourceProfile", "Ljava/lang/String;");
    GET_FIELD_ID(NativeContentTransformation_transformedProfile, "transformedProfile", "Ljava/lang/String;");
    GET_FIELD_ID(NativeContentTransformation_bitrate, "bitrate", "I"); // int
    GET_FIELD_ID(NativeContentTransformation_width, "width", "I"); // int
    GET_FIELD_ID(NativeContentTransformation_height, "height", "I"); // int
    GET_FIELD_ID(NativeContentTransformation_progressive, "progressive", "Z"); // boolean

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "HN TransformationManagerImpl.jniInit - Exit\n");
}

JNIEXPORT jobjectArray JNICALL Java_org_cablelabs_impl_ocap_hn_transformation_TransformationManagerImpl_getNativeContentTransformations
  (JNIEnv *env, jobject cls)
{
    mpe_Error err;
    uint32_t transformationCnt;
    jobjectArray javaTransformations = (jobjectArray) NULL;
    mpe_hnContentTransformation * nativeTransformations = NULL;
    int i;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "JNI(TransformationManager): getNativeContentTransformations() - Enter\n");

    err = mpeos_hnServerGetContentTransformationCnt(&transformationCnt);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() mpeos_hnServerGetContentTransformationCnt() returned %d\n",
                __FUNCTION__, err);
        return NULL;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "JNI(TransformationManager): getNativeContentTransformations(): Platform identified %d capabilities\n",
            transformationCnt );

    err = mpe_memAllocP( MPE_MEM_TEMP, sizeof(mpe_hnContentTransformation) * transformationCnt,
                         (void**) &nativeTransformations );
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() getNativeContentTransformations(): Error %d allocating capabilities array (%d bytes)\n",
                __FUNCTION__, err, sizeof(mpe_hnContentTransformation) * transformationCnt );
        goto freeAndReturn;
    }

    err = mpeos_hnServerGetContentTransformations(nativeTransformations);
    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() getNativeContentTransformations(): mpeos_hnServerGetContentTransformations returned %d\n",
                __FUNCTION__, err );
        goto freeAndReturn;
    }

    javaTransformations = (*env)->NewObjectArray( env, transformationCnt,
                                               jniutil_CachedIds.NativeContentTransformation,
                                               NULL );
    if (javaTransformations == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() getNativeContentTransformations(): Error allocating array of NativeContentTransformations\n",
                __FUNCTION__);
        goto freeAndReturn;
    }

    // Ready to fill in the values
    for (i=0; i<transformationCnt; i++)
    {
        jstring sourceProfile = NULL;
        jstring transformedProfile = NULL;
        jobject newJavaTransform = NULL;

        MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_JNI,
                "JNI(TransformationManager): getNativeContentTransformations(): Processing transformation %d: (%s, %s, %d, %d, %d, %d)\n",
                i, nativeTransformations[i].sourceProfile, nativeTransformations[i].transformedProfile,
                nativeTransformations[i].bitrate, nativeTransformations[i].width,
                nativeTransformations[i].height, nativeTransformations[i].progressive );

        sourceProfile = (*env)->NewStringUTF(env, nativeTransformations[i].sourceProfile);
        if (sourceProfile == NULL)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() getNativeContentTransformations(): Error allocating sourceProfile for transformation %d (\"%s\")\n",
                    __FUNCTION__, i, nativeTransformations[i].sourceProfile);
            javaTransformations = NULL; // The ref will be lost and GCed
            goto freeAndReturn;
        }

        transformedProfile = (*env)->NewStringUTF(env, nativeTransformations[i].transformedProfile);
        if (transformedProfile == NULL)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() getNativeContentTransformations(): Error allocating transformedProfile for capability %d (\"%s\")\n",
                    __FUNCTION__, i, nativeTransformations[i].transformedProfile);
            javaTransformations = NULL; // The refs will be lost and GCed
            goto freeAndReturn;
        }


        newJavaTransform = (*env)->NewObject( env,
                                              jniutil_CachedIds.NativeContentTransformation,
                                              jniutil_CachedIds.NativeContentTransformation_init,
                                              nativeTransformations[i].id,
                                              sourceProfile,
                                              transformedProfile,
                                              nativeTransformations[i].bitrate,
                                              nativeTransformations[i].width,
                                              nativeTransformations[i].height,
                                              nativeTransformations[i].progressive );

        if (newJavaTransform == NULL)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() getNativeContentTransformations(): Error creating NativeContentTransformation for transformation  %d\n",
                    __FUNCTION__, i);
            javaTransformations = NULL; // The refs will be lost and GCed
            goto freeAndReturn;
        }

        // Put the newly-created NativeContentTransformation in the newly-created array
        (*env)->SetObjectArrayElement(env, javaTransformations, i, newJavaTransform);
        (*env)->DeleteLocalRef(env, sourceProfile);
        (*env)->DeleteLocalRef(env, transformedProfile);
        (*env)->DeleteLocalRef(env, newJavaTransform);
    } // END loop through native capabilities

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "JNI(TransformationManager): getNativeContentTransformations(): Successfully processed %d transformations\n",
            transformationCnt );

    freeAndReturn:

    if (nativeTransformations != NULL)
    {
        mpe_memFreeP(MPE_MEM_TEMP, nativeTransformations);
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "JNI(TransformationManager): getNativeContentTransformations() - Exit (%p)\n",
            javaTransformations);
    return javaTransformations;
}
