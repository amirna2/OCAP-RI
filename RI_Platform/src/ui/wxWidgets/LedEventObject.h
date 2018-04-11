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
 * LedEventObject.h
 *
 *  Created on: Aug 10, 2009
 *      Author: Mark Millard
 */

#ifndef __LEDEVENTOBJECT_H__
#define __LEDEVENTOBJECT_H__

// Include wxWidgets header files.
#include <wx/wx.h>

// Include RI Platform header files.
#include <ri_frontpanel.h>

/**
 * Wrapper class for an Indicator or Text Display event.
 */
class LedEventObject: public wxObject
{
    /*lint -e(1516) -e(1526)*/
    DECLARE_DYNAMIC_CLASS( LedEventObject)
public:

    /**
     * A constructor that initializes the event object for
     * an Indicator.
     *
     * @param led A pointer to the RI Platform led.
     */
    LedEventObject(ri_led_t *led);

    /**
     * A constructor that initializes the event object for
     * a Text Display.
     *
     * @param led A pointer to the RI Platform text display.
     */
    LedEventObject(ri_textled_t *led);

    /**
     * The destructor.
     */
    virtual ~LedEventObject();

    /**
     * Check if event object is associated with an Indicator.
     *
     * @return <b>true</b> will be returned if the event object
     * is for an Indicator. Otherwise, <b>false</b> will be returned.
     */
    bool IsIndicator();

    /**
     * Get the the associated indicator.
     *
     * @return A pointer to a RI Platform led will be returned, if and only if
     * it was constructed that way. <b>NULL</b> will be returned if the event
     * object was constructed for a text display.
     */
    ri_led_t *GetIndicator()
    {
        return m_led;
    }

    /**
     * Check if event object is associated with a Text Display.
     *
     * @return <b>true</b> will be returned if the event object
     * is for a Text Display. Otherwise, <b>false</b> will be returned.
     */
    bool IsTextDisplay();

    /**
     * Get the the associated text display.
     *
     * @return A pointer to a RI Platform text display will be returned, if and only if
     * it was constructed that way. <b>NULL</b> will be returned if the event
     * object was constructed for an indicator.
     */
    ri_textled_t *GetTextDisplay()
    {
        return m_textled;
    }

private:

    // Hide the default constructor.
    LedEventObject()
    {
    }

protected:

    // Reference to indicator.
    ri_led_t *m_led;
    // Reference to text display.
    ri_textled_t *m_textled;
};

#endif /* __LEDEVENTOBJECT_H__ */
