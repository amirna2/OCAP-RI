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

package org.ocap.hardware;

import org.cablelabs.test.ProxySecurityManager;
import org.cablelabs.test.TestUtils;
import org.cablelabs.test.ProxySecurityManager.NullSecurityManager;

import java.security.Permission;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.ocap.system.MonitorAppPermission;

/**
 * Tests org.ocap.hardware.Host class.
 * 
 * @author Brent Thompson
 */
public class HostTest extends TestCase
{

    /**
     * Verify that there are no public constructors.
     */
    public void testConstructor()
    {
        TestUtils.testNoPublicConstructors(Host.class);
    }

    /**
     * Verify that appropriate fields are defined.
     */
    public void testFields()
    {
        String[] fields = { "FULL_POWER", "LOW_POWER" };
        TestUtils.testNoAddedFields(Host.class, fields);
    }

    /**
     * Verify that we can get a instance of Host
     */
    public void test_getInstance()
    {
        assertNotNull("Host.getInstance() returned null", host);
        assertSame("Expected singleton returned", host, Host.getInstance());
        // as other test functions (below) will use the Host retrieved from
        // getInstance(),
        // don't worry about testing to insure that this is a proper Host
        // instance here
    }

    /**
     * Verify that we can get the host ID string
     */
    public void test_getID()
    {
        String id = host.getID();
        assertNotNull("Host.getID() returned null", id);
        assertTrue("Expected ID to be at least one char long", 0 < id.length());
    }

    /**
     * Verify that we can get the MAC string.
     */
    public void testGetReverseChannelMAC()
    {
        String mac = host.getReverseChannelMAC();
        assertNotNull("Host.getReverseChannelMAC() returned null", mac);
        assertTrue("Expected MAC to be at least one char long", 0 < mac.length());
    }

    /**
     * Tests getPowerMode(). Test is currently limited to verifying that it
     * returns an acceptable value. Test assumes that power mode won't change
     * during execution.
     */
    public void testGetPowerMode()
    {
        int mode = host.getPowerMode();
        switch (mode)
        {
            case Host.FULL_POWER:
            case Host.LOW_POWER:
                break;
            default:
                fail("Invalid PowerMode: " + mode);
        }

        assertEquals("Expected PowerMode to remain unchanged", mode, host.getPowerMode());
    }

    /**
     * Tests (add|remove)PowerModeChangeListener. Simply smoke-tests these calls
     * to make sure that they can be made. No way to test that listener gets
     * called.
     */
    public void testAddRemovePowerModeChangeListener()
    {
        // Make various calls to add/remove... no exceptions should be thrown
        class Listener implements PowerModeChangeListener
        {
            public void powerModeChanged(int mode)
            { /* empty */
            }
        }
        PowerModeChangeListener[] listeners = { new Listener(), new Listener(), new Listener(), };

        try
        {
            // Add multiple listeners
            for (int i = 0; i < listeners.length; ++i)
                host.addPowerModeChangeListener(listeners[i]);

            // Add one more than once
            host.addPowerModeChangeListener(listeners[listeners.length / 2]);
            host.addPowerModeChangeListener(listeners[listeners.length / 2]);

            // Remove one more than once
            host.addPowerModeChangeListener(listeners[0]);
            host.addPowerModeChangeListener(listeners[0]);
        }
        finally
        {
            for (int i = 0; i < listeners.length; ++i)
                host.removePowerModeChangeListener(listeners[i]);
        }
    }

    /**
     * Test isACOutletPresent() and get|setACOutlet().
     */
    public void testACOutlet()
    {
        boolean present = host.isACOutletPresent();
        assertEquals("Expected same result on multiple isACOutletPresent calls", present, host.isACOutletPresent());
        // if the ac outlet is present, then test using real implementation
        if (present)
        {
            // first get the current state
            boolean enabled = host.getACOutlet();
            assertEquals("Expected same state to be returned on consecutive calls", enabled, host.getACOutlet());
            // change the state
            host.setACOutlet(!enabled);
            boolean state = host.getACOutlet();
            assertEquals("Expected the AC outlet state to change", !enabled, state);
            assertEquals("Expected same state to be returned on consecutive calls", state, host.getACOutlet());
        }
        else
        {
            try
            {
                host.getACOutlet();
                fail("expected exception to be thrown by getACOutlet()");
            }
            catch (IllegalStateException e)
            { /* expected */
            }
        }
    }

