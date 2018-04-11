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

import java.awt.*;

/**
 * Tests {@link #HImageMatte}.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.6 $, $Date: 2002/06/03 21:32:15 $
 */
public class HImageMatteTest extends AbstractMatteTest
{
    /** True if mattes are supported on this platform */
    boolean matteSupported;

    /**
     * Standard constructor.
     */
    public HImageMatteTest(String s)
    {
        super(s);
        matteSupported = TestSupport.getProperty("snap2.havi.test.imageMatteSupported", true);
    }

    /**
     * Parameterized test constructor.
     */
    public HImageMatteTest(String s, Object params)
    {
        super(s, params);
        matteSupported = TestSupport.getProperty("snap2.havi.test.imageMatteSupported", true);
    }

    /**
     * Standalone runner. This one is never called. Subclasses should duplicate
     * this one EXACTLY.
     */
    public static void main(String args[])
    {
        try
        {
            junit.textui.TestRunner.run(suite(HImageMatteTest.class));
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
        hmatte = new HImageMatte();
    }

    /**
     * Test the 2 constructors of HImageMatte.
     * <ul>
     * <li>HImageMatte()
     * <li>HImageMatte(Image data)
     * </ul>
     */
    public void testConstructors()
    {
        Image img = new HVisibleTest.EmptyImage();
        checkConstructor("HImageMatte()", (HImageMatte) hmatte, null);
        checkConstructor("HImageMatte(Image)", new HImageMatte(img), img);
    }

    /**
     * Check for proper initialization of constructor variables.
     */
    private void checkConstructor(String msg, HImageMatte matte, Image data)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", matte);
        assertSame(msg + " matte data not initialized correctly", data, matte.getMatteData());

        // Check variables not exposed in constructors
        assertEquals(msg + " image matte offset not initialized correctly", new Point(0, 0), matte.getOffset());
    }

    /**
     * Tests getMatteData/setMatteData
     * <ul>
     * <li>The set matte should be the retreived matte
     * </ul>
     */
    public void testMatteData()
    {
        HImageMatte matte = (HImageMatte) hmatte;
        Image img1 = new HVisibleTest.EmptyImage();
        Image img2 = new HVisibleTest.EmptyImage();

        matte.setMatteData(img1);
        assertSame("Set matte data should be retrieved data", img1, matte.getMatteData());
        matte.setMatteData(img2);
        assertSame("Set matte data should be retrieved data", img2, matte.getMatteData());

        matte.setMatteData(null);
        assertNull("Null matte data should be allowed", matte.getMatteData());
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

        // Create a fancy Image to use as a matte
        int imgP[] = createAlphaPixels(MED, MED);
        Image img = createSimpleImage(MED, MED, imgP);
        Point ofs = new Point(0, 0);
        doTestContainer(180, 180, 180, 180, // main background image color
                180, 0, 180, 180, // container bg image color
                180, 180, 0, 180, // component bg image color
                180, 180, 180, 0, // component bg image color
                // container image, image pixels, width, and offset
                img, imgP, MED, ofs,
                // component image, image pixels, width, and offset
                img, imgP, MED, ofs,
                // component image, image pixels, width, and offset
                img, imgP, MED, ofs, false); // grouped or ungrouped
        doTestContainer(180, 180, 180, 180, 180, 0, 180, 180, 180, 180, 0, 180, 180, 180, 180, 0, img, imgP, MED, ofs,
                img, imgP, MED, ofs, img, imgP, MED, ofs, true);

        // with (arbitrary) offsets
        ofs = new Point(SML, SML);
        doTestContainer(180, 180, 180, 180, // main background image color
                180, 0, 180, 180, // container bg image color
                180, 180, 0, 180, // component bg image color
                180, 180, 180, 0, // component bg image color
                // container image, image pixels, width, and offset
                img, imgP, MED, ofs,
                // component image, image pixels, width, and offset
                img, imgP, MED, ofs,
                // component image, image pixels, width, and offset
                img, imgP, MED, ofs, false); // grouped or ungrouped
        doTestContainer(180, 180, 180, 180, 180, 0, 180, 180, 180, 180, 0, 180, 180, 180, 180, 0, img, imgP, MED, ofs,
                img, imgP, MED, ofs, img, imgP, MED, ofs, true);
    }

    /**
     * Tests proper operation of matte with an HComponent.
     */
    public void testComponent() throws Exception
    {
        // Do not run this test if mattes are not supported on this platform.
        if (!matteSupported) return;

        // Create a fancy Image to use as a matte
        int imgP[] = createAlphaPixels(MED, MED);
        Image img = createSimpleImage(MED, MED, imgP);
        Point noofs = new Point(0, 0);
        Point ofs = new Point(3 * FACTOR, 3 * FACTOR);

        // Test with and without an Image offset
        // Image sized to component
        runComponentTests(img, imgP, MED, noofs);
        runComponentTests(img, imgP, MED, ofs);

        // Test with and without an Image offset
        // Smaller-sized image
        imgP = createAlphaPixels(SML, SML);
        img = createSimpleImage(SML, SML, imgP);
        runComponentTests(img, imgP, SML, noofs);
        runComponentTests(img, imgP, SML, ofs);

        // Test with and without an Image offset
        // Larger-sized image
        imgP = createAlphaPixels(BIG, BIG);
        img = createSimpleImage(BIG, BIG, imgP);
        runComponentTests(img, imgP, BIG, noofs);
        runComponentTests(img, imgP, BIG, ofs);
    }

