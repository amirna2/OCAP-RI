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

import org.cablelabs.impl.havi.HaviToolkit;

import java.awt.event.MouseEvent;

import org.havi.ui.event.HActionEvent;
import org.havi.ui.event.HAdjustmentEvent;
import org.havi.ui.event.HFocusEvent;
import org.havi.ui.event.HItemEvent;
import org.havi.ui.event.HKeyEvent;
import org.havi.ui.event.HTextEvent;

/**
 * The <code>HComponent</code> class extends the java.awt.Component class by
 * implementing the {@link org.havi.ui.HMatteLayer} interface.
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
 * </table>
 * 
 * @author Alex Resh
 * @author Todd Earles (Matte Support)
 * @author Aaron Kamienski (1.0.1b, 1.1)
 * @version 1.1
 */

public abstract class HComponent extends java.awt.Component implements HMatteLayer, org.dvb.ui.TestOpacity
{
    /**
     * Creates an HComponent object. See the class description for details of
     * constructor parameters and default values.
     */
    public HComponent()
    {
        iniz();
    }

    /**
     * Creates an HComponent object. See the class description for details of
     * constructor parameters and default values.
     */
    public HComponent(int x, int y, int width, int height)
    {
        super.setBounds(x, y, width, height);
        iniz();
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
        if (m != null && !HaviToolkit.getToolkit().isMatteSupported(m))
            throw new HMatteException("Unknown HMatte type");

        // If the currently assigned matte is a matte effect, then unregister
        // this component with it.
        final HMatte current = componentMatte;
        if (current instanceof HFlatEffectMatte)
        {
            HFlatEffectMatte matte = (HFlatEffectMatte) current;
            if (matte.isAnimated()) throw new HMatteException();
            matte.unregisterComponent(this);
        }
        else if (current instanceof HImageEffectMatte)
        {
            HImageEffectMatte matte = (HImageEffectMatte) current;
            if (matte.isAnimated()) throw new HMatteException();
            matte.unregisterComponent(this);
        }

        // Assign the matte to this component
        componentMatte = m;

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
        return componentMatte;
    }

    /**
     * Returns true if all the drawing done during the update and paint methods
     * for this specific HComponent object is automatically double buffered.
     * 
     * @return <code>true</code> if all the drawing done during the update and
     *         paint methods for this specific HComponent object is
     *         automatically double buffered, or false if drawing is not double
     *         buffered. The default value for the double buffering setting is
     *         platform-specific.
     * 
     * @mhp 11.4.1.2 Shall not be used by inter-operable applications.
     */
    public boolean isDoubleBuffered()
    {
        throw new UnsupportedOperationException("Shall not be used by inter-operable apps (MHP 11.4.1.2)");
        // return doubleBuffered;
    }

    /**
     * Returns true if the entire HComponent area, as given by the
     * <code>java.awt.Component#getBounds</code> method, is fully opaque, i.e.
     * its paint method (or surrogate methods) guarantee that all pixels are
     * painted in an opaque <code>Color</code>.
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
     * Enables or disables this component, depending on the value of the
     * parameter b. HComponents which are disabled will still generate and
     * respond to {@link org.havi.ui.event.HFocusEvent} if they implement
     * {@link org.havi.ui.HNavigationInputPreferred}. They will not generate or
     * respond to {@link org.havi.ui.event.HActionEvent},
     * {@link org.havi.ui.event.HAdjustmentEvent},
     * {@link org.havi.ui.event.HItemEvent}, {@link org.havi.ui.event.HKeyEvent}
     * or {@link org.havi.ui.event.HTextEvent}. (This method should not invoke
     * the superclass method.) HComponents are enabled initially by default.
     * <p>
     * If a widget implementing {@link org.havi.ui.HKeyboardInputPreferred} is
     * disabled while in edit mode, it will automatically set edit mode to false
     * and generate an HTextEvent.TEXT_END_CHANGE. Calls to setEditMode() should
     * be ignored while being disabled.
     * <p>
     * If a widget implementing {@link org.havi.ui.HAdjustmentInputPreferred} is
     * disabled while in adjust mode, it will automatically set adjust mode to
     * false and generate an HAdjustmentEvent.ADJUST_END_CHANGE. Calls to
     * setAdjustMode() should be ignored while being disabled.
     * <p>
     * If a widget implementing {@link org.havi.ui.HSelectionInputPreferred} is
     * disabled while in selection mode, it will automatically set selection
     * mode to false and generate an HItemEvent.ITEM_END_CHANGE. Calls to
     * setSelectionMode() should be ignored while being disabled.
     * 
     * @param b
     *            If true, this HComponent is enabled; otherwise this HComponent
     *            is disabled.
     */
    public void setEnabled(boolean b)
    {
        /* Take care of special implementation cases for each interface. */
        if (!b)
        {
            if (this instanceof HKeyboardInputPreferred)
            {
                ((HKeyboardInputPreferred) this).setEditMode(false);
            }
            if (this instanceof HAdjustmentInputPreferred)
            {
                ((HAdjustmentInputPreferred) this).setAdjustMode(false);
            }
            if (this instanceof HSelectionInputPreferred)
            {
                ((HSelectionInputPreferred) this).setSelectionMode(false);
            }
        }

        /* Enable/disable this component */
        enabled = b;

        return;
    }

