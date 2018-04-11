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

package org.ocap.hn.upnp.client;

import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.client.UPnPClientDeviceImpl;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import junit.framework.TestCase;
import org.cablelabs.impl.ocap.hn.upnp.XMLUtil;
import org.ocap.hn.upnp.client.UPnPClientDevice;
import org.ocap.hn.upnp.common.UPnPAction;
import org.ocap.hn.upnp.common.UPnPActionInvocation;
import org.ocap.hn.upnp.common.UPnPMessage;
import org.ocap.hn.upnp.common.UPnPIncomingMessageHandler;
import org.ocap.hn.upnp.common.UPnPOutgoingMessageHandler;
import org.w3c.dom.Document;

public class UPnPControlPointTest extends TestCase
{
    private UPnPControlPoint cp;

    public void testDeviceIdentity()
    {
        System.out.println("testDeviceIdentity");

        setUpBeforeTest();

        UPnPClientDevice[] da1 = cp.getDevices();
        assertTrue("UPnPControlPoint.getDevices() returned no devices", da1.length > 0);
        UPnPClientDevice[] da2 = cp.getDevices();
        assertTrue("UPnPControlPoint.getDevices() returned no devices", da2.length > 0);

        int len1 = da1.length;
        int len2 = da2.length;

        assertTrue("Two invocations of UPnPControlPoint.getDevices() returned different numbers of devices: " + len1 + " vs " + len2, len1 == len2);

        for (int i = 0, n = da1.length; i < n; ++ i)
        {
            UPnPClientDevice d1 = da1[i];
            UPnPClientDevice d2 = da2[i];

            assertTrue("Two invocations of UPnPControlPoint.getDevices() returned different devices: " + d1.getFriendlyName() + " vs " + d2.getFriendlyName(), d1 == d2);
        }
    }

    public void testAddAndRemoveDeviceListener()
    {
        System.out.println("testAddAndRemoveDeviceListener");

        setUpBeforeTest();

        final int[] deviceCount = {0};

        UPnPClientDeviceListener deviceListener = new UPnPClientDeviceListener()
        {
            public void notifyDeviceAdded(UPnPClientDevice device)
            {
                ++ deviceCount[0];
                System.out.println(device + " added");
            }

            public void notifyDeviceRemoved(UPnPClientDevice device)
            {
                -- deviceCount[0];
                System.out.println(device + " removed");
            }
        };

        stop();
        stop(cp);

        // Test addDeviceListener.

        cp.addDeviceListener(deviceListener);

        start(cp);
        start();

        cp.search(3);
        sleep(3);

        if (deviceCount[0] <= 0)
        {
            cp.removeDeviceListener(deviceListener);
            fail("UPnPControlPoint.addDeviceListener did not work when adding devices");
        }

        stop();

        if (deviceCount[0] != 0)
        {
            cp.removeDeviceListener(deviceListener);
//            start(cd);
            fail("UPnPControlPoint.addDeviceListener did not work when removing devices; device count is " + deviceCount[0] + " instead of 0");
        }

        // Test removeDeviceListener.

        cp.removeDeviceListener(deviceListener);

        start();

        cp.search(3);
        sleep(3);

        assertTrue("UPnPControlPoint.removeDeviceListener did not work when adding devices", deviceCount[0] == 0);

        stop();

        if (deviceCount[0] != 0)
        {
//            start(cd);
            fail("UPnPControlPoint.removeDeviceListener did not work when removing devices");
        }

        // Restore the initial state.

//        start(cd);
    }

    public void testGetDevices()
    {
        System.out.println("testGetDevices");

        setUpBeforeTest();

        UPnPClientDevice[] da = cp.getDevices();
        assertTrue("UPnPControlPoint.getDevices() returned no devices", da.length > 0);

        //for (int i = 0, n = da.length; i < n; ++ i)
        //{
        //    System.out.println(da[i].getDeviceType() + ": " + da[i].getFriendlyName());
        //}
    }

