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
 * BitLed.h
 *
 *  Created on: Jun 18, 2009
 *      Author: Mark Millard, enableTv Inc.
 */

#ifndef __BITLED_H_
#define __BITLED_H_

#include <map>

// Include RI Platform header files.
#include <ri_frontpanel.h>

// Include RI Emulator header files.
#include "LedBase.h"
#include "Image.h"
#include "ImageMap.h"

using std::map;

/**
 * @class BitLed
 *
 * @brief The class that defines a bit-style led.
 *
 * @remarks Provides the definition of the Bit-style LED, a single "light"
 * that supports multiple intensities and colors.
 *
 * Inheritance: Inherits the generic LED base class LedBase.
 */
class BitLed: public LedBase
{
public:

    /**
     * A constructor that initializes the location of the specified RI Platform
     * LED.
     *
     * @param led A pointer to the RI Platform LED data.
     * @param imageMap A pointer to the Front Panel image map.
     */
    BitLed(ri_led_t *led, ImageMap *imageMap);

    /**
     * The destructor.
     */
    virtual ~BitLed();

    /**
     * Retrieve the associated RI Platform LED data.
     *
     * @return A pointer to the RI Platform data is returned.
     */
    ri_led_t *GetLed()
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

private:

    // The LED image height (same for all LEDs in this implementation).
    static const int LED_HEIGHT;
    // The LED image width (same for all LEDs in this implementation).
    static const int LED_WIDTH;

    // An enumeration of the LED intensity levels.
    typedef enum
    {
        NO_INTENSITY = 0,
        MIN_INTENSITY = 1,
        OFF = MIN_INTENSITY,
        LOW,
        MEDIUM,
        HIGH,
        MAX_INTENSITY = HIGH
    } INTENSITY;
    typedef map<INTENSITY, Image*> IntensityMap;
    typedef map<RI_FP_INDICATOR_COLOR, IntensityMap> ColorIntensityMap;

    // The color intensity image map.
    static ColorIntensityMap m_ledImageMap;
    // Flag indicating that the color intensity image map is loaded.
    static bool m_colorIntensityMapLoaded;
    // The image for the LED off state.
    static Image *m_offPicture;
    // The last LED color displayed.
    RI_FP_INDICATOR_COLOR m_lastColor;
    // The last LED intensity level displayed.
    INTENSITY m_lastIntensity;
    // A cache of the last image displayed.
    Image *m_lastPicture;
    // The current image being displayed.
    Image *m_currentPicture;
    // The associated Front Panel image map.
    ImageMap *m_imageMap;

    /*
     * Map the specified brightness level to an intensity setting.
     *
     * @param brightness The brightness level to map.
     *
     * @return An internal intensity level will be returned. Valid values are:
     * <ul>
     * <li>OFF</li>
     * <li>LOW</li>
     * <li>MEDIUM</li>
     * <li>HIGH</li>
     * </ul>
     */
    INTENSITY MapIntensity(unsigned int brightness);

    /*
     * Get an image for the specified color and intensity level.
     *
     * @param The color mapping.
     * @param The intensity level mapping.
     *
     * @return A pointer to an <code>Image</code> will be returned.
     */
    Image *GetImage(RI_FP_INDICATOR_COLOR color, INTENSITY intensity);

    /**
     * Retrieve the pixmap data for the specified color and intensity level.
     *
     * @param The color mapping.
     * @param The intensity level mapping.
     *
     * @return A pointer to the pixel data will be returned.
     */
    unsigned char *GetImageData(RI_FP_INDICATOR_COLOR color,
            INTENSITY intensity);

    /**
     * Retrieve the pixmap location.
     *
     * @param x A pointer to the output variable for the x coordinate location.
     * @param y A pointer to the output variable for the y coordinate location.
     */
    void GetImageLocation(unsigned int *x, unsigned int *y);

    /**
     * Retrieve the pixmap size, in pixels.
     *
     * @param width A pointer to the output variable for the width.
     * @param height A pointer to the output variable for the height.
     */
    void GetImageSize(unsigned int *width, unsigned int *height);

    // Hide the default constructor.
    BitLed()
    {
    }
    ;

protected:

    // A pointer to the RI Platform led data.
    ri_led_t *m_led;
};

#endif /* __BITLED_H_ */
