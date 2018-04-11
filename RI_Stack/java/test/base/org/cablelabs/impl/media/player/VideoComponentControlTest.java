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
import java.awt.Point;
import java.awt.Rectangle;

import javax.media.Player;

import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.ocap.media.VideoComponentControl;

import org.cablelabs.impl.media.JMFBaseInterfaceTest;

/**
 * VideoComponentControlTest
 * 
 * @author Joshua Keplinger
 * 
 */
public class VideoComponentControlTest extends JMFBaseInterfaceTest
{

    private VideoComponentControl control;

    private Player player;

    public static InterfaceTestSuite isuite(ImplFactory factory)
    {
        return new InterfaceTestSuite(VideoComponentControlTest.class, factory);
    }

    public VideoComponentControlTest(String name, Class clazz, ImplFactory factory)
    {
        super(name, clazz, factory);
    }

    public VideoComponentControlTest(String name, ImplFactory factory)
    {
        this(name, Player.class, factory);
    }

    public void setUp() throws Exception
    {
        super.setUp();

        player = (Player) createImplObject();
        control = (VideoComponentControl) player.getControl(VideoComponentControl.class.getName());
    }

    public void tearDown() throws Exception
    {
        player.close();
        control = null;
        player = null;

        super.tearDown();
    }

    public void testGetVisualComponent()
    {
        assertNotNull("Returned video component should not be null", control.getVisualComponent());
    }

    public void testGetBestFit()
    {
        CannedControllerListener listener = new CannedControllerListener(1);
        player.addControllerListener(listener);
        PlayerHelper helper = new PlayerHelper(player, listener);
        helper.prefetchPlayer();
        Container cont = new Container()
        {
            public Point getLocationOnScreen()
            {
                return getLocation();
            }
        };
        cont.setSize(640, 480);
        cont.setLocation(0, 0);
        Rectangle rect = new Rectangle(640, 480);
        Rectangle result = control.getBestFit(cont, rect);
        assertEquals("Returned x location does not match", rect.x, result.x);
        assertEquals("Returned y location does not match", rect.y, result.y);
        assertEquals("Returned width does not match", rect.width, result.width);
        assertEquals("Returned height does not match", rect.height, result.height);
    }
}
