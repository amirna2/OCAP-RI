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
#include <mpeos_socket.h>
#include <mpe_dbg.h>
#include "jvm.h"
#include "jni_util.h"
#include "net_util.h"

#include "java_net_SocketOptions.h"
#include "java_net_PlainSocketImpl.h"

/************************************************************************
 * PlainSocketImpl
 */

#include "jni_statics.h"
/*
 * the maximum buffer size. Used for setting
 * SendBufferSize and ReceiveBufferSize.
 */
static const int32_t max_buffer_size = 64 * 1024;

#define SET_NONBLOCKING(fd) {		\
        int flags = TRUE;	\
        mpe_socketIoctl(fd, MPE_SOCKET_FIONBIO, &flags);	\
}

#define SET_BLOCKING(fd) {		\
        int flags = FALSE;	\
        mpe_socketIoctl(fd, MPE_SOCKET_FIONBIO, &flags);	\
}

/*
 * Return the file descriptor given a PlainSocketImpl
 */
static int getFD(JNIEnv *env, jobject this)
{
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_fdID));
    CHECK_NULL_RETURN(fdObj, -1);
    return (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainSocketImpl, IO_fd_fdID));
}

static int getSocketAddressFamily(JNIEnv *env, jobject this)
{
    jobject iaObj = (*env)->GetObjectField(env, this,
                        JNI_STATIC_MD(java_net_PlainSocketImpl, psi_addressID));
    CHECK_NULL_RETURN(iaObj, -1);

    int id = (*env)->GetIntField(env, iaObj,
                                 JNI_STATIC(java_net_InetAddress, ia_familyID));
    return id == IPv6? MPE_SOCKET_AF_INET6 : MPE_SOCKET_AF_INET4;
}

/*
 * The initProto function is called whenever PlainSocketImpl is
 * loaded, to cache fieldIds for efficiency. This is called everytime
 * the Java class is loaded.
 *
 * Class:     java_net_PlainSocketImpl
 * Method:    initProto
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainSocketImpl_initProto(JNIEnv *env, jclass cls)
{
    JNI_STATIC_MD(java_net_PlainSocketImpl, psi_fdID)
    = (*env)->GetFieldID(env, cls , "fd", "Ljava/io/FileDescriptor;");
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainSocketImpl, psi_fdID));
    JNI_STATIC_MD(java_net_PlainSocketImpl, psi_addressID)
    = (*env)->GetFieldID(env, cls, "address", "Ljava/net/InetAddress;");
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainSocketImpl, psi_addressID));
    JNI_STATIC_MD(java_net_PlainSocketImpl, psi_portID)
    = (*env)->GetFieldID(env, cls, "port", "I");
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainSocketImpl, psi_portID));
    JNI_STATIC_MD(java_net_PlainSocketImpl, psi_localportID)
    = (*env)->GetFieldID(env, cls, "localport", "I");
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainSocketImpl, psi_localportID) );
    JNI_STATIC_MD(java_net_PlainSocketImpl, psi_timeoutID)
    = (*env)->GetFieldID(env, cls, "timeout", "I");
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainSocketImpl, psi_timeoutID));

    JNI_STATIC_MD(java_net_PlainSocketImpl, psi_trafficClassID) =
    (*env)->GetFieldID(env, cls, "trafficClass", "I");
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainSocketImpl, psi_trafficClassID));
    JNI_STATIC_MD(java_net_PlainSocketImpl, psi_serverSocketID) =
    (*env)->GetFieldID(env, cls, "serverSocket", "Ljava/net/ServerSocket;");
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainSocketImpl, psi_serverSocketID));
    JNI_STATIC_MD(java_net_PlainSocketImpl, psi_fdLockID) =
    (*env)->GetFieldID(env, cls, "fdLock", "Ljava/lang/Object;");
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainSocketImpl, psi_fdLockID));
    JNI_STATIC_MD(java_net_PlainSocketImpl, psi_closePendingID) =
    (*env)->GetFieldID(env, cls, "closePending", "Z");
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainSocketImpl, psi_closePendingID));
    JNI_STATIC_MD(java_net_PlainSocketImpl, IO_fd_fdID)
    = NET_GetFileDescriptorID(env);
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainSocketImpl, IO_fd_fdID));
}

/* a global reference to the java.net.SocketException class. In
 * socketCreate, we ensure that this is initialized. This is to
 * prevent the problem where socketCreate runs out of file
 * descriptors, and is then unable to load the exception class.
 */

