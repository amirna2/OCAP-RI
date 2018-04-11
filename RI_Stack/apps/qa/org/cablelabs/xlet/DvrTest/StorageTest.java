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

package org.cablelabs.xlet.DvrTest;

import java.io.IOException;
import java.util.Vector;

import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.storage.DetachableStorageOption;
import org.ocap.storage.LogicalStorageVolume;
import org.ocap.storage.RemovableStorageOption;
import org.ocap.storage.StorageManager;
import org.ocap.storage.StorageManagerEvent;
import org.ocap.storage.StorageOption;
import org.ocap.storage.StorageProxy;

//import org.cablelabs.impl.storage.StorageManagerImpl;
//import org.cablelabs.impl.storage.StorageProxyImpl;
import org.cablelabs.xlet.DvrTest.StorageManagerEventListener;

public class StorageTest extends DvrTest
{

    StorageManager m_storageMgr;

    int m_nativeHandle;

    public static final long WAIT_TIME = 10000;

    protected StorageTest(Vector locators)
    {
        super(locators);
        // Get the storage manager we will be testing aginst.
        m_storageMgr = StorageManager.getInstance();
    }

    /*
     * --------------------------------------------------------------------------
     * ----------------
     * 
     * @author Ryan
     * 
     * Global methods to be used in the test group
     * ------------------------------
     * ------------------------------------------------------------
     */

    void detachStorage(String storage)
    {
        StorageProxy proxy = getProxyByName(storage);
        StorageManagerEventListener smel = new StorageManagerEventListener();
        m_storageMgr.addStorageManagerListener(smel);

        System.out.println("********* STARTED : detachStorage() *********");

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
        if (getProxyByName(storage) == null)
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

        m_storageMgr.removeStorageManagerListener(smel);
    }

    void attachStorage(String storage)
    {
        StorageProxy proxy = getProxyByName(storage);
        StorageManagerEventListener smel = new StorageManagerEventListener();
        m_storageMgr.addStorageManagerListener(smel);

        System.out.println("********* STARTED : attachStorage() *********");

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

        LogicalStorageVolume[] lsvs = proxy.getVolumes();
        if (lsvs.length == 0)
        {
            fail("Proxy has no volumes visible");
            return;
        }
        System.out.println("proxy.getVolumes() returned volumes");

        m_storageMgr.removeStorageManagerListener(smel);
    }

    /*
     * Compilation issues with visibility to StorageProxyImpl and
     * StorageManagerImpl void addStorage(String storage) {
     * System.out.println("********* STARTED : addStorage() *********");
     * 
     * //Register a listener StorageManagerEventListener smel = new
     * StorageManagerEventListener();
     * m_storageMgr.addStorageManagerListener(smel);
     * 
     * //Mimic a call that the device has been added back
     * ((StorageManagerImpl)m_storageMgr
     * ).asyncEvent(StorageManagerEvent.STORAGE_PROXY_ADDED, m_nativeHandle,
     * StorageProxy.OFFLINE);
     * 
     * // Wait until we get an event back while(smel.getEvent()== null) { try {
     * Thread.sleep(1000); } catch (InterruptedException e) {
     * fail("Sleep failed"); e.printStackTrace(); } }
     * 
     * if(smel.checkEvent(StorageManagerEvent.STORAGE_PROXY_ADDED)) {
     * StorageProxy proxy = getProxyByName(storage); if(proxy == null)
     * fail("Proxy was not found!"); int status = proxy.getStatus();
     * LogicalStorageVolume[] lsvs = proxy.getVolumes(); try{ if(status ==
     * StorageProxy.OFFLINE){ if(lsvs.length != 0)
     * fail("Volumes existing when in OFFLINE mode"); } if(status ==
     * StorageProxy.READY) { if(lsvs.length == 0)
     * fail("Volumes not existing in READY STATE"); } } catch
     * (NullPointerException e) {
     * fail("Empty arry was not returned, instead null"); } } else
     * fail("Failed to recieve back a STORARGE_PROXY_ADDED notification");
     * 
     * 
     * // Deregister m_storageMgr.removeStorageManagerListener(smel); }
     * 
     * void removeStorage(String storage) {
     * System.out.println("********* STARTED : removeStorage() *********");
     * 
     * StorageProxy proxy = getProxyByName(storage); m_nativeHandle
     * =((StorageProxyImpl)proxy).getNativeHandle();
     * 
     * // Register listener StorageManagerEventListener smel = new
     * StorageManagerEventListener();
     * m_storageMgr.addStorageManagerListener(smel);
     * 
     * // Make sure the event cache is cleared smel.clearEventCache();
     * 
     * //Mimic a call to remove storage
     * ((StorageManagerImpl)m_storageMgr).asyncEvent
     * (StorageManagerEvent.STORAGE_PROXY_REMOVED, m_nativeHandle,
     * StorageProxy.OFFLINE);
     * 
     * // Wait until we get an event back while(smel.getEvent()== null) { try {
     * Thread.sleep(1000); } catch (InterruptedException e) {
     * fail("Sleep failed"); e.printStackTrace(); } }
     * 
     * if(smel.checkEvent(StorageManagerEvent.STORAGE_PROXY_REMOVED)) {
     * if(getProxyByName(storage) != null) fail("Proxy was found!"); } else
     * fail("Failed to recieve back a STORARGE_PROXY_REMOVED notification");
     * 
     * // Deregister m_storageMgr.removeStorageManagerListener(smel); }
     */

