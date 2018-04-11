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

/*
 * Created on Jul 6, 2005
 */
package org.cablelabs.impl.manager.appstorage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.dvb.application.AppID;

import org.cablelabs.impl.io.AsyncLoadCallback;
import org.cablelabs.impl.io.AsyncLoadHandle;
import org.cablelabs.impl.io.FileData;
import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.FileSysCommunicationException;
import org.cablelabs.impl.io.OpenFile;
import org.cablelabs.impl.manager.AppStorageManagerTest;
import org.cablelabs.impl.manager.AuthManager;
import org.cablelabs.impl.manager.FileManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.ManagerTest;
import org.cablelabs.impl.manager.AppStorageManager.ApiStorage;
import org.cablelabs.impl.manager.AppStorageManager.AppStorage;
import org.cablelabs.impl.manager.AppStorageManagerTest.DummyAuth;
import org.cablelabs.impl.manager.AppStorageManagerTest.HttpD;
import org.cablelabs.impl.manager.appstorage.AppDescriptionInfo;
import org.cablelabs.impl.signalling.AppEntry.IcTransportProtocol;
import org.cablelabs.impl.signalling.AppEntry.LocalTransportProtocol;
import org.cablelabs.impl.signalling.AppEntry.TransportProtocol;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.test.TestUtils;

/**
 * Tests AppStorageImpl.
 * 
 * @author Aaron Kamienski
 */
public class AppStorageImplTest extends TestCase
{
    /**
     * Test that getInstance() returns ok.
     */
    public void testGetInstance()
    {
        Manager mgr = AppStorageImpl.getInstance();
        try
        {
            assertNotNull("Should not return null", mgr);
            assertTrue("Expected to be instance of AppStorageImpl", mgr instanceof AppStorageImpl);
        }
        finally
        {
            mgr.destroy();
        }
    }

    private void updateCertificates(AppStorageImpl asm)
    {
        byte[] name = getName().getBytes();
        int n = (name.length + 19) / 20;
        byte[] bytes = new byte[n * 20];
        System.arraycopy(name, 0, bytes, 0, name.length);
        asm.updatePrivilegedCertificates(bytes);
    }

    /**
     * Used to test that getInstance()/ctor sets up baseDir and maxBytes as
     * expected.
     * 
     * @param asm
     *            the newly created AppStorageImpl
     * @param maxBytes
     *            expected maxBytes
     * @param baseDir
     *            expected baseDir
     * @param srcDir
     *            source of files to copy
     */
    private void checkMaxBytesBaseDir(AppStorageImpl asm, long maxBytes, File baseDir, File srcDir) throws Exception
    {
        updateCertificates(asm);

        assertTrue("Expected baseDir to be created in ctor", baseDir.exists());
        assertTrue("Expected baseDir to be a directory", baseDir.isDirectory());

        // Figure size of files
        AppDescriptionInfo info = AppStorageManagerTest.generateInfo(srcDir, null);
        long size = AppStorageManagerTest.totalSize(info);
        int nStores = (int) (maxBytes / size);
        assertTrue("Internal error - app " + srcDir + " too big for 1 store", size < maxBytes);

        // TODO: what if nStores is REALLY big? (Like 2000?) use a different
        // srcDir...
        // Or, we could duplicate the stuff in info.fileinfo?????

        Vector toDelete = new Vector();
        try
        {
            // Store an api/app
            String name = getName();
            int index = 0;
            String version = "" + (index++);
            assertTrue("Expected store to succeed - 0", asm.storeApi(name, version, 100, info, srcDir));
            // Retrieve an api/app
            ApiStorage api = asm.retrieveApi(name, version);
            if (api != null) toDelete.addElement(api);
            assertNotNull("Expected API to be retrieved", api);

            // Check baseDirectory is under baseDir
            File apiDir = api.getBaseDirectory();
            File parentDir = apiDir.getParentFile();
            assertNotNull("Expected api base dir to have a parent", parentDir);
            assertEquals("Expected api parent dir to be baseDir", baseDir, parentDir);

            // Repeatedly store files -- expect failure at maxBytes threshold
            for (; index < nStores; ++index)
            {
                version = "" + index;
                assertTrue("Expected store to succeed - " + index, asm.storeApi(name, version, 100, info, srcDir));

                toDelete.addElement(asm.retrieveApi(name, version));
            }

            version = "" + index;
            boolean success = asm.storeApi(name, version, 100, info, srcDir);
            if (success) toDelete.addElement(asm.retrieveApi(name, version));
            assertFalse("Expected store to fail at maxBytes threshold - " + index, success);
        }
        finally
        {
            for (Enumeration e = toDelete.elements(); e.hasMoreElements();)
            {
                ApiStorage api = (ApiStorage) e.nextElement();
                asm.deleteApi(api.getName(), api.getVersion());

                // Try and avoid mixing deletions...
                File dir = api.getBaseDirectory();
                if (dir != null)
                {
                    AppStorageManagerTest.waitForDeletion(dir, 10000 * toDelete.size());
                    if (dir.exists()) System.out.println("Directory was not deleted when we checked: " + dir);
                }
            }
        }
    }

    /**
     * Test ctor() with given parameters.
     */
    public void testConstructor() throws Exception
    {
        File baseDir = new File(testDir, "ctor");
        long maxBytes = 200 * 1024;

        try
        {
            assertFalse("Internal error - test " + baseDir + " should not exist yet", baseDir.exists());

            checkMaxBytesBaseDir(new AppStorageImpl(maxBytes, baseDir), maxBytes, baseDir, SRCDIR);
        }
        finally
        {
            deleteAll(baseDir);
        }
    }

    /**
     * Tests getInstance() with parameters defined by property.
     */
    public void testGetInstance_prop() throws Exception
    {
        long maxBytes = 180 * 1024;
        File baseDir = new File(testDir, "prop");

        MPEEnv.setEnv(AppStorageImpl.MAXBYTES_PROP, "" + maxBytes);
        MPEEnv.setEnv(BASEDIR_PROP, baseDir.getAbsolutePath());

        try
        {
            assertFalse("Internal error - test " + baseDir + " should not exist yet", baseDir.exists());

            checkMaxBytesBaseDir((AppStorageImpl) AppStorageImpl.getInstance(), maxBytes, baseDir, SRCDIR);
        }
        finally
        {
            deleteAll(baseDir);
        }
    }

    /**
     * Tests getInstance() with default values.
     */
    // NOTE: this test is skipped because it takes too long to fill up
    // storage...
    public void XtestGetInstance_default() throws Exception
    {
        MPEEnv.removeEnvOverride(BASEDIR_PROP);
        MPEEnv.removeEnvOverride(AppStorageImpl.MAXBYTES_PROP);

        File baseDir = new File(MPEEnv.getSystemProperty("OCAP.persistent.appstorage", "/syscwd"),
                DEFAULT_SUBDIR);
        long maxBytes = AppStorageImpl.DEFAULT_MAXBYTES;

        checkMaxBytesBaseDir((AppStorageImpl) AppStorageImpl.getInstance(), maxBytes, baseDir, SRCDIR3);
    }

    public void testNoPublicConstructors()
    {
        TestUtils.testNoPublicConstructors(AppStorageImpl.class);
    }

