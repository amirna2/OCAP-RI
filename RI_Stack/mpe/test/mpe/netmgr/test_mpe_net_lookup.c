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

/** \file test_mpe_net_lookup.c
 *
 *  \brief Test functions for MPEOS socket related lookup functions
 *
 *  This file contains tests for the following MPEOS functions :\n
 *
 *    -# mpeos_socketAtoN()\n
 *    -# mpeos_socketNtoA()\n
 *    -# mpeos_socketHtoNL()\n
 *    -# mpeos_socketHtoNS()\n
 *    -# mpeos_socketNtoHL()\n
 *    -# mpeos_socketNtoHS()\n
 *    -# mpeos_socketNtoP()\n
 *    -# mpeos_socketPtoN()\n
 *    -# mpeos_socketGetHostByAddr()\n
 *    -# mpeos_socketGetHostByName()\n
 *    -# mpeos_socketGetHostName()\n
 *    -# mpeos_socketGetPeerName()\n
 *    -# mpeos_socketGetSockName()\n
 *
 */

#include <test_net.h>

/*  Network test functions  */

static void vte_test_socketAtoN(CuTest*);
static void vte_test_socketAtoN_neg(CuTest*);
static void vte_test_socketNtoA(CuTest*);
static void vte_test_socketHtoNL(CuTest*);
static void vte_test_socketHtoNS(CuTest*);
static void vte_test_socketNtoHL(CuTest*);
static void vte_test_socketNtoHS(CuTest*);
static void vte_test_socketNtoP(CuTest*);
static void vte_test_socketPtoN(CuTest*);
static void vte_test_socketGetHostByAddr(CuTest*);
static void vte_test_socketGetHostByName(CuTest*);
static void vte_test_socketGetHostName(CuTest*);
static void vte_test_socketGetPeerName(CuTest*);
static void vte_test_socketGetSockName(CuTest*);

CuSuite* getTestSuite_netLookup(void);

/*  Internal functions  */
static uint32_t testNet_htonl(uint32_t);
static uint16_t testNet_htons(uint16_t);
static uint32_t testNet_ntohl(uint32_t);
static uint16_t testNet_ntohs(uint16_t);

extern char* decodeError(mpe_Error);
extern void testNet_waitForThread(void);

/****************************************************************************
 *
 *  Test values used by lookup function tests.
 *
 */

/*  These are used by A to N tests only */

NetTestAddrv4 AtoNtestvals[] = /*  These are all valid IP v4 test values and  */
{ /*  conversion should succeed for all of them. */

{ "0.0.0", 0x00000000 },
{ "0.1.1", 0x00010001 },
{ "0.0.255", 0x000000ff },
{ "0.0.256", 0x00000100 },
{ "0.1.0", 0x00010000 },
{ "1.0.0", 0x01000000 },
{ "1.2.345", 0x01020159 },
{ "127.255.65535", 0x7fffffff },
{ "234.56.7890", 0xea381ed2 },

{ "0.0", 0x00000000 },
{ "0.1", 0x00000001 },
{ "1.0", 0x01000000 },
{ "2.255", 0x020000ff },
{ "3.256", 0x03000100 },
{ "4.65535", 0x0400ffff },
{ "5.65536", 0x05010000 },
{ "6.11164245", 0x06aa5a55 },

{ "0000.0001.0020.0300", 0x000110c0 },
{ "026.04.0377.0135", 0x1604ff5d },
{ "0377.4.037.0123", 0xff041f53 },

{ "0xcc.0x9e.0xa8.0x0c", 0xcc9ea80c },
{ "0x01.0x23.0x45.0x67", 0x01234567 },
{ "0xfe.0xd3.0x5c.0xab", 0xfed35cab } };

/*  These are used by both A to N  and N to A tests  */

NetTestAddrv4 NtoAtestvals[] = /*  These are all valid IP v4 test values and  */
{ /*  conversion should succeed for all of them. */
{ "0.0.0.0", 0 },
{ "0.0.0.1", 1 },
{ "0.0.0.15", 15 },
{ "0.0.0.16", 16 },
{ "0.0.0.127", 127 },
{ "0.0.0.255", 255 },
{ "0.0.1.0", 256 },
{ "0.0.255.0", 0x0000ff00 },
{ "0.1.0.0", 0x00010000 },
{ "0.17.0.0", 0x00110000 },
{ "0.165.0.0", 0x00a50000 },
{ "1.0.0.1", 0x01000001 },
{ "127.0.0.1", 0x7f000001 },
{ "192.168.158.204", 0xc0a89ecc },
{ "253.254.255.57", 0xfdfeff39 },
{ "255.255.255.255", 0xffffffff } };

