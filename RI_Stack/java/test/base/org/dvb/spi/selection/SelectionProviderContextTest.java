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
package org.dvb.spi.selection;

import javax.tv.service.SIElement;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.ServiceType;
import javax.tv.service.navigation.DeliverySystemType;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.navigation.ServiceFilter;
import javax.tv.service.navigation.ServiceIterator;
import javax.tv.service.navigation.ServiceList;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.davic.net.tuning.NetworkInterfaceController;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.dvb.spi.ProviderRegistry;
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
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.spi.SPIService;
import org.cablelabs.impl.spi.SPIServiceDetails;

/**
 * SelectionProviderContextTest
 * 
 * @author Joshua Keplinger
 * 
 */
public class SelectionProviderContextTest extends TestCase
{

    private ProviderRegistry registry;

    private MockSelectionProvider sp;

    private ServiceManager oldSM;

    private CannedServiceMgr csm;

    private NetManager oldNM;

    private CannedNetMgr cnm;

    private CannedSIDatabase csidb;

    /**
	 *
	 */
    public SelectionProviderContextTest()
    {
        this(SelectionProviderContextTest.class.getName());
    }

    /**
     * @param name
     */
    public SelectionProviderContextTest(String name)
    {
        super(name);
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(SelectionProviderContextTest.class);
        suite.setName(SelectionProviderContextTest.class.getName());
        return suite;
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
        registry.registerSystemBound(sp);
    }

    public void tearDown() throws Exception
    {
        registry.unregister(sp);

        ManagerManagerTest.updateManager(NetManager.class, oldNM.getClass(), true, oldNM);
        ManagerManagerTest.updateManager(ServiceManager.class, oldSM.getClass(), true, oldSM);
        cnm.destroy();
        csm.destroy();

        sp = null;
        registry = null;
        oldSM = null;
        csm = null;
        csidb = null;

        super.tearDown();
    }

    // Test section

    public void testServiceListChangedAddService()
    {
        String loc = "ocap://0x26";
        ServiceReference ref = new MockServiceReference(loc, loc);
        ServiceDescription sd = new MockServiceDescription(new MultilingualString[] { new MultilingualString(
                "Service 0x26", "eng") });
        // We're adding 1 service
        sp.cannedAddServiceReference(ref, sd);

        ServiceList sl = SIManager.createInstance().filterServices(new ServiceFilter()
        {

            public boolean accept(Service service)
            {
                return service.getServiceType() instanceof MockSelectionProvider.MockServiceType;
            }

        });
        assertEquals("ServiceList does not contain correct number of services", 5, sl.size());
        // Make sure that the service we added is actually in the list
        boolean found = false;
        ServiceIterator it = sl.createServiceIterator();
        while (it.hasNext())
        {
            String svcLoc = it.nextService().getLocator().toString();
            if (loc.equals(svcLoc))
            {
                found = true;
                break;
            }

        }
        assertTrue("Service (" + loc + ") was not added properly", found);
    }

    public void testServiceListChangedAddServiceOverrideProviderFirst() throws Exception
    {
        String loc = csidb.l100.toExternalForm();
        MockServiceReference ref = new MockServiceReference(loc, loc);
        MockServiceDescription sd = new MockServiceDescription(new MultilingualString[] { new MultilingualString(
                "Service 0x64", "eng") });
        sp.cannedAddServiceReference(ref, sd);

        SIManagerExt sim = (SIManagerExt) SIManager.createInstance();
        ServiceList sl = sim.filterServices(null);
        assertEquals("ServiceList size is incorrect", 27, sl.size());
        sl = sl.filterServices(new ServiceFilter()
        {

            public boolean accept(Service service)
            {
                return service instanceof SPIService;
            }

        });
        assertEquals("ServiceList size is incorrect", 5, sl.size());
        ServiceDetails[] sde = sim.getServiceDetails(csidb.l100);
        assertEquals("ServiceDetails array size is incorrect", 1, sde.length);
        assertTrue("ServiceDetails[0] should be a SPIServiceDetails", sde[0] instanceof SPIServiceDetails);

        SIElement[] eles = sim.getSIElement(csidb.l100);
        assertEquals("SIElement array size is incorrect", 1, eles.length);
        assertTrue("SIElement[0] should be a SPIServiceDetails", eles[0] instanceof SPIServiceDetails);
    }

