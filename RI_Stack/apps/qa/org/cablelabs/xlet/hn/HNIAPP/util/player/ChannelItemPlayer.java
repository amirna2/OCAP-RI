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

import javax.media.Player;
import javax.tv.service.Service;
import javax.tv.service.selection.InsufficientResourcesException;
import javax.tv.service.selection.PresentationTerminatedEvent;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextListener;

import org.cablelabs.xlet.hn.HNIAPP.controller.HomeNetController;
import org.cablelabs.xlet.hn.HNIAPP.util.logging.HomeNetLogging;

/**
 * @author Parthiban Balasubramanian, Cognizant Technology Solutions
 * 
 */
public class ChannelItemPlayer implements ServiceContextListener
{

    private static ChannelItemPlayer c_Player = null;

    private ServiceContext l_serviceContext = null;

    private HomeNetLogging hnLogger = HomeNetLogging.getInstance();

    private Player m_player = null;

    private boolean m_serviceSelected = false;

    private ChannelItemPlayer()
    {

    }

    public static ChannelItemPlayer getInstance()
    {
        if (c_Player == null)
        {
            c_Player = new ChannelItemPlayer();
        }
        return c_Player;
    }

    public void startLiveChannel(Service service)
    {
        // Remove any previous servicecontext listeners
        destroyContextListener();
        // Create a new service context and attach a listener to it.
        createServiceContext();

        // Select a service on the newly created service context
        l_serviceContext.select(service);

    }

    public void createServiceContext()
    {
        ServiceContextFactory factory = ServiceContextFactory.getInstance();
        try
        {
            l_serviceContext = factory.createServiceContext();
        }
        catch (InsufficientResourcesException e)
        {
            e.printStackTrace();
        }
        catch (SecurityException e)
        {
            e.printStackTrace();
        }
        l_serviceContext.addListener(this);
    }

    public void destroyContextListener()
    {
        if (l_serviceContext != null)
        {
            l_serviceContext.removeListener(this);
        }
    }

    public void stopService()
    {
        l_serviceContext.stop();
        // Wait till the presentation context has stopped.
        try
        {
            Thread.sleep(5000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        m_serviceSelected = true;
        destroyContextListener();
        if (!HomeNetController.getInstance().getSceneVisibility())
        {
            HomeNetController.getInstance().setSceneVisibility(true);
        }
    }

    public void receiveServiceContextEvent(ServiceContextEvent event)
    {
        if (event instanceof PresentationTerminatedEvent)
        {
            if (!HomeNetController.getInstance().getSceneVisibility())
            {
                HomeNetController.getInstance().setSceneVisibility(true);
            }
            hnLogger.homeNetLogger("Inside Presentation Terminated Event");
        }
        else if (event instanceof javax.tv.service.selection.NormalContentEvent)
        {
            hnLogger.homeNetLogger("Inside NormalContentEvent");
            m_serviceSelected = true;
            // check for visibility and set it to false if it is not already
            // invisible
            if (HomeNetController.getInstance().getSceneVisibility())
            {
                HomeNetController.getInstance().setSceneVisibility(false);
            }

        }
    }

    public void setContextPlayerRate(float newRate)
    {
        System.out.println("Inside setRate of ChannelItemPlayer");
        ServiceContentHandler[] handlers = l_serviceContext.getServiceContentHandlers();
        if (handlers.length > 0)
        {
            System.out.println("handler class " + handlers[0].getClass().getName());
            m_player = (Player) handlers[0];
            m_player.setRate(newRate);
        }
        else
        {
            System.out.println("No player available, unable to set rate");
        }
    }

    public boolean isServiceSelected()
    {
        return m_serviceSelected;
    }
}
