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

/** \file test_sys_util.c
 *
 *  \brief Test functions for MPEOS utility functions
 *
 *  This file contains tests for the following MPEOS functions :\n
 *
 *    -# mpeos_envGet()\n
 *    -# mpeos_envInit()\n
 *    -# mpeos_envSet()\n
 *    -# mpeos_iniSetPath()\n
 *    -# mpeos_longJmp()\n
 *    -# mpeos_registerForPowerKey()\n
 *    -# mpeos_setJmp()\n
 *    -# mpeos_stbBoot()\n
 *    -# mpeos_stbBootStatus()\n
 *    -# mpeos_stbGetAcOutletState()\n
 *    -# mpeos_stbGetPowerStatus()\n
 *    -# mpeos_stbGetRootCerts
 *    -# mpeos_stbSetAcOutletState()\n
 *
 */

#ifdef TEST_SYS_MPEOS

// #define _SYS_UTIL                      /*!< a #define which appears to be unused */
// #define TEST_SYS_UTIL                  /*!< another #define which appears to be unused */

#include <cutest.h>
#include <mpe_sys.h>
#include <mpeos_util.h>
#include "test_sys.h"

#define MAGIC (0xB16B00B5)
#define STATUSMASK (MPE_BS_MPE_LOWLVL | MPE_BS_MPE_MGRS | MPE_BS_NET_MASK)

static void vte_test_util_envGet(CuTest*);
static void vte_test_util_envInit(CuTest*);
static void vte_test_util_envSet(CuTest*);
static void vte_test_util_iniSetPath(CuTest*);
static void vte_test_util_longJmp(CuTest*);
static void vte_test_util_registerForPowerKey(CuTest*);
static void vte_test_util_setJmp(CuTest*);
static void vte_test_util_stbBoot(CuTest*);
static void vte_test_util_stbBootStatus(CuTest*);
static void vte_test_util_stbGetAcOutletState(CuTest*);
static void vte_test_util_stbGetPowerStatus(CuTest*);
static void vte_test_util_stbGetRootCerts(CuTest*);
static void vte_test_util_stbSetAcOutletState(CuTest*);

CuSuite* getTestSuite_sysUtil(void);

/****************************************************************************
 *
 *  vte_test_util_envGet()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_envGet" function
 *
 * \api mpeos_()
 *
 * \strategy Call the "mpeos_envGet()" function and checks for reasonable
 * return values.
 *
 * \assets none
 *
 */

