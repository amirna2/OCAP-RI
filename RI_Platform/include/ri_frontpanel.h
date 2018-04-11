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

/*
 * ri_frontpanel.h
 *
 *  Created on: May 5, 2009
 *      Author: Mark Millard
 */

#ifndef _RI_FRONTPANEL_H_
#define _RI_FRONTPANEL_H_

// Include RI Platform header files.
#include <ri_types.h>

/**
 * Front Panel Indicator colors.  Note that the order of the colors
 * defines the precedence of what default color to use if one is not
 * manually selected.
 */
typedef enum
{
    RI_FP_NONE = 0x00,
    RI_FP_RED = 0x01,
    RI_FP_GREEN = 0x02,
    RI_FP_YELLOW = 0x04,
    RI_FP_ORANGE = 0x08,
    RI_FP_BLUE = 0x10,
    RI_FP_MAX_COLOR = RI_FP_BLUE
} RI_FP_INDICATOR_COLOR;

/**
 * Front Panel text mode.
 */
typedef enum
{
    RI_FP_MODE_12H_CLOCK, // display 12 hr. clock string
    RI_FP_MODE_24H_CLOCK, // display 24 hr. clock string
    RI_FP_MODE_STRING
// display alpha-numeric string
} RI_FP_DISPLAY_MODE;

/**
 * The RI Platform Front Panel object type.
 */
typedef struct ri_frontpanel_s ri_frontpanel_t;

/**
 * The RI Platform Front Panel data type.
 */
typedef struct ri_frontpanel_data_s ri_frontpanel_data_t;

/**
 * The RI Platform Front Panel Object definition.
 */
struct ri_frontpanel_s
{
    // Front Panel APIs

    /**
     * This method is used to obtain the list of indicators supported by the
     * Front Panel.
     *
     * @return  The number of indicators supported by the front panel.  This
     *          is also the size of the returned array of names.
     *
     * @param   names   The array of the names of the supported indicators
     *                  for the front panel.  This array is allocated and
     *                  controlled within the RI Platform, and is not to be modified
     *                  by the caller.  MUST NOT BE NULL.
     *
     * @remarks This routine is used to find the number and names of the
     *          indicators available in the front panel.
     *
     * @warning The returned array MUST be freed using freeIndicatorList().
     */
    unsigned int (*getIndicatorList)(char*** names);

    /**
     * This method is used to free the memory allocated by
     * getIndicatorList().
     *
     * @param   names   The array of char* passed out by getIndicatorList().
     *
     * @warning This method MUST be called for each call to
     *          getIndicatorList().
     */
    void (*freeIndicatorList)(char** names);

    /**
     * This method is used to obtain the capabilities of the specified
     * indicator.
     *
     * @return  TRUE if the passed name corresponds to an known indicator, FALSE
     *          if not.
     *
     * @param   name            The name of the indicator to get the capabilities
     *                          for.  MAY NOT BE NULL.
     * @param   numBrightnesses The number of brightness levels supported by the
     *                          indicator.  May be NULL.
     * @param   colors          An or'd list of the available colors for the
     *                          indicator.  May be NULL.
     * @param   maxCycleRate    The maximum number of times per minute that the
     *                          inidicator can be made to blink.  May be NULL.
     *
     * @remarks This routine is used to determine the capabilities of the
     *          indicator in question.  This method is used for the text/
     *          clock indicator as well as for "bit" LEDs.
     *
     * @warning A false return will cause the provided parameters to be unchanged.
     */
    ri_bool (*getIndicatorCaps)(char* name, unsigned int* numBrightnesses,
            RI_FP_INDICATOR_COLOR* colors, unsigned int* maxCycleRate);

