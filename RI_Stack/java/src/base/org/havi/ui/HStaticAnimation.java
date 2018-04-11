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

import org.cablelabs.impl.havi.AnimationContext;
import org.cablelabs.impl.manager.AnimationManager;
import org.cablelabs.impl.manager.ManagerManager;

import java.awt.Image;

/**
 * The {@link org.havi.ui.HStaticAnimation HStaticAnimation} is a user interface
 * component used to display animated graphical content but which does
 * <i>not</i> permit the user to navigate (focus) upon it. By default it uses
 * the {@link org.havi.ui.HAnimateLook HAnimateLook} to render itself.
 * 
 * <p>
 * The {@link org.havi.ui.HStaticAnimation HStaticAnimation} class supports
 * animating images a finite number of times or infinitely (continuously), and
 * either forward or in alternating directions with a specified time delay
 * between the rendering of consecutive images.
 * 
 * <p>
 * Calling <code>setVisible(false)</code> on <code>HStaticAnimation</code> shall
 * not automatically stop the animation hence applications are not required to
 * call the <code>play()</code> method again when the animation again becomes
 * visible. It is implementation dependent whether the animation continues from
 * the last visible position or from where it would be if it kept running.
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
 * <td>imagesNormal</td>
 * <td>The array of images to be used as the content for the
 * {@link org.havi.ui.HState#NORMAL_STATE HState.NORMAL_STATE} state of this
 * component.</td>
 * <td>null</td>
 * <td>{@link org.havi.ui.HVisible#setAnimateContent setAnimateContent}</td>
 * <td>{@link org.havi.ui.HVisible#getAnimateContent getAnimateContent}</td>
 * </tr>
 * <tr>
 * <td>delay</td>
 * <td>The delay between the presentation of successive content in the
 * animation, in units of 0.1 second</td>
 * <td>1 (i.e. 0.1 seconds)</td>
 * <td>{@link org.havi.ui.HStaticAnimation#setDelay setDelay}</td>
 * <td>{@link org.havi.ui.HStaticAnimation#getDelay getDelay}</td>
 * </tr>
 * 
 * <tr>
 * <td>repeatCount</td>
 * <td>The number of times that the animation is to be played.</td>
 * <td>{@link org.havi.ui.HAnimateEffect#REPEAT_INFINITE REPEAT_INFINITE}</td>
 * <td>{@link org.havi.ui.HStaticAnimation#setRepeatCount setRepeatCount}</td>
 * <td>{@link org.havi.ui.HStaticAnimation#getRepeatCount getRepeatCount}</td>
 * </tr>
 * 
 * <tr>
 * <td>playMode</td>
 * <td>The playing mode for the animation.</td>
 * <td>{@link org.havi.ui.HAnimateEffect#PLAY_REPEATING PLAY_REPEATING}</td>
 * <td>{@link org.havi.ui.HStaticAnimation#setPlayMode setPlayMode}</td>
 * <td>{@link org.havi.ui.HStaticAnimation#getPlayMode getPlayMode}</td>
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
 * <td>The initial piece of content to be presented, i.e. its position in the
 * content array.</td>
 * <td>0</td>
 * <td>{@link org.havi.ui.HStaticAnimation#setPosition setPosition}</td>
 * <td>{@link org.havi.ui.HStaticAnimation#getPosition getPosition}</td>
 * </tr>
 * 
 * <tr>
 * <td>By default the animation should be stopped. Hence, to start the animation
 * its start method must be explicitly invoked. This mechanism allows for
 * animations that are programmatically controlled, e.g. via the setPosition
 * method.</td>
 * <td>&quot;stopped&quot;</td>
 * <td>{@link org.havi.ui.HStaticAnimation#start start} /
 * {@link org.havi.ui.HStaticAnimation#stop stop}</td>
 * <td>{@link org.havi.ui.HStaticAnimation#isAnimated isAnimated}</td>
 * </tr>
 * <tr>
 * <td>The default &quot;look&quot; for this class.</td>
 * <td>A platform specific {@link org.havi.ui.HAnimateLook HAnimateLook}</td>
 * <td>{@link org.havi.ui.HStaticAnimation#setDefaultLook
 * HStaticAnimation.setDefaultLook}</td>
 * <td>{@link org.havi.ui.HStaticAnimation#getDefaultLook
 * HStaticAnimation.getDefaultLook}</td>
 * </tr>
 * 
 * <tr>
 * <td>The &quot;look&quot; for this object.</td>
 * <td>The {@link org.havi.ui.HAnimateLook HAnimateLook} returned from
 * HStaticAnimation.getDefaultLook when this object was created.</td>
 * <td>{@link org.havi.ui.HStaticAnimation#setLook HStaticAnimation.setLook}</td>
 * <td>{@link org.havi.ui.HStaticAnimation#getLook HStaticAnimation.getLook}</td>
 * </tr>
 * </table>
 * 
 * @author Aaron Kamienski
 * @version 1.1
 */