    /**
     * Test that orphaned files are deleted upon initialization.
     */
    public void testOrphanFiles() throws Exception
    {
        File baseDir = new File(testDir, "orphans");
        File dummy = new File(baseDir, "dummy");
        long maxBytes = 200 * 1024;

        try
        {
            assertFalse("Internal error - test " + baseDir + " should not exist yet", baseDir.exists());

            // Make baseDir
            baseDir.mkdirs();

            // Create a dummy subdir
            dummy.mkdirs();
            assertTrue("Expected new dir to be created", dummy.exists());

            AppStorageImpl asm = new AppStorageImpl(maxBytes, baseDir);
            asm.updatePrivilegedCertificates(new byte[20]);

            AppStorageManagerTest.waitForDeletion(dummy, 10000);
            assertFalse("Expected orphaned subdir to be purged", dummy.exists());
        }
        finally
        {
            deleteAll(dummy);
            deleteAll(baseDir);
        }
    }

    /**
     * Test that storage is re-read upon reboot.
     */
    public void testReloadOnReboot_api() throws Exception
    {
        File baseDir = new File(testDir, "reload1");
        long maxBytes = 200 * 1024;

        try
        {
            AppStorageImpl asm = new AppStorageImpl(maxBytes, baseDir);
            asm.updatePrivilegedCertificates(new byte[20]);
            File srcDir = SRCDIR;
            AppDescriptionInfo info = AppStorageManagerTest.generateInfo(srcDir, null);
            // long size = AppStorageManagerTest.totalSize(info);

            String name = "name";
            String ver = "ver";
            assertTrue("Expected storage to succeed", asm.storeApi(name, ver, 100, info, srcDir));

            // Verify that files are there
            ApiStorage api = asm.retrieveApi(name, ver);
            assertNotNull("Could not retrieve API", api);
            AppStorageManagerTest.checkFiles(api, info);

            // Destroy current asm
            asm.destroy();

            // "Reboot"...
            asm = new AppStorageImpl(maxBytes, baseDir);

            assertNull("Should not retrieve API before loaded", asm.retrieveApi(name, ver));

            // reload, with same certificates
            asm.updatePrivilegedCertificates(new byte[20]);

            // Verify that files are there
            api = asm.retrieveApi(name, ver);
            assertNotNull("Could not retrieve API", api);
            AppStorageManagerTest.checkFiles(api, info);
        }
        finally
        {
            deleteAll(baseDir);
        }
    }

    /**
     * Test that storage is re-read upon reboot.
     */
    public void testReloadOnReboot_app() throws Exception
    {
        File baseDir = new File(testDir, "reload2");
        long maxBytes = 200 * 1024;

        try
        {
            AppStorageImpl asm = new AppStorageImpl(maxBytes, baseDir);
            asm.updatePrivilegedCertificates(new byte[20]);
            File srcDir = SRCDIR;
            AppDescriptionInfo info = AppStorageManagerTest.generateInfo(srcDir, null);
            // long size = AppStorageManagerTest.totalSize(info);

            AppID id = new AppID(0x99999, 0x4567);
            int ver = 0;
            TransportProtocol[] tp = { new LocalTransportProtocol() };
            /*
            assertTrue("Expected storage to succeed", asm.storeApp(id, ver, 100, info, tp, srcDir.getAbsolutePath(),
                    true));
                    */

            // Verify that files are there
            AppStorage app = asm.retrieveApp(id, ver, "");
            assertNotNull("Could not retrieve App", app);
            AppStorageManagerTest.checkFiles(app, info);

            // Destroy current asm
            asm.destroy();

            // "Reboot"...
            asm = new AppStorageImpl(maxBytes, baseDir);

            assertNull("Should not retrieve app before loaded", asm.retrieveApp(id, ver, ""));

            // reload, with same certificates
            asm.updatePrivilegedCertificates(new byte[20]);

            // Verify that files are there
            app = asm.retrieveApp(id, ver, "");
            assertNotNull("Could not retrieve app", app);
            AppStorageManagerTest.checkFiles(app, info);
        }
        finally
        {
            deleteAll(baseDir);
        }
    }

    /**
     * Test that storage is purged upon reboot w/ different certificates.
     */
    public void testPurgeOnReboot() throws Exception
    {
        File baseDir = new File(testDir, "purge");
        long maxBytes = 200 * 1024;

        try
        {
            AppStorageImpl asm = new AppStorageImpl(maxBytes, baseDir);
            asm.updatePrivilegedCertificates("1234567890abcdeabcde".getBytes());
            File srcDir = SRCDIR;
            AppDescriptionInfo info = AppStorageManagerTest.generateInfo(srcDir, null);
            // long size = AppStorageManagerTest.totalSize(info);

            String name = "name";
            String ver = "ver";
            assertTrue("Expected storage to succeed", asm.storeApi(name, ver, 100, info, srcDir));

            // Verify that files are there
            ApiStorage api = asm.retrieveApi(name, ver);
            assertNotNull("Could not retrieve API", api);
            AppStorageManagerTest.checkFiles(api, info);
            File apiDir = api.getBaseDirectory();

            // Destroy current asm
            asm.destroy();

            // "Reboot"...
            asm = new AppStorageImpl(maxBytes, baseDir);

            assertNull("Should not retrieve API before loaded", asm.retrieveApi(name, ver));

            // reload, with different certificates
            asm.updatePrivilegedCertificates("1234567890abcdeabcxe".getBytes());

            // Verify that files are no longer there
            ApiStorage api2 = asm.retrieveApi(name, ver);
            assertNull("Expected previous storage to be purged", api2);
            AppStorageManagerTest.waitForDeletion(apiDir, 10000);
            assertFalse("Expected previous files to be deleted", apiDir.exists());
        }
        finally
        {
            deleteAll(baseDir);
        }
    }

    /**
     * Tests that apps are purged in order of date.
     * <ol>
     * <li>Store a mid-priority app.
     * <li>Fill storage with low-priority apps.
     * <li>Store a high-priority app.
     * <li>Verify that oldest low-priority app was purged, and nothing else.
     * </ol>
     */
    public void testPurgeByDate_app() throws Exception
    {
        File baseDir = new File(testDir, "date_app");
        long maxBytes = 300 * 1024;

        try
        {
            final AppStorageImpl asm = new AppStorageImpl(maxBytes, baseDir);
            asm.updatePrivilegedCertificates("1234567890abcdeabcde".getBytes());
            final File srcDir = SRCDIR;
            final AppDescriptionInfo info = AppStorageManagerTest.generateInfo(srcDir, null);
            final TransportProtocol[] tp = { new LocalTransportProtocol() };
            long size = AppStorageManagerTest.totalSize(info);
            AppID id = new AppID(0xcafe0001, 1);

            // Store one higher-priority app
            /*
            assertTrue("Unable to store one app (mid)", asm.storeApp(id, 2000, 2, info, tp, srcDir.getAbsolutePath(),
                    true));
                    */

            // Continue storing low-priority files until fails to do so
            long time = System.currentTimeMillis();
            int loops = 0;
            for (; loops < 1000; ++loops)
            {
                //boolean success = asm.storeApp(id, 1000 - loops, 1, info, tp, srcDir.getAbsolutePath(), true);
                //if (!success) break;
            }
            time = System.currentTimeMillis() - time;
            assertTrue("Expected at least two storage for test", loops > 0);
            assertTrue("Could not fill up storage for test", loops < 1000);

            // Just for the heck of it...
            System.out.println("storeApp() " + (size * loops * 1000.0) / time + " Bps");

            // Store something at a higher priority
            /*
            assertTrue("Expected to be able to store higher-priority app", asm.storeApp(id, 1500, 3, info, tp,
                    srcDir.getAbsolutePath(), true));
                    */

            // Expect first, mid-priority to be stored still
            AppStorage app = asm.retrieveApp(id, 2000, "");
            assertNotNull("Expect oldest, mid-priority, app to be retrieved", app);

            // Expect first entry to be purged
            app = asm.retrieveApp(id, 1000, "");
            assertNull("Expect oldest, low-priority, app to be purged", app);

            // Expect all other entries to be stored
            for (int i = 1; i < loops; ++i)
            {
                app = asm.retrieveApp(id, 1000 - i, "");
                assertNotNull("Expect later, low-priority, app to be retrieved (" + i + ")", app);
            }

            // Expect stored app to be retrieved
            app = asm.retrieveApp(id, 1500, "");
            assertNotNull("Expect last, high-priority, app to be retrieved", app);
        }
        finally
        {
            deleteAll(baseDir);
        }
    }

