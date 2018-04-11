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

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManagerTest;
import org.cablelabs.impl.manager.ManagerTest.ManagerFactory;
import org.cablelabs.impl.manager.application.AppClassLoaderTest.DummyApp;
import org.cablelabs.impl.manager.application.XletAppTest.DummyJTVXlet;
import org.cablelabs.impl.service.ServicesDatabase.BootProcessCallback;
import org.cablelabs.impl.service.ServicesDatabase.ShutdownCallback;
import org.cablelabs.impl.signalling.AppEntry;

import java.util.Hashtable;

import javax.tv.locator.Locator;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestor;
import javax.tv.service.Service;
import javax.tv.service.ServiceType;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.dvb.application.AppID;
import org.dvb.application.AppProxy;
import org.dvb.application.AppStateChangeEvent;
import org.dvb.application.AppStateChangeEventListener;
import org.ocap.application.OcapAppAttributes;
import org.ocap.net.OcapLocator;

/**
 * Tests CCMgr.
 * 
 * @author Aaron Kamienski
 */
public class CCMgrTest extends TestCase
{

    public void testCreateInstance()
    {
        AppID id = new AppID(27, 33);

        assertFalse("Did not expect context to be active yet", ccMgr.isActive(id));

        AppEntry entry = new AppEntry();
        entry.controlCode = OcapAppAttributes.PRESENT;
        entry.id = id;
        AppDomainImpl domain = new DummyDomain(entry);
        XletApp app = new DummyApp(entry, domain);
        AppContext acc = ccMgr.createInstance(app, id, domain);

        try
        {
            assertNotNull("Expected createInstance() to return an instance", acc);
            assertNull("Expected another call to not return an instance", ccMgr.createInstance(app, id, domain));
        }
        finally
        {
            ccMgr.deactivate(acc, id);
            if (acc != null) acc.notifyDestroyed();
        }

        acc = ccMgr.createInstance(app, id, domain);
        try
        {
            assertNotNull("Expected createInstance() to return an instance", acc);
        }
        finally
        {
            if (acc != null) acc.notifyDestroyed();
        }

    }

    public void testIsActive_Deactivate()
    {
        AppID id = new AppID(27, 34);

        assertFalse("Did not expect context to be active yet", ccMgr.isActive(id));

        AppEntry entry = new AppEntry();
        entry.controlCode = OcapAppAttributes.PRESENT;
        entry.id = id;
        AppDomainImpl domain = new DummyDomain(entry);
        XletApp app = new DummyApp(entry, domain);
        AppContext acc = ccMgr.createInstance(app, id, domain);
        try
        {
            assertTrue("Expected context to be active for given id", ccMgr.isActive(id));
        }
        finally
        {
            ccMgr.deactivate(acc, id);
            if (acc != null) acc.notifyDestroyed();
        }

        assertFalse("Expected context to no longer be active", ccMgr.isActive(id));
    }

