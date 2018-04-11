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
 * Copyright 2000-2003 by HAVi, Inc. Java is a trademark of Sun
 * Microsystems, Inc. All rights reserved.  
 */

package org.havi.ui;

import java.awt.Image;
import java.awt.Shape;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Graphics;
import org.cablelabs.impl.havi.HaviToolkit;
import org.cablelabs.impl.havi.MatteCompositor; // following were added because of processEvent() kludge
import java.awt.event.*;
import org.havi.ui.event.*;

/* PLEASE NOTE:
 * There is a problem (not necessarily a bug, because it may be 
 * allowed) with the Z-Ordering support.  The HContainer/HScene
 * API specifications specifically state that an implementation
 * can be done with add/remove calls.  The problem, however, is
 * that these calls can have side-effects, especially when a
 * LayoutManager2 is involved.
 *
 * E.g., components are added with layout constraints, which are
 * passed on to the LayoutManager2.  Later, we call one of the 
 * Z-ordering methods (e.g., popToFront()) which will result in
 * removal and adding of components.
 * The remove calls will result in the LayoutManager2 (likely)
 * forgetting about the constraints that were previously passed.
 * Then the re-adds will pass on null constraints.  
 * Effectively, the Z-ordering methods can result in loss of constraints.
 *
 * Solutions?  
 * 1) Manipulate the component list directly.  No can do.  It is 
 *    package-private to Container.
 * 2) Save and restore the layoutManager, such that it isn't notified
 *    about the removal/add.  No can do.  The layoutManager might be
 *    sensitive to the ultimate position of the component and might
 *    want to know where it is.
 * 3) The HContainer implementation must keep track of the constraints
 *    itself (as long as there is a LayoutManager).  Then, these constraints
 *    can be passed along with the add() following a remove.
 * 4) Do nothing.  If the API specification state that this side-effect
 *    is possible, then I would have no problem with it.
 *
 * Only 3 & 4 seem viable.  For now, we are sticking with 4.
 * I believe that this problem could come up in other implementations,
 * and changing ours could result in us depending upon things working
 * in this manner.
 *
 * Aaron Kamienski
 */

/**
 * The {@link org.havi.ui.HContainer HContainer} class extends the
 * <code>java.awt.Container</code> class by implementing the
 * {@link org.havi.ui.HMatteLayer HMatteLayer} interface and providing
 * additional Z-ordering capabilities, which are required since components in
 * the HAVi user-interface are explicitly allowed to overlap each other.
 * <p>
 * Note that these Z-ordering capabilities (<code>addBefore,
     addAfter, pop, popInFrontOf, popToFront, push, pushBehind and
     pushToBack</code>) must be implemented by (implicitly) reordering the child
 * Components within the {@link org.havi.ui.HContainer HContainer}, so that the
 * standard AWT convention that the Z-order is defined as the order in which
 * Components are added to a given Container is maintained. For example, one
 * implementation of <code>popToFront</code> might be to make the specified
 * Component become the first Component added to the parent Container by
 * removing all Components from that Container, adding the specified Container
 * first, and then adding the remaining Components in their current relative
 * order to that Container.
 * 
 * <hr>
 * The parameters to the constructors are as follows, in cases where parameters
 * are not used, then the constructor should use the default values.
 * <p>
 * <h3>Default parameter values exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Parameter</th>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * <tr>
 * <td>x</td>
 * <td>x-coordinate of top left hand corner of this component in pixels,
 * relative to its parent container (subject to layout management).</td>
 * <td>---</td>
 * <td>java.awt.Component#setBounds</td>
 * <td>java.awt.Component#getBounds</td>
 * </tr>
 * <tr>
 * <td>y</td>
 * <td>y-coordinate of top left hand corner of this component in pixels,
 * relative to its parent container (subject to layout management).</td>
 * <td>---</td>
 * <td>java.awt.Component#setBounds</td>
 * <td>java.awt.Component#getBounds</td>
 * </tr>
 * <tr>
 * <td>width</td>
 * <td>width of this component in pixels (subject to layout management).</td>
 * <td>---</td>
 * <td>java.awt.Component#setBounds</td>
 * <td>java.awt.Component#getBounds</td>
 * </tr>
 * <tr>
 * <td>height</td>
 * <td>height of this component in pixels (subject to layout management).</td>
 * <td>---</td>
 * <td>java.awt.Component#setBounds</td>
 * <td>java.awt.Component#getBounds</td>
 * </tr>
 * 
 * </table>
 * 
 * <h3>Default parameter values not exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * <tr>
 * <td>Associated matte ({@link org.havi.ui.HMatte HMatte}).</td>
 * <td>none (i.e. getMatte() returns <code>null</code>)</td>
 * <td>{@link org.havi.ui.HComponent#setMatte setMatte}</td>
 * <td>{@link org.havi.ui.HComponent#getMatte getMatte}</td>
 * </tr>
 * <tr>
 * <td>LayoutManager</td>
 * <td><code>null</code> (in contrast to java.awt.Container)</td>
 * <td>java.awt.Container#setLayout</td>
 * <td>java.awt.Container#getLayout</td>
 * </tr>
 * </table>
 * 
 * @author Alex Resh
 * @author Todd Earles (Matte support)
 * @author Aaron Kamienski (push/pop rewrite, 1.0.1, 1.1)
 * @version 1.1
 */

