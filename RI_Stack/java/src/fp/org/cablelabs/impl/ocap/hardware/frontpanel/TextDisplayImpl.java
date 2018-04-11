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

import org.apache.log4j.Logger;

import org.davic.resources.ResourceClient;

import org.ocap.hardware.frontpanel.TextDisplay;
import org.ocap.hardware.frontpanel.BlinkSpec;
import org.ocap.hardware.frontpanel.BrightSpec;
import org.ocap.hardware.frontpanel.ColorSpec;
import org.ocap.hardware.frontpanel.ScrollSpec;

/**
 * This interface represents one line of characters in a front panel display.
 */
public class TextDisplayImpl extends FrontPanelResource implements TextDisplay
{

    /**
     * Protected constructor. Cannot be used by an application.
     */
    protected TextDisplayImpl(int id, ResourceClient client)
    {
        m_client = client;
        m_id = id;
        m_wrap = false;

        nGetTextData(m_id);
    }

    /**
     * Gets the blink specification for the front panel text display. Changing
     * values within the object returned by this method does not take affect
     * until one of the set display methods in this interface is called and the
     * object is passed to the implementation.
     * 
     * @return Front panel blink specification. MAY return null if blinking is
     *         not supported.
     */
    public BlinkSpec getBlinkSpec()
    {
        // create a new BlinkSpec with current parameters and
        // return it to caller
        return new BlinkSpecImpl(m_blinkIterations, m_blinkOnDuration, m_blinkMaxCycleRate);
    }

    /**
     * Gets the brightness specification for the front panel text display.
     * Changing values within the object returned by this method does not take
     * affect until one of the set methods in this interface is called and the
     * object is passed to the implementation.
     * 
     * @return Front panel brightness specification.
     */
    public BrightSpec getBrightSpec()
    {
        // create a new BrightSpec with current parameters and
        // return it to caller
        return new BrightSpecImpl(m_brightness, m_brightnessLevels);
    }

    /**
     * Gets the Color specification for the front panel text display. Changing
     * values within the object returned by this method does not take affect
     * until one of the set methods in this interface is called and the object
     * is passed to the implementation.
     * 
     * @return Front panel Color specification. MAY return null if changing the
     *         color is not supported.
     */
    public ColorSpec getColorSpec()
    {
        // create a new ColorSpec with current parameters and
        // return it to caller
        return new ColorSpecImpl(m_color, m_supportedColors);
    }

    /**
     * Gets the scroll specification for the front panel text display. Changing
     * values within the object returned by this method does not take affect
     * until one of the set display methods in this interface is called and the
     * object is passed to the implementation.
     * 
     * @return Front panel scroll specification.
     */
    public ScrollSpec getScrollSpec()
    {
        // create a new ScrollSpec with current parameters and
        // return it to caller
        return new ScrollSpecImpl(m_scrollMaxHorizontalIterations, m_scrollMaxVerticalIterations,
                m_scrollHorizontalIterations, m_scrollVerticalIterations, m_scrollHoldDuration, m_numColumns, m_numRows);
    }

    /**
     * Gets the text display mode. See definitions of {@link #TWELVE_HOUR_CLOCK}
     * , {@link #TWENTYFOUR_HOUR_CLOCK}, and {@link #STRING_MODE} for possible
     * return values.
     * 
     * @return Text display mode.
     */
    public int getMode()
    {
        return m_mode;
    }

    /**
     * Gets the number of columns (characters) per line in the text display. The
     * text is considered fixed font by this method. Dynamic font sizes can be
     * supported and the calculation for this method uses the largest character
     * size for the given font.
     * 
     * @return Number of columns.
     */
    public int getNumberColumns()
    {
        return m_numColumns;
    }

    /**
     * Gets the number of rows (i.e. lines) in the text display.
     * 
     * @return Number of rows.
     */
    public int getNumberRows()
    {
        return m_numRows;
    }

    /**
     * Gets the set of characters supported by the display. This API does not
     * contain font support and this method is the only way to discover the
     * character set supported by the front panel. In addition, certain types of
     * displays do not support the entire alphabet or symbol set, e.g. seven
     * segment LEDs.
     * 
     * @return Supported character set.
     */
    public String getCharacterSet()
    {
        return m_characterSet;
    }

