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

package org.dvb.application;

import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.test.ProxySecurityManager;
import org.cablelabs.test.iftc.InterfaceTestSuite;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestCase;

import org.dvb.application.AppProxyTest.DummySecurityManager;
import org.ocap.application.OcapAppAttributes;

/**
 * How can we REALLY test this?
 *
 * We need to be able to know what SHOULD be in the database
 * and cause the database to change.
 * We could use the AppManagerProxy.registerUnboundApp() to cause apps
 * to be added, which we know will be there.
 * We could use some other "backdoor" in the implementation to
 * either install our own apps or learn what is there.
 * Either way, it would be best for this to be a InterfaceTestCase
 * with an extended factory that allows us to manager the appsDB
 * implementation.
 *
 * What can be tested w/out such access?
 * - That filters other than those supported always return empty enumerations
 * - That getAppIDs/getAppAttributes only returns the expected AppIDs
 * - That getAppProxy returns something for given AppIDs, but not for others.
 * - That security is checked on getAppProxy.
 */

/**
 * Tests AppsDatabase. Meant to be extended by implementation-specific test.
 * 
 * @author Aaron Kamienski
 */
public class AppsDatabaseTest extends TestCase
{
    public static class TestApp extends AppEntry
    {
        private static int counter = 0;

        public static TestApp create()
        {
            TestApp app = new TestApp();

            ++counter;

            app.baseDirectory = "/a/b/c";
            app.className = "org.cablelabs.xlet.TestApp";
            app.classPathExtension = new String[0];
            app.controlCode = OcapAppAttributes.PRESENT;
            app.id = new AppID(0x4dad, 0x4000 + counter);
            app.names = new Hashtable();
            app.names.put("eng", "TestApp" + counter);
            app.parameters = new String[] { "Hello", "World" };
            app.priority = (100 + counter) % 255;
            app.serviceBound = true;
            //app.serviceId = 0x20000;
            app.transportProtocols = new TransportProtocol[] { new OcTransportProtocol()
            {
                {
                    componentTag = 10;
                    label = 1;
                    protocol = 1;
                    remoteConnection = true;
                    serviceId = 0x400;
                }
            }, new IcTransportProtocol()
            {
                {
                    label = 2;
                    protocol = 0x101;
                    urls.add("http://appsrus.com/TestApp/" + counter);
                }
            }, };
            app.version = 0;
            app.visibility = AppEntry.VISIBLE;

            return app;
        }
    }

    public static final AppsDatabaseFilter ALWAYS_YES_FILTER = new CurrentServiceFilter()
    {
        public boolean accept(AppID id)
        {
            return true;
        }
    };

    public static final AppsDatabaseFilter ALWAYS_NO_FILTER = new CurrentServiceFilter()
    {
        public boolean accept(AppID id)
        {
            return false;
        }
    };

    /**
     * InterfaceTestCase that is used to perform extended tests on the
     * AppsDatabase implementation. These tests require additional access to the
     * implementation (e.g., to install/remove apps) in order to fully test the
     * implementation.
     */
    public static class ExtendedTest extends InterfaceTestCase
    {
        private static AppEntry[] APPS = { TestApp.create(), TestApp.create(), TestApp.create(), TestApp.create(),
                TestApp.create(), };

        private static final AppEntry[] EMPTY = new AppEntry[0];

        private Vector apps = new Vector();

        private void addEntries(AppsDatabase db, AppEntry[] array)
        {
            for (int i = 0; i < array.length; ++i)
                apps.addElement(array[i]);
            update(db);
        }

        private void addEntry(AppsDatabase db, AppEntry app)
        {
            apps.addElement(app);
            update(db);
        }

        private void removeEntry(AppsDatabase db, AppEntry app)
        {
            apps.removeElement(app);
            update(db);
        }

        private void update(AppsDatabase db)
        {
            AppEntry[] array = new AppEntry[apps.size()];
            apps.copyInto(array);

            factory.update(db, array);
        }