public class HStaticAnimation extends HVisible implements HNoInputPreferred, HAnimateEffect
{
    /**
     * Creates an {@link org.havi.ui.HStaticAnimation HStaticAnimation} object.
     * See the class description for details of constructor parameters and
     * default values. For constructors which specify content as parameters, see
     * 'State-based content' in HVisible for unspecified content associated with
     * other HStates.
     */
    public HStaticAnimation()
    {
        super(getDefaultLook());
        iniz(null, 1, PLAY_REPEATING, REPEAT_INFINITE);
    }

    /**
     * Creates an {@link org.havi.ui.HStaticAnimation HStaticAnimation} object.
     * See the class description for details of constructor parameters and
     * default values. For constructors which specify content as parameters, see
     * 'State-based content' in HVisible for unspecified content associated with
     * other HStates.
     */
    public HStaticAnimation(java.awt.Image[] imagesNormal, int delay, int playMode, int repeatCount, int x, int y,
            int width, int height)
    {
        super(getDefaultLook(), x, y, width, height);
        iniz(imagesNormal, delay, playMode, repeatCount);
    }

    /**
     * Creates an {@link org.havi.ui.HStaticAnimation HStaticAnimation} object.
     * See the class description for details of constructor parameters and
     * default values. For constructors which specify content as parameters, see
     * 'State-based content' in HVisible for unspecified content associated with
     * other HStates.
     */
    public HStaticAnimation(java.awt.Image[] imagesNormal, int delay, int playMode, int repeatCount)
    {
        this();
        iniz(imagesNormal, delay, playMode, repeatCount);
    }

    /**
     * Common to all constructors.
     */
    private void iniz(java.awt.Image[] images, int delay, int playMode, int repeatCount)
    {
        manager = (AnimationManager) ManagerManager.getInstance(AnimationManager.class);

        if (images != null) setAnimateContent(images, NORMAL_STATE);

        setDelay(delay);
        setPlayMode(playMode);
        setRepeatCount(repeatCount);
    }

    /**
     * Sets the {@link org.havi.ui.HLook HLook} for this component.
     * 
     * @param hlook
     *            The {@link org.havi.ui.HLook HLook} that is to be used for
     *            this component. Note that this parameter may be null, in which
     *            case the component will not draw itself until a look is set.
     * @exception HInvalidLookException
     *                If the {@link org.havi.ui.HLook HLook} is not an
     *                {@link org.havi.ui.HAnimateLook HAnimateLook}.
     */
    public void setLook(HLook hlook) throws HInvalidLookException
    {
        if ((hlook != null) && !(hlook instanceof HAnimateLook)) throw new HInvalidLookException();
        super.setLook(hlook);
    }

    /**
     * Sets the default {@link org.havi.ui.HLook HLook} for further
     * {@link org.havi.ui.HStaticAnimation HStaticAnimation} Components.
     * 
     * @param hlook
     *            The {@link org.havi.ui.HLook HLook} that will be used by
     *            default when creating a new
     *            {@link org.havi.ui.HStaticAnimation HStaticAnimation}
     *            component. Note that this parameter may be null, in which case
     *            newly created components shall not draw themselves until a
     *            non-null look is set using the
     *            {@link org.havi.ui.HStaticAnimation#setLook setLook} method.
     */
    public static void setDefaultLook(HAnimateLook hlook)
    {
        setDefaultLookImpl(PROPERTY_LOOK, hlook);
    }

    /**
     * Returns the currently set default {@link org.havi.ui.HLook HLook} for
     * {@link org.havi.ui.HStaticAnimation HStaticAnimation} components.
     * 
     * @return The {@link org.havi.ui.HLook HLook} that is used by default when
     *         creating a new {@link org.havi.ui.HStaticAnimation
     *         HStaticAnimation} component.
     */
    public static HAnimateLook getDefaultLook()
    {
        return (HAnimateLook) getDefaultLookImpl(PROPERTY_LOOK, DEFAULT_LOOK);
    }

