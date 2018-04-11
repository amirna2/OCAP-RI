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

package org.cablelabs.impl.io.http;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.cablelabs.impl.io.OpenFile;
import org.cablelabs.impl.manager.AppStorageManagerTest.HttpD;
import org.cablelabs.impl.security.SecurityManagerImplTest.Loader;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.test.ProxySecurityManager;

public class HttpFileSysTest extends TestCase
{
    /**
     * @todo disabled due to 5571
     */
    public void testOpen() throws Exception
    {
        HttpD httpd = HttpD.createInstance(new File(TESTDIR), true);
        try
        {
            HttpFileSys fs = new HttpFileSys(httpd.getMyURL().toString(), "/http0");
            File f = new File(TESTDIR + "/testfile.txt");
            OpenFile of;
            createFile(f, 100);
            of = fs.open("/http0/testfile.txt");
            assertNotNull("Expected to get an OpenFile instance from a call to open()", of);
            // assertTrue("Expected to get an instance of an HttpOpenFile", of
            // instanceof HttpOpenFile);
            try
            {
                of = fs.open("http0/doesnotexist.txt");
                fail("Expected FileNotFound to be thrown");
            }
            catch (FileNotFoundException e)
            {
                // pass
            }
        }
        finally
        {
            httpd.shutdown();
        }
    }

    public void testOpenClass()
    {

    }

    public void testExists()
    {

    }

    public void testIsDir()
    {

    }

    public void testGetFileData()
    {

    }

    public void testList()
    {

    }

    /**
     * Tests that open() succeeds even if SocketPermission isn't provided to
     * caller.
     * 
     * @todo disabled due to 5571
     */
    public void testOpen_privileged() throws Exception
    {
        doTestPrivileged(OpenPrivTester.class.getName());
    }

    /**
     * @todo disabled due to 5571
     */
    public void testExists_privileged() throws Exception
    {
        doTestPrivileged(ExistsPrivTester.class.getName());
    }

    /**
     * @todo disabled due to 5571
     */
    public void testIsDir_privileged() throws Exception
    {
        doTestPrivileged(IsDirPrivTester.class.getName());
    }

    /**
     * @todo disabled due to 5571
     */
    public void testGetFileData_privileged() throws Exception
    {
        doTestPrivileged(GetFileDataPrivTester.class.getName());
    }

    /**
     * @todo disabled due to 5571
     */
    public void testList_privileged() throws Exception
    {
        doTestPrivileged(ListPrivTester.class.getName());
    }

    private void doTestPrivileged(String testerClassName) throws Exception
    {
        HttpD httpd = HttpD.createInstance(new File(TESTDIR), true);
        try
        {
            HttpFileSys fs = new HttpFileSys(httpd.getMyURL().toString(), "/http0");
            File f = new File(TESTDIR + "/testfile.txt");
            createFile(f, 100);

            Loader loader = new Loader();
            PermissionCollection perms = new Permissions();
            // No SocketPermissions are given!
            perms.add(new FilePermission("/*", "read"));
            perms.add(new FilePermission("/-", "read"));
            loader.pd = new ProtectionDomain(new CodeSource(null, null), perms);
            loader.addClass(testerClassName);
            Class testerClass = loader.loadClass(testerClassName);
            PrivTester tester = (PrivTester) testerClass.newInstance();

            SecurityManager sm = new SecurityManager();
            ProxySecurityManager.install();
            ProxySecurityManager.push(sm);
            try
            {
                tester.test(fs, "/http0/testfile.txt");
            }
            finally
            {
                ProxySecurityManager.pop();
            }
        }
        finally
        {
            httpd.shutdown();
        }
    }

    public static interface PrivTester
    {
        public void test(HttpFileSys fs, String name) throws Exception;
    }

    public static class OpenPrivTester implements PrivTester
    {
        public void test(HttpFileSys fs, String name) throws Exception
        {
            assertNotNull("Expected file to open", fs.open(name));
        }
    }

    public static class ExistsPrivTester implements PrivTester
    {
        public void test(HttpFileSys fs, String name) throws Exception
        {
            assertTrue("Expected file to exist", fs.exists(name));
        }
    }

    public static class IsDirPrivTester implements PrivTester
    {
        public void test(HttpFileSys fs, String name) throws Exception
        {
            assertFalse("Expected isDir to be false", fs.isDir(name));

            assertTrue("Expected isDir to be true", fs.isDir(getParent(name)));
        }
    }

    public static class GetFileDataPrivTester implements PrivTester
    {
        public void test(HttpFileSys fs, String name) throws Exception
        {
            byte[] data = fs.getFileData(name).getByteData();
            assertNotNull("Expected file data to be read", data);
            // assertEquals("Unexpected data length", data40k.length,
            // data.length); // Actually size done by createFile()
        }
    }

    public static class ListPrivTester implements PrivTester
    {
        public void test(HttpFileSys fs, String name) throws Exception
        {
            String[] list = fs.list(getParent(name));
            assertNotNull("Expected files to exist", list);
            assertTrue("Expected files to exist", list.length > 0);

            String fname = getFileName(name);
            boolean found = false;
            for (int i = 0; !found && i < list.length; ++i)
                found = fname.equals(list[i]);
            assertTrue("Expected file to be found", found);
        }
    }

    public static String getParent(String path)
    {
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);

        int i = path.lastIndexOf('/');
        if (i <= 0) return null;

        return path.substring(0, i);
    }

    public static String getFileName(String path)
    {
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);

        int i = path.lastIndexOf('/');
        if (i <= 0) return path;
        return path.substring(i + 1);
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(HttpFileSysTest.class);
        return suite;
    }

    public HttpFileSysTest(String name)
    {
        super(name);
    }

    private String TESTDIR;

    protected void setUp() throws Exception
    {
        super.setUp();
        String ocap = MPEEnv.getEnv("OCAP.persistent.root");
        assertNotNull("Error! Need a place to write files", ocap);
        TESTDIR = ocap + "/junit";
        File testDir = new File(TESTDIR);
        if (!testDir.exists()) testDir.mkdirs();
    }

    protected void tearDown() throws Exception
    {
        cleanDir(new File(TESTDIR));
        super.tearDown();
    }

    public static final byte data40k[];

    static
    {
        data40k = new byte[40 * 1024];
        for (int i = 0; i < 7 * 1024; ++i)
            data40k[i] = (byte) ((i % 26) + 'a');
    }

    private void createFile(File f, int size)
    {
        // Give asynchronicity of system time to settle.
        try
        {
            Thread.sleep(1 * 1000);
        }
        catch (InterruptedException e)
        {
            // don't care.
        }

        try
        {
            if (f.exists() == false)
            {
                if (f.createNewFile() == false) return;
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(data40k, 0, size);
                fos.close();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return;
        }
    }

    private void cleanDir(File dir)
    {
        // Give asynchronicity of system time to settle.
        try
        {
            Thread.sleep(1 * 1000);
        }
        catch (InterruptedException e)
        {
            // don't care.
        }

        if (dir.exists() == false) return;

        String[] contents = dir.list();
        for (int i = 0; i < contents.length; ++i)
        {
            File target;
            try
            {
                target = new File(dir.getCanonicalFile() + "/" + contents[i]);
            }
            catch (Exception e)
            {
                continue;
            }

            if (target.isDirectory()) cleanDir(target);
            target.delete();
        }
    }
}
