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
import javax.tv.service.ServiceInformationType;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceManager;

import org.cablelabs.impl.manager.service.CannedSIDatabase.TransportStreamHandleImpl;
import org.cablelabs.test.SICannedConcreteTest;

/**
 * <p>
 * CannedTransportStreamExtTest is an interface test for testing the
 * TransportStreamExt interface using canned data and behavior from the canned
 * testing environment. Since TransportStreamExt consists of only a handful of
 * getter methods, there will not be a need for extensive description of these
 * tests. Let it be known, however, that these methods are thoroughly tested for
 * proper functionality.
 * </p>
 * 
 * @author Joshua Keplinger
 * @author Todd Earles
 */
public class TransportStreamImplCannedTest extends SICannedConcreteTest
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
    public TransportStreamImplCannedTest()
    {
        super("TransportStreamImplCannedTest");
    }

    /**
     * Creates a test suite to for use in the testing environment.
     * 
     * @return An InterfaceTestSuite that contains the tests in this test case.
     */
    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(TransportStreamImplCannedTest.class);
        suite.setName(TransportStreamImpl.class.getName());
        return suite;
    }

    /**
     * Sets up our test
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        handle = new TransportStreamHandleImpl(33);
        frequency = 3000;
        modformat = 10;
        tsid = 45;
        desc = "transportOne";
        infoType = ServiceInformationType.ATSC_PSIP;
        now = new Date();
        tStream = new TransportStreamImpl(sic, handle, csidb.transport1, frequency, modformat, csidb.network3, tsid,
                desc, infoType, now, null);
    }

    /**
     * Clean up after the test
     */
    protected void tearDown() throws Exception
    {
        tStream = null;
        handle = null;
        desc = null;
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
        assertEquals("Handle does not match", handle, tStream.getTransportStreamHandle());
        assertEquals("Frequency does not match", frequency, tStream.getFrequency());
        assertEquals("TransportStreamID does not match", tsid, tStream.getTransportStreamID());
        assertEquals("Description does not match", desc, tStream.getDescription());
        assertEquals("ServiceInformationType does not match", infoType, tStream.getServiceInformationType());
        assertEquals("Update time does not match", now, tStream.getUpdateTime());
        assertEquals("Tranport does not match", csidb.transport1, tStream.getTransport());
        assertEquals("Network does not match", csidb.network3, tStream.getNetwork());
    }

    /**
     * Tests <code>getLocator</code> to make sure it returns a locator that
     * correctly corresponds to this transport stream
     */
    public void testGetLocator()
    {
        assertTrue("Value returned was not a valid Locator", tStream.getLocator() instanceof Locator);
        assertEquals("Returned locators do not match.", tStream.getLocator(), tStream.getLocator());
    }

    /**
     * Tests <code>equals</code> to make sure it correctly compares to exact
     * same transport stream objects.
     */
    public void testEquals()
    {
        assertTrue("Equals should succeed when testing object against itself", tStream.equals(tStream));
        // Try equals with a different handle
        TransportStreamHandleImpl diffHandle = new TransportStreamHandleImpl(43);
        TransportStreamImpl tStream2 = new TransportStreamImpl(sic, diffHandle, csidb.transport1, frequency, modformat,
                csidb.network3, tsid, desc, infoType, now, null);
        assertFalse("Equals should fail with different handle", tStream.equals(tStream2));
        // Try equals with a different transport
        tStream2 = new TransportStreamImpl(sic, handle, csidb.transport2, frequency, modformat, csidb.network3, tsid,
                desc, infoType, now, null);
        assertFalse("Equals should fail with different transport", tStream.equals(tStream2));
        // Try equals with a different frequency
        tStream2 = new TransportStreamImpl(sic, handle, csidb.transport1, 1, modformat, csidb.network3, tsid, desc,
                infoType, now, null);
        assertFalse("Equals should fail with different frequency", tStream.equals(tStream2));
        // Try equals with a different modulation format
        tStream2 = new TransportStreamImpl(sic, handle, csidb.transport1, frequency, 72, csidb.network3, tsid, desc,
                infoType, now, null);
        assertFalse("Equals should fail with different frequency", tStream.equals(tStream2));
        // Try equals with a different network
        tStream2 = new TransportStreamImpl(sic, handle, csidb.transport1, frequency, modformat, csidb.network5, tsid,
                desc, infoType, now, null);
        assertFalse("Equals should fail with different network", tStream.equals(tStream2));
        // Try equals with a different tsid
        tStream2 = new TransportStreamImpl(sic, handle, csidb.transport1, frequency, modformat, csidb.network3, 3,
                desc, infoType, now, null);
        assertFalse("Equals should fail with different transport stream id", tStream.equals(tStream2));
        // Try equals with a different description
        tStream2 = new TransportStreamImpl(sic, handle, csidb.transport1, frequency, modformat, csidb.network3, tsid,
                "foo", infoType, now, null);
        assertFalse("Equals should fail with different description", tStream.equals(tStream2));
        // Try equals with a different service information type
        tStream2 = new TransportStreamImpl(sic, handle, csidb.transport1, frequency, modformat, csidb.network3, tsid,
                desc, ServiceInformationType.SCTE_SI, now, null);
        assertFalse("Equals should fail with different service information type", tStream.equals(tStream2));
        // Try equals with a different update time
        tStream2 = new TransportStreamImpl(sic, handle, csidb.transport1, frequency, modformat, csidb.network3, tsid,
                desc, infoType, new Date(System.currentTimeMillis() + 5555), null);
        assertFalse("Equals should fail with different update time", tStream.equals(tStream2));
    }

    /**
     * Tests <code>hashCode</code> to make sure it returns a proper hash code.
     */
    public void testHashCode()
    {
        assertEquals("Hashcode does not match on same object.", tStream.hashCode(), tStream.hashCode());
        /*
         * TransportStreamImpl tStream2 = new TransportStreamImpl(handle,
         * frequency, tsid, desc, infoType, now);
         * assertFalse("Hashcode should not be the same with different objects."
         * , tStream.hashCode() == tStream2.hashCode());
         */
    }

    /**
     * Tests <code>retrieveService()</code> method of DAVIC
     * <code>TransportStream</code>.
     */
    public void testDavicRetrieveService()
    {
        // Get the DAVIC transport stream
        TransportStreamImpl jts = csidb.transportStream7;
        NetworkInterface[] ni = NetworkInterfaceManager.getInstance().getNetworkInterfaces();
        org.davic.mpeg.TransportStream dts = jts.getDavicTransportStream(ni[0]);

        // Get a DAVIC service carried in this transport stream
        org.davic.mpeg.Service service = dts.retrieveService(2);
        assertEquals("Wrong service found", csidb.serviceDetails33.getDavicService(dts), service);
        assertEquals("Wrong program number in service", 2, service.getServiceId());
    }

    /**
     * Tests <code>retrieveServices()</code> method of DAVIC
     * <code>TransportStream</code>.
     */
    public void testDavicRetrieveServices()
    {
        // Get the DAVIC transport stream
        TransportStreamImpl jts = csidb.transportStream7;
        NetworkInterface[] ni = NetworkInterfaceManager.getInstance().getNetworkInterfaces();
        org.davic.mpeg.TransportStream dts = jts.getDavicTransportStream(ni[0]);

        // Get the DAVIC services for this transport stream
        org.davic.mpeg.Service[] services = dts.retrieveServices();
        assertEquals("Number of services incorrect", 5, services.length);
        assertEquals("Service 0 incorrect", csidb.serviceDetails33.getDavicService(dts), services[0]);
        assertEquals("Service 1 incorrect", csidb.serviceDetails41.getDavicService(dts), services[1]);
        assertEquals("Service 2 incorrect", csidb.serviceDetails49.getDavicService(dts), services[2]);
    }

    // Data Section \\

    /**
     * Holds the instance of the TransportStreamExt object we are testing.
     */
    private TransportStreamImpl tStream;

    private TransportStreamHandleImpl handle;

    private int frequency;

    private int modformat;

    private int tsid;

    private String desc;

    private ServiceInformationType infoType;

    private Date now;

}
