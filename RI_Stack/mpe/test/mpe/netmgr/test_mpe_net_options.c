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

/** \file test_net_options.c
 *
 *  \brief Test functions for MPEOS/MPE socket option functions
 *
 *  This file contains tests for the following MPEOS/MPE functions :\n
 *    -# mpeos_socketIoctl()\n
 *    -# mpeos_socketGetOpt()\n
 *    -# mpeos_socketSetOpt)\n
 */

#include "test_net.h"

CuSuite* getTestSuite_mpe_netOptions(void);

static void vte_test_socketIoctl(CuTest*);
static void vte_test_socketGetLastError(CuTest*);

/****************************************************************************
 *
 *  test_socketIoctl()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "socketIoctl" function 
 *
 * \api socketIoctl()
 *
 * \strategy Call the "socketIoctl()" function and check for reasonable
 * return values.
 *
 * \assets none
 *
 */

static void vte_test_socketIoctl(CuTest *tc)
{
    int32_t serverFd;
    int result = 1;
    int blocking;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_socketIoctl\n");

    /**
     * \assertion "mpeos_socketCreate()" returns a socket FD which is > 0
     */

    serverFd = socketCreate(AF_INET, SOCK_STREAM, 0);
    CuAssert(tc, "socketCreate() failed", serverFd > 0);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  'socketCreate()' returned %d\n",
            serverFd);

    /**
     * \assertion "mpeos_socketIoctl()" returns 0 on success
     */

    blocking = 0;
    result = socketIoctl(serverFd, MPE_SOCKET_FIONBIO, &blocking);
    CuAssert(tc, "socketIoctl failed", 0 == result);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  'socketIoctl()' returned %d\n",
            result);

    socketClose(serverFd);
    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_socketIoctl finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_socketGetOpt()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "socketGetOpt" function 
 *
 * \api socketIoctl()
 *
 * \strategy Call the "socketGetOpt()" function and check for reasonable
 *           return values.
 *
 * \assets none
 *
 */

static void vte_test_socketGetOpt(CuTest *tc)
{
    int32_t Fd;
    int result = 0;
    uint8_t buff[sizeof(int)];
    int size = sizeof(int);

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_socketGetOpt\n");

    Fd = socketCreate(AF_INET, SOCK_STREAM, 0);
    CuAssert(tc, "socketCreate() failed", Fd > 0);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    'socketCreate()' returned %d\n", Fd);

    result = socketGetOpt(Fd, MPE_SOCKET_SOL_SOCKET, MPE_SOCKET_SO_TYPE, buff,
            (mpe_SocketSockLen *) &size);
    CuAssert(tc, "socketGetOpt failed", result != -1);

    socketClose(Fd);
    Fd = 0;

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_socketGetOpt finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_socketSetOpt()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "socketSetOpt" function 
 *
 * \api socketIoctl()
 *
 * \strategy Call the "socketSetOpt()" function and check for reasonable
 *           return values.
 *
 * \assets none
 *
 */
static void vte_test_socketSetOpt(CuTest *tc)
{
    int32_t Fd;
    int result = 0;
    int dummy = 1;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_socketSetOpt\n");

    Fd = socketCreate(AF_INET, SOCK_STREAM, 0);
    CuAssert(tc, "socketCreate() failed", Fd > 0);

    result = socketSetOpt(Fd, MPE_SOCKET_SOL_SOCKET, OS_SOCKET_TCP_NODELAY,
            (char*) &dummy, sizeof(dummy));
    CuAssert(tc, "socketSetOpt failed", result != -1);

    socketClose(Fd);
    Fd = 0;

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_socketSetOpt finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_socketGetLastError()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "socketGetLastError" function 
 *
 * \api socketGetLastError()
 *
 * \strategy Call socket functions with invalid parameters, then call
 *           "socketGetLastError()" and check that it returns the expected
 *           error code.
 *
 * \assets none
 *
 */
static void vte_test_socketGetLastError(CuTest *tc)
{
    int result = -1;
    mpe_SocketHostEntry *he;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_socketGetLastError\n");

    /**
     * \assertion "mpeos_socketGetHostByName()" returns NULL when passed a
     *            bogus hostname
     */

    he = socketGetHostByName("AnInvalidHostName");
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  socketGetHostByName() returned %d\n",
            (int) he);
    CuAssert(
            tc,
            "Test setup failure - 'socketGetHostByName() failed to return expected error",
            NULL == he);

    /**
     * \assertion "socketGetLastError()" returns expected error status
     */

    result = socketGetLastError();
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  socketGetLastError() returned %d\n",
            result);
    CuAssert(
            tc,
            "socketGetLastError() failed to return expected error (MPE_SOCKET_EHOSTNOTFOUND)",
            MPE_SOCKET_EHOSTNOTFOUND == result);

    /**
     * \assertion "mpeos_socketShutdown()" returns -1 when passed bad parameters
     *            
     */

    result = socketShutdown(MPE_SOCKET_INVALID_SOCKET, -9999);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  socketShutdown() returned %d\n",
            result);
    CuAssert(
            tc,
            "Test setup failure - 'socketShutdown() failed to return expected error",
            -1 == result);

    /**
     * \assertion "socketGetLastError()" returns expected error status
     */

    result = socketGetLastError();
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  socketGetLastError() returned %d\n",
            result);
    CuAssert(
            tc,
            "socketGetLastError() failed to return expected error (MPE_SOCKET_EHOSTNOTFOUND)",
            MPE_SOCKET_ENOTSOCK == result);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_socketGetLastError finished.\n\n");
}

/**
 * Create and return the test suite for the socket APIs tested in this suite.
 * @return a pointer to the new test suite.
 */
CuSuite* getTestSuite_mpe_netOptions(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, vte_test_socketIoctl);
    SUITE_ADD_TEST(suite, vte_test_socketGetOpt);
    SUITE_ADD_TEST(suite, vte_test_socketSetOpt);
    SUITE_ADD_TEST(suite, vte_test_socketGetLastError);
    return suite;
}
