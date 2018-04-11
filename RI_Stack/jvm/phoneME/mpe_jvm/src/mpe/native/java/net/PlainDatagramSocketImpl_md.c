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

#include "java_net_InetAddress.h"
#include "java_net_Inet4Address.h"
#include "java_net_Inet6Address.h"
#include "java_net_SocketOptions.h"
#include "java_net_PlainDatagramSocketImpl.h"
#include "java_net_NetworkInterface.h"

#ifdef __linux__
#include <arpa/inet.h>
#include <net/route.h>
#include <sys/utsname.h>

#ifndef IPV6_FLOWINFO_SEND
#define IPV6_FLOWINFO_SEND      33
#endif

#endif
/************************************************************************
 * PlainDatagramSocketImpl
 */

#include "jni_statics.h"


/*
 * Returns a java.lang.Integer based on 'i'
 */
static jobject createInteger(JNIEnv *env, int i)
{
    static jclass i_class;
    static jmethodID i_ctrID;

    if (i_class == NULL)
    {
        jclass c = (*env)->FindClass(env, "java/lang/Integer");
        CHECK_NULL_RETURN(c, NULL);
        i_ctrID = (*env)->GetMethodID(env, c, "<init>", "(I)V");
        CHECK_NULL_RETURN(i_ctrID, NULL);
        i_class = (*env)->NewGlobalRef(env, c);
        CHECK_NULL_RETURN(i_class, NULL);
    }

    return ((*env)->NewObject(env, i_class, i_ctrID, i));
}

/*
 * Returns a java.lang.Boolean based on 'b'
 */
static jobject createBoolean(JNIEnv *env, int b)
{
    static jclass b_class;
    static jmethodID b_ctrID;

    if (b_class == NULL)
    {
        jclass c = (*env)->FindClass(env, "java/lang/Boolean");
        CHECK_NULL_RETURN(c, NULL);
        b_ctrID = (*env)->GetMethodID(env, c, "<init>", "(Z)V");
        CHECK_NULL_RETURN(b_ctrID, NULL);
        b_class = (*env)->NewGlobalRef(env, c);
        CHECK_NULL_RETURN(b_class, NULL);
    }

    return ((*env)->NewObject(env, b_class, b_ctrID, (jboolean)(b != 0)));
}

/*
 * Returns the fd for a PlainDatagramSocketImpl or -1
 * if closed.
 */
static int getFD(JNIEnv *env, jobject this)
{
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID));
    if (fdObj == NULL)
    return -1;

    return(*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID));
}

static jboolean isIPv6Socket(JNIEnv *env, jint fd)
{
    /* find out local IP address */
    mpe_SocketSockAddr him;
    mpe_SocketSockLen len = SOCKADDR_LEN;
    int port;
    jobject iaObj;

    if (NET_GetSockName(fd, (mpe_SocketSockAddr *)&him, &len) == -1)
    {
        NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                "Error getting socket name");
        return false;
    }

    iaObj = NET_SockaddrToInetAddress(env, (mpe_SocketSockAddr *)&him, &port);
    return ((NET_GetAddressFamily(env, iaObj)) == MPE_SOCKET_AF_INET6);
}

/* Do we need this any longer???
 * the maximum buffer size. Used for setting
 * SendBufferSize and ReceiveBufferSize.
 */
static const int max_buffer_size = 64 * 1024;

/*
 * Determine the default interface for an IPv6 address.
 *
 * 1. Scans /proc/net/ipv6_route for a matching route
 *    (eg: fe80::/10 or a route for the specific address).
 *    This will tell us the interface to use (eg: "eth0").
 * 
 * 2. Lookup /proc/net/if_inet6 to map the interface
 *    name to an interface index.
 *
 * Returns :-
 *	-1 if error 
 *	 0 if no matching interface
 *      >1 interface index to use for the link-local address.
 */
#if defined(__linux__)
static int getDefaultIPv6Interface(struct in6_addr *target_addr) {
    FILE *f;
    char srcp[8][5];
    char hopp[8][5];
    int dest_plen, src_plen, use, refcnt, metric;
    unsigned long flags;
    char dest_str[40];
    struct in6_addr dest_addr;
    char device[16];
    jboolean match = JNI_FALSE;

    /*
     * Scan /proc/net/ipv6_route looking for a matching
     * route.
     */
    if ((f = fopen("/proc/net/ipv6_route", "r")) == NULL) {
	return -1;
    }
    while (fscanf(f, "%4s%4s%4s%4s%4s%4s%4s%4s %02x "
                     "%4s%4s%4s%4s%4s%4s%4s%4s %02x "
                     "%4s%4s%4s%4s%4s%4s%4s%4s "
                     "%08x %08x %08x %08lx %8s",
		     dest_str, &dest_str[5], &dest_str[10], &dest_str[15],
		     &dest_str[20], &dest_str[25], &dest_str[30], &dest_str[35],
		     &dest_plen,
		     srcp[0], srcp[1], srcp[2], srcp[3],
		     srcp[4], srcp[5], srcp[6], srcp[7],
		     &src_plen,
		     hopp[0], hopp[1], hopp[2], hopp[3],
		     hopp[4], hopp[5], hopp[6], hopp[7],
		     &metric, &use, &refcnt, &flags, device) == 31) {

	/*
 	 * Some routes should be ignored
	 */
	if ( (dest_plen < 0 || dest_plen > 128)  ||
	     (src_plen != 0) ||			
	     (flags & (RTF_POLICY | RTF_FLOW)) ||
	     ((flags & RTF_REJECT) && dest_plen == 0) ) {
	    continue;
	}

	/*
  	 * Convert the destination address
	 */
	dest_str[4] = ':';
	dest_str[9] = ':';
	dest_str[14] = ':';
	dest_str[19] = ':';
	dest_str[24] = ':';
	dest_str[29] = ':';
	dest_str[34] = ':';
	dest_str[39] = '\0';

	if (inet_pton(AF_INET6, dest_str, &dest_addr) < 0) {
	    /* not an Ipv6 address */
	    continue;
	} else {
	    /*
	     * The prefix len (dest_plen) indicates the number of bits we 
 	     * need to match on.
	     *
	     * dest_plen / 8	=> number of bytes to match
	     * dest_plen % 8	=> number of additional bits to match
	     *
	     * eg: fe80::/10 => match 1 byte + 2 additional bits in the
	     *	                the next byte.
	     */
	    int byte_count = dest_plen >> 3;		
	    int extra_bits = dest_plen & 0x3;

	    if (byte_count > 0) {
		if (memcmp(target_addr, &dest_addr, byte_count)) {
                    continue;  /* no match */
                }
	    }

	    if (extra_bits > 0) {
		unsigned char c1 = ((unsigned char *)target_addr)[byte_count];
		unsigned char c2 = ((unsigned char *)&dest_addr)[byte_count];
		unsigned char mask = 0xff << (8 - extra_bits);
		if ((c1 & mask) != (c2 & mask)) {
		    continue;
		}
	    }

	    /*
	     * We have a match
  	     */
	    match = JNI_TRUE;
	    break;
	}
    }
    fclose(f);

    /*
     * If there's a match then we lookup the interface
     * index.
     */
    if (match) {
        /* eliminate compile time warning.
           char addr6[40], devname[20];
        */
        char devname[20];
        char addr6p[8][5];
        int plen, scope, dad_status, if_idx;

        if ((f = fopen("/proc/net/if_inet6", "r")) != NULL) {
            while (fscanf(f, "%4s%4s%4s%4s%4s%4s%4s%4s %02x %02x %02x %02x %20s\n",
                      addr6p[0], addr6p[1], addr6p[2], addr6p[3],
                      addr6p[4], addr6p[5], addr6p[6], addr6p[7],
                  &if_idx, &plen, &scope, &dad_status, devname) == 13) {

		if (strcmp(devname, device) == 0) {	
		    /*
		     * Found - so just return the index
		     */
		    fclose(f);
		    return if_idx;
		}
	    }
	    fclose(f);
        } else {
	    /* 
	     * Couldn't open /proc/net/if_inet6
	     */
	    return -1;
	}
    }

    /*
     * If we get here it means we didn't there wasn't any
     * route or we couldn't get the index of the interface.
     */
    return 0;
}
#endif

