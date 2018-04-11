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

#include <mpe_socket.h>
#include <mpeos_socket.h>
#include "jvm.h"
#include "jni_util.h"
#include "net_util.h"

#include "java_net_Inet4AddressImpl.h"
#include "jni_statics.h"

/************************************************************************
 * Inet4AddressImpl
 */

/*
 * Class:     java_net_Inet4AddressImpl
 * Method:    getLocalHostName
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_java_net_Inet4AddressImpl_getLocalHostName(JNIEnv *env, jobject this)
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
 * Class:     java_net_InetAddressImpl
 * Method:    makeAnyLocalAddress
 * Signature: (Ljava/net/InetAddress;)V
 *
 * This doesn't appear to justify its own existence.
 */
JNIEXPORT void JNICALL
Java_java_net_InetAddressImpl_makeAnyLocalAddress(JNIEnv *env, jobject this, jobject iaObj)
{
    if (IS_NULL(iaObj))
    {
        JNU_ThrowNullPointerException(env, "inet address argument");
    }
    else
    {
        (*env)->SetIntField(env, iaObj, JNI_STATIC(java_net_InetAddress, ia_addressID), MPE_SOCKET_IN4ADDR_ANY);
        (*env)->SetIntField(env, iaObj, JNI_STATIC(java_net_InetAddress, ia_familyID), MPE_SOCKET_AF_INET4);
    }
}

/*
 * Class:     java_net_InetAddressImpl
 * Method:    getInetFamily
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_java_net_InetAddressImpl_getInetFamily(JNIEnv *env, jobject this)
{
    return MPE_SOCKET_AF_INET4;
}

/*
 * Find an internet address for a given hostname.  Not this this
 * code only works for addresses of type INET. The translation
 * of %d.%d.%d.%d to an address (int) occurs in java now, so the
 * String "host" shouldn't *ever* be a %d.%d.%d.%d string
 *
 * Class:     java_net_Inet4AddressImpl
 * Method:    lookupAllHostAddr
 * Signature: (Ljava/lang/String;)[[B
 */

JNIEXPORT jobjectArray JNICALL
Java_java_net_Inet4AddressImpl_lookupAllHostAddr(JNIEnv *env, jobject this, jstring host)
{
    const char *hostname;
    jobjectArray ret = 0;
    jclass byteArrayCls;
    mpe_SocketHostEntry *entry;

    if (IS_NULL(host))
    {
        JNU_ThrowNullPointerException(env, "host is null");
        return 0;
    }
    hostname = JNU_GetStringPlatformChars(env, host, JNI_FALSE);

    entry = mpe_socketGetHostByName(hostname);

    if (entry != NULL)
    {
        int len = sizeof(mpe_SocketIPv4Addr);
        int i = 0;

        mpe_SocketIPv4Addr **addrp = (mpe_SocketIPv4Addr **)entry->h_addr_list;
        while (*addrp != NULL)
        {
            i++;
            addrp++;
        }

        byteArrayCls = (*env)->FindClass(env, "[B");
        ret = (*env)->NewObjectArray(env, i, byteArrayCls, NULL);

        if (IS_NULL(ret))
        {
            JNU_ReleaseStringPlatformChars(env, host, hostname);
            return NULL;
        }
        addrp = (mpe_SocketIPv4Addr **)entry->h_addr_list;
        i = 0;
        while (*addrp != NULL)
        {
            jbyteArray barray = (*env)->NewByteArray(env, len);
            if (IS_NULL(barray))
            {
                JNU_ReleaseStringPlatformChars(env, host, hostname);
                return NULL;
            }
            (*env)->SetByteArrayRegion(env, barray, 0, len, (jbyte *)(*addrp));
            (*env)->SetObjectArrayElement(env, ret, i, barray);
            addrp++;
            i++;
        }
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
 * Class:     java_net_Inet4AddressImpl
 * Method:    getHostByAddr
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_java_net_Inet4AddressImpl_getHostByAddr(JNIEnv *env, jobject this, jbyteArray addrArray)
{
    jstring ret = NULL;
    mpe_SocketHostEntry *entry;
    mpe_SocketIPv4Addr addr;

    jbyte* bytes = (*env)->GetByteArrayElements(env, addrArray, NULL);
    addr.s_addr = bytes[0] << 24 |
    bytes[1] << 16 |
    bytes[2] << 8 |
    bytes[3];

    entry = mpe_socketGetHostByAddr((const void *)&addr, sizeof(addr.s_addr), MPE_SOCKET_AF_INET4);
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

