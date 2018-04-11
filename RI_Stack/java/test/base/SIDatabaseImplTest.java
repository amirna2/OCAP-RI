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

import java.util.Date;

import javax.tv.service.SIException;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.navigation.LocatorFilter;
import javax.tv.service.navigation.ServiceFilter;
import javax.tv.service.navigation.ServiceIterator;
import javax.tv.service.navigation.ServiceList;
import javax.tv.service.navigation.SortNotAvailableException;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.davic.net.tuning.NetworkInterfaceController;
import org.davic.net.tuning.NetworkInterfaceEvent;
import org.davic.net.tuning.NetworkInterfaceListener;
import org.davic.net.tuning.NetworkInterfaceTuningEvent;
import org.davic.net.tuning.NetworkInterfaceTuningOverEvent;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.ocap.net.OcapLocator;
import org.ocap.service.AbstractService;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.EventDispatchManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.service.NetworkExt;
import org.cablelabs.impl.service.NetworkHandle;
import org.cablelabs.impl.service.RatingDimensionExt;
import org.cablelabs.impl.service.RatingDimensionHandle;
import org.cablelabs.impl.service.SIChangedEvent;
import org.cablelabs.impl.service.SIChangedListener;
import org.cablelabs.impl.service.SIDatabase;
import org.cablelabs.impl.service.SIDatabaseException;
import org.cablelabs.impl.service.SINotAvailableYetException;
import org.cablelabs.impl.service.SIRequestInvalidException;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceComponentHandle;
import org.cablelabs.impl.service.ServiceDescriptionExt;
import org.cablelabs.impl.service.ServiceDetailsChangedEvent;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceDetailsHandle;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.service.ServiceHandle;
import org.cablelabs.impl.service.TransportExt;
import org.cablelabs.impl.service.TransportHandle;
import org.cablelabs.impl.service.TransportStreamExt;
import org.cablelabs.impl.service.TransportStreamHandle;
import org.cablelabs.impl.service.javatv.transport.TransportImpl;

/**
 * Standalone/JUnit test for the SIDatabaseImpl class. This test can be run by
 * itself as a main application or as a unit test. <br>
 * To run this program, put it in the mpeenv.ini file of whichever TC is being
 * tested, e.g. <br>
 * <br>
 * MainClassArgs.0=SIDatabaseImplTest <br>
 * <br>
 * In addition this class accepts command line parameters to only select a
 * service based on the source ID or frequency/program number/modulation format
 * (running as a standalone application only). <br>
 * To select a service by sourceID 0x44d: <br>
 * <br>
 * MainClassArgs.0=SIDatabaseImplTest<br>
 * MainClassArgs.1=0x44d <br>
 * NOTE: the '0x' prefix may be omitted, but the program assumes that the value
 * will always be in hex (base16). <br>
 * <br>
 * To select a service by frequency/program number/modulation format: <br>
 * <br>
 * MainClassArgs.0=SIDatabaseImplTest<br>
 * MainClassArgs.1=55000000<br>
 * MainClassArgs.2=7<br>
 * MainClassArgs.3=16 <br>
 * NOTE: The values cannot be in hex and are presumed to be in decimal (base10).
 * Also, the values must be in the order frequency, program number, modulation
 * format. <br>
 * 
 * 
 * @author Joshua Keplinger
 * 
 */
public class SIDatabaseImplTest extends TestCase
{

    static
    {
        ManagerManager.getInstance(EventDispatchManager.class);
    }

    /**
     * Main entry point into the program. Possible arguments (these examples may
     * or may not be valid values): - Select by source ID : 0x3e8 - Select by
     * freq/prognum/modformat : 5000 1 2
     * 
     * @param args
     *            The parameters to pass to the program
     */
    public static void main(String[] args)
    {
        SIDatabaseImplTest.args = args;
        TestRunner.run(suite());
        System.exit(0);
    }

    public static TestSuite suite()
    {
        return new TestSuite(SIDatabaseImplTest.class);
    }

    public void setUp()
    {
        sm = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);

