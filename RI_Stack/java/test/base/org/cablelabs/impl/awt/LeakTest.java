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
 * Created on Apr 21, 2006
 */
package org.cablelabs.impl.awt;

import org.cablelabs.debug.Memory;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests that various Graphics-related operations don't result in any leaks.
 * 
 * @author Aaron Kamienski
 */
public class LeakTest extends TestCase
{
    public void testNothing()
    {
        // Start out with gc()
        gc();
        int initial = getNativeMemory();

        // Allocate Java-only Object
        Object obj = new byte[1024];
        int allocated = getNativeMemory();

        assertNotNull("Expected non-null object", obj);

        // Release object
        obj = null;
        gc();
        int freed = getNativeMemory();

        assertEquals("Expected native memory to be unchanged: " + (freed - initial) + "/" + (allocated - initial),
                initial, freed);
        assertEquals("Expected native memory to be unchanged by alloc: " + (allocated - initial), initial, allocated);
    }

    public void testNewFont()
    {
        // Start out with gc()
        gc();
        int initial = getNativeMemory();

        // Allocate font resources
        // Font f = new Font("SansSerif", Font.PLAIN, 31);
        Font f = new Font("SansSerif", Font.BOLD, 16);
        FontMetrics fm = tk.getFontMetrics(f);
        int allocated = getNativeMemory();

        assertNotNull("Expected FontMetrics to be non-null", fm);

        // Release font resources
        f = null;
        fm = null;
        gc();
        int freed = getNativeMemory();

        assertEquals("Font creation leaked " + (freed - initial) + "/" + (allocated - initial), initial, freed);
    }

    public void testNewBufferedImage()
    {
        // TODO: consider eliminating the issue described below...
        // NOTE: it is known that creating a buffered image may initialize some
        // data that is remembered indefinitely... currently, that includes
        // AWT's default native font: Font(Dialog,PLAIN,12).
        // This test creates an initial image to ignore those creations...
        Image image = createBufferedImage();
        image.flush();
        image = null;

        doTestNewImage(new NewImage()
        {
            public Image create()
            {
                return createBufferedImage();
            }

            public String toString()
            {
                return "BufferedImage";
            }
        });
    }

    public void testNewBufferedImage_again()
    {
        testNewBufferedImage();
    }

    public void testNewMemoryImage()
    {
        doTestNewImage(new NewImage()
        {
            public Image create()
            {
                return createMemoryImage();
            }

            public String toString()
            {
                return "MemoryImage";
            }
        });
    }

    public void testNewMemoryImage_again()
    {
        testNewMemoryImage();
    }

    public void testNewGraphics()
    {
        doTestNewGraphics(createBufferedImage());
    }

    public void testCreateGraphics()
    {
        doTestCreateGraphics(createBufferedImage());
    }

    public void testCreateGraphics_clip()
    {
        doTestCreateGraphics_clip(createBufferedImage());
    }

    private void doTestNewGraphics(Image image)
    {
        assertNotNull("Expected Image to be non-null", image);
        try
        {
            // Start out with gc()
            gc();
            int initial = getNativeMemory();

            // Allocate Graphics
            Graphics g = image.getGraphics();
            int allocated = getNativeMemory();

            assertNotNull("Expected Graphics to be non-null", g);

            // Release Graphics resources
            g.dispose();
            g = null;
            gc();
            int freed = getNativeMemory();

            assertEquals("Graphics creation leaked " + (freed - initial) + "/" + (allocated - initial), initial, freed);
        }
        finally
        {
            image.flush();
        }
    }

    private void doTestCreateGraphics(Image image)
    {
        assertNotNull("Expected Image to be non-null", image);
        try
        {

            // Allocate initial Graphics
            Graphics base = image.getGraphics();
            assertNotNull("Expected Graphics to be non-null", base);
            try
            {

                // Start out with gc()
                gc();
                int initial = getNativeMemory();

                // Create 2nd graphics
                Graphics g = base.create();
                int allocated = getNativeMemory();

                assertNotNull("Expected Graphics to be non-null", g);

                // Release Graphics resources
                g.dispose();
                g = null;
                gc();
                int freed = getNativeMemory();

                assertEquals("Graphics creation leaked " + (freed - initial) + "/" + (allocated - initial), initial,
                        freed);
            }
            finally
            {
                base.dispose();
            }
        }
        finally
        {
            image.flush();
        }
    }

