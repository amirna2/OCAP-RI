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

/** \file test_mpe_net_connect.c
 *
 *  \brief Test functions for MPEOS network socket connection functions
 *
 *  This file contains tests for the following MPEOS functions :\n
 *
 *    -# mpeos_socketAccept()\n
 *    -# mpeos_socketBind()\n
 *    -# mpeos_socketClose()\n
 *    -# mpeos_socketConnect()\n
 *    -# mpeos_socketCreate()\n
 *    -# mpeos_socketListen()\n
 *    -# mpeos_socketShutdown()\n
 */

#include "test_net.h"

CuSuite* getTestSuite_netConnect(void);

static void vte_test_socketAccept(CuTest *tc);
static void vte_test_socketBind(CuTest *tc);
static void vte_test_socketClose(CuTest *tc);
static void vte_test_socketConnect(CuTest *tc);
static void vte_test_socketCreate(CuTest *tc);
static void vte_test_socketListen(CuTest *tc);
static void vte_test_socketShutdown(CuTest *tc);
static void vte_test_socketMultiAccept(CuTest *tc);
static void vte_test_socketNeg(CuTest *tc);

void testNet_waitForThread(void);

mpe_Cond g_cond; /* condition variable, used to coordinate test threads */
mpe_Bool gConnectLimitFlag;

/****************************************************************************
 *
 *  vte_test_socketAccept()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_socketAccept()" function.
 *
 * \api mpeos_socketAccept()
 *
 * \strategy Call the "mpeos_socketAccept()" function with various legal
 *  and illegal input values and and check for expected results.
 *
 * \assets none
 *
 */

static void vte_test_socketAccept(CuTest *tc)
{
    int32_t serverFd;
    int32_t recvFd;
    mpe_SocketIPv4SockAddr server_addr;
    int port;
    int iServerSize = sizeof(server_addr);
    mpe_Error ec;
    int32_t result;
    mpe_ThreadId tid = 0;
    ThreadData td;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_socketAccept\n");

    port = getUniquePort();

    /*
     ** Set up the values needed for the connect thread.
     */
    td.port = port;
    td.tc = tc;

    /*
     ** Initalize the global limit to allow the main thread to know that the 
     ** connect thread failed to accomplish its task. 
     */
    gConnectLimitFlag = FALSE;

    /*
     ** Create a condition to correct possible thread issues from the connect thread execution
     */
    condNew(TRUE, FALSE, &g_cond);

    serverFd = socketCreate(AF_INET, SOCK_STREAM, 0);
    CuAssert(tc, "Test Inconclusive - socketCreate() failed", serverFd > 0);

    /* 
     ** Set up the socket address stuff
     */
    (void) memset((uint8_t *) &server_addr, 0, sizeof(server_addr));
    server_addr.sin_family = AF_INET;
    server_addr.sin_addr.s_addr = htonl(INADDR_ANY);
    server_addr.sin_port = htons((uint16_t) port);
    td.fd = serverFd;

    /*  launch the test thread  */

    ec = threadCreate(nettestConnectThread, &td, MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_STACK_SIZE, &tid, "nettestConnectThread");
    CuAssert(tc, "Test Inconclusive - threadCreate() failed", ec == MPE_SUCCESS);

    /* 
     ** Finish the socket setup thing.
     */
    result = socketBind(serverFd, (mpe_SocketSockAddr*) &server_addr,
            sizeof(server_addr));
    CuAssert(tc, "Test Inconclusive - socketBind() failed", result >= 0);

    result = socketListen(serverFd, 5);
    CuAssert(tc, "Test Inconclusive - socketListen() failed", result >= 0);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    waiting for socket to connect\n");

    recvFd = socketAccept(serverFd, (mpe_SocketSockAddr *) &server_addr,
            (mpe_SocketSockLen *) &iServerSize);

    if ((0 == recvFd) && !gConnectLimitFlag)
    {
        /*
         ** If the accept fails or if the connect thread failed
         ** close the socket and fail the test.
         */
        result = socketClose(serverFd);
        CuAssert(tc, "Test Inconclusive - socketClose() failed",
                (signed) result != SOCKET_FAIL);
        serverFd = 0;
        CuAssert(tc, "Test Failed - socketAccept() failed", recvFd > 0);
        recvFd = 0;
    }
    else
    {
        /*
         ** The accept call executed successfully, now clean up the socket.
         */
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "    vte_test_socketAccept - socketAccept success\n");
        result = socketClose(serverFd);
        CuAssert(tc, "Test Inconclusive - socketClose() failed", result
                != SOCKET_FAIL);
        serverFd = 0;
        recvFd = 0;
    }

    testNet_waitForThread(); /* Wait for the test thread to exit */

    /*
     ** Fail the test if the connect limit was reached without the accept being exercised.
     */
    if (gConnectLimitFlag)
    {
        CuFail(tc,
                "Test Inconclusive - Connect limit reached without accept.\n");
    }

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_socketAccept finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_socketMultiAccept()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_socketAccept()" function.
 *
 * \api mpeos_socketAccept()
 *
 * \strategy Call the "mpeos_socketAccept()" function with various legal
 *  and illegal input values and and check for expected results.
 *
 * \assets none
 *
 */

