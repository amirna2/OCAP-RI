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

import java.awt.Component;
import java.awt.Image;
import java.awt.Point;
import java.util.Vector;

import org.cablelabs.impl.havi.AnimationContext;
import org.cablelabs.impl.manager.AnimationManager;
import org.cablelabs.impl.manager.ManagerManager;

/**
 * The {@link org.havi.ui.HImageEffectMatte HImageEffectMatte} class represents
 * a matte that varies over both space and time, it is specified as a sequence
 * of image masks.
 * 
 * <p>
 * The data for any HImageEffectMatte may be changed &quot;on the fly&quot;
 * using the {@link org.havi.ui.HImageEffectMatte#setMatteData setMatteData}
 * method.
 * 
 * However, some implementations may be asynchronously referencing their content
 * (i.e. through a separate implementation-specific animation thread). Therefore
 * the following restrictions apply to the
 * {@link org.havi.ui.HImageEffectMatte#setMatteData setMatteData} method:
 * 
 * <p>
 * <ul>
 * <li>The method must be synchronized with any implementation-specific
 * animation thread such that content cannot be changed while a different thread
 * is using it.
 * <li>If the animation was running the method should stop the animation in a
 * synchronized manner before changing content.
 * <li>The method should reset the animation to a starting position defined by
 * the current play mode. The repeat count of the animation should be reset to
 * 0.
 * <li>If the animation was running the method should start the animation.
 * </ul>
 * <p>
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
 * <td>data</td>
 * <td>The transparency data for this image effect matte.</td>
 * <td>null (the matte should be treated as being spatially and temporally
 * unvarying and opaque)</td>
 * <td>{@link org.havi.ui.HImageEffectMatte#setMatteData setMatteData}</td>
 * <td>{@link org.havi.ui.HImageEffectMatte#getMatteData getMatteData}</td>
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
 * <td>The initial piece of content to be presented, i.e. its position in the
 * content array.</td>
 * <td>0</td>
 * <td>{@link org.havi.ui.HImageEffectMatte#setPosition setPosition}</td>
 * <td>{@link org.havi.ui.HImageEffectMatte#getPosition getPosition}</td>
 * </tr>
 * 
 * <tr>
 * <td>By default the animation should be stopped. Hence, to start the animation
 * its start method must be explicitly invoked. This mechanism allows for
 * animations that are programmatically controlled, e.g. via the setPosition
 * method.</td>
 * <td>&quot;stopped&quot;</td>
 * <td>{@link org.havi.ui.HImageEffectMatte#start start} /
 * {@link org.havi.ui.HImageEffectMatte#stop stop}</td>
 * <td>{@link org.havi.ui.HImageEffectMatte#isAnimated isAnimated}</td>
 * </tr>
 * <tr>
 * <td>The pixel offset for each image within the
 * {@link org.havi.ui.HImageEffectMatte HImageEffectMatte}, relative to the top,
 * left corner of its associated component.</td>
 * <td>A java.awt.Point (0, 0)</td>
 * <td>{@link org.havi.ui.HImageEffectMatte#setOffset setOffset}</td>
 * <td>{@link org.havi.ui.HImageEffectMatte#getOffset getOffset}</td>
 * </tr>
 * </table>
 * 
 * @author Todd Earles
 * @author Aaron Kamienski (1.0.1 support)
 * @version 1.1
 */

public class HImageEffectMatte implements HMatte, HAnimateEffect
{
    /**
     * Creates an {@link org.havi.ui.HImageEffectMatte HImageEffectMatte}
     * object. See the class description for details of constructor parameters
     * and default values.
     */
    public HImageEffectMatte()
    {
    }

    /**
     * Creates an {@link org.havi.ui.HImageEffectMatte HImageEffectMatte}
     * object. See the class description for details of constructor parameters
     * and default values.
     */
    public HImageEffectMatte(java.awt.Image[] data)
    {
        setMatteData(data);
    }

