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

package org.cablelabs.impl.davic.net.tuning;

import java.util.Enumeration;
import java.util.Vector;

import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestor;
import javax.tv.service.ServiceType;
import javax.tv.service.selection.InvalidServiceComponentException;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContextListener;

import org.cablelabs.impl.manager.ApplicationManager;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.application.ApplicationTest.CCMgr;
import org.cablelabs.impl.manager.application.ApplicationTest.DummyContext;
import org.cablelabs.impl.manager.application.CannedApplicationManager;
import org.cablelabs.impl.manager.service.CannedServiceMgr;
import org.cablelabs.impl.ocap.resource.ApplicationResourceUsageImpl;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.service.ServiceContextResourceUsageImpl;
import org.cablelabs.test.TestUtils;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceController;
import org.davic.net.tuning.NetworkInterfaceEvent;
import org.davic.net.tuning.NetworkInterfaceManager;
import org.davic.net.tuning.NetworkInterfaceReleasedEvent;
import org.davic.net.tuning.NetworkInterfaceReservedEvent;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.davic.resources.ResourceStatusEvent;
import org.davic.resources.ResourceStatusListener;
import org.dvb.application.AppID;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests the NetworkInterfaceManagerImpl class.
 */
public class NetworkInterfaceManagerImplTest extends TestCase
{

    private CannedServiceMgr csm;

    private ServiceManager oldSM;

    private CCMgr ccm;

    private CallerContextManager oldCCM;

    private CannedApplicationManager cam;

    private ApplicationManager oldAM;

    private ResourceClient client;

    private NIMContext ctx;

    private NetworkInterfaceController controller;

    private CannedNetworkInterfaceManager mgr;

    private NetworkInterface nwif;

    private TestResourceListener rl;

    public void testAncestry()
    {
        TestUtils.testExtends(NetworkInterfaceManagerImpl.class, ExtendedNetworkInterfaceManager.class);
    }

    /**
     * Test the single constructor of NetworkInterfaceManagerImpl.
     * <ul>
     * NetworkInterfaceManagerImpl()
     * </ul>
     */
    public void testConstructors()
    {
        // NetworkInterfaceManagerImpl is unable to be explicitly instantiated.
        TestUtils.testNoPublicConstructors(NetworkInterfaceManagerImpl.class);
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(NetworkInterfaceManagerImpl.class);
    }

    /**
     * Test addResourceStatusListener()/removeResourceStatusEventListener()/
     * notifyResourceStatusListener()
     */
    public void testResourceStatusEventListener() throws Exception
    {
        TestResourceListener rl = new TestResourceListener();
        NetworkInterfaceManagerImpl manager = (NetworkInterfaceManagerImpl) NetworkInterfaceManager.getInstance();
        NetworkInterface ni = manager.getNetworkInterfaces()[0];

        // make sure there is at least one interface
        assertNotNull("No network interface exists", ni);

        // install resource status listener
        manager.addResourceStatusEventListener(rl);

        // send a NetworkInterfaceReservedEvent to the listener(s)
        synchronized (rl)
        {
            manager.notifyResourceStatusListener(new NetworkInterfaceReservedEvent(ni));
            rl.waitEvent(100);
        }
        // see if the listener received notification
        assertTrue("Listener was not fired for reservation event", rl.statusChangedFired);
        assertTrue("Listener did not receive NetworkInterfaceReservedEvent", rl.receiveNIReservedEvent);

        // send a resource released event
        synchronized (rl)
        {
            rl.reset();
            manager.notifyResourceStatusListener(new NetworkInterfaceReleasedEvent(ni));
            rl.waitEvent(100);
        }
        assertTrue("Listener was not fired for release event", rl.statusChangedFired);
        assertTrue("Listener did not receive NetworkInterfaceReleasedEvent", rl.receiveNIReleasedEvent);

        // remove resource status listener
        manager.removeResourceStatusEventListener(rl);

        // listener should not be called
        synchronized (rl)
        {
            rl.reset();
            manager.notifyResourceStatusListener(new NetworkInterfaceReservedEvent(ni));
            rl.waitEvent(100);
        }
        assertFalse("Removed listener was called for reserved event", rl.statusChangedFired);
        assertFalse("Listener did receive NetworkInterfaceReservedEvent", rl.receiveNIReservedEvent);

        // listener should not be called
        synchronized (rl)
        {
            rl.reset();
            manager.notifyResourceStatusListener(new NetworkInterfaceReleasedEvent(ni));
            rl.waitEvent(100);
        }
        assertFalse("Removed listener was called for release event", rl.statusChangedFired);
        assertFalse("Listener did receive NetworkInterfaceReleasedEvent", rl.receiveNIReleasedEvent);
    }