/*
 * Class:     java_net_PlainSocketImpl
 * Method:    socketCreate
 * Signature: (Z)V */
JNIEXPORT void JNICALL
Java_java_net_PlainSocketImpl_socketCreate(JNIEnv *env, jobject this,
        jboolean stream)
{
    jobject fdObj, ssObj;
    mpe_Socket fd;
    int domain = MPE_SOCKET_AF_INET4;

    if (JNI_STATIC_MD(java_net_PlainSocketImpl, socketExceptionCls) == NULL)
    {
        jclass c = (*env)->FindClass(env, "java/net/SocketException");
        JNI_STATIC_MD(java_net_PlainSocketImpl, socketExceptionCls)
        = (jclass)(*env)->NewGlobalRef(env, c);
        if (JNI_STATIC_MD(java_net_PlainSocketImpl, socketExceptionCls) == NULL)
        {
            return;
        }
    }
    fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_fdID));

    if (fdObj == NULL)
    {
        (*env)->ThrowNew(env, JNI_STATIC_MD(java_net_PlainSocketImpl, socketExceptionCls), "null fd object");
        return;
    }

    fd = NET_Socket(domain, (stream ? MPE_SOCKET_STREAM: MPE_SOCKET_DATAGRAM), 0);

    if (fd == JVM_IO_ERR)
    {
        domain = MPE_SOCKET_AF_INET6;
        fd = NET_Socket(domain, (stream ? MPE_SOCKET_STREAM: MPE_SOCKET_DATAGRAM), 0);
        /* note: if you run out of fds, you may not be able to load
         * the exception class, and get a NoClassDefFoundError
         * instead.
         */
        if (fd == JVM_IO_ERR)
        {
            (*env)->ThrowNew(env, JNI_STATIC_MD(java_net_PlainSocketImpl, socketExceptionCls), 0);
            return;
        }

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "got IPv6 socket\n");
    }
    else
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "got IPv4 socket\n");
    }

    (*env)->SetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainSocketImpl, IO_fd_fdID), fd);

    /*
     * If this is a server socket then enable SO_REUSEADDR
     * automatically
     */
#ifdef MPE_TARGET_OS_LINUX
    ssObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_serverSocketID));
     if (ssObj != NULL)
     {
     int arg = 1;
     NET_SetSockOpt(fd, MPE_SOCKET_SOL_SOCKET, MPE_SOCKET_SO_REUSEADDR, (void*)&arg, 4);
     }
#endif
}