/*  Negative test values for A to N tests  */

char
        *testnetvalsnegative[] = /*  These test values are all invalid and   */
        { /*  conversion should fail for all of them. */
                "0.z.0.0", /* invalid character  */
                "0,0,0-1", /* invalid separater  */
                "string", /* just a string  */
                "a longer string.", /* a longer string  */
                /* a much longer string (288 chars, if I've counted right)  */
                "a much longer string that may overrun a statically allocated buffer\
     somewhere important and cause a system crash at an embarrassing moment\
     when you least expect it, leading to much frustration, although it's\
     probably better to crash while testing than while running real code.",
                "256.1.0.0", /* value out of range  */
                "127.0.0.256", /* another out of range value  */
                "255.10.123456", /* out of range three part address */
                "192.168.158.one", /* almost got it right . . .  */

                "0388.0.0.0", /* out of range octal value  */
                "034v.0.0.0", /* invalid octal char  */

                "0xc0.0x23.0xg0.0x45" /* invalid hex char  */
        };

/****************************************************************************
 *
 *  vte_test_socketAtoN()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_socketsocketAtoN()" function 
 *
 * \api mpeos_socketAtoN()
 *
 * \strategy Call the "mpeos_socketAtoN()" function with a variety of valid
 *           test values and checks for correct conversion.
 *
 * \assets none
 *
 */

static void vte_test_socketAtoN(CuTest *tc)
{
    int i;
    mpe_SocketIPv4Addr my_IPaddr;
    int result = 0;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_socketAtoN\n");

    /*  first set of test values  */

    for (i = 0; i < sizeof AtoNtestvals / sizeof(AtoNtestvals[i]); i++)
    {
        result = socketAtoN(AtoNtestvals[i].strAddr, &(my_IPaddr));
        if (0 != result)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    passed %-20s got %08x",
                    AtoNtestvals[i].strAddr, my_IPaddr.s_addr);
            if (my_IPaddr.s_addr == AtoNtestvals[i].numAddr)
            {
                TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  (OK)\n");
            }
            else
            {
                TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                        ",   expected %08x  (ERROR)\n", AtoNtestvals[i].numAddr);
                CuFail(tc, "socketNtoA() returned wrong result");
            }
        }
        else
        {
            TRACE(
                    MPE_LOG_DEBUG,
                    MPE_MOD_TEST,
                    "    'socketAtoN()' returned unexpected error for valid input value '%s'\n",
                    AtoNtestvals[i].strAddr);
            CuFail(tc, "socketAtoN() returned unexpected error");
        }
    }

    /*  second set of test values  */

    for (i = 0; i < sizeof NtoAtestvals / sizeof(NtoAtestvals[i]); i++)
    {
        result = socketAtoN(NtoAtestvals[i].strAddr, &(my_IPaddr));
        if (0 != result)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    passed %-20s got %08x",
                    NtoAtestvals[i].strAddr, my_IPaddr.s_addr);
            if (my_IPaddr.s_addr == NtoAtestvals[i].numAddr)
            {
                TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  (OK)\n");
            }
            else
            {
                TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                        ",   expected %08x  (ERROR)\n", NtoAtestvals[i].numAddr);
                CuFail(tc, "socketNtoA() returned wrong result");
            }
        }
        else
        {
            TRACE(
                    MPE_LOG_DEBUG,
                    MPE_MOD_TEST,
                    "    'socketAtoN()' returned unexpected error for valid input value '%s'\n",
                    NtoAtestvals[i].strAddr);
            CuFail(tc, "socketAtoN() returned unexpected error");
        }
    }
    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_socketAtoN finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_socketAtoN_neg()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_socketsocketAtoN()" function 
 *
 * \api mpeos_socketAtoN()
 *
 * \strategy Call the "mpeos_socketAtoN()" function with a variety of invalid
 *           test values and check that the expected error is returned.
 *
 * \assets none
 *
 */