    public void testGetDevicesByServiceType()
    {
        System.out.println("testGetDevicesByServiceType");

        setUpBeforeTest();

        UPnPClientDevice[] da;

        da = cp.getDevicesByServiceType("goober");
        assertTrue("UPnPControlPoint.getDevicesByServiceType(\"goober\") returned " + da.length + " devices instead of 0", da.length == 0);

        da = cp.getDevicesByServiceType("urn:schemas-upnp-org:service:ConnectionManager:0");
        assertTrue("UPnPControlPoint.getDevicesByServiceType(\"urn:schemas-upnp-org:service:ConnectionManager:0\") returned " + da.length + " devices instead of 0", da.length == 0);

        da = cp.getDevicesByServiceType("urn:schemas-upnp-org:service:ConnectionManager:9999");
        assertTrue("UPnPControlPoint.getDevicesByServiceType(\"urn:schemas-upnp-org:service:ConnectionManager:9999\") returned " + da.length + " devices instead of 1", da.length == 1);

        da = cp.getDevicesByServiceType("urn:schemas-upnp-org:service:ContentDirectory:0");
        assertTrue("UPnPControlPoint.getDevicesByServiceType(\"urn:schemas-upnp-org:service:ContentDirectory:0\") returned " + da.length + " devices instead of 0", da.length == 0);

        da = cp.getDevicesByServiceType("urn:schemas-upnp-org:service:ContentDirectory:9999");
        assertTrue("UPnPControlPoint.getDevicesByServiceType(\"urn:schemas-upnp-org:service:ContentDirectory:9999\") returned " + da.length + " devices instead of 1", da.length == 1);

        da = cp.getDevicesByServiceType("urn:schemas-upnp-org:service:ScheduledRecording:0");
        assertTrue("UPnPControlPoint.getDevicesByServiceType(\"urn:schemas-upnp-org:service:ScheduledRecording:0\") returned " + da.length + " devices instead of 0", da.length == 0);

        da = cp.getDevicesByServiceType("urn:schemas-upnp-org:service:ScheduledRecording:9999");
        assertTrue("UPnPControlPoint.getDevicesByServiceType(\"urn:schemas-upnp-org:service:ScheduledRecording:9999\") returned " + da.length + " devices instead of 1", da.length == 1);
    }

    public void testGetDevicesByType()
    {
        System.out.println("testGetDevicesByType");

        setUpBeforeTest();

        UPnPClientDevice[] da;

        da = cp.getDevicesByType("goober");
        assertTrue("UPnPControlPoint.getDevicesByType(\"goober\") returned devices", da.length == 0);

        da = cp.getDevicesByType("urn:schemas-opencable-com:device:OCAP_HOST:0");
        assertTrue("UPnPControlPoint.getDevicesByType(\"urn:schemas-opencable-com:device:OCAP_HOST:0\") returned devices", da.length == 0);

        da = cp.getDevicesByType("urn:schemas-opencable-com:device:OCAP_HOST:9999");
        assertTrue("UPnPControlPoint.getDevicesByType(\"urn:schemas-opencable-com:device:OCAP_HOST:9999\") returned no devices", da.length > 0);

        da = cp.getDevicesByType("urn:schemas-upnp-org:device:MediaServer:0");
        assertTrue("UPnPControlPoint.getDevicesByType(\"urn:schemas-upnp-org:device:MediaServer:0\") returned devices", da.length == 0);

        da = cp.getDevicesByType("urn:schemas-upnp-org:device:MediaServer:9999");
        assertTrue("UPnPControlPoint.getDevicesByType(\"urn:schemas-upnp-org:device:MediaServer:9999\") returned no devices", da.length > 0);
    }

    public void testGetDevicesByUDN()
    {
        System.out.println("testGetDevicesByUDN");

        setUpBeforeTest();

        UPnPClientDevice[] da = cp.getDevices();
        assertTrue("UPnPControlPoint.getDevices() returned no devices", da.length > 0);

        UPnPClientDevice[] db;

        for (int i = 0, n = da.length; i < n; ++ i)
        {
            UPnPClientDevice d = da[i];
            String udn = d.getUDN();

            db = cp.getDevicesByUDN(udn);
            assertTrue("UPnPControlPoint.getDevicesByUDN(\"" + udn + "\") returned " + db.length + " devices, not 1 device", db.length == 1);
        }

        db = cp.getDevicesByUDN("goober");
        assertTrue("UPnPControlPoint.getDevicesByUDN(\"goober\") returned " + db.length + " devices, not 0", db.length == 0);
    }

    public void testGetInstance()
    {
        System.out.println("testGetInstance");

        setUpBeforeTest();

        assertNotNull("UPnPControlPoint.getInstance() returned null", cp);
    }