static void vte_test_util_envGet(CuTest *tc)
{
    const char *envString = "AbC";

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_util_envGet()\n");

    envInit();

    /**
     * \assertion NULL is returned if a NULL pointer is passed
     */
    /*
     TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  calling 'envGet()' with a NULL string\n");
     envString = envGet(NULL);
     CuAssert(tc, "envGet(NULL) failed to return NULL", NULL == envString);
     */
    /**
     * \assertion NULL is returned if the environment variable isn't found
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  calling 'envGet()' with a nonexistant environment variable\n");
    envString = envGet("sOmE/sTrInG-WhIcH!pRoBaBlY&wIlL;NoT^bE%fOuNd");
    CuAssert(tc, "envGet() failed to return NULL for unmatched environment variable", NULL == envString);

    /**
     * \assertion Valid pointer is returned if a valid env var is passed
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    calling 'envGet()' with 'MainClassArgs.0'\n");
    envString = envGet("MainClassArgs.0");
    if (envString == NULL)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "envGet() returned NULL for 'MainClassArgs.0'");
        CuFail(tc, "envGet() returned NULL for 'MainClassArgs.0'");
    }
    else
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "envGet() returned '%s' for 'MainClassArgs.0'", envString);
    }

    /**
     * \assertion Correct value of env var is returned
     */

    CuAssert(tc, "envGet() returned wrong value of environment variable", (0 == strcmp(envString, "org.cablelabs.impl.ocap.OcapMain")));

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_util_envGet finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_util_envInit()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_envInit" function
 *
 * \api mpeos_envInit()
 *
 * \strategy Call the "mpeos_envInit()" function and checks for reasonable
 * return values.
 *
 * \assets none
 *
 */

static void vte_test_util_envInit(CuTest *tc)
{

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_util_envInit()\n");

    /**
     * \assertion mpeos_envInit() can be called
     */

    envInit();

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_util_envInit finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_util_envSet()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_envSet" function
 *
 * \api mpeos_envInit()
 *
 * \strategy Call the "mpeos_envSet()" function and checks for reasonable
 * return values.
 *
 * \assets none
 *
 */

static void vte_test_util_envSet(CuTest *tc)
{
    //    mpe_Error ec;


    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_util_envSet()\n");

    /**
     * \assertion "MPE_EINVAL" is returned if a NULL pointer is passed
     */

    //    ec = envSet("a", 1);

    /**
     * \assertion
     *
     */

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_util_envSet finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_util_iniSetPath()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_iniSetPath" function
 *
 * \api mpeos_envInit()
 *
 * \strategy Call the "mpeos_iniSetPath()" function and checks for reasonable
 * return values.
 *
 * \assets none
 *
 */

static void vte_test_util_iniSetPath(CuTest *tc)
{
    //    mpe_Error ec;


    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_util_iniSetPath()\n");

    /**
     * \assertion "MPE_EINVAL" is returned if a NULL pointer is passed
     */

    /**
     * \assertion
     *
     */

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_util_iniSetPath finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_util_longJmp()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_setJmp()" & "mpeos_longJmp() functions
 *
 * \api mpeos_setJmp()
 * \api mpeos_longJmp()
 *
 * \strategy Call mpeos_setJmp(), then call mpeos_longJmp() and make sure
 *           it returns to the corrrect place with the correct values.
 *
 * \assets none
 *
 */
static int gFlag; /* this needs to be static */
static void vte_test_util_longJmp(CuTest *tc)
{
    mpe_JmpBuf jb;
    int ret;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Entered vte_test_util_longJmp\n");

    gFlag = 1; /* flag == 1 means first time through  */
    ret = setJmp(jb);
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "  Back from setJmp(), return value == %d, flag == %d\n", ret, gFlag);

    if (1 == gFlag) /* if flag says it's the first time through  */
    {
        /**
         * \assertion 'setJmp()' returns 0 when called directly
         */
        if (0 != ret) /* ret should be zero when called directly */
        {

            CuFail(tc, "setJmp() returned wrong value");
        }
        gFlag = 2; /* set gFlag to indicate longJmp was called  */
        // TODO: Figure our why 'longJmp()' reboots box
        //        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "  Calling longJmp(%p, %d), flag == %d\n", &jb, MAGIC, gFlag);
        //        longJmp(jb, MAGIC);
        //        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "  Oops! longJmp() returned!  (this should not happen)\n");
        /**
         * \assertion 'longJmp() doesn't return to the statement after the call to 'longJmp()'
         */
        //        CuFail(tc, "longJmp() returned to the wrong location");
    }
    else /* gFlag says we're back from longJmp() call */
    {
        /**
         * \assertion 'longJmp() returns to the statement after the call to 'setJmp()',
         *            with the value passed to 'longJmp()'.
         */
        //    CuAssert(tc, "  flag is corrupted", 2 == gFlag);  /* this should never, ever happen  */
        //    CuAssert(tc, "  longJmp() failed to return correct value", MAGIC == ret);
    }
}

/****************************************************************************
 *
 *  vte_test_util_registerForPowerKey()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_registerForPowerKey" function
 *
 * \api mpeos_registerForPowerKey()
 *
 * \strategy Call the "mpeos_registerForPowerKey()" function and checks for reasonable
 * return values.
 *
 * \assets none
 *
 */

static void vte_test_util_registerForPowerKey(CuTest *tc)
{
    mpe_Error ec;
    mpe_EventQueue evq;
    mpe_Event evId = (mpe_Event)-123;
    int act;
    int *actp;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_util_registerForPowerKey\n");

    /**
     * \assertion An event queue can be created
     */

    ec = eventQueueNew(&evq,"PowerKeyTestQueue");
    CuAssert(tc, "eventQueueNew(&q) failed", ec == MPE_SUCCESS);

    /**
     * \assertion "MPE_SUCCESS" is returned if valid parameters are passed
     */

    ec = registerForPowerKey(evq, (void*)&act);

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "\n\n    Press the POWER key on the STB\n\n");

    /**
     * \assertion Event is received if power key is pressed.
     */

    ec = eventQueueWaitNext(evq, &evId, NULL, (void*)&actp, NULL, 10000);

    if (MPE_ETIMEOUT == ec)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    Timed out waiting for power key, result is undetermined\n");
        CuFail(tc, "stbGregisterForPowerKey() timed out waiting for power key, result is undetermined\n");
    }
    else
    {
        if (evId == MPE_POWER_FULL)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    Received MPE_POWER_FULL event\n");
        }
        else if (evId == MPE_POWER_STANDBY)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    Received MPE_POWER_STANDBY event\n");
        }
        else
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    Received unknown event, value is %d\n", (int)evId);
            CuFail(tc, "Received unknown event\n");
        }
    }
    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_util_registerForPowerKey finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_util_stbBoot()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_stbBoot" function
 *
 * \api mpeos_stbBoot()
 *
 * \strategy Call the "mpeos_stbBoot()" function and checks for reasonable
 * return values.
 *
 * \assets none
 *
 */

