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

package org.dvb.ui;

import junit.framework.*;
import java.awt.*;
import java.awt.image.*;
import java.util.*;
import org.cablelabs.test.TestUtils;

/**
 * Tests DVBBufferedImage.
 * 
 * @todo Implement ImageTest and extend it. Perhaps with some of the basic tests
 *       moved there.
 * @todo Refactor to test both TYPE_ADVANCED/TYPE_BASE
 * @author Aaron Kamienski
 */
public class DVBBufferedImageTest extends TestCase
{
    /**
     * Verifies heritage.
     */
    public void testHeritage()
    {
        TestUtils.testExtends(DVBBufferedImage.class, Image.class);
    }

    /**
     * Tests constructors.
     */
    public void testConstructor()
    {
        // Just see if it works...
        DVBBufferedImage dbi = new DVBBufferedImage(100, 100);
        dbi = new DVBBufferedImage(100, 100, DVBBufferedImage.TYPE_BASE);
        dbi = new DVBBufferedImage(100, 100, DVBBufferedImage.TYPE_ADVANCED);
    }

    /**
     * Tests createGraphics().
     */
    public void testCreateGraphics()
    {
        DVBBufferedImage img = dvbbufferedimage;
        DVBGraphics g = img.createGraphics();

        assertNotNull("Should not return null", g);

        img.dispose();
        assertNull("Should return null after dispose()", img.createGraphics());
    }

    /**
     * Tests flush().
     */
    public void testFlush()
    {
        DVBBufferedImage img = dvbbufferedimage;

        // Fill in image
        decorateImage(img, width, height);

        // flush
        img.flush();

        // Verify that flush left pixels alone
        decorateValidate(img, width, height);

        img.flush();
        img.dispose();
        // Should fail silently
        img.flush();
    }

    /**
     * Tests getGraphics().
     */
    public void testGetGraphics()
    {
        DVBBufferedImage img = dvbbufferedimage;
        Graphics g = img.getGraphics();

        assertNotNull("Should not return null", g);
        assertTrue("Should be instanceof DVBGraphics", g instanceof DVBGraphics);

        img.dispose();
        assertNull("Should return null after dispose()", img.getGraphics());
    }

    /**
     * Tests getHeight/getWidth.
     */
    public void testGetHeightWidth()
    {
        DVBBufferedImage img = dvbbufferedimage;

        assertEquals("getHeight(ImageObserver) should return height", height, img.getHeight(null));
        assertEquals("getWidth(ImageObserver) should return width", width, img.getWidth(null));
        assertEquals("getHeight() should return height", height, img.getHeight());
        assertEquals("getWidth() should return width", width, img.getWidth());

        img.dispose();
        assertEquals("getHeight(ImageObserver) should return height after dispose()", -1, img.getHeight(null));
        assertEquals("getWidth(ImageObserver) should return width after dispose()", -1, img.getWidth(null));
        assertEquals("getHeight() should return height after dispose()", -1, img.getHeight());
        assertEquals("getWidth() should return width after dispose()", -1, img.getWidth());
    }

    /**
     * Tests getImage().
     */
    public void testGetImage()
    {
        DVBBufferedImage img = dvbbufferedimage;

        Image i = img.getImage();
        assertNotNull("getImage should not return null", i);

        img.dispose();
        assertNull("Should return null after dispose()", img.getImage());
    }

    /**
     * Tests dispose().
     * 
     * @todo verify that resources are released
     */
    public void testDispose()
    {
        // How can we test that the resources are released?
        // The other methods test everything else.

        // For now, simply verify that double-dispose isn't a problem
        DVBBufferedImage img = dvbbufferedimage;

        img.dispose();
        img.dispose();
    }

    /**
     * Tests getProperty().
     */
    public void testGetProperty()
    {
        DVBBufferedImage img = dvbbufferedimage;

        // We don't expect any properties to be set
        assertSame("Should return UndefinedProperty for unknown property", Image.UndefinedProperty, img.getProperty(
                "FLCL", null));

        img.dispose();
        assertNull("Should return null after dispose()", img.getProperty("FLCL", null));
    }