    /**
     * Sets the data for this matte (an array of images). Any previously set
     * data is replaced. If this method is called when the animation is running
     * the data is changed immediately and the current animation position is
     * reset according to the active play mode. The changes affect the animation
     * immediately.
     * <p>
     * Note that if the size of an image is smaller than the size of the
     * component to which the matte is applied, the empty space behaves as if it
     * were an opaque flat matte of value 1.0. By default images are aligned at
     * the top left corner of the component. This can be changed with the
     * {@link org.havi.ui.HImageEffectMatte#setOffset setOffset} method
     * 
     * @param data
     *            the data for this matte. Specify a null object to remove the
     *            associated data for this matte. If the length of the data
     *            array is zero, an IllegalArgumentException is thrown. If an
     *            element of the data array is null, or an image referred to is
     *            still being loaded, then that image will be skipped when the
     *            animation is playing.
     */
    public void setMatteData(java.awt.Image[] data)
    {
        images = data;
    }

    /**
     * Returns the data used for this matte.
     * 
     * @return the data used for this matte (an array of images) or null if no
     *         matte data has been set.
     */
    public java.awt.Image[] getMatteData()
    {
        return images;
    }

    /**
     * Set the offset of a specified frame of the matte relative to its
     * component in pixels.
     * 
     * @param p
     *            the offset of the specified frame of the matte relative to the
     *            top left corner of its component in pixels. If p is null a
     *            NullPointerException is thrown.
     * @param index
     *            the zero-index to the data for which the offset should be
     *            applied. If index is not valid for this matte an
     *            IndexOutOfBounds exception is thrown.
     */
    public void setOffset(java.awt.Point p, int index)
    {
        // Expand the offset array if the index is out of range
        offsets.ensureCapacity(index + 1);
        offsets.setSize(offsets.capacity());

        // Save the offset for the specified frame
        offsets.setElementAt(new Point(p.x, p.y), index);
    }

    /**
     * Get the offset of a specified frame of the matte relative to its
     * component in pixels.
     * 
     * @param index
     *            the zero-index to the data for which the offset should be
     *            recovered. If index is not valid for this matte an
     *            IndexOutOfBounds exception is thrown.
     * @return the offset of the specified frame of the matte relative to its
     *         component in pixels (as a Point)
     */
    public java.awt.Point getOffset(int index)
    {
        int x = 0, y = 0;
        try
        {
            Point offset = (Point) offsets.elementAt(index);
            if (offset != null)
            {
                x = offset.x;
                y = offset.y;
            }
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            if (index < 0) throw e;
        }
        return new Point(x, y);
    }

    /**
     * This method starts this {@link org.havi.ui.HImageEffectMatte
     * HImageEffectMatte} playing. If <code>start</code> is called when the
     * animation is already running it resets the animation according to the
     * current play mode, as returned by
     * {@link org.havi.ui.HImageEffectMatte#getPlayMode getPlayMode}.
     */
    public void start()
    {
        manager.start(context = new Context());
    }

    /**
     * This method indicates that the running
     * {@link org.havi.ui.HImageEffectMatte HImageEffectMatte} should be
     * stopped. After calling this method, there is no guarantee that one or
     * more frames will not be displayed before the animation actually stops
     * playing. If the animation is already stopped further calls to
     * <code>stop</code> have no effect.
     */
    public void stop()
    {
        manager.stop(context);
    }

    /**
     * This method indicates the animation (running) state of the
     * {@link org.havi.ui.HImageEffectMatte HImageEffectMatte}.
     * 
     * @return <code>true</code> if this {@link org.havi.ui.HImageEffectMatte
     *         HImageEffectMatte} is running, i.e. the <code>start</code> method
     *         has been invoked - <code>false</code> otherwise.
     */
    public boolean isAnimated()
    {
        return manager.isAnimated(context);
    }

    /**
     * Set this {@link org.havi.ui.HImageEffectMatte HImageEffectMatte} to
     * display the content at the specified position. If the animation is
     * already running a call to <code>setPosition</code> will change the
     * current value and affect the animation immediately.
     * 
     * @param position
     *            an index into the content array which specifies the next piece
     *            of content to be displayed. If <code>position</code> is less
     *            than 0, then the array element at index 0 is displayed, if
     *            <code>position</code> is greater than or equal to the length
     *            of the content array, then the array element at index [
     *            <code>length</code>-1] will be used.
     */
    public void setPosition(int position)
    {
        // Adjust the position if it is out of range and save it
        int length = (images == null) ? 0 : images.length;
        if (position >= length) position = length - 1;
        if (position < 0) position = 0;
        this.position = position;

        // Repaint the registered components if they are visible
        if (registeredComponents.size() > 0)
        {
            Component c[] = new Component[registeredComponents.size()];
            registeredComponents.copyInto(c);
            for (int i = 0; i < c.length; ++i)
                if (c != null && c[i].isVisible()) c[i].repaint();
        }
    }