static void vte_test_util_stbBoot(CuTest *tc)
{
    //    mpe_Error ec;


    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_util_stbBoot\n");

    /**
     * \assertion "MPE_EINVAL" is returned if a NULL pointer is passed
     */

    /**
     * \assertion
     *
     */

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_util_stbBoot finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_util_stbBootStatus()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_stbBootStatus" function
 *
 * \api mpeos_stbBootStatus()
 *
 * \strategy Call the "mpeos_stbBootStatus()" function and checks for reasonable
 * return values.
 *
 * \assets none
 *
 */

static void vte_test_util_stbBootStatus(CuTest *tc)
{
    uint32_t currentStatus;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_util_stbBootStatus\n");

    /**
     * \assertion Valid status is returned.
     */

    currentStatus = stbBootStatus(FALSE, 0, 0xffffffff);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    stbBootStatus() returned 0x%08x\n", currentStatus);
    CuAssert(tc, "stbBootStatus() returned invalid status", 0 == (currentStatus & (~STATUSMASK)));

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_util_stbBootStatus finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_util_setJmp()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_setJmp" function
 *
 * \api mpeos_setJmp()
 *
 * \strategy Call the "mpeos_setJmp()" function and checks for reasonable
 * return values.
 *
 * \assets none
 *
 */

static void vte_test_util_setJmp(CuTest *tc)
{
    mpe_JmpBuf jb;
    int ret;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_util_setJmp\n");

    /**
     * \assertion "setJmp" returns 0
     */

    ret = setJmp(jb);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "    setJmp() returned %d\n", ret);

    CuAssert(tc, "setJmp() returned invalid value, expected 0", 0 == ret);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_util_setJmp finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_util_stbGetAcOutletState()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_stbGetAcOutletState" function
 *
 * \api mpeos_()
 *
 * \strategy Call the "mpeos_stbGetAcOutletState()" function and checks for
 * valid return values.
 *
 * \assets none
 *
 */

static void vte_test_util_stbGetAcOutletState(CuTest *tc)
{
    mpe_Error ec;
    mpe_Bool state;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_util_stbGetAcOutletState\n");

    /**
     * \assertion "MPE_EINVAL" is returned if a NULL pointer is passed
     */

    ec = stbGetAcOutletState(NULL);
    CuAssert(tc, "stbGetAcOutletState(NULL) failed to return MPE_EINVAL as expected", MPE_EINVAL == ec);

    /**
     * \assertion "MPE_SUCCESS" is returned if a valid pointer is passed
     */

    ec = stbGetAcOutletState(&state);
    CuAssert(tc, "stbGetAcOutletState() failed to return MPE_SUCCESS as expected", MPE_SUCCESS == ec);

    /**
     * \assertion The returned state is either TRUE or FALSE
     */

    if (TRUE == state)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    stbGetAcOutletState() returned TRUE\n");
    }
    else if (FALSE == state)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    stbGetAcOutletState() returned FALSE\n");
    }
    else
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    stbGetAcOutletState() returned %d\n", (int)state);
        CuFail(tc, "stbGetAcOutletState() returned neither TRUE of FALSE\n");
    }
    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_util_stbGetAcOutletState finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_stbGetPowerStatus()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_stbGetPowerStatus" function
 *
 * \api mpeos_stbGetPowerStatus()
 *
 * \strategy Call the "mpeos_stbGetPowerStatus()" function and check that
 *           it indicates that the power status returned is valid.
 *
 * \assets none
 *
 */

