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

/** \file test_sys_mem.c
 *
 *  \brief Test functions for MPEOS memory functions
 *
 *  This file contains tests for the following MPEOS functions :\n
 *
 *    -# mpeos_socketRecv()\n
 *    -# mpeos_socketRecvFrom()\n
 *    -# mpeos_socketSend()\n
 *    -# mpeos_socketSendTo()\n
 */

#include "test_net.h"

/*
 * Test functions defined in this file
 */

static void vte_test_socketRecv(CuTest*);
static void vte_test_socketRecvFrom(CuTest*);
static void vte_test_socketSend(CuTest*);
static void vte_test_socketSendTo(CuTest*);

/*
 * Internal functions, not called directly by the test runner
 */

static mpe_Socket serverSetup(CuTest*, int);
static int32_t clientSetup(int, CuTest*);
static void testNet_SendThread2(void*);

/*
 * Function which adds test functions in this file to a CuTest suite
 */

CuSuite* getTestSuite_netReadWrite(void);

/****************************************************************************
 *
 *  vte_test_socketRecv()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_socketRecv" function 
 *
 * \api vte_test_socketRecv()
 *
 * \strategy Launch a test server thread and attempt to receive data from it.
 *
 * \assets none
 *
 */

static void vte_test_socketRecv(CuTest *tc)
{
    mpe_ThreadId tid = 0;
    SendThreadData std;
    mpe_Socket serverFd;
    mpe_Socket recvFd;
    const char msg[] = "test_socketRecv() test message";
    int strSize = strlen(msg) + 1;
    char* buff;
    mpe_Error ec;
    int32_t result;
    int port;
    int buffIndex;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_socketRecv\n");

    port = getUniquePort();
    memAllocP(MPE_MEM_TEST, strSize, (void **) &buff);
    memset((void*) buff, 0, strSize);

    /*
     ** Initalize the global limit to allow the main thread to know that the 
     ** connect thread failed to accomplish its task. 
     */
    gConnectLimitFlag = FALSE;

    /*
     ** Create a condition for the connect thread to signal the main thread
     */
    condNew(TRUE, FALSE, &g_cond);

    serverFd = serverSetup(tc, port);

    /*
     ** Populate the structure to send to the thread.  Includes the compare
     ** msg, the port to communicate over, thread data, and the test case ptr.
     */

    popSendStructure(&std, tc, port, msg, &serverFd);
    ec = threadCreate(testNet_SendThread, &std, MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_STACK_SIZE, &tid, "testNet_SendThread");

    CuAssert(tc, "Test Inconclusive - threadCreate() failed", ec == MPE_SUCCESS);

    recvFd = socketAccept(serverFd, NULL, NULL);

    if ((0 == recvFd) && !gConnectLimitFlag)
    {
        /*
         ** If the accept fails or if the connect thread failed
         ** close the socket and fail the test.
         */
        TRACE(
                MPE_LOG_DEBUG,
                MPE_MOD_TEST,
                "    socketAccept() failed, recvFd == %d, gConnectLimitFlag == %d\n",
                (int) recvFd, (int) gConnectLimitFlag);
        result = socketClose(serverFd);
        CuAssert(tc, "Test Inconclusive - socketClose() failed",
                (signed) result != SOCKET_FAIL);
        CuAssert(tc, "Test Inconclusive - socketAccept() failed", recvFd > 0);
    }
    else
    {
        /*
         ** The accept call executed successfully, now call socketRecv().
         */
        if (recvFd)
        {
            buffIndex = 0;
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    calling socketRecv()\n");
            while ((result = socketRecv(recvFd, buff + buffIndex, strSize, 0))
                    > 0)
            {
                buffIndex += result;
                TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                        "      socketRecv() returned %d\n", (int) result);
            }

            CuAssert(tc, "Test Failed - socketRecv() failed...", result
                    != SOCKET_FAIL);
            CuAssert(tc, "Test Failed - Messages are not equal...", strcmp(msg,
                    buff) == 0);
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  received message : '%s'\n",
                    buff);
        }

        result = socketClose(serverFd);
        CuAssert(tc, "Test Inconclusive - socketClose() failed", result
                != SOCKET_FAIL);
    }

    testNet_waitForThread(); /* Wait for the test thread to exit */

    memFreeP(MPE_MEM_TEST, buff);
    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_socketRecv finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_socketRecvFrom()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_socketRecvFrom" function 
 *
 * \api vte_test_socketRecvFrom()
 *
 * \strategy Launch a test server thread and attempt to receive data from it.
 *
 * \assets none
 *
 */

