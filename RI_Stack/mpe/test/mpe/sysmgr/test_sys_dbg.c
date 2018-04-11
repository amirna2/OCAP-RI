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

/** \file test_sys_dbg.c
 *
 *  \brief Test functions for "mpeos_dbgMsg()" function
 *
 *  This file contains tests for the "mpeos_dbgMsg()" function\n
 */

// #define VERBOSE 0                      /*!< print verbose TRACE messages if TRUE */

#include "test_dbg.h"
#include "test_sys.h"

#ifdef WIN32
# define MAXTESTMSGLEN  0x3d55c    /*!< for testing on Windows, max value is 0x3d55c */
#else
# define MAXTESTMSGLEN  0x40001    /*!< Length of long test message - 2**18 + 1 */
#endif /* WIN32 */

#define DBGTESTCOUNT   7           /*!< number of test cases  */
#define LINELEN        78          /*!< test line length  */
#define MSGINTERVAL    1023        /*!< Print progress message after this many chars  */
#define FMTPAD         3           /*!< Format string grows by three chars  */
#define BUFFPAD        20          /*!< Extra space to avoid buffer overrun  */
#define LASTCHAR       0x7e        /*!< last printable 7 bit ASCII char  */

CuSuite* getTestSuite_sysDbg(void);

/*
 * Test functions
 */

static void vte_test_dbgMsg1234(CuTest *);
static void vte_test_dbgMsg5(CuTest *);
static void vte_test_dbgMsg67(CuTest *);

/****************************************************************************
 *
 *  vte_test_dbgMsg1234()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_dbgMsg" function 
 *
 * \api mpeos_dbgMsg()
 *
 * \strategy Call the "mpeos_dbgMsg()" function with a variety of messages,
 *           some valid, some bogus. Verify that messages are printed as
 *           expected, and that no crashes occur. There are seven tests total.
 *           Each test prints a message by calling "mpeos_dbgMsg()", with a
 *           final summary message at the end. All seven test case messages
 *           messages and the summary message, which says "dbgMsg() tests
 *           complete" should print.
 *
 * \assets  --none--
 *
 */

static void vte_test_dbgMsg1234(CuTest *tc)
{

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\nEntering 'vte_test_dbgMsg1234()\n");

    /**
     * \assertion Nothing weird happens if a NULL format pointer is passed.
     *            The following test case doesn't crash and prints : 
     *
     *  "(1) NULL format. Single line, nothing between left [] and right brackets."
     */

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "(1) NULL format. Single line, nothing between left [");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, NULL);
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "] and right brackets.\n");

    /**
     * \assertion Handles "normal" 'printf()' format string tokens - %c, %d, %x, %s, 
     */

    TRACE(
            MPE_LOG_INFO,
            MPE_MOD_TEST,
            "\n(2) Normal format string.\n      - This should print '12345' - '%d'\n\
      - This should print '377' - '%o'\n      - This should print 'ab' - '%x'\n\
      - This should print 'CD' - '%X' \n      - This should print 'W' - '%c'\n\
      - This should print 'W0GK' - '%s'\n",
            12345, 255, 0xab, 0xcd, 'W', "W0GK");

    /**
     * \assertion Nothing weird happens if more arguments are passed than
     *            have matching tokens in the format string.
     */

    TRACE(
            MPE_LOG_INFO,
            MPE_MOD_TEST,
            "\n(3) The number 2 - %d, a word 'encyclopedia' - '%s', the letter 'Q' - '%c', end of message.\n",
            2, "encyclopedia", 'Q',
            /* the rest of this stuff should not print */
            1, "This should not print", 987654321, 'Z',
            "Another string which should not print");

#if 0  /* This test case crashes on the SA 3250 and the Mot 6412. Maybe it should . . . */

    /**
     * \assertion Nothing too weird happens if fewer arguments are passed than
     *            have matching tokens in the format string. It may print
     *            garbage, but should not crash.
     */

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\n(4) This test may print garbage but should not crash. At the end of\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "    the test line you should see the number 7, the word 'armadillo',\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "    and the letter 'G', possibly followed by some garbage.\n\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "    Test line :  %d, '%s', %c, %d, %d, %s, %c\n", 7, "armadillo", 'G');
#else
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n # # # #  (4) Test case 4 was not run (it crashes).\n\n");
#endif  /* This test case crashes. */

    return;
} /* end of 'vte_test_dbgMsg123()' */

