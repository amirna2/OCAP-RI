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
package org.cablelabs.impl.service.javatv.navigation;

import java.util.Date;

import javax.tv.locator.Locator;
import javax.tv.service.SIChangeType;
import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRetrievable;
import javax.tv.service.ServiceInformationType;
import javax.tv.service.navigation.DeliverySystemType;
import javax.tv.service.navigation.ServiceComponent;
import javax.tv.service.navigation.ServiceComponentChangeEvent;
import javax.tv.service.navigation.ServiceComponentChangeListener;
import javax.tv.service.navigation.ServiceDescription;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceManager;

import org.cablelabs.impl.manager.service.CannedSIDatabase.ServiceDetailsHandleImpl;
import org.cablelabs.impl.service.ServiceDetailsHandle;
import org.cablelabs.impl.util.string.MultiString;
import org.cablelabs.test.SICannedConcreteTest;

/**
 * <p>
 * CannedServiceDetailsExtTest is used to test the ServiceDetailsExt interface
 * using canned data and behavior from the canned testing environment. This test
 * is only an interface test and does not do any testing on any methods not
 * specified in the ServiceDetailsExt interface.
 * </p>
 * <p>
 * Most of ServiceDetailsExt consists of getter methods. The testing done on
 * these is thorough, yet simple, and will only be briefly described at each
 * corresponding method. However, the tests performed on the add/remove listener
 * methods and the retrieve methods are substantially more complex and will be
 * described in much more detail. These tests consists of two groups, the aptly
 * named <i>Add/Remove Listener Method Tests</i> and <i>Retrieval Method
 * Tests</i>. These tests are described below.
 * </p>
 * <p>
 * <i>Add/Remove Listener Method Tests</i><br>
 * This will test the add and remove listener functionality of the
 * ServiceDetailsExt interface. These tests follow the same steps for each type
 * of listener to be added/removed. They are described as follows:
 * <ol>
 * <li>Add listener to TransportExt object.</li>
 * <li>Force an event so that TransportExt notifies registered listeners.</li>
 * <li>Check to see if listener was properly notified.</li>
 * <li>Remove the listener from the TransportExt object.</li>
 * <li>Force an event again so that TransportExt notifies registered listeners.</li>
 * <li>Check that the removed listener was not notified.</li>
 * </ol>
 * </p>
 * <p>
 * <i>Retrieval Method Tests</i><br>
 * This tests the asynchronous retrieval of service information from the
 * ServiceDetailsExt object. Again, the tests for each of these retrieval
 * methods follow the same steps. They are as follows:
 * <ol>
 * <li>Send the request for service information to TransportExt.</li>
 * <li>Wait a short period of time to allow listener to be called (2 seconds).</li>
 * <li>Chcek listener to see if it was properly notified within the time limit.</li>
 * <li>Get the retrieved objects from the listener and check for validity.</li>
 * </ol>
 * </p>
 * 
 * @author Joshua Keplinger
 * @author Todd Earles
 */