    public void testShutdownApps() throws Exception
    {
        // Listener used to watch for app startup
        class Listener implements AppStateChangeEventListener
        {
            int count;

            int failed;

            int started;

            public synchronized void stateChange(AppStateChangeEvent evt)
            {
                if (evt.hasFailed() == true)
                {
                    ++count;
                    ++failed;
                    notifyAll();
                }
                else if (evt.getToState() == AppProxy.STARTED)
                {
                    ++count;
                    ++started;
                    notifyAll();
                }
            }
        }
        Listener listener = new Listener();
        DummyDomain domain = new DummyDomain();

        // Create and launch multiple apps
        XletApp[] apps = new XletApp[10];
        for (int i = 0; i < apps.length; ++i)
        {
            AppEntry entry = new AppEntry();
            entry.id = new AppID(i, 0x4000);
            entry.version = 1;
            entry.parameters = new String[0];
            entry.baseDirectory = XletAppTest.fs.mount;
            entry.className = DummyJTVXlet.class.getName();
            entry.classPathExtension = new String[0];
            entry.controlCode = OcapAppAttributes.PRESENT;
            entry.transportProtocols = new AppEntry.TransportProtocol[] { new AppEntry.LocalTransportProtocol() };
            domain.entries.put(entry.id, entry);
            apps[i] = new XletApp(entry, domain)
            {
                AppContext createAppContext(AppID appId, AppDomainImpl appDomain, CallerContext requestor)
                {
                    return ccMgr.createInstance(this, appId, appDomain);
                }
            };
            apps[i].addAppStateChangeEventListener(listener);

            apps[i].start();
        }

        // Wait for apps to start
        synchronized (listener)
        {
            for (int curr = -1; curr != listener.count && listener.count < apps.length;)
            {
                curr = listener.count;
                listener.wait(20000);
            }
        }
        assertEquals("Expected all apps to be started", apps.length, listener.count);
        assertEquals("Expected no apps to fail", apps.length, listener.started);

        // Verify that apps are running
        for (int i = 0; i < apps.length; ++i)
            assertTrue("Expected app to be running", ccMgr.isActive(new AppID(i, 0x4000)));

        // Invoked shutdownApps
        class Callback implements ShutdownCallback
        {
            BootProcessCallback done;

            public synchronized void complete(BootProcessCallback act)
            {
                done = act;
                notifyAll();
            }
        }
        Callback callback = new Callback();
        BootProcessCallback act = new BootProcessCallback()
        {
            public void monitorApplicationStarted()
            {
            }

            public void initialUnboundAutostartApplicationsStarted()
            {
            }

            public boolean monitorApplicationShutdown(ShutdownCallback callback)
            {
                return false;
            }
        };

        // Wait for shutdown to complete
        synchronized (callback)
        {
            assertTrue("Expected shutdownApps() to return true indicating asynchronous completion", ccMgr.shutdownApps(
                    callback, act));

            // During Expect createInstance to fail!
            AppID id = new AppID(29, 33);
            assertFalse("Did not expect context to be active yet", ccMgr.isActive(id));
            AppEntry entry = new AppEntry();
            entry.controlCode = OcapAppAttributes.PRESENT;
            entry.id = id;
            XletApp app = new DummyApp(entry, domain);
            assertNull("Expected createInstance to fail", ccMgr.createInstance(app, id, domain));

            // Verify callback of ShutdownComplete
            callback.wait(20000);
        }
        assertNotNull("Expected complete to have been called", callback.done);
        assertSame("Expected callback to have been called with given act", act, callback.done);

        // Verify that apps aren't running
        for (int i = 0; i < apps.length; ++i)
            assertFalse("Expected app to have been shutdown", ccMgr.isActive(new AppID(i, 0x4000)));

        // Finally, should be able to create an app
        testCreateInstance();
    }

    public void testGetSetCache()
    {
        SystemContext cc1 = new SystemContext(ccMgr, new ThreadPool("sys1", null, 1, Thread.MAX_PRIORITY), Thread.MAX_PRIORITY);
        SystemContext cc2 = new SystemContext(ccMgr, new ThreadPool("sys2", null, 1, Thread.MAX_PRIORITY), Thread.MAX_PRIORITY);

        try
        {
            ccMgr.setCache(cc1);
            assertSame("Expected getCache to return same", cc1, ccMgr.getCache());
            assertSame("Expected getCurrentContext to return cache", cc1, ccMgr.getCurrentContext());

            ccMgr.setCache(cc2);
            assertSame("Expected getCache to return same", cc2, ccMgr.getCache());
            assertSame("Expected getCurrentContext to return cache", cc2, ccMgr.getCurrentContext());
        }
        finally
        {
            cc1.dispose();
            cc2.dispose();
        }
    }

    public static class DummyDomain extends AppDomainImpl
    {
        public Service svc = new Service()
        {

            public SIRequest retrieveDetails(SIRequestor requestor)
            {
                return null;
            }

            public String getName()
            {
                return "";
            }

            public boolean hasMultipleInstances()
            {
                return false;
            }

            public ServiceType getServiceType()
            {
                return ServiceType.UNKNOWN;
            }

            public Locator getLocator()
            {
                try
                {
                    return new OcapLocator("ocap://n=unknown");
                }
                catch (Exception e)
                {
                    return null;
                }
            }
        };

        public Hashtable entries = new Hashtable();

        public DummyDomain()
        {
            super(null, null);
        }

        public DummyDomain(AppEntry entry)
        {
            this();
            entries.put(entry.id, entry);
        }

        AppEntry getAppEntry(AppID id)
        {
            return (AppEntry) entries.get(id);
        }

        Service getCurrentService()
        {
            return svc;
        }

    }

    protected CCMgr ccMgr;

    protected void tearDown() throws Exception
    {
        ccMgr.destroy();
        super.tearDown();
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        ccMgr = new CCMgr();
    }

    public CCMgrTest(String test)
    {
        super(test);
    }

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(CCMgrTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new CCMgrTest(tests[i]));
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

    public static Test suite()
    {
        TestSuite suite = new TestSuite(CCMgrTest.class);
        ImplFactory factory = new ManagerFactory()
        {
            public Object createImplObject()
            {
                return new CCMgr();
            }

            public void destroyImplObject(Object obj)
            {
                ((CCMgr) obj).destroy();
            }
        };
        InterfaceTestSuite ctxSuite = CallerContextManagerTest.isuite();

        ctxSuite.addFactory(factory);
        suite.addTest(ctxSuite);

        return suite;
    }
}
