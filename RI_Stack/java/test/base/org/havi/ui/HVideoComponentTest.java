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
import org.havi.ui.event.*;
import java.awt.*;

/**
 * Tests {@link #HVideoComponent}.
 * 
 * @author Tom Henriksen
 * @version $Revision: 1.3 $, $Date: 2002/06/03 21:32:23 $
 */
public class HVideoComponentTest extends GUITest
{
    private TestVideoComponent video = null;

    /**
     * Standard constructor.
     */
    public HVideoComponentTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HVideoComponentTest.class);
    }

    /**
     * Setup.
     */
    public void setUp() throws Exception
    {
        super.setUp();
        video = new TestVideoComponent();
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
     * <li>extends HComponent
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HVideoComponent.class, HComponent.class);
    }

    /**
     * Test that there are no public HVideoComponent constructors.
     */
    public void testConstructors()
    {
        TestUtils.testNoPublicConstructors(HVideoComponent.class);
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(HVideoComponent.class);
    }

    /**
     * Test getVideoDevice.
     * <ul>
     * <li>
     * </ul>
     */
    public void testVideoDevice()
    {
        HVideoDevice vd = video.getVideoDevice();

        if (vd != null)
            assertTrue(vd.getClass().getName() + " retrieved from HVideoComponent is "
                    + "not null & is not an instance of HVideoDevice.", (vd != null && vd instanceof HVideoDevice));
    }

    private void sendLocationModifiedEvent(HVideoComponent comp)
    {
        // Alter the screen location of the HVideoComponent. This should force
        // the OnScreenLocationModifiedListener to be called if one is set.
        Point onScreen = comp.getLocationOnScreen();
        onScreen.x++;
        onScreen.y++;
        comp.setLocation(onScreen.x, onScreen.y);
    }

    /**
     * Test addOnScreenLocationModifiedListener/
     * removeOnScreenLocationModifiedListener.
     * <ul>
     * <li>Test that the listener gets called
     * <li>Ensure that it doesn't after being removed
     * </ul>
     * 
     * This method is not implemented in the stub test class
     */
    public void xxxtestOnScreenLocationModifiedListener()
    {
        final boolean okay[] = new boolean[1];

        HScreenLocationModifiedListener l = new HScreenLocationModifiedListener()
        {
            public void report(HScreenLocationModifiedEvent gce)
            {
                okay[0] = true;
            }
        };

        HScene testScene = getHScene();
        try
        {
            video.setSize(100, 100);
            testScene.add(video);
            video.setSize(video.getPreferredSize());
            testScene.show();
            // Check for proper call of event handler
            okay[0] = false;
            video.addOnScreenLocationModifiedListener(l);
            sendLocationModifiedEvent(video);
            assertTrue("OnScreenLocationModifiedListener should've been called", okay[0]);

            // Check for proper disconnection of event handler
            okay[0] = false;
            video.removeOnScreenLocationModifiedListener(l);
            sendLocationModifiedEvent(video);
            assertTrue("OnScreenLocationModifiedListener should NOT have been called", !okay[0]);
        }
        finally
        {
            testScene.remove(video);
        }
    }

    class TestVideoComponent extends HVideoComponent
    {
        public TestVideoComponent()
        {
            super();
        }
    }
}
