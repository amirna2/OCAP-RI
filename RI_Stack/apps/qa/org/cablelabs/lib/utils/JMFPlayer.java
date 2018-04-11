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

package org.cablelabs.lib.utils;

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

import org.apache.log4j.Logger;
import org.ocap.shared.media.BeginningOfContentEvent;

public class JMFPlayer implements ControllerListener
{
    private static Logger log = Logger.getLogger(JMFPlayer.class);
    
    private Player m_player;

    private boolean m_initialized = false;

    private boolean m_started = false;
    
    private static JMFPlayer s_instance = new JMFPlayer();

    private JMFPlayer()
    {
        
    }
    
    public static JMFPlayer getInstance()
    {
        return s_instance;
    }
    
    public boolean start(String urlStr)
    {
        if (log.isInfoEnabled())
        {
            log.info("JMFPlayer start() - using URL: " + urlStr);
        }

        try
        {
            // Stop, deallocate and close the player instance if it is not
            // already closed.
            destroy();
            // Initialize the player with a controllerListener object and start
            // it.
            if (!m_initialized)
            {
                if (log.isInfoEnabled())
                {
                    log.info("JMFPlayer creating player");
                }
                m_player = Manager.createPlayer(new MediaLocator(urlStr));
                m_player.addControllerListener(this);
                m_initialized = true;
            }
            if (log.isInfoEnabled())
            {
                log.info("JMFPlayer calling m_player.start()");
            }
            m_player.start();
            if (log.isInfoEnabled())
            {
                log.info("JMFPlayer returned from m_player.start()");
            }

            if (m_player.getState() == Controller.Started)
            {
                m_started = true;
                if (log.isInfoEnabled())
                {
                    log.info("JMFPlayer started");
                }
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("JMFPlayer not started immediately.. Wait for Controller event for starting");
                }
            }
        }
        catch (Exception e)
        {
            if (log.isErrorEnabled())
            {
                log.error("Exception starting JMFPlayer", e);
            }
        }
        return m_started;
    }

    public void controllerUpdate(ControllerEvent event)
    {
        if (log.isInfoEnabled())
        {
            log.info("JMFPlayer received controller event: " + event);
        }
        if (event instanceof org.ocap.shared.media.EndOfContentEvent)
        {
            if (log.isInfoEnabled())
            {
                log.info("calling EndOfContentEvent " + event.toString());
            }
            destroy();
            if (log.isInfoEnabled())
            {
                log.info("after calling EndOfContentEvent");
            }
        }
        else if (event instanceof StartEvent)
        {
            // check for visibility and set it to false if it is not already
            // invisible
            // mark that the player has started.
            m_started = true;
            if (log.isInfoEnabled())
            {
                log.info("Start event called");
            }
        }
        else if (event instanceof org.ocap.shared.media.BeginningOfContentEvent)
        {
            m_player.setRate((float) 1.0);
            if (log.isInfoEnabled())
            {
                log.info("The new rate is " + ((BeginningOfContentEvent) event).getRate());
                log.info("beginning of content event");
            }
        }
        else if (event instanceof RateChangeEvent)
        {
            if (log.isInfoEnabled())
            {
                log.info("The new rate is " + ((RateChangeEvent) event).getRate());
            }
        }
        else if (event instanceof ControllerErrorEvent)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Controller error event");
            }
            destroy();
        }
        else if (event instanceof EndOfMediaEvent)
        {
            if (log.isInfoEnabled())
            {
                log.info("calling EndOfMediaEvent");
            }
            destroy();
            if (log.isInfoEnabled())
            {
                log.info("after calling EndOfMediaEvent");
            }
        }
        else if (event instanceof StopAtTimeEvent)
        {
            if (log.isInfoEnabled())
            {
                log.info("calling StopAtTimeEvent");
            }
            destroy();
            if (log.isInfoEnabled())
            {
                log.info("after calling StopAtTimeEvent");
            }
        }
        else if (event instanceof StopByRequestEvent)
        {
            if (log.isInfoEnabled())
            {
                log.info("calling StopByRequestEvent");
            }
            destroy();
            if (log.isInfoEnabled())
            {
                log.info("after calling StopByRequestEvent");
            }
        }
        else if (event instanceof ControllerErrorEvent)
        {
            if (log.isErrorEnabled())
            {
                log.error("calling ControllerErrorEvent " + event.toString());
            }
            destroy();
            if (log.isInfoEnabled())
            {
                log.info("after calling ControllerErrorEvent");
            }
        }
        else if (event instanceof StopEvent)
        {
            if (log.isInfoEnabled())
            {
                log.info("calling StopEvent ");
            }
            destroy();
            if (log.isInfoEnabled())
            {
                log.info("after calling StopEvent ");
            }
        }
    }

    public synchronized void destroy()
    {
        if (log.isInfoEnabled())
        {
            log.info("inside destroy---");
        }
        if (m_player != null)
        {
            try
            {
                m_player.stop();
                if (log.isInfoEnabled())
                {
                    log.info("after stop----");
                }
                m_player.deallocate();
                m_player.close();
                m_player.removeControllerListener(this);
                m_player = null;
                m_initialized = false;
                m_started = false;
            }
            catch (Throwable t)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Exception in destroy(): " + t.getMessage());
                }
            }
        }
    }

    public float changePlayerRate(float rateValue)
    {
        float retVal = Float.NaN;
        if (m_player != null)
        {
            retVal = m_player.setRate(rateValue);
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("Cannot change rate. Player is null");
            }
        }
        return retVal;
    }
    
    /**
     * Returns the current state of the Player
     * @return the current state of the Player, or -1 if an error occurs
     */
    public int getPlayerState()
    {
        if (m_player != null)
        {
            return m_player.getState();
        }
        return -1;
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
