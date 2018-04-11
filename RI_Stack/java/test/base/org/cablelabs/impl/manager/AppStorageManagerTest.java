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
package org.cablelabs.impl.manager;

import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.FileSysCommunicationException;
import org.cablelabs.impl.manager.AppStorageManager.ApiStorage;
import org.cablelabs.impl.manager.AppStorageManager.AppStorage;
import org.cablelabs.impl.manager.AuthManager.AuthContext;
import org.cablelabs.impl.manager.AuthManager.AuthInfo;
import org.cablelabs.impl.manager.appstorage.AppDescriptionInfo;
import org.cablelabs.impl.manager.appstorage.AppDescriptionInfo.DirInfo;
import org.cablelabs.impl.manager.appstorage.AppDescriptionInfo.FileInfo;
import org.cablelabs.impl.signalling.AppEntry.IcTransportProtocol;
import org.cablelabs.impl.signalling.AppEntry.LocalTransportProtocol;
import org.cablelabs.impl.signalling.AppEntry.TransportProtocol;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.test.NanoHTTPD;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.dvb.application.AppID;
import org.dvb.dsmcc.InsufficientResourcesException;
import org.dvb.dsmcc.InvalidFormatException;
import org.dvb.dsmcc.InvalidPathNameException;
import org.dvb.dsmcc.MPEGDeliveryException;
import org.dvb.dsmcc.NotEntitledException;
import org.dvb.dsmcc.ServerDeliveryException;
import org.dvb.dsmcc.ServiceXFRException;

/**
 * @author Aaron Kamienski
 */
public class AppStorageManagerTest extends ManagerTest
{
    /**
     * Test retrieveApp().
     */
    public void testRetrieveApp()
    {
        AppID id = new AppID(OID, AID++);
        int version = 1;
        AppStorage app = asm.retrieveApp(id, version, "");
        assertNull("Did not expect api to be stored already", app);

        // Store App
        File baseDir = BASEDIR1;
        AppDescriptionInfo info = generateInfo(baseDir, null);
        TransportProtocol[] tp = { new LocalTransportProtocol() };
        storeApp(id, version, 100, info, tp, baseDir.getAbsolutePath(), true, true);

        // Retrieve App
        app = asm.retrieveApp(id, version, "");
        checkApp(app, id, version);

        AppStorage app2 = asm.retrieveApp(id, version, "");
        assertSame("Expected same retrieved for multiple calls", app, app2);
    }

    /**
     * Test retrieveApi().
     */
    public void testRetrieveApi()
    {
        // Verify API isn't already there
        String name = getName();
        String version = "a";
        ApiStorage api = asm.retrieveApi(name, version);
        assertNull("Did not expect api to be stored already", api);

        // Store API
        File baseDir = BASEDIR1;
        AppDescriptionInfo info = generateInfo(baseDir, null);
        storeApi(name, version, 100, info, baseDir, true);

        // Retrieve API
        api = asm.retrieveApi(name, version);
        checkApi(api, name, version);

        ApiStorage api2 = asm.retrieveApi(name, version);
        assertSame("Expected same retrieved for multiple calls", api, api2);
    }

    /**
     * Test retrieveApis().
     */
    public void testRetrieveApis()
    {
        // Track originally stored APIs
        Vector original = new Vector();
        ApiStorage[] apis = asm.retrieveApis();
        assertNotNull("should not return an null array", apis);
        assertEquals("updatePrivilegedCertificates should purge database", 0, apis.length);

        // Store an API, and verify retrieved via retrieveApis()
        for (int ver = 0; ver < 5; ++ver)
        {
            File baseDir = BASEDIR1;
            AppDescriptionInfo info = generateInfo(baseDir, null);
            String name = getName();
            String version = ver + "";
            storeApi(name, version, 100, info, baseDir, true);
            ApiStorage api = asm.retrieveApi(name, version);
            checkApi(api, name, version);

            ApiStorage[] newApis = asm.retrieveApis();
            assertEquals("Expected one extra API to be retrieved", original.size() + 1, newApis.length);

            assertTrue("New API not expected to be stored already", original.indexOf(api) == -1);

            for (int i = 0; i < newApis.length; ++i)
            {
                if (original.indexOf(newApis[i]) == -1)
                {
                    assertEquals("Expected new API to be the only new one", api, newApis[i]);
                }
            }
            original.addElement(api);
        }
    }

    /**
     * Test retrieveApis(), prior to receiving privileged certificates.
     */
    public void testRetrieveApis_noPrivilegedCertificates()
    {
        // Get rid of previous...
        asm.destroy();
        mgr = asm = null;

        // Create new, but don't notify about privileged certificates
        mgr = asm = createAppStorageManager();

        // Attempt to retrieve APIs
        ApiStorage[] apis = asm.retrieveApis();
        assertNull("should return null array if storage is not 'enabled'", apis);

        // Finally, update privileged certificates
        asm.updatePrivilegedCertificates(testCertBytes());
        apis = asm.retrieveApis();
        assertNotNull("should return non-null array after privileged certificates are located", apis);
    }

    /**
     * Test storeApp().
     */
    private void doTestStoreApp(File rootDir, TransportProtocol[] tp, String baseDir)
    {
        AppID id = new AppID(OID, AID++);
        int version = 1;
        AppStorage app = asm.retrieveApp(id, version, "");
        assertNull("Did not expect api to be stored already", app);

        // Store App
        AppDescriptionInfo info = generateInfo(rootDir, null);
        storeApp(id, version, 100, info, tp, baseDir, true, true);

        // Retrieve App
        app = asm.retrieveApp(id, version, "");
        checkApp(app, id, version);

        assertTrue("Expected lock() to succeed", app.lock());
        try
        {
            checkFiles(app, info);

            // Store a new version
            int version2 = version + 1;
            storeApp(id, version2, 100, info, tp, baseDir, true, true);

            // Retrieve new version
            AppStorage app2 = asm.retrieveApp(id, version2, "");
            checkApp(app2, id, version2);

            assertTrue("Expected lock() to succeed", app2.lock());
            try
            {
                checkFiles(app2, info);

                // Verify no overlap
                assertFalse("Expected base directories to be different", app.getBaseDirectory().equals(
                        app2.getBaseDirectory()));

                // Verify that old files are still in-place
                checkFiles(app, info);
            }
            finally
            {
                app2.unlock();
            }
        }
        finally
        {
            app.unlock();
        }
    }

    /**
     * Test storeApp().
     */
    public void testStoreApp() throws Exception
    {
        File rootDir = BASEDIR1;
        TransportProtocol[] tp = { new LocalTransportProtocol() };
        doTestStoreApp(rootDir, tp, rootDir.getAbsolutePath());
    }

    /**
     * Test storeApp().
     */
    public void testStoreApp_null() throws Exception
    {
        File rootDir = BASEDIR1;
        doTestStoreApp(rootDir, null, rootDir.getAbsolutePath());
    }

    /**
     * Test storeApp().
     */
    public void testStoreApp_http() throws Exception
    {
        File rootDir = BASEDIR1;
        HttpD httpd = HttpD.createInstance(rootDir.getParentFile(), true);

        try
        {
            IcTransportProtocol ic = new IcTransportProtocol();
            ic.urls.add(httpd.getMyURL().toString());
            TransportProtocol[] tp = { ic };
            String baseDir = "/" + rootDir.getName();

            doTestStoreApp(rootDir, tp, baseDir);
        }
        finally
        {
            httpd.shutdown();
        }
    }

    public static AppStorage waitApp(AppStorageManager asm, AppID id, int version, long ms) throws InterruptedException
    {
        final long period = 500;
        final int loops = (int) ((ms + period - 1) / period);

        AppStorage app = asm.retrieveApp(id, version, "");
        for (int i = 0; app == null && i < loops; ++i)
        {
            Thread.sleep(period);
            app = asm.retrieveApp(id, version, "");
        }
        return app;
    }

    /**
     * Test storeApp(), asynchronous.
     */
    private void doTestStoreApp_async(File rootDir, TransportProtocol tp[], String baseDir) throws Exception
    {
        AppID id = new AppID(OID, AID++);
        int version = 1;
        AppStorage app = asm.retrieveApp(id, version, "");
        assertNull("Did not expect api to be stored already", app);

        AppDescriptionInfo info = generateInfo(rootDir, null);
        AppDescriptionInfo nullInfo = null;

        // Store App
        storeApp(id, version, 100, nullInfo, tp, baseDir, false, true);

        // Wait an amount of time for the app to be made available
        app = waitApp(asm, id, version, 15000L);
        checkApp(app, id, version);

        assertTrue("Expected lock() to succeed", app.lock());
        try
        {
            checkFiles(app, info);

            // Store a new version
            int version2 = version + 1;
            storeApp(id, version2, 100, nullInfo, tp, baseDir, false, true);

            // Retrieve new version
            // Wait an amount of time for the app to be made available
            AppStorage app2 = waitApp(asm, id, version2, 15000L);
            checkApp(app2, id, version2);

            assertTrue("Expected lock() to succeed", app2.lock());
            try
            {
                checkFiles(app2, info);

                // Verify no overlap
                assertFalse("Expected base directories to be different", app.getBaseDirectory().equals(
                        app2.getBaseDirectory()));

                // Verify that old files are still in-place
                checkFiles(app, info);
            }
            finally
            {
                app2.unlock();
            }
        }
        finally
        {
            app.unlock();
        }
    }

    /**
     * Test storeApp(), asynchronous.
     */
    public void testStoreApp_async() throws Exception
    {
        File rootDir = BASEDIR1;
        TransportProtocol[] tp = { new LocalTransportProtocol() };

        doTestStoreApp_async(rootDir, tp, rootDir.getAbsolutePath());
    }

