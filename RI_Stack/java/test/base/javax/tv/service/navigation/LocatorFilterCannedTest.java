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

import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.ocap.net.OcapLocator;

import org.cablelabs.impl.manager.service.CannedServicesDatabase;
import org.cablelabs.test.SICannedConcreteTest;

/**
 * This tests LocatorFilter for proper functionality.
 * 
 * @author Joshua Keplinger
 */
public class LocatorFilterCannedTest extends SICannedConcreteTest
{

    /**
     * Default constructor
     */
    public LocatorFilterCannedTest(String name)
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
        TestSuite ts = new TestSuite(LocatorFilterCannedTest.class);
        return ts;
    }

    /**
     * Sets up the tests
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        loc1 = new OcapLocator(100);
        loc2 = new OcapLocator(103);
        loc3 = new OcapLocator(110);
        locators = new OcapLocator[] { loc1, loc2, loc3 };
        filter = new LocatorFilter(locators);
    }

    /**
     * Cleans up after the tests
     */
    protected void tearDown() throws Exception
    {
        loc1 = null;
        loc2 = null;
        loc3 = null;
        locators = null;
        filter = null;

        super.tearDown();
    }

    // Test Section \\

    /**
     * Tests the constructor to make sure it throws an exception properly.
     */
    public void testConstructorWithException() throws Exception
    {
        try
        {
            Locator loc = new OcapLocator(1000000, -1); // Transport stream
                                                        // locator
            Locator[] array = new Locator[] { loc };
            filter = new LocatorFilter(array);
            fail("The constructor with a bad array should have thrown an exception");
        }
        catch (InvalidLocatorException expected)
        {
            // We wanted this to happen
        }
    }

    /**
     * Tests <code>accept</code> to make sure it properly checks a service
     * against the filter.
     */
    public void testAccept() throws Exception
    {
        assertTrue("Service passed should match locator filter", filter.accept(csidb.service15));
        assertTrue("Service passed should match locator filter", filter.accept(csidb.service18));
        assertTrue("Service passed should match locator filter", filter.accept(csidb.service25));
        assertFalse("Wrong service should not match locator filter", filter.accept(csidb.service32));

        // Test using freq/prognum/modformat
        OcapLocator loc = new OcapLocator(5000, 101, 1);

        Locator[] locs = new Locator[] { loc };
        LocatorFilter newFilter = new LocatorFilter(locs);
        assertTrue("DynamicService should match filter", newFilter.accept(csidb.dynamicService1));

        locs = new Locator[] { CannedServicesDatabase.locator1 };
        newFilter = new LocatorFilter(locs);
        assertTrue("AbstractService should match filter", newFilter.accept(CannedServicesDatabase.abs1));
    }

    /**
     * Tests <code>getFilterValue</code> to make sure it returns the right array
     * of Locators that were passed into it.
     */
    public void testGetFilterValue()
    {
        Locator[] returnedLocators = filter.getFilterValue();
        for (int i = 0; i < 3; i++)
        {
            assertEquals("Locator at element " + i + "does not match expected value", locators[i], returnedLocators[i]);
        }
    }

    // Data Section \\

    private LocatorFilter filter;

    private OcapLocator loc1;

    private OcapLocator loc2;

    private OcapLocator loc3;

    private OcapLocator[] locators;
}