static void vte_test_socketRecvFrom(CuTest *tc)
{
    mpe_ThreadId tid = 0;
    SendThreadData std;
    mpe_Error ec;
    mpe_Socket serverFd;
    mpe_Socket recvFd;
    mpe_SocketSockAddr recvSockAddr;
    mpe_SocketSockLen recvSockAddrLength;
    const char msg[] = "test_socketRecvFrom() test message";
    int strSize = strlen(msg) + 1;
    char* buff;
    int32_t result;
    int port;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_socketRecvFrom\n");

    port = getUniquePort();

    /*
     ** Initalize the global limit to allow the main thread to know that the 
     ** connect thread failed to accomplish its task. 
     */
    gConnectLimitFlag = FALSE;

    memAllocP(MPE_MEM_TEST, strSize, (void **) &buff);
    memset((void*) buff, 0, strSize);

    /*
     ** Create a condition for the connect thread to signal the main thread
     */
    condNew(TRUE, FALSE, &g_cond);

    serverFd = serverSetup(tc, port);

    /*
     ** Populate the structure to send to the thread.  Includes the compare
     ** msg, the port to communicate over, thread data, and the test case ptr.
     */
    popSendStructure(&std, tc, port, msg, &serverFd);
    ec = threadCreate(testNet_SendThread, &std, MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_STACK_SIZE, &tid, "testNet_SendThread");
    CuAssert(tc, "Test Inconclusive - threadCreate() failed", ec == MPE_SUCCESS);

    recvFd = socketAccept(serverFd, NULL, NULL);

    if ((0 == recvFd) && !gConnectLimitFlag)
    {
        /*
         ** If the accept fails or if the connect thread failed
         ** close the socket and fail the test.
         */
        result = socketClose(serverFd);
        CuAssert(tc, "Test Inconclusive - socketClose() failed",
                (signed) result != SOCKET_FAIL);
        CuAssert(tc, "Test Inconclusive - socketAccept() failed", recvFd > 0);
    }
    else
    {
        /*
         ** The accept call executed successfully, now call recvSock.
         */
        if (recvFd)
        {
            int buffIndex = 0;

            memset((void *) &recvSockAddr, 0, sizeof(mpe_SocketSockAddr));

            while ((result = socketRecvFrom(recvFd, buff + buffIndex, strSize,
                    0, &recvSockAddr, &recvSockAddrLength)) > 0)
            {
                buffIndex += result;
            }

            CuAssert(tc, "Test Failed - socketRecvFrom() failed...", result
                    != SOCKET_FAIL);
            CuAssert(tc, "Test Failed - Messages are not equal...", strcmp(msg,
                    buff) == 0);
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "     test_socketRecvFrom() - msg: %s\n", buff);
        }

        result = socketClose(serverFd);
        CuAssert(tc, "Test Inconclusive - socketClose() failed", result
                != SOCKET_FAIL);
    }

    testNet_waitForThread(); /* Wait for the test thread to exit */

    memFreeP(MPE_MEM_TEST, buff);
    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_socketRecvFrom finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_socketSend()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_socketSend" function 
 *
 * \api vte_test_socketSend()
 *
 * \strategy Launch a test server thread and attempt to receive data from it.
 *
 * \assets none
 *
 */