static void vte_test_socketAtoN_neg(CuTest *tc)
{
    int i;
    mpe_SocketIPv4Addr my_IPaddr;
    int result = 0;
    int inLen = 0;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_socketAtoN_neg\n");

    for (i = 0; i < sizeof testnetvalsnegative / sizeof(testnetvalsnegative[i]); i++)
    {
        inLen = strlen(testnetvalsnegative[i]);
        result = socketAtoN(testnetvalsnegative[i], &(my_IPaddr));
        if (0 != result)
        {
            TRACE(
                    MPE_LOG_DEBUG,
                    MPE_MOD_TEST,
                    "    failed to return expected error for invalid input value '%s',  length = %3d, got %08x  (ERROR)\n",
                    testnetvalsnegative[i], inLen, my_IPaddr.s_addr);
            CuFail(tc,
                    "socketAtoN() failed to return error for invalid input value");
        }
        else
        {
            TRACE(
                    MPE_LOG_DEBUG,
                    MPE_MOD_TEST,
                    "    got expected error for invalid input value '%s',  length = %3d  (OK)\n",
                    testnetvalsnegative[i], inLen);
        }
    }
    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_socketAtoN_neg finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_socketNtoA()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_socketsocketNtoA()" function 
 *
 * \api mpeos_socketNtoA()
 *
 * \strategy Call the "mpeos_socketNtoA()" function with a variety of test
 *           values and checks for correct result.
 *
 * \assets none
 *
 */

static void vte_test_socketNtoA(CuTest *tc)
{
    int i;
    mpe_SocketIPv4Addr my_addr;
    char *result;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_socketNtoA\n");

    for (i = 0; i < sizeof NtoAtestvals / sizeof(NtoAtestvals[i]); i++)
    {
        my_addr.s_addr = NtoAtestvals[i].numAddr;
        result = socketNtoA(my_addr);

        if (NULL != result)
        {
            if (strcmp(result, NtoAtestvals[i].strAddr) == 0)
            {
                TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                        "  passed %08x, got %-16s (OK)\n", my_addr.s_addr,
                        result);
            }
            else
            {
                TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                        "  passed %08x, got %s, expected %s (ERROR)\n",
                        my_addr.s_addr, result, NtoAtestvals[i].strAddr);
                CuFail(tc, "socketNtoA() returned wrong value");
            }
        }
        else
        {
            TRACE(
                    MPE_LOG_DEBUG,
                    MPE_MOD_TEST,
                    "  socketNtoA() returned NULL string for input value %08x (ERROR)\n",
                    my_addr.s_addr);
            CuFail(tc, "socketNtoA() returned NULL string");
        }
    }
    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_socketNtoA finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_socketHtoNL()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_socketsocketHtoNL()" function 
 *
 * \api mpeos_socketHtoNL()
 *
 * \strategy Call the "mpeos_socketHtoNL()" function with a variety of test
 *           values and checks for correct result.
 *
 * \assets none
 *
 */

static void vte_test_socketHtoNL(CuTest *tc)
{
    uint32_t testvals[] =
    { 0, 1, 127, 255, 256, 1234, 12345, 65535, 65536, 0x00080000, 0x5a438176,
            0x80000000, 0xffffffff };
    uint32_t result;
    uint32_t expected;
    int i;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_socketHtoNL\n");

    for (i = 0; i < sizeof testvals / sizeof(testvals[i]); i++)
    {
        result = socketHtoNL(testvals[i]);
        expected = testNet_htonl(testvals[i]);
        CuAssert(tc, "socketHtoNL() failed", expected == result);
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  socketHtoNL() - expected %08x, got %08x (%s)\n", expected,
                result, ((expected == result) ? "OK" : "ERROR"));
    }
    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_socketHtoNL finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_socketHtoNS()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_socketsocketHtoNS()" function 
 *
 * \api mpeos_socketHtoNS()
 *
 * \strategy Call the "mpeos_socketHtoNS()" function with a variety of test
 *           values and checks for correct result.
 *
 * \assets none
 *
 */

static void vte_test_socketHtoNS(CuTest *tc)
{
    uint16_t
            testvals[] =
            { 0, 1, 127, 255, 256, 1234, 12345, 0x8000, 0x8001, 0xa536, 0xf001,
                    0xffff };
    uint16_t result;
    uint16_t expected;
    int i;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_socketHtoNS\n");

    for (i = 0; i < sizeof testvals / sizeof(testvals[i]); i++)
    {
        result = socketHtoNS(testvals[i]);
        expected = testNet_htons(testvals[i]);
        CuAssert(tc, "socketHtoNS() failed", expected == result);
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  socketHtoNS() - expected %04x, got %04x (%s)\n", expected,
                result, ((expected == result) ? "OK" : "ERROR"));
    }
    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_socketHtoNS finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_socketNtoHL()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_socketsocketNtoHL()" function 
 *
 * \api mpeos_socketNtoHL()
 *
 * \strategy Call the "mpeos_socketNtoHL()" function with a variety of test
 *           values and checks for correct result.
 *
 * \assets none
 *
 */

