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

import org.havi.ui.event.*;
import org.cablelabs.test.*;
import junit.framework.*;
import java.net.*;

/**
 * Tests {@link #HBackgroundImage}.
 * 
 * @author Tom Henriksen
 * @author Aaron Kamienski (1.01 update)
 * @version $Revision: 1.7 $, $Date: 2002/06/03 21:32:11 $
 */
public class HBackgroundImageTest extends TestCase
{
    /** True if background images are supported on this platform */
    boolean backgroundImagesSupported;

    private String path = null;

    private HBackgroundImage image = null;

    /**
     * Standard constructor.
     */
    public HBackgroundImageTest(String str)
    {
        super(str);
        backgroundImagesSupported = TestSupport.getProperty("snap2.havi.test.backgroundDeviceImageSupported", true);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HBackgroundImageTest.class);
    }

    /**
     * Setup.
     */
    public void setUp() throws Exception
    {
        try
        {
            path = TestSupport.getBaseDirectory() + "/qa/xlet/images/test1.m2v";
        }
        catch (Exception e1)
        {
            fail("Could not get background image resource");
        }

        image = new HBackgroundImage(path);

        super.setUp();
    }

    /**
     * Teardown.
     */
    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends Object (by default)
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HBackgroundImage.class, Object.class);
    }

    /**
     * Test the constructor of HBackgroundImage.
     * <ul>
     * <li>HBackgroundImage()
     * <li>HBackgroundImage(URL)
     * <li>HBackgroundImage(byte[])
     * </ul>
     */
    public void testConstructors()
    {
        // Do not run this test if mattes are not supported on this platform.
        if (!backgroundImagesSupported) return;

        checkConstructor("HBackgroundImage()", new HBackgroundImage(path), path);
    }

    /**
     * Check for proper initialization of constructor variables.
     */
    private void checkConstructor(String msg, HBackgroundImage i, String fileName)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", i);

    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(HBackgroundImage.class);
    }

    /**
     * Tests load.
     * <ul>
     * <li>Test whether the image loaded.
     * <li>Test whether the load listener is called.
     * <li>Should test using all 3 constructions.
     * </ul>
     */
    public void testLoad()
    {
        // Do not run this test if mattes are not supported on this platform.
        if (!backgroundImagesSupported) return;

        final boolean[] loaded = new boolean[1];

        HBackgroundImageListener l = new HBackgroundImageListener()
        {
            public void imageLoaded(HBackgroundImageEvent e)
            {
                loaded[0] = true;
            }

            public void imageLoadFailed(HBackgroundImageEvent e)
            {
            }
        };

        loaded[0] = false;

        // Check for proper call of event handler
        image.load(l);

        // Wait a few seconds for this thing to load.
        try
        {
            final int sleep = 2000;
            final int incr = 100;
            for (int i = 0; !loaded[0] && i < sleep; i += incr)
                Thread.sleep(incr);
        }
        catch (Exception e)
        {
            fail("Failed to load the image");
        }

        assertTrue("The image should be loaded", loaded[0]);

    }

    /**
     * Tests getHeight and getWidth.
     * <ul>
     * <li>Should test using all 3 constructions.
     * </ul>
     */
    public void testDimensions()
    {
        // Do not run this test if mattes are not supported on this platform.
        if (!backgroundImagesSupported) return;

        testLoad();

        assertEquals("Image width incorrect", 640, image.getWidth());
        assertEquals("Image height incorrect", 480, image.getHeight());
    }

    /**
     * Tests flush.
     * <ul>
     * <li>Should test using all 3 constructions.
     * </ul>
     */
    public void testFlush()
    {
        // Do not run this test if mattes are not supported on this platform.
        if (!backgroundImagesSupported) return;

        testLoad();

        // Flush the image
        image.flush();

        assertEquals("Image width should be -1 after flush", -1, image.getWidth());
        assertEquals("Image height should be -1 after flush", -1, image.getHeight());
    }
}