    /**
     * Tests that apis are purged in order of date.
     * <ol>
     * <li>Store a mid-priority api.
     * <li>Fill storage with low-priority apis.
     * <li>Store a high-priority api.
     * <li>Verify that oldest low-priority api was purged, and nothing else.
     * </ol>
     */
    public void testPurgeByDate_api() throws Exception
    {
        File baseDir = new File(testDir, "date_api");
        long maxBytes = 300 * 1024;

        try
        {
            final AppStorageImpl asm = new AppStorageImpl(maxBytes, baseDir);
            asm.updatePrivilegedCertificates("1234567890abcdeabcde".getBytes());
            final File srcDir = SRCDIR;
            final AppDescriptionInfo info = AppStorageManagerTest.generateInfo(srcDir, null);
            long size = AppStorageManagerTest.totalSize(info);
            String id = "date_api";

            // Store one higher-priority api
            assertTrue("Unable to store one api (mid)", asm.storeApi(id, "mid", 2, info, srcDir));

            // Continue storing low-priority files until fails to do so
            long time = System.currentTimeMillis();
            int loops = 0;
            for (; loops < 1000; ++loops)
            {
                boolean success = asm.storeApi(id, "low" + loops, 1, info, srcDir);
                if (!success) break;
            }
            time = System.currentTimeMillis() - time;
            assertTrue("Expected at least two storage for test", loops > 0);
            assertTrue("Could not fill up storage for test", loops < 1000);

            // Just for the heck of it...
            System.out.println("storeApi() " + (size * loops * 1000.0) / time + " Bps");

            // Store something at a higher priority
            assertTrue("Expected to be able to store higher-priority api", asm.storeApi(id, "high", 3, info, srcDir));

            // Expect first, mid-priority to be stored still
            ApiStorage api = asm.retrieveApi(id, "mid");
            assertNotNull("Expect oldest, mid-priority, api to be retrieved", api);

            // Expect first entry to be purged
            api = asm.retrieveApi(id, "low" + 0);
            assertNull("Expect oldest, low-priority, api to be purged", api);

            // Expect all other entries to be stored
            for (int i = 1; i < loops; ++i)
            {
                api = asm.retrieveApi(id, "low" + i);
                assertNotNull("Expect later, low-priority, api to be retrieved (" + i + ")", api);
            }

            // Expect stored api to be retrieved
            api = asm.retrieveApi(id, "high");
            assertNotNull("Expect last, high-priority, api to be retrieved", api);
        }
        finally
        {
            deleteAll(baseDir);
        }
    }

    /**
     * Tests that, following a reboot, apps that haven't been touched (all else
     * being equal) are purged first.
     * <ol>
     * <li>Store a mid-priority app.
     * <li>Fill storage with low-priority apps.
     * <li>Reboot.
     * <li>"Touch" oldest low-priority app.
     * <li>Store a high-priority app.
     * <li>Verify that 2nd oldest low-priority app was purged, and nothing else.
     * </ol>
     */
    public void testPurgeUntouchedSinceReboot_app() throws Exception
    {
        File baseDir = new File(testDir, "untouch_app");
        long maxBytes = 300 * 1024;

        try
        {
            AppStorageImpl asm = new AppStorageImpl(maxBytes, baseDir);
            final byte[] priv = "1234567890abcdeabcde".getBytes();
            asm.updatePrivilegedCertificates(priv);
            final File srcDir = SRCDIR;
            final AppDescriptionInfo info = AppStorageManagerTest.generateInfo(srcDir, null);
            final TransportProtocol[] tp = { new LocalTransportProtocol() };
            long size = AppStorageManagerTest.totalSize(info);
            AppID id = new AppID(0xcafe0001, 1);

            // Store one higher-priority app
            /*
            assertTrue("Unable to store one app (mid)", asm.storeApp(id, 1500, 2, info, tp, srcDir.getAbsolutePath(),
                    true));
                    */

            // Continue storing low-priority files until fails to do so
            long time = System.currentTimeMillis();
            int loops = 0;
            for (; loops < 1000; ++loops)
            {
                /*
                boolean success = asm.storeApp(id, 1000 - loops, 1, info, tp, srcDir.getAbsolutePath(), true);
                if (!success) break;
                */
            }
            time = System.currentTimeMillis() - time;
            assertTrue("Expected at least two storage for test", loops > 1);
            assertTrue("Could not fill up storage for test", loops < 1000);

            // Just for the heck of it...
            System.out.println("storeApp() " + (size * loops * 1000.0) / time + " Bps");

            // Destroy current asm
            asm.destroy();

            // "Reboot"...
            asm = new AppStorageImpl(maxBytes, baseDir);
            asm.updatePrivilegedCertificates(priv);

            // Expect everything previously stored to be available
            assertNotNull("Expected retrieve to succeed (mid)", asm.retrieveApp(id, 1500, ""));
            for (int i = 0; i < loops; ++i)
                assertNotNull("Expected retrieve to succeed (low:" + i + ")", asm.retrieveApp(id, 1000 - i, ""));

            // "Touch" oldest low-priority (but don't actually change priority!)
            /*
            assertTrue("Expected storeApp() to succeed in 'touching'", asm.storeApp(id, 1000 - 0, 1, info, tp,
                    srcDir.getAbsolutePath(), true));
                    */

            // Store something at a higher priority
            /*
            assertTrue("Expected to be able to store higher-priority app", asm.storeApp(id, 2000, 3, info, tp,
                    srcDir.getAbsolutePath(), true));
                    */

            // Expect first, mid-priority to be stored still
            AppStorage app = asm.retrieveApp(id, 1500, "");
            assertNotNull("Expect oldest, mid-priority, app to be retrieved", app);

            // Expect 2nd entry to be purged
            app = asm.retrieveApp(id, 1000 - 1, "");
            assertNull("Expect oldest untouched, low-priority, app to be purged", app);

            // Expect all other entries to be stored
            for (int i = 0; i < loops; ++i)
            {
                if (i == 1) continue;
                app = asm.retrieveApp(id, 1000 - i, "");
                assertNotNull("Expect later/touched, low-priority, app to be retrieved (" + i + ")", app);
            }

            // Expect stored app to be retrieved
            app = asm.retrieveApp(id, 2000, "");
            assertNotNull("Expect last, high-priority, app to be retrieved", app);
        }
        finally
        {
            deleteAll(baseDir);
        }
    }

