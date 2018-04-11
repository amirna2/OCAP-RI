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

import java.net.DatagramSocket;
import java.net.SocketException;
import java.security.Permission;
import java.util.Vector;

import javax.tv.service.SIManager;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestor;
import javax.tv.service.ServiceType;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.navigation.ServiceList;
import javax.tv.service.navigation.ServiceTypeFilter;
import javax.tv.service.transport.Transport;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.davic.mpeg.Service;
import org.davic.mpeg.TransportStream;
import org.davic.net.Locator;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.davic.resources.ResourceStatusEvent;
import org.davic.resources.ResourceStatusListener;
import org.dvb.net.rc.RCInterfaceManager;
import org.dvb.net.tuning.TunerPermission;
import org.ocap.net.OcapLocator;
import org.ocap.resource.ApplicationResourceUsage;
import org.ocap.resource.ResourceContentionManager;
import org.ocap.resource.ResourceUsage;

import org.cablelabs.impl.davic.mpeg.TransportStreamExt;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.NetManager;
import org.cablelabs.impl.manager.ResourceManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.ResourceManager.Client;
import org.cablelabs.impl.manager.resource.NotifySetWarningPeriod;
import org.cablelabs.impl.manager.service.CannedServiceMgr;
import org.cablelabs.impl.manager.service.ServiceMgrImpl;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.TransportStreamHandle;
import org.cablelabs.test.ProxySecurityManager;
import org.cablelabs.test.TestUtils;
import org.cablelabs.test.ProxySecurityManager.NullSecurityManager;

/**
 * Tests the NetworkInterfaceController class.
 */
public class NetworkInterfaceControllerTest extends TestCase
{

    public void testAncestry()
    {
        TestUtils.testExtends(NetworkInterfaceController.class, Object.class);
        TestUtils.testImplements(NetworkInterfaceController.class, ResourceProxy.class);
    }

    public void testFields()
    {
        TestUtils.testNoPublicFields(NetworkInterfaceController.class);
    }

    public void testGetClient() throws Exception
    {
        NetworkInterfaceController controller = getNetworkInterfaceController();
        assertNotNull("client should not be null", controller.getClient());
    }

    public void testGetNetworkInterface_unreserved() throws Exception
    {
        NetworkInterfaceController control = getNetworkInterfaceController();
        assertNull("Network interface should be null without a reserve", control.getNetworkInterface());

    }

