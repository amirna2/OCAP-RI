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
 * These tests rely that you have copied over the new headend simulator data
 * file located at //project/RI_Stack/qa/OCAPNativeTest/assets/headend/TestVCT.dat
 * to the headend simulator vct.dat file in the bin directory. Otherwise the
 * settings will not match, and the tests will fail.
 */

#include <test_si.h>

extern CuSuite* getTestSuite_siAPI(void);
extern CuSuite* getTestSuite_siNeg(void);

/*
 * Private variables to file test_mpe_si_all.cpp
 */
/*
 static mpeos_SiElement m_channelZero;
 static mpeos_SiElement m_channelOne;
 static mpeos_SiElement m_channelTwo;
 static mpe_Bool m_isChannelZeroInit = false;
 static mpe_Bool m_isChannelOneInit = false;
 static mpe_Bool m_isChannelTwoInit = false;
 static mpe_Bool m_isSIInit = false;
 */

/**
 * SITest_Init requires updating when the service source id, or program number,
 * or any other service value changes. They are hard coded because I haven't
 * spent the time to figure out a more automated method of fetching the SI
 * elements from the headend simulator.
 *
 * @return Will return TRUE on a successful init, else FALSE.
 */
static mpe_Bool SITest_Init(void)
{
    /*
     // Win32 Simulator should only take 5 sec to connect to headend, 60 for STB.
     #ifdef WIN32
     int iWaitForHeadend = 6000; // 6 seconds
     #else
     int iWaitForHeadend = 60000; // 60 seconds
     #endif

     if( true == m_isSIInit )
     return TRUE;

     TRACE( "STTest_Init: Wait for %d sec to connect with headend.\n",
     (int)(iWaitForHeadend / 1000) );

     threadSleep( iWaitForHeadend, 0 );

     if( true != m_isChannelZeroInit )
     {
     m_channelZero.serviceChannelIndex;
     m_channelZero.serviceSourceId = 500;
     m_channelZero.serviceProgramNumber = 1;
     m_channelZero.serviceMinorChannelNumber = 801;
     m_channelZero.serviceMajorChannelNumber = 800;
     m_channelZero.serviceTsId = 0;
     m_channelZero.serviceFrequency = 99;
     m_channelZero.serviceModulationMode = 1;
     m_channelZero.serviceType = 1;
     m_channelZero.servicePmtPid = 100;
     m_channelZero.servicePcrPid = 121;
     m_channelZero.servicePMTVersionNumber = 5;

     m_channelZero.serviceNumComponents = 2;
     if( MPE_SUCCESS !=
     memAllocP(MPE_MEM_TEST,  sizeof(mpeos_SiServiceComponent)
     * m_channelZero.serviceNumComponents,
     (void**)(&m_channelZero.serviceComponents) ) )
     {
     // Error
     return FALSE;
     }

     m_channelZero.serviceComponents[0].streamType = 0;
     m_channelZero.serviceComponents[0].pid = 1;

     m_channelZero.serviceComponents[1].streamType = 3;
     m_channelZero.serviceComponents[1].pid = 2;
     } // end init m_isChannelZeroInit

     if( true != m_isChannelOneInit )
     {
     m_channelOne.serviceChannelIndex;
     m_channelOne.serviceSourceId = 501;
     m_channelOne.serviceProgramNumber = 2;
     m_channelOne.serviceMinorChannelNumber = 811;
     m_channelOne.serviceMajorChannelNumber = 810;
     m_channelOne.serviceTsId = 44;
     m_channelOne.serviceFrequency = 100;
     m_channelOne.serviceModulationMode = 2;
     m_channelOne.serviceType = 1;
     m_channelOne.servicePmtPid = 101;
     m_channelOne.servicePcrPid = 122;
     m_channelOne.servicePMTVersionNumber = 6;

     m_channelOne.serviceNumComponents = 3;
     if( MPE_SUCCESS !=
     memAllocP(MPE_MEM_TEST,  sizeof(mpeos_SiServiceComponent)
     * m_channelOne.serviceNumComponents,
     (void**)&(m_channelOne.serviceComponents) ) )
     {
     // Error
     return FALSE;
     }

     m_channelOne.serviceComponents[0].streamType = 1;
     m_channelOne.serviceComponents[0].pid = 11;

     m_channelOne.serviceComponents[1].streamType = 3;
     m_channelOne.serviceComponents[1].pid = 12;

     m_channelOne.serviceComponents[2].streamType = 5;
     m_channelOne.serviceComponents[2].pid = 13;
     }  // end init m_isChannelOneInit

     if( true != m_isChannelTwoInit )
     {
     m_channelTwo.serviceChannelIndex;
     m_channelTwo.serviceSourceId = 502;
     m_channelTwo.serviceProgramNumber = 3;
     m_channelTwo.serviceMinorChannelNumber = 821;
     m_channelTwo.serviceMajorChannelNumber = 820;
     m_channelTwo.serviceTsId = 45;
     m_channelTwo.serviceFrequency = 101;
     m_channelTwo.serviceModulationMode = 3;
     m_channelTwo.serviceType = 2;
     m_channelTwo.servicePmtPid = 102;
     m_channelTwo.servicePcrPid = 123;
     m_channelTwo.servicePMTVersionNumber = 7;

     m_channelTwo.serviceNumComponents = 4;
     if( MPE_SUCCESS !=
     memAllocP(MPE_MEM_TEST,  sizeof(mpeos_SiServiceComponent)
     * m_channelTwo.serviceNumComponents,
     (void**)&(m_channelTwo.serviceComponents) ) )
     {
     // Error
     return FALSE;
     }

     m_channelOne.serviceComponents[0].streamType = 1;
     m_channelOne.serviceComponents[0].pid = 21;

     m_channelOne.serviceComponents[1].streamType = 3;
     m_channelOne.serviceComponents[1].pid = 22;

     m_channelOne.serviceComponents[2].streamType = 7;
     m_channelOne.serviceComponents[2].pid = 23;

     m_channelOne.serviceComponents[3].streamType = 6;
     m_channelOne.serviceComponents[3].pid = 24;
     } // end init m_isChannelTwoInit

     m_isSIInit = true;
     */
    return TRUE;
} // end SITest_Init()