    /**
     * Tests that, following a reboot, apps that haven't been touched (all else
     * being equal) are purged first. Even when storing a new app of the same
     * priority.
     * <ol>
     * <li>Store a mid-priority app.
     * <li>Fill storage with low-priority apps.
     * <li>Reboot.
     * <li>"Touch" oldest low-priority app.
     * <li>Store a low-priority app.
     * <li>Verify that 2nd oldest low-priority app was purged, and nothing else.
     * </ol>
     */
    public void testPurgeUntouchedSamePrioritySinceReboot_app() throws Exception
    {
        File baseDir = new File(testDir, "untouch_app");
        long maxBytes = 300 * 1024;

        try
        {
            AppStorageImpl asm = new AppStorageImpl(maxBytes, baseDir);
            final byte[] priv = "1234567890abcdeabcde".getBytes();
            asm.updatePrivilegedCertificates(priv);
            final File srcDir = SRCDIR;
            final AppDescriptionInfo info = AppStorageManagerTest.generateInfo(srcDir, null);
            final TransportProtocol[] tp = { new LocalTransportProtocol() };
            long size = AppStorageManagerTest.totalSize(info);
            AppID id = new AppID(0xcafe0001, 1);

            // Store one higher-priority app
            /*
            assertTrue("Unable to store one app (mid)", asm.storeApp(id, 1500, 2, info, tp, srcDir.getAbsolutePath(),
                    true));
                    */

            // Continue storing low-priority files until fails to do so
            long time = System.currentTimeMillis();
            int loops = 0;
            for (; loops < 1000; ++loops)
            {
                /*
                boolean success = asm.storeApp(id, 1000 - loops, 1, info, tp, srcDir.getAbsolutePath(), true);
                if (!success) break;
                */
            }
            time = System.currentTimeMillis() - time;
            assertTrue("Expected at least two storage for test", loops > 1);
            assertTrue("Could not fill up storage for test", loops < 1000);

            // Just for the heck of it...
            System.out.println("storeApp() " + (size * loops * 1000.0) / time + " Bps");

            // Destroy current asm
            asm.destroy();

            // "Reboot"...
            asm = new AppStorageImpl(maxBytes, baseDir);
            asm.updatePrivilegedCertificates(priv);

            // Expect everything previously stored to be available
            assertNotNull("Expected retrieve to succeed (mid)", asm.retrieveApp(id, 1500, ""));
            for (int i = 0; i < loops; ++i)
                assertNotNull("Expected retrieve to succeed (low:" + i + ")", asm.retrieveApp(id, 1000 - i, ""));

            // "Touch" oldest low-priority (but don't actually change priority!)
            /*
            assertTrue("Expected storeApp() to succeed in 'touching'", asm.storeApp(id, 1000 - 0, 1, info, tp,
                    srcDir.getAbsolutePath(), true));
                    */

            // Store something at the same priority
            /*
            assertTrue("Expected to be able to store same-priority app (untouched app is purged)", asm.storeApp(id,
                    2000, 1, info, tp, srcDir.getAbsolutePath(), true));
                    */

            // Expect first, mid-priority to be stored still
            AppStorage app = asm.retrieveApp(id, 1500, "");
            assertNotNull("Expect oldest, mid-priority, app to be retrieved", app);

            // Expect 2nd entry to be purged
            app = asm.retrieveApp(id, 1000 - 1, "");
            assertNull("Expect oldest untouched, low-priority, app to be purged", app);

            // Expect all other entries to be stored
            for (int i = 0; i < loops; ++i)
            {
                if (i == 1) continue;
                app = asm.retrieveApp(id, 1000 - i, "");
                assertNotNull("Expect later/touched, low-priority, app to be retrieved (" + i + ")", app);
            }

            // Expect stored app to be retrieved
            app = asm.retrieveApp(id, 2000, "");
            assertNotNull("Expect last, same-priority, app to be retrieved", app);
        }
        finally
        {
            deleteAll(baseDir);
        }
    }

    /**
     * Tests that apis are purged in order of date irrespective of being
     * touched. Essentially, the results should be the same as for
     * testPurgeByDate_api().
     * <ol>
     * <li>Store a mid-priority app.
     * <li>Fill storage with low-priority apps.
     * <li>Reboot.
     * <li>"Touch" oldest low-priority app (shouldn't matter -- all are
     * implicitly touched)
     * <li>Store a high-priority app.
     * <li>Verify that oldest low-priority app was purged, and nothing else.
     * </ol>
     */
    public void testPurgeUntouchedSinceReboot_api() throws Exception
    {
        File baseDir = new File(testDir, "untouch_api");
        long maxBytes = 300 * 1024;

        try
        {
            AppStorageImpl asm = new AppStorageImpl(maxBytes, baseDir);
            final byte[] priv = "1234567890abcdeabcde".getBytes();
            asm.updatePrivilegedCertificates(priv);
            final File srcDir = SRCDIR;
            final AppDescriptionInfo info = AppStorageManagerTest.generateInfo(srcDir, null);
            long size = AppStorageManagerTest.totalSize(info);
            String id = "date_api";

            // Store one higher-priority api
            assertTrue("Unable to store one api (mid)", asm.storeApi(id, "mid", 2, info, srcDir));

            // Continue storing low-priority files until fails to do so
            long time = System.currentTimeMillis();
            int loops = 0;
            for (; loops < 1000; ++loops)
            {
                boolean success = asm.storeApi(id, "low" + loops, 1, info, srcDir);
                if (!success) break;
            }
            time = System.currentTimeMillis() - time;
            assertTrue("Expected at least two storage for test", loops > 0);
            assertTrue("Could not fill up storage for test", loops < 1000);

            // Just for the heck of it...
            System.out.println("storeApi() " + (size * loops * 1000.0) / time + " Bps");

            // "Reboot"...
            asm = new AppStorageImpl(maxBytes, baseDir);
            asm.updatePrivilegedCertificates(priv);

            // Expect everything previously stored to be available
            assertNotNull("Expected retrieve to succeed (mid)", asm.retrieveApi(id, "mid"));
            for (int i = 0; i < loops; ++i)
                assertNotNull("Expected retrieve to succeed (low:" + i + ")", asm.retrieveApi(id, "low" + i));

            // "Touch" oldest low-priority (but don't actually change priority!)
            assertTrue("Expected storeApp() to succeed in 'touching'", asm.storeApi(id, "low" + 0, 1, info, srcDir));

            // Store something at a higher priority
            assertTrue("Expected to be able to store higher-priority api", asm.storeApi(id, "high", 3, info, srcDir));

            // Expect first, mid-priority to be stored still
            ApiStorage api = asm.retrieveApi(id, "mid");
            assertNotNull("Expect oldest, mid-priority, api to be retrieved", api);

            // Expect first entry to be purged
            api = asm.retrieveApi(id, "low" + 0);
            assertNull("Expect oldest, low-priority, api to be purged", api);

            // Expect all other entries to be stored
            for (int i = 1; i < loops; ++i)
            {
                api = asm.retrieveApi(id, "low" + i);
                assertNotNull("Expect later, low-priority, api to be retrieved (" + i + ")", api);
            }

            // Expect stored api to be retrieved
            api = asm.retrieveApi(id, "high");
            assertNotNull("Expect last, high-priority, api to be retrieved", api);
        }
        finally
        {
            deleteAll(baseDir);
        }
    }

