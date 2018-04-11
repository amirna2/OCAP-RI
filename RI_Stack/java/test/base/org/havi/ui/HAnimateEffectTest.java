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

/**
 * Test framework required for HAnimateEffect tests.
 * 
 * @author Aaron Kamienski
 */
public abstract class HAnimateEffectTest extends TestSupport
{
    /**
     * Test for correct ancestry.
     * <ul>
     * <li>implements HAnimateEffect
     * </ul>
     */
    public static void testAncestry(Class testClass)
    {
        TestUtils.testImplements(testClass, HAnimateEffect.class);
    }

    /**
     * Test that the HAnimateEffect is stopped by default
     */
    public static void testStopped(HAnimateEffect a)
    {
        assertTrue("Should be stopped by default", !a.isAnimated());
    }

    /**
     * Test setDelay/getDelay
     * <ul>
     * <li>Check the default delay (if applicable)
     * <li>Ensure that the set delay is the retreived delay
     * <li>Ensure that a delay of < 1 results in a delay of 1
     * </ul>
     */
    public static void testDelay(HAnimateEffect a)
    {
        assertEquals("The default delay should be 1", 1, a.getDelay());
        a.setDelay(99);
        assertEquals("Set delay should be retrieved delay", 99, a.getDelay());
        a.setDelay(0);
        assertEquals("A set delay of 0 should map to 1", 1, a.getDelay());
        a.setDelay(-99);
        assertEquals("A set delay of < 0 should map to 1", 1, a.getDelay());
    }

