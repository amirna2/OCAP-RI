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

package org.dvb.dsmcc;

import javax.tv.service.SIManager;
import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.SIRetrievable;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceComponent;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.transport.TransportStream;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.davic.net.Locator;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceController;
import org.davic.net.tuning.NetworkInterfaceEvent;
import org.davic.net.tuning.NetworkInterfaceListener;
import org.davic.net.tuning.NetworkInterfaceTuningOverEvent;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.ocap.net.OcapLocator;

import org.cablelabs.impl.manager.EventDispatchManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.TransportStreamExt;

/**
 * Tests the org.dvb.dsmcc.ServiceDomain class.
 * 
 * @author Brent Thompson
 * @author Todd Earles
 */
public class ServiceDomainTest extends TestCase
{
    // ///////////////////////////////////////////////////////////////////////
    // CONSTANTS
    // ///////////////////////////////////////////////////////////////////////

    // TODO(Todd): Need at least 2 carousels on the same service in order to
    // test the non-default carousel.

    // Headend specific fields
    private static int ocSourceID;

    private static int ocComponentIndex;

    private static String ocPath;

    // ///////////////////////////////////////////////////////////////////////
    // MAIN CONTROL
    // ///////////////////////////////////////////////////////////////////////

    private static String ocServiceName;

    private static int ocProgramNumber;

    private static int ocFrequency;

    private static int ocModulationFormat;

    private static int ocCarouselID;

    private static int ocComponentTag;

    private static String ocComponentName;

    private static int ocPID;

    private static NetworkInterfaceController nic;

