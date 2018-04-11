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
package org.cablelabs.impl.spi;

import javax.tv.locator.Locator;
import javax.tv.locator.LocatorFactory;
import javax.tv.service.SIElement;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.ServiceInformationType;
import javax.tv.service.ServiceType;
import javax.tv.service.navigation.DeliverySystemType;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.navigation.ServiceFilter;
import javax.tv.service.navigation.ServiceList;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.davic.net.tuning.NetworkInterfaceController;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.dvb.spi.ProviderRegistry;
import org.dvb.spi.selection.KnownServiceReference;
import org.dvb.spi.selection.LocatorScheme;
import org.dvb.spi.selection.MockKnownServiceReference;
import org.dvb.spi.selection.MockSelectionProvider;
import org.dvb.spi.selection.MockServiceReference;
import org.dvb.spi.selection.SelectionSession;
import org.dvb.spi.selection.ServiceDescription;
import org.dvb.spi.selection.ServiceReference;
import org.dvb.spi.selection.MockSelectionProvider.MockServiceDescription;
import org.dvb.spi.util.MultilingualString;
import org.ocap.net.OcapLocator;

import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.NetManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.net.CannedNetMgr;
import org.cablelabs.impl.manager.service.CannedSIDatabase;
import org.cablelabs.impl.manager.service.CannedServiceMgr;
import org.cablelabs.impl.service.SIManagerExt;
import org.cablelabs.impl.service.SIRequestException;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.service.TransportStreamExt;

/**
 * SelectionProviderInstanceTest
 * 
 * @author Joshua Keplinger
 * 
 */
public class SelectionProviderInstanceTest extends TestCase
{

    private ProviderRegistry registry;
    
    private ProviderRegistryExt registryExt;

    private MockSelectionProvider sp;

    private long registeredTime;

    private ServiceManager oldSM;

    private CannedServiceMgr csm;

    private NetManager oldNM;

    private CannedNetMgr cnm;

    private CannedSIDatabase csidb;

    private boolean registered;

    /**
	 *
	 */
    public SelectionProviderInstanceTest()
    {
        this(SelectionProviderInstanceTest.class.getName());
    }

    /**
     * @param name
     */
    public SelectionProviderInstanceTest(String name)
    {
        super(name);
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        TestRunner.run(suite());
        System.exit(0);
    }

    public static TestSuite suite()
    {
        return new TestSuite(SelectionProviderInstanceTest.class);
    }

    public void setUp() throws Exception
    {
        super.setUp();

        oldSM = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        csm = (CannedServiceMgr) CannedServiceMgr.getInstance();
        ManagerManagerTest.updateManager(ServiceManager.class, CannedServiceMgr.class, true, csm);
        csidb = (CannedSIDatabase) csm.getSIDatabase();

        oldNM = (NetManager) ManagerManager.getInstance(NetManager.class);
        cnm = (CannedNetMgr) CannedNetMgr.getInstance();
        ManagerManagerTest.updateManager(NetManager.class, CannedNetMgr.class, true, cnm);

        registry = ProviderRegistry.getInstance();
        sp = new MockSelectionProvider();
        registryExt = (ProviderRegistryExt) ProviderRegistryExt.getInstance();

        registered = false;
    }

    public void tearDown() throws Exception
    {
        if (registered)
        {
            registry.unregister(sp);
            registered = false;
        }

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
        csidb = null;

        super.tearDown();
    }

    // Test section

    public void testRegisterSystemBound() throws Exception
    {
        register();
        checkServices();
        unregister();
    }

    public void testRegisterSystemBoundKnown() throws Exception
    {
        String loc = "ocap://0x1";
        MockServiceReference oldRef = new MockServiceReference(loc, loc);

        int freq = csidb.transportStream7.getFrequency();
        int prog = csidb.serviceDetails33.getProgramNumber();
        int mod = csidb.transportStream7.getModulationFormat();
        OcapLocator locator = new OcapLocator(freq, prog, mod);
        MockKnownServiceReference newRef = new MockKnownServiceReference(loc, loc, locator);
        MultilingualString[] names = new MultilingualString[] { new MultilingualString("Service 0x1", "eng") };
        MockServiceDescription newSd = new MockServiceDescription(names);
        sp.cannedUpdateServiceReference(oldRef, newRef, newSd);

        register();
        checkServices();
        unregister();
    }