static void vte_test_socketMultiAccept(CuTest *tc)
{
    int32_t serverFd;
    int32_t recvFd;
    mpe_SocketIPv4SockAddr server_addr;
    int port;
    int iServerSize = sizeof(server_addr);
    mpe_Error ec;
    int32_t result;
    int ii;
    mpe_ThreadId tid = 0;
    ThreadData td;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_socketMultiAccept\n");

    /*
     ** Initalize the global limit to allow the main thread to know that the 
     ** connect thread failed to accomplish its task. 
     */
    gConnectLimitFlag = FALSE;

    /*
     ** Create a condition to correct possible thread issues from 
     ** the connect thread execution
     */
    condNew(TRUE, FALSE, &g_cond);

    port = getUniquePort();
    td.port = port;
    td.tc = tc;

    serverFd = socketCreate(AF_INET, SOCK_STREAM, 0);
    CuAssert(tc, "Test Inconclusive - socketCreate() failed", serverFd > 0);

    /* 
     ** Set up the socket address stuff
     */
    (void) memset((uint8_t *) &server_addr, 0, sizeof(server_addr));
    server_addr.sin_family = AF_INET;
    server_addr.sin_addr.s_addr = htonl(INADDR_ANY);
    server_addr.sin_port = htons((uint16_t) port);

    /* 
     ** Populate the last member of the thread data struct and launch the thread.
     */
    td.fd = serverFd;
    ec = threadCreate(nettestMultiConnectThread, &td, MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_STACK_SIZE, &tid, "foo");
    CuAssert(tc, "Test Inconclusive - threadCreate() failed", ec == MPE_SUCCESS);

    /* 
     ** Finish the socket setup thing.
     */
    result = socketBind(serverFd, (mpe_SocketSockAddr*) &server_addr,
            sizeof(server_addr));
    CuAssert(tc, "Test Inconclusive - socketBind() failed", result >= 0);

    result = socketListen(serverFd, 5);
    CuAssert(tc, "Test Inconclusive - socketListen() failed", result >= 0);

    for (ii = 0; ii < MAXCONNECTATTEMPTS; ii++)
    {
        recvFd = socketAccept(serverFd, (mpe_SocketSockAddr *) &server_addr,
                (mpe_SocketSockLen *) &iServerSize);

        if ((0 == recvFd) && !gConnectLimitFlag)
        {
            result = socketClose(recvFd);
            CuAssert(tc, "Test Inconclusive - socketClose() failed", result
                    != SOCKET_FAIL);
            CuAssert(tc, "Test Fail - socketAccept() failed", recvFd > 0);
            recvFd = 0;
        }
        else
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "  vte_test_socketMultiAccept - success\n");
            result = socketClose(recvFd);
            CuAssert(tc, "Test Inconclusive - socketClose() failed", result
                    != SOCKET_FAIL);
            recvFd = 0;
        }
    }

    result = socketClose(serverFd);
    serverFd = 0;

    testNet_waitForThread(); /* Wait for the test thread to exit */

    /*
     ** Fail the test if the connect limit was reached without the accept being exercised.
     */
    if (gConnectLimitFlag)
    {
        CuFail(tc, "Connect limit reached without accept: Fail\n");
    }

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_socketMultiAccept finished.\n\n");
}

