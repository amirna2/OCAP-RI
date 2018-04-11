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

import org.havi.ui.event.HAdjustmentEvent;
import org.havi.ui.event.HAdjustmentListener;
import org.havi.ui.event.HFocusListener;

/**
 * The {@link org.havi.ui.HRangeValue HRangeValue} is a user interface component
 * used to display a value within a fixed range (as org.havi.ui.HStaticRange
 * HStaticRange}) which enables a user to navigate to and alter the value of it,
 * i.e. it can have the input focus and it can be adjusted. By default it uses
 * the {@link org.havi.ui.HRangeLook HRangeLook} to render itself.
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
 * 
 * <tr>
 * <td>orientation</td>
 * <td>The &quot;orientation&quot; of the range object.</td>
 * <td>{@link HStaticRange#ORIENT_LEFT_TO_RIGHT ORIENT_LEFT_TO_RIGHT}</td>
 * <td>{@link HStaticRange#setOrientation setOrientation}</td>
 * <td>{@link HStaticRange#getOrientation getOrientation}</td>
 * </tr>
 * <tr>
 * <td>minimum</td>
 * <td>The minimum value that can be returned by this range object.</td>
 * <td>0</td>
 * <td>{@link HStaticRange#setRange setRange}</td>
 * <td>{@link HStaticRange#getMinValue getMinValue}</td>
 * </tr>
 * <tr>
 * <td>maximum</td>
 * <td>The maximum value that can be returned by this range object.</td>
 * <td>100</td>
 * <td>{@link HStaticRange#setRange setRange}</td>
 * <td>{@link HStaticRange#getMaxValue getMaxValue}</td>
 * </tr>
 * <tr>
 * <td>value</td>
 * <td>The current value returned by this range object.</td>
 * <td>0</td>
 * <td>{@link HStaticRange#setValue setValue}</td>
 * <td>{@link HStaticRange#getValue getValue}</td>
 * </tr>
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
 * <td>The text layout manager responsible for text formatting.</td>
 * <td>An {@link org.havi.ui.HDefaultTextLayoutManager} object.</td>
 * <td> {@link org.havi.ui.HVisible#setTextLayoutManager}</td>
 * <td> {@link org.havi.ui.HVisible#getTextLayoutManager}</td>
 * </tr>
 * 
 * <tr>
 * <td>The background painting mode</td>
 * <td>{@link org.havi.ui.HVisible#NO_BACKGROUND_FILL}</td>
 * 
 * <td>{@link org.havi.ui.HVisible#setBackgroundMode}</td>
 * <td>{@link org.havi.ui.HVisible#getBackgroundMode}</td>
 * </tr>
 * 
 * <tr>
 * <td>The default preferred size</td>
 * <td>not set (i.e. NO_DEFAULT_SIZE) unless specified by <code>width</code> and
 * <code>height</code> parameters</td>
 * <td>{@link org.havi.ui.HVisible#setDefaultSize}</td>
 * <td>{@link org.havi.ui.HVisible#getDefaultSize}</td>
 * </tr>
 * 
 * <tr>
 * <td>The horizontal content alignment</td>
 * <td>{@link org.havi.ui.HVisible#HALIGN_CENTER}</td>
 * <td>{@link org.havi.ui.HVisible#setHorizontalAlignment}</td>
 * <td>{@link org.havi.ui.HVisible#getHorizontalAlignment}</td>
 * </tr>
 * 
 * <tr>
 * <td>The vertical content alignment</td>
 * <td>{@link org.havi.ui.HVisible#VALIGN_CENTER}</td>
 * <td>{@link org.havi.ui.HVisible#setVerticalAlignment}</td>
 * <td>{@link org.havi.ui.HVisible#getVerticalAlignment}</td>
 * </tr>
 * 
 * <tr>
 * <td>The content scaling mode</td>
 * <td>{@link org.havi.ui.HVisible#RESIZE_NONE}</td>
 * <td>{@link org.havi.ui.HVisible#setResizeMode}</td>
 * <td>{@link org.havi.ui.HVisible#getResizeMode}</td>
 * </tr>
 * 
 * <tr>
 * <td>The border mode</td>
 * <td><code>true</code></td>
 * <td>{@link org.havi.ui.HVisible#setBordersEnabled}</td>
 * <td>{@link org.havi.ui.HVisible#getBordersEnabled}</td>
 * </tr>
 * 
 * 
 * 
 * 
 * <tr>
 * <td>The default &quot;look&quot; for this class.</td>
 * <td>A platform specific {@link org.havi.ui.HRangeLook HRangeLook}</td>
 * <td>{@link org.havi.ui.HRangeValue#setDefaultLook HRangeValue.setDefaultLook}
 * </td>
 * <td>{@link org.havi.ui.HRangeValue#getDefaultLook HRangeValue.getDefaultLook}
 * </td>
 * </tr>
 * 
 * <tr>
 * <td>The &quot;look&quot; for this object.</td>
 * <td>The {@link org.havi.ui.HRangeLook HRangeLook} returned from
 * HRangeValue.getDefaultLook when this object was created.</td>
 * <td>{@link org.havi.ui.HRangeValue#setLook HRangeValue.setLook}</td>
 * <td>{@link org.havi.ui.HRangeValue#getLook HRangeValue.getLook}</td>
 * </tr>
 * <tr>
 * <td>The offsets for the &quot;thumb&quot; of this range control</td>
 * <td>min = 0, max = 0</td>
 * <td>{@link HStaticRange#setThumbOffsets setThumbOffsets}</td>
 * <td>{@link HStaticRange#getThumbMinOffset getThumbMinOffset} /
 * {@link HStaticRange#getThumbMaxOffset getThumbMaxOffset}</td>
 * </tr>
 * 
 * <tr>
 * <td>The behavior of this range object with respect to its &quot;thumb&quot;
 * values</td>
 * <td>{@link HStaticRange#SLIDER_BEHAVIOR SLIDER_BEHAVIOR}</td>
 * <td>{@link HStaticRange#setBehavior setBehavior}</td>
 * <td>{@link HStaticRange#getBehavior getBehavior}</td>
 * </tr>
 * 
 * <tr>
 * <td>The block increment for this object.</td>
 * <td>1 unit</td>
 * <td>{@link org.havi.ui.HRangeValue#setBlockIncrement setBlockIncrement}</td>
 * <td>{@link org.havi.ui.HRangeValue#getBlockIncrement getBlockIncrement}</td>
 * </tr>
 * 
 * <tr>
 * <td>The gain focus sound.</td>
 * <td>null</td>
 * <td>{@link org.havi.ui.HRangeValue#setGainFocusSound setGainFocusSound}</td>
 * <td>{@link org.havi.ui.HRangeValue#getGainFocusSound getGainFocusSound}</td>
 * </tr>
 * <tr>
 * <td>The lose focus sound.</td>
 * <td>null</td>
 * <td>{@link org.havi.ui.HRangeValue#setLoseFocusSound setLoseFocusSound}</td>
 * <td>{@link org.havi.ui.HRangeValue#getLoseFocusSound getLoseFocusSound}</td>
 * </tr>
 * </table>
 * 
 * @see HStaticRange
 * @see HRange
 * @see HNavigable
 * @see HAdjustmentValue
 * @author Aaron Kamienski
 * @version 1.1
 */

