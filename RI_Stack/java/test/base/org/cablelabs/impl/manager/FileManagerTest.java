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

import org.cablelabs.impl.io.DefaultFileSys;
import org.cablelabs.impl.io.FileDataImpl;
import org.cablelabs.impl.manager.filesys.AuthFileSys;
import org.cablelabs.impl.manager.filesys.CachedFileSys;
import org.cablelabs.impl.manager.filesys.OCFileSys;

import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

public class FileManagerTest extends ManagerTest
{
    /**
     * Tests the getFileSys() registerFileSys() and unregisterFileSys() methods
     * by registering some path/FileSys pairs and then tries to retrive them.
     * 
     */
    public void test_registerFileSys()
    {
        DefaultFileSys dfs = new DefaultFileSys();
        OCFileSys ofs = new OCFileSys();
        AuthFileSys afs = new AuthFileSys(ofs);
        CachedFileSys cfs = new CachedFileSys(dfs);

        // first register some file systems
        fileMgr.registerFileSys("/mount", dfs);
        fileMgr.registerFileSys("/mount/dir", dfs);
        fileMgr.registerFileSys("/oc/3/dir/", ofs);
        fileMgr.registerFileSys("/oc/2/", afs);
        fileMgr.registerFileSys("/oc/3", afs);
        fileMgr.registerFileSys("/oc", dfs);
        fileMgr.registerFileSys("/oc/30", cfs);

        // next see if we can retrieve them
        assertEquals("FileSys did not match for path=/mount", dfs, fileMgr.getFileSys("/mount"));
        assertEquals("FileSys did not match for path=/mount/dir", dfs, fileMgr.getFileSys("/mount/dir"));
        assertEquals("FileSys did not match for path=/mount/test.txt", dfs, fileMgr.getFileSys("/mount/test.txt"));
        assertEquals("FileSys did not match for path=/mount/dir/test.txt", dfs,
                fileMgr.getFileSys("/mount/dir/test.txt"));
        assertEquals("FileSys did not match for path=/oc/3", afs, fileMgr.getFileSys("/oc/3"));
        assertEquals("FileSys did not match for path=/oc/3/test.txt", afs, fileMgr.getFileSys("/oc/3/test.txt"));
        assertEquals("FileSys did not match for path=/oc/3/dir/test.txt", ofs, fileMgr.getFileSys("/oc/3/dir/test.txt"));
        assertEquals("FileSys did not match for path=/oc/2/dir1/dir2/test.txt", afs,
                fileMgr.getFileSys("/oc/2/dir1/dir2/test.txt"));
        assertEquals("FileSys did not match for path=/oc/2/test.txt", afs, fileMgr.getFileSys("/oc/2/test.txt"));
        assertEquals("FileSys did not match for path=/oc/2/dir1/dir2/test.txt", afs,
                fileMgr.getFileSys("/oc/2/dir1/dir2/test.txt"));
        assertEquals("FileSys did not match for path=/oc/2", afs, fileMgr.getFileSys("/oc/2"));
        assertEquals("FileSys did not match for path=/oc/test.txt", dfs, fileMgr.getFileSys("/oc/test.txt"));
        assertEquals("FileSys did not match for path=/oc/dir/test.txt", dfs, fileMgr.getFileSys("/oc/dir/test.txt"));
        assertEquals("FileSys did not match for path=/oc/30/test.txt", cfs, fileMgr.getFileSys("/oc/30/test.txt"));
        assertEquals("FileSys did not match for path=/oc/30", cfs, fileMgr.getFileSys("/oc/30"));

        // next remove a path and see if the correct FileSys objects can be
        // retrieved
        fileMgr.unregisterFileSys("/oc/3");
        assertEquals("FileSys did not match for path=/oc/3", dfs, fileMgr.getFileSys("/oc/3"));
        assertEquals("FileSys did not match for path=/oc/3/file.txt", dfs, fileMgr.getFileSys("/oc/3/file.txt"));
        assertEquals("FileSys did not match for path=/oc/3/dir/file.txt", ofs, fileMgr.getFileSys("/oc/3/dir/file.txt"));
        // remove /oc/30 and add /oc/3 and lookup /oc/30 to verify we can tell
        // the difference between 30 and 3
        fileMgr.unregisterFileSys("/oc/30");
        fileMgr.registerFileSys("/oc/3", afs);
        assertEquals("FileSys did not match for path=/oc/30", dfs, fileMgr.getFileSys("/oc/30"));
    }

    /**
     * Tests updateCache(), flushCache() and getCached()
     */
    public void testCache()
    {
        byte data[] = new byte[] { (byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd };
        byte data2[] = new byte[] { (byte) 0x11, (byte) 0x22, (byte) 0x33 };
        String path = "/testdir/file.txt";
        String path2 = "/testdir/file2.txt";

        // verify that no data is available for the two paths
        assertNull("Did not expect to have any file data in the cache", fileMgr.getCached(path));
        assertNull("Did not expect to have any file data in the cache", fileMgr.getCached(path2));

        // add data for the first path

        FileDataImpl fd = new FileDataImpl(data);
        fileMgr.updateCache(path, fd);
        // verify that the data is cached
        assertNotNull("Expected to have file data in cache", fileMgr.getCached(path));

        FileDataImpl fd2 = new FileDataImpl(data2);
        // add data for the second path
        fileMgr.updateCache(path2, fd2);
        // verify that the data is cached
        assertNotNull("Expected to have file data in cache", fileMgr.getCached(path2));

        // get the data and verify its contents
        byte test[] = fileMgr.getCached(path).getByteData();
        assertEquals("Expected data to have the same length", data.length, test.length);
        for (int i = 0; i < test.length; i++)
        {
            assertEquals("Expected data to match at i=" + i, data[i], test[i]);
        }

        // get the data and verify its contents
        test = fileMgr.getCached(path2).getByteData();
        assertEquals("Expected data to have the same length", data2.length, test.length);
        for (int i = 0; i < test.length; i++)
        {
            assertEquals("Expected data to match at i=" + i, data2[i], test[i]);
        }

        // change the data for path2 and verify the changes can be retrieved
        fileMgr.updateCache(path2, fd);
        test = fileMgr.getCached(path2).getByteData();
        assertNotNull("Expected to have file data in the cache", fileMgr.getCached(path2));
        assertEquals("Expected data to have the same length", data.length, test.length);
        // verify the correct data was retrieved
        for (int i = 0; i < test.length; i++)
        {
            assertEquals("Expected data to match at i=" + i, data[i], test[i]);
        }

        // remove all data and verify that it cannot be retrieved
        fileMgr.flushCache(path);
        assertNull("Did not expect to have any file data in the cache", fileMgr.getCached(path));

        fileMgr.flushCache(path2);
        assertNull("Did not expect to have any file data in the cache", fileMgr.getCached(path2));
    }

    /*  ********* Boilerplate ********* */

    public static InterfaceTestSuite isuite()
    {
        InterfaceTestSuite suite = new InterfaceTestSuite(FileManagerTest.class);
        suite.setName(FileManager.class.getName());
        return suite;
    }

    public FileManagerTest(String name, ImplFactory f)
    {
        super(name, FileManager.class, f);
    }

    protected FileManager createFileManager()
    {
        return (FileManager) createManager();
    }

    private FileManager fileMgr;

    protected void setUp() throws Exception
    {
        super.setUp();
        fileMgr = (FileManager) mgr;
    }

    protected void tearDown() throws Exception
    {
        fileMgr = null;
        super.tearDown();
    }

}