        private void clear(AppsDatabase db)
        {
            apps.clear();
            factory.update(appsdatabase, EMPTY);
        }

        public void testInvalidFilter()
        {
            AppsDatabaseFilter filter = new AppsDatabaseFilter()
            {
                public boolean accept(AppID id)
                {
                    return true;
                }
            };

            addEntries(appsdatabase, APPS);

            Enumeration e;
            e = appsdatabase.getAppAttributes(filter);
            assertFalse("Should be empty (not valid filter type)", e.hasMoreElements());

            e = appsdatabase.getAppIDs(filter);
            assertFalse("Should be empty (not valid filter type)", e.hasMoreElements());

            e = appsdatabase.getAppAttributes(new CurrentServiceFilter());
            assertTrue("Should be non-empty (valid filter type)", e.hasMoreElements());
        }

        /**
         * Tests size(). Make sure it matches what we think it should.
         */
        public void testSize()
        {
            int size1 = appsdatabase.size();
            int size2 = appsdatabase.size();
            assertEquals("Expected same size to be returned", size1, size2);

            assertEquals("Expected database to be emptied", 0, appsdatabase.size());

            for (int i = 0; i < APPS.length; ++i)
            {
                addEntry(appsdatabase, APPS[i]);

                assertEquals("Unexpected number of entries in database", i + 1, appsdatabase.size());
            }

            // duplicates don't count
            for (int i = 0; i < APPS.length; ++i)
            {
                addEntry(appsdatabase, APPS[i]);

                assertEquals("Unexpected number of entries in database", APPS.length, appsdatabase.size());
            }

            // add another version
            AppEntry v2 = APPS[0].copy();
            ++v2.version;

            addEntry(appsdatabase, v2);
            assertEquals("New version should replace old version", APPS.length, appsdatabase.size());
        }

        /**
         * Tests getAppIDs(). Make sure we can get for all installed apps. Make
         * sure we only see the ones we're supposed to see. Tests
         * CurrentServiceFilter and RunningApplicationsFilter.
         */
        public void getGetAppIDs()
        {
            Enumeration e;
            clear(appsdatabase);

            e = appsdatabase.getAppIDs(new CurrentServiceFilter());
            assertNotNull("Enumeration should not be null", e);
            assertFalse("Enumeration should be empty", e.hasMoreElements());

            e = appsdatabase.getAppIDs(new RunningApplicationsFilter());
            assertNotNull("Enumeration should not be null", e);
            assertFalse("Enumeration should be empty", e.hasMoreElements());

            e = appsdatabase.getAppIDs(ALWAYS_YES_FILTER);
            assertNotNull("Enumeration should not be null", e);
            assertFalse("Enumeration should be empty", e.hasMoreElements());

            // Add a bunch of AppIDs
            HashSet set = new HashSet();
            for (int i = 0; i < APPS.length; ++i)
            {
                addEntry(appsdatabase, APPS[i]);

                set.add(APPS[i].id);
            }

            checkGetAppIDs((HashSet) set.clone(), ALWAYS_YES_FILTER);
            checkGetAppIDs((HashSet) set.clone(), new CurrentServiceFilter());

            // Add non-visible Apps
            AppEntry nonVis = TestApp.create();
            nonVis.visibility = AppEntry.NON_VISIBLE;
            addEntry(appsdatabase, nonVis);

            // TODO: how to prove that non-visible app is really there???? I.e.,
            // can be launched!

            // Non-visible App should not show up
            checkGetAppIDs((HashSet) set.clone(), ALWAYS_YES_FILTER);
            checkGetAppIDs((HashSet) set.clone(), new CurrentServiceFilter());

            // Replace non-visible app
            removeEntry(appsdatabase, nonVis);
            nonVis.visibility = AppEntry.LISTING_ONLY;
            addEntry(appsdatabase, nonVis);

            // Previously non-visible app should show up
            set.add(nonVis.id);
            checkGetAppIDs((HashSet) set.clone(), ALWAYS_YES_FILTER);
            checkGetAppIDs((HashSet) set.clone(), new CurrentServiceFilter());
        }