    /**
     * Tests storeApp(), which should allow for simultaneous storage.
     */
    public void doTestStress_storeApp(File srcDir, final TransportProtocol[] tp, final String base) throws Exception
    {
        File baseDir = new File(testDir, "stress_app");
        long maxBytes = 300 * 1024;

        try
        {
            final AppStorageImpl asm = new AppStorageImpl(maxBytes, baseDir);
            asm.updatePrivilegedCertificates("1234567890abcdeabcde".getBytes());
            final AppDescriptionInfo info = AppStorageManagerTest.generateInfo(srcDir, null);
            long size = AppStorageManagerTest.totalSize(info);

            // Continue storing low-priority files until fails to do so
            long time = System.currentTimeMillis();
            int loops = 0;
            for (; loops < 1000; ++loops)
            {
                /*
                boolean success = asm.storeApp(new AppID(0xcafef00d, 0x1), 1 + loops, 1, info, tp, base, true);
                if (!success) break;
                */
            }
            time = System.currentTimeMillis() - time;
            assertTrue("Expected at least one storage for test", loops > 0);
            assertTrue("Could not fill up storage for test", loops < 1000);

            // Just for the heck of it...
            System.out.println("storeApp() " + (size * loops * 1000.0) / time + " Bps");

            // We can store loops files at a time...

            final int priorities[] = { 2, 25, 20, 50, 45, 75, 10, };
            int totalCount = priorities.length * loops * 2;
            final boolean success[] = new boolean[totalCount];
            final int finished[] = { totalCount };
            final AppStorage apps[] = new AppStorage[totalCount];
            int highest = 0;

            // Kick off storage...
            int count = 0;
            for (int i = 0; i < priorities.length; ++i)
            {
                final int priority = priorities[i];
                if (priority > highest) highest = priority;
                final int oid = 0xcafe0000 + priority;
                for (int j = 0; j < loops * 2; ++j)
                {
                    final int index = count++;
                    final int aid = j + 1;
                    // Asynchronously perform storage!
                    Thread t = new Thread("stress" + count + ":" + Integer.toHexString(aid) + ":" + priority)
                    {
                        public void run()
                        {
                            AppID id = new AppID(oid, aid);
                            /*
                            try
                            {
                                success[index] = asm.storeApp(id, 1, priority, info, tp, base, true);
                            }
                            catch (FileSysCommunicationException e)
                            {
                                e.printStackTrace();
                            }
                            apps[index] = asm.retrieveApp(id, 1);
                            synchronized (finished)
                            {
                                finished[0]--;
                                finished.notify();
                            }
                            */
                        }
                    };
                    t.start();
                }
            }

            // Wait for all operations to complete...
            long waitTime = time / loops * totalCount + 500 * totalCount;
            synchronized (finished)
            {
                while (finished[0] > 0)
                {
                    int tmp = finished[0];
                    finished.wait(waitTime);
                    assertFalse("Timeout [" + waitTime + "] waiting for something to complete (" + tmp + ")",
                            tmp == finished[0]);
                }
            }

            // Expect n=loops entries of AppID(0xcafe****) to be stored, and
            // nothing else!
            // Where **** is highest priority.
            int found = 0;
            count = 0;
            for (int i = 0; i < priorities.length; ++i)
            {
                int oid = 0xcafe0000 + priorities[i];
                for (int j = 0; j < loops * 2; ++j, ++count)
                {
                    int aid = j + 1;

                    AppID id = new AppID(oid, aid);
                    AppStorage app = asm.retrieveApp(id, 1, "");

                    // If cannot retrieve app, verify files have been deleted
                    if (app == null && apps[count] != null)
                    {
                        assertTrue("Should not have been able to retrieve if unsuccessful", success[count]);

                        // Since could no longer retrieve, previous app
                        // should've been deleted
                        assertFalse("Should not be able to lock purged app", apps[count].lock());
                        File dir = apps[count].getBaseDirectory();
                        if (dir != null)
                        {
                            AppStorageManagerTest.waitForDeletion(dir, waitTime);
                            assertFalse("Purged files should've been deleted", dir.exists());
                        }
                    }

                    // If highest priority, might be stored
                    if (priorities[i] == highest)
                    {
                        if (app != null)
                        {
                            ++found;

                            AppStorageManagerTest.checkFiles(app, info);
                        }
                    }
                    // If not highest priority, shouldn't be stored
                    else
                    {
                        // Expect files not to be stored
                        assertNull("Expected files to not be stored for priority=" + priorities[i] + " id=" + id, app);
                    }

                }
            }
            assertEquals("Unexpected number of apps stored", loops, found);
        }
        finally
        {
            deleteAll(baseDir);
        }
    }

    /**
     * Tests storeApp(), which should allow for simultaneous storage.
     */
    public void testStress_storeApp() throws Exception
    {
        File srcDir = SRCDIR;
        TransportProtocol[] tp = { new LocalTransportProtocol() };
        doTestStress_storeApp(srcDir, tp, srcDir.getAbsolutePath());
    }

    /**
     * Tests storeApi(), which should allow for simultaneous storage.
     */
    public void testStress_storeApi() throws Exception
    {
        File baseDir = new File(testDir, "stress_api");
        long maxBytes = 300 * 1024;

        try
        {
            final AppStorageImpl asm = new AppStorageImpl(maxBytes, baseDir);
            asm.updatePrivilegedCertificates("1234567890abcdeabcde".getBytes());
            final File srcDir = SRCDIR;
            final AppDescriptionInfo info = AppStorageManagerTest.generateInfo(srcDir, null);
            long size = AppStorageManagerTest.totalSize(info);

            // Continue storing low-priority files until fails to do so
            long time = System.currentTimeMillis();
            int loops = 0;
            for (; loops < 1000; ++loops)
            {
                boolean success = asm.storeApi("stress0", "" + loops, 1, info, srcDir);
                if (!success) break;
            }
            time = System.currentTimeMillis() - time;
            assertTrue("Expected at least one storage for test", loops > 0);
            assertTrue("Could not fill up storage for test", loops < 1000);

            // Just for the heck of it...
            System.out.println("storeApp() " + (size * loops * 1000.0) / time + " Bps");

            // We can store loops files at a time...

            final int priorities[] = { 2, 25, 20, 50, 45, 75, 10, };
            int totalCount = priorities.length * loops * 2;
            final boolean success[] = new boolean[totalCount];
            final int finished[] = { totalCount };
            final ApiStorage apis[] = new ApiStorage[totalCount];
            int highest = 0;

            // Kick off storage...
            int count = 0;
            for (int i = 0; i < priorities.length; ++i)
            {
                final int priority = priorities[i];
                if (priority > highest) highest = priority;
                final String baseId = "stress:" + priority + ":";
                for (int j = 0; j < loops * 2; ++j)
                {
                    final int index = count++;
                    final int aid = j + 1;
                    // Asynchronously perform storage!
                    Thread t = new Thread("stress" + count + ":" + Integer.toHexString(aid) + ":" + priority)
                    {
                        public void run()
                        {
                            String id = baseId + aid;
                            success[index] = asm.storeApi(id, "0", priority, info, srcDir);
                            apis[index] = asm.retrieveApi(id, "0");
                            synchronized (finished)
                            {
                                finished[0]--;
                                finished.notify();
                            }
                        }
                    };
                    t.start();
                }
            }

            // Wait for all operations to complete...
            long waitTime = time / loops * totalCount + 500 * totalCount;
            synchronized (finished)
            {
                while (finished[0] > 0)
                {
                    int tmp = finished[0];
                    finished.wait(waitTime);
                    assertFalse("Timeout [" + waitTime + "] waiting for something to complete (" + tmp + ")",
                            tmp == finished[0]);
                }
            }

            // Expect n=loops entries of AppID(0xcafe****) to be stored, and
            // nothing else!
            // Where **** is highest priority.
            int found = 0;
            count = 0;
            for (int i = 0; i < priorities.length; ++i)
            {
                final String baseId = "stress:" + priorities[i] + ":";
                for (int j = 0; j < loops * 2; ++j, ++count)
                {
                    int aid = j + 1;

                    String id = baseId + aid;
                    ApiStorage api = asm.retrieveApi(id, "0");

                    // If cannot retrieve app, verify files have been deleted
                    if (api == null && apis[count] != null)
                    {
                        assertTrue("Should not have been able to retrieve if unsuccessful", success[count]);

                        // Since could no longer retrieve, previous app
                        // should've been deleted
                        assertFalse("Should not be able to lock purged app", apis[count].lock());
                        File base = apis[count].getBaseDirectory();
                        if (base != null)
                        {
                            AppStorageManagerTest.waitForDeletion(base, waitTime);
                            assertFalse("Purged files should've been deleted", base.exists());
                        }
                    }

                    // If highest priority, might be stored
                    if (priorities[i] == highest)
                    {
                        if (api != null)
                        {
                            ++found;

                            AppStorageManagerTest.checkFiles(api, info);
                        }
                    }
                    // If not highest priority, shouldn't be stored
                    else
                    {
                        // Expect files not to be stored
                        assertNull("Expected files to not be stored for priority=" + priorities[i] + " id=" + id, api);
                    }

                }
            }
            assertEquals("Unexpected number of apis stored", loops, found);
        }
        finally
        {
            deleteAll(baseDir);
        }
    }