/*
 * Class:     java_net_PlainDatagramSocketImpl
 * Method:    init
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainDatagramSocketImpl_init(JNIEnv *env, jclass cls)
{
    JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID)
    = (*env)->GetFieldID(env, cls, "fd", "Ljava/io/FileDescriptor;");
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID));
    JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_timeoutID)
    = (*env)->GetFieldID(env, cls, "timeout", "I");
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_timeoutID));
    JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_trafficClassID)
    = (*env)->GetFieldID(env, cls, "trafficClass", "I");
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_trafficClassID));
    JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_localPortID)
    = (*env)->GetFieldID(env, cls, "localPort", "I");
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_localPortID));
    JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_connected)
    = (*env)->GetFieldID(env, cls, "connected", "Z");
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_connected));
    JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_connectedAddress)
    = (*env)->GetFieldID(env, cls, "connectedAddress", "Ljava/net/InetAddress;");
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_connectedAddress));
    JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_connectedPort)
    = (*env)->GetFieldID(env, cls, "connectedPort", "I");
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_connectedPort));

    JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID) = NET_GetFileDescriptorID(env);
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID));

    Java_java_net_InetAddress_init(env, 0);
    Java_java_net_Inet4Address_init(env, 0);
    Java_java_net_Inet6Address_init(env, 0);
    Java_java_net_NetworkInterface_init(env, 0);

// IPv6 additions...
    JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_multicastInterfaceID ) =
    (*env)->GetFieldID(env, cls, "multicastInterface", "I");
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_multicastInterfaceID));
    JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_loopbackID) =
    (*env)->GetFieldID(env, cls, "loopbackMode", "Z");
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_loopbackID));
    JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_ttlID) =
    (*env)->GetFieldID(env, cls, "ttl", "I");
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_ttlID));
// end IPv6 additions

}

/*
 * Class:     java_net_PlainDatagramSocketImpl
 * Method:    bind
 * Signature: (ILjava/net/InetAddress;)V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainDatagramSocketImpl_bind(JNIEnv *env, jobject this, jint localport, jobject iaObj)
{
    /* fdObj is the FileDescriptor field on this */
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID));
    /* fd is an int field on fdObj */
    int fd;
    int len = 0;
    mpe_SocketSockAddr him;

    if (IS_NULL(fdObj))
    {
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException","Socket closed");
        return;
    }
    else
    {
        fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID));
    }

    if (IS_NULL(iaObj))
    {
        JNU_ThrowNullPointerException(env, "iaObj is null.");
        return;
    }

    /* bind - pick a port number for local addr*/
    NET_InetAddressToSockaddr(env, iaObj, localport, (mpe_SocketSockAddr *)&him, &len);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s to %d.%d.%d.%d:%d\n", __func__,
           (((struct sockaddr_in*)&him)->sin_addr.s_addr & 0xFF),
           (((struct sockaddr_in*)&him)->sin_addr.s_addr >> 8) & 0xFF,
           (((struct sockaddr_in*)&him)->sin_addr.s_addr >> 16) & 0xFF,
           (((struct sockaddr_in*)&him)->sin_addr.s_addr >> 24) & 0xFF,
           localport);
    if (NET_Bind(fd, (mpe_SocketSockAddr *)&him, len) < 0)
    {
        int error = NET_GetLastError();
        if (error == MPE_SOCKET_EADDRINUSE || error == MPE_SOCKET_EADDRNOTAVAIL || error == MPE_SOCKET_EACCES)
        {
            NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "BindException", "Bind failed");
        }
        else
        {
            NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException", "Bind failed");
        }
        return;
    }

    /* intialize the local port */
    if (localport == 0)
    {
        /* Now that we're a connected socket, let's extract the port number
         * that the system chose for us and store it in the Socket object.
         */
        if (NET_GetSockName(fd, (mpe_SocketSockAddr *)&him, &len) == -1)
        {
            NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                    "Error getting socket name");
            return;
        }

        localport = NET_GetPortFromSockaddr((mpe_SocketSockAddr *)&him);
        (*env)->SetIntField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_localPortID), localport);
    }
    else
    {
        (*env)->SetIntField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_localPortID), localport);
    }
}

/*
 * Class:     java_net_PlainDatagramSocketImpl
 * Method:    connect0
 * Signature: (Ljava/net/InetAddress;I)V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainDatagramSocketImpl_connect0(JNIEnv *env, jobject this, jobject address, jint port)
{
    /*The object's field */
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID));
    /* The fdObj'fd */
    jint fd;
    /* The packetAddress address, family and port */
    mpe_SocketSockAddr rmtaddr;
    int len = 0;

    if (IS_NULL(fdObj))
    {
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException","Socket closed");
        return;
    }
    fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID));

    if (IS_NULL(address))
    {
        JNU_ThrowNullPointerException(env, "address");
        return;
    }

    NET_InetAddressToSockaddr(env, address, port, (mpe_SocketSockAddr *)&rmtaddr, &len);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s to %d.%d.%d.%d:%d\n", __func__,
           (((struct sockaddr_in*)&rmtaddr)->sin_addr.s_addr & 0xFF),
           (((struct sockaddr_in*)&rmtaddr)->sin_addr.s_addr >> 8) & 0xFF,
           (((struct sockaddr_in*)&rmtaddr)->sin_addr.s_addr >> 16) & 0xFF,
           (((struct sockaddr_in*)&rmtaddr)->sin_addr.s_addr >> 24) & 0xFF,
           port);
    if (NET_Connect(fd, (mpe_SocketSockAddr *)&rmtaddr, len) == -1)
    {
        NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                "Connection problem encountered: No network connection detected.");
        return;
    }
}

/*
 * Class:     java_net_PlainDatagramSocketImpl
 * Method:    disconnect0
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainDatagramSocketImpl_disconnect0(JNIEnv *env, jobject this)
{
    /* The object's field */
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID));
    /* The fdObj'fd */
    jint fd;
    mpe_SocketSockAddr addr;
    mpe_SocketSockLen len = 0;

    if (IS_NULL(fdObj))
    return;

    fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID));

    memset(&addr, 0, sizeof(addr));
    {
        /* find out local IP address */
        int port;
        jobject iaObj;

        if (NET_GetSockName(fd, (mpe_SocketSockAddr *)&addr, &len) == -1)
        {
            NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                    "Error getting socket name");
            return;
        }

        iaObj = NET_SockaddrToInetAddress(env, (mpe_SocketSockAddr *)&addr, &port);

        /*
         * Map the Java level socket option to the platform specific
         * level and option name.
         */
        if ((NET_GetAddressFamily(env, iaObj)) == MPE_SOCKET_AF_INET6)
        {
            mpe_SocketIPv6SockAddr *him6 = (mpe_SocketIPv6SockAddr *)&addr;
            him6->sin6_family = MPE_SOCKET_AF_INET6;
            len = sizeof(mpe_SocketIPv6SockAddr);
        }
        else
        {
            mpe_SocketIPv4SockAddr *him4 = (mpe_SocketIPv4SockAddr*)&addr;
            him4->sin_family = MPE_SOCKET_AF_INET4;
            len = sizeof(mpe_SocketIPv4SockAddr);
        }
    }
    NET_Connect(fd, (mpe_SocketSockAddr *)&addr, len);
}

/*
 * Class:     java_net_PlainDatagramSocketImpl
 * Method:    send
 * Signature: (Ljava/net/DatagramPacket;)V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainDatagramSocketImpl_send(JNIEnv *env, jobject this, jobject packet)
{
    char BUF[MAX_BUFFER_LEN];
    char *fullPacket = NULL;
    int ret, mallocedPacket = JNI_FALSE;
    /* The object's field */
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID));
    jint trafficClass = (*env)->GetIntField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_trafficClassID));

    jbyteArray packetBuffer;
    jobject packetAddress;
    jint packetBufferOffset, packetBufferLen, packetPort;
    jboolean connected;

    /* The fdObj'fd */
    jint fd;

    mpe_SocketSockAddr rmtaddr, *rmtaddrP=&rmtaddr;
    int len;

    if (IS_NULL(fdObj))
    {
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException","Socket closed");
        return;
    }
    fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID));

    if (IS_NULL(packet))
    {
        JNU_ThrowNullPointerException(env, "packet");
        return;
    }

    connected = (*env)->GetBooleanField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_connected));

    packetBuffer = (*env)->GetObjectField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_bufID));
    packetAddress = (*env)->GetObjectField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_addressID));
    if (IS_NULL(packetBuffer) || IS_NULL(packetAddress))
    {
        JNU_ThrowNullPointerException(env, "null buffer || null address");
        return;
    }

    packetBufferOffset = (*env)->GetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_offsetID));
    packetBufferLen = (*env)->GetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_lengthID));

    if (connected)
    {
        /* arg to NET_Sendto () null in this case */
        len = 0;
        rmtaddrP = 0;
    }
    else
    {
        packetPort = (*env)->GetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_portID));
        NET_InetAddressToSockaddr(env, packetAddress, packetPort, (mpe_SocketSockAddr *)&rmtaddr, &len);
    }

    if (packetBufferLen > MAX_BUFFER_LEN)
    {
        /* When JNI-ifying the JDK's IO routines, we turned
         * read's and write's of byte arrays of size greater
         * than 2048 bytes into several operations of size 2048.
         * This saves a malloc()/memcpy()/free() for big
         * buffers.  This is OK for file IO and TCP, but that
         * strategy violates the semantics of a datagram protocol.
         * (one big send) != (several smaller sends).  So here
         * we *must* alloc the buffer.  Note it needn't be bigger
         * than 65,536 (0xFFFF) the max size of an IP packet.
         * Anything bigger should be truncated anyway.
         *
         * We may want to use a smarter allocation scheme at some
         * point.
         */
        if (packetBufferLen > MAX_PACKET_LEN)
        packetBufferLen = MAX_PACKET_LEN;

        fullPacket = (char *)malloc(packetBufferLen);

        if (!fullPacket)
        {
            JNU_ThrowOutOfMemoryError(env, "heap allocation failed");
            return;
        }
        else
        {
            mallocedPacket = JNI_TRUE;
        }
    }
    else
    {
        fullPacket = &(BUF[0]);
    }

    (*env)->GetByteArrayRegion(env, packetBuffer, packetBufferOffset, packetBufferLen,
            (jbyte *)fullPacket);

    int port;
    jobject iaObj = NET_SockaddrToInetAddress(env, (mpe_SocketSockAddr *)&rmtaddr, &port);

    if (((NET_GetAddressFamily(env, iaObj)) == MPE_SOCKET_AF_INET6) &&
        ipv6_available() && (trafficClass != 0))
    {
        NET_SetTrafficClass((mpe_SocketSockAddr *)&rmtaddr, trafficClass);
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s to %d.%d.%d.%d:%d\n", __func__,
           (((struct sockaddr_in*)rmtaddrP)->sin_addr.s_addr & 0xFF),
           (((struct sockaddr_in*)rmtaddrP)->sin_addr.s_addr >> 8) & 0xFF,
           (((struct sockaddr_in*)rmtaddrP)->sin_addr.s_addr >> 16) & 0xFF,
           (((struct sockaddr_in*)rmtaddrP)->sin_addr.s_addr >> 24) & 0xFF,
           port);
    /*
     * Send the datagram.
     *
     * If we are connected it's possible that sendto will return
     * ECONNREFUSED indicating that an ICMP port unreachable has
     * received.
     */
    ret = NET_SendTo(fd, fullPacket, packetBufferLen, 0, (mpe_SocketSockAddr *)rmtaddrP, len);

    if (ret < 0)
    {
        switch (ret)
        {
        case JVM_IO_ERR :
            if (NET_GetLastError() == MPE_SOCKET_ECONNREFUSED)
            {
                JNU_ThrowByName(env, JNU_JAVANETPKG "PortUnreachableException",
                                "ICMP Port Unreachable");
            }
            else
            {
                NET_ThrowByNameWithLastError(env, "java/io/IOException", "sendto failed");
            }
            break;
                
        case JVM_IO_INTR:
            JNU_ThrowByName(env, "java/io/InterruptedIOException",
                            "operation interrupted");
            break;
        }
    }

    if (mallocedPacket)
    free(fullPacket);
}

