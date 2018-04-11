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

package org.cablelabs.gear.havi;

import java.awt.Component;

/**
 * <code>SnapContainer</code> is an extension of <code>FocusContainer</code>
 * which uses a key traversal setup strategy to setup keyboard traversals
 * automatically.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.4 $, $Date: 2002/06/03 21:33:19 $
 */
public class SnapContainer extends FocusContainer
{
    /**
     * Creates <code>SnapContainer</code> object with x/y coordinates and
     * width/height dimensions initialized to their default values.
     */
    public SnapContainer()
    {
        super();
        iniz(null);
    }

    /**
     * Creates <code>SnapContainer</code> object with x/y coordinates and
     * width/height dimensions initialized to the given values.
     * 
     * @param x
     *            initial x-coordinate
     * @param y
     *            initial y-coordinate
     * @param width
     *            initial width
     * @param height
     *            initial height
     */
    public SnapContainer(int x, int y, int width, int height)
    {
        super(x, y, width, height);
        iniz(null);
    }

    /**
     * Creates <code>SnapContainer</code> object with x/y coordinates,
     * width/height dimensions, and <code>SnapLayout</code> strategy initialized
     * to the given values.
     * 
     * @param x
     *            initial x-coordinate
     * @param y
     *            initial y-coordinate
     * @param width
     *            initial width
     * @param height
     *            initial height
     * @param snap2
     *            the focus traversal strategy
     */
    public SnapContainer(int x, int y, int width, int height, SnapLayout snap2)
    {
        super(x, y, width, height);
        iniz(snap2);
    }

    /** Common to all constructors. */
    private void iniz(SnapLayout snap2)
    {
        if (snap2 == null) snap2 = new SnapLayout();
        this.snap2 = snap2;
        setLayout(null);
    }

    /**
     * Overrides <code>Container.doLayout()</code> to additionally set up focus
     * traversals.
     * <p>
     * Note that {@link #layout()} is not overridden; it's use is deprecated and
     * will not give the intended results.
     */
    public void doLayout()
    {
        super.doLayout();
        setupTraversals();
    }

    /**
     * Overrides <code>Container.invalidate()</code> additionally invalidate
     * focus traversals.
     */
    public void invalidate()
    {
        super.invalidate();
        snap2.invalidateLayout(this);
    }

    /**
     * Overrides <code>Container.addImpl</code> to strip off any
     * {@link SnapLayoutConstraints} and pass them on to the
     * <code>SnapLayout</code> key traversal setup strategy.
     * 
     * @param comp
     *            the component to be added.
     * @param constraints
     *            an object expressing layout contraints for this component.
     * @param index
     *            the position in the container's list at which to insert the
     *            component, where <code>-1</code> means insert at the end.
     */
    protected void addImpl(Component comp, Object constraints, int index)
    {
        synchronized (getTreeLock())
        {
            SnapLayoutConstraints s2c = null;
            if (constraints instanceof SnapLayoutConstraints)
            {
                s2c = (SnapLayoutConstraints) constraints;
                constraints = s2c.constraints;
            }
            super.addImpl(comp, constraints, index);

            if (s2c != null) snap2.addLayoutComponent(comp, s2c);
        }
    }

    /**
     * Overrides <code>Container.remove(int)</code> to additionally notify the
     * <code>SnapLayout</code> of the component's removal.
     * 
     * @param index
     *            the index of the component to be removed
     */
    public void remove(int index)
    {
        synchronized (getTreeLock())
        {
            Component comp = getComponent(index);
            super.remove(index);
            snap2.removeLayoutComponent(comp);
        }
    }

    /**
     * Overrides <code>Container.removeAll()</code> to additionally notify the
     * <code>SnapLayout</code> of the components' removal.
     */
    public void removeAll()
    {
        synchronized (getTreeLock())
        {
            Component comps[] = getComponents();
            super.removeAll();
            for (int i = 0; i < comps.length; ++i)
                snap2.removeLayoutComponent(comps[i]);
        }
    }

    /**
     * Overrides <code>Container.paramString()</code> to additionally provide
     * information about the <code>SnapLayout</code> strategy.
     * 
     * @return the parameter string of this container
     */
    protected String paramString()
    {
        return super.paramString() + ",snap=" + snap2.getClass().getName();
    }

    /*  ******************** SnapContainer ******************* */

    /**
     * Causes this <code>SnapContainer</code> to set up keyboard traversals
     * between components contained within. This is called every time
     * {@link #doLayout()} or {@link #validate()} is invoked.
     */
    public void setupTraversals()
    {
        if (!snapLocked) snap2.layoutContainer(this);
    }

    /**
     * Locks the current keyboard traversal settings. Any further calls to
     * {@link #setupTraversals}, {@link #validate}, or {@link #doLayout} will
     * not result in any changes to the key traversals.
     * 
     * @param locked
     *            if <code>true</code> lock the current settings; if
     *            <code>false</code> unlock and allow settings to be changed
     */
    public void setSnapLocked(boolean locked)
    {
        snapLocked = locked;
    }

