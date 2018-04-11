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

#include "jvm.h"
#include "jni_util.h"
#include "net_util.h"

#include "java_net_Inet6AddressImpl.h"
#include <mpe_socket.h>
#include <mpeos_socket.h>

#include "jni_statics.h"

/************************************************************************
 * Inet6AddressImpl
 */

/*
 * Class:     java_net_Inet6AddressImpl
 * Method:    getLocalHostName
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_java_net_Inet6AddressImpl_getLocalHostName(JNIEnv *env, jobject this)
{
    char hostname[MPE_SOCKET_MAXHOSTNAMELEN + 1];

    hostname[0] = '\0';
    if (mpe_socketGetHostName(hostname, MPE_SOCKET_MAXHOSTNAMELEN) != MPE_SUCCESS)
    {
        /* Something went wrong, maybe networking is not setup? */
        strcpy(hostname, "localhost");
    }

    return (*env)->NewStringUTF(env, hostname);
}

/*
 * Find an internet address for a given hostname.  Note that this
 * code works for addresses of type INET and INET6.
 *
 * Class:     java_net_Inet6AddressImpl
 * Method:    lookupAllHostAddr
 * Signature: (Ljava/lang/String;)[[B
 */

JNIEXPORT jobjectArray JNICALL
Java_java_net_Inet6AddressImpl_lookupAllHostAddr(JNIEnv *env, jobject this,
        jstring host)
{
    const char *hostname = NULL;
    jobjectArray ret = 0;
    mpe_SocketAddrInfo* aiArray = NULL;
    mpe_SocketAddrInfo* ai = NULL;
    int rc = 0;
    int i = 0;

    if (IS_NULL(host))
    {
        JNU_ThrowNullPointerException(env, "host is null");
        return 0;
    }

    hostname = JNU_GetStringPlatformChars(env, host, JNI_FALSE);

    if (0 == (rc = mpe_socketGetAddrInfo(hostname, NULL, NULL, &aiArray)))
    {
        jclass byteArrayCls = byteArrayCls = (*env)->FindClass(env, "[B");

        // find the length of the addresses returned...
        for (i = 0, ai = aiArray; ai != NULL; ai = ai->ai_next)
        {
            if ((AF_INET6 == ai->ai_family) || (AF_INET == ai->ai_family))
            {
                i++;    // only count IPv6 and IPv4 addresses
            }
        }

        if (IS_NULL(ret = (*env)->NewObjectArray(env, i, byteArrayCls, NULL)))
        {
            JNU_ReleaseStringPlatformChars(env, host, hostname);
            return NULL;
        }

        // load IPv4 addresses first (bias)
        for (i = 0, ai = aiArray; ai != NULL; ai = ai->ai_next)
        {
            jbyte hoststr[sizeof(mpe_SocketIPv6Addr)] = {0};
            jbyteArray barray = NULL;
            int len = 0;

            if (AF_INET == ai->ai_family)
            {
                len = sizeof(mpe_SocketIPv4Addr);
                barray = (*env)->NewByteArray(env, len);

                if (IS_NULL(barray))
                {
                    JNU_ReleaseStringPlatformChars(env, host, hostname);
                    return NULL;
                }

                // load our byte array initializer (with a ntoh conversion)
                hoststr[3] =
                    ((((struct sockaddr_in*)ai->ai_addr)->sin_addr).s_addr >> 24) & 0xFF;
                hoststr[2] =
                    ((((struct sockaddr_in*)ai->ai_addr)->sin_addr).s_addr >> 16) & 0xFF;
                hoststr[1] =
                    ((((struct sockaddr_in*)ai->ai_addr)->sin_addr).s_addr >> 8) & 0xFF;
                hoststr[0] =
                    ((((struct sockaddr_in*)ai->ai_addr)->sin_addr).s_addr) & 0xFF;
                (*env)->SetByteArrayRegion(env, barray, 0, len, hoststr);
                (*env)->SetObjectArrayElement(env, ret, i, barray);
                
                i++;
            }
        }

        // now load IPv6 addresses (don't reset 'i' to 0, it's the index
        // into the output array...
        for (ai = aiArray; ai != NULL; ai = ai->ai_next)
        {
            jbyte hoststr[sizeof(mpe_SocketIPv6Addr)] = {0};
            jbyteArray barray = NULL;
            int len = 0;

            if (AF_INET6 == ai->ai_family)
            {
                int j = 0;
                len = sizeof(mpe_SocketIPv6Addr);
                barray = (*env)->NewByteArray(env, len);

                if (IS_NULL(barray))
                {
                    JNU_ReleaseStringPlatformChars(env, host, hostname);
                    return NULL;
                }

                for (j = 0; j < sizeof(mpe_SocketIPv6Addr); j++)
                {
                    hoststr[j] =
                        (((struct sockaddr_in6*)ai->ai_addr)->sin6_addr).s6_addr[j];
                }

                (*env)->SetByteArrayRegion(env, barray, 0, len, hoststr);
                (*env)->SetObjectArrayElement(env, ret, i, barray);

                i++;
            }
        }

        mpe_socketFreeAddrInfo(aiArray);
    }
    else
    {
        JNU_ThrowByName(env, JNU_JAVANETPKG "UnknownHostException",
                (char *)hostname);
        ret = NULL;
    }

    return ret;
}

/*
 * Class:     java_net_Inet6AddressImpl
 * Method:    getHostByAddr
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_java_net_Inet6AddressImpl_getHostByAddr(JNIEnv *env, jobject this,
        jbyteArray addrArray)
{
    jstring ret = NULL;
    mpe_SocketHostEntry *entry = NULL;
    mpe_SocketIPv6Addr addr = {0};
    int i = 0;
    jbyte* bytes = NULL;

    /* Validate input array length */
    if ((*env)->GetArrayLength(env,addrArray) != 16)
    {
        // We need to wrap an IPv4 addr inside an IPv6 addr...
        int j;
        bytes = (*env)->GetByteArrayElements(env, addrArray, NULL);

        // zeros in upper 10 bytes
        for (i = 0; i < 10; ++i)
        {
            addr.s6_addr[i] = 0;
        }
        // FF's in next 2 bytes
        for (i = 10; i < 12; ++i)
        {
            addr.s6_addr[i] = 0xFF;
        }
        // the IPv4 addr in the last 4 bytes
        for (i = 12, j = 0; i < 16; ++i, ++j)
        {
            addr.s6_addr[i] = bytes[j];
        }
    }
    else
    {
        bytes = (*env)->GetByteArrayElements(env, addrArray, NULL);
        for (i = 0; i < 16; ++i)
        {
            addr.s6_addr[i] = bytes[i];
        }
    }
    entry = mpe_socketGetHostByAddr((const void *)&addr, sizeof(addr.s6_addr),
            MPE_SOCKET_AF_INET6);

    if (entry)
    {
        ret = (*env)->NewStringUTF(env, entry->h_name);
    }
    else
    {
        JNU_ThrowByName(env, JNU_JAVANETPKG "UnknownHostException", NULL);
    }

    return ret;
}