public class HRangeValue extends HRange implements HAdjustmentValue
{
    /**
     * Creates an {@link org.havi.ui.HRangeValue HRangeValue} object. See the
     * class description for details of constructor parameters and default
     * values.
     */
    public HRangeValue()
    {
        super();
        iniz();
    }

    /**
     * Creates an {@link org.havi.ui.HRangeValue HRangeValue} object. See the
     * class description for details of constructor parameters and default
     * values.
     */
    public HRangeValue(int orientation, int minimum, int maximum, int value, int x, int y, int width, int height)
    {
        super(orientation, minimum, maximum, value, x, y, width, height);
        iniz();
    }

    /**
     * Creates an {@link org.havi.ui.HRangeValue HRangeValue} object. See the
     * class description for details of constructor parameters and default
     * values.
     */
    public HRangeValue(int orientation, int minimum, int maximum, int value)
    {
        super(orientation, minimum, maximum, value);
        iniz();
    }

    /**
     * Initialization common to all constructors.
     */
    private void iniz()
    {
        try
        {
            setLook(getDefaultLook());
        }
        catch (HInvalidLookException ignored)
        {
        }
    }

    /**
     * Sets the default {@link org.havi.ui.HLook HLook} for further
     * {@link org.havi.ui.HRangeValue HRangeValue} Components.
     * 
     * @param look
     *            The {@link org.havi.ui.HLook HLook} that will be used by
     *            default when creating a new {@link org.havi.ui.HRangeValue
     *            HRangeValue} component. Note that this parameter may be null,
     *            in which case newly created components shall not draw
     *            themselves until a non-null look is set using the
     *            {@link org.havi.ui.HStaticRange#setLook setLook} method.
     */
    public static void setDefaultLook(HRangeLook look)
    {
        setDefaultLookImpl(PROPERTY_LOOK, look);
    }

