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
#include <mpe_dbg.h>
#include <mpe_os.h>
#include "jni_util.h"
#include "jvm.h"
#include "net_util.h"

#include "jni_statics.h"
#include "java_net_InetAddress.h"
#include "java_net_Inet4Address.h"
#include "java_net_Inet6Address.h"
#include "java_net_SocketOptions.h"

int initialized = 0;
void init(JNIEnv *env)
{
    if (!initialized)
    {
        Java_java_net_InetAddress_init(env, 0);
        Java_java_net_Inet4Address_init(env, 0);
        Java_java_net_Inet6Address_init(env, 0);
        initialized = 1;
    }
}

char* NET_getLastError(char* defaultMsg)
{
    int error = mpe_socketGetLastError();
    
    switch (error)
    {
    case MPE_SOCKET_EACCES: 
        return "Access denied";
    case MPE_SOCKET_EADDRINUSE: 
        return "Address in use";
    case MPE_SOCKET_EADDRNOTAVAIL: 
        return "Address not available";
    case MPE_SOCKET_EAFNOSUPPORT: 
        return "Address family not supported";
    case MPE_SOCKET_EAGAIN: 
        return "Socket timeout (EAGAIN)";
    case MPE_SOCKET_EALREADY: 
        return "Socket timeout (EALREADY)";
    case MPE_SOCKET_EBADF: 
        return "Bad socket file descriptor";
    case MPE_SOCKET_ECONNABORTED: 
        return "Connection aborted";
    case MPE_SOCKET_ECONNREFUSED: 
        return "Connection refused";
    case MPE_SOCKET_ECONNRESET: 
        return "Connection reset";
    case MPE_SOCKET_EDESTADDRREQ: 
        return "No destination address";
    case MPE_SOCKET_EDOM: 
        return "Invalid timeout value";
    case MPE_SOCKET_EHOSTNOTFOUND: 
        return "Host not found";
    case MPE_SOCKET_EHOSTUNREACH: 
        return "Host unreachable";
    case MPE_SOCKET_EINTR: 
        return "Socket interrupted";
    case MPE_SOCKET_EISCONN: 
        return "Already connected";
    case MPE_SOCKET_EINPROGRESS: 
        return "Socket operation in progress";
    case MPE_SOCKET_ELOOP: 
        return "Loop exists";
    case MPE_SOCKET_EMFILE: 
        return "Too many files open";
    case MPE_SOCKET_EMSGSIZE: 
        return "Message too long";
    case MPE_SOCKET_ENAMETOOLONG: 
        return "Name too long";
    case MPE_SOCKET_ENETDOWN: 
        return "Network down";
    case MPE_SOCKET_ENETUNREACH: 
        return "Network unreachable";
    case MPE_SOCKET_ENOBUFS: 
        return "No buffers available";
    case MPE_SOCKET_ENOPROTOOPT: 
        return "Invalid protocol option";
    case MPE_SOCKET_ENOTCONN: 
        return "Not connnected";
    case MPE_SOCKET_ENOTSOCK: 
        return "Not socket";
    case MPE_SOCKET_EOPNOTSUPP: 
        return "Operation not supported";
    case MPE_SOCKET_EPROTONOSUPPORT: 
        return "Protocol not supported";
    case MPE_SOCKET_EPROTOTYPE: 
        return "Protocol wrong type for socket";
    case MPE_SOCKET_ETIMEDOUT: 
        return "Timeout";
    default:
        return defaultMsg;
    }
}

void NET_ThrowByNameWithLastError(JNIEnv *env, const char *name,
        const char *defaultDetail)
{
    JNU_ThrowByName(env, name, NET_getLastError((char*)defaultDetail));
}

void NET_ThrowCurrent(JNIEnv *env, char *msg)
{
    NET_ThrowNew(env, mpe_socketGetLastError(), msg);
}

JNIEXPORT void JNICALL
NET_ThrowNew(JNIEnv *env, int errorNumber, char *msg)
{
    char fullMsg[512];
    if (!msg)
    {
        msg = "no further information";
    }
    switch(errorNumber)
    {
        case MPE_SOCKET_EBADF:
        jio_snprintf(fullMsg, sizeof(fullMsg), "socket closed: %s", msg);
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", fullMsg);
        break;
        case MPE_SOCKET_EINTR:
        JNU_ThrowByName(env, JNU_JAVAIOPKG "InterruptedIOException", msg);
        break;
        default:
        jio_snprintf(fullMsg, sizeof(fullMsg), "%s", msg);
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", fullMsg);
        break;
    }
}

jfieldID NET_GetFileDescriptorID(JNIEnv *env)
{
    jclass cls = (*env)->FindClass(env, "java/io/FileDescriptor");
    CHECK_NULL_RETURN(cls, NULL);
    return (*env)->GetFieldID(env, cls, "fd", "I");
}

