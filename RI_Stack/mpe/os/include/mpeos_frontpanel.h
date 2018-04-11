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

#ifndef _MPEOS_FRONTPANEL_H
#define _MPEOS_FRONTPANEL_H

#ifdef __cplusplus
extern "C"
{
#endif

#include "mpe_types.h"
/*
 *   These #defines are per OCAP Front Panel Extension Specification Draft 01,
 *   OC-SP-OCAP-FPEXT-I01-050624 with ECN OCAP-FPEXT-N-05.0851-2
 */

/*
 *   The standard set of indicators.
 *   These are the strings that are used to identify the indicators.
 *   These strings are returned by mpeos_frontPanelGetSupportedIndicators and
 *   are used as parameters to various front panel functions.
 */

#define MPE_FP_INDICATOR_MESSAGE   "message"
#define MPE_FP_INDICATOR_POWER     "power"
#define MPE_FP_INDICATOR_RECORD    "record"
#define MPE_FP_INDICATOR_REMOTE    "remote"
#define MPE_FP_INDICATOR_RFBYPASS  "rfbypass"
#define MPE_FP_INDICATOR_TEXT      "text"      // text display
/*
 *   The minimum brightness is 0, which is off.
 *   A brightness of negative value is illegal.
 *   The maximum brightness is implementation dependent, 
 *     and is one less the value returned by mpeos_frontPanelGetBrightnessLevels().
 *   There must be at least two brightness levels:
 *     0, which is off, and 1, which is on.
 *
 *   see org.ocap.hardware.frontpanel.BrightSpec 
 */
#define MPE_FP_BRIGHTNESS_OFF		0x00000000

/*
 *   Color is specified as a bit field in a byte (unsigned char).
 *   To specify a color, use only one bit.
 *   To specify the set of supported colors, use one or more bits.
 *   If the implementation does not support color, use
 *   MPE_FRONT_PANEL_COLOR_UNSUPPORTED.
 */

// see org.ocap.hardware.frontpanel.ColorSpec
#define MPE_FP_COLOR_UNSUPPORTED   0x00
#define MPE_FP_COLOR_BLUE          0x01
#define MPE_FP_COLOR_GREEN         0x02
#define MPE_FP_COLOR_RED           0x04
#define MPE_FP_COLOR_YELLOW        0x08
#define MPE_FP_COLOR_ORANGE        0x10

/**
 * Specifies the operational modes of the front text panel
 *
 *     MPE_FP_TEXT_MODE_CLOCK_12HOUR - The display will show network time
 *     using a standard 12 hour HH:MM format
 *        
 *     MPE_FP_TEXT_MODE_CLOCK_24HOUR - The display will show network time
 *     using a standard 24 hour HH:MM format
 *
 *     MPE_FP_TEXT_MODE_STRING - The display will show a custom string of
 *     characters
 */
typedef enum mpe_FpTextPanelMode
{
    MPE_FP_TEXT_MODE_CLOCK_12HOUR = 0x00,
    MPE_FP_TEXT_MODE_CLOCK_24HOUR = 0x01,
    MPE_FP_TEXT_MODE_STRING = 0x02
} mpe_FpTextPanelMode;

/**
 * Defines the entire capability set of the front panel display
 *
 *     totalIndicators - Represents the total number of unique indicators
 *     available on the front panel display (including a text panel, if
 *     available).
 *
 *     indicatorNames - Points to an array (of length totalIndicators) of
 *     null-terminated strings which specify the unique name of each
 *     indicator.  See the OCAP Front Panel Extension API for specific
 *     names of mandatory and optional indicators.
 *
 *     colors - An array of integers (corresponding to each indicator in
 *     indicatorNames) set to the bitwise OR of all display colors
 *     available to each indicator.
 *
 *     brightness - An array of integers (corresponding to each indicator
 *     in indicatorNames) set to the number of discrete brightness
 *     settings available to each indicator.
 *
 *     maxCycleRate - An array of integers (corresponding to each
 *     indicator in indicatorNames) which specify the maximum number of
 *     times per minute each indicator can blink.  Zero indicates that
 *     blinking is not supported.
 *
 *     supportedChars - Points to an array of all characters that can be
 *     displayed on the text panel.  NULL if there is no text panel
 *     support on this host.
 *
 *     columns - The number characters that can be displayed on each row
 *     of the text panel.
 *
 *     rows - The number of rows available for text display on the text
 *     panel.
 *
 *     maxHorizontalIterations - The maximum number of times per minute
 *     that characters can scroll right-to-left across the text display
 *     with zero hold time set.  Zero indicates that scrolling is not
 *     supported.
 *
 *     maxVerticalIterations - The maximum number of times per minute
 *     that rows of text can scroll bottom-to-top across the text display
 *     with zero hold time set.  Zero indicates that vertical scrolling
 *     is not supported.
 */
typedef struct mpe_FpCapabilities
{
    uint32_t totalIndicators;
    char** indicatorNames;
    uint32_t* colors;
    uint32_t* brightness;
    uint32_t* maxCycleRate;
    char* supportedChars;
    uint32_t columns;
    uint32_t rows;
    uint32_t maxHorizontalIterations;
    int32_t maxVerticalIterations;
} mpe_FpCapabilities;

/**
 * Describes how the front panel text or indicator should blink
 *
 *     iterations - The number of times per minute the text display or 
 *     indicator will blink.  Zero means no blinking.
 *
 *     onDuration - A value between 0 and 100 that represents the
 *     percentage of time per blink that the LED is on.  Zero would
 *     effectively be turning off the display.  100 would effectively be
 *     always on. 
 */
typedef struct mpe_FpBlinkSpec
{
    uint16_t iterations;
    uint16_t onDuration;
} mpe_FpBlinkSpec;

/**
 * Describes how the front panel text should scroll
 *
 *     horizontalIterations - The number of times per minute the characters
 *     on the text display are set to scroll across the display from
 *     right-to-left.  Zero means horizontal scrolling is disabled.  -1 means
 *     there is more than one one row displayed and characters will scroll
 *     vertically.
 *
 *     verticalIterations - The number of times per minute the rows of text
 *     on the display are set to scroll from bottom-to-top across the display.
 *     Zero means vertical scrolling is disabled.  -1 means that the display
 *     only supports one row of characters and text will scroll horizontally.
 *
 *     holdDuration - A value between 0 and 100 which represents the percentage
 *     of time the display will hold at each character (or each line, when
 *     scrolling vertically) while scrolling.
 */
typedef struct mpe_FpScrollSpec
{
    int16_t horizontalIterations;
    int16_t verticalIterations;
    int16_t holdDuration;
} mpe_FpScrollSpec;

/**
 * Initialize platform specific front panel support.
 */
void mpeos_fpInit(void);

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
mpe_Error mpeos_fpGetCapabilities(mpe_FpCapabilities** capabilities);

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
        uint32_t* color, mpe_FpBlinkSpec* blinkSpec);

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
        uint32_t color, mpe_FpBlinkSpec blinkSpec);

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
        mpe_FpScrollSpec* scrollSpec);

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
        mpe_FpBlinkSpec blinkSpec, mpe_FpScrollSpec scrollSpec);

#ifdef __cplusplus
}
#endif

#endif /* _MPEOS_FRONTPANEL_H */

