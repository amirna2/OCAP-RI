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

import junit.framework.*;
import org.cablelabs.test.*;

import java.awt.*;
import java.awt.image.*;
import org.havi.ui.HAnimateEffectTest.TimePosition;
import org.havi.ui.HAnimateEffectTest.TestPlayback;

/**
 * Basic testing framework for testing HMattes.
 * 
 * @author Aaron Kamienski
 * @version $Id: AbstractMatteTest.java,v 1.2 2002/06/03 21:32:09 aaronk Exp $
 */
public abstract class AbstractMatteTest extends TestCase
{
    private HScene scene = null;

    private java.awt.Graphics g = null;

    private java.awt.Color bgColor = null;

    /**
     * Standard constructor.
     */
    public AbstractMatteTest(String s)
    {
        super(s);
    }

    /**
     * Parameterized test constructor.
     */
    public AbstractMatteTest(String s, Object params)
    {
        super(s);
        this.params = params;
    }

    /**
     * Standalone runner. This one is never called. Subclasses should duplicate
     * this one EXACTLY.
     */
    public static void main(String args[])
    {
        try
        {
            junit.textui.TestRunner.run(suite(AbstractMatteTest.class));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public HScene getHScene()
    {
        if (scene == null)
        {
            HSceneFactory factory = HSceneFactory.getInstance();
            scene = factory.getDefaultHScene();
            g = scene.getGraphics();
            bgColor = scene.getBackground();
        }
        // repaint the HScene
        scene.setBackground(bgColor);
        scene.setBackgroundMode(HScene.BACKGROUND_FILL);
        scene.paint(g);
        return scene;
    }

    /**
     * Parameter(s) passed to parameterized tests.
     */
    protected Object params;

    /**
     * Defines a TestSuite.
     */
    public static TestSuite suite(Class testClass) throws Exception
    {
        TestSuite suite = TestUtils.suite(testClass);

        // Now add our other tests.
        /*
         * Constructor c = testClass.getConstructor(new Class[] { String.class,
         * // name Object.class // params });
         */

        return suite;
    }

    /**
     * Matte to be tested.
     */
    protected HMatte hmatte;

    /**
     * HMatteLayer that will have the matte assigned to it.
     */
    protected HMatteLayer hmattelayer;

    /**
     * An HGraphicLook which doesn't paint bg or borders.
     */
    private HGraphicLook look = new HGraphicLook()
    {
        public Insets getInsets(HVisible v)
        {
            return new Insets(0, 0, 0, 0);
        }
    };

    /**
     * Setup. The variables <code>hmatte</code> and <code>hmattelayer</code>
     * should be set up here.
     */
    public void setUp() throws Exception
    {
        super.setUp();
    }

    /**
     * Teardown.
     */
    public void tearDown() throws Exception
    {
        if (scene != null)
        {
            scene.dispose();
        }
        if (g != null)
        {
            g.dispose();
        }
        super.tearDown();
    }

    /**
     * This value can be adjusted when debugging failed tests. Increasing FACTOR
     * (e.g., to 10) will increase the image sizes generated.
     */
    static final int FACTOR = 1;

    /**
     * Used to specify the main background image size.
     */
    static final int BIG = 20 * FACTOR;

    /**
     * Used to specify the container image size and the component image size (in
     * the component test).
     */
    static final int MED = 12 * FACTOR;

    /**
     * Used to specify the sub-component image sizes in the container test.
     */
    static final int SML = 8 * FACTOR;

    /**
     * The offset of one component to another in the container test.
     */
    static final int OFS = MED - SML;

    /**
     * Test the HMatte with a Container.
     * <p>
     * This test is composed of a Container (w/ a bg and a matte), two
     * sub-componenents (each with mattes), and a background. The sub-components
     * are meant to overlap each other within the container. The container does
     * not completely cover the background.
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
     *            extra alpha component for container matte
     * @param x1A
     *            extra alpha component for component #1 matte
     * @param x2A
     *            extra alpha component for component #2 matte
     * @param matte
     *            container matte
     * @param matte1
     *            component #1 matte
     * @param matte2
     *            component #2 matte
     * @param grouped
     *            whether the container is grouped or not
     * @param rule
     *            the rule to be used during (manual) image composition
     */
    protected void doTestContainer(int mainA, int mainR, int mainG, int mainB, int bgA, int bgR, int bgG, int bgB,
            int fg1A, int fg1R, int fg1G, int fg1B, int fg2A, int fg2R, int fg2G, int fg2B, Fraction xA, Fraction x1A,
            Fraction x2A, HMatte matte, HMatte matte1, HMatte matte2, boolean grouped, Compositor rule)
            throws Exception
    {
        // First, generate the pixel data to be used for bg images.
        // The pixel data will be used during manual composition/comparison.
        int mainP[] = createSimplePixels(BIG, BIG, mainA, mainR, mainG, mainB);
        int bgP[] = createSimplePixels(MED, MED, bgA, bgR, bgG, bgB);
        int fg1P[] = createSimplePixels(SML, SML, fg1A, fg1R, fg1G, fg1B);
        int fg2P[] = createSimplePixels(SML, SML, fg2A, fg2R, fg2G, fg2B);
        // Next, generate the actual images
        Image mainI = createSimpleImage(BIG, BIG, mainP);
        final Image bgI = createSimpleImage(MED, MED, bgP);
        Image fg1I = createSimpleImage(SML, SML, fg1P);
        Image fg2I = createSimpleImage(SML, SML, fg2P);
        // Next, create Icons that will display the images
        HStaticIcon main = new HStaticIcon(mainI, 0, 0, BIG, BIG);
        HStaticIcon fg1 = new HStaticIcon(fg1I, OFS, OFS, SML, SML);
        HStaticIcon fg2 = new HStaticIcon(fg2I, 0, 0, SML, SML);
        // The container will paint its image directly.
        // This is necessary to test ungrouped container matte functionality.
        HContainer hc = new HContainer(0, 0, MED, MED)
        {
            public void paint(Graphics g)
            {
                g.drawImage(bgI, 0, 0, this);
                super.paint(g);
            }
        };

        // Use a look that doesn't fill background or draw a border
        main.setLook(look);
        fg1.setLook(look);
        fg2.setLook(look);

        // Use the mattes that we were given
        hc.setMatte(matte);
        if (grouped)
            hc.group();
        else
            hc.ungroup();
        fg1.setMatte(matte1);
        fg2.setMatte(matte2);

        // Generate reference composite pixels.
        // These pixels will be compared againts the actual pixels
        // generated by actual image/matte composition.

        // First, combine the two component pixels (using their mattes)
        // The original pixels are of size SML, whereas the composition
        // has the second component offset such that the resulting pixels
        // will be of size MED. So, first copy the background component
        // to a larger set of pixels.
        int fgP[] = new int[MED * MED];
        copyPixels(fgP, MED, MED, fg2P, SML, SML, x2A, new Point(0, 0));
        fgP = compositePixels(fgP, MED, MED, fg1P, SML, SML, x1A, new Point(OFS, OFS), rule);

        int compP[];
        if (grouped)
        {
            // Components(SRC) + Container(DST) -> compP (no matte)
            compP = compositePixels(bgP, MED, MED, fgP, MED, MED, NoAlpha, new Point(0, 0), rule);
            // compP(SRC) + mainBackground(DST) -> compP (container matte)
            compP = compositePixels(mainP, BIG, BIG, compP, MED, MED, xA, new Point(0, 0), rule);
        }
        else
        {
            // Container(SRC) + mainBackground(DST) -> compP (container matte)
            compP = compositePixels(mainP, BIG, BIG, bgP, MED, MED, xA, new Point(0, 0), rule);
            // Components(SRC) + mainBackground(DST) -> compP (no matte)
            compP = compositePixels(compP, BIG, BIG, fgP, MED, MED, NoAlpha, new Point(0, 0), rule);
        }

        // Now, place all components into a root container so that they
        // can be rendered.
        // The overall layout will be like this (ordered from front to
        // back):
        //
        // HContainer c
        // |
        // +--HContainer hc (w/ bg and matte)
        // | |
        // | +--HComponent fg1 (w/ bg and matte)
        // | |
        // | +--HComponent fg2 (w/ bg and matte)
        // |
        // +--HStaticIcon main (w/ bg)

        // Place fg1 and fg2 into hc
        hc.add(fg1);
        hc.add(fg2);
        // Place hc into a container
        HContainer c = new HContainer(0, 0, BIG, BIG);
        c.add(hc);
        c.add(main);

        // printTree(c, "  ");

        // Paint the actual image and compare pixels
        paintAndCompare(c, compP);
    }

    private static void printTree(Component c, String pfx)
    {
        if (c instanceof HMatteLayer)
            System.out.println(pfx + c + " m=" + ((HMatteLayer) c).getMatte());
        else
            System.out.println(pfx + c);
        if (c instanceof Container)
        {
            Container co = (Container) c;
            Component cs[] = co.getComponents();
            String pfx2 = pfx + pfx;
            for (int i = 0; i < cs.length; ++i)
                printTree(cs[i], pfx2);
        }
    }

    /**
     * Test the HMatte with a Component.
     * <p>
     * This test is composed of a Component (w/ a matte) on a background. The
     * Component color is the source color and the HMatte is specified by the
     * extra alpha. The background color is the destination color.
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
     *            extra alpha for the component
     * @param matte
     *            matte for the component
     * @param rule
     *            the rule to be used during (manual) image composition
     */
    protected void doTestComponent(int dA, int dR, int dG, int dB, int sA, int sR, int sG, int sB, Fraction xA,
            HMatte matte, Compositor rule) throws Exception
    {
        // First, generat the pixel data to be used for bg images
        // The pixel data will be used during manual composition/comparison.
        int bgP[] = createSimplePixels(BIG, BIG, dA, dR, dG, dB);
        int fgP[] = createSimplePixels(MED, MED, sA, sR, sG, sB);
        // Next, generate the actual images
        Image bgI = createSimpleImage(BIG, BIG, bgP);
        Image fgI = createSimpleImage(MED, MED, fgP);
        // Next, create Icons that will display the images
        HStaticIcon bg = new HStaticIcon(bgI, 0, 0, BIG, BIG);
        HStaticIcon fg = new HStaticIcon(fgI, 0, 0, MED, MED);

        // Use a look that doesn't fill background or draw a border
        bg.setLook(look);
        fg.setLook(look);

        // Set matte for fg
        fg.setMatte(matte);

        // Generate reference composite pixels.
        // These pixels will be compared againts the actual pixels
        // generated by actual image/matte composition.
        int compP[] = compositePixels(bgP, BIG, BIG, fgP, MED, MED, xA, new Point(0, 0), rule);

        // Now, place the fg and bg components into a root container so that
        // they can be rendered.
        // The overal layout will be like this (ordered from front to
        // back):
        //
        // HContainer hc
        // |
        // +--HComponent fg (w/ bg and matte)
        // |
        // +--HComponent bg (w/ bg)
        //
        HContainer hc = new HContainer(0, 0, BIG, BIG);
        hc.add(fg);
        hc.add(bg);

        // Paint the actual image and compare pixels
        paintAndCompare(hc, compP);
    }

    /**
     * Paint the given component and compare the actual pixels to the given
     * reference pixels. Note that the sizes are assumed to be BIGxBIG.
     * 
     * @param Component
     *            to render
     * @param reference
     *            the reference pixels to compare to
     */

    private void paintAndCompare(Component c, int reference[])
    {
    }

    // ???
    /*
     * private void paintAndCompare(Component c, int reference[]) { // Draw the
     * actual image, get pixels. // The image is drawn to a BufferedImage. This
     * is done // for the following reasons: // 1) Using GUITest.getSnapshot(),
     * which is based on the Robot // class, does not maintain alpha values
     * (they are internal // to Java). No alpha values will be available for
     * comparison. // 2) Cannot simply use Component.createImage() and a
     * PixelGrabber // because Component.createImage() creates an RGB- not
     * ARGB-based // Image. No alpha values will be available for comparison. //
     * This would be the most portable solution, if it worked. // // We might be
     * able to map ARGB -> RGB for a comparison... int screenP[]; BufferedImage
     * screen = null; Graphics g = null; try { screen = new
     * BufferedImage(c.getSize().width, c.getSize().height,
     * BufferedImage.TYPE_INT_ARGB); // paint to the image g =
     * screen.getGraphics(); c.paint(g);
     * 
     * // get the pixels screenP = screen.getRGB(0, 0, BIG, BIG, null, 0, BIG);
     * } finally { if (g != null) g.dispose(); }
     * 
     * // Compare the actual pixels with the reference pixels
     * assertNotNull("Could not create screen composite pixels", screenP); try {
     * checkPixels(reference, screenP, BIG, BIG); } catch(Error e) { // Display
     * the images (for debugging purposes) if (DEBUG) { displayImage(screen,
     * "Actual", true); displayImage(createSimpleImage(BIG, BIG, reference),
     * "Reference", true); }
     * 
     * // rethrow the exception throw e; } }
     */
    /**
     * Compares the pixels in the reference array to the actual array. Asserts
     * that the colors are (within) a reasonable range of one another.
     */
    protected void checkPixels(int reference[], int actual[], int w, int h)
    {
        assertEquals("Actual and Reference pixel buffers not the same size", reference.length, actual.length);

        int i = 0;
        for (int y = 0; y < h; ++y)
            for (int x = 0; x < w; ++x)
            {
                checkColors(reference[i], actual[i], x, y);
                ++i;
            }
    }

    /**
     * Checks that the pixels defined by the reference and actual color (in
     * ARGB) are within a reasonable range of one another.
     */
    protected void checkColors(int reference, int actual, int x, int y)
    {
        int rColors[] = extractColors(reference);
        int aColors[] = extractColors(actual);

        assertClose("Actual alpha out of range of reference alpha (" + x + "," + y + ")", rColors[0], aColors[0],
                ALPHA_DELTA);

        assertClose("Actual red out of range of reference red (" + x + "," + y + ")", rColors[1], aColors[1],
                COLOR_DELTA);
        assertClose("Actual green out of range of reference green (" + x + "," + y + ")", rColors[2], aColors[2],
                COLOR_DELTA);
        assertClose("Actual blue out of range of reference blue (" + x + "," + y + ")", rColors[3], aColors[3],
                COLOR_DELTA);
    }

    /** Allowable delta for alpha portion of pixel. */
    public static final int ALPHA_DELTA = 2;

    /** Allowable delat for color portion of pixel. */
    public static final int COLOR_DELTA = 4;

    /**
     * Asserts that the two values are within delta of each other.
     */
    protected void assertClose(String msg, int expected, int actual, int delta)
    {
        assertEquals(msg, (double) expected, (double) actual, (double) delta);
    }

    /**
     * Extracts the ARGB components into an array.
     */
    protected int[] extractColors(int argb)
    {
        return new int[] { (argb >> 24) & 0xff, (argb >> 16) & 0xff, (argb >> 8) & 0xff, argb & 0xff };
    }

    /**
     * Copy the source pixels to the destination pixels (SRC rule). A new result
     * array is <b>not</b> generated. The given destination is returned with the
     * source pixels copied to it.
     */
    protected int[] copyPixels(int dst[], int dW, int dH, int src[], int sW, int sH, Fraction fraction, Point offset)
    {
        final int start_x = offset.x;
        final int start_y = offset.y;
        final int end_x = Math.min(start_x + sW, dW);
        final int end_y = Math.min(start_y + sH, dH);

        // Compose
        int dI = 0;
        int sI = 0;
        for (int y = 0; y < dH; ++y)
        {
            for (int x = 0; x < dW; ++x)
            {
                if (x >= start_x && x < end_x && y >= start_y && y < end_y)
                {
                    float f = fraction.getFraction(x - start_x, y - start_y);
                    if (f == 1.0f)
                        dst[dI] = src[sI];
                    else
                    {
                        int rgb = src[sI] & 0x00ffffff;
                        int a = ((src[sI] >>> 24) & 0xff);

                        a = (int) (a * f);

                        dst[dI] = ((a & 0xff) << 24) | rgb;
                    }
                    ++sI;
                }
                ++dI;
            }
        }
        return dst;
    }

    /**
     * This interface is used to determine the alpha value for the given pixel.
     */
    protected interface Fraction
    {
        public float getFraction(int x, int y);
    }

    /**
     * Represents a spatially and temporally constant alpha value.
     */
    protected static class FloatFraction implements Fraction
    {
        private float fraction;

        public FloatFraction(float f)
        {
            fraction = f;
        }

        public float getFraction(int x, int y)
        {
            return fraction;
        }
    }

    protected static final Fraction NoAlpha = new FloatFraction(1.0f);

    /**
     * Composite the source and destination pixels using the given composition
     * rule (in <code>rule</code>), and return the result. The source pixels are
     * offset within the destination pixels by the given <code>Point</code>. A
     * new result array is generated that is equal in size to the destination
     * array.
     * 
     * @param dst
     *            destination pixels
     * @param dW
     *            destination pixels width (scan)
     * @param dW
     *            destination pixels height
     * @param src
     *            src pixels
     * @param sW
     *            source pixels width (scan)
     * @param sW
     *            source pixels height
     * @param fraction
     *            additional fractional component to apply to src
     * @param offset
     *            defines how the src pixels should be offset within the dst
     *            pixels when composited
     * @param rule
     *            the rule to use during composition
     * 
     * @return the pixels (with dimensions of dWxdH) resulting from compositing
     *         src onto dst using the given composition rule
     */
    protected int[] compositePixels(int dst[], int dW, int dH, int src[], int sW, int sH, Fraction fraction,
            Point offset, Compositor rule)
    {
        // The pixel array to be returned upon successful composition
        int result[] = new int[dst.length];

        if (offset == null) offset = new Point(0, 0);

        // Figure the range of dst x/y where src is to be composited
        final int start_x = offset.x;
        final int start_y = offset.y;
        final int end_x = Math.min(start_x + sW, dW);
        final int end_y = Math.min(start_y + sH, dH);

        // Compose
        int dI = 0;
        int sI = 0;
        for (int y = 0; y < dH; ++y)
        {
            for (int x = 0; x < dW; ++x)
            {
                if (x >= start_x && x < end_x && y >= start_y && y < end_y)
                {
                    float f = fraction.getFraction(x - start_x, y - start_y);
                    result[dI] = rule.compositePixel(dst[dI], src[sI], f);
                    ++sI;
                }
                else
                    result[dI] = dst[dI];
                ++dI;
            }
        }

        return result;
    }

    /**
     * Represents a composition strategy.
     */
    protected static abstract class Compositor
    {
        int pD = 0, pS = 0, pR = 0;

        float pF = 0f;

        /**
         * Returns the composition of the dst and src pixel (applying the
         * additional alpha fraction to the src pixel).
         * 
         * @param dst
         *            destination pixel
         * @param src
         *            source pixel
         * @param fraction
         *            additional source alpha component
         * @return resulting composite pixel
         */
        public int compositePixel(int dst, int src, float fraction)
        {
            // Cache previous composition for faster composition
            if (dst == pD && src == pS && fraction == pF)
                return pR;
            else
                return (pR = compPixelImpl(pD = dst, pS = src, pF = fraction));
        }

        public abstract int compPixelImpl(int dst, int src, float fraction);
    }

    /** Implements SRC_OVER rule. */
    protected static final Compositor SRC_OVER = new Compositor()
    {
        /** Implements SRC_OVER rule. */
        public int compPixelImpl(int dst, int src, float fraction)
        {
            return srcOver(dst, src, fraction);
        }
    };

    /** Implements DST_IN rule. */
    protected static final Compositor DST_IN = new Compositor()
    {
        /** Implements DST_IN rule. */
        public int compPixelImpl(int dst, int src, float fraction)
        {
            return dstIn(dst, src, fraction);
        }
    };

    /** Implements SRC rule. */
    protected static final Compositor SRC = new Compositor()
    {
        /** Implements SRC rule. */
        public int compPixelImpl(int dst, int src, float fraction)
        {
            return src(dst, src, fraction);
        }
    };

    /**
     * Implements DST_IN rule.
     * 
     * <pre>
     * A = sA*dA
     * C = sA*dC
     * </pre>
     */
    protected static int dstIn(int dst, int src, float fraction)
    {
        int a, r, g, b;
        int as, rs, gs, bs;

        // Extract the destination color components
        a = (dst >>> 24);
        r = (dst >>> 16) & 0xff;
        g = (dst >>> 8) & 0xff;
        b = (dst & 0xff);

        // Extract the source color components
        as = (src >>> 24);
        rs = (src >>> 16) & 0xff;
        gs = (src >>> 8) & 0xff;
        bs = (src & 0xff);

        // Apply the additional source alpha
        if (fraction != 1.0f) as *= fraction;

        // Note that we are dealing with the numerator portion
        // of color/alpha fractions. The denominator is 255.

        // Pre-multiply the color components (only dst will be used)
        r = a * r / 255;
        g = a * g / 255;
        b = a * b / 255;

        // Calculate the new color components:
        // Alpha = sAlpha*dAlpha
        // Color = sAlpha*dColor
        a = as * a / 255;
        r = as * r / 255;
        g = as * g / 255;
        b = as * b / 255;

        // go to black color if fully transparent
        if (a <= 0) a = r = g = b = 0;
        // Un-premultiply the result
        if (a > 0 && a < 255)
        {
            r = r * 255 / a;
            g = g * 255 / a;
            b = b * 255 / a;
        }

        int result = ((a & 0xff) << 24) | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);

        return result;
    }

    /**
     * Implements SRC_OVER rule.
     * 
     * <pre>
     * A = (1-sA)*dA + sA
     * C = (1-sA)*dC + sC
     * </pre>
     */
    protected static int srcOver(int dst, int src, float fraction)
    {
        int a, r, g, b;
        int as, rs, gs, bs;

        // Extract the destination color components
        a = (dst >>> 24);
        r = (dst >>> 16) & 0xff;
        g = (dst >>> 8) & 0xff;
        b = (dst & 0xff);

        // Extract the source color components
        as = (src >>> 24);
        rs = (src >>> 16) & 0xff;
        gs = (src >>> 8) & 0xff;
        bs = (src & 0xff);

        // Apply the additional source alpha
        if (fraction != 1.0f) as *= fraction;

        // Note that we are dealing with the numerator portion
        // of color/alpha fractions. The denominator is 255.

        // Pre-multiply the color components
        r = a * r / 255;
        g = a * g / 255;
        b = a * b / 255;
        rs = as * rs / 255;
        gs = as * gs / 255;
        bs = as * bs / 255;

        // Calculate the new color components:
        // Alpha = (1-sAlpha)*dAlpha + sAlpha
        // Color = (1-sAlpha)*dColor + sColor
        int cA = 255 - as;

        a = as + (cA * a) / 255;
        r = rs + (cA * r) / 255;
        g = gs + (cA * g) / 255;
        b = bs + (cA * b) / 255;

        // go to black color if fully transparent
        if (a <= 0) a = r = g = b = 0;
        // Un-premultiply the result
        if (a > 0 && a < 255)
        {
            r = r * 255 / a;
            g = g * 255 / a;
            b = b * 255 / a;
        }

        int result = ((a & 0xff) << 24) | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);

        return result;
    }