/*
 * inetAddress is the address object passed to the socket connect
 * call.
 *
 * Class:     java_net_PlainSocketImpl
 * Method:    socketConnect
 * Signature: (Ljava/net/InetAddress;I)V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainSocketImpl_socketConnect(JNIEnv *env, jobject this,
        jobject iaObj, jint port, jint timeout)
{
    //jint localport = (*env)->GetIntField(env, this, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_localportID));
    jint localport;
    int len = 0;

    /* fdObj is the FileDescriptor field on this */
    //jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_fdID));
    jobject fdObj;
    jobject fdLock;

    //jint trafficClass = (*env)->GetIntField(env, this, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_trafficClassID));
    jint trafficClass;

    /* fd is an int field on iaObj */
    jint fd;

    mpe_SocketSockAddr him;
    /* The result of the connection */
    int connect_rv = -1;
    int error = 0;

    localport = (*env)->GetIntField(env, this, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_localportID));
    fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_fdID));
    trafficClass = (*env)->GetIntField(env, this, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_trafficClassID));

    if (IS_NULL(fdObj))
    {
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", "Socket closed");
        return;
    }
    else
    {
        fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainSocketImpl, IO_fd_fdID));
    }
    if (IS_NULL(iaObj))
    {
        JNU_ThrowNullPointerException(env, "inet address argument null.");
        return;
    }

    /* connect */
    NET_InetAddressToSockaddr(env, iaObj, port, (struct sockaddr *)&him, &len);

    if (trafficClass != 0)
    {
        NET_SetTrafficClass((mpe_SocketSockAddr*)&him, trafficClass);
    }

    if (timeout <= 0)
    {
        connect_rv = NET_Connect(fd, (mpe_SocketSockAddr *)&him, len);
    }
    else
    {
        /* 
         * A timeout was specified. We put the socket into non-blocking
         * mode, connect, and then wait for the connection to be 
         * established, fail, or timeout.
         */
        SET_NONBLOCKING(fd);

        /* no need to use NET_Connect as non-blocking */
        connect_rv = NET_Connect(fd, (mpe_SocketSockAddr *)&him, len);

        /* connection not established immediately */
        if (connect_rv != 0)
        {
            int optlen;
            jlong prevTime = JVM_CurrentTimeMillis(env, 0);

            int lastError = NET_GetLastError();
            // Note: windows doesn't follow BSD standard
            if ((lastError != MPE_SOCKET_EINPROGRESS) && (lastError != MPE_SUCCESS))
            {
                NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "ConnectException",
                        "connect failed");
                SET_BLOCKING(fd);
                return;
            }

            /*
             * Wait for the connection to be established or a
             * timeout occurs. poll/select needs to handle EINTR in
             * case lwp sig handler redirects any process signals to
             * this thread.
             */
            while (1)
            {
                jlong newTime;
                mpe_SocketFDSet wr, ex;
                struct timeval t;

                t.tv_sec = timeout / 1000;
                t.tv_usec = (timeout % 1000) * 1000;

                mpe_socketFDZero(&wr);
                mpe_socketFDSet(fd, &wr);
                mpe_socketFDZero(&ex);
                mpe_socketFDSet(fd, &ex);

                connect_rv = NET_Select(fd+1, 0, &wr, &ex, &t);

                if (connect_rv >= 0 || NET_GetLastError() != MPE_SOCKET_EINTR)
                {
                    break;
                }

                /*
                 * The poll was interrupted so adjust timeout and
                 * restart
                 */
                newTime = JVM_CurrentTimeMillis(env, 0);
                timeout -= (newTime - prevTime);
                if (timeout <= 0)
                {
                    connect_rv = 0;
                    break;
                }
                newTime = prevTime;

            } /* while */

            if (connect_rv == 0) /* timed-out */
            {
                JNU_ThrowByName(env, JNU_JAVANETPKG "SocketTimeoutException",
                        "connect timed out");
                /*
                 * Timeout out but connection may still be established.
                 * At the high level it should be closed immediately but
                 * just in case we make the socket blocking again and
                 * shutdown input & output.
                 */
                SET_BLOCKING(fd);
                NET_SocketShutdown(fd, 2);
                return;
            }

            /* has connection been established */
            optlen = sizeof(error);
            if (NET_GetSockOpt(fd, SOL_SOCKET, SO_ERROR, (void*)&error, &optlen) < 0)
            {
                error = NET_GetLastError();
            }
            // some flavor of linux indicate fd is ready when socket has error
            if ((error != MPE_SOCKET_EINPROGRESS) && (error != MPE_SUCCESS))
            {
                connect_rv = -1;
            } 
        }

        /* make socket blocking again */
        SET_BLOCKING(fd);

        if (connect_rv != 0 && error == 0)
        {
            error = JVM_IO_ERR;
        }
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "Java_java_net_PlainSocketImpl_socketConnect: connect status = %d, error = %d\n",
            connect_rv, error);

    /* report the appropriate exception */
    if (connect_rv < 0)
    {
        if (error == JVM_IO_ERR)
        {
            JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
                    "Invalid argument or cannot assign requested address");
            return;
        }

        if (error == JVM_IO_INTR)
        {
            JNU_ThrowByName(env, JNU_JAVAIOPKG "InterruptedIOException",
                    "operation interrupted");
        }
        else if (error == MPE_SOCKET_EPROTO)
        {
            NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "ProtocolException",
                    "Protocol error");
        }
        else if (error == MPE_SOCKET_ECONNREFUSED)
        {
            JNU_ThrowByName(env, JNU_JAVANETPKG "ConnectException",
                    "Connection refused");
        }
        else if (error == MPE_SOCKET_ETIMEDOUT)
        {
            JNU_ThrowByName(env, JNU_JAVANETPKG "ConnectException",
                    "Connection timed out");
        }
        else if (error == MPE_SOCKET_EHOSTUNREACH)
        {
            JNU_ThrowByName(env, JNU_JAVANETPKG "NoRouteToHostException",
                    "Host unreachable");
        }
        else if (error == MPE_SOCKET_EADDRNOTAVAIL)
        {
            JNU_ThrowByName(env, JNU_JAVANETPKG "NoRouteToHostException",
                    "Address not available");
        }
        else if ((error == MPE_SOCKET_EISCONN) || (error == MPE_SOCKET_EBADF))
        {
            JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
                    "Socket closed");
        }
        else
        {
            NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException", "connect failed");
        }
        return;
    }

    /*
     * The socket may have been closed (dup'ed) while we were
     * poll/select. In that case SO_ERROR will return 0 making
     * it appear that the connection has been established.
     * To avoid any race conditions we therefore grab the
     * fd lock, check if the socket has been closed, and 
     * set the various fields whilst holding the lock
     */
    fdLock = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_fdLockID));
    (*env)->MonitorEnter(env, fdLock);

    if ((*env)->GetBooleanField(env, this, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_closePendingID)))
    {
        /* release fdLock */
        (*env)->MonitorExit(env, fdLock);

        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
                "Socket closed");
        return;
    }

    (*env)->SetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainSocketImpl, IO_fd_fdID), fd);

    /* set the remote peer address and port */
    (*env)->SetObjectField(env, this, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_addressID), iaObj);
    (*env)->SetIntField(env, this, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_portID), port);

    /*
     * we need to initialize the local port field if bind was called
     * previously to the connect (by the client) then localport field
     * will already be initialized
     */
    if (localport == 0)
    {
        /* Now that we're a connected socket, let's extract the port number
         * that the system chose for us and store it in the Socket object.
         */
        len = SOCKADDR_LEN;
        if (NET_GetSockName(fd, (mpe_SocketSockAddr *)&him, &len) == -1)
        {
            NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException", "Error getting socket name");
        }
        else
        {
            localport = NET_GetPortFromSockaddr((mpe_SocketSockAddr *)&him);
            (*env)->SetIntField(env, this, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_localportID), localport);
        }
    }

    /*
     * Finally release fdLock
     */
    (*env)->MonitorExit(env, fdLock);
}

