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

package org.cablelabs.impl.manager.signalling;

import java.util.Enumeration;
import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.dvb.application.AppProxyTest.DummySecurityManager;
import org.ocap.net.OcapLocator;
import org.ocap.system.MonitorAppPermission;

import org.cablelabs.impl.signalling.SignallingEvent;
import org.cablelabs.impl.signalling.SignallingListener;
import org.cablelabs.test.ProxySecurityManager;

/**
 * Tests the SignallingMgr (abstract base class) implementation.
 * 
 * <p>
 * To Do:
 * <ul>
 * <li>Test freq/program AIT listeners...
 * <li>Test mixed sourceId, freq/program listeners...
 * </ul>
 */
public class SignallingMgrTest extends TestCase
{
    private void checkAddAitListeners(SignallingListener[] listeners, OcapLocator[] locators) throws Exception
    {
        assertEquals("Internal error - listeners.length != locators.length", listeners.length, locators.length);

        // Add listeners
        for (int i = 0; i < listeners.length; ++i)
        {
            sigMgr.addAitListener(locators[i], listeners[i]);
        }

        // Make sure monitors were created/started
        assertEquals("Expected AIT monitors to be created", listeners.length, sigMgr.monitors.size());
        for (int i = 0; i < listeners.length; ++i)
        {
            TestSigMgr.TestMonitor mon = (TestSigMgr.TestMonitor) sigMgr.monitors.get(new OcapLocator(
                    locators[i].toString()));
            assertNotNull("Expected AIT monitor for service " + locators[i], mon);
            assertTrue("Expected AIT monitor to be started", mon.started);
            assertFalse("Expected AIT monitor to be not-stopped", mon.stopped);
            mon.clear();
        }
    }

    /**
     * Tests addAitListener().
     * <ul>
     * <li>A new monitor is created for each service.
     * <li>And startMonitoring is called.
     * <li>Duplicates don't create new monitors, just add listeners.
     * </ul>
     */
    public void testAddAitListener() throws Exception
    {
        SignallingListener[] listeners = { new Listener(), new Listener(), new Listener(), };
        OcapLocator[] services = { new OcapLocator(100), new OcapLocator(100, 200, -1), new OcapLocator(300) };

        checkAddAitListeners(listeners, services);

        // Add more listeners
        for (int i = 0; i < listeners.length; ++i)
            sigMgr.addAitListener(services[i], new Listener());

        // Make sure no NEW monitors were created
        assertEquals("Expected no new AIT monitors", listeners.length, sigMgr.monitors.size());
        // Monitors should not have been started or stopped
        for (int i = 0; i < listeners.length; ++i)
        {
            TestSigMgr.TestMonitor mon = (TestSigMgr.TestMonitor) sigMgr.monitors.get(new OcapLocator(
                    services[i].toString()));
            assertNotNull("Expected AIT monitor for " + services[i], mon);
            assertFalse("Expected AIT monitor to be started", mon.started);
            assertFalse("Expected AIT monitor to be not-stopped", mon.stopped);
            mon.clear();
        }
    }

    /**
     * Tests addXaitListener().
     * <ul>
     * <li>A new monitor is created once.
     * <li>And startMonitoring is called, once.
     * <li>Duplicates don't create new monitors, just add listeners.
     * </ul>
     */
    public void testAddXaitListener()
    {
        // Add listener
        sigMgr.addXaitListener(new Listener());

        // Make sure monitors were created/started
        assertNotNull("Expected an XAIT monitor to be created", sigMgr.xaitMonitor);
        assertTrue("Expected XAIT monitor to be started", sigMgr.xaitMonitor.started);
        assertFalse("Expected XAIT monitor to be not-stopped", sigMgr.xaitMonitor.stopped);
        sigMgr.xaitMonitor.clear();

        // Add more listener
        sigMgr.addXaitListener(new Listener());
        sigMgr.addXaitListener(new Listener());

        // Make sure no NEW monitors were created
        // (See assertion in TestSigMgr)
        assertNotNull("Expected an XAIT monitor to be created", sigMgr.xaitMonitor);
        assertFalse("Expected XAIT monitor to be non-started", sigMgr.xaitMonitor.started);
        assertFalse("Expected XAIT monitor to be not-stopped", sigMgr.xaitMonitor.stopped);
        sigMgr.xaitMonitor.clear();
    }

