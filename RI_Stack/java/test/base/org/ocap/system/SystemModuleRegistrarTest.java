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

package org.ocap.system;

import org.cablelabs.test.ProxySecurityManager;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.dvb.application.AppProxyTest.DummySecurityManager;

/**
 * Tests SystemModuleRegistrar
 * 
 * @author Todd Earles
 */
public class SystemModuleRegistrarTest extends TestCase
{
    /**
     * Test registerSASHandler
     */
    public void testRegisterSASHandler() throws Exception
    {
        SystemModuleRegistrar registrar = SystemModuleRegistrar.getInstance();
        SystemModuleTestHandler handler = new SystemModuleTestHandler();
        byte[] appID = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
        registrar.registerSASHandler(handler, appID);
        try
        {
            handler.waitForReady(2000);
            assertNotNull("We should have a module if the session was opened", handler.module);
        }
        finally
        {
            registrar.unregisterSASHandler(handler);
        }
    }

    /**
     * Test registerSASHandler with a non-existent application ID
     */
    public void testRegisterSASHandlerWithBadID() throws Exception
    {
        SystemModuleRegistrar registrar = SystemModuleRegistrar.getInstance();
        SystemModuleTestHandler handler = new SystemModuleTestHandler();
        byte[] appID = new byte[] { 9, 9, 9, 9, 9, 9, 9, 9 };
        registrar.registerSASHandler(handler, appID);
        try
        {
            handler.waitForReady(2000);
            assertNull("A null SystemModule should have been returned", handler.module);
        }
        catch (Error e)
        {
            // Cleanup if was actually set
            try
            {
                registrar.unregisterSASHandler(handler);
            }
            catch (IllegalArgumentException iae)
            {
                // Ignored
            }
            throw e;
        }
    }

    /**
     * test registerSASHandler for permissions checks
     * 
     */
    public void testRegisterSASHandlerSecurity() throws Exception
    {
        SystemModuleRegistrar registrar = SystemModuleRegistrar.getInstance();
        SystemModuleTestHandler handler = new SystemModuleTestHandler();
        byte[] appID = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };

        // install dummy security manager
        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        sm.p = null;

