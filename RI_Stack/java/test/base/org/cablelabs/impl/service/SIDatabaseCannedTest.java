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
package org.cablelabs.impl.service;

import java.util.ArrayList;

import junit.textui.TestRunner;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.service.CannedSIDatabase;
import org.cablelabs.impl.manager.service.CannedSIDatabase.ServiceComponentHandleImpl;
import org.cablelabs.impl.manager.service.CannedSIDatabase.ServiceDetailsHandleImpl;
import org.cablelabs.test.SICannedInterfaceTest;

/**
 * <p>
 * CannedSIDatabaseTest is an interface test that tests SIDatabase interface
 * using canned data and behavior. Since this is an interface test, nothing
 * outside what the interface provides is tested.
 * </p>
 * <p>
 * All of SIDatabase consists of get/create methods. Since testing values
 * returned from these types of methods is trivial only a small amount of time
 * will be spent here describing these tests. Regardless of the lack of detail
 * in this Javadoc, these methods are thoroughly tested to ensure they perform
 * exactly the way they are supposed to.
 * </p>
 * <p>
 * There are two main parts to this test case. One part tests all of the
 * get/create methods to ensure they are working properly. The second part tests
 * the exception throwing capability by purposely raising exceptions. The latter
 * tests have the suffix "WithException".
 * </p>
 * 
 * @author Joshua Keplinger
 */
