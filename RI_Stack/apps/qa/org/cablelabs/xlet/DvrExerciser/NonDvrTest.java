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

import javax.media.Player;
import javax.tv.media.AWTVideoSize;
import javax.tv.media.AWTVideoSizeControl;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.selection.NormalContentEvent;
import javax.tv.service.selection.PresentationTerminatedEvent;
import javax.tv.service.selection.SelectionFailedEvent;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextListener;

import org.dvb.media.VideoTransformation;
import org.dvb.service.selection.DvbServiceContext;
import org.havi.ui.HScreenPoint;

/**
 * This is a 'workbench' class that can be used to exercise various kinds of DVR
 * functionality.
 * 
 * It was designed to work in concert with an xlet application that that can
 * instantiate it and
 * 
 * @author andy
 * 
 */
public class NonDvrTest implements ServiceContextListener
{
    protected DvrExerciser m_dvrExerciser = null;

    protected DvbServiceContext m_serviceContext;

    // lock used for notification of service context events
    protected Object m_objSceLock;

    protected boolean m_bNormalContentEventReceived;

    protected boolean m_bPresentationTerminatedEventReceived = false;

    protected SIManager m_siManager = null;

    /**
     * Constructor
     * 
     * Just initializes an instance of the class for use
     * 
     * @param dvrExerciser
     */
    protected NonDvrTest()
    {
        // get the service manager
        m_siManager = SIManager.createInstance();
        m_siManager.setPreferredLanguage("eng");

        // create a general lock object for event reception synchronization
        m_objSceLock = new Object();
        
        init(DvrExerciser.getInstance());        
    }

    /**
     * Obtains a single service context for use by the application, and
     * associates it with a ScaledVideoManager that places the video in the
     * upper-left quadrant of the screen.
     * 
     * @return <code>true</code> if the service context and scaled video manager
     *         are instantiated and set up without error, <code>false
     * </code> otherwise.
     */
    public boolean init(DvrExerciser dvrExerciser)
    {
        m_dvrExerciser = dvrExerciser;

        boolean bRetVal = true;
        m_dvrExerciser.logIt("initTest():entry");
        ServiceContextFactory scf = ServiceContextFactory.getInstance();
        try
        {
            // create the service context for general use
            m_serviceContext = (DvbServiceContext) scf.createServiceContext();

            m_serviceContext.addListener(this);
 
            // set default video window to quarter screen in upper left corner
            VideoTransformation videoTransform = new VideoTransformation(null, (float) 0.5, (float) 0.5,
                    new HScreenPoint(0, 0));
            
            m_serviceContext.setDefaultVideoTransformation(videoTransform);
        }
        catch (Exception e)
        {
            bRetVal = false;
            m_dvrExerciser.logIt("DvrTest: unable to create service context, exception: " + e.toString());
            e.printStackTrace();
        }
        m_dvrExerciser.logIt("initTest():exit");
        return bRetVal;
    }

    /**
     * Sizes and positions the video as described by the specified AWTVideoSize
     * parameter.
     * 
     * @param avs
     *            describes the position and size of the video.
     */
    public void resizeVideo(AWTVideoSize avs)
    {
        Player player = getServiceContextPlayer();
        if (null != player)
        {
            AWTVideoSizeControl avsc = (AWTVideoSizeControl) player.getControl("javax.tv.media.AWTVideoSizeControl");
            avsc.setSize(avs);
        }
        else
        {
            m_dvrExerciser.logIt("Service context player not found");
        }
    }

    /**
     * General handler for events generated by the service context. This method
     * is used to handle all events that are of interest to this application by
     * setting internal flags based on the event received.
     * 
     * It is assumed that routines expecting a service context event are
     * blocking their execution threads using m_objLock. Therefore, this
     * function calls notifyAll() on m_objLock to release those threads. I
     * suppose it would be more elegant to use event-specific locks, but this
     * application is intended to be as simple as practicable.
     * 
     * Note that all received events are displayed in the log.
     */
    public void receiveServiceContextEvent(ServiceContextEvent event)
    {
        // get the class name w/o the package prefix and...
        String name = event.getClass().getName();
        int firstChar = name.lastIndexOf('.') + 1;
        if (0 < firstChar)
        {
            name = name.substring(firstChar);
        }

        // ...display it
        m_dvrExerciser.logIt("Received ServiceContextEvent: " + name);

        // handle all events of interest
        if (event instanceof SelectionFailedEvent)
        {
            m_dvrExerciser.logIt("   Selection failed!!");
        }
        else if (event instanceof PresentationTerminatedEvent)
        {
            m_bPresentationTerminatedEventReceived = true;
        }
        else if (event instanceof NormalContentEvent)
        {
            m_bNormalContentEventReceived = true;
        }

        synchronized (m_objSceLock)
        {
            m_objSceLock.notifyAll();
        }
    }

    /**
     * Tunes to a 'live' service.
     * 
     * Tunes to the 'live' service given a source ID specified as a URI.
     * 
     * This routine will wait until the service is actually selected.
     * 
     * @param uri
     *            the source ID specified in the format of a URI that looks
     *            like: "ocap://source_id".
     * 
     * @return <code>true</code> if the service was successfully selected,
     *         <code>false</code> otherwise.
     */
    public boolean doTuneLive(Service service)
    {
        m_dvrExerciser.logIt("doTuneLive():entry");

        // indicate that we are waiting for a normal content event
        m_bNormalContentEventReceived = false;

        try
        {
            // select the requested service
            m_serviceContext.select(service);
        }
        catch (Exception ex)
        {
            m_dvrExerciser.logIt("Exception in doTuneLive(): " + ex);
            System.err.println("Exception in doTuneLive: " + ex);
            ex.printStackTrace();
        }

        m_dvrExerciser.logIt("doTuneLive():exit, result = " + m_bNormalContentEventReceived + ", service name = "
                + service.getName());

        return true;
    }

    /**
     * Obtains the Player on the current service context.
     * 
     * @return
     */
    public Player getServiceContextPlayer()
    {
        Player retVal = null;

        ServiceContentHandler[] handlers = m_serviceContext.getServiceContentHandlers();

        for (int i = 0; i < handlers.length; i++)
        {
            if (handlers[i] instanceof Player)
            {
                retVal = (Player) handlers[i];
                break;
            }
        }
        return retVal;
    }
}