    /**
     * Tests removeAitListener().
     * <ul>
     * <li>No harm if listener not added.
     * <li>No harm for unmatched serviceId
     * <li>stopMonitoring is called for last listener
     * <li>No harm if removed twice.
     * </ul>
     */
    public void testRemoveAitListener() throws Exception
    {
        SignallingListener[] listeners = { new Listener(), new Listener(), new Listener(), };
        OcapLocator[] services = { new OcapLocator(1000, 1, -1), new OcapLocator(500, 2, -1), new OcapLocator(33) };

        // Add listeners
        checkAddAitListeners(listeners, services);

        // Remove unknown listeners: no harm done
        for (int i = 0; i < listeners.length; ++i)
            sigMgr.removeAitListener(services[i], new Listener());

        // No start/stop
        assertEquals("Expected no new or removed monitors", listeners.length, sigMgr.monitors.size());
        for (Enumeration e = sigMgr.monitors.elements(); e.hasMoreElements();)
        {
            TestSigMgr.TestMonitor mon = (TestSigMgr.TestMonitor) e.nextElement();

            assertFalse("Expected AIT monitor to be non-started", mon.started);
            assertFalse("Expected AIT monitor to be not-stopped", mon.stopped);
        }

        // Remove unknown services: no harm done
        sigMgr.removeAitListener(new OcapLocator(1000), listeners[0]);
        // Remove unmatched services/listeners: no harm done
        sigMgr.removeAitListener(services[1], listeners[0]);

        // No start/stop
        assertEquals("Expected no new or removed monitors", listeners.length, sigMgr.monitors.size());
        for (Enumeration e = sigMgr.monitors.elements(); e.hasMoreElements();)
        {
            TestSigMgr.TestMonitor mon = (TestSigMgr.TestMonitor) e.nextElement();

            assertFalse("Expected AIT monitor to be non-started", mon.started);
            assertFalse("Expected AIT monitor to be not-stopped", mon.stopped);
        }

        // Add more listeners
        for (int i = 0; i < listeners.length; ++i)
            sigMgr.addAitListener(services[listeners.length - i - 1], listeners[i]);
        // No start/stop
        assertEquals("Expected no new or removed monitors", listeners.length, sigMgr.monitors.size());
        for (Enumeration e = sigMgr.monitors.elements(); e.hasMoreElements();)
        {
            TestSigMgr.TestMonitor mon = (TestSigMgr.TestMonitor) e.nextElement();

            assertFalse("Expected AIT monitor to be non-started", mon.started);
            assertFalse("Expected AIT monitor to be not-stopped", mon.stopped);
        }

        // Remove first listeners, does nothing
        for (int i = 0; i < listeners.length; ++i)
            sigMgr.removeAitListener(services[i], listeners[i]);
        // No start/stop
        assertEquals("Expected no new monitors", listeners.length, sigMgr.monitors.size());
        for (Enumeration e = sigMgr.monitors.elements(); e.hasMoreElements();)
        {
            TestSigMgr.TestMonitor mon = (TestSigMgr.TestMonitor) e.nextElement();

            assertFalse("Expected AIT monitor to be non-started", mon.started);
            assertFalse("Expected AIT monitor to be not-stopped", mon.stopped);
        }

        // Remove final listeners: should call stop!
        for (int i = 0; i < listeners.length; ++i)
            sigMgr.removeAitListener(services[listeners.length - i - 1], listeners[i]);
        // all stop
        assertEquals("Expected no new monitors", listeners.length, sigMgr.monitors.size());
        for (Enumeration e = sigMgr.monitors.elements(); e.hasMoreElements();)
        {
            TestSigMgr.TestMonitor mon = (TestSigMgr.TestMonitor) e.nextElement();

            assertFalse("Expected AIT monitor to be non-started", mon.started);
            assertTrue("Expected AIT monitor to be stopped", mon.stopped);
            mon.clear();
        }

        // Remove twice: no harm
        for (int i = 0; i < listeners.length; ++i)
            sigMgr.removeAitListener(services[listeners.length - i - 1], listeners[i]);
        for (Enumeration e = sigMgr.monitors.elements(); e.hasMoreElements();)
        {
            TestSigMgr.TestMonitor mon = (TestSigMgr.TestMonitor) e.nextElement();

            assertFalse("Expected AIT monitor to be non-started", mon.started);
            assertFalse("Expected AIT monitor to be not-stopped", mon.stopped);
        }
    }

