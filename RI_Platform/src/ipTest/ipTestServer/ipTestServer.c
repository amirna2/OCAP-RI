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
static struct ipTestServerData
{
    char mBindAddr[48];    // interface to bind
    int mAddrFamily;       // listener AF (AF_INET or AF_INET6)
    int mProto;            // listener protocol (UDP or TCP)
    int mPort;             // listener port
    int mType;             // listener type (SOCK_STREAM or SOCK_DGRAM)
    int mSock;             // listener socket
    int mSd;               // connection socket descriptor
#ifdef WIN32
    PROCESS_INFORMATION mProcessInfo;
    STARTUPINFO mStartupInfo;
#else
    pid_t mPid;
#endif
} data;

#ifdef WIN32

#include <windows.h>

void usleep(int wait)
{
    __int64 start = 0, now = 0;

    wait *= 10; // scale
    QueryPerformanceCounter((LARGE_INTEGER *)&start);

    do
    {
        QueryPerformanceCounter((LARGE_INTEGER *) &now);

    } while((now-start) < wait);
}

#endif

void testInit(char *addr, int port, int proto, int family)
{
    char str[INET6_ADDRSTRLEN] = {0};
    char portStr[8] = {0};
    struct addrinfo hints = {0};
    struct addrinfo* srvrInfo = NULL;
    struct addrinfo* pSrvr = NULL;
    int yes = 1;
    int ret = 0;
#ifdef RI_WIN32_SOCKETS
    WSADATA wsd;
#endif

    RILOG_INFO("%s(%s, %d, %d, %d);\n", __func__, addr, port, proto, family);

    if (0 > snprintf(data.mBindAddr, sizeof(data.mBindAddr), "%s", addr))
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

    if (0 == strcmp(addr, "0"))
    {
        RILOG_INFO("%s: attempt any interface\n", __func__);

        if (0 != (ret = getaddrinfo(NULL, portStr, &hints, &srvrInfo)))
        {
            RILOG_ERROR("%s: getaddrinfo[%s]\n", __func__, gai_strerror(ret));
            return;
        }
    }
    else
    {
        if (0 != (ret =getaddrinfo(data.mBindAddr, portStr, &hints, &srvrInfo)))
        {
            RILOG_ERROR("%s: getaddrinfo[%s]\n", __func__, gai_strerror(ret));
            return;
        }
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

        if (0 > setsockopt(data.mSock, SOL_SOCKET, SO_REUSEADDR,
                           (char*) &yes, sizeof(yes)))
        {
            CLOSESOCK(data.mSock);
            RILOG_ERROR("%s setsockopt() failed?\n", __FUNCTION__);
            return;
        }

        if (0 > (bind(data.mSock, pSrvr->ai_addr, pSrvr->ai_addrlen)))
        {
            CLOSESOCK(data.mSock);
            RILOG_ERROR("%s bind() failed?\n", __FUNCTION__);
            continue;
        }

        // We successfully bound!
        break;
    }

    if (NULL == pSrvr)
    {
        RILOG_WARN("%s failed to bind\n", __FUNCTION__);
        freeaddrinfo(srvrInfo);
        return;
    }

    RILOG_INFO("%s bound on %s\n", __FUNCTION__, 
            net_ntop(pSrvr->ai_family, pSrvr->ai_addr, str, sizeof(str)));
    freeaddrinfo(srvrInfo);

#if 1
    (void) testThread(0);
#else
    if(NULL == (data.telnet = g_thread_create(testThread, 0, FALSE, 0)))
    {
        RILOG_ERROR("g_thread_create() returned NULL?!\n");
    }
#endif
}

void testAbort(void)
{
    CLOSESOCK(data.mSd);
    CLOSESOCK(data.mSock);
    RILOG_WARN("%s\n", __func__);
}