    /**
     * Test storeApp(), asynchronous.
     */
    public void testStoreApp_async_http() throws Exception
    {
        File rootDir = BASEDIR1;
        HttpD httpd = HttpD.createInstance(rootDir.getParentFile(), true);

        try
        {
            IcTransportProtocol ic = new IcTransportProtocol();
            ic.urls.add(httpd.getMyURL().toString());
            TransportProtocol[] tp = { ic };
            String baseDir = "/" + rootDir.getName();

            doTestStoreApp_async(rootDir, tp, baseDir);
        }
        finally
        {
            httpd.shutdown();
        }
    }

    /**
     * Test storeApi().
     */
    public void testStoreApi()
    {
        // Verify API isn't already there
        String name = getName();
        String version = "a";
        ApiStorage api = asm.retrieveApi(name, version);
        assertNull("Did not expect api to be stored already", api);

        // Store API
        File baseDir = BASEDIR1;
        AppDescriptionInfo info = generateInfo(baseDir, null);
        storeApi(name, version, 100, info, baseDir, true);

        // Retrieve API
        api = asm.retrieveApi(name, version);
        checkApi(api, name, version);

        assertTrue("Expected lock() to succeed", api.lock());
        try
        {
            checkFiles(api, info);

            // Store a new version
            String version2 = version + version;
            storeApi(name, version2, 100, info, baseDir, true);

            ApiStorage api2 = asm.retrieveApi(name, version2);
            checkApi(api2, name, version2);

            assertTrue("Expected lock() to succeed", api2.lock());
            try
            {
                // Verify that new files are in-place
                checkFiles(api2, info);

                // Verify no overlap
                assertFalse("Expected base directories to be different", api.getBaseDirectory().equals(
                        api2.getBaseDirectory()));

                // Verify that old files are still in-place
                checkFiles(api, info);
            }
            finally
            {
                api2.unlock();
            }
        }
        finally
        {
            api.unlock();
        }
    }

    /**
     * Expect storeApi() to fail if files are not dual-signed.
     * 
     * @throws Exception
     */
    public void testStoreApi_signedOcap() throws Exception
    {
        doTestStoreApi_notDualSigned(AuthInfo.AUTH_SIGNED_OCAP, AuthInfo.AUTH_SIGNED_OCAP);
        doTestStoreApi_notDualSigned(AuthInfo.AUTH_SIGNED_OCAP, AuthInfo.AUTH_SIGNED_DVB);
        doTestStoreApi_notDualSigned(AuthInfo.AUTH_SIGNED_OCAP, AuthInfo.AUTH_UNSIGNED);
    }

    /**
     * Expect storeApi() to fail if files are not dual-signed.
     * 
     * @throws Exception
     */
    public void testStoreApi_signedDvb() throws Exception
    {
        doTestStoreApi_notDualSigned(AuthInfo.AUTH_SIGNED_DVB, AuthInfo.AUTH_SIGNED_DVB);
        doTestStoreApi_notDualSigned(AuthInfo.AUTH_SIGNED_DVB, AuthInfo.AUTH_UNSIGNED);
    }

    /**
     * Expect storeApi() to fail if files are not dual-signed.
     * 
     * @throws Exception
     */
    public void testStoreApi_unsigned() throws Exception
    {
        doTestStoreApi_notDualSigned(AuthInfo.AUTH_UNSIGNED, AuthInfo.AUTH_UNSIGNED);
    }

    private void doTestStoreApi_notDualSigned(int appType, int fileType) throws Exception
    {
        // Verify API isn't already there
        String name = getName();
        String version = "a";
        ApiStorage api = asm.retrieveApi(name, version);
        assertNull("Did not expect api to be stored already", api);

        // Make AuthManager say not dual-signed...
        DummyAuth auth = (DummyAuth) ManagerManager.getInstance(AuthManager.class);
        auth.appType = appType;
        auth.fileType = fileType;

        // Store API
        File baseDir = BASEDIR1;
        AppDescriptionInfo info = generateInfo(baseDir, null);
        storeApi(name, version, 100, info, baseDir, false);

        // Retrieve API
        api = asm.retrieveApi(name, version);
        assertNull("Expected api storage to fail", api);
    }

    /**
     * Tests use of storeApp() to update priorities.
     */
    private void doTestStoreApp_setPriority(File rootDir, TransportProtocol[] tp, String baseDir)
    {
        AppID id = new AppID(OID, AID++);
        int version = 1;
        AppDescriptionInfo info = generateInfo(rootDir, null);
        long size = totalSize(info);

        // Continue storing low-priority files until fails to do so
        long time = System.currentTimeMillis();
        int loops = 0;
        for (; loops < 1000; ++loops)
        {
            boolean success = storeApp(id, version + loops, 1, info, tp, baseDir, true);
            if (!success) break;
        }
        time = System.currentTimeMillis() - time;
        assertTrue("Expected at least one storage for test", loops > 0);
        assertTrue("Could not fill up storage for test", loops < 1000);

        // Just for the heck of it...
        System.out.println("storeApp() " + (size * loops * 1000.0) / time + " Bps");

        // Go through and update priorities
        for (int i = 0; i < loops; ++i)
        {
            assertTrue("Should be able to update priority: " + i, storeApp(id, version + i, 100, info, tp, baseDir,
                    true));
        }

        // Attempt to add mid-priority files (should fail)
        assertFalse("Should not be able to store mid-priority files", storeApp(id, 2000 + version + loops, 50, info,
                tp, baseDir, true));
    }

    /**
     * Tests use of storeApp() to update priorities.
     */
    public void testStoreApp_setPriority() throws Exception
    {
        File rootDir = BASEDIR1;
        TransportProtocol[] tp = { new LocalTransportProtocol() };
        doTestStoreApp_setPriority(rootDir, tp, rootDir.getAbsolutePath());
    }

    /**
     * Tests use of storeApp() to update priorities.
     */
    public void testStoreApp_setPriority_null() throws Exception
    {
        File rootDir = BASEDIR1;
        doTestStoreApp_setPriority(rootDir, null, rootDir.getAbsolutePath());
    }

    /**
     * Tests use of storeApp() to update priorities.
     */
    public void testStoreApp_setPriority_http() throws Exception
    {
        File rootDir = BASEDIR1;
        HttpD httpd = HttpD.createInstance(rootDir.getParentFile(), true);

        try
        {
            IcTransportProtocol ic = new IcTransportProtocol();
            ic.urls.add(httpd.getMyURL().toString());
            TransportProtocol[] tp = { ic };
            String baseDir = "/" + rootDir.getName();

            doTestStoreApp_setPriority(rootDir, tp, baseDir);
        }
        finally
        {
            httpd.shutdown();
        }
    }

    /**
     * Tests use of storeApp() to update priorities.
     */
    private void doTestStoreApp_async_setPriority(File rootDir, TransportProtocol[] tp, String baseDir)
            throws Exception
    {
        AppID id = new AppID(OID, AID++);
        int version = 1;
        AppStorage app = asm.retrieveApp(id, version, "");
        assertNull("Did not expect api to be stored already", app);

        AppDescriptionInfo info = generateInfo(rootDir, null);

        // Continue storing low-priority files until fails to do so
        int loops = 0;
        for (; loops < 1000; ++loops)
        {
            boolean success = storeApp(id, version + loops, 1, info, tp, baseDir, true);
            if (!success) break;
        }
        assertTrue("Expected at least one storage for test", loops > 0);
        assertTrue("Could not fill up storage for test", loops < 1000);

        // Go through and update priorities (using ASYNC)
        for (int i = 0; i < loops; ++i)
        {
            assertTrue("Should be able to update priority: " + i, storeApp(id, version + i, 100, null, null, null,
                    false));
        }

        // Attempt to add mid-priority files (should fail)
        assertFalse("Should not be able to store mid-priority files", storeApp(id, 2000 + version + loops, 50, info,
                tp, baseDir, true));
    }

    /**
     * Tests use of storeApp() to update priorities.
     */
    public void testStoreApp_async_setPriority() throws Exception
    {
        File rootDir = BASEDIR1;
        TransportProtocol[] tp = { new LocalTransportProtocol() };

        doTestStoreApp_async_setPriority(rootDir, tp, rootDir.getAbsolutePath());
    }

    /**
     * Test storeApp(), asynchronous.
     */
    public void testStoreApp_async_setPriority_http() throws Exception
    {
        File rootDir = BASEDIR1;
        HttpD httpd = HttpD.createInstance(rootDir.getParentFile(), true);

        try
        {
            IcTransportProtocol ic = new IcTransportProtocol();
            ic.urls.add(httpd.getMyURL().toString());
            TransportProtocol[] tp = { ic };
            String baseDir = "/" + rootDir.getName();

            doTestStoreApp_async_setPriority(rootDir, tp, baseDir);
        }
        finally
        {
            httpd.shutdown();
        }
    }

    /**
     * Tests use of storeApi() to update priorities.
     */
    public void testStoreApi_setPriority()
    {
        String name = getName();
        int priority = 1;
        String version = priority + "";
        File baseDir = BASEDIR2;
        AppDescriptionInfo info = generateInfo(baseDir, null);
        long size = totalSize(info);

        // Continue storing low-priority files until fails to do so
        int loops = 0;
        long time = System.currentTimeMillis();
        for (; loops < 1000; ++loops)
        {
            boolean success = storeApi(name + loops, version, priority, info, baseDir);
            if (!success) break;
        }
        time = System.currentTimeMillis() - time;
        assertTrue("Expected at least one storage for test", loops > 0);
        assertTrue("Could not fill up storage for test", loops < 1000);

        // Just for the heck of it...
        System.out.println("storeApi() " + (size * loops * 1000.0) / time + " Bps");

        // Go through and update priorities
        for (int i = 0; i < loops; ++i)
        {
            assertTrue("Should be able to update priority: " + i, storeApi(name + i, version, 100, info, baseDir));
        }

        // Attempt to add mid-priority files (should fail)
        assertFalse("Should not be able to store mid-priority files", storeApi(name, version + ".a", 50, info, baseDir));
    }

