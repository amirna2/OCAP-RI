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

package org.cablelabs.impl.ocap.hardware.frontpanel;

import org.ocap.hardware.frontpanel.BrightSpec;

/**
 * This interface represents the front panel display brightness specification.
 * If the Indicator supports just turn on/off, the brightness value {@link #OFF}
 * represents turn off and value 1 represents turn on, and a brightness level
 * shall be 2. If the Indicator supports bright/dark/off brightness, the
 * brightness value {@link #OFF} represents turn off and value 1 represents dark
 * and value 2 represents bright, and a brightness level shall be 3. The
 * brightness level can be any levels corresponding to the Indicator�s
 * capability.
 */
public class BrightSpecImpl implements BrightSpec
{
    /**
     * Protected constructor. Cannot be used by an application.
     * 
     * @param brightness
     *            Current brightness level
     * 
     * @param brightnessLevels
     *            Number of brightness levels
     */
    protected BrightSpecImpl(int brightness, int brightnessLevels)
    {
        m_brightness = brightness;
        m_brightnessLevels = brightnessLevels;
    }

    /**
     * Gets the current brightness of the Indicator. Possible return values
     * shall be an integer that meets: <BR> {@link #OFF} =< brightness =<
     * getBrightnessLevels()-1
     * 
     * @return a current brightness value of Indicator.
     */
    public int getBrightness()
    {
        return m_brightness;
    }

    /**
     * Gets the number of brightness levels supported. The minimum support
     * brightness level SHALL be 2, i.e., two levels that includes {@link #OFF}
     * and brightness=1 (on). This provides an on/off capability.
     * 
     * @return Supported indicator brightness levels.
     */
    public int getBrightnessLevels()
    {
        return m_brightnessLevels;
    }

    /**
     * Sets the brightness of the indicator. Setting the brightness level to
     * {@link #OFF} turns the indicator off.
     * 
     * @param brightness
     *            Indicator brightness.
     * 
     * @throws IllegalArgumentException
     *             if the brightness value is not an integer that meets: <BR>
     *             {@link #OFF} =< brightness =< getBrightnessLevels()-1
     */
    public void setBrightness(int brightness) throws IllegalArgumentException
    {
        if ((brightness < OFF) || (brightness > (getBrightnessLevels() - 1)))
        {
            throw new IllegalArgumentException();
        }

        m_brightness = brightness;
    }

    private int m_brightness;

    private int m_brightnessLevels;
}
