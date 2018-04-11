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

import org.ocap.hardware.frontpanel.ScrollSpec;

/**
 * This interface represents the scrolling specification of a front panel text
 * display. In a single line display, text will scroll from right to left. In a
 * multi-line display, text will scroll from bottom to top. In this mode, text
 * lines must not exceed the length of the display, or wrap must be turned on.
 */
public class ScrollSpecImpl implements ScrollSpec
{
    /**
     * Protected constructor. Cannot be used by an application.
     * 
     * @param maxHorizontalIterations
     *            Maximum horizontal scroll rate
     * @param maxVerticalIterations
     *            Maximum vertical scroll rate
     * @param horizontalIterations
     *            Current horizontal scroll rate
     * @param verticalIterations
     *            Current vertical scroll rate
     * @param holdDuration
     *            Current hold duration
     */
    protected ScrollSpecImpl(int maxHorizontalIterations, int maxVerticalIterations, int horizontalIterations,
            int verticalIterations, int holdDuration, int numDisplayColumns, int numDisplayRows)
    {
        m_maxHorizontalIterations = maxHorizontalIterations;
        m_maxVerticalIterations = maxVerticalIterations;

        m_horizontalIterations = horizontalIterations;
        m_verticalIterations = verticalIterations;

        // Can only have either horizontal or vertical scrolling, but not
        // both
        if (m_horizontalIterations > 0 && m_verticalIterations > 0) m_verticalIterations = 0;

        m_holdDuration = holdDuration;
        m_columns = numDisplayColumns;
        m_rows = numDisplayRows;
    }

    /**
     * Gets the maximum number of times per minute characters can scroll right
     * to left across the display with zero hold time set.
     * 
     * @return Number of horizontal scroll iterations per minute. Returns zero
     *         if horizontal scroll is not supported.
     */
    public int getMaxHorizontalIterations()
    {
        return m_maxHorizontalIterations;
    }

    /**
     * Gets the maximum number of times per minute characters can scroll bottom
     * to top across the display with zero hold time set.
     * 
     * @return Number of vertical scroll iterations per minute. Returns -1 if
     *         the display only supports one row. Returns zero if vertical
     *         scroll is not supported.
     */
    public int getMaxVerticalIterations()
    {
        return m_maxVerticalIterations;
    }

    /**
     * Gets the number of times per minute the characters are set to scroll
     * across the screen from right to left.
     * 
     * @return Number of horizontal scroll iterations per minute. A value of 0
     *         indicates horizontal scrolling is turned off. A value of -1
     *         indicates there is more than one row displayed and characters
     *         will scroll vertically.
     */
    public int getHorizontalIterations()
    {
        return m_horizontalIterations;
    }

    /**
     * Gets the number of times per minute the characters are set to scroll
     * across the screen from bottom to top.
     * 
     * @return Number of vertical scroll iterations per minute. A value of 0
     *         indicates vertical scrolling is turned off. A value of -1
     *         indicates there is only one row of characters displayed and
     *         characters will scroll horizontally.
     */
    public int getVerticalIterations()
    {
        return m_verticalIterations;
    }

    /**
     * Sets the number of times per minute one character will scroll across the
     * display from right to left.
     * 
     * @param iterations
     *            Number of horizontal scroll iterations per minute
     * 
     * @throws IllegalArgumentException
     *             if the iteration is negative or exceed the value returned by
     *             <code>getMaxHorizontalIterations</code>.
     */
    public void setHorizontalIterations(int iterations)
    {
        if ((iterations < 0) || (iterations > m_maxHorizontalIterations))
            throw new IllegalArgumentException("ScrollSpecImpl.setHorizontalIterations() -- Invalid parameter "
                    + iterations);

        // We are switching from vertical to horizontal scrolling. Disable
        // vertical
        // scrolling and set hold duration to 0 since it could now be invalid
        if (iterations > 0 && m_verticalIterations > 0)
        {
            m_verticalIterations = 0;
            m_holdDuration = 0;
        }

        m_horizontalIterations = iterations;
    }

    /**
     * Sets the number of times per minute one character will scroll across the
     * display from bottom to top.
     * 
     * @param iterations
     *            Number of vertical scroll iterations per minute.
     * 
     * @throws IllegalArgumentException
     *             if the iteration is negative or exceed the value returned by
     *             <code>getMaxVerticalIterations</code>.
     */
    public void setVerticalIterations(int iterations)
    {
        if (iterations < 0 || iterations > m_maxVerticalIterations)
            throw new IllegalArgumentException("ScrollSpecImpl.setVerticalIterations() -- Invalid parameter "
                    + iterations);

        // We are switching from horizontal to vertical scrolling. Disable
        // horizontal
        // scrolling and set hold duration to 0 since it could now be invalid
        if (iterations > 0 && m_horizontalIterations > 0)
        {
            m_horizontalIterations = 0;
            m_holdDuration = 0;
        }

        m_verticalIterations = iterations;
    }

    /**
     * Gets the percentage of time the scroll will hold at each character during
     * one scroll iteration.
     * 
     * @return Character hold duration.
     */
    public int getHoldDuration()
    {
        return m_holdDuration;
    }

    /**
     * Sets the percentage of time to hold at each character before scrolling it
     * to the next position during one scroll iteration.
     * 
     * @param duration
     *            Character hold percentage duration. Setting this value causes
     *            a smooth scroll across all characters without a hold on any of
     *            them.
     * 
     * @throws IllegalArgumentException
     *             if duration is negative or if the duration percentage is
     *             greater than 100 divided by the number of characters to
     *             scroll across during horizontal scroll, or the number of rows
     *             to scroll across during vertical scroll.
     */
    public void setHoldDuration(int duration)
    {
        if (duration < 0)
            throw new IllegalArgumentException("ScrollSpec.setHoldDuration() -- Cannot set negative hold duration");

        // Vertical scrolling
        if (m_rows > 1 && m_verticalIterations > 0 && duration > (100 / m_rows))
            throw new IllegalArgumentException("ScrollSpec.setHoldDuration() -- Invalid vertical scroll hold duration");

        // If vertical scroll is disabled or not supported, then validate
        // holdDuration
        // for horizontal scrolling
        if (m_columns > 1 && m_verticalIterations <= 0 && duration > (100 / m_columns))
            throw new IllegalArgumentException(
                    "ScrollSpec.setHoldDuration() -- Invalid horizontal scroll hold duration");

        m_holdDuration = duration;
    }

    private int m_maxHorizontalIterations;

    private int m_maxVerticalIterations;

    private int m_horizontalIterations;

    private int m_verticalIterations;

    private int m_holdDuration;

    private int m_rows;

    private int m_columns;
}