/****************************************************************************
 *
 *  vte_test_dbgMsg5()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_dbgMsg" function 
 *
 * \api mpeos_dbgMsg()
 *
 * \strategy Call the "mpeos_dbgMsg()" function with a long format string
 *           (>2k bytes), with a large number (>100) of string args. Verify
 *           that the text prints as expected, and that no crashes occur.
 *
 * \assets "independence.h"
 */

void vte_test_dbgMsg5(CuTest *tc)
{
    char *bigFormat;
    char lineFormat[] = "========  [ %s ]  ========\n";
    char msgbuf[100];

    unsigned int lineCount;
    unsigned int charCount = 0;
    unsigned int i;

    mpe_Error err;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Entering 'vte_test_dbgMsg5()\n");

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "(5) This test prints a bunch of lines of text using a long format string.\n");

    lineCount = sizeof inde / sizeof(char*); /* initial line count */
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "initial line count = %d\n\n", lineCount);
    for (i = 0; i < lineCount; i++)
    {
        if (inde[i] == NULL)
            break;
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "Line == %3d, len == %2d, char count == %4d\n", i, strlen(
                        inde[i]), charCount);
        charCount += strlen(inde[i]);
    }
    lineCount = i; /* update line count, without NULL  */
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "\n\n%d lines, %d chars\n", lineCount,
            charCount);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "    The final line printed by this test should be :\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, lineFormat, inde[lineCount - 1]);
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\n");

    if ((err = memAllocP(MPE_MEM_TEST, (lineCount * (strlen(lineFormat) + 2))
            + 10, (void*) &bigFormat)) != MPE_SUCCESS)
    {
        sprintf(
                msgbuf,
                "'vte_test_dbgMsg5()' - can't allocate %d bytes for test buffer\n",
                lineCount * (strlen(lineFormat) + 2) + 10);
        CuFail(tc, msgbuf);
        return;
    }

    bigFormat[0] = '\0';
    for (i = 0; i < lineCount; i++)
        strcat(bigFormat, lineFormat);

    TRACE(
            MPE_LOG_DEBUG,
            MPE_MOD_TEST,
            "\nFormat string length == %d chars, line count == %d, total char count == %d\n\n",
            strlen(bigFormat), lineCount, (strlen(bigFormat) - (lineCount * 2))
                    + charCount);

    /**
     * \assertion 'dbgMsg(MPE_LOG_INFO, MPE_MOD_TEST, ) correctly handles a long format string (>2k bytes),
     *             with a large number (>100) of string args.
     *             NOTE : This requires manual evaluation of console output.
     */

    /*
     *  WARNING : The following has hardcoded knowledge of the number of lines
     *            in 'inde[]'.
     */

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, bigFormat, inde[0], inde[1], inde[2],
            inde[3], inde[4], inde[5], inde[6], inde[7], inde[8], inde[9],
            inde[10], inde[11], inde[12], inde[13], inde[14], inde[15],
            inde[16], inde[17], inde[18], inde[19], inde[20], inde[21],
            inde[22], inde[23], inde[24], inde[25], inde[26], inde[27],
            inde[28], inde[29], inde[30], inde[31], inde[32], inde[33],
            inde[34], inde[35], inde[36], inde[37], inde[38], inde[39],
            inde[40], inde[41], inde[42], inde[43], inde[44], inde[45],
            inde[46], inde[47], inde[48], inde[49], inde[50], inde[51],
            inde[52], inde[53], inde[54], inde[55], inde[56], inde[57],
            inde[58], inde[59], inde[60], inde[61], inde[62], inde[63],
            inde[64], inde[65], inde[66], inde[67], inde[68], inde[69],
            inde[70], inde[71], inde[72], inde[73], inde[74], inde[75],
            inde[76], inde[77], inde[78], inde[79], inde[80], inde[81],
            inde[82], inde[83], inde[84], inde[85], inde[86], inde[87],
            inde[88], inde[89], inde[90], inde[91], inde[92], inde[93],
            inde[94], inde[95], inde[96], inde[97], inde[98], inde[99],
            inde[100], inde[101], inde[102], inde[103], inde[104], inde[105],
            inde[106], inde[107], inde[108], inde[109], inde[110], inde[111],
            inde[112], inde[113], inde[114], inde[115], inde[116], inde[117],
            inde[118], inde[119], inde[120], inde[121], inde[122], inde[123],
            inde[124], inde[125], inde[126], inde[127], inde[128], inde[129],
            inde[130], inde[131], inde[132], inde[133], inde[134], inde[135],
            inde[136], inde[137], inde[138], inde[139], inde[140]);

    if ((err = memFreeP(MPE_MEM_TEST, &bigFormat)) != MPE_SUCCESS)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST, "  error freeing test buffer\n");
    }

    return;
} /* end of 'vte_test_dbgMsg3()'  */

