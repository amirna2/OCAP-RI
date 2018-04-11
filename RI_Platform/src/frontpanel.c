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

// Include system header files.
#include <stdlib.h>
#include <glib.h>

// Include RI Platform header files.
#include <ri_log.h>
#include <ri_config.h>
#include "frontpanel.h"
#include "ui_frontpanel_porting.h"

//lint -sem( g_hash_table_insert, custodial(3) )

// Front Panel logging.
#define RILOG_CATEGORY riFrontPanelCat
log4c_category_t *riFrontPanelCat = NULL;

// Front Panel object information.
struct ri_frontpanel_data_s
{
    // The indicator list.
    GHashTable *ledNameMap;
    // The indicator list mutex.
    GMutex *ledNameMapMutex;
};

// The singleton instance of the Front Panel.
static ri_frontpanel_t *g_frontpanel_instance = NULL;

// Text LED Display
const unsigned int FP_LED_IND_DISPLAY = 0;
const unsigned int FP_LED_IND_POWER = 1;
const unsigned int FP_LED_IND_MESSAGE = 2;
const unsigned int FP_LED_IND_BYPASS = 3;
const unsigned int FP_LED_IND_IRR = 4;
const unsigned int FP_LED_MAX_INDICATORS = 5;

// Timers
const unsigned int FP_LED_SCROLL_TIMER_ID = 0x0B;
const unsigned int FP_LED_CLOCK_TIMER_ID = 0x0C;

// Front Panel API implementations - forward declarations.
unsigned int fp_getIndicatorList(char*** names);
void fp_freeIndicatorList(char** names);
ri_bool fp_getIndicatorCaps(char* name, unsigned int* numBrightnesses,
        RI_FP_INDICATOR_COLOR* colors, unsigned int* maxCycleRate);
ri_bool fp_getIndicator(char* name, unsigned int* brightLevel,
        RI_FP_INDICATOR_COLOR* color, unsigned int* cycleRate,
        unsigned int* percentOn);
ri_bool fp_setIndicatorBrightLevel(char* name, unsigned int brightLevel);
ri_bool fp_setIndicatorColor(char* name, RI_FP_INDICATOR_COLOR color);
ri_bool fp_setIndicatorBlinkIter(char* name, unsigned int cycleRate);
ri_bool fp_setIndicatorBlinkOn(char* name, unsigned int percentOn);
void fp_getTextCaps(unsigned int* rows, unsigned int* columns,
        const char** characters, unsigned int* maxHorizScrollIters,
        long* maxVertScrollIters);
void fp_getTextAttributes(long* horizIters, long* vertIters,
        unsigned int* delayTime);
void fp_setTextHorizScroll(unsigned int horizIters);
void fp_setTextVertScroll(unsigned int vertIters);
void fp_setTextScrollDelay(unsigned int delayTime);
void fp_getTextMode(RI_FP_DISPLAY_MODE* textMode);
void fp_setTextMode(RI_FP_DISPLAY_MODE textMode);
unsigned int fp_getTextStrings(char*** text);
void fp_freeTextStrings(char** text);
void fp_setTextStrings(unsigned int numlines, const char** text);

// LED support utilities - forward declarations.
static uint32_t getLEDNameList(char ***names);
static void freeLEDNameList(char** names);
static ri_led_t *getLEDByName(const char* name);
static void lockLEDNameMap();
static void unlockLEDNameMap();
static void setFirstAvailableColor(ri_led_t *led);
static void getLEDCaps(ri_led_t *led, unsigned int *numBrightnesses,
        RI_FP_INDICATOR_COLOR *colors, unsigned int *maxCycleRate);
static void getLEDSettings(ri_led_t *led, unsigned int *brightLevel,
        RI_FP_INDICATOR_COLOR *color, unsigned int *cycleRate,
        unsigned int *percentOn);
static ri_bool setBrightnessLevel(ri_led_t *led, unsigned int brightLevel);
static ri_bool setColor(ri_led_t *led, RI_FP_INDICATOR_COLOR color);
static ri_bool setBlinkIterations(ri_led_t *led, unsigned int cycleRate);
static ri_bool setBlinkOnPercentage(ri_led_t *led, unsigned int percentOn);
static void resetLED(ri_led_t *led);
static void getTextCaps(ri_textled_t *led, unsigned int *rows,
        unsigned int *columns, const char **characters,
        unsigned int *maxHorizScrollIters, long *maxVertScrollIters);
static void getTextAttributes(ri_textled_t *led, long *horizIters,
        long *vertIters, unsigned int *delayTime);
static void setTextHorizScroll(ri_textled_t *led, unsigned int horizIters);
static void setTextVertScroll(ri_textled_t *led, unsigned int vertIters);
static void resetScrollTimerValues(ri_textled_t *led);
static void setTextScrollDelay(ri_textled_t *led, unsigned int delayTime);
static void getTextMode(ri_textled_t *led, RI_FP_DISPLAY_MODE* textMode);
static void setTextMode(ri_textled_t *led, RI_FP_DISPLAY_MODE textMode);
static unsigned int getTextStrings(ri_textled_t *led, char*** text);
static void freeTextStrings(ri_textled_t *led, char** text);
static void setTextStrings(ri_textled_t *led, unsigned int numlines,
        const char** text);

static void initLEDDisplays(int numberOfLEDs);
static void initTextLEDDisplays(int numberOfTextLEDs);
static void resetTextLEDInternals(ri_textled_t *led);

static gboolean blink_timer_on_proc(gpointer data);
static gboolean blink_timer_off_proc(gpointer data);
static gboolean scroll_timer_on_proc(gpointer data);
static gboolean scroll_timer_off_proc(gpointer data);

static void destroy_leds(void);