    /**
     * Tests getRGB().
     */
    public void testGetRGB() throws Exception
    {
        DVBBufferedImage img = dvbbufferedimage;

        // Intially, all should be 0x0
        for (int x = 0; x < width; ++x)
            for (int y = 0; y < height; ++y)
                assertEquals("getRGB() default value expected", 0, img.getRGB(x, y));

        DVBGraphics g = null;
        int rgba = 0x80706090;
        try
        {
            g = img.createGraphics();
            g.setColor(new DVBColor(rgba, true));
            g.setDVBComposite(DVBAlphaComposite.Src);
            g.fillRect(0, 0, width, height);
        }
        finally
        {
            if (g != null) g.dispose();
        }

        for (int x = 0; x < width; ++x)
            for (int y = 0; y < height; ++y)
                assertEquals("getRGB() new value expected", rgba, img.getRGB(x, y));

        Point invalid[] = { new Point(-1, -1), new Point(width, height), new Point(width / 2, height * 2),
                new Point(width * 2, height / 2), };

        // Out of bounds
        for (int i = 0; i < invalid.length; ++i)
        {
            try
            {
                img.getRGB(invalid[i].x, invalid[i].y);
                fail("Expected ArrayIndexOutOfBoundsException for " + invalid[i]);
            }
            catch (ArrayIndexOutOfBoundsException e)
            {
            }
        }

        img.dispose();
        try
        {
            img.getRGB(0, 0);
            fail("Expected ArrayIndexOutOfBoundsException after dispose()");
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
        }
    }

    /**
     * Tests getRGB() (array).
     */
    public void testGetRGB_array() throws Exception
    {
        // Clear pixels
        int[] pixels = new int[width * height];
        for (int x = 0; x < width; ++x)
            for (int y = 0; y < height; ++y)
                pixels[y * width + x] = -1;

        DVBBufferedImage img = dvbbufferedimage;
        int[] array = img.getRGB(0, 0, width, height, pixels, 0, width);
        assertNotNull("Should not return null", array);
        assertSame("Expected same array as was passed in", pixels, array);

        // Intially, all should be 0x0
        for (int x = 0; x < width; ++x)
            for (int y = 0; y < height; ++y)
                assertEquals("getRGB() default value expected", 0, array[y * width + x]);

        DVBGraphics g = null;
        int rgba = 0x6789abcd;
        try
        {
            g = img.createGraphics();
            g.setColor(new DVBColor(rgba, true));
            g.setDVBComposite(DVBAlphaComposite.Src);
            g.fillRect(0, 0, width, height);
        }
        finally
        {
            if (g != null) g.dispose();
        }

        // Clear pixels
        for (int x = 0; x < width; ++x)
            for (int y = 0; y < height; ++y)
                pixels[y * width + x] = -1;

        array = img.getRGB(0, 0, width / 2, height / 2, pixels, 0, width);
        assertNotNull("Should not return null", array);
        assertSame("Expected same array as was passed in", pixels, array);

        for (int x = 0; x < width; ++x)
            for (int y = 0; y < height; ++y)
            {
                if (x < width / 2 && y < height / 2)
                    assertEquals("getRGB() new value expected", rgba, pixels[y * width + x]);
                else
                    assertEquals("getRGB() should not have touched rest of array", -1, pixels[y * width + x]);
            }

        try
        {
            array = img.getRGB(-1, -1, width * 2, height * 2, pixels, 0, width);
            fail("Expected ArrayIndexOutOfBoundsException for bad values");
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
        }

        img.dispose();
        try
        {
            array = img.getRGB(0, 0, width, height, pixels, 0, width);
            fail("Expected ArrayIndexOutOfBoundsException after dispose()");
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
        }
    }

    /**
     * Tests getSource().
     */
    public void testGetSource() throws Exception
    {
        DVBBufferedImage img = dvbbufferedimage;

        ImageProducer source = img.getSource();

        assertNotNull("getSource() should return the image producer for this image", source);
        assertSame("getSource() should return same instance for multiple calls", source, img.getSource());

        // Let's take that imageProducer and see if we can create another image?
        // Fill in image with data
        decorateImage(img, width, height);

        // Create "copy"
        Image copy = null;
        try
        {
            copy = Toolkit.getDefaultToolkit().createImage(img.getSource());

            assertNotNull("Should be able to create image from getSource()");

            // Use PixelGrabber to get pixels out of copy
            int[] pix = new int[width * height];
            PixelGrabber grabber = new PixelGrabber(copy, 0, 0, width, height, pix, 0, width);
            grabber.grabPixels();

            // Validate the pixels
            decorateValidate(pix, width, height);
        }
        finally
        {
            copy.flush();
            copy = null;
        }

        img.dispose();
        assertNull("null should be returned after dispose() is called", img.getSource());
    }