/****************************************************************************
 *
 *  vte_test_dbgMsg67()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_dbgMsg" function 
 *
 * \api mpeos_dbgMsg()
 *
 * \strategy Allocate a large buffer, fill with text, with a periodic message
 *           which says "\n\nChar count == %d\n\n", with a final message at
 *           the end. Verify that all the text prints as expected, and that
 *           no crashes occur.
 *
 * \assets  --none--
 */

/****************************************************************************

 
 dbg message test 6 - print the large buffer with "dbgMsg(bigBuffer)".

 dbg message test 7 - same as #5, except print with "dbgMsg("%s", bigBuffer)".

 **/

void vte_test_dbgMsg67(CuTest *tc)
{

    char *bigBuffer;
    char *cp;
    char testchar;
    char fmt[] = "\n\nByte count == %6d\n\n";
    char eot[] = "\n\nEnd of large buffer test.\n";
    char msgbuf[100];

    int i;
    size_t bufLen;
    mpe_Error err;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "\nInto 'vte_test_dbgMsg67()\n");

    bufLen = MAXTESTMSGLEN + ((strlen(fmt) + FMTPAD) * ((MAXTESTMSGLEN
            / MSGINTERVAL) + 1)) + BUFFPAD;

    if ((err = memAllocP(MPE_MEM_TEST, bufLen, (void*) &bigBuffer))
            != MPE_SUCCESS)
    {
        sprintf(
                msgbuf,
                "'vte_test_dbgMsg5()' - can't allocate %d bytes for test buffer\n",
                bufLen);
        CuFail(tc, msgbuf);
        return;
    }

    cp = bigBuffer;
    testchar = ' ';

    for (i = 0, testchar = ' '; i < MAXTESTMSGLEN; i++)
    {
        *cp++ = testchar++;
        if (testchar > LASTCHAR)
            testchar = ' ';
        if ((i % MSGINTERVAL) == 0)
        {
            sprintf(cp, fmt, i); /* WARNING : Make sure BUFFPAD extra bytes  */
            i += strlen(fmt) + FMTPAD; /*      are allocated for buffer, to avoid  */
            cp += strlen(fmt) + FMTPAD; /*      buffer overrun here.                */
        }
        else if ((i % LINELEN) == 0)
        {
            *cp++ = '\n';
            i++;
        }
    }

    strcpy(&bigBuffer[MAXTESTMSGLEN - strlen(eot) - 1], eot);

    TRACE(
            MPE_LOG_TRACE1,
            MPE_MOD_TEST,
            "\nBuffer at 0x%08x, %ld chars in buffer, end of buffer at 0x%08x\n",
            bigBuffer, strlen(bigBuffer), bigBuffer + strlen(bigBuffer));

    /**
     * \assertion 'dbgMsg() correctly handles a very long (256kb) string.
     *             NOTE : This requires manual evaluation of console output.
     */

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n(6) - Print a bunch of text with a single call - 'dbgMsg(bigBuffer)'\n");

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, bigBuffer);

    /**
     * \assertion 'TRACE() correctly handles a very long (256kb) string.
     *             NOTE : This requires manual evaluation of console output.
     */

    TRACE(
            MPE_LOG_INFO,
            MPE_MOD_TEST,
            "\n(7) - Print a bunch of text with a single call - 'dbgMsg('%%s', bigBuffer)'\n");

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s", bigBuffer);

    /* Tests are done, cleanup and print final message */

    if ((err = memFreeP(MPE_MEM_TEST, &bigBuffer)) != MPE_SUCCESS)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST, "  error freeing test buffer\n");
    }

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "\n\n'dbgMsg() tests complete.\n\n");

} /* end of 'vte_test_dbgMsg67()' */

/****************************************************************************
 *
 *  getTestSuite_sysDbg
 *
 * Create and return the test suite for the dbg API.
 *
 */

CuSuite* getTestSuite_sysDbg(void)
{
    CuSuite* suite = CuSuiteNew();

    //  SUITE_ADD_TEST(suite, vte_test_dbgMsg1234);
    SUITE_ADD_TEST(suite, vte_test_dbgMsg5);
    SUITE_ADD_TEST(suite, vte_test_dbgMsg67);

    return suite;
}
