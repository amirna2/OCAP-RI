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

package org.cablelabs.impl.manager;

import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.dvb.application.AppAttributes;
import org.dvb.application.AppID;
import org.dvb.application.AppIcon;
import org.dvb.application.AppsDatabase;
import org.dvb.application.AppsDatabaseFilter;
import org.ocap.application.AppManagerProxy;
import org.ocap.resource.ApplicationResourceUsage;
import org.ocap.resource.ResourceContentionHandler;
import org.ocap.resource.ResourceContentionManager;
import org.ocap.resource.ResourceUsage;
import org.ocap.service.ServiceContextResourceUsage;

import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.util.TaskQueue;

/**
 * Tests ResourceManager implementations.
 * 
 * <p>
 * Takes advantage of the ManagerManager.updateManager() method that exists
 * solely for testing. It is used to replace the ApplicationManager with one
 * suitable for testing.
 * 
 * @author Aaron Kamienski
 */
public class ResourceManagerTest extends ManagerTest
{
    /**
     * Tests getContentionManager().
     */
    public void testGetContentionManager()
    {
        assertNotNull("ContentionManager should not be null", rezmgr.getContentionManager());
    }

    /**
     * Tests isReservationAllowed().
     */
    public void testIsReservationAllowed()
    {
        ResourceContentionManager rcm = rezmgr.getContentionManager();

        class Filter extends AppsDatabaseFilter
        {
            public boolean ret = true;

            public boolean called = false;

            public AppID id = null;

            public void reset(boolean ret)
            {
                this.ret = ret;
                this.called = false;
                this.id = null;
            }

            public boolean accept(AppID id)
            {
                called = true;
                this.id = id;

                return ret;
            }
        }

        try
        {
            Filter filters[] = new Filter[RESOURCES.length];
            AppID id = new AppID(1, 2);
            ResourceManager.Client client = new ResourceManager.Client(new DummyClient(), new Proxy(),
                    new ResourceUsageImpl(id, -1), new Context(id));

            // Call with null filters
            for (int i = 0; i < RESOURCES.length; ++i)
            {
                assertTrue("With no filters, reservation should be allowed", rezmgr.isReservationAllowed(client,
                        RESOURCES[i]));
            }

            // Add all filters
            for (int i = 0; i < RESOURCES.length; ++i)
            {
                filters[i] = new Filter();
                rcm.setResourceFilter(filters[i], RESOURCES[i]);
            }

            // Check that the right filter is called
            for (int i = 0; i < RESOURCES.length; ++i)
            {
                for (int bool = 0; bool < 2; ++bool)
                {
                    boolean test = bool != 0;
                    filters[i].reset(test);

                    boolean ret = rezmgr.isReservationAllowed(client, RESOURCES[i]);
                    assertEquals("Incorrect value returned", test, ret);
                    for (int j = 0; j < RESOURCES.length; ++j)
                    {
                        if (j == i)
                            assertTrue("Filter should've been called", filters[j].called);
                        else
                            assertFalse("Other filters should not have been called", filters[j].called);
                    }
                    assertNotNull("AppID should've been passed, not null", filters[i].id);
                    assertEquals("Equiv appid should've been passed", id, filters[i].id);

                }
                filters[i].reset(false);
            }

            // Replace filters
            Filter replace[] = new Filter[RESOURCES.length];
            for (int i = 0; i < RESOURCES.length; ++i)
            {
                replace[i] = new Filter();
                rcm.setResourceFilter(replace[i], RESOURCES[i]);
            }

            // Check that new filters are called, not the old ones
            for (int i = 0; i < RESOURCES.length; ++i)
            {
                filters[i].reset(false);
                replace[i].reset(false);

                boolean ret = rezmgr.isReservationAllowed(client, RESOURCES[i]);
                assertFalse("Expected filter to reject request", ret);
                assertTrue("Expected replacement filter to be called", replace[i].called);
                assertFalse("Expected old filter not to be called", filters[i].called);
            }
            filters = replace;

            // Clear filters
            for (int i = 0; i < RESOURCES.length; ++i)
            {
                rcm.setResourceFilter(null, RESOURCES[i]);
            }

            // Check that no filter is called
            for (int i = 0; i < RESOURCES.length; ++i)
            {
                filters[i].reset(false);

                boolean ret = rezmgr.isReservationAllowed(client, RESOURCES[i]);
                assertTrue("Expected reservation to be allowed given no filter", ret);
                for (int j = 0; j < RESOURCES.length; ++j)
                {
                    if (j == i)
                        assertFalse("Filter should've been removed and not called", filters[i].called);
                    else
                        assertFalse("Other filter should not be called", filters[i].called);
                }
            }
        }
        finally
        {
            // Clear any filters
            for (int i = 0; i < RESOURCES.length; ++i)
                rcm.setResourceFilter(null, RESOURCES[i]);
        }
    }

