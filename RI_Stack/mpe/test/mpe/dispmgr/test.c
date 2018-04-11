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

#include "test_disp.h"
#include "vte_agent.h"

extern CuSuite* getTestSuite_gfxColor(void);
extern CuSuite* getTestSuite_gfxSurface(void);
extern CuSuite* getTestSuite_gfxContext(void);
extern CuSuite* getTestSuite_gfxDraw(void);
extern CuSuite* getTestSuite_gfxFont(void);
extern CuSuite* getTestSuite_gfxEvent(void);
extern CuSuite* getTestSuite_gfxSmoke(void);
extern CuSuite* getTestSuite_gfxfontfact(void);
extern CuSuite* getTest_DrawPolygon(void);

/**
 * Runs the given CuSuite.
 */
void test_gfxRunSuite(CuSuite* suite, char* name)
{
    CuString* output;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "#\n#  'test_gfxRunSuite()' - running '%s'\n#\n", name);

    CuSuiteRun(suite);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Reporting results");

    output = CuStringNew();
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    fflush( stdout);
    fflush( stderr);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "\n# '%s()' finished :\n\n%s\n", name,
            output->buffer);
    fflush(stdout);
    fflush(stderr);

}

/**
 * Defines and returns a CuSuite specifying all of the GFX tests.
 */
static CuSuite* getTestSuite_gfx(void)
{
    CuSuite *suite = CuSuiteNew();

    //   CuSuiteAddSuite(suite, getTestSuite_gfxEvent());

    CuSuiteAddSuite(suite, getTestSuite_gfxColor());
    CuSuiteAddSuite(suite, getTestSuite_gfxSurface());
    CuSuiteAddSuite(suite, getTestSuite_gfxContext());
    CuSuiteAddSuite(suite, getTestSuite_gfxDraw());

    CuSuiteAddSuite(suite, getTestSuite_gfxFont());
    CuSuiteAddSuite(suite, getTestSuite_gfxfontfact());
    CuSuiteAddSuite(suite, getTestSuite_gfxSmoke());

    return suite;
}

/**
 * Runs all of the GFX tests.
 */
NATIVEEXPORT_API void test_mpeos_gfxRunAllTests(void)
{
    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "gfxRunAllTests: pre GFX_INIT...");

    //	This is temporary - need to find out what does need initalization
    //  mpeos_dispInit();  
    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "gfxRunAllTests: post GFX_INIT...");

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "gfxRunAllTests: running tests...");
    test_gfxRunSuite(getTestSuite_gfx(), "MPE-GFX");
}

