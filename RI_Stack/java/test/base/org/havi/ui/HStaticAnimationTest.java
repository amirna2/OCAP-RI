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

package org.havi.ui;

import org.cablelabs.test.*;
import java.awt.*;
import java.util.Vector;

import org.havi.ui.HAnimateEffectTest.EffectPlaybackTester;
import org.havi.ui.HAnimateEffectTest.TimePosition;
import org.havi.ui.HAnimateEffectTest.TestPlayback;

/**
 * Tests {@link #HStaticAnimation}.
 * 
 * @author Aaron Kamienski
 * @author Tony Hoffman
 * @version $Id: HStaticAnimationTest.java,v 1.16 2002/11/07 21:14:09 aaronk Exp
 *          $
 */
public class HStaticAnimationTest extends HVisibleTest implements EffectPlaybackTester
{
    /**
     * Standard constructor.
     */
    public HStaticAnimationTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HStaticAnimationTest.class);
        System.exit(0);
    }

    /**
     * Creates a look of the appropriate type. Should be overridden by
     * subclasses.
     */
    protected HLook createLook()
    {
        return new HAnimateLook()
        {
        };
    }

    /**
     * The tested component.
     */
    protected HStaticAnimation hstaticanimation;

    /**
     * Should be overridden to create subclass of HStaticAnimation.
     * 
     * @return the instance of HStaticAnimation to test
     */
    protected HStaticAnimation createHStaticAnimation()
    {
        return new HStaticAnimation();
    }

    /**
     * Overridden to create an HStaticAnimation.
     * 
     * @return the instance of HVisible to test
     */
    protected HVisible createHVisible()
    {
        return (hstaticanimation = createHStaticAnimation());
    }

    /**
     * Array of images to use for animations.
     */
    protected Image images[];

    /**
     * Setup.
     */
    public void setUp() throws Exception
    {
        super.setUp();
        images = new Image[10];
    }

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends HVisible
     * <li>implements HAnimateEffect
     * <li>implements HNoInputPreferred
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HStaticAnimation.class, HVisible.class);
        TestUtils.testImplements(HStaticAnimation.class, HNoInputPreferred.class);
        HAnimateEffectTest.testAncestry(HStaticAnimation.class);
    }

    /**
     * Test the 3 constructors of HStaticAnimation.
     * <ul>
     * <li>HStaticAnimation()
     * <li>HStaticAnimation(Image img[], int delay, int mode, int repeat)
     * <li>HStaticAnimation(Image img[], int delay, int mode, int repeat, int x,
     * int y, int w, int h)
     * </ul>
     */
    public void testConstructors()
    {
        checkClass(HStaticAnimationTest.class);

        checkConstructor("HStaticAnimation()", hstaticanimation, null, 1, HStaticAnimation.PLAY_REPEATING,
                HStaticAnimation.REPEAT_INFINITE, 0, 0, 0, 0, false);
        checkConstructor("HStaticAnimation(Image img[], " + "int delay, int mode, int repeat", new HStaticAnimation(
                images, 3, HStaticAnimation.PLAY_ALTERNATING, 7), images, 3, HStaticAnimation.PLAY_ALTERNATING, 7, 0,
                0, 0, 0, false);
        checkConstructor("HStaticAnimation(Image img[], " + "int delay, int mode, int repeat, "
                + "int x, int y, int w, int h)", new HStaticAnimation(images, 0, HStaticAnimation.PLAY_REPEATING, 17,
                100, 101, 102, 103), images, 1, HStaticAnimation.PLAY_REPEATING, 17, 100, 101, 102, 103, true);
    }

    /**
     * Check for proper initialization of constructor variables.
     */
    public void checkConstructor(String msg, final HStaticAnimation a, Image img[], int delay, int mode, int repeat,
            int x, int y, int w, int h, boolean defaultSize)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", a);
        assertEquals(msg + " x-coordinated not initialized correctly", x, a.getLocation().x);
        assertEquals(msg + " y-coordinated not initialized correctly", y, a.getLocation().y);
        assertEquals(msg + " width not initialized correctly", w, a.getSize().width);
        assertEquals(msg + " height not initialized correctly", h, a.getSize().height);
        assertEquals(msg + " delay not initialized correctly", delay, a.getDelay());
        assertEquals(msg + " repeat count not initialized correctly", repeat, a.getRepeatCount());
        assertEquals(msg + " playback mode not initialized correctly", mode, a.getPlayMode());
        assertSame(msg + " animation Images not initialized correctly", img, a.getAnimateContent(HState.NORMAL_STATE));
        foreachState(new Callback()
        {
            public void callback(int state)
            {
                if (state != NORMAL_STATE)
                    assertNull(stateToString(state) + " content should not be set", a.getAnimateContent(state));
            }
        });

        // Check variables NOT exposed in constructors
        assertEquals(msg + " should be NORMAL_STATE", NORMAL_STATE, a.getInteractionState());
        assertNull(msg + " matte should be unassigned", a.getMatte());
        assertNotNull(msg + " text layout mgr should be assigned", a.getTextLayoutManager());
        assertEquals(msg + " bg mode not initialized incorrectly", a.getBackgroundMode(), a.NO_BACKGROUND_FILL);
        if (!defaultSize)
            // assertNull(msg+" default size should not be set",
            // a.getDefaultSize());
            assertEquals(msg + " default size should not be set", a.NO_DEFAULT_SIZE, a.getDefaultSize());
        else
            assertEquals(msg + " default size initialized incorrectly", a.getDefaultSize(), new Dimension(w, h));
        assertEquals(msg + " horiz alignment initialized incorrectly", a.getHorizontalAlignment(), a.HALIGN_CENTER);
        assertEquals(msg + " vert alignment initialized incorrectly", a.getVerticalAlignment(), a.VALIGN_CENTER);
        assertEquals(msg + " resize mode initialized incorrectly", a.getResizeMode(), a.RESIZE_NONE);
        assertSame(msg + " default look not used", a.getDefaultLook(), a.getLook());
        assertEquals(msg + " initial position not correct", 0, a.getPosition());
        assertTrue(msg + " should be stopped by default", !a.isAnimated());
        assertEquals(msg + " border mode not initialized correctly", true, a.getBordersEnabled());
    }

    /**
     * Tests for unexpected added fields, non-unique fields
     */
    public void testFields()
    {
        TestUtils.testNoAddedFields(HStaticAnimation.class, null);
        // This actually tests HAnimateEffect... it doesn't have
        // it's own tests.
        TestUtils.testNoAddedFields(HAnimateEffect.class, fields);
        TestUtils.testUniqueFields(HAnimateEffect.class, fields, false, 0, 3);
    }

    private static final String fields[] = { "PLAY_ALTERNATING", "PLAY_REPEATING", "REPEAT_INFINITE" };

    /**
     * Creates an instance of HStaticAnimation which pushes TimeStart objects
     * onto the given startStop Vector when start (true) or stop (false) is
     * called.
     * <p>
     * Should be overridden by subclasses.
     */
    protected HStaticAnimation createSpecialAnimation(final Vector startStop)
    {
        checkClass(HStaticAnimationTest.class);

        return new HStaticAnimation()
        {
            public void start()
            {
                if (!isAnimated())
                {
                    long time = System.currentTimeMillis();
                    startStop.addElement(new TimeStart(true, time));
                }
                super.start();
            }

            public void stop()
            {
                if (isAnimated())
                {
                    long time = System.currentTimeMillis();
                    startStop.addElement(new TimeStart(false, time));
                }
                super.stop();
            }
        };
    }

    /**
     * Tests setAnimateContent() to make sure that it functions properly when
     * the animation is currently playing.
     * 
     * It should:
     * <ol>
     * <li>The method must be synchronized with any implementation-specific
     * animation thread such that content cannot be changed while a different
     * thread is using it.
     * 
     * <li>If the animation was running the method should stop the animation in
     * a synchronized manner before changing content.
     * 
     * <li>The method should reset the animation to a starting position defined
     * by the current play mode. The repeat count of the animation should be
     * reset to 0.
     * 
     * <li>If the animation was running the method should start the animation.
     * </ol>
     */
    public void testAnimateContentWhilePlaying() throws Exception
    {
        Image[] newImages = new Image[12];
        final int count = 4;

        final Vector startStop = new Vector();
        HStaticAnimation anim = createSpecialAnimation(startStop);
        anim.setAnimateContent(images, HState.ALL_STATES);
        anim.setRepeatCount(count);

        try
        {
            // Start animation, clear the start entry
            anim.start();
            startStop.removeAllElements();

            // Let run for approximately 1 cycle
            TestSupport.delay(images.length * HAnimateEffectTest.DELAY);

            // Set new content
            assertTrue("Did not expect any start/stops at this point", startStop.size() == 0);
            assertTrue("Animation expected to be playing before content change", anim.isAnimated());
            anim.setAnimateContent(newImages, HState.ALL_STATES);
            assertTrue("Animation should still be playing after content change", anim.isAnimated());

            // Make sure that still playing after initial repeat time fades
            TestSupport.delay(images.length * HAnimateEffectTest.DELAY * (count - 1));
            assertTrue("Animation should have reset repeat count after content change", anim.isAnimated());

            // Let run to completion
            TestSupport.delay(newImages.length * HAnimateEffectTest.DELAY + (newImages.length - images.length)
                    * HAnimateEffectTest.DELAY * (count - 1));
            assertTrue("Animation should stop itself", !anim.isAnimated());

            // Should see a stop and a start in startStop
            assertTrue("Expected at least a stop/start as result of content change", startStop.size() >= 2);
            assertTrue("Expected a stop in response to content change", !((TimeStart) startStop.elementAt(0)).start);
            assertTrue("Expected a restart in response to content change", ((TimeStart) startStop.elementAt(1)).start);
        }
        finally
        {
            anim.stop();
        }
    }

    /**
     * Test that the HAnimateEffect is stopped by default
     */
    public void testStopped()
    {
        HAnimateEffectTest.testStopped(hstaticanimation);
    }

    /**
     * Test setDelay/getDelay
     * <ul>
     * <li>Check the default delay (if applicable)
     * <li>Ensure that the set delay is the retreived delay
     * <li>Ensure that a delay of < 1 results in a delay of 1
     * </ul>
     */
    public void testDelay()
    {
        HAnimateEffectTest.testDelay(hstaticanimation);
    }

    /**
     * Test setPlayMode/getPlayMode
     * <ul>
     * <li>Check the default play mode (PLAY_REPEATING)
     * <li>Ensure that the set play mode is the retreived play mode
     * <li>Ensure that invalid play modes aren't accepted
     * </ul>
     */
    public void testPlayMode()
    {
        HAnimateEffectTest.testPlayMode(hstaticanimation);
    }

    /**
     * Test setPosition/getPosition.
     * <ul>
     * <li>Check the default position (0)
     * <li>Ensure that the set position is the retreived position
     * <li>Ensure that a position of < 0 or >= length results in a position of 0
     * or length-1 respectively
     * </ul>
     */
    public void testPosition()
    {
        hstaticanimation.setAnimateContent(images, HState.ALL_STATES);
        HAnimateEffectTest.testPosition(hstaticanimation, images.length);
    }

    /**
     * Test setRepeatCount/getRepeatCount
     * <ul>
     * <li>Check the default repeat count (PLAY_INFINITE)
     * <li>Ensure that the set repeat is the retreived repeat
     * <li>Ensure that invalid repeat counts result in a repeat count of 1
     * </ul>
     */
    public void testRepeatCount()
    {
        HAnimateEffectTest.testRepeatCount(hstaticanimation);
    }

    /**
     * Tests setRepeatCount(). If set during playback, should:
     * <ol>
     * <li>change the repeatCount value
     * <li>reset the current number of repeats to 0 (i.e., stop/restart)
     * </ol>
     */
    public void testRepeatCountWhilePlaying()
    {
        hstaticanimation.setAnimateContent(images, HState.ALL_STATES);
        HAnimateEffectTest.testRepeatCountWhilePlaying(hstaticanimation, images.length);
    }

    /**
     * Test isAnimated()
     * <ul>
     * <li>Should be false when stopped
     * <li>Should be true when started
     * </ul>
     */
    public void testAnimated()
    {
        HAnimateEffectTest.testAnimated(hstaticanimation);
    }

    /**
     * Test the correct delay between frames.
     * 
     * Check that total time from playback is AT LEAST what is expected. It
     * should also be less than it would be if the delay were incremented by
     * one.
     */
    public void testDoDelay()
    {
        hstaticanimation.setAnimateContent(images, HState.ALL_STATES);
        HAnimateEffectTest.testDoDelay(hstaticanimation, this, images.length);
    }

    /**
     * Test the correct playback given various repeat counts (all using the
     * PLAY_REPEATING playback mode). Note that it cannot be guaranteed that
     * every position gets painted, or that each position is painted only once,
     * or that the time delay is accurate.
     * <ul>
     * <li>repeat == 1
     * <li>repeat == 3
     * <li>repeat == infinite
     * </ul>
     */
    public void testDoRepeat()
    {
        hstaticanimation.setAnimateContent(images, HState.ALL_STATES);
        HAnimateEffectTest.testDoRepeat(hstaticanimation, this, images.length);
    }

    /**
     * Test the correct playback given the two playback modes: repeating and
     * alternating.
     */
    public void testDoPlayMode()
    {
        hstaticanimation.setAnimateContent(images, HState.ALL_STATES);
        HAnimateEffectTest.testDoPlayMode(hstaticanimation, this, images.length);
    }

    /**
     * Tests that playback starts in the current position, whatever it may be.
     */
    public void testPositionPlay()
    {
        hstaticanimation.setAnimateContent(images, HState.ALL_STATES);
        HAnimateEffectTest.testPositionPlay(hstaticanimation, this, images.length);
    }

    /**
     * Test that multiple successive starts does not have any unexpected side
     * effects.
     * <ul>
     * <li>Multiple calls to start should have no effect.
     * </ul>
     */
    public void testMultipleStart()
    {
        hstaticanimation.setAnimateContent(images, HState.ALL_STATES);
        HAnimateEffectTest.testMultipleStart(hstaticanimation, this, images.length);
    }

    /**
     * Test setDefaultLook/getDefaultLook.
     * <ul>
     * <li>Only HAnimateLook should be accepted.
     * <li>The set look should be the retreived look.
     * <li>newly created HStaticAnimations should use the new default look
     * </ul>
     */
    public void testDefaultLook() throws HInvalidLookException
    {
        checkClass(HStaticAnimationTest.class);

        assertSame("Default look should be used", HStaticAnimation.getDefaultLook(), (new HStaticAnimation()).getLook());

        HAnimateLook save = HStaticAnimation.getDefaultLook();

        try
        {
            HAnimateLook look;

            HStaticAnimation.setDefaultLook(look = new HAnimateLook());
            assertSame("Incorrect look retrieved", look, HStaticAnimation.getDefaultLook());
            assertSame("Default look should be used", look, (new HStaticAnimation()).getLook());

            /*
             * // Not possible, because signature doesn't allow it try {
             * HStaticAnimation.setDefaultLook(new HVisibleTest.EmptyLook());
             * fail("Invalid look accepted"); } catch(HInvalidLookException
             * ignored) {}
             */
        }
        finally
        {
            // reset
            HStaticAnimation.setDefaultLook(save);
        }
    }

    /**
     * Should setup and run the given test.
     * <ol>
     * 
     * <li>Set the animation data on the animation
     * 
     * <li>Call <code>test.setup(a)</code>
     * 
     * <li>Record the current time as:
     * 
     * <pre>
     * start = System.currentTimeMillis();
     * </pre>
     * 
     * <li>Call <code>test.play(a)</code>
     * 
     * <li>Call <code>test.stop(a)</code>
     * 
     * <li>Record the current time as:
     * 
     * <pre>
     * stop = System.currentTimeMillis();
     * </pre>
     * 
     * <li>Check for correct playback by calling:
     * 
     * <pre>
     * test.check(a, positions, start, stop)
     * </pre>
     * 
     * Where positions is a <code>Vector</code> of <code>TimePosition</code>
     * objects.
     * 
     * <li>Note that positions will only be recorded for
     * ANIMATION_POSITION_CHANGE HChangeData hints.
     * 
     * </ol>
     */
    public void doTestPlayback(HAnimateEffect anim, TestPlayback test)
    {
        HStaticAnimation a = (HStaticAnimation) anim;
        PlaybackLook look = new PlaybackLook(a);

        TestSupport.setLook(a, look);

        long start, stop;

        try
        {
            // setup test
            test.setup(a);
            // push play
            look.playing = true;
            start = System.currentTimeMillis();
            test.play(a);
            // stop after a period of time
            test.stop(a);
            stop = System.currentTimeMillis();
            look.playing = false;
            // check correct playback
            test.check(a, look.positions, start, stop);
        }
        finally
        {
            // don't let play forever
            a.stop();
        }
    }

    /**
     * Tests generation of HLook.widgetChanged() calls in response to position
     * and repeat count changes:
     * <ul>
     * <li>ANIMATION_POSITION_CHANGE - setPosition()
     * </ul>
     */
    public void testWidgetChangedPosition() throws HInvalidLookException
    {
        HStaticAnimation sa = hstaticanimation;
        sa.setAnimateContent(images, HState.ALL_STATES);
        sa.setPosition(0);
        sa.setRepeatCount(1);

        final String hintName = "ANIMATION_POSITION_CHANGE";
        final HChangeData[] hcd = new HChangeData[1];
        createWidgetChangeLook(sa, HVisible.ANIMATION_POSITION_CHANGE, hintName, hcd);

        // setPosition
        hcd[0] = null;
        sa.setPosition(1);
        assertNotNull("WidgetChanged should've been called for position change", hcd[0]);
        assertEquals("ANIMATION_POSITION_CHANGE expected", HVisible.ANIMATION_POSITION_CHANGE, hcd[0].hint);
        assertTrue("Non-null Integer data expected", hcd[0].data != null && hcd[0].data instanceof Integer);
        assertEquals("Old position expected in HChangeData", 0, ((Integer) hcd[0].data).intValue());
        // Redundant
        // (Cannot really require this, but we will for now...)
        hcd[0] = null;
        sa.setPosition(1);
        assertNull("No widgetChanged was expected for redundant setPosition", hcd[0]);
    }

    /**
     * Tests generation of HLook.widgetChanged() calls in response to position
     * and repeat count changes:
     * <ul>
     * <li>REPEAT_COUNT_CHANGE - setRepeatCount()
     * </ul>
     */
    public void testWidgetChangedRepeat() throws HInvalidLookException
    {
        HStaticAnimation sa = hstaticanimation;
        sa.setAnimateContent(images, HState.ALL_STATES);
        sa.setPosition(0);
        sa.setRepeatCount(1);

        final String hintName = "REPEAT_COUNT_CHANGE";
        final HChangeData[] hcd = new HChangeData[1];
        createWidgetChangeLook(sa, HVisible.REPEAT_COUNT_CHANGE, hintName, hcd);

        // setRepeatCount
        hcd[0] = null;
        sa.setRepeatCount(99);
        assertNotNull("WidgetChanged should've been called for position change", hcd[0]);
        assertEquals("REPEAT_COUNT_CHANGE expected", HVisible.REPEAT_COUNT_CHANGE, hcd[0].hint);
        assertTrue("Non-null Integer data expected", hcd[0].data != null && hcd[0].data instanceof Integer);
        assertEquals("Old repeatCount expected in HChangeData", 1, ((Integer) hcd[0].data).intValue());
        hcd[0] = null;
        sa.setRepeatCount(HAnimateEffect.REPEAT_INFINITE);
        assertNotNull("WidgetChanged should've been called for position change", hcd[0]);
        assertEquals("REPEAT_COUNT_CHANGE expected", HVisible.REPEAT_COUNT_CHANGE, hcd[0].hint);
        assertTrue("Non-null Integer data expected", hcd[0].data != null && hcd[0].data instanceof Integer);
        assertEquals("Old repeatCount expected in HChangeData", 99, ((Integer) hcd[0].data).intValue());
        // No test for redundant because the spec implies that
        // the side-effect of starting a started animation always
        // occurs.
    }

    /**
     * Simple HLook which is used to test for appropriate playback. Tracks
     * positions and current time in a vector.
     */
    public static class PlaybackLook extends EmptyLook
    {
        public java.util.Vector positions = new java.util.Vector();

        public boolean playing = false;

        public HAnimateEffect a;

        public PlaybackLook(HAnimateEffect a)
        {
            this.a = a;
        }

        public void widgetChanged(HVisible v, HChangeData[] data)
        {
            // Should only care about animation advancement...
            if (playing && data != null)
            {
                for (int i = 0; i < data.length; ++i)
                {
                    if (data[i].hint == HVisible.ANIMATION_POSITION_CHANGE)
                    {
                        long time = System.currentTimeMillis();
                        if (DEBUG) System.out.println(a.getPosition() + " @ " + time);
                        positions.add(new TimePosition(a.getPosition(), time));
                    }
                }
            }
        }
    }

    /**
     * @see #createSpecialAnimation(Vector)
     */
    protected class TimeStart
    {
        public boolean start;

        public long time;

        public TimeStart(boolean start, long ms)
        {
            this.start = start;
            time = ms;
        }
    }

    private static final boolean DEBUG = false;
}