/*
 * Class:     java_net_PlainSocketImpl
 * Method:    socketBind
 * Signature: (Ljava/net/InetAddress;I)V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainSocketImpl_socketBind(JNIEnv *env, jobject this, jobject iaObj, jint localport)
{
    /* fdObj is the FileDescriptor field on this */
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_fdID));
    /* fd is an int field on fdObj */
    int fd;
    int len;
    mpe_SocketSockAddr him;
    int error = 0;

    if (IS_NULL(fdObj))
    {
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", "Socket closed");
        return;
    }
    else
    {
        fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainSocketImpl, IO_fd_fdID));
    }
    if (IS_NULL(iaObj))
    {
        JNU_ThrowNullPointerException(env, "iaObj is null.");
        return;
    }

    /* bind */
    NET_InetAddressToSockaddr(env, iaObj, localport, (mpe_SocketSockAddr *)&him, &len);

    if (NET_Bind(fd, (mpe_SocketSockAddr *)&him, len) < 0)
    {
        error = NET_GetLastError();
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

    /* set the address */
    (*env)->SetObjectField(env, this, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_addressID), iaObj);

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
        (*env)->SetIntField(env, this, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_localportID), localport);
    }
    else
    {
        (*env)->SetIntField(env, this, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_localportID), localport);
    }
}

/*
 * Class:     java_net_PlainSocketImpl
 * Method:    socketListen
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainSocketImpl_socketListen (JNIEnv *env, jobject this, jint count)
{
    /* this FileDescriptor fd field */
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_fdID));
    /* fdObj's int fd field */
    mpe_Socket fd;

    if (IS_NULL(fdObj))
    {
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", "Socket closed");
        return;
    }
    else
    {
        fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainSocketImpl, IO_fd_fdID));
    }

    if (NET_Listen(fd, count) == JVM_IO_ERR)
    {
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", 0);
    }
}

