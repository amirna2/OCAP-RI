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

/** \file test_cc_all.c
 *
 *  \brief Test MPE functions related to closed captioning support
 *
 *  This file contains tests for the following MPE functions :\n
 *    -# mpe_Error mpe_ccSetAttributes()
 *    -# mpe_ccSetAttributes()
 *    -# mpe_ccGetAttributes()
 *    -# mpe_ccSetAnalogServices()
 *    -# mpe_ccGetAnalogServices()
 *    -# mpe_ccSetDigitalServices()
 *    -# mpe_ccGetDigitalServices()
 *    -# mpe_ccSetClosedCaptioning()
 *    -# mpe_ccGetAvailableServices()
 *    -# mpe_ccGetCapability()
 *    -# mpe_ccGetClosedCaptioning()
 *
 * \author Amir Nathoo, Ric Yeates - Vidiom Systems, Inc.
 *
 * \strategy     This test suite exercices all MPE/MPEOS Closed Captioning (CC) APIs
 *				 Some tests will require visual checking of expected result.
 *				 Specifically, when CC text display is or is not expected.
 *				 The tests will also require that Analog and Digital 
 *               closed captioning streams to be available from the Headend.
 *
 ************************************************************************************
 * \verbatim
 *
 * HOW TO RUN THESE TESTS 
 *
 * For PowerTV
 *
 * 1.	Build the OCAP stack for PowerTV Debug
 * 2.	Re-flash your set-top-box with an ocap_image-dev.rom
 * 3.  from output terminal : 
 *     Launch MPE				PTV>launch mpe.ptv
 *     Switch to HD screen		PTV>vdm so 1 4 1
 *     Tune to analog channel  PTV>tvtest
 *                                 1. tv_SetChannel (select an analog channel)
 *     For digital cc test     tune to a digital channel with DTV CC
 *
 *     At this point Video should be playing on the tv screen
 *
 * For Motorola
 * 1.	Modify mpeos_media.c to do a gotoChan(<valid channel>) at the end of
 *      spoofSIInfo()
 * 2.   Modify DirectFB's mot_interface.c to paint the graphics plane
 *      100% transparent orange.
 * 3.   Build appropriately so DirectFB and MPE rebuild. The resulting
 *		mpetest_image.elf should start up and tune to the channel of your
 *      choice.
 * 4.   Execute the tests in the order shown below. NOTE: I (rry) was never
 * 		able to get CC text to actually appear on the screen.
 *
 * Following test suites do not require any interventions
 * But have to run in this order:
 * vpk_suite_ccGetAttributes
 * vpk_suite_ccGetDigitalServices
 * vpk_suite_ccSetDigitalServices
 * vpk_suite_ccGetAnalogServices
 * vpk_suite_ccSetAnalogServices
 * vpk_suite_ccAttributes
 *
 * following tests require visual verification
 *
 * vpk_suite_ccSetClosedCaptioningStateOn		// Check if closed caption is shown
 * vpk_suite_ccSetClosedCaptioningStateOff		// Check if closed caption is hidden
 * vpk_suite_ccSetClosedCaptioningStateOnMute	// Check if closed caption is on when audio is muted
 * \endverbatim
 *
 */

// #include <OCAPNativeTest.h> // To export RunAllTests function.

#include "test_caption.h"

typedef struct cctype_t
{
    mpe_CcType type;
    mpe_Bool valid;
    mpe_CcAttributes def_attrib;
} cctype_t;

typedef struct map_t
{
    char *str;
    uint32_t val;
} map_t;

static mpe_Bool CCTest_Init(void);
static mpe_Bool CCTest_Destroy(void);

static mpe_Bool CheckCCService(mpe_CcType type);
static void checkAttributes(CuTest *tc, mpe_CcType type,
        const mpe_CcAttributes *expected);
static mpe_Bool translate(const char *str, const map_t *map,
        uint32_t * const val);
static mpe_Bool tryAttribute(CuTest *tc, mpe_CcAttribType attrType);

/* All the CC Test suites */
CuSuite* vpk_suite_ccSetAttributes(void);
CuSuite* vpk_suite_ccGetAttributes(void);
CuSuite* vpk_suite_ccSetDigitalServices(void);
CuSuite* vpk_suite_ccGetDigitalServices(void);
CuSuite* vpk_suite_ccSetAnalogServices(void);
CuSuite* vpk_suite_ccGetAnalogServices(void);
CuSuite* vpk_suite_ccSetClosedCaptioningOn(void);
CuSuite* vpk_suite_ccSetClosedCaptioningOff(void);
CuSuite* vpk_suite_ccSetClosedCaptioningOnMute(void);

/*
 * Private variables to file test_mpe_cc_all.c
 */
static mpe_Bool m_isCCInit = false;

static mpe_CcAnalogServiceMap as;
static mpe_CcDigitalServiceMap ds;