        private void checkGetAppIDs(HashSet expected, AppsDatabaseFilter filter)
        {
            // Make sure that all AppIDs are returned
            for (Enumeration e = appsdatabase.getAppIDs(filter); e.hasMoreElements();)
            {
                AppID id = (AppID) e.nextElement();
                assertTrue("Unexpected AppID returned", expected.contains(id));
                expected.remove(id);
            }
            assertTrue("Not all AppIDs returned", expected.isEmpty());
        }

        /**
         * Tests getAppAttributes(filter). Make sure we can get for all
         * installed apps. Make sure we only see the ones we're supposed to see.
         * Tests CurrentServiceFilter and RunningApplicationsFilter.
         */
        public void testGetAppAttributes()
        {
            Enumeration e;
            clear(appsdatabase);

            for (int i = 0; i < APPS.length; ++i)
                assertNull("Expected no AppAttributes for " + APPS[i].id, appsdatabase.getAppAttributes(APPS[i].id));

            e = appsdatabase.getAppAttributes(new CurrentServiceFilter());
            assertNotNull("Enumeration should not be null", e);
            assertFalse("Enumeration should be empty", e.hasMoreElements());

            e = appsdatabase.getAppAttributes(new RunningApplicationsFilter());
            assertNotNull("Enumeration should not be null", e);
            assertFalse("Enumeration should be empty", e.hasMoreElements());

            e = appsdatabase.getAppAttributes(ALWAYS_YES_FILTER);
            assertNotNull("Enumeration should not be null", e);
            assertFalse("Enumeration should be empty", e.hasMoreElements());

            // Add a bunch of AppIDs
            Hashtable set = new Hashtable();
            for (int i = 0; i < APPS.length; ++i)
            {
                addEntry(appsdatabase, APPS[i]);

                set.put(APPS[i].id, APPS[i]);

                checkAppAttributes(APPS[i], (OcapAppAttributes) appsdatabase.getAppAttributes(APPS[i].id));
            }

            checkGetAppAttributes((Hashtable) set.clone(), ALWAYS_YES_FILTER);
            checkGetAppAttributes((Hashtable) set.clone(), new CurrentServiceFilter());

            // Add non-visible Apps
            AppEntry nonVis = TestApp.create();
            nonVis.visibility = AppEntry.NON_VISIBLE;
            addEntry(appsdatabase, nonVis);

            // TODO: how to prove that non-visible app is really there???? I.e.,
            // can be launched!

            // Non-visible App should not show up
            checkAppAttributes(nonVis, (OcapAppAttributes) appsdatabase.getAppAttributes(nonVis.id));
            checkAppAttributes(null, (OcapAppAttributes) appsdatabase.getAppAttributes(nonVis.id));
            checkGetAppAttributes((Hashtable) set.clone(), ALWAYS_YES_FILTER);
            checkGetAppAttributes((Hashtable) set.clone(), new CurrentServiceFilter());

            // Replace non-visible app
            removeEntry(appsdatabase, nonVis);
            nonVis.visibility = AppEntry.LISTING_ONLY;
            addEntry(appsdatabase, nonVis);
            set.put(nonVis.id, nonVis);

            // Previously non-visible app should show up
            checkAppAttributes(nonVis, (OcapAppAttributes) appsdatabase.getAppAttributes(nonVis.id));
            checkGetAppAttributes((Hashtable) set.clone(), ALWAYS_YES_FILTER);
            checkGetAppAttributes((Hashtable) set.clone(), new CurrentServiceFilter());
        }

        private void checkGetAppAttributes(Hashtable expected, AppsDatabaseFilter filter)
        {
            for (Enumeration e = appsdatabase.getAppAttributes(filter); e.hasMoreElements();)
            {
                OcapAppAttributes attribs = (OcapAppAttributes) e.nextElement();
                AppID id = attribs.getIdentifier();
                AppEntry entry = (AppEntry) expected.get(id);

                checkAppAttributes(entry, attribs);

                expected.remove(id);
            }
            assertTrue("Not all AppIDs returned", expected.isEmpty());
        }