    /**
     * Tests storeApp() w/ file wildcard.
     */
    public void testStoreApp_wildcardFiles() throws Exception
    {
        doTestStoreApp_wildcard(true, false, makeWildcardBaseDir());
    }

    /**
     * Tests storeApp() w/ dir wildcard.
     */
    public void testStoreApp_wildcardDirs() throws Exception
    {
        doTestStoreApp_wildcard(false, true, makeWildcardBaseDir());
    }

    /**
     * Tests storeApp() w/ file and dir wildcards.
     */
    public void testStoreApp_wildcardFilesDirs() throws Exception
    {
        doTestStoreApp_wildcard(true, true, makeWildcardBaseDir());
    }

    private File makeWildcardBaseDir() throws IOException
    {
        // First figure out where to place files...
        String ocap = MPEEnv.getEnv("OCAP.persistent.root");
        assertNotNull("Internal error - cannot test; need location to write temporary files", ocap);
        File baseDir = new File(new File(ocap), "tststor/wildcard");

        // Remember to delete directory
        toDelete.addElement(new DeleteDir(baseDir));

        // Create the baseDir
        return makeDir(baseDir, 3, 4);
    }

    private File makeDir(File dir, int dirs, int files) throws IOException
    {
        dir.mkdirs();
        assertTrue("Internal error - could not create " + dir, dir.exists());

        for (int i = 0; i < files; ++i)
            makeFile(new File(dir, "file" + i), i * 100);

        for (int i = 0; i < dirs; ++i)
            makeDir(new File(dir, "subdir" + i), dirs - 1, files - 1);

        return dir;
    }

    private void makeFile(File file, int size) throws IOException
    {
        FileOutputStream fos = new FileOutputStream(file);
        for (int i = 0; i < size; ++i)
        {
            fos.write(i);
        }
        fos.close();

        assertTrue("Internal error - could not create file " + file, file.exists());
        assertEquals("Internal error - could not create file of specified size " + file, size, file.length());
    }

    /**
     * Tests storeApp() w/ wildcard(s).
     * 
     * @param files
     *            if true, test file wildcard
     * @param dirs
     *            if true, test dir wildcard
     * @param baseDir
     *            base directory to copy from
     */
    private void doTestStoreApp_wildcard(boolean files, boolean dirs, File baseDir)
    {
        assertNotNull("Internal error - no good base directory found", baseDir);

        AppID id = new AppID(OID, AID++);
        int version = 1;
        AppStorage app = asm.retrieveApp(id, version, "");
        assertNull("Did not expect api to be stored already", app);

        // Store App
        AppDescriptionInfo info = generateWildcardInfo(baseDir, files, dirs);
        TransportProtocol[] tp = { new LocalTransportProtocol() };
        storeApp(id, version, 100, info, tp, baseDir.getAbsolutePath(), true, true);

        // Retrieve App
        app = asm.retrieveApp(id, version, "");
        checkApp(app, id, version);

        assertTrue("Expected lock() to succeed", app.lock());
        try
        {
            checkWildcardFiles(app.getBaseDirectory(), baseDir, files, dirs);

            // Store a new version
            int version2 = version + 1;
            storeApp(id, version2, 100, info, tp, baseDir.getAbsolutePath(), true, true);

            // Retrieve new version
            AppStorage app2 = asm.retrieveApp(id, version2, "");
            checkApp(app2, id, version2);

            assertTrue("Expected lock() to succeed", app2.lock());
            try
            {
                checkWildcardFiles(app2.getBaseDirectory(), baseDir, files, dirs);

                // Verify no overlap
                assertFalse("Expected base directories to be different", app.getBaseDirectory().equals(
                        app2.getBaseDirectory()));

                // Verify that old files are still in-place
                checkWildcardFiles(app.getBaseDirectory(), baseDir, files, dirs);
            }
            finally
            {
                app2.unlock();
            }
        }
        finally
        {
            app.unlock();
        }
    }

    /**
     * Tests storeApp() w/ wildcard.
     */
    public void XtestStoreApp_async_wildcard()
    {
        // TODO: implement storeApp() async w/ wildcard (x3 for file, dir,
        // file+dir) test
        // Funny thing is... all of the ASYNC tests are actually testing w/
        // wildcard (because no info is used)
        fail("Unimplemented test");
    }

    /**
     * Tests storeApi() w/ file wildcard.
     */
    public void testStoreApi_wildcardFiles() throws Exception
    {
        doTestStoreApi_wildcard(true, false, makeWildcardBaseDir());
    }

    /**
     * Tests storeApi() w/ dir wildcard.
     */
    public void testStoreApi_wildcardDirs() throws Exception
    {
        doTestStoreApi_wildcard(false, true, makeWildcardBaseDir());
    }

    /**
     * Tests storeApi() w/ files+dirs wildcard.
     */
    public void testStoreApi_wildcardFilesDirs() throws Exception
    {
        doTestStoreApi_wildcard(true, true, makeWildcardBaseDir());
    }

    /**
     * Tests storeApi() w/ wildcard.
     * 
     * @param files
     *            if true, include files wildcard
     * @param dirs
     *            if true, include dirs wildcard
     * @param baseDir
     *            base directory to copy from
     */
    private void doTestStoreApi_wildcard(boolean files, boolean dirs, File baseDir)
    {
        assertNotNull("Internal error - no good base directory found", baseDir);

        // Verify API isn't already there
        String name = getName();
        String version = "a";
        ApiStorage api = asm.retrieveApi(name, version);
        assertNull("Did not expect api to be stored already", api);

        // Store API
        AppDescriptionInfo info = generateWildcardInfo(baseDir, files, dirs);
        storeApi(name, version, 100, info, baseDir, true);

        // Retrieve API
        api = asm.retrieveApi(name, version);
        checkApi(api, name, version);

        assertTrue("Expected lock() to succeed", api.lock());
        try
        {
            checkWildcardFiles(api.getBaseDirectory(), baseDir, files, dirs);

            // Store a new version
            String version2 = version + version;
            storeApi(name, version2, 100, info, baseDir, true);

            ApiStorage api2 = asm.retrieveApi(name, version2);
            checkApi(api2, name, version2);

            assertTrue("Expected lock() to succeed", api2.lock());
            try
            {
                // Verify that new files are in-place
                checkWildcardFiles(api2.getBaseDirectory(), baseDir, files, dirs);

                // Verify no overlap
                assertFalse("Expected base directories to be different", api.getBaseDirectory().equals(
                        api2.getBaseDirectory()));

                // Verify that old files are still in-place
                checkWildcardFiles(api.getBaseDirectory(), baseDir, files, dirs);
            }
            finally
            {
                api2.unlock();
            }
        }
        finally
        {
            api.unlock();
        }
    }

    /**
     * Tests storeApp(). Lower priority apps should be purged if necessary.
     */
    public void testStoreApp_purge()
    {
        AppID id = new AppID(OID, AID++);
        int version = 1;
        File baseDir = BASEDIR2;
        AppDescriptionInfo info = generateInfo(baseDir, null);
        TransportProtocol[] tp = { new LocalTransportProtocol() };
        long size = totalSize(info);

        // Continue storing low-priority files until fails to do so
        int loops = 0;
        long time = System.currentTimeMillis();
        for (; loops < 1000; ++loops)
        {
            boolean success = storeApp(id, version + loops, 1, info, tp, baseDir.getAbsolutePath(), true);
            if (!success) break;
        }
        time = System.currentTimeMillis() - time;
        assertTrue("Expected at least one storage for test", loops > 0);
        assertTrue("Could not fill up storage for test", loops < 1000);

        // Just for the heck of it...
        System.out.println("storeApp() " + (size * loops * 1000.0) / time + " Bps");

        // Then store higher-priority files, should not fail
        time = System.currentTimeMillis();
        for (int i = 0; i < loops; ++i)
        {
            assertTrue("High-priority store should succeed: " + i + "/" + loops, storeApp(id, 2000 + version + i, 50,
                    info, tp, baseDir.getAbsolutePath(), true));
        }
        time = System.currentTimeMillis() - time;
        assertFalse("Expected more high priority stores to fail: " + loops, storeApp(id, 2000 + version + loops, 50,
                info, tp, baseDir.getAbsolutePath(), true));

        // Just for the heck of it...
        System.out.println("storeApp() (replace)" + (size * loops * 1000.0) / time + " Bps");

        // Check that low priority apps have been purged
        for (int i = 0; i < loops; ++i)
        {
            AppStorage app = asm.retrieveApp(id, version + i, "");
            assertNull("Expected low priority app to be purged", app);
        }

        // Add more high priority stores -- should not fail
        for (int i = 0; i < loops; ++i)
        {
            assertTrue("High-priority store should succeed: " + i + "/" + loops, storeApp(id, 4000 + version + i, 100,
                    info, tp, baseDir.getAbsolutePath(), true));
        }
        assertFalse("Expected more high priority stores to fail: " + loops, storeApp(id, 4000 + version + loops, 100,
                info, tp, baseDir.getAbsolutePath(), true));

        // Check that low priority apps have been purged
        for (int i = 0; i < loops; ++i)
        {
            AppStorage app = asm.retrieveApp(id, 2000 + version + i, "");
            assertNull("Expected low priority app to be purged", app);
        }
    }

