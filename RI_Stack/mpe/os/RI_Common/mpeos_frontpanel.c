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

/* Header Files */
#include <string.h>
#include "mpe_error.h"
#include "mpeos_dbg.h"
#include "mpeos_mem.h"
#include "mpeos_util.h"
#include "ri_ui_manager.h"
#include "platform_frontpanel.h"

// Maximum blink cycle rate.  Use 4 Hz for 1 minute, until we know otherwise
// from the implementation.
//static uint32_t blinkMaxCycleRate = 4 * 60;

// Milliseconds per minute
#define MS_PER_MINUTE 60000

// The size of the display in characters.  Use the typical size of
// a clock until we find out differently from the implementation.
static uint32_t textDisplayColumns = 4;
static uint32_t textDisplayRows = 1;

// The minimum time that a character must display in a position when scrolling
// horizontally (in milliseconds).  Default to 1/4 second.
static int32_t minHorizontalScrollDelay = 250;

// Maximum number of scroll interations per minute
// That is, the number of times a character will scroll across a display
// horizontally in a minute.
static int32_t maxHorizontalIterations();

// The minimum time that a row must display in a position when scrolling
// vertically (in milliseconds).  Default to 1/4 second.
static int32_t minVerticalScrollDelay = 250;

// Maximum number of scroll interations per minute
// That is, the number of times a character will scroll across a display
// horizontally in a minute.
static int32_t maxVerticalIterations();

// Simulator Capabilities
struct mpe_FpCapabilities myCapabilities;

// Flag indicating state of initialization.
static mpe_Bool g_initialized = false;

// Text display index.
static int g_textDisplayIndex = -1;

// Conversion convenience functions - forward declarations.
static void convertSimColorsToMPE(RI_FP_INDICATOR_COLOR simColors,
        uint32_t *mpeColors);
static RI_FP_INDICATOR_COLOR convertMPEColorToSim(uint32_t mpeColor);
static void convertSimTextModeToMPE(RI_FP_DISPLAY_MODE textMode,
        mpe_FpTextPanelMode* mode);
static RI_FP_DISPLAY_MODE convertMPETextModeToSim(mpe_FpTextPanelMode mode);
static mpe_Bool isScrollSpecValid(uint32_t indicator,
        mpe_FpScrollSpec scrollSpec);
static mpe_Bool isBlinkSpecValid(uint32_t indicator, mpe_FpBlinkSpec blinkSpec);

/**
 * Initializes platform specific front panel support.
 */
