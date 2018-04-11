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

/*
 * Created on Jan 30, 2006
 */
package org.cablelabs.impl.manager.application;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestCase;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.davic.net.Locator;
import org.dvb.application.AppAttributes;
import org.dvb.application.AppID;
import org.dvb.application.AppIcon;
import org.dvb.application.AppProxy;
import org.dvb.application.AppStateChangeEventListener;
import org.dvb.application.AppsDatabase;
import org.dvb.application.AppsDatabaseEvent;
import org.dvb.application.AppsDatabaseEventListener;
import org.dvb.application.AppsDatabaseFilter;
import org.dvb.application.CurrentServiceFilter;
import org.dvb.application.DVBJProxy;
import org.dvb.application.IllegalProfileParameterException;
import org.dvb.application.LanguageNotAvailableException;
import org.ocap.application.OcapAppAttributes;

/**
 * Tests CompositeAppsDB.
 * 
 * @author Aaron Kamienski
 */
public class CompositeAppsDBTest extends TestCase
{
    public void testNothing()
    {
        /* empty */
    }

    /**
     * Parameterized sub-test that tests CompositeAppsDB given various inputs.
     * 
     * @author Aaron Kamienski
     */
    public static class SubTest extends InterfaceTestCase
    {
        public void testInit()
        {
            for (int i = 0; i < dbs.length; ++i)
            {
                for (Enumeration keys = dbs[i].apps.keys(); keys.hasMoreElements();)
                {
                    AppID id = (AppID) keys.nextElement();
                    assertNotNull("Expected AppAttributes to be returned for " + id, db.getAppAttributes(id));
                    assertNotNull("Expected AppProxy to be returned for " + id, db.getAppProxy(id));
                }
            }
        }

        /**
         * Tests adding a new database.
         */
        public void testAddAppsDatabase() throws Exception
        {
            int oldSize = db.size();
            Listener l = new Listener();
            db.addListener(l);

            // Add a new DB
            AppID[] apps = { new AppID(0x1000, 0x1000), new AppID(0x1000, 0x1001), };
            TestDB test = new TestDB(apps);
            db.addAppsDatabase(test);

            // Check new size
            assertEquals("Unexpected new size", oldSize + apps.length, db.size());

            // Should be able to get these new AppIDs
            for (int i = 0; i < apps.length; ++i)
            {
                assertNotNull("Expected AppAttributes to be returned for " + apps[i], db.getAppAttributes(apps[i]));
                assertNotNull("Expected AppProxy to be returned for " + apps[i], db.getAppProxy(apps[i]));
            }

            // Should be able to still get at old AppIDs
            testInit();

            // Should've been notified of newAppsDB
            // No other changes
            synchronized (l.newDb)
            {
                if (l.newDb.size() == 0) l.newDb.wait(5000L);
                assertEquals("Expected one NEWDB event", 1, l.newDb.size());
            }
            assertEquals("Expected no ADDED events", 0, l.added.size());
            assertEquals("Expected no REMOVED events", 0, l.removed.size());
            assertEquals("Expected no CHANGED events", 0, l.changed.size());
        }

