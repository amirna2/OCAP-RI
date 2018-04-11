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

#include "test_net.h"

static void SocketTest_Init(void);

extern CuSuite* getTestSuite_netLookup(void);
extern CuSuite* getTestSuite_netConnect(void);
extern CuSuite* getTestSuite_mpe_netOptions(void);
extern CuSuite* getTestSuite_netSelect(void);
extern CuSuite* getTestSuite_netReadWrite(void);

/*
 *  Network system inited flag. If true, indicates that 'mpeos_socketInit()'
 *  has been called already.
 */

#ifdef TEST_MPEOS
static int g_socketInited = FALSE; /*  initially indicate that 'socketInit() has not been called */
#else
#if !defined(MPE_LOG_DISABLE)
static int g_socketInited = TRUE; /*  if doing MPE testing, 'socketInit() has already been called */
#endif
#endif /* TEST_MPEOS */

static void SocketTest_Term(void);

/****************************************************************************

 'test_netRunAllTests(void)' - Runs all the network tests.

 ****/

NATIVEEXPORT_API void test_netRunAllTests()
{
    CuString* output;
    CuSuite* suiteToRun;
    CuSuite* suiteToAdd;

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "\n############\n#\n#   'test_netRunAllTests()' starting\n#\n");

    SocketTest_Init();

    suiteToRun = CuSuiteNew();

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "\n    'test_netRunAllTests()' - adding tests\n");

    /*  add connection tests  */

    suiteToAdd = getTestSuite_netConnect();
    CuSuiteAddSuite(suiteToRun, suiteToAdd);
    CU_FREE(suiteToAdd);

    /*  add lookup tests  */

    suiteToAdd = getTestSuite_netLookup();
    CuSuiteAddSuite(suiteToRun, suiteToAdd);
    CU_FREE(suiteToAdd);

    /*  add options tests  */

    suiteToAdd = getTestSuite_mpe_netOptions();
    CuSuiteAddSuite(suiteToRun, suiteToAdd);
    CU_FREE(suiteToAdd);

    /*  add read/write tests  */

    suiteToAdd = getTestSuite_netReadWrite();
    CuSuiteAddSuite(suiteToRun, suiteToAdd);
    CU_FREE(suiteToAdd);

    /*  add select tests  */

    suiteToAdd = getTestSuite_netSelect();
    CuSuiteAddSuite(suiteToRun, suiteToAdd);
    CU_FREE(suiteToAdd);

    /****
     *
     *  Run the tests
     *
     */

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
            "\n'test_netRunAllTests()' - Running tests . . .\n");

    CuSuiteRun(suiteToRun);

    /****
     *
     *  Report results
     *
     */

    output = CuStringNew();

    CuSuiteSummary(suiteToRun, output);
    CuSuiteDetails(suiteToRun, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_netRunAllTests() results :\n%s\n", output->buffer);

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "\ntest_netRunAllTests() - freeing suite\n");
    CuSuiteFree(suiteToRun);

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "\ntest_netRunAllTests() - freeing string\n");
    CuStringFree(output);

    SocketTest_Term();

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "\n#\n#'test_netRunAllTests()' - Tests complete\n#\n");

} /* test_netRunAllTests() */

/****************************************************************************
 *
 *  test_netRunLookupTests()
 *
 *    Runs all the network lookup tests from "test_mpe_net_lookup.c"
 *
 */

NATIVEEXPORT_API void test_netRunLookupTests(void)
{
    CuSuite *Suite;
    CuString *output;

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "\n#  'test_netRunLookupTests()' starting\n");

    SocketTest_Init();

    Suite = CuSuiteNew();
    CuSuiteAddSuite(Suite, getTestSuite_netLookup());

    CuSuiteRun(Suite);

    output = CuStringNew();
    CuSuiteSummary(Suite, output);
    CuSuiteDetails(Suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_netRunLookupTests() results :\n%s\n", output->buffer);

    CuSuiteFree(Suite);
    CuStringFree(output);

    SocketTest_Term();

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "\n#\n#'test_netRunLookupTests()' - Tests complete\n#\n");
}

/****************************************************************************
 *
 *  test_netRunConnectTests()
 *
 *    Runs all the network connection tests from "test_mpe_net_connect.c"
 *
 */