        private void checkAppAttributes(AppEntry entry, OcapAppAttributes attribs)
        {
            if (entry == null)
            {
                assertNull("Expected no AppAttributes for non-existent entry", attribs);
                return;
            }
            if (entry.visibility == AppEntry.NON_VISIBLE)
            {
                assertNull("Expected no AppAttributes for NON-VISIBLE " + entry.id, attribs);
                return;
            }
            assertNotNull("No matching AppAttributes for " + entry.id, attribs);

            assertEquals("Unexpected AppID", entry.id, attribs.getIdentifier());
            // attribs.getAppIcon();
            assertEquals("Unexpected ACC", entry.controlCode, attribs.getApplicationControlCode());
            assertEquals("Unexpected serviceBound", entry.serviceBound, attribs.getIsServiceBound());
            // attribs.getName();
            // attribs.getName(code);
            // attribs.getNames();
            assertEquals("Unexpected priority", entry.priority, attribs.getPriority());
            // attribs.getProfiles();
            // attribs.getVersions(profile);
            assertEquals("Unexpected dvb.j.location.base", entry.baseDirectory,
                    attribs.getProperty("dvb.j.location.base"));
            assertEquals("Unexpected cpath", entry.classPathExtension,
                    (String[]) attribs.getProperty("dvb.j.location.cpath.extension"));
            // attribs.getProperty("dvb.transport.oc.component.tag");
            assertEquals("Unexpected ocap.j.location", entry.baseDirectory, attribs.getProperty("ocap.j.location"));
            // attribs.getServiceLocator();
            int sp = attribs.getStoragePriority();
            //assertEquals("Unexpected storage priority", entry.storagePriority, sp == 0 ? entry.storagePriority : sp);
            //assertEquals("Unexpected type", entry.type, attribs.getType());
        }

        private void assertEquals(String what, String[] a1, String[] a2)
        {
            if (a1 == a2) return;
            assertNotNull(what + " should be null", a1);
            assertNotNull(what + " should not be null", a2);
            assertEquals("Unexpected length for " + what, a1.length, a2.length);

            for (int i = 0; i < a1.length; ++i)
                assertEquals("Unexpected value for " + what + "[" + i + "]", a1[i], a2[i]);
        }

        /**
         * Tests getProxy. Make sure we can get for all installed apps. Make
         * sure we only see the ones we're supposed to see.
         */
        public void testGetAppProxy()
        {
            Enumeration e;
            clear(appsdatabase);

            for (int i = 0; i < APPS.length; ++i)
                assertNull("Expected no AppProxy for " + APPS[i].id, appsdatabase.getAppProxy(APPS[i].id));

            // Add a bunch of Apps
            for (int i = 0; i < APPS.length; ++i)
                addEntry(appsdatabase, APPS[i]);

            // Check for AppProxy
            for (int i = 0; i < APPS.length; ++i)
                assertNotNull("Expected AppProxy for " + APPS[i].id, appsdatabase.getAppProxy(APPS[i].id));

            // Add non-visible Apps
            AppEntry nonVis = TestApp.create();
            nonVis.visibility = AppEntry.NON_VISIBLE;
            addEntry(appsdatabase, nonVis);

            // TODO: how to prove that non-visible app is really there???? I.e.,
            // can be launched!

            // Non-visible App should not show up
            for (int i = 0; i < APPS.length; ++i)
                assertNotNull("Expected AppProxy for " + APPS[i].id, appsdatabase.getAppProxy(APPS[i].id));
            assertNull("Expected No AppProxy for non-visible", appsdatabase.getAppProxy(nonVis.id));

            // Replace non-visible app
            removeEntry(appsdatabase, nonVis);
            nonVis.visibility = AppEntry.LISTING_ONLY;
            addEntry(appsdatabase, nonVis);

            // Previously non-visible app should show up
            for (int i = 0; i < APPS.length; ++i)
                assertNotNull("Expected AppProxy for " + APPS[i].id, appsdatabase.getAppProxy(APPS[i].id));
            assertNotNull("Expected AppProxy for visible", appsdatabase.getAppProxy(nonVis.id));
        }

