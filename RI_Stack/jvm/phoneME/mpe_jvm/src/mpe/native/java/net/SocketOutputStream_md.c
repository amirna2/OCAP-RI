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
#include "jni_util.h"
#include "jvm.h"
#include "net_util.h"

#include "java_net_SocketOutputStream.h"

#define min(x, y) (((int)(x) < (int)(y)) ? (x) : (y))

/*
 * SocketOutputStream
 */

#include "jni_statics.h"

/*
 * Class:     java_net_SocketOutputStream
 * Method:    init
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_java_net_SocketOutputStream_init(JNIEnv *env, jclass cls)
{
    JNI_STATIC_MD(java_net_SocketOutputStream, IO_fd_fdID)
    = NET_GetFileDescriptorID(env);
}

/*
 * Class:     java_net_SocketOutputStream
 * Method:    socketWrite
 * Signature: ([BII)V
 */
JNIEXPORT void JNICALL
Java_java_net_SocketOutputStream_socketWrite0(JNIEnv *env, jobject this, jobject fdObj
        , jbyteArray data, jint off, jint len)
{
    /*
     * We allocate a static buffer on the stack, copy successive
     * chunks of the buffer to be written into it, then write that. It
     * is believed that this is faster that doing a malloc and copy.  
     */
    uint8_t BUF[MAX_BUFFER_LEN];
    mpe_Socket fd;
    jint datalen;

    if (IS_NULL(fdObj))
    {
        JNU_ThrowByName(env, "java/net/SocketException", "Socket closed");
        return;
    }
    else
    {
        fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_SocketOutputStream, IO_fd_fdID));
        /* Bug 4086704 - If the Socket associated with this file descriptor
         * was closed (sysCloseFD), the the file descriptor is set to -1.
         */
        if (fd == -1)
        {
            JNU_ThrowByName(env, "java/net/SocketException", "Socket closed");
            return;
        }
    }

    if (IS_NULL(data))
    {
        JNU_ThrowNullPointerException(env, "data argument");
        return;
    }

    datalen = (*env)->GetArrayLength(env, data);

    if ((len < 0) || (off < 0) || (len + off > datalen))
    {
        JNU_ThrowByName(env, "java/lang/ArrayIndexOutOfBoundsException", 0);
        return;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,"Java_java_net_SocketOutputStream_socketWrite0: length = %d\n", len);

    while(len > 0)
    {
        int loff = 0;
        int chunkLen = min(MAX_BUFFER_LEN, len);
        int llen = chunkLen;
        (*env)->GetByteArrayRegion(env, data, off, chunkLen, (jbyte *)BUF);

        while(llen > 0)
        {
            int n = NET_Send(fd, BUF + loff, llen, 0);
            if (n == JVM_IO_ERR)
            {
                JNU_ThrowByName(env, "java/io/IOException", 0);
                return;
            }
            if (n == JVM_IO_INTR)
            {
                JNU_ThrowByName(env, "java/io/InterruptedIOException", 0);
                return;
            }
            llen -= n;
            loff += n;
        }
        len -= chunkLen;
        off += chunkLen;
    }
}