    /**
     * Tests storeApp(), which should allow for simultaneous storage. Test
     * storing the same app over and over again.
     */
    public void testStressSame_storeApp() throws Exception
    {
        File baseDir = new File(testDir, "stress_app");
        long maxBytes = 300 * 1024;

        try
        {
            final AppStorageImpl asm = new AppStorageImpl(maxBytes, baseDir);
            asm.updatePrivilegedCertificates("1234567890abcdeabcde".getBytes());
            final File srcDir = SRCDIR;
            final AppDescriptionInfo info = AppStorageManagerTest.generateInfo(srcDir, null);
            final TransportProtocol[] tp = { new LocalTransportProtocol() };
            long size = AppStorageManagerTest.totalSize(info);

            // Continue storing low-priority files until fails to do so
            long time = System.currentTimeMillis();
            int loops = 0;
            for (; loops < 1000; ++loops)
            {
                /*
                boolean success = asm.storeApp(new AppID(0xcafef00d, 0x1), 1 + loops, 1, info, tp,
                        srcDir.getAbsolutePath(), true);
                if (!success) break;
                */
            }
            time = System.currentTimeMillis() - time;
            assertTrue("Expected at least one storage for test", loops > 0);
            assertTrue("Could not fill up storage for test", loops < 1000);

            // Just for the heck of it...
            System.out.println("storeApp() " + (size * loops * 1000.0) / time + " Bps");

            // We can store loops files at a time...

            int totalCount = 5 * loops * 2;
            final int finished[] = { totalCount };
            final AppID id = new AppID(0xf00dcafe, 0x0909);

            // Kick off storage...
            for (int i = 0; i < totalCount; ++i)
            {
                // Asynchronously perform storage!
                Thread t = new Thread("stress-same" + i)
                {
                    public void run()
                    {
                        /*
                        try
                        {
                            asm.storeApp(id, 1, 55, info, tp, srcDir.getAbsolutePath(), true);
                        }
                        catch (FileSysCommunicationException e)
                        {
                            e.printStackTrace();
                        }
                        synchronized (finished)
                        {
                            finished[0]--;
                            finished.notify();
                        }
                        */
                    }
                };
                t.start();
            }

            // Wait for all operations to complete...
            long waitTime = time / loops * totalCount + 500 * totalCount;
            synchronized (finished)
            {
                while (finished[0] > 0)
                {
                    int tmp = finished[0];
                    finished.wait(waitTime);
                    assertFalse("Timeout [" + waitTime + "] waiting for something to complete (" + tmp + ")",
                            tmp == finished[0]);
                }
            }

            // Expect one app to be stored, and nothing else
            AppStorage app = asm.retrieveApp(id, 1, "");
            assertNotNull("Expected app to be stored!", app);
            AppStorageManagerTest.checkFiles(app, info);
        }
        finally
        {
            deleteAll(baseDir);
        }
    }

    /**
     * This method will purge storage and then attempt to fill up with the given
     * files. The number of apps needed to fill storage is returned.
     * 
     * @param asm
     *            the AppStorageImpl to use
     * @param rootDir
     *            the root dir for files to copy
     * @param tp
     *            the transport protocol to use
     * @param baseDir
     *            the tp-relative base dir
     * @param id
     *            the AppID to use (versions are based on loops)
     * @return number of apps needed to fill storage
     */
    private int fillUpStorage(AppStorageImpl asm, File rootDir, TransportProtocol[] tp, String baseDir, AppID id)
    {
        int loops = 0;

        // Ensure that everything is purged to start
        asm.updatePrivilegedCertificates(new byte[20]);
        asm.updatePrivilegedCertificates("1234567890abcdeabcde".getBytes());

        final AppDescriptionInfo info = AppStorageManagerTest.generateInfo(rootDir, null);

        // Continue storing low-priority files until fails to do so
        for (; loops < 1000; ++loops)
        {
            boolean success = false;
            /*
            try
            {
                success = asm.storeApp(id, 1 + loops, 1, info, tp, baseDir, true);
            }
            catch (FileSysCommunicationException e)
            {
                e.printStackTrace();
            }
            */
            if (!success) break;
        }
        assertTrue("Expected at least one storage for test", loops > 0);
        assertTrue("Could not fill up storage for test", loops < 1000);

        return loops;
    }

    /**
     * Tests storeApp() async when files aren't initially available, then become
     * available. Failure occurs on initial file access.
     */
    public void testAsyncRetry_http() throws Exception
    {
        File rootDir = SRCDIR;

        File baseDir = new File(testDir, "retry");
        long maxBytes = 300 * 1024;

        try
        {
            final AppStorageImpl asm = new AppStorageImpl(maxBytes, baseDir);
            final AppID id = new AppID(0xf00df00d, 1);

            // First, fill up storage, so we know what is there
            long time = System.currentTimeMillis();
            int loops = fillUpStorage(asm, rootDir, new TransportProtocol[] { new LocalTransportProtocol() },
                    rootDir.getAbsolutePath(), new AppID(0xcafecafe, 1));
            time = System.currentTimeMillis() - time;

            AppDescriptionInfo info = AppStorageManagerTest.generateInfo(rootDir, null);

            // Start HTTP server
            XHttpD httpd = (XHttpD) XHttpD.createInstance(rootDir, true);
            try
            {
                // Fail all HTTP requests
                httpd.setDeny(true);

                IcTransportProtocol ic = new IcTransportProtocol();
                ic.urls.add(httpd.getMyURL().toString());
                TransportProtocol[] tp = { ic };

                // Schedule apps for store via HTTP.
                for (int i = 0; i < loops * 2; ++i)
                {
                    int priority = i + 3;
                    if (priority > 255) priority = 255;
                    //assertTrue("Expected async storage to succeed", asm.storeApp(id, i, priority, null, tp, "/", false));
                }
                // Wait some time (kinda arbitrary)
                Thread.sleep((time + 500 * loops) * 2);
                // Make sure nothing is stored...
                for (int i = 0; i < loops * 2; ++i)
                {
                    AppStorage app = asm.retrieveApp(id, i, "");
                    assertNull("Did not expect files to be stored given IO failures", app);
                }

                // Finally enable HTTP requests
                httpd.setDeny(false);

                // Perform a single sync store to ensure that things work...
                //assertTrue("Expected sync storage to succeed", asm.storeApp(id, 2000, 2, info, tp, "/", true));

                // Schedule 1 app to kick things off (don't expect to be stored)
                //assertTrue("Expected async storage to succeed", asm.storeApp(id, 2001, 1, null, tp, "/", false));

                // Verify expected apps are stored
                for (int i = loops; i < loops * 2; ++i)
                {
                    AppStorage app = AppStorageManagerTest.waitApp(asm, id, i, 15000L * loops);
                    assertNotNull("Expected app to be stored: " + i, app);
                    app.lock();
                    try
                    {
                        AppStorageManagerTest.checkFiles(app, info);
                    }
                    finally
                    {
                        app.unlock();
                    }
                }
                for (int i = 0; i < loops; ++i)
                {
                    AppStorage app = asm.retrieveApp(id, i, "");
                    assertNull("Expected lower-priority app not to be stored: " + i, app);
                }
            }
            finally
            {
                httpd.shutdown();
            }
        }
        finally
        {
            deleteAll(baseDir);
        }
    }

