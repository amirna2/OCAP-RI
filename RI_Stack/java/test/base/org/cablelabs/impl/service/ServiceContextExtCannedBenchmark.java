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
package org.cablelabs.impl.service;

import javax.tv.service.selection.NormalContentEvent;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextListener;

import junit.textui.TestRunner;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.davic.net.tuning.NetworkInterfaceManager;

import org.cablelabs.impl.davic.net.tuning.CannedNetworkInterfaceManager;
import org.cablelabs.impl.davic.net.tuning.NetworkInterfaceManagerImpl;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.service.CannedServiceMgr;
import org.cablelabs.test.SICannedInterfaceTest;

/**
 * This class performs benchmark measurements against objects that implement the
 * ServiceContextExt interface. These measurements are performed against the
 * canned JavaTV environment so they only measure execution of the code within
 * this environment. These measurements do not include the real amount of time
 * that would be required to perform operations outside of JavaTV (e.g. tuning,
 * media decoding, etc.)
 * 
 * @author Todd Earles
 */
public class ServiceContextExtCannedBenchmark extends SICannedInterfaceTest
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
    }

    /**
     * No-arg constructor for creating our test case.
     * 
     */
    public ServiceContextExtCannedBenchmark()
    {
        super("ServiceContextExtCannedBenchmark", ServiceContextExt.class, new CannedServiceContextExtTestFactory());
    }

    /**
     * Creates a test suite to for use in the testing environment.
     * 
     * @return An InterfaceTestSuite that contains the tests in this test case.
     */
    public static InterfaceTestSuite isuite()
    {
        InterfaceTestSuite suite = new InterfaceTestSuite(ServiceContextExtCannedBenchmark.class);
        suite.setName(ServiceContextExt.class.getName());
        suite.addFactory(new CannedServiceContextExtTestFactory());
        return suite;
    }

    /**
     * Sets up our test
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        ManagerManagerTest.updateManager(NetworkInterfaceManager.class, CannedNetworkInterfaceManager.class, true, null);
        context = (ServiceContextExt) createImplObject();
    }

    /**
     * Clean up after the test
     */
    protected void tearDown() throws Exception
    {
        context = null;

        ManagerManagerTest.updateManager(NetworkInterfaceManager.class, NetworkInterfaceManagerImpl.class, true, null);

        super.tearDown();
    }

    /**
     * Benchmark normal service selection with no delay by any external objects.
     */
    public void testBenchmarkSelect() throws Exception
    {
        // Setup the listener
        ServiceContextListener scl = new ServiceContextListener()
        {
            // Receive service context event
            public synchronized void receiveServiceContextEvent(ServiceContextEvent e)
            {
                // Set end time
                endTime = System.currentTimeMillis();

                // Make sure we got the correct event
                assertTrue("Incorrect event", e instanceof NormalContentEvent);

                // Display timing information
                long elapsed = endTime - startTime;
                System.out.println("Select() took " + elapsed + "ms");
                notify();
            }
        };
        context.addListener(scl);

        // Select a service and walk the ServiceContext through all states
        synchronized (scl)
        {
            // Invoke the garbage collector now so it does not run while
            // performing this benchmark.
            System.out.println("Waiting for GC...");
            System.gc();
            Thread.sleep(2000);
            System.gc();
            Thread.sleep(2000);
            System.gc();
            Thread.sleep(5000);

            // TODO(Todd): The canned behavior for NI and SMH is not quite
            // correct yet. Fix the code below once we have the canned methods
            // we need to control behavior.

            // Perform the test in a loop. The initial iterations will take
            // much longer because of class loading and the fact that the
            // JVM compiler has not had a chance to kick in yet.
            for (int i = 0; i < 20; i++)
            {
                startTime = System.currentTimeMillis();
                context.select(csidb.service15);
                scl.wait();
            }
        }
    }

    // Private fields
    private ServiceContextExt context;

    private long startTime, endTime;

    /**
     * This is a default factory class that is passed to the
     * <code>CannedServiceContextFactoryExtTest</code>. It is used to
     * instantiate a concrete class to be used in the test.
     */
    private static class CannedServiceContextExtTestFactory implements ImplFactory
    {

        public Object createImplObject() throws Exception
        {
            CannedServiceMgr manager = (CannedServiceMgr) ManagerManager.getInstance(ServiceManager.class);
            ServiceContextFactoryExt factory = (ServiceContextFactoryExt) manager.getServiceContextFactory();
            return factory.createServiceContext();
        }

        public String toString()
        {
            return "CannedServiceContextFactoryExtTestFactory";
        }
    }
}