    /**
     * Run component tests with the given Image, image pixels (and width), and
     * image offset (within a component).
     */
    private void runComponentTests(Image img, int imgP[], int scan, Point ofs) throws Exception
    {
        doTestComponent(180, 255, 255, 0, 180, 255, 0, 255, img, imgP, scan, ofs);
        doTestComponent(180, 255, 255, 0, 180, 255, 0, 255, null, null, 0, ofs);
        doTestComponent(180, 255, 255, 0, 255, 255, 0, 255, img, imgP, scan, ofs);
        doTestComponent(255, 255, 255, 0, 255, 255, 0, 255, null, null, 0, ofs);
    }

    /**
     * Internal mapping to the more generic doTestContainer method. Implicitly
     * generates the HImageMatte and a ImageFraction parameters.
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
     * @param img
     *            Image with alpha channel for container matte
     * @param pix
     *            pixels for image
     * @param scan
     *            width of image
     * @param ofs
     *            how the Image matte should be offset against container
     * @param img1
     *            Image with alpha channel for component matte
     * @param pix1
     *            pixels for image
     * @param scan1
     *            width of image
     * @param ofs1
     *            how the Image matte should be offset against component
     * @param img2
     *            Image with alpha channel for component matte
     * @param pix2
     *            pixels for image
     * @param scan2
     *            width of image
     * @param ofs2
     *            how the Image matte should be offset against component
     * @param grouped
     *            whether the container is grouped or not
     */
    private void doTestContainer(int mainA, int mainR, int mainG, int mainB, int bgA, int bgR, int bgG, int bgB,
            int fg1A, int fg1R, int fg1G, int fg1B, int fg2A, int fg2R, int fg2G, int fg2B, Image img, int pix[],
            int scan, Point ofs, Image img1, int pix1[], int scan1, Point ofs1, Image img2, int pix2[], int scan2,
            Point ofs2, boolean grouped) throws Exception
    {
        // Create the mattes if necessary.
        // Ensure that position is set correctly.
        HImageMatte matte, matte1 = null, matte2 = null;
        matte = new HImageMatte(img);
        matte.setOffset(ofs);
        if (img1 != null)
        {
            matte1 = new HImageMatte(img1);
            matte1.setOffset(ofs1);
        }
        if (img2 != null)
        {
            matte2 = new HImageMatte(img2);
            matte2.setOffset(ofs2);
        }
        doTestContainer(mainA, mainR, mainG, mainB, bgA, bgR, bgG, bgB, fg1A, fg1R, fg1G, fg1B, fg2A, fg2R, fg2G, fg2B,
                new ImageFraction(pix, scan, ofs), new ImageFraction(pix1, scan1, ofs1), new ImageFraction(pix2, scan2,
                        ofs2), matte, matte1, matte2, grouped, SRC_OVER);
    }

    /**
     * Internal mapping to the more generic doTestComponent method. Implicitly
     * generates the HFlatMatte and a ImageFraction parameters.
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
     * @param img
     *            Image with alpha channel for component matte
     * @param pix
     *            pixels for image
     * @param scan
     *            width of image
     * @param ofs
     *            how the Image matte should be offset against component
     */
    private void doTestComponent(int dA, int dR, int dG, int dB, int sA, int sR, int sG, int sB, Image img, int pix[],
            int scan, Point ofs) throws Exception
    {
        HImageMatte matte = new HImageMatte(img);
        matte.setOffset(ofs);
        doTestComponent(dA, dR, dG, dB, sA, sR, sG, sB, new ImageFraction(pix, scan, ofs), matte, SRC_OVER);
    }

    /**
     * Tests {get|set}Offset().
     * <ul>
     * <li>if set offset is null, a NullPointerException is thrown
     * <li>the set offset should be the retrieved offset
     * <li>the offset should be used to translate the image w.r.t. component
     * </ul>
     */
    public void testOffset()
    {
        // Do not run this test if mattes are not supported on this platform.
        if (!matteSupported) return;

        HImageMatte matte = (HImageMatte) hmatte;
        Point p1 = new Point(0, 15);
        Point p2 = new Point(15, 0);

        matte.setOffset(p1);
        assertEquals("Set offset should be retrieved offset", p1, matte.getOffset());
        matte.setOffset(p2);
        assertEquals("Set offset should be retrieved offset", p2, matte.getOffset());

        try
        {
            matte.setOffset(null);
            fail("Expected a NullPointerException with a null offset");
        }
        catch (NullPointerException expected)
        {
        }
        assertEquals("Setting to null should have no other affect", p2, matte.getOffset());
    }

    /**
     * Manages spatially-dependent alpha float values.
     */
    public static class ImageFraction implements Fraction
    {
        private int pix[];

        private int w, h;

        private Point ofs;

        public ImageFraction(int pix[], int scan, Point ofs)
        {
            this.pix = pix;
            this.w = scan;
            if (pix != null) this.h = pix.length / scan;
            this.ofs = (ofs == null) ? new Point(0, 0) : ofs;
        }

        public float getFraction(int x, int y)
        {
            x -= ofs.x;
            y -= ofs.y;

            if (pix == null || x < 0 || y < 0 || x >= w || y >= h)
                return 1.0f;
            else
            {
                int i = (y * w) + x;
                return ((pix[i] >>> 24) & 0xff) / 255.0f;
            }
        }
    }
}
