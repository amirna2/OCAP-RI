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

package org.cablelabs.gear;

import java.awt.*;
import org.cablelabs.gear.util.SizingStrategy;

/**
 * A <code>ColumnLayout</code> lays out all components in a single column. The
 * following can be specified:
 * <ul>
 * <li>vertical gap between components
 * <li>horizontal orientation of each component with respect to container
 * <li>vertical orientation of all components with respect to container
 * </ul>
 * 
 * @see RowLayout
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.2 $, $Date: 2001/06/01 15:56:48 $
 */
public class ColumnLayout implements LayoutManager
{
    public static final int CENTER = 0;

    public static final int TOP = 1;

    public static final int LEFT = 2;

    public static final int RIGHT = 3;

    public static final int BOTTOM = 4;

    public static final int NORTH = TOP;

    public static final int SOUTH = BOTTOM;

    public static final int EAST = RIGHT;

    public static final int WEST = LEFT;

    private static final int DEFAULT_GAP = 5;

    private int gap;

    private int verticalOrientation;

    private int horizontalOrientation;

    /**
     * Default constructor. Uses the following defaults:
     * <ul>
     * <li><code>horizontalOrientation == {@link #CENTER}</code>
     * <li><code>verticalOrientation == CENTER</code>
     * <li><code>gap == 5</code>
     * </ul>
     */
    public ColumnLayout()
    {
        this(CENTER, CENTER, DEFAULT_GAP);
    }

    /**
     * Constructor that takes an integer specifying the vertical gab between
     * components. Uses the following defaults:
     * <ul>
     * <li><code>horizontalOrientation == {@link #CENTER}</code>
     * <li><code>verticalOrientation == CENTER</code>
     * </ul>
     * 
     * @param gap
     *            the vertical gap between components
     */
    public ColumnLayout(int gap)
    {
        this(CENTER, CENTER, gap);
    }

    /**
     * Constructor that takes strings specifying the horizontal and vertical
     * alignments. These should be specified using the <code>ColumnLayout</code>
     * constants:
     * <ul>
     * <li>{@link #LEFT}, {@link #CENTER}, {@link #RIGHT} for horizontal
     * orientation
     * <li>{@link #TOP}, {@link #CENTER}, {@link #BOTTOM} for vertical
     * orientation
     * </ul>
     * 
     * Uses the following defaults:
     * <ul>
     * <li><code>gap == 5</code>
     * </ul>
     * 
     * @param horizontalOrient
     *            horizontal orientation
     * @param verticalOrient
     *            vertical orientation
     */
    public ColumnLayout(int horizontalOrient, int verticalOrient)
    {
        this(horizontalOrient, verticalOrient, DEFAULT_GAP);
    }

    /**
     * Constructor that takes an integer specifying horizontal gap as well as
     * strings specifying the horizontal and vertical alignments. These should
     * be specified using the <code>ColumnLayout</code> constants:
     * <ul>
     * <li>{@link #LEFT}, {@link #CENTER}, {@link #RIGHT} for horizontal
     * orientation
     * <li>{@link #TOP}, {@link #CENTER}, {@link #BOTTOM} for vertical
     * orientation
     * </ul>
     * 
     * @param horizontalOrient
     *            horizontal orientation
     * @param verticalOrient
     *            vertical orientation
     * @param gap
     *            vertical gap
     * 
     * @throws IllegalArgumentException
     *             if <code>horizontalOrient</code> is not one of {@link #LEFT},
     *             {@link #CENTER}, or {@link #RIGHT}; or if
     *             <code>verticalOrient</code> is not one of {@link #TOP},
     *             {@link #CENTER}, or {@link #BOTTOM}
     */
    public ColumnLayout(int horizontalOrient, int verticalOrient, int gap) throws IllegalArgumentException
    {
        setGap(gap);
        setVerticalOrientation(verticalOrient);
        setHorizontalOrientation(horizontalOrient);
    }

    /**
     * Returns the vertical gap between components.
     * 
     * @return the vertical gap between components
     */
    public int getGap()
    {
        return gap;
    }

    /**
     * Sets the vertical gap between components.
     * 
     * @param gap
     *            the new vertical gap to use
     */
    public void setGap(int gap)
    {
        this.gap = gap;
    }