#define CCTYPE_SIZE 2
static cctype_t cctype[CCTYPE_SIZE] =
{
{ MPE_CC_TYPE_ANALOG },
{ MPE_CC_TYPE_DIGITAL } };

static const map_t opacityMap[] =
{
{ "transparent", MPE_CC_OPACITY_TRANSPARENT },
{ "translucent", MPE_CC_OPACITY_TRANSLUCENT },
{ "solid", MPE_CC_OPACITY_SOLID },
{ "flashing", MPE_CC_OPACITY_FLASHING }, };

static const map_t styleMap[] =
{
{ "default", MPE_CC_FONT_STYLE_DEFAULT },
{ "monospacedserif", MPE_CC_FONT_STYLE_MONOSPACED_SERIF },
{ "proportionalserif", MPE_CC_FONT_STYLE_PROPORTIONAL_SERIF },
{ "monospacedsansserif", MPE_CC_FONT_STYLE_MONOSPACED_SANSSERIF },
{ "proportionalsansserif", MPE_CC_FONT_STYLE_PROPORTIONAL_SANSSERIF },
{ "casual", MPE_CC_FONT_STYLE_CASUAL },
{ "cursive", MPE_CC_FONT_STYLE_CURSIVE },
{ "smallcapital", MPE_CC_FONT_STYLE_SMALL_CAPITALS }, };

static const map_t sizeMap[] =
{
{ "small", MPE_CC_FONT_SIZE_SMALL },
{ "standard", MPE_CC_FONT_SIZE_STANDARD },
{ "large", MPE_CC_FONT_SIZE_LARGE }, };

static const map_t boolMap[] =
{
{ "false", MPE_CC_TEXT_STYLE_FALSE },
{ "true", MPE_CC_TEXT_STYLE_TRUE }, };

static const map_t borderMap[] =
{
{ "none", MPE_CC_BORDER_TYPE_NONE },
{ "raised", MPE_CC_BORDER_TYPE_RAISED },
{ "depressed", MPE_CC_BORDER_TYPE_DEPRESSED },
{ "uniform", MPE_CC_BORDER_TYPE_UNIFORM },
{ "shadowleft", MPE_CC_BORDER_TYPE_SHADOW_LEFT },
{ "shadowright", MPE_CC_BORDER_TYPE_SHADOW_RIGHT }, };

/**
 * \brief translate a string value into the MPE_CC macro value
 *
 * \param str string to find in the map
 * \param map map to find the string within
 * \param val location to fill with the MPE_CC macro value
 *
 * \return TRUE on a successful translation, else FALSE.
 */
static mpe_Bool translate(const char *str, const map_t *map,
        uint32_t * const val)
{
    while (map->str)
    {
        if (strcasecmp(map->str, str) == 0)
        {
            *val = map->val;
            return TRUE;
        }
        ++map;
    }
    return FALSE;
}

/**
 * \brief Initialize the cc testing engine
 *
 * \return TRUE on a successful init, else FALSE.
 */
static mpe_Bool CCTest_Init(void)
{
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "CCTest_Init() was called.\n");
    m_isCCInit = true;
    return TRUE;
}

/**
 * \brief CCTest_Destroy will clean up anything created by CCTest_Init.
 *
 * \return TRUE on success, else FALSE.
 */
static mpe_Bool CCTest_Destroy(void)
{
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "CCTest_Destroy() was called.\n");
    m_isCCInit = false;
    return TRUE;
}

/**
 * \brief checks if the analog/digital cc service is set correctly
 * Given a type of service to check (analog or digital), it compares what
 * the live API returns against the global variables that hold the correct
 * service map.
 *
 * \param type the type of service to check (digital or analog)
 *
 * \return TRUE on success, else FALSE.
 *
 */
mpe_Bool CheckCCService(mpe_CcType type)
{
    mpe_Bool ret = FALSE;

    switch (type)
    {
    case MPE_CC_TYPE_ANALOG:
    {
        mpe_CcAnalogServiceMap s1 = 0;
        ccGetAnalogServices(&s1);
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "<<CC_TEST>> CheckCCService() returns Analog CC %d\n", s1);
        ret = (s1 == as);
    }
        break;

    case MPE_CC_TYPE_DIGITAL:
    {
        mpe_CcDigitalServiceMap s2 = 0;
        ccGetDigitalServices(&s2);
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "<<CC_TEST>> CheckCCService() returns Digital CC %d\n", s2);
        ret = (s2 == ds);
    }
        break;

    default:
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "<<CC_TEST>> CheckCCService() called w/ bad type %d\n", type);
    }
        break;
    }

    return ret;
}

/**
 * \brief creates ccGetAttributes() test suite
 *
 * \return suite for testing mpe_ccGetAttributes
 *
 */
CuSuite* vpk_suite_ccGetAttributes(void)
{
    CuSuite* suite = CuSuiteNew();

    /* get the default attributes */
    SUITE_ADD_TEST(suite, vpk_test_ccGetAttributes1);

    /* test the NULL address case */
    SUITE_ADD_TEST(suite, vpk_test_ccGetAttributes2);

    return suite;
}

