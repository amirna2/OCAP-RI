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

import java.awt.Rectangle;

import javax.media.GainControl;
import javax.media.Player;
import javax.tv.media.AWTVideoSize;
import javax.tv.media.AWTVideoSizeControl;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.selection.NormalContentEvent;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextListener;

import org.ocap.net.OcapLocator;

public class VideoPlayer implements ServiceContextListener
{
    private ServiceContext m_serviceContext = null;

    private AWTVideoSizeControl m_videoSizeCtrl = null;

    private GainControl m_gainControl = null;

    private Rectangle m_videoBounds;

    private boolean m_isPlaying = false;

    private VideoPlayerListener m_listener;

    private int m_sourceID = -1;

    public VideoPlayer()
    {
        try
        {
            m_serviceContext = ServiceContextFactory.getInstance().createServiceContext();

            m_serviceContext.addListener(this);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /*
	 * 
	 */
    public void addListener(VideoPlayerListener listener)
    {
        m_listener = listener;
    }

    /*
     * get current source id. returns -1 if it's not selected
     */
    public int getSourceID()
    {
        return m_sourceID;
    }

    /*
	 * 
	 */
    public void tune(int sourceID)
    {
        m_sourceID = sourceID;

        try
        {
            OcapLocator ocapLoc = new OcapLocator(sourceID);

            if (ocapLoc != null)
            {
                SIManager siManager = (SIManager) SIManager.createInstance();
                if (siManager != null && m_serviceContext != null)
                {
                    // Retrieve the service corresponding to the locator
                    Service service = siManager.getService(ocapLoc);

                    System.out.println("##### TUNING: " + sourceID);

                    m_serviceContext.select(service);

                    // this is false until tune is complete
                    m_isPlaying = false;
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /*
	 * 
	 */
    public void stop()
    {
        if (m_serviceContext != null && m_isPlaying)
        {
            System.out.println("##### m_serviceContext.stop()");
            m_serviceContext.stop();
            m_isPlaying = false;
        }
    }

    public boolean isPlaying()
    {
        return m_isPlaying;
    }

    /*
	 * 
	 */
    public void setMute(boolean set)
    {
        if (m_gainControl != null)
        {
            System.out.println("##### setMute(" + set + ")");
            m_gainControl.setMute(set);
        }
    }

    /*
	 * 
	 */
    public boolean isMuted()
    {
        if (m_gainControl != null)
        {
            return m_gainControl.getMute();
        }
        else
        {
            return false;
        }
    }

    /*
     * set the video bounds w/ the current video size control
     */
    public void setVideoBounds(Rectangle rect)
    {
        if (m_videoSizeCtrl != null)
        {
            Rectangle src = new Rectangle(0, 0, 640, 480);

            AWTVideoSize size = new AWTVideoSize(src, rect);

            System.out.println("VideoPlayer.setVideoBounds: " + rect.x + " " + rect.y + " " + rect.width + " "
                    + rect.height);
            m_videoSizeCtrl.setSize(size);

            m_videoBounds = rect;

        }
    }

    // new video bounds will be effective at next tune complete
    public void setNextVideoBounds(Rectangle rect)
    {
        m_videoBounds = rect;
    }

    public Rectangle getCurrentVideoBounds()
    {
        if (m_videoSizeCtrl != null)
            return m_videoSizeCtrl.getSize().getDestination();
        else
            return new Rectangle();
    }

    /*
     * Interface ServiceContextListener
     * 
     * Notifies the ServiceContextListener of an event generated by a
     * ServiceContext.
     * 
     * Parameters: e - The generated event.
     */
    public void receiveServiceContextEvent(ServiceContextEvent ev)
    {
        if (ev != null)
        {
            if (ev instanceof NormalContentEvent)
            {
                ServiceContentHandler[] schArray = ev.getServiceContext().getServiceContentHandlers();

                if (schArray[0] != null && schArray[0] instanceof Player)
                {
                    Player player = (Player) schArray[0];

                    m_gainControl = player.getGainControl();

                    // keep the video size control
                    m_videoSizeCtrl = (AWTVideoSizeControl) player.getControl("javax.tv.media.AWTVideoSizeControl");

                    // set the new video bounds
                    if (m_videoBounds != null)
                    {
                        setVideoBounds(m_videoBounds);
                    }

                    m_isPlaying = true;

                    if (m_listener != null) m_listener.tuneComplete(m_sourceID);
                }
            }
        }
    }
}