    /**
     * Displays the current system time on the front panel text display. The
     * display is formatted to the mode parameter.
     * 
     * @param mode
     *            One of the clock modes; {@link #TWELVE_HOUR_CLOCK}, or
     *            {@link #TWENTYFOUR_HOUR_CLOCK}.
     * 
     * @param blinkSpec
     *            Blink specification if blinking is desired. A value of null
     *            turns blinking off.
     * 
     * @param scrollSpec
     *            Scroll specification if scrolling is desired. A value of null
     *            turns scrolling off. If there is only one line of text
     *            scrolling will be from right to left. If there is more than
     *            one line of text scrolling will be from bottom to top. Passing
     *            in null turns scrolling off.
     * 
     * @param brightSpec
     *            Brightness specification if a change in brightness is desired.
     *            A value of null results in no change to current brightness.
     * 
     * @param colorSpec
     *            Color specification if a change in color is desired. A value
     *            of null results in no change to current color.
     * 
     * @throws IllegalArgumentException
     *             if the mode parameter is not one of
     *             {@link #TWELVE_HOUR_CLOCK} or {@link #TWENTYFOUR_HOUR_CLOCK}.
     * 
     * @throws IllegalStateException
     *             if the TextDisplay resource was lost.
     */
    public void setClockDisplay(byte mode, BlinkSpec blinkSpec, ScrollSpec scrollSpec, BrightSpec brightSpec,
            ColorSpec colorSpec) throws IllegalStateException
    {
        if (log.isInfoEnabled()) 
        {
            log.info("setClockDisplay called");
        }

        // Validate clock mode
        if (mode != TWELVE_HOUR_CLOCK && mode != TWENTYFOUR_HOUR_CLOCK)
        {
            throw new IllegalArgumentException("TextDisplay.setClockDisplay -- Illegal clock mode");
        }

        synchronized (m_lock)
        {
            if (isResourceLost())
                throw new IllegalStateException(
                        "Error attempting to setClockDisplay on a display that has been released");

            m_mode = mode;

            setTextDisplay(blinkSpec, scrollSpec, brightSpec, colorSpec);

            // call native method to set the clock display
            nSetTextDisplay(m_mode, null, m_blinkIterations, m_blinkOnDuration, m_brightness, m_color,
                    m_scrollHorizontalIterations, m_scrollVerticalIterations, m_scrollHoldDuration);
        }
    }

    /**
     * Displays text on the front panel display. If multiple fonts are possible
     * the implementation SHALL determine which will be used. Sets the LED front
     * panel to the text mode; see {@link #STRING_MODE}. The text parameter will
     * be used to display text characters in the display. Wrapping occurs if
     * there is more than one line, wrapping is turned on, and the text
     * over-fills at least one line.
     * 
     * @param text
     *            String of characters to display. Each string in the array
     *            represents a line of text. text[0] represents the top line,
     *            text[1] represents the next line down, and so forth.
     * 
     * @param blinkSpec
     *            Blink specification if blinking is desired. Passing in null
     *            turns blinking off.
     * 
     * @param scrollSpec
     *            Scroll specification if scrolling is desired. If there is only
     *            one line of text scrolling will be from right to left. If
     *            there is more than one line of text scrolling will be from
     *            bottom to top. Passing in null turns scrolling off.
     * 
     * @param brightSpec
     *            Brightness specification if a change in brightness is desired.
     *            A value of null results in no change to current brightness.
     * 
     * @param colorSpec
     *            Color specification if a change in color is desired. A value
     *            of null results in no change to current color.
     * 
     * @throws IllegalArgumentException
     *             if the text array contains more than 1 line and one or more
     *             lines are longer than the display and wrap is turned off.
     * 
     * @throws IllegalStateException
     *             if the TextDisplay resource was lost.
     */
    public void setTextDisplay(String[] text, BlinkSpec blinkSpec, ScrollSpec scrollSpec, BrightSpec brightSpec,
            ColorSpec colorSpec) throws IllegalStateException