    /**
     * Tests negotiateRelease().
     */
    public void XtestNegotiateRelease()
    {
        fail("Unimplemented test");
    }

    private static int position(ResourceManager.Client[] list, ResourceManager.Client cl)
    {
        int pos = 0;
        for (; pos < list.length; ++pos)
            if (list[pos] == cl) break;
        return pos;
    }

    /**
     * Tests prioritizeContention(). Uses AppManagerProxyTest code to fill the
     * applications database such that default prioritization makes sense.
     * 
     * @todo This test could be refactored to be MUCH cleaner. It's much too
     *       long and a bunch of code is duplicated!
     */
    public void testPrioritizeContention() throws Exception
    {
        ResourceContentionManager rcm = rezmgr.getContentionManager();

        // Handler impl used for testing...
        class Handler implements ResourceContentionHandler
        {
            public boolean called = false;

            public int ret = 1;

            public ResourceUsage req = null;

            public ResourceUsage[] own = null;

            public String proxy = null;

            public void reset(int ret)
            {
                this.ret = ret;
                called = false;
                req = null;
                own = null;
                proxy = null;
            }

            /**
             * Returns based on value of <i>ret</i>.
             * <ol>
             * <li>-1 then null
             * <li>0 then [0]
             * <li>1 then [owners.length] with requester at front
             * </ol>
             */
            public ResourceUsage[] resolveResourceContention(ResourceUsage requester, ResourceUsage owners[])
            {
                called = true;
                req = requester;
                own = owners;

                if (ret == -1)
                    return null;
                else if (ret == 0)
                    return new ResourceUsage[0];
                else
                {
                    ResourceUsage[] prior = new ResourceUsage[owners.length];
                    prior[0] = requester;
                    System.arraycopy(owners, 0, prior, 1, owners.length - 1);
                    return prior;
                }
            }

            public void resourceContentionWarning(ResourceUsage newRequest, ResourceUsage[] currentReservations)
            {
                // does nothing for now
            }

        }
        ResourceManager.Client client = new ResourceManager.Client(new DummyClient(), new Proxy(),
                new ResourceUsageImpl(new AppID(1, 4), 1), new Context(new AppID(1, 4)));
        ResourceManager.Client clients[] = {
                new ResourceManager.Client(new DummyClient(), new Proxy(), new ResourceUsageImpl(new AppID(2, 2), 2),
                        new Context(new AppID(2, 2))),
                new ResourceManager.Client(new DummyClient(), new Proxy(), new ResourceUsageImpl(new AppID(1, 1), 1),
                        new Context(new AppID(1, 1))),
                new ResourceManager.Client(new DummyClient(), new Proxy(), new ResourceUsageImpl(new AppID(4, 4), 4),
                        new Context(new AppID(4, 4))),
                new ResourceManager.Client(new DummyClient(), new Proxy(), new ResourceUsageImpl(new AppID(3, 3), 3),
                        new Context(new AppID(3, 3))),
                new ResourceManager.Client(new DummyClient(), new Proxy(), new ResourceUsageImpl(new AppID(2, 2), 2),
                        new Context(new AppID(2, 2))),
                new ResourceManager.Client(new DummyClient(), new Proxy(), new ResourceUsageImpl(new AppID(4, 5), 4),
                        new Context(new AppID(4, 5))),
                new ResourceManager.Client(new DummyClient(), new Proxy(), new ResourceUsageImpl(new AppID(5, 5), 5),
                        new Context(new AppID(5, 5))),
                new ResourceManager.Client(new DummyClient(), new Proxy(), new ResourceUsageImpl(new AppID(4, 3), 4),
                        new Context(new AppID(4, 3))), };
        ResourceUsage usages[] = new ResourceUsage[clients.length];
        for (int i = 0; i < clients.length; ++i)
            usages[i] = clients[i].resusage;

        try
        {
            ResourceManager.Client[] prioritized;

            // Call with null handler
            int r = 0;
            prioritized = rezmgr.prioritizeContention(client, clients);
            assertNotNull("Should not return null (given no handler)", prioritized);
            assertEquals("Unexpected array size (given no handler)", clients.length + 1, prioritized.length);
            int last = 256;
            for (int i = 0; i < prioritized.length; ++i)
                System.out.println(((Context) prioritized[i].context).priority + " "
                        + position(clients, prioritized[i]));
            for (int i = 0; i < prioritized.length; ++i)
            {
                int prior = prioritized[i].getUsagePriority();
                assertTrue("Expected sorted by priority (given no handler) [" + i + "]", last >= prior);
                // If same priority as last, verify that ordering is correct
                // Current definition is to go based upon original order in
                // owner list
                if (last == prior)
                {
                    assertTrue("Expected equal priority to maintin existing order",
                            position(clients, prioritized[i]) > position(clients, prioritized[i - 1]));
                }

                last = prior;
            }

            // Add Handler
            Handler handler = new Handler();
            rcm.setResourceContentionHandler(handler);

            // Expect manager to prioritize
            handler.reset(-1);
            r = 1;
            prioritized = rezmgr.prioritizeContention(client, clients);
            assertTrue("Expected handler to be called", handler.called);
            assertSame("Expected given requester to be used", client.resusage, handler.req);
            assertNotNull("Expected owners array to be non-null", handler.own);
            assertEquals("Expected owners length to be same as specified", clients.length, handler.own.length);
            for (int i = 0; i < clients.length; ++i)
                assertEquals("Unexpected owner id passed to handler", usages[i], handler.own[i]);
            assertNotNull("Should not return null (given null)", prioritized);
            assertEquals("Unexpected array size (given null)", clients.length + 1, prioritized.length);
            last = 256;
            for (int i = 0; i < prioritized.length; ++i)
            {
                int prior = prioritized[i].getUsagePriority();
                assertTrue("Expected sorted by priority (given null) [" + i + "]", last >= prior);
                last = prior;
            }

            // Expect empty array
            handler.reset(0);
            r = 2;
            prioritized = rezmgr.prioritizeContention(client, clients);
            assertTrue("Expected handler to be called", handler.called);
            assertSame("Expected given requester to be used", client.resusage, handler.req);
            assertNotNull("Expected owners array to be non-null", handler.own);
            assertEquals("Expected owners length to be same as specified", clients.length, handler.own.length);
            for (int i = 0; i < clients.length; ++i)
                assertEquals("Unexpected owner id passed to handler", usages[i], handler.own[i]);
            assertNotNull("Should not return null (given 0)", prioritized);
            assertEquals("Unexpected array size (given 0)", 0, prioritized.length);

            // Expect specified array
            handler.reset(1);
            r = 3;
            prioritized = rezmgr.prioritizeContention(client, clients);
            assertTrue("Expected handler to be called", handler.called);
            assertSame("Expected given requester to be used", client.resusage, handler.req);
            assertNotNull("Expected owners array to be non-null", handler.own);
            assertEquals("Expected owners length to be same as specified", clients.length, handler.own.length);
            for (int i = 0; i < clients.length; ++i)
                assertEquals("Unexpected owner id passed to handler", usages[i], handler.own[i]);
            assertNotNull("Should not return null", prioritized);
            assertEquals("Unexpected array size", clients.length, prioritized.length);
            assertEquals("Unexpected array entry [0]", client, prioritized[0]);
            // Actually, just expect to be sorted by AppID
            // When we have same AppID, might be reordered...
            // Let's assign priority numbers to each AppID...
            // And then make sure that clients are sorted accordingly
            // !!! Minor issue here... is if AppID is represented more than once
            // in array returned by handler... which one is used to specify
            // priority?
            // First position or last? Here we assume last.
            // TODO (TomH) Resolve with new resource contention
            /*
             * Hashtable idprior = new Hashtable(); for(int i = 0; i <
             * handler.own.length; ++i) idprior.put(handler.own[i].getAppID(),
             * new Integer(handler.own.length-i)); last = handler.own.length+1;
             * for(int i = 1; i < prioritized.length; ++i) { Integer p =
             * (Integer)idprior.get(((Context)prioritized[i].context).id);
             * assertNotNull("AppID wasn't passed to handler to begin with", p);
             * int prior = p.intValue();
             * assertTrue("Expected clients to be in priority order", last >=
             * prior); last = prior; }
             */

            // Replace the handler
            Handler replace = new Handler();
            rcm.setResourceContentionHandler(replace);
            handler.reset(0);
            replace.reset(0);
            prioritized = rezmgr.prioritizeContention(client, clients);
            assertTrue("Replacement handler should be called", replace.called);
            assertFalse("Original handler should NOT be called", handler.called);
            handler = replace;

            // Check for empty owners
            handler.reset(-1);
            prioritized = rezmgr.prioritizeContention(client, new ResourceManager.Client[0]);
            assertTrue("Expected handler to be called", handler.called);
            assertTrue("Expected empty owners", handler.own.length == 0);
            assertNotNull("Should not return null (no owners)", prioritized);
            assertEquals("Unexpected array size (no owners)", 1, prioritized.length);
            assertEquals("Unexpected entry (no owners)", client, prioritized[0]);

            // Remove the handler
            rcm.setResourceContentionHandler(null);
            handler.reset(0);
            prioritized = rezmgr.prioritizeContention(client, clients);
            assertFalse("Handler was removed, should not be called", handler.called);
            assertNotNull("Should not return null (removed)", prioritized);
            assertEquals("Unexpected array size (removed)", clients.length + 1, prioritized.length);
            last = 256;
            for (int i = 0; i < prioritized.length; ++i)
            {
                int prior = prioritized[i].getUsagePriority();
                assertTrue("Expected sorted by priority (removed) [" + i + "]", last >= prior);
                last = prior;
            }
        }
        finally
        {
            // Clear handler
            rcm.setResourceContentionHandler(null);
        }

    }