public class HContainer extends java.awt.Container implements HMatteLayer, HComponentOrdering, org.dvb.ui.TestOpacity
{
    /**
     * Creates an HContainer object. See the class description for details of
     * constructor parameters and default values.
     */
    public HContainer()
    {
        // ...
    }

    /**
     * Creates an HContainer object. See the class description for details of
     * constructor parameters and default values.
     */
    public HContainer(int x, int y, int width, int height)
    {
        super.setBounds(x, y, width, height);
    }

    /**
     * Applies an {@link org.havi.ui.HMatte HMatte} to this component, for matte
     * compositing. Any existing animated matte must be stopped before this
     * method is called or an HMatteException will be thrown.
     * 
     * @param m
     *            The {@link org.havi.ui.HMatte HMatte} to be applied to this
     *            component -- note that only one matte may be associated with
     *            the component, thus any previous matte will be replaced. If m
     *            is null, then any matte associated with the component is
     *            removed and further calls to getMatte() shall return null. The
     *            component shall behave as if it had a fully opaque
     *            {@link org.havi.ui.HFlatMatte HFlatMatte} associated with it
     *            (i.e an HFlatMatte with the default value of 1.0.)
     * @exception HMatteException
     *                if the {@link org.havi.ui.HMatte HMatte} cannot be
     *                associated with the component. This can occur:
     *                <ul>
     *                <li>if the specific matte type is not supported
     *                <li>if the platform does not support any matte type
     *                <li>if the component is associated with an already running
     *                {@link org.havi.ui.HFlatEffectMatte HFlatEffectMatte} or
     *                {@link org.havi.ui.HImageEffectMatte HImageEffectMatte}.
     *                The exception is thrown even if m is null.
     *                </ul>
     * @see HMatte
     */
    public void setMatte(HMatte m) throws HMatteException
    {
        // Check for unknown matte type
        if (m != null && !toolkit.isMatteSupported(m)) throw new HMatteException("Unknown HMatte type");

        // If the currently assigned matte is a matte effect, then unregister
        // this component with it.
        final HMatte current = containerMatte;
        if (current instanceof HFlatEffectMatte)
        {
            HFlatEffectMatte matte = (HFlatEffectMatte) current;
            if (matte.isAnimated()) throw new HMatteException("An HFlatEffectMatte is already running");
            matte.unregisterComponent(this);
        }
        else if (current instanceof HImageEffectMatte)
        {
            HImageEffectMatte matte = (HImageEffectMatte) current;
            if (matte.isAnimated()) throw new HMatteException("An HImageEffectMatte is already running");
            matte.unregisterComponent(this);
        }

        // Assign the matte to this component
        containerMatte = m;

        // If the newly assigned matte is a matte effect, then register this
        // component with it.
        if (m instanceof HFlatEffectMatte)
            ((HFlatEffectMatte) m).registerComponent(this);
        else if (m instanceof HImageEffectMatte) ((HImageEffectMatte) m).registerComponent(this);
    }