    /**
     * Tests storeApp(). Lower priority apps should be purged if necessary.
     */
    private void doTestStoreApp_async_purge(File rootDir, TransportProtocol[] tp, String baseDir) throws Exception
    {
        AppID id = new AppID(OID, AID++);
        int version = 1;

        // First, fill up storage...
        int loops = 0;
        {
            AppDescriptionInfo info = generateInfo(rootDir, null);
            TransportProtocol[] tp2 = { new LocalTransportProtocol() };

            // Continue storing low-priority files until fails to do so
            for (; loops < 1000; ++loops)
            {
                boolean success = storeApp(id, version + loops, 1, info, tp2, rootDir.getAbsolutePath(), true);
                if (!success) break;
            }
            assertTrue("Expected at least one storage for test", loops > 0);
            assertTrue("Could not fill up storage for test", loops < 1000);
        }

        // Now store higher-priority files (async)

        AppDescriptionInfo info = generateInfo(rootDir, null);
        AppDescriptionInfo nullInfo = null;

        // Now store higher-priority files (async)
        for (int i = 0; i < loops; ++i)
        {
            assertTrue("High-priority store should succeed: " + i + "/" + loops, storeApp(id, 2000 + version + i, 50,
                    nullInfo, tp, baseDir, false));
        }
        // Wait for apps to be stored.
        for (int i = 0; i < loops; ++i)
        {
            AppStorage app = waitApp(asm, id, 2000 + version + i, 30000);
            checkApp(app, id, 2000 + version + i);
        }

        // Expect more high-priority stores (this time sync) to fail
        assertFalse("Expected more high priority stores to fail: " + loops, storeApp(id, 2000 + version + loops, 50,
                info, tp, baseDir, true));

        // Expect more high-priority stores (this time async) to seemingly work,
        // but fail in the BG
        assertTrue("Expected more high (BG) priority stores to seem to work: " + loops, storeApp(id, 2000 + version
                + loops + 1, 50, nullInfo, tp, baseDir, false));

        // Check that low priority apps have been purged
        for (int i = 0; i < loops; ++i)
        {
            AppStorage app = asm.retrieveApp(id, version + i, "");
            assertNull("Expected low priority app to be purged", app);
        }

        // Add more high priority stores -- should not fail
        for (int i = 0; i < loops; ++i)
        {
            assertTrue("High-priority store should succeed: " + i + "/" + loops, storeApp(id, 4000 + version + i, 100,
                    info, tp, baseDir, false));
        }
        // Wait for apps to be stored.
        for (int i = 0; i < loops; ++i)
        {
            AppStorage app = waitApp(asm, id, 4000 + version + i, 30000);
            checkApp(app, id, 4000 + version + i);
        }

        // Expect more high-priority stores (this time sync) to fail
        assertFalse("Expected more high priority stores to fail: " + loops, storeApp(id, 4000 + version + loops, 100,
                info, tp, baseDir, true));

        // Expect more high-priority stores (this time async) to seemingly work,
        // but fail in the BG
        assertTrue("Expected more high (BG) priority stores to seem to work: " + loops, storeApp(id, 4000 + version
                + loops + 1, 100, nullInfo, tp, baseDir, false));

        // Check that low priority apps have been purged
        for (int i = 0; i < loops; ++i)
        {
            AppStorage app = asm.retrieveApp(id, 2000 + version + i, "");
            assertNull("Expected low priority app to be purged", app);
        }
    }

    /**
     * Tests storeApp(). Lower priority apps should be purged if necessary.
     */
    public void testStoreApp_async_purge() throws Exception
    {
        File rootDir = BASEDIR2;
        TransportProtocol[] tp = { new LocalTransportProtocol() };

        doTestStoreApp_async_purge(rootDir, tp, rootDir.getAbsolutePath());
    }

    /**
     * Tests storeApp(). Lower priority apps should be purged if necessary.
     */
    // TODO: this test has been disabled because I'm having trouble with it...
    // I think it has to to with too many active connections... or something.
    public void XtestStoreApp_async_purge_http() throws Exception
    {
        File rootDir = BASEDIR2;
        HttpD httpd = HttpD.createInstance(rootDir.getParentFile(), true);

        try
        {
            IcTransportProtocol ic = new IcTransportProtocol();
            ic.urls.add(httpd.getMyURL().toString());
            TransportProtocol[] tp = { ic };
            String baseDir = "/" + rootDir.getName();

            doTestStoreApp_async_purge(rootDir, tp, baseDir);
        }
        finally
        {
            httpd.shutdown();
        }
    }

    /**
     * Tests storeApp(). Lower-priority async store should be cancelled by
     * higher-priority sync store.
     */
    public void XtestStoreApp_async_cancel() throws Exception
    {
        // TODO also need a storeApp() that cancel/purges a previous async store
        fail("Unimplemented test");
    }

    /**
     * Tests storeApi(). Lower priority APIs should be purged if necessary.
     */
    public void testStoreApi_purge()
    {
        String name = getName();
        int priority = 1;
        String version = priority + "";
        File baseDir = BASEDIR2;
        AppDescriptionInfo info = generateInfo(baseDir, null);
        long size = totalSize(info);

        // Continue storing low-priority files until fails to do so
        int loops = 0;
        long time = System.currentTimeMillis();
        for (; loops < 1000; ++loops)
        {
            boolean success = storeApi(name + loops, version, priority, info, baseDir);
            if (!success) break;
        }
        time = System.currentTimeMillis() - time;
        assertTrue("Expected at least one storage for test", loops > 0);
        assertTrue("Could not fill up storage for test", loops < 1000);

        // Just for the heck of it...
        System.out.println("storeApi() " + (size * loops * 1000.0) / time + " Bps");

        // Then store higher-priority files, should not fail
        int priority2 = 50;
        String version2 = priority2 + "";
        time = System.currentTimeMillis();
        for (int i = 0; i < loops; ++i)
        {
            assertTrue("High-priority store should succeed: " + i + "/" + loops, storeApi(name + i, version2,
                    priority2, info, baseDir));
        }
        time = System.currentTimeMillis() - time;
        assertFalse("Expected more high priority stores to fail: " + loops, storeApi(name + loops, version2, priority2,
                info, baseDir));

        // Just for the heck of it...
        System.out.println("storeApi() (purge)" + (size * loops * 1000.0) / time + " Bps");

        // Check that low priority apps have been purged
        for (int i = 0; i < loops; ++i)
        {
            ApiStorage api = asm.retrieveApi(name + i, version);
            assertNull("Expected low priority api to be purged", api);
        }

        // And store more higher-priority files, should not fail
        int priority3 = 100;
        String version3 = priority3 + "";
        for (int i = 0; i < loops; ++i)
        {
            assertTrue("High-priority store should succeed: " + i + "/" + loops, storeApi(name + i, version3,
                    priority3, info, baseDir));
        }
        assertFalse("Expected more high priority stores to fail: " + loops, storeApi(name + loops, version3, priority3,
                info, baseDir));

        // Check that low priority apps have been purged
        for (int i = 0; i < loops; ++i)
        {
            ApiStorage api = asm.retrieveApi(name + i, version2);
            assertNull("Expected low priority api to be purged", api);
        }
    }

    /**
     * Tests storeApp() w/ purging, while locked.
     */
    public void testStoreApp_purge_locked() throws Exception
    {
        AppID id = new AppID(OID, AID++);
        int version = 1;
        File baseDir = BASEDIR2;
        AppDescriptionInfo info = generateInfo(baseDir, null);
        TransportProtocol[] tp = { new LocalTransportProtocol() };

        // Continue storing low-priority files until fails to do so
        int loops = 0;
        for (; loops < 1000; ++loops)
        {
            boolean success = storeApp(id, version + loops, 1, info, tp, baseDir.getAbsolutePath(), true);
            if (!success) break;
        }
        assertTrue("Expected at least one storage for test", loops > 0);
        assertTrue("Could not fill up storage for test", loops < 1000);

        AppStorage app = asm.retrieveApp(id, version + 0, "");
        assertTrue("Expected to be able to lock app", app.lock());
        File appDir = app.getBaseDirectory();
        try
        {
            // Then store higher-priority files, should not fail
            for (int i = 0; i < loops - 1; ++i)
            {
                assertTrue("High-priority store should succeed: " + i + "/" + loops, storeApp(id, 2000 + version + i,
                        50, info, tp, baseDir.getAbsolutePath(), true));
            }
            // But one will fail, because of lock()
            assertFalse("Expected more high priority stores to fail: " + (loops - 1), storeApp(id, 2000 + version
                    + (loops - 1), 50, info, tp, baseDir.getAbsolutePath(), true));

            // Expected locked to be purged from DB, but files still ther
            AppStorage app2 = asm.retrieveApp(id, version + 0, "");
            assertNull("Expected locked App to be purged from DB", app2);
            assertTrue("Expected locked/purged App files to still exist", appDir.exists());
        }
        finally
        {
            app.unlock();
        }

        // And final store should succeed
        assertTrue("Expected more high priority stores to succeed now: " + (loops - 1), storeApp(id, 2000 + version
                + (loops - 1), 50, info, tp, baseDir.getAbsolutePath(), true));

        // Now unlocked, files should be purged
        waitForDeletion(appDir, 10000);
        assertFalse("Expected unlocked App files to be purged", appDir.exists());
    }

    /**
     * This method was added to allow deletion operations to be asynchronous.
     * Tests which verify that "old" files are deleted don't really require that
     * files be deleted immediately.
     * 
     * @param f
     *            file to test for deletion
     * @param ms
     *            maximum time to wait for deletion
     */
    public static void waitForDeletion(final File f, final long ms) throws Exception
    {
        final long sleep = 100;
        final long N = (ms + sleep - 1) / sleep;

        for (int i = 0; i < N; ++i)
        {
            if (!f.exists()) break;
            Thread.sleep(sleep);
        }
    }