/**
 * Tests the socketBind() function
 *
 * @param tc pointer to test case structure
 */
static void vte_test_socketBind(CuTest *tc)
{

    /*
     ** socket descriptor and port to use.
     */
    int32_t serverFd;
    int port = getUniquePort();

    /*
     ** The socket address stuff
     */
    mpe_SocketIPv4SockAddr server_addr;

    /*
     ** To test return values from the tested apis
     */
    int32_t result;

    /*
     ** Set up the values needed for the connect thread.
     */

    ThreadData td;
    td.port = port;
    td.tc = tc;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_socketBind\n");

    /* 
     ** Set up the socket address stuff
     */
    (void) memset((uint8_t *) &server_addr, 0, sizeof(server_addr));
    server_addr.sin_family = AF_INET;
    server_addr.sin_addr.s_addr = htonl(INADDR_ANY);
    server_addr.sin_port = htons((uint16_t) port);

    serverFd = socketCreate(AF_INET, SOCK_STREAM, 0);
    CuAssert(tc, "Test Inconclusive - socketCreate() failed", serverFd > 0);

    /*
     ** The api to test
     */
    result = socketBind(serverFd, (mpe_SocketSockAddr*) &server_addr,
            sizeof(server_addr));
    CuAssert(tc, "Test Failed - socketBind()", result != SOCKET_FAIL);

    result = socketClose(serverFd);
    CuAssert(tc, "Test Inconclusive - socketClose() failed", result
            != SOCKET_FAIL);
    serverFd = 0;

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_socketBind finished.\n\n");
}

/**
 * Tests the socketClose() function
 *
 * @param tc pointer to test case structure
 */
static void vte_test_socketClose(CuTest *tc)
{
    int32_t serverFd = 0;
    int32_t result;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_socketClose\n");

    /*
     ** Allocate a socket descriptor so it can be closed
     */
    serverFd = socketCreate(AF_INET, SOCK_STREAM, 0);
    CuAssert(tc, "Test Inconclusive - socketCreate() failed", serverFd > 0);

    result = socketClose(serverFd);
    CuAssert(tc, "Test Failed - socketClose() failed", result != SOCKET_FAIL);
    serverFd = 0;
}

/**
 * Tests the socketConnect() function:  This is tested by creating 
 * a client server connection.  There is some redundancy with this and
 * other tests in this suite.  The goal was to provide at least one separate
 * test for each api so that the test name could be searched for a matching 
 * test if a related problem is found.
 *
 * @param tc pointer to test case structure
 */