    /**
     * Get any {@link org.havi.ui.HMatte HMatte} currently associated with this
     * component.
     * 
     * @return the {@link org.havi.ui.HMatte HMatte} currently associated with
     *         this component or null if there is no associated matte.
     */
    public HMatte getMatte()
    {
        return containerMatte;
    }

    /**
     * Returns <code>true</code> if all the drawing done during the update and
     * paint methods for this specific {@link org.havi.ui.HContainer HContainer}
     * object is automatically double buffered.
     * 
     * @return <code>true</code> if all the drawing done during the update and
     *         paint methods for this specific {@link org.havi.ui.HComponent
     *         HComponent} object is automatically double buffered, or
     *         <code>false</code> if drawing is not double buffered. The default
     *         value for the double buffering setting is platform-specific.
     */
    public boolean isDoubleBuffered()
    {
        return doubleBuffered;
    }

    /**
     * Returns true if the entire {@link org.havi.ui.HContainer HContainer}
     * area, as given by the <code>java.awt.Component#getBounds</code> method,
     * is fully opaque, i.e. its paint method (or surrogate methods) guarantee
     * that all pixels are painted in an opaque <code>Color</code>.
     * <p>
     * By default, the return value is <code>false</code>. The return value
     * should be overridden by subclasses that can guarantee full opacity. The
     * consequences of an invalid overridden value are implementation specific.
     * 
     * @return <code>true</code> if all the pixels within the area given by the
     *         <code>java.awt.Component#getBounds</code> method are fully
     *         opaque, i.e. its paint method (or surrogate methods) guarantee
     *         that all pixels are painted in an opaque Color, otherwise
     *         <code>false</code>.
     */
    public boolean isOpaque()
    {
        return opaque;
    }

    /**
     * Adds a <code>java.awt.Component</code> to this
     * {@link org.havi.ui.HContainer HContainer} directly in front of a
     * previously added <code>java.awt.Component</code>.
     * <p>
     * If <code>component</code> has already been added to this container, then
     * <code>addBefore</code> moves <code>component</code> in front of
     * <code>behind</code>. If <code>behind</code> and <code>component</code>
     * are the same component which was already added to this container,
     * <code>addBefore</code> does not change the ordering of the components and
     * returns <code>component</code>.
     * <p>
     * This method affects the Z-order of the <code>java.awt.Component</code>
     * children within the {@link org.havi.ui.HContainer HContainer}, and may
     * also implicitly change the numeric ordering of those children.
     * 
     * @param component
     *            is the <code>java.awt.Component</code> to be added to the
     *            {@link org.havi.ui.HContainer HContainer}
     * @param behind
     *            is the <code>java.awt.Component</code>, which
     *            <code>component</code> will be placed in front of, i.e.
     *            <code>behind</code> will be directly behind the added
     *            <code>java.awt.Component</code>
     * @return If the <code>java.awt.Component</code> is successfully added or
     *         was already present, then it will be returned from this call. If
     *         the <code>java.awt.Component</code> is not successfully added,
     *         e.g. <code>behind</code> is not a <code>java.awt.Component</code>
     *         currently added to the {@link org.havi.ui.HContainer HContainer},
     *         then <code>null</code> will be returned.
     *         <p>
     *         This method must be implemented in a thread safe manner.
     */
    public java.awt.Component addBefore(java.awt.Component component, java.awt.Component behind)
    {
        // check to see if behind is an element of this container
        try
        {
            getOffset(behind);
        }
        catch (Exception e)
        {
            return null;
        }

        if (component == behind) return component;
        synchronized (getTreeLock())
        {
            try
            {
                // Explicitly remove component if in this container.
                // Should have no effect if not in this container.
                // This must be done so that problems don't occur
                // when componentIndex < frontIndex.
                remove(component);

                int offset = getOffset(behind);

                return add(component, offset);
            }
            catch (Exception e)
            {
                return null;
            }
        }
    }