        /**
         * Tests removing a database.
         */
        public void testRemoveAppsDatabase() throws Exception
        {
            Listener l = new Listener();
            db.addListener(l);

            // Remove a database that isn't there
            synchronized (l.newDb)
            {
                db.removeAppsDatabase(new TestDB());
                l.newDb.wait(200L);
                assertEquals("Expected no NEWDB event", 0, l.newDb.size());
            }

            // Remove each DB
            for (int i = 0; i < dbs.length; ++i)
            {
                l.clear();
                int oldSize = db.size();
                db.removeAppsDatabase(dbs[i]);

                // Check new size
                assertEquals("Unexpected new size", oldSize - dbs[i].apps.size(), db.size());

                // Cannot get old Apps
                for (int j = 0; j <= i; ++j)
                {
                    TestDB removed = dbs[j];
                    for (Enumeration keys = removed.apps.keys(); keys.hasMoreElements();)
                    {
                        AppID id = (AppID) keys.nextElement();

                        assertNull("Removed db apps should not be available for " + id, db.getAppAttributes(id));
                        assertNull("Removed db apps should not be available for " + id, db.getAppProxy(id));
                    }
                }

                // Can still get other apps
                for (int j = i + 1; j < dbs.length; ++j)
                {
                    TestDB present = dbs[j];
                    for (Enumeration keys = present.apps.keys(); keys.hasMoreElements();)
                    {
                        AppID id = (AppID) keys.nextElement();

                        assertNotNull("Removed db apps should not be available for " + id, db.getAppAttributes(id));
                        assertNotNull("Removed db apps should not be available for " + id, db.getAppProxy(id));
                    }
                }

                // Should've been notified of newAppsDB
                // No other changes
                synchronized (l.newDb)
                {
                    if (l.newDb.size() == 0) l.newDb.wait(5000L);
                    assertEquals("Expected one NEWDB event", 1, l.newDb.size());
                }
                assertEquals("Expected no ADDED events", 0, l.added.size());
                assertEquals("Expected no REMOVED events", 0, l.removed.size());
                assertEquals("Expected no CHANGED events", 0, l.changed.size());
            }

            // Remove a database that isn't there
            l.clear();
            synchronized (l.newDb)
            {
                db.removeAppsDatabase(new TestDB());
                l.newDb.wait(200L);
                assertEquals("Expected no NEWDB event", 0, l.newDb.size());
            }
        }

        public void testSize()
        {
            int size = 0;
            for (int i = 0; i < dbs.length; ++i)
                size += dbs[i].apps.size();
            assertEquals("Unexpected size", size, db.size());
        }

        public void testGetAppIDs()
        {
            Enumeration appIds = db.getAppIDs(new CurrentServiceFilter());
            assertNotNull("Should not return null enum", appIds);

            for (; appIds.hasMoreElements();)
            {
                AppID id = (AppID) appIds.nextElement();

                assertNotNull("Null AppIDs should not be returned", id);
                assertNotNull("AppID was unexpected " + id, all.remove(id));
            }
            // Any left-overs
            assertEquals("Some AppIDs were not returned", 0, all.size());
        }

        public void testGetAppAttributesAppsDatabaseFilter()
        {
            Enumeration attribs = db.getAppAttributes(new CurrentServiceFilter());
            assertNotNull("Should not return null enum", attribs);

            for (; attribs.hasMoreElements();)
            {
                AppAttributes attrib = (AppAttributes) attribs.nextElement();

                assertNotNull("Null AppAttributes should not be returned", attrib);
                assertNotNull("AppAttributes was unexpected for " + attrib.getIdentifier(),
                        all.remove(attrib.getIdentifier()));
            }
            // Any left-overs
            assertEquals("Some AppAttributess were not returned", 0, all.size());
        }

        public void testGetAppAttributesAppID()
        {
            for (int i = 0; i < dbs.length; ++i)
            {
                for (Enumeration keys = dbs[i].apps.keys(); keys.hasMoreElements();)
                {
                    AppID id = (AppID) keys.nextElement();
                    assertNotNull("Expected AppAttributes to be returned for " + id, db.getAppAttributes(id));
                }
            }
        }

        public void testGetAppProxy()
        {
            for (int i = 0; i < dbs.length; ++i)
            {
                for (Enumeration keys = dbs[i].apps.keys(); keys.hasMoreElements();)
                {
                    AppID id = (AppID) keys.nextElement();
                    assertNotNull("Expected AppProxy to be returned for " + id, db.getAppProxy(id));
                }
            }
        }