void mpeos_fpInit(void)
{
    ri_frontpanel_t *fp;
    uint32_t knownIndicatorCount;
    uint32_t accessibleIndicatorCount = 0;
    char** nameList = NULL;
    uint32_t i;
    RI_FP_INDICATOR_COLOR colors;
    uint32_t brightLevels;
    uint32_t cycleRate;
    uint32_t status;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_FP, "mpeos_fpInit()\n");

    // Prevent repeated initialization.
    if (g_initialized)
    {
        return;
    }
    else
        fp = ri_get_frontpanel();

    // Get the list of indicators.
    knownIndicatorCount = fp->getIndicatorList(&nameList);

    // Allocate space in our capabilities structure based on the total number of
    // possible indicators.
    status = mpeos_memAllocP(MPE_MEM_FP, knownIndicatorCount * sizeof(char*),
            (void**) (&myCapabilities.indicatorNames));
    if (MPE_SUCCESS == status)
    {
        status = mpeos_memAllocP(MPE_MEM_FP, knownIndicatorCount
                * sizeof(uint32_t), (void**) (&myCapabilities.colors));
    }
    if (MPE_SUCCESS == status)
    {
        status = mpeos_memAllocP(MPE_MEM_FP, knownIndicatorCount
                * sizeof(uint32_t), (void**) (&myCapabilities.brightness));
    }
    if (MPE_SUCCESS == status)
    {
        status = mpeos_memAllocP(MPE_MEM_FP, knownIndicatorCount
                * sizeof(uint32_t), (void**) (&myCapabilities.maxCycleRate));
    }
    if (MPE_SUCCESS != status)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FP,
                "mpeos_fpInit() - could not allocate capability memory!\n");
        return;
    }

    // Load the data for each indicator.
    for (i = 0; i < knownIndicatorCount; ++i)
    {
        // Got the structure - get the caps.
        (void) fp->getIndicatorCaps(nameList[i], &brightLevels, &colors,
                &cycleRate);

        // Convert them to MPE-style caps and save.
        // Make an indicator structure.
        if (MPE_SUCCESS
                != mpeos_memAllocP(
                        MPE_MEM_FP,
                        strlen(nameList[i]) + 1,
                        (void **) &(myCapabilities.indicatorNames[accessibleIndicatorCount])))
        {
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_FP,
                    "Cannot allocate memory for indicator name %s\n",
                    nameList[i]);
        }
        else
        {
            // Copy over the name.
            strcpy(myCapabilities.indicatorNames[accessibleIndicatorCount],
                    nameList[i]);

            // Set the levels.
            myCapabilities.brightness[accessibleIndicatorCount] = brightLevels;

            // Convert the colors.
            convertSimColorsToMPE(colors,
                    &(myCapabilities.colors[accessibleIndicatorCount]));

            // Set the max cycle rate.
            myCapabilities.maxCycleRate[accessibleIndicatorCount] = cycleRate;

            // If this is the text display, get it's data, too.
            if (0 == strcmp(nameList[i], MPE_FP_INDICATOR_TEXT))
            {
                // Set the index for the text display "indicator".
                MPEOS_LOG(
                        MPE_LOG_DEBUG,
                        MPE_MOD_FP,
                        "mpeos_fpInit() - Found support for text display -- indicator index = %d!\n",
                        i);
                g_textDisplayIndex = i;

                // Get the text display-specific capabilities.
                fp->getTextCaps(&myCapabilities.rows, &myCapabilities.columns,
                        (const char **) &myCapabilities.supportedChars,
                        &myCapabilities.maxHorizontalIterations,
                        (long *) &myCapabilities.maxVerticalIterations);

            }

            // Add the entry to the list.
            ++accessibleIndicatorCount;
        } // End of if we could allocate for the indicator name.
    } // End of for each indicator.

    // Record the number of usable indicators.
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_FP,
            "mpeos_fpInit() - Found support for %d indicators!\n",
            accessibleIndicatorCount);
    myCapabilities.totalIndicators = accessibleIndicatorCount;

    // Free the memory gotten from the RI Platform.
    fp->freeIndicatorList(nameList);

    // Initialization is done.
    g_initialized = true;
}

/**
 * Get the capabilities of the front panel.
 *
 * @param capabilities A pointer to a pointer to a callee allocated structure
 *                     describing the capabilities of the front panel display.
 *                     This capabilities structure is assumed to be allocated
 *                     by the system-level software and it will not be altered
 *                     by the stack (i.e. the implementation can simply set the
 *                     pointer to point to a constant structure)
 *
 * @return MPE_SUCCESS if the call was successful or MPE_EINVAL if one or more
 *         arguments to the function is invalid
 */
mpe_Error mpeos_fpGetCapabilities(mpe_FpCapabilities** capabilities)
{
    if (capabilities == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FP,
                "mpeos_fpGetCapabilities() - NULL output parameter!\n");
        return MPE_EINVAL;
    }

    *capabilities = &myCapabilities;
    return MPE_SUCCESS;
}

/**
 * Get the current settings of the specified indicator.
 *
 * @param indicator Specifies the indicator whose settings are to be retrieved.
 *                  The number corresponds to the index into the indicator array
 *                  provided by the mpeos_fpGetCapabilities call.  The text panel
 *                  should not be specified through this routine.
 *
 * @param brightness A pointer to the returned brightness level of the specified
 *                   indicator.  A value of MPE_FP_BRIGHTNESS_OFF means the
 *                   indicator is off.  Values above MPE_FP_BRIGHTNESS_OFF specify
 *                   higher levels of brightness up to and including the maximum
 *                   level specified by the mpeos_fp_GetCapabilities() return value
 *                   for this indicator.
 *
 * @param color A pointer to the returned color of the specified indicator
 *
 * @param blinkSpec A pointer to the returned value specifying the current blink
 *                  settings of the specified indicator
 *
 * @return MPE_SUCCESS if the call was successful, MPE_EINVAL if one or more
 *         arguments to the function in invalid
 */