    /*
     * public void testCreateResourceUsage() throws Exception { CallerContext
     * context = new Context(new AppID(1,1));
     * 
     * ResourceUsage usage = rezmgr.createResourceUsage(context);
     * assertNotNull("createResourceUsage() should not return null", usage);
     * assertTrue(
     * "createResourceUsage(CallerContext) should return an instance of ApplicationResourceUsage"
     * , usage instanceof ApplicationResourceUsage);
     * 
     * usage = rezmgr.createResourceUsage(context, ResourceUsage.class, null);
     * assertNotNull("createResourceUsage() should not return null", usage);
     * 
     * usage = rezmgr.createResourceUsage(context,
     * ServiceContextResoudrceUsage.class, null);
     * assertNotNull("createResourceUsage() should not return null", usage);
     * assertTrue(
     * "createResourceUsage(ServiceContextResourceUsage) should return an instance of ServiceContextResourceUsage"
     * , usage instanceof ServiceContextResourceUsage); assertFalse(
     * "createResourceUsage(ServiceContextResourceUsage) should not return an instance of ApplicationResourceUsage"
     * , usage instanceof ApplicationResourceUsage);
     * 
     * usage = rezmgr.createResourceUsage(context,
     * ApplicationResourceUsage.class, null); assertNotNull(
     * "createResourceUsage(ApplicationResourceUsage) should not return null",
     * usage); assertTrue(
     * "createResourceUsage(ApplicationResourceUsage) should return an instance of ApplicationResourceUsage"
     * , usage instanceof ApplicationResourceUsage); assertFalse(
     * "createResourceUsage(ApplicationResourceUsage) should not return an instance of ServiceContextResourceUsage"
     * , usage instanceof ServiceContextResourceUsage); }
     */

