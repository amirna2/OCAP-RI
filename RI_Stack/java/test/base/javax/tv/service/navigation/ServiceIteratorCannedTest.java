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

import java.util.NoSuchElementException;

import javax.tv.service.SIManager;

import junit.textui.TestRunner;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.service.CannedServiceMgr;
import org.cablelabs.impl.manager.service.CannedServicesDatabase;
import org.cablelabs.test.SICannedInterfaceTest;

/**
 * @author Joshua Keplinger
 */
public class ServiceIteratorCannedTest extends SICannedInterfaceTest
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

    // Setup Section \\

    /**
     * No-arg constructor for creating our test case.
     * 
     */
    public ServiceIteratorCannedTest()
    {
        super("ServiceIteratorCannedTest", ServiceIterator.class, new CannedServiceIteratorTestFactory());
    }

    /**
     * Creates our test suite to be used in the test.
     * 
     * @return a test suite to be run
     */
    public static InterfaceTestSuite isuite()
    {
        InterfaceTestSuite suite = new InterfaceTestSuite(ServiceIteratorCannedTest.class);
        suite.setName(ServiceIterator.class.getName());
        suite.addFactory(new CannedServiceIteratorTestFactory());
        return suite;
    }

    /**
     * Sets up our tests for a clean run each time.
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        iterator = (ServiceIterator) createImplObject();
    }

    /**
     * Cleans up after each test that is run.
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();

        iterator = null;
    }

    // Test Section \\

    /**
     * Tests ServiceIterator to make sure it properly checks if it has a 'next'
     * service.
     */
    public void testHasNext()
    {
        iterator.toBeginning();
        assertTrue("Iterator should have next element when at beginning of list", iterator.hasNext());
        iterator.toEnd();
        assertFalse("Iterator should not have next element when at end of list", iterator.hasNext());
        iterator.toBeginning();
        int counter = 0;
        while (iterator.hasNext())
        {
            counter++;
            iterator.nextService();
        }
        assertEquals("Iterator did not go through correct number of list items", 23, counter);
    }

    /**
     * Tests ServiceIterator to make sure it properly checks if it has a
     * 'previous' service.
     */
    public void testHasPrevious()
    {
        iterator.toBeginning();
        assertFalse("Iterator should not have previous element when at beginning of list", iterator.hasPrevious());
        iterator.toEnd();
        assertTrue("Iterator should have previous element when at end of list", iterator.hasPrevious());
        int counter = 0;
        while (iterator.hasPrevious())
        {
            counter++;
            iterator.previousService();
        }
        assertEquals("Iterator did not go through correct number of list items", 23, counter);
    }

    /**
     * Tests ServiceIterator to make sure that it returns the next service
     * properly.
     */
    public void testNextService()
    {
        iterator.toBeginning();
        assertEquals("First service returned does not match expected service", CannedServicesDatabase.abs1,
                iterator.nextService());
        assertEquals("Second service returned does not match expected service", CannedServicesDatabase.abs2,
                iterator.nextService());
        iterator.toEnd();
        // Try to throw an exception
        try
        {
            iterator.nextService();
            fail("Exception should have been thrown while trying access pass end of list");
        }
        catch (NoSuchElementException expected)
        {
            // Expected
        }
    }

    /**
     * Tests ServiceIterator to make sure that it returns the previous service
     * properly.
     */
    public void testPreviousService()
    {
        iterator.toEnd();
        assertEquals("First service returned does not match expected service", csidb.jmfService2,
                iterator.previousService());
        assertEquals("First service returned does not match expected service", csidb.jmfService1,
                iterator.previousService());
        iterator.toBeginning();
        // Try to throw an exception
        try
        {
            iterator.previousService();
        }
        catch (NoSuchElementException expected)
        {
            // Expected
        }
    }

    /**
     * Tests ServiceIterator to make sure it properly returns to the beginning
     * of the the list.
     */
    public void testToBeginning()
    {
        iterator.toBeginning();
        assertFalse("List should not have previous element at beginning", iterator.hasPrevious());
    }

    /**
     * Tests ServiceIterator to make sure it properly goes to the end of the
     * list.
     */
    public void testToEnd()
    {
        iterator.toEnd();
        assertFalse("List should not have previous element at beginning", iterator.hasNext());
    }

    // Data Section \\

    /** The iterator object to test */
    private ServiceIterator iterator;

    /**
     * This is a default factory class that is passed to the
     * <code>CannedSICacheTest</code>. It is used to instantiate a concrete
     * class to be used in the test.
     * 
     * @author Joshua Keplinger
     */
    protected static class CannedServiceIteratorTestFactory implements ImplFactory
    {

        public Object createImplObject() throws Exception
        {
            CannedServiceMgr csm = (CannedServiceMgr) ManagerManager.getInstance(ServiceManager.class);
            ServiceList sList = ((SIManager) csm.createSIManager()).filterServices(null);
            return sList.sortByNumber().createServiceIterator();
        }

        public String toString()
        {
            return "CannedServiceIteratorTestFactory";
        }
    }
}