    /**
     * Tests getSubimage().
     */
    public void testGetSubimage() throws Exception
    {
        DVBBufferedImage img = dvbbufferedimage;

        DVBBufferedImage sub = img.getSubimage(width / 4, height / 4, width / 2, height / 2);
        assertNotNull("Sub-image should not be null", sub);
        assertEquals("Sub-image should be of the expected width", width / 2, sub.getWidth());
        assertEquals("Sub-image should be of the expected height", height / 2, sub.getHeight());

        // Shares the same data array as the original...
        sub.setRGB(0, 0, 0xaabbccdd);
        assertEquals("Pixel set on sub-image should be seen on original", 0xaabbccdd, img.getRGB(25, 25));
        img.setRGB(26, 26, 0x11223344);
        assertEquals("Pixel set on original should be seen on sub-image", 0x11223344, sub.getRGB(1, 1));

        try
        {
            img.getSubimage(-1, -1, width + 2, height + 2);
            fail("Expected a DVBRasterFormatException for out-of-range values");
        }
        catch (DVBRasterFormatException e)
        {
        }

        img.dispose();
        assertNull("Should return null after dispose()", img.getSubimage(width / 4, height / 4, width / 2, height / 2));
    }

    /**
     * Tests setRGB().
     */
    public void testSetRGB()
    {
        DVBBufferedImage img = dvbbufferedimage;

        // Call setRGB
        decorateImage(img, width, height);

        // Validate setRGB
        int[] pixels = new int[width * height];
        int[] array = img.getRGB(0, 0, width, height, pixels, 0, width);
        decorateValidate(array, width, height);

        Point invalid[] = { new Point(-1, -1), new Point(width, height), new Point(width / 2, height * 2),
                new Point(width * 2, height / 2), };

        // Out of bounds
        for (int i = 0; i < invalid.length; ++i)
        {
            try
            {
                img.setRGB(invalid[i].x, invalid[i].y, 0);
                fail("Expected ArrayIndexOutOfBoundsException for " + invalid[i]);
            }
            catch (ArrayIndexOutOfBoundsException e)
            {
            }
        }

        img.dispose();
        try
        {
            img.setRGB(0, 0, -1);
            fail("Expected ArrayIndexOutOfBoundsException after dispose()");
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
        }
    }

    /**
     * Tests setRGB() (array).
     */
    public void testSetRGB_array()
    {
        DVBBufferedImage img = dvbbufferedimage;

        int pixels[] = new int[width * height];
        decoratePixels(pixels, width, height);

        // Call setRGB
        img.setRGB(0, 0, width, height, pixels, 0, width);

        // Validate setRGB
        decorateValidate(img, width, height);

        img.dispose();
        try
        {
            img.setRGB(0, 0, width, height, pixels, 0, width);
            fail("Expected ArrayIndexOutOfBoundsException after dispose()");
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
        }
    }

    /**
     * Tests setRGB() (array) array bounds checking.
     */
    public void testSetRGB_arrayBounds()
    {
        DVBBufferedImage img = dvbbufferedimage;

        // Validate bounds checking
        // Out of bounds w.r.t the array
        // scansize too big
        // offset out of range
        // width < 0, width >= width, height < 0, height >= height
        int pixels[] = new int[width * height];
        int offset = 0;
        int scansize = width;

        // Expect no problems
        img.setRGB(0, 0, width, height, pixels, offset, scansize);
        img.setRGB(0, 0, 1, 1, pixels, offset, scansize);
        img.setRGB(0, 0, 0, 0, pixels, offset, scansize);

        // Expect ArrayIndexOutOfBoundsExceptions
        for (int run = 0; run < 8; ++run)
        {
            try
            {
                switch (run)
                {
                    case 0:
                        img.setRGB(0, 0, width, height, pixels, -1, scansize);
                        break;
                    case 1:
                        img.setRGB(0, 0, width, height, pixels, pixels.length, scansize);
                        break;
                    case 2:
                        img.setRGB(0, 0, width, height, pixels, offset, -1);
                        break;
                    case 3:
                        img.setRGB(0, 0, width, height, pixels, offset, width + 1);
                        break;
                    case 4:
                        img.setRGB(0, 0, width, -1, pixels, offset, scansize);
                        break;
                    case 5:
                        img.setRGB(0, 0, width, height + 1, pixels, offset, scansize);
                        break;
                    case 6:
                        img.setRGB(0, 0, -1, height, pixels, offset, scansize);
                        break;
                    case 7:
                        img.setRGB(0, 0, width + 1, height, pixels, offset, scansize);
                        break;
                    default:
                        fail("Unknown test (" + run + ")");
                }
                fail("Expected ArrayIndexOutOfBoundsException (" + run + ")");
            }
            catch (ArrayIndexOutOfBoundsException e)
            {
            }
        }
    }