    /**
     * Adds a <code>java.awt.Component</code> to this
     * {@link org.havi.ui.HContainer HContainer} directly behind a previously
     * added <code>java.awt.Component</code>.
     * <p>
     * If <code>component</code> has already been added to this container, then
     * addAfter moves <code>component</code> behind <code>front</code>. If
     * <code>front</code> and <code>component</code> are the same component
     * which was already added to this container, <code>addAfter</code> does not
     * change the ordering of the components and returns <code>component</code>.
     * <p>
     * This method affects the Z-order of the <code>java.awt.Component</code>
     * children within the {@link org.havi.ui.HContainer HContainer}, and may
     * also implicitly change the numeric ordering of those children.
     * 
     * @param component
     *            is the <code>java.awt.Component</code> to be added to the
     *            {@link org.havi.ui.HContainer HContainer}
     * @param front
     *            is the <code>java.awt.Component</code>, which
     *            <code>component</code> will be placed behind, i.e.
     *            <code>front</code> will be directly in front of the added
     *            <code>java.awt.Component</code>
     * @return If the <code>java.awt.Component</code> is successfully added or
     *         was already present, then it will be returned from this call. If
     *         the <code>java.awt.Component</code> is not successfully added,
     *         e.g. front is not a <code>java.awt.Component</code> currently
     *         added to the {@link org.havi.ui.HContainer HContainer}, then
     *         <code>null</code> will be returned.
     *         <p>
     *         This method must be implemented in a thread safe manner.
     */
    public java.awt.Component addAfter(java.awt.Component component, java.awt.Component front)
    {
        // check to see if front is an element of this container
        try
        {
            getOffset(front);
        }
        catch (Exception e)
        {
            return null;
        }

        if (component == front) return component;
        synchronized (getTreeLock())
        {
            try
            {
                // Explicitly remove component if in this container.
                // Should have no effect if not in this container.
                // This must be done so that problems don't occur
                // when componentIndex < frontIndex.
                remove(component);

                int offset = getOffset(front);

                return add(component, offset + 1);
            }
            catch (Exception e)
            {
                return null;
            }
        }
    }

    /**
     * Brings the specified <code>java.awt.Component</code> to the
     * &quot;front&quot; of the Z-order in this {@link org.havi.ui.HContainer
     * HContainer}.
     * <p>
     * If <code>component</code> is already at the front of the Z-order, the
     * order is unchanged and <code>popToFront</code> returns <code>true</code>.
     * 
     * @param component
     *            The <code>java.awt.Component</code> to bring to the
     *            &quot;front&quot; of the Z-order of this
     *            {@link org.havi.ui.HContainer HContainer}.
     * 
     * @return returns <code>true</code> on success, <code>false</code> on
     *         failure, for example when the <code>java.awt.Component</code> has
     *         yet to be added to the {@link org.havi.ui.HContainer HContainer}.
     *         If this method fails, the Z-order is unchanged.
     */
    public boolean popToFront(java.awt.Component component)
    {
        synchronized (getTreeLock())
        {
            try
            {
                // Ensure it is there
                checkLineage(component);

                // explicitly remove component
                // (even if reparenting is implicit)
                remove(component);
                add(component, 0);
                return true;
            }
            catch (Exception e)
            {
                return false;
            }
        }
    }

    /**
     * Place the specified <code>java.awt.Component</code> at the
     * &quot;back&quot; of the Z-order in this {@link org.havi.ui.HContainer
     * HContainer}.
     * <p>
     * If <code>component</code> is already at the back the Z-order is unchanged
     * and <code>pushToBack</code> returns <code>true</code>.
     * 
     * @param component
     *            The <code>java.awt.Component</code> to place at the
     *            &quot;back&quot; of the Z-order of this
     *            {@link org.havi.ui.HContainer HContainer}.
     * @return returns <code>true</code> on success, <code>false</code> on
     *         failure, for example when the <code>java.awt.Component</code> has
     *         yet to be added to the {@link org.havi.ui.HContainer HContainer}.
     *         If the component was not added to the container
     *         <code>pushToBack</code> does not change the Z-order.
     */
    public boolean pushToBack(java.awt.Component component)
    {
        synchronized (getTreeLock())
        {
            try
            {
                // Ensure it is there
                checkLineage(component);

                // explicitly remove component
                // (even if reparenting is implicit)
                remove(component);
                add(component, -1);
                return true;
            }
            catch (Exception e)
            {
                return false;
            }
        }
    }