/*
 * Class:     java_net_PlainSocketImpl
 * Method:    socketAccept
 * Signature: (Ljava/net/SocketImpl;)V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainSocketImpl_socketAccept(JNIEnv *env, jobject this, jobject socket)
{
    /* fields on this */
    int port;
    jint timeout = (*env)->GetIntField(env, this, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_timeoutID));
    jlong prevTime = 0;
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_fdID));

    /* the FileDescriptor field on socket */
    jobject socketFdObj;
    /* the InetAddress field on socket */
    jobject socketAddressObj;

    /* the ServerSocket fd int field on fdObj */
    jint fd;

    /* accepted fd */
    jint newfd;

    mpe_SocketSockAddr him;
    int len = SOCKADDR_LEN;

    int error = 0;

    if (IS_NULL(fdObj))
    {
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException","Socket closed");
        return;
    }
    else
    {
        fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainSocketImpl, IO_fd_fdID));
    }

    if (IS_NULL(socket))
    {
        JNU_ThrowNullPointerException(env, "socket is null");
        return;
    }

    /* 
     * accept connection but ignore ECONNABORTED indicating that
     * connection was eagerly accepted by the OS but was reset
     * before accept() was called.
     *
     * If accept timeout in place and timeout is adjusted with
     * each ECONNABORTED to ensure that semantics of timeout are
     * preserved.
     */
    for (;;)
    {
        if (timeout)
        {
            int ret;

            /* first usage pick up current time */
            if (prevTime == 0)
            prevTime = JVM_CurrentTimeMillis(env, 0);

            ret = NET_Timeout(fd, timeout);
            if (ret == 0)
            {
                JNU_ThrowByName(env, JNU_JAVANETPKG "SocketTimeoutException",
                        "Accept timed out");
                return;
            }
            else if (ret == JVM_IO_ERR)
            {
                if ((error = NET_GetLastError()) == MPE_SOCKET_EBADF)
                {
                    JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", "Socket closed");
                }
                else
                {
                    NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                            "Accept failed");
                }
                return;
            }
            else if (ret == JVM_IO_INTR)
            {
                JNU_ThrowByName(env, JNU_JAVAIOPKG "InterruptedIOException",
                        "operation interrupted");
                return;
            }
        }

        newfd = NET_Accept(fd, (mpe_SocketSockAddr *)&him, &len);

        /* connection accepted */
        if (newfd >= 0)
        {
            break;
        }

        /* non-ECONNABORTED error */
        if ((error = NET_GetLastError()) != MPE_SOCKET_ECONNABORTED)
        {
            break;
        }

        /* ECONNABORTED error so adjust timeout */
        if (timeout)
        {
            jlong currTime = JVM_CurrentTimeMillis(env, 0);
            timeout -= (currTime - prevTime);

            if (timeout <= 0)
            {
                JNU_ThrowByName(env, JNU_JAVANETPKG "SocketTimeoutException",
                        "Accept timed out");
                return;
            }
            prevTime = currTime;
        }
    }

    if (newfd < 0)
    {
        if (newfd == -2)
        {
            JNU_ThrowByName(env, JNU_JAVAIOPKG "InterruptedIOException",
                    "operation interrupted");
        }
        else
        {
            if (error == MPE_EINVAL)
            {
                error = MPE_SOCKET_EBADF;
            }
            if (error == MPE_SOCKET_EBADF)
            {
                JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", "Socket closed");
            }
            else
            {
                NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                        "Accept failed");
            }
        }
        return;
    }

    /*
     * fill up the remote peer port and address in the new socket structure.
     */
    socketAddressObj = NET_SockaddrToInetAddress(env, (mpe_SocketSockAddr *)&him, &port);
    if (socketAddressObj == NULL)
    {
        /* should be pending exception */
        NET_SocketClose(newfd);
        return;
    }

    /*
     * Populate SocketImpl.fd.fd
     */
    socketFdObj = (*env)->GetObjectField(env, socket, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_fdID));
    (*env)->SetIntField(env, socketFdObj, JNI_STATIC_MD(java_net_PlainSocketImpl, IO_fd_fdID), newfd);

    (*env)->SetObjectField(env, socket, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_addressID), socketAddressObj);
    (*env)->SetIntField(env, socket, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_portID), port);

    /* also fill up the local port information */
    port = (*env)->GetIntField(env, this, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_localportID));
    (*env)->SetIntField(env, socket, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_localportID), port);
}

