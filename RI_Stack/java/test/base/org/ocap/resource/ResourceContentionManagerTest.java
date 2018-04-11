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

package org.ocap.resource;

import org.cablelabs.test.ProxySecurityManager;
import org.cablelabs.test.TestUtils;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.security.Permission;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestCase;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.dvb.application.AppID;
import org.dvb.application.AppsDatabaseFilter;
import org.dvb.application.AppProxyTest.DummySecurityManager;
import org.ocap.application.AppFilter;
import org.ocap.application.AppPattern;
import org.ocap.system.MonitorAppPermission;

/**
 * Tests ResourceContentionManager.
 * 
 * @author Aaron Kamienski
 * 
 * @todo Test actual callbacks by reserving actual resources...
 */
public class ResourceContentionManagerTest extends TestCase
{
    /**
     * Tests for no public constructors.
     */
    public void testNoPublicConstructor()
    {
        TestUtils.testNoPublicConstructors(ResourceContentionManager.class);
    }

    /**
     * Tests getInstance().
     */
    public void testGetInstance()
    {
        ResourceContentionManager rcm = ResourceContentionManager.getInstance();
        assertNotNull("getInstance() should not return null", rcm);

        // Not much that can be tested here, since singleton behavior is not
        // required
    }

    /**
     * Tests setResourceFilter().
     * 
     * Simply checks that we can add and remove (via null) filters. Does not
     * verify actual implementation.
     */
    public void testSetResourceFilter()
    {
        AppsDatabaseFilter[] filters = {
                new AppsDatabaseFilter()
                {
                    public boolean accept(AppID appid)
                    {
                        return false;
                    }
                },
                null, // delete
                new AppsDatabaseFilter()
                {
                    public boolean accept(AppID appid)
                    {
                        return true;
                    }
                },
                new AppFilter(),
                new AppFilter(new AppPattern[] { new AppPattern("1-1000", AppPattern.ASK, 0),
                        new AppPattern("1", AppPattern.ALLOW, 255), }), };
        String[] resources = RESOURCES;

        try
        {
            // Test a variety of filters/resources
            for (int fi = 0; fi < filters.length; ++fi)
            {
                for (int ri = 0; ri < resources.length; ++ri)
                {
                    rcm.setResourceFilter(filters[fi], resources[ri]);
                    // Don't assert anything -- just expect no errors
                }

                // Test for exception with null resource
                try
                {
                    rcm.setResourceFilter(filters[fi], null);
                    fail("Expected exception for null resource");
                }
                catch (NullPointerException e)
                {
                }
                catch (IllegalArgumentException e)
                {
                }
            }

        }
        finally
        {
            for (int ri = 0; ri < resources.length; ++ri)
                rcm.setResourceFilter(null, resources[ri]);
        }
    }

    /**
     * Verify that the ResourceContentionManager doesn't leak old filters.
     * 
     * @see "bug 4334"
     */
    public void testSetResourceFilter_ClearLeak()
    {
        // First ensure that WeakReference works as expected here
        Object o = new Object();
        Reference r0 = new WeakReference(o);
        assertNotNull("Reference should not be null", r0.get());
        o = null;
        // After GC, should be deleted
        System.gc();
        assertNull("Expected weak reference to return null", r0.get());

        AppFilter f1 = new AppFilter();
        Reference r1 = new WeakReference(f1);

        // Set security policy handler
        rcm.setResourceFilter(f1, RESOURCES[0]);
        f1 = null;

        // Clear security policy handler
        rcm.setResourceFilter(null, RESOURCES[0]);

        // After GC, should be deleted
        System.gc();
        assertNull("The cleared filter has apparently been leaked", r1.get());
    }