/**
 * \brief creates ccSetAttributes() test suite
 *
 * \return suite for testing mpe_ccSetAttributes
 *
 */
CuSuite* vpk_suite_ccSetAttributes(void)
{
    CuSuite* suite = CuSuiteNew();

    /* test each attribute for each possible value */
    SUITE_ADD_TEST(suite, vpk_test_ccAttributes);

    return suite;
}

#if 0 /* TODO: unused function */
/**
 * \brief runs mpe_ccGetAttributes() test suite
 * Execute tests related to the getting and setting of attributes.
 *
 */
void vpk_run_ccGetAttributes(void)
{
    CuSuite* suite;
    CuString *output;

    CCTest_Init();

    suite = CuSuiteNew();

    CuSuiteAddSuite( suite, vpk_suite_ccGetAttributes());

    CuSuiteRun( suite );

    // Report results
    output = CuStringNew();
    CuSuiteDetails( suite, output );

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer );

    CCTest_Destroy();
}
#endif 

/**
 * \brief runs mpe_ccSetAttributes() test suite
 * Execute tests related to the setting of attributes.
 *
 */
void vpk_run_ccSetAttributes(void)
{
    CuSuite* suite;
    CuString *output;

    CCTest_Init();

    suite = CuSuiteNew();

    CuSuiteAddSuite(suite, vpk_suite_ccSetAttributes());

    CuSuiteRun(suite);

    // Report results
    output = CuStringNew();
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);

    CCTest_Destroy();
}

/**
 * \brief creates ccGetDigitalServices() test suite
 *
 * \return a pointer to the suite to test mpe_ccGetDigitalServices
 */
CuSuite* vpk_suite_ccGetDigitalServices(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, vpk_test_ccGetDigitalServices1);
    SUITE_ADD_TEST(suite, vpk_test_ccGetDigitalServices2);
    return suite;
}

/**
 * \brief runs GetDigitalServices() test suite
 *
 */
void vpk_run_ccGetDigitalServices(void)
{
    CuSuite* suite;
    CuString *output;

    CCTest_Init();

    suite = CuSuiteNew();

    CuSuiteAddSuite(suite, vpk_suite_ccGetDigitalServices());

    CuSuiteRun(suite);

    // Report results
    output = CuStringNew();
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);

    CCTest_Destroy();
}

/**
 * \brief creates ccSetDigitalServices() test suite
 *
 * \return suite of tests for mpe_ccSetDigitalServices
 */
CuSuite* vpk_suite_ccSetDigitalServices(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, vpk_test_ccSetDigitalServices1);
    return suite;
}

/**
 * \brief runs SetDigitalServices() test suite
 *
 */
void vpk_run_ccSetDigitalServices(void)
{
    CuSuite* suite;
    CuString *output;

    CCTest_Init();

    suite = CuSuiteNew();

    CuSuiteAddSuite(suite, vpk_suite_ccSetDigitalServices());

    CuSuiteRun(suite);

    // Report results
    output = CuStringNew();
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);

    CCTest_Destroy();
}

/**
 * \brief creates ccGetAnalogServices() test suite
 *
 * \return suite of tests for mpe_ccGetAnalogServices
 */
CuSuite* vpk_suite_ccGetAnalogServices(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, vpk_test_ccGetAnalogServices1);
    SUITE_ADD_TEST(suite, vpk_test_ccGetAnalogServices2);
    return suite;
}

/**
 * \brief runs GetAnalogServices() test suite
 *
 */
void vpk_run_ccGetAnalogServices(void)
{
    CuSuite* suite;
    CuString *output;

    CCTest_Init();

    suite = CuSuiteNew();

    CuSuiteAddSuite(suite, vpk_suite_ccGetAnalogServices());

    CuSuiteRun(suite);

    // Report results
    output = CuStringNew();
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);

    CCTest_Destroy();
}

/**
 * \brief creates ccSetAnalogServices() test suite
 *
 * \return suite of tests for mpe_ccSetAnalogServices
 */
CuSuite* vpk_suite_ccSetAnalogServices(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, vpk_test_ccSetAnalogServices1);
    return suite;
}

/**
 * \brief runs SetAnalogServices() test suite
 *
 */
void vpk_run_ccSetAnalogServices(void)
{
    CuSuite* suite;
    CuString *output;

    CCTest_Init();

    suite = CuSuiteNew();

    CuSuiteAddSuite(suite, vpk_suite_ccSetAnalogServices());

    CuSuiteRun(suite);

    // Report results
    output = CuStringNew();
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);

    CCTest_Destroy();
}

/**
 * \brief creates ccSetClosedCaptioningState() test suite : state = MPE_CC_STATE_ON
 *
 * \return pointer to tests for mpe_ccSetClosedCaptioning(ON)
 */