mpe_Error mpeos_fpGetIndicator(uint32_t indicator, uint32_t* brightness,
        uint32_t* color, mpe_FpBlinkSpec* blinkSpec)
{
    ri_frontpanel_t *fp;
    RI_FP_INDICATOR_COLOR simColor;
    uint32_t iters;
    uint32_t on;
    uint32_t bright;

    // Check for parameter validity
    //    1)  Valid indicator index
    //    3)  Non-null output parameters
    if (indicator >= myCapabilities.totalIndicators)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FP,
                "mpeos_fpGetIndicator() - invalid indicator (%d)!\n", indicator);
        return MPE_EINVAL;
    }
    if (brightness == NULL || color == NULL || blinkSpec == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FP,
                "mpeos_fpGetIndicator() - NULL output parameter!\n");
        return MPE_EINVAL;
    }

    // Get the RI Platform Front Panel object.
    fp = ri_get_frontpanel();

    (void) fp->getIndicator(myCapabilities.indicatorNames[indicator], &bright,
            &simColor, &iters, &on);

    // Convert brightness from RI Platform data-range [1..Max] to MPE data-range [0..Max] by subtracting one
    *brightness = bright - 1;
    MPEOS_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_FP,
            "mpeos_fpGetIndicator() - indicator[%d] - Platform/Brightness=%d MPE/Brightness=%d\n",
            indicator, bright, *brightness);

    blinkSpec->iterations = (short) iters;
    blinkSpec->onDuration = (short) on;

    convertSimColorsToMPE(simColor, color);

    return MPE_SUCCESS;
}

/**
 * Set the specified indicator to specific values
 *
 * @param indicator Specifies the indicator which is to be set.  The number
 *                  corresponds to the index into the indicator array provided
 *                  by the mpeos_fpGetCapabilities call.  The text panel
 *                  should not be specified through this routine.
 *
 * @param brightness Specifies the desired brightness level of the specified
 *                   indicator.  A value of MPE_FP_BRIGHTNESS_OFF means the
 *                   indicator is off.  Values above MPE_FP_BRIGHTNESS_OFF specify
 *                   higher levels of brightness up to and including the maximum
 *                   level specified by the mpeos_fpGetCapabilities() return value
 *                   for this indicator.
 *
 * @param color The desired color of the specified indicator
 *
 * @param blinkSpec The desired blink settings of the specified indicator
 *
 * @return MPE_SUCCESS if the call was successful, MPE_EINVAL if one or more
 *         arguments to the function in invalid
 */
mpe_Error mpeos_fpSetIndicator(uint32_t indicator, uint32_t brightness,
        uint32_t color, mpe_FpBlinkSpec blinkSpec)
{
    ri_frontpanel_t *fp;
    uint32_t bright;

    // Check for parameter validity
    //    1)  Valid indicator index
    //    3)  Color must be within capabilities
    //    4)  Brightness must be within capabilities
    //    5)  BlinkSpec must be within capabilities
    if (indicator >= myCapabilities.totalIndicators)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FP,
                "mpeos_fpSetIndicator() - invalid indicator (%d)!\n", indicator);
        return MPE_EINVAL;
    }
    if ((~myCapabilities.colors[indicator] & color) != 0)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FP,
                "mpeos_fpSetIndicator() - invalid color (%x)!\n", color);
        return MPE_EINVAL;
    }
    if (brightness > myCapabilities.brightness[indicator])
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FP,
                "mpeos_fpSetIndicator() - invalid brightness (%x)!\n",
                brightness);
        return MPE_EINVAL;
    }
    if (!isBlinkSpecValid(indicator, blinkSpec))
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FP,
                "mpeos_fpSetIndicator() - invalid blink spec!\n");
        return MPE_EINVAL;
    }

    // Convert brightness from MPE data-range [0..Max] to RI Platform data-range [1..Max] by adding one
    bright = brightness + 1;
    MPEOS_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_FP,
            "mpeos_fpSetIndicator() - indicator[%d] - MPE/Brightness=%d Platform/Brightness=%d \n",
            indicator, brightness, bright);

    // Get the RI Platform Front Panel object.
    fp = ri_get_frontpanel();

    (void) fp->setIndicatorBrightLevel(
            myCapabilities.indicatorNames[indicator], bright);
    (void) fp->setIndicatorColor(myCapabilities.indicatorNames[indicator],
            convertMPEColorToSim(color));
    (void) fp->setIndicatorBlinkIter(myCapabilities.indicatorNames[indicator],
            blinkSpec.iterations);
    (void) fp->setIndicatorBlinkOn(myCapabilities.indicatorNames[indicator],
            blinkSpec.onDuration);

    return MPE_SUCCESS;
}