    /**
     * Tests storeApi() w/ purging, while locked.
     */
    public void testStoreApi_purge_locked() throws Exception
    {
        String name = getName();
        int priority = 1;
        String version = priority + "";
        File baseDir = BASEDIR2;
        AppDescriptionInfo info = generateInfo(baseDir, null);

        // Continue storing low-priority files until fails to do so
        int loops = 0;
        for (; loops < 1000; ++loops)
        {
            boolean success = storeApi(name + loops, version, priority, info, baseDir);
            if (!success) break;
        }
        assertTrue("Expected at least one storage for test", loops > 0);
        assertTrue("Could not fill up storage for test", loops < 1000);

        // Lock one
        ApiStorage locked = asm.retrieveApi(name + 0, version);
        assertNotNull("Expected non-null API to be retrieved", locked);
        assertTrue("Expected to be able to lock api", locked.lock());
        File apiDir = locked.getBaseDirectory();
        int priority2 = 50;
        String version2 = priority2 + "";
        try
        {
            // Then store higher-priority files, should not fail
            for (int i = 0; i < loops - 1; ++i)
            {
                assertTrue("High-priority store should succeed: " + i + "/" + loops, storeApi(name + i, version2,
                        priority2, info, baseDir));
            }
            // But one will fail, because of lock()
            assertFalse("Expected more high priority stores to fail: " + (loops - 1), storeApi(name + (loops - 1),
                    version2, priority2, info, baseDir));

            // Expected locked to be purged from DB, but files still ther
            ApiStorage api2 = asm.retrieveApi(name + 0, version);
            assertNull("Expected locked API to be purged from DB", api2);
            assertTrue("Expected locked/purged API files to still exist", apiDir.exists());
        }
        finally
        {
            locked.unlock();
        }

        // And final store should succeed
        assertTrue("Expected more high priority stores to succeed now: " + (loops - 1), storeApi(name + (loops - 1),
                version2, priority2, info, baseDir));

        // Now unlocked, files should be purged
        waitForDeletion(apiDir, 10000);
        assertFalse("Expected unlocked API files to be purged", apiDir.exists());
    }

    /**
     * Tests that lower priority apps are purged when storing higher priority
     * API.
     */
    public void testStoreApi_purgeApp()
    {
        AppID id = new AppID(OID, AID++);
        String name = getName();
        int version = 1;
        File baseDir = BASEDIR2;
        AppDescriptionInfo info = generateInfo(baseDir, null);
        TransportProtocol[] tp = { new LocalTransportProtocol() };

        // Continue storing low-priority files until fails to do so
        int loops = 0;
        for (; loops < 1000; ++loops)
        {
            boolean success = storeApp(id, version + loops, 1, info, tp, baseDir.getAbsolutePath(), true);
            if (!success) break;
        }
        assertTrue("Expected at least one storage for test", loops > 0);
        assertTrue("Could not fill up storage for test", loops < 1000);

        // Now install APIs w/ higher-priority
        int priority2 = 50;
        String version2 = priority2 + "";
        for (int i = 0; i < loops; ++i)
        {
            assertTrue("High-priority store should succeed: " + i + "/" + loops, storeApi(name + i, version2,
                    priority2, info, baseDir));
        }
        assertFalse("Expected more high priority stores to fail: " + loops, storeApi(name + loops, version2, priority2,
                info, baseDir));

        // Check that low priority apps have been purged
        for (int i = 0; i < loops; ++i)
        {
            AppStorage app = asm.retrieveApp(id, version + i, "");
            assertNull("Expected low priority app to be purged", app);
        }
    }

    /**
     * Tests that lower priority APIs are purged when storing higher priority
     * app.
     */
    public void testStoreApp_purgeApi()
    {
        String name = getName();
        AppID id = new AppID(OID, AID++);
        int priority = 1;
        String version = priority + "";
        File baseDir = BASEDIR2;
        TransportProtocol[] tp = { new LocalTransportProtocol() };
        AppDescriptionInfo info = generateInfo(baseDir, null);

        // Continue storing low-priority files until fails to do so
        int loops = 0;
        for (; loops < 1000; ++loops)
        {
            boolean success = storeApi(name + loops, version, priority, info, baseDir);
            if (!success) break;
        }
        assertTrue("Expected at least one storage for test", loops > 0);
        assertTrue("Could not fill up storage for test", loops < 1000);

        // Then store higher-priority files, should not fail
        for (int i = 0; i < loops; ++i)
        {
            assertTrue("High-priority store should succeed: " + i + "/" + loops, storeApp(id, i, 50, info, tp,
                    baseDir.getAbsolutePath(), true));
        }
        assertFalse("Expected more high priority stores to fail: " + loops, storeApp(id, loops, 50, info, tp,
                baseDir.getAbsolutePath(), true));

        // Check that low priority apps have been purged
        for (int i = 0; i < loops; ++i)
        {
            ApiStorage api = asm.retrieveApi(name + i, version);
            assertNull("Expected low priority api to be purged", api);
        }
    }

    /**
     * Test deleteApp().
     */
    private void doTestDeleteApp(File rootDir, TransportProtocol[] tp, String baseDir, boolean bg) throws Exception
    {
        AppID id = new AppID(OID, AID++);
        int version = 1;
        AppStorage app = asm.retrieveApp(id, version, "");
        assertNull("Did not expect api to be stored already", app);

        // Store App
        AppDescriptionInfo info = generateInfo(rootDir, null);
        AppDescriptionInfo nullInfo = bg ? null : info;
        storeApp(id, version, 100, nullInfo, tp, baseDir, !bg, true);

        // Retrieve App
        if (bg)
            app = waitApp(asm, id, version, 15000L);
        else
            app = asm.retrieveApp(id, version, "");
        checkApp(app, id, version);

        // Verify files are there to begin with
        app.lock();
        File dir;
        try
        {
            dir = app.getBaseDirectory();
            checkFiles(app, info);
        }
        finally
        {
            app.unlock();
        }

        // Delete app
        asm.deleteApp(id, version);

        AppStorage app2 = asm.retrieveApp(id, version, "");
        assertNull("Expected App to be deleted from DB", app2);

        // Now check that files have been deleted
        waitForDeletion(dir, 10000);
        assertFalse("Expected files to be deleted", dir.exists());
    }

    /**
     * Test deleteApp().
     */
    public void testDeleteApp() throws Exception
    {
        File rootDir = BASEDIR1;
        TransportProtocol[] tp = { new LocalTransportProtocol() };

        doTestDeleteApp(rootDir, tp, rootDir.getAbsolutePath(), false);
    }

    /**
     * Test deleteApp().
     */
    public void testDeleteApp_async() throws Exception
    {
        File rootDir = BASEDIR1;
        TransportProtocol[] tp = { new LocalTransportProtocol() };

        doTestDeleteApp(rootDir, tp, rootDir.getAbsolutePath(), true);
    }

    /**
     * Test deleteApp().
     */
    public void testDeleteApp_async_http() throws Exception
    {
        File rootDir = BASEDIR1;
        HttpD httpd = HttpD.createInstance(rootDir.getParentFile(), true);

        try
        {
            IcTransportProtocol ic = new IcTransportProtocol();
            ic.urls.add(httpd.getMyURL().toString());
            TransportProtocol[] tp = { ic };
            String baseDir = "/" + rootDir.getName();

            doTestDeleteApp(rootDir, tp, baseDir, true);
        }
        finally
        {
            httpd.shutdown();
        }
    }

    /**
     * Tests deleteApp().
     */
    private void doTestDeleteApp_cancel_async(File rootDir, TransportProtocol[] tp, String baseDir)
            throws InterruptedException
    {
        AppID id = new AppID(OID, AID++);
        int version = 1;
        AppStorage app = asm.retrieveApp(id, version, "");
        assertNull("Did not expect api to be stored already", app);

        // Store App
        AppDescriptionInfo info = generateInfo(rootDir, null);
        storeApp(id, version, 100, info, tp, baseDir, false, true);

        // Immediately delete that app, don't even GIVE it a chance!
        asm.deleteApp(id, version);

        // Retrieve App
        app = waitApp(asm, id, version, 5000L);
        assertNull("Expected app storage to be cancelled", app);
    }

    /**
     * Test deleteApp().
     */
    public void testDeleteApp_cancel_async() throws Exception
    {
        File rootDir = BASEDIR1;
        TransportProtocol[] tp = { new LocalTransportProtocol() };

        doTestDeleteApp_cancel_async(rootDir, tp, rootDir.getAbsolutePath());
    }

    /**
     * Test deleteApp().
     */
    public void testDeleteApp_cancel_async_http() throws Exception
    {
        File rootDir = BASEDIR1;
        HttpD httpd = HttpD.createInstance(rootDir.getParentFile(), true);

        try
        {
            IcTransportProtocol ic = new IcTransportProtocol();
            ic.urls.add(httpd.getMyURL().toString());
            TransportProtocol[] tp = { ic };
            String baseDir = "/" + rootDir.getName();

            doTestDeleteApp_cancel_async(rootDir, tp, baseDir);
        }
        finally
        {
            httpd.shutdown();
        }
    }

    /**
     * Tests that files are deleted and aren't retrievable.
     */
    public void testDeleteApi() throws Exception
    {
        String name = getName();
        String version = "0";
        File baseDir = BASEDIR2;
        AppDescriptionInfo info = generateInfo(baseDir, null);
        ApiStorage api = asm.retrieveApi(name, version);
        assertNull("Did not expect api to be stored already", api);

        // Store API
        storeApi(name, version, 100, info, baseDir, true);

        // Retrieve API
        api = asm.retrieveApi(name, version);
        checkApi(api, name, version);

        // Verify files are there to begin with
        File dir = api.getBaseDirectory();
        checkFiles(api, info);

        // Delete API
        asm.deleteApi(name, version);

        ApiStorage api2 = asm.retrieveApi(name, version);
        assertNull("Expected API to be deleted from DB", api2);

        // Now check that files have been deleted
        waitForDeletion(dir, 10000);
        assertFalse("Expected files to be deleted", dir.exists());
    }