    public void testRegisterSystemBoundNoDescription() throws Exception
    {
        String loc = "ocap://0x1";
        MockServiceReference oldRef = new MockServiceReference(loc, loc);
        sp.cannedUpdateServiceReference(oldRef, oldRef, null);

        register();
        checkServices();
        unregister();
    }

    public void testNewSession() throws Exception
    {
        String loc = "ocap://0x1";
        SelectionProviderInstance spi = new SelectionProviderInstance(sp);
        spi.init();
        ServiceReference[] refs = sp.getServiceList();
        SelectionSession session = spi.newSession(refs[0], null);
        assertNotNull("Returned SelectionSession should not be null", session);

        assertEquals("Rate is not correct", 2.0f, session.setRate(2.0f), 0.0001f);
        assertEquals("Media time is not correct", 1000000L, session.setPosition(1000000L));

        spi.dispose();

        try
        {
            spi.newSession(new ServiceReference(loc, loc), null);
            fail("Expected IllegalStateException with a disposed SelectionProviderInstance");
        }
        catch (IllegalStateException ex)
        {
        } // expected
    }

    public void testGetServiceSourceId() throws Exception
    {
        register();
        Service service = registryExt.getService("ocap", null, null);
        MultilingualString name = new MultilingualString("Service 0x1", "eng");
        ServiceDescription sd = new MockSelectionProvider.MockServiceDescription(new MultilingualString[] { name });
        checkService((ServiceExt) service, new ServiceReference("ocap://0x1", "ocap://0x1"), sd);
        unregister();
        assertNull("Returned Service should be null", registryExt.getService("ocap", null, "eng"));

        try
        {
            registryExt.getService(null, null, "eng");
            fail("Expected IllegalArgumentException with a null LocatorScheme");
        }
        catch (IllegalArgumentException ex)
        {
        } // expected
    }

    public void testGetServiceServiceName() throws Exception
    {
        String loc = "ocap://n=testService";
        MultilingualString name = new MultilingualString("testService", "eng");
        ServiceDescription sd = new MockSelectionProvider.MockServiceDescription(new MultilingualString[] { name });
        sp.cannedAddServiceReference(new ServiceReference(loc, loc), sd);
        register();

        Service service = registryExt.getService("ocap", null, null);
        checkService((ServiceExt) service, new ServiceReference(loc, loc), sd);

        service = registryExt.getService("ocap", null, null);
        name = new MultilingualString("Service 0x1", "eng");
        sd = new MockSelectionProvider.MockServiceDescription(new MultilingualString[] { name });
        checkService((ServiceExt) service, new ServiceReference("ocap://0x1", "ocap://0x1"), sd);

        unregister();
        assertNull("Returned Service should be null",
                registryExt.getService("ocap", null, "eng"));

        try
        {
            registryExt.getService(null, null, "eng");
            fail("Expected IllegalArgumentException with a null LocatorScheme");
        }
        catch (IllegalArgumentException ex)
        {
        } // expected

        try
        {
            registryExt.getService("ocap", null, "eng");
            fail("Expected IllegalArgumentException with a null LocatorScheme");
        }
        catch (IllegalArgumentException ex)
        {
        } // expected
    }

    public void testGetServiceSourceIdProviderLast() throws Exception
    {
        sp.scheme = new LocatorScheme("ocap", false);
        register();
        Service service = registryExt.getService("ocap", null, null);
        MultilingualString name = new MultilingualString("Service 0x1", "eng");
        ServiceDescription sd = new MockSelectionProvider.MockServiceDescription(new MultilingualString[] { name });
        checkService((ServiceExt) service, new ServiceReference("ocap://0x1", "ocap://0x1"), sd);
        unregister();
        assertNull("Returned Service should be null", registryExt.getService("ocap", null, "eng"));

        try
        {
            registryExt.getService(null, null, "eng");
            fail("Expected IllegalArgumentException with a null LocatorScheme");
        }
        catch (IllegalArgumentException ex)
        {
        } // expected
    }

