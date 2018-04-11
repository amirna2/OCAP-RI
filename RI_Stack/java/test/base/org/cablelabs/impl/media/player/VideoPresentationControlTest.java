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
package org.cablelabs.impl.media.player;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.net.URL;
import java.util.Vector;

import javax.media.Player;
import javax.tv.media.AWTVideoSize;

import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.dvb.media.VideoFormatControl;
import org.dvb.media.VideoPresentationControl;
import org.havi.ui.HScreenRectangle;
import org.havi.ui.HVideoConfiguration;

import org.cablelabs.impl.media.CannedHScreen;
import org.cablelabs.impl.media.CannedVideoDevice;
import org.cablelabs.impl.media.CannedGraphicsDevice;
import org.cablelabs.impl.media.JMFBaseInterfaceTest;
import org.cablelabs.impl.media.mpe.CannedMediaAPI;
import org.cablelabs.impl.media.mpe.ScalingBounds;
import org.cablelabs.impl.media.mpe.ScalingCaps;

/**
 * VideoPresentationControlTest
 * 
 * @author Joshua Keplinger
 * 
 */
public class VideoPresentationControlTest extends JMFBaseInterfaceTest
{

    private VideoPresentationControl control;

    private Player player;

    private CannedVideoDevice vd;

    private CannedGraphicsDevice gd;

    private PlayerHelper helper;

    private HVideoConfiguration defConfig;

    private Container container;

    public static InterfaceTestSuite isuite(ImplFactory factory)
    {
        return new InterfaceTestSuite(VideoPresentationControlTest.class, factory);
    }

    public VideoPresentationControlTest(String name, Class clazz, ImplFactory factory)
    {
        super(name, clazz, factory);
    }

    public VideoPresentationControlTest(String name, ImplFactory factory)
    {
        this(name, Player.class, factory);
    }

    public void setUp() throws Exception
    {
        super.setUp();
        player = (Player) createImplObject();

        vd = (CannedVideoDevice) CannedHScreen.getInstance().getDefaultHVideoDevice();
        gd = (CannedGraphicsDevice) CannedHScreen.getInstance().getDefaultHGraphicsDevice();

        defConfig = vd.getDefaultConfiguration();
        CannedControllerListener listener = new CannedControllerListener(1);
        player.addControllerListener(listener);
        helper = new PlayerHelper(player, listener);
        helper.startPlayer();
        // We need to turn the player into a component player before the control
        // will be usable.
        container = new Container()
        {
            public Point getLocationOnScreen()
            {
                return getLocation();
            }
        };
        Rectangle bounds = new Rectangle(new Point(0, 0), gd.getDefaultConfiguration().getPixelResolution());
        ((ServicePlayer) player).setInitialVideoSize(null, container, bounds);
        control = (VideoPresentationControl) player.getControl("org.dvb.media.VideoPresentationControl");
    }

    public void tearDown() throws Exception
    {
        player.close();
        vd.setVideoConfiguration(defConfig);
        vd = null;
        helper = null;
        control = null;
        player = null;
        super.tearDown();
    }

    // Test section

    public void testGetInputVideoSize()
    {
        Dimension size = vd.getCurrentConfiguration().getPixelResolution();
        assertEquals("Size does not match", size, control.getInputVideoSize());
    }

    public void testGetVideoSize()
    {
        AWTVideoSize size = new AWTVideoSize(new Rectangle(640, 480), new Rectangle(640, 480));
        assertEquals("Width does not match", size.getDestination().width, control.getVideoSize().width);
        assertEquals("Height does not match", size.getDestination().height, control.getVideoSize().height);
    }

    public void testVideoAreas() throws Exception
    {
        URL dataFile = VideoPresentationControlTest.class.getResource("VPCTestConfig.xml");
        Vector testData = VPCTestDataFactory.getTestData(dataFile.toExternalForm());
        for (int i = 0; i < testData.size(); i++)
        {
            VPCTestData td = (VPCTestData) testData.elementAt(i);
            if (td.dfc == VideoFormatControl.DFC_PLATFORM)
            {
                System.out.println("NOTE: VideoPresentationControlTest.testVideoAreas "
                        + "placing the platform under TV Behavior Control (DFC_PLATFORM)");
                cma.setDFC(vd.getHandle(), td.dfc);
                continue;
            }
            System.out.println("Testing with test data set #" + (i + 1) + " = " + td);

            // reset the bounds for the video
            cma.setBounds(vd.getHandle(), new ScalingBounds());

            cma.cannedSetAspectRatio(vd.getHandle(), td.ar);
            cma.setDFC(vd.getHandle(), td.dfc);
            cma.cannedSetAFD(vd.getHandle(), td.afd);
            if (td.ar == VideoFormatControl.ASPECT_RATIO_4_3)
                cma.cannedSetVideoInputSize(vd.getHandle(), CannedMediaAPI.small.getSize());
            else
                cma.cannedSetVideoInputSize(vd.getHandle(), CannedMediaAPI.medium.getSize());
            CannedVideoDevice.HVideoConfigurationHelper hvc = new CannedVideoDevice.HVideoConfigurationHelper(vd,
                    new Dimension(td.parWidth, td.parHeight), new Dimension(td.scrWidth, td.scrHeight));
            vd.setVideoConfiguration(hvc);

            HScreenRectangle rect = control.getActiveVideoArea();
            assertEquals("AVA.X position is incorrect, " + td, td.ava.x, rect.x, 0.001f);
            assertEquals("AVA.Y position is incorrect, " + td, td.ava.y, rect.y, 0.001f);
            assertEquals("AVA.Width is incorrect, " + td, td.ava.width, rect.width, 0.001f);
            assertEquals("AVA.Height is incorrect, " + td, td.ava.height, rect.height, 0.001f);

            rect = control.getTotalVideoArea();
            assertEquals("TVA.X position is incorrect, " + td, td.tva.x, rect.x, 0.001f);
            assertEquals("TVA.Y position is incorrect, " + td, td.tva.y, rect.y, 0.001f);
            assertEquals("TVA.Width is incorrect, " + td, td.tva.width, rect.width, 0.001f);
            assertEquals("TVA.Height is incorrect, " + td, td.tva.height, rect.height, 0.001f);

            rect = control.getActiveVideoAreaOnScreen();
            assertEquals("AVAOS.X position is incorrect, " + td, td.avaos.x, rect.x, 0.001f);
            assertEquals("AVAOS.Y position is incorrect, " + td, td.avaos.y, rect.y, 0.001f);
            assertEquals("AVAOS.Width is incorrect, " + td, td.avaos.width, rect.width, 0.001f);
            assertEquals("AVAOS.Height is incorrect, " + td, td.avaos.height, rect.height, 0.001f);

            rect = control.getTotalVideoAreaOnScreen();
            assertEquals("TVAOS.X position is incorrect, " + td, td.tvaos.x, rect.x, 0.001f);
            assertEquals("TVAOS.Y position is incorrect, " + td, td.tvaos.y, rect.y, 0.001f);
            assertEquals("TVAOS.Width is incorrect, " + td, td.tvaos.width, rect.width, 0.001f);
            assertEquals("TVAOS.Height is incorrect, " + td, td.tvaos.height, rect.height, 0.001f);
        }
    }