static void vte_test_socketSend(CuTest *tc)
{
    mpe_ThreadId tid = 0;
    SendThreadData std;
    mpe_Error ec;
    mpe_Socket serverFd;
    mpe_Socket recvFd;
    const char msg[] = "test_socketSend() test message";
    int strSize = strlen(msg) + 1;
    char* buff;
    int32_t result;
    int port;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_socketSend\n");

    port = getUniquePort();

    /*
     ** Initalize the global limit to allow the main thread to know that the 
     ** connect thread failed to accomplish its task. 
     */
    gConnectLimitFlag = FALSE;

    memAllocP(MPE_MEM_TEST, strSize, (void **) &buff);
    memset((void*) buff, 0, strSize);

    /*
     ** Create a condition for the connect thread to signal the main thread
     */
    condNew(TRUE, FALSE, &g_cond);

    serverFd = serverSetup(tc, port);

    /*
     ** Populate the structure to send to the thread.  Includes the compare
     ** msg, the port to communicate over, thread data, and the test case ptr.
     */
    popSendStructure(&std, tc, port, msg, &serverFd);
    ec = threadCreate(testNet_SendThread, &std, MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_STACK_SIZE, &tid, "testNet_SendThread");
    CuAssert(tc, "Test Inconclusive - threadCreate() failed", ec == MPE_SUCCESS);

    recvFd = socketAccept(serverFd, NULL, NULL);

    if ((0 == recvFd) && !gConnectLimitFlag)
    {
        /*
         ** If the accept fails or if the connect thread failed
         ** close the socket and fail the test.
         */
        result = socketClose(serverFd);
        CuAssert(tc, "Test Inconclusive - socketClose() failed",
                (signed) result != SOCKET_FAIL);
        CuAssert(tc, "Test Inconclusive - socketAccept() failed", recvFd > 0);
    }
    else
    {
        /*
         ** The accept call executed successfully, now call recvSock.
         */
        if (recvFd)
        {
            int buffIndex = 0;

            while ((result = socketRecv(recvFd, buff + buffIndex, strSize
                    - buffIndex, 0)) > 0)
            {
                buffIndex += result;
            }

            CuAssert(tc, "Test Failed - socketRecv() failed...", result
                    != SOCKET_FAIL);
            CuAssert(tc, "Test Failed - Messages are not equal...", strcmp(msg,
                    buff) == 0);
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "    'test_socketSend() -  msg: %s\n", buff);
        }

        result = socketClose(serverFd);
        CuAssert(tc, "Test Inconclusive - socketClose() failed", result
                != SOCKET_FAIL);
    }

    testNet_waitForThread(); /* Wait for the test thread to exit */

    memFreeP(MPE_MEM_TEST, buff);
    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_socketSend finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_socketSendTo()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_socketSendT" function 
 *
 * \api vte_test_socketRecv()
 *
 * \strategy Launch a test server thread and attempt to receive data from it.
 *
 * \assets none
 *
 */

static void vte_test_socketSendTo(CuTest *tc)
{
    mpe_ThreadId tid = 0;
    SendThreadData std;
    mpe_Error ec;
    mpe_Socket serverFd;
    mpe_Socket recvFd;
    const char msg[] = "test_socketSendTo() test message";
    int strSize = strlen(msg) + 1;
    char* buff;
    int32_t result;
    int port;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_socketSendTo\n");

    port = getUniquePort();

    /*
     ** Initalize the global limit to allow the main thread to know that the 
     ** connect thread failed to accomplish its task. 
     */
    gConnectLimitFlag = FALSE;

    memAllocP(MPE_MEM_TEST, strSize, (void **) &buff);
    memset((void*) buff, 0, strSize);

    /*
     ** Create a condition for the connect thread to signal the main thread
     */
    condNew(TRUE, FALSE, &g_cond);

    serverFd = serverSetup(tc, port);

    /*
     ** Populate the structure to send to the thread.  Includes the compare
     ** msg, the port to communicate over, thread data, and the test case ptr.
     */
    popSendStructure(&std, tc, port, msg, &serverFd);

    ec = threadCreate(testNet_SendThread2, &std, MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_STACK_SIZE, &tid, "testNet_SendThread2");
    CuAssert(tc, "Test Inconclusive - threadCreate() failed", ec == MPE_SUCCESS);

    recvFd = socketAccept(serverFd, NULL, NULL);

    if ((0 == recvFd) && !gConnectLimitFlag)
    {
        /*
         ** If the accept fails or if the connect thread failed
         ** close the socket and fail the test.
         */
        result = socketClose(serverFd);
        CuAssert(tc, "Test Inconclusive - socketClose() failed",
                (signed) result != SOCKET_FAIL);
        CuAssert(tc, "Test Inconclusive - socketAccept() failed", recvFd > 0);
    }
    else
    {
        /*
         ** The accept call executed successfully, now call recvSock.
         */
        if (recvFd)
        {
            int buffIndex = 0;

            while ((result = socketRecv(recvFd, buff + buffIndex, strSize, 0))
                    > 0)
            {
                buffIndex += result;
            }

            CuAssert(tc, "Test Inconclusive - socketSendTo() failed...", result
                    != SOCKET_FAIL);
            CuAssert(tc, "Test Failed - Messages are not equal...", strcmp(msg,
                    buff) == 0);
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    received message : '%s'\n",
                    buff);
        }

        result = socketClose(serverFd);
        CuAssert(tc, "Test Inconclusive - socketClose() failed", result
                != SOCKET_FAIL);
    }

    testNet_waitForThread(); /* Wait for the test thread to exit */

    memFreeP(MPE_MEM_TEST, buff);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_socketSendTo finished.\n\n");
}