jint NET_GetAddressFamily(JNIEnv *env, jobject iaObj)
{
    jint id = (*env)->GetIntField(env, iaObj,
                                 JNI_STATIC(java_net_InetAddress, ia_familyID));
    return id == IPv6? MPE_SOCKET_AF_INET6 : MPE_SOCKET_AF_INET4;
}

void NET_InetAddressToSockaddr(JNIEnv *env, jobject iaObj, int port,
        struct sockaddr *him, int *len)
{
    jint family = NET_GetAddressFamily(env, iaObj);

    if (family == MPE_SOCKET_AF_INET6)
    {
        //struct sockaddr_in6 *him6 = (struct sockaddr_in6 *)him;
        mpe_SocketIPv6SockAddr *him6 = (mpe_SocketIPv6SockAddr*)him;
        jbyteArray ipaddress;
        jbyte caddr[16];
        jint address;

        /* needs work. 1. family 2. clean up him6 etc deallocate memory */
        ipaddress = (*env)->GetObjectField(env, iaObj,
                            JNI_STATIC(java_net_Inet6Address, ia6_ipaddressID));
        (*env)->GetByteArrayRegion(env, ipaddress, 0, 16, caddr);
        memset((char *)him6, 0, sizeof(mpe_SocketIPv6SockAddr));
        him6->sin6_port = mpe_socketHtoNS(port);
        memcpy((void *)&(him6->sin6_addr), caddr, sizeof(mpe_SocketIPv6SockAddr) );
        him6->sin6_family = AF_INET6;
        *len = sizeof(mpe_SocketIPv6SockAddr);
    }
    else
    {
        //struct sockaddr_in *him4 = (struct sockaddr_in*)him;
        mpe_SocketIPv4SockAddr *him4 = (mpe_SocketIPv4SockAddr*) him;
        jint address;

        memset((char *) him4, 0, sizeof(mpe_SocketIPv4SockAddr));
        address = (*env)->GetIntField(env, iaObj, JNI_STATIC(
                java_net_InetAddress, ia_addressID));
        him4->sin_port = mpe_socketHtoNS((short) port);
        him4->sin_addr.s_addr = (uint32_t) mpe_socketHtoNL(address);
        him4->sin_family = MPE_SOCKET_AF_INET4;
        *len = sizeof(mpe_SocketIPv4SockAddr);
    }
}

jobject NET_SockaddrToInetAddress(JNIEnv *env, struct sockaddr *him,
        int *port)
{
    jobject iaObj;
    init(env);

    if (him->sa_family == MPE_SOCKET_AF_INET6)
    {
        jbyteArray ipaddress;
        mpe_SocketIPv6SockAddr *him6 = (mpe_SocketIPv6SockAddr *)him;
        jbyte *caddr = (jbyte *)&(him6->sin6_addr);
        if (NET_IsIPv4Mapped(caddr))
        {
            int address;
            static jclass inet4Cls = 0;
            if (inet4Cls == 0)
            {
                jclass c = (*env)->FindClass(env, "java/net/Inet4Address");
                CHECK_NULL_RETURN(c, NULL);
                inet4Cls = (*env)->NewGlobalRef(env, c);
                CHECK_NULL_RETURN(inet4Cls, NULL);
                (*env)->DeleteLocalRef(env, c);
            }
            iaObj = (*env)->NewObject(env, inet4Cls, JNI_STATIC(java_net_Inet4Address, ia4_ctrID));
            CHECK_NULL_RETURN(iaObj, NULL);
            address = NET_IPv4MappedToIPv4(caddr);
            (*env)->SetIntField(env, iaObj, JNI_STATIC(java_net_InetAddress, ia_addressID), address);
            (*env)->SetIntField(env, iaObj, JNI_STATIC(java_net_InetAddress, ia_familyID), IPv4);
        }
        else
        {
            static jclass inet6Cls = 0;
            if (inet6Cls == 0)
            {
                jclass c = (*env)->FindClass(env, "java/net/Inet6Address");
                CHECK_NULL_RETURN(c, NULL);
                inet6Cls = (*env)->NewGlobalRef(env, c);
                CHECK_NULL_RETURN(inet6Cls, NULL);
                (*env)->DeleteLocalRef(env, c);
            }
            iaObj = (*env)->NewObject(env, inet6Cls, JNI_STATIC(java_net_Inet6Address, ia6_ctrID));
            CHECK_NULL_RETURN(iaObj, NULL);
            ipaddress = (*env)->NewByteArray(env, 16);
            CHECK_NULL_RETURN(ipaddress, NULL);
            (*env)->SetByteArrayRegion(env, ipaddress, 0, 16, (jbyte *)&(him6->sin6_addr));

            (*env)->SetObjectField(env, iaObj, JNI_STATIC(java_net_Inet6Address, ia6_ipaddressID), ipaddress);

            (*env)->SetIntField(env, iaObj, JNI_STATIC(java_net_InetAddress, ia_familyID), IPv6);
        }
        *port = ntohs(him6->sin6_port);
    }
    else
    {
        mpe_SocketIPv4SockAddr *him4 = (mpe_SocketIPv4SockAddr *) him;
        static jclass inet4Cls = 0;

        if (inet4Cls == 0)
        {
            jclass c = (*env)->FindClass(env, "java/net/Inet4Address");
            CHECK_NULL_RETURN(c, NULL);
            inet4Cls = (*env)->NewGlobalRef(env, c);
            CHECK_NULL_RETURN(inet4Cls, NULL);
            (*env)->DeleteLocalRef(env, c);
        }
        iaObj = (*env)->NewObject(env, inet4Cls, JNI_STATIC(
                java_net_Inet4Address, ia4_ctrID));
        CHECK_NULL_RETURN(iaObj, NULL);
        (*env)->SetIntField(env, iaObj, JNI_STATIC(java_net_InetAddress,
                ia_familyID), IPv4);
        (*env)->SetIntField(env, iaObj, JNI_STATIC(java_net_InetAddress,
                ia_addressID), ntohl(him4->sin_addr.s_addr));
        *port = ntohs(him4->sin_port);
    }
    return iaObj;
}

