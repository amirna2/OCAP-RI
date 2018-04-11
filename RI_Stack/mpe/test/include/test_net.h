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

#ifndef _TEST_NET_H_
#define _TEST_NET_H_ 1

#include <mpeTest.h>
#include <mpetest_net.h>

#include "test_utils.h"
#include <mpetest_sys.h>

#define SOCKET_FAIL -1
#define MAXCONNECTATTEMPTS 5
#define TEST_NET_HOSTNAMEBUFFLEN  128

typedef struct _ThreadData
{
    CuTest *tc;
    int port;
    int32_t fd;
} ThreadData;

typedef struct _SendThreadData
{
    CuTest* tc;
    int port;
    const char* message;
    mpe_Socket* sfd;

} SendThreadData;

typedef struct _ServerSock
{
    mpe_SocketIPv4SockAddr server_addr;
    int32_t recvSock;

} ServerSock;

typedef struct _ClientSock
{
    mpe_SocketIPv4SockAddr client_addr;
    int32_t sendSock;

} ClientSock;

typedef struct _NetTestAddrv4 /* IPv4 test addresses  */
{
    char *strAddr; /*  string representation of IP address   */
    uint32_t numAddr; /*  numeric representation of IP address  */
} NetTestAddrv4;

typedef struct _NetTestAddrv6 /* IPv6 test addresses  */
{
    char *strAddr; /*  string representation of IP address   */
    mpe_SocketIPv6Addr numAddr; /*  numeric representation of IP address  */
}NetTestAddrv6;

extern mpe_Bool gConnectLimitFlag;
extern mpe_Cond g_cond;

void nettestConnectThread(void * threadData);
void nettestMultiConnectThread(void * threadData);
void testNet_SendThread(void*);
void popSendStructure(SendThreadData * std, CuTest * tc, int port,
        const char * message, mpe_Socket * fd);
mpe_Bool clientSetup1(int p, CuTest * t, ClientSock * c, mpe_Socket * s);
void mpetest_SendThread2(void * sendThreadData);
int getUniquePort(void);
void testNet_waitForThread(void);

NATIVEEXPORT_API void test_netRunAllTests(void);
NATIVEEXPORT_API void test_netRunLookupTests(void);
NATIVEEXPORT_API void test_netRunConnectTests(void);
NATIVEEXPORT_API void test_netRunOptionsTests(void);
NATIVEEXPORT_API void test_netRunSelectTests(void);
NATIVEEXPORT_API void test_mpe_netReadWriteTests(void);
NATIVEEXPORT_API void test_mpe_netRunSocketNegTest(void);

#endif /* #ifndef _TEST_NET_H_ */