        private void doTestAddListener(Listener l) throws Exception
        {
            for (int dbi = 0; dbi < dbs.length; ++dbi)
            {
                AppID id = new AppID(0x2000, 0x2000);

                // ADD
                synchronized (l)
                {
                    l.clear();

                    dbs[dbi].add(id);
                    l.wait(5000);
                    assertEquals("Expected app to be added once", 1, l.added.size());
                    AppsDatabaseEvent e = (AppsDatabaseEvent) l.added.elementAt(0);

                    assertEquals("Expected APP_ADDED", AppsDatabaseEvent.APP_ADDED, e.getEventId());
                    assertSame("Expected composite db", db, e.getSource());
                    assertEquals("Expected added app id " + id, id, e.getAppID());

                    assertEquals("Expected no APP_CHANGED", 0, l.changed.size());
                    assertEquals("Expected no APP_REMOVED", 0, l.removed.size());
                    assertEquals("Expected no NEW_DB", 0, l.newDb.size());
                }

                // MODIFY
                synchronized (l)
                {
                    l.clear();

                    dbs[dbi].modify(id);
                    l.wait(5000);
                    assertEquals("Expected app to be changed once", 1, l.changed.size());
                    AppsDatabaseEvent e = (AppsDatabaseEvent) l.changed.elementAt(0);

                    assertEquals("Expected APP_CHANGED", AppsDatabaseEvent.APP_CHANGED, e.getEventId());
                    assertSame("Expected composite db", db, e.getSource());
                    assertEquals("Expected added app id " + id, id, e.getAppID());

                    assertEquals("Expected no APP_ADDED", 0, l.added.size());
                    assertEquals("Expected no APP_REMOVED", 0, l.removed.size());
                    assertEquals("Expected no NEW_DB", 0, l.newDb.size());
                }

                // DELETE
                synchronized (l)
                {
                    l.clear();

                    dbs[dbi].remove(id);
                    l.wait(5000);
                    assertEquals("Expected app to be removed once", 1, l.removed.size());
                    AppsDatabaseEvent e = (AppsDatabaseEvent) l.removed.elementAt(0);

                    assertEquals("Expected APP_DELETED", AppsDatabaseEvent.APP_DELETED, e.getEventId());
                    assertSame("Expected composite db", db, e.getSource());
                    assertEquals("Expected added app id " + id, id, e.getAppID());

                    assertEquals("Expected no APP_ADDED", 0, l.added.size());
                    assertEquals("Expected no APP_CHANGED", 0, l.changed.size());
                    assertEquals("Expected no NEW_DB", 0, l.newDb.size());
                }

                // NEWDB
                synchronized (l)
                {
                    l.clear();

                    dbs[dbi].newDb();
                    l.wait(5000);
                    assertEquals("Expected app to be notified of one newDB", 1, l.newDb.size());
                    AppsDatabaseEvent e = (AppsDatabaseEvent) l.newDb.elementAt(0);

                    assertEquals("Expected NEW_DATABASE", AppsDatabaseEvent.NEW_DATABASE, e.getEventId());
                    assertSame("Expected composite db", db, e.getSource());
                    assertNull("Expected null app id", e.getAppID());

                    assertEquals("Expected no APP_ADDED", 0, l.added.size());
                    assertEquals("Expected no APP_CHANGED", 0, l.changed.size());
                    assertEquals("Expected no APP_REMOVED", 0, l.removed.size());
                }
            }
        }

        public void testAddListener() throws Exception
        {
            // Cannot do much here...
            if (dbs.length == 0) return;

            Listener l = new Listener();
            db.addListener(l);

            doTestAddListener(l);
        }

        public void testRemoveListener() throws Exception
        {
            // Cannot do much here...
            if (dbs.length == 0) return;

            Listener l = new Listener();
            db.addListener(l);
            db.removeListener(l);

            for (int dbi = 0; dbi < dbs.length; ++dbi)
            {
                AppID id = new AppID(0x2000, 0x2001);

                synchronized (l)
                {
                    l.clear();

                    dbs[dbi].add(id);
                    dbs[dbi].modify(id);
                    dbs[dbi].remove(id);
                    dbs[dbi].newDb();
                    l.wait(200L);

                    assertEquals("Expected no APP_CHANGED", 0, l.changed.size());
                    assertEquals("Expected no APP_REMOVED", 0, l.removed.size());
                    assertEquals("Expected no NEW_DB", 0, l.newDb.size());
                    assertEquals("Expected no APP_ADDED", 0, l.added.size());
                }

            }
        }

