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
//        Â·Redistributions of source code must retain the above copyright notice, this list 
//             of conditions and the following disclaimer.
//        Â·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
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
package org.cablelabs.impl.service;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.RatingDimension;
import javax.tv.service.SIChangeType;
import javax.tv.service.SIException;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.SIRetrievable;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceComponent;
import javax.tv.service.navigation.ServiceComponentChangeEvent;
import javax.tv.service.navigation.ServiceComponentChangeListener;
import javax.tv.service.navigation.ServiceDescription;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.transport.Network;
import javax.tv.service.transport.NetworkChangeEvent;
import javax.tv.service.transport.NetworkChangeListener;
import javax.tv.service.transport.ServiceDetailsChangeEvent;
import javax.tv.service.transport.ServiceDetailsChangeListener;
import javax.tv.service.transport.Transport;
import javax.tv.service.transport.TransportStream;
import javax.tv.service.transport.TransportStreamChangeEvent;
import javax.tv.service.transport.TransportStreamChangeListener;

import junit.textui.TestRunner;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.davic.net.tuning.NetworkInterfaceController;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.ocap.net.OcapLocator;

import org.cablelabs.impl.manager.service.CannedSIDatabase;
import org.cablelabs.impl.manager.service.CannedServiceMgr;
import org.cablelabs.impl.manager.service.ServiceCollection;
import org.cablelabs.impl.service.javatv.navigation.ServiceComponentImpl;
import org.cablelabs.impl.service.javatv.navigation.ServiceDetailsImpl;
import org.cablelabs.impl.service.javatv.transport.NetworkImpl;
import org.cablelabs.impl.service.javatv.transport.TransportStreamImpl;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.test.SICannedInterfaceTest;

/**
 * <p>
 * Testing for the SICache interface will consist of three main parts:
 * Caching/Flushing Testing, Asynchronous Retrieval Testing, and Add/Remove
 * Listener Testing. The Caching/Flushing Testing will check the cache and flush
 * capabilities. The Asynchronous Retrieval Testing, will test the asynchronous
 * retrieval of SI, listener notification of acquired data, proper failure
 * notifications, and correct handling of cancellation requests. Finally, the
 * Add/Remove Listener testing will check the add/remove listener
 * functionalities and test those by firing various events.
 * </p>
 * <p>
 * <b>Caching/Flushing Testing</b> – These tests are designed to ensure that the
 * SICache is properly caching and flushing data as required. These tests will
 * consist of the following steps:
 * <ol type="1">
 * <li><i>Caching Test:</i>
 * <ol type="a">
 * <li>Send a request for SI that the SICache does not contain.</li>
 * <li>Retrieve the result from the SIRequestor and check it accordingly.</li>
 * <li>Check the CannedSIDatabase to see if the SICache contacted it correctly.</li>
 * <li>Resend the request for the same SI since it should now be within the
 * cache.</li>
 * <li>Retrieve the result from the SIRequestor and check it for consistency.</li>
 * <li>Check the CannedSIDatabase to see if the SICache contacted it (it should
 * not have.)</li>
 * </ol>
 * </li>
 * <li><i>Flush Test:</i>
 * <ol type="a">
 * <li>Force the CannedSIDatabase to send out an SIUpdateEvent so that the
 * SICache will flush.</li>
 * <li>Send a request for the same SI as before.</li>
 * <li>Retrieve the result from the SIRequestor and check it.</li>
 * <li>Check the CannedSIDatabase to see if SICache contacted it correctly for
 * the new SI.</li>
 * </ol>
 * </li>
 * </ol>
 * </p>
 * <p>
 * <b>Asynchronous Retrieval Testing</b> – This test will check for seven
 * different outcomes that are possible from any given request for retrieval.
 * These are described as follows:
 * <ol type="1">
 * <li><i>SICache has SI already:</i>
 * <ol type="a">
 * <li>Send request for SI and receive SIRequest object.</li>
 * <li>Check requestor object for returned SI.</li>
 * <li>Check that CannedSIDatabase was not contacted for SI.</li>
 * </ol>
 * </li>
 * <li><i>SICache does not have SI, but it is available:</i>
 * <ol type="a">
 * <li>Send request for SI and receive SIRequest object.</li>
 * <li>Check requestor object for returned SI.</li>
 * <li>Check CannedSIDatabase to see that it was properly contacted for SI.</li>
 * </ol>
 * </li>
 * <li><i>SICache does not have SI, and it is no longer available:</i>
 * <ol type="a">
 * <li>Force CannedSIDatabase to throw SINotAvailableException.</li>
 * <li>Send request for SI and receive SIRequest object.</li>
 * <li>Check requestor for notification of failure.</li>
 * <li>Check CannedSIDatabase to see that SI was requested properly.</li>
 * </ol>
 * </li>
 * <li><i>SICache does not have SI, and it is not available yet:</i>
 * <ol type="a">
 * <li>Force CannedSIDatabase to throw SINotAvailableYetException.</li>
 * <li>Send request for SI and receive SIRequest object.</li>
 * <li>Wait a predetermined period of time.</li>
 * <li>Force CannedSIDatabase to send SIUpdateEvent so SICache can retrieve SI
 * and notify listeners.</li>
 * <li>Check listener for proper notification, retrieve SI, and check
 * accordingly.</li>
 * <li>Check CannedSIDatabase to see if SI was requested properly.</li>
 * </ol>
 * </li>
 * <li><i>SICache does not have SI, it is not available yet, and the request
 * times out:</i>
 * <ol type="a">
 * <li>Force CannedSIDatabase to throw SINotAvailableYetException.</li>
 * <li>Send request for SI and receive SIRequest object.</li>
 * <li>Wait for the cache to timeout.</li>
 * <li>Check requestor for proper failure notification.</li>
 * </ol>
 * </li>
 * <li><i>SICache does not have SI, it is not available yet, but a cancellation
 * is called before delivered results:</i>
 * <ol type="a">
 * <li>Force CannedSIDatabase to throw SINotAvailableYetException.</li>
 * <li>Send request for SI and receive SIRequest object.</li>
 * <li>Call cancel on SIRequest object.</li>
 * <li>Force CannedSIDatabase to send SIUpdateEvent so SICache can retrieve SI
 * and notify listeners.</li>
 * <li>Check listener to make sure it was not notified of update event.</li>
 * </ol>
 * </li>
 * <li><i>SICache does not have SI, it is not available yet and will time out,
 * and a cancellation is called after the timeout notification:</i>
 * <ol type="a">
 * <li>Force CannedSIDatabase to throw SINotAvailableYetException.</li>
 * <li>Send request for SI and receive SIRequest object.</li>
 * <li>Wait for the cache to timeout.</li>
 * <li>Check listener to make sure it did receive the failure notification.</li>
 * <li>Call cancel on SIRequest object.</li>
 * <li>Make sure the call to cancel fails.</li>
 * </ol>
 * </li>
 * </ol>
 * </p>
 * <p>
 * NOTE: All of these scenarios will be tested under the following conditions:
 * <ul>
 * <li>Single request for SI</li>
 * <li>Multiple requests for same SI and Multiple requests for different pieces
 * of SI of the same SI type (wording?)</li>
 * <li>Multiple requests for different SI types</li>
 * </ul>
 * This is to ensure that the queuing mechanism is properly delivering events to
 * the correct listeners for each request for SI and that when listeners are
 * removed, they are not notified.
 * </p>
 * <p>
 * <b>Add/Remove Listener Testing</b> - This test will be performed the same way
 * other listener tests are done. The steps are as follows:
 * <ol>
 * <li>Register a listener with the SICache.</li>
 * <li>Fire an event from the CannedSIDatabase.</li>
 * <li>Check to see if the listener was notified.</li>
 * <li>Remove the listener from SICache.</li>
 * <li>Fire another event from the CannedSIDatabase.</li>
 * <li>Check to make sure the listener was not notified.</li>
 * </ol>
 * </p>
 * 
 * @author Joshua Keplinger
 */
