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

import java.util.Date;

import javax.tv.locator.Locator;
import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.SIRetrievable;
import javax.tv.service.ServiceInformationType;
import javax.tv.service.transport.TransportStream;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.cablelabs.impl.manager.service.CannedSIDatabase.NetworkHandleImpl;
import org.cablelabs.test.SICannedConcreteTest;

/**
 * <p>
 * CannedNetworkExtTest tests the methods presented by the NetworkExt interface.
 * This test is performed on the a real implementation of the NetworkExt
 * interface, but in a canned testing environment, using controlled data and
 * behavior. It is important to not that this does not test any functionality
 * outside what the NetworkExt interface provides.
 * </p>
 * <p>
 * Most of the methods exposed by NetworkExt are getter methods. These tests are
 * simple and time will not be spent describing them in detail. Do note,
 * however, that these methods are thoroughly tested for proper functionality.
 * The one test that will be described below is the <i>Retrieval Method
 * Test</i>.
 * </p>
 * <p>
 * <i>Retrieval Method Test</i><br>
 * This tests the asynchronous retrieval of service information from the
 * NetworkExt object. This is as follows:
 * <ol>
 * <li>Send the request for service information to TransportExt.</li>
 * <li>Wait a short period of time to allow listener to be called.</li>
 * <li>Chcek listener to see if it was properly notified within the time limit.</li>
 * <li>Get the retrieved objects from the listener and check for validity.</li>
 * </ol>
 * </p>
 * 
 * @author Joshua Keplinger
 */
public class NetworkImplCannedTest extends SICannedConcreteTest
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
    public NetworkImplCannedTest()
    {
        super("NetworkImplCannedTest");
    }

    /**
     * Creates a test suite to for use in the testing environment.
     * 
     * @return An InterfaceTestSuite that contains the tests in this test case.
     */
    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(NetworkImplCannedTest.class);
        suite.setName(NetworkImpl.class.getName());
        return suite;
    }

    /**
     * Sets up our test
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        handle = new NetworkHandleImpl(5);
        id = 5;
        name = "networkOne";
        infoType = ServiceInformationType.ATSC_PSIP;
        now = new Date();
        network = new NetworkImpl(sic, handle, csidb.transport1, id, name, infoType, now, null);
    }

    /**
     * Clean up after the test
     */
    protected void tearDown() throws Exception
    {
        network = null;
        handle = null;
        name = null;
        infoType = null;
        now = null;

        super.tearDown();
    }

    // Test Section \\

    /**
     * Tests the constructor and the associated getter methods for proper
     * setting and retrieval of data.
     */
    public void testConstructor()
    {
        assertEquals("Handle does not match", handle, network.getNetworkHandle());
        assertEquals("NetworkID does not match", id, network.getNetworkID());
        assertEquals("Name does not match", name, network.getName());
        assertEquals("ServiceInformationType does not match", infoType, network.getServiceInformationType());
        assertEquals("Update time does not match", now, network.getUpdateTime());
        assertEquals("Transport does not match", csidb.transport1, network.getTransport());
    }

    /**
     * Tests <code>getLocator</code> to make sure it returns a locator that
     * correctly corresponds to this network
     */
    public void testGetLocator()
    {
        assertTrue("Value returned was not a valid Locator", network.getLocator() instanceof Locator);
        assertEquals("Returned locators do not match.", network.getLocator(), network.getLocator());
    }

    /**
     * Tests <code>equals</code> to make sure it correctly compares to exact
     * same transport stream objects.
     */
    public void testEquals()
    {
        assertTrue("Equals should succeed when testing object against itself.", network.equals(network));
        // Test equals with a different handle
        NetworkHandleImpl diffHandle = new NetworkHandleImpl(99);
        NetworkImpl network2 = new NetworkImpl(sic, diffHandle, csidb.transport1, id, name, infoType, now, null);
        assertFalse("Equals should fail with different handle", network.equals(network2));
        // Test equals with different transport
        network2 = new NetworkImpl(sic, handle, csidb.transport2, id, name, infoType, now, null);
        assertFalse("Equals should fail with different transport", network.equals(network2));
        // Test equals with different network id
        network2 = new NetworkImpl(sic, handle, csidb.transport1, 78, name, infoType, now, null);
        assertFalse("Equals should fail with different networkID", network.equals(network2));
        // Test equals with different name
        network2 = new NetworkImpl(sic, handle, csidb.transport1, id, "foo", infoType, now, null);
        assertFalse("Equals should fail with different name", network.equals(network2));
        // Test equals with different service information type
        network2 = new NetworkImpl(sic, handle, csidb.transport1, id, name, ServiceInformationType.SCTE_SI, now, null);
        assertFalse("Equals should fail with different service information type", network.equals(network2));
        // Test equals with different update time
        network2 = new NetworkImpl(sic, handle, csidb.transport1, id, name, infoType, new Date(
                System.currentTimeMillis() + 5555), null);
        assertFalse("Equals should fail with different update time", network.equals(network2));
    }

    /**
     * Tests <code>hashCode</code> to make sure it returns a proper hash code.
     */
    public void testHashCode()
    {
        assertEquals("Hashcode does not match on same object.", network.hashCode(), network.hashCode());
        /*
         * NetworkImpl network2 = new NetworkImpl(handle, id, name, infoType,
         * now);
         * assertFalse("Hashcode should not be the same with different objects."
         * , network.hashCode() == network2.hashCode());
         */
    }

    /**
     * Tests <code>retrieveTransportStreams</code> to make sure it returns the
     * right transport streams that are associated with this network. This is
     * done asynchronously to simulate a real data retrieval.
     */
    public void testRetrieveTransportStreams() throws Exception
    {
        SIRequestorTest requestor = new SIRequestorTest();
        csidb.network3.retrieveTransportStreams(requestor);
        requestor.waitForEvents(1, waitForRequest);
        assertTrue("Requestor was not notified of success", requestor.succeeded());
        SIRetrievable[] results = requestor.getResults();
        assertTrue("Results returned were not of the right type", results[0] instanceof TransportStream);
    }

    // Data Section \\

    /**
     * Holds the instance of the NetworkExt object we are testing.
     */
    private NetworkImpl network;

    private NetworkHandleImpl handle;

    private int id;

    private String name;

    private ServiceInformationType infoType;

    private Date now;

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