static void vte_test_socketNtoHL(CuTest *tc)
{
    uint32_t testvals[] =
    { 0, 1, 127, 255, 256, 1234, 12345, 65535, 65536, 0x00080000, 0x5a438176,
            0x80000000, 0xffffffff };
    uint32_t result;
    uint32_t expected;
    int i;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_socketNtoHL\n");

    for (i = 0; i < sizeof testvals / sizeof(testvals[i]); i++)
    {
        result = socketNtoHL(testvals[i]);
        expected = testNet_ntohl(testvals[i]);
        CuAssert(tc, "socketNtoHL() failed", expected == result);
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  socketNtoHL() - expected %08x, got %08x (%s)\n", expected,
                result, ((expected == result) ? "OK" : "ERROR"));
    }
    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_socketNtoHL finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_socketNtoHS()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_socketsocketNtoHS()" function 
 *
 * \api mpeos_socketNtoHS()
 *
 * \strategy Call the "mpeos_socketNtoHS()" function with a variety of test
 *           values and checks for correct result.
 *
 * \assets none
 *
 */

static void vte_test_socketNtoHS(CuTest *tc)
{
    uint16_t
            testvals[] =
            { 0, 1, 127, 255, 256, 1234, 12345, 0x8000, 0x8001, 0xa536, 0xf001,
                    0xffff };
    uint16_t result;
    uint16_t expected;
    int i;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_socketNtoHS\n");

    for (i = 0; i < sizeof testvals / sizeof(testvals[i]); i++)
    {
        result = socketNtoHS(testvals[i]);
        expected = testNet_ntohs(testvals[i]);
        CuAssert(tc, "socketNtoHS() failed", expected == result);
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  socketNtoHS() - expected %04x, got %04x (%s)\n", expected,
                result, ((expected == result) ? "OK" : "ERROR"));
    }
    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_socketNtoHS finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_socketPtoN(()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_socketsocketPtoN()" function 
 *
 * \api mpeos_socketPtoN()
 *
 * \strategy Call the "mpeos_socketPtoN()" function with a variety of test
 *           values and checks for correct result.
 *
 * \assets none
 *
 */

static void vte_test_socketPtoN(CuTest *tc)
{
    int result = 0;
    const char *ip = TEST_NET_HOSTIP;
    char inbuff[128];

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_socketPtoN\n");

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Running IPv4 vte_test_socketPtoN\n");
    result = mpeos_socketPtoN(MPE_SOCKET_AF_INET4, ip , (void *)inbuff);
    CuAssert( tc, "socketPtoN() failed for IPv4", result == 1);

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Running IPv6 vte_test_socketPtoN\n");
    result = mpeos_socketPtoN(MPE_SOCKET_AF_INET6, ip , (void *)inbuff);
    CuAssert( tc, "socketPtoN() failed for IPv6", result == 1);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_socketPtoN finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_socketNtoP()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_socketsocketNtoP()" function 
 *
 * \api mpeos_socketNtoP()
 *
 * \strategy Call the "mpeos_socketNtoP()" function with a variety of test
 *           values and checks for correct result.
 *
 * \assets none
 *
 */

static void vte_test_socketNtoP(CuTest *tc)
{
    int result = 0;
    const char *ip = TEST_NET_HOSTIP;
    char * b_result;
    const int buffsize = 128;
    char inbuff[buffsize];
    char outbuff[buffsize];

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_socketNtoP\n");

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Running IPv4 vte_test_socketNtoP\n");
    result = socketPtoN(MPE_SOCKET_AF_INET4, ip , (void *)inbuff);
    CuAssert( tc, "socketPtoN() prep failed for IPv4", result == 1);
    b_result = socketNtoP(MPE_SOCKET_AF_INET4, inbuff, outbuff, buffsize);
    CuAssert( tc, "socketNtoP() failed for IPv4", b_result == NULL);

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Running IPv6 vte_test_socketNtoP\n");
    result = socketPtoN(MPE_SOCKET_AF_INET6, ip , (void *)inbuff);
    CuAssert( tc, "socketPtoN() prep failed for IPv6", result == 1);
    b_result = socketNtoP(MPE_SOCKET_AF_INET6, inbuff, outbuff, buffsize);
    CuAssert( tc, "socketNtoP() failed for IPv6", b_result == NULL);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_socketNtoP finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_socketGetHostByAddr()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "socketGetHostByAddr()" function 
 *
 * \api mpeos_socketGetHostByAddr()
 *
 * \strategy Call the "mpeos_socketGetHostByAddr()" function with a variety
 *           of test values and checks for correct result.
 *
 * \assets none
 *
 */
static void vte_test_socketGetHostByAddr(CuTest *tc)
{
    mpe_SocketHostEntry *he = NULL;
    mpe_SocketIPv4Addr test_addr;
    mpe_Error ec;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
            "Entered vte_test_socketGetHostByAddr\n");

    socketAtoN(TEST_NET_HOSTIP, &test_addr);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  addr = %s (0x%08x)\n",
            TEST_NET_HOSTIP, test_addr.s_addr);
    he = socketGetHostByAddr((void *) &test_addr, sizeof(test_addr),
            MPE_SOCKET_AF_INET4);

    if (he == NULL)
    {
        ec = (mpe_Error) socketGetLastError();
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  socketGetHostByAddr() returned error %s\n", decodeError(ec));
        CuFail(tc, "socketGetHostByAddr() returned an error");
    }
    else
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  socketGetHostByAddr() returned '%s'\n", he->h_name);
        CuAssertStrEquals(tc, TEST_NET_HOSTNAME, (const char *) (he->h_name));
    }
    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_socketGetHostByAddr finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_socketGetHostByName()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "socketGetHostByName()" function 
 *
 * \api mpeos_socketGetHostByName()
 *
 * \strategy Call the "mpeos_socketGetHostByName()" function with a variety
 *           of test values and checks for correct result.
 *
 * \assets none
 *
 */

static void vte_test_socketGetHostByName(CuTest *tc)
{
    mpe_SocketHostEntry * he = NULL;
    mpe_Error ec;
    int i;
    char *cp;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
            "Entered vte_test_socketGetHostByName\n");

    he = socketGetHostByName(TEST_NET_HOSTNAME);
    if (he == NULL)
    {
        ec = (mpe_Error) socketGetLastError();
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  socketGetHostByName() returned error %s\n", decodeError(ec));
        CuFail(tc, "socketGetHostByName() returned an error");
    }
    else
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  socketGetHostByName() returned mpe_SocketHostEntry at %p\n",
                he);
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "    mpe_SocketHostEntry.h_name      == '%s'\n", he->h_name);
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "    mpe_SocketHostEntry.h_aliases   == %p\n", he->h_aliases);
        if (NULL != he->h_aliases)
        {
            for (i = 0; i < 10; i++)
            {
                if (NULL != he->h_aliases[i])
                {
                    TRACE(
                            MPE_LOG_DEBUG,
                            MPE_MOD_TEST,
                            "      mpe_SocketHostEntry.h_aliases[%d] == %p, alias == '%s'\n",
                            i, he->h_aliases[i], *he->h_aliases[i]);
                }
                else
                {
                    TRACE(
                            MPE_LOG_DEBUG,
                            MPE_MOD_TEST,
                            "      mpe_SocketHostEntry.h_aliases[%d] == NULL\n",
                            i);
                    break;
                }
            }
        }

        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "    mpe_SocketHostEntry.h_addrtype  == %d\n", he->h_addrtype);

        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "    mpe_SocketHostEntry.h_addr_list == %p\n", he->h_addr_list);

        if (NULL != he->h_addr_list)
        {
            for (i = 0; i < 10; i++)
            {
                TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                        "    mpe_SocketHostEntry.h_addr_list[%d] at %p\n", i,
                        he->h_addr_list[i]);

                if (NULL != he->h_addr_list[i])
                {
                    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                            "    mpe_SocketHostEntry.h_addr_list[%d] == ", i);

                    cp = he->h_addr_list[i];

                    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "%d.%d.%d.%d\n", cp[0],
                            cp[1], cp[2], cp[3]);

                }
                else
                {
                    TRACE(
                            MPE_LOG_DEBUG,
                            MPE_MOD_TEST,
                            "    mpe_SocketHostEntry.h_addr_list[%d] == NULL\n",
                            i);
                    break;
                }
            }
        }

        CuAssertStrEquals(tc, TEST_NET_HOSTNAME, (const char *) (he->h_name));
    }
    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_socketGetHostByName finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_socketGetHostName()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "socketGetHostName()" function 
 *
 * \api mpeos_socketGetHostName()
 *
 * \strategy Call the "mpeos_socketGetHostName()" function with a variety
 *           of valid and invalid parameters and check for correct result.
 *
 * \assets none
 *
 */

