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

package org.davic.net.tuning;

import java.util.Vector;

import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestor;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.transport.Transport;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.davic.mpeg.Service;
import org.davic.net.Locator;
import org.davic.resources.ResourceServer;
import org.davic.resources.ResourceStatusEvent;
import org.davic.resources.ResourceStatusListener;

import org.cablelabs.impl.davic.mpeg.TransportStreamExt;
import org.cablelabs.impl.service.TransportStreamHandle;
import org.cablelabs.test.TestUtils;

public class NetworkInterfaceManagerTest extends TestCase
{
    public void testAncestry()
    {
        TestUtils.testExtends(NetworkInterfaceManager.class, Object.class);
        TestUtils.testImplements(NetworkInterfaceManager.class, ResourceServer.class);
    }

    /**
     * Test the single constructor of NetworkInterfaceManager.
     * <ul>
     * NetworkInterfaceManager()
     * </ul>
     */
    public void testConstructors()
    {
        // NetworkInterfaceManager is unable to be explicitly instantiated.
        TestUtils.testNoPublicConstructors(NetworkInterfaceManager.class);
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(NetworkInterfaceManager.class);
    }

    public void testGetInstance() throws Exception
    {
        NetworkInterfaceManager manager = NetworkInterfaceManager.getInstance();
        assertNotNull("Expected a NetworkInterfaceManager instance", manager);
    }

    public void testGetNetworkInterface() throws Exception
    {
        NetworkInterfaceManager manager = NetworkInterfaceManager.getInstance();
        final NetworkInterface[] niArray = manager.getNetworkInterfaces();

        TransportStreamExt ts = new TransportStreamExt()
        {
            public int getTransportStreamId()
            {
                return 1;
            }

            public Service retrieveService(int serviceId)
            {
                return null;
            }

            public Service[] retrieveServices()
            {
                return null;
            }

            public NetworkInterface getNetworkInterface()
            {
                return niArray[0];
            }

            public int getFrequency()
            {
                return 600 * 1000000;
            }

            public int getModulationFormat()
            {
                return 16;
            }

            public Transport getTransport()
            {
                return null;
            }

            public ServiceDetails getServiceDetails()
            {
                return null;
            }

            public Locator getLocator()
            {
                return null;
            }
            
            public TransportStreamHandle getTransportStreamHandle()
            {
            	return null;
            }

            public SIRequest retrieveTsID(SIRequestor requestor)
            {
                // TODO Auto-generated method stub
                return null;
            }
        };

        assertSame("Expected supplied NetworkInterface to be returned", niArray[0], manager.getNetworkInterface(ts));
    }

    public void testGetNetworkInterfaces() throws Exception
    {
        NetworkInterfaceManager manager = NetworkInterfaceManager.getInstance();
        NetworkInterface[] niArray = manager.getNetworkInterfaces();
        assertNotNull("A null NetworkInterface array was returned", niArray);
        assertTrue("NetworkInterface array length was zero", niArray.length > 0);
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(NetworkInterfaceManagerTest.class);
        return suite;
    }

    public NetworkInterfaceManagerTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    class TestResourceListener implements ResourceStatusListener
    {
        public boolean statusChangedFired = false;

        public boolean receiveNIReservedEvent = false;

        public boolean receiveNIReleasedEvent = false;

        Vector eventList = new Vector();

        public synchronized void receiveNIEvent(NetworkInterfaceEvent e)
        {
            eventList.add(e);
            notifyAll();
        }

        public synchronized void statusChanged(ResourceStatusEvent e)
        {
            eventList.add(e);
            notifyAll();
        }

        public boolean eventsAvailable()
        {
            return eventList.size() > 0;
        }

        public void processEvent()
        {
            Object e = null;

            synchronized (eventList)
            {
                if (eventList.size() > 0)
                {
                    e = eventList.elementAt(0);
                    eventList.removeElementAt(0);
                }
            }

            if (e != null)
            {
                if (e instanceof NetworkInterfaceReleasedEvent)
                {
                    statusChangedFired = true;
                    receiveNIReleasedEvent = true;
                }
                else if (e instanceof NetworkInterfaceReservedEvent)
                {
                    statusChangedFired = true;
                    receiveNIReservedEvent = true;
                }
            }
        }

        public void waitEvent(long millisec) throws InterruptedException
        {
            int count = eventList.size();
            if (count <= 0)
            {
                wait(millisec);
            }
            processEvent();
        }

        public void resetAll()
        {
            reset();
            eventList.removeAllElements();
        }

        public void reset()
        {
            statusChangedFired = false;
            receiveNIReservedEvent = false;
            receiveNIReleasedEvent = false;
        }
    }
}