/*
 * Class:     java_net_PlainDatagramSocketImpl
 * Method:    peek
 * Signature: (Ljava/net/InetAddress;)I
 */
JNIEXPORT jint JNICALL
Java_java_net_PlainDatagramSocketImpl_peek(JNIEnv *env, jobject this, jobject addressObj)
{
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID));
    jint timeout = (*env)->GetIntField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_timeoutID));
    jint fd;
    ssize_t n;
    mpe_SocketSockAddr remote_addr;
    mpe_SocketSockLen len;
    char buf[1];
    jint family;
    jobject iaObj;
    int port;

    if (IS_NULL(fdObj))
    {
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", "Socket closed");
        return -1;
    }
    else
    {
        fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID));
    }

    if (IS_NULL(addressObj))
    {
        JNU_ThrowNullPointerException(env, "Null address in peek()");
    }

    if (timeout)
    {
        int ret = NET_Timeout(fd, timeout);
        if (ret == 0)
        {
            JNU_ThrowByName(env, JNU_JAVANETPKG "SocketTimeoutException",
                    "Peek timed out");
            return ret;
        }
        else if (ret == JVM_IO_ERR)
        {
            if (NET_GetLastError() == MPE_SOCKET_EBADF)
            {
                JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", "Socket closed");
            }
            else
            {
                NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException", "Peek failed");
            }
            return ret;
        }
        else if (ret == JVM_IO_INTR)
        {
            JNU_ThrowByName(env, JNU_JAVAIOPKG "InterruptedIOException",
                    "operation interrupted");
            return ret; /* WARNING: SHOULD WE REALLY RETURN -2??? */
        }
    }

    len = SOCKADDR_LEN;
    n = NET_RecvFrom(fd, buf, 1, MPE_SOCKET_MSG_PEEK, (mpe_SocketSockAddr *)&remote_addr, &len);

    if (n == JVM_IO_ERR)
    {
        int error = NET_GetLastError();
        if (error == MPE_SOCKET_ECONNREFUSED)
        {
            JNU_ThrowByName(env, JNU_JAVANETPKG "PortUnreachableException",
                    "ICMP Port Unreachable");
        }
        else
        {
            if (error == MPE_SOCKET_EBADF)
            {
                JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", "Socket closed");
            }
            else
            {
                NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException", "Peek failed");
            }
        }
        return 0;
    }
    else if (n == JVM_IO_INTR)
    {
        JNU_ThrowByName(env, "java/io/InterruptedIOException", 0);
        return 0;
    }

    iaObj = NET_SockaddrToInetAddress(env, (mpe_SocketSockAddr *)&remote_addr, &port);
    
    family = NET_GetAddressFamily(env, iaObj);

    if (family == MPE_SOCKET_AF_INET4) /* this api can't handle IPV6 addresses */
    {
        int address = (*env)->GetIntField(env, iaObj, JNI_STATIC(java_net_InetAddress, ia_addressID));
        (*env)->SetIntField(env, addressObj, JNI_STATIC(java_net_InetAddress, ia_addressID), address);
    }
    return port;
}

JNIEXPORT jint JNICALL
Java_java_net_PlainDatagramSocketImpl_peekData(JNIEnv *env, jobject this, jobject packet)
{
    char BUF[MAX_BUFFER_LEN];
    char *fullPacket = NULL;
    int mallocedPacket = JNI_FALSE;
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID));
    jint timeout = (*env)->GetIntField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_timeoutID));
    jbyteArray packetBuffer;
    jint packetBufferOffset, packetBufferLen;
    int fd;
    int n;
    mpe_SocketSockAddr remote_addr;
    mpe_SocketSockLen len;
    int port;

    if (IS_NULL(fdObj))
    {
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException","Socket closed");
        return -1;
    }

    fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID));

    if (IS_NULL(packet))
    {
        JNU_ThrowNullPointerException(env, "packet");
        return -1;
    }

    packetBuffer = (*env)->GetObjectField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_bufID));
    if (IS_NULL(packetBuffer))
    {
        JNU_ThrowNullPointerException(env, "packet buffer");
        return -1;
    }
    packetBufferOffset = (*env)->GetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_offsetID));
    packetBufferLen = (*env)->GetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_lengthID));
    if (timeout)
    {
        int ret = NET_Timeout(fd, timeout);
        if (ret == 0)
        {
            JNU_ThrowByName(env, JNU_JAVANETPKG "SocketTimeoutException", "Receive timed out");
            return -1;
        }
        else if (ret == JVM_IO_ERR)
        {
            if (NET_GetLastError() == MPE_SOCKET_EBADF)
            {
                JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", "Socket closed");
            }
            else
            {
                
                NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException", "Receive failed");
            }
            return -1;
        }
        else if (ret == JVM_IO_INTR)
        {
            JNU_ThrowByName(env, JNU_JAVAIOPKG "InterruptedIOException", "operation interrupted");
            return -1;
        }
    }

    if (packetBufferLen > MAX_BUFFER_LEN)
    {

        /* When JNI-ifying the JDK's IO routines, we turned
         * read's and write's of byte arrays of size greater
         * than 2048 bytes into several operations of size 2048.
         * This saves a malloc()/memcpy()/free() for big
         * buffers.  This is OK for file IO and TCP, but that
         * strategy violates the semantics of a datagram protocol.
         * (one big send) != (several smaller sends).  So here
         * we *must* alloc the buffer.  Note it needn't be bigger
         * than 65,536 (0xFFFF) the max size of an IP packet.
         * anything bigger is truncated anyway.
         *
         * We may want to use a smarter allocation scheme at some
         * point.
         */
        if (packetBufferLen > MAX_PACKET_LEN)
        packetBufferLen = MAX_PACKET_LEN;

        fullPacket = (char *)malloc(packetBufferLen);

        if (!fullPacket)
        {
            JNU_ThrowOutOfMemoryError(env, "heap allocation failed");
            return -1;
        }
        else
        {
            mallocedPacket = JNI_TRUE;
        }
    }
    else
    {
        fullPacket = &(BUF[0]);
    }

    len = SOCKADDR_LEN;
    n = NET_RecvFrom(fd, fullPacket, packetBufferLen, MPE_SOCKET_MSG_PEEK,(mpe_SocketSockAddr *)&remote_addr, &len);

    /* truncate the data if the packet's length is too small */
    if (n > packetBufferLen)
    n = packetBufferLen;

    if (n == JVM_IO_ERR)
    {
        int error = NET_GetLastError();
        if (error == MPE_SOCKET_ECONNREFUSED)
        {
            JNU_ThrowByName(env, JNU_JAVANETPKG "PortUnreachableException", "ICMP Port Unreachable");
        }
        else
        {
            if (error == MPE_SOCKET_EBADF)
            {
                JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", "Socket closed");
            }
            else
            {
                NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException", "Receive failed");
            }
        }
        (*env)->SetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_offsetID), 0);
        (*env)->SetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_lengthID), 0);
    }
    else if (n == JVM_IO_INTR)
    {
        JNU_ThrowByName(env, JNU_JAVAIOPKG "InterruptedIOException", "operation interrupted");
        (*env)->SetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_offsetID), 0);
        (*env)->SetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_lengthID), 0);
    }
    else
    {
        /*
         * success - fill in received address...
         *
         * REMIND: Fill in an int on the packet, and create inetadd
         * object in Java, as a performance improvement. Also
         * construct the inetadd object lazily.
         */

        jobject packetAddress;

        /*
         * Check if there is an InetAddress already associated with this
         * packet. If so we check if it is the same source address. We
         * can't update any existing InetAddress because it is immutable
         */
        packetAddress = (*env)->GetObjectField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_addressID));
        if (packetAddress != NULL)
        {
            if (!NET_SockaddrEqualsInetAddress(env, (mpe_SocketSockAddr *)&remote_addr, packetAddress))
            {
                /* force a new InetAddress to be created */
                packetAddress = NULL;
            }
        }
        if (packetAddress == NULL)
        {
            packetAddress = NET_SockaddrToInetAddress(env, (mpe_SocketSockAddr *)&remote_addr, &port);
            /* stuff the new Inetaddress in the packet */
            (*env)->SetObjectField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_addressID), packetAddress);
        }
        else
        {
            /* only get the new port number */
            port = NET_GetPortFromSockaddr((mpe_SocketSockAddr *)&remote_addr);
        }
        /* and fill in the data, remote address/port and such */
        (*env)->SetByteArrayRegion(env, packetBuffer, packetBufferOffset, n, (jbyte *)fullPacket);
        (*env)->SetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_portID), port);
        (*env)->SetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_lengthID), n);
    }

    if (mallocedPacket)
    free(fullPacket);

    return port;
}