/**
 * SITest_Destroy will clean up anything created by SITest_Init.
 *
 * @return TRUE on success, else FALSE.
 */
static mpe_Bool SITest_Destroy(void)
{
    /*
     if( true == m_isChannelZeroInit )
     {
     memFreeP(MPE_MEM_TEST,  (void*) m_channelZero.serviceComponents );
     m_isChannelZeroInit = false;
     }

     if( true == m_isChannelOneInit )
     {
     memFreeP(MPE_MEM_TEST,  (void*) m_channelOne.serviceComponents );
     m_isChannelOneInit = false;
     }

     if( true == m_isChannelTwoInit )
     {
     memFreeP(MPE_MEM_TEST,  (void*) m_channelTwo.serviceComponents );
     m_isChannelTwoInit = false;
     }

     m_isSIInit = false;
     */

    return TRUE;
} // end SITest_Destroy()

/**
 * Will return channel zero's element information that was created on init.
 */
/*
 extern mpeos_SiElement* SITest_GetChannelZero()
 {
 return &m_channelZero;
 }
 */

/**
 * Will return channel one's element information that was created on init.
 */
/*
 extern mpeos_SiElement* SITest_GetChannelOne()
 {
 return &m_channelOne;
 }
 */

/**
 * Will return channel two's element information that was created on init.
 */
/*
 extern mpeos_SiElement* SITest_GetChannelTwo()
 {
 return &m_channelTwo;
 }
 */

/**
 * Entry point to launch all the SI tests.
 */
void test_siRunAllTests()
{
    CuSuite* pSuite;
    CuString* output;

    // Should be first thing this function does.
    if (FALSE == SITest_Init())
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "ERROR test_siRunAllTests: Could not init SI test.\n");
        return;
    }

    pSuite = CuSuiteNew();
    CuSuiteAddSuite(pSuite, getTestSuite_siAPI());
    CuSuiteAddSuite(pSuite, getTestSuite_siNeg());
    CuSuiteRun(pSuite);

    output = CuStringNew();
    CuSuiteSummary(pSuite, output);
    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);

    /* Should be last thing this funtion does.  */

    if (FALSE == SITest_Destroy())
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "ERROR test_siRunAllTests: Could not destroy SI stuff.\n");
    }
} // end test_siRunAllTests()

/**
 * Run the API tests for the SI stuff.
 */
void test_siRunAPITests()
{
    CuSuite* pSuite;
    CuString* output;

    /* Should be first thing this function does. */
    if (FALSE == SITest_Init())
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "ERROR test_siRunAPITests: Could not init SI test.\n");
        return;
    }

    pSuite = CuSuiteNew();
    CuSuiteAddSuite(pSuite, getTestSuite_siAPI());
    CuSuiteRun(pSuite);

    output = CuStringNew();
    CuSuiteSummary(pSuite, output);
    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);

    /* Should be last thing this funtion does. */

    if (FALSE == SITest_Destroy())
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "ERROR test_siRunAPITests: Could not destroy SI stuff.\n");
    }
} // end test_siRunAPITests()

/**
 * test_siRunNegTests runs all the negative tests.
 */
void test_siRunNegTests()
{
    CuSuite* pSuite;
    CuString* output;

    /* Should be first thing this function does. */
    if (FALSE == SITest_Init())
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "ERROR test_mpe_siRunNegTests: Could not init SI test.\n");
        return;
    }

    pSuite = CuSuiteNew();
    CuSuiteAddSuite(pSuite, getTestSuite_siNeg());
    CuSuiteRun(pSuite);

    output = CuStringNew();
    CuSuiteSummary(pSuite, output);
    CuSuiteDetails(pSuite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);

    /* Should be last thing this funtion does. */

    if (FALSE == SITest_Destroy())
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "ERROR test_mpe_siRunNegTests: Could not destroy SI stuff.\n");
    }
} // end test_siRunNegTests()