ri_frontpanel_t *create_frontpanel()
{
    char *cfgValue;
    riFrontPanelCat = log4c_category_get("RI.FrontPanel");
    RILOG_INFO("%s -- Entry\n", __FUNCTION__);

    ri_frontpanel_t *fp = g_try_malloc0(sizeof(ri_frontpanel_t));

    if (NULL == fp)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    // Associate the methods defined in the plugin.
    fp->getIndicatorList = fp_getIndicatorList;
    fp->freeIndicatorList = fp_freeIndicatorList;
    fp->getIndicatorCaps = fp_getIndicatorCaps;
    fp->getIndicator = fp_getIndicator;
    fp->setIndicatorBrightLevel = fp_setIndicatorBrightLevel;
    fp->setIndicatorColor = fp_setIndicatorColor;
    fp->setIndicatorBlinkIter = fp_setIndicatorBlinkIter;
    fp->setIndicatorBlinkOn = fp_setIndicatorBlinkOn;
    fp->getTextCaps = fp_getTextCaps;
    fp->getTextAttributes = fp_getTextAttributes;
    fp->setTextHorizScroll = fp_setTextHorizScroll;
    fp->setTextVertScroll = fp_setTextVertScroll;
    fp->setTextScrollDelay = fp_setTextScrollDelay;
    fp->getTextMode = fp_getTextMode;
    fp->setTextMode = fp_setTextMode;
    fp->getTextStrings = fp_getTextStrings;
    fp->freeTextStrings = fp_freeTextStrings;
    fp->setTextStrings = fp_setTextStrings;

    // Allocate object memory.
    fp->ri_frontpanel_data = g_try_malloc(sizeof(ri_frontpanel_data_t));

    if (NULL == fp->ri_frontpanel_data)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    memset(fp->ri_frontpanel_data, 0, sizeof(ri_frontpanel_data_t));

    // Initialize internal Front Panel implementation.
    fp->ri_frontpanel_data->ledNameMap = g_hash_table_new(g_str_hash,
            g_str_equal);
    fp->ri_frontpanel_data->ledNameMapMutex = g_mutex_new();

    // Set global instance of Front Panel; must be done before we attempt to populate
    // the LED displays.
    g_frontpanel_instance = fp;

    // Initialize the LED Displays
    if ((cfgValue = ricfg_getValue("RIPlatform",
            "RI.Platform.frontpanel.number_of_leds")) != NULL)
    {
        int numberOfLEDs = atoi(cfgValue);
        RILOG_INFO("Number of LED displays: %d\n", numberOfLEDs);
        initLEDDisplays(numberOfLEDs);
    }

    // Initialize the Text LED Displays.
    if ((cfgValue = ricfg_getValue("RIPlatform",
            "RI.Platform.frontpanel.number_of_textleds")) != NULL)
    {
        int numberOfTextLEDs = atoi(cfgValue);
        RILOG_INFO("Number of Text LED displays: %d\n", numberOfTextLEDs);
        initTextLEDDisplays(numberOfTextLEDs);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return fp;
}

void destroy_frontpanel(ri_frontpanel_t *fp)
{
    if (g_frontpanel_instance != NULL)
    {
        if (g_frontpanel_instance->ri_frontpanel_data != NULL)
        {
            destroy_leds();
            g_hash_table_destroy(
                    g_frontpanel_instance->ri_frontpanel_data->ledNameMap);
            g_mutex_free(
                    g_frontpanel_instance->ri_frontpanel_data->ledNameMapMutex);

            g_free(g_frontpanel_instance->ri_frontpanel_data);
        }
        g_free(g_frontpanel_instance);
        g_frontpanel_instance = NULL;
    }
}

ri_frontpanel_t *get_frontpanel()
{
    return g_frontpanel_instance;
}

unsigned int fp_getIndicatorList(char*** names)
{
    return getLEDNameList(names);
}

void fp_freeIndicatorList(char** names)
{
    freeLEDNameList(names);
}

ri_bool fp_getIndicatorCaps(char* name, unsigned int* numBrightnesses,
        RI_FP_INDICATOR_COLOR* colors, unsigned int* maxCycleRate)
{
    ri_led_t *led = getLEDByName(name);

    // If it doesn't exist, return FALSE.
    if (NULL == led)
    {
        return FALSE;
    }

    // Got it.  Get the data.
    getLEDCaps(led, numBrightnesses, colors, maxCycleRate);
    return TRUE;
}

ri_bool fp_getIndicator(char* name, unsigned int* brightLevel,
        RI_FP_INDICATOR_COLOR* color, unsigned int* cycleRate,
        unsigned int* percentOn)
{
    ri_led_t *led = getLEDByName(name);

    // If it doesn't exist, return FALSE.
    if (NULL == led)
    {
        return FALSE;
    }

    // Got it.  Get the data.
    getLEDSettings(led, brightLevel, color, cycleRate, percentOn);
    return TRUE;
}

ri_bool fp_setIndicatorBrightLevel(char* name, unsigned int brightLevel)
{
    ri_led_t *led = getLEDByName(name);

    // If it doesn't exist, return FALSE.
    if (NULL == led)
    {
        return FALSE;
    }

    // Got it.  Set the data.
    return setBrightnessLevel(led, brightLevel);
}

ri_bool fp_setIndicatorColor(char* name, RI_FP_INDICATOR_COLOR color)
{
    ri_led_t *led = getLEDByName(name);

    // If it doesn't exist, return FALSE.
    if (NULL == led)
    {
        return FALSE;
    }

    // Got it.  Set the data.
    return setColor(led, color);
}

ri_bool fp_setIndicatorBlinkIter(char* name, unsigned int cycleRate)
{
    ri_led_t *led = getLEDByName(name);

    // If it doesn't exist, return FALSE.
    if (NULL == led)
    {
        return FALSE;
    }

    // Got it.  Set the data.
    return setBlinkIterations(led, cycleRate);
}

ri_bool fp_setIndicatorBlinkOn(char* name, unsigned int percentOn)
{
    ri_led_t *led = getLEDByName(name);

    // If it doesn't exist, return FALSE.
    if (NULL == led)
    {
        return FALSE;
    }

    // Got it.  Set the data.
    return setBlinkOnPercentage(led, percentOn);
}

void fp_getTextCaps(unsigned int* rows, unsigned int* columns,
        const char** characters, unsigned int* maxHorizScrollIters,
        long* maxVertScrollIters)
{
    // Get the text LED object. Note that this assumes that that there
    // is only one text LED display and that it is named "text".
    ri_led_t *led = getLEDByName("text");

    // No text field.
    if (NULL == led)
    {
        *rows = 0;
        *columns = 0;
        *characters = "";
        *maxHorizScrollIters = ~0;
        *maxVertScrollIters = -1;
        return;
    }

    // Got the text field, up-cast to the Text LED.
    ri_textled_t *textLED = (ri_textled_t *) led;

    // Get the data.
    getTextCaps(textLED, rows, columns, characters, maxHorizScrollIters,
            maxVertScrollIters);
}

void fp_getTextAttributes(long* horizIters, long* vertIters,
        unsigned int* delayTime)
{
    // Get the text LED object
    ri_led_t *led = getLEDByName("text");

    // No text field.
    if (NULL == led)
    {
        *horizIters = -1;
        *vertIters = -1;
        *delayTime = 0;
        return;
    }

    // Got the text field, up-cast to the Text LED.
    ri_textled_t *textLED = (ri_textled_t *) led;

    // Get the data.
    getTextAttributes(textLED, horizIters, vertIters, delayTime);
}

void fp_setTextHorizScroll(unsigned int horizIters)
{
    // Get the text LED object.
    ri_led_t *led = getLEDByName("text");

    // No text field.
    if (NULL == led)
    {
        return;
    }

    // Got the text field, up-cast to the Text LED.
    ri_textled_t *textLED = (ri_textled_t *) led;

    // Get the data.
    setTextHorizScroll(textLED, horizIters);
}

void fp_setTextVertScroll(unsigned int vertIters)
{
    // Get the text LED object.
    ri_led_t *led = getLEDByName("text");

    // No text field.
    if (NULL == led)
    {
        return;
    }

    // Got the text field, up-cast to the Text LED.
    ri_textled_t *textLED = (ri_textled_t *) led;

    // Get the data.
    setTextVertScroll(textLED, vertIters);
}

void fp_setTextScrollDelay(unsigned int delayTime)
{
    // Get the text LED object.
    ri_led_t *led = getLEDByName("text");

    // No text field.
    if (NULL == led)
    {
        return;
    }

    // Got the text field, up-cast to the Text LED.
    ri_textled_t *textLED = (ri_textled_t *) led;

    // Get the data.
    setTextScrollDelay(textLED, delayTime);
}

void fp_getTextMode(RI_FP_DISPLAY_MODE* textMode)
{
    // Get the text LED object
    ri_led_t* led = getLEDByName("text");

    // No text field.
    if (NULL == led)
    {
        *textMode = RI_FP_MODE_12H_CLOCK;
        return;
    }

    // Got the text field, up-cast to the Text LED.
    ri_textled_t *textLED = (ri_textled_t *) led;

    // Get the data.
    getTextMode(textLED, textMode);
}

void fp_setTextMode(RI_FP_DISPLAY_MODE textMode)
{
    // Get the text LED object.
    ri_led_t *led = getLEDByName("text");

    // No text field.
    if (NULL == led)
    {
        return;
    }

    // Got the text field, up-cast to the Text LED.
    ri_textled_t *textLED = (ri_textled_t *) led;

    // Get the data.
    setTextMode(textLED, textMode);
}

unsigned int fp_getTextStrings(char*** text)
{
    // Get the text LED object.
    ri_led_t *led = getLEDByName("text");

    // No text field.
    if (NULL == led)
    {
        *text = NULL;
        return 0;
    }

    // Got the text field, up-cast to the Text LED.
    ri_textled_t *textLED = (ri_textled_t *) led;

    // Get the data.
    return getTextStrings(textLED, text);
}

void fp_freeTextStrings(char** text)
{
    // Get the text LED object.
    ri_led_t *led = getLEDByName("text");

    // No text field.
    if (NULL == led)
    {
        return;
    }

    // Got the text field, up-cast to the Text LED.
    ri_textled_t *textLED = (ri_textled_t *) led;

    // Get the data.
    freeTextStrings(textLED, text);
}

void fp_setTextStrings(unsigned int numlines, const char** text)
{
    // Get the text LED object.
    ri_led_t *led = getLEDByName("text");

    // No text field.
    if (NULL == led)
    {
        return;
    }

    // Got the text field, up-cast to the Text LED.
    ri_textled_t *textLED = (ri_textled_t *) led;

    // Get the data.
    setTextStrings(textLED, numlines, text);
}

void create_led(const char* name, unsigned int numBrightnesses,
        RI_FP_INDICATOR_COLOR colors, unsigned int maxCycleRate)
{
    ri_led_t *led = NULL;

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    led = (ri_led_t *) g_try_malloc(sizeof(ri_led_t));
    if (led != NULL)
    {
        led->m_numBrightnesses = numBrightnesses;
        led->m_colorsSupported = colors;
        led->m_maxCycleRate = maxCycleRate;
        led->m_brightnessLevel = 1; // Default off
        led->m_cycleRate = 0; // Default always on
        led->m_percentageOn = 100; // Default no blinking
        led->m_on = FALSE; // Display off by default.
        led->m_blinking = FALSE; // Display is not blinking to start
        //led->m_onLock = g_mutex_new();
        led->m_privateData = g_mutex_new();

        // Set up the name.
        led->m_name = (char *) g_try_malloc(sizeof(char) * strlen(name) + 1);
        if (led != NULL)
        {
            strcpy(led->m_name, name);
        }

        // Choose the first color.
        //lint --ffc
        setFirstAvailableColor(led);
        //lint ++ffc

        // Set the blink timer callbacks.
        led->blinkTimerOnProc = blink_timer_on_proc;
        led->m_blinkTimerOnId = ~0;
        led->blinkTimerOffProc = blink_timer_off_proc;
        led->m_blinkTimerOffId = ~0;

        // Tell the UI that there is a new LED.
        frontpanel_create_led(led);

        // Add this to the global list of LEDs.
        lockLEDNameMap();
        g_hash_table_insert(
                g_frontpanel_instance->ri_frontpanel_data->ledNameMap,
                led->m_name, led);
        unlockLEDNameMap();
    }
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

void destroy_led(ri_led_t *led)
{
    if (led == NULL)
        return;

    // Remove the led from the UI.
    frontpanel_destroy_led(led);

    // Remove the led from the global list of LEDs.
    lockLEDNameMap();
    (void) g_hash_table_remove(
            g_frontpanel_instance->ri_frontpanel_data->ledNameMap, led->m_name);
    unlockLEDNameMap();

    if (led->m_name != NULL)
        g_free(led->m_name);

    if (led->m_privateData != NULL)
        g_mutex_free((GMutex *) led->m_privateData);

    g_free(led);
}

void create_textled(const char* name, unsigned int numBrightnesses,
        RI_FP_INDICATOR_COLOR colors, unsigned int maxCycleRate,
        unsigned int maxHorizScrollsPerMinute)
{
    ri_textled_t *led = NULL;

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    led = (ri_textled_t *) g_try_malloc(sizeof(ri_textled_t));
    if (led != NULL)
    {
        led->m_maxHorizScrollsPerMinute = maxHorizScrollsPerMinute;

        // Initialize common LED parameters.
        led->m_base.m_numBrightnesses = numBrightnesses;
        led->m_base.m_colorsSupported = colors;
        led->m_base.m_maxCycleRate = maxCycleRate;
        led->m_base.m_brightnessLevel = 1; // Default off
        led->m_base.m_cycleRate = 0; // Default always on
        led->m_base.m_percentageOn = 100; // Default no blinking
        led->m_base.m_on = FALSE; // Display off by default.
        led->m_base.m_blinking = FALSE; // Display is not blinking to start
        //led->m_base.m_onLock = g_mutex_new();
        led->m_base.m_privateData = g_mutex_new();

        // Set up the name.
        led->m_base.m_name = (char *) g_try_malloc(sizeof(char) * strlen(name) + 1);
        if (led->m_base.m_name != NULL)
        {
            strcpy(led->m_base.m_name, name);
        }

        // Choose the first color.
        //lint --ffc
        setFirstAvailableColor(&(led->m_base));
        //lint ++ffc

        // Set the blink timer callbacks.
        led->m_base.blinkTimerOnProc = blink_timer_on_proc;
        led->m_base.m_blinkTimerOnId = ~0;
        led->m_base.blinkTimerOffProc = blink_timer_off_proc;
        led->m_base.m_blinkTimerOffId = ~0;
        // Set the horizontal scroll callbacks.
        led->scrollTimerOnProc = scroll_timer_on_proc;
        led->m_scrollTimerOnId = ~0;
        led->scrollTimerOffProc = scroll_timer_off_proc;
        led->m_scrollTimerOffId = ~0;

        // Initialize variables that will eventually become settable parameters.
        led->m_rowCount = 1;
        led->m_columnCount = 4;
        led->m_supportedChars = "ABCDEFGHIJLNOPQRSTUYabcdefghijlnopqrstuy-_";
        led->m_maxVertScrollIters = -1;
        led->m_maxHorizScrollIters = 60; // Maximum 60 scroll iterations per second.

        // Initialize internal state.
        led->m_horizIters = 0; // No horizontal scroll to start.
        led->m_vertIters = (-1 == led->m_maxVertScrollIters) ? -1 : 0; // No vertical scroll.
        led->m_delayTime = 0; // No delay between scrolls.
        led->m_text = NULL;
        led->m_privateData = NULL;

        // Do common resets between construction time and restart/reload.
        //lint --ffc
        resetTextLEDInternals(led);
        //lint ++ffc

        // Tell the UI that there is a new LED.
        frontpanel_create_textled(led);

        // Add this to the global list of LEDs.
        lockLEDNameMap();
        g_hash_table_insert(
                g_frontpanel_instance->ri_frontpanel_data->ledNameMap,
                led->m_base.m_name, led);
        unlockLEDNameMap();
    }
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

void destroy_textled(ri_textled_t *led)
{
    if (led == NULL)
        return;

    // Remove the led from the UI.
    frontpanel_destroy_textled(led);

    // Remove the led from the global list of LEDs.
    lockLEDNameMap();
    (void) g_hash_table_remove(
            g_frontpanel_instance->ri_frontpanel_data->ledNameMap,
            led->m_base.m_name);
    unlockLEDNameMap();

    if (led->m_base.m_name != NULL)
        g_free(led->m_base.m_name);

    g_free(led);
}

static void destroy_leds(void)
{
    int i;
    ri_led_t *led = NULL;

    // Lock the name map.
    lockLEDNameMap();

    // Allocate the array to return.
    int nameListSize = g_hash_table_size(
            g_frontpanel_instance->ri_frontpanel_data->ledNameMap);

    // Fill in the names.
    GHashTableIter iter;
    g_hash_table_iter_init(&iter,
            g_frontpanel_instance->ri_frontpanel_data->ledNameMap);

    for (i = 0; i < nameListSize; ++i)
    {
        gpointer key;
        (void) g_hash_table_iter_next(&iter, &key, (gpointer) & led);
        (void) g_hash_table_remove(
                g_frontpanel_instance->ri_frontpanel_data->ledNameMap,
                (char *) key);
        destroy_led(led);
    }

    // Unlock the list.
    unlockLEDNameMap();
}

/**
 * This method is used to get an array of character string names that
 * correspond to the LEDs on the front panel.
 *
 * @return  A pointer to the array of char* strings.
 *
 * @param   name    The location to get the pointer to the created array
 *                  of char*.
 *
 * @remarks This routine gets the current set of LED names known to the
 *          system.  Note that this set may change at any time due to
 *          the ability of editing led names and adding and removing leds.
 *
 * @warning The memory allocated for this list MUST be freed by calling
 *          freeLEDNameList() with the returned pointer.
 */
uint32_t getLEDNameList(char*** names)
{
    int i;

    // Lock the name map.
    lockLEDNameMap();

    // Allocate the array to return.
    int nameListSize = g_hash_table_size(
            g_frontpanel_instance->ri_frontpanel_data->ledNameMap);
    //*names = new char*[ nameListSize + 1 ];
    *names = (char **) g_try_malloc(sizeof(char *) * (nameListSize + 1));

    if (NULL == names)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    // This will let us know how big the array is when we go to free it.
    (*names)[nameListSize] = NULL;

    // Fill in the names.
    GHashTableIter iter;
    g_hash_table_iter_init(&iter,
            g_frontpanel_instance->ri_frontpanel_data->ledNameMap);
    const char* p;
    for (i = 0; i < nameListSize; ++i)
    {
        gpointer key, value;
        (void) g_hash_table_iter_next(&iter, &key, &value);

        p = (char *) key;
        //(*names)[ i ] = new char[ strlen( p ) + 1 ];
        (*names)[i] = (char *) g_try_malloc(sizeof(char) * (strlen(p) + 1));
        if ((*names)[i] != NULL)
        {
            strcpy((*names)[i], p);
        }
    }

    // Unlock the list.
    unlockLEDNameMap();

    // And return it's count.
    return nameListSize;
}

/**
 * This method is used to free the memory allocated by getLEDNameList()
 *
 * @param   name    The returned array from a call to getLEDNameList().
 *
 * @remarks This routine gets the current set of LED names known to the
 *          system.  Note that this set may change at any time due to
 *          the ability of editing led names and adding and removing leds.
 */
void freeLEDNameList(char** names)
{
    uint32_t count, i;

    // Skip a bad pointer.
    if (NULL == names)
    {
        return;
    }

    // Check the count of entries.
    for (count = 0; NULL != names[count]; ++count)
    {
    }

    // Free the name strings.
    for (i = 0; i < count; ++i)
    {
        g_free(names[i]);
    }

    // Free the allocated char* array.
    g_free(names);
}

/**
 * This method is used to get the LED object associated with the passed
 * name.
 *
 * @return  A pointer to the LED object to be manipulated.
 *
 * @remarks This routine gets the current set of LED names known to the
 *          system.  Note that this set may change at any time due to
 *          the ability of editing led names and adding and removing leds.
 *
 * @warning It is expected that the caller has called lockLEDNameMap()
 *          prior to calling this method.  Also the caller is expected to
 *          call unlockLEDNameMap() once it is done manipulating the
 *          returned LED object.
 */
ri_led_t *getLEDByName(const char* name)
{
    ri_led_t *led = NULL;

    lockLEDNameMap();
    led = g_hash_table_lookup(
            g_frontpanel_instance->ri_frontpanel_data->ledNameMap, name);
    unlockLEDNameMap();

    return led;
}

/**
 * Locks the map of LED names so that it doesn't change while it's in use.
 *
 * @remarks This routine locks the map of names to LedBase objects so that
 *          it doesn't change while an operation is being performed.  This
 *          is used primarily to prevent deletion of an LED between lookup
 *          using getLEDByName() and use of the object by way of its
 *          pointer.
 *
 * @warning unlockLEDNameMap() MUST be called after this method is called.
 */
void lockLEDNameMap()
{
    g_mutex_lock(g_frontpanel_instance->ri_frontpanel_data->ledNameMapMutex);
}

/**
 * Unlocks the map of LED names so that it can be updated.
 *
 * @remarks This routine unlocks the map of names to LedBase previously
 *          locked by lockLEDNameMap() so that it can be modified.
 *
 * @warning lockLEDNameMap() MUST be called prior to this method.
 */
void unlockLEDNameMap()
{
    g_mutex_unlock(g_frontpanel_instance->ri_frontpanel_data->ledNameMapMutex);
}

/**
 * This method is used to set the current LED color to be the first legal
 * color available.
 *
 * @remarks The "first color" is defined as the color with the lowest bit
 *          value that is set in m_colorsSupported.
 */
void setFirstAvailableColor(ri_led_t *led)
{
    unsigned long bit;

    // Choose the new first available color.
    led->m_color = RI_FP_NONE;
    for (bit = 0x01; bit <= RI_FP_MAX_COLOR; bit <<= 1)
    {
        // Is this color available?
        if (bit & led->m_colorsSupported)
        {
            led->m_color = (RI_FP_INDICATOR_COLOR) bit;
            // BWW - According to the doxygen comment I think there should be a break here.
            // Since there is not this function actually returns the highest color found,
            // not the lowest color as the comment states
        }
    }
}

/**
 * This method is used to obtain the capabilities of the LED.
 *
 * @param   numBrightnesses The number of brightness levels supported by the
 *                          LED.  May be NULL.
 * @param   colors          An or'd list of the available colors for the
 *                          LED.  May be NULL.
 * @param   maxCycleRate    The maximum number of times per minute that the
 *                          LED can be made to blink.  May be NULL.
 */
void getLEDCaps(ri_led_t *led, unsigned int *numBrightnesses,
        RI_FP_INDICATOR_COLOR *colors, unsigned int *maxCycleRate)
{
    // If requested, give the number of brightnesses.
    if (NULL != numBrightnesses)
    {
        *numBrightnesses = led->m_numBrightnesses;
    }

    // If requested, give the supported colors.
    if (NULL != colors)
    {
        *colors = led->m_colorsSupported;
    }

    // If requested, give the maximum cycle rate.
    if (NULL != maxCycleRate)
    {
        *maxCycleRate = led->m_maxCycleRate;
    }
}

/**
 * This method is used to get the various settings of the LED.
 *
 * @param   brightLevel The number of brightness level currently set.  May
 *                      be NULL.
 * @param   color       The current color.  May be NULL.
 * @param   cycleRate   The number of times per minute that the indicator
 *                      will blink.  May be NULL.
 * @param   percentOn   A percentage integer, from 0 to 100, of the
 *                      percentage of time that an LED will be on during a
 *                      blinking display.  0 is off, and 100 is always on.
 *                      May be NULL.
 */
void getLEDSettings(ri_led_t *led, unsigned int *brightLevel,
        RI_FP_INDICATOR_COLOR *color, unsigned int *cycleRate,
        unsigned int *percentOn)
{
    // If requested, give the number of brightnesses.
    if (NULL != brightLevel)
    {
        *brightLevel = led->m_brightnessLevel;
    }

    // If requested, give the supported colors.
    if (NULL != color)
    {
        *color = led->m_color;
    }

    // If requested, give the maximum cycle rate.
    if (NULL != cycleRate)
    {
        *cycleRate = led->m_cycleRate;
    }

    // If requested, give the percent on.
    if (NULL != percentOn)
    {
        *percentOn = led->m_percentageOn;
    }
}

/**
 * This method is used to set the brightness level.
 *
 * @return  TRUE if the brightness level is valid for the indicator, FALSE
 *          if not.  Also false if the indicator name is not known.
 *
 * @param   brightLevel The brightness level to set.  Must be >0 and < the
 *                      maximum returned by fp_getIndicatorCaps() for this
 *                      indicator.
 *
 * @remarks This routine is used to set the brightness level of the LED.
 *          This is an integer which may be in the range 1 to the
 *          numBrightnesses returned by getLEDCaps().  1 is off.
 */
ri_bool setBrightnessLevel(ri_led_t *led, unsigned int brightLevel)
{
    ri_bool ret = FALSE;

    // Check the range before setting.
    if ((1 <= brightLevel) && (led->m_numBrightnesses >= brightLevel))
    {
        // It's ok - set and return TRUE.
        led->m_brightnessLevel = brightLevel;
        ret = TRUE;

        // And make it do the right thing.
        resetLED(led);
    }

    return ret;
}

/**
 * This method is used to set the color of the LED.
 *
 * @return  TRUE if the color is valid for the indicator, FALSE if not.
 *
 * @param   color       The color to set.  Must be a value returned by
 *                      getLEDCaps().
 */
ri_bool setColor(ri_led_t *led, RI_FP_INDICATOR_COLOR color)
{
    ri_bool ret = FALSE;

    // Validate the color before setting.
    if (color & led->m_colorsSupported)
    {
        // It's ok - set and return TRUE.
        led->m_color = color;
        ret = TRUE;

        // And make it do the right thing.
        resetLED(led);
    }

    return ret;
}

/**
 * This method is used to set the number of times the LED will blink in a
 * minute.
 *
 * @return  TRUE if the passed cycleRate is valid, FALSE if not.
 *
 * @param   cycleRate   The cycle rate to set.  Must be a value less than or
 *                      equal to that returned by getLEDCaps().  0 is always
 *                      on.
 */
ri_bool setBlinkIterations(ri_led_t *led, unsigned int cycleRate)
{
    ri_bool ret = FALSE;

    // Check the range before setting.
    //if ((0 <= cycleRate) && (led->m_maxCycleRate >= cycleRate))
    // cycleRate always >= 0 (unsigned!)
    if (led->m_maxCycleRate >= cycleRate)
    {
        // It's ok - set and return TRUE.
        led->m_cycleRate = cycleRate;
        ret = TRUE;

        // Make sure the blinking is doing the right thing.
        resetLED(led);
    }

    return ret;
}

/**
 * This method is used to set the percentage of time that the LED is "on"
 * during a blink cycle.
 *
 * @return  TRUE if the percent value is valid, FALSE if not.
 *
 * @param   percentOn   The percentage the indicator is to be "on" in a
 *                      blink cycle, from 0 to 100.  0 is always off, 100
 *                      is always on.
 *
 * @remarks This routine is used to set the percentage of time in a blink
 *          cycle that the LED is to be "on".  The LED starts the cycle
 *          with the LED on, then turns it off after this percentage of
 *          time has elapsed.
 */
ri_bool setBlinkOnPercentage(ri_led_t *led, unsigned int percentOn)
{
    ri_bool ret = FALSE;

    // Check the range before setting.
    // if ((0 <= percentOn ) && (100 >= percentOn))
    // percentOn always >= 0 (unsigned!)
    if (100 >= percentOn)
    {
        // It's ok - set and return TRUE.
        led->m_percentageOn = percentOn;
        ret = TRUE;

        // Make sure the blinking is doing the right thing.
        resetLED(led);
    }

    return ret;
}

/**
 * Resets the LED functionality so the right thing happens based upon
 * current settings.
 *
 * @remarks This method handles making a change in blink settings do the
 *          right thing.
 */
void resetLED(ri_led_t *led)
{
    // Get the on/off control lock so no change in state occurs while we
    // figure out what to do.
    g_mutex_lock((GMutex *) led->m_privateData);

    // First, are we in a blinking state?
    if (led->m_blinking)
    {
        // If so, kill the current timer.
        // KillTimer( sm_hwnd, (DWORD) this );
        if (led->m_on)
            (void) g_source_remove(led->m_blinkTimerOnId);
        else
            (void) g_source_remove(led->m_blinkTimerOffId);
    }

    // Are we on?
    if (led->m_on)
    {
        // Turn off the display so we're in a known starting point.
        led->m_on = FALSE;
    }

    // Should we be blinking?
    if ((0 == led->m_cycleRate) || (100 <= led->m_percentageOn) || (0
            >= led->m_percentageOn))
    {
        // Not blinking.  Just turn it on at the specified brightness.
        // Actually, brightness 1 is off, as is a 0 percentageOn, so...
        if ((1 == led->m_brightnessLevel) || (0 >= led->m_percentageOn))
        {
            led->m_on = FALSE;
        }
        else
        {
            led->m_on = TRUE;
        }
    }
    else
    {
        // OK, we're in a blink mode.  We'll need to create a timer, and
        // set it going.

        // Compute the time that we'll be on.
        unsigned int totalTimeInACycle = 60000 / led->m_cycleRate;
        led->m_timeOn = (totalTimeInACycle * led->m_percentageOn) / 100;

        // Compute the time that we'll be off.
        led->m_timeOff = totalTimeInACycle - led->m_timeOn;

        // Turn on the LED.
        led->m_on = TRUE;

        // Start the on timer, using the callback below.
        // SetTimer( sm_hwnd, (DWORD) this, m_timeOn, BlinkTimerProc );
        led->m_blinkTimerOnId = g_timeout_add(led->m_timeOn,
                led->blinkTimerOnProc, led);
    }

    // Redisplay the LED based on these changes.
    frontpanel_update_indicator_display(led);

    // Release the lock.
    g_mutex_unlock((GMutex *) led->m_privateData);
}

/**
 * This method is used to get the text area capabilities.
 *
 * @param   rows                A location to receive the number of rows
 *                              supported by the display.  May be NULL.
 * @param   columns             A location to receive the number of columns
 *                              supported by the display.  May be NULL.
 * @param   characters          A location to receive a pointer to a null-
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
 *          fp_getIndicatorCaps() with the name MPE_FP_INDICATOR_TEXT.
 */
void getTextCaps(ri_textled_t *led, unsigned int *rows, unsigned int *columns,
        const char **characters, unsigned int *maxHorizScrollIters,
        long *maxVertScrollIters)
{
    // Get the settings.
    if (NULL != rows)
    {
        *rows = led->m_rowCount;
    }

    if (NULL != columns)
    {
        *columns = led->m_columnCount;
    }

    if (NULL != characters)
    {
        *characters = led->m_supportedChars;
    }

    if (NULL != maxHorizScrollIters)
    {
        *maxHorizScrollIters = led->m_maxHorizScrollIters;
    }

    if (NULL != maxVertScrollIters)
    {
        *maxVertScrollIters = led->m_maxVertScrollIters;
    }
}

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
 *          fp_getIndicator() method using the name MPE_FP_INDICATOR_TEXT.
 */
void getTextAttributes(ri_textled_t *led, long *horizIters, long *vertIters,
        unsigned int *delayTime)
{
    // Get the settings.
    if (NULL != horizIters)
    {
        *horizIters = led->m_horizIters;
    }

    if (NULL != vertIters)
    {
        *vertIters = led->m_vertIters;
    }

    if (NULL != delayTime)
    {
        *delayTime = led->m_delayTime;
    }
}

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
void setTextHorizScroll(ri_textled_t *led, unsigned int horizIters)
{
    // Verify validity.
    // if ((0 <= horizIters) && (led->m_maxHorizScrollIters >= horizIters))
    if (led->m_maxHorizScrollIters >= horizIters)
    {
        // If we had a horizontal scroll on, kill it.
        if (0 < led->m_horizIters)
        {
            // KillTimer(sm_hwnd, SCROLL_TIMER_ID);
            if (led->m_scrollIsOn)
                (void) g_source_remove(led->m_scrollTimerOnId);
            else
                (void) g_source_remove(led->m_scrollTimerOffId);
        }

        // Kill any blinking if there's scroll delay.
        if (0 != led->m_delayTime)
        {
            (void) setBlinkIterations((ri_led_t *) led, 0);
        }

        // Set the new value.
        led->m_horizIters = horizIters;

        // If we have scrolling to do, start the timer.
        if (0 != led->m_horizIters)
        {
            resetScrollTimerValues(led);

            // Start the scroll.
            // SetTimer( sm_hwnd, SCROLL_TIMER_ID, m_scrollOnTime, NULL );
            led->m_scrollTimerOnId = g_timeout_add(led->m_scrollOnTime,
                    led->scrollTimerOnProc, led);
        }

        // Render
        frontpanel_update_text_display(led);
    }
}

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
void setTextVertScroll(ri_textled_t *led, unsigned int vertIters)
{
    // Verify validity
    if (led->m_maxVertScrollIters >= ((long) vertIters))
    {
        // It's OK - set it.
        led->m_vertIters = vertIters;

        // Force a redisplay.
        frontpanel_update_text_display(led);
    }
}

void resetScrollTimerValues(ri_textled_t *led)
{
    // Compute the new ontime/offtime.
    //
    // TODO: Fix Me!
    // prevent DIV0!
    if ((0 == led->m_columnCount) || (0 == led->m_horizIters))
    {
        led->m_scrollOnTime = 0;
        led->m_scrollOffTime = 0;
    }
    else if (0 != led->m_delayTime)
    {
        // TODO: Fix Me!
        // I have no idea what this value is supposed to do -
        // design spec and OCAP spec seem to disagree.

        // For now, we ignore this value.
        led->m_scrollOnTime = (60000 / led->m_columnCount)
                / (unsigned) led->m_horizIters;
        led->m_scrollOffTime = 0;
    }
    else
    {
        led->m_scrollOnTime = (60000 / led->m_columnCount)
                / (unsigned) led->m_horizIters;
        led->m_scrollOffTime = 0;
    }
}

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
void setTextScrollDelay(ri_textled_t *led, unsigned int delayTime)
{
    // Verify validity
    if (100 >= delayTime)
    {
        // It's OK - set it.
        led->m_delayTime = delayTime;

        // Reset the timer values just in case we're already scrolling.
        resetScrollTimerValues(led);

        frontpanel_update_text_display(led);
    }
}

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
void getTextMode(ri_textled_t *led, RI_FP_DISPLAY_MODE *textMode)
{
    *textMode = led->m_textMode;
}

void setTextMode(ri_textled_t *led, RI_FP_DISPLAY_MODE textMode)
{
    led->m_textMode = textMode;
    switch (led->m_textMode)
    {
    case RI_FP_MODE_12H_CLOCK:
    case RI_FP_MODE_24H_CLOCK:
        frontpanel_update_text_display_clock(led); // mode can be extracted from the ri_textled_t
        break;
    case RI_FP_MODE_STRING:
        frontpanel_update_text_display_string(led); // text can be extracted from the ri_textled_t
        break;
    }

    // Render
    frontpanel_update_text_display(led);
}

unsigned int getTextStrings(ri_textled_t *led, char*** text)
{
    // Current array only supports one line.
    int arraySize = 1;
    int i;

    // Create enough array space for the array and an extra entry.
    //*text = new char*[ arraySize + 1 ];
    *text = (char **) g_try_malloc(sizeof(char *) * (arraySize + 1));

    if (NULL == text)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    // Get the strings we have - note that for now, it's fixed as one,
    // but if we go to multiple lines, we just need to change the
    // arraySize initialization above - this code will continue to work.
    for (i = 0; i < arraySize; ++i)
    {
        // Allocate space for the text.
        //(*text)[ i ] = new char[ strlen( m_text ) + 1 ];
        (*text)[i] = (char *) g_try_malloc(sizeof(char) * (strlen(led->m_text) + 1));

        if (NULL == (*text)[i])
        {
            RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                        __LINE__, __FILE__, __func__);
        }

        // Copy in the text.
        strcpy((*text)[i], led->m_text);
    }

    // And terminate with a null.
    (*text)[arraySize] = NULL;

    return arraySize;
}

void freeTextStrings(ri_textled_t *led, char** text)
{
    int count, j;

    // How many entries?
    for (count = 0; NULL != text[count]; ++count)
    {
    }

    // Delete the entries.
    for (j = 0; j < count; ++j)
    {
        //delete [] text[ j ];
        g_free(text[j]);
    }

    // Delete the table.
    //delete [] text;
    g_free(text);
}

void setTextStrings(ri_textled_t *led, unsigned int numlines, const char** text)
{
    if (0 == numlines)
    {
        //delete [] m_text;
        g_free(led->m_text);
        //m_text = new char[ 1 ];
        led->m_text = (char *) g_try_malloc(sizeof(char));
        if (led->m_text != NULL)
        {
            strcpy(led->m_text, "");
        }
    }
    else
    {
        //delete [] m_text;
        g_free(led->m_text);
        //m_text = new char [ strlen( text[ 0 ] ) + 1 ];
        led->m_text = (char *) g_try_malloc(sizeof(char) * (strlen(text[0]) + 1));
        if (led->m_text != NULL)
        {
            strcpy(led->m_text, text[0]);
        }
    }

    // Render
    frontpanel_update_text_display(led);
}

/**
 * Initialize the LED displays from the RI Platform configuration file.
 */
void initLEDDisplays(int numberOfLEDs)
{
    int i;
    char *cfgValue;
    char *cfgNameBase = "RI.Platform.frontpanel.led.";
    char cfgName[256];
    char cfgNameBaseLED[256];

    RILOG_INFO("%s -- Entry\n", __FUNCTION__);

    for (i = 0; i < numberOfLEDs; i++)
    {
        RI_FP_INDICATOR_COLOR colors = RI_FP_NONE;
        unsigned int brightnessLevels, maxBlinksPerMinute;
        //unsigned int xPos, yPos;
        //unsigned int width, height;
        char *name = NULL;

        char ledId[64];
        sprintf(ledId, "%d", i);

        // Determine base of configuration value name.
        sprintf(cfgNameBaseLED, "%s%s.", cfgNameBase, ledId);
        //RILOG_DEBUG("Configuration LED Base: %s\n", cfgNameBaseLED);

        // Does LED support the color blue?
        sprintf(cfgName, "%s%s", cfgNameBaseLED, "blue");
        if ((cfgValue = ricfg_getValue("RIPlatform", cfgName)) != NULL)
        {
            RILOG_DEBUG("%s: %s\n", cfgName, cfgValue);
            if (!strcmp(cfgValue, "TRUE"))
                colors = colors | RI_FP_BLUE;
        }
        // Does LED support the color green?
        sprintf(cfgName, "%s%s", cfgNameBaseLED, "green");
        if ((cfgValue = ricfg_getValue("RIPlatform", cfgName)) != NULL)
        {
            RILOG_DEBUG("%s: %s\n", cfgName, cfgValue);
            if (!strcmp(cfgValue, "TRUE"))
                colors = colors | RI_FP_GREEN;
        }
        // Does LED support the color yellow?
        sprintf(cfgName, "%s%s", cfgNameBaseLED, "yellow");
        if ((cfgValue = ricfg_getValue("RIPlatform", cfgName)) != NULL)
        {
            RILOG_DEBUG("%s: %s\n", cfgName, cfgValue);
            if (!strcmp(cfgValue, "TRUE"))
                colors = colors | RI_FP_YELLOW;
        }
        // Does LED support the color orange?
        sprintf(cfgName, "%s%s", cfgNameBaseLED, "orange");
        if ((cfgValue = ricfg_getValue("RIPlatform", cfgName)) != NULL)
        {
            RILOG_DEBUG("%s: %s\n", cfgName, cfgValue);
            if (!strcmp(cfgValue, "TRUE"))
                colors = colors | RI_FP_ORANGE;
        }
        // Does LED support the color red?
        sprintf(cfgName, "%s%s", cfgNameBaseLED, "red");
        if ((cfgValue = ricfg_getValue("RIPlatform", cfgName)) != NULL)
        {
            RILOG_DEBUG("%s: %s\n", cfgName, cfgValue);
            if (!strcmp(cfgValue, "TRUE"))
                colors = colors | RI_FP_RED;
        }

        // How many levels of brightness are supported?
        sprintf(cfgName, "%s%s", cfgNameBaseLED, "brightnesses");
        if ((cfgValue = ricfg_getValue("RIPlatform", cfgName)) != NULL)
        {
            RILOG_DEBUG("%s: %s\n", cfgName, cfgValue);
            brightnessLevels = atoi(cfgValue);
        }
        else
            brightnessLevels = 0;

        // How many blinks per minute are supported?
        sprintf(cfgName, "%s%s", cfgNameBaseLED, "maxBlinksPerMinute");
        if ((cfgValue = ricfg_getValue("RIPlatform", cfgName)) != NULL)
        {
            RILOG_DEBUG("%s: %s\n", cfgName, cfgValue);
            maxBlinksPerMinute = atoi(cfgValue);
        }
        else
            maxBlinksPerMinute = 0;

        // What is the name of the LED?
        sprintf(cfgName, "%s%s", cfgNameBaseLED, "name");
        if ((cfgValue = ricfg_getValue("RIPlatform", cfgName)) != NULL)
        {
            RILOG_DEBUG("%s: %s\n", cfgName, cfgValue);
            if (NULL != (name = (char *) g_try_malloc(strlen(cfgValue) + 1)))
                strcpy(name, cfgValue);
        }

        if (NULL == name)
        {
            char *defaultName = "led";
            RILOG_DEBUG("Using default LED Name: %s\n", defaultName);
            if (NULL != (name = (char *) g_try_malloc(strlen(defaultName) + 1)))
                strcpy(name, defaultName);
        }

        // Create a new LED display.
        create_led(name, brightnessLevels, colors, maxBlinksPerMinute);
        if (NULL != name)
            g_free(name);
    }
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

void initTextLEDDisplays(int numberOfTextLEDs)
{
    int i;
    char *cfgValue;
    char *cfgNameBase = "RI.Platform.frontpanel.textled.";
    char cfgName[256];
    char cfgNameBaseLED[256];

    RILOG_INFO("%s -- Entry\n", __FUNCTION__);

    for (i = 0; i < numberOfTextLEDs; i++)
    {
        RI_FP_INDICATOR_COLOR colors = RI_FP_NONE;
        unsigned int brightnessLevels, maxBlinksPerMinute,
                maxHorizScrollsPerMinute;
        //unsigned int xPos, yPos;
        //unsigned int width, height;
        char *name = NULL;

        char ledId[64];
        sprintf(ledId, "%d", i);

        // Determine base of configuration value name.
        sprintf(cfgNameBaseLED, "%s%s.", cfgNameBase, ledId);
        //RILOG_DEBUG("Configuration LED Base: %s\n", cfgNameBaseLED);

        // Does LED support the color blue?
        sprintf(cfgName, "%s%s", cfgNameBaseLED, "blue");
        if ((cfgValue = ricfg_getValue("RIPlatform", cfgName)) != NULL)
        {
            RILOG_DEBUG("%s: %s\n", cfgName, cfgValue);
            if (!strcmp(cfgValue, "TRUE"))
                colors = colors | RI_FP_BLUE;
        }
        // Does LED support the color green?
        sprintf(cfgName, "%s%s", cfgNameBaseLED, "green");
        if ((cfgValue = ricfg_getValue("RIPlatform", cfgName)) != NULL)
        {
            RILOG_DEBUG("%s: %s\n", cfgName, cfgValue);
            if (!strcmp(cfgValue, "TRUE"))
                colors = colors | RI_FP_GREEN;
        }
        // Does LED support the color yellow?
        sprintf(cfgName, "%s%s", cfgNameBaseLED, "yellow");
        if ((cfgValue = ricfg_getValue("RIPlatform", cfgName)) != NULL)
        {
            RILOG_DEBUG("%s: %s\n", cfgName, cfgValue);
            if (!strcmp(cfgValue, "TRUE"))
                colors = colors | RI_FP_YELLOW;
        }
        // Does LED support the color orange?
        sprintf(cfgName, "%s%s", cfgNameBaseLED, "orange");
        if ((cfgValue = ricfg_getValue("RIPlatform", cfgName)) != NULL)
        {
            RILOG_DEBUG("%s: %s\n", cfgName, cfgValue);
            if (!strcmp(cfgValue, "TRUE"))
                colors = colors | RI_FP_ORANGE;
        }
        // Does LED support the color red?
        sprintf(cfgName, "%s%s", cfgNameBaseLED, "red");
        if ((cfgValue = ricfg_getValue("RIPlatform", cfgName)) != NULL)
        {
            RILOG_DEBUG("%s: %s\n", cfgName, cfgValue);
            if (!strcmp(cfgValue, "TRUE"))
                colors = colors | RI_FP_RED;
        }

        // How many levels of brightness are supported?
        sprintf(cfgName, "%s%s", cfgNameBaseLED, "brightnesses");
        if ((cfgValue = ricfg_getValue("RIPlatform", cfgName)) != NULL)
        {
            RILOG_DEBUG("%s: %s\n", cfgName, cfgValue);
            brightnessLevels = atoi(cfgValue);
        }
        else
            brightnessLevels = 0;

        // How many blinks per minute are supported?
        sprintf(cfgName, "%s%s", cfgNameBaseLED, "maxBlinksPerMinute");
        if ((cfgValue = ricfg_getValue("RIPlatform", cfgName)) != NULL)
        {
            RILOG_DEBUG("%s: %s\n", cfgName, cfgValue);
            maxBlinksPerMinute = atoi(cfgValue);
        }
        else
            maxBlinksPerMinute = 0;

        // What is the name of the LED?
        sprintf(cfgName, "%s%s", cfgNameBaseLED, "name");
        if ((cfgValue = ricfg_getValue("RIPlatform", cfgName)) != NULL)
        {
            RILOG_DEBUG("%s: %s\n", cfgName, cfgValue);
            if (NULL != (name = (char *) g_try_malloc(strlen(cfgValue) + 1)))
                strcpy(name, cfgValue);
        }

        if (NULL == name)
        {
            char *defaultName = "text";
            RILOG_DEBUG("Using default Text LED Name: %s\n", defaultName);
            if (NULL != (name = (char *) g_try_malloc(strlen(defaultName) + 1)))
                strcpy(name, defaultName);
        }

        sprintf(cfgName, "%s%s", cfgNameBaseLED, "maxHorizScrollsPerMinute");
        if ((cfgValue = ricfg_getValue("RIPlatform", cfgName)) != NULL)
        {
            RILOG_DEBUG("%s: %s\n", cfgName, cfgValue);
            maxHorizScrollsPerMinute = atoi(cfgValue);
        }
        else
            maxHorizScrollsPerMinute = 0;

        // Create a new LED display.
        create_textled(name, brightnessLevels, colors, maxBlinksPerMinute,
                maxHorizScrollsPerMinute);
        if (NULL != name)
            g_free(name);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * resetTextLEDInternals : Resets internal structure at construction
 *                  or reload time.
 */
void resetTextLEDInternals(ri_textled_t *led)
{
    if (NULL != led->m_text)
    {
        g_free(led->m_text);
    }

    if (NULL != (led->m_text = (char *) g_try_malloc(sizeof(char))))
        strcpy(led->m_text, "");

    // Reset the Text Display.
    frontpanel_reset_text_display(led);

    led->m_textMode = RI_FP_MODE_12H_CLOCK; // 12 hour clock to start.
}

gboolean blink_timer_on_proc(gpointer data)
{
    ri_led_t *led = (ri_led_t *) data;
    RILOG_DEBUG("Blink Timer On Fired: %d\n", led->m_blinkTimerOnId);

    // Get the on/off control lock.
    g_mutex_lock((GMutex *) led->m_privateData);

    // It's on - turn it off.
    led->m_on = FALSE;
    led->m_blinkTimerOffId = g_timeout_add(led->m_timeOff,
            led->blinkTimerOffProc, led);

    g_mutex_unlock((GMutex *) led->m_privateData);

    // And display the LED as it should appear now.
    frontpanel_update_indicator_display(led);

    // Kill the timer upon returning from the callback.
    return FALSE;
}

gboolean blink_timer_off_proc(gpointer data)
{
    ri_led_t *led = (ri_led_t *) data;
    RILOG_DEBUG("Blink Timer Off Fired: %d\n", led->m_blinkTimerOffId);

    // Get the on/off control lock.
    g_mutex_lock((GMutex *) led->m_privateData);

    // It's off - turn it on.
    led->m_on = TRUE;
    led->m_blinkTimerOnId = g_timeout_add(led->m_timeOn, led->blinkTimerOnProc,
            led);

    g_mutex_unlock((GMutex *) led->m_privateData);

    // And display the LED as it should appear now.
    frontpanel_update_indicator_display(led);

    // Kill the timer upon returning from the callback.
    return FALSE;
}

gboolean scroll_timer_on_proc(gpointer data)
{
    gboolean retVal;
    ri_textled_t *led = (ri_textled_t *) data;
    RILOG_DEBUG("Scroll Timer On Fired: %d\n", led->m_scrollTimerOnId);

    // If we were on, and we're doing a delay time, turn
    // us off and reset the timer for the time we're to
    // stay off.
    if ((led->m_scrollIsOn) && (0 != led->m_delayTime))
    {
        led->m_scrollIsOn = FALSE;
        led->m_base.m_on = FALSE;
        //SetTimer( sm_hwnd, SCROLL_TIMER_ID, m_scrollOffTime, NULL );
        led->m_scrollTimerOffId = g_timeout_add(led->m_scrollOffTime,
                led->scrollTimerOffProc, led);
        retVal = FALSE;
    }
    else
    {
        // Notify UI to update scroll.
        frontpanel_update_text_display_scroll(led);

        led->m_scrollIsOn = TRUE;
        led->m_base.m_on = TRUE;
        //SetTimer( sm_hwnd, SCROLL_TIMER_ID, m_scrollOnTime, NULL );
        // No need to reset timer for m_scrollOnTime, just return TRUE.
        retVal = TRUE;
    }

    // And display the LED as it should appear now.
    frontpanel_update_text_display(led);

    return retVal;
}

gboolean scroll_timer_off_proc(gpointer data)
{
    ri_textled_t *led = (ri_textled_t *) data;
    RILOG_DEBUG("Scroll Timer Off Fired: %d\n", led->m_scrollTimerOffId);

    // Notify UI to update scroll.
    frontpanel_update_text_display_scroll(led);

    led->m_scrollIsOn = TRUE;
    led->m_base.m_on = TRUE;
    //SetTimer( sm_hwnd, SCROLL_TIMER_ID, m_scrollOnTime, NULL );
    led->m_scrollTimerOnId = g_timeout_add(led->m_scrollOnTime,
            led->scrollTimerOnProc, led);

    // And display the LED as it should appear now.
    frontpanel_update_text_display(led);

    return FALSE;
}