/**
 * Get the mode and other settings of the front panel text display
 *
 * @param mode A pointer to the returned value specifying the current front
 *             panel text display mode.
 *
 * @param color A pointer to the returned value specifying the current color
 *              of the front panel text display.
 *
 * @param brightness A pointer to the returned value specifying the current
 *                   brightness level of the font panel text display.
 *                   MPE_FP_BRIGHTNESS_OFF means to turn the indicator off.
 *                   Values above MPE_FP_BRIGHTNESS_OFF specify higher levels
 *                   of brightness up to and including the maximum level
 *                   specified by the mpeos_fpGetCapabilities return value for
 *                   the text display.
 *
 * @param blinkSpec A pointer to the returned value specifying the current
 *                  blink settings for the front panel text display.
 *
 * @param scrollSpec A pointer to the returned value specifying the current
 *                   scroll settings for the fonrt panel text display.
 *
 * @return MPE_SUCCESS if the call was successful, MPE_EINVAL if one or more
 *         arguments to the function in invalid
 */
mpe_Error mpeos_fpGetText(mpe_FpTextPanelMode* mode, uint32_t* color,
        uint32_t* brightness, mpe_FpBlinkSpec* blinkSpec,
        mpe_FpScrollSpec* scrollSpec)
{
    ri_frontpanel_t *fp;
    RI_FP_DISPLAY_MODE textMode;
    long horizIters;
    long vertIters;
    uint32_t delayTime;
    uint32_t bright;

    // Check that none of the parameters are null and that we have a text display.
    if (mode == NULL || color == NULL || brightness == NULL || blinkSpec
            == NULL || scrollSpec == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FP,
                "mpeos_fpGetText() - NULL output parameter!\n");
        return MPE_EINVAL;
    }
    if (g_textDisplayIndex == -1)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FP,
                "mpeos_fpGetText() - Host does not have a text display!\n");
        return MPE_EINVAL;
    }

    (void) mpeos_fpGetIndicator(g_textDisplayIndex, // Should the return value be checked here?
            &bright, color, blinkSpec);

    // Convert brightness from RI Platform data-range [1..Max] to MPE data-range [0..Max] by subtracting one.
    //*brightness = bright - 1; Conversion already handled by above call to mpeos_fpGetIndicator();
    *brightness = bright;
    MPEOS_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_FP,
            "mpeos_fpGetText() - indicator[%d] - Platform/Brightness=%d MPE/Brightness=%d\n",
            g_textDisplayIndex, bright, *brightness);

    // Get the RI Platform Front Panel object.
    fp = ri_get_frontpanel();

    fp->getTextMode(&textMode);
    convertSimTextModeToMPE(textMode, mode);
    fp->getTextAttributes(&horizIters, &vertIters, &delayTime);

    scrollSpec->holdDuration = (uint16_t) delayTime;
    scrollSpec->horizontalIterations = (uint16_t) horizIters;
    scrollSpec->verticalIterations = (uint16_t) vertIters;

    return MPE_SUCCESS;
}