    public void testServiceListChangedAddServiceOverrideProviderLast() throws Exception
    {
        registry.unregister(sp);
        sp.inited = false;
        sp.scheme = new LocatorScheme("ocap", false);
        registry.registerSystemBound(sp);
        String loc = csidb.l100.toExternalForm();
        MockServiceReference ref = new MockServiceReference(loc, loc);
        MockServiceDescription sd = new MockServiceDescription(new MultilingualString[] { new MultilingualString(
                "Service 0x64", "eng") });
        sp.cannedAddServiceReference(ref, sd);

        SIManagerExt sim = (SIManagerExt) SIManager.createInstance();
        ServiceList sl = sim.filterServices(null);
        assertEquals("ServiceList size is incorrect", 27, sl.size());

        sl = sl.filterServices(new ServiceFilter()
        {

            public boolean accept(Service service)
            {
                return service instanceof SPIService;
            }

        });
        assertEquals("ServiceList size is incorrect", 4, sl.size());

        ServiceDetails[] sde = sim.getServiceDetails(csidb.l100);
        assertEquals("ServiceDetails array size is incorrect", 1, sde.length);
        assertFalse("ServiceDetails[0] should not be a SPIServiceDetails", sde[0] instanceof SPIServiceDetails);

        SIElement[] eles = sim.getSIElement(csidb.l100);
        assertEquals("SIElement array size is incorrect", 1, eles.length);
        assertFalse("SIElement[0] should not be a SPIServiceDetails", eles[0] instanceof SPIServiceDetails);
    }

    public void testServiceListChangedRemoveService()
    {
        String loc = "ocap://0x1";
        ServiceReference ref = new MockServiceReference(loc, loc);
        // Remove the OCAP transport-dependent service.
        sp.cannedRemoveServiceReference(ref);

        ServiceList sl = SIManager.createInstance().filterServices(new ServiceFilter()
        {

            public boolean accept(Service service)
            {
                return service.getServiceType() instanceof MockSelectionProvider.MockServiceType;
            }

        });
        assertEquals("ServiceList does not contain correct number of services", 3, sl.size());
        // Make sure it removed the correct service
        boolean found = false;
        ServiceIterator it = sl.createServiceIterator();
        while (it.hasNext())
        {
            String svcLoc = it.nextService().getLocator().toString();
            if (loc.equals(svcLoc))
            {
                found = true;
                break;
            }

        }
        assertFalse("Service (" + loc + ") was not removed properly", found);
    }

    public void testServiceDescriptionAvailable()
    {
        // TODO: Finish
        // How do we test to see that this is reflected in the stack?
    }

