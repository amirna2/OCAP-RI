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
 * StorageProxyTest.java
 * Created: Jun 16, 2008
 */

// Declare Package.
package org.ocap.storage;

// Import JUnit classes.
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.dvb.application.AppID;

/* Import support for CallerContext based testing. */
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.application.ApplicationTest.CCMgr;
import org.cablelabs.impl.manager.application.ApplicationTest.DummyContext;

/**
 * This class is the JUnit test case for the org.ocap.storage.StorageProxy
 * class.
 * 
 * @author Mark Millard
 */
public class StorageProxyTest extends TestCase
{
    // The name of the LogicalStorageVolume to test.
    private static final String LSV_NAME = "TestVolume";

    // The storage manager.
    private StorageManager m_storageMgr;

    /**
     * A constructor for the org.ocap.storage.StorageProxy test case.
     * 
     * @param name
     *            The name of the test case.
     */
    public StorageProxyTest(String name)
    {
        super(name);
    }

    /**
     * Test the allocateGeneralPurposeVolume() method on StorageProxy.
     * <p>
     * The test expects that at least one storage proxy exists and that a volume
     * can be created on it. The new volume is deleted if the test is
     * successful.
     * </p>
     * 
     * @throws Exception
     */
    public void testAllocateGeneralPurposeVolume() throws Exception
    {
        MyDummyContext cc = new MyDummyContext();
        cc.runInContextSync(new Runnable()
        {
            public void run()
            {
                // Create a logical volume on an internal device.
                StorageProxy[] proxies = m_storageMgr.getStorageProxies();
                if (proxies.length == 0)
                    fail("Expected at leaset one storage proxy device as configured in the mpeenv.ini.");

                ExtendedFileAccessPermissions fap = new ExtendedFileAccessPermissions(true, true, true, true, true,
                        true, null, null);
                assertNotNull("Unable to create ExtendedFileAccessPermissions.", fap);
                LogicalStorageVolume lsv = null;
                try
                {
                    lsv = proxies[0].allocateGeneralPurposeVolume(LSV_NAME, fap);
                }
                catch (IOException e)
                {
                    fail("IOException thrown ");
                }
                if (lsv != null)
                {
                    String path = lsv.getPath();
                    assertNotNull("Invalid volume path: " + path);

                    // Validate that the last element in the path is LSV_NAME.
                    assertTrue(path.endsWith(LSV_NAME));

                    // Clean up by removing logical storage volume.
                    proxies[0].deleteVolume(lsv);
                }
                else
                    fail("Unable to allocate general purpose volume.");
            }
        });
    }

    /**
     * This test is used to verify the functionality of initialize call for
     * detachable devices where contents must be detstroyed The initialize call
     * should wipe all previous volumes from the system
     * 
     * @throws Exception
     */
    public void testInitialize() throws Exception
    {
        MyDummyContext cc = new MyDummyContext();
        cc.runInContextSync(new Runnable()
        {
            public void run()
            {
                // Create a logical volume on a deatchable device.
                StorageProxy[] proxies = m_storageMgr.getStorageProxies();
                StorageProxy proxy = null;

                if (proxies.length == 0)
                    System.out.println("Expected at leaset one storage proxy device as configured in the mpeenv.ini.");
                for (int i = 0; i < proxies.length; i++)
                {
                    // Check to see if this is a detachable device, if not do
                    // nothing
                    StorageOption[] options = proxies[i].getOptions();
                    for (int j = 0; j < options.length; j++)
                    {
                        if (options[j] instanceof DetachableStorageOption)
                        {
                            proxy = proxies[i];
                            break;
                        }
                    }
                }
                assertNotNull("No detachable devices", proxy);
                ExtendedFileAccessPermissions fap = new ExtendedFileAccessPermissions(true, true, true, true, true,
                        true, null, null);
                assertNotNull("Unable to create ExtendedFileAccessPermissions.", fap);
                LogicalStorageVolume lsv = null;
                try
                {
                    lsv = proxy.allocateGeneralPurposeVolume(LSV_NAME, fap);
                }
                catch (IOException e)
                {
                    fail("IOException thrown ");
                }
                if (lsv != null)
                {
                    // Print out the volumes
                    LogicalStorageVolume[] lsvs = proxy.getVolumes();
                    for (int i = 0; i < lsvs.length; i++)
                    {
                        System.out.println("Volume " + i + " : " + lsvs[i].toString());
                    }

                    // Wipe everything
                    proxy.initialize(true);

                    // Verify that no LogicalStorageVolumes exist
                    lsvs = proxy.getVolumes();
                    if (lsvs != null && lsvs.length != 0)
                    {
                        for (int i = 0; i < lsvs.length; i++)
                        {
                            System.out.println("Volume " + i + " : " + lsvs[i].toString());
                        }
                        fail("LogicalStorageVolumes found; length : " + lsvs.length);
                    }
                }
                else
                    fail("Unable to allocate general purpose volume.");
            }
        });
    }

    /**
     * Create a new Test suite.
     * 
     * @return A Junit <code>Test</code> instance is returned.
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite(StorageProxyTest.class);
        return suite;
    }

    /**
     * Create a new Test suite, configuring it with the specified tests.
     * 
     * @param tests
     *            The name of the tests to execute.
     * 
     * @return A Junit <code>Test</code> instance is returned.
     */
    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(StorageProxyTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new StorageProxyTest(tests[i]));
            return suite;
        }
    }

    /**
     * Initialize the test case.
     * 
     * @throws Exception
     */
    public void setUp() throws Exception
    {
        System.out.println("Setting up StorageProxyTest.");

        // Get the storage manager we will be testing aginst.
        m_storageMgr = StorageManager.getInstance();

        // Replace the CallerContextManager.
        replaceCCMgr();
    }

    /**
     * Tear down the test case.
     * 
     * @throws Exception
     */
    public void tearDown() throws Exception
    {
        System.out.println("Tearing down StorageProxyTest.");

        // XXX - Should remove any LSV if they still exist.

        // Restore the CallerContextManager.
        restoreCCMgr();
    }

    // Save the normal CallerContextManager.
    private CallerContextManager m_save;

    // The new CallerContextManager.
    private CCMgr m_ccmgr;

    // Replace the CallerContextManager.
    private void replaceCCMgr()
    {
        m_save = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        ManagerManagerTest.updateManager(CallerContextManager.class, CCMgr.class, false, m_ccmgr = new CCMgr(m_save));
    }

    // Restore the original CallerContextManager.
    private void restoreCCMgr()
    {
        if (m_save != null)
            ManagerManagerTest.updateManager(CallerContextManager.class, m_save.getClass(), true, m_save);
    }

    /**
     * The main entry point for the TestCase.
     * 
     * @param args
     *            Command line arguments.
     */
    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite(args));
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        System.exit(0);
    }

    /**
     * A dummy CallerContext for the StorageProxyTest.
     * <p>
     * This class is used to return the AppID parameters required to manager a
     * general purpose volume.
     * </p>
     * 
     * @author Mark Millard
     */
    public static class MyDummyContext extends DummyContext
    {
        // The organization identifier.
        private static final int ORG_ID = 0x00000001;

        // The application identifier.
        private static final int APP_ID = 0x00007000;

        public Object get(Object key)
        {
            if (CallerContext.APP_ID.equals(key)) return new AppID(ORG_ID, APP_ID);
            throw new UnsupportedOperationException();
        }
    }

}