    /**
     * Moves the specified <code>java.awt.Component</code> one component nearer
     * in the Z-order, i.e. swapping it with the <code>java.awt.Component</code>
     * that was directly in front of it.
     * <p>
     * If <code>component</code> is already at the front of the Z-order, the
     * order is unchanged and <code>pop</code> returns <code>true</code>.
     * 
     * @param component
     *            The <code>java.awt.Component</code> to be moved.
     * @return returns <code>true</code> on success, <code>false</code> on
     *         failure, for example if the <code>java.awt.Component</code> has
     *         yet to be added to the {@link org.havi.ui.HContainer HContainer}.
     */
    public boolean pop(java.awt.Component component)
    {
        synchronized (getTreeLock())
        {
            try
            {
                int offset = getOffset(component);

                if (offset > 0)
                {
                    // explicitly remove component
                    // (even if reparenting is implicit)
                    remove(component);
                    add(component, offset - 1);
                    return true;
                }
            }
            catch (Exception e)
            {
            }
            return false;
        }
    }

    /**
     * Moves the specified <code>java.awt.Component</code> one component further
     * away in the Z-order, i.e. swapping it with the
     * <code>java.awt.Component</code> that was directly behind it.
     * <p>
     * If <code>component</code> is already at the back of the Z-order, the
     * order is unchanged and <code>push</code> returns <code>true</code>.
     * 
     * @param component
     *            The <code>java.awt.Component</code> to be moved.
     * @return returns <code>true</code> on success, <code>false</code> on
     *         failure, for example if the <code>java.awt.Component</code> has
     *         yet to be added to the {@link org.havi.ui.HContainer HContainer}.
     */
    public boolean push(java.awt.Component component)
    {
        synchronized (getTreeLock())
        {
            try
            {
                int offset = getOffset(component);
                int count = getComponentCount();

                if (offset == (count - 1))
                {
                    return true;
                }

                if (offset < (count - 1))
                {
                    // explicitly remove component
                    // (even if reparenting is implicit)
                    remove(component);
                    add(component, offset + 1);
                    return true;
                }
            }
            catch (Exception e)
            {
            }

            return false;
        }
    }

    /**
     * Puts the specified <code>java.awt.Component</code> in front of another
     * <code>java.awt.Component</code> in the Z-order of this
     * {@link org.havi.ui.HContainer HContainer}.
     * <p>
     * If <code>move</code> and <code>behind</code> are the same component which
     * has been added to the container <code>popInFront</code> does not change
     * the Z-order and returns <code>true</code>.
     * 
     * @param move
     *            The <code>java.awt.Component</code> to be moved directly in
     *            front of the &quot;behind&quot; Component in the Z-order of
     *            this {@link org.havi.ui.HContainer HContainer}.
     * @param behind
     *            The <code>java.awt.Component</code> which the &quot;move&quot;
     *            Component should be placed directly in front of.
     * @return returns <code>true</code> on success, <code>false</code> on
     *         failure, for example when either <code>java.awt.Component</code>
     *         has yet to be added to the {@link org.havi.ui.HContainer
     *         HContainer}. If this method fails, the Z-order is unchanged.
     */
    public boolean popInFrontOf(java.awt.Component move, java.awt.Component behind)
    {
        synchronized (getTreeLock())
        {
            try
            {
                if (move != behind)
                {
                    // Ensure they are present
                    checkLineage(move);
                    checkLineage(behind);

                    // explicitly remove component
                    // (even if reparenting is implicit)
                    remove(move);
                    // Re-add
                    addBefore(move, behind);
                }
                return true;
            }
            catch (Exception e)
            {
                return false;
            }
        }
    }