/**
 * Set the mode, display string, and other settings of the front panel text
 * display
 *
 * @param mode Specifies the desired front panel text mode
 *
 * @param numTextLines The number of lines of text to be set to the display.
 *                     This value will be ignored if mode is equal to
 *                     MPE_FP_TEXT_MODE_CLOCK_12HOUR or MPE_FP_TEXT_MODE_CLOCK_24HOUR.
 *
 * @param text An array (of length numTextLines) of null-terminated strings that
 *             specifies the desired contents of the text display.  The memory
 *             containing this text data will be deallocated upon return of the
 *             function.  This value will be ignored if mode is equal to
 *             MPE_FP_TEXT_MODE_CLOCK_12HOUR or MPE_FP_TEXT_MODE_CLOCK_24HOUR.
 *
 * @param color Specifies the desired color of the text display.
 *
 * @param brightness Specifies the desired brightness level of the text panel.
 *                   A value of MPE_FP_BRIGHTNESS_OFF means to turn the indicator
 *                   off.  Values above MPE_FP_BRIGHTNESS_OFF specify higher
 *                   levels of brightness up to and including the maximum level
 *                   specified by the mpeos_fpGetCapabilities() return value for
 *                   the text display.
 *
 * @param blinkSpec Specifies the desired blink settings for the text display.
 *
 * @param scrollSpec Specifies the desired scroll settings for the text display.
 *
 * @return MPE_SUCCESS if the call was successful, MPE_EINVAL if one or more
 *         arguments to the function in invalid
 */
mpe_Error mpeos_fpSetText(mpe_FpTextPanelMode mode, uint32_t numTextLines,
        const char** text, uint32_t color, uint32_t brightness,
        mpe_FpBlinkSpec blinkSpec, mpe_FpScrollSpec scrollSpec)
{
    ri_frontpanel_t *fp;
    uint32_t bright;

    // Check that we have a text display
    // Check for parameter validity
    //    1)  Color must be within capabilities
    //    2)  Brightness must be within capabilities
    //    3)  BlinkSpec must be within capabilities
    //    4)  ScrollSpec must be within capabilities
    if (g_textDisplayIndex == -1)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FP,
                "mpeos_fpSetText() - Host does not have a text display!\n");
        return MPE_EINVAL;
    }

    if ((~myCapabilities.colors[g_textDisplayIndex] & color) != 0)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FP,
                "mpeos_fpSetText() - invalid color (%x)!\n", color);
        return MPE_EINVAL;
    }
    if (brightness > myCapabilities.brightness[g_textDisplayIndex])
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FP,
                "mpeos_fpSetText() - invalid brightness (%x)!\n", brightness);
        return MPE_EINVAL;
    }
    if (!isBlinkSpecValid(g_textDisplayIndex, blinkSpec))
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FP,
                "mpeos_fpSetText() - invalid blink spec!\n");
        return MPE_EINVAL;
    }
    if (!isScrollSpecValid(g_textDisplayIndex, scrollSpec))
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FP,
                "mpeos_fpSetText() - invalid scroll spec!\n");
        return MPE_EINVAL;
    }

    // Get the RI Platform Front Panel object.
    fp = ri_get_frontpanel();

    fp->setTextStrings(numTextLines, text);
    fp->setTextMode(convertMPETextModeToSim(mode));
    fp->setTextScrollDelay(scrollSpec.holdDuration);
    fp->setTextVertScroll(scrollSpec.verticalIterations);
    fp->setTextHorizScroll(scrollSpec.horizontalIterations);

    // Convert brightness from MPE data-range [0..Max] to RI Platform data-range [1..Max] by adding one
    bright = brightness + 1;
    MPEOS_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_FP,
            "mpeos_fpSetText() - indicator[%d] - MPE/Brightness=%d Platform/Brightness=%d \n",
            g_textDisplayIndex, brightness, bright);

    return mpeos_fpSetIndicator(g_textDisplayIndex, bright, color, blinkSpec);
}