    public void testSetIncomingMessageHandlerForDatagramSocket()
    {
        System.out.println("testSetIncomingMessageHandlerForDatagramSocket");

        setUpBeforeTest();

        final int[] calls = new int[1];

        // Test search with a handler

        cp.setIncomingMessageHandler(new UPnPIncomingMessageHandler()
        {
            public UPnPMessage handleIncomingMessage(InetSocketAddress isa, byte[] ba, UPnPIncomingMessageHandler imh)
            {
                ++ calls[0];

                System.out.println(new String(ba));

                return imh.handleIncomingMessage(isa, ba, null);
            }
        });

        calls[0] = 0;

        cp.search(3);
        sleep(3);

        // At this point, I'm not going to try to predict how many messages to expect.
        // I'll just check that some came in.

        if (calls[0] == 0)
        {
            cp.setIncomingMessageHandler(null);
            fail("Search with incoming message handler resulted in no calls");
        }

        // Test search without a handler

        cp.setIncomingMessageHandler(null);

        calls[0] = 0;

        cp.search(3);
        sleep(3);

        assertEquals("Search without incoming message handler resulted in wrong number of calls:", 0, calls[0]);
    }

    public void testSetIncomingMessageHandlerForSocket()
    {
        System.out.println("testSetIncomingMessageHandlerForSocket");

        setUpBeforeTest();

        // Get services to run tests against

        UPnPClientDevice[] devices = cp.getDevicesByType("urn:schemas-upnp-org:device:MediaServer:9999");
        assertTrue("found " + devices.length + " MediaServer devices instead of 1", devices.length == 1);

        UPnPClientDevice mediaServer = devices[0];

        UPnPClientService[] services = mediaServer.getServices();
        assertTrue("MediaServer device has " + services.length + " services instead of 3", services.length == 3);
        assertTrue("MediaServer's second service is not a ContentDirectory service", services[1].getServiceType().startsWith("urn:schemas-upnp-org:service:ContentDirectory:"));
        assertTrue("MediaServer's third service is not a ScheduledRecording service", services[2].getServiceType().startsWith("urn:schemas-upnp-org:service:ScheduledRecording:"));

        UPnPClientService service;
        UPnPAction[] actions;
        final int[] calls = new int[1];

        // Test ContentDirectory service with a handler

        cp.setIncomingMessageHandler(new UPnPIncomingMessageHandler()
        {
            public UPnPMessage handleIncomingMessage(InetSocketAddress isa, byte[] ba, UPnPIncomingMessageHandler imh)
            {
                ++ calls[0];

                System.out.println(new String(ba));

                return imh.handleIncomingMessage(isa, ba, null);
            }
        });

        calls[0] = 0;

        service = services[1];
        actions = service.getActions();

        for (int i = 0, n = actions.length; i < n; ++ i)
        {
            UPnPAction action = actions[i];

            String[] argNames = action.getArgumentNames();
            String[] argVals = new String[argNames.length];

            UPnPActionInvocation uai = new UPnPActionInvocation(argVals, action);

            service.postActionInvocation(uai, null);
            sleep(2);
        }

        if (calls[0] == 0)
        {
            cp.setIncomingMessageHandler(null);
            fail("Action invocation posts to ContentDirectory service with incoming message handler resulted in no calls");
        }

        // Test ScheduledRecording service without a handler

        cp.setIncomingMessageHandler(null);

        calls[0] = 0;

        service = services[2];
        actions = service.getActions();

        for (int i = 0, n = actions.length; i < n; ++ i)
        {
            UPnPAction action = actions[i];

            String[] argNames = action.getArgumentNames();
            String[] argVals = new String[argNames.length];

            UPnPActionInvocation uai = new UPnPActionInvocation(argVals, action);

            service.postActionInvocation(uai, null);
            sleep(2);
        }

        assertEquals("Action invocation posts to ScheduledRecording service without incoming message handler resulted in wrong number of calls:", 0, calls[0]);
    }

    public void testSetOutgoingMessageHandlerForDatagramSocket()
    {
        System.out.println("testSetOutgoingMessageHandlerForDatagramSocket");

        setUpBeforeTest();

        final int[] calls = new int[1];

        // Test search with a handler

        cp.setOutgoingMessageHandler(new UPnPOutgoingMessageHandler()
        {
            public byte[] handleOutgoingMessage(InetSocketAddress isa, UPnPMessage m, UPnPOutgoingMessageHandler omh)
            {
                ++ calls[0];

                String headers[] = m.getHeaders();
                Document xml = m.getXML();
                System.out.println(headers + "\r\n" + new String(XMLUtil.toByteArray(xml)));

                return omh.handleOutgoingMessage(isa, m, null);
            }
        });

        calls[0] = 0;

        cp.search(3);
        sleep(3);

        // At this point, I'm not going to try to predict how many messages to expect.
        // I'll just check that some came in.

        if (calls[0] == 0)
        {
            cp.setOutgoingMessageHandler(null);
            fail("Search with outgoing message handler resulted in no calls");
        }

        // Test search without a handler

        cp.setOutgoingMessageHandler(null);

        calls[0] = 0;

        cp.search(3);
        sleep(3);

        assertEquals("Search without outgoing message handler resulted in wrong number of calls:", 0, calls[0]);
    }

