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
package org.cablelabs.impl.manager.service;

import javax.tv.service.SIChangeType;
import javax.tv.service.navigation.ServiceComponentChangeEvent;
import javax.tv.service.navigation.ServiceComponentChangeListener;
import javax.tv.service.transport.NetworkChangeEvent;
import javax.tv.service.transport.NetworkChangeListener;
import javax.tv.service.transport.ServiceDetailsChangeEvent;
import javax.tv.service.transport.ServiceDetailsChangeListener;
import javax.tv.service.transport.TransportStreamChangeEvent;
import javax.tv.service.transport.TransportStreamChangeListener;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.service.NetworkExt;
import org.cablelabs.impl.service.NetworkHandle;
import org.cablelabs.impl.service.RatingDimensionHandle;
import org.cablelabs.impl.service.SIChangedEvent;
import org.cablelabs.impl.service.SIChangedListener;
import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.service.SIDatabase;
import org.cablelabs.impl.service.SIHandle;
import org.cablelabs.impl.service.SINotAvailableException;
import org.cablelabs.impl.service.SIRequestInvalidException;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceComponentHandle;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceDetailsHandle;
import org.cablelabs.impl.service.ServiceHandle;
import org.cablelabs.impl.service.TransportHandle;
import org.cablelabs.impl.service.TransportStreamExt;
import org.cablelabs.impl.service.TransportStreamHandle;
import org.cablelabs.impl.service.javatv.navigation.ServiceComponentImpl;
import org.cablelabs.impl.service.javatv.navigation.ServiceDetailsImpl;
import org.cablelabs.impl.service.javatv.transport.NetworkImpl;
import org.cablelabs.impl.service.javatv.transport.TransportStreamImpl;

/**
 * <p>
 * CannedSIDatabase provides the backbone for the canned testing environment.
 * All canned data comes from the CannedSIDatabase. The tests described below
 * define the more complicated behavior that the CannedSIDatabase will be
 * expected to perform. This test consists of three main parts: <i>Exception
 * Setting Function Testing</i>, <i>Listener Registering Function Testing</i>,
 * and <i>Handle Counter Function Testing</i>. The canned behaviors that this
 * SIDatabase implementation give will also be tested to ensure that we can
 * fully control the behavior of the underlying Datasource as we test components
 * that use it.
 * </p>
 * <p>
 * <i>Exception Setting Function Testing</i><br>
 * This checks the forced exception capabilities of the CannedSIDatabase by
 * setting, throwing, and unsetting different exceptions in CannedSIDatabase.
 * This will be tested as follows:
 * <ol>
 * <li>Set the exception that will be forced.</li>
 * <li>Call a function that throws the exception to be tested.</li>
 * <li>Check that the exception was thrown properly.</li>
 * <li>Unset the exception that was forced.</li>
 * <li>Call the function again to make sure it wasn’t thrown.</li>
 * </ol>
 * </p>
 * <p>
 * <i>Listener Registering Function Testing</i><br>
 * This test checks that the CannedSIDatabase properly notifies the registered
 * listeners. Also removes the listener and tests that it isn’t fired. This will
 * be done as follows:
 * <ol>
 * <li>Add a listener to the CannedSIDatabase.</li>
 * <li>Force an event so the CannedSIDatabase notifies the listeners.</li>
 * <li>Check the listener to see if it was notified.</li>
 * <li>Remove the listener from the CannedSIDatabase.</li>
 * <li>Force an event again so the CannedSIDatabase notifies listeners.</li>
 * <li>Check the listener to make sure it wasn’t notified again.</li>
 * </ol>
 * </p>
 * <p>
 * <i>Handle Counter Function Testing</i><br>
 * This checks the handle counting capability for proper functionality. It tests
 * handle counting for both creations and requests. It also tests to make sure
 * the handle counting is cleared properly. This test is in support of the cache
 * testing to be performed on SICacheImpl. Both “create” and “request” counting
 * will be performed the same way. This test is defined as follows:
 * <ol>
 * <li>Create/request the object from CannedSIDatabase based on the handle.</li>
 * <li>Get the create/request count of the handle from CannedSIDatabase and make
 * sure it matches what is expected.</li>
 * <li>Attempt to get a create/request count for a handle we know hasn’t been
 * asked for and make sure it is zero.</li>
 * <li>Clear the create/request counters.</li>
 * <li>Ask for create/request count for any handle and make sure it is zero.</li>
 *</ol>
 *</p>
 * 
 * @author Joshua Keplinger
 */