    private void doTestCreateGraphics_clip(Image image)
    {
        assertNotNull("Expected Image to be non-null", image);
        try
        {

            // Allocate initial Graphics
            Graphics base = image.getGraphics();
            assertNotNull("Expected Graphics to be non-null", base);
            try
            {

                // Start out with gc()
                gc();
                int initial = getNativeMemory();

                // Create 2nd graphics
                Graphics g = base.create(100, 100, 400, 400);
                int allocated = getNativeMemory();

                assertNotNull("Expected Graphics to be non-null", g);

                // Release Graphics resources
                g.dispose();
                g = null;
                gc();
                int freed = getNativeMemory();

                assertEquals("Graphics creation leaked " + (freed - initial) + "/" + (allocated - initial), initial,
                        freed);
            }
            finally
            {
                base.dispose();
            }
        }
        finally
        {
            image.flush();
        }
    }

    public void testDrawString()
    {
        doTestGraphics(new Draw()
        {
            public void doit(Graphics g)
            {
                g.drawString("Silly rabbit", 10, 100);
            }

            public String toString()
            {
                return "DrawString";
            }
        });
    }

    public void testFillRect()
    {
        doTestGraphics(new Draw()
        {
            public void doit(Graphics g)
            {
                g.fillRect(20, 20, 400, 400);
            }

            public String toString()
            {
                return "FillRect";
            }
        });
    }

    private interface NewImage
    {
        public Image create();
    }

    private void doTestNewImage(NewImage newImage)
    {
        // Start out with gc()
        gc();
        int initial = getNativeMemory();

        // Allocate Image
        Image image = newImage.create();
        int allocated = getNativeMemory();

        assertNotNull("Expected " + newImage + " to be non-null", image);

        // Release image resources
        image.flush();
        image = null;
        gc();
        int freed = getNativeMemory();

        assertEquals(newImage + " creation leaked " + (freed - initial) + "/" + (allocated - initial), initial, freed);
    }

    private interface Draw
    {
        public void doit(Graphics g);
    }

    private void doTestGraphics(Draw draw)
    {
        BufferedImage image = createBufferedImage();
        assertNotNull("Expected BufferedImage to be non-null", image);
        try
        {

            // Allocate initial Graphics
            Graphics g = image.getGraphics();
            assertNotNull("Expected Graphics to be non-null", g);
            try
            {

                // Start out with gc()
                gc();
                int initial = getNativeMemory();

                draw.doit(g);
                int allocated = getNativeMemory();

                // Make sure any allocated objects (and native) are freed
                gc();
                int freed = getNativeMemory();

                assertEquals(draw + " creation leaked " + (freed - initial) + "/" + (allocated - initial), initial,
                        freed);
            }
            finally
            {
                g.dispose();
            }
        }
        finally
        {
            image.flush();
            image = null;
        }
    }

    private BufferedImage createBufferedImage()
    {
        return GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .createCompatibleImage(512, 512);
    }

    private Image createMemoryImage()
    {
        return tk.createImage(new MemoryImageSource(512, 512, new int[512 * 512], 0, 512));
    }

    private static void gc()
    {
        for (int i = 0; i < 3; ++i)
        {
            System.gc();
            System.runFinalization();
        }
        System.gc();
    }

    /*
     * private int getJavaMemory() { Runtime r = Runtime.getRuntime(); return
     * (int)(r.totalMemory() - r.freeMemory()); }
     */

    private int getNativeMemory()
    {
        Memory memory = new Memory();
        final int NCOLORS = memory.getNumColors();
        int color = Integer.MIN_VALUE;
        for (int i = 0; i < NCOLORS; ++i)
        {
            if ("GFX_LL".equals(memory.getName(i)))
            {
                color = i;
                return memory.getAllocated(color);
            }
        }
        fail("Internal Error - could not get memory for GFX_LL");
        return -1; // not executed
    }

    /*  ********** Boilerplate ************* */

    private Toolkit tk;

    protected void setUp() throws Exception
    {
        tk = Toolkit.getDefaultToolkit();

        // System.out.println(getName());
    }

    public LeakTest(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(LeakTest.class);

        return suite;
    }

}