static void vte_test_socketConnect(CuTest *tc)
{
    /*
     ** socket descriptors.
     */
    int32_t serverFd;
    int32_t recvFd;

    /*
     ** socket address stuff
     */
    mpe_SocketIPv4SockAddr server_addr;
    int port = getUniquePort();
    int iServerSize = sizeof(server_addr);

    /*
     ** To test return values from the tested apis
     */
    mpe_Error ec;
    int32_t result;

    /*
     ** Set up the values needed for the connect thread.
     */
    mpe_ThreadId tid = 0;
    ThreadData td;
    td.port = port;
    td.tc = tc;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_socketConnect\n");

    /*
     ** Initalize the global limit to allow the main thread to know that the 
     ** connect thread failed to accomplish its task. 
     */
    gConnectLimitFlag = FALSE;

    /*
     ** Create a condition to correct possible thread issues from the connect thread execution
     */
    condNew(TRUE, FALSE, &g_cond);

    serverFd = socketCreate(AF_INET, SOCK_STREAM, 0);
    CuAssert(tc, "Test Inconclusive - socketCreate() failed", serverFd > 0);

    /* 
     ** Set up the socket address stuff
     */
    (void) memset((uint8_t *) &server_addr, 0, sizeof(server_addr));
    server_addr.sin_family = AF_INET;
    server_addr.sin_addr.s_addr = htonl(INADDR_ANY);
    server_addr.sin_port = htons((uint16_t) port);

    /* 
     ** Populate the last member of the thread data struct and launch the thread.
     */
    td.fd = serverFd;
    ec = threadCreate(nettestConnectThread, &td, MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_STACK_SIZE, &tid, "foo");
    CuAssert(tc, "Test Inconclusive - threadCreate() failed", ec == MPE_SUCCESS);

    /* 
     ** Finish the socket setup thing.
     */
    result = socketBind(serverFd, (mpe_SocketSockAddr*) &server_addr,
            sizeof(server_addr));
    CuAssert(tc, "Test Inconclusive - socketBind() failed", result >= 0);

    result = socketListen(serverFd, 5);
    CuAssert(tc, "Test Inconclusive - socketListen() failed", result >= 0);

    /*TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Waiting on socket to connect...\n");*/

    recvFd = socketAccept(serverFd, (mpe_SocketSockAddr *) &server_addr,
            (mpe_SocketSockLen *) &iServerSize);

    if ((0 == recvFd) && !gConnectLimitFlag)
    {
        /*
         ** If the accept fails or if the connect thread failed
         ** close the socket and fail the test.
         */
        result = socketClose(serverFd);
        CuAssert(tc, "Test Inconclusive - socketClose() failed", result
                != SOCKET_FAIL);
        serverFd = 0;
        CuAssert(tc, "Test Inconclusive - socketAccept() failed", recvFd > 0);
        recvFd = 0;
        CuFail(tc, "Test Failed - socketConnect() Failed\n");
    }
    else
    {
        /*
         ** The accept call executed successfully, now clean up the socket.
         */
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "    vte_test_socketConnect - success\n");
        result = socketClose(serverFd);
        CuAssert(tc, "Test Inconclusive - socketClose() failed", result
                != SOCKET_FAIL);
        serverFd = 0;
        recvFd = 0;
    }

    /* Wait for the connect thread to exit  */

    testNet_waitForThread();

    /*
     ** Fail the test if the connect limit was reached without the accept being exercised.
     */
    if (gConnectLimitFlag)
    {
        CuFail(tc,
                "Test Inconclusive - Connect limit reached without accept.\n");
    }
}

/**
 * Tests the socketCreate() function
 *
 * @param tc pointer to test case structure
 */
static void vte_test_socketCreate(CuTest *tc)
{
    int32_t serverFd;
    int32_t result;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_socketCreate\n");

    /*
     ** The api to test
     */
    serverFd = socketCreate(AF_INET, SOCK_STREAM, 0);
    CuAssert(tc, "Test Failed - socketCreate() failed", serverFd > 0);

    /*
     ** Do the cleanup thing
     */
    result = socketClose(serverFd);
    CuAssert(tc, "Test Inconclusive - socketClose() failed", result
            != SOCKET_FAIL);
    serverFd = 0;

}

/**
 * Tests the socketListen() function
 *
 * @param tc pointer to test case structure
 */
static void vte_test_socketListen(CuTest *tc)
{
    /*
     ** socket descriptors.
     */
    int32_t serverFd;

    /*
     ** socket address stuff
     */
    mpe_SocketIPv4SockAddr server_addr;
    int port = getUniquePort();

    /*
     ** To test return values from the tested apis
     */
    int32_t result;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_socketListen\n");

    serverFd = socketCreate(AF_INET, SOCK_STREAM, 0);
    CuAssert(tc, "Test Inconclusive - socketCreate() failed", serverFd > 0);

    /*
     ** Now set up the socket addr for the Bind call.
     */
    (void) memset((uint8_t *) &server_addr, 0, sizeof(server_addr));
    server_addr.sin_family = AF_INET;
    server_addr.sin_addr.s_addr = htonl(INADDR_ANY);
    server_addr.sin_port = htons((uint16_t) port);

    result = socketBind(serverFd, (mpe_SocketSockAddr*) &server_addr,
            sizeof(server_addr));
    CuAssert(tc, "Test Inconclusive - socketBind() failed", result >= 0);

    /*
     ** Signature test for the listen api
     */
    result = socketListen(serverFd, 5);
    CuAssert(tc, "Test Fail - socketListen() failed", result >= 0);

    result = socketClose(serverFd);
    CuAssert(tc, "Test Inconclusive - socketClose() failed", result
            != SOCKET_FAIL);
    serverFd = 0;
}

