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

import java.io.IOException;

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

public class DetachableStorageOptionTest extends TestCase
{

    // The storage manager.
    // Setup a listener
    private StorageManager m_storageMgr;

    private static final int WAIT_TIME = 10000;

    private static int nativeHandle;

    public DetachableStorageOptionTest(String name)
    {
        super(name);
    }

    /**
     * This is a test in simulating adding a new storage device. After the the
     * device has been added, the associated proxy should be visible. The proxy
     * is then checks for volumes existing based on the state of the proxy.
     * 
     * This test is part 2 of 2 tests to run together
     */
    public void testAddDevice()
    {
        final String proxyName = "D1";

        System.out.println("********* STARTED : testAddDevice() *********");

        // Register a listener
        StorageManagerEventListener smel = new StorageManagerEventListener();
        m_storageMgr.addStorageManagerListener(smel);

        // Mimic a call that the device has been added back
        ((StorageManagerImpl) m_storageMgr).asyncEvent(StorageManagerEvent.STORAGE_PROXY_ADDED, nativeHandle,
                StorageProxy.OFFLINE);

        // Wait until we get an event back
        while (smel.getEvent() == null)
        {
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                fail("Sleep failed");
                e.printStackTrace();
            }
        }

        if (smel.checkEvent(StorageManagerEvent.STORAGE_PROXY_ADDED))
        {
            StorageProxy proxy = getProxyByName(proxyName);
            if (proxy == null) fail("Proxy was not found!");
            int status = proxy.getStatus();
            LogicalStorageVolume[] lsvs = proxy.getVolumes();
            try
            {
                if (status == StorageProxy.OFFLINE)
                {
                    if (lsvs.length != 0) fail("Volumes existing when in OFFLINE mode");
                }
                if (status == StorageProxy.READY)
                {
                    if (lsvs.length == 0) fail("Volumes not existing in READY STATE");
                }
            }
            catch (NullPointerException e)
            {
                fail("Empty arry was not returned, instead null");
            }
        }
        else
            fail("Failed to recieve back a STORARGE_PROXY_ADDED notification");

