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

/**
 * The {@link org.havi.ui.HStaticRange HStaticRange} is a user interface
 * component used to display a static value which is within a fixed range, but
 * does <i>not</i> permit the user to navigate (focus) upon it. By default it
 * uses the {@link org.havi.ui.HRangeLook HRangeLook} to render itself.
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
 * 
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
 * <td>{@link org.havi.ui.HStaticRange#setDefaultLook
 * HStaticRange.setDefaultLook}</td>
 * 
 * <td>{@link org.havi.ui.HStaticRange#getDefaultLook
 * HStaticRange.getDefaultLook}</td>
 * 
 * </tr>
 * 
 * <tr>
 * <td>The &quot;look&quot; for this object.</td>
 * <td>The {@link org.havi.ui.HRangeLook HRangeLook} returned from
 * HStaticRange.getDefaultLook when this object was created.</td>
 * <td>{@link org.havi.ui.HStaticRange#setLook HStaticRange.setLook}</td>
 * <td>{@link org.havi.ui.HStaticRange#getLook HStaticRange.getLook}</td>
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
 * </table>
 */

public class HStaticRange extends HVisible implements HNoInputPreferred, HOrientable
{
    /**
     * The {@link org.havi.ui.HStaticRange HStaticRange} shall behave as a
     * slider, i.e. the allowable values that may be set / returned for the
     * {@link org.havi.ui.HStaticRange HStaticRange} shall not be affected by
     * the &quot;thumb&quot; offsets, and hence its value shall be able to vary
     * between <code>[minimum, maximum]</code>.
     */
    public final static int SLIDER_BEHAVIOR = 0;

    /**
     * The {@link org.havi.ui.HStaticRange HStaticRange} shall behave as a
     * scrollbar, i.e. the allowable values that may be set / returned for the
     * {@link org.havi.ui.HStaticRange HStaticRange} shall be affected by the
     * &quot;thumb&quot; offsets, and hence its value shall be able to vary
     * between <code>[minimum + minThumbOffset,  maximum - maxThumbOffset]</code>
     * .
     */
    public final static int SCROLLBAR_BEHAVIOR = 1;

    /**
     * Creates an {@link org.havi.ui.HStaticRange HStaticRange} object. See the
     * class description for details of constructor parameters and default
     * values.
     */
    public HStaticRange()
    {
        this(ORIENT_LEFT_TO_RIGHT, 0, 100, 0);
    }

    /**
     * Creates an {@link org.havi.ui.HStaticRange HStaticRange} object. See the
     * class description for details of constructor parameters and default
     * values.
     */
    public HStaticRange(int orientation, int minimum, int maximum, int value, int x, int y, int width, int height)
    {
        super(getDefaultLook(), x, y, width, height);
        iniz(orientation, minimum, maximum, value);
    }

    /**
     * Creates an {@link org.havi.ui.HStaticRange HStaticRange} object. See the
     * class description for details of constructor parameters and default
     * values.
     */
    public HStaticRange(int orientation, int minimum, int maximum, int value)
    {
        super(getDefaultLook());
        iniz(orientation, minimum, maximum, value);
    }

    /**
     * Common to all constructors.
     */
    private void iniz(int orientation, int minimum, int maximum, int value)
    {
        setOrientation(orientation);
        setRange(minimum, maximum);
        setValue(value);
        behavior = SLIDER_BEHAVIOR;
    }

    /**
     * Sets the {@link org.havi.ui.HLook HLook} for this component.
     * 
     * @param hlook
     *            The {@link org.havi.ui.HLook HLook} that is to be used for
     *            this component. Note that this parameter may be null, in which
     *            case the component will not draw itself until a look is set.
     * @exception HInvalidLookException
     *                If the Look is not an {@link org.havi.ui.HRangeLook
     *                HRangeLook}.
     */
    public void setLook(HLook hlook) throws HInvalidLookException
    {
        if ((hlook != null) && !(hlook instanceof HRangeLook)) throw new HInvalidLookException();
        super.setLook(hlook);
    }

