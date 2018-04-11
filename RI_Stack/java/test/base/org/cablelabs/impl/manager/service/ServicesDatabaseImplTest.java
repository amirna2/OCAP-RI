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

package org.cablelabs.impl.manager.service;

import org.cablelabs.impl.io.FileSysCommunicationException;
import org.cablelabs.impl.manager.AppStorageManager;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.AuthManager.AuthContext;
import org.cablelabs.impl.manager.appstorage.AppDescriptionInfo;
import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.service.ServicesDatabase;
import org.cablelabs.impl.service.ServicesDatabase.ServiceChangeListener;
import org.cablelabs.impl.signalling.AbstractServiceEntry;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.signalling.SignallingEvent;
import org.cablelabs.impl.signalling.XAppEntry;
import org.cablelabs.impl.signalling.Xait;
import org.cablelabs.impl.signalling.AppEntry.IcTransportProtocol;
import org.cablelabs.impl.signalling.AppEntry.LocalTransportProtocol;
import org.cablelabs.impl.signalling.AppEntry.OcTransportProtocol;
import org.cablelabs.impl.signalling.AppEntry.TransportProtocol;
import org.cablelabs.test.TestUtils;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import javax.tv.service.SIChangeType;
import javax.tv.service.navigation.DeliverySystemType;
import javax.tv.service.transport.ServiceDetailsChangeEvent;
import javax.tv.service.transport.ServiceDetailsChangeListener;
import javax.tv.service.transport.Transport;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.dvb.application.AppID;
import org.ocap.application.OcapAppAttributes;
import org.ocap.net.OcapLocator;
import org.ocap.service.AbstractService;
import org.ocap.service.AbstractServiceTest;

/**
 * Tests ServicesDatabaseImpl.
 * 
 * @author Aaron Kamienski
 */
public class ServicesDatabaseImplTest extends TestCase
{
    /**
     * Ensures that there are no public constructors.
     */
    public void testConstructor()
    {
        TestUtils.testNoPublicConstructors(ServicesDatabaseImpl.class);
    }

    /**
     * Tests boot().
     */
    public void X_testBoot()
    {
        fail("Unimplemented test");
    }

    /**
     * Tests bootMonitorApp().
     */
    public void X_testBootMonitorApp()
    {
        fail("Unimplemented test");
    }

    /**
     * Tests getInstance().
     */
    public void testGetInstance()
    {
        ServicesDatabase instance = ServicesDatabaseImpl.getInstance();

        assertNotNull("Expected non-null from getInstance", instance);
        assertSame("Expected singleton from getInstance", instance, ServicesDatabaseImpl.getInstance());
    }

