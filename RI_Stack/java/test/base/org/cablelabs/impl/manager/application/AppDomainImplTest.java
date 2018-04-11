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

package org.cablelabs.impl.manager.application;

import java.awt.Container;
import java.awt.Rectangle;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import javax.media.protocol.DataSource;
import javax.tv.locator.Locator;
import javax.tv.service.SIManager;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestor;
import javax.tv.service.Service;
import javax.tv.service.ServiceInformationType;
import javax.tv.service.ServiceType;
import javax.tv.service.guide.ProgramSchedule;
import javax.tv.service.navigation.DeliverySystemType;
import javax.tv.service.navigation.ServiceComponentChangeListener;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextListener;
import javax.tv.service.selection.ServiceMediaHandler;

import org.cablelabs.impl.manager.AppDomain;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.SignallingManager;
import org.cablelabs.impl.manager.service.ServiceCollection;
import org.cablelabs.impl.media.mpe.MediaAPI;
import org.cablelabs.impl.media.source.ServiceDataSource;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.service.SIDatabase;
import org.cablelabs.impl.service.ServiceContextExt;
import org.cablelabs.impl.service.ServicesDatabase;
import org.cablelabs.impl.service.javatv.selection.ServiceContextCallback;
import org.cablelabs.impl.signalling.AbstractServiceEntry;
import org.cablelabs.impl.signalling.Ait;
import org.cablelabs.impl.signalling.AitTest;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.signalling.SignallingEvent;
import org.cablelabs.impl.signalling.SignallingListener;
import org.cablelabs.impl.signalling.Xait;
import org.cablelabs.impl.signalling.XaitTest;
import org.cablelabs.test.iftc.InterfaceTestSuite;
import org.davic.net.InvalidLocatorException;
import org.dvb.application.AppAttributes;
import org.dvb.application.AppID;
import org.dvb.application.AppProxy;
import org.dvb.application.AppsDatabase;
import org.dvb.application.AppsDatabaseTest;
import org.dvb.application.CurrentServiceFilter;
import org.dvb.application.RunningApplicationsFilter;
import org.dvb.media.VideoTransformation;
import org.ocap.application.AppSignalHandler;
import org.ocap.application.OcapAppAttributes;
import org.ocap.net.OcapLocator;
import org.ocap.service.AbstractService;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests <code>AppDomainImpl</code>.
 * 
 * @author Aaron Kamienski
 */
public class AppDomainImplTest extends TestCase
{
    public void testConstructor()
    {
        appDomain = new AppDomainImpl(serviceContext, new CompositeAppsDB());
        // Not much else to do...
    }

    public void testGetServiceContext()
    {
        ServiceContext sc = appDomain.getServiceContext();
        assertNotNull("Should return a non-null ServiceContext", sc);
        assertSame("Should return same ServiceContext that it was created with", serviceContext, sc);
    }

    public void testGetDatabase()
    {
        AppsDatabase db = appDomain.getDatabase();

        assertNotNull("Should return a non-null AppsDatabase", db);

        assertEquals("Database should be empty for new AppDomain", 0, db.size());
        Enumeration e = db.getAppAttributes(new CurrentServiceFilter());
        assertNotNull("Database should return non-null Enumeration", e);
        assertFalse("Database should be empty for new AppDomain", e.hasMoreElements());
    }

    /* ========================AppsDatabase======================= */

    /* ======================== AppDomain ======================== */