    /**
     * Returns the current vertical orientation used.
     * 
     * @return the current vertical orientation used
     */
    public int getVerticalOrientation()
    {
        return verticalOrientation;
    }

    /**
     * Sets the vertical orientation used to layout components.
     * 
     * @param orient
     *            the new vertical orientation to use
     * 
     * @throws IllegalArgumentException
     *             if <code>orient</code> is not one of {@link #TOP},
     *             {@link #CENTER}, or {@link #BOTTOM}
     */
    public void setVerticalOrientation(int orient) throws IllegalArgumentException
    {
        if (orient != TOP && orient != CENTER && orient != BOTTOM)
        {
            throw new IllegalArgumentException("bad gap or orientation");
        }
        this.verticalOrientation = orient;
    }

    /**
     * Returns the current horizontal orientation used.
     * 
     * @return the current horizontal orientation used
     */
    public int getHorizontalOrientation()
    {
        return horizontalOrientation;
    }

    /**
     * Sets the horizontal orientation used to layout components.
     * 
     * @param orient
     *            the new horizontal orientation to use
     * 
     * @throws IllegalArgumentException
     *             if <code>orient</code> is not one of {@link #LEFT},
     *             {@link #CENTER}, or {@link #RIGHT}
     */
    public void setHorizontalOrientation(int orient) throws IllegalArgumentException
    {
        if (orient != LEFT && orient != CENTER && orient != RIGHT)
        {
            throw new IllegalArgumentException("bad gap or orientation");
        }
        this.horizontalOrientation = orient;
    }

    /** No-op. */
    public void addLayoutComponent(String name, Component comp)
    {
    }

    /** No-op. */
    public void removeLayoutComponent(Component comp)
    {
    }

    /**
     * Returns the preferred size for the given <code>Container</code> based on
     * this layout.
     * 
     * @param target
     *            the given <code>Container</code>
     * @return the preferred size for the given <code>Container</code>
     */
    public Dimension preferredLayoutSize(Container target)
    {
        return calcSize(target, SizingStrategy.prefSize);
    }

    /**
     * Returns the minimum size for the given <code>Container</code> based on
     * this layout.
     * 
     * @param target
     *            the given <code>Container</code>
     * @return the minimum size for the given <code>Container</code>
     */
    public Dimension minimumLayoutSize(Container target)
    {
        return calcSize(target, SizingStrategy.minSize);
    }

    /**
     * Lays out <code>Component</code>s in the given <code>Container</code> in a
     * vertical column.
     * 
     * @param target
     *            the container whose components should be layed out.
     */
    public void layoutContainer(Container target)
    {
        Insets insets = target.getInsets();
        int n = target.getComponentCount();
        int top = insets.top;
        int left = 0;
        Dimension tps = target.getPreferredSize();
        Dimension targetSize = target.getSize();

        if (verticalOrientation == CENTER)
            top = top + (targetSize.height / 2) - (tps.height / 2);
        else if (verticalOrientation == BOTTOM) top = targetSize.height - tps.height - insets.bottom;

        for (int i = 0; i < n; ++i)
        {
            Component c = target.getComponent(i);

            if (c.isVisible())
            {
                Dimension ps = c.getPreferredSize();

                if (horizontalOrientation == CENTER)
                    left = (targetSize.width / 2) - (ps.width / 2);
                else if (horizontalOrientation == LEFT)
                    left = insets.left;
                else if (horizontalOrientation == RIGHT) left = targetSize.width - ps.width - insets.right;

                c.setBounds(left, top, ps.width, ps.height);
                top += ps.height + gap;
            }
        }
    }

    /**
     * Calculate the minimum or preferred size for a <code>Container</code>
     * depending upon the given sizing strategy.
     * 
     * @param target
     *            the <code>Container</code> to calculate a size for
     * @param s
     *            the <code>SizingStrategy</code> to use
     */
    private Dimension calcSize(Container target, SizingStrategy s)
    {
        Insets insets = target.getInsets();
        int w = 0, h = 0;
        int n = target.getComponentCount();

        for (int i = 0; i < n; ++i)
        {
            Component c = target.getComponent(i);

            if (c.isVisible())
            {
                Dimension d = s.getSize(c);

                h += d.height;
                w = Math.max(d.width, w);

                if (i > 0) h += gap;
            }
        }

        return new Dimension(w + insets.left + insets.right, h + insets.top + insets.bottom);
    }
}