    public void testExplicitReservation() throws Exception
    {
        // tests that explicit reservations are released when the app exits.
        cam.cannedMapAppIDAndCC(new AppID(0x1, 0x1), ctx);
        ccm.alwaysReturned = ctx;
        ResourceUsageImpl ru = new ApplicationResourceUsageImpl(ctx);

        // make sure there is at least one interface
        assertNotNull("No network interface exists", nwif);

        // install resource status listener
        mgr.addResourceStatusEventListener(rl);

        synchronized (rl)
        {
            // first reserve a NetworkInterface
            mgr.reserve(ru, nwif, controller, client, null);
            rl.waitEvent(100);
        }
        // see if the listener received notification
        assertTrue("Listener was not fired for reservation event", rl.statusChangedFired);
        assertTrue("Listener did not receive NetworkInterfaceReservedEvent", rl.receiveNIReservedEvent);

        // simulate the app exiting
        synchronized (rl)
        {
            rl.resetAll();
            ctx.notifyDestroyed();
            rl.waitEvent(100);
        }
        // see if the listener received notification
        assertTrue("Listener was not fired for released event", rl.statusChangedFired);
        assertTrue("Listener did not receive NetworkInterfaceReleasedEvent", rl.receiveNIReleasedEvent);

        assertFalse("Expected NetworkInterface to be released", nwif.isReserved());
        // remove the registered listener
        mgr.removeResourceStatusEventListener(rl);
    }

    public void testImplicitReservation() throws Exception
    {
        // tests that implicit reservations are held when the app exits.
        cam.cannedMapAppIDAndCC(new AppID(0x1, 0x1), ctx);
        ccm.alwaysReturned = ctx;
        ResourceUsageImpl ru = new ServiceContextResourceUsageImpl(ctx, new DummyServiceContext(), new DummyService(), false);

        // make sure there is at least one interface
        assertNotNull("No network interface exists", nwif);

        // install resource status listener
        mgr.addResourceStatusEventListener(rl);

        synchronized (rl)
        {
            // first reserve a NetworkInterface
            mgr.reserve(ru, nwif, controller, client, null);
            rl.waitEvent(100);
        }
        // see if the listener received notification
        assertTrue("Listener was not fired for reservation event", rl.statusChangedFired);
        assertTrue("Listener did not receive NetworkInterfaceReservedEvent", rl.receiveNIReservedEvent);

        // simulate the app exiting
        synchronized (rl)
        {
            rl.resetAll();
            ctx.notifyDestroyed();
            rl.waitEvent(100);
        }
        // see if the listener received notification
        assertFalse("Did not expect listener fired when context destroyed", rl.statusChangedFired);
        assertFalse("Listener received NetworkInterfaceReleasedEvent", rl.receiveNIReleasedEvent);
        // verify priority is zero
        assertEquals("Expected priority to be zero", 0, ru.getPriority());
        // verify reservation is still held
        assertTrue("Expected NetworkInterface to still be reserved", nwif.isReserved());
    }

