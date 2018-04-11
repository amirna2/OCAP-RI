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
package javax.tv.service;

import javax.tv.locator.InvalidLocatorException;
import javax.tv.service.navigation.SIElementFilter;
import javax.tv.service.navigation.ServiceIterator;
import javax.tv.service.navigation.ServiceList;
import javax.tv.service.navigation.ServiceTypeFilter;

import junit.textui.TestRunner;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.ocap.net.OcapLocator;

import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.application.ApplicationTest.CCMgr;
import org.cablelabs.impl.service.NetworkLocator;
import org.cablelabs.impl.service.SIManagerExt;
import org.cablelabs.impl.service.SINotAvailableException;
import org.cablelabs.impl.service.TransportExt;
import org.cablelabs.test.SICannedInterfaceTest;

/**
 * <p>
 * CannedSIManagerTest is used to test the SIManager abstract class using canned
 * data and behavior from the canned testing environment. This test is only an
 * interface test and does not do any testing on any methods not specified in
 * the SIManager abstract class.
 * </p>
 * <p>
 * Most of SIManager consists of getter methods. The testing done on these is
 * thorough, yet simple, and will only be briefly described at each
 * corresponding method. However, the tests performed on the register interest
 * method and the retrieve methods are substantially more complex and will be
 * described in much more detail. These tests consists of two groups, the aptly
 * named <i>Register Interest Method Tests</i> and <i>Retrieval Method
 * Tests</i>. These tests are described below.
 * </p>
 * <p>
 * <i>Register Interest Method Test</i><br>
 * This will test the register interest functionality of the SIManager abstract
 * class. This is described as follows:
 * <ol>
 * <li>Register interest using the given locator</li>
 * <li>Force an event in CannedSIDatabase, so that it notifies SICache of the
 * update.</li>
 * <li>Check to see if SICache cached the SIElement we are interested in.</li>
 * <li>Deactivate interest in the SI identified by the locator</li>
 * <li>Force an event again so that it notifies SICache of the update.</li>
 * <li>Check to make sure that SICache did not update its cache for that SI.</li>
 * </ol>
 * </p>
 * <p>
 * <i>Retrieval Method Tests</i><br>
 * This tests the asynchronous retrieval of service information from the
 * SIManager object. Again, the tests for each of these retrieval methods follow
 * the same steps. They are as follows:
 * <ol>
 * <li>Send the request for service information to TransportExt.</li>
 * <li>Wait a short period of time to allow listener to be called (2 seconds).</li>
 * <li>Chcek listener to see if it was properly notified within the time limit.</li>
 * <li>Get the retrieved objects from the listener and check for validity.</li>
 * </ol>
 * </p>
 * 
 * @author Joshua Keplinger
 */
