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

import org.havi.ui.HLook;
import org.havi.ui.HNoInputPreferred;
import org.havi.ui.HVisible;
import org.havi.ui.HAnimateEffect;
import org.havi.ui.HChangeData;
import org.havi.ui.HInvalidLookException;
import org.cablelabs.gear.data.GraphicData;
import org.cablelabs.gear.data.AnimationData;
import org.cablelabs.gear.util.AnimationManager;
import org.cablelabs.gear.util.AnimationManager.SingleThread;
import org.cablelabs.gear.util.AnimationManager.AnimationContext;
import org.cablelabs.gear.havi.decorator.OutlineDecorator;
import org.cablelabs.gear.havi.decorator.TextDecorator;
import org.cablelabs.gear.havi.decorator.GraphicDataDecorator;
import org.cablelabs.gear.havi.decorator.AnimateDataDecorator;
import org.cablelabs.gear.havi.decorator.FillDecorator;

/**
 * A <code>Label</code> is an {@link org.havi.ui.HVisible} subclass that
 * provides support for <i>wrapped</i> {@link GraphicData graphic} and
 * {@link AnimationData animation} data. These are used instead of the standard
 * <code>Image</code>-based {@link org.havi.ui.HVisible#getGraphicContent(int)
 * graphic} and {@link org.havi.ui.HVisible#getAnimateContent(int) animation}
 * content.
 * <p>
 * <code>Label</code> is a functional replacement for the
 * {@link org.havi.ui.HStaticIcon}, {@link org.havi.ui.HStaticText} and
 * {@link org.havi.ui.HStaticAnimation} classes; the main difference being that
 * it does not restrict itself to one type of data by restricting itself to one
 * type of look. Whether it is a text, graphic, or animated label depends on the
 * type of data and look associated with the component.
 * <p>
 * <code>Label</code> implements the {@link org.havi.ui.HAnimateEffect}
 * interface. This means that any <code>Label</code> instance can be animated.
 * It should be noted that <code>Label</code> deviates from the HAVi
 * specification of <code>HAnimateEffect</code> with respect to the base
 * {@link org.havi.ui.HAnimateEffect#setDelay(int) delay} unit. Instead of 0.1
 * seconds, <code>Label</code> uses 0.042 seconds. This allows for a maximum
 * frame rate of approximately 24 fps compared to HAVi's 10 fps.
 * <p>
 * A <code>Label</code> is permanently in the
 * {@link org.havi.ui.HState#NORMAL_STATE}. As such, setting content for any
 * other states is useless.
 * 
 * @author Aaron Kamienski
 * @version $Id: Label.java,v 1.5 2002/11/07 21:13:40 aaronk Exp $
 */