        /**
         * Tests add|removeListener. Make sure that we are notified about
         * changes in database.
         */
        public void XXXtestAddRemoveListener()
        {
            fail("Unimplemented test");
            // TODO: implement testAddRemoveListener
        }

        public static InterfaceTestSuite isuite()
        {
            InterfaceTestSuite suite = new InterfaceTestSuite(ExtendedTest.class);
            suite.setName("Extended");
            return suite;
        }

        public ExtendedTest(String name, ImplFactory f)
        {
            this(name, AppsDatabase.class, f);
        }

        protected ExtendedTest(String name, Class impl, ImplFactory f)
        {
            super(name, impl, f);
            factory = (AppsDatabaseFactory) f;
        }

        protected AppsDatabase createAppsDatabase()
        {
            return (AppsDatabase) createImplObject();
        }

        protected AppsDatabase appsdatabase;

        protected AppsDatabaseFactory factory;

        protected void setUp() throws Exception
        {
            super.setUp();
            appsdatabase = createAppsDatabase();
            clear(appsdatabase);
        }

        protected void tearDown() throws Exception
        {
            factory.dispose(appsdatabase);
            super.tearDown();
        }

        /**
         * Extends the ImplFactory interface to provide methods to control the
         * contents of the AppsDatabase for testing.
         */
        public static interface AppsDatabaseFactory extends ImplFactory
        {
            /**
             * Populate the database with new information.
             */
            public void update(AppsDatabase db, AppEntry[] apps);

            /**
             * Cleanup when the database is no longer needed.
             */
            public void dispose(AppsDatabase db);
        }
    }

    /**
     * Tests getAppsDatabase().
     */
    public void testGetAppsDatabase() throws Exception
    {
        AppsDatabase db = AppsDatabase.getAppsDatabase();

        assertNotNull("getAppsDatabase should not return null", db);
        assertSame("getAppsDatabase should return a singleton (same on successive calls)", db, appsdatabase);
        assertSame("getAppsDatabase should return a singleton (same on successive calls)", db,
                AppsDatabase.getAppsDatabase());
    }

    /**
     * Test size().
     */
    public void testSize() throws Exception
    {
        DummyFilter filter = new DummyFilter();
        Enumeration e = appsdatabase.getAppIDs(filter);
        assertNotNull("Enumeration should not be null", e);
        int size = 0;
        while (e.hasMoreElements())
            ++size;
        assertTrue("The size of the database should be at least the number of " + " appIds in the database",
                appsdatabase.size() >= size);
    }

    /**
     * Tests getAppIDs().
     */
    public void testGetAppIDs() throws Exception
    {
        Enumeration e;

        e = appsdatabase.getAppIDs(new AppsDatabaseFilter()
        {
            public boolean accept(AppID id)
            {
                return true;
            }
        });
        assertNotNull("Enumeration should not be null", e);
        assertTrue("Enumeration should contain no elements if not CurrentServiceFilter"
                + "or RunningApplicationsFilter", !e.hasMoreElements());

        e = appsdatabase.getAppIDs(new CurrentServiceFilter()
        {
            public boolean accept(AppID id)
            {
                return false;
            }
        });
        assertNotNull("Enumeration should not be null", e);
        assertTrue("Enumeration should contain no elements if none are accepted", !e.hasMoreElements());

        DummyFilter filter = new DummyFilter();
        e = appsdatabase.getAppIDs(filter);
        assertNotNull("Enumeration should not be null", e);
        while (e.hasMoreElements())
        {
            Object obj = e.nextElement();
            assertNotNull("Enumeration should not return null items", obj);
            assertTrue("Enumeration should return appIDs", obj instanceof AppID);
            // Make sure we saw this AppID in accept
            assertNotNull("AppID should've been past to accept", filter.ids.get(obj));
            assertEquals("AppID should've been past to accept", filter.ids.get(obj), obj);
            // Remove id
            filter.ids.remove(obj);
        }
        assertTrue("All IDs should've been made available via enumeration", filter.ids.size() == 0);
    }

