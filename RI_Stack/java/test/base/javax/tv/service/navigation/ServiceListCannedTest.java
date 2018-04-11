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

import java.util.Vector;

import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.Service;
import javax.tv.service.ServiceType;

import junit.textui.TestRunner;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.ocap.net.OcapLocator;

import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.service.CannedServiceMgr;
import org.cablelabs.impl.manager.service.CannedServicesDatabase;
import org.cablelabs.impl.service.NetworkLocator;
import org.cablelabs.impl.service.javatv.navigation.ServiceListImpl;
import org.cablelabs.test.SICannedInterfaceTest;

/**
 * @author Joshua Keplinger
 * @author Brian Greene
 */
public class ServiceListCannedTest extends SICannedInterfaceTest
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
    public ServiceListCannedTest()
    {
        super("ServiceListCannedTest", ServiceList.class, new CannedServiceListTestFactory());
    }

    /**
     * Creates our test suite to be used in the test.
     * 
     * @return a test suite to be run
     */
    public static InterfaceTestSuite isuite()
    {
        InterfaceTestSuite suite = new InterfaceTestSuite(ServiceListCannedTest.class);
        suite.setName(ServiceList.class.getName());
        suite.addFactory(new CannedServiceListTestFactory());
        return suite;
    }

    /**
     * Sets up our tests for a clean run each time.
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        sList = (ServiceList) createImplObject();
    }

    /**
     * Cleans up after each test that is run.
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
        sList = null;
    }

    // Test Section \\

    /**
     * Tests the ServiceList to make sure that it properly checks the contents
     * of the list for a service.
     */
    public void testContains()
    {
        assertTrue("List does not contain expected service", sList.contains(csidb.service15));
        assertTrue("List does not contain expected service", sList.contains(csidb.service19));
        assertTrue("List does not contain expected service", sList.contains(csidb.service32));

    }

    /**
     * Tests <code>createServiceIterator</code> to make sure it returns a
     * non-null reference to a ServiceIterator object.
     */
    public void testCreateServiceIterator()
    {
        assertNotNull("Returned reference should not be null", sList.createServiceIterator());
        assertTrue("Returned reference is not a ServiceIterator type",
                sList.createServiceIterator() instanceof ServiceIterator);
    }

    /**
     * Tests <code>equals</code> to make sure it properly equates two service
     * lists.
     */
    public void testEquals() throws Exception
    {
        assertTrue("Equals test failed when testing list against itself", sList.equals(sList));
        ServiceList newList = (ServiceList) createImplObject();
        assertTrue("Equals test failed when testing list against list with same contents", sList.equals(newList));
        assertTrue("Equals test failed when testing list against list with same contents", newList.equals(sList));

        // test against a name sorted list.
        ServiceList nameSort = sList.sortByName();
        assertFalse("Equals test should not pass when comparing 2 lists with same contents one sorted by name",
                newList.equals(nameSort));

        Vector v = new Vector();
        v.add(csidb.service16);
        v.add(csidb.service29);
        v.add(csidb.service19);
        ServiceList tempList = new ServiceListImpl(v, null);

        // test against a number sorted list.
        ServiceList numberSort = tempList.sortByNumber();
        assertFalse("Equals test should not pass when comparing 2 lists with same contents one sorted by number",
                tempList.equals(numberSort));

        // test 2 name sorted lists.
        ServiceList aList = (ServiceList) createImplObject();
        ServiceList bList = (ServiceList) createImplObject();
        aList = aList.sortByNumber();
        bList = bList.sortByNumber();

        assertTrue("2 of the same lists sorted by name should be equal.", aList.equals(bList));
        // test the reciprocal.
        assertTrue("2 of the same lists sorted by name should be equal.", bList.equals(aList));
        // test one against itself
        assertTrue("The same list should be equal to itself after sorting", aList.equals(aList));

        Locator[] locators = new Locator[] { new OcapLocator(100), new OcapLocator(103), new OcapLocator(110) };
        ServiceFilter filter = new LocatorFilter(locators);
        ServiceList filteredList = sList.filterServices(filter);
        assertFalse("The filtered list should not be equal to the non-filtered list.", filteredList.equals(aList));
        assertFalse("The filtered list should not be equal to the non-filtered list.", aList.equals(filteredList));
        assertTrue("The filtered list should be equal to itself", filteredList.equals(filteredList));
    }

    /**
     * Tests <code>filterServices</code> to make sure it properly returns a
     * service list that contains only the services that match the filter. All 4
     * ServiceFilter implementation filters are used for maximum test coverage.
     */
    // FIXME
    public void XXXtestFilterServices() throws Exception
    {
        Locator[] locators = new Locator[] { new OcapLocator(100), new OcapLocator(103), new OcapLocator(110) };
        ServiceFilter filter = new LocatorFilter(locators);
        ServiceList newList = sList.filterServices(filter);
        assertNotNull("Returned list should not be null", newList);
        assertTrue("Returned list does not contain expected service", newList.contains(csidb.service15));
        assertTrue("Returned list does not contain expected service", newList.contains(csidb.service18));
        assertTrue("Returned list does not contain expected service", newList.contains(csidb.service25));

        filter = new ServiceTypeFilter(ServiceType.DATA_BROADCAST);
        newList = sList.filterServices(filter);
        assertNotNull("Returned list should not be null", newList);
        assertTrue("Returned list does not contain expected service", newList.contains(csidb.service16));
        assertTrue("Returned list does not contain expected service", newList.contains(csidb.service17));

        filter = new SIElementFilter(csidb.serviceDetails33);
        newList = sList.filterServices(filter);
        assertNotNull("Returned list should not be null", newList);
        assertTrue("Returned list does not contain expected service", newList.contains(csidb.service15));

        newList = sList.filterServices(null);
        assertNotNull("Returned list should not be null", newList);
        assertTrue("Returned list does not contain expected service", newList.contains(csidb.service15));

        // TODO (Josh) Test filtering using a PreferenceFilter
    }

    /**
     * Tests <code>findService</code> to make sure it returns the right service
     * for the specified locator.
     */
    public void testFindService() throws Exception
    {
        Service service = sList.findService(new OcapLocator(100));
        assertEquals("Service returned does not match expected service", csidb.service15, service);
        service = sList.findService(new OcapLocator(110));
        assertEquals("Service returned does not match expected service", csidb.service25, service);

        assertNull("Expected null with OcapLocator pointing to non-existent Service",
                sList.findService(new OcapLocator(999)));

        try
        {
            sList.findService(new NetworkLocator("network://0x1.0x345"));
        }
        catch (InvalidLocatorException expected)
        {
            // We want this to happen
        }
    }

    /**
     * Tests getService to make sure it returns the correct service at the given
     * index.
     */
    public void testGetService()
    {
        ServiceList newList = sList.sortByName();
        assertEquals("First service found is not the service expected", csidb.jmfService1, newList.getService(0));
        assertEquals("Last service found is not the service expected", csidb.service23, newList.getService(22));
        // Now we'll try to throw an exception
        try
        {
            newList.getService(-1);
            fail("IndexOutOfBoundsException should have been thrown with -1 as index");
        }
        catch (IndexOutOfBoundsException expected)
        {
            // We want this to happen
        }
    }

    /**
     * Tests <code>hashCode</code> to make sure it returns a consistent hash
     * code each time.
     */
    public void testHashCode() throws Exception
    {
        assertTrue("Hash code should be the same on the same object", sList.hashCode() == sList.hashCode());
        ServiceList newList = sList.sortByName();
        assertFalse("Hash code should not be same on different objects", sList.hashCode() == newList.hashCode());
        newList = sList.sortByNumber();
        assertFalse("Hash code should not be same on different objects", sList.hashCode() == newList.hashCode());
    }

    /**
     * Tests indexOf to make sure it returns the correct index of the provided
     * service.
     */
    public void testIndexOf() throws Exception
    {
        ServiceList newList = sList.sortByNumber();
        assertEquals("First service found is not the service expected", 3, newList.indexOf(csidb.service15));
        assertEquals("Last service found is not the service expected", 20, newList.indexOf(csidb.service25));
    }

    /**
     * Tests <code>size</code> to make sure it returns the correct size.
     */
    public void testSize()
    {
        assertEquals("Size does not match expected value", 23, sList.size());
        assertEquals("Size does not match expected value", 23, sList.size());
    }

    /**
     * Tests <code>sortByName</code> to make sure it properly creates a new
     * service list sorted by service name.
     */
    public void testSortByName()
    {
        ServiceList newList = sList.sortByName();
        assertEquals("First service found is not the service expected", csidb.jmfService1, newList.getService(0));
        assertEquals("Last service found is not the service expected", csidb.service23, newList.getService(22));
    }

    /**
     * Tests <code>sortByNumber</code> to make sure it properly creates a new
     * service list sorted by service number.
     */
    public void testSortByNumber() throws Exception
    {
        ServiceList newList = sList.sortByNumber();
        assertEquals("First service found is not the service expected", CannedServicesDatabase.abs1,
                newList.getService(0));
        assertEquals("Last service found is not the service expected", csidb.service25, newList.getService(20));
    }

    // Data Section \\

    /**
     *
     */
    private ServiceList sList;

    /**
     * This is a default factory class that is passed to the
     * <code>CannedSICacheTest</code>. It is used to instantiate a concrete
     * class to be used in the test.
     * 
     * @author Joshua Keplinger
     */
    protected static class CannedServiceListTestFactory implements ImplFactory
    {

        public Object createImplObject() throws Exception
        {
            CannedServiceMgr sm = (CannedServiceMgr) ManagerManager.getInstance(ServiceManager.class);
            return sm.createSIManager().filterServices(null);
        }

        public String toString()
        {
            return "CannedServiceListTestFactory";
        }
    }
}
