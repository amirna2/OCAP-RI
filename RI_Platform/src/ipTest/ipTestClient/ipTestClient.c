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

#include "ipTest.h"

// contain all the module data required:
static struct ipTestClientData
{
    char mAddr[48];        // server address to test
    int mAddrFamily;       // client AF (AF_INET or AF_INET6)
    int mProto;            // client protocol (UDP or TCP)
    int mPort;             // client port
    int mType;             // client type (SOCK_STREAM or SOCK_DGRAM)
    int mSock;             // client socket
} data;

void testInit(char *addr, int port, int proto, int family)
{
    char str[INET6_ADDRSTRLEN] = {0};
    char portStr[8] = {0};
    struct addrinfo hints = {0};
    struct addrinfo* srvrInfo = NULL;
    struct addrinfo* pSrvr = NULL;
    int ret = 0;
#ifdef RI_WIN32_SOCKETS
    WSADATA wsd;
#endif

    RILOG_INFO("%s(%s, %d, %d, %d);\n", __func__, addr, port, proto, family);

    if (0 > snprintf(data.mAddr, sizeof(data.mAddr), "%s", addr))
    {
        RILOG_FATAL(1, "%s snprintf failure?!\n", __func__);
    }

#ifdef RI_WIN32_SOCKETS
    if (WSAStartup(MAKEWORD(2, 2), &wsd))
    {
        RILOG_ERROR("%s WSAStartup() failed?\n", __FUNCTION__);
        return;
    }
#endif

    data.mAddrFamily = family;
    data.mProto = proto;
    data.mPort = port;
    data.mType = proto == IPPROTO_TCP? SOCK_STREAM : SOCK_DGRAM;
    data.mSock = 0;
    hints.ai_family = data.mAddrFamily;
    hints.ai_socktype = data.mType;
    hints.ai_flags = AI_PASSIVE;
    snprintf(portStr, sizeof(portStr), "%d", data.mPort);

    if (0 != (ret = getaddrinfo(data.mAddr, "0", &hints, &srvrInfo)))
    {
        RILOG_ERROR("%s: getaddrinfo[%s]\n", __FUNCTION__, gai_strerror(ret));
        return;
    }

    for(pSrvr = srvrInfo; pSrvr != NULL; pSrvr = pSrvr->ai_next)
    {
        // skip over the IPv4 interfaces and bind to the first IPv6 which
        // will accept connections from IPv4 or IPv6 and translate...
        if ((AF_INET6 == data.mAddrFamily) && (AF_INET == pSrvr->ai_family))
        {
            continue;
        }

        if (0 > (data.mSock = socket(pSrvr->ai_family,
                                     pSrvr->ai_socktype,
                                     pSrvr->ai_protocol)))
        {
            RILOG_WARN("%s socket() failed?\n", __FUNCTION__);
            continue;
        }

        break;
    }

    if (NULL == pSrvr)
    {
        RILOG_WARN("%s failed to bind to %s\n", __FUNCTION__, data.mAddr);
        freeaddrinfo(srvrInfo);
        return;
    }
    else
    {
        net_ntop(pSrvr->ai_family, pSrvr->ai_addr, str, sizeof(str));
        RILOG_INFO("%s bound to %s\n", __FUNCTION__, str);
        freeaddrinfo(srvrInfo);
    }

    if (0 != (ret = getaddrinfo(data.mAddr, portStr, &hints, &srvrInfo)))
    {
        RILOG_ERROR("%s: getaddrinfo[%s]\n", __FUNCTION__, gai_strerror(ret));
        return;
    }

    for(pSrvr = srvrInfo; pSrvr != NULL; pSrvr = pSrvr->ai_next)
    {
        // skip over the IPv4 interfaces and bind to the first IPv6 which
        // will accept connections from IPv4 or IPv6 and translate...
        if ((AF_INET6 == data.mAddrFamily) && (AF_INET == pSrvr->ai_family))
        {
            continue;
        }

        if (SOCK_STREAM == data.mType)
        {
            if (0 > (connect(data.mSock, pSrvr->ai_addr, pSrvr->ai_addrlen)))
            {
                CLOSESOCK(data.mSock);
                RILOG_ERROR("%s connect() failed?\n", __FUNCTION__);
                continue;
            }
        }

        // We successfully connected!
        break;
    }

    if (NULL == pSrvr)
    {
        RILOG_WARN("%s failed to connect to %s\n", __FUNCTION__, data.mAddr);
        freeaddrinfo(srvrInfo);
        return;
    }
    else
    {
        net_ntop(pSrvr->ai_family, pSrvr->ai_addr, str, sizeof(str));
        RILOG_INFO("%s connected to %s\n", __FUNCTION__, str);
        freeaddrinfo(srvrInfo);
    }

#if 1
    (void) testThread(pSrvr);
#else
    if(NULL == (data.telnet = g_thread_create(testThread, pSrvr, FALSE,0)))
    {
        RILOG_ERROR("g_thread_create() returned NULL?!\n");
    }
#endif
}