    /**
     * Puts the specified <code>java.awt.Component</code> behind another
     * <code>java.awt.Component</code> in the Z-order of this
     * {@link org.havi.ui.HContainer HContainer}.
     * <p>
     * If <code>move</code> and <code>front</code> are the same component which
     * has been added to the container <code>pushBehind</code> does not change
     * the Z-order and returns <code>true</code>.
     * 
     * @param move
     *            The <code>java.awt.Component</code> to be moved directly
     *            behind the &quot;front&quot; Component in the Z-order of this
     *            {@link org.havi.ui.HContainer HContainer}.
     * @param front
     *            The <code>java.awt.Component</code> which the &quot;move&quot;
     *            Component should be placed directly behind.
     * @return returns <code>true</code> on success, <code>false</code> on
     *         failure, for example when either <code>java.awt.Component</code>
     *         has yet to be added to the {@link org.havi.ui.HContainer
     *         HContainer}.
     */
    public boolean pushBehind(java.awt.Component move, java.awt.Component front)
    {
        synchronized (getTreeLock())
        {
            try
            {
                if (move != front)
                {
                    // Ensure they are present
                    checkLineage(move);
                    checkLineage(front);

                    // explicitly remove component
                    // (even if reparenting is implicit)
                    remove(move);
                    // re-add in proper location
                    addAfter(move, front);
                }
                return true;
            }
            catch (Exception e)
            {
                return false;
            }
        }
    }

    /**
     * Groups the HContainer and its components. If the container is already
     * grouped this method has no effect
     * 
     * @see org.havi.ui.HContainer#ungroup
     * @see org.havi.ui.HContainer#isGrouped
     */
    public void group()
    {
        grouped = true;
    }

    /**
     * Ungroups the HContainer and its components. If the container is already
     * ungrouped, this method has no effect.
     * 
     * @see org.havi.ui.HContainer#group
     * @see org.havi.ui.HContainer#isGrouped
     */
    public void ungroup()
    {
        grouped = false;
    }

    /**
     * Tests whether the HContainer and its components are grouped. By default
     * the container is not grouped with its components.
     * 
     * @return returns <code>true</code> if the HContainer and its components
     *         are grouped, <code>false</code> otherwise.
     * 
     * @see org.havi.ui.HContainer#group
     * @see org.havi.ui.HContainer#ungroup
     */
    public boolean isGrouped()
    {
        return grouped;
    }

    /**
     * Override paint so that mattes can be applied to containers and
     * components. If <code>HContainer</code> is subclassed, then super.paint(g)
     * must be called to ensure that mattes are properly applied.
     * 
     * @param g
     *            the graphics context
     */
    public void paint(java.awt.Graphics g)
    {
        // Create the compositor the first time paint is called
        if (matteCompositorSet == false)
        {
            matteCompositor = toolkit.getMatteCompositor(this);
            matteCompositorSet = true;
        }

        // If no platform specific matte compositor then just perform a
        // awt (non-matted) paint of all child components.
        if (matteCompositor == null)
        {
            super.paint(g);
        }
        // Otherwise, (potentially) perform a matted paint of this container
        // and all child components.
        else
        {
            // If this container is ungrouped and has a matte then
            // apply the matte before painting the children.
            // Note that the background of this container is already
            // painted by the time we get here.
            if ((grouped == false) && (containerMatte != null))
                matteCompositor.composite(g, null, null, containerMatte);

            // Paint each child from back to front.
            Shape savedClip = g.getClip();
            Component components[] = getComponents();
            int count = components.length;
            while (--count >= 0)
            {
                Component c = components[count];
                paintComponent(c, c.getBounds(), g, savedClip);
            }

            // If this container is grouped and has a matte then
            // apply the matte now that the children have been painted.
            if ((grouped == true) && (containerMatte != null))
                matteCompositor.composite(g, null, null, containerMatte);
        }
    }

    /**
     * Render (if appropriate) the given component to the given graphics
     * context. The component will not be rendered if it is not
     * {@link Component#isVisible() visible} or if it's bounds do not intersect
     * with the current clipping <code>Shape</code>.
     * 
     * @param c
     *            the <code>HComponent</code> or <code>HContainer</code> to
     *            render
     * @param bounds
     *            the bounds of the component
     * @param G
     *            the current graphics context, clipped for this
     *            <code>HContainer</code>
     * @param clip
     *            the clipping rectangle of <code>G</code>
     * 
     * @see #paint(Graphics)
     */
    private void paintComponent(Component c, Rectangle bounds, Graphics G, Shape clip)
    {
        // Do not paint if not visible
        if (!c.isVisible()) return;

        // Do not paint if not w/in clipping
        // NOTE: Shape.intersects() doesn't exist pre-Java2.
        // Thus, if clip is not a Rectangle, we will always paint :-(
        if (clip != null && ((clip instanceof Rectangle) && !bounds.intersects((Rectangle) clip))) return;

        Graphics g = G.create(bounds.x, bounds.y, bounds.width, bounds.height);
        try
        {
            // Standard AWT setup...
            g.setFont(c.getFont());
            g.setColor(c.getForeground());

            // Get the matte for the component
            // And paint the component as appropriate
            HMatte matte;
            if ((c instanceof HMatteLayer) && (matte = ((HMatteLayer) c).getMatte()) != null)
            {
                // paint using matte
                paintWithMatte(c, bounds, g, matte);
            }
            else
            {
                // standard AWT painting
                c.paint(g);
            }
        }
        finally
        {
            g.dispose();
        }
    }

