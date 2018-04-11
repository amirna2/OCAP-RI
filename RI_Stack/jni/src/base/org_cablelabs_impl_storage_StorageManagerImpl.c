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

#include "org_cablelabs_impl_storage_StorageManagerImpl.h"
#include "mgrdef.h"
#include "mpe_dbg.h"
#include "jni_util.h"
#include "mpe_storage.h"
#include "mpe_dvr.h"
#include "mpe_ed.h"

/*
 * Class:     org_cablelabs_impl_storage_StorageManagerImpl
 * Method:    nRegister
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_storage_StorageManagerImpl_nRegister
(JNIEnv *env, jobject obj)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_EdEventInfo *edHandle = NULL;
    JNI_UNUSED(env);

    /* create a handle and register. */
    err = mpe_edCreateHandle(obj, MPE_ED_QUEUE_NORMAL, NULL,
            MPE_ED_TERMINATION_OPEN, 0, &edHandle);

    if (err != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "mpe_edCreateHandle() FAILED! -- returned: 0x%x\n", err);
    }
    else
    {
        err = mpe_storageRegisterQueue(edHandle->eventQ, (void*)edHandle);
        if (err != MPE_STORAGE_ERR_NOERR )
        {
            mpe_edDeleteHandle(edHandle);
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                    "mpe_storageRegisterQueue() FAILED! -- returned: 0x%x\n", err);
        }
    }
}
/*
 * Class:     org_cablelabs_impl_storage_StorageManagerImpl
 * Method:    nGetDevices
 * Signature: (Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_storage_StorageManagerImpl_nGetDevices
(JNIEnv *env, jobject obj, jobject vec)
{
    uint32_t i;
    uint32_t uCount = 0;
    jclass clsVector, clsInteger;
    jobject integer;
    jmethodID midVector, midInteger;
    mpe_StorageHandle *handleArray = NULL;
    JNI_UNUSED(obj);

    /* Get the max device count and allocate buffer. */
    (void)mpe_storageGetDeviceCount(&uCount);
    (void)mpe_memAllocP(MPE_MEM_STORAGE, uCount*sizeof(mpe_StorageHandle), (void**)&handleArray);

    if (handleArray != NULL)
    {
        if (mpe_storageGetDeviceList(&uCount, handleArray) ==
                MPE_STORAGE_ERR_NOERR)
        {
            clsVector =(*env)->GetObjectClass(env, vec);

            if (clsVector == NULL)
            {
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                        "StorageManagerImpl JNI: nGetDevices clsVector == NULL!\n");
            }
            else
            {
                midVector = (*env)->GetMethodID(env, clsVector,
                        "add", "(Ljava/lang/Object;)Z");
                if (midVector == NULL)
                {
                    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                            "StorageManagerImpl JNI: nGetDevices midVector == NULL!\n");
                }
                else
                {
                    clsInteger = (*env)->FindClass(env, "java/lang/Integer");
                    if (clsInteger == NULL)
                    {
                        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                                "StorageManagerImpl JNI: nGetDevices clsInteger == NULL!\n");
                    }
                    else
                    {
                        /* Get the Integer constructor method. */
                        midInteger = (*env)->GetMethodID(env, clsInteger,
                                "<init>", "(I)V");
                        if (midInteger == NULL)
                        {
                            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                                    "StorageManagerImpl JNI: nGetDevices midInteger == NULL!\n");
                        }
                        else
                        {
                            for (i = 0; i < uCount; i++)
                            {
                                /* Create an integer object and add it to vector */
                                integer = (*env)->NewObject(env, clsInteger,
                                        midInteger, handleArray[i]);
                                if (integer != NULL)
                                {
                                    (void)(*env)->CallBooleanMethod(env, vec,
                                            midVector, integer);
                                }
                            } // end-for
                        }
                    } // end-if (clsInteger)
                }
            } // end-if (clsVector)
        }
        mpe_memFreeP(MPE_MEM_STORAGE, handleArray);
    }
}

/*
 * Class:     org_cablelabs_impl_storage_StorageManagerImpl
 * Method:    nGetStatus
 * Signature: (I)B
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_storage_StorageManagerImpl_nGetStatus(
        JNIEnv *env, jobject obj, jint handle)
{
    mpe_StorageStatus status;

    JNI_UNUSED(env);
    JNI_UNUSED(obj);

    return (mpe_storageGetInfo((mpe_StorageHandle) handle, MPE_STORAGE_STATUS,
            &status) == MPE_STORAGE_ERR_NOERR ? (jint) status
            : (jint) MPE_STORAGE_STATUS_DEVICE_ERR);
}