    /**
     * Sets the default {@link org.havi.ui.HLook HLook} for further
     * {@link org.havi.ui.HStaticRange HStaticRange} Components.
     * 
     * @param look
     *            The {@link org.havi.ui.HLook HLook} that will be used by
     *            default when creating a new {@link org.havi.ui.HStaticRange
     *            HStaticRange} component. Note that this parameter may be null,
     *            in which case newly created components shall not draw
     *            themselves until a non-null look is set using the
     *            {@link org.havi.ui.HStaticRange#setLook setLook} method.
     */
    public static void setDefaultLook(HRangeLook look)
    {
        setDefaultLookImpl(PROPERTY_LOOK, look);
    }

    /**
     * Returns the currently set default {@link org.havi.ui.HLook HLook} for
     * {@link org.havi.ui.HStaticRange HStaticRange} components.
     * 
     * @return The {@link org.havi.ui.HLook HLook} that is used by default when
     *         creating a new {@link org.havi.ui.HStaticRange HStaticRange}
     *         component.
     */
    public static HRangeLook getDefaultLook()
    {
        return (HRangeLook) getDefaultLookImpl(PROPERTY_LOOK, DEFAULT_LOOK);
    }

    /**
     * Retrieve the orientation of the {@link org.havi.ui.HStaticRange
     * HStaticRange}. The orientation controls how the associated
     * <code>HLook</code> lays out the component.
     * 
     * @return one of {@link org.havi.ui.HOrientable#ORIENT_LEFT_TO_RIGHT
     *         ORIENT_LEFT_TO_RIGHT},
     *         {@link org.havi.ui.HOrientable#ORIENT_RIGHT_TO_LEFT
     *         ORIENT_RIGHT_TO_LEFT},
     *         {@link org.havi.ui.HOrientable#ORIENT_TOP_TO_BOTTOM
     *         ORIENT_TOP_TO_BOTTOM}, or
     *         {@link org.havi.ui.HOrientable#ORIENT_BOTTOM_TO_TOP
     *         ORIENT_BOTTOM_TO_TOP}.
     */
    public int getOrientation()
    {
        return orientation;
    }

    /**
     * Set the orientation of the {@link org.havi.ui.HStaticRange HStaticRange}.
     * The orientation controls the layout of the component.
     * 
     * @param orient
     *            one of {@link org.havi.ui.HOrientable#ORIENT_LEFT_TO_RIGHT
     *            ORIENT_LEFT_TO_RIGHT},
     *            {@link org.havi.ui.HOrientable#ORIENT_RIGHT_TO_LEFT
     *            ORIENT_RIGHT_TO_LEFT},
     *            {@link org.havi.ui.HOrientable#ORIENT_TOP_TO_BOTTOM
     *            ORIENT_TOP_TO_BOTTOM}, or
     *            {@link org.havi.ui.HOrientable#ORIENT_BOTTOM_TO_TOP
     *            ORIENT_BOTTOM_TO_TOP}.
     */
    public void setOrientation(int orient)
    {
        switch (orient)
        {
            case ORIENT_RIGHT_TO_LEFT:
            case ORIENT_LEFT_TO_RIGHT:
            case ORIENT_TOP_TO_BOTTOM:
            case ORIENT_BOTTOM_TO_TOP:
                int old = orientation;
                orientation = orient;

                // Notify look of change (if there is one)
                if (orient != old) notifyLook(new HChangeData(ORIENTATION_CHANGE, new Integer(old)));
                break;
            default:
                throw new IllegalArgumentException("See API Documentation");
        }
    }

