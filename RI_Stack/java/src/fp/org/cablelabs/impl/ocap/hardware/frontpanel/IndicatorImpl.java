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

import org.davic.resources.ResourceClient;
import org.ocap.hardware.frontpanel.BlinkSpec;
import org.ocap.hardware.frontpanel.BrightSpec;
import org.ocap.hardware.frontpanel.ColorSpec;
import org.ocap.hardware.frontpanel.Indicator;

/**
 * This interface represents an indicator in the front panel display and allows
 * its properties to be checked and set.
 */
public class IndicatorImpl extends FrontPanelResource implements Indicator
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
    protected IndicatorImpl(int id, ResourceClient client)
    {
        m_id = id;
        m_client = client;

        // get the min/max and current values from the native implementation
        nGetIndicatorData(m_id);
    }

    /**
     * Gets the blink specification for the front panel Indicator. Changing
     * values within the object returned by this method does not take affect
     * until one of the set display methods in this interface is called and the
     * object is passed to the implementation.
     * 
     * @return Front panel blink specification. MAY return null if blinking is
     *         not supported.
     */
    public BlinkSpec getBlinkSpec()
    {
        // create a new BlinkSpecImpl with current parameters and
        // return it to caller
        return new BlinkSpecImpl(m_blinkIterations, m_blinkOnDuration, m_blinkMaxCycleRate);
    }

    /**
     * Sets the blink specification for the front panel Indicator.
     * 
     * @param blinkSpec
     *            Blink specification if blinking is desired. A value of null
     *            turns blinking off.
     * 
     * @throws IllegalStateException
     *             if the Indicator resource was lost.
     */
    public void setBlinkSpec(BlinkSpec blinkSpec) throws IllegalStateException
    {
        synchronized (m_lock)
        {
            if (isResourceLost())
                throw new IllegalStateException(
                        "Error attempting to setBlinkSpec on an indicator that has been released");

            if (blinkSpec != null)
            {
                // set private fields from blinkSpec
                m_blinkIterations = blinkSpec.getIterations();
                m_blinkOnDuration = blinkSpec.getOnDuration();

                // invoke native method to update the indicator
                nSetIndicator(m_id, m_brightness, m_blinkIterations, m_blinkOnDuration, m_color);
            }
        }
    }

    /**
     * Gets the brightness specification for the front panel Indicator. Changing
     * values within the object returned by this method does not take affect
     * until one of the set methods in this interface is called and the object
     * is passed to the implementation.
     * 
     * @return Front panel brightness specification.
     */
    public BrightSpec getBrightSpec()
    {
        // create a new BrightSpecImpl with current parameters and
        // return it to caller
        return new BrightSpecImpl(m_brightness, m_brightnessLevels);
    }

    /**
     * Sets the Brightness specification for the front panel Indicator.
     * 
     * @param brightSpec
     *            Brightness specification.
     * 
     * @throws IllegalArgumentException
     *             if null is passed in.
     * 
     * @throws IllegalStateException
     *             if the Indicator resource was lost.
     */
    public void setBrightSpec(BrightSpec brightSpec) throws IllegalStateException
    {
        if (isResourceLost())
            throw new IllegalStateException("Error attempting to setBrightSpec on an indicator that has been released");

        if (brightSpec == null)
            throw new IllegalArgumentException("IndicatorImpl.setBrightSpec() -- Parameter is null!");

        // set private fields from brightSpec
        m_brightness = brightSpec.getBrightness();

        // invoke native method to update the indicator
        nSetIndicator(m_id, m_brightness, m_blinkIterations, m_blinkOnDuration, m_color);
    }

    /**
     * Gets the Color specification for the front panel Indicator. Changing
     * values within the object returned by this method does not take affect
     * until one of the set methods in this interface is called and the object
     * is passed to the implementation.
     * 
     * @return Front panel Color specification. MAY return null if changing the
     *         color is not supported.
     */
    public ColorSpec getColorSpec()
    {
        // create a new ColorSpecImpl with current parameters and
        // return it to caller
        return new ColorSpecImpl(m_color, m_supportedColors);
    }

    /**
     * Sets the Color specification for the front panel Indicator.
     * 
     * @param colorSpec
     *            Color specification
     * 
     * @throws IllegalArgumentException
     *             if null is passed in.
     * 
     * @throws IllegalStateException
     *             if the Indicator resource was lost.
     */
    public void setColorSpec(ColorSpec colorSpec) throws IllegalStateException
    {
        if (colorSpec == null)
            throw new IllegalArgumentException("IndicatorImpl.setColorSpec() -- Parameter is null!");

        synchronized (m_lock)
        {
            if (isResourceLost())
                throw new IllegalStateException(
                        "Error attempting to setColorSpec on an indicator that has been released");

            // set private fields from colorSpec
            m_color = colorSpec.getColor();

            // invoke native method to update the indicator
            nSetIndicator(m_id, m_brightness, m_blinkIterations, m_blinkOnDuration, m_color);
        }
    }

    public ResourceClient getClient()
    {
        return m_client;
    }

    private native static void nInit();

    /**
     * Native method to set the indicator
     * 
     * @param id
     *            indicator ID to be changed
     * @param brightness
     *            brightness value
     * @param iterations
     *            number of blinks per minute
     * @param onDuration
     *            percentage ON time
     * @param color
     *            color
     */
    private native void nSetIndicator(int id, int brightness, int iterations, int onDuration, byte color);

    /**
     * Native method to get the current set of indicator data
     * 
     * @param id
     *            ID of the indicator
     */
    private native void nGetIndicatorData(int id);

    // BlinkSpec
    private int m_blinkIterations;

    private int m_blinkMaxCycleRate;

    private int m_blinkOnDuration;

    // BrightSpec
    private int m_brightness;

    private int m_brightnessLevels;

    // ColorSpec
    private byte m_color;

    private byte m_supportedColors;

    private int m_id;

    private ResourceClient m_client;

    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
        nInit();
    }
}