    /**
     * Tests that deleting an app frees up space.
     */
    public void testDeleteApp_freeSpace()
    {
        AppID id = new AppID(OID, AID++);
        int version = 1;
        File baseDir = BASEDIR2;
        AppDescriptionInfo info = generateInfo(baseDir, null);
        TransportProtocol[] tp = { new LocalTransportProtocol() };

        // Continue storing low-priority files until fails to do so
        int loops = 0;
        for (; loops < 1000; ++loops)
        {
            boolean success = storeApp(id, version + loops, 1, info, tp, baseDir.getAbsolutePath(), true);
            if (!success) break;
        }
        assertTrue("Expected at least one storage for test", loops > 0);
        assertTrue("Could not fill up storage for test", loops < 1000);

        // Delete app should make space for new app
        asm.deleteApp(id, version);

        assertTrue("Expected to be able to store an App after deletion", storeApp(id, version + loops, 1, info, tp,
                baseDir.getAbsolutePath(), true));
    }

    /**
     * Tests that deleting an app frees up space.
     */
    public void testDeleteApp_async_freeSpace() throws Exception
    {
        AppID id = new AppID(OID, AID++);
        int version = 1;
        File rootDir = BASEDIR2;

        int loops = 0;
        {
            AppDescriptionInfo info = generateInfo(rootDir, null);
            TransportProtocol[] tp = { new LocalTransportProtocol() };

            // Continue storing low-priority files until fails to do so
            for (; loops < 1000; ++loops)
            {
                boolean success = storeApp(id, version + loops, 1, info, tp, rootDir.getAbsolutePath(), true);
                if (!success) break;
            }
            assertTrue("Expected at least one storage for test", loops > 0);
            assertTrue("Could not fill up storage for test", loops < 1000);

            // Delete app should make space for new app
            asm.deleteApp(id, version);

            // Should not be able to retrieve deleted app, or failed app
            AppStorage app = asm.retrieveApp(id, version, "");
            assertNull("Should not be able to retrieve deleted app", app);

            app = asm.retrieveApp(id, version + loops, "");
            assertNull("Should not be able to retrieve failed app", app);
        }

        // Store async, should take that freed space
        // Setup HTTPD
        HttpD httpd = HttpD.createInstance(rootDir.getParentFile(), true);

        try
        {
            AppDescriptionInfo info = generateInfo(rootDir, null);
            IcTransportProtocol ic = new IcTransportProtocol();
            ic.urls.add(httpd.getMyURL().toString());
            TransportProtocol[] tp = { ic };
            String baseDir = "/" + rootDir.getName();

            // Store app in the background... should succeed immediately...
            storeApp(id, version + loops, 1, info, tp, baseDir, false, true);

            // Wait for app to be stored
            AppStorage app = waitApp(asm, id, version + loops, 15000);
            checkApp(app, id, version + loops);
        }
        finally
        {
            httpd.shutdown();
        }
    }

    /**
     * Fill up storage to limit, then delete an API to regain storage space.
     */
    public void testDeleteApi_freeSpace()
    {
        String name = getName();
        int priority = 1;
        String version = priority + "";
        File baseDir = BASEDIR2;
        AppDescriptionInfo info = generateInfo(baseDir, null);

        // Continue storing low-priority files until fails to do so
        int loops = 0;
        for (; loops < 1000; ++loops)
        {
            boolean success = storeApi(name + loops, version, priority, info, baseDir);
            if (!success) break;
        }
        assertTrue("Expected at least one storage for test", loops > 0);
        assertTrue("Could not fill up storage for test", loops < 1000);

        // Delete API should make space for new api
        asm.deleteApi(name + 0, version);

        assertTrue("Expected to be able to store an API after deletion", storeApi(name, version + ".a", 1, info,
                baseDir));
    }

    /**
     * Tests deleteApp() while the app is locked.
     */
    public void testDeleteApp_locked() throws Exception
    {
        AppID id = new AppID(OID, AID++);
        int version = 1;
        AppStorage app = asm.retrieveApp(id, version, "");
        assertNull("Did not expect api to be stored already", app);

        // Store App
        File baseDir = BASEDIR1;
        AppDescriptionInfo info = generateInfo(baseDir, null);
        TransportProtocol[] tp = { new LocalTransportProtocol() };
        storeApp(id, version, 100, info, tp, baseDir.getAbsolutePath(), true, true);

        // Retrieve App
        app = asm.retrieveApp(id, version, "");
        checkApp(app, id, version);

        assertTrue("Expected lock() to succeed", app.lock());
        File dir = app.getBaseDirectory();
        try
        {
            // Verify files are there to begin with
            checkFiles(app, info);
            asm.deleteApp(id, version);

            AppStorage app2 = asm.retrieveApp(id, version, "");
            assertNull("Expected App to be deleted from DB", app2);

            // Verify that files still exist
            checkFiles(app, info);
        }
        finally
        {
            app.unlock();
        }

        // Now check that files have been deleted
        waitForDeletion(dir, 10000);
        assertFalse("Expected files to be deleted", dir.exists());
    }

    /**
     * Tests that files aren't deleted when locked. But are no longer
     * retrievable.
     */
    public void testDeleteApi_locked() throws Exception
    {
        String name = getName();
        String version = "0";
        File baseDir = BASEDIR2;
        AppDescriptionInfo info = generateInfo(baseDir, null);
        ApiStorage api = asm.retrieveApi(name, version);
        assertNull("Did not expect api to be stored already", api);

        // Store API
        storeApi(name, version, 100, info, baseDir, true);

        // Retrieve API
        api = asm.retrieveApi(name, version);
        checkApi(api, name, version);

        assertTrue("Expected lock() to succeed", api.lock());
        File dir = api.getBaseDirectory();
        try
        {
            // Verify files are there to begin with
            checkFiles(api, info);
            asm.deleteApi(name, version);

            ApiStorage api2 = asm.retrieveApi(name, version);
            assertNull("Expected API to be deleted from DB", api2);

            // Verify that files still exist
            checkFiles(api, info);
        }
        finally
        {
            api.unlock();
        }

        // Now check that files have been deleted
        waitForDeletion(dir, 10000);
        assertFalse("Expected files to be deleted", dir.exists());
    }

    /**
     * Tests that updatePrivilegedCertificates purges on change of byte[].
     */
    public void testUpdatePrivilegedCertificates() throws Exception
    {
        // Store some app/apis
        Vector apis = new Vector();
        Vector baseDirs = new Vector();
        for (int i = 0; i < 3; ++i)
        {
            String name = getName() + i;
            String version = "a";
            ApiStorage api = asm.retrieveApi(name, version);
            assertNull("Did not expect api to be stored already", api);

            // Store API
            File srcDir = BASEDIR1;
            AppDescriptionInfo info = generateInfo(srcDir, null);
            storeApi(name, version, 100, info, srcDir, true);

            // Verify that they are there
            api = asm.retrieveApi(name, version);
            checkApi(api, name, version);
            apis.addElement(api);
            baseDirs.addElement(api.getBaseDirectory());
        }

        // Update privilegedCertificates (no change)
        byte[] certs = testCertBytes();
        asm.updatePrivilegedCertificates(certs);

        // Files should still be there
        for (Enumeration e = apis.elements(); e.hasMoreElements();)
        {
            ApiStorage api = (ApiStorage) e.nextElement();
            String name = api.getName();
            String version = api.getVersion();
            api = asm.retrieveApi(name, version);
            assertNotNull("Expected api to still be stored: " + name, api);
        }

        // Update privilegedCertificates (change)
        byte[] newCerts = new byte[certs.length * 2];
        System.arraycopy(certs, 0, newCerts, 0, certs.length);
        System.arraycopy(certs, 0, newCerts, certs.length, certs.length);
        asm.updatePrivilegedCertificates(newCerts);

        // Files should be deleted
        for (int i = 0; i < apis.size(); ++i)
        {
            ApiStorage api = (ApiStorage) apis.elementAt(i);
            File baseDir = (File) baseDirs.elementAt(i);
            String name = api.getName();
            String version = api.getVersion();
            ApiStorage api2 = asm.retrieveApi(name, version);
            assertNull("Expected api to have been purged: " + name, api2);

            waitForDeletion(baseDir, 10000 * apis.size());
            assertFalse("Expected baseDir to be deleted: " + name, baseDir.exists());
        }
    }

    /**
     * Tests that updatePrivilegedCertificates purges on change of byte[]. Given
     * that files are locked().
     */
    public void testUpdatePrivilegedCertificates_locked() throws Exception
    {
        // Store an app/api
        String name = getName();
        String version = "a";
        ApiStorage api = asm.retrieveApi(name, version);
        assertNull("Did not expect api to be stored already", api);

        // Store API
        File srcDir = BASEDIR1;
        AppDescriptionInfo info = generateInfo(srcDir, null);
        storeApi(name, version, 100, info, srcDir, true);

        // Verify that they are there
        api = asm.retrieveApi(name, version);
        checkApi(api, name, version);
        File baseDir = api.getBaseDirectory();

        // Update privilegedCertificates (no change)
        byte[] certs = testCertBytes();
        asm.updatePrivilegedCertificates(certs);

        // API should still be there
        api = asm.retrieveApi(name, version);
        assertNotNull("Expected api to still be stored: " + name, api);

        api.lock();
        try
        {
            // Update privilegedCertificates (change)
            byte[] newCerts = new byte[certs.length * 2];
            System.arraycopy(certs, 0, newCerts, 0, certs.length);
            System.arraycopy(certs, 0, newCerts, certs.length, certs.length);
            asm.updatePrivilegedCertificates(newCerts);

            // API should be purged from DB
            ApiStorage api2 = asm.retrieveApi(name, version);
            assertNull("Expected api to have been purged: " + name, api2);
        }
        finally
        {
            api.unlock();
        }

        // Files should be deleted
        waitForDeletion(baseDir, 10000);
        assertFalse("Files should have been deleted", baseDir.exists());
    }