    /**
     * Test getVideoOutputPorts(). Simply test that valid enumeration is
     * returned.
     */
    public void testGetVideoOutputPorts()
    {
        Enumeration e = host.getVideoOutputPorts();
        assertTrue("Expect at least one VideoOutputPort", e.hasMoreElements());
        for (; e.hasMoreElements();)
        {
            VideoOutputPort port = (VideoOutputPort) e.nextElement();
            assertNotNull("Expected valid VideoOutputPort", port);
        }

        assertFalse("Should still have no more elements", e.hasMoreElements());
        try
        {
            e.nextElement();
            fail("Expected an exception");
        }
        catch (NoSuchElementException ex)
        { /* expected */
        }
    }

    /**
     * Tests security on reboot().
     */
    public void testSecurity_reboot() throws Exception
    {
        ProxySecurityManager.install();
        DummySecurity sm = new DummySecurity(new MonitorAppPermission("reboot"));
        ProxySecurityManager.push(sm);
        try
        {
            try
            {
                host.reboot();
                fail("Expected a SecurityException to be thrown");
            }
            catch (SecurityException e)
            { /* expected */
            }
        }
        finally
        {
            ProxySecurityManager.pop();
        }
    }

    /**
     * Tests security on codeDownload().
     */
    public void testSecurity_codeDownload() throws Exception
    {
        ProxySecurityManager.install();
        DummySecurity sm = new DummySecurity(new MonitorAppPermission("codeDownload"));
        ProxySecurityManager.push(sm);
        try
        {
            try
            {
                host.codeDownload();
                fail("Expected a SecurityException to be thrown");
            }
            catch (SecurityException e)
            { /* expected */
            }
        }
        finally
        {
            ProxySecurityManager.pop();
        }

    }

    /**
     * Tests RFBypass.
     */
    public void testRFBypass() throws Exception
    {
        boolean supported = host.getRFBypassCapability();
        // if the ac outlet is present, then test using real implementation
        if (supported)
        {
            // first get the current state
            boolean enabled = host.getRFBypass();
            try
            {
                assertEquals("Expected same state to be returned on consecutive calls", enabled, host.getRFBypass());

                // change the state
                host.setRFBypass(!enabled);
                assertEquals("Expected the RFBypass state to change", !enabled, host.getRFBypass());
                assertEquals("Expected same state to be returned on consecutive calls", !enabled, host.getRFBypass());

                // restore the state
                host.setRFBypass(enabled);
                assertEquals("Expected the RFBypass state to change", enabled, host.getRFBypass());
            }
            finally
            {
                host.setRFBypass(enabled);
            }
        }
        else
        {
            assertFalse("RFBypass should always be false", host.getRFBypass());
            try
            {
                host.setRFBypass(true);
                host.setRFBypass(false);
                fail("Expected IllegalStateException");
            }
            catch (IllegalStateException e)
            { /* expected */
            }
        }

    }

    /**
     * Simple SecurityManager that will record the tested permission and also
     * throw an exception if desired.
     * 
     * @author Aaron Kamienski
     */
    public static class DummySecurity extends NullSecurityManager
    {
        public Permission p;

        private Permission denied;

        public DummySecurity(Permission denied)
        {
            this.denied = denied;
        }

        public void checkPermission(Permission desiredPerm)
        {
            this.p = desiredPerm;
            if (denied != null && denied.implies(desiredPerm)) throw new SecurityException(desiredPerm.toString());
        }

        public void checkPermission(Permission perm, Object context)
        {
            checkPermission(perm);
        }
    }

    /* Boilerplate */

    protected Host host;

    protected void setUp() throws Exception
    {
        super.setUp();
        host = Host.getInstance();
    }

    protected void tearDown() throws Exception
    {
        host = null;
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

    public static Test suite() throws Exception
    {
        TestSuite suite = new TestSuite(HostTest.class);
        return suite;
    }

    public HostTest(String name)
    {
        super(name);
    }
}