    {
        if (log.isInfoEnabled()) 
        {
            log.info("setTextDisplay called: text.length = " + text.length);
        }

        for (int i=0; i<text.length; i++)
        {
            if (log.isInfoEnabled()) 
            {
                log.info("setTextDisplay: " + i + ": " + text[i]);
            }
        }

        synchronized (m_lock)
        {
            if (isResourceLost())
                throw new IllegalStateException(
                        "Error attempting to setBlinkSpec on an indicator that has been released");

            // If the text array contains more than 1 line and one or more lines
            // are
            // longer than the display and wrap is turned off, throw exception
            if (text.length > 1 && !m_wrap)
            {
                for (int i = 0; i < text.length; ++i)
                {
                    if (text[i].length() > m_numColumns)
                        throw new IllegalArgumentException(
                                "TextDisplay.setTextDisplay -- line too long and wrapping disabled!");
                }
            }

            m_mode = STRING_MODE;
            setTextDisplay(blinkSpec, scrollSpec, brightSpec, colorSpec);

            // call native method to set the text display
            nSetTextDisplay(STRING_MODE, text, m_blinkIterations, m_blinkOnDuration, m_brightness, m_color,
                    m_scrollHorizontalIterations, m_scrollVerticalIterations, m_scrollHoldDuration);
        }
    }

    /**
     * Sets wrapping on or off.
     * 
     * @param wrap
     *            If true, wrapping is turned on, otherwise wrapping is turned
     *            off.
     */
    public void setWrap(boolean wrap)
    {
        m_wrap = wrap;
    }

    /**
     * Removes characters from the text display.
     */
    public void eraseDisplay()
    {
        if (log.isInfoEnabled()) 
        {
            log.info("eraseDisplay called");
        }

        synchronized (m_lock)
        {
            if (isResourceLost())
                throw new IllegalStateException(
                        "Error attempting to setBlinkSpec on an indicator that has been released");

            String[] text = new String[1];
            text[0] = new String("");
            nSetTextDisplay(STRING_MODE, text, m_blinkIterations, m_blinkOnDuration, m_brightness, m_color,
                    m_scrollHorizontalIterations, m_scrollVerticalIterations, m_scrollHoldDuration);
        }
    }

    /**
     * Get associated resource client.
     * 
     * @return The associated <code>ResourceClient</code> is returned.
     */
    public ResourceClient getClient()
    {
        return m_client;
    }

    private void setTextDisplay(BlinkSpec blinkSpec, ScrollSpec scrollSpec, BrightSpec brightSpec, ColorSpec colorSpec)
    {
        // if blinkSpec is null, then turn off blinking
        if (blinkSpec == null)
        {
            m_blinkIterations = 0;

            // set to 100, if we set it to 0 the indicator would be turned off.
            m_blinkOnDuration = 100;
        }
        else
        {
            m_blinkIterations = blinkSpec.getIterations();
            m_blinkOnDuration = blinkSpec.getOnDuration();
        }

        // if brightSpec is null, don't change the brightness
        if (brightSpec != null)
        {
            m_brightness = brightSpec.getBrightness();
        }

        // if colorSpec is null, don't change the color
        if (colorSpec != null)
        {
            m_color = colorSpec.getColor();
        }

        // if scrollSpec is null, then turn off scrolling
        if (scrollSpec == null)
        {
            m_scrollHorizontalIterations = 0;
            m_scrollVerticalIterations = 0;
            m_scrollHoldDuration = 0;
        }
        else
        {
            m_scrollHorizontalIterations = scrollSpec.getHorizontalIterations();
            m_scrollVerticalIterations = scrollSpec.getVerticalIterations();
            m_scrollHoldDuration = scrollSpec.getHoldDuration();
        }
    }

    private native static void nInit();

    private native void nGetTextData(int id);

    private native void nSetTextDisplay(int mode, String[] text, int iterations, int onDuration, int brightness,
            byte color, int hScrollIterations, int vScrollIterations, int holdDuration);

    private int m_mode = TextDisplay.TWELVE_HOUR_CLOCK;

    private int m_numColumns = 0;

    private int m_numRows = 0;

    private String m_characterSet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_";

    private boolean m_wrap = false;

    // BlinkSpec
    private int m_blinkIterations = 0;

    private int m_blinkMaxCycleRate = 0;

    private int m_blinkOnDuration = 0;

    // BrightSpec
    private int m_brightness = BrightSpec.OFF;

    private int m_brightnessLevels = 2;

    // ColorSpec
    private byte m_color = 0x00;

    private byte m_supportedColors = 0x00;

    // ScrollSpec
    private int m_scrollHoldDuration = 0;

    private int m_scrollHorizontalIterations = 0;

    private int m_scrollVerticalIterations = 0;

    private int m_scrollMaxHorizontalIterations = 0;

    private int m_scrollMaxVerticalIterations = 0;

    private ResourceClient m_client;

    private int m_id;

    private static final Logger log = Logger.getLogger(TextDisplayImpl.class.getName());

    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
        nInit();
    }
}