    /**
     * Verify that the ResourceContentionManager doesn't leak old filters.
     * 
     * @see "bug 4334"
     */
    public void testSetResourceFilter_ReplaceLeak()
    {
        AppFilter f1 = new AppFilter();
        AppFilter f2 = new AppFilter();
        Reference r1 = new WeakReference(f1);
        Reference r2 = new WeakReference(f2);

        // Set first filter
        rcm.setResourceFilter(f1, RESOURCES[0]);
        f1 = null;

        // Replace filter
        rcm.setResourceFilter(f2, RESOURCES[0]);
        f2 = null;

        // After GC, original should be collected
        System.gc();

        try
        {
            assertNotNull("The new filter should not have been collected", r2.get());
            assertNull("A repalced filter appears to have been leaked", r1.get());
        }
        finally
        {
            rcm.setResourceFilter(null, RESOURCES[0]);
        }
    }

    /**
     * Tests setResourceContentionHandler().
     * 
     * Simply checks that we can add and remove (via null) handlers. Does not
     * verify actual implementation.
     */
    public void testSetResourceContentionHandler()
    {
        ResourceContentionHandler[] handlers = { new ContentionHandler(), null, new ContentionHandler()
        {
            public ResourceUsage[] resolveResourceContention(ResourceUsage requester, ResourceUsage owners[])
            {
                return new ResourceUsage[] { requester };
            }
        }, };

        try
        {
            for (int i = 0; i < handlers.length; ++i)
                rcm.setResourceContentionHandler(handlers[i]);
            // No assertions, just expect no errors
        }
        finally
        {
            rcm.setResourceContentionHandler(null);
        }
    }

    /**
     * Verify that the ResourceContentionManager doesn't leak old
     * ContentionHandlers.
     * 
     * @see "bug 4334"
     */
    public void testSetResourceContentionHandler_ClearLeak()
    {
        // First ensure that WeakReference works as expected here
        Object o = new Object();
        Reference r0 = new WeakReference(o);
        assertNotNull("Reference should not be null", r0.get());
        o = null;
        // After GC, should be deleted
        System.gc();
        assertNull("Expected weak reference to return null", r0.get());

        ResourceContentionHandler f1 = new ContentionHandler();
        Reference r1 = new WeakReference(f1);

        // Set security policy handler
        rcm.setResourceContentionHandler(f1);
        f1 = null;

        // Clear security policy handler
        rcm.setResourceContentionHandler(null);

        // After GC, should be deleted
        System.gc();
        assertNull("The cleared ContentionHandler has apparently been leaked", r1.get());
    }

    /**
     * Verify that the ResourceContentionManager doesn't leak old
     * ContentionHandlers.
     * 
     * @see "bug 4334"
     */
    public void testSetResourceContentionHandler_ReplaceLeak()
    {
        ResourceContentionHandler f1 = new ContentionHandler();
        ResourceContentionHandler f2 = new ContentionHandler();
        Reference r1 = new WeakReference(f1);
        Reference r2 = new WeakReference(f2);

        // Set first ContentionHandler
        rcm.setResourceContentionHandler(f1);
        f1 = null;

        // Replace ContentionHandler
        rcm.setResourceContentionHandler(f2);
        f2 = null;

        // After GC, original should be collected
        System.gc();

        try
        {
            assertNotNull("The new ContentionHandler should not have been collected", r2.get());
            assertNull("A repalced ContentionHandler appears to have been leaked", r1.get());
        }
        finally
        {
            rcm.setResourceContentionHandler(null);
        }
    }

    /**
     * Tests setResourceFilter() security checks.
     */
    public void testSetResourceFilter_security()
    {
        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        try
        {
            sm.p = null;

            AppPattern[] patterns = { new AppPattern("1", AppPattern.ALLOW, 0), };
            AppFilter filter = new AppFilter(patterns);
            rcm.setResourceFilter(filter, "org.davic.net.tuning.NetworkInterfaceController");

            verifyPermission(sm.p, "handler.resource");
        }
        finally
        {
            ProxySecurityManager.pop();
            rcm.setResourceFilter(null, "org.davic.net.tuning.NetworkInterfaceController");
        }
    }