    /**
     * Tests removeXaitListener().
     * <ul>
     * <li>No harm if listener not added.
     * <li>stopMonitoring is called for last listener
     * <li>No harm if removed twice.
     * </ul>
     */
    public void testRemoveXaitListener()
    {
        SignallingListener[] listeners = { new Listener(), new Listener(), new Listener(), };

        // Add listener
        sigMgr.addXaitListener(listeners[0]);

        // Make sure monitors were created/started
        assertNotNull("Expected an XAIT monitor to be created", sigMgr.xaitMonitor);
        assertTrue("Expected XAIT monitor to be started", sigMgr.xaitMonitor.started);
        assertFalse("Expected XAIT monitor to be not-stopped", sigMgr.xaitMonitor.stopped);
        sigMgr.xaitMonitor.clear();

        // Remove unknown listeners: no harm done
        sigMgr.removeXaitListener(listeners[1]);
        sigMgr.removeXaitListener(listeners[2]);

        // No start/stop
        assertFalse("Expected XAIT monitor to be non-started", sigMgr.xaitMonitor.started);
        assertFalse("Expected XAIT monitor to be non-stopped", sigMgr.xaitMonitor.stopped);

        // Add more listeners
        sigMgr.addXaitListener(listeners[1]);
        sigMgr.addXaitListener(listeners[2]);

        // No start/stop
        assertFalse("Expected XAIT monitor to be non-started", sigMgr.xaitMonitor.started);
        assertFalse("Expected XAIT monitor to be non-stopped", sigMgr.xaitMonitor.stopped);

        // Remove listeners
        for (int i = listeners.length; --i > 0;)
        {
            sigMgr.removeXaitListener(listeners[i]);

            assertFalse("Expected XAIT monitor to be non-started", sigMgr.xaitMonitor.started);
            assertEquals("Expected only last monitor removed to cause stop", i == 0, sigMgr.xaitMonitor.stopped);
        }

        // Remove twice: no harm
        sigMgr.xaitMonitor.clear();
        for (int i = listeners.length; --i > 0;)
        {
            sigMgr.removeXaitListener(listeners[i]);

            assertFalse("Expected XAIT monitor to be non-started", sigMgr.xaitMonitor.started);
            assertFalse("Expected XAIT monitor to be non-stopped", sigMgr.xaitMonitor.stopped);
        }
    }

