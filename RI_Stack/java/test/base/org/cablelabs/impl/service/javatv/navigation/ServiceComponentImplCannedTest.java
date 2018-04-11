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
import javax.tv.service.SIException;
import javax.tv.service.ServiceInformationType;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.cablelabs.impl.manager.service.CannedSIDatabase.ServiceComponentHandleImpl;
import org.cablelabs.impl.util.string.MultiString;
import org.cablelabs.test.SICannedConcreteTest;

/**
 * <p>
 * CannedServiceComponentExtTest tests the ServiceComponentExt interface. Since
 * this is an interface test, no methods beyond what ServiceComponentExt exposes
 * are tested. As ServiceComponentExt consists almost entirely of getter
 * methods, little space will be spent to describe these tests. Suffice it to
 * say, these methods are thoroughly tested to ensure they perform as expected.
 * </p>
 * 
 * @author Joshua Keplinger
 */
public class ServiceComponentImplCannedTest extends SICannedConcreteTest
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
     */
    public ServiceComponentImplCannedTest()
    {
        super("ServiceComponentImplCannedTest");
    }

    /**
     * Creates a test suite to for use in the testing environment.
     * 
     * @return An InterfaceTestSuite that contains the tests in this test case.
     */
    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(ServiceComponentImplCannedTest.class);
        suite.setName(ServiceComponentImpl.class.getName());
        return suite;
    }

    /**
     * Sets up our test
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        handle = new ServiceComponentHandleImpl(10);
        pid = 1;
        tag = 2;
        carouselID = 3;
        name = new MultiString(new String[] { "eng" }, new String[] { "component10" });
        language = "eng";
        streamType = org.ocap.si.StreamType.MPEG_2_VIDEO;
        infoType = ServiceInformationType.ATSC_PSIP;
        now = new Date();
        sComp = new ServiceComponentImpl(sic, handle, csidb.serviceDetails33, pid, tag,
                ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, carouselID, name, language, streamType, infoType, now,
                null);
    }

    /**
     * Clean up after the test
     */
    protected void tearDown() throws Exception
    {
        sComp = null;
        infoType = null;
        now = null;
        handle = null;

        super.tearDown();
    }

    // Test Section \\

    /**
     * Tests the constructor and the getters that are directly associated with
     * the values passed to it.
     */
    public void testConstructor() throws Exception
    {
        // Check each of the getters to make sure the constructor (and getters)
        // work correctly.
        assertEquals("Handle does not match.", handle, sComp.getServiceComponentHandle());
        assertEquals("Pid does not match", pid, sComp.getPID());
        assertEquals("ComponentTag does not match", tag, sComp.getComponentTag());
        assertEquals("CarouselID does not match", carouselID, sComp.getCarouselID());
        assertEquals("Name does not match", name.getValue(null), sComp.getName());
        assertEquals("Language does not match", language, sComp.getAssociatedLanguage());
        assertEquals("Elementary StreamType does not match", streamType, sComp.getElementaryStreamType());
        assertEquals("ServiceInformationType does not match", infoType, sComp.getServiceInformationType());
        assertEquals("Update time does not match", now, sComp.getUpdateTime());
        assertEquals("StreamType does not match", javax.tv.service.navigation.StreamType.VIDEO, sComp.getStreamType());
        assertEquals("Service does not match", csidb.service15, sComp.getService());
        assertEquals("ServiceDetails does not match", csidb.serviceDetails33, sComp.getServiceDetails());
    }

    /**
     * Tests <code>getLocator</code> to make sure it returns a valid Locator and
     * that it returns the same one each time.
     */
    public void testGetLocator()
    {
        assertTrue("Value returned was not a valid Locator", sComp.getLocator() instanceof Locator);
        assertEquals("Returned locators do not match.", sComp.getLocator(), sComp.getLocator());
    }

    /**
     * Tests <code>getComponentTag</code> to make sure it properly returns a
     * component tag and throws an exception when one is not available.
     */
    public void testGetComponentTag()
    {
        // Try to get a valid tag
        try
        {
            assertEquals("Tag does not match.", tag, sComp.getComponentTag());
        }
        catch (SIException ex)
        {
            fail("SIException should not have been thrown with valid component tag");
        }
        // Now we'll try to throw an exception with an undefined component tag
        try
        {
            sComp = new ServiceComponentImpl(sic, handle, csidb.serviceDetails33, pid,
                    ServiceComponentImpl.COMPONENT_TAG_UNDEFINED, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                    carouselID, name, language, streamType, infoType, now, null);
            sComp.getComponentTag();
            fail("SEException should've been thrown with undefined component tag");
        }
        catch (SIException expected)
        {
            // We want this to happen, so do nothing here.
        }
    }

    /**
     * Tests <code>getCarouselID</code> to make sure it properly returns a
     * carousel ID and throws an exception when one is not available.
     */
    public void testGetCarouselID()
    {
        // Try to get a valid carousel ID
        try
        {
            assertEquals("Carousel ID does not match.", carouselID, sComp.getCarouselID());
        }
        catch (SIException ex)
        {
            fail("SIException should not have been thrown with valid carousel ID");
        }
        // Now we'll try to throw an exception with an undefined carousel ID
        try
        {
            sComp = new ServiceComponentImpl(sic, handle, csidb.serviceDetails33, pid, tag,
                    ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, name,
                    language, streamType, infoType, now, null);
            sComp.getCarouselID();
            fail("SEException should've been thrown with undefined carousel ID");
        }
        catch (SIException expected)
        {
            // We want this to happen, so do nothing here.
        }
    }

    /**
     * Tests <code>equals</code> to make sure it correctly compares to exact
     * same transport stream objects.
     */
    public void testEquals()
    {
        assertTrue("Equals test failed test against itself.", sComp.equals(sComp));
        // Test equals with a different handle
        ServiceComponentHandleImpl anotherHandle = new ServiceComponentHandleImpl(40);
        ServiceComponentImpl sComp2 = new ServiceComponentImpl(sic, anotherHandle, csidb.serviceDetails33, pid, tag,
                ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, carouselID, name, language, streamType, infoType, now,
                null);
        assertFalse("Equals test failed with different handle", sComp.equals(sComp2));
        // Test equals with a different service details
        sComp2 = new ServiceComponentImpl(sic, handle, csidb.serviceDetails39, pid, tag,
                ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, carouselID, name, language, streamType, infoType, now,
                null);
        assertFalse("Equals test failed with different service details", sComp.equals(sComp2));
        // Test equals with a different pid
        sComp2 = new ServiceComponentImpl(sic, handle, csidb.serviceDetails33, 29, tag,
                ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, carouselID, name, language, streamType, infoType, now,
                null);
        assertFalse("Equals test failed with different pid", sComp.equals(sComp2));
        // Test equals with a different tag
        sComp2 = new ServiceComponentImpl(sic, handle, csidb.serviceDetails33, pid, 78,
                ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, carouselID, name, language, streamType, infoType, now,
                null);
        assertFalse("Equals test failed with different component tag", sComp.equals(sComp2));
        // Test equals with a different carousel ID
        sComp2 = new ServiceComponentImpl(sic, handle, csidb.serviceDetails33, pid, tag,
                ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, 37, name, language, streamType, infoType, now, null);
        assertFalse("Equals test failed with different carousel ID", sComp.equals(sComp2));
        // Test equals with a different name
        sComp2 = new ServiceComponentImpl(sic, handle, csidb.serviceDetails33, pid, tag,
                ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, carouselID, new MultiString(new String[] { "eng" },
                        new String[] { "foo" }), language, streamType, infoType, now, null);
        assertFalse("Equals test failed with different name", sComp.equals(sComp2));
        // Test equals with a different language
        sComp2 = new ServiceComponentImpl(sic, handle, csidb.serviceDetails33, pid, tag,
                ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, carouselID, name, "foo", streamType, infoType, now,
                null);
        assertFalse("Equals test failed with different language", sComp.equals(sComp2));
        // Test equals with a different streamType
        sComp2 = new ServiceComponentImpl(sic, handle, csidb.serviceDetails33, pid, tag,
                ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, carouselID, name, language, (short) 53, infoType, now,
                null);
        assertFalse("Equals test failed with different streamType", sComp.equals(sComp2));
        // Test equals with a different Service
        sComp2 = new ServiceComponentImpl(sic, handle, csidb.serviceDetails33, pid, tag,
                ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, carouselID, name, language, streamType,
                ServiceInformationType.DVB_SI, now, null);
        assertFalse("Equals test failed with different Service", sComp.equals(sComp2));
        // Test equals with a different ServiceInformationType
        sComp2 = new ServiceComponentImpl(sic, handle, csidb.serviceDetails33, pid, tag,
                ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, carouselID, name, language, streamType,
                ServiceInformationType.DVB_SI, now, null);
        assertFalse("Equals test failed with different ServiceInformationType", sComp.equals(sComp2));
        // Test equals with a different update time
        sComp2 = new ServiceComponentImpl(sic, handle, csidb.serviceDetails33, pid, tag,
                ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, carouselID, name, language, streamType, infoType,
                new Date(System.currentTimeMillis() + 5555), null);
        assertFalse("Equals test failed with different update time", sComp.equals(sComp2));
    }

    /**
     * Tests <code>hashCode</code> to make sure it returns a proper hash code.
     */
    public void testHashCode()
    {
        assertEquals("Hash code is not consistent for same object.", sComp.hashCode(), sComp.hashCode());
        ServiceComponentImpl sComp2 = new ServiceComponentImpl(sic, handle, csidb.serviceDetails33, 29, tag,
                ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, carouselID, name, language, streamType, infoType, now,
                null);
        assertFalse("Hash code is same for different objects.", sComp.hashCode() == sComp2.hashCode());
    }

    // Data Section \\

    /**
     * Holds the instance of the ServiceComponentExt object we are testing.
     */
    private ServiceComponentImpl sComp;

    private ServiceComponentHandleImpl handle;

    private int pid;

    private int tag;

    private int carouselID;

    private MultiString name;

    private String language;

    private short streamType;

    private ServiceInformationType infoType;

    private Date now;

}
