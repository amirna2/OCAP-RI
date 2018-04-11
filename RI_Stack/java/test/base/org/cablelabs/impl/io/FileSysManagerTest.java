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

package org.cablelabs.impl.io;

import org.cablelabs.impl.io.FileSysManager;
import org.cablelabs.impl.io.http.TestDataFileHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests org.cablelabs.impl.io.FileSysManager
 * 
 * @author Paul Bramble
 */
public class FileSysManagerTest extends TestCase
{

    /**
     * Assertion: When setFileManager(null) is invoked, it returns a
     * defaultFileSystem instance.
     * 
     * @throws Exception
     */
    public void testSetDefaultFileManager() throws Exception
    {

        try
        {
            // Set the file manager and force it to use the default file system.
            FileSysManager.setFileManager(null);
        }
        catch (Exception e)
        {
            fail("Exception occurred construction HttpOpenFile instance : " + e.getMessage());
        }

        // Verify file manager uses the default file system.
        FileSys fileSys = FileSysManager.getFileSys("doesntMatter");
        assertTrue("Expected instance of DefaultFileSyst, returned " + fileSys.getClass().getName() + " instead",
                fileSys instanceof DefaultFileSys);
    }

    /**
     * Assertion: When doGetWriteableFileSystem is invoked on the default
     * DefaultFileSystem instance returned by a null call to getFileSys, it
     * returns an instance of DefaultWritableFileSys.
     * 
     * @throws Exception
     */

    public void testGetWritableFileSys() throws Exception
    {

        try
        {
            // Set file manager forcing it to use default writable file system.
            FileSysManager.setFileManager(null);
        }
        catch (Exception e)
        {
            fail("Exception occurred construction HttpOpenFile instance : " + e.getMessage());
        }

        // Verify file manager is using the default writable file system.
        WriteableFileSys fileSys = FileSysManager.getWriteableFileSys("doesntMatter");
        assertTrue("Expected instance of DefaultFileSyst, returned " + fileSys.getClass().getName() + " instead",
                fileSys instanceof WriteableFileSys);
    }

    /**
     * Assertion: FileSysManager.setFileManager(valid Manager) sets the file
     * manager to point to the specified object, and calls to getFileSys()
     * return the FileSystems associated to that file manager.
     * 
     * @throws Exception
     */

    public void testSetFileManagerWithValue() throws Exception
    {
        String path1 = "/home/dir1/dir2/myFile";

        String path2 = "/home/dir1/dir2/yourFile";

        // Setup Test Environment.
        FileSys dummyFS1 = new DummyFS(path1);
        FileSys dummyFS2 = new DummyFS(path2);
        DummyFileSysMgr myManager = new DummyFileSysMgr();
        myManager.addFileSys(path1, dummyFS1);
        myManager.addFileSys(path2, dummyFS2);
        FileSysManager.setFileManager(myManager);

        // Verify manager was set as requested and not to default.
        DummyFS dfs1 = (DummyFS) FileSysManager.getFileSys(path1);
        assertTrue("Expected FileSys for path " + path1 + " found " + (dfs1 == null ? "null" : dfs1.getKey())
                + " instead", dummyFS1.equals(dfs1));

        // Reverify by finding second file system.
        DummyFS dfs2 = (DummyFS) FileSysManager.getFileSys(path2);
        assertTrue("Expected FileSys for path " + path1 + " found " + (dfs2 == null ? "null" : dfs1.getKey())
                + " instead", dummyFS2.equals(dfs2));

        // FileSysManager.setFileManager(null);

    }

    /**
     * A stub FileSysMgr implementation for testing.
     * 
     */
    public class DummyFileSysMgr implements FileSysMgr
    {
        private Hashtable fileSystems = new Hashtable();

        public void addFileSys(String path, FileSys fileSys)
        {
            fileSystems.put(path, fileSys);
        }

        public FileSys doGetFileSys(String path)
        {
            return (FileSys) fileSystems.get(path);
        }

        public WriteableFileSys doGetWriteableFileSys(String path)
        {
            return null;
        }
    }

    /**
     * A stub FileSys implementation for testing.
     * 
     */
    public static class DummyFS implements FileSys
    {

        private String m_key = null;

        public DummyFS(String key)
        {
            m_key = key;
        }

        public OpenFile open(String path) throws FileNotFoundException
        {
            return null;
        }

        public FileData getFileData(String path) throws FileNotFoundException, IOException
        {
            return null;
        }

        public FileSys load(String path, int loadMode) throws FileNotFoundException, IOException
        {
            return null;
        }

        public AsyncLoadHandle asynchronousLoad(String path, int loadMode, AsyncLoadCallback cb)
                throws FileNotFoundException
        {
            return null;
        }

        public FileSys unload()
        {
            return null;
        }

        public X509Certificate[][] getSigners(String path)
        {
            return null;
        }

        public X509Certificate[][] getSigners(String path, boolean checkRoot)
        {
            return null;
        }

        public boolean isDir(String path)
        {
            return false;
        }

        public String[] list(String path)
        {
            return null;
        }

        public boolean exists(String path)
        {
            return false;
        }

        public long length(String path)
        {
            return 0;
        }

        public boolean canRead(String path)
        {
            return false;
        }

        public boolean canWrite(String path)
        {
            return false;
        }

        public boolean delete(String path)
        {
            return false;
        }

        public String getCanonicalPath(String path)
        {
            return null;
        }

        public boolean isFile(String path)
        {
            return false;
        }

        public long lastModified(String path)
        {
            return 0;
        }

        public boolean mkdir(String path)
        {
            return false;
        }

        public boolean renameTo(String fromPath, String toPath)
        {
            return false;
        }

        public boolean create(String path)
        {
            return false;
        }

        public boolean setLastModified(String path, long time)
        {
            return false;
        }

        public boolean setReadOnly(String path)
        {
            return false;
        }

        public boolean deleteOnExit(String path)
        {
            return false;
        };

        public String getKey()
        {
            return m_key;
        }

        public boolean equals(Object o)
        {
            return ((o instanceof DummyFS) && ((DummyFS) o).getKey() != null)
                    && (getKey().equals(((DummyFS) o).getKey()));
        }

        public String contentType(String path)
        {
            return null;
        }
    }

    /**
     * Restore the file manager to default status. Leaving the dummy file sys
     * and filesys managers in place negativly impacts the http based tests and
     * causes some of them to fail.
     */
    protected void tearDown() throws Exception
    {
        FileSysManager.setFileManager(null);
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
        TestSuite suite = new TestSuite(FileSysManagerTest.class);
        return suite;
    }

}