    public void testSetOutgoingMessageHandlerForSocket()
    {
        System.out.println("testSetOutgoingMessageHandlerForSocket");

        setUpBeforeTest();

        // Get services to run tests against

        UPnPClientDevice[] devices = cp.getDevicesByType("urn:schemas-upnp-org:device:MediaServer:9999");
        assertTrue("found " + devices.length + " MediaServer devices instead of 1", devices.length == 1);

        UPnPClientDevice mediaServer = devices[0];

        UPnPClientService[] services = mediaServer.getServices();
        assertTrue("MediaServer device has " + services.length + " services instead of 3", services.length == 3);
        assertTrue("MediaServer's second service is not a ContentDirectory service", services[1].getServiceType().startsWith("urn:schemas-upnp-org:service:ContentDirectory:"));
        assertTrue("MediaServer's third service is not a ScheduledRecording service", services[2].getServiceType().startsWith("urn:schemas-upnp-org:service:ScheduledRecording:"));

        UPnPClientService service;
        UPnPAction[] actions;
        final int[] calls = new int[1];

        // Test ContentDirectory service with a handler

        cp.setOutgoingMessageHandler(new UPnPOutgoingMessageHandler()
        {
            public byte[] handleOutgoingMessage(InetSocketAddress isa, UPnPMessage m, UPnPOutgoingMessageHandler omh)
            {
                ++ calls[0];

                String headers[] = m.getHeaders();
                Document xml = m.getXML();
                System.out.println(headers + "\r\n" + new String(XMLUtil.toByteArray(xml)));

                return omh.handleOutgoingMessage(isa, m, null);
            }
        });

        calls[0] = 0;

        service = services[1];
        actions = service.getActions();

        for (int i = 0, n = actions.length; i < n; ++ i)
        {
            UPnPAction action = actions[i];

            String[] argNames = action.getArgumentNames();
            String[] argVals = new String[argNames.length];

            UPnPActionInvocation uai = new UPnPActionInvocation(argVals, action);

            service.postActionInvocation(uai, null);
            sleep(2);
        }

        if (calls[0] == 0)
        {
            cp.setOutgoingMessageHandler(null);
            fail("Action invocation posts to ContentDirectory service with outgoing message handler resulted in no calls");
        }

        // Test ScheduledRecording service without a handler

        cp.setOutgoingMessageHandler(null);

        calls[0] = 0;

        service = services[2];
        actions = service.getActions();

        for (int i = 0, n = actions.length; i < n; ++ i)
        {
            UPnPAction action = actions[i];

            String[] argNames = action.getArgumentNames();
            String[] argVals = new String[argNames.length];

            UPnPActionInvocation uai = new UPnPActionInvocation(argVals, action);

            service.postActionInvocation(uai, null);
            sleep(2);
        }

        assertEquals("Action invocation posts to ScheduledRecording service without outgoing message handler resulted in wrong number of calls:", 0, calls[0]);
    }