        // Get the CallerContext so we can add the listener
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        cc = ccm.getCurrentContext();

        sidb = sm.getSIDatabase();
        sial = new SIChangedListener()
        {
            public synchronized void notifyChanged(SIChangedEvent event)
            {
                if (event instanceof ServiceDetailsChangedEvent)
                {
                    log("Received ServiceDetailsChangedEvent");
                    if (failed) log("NetworkInterface failed the tune");
                    // assertFalse("NetworkInterface failed the tune", failed);
                    notify();
                }
            }

        };
        sidb.addSIChangedListener(sial, cc);

        // Create the NetworkInterfaceController

        nic = new NetworkInterfaceController(new ResourceClient()
        {

            public boolean requestRelease(ResourceProxy proxy, Object requestData)
            {
                return true;
            }

            public void release(ResourceProxy proxy)
            {
            }

            public void notifyRelease(ResourceProxy proxy)
            {
            }
        });

        nil = new NetworkInterfaceListener()
        {

            public void receiveNIEvent(NetworkInterfaceEvent anEvent)
            {
                if (anEvent instanceof NetworkInterfaceTuningEvent)
                {
                    log("*** Received NetworkInterfaceTuningEvent ***");
                }
                if (anEvent instanceof NetworkInterfaceTuningOverEvent)
                {
                    NetworkInterfaceTuningOverEvent e = (NetworkInterfaceTuningOverEvent) anEvent;
                    String reason = "";
                    if (e.getStatus() == NetworkInterfaceTuningOverEvent.FAILED)
                    {
                        failed = true;
                        reason = "FAILED";
                    }
                    if (e.getStatus() == NetworkInterfaceTuningOverEvent.SUCCEEDED)
                    {
                        failed = false;
                        reason = "SUCCEEDED";
                    }
                    log("*** Received NetworkInterfaceTuningOverEvent reason " + reason + " ***");
                }
            }

        };
    }

    public void tearDown()
    {
        sm = null;
        sidb.removeSIChangedListener(sial, cc);
        sial = null;
        cc = null;
        if (nic.getNetworkInterface() != null) nic.getNetworkInterface().removeNetworkInterfaceListener(nil);
        nil = null;
        nic = null;
        failed = false;
    }

    /**
     * Dumps all of the SI regarding RatingDimension.
     */
    public void testRatingDimensions()
    {
        try
        {
            RatingDimensionHandle[] handles = sidb.getSupportedDimensions();
            for (int i = 0; i < handles.length; i++)
            {
                RatingDimensionExt rd = sidb.createRatingDimension(handles[i]);
                short numLvls = rd.getNumberOfLevels();
                log("******************************************************************");
                log("*                   RatingDimension Information");
                log("*");
                assertNotNull("RatingDimension name should not be null", rd.getDimensionName());
                log("* RD Name: " + rd.getDimensionName());
                assertTrue("RatingDimension number of levels should be greater than zero", rd.getNumberOfLevels() > 0);
                log("* RD NumLevels: " + numLvls);
                for (int j = 0; j < numLvls; j++)
                {
                    log("* Dimension Level " + j + ":");
                    String[] descs = rd.getRatingLevelDescription((short) j);
                    assertEquals("Returned size of string array of description is incorrect", 2, descs.length);
                    log("*   " + descs[0] + " - " + descs[1]);
                }
                assertNotNull("RatingDimension handle should not be null", handles[i]);
                log("* RD Handle: " + handles[i]);
                log("*");
                log("******************************************************************");
                try
                {
                    rd.getRatingLevelDescription((short) 1000);
                    fail("Out of range number should've cause exception");
                }
                catch (Exception ex)
                {
                    // Expected
                }
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    /**
     * Dumps all of the Transport information to the console.
     */
    public void testTransportsTransportStreamsAndNetworks()
    {
        log("******************************************************************");
        log("*            Transport and Transport Stream information          *");
        log("******************************************************************");
        log("");
        try
        {
            TransportHandle[] transports = sidb.getAllTransports();
            for (int i = 0; i < transports.length; i++)
            {
                TransportImpl transport = (TransportImpl) sidb.createTransport(transports[i]);
                log("******************************************************************");

                dumpTransport(transport);

                // try to dump the transport streams
                TransportStreamHandle[] tsHandles = sidb.getTransportStreamsByTransport(transports[i]);
                for (int j = 0; j < tsHandles.length; j++)
                {
                    TransportStreamExt ts = sidb.createTransportStream(tsHandles[j]);
                    dumpTransportStream(ts);
                    assertTrue("TransportStream's Transport does not match expected value",
                            transport.equals(ts.getTransport()));
                }

                // try to dump the networks
                NetworkHandle[] netHandles = sidb.getNetworksByTransport(transports[i]);
                for (int j = 0; j < netHandles.length; j++)
                {
                    NetworkExt net = sidb.createNetwork(netHandles[j]);
                    dumpNetwork(net);
                    assertTrue("Networks's Transport does not match expected value",
                            transport.equals(net.getTransport()));
                }
                log("******************************************************************");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Dumps all of the Service information to the console.
     */
    public void testServiceList()
    {
        ServiceList services = serviceSetup();

        ServiceIterator it = services.createServiceIterator();
        int i = 0;
        try
        {
            while (it.hasNext())
            {
                // Increment the service counter
                i++;
                // Get our next service
                ServiceExt service = (ServiceExt) it.nextService();
                log("******************************************************************");
                log("*                       Services - Counter: (" + i + ")");
                dumpService(service);

                ServiceDetailsHandle[] handles = sidb.getServiceDetailsByService(service.getServiceHandle());

                ServiceDetailsExt[] details = new ServiceDetailsExt[handles.length];
                for (int j = 0; j < handles.length; j++)
                {
                    details[j] = sidb.createServiceDetails(handles[j]);
                    dumpServiceDetails(details[j]);
                    assertEquals("ServiceDetails' Service does not match expected value", service,
                            details[j].getService());
                    assertEquals("ServiceType does not match", service.getServiceType(), details[j].getServiceType());
                }
                log("******************************************************************");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Dumps all of the In-band SI to the console. This information is grouped
     * by ServiceDetails.
     * 
     * @todo disabled per 4595
     */
    public void testInBandSI() throws Exception
    {
        ServiceList services = serviceSetup();

        ServiceIterator it = services.createServiceIterator();

        try
        {
            ServiceDetailsExt[] details = null;
            while (it.hasNext())
            {
                ServiceExt service = (ServiceExt) it.nextService();
                tune(service);
                log("******************************************************************");
                // dumpService(service);

                /*
                 * Service Detail Section
                 */
                ServiceDetailsHandle[] handles = null;
                try
                {
                    try
                    {
                        handles = sidb.getServiceDetailsByService(service.getServiceHandle());
                    }
                    catch (SINotAvailableYetException ex)
                    {
                        synchronized (sial)
                        {
                            // Wait for the SIAcquiredEvent
                            sial.wait();
                            // Try again.. if it fails, it will jump out to the
                            // outer catch
                            handles = sidb.getServiceDetailsByService(service.getServiceHandle());
                        }
                    }

                    details = new ServiceDetailsExt[handles.length];
                    for (int j = 0; j < handles.length; j++)
                    {
                        details[j] = sidb.createServiceDetails(handles[j]);
                        assertEquals("ServiceDetails' Service does not match expected value", service,
                                details[j].getService());
                        /*
                         * ServiceDescription Section
                         */
                        ServiceDescriptionExt sdesc = sidb.createServiceDescription(handles[j]);

                        TransportStreamExt ts = (TransportStreamExt) details[j].getTransportStream();
                        // dumpTransportStream(ts);
                        if (ts.getModulationFormat() != 255)
                        {
                            /*
                             * ServiceComponent Section
                             */
                            ServiceComponentHandle[] scHandles = { null };
                            try
                            {
                                scHandles = sidb.getServiceComponentsByServiceDetails(handles[j]);
                            }
                            catch (SINotAvailableYetException ex)
                            {
                                synchronized (sial)
                                {
                                    // Wait for the SIAcquiredEvent
                                    sial.wait();
                                    // Try again.. if it fails, it will jump out
                                    // to the outer catch
                                    scHandles = sidb.getServiceComponentsByServiceDetails(handles[j]);
                                }
                            }
                            catch (Exception ex)
                            {
                                fail("Exception caught while getting SC Handles: " + ex.getClass() + " - "
                                        + ex.getMessage());
                            }

                            dumpService(service);
                            dumpServiceDetails(details[j]);
                            dumpServiceDescription(sdesc);

                            for (int k = 0; k < scHandles.length; k++)
                            {
                                ServiceComponentExt comp = sidb.createServiceComponent(scHandles[k]);
                                dumpComponent(comp);
                                assertEquals("ServiceComponent's ServiceDetails does not match expected value",
                                        details[j], comp.getServiceDetails());
                                assertEquals("ServiceComponent's Service does not match expected value", service,
                                        comp.getService());
                            }

                            /*
                             * Carousel Component Section
                             */
                            ServiceComponentHandle scHandle = null;

                            try
                            {
                                scHandle = sidb.getCarouselComponentByServiceDetails(handles[j]);
                                ServiceComponentExt comp = sidb.createServiceComponent(scHandle);
                                dumpComponent(comp);
                            }
                            catch (SINotAvailableYetException ex)
                            {
                                synchronized (sial)
                                {
                                    // Wait for the SIAcquiredEvent
                                    sial.wait();
                                    // Try again.. if it fails, it will jump out
                                    // to the outer catch
                                    scHandle = sidb.getCarouselComponentByServiceDetails(handles[j]);

                                    ServiceComponentExt comp = sidb.createServiceComponent(scHandle);
                                    dumpComponent(comp);
                                    assertEquals("ServiceDetails' Service does not match expected value", details[j],
                                            comp.getServiceDetails());
                                    assertEquals("ServiceDetails' Service does not match expected value", service,
                                            comp.getService());
                                }
                            }
                            catch (SIRequestInvalidException ex)
                            {
                                log("*  No Default Carousel Components Available");
                            }
                        }
                        else
                        // mod format is 255
                        {
                            dumpService(service);
                            dumpServiceDetails(details[j]);
                            dumpServiceDescription(sdesc);
                        }

                        assertTrue("TransportStreamID should be greater than zero", ts.getTransportStreamID() > 0);
                        // ServiceDetailsExt detail =
                        // sidb.createServiceDetails(handles[j]);
                        // ts = (TransportStreamExt)detail.getTransportStream();
                        dumpTransportStream(ts);

                        NetworkExt network = (NetworkExt) ts.getNetwork();
                        dumpNetwork(network);
                    }
                }
                catch (Exception e)
                {
                    fail("Exception caught: " + e.getClass() + " - " + e.getMessage());
                }
                log("*");
                log("******************************************************************");

                // Release the NIC
                nic.release();
            }
        }
        catch (Exception e)
        {
            nic.release();
            fail("Exception occurred: " + e.getClass() + " - " + e.getMessage());
        }
    }

    /**
     * Dumps and tests some DynamiceServices.
     */
    public void testDynamicServices() throws Exception
    {
        int freq = 65432198;
        int prognum = 10;
        int modformat = 16;
        ServiceHandle handle = sidb.getServiceByProgramNumber(freq, modformat, prognum);
        ServiceExt service = sidb.createService(handle);
        OcapLocator loc = (OcapLocator) service.getLocator();
        assertEquals("Frequency does not match", freq, loc.getFrequency());
        assertEquals("Program Number does not match", prognum, loc.getProgramNumber());
        assertEquals("Modulation Format does not match", modformat, loc.getModulationFormat());
        assertEquals("SourceID is incorrect", -1, loc.getSourceID());
        dumpService(service);
        log("*********************************************************");

        // Now we're going to try to get an existing service using it's
        // freq/prog/mod.
        ServiceList sl = serviceSetup();
        if (sl.size() > 0)
        {
            // Just grab the first one off the list
            service = (ServiceExt) sl.getService(0);
            tune(service);
            ServiceDetailsHandle[] handles = null;
            try
            {
                handles = sidb.getServiceDetailsByService(service.getServiceHandle());
            }
            catch (SINotAvailableYetException ex)
            {
                synchronized (sial)
                {
                    // Wait for the SIAcquiredEvent
                    sial.wait();
                    // Try again.. if it fails, it will jump out to the outer
                    // catch
                    handles = sidb.getServiceDetailsByService(service.getServiceHandle());
                }
            }
            assertNotNull("No ServiceDetails available", handles);
            assertTrue("Handles array has no data", handles.length > 0);
            ServiceDetailsExt details = sidb.createServiceDetails(handles[0]);
            assertEquals("ServiceDetails' Service does not match expected value", service, details.getService());
            TransportStreamExt ts = (TransportStreamExt) details.getTransportStream();
            freq = ts.getFrequency();
            prognum = details.getProgramNumber();
            modformat = ts.getModulationFormat();
            handle = sidb.getServiceByProgramNumber(freq, prognum, modformat);
            service = sidb.createService(handle);
            loc = (OcapLocator) service.getLocator();
            assertEquals("Frequency does not match", freq, loc.getFrequency());
            assertEquals("Program Number does not match", prognum, loc.getProgramNumber());
            assertEquals("Modulation Format does not match", modformat, loc.getModulationFormat());
            assertEquals("SourceID is incorrect", -1, loc.getSourceID());
            dumpService(service);
        }
        else
        {
            log("No Services available to test for dynamic service creation");
        }
        log("*********************************************************");

        // Try to get a service using an outrageous frequency
        freq = 1;
        prognum = 2;
        modformat = 8;
        try
        {
            sidb.getServiceByProgramNumber(freq, prognum, modformat);
            fail("Expected SIRequestInvalidException using 1 as frequency");
        }
        catch (SIDatabaseException e)
        {
            // Expected
        }
    }

    // Dump support methods

    /**
     * Dumps the contents of the supplied Transport
     * 
     * @param transport
     */
    private void dumpTransport(TransportExt transport)
    {

        log("*                   Transport Information");
        log("*");
        assertTrue("TransportID should be greater than zero", transport.getTransportID() > 0);
        log("* TransportID: " + transport.getTransportID());
        assertNotNull("Transport DeliverySystemType should not be null", transport.getDeliverySystemType());
        log("* DeliverySystemType: " + transport.getDeliverySystemType());
        assertNotNull("Transport Handle should not be null", transport.getTransportHandle());
        log("* Transport Handle: " + transport.getTransportHandle());
        log("*");
    }

    /**
     * Dumps the contents of the supplied Service
     * 
     * @param service
     */
    private void dumpService(ServiceExt service)
    {
        log("*                       Service Information");
        log("*");
        log("* Service Name: " + service.getName());
        log("* Service Number: " + service.getServiceNumber());
        log("* Service MinorNumber: " + service.getMinorNumber());
        assertNotNull("Service ServiceType cannot be null", service.getServiceType());
        log("* Service Type: " + service.getServiceType());
        log("* Service hasMultipleInstances: " + service.hasMultipleInstances());
        assertNotNull("Service Locator cannot be null", service.getLocator());
        log("* Service Locator: " + service.getLocator());
        assertNotNull("Service Handle cannot be null", service.getServiceHandle());
        log("* Service Handle: " + service.getServiceHandle());
        log("*");
    }

    /**
     * Dumps the contents of the supplied ServiceDetails
     * 
     * @param details
     */
    private void dumpServiceDetails(ServiceDetailsExt details)
    {
        log("*");
        log("*        ServiceDetails Information");
        assertNotNull("ServiceDetails' Long name cannot be null", details.getLongName());
        log("*    SD Long Name: " + details.getLongName());
        int prog = details.getProgramNumber();
        assertTrue("ServiceDetails' Program number must be -1 or greater than zero, instead found " + prog, prog == -1
                || prog > 0);
        log("*    SD Program Number: " + details.getProgramNumber());
        assertTrue("SourceID should be greater than zero", details.getSourceID() > 0);
        log("*    SD SourceID: " + details.getSourceID());
        assertNotNull("ServiceDetails' CA IDs cannot be null", details.getCASystemIDs());
        int[] caIDs = details.getCASystemIDs();
        if (caIDs.length == 0)
            log("*    SD CASystemIDs: NONE");
        else
        {
            log("*    SD CASystemIDs: ");
            for (int k = 0; k < caIDs.length; k++)
            {
                log("*       " + caIDs[k]);
            }
        }
        assertNotNull("ServiceDetails' DeliverySystemType cannot be null", details.getDeliverySystemType());
        log("*    SD DeliverySystemType: " + details.getDeliverySystemType());
        assertNotNull("ServiceDetails' ServiceType cannot be null", details.getServiceType());
        log("*    SD ServiceType: " + details.getServiceType());
        assertNotNull("ServiceDetails' ServiceInformationType cannot be null", details.getServiceInformationType());
        log("*    SD ServiceInformationType: " + details.getServiceInformationType());
        assertNotNull("ServiceDetails' Update time cannot be null", details.getUpdateTime());
        long now = new Date().getTime();
        long oneYearAgo = now - (1000 * 60 * 60 * 24 * 365);
        long oneYearAhead = now + (1000 * 60 * 60 * 24 * 365);
        assertTrue("ServiceDetails' Update time should not be more than a year off",
                (oneYearAgo < details.getUpdateTime().getTime()) && (oneYearAhead > details.getUpdateTime().getTime()));
        log("*    SD UpdateTime: " + details.getUpdateTime());
        assertNotNull("ServiceDetails' Locator cannot be null", details.getLocator());
        log("*    SD Locator: " + details.getLocator());
        assertNotNull("ServiceDetails' handle cannot be null", details.getServiceDetailsHandle());
        log("*    SD Handle: " + details.getServiceDetailsHandle());
        assertNotNull("ServiceDetails' Service cannot be null", details.getService());
        assertNotNull("ServiceDetails' TransportStream cannot be null", details.getTransportStream());
    }

    /**
     * Dumps the contents of the supplied TransportStream
     * 
     * @param ts
     */
    private void dumpTransportStream(TransportStreamExt ts)
    {
        log("*");
        log("*            TransportStream Information");
        assertNotNull("TransportStream Description cannot be null", ts.getDescription());
        log("*        TS Description: " + ts.getDescription());
        assertTrue("TransportStream Frequency should be greater than zero", ts.getFrequency() > 0);
        log("*        TS Frequency: " + ts.getFrequency());
        assertTrue("TransportStream Modulation Format should be greater than zero", ts.getModulationFormat() > 0
                && ts.getModulationFormat() <= 255);
        log("*        TS Modulation Format: " + ts.getModulationFormat());
        log("*        TS ID: " + ts.getTransportStreamID());
        assertNotNull("TransportStream ServiceInformationType cannot be null", ts.getServiceInformationType());
        log("*        TS ServiceInformationType: " + ts.getServiceInformationType());
        assertNotNull("TransportStream UpdateTime cannot be null", ts.getUpdateTime());
        long now = new Date().getTime();
        long oneYearAgo = now - (1000 * 60 * 60 * 24 * 365);
        long oneYearAhead = now + (1000 * 60 * 60 * 24 * 365);
        assertTrue("ServiceDetails' Update time should not be more than a year off", (oneYearAgo < ts.getUpdateTime()
                .getTime())
                && (oneYearAhead > ts.getUpdateTime().getTime()));
        log("*        TS UpdateTime: " + ts.getUpdateTime());
        assertNotNull("TransportStream Locator cannot be null", ts.getLocator());
        log("*        TS Locator: " + ts.getLocator());
        assertNotNull("TransportStream Handle cannot be null", ts.getTransportStreamHandle());
        log("*        TS Handle: " + ts.getTransportStreamHandle());
        assertNotNull("TransportStream's Network cannot be null", ts.getNetwork());
        assertNotNull("TransportStream's Transport cannot be null", ts.getTransport());
    }

    /**
     * Dumps the contents of the supplied Network
     * 
     * @param net
     */
    private void dumpNetwork(NetworkExt net)
    {
        log("*");
        log("*            Network Information");
        assertNotNull("Network Name cannot be null", net.getName());
        log("*        Network Name: " + net.getName());
        assertTrue("NetworkID should be greater than zero", net.getNetworkID() > 0);
        log("*        Network ID: " + net.getNetworkID());
        assertNotNull("Network ServiceInformationType cannot be null", net.getServiceInformationType());
        log("*        Network ServiceInformationType: " + net.getServiceInformationType());
        assertNotNull("Network UpdateTime cannot be null", net.getUpdateTime());
        long now = new Date().getTime();
        long oneYearAgo = now - (1000 * 60 * 60 * 24 * 365);
        long oneYearAhead = now + (1000 * 60 * 60 * 24 * 365);
        assertTrue("ServiceDetails' Update time should not be more than a year off", (oneYearAgo < net.getUpdateTime()
                .getTime())
                && (oneYearAhead > net.getUpdateTime().getTime()));
        log("*        Network UpdateTime: " + net.getUpdateTime());
        assertNotNull("Network Locator cannot be null", net.getLocator());
        log("*        Network Locator: " + net.getLocator());
        assertNotNull("Network Handle cannot be null", net.getNetworkHandle());
        log("*        Network Handle: " + net.getNetworkHandle());
        assertNotNull("Networks's Transport cannot be null", net.getTransport());
    }

    /**
     * Dumps the contents of the supplied ServiceComponent
     * 
     * @param comp
     */
    private void dumpComponent(ServiceComponentExt comp)
    {
        log("*");
        log("*            ServiceComponent Information");
        assertNotNull("SC Name cannot be null", comp.getName());
        log("*        SC Name: " + comp.getName());
        int pid = comp.getPID();
        // TODO (Josh) Verify this range of pids
        assertTrue("SC Pid does not fall within valid range", pid > 0 && pid < 65535);
        log("*        SC Pid: " + pid);
        log("*        SC Assoc. Language: " + comp.getAssociatedLanguage());
        assertNotNull("SC ServiceInformationType cannot be null", comp.getServiceInformationType());
        log("*        SC ServiceInformationType: " + comp.getServiceInformationType());
        assertNotNull("SC StreamType cannot be null", comp.getStreamType());
        log("*        SC StreamType: " + comp.getStreamType());
        short est = comp.getElementaryStreamType();
        // TODO (Josh) Enable once Elementary StreamType is broadcast properly
        // assertTrue("Elementary StreamType does not fall within valid range: 1-14 or 128-132 (inclusive)",
        // (est > 0 && est < 15) && (est > 127 && est < 133));
        log("*        SC Elementary StreamType: " + est);
        assertNotNull("SC UpdateTime cannot be null", comp.getUpdateTime());
        long now = new Date().getTime();
        long oneYearAgo = now - (1000 * 60 * 60 * 24 * 365);
        long oneYearAhead = now + (1000 * 60 * 60 * 24 * 365);
        assertTrue("ServiceDetails' Update time should not be more than a year off", (oneYearAgo < comp.getUpdateTime()
                .getTime())
                && (oneYearAhead > comp.getUpdateTime().getTime()));
        log("*        SC UpdateTime: " + comp.getUpdateTime());
        try
        {
            log("*        SC Component Tag: " + comp.getComponentTag());
        }
        catch (SIException ex)
        {
            log("*        SC Component Tag: UNDEFINED");
        }
        try
        {
            log("*        SC Carousel ID: " + comp.getCarouselID());
        }
        catch (SIException ex)
        {
            log("*        SC Carousel ID: UNDEFINED");
        }
        assertNotNull("SC Locator cannot be null", comp.getLocator());
        log("*        SC Locator: " + comp.getLocator());
        assertNotNull("SC Handle cannot be null", comp.getServiceComponentHandle());
        log("*        SC Handle: " + comp.getServiceComponentHandle());
        assertNotNull("Returned ServiceDetails cannot be null", comp.getServiceDetails());
    }

    /**
     * Dumps the contents of the supplied ServiceDescription
     * 
     * @param sdesc
     */
    private void dumpServiceDescription(ServiceDescriptionExt sdesc)
    {
        log("*");
        log("*        ServiceDescription Information");
        assertNotNull("Description cannot be null", sdesc.getServiceDescription());
        log("*    SDesc Description: " + sdesc.getServiceDescription());
        assertNotNull("UpdateTime cannot be null", sdesc.getUpdateTime());
        long now = new Date().getTime();
        long oneYearAgo = now - (1000 * 60 * 60 * 24 * 365);
        long oneYearAhead = now + (1000 * 60 * 60 * 24 * 365);
        assertTrue("ServiceDetails' Update time should not be more than a year off",
                (oneYearAgo < sdesc.getUpdateTime().getTime()) && (oneYearAhead > sdesc.getUpdateTime().getTime()));
        log("*    SDesc UpdateTime: " + sdesc.getUpdateTime());
        assertNotNull("Returned ServiceDetails cannot be null", sdesc.getServiceDetails());
    }

    /**
     * Tunes to the provided service.
     * 
     * @param service
     *            The service to tune to
     * @throws Exception
     */
    private void tune(ServiceExt service) throws Exception
    {
        // Reserve the NetworkInterface and add a listener
        nic.reserveFor((OcapLocator) service.getLocator(), null);

        // Attempt to tune to the service
        nic.getNetworkInterface().addNetworkInterfaceListener(nil);
        nic.tune((OcapLocator) service.getLocator());
    }

    private void log(String message)
    {
        System.out.println(message);
    }

    private ServiceList serviceSetup()
    {
        // load the services from the SIManager.
        SIManager simgr = sm.createSIManager();
        ServiceList services = simgr.filterServices(new ServiceFilter()
        {
            public boolean accept(Service service)
            {
                return !(service instanceof AbstractService);
            }
        });
        try
        {
            services = services.sortByNumber();
        }
        catch (SortNotAvailableException ex)
        {
            // don't sort then
        }

        // Check to see if there are arguments
        // If length of one, then sourceID was specified
        // If length of three, then freq/prognum/modformat was specified
        if (args.length == 1)
        {
            String sid = args[0];
            if (args[0].startsWith("0x")) sid = sid.substring(2, sid.length());
            try
            {
                int sourceID = Integer.parseInt(sid, 16);
                OcapLocator[] locs = { new OcapLocator(sourceID) };
                LocatorFilter filter = new LocatorFilter(locs);
                services = simgr.filterServices(filter);
            }
            catch (NumberFormatException ex)
            {
                log("SourceID is not in the correct format");
                log("Proceeding with normal SIDump process");
            }
            catch (Exception ex)
            {
                log("Exception while getting specified service: " + ex.getMessage());
                log("Proceeding with normal SIDump process");
            }
        }
        if (args.length == 3)
        {
            try
            {
                int freq = Integer.parseInt(args[0]);
                int prog = Integer.parseInt(args[1]);
                int mod = Integer.parseInt(args[2]);
                OcapLocator[] locs = { new OcapLocator(freq, prog, mod) };
                LocatorFilter filter = new LocatorFilter(locs);
                services = simgr.filterServices(filter);
            }
            catch (NumberFormatException ex)
            {
                log("Freq/prognum/modformat values are not valid");
                log("Proceeding with normal SIDump process");
            }
            catch (Exception ex)
            {
                log("Exception while getting specified service: " + ex.getMessage());
                log("Proceeding with normal SIDump process");
            }
        }

        return services;
    }

    private static String[] args = new String[0];

    private CallerContext cc;

    private ServiceManager sm;

    private SIDatabase sidb;

    private SIChangedListener sial;

    private NetworkInterfaceController nic;

    private NetworkInterfaceListener nil;

    private boolean failed = false;
}