    private static final String[] RESOURCES = org.ocap.resource.ResourceContentionManagerTest.RESOURCES;

    /* Boilerplate */

    public static InterfaceTestSuite isuite()
    {
        InterfaceTestSuite suite = new InterfaceTestSuite(ResourceManagerTest.class);
        suite.setName(ResourceManager.class.getName());
        return suite;
    }

    public ResourceManagerTest(String name, ImplFactory f)
    {
        super(name, ResourceManager.class, f);
    }

    protected ResourceManager createResourceManager()
    {
        return (ResourceManager) createManager();
    }

    private ResourceManager rezmgr;

    protected void setUp() throws Exception
    {
        super.setUp();
        replaceAppMgr();
        rezmgr = createResourceManager();
    }

    protected void tearDown() throws Exception
    {
        rezmgr = null;
        restoreAppMgr();
        super.tearDown();
    }

    private ApplicationManager save;

    private void replaceAppMgr()
    {
        save = (ApplicationManager) ManagerManager.getInstance(ApplicationManager.class);
        ManagerManager.updateManager(ApplicationManager.class, AppMgr.class, false, new AppMgr());
    }

    private void restoreAppMgr()
    {
        ManagerManager.updateManager(ApplicationManager.class, save.getClass(), true, save);
    }

    static public class DummyClient implements ResourceClient
    {
        public boolean answer = true;

        public ResourceProxy proxy;

        public Object data;

        public boolean requestCalled;

        public boolean releaseCalled;

        public boolean notifyCalled;

        public void reset(boolean answer)
        {
            this.answer = answer;
            proxy = null;
            data = null;
            requestCalled = false;
            releaseCalled = false;
            notifyCalled = false;
        }

        public boolean requestRelease(ResourceProxy proxy, Object data)
        {
            requestCalled = true;
            this.proxy = proxy;
            this.data = data;
            return answer;
        }

        public void release(ResourceProxy proxy)
        {
            releaseCalled = true;
            this.proxy = proxy;
        }