static void vte_test_socketGetHostName(CuTest *tc)
{
    char nameBuff[TEST_NET_HOSTNAMEBUFFLEN + 4];
    char *nameP = &nameBuff[2];
    int result = 0;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_socketGetHostName\n");

#if 0  // this crashes the SA 8300 STB
    /**
     * \assertion Passing a null name pointer returns an error (-1)
     */

    result = socketGetHostName(NULL, TEST_NET_HOSTNAMEBUFFLEN);
    if (-1 != result)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  socketGetHostName() failed to return expected error when passed NULL buffer pointer\n");
        CuFail(tc, "socketGetHostName() failed to return expected error when passed NULL buffer pointer\n");
    }
    else
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  socketGetHostName() returned expected error when passed NULL buffer pointer\n");
    }
#endif

    /**
     * \assertion Passing a zero length buffer returns success
     */

    memset(nameBuff, -1, TEST_NET_HOSTNAMEBUFFLEN); /*  init result  */
    result = socketGetHostName(nameP, 0);
    if (0 != result)
    {
        TRACE(
                MPE_LOG_DEBUG,
                MPE_MOD_TEST,
                "  socketGetHostName() returned an unexpected error when passed zero length buffer\n");
        CuFail(
                tc,
                "socketGetHostName() returned an unexpected error when passed zero length buffer\n");
    }
    else
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  socketGetHostName() returned success when passed zero length buffer\n");
    }

    /**
     * \assertion socketGetHostName() doesn't write to the buffer when passed a zero length name buffer
     */

    if (('\xff' != nameBuff[0]) || ('\xff' != nameBuff[1]))
    {
        TRACE(
                MPE_LOG_DEBUG,
                MPE_MOD_TEST,
                "  socketGetHostName() wrote to memory before the buffer pointer when passed zero length buffer :\n");
        TRACE(
                MPE_LOG_DEBUG,
                MPE_MOD_TEST,
                "    buff[-2] == 0x%02x, buff[-1] == 0x%02x   (should all be '0xff')\n",
                nameBuff[0], nameBuff[1]);
        CuFail(
                tc,
                "socketGetHostName() wrote to memory before the buffer pointer when passed a zero length name buffer");
    }

    if (('\xff' != nameBuff[2]) || ('\xff' != nameBuff[3]) || ('\xff'
            != nameBuff[4]) || ('\xff' != nameBuff[5]))
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  socketGetHostName() wrote to buffer when passed zero length buffer :\n");
        TRACE(
                MPE_LOG_DEBUG,
                MPE_MOD_TEST,
                "    buff[0] == 0x%02x, buff[1] == 0x%02x, buff[2] == 0x%02x, buff[3] == 0x%02x   (should all be '0xff')\n",
                nameBuff[2], nameBuff[3], nameBuff[4], nameBuff[5]);
        CuFail(tc,
                "socketGetHostName() wrote to buffer when passed a zero length name buffer");
    }

    /**
     * \assertion Passing one byte buffer does not return an error
     */

    memset(nameBuff, -1, TEST_NET_HOSTNAMEBUFFLEN); /*  init result  */

    if (('\xff' != nameBuff[0]) || ('\xff' != nameBuff[1]) || ('\xff'
            != nameBuff[2]) || ('\xff' != nameBuff[3]) || ('\xff'
            != nameBuff[4]) || ('\xff' != nameBuff[5]))

    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "\n  buffer isn't what I expect (1):\n");
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "    %02x %02x | %02x %02x %02x %02x\n", nameBuff[0],
                nameBuff[1], nameBuff[2], nameBuff[3], nameBuff[4], nameBuff[5]);
    }

    memset(nameBuff, -1, TEST_NET_HOSTNAMEBUFFLEN); /*  init result  */

    result = socketGetHostName(nameP, 1);

    if (0 != result)
    {
        TRACE(
                MPE_LOG_DEBUG,
                MPE_MOD_TEST,
                "  socketGetHostName() returned an unexpected error when passed a one byte buffer\n");
        CuFail(
                tc,
                "socketGetHostName() returned an unexpected error when passed a one byte buffer\n");
    }
    else
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  socketGetHostName() returned success when passed a one byte buffer\n");
    }

    /**
     * \assertion socketGetHostName() doesn't write before or more than 1 byte
     *            after the buffer when passed a one byte buffer
     */

    if (('\xff' != nameBuff[0]) || ('\xff' != nameBuff[1]) || ('\xff'
            != nameBuff[3]) || ('\xff' != nameBuff[4]) || ('\xff'
            != nameBuff[5]))
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  socketGetHostName() wrote beyond buffer when passed one byte buffer :\n");
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "    %02x %02x | %02x %02x %02x %02x\n", nameBuff[0],
                nameBuff[1], nameBuff[2], nameBuff[3], nameBuff[4], nameBuff[5]);
        CuFail(tc,
                "socketGetHostName() wrote beyond buffer when passed one byte buffer");
    }

    /**
     * \assertion Passing valid parameters doesn't return an error
     */

    memset(nameBuff, -1, TEST_NET_HOSTNAMEBUFFLEN); /*  init result  */
    result = socketGetHostName(nameP, TEST_NET_HOSTNAMEBUFFLEN);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    Host name is: '%s'\n", nameP);
    CuAssert(tc, "vte_test_socketGetHostName() failed", 0 == result);

    /**
     * \assertion Passing valid parameters returns the hostname
     */

    CuAssert(tc,
            "vte_test_socketGetHostName() didn't write hostname to the buffer",
            1 <= strlen(nameP));

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_socketGetHostName finished.\n\n");
}