public class ServiceDetailsImplCannedTest extends SICannedConcreteTest
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
            TestRunner.run(suite());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    // Setup Section \\

    /**
     * No-arg constructor for creating our test case.
     * 
     */
    public ServiceDetailsImplCannedTest()
    {
        super("dServiceDetailsImplCanneTest");
    }

    /**
     * Creates a test suite to for use in the testing environment.
     * 
     * @return An InterfaceTestSuite that contains the tests in this test case.
     */
    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(ServiceDetailsImplCannedTest.class);
        suite.setName(ServiceDetailsImpl.class.getName());
        return suite;
    }

    /**
     * Sets up our test
     */
    protected void setUp() throws Exception
    {

        super.setUp();

        handle = new ServiceDetailsHandleImpl(1);
        sourceID = 100;
        programNumber = 5;
        name = new MultiString(new String[] { "eng" }, new String[] { "service details" });
        delivery = DeliverySystemType.CABLE;
        infoType = ServiceInformationType.ATSC_PSIP;
        now = new Date();
        caIDs = new int[] { 1, 2, 3 };
        isFree = true;
        sDetails = new ServiceDetailsImpl(sic, handle, sourceID, programNumber, csidb.transportStream7, name,
                csidb.service15, delivery, infoType, now, caIDs, isFree, 0x1FFF, null);

    }

    /**
     * Clean up after the test
     */
    protected void tearDown() throws Exception
    {
        sDetails = null;
        handle = null;
        name = null;
        delivery = null;
        infoType = null;
        now = null;
        caIDs = null;

        super.tearDown();
    }

    // Test Section \\

    /**
     * Tests the constructor and associated getters to make sure values are set
     * and returned properly.
     */
    public void testConstructor()
    {

        assertEquals("Handle does not match", handle, sDetails.getServiceDetailsHandle());
        assertEquals("SourceID does not match", sourceID, sDetails.getSourceID());
        assertEquals("ProgramNumber does not match", programNumber, sDetails.getProgramNumber());
        assertEquals("Long name does not match", name.getValue(null), sDetails.getLongName());
        assertEquals("DeliverySystemType does not match", delivery, sDetails.getDeliverySystemType());
        assertEquals("ServiceInformationType does not match", infoType, sDetails.getServiceInformationType());
        assertEquals("Update time does not match", now, sDetails.getUpdateTime());
        assertEquals("caIDs do not match", caIDs, sDetails.getCASystemIDs());
        assertEquals("isFree does not match", isFree, sDetails.isFree());
        assertEquals("Service does not match", csidb.service15, sDetails.getService());
        assertEquals("TransportStream does not match", csidb.transportStream7, sDetails.getTransportStream());

        // Now we'll try to throw an exception
        try
        {
            new ServiceDetailsImpl(sic, null, sourceID, programNumber, csidb.transportStream7, name, csidb.service15,
                    null, infoType, now, caIDs, isFree, 0x1FFF, null);
            fail("Exception should've been thrown using null handle");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }

    }

    /**
     * Tests <code>retrieveDefaultMediaComponents</code> to make sure it
     * correctly asynchronously retrieves the default video and audio
     * components.
     * 
     * @throws Exception
     */
    public void testRetrieveDefaultMediaComponents() throws Exception
    {
        SIRequestorTest requestor = new SIRequestorTest();
        tune(csidb.transportStream7);
        csidb.serviceDetails33.retrieveDefaultMediaComponents(requestor);
        requestor.waitForEvents(waitForRequest);
        assertTrue("Requestor was not notified of success", requestor.succeeded());
        SIRetrievable[] results = requestor.getResults();
        assertEquals("Returned array is incorrect size", 2, results.length);
        assertEquals("Returned component[0] incorrect", csidb.serviceComponent69, results[0]);
        assertEquals("Returned component[1] incorrect", csidb.serviceComponent69eng, results[1]);

        requestor.reset();
        csidb.serviceDetails34.retrieveDefaultMediaComponents(requestor);
        requestor.waitForEvents(waitForRequest);
        assertTrue("Requestor was not notified of success", requestor.succeeded());
        assertEquals("Expected DATA_UNAVAILABLE Failure type", SIRequestFailureType.DATA_UNAVAILABLE,
                requestor.getFailure());

        requestor.reset();
        tune(csidb.transportStream9);
        csidb.serviceDetails35.retrieveDefaultMediaComponents(requestor);
        requestor.waitForEvents(waitForRequest);
        assertTrue("Requestor was not notified of success", requestor.succeeded());
        results = requestor.getResults();
        assertEquals("Returned array is incorrect size", 1, results.length);
        assertEquals("Returned components are incorrect", csidb.serviceComponent71, results[0]);

        requestor.reset();
        tune(csidb.transportStream10);
        csidb.serviceDetails36.retrieveDefaultMediaComponents(requestor);
        requestor.waitForEvents(waitForRequest);
        assertTrue("Requestor was not notified of success", requestor.succeeded());
        results = requestor.getResults();
        assertEquals("Returned array is incorrect size", 2, results.length);
        assertEquals("Returned components are incorrect", csidb.serviceComponent72, results[0]);
        assertEquals("Returned components are incorrect", csidb.serviceComponent108, results[1]);

        // Now we'll try to throw an exception
        try
        {
            csidb.serviceDetails33.retrieveDefaultMediaComponents(null);
            fail("NullPointerException was not thrown properly");
        }
        catch (NullPointerException expected)
        {
            // We wanted this to happen
        }
    }

    /**
     * Tests <code>getProgramSchedule</code> to make sure it returns the right
     * ProgramSchedule of this service details.
     */
    public void testGetProgramSchedule()
    {
        // TODO (Josh) Write test as soon as this can be tested.
    }

    /**
     * Tests <code>getLocator</code> to make sure it returns a locator that
     * correctly corresponds to this network
     */
    public void testGetLocator()
    {
        assertTrue("Value returned was not a valid Locator", sDetails.getLocator() instanceof Locator);
        assertEquals("Returned locators do not match.", sDetails.getLocator(), sDetails.getLocator());
    }

    /**
     * Tests <code>equals</code> to make sure it correctly compares to exact
     * same transport stream objects. It also tests <code>equals</code> on
     * different objects by varying the values passed to the constructor.
     */
    public void testEquals()
    {
        assertTrue("Equals should succeed when testing same object.", sDetails.equals(sDetails));
        // Test equals with different handle
        ServiceDetailsHandleImpl diffHandle = new ServiceDetailsHandleImpl(17);
        ServiceDetailsImpl sDetails2 = new ServiceDetailsImpl(sic, diffHandle, sourceID, programNumber,
                csidb.transportStream7, name, csidb.service15, delivery, infoType, now, caIDs, isFree, 0x1FFF, null);
        assertFalse("Equals should fail with different handles.", sDetails.equals(sDetails2));
        // Test equals with different sourceID
        sDetails2 = new ServiceDetailsImpl(sic, handle, 25, programNumber, csidb.transportStream7, name,
                csidb.service15, delivery, infoType, now, caIDs, isFree, 0x1FFF, null);
        assertFalse("Equals should fail with different sourceID.", sDetails.equals(sDetails2));
        // Test equals with different programNumber
        sDetails2 = new ServiceDetailsImpl(sic, handle, sourceID, 99, csidb.transportStream7, name, csidb.service15,
                delivery, infoType, now, caIDs, isFree, 0x1FFF, null);
        assertFalse("Equals should fail with different program number.", sDetails.equals(sDetails2));
        // Test equals with different TransportStream
        sDetails2 = new ServiceDetailsImpl(sic, handle, sourceID, programNumber, csidb.transportStream10, name,
                csidb.service15, delivery, infoType, now, caIDs, isFree, 0x1FFF, null);
        assertFalse("Equals should fail with different transport stream.", sDetails.equals(sDetails2));
        // Test equals with different long name
        sDetails2 = new ServiceDetailsImpl(sic, handle, sourceID, programNumber, csidb.transportStream7,
                new MultiString(new String[] { "eng" }, new String[] { "foo" }), csidb.service15, delivery, infoType,
                now, caIDs, isFree, 0x1FFF, null);
        assertFalse("Equals should fail with different long name.", sDetails.equals(sDetails2));
        // Test equals with different service
        sDetails2 = new ServiceDetailsImpl(sic, handle, sourceID, programNumber, csidb.transportStream7, name,
                csidb.service18, delivery, infoType, now, caIDs, isFree, 0x1FFF, null);
        assertFalse("Equals should fail with different service.", sDetails.equals(sDetails2));
        // Test equals with different DeliverySystemType
        sDetails2 = new ServiceDetailsImpl(sic, handle, sourceID, programNumber, csidb.transportStream7, name,
                csidb.service15, DeliverySystemType.TERRESTRIAL, infoType, now, caIDs, isFree, 0x1FFF, null);
        assertFalse("Equals should fail with different delivery system type.", sDetails.equals(sDetails2));
        // Test equals with different ServiceInformationType
        sDetails2 = new ServiceDetailsImpl(sic, handle, sourceID, programNumber, csidb.transportStream7, name,
                csidb.service15, delivery, ServiceInformationType.SCTE_SI, now, caIDs, isFree, 0x1FFF, null);
        assertFalse("Equals should fail with different service information type.", sDetails.equals(sDetails2));
        // Test equals with different update time
        sDetails2 = new ServiceDetailsImpl(sic, handle, sourceID, programNumber, csidb.transportStream7, name,
                csidb.service15, delivery, infoType, new Date(System.currentTimeMillis() + 5555), caIDs, isFree,
                0x1FFF, null);
        assertFalse("Equals should fail with different update time.", sDetails.equals(sDetails2));
        // Test equals with different caIDs
        sDetails2 = new ServiceDetailsImpl(sic, handle, sourceID, programNumber, csidb.transportStream7, name,
                csidb.service15, delivery, infoType, now, new int[] { 4, 5, 6 }, isFree, 0x1FFF, null);
        assertFalse("Equals should fail with different CA IDs.", sDetails.equals(sDetails2));
        // Test equals with different isFree
        sDetails2 = new ServiceDetailsImpl(sic, handle, sourceID, programNumber, csidb.transportStream7, name,
                csidb.service15, delivery, infoType, now, caIDs, false, 0x1FFF, null);
        assertFalse("Equals should fail with different isFree.", sDetails.equals(sDetails2));

    }

    /**
     * Tests <code>hashCode</code> to make sure it returns a proper hash code.
     */
    public void testHashCode()
    {
        assertEquals("Hashcode does not match on same object.", sDetails.hashCode(), sDetails.hashCode());
    }

    /**
     * Tests <code>addServiceComponentChangeListener</code> and
     * <code>removeServiceComponentChangeListener</code> to make sure it
     * properly adds and removes listeners accordingly.
     */
    public void testAddRemoveServiceComponentChangeListener() throws Exception
    {
        class ServiceComponentChangeListenerTest implements ServiceComponentChangeListener
        {
            public boolean notified = false;

            public ServiceComponentChangeEvent scEvent = null;

            public int events = 0;

            public synchronized void notifyChange(ServiceComponentChangeEvent event)
            {
                notified = true;
                scEvent = event;
                events++;
                notifyAll();
            }

            public void reset()
            {
                scEvent = null;
                notified = false;
                events = 0;
            }

            public synchronized void waitForEvents(long millis) throws InterruptedException
            {
                if (!notified) wait(millis);
            }
        }

        ServiceComponentChangeListenerTest listener = new ServiceComponentChangeListenerTest();
        // /////////////////////////////////////////////////////////////////////
        // Add SI Section
        // /////////////////////////////////////////////////////////////////////

        // Get a reference to a transport
        ServiceDetailsImpl localSD = csidb.serviceDetails33;
        // Add the listener to the cache.
        localSD.addServiceComponentChangeListener(listener);
        // Fire an event from the CannedSIDatabase
        ServiceComponentImpl sc1 = (ServiceComponentImpl) csidb.cannedAddSI(csidb.serviceComponent69);
        // Check to see that the listener was notified and contains the right
        // data.
        listener.waitForEvents(waitForRequest);
        assertTrue("ServiceComponentChangeListener was not notified.", listener.notified);
        assertTrue("Returned ServiceComponent is incorrect", sc1.equals(listener.scEvent.getServiceComponent()));
        assertTrue("Returned transport is incorrect", localSD.equals(listener.scEvent.getServiceDetails()));
        assertEquals("Returned change type is incorrect", SIChangeType.ADD, listener.scEvent.getChangeType());
        // Now we'll update another piece of SI, but on a different transport
        listener.reset();
        ServiceComponentImpl sc2 = (ServiceComponentImpl) csidb.cannedAddSI(csidb.serviceComponent73);
        listener.waitForEvents(waitForRequest);
        assertFalse("ServiceComponentChangeListener was incorrectly notified", listener.notified);

        // /////////////////////////////////////////////////////////////////////
        // Modify SI Section
        // /////////////////////////////////////////////////////////////////////

        // Reset the listener
        listener.reset();
        // Update our ServiceComponent object
        sc1 = (ServiceComponentImpl) csidb.cannedUpdateSI(sc1);
        // Wait for our event to fire
        listener.waitForEvents(waitForRequest);
        // Check the results
        assertTrue("ServiceComponentChangeListener was not notified.", listener.notified);
        assertTrue("Returned ServiceComponent is incorrect", sc1.equals(listener.scEvent.getServiceComponent()));
        assertTrue("Returned transport is incorrect", localSD.equals(listener.scEvent.getServiceDetails()));
        assertEquals("Returned change type is incorrect", SIChangeType.MODIFY, listener.scEvent.getChangeType());
        // Now we'll reset our listener
        listener.reset();
        // Update the other piece of SI and make sure we aren't notified
        csidb.cannedUpdateSI(sc2);
        // Wait for the non-notification
        listener.waitForEvents(waitForRequest);
        // Make sure that the listener was not notified
        assertFalse("ServiceComponentChangeListener was incorrectly notified", listener.notified);

        // /////////////////////////////////////////////////////////////////////
        // Remove SI Section
        // /////////////////////////////////////////////////////////////////////

        // Reset the listener
        listener.reset();
        // Remove our ServiceComponent object
        csidb.cannedRemoveSI(sc1);
        // Wait for our event to fire
        listener.waitForEvents(waitForRequest);
        // Check the results
        assertTrue("ServiceComponentChangeListener was not notified.", listener.notified);
        assertTrue("Returned ServiceComponent is incorrect", sc1.equals(listener.scEvent.getServiceComponent()));
        assertTrue("Returned transport is incorrect", localSD.equals(listener.scEvent.getServiceDetails()));
        assertEquals("Returned change type is incorrect", SIChangeType.REMOVE, listener.scEvent.getChangeType());
        // Now we'll reset our listener
        listener.reset();
        // Update the other piece of SI
        csidb.cannedRemoveSI(sc2);
        // Wait for the non-notification
        listener.waitForEvents(waitForRequest);
        // Make sure that the listener was not notified
        assertFalse("ServiceComponentChangeListener was incorrectly notified", listener.notified);

        // /////////////////////////////////////////////////////////////////////
        listener.reset();
        // We're going to see if we can add the same listener twice
        localSD.addServiceComponentChangeListener(listener);
        csidb.cannedUpdateSI(csidb.serviceComponent105);
        listener.waitForEvents(waitForRequest);
        assertEquals("Listener was incorrectly notified more than once", 1, listener.events);

        // Finally, we're going to remove our listener
        listener.reset();
        localSD.removeServiceComponentChangeListener(listener);
        // Fire the change
        csidb.cannedUpdateSI(csidb.serviceComponent69);
        // Wait for the non-notification
        listener.waitForEvents(waitForRequest);
        // Make sure that the listener was not notified
        assertFalse("ServiceComponentChangeListener was incorrectly notified", listener.notified);

    }

    /**
     * Tests <code>retrieveComponents</code> to make sure it returns the proper
     * service components that belong with this object. This test is performed
     * asynchronously to simulate the response of the SIDatabase.
     */
    public void testRetrieveComponents() throws Exception
    {
        SIRequestorTest requestor = new SIRequestorTest();
        tune(csidb.transportStream7);
        csidb.serviceDetails33.retrieveComponents(requestor);
        requestor.waitForEvents(waitForRequest);
        assertTrue("Requestor was not notified of success", requestor.succeeded());
        SIRetrievable[] results = requestor.getResults();
        assertTrue("Results returned were not of the right type", results[0] instanceof ServiceComponent);

        // Now we'll try to throw an exception
        try
        {
            csidb.serviceDetails33.retrieveComponents(null);
            fail("NullPointerException was not thrown properly");
        }
        catch (NullPointerException expected)
        {
            // We wanted this to happen
        }

        // TODO Add test for zero length array being returned
    }

    /**
     * Tests <code>retrieveServiceDescription</code> to make sure it returns the
     * proper service description requested. This test is performed
     * asynchronously to simulate the response of the SIDatabase.
     */
    public void testRetrieveServiceDescription() throws Exception
    {
        SIRequestorTest requestor = new SIRequestorTest();
        tune(csidb.transportStream7);
        csidb.serviceDetails33.retrieveServiceDescription(requestor);
        requestor.waitForEvents(waitForRequest);
        assertTrue("Requestor was not notified of success", requestor.succeeded());
        SIRetrievable[] results = requestor.getResults();
        assertTrue("Results returned were not of the right type", results[0] instanceof ServiceDescription);

        // Now we'll try to throw an exception
        try
        {
            csidb.serviceDetails33.retrieveServiceDescription(null);
            fail("NullPointerException was not thrown properly");
        }
        catch (NullPointerException expected)
        {
            // We wanted this to happen
        }
    }

    /**
     *
     *
     */
    public void testRetrieveCarouselComponent() throws Exception
    {
        SIRequestorTest requestor = new SIRequestorTest();
        tune(csidb.transportStream7);
        csidb.serviceDetails33.retrieveCarouselComponent(requestor);
        requestor.waitForEvents(waitForRequest);

        assertTrue("Requestor was not notified of success", requestor.succeeded());
        SIRetrievable[] results = requestor.getResults();
        assertEquals("Results returned were not of the right type", csidb.serviceComponent105, results[0]);

        // Now we'll try to throw an exception
        try
        {
            csidb.serviceDetails33.retrieveCarouselComponent(null);
            fail("NullPointerException was not thrown properly");
        }
        catch (NullPointerException expected)
        {
            // We wanted this to happen
        }

        requestor.reset();

        tune(csidb.transportStream7);
        csidb.serviceDetails33.retrieveCarouselComponent(requestor, 46);
        requestor.waitForEvents(waitForRequest);

        assertTrue("Requestor was not notified of success", requestor.succeeded());
        results = requestor.getResults();
        assertEquals("Results returned were not of the right type", csidb.serviceComponent105, results[0]);

        requestor.reset();

        tune(csidb.transportStream7);
        csidb.serviceDetails33.retrieveCarouselComponent(requestor, 8);
        requestor.waitForEvents(waitForRequest);
        assertTrue("Requestor was not notified", requestor.succeeded());
        assertEquals("Wrong failure type, expected DATA_UNAVAILABLE", SIRequestFailureType.DATA_UNAVAILABLE,
                requestor.getFailure());
    }

    /**
     * Tests <code>getServiceId()</code> method of DAVIC <code>Service</code>.
     */
    public void testDavicGetServiceId()
    {
        NetworkInterfaceManager nim = NetworkInterfaceManager.getInstance();
        NetworkInterface[] ni = nim.getNetworkInterfaces();
        org.davic.mpeg.TransportStream ts = csidb.transportStream7.getDavicTransportStream(ni[0]);
        org.davic.mpeg.Service s = sDetails.getDavicService(ts);
        assertEquals("Wrong service ID", programNumber, s.getServiceId());
    }

    // Data Section \\

    /**
     * Holds the instance of the ServiceDetailsExt object we are testing.
     */
    private ServiceDetailsImpl sDetails;

    private ServiceDetailsHandle handle;

    private int sourceID;

    private int programNumber;

    private MultiString name;

    private DeliverySystemType delivery;

    private ServiceInformationType infoType;

    private Date now;

    private int[] caIDs;

    private boolean isFree;

}
