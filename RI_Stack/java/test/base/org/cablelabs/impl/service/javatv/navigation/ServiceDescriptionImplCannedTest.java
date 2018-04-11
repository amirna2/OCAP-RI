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

import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.cablelabs.impl.util.string.MultiString;
import org.cablelabs.test.SICannedConcreteTest;

/**
 * <p>
 * CannedServiceDescriptionExtTest tests the ServiceDescriptionExt interface.
 * Since this is an interface test, no methods beyond what ServiceDescriptionExt
 * exposes are tested. As ServiceDescriptionExt consists almost entirely of
 * getter methods, little space will be spent to describe these tests. Suffice
 * it to say, these methods are thoroughly tested to ensure they perform as
 * expected.
 * </p>
 * 
 * @author Joshua Keplinger
 */
public class ServiceDescriptionImplCannedTest extends SICannedConcreteTest
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
    public ServiceDescriptionImplCannedTest()
    {
        super("ServiceDescriptionImplCannedTest");
    }

    /**
     * Creates a test suite to for use in the testing environment.
     * 
     * @return An InterfaceTestSuite that contains the tests in this test case.
     */
    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(ServiceDescriptionImplCannedTest.class);
        suite.setName(ServiceDescriptionImpl.class.getName());
        return suite;
    }

    /**
     * Sets up our test
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        desc = "description";
        now = new Date();
        sDesc = new ServiceDescriptionImpl(sic, csidb.serviceDetails33, new MultiString(new String[] { "eng" },
                new String[] { "description" }), now, null);
    }

    /**
     * Clean up after the test
     */
    protected void tearDown() throws Exception
    {
        sDesc = null;

        super.tearDown();
    }

    // Test Section \\

    /**
     * Tests the constructor and the getters associated with it to make sure it
     * properly sets the values.
     */
    public void testConstructor()
    {
        assertEquals("Description does not match", desc, sDesc.getServiceDescription());
        assertEquals("Update time does not match", now, sDesc.getUpdateTime());
        assertEquals("Service details does not match", csidb.serviceDetails33, sDesc.getServiceDetails());
    }

    // Data Section \\

    /**
     * Holds the instance of the ServiceDescriptionExt object we are testing.
     */
    private ServiceDescriptionImpl sDesc;

    private String desc;

    private Date now;

}