NATIVEEXPORT_API void test_netRunConnectTests(void)
{
    CuSuite *Suite;
    CuString *output;

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "\n#  'test_netRunConnectTests()' starting\n");

    SocketTest_Init();

    Suite = CuSuiteNew();
    CuSuiteAddSuite(Suite, getTestSuite_netConnect());

    CuSuiteRun(Suite);

    output = CuStringNew();
    CuSuiteSummary(Suite, output);
    CuSuiteDetails(Suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_netRunConnectTests() results :\n%s\n", output->buffer);

    CuSuiteFree(Suite);
    CuStringFree(output);

    SocketTest_Term();

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "\n#\n#'test_netRunConnectTests()' - Tests complete\n#\n");
}

/****************************************************************************
 *
 *  test_netRunOptionsTests()
 *
 *    Runs all the network connection tests from "test_mpe_net_options.c"
 *
 */

NATIVEEXPORT_API void test_netRunOptionsTests(void)
{
    CuSuite *Suite;
    CuString *output;

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "\n#  'test_netRunOptionsTests()' starting\n");

    SocketTest_Init();

    Suite = CuSuiteNew();
    CuSuiteAddSuite(Suite, getTestSuite_mpe_netOptions());

    CuSuiteRun(Suite);

    output = CuStringNew();
    CuSuiteSummary(Suite, output);
    CuSuiteDetails(Suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_netRunOptionsTests() results :\n%s\n", output->buffer);

    CuSuiteFree(Suite);
    CuStringFree(output);

    SocketTest_Term();

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "\n#\n#'test_netRunOptionsTests()' - Tests complete\n#\n");
}

/****************************************************************************
 *
 *  test_netRunSelectTests()
 *
 *    Runs all the network select tests from "test_mpe_net_select.c"
 *
 */

NATIVEEXPORT_API void test_netRunSelectTests(void)
{
    CuSuite *Suite;
    CuString *output;

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "\n#  'test_netRunSelectTests()' starting\n");

    SocketTest_Init();

    Suite = CuSuiteNew();
    CuSuiteAddSuite(Suite, getTestSuite_netSelect());

    CuSuiteRun(Suite);

    output = CuStringNew();
    CuSuiteSummary(Suite, output);
    CuSuiteDetails(Suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_netRunSelectTests() results :\n%s\n", output->buffer);

    CuSuiteFree(Suite);
    CuStringFree(output);

    SocketTest_Term();

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "\n#\n#'test_netRunSelectTests()' - Tests complete\n#\n");
}

/****************************************************************************
 *
 *  test_mpe_netReadWriteTests()
 *
 *    Runs all the network options tests from "test_mpe_net_readwrite.c"
 *
 */

NATIVEEXPORT_API void test_mpe_netReadWriteTests(void)
{
    CuSuite *Suite;
    CuString *output;

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "\n#  'test_netRunOptionsTests()' starting\n");

    SocketTest_Init();

    Suite = CuSuiteNew();
    CuSuiteAddSuite(Suite, getTestSuite_netReadWrite());

    CuSuiteRun(Suite);

    output = CuStringNew();
    CuSuiteSummary(Suite, output);
    CuSuiteDetails(Suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_mpe_netReadWriteTests() results :\n%s\n", output->buffer);

    CuSuiteFree(Suite);
    CuStringFree(output);

    SocketTest_Term();
}

/****************************************************************************
 *
 *  test_mpe_netRunSocketNegTest()
 *
 *    Runs all the network negative tests from "test_mpe_net_negative.c"
 *
 */

