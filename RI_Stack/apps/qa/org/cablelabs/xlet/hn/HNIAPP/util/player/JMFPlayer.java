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
package org.cablelabs.xlet.hn.HNIAPP.util.player;

import javax.media.Controller;
import javax.media.ControllerErrorEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.Player;
import javax.media.RateChangeEvent;
import javax.media.StartEvent;
import javax.media.StopAtTimeEvent;
import javax.media.StopByRequestEvent;
import javax.media.StopEvent;

import org.cablelabs.xlet.hn.HNIAPP.controller.HomeNetController;
import org.ocap.shared.media.BeginningOfContentEvent;
import org.ocap.shared.media.EndOfContentEvent;

/**
 * @author Parthiban Balasubramanian, Cognizant Technology Solutions
 * 
 */
public class JMFPlayer implements ControllerListener
{
    private Player m_player;

    private static JMFPlayer l_player = null;

    private boolean m_initialized = false;

    private boolean m_started = false;

    private JMFPlayer()
    {

    }

    public static JMFPlayer getInstance()
    {
        if (l_player == null)
        {
            l_player = new JMFPlayer();
        }
        return l_player;
    }

    public void start(String urlStr)
    {
        System.out.println("JMFPlayer start() - using URL: " + urlStr);

        try
        {
            // Stop, deallocate and close the player instance if it is not
            // already closed.
            destroy();
            // Initialize the player with a controllerListener object and start
            // it.
            if (!m_initialized)
            {
                System.out.println("JMFPlayer creating player");
                m_player = Manager.createPlayer(new MediaLocator(urlStr));
                m_player.addControllerListener(this);
                m_initialized = true;
            }
            System.out.println("JMFPlayer calling m_player.start()");
            m_player.start();
            System.out.println("JMFPlayer returned from m_player.start()");

            if (m_player.getState() == Controller.Started)
            {
                m_started = true;
                System.out.println("JMFPlayer started");
            }
            else
            {
                System.out.println("JMFPlayer not started immediately.. Wait for Controller event for starting");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    public void controllerUpdate(ControllerEvent event)
    {
        System.out.println("JMFPlayer received controller event: " + event);

        if (event instanceof StartEvent)
        {
            // check for visibility and set it to false if it is not already
            // invisible
            if (HomeNetController.getInstance().getSceneVisibility())
            {
                HomeNetController.getInstance().setSceneVisibility(false);
            }
            // mark that the player has started.
            m_started = true;
            System.out.println("Start event called");
        }
        else if (event instanceof org.ocap.shared.media.BeginningOfContentEvent)
        {
            m_player.setRate((float) 1.0);
            System.out.println("The new rate is " + ((BeginningOfContentEvent) event).getRate());
            System.out.println("beginning of content event");
        }
        else if (event instanceof RateChangeEvent)
        {
            System.out.println("The new rate is " + ((RateChangeEvent) event).getRate());
        }
        else if (event instanceof ControllerErrorEvent)
        {
            System.out.println("Controller error event");
            destroy();
        }
        else if (event instanceof EndOfMediaEvent)
        {
            System.out.println("calling EndOfMediaEvent");
            destroy();
            System.out.println("after calling EndOfMediaEvent");
        }
        else if (event instanceof StopAtTimeEvent)
        {
            System.out.println("calling StopAtTimeEvent");
            destroy();
            System.out.println("after calling StopAtTimeEvent");
        }
        else if (event instanceof StopByRequestEvent)
        {
            System.out.println("calling StopByRequestEvent");
            destroy();
            System.out.println("after calling StopByRequestEvent");
        }
        else if (event instanceof ControllerErrorEvent)
        {
            System.out.println("calling ControllerErrorEvent " + event.toString());
            destroy();
            System.out.println("after calling ControllerErrorEvent");
        }
        else if (event instanceof StopEvent)
        {
            System.out.println("calling StopEvent ");
            destroy();
            System.out.println("after calling StopEvent ");
        }
        else if (event instanceof org.ocap.shared.media.EndOfContentEvent)
        {
            System.out.println("calling EndOfContentEvent " + event.toString());
            destroy();
            System.out.println("after calling EndOfContentEvent");
        }

    }

    public synchronized void destroy()
    {
        System.out.println("inside destroy---");
        if (!HomeNetController.getInstance().getSceneVisibility())
        {
            HomeNetController.getInstance().setSceneVisibility(true);
        }
        if (m_player != null)
        {
            try
            {
                m_player.stop();
                System.out.println("after stop----");
                m_player.deallocate();
                m_player.close();
                m_player.removeControllerListener(this);
                m_player = null;
                m_initialized = false;
                m_started = false;
            }
            catch (Throwable t)
            {
                t.printStackTrace();
                System.out.println("inside exception");
            }
        }
    }

    public void changePlayerRate(float rateValue)
    {
        if (null != m_player)
        {
            m_player.setRate(rateValue);
        }
    }

    public int getPlayerState()
    {
        return m_player.getState();
    }

    public Player getPlayerInstance()
    {
        return m_player;
    }

    public boolean isPlayerStarted()
    {
        return m_started;
    }
}