    /**
     * Render the given component using the given matte. It is assumed that the
     * componnent <code>c</code> is either an <code>HComponent</code> or an
     * <code>HContainer</code>. It is also assumed that the current
     * <code>MatteCompositor</code> is non-<code>null</code>.
     * 
     * @param c
     *            the <code>HComponent</code> or <code>HContainer</code> to
     *            render
     * @param bounds
     *            the bounds of the component
     * @param g
     *            the current graphics context, clipped to the component bounds
     * @param matte
     *            the <code>HMatte</code> to use
     * 
     * @see #paint(Graphics)
     * @see #paintComponent(Component, Rectangle, Graphics, Shape)
     */
    private void paintWithMatte(Component c, Rectangle bounds, Graphics g, HMatte matte)
    {
        // Allocate an off-screen image and create a graphics object for it.
        Image offScreenImage = matteCompositor.createOffScreenImage(bounds.width, bounds.height);
        Graphics gOffScreen = matteCompositor.createOffScreenGraphics(offScreenImage);
        try
        {
            gOffScreen.setClip(g.getClip());

            // Paint the component to the off-screen image.
            c.paint(gOffScreen);

            // The compositing we do here depends on whether this
            // component is an HContainer or not.
            if (c instanceof HContainer)
            {
                // This component is an HContainer.
                // Therefore, we composite the component (in the
                // off-screen image) with its parent.
                // We do not apply the matte here since that has
                // already been done.
                matteCompositor.composite(g, offScreenImage);
            }
            else
            {
                // This component is not an HContainer.
                // Therefore, we need to composite the component
                // (in the off-screen image) with its matte.
                // Then we composite the result with the parent
                // of this component.
                matteCompositor.composite(g, gOffScreen, offScreenImage, matte);
            }
        }
        finally
        {
            // Dispose of the off-screen image and associated graphics
            // context
            gOffScreen.dispose();
            offScreenImage = null;
        }
    }