int udpEcho(int sock, char *rxBuf, int size)
{
    struct sockaddr_storage their_addr = {0}; // connector's address information
    char str[INET6_ADDRSTRLEN] = {0};
    unsigned int addr_len = sizeof(their_addr);
    int bytesRcvd = 0;

    while (1)
    {
        if ((bytesRcvd = recvfrom(sock, rxBuf, size - 1, 0,
                         (struct sockaddr *)&their_addr, &addr_len)) <= 0)
        {
            usleep(500000);
            fprintf(stderr, ".");
            fflush(stderr);
        }
        else
        {
            if (their_addr.ss_family == AF_UNSPEC)
            {
                if (addr_len == sizeof(struct sockaddr_in))
                {
                    their_addr.ss_family = AF_INET;
                }
                else if (addr_len == sizeof(struct sockaddr_in6))
                {
                    their_addr.ss_family = AF_INET6;
                }
            }

            rxBuf[bytesRcvd] = 0;
            RILOG_INFO("%s ping[%d] from %s = %s)\n", __func__, bytesRcvd,
                net_ntop(their_addr.ss_family, &their_addr, str, sizeof(str)),
                rxBuf);
            
            if (sendto(sock, rxBuf, bytesRcvd, 0,
                      (struct sockaddr *)&their_addr, addr_len) != bytesRcvd)
            {
                RILOG_ERROR("%s sendto(%d, %p, %d...) failed?\n", __func__,
                            sock, rxBuf, bytesRcvd);
            }
        }

        if (strstr(rxBuf, "exit"))
        {
            return FALSE;
        }
        else if (strstr(rxBuf, "quit"))
        {
            return TRUE;
        }
    }
}

int tcpEcho(int sock, char *rxBuf, int size)
{
    int bytesRcvd = 0;

    while (1)
    {
        if ((bytesRcvd = recv(sock, rxBuf, size - 1, 0)) <= 0)
        {
            usleep(500000);
            fprintf(stderr, ".");
            fflush(stderr);
        }
        else
        {
            rxBuf[bytesRcvd] = 0;
            RILOG_INFO("%s ping[%d] = %s)\n", __func__, bytesRcvd, rxBuf);

            if (send(sock, rxBuf, bytesRcvd, 0) != bytesRcvd)
            {
                RILOG_ERROR("%s send() failed?\n", __func__);
            }
        }

        if (strstr(rxBuf, "exit"))
        {
            return FALSE;
        }
        else if (strstr(rxBuf, "quit"))
        {
            return TRUE;
        }
    }
}

gpointer testThread(gpointer in)
{
    char str[INET6_ADDRSTRLEN] = {0};
    char rxBuf[RCVBUFSIZE];
    struct sockaddr_storage their_addr; // connector's address information
    int sin_size = 0;
    int quit = FALSE;

    RILOG_INFO("%s(%p); for %s:%d\n", __func__, in, data.mBindAddr, data.mPort);

    if (SOCK_STREAM == data.mType)
    {
        if (listen(data.mSock, BACKLOG) == -1)
        {
            int ret = 0;
#ifdef RI_WIN32_SOCKETS
            ret = WSAGetLastError();
#else
            ret = errno;
#endif
            RILOG_ERROR("%s listen(%d) failed with error: %d\n", __FUNCTION__,
                        data.mSock, ret);
            testAbort();
            return 0;
        }
    }

    while (quit == FALSE)
    {
        RILOG_INFO("%s waiting for a connection...\n", __FUNCTION__);

        if (SOCK_STREAM == data.mType)
        {
            sin_size = sizeof their_addr;
            data.mSd = accept(data.mSock,
                    (struct sockaddr*) &their_addr, (socklen_t*) &sin_size);
            if (data.mSd == -1)
            {
                RILOG_ERROR("%s accept(%d) failed\n", __FUNCTION__, data.mSock);
                continue;
            }

            RILOG_INFO("%s got connection from %s\n", __FUNCTION__,
                net_ntop(their_addr.ss_family, &their_addr, str, sizeof(str)));

            quit = tcpEcho(data.mSd, rxBuf, RCVBUFSIZE);
            usleep(500000);
            CLOSESOCK(data.mSd);
        }
        else
        {
            quit = udpEcho(data.mSock, rxBuf, RCVBUFSIZE);
        }
    }

    CLOSESOCK(data.mSock);
    RILOG_INFO("%s exiting...\n", __func__);
    return NULL;
}