public class SICacheCannedTest extends SICannedInterfaceTest
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

    // Setup Section \\

    /**
     * No-arg constructor for creating our test case.
     */
    public SICacheCannedTest()
    {
        super("SICacheCannedTest", SICache.class, new CannedSICacheTestFactory());
    }

    /**
     * Creates our test suite to be used in the test.
     * 
     * @return a test suite to be run
     */
    public static InterfaceTestSuite isuite()
    {
        InterfaceTestSuite suite = new InterfaceTestSuite(SICacheCannedTest.class);
        suite.setName(SICache.class.getName());
        suite.addFactory(new CannedSICacheTestFactory());
        return suite;
    }

    /**
     * Sets up our tests for a clean run each time.
     */
    protected void setUp() throws Exception
    {

        super.setUp();

        requestor = new SIRequestorTest();
        language = "eng";
        listener = new ChangeListenerTest();

    }

    /**
     * Cleans up after each test that is run.
     */
    protected void tearDown() throws Exception
    {
        requestor = null;
        language = null;
        listener = null;

        super.tearDown();

    }

    // Test Section \\

    // /////////////////////////////////////////////////////////////////////////
    // Add/Remove Listener test section
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Tests <code>addNetworkChangeListener</code> and
     * <code>removeNetworkChangeListener</code> to make sure they properly add
     * and remove listeners.
     */
    public void testAddRemoveNetworkChangeListener() throws Exception
    {
        // /////////////////////////////////////////////////////////////////////
        // Add SI Section
        // /////////////////////////////////////////////////////////////////////

        // Add the listener to the cache.
        sic.addNetworkChangeListener(listener);
        // Fire an event from the CannedSIDatabase
        NetworkImpl net1 = (NetworkImpl) csidb.cannedAddSI(csidb.network3);
        // Check to see that the listener was notified and contains the right
        // data.
        listener.waitForEvents(1);
        assertTrue("NetworkChangeListener was not notified.", listener.netChange);
        assertEquals("Returned Network is incorrect", net1, listener.netEvent.getNetwork());
        assertEquals("Returned transport is incorrect", csidb.transport1, listener.netEvent.getTransport());
        assertEquals("Returned change type is incorrect", SIChangeType.ADD, listener.netEvent.getChangeType());
        // Now we'll attempt a retrieval and make sure the cache didn't
        // go back to the database and returns the right results
        sic.retrieveNetwork(net1.getLocator(), requestor);
        requestor.waitForEvents();
        assertTrue("Requestor was not notified", requestor.success);
        // Make sure the cache didn't go to the database for the object
        assertEquals("Network creation count is incorrect", 0,
                csidb.cannedGetHandleCreateCount(net1.getNetworkHandle()));
        // Check the returned array for the newly added object
        assertEquals("Returned array length is incorrect", 1, requestor.results.length);
        assertEquals("Cached network does not match expected result", net1, requestor.results[0]);

        // /////////////////////////////////////////////////////////////////////
        // Modify SI Section
        // /////////////////////////////////////////////////////////////////////

        // Reset the listener and requestor
        listener.reset();
        requestor.reset();
        // Update our Network object
        net1 = (NetworkImpl) csidb.cannedUpdateSI(net1);
        // Wait for our event to fire
        listener.waitForEvents(1);
        // Check the results
        assertTrue("Network was not notified.", listener.netChange);
        assertEquals("Returned Network is incorrect", net1, listener.netEvent.getNetwork());
        assertEquals("Returned transport is incorrect", csidb.transport1, listener.netEvent.getTransport());
        assertEquals("Returned change type is incorrect", SIChangeType.MODIFY, listener.netEvent.getChangeType());
        // Attempt retrieval to make sure cache didn't go to the database
        // and that it returns the right data
        sic.retrieveNetwork(net1.getLocator(), requestor);
        requestor.waitForEvents();
        // Make sure the cache didn't go to the database to get the object
        assertEquals("Network creation count is incorrect", 0,
                csidb.cannedGetHandleCreateCount(net1.getNetworkHandle()));
        // Make sure the returned array contains our modified object
        assertEquals("Cached network does not match expected result", net1, requestor.results[0]);

        // /////////////////////////////////////////////////////////////////////
        // Remove SI Section
        // /////////////////////////////////////////////////////////////////////

        // Reset the listener and requestor
        listener.reset();
        requestor.reset();
        // Remove our Network object
        csidb.cannedRemoveSI(net1);
        // Wait for our event to fire
        listener.waitForEvents(1);
        // Check the results
        assertTrue("NetworkChangeListener was not notified.", listener.netChange);
        assertTrue("Returned Network is incorrect", net1.equals(listener.netEvent.getNetwork()));
        assertTrue("Returned transport is incorrect", csidb.transport1.equals(listener.netEvent.getTransport()));
        assertEquals("Returned change type is incorrect", SIChangeType.REMOVE, listener.netEvent.getChangeType());
        // Attempt retrieval for a non-existant object and make sure
        // that it failed with the right error code
        sic.retrieveNetwork(net1.getLocator(), requestor);
        requestor.waitForEvents();
        assertEquals("SICache returned wrong failure type or null", SIRequestFailureType.DATA_UNAVAILABLE,
                requestor.failtype);

        // /////////////////////////////////////////////////////////////////////

        // We're going to add the listener again to see if it gets notified
        // twice
        listener.reset();
        sic.addNetworkChangeListener(listener);
        // Fire the change
        csidb.cannedUpdateSI(csidb.network3);
        listener.waitForEvents(2);
        // Make sure the listener was only notified once
        assertEquals("Listener was incorrectly notified twice", 1, listener.eventReceived);

        // Finally, we're going to remove our listener
        listener.reset();
        sic.removeNetworkChangeListener(listener);
        // Fire the change
        csidb.cannedUpdateSI(csidb.network6);
        // Wait for the non-notification
        listener.waitForEvents(1);
        // Make sure that the listener was not notified
        assertFalse("NetworkChangeListener was incorrectly notified", listener.netChange);
    }

    /**
     * Tests <code>addServiceComponentChangeListener</code> and
     * <code>removeServiceComponentChangeListener</code> to make sure they
     * properly add and remove listeners.
     */
    public void testAddRemoveServiceComponentChangeListener() throws Exception
    {
        // /////////////////////////////////////////////////////////////////////
        // Add SI Section
        // /////////////////////////////////////////////////////////////////////

        // Get a reference to a transport
        ServiceDetailsImpl localSD = csidb.serviceDetails33;
        boolean included = false;
        // Add the listener to the cache.
        sic.addServiceComponentChangeListener(listener);
        // Fire an event from the CannedSIDatabase
        ServiceComponentImpl sc1 = (ServiceComponentImpl) csidb.cannedAddSI(csidb.serviceComponent69);
        // Check to see that the listener was notified and contains the right
        // data.
        listener.waitForEvents(1);
        assertTrue("ServiceComponentChangeListener was not notified.", listener.scChange);
        assertTrue("Returned ServiceComponent is incorrect", sc1.equals(listener.scEvent.getServiceComponent()));
        assertTrue("Returned transport is incorrect", localSD.equals(listener.scEvent.getServiceDetails()));
        assertEquals("Returned change type is incorrect", SIChangeType.ADD, listener.scEvent.getChangeType());
        // Now we'll attempt a retrieval and make sure the cache didn't
        // go back to the database and returns the right results
        sic.retrieveServiceComponents(localSD, "eng", requestor);
        requestor.waitForEvents();
        // Make sure the cache didn't go to the database for the object.
        assertEquals("ServiceComponent creation count is incorrect", 0,
                csidb.cannedGetHandleCreateCount(sc1.getServiceComponentHandle()));
        // Check the returned array for the newly added object
        assertEquals("Size of ServiceComponent array is incorrect", 6, requestor.results.length);
        for (int i = 0; i < requestor.results.length; i++)
        {
            if (sc1.equals(requestor.results[i]))
            {
                included = true;
                break;
            }
        }
        assertTrue("ServiceComponent was not included in request", included);

        // /////////////////////////////////////////////////////////////////////
        // Modify SI Section
        // /////////////////////////////////////////////////////////////////////

        // Reset the listener and requestor
        listener.reset();
        requestor.reset();
        included = false;
        // Update our ServiceComponent object
        sc1 = (ServiceComponentImpl) csidb.cannedUpdateSI(sc1);
        // Wait for our event to fire
        listener.waitForEvents(1);
        // Check the results
        assertTrue("ServiceComponentChangeListener was not notified.", listener.scChange);
        assertTrue("Returned ServiceComponent is incorrect", sc1.equals(listener.scEvent.getServiceComponent()));
        assertTrue("Returned transport is incorrect", localSD.equals(listener.scEvent.getServiceDetails()));
        assertEquals("Returned change type is incorrect", SIChangeType.MODIFY, listener.scEvent.getChangeType());
        // Now we'll attempt a retrieval and make sure the cache didn't
        // go back to the database and returns the right results
        sic.retrieveServiceComponents(localSD, "eng", requestor);
        requestor.waitForEvents();
        // Check that the cache didn't go to the database for the object
        assertEquals("ServiceComponent creation count is incorrect", 0,
                csidb.cannedGetHandleCreateCount(sc1.getServiceComponentHandle()));
        // Make sure modified object is in the resulting array
        for (int i = 0; i < requestor.results.length; i++)
        {
            if (sc1.equals(requestor.results[i]))
            {
                included = true;
                break;
            }
        }
        assertTrue("ServiceComponent was not included in request", included);

        // /////////////////////////////////////////////////////////////////////
        // Remove SI Section
        // /////////////////////////////////////////////////////////////////////

        // Reset the listener and requestor
        listener.reset();
        requestor.reset();
        included = false;
        // Remove our ServiceComponent object
        csidb.cannedRemoveSI(sc1);
        // Wait for our event to fire
        listener.waitForEvents(1);
        // Check the results
        assertTrue("ServiceComponentChangeListener was not notified.", listener.scChange);
        assertTrue("Returned ServiceComponent is incorrect", sc1.equals(listener.scEvent.getServiceComponent()));
        assertTrue("Returned transport is incorrect", localSD.equals(listener.scEvent.getServiceDetails()));
        assertEquals("Returned change type is incorrect", SIChangeType.REMOVE, listener.scEvent.getChangeType());
        // Retrieve results again
        sic.retrieveServiceComponents(localSD, "eng", requestor);
        requestor.waitForEvents();
        // Make sure the removed object isn't in the returned array
        for (int i = 0; i < requestor.results.length; i++)
        {
            if (sc1.equals(requestor.results[i]))
            {
                included = true;
                break;
            }
        }
        assertFalse("ServiceComponent was incorrectly included in request", included);

        // /////////////////////////////////////////////////////////////////////

        // We're going to add the listener again to see if it gets notified
        // twice
        listener.reset();
        sic.addServiceComponentChangeListener(listener);
        // Fire the change
        csidb.cannedUpdateSI(csidb.serviceComponent105);
        listener.waitForEvents(2);
        // Make sure the listener was only notified once
        assertEquals("Listener was incorrectly notified twice", 1, listener.eventReceived);

        // Finally, we're going to remove our listener
        listener.reset();
        sic.removeServiceComponentChangeListener(listener);
        // Fire the change
        csidb.cannedUpdateSI(csidb.serviceComponent69);
        // Wait for the non-notification
        listener.waitForEvents(1);
        // Make sure that the listener was not notified
        assertFalse("ServiceComponentChangeListener was incorrectly notified", listener.scChange);
    }

    /**
     * Tests <code>addServiceDetailsChangeListener</code> and
     * <code>removeServiceDetailsChangeListener</code> to make sure they
     * properly add and remove listeners.
     */
    public void testAddRemoveServiceDetailsChangeListener() throws Exception
    {
        // /////////////////////////////////////////////////////////////////////
        // Add SI Section
        // /////////////////////////////////////////////////////////////////////

        // Add the listener to the cache.
        sic.addServiceDetailsChangeListener(listener);
        boolean included = false;
        // Fire an event from the CannedSIDatabase
        ServiceDetailsImpl sd1 = (ServiceDetailsImpl) csidb.cannedAddSI(csidb.serviceDetails33);
        // Check to see that the listener was notified and contains the right
        // data.
        listener.waitForEvents(1);
        assertTrue("ServiceDetailsChangeListener was not notified.", listener.sdChange);
        assertTrue("Returned ServiceDetails is incorrect", sd1.equals(listener.sdEvent.getServiceDetails()));
        assertTrue("Returned transport is incorrect", csidb.transport1.equals(listener.sdEvent.getTransport()));
        assertEquals("Returned change type is incorrect", SIChangeType.ADD, listener.sdEvent.getChangeType());
        // Now we'll attempt a retrieval and make sure the cache didn't
        // go back to the database and returns the right results
        sic.retrieveServiceDetails(csidb.service15, "eng", true, requestor);
        requestor.waitForEvents();
        // Check that the cache didn't go to the database to get the object
        assertEquals("ServiceDetails creation count is incorrect", 0,
                csidb.cannedGetHandleCreateCount(sd1.getServiceDetailsHandle()));
        // Make sure the object was in the returned array
        for (int i = 0; i < requestor.results.length; i++)
        {
            if (sd1.equals(requestor.results[i]))
            {
                included = true;
                break;
            }
        }
        assertTrue("ServiceDetails was not included in request", included);

        // /////////////////////////////////////////////////////////////////////
        // Modify SI Section
        // /////////////////////////////////////////////////////////////////////

        // Reset the listener and requestor
        listener.reset();
        requestor.reset();
        included = false;
        // Update our ServiceDetails object
        sd1 = (ServiceDetailsImpl) csidb.cannedUpdateSI(sd1);
        // Wait for our event to fire
        listener.waitForEvents(1);
        // Check the results
        assertTrue("ServiceDetailsChangeListener was not notified.", listener.sdChange);
        assertTrue("Returned ServiceDetails is incorrect", sd1.equals(listener.sdEvent.getServiceDetails()));
        assertTrue("Returned transport is incorrect", csidb.transport1.equals(listener.sdEvent.getTransport()));
        assertEquals("Returned change type is incorrect", SIChangeType.MODIFY, listener.sdEvent.getChangeType());
        // Now we'll attempt a retrieval and make sure the cache didn't
        // go back to the database and returns the right results
        sic.retrieveServiceDetails(csidb.service15, "eng", true, requestor);
        requestor.waitForEvents();
        // Make sure the cache didn't go back to the database
        assertEquals("ServiceDetails creation count is incorrect", 0,
                csidb.cannedGetHandleCreateCount(sd1.getServiceDetailsHandle()));
        // Check to see if the object was in the returned array
        for (int i = 0; i < requestor.results.length; i++)
        {
            if (sd1.equals(requestor.results[i]))
            {
                included = true;
                break;
            }
        }
        assertTrue("ServiceDetails was not included in request", included);

        // /////////////////////////////////////////////////////////////////////
        // Remove SI Section
        // /////////////////////////////////////////////////////////////////////

        // Reset the listener and requestor
        listener.reset();
        requestor.reset();
        included = false;
        // Remove our ServiceDetails object
        csidb.cannedRemoveSI(sd1);
        // Wait for our event to fire
        listener.waitForEvents(1);
        // Check the results
        assertTrue("ServiceDetailsChangeListener was not notified.", listener.sdChange);
        assertTrue("Returned ServiceDetails is incorrect", sd1.equals(listener.sdEvent.getServiceDetails()));
        assertTrue("Returned transport is incorrect", csidb.transport1.equals(listener.sdEvent.getTransport()));
        assertEquals("Returned change type is incorrect", SIChangeType.REMOVE, listener.sdEvent.getChangeType());
        // Retrieve some results
        sic.retrieveServiceDetails(csidb.service15, "eng", true, requestor);
        requestor.waitForEvents();
        // Make sure that the removed object was not returned
        for (int i = 0; i < requestor.results.length; i++)
        {
            if (sd1.equals(requestor.results[i]))
            {
                included = true;
                break;
            }
        }
        assertFalse("ServiceDetails was incorrectly included in request", included);

        // /////////////////////////////////////////////////////////////////////

        // We're going to add the listener again to see if it gets notified
        // twice
        listener.reset();
        sic.addServiceDetailsChangeListener(listener);
        // Fire the change
        csidb.cannedUpdateSI(csidb.serviceDetails33);
        listener.waitForEvents(2);
        // Make sure the listener was only notified once
        assertEquals("Listener was incorrectly notified twice", 1, listener.eventReceived);

        // Finally, we're going to remove our listener
        listener.reset();
        sic.removeServiceDetailsChangeListener(listener);
        // Fire the change
        csidb.cannedUpdateSI(csidb.serviceDetails33);
        // Wait for the non-notification
        listener.waitForEvents(1);
        // Make sure that the listener was not notified
        assertFalse("ServiceDetailsChangeListener was incorrectly notified", listener.sdChange);
    }

    /**
     * Tests <code>addTransportStreamChangeListener</code> and
     * <code>removeTransportStreamChangeListener</code> to make sure they
     * properly add and remove listeners.
     */
    public void testAddRemoveTransportStreamChangeListener() throws Exception
    {
        // /////////////////////////////////////////////////////////////////////
        // Add SI Section
        // /////////////////////////////////////////////////////////////////////

        // Add the listener to the cache.
        sic.addTransportStreamChangeListener(listener);
        // Fire an event from the CannedSIDatabase
        TransportStreamImpl ts1 = (TransportStreamImpl) csidb.cannedAddSI(csidb.transportStream7);
        // Check to see that the listener was notified and contains the right
        // data.
        listener.waitForEvents(1);
        assertTrue("TransportStreamChangeListener was not notified.", listener.tsChange);
        assertTrue("Returned TransportStream is incorrect", ts1.equals(listener.tsEvent.getTransportStream()));
        assertTrue("Returned transport is incorrect", csidb.transport1.equals(listener.tsEvent.getTransport()));
        assertEquals("Returned change type is incorrect", SIChangeType.ADD, listener.tsEvent.getChangeType());
        // Now we'll attempt a retrieval and make sure the cache didn't
        // go back to the database and returns the right results
        sic.retrieveTransportStream(ts1.getLocator(), requestor);
        requestor.waitForEvents();
        assertTrue("Requestor was not notified", requestor.success);
        // Make sure the cache didn't go back to the database
        assertEquals("TransportStream creation count is incorrect", 0,
                csidb.cannedGetHandleCreateCount(ts1.getTransportStreamHandle()));
        assertEquals("Returned array length is incorrect", 1, requestor.results.length);
        // Make sure that it cached the right object
        assertEquals("Cached TransportStream does not match expected result", ts1, requestor.results[0]);

        // /////////////////////////////////////////////////////////////////////
        // Modify SI Section
        // /////////////////////////////////////////////////////////////////////

        // Reset the listener and requestor
        listener.reset();
        requestor.reset();
        // Update our TransportStream object
        ts1 = (TransportStreamImpl) csidb.cannedUpdateSI(ts1);
        // Wait for our event to fire
        listener.waitForEvents(1);
        // Check the results
        assertTrue("TransportStreamChangeListener was not notified.", listener.tsChange);
        assertTrue("Returned TransportStream is incorrect", ts1.equals(listener.tsEvent.getTransportStream()));
        assertTrue("Returned transport is incorrect", csidb.transport1.equals(listener.tsEvent.getTransport()));
        assertEquals("Returned change type is incorrect", SIChangeType.MODIFY, listener.tsEvent.getChangeType());
        // Now we'll attempt a retrieval and make sure the cache didn't
        // go back to the database and returns the right results
        sic.retrieveTransportStream(ts1.getLocator(), requestor);
        requestor.waitForEvents();
        assertTrue("Requestor was not notified", requestor.success);
        // Make sure that the cache didn't go get the object
        assertEquals("TransportStream creation count is incorrect", 0,
                csidb.cannedGetHandleCreateCount(ts1.getTransportStreamHandle()));
        assertEquals("Returned array length is incorrect", 1, requestor.results.length);
        // Check to make sure that it cached the right object
        assertEquals("Cached TransportStream does not match expected result", ts1, requestor.results[0]);

        // /////////////////////////////////////////////////////////////////////
        // Remove SI Section
        // /////////////////////////////////////////////////////////////////////

        // Reset the listener and requestor
        listener.reset();
        requestor.reset();
        // Remove our TransportStream object
        csidb.cannedRemoveSI(ts1);
        // Wait for our event to fire
        listener.waitForEvents(1);
        // Check the results
        assertTrue("TransportStreamChangeListener was not notified.", listener.tsChange);
        assertTrue("Returned TransportStream is incorrect", ts1.equals(listener.tsEvent.getTransportStream()));
        assertTrue("Returned transport is incorrect", csidb.transport1.equals(listener.tsEvent.getTransport()));
        assertEquals("Returned change type is incorrect", SIChangeType.REMOVE, listener.tsEvent.getChangeType());
        // Attempt retrieval for a non-existant object and make sure
        // that it failed with the right error code
        sic.retrieveTransportStream(ts1.getLocator(), requestor);
        requestor.waitForEvents();
        assertEquals("SICache returned wrong failure type or null", SIRequestFailureType.DATA_UNAVAILABLE,
                requestor.failtype);

        // /////////////////////////////////////////////////////////////////////

        // We're going to add the listener again to see if it gets notified
        // twice
        listener.reset();
        sic.addTransportStreamChangeListener(listener);
        // Fire the change
        csidb.cannedUpdateSI(csidb.transportStream10);
        listener.waitForEvents(2);
        // Make sure the listener was only notified once
        assertEquals("Listener was incorrectly notified twice", 1, listener.eventReceived);

        // Finally, we're going to remove our listener
        listener.reset();
        sic.removeTransportStreamChangeListener(listener);
        // Fire the change
        csidb.cannedUpdateSI(csidb.transportStream7);
        // Wait for the non-notification
        listener.waitForEvents(1);
        // Make sure that the listener was not notified
        assertFalse("TransportStreamChangeListener was incorrectly notified", listener.tsChange);
    }

    // /////////////////////////////////////////////////////////////////////////
    // 'Get' method test section
    // /////////////////////////////////////////////////////////////////////////

    /**
     * This tests <code>getService()</code> for proper retrieval, caching, and
     * exception throwing using known data from <code>CannedSIDatabase</code>.
     */
    public void testGetService() throws Exception
    {
        // Note: this test case relies on the use of the CannedSIDatabase and
        // known data
        OcapLocator locator = csidb.l100;
        ServiceExt service = null;

        try
        {
            // Try to get a Service
            service = (ServiceExt)sic.getService(locator, language);
        }
        catch (InvalidLocatorException e)
        {
            fail("Caught InvalidLocatorException: " + e.getMessage());
        }
        // Check for non-null array
        assertNotNull("Array should not be null using valid data", service);
        // Make sure we got a valid service object
        assertEquals("Service handle does not match expected value", csidb.service15.getServiceHandle(),
                service.getServiceHandle());
        // See if the cache went to get the object
        assertEquals("Creation count is incorrect", 1, csidb.cannedGetHandleCreateCount(service.getServiceHandle()));

        try
        {
            // Try to get the same object and check it
            assertEquals("Service returned does not match first value", service, (ServiceExt) sic.getService(locator,
                    language));
        }
        catch (InvalidLocatorException e)
        {
            fail("Caught InvalidLocatorException: " + e.getMessage());
        }
        // Make sure that the cache didn't go to get the object again
        assertEquals("Creation count is incorrect", 1, csidb.cannedGetHandleCreateCount(service.getServiceHandle()));

        try
        {
            // Try to get a Service
            service = (ServiceExt) sic.getService(locator, "fre");
        }
        catch (InvalidLocatorException e)
        {
            fail("Caught InvalidLocatorException: " + e.getMessage());
        }
        // Check for non-null array
        assertNotNull("Array should not be null using valid data", service);

        // See if the cache went to get the object
        assertEquals("Creation count is incorrect", 1, csidb.cannedGetHandleCreateCount(service.getServiceHandle()));

        try
        {
            // Try to get the same object and check it
            assertEquals("Service returned does not match first value", service, (ServiceExt) sic.getService(locator,
                    "fre"));
        }
        catch (InvalidLocatorException e)
        {
            fail("Caught InvalidLocatorException: " + e.getMessage());
        }
        // Make sure that the cache didn't go to get the object again
        assertEquals("Creation count is incorrect", 1, csidb.cannedGetHandleCreateCount(service.getServiceHandle()));

        // Now we'll try an invalid locator
        try
        {
            locator = new OcapLocator(1);
            sic.getService(locator, language);
            fail("Exception was not properly thrown using invalid locator");
        }
        catch (InvalidLocatorException e)
        {
            // We want this to happen, so nothing in here
        }

        // Make sure service reports locator using frequency/prog/mf if asked
        // for that way (even
        // if a source ID is known).
        try
        {
            ServiceExt ds = null;
            locator = new OcapLocator(5000, 2, 1);
            ds = (ServiceExt) sic.getService(locator, "eng");
            assertEquals("Locator does not match expected value", locator.toExternalForm(), ds.getLocator()
                    .toExternalForm());

        }
        catch (InvalidLocatorException e)
        {
            fail("InvalidLocatorException thrown using valid locator.");
        }

        // Make sure service reports locator using frequency/prog/mf if asked
        // for by name and
        // a source ID is not known.
        try
        {
            ServiceExt ds = null;
            locator = new OcapLocator("ocap://n=dynamicService1");
            ds = (ServiceExt) sic.getService(locator, "eng");
            locator = new OcapLocator(5000, 101, 1);
            assertEquals("Locator does not match expected value", locator.toExternalForm(), ds.getLocator()
                    .toExternalForm());

        }
        catch (InvalidLocatorException e)
        {
            fail("InvalidLocatorException thrown using valid locator.");
        }
    }

    /**
     * Tests <code>getAllServices()</code> for proper retrieval and caching on
     * known test data used in the <code>CannedSIDatabase</code>.
     */
    public void testGetAllServices()
    {
        // Get the array of services for the known language
        ServiceCollection collection = new ServiceCollection();
        sic.getAllServices(collection, language);
        ServiceExt[] services = (ServiceExt[]) collection.getServices().toArray(new ServiceExt[0]);
        // Check for non-null array
        assertNotNull("Array should not be null using valid data", services);
        // Makes sure that array has 20 elements
        assertEquals("Array length does not match expected value", 20, services.length);
        // See if the cache went to get the object
        assertEquals("Creation count is incorrect", 1,
                csidb.cannedGetHandleCreateCount(services[15].getServiceHandle()));

        // Now we're going to test a null language and make sure that we
        // get the same results back, since "eng" is the default, no pref.
        collection = new ServiceCollection();
        sic.getAllServices(collection, "");
        services = (ServiceExt[]) collection.getServices().toArray(new ServiceExt[0]);
        // Check for non-null array
        assertNotNull("Array should not be null using valid data", services);
        // Makes sure that array has 20 elements
        assertEquals("Array length does not match expected value", 20, services.length);
        // See if the cache went to get the object
        assertEquals("Creation count is incorrect", 1,
                csidb.cannedGetHandleCreateCount(services[15].getServiceHandle()));

        // Pass in random language and see if we get default language
        collection = new ServiceCollection();
        sic.getAllServices(collection, "spa");
        services = (ServiceExt[]) collection.getServices().toArray(new ServiceExt[0]);
        // Check for non-null array
        assertNotNull("Array should not be null using valid data", services);
        // Makes sure that array has 20 elements
        assertEquals("Array length does not match expected value", 20, services.length);
        // See if the cache went to get the object
        assertEquals("Creation count is incorrect", 1,
                csidb.cannedGetHandleCreateCount(services[11].getServiceHandle()));
    }

    /**
     * Tests <code>getRatingDimension()</code> for proper retrieval, caching,
     * and exception throwing using known test data in the
     * <code>CannedSIDatabase</code>.
     */
    public void testGetRatingDimension()
    {
        // Using known test data
        String name = "MMPA";
        RatingDimensionExt dimension = null;

        try
        {
            // Try to get our RatingDimension object
            dimension = (RatingDimensionExt) sic.getRatingDimension(name, language);
        }
        catch (SIException e)
        {
            fail("SIException thrown using known good data: " + e.getMessage());
        }
        // Make sure the reference is not null
        assertNotNull("Reference should not be null using valid data", dimension);
        // Check to make sure we got the right RatingDimension object
        assertEquals("RatingDimension name does not match expected value", name, dimension.getDimensionName());
        assertEquals("Number of levels does not match expected value", 3, dimension.getNumberOfLevels());
        // See if the cache went to get the object
        assertEquals("Creation count is incorrect", 1,
                csidb.cannedGetHandleCreateCount(dimension.getRatingDimensionHandle()));

        // Same test as above, but with a null language
        try
        {
            // Try to get our RatingDimension object
            dimension = (RatingDimensionExt) sic.getRatingDimension(name, "");
        }
        catch (SIException e)
        {
            fail("SIException thrown using known good data: " + e.getMessage());
        }
        // Check to make sure we got the right RatingDimension object
        assertEquals("RatingDimension name does not match expected value", name, dimension.getDimensionName());
        assertEquals("Number of levels does not match expected value", 3, dimension.getNumberOfLevels());
        // See if the cache went to get the object
        assertEquals("Creation count is incorrect", 1,
                csidb.cannedGetHandleCreateCount(dimension.getRatingDimensionHandle()));

        // Now we'll try to throw an exception using bad data
        try
        {
            dimension = (RatingDimensionExt) sic.getRatingDimension("foo", "");
            fail("Exception was not thrown properly using bad data");
        }
        catch (SIException e)
        {
            // We wanted this to happen, so do nothing
        }
    }

    /**
     * Tests <code>getSupportedDimensions()</code> for proper data retrieval.
     */
    public void testGetSupportedDimensions() throws Exception
    {
        // Get our array of Strings
        String[] dimensions = sic.getSupportedDimensions(language);
        // Make sure array is not null
        assertNotNull("Array should not be null using valid test data", dimensions);
        // Check length of array
        assertEquals("Length of array does not match expected value", 1, dimensions.length);
        // See if the cache went to get the object
        RatingDimensionHandle handle = csidb.getRatingDimensionByName(dimensions[0]);
        assertEquals("Creation count is incorrect", 1, csidb.cannedGetHandleCreateCount(handle));
        // Get our array of Strings
        dimensions = sic.getSupportedDimensions("eng");
        // Check length of array
        assertEquals("Length of array does not match expected value", 1, dimensions.length);
        assertEquals("Creation count is incorrect", 1, csidb.cannedGetHandleCreateCount(handle));
    }

    /**
     * Tests <code>getTransports()</code>
     */
    public void testGetTransports()
    {
        // Get our array of Transport objects
        TransportExt[] transports = (TransportExt[]) sic.getTransports();
        // Make sure array is not null
        assertNotNull("Array should not be null using valid test data", transports);
        // Check the length of the array
        assertEquals("Length of array does not match expected value", 2, transports.length);
        // See if the cache went to get the object
        assertEquals("Creation count is incorrect", 1,
                csidb.cannedGetHandleCreateCount(transports[0].getTransportHandle()));
        // Try again to make sure it cached
        transports = (TransportExt[]) sic.getTransports();
        // Make sure array is not null
        assertNotNull("Array should not be null using valid test data", transports);
        // Check the length of the array
        assertEquals("Length of array does not match expected value", 2, transports.length);
        // See if the cache went to get the object
        assertEquals("Creation count is incorrect", 1,
                csidb.cannedGetHandleCreateCount(transports[0].getTransportHandle()));

        // Same test as above, but try a different language
        transports = (TransportExt[]) sic.getTransports();
        // Make sure array is not null
        assertNotNull("Array should not be null using valid test data", transports);
        // Check the length of the array
        assertEquals("Length of array does not match expected value", 2, transports.length);
        // See if the cache went to get the object
        assertEquals("Creation count is incorrect", 1,
                csidb.cannedGetHandleCreateCount(transports[0].getTransportHandle()));
    }

    // /////////////////////////////////////////////////////////////////////////
    // testRetrieveNetwork section
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Tests <code>retrieveNetwork</code>s ability to properly cache retrieved
     * values.
     */
    public void testRetrieveNetworkCheckCache() throws Exception
    {
        // Create an array of arguments to the method
        Object[] retNetArgs = { new NetworkLocator(csidb.transport1.getTransportID(), 1), requestor };
        // Get set of expected values
        Set s = createSet(new Object[] { csidb.network3 });
        // Call helper method using created values
        checkCache(retrieveNetworkMethod, retNetArgs, s, NetworkExt.class);

        requestor.reset();

        // Create an array of arguments to the method
        retNetArgs = new Object[] { new NetworkLocator(csidb.transport1.getTransportID(), 1), requestor };
        // Get set of expected values
        s = createSet(new Object[] { csidb.network3 });
        // Call helper method using created values
        checkCache(retrieveNetworkMethod, retNetArgs, s, NetworkExt.class);

        // Reset the requestor
        requestor.reset();
        // Set up a new array corresponding to a different network
        retNetArgs = new Object[] { new NetworkLocator(csidb.transport1.getTransportID(), 2), requestor };
        // Get a new set with the changed data
        s = createSet(new Object[] { csidb.network4 });
        // Call helper method again using new values
        checkCache(retrieveNetworkMethod, retNetArgs, s, NetworkExt.class);
    }

    /**
     * Tests <code>retrieveNetwork</code>s ability to handle data not yet
     * available notifications and that it properly notifies the requestor when
     * it becomes available.
     */
    public void testRetrieveNetworkCheckUnavailable() throws Exception
    {
        // Create array of arguments to pass to the method
        Object[] retNetArgs = { new NetworkLocator(csidb.transport1.getTransportID(), 1), requestor };
        // Call helper method using created values
        checkUnavailable(retrieveNetworkMethod, retNetArgs);

        // Reset the requestor
        requestor.reset();
        // Set up a new array corresponding to a different network
        retNetArgs = new Object[] { new NetworkLocator(csidb.transport1.getTransportID(), 2), requestor };
        // Call helper method again using new values
        checkUnavailable(retrieveNetworkMethod, retNetArgs);
    }

    /**
     * Tests <code>retrieveNetwork</code>s ability to handle data not yet
     * available notifications and that it properly notifies the requestor when
     * it becomes available.
     */
    public void testRetrieveNetworkCheckNotYetAvailable() throws Exception
    {
        // Create array of arguments to pass to the method
        Object[] retNetArgs = { new NetworkLocator(csidb.transport1.getTransportID(), 1), requestor };
        // SIAcquiredEvent to pass to the database for event firing
        SIChangeType changeType = SIChangeType.ADD;
        NetworksChangedEvent event = new NetworksChangedEvent(csidb, changeType, csidb.transport1.getTransportHandle());
        // Get set of the expected values
        Set s = createSet(new Object[] { csidb.network3 });
        // Call helper method using the created values
        checkNotYetAvailable(retrieveNetworkMethod, retNetArgs, s, NetworkExt.class, event);

        // Reset the requestor
        requestor.reset();
        // Set up a new array corresponding to a different network
        retNetArgs = new Object[] { new NetworkLocator(csidb.transport1.getTransportID(), 2), requestor };
        // Get a new set with the changed data
        s = createSet(new Object[] { csidb.network4 });
        // Call helper method again using the new values
        checkNotYetAvailable(retrieveNetworkMethod, retNetArgs, s, NetworkExt.class, event);
    }

    /**
     * Tests <code>retrieveNetwork</code> to make sure it properly handles
     * cancellations of si retrieval requests.
     */
    public void testRetrieveNetworkCheckCancel() throws Exception
    {
        // Create array of arguments to pass to the method
        Object[] retNetArgs = { new NetworkLocator(csidb.transport1.getTransportID(), 1), requestor };
        // SIAcquiredEvent to pass to the database for event firing
        SIChangeType changeType = SIChangeType.ADD; // FIX!!
        NetworksChangedEvent event = new NetworksChangedEvent(csidb, changeType, csidb.transport1.getTransportHandle());
        // Call the delegate method using the created values
        checkCancel(retrieveNetworkMethod, retNetArgs, event);

        // Reset the requestor
        requestor.reset();
        // Set up a new array corresponding to a different network
        retNetArgs = new Object[] { new NetworkLocator(csidb.transport1.getTransportID(), 2), requestor };
        // Call the delegate method again using the new values
        checkCancel(retrieveNetworkMethod, retNetArgs, event);
    }

    /**
     * Tests <code>retrieveNetwork</code> to make sure it properly times out and
     * notifies the requestor accordingly.
     */
    public void testRetrieveNetworkCheckTimeOut() throws Exception
    {
        // Create array of arguments to pass to the method
        Object[] retNetArgs = { new NetworkLocator(csidb.transport1.getTransportID(), 1), requestor };
        // Call helper method using created values
        checkTimeOut(retrieveNetworkMethod, retNetArgs);

        // Reset the requestor
        requestor.reset();
        // Set up a new array corresponding to a different network
        retNetArgs = new Object[] { new NetworkLocator(csidb.transport1.getTransportID(), 2), requestor };
        // Call the delegate method again using the new values
        checkTimeOut(retrieveNetworkMethod, retNetArgs);
    }

    // /////////////////////////////////////////////////////////////////////////
    // testRetrieveNetworks section
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Tests <code>retrieveNetworks</code>' ability to properly cache retrieved
     * values.
     */
    public void testRetrieveNetworksCheckCache() throws Exception
    {
        // Create array of arguments to pass to the method
        Object[] retNetArgs = { csidb.transport1, requestor };
        // Get a set of the expected objects.
        Set s = createSet(new Object[] { csidb.network3, csidb.network4, csidb.network5 });
        // Call helper method using created values
        checkCache(retrieveNetworksMethod, retNetArgs, s, NetworkExt.class);

        requestor.reset();

        // Create array of arguments to pass to the method
        retNetArgs = new Object[] { csidb.transport1, requestor };
        // Get a set of the expected objects.
        s = createSet(new Object[] { csidb.network3, csidb.network4, csidb.network5 });
        // Call helper method using created values
        checkCache(retrieveNetworksMethod, retNetArgs, s, NetworkExt.class);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of networks
        retNetArgs = new Object[] { csidb.transport2, requestor };
        // Get a new set
        s = createSet(new Object[] { csidb.network6 });
        // Call helper method again using new values
        checkCache(retrieveNetworksMethod, retNetArgs, s, NetworkExt.class);
    }

    /**
     * Tests <code>retrieveNetworks</code>' ability to handle data not yet
     * available notifications and that it properly notifies the requestor when
     * it becomes available.
     */
    public void testRetrieveNetworksCheckUnavailable() throws Exception
    {
        // Create array of arguments to pass to the method
        Object[] retNetArgs = { csidb.transport1, requestor };
        // Call helper method using created values
        checkUnavailable(retrieveNetworksMethod, retNetArgs);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of networks
        retNetArgs = new Object[] { csidb.transport2, requestor };
        // Call helper method again using new values
        checkUnavailable(retrieveNetworksMethod, retNetArgs);
    }

    /**
     * Tests <code>retrieveNetworks</code>' ability to handle data not yet
     * available notifications and that it properly notifies the requestor when
     * it becomes available.
     */
    public void testRetrieveNetworksCheckNotYetAvailable() throws Exception
    {
        // Create array of arguments to pass to the method
        Object[] retNetArgs = { csidb.transport1, requestor };
        // SIAcquiredEvent to pass to the database for event firing
        SIChangeType changeType = SIChangeType.ADD;
        NetworksChangedEvent event = new NetworksChangedEvent(csidb, changeType, csidb.transport1.getTransportHandle());
        // Get a set of the expected objects.
        Set s = createSet(new Object[] { csidb.network3, csidb.network4, csidb.network5 });
        // Call delegate method using created values
        checkNotYetAvailable(retrieveNetworksMethod, retNetArgs, s, NetworkExt.class, event);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of networks
        retNetArgs = new Object[] { csidb.transport2, requestor };
        // Create a new event since the transport is different
        event = new NetworksChangedEvent(csidb, changeType, csidb.transport2.getTransportHandle());
        // Get a new set
        s = createSet(new Object[] { csidb.network6 });
        // Call delegate method again using new values
        checkNotYetAvailable(retrieveNetworksMethod, retNetArgs, s, NetworkExt.class, event);
    }

    /**
     * Tests <code>retrieveNetworks</code> to make sure it properly handles
     * cancellations of si retrieval requests.
     */
    public void testRetrieveNetworksCheckCancel() throws Exception
    {
        // Create array of arguments to pass to the method
        Object[] retNetArgs = { csidb.transport1, requestor };
        // SIAcquiredEvent to pass to the database for event firing
        SIChangeType changeType = SIChangeType.ADD;
        NetworksChangedEvent event = new NetworksChangedEvent(csidb, changeType, csidb.transport1.getTransportHandle());
        // Call helper method using created values
        checkCancel(retrieveNetworksMethod, retNetArgs, event);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of networks
        retNetArgs = new Object[] { csidb.transport2, requestor };
        // Create a new event since the transport is different
        event = new NetworksChangedEvent(csidb, changeType, csidb.transport2.getTransportHandle());
        // Call helper method again using new values
        checkCancel(retrieveNetworksMethod, retNetArgs, event);
    }

    /**
     * Tests <code>retrieveNetworks</code> to make sure it properly times out
     * and notifies the requestor accordingly.
     */
    public void testRetrieveNetworksCheckTimeOut() throws Exception
    {
        // Create array of arguments to pass to the method
        Object[] retNetArgs = { csidb.transport1, requestor };
        // Call helper method using created values
        checkTimeOut(retrieveNetworksMethod, retNetArgs);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of networks
        retNetArgs = new Object[] { csidb.transport2, requestor };
        // Call helper method again using new values
        checkTimeOut(retrieveNetworksMethod, retNetArgs);
    }

    // /////////////////////////////////////////////////////////////////////////
    // testRetrieveServiceComponents section
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Tests <code>retrieveServiceComponents</code>' ability to properly cache
     * retrieved values.
     */
    public void testRetrieveServiceComponentsCheckCache() throws Exception
    {
        // Create array of method arguments
        Object[] retSCompArgs = { csidb.serviceDetails33, "eng", requestor };
        // Get a set of the expected objects.
        Set s = createSet(new Object[] { csidb.serviceComponent69, csidb.serviceComponent69eng,
                csidb.serviceComponent69fre, csidb.serviceComponent69spa, csidb.serviceComponent105 });
        // Call the helper method using the created values
        tune(csidb.transportStream7);
        checkCache(retrieveServiceComponentsMethod, retSCompArgs, s, ServiceComponentExt.class);

        requestor.reset();

        // Create array of method arguments
        retSCompArgs = new Object[] { csidb.serviceDetails33, "eng", requestor };
        // Get a set of the expected objects.
        s = createSet(new Object[] { csidb.serviceComponent69, csidb.serviceComponent69eng,
                csidb.serviceComponent69fre, csidb.serviceComponent69spa, csidb.serviceComponent105 });
        // Call the helper method using the created values
        checkCache(retrieveServiceComponentsMethod, retSCompArgs, s, ServiceComponentExt.class);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of servicecomponents
        retSCompArgs = new Object[] { csidb.serviceDetails50, "eng", requestor };
        // Get a new set
        s = createSet(new Object[] { csidb.serviceComponent86, csidb.serviceComponent122 });
        // Call the helper method again using the new values
        tune(csidb.transportStream8);
        checkCache(retrieveServiceComponentsMethod, retSCompArgs, s, ServiceComponentExt.class);
    }

    /**
     * Tests <code>retrieveServiceComponents</code>' ability to handle data not
     * yet available notifications and that it properly notifies the requestor
     * when it becomes available.
     */
    public void testRetrieveServiceComponentsCheckUnavailable() throws Exception
    {
        // Create array of method arguments
        Object[] retSCompArgs = { csidb.serviceDetails33, "eng", requestor };
        // Call helper method using created values
        checkUnavailable(retrieveServiceComponentsMethod, retSCompArgs);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of servicecomponents
        retSCompArgs = new Object[] { csidb.serviceDetails50, "eng", requestor };
        // Call helper method again using new values
        checkUnavailable(retrieveServiceComponentsMethod, retSCompArgs);
    }

    /**
     * Tests <code>retrieveServiceComponents</code>' ability to handle data not
     * yet available notifications and that it properly notifies the requestor
     * when it becomes available.
     */
    public void testRetrieveServiceComponentsCheckNotYetAvailable() throws Exception
    {
        // Create array of method arguments
        Object[] retSCompArgs = { csidb.serviceDetails33, "eng", requestor };
        // Create an event for the database to fire
        SIChangeType changeType = SIChangeType.ADD;
        ServiceDetailsChangedEvent event = new ServiceDetailsChangedEvent(csidb, changeType,
                csidb.transport1.getTransportHandle());
        // Get a set of the expected objects.
        Set s = createSet(new Object[] { csidb.serviceComponent69, csidb.serviceComponent69eng,
                csidb.serviceComponent69fre, csidb.serviceComponent69spa, csidb.serviceComponent105 });
        // Call helper method using created values
        tune(csidb.transportStream7);
        checkNotYetAvailable(retrieveServiceComponentsMethod, retSCompArgs, s, ServiceComponentExt.class, event);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of servicecomponents
        retSCompArgs = new Object[] { csidb.serviceDetails50, "eng", requestor };
        // Get a new set
        s = createSet(new Object[] { csidb.serviceComponent86, csidb.serviceComponent122 });
        // Call helper method again using new values
        tune(csidb.transportStream8);
        checkNotYetAvailable(retrieveServiceComponentsMethod, retSCompArgs, s, ServiceComponentExt.class, event);
    }

    /**
     * Tests <code>retrieveServiceComponents</code> to make sure it properly
     * handles cancellations of si retrieval requests.
     */
    public void testRetrieveServiceComponentsCheckCancel() throws Exception
    {
        // Create array of method arguments
        Object[] retSCompArgs = { csidb.serviceDetails33, "eng", requestor };
        // Create an event for the database to fire
        SIChangeType changeType = SIChangeType.ADD;
        ServiceDetailsChangedEvent event = new ServiceDetailsChangedEvent(csidb, changeType,
                csidb.transport1.getTransportHandle());
        // Call helper method using created values
        checkCancel(retrieveServiceComponentsMethod, retSCompArgs, event);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of servicecomponents
        retSCompArgs = new Object[] { csidb.serviceDetails50, "eng", requestor };
        // Call helper method again using new values
        checkCancel(retrieveServiceComponentsMethod, retSCompArgs, event);
    }

    /**
     * Tests <code>retrieveServiceComponents</code> to make sure it properly
     * times out and notifies the requestor accordingly.
     */
    public void testRetrieveServiceComponentsCheckTimeOut() throws Exception
    {
        // Create array of method arguments
        Object[] retSCompArgs = { csidb.serviceDetails33, "eng", requestor };
        // Call helper method using created values
        checkTimeOut(retrieveServiceComponentsMethod, retSCompArgs);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of servicecomponents
        retSCompArgs = new Object[] { csidb.serviceDetails50, "eng", requestor };
        // Call helper method again using new values
        checkTimeOut(retrieveServiceComponentsMethod, retSCompArgs);
    }

    // /////////////////////////////////////////////////////////////////////////
    // testRetrieveServiceDescription section
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Tests <code>retrieveServiceDescription</code>s ability to properly cache
     * retrieved values.
     */
    public void testRetrieveServiceDescriptionCheckCache() throws Exception
    {
        // Create array of parameters to the method
        Object[] retSDescArgs = { csidb.serviceDetails33, "eng", requestor };
        // Get a set of the expected objects.
        Set s = createSet(new Object[] { csidb.serviceDescription33 });
        // Call helper method using created values
        checkCache(retrieveServiceDescriptionMethod, retSDescArgs, s, ServiceDescriptionExt.class);

        requestor.reset();

        // Create array of parameters to the method
        retSDescArgs = new Object[] { csidb.serviceDetails33, "eng", requestor };
        // Get a set of the expected objects.
        s = createSet(new Object[] { csidb.serviceDescription33 });
        // Call helper method using created values
        checkCache(retrieveServiceDescriptionMethod, retSDescArgs, s, ServiceDescriptionExt.class);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different servicedescription
        retSDescArgs = new Object[] { csidb.serviceDetails50, "eng", requestor };
        // Get a new set
        s = createSet(new Object[] { csidb.serviceDescription50 });
        // Call helper method again using new values
        checkCache(retrieveServiceDescriptionMethod, retSDescArgs, s, ServiceDescriptionExt.class);
    }

    /**
     * Tests <code>retrieveServiceDescription</code>s functionality to make sure
     * it properly notifies request objects of data unavailable failures.
     */
    public void testRetrieveServiceDescriptionCheckUnavailable() throws Exception
    {
        // Create array of parameters to the method
        Object[] retSDescArgs = { csidb.serviceDetails33, "eng", requestor };
        // Call helper method using created values
        checkUnavailable(retrieveServiceDescriptionMethod, retSDescArgs);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different servicedescription
        retSDescArgs = new Object[] { csidb.serviceDetails50, "eng", requestor };
        // Call helper method again using new values
        checkUnavailable(retrieveServiceDescriptionMethod, retSDescArgs);
    }

    /**
     * Tests <code>retrieveServiceDescription</code> to make sure it properly
     * handles cancellations of si retrieval requests.
     */
    public void XXXtestRetrieveServiceDescriptionCheckCancel() throws Exception
    {
        // TODO (Josh) Figure out if this can even be tested, since the
        // ServiceDescription is available as soon as the ServiceDetails is,
        // leaving it nearly impossible to cancel the request.

        // Create array of parameters to the method
        Object[] retSDescArgs = { csidb.serviceDetails33, "eng", requestor };
        SIChangeType changeType = SIChangeType.ADD;
        // Create event for the database to fire
        ServiceDetailsChangedEvent event = new ServiceDetailsChangedEvent(csidb, changeType,
                csidb.transport1.getTransportHandle());
        // Call helper method using created values
        checkCancel(retrieveServiceDescriptionMethod, retSDescArgs, event);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different servicedescription
        retSDescArgs = new Object[] { csidb.serviceDetails50, "eng", requestor };
        // Call helper method again using new values
        checkCancel(retrieveServiceDescriptionMethod, retSDescArgs, event);
    }

    /**
     * Tests <code>retrieveServiceDescription</code> to make sure it properly
     * times out and notifies the requestor accordingly.
     */
    public void XXXtestRetrieveServiceDescriptionCheckTimeOut() throws Exception
    {
        // Create array of parameters to the method
        Object[] retSDescArgs = { csidb.serviceDetails33, "eng", requestor };
        // Call helper method using created values
        checkTimeOut(retrieveServiceDescriptionMethod, retSDescArgs);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different servicedescription
        retSDescArgs = new Object[] { csidb.serviceDetails50, "eng", requestor };
        // Call helper method again using new values
        checkTimeOut(retrieveServiceDescriptionMethod, retSDescArgs);
    }

    // /////////////////////////////////////////////////////////////////////////
    // testRetrieveServiceDetails section
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Tests <code>retrieveServiceDetails</code>' ability to properly cache
     * retrieved values.
     */
    public void testRetrieveServiceDetailsCheckCache() throws Exception
    {
        Object[] retSDArgs = { csidb.service15, null, Boolean.TRUE, requestor };
        Set s = createSet(new Object[] { csidb.serviceDetails33 });
        checkCache(retrieveServiceDetailsMethod, retSDArgs, s, ServiceDetailsExt.class);

        requestor.reset();

        retSDArgs = new Object[] { csidb.service15, null, Boolean.TRUE, requestor };
        s = createSet(new Object[] { csidb.serviceDetails33 });
        checkCache(retrieveServiceDetailsMethod, retSDArgs, s, ServiceDetailsExt.class);

        // Reset the requestor
        requestor.reset();
        // Set up a new array corresponding to a different service details
        retSDArgs = new Object[] { csidb.service18, null, Boolean.TRUE, requestor };
        // Get a new set with the changed data
        s = createSet(new Object[] { csidb.serviceDetails36 });
        checkCache(retrieveServiceDetailsMethod, retSDArgs, s, ServiceDetailsExt.class);
    }

    /**
     * Tests <code>retrieveServiceDetails</code>' functionality to make sure it
     * properly notifies request objects of data unavailable failures.
     */
    public void testRetrieveServiceDetailsCheckUnavailable() throws Exception
    {
        Object[] retSDArgs = { csidb.service15, null, Boolean.TRUE, requestor };
        checkUnavailable(retrieveServiceDetailsMethod, retSDArgs);

        // Reset the requestor
        requestor.reset();
        // Set up a new array corresponding to a different service details
        retSDArgs = new Object[] { csidb.service18, null, Boolean.TRUE, requestor };
        checkUnavailable(retrieveServiceDetailsMethod, retSDArgs);
    }

    /**
     * Tests <code>retrieveServiceDetails</code>' ability to handle data not yet
     * available notifications and that it properly notifies the requestor when
     * it becomes available.
     */
    public void testRetrieveServiceDetailsCheckNotYetAvailable() throws Exception
    {
        Object[] retSDArgs = { csidb.service15, null, Boolean.TRUE, requestor };
        SIChangeType changeType = SIChangeType.ADD;
        ServiceDetailsChangedEvent event = new ServiceDetailsChangedEvent(csidb, changeType,
                csidb.transport1.getTransportHandle());
        // Create a set of the objects we expect
        Set s = createSet(new Object[] { csidb.serviceDetails33 });
        checkNotYetAvailable(retrieveServiceDetailsMethod, retSDArgs, s, ServiceDetailsExt.class, event);

        // Reset the requestor
        requestor.reset();
        // Set up a new array corresponding to a different service details
        retSDArgs = new Object[] { csidb.service18, null, Boolean.TRUE, requestor };
        // Get a new set with the changed data
        s = createSet(new Object[] { csidb.serviceDetails36 });
        checkNotYetAvailable(retrieveServiceDetailsMethod, retSDArgs, s, ServiceDetailsExt.class, event);
    }

    /**
     * Tests <code>retrieveServiceDetails</code> to make sure it properly
     * handles cancellations of si retrieval requests.
     */
    public void testRetrieveServiceDetailsCheckCancel() throws Exception
    {
        Object[] retSDArgs = { csidb.service15, null, Boolean.TRUE, requestor };
        SIChangeType changeType = SIChangeType.ADD;
        ServiceDetailsChangedEvent event = new ServiceDetailsChangedEvent(csidb, changeType,
                csidb.transport1.getTransportHandle());
        checkCancel(retrieveServiceDetailsMethod, retSDArgs, event);

        // Reset the requestor
        requestor.reset();
        // Set up a new array corresponding to a different service details
        retSDArgs = new Object[] { csidb.service18, null, Boolean.TRUE, requestor };
        checkCancel(retrieveServiceDetailsMethod, retSDArgs, event);
    }

    /**
     * Tests <code>retrieveServiceDetails</code> to make sure it properly times
     * out and notifies the requestor accordingly.
     */
    public void testRetrieveServiceDetailsCheckTimeOut() throws Exception
    {
        Object[] retSDArgs = { csidb.service15, null, Boolean.TRUE, requestor };
        checkTimeOut(retrieveServiceDetailsMethod, retSDArgs);

        // Reset the requestor
        requestor.reset();
        // Set up a new array corresponding to a different service details
        retSDArgs = new Object[] { csidb.service18, null, Boolean.TRUE, requestor };
        checkTimeOut(retrieveServiceDetailsMethod, retSDArgs);
    }

    // /////////////////////////////////////////////////////////////////////////
    // testRetrieveSIElement section
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Tests <code>retrieveSIElement</code>' ability to properly cache retrieved
     * values.
     */
    public void testRetrieveSIElementCheckCache() throws Exception
    {

        // /////////////////////////////////////////////////////////////////////
        // First test will be for retrieval of two networks
        // /////////////////////////////////////////////////////////////////////
        // Part 1
        Object[] retSIArgs = { new NetworkLocator(csidb.transport1.getTransportID(), 1), "eng", requestor };
        Set s = createSet(new Object[] { csidb.network3 });
        checkCache(retrieveSIElementMethod, retSIArgs, s, NetworkExt.class);

        requestor.reset();

        retSIArgs = new Object[] { new NetworkLocator(csidb.transport1.getTransportID(), 1), "fre", requestor };
        s = createSet(new Object[] { csidb.network3 });
        checkCache(retrieveSIElementMethod, retSIArgs, s, NetworkExt.class);

        requestor.reset();

        // Part 2
        retSIArgs = new Object[] { new NetworkLocator(csidb.transport1.getTransportID(), 3), "eng", requestor };
        s = createSet(new Object[] { csidb.network5 });
        checkCache(retrieveSIElementMethod, retSIArgs, s, NetworkExt.class);

        // /////////////////////////////////////////////////////////////////////
        // Second test will be for retrieval of two transport streams
        // /////////////////////////////////////////////////////////////////////
        // Part 1
        requestor.reset();
        retSIArgs = new Object[] {
        // new TransportStreamLocator(csidb.transport1.getTransportID(), 5000,
        // 1),
                new OcapLocator(5000, 1), "eng", requestor };
        s = createSet(new Object[] { csidb.transportStream7 });
        checkCache(retrieveSIElementMethod, retSIArgs, s, TransportStreamExt.class);

        requestor.reset();

        retSIArgs = new Object[] { new OcapLocator(5000, 1), "fre", requestor };
        s = createSet(new Object[] { csidb.transportStream7 });
        checkCache(retrieveSIElementMethod, retSIArgs, s, TransportStreamExt.class);

        requestor.reset();

        // Part 2
        retSIArgs = new Object[] { new OcapLocator(5750, 4), "eng", requestor };
        s = createSet(new Object[] { csidb.transportStream10 });
        checkCache(retrieveSIElementMethod, retSIArgs, s, TransportStreamExt.class);

        // /////////////////////////////////////////////////////////////////////
        // Third test will be for retrieval of two service details
        // /////////////////////////////////////////////////////////////////////
        // Part 1
        requestor.reset();
        retSIArgs = new Object[] { csidb.l100, "eng", requestor };
        s = createSet(new Object[] { csidb.serviceDetails33 });
        checkCache(retrieveSIElementMethod, retSIArgs, s, ServiceDetailsExt.class);

        requestor.reset();

        retSIArgs = new Object[] { csidb.l100, "fre", requestor };
        s = createSet(new Object[] { csidb.serviceDetails33 });
        checkCache(retrieveSIElementMethod, retSIArgs, s, ServiceDetailsExt.class);

        requestor.reset();

        // Part 2
        retSIArgs = new Object[] { csidb.l103, "eng", requestor };
        s = createSet(new Object[] { csidb.serviceDetails36 });
        checkCache(retrieveSIElementMethod, retSIArgs, s, ServiceDetailsExt.class);

        // /////////////////////////////////////////////////////////////////////
        // Fourth test will be for retrieval of two service components
        // /////////////////////////////////////////////////////////////////////
        // Part 1
        requestor.reset();
        retSIArgs = new Object[] {
                new OcapLocator(csidb.service15.getName(), new String[] { csidb.serviceComponent69.getName() }, -1,
                        null), "eng", requestor };
        s = createSet(new Object[] { csidb.serviceComponent69 });
        tune(csidb.transportStream7);
        checkCache(retrieveSIElementMethod, retSIArgs, s, ServiceComponentExt.class);

        requestor.reset();

        retSIArgs = new Object[] {
                new OcapLocator(csidb.service15.getName(), new String[] { csidb.serviceComponent69.getName(),
                        csidb.serviceComponent105.getName() }, -1, null), "fre", requestor };
        s = createSet(new Object[] { csidb.serviceComponent69, csidb.serviceComponent105 });
        checkCache(retrieveSIElementMethod, retSIArgs, s, ServiceComponentExt.class);

        requestor.reset();

        // Part 2
        retSIArgs = new Object[] {
                new OcapLocator(csidb.service16.getName(), new String[] { csidb.serviceComponent70.getName() }, -1,
                        null), "eng", requestor };
        s = createSet(new Object[] { csidb.serviceComponent70 });
        tune(csidb.transportStream8);
        checkCache(retrieveSIElementMethod, retSIArgs, s, ServiceComponentExt.class);

        // Amended.. now retrieves servicecomponents by using a different form
        // of the locator.
        requestor.reset();
        // Tag
        retSIArgs = new Object[] {
                new OcapLocator(((OcapLocator) csidb.service16.getLocator()).getSourceID(), -1,
                        new int[] { csidb.serviceComponent70.getComponentTag() }, null), "eng", requestor };
        s = createSet(new Object[] { csidb.serviceComponent70 });
        checkCache(retrieveSIElementMethod, retSIArgs, s, ServiceComponentExt.class);

        // streamType
        requestor.reset();
        retSIArgs = new Object[] {
                new OcapLocator(((OcapLocator) csidb.service16.getLocator()).getSourceID(),
                        new short[] { csidb.serviceComponent106.getElementaryStreamType() },
                        new String[] { csidb.serviceComponent106.getAssociatedLanguage() }, -1, null), "", requestor };
        s = createSet(new Object[] { csidb.serviceComponent106 });
        checkCache(retrieveSIElementMethod, retSIArgs, s, ServiceComponentExt.class);

        // index
        requestor.reset();
        retSIArgs = new Object[] {
                new OcapLocator(((OcapLocator) csidb.service16.getLocator()).getSourceID(),
                        new short[] { csidb.serviceComponent106.getElementaryStreamType() }, new int[] { 1 }, -1, null),
                "eng", requestor };
        s = createSet(new Object[] { csidb.serviceComponent106 });
        checkCache(retrieveSIElementMethod, retSIArgs, s, ServiceComponentExt.class);

        // pid
        requestor.reset();
        retSIArgs = new Object[] {
                new OcapLocator(((OcapLocator) csidb.service16.getLocator()).getSourceID(),
                        new int[] { csidb.serviceComponent106.getPID() }, -1, null), "eng", requestor };
        s = createSet(new Object[] { csidb.serviceComponent106 });
        checkCache(retrieveSIElementMethod, retSIArgs, s, ServiceComponentExt.class);
    }

    /**
     * Tests <code>retrieveSIElement</code>' functionality to make sure it
     * properly notifies request objects of data unavailable failures.
     */
    public void testRetrieveSIElementCheckUnavailable() throws Exception
    {

        // /////////////////////////////////////////////////////////////////////
        // First test will be for retrieval of two networks
        // /////////////////////////////////////////////////////////////////////
        // Part 1
        Object[] retSIArgs = { new NetworkLocator(csidb.transport1.getTransportID(), 1), "eng", requestor };
        checkUnavailable(retrieveSIElementMethod, retSIArgs);

        // Part 2
        retSIArgs = new Object[] { new NetworkLocator(csidb.transport1.getTransportID(), 3), "eng", requestor };
        checkUnavailable(retrieveSIElementMethod, retSIArgs);

        // /////////////////////////////////////////////////////////////////////
        // Second test will be for retrieval of two transport streams
        // /////////////////////////////////////////////////////////////////////
        // Part 1
        retSIArgs = new Object[] { new OcapLocator(5000, 1), "eng", requestor };
        checkUnavailable(retrieveSIElementMethod, retSIArgs);

        // Part 2
        retSIArgs = new Object[] { new OcapLocator(5750, 4), "eng", requestor };
        checkUnavailable(retrieveSIElementMethod, retSIArgs);

        // /////////////////////////////////////////////////////////////////////
        // Third test will be for retrieval of two service details
        // /////////////////////////////////////////////////////////////////////
        // Part 1
        retSIArgs = new Object[] { csidb.l100, "eng", requestor };
        checkUnavailable(retrieveSIElementMethod, retSIArgs);

        // Part 2
        retSIArgs = new Object[] { csidb.l103, "eng", requestor };
        checkUnavailable(retrieveSIElementMethod, retSIArgs);

        // /////////////////////////////////////////////////////////////////////
        // Fourth test will be for retrieval of two service components
        // /////////////////////////////////////////////////////////////////////
        // Part 1
        retSIArgs = new Object[] {
                new OcapLocator(csidb.service15.getName(), new String[] { csidb.serviceComponent69.getName() }, -1,
                        null), "eng", requestor };
        checkUnavailable(retrieveSIElementMethod, retSIArgs);

        // Part 2
        retSIArgs = new Object[] {
                new OcapLocator(csidb.service16.getName(), new String[] { csidb.serviceComponent70.getName() }, -1,
                        null), "eng", requestor };
        checkUnavailable(retrieveSIElementMethod, retSIArgs);
    }

    /**
     * Tests <code>retrieveSIElement</code>' ability to handle data not yet
     * available notifications and that it properly notifies the requestor when
     * it becomes available.
     */
    public void testRetrieveSIElementCheckNotYetUnavailable() throws Exception
    {

        // /////////////////////////////////////////////////////////////////////
        // First test will be for retrieval of two networks
        // /////////////////////////////////////////////////////////////////////
        // Part 1
        Object[] retSIArgs = { new NetworkLocator(csidb.transport1.getTransportID(), 1), "eng", requestor };
        SIChangeType changeType = SIChangeType.ADD;
        NetworksChangedEvent event = new NetworksChangedEvent(csidb, changeType, csidb.transport1.getTransportHandle());
        Set s = createSet(new Object[] { csidb.network3 });
        checkNotYetAvailable(retrieveSIElementMethod, retSIArgs, s, NetworkExt.class, event);

        // Part 2
        retSIArgs = new Object[] { new NetworkLocator(csidb.transport1.getTransportID(), 3), "eng", requestor };
        event = new NetworksChangedEvent(csidb, changeType, csidb.transport1.getTransportHandle());
        s = createSet(new Object[] { csidb.network5 });
        checkNotYetAvailable(retrieveSIElementMethod, retSIArgs, s, NetworkExt.class, event);

        // /////////////////////////////////////////////////////////////////////
        // Second test will be for retrieval of two transport streams
        // /////////////////////////////////////////////////////////////////////
        // Part 1
        retSIArgs = new Object[] { new OcapLocator(5000, 1), "eng", requestor };
        TransportStreamsChangedEvent event2 = new TransportStreamsChangedEvent(csidb, changeType,
                csidb.transport1.getTransportHandle());
        s = createSet(new Object[] { csidb.transportStream7 });
        checkNotYetAvailable(retrieveSIElementMethod, retSIArgs, s, TransportStreamExt.class, event2);

        // Part 2
        retSIArgs = new Object[] { new OcapLocator(5750, 4), "eng", requestor };
        event2 = new TransportStreamsChangedEvent(csidb, changeType, csidb.transport1.getTransportHandle());
        s = createSet(new Object[] { csidb.transportStream10 });
        checkNotYetAvailable(retrieveSIElementMethod, retSIArgs, s, TransportStreamExt.class, event2);

        // /////////////////////////////////////////////////////////////////////
        // Third test will be for retrieval of two service details
        // /////////////////////////////////////////////////////////////////////
        // Part 1
        retSIArgs = new Object[] { csidb.l100, "eng", requestor };
        ServiceDetailsChangedEvent event3 = new ServiceDetailsChangedEvent(csidb, changeType,
                csidb.transport1.getTransportHandle());
        s = createSet(new Object[] { csidb.serviceDetails33 });
        tune(csidb.transportStream7);
        checkNotYetAvailable(retrieveSIElementMethod, retSIArgs, s, ServiceDetailsExt.class, event3);

        // Part 2
        retSIArgs = new Object[] { csidb.l103, "eng", requestor };
        event3 = new ServiceDetailsChangedEvent(csidb, changeType, csidb.transport1.getTransportHandle());
        s = createSet(new Object[] { csidb.serviceDetails36 });
        tune(csidb.transportStream10);
        checkNotYetAvailable(retrieveSIElementMethod, retSIArgs, s, ServiceDetailsExt.class, event3);

        // /////////////////////////////////////////////////////////////////////
        // Fourth test will be for retrieval of two service components
        // /////////////////////////////////////////////////////////////////////
        // Part 1
        retSIArgs = new Object[] {
                new OcapLocator(csidb.service15.getName(), new String[] { csidb.serviceComponent69.getName() }, -1,
                        null), "eng", requestor };
        ServiceDetailsChangedEvent event4 = new ServiceDetailsChangedEvent(csidb, changeType,
                csidb.transport1.getTransportHandle());
        s = createSet(new Object[] { csidb.serviceComponent69 });
        tune(csidb.transportStream7);
        checkNotYetAvailable(retrieveSIElementMethod, retSIArgs, s, ServiceComponentExt.class, event4);

        // Part 2
        retSIArgs = new Object[] {
                new OcapLocator(csidb.service16.getName(), new String[] { csidb.serviceComponent70.getName() }, -1,
                        null), "eng", requestor };
        event4 = new ServiceDetailsChangedEvent(csidb, changeType, csidb.transport1.getTransportHandle());
        s = createSet(new Object[] { csidb.serviceComponent70 });
        tune(csidb.transportStream8);
        checkNotYetAvailable(retrieveSIElementMethod, retSIArgs, s, ServiceComponentExt.class, event4);
    }

    /**
     * Tests <code>retrieveSIElement</code> to make sure it properly handles
     * cancellations of si retrieval requests.
     */
    public void testRetrieveSIElementCheckCancel() throws Exception
    {

        // /////////////////////////////////////////////////////////////////////
        // First test will be for retrieval of two networks
        // /////////////////////////////////////////////////////////////////////
        // Part 1
        Object[] retSIArgs = { new NetworkLocator(csidb.transport1.getTransportID(), 1), "eng", requestor };
        SIChangeType changeType = SIChangeType.ADD;
        NetworksChangedEvent event = new NetworksChangedEvent(csidb, changeType, csidb.transport1.getTransportHandle());
        checkCancel(retrieveSIElementMethod, retSIArgs, event);

        // Part 2
        retSIArgs = new Object[] { new NetworkLocator(csidb.transport1.getTransportID(), 3), "eng", requestor };
        event = new NetworksChangedEvent(csidb, changeType, csidb.transport1.getTransportHandle());
        checkCancel(retrieveSIElementMethod, retSIArgs, event);

        // /////////////////////////////////////////////////////////////////////
        // Second test will be for retrieval of two transport streams
        // /////////////////////////////////////////////////////////////////////
        // Part 1
        retSIArgs = new Object[] { new OcapLocator(5000, 1), "eng", requestor };
        TransportStreamsChangedEvent event2 = new TransportStreamsChangedEvent(csidb, changeType,
                csidb.transport1.getTransportHandle());
        checkCancel(retrieveSIElementMethod, retSIArgs, event2);

        // Part 2
        retSIArgs = new Object[] { new OcapLocator(5750, 4), "eng", requestor };
        event2 = new TransportStreamsChangedEvent(csidb, changeType, csidb.transport1.getTransportHandle());
        checkCancel(retrieveSIElementMethod, retSIArgs, event2);

        // /////////////////////////////////////////////////////////////////////
        // Third test will be for retrieval of two service details
        // /////////////////////////////////////////////////////////////////////
        // Part 1
        retSIArgs = new Object[] { csidb.l100, "eng", requestor };
        ServiceDetailsChangedEvent event3 = new ServiceDetailsChangedEvent(csidb, changeType,
                csidb.transport1.getTransportHandle());
        checkCancel(retrieveSIElementMethod, retSIArgs, event3);

        // Part 2
        retSIArgs = new Object[] { csidb.l103, "eng", requestor };
        event3 = new ServiceDetailsChangedEvent(csidb, changeType, csidb.transport1.getTransportHandle());
        checkCancel(retrieveSIElementMethod, retSIArgs, event3);

        // /////////////////////////////////////////////////////////////////////
        // Fourth test will be for retrieval of two service components
        // /////////////////////////////////////////////////////////////////////
        // Part 1
        retSIArgs = new Object[] {
                new OcapLocator(csidb.service15.getName(), new String[] { csidb.serviceComponent69.getName() }, -1,
                        null), "eng", requestor };
        ServiceDetailsChangedEvent event4 = new ServiceDetailsChangedEvent(csidb, changeType,
                csidb.transport1.getTransportHandle());
        checkCancel(retrieveSIElementMethod, retSIArgs, event4);

        // Part 2
        retSIArgs = new Object[] {
                new OcapLocator(csidb.service16.getName(), new String[] { csidb.serviceComponent70.getName() }, -1,
                        null), "eng", requestor };
        event4 = new ServiceDetailsChangedEvent(csidb, changeType, csidb.transport1.getTransportHandle());
        checkCancel(retrieveSIElementMethod, retSIArgs, event4);
    }

    /**
     * Tests <code>retrieveSIElement</code> to make sure it properly times out
     * and notifies the requestor accordingly.
     */
    public void testRetrieveSIElementCheckTimeOut() throws Exception
    {

        // /////////////////////////////////////////////////////////////////////
        // First test will be for retrieval of two networks
        // /////////////////////////////////////////////////////////////////////
        // Part 1
        Object[] retSIArgs = { new NetworkLocator(csidb.transport1.getTransportID(), 1), "eng", requestor };
        checkTimeOut(retrieveSIElementMethod, retSIArgs);

        // Part 2
        retSIArgs = new Object[] { new NetworkLocator(csidb.transport1.getTransportID(), 3), "eng", requestor };
        checkTimeOut(retrieveSIElementMethod, retSIArgs);

        // /////////////////////////////////////////////////////////////////////
        // Second test will be for retrieval of two transport streams
        // /////////////////////////////////////////////////////////////////////
        // Part 1
        retSIArgs = new Object[] { new OcapLocator(5000, 1), "eng", requestor };
        checkTimeOut(retrieveSIElementMethod, retSIArgs);

        // Part 2
        retSIArgs = new Object[] { new OcapLocator(5750, 4), "eng", requestor };
        checkTimeOut(retrieveSIElementMethod, retSIArgs);

        // /////////////////////////////////////////////////////////////////////
        // Third test will be for retrieval of two service details
        // /////////////////////////////////////////////////////////////////////
        // Part 1
        retSIArgs = new Object[] { csidb.l100, "eng", requestor };
        checkTimeOut(retrieveSIElementMethod, retSIArgs);

        // Part 2
        retSIArgs = new Object[] { csidb.l103, "eng", requestor };
        checkTimeOut(retrieveSIElementMethod, retSIArgs);

        // /////////////////////////////////////////////////////////////////////
        // Fourth test will be for retrieval of two service components
        // /////////////////////////////////////////////////////////////////////
        // Part 1
        retSIArgs = new Object[] {
                new OcapLocator(csidb.service15.getName(), new String[] { csidb.serviceComponent69.getName() }, -1,
                        null), "eng", requestor };
        checkTimeOut(retrieveSIElementMethod, retSIArgs);

        // Part 2
        retSIArgs = new Object[] {
                new OcapLocator(csidb.service16.getName(), new String[] { csidb.serviceComponent70.getName() }, -1,
                        null), "eng", requestor };
        checkTimeOut(retrieveSIElementMethod, retSIArgs);
    }

    // /////////////////////////////////////////////////////////////////////////
    // testRetrieveTransportStream section
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Tests <code>retrieveTransportStream</code>s ability to properly cache
     * retrieved values.
     */
    public void testRetrieveTransportStreamCheckCache() throws Exception
    {

        // Create array of arguments to the method
        Object[] retTSArgs = { new OcapLocator(5000, 1), requestor };
        // Create a set of the expected return values
        Set s = createSet(new Object[] { csidb.transportStream7 });
        // Call on the delegate method with the created values
        checkCache(retrieveTransportStreamMethod, retTSArgs, s, TransportStreamExt.class);

        // Create array of arguments to the method
        retTSArgs = new Object[] { new OcapLocator(5000, 1), requestor };
        // Create a set of the expected return values
        s = createSet(new Object[] { csidb.transportStream7 });
        // Call on the delegate method with the created values
        checkCache(retrieveTransportStreamMethod, retTSArgs, s, TransportStreamExt.class);

        requestor.reset();

        // Reset the requestor
        requestor.reset();
        // Set up a new array corresponding to a different transportstream
        retTSArgs = new Object[] { new OcapLocator(5750, 4), requestor };
        // Get a new set with the changed data
        s = createSet(new Object[] { csidb.transportStream10 });
        // Call the delegate method again with the new values
        checkCache(retrieveTransportStreamMethod, retTSArgs, s, TransportStreamExt.class);
    }

    /**
     * Tests <code>retrieveTransportStream</code>s functionality to make sure it
     * properly notifies request objects of data unavailable failures.
     */
    public void testRetrieveTransportStreamCheckUnavailable() throws Exception
    {

        // Create an array of the arguments to the method
        Object[] retTSArgs = { new OcapLocator(5000, 1), requestor };
        // Call on our delegate testing method
        checkUnavailable(retrieveTransportStreamMethod, retTSArgs);

        // Reset the requestor
        requestor.reset();
        // Set up a new array corresponding to a different transportstream
        retTSArgs = new Object[] { new OcapLocator(5750, 4), requestor };
        // Call the delegate method again with new values
        checkUnavailable(retrieveTransportStreamMethod, retTSArgs);
    }

    /**
     * Tests <code>retrieveTransportStream</code>s ability to handle data not
     * yet available notifications and that it properly notifies the requestor
     * when it becomes available.
     */
    public void testRetrieveTransportStreamCheckNotYetAvailable() throws Exception
    {

        // Create an array of arguments for the method call
        Object[] retTSArgs = { new OcapLocator(5000, 1), requestor };
        SIChangeType changeType = SIChangeType.ADD;
        // Create the event to fire from the database
        TransportStreamsChangedEvent event = new TransportStreamsChangedEvent(csidb, changeType,
                csidb.transport1.getTransportHandle());
        // A set holding the values we are expecting
        Set s = createSet(new Object[] { csidb.transportStream7 });
        // Call our delegate method with the values we created
        checkNotYetAvailable(retrieveTransportStreamMethod, retTSArgs, s, TransportStreamExt.class, event);

        // Reset the requestor
        requestor.reset();
        // Set up a new array corresponding to a different transportstream
        retTSArgs = new Object[] { new OcapLocator(5750, 4), requestor };
        // Get a new set with the changed data
        s = createSet(new Object[] { csidb.transportStream10 });
        // Call the delegate method again with the new values
        checkNotYetAvailable(retrieveTransportStreamMethod, retTSArgs, s, TransportStreamExt.class, event);
    }

    /**
     * Tests <code>retrieveTransportStream</code> to make sure it properly
     * handles cancellations of si retrieval requests.
     */
    public void testRetrieveTransportStreamCheckCancel() throws Exception
    {

        // Create an array of the values to pass to the method
        Object[] retTSArgs = { new OcapLocator(5000, 1), requestor };
        SIChangeType changeType = SIChangeType.ADD;
        // Create an event to fire from the database
        TransportStreamsChangedEvent event = new TransportStreamsChangedEvent(csidb, changeType,
                csidb.transport1.getTransportHandle());
        // Call on the delegate method with the created values
        checkCancel(retrieveTransportStreamMethod, retTSArgs, event);

        // Reset the requestor
        requestor.reset();
        // Set up a new array corresponding to a different transportstream
        retTSArgs = new Object[] { new OcapLocator(5750, 4), requestor };
        // Call the delegate method again with new values
        checkCancel(retrieveTransportStreamMethod, retTSArgs, event);
    }

    /**
     * Tests <code>retrieveTransportStream</code> to make sure it properly times
     * out and notifies the requestor accordingly.
     */
    public void testRetrieveTransportStreamCheckTimeOut() throws Exception
    {

        // Create an array of the values to pass to the method
        Object[] retTSArgs = { new OcapLocator(5000, 1), requestor };
        // Call the delegate method with the created values
        checkTimeOut(retrieveTransportStreamMethod, retTSArgs);

        // Reset the requestor
        requestor.reset();
        // Set up a new array corresponding to a different transportstream
        retTSArgs = new Object[] { new OcapLocator(5750, 4), requestor };
        // Call the delegate method again with the new values
        checkTimeOut(retrieveTransportStreamMethod, retTSArgs);
    }

    // /////////////////////////////////////////////////////////////////////////
    // testRetrieveTransportStreamsUsingNetwork section
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Tests <code>retrieveTransportStreams</code>' ability to properly cache
     * retrieved values.
     */
    public void testRetrieveTransportStreamsUsingNetworkCheckCache() throws Exception
    {
        // Get an array of arguments to pass to the method
        Object[] retTSArgs = { csidb.network3, requestor };
        // Create a set of expected values
        Set s = createSet(new Object[] { csidb.transportStream7, csidb.transportStream8, csidb.transportStream9,
                csidb.transportStream11, csidb.transportStream12 });
        // Call the delegate method with the created values
        checkCache(retrieveTransportStreamsUsingNetworkMethod, retTSArgs, s, TransportStreamExt.class);

        requestor.reset();

        // Get an array of arguments to pass to the method
        retTSArgs = new Object[] { csidb.network3, requestor };
        // Create a set of expected values
        s = createSet(new Object[] { csidb.transportStream7, csidb.transportStream8, csidb.transportStream9,
                csidb.transportStream11, csidb.transportStream12 });
        // Call the delegate method with the created values
        checkCache(retrieveTransportStreamsUsingNetworkMethod, retTSArgs, s, TransportStreamExt.class);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different transportstream
        retTSArgs = new Object[] { csidb.network5, requestor };
        // Get a new set
        s = createSet(new Object[] { csidb.transportStream13 });
        // Call the delegate method again with new values
        checkCache(retrieveTransportStreamsUsingNetworkMethod, retTSArgs, s, TransportStreamExt.class);
    }

    /**
     * Tests <code>retrieveTransportStreams</code>' functionality to make sure
     * it properly notifies request objects of data unavailable failures.
     */
    public void testRetrieveTransportStreamsUsingNetworkCheckUnavailable() throws Exception
    {
        // Create an array of the values to pass to the method
        Object[] retTSArgs = { csidb.network3, requestor };
        // Call the helper method using the created values
        checkUnavailable(retrieveTransportStreamsUsingNetworkMethod, retTSArgs);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of networks
        retTSArgs = new Object[] { csidb.network5, requestor };
        // Call the helper method again using new values
        checkUnavailable(retrieveTransportStreamsUsingNetworkMethod, retTSArgs);
    }

    /**
     * Tests <code>retrieveTransportStreams</code>' ability to handle data not
     * yet available notifications and that it properly notifies the requestor
     * when it becomes available.
     */
    public void testRetrieveTransportStreamsUsingNetworkCheckNotYetAvailable() throws Exception
    {
        // Create array of arguments to pass to the method
        Object[] retTSArgs = { csidb.network3, requestor };
        SIChangeType changeType = SIChangeType.ADD;
        // Create an event for the database to fire
        TransportStreamsChangedEvent event = new TransportStreamsChangedEvent(csidb, changeType,
                csidb.transport1.getTransportHandle());
        // Get a set of the expected objects.
        Set s = createSet(new Object[] { csidb.transportStream7, csidb.transportStream8, csidb.transportStream9,
                csidb.transportStream11, csidb.transportStream12 });
        // Call the helper method using the values we created
        checkNotYetAvailable(retrieveTransportStreamsUsingNetworkMethod, retTSArgs, s, TransportStreamExt.class, event);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of networks
        retTSArgs = new Object[] { csidb.network5, requestor };
        // Get a new set
        s = createSet(new Object[] { csidb.transportStream13 });
        // Call the helper method again using the new values
        checkNotYetAvailable(retrieveTransportStreamsUsingNetworkMethod, retTSArgs, s, TransportStreamExt.class, event);
    }

    /**
     * Tests <code>retrieveTransportStreams</code> to make sure it properly
     * handles cancellations of si retrieval requests.
     */
    public void testRetrieveTransportStreamsUsingNetworkCheckCancel() throws Exception
    {
        // Create array of the arguments to pass to the method
        Object[] retTSArgs = { csidb.network3, requestor };
        SIChangeType changeType = SIChangeType.ADD;
        // Get a set of the expected objects.
        TransportStreamsChangedEvent event = new TransportStreamsChangedEvent(csidb, changeType,
                csidb.transport1.getTransportHandle());
        // Call the helper method using the created values
        checkCancel(retrieveTransportStreamsUsingNetworkMethod, retTSArgs, event);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of networks
        retTSArgs = new Object[] { csidb.network5, requestor };
        // Call the helper method again using new values
        checkCancel(retrieveTransportStreamsUsingNetworkMethod, retTSArgs, event);
    }

    /**
     * Tests <code>retrieveTransportStreams</code> to make sure it properly
     * times out and notifies the requestor accordingly.
     */
    public void testRetrieveTransportStreamsUsingNetworkCheckTimeOut() throws Exception
    {
        // Create an array of parameters to pass to the method
        Object[] retTSArgs = { csidb.network3, requestor };
        // Call the helper method using the created values
        checkTimeOut(retrieveTransportStreamsUsingNetworkMethod, retTSArgs);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of networks
        retTSArgs = new Object[] { csidb.network5, requestor };
        // Call the helper method again using new values
        checkTimeOut(retrieveTransportStreamsUsingNetworkMethod, retTSArgs);
    }

    // /////////////////////////////////////////////////////////////////////////
    // testRetrieveTransportStreamsUsingTransport section
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Tests <code>retrieveTransportStreams</code>' ability to properly cache
     * retrieved values.
     */
    public void testRetrieveTransportStreamsUsingTransportCheckCache() throws Exception
    {
        // Create an array of parameters to pass to the method
        Object[] retTSArgs = { csidb.transport1, requestor };
        // Get a set of the expected objects.
        Set s = createSet(new Object[] { csidb.transportStream7, csidb.transportStream8, csidb.transportStream9,
                csidb.transportStream10, csidb.transportStream11, csidb.transportStream12, csidb.transportStream13 });
        // Call the helper method using the created values
        checkCache(retrieveTransportStreamsUsingTransportMethod, retTSArgs, s, TransportStreamExt.class);

        requestor.reset();

        // Create an array of parameters to pass to the method
        retTSArgs = new Object[] { csidb.transport1, requestor };
        // Get a set of the expected objects.
        s = createSet(new Object[] { csidb.transportStream7, csidb.transportStream8, csidb.transportStream9,
                csidb.transportStream10, csidb.transportStream11, csidb.transportStream12, csidb.transportStream13 });
        // Call the helper method using the created values
        checkCache(retrieveTransportStreamsUsingTransportMethod, retTSArgs, s, TransportStreamExt.class);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of transportstreams
        retTSArgs = new Object[] { csidb.transport2, requestor };
        // Get a new set
        s = createSet(new Object[] { csidb.transportStream14 });
        // Call the helper method again using new values
        checkCache(retrieveTransportStreamsUsingTransportMethod, retTSArgs, s, TransportStreamExt.class);
    }

    /**
     * Tests <code>retrieveTransportStreams</code>' functionality to make sure
     * it properly notifies request objects of data unavailable failures.
     */
    public void testRetrieveTransportStreamsUsingTransportCheckUnavailable() throws Exception
    {
        // Create array of arguments to the method
        Object[] retTSArgs = { csidb.transport1, requestor };
        // Call the helper method using the created values
        checkUnavailable(retrieveTransportStreamsUsingTransportMethod, retTSArgs);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of transportstreams
        retTSArgs = new Object[] { csidb.transport2, requestor };
        // Call the helper method again using new values
        checkUnavailable(retrieveTransportStreamsUsingTransportMethod, retTSArgs);
    }

    /**
     * Tests <code>retrieveTransportStreams</code>' ability to handle data not
     * yet available notifications and that it properly notifies the requestor
     * when it becomes available.
     */
    public void testRetrieveTransportStreamsUsingTransportCheckNotYetAvailable() throws Exception
    {
        // Create array of arguments to the method
        Object[] retTSArgs = { csidb.transport1, requestor };
        SIChangeType changeType = SIChangeType.ADD;
        // Create an event for the database to fire
        TransportStreamsChangedEvent event = new TransportStreamsChangedEvent(csidb, changeType,
                csidb.transport1.getTransportHandle());
        // Get a set of the expected objects.
        Set s = createSet(new Object[] { csidb.transportStream7, csidb.transportStream8, csidb.transportStream9,
                csidb.transportStream10, csidb.transportStream11, csidb.transportStream12, csidb.transportStream13 });
        // Call the helper method using the created values
        checkNotYetAvailable(retrieveTransportStreamsUsingTransportMethod, retTSArgs, s, TransportStreamExt.class,
                event);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of transportstreams
        retTSArgs = new Object[] { csidb.transport2, requestor };
        // Get a new set
        s = createSet(new Object[] { csidb.transportStream14 });
        // Call the helper method again using new values
        checkNotYetAvailable(retrieveTransportStreamsUsingTransportMethod, retTSArgs, s, TransportStreamExt.class,
                event);
    }

    /**
     * Tests <code>retrieveTransportStreams</code> to make sure it properly
     * handles cancellations of si retrieval requests.
     */
    public void testRetrieveTransportStreamsUsingTransportCheckCancel() throws Exception
    {
        // Create array of arguments to the method
        Object[] retTSArgs = { csidb.transport1, requestor };
        SIChangeType changeType = SIChangeType.ADD;
        // Create event for database to fire
        TransportStreamsChangedEvent event = new TransportStreamsChangedEvent(csidb, changeType,
                csidb.transport1.getTransportHandle());
        // Call helper method using created values
        checkCancel(retrieveTransportStreamsUsingTransportMethod, retTSArgs, event);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of transportstreams
        retTSArgs = new Object[] { csidb.transport2, requestor };
        // Call helper method again using new values
        checkCancel(retrieveTransportStreamsUsingTransportMethod, retTSArgs, event);
    }

    /**
     * Tests <code>retrieveTransportStreams</code> to make sure it properly
     * times out and notifies the requestor accordingly.
     */
    public void testRetrieveTransportStreamsUsingTransportCheckTimeOut() throws Exception
    {
        // Array of arguments to the method
        Object[] retTSArgs = { csidb.transport1, requestor };
        // Call helper method using the created values
        checkTimeOut(retrieveTransportStreamsUsingTransportMethod, retTSArgs);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of transportstreams
        retTSArgs = new Object[] { csidb.transport2, requestor };
        // Call helper method again using new values
        checkTimeOut(retrieveTransportStreamsUsingTransportMethod, retTSArgs);
    }

    // /////////////////////////////////////////////////////////////////////////
    // testRetrieveDefaultMediaComponents section
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Tests <code>retrieveDefaultMediaComponents</code>' ability to properly
     * cache retrieved values.
     */
    public void testRetrieveDefaultMediaComponentsCheckCache() throws Exception
    {
        // Create an array of parameters to pass to the method
        Object[] retDMCArgs = { csidb.serviceDetails33, "eng", requestor };
        // Get a set of the expected objects.
        Set s = createSet(new Object[] { csidb.serviceComponent69, csidb.serviceComponent69eng });
        // Call the helper method using the created values
        tune(csidb.transportStream7);
        checkCache(retrieveDefaultMediaComponentsMethod, retDMCArgs, s, ServiceComponentExt.class);

        // Reset the requestor
        requestor.reset();

        // Create an array of parameters to pass to the method
        retDMCArgs = new Object[] { csidb.serviceDetails33, "fre", requestor };
        // Get a set of the expected objects.
        s = createSet(new Object[] { csidb.serviceComponent69, csidb.serviceComponent69eng });
        // Call the helper method using the created values
        checkCache(retrieveDefaultMediaComponentsMethod, retDMCArgs, s, ServiceComponentExt.class);

        // Reset the requestor
        requestor.reset();

        // Now we're going to get a different set of components
        retDMCArgs = new Object[] { csidb.serviceDetails35, "eng", requestor };
        // Get a new set
        s = createSet(new Object[] { csidb.serviceComponent71 });
        // Call the helper method again using new values
        tune(csidb.transportStream9);
        checkCache(retrieveDefaultMediaComponentsMethod, retDMCArgs, s, ServiceComponentExt.class);
    }

    /**
     * Tests <code>retrieveDefaultMediaComponents</code>' functionality to make
     * sure it properly notifies request objects of data unavailable failures.
     */
    public void testRetrieveDefaultMediaComponentsCheckUnavailable() throws Exception
    {
        // Create array of arguments to the method
        Object[] retDMCArgs = { csidb.serviceDetails33, "eng", requestor };
        // Call the helper method using the created values
        checkUnavailable(retrieveDefaultMediaComponentsMethod, retDMCArgs);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of transportstreams
        retDMCArgs = new Object[] { csidb.serviceDetails35, "eng", requestor };
        // Call the helper method again using new values
        checkUnavailable(retrieveDefaultMediaComponentsMethod, retDMCArgs);
    }

    /**
     * Tests <code>retrieveDefaultMediaComponents</code>' ability to handle data
     * not yet available notifications and that it properly notifies the
     * requestor when it becomes available.
     */
    public void testRetrieveDefaultMediaComponentsCheckNotYetAvailable() throws Exception
    {
        // Create array of arguments to the method
        Object[] retDMCArgs = { csidb.serviceDetails33, "eng", requestor };
        SIChangeType changeType = SIChangeType.ADD;
        // Create an event for the database to fire
        ServiceDetailsChangedEvent event = new ServiceDetailsChangedEvent(csidb, changeType,
                csidb.transport1.getTransportHandle());
        // Get a set of the expected objects.
        Set s = createSet(new Object[] { csidb.serviceComponent69, csidb.serviceComponent69eng });
        // Call the helper method using the created values
        tune(csidb.transportStream7);
        checkNotYetAvailable(retrieveDefaultMediaComponentsMethod, retDMCArgs, s, ServiceComponentExt.class, event);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of transportstreams
        retDMCArgs = new Object[] { csidb.serviceDetails35, "eng", requestor };
        // Get a new set
        s = createSet(new Object[] { csidb.serviceComponent71 });
        // Call the helper method again using new values
        tune(csidb.transportStream9);
        checkNotYetAvailable(retrieveDefaultMediaComponentsMethod, retDMCArgs, s, ServiceComponentExt.class, event);
    }

    /**
     * Tests <code>retrieveDefaultMediaComponents</code> to make sure it
     * properly handles cancellations of si retrieval requests.
     */
    public void testRetrieveDefaultMediaComponentsCheckCancel() throws Exception
    {
        // Create array of arguments to the method
        Object[] retDMCArgs = { csidb.serviceDetails33, "eng", requestor };
        SIChangeType changeType = SIChangeType.ADD;
        // Create event for database to fire
        ServiceDetailsChangedEvent event = new ServiceDetailsChangedEvent(csidb, changeType,
                csidb.transport1.getTransportHandle());
        // Call helper method using created values
        checkCancel(retrieveDefaultMediaComponentsMethod, retDMCArgs, event);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of transportstreams
        retDMCArgs = new Object[] { csidb.serviceDetails35, "eng", requestor };
        // Call helper method again using new values
        checkCancel(retrieveDefaultMediaComponentsMethod, retDMCArgs, event);
    }

    /**
     * Tests <code>retrieveDefaultMediaComponents</code> to make sure it
     * properly times out and notifies the requestor accordingly.
     */
    public void testRetrieveDefaultMediaComponentsCheckTimeOut() throws Exception
    {
        // Array of arguments to the method
        Object[] retDMCArgs = { csidb.serviceDetails33, "eng", requestor };
        // Call helper method using the created values
        checkTimeOut(retrieveDefaultMediaComponentsMethod, retDMCArgs);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of transportstreams
        retDMCArgs = new Object[] { csidb.serviceDetails35, "eng", requestor };
        // Call helper method again using new values
        checkTimeOut(retrieveDefaultMediaComponentsMethod, retDMCArgs);
    }

    // /////////////////////////////////////////////////////////////////////////
    // testRetrieveCarouselComponent section
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Tests <code>retrieveCarouselComponent</code>' ability to properly cache
     * retrieved values.
     */
    public void testRetrieveCarouselComponentCheckCache() throws Exception
    {
        // Create an array of parameters to pass to the method
        Object[] retCCArgs = { csidb.serviceDetails33, "eng", requestor };
        // Get a set of the expected objects.
        Set s = createSet(new Object[] { csidb.serviceComponent105 });
        // Call the helper method using the created values
        tune(csidb.transportStream7);
        checkCache(retrieveCarouselComponentMethod, retCCArgs, s, ServiceComponentExt.class);

        // Reset the requestor
        requestor.reset();

        // Create an array of parameters to pass to the method
        retCCArgs = new Object[] { csidb.serviceDetails33, "fre", requestor };
        // Get a set of the expected objects.
        s = createSet(new Object[] { csidb.serviceComponent105 });
        // Call the helper method using the created values
        checkCache(retrieveCarouselComponentMethod, retCCArgs, s, ServiceComponentExt.class);

        // Reset the requestor
        requestor.reset();

        // Now we're going to get a different set of components
        retCCArgs = new Object[] { csidb.serviceDetails35, "eng", requestor };
        // Get a new set
        s = createSet(new Object[] { csidb.serviceComponent107 });
        // Call the helper method again using new values
        tune(csidb.transportStream9);
        checkCache(retrieveCarouselComponentMethod, retCCArgs, s, ServiceComponentExt.class);
    }

    /**
     * Tests <code>retrieveCarouselComponent</code>' functionality to make sure
     * it properly notifies request objects of data unavailable failures.
     */
    public void testRetrieveCarouselComponentCheckUnavailable() throws Exception
    {
        // Create array of arguments to the method
        Object[] retCCArgs = { csidb.serviceDetails33, "eng", requestor };
        // Call the helper method using the created values
        checkUnavailable(retrieveCarouselComponentMethod, retCCArgs);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of transportstreams
        retCCArgs = new Object[] { csidb.serviceDetails35, "eng", requestor };
        // Call the helper method again using new values
        checkUnavailable(retrieveCarouselComponentMethod, retCCArgs);
    }

    /**
     * Tests <code>retrieveCarouselComponent</code>' ability to handle data not
     * yet available notifications and that it properly notifies the requestor
     * when it becomes available.
     */
    public void testRetrieveCarouselComponentCheckNotYetAvailable() throws Exception
    {
        // Create array of arguments to the method
        Object[] retCCArgs = { csidb.serviceDetails33, "eng", requestor };
        SIChangeType changeType = SIChangeType.ADD;
        // Create an event for the database to fire
        ServiceDetailsChangedEvent event = new ServiceDetailsChangedEvent(csidb, changeType,
                csidb.transport1.getTransportHandle());
        // Get a set of the expected objects.
        Set s = createSet(new Object[] { csidb.serviceComponent69 });
        // Call the helper method using the created values
        tune(csidb.transportStream7);
        checkNotYetAvailable(retrieveCarouselComponentMethod, retCCArgs, s, ServiceComponentExt.class, event);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of transportstreams
        retCCArgs = new Object[] { csidb.serviceDetails35, "eng", requestor };
        // Get a new set
        s = createSet(new Object[] { csidb.serviceComponent71 });
        // Call the helper method again using new values
        tune(csidb.transportStream9);
        checkNotYetAvailable(retrieveCarouselComponentMethod, retCCArgs, s, ServiceComponentExt.class, event);
    }

    /**
     * Tests <code>retrieveCarouselComponent</code> to make sure it properly
     * handles cancellations of si retrieval requests.
     */
    public void testRetrieveCarouselComponentCheckCancel() throws Exception
    {
        // Create array of arguments to the method
        Object[] retCCArgs = { csidb.serviceDetails33, "eng", requestor };
        SIChangeType changeType = SIChangeType.ADD;
        // Create event for database to fire
        ServiceDetailsChangedEvent event = new ServiceDetailsChangedEvent(csidb, changeType,
                csidb.transport1.getTransportHandle());
        // Call helper method using created values
        checkCancel(retrieveCarouselComponentMethod, retCCArgs, event);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of transportstreams
        retCCArgs = new Object[] { csidb.serviceDetails35, "eng", requestor };
        // Call helper method again using new values
        checkCancel(retrieveCarouselComponentMethod, retCCArgs, event);
    }

    /**
     * Tests <code>retrieveCarouselComponent</code> to make sure it properly
     * times out and notifies the requestor accordingly.
     */
    public void testRetrieveCarouselComponentCheckTimeOut() throws Exception
    {
        // Array of arguments to the method
        Object[] retCCArgs = { csidb.serviceDetails33, "eng", requestor };
        // Call helper method using the created values
        checkTimeOut(retrieveCarouselComponentMethod, retCCArgs);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of transportstreams
        retCCArgs = new Object[] { csidb.serviceDetails35, "eng", requestor };
        // Call helper method again using new values
        checkTimeOut(retrieveCarouselComponentMethod, retCCArgs);
    }

    // /////////////////////////////////////////////////////////////////////////
    // testRetrieveCarouselComponentUsingID section
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Tests <code>retrieveCarouselComponent</code>' ability to properly cache
     * retrieved values.
     */
    public void testRetrieveCarouselComponentUsingIDCheckCache() throws Exception
    {
        // Create an array of parameters to pass to the method
        Object[] retCCArgs = { csidb.serviceDetails33, new Integer(46), "eng", requestor };
        // Get a set of the expected objects.
        Set s = createSet(new Object[] { csidb.serviceComponent105 });
        // Call the helper method using the created values
        tune(csidb.transportStream7);
        checkCache(retrieveCarouselComponentUsingIDMethod, retCCArgs, s, ServiceComponentExt.class);

        // Reset the requestor
        requestor.reset();

        // Create an array of parameters to pass to the method
        retCCArgs = new Object[] { csidb.serviceDetails33, new Integer(46), "eng", requestor };
        // Get a set of the expected objects.
        s = createSet(new Object[] { csidb.serviceComponent105 });
        // Call the helper method using the created values
        checkCache(retrieveCarouselComponentUsingIDMethod, retCCArgs, s, ServiceComponentExt.class);

        // Reset the requestor
        requestor.reset();

        // Now we're going to get a different set of components
        retCCArgs = new Object[] { csidb.serviceDetails35, new Integer(30), "eng", requestor };
        // Get a new set
        s = createSet(new Object[] { csidb.serviceComponent107 });
        // Call the helper method again using new values
        tune(csidb.transportStream9);
        checkCache(retrieveCarouselComponentUsingIDMethod, retCCArgs, s, ServiceComponentExt.class);
    }

    /**
     * Tests <code>retrieveCarouselComponent</code>' functionality to make sure
     * it properly notifies request objects of data unavailable failures.
     */
    public void testRetrieveCarouselComponentUsingIDCheckUnavailable() throws Exception
    {
        // Create array of arguments to the method
        Object[] retCCArgs = { csidb.serviceDetails33, new Integer(46), "eng", requestor };
        // Call the helper method using the created values
        checkUnavailable(retrieveCarouselComponentUsingIDMethod, retCCArgs);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of transportstreams
        retCCArgs = new Object[] { csidb.serviceDetails35, new Integer(30), "eng", requestor };
        // Call the helper method again using new values
        checkUnavailable(retrieveCarouselComponentUsingIDMethod, retCCArgs);
    }

    /**
     * Tests <code>retrieveCarouselComponent</code>' ability to handle data not
     * yet available notifications and that it properly notifies the requestor
     * when it becomes available.
     */
    public void testRetrieveCarouselComponentUsingIDCheckNotYetAvailable() throws Exception
    {
        // Create array of arguments to the method
        Object[] retCCArgs = { csidb.serviceDetails33, new Integer(46), "eng", requestor };
        SIChangeType changeType = SIChangeType.ADD;
        // Create an event for the database to fire
        ServiceDetailsChangedEvent event = new ServiceDetailsChangedEvent(csidb, changeType,
                csidb.transport1.getTransportHandle());
        // Get a set of the expected objects.
        Set s = createSet(new Object[] { csidb.serviceComponent105 });
        // Call the helper method using the created values
        tune(csidb.transportStream7);
        checkNotYetAvailable(retrieveCarouselComponentUsingIDMethod, retCCArgs, s, ServiceComponentExt.class, event);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of transportstreams
        retCCArgs = new Object[] { csidb.serviceDetails35, new Integer(30), "eng", requestor };
        // Get a new set
        s = createSet(new Object[] { csidb.serviceComponent107 });
        // Call the helper method again using new values
        tune(csidb.transportStream9);
        checkNotYetAvailable(retrieveCarouselComponentUsingIDMethod, retCCArgs, s, ServiceComponentExt.class, event);
    }

    /**
     * Tests <code>retrieveCarouselComponent</code> to make sure it properly
     * handles cancellations of si retrieval requests.
     */
    public void testRetrieveCarouselComponentUsingIDCheckCancel() throws Exception
    {
        // Create array of arguments to the method
        Object[] retCCArgs = { csidb.serviceDetails33, new Integer(46), "eng", requestor };
        SIChangeType changeType = SIChangeType.ADD;
        // Create event for database to fire
        ServiceDetailsChangedEvent event = new ServiceDetailsChangedEvent(csidb, changeType,
                csidb.transport1.getTransportHandle());
        // Call helper method using created values
        checkCancel(retrieveCarouselComponentUsingIDMethod, retCCArgs, event);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of transportstreams
        retCCArgs = new Object[] { csidb.serviceDetails35, new Integer(30), "eng", requestor };
        // Call helper method again using new values
        checkCancel(retrieveCarouselComponentUsingIDMethod, retCCArgs, event);
    }

    /**
     * Tests <code>retrieveCarouselComponent</code> to make sure it properly
     * times out and notifies the requestor accordingly.
     */
    public void testRetrieveCarouselComponentUsingIDCheckTimeOut() throws Exception
    {
        // Array of arguments to the method
        Object[] retCCArgs = { csidb.serviceDetails33, new Integer(46), "eng", requestor };
        // Call helper method using the created values
        checkTimeOut(retrieveCarouselComponentUsingIDMethod, retCCArgs);

        // Reset the requestor
        requestor.reset();
        // Now we're going to get a different set of transportstreams
        retCCArgs = new Object[] { csidb.serviceDetails35, new Integer(30), "eng", requestor };
        // Call helper method again using new values
        checkTimeOut(retrieveCarouselComponentUsingIDMethod, retCCArgs);
    }

    /**
     * Tests that items are flushed from the cache when no external hard
     * references remain and maxAge is achieved.
     */
    public void testCacheLeak() throws Exception
    {
        // TODO(Todd): Add PAT and PMT objects to this test. Need canned entries
        // for PAT and PMT in the csidb.

        // Retrieve SI objects to force them into the cache
        RatingDimension rating = sic.getRatingDimension("MMPA", language);
        Transport transport = sic.getTransports()[0];
        Network network = ((TransportExt) transport).getNetworks()[0];
        TransportStream stream = ((TransportExt) transport).getTransportStreams()[0];
        tune(csidb.transportStream7);
        Service service = sic.getService(csidb.service15.getLocator(), language);
        ServiceDetails details = ((ServiceExt) service).getDetails();
        ServiceDescription description = ((ServiceDetailsExt) details).getServiceDescription();
        ServiceComponent component = ((ServiceDetailsExt) details).getComponents()[0];

        // Create weak references to SI objects then clear hard references
        WeakReference ratingRef = new WeakReference(rating);
        WeakReference transportRef = new WeakReference(transport);
        WeakReference networkRef = new WeakReference(network);
        WeakReference streamRef = new WeakReference(stream);
        WeakReference serviceRef = new WeakReference(service);
        WeakReference detailsRef = new WeakReference(details);
        WeakReference descriptionRef = new WeakReference(description);
        WeakReference componentRef = new WeakReference(component);
        rating = null;
        transport = null;
        network = null;
        stream = null;
        service = null;
        details = null;
        description = null;
        component = null;

        // Wait for SI objects to expire from the cache and make sure they are
        // discarded
        int maxAge = MPEEnv.getEnv("OCAP.sicache.maxAge", 0);
        Thread.sleep(maxAge + 2000);
        System.gc();
        System.gc();
        assertNull("Leak detected for rating dimension", ratingRef.get());
        assertNull("Leak detected for transport", transportRef.get());
        assertNull("Leak detected for network", networkRef.get());
        assertNull("Leak detected for transport stream", streamRef.get());
        assertNull("Leak detected for service", serviceRef.get());
        assertNull("Leak detected for service details", detailsRef.get());
        assertNull("Leak detected for service description", descriptionRef.get());
        assertNull("Leak detected for service component", componentRef.get());
    }

    // /////////////////////////////////////////////////////////////////////////
    // Helper check methods section
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Generalized method for testing caching and flushing functionality. This
     * method uses reflection to test each different "retrieval" method in
     * SICache. For more description of what this test covers, read parts 1 & 2
     * of the <i>Asynchronous Retrieval Testing</i> and <i>Caching/Flushing
     * Testing</i>.
     * 
     * @param m
     *            The method we will be calling.
     * @param args
     *            The arguments being passed to said method.
     * @param soughtSet
     *            Contains the results we expect.
     * @param retrievableType
     *            Used to check the return types of the result.
     */
    protected void checkCache(Method m, Object[] args, Set soughtSet, Class retrievableType) throws Exception
    {
        requestor.reset();
        csidb.cannedSetForcedException(null);
        try
        {
            // Send the request for the SI object and get receipt
            request = (SIRequest) m.invoke(sic, args);
        }
        catch (Exception e)
        {
            fail("Caught " + e.getClass() + ": " + e.getMessage());
        }

        // Wait for the event to fire
        requestor.waitForEvents();
        // Make sure that we got our request properly within the time limit
        assertTrue("Requestor was not called", requestor.success);
        if (requestor.failtype != null) fail("Failure occurred during retrieval " + requestor.failtype.toString());

        // Get the results
        SIRetrievable[] retrievable = requestor.getResults();
        // Make sure array has correct number of entries in it
        assertEquals("Array length is incorrect", soughtSet.size(), retrievable.length);

        // This test is redundant since the subsequent test encompasses it, but
        // it is included to improve debugging, since the generalized Set test
        // will be rather vague.
        for (int i = 0; i < retrievable.length; i++)
            assertTrue("Wrong Type returned", (retrievableType.isInstance(retrievable[i])));

        // Make sure that the object returned matches what we expected
        Set resultsOne = createSet(retrievable);
        assertEquals("Set sizes do not match", soughtSet.size(), resultsOne.size());
        // Compare our two Sets
        assertEquals("Returned results do not match expected values.", soughtSet, resultsOne);

        // Create an array of the available methods
        Method[] methods;
        Object[] instances = null;
        if (retrievable[0] instanceof ServiceDescription)
        {
            methods = ServiceDetailsExt.class.getMethods();
            instances = new Object[] { ((ServiceDescriptionExt) retrievable[0]).getServiceDetails() };
        }
        else
        {
            methods = retrievableType.getMethods();
            instances = retrievable;
        }
        Method handleMethod = null;

        // Extract the getXXXHandle method from the array
        for (int i = 0; i < methods.length; i++)
        {
            if ("Handle".equals(methods[i].getName().substring(methods[i].getName().length() - 6)))
            {
                handleMethod = methods[i];
                break;
            }
        }
        // Call the getXXXHandle method to count object creations
        for (int i = 0; i < instances.length; i++)
        {
            Object handle = handleMethod.invoke(instances[i], new Object[] {});
            assertEquals("Handle create count is incorrect with retrievable #" + i, 1,
                    csidb.cannedGetHandleCreateCount(handle));
        }

        requestor.reset();
        try
        {
            // Call for the results again
            request = (SIRequest) m.invoke(sic, args);
        }
        catch (Exception e)
        {
            fail("Caught " + e.getClass() + ": " + e.getMessage());
        }
        // Wait for the event to fire
        requestor.waitForEvents();
        // Make sure that we got our request properly within the time limit
        assertTrue("Requestor was not called", requestor.success);
        if (requestor.failtype != null)
            assertNull("Failure occurred during retrieval: " + requestor.failtype.toString(), requestor.failtype);

        // Get the results again
        retrievable = requestor.getResults();
        // Make sure array has the expected number of entries in it
        assertEquals("Array length is incorrect", soughtSet.size(), retrievable.length);
        // Make sure that our array contains objects of the specified type
        for (int i = 0; i < retrievable.length; i++)
            assertTrue("Wrong Type returned", retrievableType.isInstance(retrievable[i]));

        // Make sure that the object returned matches what we expected
        Set resultsTwo = createSet(retrievable);
        assertEquals("Cached results are not the same as the original values.", resultsOne, resultsTwo);
        // Call the getXXXHandle method to count object creations
        if (retrievable[0] instanceof ServiceDescription)
        {
            instances = new Object[] { ((ServiceDescriptionExt) retrievable[0]).getServiceDetails() };
        }
        else
        {
            instances = retrievable;
        }
        for (int i = 0; i < retrievable.length; i++)
        {

            Object handle = handleMethod.invoke(instances[i], new Object[] {});
            assertEquals("Handle create count is incorrect", 1, csidb.cannedGetHandleCreateCount(handle));
        }
    }

    /**
     * This checks how SICache handles SINotAvailableException. See part 3 of
     * the <i>Asynchronous Retrieval Testing</i>.
     * 
     * @param m
     *            The method we are calling.
     * @param args
     *            The arguments passed to said method.
     */
    protected void checkUnavailable(Method m, Object[] args) throws Exception
    {
        requestor.reset();
        csidb.cannedSetForcedException(SINotAvailableException.class);
        try
        {
            request = (SIRequest) m.invoke(sic, args);
        }
        catch (Exception e)
        {
            fail("Caught " + e.getClass() + ": " + e.getMessage());
        }
        requestor.waitForEvents();
        // Check to see if we got the failure correctly
        assertNotNull("Failure should have occurred with forced exception", requestor.getFailure());
        assertEquals("SIRequestFailureType does not match expected value.", "DATA_UNAVAILABLE", requestor.getFailure()
                .toString());
    }

    /**
     * Checks how SICache handles SINotYetAvailableException. See part 4 of the
     * <i>Asynchronous Retrieval Testing</i>.
     * 
     * @param m
     *            The method we are calling.
     * @param args
     *            The arguments to said method.
     * @param soughtSet
     *            The results we are expecting.
     * @param retrievableType
     *            The type we expect the results to be.
     */
    protected void checkNotYetAvailable(Method m, Object[] args, Set soughtSet, Class retrievableType,
            SIChangedEvent event) throws Exception
    {
        requestor.reset();
        csidb.cannedSetForcedException(SINotAvailableYetException.class);
        try
        {
            request = (SIRequest) m.invoke(sic, args);
        }
        catch (Exception e)
        {
            fail("Caught " + e.getClass() + ": " + e.getMessage());
        }
        assertTrue("Request listener was never notified", csidb.cannedSIWasRequested());
        // Clear the exception throwing to that the request can go through
        csidb.cannedClearAllForcedExceptions();
        // Update our data so SICache sends the notification
        csidb.cannedSendSIAcquired(event);
        // Wait a bit longer for SICache to send the notification
        requestor.waitForEvents();
        // Make sure we actually got an event notification
        assertTrue("Requestor was not called or a failure occurred.", requestor.succeeded());
        assertNull("Failure type should be null", requestor.failtype);
        // Now let's get our results
        SIRetrievable[] retrievable = requestor.getResults();
        // Make sure array has the expected number of entries in it
        assertEquals("Array length is incorrect", soughtSet.size(), retrievable.length);
        // Make sure that our array contains objects of the specified type
        for (int i = 0; i < retrievable.length; i++)
            assertTrue("Wrong Type returned", retrievableType.isInstance(retrievable[i]));
    }

    /**
     * Tests how SICache handles a time out. This covers part 5 of the
     * <i>Asynchronous Retrieval Testing</i>.
     * 
     * @param m
     *            The method we are calling.
     * @param args
     *            The arguments to that method.
     */
    protected void checkTimeOut(Method m, Object[] args) throws Exception
    {
        requestor.reset();
        csidb.cannedSetForcedException(SINotAvailableYetException.class);
        try
        {
            request = (SIRequest) m.invoke(sic, args);
        }
        catch (Exception e)
        {
            fail("Caught " + e.getClass() + ": " + e.getMessage());
        }
        assertTrue("Request listener was never notified", csidb.cannedSIWasRequested());
        // Wait for SICache to send the timeout notification.
        requestor.waitForEvents();
        // Check to see if we got the failure correctly
        assertNotNull("Failure should have occurred with forced exception", requestor.getFailure());
        assertEquals("SIRequestFailureType does not match expected value.", "DATA_UNAVAILABLE", requestor.getFailure()
                .toString());
        // We'll try to send the cancel now and make sure it fails
        assertFalse("Request not cancelled correctly.", request.cancel());
    }

    /**
     * Tests to see if SICache properly handles a cancel request. This covers
     * part 6 of the <i>Asynchronous Retrieval Testing</i>.
     * 
     * @param m
     *            The method we are calling.
     * @param args
     *            The arguments to said method.
     */
    protected void checkCancel(Method m, Object[] args, SIChangedEvent event) throws Exception
    {
        requestor.reset();
        csidb.cannedSetForcedException(SINotAvailableYetException.class);
        try
        {
            request = (SIRequest) m.invoke(sic, args);
        }
        catch (Exception e)
        {
            fail("Caught " + e.getClass() + ": " + e.getMessage());
        }
        // Immediately cancel the request
        assertTrue("Request not cancelled correctly.", request.cancel());
        // Wait for the event to post
        requestor.waitForEvents();
        // Check to make sure we got our Cancelled notification
        assertNotNull("Failure should have occurred with cancellation.", requestor.getFailure());
        assertEquals("SIRequestFailureType does not match expected value", "CANCELED", requestor.getFailure()
                .toString());
        // Fire acquired event to see if SICache notifies requestor.
        requestor.reset();
        csidb.cannedSendSIAcquired(event);
        // Wait for the event to propagate
        requestor.waitForEvents();
        // Check to see if our requestor was notifed
        assertFalse("Requestor was notified after cancel was sent.", requestor.succeeded());
        // We'll try to send the cancel now and make sure it fails
        assertFalse("Request not cancelled correctly.", request.cancel());
    }

    // /////////////////////////////////////////////////////////////////////////

    /**
     * Load tests SICache by hitting it many times with requests for retrieval.
     * The test starts off by requesting retrieval for a single SI element 50
     * times. Then, it requests 5 different SI, but of the same type, 50 times
     * each, for a total of 250 times. Finally, it requests 5 different SI of
     * different types, 50 times each, for a total of 250 times.
     */
    public void XXXtestCacheLoad() throws Exception
    {

        csidb.cannedSetForcedException(null);

        // First we're going to test for requests for a single SIElement
        // Array of the arguments we are passing
        Object[] args = { csidb.transport1, "eng", null };
        // A set holding all of the objects we expect
        Set s = createSet(new Object[] { csidb.transportStream7, csidb.transportStream8, csidb.transportStream9,
                csidb.transportStream10, csidb.transportStream11, csidb.transportStream12 });
        // Create our tester thread with the array from above
        CacheLoadTester c1 = new CacheLoadTester(retrieveTransportStreamsUsingTransportMethod, args, s,
                TransportStream.class, sic, "retrieveTransportStreams by using a Transport");
        // Start our tester thread
        c1.start();

        // Now we'll try multiple si of the same type
        // An array of our tester threads
        CacheLoadTester[] testers = new CacheLoadTester[5];
        // Array of arrays holding our arguments
        Object[][] args2 = { { csidb.serviceDetails33, "eng", null }, { csidb.serviceDetails34, "eng", null },
                { csidb.serviceDetails35, "eng", null }, { csidb.serviceDetails36, "eng", null },
                { csidb.serviceDetails37, "eng", null } };
        // An array of sets holding our expected results
        Set[] sought = new Set[5];
        sought[0] = createSet(new Object[] { csidb.serviceComponent69 });
        sought[1] = createSet(new Object[] { csidb.serviceComponent70 });
        sought[2] = createSet(new Object[] { csidb.serviceComponent71 });
        sought[3] = createSet(new Object[] { csidb.serviceComponent72 });
        sought[4] = createSet(new Object[] { csidb.serviceComponent73 });
        // Create our threads using the above arrays
        for (int i = 0; i < 5; i++)
            testers[i] = new CacheLoadTester(retrieveServiceComponentsMethod, args2[i], sought[i],
                    ServiceComponent.class, sic, "retrieveServiceComponents");
        // Start up the threads
        for (int i = 0; i < 5; i++)
            testers[i].start();

        // Lastly, we'll try lots of different SI
        // Array of our threads
        CacheLoadTester[] testers2 = new CacheLoadTester[5];
        // Array of arrays holding the arguments
        Object[][] args3 = { { csidb.service15, "eng", Boolean.TRUE, null }, // retrieveServiceDetails
                { csidb.transportStream7.getLocator(), "eng", null }, // retrieveTransportStream
                { csidb.serviceDetails40, "eng", null }, // retrieveServiceDescription
                { csidb.serviceDetails41, "eng", null }, // retrieveServiceComponents
                { csidb.transport1, csidb.network3.getLocator(), "eng", null } // retrieveNetwork
        };
        // Array of sets holding our expected values
        sought = new Set[5];
        sought[0] = createSet(new Object[] { csidb.serviceDetails33 });
        sought[1] = createSet(new Object[] { csidb.transportStream7 });
        sought[2] = createSet(new Object[] { csidb.serviceDescription40 });
        sought[3] = createSet(new Object[] { csidb.serviceComponent76, csidb.serviceComponent112 });
        sought[4] = createSet(new Object[] { csidb.network3 });
        // Array of the methods we are calling
        Method[] m = { retrieveServiceDetailsMethod, retrieveTransportStreamMethod, retrieveServiceDescriptionMethod,
                retrieveServiceComponentsMethod, retrieveNetworkMethod };
        // Array of the class types we are expecting for each method
        Class[] types = { ServiceDetails.class, TransportStream.class, ServiceDescription.class,
                ServiceComponent.class, Network.class };
        String[] names = { "retrieveServiceDetails", "retrieveTransportStream", "retrieveServiceDescription",
                "retrieveServiceComponents", "retrieveNetwork" };
        // Pass all the arrays to each of the 5 testers
        for (int i = 0; i < 5; i++)
            testers2[i] = new CacheLoadTester(m[i], args3[i], sought[i], types[i], sic, names[i]);
        // Start up the tester threads
        for (int i = 0; i < 5; i++)
            testers2[i].start();
    }

    /**
     * This takes an Object array and constructs a Set out of it.
     * 
     * @param values
     *            The array of objects to be put into the Set.
     * @return the Set created from the passed-in array.
     */
    protected Set createSet(Object[] values)
    {
        Set s = new HashSet();
        for (int i = 0; i < values.length; i++)
        {
            s.add(values[i]);
        }

        return s;
    }

    /**
     * This gets the Method from the specified class using the parms array and
     * name as arguments. More specifically, this is used to get the retrieve
     * methods from the SICache for testing purposes.
     * 
     * @param clazz
     *            The class that contains the method we want; in this case,
     *            SICache.
     * @param parms
     *            The parameters of the method we seek that define its
     *            signature.
     * @param name
     *            The name of the method we are getting.
     * @return a reference to the method for which we asked.
     */
    protected static Method getMethod(Class clazz, Class[] parms, String name) throws SecurityException,
            NoSuchMethodException
    {
        return clazz.getMethod(name, parms);
    }

    // Data Section \\

    // Variables for running the test
    private SIRequestorTest requestor;

    private SIRequest request;

    private String language;

    private ChangeListenerTest listener;

    // Static references to each of the retrieval methods
    // - used to simplify repeated calls to each method
    private static Method retrieveTransportStreamMethod;

    private static Method retrieveTransportStreamsUsingNetworkMethod;

    private static Method retrieveTransportStreamsUsingTransportMethod;

    private static Method retrieveServiceDetailsMethod;

    private static Method retrieveNetworkMethod;

    private static Method retrieveNetworksMethod;

    private static Method retrieveServiceComponentsMethod;

    private static Method retrieveServiceDescriptionMethod;

    private static Method retrieveSIElementMethod;

    private static Method retrieveDefaultMediaComponentsMethod;

    private static Method retrieveCarouselComponentMethod;

    private static Method retrieveCarouselComponentUsingIDMethod;

    static
    {
        try
        {
            Class[] retTStreamsTrans = { Transport.class, SIRequestor.class };
            retrieveTransportStreamsUsingTransportMethod = getMethod(SICache.class, retTStreamsTrans,
                    "retrieveTransportStreams");
            Class[] retTStreamsNet = { Network.class, SIRequestor.class };
            retrieveTransportStreamsUsingNetworkMethod = getMethod(SICache.class, retTStreamsNet,
                    "retrieveTransportStreams");
            Class[] retTStream = { Locator.class, SIRequestor.class };
            retrieveTransportStreamMethod = getMethod(SICache.class, retTStream, "retrieveTransportStream");
            Class[] retSDetails = { Service.class, String.class, boolean.class, SIRequestor.class };
            retrieveServiceDetailsMethod = getMethod(SICache.class, retSDetails, "retrieveServiceDetails");
            Class[] retSDesc = { ServiceDetails.class, String.class, SIRequestor.class };
            retrieveServiceDescriptionMethod = getMethod(SICache.class, retSDesc, "retrieveServiceDescription");
            Class[] retSComp = { ServiceDetails.class, String.class, SIRequestor.class };
            retrieveServiceComponentsMethod = getMethod(SICache.class, retSComp, "retrieveServiceComponents");
            Class[] retNetworks = { Transport.class, SIRequestor.class };
            retrieveNetworksMethod = getMethod(SICache.class, retNetworks, "retrieveNetworks");
            Class[] retNetwork = { Locator.class, SIRequestor.class };
            retrieveNetworkMethod = getMethod(SICache.class, retNetwork, "retrieveNetwork");
            Class[] retSIElement = { Locator.class, String.class, SIRequestor.class };
            retrieveSIElementMethod = getMethod(SICache.class, retSIElement, "retrieveSIElement");
            Class[] retDefMComp = { ServiceDetails.class, String.class, SIRequestor.class };
            retrieveDefaultMediaComponentsMethod = getMethod(SICache.class, retDefMComp,
                    "retrieveDefaultMediaComponents");
            Class[] retCarComp = { ServiceDetails.class, String.class, SIRequestor.class };
            retrieveCarouselComponentMethod = getMethod(SICache.class, retCarComp, "retrieveCarouselComponent");
            Class[] retCarCompID = { ServiceDetails.class, int.class, String.class, SIRequestor.class };
            retrieveCarouselComponentUsingIDMethod = getMethod(SICache.class, retCarCompID, "retrieveCarouselComponent");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * This class is used in load testing the SICache. It runs in a separate
     * thread and hits the cache 50 times for the type of SI that was passed to
     * it
     * 
     * @author Joshua Keplinger
     */
    protected class CacheLoadTester extends Thread
    {
        private Method m;

        private Object[] args;

        private Set sought;

        private Class retType;

        private SICache siCache;

        private String name;

        CacheLoadTester(Method m, Object[] args, Set soughtSet, Class retrievableType, SICache siCache, String name)
        {
            this.m = m;
            this.args = args;
            this.sought = soughtSet;
            this.retType = retrievableType;
            this.siCache = siCache;
            this.name = name;
        }

        public void run()
        {
            try
            {
                assaultCache(m, args, sought, retType, name);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        protected synchronized void assaultCache(Method m, Object[] args, Set soughtSet, Class retrievableType,
                String name) throws Exception
        {

            if (m == null) throw new NullPointerException("Method is null trying to call " + name);
            // Create our array of requestors and requests
            SIRequestorTest[] requestors = new SIRequestorTest[50];
            SIRequest[] requests = new SIRequest[50];
            for (int i = 0; i < 50; i++)
            {
                requestors[i] = new SIRequestorTest();
            }
            // Send requests for SI
            try
            {
                for (int i = 0; i < 50; i++)
                {
                    args[args.length - 1] = requestors[i];
                    requests[i] = (SIRequest) m.invoke(siCache, args);
                    Thread.sleep(4);
                }
            }
            catch (Exception e)
            {
                System.out.println(e.getMessage() + " thrown while calling " + m.getName());
                throw e;
            }
            // Make sure our requests array was filled
            assertNotNull("Requests array should not be null", requests);
            assertEquals("Request array length is incorrect", 50, requests.length);

            // Now let's check all of the requestors to see if they were
            // notified
            // after a short wait
            requestors[0].waitForEvents();
            SIRetrievable[] retrievable = null;
            for (int i = 0; i < 50; i++)
            {
                assertTrue("Requestor #" + i + " was not notified", requestors[i].succeeded());
                retrievable = requestors[i].getResults();
                Set s = createSet(retrievable);
                assertEquals("Returned results to not match expected values", soughtSet, s);
            }

            // Create an array of the available methods
            Method[] methods;
            Object[] instances = null;
            if (retrievable[0] instanceof ServiceDescription)
            {
                methods = ServiceDetailsExt.class.getMethods();
                instances = new Object[] { ((ServiceDescriptionExt) retrievable[0]).getServiceDetails() };
            }
            else
            {
                methods = retrievableType.getMethods();
                instances = retrievable;
            }
            Method handleMethod = null;

            // Extract the getXXXHandle method from the array
            for (int i = 0; i < methods.length; i++)
            {
                if ("Handle".equals(methods[i].getName().substring(methods[i].getName().length() - 6)))
                {
                    handleMethod = methods[i];
                    break;
                }
            }
            // Call the getXXXHandle method to count object creations
            for (int i = 0; i < retrievable.length; i++)
            {
                Object handle = handleMethod.invoke(instances[i], new Object[] {});
                assertEquals("Handle create count is incorrect", 1, csidb.cannedGetHandleCreateCount(handle));
            }

            // Now we're going to send all of the requests through again
            // and they should be cached.
            for (int i = 0; i < 50; i++)
            {
                requestors[i] = new SIRequestorTest();
            }
            // Send requests for SI
            for (int i = 0; i < 50; i++)
            {
                requests[i] = (SIRequest) m.invoke(siCache, args);
            }

            // Now let's check all of the requestors to see if they were
            // notified
            // after a short wait
            requestors[0].waitForEvents();
            for (int i = 0; i < 50; i++)
            {
                assertTrue("Requestor #" + i + " was not notified", requestors[i].succeeded());
                retrievable = requestors[i].getResults();
                Set s = createSet(retrievable);
                assertEquals("Returned results to not match expected values", soughtSet, s);
            }

            // Call the getXXXHandle method to count object creations
            if (retrievable[0] instanceof ServiceDescription)
            {
                instances = new Object[] { ((ServiceDescriptionExt) retrievable[0]).getServiceDetails() };
            }
            else
            {
                instances = retrievable;
            }
            for (int i = 0; i < retrievable.length; i++)
            {

                Object handle = handleMethod.invoke(instances[i], new Object[] {});
                assertEquals("Handle create count is incorrect", 1, csidb.cannedGetHandleCreateCount(handle));
            }
        }
    }

    /**
     * All encompassing listener for listener tests. This class implements all 4
     * types of listeners required for testing the listener functionality of
     * SICache.
     * 
     * @author Joshua Keplinger
     */
    protected class ChangeListenerTest implements NetworkChangeListener, TransportStreamChangeListener,
            ServiceComponentChangeListener, ServiceDetailsChangeListener
    {

        public boolean tsChange = false;

        public boolean scChange = false;

        public boolean sdChange = false;

        public boolean netChange = false;

        public int eventReceived = 0;

        TransportStreamChangeEvent tsEvent = null;

        ServiceComponentChangeEvent scEvent = null;

        ServiceDetailsChangeEvent sdEvent = null;

        NetworkChangeEvent netEvent = null;

        public synchronized void notifyChange(TransportStreamChangeEvent event)
        {
            tsEvent = event;
            eventReceived++;
            tsChange = true;
            notify();
        }

        public synchronized void notifyChange(ServiceComponentChangeEvent event)
        {
            scEvent = event;
            eventReceived++;
            scChange = true;
            notify();
        }

        public synchronized void notifyChange(ServiceDetailsChangeEvent event)
        {
            sdEvent = event;
            eventReceived++;
            sdChange = true;
            notify();
        }

        public synchronized void notifyChange(NetworkChangeEvent event)
        {
            netEvent = event;
            eventReceived++;
            netChange = true;
            notify();
        }

        public void reset()
        {
            tsChange = false;
            sdChange = false;
            netChange = false;
            scChange = false;
            eventReceived = 0;
            tsEvent = null;
            sdEvent = null;
            netEvent = null;
            scEvent = null;
        }

        public synchronized void waitForEvents(int numEvents) throws InterruptedException
        {
            for (int i = 0; i < numEvents; i++)
            {
                wait(waitForRequest);
            }
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

        private boolean done = false;

        public synchronized void notifyFailure(SIRequestFailureType reason)
        {
            failtype = reason;
            // System.out.println("notifyFailure Called...");
            assertNotNull("Failure type should not be null", failtype);
            success = false;
            done = true;
            notify();
        }

        public synchronized void notifySuccess(SIRetrievable[] result)
        {
            this.results = result;
            // System.out.println("notifySuccess Called...");
            assertNotNull("Results should not be null", results);
            success = true;
            done = true;
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

        public synchronized void reset()
        {
            failtype = null;
            results = new SIRetrievable[0];
            success = false;
            done = false;
        }

        public synchronized void waitForEvents() throws InterruptedException
        {
            if (!done) wait(waitForRequest);
        }
    }

    /**
     * This is a default factory class that is passed to the
     * <code>CannedSICacheTest</code>. It is used to instantiate a concrete
     * class to be used in the test.
     * 
     * @author Joshua Keplinger
     */
    private static class CannedSICacheTestFactory implements ImplFactory
    {
        public static CannedSIDatabase csidb;

        public Object createImplObject() throws Exception
        {
            CannedServiceMgr sm = (CannedServiceMgr) CannedServiceMgr.getInstance();
            csidb = (CannedSIDatabase) sm.getSIDatabase();
            return sm.getSICache();
        }

        public String toString()
        {
            return "CannedSICacheTestFactory";
        }
    }

    ResourceClient rc = new ResourceClient()
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
    };

    NetworkInterfaceController nic;
}