    /**
     * Returns the currently set default look for
     * {@link org.havi.ui.HRangeValue HRangeValue} components.
     * 
     * @return The look that is used by default when creating a new
     *         {@link org.havi.ui.HRangeValue HRangeValue} component.
     */
    public static HRangeLook getDefaultLook()
    {
        return (HRangeLook) getDefaultLookImpl(PROPERTY_LOOK, DEFAULT_LOOK);
    }

    /**
     * Defines the navigation path from the current
     * {@link org.havi.ui.HNavigable HNavigable} to another
     * {@link org.havi.ui.HNavigable HNavigable} when a particular key is
     * pressed.
     * <p>
     * Note that {@link org.havi.ui.HNavigable#setFocusTraversal
     * setFocusTraversal} is equivalent to multiple calls to
     * {@link org.havi.ui.HNavigable#setMove setMove}, where the key codes
     * <code>VK_UP</code>, <code>VK_DOWN</code>, <code>VK_LEFT</code>,
     * <code>VK_RIGHT</code> are used.
     * 
     * @param keyCode
     *            The key code of the pressed key. Any numerical keycode is
     *            allowed, but the platform may not be able to generate all
     *            keycodes. Application authors should only use keys for which
     *            <code>HRcCapabilities.isSupported()</code> or
     *            <code>HKeyCapabilities.isSupported()</code> returns true.
     * @param target
     *            The target {@link org.havi.ui.HNavigable HNavigable} object
     *            that should be navigated to. If a target is to be removed from
     *            a particular navigation path, then <code>null</code> shall be
     *            specified.
     */
    public void setMove(int keyCode, HNavigable target)
    {
        super.setMove(keyCode, target);
    }

    /**
     * Provides the {@link org.havi.ui.HNavigable HNavigable} object that is
     * navigated to when a particular key is pressed.
     * 
     * @param keyCode
     *            The key code of the pressed key.
     * @return Returns the {@link org.havi.ui.HNavigable HNavigable} object or
     *         <code>null</code> if no {@link org.havi.ui.HNavigable HNavigable}
     *         is associated with the keyCode.
     */
    public HNavigable getMove(int keyCode)
    {
        return super.getMove(keyCode);
    }

    /**
     * Set the focus control for an {@link org.havi.ui.HNavigable HNavigable}
     * component. Note {@link org.havi.ui.HNavigable#setFocusTraversal
     * setFocusTraversal} is a convenience function for application programmers
     * where a standard up, down, left and right focus traversal between
     * components is required.
     * <p>
     * Note {@link org.havi.ui.HNavigable#setFocusTraversal setFocusTraversal}
     * is equivalent to multiple calls to {@link org.havi.ui.HNavigable#setMove
     * setMove}, where the key codes VK_UP, VK_DOWN, VK_LEFT, VK_RIGHT are used.
     * <p>
     * Note that this API does not prevent the creation of &quot;isolated&quot;
     * {@link org.havi.ui.HNavigable HNavigable} components --- authors should
     * endeavor to avoid confusing the user.
     * 
     * @param up
     *            The {@link org.havi.ui.HNavigable HNavigable} component to
     *            move to, when the user generates a VK_UP KeyEvent. If there is
     *            no {@link org.havi.ui.HNavigable HNavigable} component to move
     *            &quot;up&quot; to, then null shall be specified.
     * @param down
     *            The {@link org.havi.ui.HNavigable HNavigable} component to
     *            move to, when the user generates a VK_DOWN KeyEvent. If there
     *            is no {@link org.havi.ui.HNavigable HNavigable} component to
     *            move &quot;down&quot; to, then null shall be specified.
     * @param left
     *            The {@link org.havi.ui.HNavigable HNavigable} component to
     *            move to, when the user generates a VK_LEFT KeyEvent. If there
     *            is no {@link org.havi.ui.HNavigable HNavigable} component to
     *            move &quot;left&quot; to, then null shall be specified.
     * @param right
     *            The {@link org.havi.ui.HNavigable HNavigable} component to
     *            move to, when the user generates a VK_RIGHT KeyEvent. If there
     *            is no {@link org.havi.ui.HNavigable HNavigable} component to
     *            move &quot;right&quot; to, then null shall be specified.
     */
    public void setFocusTraversal(HNavigable up, HNavigable down, HNavigable left, HNavigable right)
    {
        super.setFocusTraversal(up, down, left, right);
    }