void convertSimColorsToMPE(RI_FP_INDICATOR_COLOR simColors, uint32_t *mpeColors)
{
    *mpeColors = 0;

    if (simColors & RI_FP_BLUE)
    {
        *mpeColors |= MPE_FP_COLOR_BLUE;
    }

    if (simColors & RI_FP_GREEN)
    {
        *mpeColors |= MPE_FP_COLOR_GREEN;
    }

    if (simColors & RI_FP_RED)
    {
        *mpeColors |= MPE_FP_COLOR_RED;
    }

    if (simColors & RI_FP_YELLOW)
    {
        *mpeColors |= MPE_FP_COLOR_YELLOW;
    }

    if (simColors & RI_FP_ORANGE)
    {
        *mpeColors |= MPE_FP_COLOR_ORANGE;
    }
}

RI_FP_INDICATOR_COLOR convertMPEColorToSim(uint32_t mpeColor)
{
    RI_FP_INDICATOR_COLOR retColor = RI_FP_NONE;

    switch (mpeColor)
    {
    case MPE_FP_COLOR_BLUE:
    {
        retColor = RI_FP_BLUE;
        break;
    }
    case MPE_FP_COLOR_GREEN:
    {
        retColor = RI_FP_GREEN;
        break;
    }
    case MPE_FP_COLOR_RED:
    {
        retColor = RI_FP_RED;
        break;
    }
    case MPE_FP_COLOR_YELLOW:
    {
        retColor = RI_FP_YELLOW;
        break;
    }
    case MPE_FP_COLOR_ORANGE:
    {
        retColor = RI_FP_ORANGE;
        break;
    }
    };

    return retColor;
}

void convertSimTextModeToMPE(RI_FP_DISPLAY_MODE textMode,
        mpe_FpTextPanelMode* mode)
{
    switch (textMode)
    {
    case RI_FP_MODE_12H_CLOCK:
    {
        *mode = MPE_FP_TEXT_MODE_CLOCK_12HOUR;
        break;
    }
    case RI_FP_MODE_24H_CLOCK:
    {
        *mode = MPE_FP_TEXT_MODE_CLOCK_24HOUR;
        break;
    }
    case RI_FP_MODE_STRING:
    {
        *mode = MPE_FP_TEXT_MODE_STRING;
        break;
    }
    };
}

RI_FP_DISPLAY_MODE convertMPETextModeToSim(mpe_FpTextPanelMode mode)
{
    RI_FP_DISPLAY_MODE simMode = RI_FP_MODE_12H_CLOCK;

    switch (mode)
    {
    case MPE_FP_TEXT_MODE_CLOCK_12HOUR:
    {
        simMode = RI_FP_MODE_12H_CLOCK;
        break;
    }
    case MPE_FP_TEXT_MODE_CLOCK_24HOUR:
    {
        simMode = RI_FP_MODE_24H_CLOCK;
        break;
    }
    case MPE_FP_TEXT_MODE_STRING:
    {
        simMode = RI_FP_MODE_STRING;
        break;
    }
    }
    return simMode;
}

mpe_Bool isScrollSpecValid(uint32_t indicator, mpe_FpScrollSpec scrollSpec)
{
    MPE_UNUSED_PARAM(indicator);

    if ((-1 != scrollSpec.horizontalIterations)
            && (scrollSpec.horizontalIterations >= maxHorizontalIterations()))
    {
        return false;
    }

    if ((0 < scrollSpec.verticalIterations) && (scrollSpec.verticalIterations
            >= maxVerticalIterations()))
    {
        return false;
    }

    return true;
}

mpe_Bool isBlinkSpecValid(uint32_t indicator, mpe_FpBlinkSpec blinkSpec)
{
    // No blinking is OK
    if (0 == blinkSpec.iterations)
    {
        return true;
    }

    if (100 < blinkSpec.onDuration)
    {
        return false;
    }

    return (blinkSpec.iterations <= myCapabilities.maxCycleRate[indicator]);
}

int32_t maxHorizontalIterations()
{
    return (int32_t)(MS_PER_MINUTE / (minHorizontalScrollDelay
            * textDisplayColumns));
}

int32_t maxVerticalIterations()
{
    if (1 >= textDisplayRows)
    {
        return -1;
    }
    else
    {
        return (int32_t)(MS_PER_MINUTE / (minVerticalScrollDelay
                * textDisplayRows));
    }
}