    /**
     * Tests setRGB() (array) image bounds checking.
     */
    public void testSetRGB_arrayImageBounds()
    {
        DVBBufferedImage img = dvbbufferedimage;

        // Out of bounds w.r.t the Image
        // x < 0, x >= width, y < 0, y >= width
        // width < 0, width >= width, height < 0, height >= height
        int pixels[] = new int[(width * 2) * (height * 2)];
        int x = 0;
        int y = 0;
        int scansize = width;

        // Expect no problems
        img.setRGB(x, y, width, height, pixels, 0, scansize);
        img.setRGB(x + width - 1, y + height - 1, 1, 1, pixels, 0, scansize);

        // Expect ArrayIndexOutOfBoundsExceptions
        for (int run = 0; run < 8; ++run)
        {
            try
            {
                switch (run)
                {
                    case 0:
                        img.setRGB(-1, y, width, height, pixels, 0, scansize);
                        break;
                    case 1:
                        img.setRGB(width + 1, y, width, height, pixels, 0, scansize);
                        break;
                    case 2:
                        img.setRGB(x, -1, width, height, pixels, 0, scansize);
                        break;
                    case 3:
                        img.setRGB(x, height + 1, width, height, pixels, 0, scansize);
                        break;
                    case 4:
                        img.setRGB(x, y, -1, height, pixels, 0, scansize);
                        break;
                    case 5:
                        img.setRGB(x, y, width + 1, height, pixels, 0, scansize);
                        break;
                    case 6:
                        img.setRGB(x, y, width, -1, pixels, 0, scansize);
                        break;
                    case 7:
                        img.setRGB(x, y, width, height + 1, pixels, 0, scansize);
                        break;
                    default:
                        fail("Unknown test (" + run + ")");
                }
                fail("Expected ArrayIndexOutOfBoundsException (" + run + ")");
            }
            catch (ArrayIndexOutOfBoundsException e)
            {
            }
        }
    }

    /**
     * Tests getScaledInstance().
     */
    public void testGetScaledInstance()
    {
        DVBBufferedImage img = dvbbufferedimage;
        Image scaled = img.getScaledInstance(25, 25, Image.SCALE_DEFAULT);
        assertNotNull("Should not return a null image", scaled);
        assertEquals("Unexpected width", 25, scaled.getWidth(null));
        assertEquals("Unexpected height", 25, scaled.getHeight(null));
    }

    /**
     * Tests that DVBBufferedImage can be used for drawImage().
     */
    public void testDrawImage()
    {
        DVBBufferedImage src = dvbbufferedimage;

        // Initialize src with a Color
        Graphics g = null;
        Color srcColor = Color.red;
        try
        {
            g = src.createGraphics();
            g.setColor(srcColor);
            g.fillRect(0, 0, width, height);
        }
        finally
        {
            if (g != null) g.dispose();
        }

        // Create a new DVBBufferedImage to drawImage to
        DVBBufferedImage dst = new DVBBufferedImage(width, height);

        // Draw a background color
        // Draw src to SE quadrant
        g = null;
        Color dstColor = Color.black;
        try
        {
            g = dst.createGraphics();
            g.setColor(dstColor);
            g.fillRect(0, 0, width, height);

            int x = width / 2;
            int h = height / 2;

            g.drawImage(src, x, h, null);
        }
        finally
        {
            if (g != null) g.dispose();
        }

        // Verify -- we'll only both with each quadrant
        assertEquals("NW quadrant should be unchanged", dstColor.getRGB(), dst.getRGB(0, 0));
        assertEquals("NE quadrant should be unchanged", dstColor.getRGB(), dst.getRGB(width - 1, 0));
        assertEquals("SE quadrant should changed", srcColor.getRGB(), dst.getRGB(width - 1, height - 1));
        assertEquals("SW quadrant should be unchanged", dstColor.getRGB(), dst.getRGB(0, height - 1));
    }