/****************************************************************************
 ****************************************************************************

 Test "helper" functions which get launched in a new thread.


 - testNet_SendThread()
 - 

 */

/****************************************************************************
 *
 *  testNet_SendThread() - Send a test message on a socket.
 *
 *    This function gets launched in a new thread and sends a test message
 *    on a previously setup socket.
 *
 */

void testNet_SendThread(void * sendThreadData)
{
    int32_t result;

    SendThreadData * std = (SendThreadData *) sendThreadData;
    mpe_Socket sock;

    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "     >>>>> entered testNet_SendThread - Sending msg: '%s'\n",
            std->message);

    sock = clientSetup(std->port, std->tc);

    if (sock)
    {
        result = socketSend(sock, std->message, strlen(std->message) + 1, 0);
        CuAssert(std->tc, "socketSend() failed in testNet_SendThread()", result
                != -1);

        result = socketClose(sock);
        CuAssert(
                std->tc,
                "Test Inconclusive - socketClose() failed in testNet_SendThread()",
                result != SOCKET_FAIL);
    }
    condSet( g_cond); /* Signal the main thread that we're done.  */
    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "     <<<<< testNet_SendThread() exiting.\n");
}

/****************************************************************************
 *
 *  testNet_SendThread2() - Send a test message on a socket.
 *
 *    This function gets launched in a new thread and sends a test message
 *    on a previously setup socket.
 *
 */

static void testNet_SendThread2(void * sendThreadData)
{
    int32_t result;
    ClientSock c;
    SendThreadData *std = (SendThreadData *) sendThreadData;
    mpe_Bool b_result;

    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "     >>>>> entered testNet_SendThread2 with message : '%s'\n",
            std->message);

    b_result = clientSetup1(std->port, std->tc, &c, std->sfd);

    if (b_result)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "      testNet_SendThread2 - sending message : '%s'\n",
                std->message);
        result = socketSendTo(c.sendSock, std->message, strlen(std->message)
                + 1, 0, (mpe_SocketSockAddr*) &(c.client_addr),
                sizeof(c.client_addr));
        CuAssert(std->tc, "Test Fail - socketSendTo() failed...", result
                != SOCKET_FAIL);

        result = socketClose(c.sendSock);
        CuAssert(std->tc, "Test Inconclusive - client socketClose() failed...",
                result != SOCKET_FAIL);
    }

    condSet( g_cond); /* Signal the main thread that we're done.  */
    TRACE(MPE_LOG_TRACE5, MPE_MOD_TEST,
            "     <<<<< testNet_SendThread2() exiting.\n");
}

/****************************************************************************
 *
 *   Internal utility functions.
 *
 ***************************************************************************/

/****************************************************************************
 *
 *  serverSetup() - Setup a server socket for read/write tests.
 *
 */

static mpe_Socket serverSetup(CuTest *tc, int port)
{
    int32_t result;
    mpe_SocketIPv4SockAddr server_addr;
    mpe_Socket serverFd;

    TRACE(MPE_LOG_TRACE7, MPE_MOD_TEST, "        serverSetup() - port = %d\n",
            port);

    serverFd = socketCreate(AF_INET, SOCK_STREAM, 0);
    CuAssert(tc, "Test Inconclusive - server socketCreate() failed", serverFd
            > 0);

    memset((uint8_t *) &server_addr, 0, sizeof(server_addr));
    server_addr.sin_family = AF_INET;
    server_addr.sin_addr.s_addr = htonl(INADDR_ANY);
    server_addr.sin_port = htons((uint16_t) port);

    result = socketBind(serverFd, (mpe_SocketSockAddr*) &server_addr,
            sizeof(server_addr));
    CuAssert(tc, "Test Inconclusive - server socketBind() failed", result >= 0);

    result = socketListen(serverFd, 5);
    CuAssert(tc, "Test Inconclusive - server socketListen() failed", result
            >= 0);

    return serverFd;
}