/**
 * Tests the socketGetPeerName() function
 *
 * @param tc pointer to test case structure
 */
static void vte_test_socketGetPeerName(CuTest *tc)
{
    mpe_SocketSockAddr sockAddr;
    int size = sizeof(mpe_SocketSockAddr);
    int32_t serverFd;
    int32_t recvFd;
    mpe_SocketIPv4SockAddr server_addr;
    int port;
    int iServerSize = sizeof(server_addr);
    mpe_Error ec;
    int32_t result;
    mpe_ThreadId tid = 0;
    ThreadData td;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_socketGetPeerName\n");

    /*
     ** Set up the values needed for the connect thread.
     */

    port = getUniquePort();
    td.port = port;
    td.tc = tc;

    /*
     ** Initalize the global limit to allow the main thread to know that the 
     ** connect thread failed to accomplish its task. 
     */
    gConnectLimitFlag = FALSE;

    /*
     ** Create a condition to correct possible thread issues from the 
     ** connect thread execution
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
        CuAssert(tc, "Test Inconclusive - socketAccept() failed", recvFd > 0);
        recvFd = 0;
    }
    else
    {
        /*
         ** Call the socketGetPeerName, test for success then cleanup.
         */
        result = socketGetPeerName(recvFd, &sockAddr,
                (mpe_SocketSockLen *) &size);

        /*
         ** Clean up the socket
         */
        socketClose(serverFd);
        serverFd = 0;
        recvFd = 0;
        /* 
         ** Assert here to allow the socket to be cleaned up
         ** before exiting on a fail.
         */
        CuAssert(tc, "Test Failed - socketGetPeerName() failed", result != -1);

    }
    /*  wait for the connect thread to exit   */

    testNet_waitForThread();

    /*
     ** Fail the test if the connect limit was reached without the accept being exercised.
     */
    if (gConnectLimitFlag)
    {
        CuFail(tc,
                "Test Inconclusive - Connect limit reached without accept.\n");
    }
    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_socketGetPeerName finished.\n\n");
}