/**
 * Tests the socketShutdown() function
 *
 * @param tc pointer to test case structure
 */
static void vte_test_socketShutdown(CuTest *tc)
{
    int32_t serverFd;
    int32_t recvFd;
    mpe_SocketIPv4SockAddr server_addr;
    int port;
    int iServerSize = sizeof(server_addr);
    mpe_Error ec;
    int32_t result;
    mpe_ThreadId tid = 0;
    ThreadData td;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_socketShutdown\n");

    port = getUniquePort();

    /*
     ** Initalize the global limit to allow the main thread to know that the 
     ** connect thread failed to accomplish its task. 
     */
    gConnectLimitFlag = FALSE;

    /*
     ** Create a condition to correct possible thread issues from the connect thread execution
     */
    condNew(TRUE, FALSE, &g_cond);

    serverFd = socketCreate(AF_INET, SOCK_STREAM, 0);
    CuAssert(tc, "Test Inconclusive - socketCreate() failed", serverFd > 0);

    td.port = port;
    td.tc = tc;
    td.fd = serverFd;

    (void) memset((uint8_t *) &server_addr, 0, sizeof(server_addr));
    server_addr.sin_family = AF_INET;
    server_addr.sin_addr.s_addr = htonl(INADDR_ANY);
    server_addr.sin_port = htons((uint16_t) port);

    ec = threadCreate(nettestConnectThread, &td, MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_STACK_SIZE, &tid, "foo");
    CuAssert(tc, "Test Inconclusive - threadCreate() failed", ec == MPE_SUCCESS);

    result = socketBind(serverFd, (mpe_SocketSockAddr*) &server_addr,
            sizeof(server_addr));
    CuAssert(tc, "Test Inconclusive - socketBind() failed", result >= 0);

    result = socketListen(serverFd, 5);
    CuAssert(tc, "Test Inconclusive - socketListen() failed", result >= 0);

    recvFd = socketAccept(serverFd, (mpe_SocketSockAddr *) &server_addr,
            (mpe_SocketSockLen *) &iServerSize);

    if ((SOCKET_FAIL == recvFd) && !gConnectLimitFlag)
    {
        /*
         ** The accept failed, so clean up the socket
         */
        result = socketClose(serverFd);
        CuAssert(tc, "Test Inconclusive - socketClose() failed", result
                != SOCKET_FAIL);
        serverFd = 0;
        CuFail(tc, "Accept Failed\n");
    }
    else
    {
        // result = socketShutdown( recvFd, MPE_SOCKET_SHUT_RDWR);

        if (SOCKET_FAIL == result)
        {
            /* 
             ** If the shutdown failed, first clean up the sockets and then fail the test
             */
            result = socketClose(serverFd);
            CuAssert(tc, "Test Inconclusive - socketClose() failed", result
                    != SOCKET_FAIL);
            result = socketClose(recvFd);
            CuAssert(tc, "Test Inconclusive - socketClose() failed", result
                    != SOCKET_FAIL);

            serverFd = 0;
            recvFd = 0;

            CuFail(tc, "Test Failed - socketShutdown() failed");
        }
        else
        {
            /*
             ** Otherwise, the call was successful so just clean up the sockets
             */
            result = socketClose(serverFd);
            CuAssert(tc, "Test Inconclusive - socketClose() failed", result
                    != SOCKET_FAIL);

            result = socketClose(recvFd);
            CuAssert(tc, "Test Inconclusive - socketClose() failed", result
                    != SOCKET_FAIL);

            serverFd = 0;
            recvFd = 0;
        }
    }

    testNet_waitForThread();

    /*
     ** Fail the test if the connect limit was reached without the accept being exercised.
     */
    if (gConnectLimitFlag)
    {
        CuFail(tc,
                "Test Inconclusive - Connect limit reached without accept.\n");
    }
    threadSleep(100, 0); /* give the test thread time to really finish   */

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_socketShutdown finished\n\n");
}