void popSendStructure(SendThreadData* std, CuTest* tc, int port,
        const char* message, mpe_Socket* fd)
{
    std->tc = tc;
    std->port = port;
    std->message = message;
    std->sfd = fd;
}

/****************************************************************************
 *
 *  clientSetup() - Setup a client socket for read/write tests.
 *
 */

static int32_t clientSetup(int p, CuTest * t)
{
    int32_t clientFd;
    int port = p;
    mpe_SocketIPv4SockAddr client_addr;
    const char * serverIP = "127.0.0.1";
    int32_t result;
    mpe_Bool connected = false;
    int count = 0;

    TRACE(MPE_LOG_TRACE7, MPE_MOD_TEST, "        clientSetup() - port = %d\n",
            port);

    /*
     **  Create the socket
     */
    clientFd = socketCreate(AF_INET, SOCK_STREAM, 0);
    CuAssert(t, "Test Inconclusive - socketCreate() failed", clientFd > 0);

    /*
     ** Setup the mpe_SocketSockAddr struct
     */
    memset((uint8_t *) &client_addr, 0, sizeof(client_addr));
    client_addr.sin_family = AF_INET;
    result = socketAtoN(serverIP, &(client_addr.sin_addr));
    client_addr.sin_port = htons((uint16_t) port);

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
            count++;
            TRACE(
                    MPE_LOG_DEBUG,
                    MPE_MOD_TEST,
                    "        clientSetup() - connect attempt %2d failed - errno: %d\n",
                    count, socketGetLastError());
            threadSleep(1000, 0);
        }
        else
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "        clientSetup() - connected\n");
            connected = true;
            return clientFd;

        }
    } while (!connected && (MAXCONNECTATTEMPTS > count));

    if (MAXCONNECTATTEMPTS == count)
    {
        gConnectLimitFlag = TRUE;
        return 0;
    }
    else
    {
        return 0;
    }
}

/****************************************************************************
 *
 *  clientSetup1() - Setup a client socket for read/write tests.
 *
 */

mpe_Bool clientSetup1(int p, CuTest *t, ClientSock *c, mpe_Socket *s)
{
    const char * serverIP = "127.0.0.1";
    int32_t result;
    mpe_Bool connected = false;
    int count = 0;

    TRACE(MPE_LOG_TRACE7, MPE_MOD_TEST, "        clientSetup1()\n");

    c->sendSock = socketCreate(AF_INET, SOCK_STREAM, 0);
    CuAssert(t, "Test Inconclusive -  socketCreate() failed", c->sendSock > 0);

    /*
     ** Setup the mpe_SocketSockAddr struct
     ** Sleep to allow time for the server sock to be established.
     ** This helps the tests on the set top complete successfully.
     */
    memset((uint8_t *) &c->client_addr, 0, sizeof(c->client_addr));
    c->client_addr.sin_family = AF_INET;
    result = socketAtoN(serverIP, &(c->client_addr.sin_addr));
    c->client_addr.sin_port = htons((uint16_t) p);

    /*
     ** Attempted to connect to the socket.  Fail after MAXCONNECTATTEMPTS tries.
     */
    do
    {
        result = socketConnect(c->sendSock,
                (mpe_SocketSockAddr*) &c->client_addr, sizeof(c->client_addr));

        if (result == SOCKET_FAIL)
        {
            count++;
            TRACE(
                    MPE_LOG_DEBUG,
                    MPE_MOD_TEST,
                    "        clientSetup1() - connect attempt %2d failed - errno: %d\n",
                    count, socketGetLastError());
            threadSleep(1000, 0);
        }
        else
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "        clientSetup1() - connected\n");
            connected = true;
        }
    } while (!connected && (MAXCONNECTATTEMPTS > count));

    if (!connected)
    {
        if (MAXCONNECTATTEMPTS == count)
        {
            gConnectLimitFlag = TRUE;
        }
        return FALSE;
    }
    return TRUE;
}

/****************************************************************************
 *
 *   getTestSuite_netReadWrite - Create and return the socket read/write
 *                               test suite
 *
 ***************************************************************************/

CuSuite* getTestSuite_netReadWrite(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, vte_test_socketRecv);
    SUITE_ADD_TEST(suite, vte_test_socketRecvFrom);
    SUITE_ADD_TEST(suite, vte_test_socketSend);
    SUITE_ADD_TEST(suite, vte_test_socketSendTo);

    return suite;
}