    /**
     * Get the current index into the content array which this
     * {@link org.havi.ui.HImageEffectMatte HImageEffectMatte} is using to
     * display content.
     * 
     * @return the index of the content currently being displayed, in the range
     *         <code>0 <= index < length</code>
     */
    public int getPosition()
    {
        return position;
    }

    /**
     * Sets the number of times that this {@link org.havi.ui.HImageEffectMatte
     * HImageEffectMatte} should be played. If the animation is already running
     * a call to <code>setRepeatCount</code> will change the current value and
     * reset the current number of repeats to 0, affecting the animation
     * immediately.
     * 
     * @param count
     *            the number of times that an
     *            {@link org.havi.ui.HImageEffectMatte HImageEffectMatte} should
     *            be played. Valid values of the repeat count are one or more,
     *            and {@link org.havi.ui.HAnimateEffect#REPEAT_INFINITE
     *            REPEAT_INFINITE}.
     */
    public void setRepeatCount(int count)
    {
        if (count <= 0 && count != REPEAT_INFINITE)
            throw new IllegalArgumentException("Invalid repeat count of " + count);

        // Effectively reset current repeat count to 0
        boolean restart = isAnimated();
        stop();

        repeat = count;

        // Use new repeat count if was playing
        if (restart) start();
    }

    /**
     * Gets the number of times that this {@link org.havi.ui.HImageEffectMatte
     * HImageEffectMatte} is to be played. Note that this method does
     * <em>not</em> return the number of repeats that are remaining to be
     * played.
     * <p>
     * Except for <code>HAnimateEffect</code> implementations that specify a
     * different default, <code>getRepeatCount()</code> returns
     * <code>REPEAT_INFINITE</code> if no call to <code>setRepeatCount()</code>
     * has previously been made.
     * 
     * @return the total number of times that an
     *         {@link org.havi.ui.HImageEffectMatte HImageEffectMatte} is to be
     *         played. The returned value shall be greater than zero, or
     *         {@link org.havi.ui.HAnimateEffect#REPEAT_INFINITE
     *         REPEAT_INFINITE}.
     */
    public int getRepeatCount()
    {
        return repeat;
    }

    /**
     * Sets the delay between the presentation of successive pieces of content
     * (frames).
     * <p>
     * After calling {@link org.havi.ui.HImageEffectMatte#setDelay setDelay} on
     * a currently playing {@link org.havi.ui.HImageEffectMatte
     * HImageEffectMatte}, there is no guarantee that one or more frames will
     * not be displayed using the previous delay until the new delay value takes
     * effect.
     * 
     * @param count
     *            the content presentation delay in units of 0.1 seconds
     *            duration. If count is less than one &quot;unit&quot;, then it
     *            shall be treated as if it were a delay of one
     *            &quot;unit&quot;, i.e. 0.1 seconds.
     */
    public void setDelay(int count)
    {
        // Adjust the delay if it is out of range and save it
        if (count < 1) count = 1;
        delay = count;
    }

    /**
     * Gets the presentation delay for this
     * {@link org.havi.ui.HImageEffectMatte HImageEffectMatte}.
     * 
     * @return the presentation delay in units of 0.1 seconds.
     */
    public int getDelay()
    {
        return delay;
    }