jint NET_SockaddrEqualsInetAddress(JNIEnv *env, struct sockaddr *him,
        jobject iaObj)
{
    jint family = NET_GetAddressFamily(env, iaObj);

    if (him->sa_family == MPE_SOCKET_AF_INET6)
    {
        mpe_SocketIPv6SockAddr *him6 = (mpe_SocketIPv6SockAddr *)him;
        jbyte *caddrNew = (jbyte *)&(him6->sin6_addr);
        if (NET_IsIPv4Mapped(caddrNew))
        {
            int addrNew;
            int addrCur;
            if (family == MPE_SOCKET_AF_INET6)
            {
                return JNI_FALSE;
            }
            addrNew = NET_IPv4MappedToIPv4(caddrNew);
            addrCur = (*env)->GetIntField(env, iaObj, JNI_STATIC(java_net_InetAddress, ia_addressID));
            if (addrNew == addrCur)
            {
                return JNI_TRUE;
            }
            else
            {
                return JNI_FALSE;
            }
        }
        else
        {
            jbyteArray ipaddress;
            jbyte caddrCur[16];

            if (family == MPE_SOCKET_AF_INET4)
            {
                return JNI_FALSE;
            }
            ipaddress = (*env)->GetObjectField(env, iaObj, JNI_STATIC(java_net_Inet6Address, ia6_ipaddressID));
            (*env)->GetByteArrayRegion(env, ipaddress, 0, 16, caddrCur);
            if (NET_IsEqual(caddrNew, caddrCur))
            {
                return JNI_TRUE;
            }
            else
            {
                return JNI_FALSE;
            }
        }
    }
    else
    {
        mpe_SocketIPv4SockAddr *him4 = (mpe_SocketIPv4SockAddr *) him;
        int addrNew, addrCur;
        if (family != MPE_SOCKET_AF_INET4)
        {
            return JNI_FALSE;
        }
        addrNew = ntohl(him4->sin_addr.s_addr);
        addrCur = (*env)->GetIntField(env, iaObj, JNI_STATIC(
                java_net_InetAddress, ia_addressID));
        if (addrNew == addrCur)
        {
            return JNI_TRUE;
        }
        else
        {
            return JNI_FALSE;
        }
    }
}

void NET_SetTrafficClass(struct sockaddr *him, int trafficClass)
{
    if (him->sa_family == MPE_SOCKET_AF_INET6)
    {
        mpe_SocketIPv6SockAddr *him6 = (mpe_SocketIPv6SockAddr*)him;
        him6->sin6_flowinfo = mpe_socketHtoNL((trafficClass & 0xff) << 20);
    }
}

jint NET_GetPortFromSockaddr(struct sockaddr *him)
{
    if (him->sa_family == MPE_SOCKET_AF_INET6)
    {
        return ntohs(((mpe_SocketIPv6SockAddr*)him)->sin6_port);
    }
    else
    {
        return ntohs(((mpe_SocketIPv4SockAddr*) him)->sin_port);
    }
}

int NET_IsIPv4Mapped(jbyte* caddr)
{
    int i;
    for (i = 0; i < 10; i++)
    {
        if (caddr[i] != 0x00)
            return 0; /* false */
    }

    if (((caddr[10] & 0xff) == 0xff) && ((caddr[11] & 0xff) == 0xff))
    {
        return 1; /* true */
    }
    return 0; /* false */
}

