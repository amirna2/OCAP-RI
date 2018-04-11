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
package org.dvb.spi;

import javax.tv.xlet.XletContext;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.dvb.spi.selection.LocatorScheme;
import org.dvb.spi.selection.MockSelectionProvider;
import org.dvb.spi.selection.SelectionProvider;

import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.NetManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.net.CannedNetMgr;
import org.cablelabs.impl.manager.service.CannedServiceMgr;

/**
 * ProviderRegistryTest
 * 
 * @author Joshua Keplinger
 * 
 */
public class ProviderRegistryTest extends TestCase
{

    private ProviderRegistry registry;

    private MockSelectionProvider sp;

    private ServiceManager oldSM;

    private CannedServiceMgr csm;

    private NetManager oldNM;

    private CannedNetMgr cnm;

    private boolean registered;

    /**
	 *
	 */
    public ProviderRegistryTest()
    {
        this(ProviderRegistryTest.class.getName());
    }

    /**
     * @param name
     */
    public ProviderRegistryTest(String name)
    {
        super(name);
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
        System.exit(0);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(ProviderRegistryTest.class);
        suite.setName(ProviderRegistryTest.class.getName());
        return suite;
    }

    public void setUp() throws Exception
    {
        super.setUp();

        oldSM = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        csm = (CannedServiceMgr) CannedServiceMgr.getInstance();
        ManagerManagerTest.updateManager(ServiceManager.class, CannedServiceMgr.class, true, csm);

        oldNM = (NetManager) ManagerManager.getInstance(NetManager.class);
        cnm = (CannedNetMgr) CannedNetMgr.getInstance();
        ManagerManagerTest.updateManager(NetManager.class, CannedNetMgr.class, true, cnm);

        registry = ProviderRegistry.getInstance();
        sp = new MockSelectionProvider();

        registered = false;
    }

    public void tearDown() throws Exception
    {
        if (registered) registry.unregister(sp);

        ManagerManagerTest.updateManager(NetManager.class, oldNM.getClass(), true, oldNM);
        ManagerManagerTest.updateManager(ServiceManager.class, oldSM.getClass(), true, oldSM);
        cnm.destroy();
        csm.destroy();

        sp = null;
        registry = null;
        oldSM = null;
        oldNM = null;
        cnm = null;
        csm = null;

        super.tearDown();
    }

    // Test Section

    public void testRegisterXletBound()
    {
        XletBoundProvider xbp = new XletBoundProvider()
        {

            public XletContext getBoundXletContext()
            {
                return null;
            }

            public String getName()
            {
                return null;
            }

            public Class[] getServiceProviderInterfaces()
            {
                return new Class[] { Provider.class, XletBoundProvider.class };
            }

            public String getVersion()
            {
                return null;
            }

            public void providerRegistered()
            {

            }

            public void providerUnregistered()
            {

            }

        };
        try
        {
            registry.registerXletBound(xbp);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException ex)
        {
            // expected
        }
        catch (Exception ex)
        {
            fail("Expected IllegalArgumentException, instead received " + ex);
        }
    }

    public void testRegisterSystemBound() throws Exception
    {
        register();
        unregister();
    }

    public void testRegisterSystemBoundFailure()
    {
        SystemBoundProvider sbp = new SystemBoundProvider()
        {

            public String getName()
            {
                return null;
            }

            public Class[] getServiceProviderInterfaces()
            {
                return this.getClass().getClasses();
            }

            public String getVersion()
            {
                return null;
            }

            public void providerRegistered()
            {

            }

            public void providerUnregistered()
            {

            }

        };
        try
        {
            registry.registerSystemBound(sbp);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException ex)
        {
            // expected
        }
        catch (Exception ex)
        {
            fail("Expected IllegalArgumentException, instead got " + ex);
        }
    }

