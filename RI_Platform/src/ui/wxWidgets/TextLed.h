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
 * TextLed.h
 *
 *  Created on: Jun 18, 2009
 *      Author: Mark Millard, enableTv Inc.
 */

#ifndef __TEXTLED_H_
#define __TEXTLED_H_

// Include RI Platform header files.
#include <ri_frontpanel.h>

// Include RI Emulator header files.
#include "LedBase.h"
#include "ImageMap.h"

class RIFrontPanel;

const unsigned int FP_TEXTLED_SEG1 = 128; // top
const unsigned int FP_TEXTLED_SEG2 = 64; // top right
const unsigned int FP_TEXTLED_SEG3 = 32; // bottom right
const unsigned int FP_TEXTLED_SEG4 = 16; // bottom
const unsigned int FP_TEXTLED_SEG5 = 8; // bottom left
const unsigned int FP_TEXTLED_SEG6 = 4; // top left
const unsigned int FP_TEXTLED_SEG7 = 2; // middle
const unsigned int FP_TEXTLED_SEG8 = 1; // trailing dot

const unsigned int FP_TEXTLED_SEGMENT_ALL = 0xFF;

const unsigned int FP_TEXTLED_NUM_DISPLAY_LEDS = 4;

const unsigned int FP_TEXTLED_CHAR_START = 32;
const unsigned int FP_TEXTLED_CHAR_END = 127;
//const unsigned int FP_TEXTLED_MAX_CHAR = (FP_TEXTLED_CHAR_END - FP_TEXTLED_CHAR_START + 1);
const unsigned int FP_TEXTLED_MAX_CHAR = 96;

// First character is 32 decimal (a space), each line holds 10 entries.
// So 50 (2) is the last entry on the second line, 65 (A) is the fifth
// entry on the fourth line.
//const unsigned int FP_TEXTLED_CHARSET[FP_LED_MAX_CHAR] =
const unsigned int FP_TEXTLED_CHARSET[96] =
{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 1, 0, 252, 96, 218, 242, 102, 182,
        190, 224, 254, 246, 0, 0, 0, 0, 0, 0, 0, 238, 62, 156, 122, 158, 142,
        188, 110, 96, 120, 0, 28, 0, 236, 252, 206, 230, 10, 182, 30, 124, 0,
        0, 0, 102, 0, 0, 0, 0, 0, 0, 16, 250, 62, 26, 122, 222, 142, 246, 46,
        32, 120, 0, 96, 0, 42, 58, 206, 230, 10, 182, 30, 56, 0, 0, 0, 102, 0,
        0, 0, 0, 0, 0 };

/**
 * @class TextLed
 *
 * @brief The class that defines a Text-style led.
 *
 * @remarks Provides the definition of the Text-style LED, a display
 * capable of rendering text.
 *
 * Inheritance: Inherits the generic LED base class LedBase.
 */
class TextLed: public LedBase
{
public:

    /**
     * A constructor that initializes the location of the specified RI Platform
     * LED.
     *
     * @param led A pointer to the RI Platform LED data.
     * @param imageMap A pointer to the Front Panel image map.
     */
    TextLed(ri_textled_t *led, ImageMap *imageMap);

    /**
     * The destructor.
     */
    virtual ~TextLed();

    /**
     * Retrieve the associated RI Platform LED data.
     *
     * @return A pointer to the RI Platform data is returned.
     */
    ri_textled_t *GetLed()
    {
        return m_led;
    }

    /**
     * Display the LED.
     */
    void Redisplay();

    /**
     * Reset the LED.
     */
    void ResetLED();

    /**
     * Display the text string.
     *
     * @param text A pointer to the string to display.
     */
    void DisplayLEDString(char *text);

    /**
     * Display the clock.
     *
     * @param mode The type of clock to display.
     */
    gboolean DisplayLEDClock(int mode);

    /**
     * Update the display scroll.
     */
    void UpdateScroll();

    /**
     * Start the text display.
     *
     * @param fp A pointer to the Front Panel starting this text display.
     */
    void StartDisplay(RIFrontPanel *fp);

    /**
     * Stop the text display.
     */
    void StopDisplay();

    /**
     * Get the associated front panel.
     *
     * @return A pointer to the front panel used to start the display
     * is returned. Note that this may be <b>NULL</b>.
     *
     * @see StartDisplay
     */
    RIFrontPanel *GetFrontPanel()
    {
        return m_fp;
    }

private:

    /**
     * Retrieve the display location.
     *
     * @param x A pointer to the output variable for the x coordinate location.
     * @param y A pointer to the output variable for the y coordinate location.
     */
    void GetDisplayLocation(unsigned int *x, unsigned int *y);

    /**
     * Retrieve the display size, in pixels.
     *
     * @param width A pointer to the output variable for the width.
     * @param height A pointer to the output variable for the height.
     */
    void GetDisplaySize(unsigned int *width, unsigned int *height);

    /**
     * Draw each led of the display.
     *
     * @param seg The segment to draw.
     * @param col The led position.
     */
    void DrawLedDisplay(unsigned int seg, int col);

    /**
     * Compute the sizes of the text display
     */
    void CalculateTextSizes();

    int* m_pData;
    int m_nDataSize;
    char m_szDisplayText[4];
    int m_nStringPos;
    // Text Display segment width.
    int m_nSegWidth;
    // Text Display segment height.
    int m_nSegLength;
    // Text Display margin.
    int m_nMargin;
    // The associated Front Panel image map.
    ImageMap *m_imageMap;
    // The associated Front Panel.
    RIFrontPanel *m_fp;
    // The identifier for the clock timer.
    int m_clockTimerId;
    // Background image for clearing display.
    static void *g_background;

    // Hide the default constructor.
    TextLed()
    {
    }
    ;

protected:

    // A pointer to the RI Platform led data.
    ri_textled_t *m_led;
};

#endif /* __TEXTLED_H_ */