    /**
     * Sets the playing mode for this {@link org.havi.ui.HImageEffectMatte
     * HImageEffectMatte}. If the animation is already running a call to
     * <code>setPlayMode</code> will change the current value and affect the
     * animation immediately. The position of the animation is unchanged.
     * 
     * @param mode
     *            the play mode for this {@link org.havi.ui.HImageEffectMatte
     *            HImageEffectMatte}, which must be either
     *            {@link org.havi.ui.HAnimateEffect#PLAY_ALTERNATING} or
     *            {@link org.havi.ui.HAnimateEffect#PLAY_REPEATING}.
     */
    public void setPlayMode(int mode)
    {
        switch (mode)
        {
            case PLAY_REPEATING:
            case PLAY_ALTERNATING:
                playMode = mode;
                break;
            default:
                throw new IllegalArgumentException("See API Documentation");
        }
    }

    /**
     * Gets the playing mode for this {@link org.havi.ui.HImageEffectMatte
     * HImageEffectMatte}.
     * 
     * @return the play mode for this {@link org.havi.ui.HImageEffectMatte
     *         HImageEffectMatte}.
     */
    public int getPlayMode()
    {
        return playMode;
    }

    /**
     * Register a component with this matte. The specified component will be
     * repainted whenever the matte position changes.
     * 
     * @param component
     *            the component to repaint when the matte position changes
     */
    void registerComponent(java.awt.Component component)
    {
        registeredComponents.addElement(component);
    }

    /**
     * Unregister the component that was previously registerd with this matte.
     */
    void unregisterComponent(java.awt.Component component)
    {
        registeredComponents.removeElement(component);
    }

    /**
     * Advance the current position based on the current play mode.
     */
    private int advancePosition(int countdown)
    {
        int i = position;
        int length = (images == null) ? 0 : images.length;

        // If the length is 0, then there is nothing to play.
        if (length == 0) return (countdown == REPEAT_INFINITE) ? countdown : 0;

        // Advance depends on the play mode
        if (playMode == PLAY_REPEATING)
        {
            // Advance position
            ++i;

            // Check for complete cycle
            if (countdown != REPEAT_INFINITE && (i == length - 1)) --countdown;

            // Reset if gone too far
            if (i == length)
            {
                i = 0;
            }
        }
        else if (playMode == PLAY_ALTERNATING)
        {
            // Move in given direction
            i += direction;

            // Check for complete cycle
            if (countdown != REPEAT_INFINITE && (i == length - 1 || i == 0)) --countdown;

            // Reset and reverse direction if gone too far
            if (i == length || i == -1)
            {
                direction = -direction;
                /*
                 * if i == -1 => i += 2 if i == length => i -= 2
                 */
                i += direction + direction;
            }
        }

        // Set the new position
        setPosition(i);

        return countdown;
    }

    /**
     * This is the interface used by the AnimationManager to control the
     * animation of this component.
     */
    private class Context extends AnimationContext
    {
        private int countdown = repeat;

        // Comments copied from AnimationManager.AnimationContext
        public Object getAnimation()
        {
            return HImageEffectMatte.this;
        }

        // Comments copied from AnimationManager.AnimationContext
        public boolean advancePosition()
        {
            return 0 == (countdown = HImageEffectMatte.this.advancePosition(countdown));
        }

        // Comments copied from AnimationManager.AnimationContext
        public int getDelay()
        {
            return HImageEffectMatte.this.getDelay();
        }

        // Comments copied from AnimationManager.AnimationContext
        public boolean isAnimated()
        {
            return context == this;
        }
    }

    /** Array of images for the matte */
    private Image[] images;

    /** Matte offset for each frame in the matte */
    private Vector offsets = new Vector();

    /** Indicates the current animation position. */
    private int position;

    /** Indicates how often to repeat the animation */
    private int repeat = REPEAT_INFINITE;

    /** Indicates the delay in 0.1 second units between position advances. */
    private int delay = 1;

    /** Indicates the current playback mode. */
    private int playMode = PLAY_REPEATING;

    /** The current AnimationContext. */
    private AnimationContext context;

    /** The AnimationManager that controls playback. */
    private AnimationManager manager = (AnimationManager) ManagerManager.getInstance(AnimationManager.class);

    /**
     * Indicates the current direction (forwards or backwards) for
     * {@link PLAY_ALTERNATING} mode.
     * <ul>
     * <li><code>1</code> - forward
     * <li><code>-1</code> - backward
     * </ul>
     */
    private int direction = 1;

    /** The components to repaint when the matte position changes */
    private Vector registeredComponents = new Vector();
}