int NET_IPv4MappedToIPv4(jbyte* caddr)
{
    return ((caddr[12] & 0xff) << 24) | ((caddr[13] & 0xff) << 16)
            | ((caddr[14] & 0xff) << 8) | (caddr[15] & 0xff);
}

int NET_IsEqual(jbyte* caddr1, jbyte* caddr2)
{
    int i;
    for (i = 0; i < 16; i++)
    {
        if (caddr1[i] != caddr2[i])
            return 0; /* false */
    }
    return 1;
}

/*
 * Map the Java level socket option to the platform specific
 * level and option name for IPv6 sockets. 
 */
JNIEXPORT int JNICALL
NET_MapSocketOptionV6(jint cmd, int *level, int *optname)
{
    /*
     * Different multicast options if IPv6 is enabled
     */
    switch (cmd)
    {
        case java_net_SocketOptions_IP_MULTICAST_IF:
        case java_net_SocketOptions_IP_MULTICAST_IF2:
            *level = MPE_SOCKET_IPPROTO_IPV6;
            *optname = MPE_SOCKET_IPV6_MULTICAST_IF;
            return 0;

        case java_net_SocketOptions_IP_MULTICAST_LOOP:
            *level = MPE_SOCKET_IPPROTO_IPV6;
            *optname = MPE_SOCKET_IPV6_MULTICAST_LOOP;
            return 0;
    }

    return NET_MapSocketOption(cmd, level, optname);
}

/*
 * Map the Java level socket option to the platform specific
 * level and option name. 
 */
JNIEXPORT int JNICALL
NET_MapSocketOption(jint cmd, int *level, int *optname)
{
    static struct
    {
        jint cmd;
        int level;
        int optname;
    }const opts[] =
    {
        {   java_net_SocketOptions_TCP_NODELAY, MPE_SOCKET_IPPROTO_TCP, MPE_SOCKET_TCP_NODELAY},
        {   java_net_SocketOptions_SO_OOBINLINE, MPE_SOCKET_SOL_SOCKET, MPE_SOCKET_SO_OOBINLINE},
        {   java_net_SocketOptions_SO_LINGER, MPE_SOCKET_SOL_SOCKET, MPE_SOCKET_SO_LINGER},
        {   java_net_SocketOptions_SO_SNDBUF, MPE_SOCKET_SOL_SOCKET, MPE_SOCKET_SO_SNDBUF},
        {   java_net_SocketOptions_SO_RCVBUF, MPE_SOCKET_SOL_SOCKET, MPE_SOCKET_SO_RCVBUF},
        {   java_net_SocketOptions_SO_KEEPALIVE, MPE_SOCKET_SOL_SOCKET, MPE_SOCKET_SO_KEEPALIVE},
        {   java_net_SocketOptions_SO_REUSEADDR, MPE_SOCKET_SOL_SOCKET, MPE_SOCKET_SO_REUSEADDR},
        {   java_net_SocketOptions_SO_BROADCAST, MPE_SOCKET_SOL_SOCKET, MPE_SOCKET_SO_BROADCAST},
        {   java_net_SocketOptions_IP_TOS, MPE_SOCKET_SOL_SOCKET, MPE_SOCKET_SO_TYPE},
        {   java_net_SocketOptions_IP_MULTICAST_IF, MPE_SOCKET_IPPROTO_IPV4, MPE_SOCKET_IPV4_MULTICAST_IF},
        {   java_net_SocketOptions_IP_MULTICAST_LOOP, MPE_SOCKET_IPPROTO_IPV4, MPE_SOCKET_IPV4_MULTICAST_LOOP},
    };

    int i;

    /*
     * Map the Java level option to the native level 
     */
    for (i=0; i<(int)(sizeof(opts) / sizeof(opts[0])); i++)
    {
        if (cmd == opts[i].cmd)
        {
            *level = opts[i].level;
            *optname = opts[i].optname;
            return 0;
        }
    }

    /* not found */
    return -1;
}

/*
 * The following functions cannot be mapped to JVM_ or CVMnet equivelents with 
 * macros because of prototype definitions in the shared net_util.h header file
 * located in src/share/native/java/net.
 */

JNIEXPORT int JNICALL
NET_GetSockOpt(int fd, int level, int opt, void *result, int *len)
{
    return JVM_GetSockOpt(fd, level, opt, result, (mpe_SocketSockLen*)len);
}

JNIEXPORT int JNICALL
NET_SetSockOpt(int fd, int level, int opt, const void *arg, int len)
{
    return JVM_SetSockOpt(fd, level, opt, arg, len);
}

JNIEXPORT int JNICALL
NET_Bind(int fd, struct sockaddr *him, int len)
{
    return JVM_Bind(fd, him, len);
}