CuSuite* vpk_suite_ccSetClosedCaptioningOn(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, vpk_test_ccSetClosedCaptioningState1); // turn CC on
    return suite;
}

/**
 * \brief creates ccSetClosedCaptioningState() test suite : state = MPE_CC_STATE_OFF
 *
 * \return pointer to tests for mpe_ccSetClosedCaptioning(OFF)
 */
CuSuite* vpk_suite_ccSetClosedCaptioningOff(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, vpk_test_ccSetClosedCaptioningState2); // MPE_CC_STATE_OFF

    return suite;
}

/**
 * \brief creates ccSetClosedCaptioningState() test suite : state = MPE_CC_STATE_OFF
 *
 * \return pointer to tests for mpe_ccSetClosedCaptioning(ON_MUTE)
 */
CuSuite* vpk_suite_ccSetClosedCaptioningOnMute(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, vpk_test_ccSetClosedCaptioningState3); // MPE_CC_STATE_ON_MUTE

    return suite;
}

/**
 * \brief Runs SetClosedCaptioningStateOn() test suite
 *
 */
void vpk_run_ccSetClosedCaptioningStateOn(void)
{
    CuSuite* suite;
    CuString *output;

    CCTest_Init();

    suite = CuSuiteNew();

    CuSuiteAddSuite(suite, vpk_suite_ccSetClosedCaptioningOn());

    CuSuiteRun(suite);

    // Report results
    output = CuStringNew();
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);

    CCTest_Destroy();
}

/**
 * \brief runs SetClosedCaptioningStateOff() test suite
 *
 */
void vpk_run_ccSetClosedCaptioningStateOff(void)
{
    CuSuite* suite;
    CuString *output;

    CCTest_Init();

    suite = CuSuiteNew();

    CuSuiteAddSuite(suite, vpk_suite_ccSetClosedCaptioningOff());

    CuSuiteRun(suite);

    // Report results
    output = CuStringNew();
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);

    CCTest_Destroy();
}

/**
 * \brief runs SetClosedCaptioningStateOnMute() test suite
 *
 */
void vpk_run_ccSetClosedCaptioningStateOnMute(void)
{
    CuSuite* suite;
    CuString *output;

    CCTest_Init();

    suite = CuSuiteNew();

    CuSuiteAddSuite(suite, vpk_suite_ccSetClosedCaptioningOnMute());

    CuSuiteRun(suite);

    // Report results
    output = CuStringNew();
    CuSuiteDetails(suite, output);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer);

    CCTest_Destroy();
}

/*****************************************************************************/
/* MPE/MPEOS Closed Captioning API test cases                                */
/*****************************************************************************/

static const mpe_CcAttributes def_expected =
{ MPE_CC_EMBEDDED_COLOR, MPE_CC_EMBEDDED_COLOR, MPE_CC_EMBEDDED_COLOR,
        MPE_CC_OPACITY_EMBEDDED, MPE_CC_OPACITY_EMBEDDED,
        MPE_CC_OPACITY_EMBEDDED, MPE_CC_FONT_SIZE_EMBEDDED,
        MPE_CC_FONT_STYLE_EMBEDDED, MPE_CC_TEXT_STYLE_EMBEDDED_TEXT,
        MPE_CC_TEXT_STYLE_EMBEDDED_TEXT, MPE_CC_BORDER_TYPE_EMBEDDED,
        MPE_CC_EMBEDDED_COLOR };

/**
 * \testdescription Test default CC attribute values.
 *
 * \api	mpe_ccGetAttributes(mpe_CcAttributes *attrib)
 *
 * \strategy Perform a call to mpe_ccGetAttributes() to check for 
 *  default attribute values.\n
 * \par
 *  Expected results: All members of the structure are defaulted.\n
 *  The function returned value should be MPE_SUCCESS\n
 *
 * \assets 
 *
 */
void vpk_test_ccGetAttributes1(CuTest *tc)
{
    uint32_t i;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Into vpk_test_ccGetAttributes1\n");
    CuAssertIntEquals_Msg(tc, "MPE_CC_TYPE order now invalidates test code",
            MPE_CC_TYPE_DIGITAL - MPE_CC_TYPE_ANALOG, 1);
    CuAssertIntEquals_Msg(tc, "MPE_CC_TYPE order now invalidates test code",
            MPE_CC_TYPE_MAX - MPE_CC_TYPE_DIGITAL, 1);

    for (i = 0; i < CCTYPE_SIZE; ++i)
    {
        /* get the current attributes and check against default expected */
        checkAttributes(tc, cctype[i].type, &def_expected);

        /* if we survived that call, just copy def_expected to actual defaults */
        cctype[i].def_attrib = def_expected;

        /*
         * In the future we may have some way to know if a given type (analog,
         * or digital) is valid. For now, we just make anything that gets this
         * far as valid.
         */
        cctype[i].valid = TRUE;
    }
}

