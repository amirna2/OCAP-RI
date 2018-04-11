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

package javax.tv.service.navigation;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.cablelabs.test.SICannedConcreteTest;

/**
 * This class tests the following classes:
 * <ul>
 * <li>DeliverySystemType</li>
 * <li>StreamType</li>
 * <li>FilterNotSupportedException</li>
 * <li>SortNotAvailableException</li>
 * </ul>
 * Since the tests for these are extremely simple, they are being grouped into
 * one TestCase.
 * 
 * @author Joshua Keplinger
 * 
 */
public class PackageTypesAndExceptionsCannedTest extends SICannedConcreteTest
{

    public PackageTypesAndExceptionsCannedTest(String name)
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
        TestSuite ts = new TestSuite(PackageTypesAndExceptionsCannedTest.class);
        return ts;
    }

    // Test Section \\

    public void testDeliverySystemTypeConstructor()
    {
        String name = "type";
        DeliverySystemType dst = new DeliverySystemType(name);
        assertNotNull("Created DeliverySystemType is null", dst);
        assertEquals("Name doesn't match", name, dst.toString());

        try
        {
            dst = new DeliverySystemType(null);
            fail("NullPointerException should have been thrown");
        }
        catch (NullPointerException expected)
        {
            // Expected
        }
    }

    public void testDeliverySystemTypeToString()
    {
        String name = "type";
        DeliverySystemType dst = new DeliverySystemType(name);
        assertEquals("Name doesn't match", name, dst.toString());
        assertEquals("Name doesn't match", name, dst.toString());
    }

    public void testStreamTypeConstructor()
    {
        String name = "type";
        StreamType st = new StreamType(name);
        assertNotNull("Created StreamType is null", st);
        assertEquals("Name doesn't match", name, st.toString());

        try
        {
            st = new StreamType(null);
            fail("NullPointerException should have been thrown");
        }
        catch (NullPointerException expected)
        {
            // Expected
        }
    }

    public void testStreamTypeToString()
    {
        String name = "type";
        StreamType st = new StreamType(name);
        assertEquals("Name doesn't match", name, st.toString());
        assertEquals("Name doesn't match", name, st.toString());
    }

    public void testFilterNotSupportedExceptionConstructors()
    {
        String message = "message";
        FilterNotSupportedException ex = new FilterNotSupportedException();
        assertNull("Message should be null", ex.getMessage());

        ex = new FilterNotSupportedException(message);
        assertEquals("Message doesn't match", message, ex.getMessage());
    }

    public void testSortNotAvailableExceptionConstructors()
    {
        String message = "message";
        SortNotAvailableException ex = new SortNotAvailableException();
        assertNull("Message should be null", ex.getMessage());

        ex = new SortNotAvailableException(message);
        assertEquals("Message doesn't match", message, ex.getMessage());
    }

}
