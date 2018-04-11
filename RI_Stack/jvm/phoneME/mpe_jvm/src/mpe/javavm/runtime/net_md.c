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

#include "javavm/include/porting/net.h"
#include "javavm/include/porting/sync.h"
#include "javavm/include/porting/threads.h"
#include "javavm/include/porting/io.h"
#include "javavm/include/io_md.h"
#include "javavm/include/jvm2cvm.h"

#include <stdio.h>

#include <mpe_dbg.h>

int IPv6_supported()
{
    return CVM_TRUE;    // we always support IPv6 now (with IPv4 fallback)...
}

#ifndef CVMnetConnect
/**
 * Attempt to make a connection to a socket.
 *
 * @param fd is the file descriptor associated with the socket.
 * @param him points to a sockaddr structure containing the peer address.
 * @param len is the length of the him structure.
 *
 * @return Upon successful completion, this function shall return 0; otherwise, -1 shall
 *			be returned.
 */
CVMInt32 CVMnetConnect(CVMInt32 fd, struct sockaddr *him, CVMInt32 len)
{
    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JVM, "CVMnetConnect: fd = %x\n", fd);
    return (mpe_socketConnect((mpe_Socket) fd, (mpe_SocketSockAddr*) him, len));
}
#endif

#ifndef CVMnetAccept
/**
 * Extract the first connection on the queue of pending connections, create a new socket
 * with the same socket type protocol and address family as the specified socket, and
 * allocate a new file descriptor for that socket.
 *
 * @param fd is the file descriptor associated with the socket.
 * @param him points to a sockaddr structure containing the peer address.
 * @param len is a pointer to the size of the sockaddr structure.
 *
 * @return Upon successful completion, this function shall return the file descriptor of the
 *          accepted socket. Otherwise (-1) is returned.
 */
CVMInt32 CVMnetAccept(CVMInt32 fd, struct sockaddr *him, CVMInt32 *len)
{
    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JVM, "CVMnetAccept: fd = %x\n", fd);
    return mpe_socketAccept((mpe_Socket) fd, (mpe_SocketSockAddr*) him,
            (mpe_SocketSockLen*) len);
}
#endif

#ifndef CVMnetSendTo
/**
 * Send a message to a socket.
 *
 * @param fd is the file descriptor associated with the socket.
 * @param buf points to a buffer containing the message to send.
 * @param len is the size of the message in bytes.
 * @param flags specifies the type of message transmission.
 * @param to is a pointer to the destination sockaddr structure.
 * @param tolen is the size of the sockaddr structure.
 *
 * @return Upon successful completion, this function shall return the number of bytes sent.
 * 			Otherwise, -1 shall be returned.
 */
CVMInt32 CVMnetSendTo(CVMInt32 fd, char *buf, CVMInt32 len, CVMInt32 flags,
        struct sockaddr *to, CVMInt32 tolen)
{
    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JVM, "CVMnetSendTo: fd = %x, len = %d\n",
            fd, len);
    return mpe_socketSentTo((mpe_Socket) fd, (const void*) buf, (size_t) len,
            flags, (mpe_SocketSockAddr*) to, tolen);
}
#endif

#ifndef CVMnetRecvFrom
/**
 * Receive a message from a socket.
 *
 * @param fd is the file descriptor associated with the socket.
 * @param buf points to a buffer containing the message to send.
 * @param nBytes is the size of the message in bytes.
 * @param flags specifies the type of message reception.
 * @param from is a NULL pointer or a piointer for returning the address of the sender.
 * @param fromlen is a pointer for returning the size of the sockaddr structure.
 *
 * @return Upon successful completion, this function shall return the length of the message
 * 			in bytes. If no messages are available to be received and the peer has performed
 * 			an orderly shutdown, this function shall return 0. Otherwise, the function shall
 * 			return -1.
 */
CVMInt32 CVMnetRecvFrom(CVMInt32 fd, char *buf, CVMInt32 nBytes,
        CVMInt32 flags, struct sockaddr *from, CVMInt32 *fromlen)
{
    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JVM,
            "CVMnetRecvFrom: fd = %x, nbytes = %d\n", fd, nbytes);
    return mpe_socketRecvFrom((mpe_Socket) fd, (void*) buf, (size_t) nBytes,
            flags, (mpe_SocketSockAddr*) from, (mpe_SocketSockLen*) fromlen);
}
#endif

#ifndef CVMnetRecv
/**
 * Receive a message from a socket.
 *
 * @param fd is the file descriptor associated with the socket.
 * @param buf points to a buffer containing the message to send.
 * @param nBytes is the size of the message in bytes.
 * @param flags specifies the type of message reception.
 *
 * @return Upon successful completion, this function shall return the length of the message
 * 			in bytes. If no messages are available to be received and the peer has performed
 * 			an orderly shutdown, this function shall return 0. Otherwise, the function shall
 * 			return -1.
 */
CVMInt32 CVMnetRecv(CVMInt32 fd, char *buf, CVMInt32 nBytes, CVMInt32 flags)
{
    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JVM, "CVMnetRecv: fd = %x, nbytes = %d\n",
            fd, nbytes);
    return mpe_socketRecv((mpe_Socket) fd, (void*) buf, (size_t) nBytes,
            flags);
}
#endif

#ifndef CVMnetSend
/**
 * Initiate transmission of a message from the specified socket to its peer.
 *
 * @param fd is the file descriptor associated with the socket.
 * @param buf points to a buffer containing the message to send.
 * @param nBytes is the size of the message in bytes.
 * @param flags specifies the type of message transmission.
 *
 * Upon successful completion, this function shall return the number of bytes
 * 			sent. Otherwise, -1 shall be returned.
 */
