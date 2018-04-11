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
package org.cablelabs.impl.service.javatv.service;

import javax.tv.service.SIRetrievable;
import javax.tv.service.ServiceTest;
import javax.tv.service.ServiceType;
import javax.tv.service.ServiceTest.ServiceDescription;
import javax.tv.service.ServiceTest.ServicePair;
import javax.tv.service.navigation.ServiceDetails;

import junit.framework.TestSuite;
import junit.textui.TestRunner;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.ocap.net.OcapLocator;

import org.cablelabs.impl.manager.service.CannedSIDatabase.ServiceHandleImpl;
import org.cablelabs.impl.util.string.MultiString;
import org.cablelabs.test.SICannedConcreteTest;

/**
 * <p>
 * CannedServiceExtTest is used to test the ServiceExt interface using canned
 * data and behavior from the canned testing environment. This test is only an
 * interface test and does not do any testing on any methods not specified in
 * the ServiceExt interface.
 * </p>
 * <p>
 * Most of ServiceExt consists of getter methods. The testing done on these is
 * thorough, yet simple, and will only be briefly described at each
 * corresponding method. However, the tests performed on the retrieve method is
 * substantially more complex and will be described in much more detail. This
 * test is described below.
 * </p>
 * <p>
 * <i>Retrieval Method Test</i><br>
 * This tests the asynchronous retrieval of service information from the
 * ServiceExt object. The steps it takes are as follows:
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
public class ServiceImplCannedTest extends SICannedConcreteTest
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
    public ServiceImplCannedTest()
    {
        super("ServiceImplCannedTest");
    }

    /**
     * Creates a test suite to for use in the testing environment.
     * 
     * @return An InterfaceTestSuite that contains the tests in this test case.
     */
    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(ServiceImplCannedTest.class);
        suite.setName(ServiceImpl.class.getName());

        InterfaceTestSuite isuite = ServiceTest.isuite();
        isuite.addFactory(new ImplFactory()
        {
            public Object createImplObject() throws Exception
            {
                ServiceImplCannedTest test = new ServiceImplCannedTest();
                test.setUp();
                ServiceDescription sd = new ServiceDescription();
                sd.name = test.name.getValue("eng");
                sd.type = test.serviceType;
                sd.sourceId = test.sourceId;
                return new ServicePair(test.service, sd);
            }
        });

        suite.addTest(isuite);
        return suite;
    }

    /**
     * Sets up our test
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        handle = new ServiceHandleImpl(5);
        name = new MultiString(new String[] { "eng" }, new String[] { "serviceOne" });
        multiple = false;
        serviceType = ServiceType.DIGITAL_TV;
        serviceNumber = 2;
        minorNumber = 5;
        sourceId = 750;
        locator = new OcapLocator(sourceId);
        service = new ServiceImpl(sic, handle, name, multiple, serviceType, serviceNumber, minorNumber, locator, null);
    }

    /**
     * Clean up after the test
     */
    protected void tearDown() throws Exception
    {
        service = null;
        handle = null;
        name = null;
        serviceType = null;
        locator = null;

        super.tearDown();
    }

    // Test Section \\

    /**
     * Tests the constructor and the associated getter methods for proper
     * setting and retrieval of data.
     */
    public void testConstructor()
    {
        assertEquals("Handle does not match", handle, service.getServiceHandle());
        assertEquals("Name does not match", name.getValue(null), service.getName());
        assertEquals("hasMultipleInstances does not match", multiple, service.hasMultipleInstances());
        assertEquals("ServiceType does not match", serviceType, service.getServiceType());
        assertEquals("ServiceNumber does not match", serviceNumber, service.getServiceNumber());
        assertEquals("MinorNumber does not match", minorNumber, service.getMinorNumber());
        assertEquals("Locator does not match", locator, service.getLocator());
    }

    /**
     * Tests <code>equals</code> to make sure it correctly compares to exact
     * same transport stream objects.
     */
    public void testEquals() throws Exception
    {
        assertTrue("Equals failed on test against itself", service.equals(service));
        // Test equals with different handle
        ServiceHandleImpl diffHandle = new ServiceHandleImpl(99);
        ServiceImpl service2 = new ServiceImpl(sic, diffHandle, name, multiple, serviceType, serviceNumber,
                minorNumber, locator, null);
        assertFalse("Equals should fail with different handle", service.equals(service2));
        // Test with different name
        service2 = new ServiceImpl(sic, handle, new MultiString(new String[] { "eng" }, new String[] { "foo" }),
                multiple, serviceType, serviceNumber, minorNumber, locator, null);
        assertFalse("Equals should fail with different name", service.equals(service2));
        service2 = new ServiceImpl(sic, handle, null, multiple, serviceType, serviceNumber, minorNumber, locator, null);
        assertFalse("Equals should fail with different name", service.equals(service2));
        // Test with different multiple flag
        service2 = new ServiceImpl(sic, handle, name, true, serviceType, serviceNumber, minorNumber, locator, null);
        assertFalse("Equals should fail with different multiple flag", service.equals(service2));
        // Test with different serviceType
        service2 = new ServiceImpl(sic, handle, name, multiple, ServiceType.DATA_APPLICATION, serviceNumber,
                minorNumber, locator, null);
        assertFalse("Equals should fail with different service type", service.equals(service2));
        // Test with different service number
        service2 = new ServiceImpl(sic, handle, name, multiple, serviceType, 40, minorNumber, locator, null);
        assertFalse("Equals should fail with different service number", service.equals(service2));
        // Test with different minor number
        service2 = new ServiceImpl(sic, handle, name, multiple, serviceType, serviceNumber, 88, locator, null);
        assertFalse("Equals should fail with different minor number", service.equals(service2));
        // Test with different locator
        service2 = new ServiceImpl(sic, handle, name, multiple, serviceType, serviceNumber, minorNumber,
                new OcapLocator(56), null);
        assertFalse("Equals should fail with different locator", service.equals(service2));
    }

    /**
     * Test getName returns an empty string when the name sent in is null
     */
    public void testGetNameNullNameValue()
    {

        ServiceImpl service2 = new ServiceImpl(sic, handle, null, multiple, serviceType, serviceNumber, minorNumber,
                locator, null);
        assertTrue("Empty string not returned for name", service2.getName().equals(""));
    }

    /**
     * Tests <code>hashCode</code> to make sure it returns a proper hash code.
     */
    public void testHashCode() throws Exception
    {
        assertEquals("Hashcode does not match on same object.", service.hashCode(), service.hashCode());
        ServiceImpl service2 = new ServiceImpl(sic, handle, name, multiple, serviceType, serviceNumber, minorNumber,
                new OcapLocator(7), null);
        assertFalse("Hashcode should not be the same with different objects.",
                service.hashCode() == service2.hashCode());
    }

    /**
     * Tests <code>retrieveDetails</code> to make sure it properly returns the
     * service details associated with this service. This test is performed
     * asynchronously to simulate the response of the SIDatabase.
     */
    public void testRetrieveDetails() throws Exception
    {
        SIRequestorTest requestor = new SIRequestorTest();
        tune(csidb.transportStream7);
        csidb.service15.retrieveDetails(requestor);
        requestor.waitForEvents(waitForRequest);
        assertTrue("Requestor was not notified of success", requestor.succeeded());
        SIRetrievable[] results = requestor.getResults();
        assertNotNull("Returned results should not be null", results);
        assertTrue("Results returned were not of the right type", results[0] instanceof ServiceDetails);
    }

    // Data Section \\

    /**
     * Holds the instance of the ServiceExt object we are testing.
     */
    private ServiceImpl service;

    private ServiceHandleImpl handle;

    private MultiString name;

    private boolean multiple;

    private ServiceType serviceType;

    private int serviceNumber;

    private int minorNumber;

    private int sourceId;

    private OcapLocator locator;
}