    public void testUpdateServiceUnknownKnown() throws Exception
    {
        String loc = "ocap://0x1";
        ServiceReference oldRef = new MockServiceReference(loc, loc);
        int freq = csidb.transportStream7.getFrequency();
        int prog = csidb.serviceDetails33.getProgramNumber();
        int mod = csidb.transportStream7.getModulationFormat();
        OcapLocator locator = new OcapLocator(freq, prog, mod);
        MockKnownServiceReference newRef = new MockKnownServiceReference(loc, loc, locator);
        ServiceDescription sd = new MockServiceDescription(new MultilingualString[] { new MultilingualString(
                "Service 0x1", "eng") });
        sp.cannedUpdateServiceReference(oldRef, newRef, sd);

        ServiceList sl = SIManager.createInstance().filterServices(new ServiceFilter()
        {

            public boolean accept(Service service)
            {
                return "Service 0x1".equals(service.getName());
            }

        });
        assertEquals("ServiceList size is incorrect", 1, sl.size());
        ServiceExt service = (ServiceExt) sl.getService(0);
        assertNotNull(service);
        assertEquals("Preferred language is incorrect", null, service.getPreferredLanguage());

        ServiceDetailsExt sde = (ServiceDetailsExt) service.getDetails();
        assertEquals("Name does not match", sd.getLongName("eng").getString(), service.getName());
        assertEquals("Long name is incorrect", sd.getLongName("eng").getString(), sde.getLongName());
        assertEquals("ServiceType does not match", sd.getServiceType(), sde.getServiceType());
        assertEquals("DeliverySystemType is incorrect", sd.getDeliverySystemType(), sde.getDeliverySystemType());
        assertEquals("Service does not match", service, sde.getService());
        assertNull("Program Schedule should be null", sde.getProgramSchedule());
        assertFalse("Service should not be analog", sde.isAnalog());
        assertNotNull("TransportStream should be available", sde.getTransportStream());
        assertTrue("SourceId should not be -1", sde.getSourceID() != -1);
        assertTrue("Program number should not be -1", -1 != sde.getProgramNumber());
        assertTrue("PcrPID is incorrect", -1 != sde.getPcrPID());

        // Tune so we can get components
        NetworkInterfaceController nic = new NetworkInterfaceController(new MockResourceClient());
        ExtendedNetworkInterface ni = (ExtendedNetworkInterface) cnm.getNetworkInterfaceManager()
                .getNetworkInterfaces()[0];
        nic.reserve(ni, null);
        nic.tune(sde.getDavicTransportStream(ni));

        try
        {
            sde.getServiceDescription();
            fail("Expected SIRequestException when calling getServiceDescription()");
        }
        catch (SIRequestException expected)
        {
        }

        // Now for the exception based ones
        try
        {
            sde.getCarouselComponent();
        }
        catch (SIRequestException ex)
        {
            fail("Received SIRequestException when calling getCarouselComponent(): " + ex.getMessage());
        }

        try
        {
            // FIXME: this assumes CannedSIDatabase.serviceComponent69
            sde.getCarouselComponent(46);
        }
        catch (SIRequestException ex)
        {
            fail("Received SIRequestException when calling getCarouselComponent(int): " + ex.getMessage());
        }

        try
        {
            sde.getComponents();
        }
        catch (SIRequestException ex)
        {
            fail("Received SIRequestException when calling getComponents(): " + ex.getMessage());
        }

        try
        {
            sde.getComponentByAssociationTag(1);
            fail("No canned components have association tags");
        }
        catch (SIRequestException expected)
        {
        }

        try
        {
            sde.getDefaultMediaComponents();
        }
        catch (SIRequestException ex)
        {
            fail("Received SIRequestException when calling getDefaultMediaComponents(): " + ex.getMessage());
        }

    }

    public void testUpdateServiceServiceDescription() throws Exception
    {
        String loc = "ocap://0x1";
        MockServiceReference oldRef = new MockServiceReference(loc, loc);
        sp.cannedUpdateServiceReference(oldRef, oldRef, null);

        ServiceList sl = SIManager.createInstance().filterServices(new ServiceFilter()
        {

            public boolean accept(Service service)
            {
                return service.getServiceType() == ServiceType.UNKNOWN;
            }

        });
        assertEquals("ServiceList size is incorrect", 1, sl.size());
        ServiceExt service = (ServiceExt) sl.getService(0);
        ServiceDetailsExt sde = (ServiceDetailsExt) service.getDetails();
        assertEquals("Name does not match", "", service.getName());
        assertEquals("Long name is incorrect", "", sde.getLongName());
        assertEquals("ServiceType does not match", ServiceType.UNKNOWN, sde.getServiceType());
        assertEquals("DeliverySystemType is incorrect", DeliverySystemType.UNKNOWN, sde.getDeliverySystemType());

        MockServiceDescription sd = new MockServiceDescription(new MultilingualString[] { new MultilingualString(
                "New Service 0x1", "eng") });
        sp.cannedUpdateServiceReference(oldRef, oldRef, sd);

        sl = SIManager.createInstance().filterServices(new ServiceFilter()
        {

            public boolean accept(Service service)
            {
                return "New Service 0x1".equals(service.getName());
            }

        });
        assertEquals("ServiceList size is incorrect", 1, sl.size());
        service = (ServiceExt) sl.getService(0);
        sde = (ServiceDetailsExt) service.getDetails();
        assertEquals("Name does not match", sd.getLongName("eng").getString(), service.getName());
        assertEquals("Long name is incorrect", sd.getLongName("eng").getString(), sde.getLongName());
        assertEquals("ServiceType does not match", sd.getServiceType(), sde.getServiceType());
        assertEquals("DeliverySystemType is incorrect", sd.getDeliverySystemType(), sde.getDeliverySystemType());
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