    public void testRegisterSystemBoundRegisterTwiceFailure() throws Exception
    {
        register();
        try
        {
            register();
            fail("Expected ProviderInstallationFailedException when registering twice");
        }
        catch (ProviderFailedInstallationException expected)
        {
        };
    }

    public void testGetInstalledProviders() throws Exception
    {
        String[] providers = registry.getInstalledProviders();
        assertTrue("Array should be null or zero-length", (providers == null) || (providers.length == 0));

        register();

        providers = registry.getInstalledProviders();
        assertNotNull("Providers array should not be null", providers);
        assertEquals("Number of providers is wrong", 1, providers.length);
        assertEquals("Provider name is incorrect", sp.getName(), providers[0]);

        unregister();

        providers = registry.getInstalledProviders();
        assertTrue("Array should be null or zero-length", (providers == null) || (providers.length == 0));
    }

    public void testGetProviderVersion() throws Exception
    {
        try
        {
            registry.getProviderVersion(MockSelectionProvider.class.getName());
            fail("Expected IllegalArgumentException with invalid Provider name '"
                    + MockSelectionProvider.class.getName() + "'");
        }
        catch (IllegalArgumentException ex)
        {
            // expected
        }
        register();

        String version = registry.getProviderVersion(new MockSelectionProvider().getName());
        assertEquals("Provider version does not match", sp.getVersion(), version);

        unregister();
        try
        {
            registry.getProviderVersion(MockSelectionProvider.class.getName());
            fail("Expected IllegalArgumentException with invalid Provider name '"
                    + MockSelectionProvider.class.getName() + "'");
        }
        catch (IllegalArgumentException ex)
        {
            // expected
        }
    }

    public void testUnsupportedScheme()
    {
        // A provider that reports an unsupported scheme
        SelectionProvider provider = new MockSelectionProvider()
        {
            public LocatorScheme[] getSupportedLocatorSchemes()
            {
                return new LocatorScheme[] { new LocatorScheme("badscheme", true) };
            }
        };

        // Try to register it
        try
        {
            registry.registerSystemBound(provider);
            fail("Expected ProviderFailedInstallationException");
        }
        catch (ProviderFailedInstallationException ex)
        {
            // expected
        }
    }

    public void testMalformedOID()
    {
        // A provider that reports a name with a malformed OID
        SelectionProvider provider = new MockSelectionProvider()
        {
            public String getName()
            {
                return "00000001.MockSelectionProvider.NoCard";
            }
        };

        // Try to register it
        try
        {
            registry.registerSystemBound(provider);
            fail("Expected ProviderFailedInstallationException");
        }
        catch (ProviderFailedInstallationException ex)
        {
            // expected
        }
    }

    public void testBadOID()
    {
        // A provider that reports a name with bad OID
        SelectionProvider provider = new MockSelectionProvider()
        {
            public String getName()
            {
                return "00000001bad.MockSelectionProvider.NoCard";
            }
        };

        // Try to register it
        try
        {
            registry.registerSystemBound(provider);
            fail("Expected ProviderFailedInstallationException");
        }
        catch (ProviderFailedInstallationException ex)
        {
            // expected
        }

        provider = new MockSelectionProvider()
        {
            public String getName()
            {
                return "0xFOOBAR.MockSelectionProvider.NoCard";
            }
        };

        // Try to register it
        try
        {
            registry.registerSystemBound(provider);
            fail("Expected ProviderFailedInstallationException");
        }
        catch (ProviderFailedInstallationException ex)
        {
            // expected
        }
    }

    // Test helper section

    private void register() throws Exception
    {
        registry.registerSystemBound(sp);
        registered = true;
        assertTrue("SelectionProvider.init() was not called", sp.inited);
        assertTrue("SelectionProvider.providerRegistered() was not called", sp.registered);
    }

    private void unregister() throws Exception
    {
        registry.unregister(sp);
        registered = false;
        assertFalse("SelectionProvider.providerUnregistered() was not called", sp.registered);
    }
}
