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

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.media.Player;
import javax.tv.media.AWTVideoSize;
import javax.tv.media.AWTVideoSizeControl;

import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.havi.ui.HGraphicsConfiguration;
import org.havi.ui.HScreenRectangle;
import org.dvb.media.VideoFormatControl;
import org.dvb.media.BackgroundVideoPresentationControl;
import org.dvb.media.VideoTransformation;

import org.cablelabs.impl.dvb.ui.DVBBufferedImagePeer.Factory;
import org.cablelabs.impl.media.CannedGraphicsDevice;
import org.cablelabs.impl.media.CannedHScreen;
import org.cablelabs.impl.media.JMFBaseInterfaceTest;
import org.cablelabs.impl.media.JMFFactory;
import org.cablelabs.impl.media.mpe.CannedMediaAPI;

/**
 * AWTVideoSizeControlTest
 * 
 * @author Joshua Keplinger
 * 
 */
public class AWTVideoSizeControlTest extends JMFBaseInterfaceTest
{

    private AWTVideoSizeControl control;

    private Player player;

    private PlayerHelper helper;

    private JMFFactory factory;

    public static InterfaceTestSuite isuite(ImplFactory factory)
    {
        return new InterfaceTestSuite(AWTVideoSizeControlTest.class, factory);
    }

    public AWTVideoSizeControlTest(String name, Class clazz, ImplFactory factory)
    {
        super(name, clazz, factory);
        this.factory = (JMFFactory) factory;
    }

    public AWTVideoSizeControlTest(String name, ImplFactory factory)
    {
        this(name, Player.class, factory);
        this.factory = (JMFFactory) factory;
    }

    public void setUp() throws Exception
    {
        super.setUp();

        player = (Player) createImplObject();
        control = (AWTVideoSizeControl) player.getControl("javax.tv.media.AWTVideoSizeControl");

        CannedControllerListener listener = new CannedControllerListener(1);
        player.addControllerListener(listener);
        helper = new PlayerHelper(player, listener);
        helper.prefetchPlayer();
    }

    public void tearDown() throws Exception
    {
        player.close();
        control = null;
        player = null;
        helper = null;

        super.tearDown();
    }

    // Test section

    public void testGetAndSetSize() throws Exception
    {
        helper.startPlayer();
        factory.movePlayerToPresenting(player);
        assertTrue("Player is not presenting", ((AbstractPlayer) player).isPresenting());
        AWTVideoSize origSize = control.getSize();
        assertNotNull("Returned value should not be null", origSize);

        CannedGraphicsDevice cgd = (CannedGraphicsDevice) CannedHScreen.getInstance().getDefaultHGraphicsDevice();
        HGraphicsConfiguration hgc = new CannedGraphicsDevice.HGraphicsConfigurationHelper(cgd, new Dimension(1, 1),
                CannedMediaAPI.medium.getSize(), new HScreenRectangle(0.0f, 0.0f, 1.0f, 1.0f));
        cgd.setGraphicsConfiguration(hgc);

        AWTVideoSize newSize = new AWTVideoSize(CannedMediaAPI.medium, CannedMediaAPI.medium);
        assertTrue(control.setSize(newSize));

        assertEquals("Sizes do not match", newSize, control.getSize());

        cgd.setGraphicsConfiguration(cgd.getDefaultConfiguration());

    }

    public void testGetDefaultSize() throws Exception
    {
        // generic test
        AWTVideoSize size = new AWTVideoSize(CannedMediaAPI.small, CannedMediaAPI.small);
        assertEquals("Size does not match", size.getSource(), control.getDefaultSize().getSource());
        assertEquals("Size does not match", size.getDestination(), control.getDefaultSize().getDestination());

        // test while presenting & under application control
        helper.startPlayer();
        factory.movePlayerToPresenting(player);
        assertTrue("Player is not presenting", ((AbstractPlayer) player).isPresenting());
        assertEquals("Size does not match", size.getSource(), control.getDefaultSize().getSource());
        assertEquals("Size does not match", size.getDestination(), control.getDefaultSize().getDestination());

        // test while presenting & under platform control
        VideoFormatControl vfc = (VideoFormatControl) player.getControl("org.dvb.media.VideoFormatControl");
        BackgroundVideoPresentationControl bvpc = (BackgroundVideoPresentationControl) player.getControl("org.dvb.media.BackgroundVideoPresentationControl");

        bvpc.setVideoTransformation(vfc.getVideoTransformation(VideoFormatControl.DFC_PLATFORM));

        assertEquals("Size does not match", size.getSource(), control.getDefaultSize().getSource());
        assertEquals("Size does not match", size.getDestination(), control.getDefaultSize().getDestination());
    }

    public void testGetSourceVideoSize()
    {
        AWTVideoSize size = new AWTVideoSize(CannedMediaAPI.small, CannedMediaAPI.small);
        control.setSize(size);
        Dimension dim = control.getSourceVideoSize();
        assertNull("Dimension should be null with non-presenting Player", dim);
    }

    public void testCheckSize()
    {
        helper.startPlayer();

        AWTVideoSize size = new AWTVideoSize(CannedMediaAPI.small, CannedMediaAPI.small);
        AWTVideoSize newSize = control.checkSize(size);
        assertEquals("Size does not match", CannedMediaAPI.small, newSize.getSource());
        assertEquals("Size does not match", CannedMediaAPI.small, newSize.getDestination());

        size = new AWTVideoSize(new Rectangle(1280, 800), new Rectangle(1280, 800));
        newSize = control.checkSize(size);
        assertEquals("Size does not match", CannedMediaAPI.small, newSize.getSource());
        assertEquals("Size does not match", CannedMediaAPI.small, newSize.getDestination());

    }

    public void xtestMethodsReturnNullWhenNotEnabled()
    {
        AWTVideoSize size = new AWTVideoSize(CannedMediaAPI.small, CannedMediaAPI.small);
        // AVPlayerBase player2 = new CannedAVPlayerBase();
        // player.swapDecoders(player2, false);
        // assertTrue(player.isComponentPlayer());

        assertTrue(control.getSize() == null);
        assertTrue(control.getDefaultSize() == null);
        assertTrue(control.getSourceVideoSize() == null);
        assertTrue(!control.setSize(size));
        assertTrue(control.checkSize(size) == null);
    }

    public void testSetSizeNull()
    {
        // According to the TCK Test
        // javasoft.sqe.tests.api.javax.tv.media.AWTVideoSizeControl.SetSizeTests.testSetNullSize
        // this should throw an npe

        boolean correctExceptionCaught = false;
        try
        {
            try
            {
                control.setSize(null);
            }
            catch (NullPointerException npe)
            {
                correctExceptionCaught = true;
                npe.printStackTrace();
            }
        }
        catch (Exception exc)
        {
            correctExceptionCaught = false;
        }

        assertTrue("setSize(null) did not throw an NPE", correctExceptionCaught);
    }
}
