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
package org.cablelabs.impl.manager;

import junit.textui.TestRunner;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.cablelabs.test.SICannedInterfaceTest;

/**
 * <p>
 * CannedServiceManagerTest is an interface test on the abstract class
 * ServiceManager. Since it is only an interface test, it does not test beyond
 * what is exposed by this abstract class.
 * </p>
 * <p>
 * The ServiceManager abstract class consists of only getter methods, so there
 * will not be a detailed description of the testing performed on these.
 * However, due to the requirement of reliablility on these getter methods, it
 * should be known that they are thoroughly tested for their expected
 * functionality.
 * </p>
 * 
 * @author Joshua Keplinger
 */
public class ServiceManagerCannedTest extends SICannedInterfaceTest
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

    // Setup section \\

    /**
     * No-arg constructor for creating our test case.
     * 
     */
    public ServiceManagerCannedTest()
    {
        super("ServiceManagerCannedTest", ServiceManager.class, new CannedServiceManagerTestFactory());
    }

    /**
     * Creates a test suite to for use in the testing environment.
     * 
     * @return An InterfaceTestSuite that contains the tests in this test case.
     */
    public static InterfaceTestSuite isuite()
    {
        InterfaceTestSuite suite = new InterfaceTestSuite(ServiceManagerCannedTest.class);
        suite.setName(ServiceManager.class.getName());
        suite.addFactory(new CannedServiceManagerTestFactory());
        return suite;
    }

    /**
     * Sets up our test
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        manager = (ServiceManager) createImplObject();
    }

    /**
     * Clean up after the test
     */
    protected void tearDown() throws Exception
    {
        manager = null;

        super.tearDown();
    }

    // Test Section \\

    /**
     * Tests <code>createSIManager</code> to make sure it returns a valid
     * SIManager object.
     */
    public void testCreateSIManager()
    {
        // TODO (Josh) Implement
    }

    /**
     * Tests <code>getDecoderFactory</code> to make sure it returns a valid
     * DecoderFactory reference.
     */
    public void testGetDecoderFactory()
    {
        // TODO (Josh) Implement
    }

    /**
     * Tests <code>getServiceContextFactory</code> to make sure it returns a
     * valid ServiceContextFactory reference.
     */
    public void testGetServiceContextFactory()
    {
        // TODO (Josh) Implement
    }

    /**
     * Tests <code>getServicesDatabase</code> to make sure it returns a valid
     * ServicesDatabase reference.
     */
    public void testGetServicesDatabase()
    {
        // TODO (Josh) Implement
    }

    /**
     * Tests <code>getSICache</code> to make sure it returns a valid SICache
     * reference.
     */
    public void testGetSICache()
    {
        // TODO (Josh) Implement
    }

    /**
     * Tests <code>getSIDatabase</code> to make sure it returns a valid
     * SIDatabase reference.
     */
    public void testGetSIDatabase()
    {
        // TODO (Josh) Implement
    }

    /**
     * Tests <code>destroy</code> to make sure it properly destroys this
     * instance of ServiceManager.
     */
    public void testDestroy()
    {
        // TODO (Josh) Implement
    }

    // Data Section \\

    private ServiceManager manager;

    /**
     * This is a default factory class that is passed to the
     * <code>CannedServiceManagerTest</code>. It is used to instantiate a
     * concrete class to be used in the test.
     * 
     * @author Josh
     */
    private static class CannedServiceManagerTestFactory implements ImplFactory
    {

        public Object createImplObject() throws Exception
        {
            return (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        }

        public String toString()
        {
            return "CannedServiceManagerTestFactory";
        }
    }
}