        public void testAddListener_multiple() throws Exception
        {
            Listener[] listeners = { new Listener(), new Listener(), new Listener(), new Listener() };

            for (int i = 0; i < listeners.length; ++i)
            {
                // Add a new listener
                db.addListener(listeners[i]);

                for (int dbi = 0; dbi < dbs.length; ++dbi)
                {
                    // clear all
                    for (int j = 0; j <= i; ++j)
                        listeners[j].clear();

                    // send event
                    dbs[dbi].newDb();

                    // check each
                    for (int j = 0; j <= i; ++j)
                    {
                        synchronized (listeners[j])
                        {
                            if (listeners[j].newDb.size() == 0) listeners[j].wait(5000);
                            assertEquals("Expected notification of event", 1, listeners[j].newDb.size());
                        }
                    }
                    // Should not be notified
                    for (int j = i + 1; j < listeners.length; ++j)
                    {
                        assertEquals("Expected no notification of event", 0, listeners[j].newDb.size());
                    }
                }
            }
        }

        public void testAddListener_multipleSame() throws Exception
        {
            // Cannot do much here...
            if (dbs.length == 0) return;

            Listener l = new Listener();
            db.addListener(l);
            db.addListener(l);
            db.addListener(l);

            doTestAddListener(l);
        }

        /**
         * Tests adding same AppsDB multiple times (should act as if added only
         * once).
         */
        public void testAddAppsDatabase_multiple() throws Exception
        {
            int oldSize = db.size();
            Listener l = new Listener();
            db.addListener(l);

            // Add a new DB
            AppID[] apps = { new AppID(0x1000, 0x1000), new AppID(0x1000, 0x1001), };
            TestDB test = new TestDB(apps);

            for (int loop = 0; loop < 5; ++loop)
            {
                // Add this DB once (more)
                db.addAppsDatabase(test);

                // Check new size
                assertEquals("Unexpected new size", oldSize + apps.length, db.size());

                // Check enumerations
                Hashtable ids = new Hashtable();
                for (Enumeration e = db.getAppIDs(new CurrentServiceFilter()); e.hasMoreElements();)
                {
                    AppID id = (AppID) e.nextElement();
                    assertNull("Same AppID returned twice " + id, ids.get(id));
                    ids.put(id, id);
                }
                Hashtable attribs = new Hashtable();
                for (Enumeration e = db.getAppAttributes(new CurrentServiceFilter()); e.hasMoreElements();)
                {
                    AppAttributes attrib = (AppAttributes) e.nextElement();
                    AppID id = attrib.getIdentifier();
                    assertNull("Same AppAttributes returned twice " + id, attribs.get(id));
                    attribs.put(id, attrib);
                }
            }
        }

        /* ================ boilerplate ==================== */

        public static InterfaceTestSuite isuite()
        {
            InterfaceTestSuite suite = new InterfaceTestSuite(SubTest.class);
            suite.setName("Parameterized");
            return suite;
        }

        public SubTest(String name, ImplFactory f)
        {
            super(name, TestDB[].class, f);
        }

        protected TestDB[] getTestDBs()
        {
            return (TestDB[]) createImplObject();
        }

        protected TestDB[] dbs;

        protected CompositeAppsDB db;

        protected Hashtable all;

        protected void setUp() throws Exception
        {
            super.setUp();
            all = new Hashtable();
            dbs = getTestDBs();
            db = new CompositeAppsDB();
            for (int i = 0; i < dbs.length; ++i)
            {
                db.addAppsDatabase(dbs[i]); // start out
                addTo(all, dbs[i]);
            }
        }

        private void addTo(Hashtable allApps, TestDB appToAdd)
        {
            for (Enumeration apps = appToAdd.apps.elements(); apps.hasMoreElements();)
            {
                App app = (App) apps.nextElement();
                allApps.put(app.id, app);
            }
        }

        protected void tearDown() throws Exception
        {
            super.tearDown();
            all = null;
            dbs = null;
            db = null;
        }

    }

    public static class Factory implements ImplFactory
    {
        public Factory(String name, AppID[][] apps)
        {
            this.name = name;
            this.apps = apps;
        }