/*
 * Class:     java_net_PlainSocketImpl
 * Method:    socketAvailable
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_java_net_PlainSocketImpl_socketAvailable(JNIEnv *env, jobject this)
{
    jint ret = -1;
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_fdID));
    mpe_Socket fd;

    if (IS_NULL(fdObj))
    {
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
                "Socket closed");
        return -1;
    }
    else
    {
        fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainSocketImpl, IO_fd_fdID));
    }
    /* NET_SocketAvailable returns -1 for failure, 0 for success */
    if (NET_SocketAvailable(fd, &ret) == -1)
    {
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", 0);
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "Java_java_net_PlainSocketImpl_socketAvailable: fd = %x, available = %d\n", fd, ret);
    return ret;
}

/*
 * Class:	java/net/PlainSocketImpl
 * Method:	socketClose0
 * Signature:	(Z)V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainSocketImpl_socketClose0(JNIEnv *env, jobject this, jboolean useDeferredClose)
{
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_fdID));
    jint fd;
    static int marker_fd = -1;

    /*
     * WARNING: THIS NEEDS LOCKING. ALSO: SHOULD WE CHECK for fd being
     * -1 already?
     */
    if (IS_NULL(fdObj))
    {
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", "socket already closed");
        return;
    }
    else
    {
        fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainSocketImpl, IO_fd_fdID));
    }
    if (fd != -1)
    {
        (*env)->SetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainSocketImpl, IO_fd_fdID), -1);
        NET_SocketClose(fd);
    }
}

/*
 * Class:     java_net_PlainSocketImpl
 * Method:    socketShutdown
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainSocketImpl_socketShutdown(JNIEnv *env, jobject this, jint howto)
{
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_fdID));
    mpe_Socket fd;

    if (IS_NULL(fdObj))
    {
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", "socket already closed");
        return;
    }
    else
    {
        fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainSocketImpl, IO_fd_fdID));
    }
    NET_SocketShutdown(fd, howto);
}

/*
 * Class:     java_net_PlainSocketImpl
 * Method:    socketSetOption
 * Signature: (IZLjava/lang/Object;)V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainSocketImpl_socketSetOption(JNIEnv *env, jobject this, jint cmd, jboolean on, jobject value)
{
    int fd;
    int level, optname, optlen;
    union
    {
        int i;
        struct linger ling;
    }optval;

    /* 
     * Check that socket hasn't been closed 
     */
    fd = getFD(env, this);
    if (fd < 0)
    {
        /* the fd is closed */
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException","Socket closed");
        return;
    }

    /*
     * SO_TIMEOUT is a no-op on Solaris/Linux
     */
    if (cmd == java_net_SocketOptions_SO_TIMEOUT)
    {
        return;
    }

    /*
     * Map the Java level socket option to the platform specific
     * level and option name.
     */
    if (IPv6 == getSocketAddressFamily(env, this))
    {
        if (NET_MapSocketOptionV6(cmd, &level, &optname))
        {
            JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
                            "Invalid option");
            return;
        }
    }
    else
    {
        if (NET_MapSocketOption(cmd, &level, &optname))
        {
            JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
                            "Invalid option");
            return;
        }
    }

    switch (cmd)
    {
        case java_net_SocketOptions_SO_SNDBUF :
        case java_net_SocketOptions_SO_RCVBUF :
        case java_net_SocketOptions_SO_LINGER :
        case java_net_SocketOptions_IP_TOS :
        {
            jclass cls;
            jfieldID fid;

            cls = (*env)->FindClass(env, "java/lang/Integer");
            CHECK_NULL(cls);
            fid = (*env)->GetFieldID(env, cls, "value", "I");
            CHECK_NULL(fid);

            if (cmd == java_net_SocketOptions_SO_LINGER)
            {
                if (on)
                {
                    optval.ling.l_onoff = 1;
                    optval.ling.l_linger = (*env)->GetIntField(env, value, fid);
                }
                else
                {
                    optval.ling.l_onoff = 0;
                    optval.ling.l_linger = 0;
                }
                optlen = sizeof(optval.ling);
            }
            else
            {
                optval.i = (*env)->GetIntField(env, value, fid);
                optlen = sizeof(optval.i);
            }

            break;
        }

        /* Boolean -> int */
        default :
        optval.i = (on ? 1 : 0);
        optlen = sizeof(optval.i);
    }

    if (NET_SetSockOpt(fd, level, optname, (const void *)&optval, optlen) < 0)
    {
        NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                "Error setting socket option");
    }

}