    public void testGetServiceServiceNameProviderLast() throws Exception
    {
        sp.scheme = new LocatorScheme("ocap", false);
        String loc = "ocap://n=testService";
        MultilingualString name = new MultilingualString("testService", "eng");
        ServiceDescription sd = new MockSelectionProvider.MockServiceDescription(new MultilingualString[] { name });
        sp.cannedAddServiceReference(new ServiceReference(loc, loc), sd);
        register();

        Service service = registryExt.getService("ocap", null, null);
        checkService((ServiceExt) service, new ServiceReference(loc, loc), sd);
        unregister();
        assertNull("Returned Service should be null",
                registryExt.getService("ocap", null, "eng"));

        try
        {
            registryExt.getService(null, null, "eng");
            fail("Expected IllegalArgumentException with a null LocatorScheme");
        }
        catch (IllegalArgumentException ex)
        {
        } // expected

        try
        {
            registryExt.getService("ocap", null, "eng");
            fail("Expected IllegalArgumentException with a null LocatorScheme");
        }
        catch (IllegalArgumentException ex)
        {
        } // expected
    }

    // Test support section

    private void register() throws Exception
    {
        registeredTime = System.currentTimeMillis();
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

    private void checkServices() throws Exception
    {
        ServiceList sl = SIManager.createInstance().filterServices(new ServiceFilter()
        {

            public boolean accept(Service service)
            {
                if (service instanceof SPIService)
                    return true;
                else
                    return false;
            }

        });
        ServiceReference[] svcRefs = sp.getServiceList();
        assertEquals("Incorrect number of services in list", svcRefs.length, sl.size());
        for (int i = 0; i < svcRefs.length; i++)
        {
            ServiceDescription sd = sp.cannedGetServiceDescription(svcRefs[i]);
            ServiceExt service = (ServiceExt) sl.findService(svcRefs[i].getLocator());
            assertNotNull("Service was not found in ServiceList: " + svcRefs[i].getServiceIdentifier(), service);
            checkService(service, svcRefs[i], sd);
        }
    }

    private void checkService(ServiceExt service, ServiceReference ref, ServiceDescription sd) throws Exception
    {
        // Get canned SI
        // TODO(Todd): Currently hard-coded to expect a service mapped to
        // csidb.serviceDetails33
        ServiceDetailsExt csd = csidb.serviceDetails33;
        TransportStreamExt cts = (TransportStreamExt) csd.getTransportStream();
        ServiceComponentExt cscVideo = csidb.serviceComponent69; // video
        ServiceComponentExt cscEng = csidb.serviceComponent69eng; // english
                                                                  // audio
        ServiceComponentExt cscFre = csidb.serviceComponent69fre; // french
                                                                  // audio
        ServiceComponentExt cscSpa = csidb.serviceComponent69spa; // spanish
                                                                  // audio
        ServiceComponentExt cscData = csidb.serviceComponent105; // data

        // Set flag to true if service is mapped
        boolean mapped = ref instanceof KnownServiceReference;

        // Check service
        assertNotNull(service);
        assertNull("Service handle should be null", service.getServiceHandle());
        assertNull("Name multistring should be null", service.getNameAsMultiString());
        assertNotNull("Unique ID should not be null", service.getID());
        assertFalse("Should not report multiple instances", service.hasMultipleInstances());
        assertEquals("Incorrect service locator", ref.getServiceIdentifier(), service.getLocator().toExternalForm());
        assertEquals("Incorrect service number", -1, service.getServiceNumber());
        assertEquals("Incorrect service minor number", -1, service.getMinorNumber());
        assertEquals("Incorrect preferred language", null, service.getPreferredLanguage());
        if (sd == null)
        {
            assertEquals("Incorrect service name", "", service.getName());
            assertEquals("Incorrect service type", ServiceType.UNKNOWN, service.getServiceType());
        }
        else
        {
            assertEquals("Incorrect service name", sd.getLongName("eng").getString(), service.getName());
            assertEquals("Incorrect service type", sd.getServiceType(), service.getServiceType());
        }

        // Check service details
        ServiceDetailsExt sde = (ServiceDetailsExt) service.getDetails();
        assertNotNull(sde);
        assertNull("Service details handle should be null", sde.getServiceDetailsHandle());
        assertNull("Long name multistring should be null", sde.getLongNameAsMultiString());
        Locator loc = LocatorFactory.getInstance().createLocator(ref.getLocator().toExternalForm());
        assertEquals("Incorrect source ID", ((OcapLocator) loc).getSourceID(), sde.getSourceID());
        assertFalse("Service should not be analog", sde.isAnalog());
        assertNotNull("Unique ID should not be null", sde.getID());
        assertNull("Program Schedule should be null", sde.getProgramSchedule());
        assertEquals("Service does not match", service, sde.getService());
        assertEquals("Incorrect service details locator", ref.getLocator().toExternalForm(), sde.getLocator()
                .toExternalForm());
        assertEquals("Incorrect service information type", ServiceInformationType.UNKNOWN,
                sde.getServiceInformationType());
        assertTrue("Update time does not fall within 5 seconds of registering provider",
                (sde.getUpdateTime().getTime() - registeredTime) < 5000);
        assertEquals("Incorrect service number", -1, sde.getServiceNumber());
        assertEquals("Incorrect service minor number", -1, sde.getMinorNumber());
        assertEquals("Incorrect preferred language", null, sde.getPreferredLanguage());
        if (sd == null)
        {
            assertEquals("Incorrect long name", "", sde.getLongName());
            assertEquals("Incorrect service type", ServiceType.UNKNOWN, sde.getServiceType());
            assertEquals("Incorrect delivery system type", DeliverySystemType.UNKNOWN, sde.getDeliverySystemType());
        }
        else
        {
            assertEquals("Incorrect long name", sd.getLongName("eng").getString(), sde.getLongName());
            assertEquals("Incorrect service type", sd.getServiceType(), sde.getServiceType());
            assertEquals("Incorrect delivery system type", sd.getDeliverySystemType(), sde.getDeliverySystemType());
        }
        if (mapped)
        {
            assertEquals("Incorrect program number", csd.getProgramNumber(), sde.getProgramNumber());
            assertEquals("Incorrect PCR PID", csd.getPcrPID(), sde.getPcrPID());
            assertEquals("Incorrect value returned by isFree()", csd.isFree(), sde.isFree());
            assertEquals("Incorrect CA system IDs", csd.getCASystemIDs(), sde.getCASystemIDs());
        }
        else
        {
            assertEquals("Incorrect program number", -1, sde.getProgramNumber());
            assertEquals("Incorrect PCR PID", -1, sde.getPcrPID());
            assertEquals("Incorrect value returned by isFree()", true, sde.isFree());
            assertTrue("CA system IDs should be empty", sde.getCASystemIDs().length == 0);
        }

        // Check retrieval of service description
        try
        {
            sde.getServiceDescription();
            fail("Service description retrieval should fail");
        }
        catch (SIRequestException expected)
        {
        }

        // Check retrieval of service details through SIManager
        SIManagerExt sim = (SIManagerExt) SIManager.createInstance();
        ServiceDetails[] details = (ServiceDetails[]) sim.getSIElement(service.getLocator());
        assertEquals("Incorrect number of elements returned", 1, details.length);
        assertEquals("Incorrect SI element returned", sde, details[0]);

        // Tune so we can get inband SI
        if (mapped)
        {
            NetworkInterfaceController nic = new NetworkInterfaceController(new MockResourceClient());
            ExtendedNetworkInterface ni = (ExtendedNetworkInterface) cnm.getNetworkInterfaceManager()
                    .getNetworkInterfaces()[0];
            nic.reserve(ni, null);
            org.davic.mpeg.TransportStream dts = sde.getDavicTransportStream(ni);
            assertNotNull("Transport stream is null", dts);
            nic.tune(dts);
            Thread.sleep(2000); // wait for tune of canned NI to complete
            assertNotNull("Tune did not complete successfully", ni.getCurrentTransportStream());
        }

        // Check retrieval of transport stream
        TransportStreamExt ts = (TransportStreamExt) sde.getTransportStream();
        if (mapped)
            assertEquals("getTransportStream() returned wrong transport stream", cts, ts);
        else
            assertNull("No TransportStream should be available", sde.getTransportStream());

        // Check retrieval of all components
        try
        {
            ServiceComponentExt[] comps = (ServiceComponentExt[]) sde.getComponents();
            if (mapped)
            {
                assertEquals("getComponents() returned wrong number of components", 5, comps.length);
                assertEquals("getComponents() returned wrong component[0]", cscVideo, comps[0]);
                assertEquals("getComponents() returned wrong component[1]", cscEng, comps[1]);
                assertEquals("getComponents() returned wrong component[2]", cscFre, comps[2]);
                assertEquals("getComponents() returned wrong component[3]", cscSpa, comps[3]);
                assertEquals("getComponents() returned wrong component[4]", cscData, comps[4]);
            }
            else
            {
                fail("getComponents() expected to fail with exception");
            }
        }
        catch (Exception ex)
        {
            if (mapped) throw ex;
        }

        // Check retrieval of default media components
        try
        {
            ServiceComponentExt[] comps = (ServiceComponentExt[]) sde.getDefaultMediaComponents();
            if (mapped)
            {
                assertEquals("getDefaultMediaComponents() returned wrong number of components", 2, comps.length);
                assertEquals("getDefaultMediaComponents() returned wrong component[0]", cscVideo, comps[0]);
                assertEquals("getDefaultMediaComponents() returned wrong component[1]", cscEng, comps[1]);
            }
            else
            {
                fail("getDefaultMediaComponents() expected to fail with exception");
            }
        }
        catch (Exception ex)
        {
            if (mapped) throw ex;
        }

        // Check retrieval of default carousel component
        try
        {
            ServiceComponentExt comp = (ServiceComponentExt) sde.getCarouselComponent();
            if (mapped)
            {
                assertEquals("getCarouselComponent() returned wrong component", cscData, comp);
            }
            else
            {
                fail("getCarouselComponent() expected to fail with exception");
            }
        }
        catch (Exception ex)
        {
            if (mapped) throw ex;
        }

        // Check retrieval of carousel component by carousel ID
        try
        {
            ServiceComponentExt comp = (ServiceComponentExt) sde.getCarouselComponent(46);
            if (mapped)
            {
                assertEquals("getCarouselComponent(int) returned wrong component", cscData, comp);
            }
            else
            {
                fail("getCarouselComponent(int) expected to fail with exception");
            }
        }
        catch (Exception ex)
        {
            if (mapped) throw ex;
        }

        // Check retrieval of components by component tag
        try
        {
            ServiceComponentExt comp = (ServiceComponentExt) sde.getComponentByAssociationTag(21);
            if (mapped)
            {
                assertEquals("getComponentByAssociationTag(int) returned wrong component", cscData, comp);
            }
            else
            {
                fail("getComponentByAssociationTag(int) expected to fail with exception");
            }
        }
        catch (Exception ex)
        {
            if (mapped) throw ex;
        }

        // Check retrieval of transport stream via SIManager
        loc = service.getLocator();
        try
        {
            ts = (TransportStreamExt) sim.getTransportStream(loc);
            if (mapped)
            {
                assertEquals("getTransportStream(loc) returned wrong transport stream", cts, ts);
            }
            else
            {
                fail("getTransportStream(loc) expected to fail with exception");
            }
        }
        catch (Exception ex)
        {
            if (mapped) throw ex;
        }

        // Check retrieval of components by component PID via SIManager
        OcapLocator loc2 = new OcapLocator(loc.toExternalForm() + ".+0x2&0x4");
        try
        {
            ServiceComponentExt[] comps = (ServiceComponentExt[]) sim.getSIElement(loc2);
            if (mapped)
            {
                assertEquals("getSIElement(loc2) returned wrong number of components", 2, comps.length);
                assertEquals("getSIElement(loc2) returned wrong component[0]", cscEng, comps[0]);
                assertEquals("getSIElement(loc2) returned wrong component[1]", cscSpa, comps[1]);
            }
            else
            {
                fail("getSIElement(loc2) expected to fail with exception");
            }
        }
        catch (Exception ex)
        {
            if (mapped) throw ex;
        }

        // Check retrieval of components by component name via SIManager
        OcapLocator loc3 = new OcapLocator(loc.toExternalForm() + ".$serviceComponent69&serviceComponent69fre");
        try
        {
            ServiceComponentExt[] comps = (ServiceComponentExt[]) sim.getSIElement(loc3);
            if (mapped)
            {
                assertEquals("getSIElement(loc3) returned wrong number of components", 2, comps.length);
                assertEquals("getSIElement(loc3) returned wrong component[0]", cscVideo, comps[0]);
                assertEquals("getSIElement(loc3) returned wrong component[1]", cscFre, comps[1]);
            }
            else
            {
                fail("getSIElement(loc3) expected to fail with exception");
            }
        }
        catch (Exception ex)
        {
            if (mapped) throw ex;
        }

        // Check retrieval of components by component tag via SIManager
        OcapLocator loc4 = new OcapLocator(loc.toExternalForm() + ".@0x25"); // decimal
                                                                             // 37
        try
        {
            ServiceComponentExt[] comps = (ServiceComponentExt[]) sim.getSIElement(loc4);
            if (mapped)
            {
                assertEquals("getSIElement(loc4) returned wrong number of components", 1, comps.length);
                assertEquals("getSIElement(loc4) returned wrong component[0]", cscData, comps[0]);
            }
            else
            {
                fail("getSIElement(loc4) expected to fail with exception");
            }
        }
        catch (Exception ex)
        {
            if (mapped) throw ex;
        }

        // Check retrieval of components by stream type via SIManager
        OcapLocator loc5 = new OcapLocator(loc.toExternalForm() + ".0x3,fre");
        try
        {
            ServiceComponentExt[] comps = (ServiceComponentExt[]) sim.getSIElement(loc5);
            if (mapped)
            {
                assertEquals("getSIElement(loc5) returned wrong number of components", 1, comps.length);
                assertEquals("getSIElement(loc5) returned wrong component[0]", cscFre, comps[0]);
            }
            else
            {
                fail("getSIElement(loc5) expected to fail with exception");
            }
        }
        catch (Exception ex)
        {
            if (mapped) throw ex;
        }

        // Check retrieval of components by multiple locator forms via SIManager
        OcapLocator[] locs = new OcapLocator[] { loc3, loc4 };
        try
        {
            SIElement[] comps = (SIElement[]) sim.getSIElements(locs);
            if (mapped)
            {
                assertEquals("getSIElement(loc[]) returned wrong number of components", 3, comps.length);
                assertEquals("getSIElement(loc[]) returned wrong component[0]", cscVideo, comps[0]);
                assertEquals("getSIElement(loc[]) returned wrong component[1]", cscFre, comps[1]);
                assertEquals("getSIElement(loc[]) returned wrong component[2]", cscData, comps[2]);
            }
            else
            {
                fail("getSIElement(loc[]) expected to fail with exception");
            }
        }
        catch (Exception ex)
        {
            if (mapped) throw ex;
        }
    }

    private class MockResourceClient implements ResourceClient
    {

        public void notifyRelease(ResourceProxy proxy)
        {

        }

        public void release(ResourceProxy proxy)
        {

        }

        public boolean requestRelease(ResourceProxy proxy, Object requestData)
        {
            return true;
        }

    }
}
