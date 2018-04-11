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
 *    -# mpeos_socketFDClear()\n
 *    -# mpeos_socketFDIsSet()\n
 *    -# mpeos_socketFDSet()\n
 *    -# mpeos_socketFDZero()\n
 *    -# mpeos_socketSelect()\n
 */

#include "test_net.h"

CuSuite* getTestSuite_netSelect(void);

/*
 *  Test functions defined in this source file
 */

static void vte_test_socketFDClear(CuTest*);
static void vte_test_socketFDIsSet(CuTest*);
static void vte_test_socketFDSet(CuTest*);
static void vte_test_socketFDZero(CuTest*);
static void vte_test_socketSelect(CuTest*);

/****************************************************************************
 *
 *  vte_test_socketFDClear()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_socketFDClear()" function.
 *
 * \api mpeos_socketFDClear()
 *
 * \strategy Call the "mpeos_socketFDClear()" function.
 *
 * \assets none
 *
 */

static void vte_test_socketFDClear(CuTest *tc)
{
    mpe_Socket fd1 = MPE_SOCKET_INVALID_SOCKET;
    mpe_Socket fd2 = MPE_SOCKET_INVALID_SOCKET;
    mpe_Socket fd3 = MPE_SOCKET_INVALID_SOCKET;
    mpe_SocketFDSet fdset;
    int result;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered test_sockeFDClear\n");

    /*  Clear the test FD set  */

    socketFDZero(&fdset);

    /*  create some test sockets  */

    fd1 = socketCreate(AF_INET, SOCK_STREAM, 0);
    CuAssert(tc, "Test setup failed ('socketCreate()')", fd1
            != MPE_SOCKET_INVALID_SOCKET);

    fd2 = socketCreate(AF_INET, SOCK_STREAM, 0);
    CuAssert(tc, "Test setup failed ('socketCreate()')", fd2
            != MPE_SOCKET_INVALID_SOCKET);

    fd3 = socketCreate(AF_INET, SOCK_STREAM, 0);
    CuAssert(tc, "Test setup failed ('socketCreate()')", fd3
            != MPE_SOCKET_INVALID_SOCKET);

    /**
     * \assertion 'socketFDClear' with a test socket FD which hasn't been added
     *            to the test FD set doesn't crash the STB.
     */

    socketFDClear(fd1, &fdset);

    /*  add the test FDs to the test FD set  */

    socketFDSet(fd1, &fdset);
    socketFDSet(fd2, &fdset);
    socketFDSet(fd3, &fdset);

    /*  make sure fd2 was added  */

    result = socketFDIsSet(fd2, &fdset);
    CuAssert(tc, "Test setup failed ('socketFDSet()')", 0 != result);

    /**
     * \assertion 'socketFDClear' with a valid test socket FD which has been
     *            added to the test FD set doesn't crash the STB.
     */

    socketFDClear(fd2, &fdset);

    /**
     * \assertion 'socketFDClear' with a valid test socket FD which has been
     *            added to the test FD set removes the test socket FD.
     */

    result = socketFDIsSet(fd2, &fdset);
    CuAssert(tc, "socketFDClear() failed to remove FD", 0 == result);

    /**
     * \assertion 'socketFDClear' with a valid test socket FD which has been
     *            added to the test FD set does not remove other socket FDs
     *            from the FD set.
     */

    result = socketFDIsSet(fd1, &fdset);
    CuAssert(tc, "socketFDClear() removed wrong FD (1)", 0 != result);

    result = socketFDIsSet(fd3, &fdset);
    CuAssert(tc, "socketFDClear() removed wrong FD (3)", 0 != result);

    /*  free the test socket descriptor  */

    socketClose(fd1);
    socketClose(fd2);
    socketClose(fd3);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_socketFDClear finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_socketFDIsSet()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_socketFDIsSet()" function.
 *
 * \api mpeos_socketFDIsSet()
 *
 * \strategy Call the "mpeos_socketFDIsSet()" function.
 *
 * \assets none
 *
 */

static void vte_test_socketFDIsSet(CuTest *tc)
{
    mpe_Socket fd;
    mpe_SocketFDSet fdset;
    int32_t result;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered test_sockeFDIsSet\n");

    fd = socketCreate(AF_INET, SOCK_STREAM, 0);

    socketFDZero(&fdset);

    result = socketFDIsSet(fd, &fdset);
    CuAssert(tc, "socketFDIsSet() failed...", result == 0);
    fd = 0;

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_socketFDIsSet finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_socketFDSet()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_socketFDSet()" function.
 *
 * \api mpeos_socketFDSet()
 *
 * \strategy Call the "mpeos_socketFDSet()" function.
 *
 * \assets none
 *
 */

static void vte_test_socketFDSet(CuTest *tc)
{
    mpe_Socket fd;
    mpe_SocketFDSet fdset;
    int result;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_socketFDSet\n");

    /*  Clear the test FD set  */

    socketFDZero(&fdset);

    /*  Create a test socket  */

    fd = socketCreate(AF_INET, SOCK_STREAM, 0);
    CuAssert(tc, "Test setup failed ('socketCreate()')", fd
            != MPE_SOCKET_INVALID_SOCKET);

    /*  Add the test socket FD to the test FD set  */

    socketFDSet(fd, &fdset);

    /**
     * \assertion 'socketFDIsSet()' indicates that the test socket FD
     *            is part of the test FD set
     */

    result = socketFDIsSet(fd, &fdset);
    CuAssert(tc, "socketFDSet() failed", 0 != result);

    /*  free the test socket descriptor  */

    socketClose(fd);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_socketFDSet finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_socketFDZero()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_socketFDZero()" function.
 *
 * \api mpeos_socketFDZero()
 *
 * \strategy Call the "mpeos_socketFDZero()" function. Since
 *           "mpeos_socketFDZero()" doesn't return anything, and
 *           "mpe_SocketFDSet" is an opaque type, I'm not certain how to
 *           determine pass or fail. For now, call it pass if calling the
 *           function doesn't crash the box.
 *
 * \assets none
 *
 */

static void vte_test_socketFDZero(CuTest *tc)
{
    mpe_SocketFDSet fdset;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_sockeFDZero\n");

    socketFDZero(&fdset);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_socketFDZero finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_socketSelect()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_socketSelect()" function.
 *
 * \api mpeos_socketSelect()
 *
 * \strategy Call the "mpeos_socketSelect()" function.
 *
 * \assets none
 *
 */

static void vte_test_socketSelect(CuTest *tc)
{
    struct timeval selTimeout;
    mpe_SocketFDSet fdset;
    mpe_ThreadId tid = 0;
    SendThreadData std;
    mpe_Socket serverFd;
    int32_t recvFd;
    mpe_SocketIPv4SockAddr server_addr;
    uint32_t timeout = 0;
    uint32_t timeout_ms = 100000;
    int iServerSize = sizeof(server_addr);
    int port;
    const char msg[] = "test_socketSelect() test message";
    int strSize = sizeof(msg);
    char *buff = 0;
    mpe_Error ec;
    int32_t result;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_sockeSelect\n");

    port = getUniquePort();
    memAllocP(MPE_MEM_TEST, strSize, (void **) &buff);

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
     ** Populate the structure to send to the thread.  Includes the compare
     ** msg, the port to communicate over, thread data, and the test case ptr.
     */
    popSendStructure(&std, tc, port, msg, &serverFd);

    ec = threadCreate(testNet_SendThread, &std, MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_STACK_SIZE, &tid, "testNet_SendThread");
    CuAssert(tc, "Test Inconclusive - threadCreate() failed", ec == MPE_SUCCESS);

    (void) memset((uint8_t *) &server_addr, 0, sizeof(server_addr));
    server_addr.sin_family = AF_INET;
    server_addr.sin_addr.s_addr = htonl(INADDR_ANY);
    server_addr.sin_port = htons((uint16_t) port);

    result = socketBind(serverFd, (mpe_SocketSockAddr*) &server_addr,
            sizeof(server_addr));
    CuAssert(tc, "Test Inconclusive - socketBind() failed", result >= 0);

    result = socketListen(serverFd, 5);
    CuAssert(tc, "Test Inconclusive - socketListen() failed", result >= 0);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    listening on port: %d\n", port);

    recvFd = socketAccept(serverFd, (mpe_SocketSockAddr *) &server_addr,
            &iServerSize);
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
        CuAssert(tc, "Test Inconclusive - socketAccept() failed", recvFd > 0);
        recvFd = 0;
    }
    else
    {
        /* Zero socket descriptor vector and set for sockets */
        /* This must be reset every time select() is called */
        socketFDZero(&fdset);
        socketFDSet(recvFd, &fdset);

        selTimeout.tv_sec = timeout; /* timeout (secs.) */
        selTimeout.tv_usec = timeout_ms; /* microseconds */

        /* Now call the tested function */
        result = socketSelect(1, &fdset, NULL, NULL, &selTimeout);
        if (result > 0)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "Select Failed - errno: %d\n",
                    socketGetLastError());
            CuAssert(tc, "Test Failed - socketSelect() failed...", result > 0);
        }
        if (result)
        {
            if (FD_ISSET(recvFd, &fdset))
            {
                result = socketRecv(recvFd, buff, strSize, 0);
                CuAssert(tc, "socketRecv() failed...", result > 0);
                TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                        "test_socketSelect - msg: %s\n", buff);
            }
        }
        result = socketClose(serverFd);
        CuAssert(tc, "Test Inconclusive - socketClose() failed", result
                != SOCKET_FAIL);
        serverFd = 0;
        recvFd = 0;
    }

    /* Wait for the send thread to exit  */

    testNet_waitForThread();

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_socketSelect finished.\n\n");
}

/**
 * Create and return the test suite for the socket APIs tested in this suite.
 * @return a pointer to the new test suite.
 */
CuSuite* getTestSuite_netSelect(void)
{
    CuSuite* suite = CuSuiteNew();
    SUITE_ADD_TEST(suite, vte_test_socketFDClear);
    SUITE_ADD_TEST(suite, vte_test_socketFDIsSet);
    SUITE_ADD_TEST(suite, vte_test_socketFDSet);
    SUITE_ADD_TEST(suite, vte_test_socketFDZero);
    SUITE_ADD_TEST(suite, vte_test_socketSelect);

    return suite;
}