public class Label extends HVisible implements HNoInputPreferred, HAnimateEffect, HasGraphicData, HasAnimationData,
        HasStringData
{
    /*  ************************ Constructors ************************ */

    /**
     * Default constructor. The constructed <code>Label</code> is initialized
     * with no <i>content</i> and the standard look.
     */
    public Label()
    {
        this(getStandardLook());

        /*
         * This was added so that our components have a non-zero size when
         * created with the default constructor. When added to a form within an
         * IDE, our components would come up with a preferred size of (0,0).
         * Rule 4 of the HAVi specification for getPreferredSize() on HLook
         * states the following:
         * 
         * 4.If there is no content and no default size set then the return
         * value is the current size of the HVisible as returned by getSize.
         */
        setSize(10, 10);
    }

    /**
     * Default constructor. The constructed <code>Label</code> is initialized
     * with no <i>content</i> and the given look.
     * 
     * @param look
     *            the look to be used by this component
     */
    public Label(HLook look)
    {
        try
        {
            setLook(look);
        }
        catch (HInvalidLookException ignored)
        {
            // Will never occur
        }
    }

    /**
     * Icon constructor. The constructed <code>Label</code> is initialized with
     * graphic data content (NORMAL_STATE) and the standard look.
     * 
     * @param icon
     *            the graphic data content
     */
    public Label(GraphicData icon)
    {
        this();
        setGraphicData(NORMAL_STATE, icon);
    }

    /**
     * Text constructor. The constructed <code>Label</code> is initialized with
     * string data content (NORMAL_STATE) and the standard look.
     * 
     * @param text
     *            the string data content
     */
    public Label(String text)
    {
        this();
        setStringData(NORMAL_STATE, text);
    }

    /**
     * Animation constructor. The constructed <code>Label</code> is initialized
     * with animation data content (NORMAL_STATE) and the standard look.
     * 
     * @param anim
     *            the animation data content
     */
    public Label(AnimationData anim)
    {
        this();
        setAnimationData(NORMAL_STATE, anim);
    }

    /**
     * Constructor which takes sizing and positioning parameters.
     * 
     * @param look
     *            the look to be used by this component
     * @param x
     *            the x-coordinate
     * @param y
     *            the y-coordinate
     * @param width
     *            the width
     * @param height
     *            the height
     */
    public Label(HLook look, int x, int y, int width, int height)
    {
        super(look, x, y, width, height);
        try
        {
            setLook(look);
        }
        catch (HInvalidLookException ignored)
        {
        }
    }

    /**
     * Constructor which takes all types of content as well as sizing and
     * positioning parameters.
     * 
     * @param look
     *            the look to be used by this component
     * @param x
     *            the x-coordinate
     * @param y
     *            the y-coordinate
     * @param width
     *            the width
     * @param height
     *            the height
     * @param icon
     *            the graphic data content
     * @param text
     *            the string data content
     * @param anim
     *            the animation data content
     */
    public Label(HLook look, int x, int y, int width, int height, GraphicData icon, String text, AnimationData anim)
    {
        this(look, x, y, width, height);
        setGraphicData(NORMAL_STATE, icon);
        setStringData(NORMAL_STATE, text);
        setAnimationData(NORMAL_STATE, anim);
    }

    /**
     * The <i>standard</i> look is the default look used by all newly
     * constructed <code>Label</code> components (and subclasses). This is a
     * look that renders <code>String</code>, <code>GraphicData</code>, and
     * <code>AnimationData</code> content, allows for filling of the background,
     * and renders an outline for <code>FOCUSED_STATES</code>.
     * 
     * @return the singleton look used by all <code>Label</code> components by
     *         default
     */
    public static HLook getStandardLook()
    {
        return standardLook;
    }

    /**
     * Overrides <code>Component.paramString()</code> to change the
     * functionality of <code>toString</code>.
     * 
     * @return a string representation of this component's state
     */
    protected String paramString()
    {
        String str = super.paramString();

        // Simply add new info with a preceding ','

        // Interaction State
        str += ",state=";
        int state = getInteractionState();
        if (state == NORMAL_STATE)
            str += "NORMAL";
        else
        {
            String or = "";

            if ((state & DISABLED_STATE_BIT) != 0)
            {
                str += "DISABLED";
                or = "|";
            }
            if ((state & ACTIONED_STATE_BIT) != 0)
            {
                str += or + "ACTIONED";
                or = "|";
            }
            if ((state & FOCUSED_STATE_BIT) != 0)
            {
                str += or + "FOCUSED";
            }
        }

        // Alignment?
        // Resize Mode?
        // Background Mode?
        // Default Size?
        // TLM?
        // Frame position?
        // Play mode?
        // Repeat count?

        // Look
        HLook look;
        if ((look = getLook()) != null) str += ",look=" + look;

        // Animation Data?
        if (animationData != null) str += ",animation";

        // Graphic Data?
        if (graphicData != null) str += ",graphic";

        // String Data?

        return str;
    }

    /*  ************************* HAnimateEffect ************************* */

    // Description copied from HAnimateEffect
    public void start()
    {
        getAnimManager().start(context = new Context());
    }

    // Description copied from HAnimateEffect
    public void stop()
    {
        getAnimManager().stop(context);
    }

    // Description copied from HAnimateEffect
    public boolean isAnimated()
    {
        return getAnimManager().isAnimated(context);
    }

    // Description copied from HAnimateEffect
    public void setPosition(int position)
    {
        if (position < 0) position = 0;

        int oldPosition = getPosition();

        if (oldPosition != position)
        {
            for (int i = 0; i < NSTATES; ++i)
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
            notifyLook(ANIMATION_POSITION_CHANGE, new Integer(oldPosition));
        }
    }

    // Description copied from HAnimateEffect
    public int getPosition()
    {
        return getPosition(getInteractionState());
    }

    // Description copied from HAnimateEffect
    public void setRepeatCount(int count) throws IllegalArgumentException
    {
        if (count <= 0 && count != REPEAT_INFINITE)
            throw new IllegalArgumentException("Invalid repeat count of " + count);

        // Effectively reset current repeat count to 0
        boolean restart = isAnimated();
        stop();

        int oldCount = count;
        repeatCount = count;

        // Notify look of change
        notifyLook(REPEAT_COUNT_CHANGE, new Integer(oldCount));

        // Use new repeat count if was playing
        if (restart) start();
    }

    // Description copied from HAnimateEffect
    public int getRepeatCount()
    {
        return repeatCount;
    }

    /**
     * Sets the delay between the presentation of successive pieces of content
     * (frames).
     * <p>
     * After calling {@link #setDelay(int) setDelay} on a currently playing
     * {@link HAnimateEffect}, there is no guarantee that one or more frames
     * will not be displayed using the previous delay until the new delay value
     * takes effect.
     * 
     * @param count
     *            the content presentation delay in units of 0.042 seconds
     *            duration. If count is less than one "unit", then it shall be
     *            treated as if it were a delay of one "unit", i.e. 0.042
     *            seconds.
     */
    public void setDelay(int count)
    {
        if (count < 1) count = 1;
        delay = count;
    }

    /**
     * Gets the presentation delay for this {@link HAnimateEffect}.
     * <p>
     * The default delay is 3.
     * 
     * @return the presentation delay in units of 0.042 seconds.
     */
    public int getDelay()
    {
        return delay;
    }

    // Description copied from HAnimateEffect
    public void setPlayMode(int mode)
    {
        switch (mode)
        {
            case PLAY_REPEATING:
            case PLAY_ALTERNATING:
                playMode = mode;
                break;
            default:
                throw new IllegalArgumentException("Illegal playMode " + mode);
        }
    }

    // Description copied from HAnimateEffect
    public int getPlayMode()
    {
        return playMode;
    }

    /*  ************************ HasAnimationData ************************ */

    // Description copied from HasAnimationData
    public AnimationData[] getAnimationData()
    {
        AnimationData[] tmp = new AnimationData[NSTATES];

        if (animationData != null) System.arraycopy(animationData, 0, tmp, 0, NSTATES);

        return tmp;
    }

    // Description copied from HasAnimationData
    public void setAnimationData(AnimationData[] data)
    {
        if (data == null || data.length != NSTATES)
            throw new IllegalArgumentException("AnimationData[] must be of length " + NSTATES);

        // Stop and reset animation if currently playing
        boolean restart = reset();

        if (animationData == null) animationData = new AnimationData[NSTATES];
        System.arraycopy(data, 0, animationData, 0, NSTATES);

        // Reset animation
        if (restart) start();

        unknownChange();
    }

    // Description copied from HasAnimationData
    public AnimationData getAnimationData(int state)
    {
        validState(state);
        return (animationData == null) ? null : animationData[state & ~NORMAL_STATE];
    }

    // Description copied from HasAnimationData
    public void setAnimationData(int state, AnimationData data)
    {
        if (animationData == null) animationData = new AnimationData[NSTATES];

        // Stop and reset animation if currently playing
        boolean restart = reset();

        switch (state)
        {
            case NORMAL_STATE:
            case FOCUSED_STATE:
            case ACTIONED_STATE:
            case ACTIONED_FOCUSED_STATE:
            case DISABLED_STATE:
            case DISABLED_FOCUSED_STATE:
            case DISABLED_ACTIONED_STATE:
            case DISABLED_ACTIONED_FOCUSED_STATE:
                animationData[state & ~NORMAL_STATE] = data;
                break;
            case ALL_STATES:
                for (int i = 0; i < NSTATES; ++i)
                    animationData[i] = data;
                break;
            default:
                throw new IllegalArgumentException("See API documentation");
        }

        // Reset animation
        if (restart) start();

        unknownChange();
    }

    /**
     * Resets the animation without notifying the looks about the change. Stops
     * the current animation and resets the current position to zero. Returns a
     * boolean specifying whether the animation was currently playing or not.
     * 
     * @return <code>true</code> if the animation was playing
     */
    private boolean reset()
    {
        boolean isAnim = isAnimated();
        stop();
        for (int i = 0; i < NSTATES; ++i)
            position[i] = 0;
        return isAnim;
    }

    /*  ************************ HasGraphicData ************************ */

    // Description copied from HasGraphicData
    public GraphicData[] getGraphicData()
    {
        GraphicData[] tmp = new GraphicData[NSTATES];

        if (graphicData != null) System.arraycopy(graphicData, 0, tmp, 0, NSTATES);

        return tmp;
    }

    // Description copied from HasGraphicData
    public void setGraphicData(GraphicData[] data)
    {
        if (data == null || data.length != NSTATES)
            throw new IllegalArgumentException("GraphicData[] must be of length " + NSTATES);

        if (graphicData == null) graphicData = new GraphicData[NSTATES];
        System.arraycopy(data, 0, graphicData, 0, NSTATES);

        unknownChange();
    }

    // Description copied from HasGraphicData
    public GraphicData getGraphicData(int state)
    {
        validState(state);
        return (graphicData == null) ? null : graphicData[state & ~NORMAL_STATE];
    }

    // Description copied from HasGraphicData
    public void setGraphicData(int state, GraphicData data)
    {
        if (graphicData == null) graphicData = new GraphicData[NSTATES];

        switch (state)
        {
            case NORMAL_STATE:
            case FOCUSED_STATE:
            case ACTIONED_STATE:
            case ACTIONED_FOCUSED_STATE:
            case DISABLED_STATE:
            case DISABLED_FOCUSED_STATE:
            case DISABLED_ACTIONED_STATE:
            case DISABLED_ACTIONED_FOCUSED_STATE:
                graphicData[state & ~NORMAL_STATE] = data;
                break;
            case ALL_STATES:
                for (int i = 0; i < NSTATES; ++i)
                    graphicData[i] = data;
                break;
            default:
                throw new IllegalArgumentException("See API documentation");
        }
        unknownChange();
    }

    /*  ************************ HasStringData ************************ */

    // Description copied from HasStringData
    public String[] getStringData()
    {
        String[] tmp = new String[NSTATES];

        for (int i = 0; i < NSTATES; ++i)
            tmp[i] = getTextContent(i | NORMAL_STATE);

        return tmp;
    }

    // Description copied from HasStringData
    public void setStringData(String[] data)
    {
        if (data == null || data.length != NSTATES)
            throw new IllegalArgumentException("String[] must be of length " + NSTATES);

        for (int i = 0; i < NSTATES; ++i)
            setTextContent(data[i], i | NORMAL_STATE);
    }

    // Description copied from HasStringData
    public String getStringData(int i)
    {
        return getTextContent(i);
    }

    // Description copied from HasStringData
    public void setStringData(int i, String data)
    {
        setTextContent(data, i);
    }

    /*  ************************ Other ************************ */

    /**
     * Overrides {@link HVisible#setDefaultSize(java.awt.Dimension)} to accept
     * <code>null</code> as a default size to be treated as
     * {@link #NO_DEFAULT_SIZE}.
     * 
     * @param size
     *            specifies the default preferred size
     * 
     * @see HVisible#setDefaultSize(java.awt.Dimension)
     */
    public void setDefaultSize(java.awt.Dimension size)
    {
        super.setDefaultSize((size != null) ? size : NO_DEFAULT_SIZE);
    }

    /**
     * Returns the current frame rate in (approximate) frames-per-second. If the
     * current {@link #getDelay() delay} cannot be expressed in
     * frames-per-second, 0 will be returned.
     * <p>
     * The approximate frame rate is calculated by rounding to the nearest whole
     * integer the number of frames to be displayed in 1000 milliseconds. This
     * is calculated using the following code:
     * 
     * <pre>
     * int delay = 42 * getDelay();
     * return (1000 + delay / 2) / delay;
     * </pre>
     * <p>
     * The default frame rate is 8 frames-per-second.
     * 
     * @return the current frame rate
     * 
     * @see #getDelay()
     */
    public int getFrameRate()
    {
        int delay = getDelay() - 1;
        return (delay < fpsValues.length) ? fpsValues[delay] : 0;
    }

    /**
     * Sets the {@link #setDelay(int) delay} count to most closely match the
     * given frame rate in seconds.
     * 
     * Accepted frame rates include:
     * <ul>
     * <li>24
     * <li>12
     * <li>8
     * <li>6
     * <li>5
     * <li>4
     * <li>3
     * <li>2
     * <li>1
     * </ul>
     * No other rates will be accepted; they will be ignored.
     * 
     * @param fps
     *            the new framerate
     * 
     * @see #setDelay(int)
     * @see #getValidFrameRates()
     */
    public void setFrameRate(int fps)
    {
        for (int i = 0; i < fpsValues.length; ++i)
        {
            if (fps == fpsValues[i])
            {
                setDelay(i + 1);
                break;
            }
        }
    }

    /**
     * Return the set of valid frame rates.
     * 
     * @return the set of valid frame rates
     */
    public static int[] getValidFrameRates()
    {
        return new int[] { 24, 12, 8, 6, 5, 4, 3, 2, 1 };
    }

    /**
     * Notifies the current <code>HLook</code>, if there is one, of the
     * specified change.
     * 
     * Does not allocate an <code>HChangeData</code> or
     * <code>HChangeData[]</code> if there is no <code>HLook</code> associated
     * with this component.
     * 
     * @param hint
     *            the change of which the look should be made aware
     * @param data
     *            the data associated with the change
     * 
     * @see HLook#widgetChanged(HVisible,HChangeData[])
     */
    protected void notifyLook(int hint, Object data)
    {
        HLook look = getLook();
        if (look != null) look.widgetChanged(this, new HChangeData[] { new HChangeData(hint, data) });
    }

    /**
     * Notifies the associated look of an <i>unknown</i> change to the
     * component.
     */
    protected void unknownChange()
    {
        notifyLook(UNKNOWN_CHANGE, new Integer(UNKNOWN_CHANGE));
    }

    /**
     * Checks the state to determine that it falls within a valid range.
     */
    private void validState(int state)
    {
        switch (state)
        {
            case NORMAL_STATE:
            case FOCUSED_STATE:
            case ACTIONED_STATE:
            case ACTIONED_FOCUSED_STATE:
            case DISABLED_STATE:
            case DISABLED_FOCUSED_STATE:
            case DISABLED_ACTIONED_STATE:
            case DISABLED_ACTIONED_FOCUSED_STATE:
                return;
            default:
                throw new IllegalArgumentException("See API documentation");
        }
    }

    /**
     * Returns the static <code>AnimationManager</code> singleton.
     */
    protected synchronized static AnimationManager getAnimManager()
    {
        if (animMgr == null) animMgr = new SingleThread(DELAY_UNIT);
        return animMgr;
    }

    /**
     * Returns the current position for the current state.
     */
    private int getPosition(int state)
    {
        return position[state & ~NORMAL_STATE];
    }

    /**
     * Gets the length of the given state's content.
     */
    private int getLength(int state)
    {
        AnimationData anim = getDefaultContent(state);

        return (anim != null) ? anim.getLength() : 0;
    }

    /**
     * Gets the content for the given state. If no content exists, attempts to
     * retrieve the nearest appropriate content. This is what
     * <code>HAnimateLook</code> will do when displaying content.
     */
    private AnimationData getDefaultContent(int state)
    {
        AnimationData animate = getAnimationData(state);
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
        private int countdown = repeatCount;

        // Comments copied from AnimationManager.AnimationContext
        public Object getAnimation()
        {
            return Label.this;
        }

        // Comments copied from AnimationManager.AnimationContext
        public boolean advancePosition()
        {
            return 0 == (countdown = Label.this.advancePosition(countdown));
        }

        // Comments copied from AnimationManager.AnimationContext
        public int getDelay()
        {
            return Label.this.getDelay();
        }

        // Comments copied from AnimationManager.AnimationContext
        public boolean isAnimated()
        {
            return context == this;
        }
    }

    /*  ************************ Attributes ************************ */

    /**
     * State-dependent <code>GraphicData</code> content. Replacement for
     * {@link org.havi.ui.HVisible#getGraphicContent(int) graphic} content.
     */
    private GraphicData[] graphicData;

    /**
     * State-dependent <code>AnimationData</code> content. Replacement for
     * {@link org.havi.ui.HVisible#getAnimateContent(int) animation} content.
     */
    private AnimationData[] animationData;

    /**
     * The number of state indices.
     */
    private static final int NSTATES = 8;

    /**
     * The set of valid fps values.
     */
    private static final int[] fpsValues = { 24, 12, 8, 6, 5, 4, 3, 3, 3, 2, 2, 2, 2, 2, 2, 1 };

    /**
     * The delay unit for animation playback. 42ms is chosen (as opposed to
     * 100ms) because it will allow us playback speeds of ~24, 12, and 8 fps. We
     * would really like 41 and 2/3 ms...
     */
    private static final int DELAY_UNIT = 42; /* ms */

    /**
     * Indicates the current direction (forwards or backwards) for
     * {@link #PLAY_ALTERNATING PLAY_ALTERNATING} mode.
     * <ul>
     * <li><code>1</code> - forward
     * <li><code>-1</code> - backward
     * </ul>
     */
    private int direction = 1;

    /** Indicates the current animation position. */
    private int position[] = new int[NSTATES];

    /** Indicates how often to repeat the animation */
    private int repeatCount = REPEAT_INFINITE;

    /** Indicates the delay in 0.042 second units between position advances. */
    private int delay = 3;

    /** Indicates the current playback mode. */
    private int playMode = PLAY_REPEATING;

    /**
     * The current AnimationContext.
     */
    private AnimationContext context;

    /**
     * The <code>AnimationManager</code> used to control the frame advancement.
     */
    private static AnimationManager animMgr;

    /**
     * The <i>standard</i> look used by newly created components.
     */
    private static final HLook standardLook;

    /**
     * Static block.
     */
    static
    {
        // Initialize the standardLook
        HLook look;

        int[] states = { FOCUSED_STATE, ACTIONED_FOCUSED_STATE };
        look = new OutlineDecorator(null, states, java.awt.Color.yellow);
        look = new TextDecorator(look);
        look = new AnimateDataDecorator(look);
        look = new GraphicDataDecorator(look);
        look = new FillDecorator(look, new int[] { ALL_STATES }, FillDecorator.RECT);
        standardLook = look;
    }
}