static void vte_test_util_stbGetPowerStatus(CuTest *tc)
{
    mpe_PowerStatus power;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Entered vte_test_stbGetPowerStatus\n");

    power = stbGetPowerStatus();

    /**
     * \assertion Either "MPE_POWER_FULL" or "MPE_POWER_STANDBY" is returned.
     */

    power = stbGetPowerStatus();
    TRACE (MPE_LOG_DEBUG, MPE_MOD_TEST, "    stbGetPowerStatus() returned %d\n", power);
    CuAssert(tc, "stbGetPowerStatus() failed, neither On or Off.",
            power == MPE_POWER_FULL || power == MPE_POWER_STANDBY);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_stbGetPowerStatus finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_util_stbGetRootCerts()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_stbGetRootCerts" function
 *
 * \api mpeos_stbGetRootCerts()
 *
 * \strategy Call the "mpeos_stbGetRootCerts()" function and checks for reasonable
 * return values.
 *
 * \assets none
 *
 */

static void vte_test_util_stbGetRootCerts(CuTest *tc)
{
    mpe_Error ec;
    uint8_t *roots = (uint8_t*)-1;
    uint32_t len = -2;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_util_stbGetRootCerts\n");

    /**
     * \assertion "MPE_EINVAL" is returned if a NULL 'roots' pointer is passed
     */

    ec = stbGetRootCerts(NULL, &len);
    CuAssert(tc, "stbGetRootCerts(NULL, &len) failed to return MPE_EINVAL as expected",
            MPE_EINVAL == ec);

    /**
     * \assertion "MPE_EINVAL" is returned if a NULL 'len' pointer is passed
     */

    ec = stbGetRootCerts(&roots, NULL);
    CuAssert(tc, "stbGetRootCerts(&roots, NULL) failed to return MPE_EINVAL as expected",
            MPE_EINVAL == ec);

    /**
     * \assertion "MPE_SUCCESS" is returned if valid parameters are passed
     */

    ec = stbGetRootCerts(&roots, &len);
    CuAssert(tc, "stbGetRootCerts(&roots, &len) failed to return MPE_SUCCESS as expected",
            MPE_SUCCESS == ec);
    TRACE (MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  stbGetRootCerts() returned length == %d, certs at %p\n", len, roots);

    /**
     * \assertion Reasonable values are returned if valid parameters are passed
     */

    CuAssert(tc, "stbGetRootCerts(&roots, &len) returned an invalid length",
            1 < len && 65565 > len);

    CuAssert(tc, "stbGetRootCerts(&roots, &len) returned an invalid root certs pointer",
            NULL != roots && (uint8_t*)-1 != roots);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_util_stbGetRootCerts finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_util_stbSetAcOutletState()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_stbSetAcOutletState" function
 *
 * \api mpeos_stbSetAcOutletState()
 *
 * \strategy Call the "mpeos_stbSetAcOutletState()" function to set the
 *           state to TRUE and verifys the return value, then calls
 *           "mpeos_stbGetAcOutletState()" and verifies that it returns TRUE.
 *
 *           Call the "mpeos_stbSetAcOutletState()" function to set the
 *           state to FALSE and verifys the return value, then calls
 *           "mpeos_stbGetAcOutletState()" and verifies that it returns FALSE.
 *
 *           Toggle the AC outlet state between TRUE and FALSE several times,
 *           with delays so that an operator can verify that something plugged
 *           into the outlet turns on and off.
 *
 * \assets none
 *
 */

static void vte_test_util_stbSetAcOutletState(CuTest *tc)
{
    mpe_Error ec;
    mpe_Bool state;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_util_stbSetAcOutletState\n");

    /**
     * \assertion "MPE_SUCCESS" is returned if TRUE is passed
     */

    ec = stbSetAcOutletState(TRUE);
    CuAssert(tc, "stbSetAcOutletState() failed to return MPE_SUCCESS as expected", MPE_SUCCESS == ec);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    AC outlet turned ON\n", (int)state);

    /**
     * \assertion AC outlet state is now TRUE
     */

    ec = stbGetAcOutletState(&state);
    CuAssert(tc, "stbGetAcOutletState(TRUE) failed to return MPE_SUCCESS as expected", MPE_SUCCESS == ec);
    CuAssert(tc, "AC outlet state is not TRUE as expected", TRUE == state);

    threadSleep(1000,0);

    /**
     * \assertion "MPE_SUCCESS" is returned if FALSE is passed
     */

    ec = stbSetAcOutletState(FALSE);
    CuAssert(tc, "stbSetAcOutletState(FALSE) failed to return MPE_SUCCESS as expected", MPE_SUCCESS == ec);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    AC outlet turned OFF\n", (int)state);

    /**
     * \assertion AC outlet state is now FALSE
     */

    ec = stbGetAcOutletState(&state);
    CuAssert(tc, "stbGetAcOutletState() failed to return MPE_SUCCESS as expected", MPE_SUCCESS == ec);
    CuAssert(tc, "AC outlet state is not FALSE as expected", FALSE == state);

    /*  Now toggle the outlet on and off again  */

    threadSleep(1000,0);

    ec = stbSetAcOutletState(TRUE);
    CuAssert(tc, "stbSetAcOutletState() failed to return MPE_SUCCESS as expected", MPE_SUCCESS == ec);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    AC outlet turned ON\n", (int)state);

    ec = stbGetAcOutletState(&state);
    CuAssert(tc, "stbGetAcOutletState(TRUE) failed to return MPE_SUCCESS as expected", MPE_SUCCESS == ec);
    CuAssert(tc, "AC outlet state is not TRUE as expected", TRUE == state);

    threadSleep(1000,0);

    ec = stbSetAcOutletState(FALSE);
    CuAssert(tc, "stbSetAcOutletState(FALSE) failed to return MPE_SUCCESS as expected", MPE_SUCCESS == ec);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    AC outlet turned OFF\n", (int)state);

    ec = stbGetAcOutletState(&state);
    CuAssert(tc, "stbGetAcOutletState() failed to return MPE_SUCCESS as expected", MPE_SUCCESS == ec);
    CuAssert(tc, "AC outlet state is not FALSE as expected", FALSE == state);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_util_stbSetAcOutletState finished.\n\n");
}

/****************************************************************************
 *
 *  getTestSuite_sysUtil
 *
 * Create and return the test suite for the utility APIs
 *
 */

CuSuite* getTestSuite_sysUtil(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, vte_test_util_envGet);
    SUITE_ADD_TEST(suite, vte_test_util_envInit);
    SUITE_ADD_TEST(suite, vte_test_util_envSet);
    SUITE_ADD_TEST(suite, vte_test_util_iniSetPath);
    SUITE_ADD_TEST(suite, vte_test_util_longJmp);
    SUITE_ADD_TEST(suite, vte_test_util_registerForPowerKey);
    SUITE_ADD_TEST(suite, vte_test_util_setJmp);
    SUITE_ADD_TEST(suite, vte_test_util_stbBoot);
    SUITE_ADD_TEST(suite, vte_test_util_stbBootStatus);
    SUITE_ADD_TEST(suite, vte_test_util_stbGetAcOutletState);
    SUITE_ADD_TEST(suite, vte_test_util_stbGetPowerStatus);
    SUITE_ADD_TEST(suite, vte_test_util_stbGetRootCerts);
    SUITE_ADD_TEST(suite, vte_test_util_stbSetAcOutletState);

    return suite;
}

#endif /* TEST_SYS_MPEOS */