    /**
     * <b>Kludge alert!!!</b>
     * <p>
     * This is a complete copy of {@link HComponent#processEvent(AWTEvent)}.
     * This is <i>not</i> specified by HAVi (although we believe that it should
     * be). This is here to allow users to implement the HAVi widget interfaces
     * in <code>HContainer</code> subclasses. Without it, it is not possible to
     * have an <code>HNavigable</code> container!
     * <p>
     * Adding <code>Container</code>-like functionality to a subclass of
     * <code>HComponent</code> is not a viable alternative: nothing will
     * recognize it as a <code>Container</code>. <font size=-1>(It's a shame
     * that AWT isn't interface based; i.e., that there is no
     * <code>Container</code> interface)</font>
     * <p>
     * This functionality is enabled by the <code>TRANS_EVENT</code> private
     * boolean constant.
     * 
     * @param evt
     *            the java.awt.AWTEvent to handle.
     */
    protected void processEvent(java.awt.AWTEvent evt)
    {
        if (!TRANS_EVENTS)
            super.processEvent(evt);
        else
        {
            // Create HAVi events from AWT events..
            if (this instanceof HKeyboardInputPreferred)
            {
                HKeyEvent e = toolkit.makeKeyEvent(evt);
                if (e != null) ((HKeyboardInputPreferred) this).processHKeyEvent(e);
            }
            if (this instanceof HNavigationInputPreferred)
            {
                // Handle mouse enter/mouse clicked
                if (evt instanceof MouseEvent
                        && (evt.getID() == MouseEvent.MOUSE_ENTERED || evt.getID() == MouseEvent.MOUSE_PRESSED))
                {
                    requestFocus();
                }

                HFocusEvent e = toolkit.makeFocusEvent(evt);
                if (e != null) ((HNavigationInputPreferred) this).processHFocusEvent(e);
            }
            if (this instanceof HActionInputPreferred)
            {
                HActionEvent e = toolkit.makeActionEvent(evt);
                if (e != null) ((HActionInputPreferred) this).processHActionEvent(e);
            }
            if (this instanceof HAdjustmentInputPreferred)
            {
                HAdjustmentEvent e = toolkit.makeAdjustmentEvent(evt);
                if (e != null) ((HAdjustmentInputPreferred) this).processHAdjustmentEvent(e);
            }
            if (this instanceof HSelectionInputPreferred)
            {
                HItemEvent e = toolkit.makeItemEvent(evt);
                if (e != null) ((HSelectionInputPreferred) this).processHItemEvent(e);
            }
            if (this instanceof HTextValue)
            {
                HTextEvent e = toolkit.makeTextEvent(evt);
                if (e != null) ((HKeyboardInputPreferred) this).processHTextEvent(e);
            }

            // Process events that come through here (directly)...
            if ((evt instanceof HKeyEvent) && (this instanceof HKeyboardInputPreferred))
                ((HKeyboardInputPreferred) this).processHKeyEvent((HKeyEvent) evt);
            else if ((evt instanceof HFocusEvent) && (this instanceof HNavigationInputPreferred))
                ((HNavigationInputPreferred) this).processHFocusEvent((HFocusEvent) evt);
            else if ((evt instanceof HActionEvent) && (this instanceof HActionInputPreferred))
                ((HActionInputPreferred) this).processHActionEvent((HActionEvent) evt);
            else if ((evt instanceof HAdjustmentEvent) && (this instanceof HAdjustmentInputPreferred))
                ((HAdjustmentInputPreferred) this).processHAdjustmentEvent((HAdjustmentEvent) evt);

            else if ((evt instanceof HItemEvent) && (this instanceof HSelectionInputPreferred))
                ((HSelectionInputPreferred) this).processHItemEvent((HItemEvent) evt);
            else if ((evt instanceof HTextEvent) && (this instanceof HTextValue))
                ((HKeyboardInputPreferred) this).processHTextEvent((HTextEvent) evt);
            else
                super.processEvent(evt);
        }
    }

    /**
     * Checks that the given component is contained within this container, if
     * not an ArrayIndexOutOfBounds exception is thrown.
     * 
     * @throws ArrayIndexOutOfBoundsException
     *             if <code>Component c</code> is not container in this
     *             container
     */
    private void checkLineage(java.awt.Component c)
    {
        if (c.getParent() != this) throw new ArrayIndexOutOfBoundsException("Component not contained within");
    }

    /**
     * Return position of the component in conainer relative to the front where
     * front component is the last added to container.
     * 
     * @throws ArrayIndexOutOfBoundsException
     *             if <code>Component c</code> is not container in this
     *             container
     */
    private int getOffset(java.awt.Component c) throws ArrayIndexOutOfBoundsException
    {
        Component cs[] = getComponents();
        for (int i = 0; i < cs.length; ++i)
            if (cs[i] == c) return i;

        throw new ArrayIndexOutOfBoundsException("Component not contained within");
    }

    /**
     * Specifies whether this <code>HContainer</code> is grouped or not.
     */
    private boolean grouped = false;

    /** The <code>HMatte</code> for this container. */
    private HMatte containerMatte = null;

    /** A platform specific matte compositor for this container */
    MatteCompositor matteCompositor = null;

    /** Set to true once the matte compositor is set (potentially to null) */
    boolean matteCompositorSet = false;

    /** Is container double-buffered? */
    private static final boolean doubleBuffered = false;

    /** Is container opaque? */
    private static final boolean opaque = false;

    /**
     * The global toolkit implementation.
     */
    private static final HaviToolkit toolkit = HaviToolkit.getToolkit();

    /**
     * Whether to enable event translation or not. The HAVi spec does not
     * specify this so depending upon it is non-portable.
     */
    private static final boolean TRANS_EVENTS = false;
}