/**
 * Tests the socketGetSockName() function
 *
 * @param tc pointer to test case structure
 */
static void vte_test_socketGetSockName(CuTest *tc)
{
    /*
     ** Create the parameters to pass to socketGetSockName
     */
    mpe_SocketSockAddr sockAddr;
    int size = sizeof(mpe_SocketSockAddr);

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

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_socketGetSockName\n");

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

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "Waiting on socket to connect...\n");

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
        CuAssert(tc, "Test Inconclusive - socketAccept() failed", recvFd > 0);
        recvFd = 0;
    }
    else
    {
        /*
         ** Test socketGetSockName()
         */
        result = socketGetSockName(recvFd, &sockAddr,
                (mpe_SocketSockLen *) &size);
        /*
         ** Clean up the socket
         */
        socketClose(serverFd);
        serverFd = 0;
        recvFd = 0;

        /*
         ** Assert here to allow for the socket to be cleaned up 
         ** before exiting on a fail.
         */
        CuAssert(tc, "Test Failed - socketGetSockName() failed", result != -1);

    }
    /* 
     ** Wait for the connect thread to return.
     */

    testNet_waitForThread();

    /*
     ** Fail the test if the connect limit was reached without the accept being exercised.
     */
    if (gConnectLimitFlag)
    {
        CuFail(tc,
                "Test Inconclusive - Connect limit reached without accept.\n");
    }
    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_socketGetSockName finished.\n\n");
}