NATIVEEXPORT_API void test_mpe_netRunSocketNegTest(void)
{
    CuSuite *Suite;
    CuString *output;

    TRACE(MPE_LOG_TRACE3, MPE_MOD_TEST,
            "\n#  'test_mpe_netRunSocketNegTest()' starting\n");

    SocketTest_Init();

    Suite = CuSuiteNew();
    CuSuiteAddSuite(Suite, getTestSuite_netSelect());

    CuSuiteRun(Suite);

    output = CuStringNew();
    CuSuiteSummary(Suite, output);
    CuSuiteDetails(Suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\ntest_mpe_netRunSocketNegTest() results :\n%s\n", output->buffer);

    CuSuiteFree(Suite);
    CuStringFree(output);
}

/****************************************************************************
 *
 *  SocketTest_Init()
 *
 *    Initializes anything that needs to be initialized before using
 *    the MPEOS networking API.
 *
 *      Not needed when running MPE tests, since MPE takes care of
 *      initialization.
 *
 */

static void SocketTest_Init(void)
{
#ifdef TEST_MPEOS
    mpe_Bool result = FALSE;

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "\n#### 'SocketTest_Init()' called, g_socketInited == %d\n", g_socketInited);
    if (!g_socketInited) /* has socket subsystem been inited ? */
    {
        mpeos_envSet("NAMESERVERS", TEST_NET_NAMESERVER_IP);
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  Calling 'mpeos_socketInit()'\n" );
        result = mpeos_socketInit();
        g_socketInited = TRUE;
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  'mpeos_socketInit()' returned %s\n", ((result)?"TRUE":"FALSE") );
    }

#else /* !TEST_MPEOS */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "\n#### 'SocketTest_Init()' called, g_socketInited == %d\n",
            g_socketInited);

#endif /* TEST_MPEOS */

}

/****************************************************************************
 *
 *  SocketTest_Term()
 *
 *    Terminates usage of the MPEOS networking API.
 *
 *      Not needed when running MPE tests, since MPE takes care of
 *      initialization and termination.
 *
 */

static void SocketTest_Term(void)
{
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "\n#### 'SocketTest_Term()' called, g_socketInited == %d\n",
            g_socketInited);

#ifdef TEST_MPEOS
    if (g_socketInited) /* has socket subsystem been inited ? */
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  Calling 'mpeos_socketTerm()'\n" );
        mpeos_socketTerm();
        g_socketInited = FALSE;
    }
#endif /* TEST_MPEOS */
}

#if 0 /* TODO: is this function really needed? */
/**
 * This function is never called. It is here to ensure that all APIs are present in the
 * implementation and are syntactically correct. If something is missing or defined
 * incorrectly then the compiler or linker should catch it.
 *
 * WARNING: Compiler and linker errors in this function are usually an indication of a
 * problem with the implementation of the API on a specific platform. Relaxing the checks
 * here is probably not a good idea.
 */