static void vte_test_socketNeg(CuTest *tc)
{
    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
            "Entered vte_test_socketNeg . . .  which is not currently implemented\n");
    /*    CuFail(tc, "Unimplemented Test - 'mpetest_socketNeg()'\n"); */
}

/****************************************************************************
 *
 *  'testNet_waitForThread() - wait for a test thread to set a condition
 *
 ****/

void testNet_waitForThread(void)
{
    mpe_Error ec;

    ec = condWaitFor(g_cond, 1000);
    if (MPE_SUCCESS != ec)
    {
        if (MPE_EBUSY != ec)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "    'condWaitFor()' returned MPE_EBUSY\n");
        }
        else if (MPE_EINVAL != ec)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "    'condWaitFor()' returned MPE_EBUSY\n");
        }
        else
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "    'condWaitFor()' returned unknowm error - %d\n",
                    (int) ec);
        }
    }
    condDelete(g_cond);
    threadSleep(100, 0); /* give the test thread time to really finish   */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "        testNet_waitForThread finished\n");
}

/*
 ** Function: getUniquePort()
 ** Description:
 ** Function to return a unique port for use in the tests.
 ** Because different systems release the port resource at different times
 ** to allow for proper socket port and address cleanup before reuse, the same port
 ** can not always be used in the following scenarios especially if the are run
 ** syncronously.
 */
int getUniquePort(void)
{
    static mpe_Bool seeded = FALSE;
    if (!seeded)
    {
        srand((unsigned) clock());
        seeded = TRUE;
    }
    return ((rand() % 1000) + 8000);
}

/*
 ** Thread function used to connect to all listening tests.  
 ** This thread try to connect until the connection is successful.
 ** After the connection, the socket is closed and cleaned up.
 **
 **@param - threadData - containing the Cutest*tc and the port number.
 */
void nettestConnectThread(void * threadData)
{
    ThreadData *td = (ThreadData *) threadData;
    int32_t clientFd;
    int port = td->port;
    mpe_SocketIPv4SockAddr client_addr;
    const char * serverIP = "127.0.0.1";
    int count = 0;
    int32_t result;

    mpe_Bool connected = false;

    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "    >>>> entered 'nettestConnectThread()', threadData == %p\n", td);

    /* 
     ** First create the socket
     */
    clientFd = socketCreate(AF_INET, SOCK_STREAM, 0);
    CuAssert(td->tc, "socketCreate() failed", clientFd > 0);

    /*
     ** Setup the mpe_SocketSockAddr struct
     */
    (void) memset((uint8_t *) &client_addr, 0, sizeof(client_addr));
    client_addr.sin_family = AF_INET;
    result = socketAtoN(serverIP, &(client_addr.sin_addr));
    client_addr.sin_port = htons((uint16_t) port);

    //    threadSleep(1000,0);  /*  does this really need to wait a second ???  */
    threadSleep(100, 0);

    /*
     ** Attempted to connect to the socket.  Fail after MAXCONNECTATTEMPTS
     ** attempts to prevent the hanging of the test or this
     ** thread.
     */
    do
    {
        result = socketConnect(clientFd, (mpe_SocketSockAddr*) &client_addr,
                sizeof(client_addr));
        if (result == SOCKET_FAIL)
        {
            TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
                    "    'socketConnect()' attempt %d failed, errno == %d\n",
                    count + 1, socketGetLastError());
            threadSleep(1000, 0);
            count++;
        }
        else
        {
            TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
                    "    'nettestConnectThread()' success.\n");
            connected = true;
        }
    } while (!connected && (MAXCONNECTATTEMPTS > count));

    if (MAXCONNECTATTEMPTS == count)
    {
        gConnectLimitFlag = TRUE;
        /*
         ** Clean up the socket. First close the other threads serverFd to make the Accept fail.
         */
        result = socketClose(td->fd);
        td->fd = 0;
    }

    /*
     ** Clean up the socket.  Now clean up the local socket stuff.
     */
    result = socketClose(clientFd);
    clientFd = 0;

    /*
     ** Set the global condition so the main thread knows
     ** that it can exit.
     */
    condSet(g_cond);

    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "    <<<< 'nettestConnectThread()' exiting\n");

}