    /**
     * Fills in pixels within array with known data for later use/validation.
     * Fills in with a gradient that changes r and b with the x position and g
     * and a with the y position.
     */
    private static void decoratePixels(int pixels[], int width, int height)
    {
        for (int w = 0; w < width; ++w)
        {
            int r = w * 255 / width;
            int b = (width - w - 1) * 255 / width;
            for (int h = 0; h < height; ++h)
            {
                int g = h * 255 / height;
                int a = (height - h - 1) * 255 / height;
                int argb = (a << 24) | (r << 16) | (g << 8) | b;
                pixels[h * width + w] = argb;
            }
        }
        return;
    }

    /**
     * Fills in pixels of image with known data for later use/validation. Fills
     * in with a gradient that changes r and b with the x position and g and a
     * with the y position.
     */
    private static void decorateImage(DVBBufferedImage img, int width, int height)
    {
        for (int w = 0; w < width; ++w)
        {
            int r = w * 255 / width;
            int b = (width - w - 1) * 255 / width;
            for (int h = 0; h < height; ++h)
            {
                int g = h * 255 / height;
                int a = (height - h - 1) * 255 / height;
                int argb = (a << 24) | (r << 16) | (g << 8) | b;
                img.setRGB(w, h, argb);
                assertEquals("Set value should be Get value at  (" + w + "," + h + ")", argb, img.getRGB(w, h));
            }
        }
    }

    /**
     * Validates pixels within image match those generated by decorateImage()
     * and decoratePixels(). Assumes a gradient that changes r and b with the x
     * position and g and a with the y position.
     */
    private static void decorateValidate(DVBBufferedImage img, int width, int height)
    {
        for (int w = 0; w < width; ++w)
        {
            int r = w * 255 / width;
            int b = (width - w - 1) * 255 / width;
            for (int h = 0; h < height; ++h)
            {
                int g = h * 255 / height;
                int a = (height - h - 1) * 255 / height;
                int argb = (a << 24) | (r << 16) | (g << 8) | b;
                assertEquals("Unexpected pixel value at (" + w + "," + h + ")", argb, img.getRGB(w, h));
            }
        }
    }

    /**
     * Validates pixels within array match those generated by decorateImage()
     * and decoratePixels(). Assumes a gradient that changes r and b with the x
     * position and g and a with the y position.
     */
    private static void decorateValidate(int[] pixels, int width, int height)
    {
        for (int w = 0; w < width; ++w)
        {
            int r = w * 255 / width;
            int b = (width - w - 1) * 255 / width;
            for (int h = 0; h < height; ++h)
            {
                int g = h * 255 / height;
                int a = (height - h - 1) * 255 / height;
                int argb = (a << 24) | (r << 16) | (g << 8) | b;
                assertEquals("Unexpected pixel value at (" + w + "," + h + ")", argb, pixels[h * width + w]);
            }
        }
    }

    /**
     * Tests that DVBBufferedImage can be used for PixelGrabber.
     */
    public void testPixelGrabber() throws Exception
    {
        DVBBufferedImage img = dvbbufferedimage;

        // Call setRGB to set up the pixels in the DVBBufferedImage
        decorateImage(img, width, height);

        // Grab the pixels
        int[] pix = new int[width * height];
        PixelGrabber grabber = new PixelGrabber(img, 0, 0, width, height, pix, 0, width);
        grabber.grabPixels();

        // Validate the pixels
        decorateValidate(pix, width, height);
    }

    protected static final int width = 100, height = 100;

    protected DVBBufferedImage dvbbufferedimage;

    protected void setUp() throws Exception
    {
        super.setUp();

        dvbbufferedimage = new DVBBufferedImage(width, height);
    }

    protected void tearDown() throws Exception
    {
        dvbbufferedimage.flush();
        dvbbufferedimage.dispose();
        dvbbufferedimage = null;
        super.tearDown();
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite(args));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(DVBBufferedImageTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new DVBBufferedImageTest(tests[i]));
            return suite;
        }
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(DVBBufferedImageTest.class);
        return suite;
    }

    public DVBBufferedImageTest(String name)
    {
        super(name);
    }
}