    /**
     * Indicates if this component has focus.
     * 
     * @return <code>true</code> if the component has focus, otherwise returns
     *         <code>false</code>.
     */
    public boolean isSelected()
    {
        return super.isSelected();
    }

    /**
     * Associate a sound with gaining focus, i.e. when the
     * {@link org.havi.ui.HNavigable HNavigable} receives a
     * {@link org.havi.ui.event.HFocusEvent HFocusEvent} event of type
     * <code>FOCUS_GAINED</code>. This sound will start to be played when an
     * object implementing this interface gains focus. It is not guaranteed to
     * be played to completion. If the object implementing this interface loses
     * focus before the audio completes playing, the audio will be truncated.
     * Applications wishing to ensure the audio is always played to completion
     * must implement special logic to slow down the focus transitions.
     * <p>
     * By default, an {@link org.havi.ui.HNavigable HNavigable} object does not
     * have any gain focus sound associated with it.
     * <p>
     * Note that the ordering of playing sounds is dependent on the order of the
     * focus lost, gained events.
     * 
     * @param sound
     *            the sound to be played, when the component gains focus. If
     *            sound content is already set, the original content is
     *            replaced. To remove the sound specify a null
     *            {@link org.havi.ui.HSound HSound}.
     */
    public void setGainFocusSound(HSound sound)
    {
        super.setGainFocusSound(sound);
    }

    /**
     * Associate a sound with losing focus, i.e. when the
     * {@link org.havi.ui.HNavigable HNavigable} receives a
     * {@link org.havi.ui.event.HFocusEvent HFocusEvent} event of type
     * FOCUS_LOST. This sound will start to be played when an object
     * implementing this interface loses focus. It is not guaranteed to be
     * played to completion. It is implementation dependent whether and when
     * this sound will be truncated by any gain focus sound played by the next
     * object to gain focus.
     * <p>
     * By default, an {@link org.havi.ui.HNavigable HNavigable} object does not
     * have any lose focus sound associated with it.
     * <p>
     * Note that the ordering of playing sounds is dependent on the order of the
     * focus lost, gained events.
     * 
     * @param sound
     *            the sound to be played, when the component loses focus. If
     *            sound content is already set, the original content is
     *            replaced. To remove the sound specify a null
     *            {@link org.havi.ui.HSound HSound}.
     */
    public void setLoseFocusSound(HSound sound)
    {
        super.setLoseFocusSound(sound);
    }

    /**
     * Get the sound associated with the gain focus event.
     * 
     * @return The sound played when the component gains focus. If no sound is
     *         associated with gaining focus, then null shall be returned.
     */
    public HSound getGainFocusSound()
    {
        return super.getGainFocusSound();
    }

    /**
     * Get the sound associated with the lost focus event.
     * 
     * @return The sound played when the component loses focus. If no sound is
     *         associated with losing focus, then null shall be returned.
     */
    public HSound getLoseFocusSound()
    {
        return super.getLoseFocusSound();
    }

    /**
     * Adds the specified {@link org.havi.ui.event.HFocusListener
     * HFocusListener} to receive {@link org.havi.ui.event.HFocusEvent
     * HFocusEvent} events sent from this {@link org.havi.ui.HNavigable
     * HNavigable}: If the listener has already been added further calls will
     * add further references to the listener, which will then receive multiple
     * copies of a single event.
     * 
     * @param l
     *            the HFocusListener to add
     */
    public void addHFocusListener(HFocusListener l)
    {
        super.addHFocusListener(l);
    }

    /**
     * Removes the specified {@link org.havi.ui.event.HFocusListener
     * HFocusListener} so that it no longer receives
     * {@link org.havi.ui.event.HFocusEvent HFocusEvent} events from this
     * {@link org.havi.ui.HNavigable HNavigable}. If the specified listener is
     * not registered, the method has no effect. If multiple references to a
     * single listener have been registered it should be noted that this method
     * will only remove one reference per call.
     * 
     * @param l
     *            the HFocusListener to remove
     */
    public void removeHFocusListener(HFocusListener l)
    {
        super.removeHFocusListener(l);
    }