    /**
     * Test setPlayMode/getPlayMode
     * <ul>
     * <li>Check the default play mode (PLAY_REPEATING)
     * <li>Ensure that the set play mode is the retreived play mode
     * <li>Ensure that invalid play modes aren't accepted
     * </ul>
     */
    public static void testPlayMode(HAnimateEffect a)
    {
        assertEquals("The default play mode is incorrect", HAnimateEffect.PLAY_REPEATING, a.getPlayMode());
        a.setPlayMode(HAnimateEffect.PLAY_ALTERNATING);
        assertEquals("Set play mode should be retrieved mode", HAnimateEffect.PLAY_ALTERNATING, a.getPlayMode());
        try
        {
            a.setPlayMode(HAnimateEffect.PLAY_REPEATING * 2 + HAnimateEffect.PLAY_ALTERNATING * 2);
            assertEquals("Invalid play modes should not be accepted", HAnimateEffect.PLAY_ALTERNATING, a.getPlayMode());
        }
        catch (IllegalArgumentException expected)
        {
        }
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
    public static void testPosition(HAnimateEffect a, int length)
    {
        assertEquals("The default position is incorrect", 0, a.getPosition());
        a.setPosition(length / 2);
        assertEquals("The set position should be the retrieved position", length / 2, a.getPosition());
        a.setPosition(-1);
        assertEquals("Position of <0 should be mapped to 0", 0, a.getPosition());
        a.setPosition(length);
        assertEquals("Position of >=length should be mapped to length-1", length - 1, a.getPosition());
    }

    /**
     * Test setRepeatCount/getRepeatCount
     * <ul>
     * <li>Check the default repeat count (PLAY_INFINITE)
     * <li>Ensure that the set repeate is the retreived repeat
     * <li>Ensure that invalid repeat counts either throw exceptions or do not
     * effect any changes
     * </ul>
     */
    public static void testRepeatCount(HAnimateEffect a)
    {
        assertEquals("The default repeat count is incorrect", HStaticAnimation.REPEAT_INFINITE, a.getRepeatCount());
        a.setRepeatCount(13);
        assertEquals("The set repeat count should be the retrieved count", 13, a.getRepeatCount());

        try
        {
            a.setRepeatCount(0);
            assertEquals("Invalid repeat counts should be ignored " + "(or throw an exception)", 13, a.getRepeatCount());
        }
        catch (IllegalArgumentException okay)
        {
        }

        try
        {
            a.setRepeatCount(-13);
            assertEquals("Invalid repeat counts should be ignored " + "(or throw an exception)", 13, a.getRepeatCount());
        }
        catch (IllegalArgumentException okay)
        {
        }
    }

    /**
     * Tests setRepeatCount(). If set during playback, should:
     * <ol>
     * <li>change the repeatCount value
     * <li>reset the current number of repeats to 0 (i.e., stop/restart)
     * </ol>
     */
    public static void testRepeatCountWhilePlaying(HAnimateEffect a, int length, int DELAY)
    {
        try
        {
            // Start playback initially
            a.setRepeatCount(HAnimateEffect.REPEAT_INFINITE);
            a.setDelay(1);
            a.start();
            delay(100);
            assertTrue("Animation should be playing now", a.isAnimated());

            // Change repeat count
            a.setRepeatCount(2);
            delay(length * DELAY); // rest for 1 cycle
            assertTrue("Animation expected to be playing after repeat change", a.isAnimated());
            // Let animation run for appropriate time
            delay(length * DELAY);
            // Expect it to be finished
            assertTrue("Animation should have reset repeat count after change - "
                    + "should have stopped after 2 cycles", !a.isAnimated());
        }
        finally
        {
            a.stop();
        }
    }

    public static void testRepeatCountWhilePlaying(HAnimateEffect a, int length)
    {
        testRepeatCountWhilePlaying(a, length, DELAY);
    }

    /**
     * Test isAnimated()
     * <ul>
     * <li>Should be false when stopped
     * <li>Should be true when started
     * </ul>
     */
    public static void testAnimated(HAnimateEffect a)
    {
        a.setRepeatCount(HAnimateEffect.REPEAT_INFINITE);
        a.stop();
        assertTrue("isAnimated should be false when stopped", !a.isAnimated());
        try
        {
            a.start();
            delay(100);
            assertTrue("isAnimated should be true when playing", a.isAnimated());
            a.stop();
            assertTrue("isAnimated should be false when stopped (after playing)", !a.isAnimated());
        }
        finally
        {
            a.stop();
        }
    }

    /**
     * Test the correct delay between frames.
     * 
     * Check that total time from playback is AT LEAST what is expected. It
     * should also be less than it would be if the delay were incremented by
     * one.
     */
    public static void testDoDelay(HAnimateEffect a, EffectPlaybackTester tester, final int length, final int DELAY,
            final int DELAY_ACTUAL)
    {
        final int FRAME_DELAY = 3;
        tester.doTestPlayback(a, new TestPlayback()
        {
            public void setup(HAnimateEffect a)
            {
                a.setPosition(0);
                a.setRepeatCount(1);
                a.setDelay(FRAME_DELAY);
            }

            public void stop(HAnimateEffect a)
            {
                delay(length * DELAY * FRAME_DELAY);
                a.stop();
            }

            public void check(HAnimateEffect a, java.util.Vector pos, long start, long stop)
            {
                // Find position 1 (first ACTUAL advancement)
                int pos1;
                for (pos1 = 0; ((TimePosition) pos.elementAt(pos1)).position != 1; ++pos1);

                final int time = (pos.size() - 1 - pos1) * DELAY_ACTUAL;
                final int expected = time * FRAME_DELAY;
                final int toobig = time * (FRAME_DELAY + 1);

                start = ((TimePosition) pos.elementAt(pos1)).time;
                stop = ((TimePosition) pos.elementAt(pos.size() - 1)).time;

                assertTrue("Total playback time should be >= " + expected + "ms, was " + (stop - start),
                        (stop - start) >= expected);
                assertTrue("Total playback time should be < " + toobig + "ms, was " + (stop - start),
                        (stop - start) < toobig);
            }
        });
    }

    public static void testDoDelay(HAnimateEffect a, EffectPlaybackTester tester, final int length)
    {
        testDoDelay(a, tester, length, DELAY, DELAY_ACTUAL);
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
    public static void testDoRepeat(HAnimateEffect a, EffectPlaybackTester tester, final int length, final int DELAY)
    {
        // Test repeat of 1
        tester.doTestPlayback(a, new TestPlayback()
        {
            public void setup(HAnimateEffect a)
            {
                a.setDelay(1);
                a.setPosition(0);
                a.setRepeatCount(1);
            }

            public void stop(HAnimateEffect a)
            {
                delay(length * DELAY);
                a.stop();
            }

            public void check(HAnimateEffect a, java.util.Vector pos, long start, long stop)
            {
                int size = pos.size();
                int prev = 0;
                for (int i = 0; i < size; ++i)
                {
                    TimePosition tb = (TimePosition) pos.elementAt(i);
                    assertTrue("Positions should move forward [" + i + "]" + "(" + tb.position + "," + prev + ")",
                            tb.position >= prev);
                    prev = tb.position;
                }
                assertEquals("Final position should've been the last image", length - 1,
                        ((TimePosition) pos.elementAt(size - 1)).position);
            }
        });
        // Test repeat of 3
        tester.doTestPlayback(a, new TestPlayback()
        {
            public void setup(HAnimateEffect a)
            {
                a.setPosition(0);
                a.setRepeatCount(3);
            }

            public void stop(HAnimateEffect a)
            {
                delay(3 * length * DELAY);
                a.stop();
            }

            public void check(HAnimateEffect a, java.util.Vector pos, long start, long stop)
            {
                int size = pos.size();
                int prev = 0;
                for (int i = 0; i < size; ++i)
                {
                    TimePosition tb = (TimePosition) pos.elementAt(i);
                    assertTrue("Positions should move forward", (tb.position >= prev) || // move
                                                                                         // forward
                            ((prev == length - 1) && // or wrap around
                            (tb.position == 0)));
                    prev = tb.position;
                }
                assertEquals("Final position should've been the last image", length - 1,
                        ((TimePosition) pos.elementAt(size - 1)).position);
            }
        });
        // Test infinite repeat
        tester.doTestPlayback(a, new TestPlayback()
        {
            public void setup(HAnimateEffect a)
            {
                a.setPosition(0);
                a.setRepeatCount(a.REPEAT_INFINITE);
            }

            public void stop(HAnimateEffect a)
            {
                delay(2 * length * DELAY);
            }

            public void check(HAnimateEffect a, java.util.Vector pos, long start, long stop)
            {
                assertTrue("Animation should play ad infinitum", a.isAnimated());
                assertTrue("Animation should play ad infinitum", a.isAnimated());
                a.stop();
                assertTrue("Animation should play ad infinitum", !a.isAnimated());
            }
        });
    }

    public static void testDoRepeat(HAnimateEffect a, EffectPlaybackTester tester, final int length)
    {
        testDoRepeat(a, tester, length, DELAY);
    }

    /**
     * Test the correct playback given the two playback modes: repeating and
     * alternating.
     */
    public static void testDoPlayMode(HAnimateEffect a, EffectPlaybackTester tester, final int length, final int DELAY)
    {
        final boolean[] alternating = new boolean[1];
        TestPlayback tb = new TestPlayback()
        {
            public void setup(HAnimateEffect a)
            {
                if (alternating[0])
                    a.setPlayMode(a.PLAY_ALTERNATING);
                else
                    a.setPlayMode(a.PLAY_REPEATING);
                a.setPosition(0);
                a.setRepeatCount(2);
            }

            public void stop(HAnimateEffect a)
            {
                delay(2 * length * DELAY);
                a.stop();
            }

            public void check(HAnimateEffect a, java.util.Vector pos, long start, long stop)
            {
                int size = pos.size();
                int prev = 0;
                for (int i = 0; i < size; ++i)
                {
                    TimePosition tb = (TimePosition) pos.elementAt(i);
                    if (prev > tb.position) // Found repeat!
                    {
                        if (alternating[0])
                            assertTrue("Alternating mode should have decremented, not reset to zero", tb.position != 0);
                        else
                            assertEquals("Repeating mode should have reset to 0", 0, tb.position);
                    }
                }
            }
        };

        // Test forward and backward
        alternating[0] = false;
        tester.doTestPlayback(a, tb);
        alternating[0] = true;
        tester.doTestPlayback(a, tb);
    }

    public static void testDoPlayMode(HAnimateEffect a, EffectPlaybackTester tester, final int length)
    {
        testDoPlayMode(a, tester, length, DELAY);
    }

    /**
     * Tests that playback starts in the current position, whatever it may be.
     */
    public static void testPositionPlay(HAnimateEffect a, EffectPlaybackTester tester, final int length, final int DELAY)
    {
        tester.doTestPlayback(a, new TestPlayback()
        {
            public void setup(HAnimateEffect a)
            {
                a.setPosition(3);
                a.setRepeatCount(1);
            }

            public void stop(HAnimateEffect a)
            {
                delay(length * DELAY);
                a.stop();
            }

            public void check(HAnimateEffect a, java.util.Vector pos, long start, long stop)
            {
                TimePosition tb = (TimePosition) pos.elementAt(0);
                assertTrue("The first position should NOT be 0", tb.position != 0);
                assertTrue("The first position should be >= 3", tb.position >= 3);
            }
        });
    }

    public static void testPositionPlay(HAnimateEffect a, EffectPlaybackTester tester, final int length)
    {
        testPositionPlay(a, tester, length, DELAY);
    }

    /**
     * Test that multiple successive starts does not have any unexpected side
     * effects.
     * <ul>
     * <li>Multiple calls to start should have no effect.
     * </ul>
     */
    public static void testMultipleStart(HAnimateEffect a, EffectPlaybackTester tester, final int length)
    {
        tester.doTestPlayback(a, new TestPlayback()
        {
            public void setup(HAnimateEffect a)
            {
            }

            public void play(HAnimateEffect a)
            {
                for (int i = 0; i < 100; ++i)
                    a.start();
            }

            public void stop(HAnimateEffect a)
            {
                delay(100);
            }

            public void check(HAnimateEffect a, java.util.Vector pos, long start, long stop)
            {
                try
                {
                    assertEquals("After multiple successive starts" + " (w/out stops)"
                            + " the animation should be playing", true, a.isAnimated());
                }
                finally
                {
                    a.stop();
                }
            }
        });
    }

    /**
     * Interface to (usually) be implemented by the callers of methods that take
     * EffectPlaybackTesters.
     */
    public interface EffectPlaybackTester
    {
        /**
         * Should setup and run the given test.
         * <ol>
         * <li>Set the animation data on the animation
         * <li>Call <code>test.setup(a)</code>
         * <li>Record the current time as:
         * 
         * <pre>
         * start = System.currentTimeMillis();
         * </pre>
         * 
         * <li>Call <code>test.play(a)</code>
         * <li>Call <code>test.stop(a)</code>
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
         * </ol>
         */
        public void doTestPlayback(HAnimateEffect a, TestPlayback test);
    }

    /**
     * Simple <i>strategy</i> class used to test for correct playback.
     */
    public abstract static class TestPlayback
    {
        public abstract void setup(HAnimateEffect a);

        public void play(HAnimateEffect a)
        {
            a.start();
        }

        public void stop(HAnimateEffect a)
        {
            a.stop();
        }

        public abstract void check(HAnimateEffect a, java.util.Vector pos, long start, long stop);
    }

    /** Records the time and position of an animation. */
    public static class TimePosition
    {
        int position;

        long time;

        public TimePosition(int pos, long ms)
        {
            position = pos;
            time = ms;
        }
    }

    /**
     * Approximately one unit of effect animation time.
     */
    public static final int DELAY = 105;

    /**
     * Actual one unit of effect animation time.
     */
    public static final int DELAY_ACTUAL = 100;
}
