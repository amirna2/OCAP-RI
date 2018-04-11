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

#include "org_cablelabs_impl_storage_StorageProxyImpl.h"
#include "mgrdef.h"
#include "mpe_dbg.h"
#include "jni_util.h"
#include "mpe_storage.h"

#define MPE_STORAGE_ACCESS_RIGHT_MAX 8
/*
 * Class:     org_cablelabs_impl_storage_StorageProxyImpl
 * Method:    nGetDisplayName
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_cablelabs_impl_storage_StorageProxyImpl_nGetDisplayName(
        JNIEnv *env, jobject obj, jint handle)
{
    jchar dispName[MPE_STORAGE_MAX_DISPLAY_NAME_SIZE + 1];

    JNI_UNUSED(obj);

    return (mpe_storageGetInfo((mpe_StorageHandle) handle,
            MPE_STORAGE_DISPLAY_NAME, dispName) == MPE_STORAGE_ERR_NOERR ? (*env)->NewStringUTF(
            env, (char*) dispName)
            : NULL);
}

/*
 * Class:     org_cablelabs_impl_storage_StorageProxyImpl
 * Method:    nGetFreeSpace
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_storage_StorageProxyImpl_nGetFreeSpace(
        JNIEnv *env, jobject obj, jint handle)
{
    uint64_t freeSpace;

    JNI_UNUSED(env);
    JNI_UNUSED(obj);

    return (mpe_storageGetInfo((mpe_StorageHandle) handle,
            MPE_STORAGE_FREE_SPACE, &freeSpace) == MPE_STORAGE_ERR_NOERR ? freeSpace
            : 0);
}

/*
 * Class:     org_cablelabs_impl_storage_StorageProxyImpl
 * Method:    nGetName
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_cablelabs_impl_storage_StorageProxyImpl_nGetName(
        JNIEnv *env, jobject obj, jint handle)
{
    jchar name[MPE_STORAGE_MAX_NAME_SIZE + 1];

    JNI_UNUSED(obj);

    return (mpe_storageGetInfo((mpe_StorageHandle) handle, MPE_STORAGE_NAME,
            name) == MPE_STORAGE_ERR_NOERR ? (*env)->NewStringUTF(env,
            (char*) name) : NULL);
}

/*
 * Class:     org_cablelabs_impl_storage_StorageProxyImpl
 * Method:    nGetTotalSpace
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_storage_StorageProxyImpl_nGetTotalSpace(
        JNIEnv *env, jobject obj, jint handle)
{
    uint64_t storageCapacity;

    JNI_UNUSED(env);
    JNI_UNUSED(obj);

    return (mpe_storageGetInfo((mpe_StorageHandle) handle,
            MPE_STORAGE_CAPACITY, &storageCapacity) == MPE_STORAGE_ERR_NOERR ? (jlong) storageCapacity
            : 0);
}

/*
 * Class:     org_cablelabs_impl_storage_StorageProxyImpl
 * Method:    nGetSupportedAccessRights
 * Signature: (I)[Z
 */
