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
 * VTE_CuTest.cpp
 * CableLabs Test Environment CuTest extension library
 *
 * This contains functions to extend the CuTest library. The CuTest library
 * doesn't contain any functionality to free memory, and a method to clone
 * a single test from a test suite. There are many more examples, we can
 * extend this for our liking.
 *
 * @date 10/10/2003
 * @author Sone Filipo
 */
#include <cutest.h>
#include <VTE_CuTest.h>
#include <string.h>
#include <stdlib.h>
#include <mpe_sys.h>
#include "test_sys.h"

/**
 * VTE_CuTestFindTest will find the given test and return a pointer to CuTest*.
 * It will be up to the caller to launch the test in a suite.
 *
 * @param suite The test suite that contains testName.
 * @param testName The name of the test to be run.
 *
 * @return a pointer to the CuTest, else NULL if not found.
 */
extern CuTest* VTE_CuTestFindTest(CuSuite* suite, char* testName)
{
    CuTest* test;
    CuTest* tmp;
    int count;
    // Error checking
    if (((CuSuite*) NULL == suite) || ((char*) NULL == testName))
    {
        return (CuTest*) NULL;
    }

    test = (CuTest*) NULL; // init to NULL, in case test isn't found

    // Parse all CuTest entries in the CuSuite until you find the testName.

    for (count = 0; count < suite->count; ++count)
    {
        tmp = suite->list[count];
        if (0 == strcmp(tmp->name, testName))
        {
            test = tmp;
            break;
        }
    } // end for

    // If testName was not found in CuSuite then 'test' is still NULL

    return test;
} // end VTE_CuTestFindTest(CuSuite*,char*)


/**
 * Given a test suite, "suite", search for the test "testName" and create a new
 * test suite with it. Clone that test and create a brand new suite.
 *
 * @param suite The test suite that contains testName as a test.
 * @param testName The test name that you want to clone.
 *
 * @return The suite with the cloned test, else NULL.
 */
extern CuSuite* VTE_CuSuiteNewCloneTest(CuSuite* suite, char* testName)
{
    CuTest* test;
    CuTest* cloneTest;
    CuSuite* newSuite;

    // Error check
    if (((CuSuite*) NULL == suite) || ((char*) NULL == testName))
    {
        return (CuSuite*) NULL;
    }

    test = VTE_CuTestFindTest(suite, testName);
    if ((CuTest*) NULL == test)
    {
        return (CuSuite*) NULL;
    }

    // Allocate the memory and clone the found test.
    cloneTest = CU_ALLOC(CuTest);
    if ((CuTest*) NULL == cloneTest)
    {
        return (CuSuite*) NULL;
    }

    // Clone test.
    if (NULL == memcpy(cloneTest, test, sizeof(CuTest)))
    {
        CU_FREE(cloneTest);
        return (CuSuite*) NULL;
    }

    cloneTest->name = CuStrCopy(test->name);

    // Create a new CuSuite and add the cloned test to it.
    newSuite = CuSuiteNew();
    CuSuiteAdd(newSuite, cloneTest);

    return newSuite;
} // end VTE_CuSuiteNewCloneTest(CuSuite*,char*)

/**
 * VTE_CuSuiteRunTestCase
 * For a given test suite, look for the test name, and run it.
 *
 * @param func A function point that will construct the suite info.
 * @param testname The name of the test contained in the suite info.
 *
 * @return Will return the new CuSuite info that will need to be cleaned
 * by the user, else NULL on error.
 */
extern CuSuite* VTE_CuSuiteRunTestCase(CuSuite* (*func)(void), char* testname)
{
    CuSuite* tmpSuite;
    CuSuite* suite;

    if ((NULL == func) || ((char*) NULL == testname))
    {
        return (CuSuite*) NULL;
    }

    tmpSuite = func();
    suite = VTE_CuSuiteNewCloneTest(tmpSuite, testname);
    VTE_CuSuiteFree(tmpSuite);

    if ((CuSuite*) NULL == suite)
    {
        VTE_CuSuiteFree(tmpSuite);
        return (CuSuite*) NULL;
    }

    CuSuiteRun(suite);

    return suite;
} // end VTE_CuSuiteRunTestCase(...)

extern void VTE_CuSuiteOutcome(CuSuite* suite, char* outcome, int outcomeLen)
{
    CuString * output;
    char errorMsg[] = "error getting outcome";

    // Error check
    if (((CuSuite*) NULL == suite) || ((char*) NULL == outcome))
    {
        return;
    }

    // Get outcome of test run.
    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    // Error
    if (strlen(output->buffer) >= (unsigned int) outcomeLen)
    {
        outcome[0] = '\0';
        if (strlen(errorMsg) < (unsigned int) outcomeLen)
        {
            strcpy(outcome, errorMsg);
        }
        return;
    }

    strcpy(outcome, output->buffer);
    CuStringFree(output);
} // end VTE_CuSuiteOutcome(CuSuite*,char*)


/**
 * VTE_CuTestFree - Frees memory used by a CuTest test.
 *
 * @param test The test to delete.
 * @return nothing.
 */

extern void VTE_CuTestFree(CuTest* test)
{
    if ((CuTest*) NULL == test)
        return;

    VTE_CuStrFree((char*) test->name);
    CU_FREE(test);
} // end VTE_CuTestFree(CuTest*)