    /**
     * Tests getAppAttributes().
     */
    public void testGetAppAttributes() throws Exception
    {
        Enumeration e;

        e = appsdatabase.getAppAttributes(new AppsDatabaseFilter()
        {
            public boolean accept(AppID id)
            {
                return true;
            }
        });
        assertNotNull("Enumeration should not be null", e);
        assertTrue("Enumeration should contain no elements if not CurrentServiceFilter "
                + "or RunningApplicationsFilter", !e.hasMoreElements());

        e = appsdatabase.getAppAttributes(new CurrentServiceFilter()
        {
            public boolean accept(AppID id)
            {
                return false;
            }
        });
        assertNotNull("Enumeration should not be null", e);
        assertTrue("Enumeration should contain no elements if none are accepted", !e.hasMoreElements());

        DummyFilter filter = new DummyFilter();
        e = appsdatabase.getAppAttributes(filter);
        assertNotNull("Enumeration should not be null", e);
        while (e.hasMoreElements())
        {
            Object obj = e.nextElement();
            assertNotNull("Enumeration should not return null items", obj);
            assertTrue("Enumeration should return appAttribs", obj instanceof AppAttributes);
            AppAttributes attrib = (AppAttributes) obj;

            AppID id = attrib.getIdentifier();
            assertEquals("AppID should've been past to accept", filter.ids.get(obj), id);
            filter.ids.remove(id);
        }
        assertTrue("All attribs should've been made available via enumeration", filter.ids.size() == 0);
    }

    /**
     * Tests getAppAttributes() w/ appid.
     */
    public void testGetAppAttributes_appid() throws Exception
    {
        assertNull("Expected null attributes for non-existent application", appsdatabase.getAppAttributes(new AppID(0,
                0)));

        Enumeration e = appsdatabase.getAppIDs(new CurrentServiceFilter());
        assertNotNull("Enumeration should not be null", e);
        while (e.hasMoreElements())
        {
            Object obj = e.nextElement();
            assertNotNull("Enumeration should not return null items", obj);
            assertTrue("Enumeration should return appAttribs", obj instanceof AppAttributes);
            AppAttributes attrib = (AppAttributes) obj;
            AppAttributes attrib2 = appsdatabase.getAppAttributes(attrib.getIdentifier());

            // Make sure we get the same as via the enumeration
            if (attrib != attrib2)
            {
                assertEquals("Expected same AppID", attrib.getIdentifier(), attrib2.getIdentifier());
                assertEquals("Expected same serviceBound", attrib.getIsServiceBound(), attrib2.getIsServiceBound());
                assertEquals("Expected same name", attrib.getName(), attrib2.getName());
                assertEquals("Expected same priority", attrib.getPriority(), attrib2.getPriority());
                assertEquals("Expected same locator", attrib.getServiceLocator(), attrib2.getServiceLocator());
                assertEquals("Expected same type", attrib.getType(), attrib2.getType());
                assertEquals("Expected same isStartable", attrib.isStartable(), attrib2.isStartable());
                assertEquals("Expected same implementation class", attrib.getClass(), attrib2.getClass());
                if (attrib instanceof OcapAppAttributes)
                {
                    OcapAppAttributes oa = (OcapAppAttributes) attrib;
                    OcapAppAttributes oa2 = (OcapAppAttributes) attrib2;
                    assertEquals("Expected same control code", oa.getApplicationControlCode(),
                            oa2.getApplicationControlCode());
                    assertEquals("Expected same storage priority", oa.getStoragePriority(), oa2.getStoragePriority());
                    assertEquals("Expected same hasNewVersion", oa.hasNewVersion(), oa2.hasNewVersion());
                    assertEquals("Expect same isNewVersionSignaled", oa.isNewVersionSignaled(),
                            oa2.isNewVersionSignaled());

                }
            }
        }
    }