/**
 * \testdescription test invalid parameter to ccGetAttributes
 *
 * \api mpe_ccGetAttributes(mpe_CcAttributes *attrib)
 *
 * \strategy Perform a call to mpeos_ccGetAttributes() with an invalid parameter.
 *  For example, mpe_ccGetAttributes(NULL). The function should return
 *  MPE_CC_ERROR_INVALID_PARAM.
 *
 * \assets 
 *
 */
void vpk_test_ccGetAttributes2(CuTest *tc)
{
    mpe_Error err;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "into vpk_test_ccGetAttributes2\n");

    err = ccGetAttributes(NULL, MPE_CC_TYPE_DIGITAL);

    CuAssert(tc, "mpe(x)_ccGetAttributes should return CC_ER_INVALID", err
            == MPE_CC_ERROR_INVALID_PARAM);
}

/**
 * \testdescription test each attribute against its advertised capabilities
 *
 * \api mpe_cc[Get|Set]Attributes(), mpe_ccGetCapability()
 *
 * \strategy For each separate attribute, get the capabilities, call
 * SetAttributes() to change it, and GetAttributes to ensure it changed.
 *
 * \assets 
 *
 */
void vpk_test_ccAttributes(CuTest *tc)
{
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "vpk_test_ccAttributes\n");
    tryAttribute(tc, MPE_CC_ATTRIB_FONT_COLOR);
    tryAttribute(tc, MPE_CC_ATTRIB_BACKGROUND_COLOR);
    tryAttribute(tc, MPE_CC_ATTRIB_FONT_OPACITY);
    tryAttribute(tc, MPE_CC_ATTRIB_BACKGROUND_OPACITY);
    tryAttribute(tc, MPE_CC_ATTRIB_FONT_STYLE);
    tryAttribute(tc, MPE_CC_ATTRIB_FONT_SIZE);
    tryAttribute(tc, MPE_CC_ATTRIB_FONT_ITALIC);
    tryAttribute(tc, MPE_CC_ATTRIB_FONT_UNDERLINE);
    tryAttribute(tc, MPE_CC_ATTRIB_BORDER_TYPE);
    tryAttribute(tc, MPE_CC_ATTRIB_BORDER_COLOR);
    tryAttribute(tc, MPE_CC_ATTRIB_WIN_COLOR);
    tryAttribute(tc, MPE_CC_ATTRIB_WIN_OPACITY);
}

static void checkAttributes(CuTest *tc, mpe_CcType type,
        const mpe_CcAttributes *expected)
{
    mpe_CcAttributes actual;
    mpe_Error err;

    memset(&actual, 0, sizeof(actual));
    err = mpe_ccGetAttributes(&actual, type);
    CuAssert(tc, "getting attributes after set failed", err == MPE_SUCCESS);

#define CA(mem)	CuAssertIntEquals_Msg(tc, "attribute member " #mem " failed to match", (int)expected->mem, (int)actual.mem);
    CA(charBgColor);
    CA(charFgColor);
    CA(winColor);
    CA(charBgOpacity);
    CA(charFgOpacity);
    CA(winOpacity);
    CA(fontSize);
    CA(fontStyle);
    CA(fontItalic);
    CA(fontUnderline);
    CA(borderType);
    CA(borderColor);
}

/**
 * \brief try to set an attribute with all of its capabilities
 *
 * \param attrib attribute to put through its paces
 *
 * \return true if it succeeded, false if it failed
 *
 */