JNIEXPORT jbooleanArray JNICALL Java_org_cablelabs_impl_storage_StorageProxyImpl_nGetSupportedAccessRights(
        JNIEnv *env, jobject obj, jint handle)
{
    uint8_t accessRights;
    jboolean *arrayElement;
    jbooleanArray accessRightsArray = NULL;

    JNI_UNUSED(obj);

    if (mpe_storageGetInfo((mpe_StorageHandle) handle,
            MPE_STORAGE_SUPPORTED_ACCESS_RIGHTS, &accessRights)
            == MPE_STORAGE_ERR_NOERR)
    {
        accessRightsArray = (*env)->NewBooleanArray(env,
                MPE_STORAGE_ACCESS_RIGHT_MAX);
        if (accessRightsArray != NULL)
        {
            arrayElement = (*env)->GetBooleanArrayElements(env,
                    accessRightsArray, NULL);
            if (arrayElement != NULL)
            {
                arrayElement[0] = (MPE_STORAGE_ACCESS_RIGHT_WORLD_READ
                        & accessRights) ? JNI_TRUE : JNI_FALSE;
                arrayElement[1] = (MPE_STORAGE_ACCESS_RIGHT_WORLD_WRITE
                        & accessRights) ? JNI_TRUE : JNI_FALSE;
                arrayElement[2] = (MPE_STORAGE_ACCESS_RIGHT_APP_READ
                        & accessRights) ? JNI_TRUE : JNI_FALSE;
                arrayElement[3] = (MPE_STORAGE_ACCESS_RIGHT_APP_WRITE
                        & accessRights) ? JNI_TRUE : JNI_FALSE;
                arrayElement[4] = (MPE_STORAGE_ACCESS_RIGHT_ORG_READ
                        & accessRights) ? JNI_TRUE : JNI_FALSE;
                arrayElement[5] = (MPE_STORAGE_ACCESS_RIGHT_ORG_WRITE
                        & accessRights) ? JNI_TRUE : JNI_FALSE;
                arrayElement[6] = (MPE_STORAGE_ACCESS_RIGHT_OTHER_READ
                        & accessRights) ? JNI_TRUE : JNI_FALSE;
                arrayElement[7] = (MPE_STORAGE_ACCESS_RIGHT_OTHER_WRITE
                        & accessRights) ? JNI_TRUE : JNI_FALSE;
            }
            (*env)->ReleaseBooleanArrayElements(env, accessRightsArray,
                    arrayElement, 0);
        }
    }

    return accessRightsArray;
}

/*
 * Class:     org_cablelabs_impl_storage_StorageProxyImpl
 * Method:    nFormat
 * Signature: (IZ)Z
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_storage_StorageProxyImpl_nFormat(
        JNIEnv *env, jobject obj, jint handle, jboolean userAuthorized)
{
    JNI_UNUSED(env);
    JNI_UNUSED(obj);

    return (mpe_storageInitializeDevice((mpe_StorageHandle) handle,
            userAuthorized, NULL) == MPE_STORAGE_ERR_NOERR ? JNI_TRUE
            : JNI_FALSE);
}

/*
 * Class:     org_cablelabs_impl_storage_StorageProxyImpl
 * Method:    nIsDetachable
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_storage_StorageProxyImpl_nIsDetachable(
        JNIEnv *env, jobject obj, jint handle)
{
    mpe_Bool detachable;

    JNI_UNUSED(env);
    JNI_UNUSED(obj);

    return (mpe_storageIsDetachable((mpe_StorageHandle) handle, &detachable)
            == MPE_STORAGE_ERR_NOERR ? (detachable ? JNI_TRUE : JNI_FALSE)
            : JNI_FALSE);
}

/*
 * Class:     org_cablelabs_impl_storage_StorageProxyImpl
 * Method:    nIsRemovable
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_storage_StorageProxyImpl_nIsRemovable(
        JNIEnv *env, jobject obj, jint handle)
{
    mpe_Bool removable;

    JNI_UNUSED(env);
    JNI_UNUSED(obj);

    return (mpe_storageIsRemovable((mpe_StorageHandle) handle, &removable)
            == MPE_STORAGE_ERR_NOERR ? (removable ? JNI_TRUE : JNI_FALSE)
            : JNI_FALSE);
}

/*
 * Class:     org_cablelabs_impl_storage_StorageProxyImpl
 * Method:    nGetPath
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_cablelabs_impl_storage_StorageProxyImpl_nGetPath(
        JNIEnv *env, jobject obj, jint handle)
{
    jchar path[MPE_STORAGE_MAX_PATH_SIZE + 1];

    JNI_UNUSED(obj);

    return (mpe_storageGetInfo((mpe_StorageHandle) handle,
            MPE_STORAGE_GPFS_PATH, path) == MPE_STORAGE_ERR_NOERR ? (*env)->NewStringUTF(
            env, (char*) path)
            : NULL);
}