CVMInt32 CVMnetSend(CVMInt32 fd, char *buf, CVMInt32 nBytes, CVMInt32 flags)
{
    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JVM, "CVMnetSend: fd = %x, nbytes = %d\n",
            fd, nbytes);
    return mpe_socketSentTo((mpe_Socket) fd, (const void*) buf, (size_t) len,
            flags);
}
#endif

struct protoent * CVMnetGetProtoByName(char* name)
{
    /* Optional API that can be handled if not implemented, return NULL */
    return NULL;
}

#ifndef CVMnetSocket
/**
 * Create and unbound socket.
 *
 * @param domain specifies the communications domain in which a socket is to be created. 
 * @param type specifies the type of socket to be created. 
 * @param protocol specifies a particular protocol to be used with the socket. Specifying
 * 			a protocol of 0 causes this function to use an unspecified default protocol
 * 			appropriate for the requested socket type
 *
 * @return a socket file descriptor reference or JVM_IO_ERR if there was an error.
 */
CVMInt32 CVMnetSocket(CVMInt32 domain, CVMInt32 type, CVMInt32 protocol)
{
    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JVM,
            "CVMnetSocket: domain = %x, type = %x, protocol = %x\n", domain,
            type, protocol);
    return mpe_socketCreate(domain, type, protocol);
}
#endif /* CVMnetSocket */

#ifndef CVMnetSetSockOpt
/**
 * Set the specified socket option.
 *
 * @param fd specifies the file descriptor associated with the socket.
 * @param type is the protocol level at which the option resides
 * @param dir specifies a single option to be set.
 * @param arg points to the new value for the option
 * @param argSize specifies the length of option pointed to by <i>option_value</i>.
 *
 * @return Upon successful completion, this function shall return 0; otherwise, -1 shall
 * 			be returned.
 */
CVMInt32 CVMnetSetSockOpt(CVMInt32 fd, CVMInt32 type, CVMInt32 dir,
        const void * arg, CVMInt32 argSize)
{
    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JVM,
            "CVMnetSetSockOpt: fd = %x, type = %x\n", fd, type);
return (mpe_soscketSetOpt( (mpe_Socket)fd, type, dir, arg, argSize );
    }
#endif /* CVMnetSetSockOpt */

#ifndef CVMnetListen 
    /**
     * Mark a connection-mode socket as accepting connections.
     *
     * @param fd specifies the file descriptor associated with the socket.
     * @param count specifies the maximum number of connections to allow.
     *
     * @return Upon successful completion, this function shall return 0; otherwise, -1 shall
     * 			be returned.
     */
CVMInt32 CVMnetListen(CVMInt32 fd, CVMInt32 count)
{
    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JVM, "CVMnetListen: fd = %x, count = %d\n",
            fd, count);
    return (mpe_socketListen((mpe_Socket) fd, count));
}
#endif /* CVMnetListen */

/**
 * Get the number of byte available on the connection without blocking.
 *
 * @param fd specifies the file descriptor associated with the socket.
 * @param pbytes is pointer for returning the number of bytes available.
 *
 * @return 0 if the operation succeeded and -1 on failure.
 */
CVMInt32 CVMnetSocketAvailable(CVMInt32 fd, CVMInt32 *pbytes)
{
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "CVMnetSocketAvailable: called, fd = %x\n", fd);

    if (mpe_socketIoctl((mpe_Socket) fd, MPE_SOCKET_FIONREAD, pbytes) == (-1))
        return (-1);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "CVMnetSocketAvailable: fd = %x, avail = %d\n", fd, *pbytes);

    return 0;
}

#ifndef CVMnetSocketShutdown
/**
 * Cause a full or partial full-duplex connection to be shut down.
 *
 * @param fd specifies the file descriptor associated with the socket.
 * @param howto specifies the type of shutdown.
 *
 * @return Upon successful completion, this function shall return 0; otherwise, -1 shall
 * 			be returned.
 */
CVMInt32 CVMnetSocketShutdown(CVMInt32 fd, CVMInt32 howto)
{
    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JVM,
            "CVMnetShutdown: fd = %x, howto = %x\n", fd, howto);
    return (mpe_socketShutdown((mpe_Socket) fd, howto));
}
#endif /* CVMnetSocketShutdown */

/**
 * Perform a select operation of the socket with the specified timeout value.
 *
 * @param fd is the file descriptor associated with the socket.
 * @param timeout is the time out value.
 */

CVMInt32 CVMnetTimeout(CVMInt32 fd, CVMInt32 timeout)
{
    mpe_SocketFDSet fdset;
    mpe_TimeVal t;
    int result;

    t.tv_sec = timeout / 1000;
    t.tv_usec = (timeout % 1000) * 1000;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "CVMnetTimeout: fd = %x, timeout: %d sec, %d usec\n", fd, t.tv_sec,
            t.tv_usec);

    mpe_socketFDZero(&fdset);
    mpe_socketFDSet(fd, &fdset);

    result = mpe_socketSelect(fd + 1, &fdset, NULL, NULL, &t);
    if (result == (-1))
    {
        int err = mpe_socketGetLastError();
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "CVMnetTimeout: select failed, error = %d\n", err);
        return ((err == MPE_SOCKET_EINTR) ? JVM_IO_INTR : JVM_IO_ERR);
    }
    return result;
}

