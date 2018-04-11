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

#include <mpe_types.h>
#include <mpe_socket.h>
#include <mpe_dbg.h>
#include "jvm.h"
#include "jni_util.h"
#include "net_util.h"

#include "java_net_SocketInputStream.h"

/************************************************************************
 * SocketInputStream
 */

#include "jni_statics.h"

#define MPE_MEM_DEFAULT MPE_MEM_JVM

/*
 * Class:     java_net_SocketInputStream
 * Method:    init
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_java_net_SocketInputStream_init(JNIEnv *env, jclass cls)
{
    jclass fis_cls =
    (*env)->FindClass(env, "java/io/FileInputStream");
    if (fis_cls == NULL)
    {
        return; /* exception */
    }

    JNI_STATIC_MD(java_net_SocketInputStream, IO_fd_fdID)
    = NET_GetFileDescriptorID(env);

}

/*
 * Class:     java_net_SocketInputStream
 * Method:    socketRead
 * Signature: ([BII)I
 */
JNIEXPORT jint JNICALL
Java_java_net_SocketInputStream_socketRead0(JNIEnv *env, jobject this, jobject fdObj
        , jbyteArray data, jint off, jint len, jint timeout)
{
    jbyte BUF[MAX_BUFFER_LEN];
    jbyte *bufP;
    jint nread;
    jint datalen;
    mpe_Socket fd;
    mpe_Error error;

    if (IS_NULL(fdObj))
    {
        /* should't this be a NullPointerException? -br */
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", "null fd object");
        return -1;
    }
    else
    {
        fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_SocketInputStream, IO_fd_fdID));

        /* Bug 4086704 - If the Socket associated with this file descriptor
         * was closed (sysCloseFD), the the file descriptor is set to -1.
         */
        if (fd == -1)
        {
            JNU_ThrowByName(env, "java/net/SocketException", "Socket closed");
            return -1;
        }
    }
    if (IS_NULL(data))
    {
        JNU_ThrowNullPointerException(env, "data argument");
        return -1;
    }

    datalen = (*env)->GetArrayLength(env, data);

    if (len == 0 || datalen == 0)
    return 0;

    if (len < 0 || len + off > datalen)
    {
        JNU_ThrowByName(env, JNU_JAVAPKG "ArrayIndexOutOfBoundsException", 0);
        return -1;
    }

    /* If requested amount to be read is > MAX_BUFFER_LEN then
     * we allocate a buffer from the heap (up to the limit
     * specified by MAX_HEAP_BUFFER_LEN). If memory is exhausted
     * we always use the stack buffer.
     */
    if (len <= MAX_BUFFER_LEN)
    {
        bufP = BUF;
    }
    else
    {
        if (len > MAX_HEAP_BUFFER_LEN)
        len = MAX_HEAP_BUFFER_LEN;

        error = mpe_memAlloc(len, (void**)&bufP);
        if (error != MPE_SUCCESS)
        {
            /* allocation failed so use stack buffer */
            bufP = BUF;
            len = MAX_BUFFER_LEN;
        }
    }

    if (timeout)
    {
        /* Do select waiting for input for specified time. */
        nread = NET_Timeout(fd, timeout);
        if (nread <= 0)
        {
            if (nread == 0)
            {
                JNU_ThrowByName(env, JNU_JAVAIOPKG "InterruptedIOException",
                        "Read timed out");
            }
            else if (nread == JVM_IO_ERR)
            {
                JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
                        (mpe_socketGetLastError() == MPE_SOCKET_EBADF) ?
                        "Socket closed" : 0);
            }
            else if (nread == JVM_IO_INTR)
            {
                JNU_ThrowByName(env, JNU_JAVAIOPKG "InterruptedIOException",
                        "Operation interrupted");
            }
            if (bufP != BUF)
            {
                mpe_memFree(bufP);
            }
            return -1;
        }
    }

    nread = NET_Recv(fd, bufP, len, 0);

    if (nread < 0)
    {
        NET_ThrowCurrent(env, strerror(error));
        if (bufP != BUF)
        {
            mpe_memFree(bufP);
        }
        return -1;
    }
    (*env)->SetByteArrayRegion(env, data, off, nread, (jbyte *)bufP);

    if (bufP != BUF)
    {
        mpe_memFree(bufP);
    }
    return nread;
}
