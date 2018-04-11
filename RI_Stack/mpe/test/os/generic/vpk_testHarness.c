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

/**
 * GenericTestHarness.c
 *
 * File containing the test runner and tools for retreiving status.
 */
#include <vpk_testHarness.h>
#include <stdarg.h>
#include <stdio.h>
#include <string.h>
#ifdef NO_VTE_AGENT
#  include <mpe_types.h>
#endif /*  NO_VTE_AGENT */

/**
 * Global or Static variables **
 *
 * m_sStatus  Private container variable used for storing test status.
 */
static char* m_pcStatus = NULL;
static int m_iStatusSize = 0;
static int m_iIsUsingStatus = 0;

/**
 * Prototypes **
 *
 * vte_agent_Log(const char*...)
 *  Override logging so it's saved in m_sStatus.
 * GenericTestHarness( char*,int,char** )
 *  Will be the main test runner for launching tests.  Arg one is for launch
 *  the test, arg two is the size of arg three, arg three is status.
 */
static int vpk_StartTests(void** aFunctions, int size);

#ifdef NO_VTE_AGENT
/**
 * vte_agent_Log exists as part of the vte_agent and stubed out for this test
 * harness.  This function will be used to fill the status string sent to us
 * by the end user.
 *
 * Note: In vte_agent.h, vte_agent_Log returns mpe_Bool.  But since we wish to
 * create a non-dependant function, we do not want to use mpe_* types or calls
 * for this test runner.  If mpe_Bool type changs, you will need to change
 * this.
 *
 * @param format  A format similar to printf.
 * @param ...     Stuff to fill up the format.
 * @return Will return TRUE if successful, else FALSE.
 */
extern mpe_Bool vte_agent_Log( const char* format, ... )
{
    char myStr[GTH_MAX_STRLEN];

    int iCopySize = 0;
    int iMyStrSize = 0;
    int iStatusSize = 0;

    va_list arg;

    if( 0 == m_iIsUsingStatus )
    {
        /* Not using status do nothing when logging is called.
         */
        return TRUE;
    }

    va_start( arg, format );
    vsprintf( myStr, format, arg );
    va_end( arg );

    /* Don't copy more "stuff" then you have space for.
     */
    iMyStrSize = strlen( myStr );
    iStatusSize = strlen( m_pcStatus );

    /* Assuming m_pcStatus was NULL filled before using it.
     */
    if( (iMyStrSize + iStatusSize) < m_iStatusSize )
    {
        /* No need to truncate string, there should be enough room.
         */
        if( NULL == strcat( m_pcStatus, myStr ) )
        {
            return FALSE;
        }
    }
    else
    {
        /* Not enough room to store all of the message, trunc message.
         */
        iCopySize = m_iStatusSize - iMyStrSize + iStatusSize;

        if( NULL == strncat( m_pcStatus, myStr, iCopySize-1 ) )
        {
            return FALSE;
        }
    }

    return TRUE;
}
#endif /* NO_VTE_AGENT */

/**
 * vpk_StartTests **
 * Given an array of function pointers.  This function will start the tests.
 * @param aFunctions The array of functions.  Each function is a test.
 * @param size       The number of elements in aFunction
 * @return  Will return 0 on failure, or 1 if tests pass.
 */
static int vpk_StartTests(void** aFunctions, int size)
{
    /* All the tests return void.  The pointers are of type "void (*)(void)".
     * If that changes, this function should change.
     */
    int count;
    for (count = 0; count < size; ++count)
    {
        /*     (void (*)(void)) aFunctions[count]; */
        ((VPK_TEST_FUNC_POINTER_CAST) aFunctions[count])();
    }

    return 1;
} /* end vpk_StartTests(void**,int) */

/**
 * vpk_TestRunner **
 * This is just like vpk_TestHarness, except it's for running only 1 test.
 * If you plan on running more then 1 test, use vpk_TestHarness.
 * @param testID The test you want to run.
 * @param buffer The string to contains the log messages.
 * @param bufferSize The size of the buffer.
 */
extern int vpk_TestRunner(TestID testID, char** buffer, int bufferSize)
{
    TestID aTestID[1];
    aTestID[0] = testID;
    return vpk_TestHarness(aTestID, 1, buffer, bufferSize);
}

/**
 * vpk_TestHarness **
 * There are two ways to run the vpk_TestHarness; run the tests with
 * status output, and to run tests with out status output.  To run the tests
 * harness with out maintaining status output, send NULL as ppcStatus, and
 * 0 as iStatusLen.
 * An enumeration contains a list of all the available tests to run, located
 * in the GenericTestHarness.h header file.
 *
 * @param aTestID    The test id you wish to run.
 * @param iTestLen   The length of the TestID array.
 * @param ppStatus   Contains the status of the test run.
 * @param iStatusLen The amount of memory alloced to store status.
 *
 * @return  Will return -1 if vpk_TestHarness encountered an error,
 *   0 if test failed, or 1 if test passed.
 */
extern int vpk_TestHarness(TestID* aTestID, int iTestLen, char** ppcStatus,
        int iStatusLen)
{
    void** aFunctions;
    int iResult = 0;
    int count = 0;

    /* Error checker
     */
    if (0 == iStatusLen)
    {
        m_iIsUsingStatus = 0;
    }
    else if (0 < iStatusLen)
    {
        m_iIsUsingStatus = 1;
        if (NULL == ppcStatus)
        {
            /* If iStatusLen is greater then 0, then ppcStatus > 0.
             */
            return -1;
        }

        m_pcStatus = *ppcStatus;
        memset(m_pcStatus, (int) '\0', iStatusLen);
        m_iStatusSize = 0;
    }

    if (NULL == aTestID)
    {
        /* Error aTestID is NULL.
         */
        return -1;
    }

    /* Run test, test will update status using vte_agent_Log() function. */
    aFunctions = malloc(iTestLen * sizeof(TestID));
    for (count = 0; count < iTestLen; ++count)
    {
        aFunctions[count] = vpk_AddTest(aTestID[count]);
    }

    iResult = vpk_StartTests(aFunctions, iTestLen);
    free(aFunctions);

    return iResult;
} /* end vpk_TestHarness(TestID,int,char**,int) */