void testAbort(void)
{
    CLOSESOCK(data.mSock);
    RILOG_WARN("%s\n", __func__);
}

int udpPing(int sock, char *rxBuf, int size, struct addrinfo* pSrvr)
{
    int bytesSent = 0;
    unsigned int addrlen = pSrvr->ai_addrlen;

    RILOG_INFO("%sing %s:%d\n", __func__, data.mAddr, data.mPort);

    if ((bytesSent = sendto(sock, rxBuf, size - 1, 0,
                            pSrvr->ai_addr, pSrvr->ai_addrlen)) <= 0)
    {
        RILOG_ERROR("%s sendto() failed?\n", __func__);
        return 0;
    }
    else if (recvfrom(sock, rxBuf, bytesSent, 0,
                            pSrvr->ai_addr, &addrlen) != bytesSent)
    {
        RILOG_ERROR("%s recv() failed?\n", __func__);
        return 0;
    }

    rxBuf[bytesSent] = 0;
    RILOG_DEBUG("%d = %s(%d);\n== %s\n", bytesSent, __func__, sock, rxBuf);
    return bytesSent;
}

int tcpPing(int sock, char *rxBuf, int size)
{
    int bytesSent = 0;

    RILOG_INFO("%sing %s:%d\n", __func__, data.mAddr, data.mPort);

    if ((bytesSent = send(sock, rxBuf, size, 0)) <= 0)
    {
        RILOG_ERROR("%s send() failed?\n", __func__);
        return 0;
    }
    else if (recv(sock, rxBuf, bytesSent, 0) != bytesSent)
    {
        RILOG_ERROR("%s recv() failed?\n", __func__);
        return 0;
    }

    rxBuf[bytesSent] = 0;
    RILOG_DEBUG("%d = %s(%d);\n== %s\n", bytesSent, __func__, sock, rxBuf);
    return bytesSent;
}

gpointer testThread(gpointer in)
{
    char rxBuf[65] =
           "ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789\0";
    int i;

    RILOG_INFO("%s(%p); for %s:%d\n", __func__, in, data.mAddr, data.mPort);

    for (i = 0; i < 3; i++)
    {
        if (SOCK_STREAM == data.mType)
        {
            if (!tcpPing(data.mSock, rxBuf, sizeof(rxBuf)))
            {
                break;
            }
        }
        else
        {
            if (!udpPing(data.mSock, rxBuf, sizeof(rxBuf), in))
            {
                break;
            }
        }
    }

    if (SOCK_STREAM == data.mType)
    {
        (void) send(data.mSock, "exit", sizeof("exit"), 0);
    }
    else
    {
        struct addrinfo* pSrvr = in;
        (void) sendto(data.mSock, "exit", sizeof("exit"), 0,
                      pSrvr->ai_addr, pSrvr->ai_addrlen);
    }

    CLOSESOCK(data.mSock);
    RILOG_INFO("%s exiting...\n", __func__);
    return NULL;
}