static mpe_Bool tryAttribute(CuTest *tc, // test case
        mpe_CcAttribType attrType) // attribute type
{
    uint32_t i;

    /* make sure types are still in the assumed order */
    CuAssertIntEquals_Msg(tc, "MPE_CC_TYPE order now invalidates test code",
            MPE_CC_TYPE_DIGITAL - MPE_CC_TYPE_ANALOG, 1);
    CuAssertIntEquals_Msg(tc, "MPE_CC_TYPE order now invalidates test code",
            MPE_CC_TYPE_MAX - MPE_CC_TYPE_DIGITAL, 1);

    /* for each of the types of signals */
    for (i = 0; i < CCTYPE_SIZE; ++i)
    {
        char **caps;
        uint32_t capsLength;
        mpe_Error err;
        mpe_CcAttributes attrib;

        /* make sure we're considering this type */
        if (cctype[i].valid == FALSE)
            continue;

        /* get the capabilities for the attribute */
        err = mpe_ccGetCapability(attrType, cctype[i].type, (void **) &caps,
                &capsLength);
        CuAssertIntEquals_Msg(tc, "failed to get capability, non-MPE_SUCCESS",
                MPE_SUCCESS, err);

        /* ensure there are some capabilities */
        if (caps == NULL && capsLength == 0)
            continue;

        attrib = cctype[i].def_attrib;

        /* for each of the valid capabilities */
        while (capsLength--)
        {
            char *end;
            mpe_Bool trans;
            uint32_t attValue;

            /* convert value suitable for calling setattribute */
            switch (attrType)
            {
            case MPE_CC_ATTRIB_FONT_COLOR:
                attValue = strtoul(*caps, &end, 0);
                CuAssertIntEquals_Msg(
                        tc,
                        "failed to convert color capability to integer for FONT_COLOR",
                        0, (end == *caps) || *end != '\0');
                attrib.charFgColor = (mpe_CcColor) attValue;
                break;

            case MPE_CC_ATTRIB_BACKGROUND_COLOR:
                attValue = strtoul(*caps, &end, 0);
                CuAssertIntEquals_Msg(
                        tc,
                        "failed to convert color capability to integer for BACKGROUND_COLOR",
                        0, (end == *caps) || *end != '\0');
                attrib.charBgColor = (mpe_CcColor) attValue;
                break;

            case MPE_CC_ATTRIB_FONT_OPACITY:
                trans = translate(*caps, opacityMap, &attValue);
                CuAssert(tc, "failed to translate FONT_OPACITY capability",
                        trans);
                attrib.charFgOpacity = (mpe_CcOpacity) attValue;
                break;

            case MPE_CC_ATTRIB_BACKGROUND_OPACITY:
                trans = translate(*caps, opacityMap, &attValue);
                CuAssert(tc,
                        "failed to translate BACKGROUND_OPACITY capability",
                        trans);
                attrib.charBgOpacity = (mpe_CcOpacity) attValue;
                break;

            case MPE_CC_ATTRIB_FONT_STYLE:
                trans = translate(*caps, styleMap, &attValue);
                CuAssert(tc, "failed to translate FONT_STYLE capability", trans);
                attrib.fontStyle = (mpe_CcFontStyle) attValue;
                break;

            case MPE_CC_ATTRIB_FONT_SIZE:
                trans = translate(*caps, sizeMap, &attValue);
                CuAssert(tc, "failed to translate FONT_SIZE capability", trans);
                attrib.fontSize = (mpe_CcFontSize) attValue;
                break;

            case MPE_CC_ATTRIB_FONT_ITALIC:
                trans = translate(*caps, boolMap, &attValue);
                CuAssert(tc, "failed to translate FONT_ITALIC capability",
                        trans);
                attrib.fontItalic = (mpe_CcTextStyle) attValue;
                break;

            case MPE_CC_ATTRIB_FONT_UNDERLINE:
                trans = translate(*caps, boolMap, &attValue);
                CuAssert(tc, "failed to translate FONT_UNDERLINE capability",
                        trans);
                attrib.fontUnderline = (mpe_CcTextStyle) attValue;
                break;

            case MPE_CC_ATTRIB_BORDER_TYPE:
                trans = translate(*caps, borderMap, &attValue);
                CuAssert(tc, "failed to translate BORDER_TYPE capability",
                        trans);
                attrib.borderType = (mpe_CcBorderType) attValue;
                break;

            case MPE_CC_ATTRIB_BORDER_COLOR:
                attValue = strtoul(*caps, &end, 0);
                CuAssertIntEquals_Msg(
                        tc,
                        "failed to convert color capability to integer for BORDER_COLOR",
                        0, (end == *caps) || *end != '\0');
                attrib.borderColor = (mpe_CcColor) attValue;
                break;

            case MPE_CC_ATTRIB_WIN_COLOR:
                attValue = strtoul(*caps, &end, 0);
                CuAssertIntEquals_Msg(
                        tc,
                        "failed to convert color capability to integer for WIN_COLOR",
                        0, (end == *caps) || *end != '\0');
                attrib.winColor = (mpe_CcColor) attValue;
                break;

            case MPE_CC_ATTRIB_WIN_OPACITY:
                trans = translate(*caps, opacityMap, &attValue);
                CuAssert(tc, "failed to translate WIN_OPACITY capability",
                        trans);
                attrib.winOpacity = (mpe_CcOpacity) attValue;
                break;

            default:
                TRACE(
                        MPE_LOG_DEBUG,
                        MPE_MOD_TEST,
                        "<<CC_TEST>> tryAttribute() called w/ bad attribute type %d\n",
                        attrType);
                break;

            }

            /* set the attributes */
            err = mpe_ccSetAttributes(&attrib, attrType, cctype[i].type);
            CuAssertIntEquals_Msg(tc, "setting attribute to capability failed",
                    MPE_SUCCESS, err);

            /*
             * get and check the the attributes
             * We check them all because we want to be sure others were not
             * disturbed when we set ours.
             */
            checkAttributes(tc, cctype[i].type, &attrib);

            /* move to next capability of attribute */
            ++caps;
        }

        /* set attribute back to default */
        err = mpe_ccSetAttributes(&cctype[i].def_attrib, attrType,
                cctype[i].type);
        CuAssertIntEquals_Msg(tc, "setting attribute back to default failed",
                MPE_SUCCESS, err);

        checkAttributes(tc, cctype[i].type, &cctype[i].def_attrib);
    }
}