    /**
     * Determines whether this HComponent is enabled. An HComponent may be
     * enabled or disabled by calling its setEnabled method.
     * 
     * @return <code>true</code> if the HComponent is enabled;
     *         <code>false</code> otherwise.
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * The implementation of the method <code>HComponent.processEvent()</code>
     * shall ensure that key events which are translated to HAVi events shall
     * not be reported to <code>processKeyEvent()</code> or reported to
     * <code>KeyListeners</code>. Key events which are not translated to HAVi
     * events shall be reported to <code>processKeyEvent()</code> and
     * <code>KeyListeners</code> as defined in the Java specification.
     * <p>
     * NOTE: If applications override <code>processEvent</code> they may
     * terminally disturb these processes. Applications should not do this
     * without extreme care, as the results may be very implementation
     * dependent.
     * 
     * @param evt
     *            the java.awt.AWTEvent to handle.
     */
    protected void processEvent(java.awt.AWTEvent evt)
    {
        boolean enabled = isEnabled();
        boolean noKeyEvent = false;

        /*
         * Implementation note: At runtime many "instanceof" checks are done on
         * the "this" object. These could probably be eliminated by using a
         * Strategy object of some sort for each class/subclass. Alternatively,
         * the instanceof checks could be computed at construction time and
         * stored in private boolean variables.
         * 
         * I have not performed these "optimizations" because:
         * 
         * 1. I have not determined it to be a problem (i.e., with some sort of
         * profiling). 2. The savings are unknown. The runtime savings may be
         * outweighed by the data costs. Or, "instanceof" just isn't that
         * expensive when compared to a method call.
         */

        /*
         * Implementation note:
         * 
         * This method is intended to do one of two things:
         * 
         * 1. Create HAVi events from AWT events (with the decision left up to a
         * Toolkit implementation) and dispatch those events. 2. Dispatch HAVi
         * events directly.
         * 
         * It is possible for a JAVA system to create HAVi events directly, thus
         * eliminating the need for #1. In which case the "instanceof" checks
         * and calls to the Toolkit class to potentially create HAVi events are
         * unnecessary.
         * 
         * It might make sense then to move all of that code "up" into the
         * Toolkit class.
         */

        /*
         * From the description for setEnabled: HComponents which are disabled
         * will still generate and respond to HFocusEvents if they implement
         * HNavigationInputPreferred.
         * 
         * They will NOT generate or respond to: HActionEvent, HAdjustmentEvent,
         * HItemEvent, HKeyEvent HTextEvent
         */

        // Create HAVi events from AWT events..
        if (this instanceof HNavigationInputPreferred)
        {
            // Handle mouse enter/mouse clicked
            if (evt instanceof MouseEvent
                    && (evt.getID() == MouseEvent.MOUSE_ENTERED || evt.getID() == MouseEvent.MOUSE_PRESSED))
            {
                requestFocus();
            }

            HFocusEvent e = toolkit.makeFocusEvent(evt);
            if (e != null)
            {
                ((HNavigationInputPreferred) this).processHFocusEvent(e);

                // According to HNavigationInputPreferred,
                // no HKeyEvent should be generated if it corresponds to
                // a FOCUS_TRANSFER event.
                // Really, the best way to handle this is for focus events
                // NOT to be generated when in editMode and key events
                // NOT to be generated when NOT in editMode.
                noKeyEvent = e.getID() == HFocusEvent.FOCUS_TRANSFER;
            }
        }
        if (enabled)
        {
            if (!noKeyEvent && this instanceof HKeyboardInputPreferred)
            {
                HKeyEvent e = toolkit.makeKeyEvent(evt);
                if (e != null) ((HKeyboardInputPreferred) this).processHKeyEvent(e);
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
        }

        // Process events that come through here (directly)...
        if (enabled && (evt instanceof HKeyEvent) && (this instanceof HKeyboardInputPreferred))
            ((HKeyboardInputPreferred) this).processHKeyEvent((HKeyEvent) evt);
        else if ((evt instanceof HFocusEvent) && (this instanceof HNavigationInputPreferred))
            ((HNavigationInputPreferred) this).processHFocusEvent((HFocusEvent) evt);

        else if (enabled)
        {
            if ((evt instanceof HActionEvent) && (this instanceof HActionInputPreferred))
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
        else
            super.processEvent(evt);
    }

    /**
     * Initialization common to all constructors.
     */
    private void iniz()
    {
        enableEvents(toolkit.getEventMask(this));
    }

    /**
     * This component's HMatte.
     */
    private HMatte componentMatte = null;

    /**
     * Is the component double-buffered?
     */
    // private static final boolean doubleBuffered = false;

    /**
     * Is the component opaque?
     */
    private static final boolean opaque = false;

    /**
     * Is the component enabled?
     */
    private boolean enabled = true;

    /**
     * The global toolkit implementation.
     */
    static final HaviToolkit toolkit = HaviToolkit.getToolkit();

}