    /**
     * Retrieve the set of key codes which this component maps to navigation
     * targets.
     * 
     * @return an array of key codes, or <code>null</code> if no navigation
     *         targets are set on this component.
     */
    public int[] getNavigationKeys()
    {
        return super.getNavigationKeys();
    }

    /**
     * Process an {@link org.havi.ui.event.HFocusEvent HFocusEvent} sent to this
     * {@link org.havi.ui.HRangeValue HRangeValue}.
     * 
     * @param evt
     *            the {@link org.havi.ui.event.HFocusEvent HFocusEvent} to
     *            process.
     */

    public void processHFocusEvent(org.havi.ui.event.HFocusEvent evt)
    {
        super.processHFocusEvent(evt);
    }

    /**
     * Set the unit increment for this HRangeValue.
     * 
     * @param increment
     *            the amount by which the value of the HRangeValue should change
     *            when an {@link org.havi.ui.event.HAdjustmentEvent#ADJUST_LESS}
     *            or {@link org.havi.ui.event.HAdjustmentEvent#ADJUST_MORE}
     *            event is received. Values of <code>increment</code> less than
     *            one shall be treated as a value of one.
     */
    public void setUnitIncrement(int increment)
    {
        unit = (increment < 1) ? 1 : increment;
    }

    /**
     * Get the unit increment for this HRangeValue. <code>1</code> shall be
     * returned if this method is called before its corresponding set method.
     * 
     * @return the increment value for this HRangeValue.
     */
    public int getUnitIncrement()
    {
        return unit;
    }

    /**
     * Set the block increment for this HRangeValue.
     * 
     * @param increment
     *            the amount by which the value of the HRangeValue should change
     *            when an
     *            {@link org.havi.ui.event.HAdjustmentEvent#ADJUST_PAGE_LESS} or
     *            {@link org.havi.ui.event.HAdjustmentEvent#ADJUST_PAGE_MORE}
     *            event is received. Values of <code>increment</code> less than
     *            one shall be treated as a value of one.
     */
    public void setBlockIncrement(int increment)
    {
        block = (increment < 1) ? 1 : increment;
    }

    /**
     * Get the block increment for this HRangeValue. <code>1</code> shall be
     * returned if this method is called before its corresponding set method.
     * 
     * @return the block increment value for this HRangeValue.
     */
    public int getBlockIncrement()
    {
        return block;
    }

    /**
     * Adds the specified HAdjustmentListener to receive
     * <code>HAdjustmentEvents</code> sent from this object. If the listener has
     * already been added further calls will add further references to the
     * listener, which will then receive multiple copies of a single event.
     * 
     * @param l
     *            the HAdjustmentListener to be notified.
     */
    public void addAdjustmentListener(org.havi.ui.event.HAdjustmentListener l)
    {
        listeners = HEventMulticaster.add(listeners, l);
    }

    /**
     * Removes the specified HAdjustmentListener so that it no longer receives
     * <code>HAdjustmentEvents</code> from this object. If the specified
     * listener is not registered, the method has no effect. If multiple
     * references to a single listener have been registered it should be noted
     * that this method will only remove one reference per call.
     * 
     * @param l
     *            the HAdjustmentListener to be removed from notification.
     */
    public void removeAdjustmentListener(org.havi.ui.event.HAdjustmentListener l)
    {
        listeners = HEventMulticaster.remove(listeners, l);
    }

    /**
     * Associate a sound to be played when the value is modified. The sound is
     * played irrespective of whether an <code>HAdjustmentEvent</code> is sent
     * to one or more listeners.
     * 
     * @param sound
     *            the sound to be played, when the value is modified. If sound
     *            content is already set, the original content is replaced. To
     *            remove the sound specify a null {@link org.havi.ui.HSound}.
     */
    public void setAdjustmentSound(HSound sound)
    {
        adjustSound = sound;
    }

    /**
     * Get the sound to be played when the value changes. <code>null</code>
     * shall be returned if this method is called before its corresponding set
     * method.
     * 
     * @return The sound played when the value changes
     */
    public HSound getAdjustmentSound()
    {
        return adjustSound;
    }