    public void testGetNetworkInterface_reserved() throws Exception
    {
        NetworkInterfaceController control = getNetworkInterfaceController();
        Locator locator = getLocator();
        TestListener listener = new TestListener();

        niManager.addResourceStatusEventListener(listener);
        try
        {
            synchronized (listener)
            {
                control.reserveFor(locator, null);
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("No event generated for interface reserve", listener.statusChangedFired);
            assertNotNull("Network interface should not be null after a reserve", control.getNetworkInterface());
            synchronized (listener)
            {
                listener.reset();
                control.release();
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("No event generated for interface release", listener.statusChangedFired);
        }
        finally
        {
            try
            {
                control.release();
            }
            catch (NotOwnerException e)
            {

            }
            synchronized (listener)
            {
                listener.waitEvent(EVENT_TIMEOUT);
            }
            niManager.removeResourceStatusEventListener(listener);
        }
    }

    public void testReserve() throws Exception
    {
        NetworkInterfaceController controller = getNetworkInterfaceController();
        NetworkInterfaceController controller2 = getNetworkInterfaceController();
        TestResourceClient client1 = (TestResourceClient) controller.getClient();
        TestResourceClient client2 = (TestResourceClient) controller2.getClient();
        NetworkInterface ni = getNetworkInterface();
        TestListener listener = new TestListener();

        niManager.addResourceStatusEventListener(listener);

        try
        {
            // try to reserve a null interface
            synchronized (listener)
            {
                try
                {
                    controller.reserve(null, null);
                    listener.waitEvent(EVENT_TIMEOUT);
                    fail("NullPointerException should be thrown");
                }
                catch (NullPointerException e)
                {
                }
            }
            assertFalse("No event should be generated for bad reserve call", listener.statusChangedFired);

            // reserve the device and verify that events are generated
            synchronized (listener)
            {
                listener.reset();
                // actually do the reserve
                controller.reserve(ni, null);
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("No event was generated for a reserve call", listener.statusChangedFired);

            // try a second reserve. No events should be generated
            synchronized (listener)
            {
                listener.reset();
                controller.reserve(ni, null);
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertFalse("Event fired when interface was already reserved by same controller",
                    listener.statusChangedFired);

            client1.reset();
            client2.reset();
            // a second controller should reserve the device
            synchronized (listener)
            {
                listener.reset();
                controller2.reserve(ni, null);
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("No event was generated for a reserve call", listener.statusChangedFired);
            assertTrue("Expected requestRelease to be called on first client", client1.requestReleaseCalled);
            assertFalse("Did not expect requestRelease to be called on requestor", client2.requestReleaseCalled);

            // release the device
            synchronized (listener)
            {
                listener.reset();
                try
                {
                    controller.release();
                    listener.waitEvent(EVENT_TIMEOUT);
                    fail("Expected NotOwnerException to be thrown");
                }
                catch (NotOwnerException e)
                {
                }
            }
            assertFalse("No event should be generated for release call with no reservation",
                    listener.statusChangedFired);

            synchronized (listener)
            {
                listener.reset();
                controller2.release();
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("No event was generated for a release call with the reservation", listener.statusChangedFired);
        }
        finally
        {
            try
            {
                controller.release();
            }
            catch (NotOwnerException e)
            {

            }

            try
            {
                controller2.release();
            }
            catch (NotOwnerException e)
            {

            }

            synchronized (listener)
            {
                listener.waitEvent(EVENT_TIMEOUT);
            }
            // remove the listeners
            niManager.removeResourceStatusEventListener(listener);

        }

    }

    public void testReserve_security() throws Exception
    {
        NetworkInterfaceController controller = getNetworkInterfaceController();
        NetworkInterface ni = getNetworkInterface();
        TestListener listener = new TestListener();

        DummySecurityManager sm = new DummySecurityManager();
        BogusSecurityManager bm = new BogusSecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(bm);

        niManager.addResourceStatusEventListener(listener);

        try
        {
            // test if SecurityManager.checkPermission is called
            synchronized (listener)
            {
                listener.reset();
                try
                {
                    controller.reserve(ni, null);
                    listener.waitEvent(EVENT_TIMEOUT);
                    fail("Expected SecurityException to be thrown");
                }
                catch (SecurityException e)
                {
                }
            }
            assertFalse("No event should be generated for a reserve call with incorrect permission",
                    listener.statusChangedFired);

            // change SecurityManagers
            ProxySecurityManager.pop();
            ProxySecurityManager.push(sm);

            // Test if TunerPermission is checked
            // first clear the permission field
            sm.p = null;
            synchronized (listener)
            {
                listener.reset();
                // actually do the reserve
                controller.reserve(ni, null);
                listener.waitEvent(EVENT_TIMEOUT);
            }
            // was SecurityManager called?
            assertNotNull("SecurityManager.checkPermission should be called", sm.p);
            // was TunerPermission checked?
            assertTrue("SecurityManager.checkPermission should be called with TunerPermission",
                    sm.p instanceof TunerPermission);
            assertTrue("No event was generated for a reserve call", listener.statusChangedFired);
        }
        finally
        {
            // restore the SecurityManager
            ProxySecurityManager.pop();

            try
            {
                controller.release();
            }
            catch (NotOwnerException e)
            {

            }
            synchronized (listener)
            {
                listener.waitEvent(EVENT_TIMEOUT);
            }

            // remove the listeners
            niManager.removeResourceStatusEventListener(listener);
        }
    }

    public void testReserveFor() throws Exception
    {
        NetworkInterfaceController controller = getNetworkInterfaceController();
        Locator locator = getLocator();
        TestListener listener = new TestListener();

        niManager.addResourceStatusEventListener(listener);

        try
        {
            // call reserveFor with a null locator
            synchronized (listener)
            {
                try
                {
                    controller.reserveFor(null, null);
                    listener.waitEvent(EVENT_TIMEOUT);
                    fail("NullPointerException should be thrown");
                }
                catch (NullPointerException e)
                {
                }
            }
            assertFalse("No event should be generated for bad reserveFor call", listener.statusChangedFired);

            // actually do the reserve
            synchronized (listener)
            {
                listener.reset();
                controller.reserveFor(locator, null);
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("No event was generated for a reserve call", listener.statusChangedFired);

            // check if an event is fired if an interface has already been
            // reserved
            synchronized (listener)
            {
                listener.reset();
                controller.reserveFor(locator, null);
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertFalse("Event fired when interface was already reserved", listener.statusChangedFired);

            synchronized (listener)
            {
                listener.reset();
                controller.release();
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("No event was generated for a release call", listener.statusChangedFired);

            // release the released device
            synchronized (listener)
            {
                listener.reset();
                try
                {
                    controller.release();
                    listener.waitEvent(EVENT_TIMEOUT);
                    fail("Expected NotOwnerException to be thrown");
                }
                catch (NotOwnerException e)
                {
                }
            }
            assertFalse("No event should be generated for release call with no reservation",
                    listener.statusChangedFired);

        }
        finally
        {
            try
            {
                controller.release();
            }
            catch (NotOwnerException e)
            {

            }
            synchronized (listener)
            {
                listener.waitEvent(EVENT_TIMEOUT);
            }

            // remove the listeners
            niManager.removeResourceStatusEventListener(listener);
        }

    }

    public void testReserveFor_security() throws Exception
    {
        NetworkInterfaceController controller = getNetworkInterfaceController();
        Locator locator = getLocator();
        TestListener listener = new TestListener();

        DummySecurityManager sm = new DummySecurityManager();
        BogusSecurityManager bm = new BogusSecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(bm);

        niManager.addResourceStatusEventListener(listener);

        try
        {
            // test if SecurityManager.checkPermission is called
            synchronized (listener)
            {
                listener.reset();
                try
                {
                    controller.reserveFor(locator, null);
                    listener.waitEvent(EVENT_TIMEOUT);
                    fail("Expected SecurityException to be thrown");
                }
                catch (SecurityException e)
                {
                }
            }
            assertFalse("No event should be generated for a reserve call with incorrect permission",
                    listener.statusChangedFired);

            // change SecurityManagers
            ProxySecurityManager.pop();
            ProxySecurityManager.push(sm);

            // Test if TunerPermission is checked and actually do the reserve
            // first clear the permission field
            sm.p = null;
            // actually do the reserve
            synchronized (listener)
            {
                listener.reset();
                controller.reserveFor(locator, null);
                listener.waitEvent(EVENT_TIMEOUT);
            }
            // was SecurityManager called?
            assertNotNull("SecurityManager.checkPermission should be called", sm.p);
            // was TunerPermission checked?
            assertTrue("SecurityManager.checkPermission should be called with TunerPermission",
                    sm.p instanceof TunerPermission);
            assertTrue("No event was generated for a reserve call", listener.statusChangedFired);

            synchronized (listener)
            {
                listener.reset();
                controller.release();
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("No event was generated for a release call", listener.statusChangedFired);
        }
        finally
        {
            // restore the SecurityManager
            ProxySecurityManager.pop();
            try
            {
                controller.release();
            }
            catch (NotOwnerException e)
            {

            }
            synchronized (listener)
            {
                listener.waitEvent(EVENT_TIMEOUT);
            }

            // remove the listeners
            niManager.removeResourceStatusEventListener(listener);
        }

    }

    public void testTune_locator() throws Exception
    {
        NetworkInterfaceController control1 = getNetworkInterfaceController();
        OcapLocator locator = getLocator();
        OcapLocator badLocator = getBadLocator();
        TestListener listener = new TestListener();
        NetworkInterface reservedInterface = null;

        niManager.addResourceStatusEventListener(listener);

        try
        {
            // test tune without a reserve
            synchronized (listener)
            {
                listener.reset();
                try
                {
                    control1.tune(locator);
                    listener.waitEvent(EVENT_TIMEOUT);
                    fail("NotOwnerException should be thrown.");
                }
                catch (NotOwnerException e)
                {
                }
            }
            assertFalse("No event should be generated for the failed tune", listener.receiveNIEventFired);

            synchronized (listener)
            {
                listener.reset();
                // test tune with a null locator
                try
                {
                    control1.tune((Locator) null);
                    listener.waitEvent(EVENT_TIMEOUT);
                    fail("NullPointerException should be thrown");
                }
                catch (NullPointerException e)
                {
                }
            }
            assertFalse("No event should be generated for the failed tune", listener.receiveNIEventFired);

            // Reserve device, which should succeed
            // And result in resource status event
            synchronized (listener)
            {
                listener.reset();
                control1.reserveFor(locator, null);
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("Listener was not fired for reservation event", listener.statusChangedFired);

            // add a network interface event listener
            reservedInterface = control1.getNetworkInterface();
            assertNotNull("Reserved interface is null", reservedInterface);
            reservedInterface.addNetworkInterfaceListener(listener);

            // tune the interface to a locator that points to an unknown stream
            synchronized (listener)
            {
                try
                {
                    // FIXME(Todd): It is the responsibility of native code
                    // (canned NI in this case)
                    // to reject a tune based upon a bad frequency.
                    listener.reset();
                    control1.tune(badLocator);
                    listener.waitEvent(EVENT_TIMEOUT);
                    fail("StreamNotFoundException or IncorrectLocatorException should be thrown");
                }
                catch (StreamNotFoundException e)
                {
                }
                catch (IncorrectLocatorException e)
                {
                }
            }
            assertFalse("No event should be generated for the failed tune", listener.receiveNIEventFired);

            // tune the interface which should result in NetworkInterfaceEvents
            synchronized (listener)
            {
                listener.reset();
                control1.tune(locator);
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("Listener was not fired for tune start event", listener.receiveNIEventFired);
            assertTrue("Listener did not receive a NetworkInterfaceTuningEvent", listener.receiveNITuningEvent);
            synchronized (listener)
            {
                listener.reset();
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("Listener did not receive a NetworkInterfaceTuningOverEvent", listener.receiveNITuningOverEvent);
            assertTrue("Expected the tune to succeed", listener.tuneSucceeded);
            // see if getLocator() returns the correct locator

            assertEquals("Interface locator not equal to specified locator", locator,
                    ((OcapLocator) reservedInterface.getLocator()));
            // locator.getFrequency(),
            // ((OcapLocator)reservedInterface.getLocator()).getFrequency());

            // relase the device which should generate ResourceStatusEvents
            synchronized (listener)
            {
                listener.reset();
                control1.release();
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("Listner did not receive a ResourceReleasedEvent", listener.statusChangedFired);

        }
        finally
        {
            // remove the listeners
            niManager.removeResourceStatusEventListener(listener);
            if (reservedInterface != null)
            {
                reservedInterface.removeNetworkInterfaceListener(listener);
            }

            try
            {
                control1.release();
            }
            catch (NotOwnerException e)
            {

            }
        }
    }

    public void testTune_transportStream() throws Exception
    {
        NetworkInterfaceController control1 = getNetworkInterfaceController();
        NetworkInterface ni = getNetworkInterface();
        TransportStream ts = getTransportStream();
        TransportStream badTs = getBadTransportStream();
        TestListener listener = new TestListener();

        // add the necessary listeners
        niManager.addResourceStatusEventListener(listener);
        ni.addNetworkInterfaceListener(listener);

        try
        {
            synchronized (listener)
            {
                // test tune without a reserve
                try
                {
                    control1.tune(ts);
                    listener.waitEvent(EVENT_TIMEOUT);
                    fail("NotOwnerException should be thrown");
                }
                catch (NotOwnerException e)
                {
                }
            }
            assertFalse("Event generated for tune with no prior reserve", listener.receiveNIEventFired);

            synchronized (listener)
            {
                // test tune with a null TransportStream
                try
                {
                    listener.reset();
                    control1.tune((TransportStream) null);
                    listener.waitEvent(EVENT_TIMEOUT);
                    fail("NullPointerException should be thrown");
                }
                catch (NullPointerException e)
                {
                }
            }
            assertFalse("No event should be generated for the failed tune", listener.receiveNIEventFired);

            // Reserve device, which should succeed
            // And result in resource status event
            assertNotNull("Received bad network interface pointer", ni);

            synchronized (listener)
            {
                listener.reset();
                control1.reserve(ni, null);
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("Listener was not fired for reservation event", listener.statusChangedFired);

            // try tuning to a bogus transport stream
            synchronized (listener)
            {
                // FIXME(Todd): It is the responsibility of native code (canned
                // NI in this case)
                // to reject a tune based upon a bad frequency.
                listener.reset();
                control1.tune(badTs);
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("Expected NI event listener to be fired for failed TransportStream",
                    listener.receiveNIEventFired);
            assertTrue("Listener did not receive a NetworkInterfaceTuningEvent", listener.receiveNITuningEvent);
            synchronized (listener)
            {
                listener.reset();
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("Expected NI event listener to be fired for failed TransportStream",
                    listener.receiveNIEventFired);
            assertTrue("listener did not receive a NetworkInterfaceTuningOverEvent", listener.receiveNITuningOverEvent);
            assertFalse("Expected the tune to fail", listener.tuneSucceeded);

            // tune the interface which should result in NetworkInterfaceEvents
            synchronized (listener)
            {
                listener.resetAll();
                control1.tune(ts);
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("Listener was not fired for tune start event", listener.receiveNIEventFired);
            assertTrue("Listener did not receive a NetworkInterfaceTuningEvent", listener.receiveNITuningEvent);
            synchronized (listener)
            {
                listener.reset();
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("Listener was not fired for tune start event", listener.receiveNIEventFired);
            assertTrue("Listener did not receive a NetworkInterfaceTuningOverEvent", listener.receiveNITuningOverEvent);
            assertTrue("Expected the tune to succeed", listener.tuneSucceeded);
            // see if getLocator() returns the correct locator
            assertEquals("Interface transport stream not equal to specified transport stream", ts,
                    ni.getCurrentTransportStream());

            // relase the device which should generate ResourceStatusEvents
            synchronized (listener)
            {
                listener.resetAll();
                control1.release();
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("Listner did not receive a ResourceReleasedEvent", listener.statusChangedFired);

            // test tune after a release
            synchronized (listener)
            {
                listener.reset();
                try
                {
                    listener.reset();
                    control1.tune(ts);
                    listener.waitEvent(EVENT_TIMEOUT);
                    fail("NotOwnerException should be thrown");
                }
                catch (NotOwnerException e)
                {
                }
            }
            assertFalse("No event should be generated for tune with no reserve", listener.receiveNIEventFired);
        }
        finally
        {
            try
            {
                control1.release();
            }
            catch (NotOwnerException e)
            {

            }

            synchronized (listener)
            {
                listener.waitEvent(EVENT_TIMEOUT);
            }

            // remove the listeners
            niManager.removeResourceStatusEventListener(listener);
            ni.removeNetworkInterfaceListener(listener);
        }

    }

    /**
     * test calling release() on an unreserved and a reserved controller and
     * check that events are generated when they should be.
     */
    public void testRelease() throws Exception
    {
        // calling release on an unreserved interface should not generate
        // events. Calling release on a reserved device should generate
        // events.
        TestResourceClient rc = new TestResourceClient();
        NetworkInterface ni = getNetworkInterface();
        TestListener listener = new TestListener();
        NetworkInterfaceController nic = new NetworkInterfaceController(rc);

        // install resource status listener
        niManager.addResourceStatusEventListener(listener);

        // try to release an unreserved device
        synchronized (listener)
        {
            try
            {
                listener.reset();
                nic.release();
                listener.waitEvent(EVENT_TIMEOUT);
                fail("NotOwnerException should of been thrown.");
            }
            catch (NotOwnerException e)
            {
            }
        }
        assertFalse("No event should be generated for un-reserved device", listener.eventsAvailable());

        try
        {
            // reserve network interface
            synchronized (listener)
            {
                listener.reset();
                nic.reserve(ni, null);
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("Listener was not fired for reservation event", listener.statusChangedFired);

            // release interface
            synchronized (listener)
            {
                listener.reset();
                nic.release();
                listener.waitEvent(EVENT_TIMEOUT);
            }
            // make sure listener was fired
            assertTrue("Failed to release device", listener.statusChangedFired);

            // release interface - should throw NotOwnerException
            synchronized (listener)
            {
                try
                {
                    listener.reset();
                    nic.release();
                    listener.waitEvent(EVENT_TIMEOUT);
                    fail("NotOwnerException should of been thrown.");
                }
                catch (NotOwnerException e)
                {
                }
            }
            assertFalse("No event should be generated for un-reserved device", listener.eventsAvailable());
            // remove the listener
            niManager.removeResourceStatusEventListener(listener);
        }
        finally
        {
            try
            {
                nic.release();
            }
            catch (NotOwnerException e)
            {
                // do nothing
            }
            synchronized (listener)
            {
                listener.waitEvent(EVENT_TIMEOUT);
            }

            niManager.removeResourceStatusEventListener(listener);

        }
    }

    /**
     * Test whether the interface can be tuned without being reserved
     */
    public void testReserve_tuning() throws Exception
    {
        NetworkInterface ni = getNetworkInterface();
        TestListener listener = new TestListener();
        TestResourceClient rc = new TestResourceClient();
        NetworkInterfaceController nic = new NetworkInterfaceController(rc);

        ni.addNetworkInterfaceListener(listener);
        try
        {
            synchronized (listener)
            {
                try
                {
                    nic.tune(getLocator());
                    listener.waitEvent(1000);
                    fail("NotOwnerException should be thrown");
                }
                catch (NotOwnerException e)
                {
                }
            }
            assertFalse("No events should be generated when tuning an unowned interface", listener.eventsAvailable());

            synchronized (listener)
            {
                listener.reset();
                try
                {
                    nic.tune(createTransportStream(ni, 1, 555000000, 0x10));
                    listener.waitEvent(1000);
                    fail("NotOwnerException should be thrown");
                }
                catch (NotOwnerException e)
                {
                }
            }
            assertFalse("No events should be generated when tuning an unowned interface", listener.eventsAvailable());
        }
        finally
        {
            ni.removeNetworkInterfaceListener(listener);
        }
    }

    private void checkClient(String context, Client client, ResourceClient rc, ResourceProxy proxy)
    {
        assertNotNull("Expected a client to be passed to " + context, client);
        assertSame("Expected client to specify given client", rc, client.client);
        assertSame("Expected client to specify given proxy", proxy, client.proxy);
        assertNotNull("Expected client to specify a CallerContext", client.context);
    }

    /**
     * Test if isReservationAllowed() is called
     */
    public void testReserve_isAllowed() throws Exception
    {
        NetworkInterface ni = getNetworkInterface();
        TestListener listener = new TestListener();
        TestResourceClient rc = new TestResourceClient();
        NetworkInterfaceController nic = new NetworkInterfaceController(rc);

        niManager.addResourceStatusEventListener(listener);
        RezMgr rezmgr = replaceRezMgr();
        try
        {

            for (int truefalse = 0; truefalse < 2; ++truefalse)
            {
                boolean allowed = truefalse != 0;
                rezmgr.reset(allowed, 0);

                // Reserve interface
                synchronized (listener)
                {
                    listener.resetAll();
                    try
                    {
                        nic.reserve(ni, rc);
                    }
                    catch (NoFreeInterfaceException e)
                    {
                        // do nothing
                    }
                    listener.waitEvent(EVENT_TIMEOUT);
                }

                // Check results
                assertTrue("Expected RezMgr.isReservationAllowed() to be called", rezmgr.allowedCalled);
                checkClient("isReservationAllowed", rezmgr.allowedClient, rc, nic);

                assertEquals("Expected NetworkInterfaceController type to be passed to isReservationAllowed",
                        NetworkInterfaceController.class.getName(), rezmgr.allowedProxy);

                // Was change listener called or not?
                assertEquals("Expected status listener to be called if reservation allowed", allowed,
                        listener.statusChangedFired);

                // Release before looping
                synchronized (listener)
                {
                    listener.reset();
                    try
                    {
                        nic.release();
                    }
                    catch (NotOwnerException e)
                    {

                    }
                    listener.waitEvent(EVENT_TIMEOUT);
                }
            }
        }
        finally
        {
            restoreRezMgr();
            niManager.removeResourceStatusEventListener(listener);
        }
    }

    /**
     * Test reserve() if there is no current owner
     */
    public void testReserve_noOwner() throws Exception
    {
        TestListener listener = new TestListener();
        NetworkInterface ni = getNetworkInterface();
        TestResourceClient rc = new TestResourceClient();
        NetworkInterfaceController nic = new NetworkInterfaceController(rc);

        niManager.addResourceStatusEventListener(listener);
        RezMgr rezmgr = replaceRezMgr();
        try
        {
            // nobody owns it
            assertFalse("Did not expect somebody to own the interface", ni.isReserved());
            synchronized (listener)
            {
                nic.reserve(ni, null);
                listener.waitEvent(100);
            }
            assertTrue("Expected status listener to be called", listener.statusChangedFired);

            // Should not get so far as calling prioritizeContention
            assertFalse("Should not have contention", rezmgr.prioritizeCalled);
        }
        finally
        {
            restoreRezMgr();
            nic.release();
            synchronized (listener)
            {
                listener.waitEvent(EVENT_TIMEOUT);
            }

            niManager.removeResourceStatusEventListener(listener);
        }
    }

    /**
     * Test reservation when the interface is already owned by us
     */
    public void testReserve_alreadyOwned() throws Exception
    {
        TestListener listener = new TestListener();
        NetworkInterface ni = getNetworkInterface();
        TestResourceClient rc = new TestResourceClient();
        NetworkInterfaceController nic = new NetworkInterfaceController(rc);

        niManager.addResourceStatusEventListener(listener);
        RezMgr rezmgr = replaceRezMgr();
        try
        {
            // acquire it for ourself
            synchronized (listener)
            {
                nic.reserve(ni, null);
                listener.waitEvent(100);
            }
            assertTrue("Expected resource status listener to be called", listener.statusChangedFired);
            // We already own it
            synchronized (listener)
            {
                listener.reset();
                nic.reserve(ni, null);
                listener.waitEvent(100);
            }
            assertFalse("Expected no status listener to be called - since we own it!", listener.eventsAvailable());

            // Should not get so far as calling prioritizeContention
            assertFalse("Should not have contention", rezmgr.prioritizeCalled);
        }
        finally
        {
            restoreRezMgr();
            nic.release();
            synchronized (listener)
            {
                listener.waitEvent(EVENT_TIMEOUT);
            }

            niManager.removeResourceStatusEventListener(listener);
        }
    }

    /**
     * Test reserve() given requestRelease=true
     */
    public void testReserve_requestRelease() throws Exception
    {
        TestResourceClient rc = new TestResourceClient();
        TestResourceClient rc2 = new TestResourceClient();
        NetworkInterface ni = getNetworkInterface();
        TestListener listener = new TestListener();
        NetworkInterfaceController nic1 = new NetworkInterfaceController(rc);
        NetworkInterfaceController nic2 = new NetworkInterfaceController(rc2);

        niManager.addResourceStatusEventListener(listener);
        RezMgr rezmgr = replaceRezMgr();

        try
        {
            // acquire it for rc
            synchronized (listener)
            {
                nic1.reserve(ni, null);
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("Expected status listener to be called", listener.statusChangedFired);
            Thread.sleep(EVENT_TIMEOUT);
            // Reservation by another client
            rc.reset();
            synchronized (listener)
            {
                listener.reset();
                nic2.reserve(ni, null);
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("Expected status listener to be called", listener.statusChangedFired);
            assertTrue("Expected requestRelease to have been called", rc.requestReleaseCalled);
            assertFalse("Did not expect notifyRelease to have been called", rc.notifyReleaseCalled);

            // Should not get so far as calling prioritizeContention
            assertFalse("Should not have contention", rezmgr.prioritizeCalled);
        }
        finally
        {
            restoreRezMgr();
            nic2.release();
            synchronized (listener)
            {
                listener.waitEvent(EVENT_TIMEOUT);
            }

            niManager.removeResourceStatusEventListener(listener);
        }
    }

    /**
     * Test reserve() and application priority
     */
    public void testReserve_contention() throws Exception
    {
        TestResourceClient rc = new TestResourceClient();
        TestResourceClient rc2 = new TestResourceClient();
        NetworkInterfaceController nic1 = new NetworkInterfaceController(rc);
        NetworkInterfaceController nic2 = new NetworkInterfaceController(rc2);
        NetworkInterface ni = getNetworkInterface();
        TestListener listener = new TestListener();

        niManager.addResourceStatusEventListener(listener);
        RezMgr rezmgr = replaceRezMgr();
        try
        {
            for (int i = 0; i <= RezMgr.PRIOR_MAX; ++i)
            {
                rezmgr.reset(true, i);

                // nobody owns it
                synchronized (listener)
                {
                    listener.resetAll();
                    nic1.reserve(ni, null);
                    listener.waitEvent(EVENT_TIMEOUT);
                    assertTrue("Expected status listener to be called", listener.statusChangedFired);
                }

                // Contention
                rc2.reset();
                rc.reset(false);
                synchronized (listener)
                {
                    listener.resetAll();
                    try
                    {
                        nic2.reserve(ni, null);
                        assertTrue("Expected NoFreeInterfacesException to be thrown", rezmgr.prioritizeOkay);
                    }
                    catch (NoFreeInterfaceException e)
                    {
                        assertFalse("Did not expect NoFreeInterfacesException to be thrown", rezmgr.prioritizeOkay);
                    }
                    listener.waitEvent(EVENT_TIMEOUT);
                    assertTrue("Expected requestRelease to be called", rc.requestReleaseCalled);
                }
                Thread.sleep(EVENT_TIMEOUT);
                assertFalse("Did not expect requestRelease to be called on requester", rc2.requestReleaseCalled);
                assertTrue("Expected contention - " + i, rezmgr.prioritizeCalled);

                // prioritizeContention params
                checkClient("prioritizeContention(req) - " + i, rezmgr.prioritizeReq, rc2, nic2);
                assertNotNull("Expected owners to be passed to prioritizeContention", rezmgr.prioritizeOwn);
                assertEquals("Expected 1 owner to be passed to prioritizeContention", 1, rezmgr.prioritizeOwn.length);
                checkClient("prioritizeContention(owner) - " + i, rezmgr.prioritizeOwn[0], rc, nic1);

                // Callbacks made?
                boolean release = rezmgr.prioritizeOkay || !rezmgr.prioritizeHasClient;
                assertFalse("release() should not be called on requester", rc2.releaseCalled);
                assertFalse("notifyRelease() should not be called on requester", rc2.notifyReleaseCalled);
                assertEquals("Expected release to have been called - " + i, release, rc.releaseCalled);
                assertEquals("Expected notifyRelease to have been called - " + i, release, rc.notifyReleaseCalled);
                assertEquals("Expected statusChanged to have been called - " + i, release, listener.statusChangedFired);

                // getProxyOwner
                if (!rezmgr.prioritizeHasClient)
                {
                    assertTrue("nic1 should have no interface reserved - " + i, nic1.getNetworkInterface() == null);
                    assertTrue("nic2 should have no interface reserved - " + i, nic2.getNetworkInterface() == null);

                }
                else
                {
                    NetworkInterfaceController nic = rezmgr.prioritizeOkay ? nic2 : nic1;
                    assertTrue("Expected controller to have NetworkInterface reserved - " + i,
                            nic.getNetworkInterface() != null);
                    nic.release();
                }

                // reserved?
                // assertEquals("Expected contention to be resolved - "+i,
                // rezmgr.prioritizeOkay, reserved);

                Thread.sleep(EVENT_TIMEOUT);
            }
        }
        finally
        {

            try
            {
                nic1.release();
            }
            catch (NotOwnerException e)
            {
                // do nothing
            }

            try
            {
                nic2.release();
            }
            catch (NotOwnerException e)
            {
                // do nothing
            }

            restoreRezMgr();
            synchronized (listener)
            {
                listener.waitEvent(EVENT_TIMEOUT);
            }

            niManager.removeResourceStatusEventListener(listener);
        }
    }

    /**
     * Test reserveFor() and application priority
     */
    public void testReserveFor_contention() throws Exception
    {
        TestResourceClient rc = new TestResourceClient();
        TestResourceClient rc2 = new TestResourceClient();
        TestResourceClient rc3 = new TestResourceClient();
        NetworkInterfaceController nic1 = new NetworkInterfaceController(rc);
        NetworkInterfaceController nic2 = new NetworkInterfaceController(rc2);
        NetworkInterfaceController nic3 = new NetworkInterfaceController(rc3);
        TestListener listener = new TestListener();
        Locator locator = getLocator();

        niManager.addResourceStatusEventListener(listener);
        RezMgr rezmgr = replaceRezMgr();
        try
        {
            for (int i = 0; i <= RezMgr.PRIOR_MAX; ++i)
            {
                rezmgr.reset(true, i);

                // nobody owns it
                synchronized (listener)
                {
                    listener.resetAll();
                    nic1.reserveFor(locator, null);
                    listener.waitEvent(EVENT_TIMEOUT);
                    assertTrue("Expected status listener to be called", listener.statusChangedFired);
                }

                rc2.reset();
                rc.reset(false);
                synchronized (listener)
                {
                    listener.resetAll();
                    nic2.reserveFor(locator, null);
                    listener.waitEvent(EVENT_TIMEOUT);
                    assertTrue("Expected status listener to be called", listener.statusChangedFired);
                }

                // contention
                rc3.reset();
                rc.reset(false);
                rc2.reset(false);
                synchronized (listener)
                {
                    listener.resetAll();
                    try
                    {
                        nic3.reserveFor(locator, null);
                        assertTrue("Expected NoFreeInterfacesException to be thrown", rezmgr.prioritizeOkay);
                    }
                    catch (NoFreeInterfaceException e)
                    {
                        assertFalse("Did not expect NoFreeInterfacesException to be thrown", rezmgr.prioritizeOkay);
                    }
                    listener.waitEvent(EVENT_TIMEOUT);
                    assertTrue("Expected requestRelease to be called", rc.requestReleaseCalled);
                    assertTrue("Expected requestRelease to be called", rc2.requestReleaseCalled);
                }
                Thread.sleep(EVENT_TIMEOUT);
                assertFalse("Did not expect requestRelease to be called on requester", rc3.requestReleaseCalled);
                assertTrue("Expected contention - " + i, rezmgr.prioritizeCalled);

                // prioritizeContention params
                checkClient("prioritizeContention(req) - " + i, rezmgr.prioritizeReq, rc3, nic3);
                assertNotNull("Expected owners to be passed to prioritizeContention", rezmgr.prioritizeOwn);
                assertEquals("Expected 2 owners to be passed to prioritizeContention", 2, rezmgr.prioritizeOwn.length);
                checkClient("prioritizeContention(owner) - " + i, rezmgr.prioritizeOwn[0], rc, nic1);
                checkClient("prioritizeContention(owner) - " + i, rezmgr.prioritizeOwn[1], rc2, nic2);

                // Callbacks made?
                boolean release = rezmgr.prioritizeOkay || !rezmgr.prioritizeHasClient;
                assertFalse("release() should not be called on requester", rc3.releaseCalled);
                assertFalse("notifyRelease() should not be called on requester", rc3.notifyReleaseCalled);
                assertEquals("Expected release to have been called - " + i, release, rc2.releaseCalled);
                assertEquals("Expected notifyRelease to have been called - " + i, release, rc2.notifyReleaseCalled);
                assertEquals("Expected statusChanged to have been called - " + i, release, listener.statusChangedFired);

                // getProxyOwner
                if (!rezmgr.prioritizeHasClient)
                {
                    assertTrue("nic1 should have no interface reserved - " + i, nic1.getNetworkInterface() == null);
                    assertTrue("nic2 should have no interface reserved - " + i, nic2.getNetworkInterface() == null);
                    assertTrue("nic3 should have no interface reserved - " + i, nic3.getNetworkInterface() == null);

                }
                else
                {
                    NetworkInterfaceController nic = rezmgr.prioritizeOkay ? nic3 : nic2;
                    assertTrue("Expected controller to have NetworkInterface reserved - " + i,
                            nic.getNetworkInterface() != null);
                    nic.release();

                    try
                    {
                        nic1.release();
                    }
                    catch (NotOwnerException e)
                    {

                    }

                    try
                    {
                        nic2.release();
                    }
                    catch (NotOwnerException e)
                    {

                    }

                }

                // reserved?
                // assertEquals("Expected contention to be resolved - "+i,
                // rezmgr.prioritizeOkay, reserved);

                Thread.sleep(EVENT_TIMEOUT);
            }
        }
        finally
        {

            try
            {
                nic1.release();
            }
            catch (NotOwnerException e)
            {
                // do nothing
            }

            try
            {
                nic2.release();
            }
            catch (NotOwnerException e)
            {
                // do nothing
            }

            restoreRezMgr();
            synchronized (listener)
            {
                listener.waitEvent(EVENT_TIMEOUT);
            }

            niManager.removeResourceStatusEventListener(listener);
        }
    }

    /**
     * Test reserve() that the requested ResourceUsage type is
     * ApplicationResourceUsage
     */
    public void testReserve_resourceUsageType() throws Exception
    {
        TestListener listener = new TestListener();
        NetworkInterface ni = getNetworkInterface();
        TestResourceClient rc = new TestResourceClient();
        TestResourceClient rc2 = new TestResourceClient();
        NetworkInterfaceController nic = new NetworkInterfaceController(rc);
        NetworkInterfaceController nic2 = new NetworkInterfaceController(rc2);

        niManager.addResourceStatusEventListener(listener);
        RezMgr rezmgr = replaceRezMgr();
        // when we go to contention, the first owner will remain highest
        // priority.
        rezmgr.reset(true, 4);
        try
        {
            // nobody owns it
            assertFalse("Did not expect somebody to own the interface", ni.isReserved());
            synchronized (listener)
            {
                nic.reserve(ni, null);
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("Expected status listener to be called", listener.statusChangedFired);

            // Contention
            rc2.reset();
            rc.reset(false);
            synchronized (listener)
            {
                listener.resetAll();
                try
                {
                    nic2.reserve(ni, null);
                }
                catch (NoFreeInterfaceException e)
                {
                }
            }

            // owner ResourceUsages should be instances of
            // ApplicationResourceUsage
            for (int i = 0; i < rezmgr.prioritizeOwn.length; i++)
            {
                assertTrue("Expected owner " + i + "ResourceUsage to be of type ApplicationResourceUsage",
                        rezmgr.prioritizeOwn[i].resusage instanceof ApplicationResourceUsage);
            }

        }
        finally
        {
            restoreRezMgr();
            nic.release();
            synchronized (listener)
            {
                listener.waitEvent(EVENT_TIMEOUT);
            }

            niManager.removeResourceStatusEventListener(listener);
        }
    }

    /**
     * Test reserveFor() that the requested ResourceUsage type is
     * ApplicationResourceUsage
     */
    public void testReserveFor_resourceUsageType() throws Exception
    {
        TestListener listener = new TestListener();
        TestResourceClient rc = new TestResourceClient();
        TestResourceClient rc2 = new TestResourceClient();
        TestResourceClient rc3 = new TestResourceClient();
        NetworkInterfaceController nic = new NetworkInterfaceController(rc);
        NetworkInterfaceController nic2 = new NetworkInterfaceController(rc2);
        NetworkInterfaceController nic3 = new NetworkInterfaceController(rc3);
        Locator locator = getLocator();

        niManager.addResourceStatusEventListener(listener);
        RezMgr rezmgr = replaceRezMgr();
        rezmgr.reset(true, 4);
        try
        {
            synchronized (listener)
            {
                nic.reserveFor(locator, null);
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("Expected status listener to be called", listener.statusChangedFired);

            rc2.reset();
            synchronized (listener)
            {
                listener.resetAll();
                nic2.reserveFor(locator, null);
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("Expected status listener to be called", listener.statusChangedFired);

            // contention
            rc3.reset();
            rc2.reset(false);
            rc.reset(false);
            synchronized (listener)
            {
                listener.resetAll();
                try
                {
                    nic3.reserveFor(locator, null);
                }
                catch (NoFreeInterfaceException e)
                {
                }
            }
            // owner ResourceUsages should be instances of
            // ApplicationResourceUsage
            for (int i = 0; i < rezmgr.prioritizeOwn.length; i++)
            {
                assertTrue("Expected owner " + i + "ResourceUsage to be of type ApplicationResourceUsage",
                        rezmgr.prioritizeOwn[i].resusage instanceof ApplicationResourceUsage);
            }
        }
        finally
        {
            restoreRezMgr();
            try
            {
                nic.release();
            }
            catch (NotOwnerException e)
            {
            }

            try
            {
                nic2.release();
            }
            catch (NotOwnerException e)
            {
            }
            synchronized (listener)
            {
                listener.waitEvent(EVENT_TIMEOUT);
            }

            niManager.removeResourceStatusEventListener(listener);
        }
    }

    public void testReserveFor_exceptions() throws Exception
    {
        TestResourceClient rc = new TestResourceClient();
        NetworkInterfaceController nic = new NetworkInterfaceController(rc);
        Locator locator = new OcapLocator("ocap://oobfdc.0x1");

        try
        {
            nic.reserveFor(locator, null);
            fail("expected IncorrectLocatorException to be thrown");
        }
        catch (IncorrectLocatorException e)
        {
            // success
        }
    }

    public void testTune_exceptions() throws Exception
    {
        TestListener listener = new TestListener();
        TestResourceClient rc = new TestResourceClient();
        NetworkInterfaceController nic = new NetworkInterfaceController(rc);
        NetworkInterface ni = getNetworkInterface(0);
        Locator locator = new OcapLocator("ocap://oobfdc.0x1");

        niManager.addResourceStatusEventListener(listener);
        try
        {
            synchronized (listener)
            {
                nic.reserve(ni, null);
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("Expected status listener to be called", listener.statusChangedFired);
            try
            {
                nic.tune(locator);
                fail("expected IncorrectLocatorException to be thrown");
            }
            catch (IncorrectLocatorException e)
            {
                // success
            }

            // FIXME(Todd): It is not clear to me why this is expected to fail.
            // try
            // {
            // nic.tune(createTransportStream(getNetworkInterface(1), 1,
            // 555000000, 0x10));
            // fail("expected StreamNotFoundException to be thrown");
            // }
            // catch (NetworkInterfaceException e)
            // {
            // // success
            // }

        }
        finally
        {
            try
            {
                nic.release();
            }
            catch (NotOwnerException e)
            {
            }
            synchronized (listener)
            {
                listener.waitEvent(EVENT_TIMEOUT);
            }

            niManager.removeResourceStatusEventListener(listener);
        }
    }

    /**
     * ResourceClient, ResourceProxy, and NetworkInterfaceListener for testing.
     */
    private ResourceManager save;

    private RezMgr replaceRezMgr()
    {
        RezMgr rezmgr = new RezMgr();
        save = (ResourceManager) ManagerManager.getInstance(ResourceManager.class);
        ManagerManagerTest.updateManager(ResourceManager.class, RezMgr.class, false, rezmgr);
        return rezmgr;
    }

    private void restoreRezMgr()
    {
        if (save != null) ManagerManagerTest.updateManager(ResourceManager.class, save.getClass(), true, save);
        save = null;
    }

    private NetManager netsave;

    private NetworkMgr replaceNetMgr()
    {
        NetworkMgr netmgr = new NetworkMgr();
        netsave = (NetManager) ManagerManager.getInstance(NetManager.class);
        ManagerManagerTest.updateManager(NetManager.class, NetworkMgr.class, false, netmgr);
        return netmgr;
    }

    private void restoreNetMgr()
    {
        if (netsave != null) ManagerManagerTest.updateManager(NetManager.class, netsave.getClass(), true, netsave);
        netsave = null;
    }

    class TestListener implements NetworkInterfaceListener, ResourceStatusListener
    {
        public boolean receiveNITuningOverEvent = false;

        public boolean receiveNITuningEvent = false;

        public boolean receiveNIEventFired = false;

        public boolean tuneSucceeded = false;

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
                if (e instanceof NetworkInterfaceTuningOverEvent)
                {
                    NetworkInterfaceTuningOverEvent evt = (NetworkInterfaceTuningOverEvent) e;
                    receiveNIEventFired = true;
                    receiveNITuningOverEvent = true;
                    if (evt.getStatus() == NetworkInterfaceTuningOverEvent.SUCCEEDED) tuneSucceeded = true;
                }
                else if (e instanceof NetworkInterfaceTuningEvent)
                {
                    receiveNIEventFired = true;
                    receiveNITuningEvent = true;
                }
                else if (e instanceof NetworkInterfaceReleasedEvent)
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
            receiveNITuningOverEvent = false;
            receiveNITuningEvent = false;
            receiveNIEventFired = false;
            tuneSucceeded = false;
            statusChangedFired = false;
            receiveNIReservedEvent = false;
            receiveNIReleasedEvent = false;
        }
    }

    /**
     * ResourceClient and ResourceProxy for testing.
     */
    class TestResourceClient implements ResourceClient, ResourceProxy
    {
        public boolean notifyReleaseCalled = false;

        public boolean releaseCalled = false;

        public boolean requestReleaseCalled = false;

        public boolean REQUEST = true;

        public void reset(boolean REQUEST)
        {
            notifyReleaseCalled = false;
            releaseCalled = false;
            requestReleaseCalled = false;
            this.REQUEST = REQUEST;
        }

        public void reset()
        {
            reset(REQUEST);
        }

        public TestResourceClient()
        {
        }

        /**
         * A call to this operation notifies the ResourceClient that proxy has
         * lost access to a resource. This can happen for two reasons: either
         * the resource is unavailable for some reason beyond the control of the
         * environment (e.g. hardware failure) or because the client has been
         * too long in dealing with a ResourceClient.release() call.
         * 
         * @param proxy
         *            - the ResourceProxy representing the scarce resource to
         *            the application
         */
        public void notifyRelease(ResourceProxy p)
        {
            notifyReleaseCalled = true;
        }

        /**
         * A call to this operation informs the ResourceClient that proxy is
         * about to lose access to a resource. The ResourceClient shall complete
         * any clean-up that is needed before the resource is lost before it
         * returns from this operation. This operation is not guaranteed to be
         * allowed to complete before notifyRelease() is called.
         * 
         * @param proxy
         *            - the ResourceProxy representing the scarce resource to
         *            the application
         */
        public void release(ResourceProxy p)
        {
            releaseCalled = true;
        }

        /**
         * A call to this operation informs the ResourceClient that another
         * application has requested the resource accessed via the proxy
         * parameter. If the ResourceClient decides to give up the resource as a
         * result of this, it should terminate its usage of proxy and return
         * True, otherwise False. requestData may be used to pass more data to
         * the ResourceClient so that it can decide whether or not to give up
         * the resource, using semantics specified outside this framework; for
         * conformance to this framework, requestData can be ignored by the
         * ResourceClient.
         * 
         * @param proxy
         *            - the ResourceProxy representing the scarce resource to
         *            the application
         * @param requestData
         *            - application specific data
         * @return boolean If the ResourceClient decides to give up the resource
         *         following this call, it should terminate its usage of proxy
         *         and return True, otherwise False.
         */
        public boolean requestRelease(ResourceProxy p, Object requestData)
        {
            requestReleaseCalled = true;
            return REQUEST;
        }

        public ResourceClient getClient()
        {
            return this;
        }

    } // class TestResourceClient

    /**
     * Replacement ResourceManager for catching calls to the ResourceManager.
     */
    static class RezMgr implements ResourceManager
    {
        public boolean ALLOWED = true;

        public int PRIOR = 0;

        // isReservationAllowed
        public boolean allowedCalled = false;

        public Client allowedClient = null;

        public String allowedProxy = null;

        // prioritizeContention
        public boolean prioritizeCalled = false;

        public Client prioritizeReq = null;

        public Client[] prioritizeOwn = null;

        public boolean prioritizeOkay;

        public boolean prioritizeHasClient;

        public Class resourceUsageType = null;

        public void reset(boolean ALLOWED, int PRIOR)
        {
            this.ALLOWED = ALLOWED;
            this.PRIOR = PRIOR;
            allowedCalled = false;
            allowedClient = null;
            allowedProxy = null;
            prioritizeCalled = false;
            prioritizeReq = null;
            prioritizeOwn = null;
            resourceUsageType = null;
        }

        public boolean isReservationAllowed(Client client, String proxyType)
        {
            allowedCalled = true;
            allowedClient = client;
            allowedProxy = proxyType;
            return ALLOWED;
        }

        public static final int PRIOR_MAX = 4;

        public Client[] prioritizeContention(Client requester, Client[] owners)
        {
            prioritizeCalled = true;
            prioritizeReq = requester;
            prioritizeOwn = owners;
            Vector array;

            switch (PRIOR)
            {
                case 0:
                    prioritizeOkay = false;
                    prioritizeHasClient = false;
                    return new Client[0];
                case 1:
                    prioritizeOkay = true;
                    prioritizeHasClient = true;
                    return new Client[] { requester };
                case 2:
                    prioritizeOkay = false;
                    prioritizeHasClient = true;
                    return owners;
                case 3:
                    prioritizeOkay = true;
                    prioritizeHasClient = true;
                    array = new Vector();
                    array.add(requester);
                    for (int i = 0; i < owners.length; i++)
                        array.add(owners[i]);
                    return (Client[]) array.toArray(new Client[0]);
                case 4:
                    prioritizeOkay = false;
                    prioritizeHasClient = true;
                    array = new Vector();
                    for (int i = 0; i < owners.length; i++)
                        array.add(owners[i]);
                    array.add(requester);
                    return (Client[]) array.toArray(new Client[0]);
                default:
                    fail("Unexpected PRIOR setting");
                    return null;
            }
        }

        public ResourceUsage[] prioritizeContention2(ResourceUsageImpl requester, ResourceUsageImpl[] owners)
        {
            // does nothing
            return null;
        }

        public void destroy()
        {
            // does nothing
        }

        public ResourceContentionManager getContentionManager()
        {
            throw new UnsupportedOperationException("Not implemented");
        }

        public Client negotiateRelease(Client owners[], Object data)
        {
            // Search for a willing participant
            // doesn't call owners in ascending priority order!
            for (int i = 0; i < owners.length; ++i)
            {
                if (owners[i].requestRelease(data)) return owners[i];
            }
            return null;
        }

        // ITSB INTEGRATION CHANGE
        // public ExtendedResourceUsage createResourceUsage(CallerContext
        // context,
        // Class type, Object data)
        // {
        // resourceUsageType = type;
        // return createResourceUsage(context);
        // }

        public int getWarningPeriod()
        {
            // does nothing
            return 0;
        }

        public void registerWarningPeriodListener(NotifySetWarningPeriod nsp)
        {
            // does nothing
        }

        public void deliverContentionWarningMessage(ResourceUsage requestedResourceUsage,
                ResourceUsage[] currentReservations)
        {
        }

        public boolean isContentionHandlerValid()
        {
            return true;
        }
    }

    static class NetworkMgr implements NetManager
    {
        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.impl.manager.Manager#destroy()
         */
        public void destroy()
        {
            // does nothing
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.NetManager#getNetworkInterfaceManager()
         */
        public NetworkInterfaceManager getNetworkInterfaceManager()
        {
            return TestNetworkInterfaceManager.getInstance();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.impl.manager.NetManager#getRCInterfaceManager()
         */
        public RCInterfaceManager getRCInterfaceManager()
        {
            // does nothing
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.NetManager#getReceiveBufferSize(java.net
         * .DatagramSocket)
         */
        public int getReceiveBufferSize(DatagramSocket d) throws SocketException
        {
            // does nothing
            return 0;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.NetManager#setReceiveBufferSize(java.net
         * .DatagramSocket, int)
         */
        public void setReceiveBufferSize(DatagramSocket d, int size) throws SocketException
        {
            // does nothing.
        }
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
        TestSuite suite = new TestSuite(NetworkInterfaceControllerTest.class);
        return suite;
    }

    public NetworkInterfaceControllerTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        replaceNetMgr();
        oldSM = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        csm = (CannedServiceMgr) CannedServiceMgr.getInstance();
        ManagerManagerTest.updateManager(ServiceManager.class, CannedServiceMgr.class, true, csm);

        niManager = NetworkInterfaceManager.getInstance();

        ServiceList slist = SIManager.createInstance().filterServices(new ServiceTypeFilter(ServiceType.DIGITAL_TV));
        int i;
        for (i = 0; i < slist.size() && tune1 == null; i++)
        {
            tune1 = createTuneLocator(slist.getService(i));
        }
        assertTrue("Could not find an initial test service", tune1 != null);

        for (; i < slist.size(); i++)
        {
            // Make sure tune2 is for a different frequency
            tune2 = createTuneLocator(slist.getService(i));
            if (tune2 != null && tune1.getFrequency() != tune2.getFrequency()) break;
        }

        assertTrue("Could not find two test services", tune1 != null && tune2 != null);
    }

    private OcapLocator createTuneLocator(javax.tv.service.Service service) throws Exception
    {
        ServiceDetails sd1 = ((org.cablelabs.impl.service.ServiceExt) service).getDetails();
        javax.tv.service.transport.TransportStream ts1 = ((ServiceDetailsExt) sd1).getTransportStream();
        if (ts1 != null)
        {
            int freq = ((org.cablelabs.impl.service.TransportStreamExt) ts1).getFrequency();
            int prog = ((ServiceDetailsExt) sd1).getProgramNumber();
            int mod = ((org.cablelabs.impl.service.TransportStreamExt) ts1).getModulationFormat();
            return new OcapLocator(freq, prog, mod);
        }
        else
        {
            return null;
        }
    }

    protected void tearDown() throws Exception
    {
        restoreNetMgr();
        ManagerManagerTest.updateManager(ServiceManager.class, ServiceMgrImpl.class, true, oldSM);

        if (csm != null)
        {
            //
            // There is a reference floating around for an old version of
            // the ServiceManager someone in the tests. This is causing failures
            // because the currently running test is referring to an old,
            // destoryed
            // ServiceManager. As a result, we aren't destroying these.
            //
            // csm.destroy();
        }

        super.tearDown();

        niManager = null;
    }

    protected OcapLocator getLocator() throws Exception
    {
        return TestNetworkInterface.successLocator;
    }

    protected OcapLocator getCanonicalLocator() throws Exception
    {
        return TestNetworkInterface.canonicalSuccessLocator;
    }

    protected OcapLocator getBadLocator() throws Exception
    {
        return TestNetworkInterface.failLocator;
    }

    protected TransportStream createTransportStream(final NetworkInterface ni, final int tsid, final int frequency,
            final int modulation)
    {
        return new TransportStreamExt()
        {
            public int getTransportStreamId()
            {
                return tsid;
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
                return ni;
            }

            public int getFrequency()
            {
                return frequency;
            }

            public int getModulationFormat()
            {
                return modulation;
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
    }

    protected TransportStream getTransportStream() throws Exception
    {
        return StreamTable.getTransportStreams(TestNetworkInterface.successLocator)[0];
    }

    protected TransportStream getBadTransportStream() throws Exception
    {
        return createTransportStream(getNetworkInterface(), 0xFFFF, TestNetworkInterface.failLocator.getFrequency(),
                TestNetworkInterface.failLocator.getModulationFormat());
    }

    protected NetworkInterfaceController getNetworkInterfaceController() throws Exception
    {
        return new NetworkInterfaceController(new TestResourceClient());
    }

    /**
     * get the requested NetworkInterface. Does no bounds checking
     */
    protected NetworkInterface getNetworkInterface(int num) throws Exception
    {
        return niManager.getNetworkInterfaces()[num];
    }

    protected NetworkInterface getNetworkInterface() throws Exception
    {
        return niManager.getNetworkInterfaces()[0];
    }

    public class DummySecurityManager extends NullSecurityManager
    {
        public Permission p;

        public void checkPermission(Permission p)
        {
            if (this.p == null) this.p = p;
        }
    }

    public class BogusSecurityManager extends NullSecurityManager
    {
        public void checkPermission(Permission p)
        {
            throw new SecurityException();
        }
    }

    private NetworkInterfaceManager niManager;

    private ServiceManager oldSM;

    private CannedServiceMgr csm;

    private OcapLocator tune1;

    private OcapLocator tune2;

    final static int EVENT_TIMEOUT = 3000;

    final static int CALLBACK_TIMEOUT = 3000;
}