/****************************************************************************
 *
 *  nettestMultiConnectThread() - test 'threadDestroy(0)
 *
 *
 * Thread function used to connect to the multi accept test.  
 * This thread try to connect until the connection is successful.
 * It will then connect 9 more times or until a single connect 
 * fails 10 times.
 *
 * After the connection, the socket is closed and cleaned up.
 *
 */

void nettestMultiConnectThread(void * threadData)
{
    ThreadData *td = (ThreadData *) threadData;
    int32_t clientFd;
    int port = td->port;
    mpe_SocketIPv4SockAddr client_addr;
    const char * serverIP = "127.0.0.1";
    int32_t result;
    int ii;
    int count = 0;
    mpe_Bool connected = false;

    TRACE(
            MPE_LOG_TRACE5,
            MPE_MOD_TEST,
            "    >>>> entered 'nettestMultiConnectThread()', threadData == %p\n",
            td);

    for (ii = 0; ii < MAXCONNECTATTEMPTS; ii++)
    {
        /*  create the socket   */

        clientFd = socketCreate(AF_INET, SOCK_STREAM, 0);
        CuAssert(td->tc, "socketCreate() failed", clientFd > 0);

        /*
         ** Setup the mpe_SocketSockAddr struct
         */
        (void) memset((uint8_t *) &client_addr, 0, sizeof(client_addr));
        client_addr.sin_family = AF_INET;
        result = socketAtoN(serverIP, &(client_addr.sin_addr));
        client_addr.sin_port = htons((uint16_t) port);

        threadSleep(1000, 0);
        do
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "      MultiConnectThread: Trying to connect\n");
            result = socketConnect(clientFd,
                    (mpe_SocketSockAddr*) &client_addr, sizeof(client_addr));
            if (result == SOCKET_FAIL)
            {
                TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
                        "      Connect Failed: errno - %d\n",
                        socketGetLastError());
                threadSleep(1000, 0);
                count++;
            }
            else
            {
                TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "      Connect success!\n");
                connected = true;
            }

        } while (!connected && (MAXCONNECTATTEMPTS > count));

        result = socketClose(clientFd);
        clientFd = 0;

        if (MAXCONNECTATTEMPTS == count)
        {
            gConnectLimitFlag = TRUE;
            result = socketClose(td->fd);
            td->fd = 0;
            break;
        }
    }
    condSet(g_cond);

    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "    <<<< 'nettestMultiConnectThread()' exiting\n");
}

/**
 * Create and return the test suite for the socket APIs tested in this suite.
 * @return a pointer to the new test suite.
 */
CuSuite* getTestSuite_netConnect(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, vte_test_socketAccept);
    SUITE_ADD_TEST(suite, vte_test_socketMultiAccept);
    SUITE_ADD_TEST(suite, vte_test_socketBind);
    SUITE_ADD_TEST(suite, vte_test_socketClose);
    SUITE_ADD_TEST(suite, vte_test_socketConnect);
    SUITE_ADD_TEST(suite, vte_test_socketCreate);
    SUITE_ADD_TEST(suite, vte_test_socketListen);
    SUITE_ADD_TEST(suite, vte_test_socketShutdown);
    SUITE_ADD_TEST(suite, vte_test_socketNeg);
    return suite;
}