    /**
     * Sets the range of values for the control. If the maximum is greater than
     * the minimum and the value of the control is outside the new range
     * (subject to the control's current behavior), then the value is changed to
     * the closest valid value.
     * 
     * @param minimum
     *            The minimum value of the range control
     * @param maximum
     *            The maximum value of the range control
     * @return Indicates if the min and max values have been set correctly.
     *         Returns false if the minimum value is greater than or equal to
     *         the maximum value, otherwise returns true
     */
    public boolean setRange(int minimum, int maximum)
    {
        if (minimum >= maximum) return false;

        final int oldMin = this.minimum;
        final int oldMax = this.maximum;

        this.minimum = minimum;
        this.maximum = maximum;

        // Notify look of change (if there is one)
        if (oldMin != minimum || oldMax != maximum)
            notifyLook(new HChangeData(MIN_MAX_CHANGE, new Object[] { new Integer(oldMin), new Integer(oldMax) }));

        return true;
    }

    /**
     * Gets the minimum of the range.
     * 
     * @return The minimum value for the range
     */
    public int getMinValue()
    {
        return minimum;
    }

    /**
     * Get the maximum value of the range
     * 
     * @return The maximum value of the range.
     */
    public int getMaxValue()
    {
        return maximum;
    }

    /**
     * Sets the value of the control, subject to its current behavior. If the
     * specified value is not valid, then the method shall round it to the
     * closest valid value. An application can retrieve the corrected value by
     * means of method <code>getValue()</code>.
     * 
     * @param value
     *            the value for this <code>HStaticRange</code>
     * @see org.havi.ui.HStaticRange#SLIDER_BEHAVIOR
     * @see org.havi.ui.HStaticRange#SCROLLBAR_BEHAVIOR
     */
    public void setValue(int value)
    {
        int min = getMinValue();
        int max = getMaxValue();

        if (getBehavior() == SCROLLBAR_BEHAVIOR)
        {
            min += getThumbMinOffset();
            max -= getThumbMaxOffset();
        }
        // Make sure the value is in range and assign it.
        value = (value < min) ? min : ((value > max) ? max : value);

        int old = this.value;
        if (value != old)
        {
            this.value = value;

            notifyLook(new HChangeData(ADJUSTMENT_VALUE_CHANGE, new Integer(old)));
        }
    }

    /**
     * Gets the value of the control. Note that the recovered value is subject
     * to the control's current behavior.
     * 
     * @see org.havi.ui.HStaticRange#SLIDER_BEHAVIOR
     * @see org.havi.ui.HStaticRange#SCROLLBAR_BEHAVIOR
     */
    public int getValue()
    {
        return value;
    }

    /**
     * Set the offsets for the &quot;thumb&quot; area on this range control. The
     * &quot;thumb&quot; is then drawn from (value - minOffset), to (value +
     * maxOffset) positions outside of the HRange values [minimum:maximum] are
     * clipped to the closest value.
     * <p>
     * There is no requirement that minOffset == maxOffset. For example, both
     * offsets may be zero, yielding a thermometer-like range object. All
     * measurements are in the same units as the minimum / maximum values on the
     * {@link org.havi.ui.HStaticRange HStaticRange} object. The size of the
     * &quot;thumb&quot; is the application author's responsibility. By default
     * the &quot;thumb&quot; does not affect the range over which the value of
     * the {@link org.havi.ui.HStaticRange HStaticRange} may be modified. It is
     * recommended that the {@link org.havi.ui.HRangeLook HRangeLook} provides
     * mechanisms to denote the value of the {@link org.havi.ui.HStaticRange
     * HStaticRange}, in addition to indicating the extent of the thumb as
     * defined by the offsets.
     * <p>
     * If this control's behavior is <code>SCROLLBAR_BEHAVIOR</code>, then the
     * following rules apply: If the thumb offsets are illegal, i.e.
     * <code>minimum + thumbMinOffset</code> is equal or greater than
     * <code>maximum - thumbMaxOffset</code>, then an
     * <code>IllegalArgumentException</code> shall be thrown. If the control's
     * value is not valid for the specified offsets, then the value shall be
     * changed to the closest valid value.
     * 
     * @see HStaticRange#setBehavior
     */
    public void setThumbOffsets(int minOffset, int maxOffset)
    {
        final int oldMin = this.minOffset;
        final int oldMax = this.maxOffset;

        this.minOffset = minOffset;
        this.maxOffset = maxOffset;

        // Notify look of change (if there is one)
        if (oldMin != minimum || oldMax != maximum)
            notifyLook(new HChangeData(THUMB_OFFSETS_CHANGE, new Object[] { new Integer(oldMin), new Integer(oldMax) }));
    }

