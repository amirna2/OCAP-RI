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

/** \file Template.c
 *
 *  \brief Test functions for some OCAP stuff
 *
 *  This file contains tests for the following MPEOS functions :\n
 *    -# mpeos_blah1(),\n
 *    -# mpeos_blah2(),\n
 *    -# mpeos_blah3().\n
 *
 * \par
 * New paragraph under the same heading. Put more stuff here, if you need
 * to, or remove this if you don't need it.
 *
 * \author Your Name Here
 *
 *  <b>Template Users Note :\n</b>
 *  AFTER USING AS TEMPLATE FOR A TEST SOURCE FILE, REMOVE THIS SECTION
 *  DOWN TO THE LINE WHICH SAYS "END TEMPLATE USERS NOTE".
 *
 *  <b>Brief usage instructions :\n</b>
 *
 *  Document each test function, as in the examples below. Each test
 *  function should test one MPEOS function. A single test function
 *  may run a single test, or may run multiple tests on the same MPEOS
 *  function. Each test function should have a '\\test' tag, a
 *  '\\strategy' tag and one or more '\\assert' tags. If a test
 *  function runs multiple tests, then each test case should have it's
 *  own '\\assert'.
 *
 *  To generate docs using Doxygen, you will need a Doxygen config file
 *  which defines the custom tags used here :
 *    - '\\testdescription
 *    - '\\api'
 *    - '\\strategy'
 *    - '\\assets'
 *    - '\\requirement'
 *    - '\\assertion'
 *
 *  These new tags are defined in the config file using the ALIASES command,
 *  like this :
 *
 *  ALIASES  = "assert=\par Test assertion:\n" "strategy=\par Test strategy:\n" \
 *              "api=\par API Tested:\n" "assets=\par Test assets used:\n"
 *
 *  
 *  Put other template user information here.
 *
 *  <b>END TEMPLATE USERS NOTE\n</b>
 *
 * \note
 * Put a NOTE here if you need to add some kind of note in the finished
 * documentation, or remove this if you don't need it.
 *
 */

#include <test_Something.h>
#include <SomeOtherHeaderFile.h>

/****************************************************************************
 *
 *  vte_test_DescriptiveTestName1()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_blah1()" function with a variety of legal
 * and illegal input values.
 *
 * \api mpeos_blah1()
 *
 * \strategy Call the "mpeos_blah1()" function with various legal
 * and illegal input values and and check for expected results.
 *
 * \assets none
 *
 */

void vte_test_DescriptiveTestName1(CuTest* tc)
{
    int retval;

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_TEST,
            "Entering 'vte_test_DescriptiveTestName1()'\n");

    /**
     * \requirement SRS 3.2.1 - "MPE_EINVAL" shall be returned if invalid
     * parameters are passed.
     */

    /**
     * \assertion "MPE_EINVAL" is returned if a NULL pointer is passed
     */
    retval = mpeos_blah1(NULL);
    CuAssertIntEquals_Msg(tc, "mpeos_blah1(NULL) failed", MPE_EINVAL, retval);

    /**
     * \assertion "MPE_EINVAL" is returned if argument is too large
     */
    retval = mpeos_blah1(LARGETESTVALUE);
    CuAssertIntEquals_Msg(tc, "mpeos_blah1(LARGETESTVALUE) failed", MPE_EINVAL,
            retval);

    /**
     * \assertion "MPE_EINVAL" is returned if argument is too small
     */
    retval = mpeos_blah1(SMALLTESTVALUE);
    CuAssertIntEquals_Msg(tc, "mpeos_blah1(SMALLTESTVALUE) failed", MPE_EINVAL,
            retval);
}

/****************************************************************************
 *
 *  vte_test_DescriptiveTestName2()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_blah2()" function with a variety of legal
 * and illegal input values.
 *
 * \api mpeos_blah2()
 *
 * \strategy Call the "mpeos_blah2()" function with various legal
 * and illegal input values and and check for expected results.
 *
 * \assets none
 *
 */

void vte_test_DescriptiveTestName2(CuTest* tc)
{
    int retval;

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_TEST,
            "Entering 'vte_test_DescriptiveTestName2()'\n");

    /**
     * \requirement SRS 3.2.2 - "MPE_EINVAL" shall be returned if invalid
     * parameters are passed.
     */

    /**
     * \assertion "MPE_EINVAL" is returned if a NULL pointer is passed
     */
    retval = mpeos_blah2(NULL);
    CuAssertIntEquals_Msg(tc, "mpeos_blah2(NULL) failed", MPE_EINVAL, retval);

    /**
     * \assertion "MPE_EINVAL" is returned if argument is too large
     */
    retval = mpeos_blah2(LARGETESTVALUE);
    CuAssertIntEquals_Msg(tc, "mpeos_blah2(LARGETESTVALUE) failed", MPE_EINVAL,
            retval);

    /**
     * \assertion "MPE_EINVAL" is returned if argument is too small
     */
    retval = mpeos_blah2(SMALLTESTVALUE);
    CuAssertIntEquals_Msg(tc, "mpeos_blah2(SMALLTESTVALUE) failed", MPE_EINVAL,
            retval);
}