    /**
     * Tests select(), given an AbstractService and no currently selected
     * service.
     */
    public void testSelect_none_abstract()
    {
        TestApp[] apps = {
                new XTestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 1), "App0", true,
                        new String[0], "/", "app0.Xlet", new String[0], 0),
                new XTestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 2), "App1", true,
                        new String[0], "/", "app1.Xlet", new String[0], 0), };
        TestAbstractService service = new TestAbstractService(0x123456, "FLCL", false, apps);

        // Install a custom ServicesDatabase
        // Including the given service (plus apps)
        svcMgr.db.db.put(new Integer(service.id), service);
        assertEquals("Internal failure - ServiceDatabase doesn't have right entry!", service,
                svcMgr.db.getServiceEntry(service.id));
        svcMgr.db.getServiceEntryServiceId = -1;

        appDomain.select(service.details, null);

        // Should acquire information from ServicesDatabase
        assertEquals("Expected getServiceEntry to have been called", service.id, svcMgr.db.getServiceEntryServiceId);

        // Should have installed listener with ServicesDatabase
        assertEquals("Should've listener for serviceId", service.id, svcMgr.db.serviceId);
        assertSame("Should've installed self as listener for serviceId", appDomain, svcMgr.db.listener);

        // Check appsDatabase
        AppsDatabase appsDb = appDomain.getDatabase();

        assertEquals("Unexpected number of initial apps", apps.length, appsDb.size());
        for (int i = 0; i < apps.length; ++i)
        {
            AppAttributes app = appsDb.getAppAttributes(apps[i].id);

            assertNotNull("Expected app " + apps[i].id + " to be in database", app);
        }
    }

    /**
     * Tests select(), given a broadcast Service and no currently selected
     * service.
     */
    public void testSelect_none_broadcast()
    {
        TestApp[] apps = {
                new TestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 20), "App0", true,
                        new String[0], "/", "app0.Xlet", new String[0]),
                new TestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 21), "App1", true,
                        new String[0], "/", "app1.Xlet", new String[0]), };
        TestService service = new TestService(0x1234, "FLCL");

        appDomain.select(service.details, null);

        // Should have installed listener with SignallingManager
        assertEquals("Should've listener for serviceId", service.id, sigMgr.serviceId);
        assertSame("Should've installed self as listener for serviceId", appDomain, sigMgr.listener);

        // Send initial information
        sigMgr.listener.signallingReceived(new SignallingEvent(this, new TestAit(apps)));
        // Check appsDatabase
        AppsDatabase appsDb = appDomain.getDatabase();

        assertEquals("Unexpected number of initial apps", apps.length, appsDb.size());
        for (int i = 0; i < apps.length; ++i)
        {
            AppAttributes app = appsDb.getAppAttributes(apps[i].id);

            assertNotNull("Expected app " + apps[i].id + " to be in database", app);
        }
    }

    /**
     * Tests select(), given a unknown Service type.
     */
    public void testSelect_other()
    {
        // TODO(AaronK): select a broadcast or abstract service into it first...

        TestApp[] apps = {
                new TestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 20), "App0", true,
                        new String[0], "/", "app0.Xlet", new String[0]),
                new TestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 21), "App1", true,
                        new String[0], "/", "app1.Xlet", new String[0]), };
        TestService service = new TestService(0x1234, "FLCL", apps)
        {
            public Locator getLocator()
            {
                try
                {
                    return new OcapLocator(id)
                    {
                    };
                }
                catch (InvalidLocatorException e)
                {
                    fail("Internal error" + e.getMessage());
                    return null;
                }
            }
        };

        appDomain.select(service.details, null);

        // Should have installed listener with SignallingManager
        assertEquals("Should NOT have listener for serviceId", -1, sigMgr.serviceId);
        assertSame("Should NOT have installed self as listener for serviceId", null, sigMgr.listener);

        // Check appsDatabase
        AppsDatabase appsDb = appDomain.getDatabase();

        assertEquals("Unexpected number of initial apps", 0, appsDb.size());
    }

    /**
     * Tests stop().
     * <p>
     * This test doesn't work because:
     * <ul>
     * <li>stopApps(AppID, boolean) isn't called in this case
     * <li>stop() only calls stop() for running apps
     * </ul>
     */
    public void X_testStop()
    {
        // Let's add apps for selection
        TestApp[] apps = {
                new XTestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 30), "App0", true,
                        new String[0], "/", "app0.Xlet", new String[0], 0),
                new XTestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 31), "App1", true,
                        new String[0], "/", "app1.Xlet", new String[0], 0), };
        TestAbstractService service = new TestAbstractService(0x123456, "FLCL", false, apps);

        // Install a custom ServicesDatabase
        // Including the given service (plus apps)
        svcMgr.db.db.put(new Integer(service.id), service);
        assertEquals("Internal failure - ServiceDatabase doesn't have right entry!", service,
                svcMgr.db.getServiceEntry(service.id));
        svcMgr.db.getServiceEntryServiceId = -1;

        // Select the service
        appDomain.select(service.details, null);

        // Should acquire information from ServicesDatabase
        assertEquals("Expected getServiceEntry to have been called", service.id, svcMgr.db.getServiceEntryServiceId);

        // Should have installed listener with ServicesDatabase
        assertEquals("Should've listener for serviceId", service.id, svcMgr.db.serviceId);
        assertSame("Should've installed self as listener for serviceId", appDomain, svcMgr.db.listener);

        // Call stop
        TestAppDomain test = (TestAppDomain) appDomain;
        test.clear();
        appDomain.stop();

        // Make sure above apps were destroyed
        assertEquals("Unexpected number of apps started", 0, test.started.size());
        assertEquals("Unexpected number of apps killed", apps.length, test.killed.size());
        assertEquals("Unexpected number of apps destroyed", 0, test.destroyed.size());

        for (int i = 0; i < apps.length; ++i)
            test.killed.removeElement(apps[i].id);
        // Remove each expected app from list
        assertEquals("Unexpected apps were killed", 0, test.killed.size());
    }

    /**
     * Tests stop (at unexpected time). Expect it to do nothing.
     * <p>
     * Unfortunately, this test doesn't work. Because the handleStop is done on
     * another thread, throwing an exception there won't be caught by the test.
     * And w/out a sleep() in this test, we might finish (and System.exit())
     * before it gets output!
     */
    public void X_testStop_unexpected() throws Exception
    {
        // Unexpected in UNSELECTED state (so at creation)

        TestAppDomain test = (TestAppDomain) appDomain;

        test.stop();

        // Expect no exceptions - even in other threads...
        Thread.sleep(3000);
    }

    /**
     * Tests destroy().
     * <p>
     * This test doesn't work because:
     * <ul>
     * <li>stopApps(AppID, boolean) isn't called in this case
     * <li>destroy() only calls stop() for running apps
     * </ul>
     */
    public void X_testDestroy()
    {
        // Let's add apps for selection
        TestApp[] apps = {
                new XTestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 40), "App0", true,
                        new String[0], "/", "app0.Xlet", new String[0], 0),
                new XTestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 41), "App1", true,
                        new String[0], "/", "app1.Xlet", new String[0], 0), };
        TestAbstractService service = new TestAbstractService(0x123456, "FLCL", false, apps);

        // Install a custom ServicesDatabase
        // Including the given service (plus apps)
        svcMgr.db.db.put(new Integer(service.id), service);
        assertEquals("Internal failure - ServiceDatabase doesn't have right entry!", service,
                svcMgr.db.getServiceEntry(service.id));
        svcMgr.db.getServiceEntryServiceId = -1;

        // Select the service
        appDomain.select(service.details, null);

        // Should acquire information from ServicesDatabase
        assertEquals("Expected getServiceEntry to have been called", service.id, svcMgr.db.getServiceEntryServiceId);

        // Should have installed listener with ServicesDatabase
        assertEquals("Should've listener for serviceId", service.id, svcMgr.db.serviceId);
        assertSame("Should've installed self as listener for serviceId", appDomain, svcMgr.db.listener);

        // Call destroy
        TestAppDomain test = (TestAppDomain) appDomain;
        test.clear();
        appDomain.destroy();

        // Make sure above apps were destroyed
        assertEquals("Unexpected number of apps started", 0, test.started.size());
        assertEquals("Unexpected number of apps killed", apps.length, test.killed.size());
        assertEquals("Unexpected number of apps destroyed", 0, test.destroyed.size());

        for (int i = 0; i < apps.length; ++i)
            test.killed.removeElement(apps[i].id);
        // Remove each expected app from list
        assertEquals("Unexpected apps were killed", 0, test.killed.size());
    }

    /**
     * Tests stopBoundApps().
     * <p>
     * This test doesn't work because:
     * <ul>
     * <li>stopApps(AppID, boolean) isn't called in this case
     * <li>stopBoundApps() only calls stop() for running apps
     * </ul>
     */
    public void X_testStopBoundApps()
    {
        TestApp[] apps = {
                new TestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 50), "App0", true,
                        new String[0], "/", "app0.Xlet", new String[0]),
                new TestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 51), "App1", false,
                        new String[0], "/", "app1.Xlet", new String[0]),
                new TestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 52), "App2", true,
                        new String[0], "/", "app1.Xlet", new String[0]), };
        TestService service = new TestService(0x2345, "wxyz");

        appDomain.select(service.details, null);

        // Send initial information
        sigMgr.listener.signallingReceived(new SignallingEvent(this, new TestAit(apps)));

        // Call stopBoundApps()
        TestAppDomain test = (TestAppDomain) appDomain;
        test.clear();
        appDomain.stopBoundApps();

        // Check number of killed apps
        assertEquals("Unexpected number of apps started", 0, test.started.size());
        assertEquals("Unexpected number of apps killed", 2, test.killed.size());
        assertEquals("Unexpected number of apps destroyed", 0, test.destroyed.size());

        // Check actual killed apps
        assertTrue("Expected app " + apps[0].id + " to be killed", test.killed.removeElement(apps[0].id));
        assertTrue("Expected app " + apps[2].id + " to be killed", test.killed.removeElement(apps[2].id));
    }

    /**
     * Tests preSelect().
     * <ul>
     * <li>Pre-select service. Ensure that AppsDatabase is filled in, but apps
     * not started.
     * <li>Select service. Ensure that apps are started.
     * </ul>
     */
    public void testPreSelect() throws Exception
    {
        TestApp[] apps = {
                new XTestApp(OcapAppAttributes.AUTOSTART, new AppID(TestAppIDs.APPDOMAINIMPL, 61), "App0", true,
                        new String[0], "/", "app0.Xlet", new String[0], 0)
                {
                    {
                        priority = 255;
                    }
                },
                new XTestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 62), "App1", true,
                        new String[0], "/", "app1.Xlet", new String[0], 0), };
        TestAbstractService service = new TestAbstractService(0x123456, "FLCL", false, apps);

        // Install a custom ServicesDatabase
        // Including the given service (plus apps)
        svcMgr.db.db.put(new Integer(service.id), service);
        assertEquals("Internal failure - ServiceDatabase doesn't have right entry!", service,
                svcMgr.db.getServiceEntry(service.id));
        svcMgr.db.getServiceEntryServiceId = -1;

        // Perform preSelect
        appDomain.preSelect(service.details);

        // Should acquire information from ServicesDatabase
        assertEquals("Expected getServiceEntry to have been called", service.id, svcMgr.db.getServiceEntryServiceId);

        // Should NOT have installed listener with ServicesDatabase
        assertEquals("Should've listener for serviceId", -1, svcMgr.db.serviceId);
        assertSame("Should've installed self as listener for serviceId", null, svcMgr.db.listener);

        // Check number of kill/destroy
        TestAppDomain test = (TestAppDomain) appDomain;
        assertEquals("Unexpected number of apps started", 0, test.started.size());
        assertEquals("Unexpected number of apps killed", 0, test.killed.size());
        assertEquals("Unexpected number of apps destroyed", 0, test.destroyed.size());

        assertFalse("Expected app " + apps[0].id + " to NOT be autostarted", test.started.removeElement(apps[0]));

        // Check appsDatabase
        AppsDatabase appsDb = appDomain.getDatabase();
        assertEquals("Unexpected number of initial apps", apps.length, appsDb.size());
        // No apps should be running (yet)
        Thread.sleep(1000);
        for (int i = 0; i < apps.length; ++i)
        {
            AppAttributes app = appsDb.getAppAttributes(apps[i].id);

            assertNotNull("Expected app " + apps[i].id + " to be in database", app);
            AppProxy proxy = appsDb.getAppProxy(apps[i].id);
            assertEquals("No apps should be running yet", AppProxy.NOT_LOADED, proxy.getState());
        }
        Enumeration e = appsDb.getAppIDs(new RunningApplicationsFilter());
        assertFalse("Expected no applications to be running!", e.hasMoreElements());

        appDomain.select(service.details, null);

        // Should acquire information from ServicesDatabase
        assertEquals("Expected getServiceEntry to have been called", service.id, svcMgr.db.getServiceEntryServiceId);

        // Should have installed listener with ServicesDatabase
        assertEquals("Should've listener for serviceId", service.id, svcMgr.db.serviceId);
        assertSame("Should've installed self as listener for serviceId", appDomain, svcMgr.db.listener);

        // Check appsDatabase
        appsDb = appDomain.getDatabase();

        assertEquals("Unexpected number of initial apps", apps.length, appsDb.size());
        for (int i = 0; i < apps.length; ++i)
        {
            AppAttributes app = appsDb.getAppAttributes(apps[i].id);

            assertNotNull("Expected app " + apps[i].id + " to be in database", app);
        }

        // Check number of kill/destroy
        test = (TestAppDomain) appDomain;
        assertEquals("Unexpected number of apps started", 1, test.started.size());
        assertEquals("Unexpected number of apps killed", 0, test.killed.size());
        assertEquals("Unexpected number of apps destroyed", 0, test.destroyed.size());

        assertTrue("Expected app " + apps[0].id + " to be autostarted", test.started.removeElement(apps[0]));
    }

    /**
     * Tests preSelect().
     */
    public void testPreSelect_broadcast()
    {
        TestService nonAbstract = new TestService(1, "nonAbstract");
        try
        {
            appDomain.preSelect(nonAbstract.details);
            fail("Expected IllegalStateException when preSelecting non-Abstract service");
        }
        catch (IllegalStateException e)
        {
            // Expected
        }
    }

    /**
     * Tests preSelect().
     * <ul>
     * <li>Get to SELECTED state.
     * <li>Call preSelect, should fail.
     * <li>Stop BOUND apps.
     * <li>Call preSelect, should fail.
     * <li>Destroy.
     * <li>Call preSelect, should fail.
     * </ul>
     */
    public void testPreSelect_badState()
    {
        TestApp[] apps = {
                new XTestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 71), "App0", true,
                        new String[0], "/", "app0.Xlet", new String[0], 0),
                new XTestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 72), "App1", true,
                        new String[0], "/", "app1.Xlet", new String[0], 0), };
        TestAbstractService service = new TestAbstractService(0x123456, "FLCL", false, apps);

        // Install a custom ServicesDatabase
        // Including the given service (plus apps)
        svcMgr.db.db.put(new Integer(service.id), service);
        assertEquals("Internal failure - ServiceDatabase doesn't have right entry!", service,
                svcMgr.db.getServiceEntry(service.id));
        svcMgr.db.getServiceEntryServiceId = -1;

        // Preselect from SELECTED
        appDomain.select(service.details, null);
        try
        {
            appDomain.preSelect(service.details);
            fail("Expected IllegalStateException on preSelect when SELECTED");
        }
        catch (IllegalStateException e)
        { /* ignore */
        }

        // Preselect from STOP_BOUND
        appDomain.stopBoundApps();
        try
        {
            appDomain.preSelect(service.details);
            fail("Expected IllegalStateException on preSelect when SEMI_SELECTED");
        }
        catch (IllegalStateException e)
        { /* ignore */
        }

        // PreSelect from DESTROYED
        appDomain.destroy();
        try
        {
            appDomain.preSelect(service.details);
            fail("Expected IllegalStateException on preSelect when DESTROYED");
        }
        catch (IllegalStateException e)
        { /* ignore */
        }
    }

    /**
     * Tests getServiceContentHandlers().
     */
    public void testGetServiceContentHandlers_unselected()
    {
        // Expect AppDomain to be unselected here...

        ServiceContentHandler[] sch = appDomain.getServiceContentHandlers();
        assertNotNull("Expected non-null SCH[]", sch);
        assertEquals("Expected empty SCH[]", 0, sch.length);
    }

    /**
     * Tests getServiceContentHandlers().
     */
    public void XTestGetServiceContentHandlers_selected()
    {
        // TODO: implement test
        fail("Unimplemented test");

        // How can this be done?
        // Need to have apps that are running and not running.
    }

    /**
     * Tests AIT-update via <code>signallingReceived</code>.
     * <ul>
     * <li>Added entries
     * <li>Removed entries
     * </ul>
     */
    public void testSignallingReceived()
    {
        TestApp[] apps = {
                new TestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 60), "App0", true,
                        new String[0], "/", "app0.Xlet", new String[0]),
                new TestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 61), "App1", true,
                        new String[0], "/", "app1.Xlet", new String[0]), };
        TestService service = new TestService(0x2345, "wxyz");

        appDomain.select(service.details, null);

        // Send initial information
        sigMgr.listener.signallingReceived(new SignallingEvent(this, new TestAit(apps)));

        AppsDatabase appsDb = appDomain.getDatabase();

        assertEquals("Unexpected number of initial apps", apps.length, appsDb.size());
        for (int i = 0; i < apps.length; ++i)
        {
            AppAttributes app = appsDb.getAppAttributes(apps[i].id);

            assertNotNull("Expected app " + apps[i].id + " to be in database", app);
        }

        // Send new information
        // Remove one app, add another
        TestApp removed = apps[0];
        apps[0] = new TestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 62), "App2", true,
                new String[0], "/", "app1.Xlet", new String[0]);
        TestAppDomain test = (TestAppDomain) appDomain;
        test.clear();
        sigMgr.listener.signallingReceived(new SignallingEvent(this, new TestAit(apps)));

        assertEquals("Unexpected number of apps", apps.length, appsDb.size());
        for (int i = 0; i < apps.length; ++i)
        {
            AppAttributes app = appsDb.getAppAttributes(apps[i].id);

            assertNotNull("Expected app " + apps[i].id + " to be in database", app);
        }
        assertTrue("Expected app " + removed.id + " to be removed from database",
                null == appsDb.getAppAttributes(removed.id));

        // Make sure that removed app was "killed"
        assertEquals("Unexpected number of apps started", 0, test.started.size());
        assertEquals("Unexpected number of apps killed", 1, test.killed.size());
        assertEquals("Unexpected number of apps destroyed", 0, test.destroyed.size());

        assertEquals("Expected app " + removed.id + " to be killed", removed.id, test.killed.elementAt(0));
    }

    /**
     * Tests AIT-update via <code>signallingReceived</code>.
     * <ul>
     * <li>AUTOSTART
     * </ul>
     */
    public void testSignallingReceived_autostart() throws Exception
    {
        TestApp[] apps = {
                new TestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 70), "App0", true,
                        new String[0], "/", "app0.Xlet", new String[0]),
                new TestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 71), "App1", true,
                        new String[0], "/", "app1.Xlet", new String[0]), };
        TestService service = new TestService(0x2345, "wxyz");

        appDomain.select(service.details, null);

        // Send initial information
        sigMgr.listener.signallingReceived(new SignallingEvent(this, new TestAit(apps)));

        AppsDatabase appsDb = appDomain.getDatabase();

        assertEquals("Unexpected number of initial apps", apps.length, appsDb.size());
        for (int i = 0; i < apps.length; ++i)
        {
            AppAttributes app = appsDb.getAppAttributes(apps[i].id);

            assertNotNull("Expected app " + apps[i].id + " to be in database", app);
        }

        // Newly autostart...
        apps[1] = (TestApp) apps[1].clone();
        apps[1].controlCode = OcapAppAttributes.AUTOSTART;
        TestAppDomain test = (TestAppDomain) appDomain;
        test.clear();
        sigMgr.listener.signallingReceived(new SignallingEvent(this, new TestAit(apps)));

        // Check number of autostart apps
        assertEquals("Unexpected number of apps started", 1, test.started.size());
        assertEquals("Unexpected number of apps killed", 0, test.killed.size());
        assertEquals("Unexpected number of apps destroyed", 0, test.destroyed.size());

        assertEquals("Expected app " + apps[1].id + " to be autostarted", apps[1].id,
                ((TestApp) test.started.elementAt(0)).id);
        assertTrue("Expected app " + apps[1].id + " to be autostarted", test.started.removeElement(apps[1]));
    }

    /**
     * Tests AIT-update via <code>signallingReceived</code>.
     * <ul>
     * <li>KILL
     * <li>DESTROY
     * </ul>
     */
    public void testSignallingReceived_stop() throws Exception
    {
        TestApp[] apps = {
                new TestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 80), "App0", true,
                        new String[0], "/", "app0.Xlet", new String[0]),
                new TestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 81), "App1", true,
                        new String[0], "/", "app1.Xlet", new String[0]), };
        TestService service = new TestService(0x2345, "wxyz");

        appDomain.select(service.details, null);

        // Send initial information
        sigMgr.listener.signallingReceived(new SignallingEvent(this, new TestAit(apps)));

        AppsDatabase appsDb = appDomain.getDatabase();

        assertEquals("Unexpected number of initial apps", apps.length, appsDb.size());
        for (int i = 0; i < apps.length; ++i)
        {
            AppAttributes app = appsDb.getAppAttributes(apps[i].id);

            assertNotNull("Expected app " + apps[i].id + " to be in database", app);
        }

        // Newly KILL/DESTROY
        apps[0] = (TestApp) apps[0].clone();
        apps[0].controlCode = OcapAppAttributes.DESTROY;
        apps[1] = (TestApp) apps[1].clone();
        apps[1].controlCode = OcapAppAttributes.KILL;
        TestAppDomain test = (TestAppDomain) appDomain;
        test.clear();
        sigMgr.listener.signallingReceived(new SignallingEvent(this, new TestAit(apps)));

        // Check number of kill/destroy
        assertEquals("Unexpected number of apps started", 0, test.started.size());
        assertEquals("Unexpected number of apps killed", 1, test.killed.size());
        assertEquals("Unexpected number of apps destroyed", 1, test.destroyed.size());

        assertTrue("Expected app " + apps[0].id + " to be destroyed", test.destroyed.removeElement(apps[0].id));
        assertTrue("Expected app " + apps[1].id + " to be killed", test.killed.removeElement(apps[1].id));
    }

    /**
     * Tests AIT-update via <code>signallingReceived</code>. Deleted apps should
     * be stopped.
     */
    public void testSignallingReceived_delete() throws Exception
    {
        TestApp[] apps = {
                new TestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 80), "App0", true,
                        new String[0], "/", "app0.Xlet", new String[0]),
                new TestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 81), "App1", true,
                        new String[0], "/", "app1.Xlet", new String[0]), };
        TestService service = new TestService(0x2345, "wxyz");

        appDomain.select(service.details, null);

        // Send initial information
        sigMgr.listener.signallingReceived(new SignallingEvent(this, new TestAit(apps)));

        AppsDatabase appsDb = appDomain.getDatabase();

        assertEquals("Unexpected number of initial apps", apps.length, appsDb.size());
        for (int i = 0; i < apps.length; ++i)
        {
            AppAttributes app = appsDb.getAppAttributes(apps[i].id);

            assertNotNull("Expected app " + apps[i].id + " to be in database", app);
        }

        // Newly KILL/DESTROY
        TestApp[] apps2 = { apps[1] };
        TestAppDomain test = (TestAppDomain) appDomain;
        test.clear();
        sigMgr.listener.signallingReceived(new SignallingEvent(this, new TestAit(apps2)));

        // Check number of kill/destroy
        assertEquals("Unexpected number of apps started", 0, test.started.size());
        assertEquals("Unexpected number of apps killed", 1, test.killed.size());
        assertEquals("Unexpected number of apps destroyed", 0, test.destroyed.size());

        assertTrue("Expected app " + apps[0].id + " to be destroyed", test.killed.removeElement(apps[0].id));
    }

    /**
     * Tests AIT-update via <code>signallingReceived</code>. Deleted apps should
     * be stopped, unless there's external authorization...
     */
    public void testSignallingReceived_delete_extAuth() throws Exception
    {
        TestApp[] apps = {
                new TestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 80), "App0", true,
                        new String[0], "/", "app0.Xlet", new String[0]),
                new TestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 81), "App1", true,
                        new String[0], "/", "app1.Xlet", new String[0]), };
        TestService service = new TestService(0x2345, "wxyz");

        appDomain.select(service.details, null);

        // Send initial information
        sigMgr.listener.signallingReceived(new SignallingEvent(this, new TestAit(apps)));

        AppsDatabase appsDb = appDomain.getDatabase();

        assertEquals("Unexpected number of initial apps", apps.length, appsDb.size());
        for (int i = 0; i < apps.length; ++i)
        {
            AppAttributes app = appsDb.getAppAttributes(apps[i].id);

            assertNotNull("Expected app " + apps[i].id + " to be in database", app);
        }

        // Newly KILL/DESTROY
        TestApp[] apps2 = { apps[1] };
        TestAppDomain test = (TestAppDomain) appDomain;
        test.clear();
        // Signal deleted app, but externally authorized...
        sigMgr.listener.signallingReceived(new SignallingEvent(this, new TestAit(apps2, apps[0].id)));

        // Check number of kill/destroy
        assertEquals("Unexpected number of apps started", 0, test.started.size());
        assertEquals("Unexpected number of apps killed", 0, test.killed.size());
        assertEquals("Unexpected number of apps destroyed", 0, test.destroyed.size());
    }

    /**
     * Test signalling of duplicate instances of same app. New high priority
     * entry indicates AUTOSTART.
     */
    public void testSignallingReceived_autostart_HighPriority() throws Exception
    {
        TestApp[] apps = {
                new TestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 70), "App0", true,
                        new String[0], "/", "app0.Xlet", new String[0]),
                new TestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 71), "App1", true,
                        new String[0], "/", "app1.Xlet", new String[0]), };
        TestService service = new TestService(0x2345, "wxyz");

        appDomain.select(service.details, null);

        // Send initial information
        sigMgr.listener.signallingReceived(new SignallingEvent(this, new TestAit(apps)));

        AppsDatabase appsDb = appDomain.getDatabase();

        assertEquals("Unexpected number of initial apps", apps.length, appsDb.size());
        for (int i = 0; i < apps.length; ++i)
        {
            AppAttributes app = appsDb.getAppAttributes(apps[i].id);

            assertNotNull("Expected app " + apps[i].id + " to be in database", app);
        }

        // Newly autostart...
        TestApp[] apps2 = { (TestApp) apps[1].clone(), apps[1], apps[0], (TestApp) apps[0].clone(), };
        apps2[0].controlCode = OcapAppAttributes.AUTOSTART;
        apps2[0].priority++;
        apps2[3].controlCode = OcapAppAttributes.AUTOSTART;
        apps2[3].priority++;
        TestAppDomain test = (TestAppDomain) appDomain;
        test.clear();
        sigMgr.listener.signallingReceived(new SignallingEvent(this, new TestAit(apps2)));

        // Check number of autostart apps
        assertEquals("Unexpected number of apps started", 2, test.started.size());
        assertEquals("Unexpected number of apps killed", 0, test.killed.size());
        assertEquals("Unexpected number of apps destroyed", 0, test.destroyed.size());

        assertTrue("Expected app " + apps2[0].id + " to be autostarted", test.started.removeElement(apps2[0]));
        assertTrue("Expected app " + apps2[3].id + " to be autostarted", test.started.removeElement(apps2[3]));
    }

    /**
     * Tests XAIT-update via <code>serviceUpdate</code>.
     * <ul>
     * <li>Added entries
     * <li>Removed entries
     * </ul>
     */
    public void testServiceUpdate()
    {
        XTestApp[] apps = {
                new XTestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 90), "App0", true,
                        new String[0], "/", "app0.Xlet", new String[0], 0),
                new XTestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 91), "App1", true,
                        new String[0], "/", "app1.Xlet", new String[0], 0), };
        TestAbstractService service = new TestAbstractService(0x23456, "wxyz", false, apps);

        // Install a custom ServicesDatabase
        // Including the given service (plus apps)
        svcMgr.db.db.put(new Integer(service.id), service);
        assertEquals("Internal failure - ServiceDatabase doesn't have right entry!", service,
                svcMgr.db.getServiceEntry(service.id));
        svcMgr.db.getServiceEntryServiceId = -1;

        appDomain.select(service.details, null);

        // Should acquire information from ServicesDatabase
        assertEquals("Expected getServiceEntry to have been called", service.id, svcMgr.db.getServiceEntryServiceId);

        // Should have installed listener with ServicesDatabase
        assertEquals("Should've listener for serviceId", service.id, svcMgr.db.serviceId);
        assertSame("Should've installed self as listener for serviceId", appDomain, svcMgr.db.listener);

        // Check appsDatabase
        AppsDatabase appsDb = appDomain.getDatabase();

        assertEquals("Unexpected number of initial apps", apps.length, appsDb.size());
        for (int i = 0; i < apps.length; ++i)
        {
            AppAttributes app = appsDb.getAppAttributes(apps[i].id);

            assertNotNull("Expected app " + apps[i].id + " to be in database", app);
        }

        // Update service's apps
        TestApp removed = apps[1];
        service.apps.removeElement(removed);
        apps[1] = new XTestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 92), "App2", true,
                new String[0], "/", "app2.Xlet", new String[0], 0);
        service.apps.addElement(apps[1]);
        apps[1].service = service;
        //apps[1].serviceId = service.id;
        TestAppDomain test = (TestAppDomain) appDomain;
        test.clear();

        // Send service update
        svcMgr.db.listener.serviceUpdate(service);

        // Check appsDatabase
        assertEquals("Unexpected number of apps", apps.length, appsDb.size());
        for (int i = 0; i < apps.length; ++i)
        {
            AppAttributes app = appsDb.getAppAttributes(apps[i].id);

            assertNotNull("Expected app " + apps[i].id + " to be in database", app);
        }
        assertTrue("Expected app " + removed.id + " to be removed from database",
                null == appsDb.getAppAttributes(removed.id));

        // Make sure that removed app was "killed"
        assertEquals("Unexpected number of apps started", 0, test.started.size());
        assertEquals("Unexpected number of apps killed", 1, test.killed.size());
        assertEquals("Unexpected number of apps destroyed", 0, test.destroyed.size());

        assertEquals("Expected app " + removed.id + " to be killed", removed.id, test.killed.elementAt(0));
    }

    /**
     * Tests XAIT-update via <code>serviceUpdate</code>.
     * <ul>
     * <li>AUTOSTART
     * </ul>
     */
    public void testServiceUpdate_autostart() throws Exception
    {
        XTestApp[] apps = {
                new XTestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 100), "App0", true,
                        new String[0], "/", "app0.Xlet", new String[0], 0),
                new XTestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 101), "App1", true,
                        new String[0], "/", "app1.Xlet", new String[0], 0), };
        TestAbstractService service = new TestAbstractService(0x23456, "wxyz", false, apps);

        // Install a custom ServicesDatabase
        // Including the given service (plus apps)
        svcMgr.db.db.put(new Integer(service.id), service);
        assertEquals("Internal failure - ServiceDatabase doesn't have right entry!", service,
                svcMgr.db.getServiceEntry(service.id));
        svcMgr.db.getServiceEntryServiceId = -1;

        appDomain.select(service.details, null);

        // Should acquire information from ServicesDatabase
        assertEquals("Expected getServiceEntry to have been called", service.id, svcMgr.db.getServiceEntryServiceId);

        // Should have installed listener with ServicesDatabase
        assertEquals("Should've listener for serviceId", service.id, svcMgr.db.serviceId);
        assertSame("Should've installed self as listener for serviceId", appDomain, svcMgr.db.listener);

        // Update service's apps
        TestAppDomain test = (TestAppDomain) appDomain;
        test.clear();
        service.apps.removeElement(apps[1]);
        apps[1] = (XTestApp) apps[1].clone();
        apps[1].controlCode = OcapAppAttributes.AUTOSTART;
        service.apps.addElement(apps[1]);
        svcMgr.db.listener.serviceUpdate(service);

        // Check number of kill/destroy
        assertEquals("Unexpected number of apps started", 1, test.started.size());
        assertEquals("Unexpected number of apps killed", 0, test.killed.size());
        assertEquals("Unexpected number of apps destroyed", 0, test.destroyed.size());

        assertTrue("Expected app " + apps[1].id + " to be autostarted", test.started.removeElement(apps[1]));
    }

    /**
     * Tests XAIT-update via <code>serviceUpdate</code>.
     * <ul>
     * <li>DESTROY
     * <li>KILL
     * </ul>
     */
    public void testServiceUpdate_stop() throws Exception
    {
        XTestApp[] apps = {
                new XTestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 110), "App0", true,
                        new String[0], "/", "app0.Xlet", new String[0], 0),
                new XTestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 111), "App1", true,
                        new String[0], "/", "app1.Xlet", new String[0], 0),
                new XTestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 112), "App2", true,
                        new String[0], "/", "app2.Xlet", new String[0], 0), };
        TestAbstractService service = new TestAbstractService(0x23456, "wxyz", false, apps);

        // Install a custom ServicesDatabase
        // Including the given service (plus apps)
        svcMgr.db.db.put(new Integer(service.id), service);
        assertEquals("Internal failure - ServiceDatabase doesn't have right entry!", service,
                svcMgr.db.getServiceEntry(service.id));
        svcMgr.db.getServiceEntryServiceId = -1;

        appDomain.select(service.details, null);

        // Should acquire information from ServicesDatabase
        assertEquals("Expected getServiceEntry to have been called", service.id, svcMgr.db.getServiceEntryServiceId);

        // Should have installed listener with ServicesDatabase
        assertEquals("Should've listener for serviceId", service.id, svcMgr.db.serviceId);
        assertSame("Should've installed self as listener for serviceId", appDomain, svcMgr.db.listener);

        // Update service's apps
        TestAppDomain test = (TestAppDomain) appDomain;
        test.clear();
        service.apps.removeElement(apps[1]);
        service.apps.removeElement(apps[0]);
        apps[0] = (XTestApp) apps[0].clone();
        apps[0].controlCode = OcapAppAttributes.DESTROY;
        service.apps.addElement(apps[0]);
        apps[1] = (XTestApp) apps[1].clone();
        apps[1].controlCode = OcapAppAttributes.KILL;
        service.apps.addElement(apps[1]);
        svcMgr.db.listener.serviceUpdate(service);

        // Check number of kill/destroy
        assertEquals("Unexpected number of apps started", 0, test.started.size());
        assertEquals("Unexpected number of apps killed", 1, test.killed.size());
        assertEquals("Unexpected number of apps destroyed", 1, test.destroyed.size());

        assertTrue("Expected app " + apps[0].id + " to be destroyed", test.destroyed.removeElement(apps[0].id));
        assertTrue("Expected app " + apps[1].id + " to be killed", test.killed.removeElement(apps[1].id));
    }

    /**
     * Tests XAIT-update via <code>serviceUpdate</code>.
     * <ul>
     * <li>DESTROY
     * <li>KILL
     * </ul>
     */
    public void testServiceUpdate_stopAbstractService() throws Exception
    {
        XTestApp[] apps = {
                new XTestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 110), "App0", true,
                        new String[0], "/", "app0.Xlet", new String[0], 0),
                new XTestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 111), "App1", true,
                        new String[0], "/", "app1.Xlet", new String[0], 0), };
        TestAbstractService service = new TestAbstractService(0x23456, "wxyz", false, apps);

        // Install a custom ServicesDatabase
        // Including the given service (plus apps)
        svcMgr.db.db.put(new Integer(service.id), service);
        assertEquals("Internal failure - ServiceDatabase doesn't have right entry!", service,
                svcMgr.db.getServiceEntry(service.id));
        svcMgr.db.getServiceEntryServiceId = -1;

        appDomain.select(service.details, null);

        // Should acquire information from ServicesDatabase
        assertEquals("Expected getServiceEntry to have been called", service.id, svcMgr.db.getServiceEntryServiceId);

        // Should have installed listener with ServicesDatabase
        assertEquals("Should've listener for serviceId", service.id, svcMgr.db.serviceId);
        assertSame("Should've installed self as listener for serviceId", appDomain, svcMgr.db.listener);

        // Update service's apps

        TestAppDomain test = (TestAppDomain) appDomain;
        test.clear();
        service.apps.removeElement(apps[1]);
        service.apps.removeElement(apps[0]);
        apps[0] = (XTestApp) apps[0].clone();
        apps[0].controlCode = OcapAppAttributes.DESTROY;
        service.apps.addElement(apps[0]);
        apps[1] = (XTestApp) apps[1].clone();
        apps[1].controlCode = OcapAppAttributes.KILL;
        service.apps.addElement(apps[1]);

        TestServiceContext tsc = (TestServiceContext) appDomain.getServiceContext();
        tsc.setAbstractServiceFlagFalse();

        svcMgr.db.listener.serviceUpdate(service);

        assertTrue("Expected stopAbstractService called", tsc.getAbstractServiceFlag());
    }

    /**
     * Tests XAIT-update via <code>serviceUpdate</code>. Deleted app should be
     * stopped.
     */
    public void testServiceUpdate_delete() throws Exception
    {
        XTestApp[] apps = {
                new XTestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 110), "App0", true,
                        new String[0], "/", "app0.Xlet", new String[0], 0),
                new XTestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 111), "App1", true,
                        new String[0], "/", "app1.Xlet", new String[0], 0), };
        TestAbstractService service = new TestAbstractService(0x23456, "wxyz", false, apps);

        // Install a custom ServicesDatabase
        // Including the given service (plus apps)
        svcMgr.db.db.put(new Integer(service.id), service);
        assertEquals("Internal failure - ServiceDatabase doesn't have right entry!", service,
                svcMgr.db.getServiceEntry(service.id));
        svcMgr.db.getServiceEntryServiceId = -1;

        appDomain.select(service.details, null);

        // Should acquire information from ServicesDatabase
        assertEquals("Expected getServiceEntry to have been called", service.id, svcMgr.db.getServiceEntryServiceId);

        // Should have installed listener with ServicesDatabase
        assertEquals("Should've listener for serviceId", service.id, svcMgr.db.serviceId);
        assertSame("Should've installed self as listener for serviceId", appDomain, svcMgr.db.listener);

        // Update service's apps
        TestAppDomain test = (TestAppDomain) appDomain;
        test.clear();
        service.apps.removeElement(apps[0]);
        svcMgr.db.listener.serviceUpdate(service);

        // Check number of kill/destroy
        assertEquals("Unexpected number of apps started", 0, test.started.size());
        assertEquals("Unexpected number of apps killed", 1, test.killed.size());
        assertEquals("Unexpected number of apps destroyed", 0, test.destroyed.size());

        assertTrue("Expected app " + apps[0].id + " to be destroyed", test.killed.removeElement(apps[0].id));
    }

    /**
     * Test signalling of duplicate instances of same app. New high priority
     * entry indicates AUTOSTART.
     */
    public void testServiceUpdate_autostart_HighPriority() throws Exception
    {
        doTestServiceUpdate_priority(OcapAppAttributes.AUTOSTART, 1, 0);
    }

    /**
     * Test signalling of duplicat instances of same app. New high launchOrder
     * entry indicates AUTOSTART.
     */
    public void testServiceUpdate_autostart_HighLaunchOrder() throws Exception
    {
        doTestServiceUpdate_priority(OcapAppAttributes.AUTOSTART, 0, 1);
    }

    /**
     * Implement testServiceUpdate_autostart_*() based upon priority.
     */
    private void doTestServiceUpdate_priority(int state, int priority, int launchOrder) throws Exception
    {
        XTestApp[] apps = {
                new XTestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 100), "App0", true,
                        new String[0], "/", "app0.Xlet", new String[0], 0),
                new XTestApp(OcapAppAttributes.PRESENT, new AppID(TestAppIDs.APPDOMAINIMPL, 101), "App1", true,
                        new String[0], "/", "app1.Xlet", new String[0], 0), };
        TestAbstractService service = new TestAbstractService(0x23456, "wxyz", false, apps);

        // Install a custom ServicesDatabase
        // Including the given service (plus apps)
        svcMgr.db.db.put(new Integer(service.id), service);
        assertEquals("Internal failure - ServiceDatabase doesn't have right entry!", service,
                svcMgr.db.getServiceEntry(service.id));
        svcMgr.db.getServiceEntryServiceId = -1;

        appDomain.select(service.details, null);

        // Should acquire information from ServicesDatabase
        assertEquals("Expected getServiceEntry to have been called", service.id, svcMgr.db.getServiceEntryServiceId);

        // Should have installed listener with ServicesDatabase
        assertEquals("Should've listener for serviceId", service.id, svcMgr.db.serviceId);
        assertSame("Should've installed self as listener for serviceId", appDomain, svcMgr.db.listener);

        // Update service's apps
        TestAppDomain test = (TestAppDomain) appDomain;
        test.clear();
        XTestApp[] apps2 = { (XTestApp) apps[1].clone(), (XTestApp) apps[0].clone(), };
        apps2[0].controlCode = OcapAppAttributes.AUTOSTART;
        apps2[0].priority -= priority;
        //apps2[0].launchOrder -= launchOrder;
        apps2[1].controlCode = OcapAppAttributes.AUTOSTART;
        apps2[1].priority += priority;
        //apps2[1].launchOrder += launchOrder;
        service.apps.addElement(apps2[0]);
        service.apps.addElement(apps2[1]);
        svcMgr.db.listener.serviceUpdate(service);

        // Check number of kill/destroy
        assertEquals("Unexpected number of apps started", 1, test.started.size());
        assertEquals("Unexpected number of apps killed", 0, test.killed.size());
        assertEquals("Unexpected number of apps destroyed", 0, test.destroyed.size());

        assertTrue("Expected app " + apps2[1].id + " to be autostarted", test.started.removeElement(apps2[1]));

    }

    public AppDomainImplTest(String name)
    {
        super(name);
    }

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(AppDomainImplTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new AppDomainImplTest(tests[i]));
            return suite;
        }
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

    private static class Factory implements AppsDatabaseTest.ExtendedTest.AppsDatabaseFactory
    {
        public void update(AppsDatabase db, AppEntry[] apps)
        {
            sigMgr.listener.signallingReceived(new SignallingEvent(this, new TestAit(apps)));
        }

        private ServiceManager saveSvcMgr;

        private TestSvcMgr svcMgr;

        private void replaceSvcMgr()
        {
            saveSvcMgr = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
            ManagerManagerTest.updateManager(ServiceManager.class, TestSvcMgr.class, false, svcMgr = new TestSvcMgr());
        }

        private void restoreSvcMgr()
        {
            svcMgr = null;
            ManagerManagerTest.updateManager(ServiceManager.class, saveSvcMgr.getClass(), true, saveSvcMgr);
        }

        // TODO: cleanup below; this is duplicated from main test class!!!!

        private ServiceContext serviceContext;

        private SignallingManager saveSigMgr;

        private TestSigMgr sigMgr;

        private void replaceSigMgr()
        {
            saveSigMgr = (SignallingManager) ManagerManager.getInstance(SignallingManager.class);
            ManagerManagerTest.updateManager(SignallingManager.class, TestSigMgr.class, false,
                    sigMgr = new TestSigMgr());
        }

        private void restoreSigMgr()
        {
            sigMgr = null;
            ManagerManagerTest.updateManager(SignallingManager.class, saveSigMgr.getClass(), true, saveSigMgr);
        }

        public void dispose(AppsDatabase db)
        {
            try
            {
                ((AppDomainImpl) db).destroy();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            restoreSigMgr();
            restoreSvcMgr();
        }

        public Object createImplObject() throws Exception
        {
            // return new AppDomainImpl(serviceContext);

            replaceSvcMgr();
            replaceSigMgr();

            serviceContext = new TestServiceContext();
            // appDomain = new AppDomainImpl(serviceContext);
            AppDomainImpl appDomain = new TestAppDomain(serviceContext);
            TestService service = new TestService(0x2345, "wxyz");
            appDomain.select(service.details, null);

            return appDomain;
        }
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(AppDomainImplTest.class);

        InterfaceTestSuite isuite = AppsDatabaseTest.ExtendedTest.isuite();
        isuite.addFactory(new Factory());
        suite.addTest(isuite);
        return suite;
    }

    private AppDomainImpl appDomain;

    private ServiceContext serviceContext;

    public void setUp() throws Exception
    {
        super.setUp();
        replaceSvcMgr();
        replaceSigMgr();

        serviceContext = new TestServiceContext();
        // appDomain = new AppDomainImpl(serviceContext);
        appDomain = new TestAppDomain(serviceContext);
    }

    public void tearDown() throws Exception
    {
        try
        {
            // Don't let exceptions spoil cleanup...
            appDomain.destroy();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        appDomain = null;

        restoreSigMgr();
        restoreSvcMgr();
        super.tearDown();
    }

    private ServiceManager saveSvcMgr;

    private TestSvcMgr svcMgr;

    private void replaceSvcMgr()
    {
        saveSvcMgr = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        ManagerManagerTest.updateManager(ServiceManager.class, TestSvcMgr.class, false, svcMgr = new TestSvcMgr());
    }

    private void restoreSvcMgr()
    {
        svcMgr = null;
        ManagerManagerTest.updateManager(ServiceManager.class, saveSvcMgr.getClass(), true, saveSvcMgr);
    }

    private SignallingManager saveSigMgr;

    private TestSigMgr sigMgr;

    private void replaceSigMgr()
    {
        saveSigMgr = (SignallingManager) ManagerManager.getInstance(SignallingManager.class);
        ManagerManagerTest.updateManager(SignallingManager.class, TestSigMgr.class, false, sigMgr = new TestSigMgr());
    }

    private void restoreSigMgr()
    {
        sigMgr = null;
        ManagerManagerTest.updateManager(SignallingManager.class, saveSigMgr.getClass(), true, saveSigMgr);
    }

    private static class TestAppDomain extends AppDomainImpl
    {
        public Vector started = new Vector();

        public Vector destroyed = new Vector();

        public Vector killed = new Vector();

        TestAppDomain(ServiceContext sc)
        {
            this(sc, new CompositeAppsDB());
        }

        TestAppDomain(ServiceContext sc, CompositeAppsDB global)
        {
            super(sc, global);
        }

        void clear()
        {
            started.removeAllElements();
            destroyed.removeAllElements();
            killed.removeAllElements();
        }

        void startApp(AppEntry entry)
        {
            // Don't call super, as we don't want to start anything here...
            started.addElement(entry);
        }

        void stopApp(AppID id, boolean forced)
        {
            if (forced)
                killed.addElement(id);
            else
                destroyed.addElement(id);
            super.stopApp(id, forced);
        }

        void disposeApp(/* Application app, */AppID id)
        {
            killed.addElement(id);
            // super.disposeApp(app, id);
            super.disposeApp(id);
        }
    }

    public static TestService[] BOUND_SERVICES = { new TestService(0x1234, "ABC"), new TestService(0x5678, "DEF"),
            new TestService(0x9abc, "GHI") };

    public static class TestApp extends AitTest.TestApp implements Cloneable
    {
        public TestApp(int controlCode, AppID id, String name, boolean serviceBound, String[] parameters,
                String baseDirectory, String className, String[] classPathExtension)
        {
            super(true, controlCode, id, makeNames(name), null, serviceBound, 3, 128, null, 0, parameters,
                    baseDirectory, className, classPathExtension, makeTP(), null, null, null, null, new int[] {});
        }

        private static Hashtable makeNames(String name)
        {
            Hashtable h = new Hashtable();
            h.put("eng", name);
            return h;
        }

        protected static AppEntry.TransportProtocol[] makeTP()
        {
            return new TransportProtocol[] { new AppEntry.LocalTransportProtocol() };
        }

        public Object clone() throws CloneNotSupportedException
        {
            return super.clone();
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
    }

    public static class XTestApp extends TestApp 
    {
        public AbstractServiceEntry service;

        public int source = Xait.NETWORK_SIGNALLING;

        public XTestApp(int controlCode, AppID id, String name, int priority, int launchOrder, int version)
        {
            this(controlCode, id, name, true, new String[0], "/", "xlet.Xlet", new String[0], null, version);
            this.priority = priority;
            //this.launchOrder = launchOrder;
        }

        public XTestApp(int controlCode, AppID id, String name, boolean serviceBound, String[] parameters,
                String baseDirectory, String className, String[] classPathExtension, int version)
        {
            this(controlCode, id, name, serviceBound, parameters, baseDirectory, className, classPathExtension, null,
                    version);
        }

        public XTestApp(int controlCode, AppID id, String name, boolean serviceBound, String[] parameters,
                String baseDirectory, String className, String[] classPathExtension, AbstractServiceEntry service,
                int version)
        {
            super(controlCode, id, name, serviceBound, parameters, baseDirectory, className, classPathExtension);
            if (service != null)
            {
                this.service = service;
                //this.serviceId = service.id;

                service.apps.addElement(this);
            }
            this.version = version;
        }

        public int getServiceId()
        {
            return -1;
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

    /**
     *
     */
    static class TestAit implements Ait
    {
        private AppEntry[] apps;

        private ExternalAuthorization[] auth;

        public TestAit(AppEntry[] apps)
        {
            this.apps = apps;
            this.auth = new ExternalAuthorization[0];
        }

        public TestAit(AppEntry[] apps, AppID extAuth)
        {
            this(apps);
            this.auth = new ExternalAuthorization[] { new ExternalAuthorization() };
            this.auth[0].id = extAuth;
            this.auth[0].priority = 15;
        }

        public AppEntry[] getApps()
        {
            return apps;
        }

        public int getType()
        {
            return OcapAppAttributes.OCAP_J;
        }

        public ExternalAuthorization[] getExternalAuthorization()
        {
            return auth;
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

    /**
     * Replacement Service impl.
     */
    static class TestService extends XaitTest.TestService implements Service
    {
        public ServiceDetails details;

        public TestService(int id, String name)
        {
            super(true, id, name, false);
            details = new TestServiceDetails();
        }

        public TestService(int id, String name, TestApp[] apps)
        {
            this(id, name);
            this.apps = new Vector();
            for (int i = 0; i < apps.length; ++i)
            {
                if (apps[i] instanceof XTestApp) ((XTestApp) apps[i]).service = this;
                //apps[i].serviceId = id;
                this.apps.addElement(apps[i]);
            }
        }

        public SIRequest retrieveDetails(SIRequestor requestor)
        {
            return null;
        }

        public String getName()
        {
            return name;
        }

        public boolean hasMultipleInstances()
        {
            return false;
        }

        public ServiceType getServiceType()
        {
            return ServiceType.DIGITAL_TV;
        }

        public Locator getLocator()
        {
            try
            {
                return new OcapLocator(id);
            }
            catch (Exception e) // None expected
            {
                return null;
            }
        }

        public boolean equals(Object obj)
        {
            return obj != null && getClass() == obj.getClass() && id == ((TestService) obj).id;
        }

        public int hashCode()
        {
            return getLocator().hashCode();
        }

        class TestServiceDetails implements ServiceDetails
        {

            public void addServiceComponentChangeListener(ServiceComponentChangeListener listener)
            {
                // TODO Auto-generated method stub

            }

            public DeliverySystemType getDeliverySystemType()
            {
                // TODO Auto-generated method stub
                return null;
            }

            public String getLongName()
            {
                // TODO Auto-generated method stub
                return null;
            }

            public ProgramSchedule getProgramSchedule()
            {
                // TODO Auto-generated method stub
                return null;
            }

            public Service getService()
            {
                return TestService.this;
            }

            public ServiceType getServiceType()
            {
                // TODO Auto-generated method stub
                return null;
            }

            public void removeServiceComponentChangeListener(ServiceComponentChangeListener listener)
            {
                // TODO Auto-generated method stub

            }

            public SIRequest retrieveComponents(SIRequestor requestor)
            {
                // TODO Auto-generated method stub
                return null;
            }

            public SIRequest retrieveServiceDescription(SIRequestor requestor)
            {
                // TODO Auto-generated method stub
                return null;
            }

            public Locator getLocator()
            {
                return TestService.this.getLocator();
            }

            public ServiceInformationType getServiceInformationType()
            {
                // TODO Auto-generated method stub
                return null;
            }

            public Date getUpdateTime()
            {
                // TODO Auto-generated method stub
                return null;
            }

            public int[] getCASystemIDs()
            {
                // TODO Auto-generated method stub
                return null;
            }

            public boolean isFree()
            {
                // TODO Auto-generated method stub
                return false;
            }

        }
    }

    /**
     * Replacement AbstractService impl.
     */
    static class TestAbstractService extends TestService implements AbstractService
    {
        public TestAbstractService(int id, String name, boolean autoSelect)
        {
            super(id, name);
            this.autoSelect = true;
        }

        public TestAbstractService(int id, String name, boolean autoSelect, TestApp[] apps)
        {
            super(id, name, apps);
            this.autoSelect = true;
        }

        public java.util.Enumeration getAppIDs()
        {
            return null;
        }

        public java.util.Enumeration getAppAttributes()
        {
            return null;
        }
    }

    static class TestServiceContext implements ServiceContextExt
    {
        private boolean stopAbstractServiceFlag;

        public void select(Service selection)
        {
        }

        public void select(Locator[] components)
        {
        }

        public void stop()
        {
        }

        public void destroy()
        {
        }

        public ServiceContentHandler[] getServiceContentHandlers()
        {
            return null;
        }

        public Service getService()
        {
            return null;
        }

        public void addListener(ServiceContextListener listener)
        {
        }

        public void removeListener(ServiceContextListener listener)
        {
        }

        // from ServiceContexExt.java
        public void setDestroyWhenIdle(boolean destroyWhenIdle)
        {
        }

        public boolean isPresenting()
        {
            return false;
        }

        public boolean isDestroyed()
        {
            return false;
        }

        public void setPersistentVideoMode(boolean enable)
        {
        }

        public void setApplicationsEnabled(boolean appsEnabled)
        {
        }

        public boolean isAppsEnabled()
        {
            return false;
        }

        public boolean isPersistentVideoMode()
        {
            return false;
        }

        public void swapSettings(ServiceContext sc, boolean audioUse, boolean swapAppSettings)
                throws IllegalArgumentException
        {
        }

        public AppDomain getAppDomain()
        {
            return null;
        }

        public void setInitialBackground(VideoTransformation trans)
        {
        }

        public void setInitialComponent(Container parent, Rectangle rect)
        {
        }

        public CallerContext getCallerContext()
        {
            return null;
        }

        public CallerContext getCreatingContext()
        {
            return null;
        }

        public ServiceMediaHandler addServiceContextCallback(ServiceContextCallback callback, int priority)
        {
            return null;
        }

        public void removeServiceContextCallback(ServiceContextCallback callback)
        {
        }

        public void stopAbstractService()
        {
            stopAbstractServiceFlag = true;
        }

        public void setAvailableServiceContextDelegates(List serviceContextDelegates)
        {
            // TODO: implement
        }

        public void forceEASTune(Service service)
        {
            // TODO: implement
        }

        public void unforceEASTune()
        {

        }

        // test function
        public void setAbstractServiceFlagFalse()
        {
            stopAbstractServiceFlag = false;
        }

        // test function
        public boolean getAbstractServiceFlag()
        {
            return stopAbstractServiceFlag;
        }

        public void setDefaultVideoTransformation(org.dvb.media.VideoTransformation vt)
        {

        }

        // from DvbSerciceContext.java
        public org.davic.net.tuning.NetworkInterface getNetworkInterface()
        {
            return null;
        }

        public void setDefautVideoTransformation(org.dvb.media.VideoTransformation vt)
        {
        }
    }

    /**
     * Replacement SignallingManager.
     */
    static class TestSigMgr implements SignallingManager
    {
        public int serviceId = -1;

        public SignallingListener listener;

        public void destroy()
        {
        }

        public void addAitListener(OcapLocator loc, SignallingListener l)
        {
            this.serviceId = loc.getSourceID();
            this.listener = l;
        }

        public void removeAitListener(OcapLocator loc, SignallingListener l)
        {
            int serviceId = loc.getSourceID();
            if (serviceId == this.serviceId && l == this.listener)
            {
                this.serviceId = -1;
                this.listener = l;
            }
        }

        public void addXaitListener(SignallingListener l)
        {
        }

        public void removeXaitListener(SignallingListener l)
        {
        }

        public void registerUnboundApp(java.io.InputStream xait) throws IllegalArgumentException, SecurityException,
                java.io.IOException
        {
        }

        public Properties getAddressingProperties()
        {
            return null;
        }

        public void registerAddressingProperties(Properties properties, boolean persist, Date expirationDate)
        {
        }

        public void removeAddressingProperties(String[] properties)
        {
        }

        public Properties getSecurityAddressableAttributes()
        {
            return null;
        }

        public boolean loadPersistentXait()
        {
            return false;
        }

        public void deletePersistentXait()
        {
        }

        public void resignal(Ait ait)
        {
        }

        public void setAppSignalHandler(AppSignalHandler handler)
        {
        }

        public void resignal(AppID appID)
        {
            // TODO Auto-generated method stub

        }
    }

    /**
     * Replacement ServiceManager.
     */
    static class TestSvcMgr implements ServiceManager
    {
        public TestSvcDb db = new TestSvcDb();

        public void destroy()
        {
        }

        public ServicesDatabase getServicesDatabase()
        {
            return db;
        }

        // The following are not tested here
        public ServiceContextFactory getServiceContextFactory()
        {
            return null;
        }

        public SIManager createSIManager()
        {
            return null;
        }

        public MediaAPI getMediaAPI()
        {
            return null;
        }

        public SICache getSICache()
        {
            return null;
        }

        public SIDatabase getSIDatabase()
        {
            return null;
        }

        public ServiceDataSource createServiceDataSource(ServiceContextExt ctx, Service svc)
        {
            return null;
        }

        public ServiceMediaHandler createServiceMediaHandler(DataSource ds, ServiceContextExt sc, Object lock, ResourceUsageImpl resourceUsage)
        {
            return null;
        }

        public int getOOBWaitTime()
        {
            return 0;
        }

        public int getRequestAsyncTimeout()
        {
            return 0;
        }
    }

    /**
     *
     */
    static class TestSvcDb implements ServicesDatabase
    {
        public Hashtable db = new Hashtable();

        public int serviceId = -1;

        public ServicesDatabase.ServiceChangeListener listener;

        public int getServiceEntryServiceId = -1;

        public void bootProcess()
        { /* empty */
        }

        public void notifyMonAppConfiguring()
        { /* empty */
        }

        public void notifyMonAppConfigured()
        { /* empty */
        }

        public void addBootProcessCallback(BootProcessCallback toAdd)
        { /* empty */
        }

        public void removeBootProcessCallback(BootProcessCallback toRemove)
        { /* empty */
        }

        public AbstractServiceEntry getServiceEntry(int svc)
        {
            this.getServiceEntryServiceId = svc;
            return (AbstractServiceEntry) db.get(new Integer(svc));
        }

        public void addServiceChangeListener(int svc, ServicesDatabase.ServiceChangeListener l)
        {
            this.serviceId = svc;
            this.listener = l;
        }

        public void removeServiceChangeListener(int svc, ServicesDatabase.ServiceChangeListener l)
        {
            if (svc == this.serviceId && l == this.listener)
            {
                this.serviceId = -1;
                this.listener = l;
            }
        }

        public void unregisterUnboundApp(int svc, AppID appid)
        { /* empty */
        }

        public void setAppSignalHandler(org.ocap.application.AppSignalHandler h)
        { /* empty */
        }

        public AbstractService addSelectedService(int serviceID)
        {
            return null;
        }

        public void removeSelectedService(int service)
        { /* empty */
        }

        public boolean isServiceSelected(int service)
        {
            return false;
        }

        public AbstractService getAbstractService(int serviceId)
        {
            return null;
        }

        public void getAbstractServices(ServiceCollection collection)
        { /* empty */
        }

        public boolean isServiceMarked(int service)
        {
            return false;
        }
    }
}