    /**
     * Tests storeApp() async when files aren't initially available, then become
     * available. Failure occurs following <i>requests</i> accesses.
     * 
     * @param request
     *            number of accesses to allow (if zero, then none)
     */
    private void doTestAsyncRetry_local(int request) throws Exception
    {
        File rootDir = SRCDIR;

        File baseDir = new File(testDir, "lretry" + request);
        long maxBytes = 300 * 1024;

        try
        {
            final AppStorageImpl asm = new AppStorageImpl(maxBytes, baseDir);
            final AppID id = new AppID(0xf00df00d, 1);

            // First, fill up storage, so we know what is there
            long time = System.currentTimeMillis();
            int loops = fillUpStorage(asm, rootDir, new TransportProtocol[] { new LocalTransportProtocol() },
                    rootDir.getAbsolutePath(), new AppID(0xcafecafe, 1));
            time = System.currentTimeMillis() - time;

            AppDescriptionInfo info = AppStorageManagerTest.generateInfo(rootDir, null);

            // Mount local filesys
            FakeFS fs = FakeFS.mount(rootDir);
            try
            {
                // Fail to open files
                fs.setNo(request);

                TransportProtocol[] tp = { new LocalTransportProtocol() };

                // Schedule apps for store via local transport.
                for (int i = 0; i < loops * 2; ++i)
                {
                    int priority = i + 3;
                    if (priority > 255) priority = 255;
                    /*
                    assertTrue("Expected async storage to succeed", asm.storeApp(id, i, priority, null, tp, fs.source,
                            false));
                            */
                }
                // Wait some time (kinda arbitrary)
                Thread.sleep((time + 500 * loops) * 2);
                // Make sure nothing is stored...
                for (int i = 0; i < loops * 2; ++i)
                {
                    AppStorage app = asm.retrieveApp(id, i, "");
                    if (request == 0)
                        assertNull("Did not expect files to be stored given IO failures", app);
                    else if (app != null)
                    {
                        boolean locked = false;
                        try
                        {
                            assertFalse("Did not expect files to be stored given IO failures after " + request
                                    + " requests", locked = app.lock());
                        }
                        finally
                        {
                            if (locked) app.unlock();
                        }
                    }
                }

                // Finally enable open request
                fs.setNo(-1);

                // Perform a single sync store to ensure that things work...
                //assertTrue("Expected sync storage to succeed", asm.storeApp(id, 2000, 2, info, tp, fs.source, true));

                // Schedule 1 app to kick things off (don't expect to be stored)
                //assertTrue("Expected async storage to succeed", asm.storeApp(id, 2001, 1, null, tp, fs.source, false));

                // Verify expected apps are stored
                for (int i = loops; i < loops * 2; ++i)
                {
                    AppStorage app = AppStorageManagerTest.waitApp(asm, id, i, 15000L * loops);
                    assertNotNull("Expected app to be stored: " + i, app);
                    app.lock();
                    try
                    {
                        AppStorageManagerTest.checkFiles(app, info);
                    }
                    finally
                    {
                        app.unlock();
                    }
                }
                for (int i = 0; i < loops; ++i)
                {
                    AppStorage app = asm.retrieveApp(id, i, "");
                    assertNull("Expected lower-priority app not to be stored: " + i, app);
                }
            }
            finally
            {
                fs.unmount();
            }
        }
        finally
        {
            deleteAll(baseDir);
        }
    }

    /**
     * Tests storeApp() async when files aren't initially available, then become
     * available. Failure occurs on initial file access.
     */
    public void testAsyncRetry_local_existsFail() throws Exception
    {
        doTestAsyncRetry_local(0);
    }

    /**
     * Tests storeApp() async when files aren't initially available, then become
     * available. Failure occurs on initial file access.
     */
    public void testAsyncRetry_local_openFail() throws Exception
    {
        doTestAsyncRetry_local(3);
    }

    private static void deleteAll(File dir) throws Exception
    {
        if (!dir.delete())
        {
            String[] files = dir.list();
            if (files != null)
            {
                for (int i = 0; i < files.length; ++i)
                    deleteAll(new File(dir, files[i]));
            }
            // Kludge to make sure any open files are closed
            System.gc();
            System.runFinalization();

            dir.delete();
        }

        // Note: if a previous failure got us here, this will "erase" it...
        assertFalse("Could not delete " + dir, dir.exists());
    }

    /* ===================== boilerplate ======================= */

    private static final File SRCDIR = new File("/syscwd/apps/launcher");

    // private static final File SRCDIR2 = new
    // File("/syscwd/qa/xlet/org/cablelabs/xlet/hsampler");
    private static final File SRCDIR3 = new File("/syscwd/qa/xlet");

    private String baseDirProp;

    private String maxBytesProp;

    private AuthManager savedAm;

    private void replaceAuthManager() throws Exception
    {
        // This is a workaround for bug 5072.
        // Ensure that the FM is up first (else we may end up being stuck with
        // our AuthManager forever)
        ManagerManager.getInstance(FileManager.class);

        savedAm = (AuthManager) ManagerManager.getInstance(AuthManager.class);
        ManagerManagerTest.updateManager(AuthManager.class, DummyAuth.class, false, new DummyAuth());
    }

    private void restoreAuthManager() throws Exception
    {
        if (savedAm != null) ManagerManagerTest.updateManager(AuthManager.class, savedAm.getClass(), true, savedAm);
    }

    protected void setUp() throws Exception
    {
        super.setUp();

        replaceAuthManager();
        baseDirProp = MPEEnv.getEnv(BASEDIR_PROP);
        maxBytesProp = MPEEnv.getEnv(AppStorageImpl.MAXBYTES_PROP);
    }

    protected void tearDown() throws Exception
    {
        if (baseDirProp != null)
            MPEEnv.setEnv(BASEDIR_PROP, baseDirProp);
        else
            MPEEnv.removeEnvOverride(BASEDIR_PROP);
        if (maxBytesProp != null)
            MPEEnv.setEnv(AppStorageImpl.MAXBYTES_PROP, maxBytesProp);
        else
            MPEEnv.removeEnvOverride(AppStorageImpl.MAXBYTES_PROP);

        restoreAuthManager();
        super.tearDown();
    }

    public AppStorageImplTest(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite(args));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(AppStorageImplTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new AppStorageImplTest(tests[i]));

            // TODO: Following copied from suite()... should reorganize so it's
            // not duplicated!!!
            ImplFactory factory = new ManagerTest.ManagerFactory()
            {
                File baseDir;

                public Object createImplObject()
                {
                    baseDir = new File(testDir, (++subdir) + "");
                    return new AppStorageImpl(MAXBYTES, baseDir);
                }

                public void destroyImplObject(Object obj)
                {
                    ((Manager) obj).destroy();
                    if (baseDir != null) baseDir.delete();
                }
            };
            InterfaceTestSuite asm = AppStorageManagerTest.isuite(tests); // filters
                                                                          // tests
            asm.addFactory(factory);
            suite.addTest(asm);