    public void testSetInetAddresses()
    {
        System.out.println("testSetInetAddresses starting");

        int NETWORK_SETTLE_TIME_SECS = 15;

        // Initial test setup
        setUpBeforeTest();
        sleep(NETWORK_SETTLE_TIME_SECS);

        // *****
        // Case 0 - Default Addressses
        // *****
        UPnPClientDevice[] da0 = cp.getDevices();
        System.out.println("UPnPControlPointTest.testSetInetAddresses() - found " + da0.length + " devices at startup");
        for (int i = 0; i < da0.length; i++)
        {
            System.out.println("Found device at startup: " + da0[i].getFriendlyName());
        }
        assertTrue("testSetInetAddresses() should return at least 2 local devices using default, length = " + da0.length, da0.length > 1);

        // *****
        // Case 1 - No Addressses
        // *****
        System.out.println("testSetInetAddresses() - Setting address list to empty list");
        InetAddress addresses[] = new InetAddress[0];
        cp.setInetAddresses(addresses);
        sleep(NETWORK_SETTLE_TIME_SECS);

        // Verify no devices are found since no addresses were specified
        UPnPClientDevice[] da1 = cp.getDevices();
        System.out.println("UPnPControlPointTest.testSetInetAddresses() - found " + da1.length + " devices with no addresses");
        for (int i = 0; i < da1.length; i++)
        {
            System.out.println("Found device: " + da1[i].getFriendlyName());
        }
        assertTrue("testSetInetAddresses() should not return any devices with no addresses specified, length = " + da1.length, da1.length == 0);

        // Get all the inet addresses
        List inetAddrs = new ArrayList();
        InetAddress loopbackAddr = null;
        addresses = new InetAddress[1];
        try
        {
            Enumeration e = NetworkInterface.getNetworkInterfaces();
            while(e.hasMoreElements())
            {
                NetworkInterface ni = (NetworkInterface)e.nextElement();
                Enumeration ie = ni.getInetAddresses();
                while(ie.hasMoreElements())
                {
                    InetAddress i = (InetAddress)ie.nextElement();
                    if (!i.isLoopbackAddress())
                    {
                        System.out.println("testSetInetAddress() - Adding address: " + i.getHostAddress());
                        inetAddrs.add(i);
                    }
                    else
                    {
                        System.out.println("testSetInetAddress() - Found local loop back address: " + i.getHostAddress());
                        loopbackAddr = i;
                    }
                }
            }
        }
        catch(SocketException se)
        {
            se.printStackTrace();
        }

        // *****
        // Case 2 - Local Loopback Addresss
        // *****
        if (loopbackAddr != null)
        {
            addresses[0] = loopbackAddr;
            System.out.println("testSetInetAddresses() - Setting inet adddress to local loopback: " + addresses[0].getHostAddress());
            cp.setInetAddresses(addresses);
            sleep(NETWORK_SETTLE_TIME_SECS);

            // Verify only local devices are found - OCAP Device & OCAP Media Server
            UPnPClientDevice[] da2 = cp.getDevices();
            System.out.println("testSetInetAddresses() - found " + da2.length + " devices locally");
            for (int i = 0; i < da2.length; i++)
            {
                System.out.println("Found local device: " + da2[i].getFriendlyName());
            }
            assertTrue("testSetInetAddresses() did not return local devices, length = " + da2.length + " expecting 2", da2.length == 2);
        }
        else
        {
            fail("testSetInetAddresses() - Did not find local loopback inet address");
        }

        // *****
        // Case 3 - First Addresss found in list
        // *****
        // Set the inet address to first address from list of non-local addresses
        if (inetAddrs.size() > 0)
        {
            addresses[0] = (InetAddress)inetAddrs.get(0);
            System.out.println("Setting inet adddress to: " + addresses[0].getHostAddress());
            cp.setInetAddresses(addresses);
            sleep(NETWORK_SETTLE_TIME_SECS);
        }
        else
        {
            fail("testSetInetAddresses() - Did not find any inet addresses, not on a network?");
        }
        // Verify two local devices are not found but any additional devices on network maybe found
        UPnPClientDevice[] da3 = cp.getDevices();
        System.out.println("UPnPControlPointTest.testSetInetAddresses() - found " + da3.length + " devices on network");
        for (int i = 0; i < da3.length; i++)
        {
            System.out.println("testSetInetAddresses() - Found network device: " + da3[i].getFriendlyName());
        }
        int expectedCnt = da0.length - 2;
        assertTrue("testSetInetAddresses() - did not return other devices, length = " + da3.length
                + ", expected: " + expectedCnt, da3.length > 0);

        // *****
        // Case 4 - Restore back to default
        // *****
        // Restore to default all interfaces including loopback prior to completing test
        inetAddrs.add(loopbackAddr);
        cp.setInetAddresses((InetAddress[])inetAddrs.toArray(new InetAddress[inetAddrs.size()]));
        sleep(NETWORK_SETTLE_TIME_SECS);

        // Verify the orginal number of devices are found
        UPnPClientDevice[] da4 = cp.getDevices();
        System.out.println("UPnPControlPointTest.testSetInetAddresses() - found " + da4.length + " devices with default interfaces");
        for (int i = 0; i < da4.length; i++)
        {
            System.out.println("testSetInetAddresses() - Found devices with default interfaces: " + da4[i].getFriendlyName());
        }
        assertTrue("testSetInetAddresses() - did not return original default devices, length = " +
                da4.length + ", not equal to default: " + da0.length, da0.length == da4.length);
        System.out.println("testSetInetAddresses completed");
    }

    public void testGetInetAddresses()
    {
        System.out.println("testGetInetAddresses");

        setUpBeforeTest();

        InetAddress[] ia = cp.getInetAddresses();
        assertTrue("UPnPControlPoint.getInetAddresses() returned no inet addresses", ia.length > 0);
    }

    private void setUpBeforeTest()
    {
        cp = UPnPControlPoint.getInstance();

        stop();
        stop(cp);

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