        // Deregister
        m_storageMgr.removeStorageManagerListener(smel);
    }

    /**
     * This test is to simulate removing the storage away from the current
     * implementation. This test verifies that the device has been successfully
     * removed by verification that it is non-existant in the StorageManager's
     * database.
     * 
     * This test is part 1 of 2 tests to run together. These two tests
     * (testRemoveDevice and testAddDevice) should run after all tests below
     */
    public void testRemoveDevice()
    {
        // Get the native handle
        final String proxyName = "D1";

        System.out.println("********* STARTED : testRemoveDevice() *********");

        StorageProxy proxy = getProxyByName(proxyName);
        nativeHandle = ((StorageProxyImpl) proxy).getNativeHandle();

        // Register listener
        StorageManagerEventListener smel = new StorageManagerEventListener();
        m_storageMgr.addStorageManagerListener(smel);

        // Make sure the event cache is cleared
        smel.clearEventCache();

        // Mimic a call to remove storage
        ((StorageManagerImpl) m_storageMgr).asyncEvent(StorageManagerEvent.STORAGE_PROXY_REMOVED, nativeHandle,
                StorageProxy.OFFLINE);

        // Wait until we get an event back
        while (smel.getEvent() == null)
        {
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                fail("Sleep failed");
                e.printStackTrace();
            }
        }

        if (smel.checkEvent(StorageManagerEvent.STORAGE_PROXY_REMOVED))
        {
            if (getProxyByName(proxyName) != null) fail("Proxy was found!");
        }
        else
            fail("Failed to recieve back a STORARGE_PROXY_REMOVED notification");

        // Deregister
        m_storageMgr.removeStorageManagerListener(smel);
    }

    /**
	 * 
	 * 
	 */
    public void testMakeDetachable()
    {
        // -S1 Get a StorageProxy for the detachable storage device, if any
        final String proxyName = "D1";
        StorageProxy proxy = getProxyByName(proxyName);

        StorageManagerEventListener smel = new StorageManagerEventListener();
        m_storageMgr.addStorageManagerListener(smel);

        System.out.println("********* STARTED : testMakeDetachable() *********");

        if (proxy == null)
        {
            assertNotNull("This IUT does not support detachable storage");
            return;
        }
        System.out.println("Have StorageProxy for a detachable storage device");

        // Get the StorageOptions for the StorageProxy
        StorageOption options[] = proxy.getOptions();

        System.out.println("Got StorageOptions for the StorageProxy");

        // Find the DetachableStorageOption
        DetachableStorageOption dso = null;

        for (int i = 0; i < options.length; i++)
        {
            if (options[i] instanceof DetachableStorageOption)
            {
                dso = (DetachableStorageOption) options[i];
                break;
            }
        }

        if (dso == null)
        {
            fail("getOptions did not return a DetachableStorageOption");
            return;
        }

        System.out.println("Got DetachableStorageOption from StorageOptions");

        smel.clearEventCache(); // zero cache

        // Make the StorageProxy detachable
        try
        {
            dso.makeDetachable();
        }

        catch (IOException e)
        {
            fail("makeDetachable failed");
            e.printStackTrace();
            return;
        }

        // The spec does not guarantee that makeDetachable will block
        // until the storage device becomes detachable. So wait a
        // while in case it doesn't
        try
        {
            Thread.sleep(WAIT_TIME);
        }

        catch (InterruptedException e)
        {
            fail("Thread.sleep(...) was interrupted");
            e.printStackTrace();
        }

        System.out.println("makeDetachable succeeded");

        if (!smel.checkEvent(StorageManagerEvent.STORAGE_PROXY_CHANGED))
        {
            fail("Event not generated or worng");
        }

        // Verify that either:
        boolean proxyStillExists = getProxyByName(proxyName) != null;

        if (proxyStillExists && proxy.getStatus() != StorageProxy.OFFLINE)
        {
            fail("StorageProxy still exists in StorageManager, but getStatus() did not return OFFLINE");
            return;
        }

        // StorageProxy still exists in StorageManager, and that it's status is
        // OFFLINE
        if (proxyStillExists)
        {
            System.out.println("StorageProxy still exists in StorageManager, & getStatus() returned OFFLINE as expected");
        }

        // or that StorageProxy no longer exists in StorageManager
        else
        {
            System.out.println("StorageProxy no longer exists in StorageManager as expected");
        }
        m_storageMgr.removeStorageManagerListener(smel);
    }

    public void testDetachableTrue()
    {
        final String proxyName = "D1";
        StorageProxy proxy = getProxyByName(proxyName);

        System.out.println("********* STARTED : testDetachableTrue() *********");

        if (proxy == null)
        {
            fail("This IUT does not support detachable storage");
            return;
        }
        System.out.println("Have StorageProxy for a detachable storage device");

        // Get the StorageOptions for the StorageProxy
        StorageOption options[] = proxy.getOptions();

        System.out.println("Got StorageOptions for the StorageProxy");

        // Find the DetachableStorageOption
        DetachableStorageOption dso = null;

        for (int i = 0; i < options.length; i++)
        {
            if (options[i] instanceof DetachableStorageOption)
            {
                dso = (DetachableStorageOption) options[i];
                break;
            }
        }

        if (dso == null)
        {
            fail("getOptions did not return a DetachableStorageOption");
            return;
        }

        System.out.println("Got DetachableStorageOption from StorageOptions");

        // Put the StorageProxy into the OFFLINE state by making it detachable
        try
        {
            dso.makeDetachable();
        }

        catch (IOException e)
        {
            fail("makeDetachable failed");
            return;
        }

        // The spec does not guarantee that makeDetachable will block
        // until the storage device becomes detachable. So wait a
        // while in case it doesn't
        try
        {
            Thread.sleep(WAIT_TIME);
        }

        catch (InterruptedException e)
        {
            e.printStackTrace();
            fail("Thread.sleep(...) was interrupted");
        }

        System.out.println("makeDetachable succeeded");

        // Verify that the StorageProxy still exists in StorageManager
        if (getProxyByName(proxyName) == null)
        {
            fail("StorageProxy not longer exists in StorageManager");
            return;
        }

        System.out.println("StorageProxy still exists in StorageManager");

        // Verify that the StorageProxy status is OFFLINE
        if (proxy.getStatus() != StorageProxy.OFFLINE)
        {
            fail("getStatus() did not return OFFLINE as expected");
            return;
        }

        System.out.println("getStatus() returned OFFLINE");

        // Verify that isDetachable succeeds and returns true
        boolean isDetachable = false;

        try
        {
            isDetachable = dso.isDetachable();
        }

        catch (Exception e)
        {
            e.printStackTrace();
            fail("isDetachable() threw an exception");
            return;
        }

        System.out.println("isDetachable did not throw and exception");

        if (isDetachable != true)
        {
            fail("isDetachable did not return true");
            return;
        }

        System.out.println("isDetachable returned true as expected");
    }

    public void testMakeReady()
    {
        final String proxyName = "D1";
        StorageProxy proxy = getProxyByName(proxyName);
        StorageManagerEventListener smel = new StorageManagerEventListener();
        m_storageMgr.addStorageManagerListener(smel);

        System.out.println("********* STARTED : testMakeReady() *********");

        if (proxy == null)
        {
            fail("This IUT does not support detachable storage");
            return;
        }
        System.out.println("Have StorageProxy for a detachable storage device");

        // Get the StorageOptions for the StorageProxy
        StorageOption options[] = proxy.getOptions();

        System.out.println("Got StorageOptions for the StorageProxy");

        // Find the DetachableStorageOption
        DetachableStorageOption dso = null;

        for (int i = 0; i < options.length; i++)
        {
            if (options[i] instanceof DetachableStorageOption)
            {
                dso = (DetachableStorageOption) options[i];
                break;
            }
        }

        if (dso == null)
        {
            fail("getOptions did not return a DetachableStorageOption");
            return;
        }

        smel.clearEventCache(); // zero cache

        System.out.println("Got DetachableStorageOption from StorageOptions");

        // Make the StorageProxy detachable
        try
        {
            dso.makeDetachable();
        }

        catch (IOException e)
        {
            e.printStackTrace();
            fail("makeDetachable threw an exception");
        }

        // The spec does not guarantee that makeDetachable will block
        // until the storage device becomes detachable. So wait a
        // while in case it doesn't
        try
        {
            Thread.sleep(WAIT_TIME);
        }

        catch (InterruptedException e)
        {
            e.printStackTrace();
            fail("Thread.sleep(...) after makeDetachable was interrupted");
        }

        if (!smel.checkEvent(StorageManagerEvent.STORAGE_PROXY_CHANGED))
        {
            fail("Event not generated or worng");
        }

        System.out.println("makeDetachable called");

        // Verify that the StorageProxy still exists in the StorageManager
        if (getProxyByName(proxyName) == null)
        {
            fail("StorageProxy no longer exists in StorageManager");
            return;
        }
        System.out.println("StorageProxy still exists in StorageManager");

        // -S1 Verify that the StorageProxy is OFFLINE
        if (proxy.getStatus() != StorageProxy.OFFLINE)
        {
            fail("proxy.getStatus() did not return OFFLINE");
            return;
        }
        System.out.println("proxy.getStatus() returned OFFLINE");

        LogicalStorageVolume[] lsvs = proxy.getVolumes();
        if (lsvs.length != 0)
        {
            fail("Proxy has volumes visible");
            return;
        }
        System.out.println("proxy.getVolumes() returned no volumes");

        // Verify that makeReady succeeds ...
        try
        {
            dso.makeReady();
            System.out.println("makeReady succeeded");
        }

        catch (Exception e)
        {
            fail("makeReady threw an unexpected exception");
            System.out.println(e.toString());
            return;
        }

        // The spec does not guarantee that makeReady will block until
        // the storage device is ready. So wait a while in case it
        // doesn't
        try
        {
            Thread.sleep(WAIT_TIME);
        }

        catch (InterruptedException e)
        {
            fail("Thread.sleep(...) after makeReady was interrupted");
        }

        if (!smel.checkEvent(StorageManagerEvent.STORAGE_PROXY_CHANGED))
        {
            fail("Event not generated or worng");
        }

        // ... and that the StorageProxy is READY
        if (proxy.getStatus() != StorageProxy.READY)
        {
            fail("proxy.getStatus(), after makeReady, did not return READY as expected");
            return;
        }
        System.out.println("proxy.getStatus(), after makeReady, returned READY as expected");

        lsvs = proxy.getVolumes();
        if (lsvs.length == 0)
        {
            fail("Proxy has no volumes visible");
            return;
        }
        System.out.println("proxy.getVolumes() returned volumes");

        m_storageMgr.removeStorageManagerListener(smel);
    }

    public void testReadyWhenReady()
    {
        final String proxyName = "D1";
        StorageProxy proxy = getProxyByName(proxyName);
        System.out.println("********* STARTED : testReadyWhenReady() *********");

        if (proxy == null)
        {
            fail("This IUT does not support detachable storage");
            return;
        }
        System.out.println("Have StorageProxy for a detachable storage device");

        // Get the StorageOptions for the StorageProxy
        StorageOption options[] = proxy.getOptions();

        System.out.println("Got StorageOptions for the StorageProxy");

        // Find the DetachableStorageOption
        DetachableStorageOption dso = null;

        for (int i = 0; i < options.length; i++)
        {
            if (options[i] instanceof DetachableStorageOption)
            {
                dso = (DetachableStorageOption) options[i];
                break;
            }
        }

        if (dso == null)
        {
            fail("getOptions did not return a DetachableStorageOption");
            return;
        }
        System.out.println("Got DetachableStorageOption from StorageOptions");

        // Verify that the StorageProxy is READY
        if (proxy.getStatus() != StorageProxy.READY)
        {
            fail("proxy.getStatus() did not return READY");
            return;
        }
        System.out.println("proxy.getStatus() returned READY");

        // Verify that makeReady succeeds ...
        try
        {
            dso.makeReady();
            System.out.println("makeReady succeeded");
        }

        catch (Exception e)
        {
            fail("makeReady threw an unexpected exception");
            System.out.println(e.toString());
            return;
        }

        // The spec does not guarantee that makeReady will block until
        // the storage device is ready. So wait a while in case it
        // doesn't
        try
        {
            Thread.sleep(WAIT_TIME);
        }

        catch (InterruptedException e)
        {
            fail("Thread.sleep(...) after makeReady was interrupted");
            e.printStackTrace();
        }

        // ... and that the StorageProxy is still READY
        if (proxy.getStatus() != StorageProxy.READY)
        {
            fail("proxy.getStatus(), after makeReady, did not return READY as expected");
            return;
        }
        System.out.println("proxy.getStatus(), after makeReady, returned READY as expected");
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
        TestSuite suite = new TestSuite(DetachableStorageOptionTest.class);
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
            TestSuite suite = new TestSuite(DetachableStorageOptionTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new DetachableStorageOptionTest(tests[i]));
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
