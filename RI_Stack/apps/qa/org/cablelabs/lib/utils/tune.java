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
/**
 * Commented out non-essential packages.
 * 
 * @author bforan
 */
package org.cablelabs.lib.utils;

//Import OCAP packages.
import org.ocap.net.OcapLocator;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.selection.*;
import javax.tv.media.AWTVideoSizeControl;
import javax.tv.media.AWTVideoSize;
import javax.media.GainControl;
import javax.media.*;

//import javax.tv.locator.InvalidLocatorException;

public class tune implements ServiceContextListener
{
    // Scaling
    private AWTVideoSizeControl _crtl;

    private ServiceContext _serviceContext = null;

    private SIManager _siManager;

    private OcapLocator _ocapLoc;

    private int _frequency;

    private int _programNumber;

    private int _qam;

    private GainControl _volumeControl = null;

    private long tuningStart, tuningStop = 0;

    public tune(int frequency, int programNumber, int qam)
    {
        System.out.println("BFSTune: frequency: " + frequency + " programNumber: " + programNumber + " qam: " + qam);

        _frequency = frequency;
        _programNumber = programNumber;
        _qam = qam;

        try
        {
            _serviceContext = ServiceContextFactory.getInstance().createServiceContext();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        _serviceContext.addListener(this);

        _siManager = (SIManager) SIManager.createInstance();
        if (_siManager == null) System.out.println("BFS Tune: failed to get siManager instance");
        start();
    }

    public void start()
    {
        Service service;
        try
        {
            _ocapLoc = new OcapLocator(_frequency, _programNumber, _qam);
            if (_ocapLoc == null)
            {
                System.out.println("BFS Tune : ocaploc = null");
                return;
            }
            service = _siManager.getService(_ocapLoc);
            _serviceContext.select(service);
        }
        catch (Exception e)
        {
            System.out.println("BFS Tune : ocap locater - exception: " + e);
            e.printStackTrace();
        }
    }

    public void receiveServiceContextEvent(ServiceContextEvent event)
    {
        ServiceContentHandler[] schArray;

        if (event == null)
        {
            return;
        }

        ServiceContext sc = event.getServiceContext();

        if (sc == null)
        {
            return;
        }

        Service serv = sc.getService();

        if (event instanceof NormalContentEvent)
        {

            System.out.println("\n\n********************************");
            System.out.println("\n\nBFS Tune :receiveServiceContextEvent - "
                    + "NormalContentEvent received from Service Context\n");
            System.out.println("********************************\n");
            schArray = sc.getServiceContentHandlers();

            if (0 != schArray.length)
            {
                if (schArray[0] != null && schArray[0] instanceof Player)
                {
                    Player player = (Player) schArray[0];

                    // set the video size to full screen
                    java.awt.Rectangle scale = new java.awt.Rectangle(0, 0, 640, 480);
                    AWTVideoSize size = new AWTVideoSize(scale, scale);
                    _crtl = (AWTVideoSizeControl) player.getControl("javax.tv.media.AWTVideoSizeControl");
                    _crtl.setSize(size);
                    _volumeControl = player.getGainControl();
                }
            }

        }
        else if (event instanceof SelectionFailedEvent)
        {
            System.out.println("\nBFS Tune:receiveServiceContextEvent - "
                    + "SelectionFailedEvent received from Service Context\n");
        }
        else
        {
            System.out.println("\nBFS Tune:receiveServiceContextEvent - "
                    + "Unmatched event received from Service Context\n");
        }
    }
}