    public static void checkFiles(AppStorage app, AppDescriptionInfo info)
    {
        checkFiles(app.getBaseDirectory(), info.files);
    }

    private static void checkFiles(File dir, FileInfo[] files)
    {
        for (int i = 0; i < files.length; ++i)
        {
            if ("*".equals(files[i].name)) continue;

            File f = new File(dir, files[i].name);

            assertTrue("Expected file to exist: " + f, f.exists());
            assertEquals("Unexpected filetype (isDir?)", files[i] instanceof DirInfo, f.isDirectory());
            if (files[i] instanceof DirInfo)
            {
                checkFiles(f, ((DirInfo) files[i]).files);
            }
        }
    }

    public static void checkWildcardFiles(File destDir, File srcDir, final boolean files, final boolean dirs)
    {
        assertTrue("Internal error - wildcard must include at least files and dirs", files || dirs);

        assertTrue("Expected destDir to exist: " + destDir, destDir.exists());
        assertTrue("Expected destDir to be a directory: " + destDir, destDir.isDirectory());

        FileFilter filter = new FileFilter()
        {
            public boolean accept(File f)
            {
                return f.isDirectory() ? dirs : files;
            }
        };

        File[] destFiles = destDir.listFiles();
        File[] srcFiles = srcDir.listFiles(filter);

        assertEquals("Expected same number of files in each directory: " + srcDir + "->" + destDir, srcFiles.length,
                destFiles.length);

        Hashtable srcSet = new Hashtable();
        for (int i = 0; i < srcFiles.length; ++i)
            srcSet.put(srcFiles[i].getName(), srcFiles[i]);

        for (int i = 0; i < destFiles.length; ++i)
        {
            File f = (File) srcSet.remove(destFiles[i].getName());
            assertNotNull("Wasn't supposed to store: " + destFiles[i], f);
        }
        assertEquals("Did not store all files from src: " + srcDir, 0, srcSet.size());

        // recurse as necessary
        if (dirs)
        {
            for (int i = 0; i < srcFiles.length; ++i)
            {
                if (srcFiles[i].isDirectory())
                {
                    File subDir = new File(destDir, srcFiles[i].getName());
                    checkWildcardFiles(subDir, srcFiles[i], true, true); // when
                                                                         // recursing...
                                                                         // get
                                                                         // all
                                                                         // file
                                                                         // types!
                }
            }
        }
    }

    protected void checkApp(AppStorage app, AppID id, int version)
    {
        assertNotNull("Expected non-null app [" + id + ":" + version + "]", app);
        assertNotNull("Base directory should not be null", app.getBaseDirectory());
    }

    protected void checkApi(ApiStorage api, String name, String version)
    {
        assertNotNull("Expected non-null API [" + name + ":" + version + "]", api);
        assertEquals("Unexpected name", name, api.getName());
        assertEquals("Unexpected version", version, api.getVersion());
        assertNotNull("Base directory should not be null", api.getBaseDirectory());
    }

    public static AppDescriptionInfo generateWildcardInfo(File baseDir, boolean files, boolean dirs)
    {
        assertTrue("Internal failure - cannot generate wildcard ADF w/out files or dirs", files || dirs);

        AppDescriptionInfo info = new AppDescriptionInfo();

        info.files = new FileInfo[(files && dirs) ? 2 : 1];
        int i = 0;
        if (files)
        {
            info.files[i] = info.new FileInfo();
            info.files[i].name = "*";
            info.files[i].size = 0; // don't care
            ++i;
        }
        if (dirs)
        {
            info.files[i] = info.new DirInfo();
            info.files[i].name = "*";
            info.files[i].size = 0; // don't care
            ++i;
        }

        return info;
    }

    public static AppDescriptionInfo generateInfo(File baseDir, FileFilter filter)
    {
        AppDescriptionInfo info = new AppDescriptionInfo();

        info.files = generateFileInfos(baseDir, filter, info);

        return info;
    }

    private static FileInfo[] generateFileInfos(File baseDir, final FileFilter filter, AppDescriptionInfo info)
    {
        Vector v = new Vector();
        File[] files = baseDir.listFiles();

        for (int i = 0; i < files.length; ++i)
        {
            if (filter != null && !filter.accept(files[i])) continue;

            FileInfo fileInfo;
            if (files[i].isDirectory())
            {
                DirInfo dirInfo = info.new DirInfo();
                fileInfo = dirInfo;
                dirInfo.files = generateFileInfos(files[i], filter, info);
            }
            else
            {
                fileInfo = info.new FileInfo();
                fileInfo.size = files[i].length();
            }
            fileInfo.name = files[i].getName();

            v.addElement(fileInfo);
        }

        FileInfo fileInfos[] = new FileInfo[v.size()];
        v.copyInto(fileInfos);
        return fileInfos;
    }

    public static long totalSize(AppDescriptionInfo info)
    {
        return totalSize(info.files);
    }

    private static long totalSize(FileInfo[] files)
    {
        long size = 0;
        for (int i = 0; i < files.length; ++i)
        {
            if (files[i] instanceof DirInfo)
                size += totalSize(((DirInfo) files[i]).files);
            else
                size += files[i].size;
        }
        return size;
    }

    /**
     * <pre>
     * Hashfile () {
     *   digest_count 16 uimsbf
     *   for( i=0; i<digest_count; i++ ) {
     *     digest_type 8 uimsbf
     *     name_count 16 uimsbf
     *     for( j=0; j<name_count; j++ ) {
     *       name_length 8 uimsbf
     *       for( k=0; k<name_length; k++ ) {
     *         name_byte 8 bslbf
     *       }
     *     }
     *     for( j=0; j<digest_length; j++ ) {
     *       digest_byte 8 bslbf
     *     }
     *   }
     * }
     * </pre>
     * 
     * @param dir
     * @return byte array containing hashfile
     */
    private static byte[] hashfile(File dir) throws IOException
    {
        final boolean DEBUG = false;
        String[] files = dir.list();
        if (files == null) throw new IOException("Cannot access " + dir);

        if (DEBUG) System.out.println("Generating hashfile: " + dir);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream hashfile = new DataOutputStream(bos);

        // digest_count 16 uimsbf
        hashfile.writeShort((short) files.length);
        for (int i = 0; i < files.length; ++i)
        {
            if (DEBUG) System.out.println("\t" + files[i]);

            // digest_type 8 uimsbf
            hashfile.writeByte((byte) 0);

            // name_count 16 uimsbf
            hashfile.writeShort((short) 1);

            // name_length 8 uimsbf
            byte[] name = files[i].getBytes();
            hashfile.writeByte((byte) name.length);
            // name_byte 8 bslbf
            hashfile.write(name);
        }
        hashfile.flush();

        return bos.toByteArray();
    }

    private static byte[] dotdir(File dir) throws IOException
    {
        final boolean DEBUG = false;
        String[] files = dir.list();
        if (files == null) throw new IOException("Cannot access " + dir);

        if (DEBUG) System.out.println("Generating .dir: " + dir);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream dotdir = new PrintStream(bos);

        for (int i = 0; i < files.length; ++i)
        {
            if (DEBUG) System.out.println("\t" + files[i]);
            dotdir.println(files[i]);
        }
        dotdir.flush();

        return bos.toByteArray();
    }

    /**
     * Intended to be used to generate a fake ADF/SCDF at test runtime for
     * purposes of testing async storage.
     * 
     * @param dir
     *            root directory
     * @return byte array of ADF/SCDF contents
     * @throws IOException
     */
    private static byte[] adf(File dir) throws IOException
    {
        AppDescriptionInfo info = generateInfo(dir, null);
        String adfAsString = XmlManagerTest.createAppDesc(info);
        return adfAsString.getBytes();
    }

    abstract class ToDelete
    {
        abstract void delete() throws Exception;
    }

    class App extends ToDelete
    {
        private AppID id;

        private int version;

        App(AppID id, int version)
        {
            this.id = id;
            this.version = version;
        }

        void delete() throws Exception
        {
            // First lookup the app
            AppStorage app = asm.retrieveApp(id, version, "");
            File dir = (app == null) ? null : app.getBaseDirectory();

            // Now delete the app
            asm.deleteApp(id, version);

            // If app was previously retrieved, then wait for deletion
            if (dir != null)
            {
                waitForDeletion(dir, 10000L);
            }
        }
    }

    class Api extends ToDelete
    {
        private String name;

        private String version;

        Api(String name, String version)
        {
            this.name = name;
            this.version = version;
        }

        void delete() throws Exception
        {
            // First lookup the api
            ApiStorage api = asm.retrieveApi(name, version);
            File dir = (api == null) ? null : api.getBaseDirectory();

            // Now delete the api
            asm.deleteApi(name, version);

            // If api was previously retrieved, then wait for deletion
            if (api != null)
            {
                waitForDeletion(dir, 10000L);
            }
        }
    }

    class DeleteDir extends ToDelete
    {
        private File baseDir;

        public DeleteDir(File baseDir)
        {
            this.baseDir = baseDir;
        }

        void delete()
        {
            delete(baseDir);
        }

        private void delete(File f)
        {
            if (f.isDirectory())
            {
                File[] list = f.listFiles();
                if (list != null)
                {
                    for (int i = 0; i < list.length; ++i)
                        delete(list[i]);
                }
            }
            f.delete();
        }
    }