    /**
     * This method is used to get the various settings of the specified
     * indicator.
     *
     * @return  TRUE if the passed name corresponds to an known indicator, FALSE
     *          if not.
     *
     * @param   name        The name of the indicator to get the capabilities
     *                      for.  MAY NOT BE NULL.
     * @param   brightLevel The number of brightness level currently set.  May
     *                      be NULL.
     * @param   color       The current color.  May be NULL.
     * @param   cycleRate   The number of times per minute that the indicator will
     *                      blink.  May be NULL.
     * @param   percentOn   A percentage integer, from 0 to 100, of the percentage
     *                      of time that an indicator will be on during a blinking
     *                      display.  0 is off, and 100 is always on.  May be NULL.
     *
     * @remarks This routine is used to determine the capabilities of the
     *          indicator in question.  This method is used for the text/
     *          clock indicator as well as for "bit" LEDs.
     *
     * @warning A false return will cause the provided parameters to be unchanged.
     */
    ri_bool (*getIndicator)(char* name, unsigned int* brightLevel,
            RI_FP_INDICATOR_COLOR* color, unsigned int* cycleRate,
            unsigned int* percentOn);

    /**
     * This method is used to set the brightness level of the specified indicator.
     *
     * @return  TRUE if the passed name corresponds to an known indicator, FALSE
     *          if not.  Also, will return FALSE if the brightness level passed
     *          is invalid.
     *
     * @param   name        The name of the indicator to get the capabilities
     *                      for.  MAY NOT BE NULL.
     * @param   brightLevel The brightness level to set.  Must be >0 and < the
     *                      maximum returned by getIndicatorCaps() for this
     *                      indicator.
     *
     * @remarks This routine is used to set the brightness level of the indicator
     *          specified.
     */
    ri_bool (*setIndicatorBrightLevel)(char* name, unsigned int brightLevel);

    /**
     * This method is used to set the color of the specified indicator.
     *
     * @return  TRUE if the passed name corresponds to an known indicator, FALSE
     *          if not.  Also, will return FALSE if the color passed is invalid.
     *
     * @param   name        The name of the indicator to get the capabilities
     *                      for.  MAY NOT BE NULL.
     * @param   color       The color to set.  Must be a value returned by
     *                      getIndicatorCaps() for this indicator.
     *
     * @remarks This routine is used to set the color of the indicator
     *          specified.
     */
    ri_bool (*setIndicatorColor)(char* name, RI_FP_INDICATOR_COLOR color);

    /**
     * This method is used to set the number of times the specified indicator
     * will blink in a minute.
     *
     * @return  TRUE if the passed name corresponds to an known indicator, FALSE
     *          if not.  Also, will return FALSE if the cycleRate passed is
     *          invalid.
     *
     * @param   name        The name of the indicator to get the capabilities
     *                      for.  MAY NOT BE NULL.
     * @param   cycleRate   The cycle rate to set.  Must be a value returned by
     *                      getIndicatorCaps() for this indicator.
     *
     * @remarks This routine is used to set the cycle rate of the indicator
     *          specified.
     */
    ri_bool (*setIndicatorBlinkIter)(char* name, unsigned int cycleRate);

    /**
     * This method is used to set percentage of time that the specified indicator
     * is "on" during a blink cycle.
     *
     * @return  TRUE if the passed name corresponds to an known indicator, FALSE
     *          if not.  Also, will return FALSE if the cycleRate passed is
     *          invalid.
     *
     * @param   name        The name of the indicator to get the capabilities
     *                      for.  MAY NOT BE NULL.
     * @param   percentOn   The percentage the indicator is to be "on" in a blink
     *                      cycle, from 0 to 100.
     *
     * @remarks This routine is used to set the percentage of time in a blink cycle
     *          that an LED is to be "on".  The ClientSim starts the cycle with the
     *          LED on, then turns it off after this percentage of time has
     *          elapsed.
     */
    ri_bool (*setIndicatorBlinkOn)(char* name, unsigned int percentOn);

    /**
     * This method is used to get the text area capabilities.
     *
     * @param   rows                A location to receive the number of rows
     *                              supported by the display.  May be NULL.
     * @param   columns             A location to receive the number of columns
     *                              supported by the display.  May be NULL.
     * @param   characters          A location to recieve a pointer to a null-
     *                              terminated array of the characters supported
     *                              by the display.  May be NULL.
     * @param   maxHorizScrollIters A location to receive the maximum number of
     *                              times per minute that characters can scroll
     *                              across the display per minute.  0 indicates
     *                              that horizontal scrolling is not supported.
     *                              May be NULL.
     * @param   maxVertScrollIters  A location to receive the maximum number of
     *                              times per minute that characters can scroll
     *                              up the display per minute.  0 indicates
     *                              that vertical scrolling is not supported.  May
     *                              be NULL.
     *
     * @remarks This routine is used to get the text-specific capabilities
     *          supported by the text display.  Other capabilities that are
     *          shared with generic "bit"-type LEDs are obtained using
     *          getIndicatorCaps() with the name MPE_FP_INDICATOR_TEXT.
     */
    void (*getTextCaps)(unsigned int* rows, unsigned int* columns,
            const char** characters, unsigned int* maxHorizScrollIters,
            long* maxVertScrollIters);