            return suite;
        }
    }

    /**
     * An extension of HttpD that is capable of denying requests.
     * 
     * @author Aaron Kamienski
     */
    public static class XHttpD extends HttpD
    {
        public synchronized static HttpD createInstance(File baseDir) throws IOException
        {
            return createInstance(baseDir, false);
        }

        public synchronized static HttpD createInstance(File baseDir, boolean genHashfile) throws IOException
        {
            while (nextPort < 0xFFFF)
            {
                try
                {
                    return new XHttpD(++nextPort, baseDir, genHashfile);
                }
                catch (IOException e)
                {
                    continue;
                }
            }
            throw new IOException("No available port");
        }

        private boolean deny = false;

        public XHttpD(int port, File baseDir) throws IOException
        {
            this(port, baseDir, false);
        }

        public XHttpD(int port, File baseDir, boolean genHashfile) throws IOException
        {
            super(port, baseDir, genHashfile);
        }

        public void setDeny(boolean deny)
        {
            this.deny = deny;
        }

        public Response serve(String uri, String method, Properties header, Properties parms)
        {
            if (deny)
            {
                if (verbose) System.out.println("Deny: " + method + " " + uri);

                return null;
            }
            return super.serve(uri, method, header, parms);
        }
    }

    /**
     * A <code>FileSys</code> implementation that can be registered via
     * {@link FakeFS#mount}. This fake file system re-mounts a real file system
     * location at another mount point. It also allows for the introduction of
     * specific errors for testing.
     * 
     * @author Aaron Kamienski
     */
    public static class FakeFS implements FileSys
    {
        private static int count = 0;

        public static synchronized FakeFS mount(File rootDir)
        {
            String path = "/fake" + (count++) + "/";
            FakeFS fs = new FakeFS(rootDir, path);

            FileManager fm = (FileManager) ManagerManager.getInstance(FileManager.class);
            fm.registerFileSys(path, fs);

            return fs;
        }

        public FakeFS(File rootDir, String mount)
        {
            String tmp = rootDir.getAbsolutePath();
            dest = tmp.endsWith("/") ? tmp.substring(0, tmp.length() - 1) : tmp;
            source = mount.endsWith("/") ? mount.substring(0, mount.length() - 1) : mount;

            FileManager fm = (FileManager) ManagerManager.getInstance(FileManager.class);
            real = fm.getFileSys(dest);
        }

        public void unmount()
        {
            FileManager fm = (FileManager) ManagerManager.getInstance(FileManager.class);
            fm.unregisterFileSys(source);
        }

        public void setNo(int what)
        {
            switch (what)
            {
                case 0:
                    setNoExists(true);
                    break;
                case 1:
                    setNoList(true);
                    break;
                case 2:
                    setNoLength(true);
                    break;
                case 3:
                    setNoOpen(true);
                    break;
                case -1:
                    setNoExists(false);
                    setNoList(false);
                    setNoLength(false);
                    setNoOpen(false);
                    break;
            }
        }

        public void setNoExists(boolean noExists)
        {
            this.noExists = noExists;
        }

        public void setNoOpen(boolean noOpen)
        {
            this.noOpen = noOpen;
        }

        public void setNoLength(boolean noLength)
        {
            this.noLength = noLength;
        }

        public void setNoList(boolean noList)
        {
            this.noList = noList;
        }

        private String unmap(String path) throws IOException
        {
            if (path.startsWith(source))
            {
                return dest + path.substring(source.length());
            }
            throw new IOException("Not visible on this FS");
        }

        public boolean exists(String path)
        {
            if (noExists) return false;

            try
            {
                return real.exists(unmap(path));
            }
            catch (IOException e)
            {
                return false;
            }
        }

        public FileData getFileData(String path) throws FileNotFoundException, IOException
        {
            try
            {
                return real.getFileData(unmap(path));
            }
            catch (FileSysCommunicationException e)
            {
                throw new IOException(e.getMessage());
            }
        }

        public boolean isDir(String path)
        {
            try
            {
                return exists(path) && real.isDir(unmap(path));
            }
            catch (IOException e)
            {
                return false;
            }
        }

        public long length(String path)
        {
            try
            {
                if (noLength && real.exists(unmap(path))) return 0;
                return real.length(unmap(path));
            }
            catch (IOException e)
            {
                return 0;
            }
        }

        public String[] list(String path)
        {
            try
            {
                if (noList && real.exists(unmap(path)) && real.isDir(unmap(path))) return null;

                return real.list(unmap(path));
            }
            catch (IOException e)
            {
                return null;
            }
        }

        public FileSys load(String path, int loadMode) throws FileNotFoundException, IOException
        {
            return this;
        }

        public OpenFile open(String path) throws FileNotFoundException
        {
            if (noOpen) throw new FileNotFoundException(path);

            try
            {
                return real.open(unmap(path));
            }
            catch (FileNotFoundException e)
            {
                throw e;
            }
            catch (IOException e)
            {
                throw new FileNotFoundException(e.getMessage());
            }
        }

        public FileSys unload()
        {
            return this;
        }

        public boolean canRead(String path)
        {
            return real.canRead(path);
        }

        public boolean canWrite(String path)
        {
            return real.canWrite(path);
        }

        public boolean create(String path) throws IOException
        {
            return real.create(path);
        }

        public boolean delete(String path)
        {
            return real.delete(path);
        }

        public String getCanonicalPath(String path)
        {
            return real.getCanonicalPath(path);
        }

        public boolean isFile(String path)
        {
            return real.isFile(path);
        }

        public long lastModified(String path)
        {
            return real.lastModified(path);
        }

        public boolean mkdir(String path)
        {
            return real.mkdir(path);
        }

        public boolean renameTo(String fromPath, String toPath)
        {
            return real.renameTo(fromPath, toPath);
        }

        public boolean setLastModified(String path, long time)
        {
            return real.setLastModified(path, time);
        }

        public boolean setReadOnly(String path)
        {
            return real.setReadOnly(path);
        }

        public boolean deleteOnExit(String path)
        {
            return real.deleteOnExit(path);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.impl.io.FileSys#asynchronousLoad(java.lang.String,
         * int, org.cablelabs.impl.io.AsyncLoadCallback)
         */
        public AsyncLoadHandle asynchronousLoad(String path, int loadMode, AsyncLoadCallback cb)
                throws FileNotFoundException
        {
            // TODO Auto-generated method stub
            return null;
        }

        private boolean noOpen;

        private boolean noExists;

        private boolean noLength;

        private boolean noList;

        public final FileSys real;

        public final String source;

        private final String dest;

        public String contentType(String path)
        {
            return null;
        }

    }
    
    private static final String BASEDIR_PROP = "/syscwd";
    private static final String DEFAULT_SUBDIR = "/syscwd";

    private static final long MAXBYTES = 300 * 1024;

    private static final File testRoot = new File(MPEEnv.getEnv("OCAP.persistent.appstorage", "/syscwd"));

    private static final File testDir = new File(testRoot, "tststor");

    private static int subdir = 0;

    public static Test suite()
    {
        TestSuite suite = new TestSuite(AppStorageImplTest.class);

        ImplFactory factory = new ManagerTest.ManagerFactory()
        {
            File baseDir;

            public Object createImplObject()
            {
                baseDir = new File(testDir, (++subdir) + "");
                return new AppStorageImpl(MAXBYTES, baseDir);
            }

            public void destroyImplObject(Object obj)
            {
                ((Manager) obj).destroy();
                if (baseDir != null) baseDir.delete();
            }
        };
        InterfaceTestSuite asm = AppStorageManagerTest.isuite();
        asm.addFactory(factory);
        suite.addTest(asm);

        return suite;
    }
}
