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

import java.awt.LayoutManager;
import java.awt.LayoutManager2;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;

/**
 * The <code>WrapperLayout</code> does no layout. It is simply a placeholder.
 * Any actual layout work is passed on to another layout manager, if one is
 * given. If one is not given the <code>WrapperLayout</code> works as if
 * {@link Container#setLayout(LayoutManager) Container.setLayout(null)} were
 * called.
 * <p>
 * The <code>WrapperLayout</code> manager employs the <i>Decorator</i> design
 * pattern. The most common use is as a base class from which other
 * <i>decorating</i> layout managers can be created.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.5 $, $Date: 2002/06/03 21:34:38 $
 */
public class WrapperLayout implements LayoutManager2
{
    /**
     * Creates a <code>WrapperLayout</code> using the given layout as a base.
     * 
     * @param layout
     *            the <code>LayoutManager</code> on which this
     *            <code>WrapperLayout</code> is based.
     */
    public WrapperLayout(LayoutManager layout)
    {
        setLayout(layout);
    }

    /**
     * Adds the specified component with the specified name to the layout.
     * <p>
     * Defers to base layout given in constructor.
     * 
     * @param name
     *            the component name
     * @param comp
     *            the component to be added
     */
    public void addLayoutComponent(String name, Component comp)
    {
        if (layout != null) layout.addLayoutComponent(name, comp);
    }

    /**
     * Removes the specified component from the layout.
     * <p>
     * Defers to base layout given in constructor.
     * 
     * @param comp
     *            the component to be removed
     */
    public void removeLayoutComponent(Component comp)
    {
        if (layout != null) layout.removeLayoutComponent(comp);
    }

    /**
     * Calculates the preferred size dimensions for the specified panel given
     * the components in the specified parent container.
     * <p>
     * Defers to base layout given in constructor.
     * 
     * @param parent
     *            the component to be laid out
     *            minimumLayoutSize(java.awt.Container)
     */
    public Dimension preferredLayoutSize(Container parent)
    {
        if (layout != null)
            return layout.preferredLayoutSize(parent);
        else
            return parent.getSize();
    }

    /**
     * Calculates the minimum size dimensions for the specified panel given the
     * components in the specified parent container.
     * <p>
     * Defers to base layout given in constructor.
     * 
     * @param parent
     *            the component to be laid out
     *            preferredLayoutSize(java.awt.Container)
     */
    public Dimension minimumLayoutSize(Container parent)
    {
        if (layout != null)
            return layout.minimumLayoutSize(parent);
        else
            return new Dimension(0, 0);
    }

    /**
     * Lays out the given container.
     * <p>
     * Defers to base layout given in constructor for layout.
     * 
     * @param parent
     *            the container which needs to be laid out
     */
    public void layoutContainer(Container parent)
    {
        if (layout != null) layout.layoutContainer(parent);
    }

    /****************** LayoutManager2 *********************/

    /**
     * Adds the specified component to the layout, using the specified
     * constraint object.
     * <p>
     * Defers to base layout given in constructor (if it is an instance of
     * {@link LayoutManager2}.
     * 
     * @param comp
     *            the component to be added
     * @param constraints
     *            where/how the component is added to the layout.
     */
    public void addLayoutComponent(Component comp, Object constraints)
    {
        if (layout2 != null) layout2.addLayoutComponent(comp, constraints);
    }

    /**
     * Returns the maximum size of this component.
     * <p>
     * Defers to base layout given in constructor (if it is an instance of
     * {@link LayoutManager2}.
     */
    public Dimension maximumLayoutSize(Container target)
    {
        if (layout2 != null)
            return layout2.maximumLayoutSize(target);
        else
            return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);

    }

    /**
     * Returns the alignment along the x axis. This specifies how the component
     * would like to be aligned relative to other components. The value should
     * be a number between 0 and 1 where 0 represents alignment along the
     * origin, 1 is aligned the furthest away from the origin, 0.5 is centered,
     * etc.
     * <p>
     * Defers to base layout given in constructor (if it is an instance of
     * {@link LayoutManager2}.
     */
    public float getLayoutAlignmentX(Container target)
    {
        if (layout2 != null) return layout2.getLayoutAlignmentX(target);
        return 0.5f;
    }

    /**
     * Returns the alignment along the y axis. This specifies how the component
     * would like to be aligned relative to other components. The value should
     * be a number between 0 and 1 where 0 represents alignment along the
     * origin, 1 is aligned the furthest away from the origin, 0.5 is centered,
     * etc.
     * <p>
     * Defers to base layout given in constructor (if it is an instance of
     * {@link LayoutManager2}.
     */
    public float getLayoutAlignmentY(Container target)
    {
        if (layout2 != null) return layout2.getLayoutAlignmentY(target);
        return 0.5f;
    }

    /**
     * Invalidates the layout, indicating that if the layout manager has cached
     * information it should be discarded.
     * <p>
     * Defers to base layout given in constructor (if it is an instance of
     * {@link LayoutManager2}.
     */
    public void invalidateLayout(Container target)
    {
        if (layout2 != null) layout2.invalidateLayout(target);
    }

    /****************** WrapperLayout *********************/

    /**
     * Returns the compositor layout manager.
     * 
     * @return The layout manager that was given in the constructor.
     */
    public LayoutManager getLayout()
    {
        return layout;
    }

    /**
     * Sets the current layout strategy.
     * 
     * @param layout
     *            the LayoutManager to be used as a strategy
     */
    protected void setLayout(LayoutManager layout)
    {
        this.layout = layout;
        if (layout != null && (layout instanceof LayoutManager2)) this.layout2 = (LayoutManager2) layout;
    }

    /**
     * The base layout manager. <code>layout2</code> is non-<code>null</code>
     * only if <code>layout instanceof LayoutManager2</code>.
     */
    protected LayoutManager layout = null;

    protected LayoutManager2 layout2 = null;
}