    /**
     * This method is used to get the scroll settings for the text display.
     *
     * @param   horizIters  A location to receive the number of times a character
     *                      will scroll across the screen in a minute.  May be
     *                      NULL.
     * @param   vertIters   A location to receive the number of times a line
     *                      will scroll up the screen in a minute.  May be
     *                      NULL.
     * @param   delayTime  The percentage of time that a scroll operation will
     *                     delay movement during each segment of scrolling.
     *
     * @remarks This routine is used to determine the current settings of the
     *          text display that are unique to text displays.  Settings that are
     *          common between text and "bit"-type LEDs are obtained using the
     *          getIndicator() method using the name MPE_FP_INDICATOR_TEXT.
     */
    void (*getTextAttributes)(long* horizIters, long* vertIters,
            unsigned int* delayTime);

    /**
     * This method is used to set the horizontal scroll rate of the text display.
     *
     * @param   horizIters  The number of times in a minute that a character would
     *                      scroll across the display.
     *
     * @remarks This routine is used to set the horizontal scroll rate of a text
     *          display.  The parameter passed in represents the number of times
     *          in a minute that a character would take to move across the display.
     */
    void (*setTextHorizScroll)(unsigned int horizIters);

    /**
     * This method is used to set the vertical scroll rate of the text display.
     *
     * @param   vertIters   The number of times in a minute that a line would
     *                      scroll up the display.
     *
     * @remarks This routine is used to set the vertical scroll rate of a text
     *          display.  The parameter passed in represents the number of times
     *          in a minute that a line would take to move up the display.
     */
    void (*setTextVertScroll)(unsigned int vertIters);

    /**
     * This method is used to set the delay time of scrolling.
     *
     * @param   delayTime   The percentage of time that scrolling will pause on
     *                      a character or row before continuing to scroll.  Must
     *                      be between 0 and 100.
     *
     * @remarks This routine is used to set the vertical scroll rate of a text
     *          display.  The parameter passed in represents the number of times
     *          in a minute that a line would take to move up the display.
     */
    void (*setTextScrollDelay)(unsigned int delayTime);

    /**
     * This method is used to get the current mode of the text display.
     *
     * @param   textMode    A location to get the current mode of the text
     *                      display.  One of either RI_FP_MODE_12H_CLOCK,
     *                      RI_FP_MODE_24H_CLOCK, or RI_FP_MODE_STRING.
     *
     * @remarks The mode of the text display indicates whether it is displaying a
     *          12 hour clock, a 24 hour clock, or a text message.
     */
    void (*getTextMode)(RI_FP_DISPLAY_MODE* textMode);

    /**
     * This method is used to set the mode of the text display.
     *
     * @param   textMode    The new mode of the text display.  One of either
     *                      RI_FP_MODE_12H_CLOCK, RI_FP_MODE_24H_CLOCK, or
     *                      RI_FP_MODE_STRING.
     *
     * @remarks Sets the mode of the display to display either a 12 hour clock,
     *          a 24 hour clock, or a custom text message.
     */
    void (*setTextMode)(RI_FP_DISPLAY_MODE textMode);

    /**
     * This method is used to get the current text message for the text display.
     *
     * @param   text    A location to get a pointer to the array of asciz strings
     *                  set to display when the text display is in
     *                  FP_MODE_STRING mode.  A NULL pointer signals the
     *                  end of the array.
     *
     * @remarks The array of strings returned are the messages displayed, one per
     *          display row, on the text display when it is in
     *          FP_MODE_STRING mode.  If there are more strings than the
     *          number of rows available, then the extra rows can be shown by
     *          adjusting the scroll settings of the display using
     *          setTextVertScroll().
     *
     * @warning When the caller is done using the string data, it MUST be released
     *          using freeTextStrings().
     */
    unsigned int (*getTextStrings)(char*** text);

