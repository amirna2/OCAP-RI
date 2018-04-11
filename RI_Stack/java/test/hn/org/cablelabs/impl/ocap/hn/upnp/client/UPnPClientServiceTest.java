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

package org.cablelabs.impl.ocap.hn.upnp.client;

import java.lang.reflect.Method;

import java.util.Vector;

import junit.framework.TestCase;

import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.common.UPnPActionImpl;

import org.ocap.hn.upnp.common.UPnPAction;
import org.ocap.hn.upnp.client.UPnPActionResponseHandler;
import org.ocap.hn.upnp.client.UPnPClientDevice;
import org.ocap.hn.upnp.client.UPnPClientService;
import org.ocap.hn.upnp.client.UPnPControlPoint;

import org.ocap.hn.upnp.common.UPnPActionInvocation;
import org.ocap.hn.upnp.common.UPnPResponse;

public class UPnPClientServiceTest extends TestCase
{
    private static final Object signal = new Object();

    private UPnPControlPoint cp;

    private boolean notified;

    public void testPostActionInvocation()
    {
        System.out.println("Starting UPnPService.postActionInvocation test");

        setUpBeforeTest();

        UPnPClientDevice devices[] = cp.getDevices();
        System.out.println("Device found cnt: " + devices.length);

        // Get the services
        Vector services = new Vector();
        for (int i = 0; i < devices.length; i++)
        {
            UPnPClientDeviceImpl device = (UPnPClientDeviceImpl)devices[i];
            UPnPClientService uServices[] = device.getServices();
            for (int j = 0; j < uServices.length; j++)
            {
                services.add(uServices[j]);
            }
        }
        System.out.println("Service found cnt: " + services.size());

        // Get the actions
        Vector actions = new Vector();
        for (int i = 0; i < services.size(); i++)
        {
            UPnPClientServiceImpl service = (UPnPClientServiceImpl)services.get(i);
            UPnPAction uActions[] = service.getActions();
            for (int j = 0; j < uActions.length; j++)
            {
                actions.add(uActions[j]);
            }
        }
        System.out.println("Action found cnt: " + actions.size());

        // Execute each action
        for (int i = 0; i < actions.size(); i++)
        {
            UPnPActionImpl action = (UPnPActionImpl)actions.get(i);

            String argNames[] = action.getArgumentNames();
            String argVals[] = new String[argNames.length];

            // Put in some values for args
            for (int j = 0; j < argVals.length; j++)
            {
                argVals[j] = Integer.toString(j);
            }

            // Create an UPnPActionInvocation
            UPnPActionInvocation uai = new UPnPActionInvocation(argVals, action);

            // Create instance of handler
            UPnPActionResponseHandlerImpl handler = new UPnPActionResponseHandlerImpl();

            // Post the action
            notified = false;
            UPnPClientService service = (UPnPClientService)action.getService();
            System.out.println("Posting Action: " + (i+1));
            service.postActionInvocation(uai, handler);

            // Wait until handler is notified
            try
            {
                synchronized (signal)
                {
                    signal.wait(2000);
                }
            }
            catch (InterruptedException e)
            {
            }

            if (! notified)
            {
                fail("Failed to get response from action");
            }
        }
    }

    public void testGetXML()
    {
        System.out.println("Starting UPnPService.getXML test");

        setUpBeforeTest();

        UPnPClientDevice devices[] = cp.getDevices();
        System.out.println("Device found cnt: " + devices.length);

        // Get the services
        Vector services = new Vector();
        for (int i = 0; i < devices.length; i++)
        {
            UPnPClientDeviceImpl device = (UPnPClientDeviceImpl)devices[i];
            UPnPClientService uServices[] = device.getServices();
            for (int j = 0; j < uServices.length; j++)
            {
                services.add(uServices[j]);
            }
        }
        System.out.println("Service found cnt: " + services.size());

        if (services.size() > 0)
        {
            ((UPnPClientService) services.get(0)).getXML();
        }
        else
        {
            fail("Unable to run test since no service was found");
        }
    }

    public void testGetStateVariablesAndActions()
    {
        System.out.println("Starting UPnPService.getStateVariablesAndActions test");

        setUpBeforeTest();

        UPnPClientDevice devices[] = cp.getDevices();
        System.out.println("Device found cnt: " + devices.length);

        // Get the services
        Vector services = new Vector();
        for (int i = 0; i < devices.length; i++)
        {
            UPnPClientDeviceImpl device = (UPnPClientDeviceImpl)devices[i];
            UPnPClientService uServices[] = device.getServices();
            for (int j = 0; j < uServices.length; j++)
            {
                services.add(uServices[j]);
            }
        }
        System.out.println("Service found cnt: " + services.size());

        if (services.size() > 0)
        {
            for (int i = 0; i < services.size(); i++)
            {
                try
                {
                    ((UPnPClientService) services.get(i)).getStateVariables();
                    ((UPnPClientService) services.get(i)).getActions();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    fail("Encountered exception while getting state variables or actions");                    
                }
            }
        }
    }

    private class UPnPActionResponseHandlerImpl implements UPnPActionResponseHandler
    {
        protected UPnPActionResponseHandlerImpl()
        {
        }

        public void notifyUPnPActionResponse(UPnPResponse response)
        {
            System.out.println("Handler was notified with response");
            notified = true;
            synchronized (signal)
            {
                signal.notifyAll();
            }
        }
    }

    private void setUpBeforeTest()
    {
        cp = UPnPControlPoint.getInstance();
        stop(cp);

        stop();

        start(cp);
        start();
    }

    private static void sleep(int seconds)
    {
        try
        {
            Thread.sleep(seconds * 1000);
        }
        catch (InterruptedException e)
        {
        }
    }

    private static void start()
    {
        MediaServer.getInstance().getRootDevice().sendAlive();
        sleep(5);
    }

    private static void start(UPnPControlPoint cp)
    {
        try
        {
            Method start = UPnPControlPoint.class.getDeclaredMethod("start", new Class[] {});

            start.setAccessible(true);

            start.invoke(cp, null);

            sleep(2);
        }
        catch (Exception e)
        {
            e.printStackTrace(System.out);
        }
    }

    private static void stop()
    {
        MediaServer.getInstance().getRootDevice().sendByeBye();
        sleep(5);
    }

    private static void stop(UPnPControlPoint cp)
    {
        try
        {
            Method stop = UPnPControlPoint.class.getDeclaredMethod("stop", new Class[] {});

            stop.setAccessible(true);

            stop.invoke(cp, null);

            sleep(2);
        }
        catch (Exception e)
        {
            e.printStackTrace(System.out);
        }
    }
}