    void ejectMedia(String storage)
    {
        // Setup a listener
        StorageManagerEventListener smel = new StorageManagerEventListener();
        m_storageMgr.addStorageManagerListener(smel);
        StorageProxy proxy = getProxyByName(storage);

        System.out.println("********* STARTED : ejectMedia() *********");

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

        proxy = getProxyByName(storage);
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
        m_storageMgr.removeStorageManagerListener(smel);
    }

    StorageProxy getProxyByName(String proxyName)
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

    MediaStorageVolume getFirstMSVinProxy(String proxyName)
    {
        StorageProxy sp = getProxyByName(proxyName);
        LogicalStorageVolume[] lsvs = sp.getVolumes();
        for (int i = 0; i < lsvs.length; i++)
        {
            if (lsvs[i] instanceof MediaStorageVolume)
            {
                return (MediaStorageVolume) lsvs[i];
            }
        }
        System.out.println("No media storage volume found");
        return null;
    }

    void fail(String msg)
    {
        m_failed = TEST_FAILED;
        System.out.println(msg);
        m_failedReason = msg;
    }

    /*
     * --------------------------------------------------------------------------
     * -------------------
     * 
     * @author Ryan
     * 
     * Added VBI NotifyShell objects for automation
     * ------------------------------
     * ----------------------------------------------------------------
     */

    class addDevice extends EventScheduler.NotifyShell
    {
        public addDevice(String device, long time)
        {
            super(time);
            m_device = device;
        }

        public void ProcessCommand()
        {
            // addStorage(m_device);
            attachStorage(m_device);
        }

        String m_device = null;
    }

    class removeDevice extends EventScheduler.NotifyShell
    {
        public removeDevice(String device, long time)
        {
            super(time);
            m_device = device;
        }

        public void ProcessCommand()
        {
            detachStorage(m_device);
            // removeStorage(m_device);
        }

        String m_device = null;
    }

    class attachDevice extends EventScheduler.NotifyShell
    {
        public attachDevice(String device, long time)
        {
            super(time);
            m_device = device;
        }

        public void ProcessCommand()
        {
            attachStorage(m_device);
        }

        String m_device = null;
    }

    class detachDevice extends EventScheduler.NotifyShell
    {
        public detachDevice(String device, long time)
        {
            super(time);
            m_device = device;
        }

        public void ProcessCommand()
        {
            detachStorage(m_device);
        }

        String m_device = null;
    }

    class ejectMedium extends EventScheduler.NotifyShell
    {
        public ejectMedium(String device, long time)
        {
            super(time);
            m_device = device;
        }

        public void ProcessCommand()
        {
            ejectMedia(m_device);
        }

        String m_device = null;
    }

}
