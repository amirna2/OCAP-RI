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
import org.havi.ui.HAnimateEffectTest.EffectPlaybackTester;

/**
 * Tests {@link #HFlatEffectMatte}.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.7 $, $Date: 2002/06/03 21:32:13 $
 */
public class HFlatEffectMatteTest extends AbstractMatteTest implements EffectPlaybackTester
{
    /** True if mattes are supported on this platform */
    boolean matteSupported;

    /**
     * Standard constructor.
     */
    public HFlatEffectMatteTest(String s)
    {
        super(s);
        matteSupported = TestSupport.getProperty("snap2.havi.test.flatEffectMatteSupported", true);
    }

    /**
     * Parameterized test constructor.
     */
    public HFlatEffectMatteTest(String s, Object params)
    {
        super(s, params);
        matteSupported = TestSupport.getProperty("snap2.havi.test.flatEffectMatteSupported", true);
    }

    /**
     * Standalone runner. This one is never called. Subclasses should duplicate
     * this one EXACTLY.
     */
    public static void main(String args[])
    {
        try
        {
            junit.textui.TestRunner.run(suite(HFlatEffectMatteTest.class));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private final float data[] = { 0.0f, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f };

    /**
     * Setup.
     */
    public void setUp() throws Exception
    {
        super.setUp();
        hmatte = new HFlatEffectMatte();
    }

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>implements HMatte
     * <li>implements HAnimateEffect
     * </ul>
     */
    public void testAncestry()
    {
        super.testAncestry();
        TestUtils.testImplements(hmatte.getClass(), HAnimateEffect.class);
    }

    /**
     * Test the 2 constructors of HFlatEffectMatte.
     * <ul>
     * <li>HFlatEffectMatte()
     * <li>HFlatEffectMatte(float data[])
     * </ul>
     */
    public void testConstructors()
    {
        checkConstructor("HFlatEffectMatte()", (HFlatEffectMatte) hmatte, null);
        checkConstructor("HFlatEffectMatte(float[])", new HFlatEffectMatte(data), data);
    }

    /**
     * Check for proper initialization of constructor variables.
     */
    private void checkConstructor(String msg, HFlatEffectMatte matte, float data[])
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", matte);
        assertSame(msg + " matte data not initialized correctly", data, matte.getMatteData());

        // Check variables not exposed in constructors
        assertEquals(msg + " effect matte position not initialized correctly", 0, matte.getPosition());
        assertTrue(msg + " effect matte should be stopped by default", !matte.isAnimated());
        assertEquals(msg + " repeatCount not initialized correctly", HAnimateEffect.REPEAT_INFINITE,
                matte.getRepeatCount());
        assertEquals(msg + " delay not initialized correctly", 1, matte.getDelay());
        assertEquals(msg + " playback mode not initialized correctly", HAnimateEffect.PLAY_REPEATING,
                matte.getPlayMode());
    }

    /**
     * Tests getMatteData/setMatteData
     * <ul>
     * <li>The set matte should be the retreived matte
     * <li>Matte data of null should be treated as a flat matte with 1.0f (not
     * tested here).
     * </ul>
     */
    public void testMatteData()
    {
        HFlatEffectMatte matte = (HFlatEffectMatte) hmatte;
        float ar1[] = new float[5];
        float ar2[] = new float[0];
        matte.setMatteData(ar1);
        assertEquals("Set matte data should be retrieved data", ar1, matte.getMatteData());
        matte.setMatteData(ar2);
        assertEquals("Set matte data should be retrieved data", ar2, matte.getMatteData());
        matte.setMatteData(null);
        assertEquals("Set matte data should be retrieved data", null, matte.getMatteData());
    }

    /**
     * Tests proper operation of matte with an HContainer.
     * <ul>
     * <li>GROUPED means that the HContainer's background (if there is one) as
     * well as the sub-components are affected by the matte.
     * <li>UNGROUPED means that only HContainer's background (there isn't one by
     * default) is affected by the matte.
     * </ul>
     */
    public void testContainer() throws Exception
    {
        // Do not run this test if mattes are not supported on this platform.
        if (!matteSupported) return;

        // Try different starting positions
        // Ungrouped
        doTestContainer(180, 180, 180, 180, // main bg image color
                180, 0, 180, 180, // container bg image color
                180, 180, 0, 180, // component bg image color
                180, 180, 180, 0, // component bg image color
                data, 3, data, 3, data, 10, // data array and position for
                                            // container/component/component
                false); // grouped or ungrouped
        // Try different starting positions
        // Grouped
        doTestContainer(180, 180, 180, 180, 180, 0, 180, 180, 180, 180, 0, 180, 180, 180, 180, 0, data, 3, data, 10,
                data, 10, true);
        // Try different starting positions
        // Ungrouped
        doTestContainer(180, 180, 180, 180, 180, 0, 180, 180, 180, 180, 0, 180, 180, 180, 180, 0, data, 3, data, 3,
                data, 10, false);
        // Try different starting positions
        // Grouped
        doTestContainer(180, 180, 180, 180, 180, 0, 180, 180, 180, 180, 0, 180, 180, 180, 180, 0, data, 3, data, 10,
                data, 10, true);
    }

    /**
     * Tests proper operation of matte with an HComponent.
     */
    public void testComponent() throws Exception
    {
        // Do not run this test if mattes are not supported on this platform.
        if (!matteSupported) return;

        doTestComponent(180, 255, 255, 0, // main background image color
                180, 255, 0, 255, // component bg image color
                data, 0); // component matte array and pos
        doTestComponent(180, 255, 255, 0, 180, 255, 0, 255, data, 3);
        doTestComponent(180, 255, 255, 0, 180, 255, 0, 255, data, 10);
        doTestComponent(180, 255, 255, 0, 255, 255, 0, 255, data, 0);
        doTestComponent(180, 255, 255, 0, 255, 255, 0, 255, data, 3);
        doTestComponent(180, 255, 255, 0, 255, 255, 0, 255, data, 10);
    }

    /**
     * Internal mapping to the more generic doTestContainer method. Implicitly
     * generates the HFlatEffectMatte and a AnimFraction parameters.
     * 
     * @param mainA
     *            main background image color alpha component
     * @param mainR
     *            main background image color red component
     * @param mainG
     *            main background image color blue component
     * @param mainB
     *            main background image color green component
     * @param bgA
     *            container background image color alpha component
     * @param bgR
     *            container background image color red component
     * @param bgG
     *            container background image color blue component
     * @param bgB
     *            container background image color green component
     * @param fg1A
     *            component #1 background image color alpha component
     * @param fg1R
     *            component #1 background image color red component
     * @param fg1G
     *            component #1 background image color blue component
     * @param fg1B
     *            component #1 background image color green component
     * @param fg2A
     *            component #2 background image color alpha component
     * @param fg2R
     *            component #2 background image color red component
     * @param fg2G
     *            component #2 background image color blue component
     * @param fg2B
     *            component #2 background image color green component
     * @param xA
     *            extra alpha component array for container matte
     * @param pos
     *            position in array
     * @param x1A
     *            extra alpha component array for component #1 matte
     * @param pos1
     *            position in array
     * @param x2A
     *            extra alpha component array for component #2 matte
     * @param pos2
     *            position in array
     * @param grouped
     *            whether the container is grouped or not
     */
    private void doTestContainer(int mainA, int mainR, int mainG, int mainB, int bgA, int bgR, int bgG, int bgB,
            int fg1A, int fg1R, int fg1G, int fg1B, int fg2A, int fg2R, int fg2G, int fg2B, float xA[], int pos,
            float x1A[], int pos1, float x2A[], int pos2, boolean grouped) throws Exception
    {
        // Create the mattes (if necessary)
        // Ensure the position is set as expected
        HFlatEffectMatte matte, matte1 = null, matte2 = null;
        matte = new HFlatEffectMatte(xA);
        matte.setPosition(pos);
        if (x1A != null)
        {
            matte1 = new HFlatEffectMatte(x1A);
            matte1.setPosition(pos1);
        }
        if (x2A != null)
        {
            matte2 = new HFlatEffectMatte(x2A);
            matte2.setPosition(pos2);
        }
        // Use generic doTestContainer() in HFlatMatteTest
        doTestContainer(mainA, mainR, mainG, mainB, bgA, bgR, bgG, bgB, fg1A, fg1R, fg1G, fg1B, fg2A, fg2R, fg2G, fg2B,
                new AnimFraction(xA, pos), (x1A == null) ? NoAlpha : new AnimFraction(x1A, pos1),
                (x2A == null) ? NoAlpha : new AnimFraction(x2A, pos2), matte, matte1, matte2, grouped, SRC_OVER);
    }

    /**
     * Internal mapping to the more generic doTestComponent method. Implicitly
     * generates the HFlatEffectMatte and a AnimFraction parameters.
     * 
     * @param dA
     *            background image alpha
     * @param dR
     *            background image red
     * @param dG
     *            background image green
     * @param dB
     *            background blue
     * @param sA
     *            component bg image alpha
     * @param sR
     *            component bg image red
     * @param sG
     *            component bg image green
     * @param sB
     *            component bg image blue
     * @param xA
     *            extra alpha component array
     * @param position
     *            position within alpha component array
     */
    private void doTestComponent(int dA, int dR, int dG, int dB, int sA, int sR, int sG, int sB, float xA[],
            int position) throws Exception
    {
        // Create the matte
        // Ensure that the position is set as expected
        HFlatEffectMatte matte = new HFlatEffectMatte(xA);
        matte.setPosition(position);
        // Use generic doTestContainer() in HFlatMatteTest
        doTestComponent(dA, dR, dG, dB, sA, sR, sG, sB, new AnimFraction(xA, position), matte, SRC_OVER);
    }

    /**
     * Test that the HAnimateEffect is stopped by default
     */
    public void testStopped()
    {
        // Do not run this test if mattes are not supported on this platform.
        if (!matteSupported) return;

        HFlatEffectMatte matte = (HFlatEffectMatte) hmatte;
        matte.setMatteData(data);
        HAnimateEffectTest.testStopped(matte);
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
        // Do not run this test if mattes are not supported on this platform.
        if (!matteSupported) return;

        HFlatEffectMatte matte = (HFlatEffectMatte) hmatte;
        matte.setMatteData(data);
        HAnimateEffectTest.testDelay(matte);
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
        // Do not run this test if mattes are not supported on this platform.
        if (!matteSupported) return;

        HFlatEffectMatte matte = (HFlatEffectMatte) hmatte;
        matte.setMatteData(data);
        HAnimateEffectTest.testPlayMode(matte);
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
        // Do not run this test if mattes are not supported on this platform.
        if (!matteSupported) return;

        HFlatEffectMatte matte = (HFlatEffectMatte) hmatte;
        matte.setMatteData(data);
        HAnimateEffectTest.testPosition(matte, data.length);
    }

    /**
     * Test setRepeatCount/getRepeatCount
     * <ul>
     * <li>Check the default repeat count (PLAY_INFINITE)
     * <li>Ensure that the set repeate is the retreived repeat
     * <li>Ensure that invalid repeat counts result in a repeat count of 1
     * </ul>
     */
    public void testRepeatCount()
    {
        // Do not run this test if mattes are not supported on this platform.
        if (!matteSupported) return;

        HFlatEffectMatte matte = (HFlatEffectMatte) hmatte;
        matte.setMatteData(data);
        HAnimateEffectTest.testRepeatCount(matte);
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
        if (!matteSupported) return;

        HFlatEffectMatte matte = (HFlatEffectMatte) hmatte;
        matte.setMatteData(data);
        HAnimateEffectTest.testRepeatCountWhilePlaying(matte, data.length);
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
        // Do not run this test if mattes are not supported on this platform.
        if (!matteSupported) return;

        HFlatEffectMatte matte = (HFlatEffectMatte) hmatte;
        matte.setMatteData(data);
        HAnimateEffectTest.testAnimated(matte);
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
    public void testDoRepeat() throws Exception
    {
        // Do not run this test if mattes are not supported on this platform.
        if (!matteSupported) return;

        HFlatEffectMatte matte = (HFlatEffectMatte) hmatte;
        matte.setMatteData(data);
        HAnimateEffectTest.testDoRepeat(matte, this, data.length);
    }

    /**
     * Test the correct playback given the two playback modes: repeating and
     * alternating.
     */
    public void testDoPlayMode() throws Exception
    {
        // Do not run this test if mattes are not supported on this platform.
        if (!matteSupported) return;

        HFlatEffectMatte matte = (HFlatEffectMatte) hmatte;
        matte.setMatteData(data);
        HAnimateEffectTest.testDoPlayMode(matte, this, data.length);
    }

    /**
     * Tests that playback starts in the current position, whatever it may be.
     */
    public void testPositionPlay() throws Exception
    {
        // Do not run this test if mattes are not supported on this platform.
        if (!matteSupported) return;

        HFlatEffectMatte matte = (HFlatEffectMatte) hmatte;
        matte.setMatteData(data);
        HAnimateEffectTest.testPositionPlay(matte, this, data.length);
    }

    /**
     * Test that multiple successive starts does not have any unexpected side
     * effects.
     * <ul>
     * <li>Multiple calls to start should have no effect.
     * </ul>
     */
    public void testMultipleStart() throws Exception
    {
        // Do not run this test if mattes are not supported on this platform.
        if (!matteSupported) return;

        HFlatEffectMatte matte = (HFlatEffectMatte) hmatte;
        matte.setMatteData(data);
        HAnimateEffectTest.testMultipleStart(matte, this, data.length);
    }

    /**
     * Manages temporally-dependent alpha float values.
     */
    private static class AnimFraction implements Fraction
    {
        private float data[];

        private int position;

        public AnimFraction(float data[], int position)
        {
            this.data = data;
            this.position = position;
        }

        public void setPosition(int pos)
        {
            position = pos;
        }

        public int getPosition()
        {
            return position;
        }

        public float getFraction(int x, int y)
        {
            return (data == null) ? 1.0f : data[position];
        }
    }
}