    /**
     * This method starts this {@link org.havi.ui.HStaticAnimation
     * HStaticAnimation} playing. If <code>start</code> is called when the
     * animation is already running it resets the animation according to the
     * current play mode, as returned by
     * {@link org.havi.ui.HStaticAnimation#getPlayMode getPlayMode}.
     */
    public void start()
    {
        manager.start(context = new Context());
    }

    /**
     * This method indicates that the running
     * {@link org.havi.ui.HStaticAnimation HStaticAnimation} should be stopped.
     * After calling this method, there is no guarantee that one or more frames
     * will not be displayed before the animation actually stops playing. If the
     * animation is already stopped further calls to <code>stop</code> have no
     * effect.
     */
    public void stop()
    {
        manager.stop(context);
    }

    /**
     * This method indicates the animation (running) state of the
     * {@link org.havi.ui.HStaticAnimation HStaticAnimation}.
     * 
     * @return <code>true</code> if this {@link org.havi.ui.HStaticAnimation
     *         HStaticAnimation} is running, i.e. the <code>start</code> method
     *         has been invoked - <code>false</code> otherwise.
     */
    public boolean isAnimated()
    {
        return manager.isAnimated(context);
    }

    /**
     * Set this {@link org.havi.ui.HStaticAnimation HStaticAnimation} to display
     * the content at the specified position. If the animation is already
     * running a call to <code>setPosition</code> will change the current value
     * and affect the animation immediately.
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
        if (position < 0) position = 0;

        int oldPosition = getPosition();

        if (oldPosition != position)
        {
            for (int i = 0; i < STATE_COUNT; ++i)
            {
                int state = i | NORMAL_STATE;
                int length = getLength(state);
                if (length == 0)
                    this.position[i] = 0;
                else if (position > length - 1)
                    this.position[i] = length - 1;
                else
                    this.position[i] = position;
            }

            notifyLook(new HChangeData(ANIMATION_POSITION_CHANGE, new Integer(oldPosition)));
        }
    }

    /**
     * Get the current index into the content array which this
     * {@link org.havi.ui.HStaticAnimation HStaticAnimation} is using to display
     * content.
     * 
     * @return the index of the content currently being displayed, in the range
     *         <code>0 <= index < length</code>
     */
    public int getPosition()
    {
        return getPosition(getInteractionState());
    }

    /**
     * Returns the current position for the current state.
     */
    private int getPosition(int state)
    {
        return position[state & ~NORMAL_STATE];
    }

    /**
     * Sets the number of times that this {@link org.havi.ui.HStaticAnimation
     * HStaticAnimation} should be played. If the animation is already running a
     * call to <code>setRepeatCount</code> will change the current value and
     * reset the current number of repeats to 0, affecting the animation
     * immediately.
     * 
     * @param count
     *            the number of times that an
     *            {@link org.havi.ui.HStaticAnimation HStaticAnimation} should
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

        int oldCount = repeat;
        repeat = count;

        // Notify look of change.
        notifyLook(new HChangeData(REPEAT_COUNT_CHANGE, new Integer(oldCount)));

        // Use new repeat count if was playing
        if (restart) start();
    }

    /**
     * Gets the number of times that this {@link org.havi.ui.HStaticAnimation
     * HStaticAnimation} is to be played. Note that this method does
     * <em>not</em> return the number of repeats that are remaining to be
     * played.
     * <p>
     * Except for <code>HAnimateEffect</code> implementations that specify a
     * different default, <code>getRepeatCount()</code> returns
     * <code>REPEAT_INFINITE</code> if no call to <code>setRepeatCount()</code>
     * has previously been made.
     * 
     * @return the total number of times that an
     *         {@link org.havi.ui.HStaticAnimation HStaticAnimation} is to be
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
     * After calling {@link org.havi.ui.HStaticAnimation#setDelay setDelay} on a
     * currently playing {@link org.havi.ui.HStaticAnimation HStaticAnimation},
     * there is no guarantee that one or more frames will not be displayed using
     * the previous delay until the new delay value takes effect.
     * 
     * @param count
     *            the content presentation delay in units of 0.1 seconds
     *            duration. If count is less than one &quot;unit&quot;, then it
     *            shall be treated as if it were a delay of one
     *            &quot;unit&quot;, i.e. 0.1 seconds.
     */
    public void setDelay(int count)
    {
        if (count < 1) count = 1;
        delay = count;
    }