    /**
     * Tests setResourceContentionHandler() security checks.
     */
    public void testSetResourceContentionHandler_security()
    {
        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        try
        {
            sm.p = null;

            rcm.setResourceContentionHandler(new ResourceContentionHandler()
            {
                public ResourceUsage[] resolveResourceContention(ResourceUsage requester, ResourceUsage owners[])
                {
                    return owners;
                }

                public void resourceContentionWarning(ResourceUsage newRequest, ResourceUsage[] currentReservations)
                {

                }
            });

            verifyPermission(sm.p, "handler.resource");
        }
        finally
        {
            rcm.setResourceContentionHandler(null);
            ProxySecurityManager.pop();
        }
    }

    /**
     * Verify the permission. Used to verify the permission request submitted to
     * the SecurityManager.
     */
    private void verifyPermission(Permission p, String name)
    {
        assertNotNull("non-null Permission should be checked with SecurityManager", p);
        assertTrue("Permission should be MonitorAppPermission", p instanceof MonitorAppPermission);
        assertEquals("Permission name should be", name, ((MonitorAppPermission) p).getName());
    }

    /**
     * Dummy contention handler.
     */
    private class ContentionHandler implements ResourceContentionHandler
    {
        public ResourceUsage[] resolveResourceContention(ResourceUsage requester, ResourceUsage owners[])
        {
            return owners;
        }

        public void resourceContentionWarning(ResourceUsage newRequest, ResourceUsage[] currentReservations)
        {
            // empty
        }
    }

    /**
     * InterfaceTestCase that is used to perform extended tests on the
     * ResourceContentionManager implementation. These tests require additional
     * access to the implementation (e.g., to cause resource acquisition
     * requests and contention).
     */
    public static class ExtendedTest extends InterfaceTestCase
    {
        public void testSetResourceFilter()
        {
            fail("Unimplemented test");
        }

        public void testSetResourceContentionHandler()
        {
            fail("Unimplemented test");
        }

        public static InterfaceTestSuite isuite() throws Exception
        {
            InterfaceTestSuite suite = new InterfaceTestSuite(ExtendedTest.class);
            suite.setName("Extended");
            return suite;
        }

        public ExtendedTest(String name, ImplFactory f)
        {
            this(name, ResourceContentionManager.class, f);
        }

        protected ResourceContentionManagerFactory factory;

        protected ExtendedTest(String name, Class impl, ImplFactory f)
        {
            super(name, impl, f);
            factory = (ResourceContentionManagerFactory) f;
        }

        protected ResourceContentionManager rcm;

        protected ResourceContentionManager createResourceContentionManager()
        {
            return (ResourceContentionManager) createImplObject();
        }

        protected void setUp() throws Exception
        {
            super.setUp();
            rcm = createResourceContentionManager();
        }

        protected void tearDown() throws Exception
        {
            rcm = null;
            super.tearDown();
        }

        public static interface ResourceContentionManagerFactory extends ImplFactory
        {
            /**
             * Cause the app filter for the given proxytype to be called
             * specifying the given AppID.
             */
            public boolean checkReservation(AppID requestor, String proxyType);

            /**
             * Cause the contention handler to be called with the given
             * arguments.
             */
            public AppID[] resolveContention(AppID requestor, AppID[] owners, String proxyType);
        }
    }

    public static final String[] RESOURCES = { "org.davic.net.tuning.NetworkInterfaceController",
            "org.davic.mpeg.sections.SectionFilterGroup",
            // "org.dvb.event.RepositoryDescriptor", // handled separately
            // "org.dvb.net.rc.ConnectionRCInterface", // not supported
            // "org.havi.ui.HScreenDevice", // base class
            "org.havi.ui.HVideoDevice", "org.havi.ui.HGraphicsDevice", "org.havi.ui.HBackgroundDevice", };

    protected ResourceContentionManager rcm;

    protected void setUp() throws Exception
    {
        super.setUp();

        rcm = ResourceContentionManager.getInstance();
    }

    protected void tearDown() throws Exception
    {
        rcm = null;

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
        TestSuite suite = new TestSuite(ResourceContentionManagerTest.class);
        return suite;
    }

    public ResourceContentionManagerTest(String name)
    {
        super(name);
    }
}