/*
 * Class:     java_net_PlainDatagramSocketImpl
 * Method:    receive
 * Signature: (Ljava/net/DatagramPacket;)V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainDatagramSocketImpl_receive(JNIEnv *env, jobject this, jobject packet)
{
    char BUF[MAX_BUFFER_LEN];
    char *fullPacket = NULL;
    int mallocedPacket = JNI_FALSE;
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID));
    jint timeout = (*env)->GetIntField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_timeoutID));
    jbyteArray packetBuffer;
    jint packetBufferOffset, packetBufferLen;
    int fd;
    int n;
    mpe_SocketSockAddr remote_addr;
    mpe_SocketSockLen len;
    jboolean retry;
    jboolean connected = JNI_FALSE;
    jobject connectedAddress = 0;
    jint connectedPort = 0;
    jlong prevTime = 0;

    if (IS_NULL(fdObj))
    {
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException","Socket closed");
        return;
    }

    fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID));

    if (IS_NULL(packet))
    {
        JNU_ThrowNullPointerException(env, "packet");
        return;
    }

    packetBuffer = (*env)->GetObjectField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_bufID));
    if (IS_NULL(packetBuffer))
    {
        JNU_ThrowNullPointerException(env, "packet buffer");
        return;
    }
    packetBufferOffset = (*env)->GetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_offsetID));
    packetBufferLen = (*env)->GetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_bufLengthID));

    if (packetBufferLen > MAX_BUFFER_LEN)
    {

        /* When JNI-ifying the JDK's IO routines, we turned
         * read's and write's of byte arrays of size greater
         * than 2048 bytes into several operations of size 2048.
         * This saves a malloc()/memcpy()/free() for big
         * buffers.  This is OK for file IO and TCP, but that
         * strategy violates the semantics of a datagram protocol.
         * (one big send) != (several smaller sends).  So here
         * we *must* alloc the buffer.  Note it needn't be bigger
         * than 65,536 (0xFFFF) the max size of an IP packet.
         * anything bigger is truncated anyway.
         *
         * We may want to use a smarter allocation scheme at some
         * point.
         */
        if (packetBufferLen > MAX_PACKET_LEN)
        packetBufferLen = MAX_PACKET_LEN;

        fullPacket = (char *)malloc(packetBufferLen);

        if (!fullPacket)
        {
            JNU_ThrowOutOfMemoryError(env, "heap allocation failed");
            return;
        }
        else
        {
            mallocedPacket = JNI_TRUE;
        }
    }
    else
    {
        fullPacket = &(BUF[0]);
    }

    do
    {
        retry = JNI_FALSE;

        if (timeout)
        {
            int ret = NET_Timeout(fd, timeout);
            if (ret <= 0)
            {
                if (ret == 0)
                {
                    JNU_ThrowByName(env, JNU_JAVANETPKG "SocketTimeoutException", "Receive timed out");
                }
                else if (ret == JVM_IO_ERR)
                {
                    if (NET_GetLastError() == MPE_SOCKET_EBADF)
                    {
                        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", "Socket closed");
                    }
                    else
                    {
                        NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException", "Receive failed");
                    }
                }
                else if (ret == JVM_IO_INTR)
                {
                    JNU_ThrowByName(env, JNU_JAVAIOPKG "InterruptedIOException", "operation interrupted");
                }

                if (mallocedPacket)
                free(fullPacket);

                return;
            }
        }

        /*
         * Security Note: For Linux 2.2 with connected datagrams ensure that
         * you receive into the stack/heap allocated buffer - do not attempt
         * to receive directly into DatagramPacket's byte array.
         * (ie: if the virtual machine support pinning don't use
         * GetByteArrayElements or a JNI critical section and receive
         * directly into the byte array)
         */
        len = SOCKADDR_LEN;
        n = NET_RecvFrom(fd, fullPacket, packetBufferLen, 0,(mpe_SocketSockAddr *)&remote_addr, &len);
        /* truncate the data if the packet's length is too small */
        if (n > packetBufferLen)
        n = packetBufferLen;

        if (n == JVM_IO_ERR)
        {
            int error = NET_GetLastError();
            if (error == MPE_SOCKET_ECONNREFUSED)
            {
                JNU_ThrowByName(env, JNU_JAVANETPKG "PortUnreachableException", "ICMP Port Unreachable");
            }
            else
            {
                if (error == MPE_SOCKET_EBADF)
                {
                    JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", "Socket closed");
                }
                else
                {
                    NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException", "Receive failed");
                }
            }
            (*env)->SetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_offsetID), 0);
            (*env)->SetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_lengthID), 0);
        }
        else if (n == JVM_IO_INTR)
        {
            JNU_ThrowByName(env, JNU_JAVAIOPKG "InterruptedIOException", "operation interrupted");
            (*env)->SetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_offsetID), 0);
            (*env)->SetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_lengthID), 0);
        }
        else
        {
            int port;
            jobject packetAddress;

            /*
             * success - fill in received address...
             *
             * REMIND: Fill in an int on the packet, and create inetadd
             * object in Java, as a performance improvement. Also
             * construct the inetadd object lazily.
             */

            /*
             * Check if there is an InetAddress already associated with this
             * packet. If so we check if it is the same source address. We
             * can't update any existing InetAddress because it is immutable
             */
            packetAddress = (*env)->GetObjectField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_addressID));
            if (packetAddress != NULL)
            {
                if (!NET_SockaddrEqualsInetAddress(env, (mpe_SocketSockAddr *)&remote_addr, packetAddress))
                {
                    /* force a new InetAddress to be created */
                    packetAddress = NULL;
                }
            }
            if (packetAddress == NULL)
            {
                packetAddress = NET_SockaddrToInetAddress(env, (mpe_SocketSockAddr *)&remote_addr, &port);
                /* stuff the new Inetaddress in the packet */
                (*env)->SetObjectField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_addressID), packetAddress);
            }
            else
            {
                /* only get the new port number */
                port = NET_GetPortFromSockaddr((mpe_SocketSockAddr *)&remote_addr);
            }
            /* and fill in the data, remote address/port and such */
            (*env)->SetByteArrayRegion(env, packetBuffer, packetBufferOffset, n, (jbyte *)fullPacket);
            (*env)->SetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_portID), port);
            (*env)->SetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_lengthID), n);

            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s from %d.%d.%d.%d:%d\n", __func__,
                   (((struct sockaddr_in*)&remote_addr)->sin_addr.s_addr & 0xFF),
                   (((struct sockaddr_in*)&remote_addr)->sin_addr.s_addr >> 8) & 0xFF,
                   (((struct sockaddr_in*)&remote_addr)->sin_addr.s_addr >> 16) & 0xFF,
                   (((struct sockaddr_in*)&remote_addr)->sin_addr.s_addr >> 24) & 0xFF,
                   port);
        }

    }while (retry);

    if (mallocedPacket)
    free(fullPacket);
}

/*
 * Class:     java_net_PlainDatagramSocketImpl
 * Method:    datagramSocketCreate
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainDatagramSocketImpl_datagramSocketCreate(JNIEnv *env, jobject this)
{
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID));
    int domain = MPE_SOCKET_AF_INET4;
    int fd;
    int t = 1;

    if (IS_NULL(fdObj))
    {
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException","Socket closed");
        return;
    }
    else
    {
        fd = NET_Socket(domain, MPE_SOCKET_DATAGRAM, 0);
    }
    if (fd == JVM_IO_ERR)
    {
        // IPv4 socket creation failed - try IPv6...
        if (ipv6_available())
        {
            domain = MPE_SOCKET_AF_INET6;
            fd = NET_Socket(domain, MPE_SOCKET_DATAGRAM, 0);

            /* note: if you run out of fds, you may not be able to load
             * the exception class, and get a NoClassDefFoundError
             * instead.
             */
            if (fd == JVM_IO_ERR)
            {
                NET_ThrowByNameWithLastError(env,
                    JNU_JAVANETPKG "SocketException", "Error creating socket");
                return;
            }

            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,"%s got IPv6 socket\n",__func__);