        public Object createImplObject() throws Exception
        {
            TestDB[] dbs = new TestDB[apps.length];
            for (int i = 0; i < apps.length; ++i)
            {
                dbs[i] = new TestDB(apps[i]);
            }
            return dbs;
        }

        public String toString()
        {
            return "Factory:" + name;
        }

        private AppID[][] apps;

        private String name;
    }

    /*
     * ==========================================================================
     * ===
     */

    public CompositeAppsDBTest(String name)
    {
        super(name);
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
        TestSuite suite = new TestSuite(CompositeAppsDBTest.class);

        InterfaceTestSuite isuite = SubTest.isuite();

        AppID[][] ids = { new AppID[] { new AppID(1, 1), new AppID(1, 2), new AppID(1, 3) },
                new AppID[] { new AppID(2, 1) }, new AppID[] { new AppID(2, 2), new AppID(1, 4), },
                new AppID[] { new AppID(3, 1), new AppID(1, 5), new AppID(1, 6) }, new AppID[0], };

        Factory[] f = {
                // no apps dbs
                new Factory("No Dbs", new AppID[0][]),
                // 1 empty db
                new Factory("1 empty Db", new AppID[][] { ids[4] }),
                // 1 non-empty db
                new Factory("1 non-empty Db", new AppID[][] { ids[0] }),
                // multiple dbs
                new Factory("Multiple Dbs.1", new AppID[][] { ids[1], ids[2], ids[3], }),
                new Factory("Multiple Dbs.2", new AppID[][] { ids[4], ids[2], ids[3], ids[4], }),
                // multiple all empty
                new Factory("Multiple Empty", new AppID[][] { ids[4], ids[4], ids[4], }), };

        isuite.addFactories(f);
        suite.addTest(isuite);

        return suite;
    }

    /*
     * ==========================================================================
     * ===
     */

    private static class Listener implements AppsDatabaseEventListener
    {
        Vector newDb = new Vector();

        Vector added = new Vector();

        Vector removed = new Vector();

        Vector changed = new Vector();

        synchronized void clear()
        {
            newDb.clear();
            added.clear();
            removed.clear();
            changed.clear();
        }

        private synchronized void newEvent(Vector v, AppsDatabaseEvent evt)
        {
            synchronized (v)
            {
                v.addElement(evt);
                v.notifyAll();
            }
            notifyAll();
        }

        public void entryAdded(AppsDatabaseEvent evt)
        {
            newEvent(added, evt);
        }

        public void entryChanged(AppsDatabaseEvent evt)
        {
            newEvent(changed, evt);
        }

        public void entryRemoved(AppsDatabaseEvent evt)
        {
            newEvent(removed, evt);
        }

        public void newDatabase(AppsDatabaseEvent evt)
        {
            newEvent(newDb, evt);
        }

    }

    private static class App implements DVBJProxy, OcapAppAttributes
    {
        final AppID id;

        App(AppID id)
        {
            this.id = id;
        }

        public void init()
        { /* empty */
        }

        public void load()
        { /* empty */
        }

        public void addAppStateChangeEventListener(AppStateChangeEventListener listener)
        { /* empty */
        }

        public void removeAppStateChangeEventListener(AppStateChangeEventListener listener)
        { /* empty */
        }

        public int getState()
        {
            return AppProxy.NOT_LOADED;
        }

        public void pause()
        { /* empty */
        }

        public void resume()
        { /* empty */
        }

        public void start()
        { /* empty */
        }

        public void start(String[] args)
        { /* empty */
        }

        public void stop(boolean forced)
        { /* empty */
        }

        public int getApplicationControlCode()
        {
            return OcapAppAttributes.PRESENT;
        }

        public int getStoragePriority()
        {
            return 0;
        }

        public boolean hasNewVersion()
        {
            return false;
        }

        public boolean isNewVersionSignaled()
        {
            // if hasNewVersion returns false, then a new version can't have
            // been stored,
            // so return false.
            return false;
        }

        public AppIcon getAppIcon()
        {
            return null;
        }

        public AppID getIdentifier()
        {
            return id;
        }

        public boolean getIsServiceBound()
        {
            return true;
        }

        public String getName()
        {
            return "app:" + id;
        }