    /**
     * Implements SRC rule.
     * 
     * <pre>
     * A = sA
     * C = sC
     * </pre>
     */
    protected static int src(int dst, int src, float fraction)
    {
        // Extract the source alpha component
        int as = (src >>> 24);

        // Adjust the source alpha
        if (fraction != 1.0f) as *= fraction;

        // Use the new source alpha
        int result = ((as & 0xff) << 24) | (src & 0xffffff);

        return result;
    }

    /**
     * Creates a simple image of the specified height/width using a color
     * composed of the given alpha, red, green, and blue channels.
     * 
     * @param w
     *            width
     * @param h
     *            height
     * @param a
     *            alpha channel (0-255)
     * @param r
     *            red channel (0-255)
     * @param g
     *            green channel (0-255)
     * @param b
     *            blue channel (0-255)
     */
    protected Image createSimpleImage(int w, int h, int a, int r, int g, int b)
    {
        return Toolkit.getDefaultToolkit().createImage(
                new MemoryImageSource(w, h, createSimplePixels(w, h, a, r, g, b), 0, w));
    }

    /**
     * Creates a simple image of the specified height/width using the given
     * pixels.
     * 
     * @param w
     *            width
     * @param h
     *            height
     * @param pix
     *            the pixels (length should be w*h)
     */
    protected Image createSimpleImage(int w, int h, int pix[])
    {
        return Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(w, h, pix, 0, w));
    }

    /**
     * Creates an array of pixel values representing an image of the specified
     * height/width using a color composed of the given alpha, red, green, and
     * blue channels.
     * 
     * @param w
     *            width
     * @param h
     *            height
     * @param a
     *            alpha channel (0-255)
     * @param r
     *            red channel (0-255)
     * @param g
     *            green channel (0-255)
     * @param b
     *            blue channel (0-255)
     */
    protected int[] createSimplePixels(int w, int h, int a, int r, int g, int b)
    {
        int pix[] = new int[w * h];

        for (int i = 0; i < pix.length; ++i)
            pix[i] = (a << 24) | (r << 16) | (g << 8) | b;

        return pix;
    }

    /**
     * Calculate a 'fancy' image gradient where the alpha value changes based on
     * the x,y coordinate. This creates an image that changes from transparent
     * black in the top left corner to opaque black in the bottom right.
     */
    protected int[] createAlphaPixels(int w, int h)
    {
        int pixels[] = new int[w * h];

        int i = 0;
        for (int y = 0; y < h; ++y)
        {
            for (int x = 0; x < w; ++x)
            {
                int alpha;

                if (false)
                    alpha = (x > y) ? (x * 255) / (w - 1) : (y * 255) / (h - 1);
                else
                    alpha = (x + y) * 255 / (w + h - 1);
                pixels[i++] = (alpha << 24) | 0;
            }
        }
        if (false) displayImage(createSimpleImage(w, h, pixels), "Gradient", true);

        return pixels;
    }

    /**
     * Used for debugging purposes to display an image.
     */
    void displayImage(Image img, String name, final boolean exit)
    {
        HScene testScene = getHScene();
        HStaticIcon icon = new HStaticIcon(img);
        testScene.add(icon);
        testScene.addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent e)
            {
                System.exit(0);
            }
        });
        testScene.show();
    }

    protected static final boolean DEBUG = false;

    /**
     * Test for correct ancestry.
     * <ul>
     * <li>implements HMatte
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testImplements(hmatte.getClass(), HMatte.class);
    }

    /**
     * Tests the constructors of this HMatte class.
     */
    public abstract void testConstructors();

    /**
     * Tests the fields of this look:
     * <ul>
     * <li>No public non-static non-final fields.
     * <li>No added public fields
     * </ul>
     */
    public void testFields()
    {
        TestUtils.testNoPublicFields(hmatte.getClass());
        TestUtils.testNoAddedFields(hmatte.getClass(), new String[0]);
    }

    /**
     * Tests getMatteData/setMatteData.
     * <ul>
     * <li>The set data should be the retreived data
     * </ul>
     */
    public abstract void testMatteData();

    /**
     * Tests proper operation of matte with an HContainer.
     * <ul>
     * <li>GROUPED means that the HContainer's background (if there is one) as
     * well as the sub-components are affected by the matte.
     * <li>UNGROUPED means that only HContainer's background (there isn't one by
     * default) is affected by the matte.
     * </ul>
     */
    public abstract void testContainer() throws Exception;

    /**
     * Tests proper operation of matte with an HComponent.
     */
    public abstract void testComponent() throws Exception;

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
     * </ol>
     * <p>
     * Note that this is here so that it doesn't have to be implemented in both
     * HFlatEffectMatteTest and HImageEffectMatteTest.
     */
    public void doTestPlayback(HAnimateEffect anim, TestPlayback test)
    {
        HMatte matte = (HMatte) anim;
        PlaybackVisible c = new PlaybackVisible(anim);

        HScene testScene = getHScene();
        HContainer hc = new HContainer();
        testScene.add(hc);
        hc.add(c);
        try
        {
            c.setMatte(matte);
        }
        catch (Exception e)
        {
            fail("ERROR: Could not set matte");
        }

        try
        {
            testScene.show();

            long start, stop;

            // setup test
            test.setup(anim);
            // push play
            start = System.currentTimeMillis();
            c.playing = true;
            test.play(anim);
            // stop after a period of time
            test.stop(anim);
            stop = System.currentTimeMillis();
            c.playing = false;
            // check correct playback
            test.check(anim, c.positions, start, stop);
        }
        finally
        {
            testScene.remove(hc);
        }
    }

    /**
     * Simple HVisible which is used to test for appropriate playback. Tracks
     * positions and current time in a vector.
     * <p>
     * Note that this is here so that it doesn't have to be implemented in both
     * HFlatEffectMatteTest and HImageEffectMatteTest.
     */
    public static class PlaybackVisible extends HVisible
    {
        public java.util.Vector positions = new java.util.Vector();

        public boolean playing = false;

        public HAnimateEffect a;

        public PlaybackVisible(HAnimateEffect a)
        {
            super(null, 0, 0, 50, 50);
            this.a = a;
        }

        public void paint(Graphics g)
        {
            if (playing)
            {
                long time = System.currentTimeMillis();
                if (DEBUG) System.out.println(a.getPosition() + " @ " + time);
                positions.add(new TimePosition(a.getPosition(), time));
            }
        }
    }
}