    /**
     * Tests getAbstractService().
     */
    public void testGetAbstractService() throws Exception
    {
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        AbstractServiceEntry services[] = makeServices();

        // No such service should be known
        if (db.getAbstractService(services[0].id) != null)
        {
            fail("Expected InvalidLocatorException for no such service");
        }

        // Add services
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services)));

        // Make sure we can get each known service
        for (int i = 0; i < services.length; ++i)
        {
            OcapLocator loc = new OcapLocator(services[i].id);
            AbstractService service = db.getAbstractService(services[i].id);

            assertNotNull("getAbstractService() should not return null", service);

            assertEquals("Expected same locator for service " + services[i].id, loc, service.getLocator());
            assertEquals("Expected same name for service " + services[i].id, services[i].name, service.getName());

            // Check applications? Leave up to AbstractServiceImpl test.
        }

        // Check error conditions
        if (db.getAbstractService(0x400) != null)
        {
            fail("Expected InvalidLocatorException for no such service");
        }

        // Check for "thrown away" services
    }

    /**
     * Tests getAbstractServices().
     */
    public void testGetAbstractServices()
    {
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        AbstractServiceEntry services[] = makeServices();
        ServiceCollection collection = new ServiceCollection();
        db.getAbstractServices(collection);
        AbstractService[] svcs = (AbstractService[]) collection.getServices().toArray(new AbstractService[0]);

        // No services yet
        assertEquals("Unexpected number of services", 0, svcs.length);

        // Add services
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services)));

        collection = new ServiceCollection();
        db.getAbstractServices(collection);
        svcs = (AbstractService[]) collection.getServices().toArray(new AbstractService[0]);
        assertEquals("Unexpected number of services", services.length, svcs.length);

        // Copy into map
        Hashtable map = new Hashtable();
        for (int i = 0; i < services.length; ++i)
            map.put(new Integer(services[i].id), services[i]);

        // Check that we got each one back
        for (int i = 0; i < svcs.length; ++i)
        {
            OcapLocator loc = (OcapLocator) svcs[i].getLocator();
            Integer key = new Integer(loc.getSourceID());

            AbstractServiceEntry entry = (AbstractServiceEntry) map.get(key);
            assertNotNull("Did not expect to find service " + key, entry);

            assertEquals("Unexpected name for service " + key, entry.name, svcs[i].getName());

            map.remove(key);
        }

        assertEquals("Some services not returned", 0, map.size());
    }

    private void assertEquals(String msg, AbstractServiceEntry expected, AbstractServiceEntry actual)
    {
        assertEquals(msg + ": id", expected.id, actual.id);
        assertEquals(msg + ": name", expected.name, actual.name);
        assertEquals(msg + ": autoSelect", expected.autoSelect, actual.autoSelect);

        // Look at apps
        assertEquals(msg + ": apps.size()", expected.apps.size(), actual.apps.size());

        // Sort the apps so that we can compare...
        Vector expectedApps = (Vector) expected.apps.clone();
        Vector actualApps = (Vector) actual.apps.clone();
        Comparator c = new Comparator()
        {
            public int compare(Object o1, Object o2)
            {
                if (o1 == o2) return 0;

                int rc;

                XAppEntry app1 = (XAppEntry) o1;
                XAppEntry app2 = (XAppEntry) o2;

                AppID id1 = app1.id;
                AppID id2 = app2.id;
                rc = id1.getOID() - id2.getOID();
                if (rc == 0) rc = id1.getAID() - id2.getAID();
                if (rc == 0) rc = app1.controlCode - app2.controlCode;
                if (rc == 0) rc = app1.source - app2.source;
                if (rc == 0)
                {
                    rc = app1.priority - app2.priority;
                    if (rc == 0) rc = app1.launchOrder - app2.launchOrder;
                }
                // Don't use identityHashCode in case cloned...
                /*
                 * if ( rc == 0 ) rc = System.identityHashCode(o1) -
                 * System.identityHashCode(o2);
                 */

                return rc;
            }
        };
        Collections.sort(expectedApps, c);
        Collections.sort(actualApps, c);

        // Iterate over apps and compare...
        for (int i = 0; i < expectedApps.size(); ++i)
        {
            TestApp expApp = (TestApp) expectedApps.elementAt(i);
            XAppEntry actApp = (XAppEntry) actualApps.elementAt(i);

            assertTrue("Unexpected app " + actApp, 0 == c.compare(expApp, actApp));
        }
    }

    /**
     * Tests getServiceEntry().
     */
    public void testGetServiceEntry() throws Exception
    {
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        AbstractServiceEntry services[] = makeServices();

        assertTrue("Expected null given no services", null == db.getServiceEntry(services[0].id));

        // Add services
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services)));

        // Make sure we can get each known service
        for (int i = 0; i < services.length; ++i)
        {
            AbstractServiceEntry entry = db.getServiceEntry(services[i].id);

            assertNotNull("getServiceEntry() should not return null", entry);

            assertEquals("Expected equivalent AbstractServiceEntry", services[i], entry);
        }

        // Check non-existent services
        assertTrue("Expected null given no such service", null == db.getServiceEntry(0x400));

    }

    /**
     * Tests addAppSignalHandler.
     */
    public void X_testAddAppSignalHandler() throws Exception
    {
        fail("Unimplemented test");
    }

    /**
     * Tests addAppSignalHandler() and signalling.
     */
    public void X_testAddAppSignalHandler_signaled() throws Exception
    {
        fail("Unimplemented test");
    }

    /**
     * Tests addRemoveServiceChangeListener(). Just tests arbitrary
     * adds/removals, ensuring there are no crashes.
     */
    public void testAddRemoveServiceChangeListener() throws Exception
    {
        // Just test arbitrary adds/removals
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();

        db.addServiceChangeListener(0x12345, null);
        db.removeServiceChangeListener(0x12345, null);

        ServiceChangeListener l = new UpdateListener();

        db.addServiceChangeListener(0x12345, l);
        db.removeServiceChangeListener(0x12345, l);
        db.removeServiceChangeListener(0x12345, l);

        // These should be ignored altogether (should never be encountered)
        db.addServiceChangeListener(0x123, l);
        db.removeServiceChangeListener(0x123, l);
        db.removeServiceChangeListener(0x123, l);
    }

    /**
     * Tests invoking of serviceChangeListener for new services.
     */
    public void testServiceChangeListener() throws Exception
    {
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        AbstractServiceEntry services[] = makeServices();
        UpdateListener listeners[] = new UpdateListener[services.length];

        for (int i = 0; i < listeners.length; ++i)
        {
            listeners[i] = new UpdateListener();
            db.addServiceChangeListener(services[i].id, listeners[i]);
        }

        // Add services
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services)));

        // Listeners should only be invoked for a change!
        for (int i = 0; i < listeners.length; ++i)
        {
            assertSame("Expected listener NOT to be invoked", null, listeners[i].entry);
            assertEquals("Expected listener NOT to be invoked once", 0, listeners[i].services.size());

            listeners[i].clear();
        }

        // Now modify...
        AbstractServiceEntry services2[] = deepCopy(services);

        // Remove apps from a service
        TestApp removed = (TestApp) services2[0].apps.elementAt(0);
        services2[0].apps.removeElementAt(0);
        // Add some to another
        services2[1].apps.addElement(removed);
        //removed.serviceId = services2[1].id;
        // Modify one in another
        TestApp modified = (TestApp) services2[2].apps.elementAt(1);
        ++modified.version;
        // Clone another (should have no effect)
        services2[3] = services2[3].copy();

        // Update signalling
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2)));

        // Make sure that each listener was invoked once
        for (int i = 0; i < 3; ++i)
        {
            assertNotNull("Expected listener to be invoked", listeners[i].entry);
            assertEquals("Expected listener to be invoked once", 1, listeners[i].services.size());
            assertEquals("Expected given service", services2[i], listeners[i].entry);

            listeners[i].clear();
        }
        // Make sure listeners were NOT invoked
        for (int i = 3; i < listeners.length; ++i)
        {
            assertTrue("Expected listener to NOT be invoked", listeners[i].entry == null);
        }
    }

    /**
     * Tests invoking of serviceChangeListener for modified services.
     */
    public void testServiceChangeListener_modified() throws Exception
    {
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        AbstractServiceEntry services[] = makeServices();
        UpdateListener listeners[] = new UpdateListener[services.length];

        // Add services
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services)));

        // Add listeners
        for (int i = 0; i < listeners.length; ++i)
        {
            listeners[i] = new UpdateListener();
            db.addServiceChangeListener(services[i].id, listeners[i]);
        }

        // Modify...
        AbstractServiceEntry services2[] = deepCopy(services);

        // Modify autoselect
        services2[0].autoSelect = !services2[0].autoSelect;
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2)));
        for (int i = 0; i < listeners.length; ++i)
        {
            if (i == 0)
            {
                assertNotNull("Expected listener to be invoked", listeners[i].entry);
                assertEquals("Expected listener to be invoked once", 1, listeners[i].services.size());
                assertEquals("Expected given service", services2[i], listeners[i].entry);
            }
            else
                assertTrue("Expected listener to NOT be invoked", listeners[i].entry == null);
            listeners[i].clear();
        }

        // Modify name
        /*
         * services2 = deepCopy(services2); services2[1].name =
         * services2[1].name + "A" + services2[1].name;
         * db.signallingReceived(new SignallingEvent(this, new
         * XaitImpl(services2))); for(int i = 0; i < listeners.length; ++i) { if
         * (i == 1) { assertNotNull("Expected listener to be invoked",
         * listeners[i].entry);
         * assertEquals("Expected listener to be invoked once", 1,
         * listeners[i].services.size()); assertEquals("Expected given service",
         * services2[i], listeners[i].entry); } else
         * assertTrue("Expected listener to NOT be invoked", listeners[i].entry
         * == null); listeners[i].clear(); }
         */

        // Add apps
        TestApp newApp = new TestApp();
        services2 = deepCopy(services2);
        services2[2].apps.addElement(newApp);
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2)));
        for (int i = 0; i < listeners.length; ++i)
        {
            if (i == 2)
            {
                assertNotNull("Expected listener to be invoked", listeners[i].entry);
                assertEquals("Expected listener to be invoked once", 1, listeners[i].services.size());
                assertEquals("Expected given service", services2[i], listeners[i].entry);
            }
            else
                assertTrue("Expected listener to NOT be invoked", listeners[i].entry == null);
            listeners[i].clear();
        }

        // Add duplicate apps (updated priority/version)
        services2 = deepCopy(services2);
        newApp = (TestApp) ((TestApp) services2[2].apps.elementAt(1)).copy();
        ++newApp.priority;
        ++newApp.version;
        services2[2].apps.addElement(newApp);
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2)));
        for (int i = 0; i < listeners.length; ++i)
        {
            if (i == 2)
            {
                assertNotNull("Expected listener to be invoked", listeners[i].entry);
                assertEquals("Expected listener to be invoked once", 1, listeners[i].services.size());
                assertEquals("Expected given service", services2[i], listeners[i].entry);
            }
            else
                assertTrue("Expected listener to NOT be invoked", listeners[i].entry == null);
            listeners[i].clear();
        }

        // Remove apps
        services2 = deepCopy(services2);
        services2[3].apps.removeElementAt(1);
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2)));
        for (int i = 0; i < listeners.length; ++i)
        {
            if (i == 3)
            {
                assertNotNull("Expected listener to be invoked", listeners[i].entry);
                assertEquals("Expected listener to be invoked once", 1, listeners[i].services.size());
                assertEquals("Expected given service", services2[i], listeners[i].entry);
            }
            else
                assertTrue("Expected listener to NOT be invoked", listeners[i].entry == null);
            listeners[i].clear();
        }

        // Change app controlCode
        services2 = deepCopy(services2);
        TestApp modApp = (TestApp) services2[0].apps.elementAt(0);
        modApp.controlCode = (modApp.controlCode == OcapAppAttributes.KILL) ? OcapAppAttributes.PRESENT
                : OcapAppAttributes.KILL;
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2)));
        for (int i = 0; i < listeners.length; ++i)
        {
            if (i == 0)
            {
                assertNotNull("Expected listener to be invoked", listeners[i].entry);
                assertEquals("Expected listener to be invoked once", 1, listeners[i].services.size());
                assertEquals("Expected given service", services2[i], listeners[i].entry);
            }
            else
                assertTrue("Expected listener to NOT be invoked", listeners[i].entry == null);
            listeners[i].clear();
        }

        // Change app version - already tested above!
    }

    /**
     * Tests invoking of serviceChangeListener for unmodified services. (E.g.,
     * just reordering of info.)
     */
    public void testServiceChangeListener_unmodified()
    {
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        AbstractServiceEntry services[] = makeServices();
        UpdateListener listeners[] = new UpdateListener[services.length];

        // Add services
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services)));

        // Add listeners
        for (int i = 0; i < listeners.length; ++i)
        {
            listeners[i] = new UpdateListener();
            db.addServiceChangeListener(services[i].id, listeners[i]);
        }

        // (Don't) Modify...
        AbstractServiceEntry services2[] = deepCopy(services);

        // Reverse services
        for (int i = 0; i < services2.length / 2; ++i)
        {
            AbstractServiceEntry tmp = services2[i];
            services2[i] = services2[services2.length - i - 1];
            services2[services2.length - i - 1] = tmp;
        }
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2)));
        for (int i = 0; i < listeners.length; ++i)
            assertTrue("Expected listener to NOT be invoked", listeners[i].entry == null);

        // Reverse apps
        services2 = deepCopy(services);
        for (int i = 0; i < services2.length; ++i)
            Collections.reverse(services2[i].apps);
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2)));
        for (int i = 0; i < listeners.length; ++i)
            assertTrue("Expected listener to NOT be invoked", listeners[i].entry == null);
    }

    /**
     * Tests invoking of serviceChangeListener for destroyed apps.
     */
    public void testServiceChangeListener_destroyApps() throws Exception
    {
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        AbstractServiceEntry services[] = makeServices();
        UpdateListener listeners[] = new UpdateListener[services.length];

        // Add services
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services)));

        // Add listeners
        for (int i = 0; i < listeners.length; ++i)
        {
            listeners[i] = new UpdateListener();
            db.addServiceChangeListener(services[i].id, listeners[i]);
        }

        // Mark single app destroyed
        AbstractServiceEntry services2[] = deepCopy(services);
        TestApp modApp = (TestApp) services2[3].apps.elementAt(0);
        modApp.controlCode = OcapAppAttributes.DESTROY;
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2)));
        for (int i = 0; i < listeners.length; ++i)
        {
            if (i == 3)
            {
                assertNotNull("Expected listener to be invoked", listeners[i].entry);
                assertEquals("Expected listener to be invoked once", 1, listeners[i].services.size());
                assertEquals("Expected given service", services2[i], listeners[i].entry);
            }
            else
                assertTrue("Expected listener to NOT be invoked", listeners[i].entry == null);
            listeners[i].clear();
        }

        // Mark all apps destroyed
        services2 = deepCopy(services2);
        modApp = (TestApp) services2[3].apps.elementAt(2);
        modApp.controlCode = OcapAppAttributes.DESTROY;
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2)));
        for (int i = 0; i < listeners.length; ++i)
        {
            if (i == 3)
            {
                assertNotNull("Expected listener to be invoked", listeners[i].entry);
                assertEquals("Expected listener to be invoked once", 1, listeners[i].services.size());
                assertEquals("Expected given service", services2[i], listeners[i].entry);
            }
            else
                assertTrue("Expected listener to NOT be invoked", listeners[i].entry == null);
            listeners[i].clear();
        }
    }

    /**
     * Tests invoking of serviceDetailsChangeListener with network signalling
     * Tests the event generation described in OCAP 10.2.2.2.2 for network
     * signalling
     */
    public void testServiceDetailsChangeListener_network() throws Exception
    {
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        AbstractServiceEntry services[] = makeServices();
        DetailsListener listener = new DetailsListener();
        Transport trans = getCableTransport();
        trans.addServiceDetailsChangeListener(listener);

        // test adding services (c)
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services)));
        listener.checkInitialServiceDetailsChangeEvents();

        // test modifying services (d)
        AbstractServiceEntry services2[] = deepCopy(services);

        // Remove apps from a service
        TestApp removed = (TestApp) services2[0].apps.elementAt(0);
        services2[0].apps.removeElementAt(0);
        // Add some to another
        services2[1].apps.addElement(removed);
        //removed.serviceId = services2[1].id;
        // Modify one in another
        TestApp modified = (TestApp) services2[2].apps.elementAt(1);
        ++modified.version;
        // Clone another (should have no effect)
        services2[3] = services2[3].copy();

        // Update signalling
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2)));
        listener.checkServiceDetailsChangeEvent(SIChangeType.MODIFY, 0x020001);
        listener.checkServiceDetailsChangeEvent(SIChangeType.MODIFY, 0x030001);
        listener.checkServiceDetailsChangeEvent(SIChangeType.MODIFY, 0x020002);

        // remove a service from signalling (a)
        AbstractServiceEntry services3[] = new AbstractServiceEntry[services2.length - 1];
        services3[0] = services2[0];
        System.arraycopy(services2, 2, services3, 1, services3.length - 1);

        // Update signalling
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services3)));
        listener.checkServiceDetailsChangeEvent(SIChangeType.REMOVE, 0x030001);

        trans.removeServiceDetailsChangeListener(listener);
    }

    /**
     * Tests invoking of serviceDetailsChangeListener with registerUnboundApp
     * signalling Tests the event generation described in OCAP 10.2.2.2.2 for
     * registerUnboundApp() signalling.
     */
    public void testServiceDetailsChangeListener_register() throws Exception
    {
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        AbstractServiceEntry services[] = makeServices(Xait.REGISTER_UNBOUND_APP);
        DetailsListener listener = new DetailsListener();
        Transport trans = getCableTransport();
        trans.addServiceDetailsChangeListener(listener);

        // test adding services (c)
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services, Xait.REGISTER_UNBOUND_APP)));
        listener.checkInitialServiceDetailsChangeEvents();

        // Remove all apps in a single service (a)
        services = deepCopy(services);
        services[services.length / 2].apps.removeAllElements();
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services, Xait.REGISTER_UNBOUND_APP)));
        listener.checkServiceDetailsChangeEvent(SIChangeType.REMOVE, 0x04ffff);

        // signal apps w/out services (e)
        AbstractServiceEntry services2[] = deepCopy(services);
        TestApp[] apps = new TestApp[] { new TestApp(services2[0]), new TestApp(services2[1]),
                new TestApp(services2[2]), };

        db.signallingReceived(new SignallingEvent(this, new XaitImpl(null, apps, Xait.REGISTER_UNBOUND_APP)));
        listener.checkServiceDetailsChangeEvent(SIChangeType.MODIFY, 0x020002);
        listener.checkServiceDetailsChangeEvent(SIChangeType.MODIFY, 0x020001);
        listener.checkServiceDetailsChangeEvent(SIChangeType.MODIFY, 0x030001);

        // signal apps w/out services (f)
        TestApp[] apps2 = new TestApp[] { new TestApp(0x898989), new TestApp(0x89898a), new TestApp(0x898989) };

        db.signallingReceived(new SignallingEvent(this, new XaitImpl(null, apps2, Xait.REGISTER_UNBOUND_APP)));
        listener.checkUnexpectedEvent();
        trans.removeServiceDetailsChangeListener(listener);
    }

    /**
     * Tests invoking of service details listener for unmodified services.
     * (E.g., just reordering of info.)
     */
    public void testServiceDetailsChangeListener_unmodified() throws Exception
    {
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        AbstractServiceEntry services[] = makeServices();

        DetailsListener detailsListener = new DetailsListener();
        Transport trans = getCableTransport();
        trans.addServiceDetailsChangeListener(detailsListener);

        // Add services
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services)));
        detailsListener.checkInitialServiceDetailsChangeEvents();
        detailsListener.checkUnexpectedEvent();

        // (Don't) Modify...
        AbstractServiceEntry services2[] = deepCopy(services);

        // Reverse services
        for (int i = 0; i < services2.length / 2; ++i)
        {
            AbstractServiceEntry tmp = services2[i];
            services2[i] = services2[services2.length - i - 1];
            services2[services2.length - i - 1] = tmp;
        }
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2)));
        detailsListener.checkUnexpectedEvent();

        // Reverse apps
        services2 = deepCopy(services);
        for (int i = 0; i < services2.length; ++i)
            Collections.reverse(services2[i].apps);
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2)));
        detailsListener.checkUnexpectedEvent();
    }

    private void checkServices(String msg, ServicesDatabaseImpl db, AbstractServiceEntry[] services) throws Exception
    {
        for (int i = 0; i < services.length; ++i)
        {
            // Check entry
            AbstractServiceEntry entry = db.getServiceEntry(services[i].id);
            assertNotNull(msg + ": getServiceEntry should not return null", entry);
            assertEquals(msg + ": Expected same service", services[i], entry);

            // Check service
            OcapLocator loc = new OcapLocator(services[i].id);
            AbstractService service = db.getAbstractService(services[i].id);

            assertNotNull(msg + ": getAbstractService() should not return null", service);

            assertEquals(msg + ": Expected same locator for service " + services[i].id, loc, service.getLocator());
            assertEquals(msg + ": Expected same name for service " + services[i].id, services[i].name,
                    service.getName());

            // Check applications? Leave up to AbstractServiceImpl test.

            // Check the AppIDs...
            AbstractServiceTest.doTestGetAppIDs(services[i].apps, service);
            // Check the AppAttributes...
            AbstractServiceTest.doTestGetAppAttributes(services[i].apps, service);
        }

        // TODO(Todd): Check getAbstractServices()
    }

    /**
     * Tests signallingReceived() with network signalling. Abstract service no
     * longer signalled (a).
     */
    public void testSignallingReceived_networkRemoved() throws Exception
    {
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        AbstractServiceEntry services[] = makeServices();
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services)));

        // Make sure AbstractServiceEntries are there
        // Make sure AbstractServices are there
        checkServices("Could not setup services", db, services);

        // Remove a service
        AbstractServiceEntry services2[] = new AbstractServiceEntry[services.length - 1];
        services2[0] = services[0];
        System.arraycopy(services, 2, services2, 1, services2.length - 1);
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services)));

        // Make sure AbstractServiceEntry is removed
        // Make sure AbstractServiceEntries are there
        // Make sure AbstractService is removed
        // Make sure AbstractServices are there
        checkServices("After removing services", db, services2);
    }

    /**
     * Tests signallingReceived() with network signalling. Abstract service no
     * longer signalled, currentlySelected (b).
     */
    public void testSignallingReceived_networkMarkRemoved() throws Exception
    {
        // Signal initial set of services
        DetailsListener detailsListener = new DetailsListener();
        Transport trans = getCableTransport();
        trans.addServiceDetailsChangeListener(detailsListener);
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        AbstractServiceEntry services[] = makeServices();
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services)));
        detailsListener.checkInitialServiceDetailsChangeEvents();

        // Select a service
        AbstractService service = new AbstractServiceImpl(services[1]);
        db.addSelectedService(((OcapLocator)service.getLocator()).getSourceID());
        UpdateListener updateListener = new UpdateListener();
        db.addServiceChangeListener(services[1].id, updateListener);

        // Remove the selected service from signalling. We should get a remove
        // event
        // but the service should remain in the DB marked for removal.
        AbstractServiceEntry services2[] = new AbstractServiceEntry[services.length - 1];
        services2[0] = services[0];
        System.arraycopy(services, 2, services2, 1, services2.length - 1);
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2)));
        assertTrue("Expected listener to be invoked", updateListener.entry != null);
        detailsListener.checkServiceDetailsChangeEvent(SIChangeType.REMOVE, 0x030001);
        checkServices("After removing selected service", db, services);

        // Re-signal service removed and make sure nothing changes
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2)));
        assertTrue("Expected listener to NOT be invoked", updateListener.entry == null);
        detailsListener.checkUnexpectedEvent();
        checkServices("After removing selected service again", db, services);

        // Signal service as re-added and make sure it stays in the services
        // database
        // after it is unselected.
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services)));
        assertNotNull("Expected listener to be invoked", updateListener.entry);
        updateListener.clear();
        detailsListener.checkServiceDetailsChangeEvent(SIChangeType.ADD, 0x030001);
        db.removeSelectedService(((OcapLocator)service.getLocator()).getSourceID());
        checkServices("After re-adding selected service", db, services);

        // Signal service as removed again and make sure it goes away after it
        // is
        // un-selected.
        db.addSelectedService(((OcapLocator)service.getLocator()).getSourceID());
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2)));
        assertTrue("Expected listener to NOT be invoked", updateListener.entry == null);
        detailsListener.checkServiceDetailsChangeEvent(SIChangeType.REMOVE, 0x030001);
        checkServices("After removing selected service for the third time", db, services);
        db.removeSelectedService(((OcapLocator)service.getLocator()).getSourceID());
        checkServices("After un-selecting removed service", db, services2);

        trans.removeServiceDetailsChangeListener(detailsListener);
    }

    /**
     * Tests signallingReceived() with network signalling. Monitor application
     * flag carried forward with new signalling
     */
    public void testSignallingReceived_networkMonAppFlag() throws Exception
    {
        // Signal initial set of services
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        AbstractServiceEntry services[] = makeServices();
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services)));

        // Create new signalling with changed control code for monitor app
        AbstractServiceEntry services2[] = deepCopy(services);
        ((TestApp) services2[3].apps.get(2)).controlCode = OcapAppAttributes.PRESENT;

        // Mark application as the monitor app and make sure new signalling
        // carries
        // the flag forward.
        ((XAppEntry) services[3].apps.get(2)).isMonitorApp = true;
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2)));
        assertTrue("Monitor app flag not carried forward",
                ((XAppEntry) services2[3].apps.get(2)).isMonitorApp);
    }

    /**
     * Tests signallingReceived() with network signalling. Keep registered apps
     * while processing new network signalling
     */
    public void testSignallingReceived_networkKeepRegisteredApps() throws Exception
    {
        // Signal initial set of services
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        AbstractServiceEntry services[] = makeServices();
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services)));

        // Signal two sets of registered apps. One in a service registered
        // previously
        // by network signalling and another in a service only containing
        // registered
        // apps.
        AbstractServiceEntry[] services2 = new AbstractServiceEntry[] {
                new TestService(0x020001, "Service1", false, new TestApp[] { new TestApp(), new TestApp(),
                        new TestApp(), }, Xait.REGISTER_UNBOUND_APP),
                new TestService(0x090001, "Service9", false, new TestApp[] { new TestApp(), new TestApp(),
                        new TestApp(), }, Xait.REGISTER_UNBOUND_APP) };
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2, Xait.REGISTER_UNBOUND_APP)));

        // Re-signal initial set of network signaled services minus the one we
        // registered an app in.
        AbstractServiceEntry services3[] = new AbstractServiceEntry[services.length - 1];
        System.arraycopy(services, 1, services3, 0, services3.length);
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services3)));

        // Make sure all registered apps are present in both services.
        AbstractServiceEntry services4[] = new AbstractServiceEntry[services3.length + services2.length];
        System.arraycopy(services3, 0, services4, 0, services3.length);
        System.arraycopy(services2, 0, services4, services3.length, services2.length);
        checkServices("After all signalling", db, services4);
    }

    /**
     * Tests signallingReceived() with network signalling. Abstract service
     * newly signalled (c).
     */
    public void testSignallingReceived_networkAdded() throws Exception
    {
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        AbstractServiceEntry services[] = makeServices();
        AbstractServiceEntry services2[] = new AbstractServiceEntry[services.length - 1];
        services2[0] = services[0];
        System.arraycopy(services, 2, services2, 1, services2.length - 1);
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2)));

        // Make sure AbstractServiceEntries are there
        // Make sure AbstractServices are there
        checkServices("Could not setup services", db, services2);

        // Add a service
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services)));

        // Make sure AbstractServiceEntry is removed
        // Make sure AbstractServiceEntries are there
        // Make sure AbstractService is removed
        // Make sure AbstractServices are there
        checkServices("After adding service", db, services);
    }

    /**
     * Tests signallingReceived() with network signalling. Abstract service
     * modified (d). Modified includes:
     * <ul>
     * <li>service info
     * <li>app added
     * <li>app removed
     * <li>app modified
     * </ul>
     */
    public void testSignallingReceived_networkModified() throws Exception
    {
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        AbstractServiceEntry services[] = makeServices();

        // Add services
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services)));

        // Modify service info: autoselect
        AbstractServiceEntry[] services2 = deepCopy(services);
        services2[0].autoSelect = !services2[0].autoSelect;
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2)));
        checkServices("After modifying service", db, services2);

        // Add an application
        services2 = deepCopy(services2);
        services2[1].apps.addElement(new TestApp());
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2)));
        checkServices("After adding app", db, services2);

        // Remove an application
        services2 = deepCopy(services2);
        services2[1].apps.removeElementAt(2);
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2)));
        checkServices("After removing app", db, services2);

        // Modify an application : controlCode
        services2 = deepCopy(services2);
        TestApp modApp = (TestApp) services2[2].apps.elementAt(1);
        modApp.controlCode = (modApp.controlCode == OcapAppAttributes.KILL) ? OcapAppAttributes.PRESENT
                : OcapAppAttributes.KILL;
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2)));
        checkServices("After modifying app.controlCode", db, services2);

        // Modify an application : version
        services2 = deepCopy(services2);
        modApp = (TestApp) services2[1].apps.elementAt(1);
        ++modApp.version;
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2)));
        checkServices("After modifying app.version", db, services2);
    }

    /**
     * Tests signallingReceived() with network signalling. Cannot modify
     * registered applications.
     */
    public void testSignallingReceived_networkRegistered() throws Exception
    {
        // signal services+apps (register)
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        AbstractServiceEntry services[] = makeServices(Xait.REGISTER_UNBOUND_APP);
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services, Xait.REGISTER_UNBOUND_APP)));

        // Make sure AbstractServiceEntries are there
        // Make sure AbstractServices are there
        checkServices("Could not register services", db, services);

        // Modify an app
        AbstractServiceEntry[] services2 = deepCopy(services);
        TestApp modified = (TestApp) services2[0].apps.elementAt(1);
        modified.controlCode = (modified.controlCode == OcapAppAttributes.PRESENT) ? OcapAppAttributes.KILL
                : OcapAppAttributes.PRESENT;
        // Remove an app
        services2[1].apps.removeElementAt(0);

        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2)));

        // Make sure AbstractServiceEntries are there
        // Make sure AbstractServices are there
        checkServices("Modified registered apps w/ XAIT", db, services);

        // Make sure application wasn't removed
        /*
         * // unnecessary AbstractServiceEntry entry =
         * db.getServiceEntry(services[0].id); boolean found = false;
         * for(Enumeration e = entry.apps.elements(); e.hasMoreElements();) {
         * TestApp app = (TestApp)e.nextElement(); if
         * (modified.id.equals(app.id)) { found = true; break; } }
         * assertTrue("Expected app to NOT be removed by registration", found);
         */

        // Make sure application wasn't modified
        /*
         * // unnecessary entry = db.getServiceEntry(services[1].id);
         * for(Enumeration e = entry.apps.elements(); e.hasMoreElements();) {
         * TestApp app = (TestApp)e.nextElement(); if
         * (modified.id.equals(app.id)) {
         * assertFalse("Did not expect app to be modified by registration",
         * modified.controlCode == app.controlCode); break; } }
         */
    }

    /**
     * Tests signallingRecieved() with registerUnboundApp signalling. Don't try
     * and delete a service if it's not part of signalling.
     */
    public void testSignallingReceived_registerNoServices() throws Exception
    {
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        AbstractServiceEntry services[] = makeServices();
        // Register w/ Network signalling
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services)));

        // Make sure AbstractServiceEntries are there
        // Make sure AbstractServices are there
        checkServices("Could not setup services", db, services);

        // Modify services, adding apps, but don't reference one of them
        AbstractServiceEntry services2[] = new AbstractServiceEntry[services.length - 1];
        int j = 0;
        for (int i = 0; i < services.length; ++i)
        {
            if (i != services.length / 2)
            {
                /*
                 * services[i] = deepCopy(services[i]);
                 * services[i].apps.addElement(new TestApp());
                 */
                services2[j++] = deepCopy(services[i], true, Xait.REGISTER_UNBOUND_APP);
            }
        }
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2, Xait.REGISTER_UNBOUND_APP)));

        // Nothing should've changed (except the apps that were added)
        // Make sure AbstractServiceEntries are there
        // Make sure AbstractServices are there
        checkServices("Should ignore services not in fragment", db, services);
    }

    /**
     * Tests signallingReceived() with registerUnboundApp signalling. Abstract
     * service has no apps (a).
     */
    public void testSignallingReceived_registerNoApps() throws Exception
    {
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        AbstractServiceEntry services[] = makeServices(Xait.REGISTER_UNBOUND_APP);
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services, Xait.REGISTER_UNBOUND_APP)));

        // Make sure AbstractServiceEntries are there
        // Make sure AbstractServices are there
        checkServices("Could not register services", db, services);

        // Remove all apps in a single service
        services = deepCopy(services);
        services[services.length / 2].apps.removeAllElements();
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services, Xait.REGISTER_UNBOUND_APP)));

        AbstractServiceEntry[] services2 = new AbstractServiceEntry[services.length - 1];
        System.arraycopy(services, 0, services2, 0, services.length / 2);
        System.arraycopy(services, services.length / 2 + 1, services2, services.length / 2, services.length
                - services.length / 2 - 1);

        // Make sure AbstractServiceEntry/AbstractService was removed
        // Make sure AbstractServiceEntries are there
        // Make sure AbstractServices are there
        checkServices("Should remove service with no apps", db, services2);
    }

    /**
     * Tests signallingReceived() with registerUnboundApp signalling. Abstract
     * service has no apps, currently selected (b). (Should test w/ autoselected
     * and manual-selected.)
     */
    public void X_testSignallingReceived_registerNoAppsSelected()
    {
        fail("Unimplemented test");
    }

    /**
     * Tests signallingReceived() with registerUnboundApp signalling. Newly
     * signalled (c).
     */
    public void testSignallingReceived_registerAdded() throws Exception
    {
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        AbstractServiceEntry services[] = makeServices(Xait.REGISTER_UNBOUND_APP);
        AbstractServiceEntry services2[] = new AbstractServiceEntry[services.length - 1];
        services2[0] = services[0];
        System.arraycopy(services, 2, services2, 1, services2.length - 1);
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2, Xait.REGISTER_UNBOUND_APP)));

        // Make sure AbstractServiceEntries are there
        // Make sure AbstractServices are there
        checkServices("Could not register services", db, services2);

        // Add a service
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services, Xait.REGISTER_UNBOUND_APP)));

        // Make sure AbstractServiceEntry is removed
        // Make sure AbstractServiceEntries are there
        // Make sure AbstractService is removed
        // Make sure AbstractServices are there
        checkServices("After adding apps", db, services);
    }

    /**
     * Tests signallingReceived() with registerUnboundApp signalling. Abstract
     * service is modified (d). Modified includes:
     * <ul>
     * <li>service info
     * <li>app added
     * <li>app removed
     * <li>add modified
     * </ul>
     */
    public void X_testSignallingReceived_registerModified()
    {
        fail("Unimplemented test");
    }

    /**
     * Tests signallingReceived() with registerUnboundApp signalling. Abstract
     * service descriptor isn't necessary to update/add applications (but
     * service has to already be known) (e).
     */
    public void testSignallingReceived_registerKnownService() throws Exception
    {
        // signal services+apps (network or register?)
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        AbstractServiceEntry services[] = makeServices(Xait.REGISTER_UNBOUND_APP);
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services, Xait.REGISTER_UNBOUND_APP)));

        // Make sure AbstractServiceEntries are there
        // Make sure AbstractServices are there
        checkServices("Could not register services", db, services);

        // signal apps w/out services
        AbstractServiceEntry services2[] = deepCopy(services);
        TestApp[] apps = new TestApp[] { new TestApp(services2[0]), new TestApp(services2[1]),
                new TestApp(services2[2]), };
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(null, apps, Xait.REGISTER_UNBOUND_APP)));

        // Make sure AbstractServiceEntries are there
        // Make sure AbstractServices are there
        // Make sure new Apps are there...
        checkServices("After adding apps w/out abstract service descriptor", db, services2);

        // Make sure that new applications were added!
        for (int i = 0; i < apps.length; ++i)
        {
            // Check for service entry reference
            AbstractServiceEntry entry = db.getServiceEntry(apps[i].serviceId);

            assertNotNull("Expected a non-null entry", entry);
            boolean found = false;
            for (Enumeration e = entry.apps.elements(); e.hasMoreElements();)
            {
                TestApp app = (TestApp) e.nextElement();

                if (apps[i].id.equals(app.id))
                {
                    found = true;
                    break;
                }
            }
            assertTrue("Expected to find app in service entry", found);

            // Check for abstractService appAttributes reference
            AbstractService service = db.getAbstractService(services2[i].id);
            assertNotNull("Expected non-null service", service);
            found = false;
            for (Enumeration e = service.getAppAttributes(); e.hasMoreElements();)
            {
                OcapAppAttributes app = (OcapAppAttributes) e.nextElement();

                if (apps[i].id.equals(app.getIdentifier()))
                {
                    found = true;
                    break;
                }
            }
            assertTrue("Expected to find app in service's appAttributes", found);
        }
    }

    /**
     * Tests signallingReceived() with registerUnboundApp signalling. Previously
     * included apps that aren't included are deleted, just as for network
     * signalling.
     */
    public void testSignallingReceived_registerDeleteSkippedApps() throws Exception
    {
        // Signal service+apps
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        AbstractServiceEntry services[] = makeServices(Xait.REGISTER_UNBOUND_APP);
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services, Xait.REGISTER_UNBOUND_APP)));

        // Make sure AbstractServiceEntries are there
        // Make sure AbstractServices are there
        checkServices("Could not register services", db, services);

        // Add listeners
        UpdateListener listeners[] = new UpdateListener[services.length];
        for (int i = 0; i < listeners.length; ++i)
        {
            listeners[i] = new UpdateListener();
            db.addServiceChangeListener(services[i].id, listeners[i]);
        }

        // Signal one of the services, w/out an app - should be removed
        AbstractServiceEntry[] services2 = deepCopy(services);
        AbstractServiceEntry[] sub = { services2[0] };
        Object removed = sub[0].apps.elementAt(1);
        sub[0].apps.removeElementAt(1);
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(sub, Xait.REGISTER_UNBOUND_APP)));
        checkServices("Should have removed one app", db, services2);
        // Make sure listener is invoked
        for (int i = 0; i < listeners.length; ++i)
        {
            if (i == 0)
            {
                assertNotNull("Expected listener to be invoked", listeners[i].entry);
                assertEquals("Expected listener to be invoked once", 1, listeners[i].services.size());
                assertEquals("Expected given services", services2[i], listeners[i].entry);
            }
            else
                assertTrue("Expected listener to NOT be invoked", listeners[i].entry == null);
            listeners[i].clear();
        }

        // Signal one of the services, w/out an app, w/ an additional - should
        // be removed/added
        services2 = deepCopy(services2);
        sub = new AbstractServiceEntry[] { services2[0] };
        sub[0].apps.removeElementAt(0);
        sub[0].apps.addElement(removed);
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(sub, Xait.REGISTER_UNBOUND_APP)));
        checkServices("Should have removed one app, added another", db, services2);
        for (int i = 0; i < listeners.length; ++i)
        {
            if (i == 0)
            {
                assertNotNull("Expected listener to be invoked", listeners[i].entry);
                assertEquals("Expected listener to be invoked once", 1, listeners[i].services.size());
                assertEquals("Expected given services", services2[i], listeners[i].entry);
            }
            else
                assertTrue("Expected listener to NOT be invoked", listeners[i].entry == null);
            listeners[i].clear();
        }

        // Signal one of the services, w/ only a new app - should replace all
        services2 = deepCopy(services2);
        sub = new AbstractServiceEntry[] { services2[1] };
        sub[0].apps.clear();
        sub[0].apps.addElement(new TestApp());
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(sub, Xait.REGISTER_UNBOUND_APP)));
        checkServices("Should have replaced apps with new one", db, services2);
        for (int i = 0; i < listeners.length; ++i)
        {
            if (i == 1)
            {
                assertNotNull("Expected listener to be invoked", listeners[i].entry);
                assertEquals("Expected listener to be invoked once", 1, listeners[i].services.size());
                assertEquals("Expected given services", services2[i], listeners[i].entry);
            }
            else
                assertTrue("Expected listener to NOT be invoked", listeners[i].entry == null);
            listeners[i].clear();
        }
    }

    /**
     * Tests signallingReceived() with registerUnboundApp signalling. Ignore
     * applications for which there is no known service (f).
     */
    public void testSignallingReceived_registerUnknownService() throws Exception
    {
        // signal services+apps (network or register?)
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        AbstractServiceEntry services[] = makeServices(Xait.REGISTER_UNBOUND_APP);
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services, Xait.REGISTER_UNBOUND_APP)));

        // Make sure AbstractServiceEntries are there
        // Make sure AbstractServices are there
        checkServices("Could not register services", db, services);

        // signal apps w/out services
        TestApp[] apps = new TestApp[] { new TestApp(0x898989), new TestApp(0x89898a), new TestApp(0x898989) };
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(null, apps, Xait.REGISTER_UNBOUND_APP)));

        // Make sure AbstractServiceEntries are there
        // Make sure AbstractServices are there
        checkServices("Should not have added apps for unknown service", db, services);

        // Make sure that new applications were NOT added!
        for (int i = 0; i < apps.length; ++i)
        {
            // Check for service entry reference
            AbstractServiceEntry entry = db.getServiceEntry(apps[i].serviceId);

            assertSame("Expected null entry", null, entry);

            // Check for abstractService appAttributes reference
            AbstractService service = db.getAbstractService(apps[i].serviceId);
            assertSame("Expected null AbstractService", null, service);
        }
    }

    /**
     * Tests signallingReceived() with registerUnboundApp signalling. Cannot
     * modify/remove network-signalled applications.
     */
    public void testSignallingReceived_registerSignalled() throws Exception
    {
        // signal services+apps (network)
        DetailsListener detailsListener = new DetailsListener();
        Transport trans = getCableTransport();
        trans.addServiceDetailsChangeListener(detailsListener);
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        AbstractServiceEntry services[] = makeServices();
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services)));
        detailsListener.checkInitialServiceDetailsChangeEvents();

        // Make sure AbstractServiceEntries are there
        // Make sure AbstractServices are there
        checkServices("Could not register services", db, services);

        AbstractServiceEntry services2[] = new AbstractServiceEntry[2];
        services2[0] = deepCopy(services[0], true, Xait.REGISTER_UNBOUND_APP);
        services2[1] = deepCopy(services[1], true, Xait.REGISTER_UNBOUND_APP);

        // Modify an app
        TestApp modified = (TestApp) services2[0].apps.elementAt(1);
        modified.controlCode = (modified.controlCode == OcapAppAttributes.PRESENT) ? OcapAppAttributes.KILL
                : OcapAppAttributes.PRESENT;
        // Remove an app
        services2[1].apps.removeElementAt(0);

        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2, Xait.REGISTER_UNBOUND_APP)));
        detailsListener.checkServiceDetailsChangeEvent(SIChangeType.MODIFY, 0x030001);

        // Make sure AbstractServiceEntries are there
        // Make sure AbstractServices are there
        checkServices("Should not modify signalled apps w/ register", db, services);

        // Make sure application wasn't removed
        AbstractServiceEntry entry = db.getServiceEntry(services[0].id);
        boolean found = false;
        for (Enumeration e = entry.apps.elements(); e.hasMoreElements();)
        {
            TestApp app = (TestApp) e.nextElement();
            if (modified.id.equals(app.id))
            {
                found = true;
                break;
            }
        }
        assertTrue("Expected app to NOT be removed by registration", found);

        // Make sure application wasn't modified
        entry = db.getServiceEntry(services[1].id);
        for (Enumeration e = entry.apps.elements(); e.hasMoreElements();)
        {
            TestApp app = (TestApp) e.nextElement();
            if (modified.id.equals(app.id))
            {
                assertFalse("Did not expect app to be modified by registration",

                modified.controlCode == app.controlCode);
                break;
            }
        }
    }

    /**
     * Tests signallingReceived() with registerUnboundApp signalling. Abstract
     * service no longer signalled, currentlySelected.
     */
    public void testSignallingReceived_registerMarkRemoved() throws Exception
    {
        // Signal initial set of services
        DetailsListener detailsListener = new DetailsListener();
        Transport trans = getCableTransport();
        trans.addServiceDetailsChangeListener(detailsListener);
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        AbstractServiceEntry services[] = makeServices(Xait.REGISTER_UNBOUND_APP);
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services, Xait.REGISTER_UNBOUND_APP)));
        detailsListener.checkInitialServiceDetailsChangeEvents();

        // Select a service
        AbstractService service = new AbstractServiceImpl(services[1]);
        db.addSelectedService(((OcapLocator)service.getLocator()).getSourceID());
        UpdateListener updateListener = new UpdateListener();
        db.addServiceChangeListener(services[1].id, updateListener);

        // Remove the selected service from signalling. We should get a remove
        // event
        // but the service should remain in the DB marked for removal.
        AbstractServiceEntry services2[] = deepCopy(services);
        services2[1].apps.clear();
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2, Xait.REGISTER_UNBOUND_APP)));
        assertTrue("Expected listener to be invoked", updateListener.entry != null);
        detailsListener.checkServiceDetailsChangeEvent(SIChangeType.REMOVE, 0x030001);
        checkServices("After removing selected service", db, services2);

        // Re-signal service removed and make sure nothing changes
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2, Xait.REGISTER_UNBOUND_APP)));
        assertTrue("Expected listener to NOT be invoked", updateListener.entry == null);
        detailsListener.checkUnexpectedEvent();
        checkServices("After removing selected service again", db, services2);

        // Signal service as re-added and make sure it stays in the services
        // database
        // after it is unselected.
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services, Xait.REGISTER_UNBOUND_APP)));
        assertNotNull("Expected listener to be invoked", updateListener.entry);
        updateListener.clear();
        detailsListener.checkServiceDetailsChangeEvent(SIChangeType.ADD, 0x030001);
        db.removeSelectedService(((OcapLocator)service.getLocator()).getSourceID());
        checkServices("After re-adding selected service", db, services);

        // Signal service as removed again and make sure it goes away after it
        // is
        // un-selected.
        db.addSelectedService(((OcapLocator)service.getLocator()).getSourceID());
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2, Xait.REGISTER_UNBOUND_APP)));
        assertTrue("Expected listener to NOT be invoked", updateListener.entry == null);
        detailsListener.checkServiceDetailsChangeEvent(SIChangeType.REMOVE, 0x030001);
        checkServices("After removing selected service for the third time", db, services2);
        db.removeSelectedService(((OcapLocator)service.getLocator()).getSourceID());
        AbstractServiceEntry services3[] = new AbstractServiceEntry[services2.length - 1];
        services3[0] = services2[0];
        System.arraycopy(services2, 2, services3, 1, services3.length - 1);
        checkServices("After un-selecting removed service", db, services3);

        trans.removeServiceDetailsChangeListener(detailsListener);
    }

    /**
     * Tests signallingReceived() with registerUnboundApp signalling. Monitor
     * application flag carried forward with new signalling
     */
    public void testSignallingReceived_registerMonAppFlag() throws Exception
    {
        // Signal initial set of services
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        AbstractServiceEntry services[] = makeServices(Xait.REGISTER_UNBOUND_APP);
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services, Xait.REGISTER_UNBOUND_APP)));

        // Create new signalling with changed control code for monitor app
        AbstractServiceEntry services2[] = deepCopy(services);
        ((TestApp) services2[3].apps.get(2)).controlCode = OcapAppAttributes.PRESENT;

        // Mark application as the monitor app and make sure new signalling
        // carries
        // the flag forward.
        ((XAppEntry) services[3].apps.get(2)).isMonitorApp = true;
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services2, Xait.REGISTER_UNBOUND_APP)));
        assertTrue("Monitor app flag not carried forward",
                ((XAppEntry) services2[3].apps.get(2)).isMonitorApp);
    }

    /**
     * Tests unregisterUnboundApp().
     */
    public void testUnregisterUnboundApp() throws Exception
    {
        // signal services+apps
        DetailsListener detailsListener = new DetailsListener();
        Transport trans = getCableTransport();
        trans.addServiceDetailsChangeListener(detailsListener);
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        AbstractServiceEntry services[] = makeServices(Xait.REGISTER_UNBOUND_APP);
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services, Xait.REGISTER_UNBOUND_APP)));
        checkServices("Could not register services", db, services);
        detailsListener.checkInitialServiceDetailsChangeEvents();

        // Make sure AbstractServiceEntries are there
        // Make sure AbstractServices are there
        checkServices("Could not register services", db, services);

        // Add listeners
        UpdateListener listeners[] = new UpdateListener[services.length];
        for (int i = 0; i < listeners.length; ++i)
        {
            listeners[i] = new UpdateListener();
            db.addServiceChangeListener(services[i].id, listeners[i]);
        }

        // Unregister apps
        db.unregisterUnboundApp(services[1].id, ((AppEntry) services[1].apps.elementAt(2)).id);
        db.unregisterUnboundApp(services[0].id, ((AppEntry) services[0].apps.elementAt(0)).id);

        // Make sure they are removed
        AbstractServiceEntry services2[] = deepCopy(services);
        services2[1].apps.removeElementAt(2);
        services2[0].apps.removeElementAt(0);
        checkServices("Problems unregistering", db, services2);

        // Make sure listener is invoked
        for (int i = 0; i < listeners.length; ++i)
        {
            if (i == 0 || i == 1)
            {
                assertNotNull("Expected listener to be invoked", listeners[i].entry);
                assertEquals("Expected listener to be invoked once", 1, listeners[i].services.size());
                assertEquals("Expected given services", services2[i], listeners[i].entry);
            }
            else
                assertTrue("Expected listener to NOT be invoked", listeners[i].entry == null);
        }
        detailsListener.checkServiceDetailsChangeEvent(SIChangeType.MODIFY, services[1].id);
        detailsListener.checkServiceDetailsChangeEvent(SIChangeType.MODIFY, services[0].id);
    }

    /**
     * Tests unregisterUnboundApp(), case where service is marked for removal.
     */
    public void testUnregisterUnboundApp_markRemoved() throws Exception
    {
        // Signal initial services
        DetailsListener detailsListener = new DetailsListener();
        Transport trans = getCableTransport();
        trans.addServiceDetailsChangeListener(detailsListener);
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        AbstractServiceEntry services[] = makeServices(Xait.REGISTER_UNBOUND_APP);
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services, Xait.REGISTER_UNBOUND_APP)));
        checkServices("Could not register services", db, services);
        detailsListener.checkInitialServiceDetailsChangeEvents();

        // Select a service
        AbstractService service = new AbstractServiceImpl(services[1]);
        db.addSelectedService(((OcapLocator)service.getLocator()).getSourceID());
        UpdateListener updateListener = new UpdateListener();
        db.addServiceChangeListener(services[1].id, updateListener);

        // Unregister all apps in selected service. This should cause the
        // service
        // to be marked for removal.
        db.unregisterUnboundApp(services[1].id, ((AppEntry) services[1].apps.elementAt(0)).id);
        assertNotNull("Expected listener to be invoked", updateListener.entry);
        detailsListener.checkServiceDetailsChangeEvent(SIChangeType.MODIFY, services[1].id);
        db.unregisterUnboundApp(services[1].id, ((AppEntry) services[1].apps.elementAt(1)).id);
        assertNotNull("Expected listener to be invoked", updateListener.entry);
        detailsListener.checkServiceDetailsChangeEvent(SIChangeType.MODIFY, services[1].id);
        db.unregisterUnboundApp(services[1].id, ((AppEntry) services[1].apps.elementAt(2)).id);
        assertNotNull("Expected listener to be invoked", updateListener.entry);
        detailsListener.checkServiceDetailsChangeEvent(SIChangeType.REMOVE, services[1].id);
        AbstractServiceEntry services2[] = deepCopy(services);
        services2[1].apps.clear();
        checkServices("After removing apps from selected service", db, services2);

        // Un-select service and make sure it goes away.
        db.removeSelectedService(((OcapLocator)service.getLocator()).getSourceID());
        AbstractServiceEntry services3[] = new AbstractServiceEntry[services2.length - 1];
        services3[0] = services2[0];
        System.arraycopy(services2, 2, services3, 1, services3.length - 1);
        checkServices("After un-selecting service", db, services3);

        trans.removeServiceDetailsChangeListener(detailsListener);
    }

    /**
     * Tests unregisterUnboundApp(), cases where not allowed or invalid.
     * Including:
     * <ul>
     * <li>not found (at all)
     * <li>not found (in given service)
     * <li>network signalled app
     * <li>host device app
     * <li>registered by other app
     * </ul>
     */
    public void testUnregisterUnboundApp_invalid() throws Exception
    {
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();

        // signal registered services+apps
        AbstractServiceEntry registered[] = makeServices(Xait.REGISTER_UNBOUND_APP);
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(registered, Xait.REGISTER_UNBOUND_APP)));

        // signal network services+apps
        AbstractServiceEntry network[] = makeServices();
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(network)));

        AbstractServiceEntry services[] = deepCopy(registered);
        for (int i = 0; i < services.length; ++i)
        {
            for (Enumeration e = network[i].apps.elements(); e.hasMoreElements();)
            {
                services[i].apps.addElement(e.nextElement());
            }
        }
        // Make sure AbstractServiceEntries are there
        // Make sure AbstractServices are there
        checkServices("Could not register services", db, services);

        // Find bad serviceid
        // Find bad appid
        int badSvc = 0;
        int badOid = 0, badAid = 0;
        for (int i = 0; i < services.length; ++i)
        {
            badSvc = Math.max(services[i].id, badSvc);
            for (Enumeration e = services[i].apps.elements(); e.hasMoreElements();)
            {
                AppEntry app = (AppEntry) e.nextElement();
                badOid = Math.max(app.id.getOID(), badOid);
                badAid = Math.max(app.id.getAID(), badAid);
            }
        }
        ++badSvc;
        AppID badAppId = new AppID(badOid++, badAid++);

        // Attempt to modify non-existing apps
        // good service, bad appid
        try
        {
            db.unregisterUnboundApp(services[0].id, badAppId);
            // fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        { /* empty */
        }
        // bad service, good appid
        try
        {
            db.unregisterUnboundApp(badSvc, ((AppEntry) services[0].apps.elementAt(0)).id);
            // fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        { /* empty */
        }
        // bad service, bad appid
        try
        {
            db.unregisterUnboundApp(badSvc, badAppId);
            // fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        { /* empty */
        }

        // Attempt to modify network svc/app
        // network service, bad appid
        try
        {
            db.unregisterUnboundApp(network[0].id, badAppId);
        }
        catch (IllegalArgumentException e)
        { /* empty */
        }
        // network service, network appid
        try
        {
            db.unregisterUnboundApp(network[1].id, ((AppEntry) network[1].apps.elementAt(0)).id);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        { /* empty */
        }
        // bad service, network appid
        try
        {
            db.unregisterUnboundApp(badSvc, ((AppEntry) network[0].apps.elementAt(0)).id);
            // fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        { /* empty */
        }

        // Attempt to modify app giving wrong service
        db.unregisterUnboundApp(registered[0].id, ((AppEntry) registered[1].apps.elementAt(0)).id);

        // Null AppID
        try
        {
            db.unregisterUnboundApp(registered[1].id, null);
            fail("Expected NullPointerException");
        }
        catch (NullPointerException e)
        { /* empty */
        }

        // Host app service id
        try
        {
            db.unregisterUnboundApp(0x12345, ((AppEntry) registered[1].apps.elementAt(0)).id);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        { /* empty */
        }

        // Bound service id
        try
        {
            db.unregisterUnboundApp(0x1234, ((AppEntry) registered[2].apps.elementAt(1)).id);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        { /* empty */
        }

        // Make sure nothing changed!
        checkServices("Did not expect any changes", db, services);
    }

    /**
     * Tests addSelectedService() and removeSelectedService()
     */
    public void testAddRemoveSelectedService() throws Exception
    {
        // Create test database and services
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        AbstractServiceEntry services[] = makeServices();
        db.signallingReceived(new SignallingEvent(this, new XaitImpl(services)));
        int service1 = services[0].id;
        int service2 = services[1].id;

        // Create a service which is not in the database
        AbstractServiceEntry unknownServiceEntry = new AbstractServiceEntry();
        unknownServiceEntry.id = 0x20003;

        // Make sure services not in the database cannot be selected
        assertFalse("Able to add unknown service", db.addSelectedService(unknownServiceEntry.id) != null);

        // Make sure services can be selected
        assertTrue("Cannot add the 1st selected service", db.addSelectedService(service1) != null);
        assertTrue("Cannot add the 2nd selected service", db.addSelectedService(service2) != null);

        // Make sure services cannot be selected more than once
        assertFalse("Added 1st selected service twice", db.addSelectedService(service1) != null);
        assertFalse("Added 2nd selected service twice", db.addSelectedService(service2) != null);

        // Make sure services can be re-selected after they are removed
        db.removeSelectedService(services[0].id);
        assertTrue("Cannot re-add the 1st selected service", db.addSelectedService(service1) != null);
        db.removeSelectedService(service2);
        assertTrue("Cannot re-add the 2nd selected service", db.addSelectedService(service2) != null);

        // Make sure attempts to un-select services more than once are silent
        db.removeSelectedService(service1);
        db.removeSelectedService(service2);
        db.removeSelectedService(service1);
        db.removeSelectedService(service2);

        // Make sure attempt to un-select never known service is silent
        db.removeSelectedService(unknownServiceEntry.id);
    }

    /**
     * Tests requests to AppStorageManager to store applications.
     * <ul>
     * <li>Ordering of storage requests, highest priority first.
     * <li>All apps are stored. Regardless of priority or app control code.
     * <li>On a change, all deleted apps are deleted.
     * </ul>
     */
    public void testAppStorageRequest_network()
    {
        doTestAppStorageRequest(new ServicesDatabaseImpl(), Xait.NETWORK_SIGNALLING);
    }

    /**
     * Tests requests to AppStorageManager to store applications.
     * <ul>
     * <li>Ordering of storage requests, highest priority first.
     * <li>All apps are stored. Regardless of priority or app control code.
     * <li>On a change, all deleted apps are deleted.
     * </ul>
     */
    public void testAppStorageRequest_registered1()
    {
        doTestAppStorageRequest(new ServicesDatabaseImpl(), Xait.REGISTER_UNBOUND_APP);
    }

    /**
     * Tests requests to AppStorageManager to store applications.
     * <ul>
     * <li>Ordering of storage requests, highest priority first.
     * <li>All apps are stored. Regardless of priority or app control code.
     * <li>On a change, all deleted apps are deleted.
     * </ul>
     */
    private void doTestAppStorageRequest(ServicesDatabaseImpl db, int source)
    {
        AbstractServiceEntry services[] = makeServices(source);

        // Add services
        Xait xait = new XaitImpl(services, source);
        XAppEntry[] apps = (XAppEntry[])xait.getApps();
        db.signallingReceived(new SignallingEvent(this, xait));

        // No apps should've been deleted on first XAIT
        assertEquals("No apps should've been deleted yet", 0, asm.deleted.size());

        // Expected all apps to be stored
        assertEquals("Unexpected number of apps stored", apps.length, asm.stored.size());

        // Expected all apps to be stored in priority-descending order
        int lastPriority = Integer.MAX_VALUE;
        for (Enumeration e = asm.stored.elements(); e.hasMoreElements();)
        {
            AppData app = (AppData) e.nextElement();
            assertTrue("Expected apps to be stored in priority-descending order (" + lastPriority + "<" + app.priority
                    + ")", lastPriority >= app.priority);
            lastPriority = app.priority;
        }

        // Verify that all apps are stored as expected
        for (int i = 0; i < apps.length; ++i)
        {
            checkStoredApp(apps[i]);
        }

        // Now, let's change things a bit...
        services = deepCopy(services);

        // * Delete apps
        AppData del1, del2;
        deleteApp(services, del1 = (AppData) asm.stored.elementAt(2));
        deleteApp(services, del2 = (AppData) asm.stored.lastElement());
        // * Add apps
        services[0].apps.addElement(new TestApp());
        services[1].apps.addElement(new TestApp(33, new TransportProtocol[] { new OC() }, "/dir"));
        services[services.length - 1].apps.addElement(new TestApp(34, new TransportProtocol[] { new IC() }, "/"));
        // * Modify app priorities
        swapAppPriority(services, (AppData) asm.stored.elementAt(0),
                (AppData) asm.stored.elementAt(asm.stored.size() - 2));
        // * Modify app control code
        modifyACC(services, (AppData) asm.stored.elementAt(1));

        asm.deleted.clear();
        asm.stored.clear();

        // Update XAIT
        xait = new XaitImpl(services, source);
        apps = (XAppEntry[])xait.getApps();
        db.signallingReceived(new SignallingEvent(this, xait));

        // Expected two apps to be deleted
        assertEquals("Expected 2 apps to be deleted", 2, asm.deleted.size());
        AppKey key = new AppKey(del1.id, del1.version);
        assertNull("Deleted app should not be in DB " + key, asm.db.get(key));
        key = new AppKey(del2.id, del1.version);
        assertNull("Deleted app should not be in DB " + key, asm.db.get(key));

        // Expected all apps to be stored
        assertEquals("Unexpected number of apps stored", apps.length, asm.stored.size());

        // Expected all apps to be stored in priority-descending order
        lastPriority = Integer.MAX_VALUE;
        for (Enumeration e = asm.stored.elements(); e.hasMoreElements();)
        {
            AppData app = (AppData) e.nextElement();
            assertTrue("Expected apps to be stored in priority-descending order (" + lastPriority + "<" + app.priority
                    + ")", lastPriority >= app.priority);
            lastPriority = app.priority;
        }

        // Verify that all apps are stored as expected
        for (int i = 0; i < apps.length; ++i)
        {
            checkStoredApp(apps[i]);
        }
    }

    /**
     * Tests requests to AppStorageManager to store applications (based upon app
     * registration).
     * <ul>
     * <li>On unregister, all deleted apps are deleted.
     * </ul>
     */
    public void testAppStorageRequest_unregister()
    {
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();
        doTestAppStorageRequest(db, Xait.REGISTER_UNBOUND_APP);

        // Find an app to unregister
        AppData toDel = (AppData) asm.stored.lastElement();
        // Find all versions of app
        Vector toDelAll = new Vector();
        for (Enumeration e = asm.stored.elements(); e.hasMoreElements();)
        {
            AppData app = (AppData) e.nextElement();

            if (app.id.equals(toDel.id)) toDelAll.addElement(app);
        }
        // Find app and service
        int service = -1;
        ServiceCollection collection = new ServiceCollection();
        db.getAbstractServices(collection);
        AbstractService[] svcs = (AbstractService[]) collection.getServices().toArray(new AbstractService[0]);
        for (int i = 0; i < svcs.length; ++i)
        {
            for (Enumeration e = svcs[i].getAppIDs(); e.hasMoreElements();)
            {
                AppID id = (AppID) e.nextElement();
                if (id.equals(toDel.id))
                {
                    service = ((OcapLocator) svcs[i].getLocator()).getSourceID();
                    break;
                }
            }
        }
        assertTrue("Internal error - could not find service!", service != -1);

        asm.deleted.clear();
        asm.stored.clear();

        // Unregister the app
        db.unregisterUnboundApp(service, toDel.id);
        assertEquals("No apps should've been stored by unregister", 0, asm.stored.size());
        assertEquals("Expected app to be deleted from storage", toDelAll.size(), asm.deleted.size());
        for (Enumeration e = toDelAll.elements(); e.hasMoreElements();)
        {
            AppData app = (AppData) e.nextElement();
            assertNull("Expected app to be deleted from storage: " + app, asm.db.get(new AppKey(app.id, app.version)));
        }
    }

    /**
     * Tests proper order of apps with same AppID in store requests.
     * 
     * @param source
     */
    private void doTestAppStorageRequestSame(int source)
    {
        ServicesDatabaseImpl db = new ServicesDatabaseImpl();

        // Start w/ an empty XAIT
        Xait xait = new XaitImpl(new AbstractServiceEntry[0]);
        db.signallingReceived(new SignallingEvent(this, xait));
        assertEquals("No apps should've been deleted yet", 0, asm.deleted.size());
        assertEquals("No apps should've been stored yet", 0, asm.stored.size());
        assertEquals("No apps should've been stored yet", 0, asm.db.size());

        TransportProtocol[] oc = { new OC() };
        TransportProtocol[] ic = { new IC() };
        int OID = 0xbeefcafe;
        AppID id1 = new AppID(OID, 1);
        AppID id2 = new AppID(OID, 2);

        AbstractServiceEntry[] services = {
                new TestService(0x020001, "Service1", false,
                        new TestApp[] { new TestApp(id1, 2, 50, oc, "/" + id1 + "/ver2", 2),
                                new TestApp(id1, 1, 0, oc, "/" + id1, 1), new TestApp(id2, 0xFF, 0, oc, "/junk", 0),
                                new TestApp(id1, 3, 25, ic, "/" + id1 + "/ver3", 0), }, source),
                new TestService(0x07FFFF, "Service2", false, new TestApp[] { new TestApp(id2, 2, 1, oc, "/", 1),
                        new TestApp(id2, 1, 3, oc, "/", 1), new TestApp(id2, 3, 2, oc, "/", 1),
                        new TestApp(id2, 4, 3, oc, "/", 1), }, source), };

        // Signal apps
        xait = new XaitImpl(services, source);
        XAppEntry[] apps = (XAppEntry[])xait.getApps();
        db.signallingReceived(new SignallingEvent(this, xait));

        // Verify storage
        assertEquals("Expected no apps to be deleted from storage", 0, asm.deleted.size());
        assertEquals("Expected all apps to be stored", apps.length, asm.stored.size());

        // Expected all apps to be stored in priority-descending order
        int lastPriority = Integer.MAX_VALUE;
        for (Enumeration e = asm.stored.elements(); e.hasMoreElements();)
        {
            AppData app = (AppData) e.nextElement();
            assertTrue("Expected apps to be stored in priority-descending order (" + lastPriority + "<" + app.priority
                    + ")", lastPriority >= app.priority);
            lastPriority = app.priority;
        }

        // Verify that all apps are stored as expected
        for (int i = 0; i < apps.length; ++i)
        {
            checkStoredApp(apps[i]);
        }
    }

    /**
     * Tests proper order of apps with same AppID in store requests.
     */
    public void testAppStorageRequestSame_network()
    {
        doTestAppStorageRequestSame(Xait.NETWORK_SIGNALLING);
    }

    /**
     * Tests proper order of apps with same AppID in store requests.
     */
    public void testAppStorageRequestSame_registered()
    {
        doTestAppStorageRequestSame(Xait.REGISTER_UNBOUND_APP);
    }

    /* ================= Support ================== */

    private void checkStoredApp(XAppEntry origApp)
    {
        AppKey key = new AppKey(origApp.id, origApp.version);
        AppData app = (AppData) asm.db.get(key);

        assertNotNull("Expected app to be stored " + key, app);

        // Check id, version, baseDir, priority, tp
        assertEquals("Unexpected AppID: " + key, origApp.id, app.id);
        assertEquals("Unexpected version: " + key, origApp.version, app.version);
        assertEquals("Unexpected priority: " + key, origApp.storagePriority, app.priority);
        assertEquals("Unexpected baseDir: " + key, origApp.baseDirectory, app.baseDir);
        assertEquals("Unexpected tp[]: " + key, origApp.transportProtocols, app.tp);
    }

    private AppEntry deleteApp(AbstractServiceEntry[] services, AppData app)
    {
        for (int i = 0; i < services.length; ++i)
        {
            AbstractServiceEntry service = services[i];
            if (service.apps == null) continue;
            for (int j = 0; j < service.apps.size(); ++j)
            {
                XAppEntry x = (XAppEntry) service.apps.elementAt(j);
                if (app.id.equals(x.id))
                {
                    if (x.version == app.version)
                    {
                        service.apps.removeElementAt(j);
                        return x;
                    }
                }
            }
        }
        fail("Could not find app to remove: " + app.id + ":" + app.version);
        return null;
    }

    private TestApp findApp(AbstractServiceEntry[] services, AppData app)
    {
        for (int i = 0; i < services.length; ++i)
        {
            AbstractServiceEntry service = services[i];
            if (service.apps == null) continue;
            for (int j = 0; j < service.apps.size(); ++j)
            {
                XAppEntry x = (XAppEntry) service.apps.elementAt(j);
                if (app.id.equals(x.id))
                {
                    if (x.version == app.version)
                    {
                        return (TestApp) x;
                    }
                }
            }
        }
        fail("Could not find app to remove: " + app.id + ":" + app.version);
        return null;
    }

    private void swapAppPriority(AbstractServiceEntry[] services, AppData app1, AppData app2)
    {
        TestApp xapp1 = findApp(services, app1);
        TestApp xapp2 = findApp(services, app2);

        xapp1.storagePriority = app2.priority;
        xapp2.storagePriority = app1.priority;
    }

    private void modifyACC(AbstractServiceEntry[] services, AppData app)
    {
        TestApp xapp = findApp(services, app);
        switch (xapp.controlCode)
        {
            case OcapAppAttributes.AUTOSTART:
                xapp.controlCode = OcapAppAttributes.PRESENT;
                break;
            case OcapAppAttributes.PRESENT:
                xapp.controlCode = OcapAppAttributes.AUTOSTART;
                break;
            case OcapAppAttributes.DESTROY:
            case OcapAppAttributes.KILL:
            case OcapAppAttributes.REMOTE:
                xapp.controlCode = OcapAppAttributes.PRESENT;
                break;
        }
    }

    /**
     * Performs a "deep" copy of the given service array.
     */
    private AbstractServiceEntry[] deepCopy(AbstractServiceEntry orig[])
    {
        return deepCopy(orig, false, 0);
    }

    private AbstractServiceEntry[] deepCopy(AbstractServiceEntry orig[], boolean modSource, int source)
    {
        AbstractServiceEntry copy[] = new AbstractServiceEntry[orig.length];
        for (int i = 0; i < orig.length; ++i)
            copy[i] = deepCopy(orig[i], modSource, source);
        return copy;
    }

    /**
     * Performs a "deep" copy of the given service. That is the apps array is
     * copied and the elements in the apps arrays are copied. Note that the
     * elements in the apps array are not deep copied.
     */
    private AbstractServiceEntry deepCopy(AbstractServiceEntry orig, boolean modSource, int source)
    {
        AbstractServiceEntry copy = orig.copy();
        copy.apps = new Vector();
        for (Enumeration e = orig.apps.elements(); e.hasMoreElements();)
        {
            TestApp app = (TestApp) e.nextElement();
            app = (TestApp) app.copy();
            if (modSource) app.source = source;
            copy.apps.addElement(app);
        }
        return copy;
    }

    private AbstractServiceEntry[] makeServices()
    {
        return makeServices(Xait.NETWORK_SIGNALLING);
    }

    private AbstractServiceEntry[] makeServices(int source)
    {
        TransportProtocol[] tp1 = { new OC(), new OC() };
        TransportProtocol[] tp2 = { new IC(), new OC(), new OC() };
        TransportProtocol[] tp3 = { new IC() };
        TransportProtocol[] tp4 = { new IC(), new IC() };

        return new AbstractServiceEntry[] {
                new TestService(0x020001, "Service1", false, new TestApp[] { new TestApp(), new TestApp(),
                        new TestApp(), }, source),
                new TestService(0x030001, "Service2", false, new TestApp[] { new TestApp(2, tp1, "/"),
                        new TestApp(99, tp2, "/racerx/stuff"), new TestApp(1, tp3, "/"), }, source),
                new TestService(0x020002, "Service3", false, new TestApp[] { new TestApp(0, tp4, "/gogo/mach5"),
                        new TestApp(0, tp3, "/"), }, source),
                new TestService(0x04FFFF, "Service4", false, new TestApp[] {
                        new TestApp(1000, 20, OcapAppAttributes.PRESENT, 1, 100, 20),
                        new TestApp(1000, 21, OcapAppAttributes.DESTROY, 0x7FFFFFFF, 255, 20),
                        new TestApp(1000, 21, OcapAppAttributes.AUTOSTART, 0x80001234, 200, 20),
                        new TestApp(1000, 23, OcapAppAttributes.KILL, 100, 210, 0), }, source),
                new TestService(0x0FFFFF, "Service5", false, new TestApp[] { new TestApp(), new TestApp(), }, source),
                new TestService(0x041234, "Service6", false, new TestApp[] {
                        new TestApp(), // extra
                        new TestApp(1001, 120, OcapAppAttributes.PRESENT, 3, 100, 20),
                        new TestApp(1001, 121, OcapAppAttributes.PRESENT, 1, 1, 1), // extra
                        new TestApp(1001, 120, OcapAppAttributes.DESTROY, 2, 101, 20), // this
                                                                                       // one
                                                                                       // (priority)
                        new TestApp(1001, 120, OcapAppAttributes.REMOTE, 1, 100, 19),
                        new TestApp(1001, 119, OcapAppAttributes.KILL, 1, 101, 21), // this
                                                                                    // one
                                                                                    // (launch
                                                                                    // order)
                        new TestApp(1001, 119, OcapAppAttributes.AUTOSTART, 2, 101, 20),
                        new TestApp(1001, 119, OcapAppAttributes.DESTROY, 3, 101, 20), new TestApp(), // extra
                }, source), };
    }

    private Transport getCableTransport()
    {
        ServiceManager serviceMgr = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        SICache cache = serviceMgr.getSICache();
        Transport transports[] = cache.getTransports();

        for (int i = 0; i < transports.length; i++)
        {
            if (transports[i].getDeliverySystemType() == DeliverySystemType.CABLE)
            {
                return transports[i];
            }
        }
        return null;
    }

    // Copied from AitTest w/ minor mods
    public static void assertEquals(String msg, TransportProtocol[] expected, TransportProtocol[] actual)
    {
        if (expected == null)
            assertSame(msg, expected, actual);
        else
        {
            // assertFalse(msg + ": did not expect EXACT same protocols",
            // expected == actual);

            assertNotNull(msg, actual);

            assertEquals(msg + ".length", expected.length, actual.length);
            for (int i = 0; i < expected.length; ++i)
                assertEquals(msg + i, expected[i], actual[i]);
        }
    }

    // Copied from AitTest w/ minor mods
    public static void assertEquals(String msg, TransportProtocol expected, TransportProtocol actual)
    {
        if (expected == null)
            assertSame(msg, expected, actual);
        else
        {
            // assertFalse(msg + ": did not expect EXACT same protocol",
            // expected == actual);

            assertEquals(msg + ": label", expected.label, actual.label);
            assertEquals(msg + ": remote", expected.remoteConnection, actual.remoteConnection);
            assertEquals(msg + ": service", expected.serviceId, actual.serviceId);
            if (expected instanceof OcTransportProtocol)
            {
                OcTransportProtocol ex = (OcTransportProtocol) expected;
                OcTransportProtocol ac = (OcTransportProtocol) actual;

                assertEquals(msg + ": comp", ex.componentTag, ac.componentTag);
            }
            else if (expected instanceof IcTransportProtocol)
            {
                IcTransportProtocol ex = (IcTransportProtocol) expected;
                IcTransportProtocol ac = (IcTransportProtocol) actual;

                //assertEquals(msg + ": url", ex.url, ac.url);
            }
            else if (expected instanceof LocalTransportProtocol)
            {
                // Don't do anything
            }
        }
    }

    /**
     * Replacement AbstractServiceEntry impl.
     */
    static class TestService extends AbstractServiceEntry
    {
        public TestService(int id, String name, boolean autoSelect)
        {
            this(id, name, autoSelect, null);
        }

        public TestService(int id, String name, boolean autoSelect, TestApp[] apps)
        {
            this(id, name, autoSelect, apps, Xait.NETWORK_SIGNALLING);
        }

        public TestService(int id, String name, boolean autoSelect, TestApp[] apps, int source)
        {
            this.name = name;
            this.id = id;
            this.autoSelect = autoSelect;
            this.apps = new Vector();
            if (apps != null) for (int i = 0; i < apps.length; ++i)
            {
                this.apps.addElement(apps[i]);
                apps[i].serviceId = id;
                apps[i].source = source;
            }
        }
    }

    /**
     * Replacement AppEntry/AppSignalling entry.
     */
    static class TestApp extends XAppEntry
    {
        public int source;

        public static int nextOid = 0x10000;

        public static int nextAid = 0x1;

        public static synchronized AppID makeAppID()
        {
            if (nextAid++ == 0xFFFF)
            {
                ++nextOid;
                nextAid = 1;
            }
            return new AppID(nextOid, nextAid);
        }

        public static synchronized AppID makeAppID(int oid, int aid)
        {
            aid = nextAid + aid;
            if (aid > 0xFFFF)
            {
                oid = oid + (aid >> 16) & 0xFFFF;
                aid = aid & 0xFFFF;
            }
            oid = nextOid + oid;

            return new AppID(aid, oid);
        }

        public TestApp()
        {
            this(makeAppID(), OcapAppAttributes.PRESENT, 0);
        }

        public TestApp(AbstractServiceEntry service)
        {
            this();
            this.serviceId = service.id;
            service.apps.addElement(this);
        }

        public TestApp(int serviceId)
        {
            this();
            this.serviceId = serviceId;
        }

        public TestApp(AppID id, int controlCode, int version)
        {
            this.id = id;
            this.controlCode = controlCode;
            this.version = version;
            this.source = Xait.NETWORK_SIGNALLING;
            this.names = new Hashtable();
            this.names.put("eng", "App");
            this.versions = new Hashtable();
            this.serviceBound = true;
            this.visibility = AppEntry.VISIBLE;
            this.priority = 200;
            this.parameters = new String[0];
            this.baseDirectory = "/";
            this.className = "MainXlet";
            this.classPathExtension = new String[0];
            this.transportProtocols = new TransportProtocol[0];
        }

        public TestApp(AppID id, int controlCode, int version, int serviceId)
        {
            this(id, controlCode, version);
            this.serviceId = serviceId;
        }

        public TestApp(int oid, int aid, int controlCode, int version, int priority, int launchOrder)
        {
            this(makeAppID(oid, aid), controlCode, version);
            this.priority = priority;
            this.launchOrder = launchOrder;
        }

        public TestApp(int storagePriority, TransportProtocol[] tp, String baseDir)
        {
            this();
            this.storagePriority = storagePriority;
            this.transportProtocols = tp;
            this.baseDirectory = baseDir;
        }

        public TestApp(AppID id, int version, int storagePriority, TransportProtocol[] tp, String baseDir,
                int launchOrder)
        {
            this();
            this.id = id;
            this.version = version;
            this.storagePriority = storagePriority;
            this.transportProtocols = tp;
            this.baseDirectory = baseDir;
            this.launchOrder = launchOrder;
        }

        public AppID getAppID()
        {
            return id;
        }

        public int getControlCode()
        {
            return controlCode;
        }

        public int getType()
        {
            return OcapAppAttributes.OCAP_J;
        }

        public AppEntry getAppEntry()
        {
            return this;
        }

        public int getServiceId()
        {
            return serviceId;
        }

        public long getVersionNumber()
        {
            return version;
        }

        public int getSource()
        {
            return source;
        }
    }

    class XaitImpl implements Xait
    {
        public AbstractServiceEntry[] services;

        public XAppEntry[] otherApps;

        public int source;

        public XaitImpl(AbstractServiceEntry[] services)
        {
            this(services, null, NETWORK_SIGNALLING);
        }

        public XaitImpl(AbstractServiceEntry[] services, int source)
        {
            this(services, null, source);
        }

        public XaitImpl(AbstractServiceEntry[] services, XAppEntry[] otherApps, int source)
        {
            this.services = (services == null) ? (new AbstractServiceEntry[0]) : services;
            this.source = source;
            this.otherApps = otherApps;
        }

        public AppEntry[] getApps()
        {
            Vector apps = new Vector();
            for (int i = 0; i < services.length; ++i)
            {
                if (services[i].apps != null) for (Enumeration e = services[i].apps.elements(); e.hasMoreElements();)
                    apps.addElement(e.nextElement());
            }
            int length = apps.size() + ((otherApps == null) ? 0 : otherApps.length);
            XAppEntry[] array = new XAppEntry[length];
            apps.copyInto(array);
            if (otherApps != null) System.arraycopy(otherApps, 0, array, apps.size(), otherApps.length);
            return array;
        }

        public int getType()
        {
            return OcapAppAttributes.OCAP_J;
        }

        public ExternalAuthorization[] getExternalAuthorization()
        {
            return new ExternalAuthorization[0];
        }

        public byte[] getPrivilegedCertificateBytes()
        {
            return new byte[0];
        }

        public AbstractServiceEntry[] getServices()
        {
            AbstractServiceEntry[] copy = new AbstractServiceEntry[services.length];
            System.arraycopy(services, 0, copy, 0, services.length);
            return copy;
        }

        public int getSource()
        {
            return source;
        }

        public int getVersion()
        {
            return 0;
        }

        public boolean filterApps(Properties securityProps,
                Properties registeredProps)
        {
            return false;
        }
    }

    class UpdateListener implements ServiceChangeListener
    {
        public AbstractServiceEntry entry;

        public Vector services = new Vector();

        public void clear()
        {
            entry = null;
            services.removeAllElements();
        }

        public void serviceUpdate(AbstractServiceEntry e)
        {
            this.entry = e;
            services.addElement(e);
        }
    }

    class DetailsListener implements ServiceDetailsChangeListener
    {
        public Vector events = new Vector();

        public synchronized void notifyChange(ServiceDetailsChangeEvent event)
        {
            events.add(event);
            notifyAll();
        }

        public synchronized void checkServiceDetailsChangeEvent(SIChangeType changeType, int serviceId)
                throws Exception
        {
            if (events.size() == 0) wait(LISTENER_TIMEOUT);
            ServiceDetailsChangeEvent event = (ServiceDetailsChangeEvent) events.get(0);
            AbstractServiceImpl service = (AbstractServiceImpl) event.getServiceDetails().getService();
            assertEquals("Expected ServiceDetailsChangeEvent with change type " + changeType, changeType,
                    event.getChangeType());
            assertEquals("Expected ServiceDetailsChangeEvent for service ID " + serviceId, serviceId,
                    ((OcapLocator) service.getLocator()).getSourceID());
            events.remove(0);
        }

        public void checkInitialServiceDetailsChangeEvents() throws Exception
        {
            checkServiceDetailsChangeEvent(SIChangeType.ADD, 0x020001);
            checkServiceDetailsChangeEvent(SIChangeType.ADD, 0x030001);
            checkServiceDetailsChangeEvent(SIChangeType.ADD, 0x020002);
            checkServiceDetailsChangeEvent(SIChangeType.ADD, 0x04ffff);
            checkServiceDetailsChangeEvent(SIChangeType.ADD, 0x0fffff);
            checkServiceDetailsChangeEvent(SIChangeType.ADD, 0x041234);
        }

        // Check for unexpected events
        public synchronized void checkUnexpectedEvent() throws Exception
        {
            clearEvents();
            if (events.size() != 0) assertEquals("Expected no more events: " + events.elementAt(0), 0, events.size());
            events.removeAllElements();
        }
    }

    /**
     * Extension of <code>OcTransportProtocol</code> that provides a
     * constructor.
     * 
     * @author Aaron Kamienski
     */
    static class OC extends OcTransportProtocol
    {
        private static int TAG = 1;

        static int LABEL = 1;

        private static int SERVICE = 1;

        OC()
        {
            this.componentTag = (TAG++) & 0xFF;
            this.label = (LABEL++) & 0xFF;
            this.serviceId = (SERVICE++) & 0xFFFF;
        }
    }

    /**
     * Extension of <code>IcTransportProtocol</code> that provides a
     * constructor.
     * 
     * @author Aaron Kamienski
     */
    static class IC extends IcTransportProtocol
    {
        private static int PORT = 8000;

        IC()
        {
            this.label = (OC.LABEL++) & 0xFF;
            this.urls.add("http://www.cablelabs.org:" + (PORT++));
        }
    }

    /**
     * Class used as a key to track stored applications, encapsulating
     * <code>AppID</code> and <i>version</i>.
     * 
     * @author Aaron Kamienski
     */
    class AppKey
    {
        public AppKey(AppID id, long version)
        {
            this.id = id;
            this.version = version;
        }

        public boolean equals(Object obj)
        {
            if (obj instanceof AppKey)
            {
                AppKey other = (AppKey) obj;
                return id.equals(other.id) && version == other.version;
            }
            return false;
        }

        public int hashCode()
        {
            return id.hashCode() ^ ((int)(version & 0xFFFFFFFF));
        }

        public String toString()
        {
            return id + ":" + version;
        }

        final AppID id;

        final long version;
    }

    /**
     * Class used to encapsulate information about stored applications.
     * 
     * @author Aaron Kamienski
     */
    class AppData
    {
        public AppData(AppID id, long version, int priority, TransportProtocol[] tp, String baseDir)
        {
            this.id = id;
            this.version = version;
            this.priority = priority;
            this.tp = tp;
            this.baseDir = baseDir;
        }

        public boolean equals(Object obj)
        {
            if (obj instanceof AppData)
            {
                AppData other = (AppData) obj;
                return (id.equals(other.id) && version == other.version && priority == other.priority
                        && (baseDir == other.baseDir || (baseDir != null && baseDir.equals(other.baseDir))) && equals(
                        tp, other.tp));
            }
            return false;
        }

        private boolean equals(TransportProtocol[] tp1, TransportProtocol[] tp2)
        {
            if (tp1 == tp2)
                return true;
            else if (tp1 != null && tp1.length == tp2.length)
            {
                for (int i = 0; i < tp1.length; ++i)
                    if (tp1[i] != tp2[i]) return false;
                return true;
            }
            return false;
        }

        public int hashCode()
        {
            return id.hashCode() ^ ((int)(version & 0xFFFFFFFF)) ^ priority;
        }

        final AppID id;

        final long version;

        final int priority;

        final TransportProtocol[] tp;

        final String baseDir;
    }

    /**
     * Stub <code>AppStorageManager</code>, used to catch and record storage
     * requests.
     * 
     * @author Aaron Kamienski
     */
    class FakeAppStorage implements AppStorageManager
    {
        Vector deleted = new Vector();

        Vector stored = new Vector();

        Hashtable db = new Hashtable();

        public void deleteApi(String name, String version)
        {
            // empty
        }

        public void deleteApp(AppID id, long version)
        {
            AppKey app = new AppKey(id, version);
            deleted.addElement(app);
            db.remove(app);
        }

        public ApiStorage retrieveApi(String name, String version)
        {
            return null;
        }

        public ApiStorage[] retrieveApis()
        {
            return null;
        }

        public AppStorage retrieveApp(AppID id, long version, String className)
        {
            return null;
        }

        public String readAppBaseDir(AppID id, long version)
        {
            return null;
        }

        public boolean storeApi(String id, String version, int priority, AppDescriptionInfo info, File baseDir)
        {
            return false;
        }

        public boolean storeApp(AppID id, long version, int priority, AppDescriptionInfo info, TransportProtocol[] tp,
                String baseDir, boolean now)
        {
            AppKey key = new AppKey(id, version);

            assertNull("Expected null ADF", info);
            AppData app = new AppData(id, version, priority, tp, baseDir);
            stored.addElement(app);
            db.put(key, app);

            return !now; // only background stores are successful!
        }

        public void updatePrivilegedCertificates(byte[] privCertDescriptor)
        {
            // empty
        }

        public void destroy()
        {
            // empty
        }

        public boolean isPartiallyStored(AppID id, long version)
        {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean backgroundStoreApp(XAppEntry entry, TransportProtocol tp)
        {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean storeApp(XAppEntry entry, String[] fsMounts, boolean adfFromHashfiles)
            throws FileSysCommunicationException
        {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean updateStoragePriority(AppStorage app, int priority)
        {
            // TODO Auto-generated method stub
            return false;
        }

        public String getAppStorageDirectory()
        {
            // TODO Auto-generated method stub
            return null;
        }
    }

    /* ================= Boilerplate ================== */
    public ServicesDatabaseImplTest(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite(args));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(ServicesDatabaseImplTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new ServicesDatabaseImplTest(tests[i]));
            return suite;
        }
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(ServicesDatabaseImplTest.class);
        return suite;
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        csm = (CannedServiceMgr) CannedServiceMgr.getInstance();
        oldSM = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        ManagerManagerTest.updateManager(ServiceManager.class, CannedServiceMgr.class, true, csm);

        oldASM = (AppStorageManager) ManagerManager.getInstance(AppStorageManager.class);
        asm = new FakeAppStorage();
        ManagerManagerTest.updateManager(AppStorageManager.class, asm.getClass(), true, asm);
    }

    /**
     * Cleans up after the test
     */
    protected void tearDown() throws Exception
    {
        clearEvents();

        ManagerManagerTest.updateManager(ServiceManager.class, oldSM.getClass(), true, oldSM);
        oldSM = null;

        ManagerManagerTest.updateManager(AppStorageManager.class, oldASM.getClass(), true, oldASM);
        oldASM = null;
        asm = null;

        csm.destroy();
        csm = null;
        super.tearDown();
    }

    /**
     * Ensure that all events have been delivered relating to the previously
     * executed test. This was added because problems were encountered where
     * service details events on the cable transport were generated by a test
     * which didn't cary about such events, but weren't dispatched
     * (asynchronously) until later -- during another test which did care!
     * <p>
     * This method assumes that listeners were added on <i>current</i>
     * CallerContext and that all events are delivered using
     * {@link CallerContext#runInContext(Runnable)}.
     */
    protected void clearEvents() throws Exception
    {
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        CallerContext cc = ccm.getCurrentContext();

        class Wait implements Runnable
        {
            public synchronized void run()
            {
                // Once we execute, all previous runInContexts() have executed
                // as well!
                notifyAll();
            }
        }

        Wait wait = new Wait();
        synchronized (wait)
        {
            cc.runInContext(wait);
            wait.wait(15000);
        }
    }

    protected CannedServiceMgr csm;

    protected ServiceManager oldSM;

    protected FakeAppStorage asm;

    protected AppStorageManager oldASM;

    private static final int LISTENER_TIMEOUT = 5000;
}