/**
 * VTE_CuSuiteFree - Frees memory used by a CuTest suite and all the test
 *                   cases in it.
 *
 * WARNING : Be careful using this when using "CuSuiteAddSuite()", which
 *           makes copies of pointers to test cases. It's possible to free
 *           the same memory more than once if you're not careful.
 *
 * @param test The suite to delete.
 * @return nothing.
 */

extern void VTE_CuSuiteFree(CuSuite* suite)
{
    int count;

    TRACE(MPE_LOG_TRACE4, MPE_MOD_TEST, "VTE_CuSuiteFree() - suite at %p, ",
            suite);

    if ((CuSuite*) NULL == suite)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "VTE_CuSuiteFree() - NULL suite pointer\n");
        return;
    }
    else
    {
        TRACE(MPE_LOG_TRACE4, MPE_MOD_TEST, "%d tests in suite\n", suite->count);
    }

    for (count = 0; count < suite->count; ++count)
    {
        TRACE(MPE_LOG_TRACE4, MPE_MOD_TEST, "  Free test at %p\n",
                suite->list[count]);
        VTE_CuTestFree(suite->list[count]);
    }
    TRACE(MPE_LOG_TRACE4, MPE_MOD_TEST, "  Freeing suite memory %p, ", suite);

    CU_FREE(suite);
} // end VTE_CuSuiteFree(CuSuite*)


/**
 * VTE_CuStrFree - Frees memory.
 *
 * @param str Pointer to memory to free
 * @return nothing.
 */

extern void VTE_CuStrFree(char* str)
{
    if ((char*) NULL == str)
        return;

    CU_FREE(str);
} // end VTE_CuStrFree(char*)


/**
 * VTE_CuStringFree - Frees memory used by a CuTest string.
 *
 * @param str The CuString to delete.
 * @return nothing.
 */

extern void VTE_CuStringFree(CuString* str)
{
    if ((CuString*) NULL == str)
        return;

    VTE_CuStrFree(str->buffer);
    CU_FREE(str);
} // end VTE_CuStringFree(CuString*)


/****************************************************************************
 *
 *  VTE_CuSuiteDump() - prints diagnostic information about a CuSuite, does
 *                      some simple sanity checks on the suite.
 *
 *                      NOTE : All TRACE calls are done at level INFO because
 *                      if you're calling this function you probably want to
 *                      see everything it can tell you.
 */

extern void VTE_CuSuiteDump(CuSuite* suite)
{
    int count;
    int n;

    if ((CuSuite*) NULL == suite)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "VTE_CuSuiteDump() - NULL suite pointer\n");
        return;
    }
    else
    {
        TRACE(
                MPE_LOG_INFO,
                MPE_MOD_TEST,
                "VTE_CuSuiteDump() - Suite at %p, %d tests in suite. %d failures\n",
                suite, suite->count, suite->failCount);

        if ((n = suite->count) > 500)
        {
            n = 20;
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                    "  Test count looks bogus; only dumping first %d\n", n);

            TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                    "\n  Here's a hex dump before the suite struct :\n");
            VTE_CuHexDump(suite - 512, 512);

            TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                    "\n  Here's a hex dump of the begining of the suite struct :\n");
            VTE_CuHexDump(suite, 512);
        }

        for (count = 0; count < n; count++)
        {
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "  Test %2d at %p\n", count,
                    suite->list[count]);
        }
    }
}

/****************************************************************************
 *
 *  VTE_CuStringDump() - prints information about a CuString.
 */

extern void VTE_CuStringDump(CuString* str)
{
    char buf[100];

    if (NULL == str)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "VTE_CuStringDump() - NULL string pointer\n");
        return;
    }
    else
    {
        TRACE(
                MPE_LOG_INFO,
                MPE_MOD_TEST,
                "VTE_CuStringDump() - String at %p, length == %d, size == %d\n",
                str, str->length, str->size);
        strncpy(buf, str->buffer, 98);
        buf[98] = '\0';
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "    Begining of string buffer == '%s'\n\n", buf);

    }
}

/****************************************************************************
 *
 *  VTE_CuHexDump() - prints a hex dump of some memory
 */

#define HEXBUFLEN 100

extern void VTE_CuHexDump(void *p, int len)
{
    char buf[HEXBUFLEN + 2];
    unsigned char *cp;

    int i;
    int j;

    if (NULL == (cp = (unsigned char*) p))
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "VTE_CuHexDump() - NULL pointer\n");
        return;
    }
    else
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "VTE_CuHexDump() - dumping %d bytes at 0x%p\n", len, cp);
        while (len > 0)
        {
            memset(buf, ' ', HEXBUFLEN);
            buf[HEXBUFLEN - 1] = '\0';
            i = len > 16 ? 16 : len;
            for (j = 0; j < i; j++, cp++)
            {
                sprintf(&buf[j * 3], "%02x ", *cp);
                if (isprint(*cp))
                {
                    buf[50 + j] = *cp;
                }
                else
                {
                    buf[50 + j] = '.';
                }
            }
            buf[j * 3] = ' '; /* get rid of the last '\0' left by sprintf()  */
            buf[66] = '\0'; /* terminate string  */
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "  0x%p  %s\n", cp - i, buf);
            len -= i;
        }
    }
}