        try
        {
            synchronized (handler)
            {
                registrar.registerSASHandler(handler, appID);
                handler.waitForReady(10000);
                assertNotNull("Expected ready() to be invoked", handler.module);
            }

            // verify that checkPermission() was called
            assertNotNull("expected checkPermission() to be called", sm.p);
            assertTrue("Permission should be MonitorAppPermission", sm.p instanceof MonitorAppPermission);
            assertEquals("Permission name should be podApplication", "podApplication",
                    ((MonitorAppPermission) sm.p).getName());
        }
        finally
        {
            // remove DummySecurityManager
            ProxySecurityManager.pop();
            try
            {
                registrar.unregisterSASHandler(handler);
            }
            catch (IllegalArgumentException e)
            {
                // Ignored if never successfully set
            }
        }
    }

    /**
     * test unregisterSASHandler() for permission checks
     */
    public void testUnregisterSASHandlerSecurity() throws Exception
    {
        SystemModuleRegistrar registrar = SystemModuleRegistrar.getInstance();
        SystemModuleTestHandler handler = new SystemModuleTestHandler();
        byte[] appID = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };

        // install dummy security manager
        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);

        try
        {
            // test unregisterSASHandler(SystemModuleHandler handler)

            // register a handler
            synchronized (handler)
            {
                registrar.registerSASHandler(handler, appID);
                handler.waitForReady(10000);
                assertNotNull("Expected ready() to be invoked", handler.module);
                handler.clear();
            }
            // clear any permission checks
            sm.p = null;

            registrar.unregisterSASHandler(handler);

            // verify that checkPermission() was called
            assertNotNull("expected checkPermission() to be called", sm.p);
            assertTrue("Permission should be MonitorAppPermission", sm.p instanceof MonitorAppPermission);
            assertEquals("Permission name should be podApplication", "podApplication",
                    ((MonitorAppPermission) sm.p).getName());

            handler.waitForUnregister(10000);
            assertTrue("Expected notify unregister to be called", handler.unregister);
            handler.clear();

            // test unregisterSASHandler(byte[] privateHostAppID)

            // register a handler
            synchronized (handler)
            {
                registrar.registerSASHandler(handler, appID);
                handler.waitForReady(10000);
                assertNotNull("Expected ready() to be invoked", handler.module);
                handler.clear();
            }
            // clear any permission checks
            sm.p = null;

            registrar.unregisterSASHandler(appID);

            // verify that checkPermission() was called
            assertNotNull("expected checkPermission() to be called", sm.p);
            assertTrue("Permission should be MonitorAppPermission", sm.p instanceof MonitorAppPermission);
            assertEquals("Permission name should be podApplication", "podApplication",
                    ((MonitorAppPermission) sm.p).getName());

        }
        finally
        {
            // remove DummySecurityManager
            ProxySecurityManager.pop();
            try
            {
                registrar.unregisterSASHandler(handler);
            }
            catch (IllegalArgumentException e)
            {
                // Ignored if never successfully set
            }
        }
    }

    /**
     * test getInstance() for security and the same object
     * 
     */
    public void testGetInstance()
    {
        // install dummy security manager
        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        sm.p = null;

        try
        {
            SystemModuleRegistrar registrar = SystemModuleRegistrar.getInstance();

            assertEquals("expected the same object on consecutive calls", registrar,
                    SystemModuleRegistrar.getInstance());

            // verify that checkPermission() was called
            assertNotNull("expected checkPermission() to be called", sm.p);
            assertTrue("Permission should be MonitorAppPermission", sm.p instanceof MonitorAppPermission);
            assertEquals("Permission name should be podApplication", "podApplication",
                    ((MonitorAppPermission) sm.p).getName());
        }
        finally
        {
            // remove DummySecurityManager
            ProxySecurityManager.pop();
        }
    }

    /**
     * test registerMMIHandler() for permission checks
     */
    public void testRegisterMMIHandlerSecurity() throws Exception
    {
        SystemModuleRegistrar registrar = SystemModuleRegistrar.getInstance();
        SystemModuleTestHandler handler = new SystemModuleTestHandler();

        // install dummy security manager
        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        sm.p = null;

        try
        {
            synchronized (handler)
            {
                registrar.registerMMIHandler(handler);
                handler.waitForReady(10000);
                assertNotNull("Expected ready() to be invoked", handler.module);
                handler.clear();
            }

            // verify that checkPermission() was called
            assertNotNull("expected checkPermission() to be called", sm.p);
            assertTrue("Permission should be MonitorAppPermission", sm.p instanceof MonitorAppPermission);
            assertEquals("Permission name should be podApplication", "podApplication",
                    ((MonitorAppPermission) sm.p).getName());
        }
        finally
        {
            // remove DummySecurityManager
            ProxySecurityManager.pop();
            registrar.unregisterMMIHandler();
        }
    }

    /**
     * test registerMMIHandler() for permission checks
     */
    public void testUnregisterMMIHandlerSecurity() throws Exception
    {
        SystemModuleRegistrar registrar = SystemModuleRegistrar.getInstance();
        SystemModuleTestHandler handler = new SystemModuleTestHandler();

        // install dummy security manager
        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);

        try
        {
            synchronized (handler)
            {
                registrar.registerMMIHandler(handler);
                handler.waitForReady(10000);
                assertNotNull("Expected ready() to be called", handler.module);
                handler.clear();
            }
            // clear previous calls to checkPermission()
            sm.p = null;

            registrar.unregisterMMIHandler();

            // verify that checkPermission() was called
            assertNotNull("expected checkPermission() to be called", sm.p);
            assertTrue("Permission should be MonitorAppPermission", sm.p instanceof MonitorAppPermission);
            assertEquals("Permission name should be podApplication", "podApplication",
                    ((MonitorAppPermission) sm.p).getName());
        }
        finally
        {
            // remove DummySecurityManager
            ProxySecurityManager.pop();
        }
    }

    /**
     * Tests that replacing an SAS handler is supported.
     */
    public void testRegisterSASHandler_Replace() throws Exception
    {
        SystemModuleRegistrar registrar = SystemModuleRegistrar.getInstance();
        SystemModuleTestHandler h1 = new SystemModuleTestHandler();
        SystemModuleTestHandler h2 = new SystemModuleTestHandler();
        byte[] appID = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };

        registrar.registerSASHandler(h1, appID);
        try
        {
            h1.waitForReady(2000);
            assertNotNull("We should have a module if the session was opened", h1.module);
            h1.clear();

            // Attempt to register with same should fail
            try
            {
                registrar.registerSASHandler(h1, appID);
                fail("Expected IllegalArgumentException re-registering same handler");
            }
            catch (IllegalArgumentException e)
            {
                // expected
            }
            // Should not unregister
            h1.waitForUnregister(1000);
            assertFalse("Should not be unregistered", h1.unregister);

            // Replace
            registrar.registerSASHandler(h2, appID);
            h2.waitForReady(2000);
            assertNotNull("We should have a module if the session was opened", h2.module);
            h1.waitForUnregister(2000);
            assertTrue("Expected original handle to be unregistered", h1.unregister);
        }
        finally
        {
            try
            {
                registrar.unregisterSASHandler(appID);
            }
            catch (IllegalArgumentException e)
            {
                /* Ignore */
            }
        }
    }

    /**
     * Tests that replacing an MMI handler is not supported.
     */
    public void testRegisterMMIHandler_Replace() throws Exception
    {
        SystemModuleRegistrar registrar = SystemModuleRegistrar.getInstance();
        SystemModuleTestHandler h1 = new SystemModuleTestHandler();
        SystemModuleTestHandler h2 = new SystemModuleTestHandler();

        registrar.registerMMIHandler(h1);
        try
        {
            h1.waitForReady(2000);
            assertNotNull("We should have a module if the session was opened", h1.module);
            h1.clear();

            // Attempt to replace should fail
            try
            {
                registrar.registerMMIHandler(h2);
                fail("Expected IllegalArgumentException re-registering same handler");
            }
            catch (IllegalArgumentException e)
            {
                // expected
            }

            // Should not unregister
            h1.waitForUnregister(1000);
            assertFalse("Should not be unregistered", h1.unregister);
            // Should not become ready
            h2.waitForReady(1000);
            assertFalse("Should not be ready", h2.ready);
        }
        finally
        {
            try
            {
                registrar.unregisterMMIHandler();
            }
            catch (IllegalArgumentException e)
            {
                /* Ignore */
            }
        }
    }

    /**
     * Tests for leaks.
     */
    public void testRegisterSASHandler_ReplaceLeak() throws Exception
    {
        doTestRegister_ReplaceLeak(REG_SAS);
    }

    /**
     * Tests for leaks.
     */
    public void testRegistereSASHandler_UnregLeak() throws Exception
    {
        doTestRegister_UnregLeak(REG_SAS);
    }

    /**
     * Tests for leaks.
     */
    // Currently skipped -- apparently it cannot be replaced with another
    public void XtestRegisterMMIHandler_ReplaceLeak() throws Exception
    {
        doTestRegister_ReplaceLeak(REG_MMI);
    }

    /**
     * Tests for leaks.
     */
    public void testRegistereMMIHandler_UnregLeak() throws Exception
    {
        doTestRegister_UnregLeak(REG_MMI);
    }

    // TODO(Todd): Use real application IDs so we can talk to the real POD

    // TODO(Todd): Add tests for all other methods

    /**
     * Tests for leaks.
     */
    private void doTestRegister_ReplaceLeak(TestRegister test) throws Exception
    {
        SystemModuleRegistrar registrar = SystemModuleRegistrar.getInstance();
        SystemModuleTestHandler h1 = new SystemModuleTestHandler();
        SystemModuleTestHandler h2 = new SystemModuleTestHandler();
        Reference r1 = new WeakReference(h1);
        Reference r2 = new WeakReference(h2);
        byte[] appID = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };

        test.register(registrar, h1, appID);
        try
        {
            h1.waitForReady(2000);
            assertNotNull("We should have a module if the session was opened", h1.module);
            h1 = null;
            System.gc();
            System.gc();
            assertNotNull("Set handler should not be collected", h1 = (SystemModuleTestHandler) r1.get());

            // Replace
            test.register(registrar, h2, appID);
            h2.waitForReady(2000);
            assertNotNull("We should have a module if the session was opened", h2.module);
            h1.waitForUnregister(2000);
            assertTrue("Expected original handle to be unregistered", h1.unregister);
            h1 = null;
            h2 = null;
            System.gc();
            System.gc();
            assertNull("Replaced handler should be collected", r1.get());
            assertNotNull("Replacement handler should not be collected", r2.get());
        }
        finally
        {
            try
            {
                test.unregister(registrar, appID);
            }
            catch (IllegalArgumentException e)
            {
                /* Ignore */
            }
        }

    }

    /**
     * Tests for leaks.
     */
    private void doTestRegister_UnregLeak(TestRegister test) throws Exception
    {
        SystemModuleRegistrar registrar = SystemModuleRegistrar.getInstance();
        SystemModuleTestHandler h1 = new SystemModuleTestHandler();
        Reference r1 = new WeakReference(h1);
        byte[] appID = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
        test.register(registrar, h1, appID);
        try
        {
            h1.waitForReady(2000);
            assertNotNull("We should have a module if the session was opened", h1.module);
            h1 = null;
            System.gc();
            System.gc();
            assertNotNull("Set handler should not be collected", h1 = (SystemModuleTestHandler) r1.get());

            // Unreg
            test.unregister(registrar, appID);
            h1.waitForUnregister(2000);
            assertTrue("Expected original handle to be unregistered", h1.unregister);
            h1 = null;
            System.gc();
            System.gc();
            assertNull("Replaced handler should be collected", r1.get());
        }
        finally
        {
            try
            {
                test.unregister(registrar, appID);
            }
            catch (IllegalArgumentException e)
            {
                /* Ignore */
            }
        }
    }

    private static interface TestRegister
    {
        public void register(SystemModuleRegistrar registrar, SystemModuleHandler h, byte[] appID);

        public void unregister(SystemModuleRegistrar registrar, byte[] appID);
    }

    private static final TestRegister REG_SAS = new TestRegister()
    {
        public void register(SystemModuleRegistrar r, SystemModuleHandler h, byte[] id)
        {
            r.registerSASHandler(h, id);
        }

        public void unregister(SystemModuleRegistrar r, byte[] id)
        {
            r.unregisterSASHandler(id);
        }
    };

    private static final TestRegister REG_MMI = new TestRegister()
    {
        public void register(SystemModuleRegistrar r, SystemModuleHandler h, byte[] id)
        {
            r.registerMMIHandler(h);
        }

        public void unregister(SystemModuleRegistrar r, byte[] id)
        {
            r.unregisterMMIHandler();
        }
    };

    /**
     * Test handler for dealing with SystemModuleHandler events
     */
    public static class SystemModuleTestHandler implements SystemModuleHandler
    {
        /** The system module that should be used to send APDUs to the POD */
        public SystemModule module;

        public boolean ready;

        public boolean unregister;

        void clear()
        {
            module = null;
            ready = false;
            unregister = false;
        }

        /** The session is now ready */
        public synchronized void ready(SystemModule module)
        {
            this.module = module;
            ready = true;
            notifyAll();
        }

        /**
         * Wait for the session to become ready
         * 
         * @param millis
         *            the maximum time to wait
         */
        public synchronized void waitForReady(long millis) throws InterruptedException
        {
            long end = System.currentTimeMillis() + millis;
            while (!ready && millis > 0)
            {
                wait(millis);
                millis = end - System.currentTimeMillis();
            }
        }

        /** Receive an APDU */
        public synchronized void receiveAPDU(int apduTag, int lengthField, byte[] APDU)
        {
            // TODO(Todd): Finish
        }

        /** APDU send has failed */
        public synchronized void sendAPDUFailed(int apduTag, byte[] failedAPDU)
        {
            // TODO(Todd): Finish
        }

        /** The session has been lost */
        public synchronized void notifyUnregister()
        {
            // TODO(Todd): Finish
            unregister = true;
            module = null;
        }

        public synchronized void waitForUnregister(long millis) throws InterruptedException
        {
            long end = System.currentTimeMillis() + millis;
            while (!unregister && millis > 0)
            {
                wait(millis);
                millis = end - System.currentTimeMillis();
            }
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
        TestSuite suite = new TestSuite(SystemModuleRegistrarTest.class);
        return suite;
    }

    public SystemModuleRegistrarTest(String name)
    {
        super(name);
    }
}