/****************************************************************************
 *
 *
 *  Internal utility functions, called by test code.
 *
 */

#if 0 /* TODO: unused function */
/****************************************************************************
 *
 *  'testNet_whatEndian() - determines and prints endian-ness of target machine
 *
 ****/
static void testNet_whatEndian(void)
{
    uint32_t test32 = 0x01020304;
    uint8_t *bp;

    bp = (uint8_t*)&test32;

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "test32 == %08x, bp[0] == %d, bp[1] == %d, bp[2] == %d, bp[3] == %d\n",
            test32, bp[0], bp[1], bp[2], bp[3]);

    if ((0x01==bp[0])&&(0x02==bp[1])&&(0x03==bp[2])&&(0x04==bp[3]))
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,"  STB is BigEndian\n");
    else if ((0x01==bp[3])&&(0x02==bp[2])&&(0x03==bp[1])&&(0x04==bp[0]))
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,"  STB is LittleEndian\n");
    else
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,"  STB is neither BigEndian or LittleEndian !\n");
    return;
}
#endif

/****************************************************************************

 An incredibly life-like reproduction of 'htonl()'

 ****/

static uint32_t testNet_htonl(uint32_t h)
{
    uint32_t netOrder;
    uint8_t *bp;

    bp = (uint8_t*) &netOrder;

    bp[0] = (h & 0xff000000U) >> 24;
    bp[1] = (h & 0x00ff0000U) >> 16;
    bp[2] = (h & 0x0000ff00U) >> 8;
    bp[3] = (h & 0x000000ffU);

    return (netOrder);
}

/****************************************************************************

 An incredibly life-like reproduction of 'htons()'

 ****/

static uint16_t testNet_htons(uint16_t h)
{
    uint16_t netOrder;
    uint8_t *bp;

    bp = (uint8_t*) &netOrder;

    bp[0] = (h & 0xff00U) >> 8;
    bp[1] = (h & 0x00ffU);

    return (netOrder);
}

/****************************************************************************

 An incredibly life-like reproduction of 'ntohl()'

 ****/

static uint32_t testNet_ntohl(uint32_t n)
{
    uint32_t hostOrder;
    uint8_t *bp;

    bp = (uint8_t*) &n;

    hostOrder = (bp[0] << 24) | (bp[1] << 16) | (bp[2] << 8) | bp[3];

    return (hostOrder);
}

/****************************************************************************

 An incredibly life-like reproduction of 'ntohs()'

 ****/

static uint16_t testNet_ntohs(uint16_t n)
{
    uint16_t hostOrder;
    uint8_t *bp;

    bp = (uint8_t*) &n;

    hostOrder = (bp[0] << 8) | bp[1];

    return (hostOrder);
}

/**
 * Create and return the test suite for the socket APIs tested in this suite.
 * @return a pointer to the new test suite.
 */
CuSuite* getTestSuite_netLookup(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, vte_test_socketAtoN);
    SUITE_ADD_TEST(suite, vte_test_socketAtoN_neg);
    SUITE_ADD_TEST(suite, vte_test_socketNtoA);
    SUITE_ADD_TEST(suite, vte_test_socketHtoNL);
    SUITE_ADD_TEST(suite, vte_test_socketHtoNS);
    SUITE_ADD_TEST(suite, vte_test_socketNtoHL);
    SUITE_ADD_TEST(suite, vte_test_socketNtoHS);
    SUITE_ADD_TEST(suite, vte_test_socketNtoP);
    SUITE_ADD_TEST(suite, vte_test_socketPtoN);
    SUITE_ADD_TEST(suite, vte_test_socketGetHostByAddr);
    SUITE_ADD_TEST(suite, vte_test_socketGetHostByName);
    SUITE_ADD_TEST(suite, vte_test_socketGetHostName);
    SUITE_ADD_TEST(suite, vte_test_socketGetPeerName);
    SUITE_ADD_TEST(suite, vte_test_socketGetSockName);

    return suite;
}