    /**
     * Get the adjustment mode for this HRangeValue. If the returned value is
     * <code>true</code> the component is in adjustment mode, and its value may
     * be changed on receipt of
     * {@link org.havi.ui.event.HAdjustmentEvent#ADJUST_LESS} and
     * {@link org.havi.ui.event.HAdjustmentEvent#ADJUST_MORE} events.
     * <p>
     * The component is switched into and out of adjustment mode on receiving
     * {@link org.havi.ui.event.HAdjustmentEvent#ADJUST_START_CHANGE}
     * {@link org.havi.ui.event.HAdjustmentEvent#ADJUST_END_CHANGE} events. Note
     * that these events are ignored, if the component is disabled.
     * 
     * @return true if this component is in adjustment mode, false otherwise.
     * @see HComponent#setEnabled
     */
    public boolean getAdjustMode()
    {
        return adjustMode;
    }

    /**
     * Set the adjustment mode for this HRangeValue.
     * <p>
     * This method is provided for the convenience of component implementors.
     * Interoperable applications shall not call this method. It cannot be made
     * protected because interfaces cannot have protected methods.
     * <p>
     * Calls to this method shall be ignored, if the component is disabled.
     * 
     * @param edit
     *            true to switch this component into adjustment mode, false
     *            otherwise.
     * @see HAdjustmentInputPreferred#getAdjustMode
     * @see HComponent#setEnabled
     */
    public void setAdjustMode(boolean adjust)
    {
        adjustMode = adjust;
    }

    /**
     * Process an <code>HAdjustmentEvent</code> sent to this
     * <code>HAdjustmentInputPreferred</code>. Widgets implementing this
     * interface shall ignore <code>HAdjustmentEvents</code>, while the
     * component is disabled.
     * 
     * @param evt
     *            the <code>HAdjustmentEvent</code> to process.
     * @see HComponent#setEnabled
     */
    public void processHAdjustmentEvent(org.havi.ui.event.HAdjustmentEvent evt)
    {
        boolean editChanged = false;
        boolean changed = false;

        int id;
        switch (id = evt.getID())
        {
            case HAdjustmentEvent.ADJUST_START_CHANGE:
                changed = editChanged = !getAdjustMode();
                setAdjustMode(true);
                break;
            case HAdjustmentEvent.ADJUST_END_CHANGE:
                changed = editChanged = getAdjustMode();
                setAdjustMode(false);
                break;
            default:
                // All other events only occur when in adjustment mode
                if (getAdjustMode())
                {
                    int oldValue = getValue();
                    int unit = getUnitIncrement();
                    int block = getBlockIncrement();

                    switch (id)
                    {
                        case HAdjustmentEvent.ADJUST_LESS:
                            setValue(oldValue - unit);
                            break;
                        case HAdjustmentEvent.ADJUST_MORE:
                            setValue(oldValue + unit);
                            break;
                        case HAdjustmentEvent.ADJUST_PAGE_LESS:
                            setValue(oldValue - (block * unit));
                            break;
                        case HAdjustmentEvent.ADJUST_PAGE_MORE:
                            setValue(oldValue + (block * unit));
                            break;
                    }
                    changed = getValue() != oldValue;
                }
        }

        // Always play sound (regardless of ACTUAL change)
        HSound sound;
        if ((sound = getAdjustmentSound()) != null) sound.play();

        // Notify the look of the change
        if (editChanged) notifyLook(new HChangeData(EDIT_MODE_CHANGE, new Boolean(!getAdjustMode())));

        // Only notify listeners of ACTUAL changes
        if (listeners != null && changed) listeners.valueChanged(evt);
    }

    /**
     * The set of HAdjustmentListeners.
     */
    private HAdjustmentListener listeners;

    /**
     * Sound to be played when the value of this component changes.
     */
    private HSound adjustSound;

    /**
     * Minimum increment by which the value of the HRangeValue should change.
     */
    private int unit = 1;

    /**
     * Minimum increment of units by which the value should change on page
     * up/down.
     */
    private int block = 1;

    /**
     * If true, then the component is currently in adjustment (edit) mode.
     */
    private boolean adjustMode;

    /**
     * The property which specifies the platform-specific default look for this
     * class. Can be retreived with HaviToolkit.getProperty().
     */
    private static final String PROPERTY_LOOK = "org.havi.ui.HRangeValue.defaultLook";
}