    public void testSupportsClipping()
    {
        assertTrue("Control should support clipping", control.supportsClipping());
        cma.cannedSetClipping(vd.getHandle(), false);
        assertFalse("Control should not support clipping", control.supportsClipping());
    }

    public void testGetAndSetClipRegion()
    {
        assertTrue("Control should support clipping", control.supportsClipping());
        Rectangle origRegion = control.getClipRegion();
        assertNotNull("Returned value should not be null", origRegion);

        Rectangle newRegion = new Rectangle(320, 240);
        control.setClipRegion(newRegion);

        assertEquals("Returned value does not match expected result", newRegion, control.getClipRegion());
    }

    public void testSupportsArbitraryHorizontalScaling()
    {
        float[] range = control.supportsArbitraryHorizontalScaling();
        assertNotNull("Array should not be null", range);
        assertEquals("Low value is incorrect", 0.5f, range[0], 0.001f);
        assertEquals("High value is incorrect", 2.0f, range[1], 0.001f);

        ScalingCaps caps = new ScalingCaps();
        caps.setIsArbitraryFlag(false);
        cma.cannedSetScalingCaps(vd.getHandle(), caps);
        assertNull("Array should be null", control.supportsArbitraryHorizontalScaling());
    }

    public void testSupportsArbitraryVerticalScaling()
    {
        float[] range = control.supportsArbitraryVerticalScaling();
        assertNotNull("Array should not be null", range);
        assertEquals("Low value is incorrect", 0.5f, range[0], 0.001f);
        assertEquals("High value is incorrect", 2.0f, range[1], 0.001f);

        ScalingCaps caps = new ScalingCaps();
        caps.setIsArbitraryFlag(false);
        cma.cannedSetScalingCaps(vd.getHandle(), caps);
        assertNull("Array should be null", control.supportsArbitraryVerticalScaling());
    }

    public void testGetHorizontalScalingFactors()
    {
        assertNull("Array should be null", control.getHorizontalScalingFactors());

        ScalingCaps caps = new ScalingCaps();
        caps.setIsArbitraryFlag(false);
        caps.setHorizCaps(new float[] { 0.5f, 1.0f, 2.0f });
        cma.cannedSetScalingCaps(vd.getHandle(), caps);
        float[] range = control.getHorizontalScalingFactors();
        assertNotNull("Array should not be null", range);
        assertEquals("Low value is incorrect", 0.5f, range[0], 0.001f);
        assertEquals("High value is incorrect", 1.0f, range[1], 0.001f);
        assertEquals("High value is incorrect", 2.0f, range[2], 0.001f);
    }

    public void testGetVerticalScalingFactors()
    {
        assertNull("Array should be null", control.getVerticalScalingFactors());

        ScalingCaps caps = new ScalingCaps();
        caps.setIsArbitraryFlag(false);
        caps.setVertCaps(new float[] { 0.5f, 1.0f, 2.0f });
        cma.cannedSetScalingCaps(vd.getHandle(), caps);
        float[] range = control.getVerticalScalingFactors();
        assertNotNull("Array should not be null", range);
        assertEquals("Low value is incorrect", 0.5f, range[0], 0.001f);
        assertEquals("High value is incorrect", 1.0f, range[1], 0.001f);
        assertEquals("High value is incorrect", 2.0f, range[2], 0.001f);
    }

    public void testGetPositionCapability()
    {
        assertEquals("Positioning capability is incorrect", VideoPresentationControl.POS_CAP_FULL,
                control.getPositioningCapability());
        cma.cannedSetPositionCapability(vd.getHandle(), VideoPresentationControl.POS_CAP_OTHER);
        assertEquals("Positioning capability is incorrect", VideoPresentationControl.POS_CAP_OTHER,
                control.getPositioningCapability());
    }

}