    /**
     * Returns the thumb offset for its minimum value.
     * 
     * @return the thumb offset for its minimum value.
     */
    public int getThumbMinOffset()
    {
        return minOffset;
    }

    /**
     * Returns the thumb offset for its maximum value.
     * 
     * @return the thumb offset for its maximum value.
     */
    public int getThumbMaxOffset()
    {
        return maxOffset;
    }

    /**
     * Sets the behavior for this {@link org.havi.ui.HStaticRange HStaticRange}.
     * If the new behavior is <code>SCROLLBAR_BEHAVIOR</code> and the control's
     * settings for range and thumb offsets are illegal, i.e. <code>minimum +
     * thumbMinOffset</code> is equal or greater than <code>maximum -
     * thumbMaxOffset</code>, then an <code>IllegalArgumentException</code>
     * shall be thrown. If the control's value is not valid for the offsets,
     * then the value shall be changed to the closest valid value.
     * 
     * @param behavior
     *            the behavior for this {@link org.havi.ui.HStaticRange
     *            HStaticRange} (
     *            {@link org.havi.ui.HStaticRange#SLIDER_BEHAVIOR
     *            SLIDER_BEHAVIOR} or
     *            {@link org.havi.ui.HStaticRange#SCROLLBAR_BEHAVIOR
     *            SCROLLBAR_BEHAVIOR}).
     */
    public void setBehavior(int behavior)
    {
        if (behavior == SLIDER_BEHAVIOR || behavior == SCROLLBAR_BEHAVIOR)
            this.behavior = behavior;
        else
            throw new IllegalArgumentException("See API Documentation");
    }

    /**
     * Returns the behavior for this {@link org.havi.ui.HStaticRange
     * HStaticRange}.
     * 
     * @return the behavior for this {@link org.havi.ui.HStaticRange
     *         HStaticRange}.
     */
    public int getBehavior()
    {
        return behavior;
    }

    /**
     * Defines the range of possible values for this {@link HStaticRange}. The
     * minimum and maximum default to 0 and 100 respectively.
     */
    private int minimum;

    /**
     * Defines the range of possible values for this {@link HStaticRange}. The
     * minimum and maximum default to 0 and 100 respectively.
     */
    private int maximum;

    /**
     * The orientation for this {@link HStaticRange}. Defaults to
     * {@link #ORIENT_LEFT_TO_RIGHT}.
     */
    private int orientation;

    /**
     * The behavior for this {@link HStaticRange}. Defaults to
     * {@link #SLIDER_BEHAVIOR}.
     */
    private int behavior;

    /**
     * The current value for this {@link HStaticRange}. Defaults to
     * <code>0</code>.
     */
    private int value;

    /**
     * The offsets for the thumb of this range control.
     */
    private int minOffset;

    /**
     * The offsets for the thumb of this range control.
     */
    private int maxOffset;

    /**
     * The property which specifies the platform-specific default look for this
     * class. Can be retreived with HaviToolkit.getProperty().
     */
    private static final String PROPERTY_LOOK = "org.havi.ui.HStaticRange.defaultLook";

    /**
     * The type of <code>HLook</code> to use as the default if not explicitly
     * overridden.
     */
    static final Class DEFAULT_LOOK = HRangeLook.class;
}
