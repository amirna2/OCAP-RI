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

#if defined(WIN32)
#  include <mpe_dbg.h> // DEBUG: While MPE not supported by PowerTV.
#elif defined(POWERTV)
#  include <mpeos_dbg.h> // DEBUG: While MPEOS not supported by Win32.
#endif

#include <cutest.h>
#include <vte_agent.h>

static void test_myTestTemplate(CuTest *tc)
{
    CuAssert(tc, "Hello World from my test template", 0 == 0);

}

CuSuite* test_myTestSuite(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, test_myTestTemplate);
    return suite;
}

#if 0
NATIVEEXPORT_API void test_TemplateRunAll()
{
    CuSuite* pSuite = CuSuiteNew();

    // Define suite
    CuSuiteAddSuite( pSuite, test_myTestSuite() );

    // Run the suite
    CuSuiteRun( pSuite );

    // Get output
    CuString* output = CuStringNew();
    CuSuiteSummary(pSuite, output);
    CuSuiteDetails(pSuite, output);

#if defined(WIN32)
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "%s\n", output->buffer ); // DEBUG While MPE not supported by PowerTV.
#elif defined(POWERTV)
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "%s\n", output->buffer ); // DEBUG: While MPEOS not supported by Win32.
#endif

    vte_agent_Log( "Test results: test_TemplateRunAll...\n%s\n",
            output->buffer );
}

#endif
void ptvtest_TemplateRunAll()
{
    CuString* output;
    CuSuite* pSuite = CuSuiteNew();

    // Define suite
    CuSuiteAddSuite(pSuite, test_myTestSuite());

    // Run the suite
    CuSuiteRun(pSuite);

    // Get output
    output = CuStringNew();
    CuSuiteSummary(pSuite, output);
    CuSuiteDetails(pSuite, output);

    //mpeos_sys_init();
#if defined(WIN32)
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "%s\n", output->buffer ); // DEBUG While MPE not supported by PowerTV.
#elif defined(POWERTV)
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "%s\n", output->buffer ); // DEBUG: While MPEOS not supported by Win32.
#endif
}