        public void notifyRelease(ResourceProxy proxy)
        {
            notifyCalled = true;
            this.proxy = proxy;
        }
    }

    static public class Context implements CallerContext
    {
        public Context(AppID id)
        {
            this.id = id;
            this.priority = id.getOID();
        }

        public Context(AppID id, int priority)
        {
            this.id = id;
            this.priority = priority;
        }

        public final AppID id;

        public final int priority;

        public void addCallbackData(CallbackData data, Object key)
        {
        }

        public void removeCallbackData(Object key)
        {
        }

        public CallbackData getCallbackData(Object key)
        {
            return null;
        }

        public boolean isAlive()
        {
            return true;
        }

        public void checkAlive()
        {
        }

        public boolean isActive()
        {
            return true;
        }

        public void runInContext(Runnable run)
        {
            run.run();
        }

        public void runInContextSync(Runnable run)
        {
            run.run();
        }

        public void runInContextAsync(Runnable run)
        {
            run.run();
        }

        public void runAsContext(Runnable run)
        {
            throw new RuntimeException("Should not be used");
        }

        public Object get(Object key)
        {
            if (key == APP_ID)
            {
                return id;
            }
            else if (key == APP_PRIORITY)
            {
                return new Integer(priority);
            }

            throw new UnsupportedOperationException();
        }

        public TaskQueue createTaskQueue()
        {
            throw new UnsupportedOperationException();
        }

        public void runInContextAWT(Runnable run) throws SecurityException,
                IllegalStateException
        {
        }
    }

    /** Placeholder proxy implementation for testing. */
    static public class Proxy implements ResourceProxy
    {
        public ResourceClient getClient()
        {
            return null;
        }
    }

    /**
     * Replacement ApplicationManager so we can affect the AppID returned for a
     * CallerContext.
     * <p>
     * Assumes that implementation only use getAppAttributes() and then
     * getIdentifier()/getPriority() from an attributes.
     */
    public static class AppMgr implements ApplicationManager
    {
        public static Manager getInstance()
        {
            return new AppMgr();
        }

        public void destroy()
        {
        }

        public AppManagerProxy getAppManagerProxy()
        {
            return null;
        }

        public org.ocap.system.RegisteredApiManager getRegisteredApiManager()
        {
            return null;
        }

        public AppsDatabase getAppsDatabase()
        {
            return null;
        }

        public ClassLoader getAppClassLoader(CallerContext ctx)
        {
            return null;
        }

        public void registerResidentApp(java.io.InputStream in)
        {
        }

        public org.cablelabs.impl.manager.AppDomain createAppDomain(javax.tv.service.selection.ServiceContext sc)
        {
            return null;
        }

        public org.ocap.application.OcapAppAttributes createAppAttributes(org.cablelabs.impl.signalling.AppEntry entry,
                javax.tv.service.Service service)
        {
            return null;
        }

        public boolean purgeLowestPriorityApp(long x, long y, boolean urgent)
        {
            return false;
        }

        public int getRuntimePriority(AppID id)
        {
            return 0;
        }

        public AppAttributes getAppAttributes(final CallerContext ctx)
        {
            if (!(ctx instanceof Context))
                return null;
            else
                return new AppAttributes()
                {
                    public AppID getIdentifier()
                    {
                        return ((Context) ctx).id;
                    }

                    public int getPriority()
                    {
                        return ((Context) ctx).priority;
                    }

                    private void die()
                    {
                        fail("Unimplemented - not expected to be called");
                    }

                    public int getType()
                    {
                        die();
                        return 0;
                    }

                    public String getName()
                    {
                        die();
                        return null;
                    }

                    public String getName(String iso)
                    {
                        die();
                        return null;
                    }

                    public String[][] getNames()
                    {
                        die();
                        return null;
                    }

                    public String[] getProfiles()
                    {
                        die();
                        return null;
                    }

                    public int[] getVersions(String profile)
                    {
                        die();
                        return null;
                    }

                    public boolean getIsServiceBound()
                    {
                        die();
                        return false;
                    }

                    public boolean isStartable()
                    {
                        die();
                        return false;
                    }

                    public AppIcon getAppIcon()
                    {
                        die();
                        return null;
                    }

                    public org.davic.net.Locator getServiceLocator()
                    {
                        die();
                        return null;
                    }

                    public Object getProperty(String key)
                    {
                        die();
                        return null;
                    }

                    public boolean isVisible()
                    {
                        die();
                        return false;
                    }
                };
        }

        public AppEntry getRunningVersion(AppID id)
        {
            return null;
        }
    }
}
