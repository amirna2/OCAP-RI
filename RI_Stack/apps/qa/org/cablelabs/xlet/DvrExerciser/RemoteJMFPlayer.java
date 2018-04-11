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
package org.cablelabs.xlet.DvrExerciser;

import java.io.IOException;

import javax.media.Controller;
import javax.media.ControllerErrorEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoPlayerException;
import javax.media.Player;
import javax.media.StartEvent;

import org.ocap.hn.ContentServerNetModule;
import org.ocap.hn.content.ContentItem;

public class RemoteJMFPlayer
{
    private Player m_player;

    boolean m_initialized;

    private RemoteJMFStatusBox m_statusBox = null;

    public RemoteJMFPlayer()
    {
        m_statusBox = new RemoteJMFStatusBox(50, 10, 375, 30);
        m_statusBox.update(RemoteJMFStatusBox.COLOR_INIT, RemoteJMFStatusBox.MSG_INIT);
    }

    public void start(ContentServerNetModule mediaServer, ContentItem contentItem)
    {
        //look up the URL for the content item and pass that in to start
        start(((String[])contentItem.getRootMetadataNode().getMetadata("didl-lite:res"))[0]);
    }
    
    public void start(String urlStr)
    {
        System.out.println("RemoteJMFPlayer start() - using URL: " + urlStr);

        try
        {
            DvrExerciser.getInstance().m_scene.add(m_statusBox);
            DvrExerciser.getInstance().m_scene.setVisible(true);
            DvrExerciser.getInstance().m_scene.show();
            DvrExerciser.getInstance().m_scene.requestFocus();
            
            if (!m_initialized)
            {
                System.out.println("RemoteJMFPlayer creating player");
                ControllerListener listener = new ControllerListenerImpl();
                m_player = Manager.createPlayer(new MediaLocator(urlStr));
                m_player.addControllerListener(listener);
                m_initialized = true;
            }

            System.out.println("RemoteJMFPlayer calling player.start");
            m_player.start();
            m_statusBox.update(RemoteJMFStatusBox.COLOR_PENDING, RemoteJMFStatusBox.MSG_PENDING);
            DvrExerciser.getInstance().m_scene.repaint();
            System.out.println("RemoteJMFPlayer returned from player.start");

            if (m_player.getState() == Controller.Started)
            {
                m_statusBox.update(RemoteJMFStatusBox.COLOR_PLAY, RemoteJMFStatusBox.MSG_PLAY_1);
                DvrExerciser.getInstance().m_scene.repaint();
                System.out.println("RemoteJMFPlayer started");
                /*
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
                        statusBox.update(RemoteJMFStatusBox.COLOR_STOP, "Failed - No Scene available");
                        System.out.println("unable to get scene - unable to add component: " + component);
                    }
                }
                else
                {
                    System.out.println("standalonejmfplayer no component, show Xlet as component");
                }
                */
            }
            else
            {
                System.out.println("RemoteJMFPlayer not started");
            }
        }
        catch (IOException e)
        {
            m_statusBox.update(RemoteJMFStatusBox.COLOR_STOP, "Failed - Unable to locate content");
            e.printStackTrace(); // To change body of catch statement use File |
                                 // Settings | File Templates.
        }
        catch (NoPlayerException e)
        {
            m_statusBox.update(RemoteJMFStatusBox.COLOR_STOP, "Failed - Unsupported Player Type");
            e.printStackTrace(); // To change body of catch statement use File |
                                 // Settings | File Templates.
        }
        DvrExerciser.getInstance().m_scene.repaint();
    }

    protected void stop()
    {
        if (m_initialized)
        {
            if (m_player != null)
            {
                m_player.close();
            }
            DvrExerciser.getInstance().m_scene.remove(m_statusBox);
        }
    }

    private class ControllerListenerImpl implements ControllerListener
    {
        public void controllerUpdate(ControllerEvent event)
        {
            System.out.println("RemoteJMFPlayer received controller event: " + event);

            if (event instanceof StartEvent)
            {
                /*
                if (null != m_player)
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
                */
                m_statusBox.update(RemoteJMFStatusBox.COLOR_PLAY, RemoteJMFStatusBox.MSG_PLAY_1);
            }
            if (event instanceof ControllerErrorEvent)
            {
                m_statusBox.update(RemoteJMFStatusBox.COLOR_STOP, RemoteJMFStatusBox.MSG_FAILED);
            }
            if (event instanceof org.ocap.shared.media.BeginningOfContentEvent)
            {
                m_statusBox.update(RemoteJMFStatusBox.COLOR_PLAY, RemoteJMFStatusBox.MSG_PLAY_1);
            }
            if (event instanceof org.ocap.shared.media.BeginningOfContentEvent)
            {
                m_statusBox.update(RemoteJMFStatusBox.COLOR_PLAY, RemoteJMFStatusBox.MSG_PLAY_1);
            }
            if (event instanceof org.ocap.shared.media.EndOfContentEvent)
            {
                m_statusBox.update(RemoteJMFStatusBox.COLOR_STOP, RemoteJMFStatusBox.MSG_STOP);
                if (m_player != null)
                {
                    m_player.close();
                }
            }
            DvrExerciser.getInstance().m_scene.repaint();
        }
    }

    public int getPlayerState()
    {
        return m_player.getState();
    }
}
