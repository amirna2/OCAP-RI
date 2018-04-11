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

package org.cablelabs.impl.manager.service;

import java.util.Vector;

import javax.tv.locator.Locator;
import javax.tv.service.navigation.ServiceComponent;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.transport.Network;
import javax.tv.service.transport.Transport;
import javax.tv.service.transport.TransportStream;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.TransportExt;
import org.cablelabs.impl.service.TransportStreamExt;
import org.cablelabs.test.SICannedConcreteTest;

public class SISnapshotManagerCannedTest extends SICannedConcreteTest
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

    /**
     * No-arg constructor for creating our test case.
     * 
     */
    public SISnapshotManagerCannedTest()
    {
        super("SISnapshotManagerCannedTest");
    }

    /**
     * Creates a test suite to for use in the testing environment.
     * 
     * @return An InterfaceTestSuite that contains the tests in this test case.
     */
    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(SISnapshotManagerCannedTest.class);
        suite.setName(SISnapshotManager.class.getName());
        return suite;
    }

    /**
     * Sets up our test
     */
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    /**
     * Clean up after the test
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    /**
     * Tests a snapshot containing a single service
     */
    public void testSingleService() throws Exception
    {
        // Create the snapshot
        Locator[] locators = new Locator[] { csidb.service15.getLocator() };
        SISnapshotManager snapshot = new SISnapshotManager(locators);

        // Build a collection of objects that represents what should be in the
        // snapshot
        ExpectedVector expectedObjects = new ExpectedVector();
        expectedObjects.add(csidb.transport1);
        expectedObjects.add(csidb.network3);
        expectedObjects.add(csidb.transportStream7);
        expectedObjects.add(csidb.service15);
        expectedObjects.add(csidb.serviceDetails33);
        expectedObjects.add(csidb.serviceDescription33);
        expectedObjects.add(csidb.serviceComponent69);
        expectedObjects.add(csidb.serviceComponent69eng);
        expectedObjects.add(csidb.serviceComponent69fre);
        expectedObjects.add(csidb.serviceComponent69spa);
        expectedObjects.add(csidb.serviceComponent105);

        // Verify the snapshot contains exactly the expected set
        verifyObjects(snapshot, expectedObjects);
    }

    /**
     * Tests a snapshot containing a two services in the same transport stream
     */
    public void testTwoServicesInOneTransportStream() throws Exception
    {
        // Create the snapshot
        Locator[] locators = new Locator[] { csidb.service15.getLocator(), csidb.service23.getLocator() };
        SISnapshotManager snapshot = new SISnapshotManager(locators);

        // Build a collection of objects that represents what should be in the
        // snapshot
        ExpectedVector expectedObjects = new ExpectedVector();
        expectedObjects.add(csidb.transport1);
        expectedObjects.add(csidb.network3);
        expectedObjects.add(csidb.transportStream7);
        expectedObjects.add(csidb.service15);
        expectedObjects.add(csidb.serviceDetails33);
        expectedObjects.add(csidb.serviceDescription33);
        expectedObjects.add(csidb.serviceComponent69);
        expectedObjects.add(csidb.serviceComponent69eng);
        expectedObjects.add(csidb.serviceComponent69fre);
        expectedObjects.add(csidb.serviceComponent69spa);
        expectedObjects.add(csidb.serviceComponent105);
        expectedObjects.add(csidb.service23);
        expectedObjects.add(csidb.serviceDetails41);
        expectedObjects.add(csidb.serviceDescription41);
        expectedObjects.add(csidb.serviceComponent77);
        expectedObjects.add(csidb.serviceComponent113);

        // Verify the snapshot contains exactly the expected set
        verifyObjects(snapshot, expectedObjects);
    }

    // TODO(Todd): Test 2 services in same transport
    // TODO(Todd): Test 2 services with nothing in common
    // TODO(Todd): Test SI not available

    /**
     * Verify the objects in the snapshot match the expected set of objects.
     * 
     * @param snapshot
     *            The snapshot manager
     * @param expectedObjects
     *            The SI objects expected to be in the snapshot
     */
    private void verifyObjects(SISnapshotManager snapshot, Vector expectedObjects) throws Exception
    {
        // Work on a copy of the collection of expected objects
        Vector expected = (Vector) expectedObjects.clone();

        // Walk the snapshot and remove each object we find from the expected
        // set.
        // If we find an object not in the expected set, then assert.
        Transport[] tarray = snapshot.getTransports();
        for (int t = 0; t < tarray.length; t++)
        {
            expected.remove(tarray[t]);
            Network[] narray = ((TransportExt) tarray[t]).getNetworks();
            for (int n = 0; n < narray.length; n++)
            {
                expected.remove(narray[n]);
            }
            TransportStream[] tsarray = ((TransportExt) tarray[t]).getTransportStreams();
            for (int ts = 0; ts < tsarray.length; ts++)
            {
                expected.remove(tsarray[ts]);
                ServiceDetails[] sdarray = ((TransportStreamExt) tsarray[ts]).getAllServiceDetails();
                for (int sd = 0; sd < sdarray.length; sd++)
                {
                    expected.remove(sdarray[sd]);
                    expected.remove(((ServiceDetailsExt) sdarray[sd]).getService());
                    expected.remove(((ServiceDetailsExt) sdarray[sd]).getServiceDescription());
                    ServiceComponent[] scarray = ((ServiceDetailsExt) sdarray[sd]).getComponents();
                    for (int sc = 0; sc < scarray.length; sc++)
                    {
                        expected.remove(scarray[sc]);
                    }
                }
            }
        }

        // Make sure we found all expected objects
        assertTrue("Failed to find " + expected.size() + " expected objects", expected.size() == 0);
    }

    /**
     * A helper class for dealing with a collection of expected SI objects
     */
    private static class ExpectedVector extends Vector
    {
        // Remove object if it is an exact match to one of the expected objects.
        // Assert if it is not an expected object.
        public boolean remove(Object o)
        {
            boolean removed = super.remove(o);
            assertTrue("Unexpected object " + o, removed);
            return removed;
        }
    }
}