    class DummyService implements javax.tv.service.Service
    {
        /*
         * (non-Javadoc)
         * 
         * @see javax.tv.service.Service#getLocator()
         */
        public Locator getLocator()
        {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.tv.service.Service#getName()
         */
        public String getName()
        {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.tv.service.Service#getServiceType()
         */
        public ServiceType getServiceType()
        {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.tv.service.Service#hasMultipleInstances()
         */
        public boolean hasMultipleInstances()
        {
            // TODO Auto-generated method stub
            return false;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * javax.tv.service.Service#retrieveDetails(javax.tv.service.SIRequestor
         * )
         */
        public SIRequest retrieveDetails(SIRequestor requestor)
        {
            // TODO Auto-generated method stub
            return null;
        }
    }

    class DummyServiceContext implements javax.tv.service.selection.ServiceContext
    {
        /*
         * (non-Javadoc)
         * 
         * @see
         * javax.tv.service.selection.ServiceContext#addListener(javax.tv.service
         * .selection.ServiceContextListener)
         */
        public void addListener(ServiceContextListener listener)
        {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.tv.service.selection.ServiceContext#destroy()
         */
        public void destroy() throws SecurityException
        {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.tv.service.selection.ServiceContext#getService()
         */
        public javax.tv.service.Service getService()
        {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * javax.tv.service.selection.ServiceContext#getServiceContentHandlers()
         */
        public ServiceContentHandler[] getServiceContentHandlers() throws SecurityException
        {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * javax.tv.service.selection.ServiceContext#removeListener(javax.tv
         * .service.selection.ServiceContextListener)
         */
        public void removeListener(ServiceContextListener listener)
        {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * javax.tv.service.selection.ServiceContext#select(javax.tv.locator
         * .Locator[])
         */
        public void select(Locator[] components) throws InvalidLocatorException, InvalidServiceComponentException,
                SecurityException
        {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * javax.tv.service.selection.ServiceContext#select(javax.tv.service
         * .Service)
         */
        public void select(javax.tv.service.Service selection) throws SecurityException
        {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.tv.service.selection.ServiceContext#stop()
         */
        public void stop() throws SecurityException
        {
            // TODO Auto-generated method stub

        }
    }

    class NIMContext extends DummyContext
    {
        void notifyDestroyed()
        {
            synchronized (callbackData)
            {
                Enumeration e = callbackData.keys();
                while (e.hasMoreElements())
                {
                    CallbackData callback = (CallbackData) callbackData.get(e.nextElement());
                    callback.destroy(this);
                }
            }

        }

        public Object get(Object key)
        {
            if (key == CallerContext.APP_PRIORITY) return new Integer(100);
            if (key == CallerContext.APP_ID) return new AppID(0x1, 0x1);
            return super.get(key);
        }
    }

    class DummyResourceClient implements ResourceClient
    {
        /*
         * (non-Javadoc)
         * 
         * @see
         * org.davic.resources.ResourceClient#notifyRelease(org.davic.resources
         * .ResourceProxy)
         */
        public void notifyRelease(ResourceProxy proxy)
        {
            // do nothing
        }

        /*
         * (non-Javadoc)
         * 
         * @seeorg.davic.resources.ResourceClient#release(org.davic.resources.
         * ResourceProxy)
         */
        public void release(ResourceProxy proxy)
        {
            // do nothing
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.davic.resources.ResourceClient#requestRelease(org.davic.resources
         * .ResourceProxy, java.lang.Object)
         */
        public boolean requestRelease(ResourceProxy proxy, Object requestData)
        {
            return false;
        }
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
        TestSuite suite = new TestSuite(NetworkInterfaceManagerImplTest.class);
        return suite;
    }

    public NetworkInterfaceManagerImplTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();

        csm = (CannedServiceMgr) CannedServiceMgr.getInstance();
        oldSM = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        ManagerManagerTest.updateManager(ServiceManager.class, csm.getClass(), true, csm);

        oldCCM = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        ccm = new CCMgr(oldCCM);
        ManagerManagerTest.updateManager(CallerContextManager.class, ccm.getClass(), true, ccm);

        cam = (CannedApplicationManager) CannedApplicationManager.getInstance();
        oldAM = (ApplicationManager) ManagerManager.getInstance(ApplicationManager.class);
        ManagerManagerTest.updateManager(ApplicationManager.class, cam.getClass(), true, cam);

        client = new DummyResourceClient();
        ctx = new NIMContext();
        controller = new NetworkInterfaceController(client);
        mgr = new CannedNetworkInterfaceManager();
        nwif = mgr.getNetworkInterfaces()[0];
        rl = new TestResourceListener();
    }

    protected void tearDown() throws Exception
    {
        ManagerManagerTest.updateManager(ApplicationManager.class, oldAM.getClass(), true, oldAM);
        ManagerManagerTest.updateManager(CallerContextManager.class, oldCCM.getClass(), true, oldCCM);
        ManagerManagerTest.updateManager(ServiceManager.class, oldSM.getClass(), true, oldSM);

        cam.destroy();
        csm.destroy();

        super.tearDown();
    }

}