/* ccGetAnalogServices() */

/**
 * \testdescription check for default analog services.
 *
 * \api mpe_ccGetAnalogServices(mpe_CcAnalogServiceMap *service)
 *
 * \strategy perform a call to mpe_ccGetAnalogServices()
 * \par			  
 * The function should return MPE_SUCCESS. 
 * The returned analog service map should be 0.
 *
 * \assets 
 *
 */
void vpk_test_ccGetAnalogServices1(CuTest *tc)
{
    mpe_Error err;

    as = (mpe_CcAnalogServiceMap) 0x01;

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "<<CC_TEST>> vpk_test_ccGetAnalogServices1() calling ccGetAnalogServices()\n");

    err = ccGetAnalogServices(&as);

    CuAssert(tc, "mpe(x)_ccGetAnalogServices failed", err == MPE_SUCCESS);

    CuAssertIntEquals_Msg(tc, "Default analog service should be 0", 0, as);

}

/**
 * \testdescription use an invalid parameter.
 *
 * \api mpe_ccGetAnalogServices(mpe_CcAnalogServiceMap *service)
 *
 * \strategy Perform a call to mpe_ccGetAnalogServices() with
 * an invalid parameter: mpe_ccGetAnalogServices(NULL)
 * \par 			  
 * The function should return MPE_CC_ERROR_INVALID_PARAM. 
 *
 * \assets 
 *
 */
void vpk_test_ccGetAnalogServices2(CuTest *tc)
{
    mpe_Error err;

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "<<CC_TEST>> vpk_test_ccGetAnalogServices2() calling ccGetAnalogServices()\n");
    err = ccGetAnalogServices(NULL);

    CuAssert(tc, "mpe(x)_ccGetAnalogServices failed", err
            == MPE_CC_ERROR_INVALID_PARAM);
}

/**
 * \testdescription check for default analog services
 *
 * \api mpe_ccGetDigitalServices(mpe_CcDigitalServiceMap *service)
 *
 * \strategy Perform a call to mpe_ ccGetDigitalServices ()
 * \par  			  
 * The function should return MPE_SUCCESS. 
 * The returned digital service map should be 0.
 *
 * \assets 
 *
 */
void vpk_test_ccGetDigitalServices1(CuTest *tc)
{
    mpe_Error err;
    ds = (mpe_CcDigitalServiceMap) 0x01;

    err = ccGetDigitalServices(&ds);

    CuAssert(tc, "mpe(x)_ccGetDigitalServices failed", err == MPE_SUCCESS);

    CuAssertIntEquals_Msg(tc, "Default digital service should be 0", 0, ds);
}

/**
 * \testdescription use an invalid parameter.
 *
 * \api mpe_ccGetDigitalServices(mpe_CcDigitalServiceMap *service)
 *
 * \strategy Perform a call to mpe_ ccGetDigitalServices () with
 * an invalid parameter: mpe_ccGetDigitalServices (NULL)
 * \par
 * The function should return MPE_CC_ERROR_INVALID_PARAM. 
 *
 * \assets 
 *
 */
void vpk_test_ccGetDigitalServices2(CuTest *tc)
{
    mpe_Error err;

    err = ccGetDigitalServices(NULL);

    CuAssert(tc, "mpe(x)_ccGetDigitalServices failed", err
            == MPE_CC_ERROR_INVALID_PARAM);
}

/**
 * \testdescription Set Analog service CC1
 *
 * \api mpe_ccSetAnalogServices(mpe_CcAnalogServiceMap service)
 *
 * \strategy Perform a call to mpe_ccSetAnalogServices ()
 * \par
 * The function should return MPE_SUCCESS. A call to mpe_ccGetAnalogServices() should return 
 * Service Map = 0x80
 *
 * \assets 
 *
 */
void vpk_test_ccSetAnalogServices1(CuTest *tc)
{
    mpe_Error err = 0x0;

    /* Analog service map is an 8 bit value. 
     Least significant bit is service CC1 : b1000 0000 */
    uint32_t analog_s = 1000L;

    as = (mpe_CcDigitalServiceMap) 1 << ((sizeof(as) * 8) - 1);

    TRACE(
            MPE_LOG_DEBUG,
            MPE_MOD_TEST,
            "<<CC_TEST>> vpk_test_ccSetAnalogServices1() set analog service to %d\n",
            as);

    err = ccSetAnalogServices(analog_s);

    CuAssert(tc, "mpe(x)_ccSetAnalogServices failed", err == MPE_SUCCESS);

    /* check if service is set correctly */
    CuAssert(tc, "Analog Service should be CC1", CheckCCService(
            MPE_CC_TYPE_ANALOG) == true);

}