    /**
     * This extension of NanoHTTPD allows us to block waiting for specific
     * accesses. It also supports generating
     * 
     * @author Aaron Kamienski
     */
    public static class HttpD extends NanoHTTPD
    {
        public final Vector requests = new Vector();

        public final Vector responses = new Vector();

        private final File baseDir;

        private final boolean genHashfile;

        protected static int nextPort = 7999;

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
                    return new HttpD(++nextPort, baseDir, genHashfile);
                }
                catch (IOException e)
                {
                    continue;
                }
            }
            throw new IOException("No available port");
        }

        public HttpD(int port, File baseDir) throws IOException
        {
            this(port, baseDir, false);
        }

        public HttpD(int port, File baseDir, boolean genHashfile) throws IOException
        {
            super(port, baseDir);

            setVerbose(false);
            this.genHashfile = genHashfile;
            this.baseDir = baseDir;
        }

        public Response serve(String uri, String method, Properties header, Properties parms)
        {
            synchronized (requests)
            {
                requests.addElement(uri);
                requests.notifyAll();
            }
            Response response = super.serve(uri, method, header, parms);
            synchronized (responses)
            {
                responses.addElement(response);
                responses.notifyAll();
            }
            return response;
        }

        public Response serveFile(String uri, Properties header, File homeDir, boolean allowDirectoryListing)
        {
            Response r = super.serveFile(uri, header, homeDir, allowDirectoryListing);
            try
            {
                if (genHashfile && !HTTP_OK.equals(r.status))
                {
                    boolean hashfile;
                    // Fake hashfile/.dir (if one didn't exist)
                    if ((hashfile = uri.endsWith("hashfile")) || uri.endsWith(".dir"))
                    {
                        File file = new File(baseDir, uri.trim().replace('/', File.separatorChar));
                        File dir = file.getParentFile();
                        byte[] hash = hashfile ? hashfile(dir) : dotdir(dir);

                        Response r2 = new Response(HTTP_OK, MIME_DEFAULT_BINARY, new ByteArrayInputStream(hash));
                        r2.addHeader("Content-length", "" + hash.length);
                        return r2;
                    }
                }
            }
            catch (Exception e)
            {
                // e.printStackTrace();
            }
            return r;
        }

        private int wait(Vector v, int n, long ms) throws InterruptedException
        {
            synchronized (v)
            {
                int size;
                while ((size = v.size()) < n)
                {
                    v.wait(ms);
                    if (size == v.size()) // break out if nothing changed
                        break;
                }
                return v.size();
            }
        }

        public void clear()
        {
            clearRequest();
            clearResponse();
        }

        public void clearRequest()
        {
            requests.clear();
        }

        public int waitRequest(int n, long ms) throws InterruptedException
        {
            return wait(requests, n, ms);
        }

        public void clearResponse()
        {
            responses.clear();
        }

        public int waitResponse(int n, long ms) throws InterruptedException
        {
            return wait(responses, n, ms);
        }
    }

    private void storeApp(AppID id, int version, int priority, AppDescriptionInfo info, TransportProtocol[] tp,
            String baseDir, boolean now, boolean expect)
    {
        assertEquals("Expected storeApp to succeed", expect, storeApp(id, version, priority, info, tp, baseDir, now));
    }

    private boolean storeApp(AppID id, int version, int priority, AppDescriptionInfo info, TransportProtocol[] tp,
            String baseDir, boolean now)
    {
        boolean success = true;
        /*
        try
        {
            success = asm.storeApp(id, version, priority, info, tp, baseDir, now);
        }
        catch (FileSysCommunicationException e)
        {
            e.printStackTrace();
        }
        if (success) toDelete.addElement(new App(id, version));
        */
        return success;
        
    }

    private void storeApi(String name, String version, int priority, AppDescriptionInfo info, File baseDir,
            boolean expect)
    {
        assertEquals("Expected storeApi to succeed", expect, storeApi(name, version, priority, info, baseDir));
    }

    private boolean storeApi(String name, String version, int priority, AppDescriptionInfo info, File baseDir)
    {
        boolean success = asm.storeApi(name, version, priority, info, baseDir);
        if (success) toDelete.addElement(new Api(name, version));

        return success;
    }

    // private static final File BASEDIR1 = new File("/syscwd/apps/config");
    private static final File BASEDIR1 = new File("/syscwd/apps/launcher");

    private static final File BASEDIR2 = new File("/syscwd/apps/launcher");

    private static final int OID = 0xf1c1d00d;

    private static int AID = 0x6001; // TODO: may have a problem here once
                                     // authentication is on by default

    /**
     * Test implementation of <code>AppStorageManager</code>. Always says that
     * everything is dual-signed.
     * 
     * @author Aaron Kamienski
     */
    public static class DummyAuth implements AuthManager
    {
        public int appType = AuthInfo.AUTH_SIGNED_DUAL;

        public int fileType = -99;

        private AuthContext ac = new AuthContext()
        {
            public int getAppSignedStatus()
            {
                return appType;
            }

            public AuthInfo getClassAuthInfo(String targName, FileSys fs)
            {
                return DummyAuth.this.getClassAuthInfo(targName, fs);
            }

        };

        public AuthContext createAuthCtx(String initialFile, int signers, int orgId)
        {
            throw new UnsupportedOperationException();
        }

        public void setAuthCtx(CallerContext cc, AuthContext authCtx)
        {
            throw new UnsupportedOperationException();
        }

        public AuthContext getAuthCtx(CallerContext cc)
        {
            return ac;
        }

        public AuthInfo getClassAuthInfo(String targName, FileSys fs)
        {
            try
            {
                return newInfo(targName, fs, null);
            }
            catch (IOException e)
            {
                return null;
            }
        }

        public AuthInfo getFileAuthInfo(String targName, FileSys fs) throws IOException
        {
            return newInfo(targName, fs, null);
        }

        public AuthInfo getFileAuthInfo(String targName, FileSys fs, byte[] file) throws IOException
        {
            return newInfo(targName, fs, file);
        }

        private AuthInfo newInfo(final String name, final FileSys fs, final byte[] data) throws IOException
        {
            try
            {
                final byte[] fileData = (data != null) ? data : fs.getFileData(name).getByteData();

                return new AuthInfo()
                {

                    public boolean isSigned()
                    {
                        return true;
                    }

                    public int getClassAuth()
                    {
                        return fileType == -99 ? appType : fileType;
                    }

                    public byte[] getFile()
                    {
                        return fileData;
                    }
                };
            }
            catch (FileSysCommunicationException e)
            {
                return null;
            }
        }

        public AuthInfo getDirAuthInfo(String dir, FileSys fs, String[] files) throws IOException
        {
            return new AuthInfo()
            {
                public boolean isSigned()
                {
                    return true;
                }

                public int getClassAuth()
                {
                    return fileType == -99 ? appType : fileType;
                }

                public byte[] getFile()
                {
                    return null;
                }
            };
        }

        public String[] getHashfileNames(String dir, FileSys fs) throws IOException
        {
            // Expected to be used with HTTP FS and HttpD...
            // Use .dir...
            if (!dir.endsWith("/")) dir = dir + "/";
            BufferedReader in;
            try
            {
                in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(fs.getFileData(dir + ".dir")
                        .getByteData())));
            }
            catch (FileSysCommunicationException e)
            {
                return null;
            }
            String line;
            Vector lines = new Vector();
            while ((line = in.readLine()) != null)
                lines.addElement(line);
            String[] names = new String[lines.size()];
            lines.copyInto(names);

            return names;
        }

        public X509Certificate[][] getSigners(String targName, FileSys fs, byte[] file)
        {
            return null;
        }

        public X509Certificate[][] getSigners(String targName, boolean knownRoot, FileSys fs, byte[] file)
                throws InvalidFormatException, InterruptedIOException, MPEGDeliveryException, ServerDeliveryException,
                InvalidPathNameException, NotEntitledException, ServiceXFRException, InsufficientResourcesException
        {
            return null;
        }

        public void invalidate(String targName)
        {
        }

        public void registerCRLMount(String path)
        {
        }

        public void unregisterCRLMount(String path)
        {
        }

        public void setPrivilegedCerts(byte[] codes)
        {
        }

        public void destroy()
        {
        }
    }

    /* ====================== boilerplate =================== */

    public static InterfaceTestSuite isuite()
    {
        InterfaceTestSuite suite = new InterfaceTestSuite(AppStorageManagerTest.class);
        suite.setName(AppStorageManager.class.getName());
        return suite;
    }

    public static InterfaceTestSuite isuite(String[] tests)
    {
        InterfaceTestSuite suite = new org.cablelabs.test.iftc.InterfaceTestSuite(AppStorageManagerTest.class, tests);
        suite.setName(AppStorageManager.class.getName());
        return suite;
    }

    public AppStorageManagerTest(String name, ImplFactory f)
    {
        super(name, AppStorageManager.class, f);
    }

    protected AppStorageManager createAppStorageManager()
    {
        return (AppStorageManager) createManager();
    }

    private AppStorageManager asm;

    private Vector toDelete;

    private AuthManager savedAm;

    protected byte[] testCertBytes()
    {
        byte[] name = getName().getBytes();
        int n = (name.length + 19) / 20;
        byte[] bytes = new byte[n * 20];

        System.arraycopy(name, 0, bytes, 0, name.length);

        return bytes;
    }

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
        // System.out.println(getName());

        super.setUp();
        replaceAuthManager();
        asm = (AppStorageManager) mgr;
        asm.updatePrivilegedCertificates(testCertBytes());
        toDelete = new Vector();
    }

    protected void tearDown() throws Exception
    {
        for (Enumeration e = toDelete.elements(); e.hasMoreElements();)
            ((ToDelete) e.nextElement()).delete();
        asm = null;
        restoreAuthManager();
        super.tearDown();
    }
}
