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

import javax.tv.service.ServiceType;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.cablelabs.test.SICannedConcreteTest;

/**
 * Tests ServiceTypeFilter for proper functionality.
 * 
 * @author Joshua Keplinger
 */
public class ServiceTypeFilterCannedTest extends SICannedConcreteTest
{

    /**
     * Constructor taking the name of this test.
     * 
     * @param name
     *            The name of this test.
     */
    public ServiceTypeFilterCannedTest(String name)
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
        TestSuite ts = new TestSuite(ServiceTypeFilterCannedTest.class);
        return ts;
    }

    /**
     * Sets up the tests
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        type = ServiceType.DIGITAL_TV;
        filter = new ServiceTypeFilter(type);
    }

    /**
     * Cleans up after the tests
     */
    protected void tearDown() throws Exception
    {
        filter = null;
        type = null;

        super.tearDown();
    }

    // Test Section \\

    /**
     * Tests accept to make sure it properly accepts or rejects a service based
     * on the filter value.
     */
    public void testAccept()
    {
        assertTrue("Service should pass test", filter.accept(csidb.service15));
        assertTrue("Service should pass test", filter.accept(csidb.service18));
        assertFalse("Service should pass test", filter.accept(csidb.service16));
    }

    /**
     * Tests <code>getFilterValue</code> to make sure it returns the right
     * ServiceType that was passed to its constructor.
     */
    public void testGetFilterValue()
    {
        assertEquals("Returned filter value does not equal expected value", type, filter.getFilterValue());
        assertEquals("Returned filter value does not equal expected value", type, filter.getFilterValue());
    }

    // Data Section \\

    private ServiceTypeFilter filter;

    private ServiceType type;
}