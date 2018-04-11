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

package org.ocap.storage;

import org.dvb.application.AppID;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.application.ApplicationTest.CCMgr;
import org.cablelabs.impl.manager.application.ApplicationTest.DummyContext;
import org.cablelabs.impl.storage.StorageManagerImpl;
import org.cablelabs.impl.storage.StorageProxyImpl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class RemovableStorageOptionTest extends TestCase
{

    public RemovableStorageOptionTest(String name)
    {
        super(name);
    }

    /**
     * This is a basic test to see if we can mock up for the Java layer a way to
     * simulate re-inserting the media from the previous test and test by way of
     * viewing the volumes
     * 
     * This test is expected to be run after the test below. The medium in the
     * proxy should be removed at start.
     * 
     * This test should not be run with any other tests dealing with Storage
     * Manager afterwards
     * 
     */
    public void testInsert()
    {
        System.out.println("********* STARTED : testAddDevice() *********");

        // Register a listener
        StorageManagerEventListener smel = new StorageManagerEventListener();
        m_storageMgr.addStorageManagerListener(smel);

        // Get a StorageProxy for the removable storage device, if any
        final String proxyName = "R1";
        StorageProxy proxy = getProxyByName(proxyName);
        int nativeHandle = ((StorageProxyImpl) proxy).getNativeHandle();

        // Mimic a call that the device has been added back
        ((StorageManagerImpl) m_storageMgr).asyncEvent(StorageManagerEvent.STORAGE_PROXY_CHANGED, nativeHandle,
                StorageProxy.READY);

        while (smel.getEvent() == null)
        {
            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException e1)
            {
                fail("Failed to fall asleep");
            }
        }

        // Get the StorageOptions for the StorageProxy
        StorageOption options[] = proxy.getOptions();

        // Find the RemovableStorageOption
        RemovableStorageOption rso = null;

        for (int i = 0; i < options.length; i++)
        {
            if (options[i] instanceof RemovableStorageOption)
            {
                rso = (RemovableStorageOption) options[i];
                break;
            }
        }

        if (rso == null) fail("getOptions did not return a RemovableStorageOption");

        System.out.println("Got RemovableStorageOption from StorageOptions");

        // isPresent is not used since this API is native dependent
        // Instead just verify volumes exist
        LogicalStorageVolume[] lsv = proxy.getVolumes();
        if (lsv == null || lsv.length == 0) fail("Volumes are not visible");

        System.out.println("Volumes still exist");

        int status = proxy.getStatus();
        if (status != StorageProxy.READY) fail("Device is not ready");
    }

    /**
     * This test shall eject a removable storage device if it exists A call to
     * isPresent() should be false. A recall to eject() should not affect the
     * state of isPresent().
     * 
     */
    public void testEjectPresent()
    {
        // Setup a listener
        StorageManagerEventListener smel = new StorageManagerEventListener();
        m_storageMgr.addStorageManagerListener(smel);

        // Get a StorageProxy for the removable storage device, if any
        final String proxyName = "R1";
        StorageProxy proxy = getProxyByName(proxyName);

        System.out.println("********* STARTED : testEjectPresent() *********");

        if (proxy == null)
        {
            fail("This IUT does not support removable storage");
            return;
        }

        System.out.println("Have StorageProxy for a removable storage device");

        // Get the StorageOptions for the StorageProxy
        StorageOption options[] = proxy.getOptions();

        System.out.println("Got StorageOptions for the StorageProxy");

        // Find the RemovableStorageOption
        RemovableStorageOption rso = null;

        for (int i = 0; i < options.length; i++)
        {
            if (options[i] instanceof RemovableStorageOption)
            {
                rso = (RemovableStorageOption) options[i];
                break;
            }
        }

        if (rso == null)
        {
            fail("getOptions did not return a RemovableStorageOption");
        }

        System.out.println("Got RemovableStorageOption from StorageOptions");

        // Verify that isPresent succeeds & returns true
        boolean mediaIsPresent = false;

        try
        {
            mediaIsPresent = rso.isPresent();
        }

        catch (Exception e)
        {
            e.printStackTrace();
            fail("isPresent threw an exception");
        }

        System.out.println("isPresent did not throw an exception");

        if (mediaIsPresent == false)
        {
            fail("isPresent returned false");
        }

        System.out.println("isPresent returned true");

        LogicalStorageVolume[] lsv = proxy.getVolumes();
        if (lsv == null || lsv.length == 0)
        {
            fail("Volumes are not visible");
        }

        System.out.println("Volumes still exist");

        int status = proxy.getStatus();
        if (status != StorageProxy.READY)
        {
            fail("Device is not ready");
        }
        smel.clearEventCache(); // Clear event listener's cache

        System.out.println("Device is ready");

        System.out.println("Eject the media from the device");

        rso.eject();

        while (smel.getEvent() == null)
        {
            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException e1)
            {
                fail("Failed to fall asleep");
            }
        }

        proxy = getProxyByName(proxyName);
        if (proxy == null)
        {
            fail("Storage Proxy should still exist");
        }

        try
        {
            mediaIsPresent = rso.isPresent();
        }

        catch (Exception e)
        {
            e.printStackTrace();
            fail("isPresent threw an exception");
        }

        System.out.println("isPresent did not throw an exception");

        if (mediaIsPresent == true)
        {
            fail("isPresent returned true");
        }
        lsv = proxy.getVolumes();
        if (lsv != null && lsv.length != 0)
        {
            fail("Volumes are still visible");
        }
        System.out.println("isPresent returned false and volumes are not visible");

        status = proxy.getStatus();
        if (status != StorageProxy.NOT_PRESENT)
        {
            fail("Device is not in the NOT_PRESENT state, instead " + status);
        }

        if (!smel.checkEvent(StorageManagerEvent.STORAGE_PROXY_CHANGED))
        {
            fail("Event retunred is incorrect");
        }
        smel.clearEventCache();

        System.out.println("Device is not present");

        // Recall to device
        rso.eject();

        proxy = getProxyByName(proxyName);
        if (proxy == null)
        {
            fail("Storage Proxy should still exist");
        }

        try
        {
            mediaIsPresent = rso.isPresent();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("isPresent threw an exception");
        }

        System.out.println("isPresent did not throw an exception");

        if (mediaIsPresent == true)
        {
            fail("isPresent returned true");
        }
        lsv = proxy.getVolumes();
        if (lsv != null && lsv.length != 0)
        {
            fail("Volumes are still visible");
        }
        System.out.println("isPresent returned false and volumes are not visible again");

        status = proxy.getStatus();
        if (status != StorageProxy.NOT_PRESENT)
        {
            fail("Device is not in the NOT_PRESENT state, instead " + status);
        }

        System.out.println("Device is not present");

        m_storageMgr.removeStorageManagerListener(smel);
    }

    private StorageProxy getProxyByName(String proxyName)
    {
        StorageProxy storageproxy = null;
        StorageProxy astorageproxy[] = m_storageMgr.getStorageProxies();
        if (astorageproxy == null)
        {
            return storageproxy;
        }
        for (int i = 0; i < astorageproxy.length; i++)
        {
            StorageProxy storageproxy1 = astorageproxy[i];
            String s1 = storageproxy1.getName();
            if (!proxyName.equals(s1)) continue;
            storageproxy = storageproxy1;
            break;
        }

        return storageproxy;
    }

    /**
     * Create a new Test suite.
     * 
     * @return A Junit <code>Test</code> instance is returned.
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite(RemovableStorageOptionTest.class);
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
            TestSuite suite = new TestSuite(RemovableStorageOptionTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new RemovableStorageOptionTest(tests[i]));
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

    // The storage manager.
    private StorageManager m_storageMgr;

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