void test_netCompileTimeChecks(void)
{
    // Basic types
    int i = 0;
    uint16_t u16 = 0;
    uint32_t u32 = 0;
    char *str = NULL;
    char **strptr = NULL;
    size_t sz = 0;
    char *charArray = NULL;
    mpe_Bool b = TRUE;
    mpe_TimeVal *timeVal = NULL;

    // Define a variable of each type
    mpe_Socket socket = 0;
    mpe_SocketSockLen sockLen = 0;
    mpe_SocketFDSet *fdSet = NULL;
    mpe_SocketHostEntry *hostEntry = NULL;
    mpe_SocketLinger *linger = NULL;
    mpe_SocketSockAddr *sockAddr = NULL;
    mpe_SocketIPv4Addr *ipv4Addr = NULL;
    mpe_SocketIPv4McastReq *ipv4McastReq = NULL;
    mpe_SocketIPv4SockAddr *ipv4SockAddr = NULL;
#ifdef MPE_SOCKET_AF_INET6
    mpe_SocketIPv6Addr *ipv6Addr = NULL;
    mpe_SocketIPv6McastReq *ipv6McastReq = NULL;
    mpe_SocketIPv6SockAddr *ipv6SockAddr = NULL;
#endif-

    // Assign each constant to all types it is valid for
    i = socket = MPE_SOCKET_FD_SETSIZE;
    //    i = MPE_HOST_NAME_MAX;
    i = MPE_SOCKET_MSG_OOB;
    i = MPE_SOCKET_MSG_PEEK;
    i = MPE_SOCKET_SHUT_RD;
    i = MPE_SOCKET_SHUT_RDWR;
    i = MPE_SOCKET_SHUT_WR;
    i = MPE_SOCKET_DATAGRAM;
    i = MPE_SOCKET_STREAM;
    i = MPE_SOCKET_INVALID_SOCKET;
    i = MPE_SOCKET_AF_INET4;
    i = MPE_SOCKET_IPPROTO_IPV4;
    i = MPE_SOCKET_IPPROTO_TCP;
    i = MPE_SOCKET_IPPROTO_UDP;
    ipv4Addr->s_addr = socketHtoNL(MPE_SOCKET_IN4ADDR_ANY);
    ipv4Addr->s_addr = socketHtoNL(MPE_SOCKET_IN4ADDR_LOOPBACK);
#ifdef MPE_SOCKET_AF_INET6
    i = MPE_SOCKET_AF_INET6;
    ipv6Addr->sin6_addr.s6_addr = socketHtoNL(MPE_IN6ADDR_ANY);
    ipv6Addr->sin6_addr.s6_addr = socketHtoNL(MPE_SOCKET_IN6ADDR_LOOPBACK_INIT);
    sz = MPE_SOCKET_INET4_ADDRSTRLEN;
    sz = MPE_SOCKET_INET6_ADDRSTRLEN;
#endif
    i = MPE_SOCKET_SOL_SOCKET;
    i = MPE_SOCKET_SO_BROADCAST;
    i = MPE_SOCKET_SO_DEBUG;
    i = MPE_SOCKET_SO_DONTROUTE;
    i = MPE_SOCKET_SO_ERROR;
    i = MPE_SOCKET_SO_KEEPALIVE;
    i = MPE_SOCKET_SO_LINGER;
    i = MPE_SOCKET_SO_OOBINLINE;
    i = MPE_SOCKET_SO_RCVBUF;
    i = MPE_SOCKET_SO_RCVLOWAT;
    i = MPE_SOCKET_SO_RCVTIMEO;
    i = MPE_SOCKET_SO_REUSEADDR;
    i = MPE_SOCKET_SO_SNDBUF;
    i = MPE_SOCKET_SO_SNDLOWAT;
    i = MPE_SOCKET_SO_SNDTIMEO;
    i = MPE_SOCKET_SO_TYPE;
    i = MPE_SOCKET_IPV4_ADD_MEMBERSHIP;
    i = MPE_SOCKET_IPV4_DROP_MEMBERSHIP;
    i = MPE_SOCKET_IPV4_MULTICAST_IF;
    i = MPE_SOCKET_IPV4_MULTICAST_LOOP;
    i = MPE_SOCKET_IPV4_MULTICAST_TTL;
#ifdef MPE_SOCKET_AF_INET6
    i = MPE_SOCKET_IPPROTO_IPV6;
    i = MPE_SOCKET_IPV6_ADD_MEMBERSHIP;
    i = MPE_SOCKET_IPV6_DROP_MEMBERSHIP;
    i = MPE_SOCKET_IPV6_MULTICAST_IF;
    i = MPE_SOCKET_IPV6_MULTICAST_HOPS;
    i = MPE_SOCKET_IPV6_MULTICAST_LOOP;
#endif
    i = MPE_SOCKET_TCP_NODELAY;
    i = MPE_SOCKET_EACCES;
    i = MPE_SOCKET_EADDRINUSE;
    i = MPE_SOCKET_EADDRNOTAVAIL;
    i = MPE_SOCKET_EAFNOSUPPORT;
    i = MPE_SOCKET_EAGAIN;
    i = MPE_SOCKET_EALREADY;
    i = MPE_SOCKET_EBADF;
    i = MPE_SOCKET_ECONNABORTED;
    i = MPE_SOCKET_ECONNREFUSED;
    i = MPE_SOCKET_ECONNRESET;
    i = MPE_SOCKET_EDESTADDRREQ;
    i = MPE_SOCKET_EDOM;
    i = MPE_SOCKET_EHOSTNOTFOUND;
    i = MPE_SOCKET_EHOSTUNREACH;
    i = MPE_SOCKET_EINTR;
    i = MPE_EINVAL;
    i = MPE_SOCKET_EIO;
    i = MPE_SOCKET_EISCONN;
    i = MPE_SOCKET_ELOOP;
    i = MPE_SOCKET_EMFILE;
    i = MPE_SOCKET_EMSGSIZE;
    i = MPE_SOCKET_ENAMETOOLONG;
    i = MPE_SOCKET_ENFILE;
    i = MPE_SOCKET_ENETDOWN;
    i = MPE_SOCKET_ENETUNREACH;
    i = MPE_SOCKET_ENOBUFS;
    i = MPE_ENODATA;
    i = MPE_ENOMEM;
    i = MPE_SOCKET_ENOPROTOOPT;
    i = MPE_SOCKET_ENORECOVERY;
    //   i = MPE_SOCKET_ENOSPC;
    i = MPE_SOCKET_ENOTCONN;
    i = MPE_SOCKET_ENOTSOCK;
    i = MPE_SOCKET_EOPNOTSUPP;
    i = MPE_SOCKET_EPIPE;
    i = MPE_SOCKET_EPROTO;
    i = MPE_SOCKET_EPROTONOSUPPORT;
    i = MPE_SOCKET_EPROTOTYPE;
    //  i = MPE_ETHREADDEATH;
    i = MPE_SOCKET_ETIMEDOUT;
    i = MPE_SOCKET_ETRYAGAIN;
    i = MPE_SOCKET_EWOULDBLOCK;

    // Access each structure member that is required to be implemented
    str = (char *)hostEntry->h_name;
    strptr = (char **)hostEntry->h_aliases;
    i = hostEntry->h_addrtype;
    i = hostEntry->h_length;
    strptr = (char **)hostEntry->h_addr_list;
    i = linger->l_onoff;
    i = linger->l_linger;
    u16 = sockAddr->sa_family;
    u32 = ipv4Addr->s_addr;
    ipv4Addr = &ipv4McastReq->imr_multiaddr;
    ipv4Addr = &ipv4McastReq->imr_interface;
    u16 = ipv4SockAddr->sin_family;
    u16 = ipv4SockAddr->sin_port;
    ipv4Addr = &ipv4SockAddr->sin_addr;
    charArray = (char *)ipv4SockAddr->sin_zero;
#ifdef MPE_SOCKET_AF_INET6
    uint8_t *u8Array = NULL;
    u8Array = ipv6Addr->s6_addr;
    ipv6Addr = &ipv6McastReq->ipv6mr_multiaddr;
    ipv6Addr = &ipv6McastReq->ipv6mr_interface;

    uint8_t u8 = 0;
    u8 = ipv6SockAddr->sin6_family;
    u16 = ipv6SockAddr->sin6_port;
    u32 = ipv6SockAddr->sin6_flowinfo;
    ipv6Addr = &ipv6SockAddr->sin6_addr;
#endif

    // Call each function
    b = mpe_socketInit();
    mpe_socketTerm();
    i = mpe_socketGetLastError();
    socket = mpe_socketAccept(socket, sockAddr, &sockLen);
    i = mpe_socketBind(socket, sockAddr, sockLen);
    i = mpe_socketClose(socket);
    i = mpe_socketConnect(socket, sockAddr, sockLen);
    socket = mpe_socketCreate(i, i, i);
    mpe_socketFDClear(socket, fdSet);
    i = mpe_socketFDIsSet(socket, fdSet);
    mpe_socketFDSet(socket, fdSet);
    mpe_socketFDZero(fdSet);
    hostEntry = mpe_socketGetHostByAddr("", sockLen, i);
    hostEntry = mpe_socketGetHostByName("");
    i = mpe_socketGetHostName("", sz);
    i = mpe_socketGetSockName(socket, sockAddr, &sockLen);
    i = mpe_socketGetOpt(socket, i, i, NULL, &sockLen);
    i = mpe_socketGetPeerName(socket, sockAddr, &sockLen);
    u32 = mpe_socketHtoNL(u32);
    u16 = mpe_socketHtoNS(u16);
    u32 = mpe_socketNtoHL(u32);
    u16 = mpe_socketNtoHS(u16);
    i = mpe_socketIoctl(socket, MPE_SOCKET_FIONBIO, i);
    i = mpe_socketIoctl(socket, MPE_SOCKET_FIONREAD, &i);
    i = mpe_socketListen(socket, i);
    i = mpe_socketAtoN("", ipv4Addr);
    str = mpe_socketNtoA(*ipv4Addr);
#ifdef MPE_SOCKET_AF_INET6
    str = mpe_socketNtoP(i, NULL, "", sz);
    i = mpe_socketPtoN(i, "", NULL);
#endif
    sz = mpe_socketRecv(socket, NULL, sz, i);
    sz = mpe_socketRecvFrom(socket, NULL, sz, i, sockAddr, &sockLen);
    i = mpe_socketSelect(i, fdSet, fdSet, fdSet, timeVal);
    sz = mpe_socketSend(socket, NULL, sz, i);
    sz = mpe_socketSendTo(socket, "", sz, i, sockAddr, sockLen);
    i = mpe_socketSetOpt(socket, i, i, NULL, sockLen);
    i = mpe_socketShutdown(socket, i);
}
#endif 

