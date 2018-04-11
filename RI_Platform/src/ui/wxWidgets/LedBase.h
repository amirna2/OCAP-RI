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
 * LedBase.h
 *
 *  Created on: Jun 18, 2009
 *      Author: Mark Millard
 */

#ifndef __LEDBASE_H_
#define __LEDBASE_H_

/**
 * @class LedBase
 *
 * @brief The base class for the various type of LED displays for the
 * front panel.
 *
 * @remarks The LedBase provides the common functionality interface for all
 * LEDs on the front panel.
 *
 * Inheritance: All LED objects for a front panel MUST inherit this
 * interface.
 */
class LedBase
{
public:

    /**
     * A constructor that initializes the location and size of the LED.
     *
     * @param x The x location of the LED image.
     * @param y The y location of the LED image.
     * @param w The width of the LED image.
     * @param h The height of the LED image.
     */
    LedBase(unsigned int x, unsigned int y, unsigned int w, unsigned int h);

    /**
     * The destructor.
     */
    virtual ~LedBase();

    /**
     * This method is used to get the location of the LED.
     *
     * @param x The leftmost edge of the LED.
     * @param y The topmost edge of the LED.
     */
    void GetLEDLocation(unsigned int *x, unsigned int *y);

    /**
     * This method is used to set the location of the LED.
     *
     * @param x The leftmost edge of the LED.
     * @param y The topmost edge of the LED.
     */
    void SetLEDLocation(unsigned int x, unsigned int y);

    /**
     * This method is used to get the rectangle coordinates of the LED.
     *
     * @param x  The location to get the x coordinate of the upper left
     *           corner of the rectangle.
     * @param y  The location to get the y coordinate of the upper left
     *           corner of the rectangle.
     * @param x2 The location to get the x coordinate of the lower right
     *           corner of the rectangle.
     * @param y2 The location to get the y coordinate of the lower right
     *           corner of the rectangle.
     */
    void GetLEDRect(unsigned int *x, unsigned int *y, unsigned int *x2,
            unsigned int *y2);

    /**
     * Reset the LED.
     */
    virtual void ResetLED() = 0;

    /**
     * Set rendering state.
     *
     * @param render If <b>true</b>, then the LED can be rendered. Otherwise, a value of
     * <b>false</b> indicates that the LED can not be displayed.
     */
    void CanRender(bool render)
    {
        m_canRender = render;
    }

protected:

    // Hide the default constructor.
    LedBase() :
        m_x(0), m_y(0), m_width(0), m_height(0), m_canRender(false)
    {
    }
    ;

    /** The x location to render the LED. */
    unsigned int m_x;
    /** The y location to render the LED. */
    unsigned int m_y;
    /** The width of the the LED. */
    unsigned int m_width;
    /** The height of the LED. */
    unsigned int m_height;

    // Flag indicating whether LED can be rendered or not.
    bool m_canRender;
};

#endif /* __LEDBASE_H_ */