    /**
     * This method is used to release the memory obtained by a call to
     * getTextStrings().  Every call to getTextStrings() MUST be followed
     * by a call to this method.
     *
     * @param   text   The returned pointer from a call to getTextStrings().
     *
     * @remarks Releases the memory allocated for the user during a call to
     *          getTextStrings() to make that data thread-safe,
     */
    void (*freeTextStrings)(char** text);

    /**
     * This method is used to set the strings displayed in the text display's rows.
     *
     * @param   numLines    The number of lines of text.
     * @param   text        And array of asciz strings that will be displayed in
     *                      the rows of the text display.  The array entry after
     *                      the last string must be NULL to signal the end of the
     *                      array.  This data is copied to internal structures.
     *
     * @remarks Sets the strings for the rows of the text display.  May have more
     *          entries in the array than rows, which will be displayed by setting
     *          up vertical scrolling using setTextVertScroll().
     */
    void (*setTextStrings)(unsigned int numlines, const char** text);

    // Front Panel data.
    ri_frontpanel_data_t *ri_frontpanel_data;
};

/**
 * Type definition for Front Panel LED.
 */
typedef struct ri_led_s ri_led_t;

/**
 * Type definition for Front Panel Text LED.
 */
typedef struct ri_textled_s ri_textled_t;

/**
 * A Front Panel LED object.
 */
struct ri_led_s
{
    // Attributes.
    unsigned int m_numBrightnesses;
    RI_FP_INDICATOR_COLOR m_colorsSupported;
    unsigned int m_maxCycleRate;

    unsigned int m_brightnessLevel;
    RI_FP_INDICATOR_COLOR m_color;
    unsigned int m_cycleRate;
    unsigned int m_percentageOn;
    char* m_name;
    ri_bool m_on;
    unsigned int m_timeOn;
    unsigned int m_timeOff;
    ri_bool m_blinking;
    void *m_privateData;

    /**
     * Callback function for blink timer; period of on delay.
     *
     * @param data The callback data passed to the function on time-out.
     */
    ri_bool (*blinkTimerOnProc)(void *data);
    unsigned int m_blinkTimerOnId;

    /**
     * Callback function for blink timer; period of off delay.
     *
     * @param data The callback data passed to the function on time-out.
     */
    ri_bool (*blinkTimerOffProc)(void *data);
    unsigned int m_blinkTimerOffId;
};

/**
 * A Front Panel Text LED object.
 */
struct ri_textled_s
{
    ri_led_t m_base; // MUST be the first element in the structure.
    unsigned int m_maxHorizScrollsPerMinute;
    unsigned int m_rowCount;
    unsigned int m_columnCount;
    char *m_supportedChars;
    unsigned int m_maxHorizScrollIters;
    long m_maxVertScrollIters;
    long m_horizIters;
    long m_vertIters;
    unsigned int m_delayTime;
    unsigned int m_scrollOnTime;
    unsigned int m_scrollOffTime;
    unsigned int m_scrollIsOn;
    RI_FP_DISPLAY_MODE m_textMode;
    char* m_text;
    void *m_privateData;

    /**
     * Callback function for horizontal scroll timer; period of on delay.
     *
     * @param data The callback data passed to the function on time-out.
     */
    ri_bool (*scrollTimerOnProc)(void *data);
    unsigned int m_scrollTimerOnId;

    /**
     * Callback function for horizontal scroll timer; period of off delay.
     *
     * @param data The callback data passed to the function on time-out.
     */
    ri_bool (*scrollTimerOffProc)(void *data);
    unsigned int m_scrollTimerOffId;
};

// Indicators
#define RI_FP_NO_IND		0x00
#define	RI_FP_IRR_IND		0x01			// Infra red reciever.
#define	RI_FP_POWER_IND		0x02			// power led indicator.
#define	RI_FP_MSG_IND		0x04			// message led indicator.
#define	RI_FP_RFBYPASS_IND 	0x08			// rf bypass led indicator.
#endif /* _RI_FRONTPANEL_H_ */