public class SIManagerCannedTest extends SICannedInterfaceTest
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
    public SIManagerCannedTest()
    {
        super("SIManagerCannedTest", SIManager.class, new CannedSIManagerTestFactory());
    }

    /**
     * Creates a test suite to for use in the testing environment.
     * 
     * @return An InterfaceTestSuite that contains the tests in this test case.
     */
    public static InterfaceTestSuite isuite()
    {
        InterfaceTestSuite suite = new InterfaceTestSuite(SIManagerCannedTest.class);
        suite.setName(SIManager.class.getName());
        suite.addFactory(new CannedSIManagerTestFactory());
        return suite;
    }

    /**
     * Sets up our test
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        requestor = new SIRequestorTest();
        manager = (SIManager) createImplObject();
        realccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        ManagerManagerTest.updateManager(CallerContextManager.class, CCMgr.class, false, new CCMgr(realccm));
    }

    /**
     * Clean up after the test
     */
    protected void tearDown() throws Exception
    {
        manager = null;
        requestor = null;
        csidb.cannedFullReset();
        if (realccm != null)
            ManagerManagerTest.updateManager(CallerContextManager.class, realccm.getClass(), true, realccm);
        super.tearDown();
    }

    // Test Section \\

    /**
     * Tests <code>createInstance</code> to make sure it returns a valid
     * SIManager instance.
     */
    public void testCreateInstance()
    {
        assertTrue("Returned value is not an instance of SIManager", SIManager.createInstance() instanceof SIManager);
        assertTrue("Returned value is not an instance of SIManager", SIManager.createInstance() instanceof SIManager);
    }

    /**
     * Tests handling of OOB locator.
     */
    public void testOOBLocator()
    {
        // Get SIManager, OOB locator and requestor for calls below
        SIManager manager = SIManager.createInstance();
        OcapLocator oobLocator = null;
        try
        {
            oobLocator = new OcapLocator("ocap://oobfdc.0x1");
        }
        catch (org.davic.net.InvalidLocatorException e)
        {
            fail("Could not construct OOB locator");
        }
        SIRequestor requestor = new SIRequestor()
        {
            public void notifySuccess(SIRetrievable[] result)
            {
            }

            public void notifyFailure(SIRequestFailureType reason)
            {
            }
        };

        // Test OOB locator handling in getService()
        try
        {
            manager.getService(oobLocator);
            fail("Expected InvalidLocatorException from getService() but got success");
        }
        catch (Exception e)
        {
            assertTrue("Expected InvalidLocatorException from getService() but got" + e,
                    e instanceof InvalidLocatorException);
        }

        // Test OOB locator handling in retrieveProgramEvent()
        try
        {
            manager.retrieveProgramEvent(oobLocator, requestor);
            fail("Expected InvalidLocatorException from retrieveProgramEvent() but got success");
        }
        catch (Exception e)
        {
            assertTrue("Expected InvalidLocatorException from retrieveProgramEvent() but got" + e,
                    e instanceof InvalidLocatorException);
        }

        // Test OOB locator handling in retrieveServiceDetails()
        try
        {
            manager.retrieveServiceDetails(oobLocator, requestor);
            fail("Expected InvalidLocatorException from retrieveServiceDetails() but got success");
        }
        catch (Exception e)
        {
            assertTrue("Expected InvalidLocatorException from retrieveServiceDetails() but got" + e,
                    e instanceof InvalidLocatorException);
        }

        // Test OOB locator handling in retrieveSIElement()
        try
        {
            manager.retrieveSIElement(oobLocator, requestor);
            fail("Expected InvalidLocatorException from retrieveSIElement() but got success");
        }
        catch (Exception e)
        {
            assertTrue("Expected InvalidLocatorException from retrieveSIElement() but got" + e,
                    e instanceof InvalidLocatorException);
        }
    }

    /**
     * Tests <code>filterServices</code> to make sure it returns a properly made
     * ServiceList using a ServiceFilter.
     */
    public void testFilterServices() throws Exception
    {
        ServiceList list = manager.filterServices(null);
        assertEquals("Wrong number of services in the service list.", 23, list.size());
        ServiceIterator siter = list.createServiceIterator();
        int counter = 0;
        while (siter.hasNext())
        {
            counter++;
            siter.nextService();
        }
        assertEquals("Wrong number of forward iterations", list.size(), counter);
        assertFalse("Iterator should not have a next()", siter.hasNext());
        assertTrue("Iterator should have a previous", siter.hasPrevious());
        counter = 0;
        siter.toEnd();
        while (siter.hasPrevious())
        {
            counter++;
            siter.previousService();
        }
        assertTrue("List should have a next", siter.hasNext());
        assertFalse("List should not have a previous", siter.hasPrevious());
        assertEquals("Wrong number of backwards iterations", list.size(), counter);

        list = manager.filterServices(new ServiceTypeFilter(ServiceType.DIGITAL_TV));
        assertEquals("Wrong number of services in the servicelist", 16, list.size());

        list = manager.filterServices(new SIElementFilter(csidb.transportStream7));
        assertEquals("Wrong number of services in the servicelist", 5, list.size());
    }

    /**
     * Tests <code>getPreferredLanguage</code> to make sure it returns the right
     * preferred language.
     */
    public void testGetPreferredLanguage()
    {
        manager.setPreferredLanguage("fre");
        assertEquals("Returned language does not match expected value", "fre", manager.getPreferredLanguage());
        assertEquals("Returned language does not match expected value", "fre", manager.getPreferredLanguage());
    }

    /**
     * Tests <code>getRatingDimension</code> to make sure it returns the right
     * rating dimension.
     */
    public void testGetRatingDimension() throws Exception
    {
        RatingDimension dimension = manager.getRatingDimension(csidb.ratingDimension1.getDimensionName());
        assertEquals("Returned dimension is incorrect", csidb.ratingDimension1, dimension);
        try
        {
            manager.getRatingDimension("foo");
            fail("SIException should have been thrown with invalid RatingDimension name");
        }
        catch (SIException expected)
        {
            // We wanted this to happen
        }
    }

    /**
     * Tests <code>getService</code> to make sure it returns the right Service.
     */
    public void testGetService() throws Exception
    {
        Service service = manager.getService(csidb.l100);
        assertEquals("Returned service is incorrect", csidb.service15, service);
        service = manager.getService(csidb.l117);
        assertEquals("Returned service is incorrect", csidb.service32, service);

        // Test null locator
        try
        {
            service = manager.getService(null);
            fail("Should have thrown NullPointerException");
        }
        catch (NullPointerException e)
        {
        }

        // Test not-a-service locator
        OcapLocator invalidLocator = new OcapLocator(1000, -1);
        try
        {
            service = manager.getService(invalidLocator);
            fail("Should have thrown InvalidLocatorException");
        }
        catch (InvalidLocatorException e)
        {
        }
    }

    /**
     * Tests <code>getService</code> by service number to make sure it returns
     * the right Service.
     */
    public void testGetServiceByServiceNumber() throws Exception
    {
        SIManagerExt simgr = (SIManagerExt) manager;
        Service service;

        // Test two part service number
        service = simgr.getService(110, 5); // channel 110.5
        assertEquals("Returned service is incorrect", csidb.service24, service);

        // Test one part service number
        service = simgr.getService(1100, -1); // channel 1100
        assertEquals("Returned service is incorrect", csidb.service25, service);

        // Test invalid service number
        try
        {
            service = simgr.getService(0, 1000);
            fail("Should have thrown SIException");
        }
        catch (SIException e)
        {
        }
    }

    /**
     * Tests <code>getService</code> by service number to make sure it returns
     * the right Service.
     */
    public void testGetServiceByChannelNumber() throws Exception
    {
        SIManagerExt simgr = (SIManagerExt) manager;
        Service service;

        // Test two part service number
        service = simgr.getService((short) 0x06e, (short) 0x005); // channel
                                                                  // 110.5
        assertEquals("Returned service is incorrect", csidb.service24, service);

        // Test one part service number
        service = simgr.getService((short) 0x3f1, (short) 0x04c); // channel
                                                                  // 1100
        assertEquals("Returned service is incorrect", csidb.service25, service);

        // Test invalid service number
        try
        {
            service = simgr.getService((short) 0, (short) 1000);
            fail("Should have thrown SIException");
        }
        catch (SIException e)
        {
        }
    }

    /**
     * Tests <code>getSupportedDimensions</code> to make sure it returns the
     * right supported dimensions.
     */
    public void testGetSupportedDimensions()
    {
        String[] dimensions = manager.getSupportedDimensions();
        assertEquals("Array length is incorrect", 1, dimensions.length);
        assertEquals("First element is incorrect", csidb.ratingDimension1.getDimensionName(), dimensions[0]);
    }

    /**
     * Tests <code>getTransports</code> to make sure it returns an array of the
     * correct transports.
     */
    public void testGetTransports()
    {
        TransportExt[] transports = (TransportExt[]) manager.getTransports();
        assertEquals("Returned array length is incorrect", 2, transports.length);
        boolean transFound1 = false;
        boolean transFound2 = false;
        for (int i = 0; i < transports.length; i++)
        {
            if (csidb.transport1.getTransportID() == transports[i].getTransportID()) transFound1 = true;
            if (csidb.transport2.getTransportID() == transports[i].getTransportID()) transFound2 = true;
        }
        assertTrue("Expected transports are not in array", transFound1 && transFound2);
    }

    /**
     * Tests <code>registerInterest</code> to make sure it properly registers
     * interest in the SI.
     */
    public void testRegisterInterest()
    {
        // TODO (Josh) Implement
    }

    /**
     * Tests <code>retrieveProgramEvent</code> to make sure it properly returns
     * the program event corresponding to the given locator. This test is
     * performed asynchronously to simulate the response of the SIDatabase.
     */
    public void testRetrieveProgramEvent()
    {
        // TODO (Josh) Implement
    }

    /**
     * Tests <code>retrieveServiceDetails</code> to make sure it properly
     * returns the service details corresponding to the given locator. This test
     * is performed asynchronously to simulate the response of the SIDatabase.
     */
    public void testRetrieveServiceDetails() throws Exception
    {
        // Test with null locator and requestor
        try
        {
            manager.retrieveServiceDetails(null, requestor);
            fail("Should have thrown NullPointerException");
        }
        catch (NullPointerException e)
        {
        }
        try
        {
            manager.retrieveServiceDetails(csidb.l100, null);
            fail("Should have thrown NullPointerException");
        }
        catch (NullPointerException e)
        {
        }

        manager.retrieveServiceDetails(csidb.l100, requestor);
        // Wait for the event
        requestor.waitForEvents(waitForRequest);
        assertTrue("Requestor was not notified of success", requestor.success);
        SIRetrievable[] results = requestor.getResults();
        assertEquals("Returned results are incorrect", csidb.serviceDetails33, results[0]);

        // Send the same request, but use the SIManagerExt synchronous call
        results = ((SIManagerExt) manager).getServiceDetails(csidb.l100);
        assertEquals("Returned results are incorrect", csidb.serviceDetails33, results[0]);

        // Now we'll send another request, but cause a failure
        requestor.reset();
        csidb.cannedSetForcedException(SINotAvailableException.class);
        manager.retrieveServiceDetails(csidb.l100, requestor);
        requestor.waitForEvents(waitForRequest);
        assertTrue("Requestor was not notified of success", requestor.success);
        assertEquals("Failure returned is incorrect", SIRequestFailureType.DATA_UNAVAILABLE, requestor.getFailure());
    }

    /**
     * Tests <code>retrieveSIElement</code> to make sure it properly returns the
     * SIElement associated with the given locator. This test is performed
     * asynchronously to simulate the response of the SIDatabase.
     */
    public void testRetrieveSIElement() throws Exception
    {
        // Test null parameters
        try
        {
            manager.retrieveSIElement(null, requestor);
            fail("Should have thrown NullPointerException");
        }
        catch (NullPointerException e)
        {
        }
        try
        {
            manager.retrieveSIElement(new OcapLocator(5000, 1), null);
            fail("Should have thrown NullPointerException");
        }
        catch (NullPointerException e)
        {
        }

        // First we'll request a network
        manager.retrieveSIElement(new NetworkLocator(csidb.transport1.getTransportID(), csidb.network3.getNetworkID()),
                requestor);
        requestor.waitForEvents(waitForRequest);
        assertTrue("Requestor was not notified", requestor.success);
        assertEquals("Returned network is incorrect", csidb.network3, requestor.results[0]);

        // Next we'll ask for a transport stream
        requestor.reset();
        manager.retrieveSIElement(new OcapLocator(5000, 1), requestor);
        requestor.waitForEvents(waitForRequest);
        assertTrue("Requestor was not notified", requestor.success);
        assertEquals("Returned transport stream is incorrect", csidb.transportStream7, requestor.results[0]);

        // Now for a service details
        requestor.reset();
        manager.retrieveSIElement(csidb.l100, requestor);
        requestor.waitForEvents(waitForRequest);
        assertTrue("Requestor was not notified", requestor.success);
        assertEquals("Returned service details is incorrect", csidb.serviceDetails33, requestor.results[0]);

        // Use the synchronous SIManagerExt call to retrieve the same service
        // details
        SIRetrievable[] results = ((SIManagerExt) manager).getSIElement(csidb.l100);
        assertEquals("Returned service details is incorrect", csidb.serviceDetails33, results[0]);

        // Lastly, we'll ask for a service component
        requestor.reset();
        OcapLocator locator = new OcapLocator(csidb.service15.getName(),
                new String[] { csidb.serviceComponent69.getName() }, -1, null);
        manager.retrieveSIElement(locator, requestor);
        requestor.waitForEvents(waitForRequest);
        assertTrue("Requestor was not notified", requestor.success);
        assertEquals("Returned service component is incorrect", csidb.serviceComponent69, requestor.results[0]);
    }

    /**
     * Tests <code>setPreferredLanguage</code> to make sure it properly sets the
     * preferred language.
     */
    public void testSetPreferredLanguage()
    {
        manager.setPreferredLanguage("fre");
        assertEquals("Returned language does not match expected value", "fre", manager.getPreferredLanguage());
        manager.setPreferredLanguage("eng");
        assertEquals("Returned language does not match expected value", "eng", manager.getPreferredLanguage());
        manager.setPreferredLanguage(null);
        assertNull("Language returned should be null", manager.getPreferredLanguage());
    }

    // Data Section \\

    private SIManager manager;

    private SIRequestorTest requestor;

    /**
     * This is a default factory class that is passed to the
     * <code>CannedSIManagerTest</code>. It is used to instantiate a concrete
     * class to be used in the test.
     * 
     * @author Josh
     */
    private static class CannedSIManagerTestFactory implements ImplFactory
    {

        public Object createImplObject() throws Exception
        {
            ServiceManager srvcManager = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
            return srvcManager.createSIManager();
        }

        public String toString()
        {
            return "CannedSIManagerTestFactory";
        }
    }

    /**
     * Concrete implementation of SIRequestor. This is used in the unit tests to
     * handle the request responses.
     * 
     * @author Joshua Keplinger
     */
    protected class SIRequestorTest implements SIRequestor
    {
        private SIRetrievable[] results = new SIRetrievable[0];

        private SIRequestFailureType failtype = null;

        private boolean success = false;

        public SIRequestorTest()
        {
            results = new SIRetrievable[0];
            failtype = null;
            success = false;
        }

        public synchronized void notifyFailure(SIRequestFailureType reason)
        {
            failtype = reason;
            assertNotNull("Failure type should not be null", failtype);
            success = true;
            notify();
        }

        public synchronized void notifySuccess(SIRetrievable[] result)
        {
            this.results = result;
            assertNotNull("Results should not be null", results);
            this.success = true;
            notify();
        }

        public SIRetrievable[] getResults()
        {
            return results;
        }

        public boolean succeeded()
        {
            return success;
        }

        public SIRequestFailureType getFailure()
        {
            return failtype;
        }

        public void reset()
        {
            failtype = null;
            results = new SIRetrievable[0];
            success = false;
        }

        public synchronized void waitForEvents(long millis) throws InterruptedException
        {
            wait(millis);
        }
    }

    // Real caller context manager
    private CallerContextManager realccm;
}