    /**
     * Returns whether keyboard traversal settings are locked as they are and
     * any calls to {@link #setupTraversals}, {@link #validate}, or
     * {@link #doLayout} will result in changes or not.
     * 
     * @return <code>true</code> if the current settings are locked;
     *         <code>false</code> if the current settings are changeable by
     *         <code>setupTraversals</code>, <code>validate</code>, or
     *         <code>doLayout</code>
     */
    public boolean isSnapLocked()
    {
        return snapLocked;
    }

    /**
     * Controls the <i>wrapped</i> property of this <code>SnapContainer</code>.
     * When enabled, traversals are setup such that they <i>wrap</i>. In other
     * words, components at the outer edge of the container will have they
     * traversals <i>away</i> from the container set to be a component at the
     * opposite outer edge of the container.
     * 
     * @param wrapped
     *            if <code>true</code> then wrapping is enabled; else wrapping
     *            is disabled
     */
    public void setWrapped(boolean wrapped)
    {
        snap2.setWrap(wrapped);
    }

    /**
     * Returns whether wrapping is enabled or disabled.
     * 
     * @return <code>true</code> if wrapping is enabled; <code>false</code>
     *         otherwise
     * @see #setWrapped(boolean)
     */
    public boolean isWrapped()
    {
        return snap2.getWrap();
    }

    /**
     * Sets the <i>straight</i> portion of the <code>straight:close</code> ratio
     * used in calculating keyboard traversals. If <code>straight</code> is >
     * <code>close</code> then selecting components in a straight line is deemed
     * more important than selecting the closest component.
     * 
     * @param straight
     *            the straight portion of the straight:close ratio
     */
    public void setStraightWeight(int straight)
    {
        snap2.setStraightWeight(straight);
    }

    /**
     * Sets the <i>close</i> portion of the <code>straight:close</code> ratio
     * used in calculating keyboard traversals. If <code>close</code> is >
     * <code>close</code> then selecting the closest component is deemed more
     * important than selecting components in a straight line.
     * 
     * @param close
     *            the close portion of the straight:close ratio
     */
    public void setCloseWeight(int close)
    {
        snap2.setCloseWeight(close);
    }

    /**
     * Returns the straight portion of the straight:close ratio.
     * 
     * @return the straight portion of the straight:close ratio
     * @see #setStraightWeight(int)
     */
    public int getStraightWeight()
    {
        return snap2.getStraightWeight();
    }

    /**
     * Returns the close portion of the straight:close ratio.
     * 
     * @return the close portion of the straight:close ratio
     * @see #setCloseWeight(int)
     */
    public int getCloseWeight()
    {
        return snap2.getCloseWeight();
    }

    /**
     * Sets the current <code>SnapLayout</code> used to set up focus traversals.
     * Provided primarily for testing purposes.
     * 
     * @param snap2
     *            the new current <code>SnapLayout</code>
     */
    void setSnap2(SnapLayout snap2)
    {
        this.snap2 = snap2;
    }

    /**
     * Associates the given <code>SnapLayoutConstraints</code> with the given
     * <code>Component</code>. The constraints provide hints as to how focus
     * traversals should be set up with respect to the given component.
     * <p>
     * These constraints can also be set when adding a component to this
     * <code>Container</code>.
     * 
     * @param c
     *            the <code>HNavigable</code> component
     * @param constraints
     *            specifies how focus traversals should be setup with respect to
     *            the given component
     * 
     * @see SnapLayoutConstraints
     * @see #add(Component, Object)
     * @see #add(Component, Object, int)
     */
    public void setConstraints(Component c, SnapLayoutConstraints constraints)
    {
        snap2.setConstraints(c, constraints);
    }

    /**
     * Returns the constraints currently associated with the given
     * <code>Component</code>. The constraints provide hints as to how focus
     * traversals should be set up with respect to the given component.
     * 
     * @param c
     *            the <code>HNavigable</code> component
     * @return the <code>SnapLayoutConstraints</code> which provide hints on how
     *         focus traversals should be setup with respect to the given
     *         component
     * 
     * @see SnapLayoutConstraints
     */
    public SnapLayoutConstraints getConstraints(Component c)
    {
        return snap2.getConstraints(c);
    }

    /**
     * Retrieves the current <code>SnapLayout</code> used to set up focus
     * traversals. Provided primarily for testing purposes.
     * 
     * @return snap2 the current <code>SnapLayout</code>
     */
    SnapLayout getSnap2()
    {
        return snap2;
    }

    /** The SnapLayout strategy object. */
    private SnapLayout snap2;

    /** Specifies whether traversals are locked. */
    private boolean snapLocked;
}
