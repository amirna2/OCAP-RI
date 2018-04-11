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
package org.cablelabs.xlet.StandaloneJMF;

import java.io.IOException;
import java.awt.Component;

import javax.media.Controller;
import javax.media.ControllerClosedEvent;
import javax.media.ControllerErrorEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoPlayerException;
import javax.media.Player;
import javax.media.StartEvent;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;

public class StandalonePlayerXlet implements Xlet
{
    private String arg;

    XletContext ctx;

    private HScene scene;

    private Player player;

    boolean initialized;

    private StatusBox statusBox = null;

    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        String[] args = (String[]) ctx.getXletProperty(XletContext.ARGS);
        arg = args[0];
        this.ctx = ctx;
        System.out.println("standalonejmfplayer initXlet - arg: " + arg);
        statusBox = new StatusBox(50, 10, 375, 30);
        statusBox.update(StatusBox.COLOR_INIT, StatusBox.MSG_INIT);
    }

    public void startXlet() throws XletStateChangeException
    {
        System.out.println("standalonejmfplayer startXlet");

        try
        {
            scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
            if (scene != null)
            {
                scene.add(statusBox);
                scene.setVisible(true);
                scene.show();
                scene.requestFocus();
            }
            if (!initialized)
            {
                System.out.println("standalonejmfplayer creating player");
                ControllerListener listener = new ControllerListenerImpl();
                player = Manager.createPlayer(new MediaLocator(arg));
                player.addControllerListener(listener);
                initialized = true;
            }

            System.out.println("standalonejmfplayer calling player.start");
            player.start();
            statusBox.update(StatusBox.COLOR_PENDING, StatusBox.MSG_PENDING);
            scene.repaint();
            System.out.println("standalonejmfplayer returned from player.start");

            if (player.getState() == Controller.Started)
            {
                statusBox.update(StatusBox.COLOR_PLAY, StatusBox.MSG_PLAY_1);
                scene.repaint();
                System.out.println("standalonejmfplayer started");
                Component component = player.getVisualComponent();
                if (component != null)
                {
                    System.out.println("standalonejmfplayer component available, adding to scene: " + component);
                    if (scene != null)
                    {
                        scene.add(component);
                    }
                    else
                    {
                        statusBox.update(StatusBox.COLOR_STOP, "Failed - No Scene available");
                        System.out.println("unable to get scene - unable to add component: " + component);
                    }
                }
                else
                {
                    System.out.println("standalonejmfplayer no component, show Xlet as component");
                }
            }
            else
            {
                System.out.println("standalonejmfplayer not started");
            }
        }
        catch (IOException e)
        {
            statusBox.update(StatusBox.COLOR_STOP, "Failed - Unable to locate content");
            e.printStackTrace(); // To change body of catch statement use File |
                                 // Settings | File Templates.
        }
        catch (NoPlayerException e)
        {
            statusBox.update(StatusBox.COLOR_STOP, "Failed - Unsupported Player Type");
            e.printStackTrace(); // To change body of catch statement use File |
                                 // Settings | File Templates.
        }
        scene.repaint();
    }

    public void pauseXlet()
    {
        System.out.println("standalonejmfplayer pausexlet");
        releaseResources();
    }

    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
        releaseResources();
    }

    private void releaseResources()
    {
        if (initialized)
        {
            if (player != null)
            {
                player.stop();
            }
            if (scene != null)
            {
                scene.removeAll();
                scene.setVisible(false);
            }
        }
    }

    private class ControllerListenerImpl implements ControllerListener
    {
        public void controllerUpdate(ControllerEvent event)
        {
            System.out.println("standalonejmfplayer received controller event: " + event);

            if (event instanceof StartEvent)
            {
                if (null != player)
                {
                    Component component = player.getVisualComponent();
                    if (component != null)
                    {
                        System.out.println("standalonejmfplayer component available, adding to scene: " + component);
                        if (scene != null)
                        {
                            scene.add(component);
                        }
                    }
                }
                statusBox.update(StatusBox.COLOR_PLAY, StatusBox.MSG_PLAY_1);
                scene.repaint();
            }
            if (event instanceof ControllerErrorEvent)
            {
                statusBox.update(StatusBox.COLOR_STOP, StatusBox.MSG_FAILED);
                scene.repaint();
            }
            if (event instanceof org.ocap.shared.media.BeginningOfContentEvent)
            {
                statusBox.update(StatusBox.COLOR_PLAY, StatusBox.MSG_PLAY_1);
                scene.repaint();
            }
            if (event instanceof org.ocap.shared.media.BeginningOfContentEvent)
            {
                statusBox.update(StatusBox.COLOR_PLAY, StatusBox.MSG_PLAY_1);
                scene.repaint();
            }
            if (event instanceof org.ocap.shared.media.EndOfContentEvent)
            {
                statusBox.update(StatusBox.COLOR_STOP, StatusBox.MSG_STOP);
                scene.repaint();
                if (player != null)
                {
                    player.stop();
                }
            }
        }
    }
}