#if defined(__linux__)
            /*
             * On Linux for IPv6 sockets we must set the hop limit
             * to 1 to be compatible with default ttl of 1 for IPv4 sockets.
             */
            int ttl = 1;
            NET_SetSockOpt(fd, MPE_SOCKET_IPPROTO_IPV6,
                     MPE_SOCKET_IPV6_MULTICAST_HOPS, (char *)&ttl, sizeof(ttl));
#endif
        }
        else
        {
            NET_ThrowByNameWithLastError(env,
                JNU_JAVANETPKG "SocketException", "Error creating socket");
            return;
        }
    }
    else
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s got IPv4 socket\n", __func__);
    }

    NET_SetSockOpt(fd, MPE_SOCKET_SOL_SOCKET, MPE_SOCKET_SO_BROADCAST, (char*) &t, sizeof(int));

    (*env)->SetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID), fd);
}

/*
 * Class:     java_net_PlainDatagramSocketImpl
 * Method:    datagramSocketClose
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainDatagramSocketImpl_datagramSocketClose(JNIEnv *env, jobject this)
{
    /*
     * REMIND: PUT A LOCK AROUND THIS CODE
     */
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID));
    int fd;

    if (IS_NULL(fdObj))
    return;

    fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID));
    if (fd == -1)
    return;

    (*env)->SetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID), -1);
    NET_SocketClose(fd);
}

/*
 * Sets the multicast interface.
 *
 * SocketOptions.IP_MULTICAST_IF :-
 *  value is a InetAddress
 *  IPv4:   set outgoing multicast interface using
 *      IPPROTO_IP/IP_MULTICAST_IF
 *  IPv6:   Get the index of the interface to which the
 *      InetAddress is bound
 *      Set outgoing multicast interface using
 *      IPPROTO_IPV6/IPV6_MULTICAST_IF
 *      On Linux 2.2 record interface index as can't
 *      query the multicast interface.
 *
 * SockOptions.IF_MULTICAST_IF2 :-
 *  value is a NetworkInterface
 *  IPv4:   Obtain IP address bound to network interface
 *      (NetworkInterface.addres[0])
 *      set outgoing multicast interface using
 *              IPPROTO_IP/IP_MULTICAST_IF
 *  IPv6:   Obtain NetworkInterface.index
 *      Set outgoing multicast interface using
 *              IPPROTO_IPV6/IPV6_MULTICAST_IF
 *              On Linux 2.2 record interface index as can't
 *              query the multicast interface.
 *
 */
static void setMulticastInterface(JNIEnv *env, jobject this, int fd, jint opt, jobject value)
{
    if (opt == java_net_SocketOptions_IP_MULTICAST_IF)
    {
        /*
         * value is an InetAddress.
         * On IPv4 system use IP_MULTICAST_IF socket option
         * On IPv6 system get the NetworkInterface that this IP
         * address is bound too and use the IPV6_MULTICAST_IF
         * option instead of IP_MULTICAST_IF
         */
        if (ipv6_available() && isIPv6Socket(env, fd))
        {
            static jclass ni_class;
            
            if (ni_class == NULL)
            {
                jclass c = (*env)->FindClass(env, "java/net/NetworkInterface");
                CHECK_NULL(c);
                ni_class = (*env)->NewGlobalRef(env, c);
                CHECK_NULL(ni_class);
            }
            
            value = Java_java_net_NetworkInterface_getByInetAddress0(env, ni_class, value);
            if (value == NULL)
            {
                if (!(*env)->ExceptionOccurred(env))
                {
                    JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
                                    "bad argument for IP_MULTICAST_IF"
                                    ": address not bound to any interface");
                }
                return;
            }
            opt = java_net_SocketOptions_IP_MULTICAST_IF2;
        }
        else
        {
            static jfieldID ia_addressID;
            mpe_SocketIPv4Addr in;
            
            if (ia_addressID == NULL)
            {
                jclass c = (*env)->FindClass(env,"java/net/InetAddress");
                CHECK_NULL(c);
                ia_addressID = (*env)->GetFieldID(env, c, "address", "I");
                CHECK_NULL(ia_addressID);
            }
            
            in.s_addr = mpe_socketHtoNL( (*env)->GetIntField(env, value, ia_addressID) );
            if (NET_SetSockOpt(fd, MPE_SOCKET_IPPROTO_IPV4, MPE_SOCKET_IPV4_MULTICAST_IF
                               , (const char*)&in, sizeof(in)) < 0)
            {
                NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                                             "Error setting socket option");
            }
            return;
        }
    }

    if (opt == java_net_SocketOptions_IP_MULTICAST_IF2)
    {
        /*
         * value is a NetworkInterface.
         * On IPv6 system get the index of the interface and use the
         * IPV6_MULTICAST_IF socket option
         * On IPv4 system extract addr[0] and use the IP_MULTICAST_IF
         * option.
         */
        if (ipv6_available() && isIPv6Socket(env, fd))
        {
            static jfieldID ni_indexID;
            int index;
            
            if (ni_indexID == NULL)
            {
                jclass c = (*env)->FindClass(env, "java/net/NetworkInterface");
                CHECK_NULL(c);
                ni_indexID = (*env)->GetFieldID(env, c, "index", "I");
                CHECK_NULL(ni_indexID);
            }
            index = (*env)->GetIntField(env, value, ni_indexID);
            
            if (NET_SetSockOpt(fd, MPE_SOCKET_IPPROTO_IPV6, MPE_SOCKET_IPV6_MULTICAST_IF,
                               (const char*)&index, sizeof(index)) < 0)
            {
                if (NET_GetLastError() == MPE_EINVAL && index > 0)
                {
                    JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
                                    "IPV6_MULTICAST_IF failed (interface has IPv4 "
                                    "address only?)");
                }
                else
                {
                    NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                                                 "Error setting socket option");
                }
            }
        }
        else
        {
            static jfieldID ni_addrsID;
            static jfieldID ia_addressID;
            mpe_SocketIPv4Addr in;
            jobjectArray addrArray;
            jsize len;
            jobject addr;

            if (ni_addrsID == NULL)
            {
                jclass c = (*env)->FindClass(env, "java/net/NetworkInterface");
                CHECK_NULL(c);
                ni_addrsID = (*env)->GetFieldID(env, c, "addrs",
                                                "[Ljava/net/InetAddress;");
                CHECK_NULL(ni_addrsID);
                c = (*env)->FindClass(env,"java/net/InetAddress");
                CHECK_NULL(c);
                ia_addressID = (*env)->GetFieldID(env, c, "address", "I");
                CHECK_NULL(ia_addressID);
            }

            addrArray = (*env)->GetObjectField(env, value, ni_addrsID);
            len = (*env)->GetArrayLength(env, addrArray);

            /*
             * Check that there is at least one address bound to this
             * interface.
             */
            if (len < 1)
            {
                JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
                                "bad argument for IP_MULTICAST_IF2: No IP addresses bound to interface");
                return;
            }
            
            addr = (*env)->GetObjectArrayElement(env, addrArray, 0);
            in.s_addr = mpe_socketHtoNL((*env)->GetIntField(env, addr, ia_addressID));
            
            if (NET_SetSockOpt(fd, MPE_SOCKET_IPPROTO_IPV4, MPE_SOCKET_IPV4_MULTICAST_IF,
                               (const char*)&in, sizeof(in)) < 0)
            {
                NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                                             "Error setting socket option");
            }
        }
    }
}

