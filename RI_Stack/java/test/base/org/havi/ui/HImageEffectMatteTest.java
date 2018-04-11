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
import org.havi.ui.HAnimateEffectTest.EffectPlaybackTester;

/**
 * Tests {@link #HImageEffectMatte}.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.6 $, $Date: 2002/06/03 21:32:15 $
 */
public class HImageEffectMatteTest extends AbstractMatteTest implements EffectPlaybackTester
{
    /** True if mattes are supported on this platform */
    boolean matteSupported;

    /**
     * Standard constructor.
     */
    public HImageEffectMatteTest(String s)
    {
        super(s);
        matteSupported = TestSupport.getProperty("snap2.havi.test.imageEffectMatteSupported", true);
    }

    /**
     * Parameterized test constructor.
     */
    public HImageEffectMatteTest(String s, Object params)
    {
        super(s, params);
        matteSupported = TestSupport.getProperty("snap2.havi.test.imageEffectMatteSupported", true);
    }

    /**
     * Standalone runner. This one is never called. Subclasses should duplicate
     * this one EXACTLY.
     */
    public static void main(String args[])
    {
        try
        {
            junit.textui.TestRunner.run(suite(HImageEffectMatteTest.class));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Setup.
     */
    public void setUp() throws Exception
    {
        super.setUp();
        hmatte = new HImageEffectMatte();
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
     * Test the 2 constructors of HImageEffectMatte.
     * <ul>
     * <li>HImageEffectMatte()
     * <li>HImageEffectMatte(Image data[])
     * </ul>
     */
    public void testConstructors()
    {
        Image data[] = { new HVisibleTest.EmptyImage(), new HVisibleTest.EmptyImage(), new HVisibleTest.EmptyImage(),
                new HVisibleTest.EmptyImage(), new HVisibleTest.EmptyImage(), };
        checkConstructor("HImageEffectMatte()", (HImageEffectMatte) hmatte, null);
        checkConstructor("HImageEffectMatte(Image[])", new HImageEffectMatte(data), data);
    }

    /**
     * Check for proper initialization of constructor variables.
     */
    private void checkConstructor(String msg, HImageEffectMatte matte, Image data[])
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
        if (data != null) for (int i = 0; i < data.length; ++i)
            assertEquals(msg + " image matte offset not initialized correctly", new Point(0, 0), matte.getOffset(i));
    }

    /**
     * Tests getMatteData/setMatteData
     * <ul>
     * <li>The set matte should be the retreived matte
     * <li>Matte data of null should be treated as a flat matte with 1.0f
     * </ul>
     */
    public void testMatteData()
    {
        HImageEffectMatte matte = (HImageEffectMatte) hmatte;
        Image ar1[] = new Image[5];
        Image ar2[] = new Image[0];
        matte.setMatteData(ar1);
        assertEquals("Set matte data should be retrieved data", ar1, matte.getMatteData());
        matte.setMatteData(ar2);
        assertEquals("Set matte data should be retrieved data", ar2, matte.getMatteData());
        matte.setMatteData(null);
        assertEquals("Set matte data should be retrieved data", null, matte.getMatteData());
    }

    /**
     * Tests proper operation of matte with an HContainer.
     */
    public void testContainer() throws Exception
    {
        // Do not run this test if mattes are not supported on this platform.
        if (!matteSupported) return;

        createData();
        Point ofs[] = new Point[] { new Point(3 * FACTOR, 3 * FACTOR), new Point(0, 0),
                new Point(-3 * FACTOR, -3 * FACTOR), };

        doTestContainer(180, 180, 180, 180, 180, 0, 180, 180, 180, 180, 0, 180, 180, 180, 180, 0, data, dataP, dataW,
                ofs, 0, data, dataP, dataW, ofs, 1, data, dataP, dataW, null, 2, false);
        doTestContainer(180, 180, 180, 180, 180, 0, 180, 180, 180, 180, 0, 180, 180, 180, 180, 0, data, dataP, dataW,
                ofs, 0, data, dataP, dataW, ofs, 1, data, dataP, dataW, null, 2, true);
    }

    /**
     * Tests proper operation of matte with an HComponent.
     */
    public void testComponent() throws Exception
    {
        // Do not run this test if mattes are not supported on this platform.
        if (!matteSupported) return;

        createData();
        Point ofs[] = new Point[] { new Point(3 * FACTOR, 3 * FACTOR), new Point(0, 0),
                new Point(-3 * FACTOR, -3 * FACTOR), };

        for (int i = 0; i < dataW.length; ++i)
            runComponentTests(data, dataP, dataW, ofs, i);
    }

    /**
     *
     */
    private void runComponentTests(Image img[], int imgP[][], int scan[], Point ofs[], int position) throws Exception
    {
        doTestComponent(180, 255, 255, 0, 180, 255, 0, 255, img, imgP, scan, ofs, position);
        doTestComponent(180, 255, 255, 0, 180, 255, 0, 255, null, null, null, ofs, position);
        doTestComponent(180, 255, 255, 0, 255, 255, 0, 255, img, imgP, scan, ofs, position);
        doTestComponent(255, 255, 255, 0, 255, 255, 0, 255, null, null, null, ofs, position);
    }

    /**
     * @see #doTestContainer(int,int,int,int, int,int,int,int, int,int,int,int,
     *      int,int,int,int, Fraction,Fraction,Fraction, HMatte,HMatte,HMatte,
     *      boolean)
     */
    private void doTestContainer(int mainA, int mainR, int mainG, int mainB, int bgA, int bgR, int bgG, int bgB,
            int fg1A, int fg1R, int fg1G, int fg1B, int fg2A, int fg2R, int fg2G, int fg2B, Image img[], int pix[][],
            int scan[], Point ofs[], int pos, Image img1[], int pix1[][], int scan1[], Point ofs1[], int pos1,
            Image img2[], int pix2[][], int scan2[], Point ofs2[], int pos2, boolean grouped) throws Exception
    {
        HImageEffectMatte matte, matte1 = null, matte2 = null;
        matte = new HImageEffectMatte(img);
        setOffsets(matte, ofs);
        if (img1 != null)
        {
            matte1 = new HImageEffectMatte(img1);
            setOffsets(matte1, ofs1);
        }
        if (img2 != null)
        {
            matte2 = new HImageEffectMatte(img2);
            setOffsets(matte2, ofs2);
        }
        doTestContainer(mainA, mainR, mainG, mainB, bgA, bgR, bgG, bgB, fg1A, fg1R, fg1G, fg1B, fg2A, fg2R, fg2G, fg2B,
                new AnimFraction(pix, scan, ofs, pos), new AnimFraction(pix1, scan1, ofs1, pos1), new AnimFraction(
                        pix2, scan2, ofs2, pos2), matte, matte1, matte2, grouped, SRC_OVER);
    }

    /**
     * @see #doTestComponent(int,int,int,int, int,int,int,int, Fraction,HMatte)
     */
    private void doTestComponent(int dA, int dR, int dG, int dB, int sA, int sR, int sG, int sB, Image img[],
            int pix[][], int scan[], Point ofs[], int position) throws Exception
    {
        HImageEffectMatte matte = new HImageEffectMatte(img);
        matte.setPosition(position);
        setOffsets(matte, ofs);
        doTestComponent(dA, dR, dG, dB, sA, sR, sG, sB, new AnimFraction(pix, scan, ofs, position), matte, SRC_OVER);
    }

    private final int dataW[] = { SML, MED, BIG, SML, MED, BIG, SML, MED, BIG, };

    private int dataP[][] = new int[dataW.length][];

    private Image data[] = new Image[dataW.length];

    /**
     *
     */
    private void createData()
    {
        for (int i = 0; i < dataW.length; ++i)
        {
            if (i < 3)
            {
                dataP[i] = createAlphaPixels(dataW[i], dataW[i]);
                data[i] = createSimpleImage(dataW[i], dataW[i], dataP[i]);
            }
            else
            {
                dataP[i] = dataP[i % 3];
                data[i] = data[i % 3];
            }
        }
    }

    /**
     *
     */
    private void setOffsets(HImageEffectMatte matte, Point ofs[])
    {
        if (ofs != null) for (int i = 0; i < ofs.length; ++i)
            matte.setOffset(ofs[i], i);
    }

    /**
     * Tests {get|set}Offset().
     * <ul>
     * <li>the set offset should be the retrieved offset
     * <li>the offset should be used to translate the image w.r.t. component
     * </ul>
     */
    public void testOffset()
    {
        // Do not run this test if mattes are not supported on this platform.
        if (!matteSupported) return;

        HImageEffectMatte matte = (HImageEffectMatte) hmatte;
        Point p1;
        Point p2 = new Point(15, 0);
        Image data[] = new Image[] { null, new HVisibleTest.EmptyImage() };

        matte.setMatteData(data);

        try
        {
            for (int i = 0; i < data.length + 1; ++i)
            {
                p1 = new Point(i, i);
                matte.setOffset(p1, i);
                assertEquals("Set offset should be retrieved offset", p1, matte.getOffset(i));
                matte.setOffset(p2, i);
                assertEquals("Set offset should be retrieved offset", p2, matte.getOffset(i));

                try
                {
                    matte.setOffset(null, i);
                    fail("Expected a NullPointerException with a null offset");
                }
                catch (NullPointerException expected)
                {
                }
                assertEquals("Setting to null should have no other affect", p2, matte.getOffset(i));
            }

            matte.setMatteData(data = new Image[20]);
            assertEquals("Unset offsets should default to (0,0)", new Point(0, 0), matte.getOffset(data.length - 1));
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            e.printStackTrace();
            fail("Should not throw exceptions for unassigned offsets, " + "should return (0,0)");
        }
    }

    /**
     * Test that the HAnimateEffect is stopped by default
     */
    public void testStopped()
    {
        // Do not run this test if mattes are not supported on this platform.
        if (!matteSupported) return;

        HImageEffectMatte matte = (HImageEffectMatte) hmatte;
        createData();
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

        HImageEffectMatte matte = (HImageEffectMatte) hmatte;
        createData();
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

        HImageEffectMatte matte = (HImageEffectMatte) hmatte;
        createData();
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

        HImageEffectMatte matte = (HImageEffectMatte) hmatte;
        createData();
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

        HImageEffectMatte matte = (HImageEffectMatte) hmatte;
        createData();
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

        HImageEffectMatte matte = (HImageEffectMatte) hmatte;
        createData();
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

        HImageEffectMatte matte = (HImageEffectMatte) hmatte;
        createData();
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

        HImageEffectMatte matte = (HImageEffectMatte) hmatte;
        createData();
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

        HImageEffectMatte matte = (HImageEffectMatte) hmatte;
        createData();
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

        HImageEffectMatte matte = (HImageEffectMatte) hmatte;
        createData();
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

        HImageEffectMatte matte = (HImageEffectMatte) hmatte;
        createData();
        matte.setMatteData(data);
        HAnimateEffectTest.testMultipleStart(matte, this, data.length);
    }

    private static class AnimFraction implements Fraction
    {
        private int pix[][];

        private int w[], h[];

        private Point ofs[];

        private int position;

        public AnimFraction(int pix[][], int scan[], Point ofs[], int position)
        {
            this.pix = pix;
            if (pix != null)
            {
                if (pix.length != scan.length) throw new RuntimeException("scan[] isn't same size as pix[]");
                this.w = new int[pix.length];
                this.h = new int[pix.length];
                for (int i = 0; i < scan.length; ++i)
                {
                    w[i] = scan[i];
                    h[i] = pix[i].length / scan[i];
                }
            }
            this.ofs = ofs;
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
            if (pix == null || pix[position] == null)
                return 1.0f;
            else
            {
                if (ofs != null && ofs.length > position && ofs[position] != null)
                {
                    x -= ofs[position].x;
                    y -= ofs[position].y;
                }

                if (x < 0 || y < 0 || x >= w[position] || y >= h[position])
                    return 1.0f;
                else
                {
                    int i = (y * w[position]) + x;
                    return ((pix[position][i] >>> 24) & 0xff) / 255.f;
                }
            }
        }
    }
}
