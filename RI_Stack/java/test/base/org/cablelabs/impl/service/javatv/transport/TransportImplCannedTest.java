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
package org.cablelabs.impl.service.javatv.transport;

import javax.tv.service.SIChangeType;
import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.SIRetrievable;
import javax.tv.service.navigation.DeliverySystemType;
import javax.tv.service.transport.Network;
import javax.tv.service.transport.NetworkChangeEvent;
import javax.tv.service.transport.NetworkChangeListener;
import javax.tv.service.transport.ServiceDetailsChangeEvent;
import javax.tv.service.transport.ServiceDetailsChangeListener;
import javax.tv.service.transport.TransportStream;
import javax.tv.service.transport.TransportStreamChangeEvent;
import javax.tv.service.transport.TransportStreamChangeListener;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.ocap.net.OcapLocator;

import org.cablelabs.impl.manager.service.CannedSIDatabase.TransportHandleImpl;
import org.cablelabs.impl.service.NetworkLocator;
import org.cablelabs.impl.service.javatv.navigation.ServiceDetailsImpl;
import org.cablelabs.test.SICannedConcreteTest;

/**
 * <p>
 * CannedTransportExtTest is used to test the TransportExt interface using
 * canned data and behavior from the canned testing environment. This test is
 * only an interface test and does not do any testing on any methods not
 * specified in the TransportExt interface.
 * </p>
 * <p>
 * Most of TransportExt consists of getter methods. The testing done on these is
 * thorough, yet simple, and will only be briefly described at each
 * corresponding method. However, the tests performed on the add/remove listener
 * methods and the retrieve methods are substantially more complex and will be
 * described in much more detail. These tests consists of two groups, the aptly
 * named <i>Add/Remove Listener Method Tests</i> and <i>Retrieval Method
 * Tests</i>. These tests are described below.
 * </p>
 * <p>
 * <i>Add/Remove Listener Method Tests</i><br>
 * This will test the add and remove listener functionality of the TransportExt
 * interface. These tests follow the same steps for each type of listener to be
 * added/removed. They are described as follows:
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
 * TransportExt object. Again, the tests for each of these retrieval methods
 * follow the same steps. They are as follows:
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
public class TransportImplCannedTest extends SICannedConcreteTest
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

    // Setup section \\

    /**
     * No-arg constructor for creating our test case.
     * 
     */
    public TransportImplCannedTest()
    {
        super("TransportImplCannedTest");
    }

    /**
     * Creates a test suite to for use in the testing environment.
     * 
     * @return An InterfaceTestSuite that contains the tests in this test case.
     */
    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(TransportImplCannedTest.class);
        suite.setName(TransportImpl.class.getName());
        return suite;
    }

    /**
     * Sets up our test
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        handle = new TransportHandleImpl(1);
        delivery = DeliverySystemType.CABLE;
        tsid = 199;
        transport = new TransportImpl(sic, handle, delivery, tsid, null);
        listener = new ChangeListenerTest();
    }

    /**
     * Clean up after the test
     */
    protected void tearDown() throws Exception
    {
        transport = null;
        handle = null;
        delivery = null;
        listener = null;

        super.tearDown();
    }

    // Test Section \\

    /**
     * Tests the constructor and the associated getter methods for proper
     * setting and retrieval of data.
     */
    public void testConstructor()
    {
        assertEquals("Handle does not match", handle, transport.getTransportHandle());
        assertEquals("DeliverySystemType does not match", delivery, transport.getDeliverySystemType());
        assertEquals("TransportID does not match", tsid, transport.getTransportID());
    }

    /**
     * Tests <code>addServiceDetailsChangeListener</code> and
     * <code>removeServiceDetailsChangeListener</code> to make sure it properly
     * adds and removes listeners accordingly.
     */
    public void testAddRemoveServiceDetailsChangeListener() throws Exception
    {
        // /////////////////////////////////////////////////////////////////////
        // Add SI Section
        // /////////////////////////////////////////////////////////////////////

        // Get a reference to a transport
        TransportImpl localTransport = csidb.transport1;
        // Add the listener to the cache.
        localTransport.addServiceDetailsChangeListener(listener);
        ServiceDetailsImpl sd1 = null;
        // Fire an event from the CannedSIDatabase
        synchronized (listener)
        {
            sd1 = (ServiceDetailsImpl) csidb.cannedAddSI(csidb.serviceDetails33);
            listener.wait(waitForRequest);
        }
        // Check to see that the listener was notified and contains the right
        // data.
        assertTrue("ServiceDetailsChangeListener was not notified.", listener.sdChange);
        assertTrue("Returned ServiceDetails is incorrect", sd1.equals(listener.sdEvent.getServiceDetails()));
        assertTrue("Returned transport is incorrect", localTransport.equals(listener.sdEvent.getTransport()));
        assertEquals("Returned change type is incorrect", SIChangeType.ADD, listener.sdEvent.getChangeType());
        // Now we'll update another piece of SI, but on a different transport
        listener.reset();
        ServiceDetailsImpl sd2 = null;
        // Wait for the event and check the results
        synchronized (listener)
        {
            sd2 = (ServiceDetailsImpl) csidb.cannedAddSI(csidb.serviceDetails40);
            listener.wait(waitForRequest);
        }
        assertFalse("ServiceDetailsChangeListener was incorrectly notified", listener.sdChange);

        // /////////////////////////////////////////////////////////////////////
        // Modify SI Section
        // /////////////////////////////////////////////////////////////////////

        // Reset the listener
        listener.reset();
        // Wait for our event to fire
        synchronized (listener)
        {
            // Update our ServiceDetails object
            sd1 = (ServiceDetailsImpl) csidb.cannedUpdateSI(sd1);
            listener.wait(waitForRequest);
        }
        // Check the results
        assertTrue("ServiceDetailsChangeListener was not notified.", listener.sdChange);
        assertTrue("Returned ServiceDetails is incorrect", sd1.equals(listener.sdEvent.getServiceDetails()));
        assertTrue("Returned transport is incorrect", localTransport.equals(listener.sdEvent.getTransport()));
        assertEquals("Returned change type is incorrect", SIChangeType.MODIFY, listener.sdEvent.getChangeType());
        // Now we'll reset our listener
        listener.reset();
        // Wait for the non-notification
        synchronized (listener)
        {
            // Update the other piece of SI and make sure we aren't notified
            csidb.cannedUpdateSI(sd2);
            listener.wait(waitForRequest);
        }
        // Make sure that the listener was not notified
        assertFalse("ServiceDetailsChangeListener was incorrectly notified", listener.sdChange);

        // /////////////////////////////////////////////////////////////////////
        // Remove SI Section
        // /////////////////////////////////////////////////////////////////////

        // Reset the listener
        listener.reset();
        // Wait for our event to fire
        synchronized (listener)
        {
            // Remove our ServiceDetails object
            csidb.cannedRemoveSI(sd1);
            listener.wait(waitForRequest);
        }
        // Check the results
        assertTrue("ServiceDetailsChangeListener was not notified.", listener.sdChange);
        assertTrue("Returned ServiceDetails is incorrect", sd1.equals(listener.sdEvent.getServiceDetails()));
        assertTrue("Returned transport is incorrect", localTransport.equals(listener.sdEvent.getTransport()));
        assertEquals("Returned change type is incorrect", SIChangeType.REMOVE, listener.sdEvent.getChangeType());
        // Now we'll reset our listener
        listener.reset();

        // Wait for the non-notification
        synchronized (listener)
        {
            // Update the other piece of SI
            csidb.cannedRemoveSI(sd2);
            listener.wait(waitForRequest);
        }
        // Make sure that the listener was not notified
        assertFalse("ServiceDetailsChangeListener was incorrectly notified", listener.sdChange);

        // /////////////////////////////////////////////////////////////////////
        listener.reset();
        // See if we can add the listener more than once
        localTransport.addServiceDetailsChangeListener(listener);
        synchronized (listener)
        {
            // Fire the change
            csidb.cannedUpdateSI(csidb.serviceDetails33);
            listener.wait(waitForRequest);
        }
        assertEquals("Listener was incorrectly notified more than once", 1, listener.events);

        // Finally, we're going to remove our listener
        listener.reset();
        localTransport.removeServiceDetailsChangeListener(listener);
        // Wait for the non-notification
        synchronized (listener)
        {
            // Fire the change
            csidb.cannedUpdateSI(csidb.serviceDetails33);
            listener.wait(waitForRequest);
        }
        // Make sure that the listener was not notified
        assertFalse("ServiceDetailsChangeListener was incorrectly notified", listener.sdChange);

    }

    /**
     * Tests <code>addNetworkChangeListener</code> and
     * <code>removeNetworkChangeListener</code> to make sure it properly adds
     * and removes listeners accordingly.
     */
    public void testAddRemoveNetworkChangeListener() throws Exception
    {
        // /////////////////////////////////////////////////////////////////////
        // Add SI Section
        // /////////////////////////////////////////////////////////////////////

        // Get a reference to a transport
        TransportImpl localTransport = csidb.transport1;
        // Add the listener to the cache.
        localTransport.addNetworkChangeListener(listener);
        // Fire an event from the CannedSIDatabase
        NetworkImpl net1 = null;
        // Check to see that the listener was notified and contains the right
        // data.
        synchronized (listener)
        {
            net1 = (NetworkImpl) csidb.cannedAddSI(csidb.network3);
            listener.wait(waitForRequest);
        }
        assertTrue("NetworkChangeListener was not notified.", listener.netChange);
        assertTrue("Returned Network is incorrect", net1.equals(listener.netEvent.getNetwork()));
        assertTrue("Returned transport is incorrect", localTransport.equals(listener.netEvent.getTransport()));
        assertEquals("Returned change type is incorrect", SIChangeType.ADD, listener.netEvent.getChangeType());
        // Now we'll update another piece of SI, but on a different transport
        listener.reset();
        NetworkImpl net2 = null;
        // Wait for the event and check the result
        synchronized (listener)
        {
            net2 = (NetworkImpl) csidb.cannedAddSI(csidb.network6);
            listener.wait(waitForRequest);
        }
        assertFalse("NetworkChangeListener was incorrectly notified", listener.netChange);

        // /////////////////////////////////////////////////////////////////////
        // Modify SI Section
        // /////////////////////////////////////////////////////////////////////

        // Reset the listener
        listener.reset();
        // Wait for our event to fire
        synchronized (listener)
        {
            // Update our Network object
            net1 = (NetworkImpl) csidb.cannedUpdateSI(net1);
            listener.wait(waitForRequest);
        }
        // Check the results
        assertTrue("Network was not notified.", listener.netChange);
        assertTrue("Returned Network is incorrect", net1.equals(listener.netEvent.getNetwork()));
        assertTrue("Returned transport is incorrect", localTransport.equals(listener.netEvent.getTransport()));
        assertEquals("Returned change type is incorrect", SIChangeType.MODIFY, listener.netEvent.getChangeType());
        // Now we'll reset our listener
        listener.reset();
        // Wait for the non-notification
        synchronized (listener)
        {
            // Update the other piece of SI and make sure we aren't notified
            csidb.cannedUpdateSI(net2);
            listener.wait(waitForRequest);
        }
        // Make sure that the listener was not notified
        assertFalse("NetworkChangeListener was incorrectly notified", listener.netChange);

        // /////////////////////////////////////////////////////////////////////
        // Remove SI Section
        // /////////////////////////////////////////////////////////////////////

        // Reset the listener
        listener.reset();

        // Wait for our event to fire
        synchronized (listener)
        {
            // Remove our Network object
            csidb.cannedRemoveSI(net1);
            listener.wait(waitForRequest);
        }
        // Check the results
        assertTrue("NetworkChangeListener was not notified.", listener.netChange);
        assertTrue("Returned Network is incorrect", net1.equals(listener.netEvent.getNetwork()));
        assertTrue("Returned transport is incorrect", localTransport.equals(listener.netEvent.getTransport()));
        assertEquals("Returned change type is incorrect", SIChangeType.REMOVE, listener.netEvent.getChangeType());
        // Now we'll reset our listener
        listener.reset();

        // Wait for the non-notification
        synchronized (listener)
        {
            // Update the other piece of SI
            csidb.cannedRemoveSI(net2);
            listener.wait(waitForRequest);
        }
        // Make sure that the listener was not notified
        assertFalse("NetworkChangeListener was incorrectly notified", listener.netChange);

        // /////////////////////////////////////////////////////////////////////

        listener.reset();
        // See if we can add the listener more than once
        localTransport.addNetworkChangeListener(listener);
        synchronized (listener)
        {
            // Fire the change
            csidb.cannedUpdateSI(csidb.network3);
            listener.wait(waitForRequest);
        }
        assertEquals("Listener was incorrectly notified more than once", 1, listener.events);

        // Finally, we're going to remove our listener
        listener.reset();
        localTransport.removeNetworkChangeListener(listener);
        // Wait for the non-notification
        synchronized (listener)
        {
            // Fire the change
            csidb.cannedUpdateSI(csidb.network6);
            listener.wait(waitForRequest);
        }
        // Make sure that the listener was not notified
        assertFalse("NetworkChangeListener was incorrectly notified", listener.netChange);
    }

    /**
     * Tests <code>addTransportStreamChangeListener</code> and
     * <code>removeTransportStreamChangeListener</code> to make sure it properly
     * adds and removes listeners accordingly.
     */
    public void testAddRemoveTransportStreamChangeListener() throws Exception
    {
        // /////////////////////////////////////////////////////////////////////
        // Add SI Section
        // /////////////////////////////////////////////////////////////////////

        // Get a reference to a transport
        TransportImpl localTransport = csidb.transport1;
        // Add the listener to the cache.
        localTransport.addTransportStreamChangeListener(listener);
        // Fire an event from the CannedSIDatabase
        TransportStreamImpl ts1 = null;
        // Check to see that the listener was notified and contains the right
        // data.
        synchronized (listener)
        {
            ts1 = (TransportStreamImpl) csidb.cannedAddSI(csidb.transportStream7);
            listener.wait(waitForRequest);
        }
        assertTrue("TransportStreamChangeListener was not notified.", listener.tsChange);
        assertTrue("Returned TransportStream is incorrect", ts1.equals(listener.tsEvent.getTransportStream()));
        assertTrue("Returned transport is incorrect", localTransport.equals(listener.tsEvent.getTransport()));
        assertEquals("Returned change type is incorrect", SIChangeType.ADD, listener.tsEvent.getChangeType());
        // Now we'll update another piece of SI, but on a different transport
        listener.reset();
        TransportStreamImpl ts2 = null;
        synchronized (listener)
        {
            ts2 = (TransportStreamImpl) csidb.cannedAddSI(csidb.transportStream14);
            listener.wait(waitForRequest);
        }
        assertFalse("TransportStreamChangeListener was incorrectly notified", listener.tsChange);

        // /////////////////////////////////////////////////////////////////////
        // Modify SI Section
        // /////////////////////////////////////////////////////////////////////

        // Reset the listener
        listener.reset();
        // Wait for our event to fire
        synchronized (listener)
        {
            // Update our TransportStream object
            ts1 = (TransportStreamImpl) csidb.cannedUpdateSI(ts1);
            listener.wait(waitForRequest);
        }
        // Check the results
        assertTrue("TransportStreamChangeListener was not notified.", listener.tsChange);
        assertTrue("Returned TransportStream is incorrect", ts1.equals(listener.tsEvent.getTransportStream()));
        assertTrue("Returned transport is incorrect", localTransport.equals(listener.tsEvent.getTransport()));
        assertEquals("Returned change type is incorrect", SIChangeType.MODIFY, listener.tsEvent.getChangeType());
        // Now we'll reset our listener
        listener.reset();
        // Wait for the non-notification
        synchronized (listener)
        {
            // Update the other piece of SI and make sure we aren't notified
            csidb.cannedUpdateSI(ts2);
            listener.wait(waitForRequest);
        }
        // Make sure that the listener was not notified
        assertFalse("TransportStreamChangeListener was incorrectly notified", listener.tsChange);

        // /////////////////////////////////////////////////////////////////////
        // Remove SI Section
        // /////////////////////////////////////////////////////////////////////

        // Reset the listener
        listener.reset();
        // Wait for our event to fire
        synchronized (listener)
        {
            // Remove our TransportStream object
            csidb.cannedRemoveSI(ts1);
            listener.wait(waitForRequest);
        }
        // Check the results
        assertTrue("TransportStreamChangeListener was not notified.", listener.tsChange);
        assertTrue("Returned TransportStream is incorrect", ts1.equals(listener.tsEvent.getTransportStream()));
        assertTrue("Returned transport is incorrect", localTransport.equals(listener.tsEvent.getTransport()));
        assertEquals("Returned change type is incorrect", SIChangeType.REMOVE, listener.tsEvent.getChangeType());
        // Now we'll reset our listener
        listener.reset();

        // Wait for the non-notification
        synchronized (listener)
        {
            // Update the other piece of SI
            csidb.cannedRemoveSI(ts2);
            listener.wait(waitForRequest);
        }
        // Make sure that the listener was not notified
        assertFalse("TransportStreamChangeListener was incorrectly notified", listener.tsChange);

        // /////////////////////////////////////////////////////////////////////

        listener.reset();
        // See if we can add the listener more than once
        localTransport.addTransportStreamChangeListener(listener);
        synchronized (listener)
        {
            // Fire the change
            csidb.cannedUpdateSI(csidb.transportStream10);
            listener.wait(waitForRequest);
        }
        assertEquals("Listener was incorrectly notified more than once", 1, listener.events);

        // Finally, we're going to remove our listener
        listener.reset();
        localTransport.removeTransportStreamChangeListener(listener);
        // Wait for the non-notification
        synchronized (listener)
        {
            // Fire the change
            csidb.cannedUpdateSI(csidb.transportStream7);
            listener.wait(waitForRequest);
        }
        // Make sure that the listener was not notified
        assertFalse("TransportStreamChangeListener was incorrectly notified", listener.tsChange);
    }

    /**
     * Tests <code>retrieveNetwork</code> to make sure it returns the proper
     * network requested using the corresponding locator. This test is performed
     * asynchronously to simulate the response of the SIDatabase.
     */
    public void testRetrieveNetwork() throws Exception
    {
        SIRequestorTest requestor = new SIRequestorTest();
        NetworkLocator locator = new NetworkLocator(csidb.transport1.getTransportID(), 1);
        csidb.transport1.retrieveNetwork(locator, requestor);
        requestor.waitForEvents(1, waitForRequest);
        assertTrue("Requestor was not notified of success", requestor.succeeded());
        SIRetrievable[] results = requestor.getResults();
        assertTrue("Results returned were not of the right type", results[0] instanceof Network);
    }

    /**
     * Tests <code>retrieveNetworks</code> to make sure it properly returns all
     * of the networks associated with this transport. This test is performed
     * asynchronously to simulate the response of the SIDatabase.
     */
    public void testRetrieveNetworks() throws Exception
    {
        SIRequestorTest requestor = new SIRequestorTest();
        csidb.transport1.retrieveNetworks(requestor);
        requestor.waitForEvents(1, waitForRequest);
        assertTrue("Requestor was not notified of success", requestor.succeeded());
        SIRetrievable[] results = requestor.getResults();
        assertTrue("Results returned were not of the right type", results[0] instanceof Network);

        // TODO Add test for zero length array being returned
    }

    /**
     * Tests <code>retrieveTransportStream</code> to make sure it returns the
     * proper transport stream requested using the corresponding locator. This
     * test is performed asynchronously to simulate the response of the
     * SIDatabase.
     */
    public void testRetrieveTransportStream() throws Exception
    {

        SIRequestorTest requestor = new SIRequestorTest();
        OcapLocator locator = new OcapLocator(5000, 1);
        csidb.transport1.retrieveTransportStream(locator, requestor);
        requestor.waitForEvents(1, waitForRequest);
        assertTrue("Requestor was not notified of success", requestor.succeeded());
        SIRetrievable[] results = requestor.getResults();
        assertTrue("Results returned were not of the right type", results[0] instanceof TransportStream);
    }

    /**
     * Tests <code>retrieveTransportStreams</code> to make sure it properly
     * returns all of the networks associated with this transport. This test is
     * performed asynchronously to simulate the response of the SIDatabase.
     */
    public void testRetrieveTransportStreams() throws Exception
    {
        SIRequestorTest requestor = new SIRequestorTest();
        csidb.transport1.retrieveTransportStreams(requestor);
        requestor.waitForEvents(1, waitForRequest);
        assertTrue("Requestor was not notified of success", requestor.succeeded());
        SIRetrievable[] results = requestor.getResults();
        assertTrue("Results returned were not of the right type", results[0] instanceof TransportStream);

        // TODO Add test for zero length array being returned
    }

    // Data Section \\

    /**
     * Holds the instance of the TransportExt object we are testing.
     */
    private TransportImpl transport;

    private TransportHandleImpl handle;

    private DeliverySystemType delivery;

    private ChangeListenerTest listener;

    private int tsid;

    /**
     * All encompassing listener for listener tests. This class implements all 4
     * types of listeners required for testing the listener functionality of
     * SICache.
     * 
     * @author Joshua Keplinger
     */
    protected class ChangeListenerTest implements NetworkChangeListener, TransportStreamChangeListener,
            ServiceDetailsChangeListener
    {

        public volatile boolean tsChange = false;

        public volatile boolean sdChange = false;

        public volatile boolean netChange = false;

        public TransportStreamChangeEvent tsEvent = null;

        public ServiceDetailsChangeEvent sdEvent = null;

        public NetworkChangeEvent netEvent = null;

        public int events = 0;

        public synchronized void notifyChange(TransportStreamChangeEvent event)
        {
            tsEvent = event;
            tsChange = true;
            events++;
            this.notifyAll();
        }

        public synchronized void notifyChange(ServiceDetailsChangeEvent event)
        {
            sdEvent = event;
            sdChange = true;
            events++;
            this.notifyAll();
        }

        public synchronized void notifyChange(NetworkChangeEvent event)
        {
            netEvent = event;
            netChange = true;
            events++;
            this.notifyAll();
        }

        public void reset()
        {
            tsChange = false;
            sdChange = false;
            netChange = false;
            tsEvent = null;
            sdEvent = null;
            netEvent = null;
            events = 0;
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

        public void notifyFailure(SIRequestFailureType reason)
        {
            failtype = reason;
            assertNotNull("Failure type should not be null", failtype);
            success = true;
        }

        public void notifySuccess(SIRetrievable[] result)
        {
            this.results = result;
            assertNotNull("Results should not be null", results);
            this.success = true;
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

        public synchronized void waitForEvents(int numEvents, long millis) throws InterruptedException
        {
            /*
             * long end = System.currentTimeMillis() + millis; while(millis > 0
             * && results.length < numEvents) { wait(millis);
             * 
             * millis = end - System.currentTimeMillis(); }
             */
            for (int i = 0; i < millis / 100; i++)
            {
                if (success) break;
                // wait(100);
                Thread.sleep(100);
            }
        }
    }
}