/*
 * Class:   java/net/PlainDatagramSocketImpl
 * Method:  socketSetOption
 * Signature:   (ILjava/lang/Object;)V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainDatagramSocketImpl_socketSetOption(JNIEnv *env, jobject this, jint opt, jobject value)
{
    int fd;
    int level, optname, optlen = 0;
    union
    {
        int i;
        char c;
    }optval;

    /*
     * Check that socket hasn't been closed
     */
    fd = getFD(env, this);
    if (fd < 0)
    {
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", "Socket closed");
        return;
    }

    /*
     * Check argument has been provided
     */
    if (IS_NULL(value))
    {
        JNU_ThrowNullPointerException(env, "value argument");
        return;
    }

    /*
     * Setting the multicast interface handled seperately
     */
    if (opt == java_net_SocketOptions_IP_MULTICAST_IF ||
            opt == java_net_SocketOptions_IP_MULTICAST_IF2)
    {
        setMulticastInterface(env, this, fd, opt, value);
        return;
    }

    /*
     * Map the Java level socket option to the platform specific
     * level and option name.
     */
    if (NET_MapSocketOption(opt, &level, &optname))
    {
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", "Invalid socket option");
        return;
    }

    switch (opt)
    {
        case java_net_SocketOptions_SO_SNDBUF :
        case java_net_SocketOptions_SO_RCVBUF :
        case java_net_SocketOptions_IP_TOS :
        {
            jclass cls;
            jfieldID fid;

            cls = (*env)->FindClass(env, "java/lang/Integer");
            CHECK_NULL(cls);
            fid = (*env)->GetFieldID(env, cls, "value", "I");
            CHECK_NULL(fid);

            optval.i = (*env)->GetIntField(env, value, fid);
            optlen = sizeof(optval.i);
            break;
        }

        case java_net_SocketOptions_SO_REUSEADDR:
        case java_net_SocketOptions_SO_BROADCAST:
        case java_net_SocketOptions_IP_MULTICAST_LOOP:
        {
            jclass cls;
            jfieldID fid;
            jboolean on;

            cls = (*env)->FindClass(env, "java/lang/Boolean");
            CHECK_NULL(cls);
            fid = (*env)->GetFieldID(env, cls, "value", "Z");
            CHECK_NULL(fid);

            on = (*env)->GetBooleanField(env, value, fid);
            if (opt == java_net_SocketOptions_IP_MULTICAST_LOOP)
            {
                /*
                 * IP_MULTICAST_LOOP may be mapped to IPPROTO (arg
                 * type 'char') or IPPROTO_V6 (arg type 'int').
                 *
                 * In addition setLoopbackMode(true) disables
                 * IP_MULTICAST_LOOP - doesn't enable it.
                 */
                if (level == MPE_SOCKET_IPPROTO_IPV4)
                {
                    optval.c = (!on ? 1 : 0);
                    optlen = sizeof(optval.c);
                }
                else
                {
                    optval.i = (!on ? 1 : 0);
                    optlen = sizeof(optval.i);
                }
            }
            else
            {
                /* SO_REUSEADDR or SO_BROADCAST */
                optval.i = (on ? 1 : 0);
                optlen = sizeof(optval.i);
            }
            break;
        }

        default :
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
                "Socket option not supported by PlainDatagramSocketImp");
        break;
    }

    if (NET_SetSockOpt(fd, level, optname, (const void *)&optval, optlen) < 0)
    {
        NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException", "Error setting socket option");
        return;
    }
}

/*
 * Return the multicast interface:
 *
 * SocketOptions.IP_MULTICAST_IF
 *  IPv4:   Query IPPROTO_IP/IP_MULTICAST_IF
 *      Create InetAddress
 *      IP_MULTICAST_IF returns struct ip_mreqn on 2.2
 *      kernel but struct in_addr on 2.4 kernel
 *  IPv6:   Query IPPROTO_IPV6 / IPV6_MULTICAST_IF or
 *      obtain from impl is Linux 2.2 kernel
 *      If index == 0 return InetAddress representing
 *      anyLocalAddress.
 *      If index > 0 query NetworkInterface by index
 *      and returns addrs[0]
 *
 * SocketOptions.IP_MULTICAST_IF2
 *  IPv4:   Query IPPROTO_IP/IP_MULTICAST_IF
 *      Query NetworkInterface by IP address and
 *      return the NetworkInterface that the address
 *      is bound too.
 *  IPv6:   Query IPPROTO_IPV6 / IPV6_MULTICAST_IF
 *      (except Linux .2 kernel)
 *      Query NetworkInterface by index and
 *      return NetworkInterface.
 */
jobject getMulticastInterface(JNIEnv *env, jobject this, int fd, jint opt)
{
    if (ipv6_available() && isIPv6Socket(env, fd))
    {
        /*
         * IPv6 implementation
         */
        assert ((opt == java_net_SocketOptions_IP_MULTICAST_IF) ||
                (opt == java_net_SocketOptions_IP_MULTICAST_IF2));
        static jclass ni_class;
        static jmethodID ni_ctrID;
        static jfieldID ni_indexID;
        static jfieldID ni_addrsID;
        static jclass ia_class;
        static jmethodID ia_anyLocalAddressID;

        int index;
        int len = sizeof(index);

        jobjectArray addrArray;
        jobject addr;
        jobject ni;

        if (NET_GetSockOpt(fd, MPE_SOCKET_IPPROTO_IPV6, MPE_SOCKET_IPV6_MULTICAST_IF,
                        (char*)&index, &len) < 0)
        {
            NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                    "Error getting socket option");
            return NULL;
        }

        if (ni_class == NULL)
        {
            jclass c = (*env)->FindClass(env, "java/net/NetworkInterface");
            CHECK_NULL_RETURN(c, NULL);
            ni_ctrID = (*env)->GetMethodID(env, c, "<init>", "()V");
            CHECK_NULL_RETURN(ni_ctrID, NULL);
            ni_indexID = (*env)->GetFieldID(env, c, "index", "I");
            CHECK_NULL_RETURN(ni_indexID, NULL);
            ni_addrsID = (*env)->GetFieldID(env, c, "addrs",
                    "[Ljava/net/InetAddress;");
            CHECK_NULL_RETURN(ni_addrsID, NULL);

            ia_class = (*env)->FindClass(env, "java/net/InetAddress");
            CHECK_NULL_RETURN(ia_class, NULL);
            ia_class = (*env)->NewGlobalRef(env, ia_class);
            CHECK_NULL_RETURN(ia_class, NULL);
            ia_anyLocalAddressID = (*env)->GetStaticMethodID(env,
                    ia_class,
                    "anyLocalAddress",
                    "()Ljava/net/InetAddress;");
            CHECK_NULL_RETURN(ia_anyLocalAddressID, NULL);
            ni_class = (*env)->NewGlobalRef(env, c);
            CHECK_NULL_RETURN(ni_class, NULL);
        }

        /*
         * If multicast to a specific interface then return the
         * interface (for IF2) or the any address on that interface
         * (for IF).
         */
        if (index > 0)
        {
            ni = Java_java_net_NetworkInterface_getByIndex(env, ni_class, index);
            if (ni == NULL)
            {
                char errmsg[255];
                sprintf(errmsg, "IPV6_MULTICAST_IF returned index to unrecognized interface: %d",index);
                JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", errmsg);
                return NULL;
            }

            /*
             * For IP_MULTICAST_IF2 return the NetworkInterface
             */
            if (opt == java_net_SocketOptions_IP_MULTICAST_IF2)
            return ni;

            /*
             * For IP_MULTICAST_IF return addrs[0]
             */
            addrArray = (*env)->GetObjectField(env, ni, ni_addrsID);
            if ((*env)->GetArrayLength(env, addrArray) < 1)
            {
                JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
                        "IPV6_MULTICAST_IF returned interface without IP bindings");
                return NULL;
            }

            addr = (*env)->GetObjectArrayElement(env, addrArray, 0);
            return addr;
        }

        /*
         * Multicast to any address - return anyLocalAddress
         * or a NetworkInterface with addrs[0] set to anyLocalAddress
         */

        addr = (*env)->CallStaticObjectMethod(env, ia_class, ia_anyLocalAddressID, NULL);
        if (opt == java_net_SocketOptions_IP_MULTICAST_IF)
        return addr;

        ni = (*env)->NewObject(env, ni_class, ni_ctrID, 0);
        CHECK_NULL_RETURN(ni, NULL);
        (*env)->SetIntField(env, ni, ni_indexID, -1);
        addrArray = (*env)->NewObjectArray(env, 1, ia_class, NULL);
        CHECK_NULL_RETURN(addrArray, NULL);
        (*env)->SetObjectArrayElement(env, addrArray, 0, addr);
        (*env)->SetObjectField(env, ni, ni_addrsID, addrArray);
        return ni;
    }
    else
    {
        /*
         * IPv4 implementation
         */
        static jclass inet4_class;
        static jmethodID inet4_ctrID;
        static jfieldID inet4_addrID;

        static jclass ni_class;
        static jmethodID ni_ctrID;
        static jfieldID ni_indexID;
        static jfieldID ni_addrsID;

        jobjectArray addrArray;
        jobject addr;
        jobject ni;

        mpe_SocketIPv4Addr in;
        int len = sizeof(mpe_SocketIPv4Addr);

        if (NET_GetSockOpt(fd, MPE_SOCKET_IPPROTO_IPV4, MPE_SOCKET_IPV4_MULTICAST_IF, (char *)&in, &len) < 0)
        {
            NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException","Error getting socket option");
            return NULL;
        }

        /*
         * Construct and populate an Inet4Address
         */
        if (inet4_class == NULL)
        {
            jclass c = (*env)->FindClass(env, "java/net/Inet4Address");
            CHECK_NULL_RETURN(c, NULL);
            inet4_ctrID = (*env)->GetMethodID(env, c, "<init>", "()V");
            CHECK_NULL_RETURN(inet4_ctrID, NULL);
            inet4_addrID = (*env)->GetFieldID(env, c, "address", "I");
            CHECK_NULL_RETURN(inet4_addrID, NULL);
            inet4_class = (*env)->NewGlobalRef(env, c);
            CHECK_NULL_RETURN(inet4_class, NULL);
        }
        addr = (*env)->NewObject(env, inet4_class, inet4_ctrID, 0);
        CHECK_NULL_RETURN(addr, NULL);

        (*env)->SetIntField(env, addr, inet4_addrID, ntohl(in.s_addr));

        /*
         * For IP_MULTICAST_IF return InetAddress
         */
        if (opt == java_net_SocketOptions_IP_MULTICAST_IF)
        return addr;

        /*
         * For IP_MULTICAST_IF2 we get the NetworkInterface for
         * this address and return it
         */
        if (ni_class == NULL)
        {
            jclass c = (*env)->FindClass(env, "java/net/NetworkInterface");
            CHECK_NULL_RETURN(c, NULL);
            ni_ctrID = (*env)->GetMethodID(env, c, "<init>", "()V");
            CHECK_NULL_RETURN(ni_ctrID, NULL);
            ni_indexID = (*env)->GetFieldID(env, c, "index", "I");
            CHECK_NULL_RETURN(ni_indexID, NULL);
            ni_addrsID = (*env)->GetFieldID(env, c, "addrs",
                    "[Ljava/net/InetAddress;");
            CHECK_NULL_RETURN(ni_addrsID, NULL);
            ni_class = (*env)->NewGlobalRef(env, c);
            CHECK_NULL_RETURN(ni_class, NULL);
        }
        ni = Java_java_net_NetworkInterface_getByInetAddress0(env, ni_class, addr);
        if (ni)
        return ni;

        /*
         * The address doesn't appear to be bound at any known
         * NetworkInterface. Therefore we construct a NetworkInterface
         * with this address.
         */
        ni = (*env)->NewObject(env, ni_class, ni_ctrID, 0);
        CHECK_NULL_RETURN(ni, NULL);

        (*env)->SetIntField(env, ni, ni_indexID, -1);
        addrArray = (*env)->NewObjectArray(env, 1, inet4_class, NULL);
        CHECK_NULL_RETURN(addrArray, NULL);
        (*env)->SetObjectArrayElement(env, addrArray, 0, addr);
        (*env)->SetObjectField(env, ni, ni_addrsID, addrArray);
        return ni;
    }

    return NULL;
}

