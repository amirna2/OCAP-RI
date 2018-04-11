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
 * The {@link org.havi.ui.HAnimateEffect HAnimateEffect} interface defines
 * effect constants and controls for time-varying animations.
 * <p>
 * <hr>
 * <p>
 * Implementations of HAnimateEffect should have the following default
 * behaviors:
 * <ul>
 * <li>By default the {@link org.havi.ui.HAnimateEffect HAnimateEffect} should
 * be stopped. Hence, to start an {@link org.havi.ui.HAnimateEffect
 * HAnimateEffect} the {@link org.havi.ui.HAnimateEffect#start start} method
 * must be explicitly invoked. This mechanism allows for animations that are
 * programmatically controlled, e.g. via the
 * {@link org.havi.ui.HAnimateEffect#setPosition setPosition} method.
 * <li>By default the position for rendering should be the first image in the
 * sequence, i.e. 0.
 * <li>By default the play mode should be
 * {@link org.havi.ui.HAnimateEffect#PLAY_REPEATING PLAY_REPEATING}.
 * <li>By default the repeat count should be
 * {@link org.havi.ui.HAnimateEffect#REPEAT_INFINITE REPEAT_INFINITE}.
 * <li>The default rendering should simply display the single image at the
 * current position of the animation within the sequence.
 * </ul>
 */

public interface HAnimateEffect
{
    /**
     * Indicates that the animation should be played forwards (in a repeating
     * loop).
     * <p>
     * The images are rendered in the same order that they are present in the
     * sequence (array):
     * <p>
     * <center>0, 1, 2, 3, ... length-1</center>
     * <p>
     * If the animation has not repeated sufficiently, then the rendering of the
     * sequence is restarted from the first image, i.e. the images will continue
     * to be rendered in the order:
     * <p>
     * <center> 0, 1, 2, 3, ... length-1</center>
     * <p>
     * Each rendering of the sequence of images 0 to (length-1), should be
     * considered as a single &quot;repeat&quot;.
     */
    public static final int PLAY_REPEATING = 1;

    /**
     * Indicates that the animation should be played in a repeating loop,
     * alternating between the forward and reverse direction.
     * <p>
     * The images are rendered in the same order that they are present in the
     * sequence (array):
     * <p>
     * <center>0, 1, 2, 3, ... length-2, length-1 </center>
     * <p>
     * If the animation has not repeated sufficiently, then the rendering of the
     * sequence is reversed - i.e. the images are rendered in the order
     * <p>
     * <center>length-2, length-3, ... 1, 0</center>
     * <p>
     * If the animation has not repeated sufficiently, then the rendering of the
     * sequence is reversed (again) back to a forwards direction. I.e. the
     * images are rendered in the order
     * <p>
     * <center>1, 2, 3, ... length-2, length-1</center></center>
     * <p>
     * Each rendering of the sequence of images forwards or backwards, should be
     * considered as a single &quot;repeat&quot;.
     * <p>
     * Note that when the sequence repeats, the last image (first image) is not
     * rendered consecutively, i.e. twice.
     */
    public static final int PLAY_ALTERNATING = 2;

    /**
     * This value, when passed to <code>setRepeatCount</code>, indicates that
     * the animation shall repeat until the
     * {@link org.havi.ui.HAnimateEffect#stop stop} method is invoked.
     */
    public static final int REPEAT_INFINITE = -1;

    /**
     * This method starts this {@link org.havi.ui.HAnimateEffect HAnimateEffect}
     * playing. If <code>start</code> is called when the animation is already
     * running it resets the animation according to the current play mode, as
     * returned by {@link org.havi.ui.HAnimateEffect#getPlayMode getPlayMode}.
     */
    public void start();

    /**
     * This method indicates that the running {@link org.havi.ui.HAnimateEffect
     * HAnimateEffect} should be stopped. After calling this method, there is no
     * guarantee that one or more frames will not be displayed before the
     * animation actually stops playing. If the animation is already stopped
     * further calls to <code>stop</code> have no effect.
     */
    public void stop();

    /**
     * This method indicates the animation (running) state of the
     * {@link org.havi.ui.HAnimateEffect HAnimateEffect}.
     * 
     * @return <code>true</code> if this {@link org.havi.ui.HAnimateEffect
     *         HAnimateEffect} is running, i.e. the <code>start</code> method
     *         has been invoked - <code>false</code> otherwise.
     */
    public boolean isAnimated();

    /**
     * Set this {@link org.havi.ui.HAnimateEffect HAnimateEffect} to display the
     * content at the specified position. If the animation is already running a
     * call to <code>setPosition</code> will change the current value and affect
     * the animation immediately.
     * 
     * @param position
     *            an index into the content array which specifies the next piece
     *            of content to be displayed. If <code>position</code> is less
     *            than 0, then the array element at index 0 is displayed, if
     *            <code>position</code> is greater than or equal to the length
     *            of the content array, then the array element at index [
     *            <code>length</code>-1] will be used.
     */
    public void setPosition(int position);

    /**
     * Get the current index into the content array which this
     * {@link org.havi.ui.HAnimateEffect HAnimateEffect} is using to display
     * content.
     * 
     * @return the index of the content currently being displayed, in the range
     *         <code>0 <= index < length</code>
     */
    public int getPosition();

    /**
     * Sets the number of times that this {@link org.havi.ui.HAnimateEffect
     * HAnimateEffect} should be played. If the animation is already running a
     * call to <code>setRepeatCount</code> will change the current value and
     * reset the current number of repeats to 0, affecting the animation
     * immediately.
     * 
     * @param count
     *            the number of times that an {@link org.havi.ui.HAnimateEffect
     *            HAnimateEffect} should be played. Valid values of the repeat
     *            count are one or more, and
     *            {@link org.havi.ui.HAnimateEffect#REPEAT_INFINITE
     *            REPEAT_INFINITE}.
     */
    public void setRepeatCount(int count);

    /**
     * Gets the number of times that this {@link org.havi.ui.HAnimateEffect
     * HAnimateEffect} is to be played. Note that this method does <em>not</em>
     * return the number of repeats that are remaining to be played.
     * <p>
     * Except for <code>HAnimateEffect</code> implementations that specify a
     * different default, <code>getRepeatCount()</code> returns
     * <code>REPEAT_INFINITE</code> if no call to <code>setRepeatCount()</code>
     * has previously been made.
     * 
     * @return the total number of times that an
     *         {@link org.havi.ui.HAnimateEffect HAnimateEffect} is to be
     *         played. The returned value shall be greater than zero, or
     *         {@link org.havi.ui.HAnimateEffect#REPEAT_INFINITE
     *         REPEAT_INFINITE}.
     */
    public int getRepeatCount();

    /**
     * Sets the delay between the presentation of successive pieces of content
     * (frames).
     * <p>
     * After calling {@link org.havi.ui.HAnimateEffect#setDelay setDelay} on a
     * currently playing {@link org.havi.ui.HAnimateEffect HAnimateEffect},
     * there is no guarantee that one or more frames will not be displayed using
     * the previous delay until the new delay value takes effect.
     * 
     * @param count
     *            the content presentation delay in units of 0.1 seconds
     *            duration. If count is less than one &quot;unit&quot;, then it
     *            shall be treated as if it were a delay of one
     *            &quot;unit&quot;, i.e. 0.1 seconds.
     */
    public void setDelay(int count);

    /**
     * Gets the presentation delay for this {@link org.havi.ui.HAnimateEffect
     * HAnimateEffect}.
     * 
     * @return the presentation delay in units of 0.1 seconds.
     */
    public int getDelay();

    /**
     * Sets the playing mode for this {@link org.havi.ui.HAnimateEffect
     * HAnimateEffect}. If the animation is already running a call to
     * <code>setPlayMode</code> will change the current value and affect the
     * animation immediately. The position of the animation is unchanged.
     * 
     * @param mode
     *            the play mode for this {@link org.havi.ui.HAnimateEffect
     *            HAnimateEffect}, which must be either
     *            {@link org.havi.ui.HAnimateEffect#PLAY_ALTERNATING} or
     *            {@link org.havi.ui.HAnimateEffect#PLAY_REPEATING}.
     */
    public void setPlayMode(int mode);

    /**
     * Gets the playing mode for this {@link org.havi.ui.HAnimateEffect
     * HAnimateEffect}.
     * 
     * @return the play mode for this {@link org.havi.ui.HAnimateEffect
     *         HAnimateEffect}.
     */
    public int getPlayMode();
}