    /**
     * Gets the presentation delay for this {@link org.havi.ui.HStaticAnimation
     * HStaticAnimation}.
     * 
     * @return the presentation delay in units of 0.1 seconds.
     */
    public int getDelay()
    {
        return delay;
    }

    /**
     * Sets the playing mode for this {@link org.havi.ui.HStaticAnimation
     * HStaticAnimation}. If the animation is already running a call to
     * <code>setPlayMode</code> will change the current value and affect the
     * animation immediately. The position of the animation is unchanged.
     * 
     * @param mode
     *            the play mode for this {@link org.havi.ui.HStaticAnimation
     *            HStaticAnimation}, which must be either
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
                throw new IllegalArgumentException("See API documentation");
        }
    }

    /**
     * Gets the playing mode for this {@link org.havi.ui.HStaticAnimation
     * HStaticAnimation}.
     * 
     * @return the play mode for this {@link org.havi.ui.HStaticAnimation
     *         HStaticAnimation}.
     */
    public int getPlayMode()
    {
        return playMode;
    }

    /**
     * Gets the length of the given state's content.
     */
    private int getLength(int state)
    {
        Image[] images = getDefaultContent(state);

        return (images != null) ? images.length : 0;
    }

    /**
     * Gets the content for the given state. If no content exists, attempts to
     * retrieve the nearest appropriate content. This is what
     * <code>HAnimateLook</code> will do when displaying content.
     */
    private Image[] getDefaultContent(int state)
    {
        Image[] animate = getAnimateContent(state);
        if (animate == null)
        {
            switch (state)
            {
                case FOCUSED_STATE:
                case DISABLED_STATE:
                    return getDefaultContent(NORMAL_STATE);
                case ACTIONED_STATE:
                case ACTIONED_FOCUSED_STATE:
                    return getDefaultContent(FOCUSED_STATE);
                case DISABLED_FOCUSED_STATE:
                case DISABLED_ACTIONED_FOCUSED_STATE:
                    return getDefaultContent(DISABLED_STATE);
                case DISABLED_ACTIONED_STATE:
                    return getDefaultContent(ACTIONED_STATE);
            }
        }
        return animate;
    }

    /**
     * Advance the current position based on the current play mode.
     */
    private int advancePosition(int countdown)
    {
        int state = getInteractionState();
        int i = getPosition(state);
        int length = getLength(state);

        // Nothing to play... forget it
        if (length == 0) return (countdown == REPEAT_INFINITE) ? countdown : 0;

        if (playMode == PLAY_REPEATING)
        {
            // Advance position
            ++i;

            // Check for complete cycle
            if (countdown != REPEAT_INFINITE && (i == length - 1)) --countdown;
            // Reset if gone too far
            if (i == length) i = 0;
        }
        else
        // if (playMode == PLAY_ALTERNATING)
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
            return HStaticAnimation.this;
        }

        // Comments copied from AnimationManager.AnimationContext
        public boolean advancePosition()
        {
            return 0 == (countdown = HStaticAnimation.this.advancePosition(countdown));
        }

        // Comments copied from AnimationManager.AnimationContext
        public int getDelay()
        {
            return HStaticAnimation.this.getDelay();
        }

        // Comments copied from AnimationManager.AnimationContext
        public boolean isAnimated()
        {
            return context == this;
        }
    }

    /**
     * Indicates the current direction (forwards or backwards) for
     * {@link PLAY_ALTERNATING} mode.
     * <ul>
     * <li><code>1</code> - forward
     * <li><code>-1</code> - backward
     * </ul>
     */
    private int direction = 1;

    /** The number of states. */
    private static final int STATE_COUNT = 8;

    /** Indicates the current animation position. */
    private int position[] = new int[STATE_COUNT];

    /** Indicates how often to repeat the animation */
    private int repeat = REPEAT_INFINITE;

    /** Indicates the delay in 0.1 second units between position advances. */
    private int delay = 1;

    /** Indicates the current playback mode. */
    private int playMode = PLAY_REPEATING;

    /** The current AnimationContext. */
    private AnimationContext context;

    /**
     * The AnimationManager being used.
     */
    private AnimationManager manager;

    /**
     * The property which specifies the platform-specific default look for this
     * class. Can be retrieved with HaviToolkit.getProperty().
     */
    private static final String PROPERTY_LOOK = "org.havi.ui.HStaticAnimation.defaultLook";

    /**
     * The type of <code>HLook</code> to use as the default if not explicitly
     * overridden.
     */
    static final Class DEFAULT_LOOK = HAnimateLook.class;
}