/*
 * Class:     java_net_PlainSocketImpl
 * Method:    socketGetOption
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL
Java_java_net_PlainSocketImpl_socketGetOption(JNIEnv *env, jobject this, jint cmd, jobject iaContainerObj)
{
    int fd;
    int level, optname, optlen;
    union
    {
        int i;
        struct linger ling;
    }optval;

    /*
     * Check that socket hasn't been closed
     */
    fd = getFD(env, this);
    if(fd < 0)
    {
        /* the fd is closed */
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException","Socket closed");
        return -1;
    }

    /*
     * SO_BINDADDR isn't a socket option
     */
    if (cmd == java_net_SocketOptions_SO_BINDADDR)
    {
        mpe_SocketSockAddr him;
        int len = 0;
        int port;
        jobject iaObj;
        jclass iaCntrClass;
        jfieldID iaFieldID;

        len = SOCKADDR_LEN;

        if ( NET_GetSockName(fd, (mpe_SocketSockAddr *)&him, &len) == -1)
        {
            NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                    "Error getting socket name");
            return -1;
        }
        iaObj = NET_SockaddrToInetAddress(env, (mpe_SocketSockAddr *)&him, &port);
        CHECK_NULL_RETURN(iaObj, -1);

        iaCntrClass = (*env)->GetObjectClass(env, iaContainerObj);
        iaFieldID = (*env)->GetFieldID(env, iaCntrClass, "addr", "Ljava/net/InetAddress;");
        CHECK_NULL_RETURN(iaFieldID, -1);
        (*env)->SetObjectField(env, iaContainerObj, iaFieldID, iaObj);
        return 0; /* notice change from before */
    }

    /*
     * Map the Java level socket option to the platform specific
     * level and option name.
     */
    if (IPv6 == getSocketAddressFamily(env, this))
    {
        if (NET_MapSocketOptionV6(cmd, &level, &optname))
        {
            JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
                            "Invalid option");
            return -1;
        }
    }
    else
    {
        if (NET_MapSocketOption(cmd, &level, &optname))
        {
            JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
                            "Invalid option");
            return -1;
        }
    }

    /*
     * Args are int except for SO_LINGER
     */
    if (cmd == java_net_SocketOptions_SO_LINGER)
    {
        optlen = sizeof(optval.ling);
    }
    else
    {
        optlen = sizeof(optval.i);
    }

    if (NET_GetSockOpt(fd, level, optname, (void *)&optval, &optlen) < 0)
    {
        NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                "Error getting socket option");
        return -1;
    }

    switch (cmd)
    {
        case java_net_SocketOptions_SO_LINGER:
        return (optval.ling.l_onoff ? optval.ling.l_linger: -1);

        case java_net_SocketOptions_SO_SNDBUF:
        case java_net_SocketOptions_SO_RCVBUF:
        case java_net_SocketOptions_IP_TOS:
        return optval.i;

        default :
        return (optval.i == 0) ? -1 : 1;
    }
}

/*
 * Class:	java/net/PlainSocketImpl
 * Method:	socketSendUrgentData
 * Signature:	(I)V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainSocketImpl_socketSendUrgentData(JNIEnv *env, jobject this, jint data)
{
    /* The fd field */
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainSocketImpl, psi_fdID));
    int n, fd;
    unsigned char d = data & 0xFF;

    if (IS_NULL(fdObj))
    {
        JNU_ThrowByName(env, "java/net/SocketException", "Socket closed");
        return;
    }
    else
    {
        fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainSocketImpl, IO_fd_fdID));
        /* Bug 4086704 - If the Socket associated with this file descriptor
         * was closed (sysCloseFD), the the file descriptor is set to -1.
         */
        if (fd == -1)
        {
            JNU_ThrowByName(env, "java/net/SocketException", "Socket closed");
            return;
        }
    }
    n = NET_Send(fd, (char *)&d, 1, MSG_OOB);
    if (n == JVM_IO_ERR)
    {
        NET_ThrowByNameWithLastError(env, "java/io/IOException", "Write failed");
        return;
    }
    if (n == JVM_IO_INTR)
    {
        JNU_ThrowByName(env, "java/io/InterruptedIOException", 0);
        return;
    }
}

