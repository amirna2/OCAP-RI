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

/**
 * 
 */
package javax.tv.service;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.cablelabs.test.SICannedConcreteTest;

/**
 * This class tests the following classes:
 * <ul>
 * <li>ServiceInformationType</li>
 * <li>ServiceType</li>
 * <li>SIChangeType</li>
 * <li>SIRequestFailureType</li>
 * </ul>
 * 
 * @author Joshua Keplinger
 * 
 */
public class PackageTypesCannedTest extends SICannedConcreteTest
{

    /**
     * @param name
     */
    public PackageTypesCannedTest(String name)
    {
        super(name);
    }

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
     * This simple creates a test suite containing the tests in this class.
     * 
     * @return A TestSuite object containing the tests in this class.
     */
    public static TestSuite suite()
    {
        TestSuite ts = new TestSuite(PackageTypesCannedTest.class);
        return ts;
    }

    // Test Section \\

    public void testServiceInformationTypeConstructor()
    {
        String type = "someType";
        ServiceInformationType sit = new ServiceInformationType(type);
        assertNotNull("ServiceInformationType creation failed - reference is null", sit);
        assertEquals("Type string does not match expected value", type, sit.toString());
    }

    public void testServiceInformationTypeToString()
    {
        String type = "someType";
        ServiceInformationType sit = new ServiceInformationType(type);
        assertEquals("Type string does not match expected value", type, sit.toString());
        assertEquals("Type string does not match expected value", type, sit.toString());
    }

    public void testServiceTypeConstructor()
    {
        String type = "someType";
        ServiceType st = new ServiceType(type);
        assertNotNull("ServiceType creation failed - reference is null", st);
        assertEquals("Type string does not match expected value", type, st.toString());
    }

    public void testServiceTypeToString()
    {
        String type = "someType";
        ServiceType st = new ServiceType(type);
        assertEquals("Type string does not match expected value", type, st.toString());
        assertEquals("Type string does not match expected value", type, st.toString());
    }

    public void testSIChangeTypeConstructor()
    {
        String type = "someType";
        SIChangeType sct = new SIChangeType(type);
        assertNotNull("SIChangeType creation failed - reference is null", sct);
        assertEquals("Type string does not match expected value", type, sct.toString());
    }

    public void testSIChangeTypeToString()
    {
        String type = "someType";
        SIChangeType sct = new SIChangeType(type);
        assertEquals("Type string does not match expected value", type, sct.toString());
        assertEquals("Type string does not match expected value", type, sct.toString());
    }

    public void testSIRequestFailureTypeConstructor()
    {
        String type = "someType";
        SIRequestFailureType srft = new SIRequestFailureType(type);
        assertNotNull("SIRequestFailureType creation failed - reference is null", srft);
        assertEquals("Type string does not match expected value", type, srft.toString());
    }

    public void testSIRequestFailureTypeToString()
    {
        String type = "someType";
        SIRequestFailureType srft = new SIRequestFailureType(type);
        assertEquals("Type string does not match expected value", type, srft.toString());
        assertEquals("Type string does not match expected value", type, srft.toString());
    }

}