public class CannedSIDatabaseTest extends TestCase
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
     * Simple constructor taking only a name.
     * 
     * @param name
     *            The name of this test.
     */
    public CannedSIDatabaseTest(String name)
    {
        super(name);
    }

    /**
     * This simple creates a test suite containing the tests in this class.
     * 
     * @return A TestSuite object containing the tests in this class.
     */
    public static TestSuite suite()
    {
        TestSuite ts = new TestSuite(CannedSIDatabaseTest.class);
        return ts;
    }

    /**
     * Sets up each of the tests
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        listener = new ChangeListenerTest();
        sidb = new CannedSIDatabase();
        sic = new SICacheImpl();
        sidb.setSICache(sic);
        sic.setSIDatabase(sidb);
        sidb.createStaticSI();
        sidb.cannedClearAllForcedExceptions();
        sidb.cannedClearAllHandleCreateCount();
        sidb.cannedClearAllRequestedByCount();
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        cc = ccm.getSystemContext();
    }

    /**
     * Cleans up after the test
     */
    protected void tearDown() throws Exception
    {
        sidb = null;
        sic.destroy();
        sic = null;
        listener = null;

        super.tearDown();
    }

    // Test Section \\

    /**
     * Used to test the abiltity to selectivly turn exceptions on and off. Note
     * this uses very specific domain knowledge of the Canned implementation to
     * make sure we are calling functions with known good data.
     */
    public void testExceptionSettingFunctions()
    {

        CannedSIDatabase canned = (CannedSIDatabase) sidb;
        // test the ability to turn on a simple SIRequestInvalidException
        boolean errorCorrect = false;
        try
        {
            canned.createTransport(canned.transport1.getTransportHandle());
            canned.cannedSetForcedException(SIRequestInvalidException.class);
            canned.createTransport(canned.transport1.getTransportHandle());
            fail("The exception to create for transport 1 was forced and should have been thrown.");
        }
        catch (SIRequestInvalidException e)
        {
            errorCorrect = true;
        }
        assertTrue("The error correct flag should have been set, " + "indicating the correct exception wasn't thrown.",
                errorCorrect);
        errorCorrect = false;
        // now reset the exception and try again
        try
        {
            canned.cannedRemoveForcedException(SIRequestInvalidException.class);
            canned.createTransport(canned.transport1.getTransportHandle());
        }
        catch (SIRequestInvalidException e)
        {
            fail("The exception should not have been thrown in this case, "
                    + "as the forced exception behavior was removed.");
        }
        // test the ability to have a particular exception thrown
        try
        {

            // demonstrate that we can correctly call the method with a given
            // set of args.
            canned.createServiceComponent(canned.serviceComponent69.getServiceComponentHandle());
            // now turn on the SINotAvailableException
            canned.cannedSetForcedException(SINotAvailableException.class);
            // now try again
            canned.createServiceComponent(canned.serviceComponent69.getServiceComponentHandle());
            fail("SINotAvailableException should have been thrown.");
        }
        catch (SIRequestInvalidException e)
        {
            fail("SIRequestInvalidException should not have been thrown.");
        }
        catch (SINotAvailableException e)
        {
            // since we got here we got the right error.
            errorCorrect = true;
        }
        assertTrue("The errorCorrect flag was not correctly set, "
                + "indicating that the right exception wasn't thrown.", errorCorrect);

    }

    /**
     * This tests both <code>addSIAcquiredListener()</code> and
     * <code>removeSIAcquiredListener</code> to make sure the listeners are
     * added, fired, and removed properly.
     */
    public void testSIChangedListener()
    {

        // set up a little listener class.
        class SIChangedListenerTest implements SIChangedListener
        {
            private boolean receivedEvent = false;

            public SIChangedListenerTest(TransportHandle handle)
            {
                if (handle == null) throw new IllegalArgumentException("handle must be non-null");
            }

            public void notifyAcquired(SIChangedEvent event)
            {
                receivedEvent = true;
            }

            public void notifyChanged(SIChangedEvent event)
            {
                receivedEvent = true;
            }

            public boolean wasEventReceived()
            {
                return this.receivedEvent;
            }

            public void setEventReceived(boolean val)
            {
                this.receivedEvent = val;
            }
        }

        class TestChangedEvent extends SIChangedEvent
        {
            public TestChangedEvent(SIDatabase sidb, SIChangeType changeType, TransportHandle handle)
            {
                super(sidb, changeType, handle);
            }
        }

        SIChangedListenerTest listener1 = new SIChangedListenerTest(sidb.transport1.getTransportHandle());
        SIChangeType changeType = SIChangeType.ADD;
        sidb.addSIAcquiredListener(listener1, cc);
        TestChangedEvent testEvent1 = new TestChangedEvent(sidb, changeType, sidb.transport1.getTransportHandle());
        sidb.cannedSendSIAcquired(testEvent1);
        // test that the listener correctly received a notification.
        assertTrue("The event was not fired", listener1.wasEventReceived());
        // now remove the listener
        listener1.setEventReceived(false);
        sidb.removeSIAcquiredListener(listener1, cc);
        sidb.cannedSendSIAcquired(testEvent1);
        assertFalse("The event was fired", listener1.wasEventReceived());

    }

    /**
     * Tests <code>cannedAddSI</code> for proper functionality.
     */
    public void testCannedAddSI() throws Exception
    {
        // Start by adding a listener for each SIElement
        sidb.addNetworkChangeListener(listener, cc);
        sidb.addServiceComponentChangeListener(listener, cc);
        sidb.addServiceDetailsChangeListener(listener, cc);
        sidb.addTransportStreamChangeListener(listener, cc);

        // /////////////////////////// Network \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\

        // Get the current handles for later comparison
        SIHandle[] handles = sidb.getNetworksByTransport(sidb.transport1.getTransportHandle());
        int initialSize = handles.length;
        // Now we'll add a network
        NetworkImpl net = (NetworkImpl) sidb.cannedAddSI(sidb.network3);
        // Check our listener for notification
        assertTrue("NetworkChangeListener was not notified", listener.netChange);
        // Check the results of the change
        assertEquals("Returned network is incorrect", net, listener.netEvent.getNetwork());
        assertEquals("Returned transport is incorrect", sidb.transport1, listener.netEvent.getTransport());
        assertEquals("Returned change type is incorrect", SIChangeType.ADD, listener.netEvent.getChangeType());
        // Now we'll check the list to make sure it was added correctly
        assertTrue("Network was not added to list", sidb.networks.contains(net));
        // Check the mapping as well
        handles = sidb.getNetworksByTransport(sidb.transport1.getTransportHandle());
        assertEquals("Second array length is not one greater than first array length", initialSize + 1, handles.length);

        // //////////////////////// ServiceComponent \\\\\\\\\\\\\\\\\\\\\\\\\\\

        // Get the current handles for later comparison
        handles = sidb.getServiceComponentsByServiceDetails(sidb.serviceDetails33.getServiceDetailsHandle());
        initialSize = handles.length;
        listener.reset();
        // Now we're going to add a ServiceComponent
        ServiceComponentImpl sc = (ServiceComponentImpl) sidb.cannedAddSI(sidb.serviceComponent69);
        // Check our listener for notification
        assertTrue("ServiceComponentChangeListener was not notified", listener.scChange);
        // Check the results of the change
        assertEquals("Returned ServiceComponent is incorrect", sc, listener.scEvent.getServiceComponent());
        assertEquals("Returned ServiceDetails is incorrect", sidb.serviceDetails33,
                listener.scEvent.getServiceDetails());
        assertEquals("Returned change type is incorrect", SIChangeType.ADD, listener.scEvent.getChangeType());
        // Now we'll check the list to make sure it was added correctly
        assertTrue("ServiceDetails was not added to list", sidb.serviceComponentsList.contains(sc));
        // Check the mapping as well
        handles = sidb.getServiceComponentsByServiceDetails(sidb.serviceDetails33.getServiceDetailsHandle());
        assertEquals("Second array length is not one greater than first array length", initialSize + 1, handles.length);

        // //////////////////////// ServiceDetails \\\\\\\\\\\\\\\\\\\\\\\\\\\\\

        // Get the current handles for later comparison
        handles = sidb.getServiceDetailsByService(sidb.service15.getServiceHandle());
        initialSize = handles.length;
        listener.reset();
        // Now we're going to add a ServiceDetails object
        ServiceDetailsImpl sd = (ServiceDetailsImpl) sidb.cannedAddSI(sidb.serviceDetails33);
        // Check our listener for notification
        assertTrue("ServiceDetailsChangeListener was not notified", listener.sdChange);
        // Check the results of the change
        assertEquals("Returned ServiceDetails is incorrect", sd, listener.sdEvent.getServiceDetails());
        assertEquals("Returned Transport is incorrect", sidb.transport1, listener.sdEvent.getTransport());
        assertEquals("Returned change type is incorrect", SIChangeType.ADD, listener.sdEvent.getChangeType());
        // Now we'll check the list to make sure it was added correctly
        assertTrue("ServiceDetails was not added to list", sidb.serviceDetailsList.contains(sd));
        // Check the mapping as well
        handles = sidb.getServiceDetailsByService(sidb.service15.getServiceHandle());
        assertEquals("Second array length is not one greater than first array length", initialSize + 1, handles.length);

        // ///////////////////////// TransportStream \\\\\\\\\\\\\\\\\\\\\\\\\\\

        // Get the current handles for later comparison
        handles = sidb.getTransportStreamsByTransport(sidb.transport1.getTransportHandle());
        initialSize = handles.length;
        // Now we're going to add a ServiceComponent
        TransportStreamImpl ts = (TransportStreamImpl) sidb.cannedAddSI(sidb.transportStream7);
        // Check our listener for notification
        assertTrue("TransportStreamChangeListener was not notified", listener.tsChange);
        // Check the results of the change
        assertEquals("Returned TransportStream is incorrect", ts, listener.tsEvent.getTransportStream());
        assertEquals("Returned Transport is incorrect", sidb.transport1, listener.tsEvent.getTransport());
        assertEquals("Returned change type is incorrect", SIChangeType.ADD, listener.tsEvent.getChangeType());
        // Now we'll check the list to make sure it was added correctly
        assertTrue("ServiceDetails was not added to list", sidb.transportStreams.contains(ts));
        // Check the mapping as well
        handles = sidb.getTransportStreamsByTransport(sidb.transport1.getTransportHandle());
        assertEquals("Second array length is not one greater than first array length", initialSize + 1, handles.length);

        listener.reset();
        // Finally we'll remove the listener from each type
        sidb.removeNetworkChangeListener(listener, cc);
        sidb.removeServiceComponentChangeListener(listener, cc);
        sidb.removeServiceDetailsChangeListener(listener, cc);
        sidb.removeTransportStreamChangeListener(listener, cc);

        // We'll fire events for each one and make sure they aren't notified
        sidb.cannedAddSI(sidb.network6);
        assertFalse("NetworkChangeListener was falsely notified", listener.netChange);
        sidb.cannedAddSI(sidb.serviceComponent105);
        assertFalse("ServiceComponentChangeListener was falsely notified", listener.scChange);
        sidb.cannedAddSI(sidb.serviceDetails33);
        assertFalse("ServiceDetailsChangeListener was falsely notified", listener.sdChange);
        sidb.cannedAddSI(sidb.transportStream10);
        assertFalse("TransportStreamChangeListener was falsely notified", listener.tsChange);
    }

    /**
     * Tests <code>cannedUpdateSI</code> for proper functionality.
     */
    public void testCannedUpdateSI() throws Exception
    {
        // Start by adding a listener for each SIElement
        sidb.addNetworkChangeListener(listener, cc);
        sidb.addServiceComponentChangeListener(listener, cc);
        sidb.addServiceDetailsChangeListener(listener, cc);
        sidb.addTransportStreamChangeListener(listener, cc);

        // ///////////////////////////// Network \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\

        // Now we'll add a network
        NetworkImpl net = (NetworkImpl) sidb.cannedUpdateSI(sidb.network3);
        // Check our listener for notification
        assertTrue("NetworkChangeListener was not notified", listener.netChange);
        // Check the results of the change
        assertEquals("Returned network is incorrect", net, listener.netEvent.getNetwork());
        assertEquals("Returned transport is incorrect", sidb.transport1, listener.netEvent.getTransport());
        assertEquals("Returned change type is incorrect", SIChangeType.MODIFY, listener.netEvent.getChangeType());
        // Now we'll check the list to make sure it was modified correctly
        assertTrue("Network was not updated in list", sidb.networks.contains(net));
        // Let's check the original object and make sure it's not in there
        // anymore
        assertFalse("Old network should not be in list", sidb.networks.contains(sidb.network3));
        // Check the mapping as well
        NetworkExt sameNet = sidb.createNetwork(net.getNetworkHandle());
        assertEquals("Network returned is incorrect for supplied handle", net, sameNet);

        // ///////////////////////// ServiceComponent \\\\\\\\\\\\\\\\\\\\\\\\\\

        // Now we're going to add a ServiceComponent
        ServiceComponentImpl sc = (ServiceComponentImpl) sidb.cannedUpdateSI(sidb.serviceComponent69);
        // Check our listener for notification
        assertTrue("ServiceComponentChangeListener was not notified", listener.scChange);
        // Check the results of the change
        assertEquals("Returned ServiceComponent is incorrect", sc, listener.scEvent.getServiceComponent());
        assertEquals("Returned ServiceDetails is incorrect", sidb.serviceDetails33,
                listener.scEvent.getServiceDetails());
        assertEquals("Returned change type is incorrect", SIChangeType.MODIFY, listener.scEvent.getChangeType());
        // Now we'll check the list to make sure it was modified correctly
        assertTrue("ServiceDetails was not updated in list", sidb.serviceComponentsList.contains(sc));
        // Let's check the original object and make sure it's not in there
        // anymore
        assertFalse("Old service component should not be in list",
                sidb.serviceComponentsList.contains(sidb.serviceComponent69));
        // Check the mapping as well
        ServiceComponentExt sameSC = sidb.createServiceComponent(sc.getServiceComponentHandle());
        assertEquals("ServiceComponent returned is incorrect for supplied handle", sc, sameSC);

        // ///////////////////////// ServiceDetails \\\\\\\\\\\\\\\\\\\\\\\\\\\\

        // Now we're going to add a ServiceDetails object
        ServiceDetailsImpl sd = (ServiceDetailsImpl) sidb.cannedUpdateSI(sidb.serviceDetails33);
        // Check our listener for notification
        assertTrue("ServiceDetailsChangeListener was not notified", listener.sdChange);
        // Check the results of the change
        assertEquals("Returned ServiceDetails is incorrect", sd, listener.sdEvent.getServiceDetails());
        assertEquals("Returned Transport is incorrect", sidb.transport1, listener.sdEvent.getTransport());
        assertEquals("Returned change type is incorrect", SIChangeType.MODIFY, listener.scEvent.getChangeType());
        // Now we'll check the list to make sure it was added correctly
        assertTrue("ServiceDetails was not updated in list", sidb.serviceDetailsList.contains(sd));
        // Let's check the original object and make sure it's not in there
        // anymore
        assertFalse("Old service details should not be in list",
                sidb.serviceDetailsList.contains(sidb.serviceComponent69));
        // Check the mapping as well
        ServiceDetailsExt sameSD = sidb.createServiceDetails(sd.getServiceDetailsHandle());
        assertEquals("ServiceDetails returned is incorrect for supplied handle", sd, sameSD);

        // ///////////////////////// TransportStream \\\\\\\\\\\\\\\\\\\\\\\\\\\

        // Now we're going to add a ServiceComponent
        TransportStreamImpl ts = (TransportStreamImpl) sidb.cannedUpdateSI(sidb.transportStream7);
        // Check our listener for notification
        assertTrue("TransportStreamChangeListener was not notified", listener.tsChange);
        // Check the results of the change
        assertEquals("Returned TransportStream is incorrect", ts, listener.tsEvent.getTransportStream());
        assertEquals("Returned Transport is incorrect", sidb.transport1, listener.tsEvent.getTransport());
        assertEquals("Returned change type is incorrect", SIChangeType.MODIFY, listener.tsEvent.getChangeType());
        // Now we'll check the list to make sure it was added correctly
        assertTrue("ServiceDetails was not added to list", sidb.transportStreams.contains(ts));
        // Let's check the original object and make sure it's not in there
        // anymore
        assertFalse("Old service details should not be in list", sidb.transportStreams.contains(sidb.transportStream7));
        // Check the mapping as well
        TransportStreamExt sameTS = sidb.createTransportStream(ts.getTransportStreamHandle());
        assertEquals("TransportStream returned is incorrect for supplied handle", ts, sameTS);

        listener.reset();
        // Finally we'll remove the listener from each type
        sidb.removeNetworkChangeListener(listener, cc);
        sidb.removeServiceComponentChangeListener(listener, cc);
        sidb.removeServiceDetailsChangeListener(listener, cc);
        sidb.removeTransportStreamChangeListener(listener, cc);

        // We'll fire events for each one and make sure they aren't notified
        sidb.cannedUpdateSI(sidb.network6);
        assertFalse("NetworkChangeListener was falsely notified", listener.netChange);
        sidb.cannedUpdateSI(sidb.serviceComponent105);
        assertFalse("ServiceComponentChangeListener was falsely notified", listener.scChange);
        sidb.cannedUpdateSI(sidb.serviceDetails33);
        assertFalse("ServiceDetailsChangeListener was falsely notified", listener.sdChange);
        sidb.cannedUpdateSI(sidb.transportStream10);
        assertFalse("TransportStreamChangeListener was falsely notified", listener.tsChange);
    }

    /**
     * Tests <code>cannedRemoveSI</code> for proper functionality.
     */
    public void testCannedRemoveSI() throws Exception
    {
        // Start by adding a listener for each SIElement
        sidb.addNetworkChangeListener(listener, cc);
        sidb.addServiceComponentChangeListener(listener, cc);
        sidb.addServiceDetailsChangeListener(listener, cc);
        sidb.addTransportStreamChangeListener(listener, cc);

        // /////////////////////////// Network \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\

        // Get the current handles for later comparison
        SIHandle[] handles = sidb.getNetworksByTransport(sidb.transport1.getTransportHandle());
        int initialSize = handles.length;
        // Now we'll add a network
        sidb.cannedRemoveSI(sidb.network3);
        // Check our listener for notification
        assertTrue("NetworkChangeListener was not notified", listener.netChange);
        // Check the results of the change
        assertEquals("Returned network is incorrect", sidb.network3, listener.netEvent.getNetwork());
        assertEquals("Returned transport is incorrect", sidb.transport1, listener.netEvent.getTransport());
        assertEquals("Returned change type is incorrect", SIChangeType.REMOVE, listener.netEvent.getChangeType());
        // Now we'll check the list to make sure it was removed correctly
        assertFalse("Network was not removed from list", sidb.networks.contains(sidb.network3));
        // Check the mapping as well
        handles = sidb.getNetworksByTransport(sidb.transport1.getTransportHandle());
        assertEquals("Second array length is not one less than first array length", initialSize - 1, handles.length);

        // //////////////////////// ServiceComponent \\\\\\\\\\\\\\\\\\\\\\\\\\\

        // Get the current handles for later comparison
        handles = sidb.getServiceComponentsByServiceDetails(sidb.serviceDetails33.getServiceDetailsHandle());
        initialSize = handles.length;
        // Now we're going to add a ServiceComponent
        sidb.cannedRemoveSI(sidb.serviceComponent69);
        // Check our listener for notification
        assertTrue("ServiceComponentChangeListener was not notified", listener.scChange);
        // Check the results of the change
        assertEquals("Returned ServiceComponent is incorrect", sidb.serviceComponent69,
                listener.scEvent.getServiceComponent());
        assertEquals("Returned ServiceDetails is incorrect", sidb.serviceDetails33,
                listener.scEvent.getServiceDetails());
        assertEquals("Returned change type is incorrect", SIChangeType.REMOVE, listener.scEvent.getChangeType());
        // Now we'll check the list to make sure it was removed correctly
        assertFalse("ServiceDetails was not removed from list",
                sidb.serviceComponentsList.contains(sidb.serviceComponent69));
        // Check the mapping as well
        handles = sidb.getServiceComponentsByServiceDetails(sidb.serviceDetails33.getServiceDetailsHandle());
        assertEquals("Second array length is not one less than first array length", initialSize - 1, handles.length);

        // //////////////////////// ServiceDetails \\\\\\\\\\\\\\\\\\\\\\\\\\\\\

        // Get the current handles for later comparison
        handles = sidb.getServiceDetailsByService(sidb.service15.getServiceHandle());
        initialSize = handles.length;
        // Now we're going to add a ServiceDetails object
        sidb.cannedRemoveSI(sidb.serviceDetails33);
        // Check our listener for notification
        assertTrue("ServiceDetailsChangeListener was not notified", listener.sdChange);
        // Check the results of the change
        assertEquals("Returned ServiceDetails is incorrect", sidb.serviceDetails33,
                listener.sdEvent.getServiceDetails());
        assertEquals("Returned Transport is incorrect", sidb.transport1, listener.sdEvent.getTransport());
        assertEquals("Returned change type is incorrect", SIChangeType.REMOVE, listener.scEvent.getChangeType());
        // Now we'll check the list to make sure it was added correctly
        assertFalse("ServiceDetails was not removed from list", sidb.serviceDetailsList.contains(sidb.serviceDetails33));
        // Check the mapping as well
        handles = sidb.getServiceDetailsByService(sidb.service15.getServiceHandle());
        assertEquals("Second array length is not one less than first array length", initialSize - 1, handles.length);

        // ///////////////////////// TransportStream \\\\\\\\\\\\\\\\\\\\\\\\\\\

        // Get the current handles for later comparison
        handles = sidb.getTransportStreamsByTransport(sidb.transport1.getTransportHandle());
        initialSize = handles.length;
        // Now we're going to add a ServiceComponent
        sidb.cannedRemoveSI(sidb.transportStream7);
        // Check our listener for notification
        assertTrue("TransportStreamChangeListener was not notified", listener.tsChange);
        // Check the results of the change
        assertEquals("Returned TransportStream is incorrect", sidb.transportStream7,
                listener.tsEvent.getTransportStream());
        assertEquals("Returned Transport is incorrect", sidb.transport1, listener.tsEvent.getTransport());
        assertEquals("Returned change type is incorrect", SIChangeType.REMOVE, listener.tsEvent.getChangeType());
        // Now we'll check the list to make sure it was added correctly
        assertFalse("TransportStream was not removed from list", sidb.transportStreams.contains(sidb.transportStream7));
        // Check the mapping as well
        handles = sidb.getTransportStreamsByTransport(sidb.transport1.getTransportHandle());
        assertEquals("Second array length is not one greater than first array length", initialSize - 1, handles.length);

        listener.reset();
        // Finally we'll remove the listener from each type
        sidb.removeNetworkChangeListener(listener, cc);
        sidb.removeServiceComponentChangeListener(listener, cc);
        sidb.removeServiceDetailsChangeListener(listener, cc);
        sidb.removeTransportStreamChangeListener(listener, cc);

        // We'll fire events for each one and make sure they aren't notified
        sidb.cannedAddSI(sidb.network6);
        assertFalse("NetworkChangeListener was falsely notified", listener.netChange);
        sidb.cannedAddSI(sidb.serviceComponent105);
        assertFalse("ServiceComponentChangeListener was falsely notified", listener.scChange);
        sidb.cannedAddSI(sidb.serviceDetails33);
        assertFalse("ServiceDetailsChangeListener was falsely notified", listener.sdChange);
        sidb.cannedAddSI(sidb.transportStream10);
        assertFalse("TransportStreamChangeListener was falsely notified", listener.tsChange);
    }

    /**
     * Designed to test the <code>CannedSIDatabase</code>'s internal counters
     * that are used to track the number of times objects are requested. Note
     * that currently this test is not exhaustive.
     */
    public void testHandleCreateCounter() throws Exception
    {

        // we will test all of the create methods for correct counting behavior.

        // network
        NetworkHandle n6hdl = sidb.network6.getNetworkHandle();
        sidb.createNetwork(n6hdl);
        assertEquals("The handleCreateCount for networks was wrong", 1, sidb.cannedGetHandleCreateCount(n6hdl));

        RatingDimensionHandle rdh = sidb.ratingDimension1.getRatingDimensionHandle();
        sidb.createRatingDimension(rdh);
        assertEquals("The handleCreateCount for rating dimension was wrong", 1, sidb.cannedGetHandleCreateCount(rdh));

        // service
        ServiceHandle s15hdl = sidb.service15.getServiceHandle();
        sidb.createService(s15hdl);
        sidb.createService(s15hdl);
        assertEquals("The handleCreateCount was incorrect", 2, sidb.cannedGetHandleCreateCount(s15hdl));
        ServiceHandle s17hdl = sidb.service17.getServiceHandle();
        sidb.createService(s17hdl);
        sidb.createService(s17hdl);
        sidb.createService(s17hdl);
        assertEquals("The handleCreateCount was wrong", 3, sidb.cannedGetHandleCreateCount(s17hdl));

        // service component
        ServiceComponentHandle sc69 = sidb.serviceComponent69.getServiceComponentHandle();
        sidb.createServiceComponent(sc69);
        sidb.createServiceComponent(sc69);
        assertEquals("The handleCreateCount was wrong", 2, sidb.cannedGetHandleCreateCount(sc69));

        // service description - note this actually uses one of the
        // serviceDetails handles.
        sidb.createServiceDescription(sidb.serviceDetails34.getServiceDetailsHandle());
        assertEquals("The handleCreateCount was wrong", 1,
                sidb.cannedGetHandleCreateCount(sidb.serviceDetails34.getServiceDetailsHandle()));

        // service details note we'll use the same serviceDetailsHandle
        ServiceDetailsHandle sd34 = sidb.serviceDetails34.getServiceDetailsHandle();
        sidb.createServiceDetails(sd34);
        assertEquals("The handleCreateCount was wrong for serviceDetails", 2, sidb.cannedGetHandleCreateCount(sd34));
        ServiceDetailsHandle sd50 = sidb.serviceDetails50.getServiceDetailsHandle();
        sidb.createServiceDetails(sd50);
        assertEquals("The handleCreateCount was wrong for serviceDetails", 1, sidb.cannedGetHandleCreateCount(sd50));

        // transport
        TransportHandle tr1 = sidb.transport1.getTransportHandle();
        sidb.createTransport(tr1);
        sidb.createTransport(tr1);
        assertEquals("The handleCreateCount was wrong for transport", 2, sidb.cannedGetHandleCreateCount(tr1));

        // transportStream
        TransportStreamHandle ts8 = sidb.transportStream8.getTransportStreamHandle();
        sidb.createTransportStream(ts8);
        assertEquals("the handleCreateCount was wrong for transportStream", 1, sidb.cannedGetHandleCreateCount(ts8));
        sidb.createTransportStream(ts8);
        assertEquals("the handleCreateCount was wrong for transportStream", 2, sidb.cannedGetHandleCreateCount(ts8));

        // lets look for some we know don't exist
        assertEquals("the empty count was wrong", 0, sidb.cannedGetHandleCreateCount(new Integer(300)));

        // clear the create count and test that it was cleared.
        // note - we know the canned version uses Map.clear(),
        // so we only test 2, and that's a bit redundant.
        sidb.cannedClearAllHandleCreateCount();
        assertEquals("The handleCreateCount was not correctly cleared.", 0, sidb.cannedGetHandleCreateCount(s15hdl));
        assertEquals("The handleCreateCount was not correctly cleared", 0, sidb.cannedGetHandleCreateCount(n6hdl));
    }

    /**
     * This test is designed to test the <code>CannedSIDatabase</code>'s
     * internal tracking of the number of times a particular handle is used as
     * an argument to a request.
     */
    public void testHandleRequestCounter() throws Exception
    {

        // this test attempts to exhaustively test the counting of request
        // counts
        // to the CannedSIDatabase.
        TransportHandle t1 = sidb.transport1.getTransportHandle();
        sidb.getNetworksByTransport(t1);
        assertEquals("bad request count getNetworksByTransport", 1, sidb.cannedGetHandleRequestedByCount(t1));

        // by program number
        sidb.getServiceByProgramNumber(5000, 2, 1);
        sidb.getServiceByProgramNumber(5000, 2, 1);
        // note this assertion makes use of particular internal information on
        // how the counting is done,
        // namely on the order to append the ints to make the object key that
        // was used for tracking.
        assertEquals("bad request count getServiceByProgramNumber", 2,
                sidb.cannedGetHandleRequestedByCount("" + 5000 + 2));
        sidb.getServiceByProgramNumber(5500, 4, 3);
        assertEquals("bad request count getServiceByProgramNumber", 1,
                sidb.cannedGetHandleRequestedByCount("" + 5500 + 4));

        // by service name
        sidb.getServiceByServiceName("testService1");
        assertEquals("bad request count getServiceByServiceName", 1,
                sidb.cannedGetHandleRequestedByCount("testService1"));
        sidb.getServiceByServiceName("testService2");
        assertEquals("bad request count getServiceByServiceName", 1,
                sidb.cannedGetHandleRequestedByCount("testService2"));

        // sourceId
        sidb.getServiceBySourceID(103);
        sidb.getServiceBySourceID(103);
        sidb.getServiceBySourceID(103);
        assertEquals("bad request count getServiceBySourceId", 3,
                sidb.cannedGetHandleRequestedByCount(new Integer(103)));

        // test the count clearing in the midst of it.
        sidb.cannedFullReset();
        assertEquals("bad request count getServiceBySourceId", 0,
                sidb.cannedGetHandleRequestedByCount(new Integer(103)));

        // service component by name and service details handle. Note we know
        // how to create the correct key used for tracking in
        // this case as well.
        // tests at the bounds too.
        ServiceDetailsHandle sd33 = sidb.serviceDetails33.getServiceDetailsHandle();
        sidb.getServiceComponentByName(sd33, "serviceComponent69");
        assertEquals("bad request count getServiceBySourceId", 1, sidb.cannedGetHandleRequestedByCount(sd33
                + "serviceComponent69"));
        ServiceDetailsHandle sd50 = sidb.serviceDetails50.getServiceDetailsHandle();
        sidb.getServiceComponentByName(sd50, "serviceComponent122");
        assertEquals("bad request count getServiceComponentByName", 1, sidb.cannedGetHandleRequestedByCount(sd50
                + "serviceComponent122"));

        // serviceComponentByPid - note we are reusing the service details from
        // above so we'll reset the db
        sidb.cannedFullReset();
        sidb.getServiceComponentByPID(sd33, 1);
        assertEquals("bad request count getServiceComponentByPid", 1,
                sidb.cannedGetHandleRequestedByCount(sd33.toString() + 1));

        // service component by tag - note we are reusing the service details
        // from above so we'll reset the db
        sidb.cannedFullReset();
        sidb.getServiceComponentByTag(sd33, 1);
        assertEquals("bad request count getServiceComponentByTag", 1,
                sidb.cannedGetHandleRequestedByCount(sd33.toString() + 1));

        // serviceComponentByServiceDetails
        sidb.cannedFullReset();
        sidb.getServiceComponentsByServiceDetails(sd33);
        assertEquals("bad request count getServiceComponentsByServiceDetails", 1,
                sidb.cannedGetHandleRequestedByCount(sd33));
        // check for one we haven't looked for
        assertEquals("bad request count getServiceComponentsByServiceDetails", 0,
                sidb.cannedGetHandleRequestedByCount(new Integer(35)));

        // serviceDetailsByService
        ServiceHandle s15 = sidb.service15.getServiceHandle();
        sidb.getServiceDetailsByService(s15);
        sidb.getServiceDetailsByService(s15);
        sidb.getServiceDetailsByService(s15);
        assertEquals("bad request count getServiceDetailsByService", 3, sidb.cannedGetHandleRequestedByCount(s15));

        // TODO: test the counting of supported dimensions - is this a cached
        // thing?
        // transportStreamsByNetwork
        NetworkHandle n4 = sidb.network4.getNetworkHandle();
        sidb.getTransportStreamsByNetwork(n4);
        assertEquals("bad request count getTransportStreamsbyNetwork", 1, sidb.cannedGetHandleRequestedByCount(n4));

        // transportStreamsByTransport
        TransportHandle t2 = sidb.transport2.getTransportHandle();
        sidb.getTransportStreamsByTransport(t2);
        assertEquals("bad request count getTransportStreamsByTransport", 1, sidb.cannedGetHandleRequestedByCount(t2));

    }

    /**
     * Holds the instance of the CannedSIDatabase object we are testing.
     */
    private CannedSIDatabase sidb;

    private SICache sic;

    private ChangeListenerTest listener;

    private CallerContext cc;

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

        public void notifyChange(TransportStreamChangeEvent event)
        {
            tsEvent = event;
            eventReceived++;
            tsChange = true;
        }

        public void notifyChange(ServiceComponentChangeEvent event)
        {
            scEvent = event;
            eventReceived++;
            scChange = true;
        }

        public void notifyChange(ServiceDetailsChangeEvent event)
        {
            sdEvent = event;
            eventReceived++;
            sdChange = true;
        }

        public void notifyChange(NetworkChangeEvent event)
        {
            netEvent = event;
            eventReceived++;
            netChange = true;
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

        public synchronized void waitForEvents(int numEvents, long millis) throws InterruptedException
        {
            for (int i = 0; i < millis / 100; i++)
            {
                if (eventReceived >= numEvents) break;
                Thread.sleep(100);
            }
        }
    }
}