        public String getName(String iso639code) throws LanguageNotAvailableException
        {
            return "app:" + id;
        }

        public String[][] getNames()
        {
            return new String[][] { { "app:" + id } };
        }

        public int getPriority()
        {
            return 100;
        }

        public String[] getProfiles()
        {
            return null;
        }

        public Object getProperty(String index)
        {
            return null;
        }

        public Locator getServiceLocator()
        {
            return null;
        }

        public int getType()
        {
            return AppAttributes.DVB_J_application;
        }

        public int[] getVersions(String profile) throws IllegalProfileParameterException
        {
            return null;
        }

        public boolean isStartable()
        {
            return true;
        }

        public boolean isVisible()
        {
            return true;
        }

        public int getApplicationMode()
        {
            // TODO Auto-generated method stub
            return 0;
        }
    }

    private static class TestDB extends AppsDatabase
    {
        public Hashtable apps = new Hashtable();

        private Vector listeners = new Vector();

        public boolean addNew = false;

        public TestDB()
        {
            addNew = true;
        }

        public TestDB(AppID[] apps)
        {
            for (int i = 0; i < apps.length; ++i)
                add(apps[i]);
            addNew = true;
        }

        public TestDB(App[] apps)
        {
            for (int i = 0; i < apps.length; ++i)
                add(apps[i]);
            addNew = true;
        }

        void add(App app)
        {
            synchronized (apps)
            {
                boolean isNew = !apps.containsKey(app.id);
                apps.put(app.id, app);

                if (addNew)
                {
                    if (isNew)
                        notifyListeners(new AppsDatabaseEvent(AppsDatabaseEvent.APP_ADDED, app.id, this));
                    else
                        notifyListeners(new AppsDatabaseEvent(AppsDatabaseEvent.APP_CHANGED, app.id, this));
                }
            }
        }

        void add(AppID id)
        {
            add(new App(id));
        }

        void remove(App app)
        {
            remove(app.id);
        }

        void remove(AppID key)
        {
            if (apps.remove(key) != null)
            {
                notifyListeners(new AppsDatabaseEvent(AppsDatabaseEvent.APP_DELETED, key, this));
            }
        }

        void modify(AppID app)
        {
            notifyListeners(new AppsDatabaseEvent(AppsDatabaseEvent.APP_CHANGED, app, this));
        }

        void newDb()
        {
            notifyListeners(new AppsDatabaseEvent(AppsDatabaseEvent.NEW_DATABASE, null, this));
        }

        void notifyListeners(AppsDatabaseEvent e)
        {
            AppsDatabaseEventListener[] array;
            synchronized (listeners)
            {
                array = new AppsDatabaseEventListener[listeners.size()];
                listeners.copyInto(array);
            }
            for (int i = 0; i < array.length; ++i)
            {
                switch (e.getEventId())
                {
                    case AppsDatabaseEvent.APP_ADDED:
                        array[i].entryAdded(e);
                        break;
                    case AppsDatabaseEvent.APP_DELETED:
                        array[i].entryRemoved(e);
                        break;
                    case AppsDatabaseEvent.APP_CHANGED:
                        array[i].entryChanged(e);
                        break;
                    case AppsDatabaseEvent.NEW_DATABASE:
                        array[i].newDatabase(e);
                        break;
                }
            }
        }

        public void addListener(AppsDatabaseEventListener listener)
        {
            synchronized (listeners)
            {
                listeners.removeElement(listener);
                listeners.addElement(listener);
            }
        }

        public void removeListener(AppsDatabaseEventListener listener)
        {
            listeners.removeElement(listener);
        }

        public AppAttributes getAppAttributes(AppID key)
        {
            return (AppAttributes) apps.get(key);
        }

        public Enumeration getAppAttributes(AppsDatabaseFilter filter)
        {
            return apps.elements();
        }

        public Enumeration getAppIDs(AppsDatabaseFilter filter)
        {
            return apps.keys();
        }

        public AppProxy getAppProxy(AppID key)
        {
            return (AppProxy) apps.get(key);
        }

        public int size()
        {
            return apps.size();
        }
    }
}
