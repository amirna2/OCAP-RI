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

#include <mpe_dbg.h>
#include <mpe_error.h>

#include "org_cablelabs_impl_net_Socket.h"

/*
 * Class:     org_cablelabs_impl_net_Socket
 * Method:    getNativeHandle
 * Signature: (Ljava/net/Socket;)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_net_Socket_getNativeHandle
  (JNIEnv *env, jclass cls, jobject jSocket)
{
    jclass clazz = 0;
    jobject  obj = 0;
    jfieldID fid = 0;

    jint fd = -1;

    MPE_UNUSED_PARAM(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s() - called\n", __FUNCTION__);

    // Drill down into the Java Socket and get the native handle.
    if ((clazz = (*env)->GetObjectClass(env, jSocket)) == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
            "%s() - failed to get jSocket class\n", __FUNCTION__);
    }
    // Get the socketImpl object 'impl' field id
    else if ((fid = (*env)->GetFieldID(env, clazz, "impl",
        "Ljava/net/SocketImpl;" )) == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
            "%s() - failed to get field id of impl\n", __FUNCTION__);
    }
    // Get socketImpl object
    else if ((obj = (*env)->GetObjectField(env, jSocket, fid)) == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
            "%s() - failed to get impl object\n", __FUNCTION__);
    }
    // Get the class associated with socketImpl object
    else if ((clazz = (*env)->GetObjectClass(env, obj)) == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
            "%s() - failed to get jSocketImpl class\n", __FUNCTION__);
    }
    // Get the field id of file description object
    else if ((fid = (*env)->GetFieldID(env, clazz, "fd",
        "Ljava/io/FileDescriptor;")) == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
            "%s() - failed to get file descriptor field id\n", __FUNCTION__); 
    }
    // Get the file descriptor object
    else if ((obj = (*env)->GetObjectField(env, obj, fid)) == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
            "%s() - failed to get file descriptor object\n", __FUNCTION__);
    }
    // Get the class associated with File Descriptor object
    else if ((clazz = (*env)->GetObjectClass(env, obj)) == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
            "%s() - failed to get FileDescriptor class\n", __FUNCTION__);
    }
    // Get the fd object field id
    else if ((fid = (*env)->GetFieldID(env, clazz, "fd", "I" )) == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
            "%s() - failed to get fd field id\n", __FUNCTION__);
    }
    // Get the integer value
    else if ((fd = (*env)->GetIntField(env, obj, fid)) == -1)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
            "%s() - failed to get fd field (socket handle)\n", __FUNCTION__);
    }
    else
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "%s() - successfully got fd field (socket handle) = 0x%x\n",
            __FUNCTION__, fd);
    }

    if (fd == -1)
    {
        clazz = (*env)->FindClass(env, "java/lang/Error");
        if (clazz != NULL)
        {
            (*env)->ThrowNew(env, clazz,
                "Unable to extract native mpe_Socket handle");
        }
        (*env)->DeleteLocalRef(env, clazz);
    }

    return fd;
}