/*
 * Class:   java/net/PlainDatagramSocketImpl
 * Method:  socketGetOption
 * Signature:   (I)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL
Java_java_net_PlainDatagramSocketImpl_socketGetOption(JNIEnv *env, jobject this, jint opt)
{
    int fd;
    int level, optname, optlen;
    union
    {
        int i;
        char c;
    }optval;

    fd = getFD(env, this);
    if (fd < 0)
    {
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException","socket closed");
        return NULL;
    }

    /*
     * Handle IP_MULTICAST_IF seperately
     */
    if (opt == java_net_SocketOptions_IP_MULTICAST_IF ||
            opt == java_net_SocketOptions_IP_MULTICAST_IF2)
    {
        return getMulticastInterface(env, this, fd, opt);
    }

    /* find out local IP address */
    mpe_SocketSockAddr him;
    mpe_SocketSockLen len = SOCKADDR_LEN;
    int port;
    jobject iaObj;

    if (NET_GetSockName(fd, (mpe_SocketSockAddr *)&him, &len) == -1)
    {
        NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                "Error getting socket name");
        return NULL;
    }

    iaObj = NET_SockaddrToInetAddress(env, (mpe_SocketSockAddr *)&him, &port);

    /*
     * SO_BINDADDR implemented using getsockname
     */
    if (opt == java_net_SocketOptions_SO_BINDADDR)
    {
        return iaObj;
    }

    /*
     * Map the Java level socket option to the platform specific
     * level and option name.
     */
    if ((NET_GetAddressFamily(env, iaObj)) == MPE_SOCKET_AF_INET6)
    {
        if (NET_MapSocketOptionV6(opt, &level, &optname))
        {
            JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
                            "Invalid IPv6 socket option");
            return NULL;
        }
    }
    else
    {
        if (NET_MapSocketOption(opt, &level, &optname))
        {
            JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
                            "Invalid socket option");
            return NULL;
        }
    }

    if (opt == java_net_SocketOptions_IP_MULTICAST_LOOP && level == MPE_SOCKET_IPPROTO_IPV4)
    optlen = sizeof(optval.c);
    else
    optlen = sizeof(optval.i);

    if (NET_GetSockOpt(fd, level, optname, (void *)&optval, &optlen) < 0)
    {
        NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                "Error getting socket option");
        return NULL;
    }

    switch (opt)
    {
        case java_net_SocketOptions_IP_MULTICAST_LOOP:
        /* getLoopbackMode() returns true if IP_MULTICAST_LOOP disabled */
        if (level == MPE_SOCKET_IPPROTO_IPV4)
        return createBoolean(env, (int)!optval.c);
        else
        return createBoolean(env, !optval.i);

        case java_net_SocketOptions_SO_BROADCAST:
        case java_net_SocketOptions_SO_REUSEADDR:
        return createBoolean(env, optval.i);

        case java_net_SocketOptions_SO_SNDBUF:
        case java_net_SocketOptions_SO_RCVBUF:
        case java_net_SocketOptions_IP_TOS:
        return createInteger(env, optval.i);

    }

    /* should never rearch here */
    return NULL;
}

/*
 * Class:   java/net/PlainDatagramSocketImpl
 * Method:  setTimeToLive
 * Signature:   (I)V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainDatagramSocketImpl_setTimeToLive(JNIEnv *env, jobject this, jint ttl)
{
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID));
    int fd;
    /* it is important to cast this to a char, otherwise setsockopt gets confused */

    if (IS_NULL(fdObj))
    {
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException","Socket closed");
        return;
    }
    else
    {
        fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID));
    }
    /* setsockopt to be correct ttl */
    if (ipv6_available() && isIPv6Socket(env, fd))
    {
        if (NET_SetSockOpt(fd, MPE_SOCKET_IPPROTO_IPV6, MPE_SOCKET_IPV6_MULTICAST_HOPS,(char*)&ttl, sizeof(ttl)) < 0)
        {
            NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException", "Error setting socket option");
            return;
        }
    }
    else
    {
        if (NET_SetSockOpt(fd, MPE_SOCKET_IPPROTO_IPV4, MPE_SOCKET_IPV4_MULTICAST_TTL, (char*)&ttl,sizeof(ttl)) < 0)
        {
            NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException","Error setting socket option");
        }
    }
}

/*
 * Class:   java/net/PlainDatagramSocketImpl
 * Method:  getTimeToLive
 * Signature:   ()I
 */
JNIEXPORT jint JNICALL
Java_java_net_PlainDatagramSocketImpl_getTimeToLive(JNIEnv *env, jobject this)
{
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID));
    jint fd = -1;

    if (IS_NULL(fdObj))
    {
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException","Socket closed");
        return -1;
    }
    else
    {
        fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID));
    }
    /* getsockopt of ttl */
    if (ipv6_available() && isIPv6Socket(env, fd))
    {
        int ttl = 0;
        int len = sizeof(ttl);
        
        if (NET_GetSockOpt(fd, MPE_SOCKET_IPPROTO_IPV6, MPE_SOCKET_IPV6_MULTICAST_HOPS,(char*)&ttl, &len) < 0)
        {
            NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                                         "Error getting socket option");
            return -1;
        }
        return(jint)ttl;
    }
    else
    {
        jint ttl = 0;
        int len = sizeof(ttl);
        if (NET_GetSockOpt(fd, MPE_SOCKET_IPPROTO_IPV4, MPE_SOCKET_IPV4_MULTICAST_TTL, (char*)&ttl, &len) < 0)
        {
            NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                                         "Error getting socket option");
            return -1;
        }
        return ttl;
    }
}

/*
 * mcast_join_leave: Join or leave a multicast group.
 *
 * For IPv4 sockets use IP_ADD_MEMBERSHIP/IP_DROP_MEMBERSHIP socket option
 * to join/leave multicast group.
 *
 * For IPv6 sockets use IPV6_ADD_MEMBERSHIP/IPV6_DROP_MEMBERSHIP socket option
 * to join/leave multicast group. If multicast group is an IPv4 address then
 * an IPv4-mapped address is used.
 *
 * On Linux with IPv6 if we wish to join/leave an IPv4 multicast group then
 * we must use the IPv4 socket options. This is because the IPv6 socket options
 * don't support IPv4-mapped addresses. This is true as per 2.2.19 and 2.4.7
 * kernel releases. In the future it's possible that IP_ADD_MEMBERSHIP
 * will be updated to return ENOPROTOOPT if uses with an IPv6 socket (Solaris
 * already does this). Thus to cater for this we first try with the IPv4
 * socket options and if they fail we use the IPv6 socket options. This
 * seems a reasonable failsafe solution.
 */