    /**
     * Tests destroy(). Ensure that destroy can be called from the get-go.
     * Ensure that destroy stops all created monitors.
     */
    public void testDestroy() throws Exception
    {
        // Nothing bad should happen here
        sigMgr.destroy();

        // Try again, with monitors created
        sigMgr = new TestSigMgr();

        SignallingListener[] listeners = { new Listener(), new Listener(), new Listener(), new Listener(), };

        sigMgr.addXaitListener(listeners[0]);
        for (int i = 1; i < listeners.length; ++i)
            sigMgr.addAitListener(new OcapLocator(i * 400 + i), listeners[i]);

        // Make sure monitors were created
        // Make sure monitors were started
        assertNotNull("Expected an XAIT monitor to be created", sigMgr.xaitMonitor);
        assertTrue("Expected XAIT monitor to be started", sigMgr.xaitMonitor.started);
        assertFalse("Expected XAIT monitor to be not-stopped", sigMgr.xaitMonitor.stopped);
        sigMgr.xaitMonitor.clear();
        assertEquals("Expected AIT monitors to be created", listeners.length - 1, sigMgr.monitors.size());
        for (int i = 1; i < listeners.length; ++i)
        {
            TestSigMgr.TestMonitor mon = (TestSigMgr.TestMonitor) sigMgr.monitors.get(new OcapLocator(i * 400 + i));
            assertNotNull("Expected AIT monitor for serviceId " + (i * 400 + i), mon);
            assertTrue("Expected AIT monitor to be started", mon.started);
            assertFalse("Expected AIT monitor to be not-stopped", mon.stopped);
            mon.clear();
        }

        // Finally.... test destroy
        sigMgr.destroy();

        // Ensure that stop was called on EACH monitor!
        assertTrue("Expected XAIT monitor to be stopped", sigMgr.xaitMonitor.stopped);
        assertFalse("Expected XAIT monitor to be not-started", sigMgr.xaitMonitor.started);
        for (int i = 1; i < listeners.length; ++i)
        {
            TestSigMgr.TestMonitor mon = (TestSigMgr.TestMonitor) sigMgr.monitors.get(new OcapLocator(i * 400 + i));
            assertTrue("Expected AIT monitor to be stopped", mon.stopped);
            assertFalse("Expected AIT monitor to be not-started", mon.started);
        }
    }

    public void testRegisterUnboundApp_security() throws Exception
    {
        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        sm.p = null;
        sigMgr.destroy();

        try
        {
            sigMgr.registerUnboundApp(null);
            assertTrue("Permission should be MonitorAppPermission", sm.p instanceof MonitorAppPermission);
            assertEquals("Permission name should be", "registrar", ((MonitorAppPermission) sm.p).getName());
        }
        catch (IllegalArgumentException x)
        {
            // ignore complaint by AitParser that the xait fragment is null
        }
        finally
        {
            ProxySecurityManager.pop();
        }
    }

    public void X_testRegisterUnboundApp()
    {
    }

    public void X_testHandleSignalling()
    {
    }

    public void X_testHandleXait()
    {
    }

    public void X_testHandleAit()
    {
    }

    private TestSigMgr sigMgr;

    protected void setUp() throws Exception
    {
        sigMgr = new TestSigMgr();
    }

    protected void tearDown() throws Exception
    {
        if (sigMgr != null) sigMgr.destroy();
        sigMgr = null;
    }

    public SignallingMgrTest(String name)
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
            TestSuite suite = new TestSuite(SignallingMgrTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new SignallingMgrTest(tests[i]));
            return suite;
        }
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(SignallingMgrTest.class);
        return suite;
    }

    private static class Listener implements SignallingListener
    {
        public SignallingEvent event;

        public void signallingReceived(SignallingEvent event)
        {
            this.event = event;
        }
    }

    private static class TestSigMgr extends SignallingMgr
    {
        public TestMonitor xaitMonitor;

        public Hashtable monitors = new Hashtable();

        public void clear()
        {
            xaitMonitor = null;
            monitors.clear();
        }

        protected SignallingMonitor createXaitMonitor()
        {
            assertTrue("Expected no monitor to be set!", xaitMonitor == null);
            xaitMonitor = new TestMonitor();
            return xaitMonitor;
        }

        protected SignallingMonitor createAitMonitor(OcapLocator service)
        {
            TestMonitor mon = new TestMonitor();
            assertTrue("Expected no monitor to be set!", monitors.get(service) == null);
            monitors.put(service, mon);
            return mon;
        }

        public class TestMonitor extends SignallingMonitor
        {
            public boolean started = false;

            public boolean stopped = false;

            public void clear()
            {
                started = false;
                stopped = false;
            }

            public void startMonitoring()
            {
                started = true;
            }

            public void stopMonitoring()
            {
                stopped = true;
            }

            public void resignal()
            {
            }
        }

        public void deletePersistentXait()
        {
        }

        public boolean loadPersistentXait()
        {
            return false;
        }
    }
}
