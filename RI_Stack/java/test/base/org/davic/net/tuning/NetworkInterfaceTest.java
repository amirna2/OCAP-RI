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
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.davic.net.Locator;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.davic.resources.ResourceStatusEvent;
import org.davic.resources.ResourceStatusListener;
import org.dvb.net.rc.RCInterfaceManager;

import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.NetManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.service.CannedServiceMgr;
import org.cablelabs.test.TestUtils;

/**
 * Tests the NetworkInterface class.
 */
public class NetworkInterfaceTest extends TestCase
{
    public void testAncestry()
    {
        TestUtils.testExtends(NetworkInterface.class, Object.class);
    }

    /**
     * Test the single constructor of NetworkInterface.
     * <ul>
     * NetworkInterface()
     * </ul>
     */
    public void testConstructors()
    {
        // NetworkInterface is unable to be explicitly instantiated.
        TestUtils.testNoPublicConstructors(NetworkInterface.class);
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(NetworkInterface.class);
    }

    /**
     * Verify that getCurrentTransportStream() returns a TransportStream after a
     * successful tune and returns null after a failed tune.
     */
    public void testGetCurrentTransportStream_tuned() throws Exception
    {
        NetworkInterface ni = getNetworkInterface();
        TestListener listener = new TestListener();
        TestResourceClient rc = new TestResourceClient();
        Locator locator = getLocator();
        Locator badLocator = getBadLocator();
        NetworkInterfaceController nic = new NetworkInterfaceController(rc);

        try
        {
            // install resource status listener
            manager.addResourceStatusEventListener(listener);
            // install NetworkInterfaceEvent listener
            ni.addNetworkInterfaceListener(listener);

            // Reserve interface, which should succeed
            // And result in resource status event
            synchronized (listener)
            {
                nic.reserve(ni, null);
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("Listener was not fired for reservation event", listener.statusChangedFired);
            assertTrue("expected ResourceReserved event to be fired", listener.receiveNIReservedEvent);

            // tune the interface to a bad TransportStream
            synchronized (listener)
            {
                listener.reset();
                nic.tune(badLocator);
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("Listener was not fired for tune start event", listener.receiveNIEventFired);
            assertTrue("Listener did not receive a NetworkInterfaceTuningEvent", listener.receiveNITuningEvent);
            synchronized (listener)
            {
                // listener.reset();
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("Listener did not receive a NetworkInterfaceTuningOverEvent", listener.receiveNITuningOverEvent);
            assertFalse("Listener received a NetworkInterfaceTuningOverEvent with a SUCCEEDED status",
                    listener.tuneSucceeded);
            assertNull("Interface returned a TransportStream after failed tune", ni.getCurrentTransportStream());

            // tune the interface using a valid Locator
            synchronized (listener)
            {
                listener.reset();
                nic.tune(locator);
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
            assertTrue("Listener received a NetworkInterfaceTuningOverEvent with a FAILED status",
                    listener.tuneSucceeded);
            // see if getLocator() returns the correct locator
            assertNotNull("Interface returned null TransportStream after successful tune",
                    ni.getCurrentTransportStream());

            // release the interface which should generate ResourceStatusEvents
            synchronized (listener)
            {
                listener.reset();
                nic.release();
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("Listner did not receive a ResourceReleasedEvent", listener.statusChangedFired);
            assertTrue("Expected resourceReleased event to be fired", listener.receiveNIReleasedEvent);
            // remove the listeners
            manager.removeResourceStatusEventListener(listener);
            ni.removeNetworkInterfaceListener(listener);
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
            manager.removeResourceStatusEventListener(listener);
            ni.removeNetworkInterfaceListener(listener);
        }
    }

    public void testGetDeliverySystemType() throws Exception
    {
        NetworkInterface ni = getNetworkInterface();
        assertTrue("Delivery system should be cable",
                ni.getDeliverySystemType() == DeliverySystemType.CABLE_DELIVERY_SYSTEM);
    }

    /**
     * Verify getLocator() returns a locator after a vaild tune and returns null
     * after a tune failed.
     */
    public void testGetLocator_tuned() throws Exception
    {
        NetworkInterface ni = getNetworkInterface();
        TestListener listener = new TestListener();
        TestResourceClient rc = new TestResourceClient();
        Locator locator = getLocator();
        Locator badLocator = getBadLocator();
        NetworkInterfaceController nic = new NetworkInterfaceController(rc);

        // install resource status listener
        manager.addResourceStatusEventListener(listener);
        // install NetworkInterfaceEvent listener
        ni.addNetworkInterfaceListener(listener);

        try
        {
            // Reserve interface, which should succeed
            // And result in resource status event
            synchronized (listener)
            {
                nic.reserve(ni, null);
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("Listener was not fired for reservation event", listener.statusChangedFired);
            assertTrue("expected ResourceReserved event to be fired", listener.receiveNIReservedEvent);

            // tune the interface to an invalid locator
            synchronized (listener)
            {
                listener.reset();
                nic.tune(badLocator);
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
            assertFalse("Listener received a NetworkInterfaceTuningOverEvent with a SUCCEEDED status",
                    listener.tuneSucceeded);
            // see if getLocator() returns the correct locator
            assertNull("Interface returned locator after unsuccessful tune", ni.getLocator());

            // tune the interface using a valid Locator
            synchronized (listener)
            {
                listener.reset();
                nic.tune(locator);
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
            assertTrue("Listener received a NetworkInterfaceTuningOverEvent with a FAILED status",
                    listener.tuneSucceeded);
            // see if getLocator() returns the correct locator
            assertNotNull("Interface returned null locator after successful tune", ni.getLocator());

            // relase the interface which should generate ResourceStatusEvents
            synchronized (listener)
            {
                listener.reset();
                nic.release();
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("Listner did not receive a ResourceReleasedEvent", listener.statusChangedFired);
            assertTrue("Expected resourceReleased event to be fired", listener.receiveNIReleasedEvent);
            // remove the listeners
            manager.removeResourceStatusEventListener(listener);
            ni.removeNetworkInterfaceListener(listener);
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
            manager.removeResourceStatusEventListener(listener);
            ni.removeNetworkInterfaceListener(listener);

        }
    }

    public void testIsLocal() throws Exception
    {
        NetworkInterface ni = getNetworkInterface();
        assertTrue("NetworkInterface isLocal() should return true", ni.isLocal());
    }

    public void testIsReserved_unreserved() throws Exception
    {
        NetworkInterface ni = getNetworkInterface();
        assertFalse("NetworkInterface isReserved() should return false", ni.isReserved());
    }

    public void testIsReserved_reserved() throws Exception
    {
        TestResourceClient rc = new TestResourceClient();
        TestListener listener = new TestListener();
        NetworkInterface ni = getNetworkInterface();
        NetworkInterfaceController nic = new NetworkInterfaceController(rc);

        // install resource status listener
        manager.addResourceStatusEventListener(listener);

        try
        {
            // Reserve interface, which should succeed
            // And result in resource status event
            synchronized (listener)
            {
                nic.reserve(ni, null);
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("Listener was not fired for reservation event", listener.statusChangedFired);
            assertTrue("Interface returned the wrong value for isReserved()", ni.isReserved());

            synchronized (listener)
            {
                listener.reset();
                nic.release();
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("Listner did not receive a ResourceReleasedEvent", listener.statusChangedFired);
            // remove the listeners
            manager.removeResourceStatusEventListener(listener);
        }
        finally
        {
            manager.removeResourceStatusEventListener(listener);
            try
            {
                nic.release();
            }
            catch (NotOwnerException e)
            {
                // do nothing
            }
        }
    }

    public void XtestListAccessibleTransportStreams() throws Exception
    {
        fail("Unimplemented test");
    }

    /**
     * Test that the NetworkInterfaceListeners are fired when a tune occurs.
     */
    public void testNetworkInterfaceListener() throws Exception
    {
        TestResourceClient rc = new TestResourceClient();
        TestListener listener = new TestListener();
        NetworkInterface ni = getNetworkInterface();
        Locator locator = getLocator();
        NetworkInterfaceController nic = new NetworkInterfaceController(rc);

        // install resource status listener
        manager.addResourceStatusEventListener(listener);
        // install NetworkInterfaceEvent listener
        ni.addNetworkInterfaceListener(listener);

        try
        {
            // Reserve interface, which should succeed
            // And result in resource status event
            synchronized (listener)
            {
                nic.reserve(ni, null);
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("Listener was not fired for reservation event", listener.statusChangedFired);

            // tune the interface which should result in NetworkInterfaceEvents
            synchronized (listener)
            {
                listener.reset();
                nic.tune(locator);
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
            assertTrue("Listener received a FAILED status in the NetworkInterfaceTuningOverEvent",
                    listener.tuneSucceeded);

            // relase the interface which should generate ResourceStatusEvents
            synchronized (listener)
            {
                listener.reset();
                nic.release();
                listener.waitEvent(EVENT_TIMEOUT);
            }
            assertTrue("Listner did not receive a ResourceReleasedEvent", listener.statusChangedFired);
            // remove the listeners
            manager.removeResourceStatusEventListener(listener);
            ni.removeNetworkInterfaceListener(listener);
        }
        finally
        {
            manager.removeResourceStatusEventListener(listener);
            ni.removeNetworkInterfaceListener(listener);
            try
            {
                nic.release();
            }
            catch (NotOwnerException e)
            {
                // do nothing
            }
        }
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

    static class NetworkMgr implements NetManager
    {

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.impl.manager.Manager#destroy()
         */
        public void destroy()
        {
            mgr.destroy();
            mgr = null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.NetManager#getNetworkInterfaceManager()
         */
        public NetworkInterfaceManager getNetworkInterfaceManager()
        {

            return mgr;
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

        TestNetworkInterfaceManager mgr = (TestNetworkInterfaceManager) TestNetworkInterfaceManager.getInstance();
    }

    /**
     * This class allows test code in other packages to override the specified
     * methods.
     */
    public static abstract class NetworkInterface2 extends NetworkInterface
    {
        abstract protected void release();
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
        TestSuite suite = new TestSuite(NetworkInterfaceTest.class);
        return suite;
    }

    public NetworkInterfaceTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();

        // replace the ServiceManager before the NetManager so a
        // NetworkInterface gets the right ServiceManager
        // on construction.
        oldSM = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        csm = (CannedServiceMgr) CannedServiceMgr.getInstance();
        ManagerManagerTest.updateManager(ServiceManager.class, CannedServiceMgr.class, true, csm);

        netmgr = new NetworkMgr();
        netsave = (NetManager) ManagerManager.getInstance(NetManager.class);
        ManagerManagerTest.updateManager(NetManager.class, NetworkMgr.class, false, netmgr);
        manager = netmgr.getNetworkInterfaceManager();

    }

    protected void tearDown() throws Exception
    {
        if (netsave != null)
        {
            ManagerManagerTest.updateManager(NetManager.class, netsave.getClass(), true, netsave);
            netmgr.destroy();
            netsave = null;
        }

        if (oldSM != null)
        {
            ManagerManagerTest.updateManager(ServiceManager.class, oldSM.getClass(), true, oldSM);
            csm.destroy();
        }
        oldSM = null;
        manager = null;

        super.tearDown();

    }

    /**
     * get the requested NetworkInterface. Does no bounds checking
     */
    protected NetworkInterface getNetworkInterface(int num) throws Exception
    {
        return manager.getNetworkInterfaces()[num];
    }

    protected NetworkInterface getNetworkInterface() throws Exception
    {
        return manager.getNetworkInterfaces()[0];
    }

    protected Locator getLocator() throws Exception
    {
        return TestNetworkInterface.successLocator;
    }

    protected Locator getCanonicalLocator() throws Exception
    {
        return TestNetworkInterface.canonicalSuccessLocator;
    }

    protected Locator getBadLocator() throws Exception
    {
        return TestNetworkInterface.failLocator;
    }

    final static int EVENT_TIMEOUT = 30000;

    private NetworkInterfaceManager manager;

    private NetManager netsave;

    private ServiceManager oldSM;

    private CannedServiceMgr csm;

    private NetworkMgr netmgr;
}