/**
 * \testdescription Set Digital services
 *
 * \api mpe_ccSetDigitalServices(mpe_CcDigitalServiceMap service)
 *
 * \strategy Perform a call to mpe_ccSetDigitalServices ()
 * \par
 * The function should return MPE_SUCCESS.
 * A call to mpe_ccSetDigitalServices() should return 
 * Service Map = 0x80....
 *
 * \assets 
 *
 */
void vpk_test_ccSetDigitalServices1(CuTest *tc)
{
    mpe_Error err = 0x0;

    /* Digital service map is a 64 bits value where 0 is not used.
     Second most significant bit is service Digital1 : b0100 0000.... */

    uint32_t digital_s = 0x1;

    ds = (mpe_CcDigitalServiceMap) 1 << ((sizeof(ds) * 8) - 2);

    err = ccSetDigitalServices(digital_s);

    CuAssert(tc, "mpe(x)_ccSetDigitalServices failed", err == MPE_SUCCESS);

    /* check if service is set correctly */
    CuAssert(tc, "Digital Service should be Digital1", CheckCCService(
            MPE_CC_TYPE_DIGITAL) == true);
}

/**
 * \testdescription Set CC state to MPE_CC_STATE_ON
 *
 * \api mpe_ccSetClosedCaptioningState(mpe_CcState *state)
 *
 * \strategy Perform a call to mpe_ccSetClosedCaptioningState( )
 * \par 			  
 * The function should return MPE_SUCCESS. Visually check that CC text is
 * presented on the screen.
 *
 * \assets 
 *
 */
void vpk_test_ccSetClosedCaptioningState1(CuTest *tc)
{
    mpe_Error err;

    mpe_CcState state;

    err = ccSetClosedCaptioning(MPE_CC_STATE_ON);
    CuAssert(tc, "mpe(x)ccSetClosedCaptioning() should return MPE_SUCCESS", err
            == MPE_SUCCESS);

    err = ccGetClosedCaptioning(&state);
    CuAssert(tc, "mpe(x)ccGetClosedCaptioning() should return MPE_SUCCESS", err
            == MPE_SUCCESS);
    CuAssert(tc, "mpe(x)ccGetClosedCaptioning() should return MPE_CC_STATE_ON",
            state == MPE_CC_STATE_ON);

}

/**
 * \testdescription Set CC state to MPE_CC_STATE_OFF
 *
 * \api mpe_ccSetClosedCaptioningState(mpe_CcState *state)
 *
 * \strategy Perform a call to mpe_ccSetClosedCaptioningState( )
 * \par
 * The function should return MPE_SUCCESS.
 * Visually check that CC text is no longer presented.
 *
 * \assets 
 *
 */
void vpk_test_ccSetClosedCaptioningState2(CuTest *tc)
{
    mpe_Error err;
    mpe_CcState state;

    err = ccSetClosedCaptioning(MPE_CC_STATE_OFF);
    CuAssert(tc, "mpe(x)ccSetClosedCaptioning() should return MPE_SUCCESS", err
            == MPE_SUCCESS);

    err = ccGetClosedCaptioning(&state);
    CuAssert(tc, "mpe(x)ccGetClosedCaptioning() should return MPE_SUCCESS", err
            == MPE_SUCCESS);
    CuAssert(tc,
            "mpe(x)ccGetClosedCaptioning() should return MPE_CC_STATE_OFF",
            state == MPE_CC_STATE_OFF);
}

/**
 * \testdescription Set CC state to MPE_CC_STATE_ON_MUTE
 *
 * \api mpe_ccSetClosedCaptioningState(mpe_CcState *state)
 *
 * \strategy Perform a call to mpe_ccSetClosedCaptioningState( )
 * \par   
 * The function should return MPE_SUCCESS.
 * Visually check that CC text is presented.
 *
 * \assets 
 *
 */
void vpk_test_ccSetClosedCaptioningState3(CuTest *tc)
{
    mpe_Error err;
    mpe_CcState state;

    err = ccSetClosedCaptioning(MPE_CC_STATE_ON_MUTE);
    CuAssert(tc, "mpe(x)ccSetClosedCaptioning() should return MPE_SUCCESS", err
            == MPE_SUCCESS);

    err = ccGetClosedCaptioning(&state);
    CuAssert(tc, "mpe(x)ccGetClosedCaptioning() should return MPE_SUCCESS", err
            == MPE_SUCCESS);
    CuAssert(tc,
            "mpe(x)ccGetClosedCaptioning() should return MPE_CC_STATE_ON_MUTE",
            state == MPE_CC_STATE_ON_MUTE);

}

void vpk_test_ccSetClosedCaptioningState4(CuTest *tc)
{
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "NOT IMPLEMENTED YET\n");
}

void vpk_test_ccSetClosedCaptioningState5(CuTest *tc)
{
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "NOT IMPLEMENTED YET\n");
}
void vpk_test_ccSetClosedCaptioningState6(CuTest *tc)
{
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "NOT IMPLEMENTED YET\n");
}