    /**
     * Main method
     * 
     * @param args
     *            Command line arguments
     */
    public static void main(String[] args) throws Exception
    {
        ManagerManager.getInstance(EventDispatchManager.class);
        tuneOCgetSI();
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

    static
    {
        // Set headend specific hardcoded values
        String headend = System.getProperty("headend", "DesMoinesSA");
        if (headend.equals("DesMoinesSA"))
        {
            ocSourceID = 0x3e8;
            ocComponentIndex = 0;
            String ocPath = "org/cablelabs/apps/stupid/StupidXlet.class";
        }
        else
            fail("Specified headend property '" + headend + "' is not a supported value");
    }

    /**
     * Tune to the OC and get SI
     */
    public static void tuneOCgetSI() throws Exception
    {
        // Get the service
        OcapLocator locator = new OcapLocator(ocSourceID);
        Service service = SIManager.createInstance().getService(locator);
        ServiceDetails details = null;
        TransportStream transportStream = null;
        ServiceComponent[] components = null;

        // Tune to the service
        nic = new NetworkInterfaceController(new ResourceClient()
        {
            public boolean requestRelease(ResourceProxy proxy, Object requestData)
            {
                return false;
            }

            public void release(ResourceProxy proxy)
            {
            }

            public void notifyRelease(ResourceProxy proxy)
            {
                throw new RuntimeException("Lost the tuner");
            }
        });
        nic.reserveFor(locator, null);
        NetworkInterface ni = nic.getNetworkInterface();
        NetworkInterfaceListener nil = new NetworkInterfaceListener()
        {
            synchronized public void receiveNIEvent(NetworkInterfaceEvent e)
            {
                if (e instanceof NetworkInterfaceTuningOverEvent)
                {
                    if (((NetworkInterfaceTuningOverEvent) e).getStatus() == NetworkInterfaceTuningOverEvent.SUCCEEDED)
                    {
                        notify();
                    }
                    else
                        throw new RuntimeException("Cannot tune: " + e);
                }
            }
        };
        synchronized (nil)
        {
            ni.addNetworkInterfaceListener(nil);
            nic.tune(locator);
            nil.wait();
            ni.removeNetworkInterfaceListener(nil);
        }

        // Retrieve the service details
        final Object[] detailsResult = new Object[1];
        SIRequestor detailsRequestor = new SIRequestor()
        {
            synchronized public void notifyFailure(SIRequestFailureType reason)
            {
                throw new RuntimeException("Cannot get service details: " + reason);
            }

            synchronized public void notifySuccess(SIRetrievable[] result)
            {
                detailsResult[0] = result[0];
                notify();
            }
        };
        synchronized (detailsRequestor)
        {
            service.retrieveDetails(detailsRequestor);
            detailsRequestor.wait();
            details = (ServiceDetails) detailsResult[0];
            transportStream = ((ServiceDetailsExt) details).getTransportStream();
        }

        // Retrieve the service components
        final Object[] componentsResult = new Object[1];
        SIRequestor componentsRequestor = new SIRequestor()
        {
            synchronized public void notifyFailure(SIRequestFailureType reason)
            {
                throw new RuntimeException("Cannot get service components: " + reason);
            }

            synchronized public void notifySuccess(SIRetrievable[] result)
            {
                componentsResult[0] = result;
                notify();
            }
        };
        synchronized (componentsRequestor)
        {
            details.retrieveComponents(componentsRequestor);
            componentsRequestor.wait();
            components = (ServiceComponent[]) componentsResult[0];
        }

        // Extract SI needed by tests
        ocServiceName = service.getName();
        ocProgramNumber = ((ServiceDetailsExt) details).getProgramNumber();
        ocFrequency = ((TransportStreamExt) transportStream).getFrequency();
        ocModulationFormat = ((TransportStreamExt) transportStream).getModulationFormat();
        ocCarouselID = ((ServiceComponentExt) components[ocComponentIndex]).getCarouselID();
        ocComponentTag = ((ServiceComponentExt) components[ocComponentIndex]).getComponentTag();
        ocComponentName = ((ServiceComponentExt) components[ocComponentIndex]).getName();
        ocPID = ((ServiceComponentExt) components[ocComponentIndex]).getPID();

        // Dump information about the test OC
        System.out.println("ocServiceName = " + ocServiceName);
        System.out.println("ocFrequency = " + ocFrequency);
        System.out.println("ocModulationFormat = " + ocModulationFormat);
        System.out.println("ocCarouselID = " + ocCarouselID);
        System.out.println("ocComponentTag = " + ocComponentTag);
        System.out.println("ocComponentName = " + ocComponentName);
        System.out.println("ocPID = " + ocPID);
    }

    /**
     * Construct the default test suite
     * 
     * @return A test suite including all tests
     */
    public static Test suite()
    {
        return new TestSuite(ServiceDomainTest.class);
    }

    /**
     * Construct the test suite consisting of the named tests
     * 
     * @param tests
     *            The named tests to be included in the suite
     * @return A test suite including only the named tests
     */
    public static Test suite(String[] tests) throws Exception
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(ServiceDomainTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new ServiceDomainTest(tests[i]));
            return suite;
        }
    }

    /**
     * Unit test constructor
     * 
     * @param name
     *            The name of the unit test
     */
    public ServiceDomainTest(String name)
    {
        super(name);
    }

    // ///////////////////////////////////////////////////////////////////////
    // TESTS
    // ///////////////////////////////////////////////////////////////////////

    /**
     * Test constructor
     */
    public void testConstructor()
    {
        ServiceDomain carousel = new ServiceDomain();
        assertNotNull("ServiceDomain object wasn't instantiated", carousel);
        assertFalse("ServiceDomain object shouldn't initially be attached", carousel.isAttached());
        assertNull("ServiceDomain object shouldn't initially have a locator", carousel.getLocator());
    }

    /**
     * Test attach to inband carousel by source ID and carousel ID using
     * OcapLocator
     * 
     * @todo Disabled per bug 5128
     */
    public void xxxtestAttachBySourceIdCarouselId() throws Exception
    {
        ServiceDomain carousel = new ServiceDomain();
        OcapLocator loc = new OcapLocator(ocSourceID);
        OcapLocator loc2 = new OcapLocator(ocSourceID, -1, new int[] { ocComponentTag }, null);
        carousel.attach(loc, ocCarouselID);
        verifyAttached(carousel, loc2, ocSourceID, ocCarouselID, true);
        carousel.detach();
        verifyDetached(carousel);
    }

    /**
     * Test attach to inband carousel by service name and carousel ID using
     * OcapLocator
     */
    public void XXXtestAttachByServiceNameCarouselId() throws Exception
    {
        ServiceDomain carousel = new ServiceDomain();
        OcapLocator loc = new OcapLocator(ocServiceName, new int[0], -1, null);
        OcapLocator loc2 = new OcapLocator(ocSourceID, -1, new int[] { ocComponentTag }, null);
        carousel.attach(loc, ocCarouselID);
        verifyAttached(carousel, loc2, ocSourceID, ocCarouselID, true);
        carousel.detach();
        verifyDetached(carousel);
    }

    /**
     * Test attach to inband carousel by frequency and carousel ID using
     * OcapLocator
     * 
     * @todo Disabled per bug 5128
     */
    public void xxxtestAttachByFreqCarouselId() throws Exception
    {
        ServiceDomain carousel = new ServiceDomain();
        OcapLocator loc = new OcapLocator(ocFrequency, ocProgramNumber, ocModulationFormat);
        OcapLocator loc2 = new OcapLocator(ocSourceID, -1, new int[] { ocComponentTag }, null);
        carousel.attach(loc, ocCarouselID);
        verifyAttached(carousel, loc2, ocSourceID, ocCarouselID, true);
        carousel.detach();
        verifyDetached(carousel);
    }

    /**
     * Test attach to inband carousel by source ID and component PID using
     * OcapLocator
     * 
     * @todo Disabled per bug 5128
     */
    public void xxxtestAttachBySourceIdComponentPid() throws Exception
    {
        ServiceDomain carousel = new ServiceDomain();
        OcapLocator loc = new OcapLocator(ocSourceID, new int[] { ocPID }, -1, null);
        OcapLocator loc2 = new OcapLocator(ocSourceID, -1, new int[] { ocComponentTag }, null);
        carousel.attach(loc);
        // TODO(Todd): Should loc or loc2 be expected (see email on 10/12/2005)
        verifyAttached(carousel, loc, ocSourceID, ocCarouselID, true);
        carousel.detach();
        verifyDetached(carousel);
    }

    /**
     * Test attach to inband carousel by service name and component PID using
     * OcapLocator
     */
    public void XXXtestAttachByServiceNameComponentPid() throws Exception
    {
        ServiceDomain carousel = new ServiceDomain();
        OcapLocator loc = new OcapLocator(ocServiceName, new int[] { ocPID }, -1, null);
        OcapLocator loc2 = new OcapLocator(ocSourceID, -1, new int[] { ocComponentTag }, null);
        carousel.attach(loc);
        // TODO(Todd): Should loc or loc2 be expected (see email on 10/12/2005)
        verifyAttached(carousel, loc, ocSourceID, ocCarouselID, true);
        carousel.detach();
        verifyDetached(carousel);
    }

    /**
     * Test attach to inband carousel by frequency and component PID using
     * OcapLocator
     * 
     * @todo Disabled per bug 5128
     */
    public void xxxtestAttachByFreqComponentPid() throws Exception
    {
        ServiceDomain carousel = new ServiceDomain();
        OcapLocator loc = new OcapLocator(ocFrequency, ocProgramNumber, ocModulationFormat, new int[] { ocPID }, -1,
                null);
        OcapLocator loc2 = new OcapLocator(ocFrequency, ocProgramNumber, ocModulationFormat, -1,
                new int[] { ocComponentTag }, null);
        carousel.attach(loc);
        // TODO(Todd): Should loc or loc2 be expected (see email on 10/12/2005)
        verifyAttached(carousel, loc, ocSourceID, ocCarouselID, true);
        carousel.detach();
        verifyDetached(carousel);
    }

    /**
     * Test attach to inband carousel by source ID and component tag using
     * OcapLocator
     * 
     * @todo Disabled per bug 5128
     */
    public void xxxtestAttachBySourceIdComponentTag() throws Exception
    {
        ServiceDomain carousel = new ServiceDomain();
        OcapLocator loc = new OcapLocator(ocSourceID, -1, new int[] { ocComponentTag }, null);
        OcapLocator loc2 = new OcapLocator(ocSourceID, -1, new int[] { ocComponentTag }, null);
        carousel.attach(loc);
        // TODO(Todd): Should loc or loc2 be expected (see email on 10/12/2005)
        verifyAttached(carousel, loc, ocSourceID, ocCarouselID, true);
        carousel.detach();
        verifyDetached(carousel);
    }

    /**
     * Test attach to inband carousel by service name and component tag using
     * OcapLocator
     */
    public void XXXtestAttachByServiceNameComponentTag() throws Exception
    {
        ServiceDomain carousel = new ServiceDomain();
        OcapLocator loc = new OcapLocator(ocServiceName, -1, new int[] { ocComponentTag }, null);
        OcapLocator loc2 = new OcapLocator(ocSourceID, -1, new int[] { ocComponentTag }, null);
        carousel.attach(loc);
        // TODO(Todd): Should loc or loc2 be expected (see email on 10/12/2005)
        verifyAttached(carousel, loc, ocSourceID, ocCarouselID, true);
        carousel.detach();
        verifyDetached(carousel);
    }

    /**
     * Test attach to inband carousel by frequency and component tag using
     * OcapLocator
     * 
     * @todo Disabled per bug 5128
     */
    public void xxxtestAttachByFreqComponentTag() throws Exception
    {
        ServiceDomain carousel = new ServiceDomain();
        OcapLocator loc = new OcapLocator(ocFrequency, ocProgramNumber, ocModulationFormat, -1,
                new int[] { ocComponentTag }, null);
        OcapLocator loc2 = new OcapLocator(ocFrequency, ocProgramNumber, ocModulationFormat, -1,
                new int[] { ocComponentTag }, null);
        carousel.attach(loc);
        // TODO(Todd): Should loc or loc2 be expected (see email on 10/12/2005)
        verifyAttached(carousel, loc, ocSourceID, ocCarouselID, true);
        carousel.detach();
        verifyDetached(carousel);
    }

    /**
     * Test attach to inband carousel by source ID and component name using
     * OcapLocator
     */
    public void XXXtestAttachBySourceIdComponentName() throws Exception
    {
        ServiceDomain carousel = new ServiceDomain();
        OcapLocator loc = new OcapLocator(ocSourceID, new String[] { ocComponentName }, -1, null);
        OcapLocator loc2 = new OcapLocator(ocSourceID, -1, new int[] { ocComponentTag }, null);
        carousel.attach(loc);
        // TODO(Todd): Should loc or loc2 be expected (see email on 10/12/2005)
        verifyAttached(carousel, loc, ocSourceID, ocCarouselID, true);
        carousel.detach();
        verifyDetached(carousel);
    }

    /**
     * Test attach to inband carousel by service name and component name using
     * OcapLocator
     */
    public void XXXtestAttachByServiceNameComponentName() throws Exception
    {
        ServiceDomain carousel = new ServiceDomain();
        OcapLocator loc = new OcapLocator(ocServiceName, new String[] { ocComponentName }, -1, null);
        OcapLocator loc2 = new OcapLocator(ocSourceID, -1, new int[] { ocComponentTag }, null);
        carousel.attach(loc);
        // TODO(Todd): Should loc or loc2 be expected (see email on 10/12/2005)
        verifyAttached(carousel, loc, ocSourceID, ocCarouselID, true);
        carousel.detach();
        verifyDetached(carousel);
    }

    /**
     * Test attach to inband carousel by frequency and component name using
     * OcapLocator
     */
    public void XXXtestAttachByFreqComponentName() throws Exception
    {
        ServiceDomain carousel = new ServiceDomain();
        OcapLocator loc = new OcapLocator(ocFrequency, ocProgramNumber, ocModulationFormat,
                new String[] { ocComponentName }, -1, null);
        OcapLocator loc2 = new OcapLocator(ocFrequency, ocProgramNumber, ocModulationFormat, -1,
                new int[] { ocComponentTag }, null);
        carousel.attach(loc);
        // TODO(Todd): Should loc or loc2 be expected (see email on 10/12/2005)
        verifyAttached(carousel, loc, ocSourceID, ocCarouselID, true);
        carousel.detach();
        verifyDetached(carousel);
    }

    /**
     * Test attach to default inband carousel by source ID using OcapLocator
     * 
     * @todo Disabled per bug 5128
     */
    public void xxxtestAttachBySourceId() throws Exception
    {
        ServiceDomain carousel = new ServiceDomain();
        OcapLocator loc = new OcapLocator(ocSourceID);
        OcapLocator loc2 = new OcapLocator(ocSourceID, -1, new int[] { ocComponentTag }, null);
        carousel.attach(loc);
        // TODO(Todd): Should loc or loc2 be expected (see email on 10/12/2005)
        verifyAttached(carousel, loc, ocSourceID, ocCarouselID, true);
        carousel.detach();
        verifyDetached(carousel);
    }

    /**
     * Test attach to default inband carousel by service name using OcapLocator
     */
    public void XXXtestAttachByServiceName() throws Exception
    {
        ServiceDomain carousel = new ServiceDomain();
        OcapLocator loc = new OcapLocator(ocServiceName, new String[0], -1, null);
        OcapLocator loc2 = new OcapLocator(ocSourceID, -1, new int[] { ocComponentTag }, null);
        carousel.attach(loc);
        // TODO(Todd): Should loc or loc2 be expected (see email on 10/12/2005)
        verifyAttached(carousel, loc, ocSourceID, ocCarouselID, true);
        carousel.detach();
        verifyDetached(carousel);
    }

    /**
     * Test attach to default inband carousel by frequency using OcapLocator
     * 
     * @todo Disabled per bug 5128
     */
    public void xxxtestAttachByFreq() throws Exception
    {
        ServiceDomain carousel = new ServiceDomain();
        OcapLocator loc = new OcapLocator(ocFrequency, ocProgramNumber, ocModulationFormat);
        OcapLocator loc2 = new OcapLocator(ocFrequency, ocProgramNumber, ocModulationFormat, -1,
                new int[] { ocComponentTag }, null);
        carousel.attach(loc);
        // TODO(Todd): Should loc or loc2 be expected (see email on 10/12/2005)
        verifyAttached(carousel, loc, ocSourceID, ocCarouselID, true);
        carousel.detach();
        verifyDetached(carousel);
    }

    /**
     * Test attach to inband carousel by carousel ID and source ID using NSAP
     */
    public void XXXtestAttachByInbandNsap() throws Exception
    {
        ServiceDomain carousel = new ServiceDomain();
        byte[] nsap = { 0x00, // AFI
                0x00, // Type
                0x00, 0x00, 0x00, 0x01, // carouselId
                0x01, // specifierType
                0x00, 0x01, 0x5A, // specifierData
                0x00, 0x00, // transport_stream_id
                0x00, 0x00, // original_network_id
                0x07, (byte) 0xD4, // service_id
                -1, -1, -1, -1 // reserved
        };
        OcapLocator loc2 = new OcapLocator(ocSourceID, -1, new int[] { ocComponentTag }, null);
        carousel.attach(nsap);
        verifyAttached(carousel, loc2, ocSourceID, ocCarouselID, true);
        carousel.detach();
        verifyDetached(carousel);
    }

    /**
     * Test attach to OOB carousel by carousel ID and program number using
     * OcapLocator
     */
    public void XXXtestOobAttachByProgramNumber() throws Exception
    {
        // TODO(Todd): Implement this test (put any HE specific settings in
        // the "constants" section at the top of this file.
    }

    /**
     * Test attach to OOB carousel by carousel ID and program number using OOB
     * NSAP
     */
    public void XXXtestOobAttachByNsap() throws Exception
    {
        // TODO(Todd): Implement this test (put any HE specific settings in
        // the "constants" section at the top of this file.
    }

    /**
     * Test cancellation of in-progress attach
     */
    public void testCancelAttach() throws Exception
    {
        // TODO(Todd): Test that detach causes InterruptedIOException on an
        // attach()
        // that is in progress. The detach needs to be done on a separate thread
        // because the mount is a synchronous call.
    }

    // ///////////////////////////////////////////////////////////////////////
    // HELPER METHODS
    // ///////////////////////////////////////////////////////////////////////

    /**
     * Verify the carousel is attached and operating correctly
     * 
     * @param carousel
     *            The carousel to be verified
     * @param loc
     *            The locator which should be reported by the carousel
     * @param sourceId
     *            The source ID which should be reported by the carousel
     * @param carouselId
     *            The carousel ID which should be reported by the carousel
     */
    private void verifyAttached(ServiceDomain carousel, OcapLocator loc, int sourceId, int carouselId, boolean inband)
            throws Exception
    {
        // Verify attached
        assertTrue("Carousel is not attached", carousel.isAttached());

        // Verify mount point
        assertNotNull("Cannot get mount point", carousel.getMountPoint());

        // Verify connection is available
        // FIXME(Todd): assertTrue("Connection is not available",
        // carousel.isNetworkConnectionAvailable());

        // Verify locator
        Locator l = carousel.getLocator();
        assertTrue("Expected locator '" + loc + "' but got '" + l + "'", l.toExternalForm()
                .equals(loc.toExternalForm()));

        // Verify URL
        // FIXME(Todd): URL url = ServiceDomain.getURL(new
        // OcapLocator(loc.toExternalForm()+"/"+ocPath));
        // FIXME(Todd): assertTrue("Incorrect URL",
        // url.toExternalForm().equals("file:/oc/0/"+ocPath));

        // Verify NSAP address
        byte[] nsap = { 0x00, // AFI
                0x00, // Type
                0x00, 0x00, 0x00, 0x00, // carouselId
                0x01, // specifierType
                0x00, 0x10, 0x00, // specifierData
                0x00, 0x00, // transport_stream_id
                0x00, 0x00, // original_network_id
                0x00, 0x00, // service_id
                0x7f, // multiplex type (high bit) + reserved
                -1, -1, -1 // reserved
        };
        nsap[2] = (byte) ((carouselId >> 24) & 0xFF);
        nsap[3] = (byte) ((carouselId >> 16) & 0xFF);
        nsap[4] = (byte) ((carouselId >> 8) & 0xFF);
        nsap[5] = (byte) (carouselId & 0xFF);
        nsap[14] = (byte) ((sourceId >> 8) & 0xFF);
        nsap[15] = (byte) (sourceId & 0xFF);
        if (!inband) nsap[16] = (byte) 0xFF;
        byte[] carouselNsap = carousel.getNSAPAddress();
        assertEquals("Incorrect NSAP address", carouselNsap, nsap);
    }

    /**
     * Verify that the carousel is detached
     * 
     * @param carousel
     *            The carousel to check
     */
    private void verifyDetached(ServiceDomain carousel)
    {
        assertFalse("Carousel should not be attached", carousel.isAttached());
        assertNull("Locator should be null", carousel.getLocator());
        assertNull("Mount point should be null", carousel.getMountPoint());
        assertFalse("Connection should not be available", carousel.isNetworkConnectionAvailable());
    }

    /**
     * Asserts that two byte arrays contain equal array elements. If they do not
     * an AssertionFailedError is thrown with the given message.
     * 
     * @param message
     *            The message to include when an assertion is generated
     * @param expected
     *            The expected byte array
     * @param actual
     *            The actual byte array
     */
    static private void assertEquals(String message, byte[] expected, byte[] actual)
    {
        if (expected == null && actual == null) return;
        if (expected.length != actual.length) fail(message);
        int i = expected.length;
        while (--i >= 0)
        {
            if (expected[i] != actual[i]) fail(message);
        }
    }
}