static void
mcast_join_leave(JNIEnv *env, jobject this, jobject iaObj, jobject niObj, jboolean join)
{
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID));
    jint fd;
    jint ipv6_join_leave = JNI_FALSE;

    if (IS_NULL(fdObj))
    {
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", "Socket closed");
        return;
    }
    else
    {
        fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID));
    }

    if (IS_NULL(iaObj))
    {
        JNU_ThrowNullPointerException(env, "iaObj");
        return;
    }

    /*
     * Determine if this is an IPv4 or IPv6 join/leave.
     */
    ipv6_join_leave = (ipv6_available() && isIPv6Socket(env, fd));

    /*
     * For IPv4 join use IP_ADD_MEMBERSHIP/IP_DROP_MEMBERSHIP socket option
     *
     * On Linux if IPv4 or IPv6 use IP_ADD_MEMBERSHIP/IP_DROP_MEMBERSHIP
     */
    if (!ipv6_join_leave)
    {
        mpe_SocketIPv4McastReq mname;
        int mname_len = 0;

        /*
         * joinGroup(InetAddress, NetworkInterface) implementation :-
         *
         * Linux/IPv6:  use ip_mreqn structure populated with multicast
         *      address and interface index.
         *
         * IPv4:    use ip_mreq structure populated with multicast
         *      address and first address obtained from
         *      NetworkInterface
         */
        if (niObj != NULL)
        {
            static jfieldID ni_addrsID;
            jobjectArray addrArray;
            jobject addr;
            
            if (ni_addrsID == NULL)
            {
                jclass c = (*env)->FindClass(env, "java/net/NetworkInterface");
                CHECK_NULL(c);
                ni_addrsID = (*env)->GetFieldID(env, c, "addrs", "[Ljava/net/InetAddress;");
                CHECK_NULL(ni_addrsID);
            }
            
            addrArray = (*env)->GetObjectField(env, niObj, ni_addrsID);
            if ((*env)->GetArrayLength(env, addrArray) < 1)
            {
                JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
                                "bad argument for IP_ADD_MEMBERSHIP: "
                                "No IP addresses bound to interface");
                return;
            }
            addr = (*env)->GetObjectArrayElement(env, addrArray, 0);
            
            mname.imr_multiaddr.s_addr = mpe_socketHtoNL((*env)->GetIntField(env, iaObj, JNI_STATIC(java_net_InetAddress, ia_addressID)));
            mname.imr_interface.s_addr = mpe_socketHtoNL((*env)->GetIntField(env, addr, JNI_STATIC(java_net_InetAddress, ia_addressID)));
            mname_len = sizeof(mpe_SocketIPv4McastReq);
        }

        /*
         * joinGroup(InetAddress) implementation :-
         *
         * Linux/IPv6:  use ip_mreqn structure populated with multicast
         *              address and interface index. index obtained
         *      from cached value or IPV6_MULTICAST_IF.
         *
         * IPv4:        use ip_mreq structure populated with multicast
         *              address and local address obtained from
         *              IP_MULTICAST_IF. On Linux IP_MULTICAST_IF
         *      returns different structure depending on
         *      kernel.
         */

        if (niObj == NULL)
        {
            struct in_addr in;
            int len = sizeof(mpe_SocketIPv4Addr);
            
            if (NET_GetSockOpt(fd, MPE_SOCKET_IPPROTO_IPV4, MPE_SOCKET_IPV4_MULTICAST_IF, (char *)&in, &len) < 0)
            {
                NET_ThrowCurrent(env, "getsockopt IP_MULTICAST_IF failed");
                return;
            }
            
            mname.imr_interface.s_addr = in.s_addr;
            mname.imr_multiaddr.s_addr = mpe_socketHtoNL((*env)->GetIntField(env, iaObj, JNI_STATIC(java_net_InetAddress, ia_addressID)));
            mname_len = sizeof(mpe_SocketIPv4McastReq);
        }

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s IPv4 %s for %d.%d.%d.%d\n",
                __func__, join? "ADD" : "DROP",
                (mname.imr_multiaddr.s_addr & 0xFF),
                (mname.imr_multiaddr.s_addr >> 8) & 0xFF,
                (mname.imr_multiaddr.s_addr >> 16) & 0xFF,
                (mname.imr_multiaddr.s_addr >> 24) & 0xFF);
        /*
         * Join the multicast group.
         */
        if (NET_SetSockOpt(fd, MPE_SOCKET_IPPROTO_IPV4, (join ? MPE_SOCKET_IPV4_ADD_MEMBERSHIP
                                : MPE_SOCKET_IPV4_DROP_MEMBERSHIP),
                        (char *) &mname, mname_len) < 0)
        {
            int error = NET_GetLastError();

            /*
             * If IP_ADD_MEMBERSHIP returns ENOPROTOOPT on Linux and we've got
             * IPv6 enabled then it's possible that the kernel has been fixed
             * so we switch to IPV6_ADD_MEMBERSHIP socket option.
             * As of 2.4.7 kernel IPV6_ADD_MEMERSHIP can't handle IPv4-mapped
             * addresses so we have to use IP_ADD_MEMERSHIP for IPv4 multicast
             * groups. However if the socket is an IPv6 socket then then setsockopt
             * should reurn ENOPROTOOPT. We assume this will be fixed in Linux
             * at some stage.
             */
            if (error)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s error[%d] %s\n",
                        __func__, error, NET_getLastError(NULL));
                if (join)
                {
                    NET_ThrowCurrent(env, "setsockopt IP_ADD_MEMBERSHIP failed");
                }
                else
                {
                    NET_ThrowCurrent(env, "setsockopt IP_DROP_MEMBERSHIP failed");
                }
            }
        }
    }
    else
    /*
     * IPv6 join. If it's an IPv4 multicast group then we use an IPv4-mapped
     * address.
     */
    {
        mpe_SocketIPv6McastReq mname6;
        jbyteArray ipaddress;
        jbyte caddr[16];
        jint family = NET_GetAddressFamily(env, iaObj);
        jint address;

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "%s IPv6 %s\n",
                __func__, join? "ADD" : "DROP");

        if (family == MPE_SOCKET_AF_INET4) /* will convert to IPv4-mapped address */
        {
            memset((char *) caddr, 0, 16);
            address = (*env)->GetIntField(env, iaObj, JNI_STATIC(java_net_InetAddress, ia_addressID));

            caddr[10] = 0xff;
            caddr[11] = 0xff;

            caddr[12] = ((address >> 24) & 0xff);
            caddr[13] = ((address >> 16) & 0xff);
            caddr[14] = ((address >> 8) & 0xff);
            caddr[15] = (address & 0xff);
        }
        else
        {
            jfieldID ia_ipaddress;
            jclass c = (*env)->FindClass(env, "java/net/Inet6Address");
            CHECK_NULL(c);
            ia_ipaddress = (*env)->GetFieldID(env, c, "ipaddress", "[B");
            ipaddress = (*env)->GetObjectField(env, iaObj, ia_ipaddress);

            (*env)->GetByteArrayRegion(env, ipaddress, 0, 16, caddr);
        }

        memcpy((void *)&(mname6.ipv6mr_multiaddr), caddr, sizeof(mpe_SocketIPv6Addr));
        if (IS_NULL(niObj))
        {
            int index;
            int len = sizeof(index);

            if (NET_GetSockOpt(fd, MPE_SOCKET_IPPROTO_IPV6, MPE_SOCKET_IPV6_MULTICAST_IF, (char*)&index, &len) < 0)
            {
                NET_ThrowCurrent(env, "getsockopt IPV6_MULTICAST_IF failed");
                return;
            }

#ifdef __linux__
            /*
             * On 2.4.8+ if we join a group with the interface set to 0
             * then the kernel records the interface it decides. This causes
             * subsequent leave groups to fail as there is no match. Thus we
             * pick the interface if there is a matching route.
             */
            if (index == 0)
            {
                int rt_index = getDefaultIPv6Interface(&(mname6.ipv6mr_multiaddr));
                if (rt_index > 0)
                    index = rt_index;
            }
#endif
            mname6.ipv6mr_interface = index;
        }
        else
        {
            static jfieldID ni_indexID;

            if (ni_indexID == NULL)
            {
                jclass c = (*env)->FindClass(env, "java/net/NetworkInterface");
                CHECK_NULL(c);
                ni_indexID = (*env)->GetFieldID(env, c, "index", "I");
                CHECK_NULL(ni_indexID);
            }

            mname6.ipv6mr_interface =(*env)->GetIntField(env, niObj, ni_indexID);
        }

        /* Join the multicast group */
        if (NET_SetSockOpt(fd, MPE_SOCKET_IPPROTO_IPV6, (join ? MPE_SOCKET_IPV6_ADD_MEMBERSHIP
                                : MPE_SOCKET_IPV6_DROP_MEMBERSHIP),
                        (char *) &mname6, sizeof (mname6)) < 0)
        {
            if (join)
            {
                NET_ThrowCurrent(env, "setsockopt IPV6_ADD_MEMBERSHIP failed");
            }
            else
            {
                NET_ThrowCurrent(env, "setsockopt IPV6_DROP_MEMBERSHIP failed");
            }
        }
    }
}

/*
 * Class:     java_net_PlainDatagramSocketImpl
 * Method:    join
 * Signature: (Ljava/net/InetAddress;)V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainDatagramSocketImpl_join(JNIEnv *env, jobject this, jobject iaObj, jobject niObj)
{
    mcast_join_leave(env, this, iaObj, niObj, JNI_TRUE);
}

/*
 * Class:     java_net_PlainDatagramSocketImpl
 * Method:    leave
 * Signature: (Ljava/net/InetAddress;)V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainDatagramSocketImpl_leave(JNIEnv *env, jobject this, jobject iaObj, jobject niObj)
{
    mcast_join_leave(env, this, iaObj, niObj, JNI_FALSE);
}
