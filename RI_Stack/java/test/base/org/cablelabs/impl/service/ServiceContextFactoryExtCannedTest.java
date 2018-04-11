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
package org.cablelabs.impl.service;

import java.lang.ref.WeakReference;

import javax.tv.service.selection.PresentationTerminatedEvent;
import javax.tv.service.selection.ServiceContext;

import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.service.CannedServicesDatabase;
import org.cablelabs.test.SICannedInterfaceTest;

import junit.textui.TestRunner;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

/**
 * CannedServiceContextFactoryExtTest is an interface test on the
 * ServiceContextFactoryExt interface. Since it is only an interface test, it
 * does not test beyond what is exposed by this interface.</p>
 * <p>
 * The ServiceContextFactoryExt interface consists of only getter methods, so
 * there will not be a detailed description of the testing performed on these.
 * However, due to the requirement of reliablility on these getter methods, it
 * should be known that they are thoroughly tested for their expected
 * functionality.
 * </p>
 * 
 * @author Joshua Keplinger
 */
public class ServiceContextFactoryExtCannedTest extends SICannedInterfaceTest
{
    /**
     * Main method, allows this test to be run stand-alone.
     * 
     * @param args
     *            Arguments to be passed to the main method (ignored)
     */
    public static void main(String[] args)
    {
        try
        {
            TestRunner.run(isuite());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    // Setup section \\

    /**
     * No-arg constructor for creating our test case.
     * 
     */
    public ServiceContextFactoryExtCannedTest()
    {
        super("ServiceContextFactoryExtCannedTest", ServiceContextFactoryExt.class,
                new CannedServiceContextFactoryExtTestFactory());
    }

    /**
     * Creates a test suite to for use in the testing environment.
     * 
     * @return An InterfaceTestSuite that contains the tests in this test case.
     */
    public static InterfaceTestSuite isuite()
    {
        InterfaceTestSuite suite = new InterfaceTestSuite(ServiceContextFactoryExtCannedTest.class);
        suite.setName(ServiceContextFactoryExt.class.getName());
        suite.addFactory(new CannedServiceContextFactoryExtTestFactory());
        return suite;
    }

    /**
     * Sets up our test
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        factory = (ServiceContextFactoryExt) createImplObject();
        ccManager = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
    }

    /**
     * Clean up after the test
     */
    protected void tearDown() throws Exception
    {
        factory = null;

        super.tearDown();
    }

    // Test Section \\

    /**
     * Tests <code>createServiceContext</code> to make sure it returns a valid
     * ServiceContext.
     */
    public void testCreateServiceContext() throws Exception
    {
        ServiceContextExt ctx1 = (ServiceContextExt) factory.createServiceContext();
        ServiceContextExt ctx2 = (ServiceContextExt) factory.createServiceContext();
        try
        {
            assertNotNull("Created ServiceContext is null", ctx1);
            assertNotNull("Created ServiceContext is null", ctx2);
            assertFalse("Different ServiceContext object shouldn't be equal", ctx1.equals(ctx2));

            // Destroy service context objects
            ServiceContextExtCannedTest.CannedServiceContextListener listener = new ServiceContextExtCannedTest.CannedServiceContextListener(
                    getName());
            ctx1.addListener(listener);
            ctx1.destroy();
            listener.getServiceContextDestroyedEvent();
            ctx2.addListener(listener);
            ctx2.destroy();
            listener.getServiceContextDestroyedEvent();
        }
        finally
        {
            if (ctx1 != null && !ctx1.isDestroyed())
            {
                ctx1.destroy();
            }
            if (ctx2 != null && !ctx2.isDestroyed())
            {
                ctx2.destroy();
            }
        }
        // Make sure service context objects are not leaked
        WeakReference ref1 = new WeakReference(ctx1);
        WeakReference ref2 = new WeakReference(ctx2);
        ctx1 = null;
        ctx2 = null;
        freeUnreferencedObjects();
        assertNull("Leak detected trying to destroy ctx1", ref1.get());
        assertNull("Leak detected trying to destroy ctx2", ref2.get());

    }

    /**
     * Tests <code>createAutoSelectServiceContext</code>
     */
    public void testCreateAutoSelectServiceContext() throws Exception
    {
        ServiceContextExt ctx1 = (ServiceContextExt) factory.createAutoSelectServiceContext();
        assertNotNull("Created ServiceContext is null", ctx1);
        ServiceContextExt ctx2 = (ServiceContextExt) factory.createAutoSelectServiceContext();
        assertNotNull("Created ServiceContext is null", ctx2);
        assertFalse("Different ServiceContext object shouldn't be equal", ctx1.equals(ctx2));

        try
        {
            // Make sure the ServiceContext destroys itself when no service
            // remains selected
            ServiceContextExtCannedTest.CannedServiceContextListener listener = new ServiceContextExtCannedTest.CannedServiceContextListener(
                    getName());
            ctx1.addListener(listener);
            ctx1.select(CannedServicesDatabase.abs1);
            listener.getNormalContentEvent();
            ctx1.stop();
            listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.USER_STOP);
            listener.getServiceContextDestroyedEvent();
            ctx2.addListener(listener);
            ctx2.select(CannedServicesDatabase.abs1);
            listener.getNormalContentEvent();
            ctx2.stop();
            listener.getPresentationTerminatedEvent(PresentationTerminatedEvent.USER_STOP);
            listener.getServiceContextDestroyedEvent();

            // Make sure service context objects are not leaked
            WeakReference ref1 = new WeakReference(ctx1);
            WeakReference ref2 = new WeakReference(ctx2);
            ctx1 = null;
            ctx2 = null;
            freeUnreferencedObjects();
            assertNull("Leak detected trying to destroy ctx1", ref1.get());
            assertNull("Leak detected trying to destroy ctx2", ref2.get());
        }
        finally
        {
            if (ctx1 != null)
            {
                ctx1.destroy();
            }
            if (ctx2 != null)
            {
                ctx2.destroy();
            }
        }
    }

    /**
     * Request garbage collection and running of finalizers to help ensure that
     * unreferenced objects are removed from the heap.
     */
    private void freeUnreferencedObjects()
    {
        System.gc();
        System.runFinalization();
        System.gc();
        System.runFinalization();
        System.gc();
    }

    /**
     * Tests <code>getAllServiceContexts</code> to make sure it returns a valid
     * array of ServiceContexts.
     */
    public void testGetAllServiceContexts() throws Exception
    {
        boolean ctxFound = false;
        boolean ctx2Found = false;
        boolean ctx3Found = false;
        ServiceContext ctx = factory.createServiceContext();
        ServiceContext ctx2 = factory.createServiceContext();
        ServiceContext ctx3 = factory.createServiceContext();

        ServiceContext[] contexts = factory.getAllServiceContexts();
        for (int i = 0; i < contexts.length; i++)
        {
            if (contexts[i].equals(ctx)) ctxFound = true;
            if (contexts[i].equals(ctx2)) ctx2Found = true;
            if (contexts[i].equals(ctx3)) ctx3Found = true;
        }

        ctx.destroy();
        ctx2.destroy();
        ctx3.destroy();
        assertTrue("One or both of the ServiceContexts was not returned", ctxFound && ctx2Found && ctx3Found);
    }

    /**
     * Tests <code>getInstance</code> to make sure it returns a valid reference
     * to itself.
     */
    public void testGetInstance()
    {
        assertTrue("Returned reference is not of the right type",
                ServiceContextFactoryExt.getInstance() instanceof ServiceContextFactoryExt);
        assertTrue("Returned reference is not of the right type",
                ServiceContextFactoryExt.getInstance() instanceof ServiceContextFactoryExt);
    }

    /**
     * Tests <code>getServiceContext</code> to make sure it returns a valid
     * reference to a ServiceContext corresponding to the given XletContext.
     * This test will be finished once it is discovered how to properly create
     * an XletContext in the canned environment (if possible).
     */
    public void testGetServiceContext()
    {
        // TODO (Josh) Implement
    }

    /**
     * Tests <code>getServiceContexts</code> to make sure it returns a valid
     * array of ServiceContexts.
     */
    public void testGetServiceContexts() throws Exception
    {
        boolean ctxFound = false;
        boolean ctx2Found = false;
        boolean ctx3Found = false;
        ServiceContext ctx = factory.createServiceContext();
        ServiceContext ctx2 = factory.createServiceContext();
        ServiceContext ctx3 = factory.createServiceContext();
        ServiceContext[] contexts = factory.getServiceContexts();
        for (int i = 0; i < contexts.length; i++)
        {
            if (contexts[i].equals(ctx)) ctxFound = true;
            if (contexts[i].equals(ctx2)) ctx2Found = true;
            if (contexts[i].equals(ctx3)) ctx3Found = true;
        }
        ctx.destroy();
        ctx2.destroy();
        ctx3.destroy();
        assertTrue("One or both of the ServiceContexts was not returned", ctxFound && ctx2Found && ctx3Found);
    }

    /**
     * Tests <code>setCreateEnabled()</code> and the ability to disable/enable
     * service context creation.
     */
    public void testSetCreateEnabled() throws Exception
    {
        factory.setCreateEnabled(false);
        try
        {
            ServiceContext sc = null;
            try
            {
                sc = factory.createServiceContext();
                fail("Expected failure while creation is disabled");
            }
            catch (Exception e)
            { /* expected */
            }
            finally
            {
                if (sc != null)
                {
                    sc.destroy();
                }
            }

            factory.setCreateEnabled(true);
            try
            {
                sc = factory.createServiceContext();
                assertNotNull("Expected success while creation is enabled", sc);
            }
            finally
            {
                if (sc != null)
                {
                    sc.destroy();
                }
            }
        }
        finally
        {
            factory.setCreateEnabled(true);
        }
    }

    // Data Section \\

    private ServiceContextFactoryExt factory;

    private CallerContextManager ccManager;

    /**
     * This is a default factory class that is passed to the
     * <code>CannedServiceContextFactoryExtTest</code>. It is used to
     * instantiate a concrete class to be used in the test.
     * 
     * @author Josh
     */
    private static class CannedServiceContextFactoryExtTestFactory implements ImplFactory
    {

        public Object createImplObject() throws Exception
        {
            ServiceManager manager = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
            return manager.getServiceContextFactory();
        }

        public String toString()
        {
            return "CannedServiceContextFactoryExtTestFactory";
        }
    }
}