    /**
     * Tests getAppProxy().
     * 
     * @todo disabled per 5685
     */
    public void xxxtestGetAppProxy() throws Exception
    {
        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        try
        {

            // Call getAppProxy
            Enumeration e = appsdatabase.getAppIDs(new CurrentServiceFilter());
            assertNotNull("Should be able to get enumeration of AppIDs", e);
            assertTrue("No AppIDs are in database w/ CurrentServiceFilter", e.hasMoreElements());
            while (e.hasMoreElements())
            {
                AppID id = (AppID) e.nextElement();
                AppProxy proxy = appsdatabase.getAppProxy(id);

                assertNotNull("Should return a proxy for each appID", proxy);
                assertTrue("Only DVBJProxy's should be returned for this version", proxy instanceof DVBJProxy);
            }
        }
        finally
        {
            ProxySecurityManager.pop();
        }
    }

    /**
     * Tests getAppProxy(); ensures that SecurityManager is consulted.
     * 
     * @todo disabled per 5685
     */
    public void xxxtestGetAppProxy_security() throws Exception
    {
        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        try
        {

            // Clear current Permission (only catch the first)
            sm.p = null;

            // Call getAppProxy
            Enumeration e = appsdatabase.getAppIDs(new CurrentServiceFilter());
            assertNotNull("Should be able to get enumeration of AppIDs", e);
            assertTrue("No AppIDs are in database w/ CurrentServiceFilter", e.hasMoreElements());
            AppID id = (AppID) e.nextElement();
            AppProxy proxy = appsdatabase.getAppProxy(id);

            assertNotNull("SecurityManager.checkPermission should be called", sm.p);
            assertTrue("SecurityManager.checkPermission should be called with an AppsControlPermission",
                    sm.p instanceof AppsControlPermission);
        }
        finally
        {
            ProxySecurityManager.pop();
        }
    }

    /**
     * Tests addRemoveListener().
     */
    public void testAddRemoveListener() throws Exception
    {
        // Verify that no exceptions are thrown
        AppsDatabaseEventListener l1, l2;
        appsdatabase.addListener(l1 = new AppsDatabaseEventListener()
        {
            public void entryAdded(AppsDatabaseEvent e)
            {
            }

            public void entryChanged(AppsDatabaseEvent e)
            {
            }

            public void entryRemoved(AppsDatabaseEvent e)
            {
            }

            public void newDatabase(AppsDatabaseEvent e)
            {
            }
        });
        appsdatabase.addListener(l2 = new AppsDatabaseEventListener()
        {
            public void entryAdded(AppsDatabaseEvent e)
            {
            }

            public void entryChanged(AppsDatabaseEvent e)
            {
            }

            public void entryRemoved(AppsDatabaseEvent e)
            {
            }

            public void newDatabase(AppsDatabaseEvent e)
            {
            }
        });
        appsdatabase.addListener(l1);
        appsdatabase.removeListener(l1);
        appsdatabase.removeListener(l1);
        appsdatabase.removeListener(l2);
        // Extra removals shouldn't be a problem
        appsdatabase.removeListener(l1);
        appsdatabase.removeListener(l2);
    }

    class DummyFilter extends CurrentServiceFilter
    {
        public boolean nullId;

        public Hashtable ids = new Hashtable();

        public void clear()
        {
            ids.clear();
        }

        public boolean accept(AppID id)
        {
            if (id == null)
                nullId = true;
            else
                ids.put(ids, ids);
            return true;
        }
    }

    public AppsDatabaseTest(String name)
    {
        super(name);
    }

    protected AppsDatabase appsdatabase;

    protected AppsDatabase createAppsDatabase() throws Exception
    {
        return AppsDatabase.getAppsDatabase();
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        appsdatabase = createAppsDatabase();
    }

    protected void tearDown() throws Exception
    {
        appsdatabase = null;
        super.tearDown();
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
        TestSuite suite = new TestSuite(AppsDatabaseTest.class);
        return suite;
    }
}