public class SIDatabaseCannedTest extends SICannedInterfaceTest
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

    /**
     * Default constructor that takes no parameters and provides a default name
     * of "CannedSIDatabaseTest", <code>SIDatabaseFactoryTest</code> as default
     * <code>ImplFactory</code>, and <code>SIDatabase</code> as the default
     * class implementation.
     */
    public SIDatabaseCannedTest()
    {
        super("SIDatabaseCannedTest", SIDatabase.class, new CannedSIDatabaseFactoryTest());
    }

    /**
     * Creates an InterfaceTestSuite using the tests in this test case.
     * 
     * @return an InterfaceTestSuite specifying all tests in this
     *         InterfaceTestCase.
     */
    public static InterfaceTestSuite isuite()
    {
        InterfaceTestSuite suite = new InterfaceTestSuite(SIDatabaseCannedTest.class);
        suite.setName(SIDatabase.class.getName());
        suite.addFactory(new CannedSIDatabaseFactoryTest());
        return suite;
    }

    // Setup section \\

    /**
     * Holds the instance of the SIDatabase being tested.
     */
    private SIDatabase sidb;

    /**
     * Sets up the test case.
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        sidb = (SIDatabase) createImplObject();
    }

    /**
     * Cleans up after the test case.
     */
    protected void tearDown() throws Exception
    {
        sidb = null;

        super.tearDown();
    }

    // Testing section \\

    /**
     * Tests <code>getSupportedDimensions()</code> for a null String array
     */
    public void testGetSupportedDimensions() throws Exception
    {
        // Get array of handles
        RatingDimensionHandle[] dimensions = sidb.getSupportedDimensions();
        // Check for null array
        assertNotNull("Expected non-null RatingDimensionHandle array", dimensions);
        // Check for proper array length
        assertEquals("Array length does not match expected value", 1, dimensions.length);
        // Check array elements for proper data
        // assertEquals("First element does not match expected value.", "MMPA",
        // dimensions[0]);
    }

    /**
     * Tests <code>createRatingDimension()</code> to ensure that the
     * RatingDimension object reference is not null and matches test values.
     */
    public void testCreateRatingDimension() throws Exception
    {

        // Test data
        String ratingDimensionName = csidb.ratingDimension1.getDimensionName();
        RatingDimensionHandle handle = csidb.ratingDimension1.getRatingDimensionHandle();

        try
        {
            // Get RatingDimension object
            RatingDimensionExt dimension = sidb.createRatingDimension(handle);
            // Test for null object reference
            assertNotNull("Expected non-null RatingDimension object", dimension);

            // Check for matching dimension name
            assertEquals("Rating dimension name does not match expected value.", ratingDimensionName,
                    dimension.getDimensionName());
            // Check number of levels
            assertEquals("Number of levels does not match expected value.", csidb.ratingDimension1.getNumberOfLevels(),
                    dimension.getNumberOfLevels());
            // Check for proper array length in rating level description
            String[] desc = dimension.getRatingLevelDescription((short) 1);
            assertEquals("Length of description does not match expected value.",
                    csidb.ratingDimension1.getRatingLevelDescription((short) 1).length, desc.length);
        }
        catch (SIRequestInvalidException ex)
        {
            fail("Caught SIRequestInvalidException using valid data: " + ex.getMessage());
        }

    }

    /**
     * Tests <code>createRatingDimension()</code> for proper Exception throwing
     * using invalid and null values.
     */
    public void testCreateRatingDimensionWithException() throws Exception
    {
        // Create an invalid handle for exception testing
        RatingDimensionHandle handle = new CannedSIDatabase.RatingDimensionHandleImpl(100);

        // Try to raise exception with invalid handle
        try
        {
            // Attempt to create RatingDimension object with invalid data
            sidb.createRatingDimension(handle);
            // Exception was not thrown properly
            fail("Invalid rating dimension handle should have raised exception");
        }
        catch (SIRequestInvalidException expected)
        {
        }
        // Try to raise exception with null handle
        try
        {
            // Attempt to create RatingDimension object with null data
            sidb.createRatingDimension(null);
            // Exception was not thrown properly
            fail("Invalid rating dimension name should have raised exception");
        }
        catch (SIRequestInvalidException expected)
        {
        }

    }

    /**
     * Tests <code>getAllTransports()</code> for a null Object array.
     */
    public void testGetAllTransports() throws Exception
    {
        // Get array of Objects
        TransportHandle[] handles = sidb.getAllTransports();

        // Check for null array
        assertNotNull("Expected non-null Object array", handles);
        // Make sure we got a handle back and not something else
        assertTrue("Array returned does not contain valid handles", handles[0] instanceof TransportHandle);
        // Check handle of first object in array
        assertEquals("Transport handle does not match expected value", csidb.transport1.getTransportHandle(),
                handles[0]);
    }

    /**
     * Tests <code>createTransport()</code> to ensure that the object reference
     * is not null and data matches test values.
     */
    public void testCreateTransport() throws Exception
    {

        // Test Transport handle
        TransportHandle transportHandle = csidb.transport1.getTransportHandle();

        try
        {
            // Get TransportExt object
            TransportExt transport = sidb.createTransport(transportHandle);
            // Check for null object reference
            assertNotNull("Expected non-null Transport object", transport);
            // Get and check the transport handle
            Object handle = transport.getTransportHandle();
            assertEquals("Transport handle does not match expected value.", transportHandle, handle);
        }
        catch (SIRequestInvalidException ex)
        {
            fail("Caught SIRequestInvalidException using valid data: " + ex.getMessage());
        }

    }

    /**
     * Test <code>createTransport()</code> for proper exception throwing using
     * invalid and null values.
     */
    public void testCreateTransportWithException() throws Exception
    {
        // Create invalid TransportHandle
        TransportHandle transportHandle = new CannedSIDatabase.TransportHandleImpl(100);

        // Try to raise exception
        try
        {
            // Attempt to create TransportExt object using invalid data
            sidb.createTransport(transportHandle);
            // Exception failed to be thrown
            fail("Exception not thrown properly using invalid Transport handle");
        }
        catch (SIRequestInvalidException expected)
        {
        }
        // Try to raise exception
        try
        {
            // Attempt to create TransportExt object using null data
            sidb.createTransport(null);
            // Exception failed to be thrown
            fail("Exception not thrown properly using null Transport handle");
        }
        catch (SIRequestInvalidException expected)
        {
        }

    }

    /**
     * Tests <code>getNetworksByTransport()</code> for a null Object array and
     * valid test data.
     */
    public void testGetNetworksByTransport() throws Exception
    {

        // Test Transport handle
        TransportHandle transportHandle = csidb.transport1.getTransportHandle();
        try
        {
            // Get our Object array
            NetworkHandle[] networks = sidb.getNetworksByTransport(transportHandle);
            // Check for null array
            assertNotNull("Expected non-null Object array", networks);
            // Check for length of array
            assertEquals("Array length does not match expected value.", 3, networks.length);
            // Make sure we got valid handles back
            // assertTrue("Array returned does not contain valid handles",
            // networks[0] instanceof Integer);
            // Check a few values for equality
            assertEquals("First network handle does not match expected value.", csidb.network3.getNetworkHandle(),
                    networks[0]);
        }
        catch (SIRequestInvalidException ex)
        {
            fail("Caught SIRequestInvalidException using valid data: " + ex.getMessage());
        }
    }

    /**
     * Tests <code>getNetworksByTransport()</code> for proper exception throwing
     * using invalid and null values.
     */
    public void testGetNetworksByTransportWithException() throws Exception
    {

        // Test Transport handle
        TransportHandle transportHandle = new CannedSIDatabase.TransportHandleImpl(100);

        // Try to raise exception using invalid data
        try
        {
            // Attempt to create array of Objects
            sidb.getNetworksByTransport(transportHandle);
            fail("Exception not thrown properly using invalid Transport handle");
        }
        catch (SIRequestInvalidException expected)
        {
        }
        // Try to raise exception using null data
        try
        {
            // Attempt to create array of Objects
            sidb.getNetworksByTransport(null);
            // Exception not thrown correctly
            fail("Exception not thrown properly using invalid Transport handle");
        }
        catch (SIRequestInvalidException expected)
        {
        }

    }

    /**
     * Tests <code>createNetwork()</code> to ensure that the object reference is
     * not null and has valid test data.
     */
    public void testCreateNetwork() throws Exception
    {
        NetworkHandle networkHandle = csidb.network3.getNetworkHandle();

        try
        {
            // Get our NetworkExt object
            NetworkExt network = sidb.createNetwork(networkHandle);
            // Test for null value
            assertNotNull("Expected non-null Network object", network);
            // Check for correct values
            Object handle = network.getNetworkHandle();
            assertEquals("Network handle does not match expected value.", networkHandle, handle);
        }
        catch (SIRequestInvalidException ex)
        {
            fail("Caught SIRequestInvalidException using valid data: " + ex.getMessage());
        }
    }

    /**
     * Tests <code>createNetwork()</code> for proper Exception throwing using
     * invalid and null data.
     */
    public void testCreateNetworkWithException() throws Exception
    {

        // Test data
        NetworkHandle networkHandle = new CannedSIDatabase.NetworkHandleImpl(100);

        // Try to raise an exception using invalid network handle
        try
        {
            // Attempt to create a NetworkExt object
            sidb.createNetwork(networkHandle);
            // Exception was not thrown properly
            fail("Exception not thrown properly using invalid network handle");
        }
        catch (SIRequestInvalidException expected)
        {
        }
        // Try to raise an exception using null network handle
        try
        {
            // Attempt to create a NetworkExt object
            sidb.createNetwork(null);
            // Exception was not thrown properly
            fail("Exception not thrown properly using null network handle");
        }
        catch (SIRequestInvalidException expected)
        {
        }
    }

    /**
     * Tests <code>getTransportStreamsByTransport()</code> for a null Object
     * array and valid test data.
     */
    public void testGetTransportStreamsByTransport() throws Exception
    {
        // Test data
        TransportHandle transportHandle = csidb.transport1.getTransportHandle();

        try
        {
            // Get an array of Objects
            TransportStreamHandle[] streams = sidb.getTransportStreamsByTransport(transportHandle);
            // Check that the array is not null
            assertNotNull("Expected non-null Object array", streams);
            // Check array length
            assertEquals("Array length does not match expected value.", 7, streams.length);
            // Make sure we got valid handles
            assertTrue("Returned array does not contain valid handles.", streams[0] instanceof TransportStreamHandle);
            // Check a couple values in result
            // assertEquals("Last element transport stream handle does not match expected value",
            // CannedSIDatabase.transportStream12.getTransportStreamHandle(),
            // streams[5]);
        }
        catch (SIRequestInvalidException ex)
        {
            fail("Caught SIRequestInvalidException using valid data: " + ex.getMessage());
        }

    }

    /**
     * Tests <code>getTransportStreamsByTransport()</code> for proper Exception
     * throwing using invalid and null data.
     */
    public void testGetTransportStreamsByTransportWithException() throws Exception
    {
        // Test data
        TransportHandle transportHandle = new CannedSIDatabase.TransportHandleImpl(100);

        // Try to raise an exception using invalid transport handle
        try
        {
            // Attempt to retrieve an Object array
            sidb.getTransportStreamsByTransport(transportHandle);
            // Exception was not thrown as expected
            fail("Exception not thrown properly using invalid transport handle");
        }
        catch (SIRequestInvalidException expected)
        {
        }
        // Try to raise an exception using null transport handle
        try
        {
            // Attempt to retrieve an Object array
            sidb.getTransportStreamsByTransport(null);
            // Exception was not thrown as expected
            fail("Exception not thrown properly using null transport handle");
        }
        catch (SIRequestInvalidException expected)
        {
        }
    }

    /**
     * Tests <code>getTransportStreamsByNetwork()</code> for a null Object array
     * and valid test data.
     */
    public void testGetTransportStreamsByNetwork() throws Exception
    {
        // Test data
        NetworkHandle networkHandle = csidb.network3.getNetworkHandle();

        try
        {
            // Get an array of Objects
            TransportStreamHandle[] streams = sidb.getTransportStreamsByNetwork(networkHandle);
            // Check for null array
            assertNotNull("Expected non-null Object array", streams);
            // Check array length
            assertEquals("Array length does not match expected value.", 5, streams.length);
            // Make sure we got valid handles
            assertTrue("Array returned does not contain valid handles", streams[0] instanceof TransportStreamHandle);
            // Make sure our handle matches
            // assertEquals("Last element transport stream handle does not match expected value.",
            // CannedSIDatabase.transportStream12.getTransportStreamHandle(),
            // streams[4]);
        }
        catch (SIRequestInvalidException ex)
        {
            fail("Caught SIRequestInvalidException using valid data: " + ex.getMessage());
        }
    }

    /**
     * Tests <code>getTransportStreamsByNetwork()</code> for proper Exception
     * raising using invalid and null data.
     */
    public void testGetTransportStreamsByNetworkWithException() throws Exception
    {
        // Test data
        NetworkHandle networkHandle = new CannedSIDatabase.NetworkHandleImpl(100);

        // Try to raise an exception using invalid data
        try
        {
            // Attempt to create an Object array
            sidb.getTransportStreamsByNetwork(networkHandle);
            // Exception was not thrown as expected
            fail("Invalid data should have raised exception");
        }
        catch (SIRequestInvalidException expected)
        {
        }
        // Try to raise an exception using null data
        try
        {
            // Attempt to create an Object array
            sidb.getTransportStreamsByNetwork(null);
            // Exception was not thrown as expected
            fail("Null data should have raised exception");
        }
        catch (SIRequestInvalidException expected)
        {
        }
    }

    /**
     * Tests <code>createTransportStream()</code> to ensure that the reference
     * is not null and has valid test data.
     */
    public void testCreateTransportStream() throws Exception
    {
        // Test data
        TransportStreamHandle transportStreamHandle = csidb.transportStream7.getTransportStreamHandle();

        try
        {
            // Get a TransportStreamExt object
            TransportStreamExt tStream = sidb.createTransportStream(transportStreamHandle);
            // Check for null TransportStreamExt reference
            assertNotNull("Expected non-null TransportStream reference", tStream);
            // Check for matching transport stream handle
            Object handle = tStream.getTransportStreamHandle();
            assertEquals("Transport stream handle does not match expected value.", transportStreamHandle, handle);
            assertEquals("TransportStream frequency is incorrect", 5000, tStream.getFrequency());
            assertEquals("TransportStream modulation format is incorrect", 1, tStream.getModulationFormat());
        }
        catch (SIRequestInvalidException ex)
        {
            fail("Caught SIRequestInvalidException using valid data: " + ex.getMessage());
        }
    }

    /**
     * Tests <code>createTransportStream()</code> for proper Exception throwing
     * using invalid and null data.
     */
    public void testCreateTransportStreamWithException() throws Exception
    {
        // Test data
        TransportStreamHandle transportStreamHandle = new CannedSIDatabase.TransportStreamHandleImpl(100);

        // Try to raise exception using invalid data
        try
        {
            // Attempt to create TransportStreamExt object
            sidb.createTransportStream(transportStreamHandle);
            // Exception was not thrown properly
            fail("Invalid transport stream handle should have raised exception");
        }
        catch (SIRequestInvalidException expected)
        {
        }
        // Try to raise exception using null data
        try
        {
            // Attempt to create TransportStreamExt object
            sidb.createTransportStream(null);
            // Exception was not thrown properly
            fail("Null transport stream handle should have raised exception");
        }
        catch (SIRequestInvalidException expected)
        {
        }
    }

    /**
     * Tests <code>getAllServices()</code> for a null Object array and valid
     * test data.
     */
    public void testGetAllServices() throws Exception
    {
        // Get Object array
        ServiceHandle[] services = sidb.getAllServices();
        // Check for null array
        assertNotNull("Expected non-null Object array", services);
        // Check for proper array length
        assertEquals("Array length does not match expected value.", 20, services.length);
        // Make sure array contains valid handles
        assertTrue("Array returned does not contain valid handles", services[0] instanceof ServiceHandle);
    }

    /**
     * Tests <code>getServiceBySourceID()</code> for a null Object reference and
     * valid test data.
     */
    public void testGetServiceBySourceID() throws Exception
    {
        // Test data
        int sourceID = 100;

        try
        {
            // Get Object reference
            ServiceHandle service = sidb.getServiceBySourceID(sourceID);
            // Check for null reference
            assertNotNull("Expected non-null Object reference", service);
            // make sure we got a valid handle
            assertTrue("Returned reference is not a valid handle", service instanceof ServiceHandle);
            // Check a few values
            assertEquals("Service handle does not match expected value.", csidb.service15.getServiceHandle(), service);
        }
        catch (SIRequestInvalidException ex)
        {
            fail("Caught SIRequestInvalidException using valid data: " + ex.getMessage());
        }

        sourceID = 117;

        try
        {
            // Get Object reference
            ServiceHandle service = sidb.getServiceBySourceID(sourceID);
            // Check for null reference
            assertNotNull("Expected non-null Object reference", service);
            // make sure we got a valid handle
            assertTrue("Returned reference is not a valid handle", service instanceof ServiceHandle);
            // Check a few values
            assertEquals("Service handle does not match expected value.", csidb.service32.getServiceHandle(), service);
        }
        catch (SIRequestInvalidException ex)
        {
            fail("Caught SIRequestInvalidException using valid data: " + ex.getMessage());
        }
    }

    /**
     * Tests <code>getServiceBySourceID()</code> for proper Exception throwing
     * using invalid data.
     */
    public void testGetServiceBySourceIDWithException() throws Exception
    {
        // Test data
        int sourceID = -1000;

        // Try to raise an exception
        try
        {
            // Attempt to get Object reference
            sidb.getServiceBySourceID(sourceID);
            // Exception not raised properly
            fail("Exception not thrown properly using invalid data");
        }
        catch (SIRequestInvalidException expected)
        {
        }

        sourceID = 99;
        // Try to raise an exception
        try
        {
            // Attempt to get Object reference
            sidb.getServiceBySourceID(sourceID);
            // Exception not raised properly
            fail("Exception not thrown properly using invalid data");
        }
        catch (SIRequestInvalidException expected)
        {
        }

        sourceID = 136;
        // Try to raise an exception
        try
        {
            // Attempt to get Object reference
            sidb.getServiceBySourceID(sourceID);
            // Exception not raised properly
            fail("Exception not thrown properly using invalid data");
        }
        catch (SIRequestInvalidException expected)
        {
        }
    }

    /**
     * Tests <code>getServiceByServiceName()</code> for a null Object reference
     * and valid test data.
     */
    public void testGetServiceByServiceName() throws Exception
    {
        // Test data
        String serviceName = csidb.service15.getName();

        try
        {
            // Get Object reference
            ServiceHandle service = sidb.getServiceByServiceName(serviceName);
            // Check for null reference
            assertNotNull("Expected non-null Object reference", service);
            // Check a few values
            assertEquals("Service handle does not match expected value.", csidb.service15.getServiceHandle(), service);
        }
        catch (SIRequestInvalidException ex)
        {
            fail("Caught SIRequestInvalidException using valid data: " + ex.getMessage());
        }
    }

    /**
     * Tests <code>getServiceByServiceName()</code> for proper exception
     * throwing using invalid and null values.
     * 
     */
    public void testGetServiceByServiceNameWithException() throws Exception
    {
        // Test data
        String serviceName = "flah service1";

        // Try to raise exception using invalid service name
        try
        {
            // Attempt to get Object reference
            sidb.getServiceByServiceName(serviceName);
            // Exception not thrown correctly
            fail("Exception not thrown correctly using invalid data");
        }
        catch (SIRequestInvalidException expected)
        {
        }
        // Try to raise exception using null service name
        try
        {
            // Attempt to get Object reference
            sidb.getServiceByServiceName("");
            // Exception not thrown correctly
            fail("Exception not thrown correctly using invalid data");
        }
        catch (SIRequestInvalidException expected)
        {
        }
    }

    /**
     * Tests <code>getServiceByProgramNumber()</code> for a null Object
     * reference and valid test data.
     */
    public void testGetServiceByProgramNumber() throws Exception
    {
        // Test data
        int frequency = 5000;
        int programNumber = 2;
        int modulationFormat = 1;
        try
        {
            // Get Object reference
            ServiceHandle service = sidb.getServiceByProgramNumber(frequency, modulationFormat, programNumber);
            // Check for null reference
            assertNotNull("Expected non-null Object reference", service);
            // make sure we got a valid handle
            assertTrue("Returned reference is not a valid handle", service instanceof ServiceHandle);
            // Check for a couple values
            assertEquals("Service handle does not match expected value.", csidb.service15.getServiceHandle(), service);
        }
        catch (SIRequestInvalidException ex)
        {
            fail("Caught SIRequestInvalidException using valid data: " + ex.getMessage());
        }
    }

    /**
     * Tests <code>getServiceByProgramNumber()</code> for proper Exception
     * throwing using invalid data.
     */
    public void testGetServiceByProgramNumberWithException() throws Exception
    {
        // Test data
        int frequency = -5000;
        int programNumber = -2;
        int modulationFormat = -51;

        // Try to raise an exception using invalid data
        try
        {
            // Attempt to get Object reference using invalid data
            sidb.getServiceByProgramNumber(frequency, modulationFormat, programNumber);
            // Exception not thrown properly
            fail("Exception not thrown using invalid data");
        }
        catch (SIRequestInvalidException expected)
        {
        }
    }

    /**
     * Tests <code>createService()</code> to ensure that reference is not null
     * and has valid test data.
     */
    public void testCreateService() throws Exception
    {
        // Test data
        ServiceHandle serviceHandle = csidb.service15.getServiceHandle();

        try
        {
            // Get ServiceExt object
            ServiceExt service = sidb.createService(serviceHandle);
            // Check for null ServiceExt reference
            assertNotNull("Expected non-null Service reference", service);
            // Check for a couple values
            assertEquals("Service handle does not match expected value.", csidb.service15.getServiceHandle(),
                    service.getServiceHandle());
            assertEquals("Service number does not match expected value.", csidb.service15.getServiceNumber(),
                    service.getServiceNumber());
        }
        catch (SIRequestInvalidException ex)
        {
            fail("Caught SIRequestInvalidException using valid data: " + ex.getMessage());
        }
    }

    /**
     * Tests <code>createService()</code> for proper exception throwing using
     * invalid and null values.
     */
    public void testCreateServiceWithException() throws Exception
    {
        // Test data
        ServiceHandle serviceHandle = new CannedSIDatabase.ServiceHandleImpl(100);

        // Try to raise an exception using invalid data
        try
        {
            // Attempt to get ServiceExt object using invalid service handle
            sidb.createService(serviceHandle);
            // Exception was not thrown properly
            fail("Exception not properly thrown using invalid data");
        }
        catch (SIRequestInvalidException expected)
        {
        }
        // Try to raise exception using null data
        try
        {
            // Attempt to get another ServiceExt object using null service
            // handle
            sidb.createService(null);
            // Exception failed to raise
            fail("Exception not properly thrown using null data");
        }
        catch (SIRequestInvalidException expected)
        {
        }
    }

    /**
     * Tests <code>getServiceDetailsByService()</code> for a null Object array
     * and valid test data.
     */
    public void testGetServiceDetailsByService() throws Exception
    {
        // Test data
        ServiceHandle serviceHandle = csidb.service15.getServiceHandle();

        try
        {
            // Get Object array
            ServiceDetailsHandle[] sDetails = sidb.getServiceDetailsByService(serviceHandle);
            // Check for null array
            assertNotNull("Expected non-null Object array", sDetails);
            // Check array length
            assertEquals("Array length does not match value expected.", 1, sDetails.length);
            // Make sure we got valid handles
            assertTrue("Array returned does not contain valid handles", sDetails[0] instanceof ServiceDetailsHandle);
            assertEquals("Returned handle does not match expected value.",
                    csidb.serviceDetails33.getServiceDetailsHandle(), ((ServiceDetailsHandleImpl) sDetails[0]));

        }
        catch (SIRequestInvalidException ex)
        {
            fail("Caught SIRequestInvalidException using valid data: " + ex.getMessage());
        }
    }

    /**
     * Tests <code>getServiceDetailsByService()</code> for proper exception
     * using invalid and null data.
     */
    public void testGetServiceDetailsByServiceWithException() throws Exception
    {
        // Test data
        ServiceHandle serviceHandle = new CannedSIDatabase.ServiceHandleImpl(100);

        // Try to raise exception using invalid data
        try
        {
            // Attempt to get Object array using invalid data
            sidb.getServiceDetailsByService(serviceHandle);
            // Exception failed to be thrown
            fail("Exception was not thrown properly using invalid data");
        }
        catch (SIRequestInvalidException expected)
        {
        }

        // Try to raise exception using null data
        try
        {
            // Attempt to get Object array using null data
            sidb.getServiceDetailsByService(null);
            // Exception failed to be thrown
            fail("Exception was not thrown properly using null data");
        }
        catch (SIRequestInvalidException expected)
        {
        }
    }

    /**
     * Tests <code>createServiceDetails()</code> for a null object reference and
     * valid test data.
     */
    public void testCreateServiceDetails() throws Exception
    {
        // Test data
        ServiceDetailsHandle serviceDetailsHandle = csidb.serviceDetails33.getServiceDetailsHandle();
        ServiceDetailsExt sDetails = null;

        try
        {
            // Get ServiceDetailsExt object
            sDetails = sidb.createServiceDetails(serviceDetailsHandle);
            // Check for null reference
            assertNotNull("Expected non-null ServiceDetails reference", sDetails);
            // Check a few values
            assertEquals("Service details handle does not match expected value.", serviceDetailsHandle,
                    sDetails.getServiceDetailsHandle());
        }
        catch (SIRequestInvalidException ex)
        {
            fail("Caught SIRequestInvalidException using valid data: " + ex.getMessage());
        }
    }

    /**
     * Tests <code>createServiceDetails()</code> for proper exception throwing
     * using invalid and null values.
     */
    public void testCreateServiceDetailsWithException() throws Exception
    {
        // Test data
        ServiceDetailsHandle serviceDetailsHandle = new CannedSIDatabase.ServiceDetailsHandleImpl(1000);

        // Try to raise exception using invalid data
        try
        {
            // Attempt to get ServicDetailsExt object using invalid service
            // details handle
            sidb.createServiceDetails(serviceDetailsHandle);
            // Exception not raised as expected
            fail("Exception not properly thrown using invalid service details handle");
        }
        catch (SIRequestInvalidException expected)
        {
        }
        // Try to raise exception using null data
        try
        {
            // Attempt to get another ServiceDetailsExt object using null
            // service details handle
            sidb.createServiceDetails(null);
            fail("Exception not thrown properly using null service details handle");
        }
        catch (SIRequestInvalidException expected)
        {
        }
    }

    /**
     * Tests <code>createServiceDescription()</code> to make sure that the
     * object reference is not null and has valid test data.
     */
    public void testCreateServiceDescription() throws Exception
    {
        // Test data
        ServiceDescriptionExt sDesc = null;

        try
        {
            // Get ServiceDescriptionExt object
            sDesc = sidb.createServiceDescription(csidb.serviceDetails33.getServiceDetailsHandle());
            // Check for null reference
            assertNotNull("Expected non-null ServiceDescription reference", sDesc);
            // Check a few values
            assertEquals("Service description does not match expected value.",
                    csidb.serviceDescription33.getServiceDescription(), sDesc.getServiceDescription());
            assertEquals("Description does not match expected value.",
                    csidb.serviceDescription33.getServiceDescription(), sDesc.getServiceDescription());
        }
        catch (SIRequestInvalidException ex)
        {
            fail("Caught SIRequestInvalidException using valid data: " + ex.getMessage());
        }

        try
        {
            // Get another ServiceDescriptionExt object
            ServiceDescriptionExt sDesc2 = sidb.createServiceDescription(csidb.serviceDetails33.getServiceDetailsHandle());
            // Check for null reference
            assertNotNull("Expected non-null ServiceDescription reference", sDesc2);
            assertEquals("Second ServiceDetails object does not match first.", sDesc, sDesc2);
        }
        catch (SIRequestInvalidException ex)
        {
            fail("Caught SIRequestInvalidException using valid data: " + ex.getMessage());
        }
    }

    /**
     * Tests <code>createServiceDescription()</code> for proper exception
     * throwing using invalid and null values.
     */
    public void testCreateServiceDescriptionWithException() throws Exception
    {
        // Test data
        ServiceDetailsHandle handle = new CannedSIDatabase.ServiceDetailsHandleImpl(2343);
        // Try to raise an exception using invalid data
        try
        {
            sidb.createServiceDescription(handle);
        }
        catch (SIRequestInvalidException expected)
        {
            // We wanted this to happen
        }
        // Try to raise exception using null data
        try
        {
            // Attempt to get another ServiceDescriptionExt object using null
            // service details handle
            sidb.createServiceDescription(null);
            // Exception not raised as expected
            fail("Exception not properly thrown using null service details handle");
        }
        catch (SIRequestInvalidException expected)
        {
        }
        catch (NullPointerException alsoExpected)
        {
        }
    }

    /**
     * Tests <code>getServiceComponentsByServiceDetails()</code> for a null
     * Object array and valid test data.
     */
    public void testGetServiceComponentsByServiceDetails() throws Exception
    {
        // Test data
        ServiceDetailsHandle serviceDetailsHandle = csidb.serviceDetails33.getServiceDetailsHandle();

        try
        {
            // Get Object array
            ServiceComponentHandle[] sComps = sidb.getServiceComponentsByServiceDetails(serviceDetailsHandle);
            // Check for null array
            assertNotNull("Expected non-null Object array.", sComps);
            // Check array for proper length
            assertEquals("Array length does not match expected value.", 5, sComps.length);
            // Make sure we got an array of handles
            assertTrue("Array returned does not hold valid handles.", sComps[0] instanceof ServiceComponentHandle);
            ArrayList list = new ArrayList();
            for (int i = 0; i < sComps.length; i++)
            {
                list.add(sComps[i]);
            }
            /*
             * assertTrue("Array does not contain handle for ServiceComponent 105."
             * ,list.contains(CannedSIDatabase.serviceComponentImpl105.
             * getServiceComponentHandle()));
             * assertTrue("Array does not contain handle for ServiceComponent 69."
             * ,list.contains(CannedSIDatabase.serviceComponentImpl69.
             * getServiceComponentHandle()));
             */

        }
        catch (SIRequestInvalidException ex)
        {
            fail("Caught SIRequestInvalidException using valid data: " + ex.getMessage());
        }
    }

    /**
     * Tests <code>getServiceComponentsByServiceDetails()</code> for proper
     * exception throwing using invalid and null values.
     */
    public void testGetServiceComponentsByServiceDetailsWithException() throws Exception
    {
        // Test data
        ServiceDetailsHandle serviceDetailsHandle = new CannedSIDatabase.ServiceDetailsHandleImpl(7000);

        // Try to raise exception using invalid data
        try
        {
            // Attempt to get Object array using invalid service details handle
            sidb.getServiceComponentsByServiceDetails(serviceDetailsHandle);
            // Exception not raised properly
            fail("Exception not properly thrown using invalid service details handle");
        }
        catch (SIRequestInvalidException expected)
        {
        }
        // Try to raise exception using null data
        try
        {
            // Attempt to get Object array using null service details handle
            sidb.getServiceComponentsByServiceDetails(null);
            // Exception not raised properly
            fail("Exception not properly thrown using null service details handle");
        }
        catch (SIRequestInvalidException expected)
        {
        }
    }

    /**
     * Tests <code>getServiceComponentByPID()</code> for a null Object
     * reference.
     */
    public void testGetServiceComponentByPID() throws Exception
    {
        // Test data
        ServiceDetailsHandle serviceDetailsHandle = csidb.serviceDetails33.getServiceDetailsHandle();
        int pid = csidb.serviceComponent105.getPID();

        try
        {
            // Get Object reference
            ServiceComponentHandle sComp = sidb.getServiceComponentByPID(serviceDetailsHandle, pid);
            // Check for null reference
            assertNotNull("Expected non-null Object reference.", sComp);
            // Make sure we got a valid handle
            assertTrue("Returned reference is not a valid handle.", sComp instanceof ServiceComponentHandle);
        }
        catch (SIRequestInvalidException ex)
        {
            fail("Caught SIRequestInvalidException using valid data: " + ex.getMessage());
        }
    }

    /**
     * Tests <code>getServiceComponentByPID()</code> for proper exception
     * throwing using invalid and null values.
     */
    public void testGetServiceComponentByPIDWithException() throws Exception
    {
        // Test data - use invalid service details handle and valid pid
        ServiceDetailsHandle serviceDetailsHandle = new CannedSIDatabase.ServiceDetailsHandleImpl(7000);
        int pid = 1;

        // Try to raise exception using invalid service details handle
        try
        {
            // Attempt to get Object reference using invalid service details
            // handle
            sidb.getServiceComponentByPID(serviceDetailsHandle, pid);
            // Exception failed to be thrown
            fail("Exception not thrown properly using invalid service details handle");
        }
        catch (SIRequestInvalidException expected)
        {
        }
        // Try to raise exception using null service details handle
        try
        {
            // Attempt to get Object reference using null service details handle
            sidb.getServiceComponentByPID(null, pid);
            // Exception failed to be thrown
            fail("Exception not thrown properly using null service details handle");
        }
        catch (SIRequestInvalidException expected)
        {
        }

        // Set service details handle to valid value to test invalid pid
        serviceDetailsHandle = new CannedSIDatabase.ServiceDetailsHandleImpl(33);
        pid = -500;

        // Try to raise exception using invalid pid
        try
        {
            // Attempt to get Object reference using invalid pid
            sidb.getServiceComponentByPID(serviceDetailsHandle, pid);
            // Exception failed to be thrown
            fail("Exception not thrown properly using invalid pid");
        }
        catch (SIRequestInvalidException expected)
        {
        }
    }

    /**
     * Tests <code>getServiceComponentByTag()</code> for a null Object
     * reference.
     */
    public void testGetServiceComponentByTag() throws Exception
    {
        // Test data
        ServiceDetailsHandle serviceDetailsHandle = csidb.serviceDetails33.getServiceDetailsHandle();
        int tag = csidb.serviceComponent105.getComponentTag();

        try
        {
            // Get Object reference
            ServiceComponentHandle sComp = sidb.getServiceComponentByTag(serviceDetailsHandle, tag);
            // Check for null reference
            assertNotNull("Expected non-null Object reference", sComp);
            // Make sure we got a valid handle back
            assertTrue("Returned reference is not a valid handle.", sComp instanceof ServiceComponentHandle);
            // Check a couple values
            assertEquals("Service component handle does not match expected value.",
                    csidb.serviceComponent105.getServiceComponentHandle(), ((ServiceComponentHandleImpl) sComp));
        }
        catch (SIRequestInvalidException ex)
        {
            fail("Caught SIRequestInvalidException using valid data: " + ex.getMessage());
        }
    }

    /**
     * Tests <code>getServiceComponentByTag()</code> for proper exception
     * throwing using invalid and null values.
     */
    public void testGetServiceComponentByTagWithException() throws Exception
    {
        // Test data - invalid service details handle and valid Tag
        ServiceDetailsHandle serviceDetailsHandle = new CannedSIDatabase.ServiceDetailsHandleImpl(7000);
        int tag = 1;

        // Try to raise exception using invalid service details handle
        try
        {
            // Attempt to get Object reference using invalid service details
            // handle
            sidb.getServiceComponentByTag(serviceDetailsHandle, tag);
            // Exception failed to be thrown
            fail("Exception not thrown properly using invalid service details handle");
        }
        catch (SIRequestInvalidException expected)
        {
        }
        // Try to raise exception using null service details handle
        try
        {
            // Attempt to get Object reference using null service details handle
            sidb.getServiceComponentByTag(null, tag);
            // Exception failed to be thrown
            fail("Exception not thrown properly using null service details handle");
        }
        catch (SIRequestInvalidException expected)
        {
        }

        // Set service details handle to valid value to test invalid tag
        serviceDetailsHandle = csidb.serviceDetails33.getServiceDetailsHandle();
        tag = -500;

        // Try to raise exception using invalid tag
        try
        {
            // Attempt to get Object reference using invalid tag
            sidb.getServiceComponentByTag(serviceDetailsHandle, tag);
            // Exception failed to be thrown
            fail("Exception not thrown properly using invalid tag");
        }
        catch (SIRequestInvalidException expected)
        {
        }
    }

    /**
     * Tests <code>getServiceComponentByName()</code> for a null Object
     * reference.
     */
    public void testGetServiceComponentByName() throws Exception
    {
        // Test data
        ServiceDetailsHandle serviceDetailsHandle = csidb.serviceDetails33.getServiceDetailsHandle();
        String serviceComponentName = csidb.serviceComponent105.getName();

        try
        {
            // Get Object reference
            ServiceComponentHandle sComp = sidb.getServiceComponentByName(serviceDetailsHandle, serviceComponentName);
            // Check for null reference
            assertNotNull("Expected non-null Object reference", sComp);
            // Make sure we got a valid handle
            assertTrue("Returned reference is not a valid handle.", sComp instanceof ServiceComponentHandle);
            // Check a couple values
            assertEquals("Service component handle does not match expected value.",
                    csidb.serviceComponent105.getServiceComponentHandle(), ((ServiceComponentHandleImpl) sComp));
        }
        catch (SIRequestInvalidException ex)
        {
            fail("Caught SIRequestInvalidException using valid data: " + ex.getMessage());
        }
    }

    /**
     * Tests <code>getServiceComponentByName()</code> for proper exception
     * throwing using invalid and null values.
     */
    public void testGetServiceComponentByNameWithException() throws Exception
    {
        // Test data - Use invalid service details handle and valid name
        ServiceDetailsHandle serviceDetailsHandle = new CannedSIDatabase.ServiceDetailsHandleImpl(7000);
        String serviceComponentName = csidb.serviceComponent105.getName();

        // Try to raise exception using invalid service details handle
        try
        {
            // Attempt to get Object reference using invalid service details
            // handle
            sidb.getServiceComponentByName(serviceDetailsHandle, serviceComponentName);
            // Exception failed to be thrown
            fail("Exception not thrown properly using invalid service details handle");
        }
        catch (SIRequestInvalidException expected)
        {
        }
        // Try to raise exception using null service details handle
        try
        {
            // Attempt to get Object reference using null service details handle
            sidb.getServiceComponentByName(null, serviceComponentName);
            // Exception failed to be thrown
            fail("Exception not thrown properly using null service details handle");
        }
        catch (SIRequestInvalidException expected)
        {
        }

        // Set service details handle to valid value to test invalid
        // serviceComponentName
        serviceDetailsHandle = csidb.serviceDetails33.getServiceDetailsHandle();
        serviceComponentName = "invalid string";

        // Try to raise exception using invalid serviceComponentName
        try
        {
            // Attempt to get Object reference using invalid
            // serviceComponentName
            sidb.getServiceComponentByName(serviceDetailsHandle, serviceComponentName);
            // Exception failed to be thrown
            fail("Exception not thrown properly using invalid service component name");
        }
        catch (SIRequestInvalidException expected)
        {
        }
        // Try to raise exception using invalid service component name
        try
        {
            // Attempt to get Object reference using invalid service component
            // name
            sidb.getServiceComponentByName(serviceDetailsHandle, null);
            // Exception failed to be thrown
            fail("Exception not thrown properly using invalid service component name");
        }
        catch (SIRequestInvalidException expected)
        {
        }
    }

    public void testGetCarouselComponentByServiceDetails() throws Exception
    {
        ServiceComponentHandle scHandle = sidb.getCarouselComponentByServiceDetails(csidb.serviceDetails33.getServiceDetailsHandle());
        assertNotNull("Returned handle should not be null", scHandle);

        try
        {
            sidb.getCarouselComponentByServiceDetails(null);
            fail("Expected NullPointerException");
        }
        catch (SIRequestInvalidException ex)
        {
            // Expected
        }
    }

    public void testGetCarouselComponentByServiceDetailsAndID() throws Exception
    {
        ServiceComponentHandle scHandle = sidb.getCarouselComponentByServiceDetails(
                csidb.serviceDetails33.getServiceDetailsHandle(), 46);
        assertNotNull("Returned handle should not be null", scHandle);

        try
        {
            sidb.getCarouselComponentByServiceDetails(null);
            fail("Expected NullPointerException");
        }
        catch (SIRequestInvalidException ex)
        {
            // Expected
        }
    }

    /**
     * Tests <code>createServiceComponent()</code> to ensure that the object
     * reference is not null.
     */
    public void testCreateServiceComponent() throws Exception
    {
        // Test data
        ServiceComponentHandle serviceComponentHandle = csidb.serviceComponent105.getServiceComponentHandle();
        ServiceComponentExt sComp = null;

        try
        {
            // Get ServiceComponentExt object
            sComp = sidb.createServiceComponent(serviceComponentHandle);
            // Check for null reference
            assertNotNull("Expected non-null Object reference", sComp);
            // Check a couple values
            assertEquals("PID does not match expected value.", csidb.serviceComponent105.getPID(), sComp.getPID());
            assertEquals("Associated language does not match expected value.", "eng", sComp.getAssociatedLanguage());
        }
        catch (SIRequestInvalidException ex)
        {
            fail("Caught SIRequestInvalidException using valid data: " + ex.getMessage());
        }

        try
        {
            // Get another ServiceComponentExt object
            ServiceComponentExt sComp2 = sidb.createServiceComponent(serviceComponentHandle);
            // Check for null reference
            assertNotNull("Expected non-null Object reference", sComp2);
            assertEquals("Second ServiceComponent object does not match first.", sComp, sComp2);
        }
        catch (SIRequestInvalidException ex)
        {
            fail("Caught SIRequestInvalidException using valid data: " + ex.getMessage());
        }
    }

    /**
     * Tests <code>createServiceComponent()</code> to make sure it properly
     * throws exceptions
     */
    public void testCreateServiceComponentWithException() throws Exception
    {
        // Test data
        ServiceComponentHandle serviceComponentHandle = new CannedSIDatabase.ServiceComponentHandleImpl(10000);

        // Try to raise exception using invalid data
        try
        {
            // Attempt to get ServicComponentExt object using invalid service
            // component handle
            sidb.createServiceComponent(serviceComponentHandle);
            // Exception not raised as expected
            fail("Exception not properly thrown using invalid service component handle");
        }
        catch (SIRequestInvalidException expected)
        {
        }

        // Try to raise exception using null data
        try
        {
            // Attempt to get another ServiceComponentExt object using null
            // service component handle
            sidb.createServiceComponent(null);
            // Exception not raised as expected
            fail("Exception not properly thrown using null service component handle");
        }
        catch (SIRequestInvalidException expected)
        {
        }
    }

    /**
     *
     */
    public void testGetRatingDimensionByName() throws Exception
    {
        RatingDimensionHandle handle = sidb.getRatingDimensionByName("MMPA");
        assertNotNull("Returned handle is null", handle);
        assertEquals("Returned handle does not match expected value",
                csidb.ratingDimension1.getRatingDimensionHandle(), handle);
    }

    /**
     *
     */
    public void testGetTransportByID() throws Exception
    {
        TransportHandle handle = sidb.getTransportByID(csidb.transport1.getTransportID());
        assertNotNull("Returned handle is null", handle);
        assertEquals("Returned handle does not match expected value", csidb.transport1.getTransportHandle(), handle);
    }

    /**
     * This is a default factory class that is passed to the
     * <code>CannedSIDatabaseTest</code>. It is used to instantiate a concrete
     * class to be used in the test.
     * 
     * @author Josh
     */
    private static class CannedSIDatabaseFactoryTest implements ImplFactory
    {

        public Object createImplObject() throws Exception
        {
            SIDatabase sidb;
            ServiceManager sm = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
            sidb = sm.getSIDatabase();
            CannedSIDatabase canned = (CannedSIDatabase) sidb;
            canned.cannedFullReset();
            return sidb;
        }

        public String toString()
        {
            return "CannedSIDatabaseFactoryTest";
        }
    }
}
