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

package org.cablelabs.gear.havi.decorator;

import org.havi.ui.*;
import java.awt.*;

/**
 * A <code>StateDecorator</code> which fills the bounds of the given
 * {@link org.havi.ui.HVisible} with its background color according to its
 * {@link HVisible#getBackgroundMode() background mode}.
 * <p>
 * Whether to fill or not can be enabled/disabled for each state. The background
 * is only filled for those states in which this <code>StateDecorator</code> is
 * enabled if the background mode is {@link HVisible#BACKGROUND_FILL}.
 * 
 * @see StateDecorator
 * 
 * @author Aaron Kamienski
 * @version $Id: FillDecorator.java,v 1.13 2002/06/03 21:32:27 aaronk Exp $
 */
public class FillDecorator extends StateDecorator
{
    /**
     * Fills the entire rectangular bounds of the <code>HVisible</code>. This is
     * the default.
     */
    public static final int RECT = 0;

    /**
     * Fills the entire rectangular bounds of the <code>HVisible</code> with a
     * rounded rectangle (areas in the corners will not be filled).
     */
    public static final int ROUNDRECT = 1;

    /**
     * Fills the entire rectangular bounds of the <code>HVisible</code> with an
     * oval (areas in the corners will not be filled).
     */
    public static final int OVAL = 2;

    /**
     * Default constructor. Creates a <code>FillDecorator</code>, initialized to
     * operate in no states.
     * <p>
     * Initialized with a {@link #getFillType() fillType} of <code>RECT</code>
     * and arc ({@link #getArcWidth() width}, {@link #getArcHeight height}) to
     * be <code>(2,2)</code>.
     */
    public FillDecorator()
    {
        super();
    }

    /**
     * Creates a <code>FillDecorator</code>, initialized to operate in no
     * states.
     * <p>
     * Initialized with a {@link #getFillType() fillType} of <code>RECT</code>
     * and arc ({@link #getArcWidth() width}, {@link #getArcHeight height}) to
     * be <code>(2,2)</code>.
     * 
     * @param look
     *            the component <code>HLook</code>
     */
    public FillDecorator(HLook look)
    {
        super(look);
    }

    /**
     * Creates a <code>FillDecorator</code>, intialized to operate in the given
     * set of states.
     * <p>
     * Initialized with an arc ({@link #getArcWidth() width},
     * {@link #getArcHeight height}) to be <code>(2,2)</code>.
     * 
     * @param look
     *            the component <code>HLook</code>
     * @param bitMask
     *            the bitMask specifying the states to operate in
     * @param fillType
     *            one of ({@link #RECT}, {@link #ROUNDRECT}, {@link #OVAL})
     */
    public FillDecorator(HLook look, int bitMask, int fillType)
    {
        super(look, bitMask);
        setFillType(fillType);
    }

    /**
     * Creates a <code>FillDecorator</code>, initialized to operation in the
     * given set of states.
     * 
     * @param look
     *            the component <code>HLook</code>
     * @param states
     *            an array of {@link org.havi.ui.HState HState-defined} states
     * @param fillType
     *            one of ({@link #RECT}, {@link #ROUNDRECT}, {@link #OVAL})
     * 
     * @throws IllegalArgumentException
     *             if an invalid state is specified
     * @throws NullPointerException
     *             if <code>states</code> is <code>null</code>
     */
    public FillDecorator(HLook look, int[] states, int fillType)
    {
        super(look, states);
        setFillType(fillType);
    }

    /**
     * Returns the current <code>fillType</code>.
     * 
     * @return one of ({@link #RECT}, {@link #ROUNDRECT}, {@link #OVAL})
     */
    public int getFillType()
    {
        return fillType;
    }

    /**
     * Sets the current <code>fillType</code>.
     * 
     * @param fillType
     *            the new <code>fillType</code>; can be one of ({@link #RECT},
     *            {@link #ROUNDRECT}, {@link #OVAL})
     * 
     * @throws IllegalArgumentException
     *             if <code>fillType</code> is of an unexpected type
     */
    public void setFillType(int fillType) throws IllegalArgumentException
    {
        switch (fillType)
        {
            case RECT:
            case ROUNDRECT:
            case OVAL:
                this.fillType = fillType;
                break;
            default:
                throw new IllegalArgumentException("Invalid fillType " + fillType);
        }
    }

    /**
     * Returns the arc width used for {@link #ROUNDRECT} fills.
     * 
     * @return the arc width used for {@link #ROUNDRECT} fills
     */
    public int getArcWidth()
    {
        return arcWidth;
    }

    /**
     * Sets the arc width used for {@link #ROUNDRECT} fills.
     * 
     * @param width
     *            the new arc width
     */
    public void setArcWidth(int width)
    {
        arcWidth = width;
    }

    /**
     * Returns the arc height used for {@link #ROUNDRECT} fills.
     * 
     * @return the arc height used for {@link #ROUNDRECT} fills
     */
    public int getArcHeight()
    {
        return arcHeight;
    }

    /**
     * Sets the arc height used for {@link #ROUNDRECT} fills.
     * 
     * @param height
     *            the new arc height
     */
    public void setArcHeight(int height)
    {
        arcHeight = height;
    }

    /**
     * Fills the entire bounds of the given <code>HVisible</code> with its
     * background color if the <code>visible</code> is in one of the states for
     * which this <code>StateDecorator</code> is enabled.
     * 
     * @param g
     * @param visible
     * @return <code>null</code>
     */
    protected Object showLook(java.awt.Graphics g, HVisible visible)
    {
        if (visible.getBackgroundMode() == HVisible.BACKGROUND_FILL)
        {
            // Draw the background
            Dimension d = visible.getSize();
            g.setColor(visible.getBackground());

            switch (fillType)
            {
                case RECT:
                default:
                    g.fillRect(0, 0, d.width, d.height);
                    break;
                case ROUNDRECT:
                    g.fillRoundRect(0, 0, d.width, d.height, arcWidth, arcHeight);
                    break;
                case OVAL:
                    g.fillOval(0, 0, d.width, d.height);
                    break;
            }
        }
        return null;
    }

    /** The current fill type. Defaults to RECT. */
    private int fillType = RECT;

    /** The width to use when in ROUNDRECT mode. */
    private int arcWidth = 2;

    /** The height to use when in ROUNDRECT mode. */
    private int arcHeight = 2;
}
